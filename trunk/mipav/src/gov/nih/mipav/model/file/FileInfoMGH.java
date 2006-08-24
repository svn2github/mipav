package gov.nih.mipav.model.file;


import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;
import gov.nih.mipav.view.dialogs.*;

import java.util.*;


/**
 * This structures contains the information that describes how a NIFTI image is stored on disk. NIFTI is intended to be
 * "mostly compatible" with the ANALYZE 7.5 file format. Most of the "unused" fields in that format have been taken, and
 * some of the lesser-used fields have been co-opted for other purposes. We have extended this format to store image
 * orientation and the origin. We have used unused variables to store these data. Almost all programs ignore these
 * variables and should not have any problems reading images saved with MIPAV, except SPM. A new format for MIPAV is now
 * XML based.
 *
 * <p>RGB NIFTI images are store in chunky format rgb, rgb, rgb ......</p>
 *
 * <p>Note that there is a short datatype field.</p>
 *
 * @see  FileNIFTI
 */

public class FileInfoMGH extends FileInfoBase {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    //private static final long serialVersionUID;
    
    /** Currently version number is 1 */
    private int version = 1;
    
    private int dof;

    /** Default, no intent indicated. */
    public static final short NIFTI_INTENT_NONE = 0;

    /** Correlation coefficient R (1 param): p1 = degrees of freedom R/sqrt(1-R*R) is t-distributed with p1 DOF. */
    public static final short NIFTI_INTENT_CORREL = 2;

    /** Student t statistic (1 param): p1 = DOF. */
    public static final short NIFTI_INTENT_TTEST = 3;

    /** Fisher F statistic (2 params): p1 = numerator DOF, p2 = denominator DOF. */
    public static final short NIFTI_INTENT_FTEST = 4;

    /** Standard normal (0 params): Density = N(0,1). */
    public static final short NIFTI_INTENT_ZSCORE = 5;

    /** Chi-squared (1 param): p1 = DOF Density(x) proportional to exp(-x/2) * x^(p1/2 - 1). */
    public static final short NIFTI_INTENT_CHISQ = 6;

    /** Beta distribution (2 params): p1 = a, p2 = b Density (x) proportional to x^(a-1) * (1-x)^(b-1). */
    public static final short NIFTI_INTENT_BETA = 7;

    /**
     * Binomial distribution (2 params): p1 = number of trials, p2 = probability per trial Prob(x) = (p1 choose x) *
     * p2^x * (1-p2)^(p1-x), for x = 0,1,...p1.
     */
    public static final short NIFTI_INTENT_BINOM = 8;

    /** Gamma distribution (2 params): p1 = shape, p2 = scale Density (x) proportional to x^(p1-1) * exp(-p2*x). */
    public static final short NIFTI_INTENT_GAMMA = 9;

    /** Poisson distribution (1 param): p1 = mean Prob(x) = exp(-p1) * p1^x/x!, for x = 0, 1, 2, ... */
    public static final short NIFTI_INTENT_POISSON = 10;

    /** Normal distribution (2 params): p1 = mean, p2 = standard deviation. */
    public static final short NIFTI_INTENT_NORMAL = 11;

    /**
     * Noncentral F statistic (3 params): p1 = numerator DOF, p2 = denominator DOF, p3 = numerator noncentrality
     * parameter.
     */
    public static final short NIFTI_INTENT_FTEST_NONC = 12;

    /** Noncentral chi-squared statistic (2 params): p1 = DOF, p2 = noncentrality parameter. */
    public static final short NIFTI_INTENT_CHISQ_NONC = 13;

    /**
     * Logistic distribution (2 params): p1 = location, p2 = scale Density (x) proportional to sech^2((x-p1)/(2*p2)).
     */
    public static final short NIFTI_INTENT_LOGISTIC = 14;

    /** Laplace distribution (2 params): p1 = location, p2 = scale Density (x) proportional to exp(-abs(x-p1)/p2). */
    public static final short NIFTI_INTENT_LAPLACE = 15;

    /** Uniform distribution: p1 = lower end, p2 = upper end. */
    public static final short NIFTI_INTENT_UNIFORM = 16;

