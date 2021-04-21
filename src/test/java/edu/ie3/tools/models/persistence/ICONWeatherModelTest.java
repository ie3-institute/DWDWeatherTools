/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools.models.persistence;

import static junit.framework.TestCase.*;

import edu.ie3.tools.utils.ConfigurationParameters;
import edu.ie3.tools.utils.enums.Parameter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import org.junit.Before;
import org.junit.Test;

public class ICONWeatherModelTest {

  private CoordinateModel coordinate = new CoordinateModel(47.3, 7.6);
  private ZonedDateTime date = ZonedDateTime.of(2019, 7, 3, 3, 14, 0, 0, ZoneId.of("UTC"));
  private final double alb_rad = 0.0;
  private final double asob_s = 3.14159265359;
  private final double aswdifd_s = -1.0;
  private final double aswdifu_s = -1.0;
  private final double aswdirs_s = 0.3333333;
  private final double sobs_rad = 0.375;
  private final double p_20m = 1.0;
  private final double p_65m = 3.0;
  private final double p_131m = 6.0;
  private final double t_131m = 1.0;
  private final double t_2m = 1.0;
  private final double t_g = 1.5;
  private final double u_10m = 22.0;
  private final double u_131m = 22.5;
  private final double u_20m = 23.0;
  private final double u_216m = 23.5;
  private final double u_65m = 24.0;
  private final double v_10m = 34.0;
  private final double v_131m = 34.5;
  private final double v_20m = 35.0;
  private final double v_216m = 35.5;
  private final double v_65m = 36.0;
  private final double w_131m = 46.0;
  private final double w_20m = 6.3;
  private final double w_216m = 6.4;
  private final double w_65m = 6.5;
  private final double z0 = 200.0;

  private ICONWeatherModel weather;

  @Before
  public void setUp() throws Exception {
    weather = generateWeatherEntity();
  }

  @Test
  public void getInterpolatedEntity() {
    ICONWeatherModel interpolatedEntity = ICONWeatherModel.getInterpolatedEntity(weather);
    interpolatedEntity.setTime(date);
    interpolatedEntity.setCoordinate(coordinate);
    assertEquals(weather, interpolatedEntity); // should be the same

    ICONWeatherModel newWeather = new ICONWeatherModel(date, coordinate);
    interpolatedEntity.setTime(date);
    interpolatedEntity.setCoordinate(coordinate);
    newWeather.setAlb_rad(alb_rad);

    interpolatedEntity = ICONWeatherModel.getInterpolatedEntity(weather, newWeather);
    interpolatedEntity.setTime(date);
    interpolatedEntity.setCoordinate(coordinate);
    assertEquals(
        weather,
        interpolatedEntity); // should be the same as alb_rad should be the same and all other
    // values should be null

    ICONWeatherModel[] testEntities = new ICONWeatherModel[10];
    testEntities[0] = weather;
    for (int numberOfEntities = 1; numberOfEntities < 10; numberOfEntities++) {
      testEntities[numberOfEntities] = generateRandomWeatherEntity();
    }
    HashMap<String, Double> collectedParameterValues = collectParameterValues(testEntities);
    interpolatedEntity = ICONWeatherModel.getInterpolatedEntity(testEntities);
    interpolatedEntity.setTime(date);
    interpolatedEntity.setCoordinate(coordinate);

    assertEquals(collectedParameterValues.get("alb_rad") / 10, interpolatedEntity.getAlb_rad());
    assertEquals(collectedParameterValues.get("asob_s") / 10, interpolatedEntity.getAsob_s());
    assertEquals(collectedParameterValues.get("aswdifd_s") / 10, interpolatedEntity.getAswdifd_s());
    assertEquals(collectedParameterValues.get("aswdifu_s") / 10, interpolatedEntity.getAswdifu_s());
    assertEquals(collectedParameterValues.get("aswdirs_s") / 10, interpolatedEntity.getAswdirs_s());
    assertEquals(collectedParameterValues.get("sobs_rad") / 10, interpolatedEntity.getSobs_rad());
    assertEquals(collectedParameterValues.get("p_20m") / 10, interpolatedEntity.getP_20m());
    assertEquals(collectedParameterValues.get("p_65m") / 10, interpolatedEntity.getP_65m());
    assertEquals(collectedParameterValues.get("p_131m") / 10, interpolatedEntity.getP_131m());
    assertEquals(collectedParameterValues.get("t_131m") / 10, interpolatedEntity.getT_131m());
    assertEquals(collectedParameterValues.get("t_2m") / 10, interpolatedEntity.getT_2m());
    assertEquals(collectedParameterValues.get("t_g") / 10, interpolatedEntity.getT_g());
    assertEquals(collectedParameterValues.get("u_10m") / 10, interpolatedEntity.getU_10m());
    assertEquals(collectedParameterValues.get("u_131m") / 10, interpolatedEntity.getU_131m());
    assertEquals(collectedParameterValues.get("u_20m") / 10, interpolatedEntity.getU_20m());
    assertEquals(collectedParameterValues.get("u_216m") / 10, interpolatedEntity.getU_216m());
    assertEquals(collectedParameterValues.get("u_65m") / 10, interpolatedEntity.getU_65m());
    assertEquals(collectedParameterValues.get("v_10m") / 10, interpolatedEntity.getV_10m());
    assertEquals(collectedParameterValues.get("v_131m") / 10, interpolatedEntity.getV_131m());
    assertEquals(collectedParameterValues.get("v_20m") / 10, interpolatedEntity.getV_20m());
    assertEquals(collectedParameterValues.get("v_216m") / 10, interpolatedEntity.getV_216m());
    assertEquals(collectedParameterValues.get("v_65m") / 10, interpolatedEntity.getV_65m());
    assertEquals(collectedParameterValues.get("w_131m") / 10, interpolatedEntity.getW_131m());
    assertEquals(collectedParameterValues.get("w_20m") / 10, interpolatedEntity.getW_20m());
    assertEquals(collectedParameterValues.get("w_216m") / 10, interpolatedEntity.getW_216m());
    assertEquals(collectedParameterValues.get("w_65m") / 10, interpolatedEntity.getW_65m());
    assertEquals(collectedParameterValues.get("z0") / 10, interpolatedEntity.getZ0());
  }

