/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools.models.persistence;

import edu.ie3.tools.models.persistence.keys.ICONWeatherKey;
import edu.ie3.tools.utils.ConfigurationParameters;
import edu.ie3.tools.utils.enums.Parameter;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Objects;
import javax.persistence.*;

@Entity(name = "weather")
@IdClass(ICONWeatherKey.class)
public class ICONWeatherModel implements Serializable {
  private static final long serialVersionUID = -3506091597737302833L;

  @Id
  @Column(name = "datum", nullable = false)
  private ZonedDateTime date;

  @Id
  @ManyToOne(cascade = CascadeType.DETACH)
  private CoordinateModel coordinate;

  /** Albedo in % */
  @Column(name = "alb_rad")
  private Double alb_rad;

  /** Net short-wave radiation flux at surface in W/m² */
  @Column(name = "asob_s")
  private Double asob_s;

  /** Surface down solar diffuse radiation in W/m² */
  @Column(name = "aswdifd_s")
  private Double aswdifd_s;

  /** Surface up diffuse radiation in W/m² */
  @Column(name = "aswdifu_s")
  private Double aswdifu_s;

  /** Direct radiation in W/m² */
  @Column(name = "aswdir_s")
  private Double aswdirs_s;

  /** Net short-wave radiation flux at surface (instantaneous) in W/m² */
  @Column(name = "sobs_rad")
  private Double sobs_rad;

  /** Temperature at 131m above ground in K */
  @Column(name = "t_131m")
  private Double t_131m;

  /** Temperature at 2m above ground in K */
  @Column(name = "t_2m")
  private Double t_2m;

  /** Ground temperature in K */
  @Column(name = "t_g")
  private Double t_g;

  /** Zonal wind at 10m above ground in m/s */
  @Column(name = "u_10m")
  private Double u_10m;

  /** Meridional wind at 10m above ground in m/s */
  @Column(name = "v_10m")
  private Double v_10m;

  /** Surface roughness in m */
  @Column(name = "z0")
  private Double z0;

  /** Pressure at 20m above ground in Pa */
  @Column(name = "p_20m")
  private Double p_20m;

  /** Pressure at 65m above ground in Pa */
  @Column(name = "p_65m")
  private Double p_65m;

  /** Pressure at 131m above ground in Pa */
  @Column(name = "p_131m")
  private Double p_131m;

  /** Zonal wind at 20m above ground in m/s */
  @Column(name = "u_20m")
  private Double u_20m;

  /** Meridional wind at 20m above ground in m/s */
  @Column(name = "v_20m")
  private Double v_20m;

  /** Vertical wind at 20m above ground in m/s */
  @Column(name = "w_20m")
  private Double w_20m;

  /** Zonal wind at 65m above ground in m/s */
  @Column(name = "u_65m")
  private Double u_65m;

  /** Meridional wind at 65m above ground in m/s */
  @Column(name = "v_65m")
  private Double v_65m;

  /** Vertical wind at 65m above ground in m/s */
  @Column(name = "w_65m")
  private Double w_65m;

  /** Zonal wind at 131m above ground in m/s */
  @Column(name = "u_131m")
  private Double u_131m;

  /** Meridional wind at 131m above ground in m/s */
  @Column(name = "v_131m")
  private Double v_131m;

  /** Vertical wind at 131m above ground in m/s */
  @Column(name = "w_131m")
  private Double w_131m;

  /** Zonal wind at 216m above ground in m/s */
  @Column(name = "u_216m")
  private Double u_216m;

  /** Meridional wind at 216m above ground in m/s */
  @Column(name = "v_216m")
  private Double v_216m;

  /** Vertical wind at 216m above ground in m/s */
  @Column(name = "w_216m")
  private Double w_216m;

  public ICONWeatherModel() {}

  public ICONWeatherModel(ZonedDateTime date, CoordinateModel coordinate) {
    this.date = date;
    this.coordinate = coordinate;
  }

