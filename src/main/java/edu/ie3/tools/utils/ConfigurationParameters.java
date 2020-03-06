/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools.utils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ConfigurationParameters {

  private ConfigurationParameters() {
    throw new IllegalStateException("Utility class");
  }

  /** Needed for JPA/ Hibernate */
  public static final String PERSISTENCE_UNIT_NAME = "dwdtools";

  /**
   * Only for logger output - Formats according to pattern "dd.MM.YYYY HH:mm zzz" incl. time zone
   * information
   */
  public static final DateTimeFormatter MODEL_RUN_FORMATTER =
      DateTimeFormatter.ofPattern("dd.MM.YYYY HH:mm zzz");

  /**
   * Only for sql output - this convert's a given ZonedDateTime to LocalDateTime neglecting the
   * timezone. This can be done as all{@link ZonedDateTime} processed in this application <b>must be
   * in UTC</b> as the input data is in UTC as well and hence the data in the database is assumed to
   * be always UTC. Postgres can process the resulting string directly - see
   * https://jdbc.postgresql.org/documentation/head/8-date-time.html for details
   */
  public static final String SQL_FORMATTER(ZonedDateTime zonedDateTime) {
    return zonedDateTime.toLocalDateTime().toString();
  }

  /** For URL and filename generation */
  public static final DateTimeFormatter FILENAME_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHH");

  public static final String URL = "https://opendata.dwd.de/weather/nwp/icon-eu/grib/";
  public static final String LEGACY_URL = "https://opendata.dwd.de/weather/icon/eu_nest/grib/";
}