  @Test
  public void getSQLUpsertStatement() {
    ICONWeatherModel randWeather1 = generateRandomWeatherEntity();
    randWeather1.setTime(date);
    randWeather1.setCoordinate(coordinate);
    ICONWeatherModel randWeather2 = generateRandomWeatherEntity();
    randWeather2.setTime(date);
    randWeather2.setCoordinate(coordinate);
    Collection<ICONWeatherModel> entities = Arrays.asList(weather, randWeather1, randWeather2);
    String generatedUpsertStatement = ICONWeatherModel.getSQLUpsertStatement(entities, "test");
    String sqlInsertInto =
        "INSERT INTO test.weather(\n\ttime, alb_rad, asob_s, aswdifd_s, aswdifu_s, aswdir_s, sobs_rad, p_20m, p_65m, p_131m, t_131m, t_2m, t_g, "
            + "u_10m, u_131m, u_20m, u_216m, u_65m, v_10m, v_131m, v_20m, v_216m, v_65m, w_131m, w_20m, w_216m, w_65m, "
            + "z0, coordinate_id)\n\t VALUES ";
    assertTrue(generatedUpsertStatement.startsWith(sqlInsertInto));
    String sqlOnConflict =
        " ON CONFLICT (coordinate_id, time) DO UPDATE \n  SET time=excluded.time,\n "
            + "alb_rad=excluded.alb_rad,\n asob_s=excluded.asob_s,\n aswdifd_s=excluded.aswdifd_s,\n "
            + "aswdifu_s=excluded.aswdifu_s,\n aswdir_s=excluded.aswdir_s,\n sobs_rad=excluded.sobs_rad,\n "
            + "p_20m=excluded.p_20m,\n p_65m=excluded.p_65m,\n p_131m=excluded.p_131m,\n t_131m=excluded.t_131m,\n "
            + "t_2m=excluded.t_2m,\n t_g=excluded.t_g,\n "
            + "u_10m=excluded.u_10m,\n u_131m=excluded.u_131m,\n u_20m=excluded.u_20m,\n u_216m=excluded.u_216m,\n "
            + "u_65m=excluded.u_65m,\n v_10m=excluded.v_10m,\n v_131m=excluded.v_131m,\n v_20m=excluded.v_20m,\n "
            + "v_216m=excluded.v_216m,\n v_65m=excluded.v_65m,\n w_131m=excluded.w_131m,\n w_20m=excluded.w_20m,\n "
            + "w_216m=excluded.w_216m,\n w_65m=excluded.w_65m,\n z0=excluded.z0,\n coordinate_id=excluded.coordinate_id;";
    String expectedUpsertStatement =
        sqlInsertInto
            + weather.getSQLInsertValuesString()
            + ", "
            + randWeather1.getSQLInsertValuesString()
            + ", "
            + randWeather2.getSQLInsertValuesString()
            + sqlOnConflict;
    assertEquals(expectedUpsertStatement, generatedUpsertStatement);
  }