  // new Model
  public static ICONWeatherModel getInterpolatedEntity(ICONWeatherModel... entities) {
    ICONWeatherModel interpolatedEntity = new ICONWeatherModel();
    for (Parameter param : Parameter.values()) {
      int numberOfValues = 0;
      double paramValue = 0.0;
      for (ICONWeatherModel entity : entities) {
        Double val;
        if ((val = entity.getParameter(param)) != null) {
          numberOfValues++;
          paramValue += val;
        }
      }
      if (numberOfValues > 0) interpolatedEntity.setParameter(param, paramValue / numberOfValues);
    }
    return interpolatedEntity;
  }

  public ICONWeatherKey getKey() {
    return new ICONWeatherKey(coordinate, date);
  }

  public static String getSQLUpsertStatement(Collection<ICONWeatherModel> entities) {
    return getSQLUpsertStatement(entities, "public");
  }

  public static String getSQLUpsertStatement(
      Collection<ICONWeatherModel> entities, String database_schema) {
    StringBuilder upsertStatementBuilder = new StringBuilder();
    upsertStatementBuilder.append(
        "INSERT INTO "
            + database_schema
            + ".weather(\n"
            + "\tdatum, alb_rad, asob_s, aswdifd_s, aswdifu_s, aswdir_s, sobs_rad, p_20m, p_65m, p_131m, t_131m, t_2m, t_g, u_10m, u_131m, u_20m, u_216m, u_65m, v_10m, v_131m, v_20m, v_216m, v_65m, w_131m, w_20m, w_216m, w_65m, z0, coordinate_id)\n"
            + "\t VALUES ");
    entities.forEach(
        entity -> upsertStatementBuilder.append(entity.getSQLInsertValuesString() + ", "));
    int lastComma = upsertStatementBuilder.lastIndexOf(",");
    upsertStatementBuilder.deleteCharAt(lastComma);
    upsertStatementBuilder.append("ON CONFLICT (coordinate_id, datum) DO UPDATE \n" + "  SET ");
    upsertStatementBuilder.append(
        "datum=excluded.datum,\n"
            + " alb_rad=excluded.alb_rad,\n"
            + " asob_s=excluded.asob_s,\n"
            + " aswdifd_s=excluded.aswdifd_s,\n"
            + " aswdifu_s=excluded.aswdifu_s,\n"
            + " aswdir_s=excluded.aswdir_s,\n"
            + " sobs_rad=excluded.sobs_rad,\n"
            + " p_20m=excluded.p_20m,\n"
            + " p_65m=excluded.p_65m,\n"
            + " p_131m=excluded.p_131m,\n"
            + " t_131m=excluded.t_131m,\n"
            + " t_2m=excluded.t_2m,\n"
            + " t_g=excluded.t_g,\n"
            + " u_10m=excluded.u_10m,\n"
            + " u_131m=excluded.u_131m,\n"
            + " u_20m=excluded.u_20m,\n"
            + " u_216m=excluded.u_216m,\n"
            + " u_65m=excluded.u_65m,\n"
            + " v_10m=excluded.v_10m,\n"
            + " v_131m=excluded.v_131m,\n"
            + " v_20m=excluded.v_20m,\n"
            + " v_216m=excluded.v_216m,\n"
            + " v_65m=excluded.v_65m,\n"
            + " w_131m=excluded.w_131m,\n"
            + " w_20m=excluded.w_20m,\n"
            + " w_216m=excluded.w_216m,\n"
            + " w_65m=excluded.w_65m,\n"
            + " z0=excluded.z0,\n"
            + " coordinate_id=excluded.coordinate_id;");
    return upsertStatementBuilder.toString();
  }

  public ZonedDateTime getDate() {
    return date;
  }

  public void setDate(ZonedDateTime date) {
    this.date = date;
  }

  public Double getAlb_rad() {
    return alb_rad;
  }

  public void setAlb_rad(Double alb_rad) {
    this.alb_rad = alb_rad;
  }

  public Double getAsob_s() {
    return asob_s;
  }

