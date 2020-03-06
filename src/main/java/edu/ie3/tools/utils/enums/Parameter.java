/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools.utils.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum Parameter {

  /** Albedo in % */
  ALBEDO("ALB_RAD", 0),
  /** Net short-wave radiation flux at surface in W/m² */
  ASOB_S("ASOB_S", 0),
  /** Surface down solar diffuse radiation in W/m² */
  DIFS_D("ASWDIFD_S", 0),
  /** Surface up diffuse radiation in W/m² */
  DIFS_U("ASWDIFU_S", 0),
  /** Direct radiation in W/m² */
  DIRS("ASWDIR_S", 0),
  /** Pressure at 20m above ground in Pa */
  P_20M("P", 60),
  /** Pressure at 65m above ground in Pa */
  P_65M("P", 59),
  /** Pressure at 131m above ground in Pa */
  P_131M("P", 58),
  /** Net short-wave radiation flux at surface (instantaneous) in W/m² */
  SOBS_RAD("SOBS_RAD", 0),
  /** Ground temperature in K */
  T_G("T_G", 0),
  /** Temperature at 2m above ground in K */
  T_2M("T_2M", 0),
  /** Temperature at 131m above ground in K */
  T_131M("T", 58),
  /** Zonal wind at 10m above ground in m/s */
  U_10M("U_10M", 0),
  /** Zonal wind at 20m above ground in m/s */
  U_20M("U", 60),
  /** Zonal wind at 65m above ground in m/s */
  U_65M("U", 59),
  /** Zonal wind at 131m above ground in m/s */
  U_131M("U", 58),
  /** Zonal wind at 216m above ground in m/s */
  U_216M("U", 57),
  /** Meridional wind at 10m above ground in m/s */
  V_10M("V_10M", 0),
  /** Meridional wind at 20m above ground in m/s */
  V_20M("V", 60),
  /** Meridional wind at 65m above ground in m/s */
  V_65M("V", 59),
  /** Meridional wind at 131m above ground in m/s */
  V_131M("V", 58),
  /** Meridional wind at 216m above ground in m/s */
  V_216M("V", 57),
  /** Vertical wind at 20m above ground in m/s */
  W_20M("W", 60),
  /** Vertical wind at 65m above ground in m/s */
  W_65M("W", 59),
  /** Vertical wind at 131m above ground in m/s */
  W_131M("W", 58),
  /** Vertical wind at 216m above ground in m/s */
  W_216M("W", 57),
  /** Surface roughness in m */
  Z0("Z0", 0);

  public static final int MIN_SIZE = 10000;

  public static final String PREFIX = "icon-eu_europe_regular-lat-lon_";
  public static final String PREFIX_SINGLE_LEVEL = PREFIX + "single-level_";
  public static final String PREFIX_MULTI_LEVEL = PREFIX + "model-level_";

  /**
   * Height levels in meter according to
   * https://isabel.dwd.de/DWD/forschung/nwv/fepub/icon_database_main.pdf#page=78 <br>
   * Level 57 = 216.516m <br>
   * Level 58 = 131.880m <br>
   * Level 59 = 65.677m <br>
   * Level 60 = 20m <br>
   * (Used as bottom level)
   */
  private static final Map<Integer, Integer> levelToHeight;

  static {
    Map<Integer, Integer> aMap = new HashMap<>();
    aMap.put(57, 216); // 216.516m
    aMap.put(58, 131); // 131.880m
    aMap.put(59, 65); // 65.677m
    aMap.put(60, 20); // 20m
    levelToHeight = Collections.unmodifiableMap(aMap);
  }

  private final String parameterName;
  private final int heightLevel;

  Parameter(String parameterName, int heightLevel) {
    this.parameterName = parameterName;
    this.heightLevel = heightLevel;
  }

  @Override
  public String toString() {
    return parameterName + (isMultiLevel() ? "_" + levelToHeight.get(heightLevel) + "M" : "");
  }

  public String getParameterName() {
    return parameterName;
  }

  public String getIconName() {
    return isSingleLevel() ? parameterName : heightLevel + "_" + parameterName;
  }

  public int getHeightLevel() {
    return heightLevel;
  }

  /**
   * Height level in meter according to
   * https://isabel.dwd.de/DWD/forschung/nwv/fepub/icon_database_main.pdf#page=78 <br>
   * Level 57 = 216.516m <br>
   * Level 58 = 131.880m <br>
   * Level 59 = 65.677m <br>
   * Level 60 = 20m <br>
   * (Used as bottom level)
   */
  public int getHeightInMeter() {
    return levelToHeight.get(heightLevel);
  }

  public boolean isSingleLevel() {
    return heightLevel == 0;
  }

  public boolean isMultiLevel() {
    return !isSingleLevel();
  }

  public String getPrefix() {
    return isSingleLevel() ? PREFIX_SINGLE_LEVEL : PREFIX_MULTI_LEVEL;
  }
}
