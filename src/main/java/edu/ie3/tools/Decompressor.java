/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools;

import edu.ie3.tools.models.persistence.FileModel;
import edu.ie3.tools.utils.enums.Parameter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.Callable;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class Decompressor implements Callable<Boolean> {

  public static final Logger logger = LogManager.getLogger(Decompressor.class);
  public static final Logger filestatusLogger = LogManager.getLogger("FileStatus");

  private FileModel file;
  private String folderpath;

  public Decompressor(FileModel file, String folderpath) {
    filestatusLogger.setLevel(Main.filestatus ? Level.ALL : Level.OFF);
    logger.setLevel(Main.debug ? Level.ALL : Level.INFO);
    this.file = file;
    this.folderpath = folderpath;
  }

  /**
   * Decompresses the bz2 archive file of the referenced FileModel using ByteStreams.
   *
   * @return success of decompression, including plausible file size
   */
  public static boolean decompress(@NotNull FileModel file, String folderpath) {
    boolean success = true;

    String filenameFrom = folderpath + file.getBZ2FileName();
    String filenameTo = folderpath + file.getGRIB2FileName();

    try (FileInputStream in = new FileInputStream(filenameFrom);
        FileOutputStream out = new FileOutputStream(filenameTo);
        BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in)) {
      IOUtils.copy(bzIn, out);
    } catch (FileNotFoundException e) {
      Parameter parameter = file.getParameter();
      logger.warn(
          Converter.getFormattedTimestep(file) + "File not found for parameter " + parameter);
      file.setArchivefile_deleted(true);
      filestatusLogger.trace(
          file.getName() + "  |  adt  |  archivefile_deleted = true  |  File not Found");

      success = false;
    } catch (Exception e) {
      String formattedTimestep = Converter.getFormattedTimestep(file);
      logger.error(formattedTimestep, e);
      success = false;
    }
    if (success) {
      file.setDecompressed(true);
      filestatusLogger.trace(file.getName() + "  |  dt   | decompressed = true  |  Decompressor");
      logger.trace("File " + file.getName() + " successfully decompressed");
    }
    return success;
  }

  @Override
  public Boolean call() {
    return decompress(file, folderpath);
  }
}
