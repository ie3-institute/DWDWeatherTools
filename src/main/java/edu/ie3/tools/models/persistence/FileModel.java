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
  public static final String NewestDownloadedModelrun = "FileModel.NewestDownloadedModelrun";
  /**
   * Selects the oldest (min) modelrun, which has a sufficient file size, but hasn't been processed
   * yet
   */
  public static final String OldestModelrunWithUnprocessedFiles =
      "FileModel.OldestModelrunWithUnprocessedFiles";
  /** Selects files, that have been too small or invalid in previous runs */
  public static final String FailedDownloads = "FileModel.FailedDownloads";
  /** Selects files, that are invalid and not yet deleted */
  public static final String InvalidFiles = "FileModel.InvalidFiles";

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

  @Column private int download_fails;

  @Column private boolean sufficient_size;

  @Column private ZonedDateTime download_date;

  @Column private boolean decompressed;

  @Column private int missing_coordinates;

  @Column private Boolean valid_file;

  @Column private boolean persisted;

  @Column private boolean archivefile_deleted;

  @Column private boolean gribfile_deleted;

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

  /** Creates the correct file name within the icon model for a parameter within a given time step for a given model run
   *
   * @param modelrun the model run to consider
   * @param timestep the time step to check
   * @param parameter the considered parameter
   * @return a String of the expected file name
   */
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
    return download_fails;
  }

  public void setDownloadFails(int download_fails) {
    this.download_fails = download_fails;
  }

  public void incrementDownload_fails() {
    this.download_fails++;
  }

  public boolean isSufficient_size() {
    return sufficient_size;
  }

  public void setSufficient_size(boolean sufficient_size) {
    this.sufficient_size = sufficient_size;
  }

  public ZonedDateTime getDownload_date() {
    return download_date;
  }

  public void setDownload_date(ZonedDateTime download_date) {
    this.download_date = download_date;
  }

  public boolean isDecompressed() {
    return decompressed;
  }

  public void setDecompressed(boolean decompressed) {
    this.decompressed = decompressed;
  }

  public int getMissing_coordinates() {
    return missing_coordinates;
  }

  public void setMissing_coordinates(int missing_coordinates) {
    this.missing_coordinates = missing_coordinates;
  }

  public void addMissing_coordinate() {
    this.missing_coordinates++;
  }

  public Boolean isValid_file() {
    return valid_file;
  }

  public void setValid_file(Boolean valid_file) {
    this.valid_file = valid_file;
  }

  public boolean isPersisted() {
    return persisted;
  }

  public void setPersisted(boolean persisted) {
    this.persisted = persisted;
  }

  /** Checks if the raw file downloaded from the model run is deleted.
   *
   * @return whether it is deleted or not
   */
  public boolean isArchivefile_deleted() {
    return archivefile_deleted;
  }

  public void setArchivefile_deleted(boolean archivefile_deleted) {
    this.archivefile_deleted = archivefile_deleted;
  }

  public boolean isGribfile_deleted() {
    return gribfile_deleted;
  }

  public void setGribfile_deleted(boolean gribfile_deleted) {
    this.gribfile_deleted = gribfile_deleted;
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
