/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools;

import static edu.ie3.tools.utils.ConfigurationParameters.*;

import edu.ie3.tools.models.ExtractorResult;
import edu.ie3.tools.models.persistence.CoordinateModel;
import edu.ie3.tools.models.persistence.FileModel;
import edu.ie3.tools.models.persistence.ICONWeatherModel;
import edu.ie3.tools.utils.DatabaseController;
import edu.ie3.tools.utils.FileEraser;
import edu.ie3.tools.utils.LockMechanism;
import edu.ie3.tools.utils.enums.Parameter;
import java.io.File;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Decompresses bz2 archives to GRIB2 files, extracts and converts values and interpolates entities
 * with previous ones
 *
 * @author krause
 * @version 4.0
 */
public class Converter implements Runnable {
  public static final Logger logger = LogManager.getLogger(Converter.class);
  public static final Logger fileStatusLogger = LogManager.getLogger("FileStatus");

  private FileEraser fileEraser;
  private Collection<CoordinateModel> coordinates;
  private DatabaseController dbController;
  private EnumMap<Parameter, FileModel> parameterLevelToFile;
  private final int noOfProcessors = Runtime.getRuntime().availableProcessors();
  private final ExecutorService decompressionExecutor =
      Executors.newFixedThreadPool((int) Math.ceil(noOfProcessors / 3d));
  private final ExecutorService parsingExecutor =
      Executors.newFixedThreadPool((int) Math.ceil(noOfProcessors / 2d));
  private final ExecutorService fileEraserExecutor =
      Executors.newFixedThreadPool((int) Math.ceil(noOfProcessors / 3d));

  /** @return timestamp for logging output (e.g "MR 09.10.2018 18:00 - TS 01 | ") */
  public static String getFormattedTimestep(@NotNull ZonedDateTime modelrun, int timestep) {
    return "MR "
        + MODEL_RUN_FORMATTER.format(modelrun)
        + " - TS "
        + String.format("%02d", timestep)
        + " |    ";
  }

  /** @return timestamp for logging output (e.g "MR 09.10.2018 18:00 | ") */
  public static String getFormattedModelrun(@NotNull ZonedDateTime modelrun) {
    return "MR " + MODEL_RUN_FORMATTER.format(modelrun) + "         |    ";
  }

  /** @return timestamp for logging output (e.g "MR 09.10.2018 18:00 - TS 01 | ") */
  public static String getFormattedTimestep(@NotNull FileModel file) {
    return getFormattedTimestep(file.getModelrun(), file.getTimestep());
  }

  @Override
  public void run() {
    if (acquireLock()) {
      fileStatusLogger.setLevel(Main.filestatus ? Level.ALL : Level.OFF);
      logger.setLevel(Main.debug ? Level.ALL : Level.INFO);
      printInit();
      validateConnectionProperties();
      convert();
    } else logger.info("Converter is already running.");
  }

  /** Validates Connection Properties from user input */
  private void validateConnectionProperties() {
    Properties receivedProperties = new Properties();

    if (edu.ie3.tools.Main.connectionUrl != null)
      receivedProperties.setProperty(
          "javax.persistence.jdbc.url", edu.ie3.tools.Main.connectionUrl);

    if (edu.ie3.tools.Main.databaseUser != null)
      receivedProperties.setProperty(
          "javax.persistence.jdbc.user", edu.ie3.tools.Main.databaseUser);

    if (edu.ie3.tools.Main.databasePassword != null)
      receivedProperties.setProperty(
          "javax.persistence.jdbc.password", edu.ie3.tools.Main.databasePassword);

    if (!edu.ie3.tools.Main.database_schema.equals("icon"))
      receivedProperties.setProperty(
          "hibernate.default_schema", edu.ie3.tools.Main.database_schema);

    dbController = new DatabaseController(PERSISTENCE_UNIT_NAME, receivedProperties);
  }

  public void printInit() {
    logger.info("________________________________________________________________________________");
    logger.info("Converter started");
    logger.trace("Program arguments:");
    Main.printProgramArguments().forEach(s -> logger.trace("   " + s));
  }

