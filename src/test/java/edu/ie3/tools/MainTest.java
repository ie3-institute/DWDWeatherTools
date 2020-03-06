/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools;

import static org.junit.Assert.assertEquals;

import java.util.TimeZone;
import org.junit.BeforeClass;
import org.junit.Test;

public class MainTest {

  @BeforeClass
  public static void setUp() {
    Main.main(new String[] {});
  }

  @Test
  public void testTimeZone() {
    // ensure that default application time zone is set to UTC @ startup in main method
    assertEquals(TimeZone.getDefault(), TimeZone.getTimeZone("UTC"));
  }
}
