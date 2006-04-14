package gov.nih.mipav.model.file;


import java.io.File;


/**
 * Class to safely delete a file using a looped thread.
 */
public class FileDeleter extends Thread {
    private File target;

    private String filename;

    /**
     * Construct the file deleter.
     * @param filename the file to delete
     */
    public FileDeleter(String filename) {
        this.filename = filename;
    }

    /**
     * Run method: if the file exists, try deleting it. if the delete fails, then sleep for 2 seconds and start again.
     */
    public void run() {
        try {
            // add filename to file list vector
            target = new File(filename);
            // keep trying to delete file every 2000 milliseconds
            while (true) {
                if (target.exists()) {
                    if ( !target.delete())
                        Thread.sleep(2000);
                } else {
                    target = null;
                    System.gc();
                    // System.err.println("Successfully deleted file");
                    break;
                }
            }
        } catch (Exception ex) {
            target = null;
        }
    }
}
