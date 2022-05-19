/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools;

import static edu.ie3.tools.utils.ConfigurationParameters.*;

import edu.ie3.tools.models.persistence.FileModel;
import edu.ie3.tools.utils.DatabaseController;
import edu.ie3.tools.utils.enums.Parameter;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Downloads all DWD GRIB2-files in a window between [now-3h, now-27h] (rounded down to next
 * multiple of 3)
 *
 * @author krause
 * @version 4.0
 */
public class Downloader implements Runnable {
  public static final Logger logger = LogManager.getLogger(Downloader.class);
  public static final Logger filestatusLogger = LogManager.getLogger("FileStatus");

  private int downloadedFiles = 0;
  private final DatabaseController dbController;

  public Downloader() {
    dbController = new DatabaseController(PERSISTENCE_UNIT_NAME, validateConnectionProperties());
  }

  /**
   * @return if a connection to given url could be established
   */
  public static boolean isUrlReachable(String url) {
    HttpURLConnection.setFollowRedirects(false);
    HttpURLConnection con;
    try {
      con = (HttpURLConnection) new URL(url).openConnection();
      if (con == null) return false;
      con.setRequestMethod("HEAD");
      return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
    } catch (IOException e) {
      logger.warn("Exception during url reachable check occurred: " + e);
    }
    return false;
  }

  private void printInit() {
    logger.info("________________________________________________________________________________");
    logger.info("Downloader started");
  }

  private ZonedDateTime safeZonedDateTime(ZonedDateTime zdt) {
    return zdt.toLocalDateTime().atZone(ZoneId.of("UTC"));
  }

  public void run() {
    filestatusLogger.setLevel(Main.filestatus ? Level.ALL : Level.OFF);
    logger.setLevel(Main.debug ? Level.ALL : Level.INFO);
    printInit();
    try {

      // get newest possible modelrun: (now - 3h) rounded down to the next multiple of 3
      ZonedDateTime newestPossibleModelrun = safeZonedDateTime(ZonedDateTime.now());

      newestPossibleModelrun =
          newestPossibleModelrun
              .minusHours(3)
              .minusHours(newestPossibleModelrun.getHour() % 3)
              .withMinute(0)
              .withSecond(0)
              .withNano(0);

      // get earliest possible modelrun (newestPossibleModelrun - 1d)
      ZonedDateTime earliestPossibleModelrun = newestPossibleModelrun.minusDays(1);

      // try to download all former failed files
      logger.info(
          "############################## "
              + "Retry missing files"
              + " ##############################");
      ZonedDateTime newestDateDownloaded = earliestPossibleModelrun;
      List<FileModel> failedDownloads =
          dbController.execNamedQuery(
              FileModel.FAILED_DOWNLOADS, Collections.singletonList(earliestPossibleModelrun));
      for (FileModel file : failedDownloads) {
        ZonedDateTime modelrun = file.getModelrun();
        if (modelrun.isAfter(newestDateDownloaded)) {
          newestDateDownloaded = modelrun;
          logger.info("Current Modelrun: " + MODEL_RUN_FORMATTER.format(newestDateDownloaded));
        }
        String folder = Main.directory + File.separator + FILENAME_DATE_FORMATTER.format(modelrun);
        File dayFolder = new File(folder);
        if (!dayFolder.exists()) dayFolder.mkdirs();
        downloadFile(folder, file);
      }
      logger.info(
          "Done. Revisited "
              + failedDownloads.size()
              + " missing files, downloaded "
              + downloadedFiles
              + ".");
      downloadedFiles = 0;

      // download new files
      ZonedDateTime currentModelrun = safeZonedDateTime(newestDateDownloaded).plusHours(3);

      // while the newest possible modelrun is later than or equal to the current one
      // write Files from DWD database to file
      boolean success = true;
      long tic, toc;
      while ((newestPossibleModelrun.isAfter(currentModelrun)
              || newestPossibleModelrun.isEqual(currentModelrun))
          && success) {
        logger.info(
            "################################ "
                + MODEL_RUN_FORMATTER.format(currentModelrun)
                + " ###############################");
        for (Parameter param : Parameter.values()) {
          logger.trace("Downloading " + param.toString());
          tic = System.currentTimeMillis();
          if (!download(currentModelrun, param)) {
            success = false;
          }
          toc = System.currentTimeMillis();
          logger.info(
              param.toString()
                  + " done after "
                  + (toc - tic) / 1000
                  + "s, downloaded "
                  + downloadedFiles
                  + " files.");
          downloadedFiles = 0;
        }
        currentModelrun = currentModelrun.plusHours(3);
      }
    } catch (Exception e) {
      logger.error(e);
    } finally {
      shutdown();
    }
  }