  @Test
  public void setParameter() {
    weather.setParameter(Parameter.ASOB_S, 1.337);
    assertEquals(1.337, weather.getAsob_s()); // value changed
    weather.setParameter(Parameter.ASOB_S, null);
    assertEquals(1.337, weather.getAsob_s()); // value does not change, when null
    weather.setParameter(Parameter.ASOB_S, asob_s);
    assertEquals(generateWeatherEntity(), weather); // no other values should have changed
    weather.setParameter(Parameter.U_10M, 4.2);
    assertEquals(4.2, weather.getU_10m()); // value changed for single level values
    weather.setParameter(Parameter.U_216M, 2.4);
    assertEquals(2.4, weather.getU_216m()); // value changed for multi level values
  }

  @Test
  public void getParameter() {
    assertEquals(asob_s, weather.getParameter(Parameter.ASOB_S));
    assertEquals(u_10m, weather.getParameter(Parameter.U_10M));
    assertEquals(u_216m, weather.getParameter(Parameter.U_216M));
  }

  @Test
  public void interpolateParameter() {
    ICONWeatherModel defaultWeather = generateWeatherEntity();
    weather.interpolateParameter(Parameter.ALBEDO, 0.5, null);
    assertEquals(weather, defaultWeather); // don't interpolate when value is null
    weather.interpolateParameter(Parameter.ALBEDO, 0.0, 1.337);
    assertEquals(
        weather, defaultWeather); // don't (really) interpolate when interpolationRatio is 0
    weather.interpolateParameter(Parameter.ALBEDO, 1.0, 1.337);
    assertEquals(1.337, weather.getAlb_rad()); // overwrite when interpolationRatio is 1
    weather.interpolateParameter(Parameter.ASOB_S, 0.5, 1.337);
    assertEquals((asob_s + 1.337) / 2, weather.getAsob_s()); // 50 % old value, 50% new value
    weather.interpolateParameter(Parameter.Z0, 0.67, 100.0);
    assertEquals(z0 * (1 - 0.67) + 100 * 0.67, weather.getZ0()); // 33% old value, 66% new value
  }

