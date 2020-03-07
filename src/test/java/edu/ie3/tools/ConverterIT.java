/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools;

import static org.junit.Assert.*;

import edu.ie3.tools.models.persistence.CoordinateModel;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

public class ConverterIT {

  static final String resourcesPath =
      System.getProperty("user.dir")
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "resources"
          + File.separator;

  static final int numberOfCoordinates = 44885;

  // Coordinates with min, max and average Lat/Lon values, which will be our sample coords
  static final CoordinateModel minCoordinate =
      new CoordinateModel(227670); // lat, lon: 45.75, 4.312
  static final CoordinateModel maxCoordinate =
      new CoordinateModel(304736); // lat, lon: 57.625,18.938
  static final CoordinateModel avgCoordinate =
      new CoordinateModel(216196); // lat, lon: 51.688, 11.625

  private static final Map<CoordinateModel, Double> U20Mmap;
  private static final Map<CoordinateModel, Double> U10Mmap;
  private static final Map<CoordinateModel, Double> Z0_00_03Mmap;
  private static final Map<CoordinateModel, Double> Z0_03_00Mmap;
  private static final Map<CoordinateModel, Double> Z0_03_01Mmap;

  // Initialize Maps with manually read values from the corresponding files
  // (also documented in resources/test_values_overview.txt)
  static {
    Map<CoordinateModel, Double> aMap = new HashMap<>();
    aMap.put(minCoordinate, 2.7225494385e-02);
    aMap.put(maxCoordinate, 7.0867958069e+00);
    aMap.put(avgCoordinate, 9.8132705688e-01);
    U20Mmap = Collections.unmodifiableMap(aMap);
    U10Mmap = Collections.unmodifiableMap(aMap); // same values as it is the same file

    aMap = new HashMap<>();
    aMap.put(minCoordinate, 1.0629452213e-01);
    aMap.put(maxCoordinate, 7.8091463365e-05);
    aMap.put(avgCoordinate, 8.4794888338e-02);
    Z0_00_03Mmap = Collections.unmodifiableMap(aMap);

    aMap = new HashMap<>();
    aMap.put(minCoordinate, 1.0629452213e-01);
    aMap.put(maxCoordinate, 1.0860904149e-04);
    aMap.put(avgCoordinate, 8.4794888338e-02);
    Z0_03_00Mmap = Collections.unmodifiableMap(aMap);
    Z0_03_01Mmap =
        Collections.unmodifiableMap(
            aMap); // same values as weather does not seem to change that fast
  }

  @ClassRule
  public static final PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:11.4-alpine").withDatabaseName("test");

  @BeforeClass
  public static void setUp() throws Exception {
    Main.connectionUrl = postgres.getJdbcUrl();
    Main.databaseUser = postgres.getUsername();
    Main.databasePassword = postgres.getPassword();
    Main.debug = true;
    Main.deleteDownloadedFiles = true;

    setDatabaseDockerContainerUp(Main.connectionUrl, Main.databaseUser, Main.databasePassword);
    setFilesUp();

    Converter converter = new Converter();
    converter.run();
  }

  public static void setDatabaseDockerContainerUp(
      String connectionUrl, String databaseUser, String databasePassword)
      throws SQLException, IOException {
    postgres.start();
    System.out.println("Connected to Docker Container: " + connectionUrl);
    File file = new File(resourcesPath + "sql" + File.separator + "initDatabase.sql");
    String initDatabase = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    Connection connect = DriverManager.getConnection(connectionUrl, databaseUser, databasePassword);
    Statement statement = connect.createStatement();
    statement.execute(initDatabase);
    statement.closeOnCompletion();

    postgres.copyFileToContainer(
        MountableFile.forHostPath(resourcesPath + "sql" + File.separator + "coordinates.csv"),
        "/coordinates.csv");
    statement = connect.createStatement();
    statement.execute(
        "COPY icon.icon_coordinates \n" + "FROM '/coordinates.csv' DELIMITER ',' CSV HEADER;");
    statement.closeOnCompletion();
    connect.close();

    System.out.println("Postgres Container");
  }