    /** Noncentral t statistic (2 params): p1 = DOF, p2 = noncentrality parameter. */
    public static final short NIFTI_INTENT_TTEST_NONC = 17;

    /**
     * Weibull distribution (3 params): p1 = location, p2 = scale, p3 = power Density (x) proportional to
     * ((x-p1)/p2)^(p3-1) * exp(-((x-p1)/p2)^p3) for x > p1.
     */
    public static final short NIFTI_INTENT_WEIBULL = 18;

    /**
     * Chi distribution (1 param): p1 = DOF Density (x) proportional to x^(p1-1) * exp(-x^2/2) for x > 0 p1 = 1 = 'half
     * normal distribution p1 = 2 = Rayleigh distribution p1 = 3 = Maxwell_Boltzmann distribution.
     */

    public static final short NIFTI_INTENT_CHI = 19;

    /**
     * Inverse Gaussian (2 params): p1 = mu, p2 = lambda Density (x) proportional to exp(-p2*(x-p1)^2/(2*p1^2*x)) / x^3
     * for x > 0.
     */
    public static final short NIFTI_INTENT_INVGAUSS = 20;

    /** Extreme value type I (2 params): p1 = location, p2 = scale cdf(x) = exp(-exp(-(x-p1)/p2)). */
    public static final short NIFTI_INTENT_EXTVAL = 21;

    /** Data is a 'p-value' (no params). */
    public static final short NIFTI_INTENT_PVAL = 22;

    /**
     * Data is ln(p-value) (no params). To be safe, a program should compute p = exp(-abs(this_value)). The
     * nifti_stats.c library returns this_value as positive, so that this_value = -log(p).
     */
    public static final short NIFTI_INTENT_LOGPVAL = 23;

    /**
     * Data is log10(p-value) (no params). To be safe, a program should compute p = pow(10.,-abs(this_value)). The
     * nifti_stats.c library returns this_value as positive, so that this_value = -log10(p).
     */
    public static final short NIFTI_INTENT_LOG10PVAL = 24;

    /** Smallest intent code that indicates a statistic. */
    public static final short NIFTI_FIRST_STATCODE = 2;

    /** Largest intent code that indicates a statistic. */
    public static final short NIFTI_LAST_STATCODE = 22;

    // The following intent code values are not for statistics
    /**
     * The value at each voxel is an estimate of some parameter The name of the parameter may be stored in intentName.
     */
    public static final short NIFTI_INTENT_ESTIMATE = 1001;

    /**
     * The value at each voxel is an index into some set of labels The filename with the labels may be stored in
     * auxFile.
     */
    public static final short NIFTI_INTENT_LABEL = 1002;

    /** The value at each voxel is an index into the NeuroNames labels set. */
    public static final short NIFTI_INTENT_NEURONAME = 1003;

    /**
     * To store an M x N matrix at each voxel Dataset must have a 5th dimension (dim[0] = 5 and dim[5] > 1) dim[5] must
     * be M*N intentP1 must be M (in float format) intentP2 must be N (in float format) the matrix values A[i][j] are
     * stored in row order: A[0][0] A[0][1] ... A[0][N-1] A[1][0] A[1][1] ... A[1][N-1] ... A[M-1][0] A[M-1][1] ...
     * A[M-1][N-1]
     */
    public static final short NIFTI_INTENT_GENMATRIX = 1004;

    /**
     * To store an NxN symmetric matrix at each voxel Dataset must have a 5th dimension dim[5] must be N*(N+1)/2
     * intentP1 must be N (in float format) The matrix values A[i][j] are stored in row order A[0][0] A[1][0] A[1][1]
     * A{2][0] A[2][1] A[2][2].
     */
    public static final short NIFTI_INTENT_SYMMATRIX = 1005;

    /**
     * To signify that the vector value at each voxel is to be taken as a displacement field or vector: Dataset must
     * have a 5th dimension dim[5] must be the dimensionality of the displacement vector (e.g., 3 for spatial
     * displacement, 2 for in-plane).
     */
    public static final short NIFTI_INTENT_DISPVECT = 1006; /* specifically for displacements */

