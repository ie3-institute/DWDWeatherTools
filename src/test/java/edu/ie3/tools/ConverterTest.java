/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

import edu.ie3.tools.models.persistence.FileModel;
import edu.ie3.tools.utils.enums.Parameter;
import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

public class ConverterTest {

  @Test
  public void getFormattedTimestep() {
    ZonedDateTime modelrun = ZonedDateTime.of(2019, 7, 3, 3, 14, 0, 0, ZoneId.of("UTC"));
    int timestep = 13;
    assertEquals(
        "MR 03.07.2019 03:14 UTC - TS 13 |",
        Converter.getFormattedTimestep(modelrun, timestep).trim());

    FileModel file = new FileModel(modelrun, timestep, Parameter.ALBEDO);
    assertEquals("MR 03.07.2019 03:14 UTC - TS 13 |", Converter.getFormattedTimestep(file).trim());
  }

  @Test
  public void getFormattedModelrun() {
    ZonedDateTime modelrun = ZonedDateTime.of(2019, 7, 3, 3, 14, 0, 0, ZoneId.of("UTC"));
    assertEquals(
        "MR 03.07.2019 03:14 UTC         |", Converter.getFormattedModelrun(modelrun).trim());
  }
}