  public static void setFilesUp() throws IOException {
    Main.directory = resourcesPath + "downloads";
    String modelrunDirectoryPath = Main.directory + File.separator + "2019082300" + File.separator;
    File modelrunDirectory = new File(modelrunDirectoryPath);
    modelrunDirectory.mkdirs();

    FileUtils.copyFile(
        new File(
            resourcesPath
                + "testFiles"
                + File.separator
                + "icon-eu_europe_regular-lat-lon_model-level_2018031417_042_60_U.grib2.bz2"),
        new File(
            modelrunDirectoryPath
                + "icon-eu_europe_regular-lat-lon_model-level_2019082300_001_60_U.grib2.bz2")); // !!! new name !!!

    FileUtils.copyFile(
        new File(
            resourcesPath
                + "testFiles"
                + File.separator
                + "icon-eu_europe_regular-lat-lon_single-level_2018031417_042_U_10M.grib2.bz2"),
        new File(
            modelrunDirectoryPath
                + "icon-eu_europe_regular-lat-lon_single-level_2019082300_001_U_10M.grib2.bz2")); // !!! new name !!!

    FileUtils.copyFile(
        new File(
            resourcesPath
                + "testFiles"
                + File.separator
                + "icon-eu_europe_regular-lat-lon_single-level_2019082300_003_Z0.grib2.bz2"),
        new File(
            modelrunDirectoryPath
                + "icon-eu_europe_regular-lat-lon_single-level_2019082300_003_Z0.grib2.bz2"));

    modelrunDirectoryPath = Main.directory + File.separator + "2019082303" + File.separator;
    FileUtils.copyFile(
        new File(
            resourcesPath
                + "testFiles"
                + File.separator
                + "icon-eu_europe_regular-lat-lon_single-level_2019082303_000_Z0.grib2.bz2"),
        new File(
            modelrunDirectoryPath
                + "icon-eu_europe_regular-lat-lon_single-level_2019082303_000_Z0.grib2.bz2"));

    FileUtils.copyFile(
        new File(
            resourcesPath
                + "testFiles"
                + File.separator
                + "icon-eu_europe_regular-lat-lon_single-level_2019082303_001_Z0.grib2.bz2"),
        new File(
            modelrunDirectoryPath
                + "icon-eu_europe_regular-lat-lon_single-level_2019082303_001_Z0.grib2.bz2"));
  }

  @AfterClass
  public static void tearDown() throws IOException {
    boolean setBreakpointHereForContainerInspection;
    Main.directory = resourcesPath + "downloads";
    FileUtils.deleteDirectory(new File(Main.directory));
    postgres.stop();
  }