  /** Downloads all timesteps for a given Parameter and modelrun */
  public boolean download(ZonedDateTime modelrun, Parameter param) {
    boolean success = true;
    String folder = Main.directory + File.separator + FILENAME_DATE_FORMATTER.format(modelrun);
    File dayFolder = new File(folder);
    if (!dayFolder.exists()) dayFolder.mkdirs();
    for (int timestep = 0; timestep < Main.timesteps; timestep++) {
      FileModel filemodel =
          dbController.find(FileModel.class, FileModel.createFileName(modelrun, timestep, param));
      if (filemodel == null) {
        filemodel = new FileModel(modelrun, timestep, param);
        filestatusLogger.trace(
            filemodel.getName() + "  |  fmc  |  FileModel created  | Downloader");
        dbController.persist(filemodel);
      }
      if (!downloadFile(folder, filemodel)) success = false;
    }
    return success;
  }

  /**
   * Downloads the given file, using the specified folder
   *
   * @return success of download (reachable URL, sufficient filesize, no errors)
   */
  public boolean downloadFile(String folder, FileModel filemodel) {
    boolean success = false;
    if (!filemodel.isSufficientSize() && filemodel.getDownloadFails() < 3) {
      String url = filemodel.getURL();
      try {
        if (isUrlReachable(url)) {
          File file = new File(folder + File.separator + filemodel.getBZ2FileName());
          if (file.exists()) file.delete();
          file.createNewFile();
          FileUtils.copyURLToFile(new URL(url), file);
          if (file.length() < Parameter.MIN_SIZE) {
            success = false;
            logger.warn("File " + filemodel.getName() + " is too small (" + file.length() + "B)");
          } else {
            filemodel.setSufficientSize(true);
            filestatusLogger.trace(
                file.getName() + "  |  ss   |  sufficient_size = true  | Download success");

            filemodel.setDownloadDate(ZonedDateTime.now());
            filestatusLogger.trace(
                file.getName() + "  |  dd   |  Downloadd_date = now  | Download success");

            filemodel.setArchiveFileDeleted(false);
            filestatusLogger.trace(
                file.getName() + "  |  adf  |  archivefile_deleted = false  | Download success");
            success = true;
            downloadedFiles++;
          }
        }
      } catch (IOException e) {
        success = false;
        logger.error("Could not download " + filemodel.getName() + " (" + e.getMessage() + ")");
      }
      if (!success) {
        filemodel.incrementDownloadFails();
        filestatusLogger.trace(
            filemodel.getName() + "  |  idf  |  incremented download_fails  | failed Download");
      }
      dbController.persist(filemodel);
    } else success = true;
    return success;
  }

  private void shutdown() {
    if (dbController != null) {
      dbController.flush();
      dbController.shutdown();
    }
    logger.info("Downloader shut down");
    logger.info(
        "________________________________________________________________________________\n\n\n");
  }

  /** Validates Connection Properties from user input */
  private Properties validateConnectionProperties() {
    Properties receivedProperties = new Properties();

    if (Main.connectionUrl != null)
      receivedProperties.setProperty("javax.persistence.jdbc.url", Main.connectionUrl);

    if (Main.databaseUser != null)
      receivedProperties.setProperty("javax.persistence.jdbc.user", Main.databaseUser);

    if (Main.databasePassword != null)
      receivedProperties.setProperty("javax.persistence.jdbc.password", Main.databasePassword);

    return receivedProperties;
  }
}