  @Test
  public void interpolateValues() {
    ICONWeatherModel randWeather = generateRandomWeatherEntity();
    HashMap<String, Double> collectedParameterValues = collectParameterValues(weather, randWeather);
    weather.interpolateValues(randWeather, 0.5);
    assertEquals(collectedParameterValues.get("alb_rad") / 2, weather.getAlb_rad());
    assertEquals(collectedParameterValues.get("asob_s") / 2, weather.getAsob_s());
    assertEquals(collectedParameterValues.get("aswdifd_s") / 2, weather.getAswdifd_s());
    assertEquals(collectedParameterValues.get("aswdifu_s") / 2, weather.getAswdifu_s());
    assertEquals(collectedParameterValues.get("aswdirs_s") / 2, weather.getAswdirs_s());
    assertEquals(collectedParameterValues.get("sobs_rad") / 2, weather.getSobs_rad());
    assertEquals(collectedParameterValues.get("p_20m") / 2, weather.getP_20m());
    assertEquals(collectedParameterValues.get("p_65m") / 2, weather.getP_65m());
    assertEquals(collectedParameterValues.get("p_131m") / 2, weather.getP_131m());
    assertEquals(collectedParameterValues.get("t_131m") / 2, weather.getT_131m());
    assertEquals(collectedParameterValues.get("t_2m") / 2, weather.getT_2m());
    assertEquals(collectedParameterValues.get("t_g") / 2, weather.getT_g());
    assertEquals(collectedParameterValues.get("u_10m") / 2, weather.getU_10m());
    assertEquals(collectedParameterValues.get("u_131m") / 2, weather.getU_131m());
    assertEquals(collectedParameterValues.get("u_20m") / 2, weather.getU_20m());
    assertEquals(collectedParameterValues.get("u_216m") / 2, weather.getU_216m());
    assertEquals(collectedParameterValues.get("u_65m") / 2, weather.getU_65m());
    assertEquals(collectedParameterValues.get("v_10m") / 2, weather.getV_10m());
    assertEquals(collectedParameterValues.get("v_131m") / 2, weather.getV_131m());
    assertEquals(collectedParameterValues.get("v_20m") / 2, weather.getV_20m());
    assertEquals(collectedParameterValues.get("v_216m") / 2, weather.getV_216m());
    assertEquals(collectedParameterValues.get("v_65m") / 2, weather.getV_65m());
    assertEquals(collectedParameterValues.get("w_131m") / 2, weather.getW_131m());
    assertEquals(collectedParameterValues.get("w_20m") / 2, weather.getW_20m());
    assertEquals(collectedParameterValues.get("w_216m") / 2, weather.getW_216m());
    assertEquals(collectedParameterValues.get("w_65m") / 2, weather.getW_65m());
    assertEquals(collectedParameterValues.get("z0") / 2, weather.getZ0());
  }

  @Test
  public void getSQLInsertValuesString() {
    String generatedInsertValuesString = weather.getSQLInsertValuesString();
    assertTrue(generatedInsertValuesString.startsWith("('2019-07-03T03:14'"));
    assertTrue(generatedInsertValuesString.endsWith(")"));
    assertTrue(
        generatedInsertValuesString.split(",").length == 29); // contains 29 comma separated values
    assertTrue(generatedInsertValuesString.contains("-1.0")); // negative values
    String expectedInsertValuesString = "(";
    expectedInsertValuesString += "'" + ConfigurationParameters.SQL_FORMATTER(date) + "', ";
    expectedInsertValuesString += alb_rad + ", ";
    expectedInsertValuesString += asob_s + ", ";
    expectedInsertValuesString += aswdifd_s + ", ";
    expectedInsertValuesString += aswdifu_s + ", ";
    expectedInsertValuesString += aswdirs_s + ", ";
    expectedInsertValuesString += sobs_rad + ", ";
    expectedInsertValuesString += p_20m + ", ";
    expectedInsertValuesString += p_65m + ", ";
    expectedInsertValuesString += p_131m + ", ";
    expectedInsertValuesString += t_131m + ", ";
    expectedInsertValuesString += t_2m + ", ";
    expectedInsertValuesString += t_g + ", ";
    expectedInsertValuesString += u_10m + ", ";
    expectedInsertValuesString += u_131m + ", ";
    expectedInsertValuesString += u_20m + ", ";
    expectedInsertValuesString += u_216m + ", ";
    expectedInsertValuesString += u_65m + ", ";
    expectedInsertValuesString += v_10m + ", ";
    expectedInsertValuesString += v_131m + ", ";
    expectedInsertValuesString += v_20m + ", ";
    expectedInsertValuesString += v_216m + ", ";
    expectedInsertValuesString += v_65m + ", ";
    expectedInsertValuesString += w_131m + ", ";
    expectedInsertValuesString += w_20m + ", ";
    expectedInsertValuesString += w_216m + ", ";
    expectedInsertValuesString += w_65m + ", ";
    expectedInsertValuesString += z0 + ", ";
    expectedInsertValuesString += coordinate.getId();
    expectedInsertValuesString += ")";
    assertEquals(expectedInsertValuesString, generatedInsertValuesString);
  }

