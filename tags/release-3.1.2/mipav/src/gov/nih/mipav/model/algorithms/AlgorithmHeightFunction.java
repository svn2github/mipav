package gov.nih.mipav.model.algorithms;


import gov.nih.mipav.*;
import gov.nih.mipav.model.structures.*;
import gov.nih.mipav.view.ProgressChangeListener;

import java.io.*;

import javax.vecmath.*;
import gov.nih
.mipav.view.*;
/**
 * The class generates a triangle or quad mesh of a 2D dataset (image) to be displayed in the surface viewer. If the
 * image is 3D, takes the 2D current slice. The triangle mesh is like a relief map of the image; the higher intensities
 * are peaks and the lower intensities are valleys.
 *
 * @version  0.1 Aug 1, 2001
 * @author   Matthew J. McAuliffe, Ph.D.
 */
public class AlgorithmHeightFunction extends AlgorithmBase {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** DOCUMENT ME! */
    private static final int QUAD = 0;

    /** DOCUMENT ME! */
    private static final int TRIANGLE = 1;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** DOCUMENT ME! */
    private int mesh;

    /** DOCUMENT ME! */
    private ModelQuadMesh qMesh;

    /** DOCUMENT ME! */
    private int sampleSize = 1;

    /** DOCUMENT ME! */
    private int slice;

    /** DOCUMENT ME! */
    private String surfaceFileName;

