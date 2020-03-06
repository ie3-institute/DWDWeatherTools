/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools.models;

import edu.ie3.tools.models.persistence.CoordinateModel;
import edu.ie3.tools.utils.enums.Parameter;
import java.util.Map;

public class ExtractorResult {

  protected final Map<CoordinateModel, Double> coordinatesToValues;

  protected final Parameter parameter;

  protected final boolean validFile;

  public ExtractorResult(
      Parameter parameter, Map<CoordinateModel, Double> coordinateToValueMap, boolean validFile) {
    this.validFile = validFile;
    this.coordinatesToValues = coordinateToValueMap;
    this.parameter = parameter;
  }

  public Double getValue(CoordinateModel coordinate) {
    return coordinatesToValues.get(coordinate);
  }

  public Map<CoordinateModel, Double> getCoordinatesToValues() {
    return coordinatesToValues;
  }

  public Parameter getParameter() {
    return parameter;
  }

  public boolean isValidFile() {
    return validFile;
  }
}