  private static ICONWeatherModel generateRandomWeatherEntity() {
    ICONWeatherModel randEntity = new ICONWeatherModel();
    double min = -200d;
    double max = 200d;
    randEntity.setAlb_rad((Math.random() * ((max - min) + 1) + min));
    randEntity.setAsob_s((Math.random() * ((max - min) + 1) + min));
    randEntity.setAswdifd_s((Math.random() * ((max - min) + 1) + min));
    randEntity.setAswdifu_s((Math.random() * ((max - min) + 1) + min));
    randEntity.setAswdirs_s((Math.random() * ((max - min) + 1) + min));
    randEntity.setSobs_rad((Math.random() * ((max - min) + 1) + min));
    randEntity.setP_20m((Math.random() * ((max - min) + 1) + min));
    randEntity.setP_65m((Math.random() * ((max - min) + 1) + min));
    randEntity.setP_131m((Math.random() * ((max - min) + 1) + min));
    randEntity.setT_131m((Math.random() * ((max - min) + 1) + min));
    randEntity.setT_2m((Math.random() * ((max - min) + 1) + min));
    randEntity.setT_g((Math.random() * ((max - min) + 1) + min));
    randEntity.setU_10m((Math.random() * ((max - min) + 1) + min));
    randEntity.setU_131m((Math.random() * ((max - min) + 1) + min));
    randEntity.setU_20m((Math.random() * ((max - min) + 1) + min));
    randEntity.setU_216m((Math.random() * ((max - min) + 1) + min));
    randEntity.setU_65m((Math.random() * ((max - min) + 1) + min));
    randEntity.setV_10m((Math.random() * ((max - min) + 1) + min));
    randEntity.setV_131m((Math.random() * ((max - min) + 1) + min));
    randEntity.setV_20m((Math.random() * ((max - min) + 1) + min));
    randEntity.setV_216m((Math.random() * ((max - min) + 1) + min));
    randEntity.setV_65m((Math.random() * ((max - min) + 1) + min));
    randEntity.setW_131m((Math.random() * ((max - min) + 1) + min));
    randEntity.setW_20m((Math.random() * ((max - min) + 1) + min));
    randEntity.setW_216m((Math.random() * ((max - min) + 1) + min));
    randEntity.setW_65m((Math.random() * ((max - min) + 1) + min));
    randEntity.setZ0((Math.random() * ((max - min) + 1) + min));
    return randEntity;
  }

  private static HashMap<String, Double> collectParameterValues(ICONWeatherModel... entities) {
    HashMap<String, Double> map = new HashMap<>();
    map.put("alb_rad", 0.0);
    map.put("asob_s", 0.0);
    map.put("aswdifd_s", 0.0);
    map.put("aswdifu_s", 0.0);
    map.put("aswdirs_s", 0.0);
    map.put("sobs_rad", 0.0);
    map.put("p_20m", 0.0);
    map.put("p_65m", 0.0);
    map.put("p_131m", 0.0);
    map.put("t_131m", 0.0);
    map.put("t_2m", 0.0);
    map.put("t_g", 0.0);
    map.put("u_10m", 0.0);
    map.put("u_131m", 0.0);
    map.put("u_20m", 0.0);
    map.put("u_216m", 0.0);
    map.put("u_65m", 0.0);
    map.put("v_10m", 0.0);
    map.put("v_131m", 0.0);
    map.put("v_20m", 0.0);
    map.put("v_216m", 0.0);
    map.put("v_65m", 0.0);
    map.put("w_131m", 0.0);
    map.put("w_20m", 0.0);
    map.put("w_216m", 0.0);
    map.put("w_65m", 0.0);
    map.put("z0", 0.0);
    for (ICONWeatherModel entity : entities) {
      map.put("alb_rad", map.get("alb_rad") + entity.getAlb_rad());
      map.put("asob_s", map.get("asob_s") + entity.getAsob_s());
      map.put("aswdifd_s", map.get("aswdifd_s") + entity.getAswdifd_s());
      map.put("aswdifu_s", map.get("aswdifu_s") + entity.getAswdifu_s());
      map.put("aswdirs_s", map.get("aswdirs_s") + entity.getAswdirs_s());
      map.put("sobs_rad", map.get("sobs_rad") + entity.getSobs_rad());
      map.put("p_20m", map.get("p_20m") + entity.getP_20m());
      map.put("p_65m", map.get("p_65m") + entity.getP_65m());
      map.put("p_131m", map.get("p_131m") + entity.getP_131m());
      map.put("t_131m", map.get("t_131m") + entity.getT_131m());
      map.put("t_2m", map.get("t_2m") + entity.getT_2m());
      map.put("t_g", map.get("t_g") + entity.getT_g());
      map.put("u_10m", map.get("u_10m") + entity.getU_10m());
      map.put("u_131m", map.get("u_131m") + entity.getU_131m());
      map.put("u_20m", map.get("u_20m") + entity.getU_20m());
      map.put("u_216m", map.get("u_216m") + entity.getU_216m());
      map.put("u_65m", map.get("u_65m") + entity.getU_65m());
      map.put("v_10m", map.get("v_10m") + entity.getV_10m());
      map.put("v_131m", map.get("v_131m") + entity.getV_131m());
      map.put("v_20m", map.get("v_20m") + entity.getV_20m());
      map.put("v_216m", map.get("v_216m") + entity.getV_216m());
      map.put("v_65m", map.get("v_65m") + entity.getV_65m());
      map.put("w_131m", map.get("w_131m") + entity.getW_131m());
      map.put("w_20m", map.get("w_20m") + entity.getW_20m());
      map.put("w_216m", map.get("w_216m") + entity.getW_216m());
      map.put("w_65m", map.get("w_65m") + entity.getW_65m());
      map.put("z0", map.get("z0") + entity.getZ0());
    }
    return map;
  }

