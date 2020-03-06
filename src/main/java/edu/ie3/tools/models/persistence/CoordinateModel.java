/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools.models.persistence;

import edu.ie3.tools.models.enums.CoordinateType;
import javax.persistence.*;

@NamedQueries({
  @NamedQuery(
      name = "CoordinateModel.COSMO_Coordinates",
      query =
          "SELECT coordinate FROM icon_coordinates coordinate "
              + "WHERE coordinate.coordinate_type = edu.ie3.tools.models.enums.CoordinateType.COSMO"),
  @NamedQuery(
      name = "CoordinateModel.ICON_Coordinates",
      query =
          "SELECT coordinate FROM icon_coordinates coordinate "
              + "WHERE coordinate.coordinate_type = edu.ie3.tools.models.enums.CoordinateType.ICON"),
  @NamedQuery(
      name = "CoordinateModel.CoordinatesInRectangle",
      query =
          "SELECT coordinate FROM icon_coordinates coordinate "
              + "WHERE coordinate.coordinate_type = edu.ie3.tools.models.enums.CoordinateType.ICON "
              + "AND latitude BETWEEN :minLatitude AND :maxLatitude "
              + "AND longitude BETWEEN :minLongitude AND :maxLongitude"),
})
@Entity(name = "icon_coordinates")
public class CoordinateModel extends edu.ie3.tools.models.persistence.AbstractCoordinateModel {

  public static final String COSMO_Coordinates = "CoordinateModel.COSMO_Coordinates";
  public static final String ICON_Coordinates = "CoordinateModel.ICON_Coordinates";
  public static final String CoordinatesInRectangle = "CoordinateModel.CoordinatesInRectangle";

  @Column
  @Enumerated(EnumType.STRING)
  private CoordinateType coordinate_type = CoordinateType.UNDEFINED;

  public CoordinateModel(int id) {
    super(id);
  }

  public CoordinateModel(double latitude, double longitude) {
    super(latitude, longitude);
  }

  public CoordinateModel(double latitude, double longitude, CoordinateType coordinate_type) {
    this(latitude, longitude);
    this.coordinate_type = coordinate_type;
  }

  protected CoordinateModel() {
    super();
  }

  public CoordinateType getCoordinate_type() {
    return coordinate_type;
  }

  public void setCoordinate_type(CoordinateType coordinate_type) {
    this.coordinate_type = coordinate_type;
  }
}