  public void setAsob_s(Double asob_s) {
    this.asob_s = asob_s;
  }

  public Double getAswdifd_s() {
    return aswdifd_s;
  }

  public void setAswdifd_s(Double aswdifd_s) {
    this.aswdifd_s = aswdifd_s;
  }

  public Double getAswdifu_s() {
    return aswdifu_s;
  }

  public void setAswdifu_s(Double aswdifu_s) {
    this.aswdifu_s = aswdifu_s;
  }

  public Double getAswdirs_s() {
    return aswdirs_s;
  }

  public void setAswdirs_s(Double aswdirs_s) {
    this.aswdirs_s = aswdirs_s;
  }

  public Double getT_2m() {
    return t_2m;
  }

  public void setT_2m(Double t_2m) {
    this.t_2m = t_2m;
  }

  public Double getT_g() {
    return t_g;
  }

  public void setT_g(Double t_g) {
    this.t_g = t_g;
  }

  public Double getU_10m() {
    return u_10m;
  }

  public void setU_10m(Double u_10m) {
    this.u_10m = u_10m;
  }

  public Double getV_10m() {
    return v_10m;
  }

  public void setV_10m(Double v_10m) {
    this.v_10m = v_10m;
  }

  public Double getZ0() {
    return z0;
  }

  public void setZ0(Double z0) {
    this.z0 = z0;
  }

  public Double getU_20m() {
    return u_20m;
  }

  public void setU_20m(Double u_20m) {
    this.u_20m = u_20m;
  }

  public Double getV_20m() {
    return v_20m;
  }

  public void setV_20m(Double v_20m) {
    this.v_20m = v_20m;
  }

  public Double getW_20m() {
    return w_20m;
  }

  public void setW_20m(Double w_20m) {
    this.w_20m = w_20m;
  }

  public Double getU_65m() {
    return u_65m;
  }

  public void setU_65m(Double u_65m) {
    this.u_65m = u_65m;
  }

  public Double getV_65m() {
    return v_65m;
  }

  public void setV_65m(Double v_65m) {
    this.v_65m = v_65m;
  }

  public Double getW_65m() {
    return w_65m;
  }

  public void setW_65m(Double w_65m) {
    this.w_65m = w_65m;
  }

  public Double getU_216m() {
    return u_216m;
  }

  public void setU_216m(Double u_216m) {
    this.u_216m = u_216m;
  }

  public Double getV_216m() {
    return v_216m;
  }

  public void setV_216m(Double v_216m) {
    this.v_216m = v_216m;
  }

  public Double getW_216m() {
    return w_216m;
  }

  public void setW_216m(Double w_216m) {
    this.w_216m = w_216m;
  }

  public Double getU_131m() {
    return u_131m;
  }

  public void setU_131m(Double u_131m) {
    this.u_131m = u_131m;
  }

  public Double getV_131m() {
    return v_131m;
  }

  public void setV_131m(Double v_131m) {
    this.v_131m = v_131m;
  }

  public Double getW_131m() {
    return w_131m;
  }

  public void setW_131m(Double w_131m) {
    this.w_131m = w_131m;
  }

  public Double getSobs_rad() {
    return sobs_rad;
  }

  public void setSobs_rad(Double sobs_rad) {
    this.sobs_rad = sobs_rad;
  }

  public Double getT_131m() {
    return t_131m;
  }

  public void setT_131m(Double t_131m) {
    this.t_131m = t_131m;
  }

  public Double getP_20m() {
    return p_20m;
  }

  public void setP_20m(Double p_20m) {
    this.p_20m = p_20m;
  }

  public Double getP_65m() {
    return p_65m;
  }

  public void setP_65m(Double p_65m) {
    this.p_65m = p_65m;
  }

  public Double getP_131m() {
    return p_131m;
  }

  public void setP_131m(Double p_131m) {
    this.p_131m = p_131m;
  }