  public ICONWeatherModel generateWeatherEntity() {
    return generateWeatherEntity(
        alb_rad,
        asob_s,
        aswdifd_s,
        aswdifu_s,
        aswdirs_s,
        sobs_rad,
        p_20m,
        p_65m,
        p_131m,
        t_131m,
        t_2m,
        t_g,
        u_10m,
        u_131m,
        u_20m,
        u_216m,
        u_65m,
        v_10m,
        v_131m,
        v_20m,
        v_216m,
        v_65m,
        w_131m,
        w_20m,
        w_216m,
        w_65m,
        z0,
        date,
        coordinate);
  }

  public ICONWeatherModel generateWeatherEntity(
      Double alb_rad,
      Double asob_s,
      Double aswdifd_s,
      Double aswdifu_s,
      Double aswdirs_s,
      Double sobs_rad,
      Double p_20m,
      Double p_65m,
      Double p_131m,
      Double t_131m,
      Double t_2m,
      Double t_g,
      Double u_10m,
      Double u_131m,
      Double u_20m,
      Double u_216m,
      Double u_65m,
      Double v_10m,
      Double v_131m,
      Double v_20m,
      Double v_216m,
      Double v_65m,
      Double w_131m,
      Double w_20m,
      Double w_216m,
      Double w_65m,
      Double z0,
      ZonedDateTime date,
      CoordinateModel coordinate) {
    ICONWeatherModel newWeather = new ICONWeatherModel(date, coordinate);
    newWeather.setAlb_rad(alb_rad);
    newWeather.setAsob_s(asob_s);
    newWeather.setAswdifd_s(aswdifd_s);
    newWeather.setAswdifu_s(aswdifu_s);
    newWeather.setAswdirs_s(aswdirs_s);
    newWeather.setSobs_rad(sobs_rad);
    newWeather.setP_20m(p_20m);
    newWeather.setP_65m(p_65m);
    newWeather.setP_131m(p_131m);
    newWeather.setT_131m(t_131m);
    newWeather.setT_2m(t_2m);
    newWeather.setT_g(t_g);
    newWeather.setU_10m(u_10m);
    newWeather.setU_131m(u_131m);
    newWeather.setU_20m(u_20m);
    newWeather.setU_216m(u_216m);
    newWeather.setU_65m(u_65m);
    newWeather.setV_10m(v_10m);
    newWeather.setV_131m(v_131m);
    newWeather.setV_20m(v_20m);
    newWeather.setV_216m(v_216m);
    newWeather.setV_65m(v_65m);
    newWeather.setW_131m(w_131m);
    newWeather.setW_20m(w_20m);
    newWeather.setW_216m(w_216m);
    newWeather.setW_65m(w_65m);
    newWeather.setZ0(z0);
    return newWeather;
  }
}