  private void convert() {
    String formattedModelrun = "";
    try {
      fileEraser = new FileEraser(edu.ie3.tools.Main.directory, dbController);

      // retrieves the newest possible modelrun ( = newest downloaded modelrun)
      ZonedDateTime newestPossibleModelrun =
          (ZonedDateTime)
              dbController.execSingleResultNamedQuery(
                  FileModel.NEWEST_DOWNLOADED_MODELRUN, Collections.emptyList());

      // retrieves the starting modelrun ( = oldest modelrun where persisted==false or no converter
      // run info available)
      ZonedDateTime currentModelrun =
          (ZonedDateTime)
              dbController.execSingleResultNamedQuery(
                  FileModel.OLDEST_MODELRUN_WITH_UNPROCESSED_FILES, Collections.emptyList());

      if (currentModelrun != null) {
        coordinates = getCoordinates();
        while (currentModelrun.isBefore(newestPossibleModelrun)
            || currentModelrun.isEqual(newestPossibleModelrun)) {
          logger.info(
              "############################### "
                  + MODEL_RUN_FORMATTER.format(currentModelrun)
                  + " ###############################");
          formattedModelrun = getFormattedModelrun(currentModelrun);
          long tic;
          long toc;
          tic = System.currentTimeMillis();
          for (int timestep = 0; timestep < edu.ie3.tools.Main.timesteps; timestep++) {
            handleTimestep(currentModelrun, timestep);
            dbController.flush();
          }

          toc = System.currentTimeMillis();
          logger.debug(formattedModelrun + "This modelrun took " + (toc - tic) / 60000 + "m \n");
          currentModelrun = currentModelrun.plusHours(3); // increment modelrun
        }
      }
    } catch (Exception e) {
      logger.fatal(formattedModelrun, e);
    } finally {
      shutdown();
    }
  }

  /** opens archive files and converts the data for one timestep */
  private void handleTimestep(ZonedDateTime currentModelrun, int timestep) {
    String formattedTimestep = getFormattedTimestep(currentModelrun, timestep);
    parameterLevelToFile = new EnumMap<>(Parameter.class);

    logger.info(formattedTimestep + "Opening of archive files started");
    long tic, toc;
    tic = System.currentTimeMillis();
    openArchiveFiles(currentModelrun, timestep);
    toc = System.currentTimeMillis();
    logger.info(
        formattedTimestep + "Opening of archive files finished (" + (toc - tic) / 1000 + "s)");

    convertTimeStep(currentModelrun, timestep);

    logger.info(formattedTimestep + "Timestep finished");
  }

