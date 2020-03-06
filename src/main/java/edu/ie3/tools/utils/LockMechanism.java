/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools.utils;

import edu.ie3.tools.Main;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LockMechanism {

  private File file;
  private FileChannel channel;
  private FileLock lock;
  private String appName;
  private Path pathToLock;

  public LockMechanism(String appName) {
    this.appName = appName;
    pathToLock = Paths.get(Main.directory).toAbsolutePath().getParent();
  }

  public boolean isAppActive() {
    try {
      file = new File(pathToLock.toFile(), appName + ".lock");
      channel = new RandomAccessFile(file, "rw").getChannel();

      try {
        lock = channel.tryLock();
      } catch (OverlappingFileLockException e) {
        // already locked
        closeLock();
        return true;
      }

      if (lock == null) {
        closeLock();
        return true;
      }

      // destroy the lock when the JVM is closing
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    closeLock();
                    deleteFile();
                  }));
      return false;
    } catch (Exception e) {
      closeLock();
      return true;
    }
  }

  private void closeLock() {
    try {
      lock.release();
    } catch (Exception e) {
    }
    try {
      channel.close();
    } catch (Exception e) {
    }
  }

  private void deleteFile() {
    try {
      file.delete();
    } catch (Exception e) {
    }
  }
}