  public void setParameter(Parameter param, Double value) {
    if (value == null) return;
    switch (param) {
        // Single Level Params
      case ALBEDO:
        alb_rad = value;
        break;
      case ASOB_S:
        asob_s = value;
        break;
      case DIFS_D:
        aswdifd_s = value;
        break;
      case DIFS_U:
        aswdifu_s = value;
        break;
      case DIRS:
        aswdirs_s = value;
        break;
      case SOBS_RAD:
        sobs_rad = value;
        break;
      case T_2M:
        t_2m = value;
        break;
      case T_G:
        t_g = value;
        break;
      case U_10M:
        u_10m = value;
        break;
      case V_10M:
        v_10m = value;
        break;
      case Z0:
        z0 = value;
        break;

        // Matrix Level Params
      case P_20M:
        p_20m = value;
        break;
      case P_65M:
        p_65m = value;
        break;
      case P_131M:
        p_131m = value;
        break;

      case T_131M:
        t_131m = value;
        break;

      case U_20M:
        u_20m = value;
        break;
      case U_65M:
        u_65m = value;
        break;
      case U_131M:
        u_131m = value;
        break;
      case U_216M:
        u_216m = value;
        break;

      case V_20M:
        v_20m = value;
        break;
      case V_65M:
        v_65m = value;
        break;
      case V_131M:
        v_131m = value;
        break;
      case V_216M:
        v_216m = value;
        break;

      case W_20M:
        w_20m = value;
        break;
      case W_65M:
        w_65m = value;
        break;
      case W_131M:
        w_131m = value;
        break;
      case W_216M:
        w_216m = value;
        break;
    }
  }

  public Double getParameter(Parameter param) {
    switch (param) {
        // Singlelevel Parameter
      case ALBEDO:
        return alb_rad;
      case ASOB_S:
        return asob_s;
      case DIFS_D:
        return aswdifd_s;
      case DIFS_U:
        return aswdifu_s;
      case DIRS:
        return aswdirs_s;
      case SOBS_RAD:
        return sobs_rad;
      case T_2M:
        return t_2m;
      case T_G:
        return t_g;
      case U_10M:
        return u_10m;
      case V_10M:
        return v_10m;
      case Z0:
        return z0;

        // Multilevel parameters
      case U_20M:
        return u_20m;
      case U_65M:
        return u_65m;
      case U_131M:
        return u_131m;
      case U_216M:
        return u_216m;

      case P_20M:
        return p_20m;
      case P_65M:
        return p_65m;
      case P_131M:
        return p_131m;

      case T_131M:
        return t_131m;

      case V_20M:
        return v_20m;
      case V_65M:
        return v_65m;
      case V_131M:
        return v_131m;
      case V_216M:
        return v_216m;

      case W_20M:
        return w_20m;
      case W_65M:
        return w_65m;
      case W_131M:
        return w_131m;
      case W_216M:
        return w_216m;

      default:
        return null;
    }
  }

