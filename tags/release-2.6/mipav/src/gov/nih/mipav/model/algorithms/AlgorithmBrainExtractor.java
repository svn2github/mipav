package gov.nih.mipav.model.algorithms;


import gov.nih.mipav.*;

import gov.nih.mipav.model.file.*;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.io.*;

import java.util.*;

import javax.vecmath.*;


/**
 * A class for segmenting the brain from a 3D MRI. The algorithm is partially based on the paper:
 *
 * <pre>
     BET: Brain Extraction Tool<br>
     Stephen M. Smith<br>
     FMRIB Technical Report TR00SMS2<br>
     Oxford Centre for Functional Magnetic Resonance Imaging of the Brain<br>
 * </pre>
 *
 * See the document BrainExtraction.pdf for a detailed description of the algorithm as implemented in this class. A few
 * modifications to the original algorithm were made.
 *
 * @author  David Henry Eberly, Magic Software. Under contract from NIH. with modifications by Matthew McAuliffe
 */
public class AlgorithmBrainExtractor extends AlgorithmBase {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Axis orientation unknown. */
    public static final int ORI_UNKNOWN_TYPE = 0;

    /** Axis orientation Right to Left. */
    public static final int ORI_R2L_TYPE = 1;

    /** Axis orientation Left to Right. */
    public static final int ORI_L2R_TYPE = 2;

    /** Axis orientation Posterior to Anterior. */
    public static final int ORI_P2A_TYPE = 3;

    /** Axis orientation Anterior to Posterior. */
    public static final int ORI_A2P_TYPE = 4;

    /** Axis orientation Inferior to Superior. */
    public static final int ORI_I2S_TYPE = 5;

    /** Axis orientation Superior to Inferior. */
    public static final int ORI_S2I_TYPE = 6;

    /** DOCUMENT ME! */
    public static final int SAT_COR = 0;

    /** DOCUMENT ME! */
    public static final int AXIAL = 1;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** DOCUMENT ME! */
    protected float f3Factor = 0.08f;

    /** DOCUMENT ME! */
    protected float[] m_afCurvature;

    /** DOCUMENT ME! */
    protected float[] m_afLength;

    /** DOCUMENT ME! */
    protected int[] m_aiConnect;

    /** The 3D MRI image stored as a 1D array. The mapping from (x,y,z) to 1D is: index = x + xbound*(y + ybound*z). */
    protected int[] m_aiImage;

    /** brain mask creation. */
    protected byte[] m_aiMask;

    /** DOCUMENT ME! */
    protected UnorderedSetInt[] m_akAdjacent;

    /** DOCUMENT ME! */
    protected Vector3f[] m_akSNormal;

    /** DOCUMENT ME! */
    protected Vector3f[] m_akSTangent;

    /** DOCUMENT ME! */
    protected Point3f[] m_akVertex;

    /** DOCUMENT ME! */
    protected Point3f[] m_akVMean;

    /** DOCUMENT ME! */
    protected Vector3f[] m_akVNormal;

    /** DOCUMENT ME! */
    protected float m_fBrainSelection;

    /** DOCUMENT ME! */
    protected float m_fEParam, m_fFParam;

    /** update parameters. */
    protected float m_fMeanEdgeLength;

    /** DOCUMENT ME! */
    protected float m_fRayDelta = 1;

    /** DOCUMENT ME! */
    protected float m_fReductionX = 0.6f;

    /** DOCUMENT ME! */
    protected float m_fReductionY = 0.4f;

    /** DOCUMENT ME! */
    protected float m_fReductionZ = 0.6f;

    /** DOCUMENT ME! */
    protected float m_fStiffness = 0.15f;

    /** The size of a voxel, in voxel units. */
    protected float m_fXDelta, m_fYDelta, m_fZDelta;

    /** DOCUMENT ME! */
    protected int m_iBackThreshold;

    /** DOCUMENT ME! */
    protected int m_iBrightThreshold;

    /** DOCUMENT ME! */
    protected int m_iDMax; // dilation size

    /** DOCUMENT ME! */
    protected int m_iEQuantity;

    /** DOCUMENT ME! */
    protected int m_iHalfMaxDepth = 3;

    /** DOCUMENT ME! */
    protected int m_iMaxDepth = 7;

    /** DOCUMENT ME! */
    protected int m_iMaxThreshold;

    /** DOCUMENT ME! */
    protected int m_iMedianIntensity;

    /** histogram parameters. */
    protected int m_iMinThreshold;

    /** DOCUMENT ME! */
    protected int m_iTQuantity;

    /** mesh data. */
    protected int m_iVQuantity;

    /** The MRI image bounds and quantity of voxels. */
    protected int m_iXBound, m_iYBound, m_iZBound, m_iQuantity;

    /** initial ellipsoid parameters. */
    protected Point3f m_kCenter;

    /** DOCUMENT ME! */
    protected HashMap m_kEMap; // map<Edge,int>

    /** DOCUMENT ME! */
    protected Matrix3f m_kRotate;

    /** DOCUMENT ME! */
    protected int nIterations = 1500;

    /** DOCUMENT ME! */
    protected int orientationFlag = SAT_COR;

    /** DOCUMENT ME! */
    private boolean abort = false;

    /** factor above median at which edge values are taken to zero. */
    private float aboveMedian = 1.5f;

    /** DOCUMENT ME! */
    private int[] axisOrientation;

    /** DOCUMENT ME! */
    private float[] box;

    /** DOCUMENT ME! */
    private int[] direction;

    /** DOCUMENT ME! */
    private boolean extractPaint;

    /** DOCUMENT ME! */
    private float fUpdate1 = 0.5f;

    /** DOCUMENT ME! */
    private ModelImage image;

    /** DOCUMENT ME! */
    private int nBrainVoxels = 0;

    /** DOCUMENT ME! */
    private boolean onlyEllipse = false;

    /** DOCUMENT ME! */
    private boolean saveBrainMesh = true;

    /** Tells whether second stage which erodes edge values exceeding median by a factor >= aboveMedian occurs. */
    private boolean secondStageErosion = false;

    /** DOCUMENT ME! */
    private float[] startLocation;

