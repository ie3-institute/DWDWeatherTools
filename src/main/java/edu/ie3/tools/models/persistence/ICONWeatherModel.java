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
  private Double albRad;

  /** Net short-wave radiation flux at surface in W/m² */
  @Column(name = "asob_s")
  private Double asobS;

  /** Surface down solar diffuse radiation in W/m² */
  @Column(name = "aswdifd_s")
  private Double aswdifdS;

  /** Surface up diffuse radiation in W/m² */
  @Column(name = "aswdifu_s")
  private Double aswdifuS;

  /** Direct radiation in W/m² */
  @Column(name = "aswdir_s")
  private Double aswdirS;

  /** Net short-wave radiation flux at surface (instantaneous) in W/m² */
  @Column(name = "sobs_rad")
  private Double sobsRad;

  /** Temperature at 131m above ground in K */
  @Column(name = "t_131m")
  private Double t131m;

  /** Temperature at 2m above ground in K */
  @Column(name = "t_2m")
  private Double t2m;

  /** Ground temperature in K */
  @Column(name = "t_g")
  private Double tG;

  /** Zonal wind at 10m above ground in m/s */
  @Column(name = "u_10m")
  private Double u10m;

  /** Meridional wind at 10m above ground in m/s */
  @Column(name = "v_10m")
  private Double v10m;

  /** Surface roughness in m */
  @Column(name = "z0")
  private Double z0;

  /** Pressure at 20m above ground in Pa */
  @Column(name = "p_20m")
  private Double p20m;

  /** Pressure at 65m above ground in Pa */
  @Column(name = "p_65m")
  private Double p65m;

  /** Pressure at 131m above ground in Pa */
  @Column(name = "p_131m")
  private Double p131m;

  /** Zonal wind at 20m above ground in m/s */
  @Column(name = "u_20m")
  private Double u20m;

  /** Meridional wind at 20m above ground in m/s */
  @Column(name = "v_20m")
  private Double v20m;

  /** Vertical wind at 20m above ground in m/s */
  @Column(name = "w_20m")
  private Double w20m;

  /** Zonal wind at 65m above ground in m/s */
  @Column(name = "u_65m")
  private Double u65m;

  /** Meridional wind at 65m above ground in m/s */
  @Column(name = "v_65m")
  private Double v65m;

  /** Vertical wind at 65m above ground in m/s */
  @Column(name = "w_65m")
  private Double w65m;

  /** Zonal wind at 131m above ground in m/s */
  @Column(name = "u_131m")
  private Double u131m;

  /** Meridional wind at 131m above ground in m/s */
  @Column(name = "v_131m")
  private Double v131m;

  /** Vertical wind at 131m above ground in m/s */
  @Column(name = "w_131m")
  private Double w131m;

  /** Zonal wind at 216m above ground in m/s */
  @Column(name = "u_216m")
  private Double u216m;

  /** Meridional wind at 216m above ground in m/s */
  @Column(name = "v_216m")
  private Double v216m;

  /** Vertical wind at 216m above ground in m/s */
  @Column(name = "w_216m")
  private Double w216m;

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
      Collection<ICONWeatherModel> entities, String databaseSchema) {
    StringBuilder upsertStatementBuilder = new StringBuilder();
    upsertStatementBuilder
        .append("INSERT INTO ")
        .append(databaseSchema)
        .append(".weather(\n")
        .append(
            "\tdatum, alb_rad, asob_s, aswdifd_s, aswdifu_s, aswdir_s, sobs_rad, p_20m, p_65m, p_131m, t_131m, t_2m, t_g, u_10m, u_131m, u_20m, u_216m, u_65m, v_10m, v_131m, v_20m, v_216m, v_65m, w_131m, w_20m, w_216m, w_65m, z0, coordinate_id)\n")
        .append("\t VALUES ");
    entities.forEach(
        entity -> upsertStatementBuilder.append(entity.getSQLInsertValuesString()).append(", "));
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

  public Double getAlbRad() {
    return albRad;
  }

  public void setAlbRad(Double albRad) {
    this.albRad = albRad;
  }

  public Double getAsobS() {
    return asobS;
  }

  public void setAsobS(Double asobS) {
    this.asobS = asobS;
  }

  public Double getAswdifdS() {
    return aswdifdS;
  }

  public void setAswdifdS(Double aswdifdS) {
    this.aswdifdS = aswdifdS;
  }

  public Double getAswdifuS() {
    return aswdifuS;
  }

  public void setAswdifuS(Double aswdifuS) {
    this.aswdifuS = aswdifuS;
  }

  public Double getAswdirS() {
    return aswdirS;
  }

  public void setAswdirS(Double aswdirsS) {
    this.aswdirS = aswdirsS;
  }

  public Double getT2m() {
    return t2m;
  }

  public void setT2m(Double t2m) {
    this.t2m = t2m;
  }

  public Double getTG() {
    return tG;
  }

  public void setTG(Double tG) {
    this.tG = tG;
  }

  public Double getU10m() {
    return u10m;
  }

  public void setU10m(Double u10m) {
    this.u10m = u10m;
  }

  public Double getV10m() {
    return v10m;
  }

  public void setV10m(Double v10m) {
    this.v10m = v10m;
  }

  public Double getZ0() {
    return z0;
  }

  public void setZ0(Double z0) {
    this.z0 = z0;
  }

  public Double getU20m() {
    return u20m;
  }

  public void setU20m(Double u20m) {
    this.u20m = u20m;
  }

  public Double getV20m() {
    return v20m;
  }

  public void setV20m(Double v20m) {
    this.v20m = v20m;
  }

  public Double getW20m() {
    return w20m;
  }

  public void setW20m(Double w20m) {
    this.w20m = w20m;
  }

  public Double getU65m() {
    return u65m;
  }

  public void setU65m(Double u65m) {
    this.u65m = u65m;
  }

  public Double getV65m() {
    return v65m;
  }

  public void setV65m(Double v65m) {
    this.v65m = v65m;
  }

  public Double getW65m() {
    return w65m;
  }

  public void setW65m(Double w65m) {
    this.w65m = w65m;
  }

  public Double getU216m() {
    return u216m;
  }

  public void setU216m(Double u216m) {
    this.u216m = u216m;
  }

  public Double getV216m() {
    return v216m;
  }

  public void setV216m(Double v216m) {
    this.v216m = v216m;
  }

  public Double getW216m() {
    return w216m;
  }

  public void setW216m(Double w216m) {
    this.w216m = w216m;
  }

  public Double getU131m() {
    return u131m;
  }

  public void setU131m(Double u131m) {
    this.u131m = u131m;
  }

  public Double getV131m() {
    return v131m;
  }

  public void setV131m(Double v131m) {
    this.v131m = v131m;
  }

  public Double getW131m() {
    return w131m;
  }

  public void setW131m(Double w131m) {
    this.w131m = w131m;
  }

  public Double getSobsRad() {
    return sobsRad;
  }

  public void setSobsRad(Double sobsRad) {
    this.sobsRad = sobsRad;
  }

  public Double getT131m() {
    return t131m;
  }

  public void setT131m(Double t131m) {
    this.t131m = t131m;
  }

  public Double getP20m() {
    return p20m;
  }

  public void setP20m(Double p20m) {
    this.p20m = p20m;
  }

  public Double getP65m() {
    return p65m;
  }

  public void setP65m(Double p65m) {
    this.p65m = p65m;
  }

  public Double getP131m() {
    return p131m;
  }

  public void setP131m(Double p131m) {
    this.p131m = p131m;
  }

  public void setParameter(Parameter param, Double value) {
    if (value == null) return;
    switch (param) {
        // Single Level Params
      case ALBEDO:
        albRad = value;
        break;
      case ASOB_S:
        asobS = value;
        break;
      case DIFS_D:
        aswdifdS = value;
        break;
      case DIFS_U:
        aswdifuS = value;
        break;
      case DIRS:
        aswdirS = value;
        break;
      case SOBS_RAD:
        sobsRad = value;
        break;
      case T_2M:
        t2m = value;
        break;
      case T_G:
        tG = value;
        break;
      case U_10M:
        u10m = value;
        break;
      case V_10M:
        v10m = value;
        break;
      case Z0:
        z0 = value;
        break;

        // Matrix Level Params
      case P_20M:
        p20m = value;
        break;
      case P_65M:
        p65m = value;
        break;
      case P_131M:
        p131m = value;
        break;

      case T_131M:
        t131m = value;
        break;

      case U_20M:
        u20m = value;
        break;
      case U_65M:
        u65m = value;
        break;
      case U_131M:
        u131m = value;
        break;
      case U_216M:
        u216m = value;
        break;

      case V_20M:
        v20m = value;
        break;
      case V_65M:
        v65m = value;
        break;
      case V_131M:
        v131m = value;
        break;
      case V_216M:
        v216m = value;
        break;

      case W_20M:
        w20m = value;
        break;
      case W_65M:
        w65m = value;
        break;
      case W_131M:
        w131m = value;
        break;
      case W_216M:
        w216m = value;
        break;
    }
  }

  public Double getParameter(Parameter param) {
    switch (param) {
        // Singlelevel Parameter
      case ALBEDO:
        return albRad;
      case ASOB_S:
        return asobS;
      case DIFS_D:
        return aswdifdS;
      case DIFS_U:
        return aswdifuS;
      case DIRS:
        return aswdirS;
      case SOBS_RAD:
        return sobsRad;
      case T_2M:
        return t2m;
      case T_G:
        return tG;
      case U_10M:
        return u10m;
      case V_10M:
        return v10m;
      case Z0:
        return z0;

        // Multilevel parameters
      case U_20M:
        return u20m;
      case U_65M:
        return u65m;
      case U_131M:
        return u131m;
      case U_216M:
        return u216m;

      case P_20M:
        return p20m;
      case P_65M:
        return p65m;
      case P_131M:
        return p131m;

      case T_131M:
        return t131m;

      case V_20M:
        return v20m;
      case V_65M:
        return v65m;
      case V_131M:
        return v131m;
      case V_216M:
        return v216m;

      case W_20M:
        return w20m;
      case W_65M:
        return w65m;
      case W_131M:
        return w131m;
      case W_216M:
        return w216m;

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
        albRad = interpolationCalculation(albRad, value, interpolationRatio);
        break;
      case ASOB_S:
        asobS = interpolationCalculation(asobS, value, interpolationRatio);
        break;
      case DIFS_D:
        aswdifdS = interpolationCalculation(aswdifdS, value, interpolationRatio);
        break;
      case DIFS_U:
        aswdifuS = interpolationCalculation(aswdifuS, value, interpolationRatio);
        break;
      case DIRS:
        aswdirS = interpolationCalculation(aswdirS, value, interpolationRatio);
        break;
      case SOBS_RAD:
        sobsRad = interpolationCalculation(sobsRad, value, interpolationRatio);
        break;
      case T_2M:
        t2m = interpolationCalculation(t2m, value, interpolationRatio);
        break;
      case T_G:
        tG = interpolationCalculation(tG, value, interpolationRatio);
        break;
      case U_10M:
        u10m = interpolationCalculation(u10m, value, interpolationRatio);
        break;
      case V_10M:
        v10m = interpolationCalculation(v10m, value, interpolationRatio);
        break;
      case Z0:
        z0 = interpolationCalculation(z0, value, interpolationRatio);
        break;

        // Matrix Type Params
      case P_20M:
        p20m = interpolationCalculation(p20m, value, interpolationRatio);
        break;
      case P_65M:
        p65m = interpolationCalculation(p65m, value, interpolationRatio);
        break;
      case P_131M:
        p131m = interpolationCalculation(p131m, value, interpolationRatio);
        break;

      case T_131M:
        t131m = interpolationCalculation(t131m, value, interpolationRatio);
        break;

      case U_20M:
        u20m = interpolationCalculation(u20m, value, interpolationRatio);
        break;
      case U_65M:
        u65m = interpolationCalculation(u65m, value, interpolationRatio);
        break;
      case U_131M:
        u131m = interpolationCalculation(u131m, value, interpolationRatio);
        break;
      case U_216M:
        u216m = interpolationCalculation(u216m, value, interpolationRatio);
        break;

      case V_20M:
        v20m = interpolationCalculation(v20m, value, interpolationRatio);
        break;
      case V_65M:
        v65m = interpolationCalculation(v65m, value, interpolationRatio);
        break;
      case V_131M:
        v131m = interpolationCalculation(v131m, value, interpolationRatio);
        break;
      case V_216M:
        v216m = interpolationCalculation(v216m, value, interpolationRatio);
        break;

      case W_20M:
        w20m = interpolationCalculation(w20m, value, interpolationRatio);
        break;
      case W_65M:
        w65m = interpolationCalculation(w65m, value, interpolationRatio);
        break;
      case W_131M:
        w131m = interpolationCalculation(w131m, value, interpolationRatio);
        break;
      case W_216M:
        w216m = interpolationCalculation(w216m, value, interpolationRatio);
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
    insertValues += albRad + ", ";
    insertValues += asobS + ", ";
    insertValues += aswdifdS + ", ";
    insertValues += aswdifuS + ", ";
    insertValues += aswdirS + ", ";
    insertValues += sobsRad + ", ";
    insertValues += p20m + ", ";
    insertValues += p65m + ", ";
    insertValues += p131m + ", ";
    insertValues += t131m + ", ";
    insertValues += t2m + ", ";
    insertValues += tG + ", ";
    insertValues += u10m + ", ";
    insertValues += u131m + ", ";
    insertValues += u20m + ", ";
    insertValues += u216m + ", ";
    insertValues += u65m + ", ";
    insertValues += v10m + ", ";
    insertValues += v131m + ", ";
    insertValues += v20m + ", ";
    insertValues += v216m + ", ";
    insertValues += v65m + ", ";
    insertValues += w131m + ", ";
    insertValues += w20m + ", ";
    insertValues += w216m + ", ";
    insertValues += w65m + ", ";
    insertValues += z0 + ", ";
    insertValues += coordinate.getId();
    insertValues += ")";
    return insertValues;
  }

  /**
   * Get String for an prepared statement, possibly postgres specific <br>
   * refer to:
   * https://stackoverflow.com/questions/3107044/preparedstatement-with-list-of-parameters-in-a-in-clause
   */
  public static String getPSQLFindString(String databaseSchema) {
    return "SELECT * FROM "
        + databaseSchema
        + ".weather w JOIN "
        + databaseSchema
        + ".icon_coordinates c ON w.coordinate_id = c.id "
        + "WHERE datum=? AND coordinate_id = ANY(?);";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ICONWeatherModel)) return false;
    ICONWeatherModel that = (ICONWeatherModel) o;
    return Objects.equals(date, that.date)
        && Objects.equals(coordinate, that.coordinate)
        && Objects.equals(albRad, that.albRad)
        && Objects.equals(asobS, that.asobS)
        && Objects.equals(aswdifdS, that.aswdifdS)
        && Objects.equals(aswdifuS, that.aswdifuS)
        && Objects.equals(aswdirS, that.aswdirS)
        && Objects.equals(sobsRad, that.sobsRad)
        && Objects.equals(p20m, that.p20m)
        && Objects.equals(p65m, that.p65m)
        && Objects.equals(p131m, that.p131m)
        && Objects.equals(t131m, that.t131m)
        && Objects.equals(t2m, that.t2m)
        && Objects.equals(tG, that.tG)
        && Objects.equals(u10m, that.u10m)
        && Objects.equals(v10m, that.v10m)
        && Objects.equals(z0, that.z0)
        && Objects.equals(u20m, that.u20m)
        && Objects.equals(v20m, that.v20m)
        && Objects.equals(w20m, that.w20m)
        && Objects.equals(u65m, that.u65m)
        && Objects.equals(v65m, that.v65m)
        && Objects.equals(w65m, that.w65m)
        && Objects.equals(u131m, that.u131m)
        && Objects.equals(v131m, that.v131m)
        && Objects.equals(w131m, that.w131m)
        && Objects.equals(u216m, that.u216m)
        && Objects.equals(v216m, that.v216m)
        && Objects.equals(w216m, that.w216m);
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
        + albRad
        + ", asob_s="
        + asobS
        + ", aswdifd_s="
        + aswdifdS
        + ", aswdifu_s="
        + aswdifuS
        + ", aswdirs_s="
        + aswdirS
        + ", sobs_rad="
        + sobsRad
        + ", t_131m="
        + t131m
        + ", t_2m="
        + t2m
        + ", t_g="
        + tG
        + ", u_10m="
        + u10m
        + ", v_10m="
        + v10m
        + ", z0="
        + z0
        + ", p_20m="
        + p20m
        + ", p_65m="
        + p65m
        + ", p_131m="
        + p131m
        + ", u_20m="
        + u20m
        + ", v_20m="
        + v20m
        + ", w_20m="
        + w20m
        + ", u_65m="
        + u65m
        + ", v_65m="
        + v65m
        + ", w_65m="
        + w65m
        + ", u_131m="
        + u131m
        + ", v_131m="
        + v131m
        + ", w_131m="
        + w131m
        + ", u_216m="
        + u216m
        + ", v_216m="
        + v216m
        + ", w_216m="
        + w216m
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
