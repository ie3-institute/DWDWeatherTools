/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools.models.persistence;

import static edu.ie3.tools.utils.ConfigurationParameters.FILENAME_DATE_FORMATTER;

import edu.ie3.tools.Main;
import edu.ie3.tools.utils.ConfigurationParameters;
import edu.ie3.tools.utils.enums.Parameter;
import java.io.File;
import java.io.Serializable;
import java.time.ZonedDateTime;
import javax.persistence.*;

@NamedQueries({
  @NamedQuery(
      name = "FileModel.NewestDownloadedModelrun",
      query = "SELECT MAX(file.modelrun) FROM files file"),
  @NamedQuery(
      name = "FileModel.OldestModelrunWithUnprocessedFiles",
      query =
          "SELECT MIN(file.modelrun) FROM files file WHERE sufficient_size=true "
              + "AND persisted=false AND (valid_file is null or valid_file=true)"),
  @NamedQuery(
      name = "FileModel.FailedDownloads",
      query =
          "SELECT file FROM files file WHERE modelrun>= ?1 AND "
              + "(sufficient_size=false OR valid_file=false) ORDER BY modelrun"),
  @NamedQuery(
      name = "FileModel.InvalidFiles",
      query =
          "SELECT file FROM files file WHERE (sufficient_size=false OR valid_file=false) AND archivefile_deleted=false")
})
@Entity(name = "files")
public class FileModel implements Serializable {

  /** Selects newest (max) modelrun */
  public static final String NEWEST_DOWNLOADED_MODELRUN = "FileModel.NewestDownloadedModelrun";
  /**
   * Selects the oldest (min) modelrun, which has a sufficient file size, but hasn't been processed
   * yet
   */
  public static final String OLDEST_MODELRUN_WITH_UNPROCESSED_FILES =
      "FileModel.OldestModelrunWithUnprocessedFiles";
  /** Selects files, that have been too small or invalid in previous runs */
  public static final String FAILED_DOWNLOADS = "FileModel.FailedDownloads";
  /** Selects files, that are invalid and not yet deleted */
  public static final String INVALID_FILES = "FileModel.InvalidFiles";

  public static final String PREFIX = "icon-eu_europe_regular-lat-lon_";
  public static final String PREFIX_SINGLE_LEVEL = PREFIX + "single-level_";
  public static final String PREFIX_MULTI_LEVEL = PREFIX + "model-level_";

  @Id
  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false)
  private ZonedDateTime modelrun;

  @Column(nullable = false)
  private int timestep;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Parameter parameter;

  @Column(name = "download_fails")
  private int downloadFails;

  @Column(name = "sufficient_size")
  private boolean sufficientSize;

  @Column(name = "download_date")
  private ZonedDateTime downloadDate;

  @Column private boolean decompressed;

  @Column(name = "missing_coordinates")
  private int missingCoordinates;

  @Column(name = "valid_file")
  private Boolean validFile;

  @Column private boolean persisted;

  @Column(name = "archivefile_deleted")
  private boolean archiveFileDeleted;

  @Column(name = "gribfile_deleted")
  private boolean isGribFileDeleted;

  public FileModel(ZonedDateTime modelrun, int timestep, Parameter parameter) {
    this.modelrun = modelrun;
    this.timestep = timestep;
    this.parameter = parameter;
    this.name = createFileName(modelrun, timestep, parameter);
  }

  /**  @deprecated (only for persistence purposes) */
  @Deprecated
  public FileModel() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public static String createFileName(ZonedDateTime modelrun, int timestep, Parameter parameter) {
    String name = parameter.getPrefix(); // ie icon-eu_europe_regular-lat-lon_single-level_
    name += FILENAME_DATE_FORMATTER.format(modelrun) + "_"; // ie 2018090512_
    name += String.format("%03d", timestep) + "_"; // three-digit timestep eg 011_
    name += parameter.getIconName(); // ie ASWDIFD_S
    return name;
  }

  public ZonedDateTime getModelrun() {
    return modelrun;
  }

  public int getTimestep() {
    return timestep;
  }

  public void setTimestep(int timestep) {
    this.timestep = timestep;
  }

  public Parameter getParameter() {
    return parameter;
  }

  public void setParameter(Parameter parameter) {
    this.parameter = parameter;
  }

  public int getDownloadFails() {
    return downloadFails;
  }

  public void setDownloadFails(int download_fails) {
    this.downloadFails = download_fails;
  }

  public void incrementDownloadFails() {
    this.downloadFails++;
  }

  public boolean isSufficientSize() {
    return sufficientSize;
  }

  public void setSufficientSize(boolean sufficient_size) {
    this.sufficientSize = sufficient_size;
  }

  public ZonedDateTime getDownloadDate() {
    return downloadDate;
  }

  public void setDownloadDate(ZonedDateTime download_date) {
    this.downloadDate = download_date;
  }

  public boolean isDecompressed() {
    return decompressed;
  }

  public void setDecompressed(boolean decompressed) {
    this.decompressed = decompressed;
  }

  public int getMissingCoordinates() {
    return missingCoordinates;
  }

  public void setMissingCoordinates(int missingCoordinates) {
    this.missingCoordinates = missingCoordinates;
  }

  public void addMissingCoordinate() {
    this.missingCoordinates++;
  }

  public Boolean isValidFile() {
    return validFile;
  }

  public void setValidFile(Boolean valid_file) {
    this.validFile = valid_file;
  }

  public boolean isPersisted() {
    return persisted;
  }

  public void setPersisted(boolean persisted) {
    this.persisted = persisted;
  }

  public boolean isArchiveFileDeleted() {
    return archiveFileDeleted;
  }

  public void setArchiveFileDeleted(boolean archiveFileDeleted) {
    this.archiveFileDeleted = archiveFileDeleted;
  }

  public boolean isGribFileDeleted() {
    return isGribFileDeleted;
  }

  public void setGribFileDeleted(boolean isGribFileDeleted) {
    this.isGribFileDeleted = isGribFileDeleted;
  }

  public void setModelrun(ZonedDateTime modelrun) {
    this.modelrun = modelrun;
  }

  public String createFileName() {
    return createFileName(modelrun, timestep, parameter);
  }

  public String getBZ2FileName() {
    return getGRIB2FileName() + ".bz2";
  }

  public String getGRIB2FileName() {
    return name + ".grib2";
  }

  public String getTextFileName() {
    return name + "txt";
  }

  public String getURL() {
    String url = ConfigurationParameters.URL;
    url += String.format("%02d", modelrun.getHour()) + "/";
    url += parameter.getParameterName().toLowerCase() + "/";
    url += getBZ2FileName();
    return url;
  }

  public File getBZ2File() {
    return getBZ2File(Main.directory + File.separator + FILENAME_DATE_FORMATTER.format(modelrun));
  }

  public File getBZ2File(String folderpath) {
    return new File(folderpath + File.separator + getBZ2FileName());
  }

  public File getGRIB22File() {
    return getGRIB22File(
        Main.directory + File.separator + FILENAME_DATE_FORMATTER.format(modelrun));
  }

  public File getGRIB22File(String folderpath) {
    return new File(folderpath + File.separator + getGRIB2FileName());
  }
}