    /** DOCUMENT ME! */
    private boolean useSphere = false;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Create an extractor for segmenting the brain from a 3D magnetic resonance image.
     *
     * @param  srcImg          the source image. Should be MR image of the brain
     * @param  orientation     image orienation
     * @param  justEllipse     DOCUMENT ME!
     * @param  estimateSphere  DOCUMENT ME!
     * @param  centerPoint     DOCUMENT ME!
     */
    public AlgorithmBrainExtractor(ModelImage srcImg, int orientation, boolean justEllipse, boolean estimateSphere,
                                   Point3f centerPoint) {
        int i;
        float fMax, fMin;
        orientationFlag = orientation;
        onlyEllipse = justEllipse;
        useSphere = estimateSphere;
        m_kCenter = centerPoint;

        /* The number of levels to subdivide the initial
         *   ellipsoid into a mesh that approximates the brain surface.  The  number of triangles in the mesh is
         * 8*pow(4,S) where S is the  subdivision parameter.  A reasonable choice is 5, leading to a mesh
         *   with 8192 triangle. */
        int iSubdivisions = 5; // 6 =  32K triangles

        // iSubdivisions = 6;         // 6 =  32K triangles
        // image bounds and voxel sizes
        m_iXBound = srcImg.getExtents()[0];
        m_iYBound = srcImg.getExtents()[1];
        m_iZBound = srcImg.getExtents()[2];
        m_iQuantity = m_iXBound * m_iYBound * m_iZBound;
        m_fXDelta = srcImg.getFileInfo()[0].getResolutions()[0];
        m_fYDelta = srcImg.getFileInfo()[0].getResolutions()[1];
        m_fZDelta = srcImg.getFileInfo()[0].getResolutions()[2];

        box = new float[3];
        box[0] = (m_iXBound - 1) * m_fXDelta;
        box[1] = (m_iYBound - 1) * m_fYDelta;
        box[2] = (m_iZBound - 1) * m_fZDelta;
        axisOrientation = srcImg.getFileInfo()[0].getAxisOrientation();
        direction = new int[] { 1, 1, 1 };

        for (i = 0; i <= 2; i++) {

            if ((axisOrientation[i] == ORI_L2R_TYPE) || (axisOrientation[i] == ORI_P2A_TYPE) ||
                    (axisOrientation[i] == ORI_S2I_TYPE)) {
                direction[i] = -1;
            }
        }

        startLocation = srcImg.getFileInfo()[0].getOrigin();
        image = srcImg;

        // The histogram analysis is best performed by binning into 8-bit
        // data.  The image term in the evolution scheme depends only on a
        // few intensity threshold values, so the method appears not to be
        // sensitive to number of bits in the image data.
        m_aiImage = new int[m_iQuantity];

        fMin = (float) srcImg.getMin();
        fMax = (float) srcImg.getMax();

        // Remap image data to 0 - 1023
        float fMult = 1023.0f / (fMax - fMin);

        for (i = 0; i < m_iQuantity; i++) {
            m_aiImage[i] = (int) (fMult * (srcImg.getFloat(i) - fMin));
        }

        // Based on empirical studies, these parameters seem to work well.
        m_fBrainSelection = 0.5f;
        m_iMaxDepth = 7;
        m_iHalfMaxDepth = m_iMaxDepth / 2;
        m_fRayDelta = 1.0f;
        m_fStiffness = 0.1f;
        m_iDMax = 0;

        // Compute various intensity values needed for the image term in the
        // evolution method.
        histogramAnalysis();

        // Construct the initial ellipsoid and corresponding mesh that
        // approximate the brain surface.

        if (useSphere == false) {

            // if estimate ellipsoid is false set use estimate sphere!
            useSphere = !estimateEllipsoid();
        }

        if (useSphere == true) {
            estimateSphere();
        }

        generateEllipsoidMesh(iSubdivisions);

        // Compute the median intensity for voxels inside the initial ellipsoid.
        computeMedianIntensity();

        // Supporting quantities for update of mesh.  VMean[i] stores the
        // average of the immediate vertex neighbors of vertex V[i].
        // VNormal[i] is the vertex normal at V[i] computed as the average
        // of the non-unit normals for all triangles sharing V[i].  Define
        // S = VMean[i] - V[i].  SNormal[i] is the component of S in the
        // VNormal[i] direction and STangent[i] = S - SNormal[i].  The value
        // Curvature[i] is an estimate of the surface curvature at V[i].
        m_akVMean = new Point3f[m_iVQuantity];
        m_akVNormal = new Vector3f[m_iVQuantity];
        m_akSNormal = new Vector3f[m_iVQuantity];
        m_akSTangent = new Vector3f[m_iVQuantity];
        m_afCurvature = new float[m_iVQuantity];

        for (i = 0; i < m_iVQuantity; i++) {
            m_akVMean[i] = new Point3f();
            m_akVNormal[i] = new Vector3f();
            m_akSNormal[i] = new Vector3f();
            m_akSTangent[i] = new Vector3f();
        }

        // the binary mask for voxels inside the brain surface
        m_aiMask = new byte[m_iQuantity];
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * The segmentation function. Ideally, the only work an application needs to do is create an MjBrainExtractor object
     * and call the method extractBrain(). Various parameters managed by MjBrainExtractor may be modified, if necessary,
     * before the call.
     */
    public void extractBrain() {
        // The parameters were selected basic on empirical studies. First stage
        // the mesh is kept fairly stiff to reduce likelyhood of surface intesections.
        // The datasets I have tested I not seen (visual inspection) any intersections.
        // The second stage the message is a little less stiff and mesh is allowed to
        // conform more to the surface of the brain.


        // First phase is mandatory with set values
        int i;
        int iMaxStep = 8;
        int iMaxUpdate;

        int tmpMD = m_iMaxDepth;
        int tmHMD = m_iHalfMaxDepth;
        float tmpF3F = f3Factor;
        float tmpStiff = m_fStiffness;

        m_iMaxDepth = 7;
        m_iHalfMaxDepth = 3;
        f3Factor = 0.1f;
        m_fStiffness = 0.2f;

        buildProgressBar(image.getImageName(), "Extracting brain ...", 0, 100);
        initProgressBar();

        if (onlyEllipse == true) {
            iMaxUpdate = 0;
        } else {
            iMaxUpdate = 100;
        }

        for (int iStep = 1; (iStep <= iMaxStep) && !threadStopped; iStep++) {

            if (isProgressBarVisible()) {

                if (secondStageErosion) {
                    progressBar.updateValue(Math.round((iStep * 100.0f) / 800 * 25), activeImage);
                } else {
                    progressBar.updateValue(Math.round((iStep * 100.0f) / 800 * 50), activeImage);
                }
            }

            for (i = 1; i <= iMaxUpdate; i++) {
                updateMesh();
            }
        }

        m_iMaxDepth = tmpMD;
        m_iHalfMaxDepth = tmHMD;
        f3Factor = tmpF3F;
        m_fStiffness = tmpStiff;

        //
        // m_iMaxDepth         = 4;
        // m_iHalfMaxDepth     = 3;
        // f3Factor            = 0.08f;        // --> 0  more stiff
        // m_fStiffness        = 0.15f;        // --> 0  less stiff
        if (onlyEllipse == true) {
            iMaxUpdate = 0;
        } else {
            iMaxUpdate = nIterations;
        }

        for (i = 1; (i <= iMaxUpdate) && !threadStopped; i++) {

            if (((i % 100) == 0) && isProgressBarVisible()) {

                if (secondStageErosion) {
                    progressBar.updateValue(Math.round((25 + (((float) (i)) / iMaxUpdate * 25))), activeImage);
                } else {
                    progressBar.updateValue(Math.round((50 + (((float) (i)) / iMaxUpdate * 50))), activeImage);
                }
            }

            updateMesh();
        }

        if (threadStopped) {
            finalize();

            return;
        }

        // identify those voxels inside the mesh
        getInsideVoxels(secondStageErosion);

        if (secondStageErosion) {

            // After the image edge erosion in getInsideVoxels(), the mesh no longer corresponds
            // to the image.  Therefore, use the eroded image to recalculate the mesh.
            progressBar.setMessage("Recalculating mesh for eroded image");
            m_iMaxDepth = 7;
            m_iHalfMaxDepth = 3;
            f3Factor = 0.1f;
            m_fStiffness = 0.2f;

            if (onlyEllipse == true) {
                iMaxUpdate = 0;
            } else {
                iMaxUpdate = 100;
            }

            for (int iStep = 1; (iStep <= iMaxStep) && !threadStopped; iStep++) {

                if (isProgressBarVisible()) {
                    progressBar.updateValue(Math.round(50 + ((iStep * 100.0f) / 800 * 25)), activeImage);
                }

                for (i = 1; i <= iMaxUpdate; i++) {
                    updateMesh();
                }
            }

            m_iMaxDepth = tmpMD;
            m_iHalfMaxDepth = tmHMD;
            f3Factor = tmpF3F;
            m_fStiffness = tmpStiff;

            //
            // m_iMaxDepth         = 4;
            // m_iHalfMaxDepth     = 3;
            // f3Factor            = 0.08f;        // --> 0  more stiff
            // m_fStiffness        = 0.15f;        // --> 0  less stiff
            if (onlyEllipse == true) {
                iMaxUpdate = 0;
            } else {
                iMaxUpdate = nIterations;
            }

            for (i = 1; (i <= iMaxUpdate) && !threadStopped; i++) {

                if (((i % 100) == 0) && isProgressBarVisible()) {
                    progressBar.updateValue(Math.round((75 + (((float) (i)) / iMaxUpdate * 25))), activeImage);
                }

                updateMesh();
            }

            if (threadStopped) {
                finalize();

                return;
            }

            getInsideVoxels(false);
        } // if (secondStageErosion)

        try {

            if (saveBrainMesh) {
                saveMesh(true);
            }
        } catch (IOException e) {
            System.out.println(" Problem saving mesh.");
        }

        disposeProgressBar();
        setCompleted(true);
    }

    /**
     * Prepares this class for destruction.
     */
    public void finalize() {

        m_afLength = null;
        m_akVertex = null;
        m_aiConnect = null;
        m_akAdjacent = null;
        m_kEMap = null;

        m_akVMean = null;
        m_akVNormal = null;
        m_akSNormal = null;
        m_akSTangent = null;
        m_afCurvature = null;

        axisOrientation = null;
        direction = null;
        startLocation = null;
        box = null;
        image = null;
        m_aiImage = null;
        m_aiMask = null;

        super.finalize();
    }

    /**
     * Get the 3D image that represents the extracted brain. The image is ternary and has the same dimensions as the
     * input MRI. A voxel value of 0 indicates background. A voxel value of 1 indicates brain surface. A voxel value of
     * 2 indicates a voxel inside the brain surface.
     *
     * @return  the image that represents the extracted brain
     */
    public final byte[] getBrainMask() {
        return m_aiMask;
    }

    /**
     * Get the current brain selection term, as described in BrainExtraction.pdf, that is part of the image term in the
     * surface evolution. The default value is 0.5.
     *
     * @return  the current brain selection term
     */
    public final float getBrainSelection() {
        return m_fBrainSelection;
    }

    /**
     * Get the 3D image that represents the extracted brain. The image is ternary and has the same dimensions as the
     * input MRI. A voxel value of 0 indicates background. A voxel value of 1 indicates brain surface. A voxel value of
     * 2 indicates a voxel inside the brain surface.
     *
     * @return  the image that represents the extracted brain
     */
    public final float getBrainVolume() {
        return nBrainVoxels * m_fXDelta * m_fYDelta * m_fZDelta;
    }

    /**
     * Get the dilation size for dilating the voxelized brain surface obtained by rasterizing the triangle mesh. The
     * rasterization is designed so that the voxel surface has no holes, thereby allowing it to be flood-filled. But
     * just in case numerical round-off errors cause a few holes, this parameter is exposed for public use. The default
     * value is 0 (no dilation). If the value is D > 0, the dilation mask is a cube of size (2*D+1)x(2*D+1)x(2*D+1).
     *
     * @return  the current dilation size
     */
    public final int getDilationSize() {
        return m_iDMax;
    }

    /**
     * Get the maximum depth, as described in BrainExtraction.pdf, that is part of the image term in the surface
     * evolution. The default value is 7.
     *
     * @return  the current maximum depth
     */
    public final int getMaxDepth() {
        return m_iMaxDepth;
    }

    /**
     * Get the spacing along the vertex normal rays, as described in BrainExtraction.pdf, that is part of the image term
     * in the surface evolution. The default value is 1.0.
     *
     * @return  the current spacing
     */
    public final float getRayDelta() {
        return m_fRayDelta;
    }

    /**
     * Set the reduction factor for estimating the initial ellipsoid, as described in BrainExtraction.pdf. The default
     * value is 0.75.
     *
     * @return  the current reduction factor
     */
    public final float getReductionX() {
        return m_fReductionX;
    }

    /**
     * Set the reduction factor for estimating the initial ellipsoid, as described in BrainExtraction.pdf. The default
     * value is 0.75.
     *
     * @return  the current reduction factor
     */
    public final float getReductionY() {
        return m_fReductionY;
    }

    /**
     * Set the reduction factor for estimating the initial ellipsoid, as described in BrainExtraction.pdf. The default
     * value is 0.75.
     *
     * @return  the current reduction factor
     */
    public final float getReductionZ() {
        return m_fReductionZ;
    }

    /**
     * Set the stiffness of the mesh, as described in BrainExtraction.pdf, that is part of the surface normal term in
     * the surface evolution. The default value is 0.1.
     *
     * @return  the current stiffness
     */
    public final float getStiffness() {
        return m_fStiffness;
    }

    /**
     * Analyze the histogram of the 10-bit binned 3D MRI. The function computes a minimum threshold, a maximum
     * threshold, and a background threshold that are used in the image term of the surface evolution. A brightness
     * threshold is also computed that is used for determining the initial ellipsoid that approximates the brain
     * surface.
     */
    public void histogramAnalysis() {

        // compute histogram
        int[] aiHistogram = new int[1024];
        Arrays.fill(aiHistogram, 0);

        int i;
        int j;

        for (i = 0; i < m_iQuantity; i++) {
            aiHistogram[m_aiImage[i]]++;
        }

        // Eliminate a large chunk of background.  The four parameters below
        // were selected based on empirical studies.
        double dMinFactor = 0.03;
        double dMaxFactor = 0.98;
        double dBrightFactor = 0.95;

        int iMax = 64;
        int iMinCutoff = (int) (dMinFactor * m_iQuantity);
        int iMaxCutoff = (int) (dMaxFactor * m_iQuantity);

        // Find background - i.e. the value with the most counts
        float maxCount = -1;
        int maxCountIndex = 0;

        for (j = 0; j < iMax; j++) {

            if (aiHistogram[j] > maxCount) {
                maxCount = aiHistogram[j];
                maxCountIndex = j;
            }
        }

        // maxCountIndex = iMax;
        int iAccum = 0;

        if (maxCountIndex == iMax) {

            // unable to find background from above - use cummalitive histogram method
            for (i = 0; i < 64; i++) {
                iAccum += aiHistogram[i];

                if (iAccum <= iMaxCutoff) {
                    m_iMaxThreshold = i;
                }

                if (iAccum <= iMinCutoff) {
                    m_iMinThreshold = i;
                }
            }

            m_iBackThreshold = MipavMath.round((0.9f * m_iMinThreshold) + (0.1f * m_iMaxThreshold));
        } else {
            m_iBackThreshold = maxCountIndex + 1;
        }

        int iReducedQuantity = m_iQuantity;

        for (j = 0; j <= maxCountIndex; j++) {
            iReducedQuantity -= aiHistogram[j];
        }

        Preferences.debug("Brain extractor: histogramAnalysis: m_iQuantity = " + m_iQuantity + "\n");
        Preferences.debug("Brain extractor: histogramAnalysis: iReducedQuantity = " + iReducedQuantity + "\n");

        // compute brightness thresholds
        iAccum = 0;

        int iBrightCutoff = (int) (dBrightFactor * iReducedQuantity);

        // m_iMaxThreshold     = m_iBackThreshold;
        for (i = m_iBackThreshold; i < 1024; i++) {
            iAccum += aiHistogram[i];

            if (iBrightCutoff >= iAccum) {
                m_iBrightThreshold = i;
            } // Used to estimate ellipsoid !
        }

        if (m_iBackThreshold == 0) {
            m_iBackThreshold = 1;
            m_iMinThreshold = 0;
        } else if (m_iBackThreshold == 1) {
            m_iMinThreshold = 0;
        } else {
            m_iMinThreshold = (int) Math.floor(0.5f * m_iBackThreshold);
        }

        if (m_iMinThreshold == m_iBackThreshold) {
            m_iBackThreshold++;
        }

        // m_iBrightThreshold *= 1.25f;
        Preferences.debug("Brain extractor: histogramAnalysis: MinThreshold = " + m_iMinThreshold + "\n");
        Preferences.debug("Brain extractor: histogramAnalysis: BackThreshold = " + m_iBackThreshold + "\n");
        Preferences.debug("Brain extractor: histogramAnalysis: Brightness Threshold = " + m_iBrightThreshold + "\n");
    }

    /**
     * Reinitialize when secondStageErosion has been performed so that the mask will match the eroded image.
     */
    public void reIntialize() {
        int i;
        float fMax, fMin;

        /* The number of levels to subdivide the initial
         *   ellipsoid into a mesh that approximates the brain surface.  The  number of triangles in the mesh is
         * 8*pow(4,S) where S is the  subdivision parameter.  A reasonable choice is 5, leading to a mesh
         *   with 8192 triangle. */
        int iSubdivisions = 5; // 6 =  32K triangles

        // iSubdivisions = 6;
        fMin = (float) image.getMin();
        fMax = (float) image.getMax();

        // Remap image data to 0 - 1023
        float fMult = 1023.0f / (fMax - fMin);

        for (i = 0; i < m_iQuantity; i++) {
            m_aiImage[i] = (int) (fMult * (image.getFloat(i) - fMin));
        }

        // Based on empirical studies, these parameters seem to work well.
        m_fBrainSelection = 0.5f;
        m_iMaxDepth = 7;
        m_iHalfMaxDepth = m_iMaxDepth / 2;
        m_fRayDelta = 1.0f;
        m_fStiffness = 0.1f;
        m_iDMax = 0;

        // Compute various intensity values needed for the image term in the
        // evolution method.
        histogramAnalysis();

        // Construct the initial ellipsoid and corresponding mesh that
        // approximate the brain surface.

        if (useSphere == false) {

            // if estimate ellipsoid is false set use estimate sphere!
            useSphere = !estimateEllipsoid();
        }

        if (useSphere == true) {
            estimateSphere();
        }

        generateEllipsoidMesh(iSubdivisions);

        // Compute the median intensity for voxels inside the initial ellipsoid.
        computeMedianIntensity();

        // Supporting quantities for update of mesh.  VMean[i] stores the
        // average of the immediate vertex neighbors of vertex V[i].
        // VNormal[i] is the vertex normal at V[i] computed as the average
        // of the non-unit normals for all triangles sharing V[i].  Define
        // S = VMean[i] - V[i].  SNormal[i] is the component of S in the
        // VNormal[i] direction and STangent[i] = S - SNormal[i].  The value
        // Curvature[i] is an estimate of the surface curvature at V[i].
        m_akVMean = new Point3f[m_iVQuantity];
        m_akVNormal = new Vector3f[m_iVQuantity];
        m_akSNormal = new Vector3f[m_iVQuantity];
        m_akSTangent = new Vector3f[m_iVQuantity];
        m_afCurvature = new float[m_iVQuantity];

        for (i = 0; i < m_iVQuantity; i++) {
            m_akVMean[i] = new Point3f();
            m_akVNormal[i] = new Vector3f();
            m_akSNormal[i] = new Vector3f();
            m_akSTangent[i] = new Vector3f();
        }
    }

    /**
     * Starts the program.
     */
    public void runAlgorithm() {

        if (image == null) {
            displayError("Source Image is null");
            finalize();

            return;
        }

        if (image.getNDims() != 3) {
            displayError("Source Image must be 3D");
            finalize();

            return;
        }

        constructLog();

        if (abort != true) {
            extractBrain();
        }
    }

    /**
     * sets the aboveMedian ratio for second stage erosion Edge values >= median * aboveMedian are taken to zero.
     *
     * @param  aboveMedian  DOCUMENT ME!
     */
    public final void setAboveMedian(float aboveMedian) {
        this.aboveMedian = aboveMedian;
    }

    /**
     * Set the brain selection term, as described in BrainExtraction.pdf, that is part of the image term in the surface
     * evolution. The default value is 0.5.
     *
     * @param  fBrainSelection  the new brain selection term
     */
    public final void setBrainSelection(float fBrainSelection) {
        m_fBrainSelection = fBrainSelection;
    }

    /**
     * Set the dilation size for dilating the voxelized brain surface obtained by rasterizing the triangle mesh. The
     * rasterization is designed so that the voxel surface has no holes, thereby allowing it to be flood-filled. But
     * just in case numerical round-off errors cause a few holes, this parameter is exposed for public use. The default
     * value is 0 (no dilation). If the value is D > 0, the dilation mask is a cube of size (2*D+1)x(2*D+1)x(2*D+1).
     *
     * @param  iDMax  the new dilation size
     */
    public final void setDilationSize(int iDMax) {
        m_iDMax = iDMax;
    }

    /**
     * Sets whether to extract the brain to a paint mask instead of removing non-brain image data.
     *
     * @param  extractPaint  whether to extract the brain to a paint mask
     */
    public void setExtractPaint(boolean extractPaint) {
        this.extractPaint = extractPaint;
    }

    /**
     * Sets the number of iterations.
     *
     * @param  ratio  DOCUMENT ME!
     */
    public final void setImageRatio(float ratio) {
        f3Factor = ratio;
    }

    /**
     * Sets the number of iterations.
     *
     * @param  nIter  DOCUMENT ME!
     */
    public final void setIterations(int nIter) {
        this.nIterations = nIter;
    }

    /**
     * Indicates if surface evolution should be skipped. This is helpful when determining if the initial ellipsoid is a
     * good estimate.
     *
     * @param  flag  if true skip evolution of surface
     */
    public final void setJustIntialEllipsoid(boolean flag) {
        onlyEllipse = flag;
    }

    /**
     * Set the maximum depth, as described in BrainExtraction.pdf, that is part of the image term in the surface
     * evolution. The default value is 7.
     *
     * @param  iMaxDepth  the new maximum depth
     */
    public final void setMaxDepth(int iMaxDepth) {
        m_iMaxDepth = iMaxDepth;
        m_iHalfMaxDepth = iMaxDepth / 2;
    }

    /**
     * Indicates the orientation of the image for use in the ellipsoid estimation.
     *
     * @param  orientation  the new image orientation
     */
    public final void setOrientation(int orientation) {
        orientationFlag = orientation;
    }

    /**
     * Set the spacing along the vertex normal rays, as described in BrainExtraction.pdf, that is part of the image term
     * in the surface evolution. The default value is 1.0.
     *
     * @param  fRayDelta  the new normal ray spacing
     */
    public final void setRayDelta(float fRayDelta) {
        m_fRayDelta = fRayDelta;
    }

    /**
     * Set the reduction factor for estimating the initial ellipsoid, as described in BrainExtraction.pdf. The default
     * value is 0.6.
     *
     * @param  fReduction  the amount to reduce the axis of the ellipsoid.
     */
    public final void setReductionX(float fReduction) {
        m_fReductionX = fReduction;
    }

    /**
     * Set the reduction factor for estimating the initial ellipsoid, as described in BrainExtraction.pdf. The default
     * value is 0.5.
     *
     * @param  fReduction  the amount to reduce the axis of the ellipsoid.
     */
    public final void setReductionY(float fReduction) {
        m_fReductionY = fReduction;
    }

    /**
     * Set the reduction factor for estimating the initial ellipsoid, as described in BrainExtraction.pdf. The default
     * value is 0.6.
     *
     * @param  fReduction  the amount to reduce the axis of the ellipsoid.
     */
    public final void setReductionZ(float fReduction) {
        m_fReductionZ = fReduction;
    }

    /**
     * Sets whether to save a surface mesh of the extracted brain.
     *
     * @param  saveMesh  whether to save a brain surface mesh
     */
    public void setSaveBrainMesh(boolean saveMesh) {
        saveBrainMesh = saveMesh;
    }

    /**
     * Sets whether or not second stage erosion occurs taking edge values exceeding median by a factor >= aboveMedian to
     * zero.
     *
     * @param  secondStageErosion  DOCUMENT ME!
     */
    public final void setSecondStageErosion(boolean secondStageErosion) {
        this.secondStageErosion = secondStageErosion;
    }

    /**
     * Set the stiffness of the mesh, as described in BrainExtraction.pdf, that is part of the surface normal term in
     * the surface evolution. The default value is 0.1.
     *
     * @param  fStiffness  the new stiffness
     */
    public final void setStiffness(float fStiffness) {
        m_fStiffness = fStiffness;
    }

    /**
     * Don't try estimating ellipse as initial surface use sphere estimate.
     *
     * @param  flag  if true estimate initial surface with sphere and not ellipse.
     */
    public final void setUseSphere(boolean flag) {
        useSphere = flag;
    }

    /**
     * The heart of the segmentation. This function is responsible for the evolution of the triangle mesh that
     * approximates the brain surface. The update has a tangential component, a surface normal component, and a vertex
     * normal component for each vertex in the mesh. The first two components control the geometry of the mesh. The last
     * component is based on the MRI data itself. See BrainExtraction.pdf for a detailed description of the update
     * terms.
     */
    public void updateMesh() {
        computeMeanEdgeLength();
        computeVertexNormals();
        computeVertexInformation();

        int xDim = image.getExtents()[0] - 1;
        int yDim = image.getExtents()[1] - 1;
        int zDim = image.getExtents()[2] - 1;

        // update the vertices
        for (int i = 0; i < m_iVQuantity; i++) {
            Point3f kVertex = m_akVertex[i];

            // tangential update
            fUpdate1 = 0.5f;
            kVertex.scaleAdd(fUpdate1, m_akSTangent[i], kVertex);

            // normal update
            float fUpdate2 = update2(i);
            float fUpdate3 = update3(i);
            kVertex.scaleAdd(fUpdate2, m_akSNormal[i], kVertex);
            kVertex.scaleAdd(fUpdate3, m_akVNormal[i], kVertex);

            if (kVertex.x < 0) {
                kVertex.x = 0;
            }

            if (kVertex.y < 0) {
                kVertex.y = 0;
            }

            if (kVertex.z < 0) {
                kVertex.z = 0;
            }

            if (kVertex.x > (xDim - 1)) {
                kVertex.x = xDim - 1;
            }

            if (kVertex.y > (yDim - 1)) {
                kVertex.y = yDim - 1;
            }

            if (kVertex.z > (zDim - 1)) {
                kVertex.z = zDim - 1;
            }

        }
    }

    /**
     * Compute the average length of all the edges in the triangle mesh.
     */
    protected void computeMeanEdgeLength() {
        m_fMeanEdgeLength = 0.0f;

        Iterator kEIter = m_kEMap.entrySet().iterator();
        Map.Entry kEntry = null;
        Vector3f kEdge = new Vector3f();

        while (kEIter.hasNext()) {
            kEntry = (Map.Entry) kEIter.next();

            Edge kE = (Edge) kEntry.getKey();
            Point3f kP0 = m_akVertex[kE.m_i0];
            Point3f kP1 = m_akVertex[kE.m_i1];
            kEdge.sub(kP1, kP0);
            m_fMeanEdgeLength += kEdge.length();
        }

        m_fMeanEdgeLength /= m_kEMap.size();
    }

    /**
     * Compute the median intensity of those voxels inside the initial ellipsoid. This intensity is used in the image
     * term of the surface evolution.
     */
    protected void computeMedianIntensity() {

        // compute median intensity of voxels inside initial ellipsoid
        float fInvLength0 = 1.0f / m_afLength[0];
        float fInvLength1 = 1.0f / m_afLength[1];
        float fInvLength2 = 1.0f / m_afLength[2];
        int iIQuantity = 0;
        int[] aiIntensity = new int[m_iQuantity];
        Point3f kP = new Point3f();

        for (int iZ = 0, i = 0; iZ < m_iZBound; iZ++) {

            for (int iY = 0; iY < m_iYBound; iY++) {

                for (int iX = 0; iX < m_iXBound; iX++) {
                    int iIntensity = m_aiImage[i++];

                    if (iIntensity > m_iBackThreshold) {

                        // transform to ellipsoid coordinates
                        float fX = (float) iX - m_kCenter.x;
                        float fY = (float) iY - m_kCenter.y;
                        float fZ = (float) iZ - m_kCenter.z;
                        kP.x = (fX * m_kRotate.m00) + (fY * m_kRotate.m10) + (fZ * m_kRotate.m20);
                        kP.y = (fX * m_kRotate.m01) + (fY * m_kRotate.m11) + (fZ * m_kRotate.m21);
                        kP.z = (fX * m_kRotate.m02) + (fY * m_kRotate.m12) + (fZ * m_kRotate.m22);

                        kP.x *= fInvLength0;
                        kP.y *= fInvLength1;
                        kP.z *= fInvLength2;

                        if (((kP.x * kP.x) + (kP.y * kP.y) + (kP.z * kP.z)) <= 1.0f) {

                            // voxel is inside ellipsoid
                            aiIntensity[iIQuantity++] = iIntensity;
                        }
                    }
                }
            }
        }

        if (iIQuantity != 0) {
            Arrays.sort(aiIntensity, 0, iIQuantity - 1);
        }

        m_iMedianIntensity = aiIntensity[iIQuantity / 2];
        Preferences.debug("Brain extractor: computeMedianIntensity: m_iMedianIntensity = " + m_iMedianIntensity + "\n");
    }

    /**
     * Let V[i] be a vertex in the triangle mesh. This function computes VMean[i], the average of the immediate
     * neighbors of V[i]. Define S[i] = VMean[i] - V[i]. The function also computes a surface normal SNormal[i], the
     * component of S[i] in the vertex normal direction. STangent[i] = S[i] - SNormal[i] is computed as an approximation
     * to a tangent to the surface. Finally, Curvature[i] is an approximation of the surface curvature at V[i].
     */
    protected void computeVertexInformation() {
        float fMinCurvature = Float.POSITIVE_INFINITY;
        float fMaxCurvature = Float.NEGATIVE_INFINITY;
        float fInvMeanLength = 1.0f / m_fMeanEdgeLength;

        int i;

        for (i = 0; i < m_iVQuantity; i++) {
            m_akVMean[i].set(0.0f, 0.0f, 0.0f);
        }

        Vector3f kS = new Vector3f();

        for (i = 0; i < m_iVQuantity; i++) {

            // compute the mean of the vertex neighbors
            // Point3f kMean = m_akVMean[i];
            UnorderedSetInt kAdj = m_akAdjacent[i];

            for (int j = 0; j < kAdj.getQuantity(); j++) {
                m_akVMean[i].add(m_akVertex[kAdj.get(j)]);
            }

            m_akVMean[i].scale(1.0f / kAdj.getQuantity());

            // compute the normal and tangential components of mean-vertex
            kS.sub(m_akVMean[i], m_akVertex[i]);
            m_akSNormal[i].scale(kS.dot(m_akVNormal[i]), m_akVNormal[i]);
            m_akSTangent[i].sub(kS, m_akSNormal[i]);

            // compute the curvature
            float fLength = m_akSNormal[i].length();
            m_afCurvature[i] = ((2.0f * fLength) * fInvMeanLength) * fInvMeanLength;

            if (m_afCurvature[i] < fMinCurvature) {
                fMinCurvature = m_afCurvature[i];
            }

            if (m_afCurvature[i] > fMaxCurvature) {
                fMaxCurvature = m_afCurvature[i];
            }
        }

        // compute the fractional function parameters for update2()
        m_fEParam = 0.5f * (fMinCurvature + fMaxCurvature);
        m_fFParam = 6.0f / (fMaxCurvature - fMinCurvature);
    }

    /**
     * Compute the vertex normals of the triangle mesh. Each vertex normal is the unitized average of the non-unit
     * triangle normals for those triangles sharing the vertex.
     */
    protected void computeVertexNormals() {

        // maintain a running sum of triangle normals at each vertex
        int i;

        for (i = 0; i < m_iVQuantity; i++) {
            m_akVNormal[i].set(0.0f, 0.0f, 0.0f);
        }

        Vector3f kEdge1 = new Vector3f();
        Vector3f kEdge2 = new Vector3f();
        Vector3f kNormal = new Vector3f();

        for (int iT = 0; iT < m_iTQuantity; iT++) {

            // get the vertices of the triangle
            int iP0 = m_aiConnect[3 * iT];
            int iP1 = m_aiConnect[(3 * iT) + 1];
            int iP2 = m_aiConnect[(3 * iT) + 2];
            Point3f kP0 = m_akVertex[iP0];
            Point3f kP1 = m_akVertex[iP1];
            Point3f kP2 = m_akVertex[iP2];

            // compute the triangle normal
            kEdge1.sub(kP1, kP0);
            kEdge2.sub(kP2, kP0);
            kNormal.cross(kEdge1, kEdge2);

            // the triangle normal partially contributes to each vertex normal
            m_akVNormal[iP0].add(kNormal);
            m_akVNormal[iP1].add(kNormal);
            m_akVNormal[iP2].add(kNormal);
        }

        for (i = 0; i < m_iVQuantity; i++) {
            m_akVNormal[i].normalize();
        }
    }

    /**
     * Approximate the brain surface by an ellipsoid. The approximation is based on locating all voxels of intensity
     * larger than a brightness threshold and that are part of the upper-half of the head. The idea is that the scalp
     * voxels in the upper-half form lie approximately on an ellipsoidal surface.<br>
     * <br>
     *
     * <p>NOTE. The assumption is that the traversal from bottom to top of head is in the y-direction of the 3D image.
     * It does not matter if the top of the head has y-values smaller/larger than those for the bottom of the head. If
     * this assumption is not met, the image should be permuted OR this code must be modified to attempt to recognize
     * the orientation of the head</p>
     *
     * @return  DOCUMENT ME!
     */
    protected boolean estimateEllipsoid() {

        // center-orient-length format for ellipsoid
        m_kRotate = new Matrix3f();
        m_afLength = new float[3];

        // Make the estimation numerically robust by tracking voxel positions
        // that are uniformly scaled into [-1,1]^3.
        float fBMax = (float) m_iXBound;

        if ((float) m_iYBound > fBMax) {
            fBMax = (float) m_iYBound;
        }

        if ((float) m_iZBound > fBMax) {
            fBMax = (float) m_iZBound;
        }

        float fInvBMax = 1.0f / fBMax;

        // The arrays "less" and "greater" store positions of bright voxels
        // that occur less or greater than YBound/2, respectively.  The
        // array with the smaller number of voxels represents the scalp
        // voxels in the upper-half of the head.  The comparison of counts
        // is based on empirical studies.
        Vector kLess = new Vector();
        Vector kGreater = new Vector();
        int iHalfYBound = m_iYBound / 2;
        int iHalfZBound = m_iZBound / 2;

        for (int iZ = 0, iIndex = 0; iZ < m_iZBound; iZ++) {

            for (int iY = 0; iY < m_iYBound; iY++) {

                for (int iX = 0; iX < m_iXBound; iX++) {

                    if (m_aiImage[iIndex++] >= m_iBrightThreshold) {
                        Point3f kVoxel = new Point3f(fInvBMax * iX, fInvBMax * iY, fInvBMax * iZ);

                        if (orientationFlag == AlgorithmBrainExtractor.SAT_COR) {

                            if (iY < iHalfYBound) {
                                kLess.add(kVoxel);
                            } else {
                                kGreater.add(kVoxel);
                            }
                        } else { // Axial image

                            if (iZ < iHalfZBound) {
                                kLess.add(kVoxel);
                            } else {

                                // kGreater.add(kVoxel);
                                kLess.add(kVoxel);
                            }
                        }
                    }
                }
            }
        }

        // Fit points with an ellipsoid.  The algorithm uses a least-squares
        // estimation of the coefficients for a quadratic equation that
        // represents the ellipsoid.
        AlgorithmQuadraticFit kQFit;
        kQFit = new AlgorithmQuadraticFit(kLess);

        // get the orientation matrix
        m_kRotate.set(kQFit.getOrient());

        // compute the semi-axis lengths
        m_afLength[0] = kQFit.getConstant() / kQFit.getDiagonal(0);
        m_afLength[1] = kQFit.getConstant() / kQFit.getDiagonal(1);
        m_afLength[2] = kQFit.getConstant() / kQFit.getDiagonal(2);

        // assert: m_afLength[0] > 0 && m_afLength[1] > 0 && m_afLength[2] > 0
        m_afLength[0] = (float) Math.sqrt(m_afLength[0]);
        m_afLength[1] = (float) Math.sqrt(m_afLength[1]);
        m_afLength[2] = (float) Math.sqrt(m_afLength[2]);

        if (m_afLength[0] > 1) {
            m_afLength[0] = 0.75f;
        }

        if (m_afLength[1] > 1) {
            m_afLength[1] = 0.75f;
        }

        if (m_afLength[2] > 1) {
            m_afLength[2] = 0.75f;
        }

        m_afLength[0] *= fBMax;
        m_afLength[1] *= fBMax;
        m_afLength[2] *= fBMax;

        // Use a smaller version of the ellipsoid for the initial mesh.  The
        // default reduction is 0.6, 0.4, 0,6.
        m_afLength[0] *= m_fReductionX;
        m_afLength[1] *= m_fReductionY;
        m_afLength[2] *= m_fReductionZ;

        Preferences.debug("Brain extractor: extimateEllipsoid: ellipse length 1 = " + m_afLength[0] + "\n");
        Preferences.debug("Brain extractor: extimateEllipsoid: ellipse length 2 = " + m_afLength[1] + "\n");
        Preferences.debug("Brain extractor: extimateEllipsoid: ellipse length 3 = " + m_afLength[2] + "\n");

        if ((m_afLength[0] > 0) && (m_afLength[1] > 0) && (m_afLength[2] > 0)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Approximate the intial brain surface by an sphere. Find the center of mass and approimate radius
     */
    protected void estimateSphere() {

        // center-orient-length format for ellipsoid
        m_kRotate = new Matrix3f();
        m_afLength = new float[3];

        m_kRotate.setIdentity();

        int count = 0;

        for (int iZ = 0, iIndex = 0; iZ < m_iZBound; iZ++) {

            for (int iY = 0; iY < m_iYBound; iY++) {

                for (int iX = 0; iX < m_iXBound; iX++) {

                    if (m_aiImage[iIndex++] >= m_iBackThreshold) {
                        count++;
                    }
                }
            }
        }

        float radius = (float) Math.pow(0.75 * (1 / Math.PI) * count, 0.333333);
        Preferences.debug("Brain extractor: extimateSphere: radius = " + radius + "\n");

        // Use a smaller version of the sphere for the initial mesh.  The
        // default reduction is 0.75.
        // m_afLength[0] *= m_fReduction;
        // m_afLength[1] *= m_fReduction;
        // m_afLength[2] *= m_fReduction;
        m_afLength[0] = radius * 0.45f;
        m_afLength[1] = radius * 0.45f;
        m_afLength[2] = (radius * 0.3f) * (m_fXDelta / m_fZDelta);

        Preferences.debug("Brain extractor: extimateSphere:  sphere length 1 = " + m_afLength[0] + "\n");
        Preferences.debug("Brain extractor: extimateSphere:  sphere length 2 = " + m_afLength[1] + "\n");
        Preferences.debug("Brain extractor: extimateSphere:  sphere length 3 = " + m_afLength[2] + "\n");
    }

    /**
     * Identify voxels enclosed by the brain surface by using a flood fill. The flood fill is nonrecursive to avoid
     * overflowing the program stack.
     *
     * @param  iX  the x-value of the seed point for the fill
     * @param  iY  the y-value of the seed point for the fill
     * @param  iZ  the z-value of the seed point for the fill
     */
    protected void floodFill(int iX, int iY, int iZ) {

        // Allocate the maximum amount of space needed.   An empty stack has
        // iTop == -1.
        int[] aiXStack = new int[m_iQuantity];
        int[] aiYStack = new int[m_iQuantity];
        int[] aiZStack = new int[m_iQuantity];

        // An empty stack has iTop = -1.  Push seed point onto stack.  All
        // points pushed onto stack have background color zero.
        int iTop = 0;
        aiXStack[iTop] = iX;
        aiYStack[iTop] = iY;
        aiZStack[iTop] = iZ;

        while (iTop >= 0) // stack is not empty
        {

            // Read top of stack.  Do not pop since we need to return to this
            // top value later to restart the fill in a different direction.
            iX = aiXStack[iTop];
            iY = aiYStack[iTop];
            iZ = aiZStack[iTop];

            // fill the pixel
            m_aiMask[getIndex(iX, iY, iZ)] = 2;

            int iXp1 = iX + 1;

            if ((iXp1 < m_iXBound) && (m_aiMask[getIndex(iXp1, iY, iZ)] == 0)) {

                // push pixel with background color
                iTop++;
                aiXStack[iTop] = iXp1;
                aiYStack[iTop] = iY;
                aiZStack[iTop] = iZ;

                continue;
            }

            int iXm1 = iX - 1;

            if ((0 <= iXm1) && (m_aiMask[getIndex(iXm1, iY, iZ)] == 0)) {

                // push pixel with background color
                iTop++;
                aiXStack[iTop] = iXm1;
                aiYStack[iTop] = iY;
                aiZStack[iTop] = iZ;

                continue;
            }

            int iYp1 = iY + 1;

            if ((iYp1 < m_iYBound) && (m_aiMask[getIndex(iX, iYp1, iZ)] == 0)) {

                // push pixel with background color
                iTop++;
                aiXStack[iTop] = iX;
                aiYStack[iTop] = iYp1;
                aiZStack[iTop] = iZ;

                continue;
            }

            int iYm1 = iY - 1;

            if ((0 <= iYm1) && (m_aiMask[getIndex(iX, iYm1, iZ)] == 0)) {

                // push pixel with background color
                iTop++;
                aiXStack[iTop] = iX;
                aiYStack[iTop] = iYm1;
                aiZStack[iTop] = iZ;

                continue;
            }

            int iZp1 = iZ + 1;

            if ((iZp1 < m_iZBound) && (m_aiMask[getIndex(iX, iY, iZp1)] == 0)) {

                // push pixel with background color
                iTop++;
                aiXStack[iTop] = iX;
                aiYStack[iTop] = iY;
                aiZStack[iTop] = iZp1;

                continue;
            }

            int iZm1 = iZ - 1;

            if ((0 <= iZm1) && (m_aiMask[getIndex(iX, iY, iZm1)] == 0)) {

                // push pixel with background color
                iTop++;
                aiXStack[iTop] = iX;
                aiYStack[iTop] = iY;
                aiZStack[iTop] = iZm1;

                continue;
            }

            // Done in all directions, pop and return to search other
            // directions.
            iTop--;
        }

        aiXStack = null;
        aiYStack = null;
        aiZStack = null;
    }

    /**
     * Tessellate a unit sphere centered at the origin. Start with an octahedron and subdivide. The final mesh is then
     * affinely mapped to the initial ellipsoid produced by estimateEllipsoid(). The subdivision scheme is described in
     * BrainExtraction.pdf.
     *
     * @param  iSubdivisions  the number of levels to subdivide the ellipsoid
     */
    protected void generateEllipsoidMesh(int iSubdivisions) {

        // Compute the number of vertices, edges, and triangles for an
        // octahedron subdivided to the specified level.  The recursions are
        // V1 = V0 + E0
        // E1 = 2*E0 + 3*T0
        // T1 = 4*T0
        m_iVQuantity = 6;
        m_iEQuantity = 12;
        m_iTQuantity = 8;

        int iStep;

        for (iStep = 1; iStep <= iSubdivisions; iStep++) {
            m_iVQuantity = m_iVQuantity + m_iEQuantity;
            m_iEQuantity = (2 * m_iEQuantity) + (3 * m_iTQuantity);
            m_iTQuantity = 4 * m_iTQuantity;
        }

        // See BrainExtraction.pdf for a description of the subdivision
        // algorithm.  The use of the HashMap m_kEMap is to store midpoint
        // information for edges so that triangles sharing an edge know what
        // the new vertices are for replacing themselves with subtriangles.
        m_akVertex = new Point3f[m_iVQuantity];
        m_aiConnect = new int[3 * m_iTQuantity];
        m_akAdjacent = new UnorderedSetInt[m_iVQuantity];

        int i;

        for (i = 0; i < m_iVQuantity; i++) {
            m_akVertex[i] = new Point3f();
            m_akAdjacent[i] = new UnorderedSetInt(6, 1);
        }

        m_akVertex[0].set(+1.0f, 0.0f, 0.0f);
        m_akVertex[1].set(-1.0f, 0.0f, 0.0f);
        m_akVertex[2].set(0.0f, +1.0f, 0.0f);
        m_akVertex[3].set(0.0f, -1.0f, 0.0f);
        m_akVertex[4].set(0.0f, 0.0f, +1.0f);
        m_akVertex[5].set(0.0f, 0.0f, -1.0f);

        m_aiConnect[0] = 4;
        m_aiConnect[1] = 0;
        m_aiConnect[2] = 2;
        m_aiConnect[3] = 4;
        m_aiConnect[4] = 2;
        m_aiConnect[5] = 1;
        m_aiConnect[6] = 4;
        m_aiConnect[7] = 1;
        m_aiConnect[8] = 3;
        m_aiConnect[9] = 4;
        m_aiConnect[10] = 3;
        m_aiConnect[11] = 0;
        m_aiConnect[12] = 5;
        m_aiConnect[13] = 2;
        m_aiConnect[14] = 0;
        m_aiConnect[15] = 5;
        m_aiConnect[16] = 1;
        m_aiConnect[17] = 2;
        m_aiConnect[18] = 5;
        m_aiConnect[19] = 3;
        m_aiConnect[20] = 1;
        m_aiConnect[21] = 5;
        m_aiConnect[22] = 0;
        m_aiConnect[23] = 3;

        m_kEMap = new HashMap();

        Integer kInvalid = new Integer(-1);
        m_kEMap.put(new Edge(0, 4), kInvalid);
        m_kEMap.put(new Edge(1, 4), kInvalid);
        m_kEMap.put(new Edge(2, 4), kInvalid);
        m_kEMap.put(new Edge(3, 4), kInvalid);
        m_kEMap.put(new Edge(0, 5), kInvalid);
        m_kEMap.put(new Edge(1, 5), kInvalid);
        m_kEMap.put(new Edge(2, 5), kInvalid);
        m_kEMap.put(new Edge(3, 5), kInvalid);
        m_kEMap.put(new Edge(0, 2), kInvalid);
        m_kEMap.put(new Edge(2, 1), kInvalid);
        m_kEMap.put(new Edge(1, 3), kInvalid);
        m_kEMap.put(new Edge(3, 0), kInvalid);

        int iPNext = 6, iTSubQuantity = 8, iCNext = 24;
        int i0, i1, i2, iP0, iP1, iP2, iT;

        for (iStep = 1; iStep <= iSubdivisions; iStep++) {

            // generate midpoints of edges
            Iterator kEIter = m_kEMap.entrySet().iterator();
            Map.Entry kEntry = null;

            while (kEIter.hasNext()) {
                kEntry = (Map.Entry) kEIter.next();

                Edge kE = (Edge) kEntry.getKey();
                Point3f kP0 = m_akVertex[kE.m_i0];
                Point3f kP1 = m_akVertex[kE.m_i1];
                Point3f kPMid = m_akVertex[iPNext];
                kPMid.add(kP0, kP1);

                float fInvLen = 1.0f /
                                    (float) Math.sqrt((kPMid.x * kPMid.x) + (kPMid.y * kPMid.y) + (kPMid.z * kPMid.z));
                kPMid.scale(fInvLen);
                kEntry.setValue(new Integer(iPNext));
                iPNext++;
            }

            // replace triangle by four subtriangles
            for (iT = 0; iT < iTSubQuantity; iT++) {
                i0 = 3 * iT;
                i1 = i0 + 1;
                i2 = i1 + 1;
                iP0 = m_aiConnect[i0];
                iP1 = m_aiConnect[i1];
                iP2 = m_aiConnect[i2];

                Edge kE01 = new Edge(iP0, iP1);
                Edge kE12 = new Edge(iP1, iP2);
                Edge kE20 = new Edge(iP2, iP0);
                int iM01 = ((Integer) m_kEMap.get(kE01)).intValue();
                int iM12 = ((Integer) m_kEMap.get(kE12)).intValue();
                int iM20 = ((Integer) m_kEMap.get(kE20)).intValue();

                // add new edges

                // replace current triangle by middle triangle
                m_aiConnect[i0] = iM01;
                m_aiConnect[i1] = iM12;
                m_aiConnect[i2] = iM20;

                // append remaining subtriangles
                m_aiConnect[iCNext++] = iP0;
                m_aiConnect[iCNext++] = iM01;
                m_aiConnect[iCNext++] = iM20;

                m_aiConnect[iCNext++] = iM01;
                m_aiConnect[iCNext++] = iP1;
                m_aiConnect[iCNext++] = iM12;

                m_aiConnect[iCNext++] = iM20;
                m_aiConnect[iCNext++] = iM12;
                m_aiConnect[iCNext++] = iP2;
            }

            iTSubQuantity *= 4;

            // remove old edges
            m_kEMap.clear();

            // add new edges
            for (iT = 0; iT < iTSubQuantity; iT++) {
                i0 = 3 * iT;
                i1 = i0 + 1;
                i2 = i1 + 1;
                iP0 = m_aiConnect[i0];
                iP1 = m_aiConnect[i1];
                iP2 = m_aiConnect[i2];
                m_kEMap.put(new Edge(iP0, iP1), kInvalid);
                m_kEMap.put(new Edge(iP1, iP2), kInvalid);
                m_kEMap.put(new Edge(iP2, iP0), kInvalid);
            }
        }

        // generate vertex adjacency
        for (iT = 0; iT < m_iTQuantity; iT++) {
            iP0 = m_aiConnect[3 * iT];
            iP1 = m_aiConnect[(3 * iT) + 1];
            iP2 = m_aiConnect[(3 * iT) + 2];

            m_akAdjacent[iP0].insert(iP1);
            m_akAdjacent[iP0].insert(iP2);
            m_akAdjacent[iP1].insert(iP0);
            m_akAdjacent[iP1].insert(iP2);
            m_akAdjacent[iP2].insert(iP0);
            m_akAdjacent[iP2].insert(iP1);
        }

        // rotate, scale, and translate sphere to get ellipsoid
        float resXFactor = m_fXDelta /
                               (float) Math.sqrt((m_kRotate.m00 * m_kRotate.m00 * m_fXDelta * m_fXDelta) +
                                                     (m_kRotate.m01 * m_kRotate.m01 * m_fYDelta * m_fYDelta) +
                                                     (m_kRotate.m02 * m_kRotate.m02 * m_fZDelta * m_fZDelta));
        float resYFactor = m_fYDelta /
                               (float) Math.sqrt((m_kRotate.m10 * m_kRotate.m10 * m_fXDelta * m_fXDelta) +
                                                     (m_kRotate.m11 * m_kRotate.m11 * m_fYDelta * m_fYDelta) +
                                                     (m_kRotate.m12 * m_kRotate.m12 * m_fZDelta * m_fZDelta));
        float resZFactor = m_fZDelta /
                               (float) Math.sqrt((m_kRotate.m20 * m_kRotate.m20 * m_fXDelta * m_fXDelta) +
                                                     (m_kRotate.m21 * m_kRotate.m21 * m_fYDelta * m_fYDelta) +
                                                     (m_kRotate.m22 * m_kRotate.m22 * m_fZDelta * m_fZDelta));

        for (i = 0; i < m_iVQuantity; i++) {
            m_akVertex[i].x *= m_afLength[0];
            m_akVertex[i].y *= m_afLength[1];
            m_akVertex[i].z *= m_afLength[2];

            // Transform for equal reslution units
            m_kRotate.transform(m_akVertex[i]);

            // Correct for unequal resolution units
            m_akVertex[i].x *= resXFactor;
            m_akVertex[i].y *= resYFactor;
            m_akVertex[i].z *= resZFactor;
            m_akVertex[i].add(m_kCenter);
        }
    }

    /**
     * A convenience function for mapping the 3D voxel position (iX,iY,iZ) to a 1D array index. The images are stored as
     * 1D arrays, so this function is used frequently.
     *
     * @param   iX  the x-value of the voxel position
     * @param   iY  the y-value of the voxel position
     * @param   iZ  the z-value of the voxel position
     *
     * @return  the 1D array index corresponding to (iX,iY,iZ)
     */
    protected final int getIndex(int iX, int iY, int iZ) {
        return iX + (m_iXBound * (iY + (m_iYBound * iZ)));
    }

    /**
     * Identify all voxels that are inside or on the mesh that represents the brain surface. The surface voxels are
     * constructed by rasterizing the triangles of the mesh in 3D. The centroid of these voxels is used as a seed point
     * for a flood fill of the region enclosed by the surface.
     *
     * @param  doErode  DOCUMENT ME!
     */
    protected void getInsideVoxels(boolean doErode) {

        for (int n = 0; n < m_aiMask.length; n++) {
            m_aiMask[n] = 0;
        }

        int i, iX, iY, iZ;

        for (int iT = 0; iT < m_iTQuantity; iT++) {

            // get the vertices of the triangle
            Point3f kV0 = m_akVertex[m_aiConnect[3 * iT]];
            Point3f kV1 = m_akVertex[m_aiConnect[(3 * iT) + 1]];
            Point3f kV2 = m_akVertex[m_aiConnect[(3 * iT) + 2]];

            // compute the axis-aligned bounding box of the triangle
            float fXMin = kV0.x, fXMax = fXMin;
            float fYMin = kV0.y, fYMax = fYMin;
            float fZMin = kV0.z, fZMax = fZMin;

            if (kV1.x < fXMin) {
                fXMin = kV1.x;
            } else if (kV1.x > fXMax) {
                fXMax = kV1.x;
            }

            if (kV1.y < fYMin) {
                fYMin = kV1.y;
            } else if (kV1.y > fYMax) {
                fYMax = kV1.y;
            }

            if (kV1.z < fZMin) {
                fZMin = kV1.z;
            } else if (kV1.z > fZMax) {
                fZMax = kV1.z;
            }

            if (kV2.x < fXMin) {
                fXMin = kV2.x;
            } else if (kV2.x > fXMax) {
                fXMax = kV2.x;
            }

            if (kV2.y < fYMin) {
                fYMin = kV2.y;
            } else if (kV2.y > fYMax) {
                fYMax = kV2.y;
            }

            if (kV2.z < fZMin) {
                fZMin = kV2.z;
            } else if (kV2.z > fZMax) {
                fZMax = kV2.z;
            }

            // Rasterize the triangle.  The rasterization is repeated in all
            // three coordinate directions to make sure that floating point
            // round-off errors do not cause any holes in the rasterized
            // surface.
            int iXMin = (int) fXMin, iXMax = (int) fXMax;
            int iYMin = (int) fYMin, iYMax = (int) fYMax;
            int iZMin = (int) fZMin, iZMax = (int) fZMax;
            int ptr;
            int end = m_aiMask.length;

            for (iY = iYMin; iY <= iYMax; iY++) {

                for (iZ = iZMin; iZ <= iZMax; iZ++) {
                    iX = getIntersectX(kV0, kV1, kV2, iY, iZ);

                    if (iX != -1) {
                        ptr = getIndex(iX, iY, iZ);

                        if ((ptr >= 0) && (ptr < end)) {
                            m_aiMask[ptr] = 1;
                        }
                        // m_aiMask[getIndex(iX,iY,iZ)] = 1;
                    }
                }
            }

            for (iX = iXMin; iX <= iXMax; iX++) {

                for (iZ = iZMin; iZ <= iZMax; iZ++) {
                    iY = getIntersectY(kV0, kV1, kV2, iX, iZ);

                    if (iY != -1) {
                        ptr = getIndex(iX, iY, iZ);

                        if ((ptr >= 0) && (ptr < end)) {
                            m_aiMask[ptr] = 1;
                        }
                        // m_aiMask[getIndex(iX,iY,iZ)] = 1;
                    }
                }
            }

            for (iX = iXMin; iX <= iXMax; iX++) {

                for (iY = iYMin; iY <= iYMax; iY++) {
                    iZ = getIntersectZ(kV0, kV1, kV2, iX, iY);

                    if (iZ != -1) {
                        ptr = getIndex(iX, iY, iZ);

                        if ((ptr >= 0) && (ptr < end)) {
                            m_aiMask[ptr] = 1;
                        }
                        // m_aiMask[getIndex(iX,iY,iZ)] = 1;
                    }
                }
            }
        }

        if (m_iDMax > 0) {

            // dilate to fill in gaps
            for (iZ = 1; iZ < (m_iZBound - 1); iZ++) {

                for (iY = 1; iY < (m_iYBound - 1); iY++) {

                    for (iX = 1; iX < (m_iXBound - 1); iX++) {
                        i = getIndex(iX, iY, iZ);

                        if (m_aiMask[i] == 1) {

                            for (int iDz = -m_iDMax; iDz <= m_iDMax; iDz++) {

                                for (int iDy = -m_iDMax; iDy <= m_iDMax; iDy++) {

                                    for (int iDx = -m_iDMax; iDx <= m_iDMax; iDx++) {
                                        int iX0 = iX + iDx;

                                        if ((iX0 < 0) || (iX0 >= m_iXBound)) {
                                            continue;
                                        }

                                        int iY0 = iY + iDy;

                                        if ((iY0 < 0) || (iY0 >= m_iYBound)) {
                                            continue;
                                        }

                                        int iZ0 = iZ + iDz;

                                        if ((iZ0 < 0) || (iZ0 >= m_iZBound)) {
                                            continue;
                                        }

                                        i = getIndex(iX0, iY0, iZ0);

                                        if (m_aiMask[i] == 0) {
                                            m_aiMask[i] = 2;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // reset to a binary image
            for (i = 0; i < m_iQuantity; i++) {

                if (m_aiMask[i] == 2) {
                    m_aiMask[i] = 1;
                }
            }
        }

        // compute centroid of the surface voxels to act as flood fill seed
        float fXC = 0.0f, fYC = 0.0f, fZC = 0.0f;
        int iCount = 0;

        for (iZ = 1; iZ < (m_iZBound - 1); iZ++) {

            for (iY = 1; iY < (m_iYBound - 1); iY++) {

                for (iX = 1; iX < (m_iXBound - 1); iX++) {

                    if (m_aiMask[getIndex(iX, iY, iZ)] > 0) {
                        fXC += (float) iX;
                        fYC += (float) iY;
                        fZC += (float) iZ;
                        iCount++;
                    }
                }
            }
        }

        float fInvCount = 1.0f / iCount;
        fXC *= fInvCount;
        fYC *= fInvCount;
        fZC *= fInvCount;

        floodFill((int) fXC, (int) fYC, (int) fZC);

        float fMin = (float) image.getMin();
        float fMax = (float) image.getMax();

        if (extractPaint) {
            BitSet bitSet = new BitSet(m_aiMask.length);

            for (int m = 0; m < m_aiMask.length; m++) {

                if (m_aiMask[m] != 0) {
                    bitSet.set(m);
                }
            }

            image.setMask(bitSet);
        } else {

            for (int m = 0; m < m_aiMask.length; m++) {

                if (m_aiMask[m] == 0) {
                    image.set(m, fMin);
                }
            }
        }

        float[] buffer = null;

        if (doErode) {

            // Optionally erode away all values >= median * aboveMedian that are next
            // to a zero background value or to another value already eroded
            progressBar.setMessage("Performing second stage edge erosion");

            float median = fMin + (m_iMedianIntensity * (fMax - fMin) / 1023);
            float th = median * aboveMedian;
            Preferences.debug("Brain extractor: getInsideVoxels: erode threshold = " + th + "\n");

            boolean[] chk;
            int sliceSize = m_iXBound * m_iYBound;
            int pos1;
            int pos2;
            int pos3;
            int pos;
            boolean found;
            int neighbors;

            try {
                buffer = new float[m_iQuantity];
                chk = new boolean[m_iQuantity];
            } catch (OutOfMemoryError e) {
                MipavUtil.displayError("AlgorithmBrainExtractor: Out of memory error creating buffer and chk");
                progressBar.dispose();
                setCompleted(false);

                return;
            }

            try {
                image.exportData(0, m_iQuantity, buffer);
            } catch (IOException er) {
                MipavUtil.displayError("AlgorithmBrainExtractor: IO error on image export data");
                progressBar.dispose();
                setCompleted(false);

                return;
            }

            for (i = 0; i < m_iQuantity; i++) {
                chk[i] = false;
            }

            // Set chk = true for x = 0 and x = m_iXBound - 1 when buffer = fMin
            for (iZ = 0, pos1 = 0; iZ < m_iZBound; iZ++, pos1 += sliceSize) {

                for (iY = 0, pos2 = 0; iY < m_iYBound; iY++, pos2 += m_iXBound) {
                    pos = pos1 + pos2;

                    if (buffer[pos] == fMin) {
                        chk[pos] = true;
                    }

                    pos += m_iXBound - 1;

                    if (buffer[pos] == fMin) {
                        chk[pos] = true;
                    }
                } // for (iY = 0,...)
            } // for (iZ = 0,...)

            // Set chk = true for y = 0 and y = m_iYBound - 1 when buffer = fMin
            for (iZ = 0, pos1 = 0; iZ < m_iZBound; iZ++, pos1 += sliceSize) {

                for (iX = 0; iX < m_iXBound; iX++) {
                    pos = pos1 + iX;

                    if (buffer[pos] == fMin) {
                        chk[pos] = true;
                    }

                    pos += (m_iYBound - 1) * m_iXBound;

                    if (buffer[pos] == fMin) {
                        chk[pos] = true;
                    }
                } // for (iX = 0;...)
            } // for (iZ = 0,...)

            // if a new position buffer = fMin is next to an old position buffer = fMin
            // for which the old position chk = true, then set the new positon chk = true
            found = true;

            while (found) {
                found = false;

                for (iZ = 0, pos1 = 0; iZ < m_iZBound; iZ++, pos1 += sliceSize) {

                    for (iY = 0, pos2 = 0; iY < m_iYBound; iY++, pos2 += m_iXBound) {
                        pos3 = pos1 + pos2;

                        for (iX = 0; iX < m_iXBound; iX++) {
                            pos = pos3 + iX;

                            if (chk[pos]) {

                                if ((iY >= 1) && (buffer[pos - m_iXBound] == fMin) && (!chk[pos - m_iXBound])) {
                                    found = true;
                                    chk[pos - m_iXBound] = true;
                                }

                                if ((iY < (m_iYBound - 1)) && (buffer[pos + m_iXBound] == fMin) &&
                                        (!chk[pos + m_iXBound])) {
                                    found = true;
                                    chk[pos + m_iXBound] = true;
                                }

                                if ((iX >= 1) && (buffer[pos - 1] == fMin) && (!chk[pos - 1])) {
                                    found = true;
                                    chk[pos - 1] = true;
                                }

                                if ((iX < (m_iXBound - 1)) && (buffer[pos + 1] == fMin) && (!chk[pos + 1])) {
                                    found = true;
                                    chk[pos + 1] = true;
                                }
                            } // if (chk[pos])
                        } // for (iX = 0;...)
                    } // for (iY = 0, ...)
                } // for (iZ = 0,...)
            } // while (found)

            // If a new position buffer >= median * aboveMedian and is next to
            // another position so that the eroded pixel width would be at least 2
            // pixels wide, then replace the new buffer positon with fMin
            found = true;

            while (found) {
                found = false;

                for (iZ = 0, pos1 = 0; iZ < m_iZBound; iZ++, pos1 += sliceSize) {

                    for (iY = 0, pos2 = 0; iY < m_iYBound; iY++, pos2 += m_iXBound) {
                        pos3 = pos1 + pos2;

                        for (iX = 0; iX < m_iXBound; iX++) {
                            pos = pos3 + iX;

                            if (chk[pos]) {

                                if ((iY >= 1) && (buffer[pos - m_iXBound] >= th)) {

                                    if (((iX >= 1) &&
                                             ((buffer[pos - m_iXBound - 1] >= th) || chk[pos - m_iXBound - 1])) ||
                                            ((iX < (m_iXBound - 1)) &&
                                                 ((buffer[pos - m_iXBound + 1] >= th) || chk[pos - m_iXBound + 1]))) {
                                        found = true;
                                        chk[pos - m_iXBound] = true;
                                        buffer[pos - m_iXBound] = fMin;
                                    }
                                }

                                if ((iY < (m_iYBound - 1)) && (buffer[pos + m_iXBound] >= th)) {

                                    if (((iX >= 1) &&
                                             ((buffer[pos + m_iXBound - 1] >= th) || chk[pos + m_iXBound - 1])) ||
                                            ((iX < (m_iXBound - 1)) &&
                                                 ((buffer[pos + m_iXBound + 1] >= th) || chk[pos + m_iXBound + 1]))) {
                                        found = true;
                                        chk[pos + m_iXBound] = true;
                                        buffer[pos + m_iXBound] = fMin;
                                    }
                                }

                                if ((iX >= 1) && (buffer[pos - 1] >= th)) {

                                    if (((iY >= 1) &&
                                             ((buffer[pos - m_iXBound - 1] >= th) || chk[pos - m_iXBound - 1])) ||
                                            ((iY < (m_iYBound - 1)) &&
                                                 ((buffer[pos + m_iXBound - 1] >= th) || chk[pos + m_iXBound - 1]))) {
                                        found = true;
                                        chk[pos - 1] = true;
                                        buffer[pos - 1] = fMin;
                                    }
                                }

                                if ((iX < (m_iXBound - 1)) && (buffer[pos + 1] >= th)) {

                                    if (((iY >= 1) &&
                                             ((buffer[pos - m_iXBound + 1] >= th) || chk[pos - m_iXBound + 1])) ||
                                            ((iY < (m_iYBound - 1)) &&
                                                 ((buffer[pos + m_iXBound + 1] >= th) || chk[pos + m_iXBound + 1]))) {
                                        found = true;
                                        chk[pos + 1] = true;
                                        buffer[pos + 1] = fMin;
                                    }
                                }
                            } // if (chk[pos])
                        } // for (iX = 0;...)
                    } // for (iY = 0, ...)
                } // for (iZ = 0,...)
            } // while (found)

            // If a pixel is surrounded by no more than 1 other nonbackground pixel,
            // then erode it away.
            found = true;

            while (found) {
                found = false;

                for (iZ = 0, pos1 = 0; iZ < m_iZBound; iZ++, pos1 += sliceSize) {

                    for (iY = 0, pos2 = 0; iY < m_iYBound; iY++, pos2 += m_iXBound) {
                        pos3 = pos1 + pos2;

                        for (iX = 0; iX < m_iXBound; iX++) {
                            pos = pos3 + iX;

                            if (buffer[pos] > fMin) {
                                neighbors = 0;

                                if ((iY >= 1) && (!chk[pos - m_iXBound])) {
                                    neighbors++;
                                }

                                if ((iY < (m_iYBound - 1)) && (!chk[pos + m_iXBound])) {
                                    neighbors++;
                                }

                                if ((iX >= 1) && (!chk[pos - 1])) {
                                    neighbors++;
                                }

                                if ((iX < (m_iXBound - 1)) && (!chk[pos + 1])) {
                                    neighbors++;
                                }

                                if (neighbors <= 1) {
                                    found = true;
                                    chk[pos] = true;
                                    buffer[pos] = fMin;
                                }
                            } // if (buffer[pos] > fMin)
                        } // for (iX = 0;...)
                    } // for (iY = 0, ...)
                } // for (iZ = 0,...)
            } // while (found)

            try {

                image.importData(0, buffer, false);


            } catch (IOException er) {
                MipavUtil.displayError("AlgorithmBrainExtractor: IO error on image import data");
                progressBar.dispose();
                setCompleted(false);

                return;
            }
        } // if (doErode)

        if (doErode && (buffer != null)) {
            nBrainVoxels = 0;

            for (int m = 0; m < buffer.length; m++) {

                if (buffer[m] > fMin) {
                    nBrainVoxels++;
                }
            }
        } else {
            nBrainVoxels = 0;

            for (int m = 0; m < m_aiMask.length; m++) {

                if (m_aiMask[m] > 0) {
                    nBrainVoxels++;
                }
            }
        }

        image.calcMinMax();
    }

    /**
     * Compute the point of intersection between a line (0,iY,iZ)+t(1,0,0) and the triangle defined by the three input
     * points. All calculations are in voxel coordinates and the x-value of the intersection point is truncated to an
     * integer.
     *
     * @param   kV0  a 3D vertex of the triangle
     * @param   kV1  a 3D vertex of the triangle
     * @param   kV2  a 3D vertex of the triangle
     * @param   iY   the y-value of the origin of the line
     * @param   iZ   the z-value of the origin of the line
     *
     * @return  the x-value of the intersection
     */
    protected int getIntersectX(Point3f kV0, Point3f kV1, Point3f kV2, int iY, int iZ) {

        // Compute the intersection, if any, by calculating barycentric
        // coordinates of the intersection of the line with the plane of
        // the triangle.  The barycentric coordinates are K0 = fC0/fDet,
        // K1 = fC1/fDet, and K2 = fC2/fDet with K0+K1+K2=1.  The intersection
        // point with the plane is K0*V0+K1*V1+K2*V2.  The point is inside
        // the triangle whenever K0, K1, and K2 are all in the interval [0,1].
        float fPu = iY - kV0.y, fPv = iZ - kV0.z;
        float fE1u = kV1.y - kV0.y, fE1v = kV1.z - kV0.z;
        float fE2u = kV2.y - kV0.y, fE2v = kV2.z - kV0.z;
        float fE1dP = (fE1u * fPu) + (fE1v * fPv);
        float fE2dP = (fE2u * fPu) + (fE2v * fPv);
        float fE1dE1 = (fE1u * fE1u) + (fE1v * fE1v);
        float fE1dE2 = (fE1u * fE2u) + (fE1v * fE2v);
        float fE2dE2 = (fE2u * fE2u) + (fE2v * fE2v);
        float fDet = (float) Math.abs((fE1dE1 * fE2dE2) - (fE1dE2 * fE1dE2));

        float fC1 = (fE2dE2 * fE1dP) - (fE1dE2 * fE2dP);

        if ((fC1 < 0.0f) || (fC1 > fDet)) {

            // ray does not intersect triangle
            return -1;
        }

        float fC2 = (fE1dE1 * fE2dP) - (fE1dE2 * fE1dP);

        if ((fC2 < 0.0f) || (fC2 > fDet)) {

            // ray does not intersect triangle
            return -1;
        }

        float fC0 = fDet - fC1 - fC2;

        if ((fC0 < 0.0f) || (fC0 > fDet)) {

            // ray does not intersect triangle
            return -1;
        }

        return (int) MipavMath.round(((fC0 * kV0.x) + (fC1 * kV1.x) + (fC2 * kV2.x)) / fDet);
    }

    /**
     * Compute the point of intersection between a line (iX,0,iZ)+t(0,1,0) and the triangle defined by the three input
     * points. All calculations are in voxel coordinates and the y-value of the intersection point is truncated to an
     * integer.
     *
     * @param   kV0  a 3D vertex of the triangle
     * @param   kV1  a 3D vertex of the triangle
     * @param   kV2  a 3D vertex of the triangle
     * @param   iX   the x-value of the origin of the line
     * @param   iZ   the z-value of the origin of the line
     *
     * @return  the y-value of the intersection
     */
    protected int getIntersectY(Point3f kV0, Point3f kV1, Point3f kV2, int iX, int iZ) {

        // Compute the intersection, if any, by calculating barycentric
        // coordinates of the intersection of the line with the plane of
        // the triangle.  The barycentric coordinates are K0 = fC0/fDet,
        // K1 = fC1/fDet, and K2 = fC2/fDet with K0+K1+K2=1.  The intersection
        // point with the plane is K0*V0+K1*V1+K2*V2.  The point is inside
        // the triangle whenever K0, K1, and K2 are all in the interval [0,1].
        float fPu = iX - kV0.x, fPv = iZ - kV0.z;
        float fE1u = kV1.x - kV0.x, fE1v = kV1.z - kV0.z;
        float fE2u = kV2.x - kV0.x, fE2v = kV2.z - kV0.z;
        float fE1dP = (fE1u * fPu) + (fE1v * fPv);
        float fE2dP = (fE2u * fPu) + (fE2v * fPv);
        float fE1dE1 = (fE1u * fE1u) + (fE1v * fE1v);
        float fE1dE2 = (fE1u * fE2u) + (fE1v * fE2v);
        float fE2dE2 = (fE2u * fE2u) + (fE2v * fE2v);
        float fDet = (float) Math.abs((fE1dE1 * fE2dE2) - (fE1dE2 * fE1dE2));

        float fC1 = (fE2dE2 * fE1dP) - (fE1dE2 * fE2dP);

        if ((fC1 < 0.0f) || (fC1 > fDet)) {

            // ray does not intersect triangle
            return -1;
        }

        float fC2 = (fE1dE1 * fE2dP) - (fE1dE2 * fE1dP);

        if ((fC2 < 0.0f) || (fC2 > fDet)) {

            // ray does not intersect triangle
            return -1;
        }

        float fC0 = fDet - fC1 - fC2;

        if ((fC0 < 0.0f) || (fC0 > fDet)) {

            // ray does not intersect triangle
            return -1;
        }

        int iY = (int) MipavMath.round(((fC0 * kV0.y) + (fC1 * kV1.y) + (fC2 * kV2.y)) / fDet);

        return iY;
    }

    /**
     * Compute the point of intersection between a line (iX,iY,0)+t(0,0,1) and the triangle defined by the three input
     * points. All calculations are in voxel coordinates and the z-value of the intersection point is truncated to an
     * integer.
     *
     * @param   kV0  a 3D vertex of the triangle
     * @param   kV1  a 3D vertex of the triangle
     * @param   kV2  a 3D vertex of the triangle
     * @param   iX   the x-value of the origin of the line
     * @param   iY   the y-value of the origin of the line
     *
     * @return  the z-value of the intersection
     */
    protected int getIntersectZ(Point3f kV0, Point3f kV1, Point3f kV2, int iX, int iY) {

        // Compute the intersection, if any, by calculating barycentric
        // coordinates of the intersection of the line with the plane of
        // the triangle.  The barycentric coordinates are K0 = fC0/fDet,
        // K1 = fC1/fDet, and K2 = fC2/fDet with K0+K1+K2=1.  The intersection
        // point with the plane is K0*V0+K1*V1+K2*V2.  The point is inside
        // the triangle whenever K0, K1, and K2 are all in the interval [0,1].
        float fPu = iX - kV0.x, fPv = iY - kV0.y;
        float fE1u = kV1.x - kV0.x, fE1v = kV1.y - kV0.y;
        float fE2u = kV2.x - kV0.x, fE2v = kV2.y - kV0.y;
        float fE1dP = (fE1u * fPu) + (fE1v * fPv);
        float fE2dP = (fE2u * fPu) + (fE2v * fPv);
        float fE1dE1 = (fE1u * fE1u) + (fE1v * fE1v);
        float fE1dE2 = (fE1u * fE2u) + (fE1v * fE2v);
        float fE2dE2 = (fE2u * fE2u) + (fE2v * fE2v);
        float fDet = (float) Math.abs((fE1dE1 * fE2dE2) - (fE1dE2 * fE1dE2));

        float fC1 = (fE2dE2 * fE1dP) - (fE1dE2 * fE2dP);

        if ((fC1 < 0.0f) || (fC1 > fDet)) {

            // ray does not intersect triangle
            return -1;
        }

        float fC2 = (fE1dE1 * fE2dP) - (fE1dE2 * fE1dP);

        if ((fC2 < 0.0f) || (fC2 > fDet)) {

            // ray does not intersect triangle
            return -1;
        }

        float fC0 = fDet - fC1 - fC2;

        if ((fC0 < 0.0f) || (fC0 > fDet)) {

            // ray does not intersect triangle
            return -1;
        }

        int iZ = (int) MipavMath.round(((fC0 * kV0.z) + (fC1 * kV1.z) + (fC2 * kV2.z)) / fDet);

        return iZ;
    }

    /**
     * Internal support for 'void save (String)' and 'void save (String, ModelTriangleMesh[])'. ModelTriangleMesh uses
     * this function to write vertices, normals, and connectivity indices to the file. ModelClodMesh overrides this to
     * additionally write collapse records to the file
     *
     * @param      flip  if the y axis should be flipped - true for extract, false for from another surface
     *
     * @exception  IOException  if there is an error writing to the file
     */
    protected void saveMesh(boolean flip) throws IOException {
        TransMatrix dicomMatrix;
        TransMatrix inverseDicomMatrix = null;
        double[][] inverseDicomArray = null;
        float[] coord;
        float[] tCoord;
        int i;

        String kName = image.getUserInterface().getDefaultDirectory() + image.getImageName() + "_brain.sur";

        if (image.getFileInfo()[0].getTransformID() == FileInfoBase.TRANSFORM_SCANNER_ANATOMICAL) {

            // Get the DICOM transform that describes the transformation from
            // axial to this image orientation
            dicomMatrix = (TransMatrix) (image.getMatrix().clone());
            inverseDicomMatrix = (TransMatrix) (image.getMatrix().clone());
            inverseDicomMatrix.invert();
            inverseDicomArray = inverseDicomMatrix.getMatrix();
            inverseDicomMatrix = null;
            coord = new float[3];
            tCoord = new float[3];

            for (i = 0; i < m_iVQuantity; i++) {

                // Change the voxel coordinate into millimeter space
                coord[0] = m_akVertex[i].x * m_fXDelta;
                coord[1] = m_akVertex[i].y * m_fYDelta;
                coord[2] = m_akVertex[i].z * m_fZDelta;

                // Convert the point to axial millimeter DICOM space
                dicomMatrix.transform(coord, tCoord);

                // Add in the DICOM origin
                m_akVertex[i].x = startLocation[0] + tCoord[0];
                m_akVertex[i].y = startLocation[1] + tCoord[1];
                m_akVertex[i].z = startLocation[2] + tCoord[2];
            }
        } // if (image.getFileInfo()[0].getTransformID() ==
        else {

            for (i = 0; i < m_iVQuantity; i++) {
                m_akVertex[i].x = (m_akVertex[i].x * m_fXDelta * direction[0]) + startLocation[0];
                m_akVertex[i].y = (m_akVertex[i].y * m_fYDelta * direction[1]) + startLocation[1];
                m_akVertex[i].z = (m_akVertex[i].z * m_fZDelta * direction[2]) + startLocation[2];
            }
        } // else

        ModelTriangleMesh[] newMesh = new ModelTriangleMesh[1];
        newMesh[0] = new ModelTriangleMesh(m_akVertex, m_aiConnect);

        ModelTriangleMesh.save(kName, newMesh, flip, direction, startLocation, box, inverseDicomArray);
    }

    /**
     * Compute the coefficient of the surface normal for the update of the mesh vertex V[i] in the SNormal[i] direction.
     * See BrainExtraction.pdf for a description of the update.
     *
     * @param   i  the index of the vertex to update
     *
     * @return  the coefficient of SNormal[i] for the update
     */
    protected float update2(int i) {
        float fArg = m_fFParam * (m_afCurvature[i] - m_fEParam);
        float fExpP = (float) Math.exp(fArg);
        float fExpN = (float) Math.exp(-fArg);
        float fTanh = (fExpP - fExpN) / (fExpP + fExpN);
        float fUpdate2 = 0.5f * m_fStiffness * (1.0f + fTanh);

        return fUpdate2;
    }

    /**
     * Compute the coefficient of the vertex normal for the update of the mesh vertex V[i] in the VNormal[i] direction.
     * See BrainExtraction.pdf for a description of the update.
     *
     * @param   i  the index of the vertex to update
     *
     * @return  the coefficient of VNormal[i] for the update
     */
    protected float update3(int i) {
        Point3f kVertex = m_akVertex[i];
        Vector3f kNormal = m_akVNormal[i];

        float fIMin = (float) m_iMedianIntensity;
        float fIMax = (float) m_iBackThreshold;

        // TO DO.  The ray depth should be in millimeters, not voxel units.
        // For now I'll just use the value as specified.  Later I need to
        // input the dx, dy, and dz terms for millimeters per voxel.
        Vector3f kDiff = new Vector3f();

        for (int j = 0; j < m_iMaxDepth; j++) {

            // get point on ray emanating from vertex into bounded region
            kDiff.scaleAdd(-m_fRayDelta * j, kNormal, kVertex);

            // nearest neighbor interpolation
            int iX = (int) (kDiff.x + 0.5f);
            int iY = (int) (kDiff.y + 0.5f);
            int iZ = (int) (kDiff.z + 0.5f);

            if (iX < 0) {
                iX = 0;
            } else if (iX >= m_iXBound) {
                iX = m_iXBound - 1;
            }

            if (iY < 0) {
                iY = 0;
            } else if (iY >= m_iYBound) {
                iY = m_iYBound - 1;
            }

            if (iZ < 0) {
                iZ = 0;
            } else if (iZ >= m_iZBound) {
                iZ = m_iZBound - 1;
            }

            float fValue = m_aiImage[iX + (m_iXBound * (iY + (m_iYBound * iZ)))];

            // update the minimum intensity
            if (fValue < fIMin) {
                fIMin = fValue;
            }

            if (j < m_iHalfMaxDepth) {

                // update the maximum intensity
                if (fValue > fIMax) {
                    fIMax = fValue;
                }
            }
        }

        float fRatio = (-m_fBrainSelection + ((fIMin - m_iMinThreshold) / (fIMax - m_iMinThreshold)));

        // float fUpdate3 = 0.05f * fRatio * m_fMeanEdgeLength;
        float fUpdate3 = f3Factor * fRatio * m_fMeanEdgeLength;

        return fUpdate3;
    }

    /**
     * Constructs a string of the contruction parameters and out puts the string to the messsage frame if the logging
     * procedure is turned on.
     */
    private void constructLog() {
        historyString = new String("ExtractBrain(" + orientationFlag + ", " + onlyEllipse + ", " + useSphere + ", " +
                                   nIterations + ", " + m_iMaxDepth + ", " + f3Factor + ", " + m_fStiffness + ", " +
                                   secondStageErosion + ", " + aboveMedian + ")\n");
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    /**
     * A representation of an edge for the vertex-edge-triangle table. This class stores the pair of vertex indices for
     * the end points of the edge. The edges <V0,V1> and <V1,V0> are considered to be identical. To simplify
     * comparisons, the class stores the ordered indices. The class extends Object to obtain support for hashing into a
     * map of edges.
     */
    protected class Edge extends Object {

        /** DOCUMENT ME! */
        public int m_i0, m_i1;

        /**
         * Constructs an edge in the table.
         *
         * @param  i0  a vertex index for an end point
         * @param  i1  a vertex index for an end point
         */
        public Edge(int i0, int i1) {

            if (i0 < i1) {

                // i0 is minimum
                m_i0 = i0;
                m_i1 = i1;
            } else {

                // i1 is minimum
                m_i0 = i1;
                m_i1 = i0;
            }
        }

        /**
         * Support for hashing into a map of edges.
         *
         * @param   kObject  an edge for comparison to the current one
         *
         * @return  true iff the edges are identical. Because the class stores ordered indices, it is not necessary to
         *          use the more expensive test (i0 == other.i0 && i1 == other.i1) || (i0 == other.i1 && i1 ==
         *          other.i0).
         */
        public boolean equals(Object kObject) {
            Edge kE = (Edge) kObject;

            return (m_i0 == kE.m_i0) && (m_i1 == kE.m_i1);
        }

        /**
         * Support for hashing into a map of edges.
         *
         * @return  the hash key for the edge
         */
        public int hashCode() {
            return (m_i0 << 16) | m_i1;
        }
    }

    /**
     * An unordered set of 'int' stored in an array. The class is used to store adjacency information for the triangles
     * in the mesh representing the brain surface. The reason for using an array is to minimize reallocations during
     * dynamic changes to a mesh. When an item is deleted from the set, the last element in the array is moved into that
     * location. The sets for which this class is used are typically small, so the costs for searching the unordered
     * items are not a factor.
     *
     * <p>The class has a static value DEFAULT_GROW that is used to increase the number of elements when a reallocation
     * must occur. The new storage size is the current maximum quantity plus the growth value.</p>
     */

    private class UnorderedSetInt {

        /** The array storage for the set. */
        protected int[] m_aiElement;

        /** On a reallocation, the old maximum quantity is incremented by this value. */
        protected int m_iGrow;

        /** The maximum number of elements in the array. It is always the case that m_iQuantity <= m_iMaxQuantity. */
        protected int m_iMaxQuantity;

        /** Support for remove and removeAt. */
        protected int m_iOldIndex, m_iNewIndex;

        /** The number of valid elements in the array. The valid indices are 0 <= i < m_iQuantity. */
        protected int m_iQuantity;

        /**
         * The default growth value for reallocations of the array representing the set. The application can change this
         * to whatever is appropriate for its purposes.
         */
        private int DEFAULT_GROW = 8;

        /**
         * Construct an empty unordered set. The initial maximum quantity and growth values are DEFAULT_GROW. When
         */
        public UnorderedSetInt() {
            reset();
        }

        /**
         * Create an unordered set that is a deep copy of the input set.
         *
         * @param  kSet  The input set to copy.
         */
        public UnorderedSetInt(UnorderedSetInt kSet) {
            copy(kSet);
        }

        /**
         * Construct an empty unordered set with the specified maximum quantity and growth values.
         *
         * @param  iMaxQuantity  The initial number of elements in the array. If the value is nonpositive, the initial
         *                       number is DEFAULT_GROW.
         * @param  iGrow         The growth amount for a reallocation. If a reallocation occurs, the new number of
         *                       elements is the current maximum quantity plus the growth value. If the input value is
         *                       nonpositive, the growth is set to DEFAULT_GROW.
         */
        public UnorderedSetInt(int iMaxQuantity, int iGrow) {
            reset(iMaxQuantity, iGrow);
        }

        /**
         * Append an element to the end of the storage array.
         *
         * @param   iElement  The element to append.
         *
         * @return  The array location that contains the newly appended element. A side effect of this call is
         *          reallocation of the storage array, if necessary.
         */
        public int append(int iElement) {

            if (m_iQuantity == m_iMaxQuantity) {
                int iNewMaxQuantity = m_iMaxQuantity + m_iGrow;
                int[] aiNewElement = new int[iNewMaxQuantity];
                System.arraycopy(m_aiElement, 0, aiNewElement, 0, m_iMaxQuantity);
                m_iMaxQuantity = iNewMaxQuantity;
                m_aiElement = aiNewElement;
            }

            int iLocation = m_iQuantity++;
            m_aiElement[iLocation] = iElement;

            return iLocation;
        }

        /**
         * Use exactly the amount of array storage for the current elements in the set. After the call, getQuantity()
         * and getMaximumQuantity() return the same value. This call does cause a reallocation.
         */
        public void compactify() {

            if (m_iQuantity > 0) {

                // Try Catch - Matt
                int[] aiNewElement = new int[m_iQuantity];
                System.arraycopy(m_aiElement, 0, aiNewElement, 0, m_iQuantity);
                m_iMaxQuantity = m_iQuantity;
                m_aiElement = aiNewElement;
            } else {
                reset();
            }
        }

        /**
         * Make a deep copy of the input set.
         *
         * @param  kSet  The set to make a deep copy of.
         */
        public void copy(UnorderedSetInt kSet) {
            m_iQuantity = kSet.m_iQuantity;
            m_iMaxQuantity = kSet.m_iMaxQuantity;
            m_iGrow = kSet.m_iGrow;
            m_aiElement = new int[m_iMaxQuantity];
            System.arraycopy(kSet.m_aiElement, 0, m_aiElement, 0, m_iMaxQuantity);
        }

        /**
         * Search the set to see if the input element currently exists.
         *
         * @param   iElement  The element to search for.
         *
         * @return  The value is true if and only if the element is found in the set.
         */
        public boolean exists(int iElement) {

            for (int i = 0; i < m_iQuantity; i++) {

                if (iElement == m_aiElement[i]) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Retrieve the element in the array location i. It is necessary that 0 <= i < getQuantity() in order to read
         * valid elements.
         *
         * @param   i  The array location whose element is to be retrieved.
         *
         * @return  The element in array location i.
         */
        public final int get(int i) {
            return m_aiElement[i];
        }

        /**
         * The growth value for reallocations. If a reallocation must occur, the new maximum quantity is the current
         * maximum quantity plus the growth amount.
         *
         * @return  The growth value.
         */
        public final int getGrow() {
            return m_iGrow;
        }

        /**
         * The maximum quantity of elements in the set. Not all elements are necessarily used. The used quantity is
         * provided by getQuantity().
         *
         * @return  The maximum quantity of elements in the set.
         */
        public final int getMaxQuantity() {
            return m_iMaxQuantity;
        }

        /**
         * On a call to remove or removeAt, the last element in the array is potentially moved to the array location
         * vacated by the removed element. The new location of the last element is retrived by this function. However,
         * if the last element is the one that was removed, this function returns -1. If you need the value, you must
         * call this function before the next call to remove or removeAt.
         *
         * @return  The new location of the last element that was moved.
         */
        public final int getNewIndex() {
            return m_iNewIndex;
        }

        /**
         * On a call to remove or removeAt, the last element in the array is moved to the array location vacated by the
         * removed element. The old location of the last element is retrived by this function. If you need the value,
         * you must call this function before the next call to remove or removeAt.
         *
         * @return  The old location of the last element that was moved.
         */
        public final int getOldIndex() {
            return m_iOldIndex;
        }

        /**
         * The current number of valid elements in the array. This number is less than or equal to the maximum quantity.
         * The elements with indices 0 through getQuantity()-1 are the valid ones.
         *
         * @return  The current number of valid elements.
         */
        public final int getQuantity() {
            return m_iQuantity;
        }

        /**
         * Insert an element into the set.
         *
         * @param   iElement  The element to insert.
         *
         * @return  The value is true if and only if the element is inserted. The input element is not inserted if it
         *          already exists in the set. A side effect of this call is reallocation of the storage array, if
         *          necessary.
         */
        public boolean insert(int iElement) {
            int i;

            for (i = 0; i < m_iQuantity; i++) {

                if (iElement == m_aiElement[i]) {
                    return false;
                }
            }

            if (m_iQuantity == m_iMaxQuantity) {
                int iNewMaxQuantity = m_iMaxQuantity + m_iGrow;
                int[] aiNewElement = new int[iNewMaxQuantity];
                System.arraycopy(m_aiElement, 0, aiNewElement, 0, m_iMaxQuantity);
                m_iMaxQuantity = iNewMaxQuantity;
                m_aiElement = aiNewElement;
            }

            m_aiElement[m_iQuantity++] = iElement;

            return true;
        }

        /**
         * Remove the specified element from the set.
         *
         * @param   iElement  The element to remove.
         *
         * @return  The value is true if and only if the element existed and was removed. The last element is
         *          potentially moved into the slot vacated by the specified element. If needed, the old and new
         *          locations of the last element can be retrieved by calls to getOldIndex() and getNewIndex(). If the
         *          last element was the one removed, getNewIndex() returns -1.
         */
        public boolean remove(int iElement) {

            for (int i = 0; i < m_iQuantity; i++) {

                if (iElement == m_aiElement[i]) {
                    m_iQuantity--;
                    m_iOldIndex = m_iQuantity;

                    if (i != m_iQuantity) {
                        m_aiElement[i] = m_aiElement[m_iQuantity];
                        m_iNewIndex = i;
                    } else {
                        m_iNewIndex = -1;
                    }

                    return true;
                }
            }

            return false;
        }

        /**
         * Remove the element from the set in the specified location.
         *
         * @param   i  The array location whose element is to be removed.
         *
         * @return  The value is true if and only if the input location is within the valid index range 0 <= i <
         *          getQuantity(). The last element is potentially moved into the slot vacated by the specified element.
         *          If needed, the old and new locations of the last element can be retrieved by calls to getOldIndex()
         *          and getNewIndex(). If the last element was the one removed, getNewIndex() returns -1.
         */
        public boolean removeAt(int i) {

            if ((0 <= i) && (i < m_iQuantity)) {
                m_iQuantity--;
                m_iOldIndex = m_iQuantity;

                if (i != m_iQuantity) {
                    m_aiElement[i] = m_aiElement[m_iQuantity];
                    m_iNewIndex = i;
                } else {
                    m_iNewIndex = -1;
                }

                return true;
            }

            return false;
        }

        /**
         * Reset the unordered set to its initial state. The old array is deleted. The new array has a maximum quantity
         * of DEFAULT_GROW and the growth value is DEFAULT_GROW.
         */
        public void reset() {
            reset(0, 0);
        }

        /**
         * Reset the unordered set to the specified state. The old array is deleted. The new array has a maximum
         * quantity and growth value as specified by the inputs.
         *
         * @param  iMaxQuantity  The new maximum quantity for the array.
         * @param  iGrow         The new growth value.
         */
        public void reset(int iMaxQuantity, int iGrow) {

            if (iMaxQuantity <= 0) {
                iMaxQuantity = DEFAULT_GROW;
            }

            if (iGrow <= 0) {
                iGrow = DEFAULT_GROW;
            }

            m_iQuantity = 0;
            m_iMaxQuantity = iMaxQuantity;
            m_iGrow = iGrow;
            m_aiElement = new int[m_iMaxQuantity];
        }

        /**
         * Assign the specified element to array location i. It is necessary that 0 <= i < getMaxQuantity().
         *
         * @param  i         The array location to assign to.
         * @param  iElement  The element to assign to array location i.
         */
        public final void set(int i, int iElement) {
            m_aiElement[i] = iElement;
        }
    }
}
