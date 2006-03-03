package gov.nih.mipav.model.file;

import gov.nih.mipav.view.dialogs.*;
import gov.nih.mipav.model.structures.*;

/**
*   This structure contains the information that describes how
*   a RAW image is stored on disk.
*
*		@version    0.1 May 5, 1997
*		@author     Matthew J. McAuliffe, Ph.D.
*       @see        FileRaw
*
*
*/

public class FileInfoRaw extends FileInfoBase {

    /**
    *  FileInfoRaw
    *  @param name        file name
    *  @param directory   file directory
    *  @param format      file format (RAW)
    */
    public FileInfoRaw(String name, String directory, int format) {
        super(name, directory, format);
    }

    /**
    *  Displays the file information
    *  @param dlog    dialog box that is written to
    *  @param matrix  transformation matrix
    */
    public void displayAboutInfo(JDialogBase dlog, TransMatrix matrix){
        JDialogText dialog = (JDialogText) dlog;
        displayPrimaryInfo(dialog, matrix);
    }
}
