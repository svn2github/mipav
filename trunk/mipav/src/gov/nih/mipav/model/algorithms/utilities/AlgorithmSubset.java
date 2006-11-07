package gov.nih.mipav.model.algorithms.utilities;


import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.file.*;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.io.*;


/**
 * Algorithm to create a 3D subset image from a 4D image. The user specifies the dimension to remove - x, y, z, or t and
 * the value of the removed dimension.
 */
public class AlgorithmSubset extends AlgorithmBase {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Remove x dimension. */
    public static final int REMOVE_X = 0;

    /** Remove y dimension. */
    public static final int REMOVE_Y = 1;

    /** Remove z dimension. */
    public static final int REMOVE_Z = 2;

    /** Remove t dimension. */
    public static final int REMOVE_T = 3;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** Dimension to be removed. */
    private int removeDim;

    /** Slice value for removed dimension. */
    private int sliceNum;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * import source and destination images into the class.
     *
     * @param  srcImage   source image (image to clip from)
     * @param  destImage  destination image (image to paste to)
     * @param  removeDim  the dimension to be removed
     * @param  sliceNum   slice value for removed dimension
     */
    public AlgorithmSubset(ModelImage srcImage, ModelImage destImage, int removeDim, int sliceNum) {
        super(destImage, srcImage);
        this.removeDim = removeDim;
        this.sliceNum = sliceNum;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Prepares this class for destruction.
     */
    public void finalize() {
        super.finalize();
    }

    /**
     * Runs the algorithm.
     */
    public void runAlgorithm() {
        float[] destResolutions;
        int[] destUnitsOfMeasure;
        float[] imageBuffer;

        constructLog();

        try {
            destResolutions = new float[3];
            destUnitsOfMeasure = new int[3];
        } catch (OutOfMemoryError e) {
            imageBuffer = null;
            System.gc();
            displayError("AlgorithmSubset reports: Out of memory");
            setCompleted(false);
            return;
        }

        // No DICOM 4D images
        if (removeDim == REMOVE_T) {
            if ((srcImage.getFileInfo()[0]).getFileFormat() == FileUtility.DICOM) {
                
                // determine along which axis the imagePositionCoords the image varies
                if (srcImage.getFileInfo(1) != null) {
                    float[] imagePositionCoords = convertIntoFloat(((FileInfoDicom) srcImage.getFileInfo(0)).parseTagValue("0020,0032"));
                    float[] nextPositionCoords = convertIntoFloat(((FileInfoDicom) srcImage.getFileInfo(1)).parseTagValue("0020,0032"));
                    
                    // check along which axis the image coords change --- so far, only deal with orthogonal basis axis
                    // to figure out which axis the slice changes in, check the first slice and the second slice for a
                    // a difference along the basis.
                    if ((nextPositionCoords[0] != imagePositionCoords[0]) &&
                        (nextPositionCoords[1] == imagePositionCoords[1]) &&
                        (nextPositionCoords[2] == imagePositionCoords[2])) { // change ONLY in X-axis
                        //axisOfChange = 0;
                    } else if ((nextPositionCoords[0] == imagePositionCoords[0]) &&
                               (nextPositionCoords[1] != imagePositionCoords[1]) &&
                               (nextPositionCoords[2] == imagePositionCoords[2])) { // change ONLY in Y-axis
                        //axisOfChange = 1;
                    } else if ((nextPositionCoords[0] == imagePositionCoords[0]) &&
                               (nextPositionCoords[1] == imagePositionCoords[1]) &&
                               (nextPositionCoords[2] != imagePositionCoords[2])) { // change ONLY in Z-axis
                        //axisOfChange = 2;
                    } else { // change ONLY in ANY OTHER axis
                        MipavUtil.displayWarning("Remove Slices does not support changes in\n" +
                                                 "image position (DICOM tag 0020,0032)\n" +
                                                 "in more than one dimension.");
                        setCompleted(false);
                        
                        return;
                    }
                }
            }
        }

        /* axisFlip is always false: */
        boolean[] axisFlip = { false, false, false };

        /* set the axisOrder variable for remapping coordinate axes: */
        int[] axisOrder = { 0, 1, 2 }; /* no remapping is the default (REMOVE_T or REMOVE_Z) */
        if ( removeDim == REMOVE_Y )
        {
            /* get the ZY slice: */
            axisOrder[1] = 2;
            axisOrder[2] = 1;
        }
        else if (removeDim == REMOVE_X) {
            /* get the XZ slice: */
            axisOrder[0] = 2;
            axisOrder[2] = 0;
        }

        /* The third axis, is 2 for REMOVE_T condition, 3 otherwise: */
        int axis3 = ( removeDim == REMOVE_T ) ? 2 : 3;

        /* calculate slice size: */
        int slice = ( srcImage.getExtents()[ axisOrder[0] ] * 
                      srcImage.getExtents()[ axisOrder[1] ]   );
        if ( removeDim == REMOVE_T )
        {
            slice *= srcImage.getExtents()[ axis3 ];
        }

        destResolutions[0] = srcImage.getFileInfo(0).getResolutions()[ axisOrder[0] ];
        destResolutions[1] = srcImage.getFileInfo(0).getResolutions()[ axisOrder[1] ];
        destResolutions[2] = srcImage.getFileInfo(0).getResolutions()[ axis3 ];
        destUnitsOfMeasure[0] = srcImage.getFileInfo(0).getUnitsOfMeasure()[ axisOrder[0] ];
        destUnitsOfMeasure[1] = srcImage.getFileInfo(0).getUnitsOfMeasure()[ axisOrder[1] ];
        destUnitsOfMeasure[2] = srcImage.getFileInfo(0).getUnitsOfMeasure()[ axis3 ];

        /* If this is a color image: */
        int buffFactor = (srcImage.isColorImage()) ? 4 : 1;
        try {
            imageBuffer = new float[ buffFactor * slice ];
            fireProgressStateChanged(srcImage.getImageName(), "Creating 3D subset...");
        } catch (OutOfMemoryError e) {
            imageBuffer = null;
            System.gc();
            displayError("AlgorithmSubset reports: Out of memory");
            setCompleted(false);
            return;
        }

        /* If REMOVE_T export the desired volume: */
        if ( removeDim == REMOVE_T )
        {
            try {
                srcImage.exportData(sliceNum * buffFactor * slice, buffFactor * slice, imageBuffer);
                destImage.importData(0, imageBuffer, true);
            } catch (IOException error) {
                displayError("AlgorithmSubset reports: Destination image already locked.");
                setCompleted(false);
                return;
            }
        }
        /* Othewise export the remapped slices one at a time: */
        else
        {
            // make a location & view the progressbar; make length & increment of progressbar.
            int tDim = srcImage.getExtents()[3];
            for (int t = 0; (t < tDim) && !threadStopped; t++) {
                fireProgressStateChanged(Math.round((float) (t) / (tDim - 1) * 100));
                    
                try {
                    srcImage.export( axisOrder, axisFlip, t, sliceNum, imageBuffer);
                    destImage.importData(t * buffFactor * slice, imageBuffer, false);
                } catch (IOException error) {
                    displayError("AlgorithmSubset reports: Destination image already locked.");
                    setCompleted(false);
                    return;
                }
            } // for (t = 0; t < tDim; t++)
        }
        if (threadStopped) {
            imageBuffer = null;
            finalize();
            return;
        }
            
        /* copy srcImage FileInfoBase information into destImage : */
        FileInfoBase fileInfoBuffer; // buffer of any old type
        int dim3 = srcImage.getExtents()[axis3];
        for (int d = 0; (d < dim3) && !threadStopped; d++) {
            fileInfoBuffer = (FileInfoBase) srcImage.getFileInfo(0).clone();
            fileInfoBuffer.setExtents(destImage.getExtents());
            fileInfoBuffer.setResolutions(destResolutions);
            fileInfoBuffer.setUnitsOfMeasure(destUnitsOfMeasure);
            destImage.setFileInfo(fileInfoBuffer, d);
        }

        if (threadStopped) {
            imageBuffer = null;
            finalize();
            return;
        }
        destImage.calcMinMax();
        
        // Clean up and let the calling dialog know that algorithm did its job
        setCompleted(true);
    }

    /**
     * Constructs a string of the contruction parameters and outputs the
     * string to the messsage frame if the logging procedure is turned on.
     */
    private void constructLog() {
        String subsetStr = new String();

        if (removeDim == REMOVE_X) {
            subsetStr = "remove at x = ";
        } else if (removeDim == REMOVE_Y) {
            subsetStr = "remove at y = ";
        } else if (removeDim == REMOVE_Z) {
            subsetStr = "remove at z = ";
        } else {
            subsetStr = "remove at t = ";
        }

        subsetStr = subsetStr + sliceNum;
        historyString = new String("AlgorithmSubset(" + subsetStr + ")" + "\n");
    }
}
