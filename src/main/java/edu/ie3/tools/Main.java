/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import picocli.CommandLine;

@CommandLine.Command(
    name = "dwdtools",
    description =
        "Provides functions for the download, extraction and conversion of DWD GRIB2-Files",
    mixinStandardHelpOptions = true,
    version = "0.4",
    header =
        "\n"
            + "\n"
            + "██████╗ ██╗    ██╗██████╗     ████████╗ ██████╗  ██████╗ ██╗     ███████╗\n"
            + "██╔══██╗██║    ██║██╔══██╗    ╚══██╔══╝██╔═══██╗██╔═══██╗██║     ██╔════╝\n"
            + "██║  ██║██║ █╗ ██║██║  ██║       ██║   ██║   ██║██║   ██║██║     ███████╗\n"
            + "██║  ██║██║███╗██║██║  ██║       ██║   ██║   ██║██║   ██║██║     ╚════██║\n"
            + "██████╔╝╚███╔███╔╝██████╔╝       ██║   ╚██████╔╝╚██████╔╝███████╗███████║\n"
            + "╚═════╝  ╚══╝╚══╝ ╚═════╝        ╚═╝    ╚═════╝  ╚═════╝ ╚══════╝╚══════╝\n"
            + "                                                                         \n"
            + "\n\n",
    footer = {
      "\n\n© 2019 Technische Universität Dortmund",
      "Institut für Energiesysteme, Energieeffizienz und Energiewirtschaft",
      "Forschungsgruppe Verteilnetzplanung und -betrieb"
    })
public class Main {

  @CommandLine.Option(
      names = {"-d", "--dir", "--directory"},
      description =
          "The directory used to write downloads to or read files from. Default: downloads")
  public static String directory = "downloads";

  @CommandLine.Option(
      names = {"--timesteps"},
      description = "Number of timesteps ( = hours) to process in a modelrun. Default: 12")
  public static int timesteps = 12;

  @CommandLine.Option(
      names = {"-t", "--tolerance", "--fault_tolerance"},
      description = "Fault tolerance, e.g. missing coordinates. Default: 0.33")
  public static double faultTolerance = 0.33;

  @CommandLine.Option(
      names = {"convert"},
      description = "Converts grib2 files")
  public static boolean doConvert;

  @CommandLine.Option(
      names = {"-convert_from"},
      description = "Start datetime of conversion. Format: yyyy-MM-dd HH:mm:ss")
  public static String convertFrom;

  @CommandLine.Option(
      names = {"-convert_until"},
      description = "End datetime of conversion. Format: yyyy-MM-dd HH:mm:ss")
  public static String convertUntil;

  @CommandLine.Option(
      names = {"download"},
      description = "Downloads grib2 files")
  public static boolean doDownload;

  @CommandLine.Option(
      names = {"-i", "--interpolation_ratio"},
      description =
          "The ratio at which data is interpolated, at a value of 1, previous value will be overriden. Default: 0.67")
  public static double interpolationRatio = 0.67; // Overrides previous values if set to 1

  @CommandLine.Option(
      names = {"-del", "--delete"},
      description = "Delete downloaded files after conversion. Default: false")
  public static boolean deleteDownloadedFiles = false;

  @CommandLine.Option(
      names = {"-user", "--database_user"},
      description = "Database user")
  public static String databaseUser = "";

  @CommandLine.Option(
      names = {"-pwd", "--database_password"},
      description = "Database password")
  public static String databasePassword = "";

  @CommandLine.Option(
      names = {"-conn", "--connection_url"},
      description = "Database connection url. Default: jdbc:postgresql://127.0.0.1:5432/dwd")
  public static String connectionUrl = "jdbc:postgresql://127.0.0.1:5432/dwd";

  @CommandLine.Option(
      names = {"-schema"},
      description = "default database schema")
  public static String database_schema = "icon";

  @CommandLine.Option(
      names = {"-m", "--missing_value_string"},
      description = "Configure the NULL-Value-String to set and parse in textfile")
  public static String missingValue = "null";

  @CommandLine.Option(
      names = {"-eccodes", "--eccodes_location"},
      description = "The location of the eccodes commands")
  public static String eccodes = "/usr/local/bin/grib_get_data";

  @CommandLine.Option(
      names = {"-filestatus"},
      description = "Write file status changes into FileStatus.log")
  public static boolean filestatus = false;

  @CommandLine.Option(
      names = {"debug"},
      description = "Create debugging output")
  public static boolean debug = false;

  // Coordinate-Rectangle to convert
  public static final double MIN_LONGITUDE = 4.29694;
  public static final double MAX_LONGITUDE = 18.98635;
  public static final double MIN_LATITUDE = 45.71457;
  public static final double MAX_LATITUDE = 57.65129;

  public static void main(String[] args) {
    // ICON-EU data timezone is UTC time,
    // hence we need to enforce this timezone for the whole application
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    CommandLine commandLine = new CommandLine(new Main());
    commandLine.parseArgs(args);
    if (commandLine.isUsageHelpRequested() || !(doDownload || doConvert)) {
      commandLine.usage(System.out);
      return;
    } else if (commandLine.isVersionHelpRequested()) {
      commandLine.printVersionHelp(System.out);
      return;
    }
    directory = directory.replace("\"", "").replace("'", "");
    if (directory.endsWith(File.separator))
      directory = directory.substring(0, directory.length() - 1).trim();
    eccodes = eccodes.replace("\"", "").replace("'", "");
    if (eccodes.endsWith(File.separator))
      eccodes = eccodes.substring(0, eccodes.length() - 1).trim();
    if (!eccodes.endsWith("grib_get_data")) {
      if (!eccodes.isEmpty()) eccodes += File.separator;
      eccodes += "grib_get_data";
    }

    if (doConvert) {
      if (convertFrom != null && convertUntil != null) {
        DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("UTC"))
                .withLocale(Locale.GERMANY);
        ZonedDateTime from = ZonedDateTime.parse(convertFrom, dateTimeFormatter);
        ZonedDateTime until = ZonedDateTime.parse(convertUntil, dateTimeFormatter);
        new Converter(from, until).run();
      } else if (convertFrom != null || convertUntil != null) {
        throw new IllegalArgumentException(
            "Either convertFrom or convertTo is missing. We need both to convert data for a specific interval");
      } else {
        new Downloader().run();
      }
    }
  }

  public static Set<String> printProgramArguments() {
    return printProgramArguments(false);
  }

  public static Set<String> printProgramArguments(boolean verbose) {
    HashSet<String> args = new HashSet<>();
    args.add("directory = \"" + Main.directory + "\"");
    if (verbose) args.add("timesteps = " + timesteps);
    if (verbose) args.add("faultTolerance = " + faultTolerance);
    args.add("interpolationRatio = " + interpolationRatio);
    if (verbose) args.add("databaseUser = \"" + databaseUser + "\"");
    args.add("connectionUrl = \"" + connectionUrl + "\"");
    if (verbose) args.add("missingValue = \"" + missingValue + "\"");
    args.add("eccodes location= \"" + eccodes + "\"");
    return args;
  }
}