  private void openArchiveFiles(ZonedDateTime currentModelrun, int timestep) {
    String folderpath =
        Main.directory
            + File.separator
            + FILENAME_DATE_FORMATTER.format(currentModelrun)
            + File.separator;
    List<Decompressor> tasks = new ArrayList<>();
    List<FileModel> files = new ArrayList<>();
    for (Parameter param : Parameter.values()) {
      FileModel file =
          dbController.find(
              FileModel.class, FileModel.createFileName(currentModelrun, timestep, param));
      if (file != null) {
        files.add(file);
        if (file.isSufficientSize() && (file.isValidFile() == null || file.isValidFile())) {
          if (!file.isPersisted() && !file.isArchiveFileDeleted() && !file.isDecompressed()) {
            tasks.add(new Decompressor(file, folderpath));
          }
        } else if (file.getDownloadFails() > 3
            || file.getModelrun().isBefore(ZonedDateTime.now().minusDays(1))) {
          if (edu.ie3.tools.Main.deleteDownloadedFiles) {
            logger.trace(
                "Delete file "
                    + file.getName()
                    + " because it did not have a valid size or content.");
            fileEraserExecutor.submit(fileEraser.eraseCallable(file));
          } else {
            logger.trace(
                "File "
                    + file.getName()
                    + " did not have a valid size or content. If you want to delete it pass -del as argument!");
          }
        }
      }
    }

    try {
      decompressionExecutor.invokeAll(tasks);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    files.forEach(
        file -> {
          if (file.isDecompressed() && (file.isValidFile() == null || file.isValidFile()))
            parameterLevelToFile.put(file.getParameter(), file);
          else {
            file.setValidFile(false);
            fileStatusLogger.trace(
                file.getName() + "  |  vff  |  valid_file = false  | Decompression failed");
          }
        });
  }

  /**
   * Calls {@link Converter#convertTimeStep(ZonedDateTime, int, String)} with default folderpath
   * <br>
   * Extracts values from the previously decompressed archive files into {@link ICONWeatherModel
   * WeatherPreparationModels} and persists them. <br>
   * Marks status as persisted, if more than 50% of values could be extracted. <br>
   * Deletes files using {@link FileEraser#eraseCallable(FileModel)} after completion.
   */
  public void convertTimeStep(ZonedDateTime modelrun, int timestep) {
    String folderPath =
        Main.directory + File.separator + FILENAME_DATE_FORMATTER.format(modelrun) + File.separator;
    convertTimeStep(modelrun, timestep, folderPath);
  }

  /**
   * Extracts values from the previously decompressed archive files into {@link ICONWeatherModel
   * WeatherPreparationModels} and persists them. <br>
   * Marks status as persisted, if more than 50% of values could be extracted. <br>
   * Deletes files using {@link FileEraser#eraseCallable(FileModel)} after completion.
   */
  public void convertTimeStep(ZonedDateTime modelRun, int timeStep, String folderPath) {
    String formattedTimeStep = getFormattedTimestep(modelRun, timeStep);

    // Skip timeStep if no file could be decompressed
    HashSet params = new HashSet(parameterLevelToFile.keySet());
    if (params.isEmpty()) {
      logger.debug(formattedTimeStep + "Skipped");
      return;
    }

    logger.info(formattedTimeStep + "Parsing files");
    final ZonedDateTime date = modelRun.plusHours(timeStep);
    long tic, toc;
    tic = System.currentTimeMillis();

    List<ICONWeatherModel> entities = new ArrayList<>(coordinates.size());
    for (CoordinateModel coordinate : coordinates) {
      entities.add(new ICONWeatherModel(date, coordinate));
    }

    AtomicBoolean newValues = new AtomicBoolean(false);
    CompletionService<ExtractorResult> completionService =
        new ExecutorCompletionService<>(parsingExecutor);

    for (FileModel file : parameterLevelToFile.values()) {
      completionService.submit(new Extractor(folderPath, file, coordinates, Main.eccodes));
    }

    int received = 0;
    boolean errors = false;
    Collection<ExtractorResult> extractionResults = new ArrayList<>(parameterLevelToFile.size());
    while (received < parameterLevelToFile.size() && !errors) {
      try {
        Future<ExtractorResult> resultFuture =
            completionService.take(); // blocks if nothing is available

        // if we reached this point, we received something
        received++;

        // get the result from the received future
        ExtractorResult extractorResult = resultFuture.get();

        // add the extraction result to the result collection for further processing
        extractionResults.add(extractorResult);

        // update the file model information about the validity of the extractor result
        FileModel file = parameterLevelToFile.get(extractorResult.getParameter());
        file.setValidFile(extractorResult.isValidFile());
        fileStatusLogger.trace(
            file.getName()
                + (Boolean.TRUE.equals(file.isValidFile())
                    ? "  |  vft  |  valid_file = true  | Extraction"
                    : "  |  vff  |  valid_file = false  | Extraction"));

      } catch (InterruptedException | ExecutionException e) {
        errors = true;
        logger.error("An error occurred during parameter extraction!", e);
        Thread.currentThread().interrupt();
      }
    }

    // update the entities with the extraction results
    entities
        .parallelStream()
        .forEach(
            entity ->
                // get each parameter from the extraction results and update the entity accordingly
                extractionResults.stream()
                    .filter(extractorResult -> extractorResult.getCoordinatesToValues() != null)
                    .forEach(
                        extractorResult -> {
                          entity.setParameter(
                              extractorResult.getParameter(),
                              extractorResult.getCoordinatesToValues().get(entity.getCoordinate()));
                          newValues.set(true);
                        }));

    toc = System.currentTimeMillis();
    logger.info(formattedTimeStep + "Parsing completed (" + (toc - tic) / 1000 + "s)");

    if (!newValues.get() || errors) {
      logger.warn(
          formattedTimeStep
              + "Could not parse any new values or an error occurred during parsing (maybe the files are missing?). Skipped.");
      dbController.renewManager();
      return;
    }

    logger.info(formattedTimeStep + "Checking for previous entries ...");
    tic = System.currentTimeMillis();
    entities = checkForPreviousEntries(entities);
    toc = System.currentTimeMillis();
    logger.info(formattedTimeStep + "Checking done (" + (toc - tic) / 1000 + "s)");
    logger.info(formattedTimeStep + "Persisting entities ...");
    tic = System.currentTimeMillis();
    dbController.jdbcUpsert(entities);
    toc = System.currentTimeMillis();
    logger.info(formattedTimeStep + "Persisted all entities (" + (toc - tic) / 1000 + "s)");

    logger.info(formattedTimeStep + "Starting validation ...");
    tic = System.currentTimeMillis();
    validation();
    toc = System.currentTimeMillis();
    logger.info(formattedTimeStep + "Validation complete (" + (toc - tic) / 1000 + "s)");

    logger.info(formattedTimeStep + "Renewing database connection ...");
    tic = System.currentTimeMillis();
    dbController.renewManager();
    toc = System.currentTimeMillis();
    logger.info(
        formattedTimeStep
            + "Database connection successfully renewed ("
            + (toc - tic) / 1000
            + "s)");
  }

  /** Validate files by number of extracted coordinates, delete Files afterwards */
  private void validation() {
    List<Callable<Void>> deletionList = new ArrayList<>(parameterLevelToFile.size());
    String formattedTimestep =
        getFormattedTimestep(parameterLevelToFile.values().iterator().next());
    for (Map.Entry<Parameter, FileModel> entry : parameterLevelToFile.entrySet()) {
      FileModel file = entry.getValue();
      double relAmountMissingCoordinates =
          ((double) file.getMissingCoordinates()) / coordinates.size();
      if (relAmountMissingCoordinates < edu.ie3.tools.Main.faultTolerance) {
        file.setPersisted(true);
        fileStatusLogger.trace(file.getName() + "  |  pf   |  persisted = true  |  Validation");
      } else {
        logger.info(
            getFormattedTimestep(file)
                + entry.getKey().toString()
                + " had "
                + relAmountMissingCoordinates * 100
                + "% missing values");
      }
      deletionList.add(fileEraser.eraseCallable(file));
    }

    // wait for the fileEraser to be done, otherwise this creates a race condition and the database
    // is not updated properly, invokeAll() blocks
    if (edu.ie3.tools.Main.deleteDownloadedFiles) {
      logger.info(
          formattedTimestep + "Argument for file deletion has been passed. Deleting files ... ");
      try {
        fileEraserExecutor.invokeAll(deletionList);
      } catch (InterruptedException e) {
        logger.error(formattedTimestep + "Error while deleting files: {}", e);
        Thread.currentThread().interrupt();
      }
      logger.info(formattedTimestep + "File deletion complete!");
    }
  }

  @NotNull
  /** Checks for previous entries for all given entities and interpolates them */
  private List<ICONWeatherModel> checkForPreviousEntries(List<ICONWeatherModel> entities) {
    LinkedList<ICONWeatherModel> checkedEntities = new LinkedList<>();
    Map<ZonedDateTime, List<ICONWeatherModel>> entitiesByDate =
        entities.stream().collect(Collectors.groupingBy(ICONWeatherModel::getDate));
    entitiesByDate.forEach(
        (date, entitiesAtDate) ->
            checkedEntities.addAll(checkForPreviousEntries(entitiesAtDate, date)));
    return checkedEntities;
  }

  @NotNull
  /** Checks for previous entries for all given entities and interpolates them */
  private List<ICONWeatherModel> checkForPreviousEntries(
      List<ICONWeatherModel> entities, ZonedDateTime date) {
    Map<Integer, ICONWeatherModel> foundEntities =
        dbController.jdbcFindWeather(
            entities.stream().map(w -> w.getCoordinate().getId()).collect(Collectors.toList()),
            date);
    LinkedList<ICONWeatherModel> checkedEntities = new LinkedList<>();
    for (ICONWeatherModel entity : entities) {
      ICONWeatherModel foundWeather = foundEntities.get(entity.getCoordinate().getId());
      if (foundWeather != null) {
        foundWeather.interpolateValues(entity, Main.interpolationRatio);
        checkedEntities.add(foundWeather);
      } else {
        checkedEntities.add(entity);
      }
    }
    return checkedEntities;
  }

  public void shutdown() {
    shutdownAllExecutors();
    if (dbController != null) {
      dbController.flush();
      dbController.shutdown();
    }
    logger.info("Converter shut down");
    logger.info(
        "________________________________________________________________________________\n\n\n");
  }

  public boolean acquireLock() {
    LockMechanism lock = new LockMechanism("Converter");
    return !lock.isAppActive();
  }

  private void shutdownAllExecutors() {

    // decompression executor
    try {
      decompressionExecutor.shutdown();
      decompressionExecutor.awaitTermination(1, TimeUnit.MINUTES);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    } finally {
      decompressionExecutor.shutdownNow();
    }

    // parsing executor
    try {
      parsingExecutor.shutdown();
      parsingExecutor.awaitTermination(1, TimeUnit.MINUTES);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    } finally {
      parsingExecutor.shutdownNow();
    }

    // validation executor
    try {
      fileEraserExecutor.shutdown();
      fileEraserExecutor.awaitTermination(1, TimeUnit.MINUTES);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    } finally {
      fileEraserExecutor.shutdownNow();
    }
  }

  private Collection<CoordinateModel> getCoordinates() {
    HashMap<String, Object> namedCoordinateParams = new HashMap<>();
    namedCoordinateParams.put("minLatitude", Main.minLatitude);
    namedCoordinateParams.put("maxLatitude", Main.maxLatitude);
    namedCoordinateParams.put("minLongitude", Main.minLongitude);
    namedCoordinateParams.put("maxLongitude", Main.maxLongitude);
    return dbController.execNamedQuery(
        CoordinateModel.CoordinatesInRectangle, namedCoordinateParams);
  }
}
