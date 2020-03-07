/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools;

import static org.junit.Assert.*;

import edu.ie3.tools.models.ExtractorResult;
import edu.ie3.tools.models.enums.CoordinateType;
import edu.ie3.tools.models.persistence.CoordinateModel;
import edu.ie3.tools.models.persistence.FileModel;
import edu.ie3.tools.utils.enums.Parameter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExtractorTest {

  private static String resourcesPath =
      System.getProperty("user.dir")
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "resources"
          + File.separator;
  private static String extractorPath = resourcesPath + "extractorFiles";

  private final FileModel dummyFileModel = new FileModel(ZonedDateTime.now(), 1, Parameter.ASOB_S);

  @BeforeClass
  public static void setUp() throws IOException {
    System.out.println(resourcesPath);
    System.out.println(new File(
                    resourcesPath
                    + "testFiles"
                    + File.separator
                    + "icon-eu_europe_regular-lat-lon_model-level_2018031417_042_60_U.grib2").exists());
    File extractorFiles = new File(extractorPath);
    extractorFiles.mkdirs();
    FileUtils.copyFile(
        new File(
            resourcesPath
                + "testFiles"
                + File.separator
                + "icon-eu_europe_regular-lat-lon_model-level_2018031417_042_60_U.grib2"),
        new File(
            extractorFiles
                + File.separator
                + "icon-eu_europe_regular-lat-lon_model-level_2018031417_042_60_U.grib2"));

    FileUtils.copyFile(
        new File(
            resourcesPath
                + "testFiles"
                + File.separator
                + "icon-eu_europe_regular-lat-lon_single-level_2018031417_042_U_10M.grib2"),
        new File(
            extractorFiles
                + File.separator
                + "icon-eu_europe_regular-lat-lon_single-level_2018031417_042_U_10M.grib2"));

    FileUtils.copyFile(
        new File(resourcesPath + "testFiles" + File.separator + "gribTextFileWithoutHeader.txt"),
        new File(extractorFiles + File.separator + "gribTextFileWithoutHeader.txt"));

    FileUtils.copyFile(
        new File(resourcesPath + "testFiles" + File.separator + "unsplittableGribTextFile.txt"),
        new File(extractorFiles + File.separator + "unsplittableGribTextFile.txt"));

    FileUtils.copyFile(
        new File(resourcesPath + "testFiles" + File.separator + "validGribTextFile.txt"),
        new File(extractorFiles + File.separator + "validGribTextFile.txt"));
  }

  @Test
  public void testValidParse() throws IOException {
    System.out.println("ExtractorTest.testValidParse---------------------------");
    String path = extractorPath + "/validGribTextFile.txt";
    InputStreamReader input =
        new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8);
    BufferedReader bufferedReader = new BufferedReader(input);
    Map<CoordinateModel, Double> result =
        new Extractor(path, dummyFileModel, Collections.emptyList(), "").parse(bufferedReader);

    assertEquals(1098, result.size());

    Set<Double> latitudes =
        result.keySet().stream().map(CoordinateModel::getLatitude).collect(Collectors.toSet());
    assertEquals(2, latitudes.size());
    assertTrue(latitudes.contains(29.5));
    assertTrue(latitudes.contains(29.562));

    Set<Double> longitudes =
        result.keySet().stream().map(CoordinateModel::getLongitude).collect(Collectors.toSet());
    assertEquals(1097, longitudes.size());

    CoordinateModel coordinate = new CoordinateModel(29.5, -23.5, CoordinateType.ICON);
    assertEquals(1.2936019897e-01, result.get(coordinate), 0);
    coordinate = new CoordinateModel(29.562, -23.5, CoordinateType.ICON);
    assertEquals(2.5045394897e-01, result.get(coordinate), 0);
  }

  @Test(expected = IOException.class)
  public void testParseWithoutHeader() throws IOException {
    System.out.println("ExtractorTest.testParseWithoutHeader-------------------");
    String path = extractorPath + File.separator + "gribTextFileWithoutHeader.txt";
    InputStreamReader input =
        new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8);
    BufferedReader bufferedReader = new BufferedReader(input);
    new Extractor(path, dummyFileModel, Collections.emptyList(), "").parse(bufferedReader);
  }

  @Test
  public void testParseWithUnsplittableLines() throws IOException {
    System.out.println("ExtractorTest.testParseWithUnsplittableLines-----------");
    String path = extractorPath + File.separator + "unsplittableGribTextFile.txt";
    InputStreamReader input =
        new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8);
    BufferedReader bufferedReader = new BufferedReader(input);
    Map<CoordinateModel, Double> result =
        new Extractor(path, dummyFileModel, Collections.emptyList(), "").parse(bufferedReader);
    assertEquals(2, result.size());

    CoordinateModel coordinate = new CoordinateModel(29.5, -18.375, CoordinateType.ICON);
    assertEquals(-1.5698585510e+00, result.get(coordinate), 0);
    coordinate = new CoordinateModel(29.5, -23.125, CoordinateType.ICON);
    assertEquals(4.1061019897e-01, result.get(coordinate), 0);
  }

  @Test
  public void testExtract() throws IOException {
    System.out.println("ExtractorTest.testExtract------------------------------");
    Main.directory = extractorPath;
    FileModel file =
        new FileModel(
            ZonedDateTime.of(LocalDateTime.of(2018, 3, 14, 17, 0), ZoneId.of("UTC")),
            42,
            Parameter.U_20M);

    List<CoordinateModel> coordinatesWithIds = new ArrayList<>();
    // read the coordinates file
    String pathToCoordinatesCsv =
        resourcesPath + File.separator + "sql" + File.separator + "coordinates.csv";
    BufferedReader csvReader = new BufferedReader(new FileReader(pathToCoordinatesCsv));
    String row;
    csvReader.readLine(); // jump over headline
    while ((row = csvReader.readLine()) != null) {
      String[] data = row.split(",");
      double latitude = Double.parseDouble(data[1]);
      double longitude = Double.parseDouble(data[2]);
      Integer id = Integer.parseInt(data[0]);
      CoordinateModel coordinateModel = new CoordinateModel(latitude, longitude);
      coordinateModel.setId(id);
      coordinateModel.setCoordinate_type(CoordinateType.ICON);
      coordinatesWithIds.add(coordinateModel);
    }
    csvReader.close();

    ExtractorResult parameterValues = null;
    try {
      parameterValues =
          new Extractor(extractorPath + File.separator, file, coordinatesWithIds, "grib_get_data")
              .call();
    } catch (IOException e) {
      fail(
          "Unable to use Extractor to get data "
              + file.getName()
              + " from path "
              + extractorPath
              + File.separator
              + ". Exception that has been thrown: "
              + e);
    }
    assertEquals(Parameter.U_20M, parameterValues.getParameter());

    assertEquals(720729, parameterValues.getCoordinatesToValues().size());

    CoordinateModel coordinate = new CoordinateModel(29.5, -23.5, CoordinateType.ICON);
    coordinate.setId(473694);
    Double value = parameterValues.getValue(coordinate);
    assertNotNull(value);
    assertEquals(1.3074111938e-01, value, 0);

    file =
        new FileModel(
            ZonedDateTime.of(LocalDateTime.of(2018, 3, 14, 17, 0), ZoneId.of("UTC")),
            42,
            Parameter.U_10M);

    try {
      parameterValues =
          new Extractor(extractorPath + File.separator, file, coordinatesWithIds, "grib_get_data")
              .call();
    } catch (IOException e) {
      fail(
          "Unable to use Extractor to get data "
              + file.getName()
              + " from path "
              + extractorPath
              + File.separator);
    }
    assertEquals(Parameter.U_10M, parameterValues.getParameter());

    value = parameterValues.getValue(coordinate);
    assertEquals(1.3074111938e-01, value, 0);
  }

  @AfterClass
  public static void tearDown() throws IOException {
    FileUtils.deleteDirectory(new File(extractorPath));
  }
}