    /** DOCUMENT ME! */
    private ModelTriangleMesh tMesh;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Sets up variables needed by algorithm to run.
     *
     * @param  srcImg      Source image model.
     * @param  sampleSize  Sample size to use when plotting surface.
     * @param  fileName    File name to save surface to.
     * @param  slice       Slice of 3D image to plot; 0 if 2D image.
     * @param  mesh        Type of mesh; 0 = Quad mesh, 1 = Triangle mesh
     */
    public AlgorithmHeightFunction(ModelImage srcImg, int sampleSize, String fileName, int slice, int mesh) {
        super(null, srcImg);
        this.sampleSize = sampleSize;
        surfaceFileName = fileName;
        this.slice = slice;
        this.mesh = mesh;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     */
    public void finalize() {

        tMesh = null;
        qMesh = null;
        super.finalize();
    }

    /**
     * Runs the algorithm; calls either calcQuadSurface or calcTriangleSurface, depending on the mesh parameter set in
     * the constructor.
     */
    public void runAlgorithm() {

        if (srcImage.isColorImage()) {
            displayError("Algorithm does not support RGB images.");

            return;
        }

        if (mesh == QUAD) {
            calcQuadSurface();
        } else if (mesh == TRIANGLE) {
            calcTriSurface();
        } else {
            displayError("AlgorithmHeightFunction: Invalid mesh type - " + mesh);
        }
    }

    /**
     * Calculates quad surface.
     */
    private void calcQuadSurface() {

        int i, x, y;
        int offset;
        int length;
        int xDim, yDim;
        float[] buffer;
        Point3f[] cVertex = null;
        int[] cConnect = null;

        try {
            xDim = srcImage.getExtents()[0];
            yDim = srcImage.getExtents()[1];
            length = xDim * yDim;
            cVertex = new Point3f[length / sampleSize];
            cConnect = new int[length * 4 / sampleSize];
            buffer = new float[length];
            fireProgressStateChanged(srcImage.getImageName(), "Generating surface ...");
        } catch (OutOfMemoryError e) {
            buffer = null;
            System.gc();
            displayError("AlgorithmHeightFunction: Out of memory");
            setCompleted(false);


            return;
        }



        try {
            srcImage.exportSliceXY(slice, buffer);
        } catch (IOException error) {
            displayError("AlgorithmHeightFunction: Out of memory");
            setCompleted(false);


            return;
        }

        fireProgressStateChanged("Building vertex connectivity.");
        fireProgressStateChanged(10);

        // build connectivity
        i = 0;

        for (y = 0; y < (yDim - sampleSize); y = y + sampleSize) {
            offset = y / sampleSize * (int) Math.ceil((float) xDim / sampleSize);

            for (x = 0; x < (xDim - sampleSize); x = x + sampleSize) {
                cConnect[i++] = offset + (x / sampleSize); // 0   1
                cConnect[i++] = offset + (x / sampleSize) + 1; // 3   2
                cConnect[i++] = offset + (x / sampleSize) + (int) Math.ceil((float) xDim / sampleSize) + 1;
                cConnect[i++] = offset + (x / sampleSize) + (int) Math.ceil((float) xDim / sampleSize);
            }
        }

        int length1 = i;

        fireProgressStateChanged("Building vertex data.");
        fireProgressStateChanged(60);

        float min = (float) srcImage.getMin();
        float max = (float) srcImage.getMax();
        float range = max - min;
        float height;

        i = 0;

        float maxBox, xBox, yBox;

        xBox = srcImage.getFileInfo(0).getResolutions()[0] * srcImage.getExtents()[0];
        yBox = srcImage.getFileInfo(0).getResolutions()[1] * srcImage.getExtents()[1];
        maxBox = Math.max(xBox, yBox);

        for (y = 0; (y < yDim) && !threadStopped; y = y + sampleSize) {

            for (x = 0; x < xDim; x = x + sampleSize) {

                height = (((buffer[(y * xDim) + x] - min) / range) * maxBox) - (maxBox / 2.0f); // Set function height
                                                                                                // relative image size
                cVertex[i++] = new Point3f(x - (xDim / 2), -(y - (yDim / 2)), height);
            }
        }

        int length2 = i;

        if (threadStopped) {
            finalize();

            return;
        }

        fireProgressStateChanged("Saving surface.");
        fireProgressStateChanged(90);

        try {
            qMesh = new ModelQuadMesh(cVertex, cConnect, length2, length1, null, maxBox);

            if (surfaceFileName.endsWith(".sur") == false) {
                surfaceFileName = srcImage.getUserInterface().getDefaultDirectory() + surfaceFileName + ".sur";
            } else {
                surfaceFileName = srcImage.getUserInterface().getDefaultDirectory() + surfaceFileName;
            }


            qMesh.save(surfaceFileName, getProgressChangeListener(), 90, 10);
        } catch (IOException e) {
            System.gc();
            displayError("AlgorithmHeightFunction: " + e);
            setCompleted(false);


            return;
        } catch (OutOfMemoryError e) {
            System.gc();
            displayError("AlgorithmHeightFunction: Out of memory");
            setCompleted(false);


            return;
        }


        setCompleted(true);
    }

    /**
     * Calculates triangulated surface.
     */
    private void calcTriSurface() {

        int i, x, y;
        int offset;
        int length;
        int xDim, yDim;
        int vCnt; // vertex count;
        float[] buffer;
        Point3f[] cVertex = null;
        Point3f[] vertex = null;
        int[] cConnect = null;
        int[] connect = null;

        try {
            xDim = srcImage.getExtents()[0];
            yDim = srcImage.getExtents()[1];
            length = xDim * yDim;
            vCnt = xDim * yDim;
            cVertex = new Point3f[vCnt / sampleSize];
            cConnect = new int[length * 6 / sampleSize];
            buffer = new float[length];
            fireProgressStateChanged(srcImage.getImageName(), "Generating surface ...");
        } catch (OutOfMemoryError e) {
            buffer = null;
            cConnect = null;
            cVertex = null;
            System.gc();
            displayError("AlgorithmHeightFunction: Out of memory");
            setCompleted(false);


            return;
        }



        try {
            srcImage.exportData(slice, length, buffer); // locks and releases lock
        } catch (IOException error) {
            displayError("AlgorithmHeightFunction: Out of memory");
            setCompleted(false);


            return;
        }

        fireProgressStateChanged("Building vertex connectivity.");
        fireProgressStateChanged(10);

        // build connectivity
        i = 0;

        for (y = 0; y < (yDim - sampleSize); y = y + sampleSize) {
            offset = y / sampleSize * (int) Math.ceil((float) xDim / sampleSize);

            for (x = 0; x < (xDim - sampleSize); x = x + sampleSize) {
                cConnect[i++] = offset + (x / sampleSize);
                cConnect[i++] = offset + (x / sampleSize) + 1;
                cConnect[i++] = offset + (x / sampleSize) + (int) Math.ceil((float) xDim / sampleSize) + 1;

                cConnect[i++] = offset + (x / sampleSize);
                cConnect[i++] = offset + (x / sampleSize) + (int) Math.ceil((float) xDim / sampleSize) + 1;
                cConnect[i++] = offset + (x / sampleSize) + (int) Math.ceil((float) xDim / sampleSize);
            }
        }

        try {
            connect = new int[i];

            for (int n = 0; n < i; n++) {
                connect[n] = cConnect[n];
            }
        } catch (OutOfMemoryError e) {
            System.gc();
            displayError("AlgorithmHeightFunction: Out of memory");
            setCompleted(false);


            return;
        }

        fireProgressStateChanged("Building vertex data.");
        fireProgressStateChanged(60);

        float min = (float) srcImage.getMin();
        float max = (float) srcImage.getMax();
        float range = max - min;
        float height;

        i = 0;

        float maxBox, xBox, yBox, zBox;
        float[] box = new float[3];

        xBox = srcImage.getFileInfo(0).getResolutions()[0] * srcImage.getExtents()[0];
        yBox = srcImage.getFileInfo(0).getResolutions()[1] * srcImage.getExtents()[1];
        zBox = srcImage.getFileInfo(0).getResolutions()[2] * srcImage.getExtents()[2];
        maxBox = Math.max(xBox, yBox);
        box[0] = xBox;
        box[1] = yBox;
        box[2] = zBox;

        /* Read the direction vector from the MipavCoordinateSystems class: */
        int[] direction = MipavCoordinateSystems.getModelDirections( srcImage );
        float[] startLocation = srcImage.getFileInfo(0).getOrigin();

        for (y = 0; (y < yDim) && !threadStopped; y = y + sampleSize) {

            for (x = 0; x < xDim; x = x + sampleSize) {

                height = (((buffer[(y * xDim) + x] - min) / range) * maxBox) - (maxBox / 2.0f); // Set function height
                                                                                                // relative image size
                cVertex[i++] = new Point3f(x - (xDim / 2), -(y - (yDim / 2)), height);
            }
        }

        if (threadStopped) {
            finalize();

            return;
        }

        fireProgressStateChanged("Saving surface.");
        fireProgressStateChanged(90);

        try {
            vertex = new Point3f[i];

            for (int n = 0; n < i; n++) {
                vertex[n] = new Point3f(cVertex[n]);
            }

            tMesh = new ModelTriangleMesh(vertex, connect);

            if (surfaceFileName.endsWith(".sur") == false) {
                surfaceFileName = srcImage.getUserInterface().getDefaultDirectory() + surfaceFileName + ".sur";
            } else {
                surfaceFileName = srcImage.getUserInterface().getDefaultDirectory() + surfaceFileName;
            }

            tMesh.save(surfaceFileName, true, direction, startLocation, box, null);
        } catch (IOException e) {
            System.gc();
            displayError("AlgorithmHeightFunction: " + e);
            setCompleted(false);


            return;
        } catch (OutOfMemoryError e) {
            System.gc();
            displayError("AlgorithmHeightFunction: Out of memory");
            setCompleted(false);


            return;
        }


        setCompleted(true);
    }

}