  /** Overwrites if existing value is null */
  public void interpolateParameter(Parameter param, double interpolationRatio, Double value) {
    if (value == null) return;
    switch (param) {
        // Single Level Params
      case ALBEDO:
        alb_rad = interpolationCalculation(alb_rad, value, interpolationRatio);
        break;
      case ASOB_S:
        asob_s = interpolationCalculation(asob_s, value, interpolationRatio);
        break;
      case DIFS_D:
        aswdifd_s = interpolationCalculation(aswdifd_s, value, interpolationRatio);
        break;
      case DIFS_U:
        aswdifu_s = interpolationCalculation(aswdifu_s, value, interpolationRatio);
        break;
      case DIRS:
        aswdirs_s = interpolationCalculation(aswdirs_s, value, interpolationRatio);
        break;
      case SOBS_RAD:
        sobs_rad = interpolationCalculation(sobs_rad, value, interpolationRatio);
        break;
      case T_2M:
        t_2m = interpolationCalculation(t_2m, value, interpolationRatio);
        break;
      case T_G:
        t_g = interpolationCalculation(t_g, value, interpolationRatio);
        break;
      case U_10M:
        u_10m = interpolationCalculation(u_10m, value, interpolationRatio);
        break;
      case V_10M:
        v_10m = interpolationCalculation(v_10m, value, interpolationRatio);
        break;
      case Z0:
        z0 = interpolationCalculation(z0, value, interpolationRatio);
        break;

        // Matrix Type Params
      case P_20M:
        p_20m = interpolationCalculation(p_20m, value, interpolationRatio);
        break;
      case P_65M:
        p_65m = interpolationCalculation(p_65m, value, interpolationRatio);
        break;
      case P_131M:
        p_131m = interpolationCalculation(p_131m, value, interpolationRatio);
        break;

      case T_131M:
        t_131m = interpolationCalculation(t_131m, value, interpolationRatio);
        break;

      case U_20M:
        u_20m = interpolationCalculation(u_20m, value, interpolationRatio);
        break;
      case U_65M:
        u_65m = interpolationCalculation(u_65m, value, interpolationRatio);
        break;
      case U_131M:
        u_131m = interpolationCalculation(u_131m, value, interpolationRatio);
        break;
      case U_216M:
        u_216m = interpolationCalculation(u_216m, value, interpolationRatio);
        break;

      case V_20M:
        v_20m = interpolationCalculation(v_20m, value, interpolationRatio);
        break;
      case V_65M:
        v_65m = interpolationCalculation(v_65m, value, interpolationRatio);
        break;
      case V_131M:
        v_131m = interpolationCalculation(v_131m, value, interpolationRatio);
        break;
      case V_216M:
        v_216m = interpolationCalculation(v_216m, value, interpolationRatio);
        break;

      case W_20M:
        w_20m = interpolationCalculation(w_20m, value, interpolationRatio);
        break;
      case W_65M:
        w_65m = interpolationCalculation(w_65m, value, interpolationRatio);
        break;
      case W_131M:
        w_131m = interpolationCalculation(w_131m, value, interpolationRatio);
        break;
      case W_216M:
        w_216m = interpolationCalculation(w_216m, value, interpolationRatio);
        break;
    }
  }

  /** Interpolate values INTO this entity */
  public void interpolateValues(
      ICONWeatherModel entityToInterpolateWith, double interpolationRatio) {
    for (Parameter param : Parameter.values()) {
      interpolateParameter(param, interpolationRatio, entityToInterpolateWith.getParameter(param));
    }
  }

  public CoordinateModel getCoordinate() {
    return coordinate;
  }

  public void setCoordinate(CoordinateModel coordinate) {
    this.coordinate = coordinate;
  }

