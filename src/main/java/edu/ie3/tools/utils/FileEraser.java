/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools.utils;

import static edu.ie3.tools.utils.ConfigurationParameters.FILENAME_DATE_FORMATTER;

import edu.ie3.tools.Main;
import edu.ie3.tools.models.persistence.FileModel;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class FileEraser {

  private final String folderpath;
  private final edu.ie3.tools.utils.DatabaseController dbController;

  public static final Logger filestatusLogger = LogManager.getLogger("FileStatus");

  public FileEraser(String directory, edu.ie3.tools.utils.DatabaseController dbController) {
    filestatusLogger.setLevel(Main.filestatus ? Level.ALL : Level.OFF);
    this.dbController = dbController;
    folderpath = directory + File.separator;
  }

  public Callable<Void> eraseCallable(final FileModel file) {
    return () -> {
      erase(file);
      return null;
    };
  }

  private void erase(@NotNull FileModel file) {
    String modelrunFolderPath = FILENAME_DATE_FORMATTER.format(file.getModelrun()) + File.separator;
    String fullDirectoryPath = folderpath + modelrunFolderPath;

    // delete archive file
    if (file.isDecompressed()) {
      String filename = file.getBZ2FileName();
      file.setArchivefile_deleted(eraseFile(fullDirectoryPath + filename));
      filestatusLogger.trace(
          file.getName()
              + "  |  ad"
              + (file.isArchivefile_deleted()
                  ? "t  |  archivefile_deleted = true"
                  : "f  |  archivefile_deleted = false")
              + "|  FileEraser");
    }

    // delete grib file
    if (file.isPersisted()
        || (file.isValid_file() != null && file.isValid_file() != true)
        || file.isSufficient_size() == false) {
      String filename = file.getGRIB2FileName();
      file.setGribfile_deleted(eraseFile(fullDirectoryPath + filename));
      filestatusLogger.trace(
          file.getName()
              + "  |  gd"
              + (file.isGribfile_deleted()
                  ? "t  |  gribfile_deleted = true"
                  : "f  |  gribfile_deleted = false")
              + "|  FileEraser");

      file.setDecompressed(false);
      filestatusLogger.trace(file.getName() + "  |  df   |  decompressed = false  | FileEraser");
    }
  }

  public boolean eraseFile(String filename) {
    File f = new File(filename);
    if (f.exists()) {
      return f.delete();
    } else return true;
  }

  public int eraseInvalidFiles() {
    List<FileModel> files =
        dbController.execNamedQuery(FileModel.InvalidFiles, Collections.emptyList());
    int deleted = 0;
    for (FileModel file : files) {
      String filename = file.getName();
      String modelrunFolderPath =
          FILENAME_DATE_FORMATTER.format(file.getModelrun()) + File.separator;
      String fullDirectoryPath = folderpath + modelrunFolderPath;
      if (eraseFile(fullDirectoryPath + filename)) deleted++;
    }
    return deleted;
  }
}