    /** DOCUMENT ME! */
    public static final short NIFTI_INTENT_VECTOR = 1007; /* for any other type of vector */

    /**
     * To signify that the vector value at each voxel is really a spatial coordinate (e.g., the verticies or nodes of a
     * surface mesh): dim[0] = 5 dim[1] = number of points dim[2] = dim[3] = dim[4] = 1 dim[5] must be the
     * dimensionality of space (e.g., 3 => 3D space) intentName may describe the object these points come from (e.g.,
     * "pial", "gray/white", "EEG", "MEG").
     */
    public static final short NIFTI_INTENT_POINTSET = 1008;

    /**
     * To signify that the vector value at each voxel is really a triple of indexes (e.g., forming a triangle) from a
     * pointset dataset: Dataset must have a fifth dimension dim[0] = 5 dim[1] = number of triangles dim[2] = dim[3] =
     * dim[4] = 1 dim[5] = 3 dataType should be an integer type (preferably DT_INT32) The data values are indexes
     * (0,1,...) into a pointset dataset.
     */
    public static final short NIFTI_INTENT_TRIANGLE = 1009;

    /**
     * To signify that the vector value at each voxel is a quaternion: Dataset must have a 5th dimension dim[0] = 5
     * dim[5] = 4 dataType should be a floating point type.
     */
    public static final short NIFTI_INTENT_QUATERNION = 1010;

    /**
     * Dimensionless value - no params - although, as in _ESTIMATE the name of the parameter may be stored in
     * intent_name.
     */
    public static final short NIFTI_INTENT_DIMLESS = 1011;

    /** DOCUMENT ME! */
    public static final short DT_NONE = 0;

    /** DOCUMENT ME! */
    public static final short DT_UNKNOWN = 0;

    /** DOCUMENT ME! */
    public static final short DT_BINARY = 1;

    /** DOCUMENT ME! */
    public static final short NIFTI_TYPE_UINT8 = 2;

    /** DOCUMENT ME! */
    public static final short NIFTI_TYPE_INT16 = 4;

    /** DOCUMENT ME! */
    public static final short NIFTI_TYPE_INT32 = 8;

    /** DOCUMENT ME! */
    public static final short NIFTI_TYPE_FLOAT32 = 16;

    /** 64 bit COMPLEX = 2 32 bit floats. */
    public static final short NIFTI_TYPE_COMPLEX64 = 32;

    /** DOCUMENT ME! */
    public static final short NIFTI_TYPE_FLOAT64 = 64;

    /** DOCUMENT ME! */
    public static final short NIFTI_TYPE_RGB24 = 128;

    /** DOCUMENT ME! */
    public static final short NIFTI_TYPE_INT8 = 256;

    /** DOCUMENT ME! */
    public static final short NIFTI_TYPE_UINT16 = 512;

    /** DOCUMENT ME! */
    public static final short NIFTI_TYPE_UINT32 = 768;

    /** DOCUMENT ME! */
    public static final short NIFTI_TYPE_INT64 = 1024;

    /** DOCUMENT ME! */
    public static final short NIFTI_TYPE_UINT64 = 1280;

    /** DOCUMENT ME! */
    public static final short NIFTI_TYPE_FLOAT128 = 1536;

    /** 128 bit COMPLEX = 2 64 bit floats. */
    public static final short NIFTI_TYPE_COMPLEX128 = 1792;

    /** 256 bit COMPLEX = 2 128 bit floats. */
    public static final short NIFTI_TYPE_COMPLEX256 = 2048;

    /** DOCUMENT ME! */
    public static final int NIFTI_UNITS_UNKNOWN = 0;

    /** DOCUMENT ME! */
    public static final int NIFTI_UNITS_METER = 1;

    /** DOCUMENT ME! */
    public static final int NIFTI_UNITS_MM = 2;

    /** DOCUMENT ME! */
    public static final int NIFTI_UNITS_MICRON = 3;

    /** DOCUMENT ME! */
    public static final int NIFTI_UNITS_SEC = 8;

    /** DOCUMENT ME! */
    public static final int NIFTI_UNITS_MSEC = 16;