  // (datum, alb_rad, asob_s, aswdifd_s, aswdifu_s, aswdir_s,sobs_rad,p_20m,p_65m,p_131m, t_131m,
  // t_2m, t_g, u_10m, u_131m, u_20m,
  // u_216m, u_65m, v_10m, v_131m, v_20m, v_216m, v_65m, w_131m, w_20m, w_216m, w_65m, z0,
  // coordinate_id)
  public String getSQLInsertValuesString() {
    String insertValues = "(";
    insertValues += "'" + ConfigurationParameters.SQL_FORMATTER(date) + "', ";
    insertValues += alb_rad + ", ";
    insertValues += asob_s + ", ";
    insertValues += aswdifd_s + ", ";
    insertValues += aswdifu_s + ", ";
    insertValues += aswdirs_s + ", ";
    insertValues += sobs_rad + ", ";
    insertValues += p_20m + ", ";
    insertValues += p_65m + ", ";
    insertValues += p_131m + ", ";
    insertValues += t_131m + ", ";
    insertValues += t_2m + ", ";
    insertValues += t_g + ", ";
    insertValues += u_10m + ", ";
    insertValues += u_131m + ", ";
    insertValues += u_20m + ", ";
    insertValues += u_216m + ", ";
    insertValues += u_65m + ", ";
    insertValues += v_10m + ", ";
    insertValues += v_131m + ", ";
    insertValues += v_20m + ", ";
    insertValues += v_216m + ", ";
    insertValues += v_65m + ", ";
    insertValues += w_131m + ", ";
    insertValues += w_20m + ", ";
    insertValues += w_216m + ", ";
    insertValues += w_65m + ", ";
    insertValues += z0 + ", ";
    insertValues += coordinate.getId();
    insertValues += ")";
    return insertValues;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ICONWeatherModel)) return false;
    ICONWeatherModel that = (ICONWeatherModel) o;
    return Objects.equals(date, that.date)
        && Objects.equals(coordinate, that.coordinate)
        && Objects.equals(alb_rad, that.alb_rad)
        && Objects.equals(asob_s, that.asob_s)
        && Objects.equals(aswdifd_s, that.aswdifd_s)
        && Objects.equals(aswdifu_s, that.aswdifu_s)
        && Objects.equals(aswdirs_s, that.aswdirs_s)
        && Objects.equals(sobs_rad, that.sobs_rad)
        && Objects.equals(p_20m, that.p_20m)
        && Objects.equals(p_65m, that.p_65m)
        && Objects.equals(p_131m, that.p_131m)
        && Objects.equals(t_131m, that.t_131m)
        && Objects.equals(t_2m, that.t_2m)
        && Objects.equals(t_g, that.t_g)
        && Objects.equals(u_10m, that.u_10m)
        && Objects.equals(v_10m, that.v_10m)
        && Objects.equals(z0, that.z0)
        && Objects.equals(u_20m, that.u_20m)
        && Objects.equals(v_20m, that.v_20m)
        && Objects.equals(w_20m, that.w_20m)
        && Objects.equals(u_65m, that.u_65m)
        && Objects.equals(v_65m, that.v_65m)
        && Objects.equals(w_65m, that.w_65m)
        && Objects.equals(u_131m, that.u_131m)
        && Objects.equals(v_131m, that.v_131m)
        && Objects.equals(w_131m, that.w_131m)
        && Objects.equals(u_216m, that.u_216m)
        && Objects.equals(v_216m, that.v_216m)
        && Objects.equals(w_216m, that.w_216m);
  }

  @Override
  public int hashCode() {
    return Objects.hash(coordinate, date);
  }

  @Override
  public String toString() {
    return "ICONWeatherModel{"
        + "date="
        + date
        + "\n, coordinate="
        + coordinate
        + ", alb_rad="
        + alb_rad
        + ", asob_s="
        + asob_s
        + ", aswdifd_s="
        + aswdifd_s
        + ", aswdifu_s="
        + aswdifu_s
        + ", aswdirs_s="
        + aswdirs_s
        + ", sobs_rad="
        + sobs_rad
        + ", t_131m="
        + t_131m
        + ", t_2m="
        + t_2m
        + ", t_g="
        + t_g
        + ", u_10m="
        + u_10m
        + ", v_10m="
        + v_10m
        + ", z0="
        + z0
        + ", p_20m="
        + p_20m
        + ", p_65m="
        + p_65m
        + ", p_131m="
        + p_131m
        + ", u_20m="
        + u_20m
        + ", v_20m="
        + v_20m
        + ", w_20m="
        + w_20m
        + ", u_65m="
        + u_65m
        + ", v_65m="
        + v_65m
        + ", w_65m="
        + w_65m
        + ", u_131m="
        + u_131m
        + ", v_131m="
        + v_131m
        + ", w_131m="
        + w_131m
        + ", u_216m="
        + u_216m
        + ", v_216m="
        + v_216m
        + ", w_216m="
        + w_216m
        + '}';
  }

  public Double interpolationCalculation(
      Double earlierValue, Double newerValue, double interpolationRatio) {
    if (newerValue == null) {
      return earlierValue;
    } else if (earlierValue == null) {
      return newerValue;
    } else {
      return earlierValue * (1 - interpolationRatio) + newerValue * interpolationRatio;
    }
  }
}
