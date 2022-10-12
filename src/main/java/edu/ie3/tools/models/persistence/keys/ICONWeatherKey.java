/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools.models.persistence.keys;

import edu.ie3.tools.models.persistence.CoordinateModel;
import java.io.Serializable;
import java.time.ZonedDateTime;

public class ICONWeatherKey implements Serializable {

  private CoordinateModel coordinate;
  private ZonedDateTime time;

  public ICONWeatherKey(CoordinateModel coordinate, ZonedDateTime time) {
    this.coordinate = coordinate;
    this.time = time;
  }

  public ICONWeatherKey() {}

  public CoordinateModel getCoordinate() {
    return coordinate;
  }

  public void setCoordinate(CoordinateModel coordinate) {
    this.coordinate = coordinate;
  }

  public ZonedDateTime getTime() {
    return time;
  }

  public void setTime(ZonedDateTime date) {
    this.time = date;
  }

  @Override
  public String toString() {
    return "ICONWeatherKey{" + "coordinate=" + coordinate + ", time=" + time + '}';
  }
}