  @Test
  public void testAmountOfData() {
    try {
      String dateString = "2019-08-23 ";
      Connection connect =
          DriverManager.getConnection(Main.connectionUrl, Main.databaseUser, Main.databasePassword);

      Statement statement = connect.createStatement();
      ResultSet rs = statement.executeQuery("SELECT count(*) AS cnt FROM icon.weather;");
      statement.closeOnCompletion();
      rs.next();
      int numberOfRows = rs.getInt("cnt");
      assertEquals(numberOfCoordinates * 3, numberOfRows);

      statement = connect.createStatement();
      rs = statement.executeQuery("SELECT DISTINCT(datum) AS datum FROM icon.weather;");
      statement.closeOnCompletion();
      Set<String> dates = new HashSet<>();
      while (rs.next()) {
        dates.add(rs.getTimestamp("datum").toString());
      }
      // contains three dates, as two date points should overlap
      // (Modelrun 3 + Timestep 0 == Modelrun 0 + Timestep 3 == 3 o' Clock)
      assertTrue(dates.contains(dateString + "01:00:00.0"));
      assertTrue(dates.contains(dateString + "03:00:00.0"));
      assertTrue(dates.contains(dateString + "04:00:00.0"));

      connect.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testQualityOfData() {
    try {
      String dateString = "2019-08-23 ";
      Connection connect =
          DriverManager.getConnection(Main.connectionUrl, Main.databaseUser, Main.databasePassword);

      for (Map.Entry<CoordinateModel, Double> entry : U20Mmap.entrySet()) {
        Statement statement = connect.createStatement();
        ResultSet rs =
            statement.executeQuery(
                getValueQuery(entry.getKey().getId(), dateString + "01:00", "u_20m"));
        statement.closeOnCompletion();
        rs.next();
        Double value = rs.getDouble("value");
        assertEquals(entry.getValue(), value);
      }

      for (Map.Entry<CoordinateModel, Double> entry : U10Mmap.entrySet()) {
        Statement statement = connect.createStatement();
        ResultSet rs =
            statement.executeQuery(
                getValueQuery(entry.getKey().getId(), dateString + "01:00", "u_10m"));
        statement.closeOnCompletion();
        rs.next();
        Double value = rs.getDouble("value");
        assertEquals(entry.getValue(), value);
      }

      for (Map.Entry<CoordinateModel, Double> entry : Z0_03_01Mmap.entrySet()) {
        Statement statement = connect.createStatement();
        ResultSet rs =
            statement.executeQuery(
                getValueQuery(entry.getKey().getId(), dateString + "04:00", "z0"));
        statement.closeOnCompletion();
        rs.next();
        Double value = rs.getDouble("value");
        assertEquals(entry.getValue(), value);
      }

      // These values should be interpolated, as there were two files for the same timestamp
      for (Map.Entry<CoordinateModel, Double> entry : Z0_03_00Mmap.entrySet()) {
        Statement statement = connect.createStatement();
        ResultSet rs =
            statement.executeQuery(
                getValueQuery(entry.getKey().getId(), dateString + "03:00", "z0"));
        statement.closeOnCompletion();
        rs.next();
        Double fetchedValue = rs.getDouble("value");
        Double earlierValue = Z0_00_03Mmap.get(entry.getKey());
        Double laterValue = entry.getValue();
        Double interpolatedValue =
            earlierValue * (1 - Main.interpolationRatio) + laterValue * Main.interpolationRatio;
        assertEquals(interpolatedValue, fetchedValue);
      }

      connect.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Test
  // Converter expects a file based on the information in the database, but should not find it
  public void testNonexistentFile() {
    try {
      Connection connect =
          DriverManager.getConnection(Main.connectionUrl, Main.databaseUser, Main.databasePassword);
      Statement statement = connect.createStatement();
      ResultSet rs = statement.executeQuery("SELECT * FROM icon.files WHERE parameter = 'ASOB_S';");
      statement.closeOnCompletion();
      while (rs.next()) {
        assertFalse(rs.getBoolean("decompressed"));
        assertFalse(rs.getBoolean("valid_file"));
        assertFalse(rs.getBoolean("persisted"));
        assertTrue(rs.getBoolean("archivefile_deleted"));
      }

      statement = connect.createStatement();
      rs = statement.executeQuery("SELECT DISTINCT(asob_s) AS asob_s FROM icon.weather;");
      statement.closeOnCompletion();
      rs.next();
      assertNull(rs.getObject("asob_s"));
      assertFalse(rs.next());

      connect.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testFileModelStatus() {
    try {
      Connection connect =
          DriverManager.getConnection(Main.connectionUrl, Main.databaseUser, Main.databasePassword);
      Statement statement = connect.createStatement();
      ResultSet rs =
          statement.executeQuery("SELECT * from icon.files WHERE NOT parameter = 'ASOB_S';");
      statement.closeOnCompletion();
      while (rs.next()) {
        assertTrue(rs.getBoolean("sufficient_size"));
        assertFalse(rs.getBoolean("decompressed"));
        assertEquals(0, rs.getInt("missing_coordinates"));
        assertTrue(rs.getBoolean("valid_file"));
        assertTrue(rs.getBoolean("persisted"));
        assertTrue(rs.getBoolean("archivefile_deleted"));
        assertTrue(rs.getBoolean("gribfile_deleted"));
      }
      connect.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testIfFilesAreDeleted() {
    File dir = new File(Main.directory + File.separator + "2019082300");
    assertEquals(0, dir.listFiles().length);
    dir = new File(Main.directory + File.separator + "2019082303");
    assertEquals(0, dir.listFiles().length);
  }

  private String getValueQuery(int coord_id, String dateString, String param) {
    String query =
        "SELECT "
            + param
            + " as value from icon.weather where coordinate_id = "
            + coord_id
            + " and datum = '"
            + dateString
            + "';";
    return query;
  }
}
