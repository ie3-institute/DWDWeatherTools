/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools.models.persistence;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.*;

@MappedSuperclass
public abstract class AbstractCoordinateModel implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(nullable = false)
  private Integer id;

  @Column(nullable = false)
  private double latitude;

  @Column(nullable = false)
  private double longitude;

  public AbstractCoordinateModel(Integer id) {
    this.id = id;
  }

  public AbstractCoordinateModel(double latitude, double longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public AbstractCoordinateModel(Integer id, double latitude, double longitude) {
    this(id);
    this.latitude = latitude;
    this.longitude = longitude;
  }

  /** Only needed for JPA use */
  protected AbstractCoordinateModel() {}

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AbstractCoordinateModel that = (AbstractCoordinateModel) o;
    if (!Objects.equals(that.id, id)) return false;
    return hasEqualLatLon(that);
  }

  public boolean hasEqualLatLon(AbstractCoordinateModel that) {
    return Double.compare(that.latitude, latitude) == 0
        && Double.compare(that.longitude, longitude) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(latitude, longitude);
  }

  @Override
  public String toString() {
    return "AbstractCoordinateModel{"
        + "id="
        + id
        + ", latitude="
        + latitude
        + ", longitude="
        + longitude
        + '}';
  }
}
