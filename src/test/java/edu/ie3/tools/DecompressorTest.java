/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools;

import static junit.framework.TestCase.assertTrue;

import edu.ie3.tools.models.persistence.FileModel;
import edu.ie3.tools.utils.enums.Parameter;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DecompressorTest {

  private static String resourcesPath =
      System.getProperty("user.dir")
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "resources"
          + File.separator;
  private static String decompresserFilesPath = resourcesPath + "decompressorFiles";

  @BeforeClass
  public static void before() throws IOException {
    File from =
        new File(
            resourcesPath
                + "testFiles"
                + File.separator
                + "_icon-eu_europe_regular-lat-lon_model-level_2018112509_007_57_U.grib2.bz2");
    File to =
        new File(
            decompresserFilesPath
                + File.separator
                + "icon-eu_europe_regular-lat-lon_model-level_2018112509_007_57_U.grib2.bz2");
    FileUtils.copyFile(from, to);
    to.deleteOnExit();
  }

  @Test
  public void testDecompression() {
    System.out.println("DecompressorTest.testDecompression---------------------");
    FileModel file =
        new FileModel(
            ZonedDateTime.of(LocalDateTime.of(2018, 11, 25, 9, 0), ZoneId.of("UTC")),
            7,
            Parameter.U_216M);
    Decompressor.decompress(
        file, decompresserFilesPath + File.separator, decompresserFilesPath + File.separator);
    File gribFile =
        new File(
            decompresserFilesPath
                + File.separator
                + "icon-eu_europe_regular-lat-lon_model-level_2018112509_007_57_U.grib2");
    assertTrue(gribFile.exists());
    assertTrue(gribFile.getTotalSpace() > 128);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    File gribFile =
        new File(
            decompresserFilesPath
                + File.separator
                + "icon-eu_europe_regular-lat-lon_model-level_2018112509_007_57_U.grib2");
    if (gribFile.exists()) gribFile.delete();
    File bz2File =
        new File(
            decompresserFilesPath
                + File.separator
                + "icon-eu_europe_regular-lat-lon_model-level_2018112509_007_57_U.grib2.bz2");
    if (bz2File.exists()) gribFile.delete();
  }
}