    /** DOCUMENT ME! */
    public static final int NIFTI_UNITS_USEC = 24;

    /** DOCUMENT ME! */
    public static final int NIFTI_UNITS_HZ = 32;

    /** DOCUMENT ME! */
    public static final int NIFTI_UNITS_PPM = 40;

    /** DOCUMENT ME! */
    public static final int NIFTI_UNITS_RADS = 48;

    /** DOCUMENT ME! */
    public static final byte NIFTI_SLICE_SEQ_INC = 1;

    /** DOCUMENT ME! */
    public static final byte NIFTI_SLICE_SEQ_DEC = 2;

    /** DOCUMENT ME! */
    public static final byte NIFTI_SLICE_ALT_INC = 3;

    /** DOCUMENT ME! */
    public static final byte NIFTI_SLICE_ALT_DEC = 4;

    /** DOCUMENT ME! */
    public static final byte NIFTI_SLICE_ALT_INC2 = 5;

    /** DOCUMENT ME! */
    public static final byte NIFTI_SLICE_ALT_DEC2 = 6;

    // Codes for type of X, Y, Z coordinate system.
    /** Arbitrary coordinates. */
    public static final short NIFTI_XFORM_UNKNOWN = 0;

    /** Scanner based anatomical coordinates. */
    public static final short NIFTI_XFORM_SCANNER_ANAT = 1;

    /** Coordinates aligned to another file's or to anatomical "truth". */
    public static final short NIFTI_XFORM_ALIGNED_ANAT = 2;

    /** Coordinates aligned to Talairach-Tournoux Atlas; (0,0,0) = AC, etc. */
    public static final short NIFTI_XFORM_TALAIRACH = 3;

    /** MNI 152 normalized coordiantes. */
    public static final short NIFTI_XFORM_MNI_152 = 4;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** DOCUMENT ME! */
    private String aux_file = null;

    /** DOCUMENT ME! */
    private short bitpix = -1;

    /** range of calibration values. */
    private float cal_max = 0;

    /** values of 0.0 for both fields imply that no calibration min and max values are used ! */
    private float cal_min = 0;

    /** DOCUMENT ME! */
    private short coord_code = 0;

    /** DOCUMENT ME! */
    private String descrip = null;

    /** Is always 16384. */
    private int extents = 0;

    /** DOCUMENT ME! */
    private int freq_dim = 0;

    /** DOCUMENT ME! */
    private float funused3 = -1;

    /** DOCUMENT ME! */
    private short intentCode = 0;

    /** DOCUMENT ME! */
    private String intentName = null;

    /**
     * public short dim[] = new short[8]; // image dimension data stored in FileInfoBase dim[0] = number of dimensions;
     * usually 4 dim[1] = image width dim[2] = image height dim[3] = image depth (# of slices) dim[4] = volumes in image
     * --- must be one for 3D image.
     */
    private float intentP1;

    /** DOCUMENT ME! */
    private float intentP2;

    /** DOCUMENT ME! */
    private float intentP3;

    /** Transformation matrix. */
    private TransMatrix matrix = new TransMatrix(4);

    /** DOCUMENT ME! */
    private byte orient = -1;

    /** DOCUMENT ME! */
    private int phase_dim = 0;

    /** DOCUMENT ME! */
    private float scl_inter = 0.0f;

    /** Data is scaled according to:scaled_data[i] = unscaled_data[i]*scl_slope + scl_inter. */
    private float scl_slope = 1.0f;

    /** Should always be a length of 348. */
    private int sizeof_hdr = -1;

    /** DOCUMENT ME! */
    private int slice_dim = 0;

    /** DOCUMENT ME! */
    private byte sliceCode = 0;

    /** DOCUMENT ME! */
    private float sliceDuration = -1.0f;

    /** DOCUMENT ME! */
    private short sliceEnd = -1;

    /** DOCUMENT ME! */
    private short sliceStart = -1;


    /** Bits per pixel : 1,8,16,32,64,128 24(rgb). */
    private short sourceBitPix = -1;


    /** DOCUMENT ME! */
    private short sourceType = -1;

