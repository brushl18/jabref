package net.sf.jabref.collab;

import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * This thread monitors a set of files, each associated with a FileUpdateListener, for changes
* in the file's last modification time stamp. The
 */
public class FileUpdateMonitor extends Thread {

  final int WAIT = 5000;
  int no = 0;
  HashMap entries = new HashMap();
  boolean running;

  public FileUpdateMonitor() {
    setPriority(MIN_PRIORITY);
  }

  public void run() {
    running = true;

    // The running variable is used to make the thread stop when needed.
    while (running) {
      //System.out.println("Polling...");
      Iterator i = entries.keySet().iterator();
      for (;i.hasNext();) {
        Entry e = (Entry)entries.get(i.next());
        try {
          if (e.hasBeenUpdated())
            e.notifyListener();

          //else
          //System.out.println("File '"+e.file.getPath()+"' not modified.");
        } catch (IOException ex) {
          e.notifyFileRemoved();
        }
      }

      // Sleep for a while before starting a new polling round.
      try {
        sleep(WAIT);
      } catch (InterruptedException ex) {
      }
    }
  }

  /**
   * Cause the thread to stop monitoring. It will finish the current round before stopping.
   */
  public void stopMonitoring() {
    running = false;
  }

  /**
   * Add a new file to monitor. Returns a handle for accessing the entry.
   * @param ul FileUpdateListener The listener to notify when the file changes.
   * @param file File The file to monitor.
   * @throws IOException if the file does not exist.
   */
  public String addUpdateListener(FileUpdateListener ul, File file) throws IOException {
    if (!file.exists())
      throw new IOException("File not found");
    no++;
    String key = ""+no;
    entries.put(key, new Entry(ul, file));
    return key;
  }

  /**
   * Removes a listener from the monitor.
   * @param handle String The handle for the listener to remove.
   */
  public void removeUpdateListener(String handle) {
    entries.remove(handle);
  }

  public void updateTimeStamp(String key) throws IllegalArgumentException {
    Object o = entries.get(key);
    if (o == null)
      throw new IllegalArgumentException("Entry not found");
    ((Entry)o).updateTimeStamp();
  }

  public void changeFile(String key, File file) throws IOException, IllegalArgumentException {
    if (!file.exists())
      throw new IOException("File not found");
    Object o = entries.get(key);
    if (o == null)
      throw new IllegalArgumentException("Entry not found");
    ((Entry)o).file = file;
  }

  /**
   * A class containing the File, the FileUpdateListener and the current time stamp for one file.
   */
  class Entry {
    FileUpdateListener listener;
    File file;
    long timeStamp;

    public Entry(FileUpdateListener ul, File f) {
      listener = ul;
      file = f;
      timeStamp = file.lastModified();
    }

    /**
     * Check if time stamp has changed.
     * @throws IOException if the file does no longer exist.
     * @return boolean true if the file has changed.
     */
    public boolean hasBeenUpdated() throws IOException {
      long modified = file.lastModified();
      if (modified == 0L)
        throw new IOException("File deleted");
      return timeStamp != modified;
    }

    public void updateTimeStamp() {
      timeStamp = file.lastModified();
      if (timeStamp == 0L)
        notifyFileRemoved();
    }

    /**
     * Call the listener method to signal that the file has changed.
     */
    public void notifyListener() {
      // Update time stamp.
      timeStamp = file.lastModified();
      listener.fileUpdated();
    }

    /**
     * Call the listener method to signal that the file has been removed.
     */
    public void notifyFileRemoved() {
      listener.fileRemoved();
    }
  }

}
