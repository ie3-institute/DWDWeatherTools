/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3;

import edu.ie3.tools.ConverterTest;
import edu.ie3.tools.DecompressorTest;
import edu.ie3.tools.ExtractorTest;
import edu.ie3.tools.models.persistence.ICONWeatherModelTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
  DecompressorTest.class,
  ExtractorTest.class,
  ConverterTest.class,
  ICONWeatherModelTest.class
})
public class AllTests {
  // define all test cases in @SuiteClasses
}