    // public     float   pixdim               = new float[8]; // image resolutions info mm or ms
    // stored in FileInfoBase
    // pixdim[0] = number of dimensions
    // pixdim[1] = voxel width
    // pixdim[2] = voxel height
    // pixdim[3] = voxel thickness
    // pixdim[4] = time

    /**
     * Byte offset in the ".img" file at which voxels start. This value can be negative to specify that the absolute
     * value is applied for every image in the file
     */
    private float vox_offset = -1;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * file info storage constructor.
     *
     * @param  name       file name
     * @param  directory  directory
     * @param  format     file format
     */
    public FileInfoMGH(String name, String directory, int format) {
        super(name, directory, format);
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Displays the file information.
     *
     * @param  dlog    dialog box that is written to
     * @param  matrix  transformation matrix
     */
    public void displayAboutInfo(JDialogBase dlog, TransMatrix matrix) {
        JDialogFileInfo dialog = (JDialogFileInfo) dlog;
        int[] extents;
        int i;
        int[] editorChoice = new int[1];
        editorChoice[0] = JDialogEditor.STRING;

        dialog.displayAboutInfo(this); // setup layout in the dialog

        extents = super.getExtents();

        for (i = 0; i < extents.length; i++) {
            dialog.appendPrimaryData("Dimension " + i, Integer.toString(extents[i]));
        }

        dialog.appendPrimaryData("Type", ModelStorageBase.getBufferTypeStr(getDataType()));

        
        dialog.appendPrimaryData("Min", Double.toString(getMin()));
        dialog.appendPrimaryData("Max", Double.toString(getMax()));

        dialog.appendPrimaryData("Orientation", getImageOrientationStr(getImageOrientation()));

        float[] resolutions; // = new float[5];
        resolutions = getResolutions();

        for (i = 0; i < extents.length; i++) {

            if (resolutions[i] > 0.0) {
                String pixelRes = "Pixel resolution " + i;
                dialog.appendPrimaryData(pixelRes,
                                         Float.toString(resolutions[i]));
            } // end of if (resolutions[i] > 0.0)
        } // for (i=0; i < 5; i++)

        
        dialog.appendPrimaryData("Endianess", "Big Endian");

        if (matrix != null) {

            // when using displayAboutInfo(dialog) this doesn't appear
            // calling prg might use an editing panel to adjust this matrix
            dialog.appendPrimaryData("Matrix", matrix.matrixToString(10, 4));
        }
        
        editorChoice[0] = JDialogEditor.ANALYZE_AXIS_ORIENTATION;
        dialog.appendSecondaryData("Axis: x-orientation", getAxisOrientationStr(super.getAxisOrientation(0)),
                                   editorChoice);
        dialog.appendSecondaryData("Axis: y-orientation", getAxisOrientationStr(super.getAxisOrientation(1)),
                                   editorChoice);
        dialog.appendSecondaryData("Axis: z-orientation", getAxisOrientationStr(super.getAxisOrientation(2)),
                                   editorChoice);


        editorChoice[0] = JDialogEditor.FLOAT_STRING;
        dialog.appendSecondaryData("X Origin: ", Float.toString(super.getOrigin(0)), editorChoice);
        dialog.appendSecondaryData("Y Origin: ", Float.toString(super.getOrigin(1)), editorChoice);
        dialog.appendSecondaryData("Z Origin: ", Float.toString(super.getOrigin(2)), editorChoice);

        dialog.appendSecondaryData("Version number", String.valueOf(version));
        
        dialog.appendSecondaryData("DOF", String.valueOf(dof));
        
    }
    
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public void setDOF(int dof) {
        this.dof = dof;
    }

    /**
     * accessor to the aux_file string.
     *
     * @return  String aux_file
     */
    public String getAuxFile() {
        return aux_file;
    }

    /**
     * accessor to the bitpix value.
     *
     * @return  short the bitpix value.
     */
    public short getBitPix() {
        return bitpix;
    }

    /**
     * accessor to cal-max.
     *
     * @return  float cal_max
     */
    public float getCalMax() {
        return cal_max;
    }

    /**
     * accessor to cal-min.
     *
     * @return  float cal_min
     */
    public float getCalMin() {
        return cal_min;
    }

    /**
     * Returns type of x, y, z coordinates.
     *
     * @return  coord_code
     */
    public short getCoordCode() {
        return coord_code;
    }

    /**
     * accessor to the current analyze-image description.
     *
     * @return  String description
     */
    public String getDescription() {
        return descrip;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public int getFileExtents() {
        return extents;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public int getFreqDim() {
        return freq_dim;
    }

    /**
     * Accessor that returns the intent code.
     *
     * @return  intentCode
     */
    public short getIntentCode() {
        return intentCode;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getIntentName() {
        return intentName;
    }

    /**
     * Accessor that returns first statistical parameter.
     *
     * @return  intentP1
     */
    public float getIntentP1() {
        return intentP1;
    }

    /**
     * Accessor that returns second statistical parameter.
     *
     * @return  intentP2
     */
    public float getIntentP2() {
        return intentP2;
    }

    /**
     * Accessor that returns third statistical parameter.
     *
     * @return  intentP3
     */
    public float getIntentP3() {
        return intentP3;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public TransMatrix getMatrix() {
        return this.matrix;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public int getPhaseDim() {
        return phase_dim;
    }

    /**
     * Gets the data additive factor.
     *
     * @return  scl_inter
     */
    public float getSclInter() {
        return scl_inter;
    }

    /**
     * Gets the data scaling multiplicative factor.
     *
     * @return  scl_slope
     */
    public float getSclSlope() {
        return scl_slope;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public int getSizeOfHeader() {
        return sizeof_hdr;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public byte getSliceCode() {
        return sliceCode;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public int getSliceDim() {
        return slice_dim;
    }

    /**
     * provides the sliceDuration value.
     *
     * @return  float sliceDuration
     */
    public float getSliceDuration() {
        return sliceDuration;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public short getSliceEnd() {
        return sliceEnd;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public short getSliceStart() {
        return sliceStart;
    }

    /**
     * accessor to the sourceBitPix value.
     *
     * @return  short the sourceBitPix value.
     */
    public short getSourceBitPix() {
        return sourceBitPix;
    }

    /**
     * accessor to coded datatype value.
     *
     * @return  short datatype
     */
    public short getSourceType() {
        return sourceType;
    }

    /**
     * accessor to the vox offset value.
     *
     * @return  float vox_offset
     */
    public float getVoxOffset() {
        return vox_offset;
    }

    /**
     * supplies auxiliary-file string; permits no more than 24 characters.
     *
     * @param  aux  DOCUMENT ME!
     */
    public void setAuxFile(String aux) {
        aux_file = setString(aux, 24);
    }

    /**
     * sets bitpix; any value other than 1, 8, 16, 32, 64, 128, or 24 gets set to the dissalowed trap value, -1.
     *
     * @param  bp  DOCUMENT ME!
     */
    public void setBitPix(short bp) {

        if ((bp == 1) || (bp == 8) || (bp == 16) || (bp == 32) || (bp == 64) || (bp == 128) || (bp == 24)) {
            bitpix = bp;
        } else {
            bitpix = -1;
        } // a disallowed trap value
    }

    /**
     * sets cal-max. if supplied value is less than cal-min, the cal-min gets reset to the supplied value as well, so
     * that cal-min is still no greater than cal-max.
     *
     * @param  cal  DOCUMENT ME!
     */
    public void setCalMax(float cal) {
        cal_max = cal;

        if (cal_max < cal_min) {
            cal_min = cal_max;
        }
    }

    /**
     * sets cal-min. if supplied value is greater than cal-max, the cal-max gets reset to the supplied value as well, so
     * that cal-max is still no less than cal-min.
     *
     * @param  cal  DOCUMENT ME!
     */
    public void setCalMin(float cal) {
        cal_min = cal;

        if (cal_min > cal_max) {
            cal_max = cal_min;
        }
    }


    /**
     * Sets type of xyz coordinates.
     *
     * @param  coord_code  DOCUMENT ME!
     */
    public void setCoordCode(short coord_code) {
        this.coord_code = coord_code;
    }


    /**
     * allows no more than 80 characters to fill in the analyze-image description.
     *
     * @param  description  DOCUMENT ME!
     */
    public void setDescription(String description) {
        descrip = setString(description, 80);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  ext  DOCUMENT ME!
     */
    public void setFileExtents(int ext) {
        extents = ext;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  freq_dim  DOCUMENT ME!
     */
    public void setFreqDim(int freq_dim) {
        this.freq_dim = freq_dim;
    }

    /**
     * Accessor that sets the stat code.
     *
     * @param  intentCode  DOCUMENT ME!
     */
    public void setIntentCode(short intentCode) {
        this.intentCode = intentCode;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  intentName  DOCUMENT ME!
     */
    public void setIntentName(String intentName) {
        this.intentName = intentName;
    }


    /**
     * Accessor that sets first statistical parameter.
     *
     * @param  intentP1  DOCUMENT ME!
     */
    public void setIntentP1(float intentP1) {
        this.intentP1 = intentP1;
    }

    /**
     * Accessor that sets second statistical parameter.
     *
     * @param  intentP2  DOCUMENT ME!
     */
    public void setIntentP2(float intentP2) {
        this.intentP2 = intentP2;
    }

    /**
     * Accessor that sets third statistical parameter.
     *
     * @param  intentP3  DOCUMENT ME!
     */
    public void setIntentP3(float intentP3) {
        this.intentP3 = intentP3;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  matrix  DOCUMENT ME!
     */
    public void setMatrix(TransMatrix matrix) {
        this.matrix = matrix;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  phase_dim  DOCUMENT ME!
     */
    public void setPhaseDim(int phase_dim) {
        this.phase_dim = phase_dim;
    }

    /**
     * Sets the data additive factor.
     *
     * @param  scl_inter  DOCUMENT ME!
     */
    public void setSclInter(float scl_inter) {
        this.scl_inter = scl_inter;
    }

    /**
     * Sets the data scaling multiplicative factor.
     *
     * @param  scl_slope  DOCUMENT ME!
     */
    public void setSclSlope(float scl_slope) {
        this.scl_slope = scl_slope;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  size  DOCUMENT ME!
     */
    public void setSizeOfHeader(int size) {
        sizeof_hdr = size;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  sliceCode  DOCUMENT ME!
     */
    public void setSliceCode(byte sliceCode) {
        this.sliceCode = sliceCode;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  slice_dim  DOCUMENT ME!
     */
    public void setSliceDim(int slice_dim) {
        this.slice_dim = slice_dim;
    }

    /**
     * sets the sliceDuration variable.
     *
     * @param  sliceDuration  DOCUMENT ME!
     */
    public void setSliceDuration(float sliceDuration) {
        this.sliceDuration = sliceDuration;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  sliceEnd  DOCUMENT ME!
     */
    public void setSliceEnd(short sliceEnd) {
        this.sliceEnd = sliceEnd;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  sliceStart  DOCUMENT ME!
     */
    public void setSliceStart(short sliceStart) {
        this.sliceStart = sliceStart;
    }

    /**
     * sets sourceBitPix; any value other than 1, 8, 16, 32, 64, 128, or 24 gets set to the disallowed trap value, -1.
     *
     * @param  bp  DOCUMENT ME!
     */
    public void setSourceBitPix(short bp) {

        if ((bp == 1) || (bp == 8) || (bp == 16) || (bp == 32) || (bp == 64) || (bp == 128) || (bp == 24)) {
            sourceBitPix = bp;
        } else {
            sourceBitPix = -1;
        } // a disallowed trap value
    }

    /**
     * accessor to supply coded datatype.
     *
     * @param  dtype  DOCUMENT ME!
     */
    // Data type before conversion for scl_slope and scl_offset
    public void setSourceType(short dtype) {
        sourceType = dtype;
    }

    /**
     * sets vox offset value.
     *
     * @param  vox  DOCUMENT ME!
     */
    public void setVoxOffset(float vox) {
        vox_offset = vox;
    }

    /**
     * .
     *
     * <table>
     *   <tr>
     *     <td>ce[0] = table</td>
     *     <td>0 = primary, 1 = secondary, etC</td>
     *   </tr>
     *   <tr>
     *     <td>ce[1] = line of table</td>
     *     <td></td>
     *   </tr>
     *   <tr>
     *     <td>ce[2] = string name</td>
     *     <td>eg, "Type"</td>
     *   </tr>
     *   <tr>
     *     <td>ce[3] = Vector codeValue</td>
     *     <td>eg, "B"</td>
     *   </tr>
     *   <tr>
     *     <td>ce[4] = string value</td>
     *     <td>eg, "Big"</td>
     *   </tr>
     * </table>
     *
     * "ce" comes from ChangeEvent upon which this is based. care to make our own ChangeEvent to store and handle this?
     *
     * @param  ce  DOCUMENT ME!
     */
    public void stateChanged(Vector ce) {
        String tname = (String) ce.elementAt(2); // [t]able [name]
        Vector tcvalue = (Vector) ce.elementAt(3); // [t]able [c]ode [value]
        String tvalue = (String) ce.elementAt(4); // [t]able [value]

        if (tname.equalsIgnoreCase("Description")) {
            setDescription(tvalue);
        } else if (tname.equalsIgnoreCase("voxel offset")) {
            setVoxOffset(Float.parseFloat((String) tcvalue.elementAt(0)));
        } else if (tname.equalsIgnoreCase("cal_min")) {
            setCalMin(Float.parseFloat((String) tcvalue.elementAt(0)));
        } else if (tname.equalsIgnoreCase("cal_max")) {
            setCalMax(Float.parseFloat((String) tcvalue.elementAt(0)));
        } else if (tname.equalsIgnoreCase("Orientation")) {
            super.setImageOrientation(((Integer) tcvalue.elementAt(0)).intValue());
            // setImageOrientation(((Byte) tcvalue.elementAt(0)).byteValue());
        } else if (tname.startsWith("Axis: x-orientation")) {
            super.setAxisOrientation(((Integer) tcvalue.elementAt(0)).intValue(), 0);
        } else if (tname.startsWith("Axis: y-orientation")) {
            super.setAxisOrientation(((Integer) tcvalue.elementAt(0)).intValue(), 1);
        } else if (tname.startsWith("Axis: z-orientation")) {
            super.setAxisOrientation(((Integer) tcvalue.elementAt(0)).intValue(), 2);
        } else if (tname.startsWith("Start Location: x-axis")) {
            super.setOrigin(Float.parseFloat((String) tcvalue.elementAt(0)), 0);

        } else if (tname.startsWith("Start Location: y-axis")) {
            super.setOrigin(Float.parseFloat((String) tcvalue.elementAt(0)), 1);
        } else if (tname.startsWith("Start Location: z-axis")) {
            super.setOrigin(Float.parseFloat((String) tcvalue.elementAt(0)), 2);
        } else if (tname.equalsIgnoreCase("Orientation")) {
            setImageOrientation(((Integer) tcvalue.elementAt(0)).intValue());
            // setOrientation(((Byte)tcvalue.elementAt(0)).byteValue());

        } else {
            Preferences.debug("tname: " + tname + ", not found.");
        }
    }


    /**
     * Propogates the current file info to another FileInfoMGH.
     *
     * <p>It does not copy over the datatypeCode. (though, aside from, "it isn't in the about table", I can't think of a
     * reason why it shouldn't. but it doesn't.) Also, copied over is bitPix, aux_file.</p>
     *
     * @param  fInfo  DOCUMENT ME!
     */
    public void updateFileInfos(FileInfoMGH fInfo) {

        if (this == fInfo) {
            return;
        }

        
        fInfo.setImageOrientation(this.getImageOrientation());
        
    }


    /**
     * verifies string is not larger than len length; strings larger than len, are clipped before being returned.
     *
     * @see     String#substring(int, int)
     *
     * @return  String new substring
     */
    protected String setString(String str, int len) {

        if (str.length() < len) {
            return str;
        } else {
            return str.substring(0, len);
        }
    }
}
