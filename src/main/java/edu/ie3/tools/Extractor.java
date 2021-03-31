/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools;

import edu.ie3.tools.models.ExtractorResult;
import edu.ie3.tools.models.enums.CoordinateType;
import edu.ie3.tools.models.persistence.CoordinateModel;
import edu.ie3.tools.models.persistence.FileModel;
import edu.ie3.tools.utils.enums.Parameter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Extractor implements Callable<ExtractorResult> {
  private static final Logger logger = LogManager.getLogger(Extractor.class);

  private final String formattedTimestep;
  private final String eccodesLocation;
  private final String path;
  private final FileModel file;
  private final Collection<CoordinateModel> coordinates;

  public Extractor(
      String path,
      FileModel file,
      Collection<CoordinateModel> coordinates,
      String ecCodesLocation) {
    logger.setLevel(Main.debug ? Level.ALL : Level.INFO);
    this.path = path;
    this.file = file;
    this.eccodesLocation = ecCodesLocation;
    this.coordinates = coordinates;
    formattedTimestep = Converter.getFormattedTimestep(file);
  }

  private boolean validHeadline(String headlineString) {
    // since eccodes v2.21.0 the data extraction expects a headline w/o commas but with whitespaces
    // see ECC-1197 - https://jira.ecmwf.int/browse/ECC-1197
    String headline = "Latitude Longitude Value";
    String oldHeadline = "Latitude, Longitude, Value";

    return headlineString != null
        && (headlineString.trim().equals(headline) || headlineString.trim().equals(oldHeadline));
  }

  protected HashMap<CoordinateModel, Double> parse(BufferedReader reader) throws IOException {
    HashMap<CoordinateModel, Double> coordinateToValue = new HashMap<>(720729);
    try {
      String line = reader.readLine();
      if (!validHeadline(line)) {
        throw new IOException("Unexpected start of file: " + line);
      }
      while ((line = reader.readLine()) != null) {
        String[] splitArr = line.trim().split("\\s+");
        if (splitArr.length != 3) {
          logger.debug(formattedTimestep + "Line \"" + line + "\" could not be split correctly");
        } else {
          double lat = Double.parseDouble(splitArr[0]);
          double lon = Double.parseDouble(splitArr[1]);
          Double value =
              splitArr[2].equalsIgnoreCase(Main.missingValue)
                  ? null
                  : Double.parseDouble(splitArr[2]);
          coordinateToValue.put(new CoordinateModel(lat, lon, CoordinateType.ICON), value);
        }
      }
      reader.close();
    } catch (EOFException e) {
      logger.warn("Exception during extraction process occurred: " + e);
    }
    return coordinateToValue;
  }

  private HashMap<CoordinateModel, Double> extractParameterViaEccodes(String command)
      throws IOException {
    HashMap<CoordinateModel, Double> coordinateToLevelValue;
    logger.trace("Executing command \"" + command + "\"");
    Process cmdProc = Runtime.getRuntime().exec(command);

    try (BufferedReader stdoutReader =
            new BufferedReader(
                new InputStreamReader(cmdProc.getInputStream(), StandardCharsets.UTF_8));
        BufferedReader stderrReader =
            new BufferedReader(
                new InputStreamReader(cmdProc.getErrorStream(), StandardCharsets.UTF_8))) {
      coordinateToLevelValue = parse(stdoutReader);
      String stderr = stderrReader.lines().collect(Collectors.joining());
      if (!stderr.isEmpty()) logger.error("Error(s) at command execution: \"" + stderr + "\"");
    }

    try {
      int returnVal = cmdProc.waitFor();
      if (returnVal == 0) logger.trace("Command execution returned " + returnVal);
      else logger.error("Command execution returned " + returnVal);
    } catch (InterruptedException e) {
      logger.error(e);
      Thread.currentThread().interrupt();
    }
    return coordinateToLevelValue;
  }

  public String gribGetDataCommand(String filepath, Integer heightlevel) {
    if (heightlevel == null) return gribGetDataCommand(filepath);
    return eccodesLocation
        + " -m "
        + Main.missingValue
        + " -w bottomLevel="
        + heightlevel
        + " "
        + filepath;
  }

  private String gribGetDataCommand(String filepath) {
    return eccodesLocation + " -m " + Main.missingValue + " " + filepath;
  }

  private ExtractorResult extractParameterViaEccodes() throws IOException {
    if (coordinates == null || coordinates.isEmpty())
      throw new IOException("Provided coordinates are not allowed to be null or empty!");
    ExtractorResult parameterLevelValues = extractParameters();
    // update the coordinate Ids which are not included in the raw files but only in our database
    Map<CoordinateModel, Double> parameterLevelValuesMapWithIds =
        updateCoordinatesWithIds(parameterLevelValues.getCoordinatesToValues());

    // return an updated ExtractorResult
    return new ExtractorResult(
        parameterLevelValues.getParameter(),
        parameterLevelValuesMapWithIds,
        parameterLevelValues.isValidFile());
  }

  private ExtractorResult extractParameters() throws IOException {
    Parameter parameter = file.getParameter();
    boolean validFile = true;
    logger.debug(formattedTimestep + "Extracting " + parameter.toString());

    // check if file exists in path
    File f = file.getGRIB22File(path);
    if (!f.exists()) {
      file.setGribfile_deleted(true);
      f = file.getBZ2File(path);
      if (!f.exists()) file.setArchivefile_deleted(true);
      throw new IOException(
          "Could not find file " + file.getName() + " ( " + f.getAbsolutePath() + " )");
    }

    HashMap<CoordinateModel, Double> coordinateToLevelValue = null;
    String command = gribGetDataCommand(path + file.getName() + ".grib2");
    try {
      coordinateToLevelValue = extractParameterViaEccodes(command);
    } catch (IOException e) {
      if (e.getMessage().contains("Cannot run program")) {
        logger.error(
            e
                + ". Are eccodes (https://confluence.ecmwf.int/display/ECC) installed and did you pass the correct path of the eccodes for a custom install location (-eccodes=<path-to-grib_get_data>)? ");
      } else {
        logger.error(e);
        validFile = false;
      }
    }
    if (coordinateToLevelValue == null || coordinateToLevelValue.isEmpty()) validFile = false;
    return new ExtractorResult(parameter, coordinateToLevelValue, validFile);
  }

  private Map<CoordinateModel, Double> updateCoordinatesWithIds(
      Map<CoordinateModel, Double> oldMap) {
    if (oldMap == null) {
      logger.warn(
          "Raw data file extraction for file '"
              + file.getName()
              + "' lead to an empty or null map.");
      return new HashMap<>();
    }
    if (coordinates == null) return oldMap;
    HashMap<CoordinateModel, Double> newMap = new HashMap<>();
    for (CoordinateModel coordinateWithID : coordinates) {
      CoordinateModel coordinateWithoutID =
          new CoordinateModel(
              coordinateWithID.getLatitude(),
              coordinateWithID.getLongitude(),
              coordinateWithID.getCoordinate_type());
      newMap.put(coordinateWithID, oldMap.get(coordinateWithoutID));
    }
    return newMap;
  }

  @Override
  public ExtractorResult call() throws IOException {
    return extractParameterViaEccodes();
  }
}
