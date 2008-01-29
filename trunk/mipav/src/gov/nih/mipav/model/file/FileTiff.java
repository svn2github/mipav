package gov.nih.mipav.model.file;


import gov.nih.mipav.model.algorithms.AlgorithmTransform;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;
import gov.nih.mipav.view.dialogs.JDialogLoadLeica.*;

import java.io.*;

import java.util.*;
import java.util.zip.*;


/**
 * Tagged Image File Format (TIFF 6.0) reader/ writer. Note that
 * although EchoTech has a tResolution field, there is no provision for 4D in TIFF.
 * Almost all of the FAX decompression code, modified Huffman decompression code, and JPEG decompression code
 * was taken from the free software at the website 
 * http://www.mms-computing.co.uk/uk/co/mmscomputing/imageio/tiff
 * ThunderScan decompression code was obtained by porting C++ code from tif_thunder.c in tif-4.0.0.alpha\libtiff.
 * 16 bit integer data with SGILogCompression and photometric = CIE Log2(L)was obtained by porting LogL16Decode in 
 * tif_luv.c in tif-4.0.0.alpha\libtiff.
 * 32 bit integer data with SGILogCompression and photometric = CIE Log2(L) (u', v') was obtained by porting
 * LogLuvDecode32, LogL16toY, LogLuv32toXYZ, and XYZtoRGB24 in tif_luv.c in tif-4.0.0.alpha\libtiff.
 * 24 bit integer data with SGILog24Compression and photometric = CIE Log2(L) (u', v') was obtained by porting
 * LogLuvDecode24, LogLuv24toXYZ, LogL10toY, uv_decode and XYZtoRGB24 in tif_luv.h and uvcode.h in
 * tif-4.0.0.alpha\libtiff.
 * The current port of the LogLuv color compression is not ideal for the MIPAV system.  off_l16.tif, a grayscale picture
 * of an office with a view out a much brighter window, preserves the wide range of pixels both inside the darker 
 * office and outside the brighter window when converted to short in MIPAV.  However, when 24 bit SGILog24Compression
 * off_luv24 and 32 bit SGILogCompression off_luv32 are converted to ARGB in MIPAV only the darker view inside the
 * office is adequately represented.  The brighter view thru the window is mostly saturated, so a transformation
 * to ARGB_USHORT which preserves the window information is really needed.
 * Note that compression = 7 for new JPEG has been implemented, but compression = 6 for deprecated JPEG has not been
 * implemented.  libtiff does have a 46 page file tif_ojpeg which reads in old JPEG, but even porting this would not
 * be sufficient since the tif_ojpeg.c file interfaces with Release 6B of the independent JPEG library written by
 * the Independent JPEG group.
 * LZW compression code was obtained by porting code from tif_lzw.c in tif-4.0.0.alpha\libtiff except for the
 * final section on the Horizontal Differencing Predictor which comes from the Sun Microsystems file 
 * TIFFLZWDecoder.java.  Originally TIFFLZWDecoder.java was used, but it could not handle old-style LZW code,
 * such as with libtiff library file quad-lzw.tif.
 * Note that the tif reader will not work properly for some files with different tag values for different slices.
 * For example, the libtiff file dscf0013.tif, which has slice 1 with 640 by 480 pixels and 15 rows per strip and slice 2 with 
 * 160 by 120 pixels and 120 rows per strip, will not be read in properly.  The libtiff file text.tif with 4 bits per
 * sample and thunderscan compression in the first slice and 1 bit per sample and no compression in the second
 * slice will not be read in properly.
 *
 * @version  1.0 Feb 29, 2000
 * @author   Matthew J. McAuliffe, Ph.D.
 * @author   William Gandler
 * @see      FileIO
 * @see      FileInfoTiff
 * @see      FileRaw
 */
public class FileTiff extends FileBase {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Tiff Types. */
    public static final int BYTE = 1; // 8 bit unsigned

    /** DOCUMENT ME! */
    public static final int ASCII = 2; // 7 bit ASCII

    /** DOCUMENT ME! */
    public static final int SHORT = 3; // 16 bit unsigned

    /** DOCUMENT ME! */
    public static final int LONG = 4; // 32 bit unsigned ****** 4 bytes !!!!

    /** DOCUMENT ME! */
    public static final int RATIONAL = 5; // 2 longs 1st numerator

    /** 2nd denom. */
    public static final int SBYTE = 6; // 8 bit signed

    /** DOCUMENT ME! */
    public static final int UNDEFINED = 7; // 8 bit undefined

    /** DOCUMENT ME! */
    public static final int SSHORT = 8; // 16 bit signed

    /** DOCUMENT ME! */
    public static final int SLONG = 9; // 32 bit signed

    /** DOCUMENT ME! */
    public static final int SRATIONAL = 10; //

    /** DOCUMENT ME! */
    public static final int FLOAT = 11; // single precision 4 byte IEEE format

    /** DOCUMENT ME! */
    public static final int DOUBLE = 12; // double precision 8 byte IEEE format

    /** Tiff Tags. */
    public static final int NEW_SUBFILE_TYPE = 254;
    
    public static final int SUBFILE_TYPE = 255;

    /** DOCUMENT ME! */
    public static final int IMAGE_WIDTH = 256;

    /** DOCUMENT ME! */
    public static final int IMAGE_LENGTH = 257;

    /** DOCUMENT ME! */
    public static final int BITS_PER_SAMPLE = 258;

    /** Compression: 1 = no compression 2 = modified huffman 3 = CCITT-T4(FAX3) 4 = CCITT-T6(FAX4) 5 = LZW 6 = old JPEG
     *               7 = JPEG  8 or 32946 = zlib 32773 = packbits 32809 = thunderscan RLE. 
     *               34676 = SGI Log Luminance RLE 34677 = SGI Log 24-bit packed */
    public static final int COMPRESSION = 259;

    /** DOCUMENT ME! */
    public static final int PHOTO_INTERP = 262;
    
    public static final int THRESHHOLDING = 263;
    
    public static final int FILL_ORDER = 266;
    
    public static final int DOCUMENT_NAME = 269;

    /** DOCUMENT ME! */
    public static final int IMAGE_DESCRIPTION = 270;
    
    public static final int MAKE = 271;
    
    public static final int MODEL = 272;

    /** DOCUMENT ME! */
    public static final int STRIP_OFFSETS = 273;

    /** DOCUMENT ME! */
    public static final int ORIENTATION = 274;

    /** DOCUMENT ME! */
    public static final int SAMPLES_PER_PIXEL = 277;

    /** DOCUMENT ME! */
    public static final int ROWS_PER_STRIP = 278;

    /** DOCUMENT ME! */
    public static final int STRIP_BYTE_COUNTS = 279;

    /** DOCUMENT ME! */
    public static final int MIN_SAMPLE_VALUE = 280;

    /** DOCUMENT ME! */
    public static final int MAX_SAMPLE_VALUE = 281;

    /** DOCUMENT ME! */
    public static final int XRESOLUTION = 282;

    /** DOCUMENT ME! */
    public static final int YRESOLUTION = 283;

    /** DOCUMENT ME! */
    public static final int PLANAR_CONFIG = 284;
    
    public static final int XPOSITION = 286;
    
    public static final int YPOSITION = 287;
    
    public static final int T4OPTIONS = 292;
    
    public static final int T6OPTIONS = 293;

    /** DOCUMENT ME! */
    public static final int RESOLUTION_UNIT = 296;
    
    public static final int PAGE_NUMBER = 297;

    /** DOCUMENT ME! */
    public static final int SOFTWARE = 305;

    /** DOCUMENT ME! */
    public static final int DATE_TIME = 306;

    /** DOCUMENT ME! */
    public static final int ARTIST = 315;

    /** DOCUMENT ME! */
    public static final int HOST_COMPUTER = 316;

    /** DOCUMENT ME! */
    public static final int PREDICTOR = 317;

    /** DOCUMENT ME! */
    public static final int COLOR_MAP = 320;
    
    public static final int HALFTONE_HINTS = 321;

    /** DOCUMENT ME! */
    public static final int TILE_WIDTH = 322;

    /** DOCUMENT ME! */
    public static final int TILE_LENGTH = 323;

    /** DOCUMENT ME! */
    public static final int TILE_OFFSETS = 324;

    /** DOCUMENT ME! */
    public static final int TILE_BYTE_COUNTS = 325;
    
    public static final int BAD_FAX_LINES = 326;
    
    public static final int CLEAN_FAX_DATA = 327;
    
    public static final int CONSECUTIVE_BAD_FAX_LINES = 328;
    
    public static final int INK_SET = 332;
    
    public static final int INK_NAMES = 333;
    
    public static final int EXTRA_SAMPLES = 338;

    /** DOCUMENT ME! */
    public static final int SAMPLE_FORMAT = 339;
    
    // This field is the only auxiliary TIFF field added for the new-style 
    // JPEG compression(Compression = 7).  It is optional.
    // The purpose of the JPEG_TABLES is to predefine JPEG quantization
    // and/or Huffman tables for subsequent use by JPEG image segments.
    public static final int JPEG_TABLES = 347;
    
    public static final int YCBCR_COEFFICIENTS = 529;
    
    public static final int YCBCR_SUBSAMPLING = 530;
    
    public static final int YCBCR_POSITIONING = 531;
    
    public static final int REFERENCE_BLACK_WHITE = 532;
    
    public static final int COPYRIGHT = 33432;
    
    // Exposure time, given in seconds.
    public static final int EXIFTAG_EXPOSURE_TIME = 33434;
    
    // The F number.
    public static final int EXIFTAG_FNUMBER = 33437;
    
    // Pointer to EXIF private directory
    public static final int EXIFIFD = 34665;
    
    // The class of program used by the camera to set exposure when the picture is taken.
    public static final int EXIFTAG_EXPOSURE_PROGRAM = 34850;
    
    // Indicates the ISO Speed and ISO Latitude of the camera or input device as specified in ISO 12232.
    public static final int EXIFTAG_ISO_SPEED_RATINGS = 34855;
    
    // The version of EXIF standard supported.  Nonexistence of this field is taken to mean nonconformance
    // to the standard.
    public static final int EXIFTAG_EXIF_VERSION = 36864;
    
    // The date and time when the original image data was generated.
    // The format is "YYYY:MM:DD HH:MM:SS" with the time shown in 24-hour format
    public static final int EXIFTAG_DATE_TIME_ORIGINAL = 36867;
    
    // The date and time when the image was stored as digital data.
    //  The format is "YYYY:MM:DD HH:MM:SS" with the time shown in 24-hour format
    public static final int EXIFTAG_DATE_TIME_DIGITIZED = 36868;
    
    // Shutter speed.  The unit is the APEX(Additive System of Photographic Exposure)setting.
    public static final int EXIFTAG_SHUTTER_SPEED_VALUE = 37377;
    
    // The lens aperture.  The unit is the APEX value.
    public static final int EXIFTAG_APERTURE_VALUE = 37378;
    
    // The value of birghtness.  The unit is the APEX value.  Ordinarily it is given in the range
    // of -99.99 to 99.99.  Note that if the numerator of the recorded value is FFFFFFFF.H, unknown
    // shall be recorded.
    public static final int EXIFTAG_BRIGHTNESS_VALUE = 37379;
    
    // The exposure bias.  The unit is the apex value.  Ordinarily it is given
    // in the range of -99.99 to 99.99
    public static final int EXIFTAG_EXPOSURE_BIAS_VALUE = 37380;
    
    // The smallest F number of the lens.  The unit is the APEX value.  Ordinarily it is given
    // in the range of 00.00 to 99.99, but it is not limited to this range
    public static final int EXIFTAG_MAX_APERTURE_VALUE = 37381;
    
    // The metering mode
    public static final int EXIFTAG_METERING_MODE = 37383;
    
    // The kind of light source.
    public static final int EXIFTAG_LIGHT_SOURCE = 37384;
    
    // This tag indicates the status of flash when the image was shot.  Bit 0 indicates the
    // flash firing status, bits 1 and 2 indicate the flash return status, bits 3 and 4
    // indicate the flash mode, bit 5 indicates whether the flash function is present,
    // and bit 6 indicates "red eye" mode.
    public static final int EXIFTAG_FLASH = 37385;
    
    // The actual focal length of the lens in millimeters
    public static final int EXIFTAG_FOCAL_LENGTH = 37386;
    
    // A tag for manufacturers of Exif writers to record any desired information.
    public static final int EXIFTAG_MAKER_NOTE = 37500;
    
    // A tag for Exif users to write keywords or comments on the image besides those in
    // imageDescription, and without the character code limitations of the imageDescription tag.
    public static final int EXIFTAG_USER_COMMENT = 37510;
    
    /** Sample value to Nits, where Nits is the photometric unit for luminance,
     *  also written candelas/meter**2.
     */
    public static final int STONITS = 37439;
    
    // The Flashpix format version supported by a FPXR file.
    public static final int EXIFTAG_FLASHPIX_VERSION = 40960;
    
    // Normally sRGB(=1) is used to define the color space based on the PC monitor conditions
    // and environment.  If a colorSpace other than sRGB is used, uncalibrated(0xFFFF) is set.
    // Image data recorded as uncalibrated can be treated as sRGB when it is converted to Flashpix.
    public static final int EXIFTAG_COLOR_SPACE = 40961;
    
    // Indicates the number of pixels in the image width (X) direction per FocalPlaneResolutionUnit on
    // the camera focal plane.
    public static final int EXIFTAG_FOCAL_PLANE_X_RESOLUTION = 41486;
    
    // Indicates the number of pixels in the image height (Y) direction per FocalPlaneResolutionUnit on
    // the camera focal plane.
    public static final int EXIFTAG_FOCAL_PLANE_Y_RESOLUTION = 41487;
    
    // Indicates the unit for measuring focalPlaneXResolution and focalPlaneYResolution.
    // This value is the same as ResolutionUnit.
    public static final int EXIFTAG_FOCAL_PLANE_RESOLUTION_UNIT = 41488;
    
    // Indicates the image sensor type on the camera or input device.
    public static final int EXIFTAG_SENSING_METHOD = 41495;
    
    // Indicates the image source.  If a DSC recorded the image, the tag value of this tag
    // is set to 3.
    public static final int EXIFTAG_FILE_SOURCE = 41728;
    
    // Indicates the type of a scene.  If a DSC recorded the image, this tatg shall always be
    // set to 1, indicating that the image was directly photographed.
    public static final int EXIFTAG_SCENE_TYPE = 41729;
    
    // This tag indicates the use of special processing on image data, such as rendering geared
    // to output.  When special processing is performed, the reader is expected to disable or
    // minimize any further processing.
    public static final int EXIFTAG_CUSTOM_RENDERED = 41985;
    
    // This tag indicates the exposure mode set when the image was shot.  In auto-bracketing
    // node the camera shoots a series of frames of the same image at different exposure settings.
    public static final int EXIFTAG_EXPOSURE_MODE = 41986;
    
    // This tag indicates the white balance mode when the image was shot.
    public static final int EXIFTAG_WHITE_BALANCE = 41987;
    
    // This tag indicates the digital zoom ratio when the image was shot.  If the numerator
    // of the recorded value is zero, this indicates that digital zoom was not used.
    public static final int EXIFTAG_DIGITAL_ZOOM_RATIO = 41988;
    
    // This tag indicates the type of scene that was shot.  It can also be used to record
    // the mode in which the image was shot.
    public static final int EXIFTAG_SCENE_CAPTURE_TYPE = 41990;
    
    // This tag indicates the degree of overall image gain adjustment
    public static final int EXIFTAG_GAIN_CONTROL = 41991;
    
    // This tag indicates the direction of contrast processing applied by the camera
    // when image was shot.
    public static final int EXIFTAG_CONTRAST = 41992;
    
    // This tag indicates the direction of saturation processing applied by the camera
    // when the image was shot.
    public static final int EXIFTAG_SATURATION = 41993;
    
    // This tag indicates the direction of sharpness processing applied by the camera
    // when the image was shot.
    public static final int EXIFTAG_SHARPNESS = 41994;
    
    // Used by the GDAL library, holds an XML list of name=value 'metadata' values
    // about the image as a whole, and about specific samples.
    public static final int GDAL_METADATA = 42112;

    /** EchoTech Tiff TAGS. */
    public static final int ZRESOLUTION = 65000;

    /** DOCUMENT ME! */
    public static final int TRESOLUTION = 65001;

    /** DOCUMENT ME! */
    public static final int MAX_IFD_LENGTH = 10000;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** DOCUMENT ME! */
    private byte[] artist;

    /** DOCUMENT ME! */
    private byte[] byteBuffer = null;

    /** DOCUMENT ME! */
    private boolean chunky = true;

    /** DOCUMENT ME! */
    private Vector[] dataOffsets = new Vector[4000];

    /** DOCUMENT ME! */
    private byte[] dateTime;

    /** DOCUMENT ME! */
    private double[] doubleBuffer;

    /** DOCUMENT ME! */
    private boolean endianess;

    /** The file... */
    private File file;

    /** File directory. */
    private String fileDir;

    /** File Info. */
    private FileInfoTiff fileInfo;

    /** File name. */
    private String fileName;

    /** DOCUMENT ME! */
    private FilePackBit filePB;

    /** DOCUMENT ME! */
    private FileRawChunk fileRW;

    /** DOCUMENT ME! */
    private boolean foundTag43314; // This tag is only present in the Scion variation of

    /** DOCUMENT ME! */
    private byte[] hostComputer;

    /** DOCUMENT ME! */
    private int[] IFDoffsets = new int[4096];

    /** DOCUMENT ME! */
    private ModelImage image;

    /** DOCUMENT ME! */
    private byte[] imageDescription;
    
    private byte[] make;
    
    private byte[] model;

    /** DOCUMENT ME! */
    private int imageSlice = 0;

    /** DOCUMENT ME! */
    private float[] imgBuffer = null;

    /** DOCUMENT ME! */
    private float[] imgResols;
    
    private float xPosition;
    
    private float yPosition;

    /** DOCUMENT ME! */
    private ModelLUT LUT = null;

    /** DOCUMENT ME! */
    private int LUTOffset;
    
    private short lightHalftone;
    
    private short darkHalftone;
    
    private short extraSamples[];

    /** DOCUMENT ME! */
    private boolean lzwCompression = false; // true if the read data file has LZW compression

    /** TIFF files. */
    private TIFFLZWDecoder lzwDecoder = null; // for decoding LZW compressed images
    
    private boolean zlibCompression = false; // true if the read data file has LZW compression
    
    private Inflater zlibDecompresser = null;

    /** DOCUMENT ME! */
    private short nDirEntries;

    /** Pack Bit: true if the read data file has pack bit compression. */
    private boolean packBit = false;

    /** DOCUMENT ME! */
    private int predictor = 1; // prediction scheme used (needed to create LZW decoder)

    /** DOCUMENT ME! */
    private int rowsPerStrip;
    
    private boolean haveRowsPerStrip = false;

    /** DOCUMENT ME! */
    private int samplesPerPixel = 1;

    /** DOCUMENT ME! */
    private double[] sliceBufferDouble = null;

    /** DOCUMENT ME! */
    private float[] sliceBufferFloat = null;

    /** DOCUMENT ME! */
    private byte[] software;
    
    private byte[] documentName;
    
    private byte[] inkNames;
    
    private byte[] copyright;

    /** DOCUMENT ME! */
    private String str;

    /** DOCUMENT ME! */
    private int[] tileByteCounts = null;

    /** DOCUMENT ME! */
    private int tileByteNumber;

    /** DOCUMENT ME! */
    private int tileLength;

    /** DOCUMENT ME! */
    private int tileMaxByteCount;

    /** DOCUMENT ME! */
    private int tileOffsetNumber;

    /** DOCUMENT ME! */
    private int[] tileOffsets = null;

    /** DOCUMENT ME! */
    private int tilesAcross;

    /** DOCUMENT ME! */
    private int tilesDown;

    /** DOCUMENT ME! */
    private int tilesPerImage;

    /** DOCUMENT ME! */
    private int tilesPerSlice;

    /** DOCUMENT ME! */
    private int[] tileTemp;

    /** DOCUMENT ME! */
    private int tileWidth;
    
    private boolean haveTileWidth = false;
    
    private boolean haveTileLength = false;
    
    private boolean haveTileOffsets = false;
    
    private int[] bitsPerSample = null;
    
    private boolean haveChangedPhotometricTo1 = false;

    /** DOCUMENT ME! */
    private double tRes = 1.0;

    /** DOCUMENT ME! */
    private int xDim = 0;

    /** DOCUMENT ME! */
    private int yDim = 0;
    
    private long fileLength;

    /** DOCUMENT ME! */
    private double zRes = 1.0;
    
    // 1 = Pixels are arranged within a byte such that pixels with lower column values are
    //     stored in the higher-order bits of the byte.
    // 2 = Pixels are arranged within a byte such that pixels with lower column values are
    //     stored in the lower-order bits of the byte.
    private int fillOrder = 1;
    
    private boolean isCIELAB = false;
    
    private boolean isYCbCr = false;
    
    // True when photometric == 5 for separated and inskSet = 1 for CMYK
    private boolean isCMYK = false;
    
    // YCbCr Coefficients
    // Defaults are from CCIR Recommendation 601-1
    private float LumaRed = 0.299f;
    private float LumaGreen = 0.587f;
    private float LumaBlue = 0.114f;
    
    // 1 = chroma dimension length same as dimension length of associated luma image
    // 2 = chroma dimension length half the dimension length of the associated luma image
    // 4 = chroma dimension length 1/4 the dimension length of the assoicated luma image
    // Default is 2, 2
    private int YCbCrSubsampleHoriz = 2;
    private int YCbCrSubsampleVert = 2;
    
    // 1 = centered for xOffset[0, 0] = YCbCrSubsampleHoriz/2 - 0.5, yOffset[0, 0] = YCbCrSubsampleVert/2 - 0.5
    // 2 = cosited for xOffset[0, 0] = 0, yOffset[0,0] = 0
    // Default = 1
    private int YCbCrPositioning = 1;
    
    private int YReferenceBlack = 0;
    private int YReferenceWhite = 255;
    private int CbReferenceBlack = 128;
    private int CbReferenceWhite = 255;
    private int CrReferenceBlack = 128;
    private int CrReferenceWhite = 255;
    
    // Used with a separated (photometric = 5) image
    // 1 = CMYK.  The order of components is cyan, magenta, yellow, black
    // 2 = not CMYK.  See the INK_NAMES field for a description of the inks
    private int inkSet = 1;
    
    private boolean isBW2 = false;
    private boolean isBW4 = false;
    private boolean isBW6 = false;
    private boolean isBW10 = false;
    private boolean isBW12 = false;
    private boolean isBW14 = false;
    private boolean isBW24 = false;
    
    private boolean isRGB2 = false;
    private boolean isRGB4 = false;
    private boolean isRGB10 = false;
    private boolean isRGB12 = false;
    private boolean isRGB14 = false;
    private boolean isRGB24UINTtoFLOAT = false;
    private boolean isRGB32UINTtoFLOAT = false;
    
    // CCITT FAX3 or T4 encoding
    private boolean fax3Compression = false;
    // CCITT FAX4 or T6 encoding
    private boolean fax4Compression = false;
    
    private boolean modHuffmanCompression = false;
    
    // Options with group 3, also known as T4, fax coding
    // Default is for basic 1-dimensional coding
    private boolean group3_2D_Coding = false;
    private boolean group3Uncompressed = false;
    private boolean group3Fillbits = false;
    
    // Options with group 4, also know as T6, fax coding
    // Default is for uncompressed mode not allowed
    private boolean group4Uncompressed = false;
    
    private boolean haveMultiSpectraImage = false;
    
    private boolean jpegCompression = false;
    
    private byte jpegTables[] = null;
    
    private JPEGInputStream tableStream = null;
    
    private boolean ThunderScanCompression = false;
    
    private boolean isLogL = false; // photometric CIE Log2(L)
    
    private boolean isLogLuv = false; // photometric CIE Log2(L) (u',
    
    private boolean SGILogCompression = false; // SGI Log Luminance RLE
    
    private boolean SGILog24Compression = false; // SGI Log 24-bit packed
    
    private float exposureTime; 
    private float fNumber;
    private short exposureProgram;
    private short isoSpeedRatings[];
    private byte[] exifVersion;
    private byte[] dateTimeOriginal;
    private byte[] dateTimeDigitized;
    private float exposureBias;
    private float maxAperture;
    private short meteringMode;
    private short lightSource;
    private short flash;
    private float focalLength;
    private byte[] makerNote;
    private byte[] characterCode;
    private byte[] userComment;
    private byte[] flashPixVersion;
    private int colorSpace;
    private int fileSource;
    private int sceneType;
    private short customRendered;
    private short exposureMode;
    private short whiteBalance;
    private float digitalZoomRatio;
    private short sceneCaptureType;
    private short gainControl;
    private short contrast;
    private short saturation;
    private short sharpness;
    private float shutterSpeed;
    private float aperture;
    private float brightness;
    private float focalPlaneXResolution;
    private float focalPlaneYResolution;
    private short focalPlaneResolutionUnit;
    private short sensingMethod;
    
    private byte[] gdalMetadata;
    
    private int badFaxLines = 0;
    private short cleanFaxData = 0;
    private int consecutiveBadFaxLines = 0;
    
    private short threshholding = 1;
    
    // If true, not old version of LZW
    private boolean newLZW = true;
    
    private boolean haveLZWInit = false;
    
    private int next[];
    private short length[];  // string length, including this token
    private byte value[];    // data value
    private byte firstChar[]; // first token of string
    
    private short nBits; // # of bits/code
    private short maxCode; // maximum code for nBits
    private short free_ent; // next entry in hash table
    private long nextData; // next bits of i/o
    private long nextBits; // # of valid bits in nextData
    private int rw_mode; // preserve rw_mode from init
    
    private long nBitsMask; // LZWBaseState nBits 1 bits, right adjusted
    private long restart; // restart count
    private int bitsLeft; // available bits in raw data
    private int currentRecognizedCode; // current recognized code
    private int previousRecognizedCode; // previously recognized code
    private int nextFreeEntry; // next free entry in dec_codetab
    private int maxAvailableEntry; // max available entry

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Tiff reader/writer constructor.
     *
     * @param      fileName  file name
     * @param      fileDir   file directory
     *
     * @exception  IOException  if there is an error making the file
     */
    public FileTiff(String fileName, String fileDir) throws IOException {
        this.fileName = fileName;
        this.fileDir = fileDir;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Prepares this class for cleanup. Calls the <code>finalize</code> method for existing elements, closes any open
     * files and sets other elements to <code>null</code>.
     */
    public void finalize() {

        imgBuffer = null;

        sliceBufferFloat = null;

        sliceBufferDouble = null;
        
        if (zlibDecompresser != null) {
            zlibDecompresser.end();
        }

        try {
            super.finalize();
        } catch (Throwable er) { }
    }

    /**
     * Accessor that returns the file info.
     *
     * @return  FileInfoBase containing the file info
     */
    public FileInfoBase getFileInfo() {
        return fileInfo;
    }

    /**
     * Accessor that returns the image buffer.
     *
     * @return  buffer of image.
     */
    public float[] getImageBuffer() {
        return imgBuffer;
    }

    /**
     * Rreturns LUT if defined.
     *
     * @return  the LUT if defined else it is null
     */
    public ModelLUT getModelLUT() {
        return LUT;
    }

    /**
     * Reads the Tiff header which indicates endianess, the TIFF magic number, and the offset in bytes of the first IFD.
     * It then reads all the IFDs. This method then opens a Model of an image and imports the the images one slice at a
     * time. Image slices are separated by an IFD.
     *
     * @param      multiFile  <code>true</code> if a set of files each containing a separate 2D image is present <code>
     *                        false</code> if one file with either a 2D image or a stack of 2D images
     * @param      one        <code>true</code> if only want to read in one image of the 3D set
     *
     * @return     returns the image
     *
     * @exception  IOException  if there is an error reading the file
     */
    public ModelImage readImage(boolean multiFile, boolean one) throws IOException {
        int[] imgExtents;
        int totalSize;

        try {
            file = new File(fileDir + fileName);
            raFile = new RandomAccessFile(file, "r");
            
            fileLength = raFile.length();

            short byteOrder = raFile.readShort();

            if (byteOrder == 0x4949) {
                endianess = FileBase.LITTLE_ENDIAN;
            } else if (byteOrder == 0x4d4d) {
                endianess = FileBase.BIG_ENDIAN;
            } else {
                raFile.close();
                throw new IOException("TIFF Read Header: Error - first 2 bytes are an illegal " + byteOrder);
            }

            int magicTIFFNumber = getUnsignedShort(endianess);

            if (magicTIFFNumber != 42) {
                raFile.close();
                throw new IOException("Tiff Read Header: Error - Invalid Magic number = " + magicTIFFNumber);
            }

            long saveLoc = raFile.getFilePointer();
            fileInfo = new FileInfoTiff(fileName, fileDir, FileUtility.TIFF); // dummy fileInfo
            fileInfo.setEndianess(endianess);
            imageSlice = 0;
            IFDoffsets[imageSlice] = getInt(endianess);

            boolean moreIFDs = true;
            imgResols = new float[5];
            Preferences.debug("\n ************** FileTiff.openIFD: Opening = " + fileName + "\n",
                              Preferences.DEBUG_FILEIO);
            tileOffsetNumber = 0;
            tileByteNumber = 0;
            foundTag43314 = false;

            while ((moreIFDs) && (!foundTag43314)) { // Find number of images!!
                raFile.seek(IFDoffsets[imageSlice]);
                moreIFDs = openIFD(fileInfo);
            }

            if (foundTag43314) {
                raFile.seek(516);
                imageSlice = getUnsignedShort(BIG_ENDIAN);
            }

            Preferences.debug("Just past init IFD read", Preferences.DEBUG_FILEIO);

            if (haveTileWidth && (!lzwCompression) && (!zlibCompression) && (!fax3Compression) && (!fax4Compression) &&
                                 (!modHuffmanCompression) && (!jpegCompression) && (!ThunderScanCompression) &&
                                 (!SGILogCompression) && (!SGILog24Compression) && haveTileOffsets) {
                if (chunky) {
                   tilesPerSlice = tilesAcross * tilesDown;
                }
                else {
                    tilesPerSlice = samplesPerPixel * tilesAcross * tilesDown;
                }
                imageSlice = tilesPerImage / tilesPerSlice;
                // System.err.println("DoTile: tilesPerSlice: " + tilesPerSlice + " imageSlice: " + imageSlice);
            } // if (haveTileWidth && (!lzwCompression) && (!zlibCompression)&& (!fax3Compression) && (!fax4Compression) 
            else if (haveTileWidth || lzwCompression || zlibCompression || fax3Compression || fax4Compression ||
                     modHuffmanCompression || jpegCompression || ThunderScanCompression || SGILogCompression ||
                     SGILog24Compression) {

                // set the tile width to the xDim for use in LZW Decoder or zlib deflater or fax decompression
                if (!haveTileWidth) {
                    tileWidth = xDim;
                }
                // Code to put all the rows in 1 strip often uses rowsPerStrip = 2**32 -1
                // which shows up as -1.
                if (!haveTileLength && haveRowsPerStrip && (rowsPerStrip != -1)) {
                    tileLength = rowsPerStrip;
                }
                else if (!haveTileLength) {
                    tileLength = yDim;
                }
                
                if ((tileOffsets == null) || (tileByteCounts == null)) {
                    totalSize = 0;
                    for (int i = 0; i < imageSlice; i++) {
                        totalSize += dataOffsets[i].size();
                    }
                    tileOffsets = new int[totalSize];
                    tileByteCounts = new int[totalSize];
                    tileMaxByteCount = 0;
    
                    for (int i = 0, k = 0; i < imageSlice; i++) {
                        for (int j = 0; j < dataOffsets[i].size(); j++) {
                            tileOffsets[k] = (int) ((Index) (dataOffsets[i].elementAt(j))).index;
                            tileByteCounts[k] = (int) ((Index) (dataOffsets[i].elementAt(j))).byteCount;
        
                            if (tileByteCounts[k] > tileMaxByteCount) {
                                tileMaxByteCount = tileByteCounts[k];
                            }
                            k++;
                        }
                    }
                }
                else {
                    tileMaxByteCount = 0;
                    for (int k = 0; k < tileByteCounts.length; k++) {
                        if (tileByteCounts[k] > tileMaxByteCount) {
                            tileMaxByteCount = tileByteCounts[k];
                        }
                    }
                }

                // System.err.println("Number of tile offsets: " + tileOffsets.length);
                tilesAcross = (xDim + tileWidth - 1) / tileWidth;
                tilesDown = (yDim + tileLength - 1) / tileLength;

                // tileSize = tileWidth * tileHeight * numBands;
                if (chunky) {
                    tilesPerSlice = tilesAcross * tilesDown;
                 }
                 else {
                     tilesPerSlice = samplesPerPixel * tilesAcross * tilesDown;
                 }
                //System.err.println("Tiles Across: " + tilesAcross + " tiles down: " + tilesDown);
                // System.err.println("TileMaxByteCount: " + tileMaxByteCount);
            }

            // System.err.println("Tile width: " + tileWidth + " samples per pixel: " + samplesPerPixel);
            //System.err.println("Image slice: " + imageSlice);
            imgResols[0] = imgResols[1] = imgResols[2] = imgResols[3] = imgResols[4] = (float) 1.0;
            Preferences.debug("imageSlice = " + imageSlice, Preferences.DEBUG_FILEIO);

            if (haveMultiSpectraImage && (imageSlice > 1)) {
                imgExtents = new int[4];
                imgExtents[0] = xDim;
                imgExtents[1] = yDim;
                imgExtents[2] = imageSlice;
                imgExtents[3] = bitsPerSample.length;
            } // if (haveMultiSpectraImage)
            else if (haveMultiSpectraImage) {
                imgExtents = new int[3];
                imgExtents[0] = xDim;
                imgExtents[1] = yDim;
                imgExtents[2] = bitsPerSample.length;
            }
            else if (imageSlice > 1) {
                imgExtents = new int[3];
                imgExtents[0] = xDim;
                imgExtents[1] = yDim;
                imgExtents[2] = imageSlice;
            } else {
                imgExtents = new int[2];
                imgExtents[0] = xDim;
                imgExtents[1] = yDim;
            }

            fileInfo.setExtents(imgExtents);

            if (multiFile == false) {

                if (one) {
                    image = new ModelImage(fileInfo.getDataType(), new int[] { imgExtents[0], imgExtents[1] },
                                           fileInfo.getFileName());
                } else {
                    image = new ModelImage(fileInfo.getDataType(), imgExtents, fileInfo.getFileName());
                }
            }

            imageSlice = 0;
            raFile.seek(saveLoc);
            moreIFDs = true;

            int i = 0;
            tileOffsetNumber = 0;
            tileByteNumber = 0;

            if (!foundTag43314) {
                while (moreIFDs){
                    fileInfo = new FileInfoTiff(fileName, fileDir, FileUtility.TIFF);
                    fileInfo.setExtents(imgExtents);
                    raFile.seek(IFDoffsets[imageSlice]);
                    moreIFDs = openIFD(fileInfo);

                    // Set the resolutions
                    fileInfo.setResolutions(imgResols);

                    if ((multiFile == false) && (one == false)) {
                        image.setFileInfo(fileInfo, i);
                    }

                    i++;
                } // while (moreIFDs)
            } // if (!foundTag43314)
            else { // foundTag43314
                fileInfo = new FileInfoTiff(fileName, fileDir, FileUtility.TIFF);
                fileInfo.setExtents(imgExtents);
                fileInfo.setDataType(image.getType());

                if ((imgExtents.length > 2) && !one) {
                    imageSlice = imgExtents[2];
                } else {
                    imageSlice = 1;
                }

                for (i = 0; i < imageSlice; i++) {

                    if (multiFile == false) {
                        image.setFileInfo(fileInfo, i);
                        dataOffsets[i] = new Vector();
                        dataOffsets[i].addElement(new Index(768 + (i * xDim * yDim)));
                        ((Index) (dataOffsets[i].elementAt(0))).byteCount = xDim * yDim;
                    }
                }
            } // else foundTag43314

            if (haveTileWidth && (!lzwCompression) && (!zlibCompression) && (!fax3Compression) && (!fax4Compression) && 
               (!modHuffmanCompression) && (!jpegCompression) && (!ThunderScanCompression) && 
                (!SGILogCompression) && (!SGILog24Compression) && haveTileOffsets) {
                imageSlice = tilesPerImage / tilesPerSlice;
            }

            if (one) {
                imageSlice = 1;
            }

            if (lzwCompression) {
                // lzwDecoder = new TIFFLZWDecoder(tileWidth, predictor, samplesPerPixel);
                // System.err.println("Created LZW Decoder");
            }
            else if (zlibCompression) {
                zlibDecompresser = new Inflater();
            }
            else if (fax3Compression || fax4Compression) {
                fax34Init();   
            }

            int bufferSize;
            int sliceSize = imgExtents[0] * imgExtents[1];

            if (imgExtents.length == 3) {
                bufferSize = imgExtents[0] * imgExtents[1] * imgExtents[2];
            } else {
                bufferSize = imgExtents[0] * imgExtents[1];
            }

            if (ModelImage.isColorImage(fileInfo.getDataType())) {
                bufferSize *= 4;
                sliceSize *= 4;
            }
            
            if (haveMultiSpectraImage) {
                sliceSize *= bitsPerSample.length;
            }

            // long secondTime = System.currentTimeMillis();
            // System.err.println("Time elapsed reading IFDs: " + ((secondTime - firstTime) / 1000));
            // System.err.println("pbar visible: " + pBarVisible);
            if (fileInfo.getDataType() == ModelStorageBase.DOUBLE) {

                if (doubleBuffer == null) {
                    doubleBuffer = new double[bufferSize];
                }

                if (sliceBufferDouble == null) {
                    sliceBufferDouble = new double[sliceSize];
                }

                for (i = 0; i < imageSlice; i++) {

                    try {

                        if (one) {

                            if (imgExtents.length > 2) {
                                i = imgExtents[2] / 2;
                            }
                        }

                        readDoubleBuffer(i, sliceBufferDouble);

                        if (one) {
                            i = 0;
                        }
                    } catch (IOException error) {
                        throw new IOException("FileTiff: Read: " + error);
                    }

                    System.arraycopy(sliceBufferDouble, 0, doubleBuffer, i * sliceSize, sliceSize);

                    if (multiFile == false) {
                        image.importData(i * sliceSize, sliceBufferDouble, false);
                    }
                }
            } else { // not ModelStorageBase.DOUBLE

                // System.err.println("not buffer of doubles, type: " + fileInfo.getDataType());
                if (imgBuffer == null) {
                    imgBuffer = new float[bufferSize];
                }

                if (sliceBufferFloat == null) {
                    sliceBufferFloat = new float[sliceSize];
                }


                for (i = 0; i < imageSlice; i++) {

                    try {

                        if (haveTileWidth || lzwCompression || zlibCompression || fax3Compression || fax4Compression ||
                            modHuffmanCompression || jpegCompression || ThunderScanCompression || SGILogCompression ||
                            SGILog24Compression) {
                            readTileBuffer(i, sliceBufferFloat);
                        } else {

                            if (one) {

                                if (imgExtents.length > 2) {
                                    i = imgExtents[2] / 2;
                                }
                            }

                            readBuffer(i, sliceBufferFloat); // Slice a time;

                            if (one) {
                                i = 0;
                            }
                        }
                        
                        if (isCIELAB) {
                            CIELABtoRGB(sliceBufferFloat);
                        }
                        
                    } catch (IOException error) {
                        throw new IOException("FileTiff: read: " + error);
                    }

                    System.arraycopy(sliceBufferFloat, 0, imgBuffer, i * sliceSize, sliceSize);

                    if (multiFile == false) {
                        image.importData(i * sliceSize, sliceBufferFloat, false);
                        // System.err.println("Imported image data");
                    }
                }
            } // else not ModelStorageBase.Double
            
            if (haveChangedPhotometricTo1) {
                // Changed to black is zero
                fileInfo.setPhotometric((short)1);
            }

            fileInfo.setExtents(imgExtents);
            raFile.close();

        } catch (OutOfMemoryError error) {

            if (image != null) {
                image.disposeLocal();
                image = null;
            }

            byteBuffer = null;
            System.gc();
            throw error;
        }

        return image;
    }
    
    // Convert CIELAB to XYZ and convert XYZ to RGB
    // R position has CIE_L*, G position has CIE-a*, B position has CIE-b*
    // For 8 bits:
    // The L* range goes from 0 to 100 as the 8 bit values go from 0 to 255
    // The a* and b* ranges are signed 8 bit values having the range -128 to 127.
    // so in an ARGB read in -1 goes to 255, -2 goes to 254,..., -128 goes to 128
    // For 16 bits:
    //  The L* range goes from 0 to 100 as the 8 bit values go from 0 to 65535
    // The a* and b* ranges are signed 8 bit values having the range -32768 to 32767.
    // so in an ARGB read in -1 goes to 65535, -2 goes to 65534,..., -32768 goes to 32768
    // CIELAB to XYZ comes from Lab Color space in Wilkepedia
    // XYZ to RGB comes from EasyRGB http://www.easyrgb.com
    private void CIELABtoRGB(float buffer[]) {
        double varX; 
        double varY;
        double varZ;
        double delta = 6.0/29.0;
        double offset = 16.0/116.0;
        double threeDeltaSquared = 3.0 * delta * delta;
        // refX, refY, and refZ are the trisimulus values of an appropriate reference white
        // From web comment by Matri Maria:
        // The TIFF 6 spec is more than a little vague about what white point should
        // be used for L*a*b images, and there's no specified tag to put that data
        // into the file.
        //
        // There is a tag to store White point, the tag is:
        //     Tag = 318 (13Eh)
        //     Type = Rational
        //     N = 2
        // The chromaticity of the white point of the image.  This is the chromaticity when
        // each of the primaries has its ReferenceWhite value.
        // The value is described using the 1931 CIE xy chromaticity diagram and only the
        // chromaticity is specified.
        //
        // TIFF 6.0 specification has a "Section23: CIE L*a*b images" where it says
        // that "White point : does not apply, but I have seen several TIFF with
        // photometric = CIELAB and white point specified as D50 in this tag.
        
        // Adobe Photoshop TIFF Technical Notes states:
        // WhitePoint: default is CIE D50, may be omitted if default value applies
        
        // www.easyrgb.com uses D65
        //double refX = 95.047;  // Observer = 2 degrees, Illuminant = D65
        //double refY = 100.000; // Observer = 2 degrees, Illuminant = D65
        //double refZ = 108.883; // Observer = 2 degrees, Illuminant = D65
        double refX = 96.420;  // Observer = 2 degrees, Illuminant = D50
        double refY = 100.000; // Observer = 2 degrees, Illuminant = D50
        double refZ = 82.490;  // Observer = 2 degrees, Illuminant = D50
        double xMul = threeDeltaSquared * refX;
        double yMul = threeDeltaSquared * refY;
        double zMul = threeDeltaSquared * refZ;
        double X;
        double Y;
        double Z;
        double varR;
        double varG;
        double varB;
        int sliceLength = buffer.length/4;
        int i;
        if (fileInfo.getDataType() == ModelStorageBase.ARGB) {
            for (i = 0; i < sliceLength; i++) {
                buffer[4*i + 1] = buffer[4*i + 1]*100.0f/255.0f;
                varY = (buffer[4*i + 1] + 16.0)/116.0;
                if (buffer[4*i + 2] > 127) {
                    buffer[4*i + 2] = buffer[4*i + 2] - 256;   
                }
                varX = buffer[4*i + 2]/500.0 + varY;
                if (buffer[4*i + 3] > 127) {
                    buffer[4*i + 3] = buffer[4*i + 3] - 256;
                }
                varZ = varY - buffer[4*i + 3]/200.0;
                
                if (varY > delta) {
                    Y = refY * varY * varY * varY;
                }
                else {
                    Y = (varY - offset) * yMul;
                }
                
                if (varX > delta) {
                    X = refX * varX * varX * varX;;
                }
                else {
                    X = (varX - offset) * xMul;
                }
                
                if (varZ > delta) {
                    Z = refZ * varZ * varZ * varZ;
                }
                else {
                    Z = (varZ - offset) * zMul;
                }
                
                varX = X / 100.0;
                varY = Y / 100.0;
                varZ = Z / 100.0;
                
                varR = 3.2406 * varX - 1.5372 * varY - 0.4986 * varZ;
                varG = -0.9689 * varX + 1.8758 * varY + 0.0415 * varZ;
                varB = 0.0557 * varX - 0.2040 * varY + 1.0570 * varZ;
                
                if (varR > 0.0031308) {
                    varR = 1.055 * Math.pow(varR, (1.0/2.4)) - 0.055;
                }
                else {
                    varR = 12.92 * varR;
                }
                if (varR < 0.0) {
                    varR = 0.0;
                }
                if (varR > 1.0) {
                    varR = 1.0;
                }
                
                if (varG > 0.0031308) {
                    varG = 1.055 * Math.pow(varG, (1.0/2.4)) - 0.055;
                }
                else {
                    varG = 12.92 * varG;
                }
                if (varG < 0.0) {
                    varG = 0.0;
                }
                if (varG > 1.0) {
                    varG = 1.0;
                }
                
                if (varB > 0.0031308) {
                    varB = 1.055 * Math.pow(varB, (1.0/2.4)) - 0.055;
                }
                else {
                    varB = 12.92 * varB;
                }
                if (varB < 0.0) {
                    varB = 0.0;
                }
                if (varB > 1.0) {
                    varB = 1.0;
                }
                
                buffer[4*i + 1] = (float)(varR * 255.0);
                buffer[4*i + 2] = (float)(varG * 255.0);
                buffer[4*i + 3] = (float)(varB * 255.0);
            }
        }
        else if (fileInfo.getDataType() == ModelStorageBase.ARGB_USHORT) {
            for (i = 0; i < sliceLength; i++) {
                buffer[4*i + 1] = buffer[4*i + 1]*100.0f/65535.0f;
                varY = (buffer[4*i + 1] + 16.0)/116.0;
                if (buffer[4*i + 2] > 32767) {
                    buffer[4*i + 2] = buffer[4*i + 2] - 65536;   
                }
                buffer[4*i + 2] = buffer[4*i + 2]/256;
                varX = buffer[4*i + 2]/500.0 + varY;
                if (buffer[4*i + 3] > 32767) {
                    buffer[4*i + 3] = buffer[4*i + 3] - 65536;
                }
                buffer[4*i + 3] = buffer[4*i + 3]/256;
                varZ = varY - buffer[4*i + 3]/200.0;
                
                if (varY > delta) {
                    Y = refY * varY * varY * varY;
                }
                else {
                    Y = (varY - offset) * yMul;
                }
                
                if (varX > delta) {
                    X = refX * varX * varX * varX;;
                }
                else {
                    X = (varX - offset) * xMul;
                }
                
                if (varZ > delta) {
                    Z = refZ * varZ * varZ * varZ;
                }
                else {
                    Z = (varZ - offset) * zMul;
                }
                
                varX = X / 100.0;
                varY = Y / 100.0;
                varZ = Z / 100.0;
                
                varR = 3.2406 * varX - 1.5372 * varY - 0.4986 * varZ;
                varG = -0.9689 * varX + 1.8758 * varY + 0.0415 * varZ;
                varB = 0.0557 * varX - 0.2040 * varY + 1.0570 * varZ;
                
                if (varR > 0.0031308) {
                    varR = 1.055 * Math.pow(varR, (1.0/2.4)) - 0.055;
                }
                else {
                    varR = 12.92 * varR;
                }
                if (varR < 0.0) {
                    varR = 0.0;
                }
                if (varR > 1.0) {
                    varR = 1.0;
                }
                
                if (varG > 0.0031308) {
                    varG = 1.055 * Math.pow(varG, (1.0/2.4)) - 0.055;
                }
                else {
                    varG = 12.92 * varG;
                }
                if (varG < 0.0) {
                    varG = 0.0;
                }
                if (varG > 1.0) {
                    varG = 1.0;
                }
                
                if (varB > 0.0031308) {
                    varB = 1.055 * Math.pow(varB, (1.0/2.4)) - 0.055;
                }
                else {
                    varB = 12.92 * varB;
                }
                if (varB < 0.0) {
                    varB = 0.0;
                }
                if (varB > 1.0) {
                    varB = 1.0;
                }
                
                buffer[4*i + 1] = (float)(varR * 65535.0);
                buffer[4*i + 2] = (float)(varG * 65535.0);
                buffer[4*i + 3] = (float)(varB * 65535.0);
            }    
        }
        
    }
    
    /**
     * 
     * @param buffer
     * @param YBuffer
     * @param CbInBuffer
     * @param CrInBuffer
     */
    private void YCbCrtoRGB(float buffer[], int YBuffer[], int CbInBuffer[], int CrInBuffer[]) {
        int i;
        int sliceSize = xDim * yDim;
        int CbOutBuffer[];
        int CrOutBuffer[];
        float floatR;
        float floatG;
        float floatB;
        int R;
        int G;
        int B;
        int originalExtents[] = new int[2];
        ModelImage originalImage;
        ModelImage newImage;
        TransMatrix xfrm;
        float originalResolutions[] = new float[2];
        AlgorithmTransform transform;
        float newXResolution;
        float newYResolution;
        boolean transformVOI = false;
        boolean clip = true;
        boolean pad = false;
        float Sx;
        float Sy;
        float inx1;
        float inx2;
        float iny1;
        float iny2;
        float outx1;
        float outx2;
        float outy1;
        float outy2;
        float T02;
        float T12;
        int x;
        int y;
        
        if ((YCbCrSubsampleHoriz == 1) && (YCbCrSubsampleVert == 1)) {
            CbOutBuffer = CbInBuffer;
            CrOutBuffer = CrInBuffer;
        }
        else {
            CbOutBuffer = new int[sliceSize];
            CrOutBuffer = new int[sliceSize];
            originalExtents[0] = xDim/YCbCrSubsampleHoriz;
            if ((xDim % YCbCrSubsampleHoriz) != 0) {
                originalExtents[0]++;
            }
            originalExtents[1] = yDim/YCbCrSubsampleVert;
            if ((yDim % YCbCrSubsampleVert) != 0) {
                originalExtents[1]++;
            }
            originalResolutions[0] = 1.0f;
            originalResolutions[1] = 1.0f;
            // output is the unshrunk image and input is the shrunk image
            // xfrm is the inverse output to input transformation matrix
            // input X = (output X * oXres* xfrm[0,0] + xfrm[0,2])/iXres
            // For YCbCrPositioning = 1 for centered:
            // output X = YCbCrSubsampleHoriz/2 - 0.5 yields input X = 0
            // output X = xDim - 1 - (YCbCrSubsampleHoriz/2 - 0.5) yields input originalExtents[0] - 1.
            // xfrm[0,0] = Sx = (xDim - 1)/(xDim - YCbCrSubsampleHoriz).
            // For YCbCrpositioning = 2 for cosited:
            // output x = 0 yields input X = 0
            // ouput x = xDim - YCbCrSubsampleHoriz yields input originalExtents[0] - 1.
            // xfrm[0,0] = Sx = (xDim - 1)/(xDim - YCbCrSubsampleHoriz).
            // inx1 = outx1 * newXResolution * Sx + T02
            // inx2 = outx2 * newXResolution * Sx + T02
            // inx2 - inx1 = (outx2 - outx1) * newXResolution * Sx
            // newXResolution = (inx2 - inx1)/((outx2 - outx1) * Sx)
            Sx = ((float)(xDim - 1))/((float)(xDim - YCbCrSubsampleHoriz));
            Sy = ((float)(yDim - 1))/((float)(yDim - YCbCrSubsampleVert));
            inx1 = 0.0f;
            inx2 = originalExtents[0] - 1.0f;
            iny1 = 0.0f;
            iny2 = originalExtents[1] - 1.0f;
            if (YCbCrPositioning == 1) {
                if (YCbCrSubsampleHoriz == 1) {
                    outx1 = 0;
                    outx2 = xDim - 1;
                }
                else {
                    outx1 = YCbCrSubsampleHoriz/2 - 0.5f;
                    outx2 = xDim - 1 - (YCbCrSubsampleHoriz/2 - 0.5f);
                }
                if (YCbCrSubsampleVert == 1) {
                    outy1 = 0;
                    outy2 = yDim - 1;
                }
                else {
                    outy1 = YCbCrSubsampleVert/2 - 0.5f;
                    outy2 = yDim - 1 - (YCbCrSubsampleVert/2 - 0.5f);
                }
            }
            else {
                outx1 = 0.0f;
                outx2 = xDim - YCbCrSubsampleHoriz;
                outy1 = 0.0f;
                outy2 = yDim - YCbCrSubsampleVert;
            }
            if (YCbCrSubsampleHoriz == 1) {
                newXResolution = 1.0f;
            }
            else {
                newXResolution = (inx2 - inx1)/((outx2 - outx1) * Sx);    
            }
            if (YCbCrSubsampleVert == 1) {
                newYResolution = 1.0f;
            }
            else {
                newYResolution = (iny2 - iny1)/((outy2 - outy1) * Sy);
            }
            T02 = inx1 - outx1 * newXResolution * Sx;
            T12 = iny1 - outy1 * newYResolution * Sy;
            xfrm = new TransMatrix(3);
            xfrm.identity();
            xfrm.setZoom(Sx, Sy);
            xfrm.setTranslate(T02, T12);
            if (!xfrm.isIdentity()) {
                double inverseMat[][] = (xfrm.inverse()).getArray();
                xfrm.setMatrix(inverseMat);
            }
            
            originalImage = new ModelImage(ModelStorageBase.INTEGER, originalExtents, "originalImage");
            originalImage.getFileInfo()[0].setResolutions(originalResolutions);
            try {
                 originalImage.importData(0, CbInBuffer, true);   
            }
            catch (IOException e) {
                MipavUtil.displayError("IOException on originalImage.importData(0, CbInBuffer, true");
                return;
            }
            
            transform = new AlgorithmTransform(originalImage, xfrm, AlgorithmTransform.BILINEAR, newXResolution,
                                               newYResolution, xDim, yDim,
                                               transformVOI, clip, pad);
    
            transform.run();
            newImage = transform.getTransformedImage();
            transform.finalize();
            try {
                newImage.exportData(0, sliceSize, CbOutBuffer);
            }
            catch(IOException e) {
                MipavUtil.displayError("IOException on originalImage.exportData(0, sliceSize, CbOutBuffer");
                return;    
            }
            newImage.disposeLocal();
            newImage = null;
            
            try {
                originalImage.importData(0, CrInBuffer, true);   
           }
           catch (IOException e) {
               MipavUtil.displayError("IOException on originalImage.importData(0, CrInBuffer, true");
               return;
           }
           
           transform = new AlgorithmTransform(originalImage, xfrm, AlgorithmTransform.BILINEAR, newXResolution,
                                              newYResolution, xDim, yDim,
                                              transformVOI, clip, pad);
    
           transform.run();
           newImage = transform.getTransformedImage();
           transform.finalize();
           try {
               newImage.exportData(0, sliceSize, CrOutBuffer);
           }
           catch(IOException e) {
               MipavUtil.displayError("IOException on originalImage.exportData(0, sliceSize, CrOutBuffer");
               return;    
           }
           newImage.disposeLocal();
           newImage = null;
           
           if (YCbCrPositioning == 1) {
               if (YCbCrSubsampleVert != 1) {
                   for (y = 0; y < (int)Math.floor(outy1); y++) {
                       for (x = 0; x < xDim; x++) {
                           CbOutBuffer[x + y*xDim] = CbOutBuffer[x + ((int)Math.ceil(outy1))*xDim];
                           CrOutBuffer[x + y*xDim] = CrOutBuffer[x + ((int)Math.ceil(outy1))*xDim];
                       }  
                   }
                 
                   for (y = (int)Math.ceil(outy2); y < yDim; y++) {
                       for (x = 0; x < xDim; x++){
                           CbOutBuffer[x + y*xDim] = CbOutBuffer[x + ((int)Math.floor(outy2))*xDim];
                           CrOutBuffer[x + y*xDim] = CrOutBuffer[x + ((int)Math.floor(outy2))*xDim];
                       }
                   }
               } // if (YCbCrSubsampleVert != 1)
               
               if (YCbCrSubsampleHoriz != 1) {
                   for (x = 0; x < (int)Math.floor(outx1); x++) {
                       for (y = 0; y < yDim; y++) {
                           CbOutBuffer[x + y*xDim] = CbOutBuffer[((int)Math.ceil(outx1)) + y*xDim];
                           CrOutBuffer[x + y*xDim] = CrOutBuffer[((int)Math.ceil(outx1)) + y*xDim];
                       }  
                   }
                   for (x = (int)Math.ceil(outx2); x < xDim; x++) {
                       for (y = 0; y < yDim; y++){
                           CbOutBuffer[x + y*xDim] = CbOutBuffer[((int)Math.floor(outx2)) + y*xDim];
                           CrOutBuffer[x + y*xDim] = CrOutBuffer[((int)Math.floor(outx2)) + y*xDim];
                       }
                   }
               } // if (YCbCrSubsampleHoriz != 1)
           } // if (YCbCrPositioning == 1)
           else { // YCbCrPositioning == 2
               if (YCbCrSubsampleVert != 1) {
                 for (y = yDim - YCbCrSubsampleVert + 1; y < yDim; y++) {
                     for (x = 0; x < xDim; x++) {
                         CbOutBuffer[x + y*xDim] = CbOutBuffer[x + (yDim - YCbCrSubsampleVert)*xDim];
                         CrOutBuffer[x + y*xDim] = CrOutBuffer[x + (yDim - YCbCrSubsampleVert)*xDim];    
                     }
                 }
               } // if (YCbCrSubsampleVert != 1)
               if (YCbCrSubsampleHoriz != 1) {
                 for (x = xDim - YCbCrSubsampleHoriz + 1; x < xDim; x++) {
                     for (y = 0; y < yDim; y++) {
                         CbOutBuffer[x + y*xDim] = CbOutBuffer[(xDim - YCbCrSubsampleHoriz) + y*xDim];
                         CrOutBuffer[x + y*xDim] = CrOutBuffer[(xDim - YCbCrSubsampleHoriz) + y*xDim];    
                     }
                 }
               } // if (YCbCrSubsampleHoriz != 1)
           } // else YCbCrPositioning == 2
        } // else !((YCbCrSubsampleHoriz == 1) && (YCbCrSubsampleVert == 1))
        
        for (i = 0; i < sliceSize; i++) {
            YBuffer[i] = (YBuffer[i] - YReferenceBlack)*255/(YReferenceWhite - YReferenceBlack);
            CbOutBuffer[i] = (CbOutBuffer[i] - CbReferenceBlack)*127/(CbReferenceWhite - CbReferenceBlack);
            CrOutBuffer[i] = (CrOutBuffer[i] - CrReferenceBlack)*127/(CrReferenceWhite - CrReferenceBlack);
            floatR = CrOutBuffer[i]*(2 - 2*LumaRed) + YBuffer[i];
            floatB = CbOutBuffer[i]*(2 - 2*LumaBlue) + YBuffer[i];
            floatG = (YBuffer[i] - LumaBlue * floatB - LumaRed * floatR)/LumaGreen;
            R = (int)Math.round(floatR);
            if (R < 0) {
                R = 0;
            }
            if (R > 255) {
                R = 255;
            }
            buffer[4*i] = (byte)255;
            buffer[4*i+1] = (byte)R;
            G = (int)Math.round(floatG);
            if (G < 0) {
                G = 0;
            }
            if (G > 255) {
                G = 255;
            }
            buffer[4*i+2] = (byte)G;
            B = (int)Math.round(floatB);
            if (B < 0) {
                B = 0;
            }
            if (B > 255) {
                B = 255;
            }
            buffer[4*i+3] = (byte)B;
        }
    }
    
    /**
     * Method to decode LZW compressed data.
     *
     * @param   inData        The compressed data.
     * @param   bytesToRead   bytes to be read from inData
     * @param   outData  Array to return the uncompressed data in.
     * @param   startingRow First row to be generated from the call
     * @param   rowsToDo The number of rows the compressed data contains.
     * @param   bytesGeneratedPerRow
     */
    // Ported from C++ code in tif_lzw.c in tif-4.0.0.alpha\libtiff
    // except for the final section for the Horizontal Differencing Predictor
    // which was ported from the Sun Microsystems file TIFFLZWDecoder.java.
    // The following copyright notices appear in the original code of tif_lzw.c.
    /*
     * Copyright (c) 1988-1997 Sam Leffler
     * Copyright (c) 1991-1997 Silicon Graphics, Inc.
     *
     * Permission to use, copy, modify, distribute, and sell this software and 
     * its documentation for any purpose is hereby granted without fee, provided
     * that (i) the above copyright notices and this permission notice appear in
     * all copies of the software and related documentation, and (ii) the names of
     * Sam Leffler and Silicon Graphics may not be used in any advertising or
     * publicity relating to the software without the specific, prior written
     * permission of Sam Leffler and Silicon Graphics.
     * 
     * THE SOFTWARE IS PROVIDED "AS-IS" AND WITHOUT WARRANTY OF ANY KIND, 
     * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY 
     * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  
     * 
     * IN NO EVENT SHALL SAM LEFFLER OR SILICON GRAPHICS BE LIABLE FOR
     * ANY SPECIAL, INCIDENTAL, INDIRECT OR CONSEQUENTIAL DAMAGES OF ANY KIND,
     * OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
     * WHETHER OR NOT ADVISED OF THE POSSIBILITY OF DAMAGE, AND ON ANY THEORY OF 
     * LIABILITY, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 
     * OF THIS SOFTWARE.
     */
    /*
     * TIFF Library.  
     * Rev 5.0 Lempel-Ziv & Welch Compression Support
     *
     * This code is derived from the compress program whose code is
     * derived from software contributed to Berkeley by James A. Woods,
     * derived from original work by Spencer Thomas and Joseph Orost.
     *
     * The original Berkeley copyright notice appears below in its entirety.
     */
    
    /*
     * Copyright (c) 1985, 1986 The Regents of the University of California.
     * All rights reserved.
     *
     * This code is derived from software contributed to Berkeley by
     * James A. Woods, derived from original work by Spencer Thomas
     * and Joseph Orost.
     *
     * Redistribution and use in source and binary forms are permitted
     * provided that the above copyright notice and this paragraph are
     * duplicated in all such forms and that any documentation,
     * advertising materials, and other materials related to such
     * distribution and use acknowledge that the software was developed
     * by the University of California, Berkeley.  The name of the
     * University may not be used to endorse or promote products derived
     * from this software without specific prior written permission.
     * THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR
     * IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
     * WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
     */
    
    // The following copyright notice appears in the Sun Microsystems file TIFFLZWDecoder.java.
    /*
     * Copyright (c) 2001 Sun Microsystems, Inc. All Rights Reserved.
     *
     * Redistribution and use in source and binary forms, with or without
     * modification, are permitted provided that the following conditions are met:
     *
     * -Redistributions of source code must retain the above copyright notice, this
     * list of conditions and the following disclaimer.
     *
     * -Redistribution in binary form must reproduct the above copyright notice,
     * this list of conditions and the following disclaimer in the documentation
     * and/or other materials provided with the distribution.
     *
     * Neither the name of Sun Microsystems, Inc. or the names of contributors may
     * be used to endorse or promote products derived from this software without
     * specific prior written permission.
     *
     * This software is provided "AS IS," without a warranty of any kind. ALL
     * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
     * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
     * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
     * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
     * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
     * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
     * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
     * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
     * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
     * POSSIBILITY OF SUCH DAMAGES.
     *
     * You acknowledge that Software is not designed,licensed or intended for use in
     * the design, construction, operation or maintenance of any nuclear facility.
     */

    private void LZWDecompresser(byte[] inData, int bytesToRead, byte[] outData, int startingRow, int rowsToDo, int bytesToGeneratePerRow) 
                 throws IOException {
        final int BITS_MIN = 9; // Start LZW with 9 bits
        final int BITS_MAX = 12; // max of 12 bit strings for LZW
        final int CODE_CLEAR = 256; // code to clear string table
        final int CODE_EOI = 257; // end-of-information code
        final int CODE_FIRST = 258; // first free code entry
        // +1024 is for compatibility with old LZW files
        final int CSIZE = (1 << BITS_MAX) + 1023;
        int i;
        short code;
        int occ;
        int inPosition = 0;
        int op0 = 0; // initial output position
        int op;
        int tp;
        long residue;
        byte t;
        int bp;
        long localNBits;
        long localNextBits;
        long localNextData;
        long localNBitsMask;
        int localCurrentRecognizedCode;
        int localNextFreeEntry;
        int localMaxAvailableEntry;
        int localPreviousRecognizedCode;
        int len;
        if (!haveLZWInit) {
            haveLZWInit = true;
            next = new int[CSIZE];
            length = new short[CSIZE];
            value = new byte[CSIZE];
            firstChar = new byte[CSIZE];
            code = 255;
            do {
                value[code] = (byte)code;
                firstChar[code] = (byte)code;
                length[code] = 1;
                next[code] = -1;
            } while (code-- > 0);
        } // if (!haveLZWInit)
        if ((inData[0] == 0) && ((inData[1] & 0x1) == 1)) {
            // Check for old bit-reversed codes.
            Preferences.debug("Old-style LZW codes\n");
            newLZW = false;
            maxCode = ((1 << BITS_MIN) - 1);
        }
        else {
            newLZW = true;
            maxCode = ((1 << BITS_MIN) - 2);
        }
        nBits = (short)BITS_MIN;
        nextBits = 0L;
        nextData = 0L;
        restart = 0L;
        nBitsMask = ((1L << BITS_MIN)- 1);
        bitsLeft = (bytesToRead << 3);
        nextFreeEntry = CODE_FIRST;
        // Zero entries that are not yet filled in.  We do this to guard against bogus input data
        // that causes us to index into undefined entries.
        for (i = nextFreeEntry; i < CSIZE; i++) {
            value[i] = (byte)0;
            firstChar[i] = (byte)0;
            length[i] = (short)0;
            next[i] = -1;
        }
        previousRecognizedCode = -1;
        maxAvailableEntry = (int)(nBitsMask - 1);
        op = op0;
        if (newLZW) {
            occ = bytesToGeneratePerRow * rowsToDo;  
            if (restart > 0) {
                // Restart interrupted output operation
                localCurrentRecognizedCode = currentRecognizedCode;
                residue = length[localCurrentRecognizedCode] - restart;
                if (residue > occ) {
                    // Residue from previous decode is sufficient to satisfy the decode request.
                    // Skip to the start of the decoded string, place decoded values in the output 
                    // buffer, and return.
                    restart += occ;
                    do {
                        localCurrentRecognizedCode = next[localCurrentRecognizedCode];
                    } while ((--residue > occ) && (localCurrentRecognizedCode >= 0));
                    if (localCurrentRecognizedCode >= 0) {
                        tp = op + occ;
                        do {
                            outData[--tp] = value[localCurrentRecognizedCode];
                            localCurrentRecognizedCode = next[localCurrentRecognizedCode];
                        } while ((--occ > 0) && (localCurrentRecognizedCode >= 0));
                    } // if (localCurrentRecognizedCode >= 0)
                    return;
                } // if (residue > occ)
                // residue satisfies only part of the decode request
                op += residue;
                occ -= residue;
                tp = op;
                do {
                    --tp;
                    t = value[localCurrentRecognizedCode];
                    localCurrentRecognizedCode = next[localCurrentRecognizedCode];
                    outData[tp] = t;
                } while ((--residue > 0) && (localCurrentRecognizedCode >= 0));
                restart = 0;
            } // if (restart > 0)
            
            bp = inPosition;
            localNBits = nBits;
            localNextData = nextData;
            localNextBits = nextBits;
            localNBitsMask = nBitsMask;
            localPreviousRecognizedCode = previousRecognizedCode;
            localNextFreeEntry = nextFreeEntry;
            localMaxAvailableEntry = maxAvailableEntry;
            
            while (occ > 0) {
                if (bitsLeft < localNBits) {
                    Preferences.debug("Starting row " + startingRow + " not terminated with EOI code\n");
                    code = CODE_EOI;
                }
                else {
                    localNextData = (localNextData << 8) | (0xffL & inData[bp++]);
                    localNextBits += 8;
                    if (localNextBits < localNBits) {
                        localNextData = (localNextData << 8) | (0xffL & inData[bp++]);
                        localNextBits += 8;
                    }
                    code = (short)((localNextData >> (localNextBits - localNBits)) & localNBitsMask);
                    localNextBits -= localNBits;
                    bitsLeft -= localNBits;
                }
                if (code == CODE_EOI) {
                    break;
                }
                if (code == CODE_CLEAR) {
                    localNextFreeEntry = CODE_FIRST;
                    localNBits = BITS_MIN;
                    localNBitsMask = (1L << BITS_MIN) - 1;
                    localMaxAvailableEntry = (int)(localNBitsMask - 1);
                    if (bitsLeft < localNBits) {
                        Preferences.debug("Starting row " + startingRow + " not terminated with EOI code\n");
                        code = CODE_EOI;
                    }
                    else {
                        localNextData = (localNextData << 8) | (0xffL & inData[bp++]);
                        localNextBits += 8;
                        if (localNextBits < localNBits) {
                            localNextData = (localNextData << 8) | (0xffL & inData[bp++]);
                            localNextBits += 8;
                        }
                        code = (short)((localNextData >> (localNextBits - localNBits)) & localNBitsMask);
                        localNextBits -= localNBits;
                        bitsLeft -= localNBits;
                    }
                    if (code == CODE_EOI) {
                        break;
                    }
                    outData[op++] = (byte)code;
                    occ--;
                    localPreviousRecognizedCode = code;
                    continue;
                } // if (code == CODE_CLEAR)
                localCurrentRecognizedCode = code;
                
                // Add new entry to the code table
                if ((localNextFreeEntry < 0) || (localNextFreeEntry >= CSIZE)) {
                    throw new IOException("Corrupted LZW table at starting row = " + startingRow);
                }
                
                next[localNextFreeEntry] = localPreviousRecognizedCode;
                if ((next[localNextFreeEntry] < 0) || (next[localNextFreeEntry] >= CSIZE)) {
                    throw new IOException("Corrupted LZW table at starting row = " + startingRow);
                }
                firstChar[localNextFreeEntry] = firstChar[next[localNextFreeEntry]];
                length[localNextFreeEntry] = (short)(length[next[localNextFreeEntry]] + 1);
                if (localCurrentRecognizedCode < localNextFreeEntry) {
                    value[localNextFreeEntry] = firstChar[localCurrentRecognizedCode];
                }
                else {
                    value[localNextFreeEntry] = firstChar[localNextFreeEntry];    
                }
                if (++localNextFreeEntry > localMaxAvailableEntry) {
                    if (++localNBits > BITS_MAX) {
                        // should not happen
                        localNBits = BITS_MAX;
                    }
                    localNBitsMask = (1L << localNBits) - 1;
                    localMaxAvailableEntry = (int)(localNBitsMask - 1);
                } // if (++localNextFreeEntry > localMaxAvailableEntry)
                localPreviousRecognizedCode = localCurrentRecognizedCode;
                if (code >= 256) {
                    // Code maps to a string, copy string
                    // value to output (written in reverse).
                    if (length[localCurrentRecognizedCode] == 0) {
                        throw new IOException("Wrong length of decoded string Data probably corrupted at starting row = " +
                                startingRow);
                    }
                    if (length[localCurrentRecognizedCode] > occ) {
                        // String is too long for decode buffer, locate portion that will fit,
                        // copy to the decode buffer, and setup restart logic for the next
                        // decoding call.
                        currentRecognizedCode = localCurrentRecognizedCode;
                        do {
                            localCurrentRecognizedCode = next[localCurrentRecognizedCode];
                        } while((localCurrentRecognizedCode >= 0) && (length[localCurrentRecognizedCode] > occ));
                        if (localCurrentRecognizedCode >= 0) {
                            restart = (long)occ;
                            tp = op + occ;
                            do {
                                outData[--tp] = value[localCurrentRecognizedCode];
                                localCurrentRecognizedCode = next[localCurrentRecognizedCode];
                            } while ((--occ > 0) && (localCurrentRecognizedCode >= 0));
                            if (localCurrentRecognizedCode >= 0) {
                                throw new IOException("Bogus encoding, loop in the code table, starting row = " +
                                        startingRow);
                            } // if (localCurrentRecognizedCode >= 0)
                        } // if (localCurrentRecognizedCode >= 0)
                        break; // break out of while (occ > 0)
                    } // if (length[localCurrentRecognizedCode] > occ)
                    len = length[localCurrentRecognizedCode];
                    tp = op + len;
                    do {
                        --tp;
                        t = value[localCurrentRecognizedCode];
                        localCurrentRecognizedCode = next[localCurrentRecognizedCode];
                        outData[tp] = t;
                    } while ((localCurrentRecognizedCode >= 0) && (tp > op));
                    if (localCurrentRecognizedCode >= 0) {
                        throw new IOException("Bogus encoding, loop in the code table, starting row = " +
                                startingRow);
                    }
                    if (occ < len) {
                        throw new IOException("occ < len, starting row = " + startingRow);
                    }
                    op += len;
                    occ -= len;
                } // if (code >= 256)
                else {
                    outData[op++] = (byte)code;
                    occ--;
                }
            } // while (occ > 0)
            inPosition = bp;
            nBits = (short)localNBits;
            nextData = localNextData;
            nextBits = localNextBits;
            nBitsMask = localNBitsMask;
            previousRecognizedCode = localPreviousRecognizedCode;
            nextFreeEntry = localNextFreeEntry;
            maxAvailableEntry = localMaxAvailableEntry;
            if (occ > 0) {
                throw new IOException("Not enough data at starting row = " +
                        startingRow + " short by " + occ + " bytes");
            }
        } // if (newLZW)
        else { // oldLZW
            occ = bytesToGeneratePerRow * rowsToDo;
            if (restart > 0) {
                // Restart interrupted output operation
                localCurrentRecognizedCode = currentRecognizedCode;
                residue = length[localCurrentRecognizedCode] - restart;
                if (residue > occ) {
                    // Residue from previous decode is sufficient to satisfy the decode request.
                    // Skip to the start of the decoded string, place decoded values in the output 
                    // buffer, and return.
                    restart += occ;
                    do {
                        localCurrentRecognizedCode = next[localCurrentRecognizedCode];
                    } while (--residue > occ); 
                    tp = op + occ;
                    do {
                        outData[--tp] = value[localCurrentRecognizedCode];
                        localCurrentRecognizedCode = next[localCurrentRecognizedCode];
                    } while (--occ > 0);
                    return;
                } // if (residue > occ)
                // Residue satisfies only part of the decode request.
                op += residue;
                occ -= residue;
                tp = op;
                do {
                    outData[--tp] = value[localCurrentRecognizedCode];
                    localCurrentRecognizedCode = next[localCurrentRecognizedCode];
                } while (--residue > 0);
                restart = 0;
            } // if (restart > 0)
            
            bp = inPosition;
            localNBits = nBits;
            localNextData = nextData;
            localNextBits = nextBits;
            localNBitsMask = nBitsMask;
            localPreviousRecognizedCode = previousRecognizedCode;
            localNextFreeEntry = nextFreeEntry;
            localMaxAvailableEntry = maxAvailableEntry;
            
            while (occ > 0) {
                if (bitsLeft < localNBits) {
                    Preferences.debug("Starting row " + startingRow + " not terminated with EOI code\n");
                    code = CODE_EOI;
                }
                else {
                    localNextData |= ((0xffL & inData[bp++]) << localNextBits);
                    localNextBits += 8;
                    if (localNextBits < localNBits) {
                        localNextData |= ((0xffL & inData[bp++]) << localNextBits);
                        localNextBits += 8;
                    }
                    code = (short)(localNextData & localNBitsMask);
                    localNextData >>= localNBits;
                    localNextBits -= localNBits;
                    bitsLeft -= localNBits;
                }
                if (code == CODE_EOI) {
                    break;
                }
                if (code == CODE_CLEAR) {
                    localNextFreeEntry = CODE_FIRST;
                    localNBits = BITS_MIN;
                    localNBitsMask = (1L << BITS_MIN)-1;
                    localMaxAvailableEntry = (int)localNBitsMask;
                    if (bitsLeft < localNBits) {
                        Preferences.debug("Starting row " + startingRow + " not terminated with EOI code\n");
                        code = CODE_EOI;
                    }
                    else {
                        localNextData |= ((0xffL & inData[bp++]) << localNextBits);
                        localNextBits += 8;
                        if (localNextBits < localNBits) {
                            localNextData |= ((0xffL & inData[bp++]) << localNextBits);
                            localNextBits += 8;
                        }
                        code = (short)(localNextData & localNBitsMask);
                        localNextData >>= localNBits;
                        localNextBits -= localNBits;
                        bitsLeft -= localNBits;
                    }
                    if (code == CODE_EOI) {
                        break;
                    }
                    outData[op++] = (byte)code;
                    occ--;
                    localPreviousRecognizedCode = code;
                    continue;
                } // if (code == CODE_CLEAR)
                localCurrentRecognizedCode = code;
                
                // Add the new entry to the code table
                if ((localNextFreeEntry < 0) || (localNextFreeEntry >= CSIZE)) {
                    throw new IOException("Corrupted LZW table at starting row = " + startingRow);
                }
                next[localNextFreeEntry] = localPreviousRecognizedCode;
                if ((next[localNextFreeEntry] < 0) || (next[localNextFreeEntry] >= CSIZE)) {
                    throw new IOException("Corrupted LZW table at starting row = " + startingRow);
                }
                firstChar[localNextFreeEntry] = firstChar[next[localNextFreeEntry]];
                length[localNextFreeEntry] = (short)(length[next[localNextFreeEntry]] + 1);
                if (localCurrentRecognizedCode < localNextFreeEntry) {
                    value[localNextFreeEntry] = firstChar[localCurrentRecognizedCode];
                }
                else {
                    value[localNextFreeEntry] = firstChar[localNextFreeEntry];
                }
                if (++localNextFreeEntry > localMaxAvailableEntry) {
                    if (++localNBits > BITS_MAX) {
                        // should not happen
                        localNBits = BITS_MAX;
                    }
                    localNBitsMask = (1L << localNBits)-1;
                    localMaxAvailableEntry = (int)localNBitsMask;
                } // if (++localNextFreeEntry > localMaxAvailableEntry)
                localPreviousRecognizedCode = localCurrentRecognizedCode;
                if (code >= 256) {
                    // Code maps to a string, copy string value to output (written in reverse).
                    if (length[localCurrentRecognizedCode] == 0) {
                    throw new IOException("Wrong length of decoded string: Data probably corrupted, starting row = " +
                                startingRow);
                    }
                    if (length[localCurrentRecognizedCode] > occ) {
                        // String is too long for decode buffer, locate portion that will fit, copy to
                        // the decode buffer, and setup restart logic for the next decoding call.
                        currentRecognizedCode = localCurrentRecognizedCode;
                        do {
                            localCurrentRecognizedCode = next[localCurrentRecognizedCode];
                        } while (length[localCurrentRecognizedCode] > occ);
                        restart = occ;
                        tp = op + occ;
                        do {
                            outData[--tp] = value[localCurrentRecognizedCode];
                            localCurrentRecognizedCode = next[localCurrentRecognizedCode];
                        } while (--occ > 0);
                        break;
                    } // if (length[localCurrentRecognizedCode] > occ)
                    if (occ < length[localCurrentRecognizedCode]) {
                        throw new IOException("occ < length[localCurrentRecognizedCode], starting row = " +
                                             startingRow);
                    }
                    op += length[localCurrentRecognizedCode];
                    occ -= length[localCurrentRecognizedCode];
                    tp = op;
                    do {
                        outData[--tp] = value[localCurrentRecognizedCode];
                    } while ((localCurrentRecognizedCode = next[localCurrentRecognizedCode]) != -1);
                } // if (code >= 256)
                else {
                    outData[op++] = (byte)code;
                    occ--;
                }
            } // while (occ > 0)
            inPosition = bp;
            nBits = (short)localNBits;
            nextData = localNextData;
            nextBits = localNextBits;
            nBitsMask = localNBitsMask;
            previousRecognizedCode = localPreviousRecognizedCode;
            nextFreeEntry = localNextFreeEntry;
            maxAvailableEntry = localMaxAvailableEntry;
            if (occ > 0) {
                throw new IOException("Not enough data at starting row = " + startingRow + " short by " + occ + " bytes");
            }
        } // else oldLZW
        
        // Horizontal Differencing Predictor
        if (predictor == 2) {

            int count;

            for (int j = 0; j < rowsToDo; j++) {

                count = samplesPerPixel * ((j * tileWidth) + 1);

                for (i = samplesPerPixel; i < (tileWidth * samplesPerPixel); i++) {

                    outData[count] += outData[count - samplesPerPixel];
                    count++;
                }
            }
        }
    }
    
    // Almost all of the FAX decompression code and modified Huffman decompression code 
    // was taken from the website http://www.mms-computing.co.uk/uk/co/mmscomputing/imageio/tiff
    
    private void fax34Init() {
        if (bitsPerSample == null) {
            Preferences.debug("bitsPerSample not found for fax decoding - settting to 1\n");
            bitsPerSample = new int[1];
            bitsPerSample[0] = 1;
            return;
        }
        if (bitsPerSample[0] != 1) {
            MipavUtil.displayError("Must have bits per sample == 1 for fax3 and fax4 decoding");
        }   
        return;
    }
    
    //  Modified from part of routine readBWImage in TiffBaselineFactory.java in package uk.co.mmscomputing.imageio.tiff
    private int modHuffmanDecompresser(byte dataOut[], byte dataIn[]) {
        InputStream is;
        int offset = 0;
        int resultLength;
        
        is = new ByteArrayInputStream(dataIn);
        if (fillOrder == 1) { // baseline tiff default
            try {
                is = new  BitSwapInputStream(is);
            }
            catch(IOException e) {
                MipavUtil.displayError("IOEXception on is = new BitSwapInputStream(is)");
                return -1;
            }
        } // if (fillOrder == 1)
        try {
            resultLength = readMH(dataOut, offset, is, xDim);
        }
        catch(IOException e) {
            MipavUtil.displayError("IOException on resultLength = readMH(dataOut, offset, is, xDim");
            return -1;
        }
        return resultLength;
        
    }
    
    // Copied almost unchanged from TiffBaselineFactory.java in package uk.co.mmscomputing.imageio.tiff
    private int readMH(byte[] imgdata,int off,InputStream is,int width)throws IOException{
        ModHuffmanInputStream mhis=new ModHuffmanInputStream(is);
        RLEBitInputStream     rlis=new RLEBitInputStream(mhis);

        if((width&0x0007)==0){
          byte[] buf=new byte[width>>3];int len=0;
          while(true){
            rlis.resetToStartCodeWord();                    // start next line with white
            try{
              len=rlis.read(buf);                           // read one image line
              if(len==-1){break;}                           // end of page
              System.arraycopy(buf,0,imgdata,off,len);      // copy line to image buffer
              mhis.skipPadding(8);                          // skip bits up until next byte boundary
            }catch(ModHuffmanInputStream.ModHuffmanCodingException mhce){
              MipavUtil.displayError("copyin:\n\t"+mhce);
            }
            off+=len;
          }
        }else{
          byte[] buf=new byte[(width+7)>>3];int len=0,ecw=8-(width&0x0007),bits;
          while(true){
            rlis.resetToStartCodeWord();                    // start next line with white
            try{
              len=rlis.read(buf,0,buf.length-1);            // read one image line
              if(len==-1){break;}                           // end of page
              bits=rlis.readBits(7,ecw);
              buf[len]=(byte)bits;
              System.arraycopy(buf,0,imgdata,off,len+1);    // copy line to image buffer
              mhis.skipPadding(8);                          // skip bits up until next byte boundary
            }catch(ModHuffmanInputStream.ModHuffmanCodingException mhce){
              MipavUtil.displayError("copyin:\n\t"+mhce);
            }
            off+=len+1;
          }
        }
        return off;
      }
    
    // Modified from routines readRGBImage and readYCbCrImage in TiffBaselineFactory.java in 
    // package uk.co.mmscomputing.imageio.tiff
    private int jpegDecompresser(byte dataOut[], byte dataIn[]) {
        InputStream is;
        IntFilterInputStream intis;
        TIFFYCbCrInputStream ycbcris;
        int resultLength;
        int mbps = xDim * rowsPerStrip;
        int max = xDim * yDim;
        int dataOutInt[] = new int[dataOut.length/samplesPerPixel];
        int i;
        
        if (max < mbps) {
            mbps = max;
        }
        
        is = new ByteArrayInputStream(dataIn);
        if (fillOrder != 1) {
            try {
                is = new BitSwapInputStream(is);
            }
            catch(IOException e) {
                MipavUtil.displayError("IOExeption on is = new BitSwapInputStream(is)");
                return -1;
            } 
        } // if (fillOrder != 1)
        
        if (tableStream != null) {
            try {
                intis = new JPEGInputStream(is,tableStream.getQTs(), tableStream.getDCIns(), tableStream.getACIns());
            }
            catch(IOException e) {
                MipavUtil.displayError("IOException on JPEGInputStream(is, tableStream.getQTs(), " +
                        "tableStream.getDCIns(), tableStream.getACIns())");
                return - 1;
            }
        } // if (tableStream != null)
        else {
            try {
                intis = new JPEGInputStream(is);
            }
            catch(IOException e) {
                MipavUtil.displayError("IOException on initis = new JPEGInputStream(is)");
                return -1;
            }
        } // else
        if (isYCbCr) {
            try {
                ycbcris = new TIFFYCbCrInputStream(intis);
                
            }
            catch(IOException e) {
                MipavUtil.displayError("IOException on ycbcris = new TIFFYCbCrInputStream(intis)");
                return -1;
            }
            ycbcris.setColourCoefficients((double)LumaRed,(double)LumaGreen,(double)LumaBlue);
            ycbcris.setRfBWY(YReferenceBlack, YReferenceWhite);
            ycbcris.setRfBWCb(CbReferenceBlack, CbReferenceWhite);
            ycbcris.setRfBWCr(CrReferenceBlack, CrReferenceWhite);
            try {
                resultLength = ycbcris.read(dataOutInt, 0, mbps);
            }
            catch(IOException e) {
                MipavUtil.displayError("IOException on ycbcris.read(dataOut, 0, mbps)");
                return -1;
            }
        } // if (isYCbCr)
        else {
            try {
                resultLength = intis.read(dataOutInt, 0, mbps);
            }
            catch(IOException e) {
                MipavUtil.displayError("IOException on resultLength = initis.read(dataOut, 0, mbps)");
                return -1;
            }
        } // else
        for (i = 0; i < resultLength; i++) {
            dataOut[3*i] = (byte)(0xff & (dataOutInt[i] >> 16));
            dataOut[3*i + 1] = (byte)(0xff & (dataOutInt[i] >> 8));
            dataOut[3*i + 2] = (byte)(0xff & dataOutInt[i]);
        }
        return 3*resultLength;
    }
    
    /* LogL16Decompresser is created from a port of LogL16Decode in tif_luv.h 
     * LogL16Decode was set up to work on only 1 row at a time, so I added the
     * outer loop for (r = 0; r < rowsToDo; r++)
     * After the decoding, note that the luminance is then a sign bit followed
     * by a 15-bit integer.  I convert this to a traditional signed short.
     * I leave the data in short form.  However, if desired conversion to actual
     * luminance in candelas/meter**2 could be accomplished by converting to a float with the formula
     * L = 2**((Le + 0.5)/256 - 64)
     * and multiplying L by the value found in the STONITS tag.
     */
     // The following copyright notice appears in the original code:
    /*
     * Copyright (c) 1997 Greg Ward Larson
     * Copyright (c) 1997 Silicon Graphics, Inc.
     *
     * Permission to use, copy, modify, distribute, and sell this software and 
     * its documentation for any purpose is hereby granted without fee, provided
     * that (i) the above copyright notices and this permission notice appear in
     * all copies of the software and related documentation, and (ii) the names of
     * Sam Leffler, Greg Larson and Silicon Graphics may not be used in any
     * advertising or publicity relating to the software without the specific,
     * prior written permission of Sam Leffler, Greg Larson and Silicon Graphics.
     * 
     * THE SOFTWARE IS PROVIDED "AS-IS" AND WITHOUT WARRANTY OF ANY KIND, 
     * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY 
     * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  
     * 
     * IN NO EVENT SHALL SAM LEFFLER, GREG LARSON OR SILICON GRAPHICS BE LIABLE
     * FOR ANY SPECIAL, INCIDENTAL, INDIRECT OR CONSEQUENTIAL DAMAGES OF ANY KIND,
     * OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
     * WHETHER OR NOT ADVISED OF THE POSSIBILITY OF DAMAGE, AND ON ANY THEORY OF 
     * LIABILITY, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 
     * OF THIS SOFTWARE.
     */

    private int LogL16Decompresser(byte dataOut[], byte dataIn[], int bytesToRead, int rowsToDo) {
        int nPixels = xDim;
        int inPosition = 0;
        int i;
        int rc;
        byte b;
        short temp;
        int r;
        int m;
        /* get each byte string */
        for (r = 0; r < rowsToDo; r++) {
            for (m = 0; m < 2; m++) {
                for (i = 0; i < nPixels && bytesToRead > 0; ) {
                    if ((dataIn[inPosition] & 0xff) >= 128) { // run
                        rc = (dataIn[inPosition++] & 0xff) + (2 - 128);   
                        b = dataIn[inPosition++];
                        bytesToRead -= 2;
                        while ((rc-- > 0) && (i < nPixels)) {
                            dataOut[2*r*xDim + 2*i + m] = b;
                            i++;
                        }
                    } // if (dataIn[inPosition] >= 128)
                    else { // non-run
                        rc = dataIn[inPosition++] & 0xff;
                        while ((--bytesToRead > 0) && (rc-- > 0) && (i < nPixels)) {
                            dataOut[2*r*xDim + 2*i + m] =  dataIn[inPosition++];
                            i++;
                        }
                    } // else non-run
                } // for (i = 0; i < nPixels && bytesToRead > 0; )
            } // for (m = 0; m < 2; m++)
        } // for (r = 0; r < rowsToDo; r++)
        for (i = 0; i < xDim * rowsToDo; i++) {
            temp = (short)(((dataOut[2*i] << 8) & 0xff00) | (dataOut[2*i+1] & 0xff));
            if ((temp & 0x8000) != 0) {
                temp = (short)(temp & 0x7fff);
                temp = (short)(-temp);
                dataOut[2*i] = (byte)(temp >>> 8);
                dataOut[2*i + 1] = (byte)(temp & 0xff);
            }
          }
        return 2 * xDim * rowsToDo;
    }
    
    /* LogLuv24Decompresser is created from a port of LogLuvDecode24, LogLuv24toXYZ,
     * LogL10toY, uv_decode and XYZtoRGB24 in tif_luv.h and uvcode.h
     */
    // The following copyright notice appears in the original code:
    /*
     * Copyright (c) 1997 Greg Ward Larson
     * Copyright (c) 1997 Silicon Graphics, Inc.
     *
     * Permission to use, copy, modify, distribute, and sell this software and 
     * its documentation for any purpose is hereby granted without fee, provided
     * that (i) the above copyright notices and this permission notice appear in
     * all copies of the software and related documentation, and (ii) the names of
     * Sam Leffler, Greg Larson and Silicon Graphics may not be used in any
     * advertising or publicity relating to the software without the specific,
     * prior written permission of Sam Leffler, Greg Larson and Silicon Graphics.
     * 
     * THE SOFTWARE IS PROVIDED "AS-IS" AND WITHOUT WARRANTY OF ANY KIND, 
     * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY 
     * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  
     * 
     * IN NO EVENT SHALL SAM LEFFLER, GREG LARSON OR SILICON GRAPHICS BE LIABLE
     * FOR ANY SPECIAL, INCIDENTAL, INDIRECT OR CONSEQUENTIAL DAMAGES OF ANY KIND,
     * OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
     * WHETHER OR NOT ADVISED OF THE POSSIBILITY OF DAMAGE, AND ON ANY THEORY OF 
     * LIABILITY, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 
     * OF THIS SOFTWARE.
     */
    private int LogLuv24Decompresser(byte dataOut[], byte dataIn[], int bytesToRead, int rowsToDo) {
        final double M_LN2 = 0.69314718055994530942;
        final double U_NEU = 0.210526316;
        final double V_NEU = 0.473684211;
        float ustart[] = new float[]
        { 0.247663f, 0.243779f, 0.241684f, 0.237874f, 0.235906f, 0.232153f, 0.228352f, 0.226259f,
          0.222371f, 0.220410f, 0.214710f, 0.212714f, 0.210721f, 0.204976f, 0.202986f, 0.199245f,
          0.195525f, 0.193560f, 0.189878f, 0.186216f, 0.186216f, 0.182592f, 0.179003f, 0.175466f,
          0.172001f, 0.172001f, 0.168612f, 0.168612f, 0.163575f, 0.158642f, 0.158642f, 0.158642f,
          0.153815f, 0.153815f, 0.149097f, 0.149097f, 0.142746f, 0.142746f, 0.142746f, 0.138270f,
          0.138270f, 0.138270f, 0.132166f, 0.132166f, 0.126204f, 0.126204f, 0.126204f, 0.120381f,
          0.120381f, 0.120381f, 0.120381f, 0.112962f, 0.112962f, 0.112962f, 0.107450f, 0.107450f,
          0.107450f, 0.107450f, 0.100343f, 0.100343f, 0.100343f, 0.095126f, 0.095126f, 0.095126f,
          0.095126f, 0.088276f, 0.088276f, 0.088276f, 0.088276f, 0.081523f, 0.081523f, 0.081523f,
          0.081523f, 0.074861f, 0.074861f, 0.074861f, 0.074861f, 0.068290f, 0.068290f, 0.068290f,
          0.068290f, 0.063573f, 0.063573f, 0.063573f, 0.063573f, 0.057219f, 0.057219f, 0.057219f,
          0.057219f, 0.050985f, 0.050985f, 0.050985f, 0.050985f, 0.050985f, 0.044859f, 0.044859f,
          0.044859f, 0.044859f, 0.040571f, 0.040571f, 0.040571f, 0.040571f, 0.036339f, 0.036339f,
          0.036339f, 0.036339f, 0.032139f, 0.032139f, 0.032139f, 0.032139f, 0.027947f, 0.027947f,
          0.027947f, 0.023739f, 0.023739f, 0.023739f, 0.023739f, 0.019504f, 0.019504f, 0.019504f,
          0.016976f, 0.016976f, 0.016976f, 0.016976f, 0.012639f, 0.012639f, 0.012639f, 0.009991f,
          0.009991f, 0.009991f, 0.009016f, 0.009016f, 0.009016f, 0.006217f, 0.006217f, 0.005097f,
          0.005097f, 0.005097f, 0.003909f, 0.003909f, 0.002340f, 0.002389f, 0.001068f, 0.001653f,
          0.000717f, 0.001614f, 0.000270f, 0.000484f, 0.001103f, 0.001242f, 0.001188f, 0.001011f,
          0.000709f, 0.000301f, 0.002416f, 0.003251f, 0.003246f, 0.004141f, 0.005963f, 0.008839f,
          0.010490f, 0.016994f, 0.023659f};
        int ncum[] = new int[]
        { 0,         4,         10,        17,        26,        36,        48,        62,
          77,        94,        112,       133,       155,       178,       204,       231,
          260,       291,       323,       357,       393,       429,       467,       507,
          549,       593,       637,       683,       729,       778,       830,       882,
          934,       989,       1044,      1102,      1160,      1222,      1284,      1346,
          1411,      1476,      1541,      1610,      1679,      1752,      1825,      1898,
          1975,      2052,      2129,      2206,      2288,      2370,      2452,      2538,
          2624,      2710,      2796,      2887,      2978,      3069,      3164,      3259,
          3354,      3449,      3549,      3649,      3749,      3849,      3954,      4059,
          4164,      4269,      4379,      4489,      4599,      4709,      4824,      4939,
          5054,      5169,      5288,      5407,      5526,      5645,      5769,      5893,
          6017,      6141,      6270,      6399,      6528,      6657,      6786,      6920,
          7054,      7188,      7322,      7460,      7598,      7736,      7874,      8016,
          8158,      8300,      8442,      8588,      8734,      8880,      9026,      9176,
          9326,      9476,      9630,      9784,      9938,      10092,     10250,     10408,
          10566,     10727,     10888,     11049,     11210,     11375,     11540,     11705,
          11873,     12041,     12209,     12379,     12549,     12719,     12892,     13065,
          13240,     13415,     13590,     13767,     13944,     14121,     14291,     14455,
          14612,     14762,     14905,     15041,     15170,     15293,     15408,     15517,
          15620,     15717,     15806,     15888,     15964,     16033,     16095,     16150,
          16197,     16237,     16268};
        final int UV_NDIVS = 16289;
        final float UV_VSTART = 0.016940f;
        final float UV_SQSIZ = 0.003500f;
        final int UV_NVS = 163;

        int row;
        int nPixels = xDim;
        int i;
        int tp;
        int p10;
        double L;
        int Ce;
        double u;
        double v;
        int lower;
        int upper;
        int ui;
        int vi;
        double s;
        double x;
        double y;
        double xyz[] = new double[3];
        double r;
        double g;
        double b;
        for (row = 0; row < rowsToDo; row++) {
            for (i = 0; i < nPixels && bytesToRead > 0; i++) {    
              tp = ((dataIn[3*row*xDim + 3*i] << 16) & 0xff0000) |
                   ((dataIn[3*row*xDim + 3*i + 1] << 8) & 0xff00) |
                   (dataIn[3*row*xDim + 3*i + 2] & 0xff);
              p10 = (tp >> 14 & 0x3ff);
              // Compute luminance from 10-bit LogL
              if (p10 == 0) {
                  dataOut[3*row*xDim + 3*i] = 0;
                  dataOut[3*row*xDim + 3*i + 1] = 0;
                  dataOut[3*row*xDim + 3*i + 2] = 0;
              }
              else {
                  L = Math.exp(M_LN2/64.0 * (p10 + 0.5) - M_LN2 * 12.0);
                  if (L <= 0.0) {
                      dataOut[3*row*xDim + 3*i] = 0;
                      dataOut[3*row*xDim + 3*i + 1] = 0;
                      dataOut[3*row*xDim + 3*i + 2] = 0;
                  }
                  else {
                      // Decode color
                      Ce = tp & 0x3fff;
                      if ((Ce < 0) || (Ce >= UV_NDIVS)) {
                          u = U_NEU;
                          v = V_NEU;
                      }
                      else {
                          // binary search
                          lower = 0;
                          upper = UV_NVS;
                          while (upper - lower > 1) {
                              vi = (lower + upper) >> 1;
                              ui = Ce - ncum[vi];
                              if (ui > 0) {
                                  lower = vi;
                              }
                              else if (ui < 0) {
                                  upper = vi;
                              }
                              else {
                                  lower = vi;
                                  break;
                              }
                          } // while (upper - lower > 1)
                          vi = lower;
                          ui = Ce - ncum[vi];
                          u = ustart[vi] + (ui + 0.5)*UV_SQSIZ;
                          v = UV_VSTART + (vi + .5)*UV_SQSIZ;
                      } // else binary search
                      s = 1.0/(6.0 * u - 16.0*v + 12.0);
                      x = 9.0 * u * s;
                      y = 4.0 * v * s;
                      // Convert to XYZ
                      xyz[0] = x/y * L;
                      xyz[1] = L;
                      xyz[2] = (1.0 - x - y)/y * L;
                      /* assume CCIR-709 primaries */
                      r =  2.690*xyz[0] + -1.276*xyz[1] + -0.414*xyz[2];
                      g = -1.022*xyz[0] +  1.978*xyz[1] +  0.044*xyz[2];
                      b =  0.061*xyz[0] + -0.224*xyz[1] +  1.163*xyz[2];
                                      /* assume 2.0 gamma for speed */
                      /* could use integer sqrt approx., but this is probably faster */
                      if (r <= 0) {
                          dataOut[3*row*xDim + 3*i] = 0;
                      }
                      else if (r >= 1.0) {
                          dataOut[3*row*xDim + 3*i] = (byte)255;
                      }
                      else {
                          dataOut[3*row*xDim + 3*i] = (byte)(256.0* Math.sqrt(r));
                      }
                      if (g <= 0) {
                          dataOut[3*row*xDim + 3*i + 1] = 0;
                      }
                      else if (g >= 1.0) {
                          dataOut[3*row*xDim + 3*i + 1] = (byte)255;
                      }
                      else {
                          dataOut[3*row*xDim + 3*i + 1] = (byte)(256.0* Math.sqrt(g));
                      }
                      if (b <= 0) {
                          dataOut[3*row*xDim + 3*i + 2] = 0;
                      }
                      else if (b >= 1.0) {
                          dataOut[3*row*xDim + 3*i + 2] = (byte)255;
                      }
                      else {
                          dataOut[3*row*xDim + 3*i + 2] = (byte)(256.0* Math.sqrt(b));
                      }
                  }
              }
            } // for (i = 0; i < nPixels && bytesToRead > 0; i++)
        } // for (row = 0; row < rowsToDo; row++)
        return 3 * xDim *rowsToDo;
    }
    
    
    /* LogLuv32Decompresser is created from a port of LogLuvDecode32, 
     * LogL16toY, LogLuv32toXYZ, and XYZtoRGB24 in tif_luv.h 
     * LogLuvDecode32 was set up to work on only 1 row at a time, so I added the
     * outer loop for (r = 0; r < rowsToDo; r++)
     * After the decoding, note that the luminance is then a sign bit followed
     * by a 15-bit integer.  Then follows an 8 bit ue.  Finally an 8 bit ve. I convert this to RGB.
     */
     // The following copyright notice appears in the original code:
    /*
     * Copyright (c) 1997 Greg Ward Larson
     * Copyright (c) 1997 Silicon Graphics, Inc.
     *
     * Permission to use, copy, modify, distribute, and sell this software and 
     * its documentation for any purpose is hereby granted without fee, provided
     * that (i) the above copyright notices and this permission notice appear in
     * all copies of the software and related documentation, and (ii) the names of
     * Sam Leffler, Greg Larson and Silicon Graphics may not be used in any
     * advertising or publicity relating to the software without the specific,
     * prior written permission of Sam Leffler, Greg Larson and Silicon Graphics.
     * 
     * THE SOFTWARE IS PROVIDED "AS-IS" AND WITHOUT WARRANTY OF ANY KIND, 
     * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY 
     * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  
     * 
     * IN NO EVENT SHALL SAM LEFFLER, GREG LARSON OR SILICON GRAPHICS BE LIABLE
     * FOR ANY SPECIAL, INCIDENTAL, INDIRECT OR CONSEQUENTIAL DAMAGES OF ANY KIND,
     * OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
     * WHETHER OR NOT ADVISED OF THE POSSIBILITY OF DAMAGE, AND ON ANY THEORY OF 
     * LIABILITY, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 
     * OF THIS SOFTWARE.
     */

    private int LogLuv32Decompresser(byte dataOut[], byte dataIn[], int bytesToRead, int rowsToDo) {
        int nPixels = xDim;
        int inPosition = 0;
        int i;
        int rc;
        byte by;
        int row;
        int m;
        int LogL;
        int Le;
        final double M_LN2 = 0.69314718055994530942;
        final double UVSCALE = 410.0;
        double L;
        double u;
        double v;
        double s;
        double x;
        double y;
        double xyz[] = new double[3];
        double r;
        double g;
        double b;
        byte dataTemp[] = new byte[4 * rowsToDo * xDim];
  

        /* get each byte string */
        for (row = 0; row < rowsToDo; row++) {
            for (m = 0; m < 4; m++) {
                for (i = 0; i < nPixels && bytesToRead > 0; ) {
                    if ((dataIn[inPosition] & 0xff) >= 128) { // run
                        rc = (dataIn[inPosition++] & 0xff) + (2 - 128);   
                        by = dataIn[inPosition++];
                        bytesToRead -= 2;
                        while ((rc-- > 0) && (i < nPixels)) {
                            dataTemp[4*row*xDim + 4*i + m] = by;
                            i++;
                        }
                    } // if (dataIn[inPosition] >= 128)
                    else { // non-run
                        rc = dataIn[inPosition++] & 0xff;
                        while ((--bytesToRead > 0) && (rc-- > 0) && (i < nPixels)) {
                            dataTemp[4*row*xDim + 4*i + m] =  dataIn[inPosition++];
                            i++;
                        }
                    } // else non-run
                } // for (i = 0; i < nPixels && bytesToRead > 0; )
            } // for (m = 0; m < 4; m++)
        } // for (row = 0; row < rowsToDo; row++)
        for (i = 0; i < xDim * rowsToDo; i++) {
            LogL = (((dataTemp[4*i] << 8) & 0xff00) | (dataTemp[4*i+1] & 0xff));
            if (((LogL & 0x8000) != 0) || (LogL == 0)) {
                // Don't allow negative luminance
                dataOut[3*i] = 0;
                dataOut[3*i + 1] = 0;
                dataOut[3*i + 2] = 0;
            }
            else {
                Le = LogL & 0x7fff;
                L = Math.exp(M_LN2/256.0*(Le + 0.5) - M_LN2*64.0);
                u = 1./UVSCALE * ((dataTemp[4*i + 2] & 0xff) + 0.5);
                v = 1./UVSCALE * ((dataTemp[4*i + 3] & 0xff) + 0.5);
                s = 1./(6.0 * u - 16.0*v + 12.0);
                x = 9.0 * u * s;
                y = 4.0 * v * s;
                xyz[0] = x/y * L;
                xyz[1] = L;
                xyz[2] = (1.0 - x - y)/y * L;
                /* assume CCIR-709 primaries */
                r =  2.690*xyz[0] + -1.276*xyz[1] + -0.414*xyz[2];
                g = -1.022*xyz[0] +  1.978*xyz[1] +  0.044*xyz[2];
                b =  0.061*xyz[0] + -0.224*xyz[1] +  1.163*xyz[2];
                                /* assume 2.0 gamma for speed */
                /* could use integer sqrt approx., but this is probably faster */
                if (r <= 0) {
                    dataOut[3*i] = 0;
                }
                else if (r >= 1.0) {
                    dataOut[3*i] = (byte)255;
                }
                else {
                    dataOut[3*i] = (byte)(256.0* Math.sqrt(r));
                }
                if (g <= 0) {
                    dataOut[3*i + 1] = 0;
                }
                else if (g >= 1.0) {
                    dataOut[3*i + 1] = (byte)255;
                }
                else {
                    dataOut[3*i + 1] = (byte)(256.0* Math.sqrt(g));
                }
                if (b <= 0) {
                    dataOut[3*i + 2] = 0;
                }
                else if (b >= 1.0) {
                    dataOut[3*i + 2] = (byte)255;
                }
                else {
                    dataOut[3*i + 2] = (byte)(256.0* Math.sqrt(b));
                }
            }
          }
        return 3 * xDim * rowsToDo;
    }
    
    // ThunderScanDecompresser is a port and modification of C++ code in 
    // tif_thunder.c in tif-4.0.0.alpha\libtiff
    // The following copyright notice appears in the original code
    /*
     * Copyright (c) 1988-1997 Sam Leffler
     * Copyright (c) 1991-1997 Silicon Graphics, Inc.
     *
     * Permission to use, copy, modify, distribute, and sell this software and 
     * its documentation for any purpose is hereby granted without fee, provided
     * that (i) the above copyright notices and this permission notice appear in
     * all copies of the software and related documentation, and (ii) the names of
     * Sam Leffler and Silicon Graphics may not be used in any advertising or
     * publicity relating to the software without the specific, prior written
     * permission of Sam Leffler and Silicon Graphics.
     * 
     * THE SOFTWARE IS PROVIDED "AS-IS" AND WITHOUT WARRANTY OF ANY KIND, 
     * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY 
     * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  
     * 
     * IN NO EVENT SHALL SAM LEFFLER OR SILICON GRAPHICS BE LIABLE FOR
     * ANY SPECIAL, INCIDENTAL, INDIRECT OR CONSEQUENTIAL DAMAGES OF ANY KIND,
     * OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
     * WHETHER OR NOT ADVISED OF THE POSSIBILITY OF DAMAGE, AND ON ANY THEORY OF 
     * LIABILITY, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 
     * OF THIS SOFTWARE.
     */
    
    /*
     * ThunderScan uses an encoding scheme designed for
     * 4-bit pixel values.  Data is encoded in bytes, with
     * each byte split into a 2-bit code word and a 6-bit
     * data value.  The encoding gives raw data, runs of
     * pixels, or pixel values encoded as a delta from the
     * previous pixel value.  For the latter, either 2-bit
     * or 3-bit delta values are used, with the deltas packed
     * into a single byte.
     */


    private int ThunderScanDecompresser(byte dataOut[], byte dataIn[], int bytesToRead, int rowsToDo) {
        final int THUNDER_CODE = 0xc0; /* Mask for 2 bit data code word */
        // Code values
        final int THUNDER_RUN = 0x00; /* Run of pixels with encoded count */
        final int THUNDER_2BITDELTAS = 0x40; /* 3 pixels with encoded 2-bit deltas */
        final int DELTA2_SKIP = 2; /* skip code for 2 bit deltas */
        final int THUNDER_3BITDELTAS = 0x80; /* 2 pixels with encoded 3-bit deltas */
        final int DELTA3_SKIP = 4; /* skip code for 3-bit deltas */
        final int THUNDER_RAW = 0xc0; /* raw data encoded */
        int twobitdeltas[] = new int[]{0, 1, 0, -1};
        int threebitdeltas[] = new int[]{0, 1, 2, 3, 0, -3, -2, -1};
        int inPosition = 0;
        int outPosition = 0;
        int outPositionLoop;
        int bytesToGenerate = ((xDim + 1)/2) * rowsToDo;
        byte lastPixel;
        int nPixels;
        int n;
        int delta;
        byte dataPack[] = new byte[(dataOut.length+1)/2];
        while (bytesToGenerate > 0) {
            lastPixel = 0;
            nPixels = 0;
            outPositionLoop = outPosition;
            while (bytesToRead > 0 && nPixels < xDim) {
                n = (dataIn[inPosition++] & 0xff);
                bytesToRead--;
                switch(n & THUNDER_CODE) {
                    case THUNDER_RUN:            /* pixel run */
                        // Replicate the last pixel n1 times,
                        // where n1 is the low-order 6 bits
                        if ((nPixels & 1) != 0) {
                            dataPack[outPositionLoop] |= lastPixel;  
                            lastPixel = dataPack[outPositionLoop++];
                            nPixels++;
                            n--;
                        }
                        else {
                            lastPixel |= (lastPixel << 4);
                        }
                        nPixels += n;
                        if (nPixels < xDim) {
                            for (; n > 0; n -= 2) {
                                dataPack[outPositionLoop++] = lastPixel;
                            }
                        }
                        if (n == -1) {
                            dataPack[--outPositionLoop] &= 0xf0;
                        }
                        lastPixel &= 0xf;
                        break;
                    case THUNDER_2BITDELTAS:            /* 2-bit deltas */
                        delta = (n >> 4) & 3;
                        if (delta != DELTA2_SKIP) {
                            lastPixel = (byte)((lastPixel + twobitdeltas[delta]) & 0xf);
                            if ((nPixels++ & 1) != 0) {
                                dataPack[outPositionLoop++] |= lastPixel;
                            }
                            else {
                                dataPack[outPositionLoop] = (byte)(lastPixel << 4);
                            }
                        } // if (delta != DELTA2_SKIP)
                        delta = (n >> 2) & 3;
                        if (delta != DELTA2_SKIP) {
                            lastPixel = (byte)((lastPixel + twobitdeltas[delta]) & 0xf);
                            if ((nPixels++ & 1) != 0) {
                                dataPack[outPositionLoop++] |= lastPixel;
                            }
                            else {
                                dataPack[outPositionLoop] = (byte)(lastPixel << 4);
                            }
                        } // if (delta != DELTA2_SKIP)
                        delta = n & 3;
                        if (delta != DELTA2_SKIP) {
                            lastPixel = (byte)((lastPixel + twobitdeltas[delta]) & 0xf);
                            if ((nPixels++ & 1) != 0) {
                                dataPack[outPositionLoop++] |= lastPixel;
                            }
                            else {
                                dataPack[outPositionLoop] = (byte)(lastPixel << 4);
                            }
                        } // if (delta != DELTA2_SKIP)
                        break;
                    case THUNDER_3BITDELTAS:       /* 3-bit deltas */
                        delta = (n >> 3) & 7;
                        if (delta != DELTA3_SKIP) {
                            lastPixel = (byte)((lastPixel + threebitdeltas[delta]) & 0xf);
                            if ((nPixels++ & 1) != 0) {
                                dataPack[outPositionLoop++] |= lastPixel;
                            }
                            else {
                                dataPack[outPositionLoop] = (byte)(lastPixel << 4);
                            }
                        } // if (delta != DELTA3_SKIP)
                        delta = n & 7;
                        if (delta != DELTA3_SKIP) {
                            lastPixel = (byte)((lastPixel + threebitdeltas[delta]) & 0xf);
                            if ((nPixels++ & 1) != 0) {
                                dataPack[outPositionLoop++] |= lastPixel;
                            }
                            else {
                                dataPack[outPositionLoop] = (byte)(lastPixel << 4);
                            }
                        } // if (delta != DELTA3_SKIP)
                        break;
                    case THUNDER_RAW:    /* raw data */
                        lastPixel = (byte)(n & 0xf);
                        if ((nPixels++ & 1) != 0) {
                            dataPack[outPositionLoop++] |= lastPixel;
                        }
                        else {
                            dataPack[outPositionLoop] = (byte)(lastPixel << 4);
                        }
                        break;
                }
            } // while (bytesToRead > 0 && nPixels < xDim)
            bytesToGenerate -= (xDim+1)/2;
            outPosition += (xDim + 1)/2;
        } // while (bytesToGenerate > 0)
        for (n = 0; n < outPosition; n++) {
            dataOut[2*n] = (byte)((dataPack[n] >> 4) & 0xf);
            dataOut[2*n + 1] = (byte)(dataPack[n] & 0xf);
        }
        return xDim * rowsToDo;
    }

    
    // Modified from readImage routine TIFFClassFFactory.java in package uk.co.mmscomputing.imageio.tiff
    private int fax34Decompresser(byte dataOut[], byte dataIn[]) {
        ByteArrayInputStream bais;
        ModHuffmanInputStream mhis;
        int resultLength;
        
        bais = new ByteArrayInputStream(dataIn);
        if (fillOrder == 1) { // baseline tiff default
            try {
                mhis = getDecoder(new BitSwapInputStream(bais));  
            }
            catch(IOException e) {
                MipavUtil.displayError("IOException on getDecoder(new BitSwapInputStream(bais)");
                return -1;
            }
        } // if (fillOrder == 1)
        else { // fillOrder == 2 fax devices usually code low pixel col low bit positions
            try {
                mhis = getDecoder(bais);
            }
            catch(IOException e) {
                MipavUtil.displayError("IOException on getDecoder(bais)");
                return -1;
            }
        }
        // photometric == 0 corresponds to WhiteIsZero
        try {
            resultLength = copyin(dataOut, mhis, xDim, fileInfo.getPhotometric() != 0);
        }
        catch (IOException e) {
            MipavUtil.displayError("IOException error on copyin");
            return -1;
        }
        return resultLength;
    }
    
    //  Modified from copyin routine in TIFFClassFFactory.java in package uk.co.mmscomputing.imageio.tiff
    private int copyin(byte[] imgdata, ModHuffmanInputStream is,int width,boolean invert)throws IOException{
        RLEBitInputStream rlis;
        rlis = new RLEBitInputStream(is);
        rlis.setInvert(invert);

        if((width&0x0007)==0){
          byte[] buf=new byte[width>>3];int off=0,len=0;
          while(true){
            rlis.resetToStartCodeWord();                    // start next line with white
            is.readEOL();                                   // set settings for a new line
            try{
              len=rlis.read(buf);                           // read one image line
              if(len==-1){break;}                           // end of page
              System.arraycopy(buf,0,imgdata,off,len);      // copy line to image buffer
            }catch(ModHuffmanInputStream.ModHuffmanCodingException mhce){
              MipavUtil.displayError("copyin:\n\t"+mhce);
            }
            off+=len;
          }
          return off;
        }else{
          byte[] buf=new byte[(width+7)>>3];int off=0,len=0,ecw=8-(width&0x0007),bits;
          while(true){
            rlis.resetToStartCodeWord();                    // start next line with white
            is.readEOL();                                   // set settings for a new line
            try{
              len=rlis.read(buf,0,buf.length-1);            // read one image line
              if(len==-1){break;}                           // end of page
              bits=rlis.readBits(7,ecw);
              buf[len]=(byte)bits;
              System.arraycopy(buf,0,imgdata,off,len+1);    // copy line to image buffer
            }catch(ModHuffmanInputStream.ModHuffmanCodingException mhce){
              MipavUtil.displayError("copyin:\n\t"+mhce);
            }
            off+=len+1;
          }
          return off;
        }
      }
    
    // A copy of RLEBitInputStream.java in package uk.co.mmscomputing.io
    public class RLEBitInputStream extends FilterInputStream{

        private int           rlen;                     // the run length
        private int       ccw;                      // the current code word
        private boolean   invert=false;

        public RLEBitInputStream(InputStream in){
          super(in);
          resetToStartCodeWord();
        }

        public void resetToStartCodeWord(){ccw=1;rlen=0;}
        public void setInvert(boolean invert){this.invert=invert;}

        public int read(byte[] b,int off,int len)throws IOException{
          for(int i=0;i<len;i++){
            int v=read();
            if(v==-1){return (i==0)?-1:i;}
            b[off+i]=(byte)v;      
          }
          return len;
        }

        public int readBits(int end,int start)throws IOException{
          int b=0;
          for(int i=end;i>=start;i--){
            while(rlen==0){
              rlen=in.read();                         // can be 32 bit number
              if(rlen==-1){return -1;}                // end of stream
              ccw=(ccw+1)&0x01;                       // change current code word
            }
            rlen--;
            if(ccw!=1){b|=(1<<i);}
          }
          return (invert)?((~b)&0x00FF):b;
        }

        public int read()throws IOException{
          int b=0;
          for(int i=7;i>=0;i--){
            while(rlen==0){
              rlen=in.read();                         // can be 32 bit number
              if(rlen==-1){return -1;}                // end of stream
              ccw=(ccw+1)&0x01;                       // change current code word
            }
            rlen--;
            if(ccw!=1){b|=(1<<i);}
          }
          return (invert)?((~b)&0x00FF):b;
        }
      }


    //  Modified from getDecoder routine in TIFFClassFFactory.java in package uk.co.mmscomputing.imageio.tiff
    private ModHuffmanInputStream getDecoder(InputStream is) throws IOException {
        if (fax3Compression){

          if(!group3_2D_Coding){
            return new ModHuffmanInputStream(is);
          }else{
            return new ModREADInputStream(is, xDim);
          }
        }
        else if (fax4Compression) {
          // Assume: T6Options == 0 => compressed MMR coding
          return new ModModREADInputStream(is, xDim);
        }
        else {
            throw new IOException("Not required fax3 or fax4 compression");
        }
      }

    //  A copy of BitInputStream.java in package uk.co.mmscomputing.io
    public class BitInputStream extends FilterInputStream{
        private     int     buf;
        private     int     bitsAvail;
        private     boolean eof;
        protected   int     count=0;
        protected boolean nextByteMoreSignificant;

        public BitInputStream(InputStream in,boolean nbms){
          super(in);
          bitsAvail=0;
          buf=0;
          eof=false;
          count=0;
          nextByteMoreSignificant=nbms;
        }

        public BitInputStream(InputStream in){
          this(in,true);
        }

        public void setNextByteMoreSignificant(boolean nextByteMoreSignificant){
          this.nextByteMoreSignificant=nextByteMoreSignificant;
        }

        public void reset()throws IOException{
          super.reset();
          bitsAvail=0;
          buf=0;
          eof=false;
          count=0;
        }

        public int availableBits(){
          if(eof && (bitsAvail<=0)){return -1;}
          return bitsAvail;
        }

        public void skipPadding(int bits)throws IOException{
          clrBits(bitsAvail%bits);
        }

        public int readBit()throws IOException{     // read one bit
          if(eof && (bitsAvail<=0)){ return -1; }
          needBits(1);          
          int bit=getBits(1);
          clrBits(1);
          return bit;
        }

        public int readBits(int bitcount)throws IOException{    // read "count" bit
          if(eof && (bitsAvail<=0)){ return -1; }
          if(bitcount==0){ return 0; }
          needBits(bitcount);           
          int bits=getBits(bitcount);
          clrBits(bitcount);
          return bits;
        }

        protected int cbCount()throws IOException{      //  callback for subclasses
          return in.read();
        }

        public void needBits(int bitcount)throws IOException{
          // Assert(bitcount<32);
          while((eof==false)&&(bitsAvail<bitcount)){
            int b=cbCount();
            if(b==-1){eof=true;break;}
            b&=0x00FF;
            count++;
            buf|=(nextByteMoreSignificant)?(b<<bitsAvail):(b<<(24-bitsAvail));
            bitsAvail += 8;
          }
        }

        protected void clrBits(int bitcount){
          bitsAvail -= bitcount;
          if(nextByteMoreSignificant){buf>>>=bitcount;}else{buf<<=bitcount;}
        }

        protected int getBits(int bitcount){
          bitcount=32-bitcount;
          return (nextByteMoreSignificant)?((buf<<bitcount)>>>bitcount):(buf>>>bitcount);
        }

        /*public static void main(String[] argv){

        // bytes 1001 1001 1000 1000 1001 1001

        // nextByteMoreSignificant=true bitcount=9    // i.e. GIF LZW

        // code1 = 0 1001 1001
        // code2 = 0 1100 0100
        // code3 = 0 0010 0110

        // nextByteMoreSignificant=false bitcount=9   // i.e. TIFF LZW, JPEG

        // code1 = 1 0011 0011
        // code2 = 0 0010 0010
        // code3 = 0 1100 1000

          try{
            byte[] buf=new byte[]{(byte)0x99,(byte)0x88,(byte)0x99};
            ByteArrayInputStream  bais=new ByteArrayInputStream(buf);
            BitInputStream is=new BitInputStream(bais,true);

            System.out.println("\nnextByteMoreSignificant = true");
            int code;
            while((code=is.readBits(9))!=-1){System.out.println("code= "+Integer.toBinaryString(code));}


            bais=new ByteArrayInputStream(buf);
            is=new BitInputStream(bais,false);
            
            System.out.println("\nnextByteMoreSignificant = false");
            while((code=is.readBits(9))!=-1){System.out.println("code= "+Integer.toBinaryString(code));}
          }catch(Exception e){
            e.printStackTrace();
          }
        }*/
      }
    
    //  A copy of ModHuffmantable.java in package uk.co.mmscomputing.io
    public interface ModHuffmanTable{

        static final int WHITE   =0;
        static final int BLACK   =1;
        static final int EOL     =2;

//        static final int INVALID =3;

        static final int MAXCHUNK=2560;
        static final int EOLCW=0x0800;

        static final int makeUpWhite[][] = {    //  make-up white codes
        { 0x001b,   64,  5 },
        { 0x0009,  128,  5 },
        { 0x003a,  192,  6 },
        { 0x0076,  256,  7 },
        { 0x006c,  320,  8 },
        { 0x00ec,  384,  8 },
        { 0x0026,  448,  8 },
        { 0x00a6,  512,  8 },
        { 0x0016,  576,  8 },
        { 0x00e6,  640,  8 },
        { 0x0066,  704,  9 },
        { 0x0166,  768,  9 },
        { 0x0096,  832,  9 },
        { 0x0196,  896,  9 },
        { 0x0056,  960,  9 },
        { 0x0156, 1024,  9 },
        { 0x00d6, 1088,  9 },
        { 0x01d6, 1152,  9 },
        { 0x0036, 1216,  9 },
        { 0x0136, 1280,  9 },
        { 0x00b6, 1344,  9 },
        { 0x01b6, 1408,  9 },
        { 0x0032, 1472,  9 },
        { 0x0132, 1536,  9 },
        { 0x00b2, 1600,  9 },
        { 0x0006, 1664,  6 },
        { 0x01b2, 1728,  9 },

        { 0x0080, 1792,  11},
        { 0x0180, 1856,  11},
        { 0x0580, 1920,  11},
        { 0x0480, 1984,  12},
        { 0x0c80, 2048,  12},
        { 0x0280, 2112,  12},
        { 0x0a80, 2176,  12},
        { 0x0680, 2240,  12},
        { 0x0e80, 2304,  12},
        { 0x0380, 2368,  12},
        { 0x0b80, 2432,  12},
        { 0x0780, 2496,  12},
        { 0x0f80, 2560,  12},
        { EOLCW,  0,     12},               // end of line code 
         };
        static final int termWhite[][] = {  // terminating white codes
            { 0x00ac,    0,  8 },
            { 0x0038,    1,  6 },
            { 0x000e,    2,  4 },
            { 0x0001,    3,  4 },
            { 0x000d,    4,  4 },
            { 0x0003,    5,  4 },
            { 0x0007,    6,  4 },
            { 0x000f,    7,  4 },
            { 0x0019,    8,  5 },
            { 0x0005,    9,  5 },
            { 0x001c,   10,  5 },
            { 0x0002,   11,  5 },
            { 0x0004,   12,  6 },
            { 0x0030,   13,  6 },
            { 0x000b,   14,  6 },
            { 0x002b,   15,  6 },
            { 0x0015,   16,  6 },
            { 0x0035,   17,  6 },
            { 0x0072,   18,  7 },
            { 0x0018,   19,  7 },
            { 0x0008,   20,  7 },
            { 0x0074,   21,  7 },
            { 0x0060,   22,  7 },
            { 0x0010,   23,  7 },
            { 0x000a,   24,  7 },
            { 0x006a,   25,  7 },
            { 0x0064,   26,  7 },
            { 0x0012,   27,  7 },
            { 0x000c,   28,  7 },
            { 0x0040,   29,  8 },
            { 0x00c0,   30,  8 },
            { 0x0058,   31,  8 },
            { 0x00d8,   32,  8 },
            { 0x0048,   33,  8 },
            { 0x00c8,   34,  8 },
            { 0x0028,   35,  8 },
            { 0x00a8,   36,  8 },
            { 0x0068,   37,  8 },
            { 0x00e8,   38,  8 },
            { 0x0014,   39,  8 },
            { 0x0094,   40,  8 },
            { 0x0054,   41,  8 },
            { 0x00d4,   42,  8 },
            { 0x0034,   43,  8 },
            { 0x00b4,   44,  8 },
            { 0x0020,   45,  8 },
            { 0x00a0,   46,  8 },
            { 0x0050,   47,  8 },
            { 0x00d0,   48,  8 },
            { 0x004a,   49,  8 },
            { 0x00ca,   50,  8 },
            { 0x002a,   51,  8 },
            { 0x00aa,   52,  8 },
            { 0x0024,   53,  8 },
            { 0x00a4,   54,  8 },
            { 0x001a,   55,  8 },
            { 0x009a,   56,  8 },
            { 0x005a,   57,  8 },
            { 0x00da,   58,  8 },
            { 0x0052,   59,  8 },
            { 0x00d2,   60,  8 },
            { 0x004c,   61,  8 },
            { 0x00cc,   62,  8 },
            { 0x002c,   63,  8 },
          };

        static final int makeUpBlack[][] = {    //  make-up black codes
            { 0x03c0,   64, 10 },
            { 0x0130,  128, 12 },
            { 0x0930,  192, 12 },
            { 0x0da0,  256, 12 },
            { 0x0cc0,  320, 12 },
            { 0x02c0,  384, 12 },
            { 0x0ac0,  448, 12 },
            { 0x06c0,  512, 13 },
            { 0x16c0,  576, 13 },
            { 0x0a40,  640, 13 },
            { 0x1a40,  704, 13 },
            { 0x0640,  768, 13 },
            { 0x1640,  832, 13 },
            { 0x09c0,  896, 13 },
            { 0x19c0,  960, 13 },
            { 0x05c0, 1024, 13 },
            { 0x15c0, 1088, 13 },
            { 0x0dc0, 1152, 13 },
            { 0x1dc0, 1216, 13 },
            { 0x0940, 1280, 13 },
            { 0x1940, 1344, 13 },
            { 0x0540, 1408, 13 },
            { 0x1540, 1472, 13 },
            { 0x0b40, 1536, 13 },
            { 0x1b40, 1600, 13 },
            { 0x04c0, 1664, 13 },
            { 0x14c0, 1728, 13 },

            { 0x0080, 1792,  11},
            { 0x0180, 1856,  11},
            { 0x0580, 1920,  11},
            { 0x0480, 1984,  12},
            { 0x0c80, 2048,  12},
            { 0x0280, 2112,  12},
            { 0x0a80, 2176,  12},
            { 0x0680, 2240,  12},
            { 0x0e80, 2304,  12},
            { 0x0380, 2368,  12},
            { 0x0b80, 2432,  12},
            { 0x0780, 2496,  12},
            { 0x0f80, 2560,  12},
            { EOLCW,  0,     12},               // end of line code 
          };
          static final int termBlack[][] = {    //  terminating black codes
            { 0x03b0,    0, 10 },
            { 0x0002,    1,  3 },
            { 0x0003,    2,  2 },
            { 0x0001,    3,  2 },
            { 0x0006,    4,  3 },
            { 0x000c,    5,  4 },
            { 0x0004,    6,  4 },
            { 0x0018,    7,  5 },
            { 0x0028,    8,  6 },
            { 0x0008,    9,  6 },
            { 0x0010,   10,  7 },
            { 0x0050,   11,  7 },
            { 0x0070,   12,  7 },
            { 0x0020,   13,  8 },
            { 0x00e0,   14,  8 },
            { 0x0030,   15,  9 },
            { 0x03a0,   16, 10 },
            { 0x0060,   17, 10 },
            { 0x0040,   18, 10 },
            { 0x0730,   19, 11 },
            { 0x00b0,   20, 11 },
            { 0x01b0,   21, 11 },
            { 0x0760,   22, 11 },
            { 0x00a0,   23, 11 },
            { 0x0740,   24, 11 },
            { 0x00c0,   25, 11 },
            { 0x0530,   26, 12 },
            { 0x0d30,   27, 12 },
            { 0x0330,   28, 12 },
            { 0x0b30,   29, 12 },
            { 0x0160,   30, 12 },
            { 0x0960,   31, 12 },
            { 0x0560,   32, 12 },
            { 0x0d60,   33, 12 },
            { 0x04b0,   34, 12 },
            { 0x0cb0,   35, 12 },
            { 0x02b0,   36, 12 },
            { 0x0ab0,   37, 12 },
            { 0x06b0,   38, 12 },
            { 0x0eb0,   39, 12 },
            { 0x0360,   40, 12 },
            { 0x0b60,   41, 12 },
            { 0x05b0,   42, 12 },
            { 0x0db0,   43, 12 },
            { 0x02a0,   44, 12 },
            { 0x0aa0,   45, 12 },
            { 0x06a0,   46, 12 },
            { 0x0ea0,   47, 12 },
            { 0x0260,   48, 12 },
            { 0x0a60,   49, 12 },
            { 0x04a0,   50, 12 },
            { 0x0ca0,   51, 12 },
            { 0x0240,   52, 12 },
            { 0x0ec0,   53, 12 },
            { 0x01c0,   54, 12 },
            { 0x0e40,   55, 12 },
            { 0x0140,   56, 12 },
            { 0x01a0,   57, 12 },
            { 0x09a0,   58, 12 },
            { 0x0d40,   59, 12 },
            { 0x0340,   60, 12 },
            { 0x05a0,   61, 12 },
            { 0x0660,   62, 12 },
            { 0x0e60,   63, 12 },
          };
        }

    //  A copy of ModHuffmanInputStream.java in package uk.co.mmscomputing.io
    public class ModHuffmanInputStream extends BitInputStream implements ModHuffmanTable{

        protected int state;

        public ModHuffmanInputStream(InputStream in){
          super(in);
          state=WHITE;
        }

        public void skipPadding(int bits)throws IOException{  // TIFFImageReader (Class B MH)
          super.skipPadding(bits);
          state=WHITE;
        }

        public void syncWithEOL()throws IOException{          // SFFImageReader, TIFFImageReader (Class F T4 MH)
          if(state!=EOL){
            needBits(12);
            while(availableBits()>=12){
              if(getBits(12)==EOLCW){
                clrBits(12);break;
              }
              clrBits(1);needBits(12);
            }
          }
          state=WHITE;
        }

        public void readEOL()throws IOException{
          syncWithEOL();
        }

        public int getState(){return state;}
        public int getColour(int colwhite){return (state==BLACK)?~colwhite:colwhite;}

        public int read()throws IOException{        
          if(state == WHITE){  state=BLACK;    return read(makeUpWhite,12,termWhite,8);
          }else{               state=WHITE;    return read(makeUpBlack,13,termBlack,12);
          }
        }

        public int read(byte[] b)throws IOException{
          throw new IOException(getClass().getName()+".read:\n\tInternal Error. Cannot read whole byte array with this stream !!!");
        }

        public int read(byte[] b,int off,int len)throws IOException{
          throw new IOException(getClass().getName()+".read:\n\tInternal Error. Cannot read whole byte array with this stream !!!");
        }

        private int read(int[][] makeUp,int maxmakeUp,int[][] term,int maxterm)throws IOException{
          needBits(maxterm);
          int len=findToken(term);                     // read terminating code
          if(len>=0){return len;}
          int runlen=0;
          needBits(maxmakeUp);                         // expect make-up code now
          len=findToken(makeUp);                       // read make-up code
          if(len==0){state=EOL;return 0;}
          if(len>=0){                                  // found make-up code
            while(len==MAXCHUNK){                      // read 2560 codes
              runlen+=MAXCHUNK;
              needBits(maxmakeUp);
              len=findToken(makeUp);
            }
            if(len>=0){runlen+=len;}
            needBits(maxterm);
          }
          len=findToken(term);                         // read terminating code
          if(len>=0){return runlen+len;}
          return checkEOL();
        }

        protected int findToken(int[][] table){
          for(int i=0; i<table.length; i++) {
            int[] entry=table[i];
            int bits=getBits(entry[2]);
            if(entry[0]==bits){
              clrBits(entry[2]);
              return entry[1];
            }
          }
          return -1;
        }

        protected int checkEOL()throws IOException{
          int bits;
          needBits(12);
          while(availableBits()>=12){
            bits=getBits(12);
            if(bits==EOLCW){state=EOL;clrBits(12);return 0;}
            if(bits!=0){throw new ModHuffmanCodingException(getClass().getName()+".checkEOL:\n\tCoding error: End of line code is missing.");}
            clrBits(1);needBits(12);
          }
          return -1;                                           // eof
        }

        public class ModHuffmanCodingException extends IOException{
          public ModHuffmanCodingException(String msg){
            super(msg);
          }
        }

        /*public static void main(String[] argv){
          try{
//          byte[] buf=new byte[]{0x06,0x25,(byte)0xD0,0x01};  1728=1704+24

//          1728 white standard G3 fax line => B2 59 01

            byte[] buf=new byte[]{(byte)0xB2,0x59,0x01};
            ByteArrayInputStream  bais=new ByteArrayInputStream(buf);
            ModHuffmanInputStream mhis=new ModHuffmanInputStream(bais);

            int runlen;
            while((runlen=mhis.read())!=-1){
              System.out.println("runlen= "+runlen);
            }
          }catch(Exception e){
            e.printStackTrace();
          }
        }*/
      }
    
    //  A copy of ModREADTable.java in package uk.co.mmscomputing.io
    public interface ModREADTable{
        static final int P    =0;
        static final int H    =1;

        static final int VL3  =2;
        static final int VL2  =3;
        static final int VL1  =4;
        static final int V0   =5;
        static final int VR1  =6;
        static final int VR2  =7;
        static final int VR3  =8;

        static final int HX   =9;
        static final int EOFB =10;

        static final int codes[][] = {
        { 0x0001,  V0,   1 },
        { 0x0002, VL1,   3 },
        { 0x0004,   H,   3 },
        { 0x0006, VR1,   3 },
        { 0x0008,   P,   4 },
        { 0x0010, VL2,   6 },
        { 0x0030, VR2,   6 },
        { 0x0020, VL3,   7 },
        { 0x0060, VR3,   7 },
        { 0x00800800, EOFB, 24},  // two EOLs T.6
        };
       }

    //  A copy of ModModREADInputStream.java in package uk.co.mmscomputing.io
    public class ModModREADInputStream extends ModHuffmanInputStream implements ModREADTable{

        // T.6 MMR Input Stream. How to use, see ..imageio.tiff.TIFFImageReader

        private   int[]    refline=null;
        private   int[]    codeline=null;

        private   int      clindex=0;                     // index into code line

        private   int      a0;                            // pixel position in scan line
        private   int      b1;                            // index into reference line
        private   int      maxb1;                         // maximum available run length values from reference line
        private   int      code;                          // next READ code
        private   int      width;                         // page width

        public ModModREADInputStream(InputStream in,int width)throws IOException{
          super(in);
          this.width=width;
          refline  =new int[width+3];
          codeline =new int[width+3];
          init(width);
        }

        protected void init(int width)throws IOException{
          clindex     =1;
          codeline[0] =width;                             // setup imaginary white line
          getREADCode();                                  // read first READ code
        }

        protected void initNewLine()throws IOException{
          if(code==HX){readHorizontalMode2();}            // HX is left over when H coded at end of line and second run is zero.

          int[] swap=refline;refline=codeline;codeline=swap;

          maxb1=clindex;                                  // set max possible code index from last line.
          refline[maxb1]  =width;                         // set max possible line width
          refline[maxb1+1]=width;
          refline[maxb1+2]=width;

          clindex=0;b1=0;a0=0;
        }

        public void readEOL()throws IOException{          // remark: T.6 does not use EOL codes
          initNewLine();                                  //         but we need to set up our buffers
        }

        protected int getREADCode()throws IOException{
          needBits(24);
          code=findToken(codes);
          return code;
        }

        private void setB1(){                             // find b1 so that a0 < b1 and ref line and code line runlength have opposite colour
          if(a0==0){                                      
            b1=0;
          }else{
            while((0<b1)&&(a0<refline[b1-1])){b1--;}
            while((b1<maxb1)&&(refline[b1]<=a0)){b1++;}
          }
          if((b1&0x0001)!=(clindex&0x0001)){b1++;}        // want opposite colour
        }

        private int readPassMode()throws IOException{
          int rl=0,len;
          do{
            setB1();                                      // find b1>a0 and opposite colour
            len =refline[b1+1]-a0;                        // b2 - a0
            a0 +=len;
            rl +=len;
          }while(getREADCode()==P);
          rl+=read2D();                                   // colour in pass mode never changes, hence need to add another run
          return rl;
        }

        private int readHorizontalMode1()throws IOException{
          state=((clindex&0x01)==0)?WHITE:BLACK;          // set right 'colour' table in ModHuffmanInputStream
          int rl=super.read();                            // read first mod huffman code
          a0+=rl;
          codeline[clindex++]=a0;
          code=HX;                                        // signal to read second ModHuffman code during next read()
          return rl;
        }

        private int readHorizontalMode2()throws IOException{
          int rl=super.read();                            // read second mod huffman code
          a0+=rl;
          codeline[clindex++]=a0;
          getREADCode();
          return rl;
        }

        private int readVerticalMode()throws IOException{
          setB1();                                        // find b1>a0 and opposite colour

          int offset = code-V0;
          int a1     = refline[b1]+offset;                // a1 = reference line position +- offset
          int rl     = a1-a0;

          a0=a1;
          codeline[clindex++]=a0;
          getREADCode();
          return rl;
        }   

        protected int read2D()throws IOException{
          switch(code){
          case P:    return readPassMode();
          case H:    return readHorizontalMode1();
          case V0:
          case VL1:case VL2:case VL3:
          case VR1:case VR2:case VR3:
                     return readVerticalMode();
          case HX:   return readHorizontalMode2();
          case EOFB:                                      // TIFF F : end of strip
          default:   return -1;
          }
        }

        protected int read1D()throws IOException{
          int rl=super.read();
          a0+=rl;
          codeline[clindex++]=a0;
          return rl;
        }

        public int read()throws IOException{
          return read2D();                            // remark: T.6 does only use READ codes, no reference line
        }
      }
    
    //  A copy of ModREADInputStream.java in package uk.co.mmscomputing.io
    public class ModREADInputStream extends ModModREADInputStream{

        // T.4 MR Input Stream. How to use, see ..imageio.tiff.TIFFImageReader

        private   boolean  isReferenceLine=false;

        public ModREADInputStream(InputStream in,int width)throws IOException{
          super(in,width);
        }

        protected void init(int width)throws IOException{
        }

        public void readEOL()throws IOException{          // TIFFImageReader (Class F T4 MR)
          initNewLine();                                  // initialise for new line scan
          syncWithEOL();                                  // read EOL code
          isReferenceLine=(readBit()==1);                 // 1 = one-dimensional 0 = two-dimensional
          if(!isReferenceLine){
            getREADCode();
          }
        }

        public int read()throws IOException{
          return (isReferenceLine)?read1D():read2D();
        }
      }

    //  A copy of BitSwapTable.java in package uk.co.mmscomputing.io
    public interface BitSwapTable{

        public byte[] bitSwapTable={ 
          0x0,0xffffff80,0x40,0xffffffc0,0x20,0xffffffa0,0x60,0xffffffe0,
          0x10,0xffffff90,0x50,0xffffffd0,0x30,0xffffffb0,0x70,0xfffffff0,
          0x8,0xffffff88,0x48,0xffffffc8,0x28,0xffffffa8,0x68,0xffffffe8,
          0x18,0xffffff98,0x58,0xffffffd8,0x38,0xffffffb8,0x78,0xfffffff8,
          0x4,0xffffff84,0x44,0xffffffc4,0x24,0xffffffa4,0x64,0xffffffe4,
          0x14,0xffffff94,0x54,0xffffffd4,0x34,0xffffffb4,0x74,0xfffffff4,
          0xc,0xffffff8c,0x4c,0xffffffcc,0x2c,0xffffffac,0x6c,0xffffffec,
          0x1c,0xffffff9c,0x5c,0xffffffdc,0x3c,0xffffffbc,0x7c,0xfffffffc,
          0x2,0xffffff82,0x42,0xffffffc2,0x22,0xffffffa2,0x62,0xffffffe2,
          0x12,0xffffff92,0x52,0xffffffd2,0x32,0xffffffb2,0x72,0xfffffff2,
          0xa,0xffffff8a,0x4a,0xffffffca,0x2a,0xffffffaa,0x6a,0xffffffea,
          0x1a,0xffffff9a,0x5a,0xffffffda,0x3a,0xffffffba,0x7a,0xfffffffa,
          0x6,0xffffff86,0x46,0xffffffc6,0x26,0xffffffa6,0x66,0xffffffe6,
          0x16,0xffffff96,0x56,0xffffffd6,0x36,0xffffffb6,0x76,0xfffffff6,
          0xe,0xffffff8e,0x4e,0xffffffce,0x2e,0xffffffae,0x6e,0xffffffee,
          0x1e,0xffffff9e,0x5e,0xffffffde,0x3e,0xffffffbe,0x7e,0xfffffffe,
          0x1,0xffffff81,0x41,0xffffffc1,0x21,0xffffffa1,0x61,0xffffffe1,
          0x11,0xffffff91,0x51,0xffffffd1,0x31,0xffffffb1,0x71,0xfffffff1,
          0x9,0xffffff89,0x49,0xffffffc9,0x29,0xffffffa9,0x69,0xffffffe9,
          0x19,0xffffff99,0x59,0xffffffd9,0x39,0xffffffb9,0x79,0xfffffff9,
          0x5,0xffffff85,0x45,0xffffffc5,0x25,0xffffffa5,0x65,0xffffffe5,
          0x15,0xffffff95,0x55,0xffffffd5,0x35,0xffffffb5,0x75,0xfffffff5,
          0xd,0xffffff8d,0x4d,0xffffffcd,0x2d,0xffffffad,0x6d,0xffffffed,
          0x1d,0xffffff9d,0x5d,0xffffffdd,0x3d,0xffffffbd,0x7d,0xfffffffd,
          0x3,0xffffff83,0x43,0xffffffc3,0x23,0xffffffa3,0x63,0xffffffe3,
          0x13,0xffffff93,0x53,0xffffffd3,0x33,0xffffffb3,0x73,0xfffffff3,
          0xb,0xffffff8b,0x4b,0xffffffcb,0x2b,0xffffffab,0x6b,0xffffffeb,
          0x1b,0xffffff9b,0x5b,0xffffffdb,0x3b,0xffffffbb,0x7b,0xfffffffb,
          0x7,0xffffff87,0x47,0xffffffc7,0x27,0xffffffa7,0x67,0xffffffe7,
          0x17,0xffffff97,0x57,0xffffffd7,0x37,0xffffffb7,0x77,0xfffffff7,
          0xf,0xffffff8f,0x4f,0xffffffcf,0x2f,0xffffffaf,0x6f,0xffffffef,
          0x1f,0xffffff9f,0x5f,0xffffffdf,0x3f,0xffffffbf,0x7f,0xffffffff,
        };

      /*
        static{
          bitSwapTable=new byte[256];
          int s=0;
          for(int t=0;t<256;t++){
            bitSwapTable[t]=(byte)s;
            int u=256;
            do{
              u>>=1;
              s^=u; 
            }while((s^u)>s);
          }
        }
      */
      }

    //  A copy of BitSwapInputStream.java in package uk.co.mmscomputing.io
    public class BitSwapInputStream extends FilterInputStream implements BitSwapTable{

        public BitSwapInputStream(InputStream in)throws IOException{
          super(in);
        }

        public int read()throws IOException{
          int sample=in.read();
          if(sample==-1){ return -1;}
          sample=bitSwapTable[sample];
          return sample&0x000000FF;
        }

        public int read(byte[] b)throws IOException{
          int len=in.read(b);
          for(int i=0;i<len;i++){
            b[i]=bitSwapTable[b[i]&0x000000FF];
          }
          return len;
        }

        public int read(byte[] b, int off, int len)throws IOException{
          len=in.read(b,off,len);
          for(int i=0;i<len;i++){
            b[off+i]=bitSwapTable[b[off+i]&0x000000FF];
          }
          return len;
        }
      }
    
    // A copy of JPEGConstants.java from package uk.co.mmscomputing.imageio.jpeg
    public interface JPEGConstants{

        // [1] p.32

        // 'byte stuffing'  = 0x0000;

        static final int TEM    = 0x0001;          // Temporary private use in arithmetic coding

        // Reserved 0x0002 - 0x00BF

        static final int SOF0   = 0x00C0;          // Start of Frame BaseLine sequential DCT, non differential, Huffman coding
        static final int SOF1   = 0x00C1;          // Start of Frame Extended sequential DCT, non differential, Huffman coding
        static final int SOF2   = 0x00C2;          // Start of Frame Progressive DCT, non differential, Huffman coding
        static final int SOF3   = 0x00C3;          // Start of Frame Lossless sequential, non differential, Huffman coding
        static final int DHT    = 0x00C4;          // Define Huffman Tables
        static final int SOF5   = 0x00C5;          // Start of Frame Sequential DCT, differential, Huffman coding
        static final int SOF6   = 0x00C6;          // Start of Frame Progressive DCT, differential, Huffman coding
        static final int SOF7   = 0x00C7;          // Start of Frame Lossless sequential, differential, Huffman coding

        static final int JPG    = 0x00C8;          // JPEG Extensions
        static final int SOF9   = 0x00C9;          // Start of Frame Extended sequential DCT, non differential, arithmetic coding
        static final int SOF10  = 0x00CA;          // Start of Frame Progressive DCT, non differential, arithmetic coding
        static final int SOF11  = 0x00CB;          // Start of Frame Lossless sequential, non differential, arithmetic coding
        static final int DAC    = 0x00CC;          // Define Arithmetic Conditioning
        static final int SOF13  = 0x00CD;          // Start of Frame Extended sequential DCT, differential, arithmetic coding
        static final int SOF14  = 0x00CE;          // Start of Frame Progressive DCT, differential, arithmetic coding
        static final int SOF15  = 0x00CF;          // Start of Frame Lossless sequential, differential, arithmetic coding


        static final int RST0   = 0x00D0;          // Restart Interval Termination
        static final int RST1   = 0x00D1;          // Restart Interval Termination
        static final int RST2   = 0x00D2;          // Restart Interval Termination
        static final int RST3   = 0x00D3;          // Restart Interval Termination
        static final int RST4   = 0x00D4;          // Restart Interval Termination
        static final int RST5   = 0x00D5;          // Restart Interval Termination
        static final int RST6   = 0x00D6;          // Restart Interval Termination
        static final int RST7   = 0x00D7;          // Restart Interval Termination

        static final int SOI    = 0x00D8;          // Start Of Image
        static final int EOI    = 0x00D9;          // End Of Image
        static final int SOS    = 0x00DA;          // Start of Scan
        static final int DQT    = 0x00DB;          // Define Quantization Tables
        static final int DNL    = 0x00DC;          // Define Number of Lines
        static final int DRI    = 0x00DD;          // Define Restart Interval
        static final int DHP    = 0x00DE;          // Define hierarchical progression
        static final int EXP    = 0x00DF;          // Expand reference component(s)


        static final int APP0   = 0x00E0;          // Application JFIF, EXIF ?
        static final int APP1   = 0x00E1;          // Application 
        static final int APP2   = 0x00E2;          // Application 
        static final int APP3   = 0x00E3;          // Application 
        static final int APP4   = 0x00E4;          // Application 
        static final int APP5   = 0x00E5;          // Application 
        static final int APP6   = 0x00E6;          // Application 
        static final int APP7   = 0x00E7;          // Application 

        static final int APP8   = 0x00E8;          // Application 
        static final int APP9   = 0x00E9;          // Application 
        static final int APP10  = 0x00EA;          // Application 
        static final int APP11  = 0x00EB;          // Application 
        static final int APP12  = 0x00EC;          // Application 
        static final int APP13  = 0x00ED;          // Application 
        static final int APP14  = 0x00EE;          // Application Adobe ?
        static final int APP15  = 0x00EF;          // Application 

        static final int JPG0   = 0x00F0;          // JPEG Extensions
        static final int JPG1   = 0x00F1;          // JPEG Extensions
        static final int JPG2   = 0x00F2;          // JPEG Extensions
        static final int JPG3   = 0x00F3;          // JPEG Extensions
        static final int JPG4   = 0x00F4;          // JPEG Extensions
        static final int JPG5   = 0x00F5;          // JPEG Extensions
        static final int JPG6   = 0x00F6;          // JPEG Extensions
        static final int JPG7   = 0x00F7;          // JPEG Extensions
        static final int JPG8   = 0x00F8;          // JPEG Extensions
        static final int JPG9   = 0x00F9;          // JPEG Extensions
        static final int JPG10  = 0x00FA;          // JPEG Extensions
        static final int JPG11  = 0x00FB;          // JPEG Extensions
        static final int JPG12  = 0x00FC;          // JPEG Extensions
        static final int JPG13  = 0x00FD;          // JPEG Extensions
        static final int COM    = 0x00FE;          // Comment

        static final int MARK   = 0x00FF;

        static final int DCTSize      =  8;
        static final int DCTBlockSize = 64;

        static final int[] ZigZagTable={           // [1] p.30
           0, 1, 5, 6,14,15,27,28,
           2, 4, 7,13,16,26,29,42,
           3, 8,12,17,25,30,41,43,
           9,11,18,24,31,40,44,53,
          10,19,23,32,39,45,52,54,
          20,22,33,38,46,51,55,60,
          21,34,37,47,50,56,59,61,
          35,36,48,49,57,58,62,63
        };

        static final int[] IZigZagTable={
           0, 1, 8,16, 9, 2, 3,10,
          17,24,32,25,18,11, 4, 5,
          12,19,26,33,40,48,41,34,
          27,20,13, 6, 7,14,21,28,
          35,42,49,56,57,50,43,36,
          29,22,15,23,30,37,44,51,
          58,59,52,45,38,31,39,46,
          53,60,61,54,47,55,62,63
        };

        public static final byte[] HLDCTable={      // Huffman Luminance DC Coefficients [1] p.158
          0,1,5,1,1,1,1,1,1,0,0,0,0,0,0,0,          // BITS
          0,1,2,3,4,5,6,7,8,9,10,11                 // HUFFVAL
        };

        public static final byte[] HCDCTable={      // Huffman chrominance DC Coefficients [1] p.158
          0,3,1,1,1,1,1,1,1,1,1,0,0,0,0,0,          // BITS
          0,1,2,3,4,5,6,7,8,9,10,11                 // HUFFVAL
        };

        public static final byte[] HLACTable={      // Huffman Luminance AC Coefficients [1] p.158
          0,2,1,3,3,2,4,3,5,5,4,4,0,0,1,125,        // BITS
          1,2,3,0,4,17,5,18,33,49,65,6,19,81,97,7,  // HUFFVAL
          34,113,20,50,-127,-111,-95,8,35,66,-79,-63,21,82,-47,-16,
          36,51,98,114,-126,9,10,22,23,24,25,26,37,38,39,40,
          41,42,52,53,54,55,56,57,58,67,68,69,70,71,72,73,
          74,83,84,85,86,87,88,89,90,99,100,101,102,103,104,105,
          106,115,116,117,118,119,120,121,122,-125,-124,-123,-122,-121,-120,-119,
          -118,-110,-109,-108,-107,-106,-105,-104,-103,-102,-94,-93,-92,-91,-90,-89,
          -88,-87,-86,-78,-77,-76,-75,-74,-73,-72,-71,-70,-62,-61,-60,-59,
          -58,-57,-56,-55,-54,-46,-45,-44,-43,-42,-41,-40,-39,-38,-31,-30,
          -29,-28,-27,-26,-25,-24,-23,-22,-15,-14,-13,-12,-11,-10,-9,-8,
          -7,-6
        };

        public static final byte[] HCACTable={      // Huffman chrominance AC Coefficients [1] p.159
          0,2,1,2,4,4,3,4,7,5,4,4,0,1,2,119,        // BITS
          0,1,2,3,17,4,5,33,49,6,18,65,81,7,97,113, // HUFFVAL
          19,34,50,-127,8,20,66,-111,-95,-79,-63,9,35,51,82,-16,
          21,98,114,-47,10,22,36,52,-31,37,-15,23,24,25,26,38,
          39,40,41,42,53,54,55,56,57,58,67,68,69,70,71,72,
          73,74,83,84,85,86,87,88,89,90,99,100,101,102,103,104,
          105,106,115,116,117,118,119,120,121,122,-126,-125,-124,-123,-122,-121,
          -120,-119,-118,-110,-109,-108,-107,-106,-105,-104,-103,-102,-94,-93,-92,-91,
          -90,-89,-88,-87,-86,-78,-77,-76,-75,-74,-73,-72,-71,-70,-62,-61,
          -60,-59,-58,-57,-56,-55,-54,-46,-45,-44,-43,-42,-41,-40,-39,-38,
          -30,-29,-28,-27,-26,-25,-24,-23,-22,-14,-13,-12,-11,-10,-9,-8,
          -7,-6
        };

        public static final int[] LQT={             // [1]p.143 in zigzag order
           16, 11, 12, 14, 12, 10, 16, 14,
           13, 14, 18, 17, 16, 19, 24, 40,
           26, 24, 22, 22, 24, 49, 35, 37,
           29, 40, 58, 51, 61, 60, 57, 51,
           56, 55, 64, 72, 92, 78, 64, 68,
           87, 69, 55, 56, 80,109, 81, 87,
           95, 98,103,104,103, 62, 77,113,
          121,112,100,120, 92,101,103, 99
        };

        public static final int[] CQT={
          17,18,18,24,21,24,47,26,
          26,47,99,66,56,66,99,99,
          99,99,99,99,99,99,99,99,
          99,99,99,99,99,99,99,99,
          99,99,99,99,99,99,99,99,
          99,99,99,99,99,99,99,99,
          99,99,99,99,99,99,99,99,
          99,99,99,99,99,99,99,99
        };

        public static final int[] LQT2={            
           8, 6, 6, 7, 6, 5, 8, 7,
           7, 7, 9, 9, 8,10,12,20,
          13,12,11,11,12,25,18,19,
          15,20,29,26,31,30,29,26,
          28,28,32,36,46,39,32,34,
          44,35,28,28,40,55,41,44,
          48,49,52,52,52,31,39,57,
          61,56,50,60,46,51,52,50
        };

        public static final int[] CQT2={
           9, 9, 9,12,11,12,24,13,
          13,24,50,33,28,33,50,50,
          50,50,50,50,50,50,50,50,
          50,50,50,50,50,50,50,50,
          50,50,50,50,50,50,50,50,
          50,50,50,50,50,50,50,50,
          50,50,50,50,50,50,50,50,
          50,50,50,50,50,50,50,50
        };
      }

    // A copy of TIFFYCbCrInputStream from package uk.co.mmscomputing.imageio.tiff.
    public class TIFFYCbCrInputStream extends IntFilterInputStream{

      protected double LumaRed,LumaGreen,LumaBlue;
      protected double RfBY,RfBCb,RfBCr,RfWY,RfWCb,RfWCr;

      private   double RCr,BCb,GY,GB,GR;

      public TIFFYCbCrInputStream(IntFilterInputStream in)throws IOException{
        super(in);
      }

      public void setColourCoefficients(double LumaRed,double LumaGreen,double LumaBlue){
        this.LumaRed=LumaRed;this.LumaGreen=LumaGreen;this.LumaBlue=LumaBlue;

        RCr=2.0-2.0*LumaRed;              // R = Cr * ( 2 - 2 * LumaRed ) + Y
        BCb=2.0-2.0*LumaBlue;             // B = Cb * ( 2 - 2 * LumaBlue ) + Y
        GY =1.0/LumaGreen;                // G = ( Y - LumaBlue * B - LumaRed * R ) / LumaGreen
        GB =-LumaBlue/LumaGreen;
        GR =-LumaRed /LumaGreen;
      }

      public double getLumaRed(){return LumaRed;}
      public double getLumaGreen(){return LumaGreen;}
      public double getLumaBlue(){return LumaBlue;}

      public void setRfBWY(double black,double white){RfBY=black;RfWY=white;}
      public void setRfBWCb(double black,double white){RfBCb=black;RfWCb=white;}
      public void setRfBWCr(double black,double white){RfBCr=black;RfWCr=white;}

      public int read()throws IOException{
        throw new IOException(getClass().getName()+".read:\t\nInternal Error: Please use read(int[] buf,int off,int len).");
      }

      protected int convert(int YCbCr)throws IOException{
        double Y,Cb,Cr;

        Y =((YCbCr>>16)&0x000000FF);
        Cb=((YCbCr>> 8)&0x000000FF);
        Cr=((YCbCr    )&0x000000FF);

        // FullRangeValue = (code - ReferenceBlack) * CodingRange / (ReferenceWhite - ReferenceBlack);

        Y =(Y -RfBY )*255.0/(RfWY -RfBY );
        Cb=(Cb-RfBCb)*127.0/(RfWCb-RfBCb);
        Cr=(Cr-RfBCr)*127.0/(RfWCr-RfBCr);

        // R = Cr * ( 2 - 2 * LumaRed  ) + Y
        // B = Cb * ( 2 - 2 * LumaBlue ) + Y
        // G = ( Y - LumaBlue * B - LumaRed * R ) / LumaGreen

        int R =(int)Math.round(Y + RCr * Cr);             if(R<0){R=0;}else if(R>255){R=255;}
        int B =(int)Math.round(Y + BCb * Cb);             if(B<0){B=0;}else if(B>255){B=255;}
        int G =(int)Math.round(Y * GY + GB * B + GR * R); if(G<0){G=0;}else if(G>255){G=255;}

        return (R<<16)|(G<<8)|B;
      }

      public int read(int[] buf, int off, int len)throws IOException{
        len=((IntFilterInputStream)in).read(buf,off,len);
        for(int i=0;i<len;i++){
          buf[off+i]=convert(buf[off+i]);
        }
        return len;
      }
    }


    
    // A copy of JPEGHuffmanInputStream.java from package uk.co.mmscomputing.imageio.jpeg
    public class JPEGHuffmanInputStream extends InputStream{

        protected int[] BITS=new int[16];             // 16-byte list containing number of Huffman codes of each length
        protected int[] HUFFVAL;
        protected int[] HUFFSIZE;
        protected int[] HUFFCODE;

        protected int[] VALPTR  = new int[16];
        protected int[] MINCODE = new int[16];
        protected int[] MAXCODE = new int[16];

        protected JPEGBitInputStream in;

        public JPEGHuffmanInputStream(JPEGBitInputStream in,InputStream tables)throws IOException{
          this.in=in;
          initialize(tables);
        }

        public JPEGHuffmanInputStream(InputStream tables)throws IOException{
          this.in=null;
          initialize(tables);
        }

        public void setInputStream(JPEGBitInputStream in)throws IOException{
          this.in=in;
        }

        private void initialize(InputStream tables)throws IOException{
          int LASTK=readTableData(tables);
          generateSizeTable(LASTK);
          generateCodeTable(LASTK);
          generateDecoderTable();
        }

        private int readTableData(InputStream tables)throws IOException{ // [1] p.40, p.50
          int m=0;
          for(int i=0;i<16;i++){                      // 16-byte list containing number of Huffman codes of each length
            BITS[i]=tables.read();                    // System.out.println(Integer.toString((BITS[i]<128)?BITS[i]:BITS[i]-256))+",");
            m+=BITS[i];
          }
          HUFFVAL=new int[m];
          for(int i=0;i<m;i++){
            HUFFVAL[i]=tables.read();                 // System.out.println(Integer.toString((HUFFVAL[i]<128)?HUFFVAL[i]:HUFFVAL[i]-256)+",");
          }
          return m;
        }

        private void generateSizeTable(int LASTK){     // [1] p.51
          HUFFSIZE=new int[LASTK+1];
          int k=0;
          for(int i=0;i<16;i++){
            for(int j=0;j<BITS[i];j++){
              HUFFSIZE[k++]=i+1;
            }
          }
          HUFFSIZE[k]=0;
        }

        private void generateCodeTable(int LASTK){    // [1] p.52
          HUFFCODE=new int[LASTK];
          int k=0,code=0;
          int si=HUFFSIZE[0];
          while(true){
            do{
              HUFFCODE[k]=code;
              code++;k++;
            }while(HUFFSIZE[k]==si);
            if(HUFFSIZE[k]==0){break;}
            do{
              code<<=1;si++;
            }while(HUFFSIZE[k]!=si);
          }
        }

        private void generateDecoderTable(){          // [1] p.108
          int j=0;
          for(int i=0;i<16;i++){
            if(BITS[i]==0){
              MAXCODE[i]=-1;
            }else{
              VALPTR[i]=j;
              MINCODE[i]=HUFFCODE[j];
              j+=BITS[i]-1;
              MAXCODE[i]=HUFFCODE[j];
              j++;
            }
          }
        }

        public String toString(){
          String s="\n"+getClass().getName()+"\n";

          s+="byte[] BITS={";
          for(int i=0;i<BITS.length;i++){             // 16-byte list containing number of Huffman codes of each length
            s+=Integer.toString((BITS[i]<128)?BITS[i]:BITS[i]-256)+",";
//            s+=Integer.toHexString(BITS[i])+",";
          }
          s+="};\n";
          s+="byte[] HUFFVAL={";
          for(int i=0;i<HUFFVAL.length;i++){
            s+=Integer.toString((HUFFVAL[i]<128)?HUFFVAL[i]:HUFFVAL[i]-256)+",";
//            s+=Integer.toHexString(HUFFVAL[i])+",";
          }
          s+="};\n";
          return s;
        }

        public int readBits(int bitSize)throws IOException{
          int V = in.readBits(bitSize);
          int Vt=1<<(bitSize-1);                      // [1] p.105 extend
          if(V<Vt){                                   // if V should be negative (bit T is zero, hence V is smaller then Vt)
            Vt=(-1<<bitSize)+1;                       // sign extend: put 1 bits in front of the T bits of V
            V+=Vt;
          }
          return V;
        }

        private int code,index;

        public void restart()throws IOException{      // Encountered RST marker. This can happen in middle of read()
          code=0;index=0;                             // Hence need to declare code and index as class variables.
        }

        public int read()throws IOException{          // [1] p.109 decode
          int b;
          index=0;
          code=in.readBit();
          if(code==-1){return -1;}
          while(code>MAXCODE[index++]){
            b=in.readBit();                          
            if(b==-1){return -1;}
            code=(code<<1)|b;
          }
          int j=VALPTR[--index];
          j+=code-MINCODE[index];
          return HUFFVAL[j];
        }
      }
    
    // A copy of JPEGACInputStream.java from package uk.co.mmscomputing.imageio.jpeg
    public class JPEGACInputStream extends InputStream implements JPEGConstants{

        protected JPEGHuffmanInputStream in;
        protected int[]    qt;                            // quantization table

        protected int      count;
        protected int[]    buffer=new int[DCTBlockSize];

        public JPEGACInputStream(JPEGHuffmanInputStream in,int[] qt){
          this.in=in;
          this.qt=qt;
          count=DCTBlockSize;
        }

        public void restart()throws IOException{          // Call at beginning of restart interval
          in.restart();
          count=DCTBlockSize;                             // count should be DCTBlockSize !
        }

        public void fillBuffer()throws IOException{       
          int K=1,RS,SSSS,RRRR;                           // [1] p.106 decode
          for(int i=1;i<DCTBlockSize;i++){buffer[i]=0;}   // zero ZZ

          while(K<DCTBlockSize){
            RS=in.read();
            if(RS==-1){throw new IOException(getClass().getName()+"fillBuffer:\n\tUnexpected end of file.");}
            SSSS=RS&0x000F;                               // run length of zero ac coefficients
            RRRR=(RS>>4)&0x000F;                          // bit size of next non zero ac coefficient
            if(SSSS==0){
              if(RRRR!=15){break;}                        //  0/0 => End of block (EOB)
              K+=16;                                      // 15/0 => 16 0s
            }else{                                        //  R/S
              K+=RRRR;                                    // skip over RRRR zero ac coefficients
              buffer[IZigZagTable[K]]=qt[K]*in.readBits(SSSS);// read non zero ac coefficient; [1] p.107 decodeZZ
              K++;
            }
          }
        }

        public int read()throws IOException{          
          if(count==DCTBlockSize){
            fillBuffer();
            count=1;
          }
          return buffer[count++];
        }
      }

    // A copy of JPEGDCInputStream.java from package uk.co.mmscomputing.imageio.java
    public class JPEGDCInputStream extends InputStream{

        private JPEGHuffmanInputStream in;
        private int PRED;

        public JPEGDCInputStream(JPEGHuffmanInputStream in){
          this.in=in;
          PRED=0;
        }

        public void restart()throws IOException{      // Call at beginning of restart interval
          in.restart();
          PRED=0;
        }

        public int read()throws IOException{          // [1] p.104 decode
          int T=in.read();                            
          if(T==-1){return -1;}                       
          PRED+=in.readBits(T);
          return PRED;
        }
      }

    
    // A copy of JPEGTDCTInputStream.java from package uk.co.mmscomputing.imageio.jpeg
    public class JPEGDCTInputStream extends JPEGACInputStream{

        protected JPEGDCInputStream dc;
        protected int levelshift,negclamp,posclamp;

        public JPEGDCTInputStream(JPEGHuffmanInputStream dc,JPEGHuffmanInputStream ac,int[] qt,int bps){
          super(ac,qt);
          this.dc=new JPEGDCInputStream(dc);
          levelshift=1<<(bps-1);negclamp=-levelshift;posclamp= levelshift-1;
        }

        public void restart()throws IOException{                 // Call at beginning of restart interval
          dc.restart();
          super.restart();
        }

        public int[] getBuffer(){return buffer;}

        protected void levelShift(){
          int col;
          for(int i=0;i<DCTBlockSize;i++){
            col=buffer[i];
            if(col<negclamp){       col=negclamp;                // clamp to -128 .. 127 or -2048 .. 2047
            }else if(col>posclamp){ col=posclamp;
            }
            col+=levelshift;                                     // level shift;[1] A.3.1; F.1.1.3 p.87
            buffer[i]=col;
          }
        }

        public void fillBuffer()throws IOException{       
          buffer[0]=qt[0]*dc.read();
          super.fillBuffer();
          inverseDCT(buffer);
          levelShift();
          count=0;
        }

        public int read()throws IOException{          
          if(count==DCTBlockSize){
            fillBuffer();
          }
          return buffer[count++];
        }

        protected int matr1[]=new int[DCTBlockSize];             // temp buffer; used in JPEGFastDCTInputStream as well

        public void inverseDCT(int[] buffer)throws IOException{  // The text book DCT algorithm.

          double[] coeff={1.0/Math.sqrt(2.0),1,1,1,1,1,1,1};

          double sum;
          for(int y=0;y<DCTSize;y++){
            for(int x=0;x<DCTSize;x++){
              sum = 0.0;
              for(int v=0;v<DCTSize;v++) {
                for(int u=0;u<DCTSize;u++) {
                  sum+=coeff[u]*coeff[v]*buffer[v*DCTSize+u]*Math.cos(((2.0*x+1.0)/16.0)*u*Math.PI)*Math.cos(((2.0*y+1.0)/16.0)*v*Math.PI);
                }
              }
              sum/=4.0;
              matr1[y*DCTSize+x]=((int)Math.round(sum));  
            }
          }
          System.arraycopy(matr1,0,buffer,0,DCTBlockSize);
        }
      }

    
    // A copy of JPEGFastDCTInputStream.java from package uk.co.mmscomputing.imageio.jpeg
    public class JPEGFastDCTInputStream extends JPEGDCTInputStream{

        public JPEGFastDCTInputStream(JPEGHuffmanInputStream dc,JPEGHuffmanInputStream ac,int[] qt,int bps){
          super(dc,ac,qt,bps);
        }

        static private final int CONST_BITS = 11;

        private int compute(double val){return (int)(val*(1<<CONST_BITS));}

        static private final int VAL_BITS   = 11;
        static private final int ALLBITS    = CONST_BITS + VAL_BITS;
        static private final int TWO        = CONST_BITS + 1;

        private final int C6      = compute(2.0*Math.sin(Math.PI/8.0));
        private final int C4C6    = compute(2.0*Math.sqrt(2.0)*Math.sin(Math.PI/8.0));
        private final int C4      = compute(Math.sqrt(2.0));
        private final int Q       = compute(2.0*(Math.cos(Math.PI/8.0)-Math.sin(Math.PI/8.0)));
        private final int C4Q     = compute(2.0*Math.sqrt(2.0)*(Math.cos(Math.PI/8.0)-Math.sin(Math.PI/8.0)));
        private final int R       = compute(2.0*(Math.cos(Math.PI/8.0)+Math.sin(Math.PI/8.0)));
        private final int C4R     = compute(2.0*Math.sqrt(2.0)*(Math.cos(Math.PI/8.0)+Math.sin(Math.PI/8.0)));

//        protected int matr1[]=new int[DCTBlockSize];  // declared in JPEGDCTInputStream
        protected int matr2[]=new int[DCTBlockSize];

        public void inverseDCT(int[] buffer){
          int tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7;
          int plus8, plus16, plus24, plus32, plus40, plus48, plus56;
          int co1, co2, co3, co5, co6, co7, co35, co17;
          int n1, n2, n3;
          int tmp;
          int l0 = 0, l1 = 0, l2 = 0, l3 = 0;
          int g0, g1, g2, g3;
          int i, j, p;

          for (p = j = 0; j < 64; j+=8) {
            matr1[p++] = buffer[j+0];
            matr1[p++] = buffer[j+4];
            matr1[p++] = (co2 = buffer[j+2])-(co6 = buffer[j+6]);
            matr1[p++] = co2+co6;
            matr1[p++] =-(co3=buffer[j+3])+(co5=buffer[j+5]); 
            matr1[p++] = (co17=(co1=buffer[j+1]+(co7=buffer[j+7])))-(co35=co3+co5);
            matr1[p++] = co1-co7;
            matr1[p++] = co17+co35;
          }

          for (p = i = 0; i < 8; i++) {
            switch(i) {
            case 0:
            case 1:
            case 3:
            case 7:
              tmp4 = (co3=matr1[24+i])-(co5=matr1[40+i]);
              tmp6 = (co1=matr1[ 8+i])-(co7=matr1[56+i]);
              tmp = C6 * (tmp6-tmp4);
              matr2[p++] =  matr1[i  ] << CONST_BITS;
              matr2[p++] =  matr1[32+i] << CONST_BITS;
              matr2[p++] =  ((co2=matr1[16+i])-(co6=matr1[48+i]))*C4;
              matr2[p++] =  (co2+co6) << CONST_BITS;
              matr2[p++] =  Q*tmp4-tmp;
              matr2[p++] =  ((co17=co1 + co7)-(co35=co3+co5))*C4;
              matr2[p++] =  R*tmp6-tmp;
              matr2[p++] =  (co17+co35) << CONST_BITS;
              break;
            case 2:
            case 5:
              tmp4 = (co3=matr1[24+i])-(co5=matr1[40+i]);
              tmp6 = (co1=matr1[ 8+i])-(co7=matr1[56+i]);
              tmp = C4C6 * (tmp6-tmp4);
              matr2[p++] = C4*matr1[i  ];
              matr2[p++] = C4*matr1[i+32];
              matr2[p++] = ((co2=matr1[16+i])-(co6=matr1[48+i])) << TWO;
              matr2[p++] = C4*(co2+co6);
              matr2[p++] = C4Q*tmp4-tmp;
              matr2[p++] = ((co17 =co1+co7)-(co35=co3+co5)) << TWO;
              matr2[p++] = C4R*tmp6-tmp;
              matr2[p++] = C4* (co17+co35);
              break;
            case 4:
              matr2[p++] = matr1[   i];
              matr2[p++] = matr1[32+i];
              matr2[p++] = (co2=matr1[16+i])-(co6=matr1[48+i]);
              matr2[p] = co2+co6;
              l0 = l2 = -(co3=matr1[24+i])+(co5=matr1[40+i]);
              p += 2;
              matr2[p] = (co17 =(co1=matr1[ 8+i]) + (co7=matr1[56+i]))-(co35=co3+co5);
              l3 = -( l1 = co1-co7);
              p += 2;
              matr2[p++] = co17+co35;
              break;
            case 6:
              matr2[p++] = matr1[   i];
              matr2[p++] = matr1[32+i];
              matr2[p++] = (co2=matr1[16+i])-(co6=matr1[48+i]);
              matr2[p] = co2+co6;
              l1 += (tmp4 = -(co3=matr1[24+i])+(co5=matr1[40+i]));
              l3 += tmp4;
              p += 2;
              matr2[p] = (co17 =(co1=matr1[ 8+i]) + (co7=matr1[56+i]))-(co35=co3+co5);
              l2 += (tmp6 = co1-co7);
              l0 -= tmp6;
              p += 2;
              matr2[p++] = co17+co35;
              break;
            }
          }

          g0 = C4*(l0+l1);
          g1 = C4*(l0-l1);
          g2 = l2 << TWO;
          g3 = l3 << TWO;

          matr2[36] = g0+g2;
          matr2[38] = g1+g3;
          matr2[52] = g1-g3;
          matr2[54] = g2-g0;

          tmp = C6*(matr2[32]+matr2[48]);
          matr2[32] = -Q*matr2[32]-tmp;
          matr2[48] =  R*matr2[48]-tmp;

          tmp = C6*(matr2[33] + matr2[49]);
          matr2[33] = -Q*matr2[33]-tmp;
          matr2[49] =  R*matr2[49]-tmp;

          tmp = C4C6 * (matr2[34] + matr2[50]);
          matr2[34] = -C4Q*matr2[34]-tmp;
          matr2[50] =  C4R*matr2[50]-tmp;

          tmp = C6*(matr2[35] + matr2[51]);
          matr2[35] = -Q*matr2[35]-tmp;
          matr2[51] =  R*matr2[51]-tmp;

          tmp = C4C6 * (matr2[37] + matr2[53]);
          matr2[37] = -C4Q*matr2[37]-tmp;
          matr2[53] =  C4R*matr2[53]-tmp;

          tmp = C6*(matr2[39] + matr2[55]);
          matr2[39] = -Q*matr2[39]-tmp;
          matr2[55] =  R*matr2[55]-tmp;

          for (p=i = 0; i < 8; i++,p+=8) {
            matr1[p] = (tmp4 = (n3 = matr2[p]+matr2[p+1]) + matr2[p+3]) + matr2[p+7];
            matr1[p+3] = (tmp6=n3-matr2[p+3])-(tmp7=matr2[p+4]-(tmp1=(tmp2=matr2[p+6]-matr2[p+7])-matr2[p+5]));
            matr1[p+4] = tmp6+tmp7;
            matr1[p+1] = (tmp3=(n1=matr2[p]-matr2[p+1])+(n2=matr2[p+2]-matr2[p+3]))+tmp2;
            matr1[p+2] = (tmp5=n1-n2)-tmp1;
            matr1[p+5] = tmp5+tmp1;
            matr1[p+6] = tmp3-tmp2;
            matr1[p+7] = tmp4-matr2[p+7];
          }
          plus8 = 8; plus16 = 16; plus24 = 24; plus32 = 32; plus40 = 40; plus48 = 48; plus56 = 56;
          for (p = i = 0; p < 64; p+=8) {
            buffer[p] = ((tmp4 = (n3 = matr1[i]+matr1[plus8]) +matr1[plus24]) + matr1[plus56]) >> ALLBITS;
            buffer[p+3] = ((tmp6=n3-matr1[plus24])-(tmp7=matr1[plus32++]-(tmp1=(tmp2=matr1[plus48++]-matr1[plus56])-matr1[plus40++])))>>ALLBITS;
            buffer[p+4] = (tmp6+tmp7) >> ALLBITS;
            buffer[p+1] = ((tmp3 = (n1 = matr1[i++]-matr1[plus8++])+ (n2 = matr1[plus16++]-matr1[plus24++]))+tmp2) >> ALLBITS;
            buffer[p+2] = ((tmp5 = n1-n2) -tmp1) >> ALLBITS;
            buffer[p+5] = (tmp5+tmp1) >> ALLBITS;
            buffer[p+6] = (tmp3-tmp2) >> ALLBITS;
            buffer[p+7] = (tmp4-matr1[plus56++]) >> ALLBITS;
          }
        }

        void normalize(int qt[]){   // need to normalize quantization tables; called in JPEGInputStream
          double d;

          for(int j=0;j<DCTSize;j++){
            for(int i=0;i<DCTSize;i++){
              d=(double)qt[ZigZagTable[j*DCTSize+i]];
              if((i==0)&&(j==0)){
                d/=8.0;
              }else if((i==0)||(j==0)){
                d/=8.0/Math.sqrt(2.0);
              }else{
                d/=4.0;
              }
              qt[ZigZagTable[j*DCTSize+i]]=(int)(d*(1<<VAL_BITS)*Math.cos(Math.PI*i/16.0)* Math.cos(Math.PI*j/16.0)+0.5);
            }
          }
        }
      }



    
    // A copy of JPEGComponentInputStream.java from package uk.co.mmscomputing.imageio.jpeg
    public class JPEGComponentInputStream extends InputStream{

        protected int   id,shift=0;

        protected int   height,width;
        protected int   bps,Hf,Vf,HMax,VMax,Yf,Xf;
        protected int[] qt;

        protected JPEGDCTInputStream in;

        public JPEGComponentInputStream(int id){this.id=id;}

        public int  getId(){return id;}

        public void setShift(int shift)           {this.shift=shift;}
        public void setBitsPerSample(int bps)     {this.bps=bps;}
        public void setDimensions(int h,int w)    {height=h;width=w;}
        public void setSamplingRate(int Vf,int Hf){this.Vf=Vf;this.Hf=Hf;}
        public void setMaxSamplingRate(int VMax,int HMax){
          this.VMax=VMax;
          this.HMax=HMax;
          Xf=HMax/Hf;
          Yf=VMax/Vf;
        }
        public void setQuantizationTable(int[] qt){this.qt=qt;}

        public void setHuffmanTables(JPEGHuffmanInputStream dc, JPEGHuffmanInputStream ac){
          in=new JPEGFastDCTInputStream(dc,ac,qt,bps);
//          in=new JPEGDCTInputStream(dc,ac,qt,bps);
        }

        public void restart()throws IOException{in.restart();}

        public int read()throws IOException{          
          return in.read();
        }

        // YCbCr

        protected void copyPixel(int[] buf,int off,int maxy,int maxx,int b){
          for(int y=0;y<maxy;y++){
            for(int x=0;x<maxx;x++){
//              buf[off+x]=(buf[off+x]<<8)|b;
              buf[off+x]=buf[off+x]|(b<<shift);
            }
            off+=width;
          }
        }

        protected void copyDataUnit(int[] buf,int off,int maxy,int maxx,int[] buffer)throws IOException{
          int count=0;
          int yoff=off;
          for(int y=0;y<maxy;y+=Yf){
            int x=0;
            while(x<maxx){
              copyPixel(buf,yoff+x,
                ((maxy-y)>=Yf)?Yf:maxy&(Yf-1),
                ((maxx-x)>=Xf)?Xf:maxx&(Xf-1),
                buffer[count++]
              );
              x+=Xf;
            }
            while(x<8*Xf){count++;x+=Xf;}
            yoff+=width*Yf;
          }
        }

        public void read(int[] buf,int off,int maxy,int maxx)throws IOException{
          int yoff=off,yl=0;
          for(int y=0;y<Vf;y++){
            int xoff=yoff,xl=0;
            for(int x=0;x<Hf;x++){
              in.fillBuffer();
              if((yl<maxy)&&(xl<maxx)){
                copyDataUnit(buf,xoff,
                  ((maxy-yl)>=8*Yf)?8*Yf:maxy&(8*Yf-1),
                  ((maxx-xl)>=8*Xf)?8*Xf:maxx&(8*Xf-1),
                  in.getBuffer()
                );
              }
              xoff+=8*Xf;xl+=8*Xf;
            }
            yoff+=width*8*Yf;yl+=8*Yf;
          }
        }

        // grayscale

        protected void copyPixel(byte[] buf,int off,int maxy,int maxx,int b){
          for(int y=0;y<maxy;y++){
            for(int x=0;x<maxx;x++){
              buf[off+x]=(byte)b;
            }
            off+=width;
          }
        }

        protected void copyDataUnit(byte[] buf,int off,int maxy,int maxx,int[] buffer)throws IOException{
          int count=0;
          int yoff=off;
          for(int y=0;y<maxy;y+=Yf){
            int x=0;
            while(x<maxx){
              copyPixel(buf,yoff+x,
                ((maxy-y)>=Yf)?Yf:maxy&(Yf-1),
                ((maxx-x)>=Xf)?Xf:maxx&(Xf-1),
                buffer[count++]
              );
              x+=Xf;
            }
            while(x<8*Xf){count++;x+=Xf;}
            yoff+=width*Yf;
          }
        }

        public void read(byte[] buf,int off,int maxy,int maxx)throws IOException{
          int yoff=off,yl=0;
          for(int y=0;y<Vf;y++){
            int xoff=yoff,xl=0;
            for(int x=0;x<Hf;x++){
              in.fillBuffer();
              if((yl<maxy)&&(xl<maxx)){
                copyDataUnit(buf,xoff,
                  ((maxy-yl)>=8*Yf)?8*Yf:maxy&(8*Yf-1),
                  ((maxx-xl)>=8*Xf)?8*Xf:maxx&(8*Xf-1),
                  in.getBuffer()
                );
              }
              xoff+=8*Xf;xl+=8*Xf;
            }
            yoff+=width*8*Yf;yl+=8*Yf;
          }
        }

        public String toString(){
          String s="";
          s+="id  ="+id+"\n";
          s+="bps ="+bps+"\n";
          s+="Hf  ="+Hf+"\n";
          s+="Vf  ="+Vf+"\n";
          s+="HMax  ="+HMax+"\n";
          s+="VMax  ="+VMax+"\n";
          s+="Yf  ="+Yf+"\n";
          s+="Xf  ="+Xf+"\n";
          return s;
        }
      }

    
    // A copy of IntFilterInputStream form package uk.co.mmscomputing.io.

    abstract public class IntFilterInputStream extends FilterInputStream{

      public IntFilterInputStream(InputStream in){super(in);}

      public void setIn(InputStream in){this.in=in;}

      public int read(int[] buf)throws IOException{return read(buf,0,buf.length);}

      abstract public int read(int[] buf, int off, int len)throws IOException;
    }


    
    // A copy of JPEGInputStream.java from package uk.co.mmscomputing.imageio.jpeg
    public class JPEGInputStream extends IntFilterInputStream implements JPEGConstants{

        JPEGBitInputStream in;

        protected int   bps;                         // bits per sample: DCT compression [8,12] or lossless [2..16]
        protected int   height,width;
        protected int   maxHor,maxVert;
        protected int   mcuHeight,mcuWidth,mcuRows,mcuCols;
        protected int   spp;                         // samples per pixel

        private   int[][] qts = new int[4][];        // available quantization tables

        protected JPEGHuffmanInputStream[]   dcins   = new JPEGHuffmanInputStream[4];
        protected JPEGHuffmanInputStream[]   acins   = new JPEGHuffmanInputStream[4];

        protected JPEGComponentInputStream[] compins = new JPEGComponentInputStream[4];
        protected JPEGComponentInputStream[] scanins = new JPEGComponentInputStream[4];

        protected int Ri;                            // restart segment: how many MCUs in interval;

        public JPEGInputStream(InputStream input)throws IOException{
          super(null);
          Ri=0;maxHor=0;maxVert=0;
          in=new JPEGBitInputStream(input,this);
          ((JPEGBitInputStream)in).start();
        }

        public JPEGInputStream(
            InputStream input,
            int[][] qts,
            JPEGHuffmanInputStream[] dcins,
            JPEGHuffmanInputStream[] acins
        )throws IOException{
          super(null);
          Ri=0;maxHor=0;maxVert=0;
          this.qts  =qts;
          this.dcins=dcins;
          this.acins=acins;
          in=new JPEGBitInputStream(input,this);
          ((JPEGBitInputStream)in).start();
          for(int i=0;(dcins[i]!=null)&&(i<dcins.length);i++){dcins[i].setInputStream(in);}
          for(int i=0;(acins[i]!=null)&&(i<acins.length);i++){acins[i].setInputStream(in);}
        }

        public int[][]                  getQTs()  {return qts;}
        public JPEGHuffmanInputStream[] getDCIns(){return dcins;}
        public JPEGHuffmanInputStream[] getACIns(){return acins;}

        public int getHeight(){return height;}
        public int getWidth(){ return width;}
        public int getNumComponents(){return spp;}

        protected int readIn(InputStream in)throws IOException{
          int b=in.read();
          if(b==-1){
            IOException ioe=new IOException(getClass().getName()+"readIn:\n\tUnexpected end of file.");
            ioe.printStackTrace();
            throw ioe;
          }
          return b;
        }

        public void startOfFrame(InputStream in,int mode)throws IOException{ // 0xC0,0xC1
          bps   =readIn(in);
          height=(readIn(in)<<8)|readIn(in);                        // System.out.println("Height="+height);
          width =(readIn(in)<<8)|readIn(in);                        // System.out.println("Width="+width);
          spp   =readIn(in);                                        // System.out.println("3\bspp="+spp);

          for(int i=0;i<spp;i++){
            compins[i]=new JPEGComponentInputStream(readIn(in));    // Component id can be between 0..255
            compins[i].setBitsPerSample(bps);
            compins[i].setDimensions(height,width);
            int b  = readIn(in);
            int Hi = ((b>>4)&0x0F);                                 // 1..4
            int Vi =  (b    &0x0F);                                 // 1..4

            if(spp==1){                                             // [1]p.25 order left to right and top to bottom whatever the Hi,Vi values
              maxHor=1;maxVert=1;
              compins[i].setSamplingRate(1,1);
            }else{
              if(Hi>maxHor){maxHor=Hi;}
              if(Vi>maxVert){maxVert=Vi;}
              compins[i].setSamplingRate(Vi,Hi);
            }
            compins[i].setQuantizationTable(qts[readIn(in)]);       // assign one of the available QTs to a component
//            System.out.println(compins[i].toString());
          }    
          for(int i=0;i<spp;i++){
            compins[i].setMaxSamplingRate(maxVert,maxHor);
          }

          mcuHeight = maxVert*DCTSize;
          mcuWidth  = maxHor *DCTSize;
          mcuRows   =(height+mcuHeight-1)/mcuHeight;
          mcuCols   =(width +mcuWidth -1)/mcuWidth ;
      /*
          System.out.println("maxHor="+maxHor);
          System.out.println("maxVert="+maxVert);
          System.out.println("mcuHeight="+mcuHeight);
          System.out.println("mcuWidth="+mcuWidth);
          System.out.println("mcuRows="+mcuRows);
          System.out.println("mcuCols="+mcuCols);
      */
        }

        public void defineHuffmanTables(InputStream tables)throws IOException{   // 0xC4
//          System.out.println("3\bDefine Huffman Tables");

          for(int n=0;n<8;n++){                                                  // max 8 tables possible
            int b=tables.read();
            switch((b>>4)&0x0F){                                                 // table class
            case 0:  dcins[b&0x0F]=new JPEGHuffmanInputStream(in,tables);break;  // DC table 
            case 1:  acins[b&0x0F]=new JPEGHuffmanInputStream(in,tables);break;  // AC table
            default: return;
            }
          }
        }

        public void defineArithmeticConditioning(InputStream in)throws IOException{ // 0xC8
          System.out.println("3\bDefine Arithmetic Conditioning");
        }

        public void restartIntervalTermination(int no)throws IOException{        // 0xD0 .. 0xD7; no = 0..7
          for(int c=0;c<spp;c++){scanins[c].restart();}                          // System.out.println("3\bRestart Interval Termination: no = "+no);
        }

        public void startOfImage(){                                              // 0xD8
//          System.out.println("3\bStart Of Image");
        }

        public void endOfImage(){                                                // 0xD9
//          System.out.println("3\bEnd Of Image");
        }

        public void startOfScan(InputStream in)throws IOException{               // 0xDA
          spp=readIn(in);                                                        // number of components: max 4
          for(int i=0;i<spp;i++){
            int c=readIn(in);                                                    // Component id can be between 0..255
            for(int j=0;(compins[j]!=null)&&(j<4);j++){
              if(compins[j].getId()==c){scanins[i]=compins[j];break;}
            }
            int b=readIn(in);
            scanins[i].setHuffmanTables(dcins[(b>>4)&0x0F],acins[b&0x0F]);
            switch(i){
            case 0: scanins[i].setShift(16); break;   // R        Y
            case 1: scanins[i].setShift( 8); break;   // G        Cb
            case 2: scanins[i].setShift( 0); break;   // B        Cr
            case 3: scanins[i].setShift(24); break;   // alpha
            }
          }
          int ss=readIn(in);
          int se=readIn(in);
          
          int b=readIn(in);
          int ah =((b>>4)&0x0F);
          int al = (b    &0x0F);
        }

        public void defineQuantizationTables(InputStream in)throws IOException{  // 0xDB
//          System.out.println("3\bDefine Quantization Tables");

          int[] qt;
          for(int n=0;n<4;n++){                                                  // max 4 tables
            int b=in.read();
            int t=(b>>4)&0x0F;
            int c=b&0x0F;
            switch(t){                                                           // table class
            case 0:                                                              // 8bit table
              qt=new int[64];for(int i=0;i<64;i++){qt[i]=readIn(in);}            // are in zigzag scan order [1]p.40
              break;
            case 1:                                                              // 16bit table
              qt=new int[64];for(int i=0;i<64;i++){qt[i]=(readIn(in)<<8)|readIn(in);}
              break;
            default: return;
            }
            normalize(qt); // comment this out if you want to use JPEGDCTInputStream.inverseDCT
            qts[c]=qt;
          }
        }
        
        void normalize(int qt[]){   // need to normalize quantization tables; called in JPEGInputStream
            final int VAL_BITS   = 11;
            double d;

            for(int j=0;j<DCTSize;j++){
              for(int i=0;i<DCTSize;i++){
                d=(double)qt[ZigZagTable[j*DCTSize+i]];
                if((i==0)&&(j==0)){
                  d/=8.0;
                }else if((i==0)||(j==0)){
                  d/=8.0/Math.sqrt(2.0);
                }else{
                  d/=4.0;
                }
                qt[ZigZagTable[j*DCTSize+i]]=(int)(d*(1<<VAL_BITS)*Math.cos(Math.PI*i/16.0)* Math.cos(Math.PI*j/16.0)+0.5);
              }
            }
          }

        public void defineNumberOfLines(InputStream in)throws IOException{       // 0xDC Not allowed in TIFF file
          height = (readIn(in)<<8)|readIn(in);                                      System.out.println("3\bDefine Number of Lines: height="+height);    
        }

        public void defineRestartInterval(InputStream in)throws IOException{     // 0xDD
          Ri     = (readIn(in)<<8)|readIn(in);                                   // System.out.println("3\bDefine Restart Interval: Ri = "+Ri);    
        }

        public void defineHierarchicalProgression(InputStream in)throws IOException{     // 0xDE
          throw new IOException(getClass().getName()+"defineHierarchicalProgression:\n\tDo not support 'Hierarchical Progression' mode.");
        }

        public void expandReferenceComponents(InputStream in)throws IOException{ // 0xDF
          throw new IOException(getClass().getName()+"defineHierarchicalProgression:\n\tDo not support 'expand reference component(s)'.");
        }

        protected void dump(InputStream in)throws IOException{
          int b,i=0;
          while((b=in.read())!=-1){
            System.out.println("appl["+i+"] 0x"+Integer.toHexString(b)+" "+(char)((b>=' ')?b:' ')+" "+b);i++;
          }
        }

        public void app0(InputStream in)throws IOException{dump(in);}            // 0xE0  JFIF
        public void app1(InputStream in)throws IOException{dump(in);}            // 0xE1  Exif
        public void app2(InputStream in)throws IOException{dump(in);}            // 0xE2
        public void app3(InputStream in)throws IOException{dump(in);}            // 0xE3
        public void app4(InputStream in)throws IOException{dump(in);}            // 0xE4
        public void app5(InputStream in)throws IOException{dump(in);}            // 0xE5
        public void app6(InputStream in)throws IOException{dump(in);}            // 0xE6
        public void app7(InputStream in)throws IOException{dump(in);}            // 0xE7
        public void app8(InputStream in)throws IOException{dump(in);}            // 0xE8
        public void app9(InputStream in)throws IOException{dump(in);}            // 0xE9
        public void app10(InputStream in)throws IOException{dump(in);}           // 0xEA
        public void app11(InputStream in)throws IOException{dump(in);}           // 0xEB
        public void app12(InputStream in)throws IOException{dump(in);}           // 0xEC
        public void app13(InputStream in)throws IOException{dump(in);}           // 0xED
        public void app14(InputStream in)throws IOException{dump(in);}           // 0xEE
        public void app15(InputStream in)throws IOException{dump(in);}           // 0xEF
        public void comment(InputStream in)throws IOException{dump(in);}         // 0xFE

        public int read()throws IOException{
          throw new IOException(getClass().getName()+".read():\nInternal Error: Don't support simple read().");
        }

        public int read(int[] buf, int off, int len)throws IOException{
          int y=0;
          for(int row=0;row<mcuRows;row++){
            int x=0;
            for(int col=0;col<mcuCols;col++){
              for(int c=0;c<spp;c++){
                scanins[c].read(
                  buf,
                  off+y*width+x,
                  (mcuHeight<(height-y))?mcuHeight:(height-y),
                  (mcuWidth <(width -x))?mcuWidth: (width-x)
                );
              }
              x+=mcuWidth;
            }
            y+=mcuHeight;
          }
          return len;
        }

        // call only if one component and BufferedImage has grayscale type

        public int read(byte[] buf, int off, int len)throws IOException{
          int y=0;
          for(int row=0;row<mcuRows;row++){
            int x=0;
            for(int col=0;col<mcuCols;col++){
              scanins[0].read(
                buf,
                off+y*width+x,
                (mcuHeight<(height-y))?mcuHeight:(height-y),
                (mcuWidth <(width -x))?mcuWidth: (width-x)
              );
              x+=mcuWidth;
            }
            y+=mcuHeight;
          }
          return len;
        }
      }

    
    // A copy of JPEGBitInputStream from package uk.co.mmscomputing.imageio.jpeg
    public class JPEGBitInputStream extends FilterInputStream implements JPEGConstants{

        JPEGInputStream jpeg;                        // wrapping jpeg input stream. if marked segment then call this object 
        private   int bitBuffer;                     // entropy buffer; huffman coding
        private   int bitCount;


        public JPEGBitInputStream(InputStream in,JPEGInputStream jpeg)throws IOException{
          super(in);this.jpeg=jpeg;
        }

        private int readIn()throws IOException{
          int b=in.read();
          if(b==-1){
            IOException ioe=new IOException(getClass().getName()+"readIn:\n\tUnexpected end of file.");
            ioe.printStackTrace();
            throw ioe;
          }
          return b;
        }

        protected InputStream readMarkedSegment()throws IOException{
          int    length =((readIn()<<8)|readIn())-2;
          byte[] data   = new byte[length];
          int    len    = in.read(data);

          if(len!=length){throw new IOException(getClass().getName()+".readMarkedSegment:\n\tUnexpected end of file.");}
          return new ByteArrayInputStream(data);
        }

        //  JPEG Syntax:
        //  1-coded image data
        //  2-marked segments for additional information: 
        //    a-0xFF 0x00  => 'byte stuffing' 0xFF 0x00 is interpreted as image data 0xFF
        //    b-0xFF 0xmarker
        //    c-0xFF 0xmarker 0xlenHigh 0xlenLow MarkerPayload[0xlenHigh<<8 | 0xlenLow]

        public int readCompressed()throws IOException{
          int b,marker;
          while(true){
            b=readIn();
            if(b==MARK){
              do{marker=readIn();}while(marker==MARK);                                         // [1]p.30 B.1.1.2
              switch(marker){
              case 0:    /*System.out.println("b="+Integer.toBinaryString(b));*/ return 0x00FF;// byte stuffing
              case SOF0:    // BaseLine sequential DCT, non differential, Huffman coding
              case SOF1:    // Extended sequential DCT, non differential, Huffman coding
                         jpeg.startOfFrame(readMarkedSegment(),marker&0x000F);
                break;        

              case DHT:  jpeg.defineHuffmanTables(readMarkedSegment());          break;        // Define Hufman Tables
              case DAC:  jpeg.defineArithmeticConditioning(readMarkedSegment()); break;        // Define Arithmetic Conditioning
              case RST0:case RST1:case RST2:case RST3:case RST4:case RST5:case RST6:case RST7:
                         jpeg.restartIntervalTermination(marker&0x0007);
                         bitBuffer=0;bitCount=0;
                break;
              case SOI:  jpeg.startOfImage();                                    break;        // Start Of Image        
              case EOI:  jpeg.endOfImage();                                      return -1;    // End Of Image        
              case SOS:  jpeg.startOfScan(readMarkedSegment());bitCount = 0;     break;        // Start of Scan;reset entropy decoder buffer
              case DQT:  jpeg.defineQuantizationTables(readMarkedSegment());     break;        // Define Quantization Tables
              case DNL:  jpeg.defineNumberOfLines(readMarkedSegment());          break;        // Define Number of Lines
              case DRI:  jpeg.defineRestartInterval(readMarkedSegment());        break;        // Define Restart Interval
              case DHP:  jpeg.defineHierarchicalProgression(readMarkedSegment());break;        // Define Hierarchical Progression
              case EXP:  jpeg.expandReferenceComponents(readMarkedSegment());    break;        // Expand reference component(s)

              case APP0: jpeg.app0(readMarkedSegment());                         break;        // Application i.e. JFIF
              case APP1: jpeg.app1(readMarkedSegment());                         break;        // Application i.e. Exif
              case APP2: jpeg.app2(readMarkedSegment());                         break;        // Application
              case APP3: jpeg.app3(readMarkedSegment());                         break;        // Application
              case APP4: jpeg.app4(readMarkedSegment());                         break;        // Application
              case APP5: jpeg.app5(readMarkedSegment());                         break;        // Application
              case APP6: jpeg.app6(readMarkedSegment());                         break;        // Application
              case APP7: jpeg.app7(readMarkedSegment());                         break;        // Application
              case APP8: jpeg.app8(readMarkedSegment());                         break;        // Application
              case APP9: jpeg.app9(readMarkedSegment());                         break;        // Application
              case APP10:jpeg.app10(readMarkedSegment());                        break;        // Application
              case APP11:jpeg.app11(readMarkedSegment());                        break;        // Application
              case APP12:jpeg.app12(readMarkedSegment());                        break;        // Application
              case APP13:jpeg.app13(readMarkedSegment());                        break;        // Application
              case APP14:jpeg.app14(readMarkedSegment());                        break;        // Application
              case APP15:jpeg.app15(readMarkedSegment());                        break;        // Application

              case COM:  jpeg.comment(readMarkedSegment());                      break;        // Comment
              default:   throw new IOException(getClass().getName()+".readCompressed:\n\tUnknown marker = "+Integer.toHexString(marker));
              }
            }else{                                                                        
//            System.out.println("b="+Integer.toBinaryString(b));
              return b;                                                                   // coded image data
            }
          }
        }

        public void start()throws IOException{
          int b=readCompressed();
          if(b==-1){return;}   // no image data as such; [1] p.47 B.5 abbreviated format for table specification data; i.e. TIFF JPEGTables tag
          bitBuffer=(b<<24);
          bitCount=8;
        }

        public int readBit()throws IOException{
          if(bitCount==0){
            int b=readCompressed();
            if(b==-1){return -1;}
            bitBuffer=(b<<24);
            bitCount=8;
          }
          int bit=bitBuffer>>>31;
          bitBuffer<<=1;
          bitCount--;
          return bit;
        }

        public int readBits(int neededBits)throws IOException{
          if(neededBits==0){return 0;}
          while(bitCount<neededBits){
            int b=readCompressed();
            if(b==-1){return -1;}
            bitBuffer|=(b<<(24-bitCount));
            bitCount+=8;
          }
          int bits=bitBuffer>>>(32-neededBits);
          bitBuffer<<=neededBits;
          bitCount-=neededBits;
          return bits;
        }
      }

    
    /**
     * Passed in a LeicaSeries object, this function builds a 2d or 3d reconstruction using the Vector of filenames
     * within the series The vector has been presorted so that all files (whether red - green and then blue, or just
     * grayscale) are read in sequentially to build the ModelImage.
     *
     * @param   series  LeicaSeries series contains leica information for construction
     *
     * @throws  IOException  DOCUMENT ME!
     *
     * @return  ModelImage a 3d or 2d rgb or grayscale ModelImage
     */
    public ModelImage readLeicaSeries(LeicaSeries series) throws IOException {
        ModelImage finalImage = null;
        int[] imgExtents = null;

        // determine the destination ModelImage's extents
        imgExtents = new int[series.getResolutions().length];

        boolean doColor = (series.getChannels().length > 2);
        imgExtents[0] = series.getExtents()[0];
        imgExtents[1] = series.getExtents()[1];

        if (imgExtents.length > 2) {
            imgExtents[2] = series.getExtents()[3];
        }

        float[] imgRes = new float[imgExtents.length];
        imgRes[0] = series.getResolutions()[0];
        imgRes[1] = series.getResolutions()[1];

        if (imgRes.length > 2) {
            imgRes[2] = series.getResolutions()[2];
        }

        int[] units = new int[imgExtents.length];

        for (int i = 0; i < units.length; i++) {
            units[i] = FileInfoBase.MICROMETERS;
        }

        // Create the fileInfo and set up the extents/resolutions/units of measure
        fileInfo = new FileInfoTiff(series.getName(), "", FileUtility.TIFF);
        fileInfo.setExtents(imgExtents);
        fileInfo.setResolutions(imgRes);
        fileInfo.setUnitsOfMeasure(units);

        // if the image is color, load the files in the order of R-G-B
        if (doColor) {

            // create the final image where we will dump each concatenated rgb slice
            finalImage = new ModelImage(ModelImage.ARGB, imgExtents, series.getName());
            fileInfo.setDataType(ModelStorageBase.ARGB);

            ModelImage tempImage = null;
            Vector fileNameVector = series.getFileNames();
            int numImages = fileNameVector.size();
            String tempPath, tempDir, tempName;
            FileTiff tempTiff;
            int colorIndex = 0;
            float[] tempBuffer = new float[imgExtents[0] * imgExtents[1]];
            int alphaIndexStart = 0;


            // go through each file name in the vector, loading
            // them into the r,g, and b slot of the final image
            for (int i = 0, j = 0; i < numImages; i++) {
                colorIndex = i % 3;

                // when we get to each red channel, set start of the
                // alpha index so we can load the slices in correctly
                if (colorIndex == 0) {
                    alphaIndexStart = (j * 4 * tempBuffer.length);
                    j++;
                }

                // get the file name and directory
                tempPath = (String) fileNameVector.elementAt(i);
                tempDir = tempPath.substring(0, tempPath.lastIndexOf(File.separator) + 1);
                tempName = tempPath.substring(tempPath.lastIndexOf(File.separator) + 1, tempPath.length());

                // use FileTiff to read a single ModelImage
                tempTiff = new FileTiff(tempName, tempDir);
                tempImage = tempTiff.readImage(false, true);

                fireProgressStateChanged(((int) (((float) (i + 1) / (float) numImages) * 100f)));


                tempImage.exportData(0, tempBuffer.length, tempBuffer);
                finalImage.importRGBData((colorIndex + 1), alphaIndexStart, tempBuffer, false);
                tempImage.disposeLocal();
            }
        }
        // image is single channel
        else {

            // create the final image where we will dump each slice
            finalImage = new ModelImage(ModelImage.USHORT, imgExtents, series.getName());
            fileInfo.setDataType(ModelStorageBase.USHORT);

            for (int j = 0; j < imgExtents[2]; j++) {
                finalImage.setFileInfo((FileInfoBase) fileInfo.clone(), j);
            }

            ModelImage tempImage = null;
            Vector fileNameVector = series.getFileNames();
            int numImages = fileNameVector.size();
            String tempPath, tempDir, tempName;
            FileTiff tempTiff;
            float[] tempBuffer = new float[imgExtents[0] * imgExtents[1]];

            // go through each file name in the vector and load them into the slices
            // of the final image (single channel so one per slice)
            for (int i = 0; i < numImages; i++) {

                // get the file name and directory
                tempPath = (String) fileNameVector.elementAt(i);
                tempDir = tempPath.substring(0, tempPath.lastIndexOf(File.separator) + 1);
                tempName = tempPath.substring(tempPath.lastIndexOf(File.separator) + 1, tempPath.length());

                // use FileTiff to read a single ModelImage
                tempTiff = new FileTiff(tempName, tempDir);
                tempImage = tempTiff.readImage(false, true);

                fireProgressStateChanged(((int) (((float) (i + 1) / (float) numImages) * 100f)));

                tempImage.exportData(0, tempBuffer.length, tempBuffer);
                finalImage.importData((i * tempBuffer.length), tempBuffer, false);
                tempImage.disposeLocal();
            }
        }

        // set the fileinfos for each slice
        for (int j = 0; j < imgExtents[2]; j++) {
            finalImage.setFileInfo((FileInfoBase) fileInfo.clone(), j);
        }

        finalImage.calcMinMax();

        return finalImage;
    }

    /**
     * Accessor to set the file dir (used when reading TIFF multiFile).
     *
     * @param  fDir  file dir of image to read.
     */
    public void setFileDir(String fDir) {
        fileDir = fDir;
    }

    /**
     * Accessor to set the file name (used when reading TIFF multiFile).
     *
     * @param  fName  file name of image to read.
     */
    public void setFileName(String fName) {
        fileName = fName;
    }

    /**
     * This method writes a tiff image file.
     *
     * @param      image    image model where the data is stored.
     * @param      LUT      LUT to be saved with image if not null.
     * @param      options  options to be used to write out the image
     *
     * @exception  IOException  if there is an error writing the file.
     */
    public void writeImage(ModelImage image, ModelLUT LUT, FileWriteOptions options) throws IOException {
        int k, s, sEnd = 1, sBegin = 0;
        ModelImage tmpImage = null;
        ModelImage resultImage = null;
        int seq;
        int imgOffset;
        int nextIFD;
        int m;
        int type;
        int[] extents;
        int bufferSize;
        int bytesPerSample;
        int samplesPerPixel;
        int resolutionCount = 16; // xResolution = 2 * (4 bytes) + yResolution = 2 * (4 bytes)
        int rgbCount = 0; // Set to 6 for storage of 3 short bitsPerSample values

        // in ARGB, ARGB_USHORT, and ARGB_FLOAT
        int rgbFormat = 0; // Set to 6 for storage of 3 short sampleFormat values

        // in ARGB, ARGB_USHORT, and ARGB_FLOAT
        int intAlign = 0; // For integer data image rows must begin on integer boundaries, so set

        // intAlign = 2 if ModelStorageBase.INTEGER is used so that imgOffset is
        // always a multiple of 4.
        int zResCount = 0; // set to 8 if EchoTech zResolution field present
        int tResCount = 0; // set to 8 if EchoTech tResolution field present
        int ztEntries = 0;
        int index;
        int timeOffset = 0; // used to offset for writing one time slice of a 4D image
        boolean oneFile = true;

        // int offset = 0;
        String prefix, fileSuffix;
        int stripCount, totStripCount;

        if ((LUT != null) && !image.isColorImage()) {

            for (int i = 0; i < image.getFileInfo().length; i++) {
                image.getFileInfo()[i].setPhotometric((short) 3);
            }
        }

        this.image = image;
        this.LUT = LUT;

        try {
            extents = image.getExtents();

            if (image.getNDims() == 1) {
                throw new IOException("image.getNDims returned 1");
            }

            bufferSize = extents[0] * extents[1];

            if (image.getNDims() >= 3) {

                if (options.isMultiFile()) {
                    oneFile = false;
                    sBegin = options.getBeginSlice();
                    sEnd = options.getEndSlice() + 1;
                } else {
                    sBegin = 0;
                    sEnd = 1;
                    oneFile = true;
                }

                if (image.getNDims() == 4) {
                    timeOffset = options.getTimeSlice() * image.getExtents()[2] * bufferSize;
                }
            }

            index = fileName.indexOf(".");
            prefix = fileName.substring(0, index); // Used for setting file name
            fileSuffix = fileName.substring(index);
            zRes = -1.0;

            if ((image.getNDims() > 2) && (image.getFileInfo(0).getResolutions().length > 2)) {
                zRes = (double) (image.getFileInfo(0).getResolutions()[2]);
            }

            tRes = -1.0;

            if ((image.getNDims() > 3) && (image.getFileInfo(0).getResolutions().length > 3)) {
                tRes = (double) (image.getFileInfo(0).getResolutions()[3]);
            }

            if (zRes >= 0.0) {
                zResCount = 8;
                ztEntries++;
            }

            if (tRes >= 0.0) {
                tResCount = 8;
                ztEntries++;
            }

            for (s = sBegin, seq = options.getStartNumber(); s < sEnd; s++, seq++) {

                if (oneFile) {
                    file = new File(fileDir + fileName);
                    raFile = new RandomAccessFile(file, "rw");
                    raFile.setLength(0); // necessary so that if this is an overwritten file there isn't junk at the
                                         // end

                    if (options.isWritePackBit() == false) {
                        fileRW = new FileRawChunk(raFile, image.getFileInfo(0));
                    } else {
                        filePB = new FilePackBit(raFile);
                    }
                } else {

                    if (options.isSaveAs()) {

                        if (options.getDigitNumber() == 1) {
                            file = new File(fileDir + prefix + Integer.toString(seq) + fileSuffix);
                        } else if (options.getDigitNumber() == 2) {

                            if (seq < 10) {
                                file = new File(fileDir + prefix + "0" + Integer.toString(seq) + fileSuffix);
                            } else {
                                file = new File(fileDir + prefix + Integer.toString(seq) + fileSuffix);
                            }
                        } else if (options.getDigitNumber() == 3) {

                            if (seq < 10) {
                                file = new File(fileDir + prefix + "00" + Integer.toString(seq) + fileSuffix);
                            } else if (seq < 100) {
                                file = new File(fileDir + prefix + "0" + Integer.toString(seq) + fileSuffix);
                            } else {
                                file = new File(fileDir + prefix + Integer.toString(seq) + fileSuffix);
                            }
                        } else if (options.getDigitNumber() == 4) {

                            if (seq < 10) {
                                file = new File(fileDir + prefix + "000" + Integer.toString(seq) + fileSuffix);
                            } else if (seq < 100) {
                                file = new File(fileDir + prefix + "00" + Integer.toString(seq) + fileSuffix);
                            } else if (seq < 1000) {
                                file = new File(fileDir + prefix + "0" + Integer.toString(seq) + fileSuffix);
                            } else {
                                file = new File(fileDir + prefix + Integer.toString(seq) + fileSuffix);
                            }
                        }
                    } else {
                        file = new File(fileDir + image.getFileInfo(s).getFileName());
                    }

                    raFile = new RandomAccessFile(file, "rw");
                    raFile.setLength(0); // necessary so that if this is an overwritten file there isn't junk at the
                                         // end

                    if (options.isWritePackBit() == false) {
                        fileRW = new FileRawChunk(raFile, image.getFileInfo(s));
                    } else {
                        filePB = new FilePackBit(raFile);
                    }
                }

                type = image.getFileInfo(0).getDataType();

                switch (type) {

                    case ModelStorageBase.BOOLEAN:
                        bytesPerSample = 1;
                        samplesPerPixel = 1;
                        nDirEntries = (short) (11 + ztEntries);
                        rgbCount = 0;
                        intAlign = 0;
                        break;

                    case ModelStorageBase.UBYTE:
                    case ModelStorageBase.BYTE:
                        bytesPerSample = 1;
                        samplesPerPixel = 1;
                        intAlign = 0;
                        if (image.getFileInfo(0).getPhotometric() == 3) {
                            nDirEntries = (short) (13 + ztEntries); // Add one for color map

                            // Only one color map for all the images at the end of the file
                            // Only used if color map is saved with image. Pointer to the
                            // color map at the end of the file.
                            if (oneFile == true) {
                                LUTOffset = 8 +
                                            ((2 + (nDirEntries * 12) + resolutionCount + 4 + intAlign + zResCount +
                                              tResCount) * (options.getEndSlice() - options.getBeginSlice() + 1)) +
                                            (bufferSize * (options.getEndSlice() - options.getBeginSlice() + 1));
                            } else {
                                LUTOffset = 8 +
                                            (2 + (nDirEntries * 12) + resolutionCount + 4 + intAlign + zResCount +
                                             tResCount) + bufferSize;
                            }
                        } else {
                            nDirEntries = (short) (12 + ztEntries);
                        }

                        rgbCount = 0;
                        break;

                    case ModelStorageBase.USHORT:
                        bytesPerSample = 2;
                        samplesPerPixel = 1;
                        nDirEntries = (short) (12 + ztEntries);
                        rgbCount = 0;
                        intAlign = 0;
                        break;

                    case ModelStorageBase.SHORT:
                        bytesPerSample = 2;
                        samplesPerPixel = 1;
                        nDirEntries = (short) (12 + ztEntries);
                        rgbCount = 0;
                        intAlign = 0;
                        break;

                    case ModelStorageBase.INTEGER:
                    case ModelStorageBase.UINTEGER:
                    case ModelStorageBase.FLOAT:
                        bytesPerSample = 4;
                        samplesPerPixel = 1;
                        nDirEntries = (short) (12 + ztEntries);
                        rgbCount = 0;
                        intAlign = 0; // Used to be 2 but this caused problems.
                        break;

                    case ModelStorageBase.DOUBLE:
                        bytesPerSample = 8;
                        samplesPerPixel = 1;
                        nDirEntries = (short) (12 + ztEntries);
                        rgbCount = 0;
                        intAlign = 0; // Used to be 2 but this caused problems.
                        break;

                    case ModelStorageBase.ARGB:
                        bytesPerSample = 1; // since SamplesPerPixel is defined as 3 for RGB images
                        samplesPerPixel = 3;
                        nDirEntries = (short) (14 + ztEntries);
                        rgbCount = 6;
                        rgbFormat = 6;
                        intAlign = 0;
                        break;

                    case ModelStorageBase.ARGB_USHORT:
                        bytesPerSample = 2;
                        samplesPerPixel = 3;
                        nDirEntries = (short) (14 + ztEntries);
                        rgbCount = 6;
                        rgbFormat = 6;
                        intAlign = 0;
                        break;

                    default:
                        throw new IOException("Unsupported Image Type");
                }

                writeHeader();
                nextIFD = 8;
                totStripCount = 0;
                imgOffset = 0;

                if (oneFile == true) { // one file with one or more images

                    for (k = options.getBeginSlice(), m = 0; k <= options.getEndSlice(); k++, m++) {
                        fireProgressStateChanged(Math.round((float) (k - options.getBeginSlice() + 1) /
                                                                (options.getEndSlice() - options.getBeginSlice() + 1) *
                                                                100));

                        if (options.isWritePackBit()) {
                            stripCount = filePB.getStripSize(image, timeOffset + (k * bufferSize),
                                                             timeOffset + (k * bufferSize) + bufferSize);
                            totStripCount += stripCount;
                        } else {
                            stripCount = image.getSliceSize();
                        }

                        if (k == options.getEndSlice()) {
                            nextIFD = 0;
                        } else if (!options.isWritePackBit()) {
                            nextIFD += ((2 + (nDirEntries * 12) + resolutionCount + 4 + rgbCount + rgbFormat +
                                         intAlign + zResCount + tResCount) +
                                        (bufferSize * bytesPerSample * samplesPerPixel));
                        } else if (options.isWritePackBit()) {
                            nextIFD += ((2 + (nDirEntries * 12) + resolutionCount + 4 + rgbCount + rgbFormat +
                                         intAlign + zResCount + tResCount) + stripCount);
                        }

                        if (!options.isWritePackBit()) {
                            imgOffset = 8 +
                                        ((m + 1) *
                                             (2 + (nDirEntries * 12) + resolutionCount + 4 + rgbCount + rgbFormat +
                                                  intAlign + zResCount + tResCount)) +
                                        (m * bufferSize * bytesPerSample * samplesPerPixel);
                        } else if (options.isWritePackBit()) {
                            imgOffset = 8 +
                                        ((m + 1) *
                                             (2 + (nDirEntries * 12) + resolutionCount + 4 + rgbCount + rgbFormat +
                                                  intAlign + zResCount + tResCount)) + totStripCount - stripCount;
                        }

                        Preferences.debug("Image name = " + image.getImageName() + "\n", Preferences.DEBUG_FILEIO);
                        writeIFDs(image, imgOffset, nextIFD, k, stripCount, options.isWritePackBit());

                        try {

                            if (!options.isWritePackBit()) {

                                // adjust for intAlign ????
                                fileRW.writeImage(image, timeOffset + (k * bufferSize),
                                                  timeOffset + (k * bufferSize) + bufferSize, 0);
                            } else {
                                filePB.writePackBitImage(image, timeOffset + (k * bufferSize),
                                                         timeOffset + (k * bufferSize) + bufferSize);
                            }
                        } catch (IOException error) {
                            throw error;
                        }
                    }
                } else {
                    fireProgressStateChanged(Math.round((float) s / (options.getEndSlice()) * 100));

                    if (options.isWritePackBit()) {
                        stripCount = filePB.getStripSize(image, s * bufferSize, (s * bufferSize) + bufferSize);
                    } else {
                        stripCount = image.getSliceSize();
                    }

                    imgOffset = 8 +
                                (2 + (nDirEntries * 12) + resolutionCount + 4 + rgbCount + rgbFormat + intAlign +
                                 zResCount + tResCount);
                    writeIFDs(image, imgOffset, 0, s, stripCount, options.isWritePackBit());

                    try {

                        if (!options.isWritePackBit()) {
                            fileRW.writeImage(image, s * bufferSize, (s * bufferSize) + bufferSize, 0);
                        } else {
                            filePB.writePackBitImage(image, s * bufferSize, (s * bufferSize) + bufferSize);
                        }
                    } catch (IOException error) {
                        throw error;
                    }
                }

                if ((LUT != null) && ((type == ModelStorageBase.BYTE) || (type == ModelStorageBase.UBYTE)) &&
                        (image.getFileInfo(0).getPhotometric() == 3)) {
                    writeColorMap(LUTOffset);
                }
            }
        } catch (OutOfMemoryError error) {
            System.gc();
            raFile.close();

            throw error;
        }

        if (tmpImage != null) {
            image = tmpImage;
            resultImage.disposeLocal();
            resultImage = null;
        }

        raFile.close();

    }

    /**
     * Reads and decodes IFDs (Image File Directory).
     *
     * @param      fileInfo  DOCUMENT ME!
     *
     * @return     DOCUMENT ME!
     *
     * @exception  IOException  if there is an error reading the file
     */
    private boolean openIFD(FileInfoTiff fileInfo) throws IOException {
        int i;
        int iExifStart = 0;
        int i1;
        int tag;
        int type;
        int count;
        int ecount;
        long[] valueArray = new long[MAX_IFD_LENGTH];
        int value_offset;
        int nDirEntries;
        long numerator, denominator;
        float valueFloat = 0.0f;
        double valueDouble = 0.0;
        long saveLocus;
        long preExifLocus = 0L;
        int sampleFormat = 1; // 1 is default for unsigned integers
        int expectedCount;
        boolean debuggingFileIO = Preferences.debugLevel(Preferences.DEBUG_FILEIO);
        int exifDirEntries = 0;
        boolean zero;
        fileInfo.setEndianess(endianess);
        nDirEntries = getUnsignedShort(endianess);

        if (nDirEntries <= 0) {
            throw new IOException("First 2 IFD bytes are an illegal " + nDirEntries);
        }

        if (debuggingFileIO) {
            Preferences.debug("\nOpenIFD: Entries = " + nDirEntries + "\n", Preferences.DEBUG_FILEIO);
        }

        for (i = 0; i < nDirEntries + exifDirEntries; i++) {
            tag = getUnsignedShort(endianess);

            if (tag == 0) {
                throw new IOException("Tiff Zero Tag Error");
            } else if (tag == 43314) {
                foundTag43314 = true;
            }

            type = getUnsignedShort(endianess);
            count = getInt(endianess);

            if ((type == SHORT) && (count == 1)) {
                valueArray[0] = getUnsignedShort(endianess);
                getUnsignedShort(endianess);
            } else if ((type == SHORT) && (count == 2)) {
                valueArray[0] = getUnsignedShort(endianess);
                valueArray[1] = getUnsignedShort(endianess);
            } else if ((type == SHORT) && (count >= 3)) {
                value_offset = getInt(endianess);

                saveLocus = raFile.getFilePointer();
                raFile.seek(value_offset);

                for (i1 = 0; ((i1 < count) && (i1 < MAX_IFD_LENGTH)); i1++) {
                    valueArray[i1] = getUnsignedShort(endianess);
                }

                raFile.seek(saveLocus);
            } else if ((type == LONG) && (count == 1)) {
                valueArray[0] = getUInt(endianess);
            } else if ((type == LONG) && (count >= 2)) {
                value_offset = getInt(endianess);

                saveLocus = raFile.getFilePointer();
                raFile.seek(value_offset);

                for (i1 = 0; ((i1 < count) && (i1 < MAX_IFD_LENGTH)); i1++) {
                    valueArray[i1] = getUInt(endianess);
                }

                raFile.seek(saveLocus);
            } else if ((type == SLONG) && (count == 1)) {
                valueArray[0] = getInt(endianess);
            } else if ((type == SLONG) && (count >= 2)) {
                value_offset = getInt(endianess);

                saveLocus = raFile.getFilePointer();
                raFile.seek(value_offset);

                for (i1 = 0; ((i1 < count) && (i1 < MAX_IFD_LENGTH)); i1++) {
                    valueArray[i1] = getInt(endianess);
                }

                raFile.seek(saveLocus);
            } else if ((type == RATIONAL) || (type == SRATIONAL)) {
                value_offset = getInt(endianess);

                saveLocus = raFile.getFilePointer();
                raFile.seek(value_offset);

                for (i1 = 0; ((i1 < (2 * count)) && (i1 < MAX_IFD_LENGTH)); i1 = i1 + 2) {
                    valueArray[i1] = getInt(endianess);
                    valueArray[i1 + 1] = getInt(endianess);
                }

                raFile.seek(saveLocus);
            } else if ((type == DOUBLE) && (count == 1)) {
                value_offset = getInt(endianess);

                saveLocus = raFile.getFilePointer();
                raFile.seek(value_offset);
                valueDouble = getDouble(endianess);
                raFile.seek(saveLocus);
            } else if ((type == DOUBLE) && (count > 1)) {

                // Ignore these EchoTech fields for now
                value_offset = getInt(endianess);
            } else if (((type == BYTE) || (type == UNDEFINED) || (type == ASCII)) && (count == 0)) {
                raFile.seek(raFile.getFilePointer() + 4);
                // raFile.readUnsignedByte();
                // raFile.readUnsignedByte();
                // raFile.readUnsignedByte();
                // raFile.readUnsignedByte();
            } else if (((type == BYTE) || (type == UNDEFINED) || (type == ASCII)) && (count == 1)) {
                valueArray[0] = raFile.readUnsignedByte();
                raFile.seek(raFile.getFilePointer() + 3);
                // raFile.readUnsignedByte();
                // raFile.readUnsignedByte();
                // raFile.readUnsignedByte();
            } else if (((type == BYTE) || (type == UNDEFINED) || (type == ASCII)) && (count == 2)) {
                valueArray[0] = raFile.readUnsignedByte();
                valueArray[1] = raFile.readUnsignedByte();
                raFile.seek(raFile.getFilePointer() + 2);
                // raFile.readUnsignedByte();
                // raFile.readUnsignedByte();
            } else if (((type == BYTE) || (type == UNDEFINED) || (type == ASCII)) && (count == 3)) {
                valueArray[0] = raFile.readUnsignedByte();
                valueArray[1] = raFile.readUnsignedByte();
                valueArray[2] = raFile.readUnsignedByte();
                raFile.seek(raFile.getFilePointer() + 1);
                // raFile.readUnsignedByte();
            } else if (((type == BYTE) || (type == UNDEFINED) || (type == ASCII)) && (count == 4)) {
                valueArray[0] = raFile.readUnsignedByte();
                valueArray[1] = raFile.readUnsignedByte();
                valueArray[2] = raFile.readUnsignedByte();
                valueArray[3] = raFile.readUnsignedByte();
            } else if (((type == BYTE) || (type == UNDEFINED) || (type == ASCII)) && (count > 4)) {
                value_offset = getInt(endianess);

                saveLocus = raFile.getFilePointer();
                raFile.seek(value_offset);

                for (i1 = 0; ((i1 < count) && (i1 < MAX_IFD_LENGTH)); i1++) {
                    valueArray[i1] = raFile.readUnsignedByte();
                }

                raFile.seek(saveLocus);
            } else if ((type == SBYTE) && (count == 1)) {
                valueArray[0] = raFile.readByte();
                raFile.seek(raFile.getFilePointer() + 3);
                // raFile.readByte();
                // raFile.readByte();
                // raFile.readByte();
            } else if ((type == SBYTE) && (count == 2)) {
                valueArray[0] = raFile.readByte();
                valueArray[1] = raFile.readByte();
                raFile.seek(raFile.getFilePointer() + 2);
                // raFile.readByte();
                // raFile.readByte();
            } else if ((type == SBYTE) && (count == 3)) {
                valueArray[0] = raFile.readByte();
                valueArray[1] = raFile.readByte();
                valueArray[2] = raFile.readByte();
                raFile.seek(raFile.getFilePointer() + 1);
                // raFile.readByte();
            } else if ((type == SBYTE) && (count == 4)) {
                valueArray[0] = raFile.readByte();
                valueArray[1] = raFile.readByte();
                valueArray[2] = raFile.readByte();
                valueArray[3] = raFile.readByte();
            } else if ((type == SBYTE) && (count > 4)) {
                value_offset = getInt(endianess);

                saveLocus = raFile.getFilePointer();
                raFile.seek(value_offset);

                for (i1 = 0; ((i1 < count) && (i1 < MAX_IFD_LENGTH)); i1++) {
                    valueArray[i1] = raFile.readByte();
                }

                raFile.seek(saveLocus);
            } else if ((type == SSHORT) && (count == 1)) {
                valueArray[0] = getSignedShort(endianess);
                getSignedShort(endianess);
            } else if ((type == SSHORT) && (count == 2)) {
                valueArray[0] = getSignedShort(endianess);
                valueArray[1] = getSignedShort(endianess);
            } else if ((type == SSHORT) && (count >= 3)) {
                value_offset = getInt(endianess);

                saveLocus = raFile.getFilePointer();
                raFile.seek(value_offset);

                for (i1 = 0; ((i1 < count) && (i1 < MAX_IFD_LENGTH)); i1++) {
                    valueArray[i1] = getSignedShort(endianess);
                }

                raFile.seek(saveLocus);
            } else if ((type == FLOAT) && (count == 1)) {
                valueFloat = getFloat(endianess);
            } else if ((type == FLOAT) && (count > 1)) {

                // Ignore these fields for now
                value_offset = getInt(endianess);
            } else {

                if (debuggingFileIO) {
                    Preferences.debug("\nOpenIFD: Unknown field type = " + type + " Tag = " + tag + " count = " +
                                      count + "\n", Preferences.DEBUG_FILEIO);
                }

                throw new IOException("OpenIFD: Unknown field type = " + type + " Tag = " + tag + " count = " + count);
            }

            if (debuggingFileIO) {
                Preferences.debug("\nFileTiff.openIFD: Tag = " + tag + "\n", Preferences.DEBUG_FILEIO);

                switch (type) {

                    case BYTE:
                        Preferences.debug("FileTiff.openIFD: Type = BYTE  Count = " + count + "\n",
                                          Preferences.DEBUG_FILEIO);
                        break;

                    case ASCII:
                        Preferences.debug("FileTiff.openIFD: Type = ASCII  Count = " + count + "\n",
                                          Preferences.DEBUG_FILEIO);
                        break;

                    case SHORT:
                        Preferences.debug("FileTiff.openIFD: Type = SHORT  Count = " + count + "\n",
                                          Preferences.DEBUG_FILEIO);
                        break;

                    case LONG:
                        Preferences.debug("FileTiff.openIFD: Type = LONG  Count = " + count + "\n",
                                          Preferences.DEBUG_FILEIO);
                        break;

                    case RATIONAL:
                        Preferences.debug("FileTiff.openIFD: Type = RATIONAL  Count = " + count + "\n",
                                          Preferences.DEBUG_FILEIO);
                        break;

                    case SBYTE:
                        Preferences.debug("FileTiff.openIFD: Type = SBYTE  Count = " + count + "\n",
                                          Preferences.DEBUG_FILEIO);
                        break;

                    case UNDEFINED:
                        Preferences.debug("FileTiff.openIFD: Type = UNDEFINED  Count = " + count + "\n",
                                          Preferences.DEBUG_FILEIO);
                        break;

                    case SSHORT:
                        Preferences.debug("FileTiff.openIFD: Type = SSHORT  Count = " + count + "\n",
                                          Preferences.DEBUG_FILEIO);
                        break;

                    case SLONG:
                        Preferences.debug("FileTiff.openIFD: Type = SLONG  Count = " + count + "\n",
                                          Preferences.DEBUG_FILEIO);
                        break;

                    case SRATIONAL:
                        Preferences.debug("FileTiff.openIFD: Type = SRATIONAL  Count = " + count + "\n",
                                          Preferences.DEBUG_FILEIO);
                        break;

                    case FLOAT:
                        Preferences.debug("FileTiff.openIFD: Type = FLOAT  Count = " + count + "\n",
                                          Preferences.DEBUG_FILEIO);
                        break;

                    case DOUBLE:
                        Preferences.debug("FileTiff.openIFD: Type = DOUBLE  Count = " + count + "\n",
                                          Preferences.DEBUG_FILEIO);
                        break;
                }
            }

            if ((type == RATIONAL) || (type == SRATIONAL)) {
                ecount = 2 * count;
            } else {
                ecount = count;
            }

            if ((type != DOUBLE) && (type != FLOAT) && debuggingFileIO) {

                for (i1 = 0; ((i1 < ecount) && (i1 < MAX_IFD_LENGTH)); i1++) {
                    Preferences.debug("FileTiff.openIFD: value[" + (i1 + 1) + "] = " + valueArray[i1] + "\n",
                                      Preferences.DEBUG_FILEIO);
                }
            } else if ((type == DOUBLE) && (count == 1) && debuggingFileIO) {
                Preferences.debug("FIleTiff.openIFD: value = " + valueDouble + "\n", Preferences.DEBUG_FILEIO);
            } else if ((type == FLOAT) && (count == 1) && debuggingFileIO) {
                Preferences.debug("fileTiff.openIFD: value = " + valueFloat + "\n", Preferences.DEBUG_FILEIO);
            }

            switch (tag) {

                case SUBFILE_TYPE:
                    if (type != SHORT) {
                        throw new IOException("SUBFILE_TYPE has illegal TYPE = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("SUBFILE_TYPE has illegal count = " + count + "\n");    
                    }
                    
                    if (debuggingFileIO) {
                        if (valueArray[0] == 1) {
                            Preferences.debug("SUBFILE_TYPE = 1 for full resolution image data\n",
                                               Preferences.DEBUG_FILEIO);
                        }
                        else if (valueArray[0] == 2) {
                            Preferences.debug("SUBFILE_TYPE = 2 for reduced-resolution image data\n",
                                               Preferences.DEBUG_FILEIO);
                        }
                        else if (valueArray[0] == 3) {
                            Preferences.debug("SUBFILE_TYPE = 3 for a single page of a multi-page image\n",
                                               Preferences.DEBUG_FILEIO);   
                        }
                        else {
                            Preferences.debug("SUBFILE_TYPE has an unrecognized value = " + valueArray[0] + "\n",
                                               Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                case NEW_SUBFILE_TYPE:
                    if (type != LONG) {
                        throw new IOException("NEW_SUBFILE_TYPE has illegal TYPE = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("NEW_SUBFILE_TYPE has illegal count = " + count + "\n");
                    }

                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: tag = NEW_SUBFILE_TYPE\n", Preferences.DEBUG_FILEIO);

                        if ((valueArray[0] & 0x01) == 0x01) {
                            Preferences.debug("Image is a reduced resolution version of another " +
                                              "image in this TIFF file\n", Preferences.DEBUG_FILEIO);
                        } else {
                            Preferences.debug("Image is not a reduced resolution version of another " +
                                              "image in this TIFF file\n", Preferences.DEBUG_FILEIO);
                        }

                        if ((valueArray[0] & 0x02) == 0x02) {
                            Preferences.debug("Image is a single page of a multi-page image\n", 2);
                        } else {
                            Preferences.debug("Image is not a single page of a multi-page image\n",
                                              Preferences.DEBUG_FILEIO);
                        }

                        if ((valueArray[0] & 0x04) == 0x04) {
                            Preferences.debug("Images defines a transparency mask for another image " +
                                              "in this TIFF file\n", Preferences.DEBUG_FILEIO);
                        } else {
                            Preferences.debug("Images does not define a transparency mask for another image " +
                                              "in this TIFF file\n", Preferences.DEBUG_FILEIO);
                        }
                    }

                    break;

                case IMAGE_WIDTH:
                    if ((type != SHORT) && (type != LONG)) {
                        throw new IOException("IMAGE_WIDTH has illegal TYPE = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("IMAGE_WIDTH has illegal count = " + count + "\n");
                    }

                    xDim = (int) valueArray[0];
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: Image_Width = " + xDim + "\n", 2);
                    }

                    break;

                case IMAGE_LENGTH:
                    if ((type != SHORT) && (type != LONG)) {
                        throw new IOException("IMAGE_LENGTH has illegal TYPE = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("IMAGE_LENGTH has illegal COUNT = " + count + "\n");
                    }

                    yDim = (int) valueArray[0];
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: Image_Length = " + yDim + "\n", 2);
                    }

                    break;
                    
                case FILL_ORDER:
                    if (type != SHORT) {
                        throw new IOException ("FILL_ORDER has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("FILL_ORDER has illegal COUNT = " + count + "\n");
                    }
                    fillOrder =  (int) valueArray[0];
                    if (debuggingFileIO) {
                        if (fillOrder == 1) {
                            Preferences.debug("FileTiff.openIFD: FILL_ORDER = 1 for pixels are arranged within a byte\n" +
                                                 "such that lower column values are stored in the\n" +
                                                 "higher-order bits of the byte\n");
                        }
                        else if (fillOrder == 2) {
                            Preferences.debug("FileTiff.openIFD: FILL_ORDER = 2 for pixels are arranged within a byte\n" +
                                    "such that lower column values are stored in the\n" +
                                    "lower-order bits of the byte\n");
                        }
                        else {
                            Preferences.debug("FileTiff.openIFD: FILL_ORDER has an illegal values = " + fillOrder + "\n");
                        }
                    }
                    break;

                case BITS_PER_SAMPLE:
                    if (type != SHORT) {
                        throw new IOException("BITS_PER_SAMPLE has illegal type = " + type + "\n");
                    }

                    bitsPerSample = new int[count];
                    for (i1 = 0; i1 < count; i1++) {
                        bitsPerSample[i1] = (int) valueArray[i1];
                    }

                    if ((count == 1) && debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: BitsPerSample = " + valueArray[0] + "\n",
                                          Preferences.DEBUG_FILEIO);
                    } else if (count > 1) {

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: BitsPerSample are above\n", Preferences.DEBUG_FILEIO);
                        }

                        for (i1 = 1; i1 < count; i1++) {

                            if (valueArray[i1] != valueArray[0]) {

                                if (debuggingFileIO) {
                                    Preferences.debug("MIPAV cannot handle mixed data types", Preferences.DEBUG_FILEIO);
                                }

                                throw new IOException("MIPAV cannot handle mixed data types");
                            }
                        }
                    } // else if (count > 1)

                    break;

                case ROWS_PER_STRIP:
                    if ((type != SHORT) && (type != LONG)) {
                        throw new IOException("ROWS_PER-STRIP has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("ROWS_PER_STRIP has illegal count = " + count + "\n");
                    }

                    // Note that 2**32 -1 meaning to put all the rows in 1 strip shows up as -1
                    rowsPerStrip = (int) valueArray[0];
                    haveRowsPerStrip = true;
                    if (debuggingFileIO) {
                        Preferences.debug("ROWS_PER_STRIP = " + valueArray[0] + "\n", Preferences.DEBUG_FILEIO);
                    }

                    break;

                case STRIP_OFFSETS:
                    if ((type != SHORT) && (type != LONG)) {
                        throw new IOException("STRIP_OFFSETS has illegal type = " + type + "\n");
                    }

                    dataOffsets[imageSlice] = new Vector();
                    if (count == 1) {

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Strip_offset = " + valueArray[0] + "\n",
                                              Preferences.DEBUG_FILEIO);
                        }

                        dataOffsets[imageSlice].addElement(new Index((int) valueArray[0]));
                    } else if (count > 1) {

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Strip_offsets are above\n");
                        }

                        for (i1 = 0; i1 < count; i1++) {

                            dataOffsets[imageSlice].addElement(new Index((int) valueArray[i1]));
                        }
                    }

                    break;

                case STRIP_BYTE_COUNTS:
                    if ((type != SHORT) && (type != LONG)) {
                        throw new IOException("STRIP_BYTE_COUNTS has illegal type = " + type + "\n");
                    }

                    if (count == 1) {

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Strip byte counts = " + valueArray[0] + "\n",
                                              Preferences.DEBUG_FILEIO);
                        }

                        ((Index) (dataOffsets[imageSlice].elementAt(0))).byteCount = (int) valueArray[0];
                    } else if (count > 1) {

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD. Strip byte counts are above\n",
                                              Preferences.DEBUG_FILEIO);
                        }

                        for (i1 = 0; i1 < count; i1++) {

                            ((Index) (dataOffsets[imageSlice].elementAt(i1))).byteCount = (int) valueArray[i1];
                        }
                    }

                    break;
                    
                case PAGE_NUMBER:
                    if (type != SHORT) {
                        throw new IOException("PAGE_NUMBER has illegal type = " + type + "\n");
                    }
                    
                    if (count != 2) {
                        throw new IOException("PAGE_NUMBER has illegal count = " + count + "\n");
                    }
                    
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: Page number = " + valueArray[0] + "\n",
                                          Preferences.DEBUG_FILEIO);
                        if (valueArray[1] > 0) {
                            Preferences.debug("FileTiff.openIFD: Number of pages in the document = " +
                                               valueArray[1] + "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;

                case PHOTO_INTERP:
                    if (type != SHORT) {
                        throw new IOException("PHOTO_INTERP has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("PHOTO_INTERP has illegal count = " + count + "\n");
                    }

                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: PhotoInterp= " + valueArray[0] + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    if (valueArray[0] == 1) { // Black is zero
                        fileInfo.setPhotometric((short) 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: PhotoInterp = Black is zero\n" +
                                              "For bilevel and grayscale images\n", Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == 0) { // white is zero
                        fileInfo.setPhotometric((short) 0);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: PhotoInterp = White is zero\n" +
                                              "For bilevel and grayscale images\n", Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == 2) { // Color RGB
                        fileInfo.setPhotometric((short) 2);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: PhotoInterp = RGB\n", Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == 3) { // Color Indexed
                        fileInfo.setPhotometric((short) 3);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: PhotoInterp = Palette color\n", 2);
                        }
                    } else if (valueArray[0] == 4) { // Transparency Mask
                        fileInfo.setPhotometric((short) 4);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: PhotoInterp = Transparency Mask\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == 5) { // Separated - usually CMYK
                        // bits per pixel = 8,8,8,8 for CMYK
                        fileInfo.setPhotometric((short) 5);
                         
                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: PhotoInterp = Separated\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == 6) { // YCbCr
                        isYCbCr = true;
                        fileInfo.setPhotometric((short) 6);
                        
                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: PhotoInterp = YCbCr\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == 8) { // CIELAB
                        isCIELAB = true;
                        fileInfo.setPhotometric((short) 8);
                        
                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: PhotoInterp = CIELAB\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == 32844) { // CIE Log2(L)
                        isLogL = true;
                        fileInfo.setPhotometric((short) 32844);
                        
                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: PhotoInterp = CIE Log2(L)\n",
                                               Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == 32845) { // CIE Log2(L) (u', v')
                        isLogLuv = true;
                        fileInfo.setPhotometric((short) 32845);
                        
                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: PhotoInterp = CIE Log2(L) (u', v')\n",
                                               Preferences.DEBUG_FILEIO);
                        }
                    } else {
                        throw new IOException("PHOTOINTERP has illegal value = " + valueArray[0]);
                    }

                    break;

                case SAMPLES_PER_PIXEL:
                    if (type != SHORT) {
                        throw new IOException("SAMPLES_PER_PIXEL has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("SAMPLES_PER_PIXEL has illegal count = " + count + "\n");
                    }

                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: samplesPerPixel = " + valueArray[0] + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    samplesPerPixel = (int) valueArray[0];
                    break;

                case PLANAR_CONFIG:
                    if (type != SHORT) {
                        throw new IOException("PLANAR_CONFIG has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("PLANAR_CONFIG has illegal count = " + count + "\n");
                    }

                    if ((valueArray[0] != 1) && (valueArray[0] != 2)) {
                        throw new IOException("PLANAR_CONFIG has illegal value = " + valueArray[0] + "\n");
                    }

                    if (valueArray[0] == 1) { // can be black and white or color
                        chunky = true;

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: planar config = chunky \n", 2);
                        }
                    } else if (valueArray[0] == 2) { // Color RGB
                        chunky = false;

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: planar config = RRRRR, GGGG, BBBB. \n",
                                              Preferences.DEBUG_FILEIO);
                        }
                    }

                    break;

                case COMPRESSION:
                    if (type != SHORT) {
                        throw new IOException("COMPRESSION has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("COMPRESSION has illegal count = " + count + "\n");
                    }

                    if (valueArray[0] == 1) {

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: compression = no compression\n ",
                                              Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == 2) {
                        modHuffmanCompression = true;
                        
                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: compression = modified Huffman\n",
                                               Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == 3) {
                        fax3Compression = true;
                        
                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: compression = CCITT FAX3 or T4\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == 4) {
                        fax4Compression = true;
                        
                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: compression = CCITT FAX4 or T6\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                    
                    } else if (valueArray[0] == 5) {
                        lzwCompression = true;

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: compression = LZW\n ", Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == 6) {
                        throw new IOException("Deprecated JPEG compression is not implemented");
                    } else if (valueArray[0] == 7) {
                        jpegCompression = true;
                        
                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: compression = jpeg\n", Preferences.DEBUG_FILEIO);
                        }
                    } else if ((valueArray[0] == 8) || (valueArray[0] == 32946)) {
                        zlibCompression = true;
                        
                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: compression = zlib\n", Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == 32773) {
                        packBit = true;

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: compression = packed bit\n ", 2);
                        }
                    } else if (valueArray[0] == 32809) {
                        ThunderScanCompression = true;
                        
                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: compression = ThunderScan\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                    }
                    else if (valueArray[0] == 34676) {
                        SGILogCompression = true;
                        
                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: compression = SGI Log Luminance RLE\n",
                                               Preferences.DEBUG_FILEIO);
                        }
                    }
                    else if (valueArray[0] == 34677) {
                        SGILog24Compression = true;
                        
                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: compression = SGI Log 24-bit packed\n",
                                               Preferences.DEBUG_FILEIO);
                        }
                    }
                    else {
                            throw new IOException("COMPRESSION has illegal value = " + valueArray[0] + "\n");
                    }

                    break;
                    
                case T4OPTIONS:
                    if (type != LONG) {
                        throw new IOException("T4OPTIONS has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("T4OPTIONS has illegal count = " + count + "\n");
                    }
                    
                    if ((valueArray[0] & 0x01) != 0) {
                        group3_2D_Coding = true;
                        if (debuggingFileIO) {
                            Preferences.debug("FAX 3 decompression uses 2-dimensional coding\n");
                        }
                    }
                    else {
                        group3_2D_Coding = false;
                        if (debuggingFileIO) {
                            Preferences.debug("FAX 3 decompression uses 1-dimensional coding\n");
                        }
                    }
                    
                    if ((valueArray[0] & 0x02) != 0) {
                        group3Uncompressed = true;
                        if (debuggingFileIO) {
                            Preferences.debug("FAX 3 decompression uses uncompressed mode\n");
                        }
                    }
                    else {
                        group3Uncompressed = false;
                        if (debuggingFileIO) {
                            Preferences.debug("FAX 3 decompression does not use uncompressed mode\n");
                        }
                    }
                    
                    if ((valueArray[0] & 0x04) != 0) {
                        group3Fillbits = true;
                        if (debuggingFileIO) {
                            Preferences.debug("In FAX3 decompression fill bits have been added\n");
                        }
                    }
                    else {
                        group3Fillbits = false;
                        if (debuggingFileIO) {
                            Preferences.debug("in FAX3 decompression no fill bits are used\n");
                        }
                    }
                    
                    break;
                    
                    
                case T6OPTIONS:
                    if (type != LONG) {
                        throw new IOException("T6OPTIONS has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("T6OPTIONS has illegal count = " + count + "\n");
                    }
                    
                    if ((valueArray[0] & 0x02) != 0) {
                        group4Uncompressed = true;
                        if (debuggingFileIO) {
                            Preferences.debug("FAX 4 decompression allows uncompressed mode\n");
                        }
                    }
                    else {
                        group4Uncompressed = false;
                        if (debuggingFileIO) {
                            Preferences.debug("FAX 4 decompression does not allow uncompressed mode\n");
                        }
                    }
                    
                    break;

                case ORIENTATION:
                    if (type != SHORT) {
                        throw new IOException("ORIENTATION has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("ORIENTATION has illegal count = " + count + "\n");
                    }

                    if ((valueArray[0] < 1) || (valueArray[0] > 8)) {
                        throw new IOException("ORIENTATION has illegal value = " + valueArray[0] + "\n");
                    }

                    if (valueArray[0] > 1) {
                        throw new IOException("Only default orientation is currently supported\n");
                    } else {

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: orientation has 0th row representing\n");
                            Preferences.debug("the top of the image, and the 0th column representing\n");
                            Preferences.debug("the left hand side of the image\n");
                        }
                    }

                    break;

                case COLOR_MAP:
                    if (type != SHORT) {
                        throw new IOException("COLOR_MAP has illegal type = " + type + "\n");
                    }

                    if ((count % 6) != 0) {
                        throw new IOException("COLOR_MAP has illegal count = " + count + "\n");
                    }

                    // Already read LUT - only same LUT in every file of multiFile and only one
                    // LUT for a multiImage file for now.
                    expectedCount = 768;
                    if ((bitsPerSample != null) && ((bitsPerSample[0] == 2) || (bitsPerSample[0] == 4))) {
                        expectedCount = (int)Math.round(3 * Math.pow(2, bitsPerSample[0])); // 12 or 48
                    }
                    if ((count == expectedCount) && (LUT == null)) {

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff creating color map\n");
                        }

                        int[] extents = new int[2];
                        extents[0] = 4;
                        extents[1] = 256;

                        // System.err.println("Creating new ModelLUT");
                        LUT = new ModelLUT(ModelLUT.GRAY, 256, extents);

                        int maxValue = -Integer.MAX_VALUE;

                        for (i1 = 0; i1 < count; i1++) {

                            if (valueArray[i1] > maxValue) {
                                maxValue = (int) valueArray[i1];
                            }
                        }

                        if (maxValue > 255) {

                            for (i1 = 0; i1 < count; i1++) {
                                valueArray[i1] = valueArray[i1] * 255 / maxValue;
                            }
                        }

                        for (i1 = 0; i1 < count/3; i1++) {
                            LUT.set(0, i1, 1.0f);
                            LUT.set(1, i1, (float) valueArray[i1]);
                            LUT.set(2, i1, (float) valueArray[i1 + count/3]);
                            LUT.set(3, i1, (float) valueArray[i1 + 2*count/3]);
                        }

                        LUT.makeIndexedLUT(null);
                    }

                    break;
                    
                case YCBCR_COEFFICIENTS:
                    if (type != RATIONAL) {
                        throw new IOException("YCBCR_CEOFFICIENTS has illegal type = " + type + "\n");
                    }

                    if (count != 3) {
                        throw new IOException("XRESOLUTION has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    LumaRed = (float) numerator / denominator;
                    
                    numerator = valueArray[2];
                    denominator = valueArray[3];
                    LumaGreen = (float) numerator / denominator;
                    
                    numerator = valueArray[4];
                    denominator = valueArray[5];
                    LumaBlue = (float) numerator / denominator;
                    
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: YCbCr coefficients\n",
                                          Preferences.DEBUG_FILEIO);
                        Preferences.debug("FileTiff.openIFD: LumaRed = " + LumaRed +  
                                          "  LumaGreen = " + LumaGreen +
                                          "  LumaBlue = " + LumaBlue +
                                          "\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    break;
                    
                case YCBCR_SUBSAMPLING:
                    if (type != SHORT) {
                        throw new IOException("YCBCR_SUBSAMPLING has illegal type = " + type + "\n");
                    }
                    
                    if (count != 2) {
                        throw new IOException("YCBCR_SUBSAMPLING has illegal count = " + count + "\n");
                    }
                    
                    YCbCrSubsampleHoriz = (int)valueArray[0];
                    YCbCrSubsampleVert = (int)valueArray[1];
                    
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: YCbCrSubsampleHoriz = " + YCbCrSubsampleHoriz +
                                          "  YCbCrSubsampleVert = " + YCbCrSubsampleVert + "\n",
                                          Preferences.DEBUG_FILEIO);
                        if (YCbCrSubsampleHoriz == 1) {
                            Preferences.debug("FileTiff.openIFD: Image width of this chroma image is equal to the\n" +
                                              "image width of the associated luma image\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                        else if (YCbCrSubsampleHoriz == 2) {
                            Preferences.debug("FileTIff.openIFD: Image width of this chroma image is half the image\n" +
                                              "width of the associated luma image\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                        else if (YCbCrSubsampleHoriz == 4) {
                            Preferences.debug("FileTIff.openIFD: Image width of this chroma image is one-quarter the\n" +
                                    "image width of the associated luma image\n",
                                    Preferences.DEBUG_FILEIO);   
                        }
                        if (YCbCrSubsampleVert == 1) {
                            Preferences.debug("FileTiff.openIFD: Image height of this chroma image is equal to the\n" +
                                              "image height of the associated luma image\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                        else if (YCbCrSubsampleVert == 2) {
                            Preferences.debug("FileTIff.openIFD: Image height of this chroma image is half the image\n" +
                                              "height of the associated luma image\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                        else if (YCbCrSubsampleVert == 4) {
                            Preferences.debug("FileTIff.openIFD: Image height of this chroma image is one-quarter the\n" +
                                    "image height of the associated luma image\n",
                                    Preferences.DEBUG_FILEIO);   
                        }
                    }
                    
                    break;
                    
                case YCBCR_POSITIONING:
                    if (type != SHORT) {
                        throw new IOException("YCBCR_POSITIONING has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("YCBCR_POSITIONING has illegal count = " + count + "\n");
                    }
                    
                    YCbCrPositioning = (int)valueArray[0];
                    if (debuggingFileIO) {
                        if (YCbCrPositioning == 1) {
                            Preferences.debug("FileTIff.openIFD: YCbCrPositioning = 1 for centered\n" +
                                              "xOffset[0,0] = chromaSubsampleHoriz/2 - 0.5\n" +
                                              "yOffset[0,0] = chromaSubsampleVert/2 - 0.5\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                        else if (YCbCrPositioning == 2) {
                            Preferences.debug("FileTIff.openIFD: YCbCrPositioning = 2 for cosited\n" +
                                    "xOffset[0,0] = 0, yOffset[0,0] = 0\n",
                                    Preferences.DEBUG_FILEIO);   
                        }
                    }
                    break;
                    
                case REFERENCE_BLACK_WHITE:
                    if (type != RATIONAL) {
                        throw new IOException("REFERENCE_BLACK_WHITE has illegal type = " + type + "\n");
                    }

                    if (count != 6) {
                        throw new IOException("REFERENCE_BLACK_WHITE has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    YReferenceBlack = (int) (numerator / denominator);
                    
                    numerator = valueArray[2];
                    denominator = valueArray[3];
                    YReferenceWhite = (int) (numerator / denominator);
                    
                    numerator = valueArray[4];
                    denominator = valueArray[5];
                    CbReferenceBlack = (int) (numerator / denominator);
                    
                    numerator = valueArray[6];
                    denominator = valueArray[7];
                    CbReferenceWhite = (int) (numerator / denominator);
                    
                    numerator = valueArray[8];
                    denominator = valueArray[9];
                    CrReferenceBlack = (int) (numerator / denominator);
                    
                    numerator = valueArray[10];
                    denominator = valueArray[11];
                    CrReferenceWhite = (int) (numerator / denominator);
                    
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: YReferenceBlack = " + YReferenceBlack + 
                                                             " YReferenceWhite = " + YReferenceWhite + "\n" +
                                                             "CbReferenceBlack = " + CbReferenceBlack +
                                                             " CbReferenceWhite = " + CbReferenceWhite + "\n" +
                                                             "CrReferenceBlack = " + CrReferenceBlack +
                                                             " CrReferenceWhite = " + CrReferenceWhite + "\n",
                                                             Preferences.DEBUG_FILEIO);
                    }
                    
                    break;

                case RESOLUTION_UNIT:
                    if (type != SHORT) {
                        throw new IOException("RESOLUTION_UNIT has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("RESOLUTION_UNIT has illegal count = " + count + "\n");
                    } else if ((valueArray[0] < 1) || (valueArray[0] > 17)) {
                        throw new IOException("RESOLUTION_UNIT has illegal value = " + valueArray[0] + "\n");
                    }

                    if (valueArray[0] == FileInfoBase.MILLIMETERS) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.MILLIMETERS, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.MILLIMETERS, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = MILLIMETERS\n");
                        }
                    } else if (valueArray[0] == FileInfoBase.UNKNOWN_MEASURE) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.UNKNOWN_MEASURE, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.UNKNOWN_MEASURE, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = UNKNOWN\n", 2);
                        }
                    } else if (valueArray[0] == FileInfoBase.INCHES) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.INCHES, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.INCHES, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = INCHES\n", 2);
                        }
                    } else if (valueArray[0] == FileInfoBase.CENTIMETERS) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.CENTIMETERS, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.CENTIMETERS, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = CENTIMETERS\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == FileInfoBase.ANGSTROMS) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.ANGSTROMS, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.ANGSTROMS, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = ANGSTROMS\n", 2);
                        }
                    } else if (valueArray[0] == FileInfoBase.NANOMETERS) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.NANOMETERS, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.NANOMETERS, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = NANOMETERS\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == FileInfoBase.MICROMETERS) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.MICROMETERS, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.MICROMETERS, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = MICROMETERS\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == FileInfoBase.METERS) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.METERS, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.METERS, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = METERS\n", 2);
                        }
                    } else if (valueArray[0] == FileInfoBase.KILOMETERS) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.KILOMETERS, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.KILOMETERS, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = KILOMETERS\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                    } else if (valueArray[0] == FileInfoBase.MILES) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.MILES, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.MILES, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = MILES\n", 2);
                        }
                    } else if (valueArray[0] == FileInfoBase.NANOSEC) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.NANOSEC, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.NANOSEC, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = NANOSEC\n", 2);
                        }
                    } else if (valueArray[0] == FileInfoBase.MICROSEC) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.MICROSEC, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.MICROSEC, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = MICROSEC\n", 2);
                        }
                    } else if (valueArray[0] == FileInfoBase.MILLISEC) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.MILLISEC, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.MILLISEC, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = MILLISEC\n", 2);
                        }
                    } else if (valueArray[0] == FileInfoBase.SECONDS) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.SECONDS, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.SECONDS, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = SECONDS\n", 2);
                        }
                    } else if (valueArray[0] == FileInfoBase.MINUTES) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.MINUTES, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.MINUTES, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = MINUTES\n", 2);
                        }
                    } else if (valueArray[0] == FileInfoBase.HOURS) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.HOURS, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.HOURS, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = HOURS\n", 2);
                        }
                    } else if (valueArray[0] == FileInfoBase.HZ) {
                        fileInfo.setUnitsOfMeasure(FileInfoBase.HZ, 0);
                        fileInfo.setUnitsOfMeasure(FileInfoBase.HZ, 1);

                        if (debuggingFileIO) {
                            Preferences.debug("FileTiff.openIFD: Resolution Unit = HERTZ\n", 2);
                        }
                    }

                    break;

                case XRESOLUTION:
                    if (type != RATIONAL) {
                        throw new IOException("XRESOLUTION has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("XRESOLUTION has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    imgResols[0] = (float) numerator / denominator;
                    imgResols[0] = 1 / imgResols[0];
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: X Resolution = " + imgResols[0] + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    break;

                case YRESOLUTION:
                    if (type != RATIONAL) {
                        throw new IOException("YRESOLUTION has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("YRESOLUTION has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    imgResols[1] = (float) numerator / denominator;
                    imgResols[1] = 1 / imgResols[1];
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: Y Resolution = " + imgResols[1] + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    break;

                case ZRESOLUTION:
                    if (type != DOUBLE) {
                        throw new IOException("ZRESOLUTION has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("ZRESOLUTION has illegal count = " + count + "\n");
                    }

                    imgResols[2] = (float) valueDouble;
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: Z Resolution = " + imgResols[2] + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    // EchoTech uses mm for Z resolution units
                    fileInfo.setUnitsOfMeasure(FileInfoBase.MILLIMETERS, Preferences.DEBUG_FILEIO);
                    break;

                case TRESOLUTION:
                    if (type != DOUBLE) {
                        throw new IOException("TRESOLUTION has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("TRESOLUTION has illegal count = " + count + "\n");
                    }

                    imgResols[3] = (float) valueDouble;
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: T Resolution = " + imgResols[3] + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    // EchoTech uses msec for T resolution units
                    fileInfo.setUnitsOfMeasure(FileInfoBase.MILLISEC, 3);
                    break;
                    
                case XPOSITION:
                    if (type != RATIONAL) {
                        throw new IOException("XPOSITION has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("XPOSITION has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    xPosition = (float) numerator / denominator;
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: X Position = " + xPosition + 
                                          " in resolution units from the left side of the page\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    break;

                case YPOSITION:
                    if (type != RATIONAL) {
                        throw new IOException("YPOSITION has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("YPOSITION has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    yPosition = (float) numerator / denominator;
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: Y Position = " + yPosition + 
                                          " in resolution units from the top of the page\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    break;

                case TILE_WIDTH:
                    if ((type != SHORT) && (type != LONG)) {
                        throw new IOException("TILE_WIDTH has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("TILE_WIDTH has illegal count = " + count + "\n");
                    }

                    haveTileWidth = true;
                    tileWidth = (int) valueArray[0];
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: tileWidth = " + tileWidth + "\n", 2);
                    }

                    tilesAcross = (xDim + tileWidth - 1) / tileWidth;
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: tilesAcross = " + tilesAcross + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    break;

                case TILE_LENGTH:
                    if ((type != SHORT) && (type != LONG)) {
                        throw new IOException("TILE_LENGTH has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("TILE_LENGTH has illegal count = " + count + "\n");
                    }

                    tileLength = (int) valueArray[0];
                    haveTileLength = true;
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: tileLength = " + tileLength + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    tilesDown = (yDim + tileLength - 1) / tileLength;
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: tilesDown = " + tilesDown + "\n", 2);
                    }

                    break;

                case TILE_OFFSETS:

                    // System.err.println("Tiles per image: " + count);
                    if (type != LONG) {
                        throw new IOException("TILE_OFFSETS has illegal type = " + type + "\n");
                    }

                    tilesPerImage = count;
                    haveTileOffsets = true;

                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: tilesPerImage = " + tilesPerImage + "\n",
                                          Preferences.DEBUG_FILEIO);
                        Preferences.debug("FileTiff.openIFD: tileOffsets are above\n", Preferences.DEBUG_FILEIO);
                    }

                    if (tileOffsetNumber == 0) {
                        tileOffsets = new int[count];

                        for (i1 = 0; i1 < count; i1++) {
                            tileOffsets[i1] = (int) valueArray[i1];
                        }

                        tileOffsetNumber = count;
                    } // if (tileOffsetNumber == 0)
                    else {
                        tileTemp = new int[tileOffsetNumber];

                        for (i1 = 0; i1 < tileOffsetNumber; i1++) {
                            tileTemp[i1] = tileOffsets[i1];
                        }

                        tileOffsets = new int[tileOffsetNumber + count];

                        for (i1 = 0; i1 < tileOffsetNumber; i1++) {
                            tileOffsets[i1] = tileTemp[i1];
                        }

                        for (i1 = 0; i1 < count; i1++) {
                            tileOffsets[i1 + count] = (int) valueArray[i1];
                        }

                        tileOffsetNumber += count;
                    } // else for tileOffsetNumber != 0

                    break;

                case TILE_BYTE_COUNTS:
                    if ((type != SHORT) && (type != LONG)) {
                        throw new IOException("TILE_BYTE_COUNTS has illegal type = " + type + "\n");
                    }

                    if (tilesPerImage != count) {
                        throw new IOException("Count fields do not agree in TILE_OFFSETS and TILE_BYTE_COUNTS");
                    }

                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: tileByteCounts are above\n", Preferences.DEBUG_FILEIO);
                    }

                    if (tileByteNumber == 0) {
                        tileByteCounts = new int[count];

                        for (i1 = 0; i1 < count; i1++) {
                            tileByteCounts[i1] = (int) valueArray[i1];
                        }

                        tileByteNumber = count;
                    } else {
                        tileTemp = new int[tileByteNumber];

                        for (i1 = 0; i1 < tileByteNumber; i1++) {
                            tileTemp[i1] = tileByteCounts[i1];
                        }

                        tileByteCounts = new int[tileByteNumber + count];

                        for (i1 = 0; i1 < tileByteNumber; i1++) {
                            tileByteCounts[i1] = tileTemp[i1];
                        }

                        for (i1 = 0; i1 < count; i1++) {
                            tileByteCounts[i1 + count] = (int) valueArray[i1];
                        }

                        tileByteNumber += count;
                    } // else for tileByteNumber != 0

                    break;

                case IMAGE_DESCRIPTION:
                    if (type != ASCII) {
                        throw new IOException("IMAGE_DESCRIPTION has illegal type = " + type + "\n");
                    }

                    imageDescription = new byte[count];
                    for (i1 = 0; i1 < count; i1++) {
                        imageDescription[i1] = (byte) valueArray[i1];
                    }

                    str = new String(imageDescription);
                    fileInfo.setImageDescription(str);
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: imageDescription = " + str.trim() + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    break;
                    
                case MAKE:
                    if (type != ASCII) {
                        throw new IOException("MAKE has illegal type = " + type + "\n");
                    }

                    make = new byte[count];
                    for (i1 = 0; i1 < count; i1++) {
                        make[i1] = (byte) valueArray[i1];
                    }

                    str = new String(make);
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: make = " + str.trim() + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    break;
                    
                case MODEL:
                    if (type != ASCII) {
                        throw new IOException("MODEL has illegal type = " + type + "\n");
                    }

                    model = new byte[count];
                    for (i1 = 0; i1 < count; i1++) {
                        model[i1] = (byte) valueArray[i1];
                    }

                    str = new String(model);
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: model = " + str.trim() + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    break;

                case MIN_SAMPLE_VALUE:
                    if (type != SHORT) {
                        throw new IOException("MIN_SAMPLE_VALUE has illegal type = " + type + "\n");
                    }

                    if (debuggingFileIO) {

                        for (i1 = 0; i1 < count; i1++) {
                            Preferences.debug("FileTiff.openIFD: minSampleValue[" + i1 + "] = " + valueArray[i1] + "\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                    }

                    break;

                case MAX_SAMPLE_VALUE:
                    if (type != SHORT) {
                        throw new IOException("MAX_SAMPLE_VALUE has illegal type = " + type + "\n");
                    }

                    if (debuggingFileIO) {

                        for (i1 = 0; i1 < count; i1++) {
                            Preferences.debug("FileTiff.openIFD: maxSampleValue[" + i1 + "] = " + valueArray[i1] + "\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                    }

                    break;

                case SOFTWARE:
                    if (type != ASCII) {
                        throw new IOException("SOFTWARE has illegal type = " + type + "\n");
                    }

                    software = new byte[count];
                    for (i1 = 0; i1 < count; i1++) {
                        software[i1] = (byte) valueArray[i1];
                    }

                    str = new String(software);
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: software = " + str.trim() + "\n", Preferences.DEBUG_FILEIO);
                    }

                    break;
                    
                case DOCUMENT_NAME:
                    if (type != ASCII) {
                        throw new IOException("DOCUMENT_NAME has illegal type = " + type + "\n");
                    }

                    documentName = new byte[count];
                    for (i1 = 0; i1 < count; i1++) {
                        documentName[i1] = (byte) valueArray[i1];
                    }

                    str = new String(documentName);
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: Document name = " + str.trim() + "\n", Preferences.DEBUG_FILEIO);
                    }

                    break;
                    
                case INK_NAMES:
                    if (type != ASCII) {
                        throw new IOException("INK_NAMES has illegal type = " + type + "\n");
                    }

                    inkNames = new byte[count];
                    for (i1 = 0; i1 < count; i1++) {
                        inkNames[i1] = (byte) valueArray[i1];
                    }

                    str = new String(inkNames);
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: Ink names = " + str.trim() + "\n", Preferences.DEBUG_FILEIO);
                    }

                    break;

                case ARTIST:
                    if (type != ASCII) {
                        throw new IOException("ARTIST has illegal type = " + type + "\n");
                    }

                    artist = new byte[count];
                    for (i1 = 0; i1 < count; i1++) {
                        artist[i1] = (byte) valueArray[i1];
                    }

                    str = new String(artist);
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: artist = " + str.trim() + "\n", 2);
                    }

                    break;

                case HOST_COMPUTER:
                    if (type != ASCII) {
                        throw new IOException("HOST_COMPUTER has illegal type = " + type + "\n");
                    }

                    hostComputer = new byte[count];
                    for (i1 = 0; i1 < count; i1++) {
                        hostComputer[i1] = (byte) valueArray[i1];
                    }

                    str = new String(hostComputer);
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: hostComputer = " + str.trim() + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    break;
                    
                case COPYRIGHT:
                    if (type != ASCII) {
                        throw new IOException("COPYRIGHT has illegal type = " + type + "\n");
                    }

                    copyright = new byte[count];
                    for (i1 = 0; i1 < count; i1++) {
                        copyright[i1] = (byte) valueArray[i1];
                    }

                    str = new String(copyright);
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: copyright = " + str.trim() + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    break;

                case DATE_TIME:
                    if (type != ASCII) {
                        throw new IOException("DATE_TIME has illegal type = " + type + "\n");
                    }

                    // The TIFF standard specifies a count of 20, but we encountered a
                    // file with a count of 25, so remove count restriction to 20.
                    /*
                     * if (count != 20) throw new IOException("DATE_TIME has illegal count = " + count + "\n");
                     */
                    dateTime = new byte[count];
                    for (i1 = 0; i1 < count; i1++) {
                        dateTime[i1] = (byte) valueArray[i1];
                    }

                    str = new String(dateTime);
                    Preferences.debug("FileTiff.openIFD: dateTime = " + str.trim() + "\n", Preferences.DEBUG_FILEIO);
                    break;

                case SAMPLE_FORMAT:

                    // Default is 1, unsigned integer data
                    if (type != SHORT) {
                        throw new IOException("SAMPLE_FORMAT has illegal type = " + type + "\n");
                    }

                    sampleFormat = (int) valueArray[0];
                    if (debuggingFileIO) {

                        for (i1 = 0; i1 < count; i1++) {
                            Preferences.debug("FileTiff.openIFD: sampleFormat[ " + i1 + "] = " + valueArray[i1] + "\n",
                                              Preferences.DEBUG_FILEIO);

                            if (valueArray[i1] == 1) {
                                Preferences.debug("FileTiff.openIFD: unsigned integer data\n");
                            } else if (valueArray[i1] == 2) {
                                Preferences.debug("FileTiff.openIFD: two's complement signed integer data\n");
                            } else if (valueArray[i1] == 3) {
                                Preferences.debug("FileTiff.openIFD: IEEE floating point data\n");
                            } else if (valueArray[i1] == 4) {
                                Preferences.debug("FileTiff.openIFD: undefined data format\n");
                            }
                        }
                    }

                    break;

                case PREDICTOR:
                    if (type != SHORT) {
                        throw new IOException("PREDICTOR has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("PREDICTOR has illegal count = " + count + "\n");
                    }

                    if (valueArray[0] == 1) {
                        predictor = 1;

                        if (debuggingFileIO) {
                            Preferences.debug("PREDICTOR = 1 for no prediction scheme used\n");
                        }
                    } else if (valueArray[0] == 2) {
                        predictor = 2;

                        if (debuggingFileIO) {
                            Preferences.debug("PREDICTOR = 2 for horizontal differencing\n", 2);
                        }
                    } else {

                        if (debuggingFileIO) {
                            Preferences.debug("PREDICTOR = " + valueArray[0] + ", an illegal value",
                                              Preferences.DEBUG_FILEIO);
                        }
                    }

                    break;
                    
                case JPEG_TABLES:
                    if (type != UNDEFINED) {
                        throw new IOException("JPEG_TABLES has illegal type = " + type + "\n");
                    }
                    
                    jpegTables = new byte[count];
                    for (i1 = 0; i1 < count; i1++) {
                        jpegTables[i1] = (byte)valueArray[i1];
                    }
                    
                    tableStream = new JPEGInputStream(new ByteArrayInputStream(jpegTables));
                    
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: JPEG tables are above\n", Preferences.DEBUG_FILEIO);
                    }
                    
                    break;
                    
                case EXIFIFD:
                    if (type != LONG) {
                        throw new IOException("EXIFIFD has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFIFD has illegal count = " + count + "\n");
                    }
                    
                    if (debuggingFileIO) {
                        Preferences.debug("EXIF private directory located at " + valueArray[0] + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }
                    
                    preExifLocus = raFile.getFilePointer();
                    raFile.seek(valueArray[0]);
                    exifDirEntries = getUnsignedShort(endianess);
                    if (debuggingFileIO) {
                        Preferences.debug("\nOpenIFD: Exif directory entries = " + exifDirEntries +
                                          "\n", Preferences.DEBUG_FILEIO);
                    }
                    if (exifDirEntries <= 0) {
                        raFile.seek(preExifLocus);
                    }
                    iExifStart = i;
                    
                    break;
                    
                case EXIFTAG_EXPOSURE_TIME:
                    if (type != RATIONAL) {
                        throw new IOException("EXIFTAG_EXPOSURE_TIME has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("EXIFTAG_EXPOSURE_TIME has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    exposureTime = (float) numerator / denominator;
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: Exposure time = " + exposureTime  +
                                          " seconds\n", Preferences.DEBUG_FILEIO);
                    }
                    break;
                    
                case EXIFTAG_FNUMBER:
                    if (type != RATIONAL) {
                        throw new IOException("EXIFTAG_FNUMBER has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("EXIFTAG_FNUMBER has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    fNumber = (float) numerator / denominator;
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: F number = " + fNumber  +
                                          "\n", Preferences.DEBUG_FILEIO);
                    }
                    break;
                    
                case EXIFTAG_EXPOSURE_PROGRAM:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_EXPOSURE_PROGRAM has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_EXPOSURE_PRoGRAM has illegal count = " + count + "\n");
                    }
                    
                    exposureProgram = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch (exposureProgram) {
                            case 0:
                                Preferences.debug("OpenIFD: Exposure program is not defined\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 1:
                                Preferences.debug("OpenIFD: Exposure program is manual\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 2:
                                Preferences.debug("OpenIFD: Exposure program is normal program\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 3:
                                Preferences.debug("OpenIFD: Exposure program is aperture priority\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 4:
                                Preferences.debug("OpenIFD: Exposure program is shutter priority\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 5:
                                Preferences.debug("OpenIFD: Exposure program is creative program\n" +
                                                  "Biased toward depth of field\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 6:
                                Preferences.debug("OpenIFD: Exposure program is action program\n" +
                                                  "Biased towad fast shutter speed\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 7:
                                Preferences.debug("OpenIFD: Exposure program is portrait mode\n" +
                                                  "For closeup photos with the background out of focus\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 8:
                                Preferences.debug("OpenIFD: Exposure program is landscape mode\n" +
                                                  "For landscape photos with the background in focus\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("OpenIFD: Exposure program is " + exposureProgram + 
                                                  ", an unrecognized value\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    break;
                    
                case EXIFTAG_ISO_SPEED_RATINGS:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_ISO_SPEED_RATINGS has illegal type = " + type + "\n");
                    }
                    
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: ISO speed ratings are above\n", Preferences.DEBUG_FILEIO);
                    }
                    
                    isoSpeedRatings = new short[count];
                    for (i1 = 0; i1 < count; i1++) {
                        isoSpeedRatings[i1] = (short)valueArray[i1];
                    }
                    
                    break;
                    
                case EXIFTAG_EXIF_VERSION:
                    if (type != UNDEFINED) {
                        throw new IOException("EXIFTAG_EXIF_VERSION has illegal type = " + type + "\n");
                    }
                    
                    if (count != 4) {
                        throw new IOException("EXIFTAG_EXIF_VERSION has illegal count = " + count + "\n");
                    }
                    
                    exifVersion = new byte[4];
                    for (i1 = 0; i1 < 4; i1++) {
                        exifVersion[i1] = (byte)valueArray[i1];
                    }
                    str = new String(exifVersion);
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: EXIF_VERSION = " + str + "\n",
                                           Preferences.DEBUG_FILEIO);
                    }   
                    break;
                    
                case EXIFTAG_DATE_TIME_ORIGINAL:
                    if (type != ASCII) {
                        throw new IOException("EXIFTAG_DATE_TIME_ORIGINAL has illegal type = " + type + "\n");
                    }
                    
                    if (count != 20) {
                        throw new IOException("EXIFTAG_DATE_TIME_ORIGINAL has illegal count = " + count + "\n");
                    }
                    
                    dateTimeOriginal = new byte[20];
                    for (i1 = 0; i1 < 20; i1++) {
                        dateTimeOriginal[i1] = (byte)valueArray[i1];
                    }
                    str = new String(dateTimeOriginal);
                    
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: Date and time original image generated = " + str.trim() + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }
                    break;
                    
                case EXIFTAG_DATE_TIME_DIGITIZED:
                    if (type != ASCII) {
                        throw new IOException("EXIFTAG_DATE_TIME_DIGITIZED has illegal type = " + type + "\n");
                    }
                    
                    if (count != 20) {
                        throw new IOException("EXIFTAG_DATE_TIME_DIGITIZED has illegal count = " + count + "\n");
                    }
                    
                    dateTimeDigitized = new byte[20];
                    for (i1 = 0; i1 < 20; i1++) {
                        dateTimeDigitized[i1] = (byte)valueArray[i1];
                    }
                    str = new String(dateTimeDigitized);
                    
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: Date and time image digitized = " + str.trim() + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }
                    break;
                    
                case EXIFTAG_EXPOSURE_BIAS_VALUE:
                    if (type != SRATIONAL) {
                        throw new IOException("EXIFTAG_EXPOSURE_BIAS_VALUE has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("EXIFTAG_EXPOSURE_BIAS_VALUE has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    exposureBias = (float) numerator / denominator;
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: Exposure bias = " + exposureBias  +
                                          "\n", Preferences.DEBUG_FILEIO);
                    }
                    break;
                    
                case EXIFTAG_MAX_APERTURE_VALUE:
                    if (type != RATIONAL) {
                        throw new IOException("EXIFTAG_MAX_APERTURE_VALUE has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("EXIFTAG_MAX_APERTURE_VALUE has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    maxAperture = (float) numerator / denominator;
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: Smallest lens F number = " + maxAperture  +
                                          "\n", Preferences.DEBUG_FILEIO);
                    }
                    break;
                    
                case EXIFTAG_METERING_MODE:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_METERING_MODE has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_METERING_MODE has illegal count = " + count + "\n");
                    }
                    
                    meteringMode = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(meteringMode) {
                            case 0:
                                Preferences.debug("OpenIFD: Metering mode is unknown\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 1:
                                Preferences.debug("OpenIFD: Metering mode is average\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 2:
                                Preferences.debug("OpenIFD: Metering mode is center weighted average\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 3:
                                Preferences.debug("OpenIFD: Metering mode is spot\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 4:
                                Preferences.debug("OpenIFD: Metering mode is multispot\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 5:
                                Preferences.debug("OpenIFD: Metering mode is pattern\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 6:
                                Preferences.debug("OpenIFD: Metering mode is partial\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 255:
                                Preferences.debug("OpenIFD: Metering mode is other\n", Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("OpenIFD: Metering mode has unrecognized value = " + 
                                                  meteringMode + "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case EXIFTAG_LIGHT_SOURCE:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_LIGHT_SOURCE has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_LIGHT_SOURCE has illegal count = " + count + "\n");
                    }
                    
                    lightSource = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(lightSource) {
                            case 0:
                                Preferences.debug("OpenIFD: Light source is unknown\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 1:
                                Preferences.debug("OpenIFD: Light source is daylight\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 2:
                                Preferences.debug("OpenIFD: Light source is fluorescent\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 3:
                                Preferences.debug("OpenIFD: Light source is tungsten(incandescent light)\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 4:
                                Preferences.debug("OpenIFD: Light source is flash\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 9:
                                Preferences.debug("OpenIFD: Light source is fine weather\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 10:
                                Preferences.debug("OpenIFD: Light source is cloudy weather\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 11:
                                Preferences.debug("OpenIFD: Light source is shade\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 12:
                                Preferences.debug("OpenIFD: Light source is daylight fluorescent(D 5700 - 7100K)\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 13:
                                Preferences.debug("OpenIFD: Light source is day white fluorescent(N 4600 - 5400K)\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 14:
                                Preferences.debug("OpenIFD: Light source is cool white fluorescent(W 3900 - 4500K)\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 15:
                                Preferences.debug("OpenIFD: Light source is white fluorescent(WW 3200 - 3700K)\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 17:
                                Preferences.debug("OpenIFD: Light source is standard light A\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 18:
                                Preferences.debug("OpenIFD: Light source is standard light B\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 19:
                                Preferences.debug("OpenIFD: Light source is standard light C\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 20:
                                Preferences.debug("OpenIFD: Light source is D55\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 21:
                                Preferences.debug("OpenIFD: Light source is D65\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 22:
                                Preferences.debug("OpenIFD: Light source is D75\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 23:
                                Preferences.debug("OpenIFD: Light source is D50\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 24:
                                Preferences.debug("OpenIFD: Light source is ISO studio tungsten\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 255:
                                Preferences.debug("OpenIFD: Light source is other light source\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("OpenIFD: Light source has unrecognized value = " +
                                                   lightSource + "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case EXIFTAG_FLASH:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_FLASH has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_FLASH has illegal count = " + count + "\n");
                    }
                    
                    flash = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(flash) {
                            case 0x0:
                                Preferences.debug("OpenIFD: Flash did not fire\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 0x1:
                                Preferences.debug("OpenIFD: Flash fired\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 0x5:
                                Preferences.debug("OpenIFD: Strobe return light not detected\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 0x7:
                                Preferences.debug("OpenIFD: Strobe return light detected\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 0x9:
                                Preferences.debug("OpenIFD: Flash fired, compulsory flash mode\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 0xD:
                                Preferences.debug("OpenIFD: Flash fired, compulsory flash mode\n" +
                                                  "Return light not detected\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 0xF:
                                Preferences.debug("OpenIFD: Flash fired, compulsory flash mode\n" +
                                                  "Return light detected\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 0x10:
                                Preferences.debug("OpenIFD: Flash did not fire, compulsory flash mode\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 0x18:
                                Preferences.debug("OpenIFD: Flash did not fire, auto mode\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 0x19:
                                Preferences.debug("OpenIFD: Flash fired, auto mode\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 0x1D:
                                Preferences.debug("OpenIFD: Flash fired, auto mode, return light not detected\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 0x1F:
                                Preferences.debug("OpenIFD: Flash fired, auto mode, return light detected\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 0x20:
                                Preferences.debug("OpenIFD: No flash function\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 0x41:
                                Preferences.debug("OpenIFD: Flash fired, red-eye reduction mode\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 0x45:
                                Preferences.debug("OpenIFD: Flash fired, red-eye reduction mode\n" +
                                                  "Return light not detected\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 0x47:
                                Preferences.debug("OpenIFD: Flash fired, red-eye reduction mode\n" +
                                                  "Return light detected\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 0x49:
                                Preferences.debug("OpenIFD: Flash fired, compulsory flash mode\n" +
                                                  "Red-eye reduction mode\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 0x4D:
                                Preferences.debug("OpenIFD: Flash fired, compulsory flash mode\n" +
                                                  "red-eye reduction mode, return light not detected\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 0x4F:
                                Preferences.debug("OpenIFD: Flash fired, compulsory flash mode\n" +
                                                  "red-eye reduction mode, return light detected\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 0x59:
                                Preferences.debug("OpenIFD: Flash fired, auto mode, red-eye reduction mode\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 0x5D:
                                Preferences.debug("OpenIFD: Flash fired, auto mode, return light not detected\n" +
                                                   "Red-eye reduction mode\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 0x5F:
                                Preferences.debug("OpenIFD: Flash fired, auto mode, return light detected\n" +
                                                  "Red-eye reduction mode\n", Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("OpenIFD: Flash has unrecognized value = " + flash + "\n",
                                                  Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case EXIFTAG_FOCAL_LENGTH:
                    if (type != RATIONAL) {
                        throw new IOException("EXIFTAG_FOCAL_LENGTH has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("EXIFTAG_FOCAL_LENGTH has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    focalLength = (float) numerator / denominator;
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: Focal length of lens = " + focalLength  +
                                          " millimeters\n", Preferences.DEBUG_FILEIO);
                    }
                    break;
                    
                case EXIFTAG_MAKER_NOTE:
                    if (type != UNDEFINED) {
                        throw new IOException("EXIFTAG_MAKER_NOTE has illegal type = " + type + "\n");
                    }
                    
                    makerNote = new byte[count];
                    for (i1 = 0; i1 < count; i1++) {
                        makerNote[i1] = (byte)valueArray[i1];
                    }
                    str = new String(makerNote);
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: Maker note = " + str.trim() + "\n",
                                           Preferences.DEBUG_FILEIO);
                    }   
                    break;
                    
                case EXIFTAG_USER_COMMENT:
                    if (type != UNDEFINED) {
                        throw new IOException("EXIFTAG_USER_COMMENT has illegal type = " + type + "\n");
                    }
                    
                    characterCode = new byte[8];
                    zero = true;
                    for (i1 = 0; i1 < 8; i1++) {
                        characterCode[i1] = (byte)valueArray[i1];
                        if (characterCode[i1] != 0) {
                            zero = false;
                        }
                    }
                    
                    if (zero) {
                        if (debuggingFileIO) {
                            Preferences.debug("OpenIFD: Character code for user comment is undefined\n",
                                              Preferences.DEBUG_FILEIO);
                        }
                    }
                    else {
                        str = new String(characterCode);
                        if (debuggingFileIO) {
                            Preferences.debug("OpenIFD: Character code for user comment = " + str.trim() + "\n",
                                               Preferences.DEBUG_FILEIO);
                        } 
                        
                        if (str.trim().equals("ASCII")) {
                            for (i1 = 0; i1 < count - 8; i1++) {
                                userComment[i1] = (byte)valueArray[i1+8];
                            }
                            str = new String(userComment);
                            if (debuggingFileIO) {
                                Preferences.debug("OpenIFD: User comment = " + str.trim() + "\n",
                                                  Preferences.DEBUG_FILEIO);
                            }
                        }
                        else if (str.trim().equals("UNICODE")) {
                            for (i1 = 0; i1 < count - 8; i1++) {
                                userComment[i1] = (byte)valueArray[i1+8];
                            }
                            str = new String(userComment, 0, count-8, "UTF_16BE");
                            if (debuggingFileIO) {
                                Preferences.debug("OpenIFD: User comment = " + str.trim() + "\n",
                                                  Preferences.DEBUG_FILEIO);
                            }
                        }
                    }
                    break;
                    
                case EXIFTAG_FLASHPIX_VERSION:
                    if (type != UNDEFINED) {
                        throw new IOException("EXIFTAG_FLASHPIX_VERSION has illegal type = " + type + "\n");
                    }
                    
                    if (count != 4) {
                        throw new IOException("EXIFTAG_FLASHPIX_VERSION has illegal count = " + count + "\n");
                    }
                    
                    flashPixVersion = new byte[4];
                    for (i1 = 0; i1 < 4; i1++) {
                        flashPixVersion[i1] = (byte)valueArray[i1];
                    }
                    str = new String(flashPixVersion);
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: Flashpix Format Version = " + str + "\n",
                                           Preferences.DEBUG_FILEIO);
                    }   
                    break;
                    
                case EXIFTAG_COLOR_SPACE:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_COLOR_SPACE has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_COLOR_SPACE has illegal count = " + count + "\n");
                    }
                    
                    colorSpace = (int)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(colorSpace) {
                            case 1:
                                Preferences.debug("OpenIFD: EXIFTAG_COlOR_SPACE = sRGB\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 0xffff:
                                Preferences.debug("OpenIFD: EXIFTAG_COLOR_SPACE = uncalibrated\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("OpenIFD: EXIFTAG_COLOR_SPACE has an unrecognized value = " +
                                                   colorSpace + "\n",Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case EXIFTAG_FILE_SOURCE:
                    if (type != UNDEFINED) {
                        throw new IOException("EXIFTAG_FILE_SOURCE has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_FILE_SOURCE has illegal count = " + count + "\n");
                    }
                    
                    fileSource = (int)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(fileSource) {
                            case 3:
                                Preferences.debug("OpenIFD: Image was recorded on a DSC\n", Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("OpenIFD: EXIFTAG_FILE_SOURCE has an unrecognized value = " +
                                                   fileSource + "\n",Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case EXIFTAG_SCENE_TYPE:
                    if (type != UNDEFINED) {
                        throw new IOException("EXIFTAG_SCENE_TYPE has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_SCENE_TYPE has illegal count = " + count + "\n");
                    }
                    
                    sceneType = (int)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(sceneType) {
                            case 1:
                                Preferences.debug("OpenIFD: Image was directly photographed\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("OpenIFD: EXIFTAG_SCENE_TYPE has an unrecognized value = " +
                                                   sceneType + "\n",Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case EXIFTAG_CUSTOM_RENDERED:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_CUSTOM_RENDERED has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_CUSTOM_RENDERED has illegal count = " + count + "\n");
                    }
                    
                    customRendered = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(customRendered) {
                            case 0:
                                Preferences.debug("OpenIFD: Normal processing on image data\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 1:
                                Preferences.debug("OpenIFD: Custom processing on image data\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("OpenIFD: EXIFTAG_CUSTOM_RENDERED has unrecognized value = " +
                                                  customRendered + "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case EXIFTAG_EXPOSURE_MODE:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_EXPOSURE_MODE has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_EXPOSURE_MODE has illegal count = " + count + "\n");
                    }
                    
                    exposureMode = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(exposureMode) {
                            case 0:
                                Preferences.debug("OpenIFD: Exposure mode is auto exposure\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 1:
                                Preferences.debug("OpenIFD: Exposure mode is manual exposure\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 2:
                                Preferences.debug("OpenIFD: Exposure mode is auto bracket\n" +
                                        "In auto-bracketing mode the camera shoots a series of frames at\n" +
                                        "different exposure settings\n", Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("OpenIFD: EXIFTAG_EXPOSURE_MODE has unrecognized value = " +
                                                  exposureMode + "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case EXIFTAG_WHITE_BALANCE:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_WHITE_BALANCE has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_WHITE_BALANCE has illegal count = " + count + "\n");
                    }
                    
                    whiteBalance = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(whiteBalance) {
                            case 0:
                                Preferences.debug("OpenIFD: White balance mode is auto white balance\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 1:
                                Preferences.debug("OpenIFD: White balance mode is manual white balance\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("OpenIFD: EXIFTAG_WHITE_BALANCE has unrecognized value = " +
                                                  whiteBalance + "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case EXIFTAG_DIGITAL_ZOOM_RATIO:
                    if (type != RATIONAL) {
                        throw new IOException("EXIFTAG_DIGITAL_ZOOM_RATIO has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("EXIFTAG_DIGITAL_ZOOM_RATIO has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    digitalZoomRatio = (float) numerator / denominator;
                    if (debuggingFileIO) {
                        if (numerator == 0) {
                            Preferences.debug("OpneIFD: Digital zoom was not used\n", Preferences.DEBUG_FILEIO);
                        }
                        else {
                        Preferences.debug("OpenIFD: Digital zoom ratio = " + digitalZoomRatio  +
                                          "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    break;
                    
                case EXIFTAG_SCENE_CAPTURE_TYPE:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_SCENE_CAPTURE_TYPE has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_SCENE_CAPTURE_TYPE has illegal count = " + count + "\n");
                    }
                    
                    sceneCaptureType = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(sceneCaptureType) {
                            case 0:
                                Preferences.debug("OpenIFD: Scene capture type is standard\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 1:
                                Preferences.debug("OpenIFD: Scene capture type is landscape\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 2:
                                Preferences.debug("OpenIFD: Scene capture type is portrait\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 3:
                                Preferences.debug("OpenIFD: Scene capture type is night scene\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("OpenIFD: EXIFTAG_SCENE_CAPTURE_TYPE has unrecognized value = " +
                                                  sceneCaptureType + "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case EXIFTAG_GAIN_CONTROL:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_GAIN_CONTROL has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_GAIN_CONTROL has illegal count = " + count + "\n");
                    }
                    
                    gainControl = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(gainControl) {
                            case 0:
                                Preferences.debug("OpenIFD: Overall image gain adjustment is none\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 1:
                                Preferences.debug("OpenIFD: Overall image gain adjustment is low gain up\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 2:
                                Preferences.debug("OpenIFD: Overall image gain adjustment is high gain up\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 3:
                                Preferences.debug("OpenIFD: Overall image gain adjustment is low gain down\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 4:
                                Preferences.debug("OpenIFD: Overall image gain adjustment is high gain down\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("OpenIFD: EXIFTAG_GAIN_CONTROL has unrecognized value = " +
                                                  gainControl + "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case EXIFTAG_CONTRAST:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_CONTRAST has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_CONTRAST has illegal count = " + count + "\n");
                    }
                    
                    contrast = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(contrast) {
                            case 0:
                                Preferences.debug(
                                "OpenIFD: Direction of contrast processing applied by the camera is normal\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 1:
                                Preferences.debug(
                                "OpenIFD: Direction of contrast processing applied by the camera is soft\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 2:
                                Preferences.debug(
                                "OpenIFD: Direction of contrast processing applied by the camera is hard\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("OpenIFD: EXIFTAG_CONTRAST has unrecognized value = " +
                                                  contrast + "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case EXIFTAG_SATURATION:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_SATURATION has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_SATAURATION has illegal count = " + count + "\n");
                    }
                    
                    saturation = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(saturation) {
                            case 0:
                                Preferences.debug(
                                "OpenIFD: Direction of saturation processing applied by the camera is normal\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 1:
                                Preferences.debug(
                                "OpenIFD: Direction of saturation processing applied by the camera is low saturation\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 2:
                                Preferences.debug(
                                "OpenIFD: Direction of saturation processing applied by the camera is high saturation\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("OpenIFD: EXIFTAG_SATURATION has unrecognized value = " +
                                                  saturation + "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case EXIFTAG_SHARPNESS:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_SHARPNESS has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_SHARPNESS has illegal count = " + count + "\n");
                    }
                    
                    sharpness = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(sharpness) {
                            case 0:
                                Preferences.debug(
                                "OpenIFD: Direction of sharpness processing applied by the camera is normal\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 1:
                                Preferences.debug(
                                "OpenIFD: Direction of sharpness processing applied by the camera is soft\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 2:
                                Preferences.debug(
                                "OpenIFD: Direction of sharpness processing applied by the camera is hard\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("OpenIFD: EXIFTAG_SHARPNESS has unrecognized value = " +
                                                  sharpness + "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case EXIFTAG_SHUTTER_SPEED_VALUE:
                    if (type != SRATIONAL) {
                        throw new IOException("EXIFTAG_SHUTTER_SPEED_VALUE has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("EXIFTAG_SHUTTER_SPEED_VALUE has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    shutterSpeed = (float) numerator / denominator;
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: Shutter speed = " + shutterSpeed  +
                                          "\n", Preferences.DEBUG_FILEIO);
                    }
                    break;
                    
                case EXIFTAG_APERTURE_VALUE:
                    if (type != RATIONAL) {
                        throw new IOException("EXIFTAG_APERTURE_VALUE has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("EXIFTAG_APERTURE_VALUE has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    aperture = (float) numerator / denominator;
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: Aperture = " + aperture  +
                                          "\n", Preferences.DEBUG_FILEIO);
                    }
                    break;
                    
                case EXIFTAG_BRIGHTNESS_VALUE:
                    if (type != SRATIONAL) {
                        throw new IOException("EXIFTAG_BRIGHTNESS_VALUE has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("EXIFTAG_BRIGHTNESS_VALUE has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    brightness = (float) numerator / denominator;
                    if (debuggingFileIO) {
                        if (numerator == 0xffffffff) {
                            Preferences.debug("OpenIFD: Brightness is unknown\n", Preferences.DEBUG_FILEIO);
                        }
                        else {
                            Preferences.debug("OpenIFD: Brightness = " + brightness  +
                                          "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    break;
                    
                case EXIFTAG_FOCAL_PLANE_X_RESOLUTION:
                    if (type != RATIONAL) {
                        throw new IOException("EXIFTAG_FOCAL_PLANE_X_RESOLUTION has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("EXIFTAG_FOCAL_PLANE_X_RESOLUTION has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    focalPlaneXResolution = (float) numerator / denominator;
                    if (debuggingFileIO) {
                        Preferences.debug(
                        "OpenIFD: Number of pixels in the image width direction\n" +
                        "per focal plane resolution unit on the camera focal plane = " 
                                          + focalPlaneXResolution  + "\n", Preferences.DEBUG_FILEIO);
                    }
                    break;
                  
                case EXIFTAG_FOCAL_PLANE_Y_RESOLUTION:
                    if (type != RATIONAL) {
                        throw new IOException("EXIFTAG_FOCAL_PLANE_Y_RESOLUTION has illegal type = " + type + "\n");
                    }

                    if (count != 1) {
                        throw new IOException("EXIFTAG_FOCAL_PLANE_Y_RESOLUTION has illegal count = " + count + "\n");
                    }

                    numerator = valueArray[0];
                    denominator = valueArray[1];
                    focalPlaneYResolution = (float) numerator / denominator;
                    if (debuggingFileIO) {
                        Preferences.debug(
                        "OpenIFD: Number of pixels in the image height direction\n" +
                        "per focal plane resolution unit on the camera focal plane = " 
                                          + focalPlaneYResolution  + "\n", Preferences.DEBUG_FILEIO);
                    }
                    break;
                    
                case EXIFTAG_FOCAL_PLANE_RESOLUTION_UNIT:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_FOCAL_PLANE_RESOLUTION_UNIT has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_FOCAL_PLANE_RESOLUTION_UNIT has illegal count = " + count + "\n");
                    }
                    
                    focalPlaneResolutionUnit = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(focalPlaneResolutionUnit) {
                            case 1:
                                Preferences.debug("Focal plane resolution unit has no absolute unit of measurement\n",
                                                   Preferences.DEBUG_FILEIO);
                                break;
                            case 2:
                                Preferences.debug("Focal plane resolution unit = inch\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 3:
                                Preferences.debug("Focal plane resolution unit = centimeter\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("EXIFTAG_FOCAL_PLANE_RESOLUTION_UNIT has unrecognized value = " +
                                                  focalPlaneResolutionUnit + "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case EXIFTAG_SENSING_METHOD:
                    if (type != SHORT) {
                        throw new IOException("EXIFTAG_SENSING_METHOD has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("EXIFTAG_SENSING_METHOD has illegal count = " + count + "\n");
                    }
                    
                    sensingMethod = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(sensingMethod) {
                            case 1:
                                Preferences.debug("OpenIFD: Sensing method is not defined\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 2:
                                Preferences.debug("OpenIFD: One-chip color area sensor\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 3:
                                Preferences.debug("OpenIFD: Two-chip color area sensor\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 4:
                                Preferences.debug("OpenIFD: Three-chip color area sensor\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 5:
                                Preferences.debug("OpenIFD: Color sequential area sensor\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 7:
                                Preferences.debug("OpenIFD: Trilinear sensor\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 8:
                                Preferences.debug("OpenIFD: Color sequential linear sensor\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            default:
                                Preferences.debug("openIFD: EXIFTAG_SENSING_METHOD has unrecognized value = " +
                                                  sensingMethod + "\n");
                        }
                    }
                    
                    break;
                    
                case STONITS:
                    if (type != DOUBLE) {
                        throw new IOException("STONITS has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("STONITS has illegal count = " + count + "\n");
                    }
                    
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: STONITS = " + valueDouble + 
                                          " candelas/meter**2\n", Preferences.DEBUG_FILEIO);
                    }
                    
                    break;
                    
                case GDAL_METADATA:
                    if (type != ASCII) {
                        throw new IOException("GDAL_METADATA has illegal type = " + type + "\n");
                    }

                    gdalMetadata = new byte[count];
                    for (i1 = 0; i1 < count; i1++) {
                        gdalMetadata[i1] = (byte) valueArray[i1];
                    }

                    str = new String(gdalMetadata);
                    if (debuggingFileIO) {
                        Preferences.debug("FileTiff.openIFD: GDAL metadata = " + str.trim() + "\n",
                                          Preferences.DEBUG_FILEIO);
                    }

                    break;
                    
                case BAD_FAX_LINES:
                    if ((type != SHORT) && (type != LONG)) {
                        throw new IOException("BAD_FAX_LINES has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("BAD_FAX_LINES has illegal count = " + count + "\n");
                    }
                    
                    badFaxLines = (int)valueArray[0];
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: " + badFaxLines +
                                " bad fax lines were encountered by the facsimile device\n",
                                          Preferences.DEBUG_FILEIO);
                    }
                    
                    break;
                    
                case CLEAN_FAX_DATA:
                    if (type != SHORT) {
                        throw new IOException("CLEAN_FAX_DATA has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("CLEAN_FAX_DATA has illegal count = " + count + "\n");
                    }
                    
                    cleanFaxData = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(cleanFaxData) {
                            case 0:
                                Preferences.debug("OpenIFD: Tag CLEAN_FAX_DATA: No bad fax lines exist\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 1:
                                Preferences.debug("OpenIFD: Tag CLEAN_FAX_DATA: Bad fax lines exist,\n" +
                                        "but were regenerated by the receiver\n", Preferences.DEBUG_FILEIO);
                                break;
                            case 2:
                                Preferences.debug("OpenIFD: Tag CLEAN_FAX_DATA: Bad fax lines exist,\n" +
                                        "but have not been regenerated\n", Preferences.DEBUG_FILEIO);
                                break;  
                            default:
                                Preferences.debug("OpenIFD: Tag CLEAN_FAX_DATA has unrecognized value = " +
                                       cleanFaxData +  "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case CONSECUTIVE_BAD_FAX_LINES:
                    if ((type != SHORT) && (type != LONG)) {
                        throw new IOException("CONSECUTIVE_BAD_FAX_LINES has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("CONSECUTIVE_BAD_FAX_LINES has illegal count = " + count + "\n");
                    }
                    
                    consecutiveBadFaxLines = (int)valueArray[0];
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: " + consecutiveBadFaxLines +
                                " consecutive bad fax lines were encountered by the facsimile device\n",
                                          Preferences.DEBUG_FILEIO);
                    }
                    
                    break;
                    
                case THRESHHOLDING:
                    if (type != SHORT) {
                        throw new IOException("THRESHHOLDING has illegal type = " + type + "\n");
                    }
                    
                    if (count != 1) {
                        throw new IOException("THRESHHOLDING has illegal count = " + count + "\n");
                    }
                    
                    threshholding = (short)valueArray[0];
                    
                    if (debuggingFileIO) {
                        switch(threshholding) {
                            case 1:
                                Preferences.debug(
                                "OpenIFD: No dithering or halftoning has been applied to the image data\n",
                                                  Preferences.DEBUG_FILEIO);
                                break;
                            case 2:
                                Preferences.debug(
                                "OpenIFD: An ordered dither or halftone technique has been applied to the image data\n",
                                Preferences.DEBUG_FILEIO);
                                break;
                            case 3:
                                Preferences.debug(
                                "OpenIFD: A randomzied process such as error diffusion has been applied\n" +
                                "to the image data\n", Preferences.DEBUG_FILEIO);
                                break;  
                            default:
                                Preferences.debug("OpenIFD: Tag THRESHHOLDING has unrecognized value = " +
                                       threshholding +  "\n", Preferences.DEBUG_FILEIO);
                        }
                    }
                    
                    break;
                    
                case HALFTONE_HINTS:
                    if (type != SHORT) {
                        throw new IOException("HALFTONE_HINTS has illegal type = " + type + "\n");
                    }
                    
                    if (count != 2) {
                        throw new IOException("HALFTONE_HINTS has illegal count = " + count + "\n");
                    }
                    
                    lightHalftone = (short)valueArray[0];
                    darkHalftone = (short)valueArray[1];
                    if (debuggingFileIO) {
                        Preferences.debug("OpenIFD: The highlight gray level which should be halftoned at the\n" +
                                "lightest printable tint of the final output device = " + lightHalftone + "\n",
                                Preferences.DEBUG_FILEIO);
                        Preferences.debug("OpenIFD: The shadow gray level which should be halftoned at the\n" +
                                "darkest printable tint of the final output device = " + darkHalftone + "\n",
                                Preferences.DEBUG_FILEIO);
                    }
                    break;
                    
                case EXTRA_SAMPLES:
                    if (type != SHORT) {
                        throw new IOException("EXTRA_SAMPLES has illegal type = " + type + "\n");
                    }
                    
                    extraSamples = new short[count];
                    for (i1 = 0; i1 < count; i1++) {
                        extraSamples[i1] = (short)valueArray[i1];
                    }
                    
                    if (debuggingFileIO) {
                        for (i1 = 0; i1 < count; i1++) {
                            switch(extraSamples[i1]) {
                                case 0:
                                    Preferences.debug("OpenIFD: Extra sample " + (i1 + 1) + " is unspecified data\n",
                                                      Preferences.DEBUG_FILEIO);
                                    break;
                                case 1:
                                    Preferences.debug("OpenIFD: Extra sample " + (i1 + 1) + 
                                            " is asscociated alpha data(with pre-multiplied color)\n" +
                                            "Associated alpha data is opacity information\n",
                                            Preferences.DEBUG_FILEIO);
                                    break;
                                case 2:
                                    Preferences.debug("OpenIFD: Extra sample " + (i1 + 1) +
                                            " is unassociated alpha data\n" +
                                            "Unassociated alpha data is transparency information that logically\n" +
                                            "exists independent of the image; it is commonly called a soft matte\n",
                                            Preferences.DEBUG_FILEIO);
                                    break;
                                default:
                                    Preferences.debug("OpenIFD: Extra sample " + (i1 + 1) +
                                            " has an unrecognized value = " + extraSamples[i1] + "\n",
                                            Preferences.DEBUG_FILEIO);
                                    
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
            if ((i != 0) && (i == iExifStart + exifDirEntries)) {
                raFile.seek(preExifLocus);
            }
        }

        if (bitsPerSample != null) {

            if (bitsPerSample.length == 1) {

                if (sampleFormat == 1) { // default for unsigned integers

                    switch (bitsPerSample[0]) {

                        case 1:
                            fileInfo.setDataType(ModelStorageBase.BOOLEAN);
                            break;

                        case 2:
                            isBW2 = true;
                            fileInfo.setDataType(ModelStorageBase.UBYTE);
                            break;
                        case 4:
                            isBW4 = true;
                            fileInfo.setDataType(ModelStorageBase.UBYTE);
                            break;
                        case 6:
                            isBW6 = true;
                            fileInfo.setDataType(ModelStorageBase.UBYTE);
                            break;
                        case 8:
                            fileInfo.setDataType(ModelStorageBase.UBYTE);
                            break;
        
                            
                        case 10:
                            isBW10 = true;
                            fileInfo.setDataType(ModelStorageBase.USHORT);
                            break;
                        case 12:
                            isBW12 = true;
                            fileInfo.setDataType(ModelStorageBase.USHORT);
                            break;
                        case 14:
                            isBW14 = true;
                            fileInfo.setDataType(ModelStorageBase.USHORT);
                            break;
                        case 16:
                            fileInfo.setDataType(ModelStorageBase.USHORT);
                            break;

                        case 24:
                            isBW24 = true;
                            fileInfo.setDataType(ModelStorageBase.UINTEGER);
                            break;
                        case 32:
                            fileInfo.setDataType(ModelStorageBase.UINTEGER);
                            break;

                        default:
                            throw new IOException("TIFF Tag BitsPerSample has illegal value = " + bitsPerSample[0]);
                    } // switch(bitsPerSample[0])
                } // if (sampleFormat == 1)
                else if (sampleFormat == 2) { // signed integers

                    switch (bitsPerSample[0]) {

                        case 1:
                            fileInfo.setDataType(ModelStorageBase.BOOLEAN);
                            break;

                        case 8:
                            fileInfo.setDataType(ModelStorageBase.BYTE);
                            break;

                        case 16:
                            fileInfo.setDataType(ModelStorageBase.SHORT);
                            break;

                        case 32:
                            fileInfo.setDataType(ModelStorageBase.INTEGER);
                            break;

                        default:
                            throw new IOException("TIFF Tag BitsPerSample has illegal value = " + bitsPerSample[0]);
                    } // switch(bitsPerSample[0])
                } // else if (sampleFormat == 2)
                else if (sampleFormat == 3) {

                    switch (bitsPerSample[0]) {

                        case 32:
                            fileInfo.setDataType(ModelStorageBase.FLOAT);
                            break;

                        case 64:
                            fileInfo.setDataType(ModelStorageBase.DOUBLE);
                            break;

                        default:
                            throw new IOException("TIFF tag BitsPerSample has illegal value = " + bitsPerSample[0]);
                    } // switch(bitsPerSample[0]
                } // else if (sampleFormat == 3)
            } // if (bitsPerSample.length == 1)
            else if (bitsPerSample.length <= 4) {

                if (sampleFormat == 1) { // default for unsigned integers

                    switch (bitsPerSample[0]) {
                        case 2:
                            isRGB2 = true;
                            fileInfo.setDataType(ModelStorageBase.ARGB);
                            break;
                        case 4:
                            isRGB4 = true;
                            fileInfo.setDataType(ModelStorageBase.ARGB);
                            break;
                        case 8:
                            fileInfo.setDataType(ModelStorageBase.ARGB);
                            break;

                        case 10:
                            isRGB10 = true;
                            fileInfo.setDataType(ModelStorageBase.ARGB_USHORT);
                            break;
                        case 12:
                            isRGB12 = true;
                            fileInfo.setDataType(ModelStorageBase.ARGB_USHORT);
                            break;
                        case 14:
                            isRGB14 = true;
                            fileInfo.setDataType(ModelStorageBase.ARGB_USHORT);
                            break;
                        case 16:
                            fileInfo.setDataType(ModelStorageBase.ARGB_USHORT);
                            break;
                        case 24:
                            isRGB24UINTtoFLOAT = true;
                            Preferences.debug("Ideally should be ARGB_UINTEGER, but MIPAV does not have this type\n");
                            Preferences.debug("so making ARGB_FLOAT\n");
                            fileInfo.setDataType(ModelStorageBase.ARGB_FLOAT);
                            break;
                        case 32:
                            isRGB32UINTtoFLOAT = true;
                            Preferences.debug("Ideally should be ARGB_UINTEGER, but MIPAV does not have this type\n");
                            Preferences.debug("so making ARGB_FLOAT\n");
                            fileInfo.setDataType(ModelStorageBase.ARGB_FLOAT);
                            break;
                        default:
                            throw new IOException("TIFF Tag BitsPerSample has illegal value = " + bitsPerSample[0]);
                    } // switch(bitsPerSample[0])
                } // if (sampleFormat == 1)
                else if (sampleFormat == 2) {
                    switch(bitsPerSample[0]) {
                        case 8:
                            fileInfo.setDataType(ModelStorageBase.ARGB);
                            Preferences.debug("Signed byte color is being treated as unsigned byte color\n");
                            break;
                        case 16:
                            if (SGILogCompression || SGILog24Compression) {
                                fileInfo.setDataType(ModelStorageBase.ARGB);
                                Preferences.debug("Signed short color is being treated as unsigned byte color\n");
                            }
                            else {
                                fileInfo.setDataType(ModelStorageBase.ARGB_USHORT);
                                Preferences.debug("Signed short color is being treated as unsigned short color\n");   
                            }
                            break;
                        default:
                            throw new IOException("TIFF Tag BitsPerSample has illegal value = " + bitsPerSample[0]);
                    }
                    
                } // else if (sampleFormat == 2)
                else if (sampleFormat == 3) {

                    switch (bitsPerSample[0]) {

                        case 32:
                            fileInfo.setDataType(ModelStorageBase.ARGB_FLOAT);
                            break;
                        case 64:
                            Preferences.debug("Ideally should be ARGB_DOUBLE, but MIPAV does not have this type\n");
                            Preferences.debug("so making ARGB_FLOAT\n");
                            fileInfo.setDataType(ModelStorageBase.ARGB_FLOAT);
                            break;

                        default:
                            throw new IOException("TIFF Tag BitsPerSample has illegal value = " + bitsPerSample[0]);
                    } // switch(bitsPerSample[0])
                } // else if (sampleFormat == 3)
            } // else if bitsPerSample.length <= 4
            else { // bitsPerSample.length > 4
                // Portray colors along the last dimension
                if (sampleFormat == 1) {
                    switch(bitsPerSample[0]) {
                        case 8:
                            fileInfo.setDataType(ModelStorageBase.UBYTE);
                            haveMultiSpectraImage = true;
                            break;
                    }
                        
                }
            }
        } // if (bitsPerSample != null)
        
        if ((fileInfo.getPhotometric() == 5) && (inkSet == 1)) {
            isCMYK = true;
        }

        imageSlice++;
        IFDoffsets[imageSlice] = getInt(endianess);

        if (debuggingFileIO) {
            Preferences.debug("\nFileTiff.openIFD: Ref. to next imageSlice = " + IFDoffsets[imageSlice] + "\n",
                              Preferences.DEBUG_FILEIO);
        }

        if ((IFDoffsets[imageSlice] <= 0) || (IFDoffsets[imageSlice] >= fileLength)) {
            return false; // Done reading images
        }
        
        // Make sure the next IFD entry starts with a valid number of directory entries 
        // before considering it as valid.
        saveLocus = raFile.getFilePointer();
        raFile.seek(IFDoffsets[imageSlice]);
        
        nDirEntries = getUnsignedShort(endianess);

        if ((nDirEntries <= 0) || (nDirEntries >= 100)) {
            raFile.seek(saveLocus);
            return false;
        }

        raFile.seek(saveLocus);
        return true; // Read more IFDs (ie. images)
    }

    /**
     * Reads a slice of data at a time and stores the results in the buffer.
     *
     * @param      slice   offset into the file stored in the dataOffset array
     * @param      buffer  buffer where the info is stored
     *
     * @exception  IOException  if there is an error reading the file
     */
    private void readBuffer(int slice, float[] buffer) throws IOException {
        int i, j, a;
        int iCount, iNext;
        int b1, b2, b3, b4;
        long b1L, b2L, b3L, b4L;
        long progress, progressLength, mod;
        byte intermedBuffer[] = null;
        int interLength;
        int in;
        int jSkip;

        // long pointer;
        int idx = 0;
        int nBytes;
        int nLength;
        int totalLength;
        int planarRGB = 0; // Use this for planar RGB where you must read a stripsPerImage

        // number of red strips, followed by a stripsPerImage number of
        // green strips, followed by a stripsPerImage number of blue strips.
        // System.err.println("doing readBuffer()");
        int nIndex = dataOffsets[slice].size();
        int stripsPerImage = (nIndex + 1) / 3; // used for planar RGB
        int tmpInt;

        // System.err.println("Datatype is: " + fileInfo.getDataType());
        // System.err.println("number of data offsets: " + nIndex);
        // System.err.println("buffer length: " + buffer.length);
        // ben mod: try calculating total length you'd need to read in ALL at one time
        int firstIndex = ((Index) (dataOffsets[slice].elementAt(0))).index;
        int lastIndex = ((Index) (dataOffsets[slice].elementAt(nIndex - 1))).index;
        
        // Buffers needed if photometric is YCbCr
        int YBuffer[] = null;
        int CbInBuffer[] = null;
        int CrInBuffer[] = null;
        int CbCrXSize;
        int CbCrYSize;
        int CbCrInSize;
        int Y00;
        int Y01;
        int Y10;
        int Y11;
        int Cb00;
        int Cr00;
        int halfXDim = xDim/2;
        int x = 0;
        int y = 0;
        int c = 0;
        int m;
        
        if (isYCbCr) {
            YBuffer = new int[buffer.length/4];
            CbCrXSize = xDim/YCbCrSubsampleHoriz;
            if ((xDim % YCbCrSubsampleHoriz) != 0) {
                CbCrXSize++;
            }
            CbCrYSize = yDim/YCbCrSubsampleVert;
            if ((yDim % YCbCrSubsampleVert) != 0) {
                CbCrYSize++;
            }
            CbCrInSize = CbCrXSize * CbCrYSize;
            CbInBuffer = new int[CbCrInSize];
            CrInBuffer = new int[CbCrInSize];
        }

        if (((Index) (dataOffsets[slice].elementAt(nIndex - 1))).byteCount == 0) {

            switch (fileInfo.getDataType()) {

                case ModelStorageBase.BOOLEAN:
                    nLength = 8 * ((buffer.length + 63) >> 6);
                    break;

                case ModelStorageBase.BYTE:
                case ModelStorageBase.UBYTE:
                case ModelStorageBase.ARGB:
                    nLength = buffer.length;
                    if (haveMultiSpectraImage) {
                        nLength *= bitsPerSample.length;
                    }
                    break;

                case ModelStorageBase.SHORT:
                case ModelStorageBase.USHORT:
                case ModelStorageBase.ARGB_USHORT:
                    nLength = 2 * buffer.length;
                    break;

                case ModelStorageBase.INTEGER:
                case ModelStorageBase.UINTEGER:
                case ModelStorageBase.FLOAT:
                    nLength = 4 * buffer.length;
                    break;

                default:
                    nLength = buffer.length;
            } // switch (fileInfo.getDataType())

            totalLength = nLength + lastIndex - firstIndex;
        } // if (((Index) (dataOffsets[slice].elementAt(nIndex - 1))).byteCount == 0)
        else {
            totalLength = (lastIndex - firstIndex) + ((Index) (dataOffsets[slice].elementAt(nIndex - 1))).byteCount;
        }

        byteBuffer = new byte[totalLength];

        int currentIndex = 0;

        //System.err.println("first index: " + firstIndex + ", last index: " + lastIndex + ", totalLength: " +
        //totalLength);
        // System.err.println("packbit is: " + packBit);
        raFile.seek(firstIndex);
        raFile.read(byteBuffer, 0, totalLength);
        i = 0;

        for (a = 0; a < nIndex; a++, idx++) {

            try {

                // System.err.println("Seeking to: " + ( (Index) (dataOffsets[slice].elementAt(idx))).index);
                currentIndex = ((Index) (dataOffsets[slice].elementAt(idx))).index - firstIndex;
                //System.out.println("CurrentIndex = " + currentIndex);

                // raFile.seek( ( (Index) (dataOffsets[slice].elementAt(idx))).index);
                nBytes = ((Index) (dataOffsets[slice].elementAt(idx))).byteCount;

                //System.err.println("doing nBytes: " + nBytes);
                if (nBytes == 0) {
                    nBytes = buffer.length;
                }

                ;

                switch (fileInfo.getDataType()) {

                    case ModelStorageBase.BOOLEAN:
                        
                        if (fileInfo.getPhotometric() == 0) {
                            haveChangedPhotometricTo1 = true;
                        }
                        progress = slice * buffer.length;
                        progressLength = buffer.length * imageSlice;
                        mod = progressLength / 10;
                        if (packBit == false) {
                            for (j = 0, m = 0; y < yDim; y++) {
                                for (x = 0; x < xDim; x++, i++) {
                                  if (m == 0) {
                                      buffer[i] = (byteBuffer[j + currentIndex] & 0x80) >>> 7;
                                      if (x < xDim - 1) {
                                          m = 1;
                                      }
                                      else {
                                          m = 0;
                                          j++;
                                      }
                                  } // if (m == 0)
                                  else if (m == 1) {
                                      buffer[i] = (byteBuffer[j + currentIndex] & 0x40) >>> 6;
                                      if (x < xDim - 1) {
                                          m = 2;
                                      }
                                      else {
                                          m = 0;
                                          j++;
                                      }
                                  } // else if (m == 1)
                                  else if (m == 2) {
                                      buffer[i] = (byteBuffer[j + currentIndex] & 0x20) >>> 5;
                                      if (x < xDim - 1) {
                                          m = 3;
                                      }
                                      else {
                                          m = 0;
                                          j++;
                                      }
                                  } // else if (m == 2)
                                  else if (m == 3) {
                                      buffer[i] = (byteBuffer[j + currentIndex] & 0x10) >>> 4;
                                      if (x < xDim - 1) {
                                          m = 4;
                                      }
                                      else {
                                          m = 0;
                                          j++;
                                      }
                                  } // else if (m == 3)
                                  else if (m == 4) {
                                      buffer[i] = (byteBuffer[j + currentIndex] & 0x08) >>> 3;
                                      if (x < xDim - 1) {
                                          m = 5;
                                      }
                                      else {
                                          m = 0;
                                          j++;
                                      }
                                  } // else if (m == 4)
                                  else if (m == 5) {
                                      buffer[i] = (byteBuffer[j + currentIndex] & 0x04) >>> 2;
                                      if (x < xDim - 1) {
                                          m = 6;
                                      }
                                      else {
                                          m = 0;
                                          j++;
                                      }
                                  } // else if (m == 5)
                                  else if (m == 6) {
                                      buffer[i] = (byteBuffer[j + currentIndex] & 0x02) >>> 1;
                                      if (x < xDim - 1) {
                                          m = 7;
                                      }
                                      else {
                                          m = 0;
                                          j++;
                                      }
                                  } // else if (m == 6)
                                  else { // m == 7
                                      buffer[i] = (byteBuffer[j + currentIndex] & 0x01);
                                      m = 0;
                                      j++;
                                  } // else m == 7
                                  if (fileInfo.getPhotometric() == 0) {
                                      // White is zero in oroginal readin
                                      // Convert to black is zero for usual display
                                      buffer[i] = 1 - buffer[i];
                                  }
                                }
                            }
                        } // if (packBit == false)
                        else { // packBit == true
                            interLength = xDim;
                            if ((xDim % 8) != 0) {
                                interLength += 8 - xDim%8;
                            }
                            interLength *= yDim;
                            intermedBuffer = new byte[interLength];
                            j = 0;
                            in = 0;
                            
                            while (j < nBytes) {

                                // uncompressed data bytes follow
                                // Copy the next n+1 bytes literally
                                if ((byteBuffer[j + currentIndex] & 0x80) == 0) {
                                    iCount = byteBuffer[j + currentIndex] + 1;
                                    j++;

                                    for (iNext = 0; iNext < iCount; iNext++, j++, in++) {

                                        intermedBuffer[in] = byteBuffer[j + currentIndex];
                                    }
                                } // end of if (byteBuffer[j] & 0x80 == 0)

                                // Do nothing if the byte value is -128
                                else if (byteBuffer[j + currentIndex] == -128) {
                                    j++;
                                }
                                // compressed data bytes follow
                                // (~byteBuffer[j]) + 1 is the 2's complement of n or -n
                                // Hence (~byteBuffer[j]) + 2 equals -n + 1
                                else {
                                    iCount = (~byteBuffer[j + currentIndex]) + 2;
                                    j++;

                                    for (iNext = 0; iNext < iCount; iNext++, in++) {

                                        intermedBuffer[in] = byteBuffer[j + currentIndex];
                                    }

                                    j++;
                                } // end of else for compressed data bytes
                            } // end of while (j < nBytes)
                            for (j = 0, m = 0; y < yDim; y++) {
                                for (x = 0; x < xDim; x++, i++) {
                                  if (m == 0) {
                                      buffer[i] = (intermedBuffer[j + currentIndex] & 0x80) >>> 7;
                                      if (x < xDim - 1) {
                                          m = 1;
                                      }
                                      else {
                                          m = 0;
                                          j++;
                                      }
                                  } // if (m == 0)
                                  else if (m == 1) {
                                      buffer[i] = (intermedBuffer[j + currentIndex] & 0x40) >>> 6;
                                      if (x < xDim - 1) {
                                          m = 2;
                                      }
                                      else {
                                          m = 0;
                                          j++;
                                      }
                                  } // else if (m == 1)
                                  else if (m == 2) {
                                      buffer[i] = (intermedBuffer[j + currentIndex] & 0x20) >>> 5;
                                      if (x < xDim - 1) {
                                          m = 3;
                                      }
                                      else {
                                          m = 0;
                                          j++;
                                      }
                                  } // else if (m == 2)
                                  else if (m == 3) {
                                      buffer[i] = (intermedBuffer[j + currentIndex] & 0x10) >>> 4;
                                      if (x < xDim - 1) {
                                          m = 4;
                                      }
                                      else {
                                          m = 0;
                                          j++;
                                      }
                                  } // else if (m == 3)
                                  else if (m == 4) {
                                      buffer[i] = (intermedBuffer[j + currentIndex] & 0x08) >>> 3;
                                      if (x < xDim - 1) {
                                          m = 5;
                                      }
                                      else {
                                          m = 0;
                                          j++;
                                      }
                                  } // else if (m == 4)
                                  else if (m == 5) {
                                      buffer[i] = (intermedBuffer[j + currentIndex] & 0x04) >>> 2;
                                      if (x < xDim - 1) {
                                          m = 6;
                                      }
                                      else {
                                          m = 0;
                                          j++;
                                      }
                                  } // else if (m == 5)
                                  else if (m == 6) {
                                      buffer[i] = (intermedBuffer[j + currentIndex] & 0x02) >>> 1;
                                      if (x < xDim - 1) {
                                          m = 7;
                                      }
                                      else {
                                          m = 0;
                                          j++;
                                      }
                                  } // else if (m == 6)
                                  else { // m == 7
                                      buffer[i] = (intermedBuffer[j + currentIndex] & 0x01);
                                      m = 0;
                                      j++;
                                  } // else m == 7
                                  if (fileInfo.getPhotometric() == 0) {
                                      // White is zero in oroginal readin
                                      // Convert to black is zero for usual display
                                      buffer[i] = 1 - buffer[i];
                                  }
                                }
                            }
                        } // else packBit == true
                        break;

                    case ModelStorageBase.BYTE:
                        if (packBit == false) {

                            // if (byteBuffer == null)
                            // byteBuffer = new byte[buffer.length];
                            // raFile.read(byteBuffer, 0, nBytes);
                            progress = slice * buffer.length;
                            progressLength = buffer.length * imageSlice;
                            mod = progressLength / 10;


                            for (j = 0; j < nBytes; j++, i++) {

                                if (((i + progress) % mod) == 0) {
                                    fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                }

                                buffer[i] = byteBuffer[j + currentIndex];
                            }
                        } // if (packBit == false)
                        else if (packBit == true) {

                            // byteBuffer = new byte[nBytes];
                            // raFile.read(byteBuffer, 0, nBytes);
                            progress = slice * buffer.length;
                            progressLength = buffer.length * imageSlice;
                            mod = progressLength / 100;

                            j = 0;

                            while (j < nBytes) {

                                // uncompressed data bytes follow
                                // Copy the next n+1 bytes literally
                                if ((byteBuffer[j + currentIndex] & 0x80) == 0) {
                                    iCount = byteBuffer[j + currentIndex] + 1;
                                    j++;

                                    for (iNext = 0; iNext < iCount; iNext++, j++, i++) {

                                        if (((i + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                    progressLength * 100));
                                        }

                                        buffer[i] = byteBuffer[j + currentIndex];
                                    }
                                } // end of if (byteBuffer[j] & 0x80 == 0)

                                // Do nothing if the byte value is -128
                                else if (byteBuffer[j + currentIndex] == -128) {
                                    j++;
                                }
                                // compressed data bytes follow
                                // (~byteBuffer[j]) + 1 is the 2's complement of n or -n
                                // Hence (~byteBuffer[j]) + 2 equals -n + 1
                                else {
                                    iCount = (~byteBuffer[j + currentIndex]) + 2;
                                    j++;

                                    for (iNext = 0; iNext < iCount; iNext++, i++) {

                                        if (((i + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                    progressLength * 100));
                                        }

                                        buffer[i] = byteBuffer[j + currentIndex];
                                    }

                                    j++;
                                } // end of else for compressed data bytes
                            } // end of while (j < nBytes)
                        } // end of else if (packBit == true)

                        break;

                    case ModelStorageBase.UBYTE:
                        if (packBit == false) {

                            // if (byteBuffer == null)
                            // byteBuffer = new byte[buffer.length];
                            // raFile.read(byteBuffer, 0, nBytes);
                            progress = slice * buffer.length;
                            progressLength = buffer.length * imageSlice;
                            mod = progressLength / 100;
                            jSkip = 0;
                            if ((totalLength > xDim*yDim*bitsPerSample.length) && haveRowsPerStrip &&
                                    ((yDim % rowsPerStrip) != 0)) {
                                    jSkip = xDim * (rowsPerStrip - yDim%rowsPerStrip);
                            }
                            if (haveMultiSpectraImage  && (!chunky)) {
                                for (j = 0; c < bitsPerSample.length; c++) {
                                    for (y = 0; y < yDim; y++) {
                                        for (x = 0; x < xDim; x++, i++, j++) {
                                            if (((i + progress) % mod) == 0) {
                                                fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                            }
                                            buffer[i] = getUnsignedByte(byteBuffer, j + currentIndex);
                                        }
                                    }
                                    j += jSkip;
                                }
                            } // if (haveMultiSpectraImage && (!chunky))
                            else if (isBW2) {
                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i++) {
    
                                        if (((i + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                        }
                                        
                                        if (m == 0) {
                                            buffer[i] = (byteBuffer[j + currentIndex] & 0xc0) >>> 6;
                                            if (x < xDim - 1) {
                                                m = 1;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // if (m == 0)
                                        else if (m == 1) {
                                            buffer[i] = (byteBuffer[j + currentIndex] & 0x30) >>> 4;
                                            if (x < xDim - 1) {
                                                m = 2;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // else if (m == 1)
                                        else if (m == 2) {
                                            buffer[i] = (byteBuffer[j + currentIndex] & 0x0c) >>> 2;
                                            if (x < xDim - 1) {
                                                m = 3;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // else if (m == 2)
                                        else { // m == 3
                                            buffer[i] = (byteBuffer[j + currentIndex] & 0x03);
                                            m = 0;
                                            j++;
                                        } // else m == 3
                                    } 
                                }
                            } // else if (isBW2)
                            else if (isBW4) {
                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i++) {
    
                                        if (((i + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                        }
                                        
                                        if (m == 0) {
                                            buffer[i] = (byteBuffer[j + currentIndex] & 0xf0) >>> 4;
                                            if (x < xDim - 1) {
                                                m = 1;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // if (m == 0)
                                        else { // m == 1
                                            buffer[i] = (byteBuffer[j + currentIndex] & 0x0f);
                                            m = 0;
                                            j++;
                                        } // else m == 1
                                    } 
                                }    
                            } // else if (isBW4)
                            else if (isBW6) {
                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i++) {
    
                                        if (((i + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                        }
    
                                        if (m == 0) {
                                            buffer[i] = (byteBuffer[j + currentIndex] & 0xfc) >>> 2;
                                            m = 1;
                                            if (x == (xDim - 1)) {
                                                m = 0;
                                                j++;
                                            }
                                        }
                                        else if (m == 1) {
                                            buffer[i] = ((byteBuffer[j + currentIndex] & 0x03) << 4) |
                                                        ((byteBuffer[j + 1 + currentIndex] & 0xf0) >>> 4);
                                            if (x < xDim - 1) {
                                                m = 2;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 2) {
                                            buffer[i] = ((byteBuffer[j + currentIndex] & 0x0f) << 2) |
                                                        ((byteBuffer[j + 1 + currentIndex] & 0xc0)  >>> 6);
                                            if (x < xDim - 1) {
                                                m = 3;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 3) {
                                            buffer[i] = (byteBuffer[j + currentIndex] & 0x3f);
                                            m = 0;
                                            j++;
                                        }
                                    } 
                                } 
                            } // else if (isBW6)
                            else { 
                                if (fileInfo.getPhotometric() == 0) {
                                    haveChangedPhotometricTo1 = true;
                                }
                                for (j = 0; j < nBytes; j++, i++) {
    
                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                    }
    
                                    buffer[i] = byteBuffer[j + currentIndex] & 0xff;
                                    if (fileInfo.getPhotometric() == 0) {
                                        // White is zero in original readin
                                        // Convert to black is zero for usual display
                                        buffer[i] = 255 - buffer[i];
                                    }
                                    
                                }
                            } // else 
                        } // if (packBit == false)
                        else if (packBit == true) {

                            // System.err.println("doing packbit UBYTE");
                            // byteBuffer = new byte[nBytes];
                            // raFile.read(byteBuffer, 0, nBytes);
                            progress = slice * buffer.length;
                            progressLength = buffer.length * imageSlice;
                            mod = progressLength / 100;

                            j = 0;

                            while (j < nBytes) {

                                // uncompressed data bytes follow
                                // Copy the next n+1 bytes literally
                                if ((byteBuffer[j + currentIndex] & 0x80) == 0) {
                                    iCount = byteBuffer[j + currentIndex] + 1;
                                    j++;

                                    for (iNext = 0; iNext < iCount; iNext++, j++, i++) {

                                        if (((i + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                    progressLength * 100));
                                        }

                                        buffer[i] = byteBuffer[j + currentIndex] & 0xff;
                                        if (fileInfo.getPhotometric() == 0) {
                                            // White is zero in original readin
                                            // Convert to black is zero for usual display
                                            buffer[i] = 255 - buffer[i];
                                        }
                                    }
                                } // end of if (byteBuffer[j] & 0x80 == 0)

                                // Do nothing if the byte value is -128
                                else if (byteBuffer[j + currentIndex] == -128) {
                                    j++;
                                }
                                // compressed data bytes follow
                                // (~byteBuffer[j]) + 1 is the 2's complement of n or -n
                                // Hence (~byteBuffer[j]) + 2 equals -n + 1
                                else {
                                    iCount = (~byteBuffer[j + currentIndex]) + 2;
                                    j++;

                                    for (iNext = 0; iNext < iCount; iNext++, i++) {

                                        if (((i + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                    progressLength * 100));
                                        }

                                        buffer[i] = byteBuffer[j + currentIndex] & 0xff;
                                        if (fileInfo.getPhotometric() == 0) {
                                            // White is zero in original readin
                                            // Convert to black is zero for usual display
                                            buffer[i] = 255 - buffer[i];
                                        }
                                    }

                                    j++;
                                } // end of else for compressed data bytes
                            } // end of while (j < nBytes)
                        } // end of else if (packBit == true)

                        break;

                    case ModelStorageBase.SHORT:

                        // if (byteBuffer == null) {
                        // byteBuffer = new byte[2 * buffer.length];
                        // }
                        // raFile.read(byteBuffer, 0, nBytes);
                        progress = slice * buffer.length;
                        progressLength = buffer.length * imageSlice;
                        mod = progressLength / 10;

                        for (j = 0; j < nBytes; j += 2, i++) {

                            if (((i + progress) % mod) == 0) {
                                fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                            }

                            b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                            b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);

                            if (endianess) {
                                buffer[i] = (short) ((b1 << 8) + b2);
                            } else {
                                buffer[i] = (short) ((b2 << 8) + b1);
                            }
                        }

                        break;

                    case ModelStorageBase.USHORT:

                        // if (byteBuffer == null) {
                        // byteBuffer = new byte[2 * buffer.length];
                        // }
                        // raFile.read(byteBuffer, 0, nBytes);
                        progress = slice * buffer.length;
                        progressLength = buffer.length * imageSlice;
                        mod = progressLength / 10;
                        
                        if (isBW10 && endianess) {
                            for (j = 0, m = 0; y < yDim; y++) {
                                for (x = 0; x < xDim; x++, i++) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                    }

                                    if (m == 0) {
                                        buffer[i] = ((byteBuffer[j + currentIndex] & 0xff) << 2) |
                                                    ((byteBuffer[j + currentIndex + 1] & 0xc0) >>> 6);
                                        if (x < xDim - 1) {
                                            m = 1;
                                            j++;
                                        }
                                        else {
                                            m = 0;
                                            j += 2;
                                        }
                                    }
                                    else if (m == 1) {
                                        buffer[i] = ((byteBuffer[j + currentIndex] & 0x3f) << 4) |
                                                    ((byteBuffer[j + 1 + currentIndex] & 0xf0) >>> 4);
                                        if (x < xDim - 1) {
                                            m = 2;
                                            j++;
                                        }
                                        else {
                                            m = 0;
                                            j += 2;
                                        }
                                    }
                                    else if (m == 2) {
                                        buffer[i] = ((byteBuffer[j + currentIndex] & 0x0f) << 6) |
                                                    ((byteBuffer[j + 1 + currentIndex] & 0xfc)  >>> 2);
                                        if (x < xDim - 1) {
                                            m = 3;
                                            j++;
                                        }
                                        else {
                                            m = 0;
                                            j += 2;
                                        }
                                    }
                                    else if (m == 3) {
                                        buffer[i] = ((byteBuffer[j + currentIndex] & 0x03) << 8) |
                                                    (byteBuffer[j + 1 + currentIndex]& 0xff);
                                        m = 0;
                                        j += 2;
                                    }
                                } 
                            }     
                        } // if (isBW10 && endianess)
                        else if (isBW12 && endianess) {
                            for (j = 0, m = 0; y < yDim; y++) {
                                for (x = 0; x < xDim; x++, i++) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                    }

                                    if (m == 0) {
                                        buffer[i] = ((byteBuffer[j + currentIndex] & 0xff) << 4) |
                                                    ((byteBuffer[j + currentIndex + 1] & 0xf0) >>> 4);
                                        if (x < xDim - 1) {
                                            m = 1;
                                            j++;
                                        }
                                        else {
                                            m = 0;
                                            j += 2;
                                        }
                                    }
                                    else if (m == 1) {
                                        buffer[i] = ((byteBuffer[j + currentIndex] & 0x0f) << 8) |
                                                    (byteBuffer[j + 1 + currentIndex] & 0xff);
                                        m = 0;
                                        j += 2;
                                    }
                                } 
                            }         
                        } // else if (isBW12 && endianess)
                        else if (isBW14 && endianess) {
                            for (j = 0, m = 0; y < yDim; y++) {
                                for (x = 0; x < xDim; x++, i++) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                    }

                                    if (m == 0) {
                                        buffer[i] = ((byteBuffer[j + currentIndex] & 0xff) << 6) |
                                                    ((byteBuffer[j + currentIndex + 1] & 0xfc) >>> 2);
                                        if (x < xDim - 1) {
                                            m = 1;
                                            j++;
                                        }
                                        else {
                                            m = 0;
                                            j += 2;
                                        }
                                    }
                                    else if (m == 1) {
                                        buffer[i] = ((byteBuffer[j + currentIndex] & 0x03) << 12) |
                                                    ((byteBuffer[j + 1 + currentIndex] & 0xff) << 4) |
                                                    ((byteBuffer[j + 2 + currentIndex] & 0xf0) >>> 4);
                                        if (x < xDim - 1) {
                                            m = 2;
                                            j += 2;
                                        }
                                        else {
                                            m = 0;
                                            j += 3;
                                        }
                                    }
                                    else if (m == 2) {
                                        buffer[i] = ((byteBuffer[j + currentIndex] & 0x0f) << 10) |
                                                    ((byteBuffer[j + 1 + currentIndex] & 0xff)  << 2) |
                                                    ((byteBuffer[j + 2 + currentIndex] & 0xc0) >>> 6);
                                        if (x < xDim - 1) {
                                            m = 3;
                                            j += 2;
                                        }
                                        else {
                                            m = 0;
                                            j += 3;
                                        }
                                    }
                                    else if (m == 3) {
                                        buffer[i] = ((byteBuffer[j + currentIndex] & 0x3f) << 8) |
                                                    (byteBuffer[j + 1 + currentIndex]& 0xff);
                                        m = 0;
                                        j += 2;
                                    }
                                } 
                            }        
                        } // else if (isBW14 && endianess)
                        else {
                            for (j = 0; j < nBytes; j += 2, i++) {
    
                                if (((i + progress) % mod) == 0) {
                                    fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                }
    
                                b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);
    
                                if (endianess) {
                                    buffer[i] = ((b1 << 8) + b2);
                                } else {
                                    buffer[i] = ((b2 << 8) + b1);
                                }
                            }
                        } // else 

                        break;

                    case ModelStorageBase.INTEGER:

                        // if (byteBuffer == null) {
                        // byteBuffer = new byte[4 * buffer.length];
                        // }
                        // raFile.read(byteBuffer, 0, nBytes);
                        progress = slice * buffer.length;
                        progressLength = buffer.length * imageSlice;
                        mod = progressLength / 10;

                        for (j = 0; j < nBytes; j += 4, i++) {

                            if (((i + progress) % mod) == 0) {
                                fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                            }

                            b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                            b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);
                            b3 = getUnsignedByte(byteBuffer, j + currentIndex + 2);
                            b4 = getUnsignedByte(byteBuffer, j + currentIndex + 3);

                            if (endianess) {
                                buffer[i] = ((b1 << 24) | (b2 << 16) | (b3 << 8) | b4); // Big Endian
                            } else {
                                buffer[i] = ((b4 << 24) | (b3 << 16) | (b2 << 8) | b1); // Little Endian
                            }
                        }

                        break;

                    case ModelStorageBase.UINTEGER:

                        // if (byteBuffer == null) {
                        // byteBuffer = new byte[4 * buffer.length];
                        // }
                        // raFile.read(byteBuffer, 0, nBytes);
                        progress = slice * buffer.length;
                        progressLength = buffer.length * imageSlice;
                        mod = progressLength / 10;
                        
                        if (isBW24) {
                            for (j = 0; j < nBytes; j += 3, i++) {
                                
                                if (((i + progress) % mod) == 0) {
                                    fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                }
    
                                b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);
                                b3 = getUnsignedByte(byteBuffer, j + currentIndex + 2);
    
                                if (endianess) {
                                    buffer[i] = (((long) b1 << 16) | ((long) b2 << 8) | (long) b3); // Big
                                                                                                    // Endian
                                } else {
                                    buffer[i] = (((long) b3 << 16) | ((long) b2 << 8) | (long) b1); // Little
                                                                                                        // Endian
                                }
                            }    
                        } // if (isBW24)
                        else {

                            for (j = 0; j < nBytes; j += 4, i++) {
    
                                if (((i + progress) % mod) == 0) {
                                    fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                }
    
                                b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);
                                b3 = getUnsignedByte(byteBuffer, j + currentIndex + 2);
                                b4 = getUnsignedByte(byteBuffer, j + currentIndex + 3);
    
                                if (endianess) {
                                    buffer[i] = (((long) b1 << 24) | ((long) b2 << 16) | ((long) b3 << 8) | (long) b4); // Big
                                                                                                                        // Endian
                                } else {
                                    buffer[i] = (((long) b4 << 24) | ((long) b3 << 16) | ((long) b2 << 8) | (long) b1); // Little
                                                                                                                        // Endian
                                }
                            }
                        } // else 

                        break;

                    case ModelStorageBase.FLOAT:

                        // if (byteBuffer == null) {
                        // byteBuffer = new byte[4 * buffer.length];
                        // }
                        // raFile.read(byteBuffer, 0, nBytes);
                        progress = slice * buffer.length;
                        progressLength = buffer.length * imageSlice;
                        mod = progressLength / 10;

                        for (j = 0; j < nBytes; j += 4, i++) {

                            if (((i + progress) % mod) == 0) {
                                fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                            }

                            b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                            b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);
                            b3 = getUnsignedByte(byteBuffer, j + currentIndex + 2);
                            b4 = getUnsignedByte(byteBuffer, j + currentIndex + 3);

                            if (endianess) {
                                tmpInt = ((b1 << 24) | (b2 << 16) | (b3 << 8) | b4); // Big Endian
                            } else {
                                tmpInt = ((b4 << 24) | (b3 << 16) | (b2 << 8) | b1);
                            }

                            buffer[i] = Float.intBitsToFloat(tmpInt);
                        } // for (j =0; j < nBytes; j+=4, i++ )

                        break;

                    case ModelStorageBase.ARGB:
                        if (isYCbCr) {
                            if (chunky == true) {
                                
                                if ((YCbCrSubsampleHoriz == 2) && (YCbCrSubsampleVert == 2)) {
                                    // This is the default subsampling
                                    for (j = 0; j < nBytes; j += 6) {
                                        Y00 = getUnsignedByte(byteBuffer, j + currentIndex);
                                        Y01 = getUnsignedByte(byteBuffer, j + currentIndex + 1);
                                        Y10 = getUnsignedByte(byteBuffer, j + currentIndex + 2);
                                        Y11 = getUnsignedByte(byteBuffer, j + currentIndex + 3);
                                        Cb00 = getUnsignedByte(byteBuffer, j + currentIndex + 4);
                                        Cr00 = getUnsignedByte(byteBuffer, j + currentIndex + 5);
                                        if ((x < xDim) && (y < yDim)) {
                                            YBuffer[y*xDim + x] = Y00;
                                            CbInBuffer[y*halfXDim/2 + x/2] = Cb00;
                                            CrInBuffer[y*halfXDim/2 + x/2] = Cr00;
                                        }
                                        if (((x+1) < xDim) && (y < yDim)) {
                                            YBuffer[y*xDim + x + 1] = Y01;
                                        }
                                        if ((x < xDim) && ((y+1) < yDim)) {
                                            YBuffer[(y+1)*xDim + x] = Y10;
                                        }
                                        if (((x+1) < xDim) && ((y+1) < yDim)) {
                                            YBuffer[(y+1)*xDim + x + 1] = Y11;
                                        }
                                            
                                        x += 2;
                                        if (x >= xDim) {
                                            x = 0;
                                            y += 2;
                                        }
                                    }
                                } // if ((YCbCrSubsampleHoriz == 2) && (YCbCrSubsampleVert == 2))
                                else if ((YCbCrSubsampleHoriz == 2) && (YCbCrSubsampleVert == 1)) {
                                    for (j = 0; j < nBytes; j += 4) {
                                        Y00 = getUnsignedByte(byteBuffer, j + currentIndex);
                                        Y01 = getUnsignedByte(byteBuffer, j + currentIndex + 1);
                                        Cb00 = getUnsignedByte(byteBuffer, j + currentIndex + 2);
                                        Cr00 = getUnsignedByte(byteBuffer, j + currentIndex + 3);
                                        if ((x < xDim) && (y < yDim)) {
                                            YBuffer[y*xDim + x] = Y00;
                                            CbInBuffer[y*halfXDim + x/2] = Cb00;
                                            CrInBuffer[y*halfXDim + x/2] = Cr00;
                                        }
                                        if (((x+1) < xDim) && (y < yDim)) {
                                            YBuffer[y*xDim + x + 1] = Y01;
                                        }
                                            
                                        x += 2;
                                        if (x >= xDim) {
                                            x = 0;
                                            y++;
                                        }
                                    }    
                                } // else if ((YCbCrSubsampleHoriz == 2) && (YCbCrSubsampleVert == 1))
                                else if ((YCbCrSubsampleHoriz == 1) && (YCbCrSubsampleVert == 2)) {
                                    for (j = 0; j < nBytes; j += 4) {
                                        Y00 = getUnsignedByte(byteBuffer, j + currentIndex);
                                        Y10 = getUnsignedByte(byteBuffer, j + currentIndex + 1);
                                        Cb00 = getUnsignedByte(byteBuffer, j + currentIndex + 2);
                                        Cr00 = getUnsignedByte(byteBuffer, j + currentIndex + 3);
                                        if ((x < xDim) && (y < yDim)) {
                                            YBuffer[y*xDim + x] = Y00;
                                            CbInBuffer[y*xDim/2 + x] = Cb00;
                                            CrInBuffer[y*xDim/2 + x] = Cr00;
                                        }
                                        if ((x < xDim) && ((y+1) < yDim)) {
                                            YBuffer[(y+1)*xDim + x] = Y10;
                                        }
                                            
                                        x++;
                                        if (x == xDim) {
                                            x = 0;
                                            y += 2;
                                        }
                                    }    
                                } // else if ((YCbCrSubsampleHoriz == 1) && (YCbCrSubsampleVert == 2))
                                else if ((YCbCrSubsampleHoriz == 1) && (YCbCrSubsampleVert == 1)) {
                                    for (j = 0; j < nBytes; j += 3) {
                                        Y00 = getUnsignedByte(byteBuffer, j + currentIndex);
                                        Cb00 = getUnsignedByte(byteBuffer, j + currentIndex + 1);
                                        Cr00 = getUnsignedByte(byteBuffer, j + currentIndex + 2);
                                        if ((x < xDim) && (y < yDim)) {
                                            YBuffer[y*xDim + x] = Y00;
                                            CbInBuffer[y*halfXDim/2 + x/2] = Cb00;
                                            CrInBuffer[y*halfXDim/2 + x/2] = Cr00;
                                        }
                                            
                                        x++;
                                        if (x == xDim) {
                                            x = 0;
                                            y++;
                                        }
                                    }        
                                } // else if ((YCbCrSubsampleHoriz == 1) && (YCbCrSubsampleVert == 1))
                                
                            } // if (chunky == true)    
                        } // if (isYCbCr)
                        else if (isCMYK) {
                            if (chunky) {
                                // if (byteBuffer == null)
                                // byteBuffer = new byte[buffer.length];
                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;
    
    
                                // For the moment I compress RGB images to unsigned bytes.
                                for (j = 0; j < nBytes; j += 4, i += 4) {
    
                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                    }
    
                                    buffer[i] = 255;
                                    // red = 255 - cyan
                                    buffer[i + 1] = 255 - getUnsignedByte(byteBuffer, j + currentIndex);
                                    // green = 255 - magenta
                                    buffer[i + 2] = 255 - getUnsignedByte(byteBuffer, j + currentIndex + 1);
                                    // blue = 255 - yellow
                                    buffer[i + 3] = 255 - getUnsignedByte(byteBuffer, j + currentIndex + 2);
                                    // don't use black
                                }  
                            } // if (chunky)
                            else { // planar
                                if (planarRGB < stripsPerImage) {

                                    // if (byteBuffer == null)
                                    // byteBuffer = new byte[buffer.length];
                                    // raFile.read(byteBuffer, 0, nBytes);
                                    progress = slice * buffer.length;
                                    progressLength = buffer.length * imageSlice;
                                    mod = progressLength / 10;


                                    // For the moment I compress RGB images to unsigned bytes
                                    for (j = 0; j < nBytes; j++, i += 4) {

                                        if ((((i / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + progress) /
                                                                                    progressLength * 100));
                                        }

                                        buffer[i] = 255;
                                        // red = 255 - cyan
                                        buffer[i + 1] = 255 - getUnsignedByte(byteBuffer, j + currentIndex);
                                    }

                                    planarRGB++;

                                    if (planarRGB == stripsPerImage) {
                                        i = 0;
                                    }
                                } // end of if (planarRGB < stripsPerImage)
                                else if (planarRGB < (2 * stripsPerImage)) {

                                    // raFile.read(byteBuffer, 0, nBytes);
                                    progress = slice * buffer.length;
                                    progressLength = buffer.length * imageSlice;
                                    mod = progressLength / 10;


                                    for (j = 0; j < nBytes; j++, i += 4) {

                                        if ((((i / 3) + (buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + (buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }
                                        // green = 255 - magenta
                                        buffer[i + 2] = 255 - getUnsignedByte(byteBuffer, j + currentIndex);
                                    }

                                    planarRGB++;

                                    if (planarRGB == (2 * stripsPerImage)) {
                                        i = 0;
                                    }
                                } // end of else if (planarRGB < 2*stripsPerImage)
                                else if (planarRGB < 3*stripsPerImage){

                                    // raFile.read(byteBuffer, 0, nBytes);
                                    progress = slice * buffer.length;
                                    progressLength = buffer.length * imageSlice;
                                    mod = progressLength / 10;


                                    for (j = 0; j < nBytes; j++, i += 4) {

                                        if ((((i / 3) + (2 * buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) +
                                                                                         (2 * buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }
                                        // blue = 255 - yellow
                                        buffer[i + 3] = 255 - getUnsignedByte(byteBuffer, j + currentIndex);
                                    }

                                    planarRGB++;
                                } // end of else if (planarRGB < 3*stripsPerImage)    
                            } // else planar
                        } // else if (isCMYK)
                        else if (chunky && isRGB2) {
                            progress = slice * buffer.length;
                            progressLength = buffer.length * imageSlice;
                            mod = progressLength / 10;
                            for (j = 0, m = 0; y < yDim; y++) {
                                for (x = 0; x < xDim; x++, i += 4) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                    }

                                    if (m == 0) {
                                        buffer[i] = 255;
                                        buffer[i + 1] = (byteBuffer[j + currentIndex] & 0xC0) >>> 6;
                                        buffer[i + 2] = (byteBuffer[j + currentIndex] & 0x30) >>> 4;
                                        buffer[i + 3] = (byteBuffer[j + currentIndex] & 0x0C) >>> 2;
                                        if (x < xDim - 1) {
                                            m = 1;
                                        }
                                        else {
                                            j++;
                                            m = 0;
                                        }
                                    } // if (m == 0)
                                    else if (m == 1) {
                                        buffer[i] = 255;
                                        buffer[i+1] = (byteBuffer[j + currentIndex] & 0x03);
                                        buffer[i+2] = (byteBuffer[j + 1 + currentIndex] & 0xC0) >>> 6;
                                        buffer[i+3] = (byteBuffer[j + 1 + currentIndex] & 0x30) >>> 4;
                                        if (x < xDim - 1) {
                                            m = 2;
                                            j++;
                                        }
                                        else {
                                            m = 0;
                                            j += 2;
                                        }
                                    } // else if (m == 1)
                                    else if (m == 2) {
                                        buffer[i] = 255;
                                        buffer[i+1] = (byteBuffer[j + currentIndex] & 0x0C) >>> 2;
                                        buffer[i+2] = (byteBuffer[j + currentIndex] & 0x03);
                                        buffer[i+3] = (byteBuffer[j + 1 + currentIndex] & 0xC0) >>> 6;
                                        if (x < xDim - 1) {
                                            m = 3;
                                            j++;
                                        }
                                        else {
                                            m = 0;
                                            j += 2;
                                        }
                                    } // else if (m == 2)
                                    else if (m == 3) {
                                        buffer[i] = 255;
                                        buffer[i + 1] = (byteBuffer[j + currentIndex] & 0x30) >>> 4;
                                        buffer[i + 2] = (byteBuffer[j + currentIndex] & 0x0C) >>> 2; 
                                        buffer[i + 3] = (byteBuffer[j + currentIndex] & 0x03);
                                        m = 0;
                                        j++;
                                    } // else if (m == 3)
                                } 
                            }
                        } // else if (chunky && isRGB2))
                        else if ((!chunky) && isRGB2) {
                            if (planarRGB < stripsPerImage) {

                                // if (byteBuffer == null)
                                // byteBuffer = new byte[buffer.length];
                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;

                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i += 4) {
    
                                        if ((((i / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + progress) /
                                                                                    progressLength * 100));
                                        }
                                        
                                        buffer[i] = 255;
                                        if (m == 0) {
                                            buffer[i+1] = (byteBuffer[j + currentIndex] & 0xc0) >>> 6;
                                            if (x < xDim - 1) {
                                                m = 1;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // if (m == 0)
                                        else if (m == 1) {
                                            buffer[i+1] = (byteBuffer[j + currentIndex] & 0x30) >>> 4;
                                            if (x < xDim - 1) {
                                                m = 2;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // else if (m == 1)
                                        else if (m == 2) {
                                            buffer[i+1] = (byteBuffer[j + currentIndex] & 0x0c) >>> 2;
                                            if (x < xDim - 1) {
                                                m = 3;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // else if (m == 2)
                                        else { // m == 3
                                            buffer[i+1] = (byteBuffer[j + currentIndex] & 0x03);
                                            m = 0;
                                            j++;
                                        } // else m == 3
                                    } 
                                }

                                planarRGB++;

                                if (planarRGB == stripsPerImage) {
                                    i = 0;
                                    y = 0;
                                }
                            } // end of if (planarRGB < stripsPerImage)
                            else if (planarRGB < (2 * stripsPerImage)) {

                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;

                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i += 4) {
    
                                        if ((((i / 3) + (buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + (buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }
                                        
                                        if (m == 0) {
                                            buffer[i+2] = (byteBuffer[j + currentIndex] & 0xc0) >>> 6;
                                            if (x < xDim - 1) {
                                                m = 1;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // if (m == 0)
                                        else if (m == 1) {
                                            buffer[i+2] = (byteBuffer[j + currentIndex] & 0x30) >>> 4;
                                            if (x < xDim - 1) {
                                                m = 2;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // else if (m == 1)
                                        else if (m == 2) {
                                            buffer[i+2] = (byteBuffer[j + currentIndex] & 0x0c) >>> 2;
                                            if (x < xDim - 1) {
                                                m = 3;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // else if (m == 2)
                                        else { // m == 3
                                            buffer[i+2] = (byteBuffer[j + currentIndex] & 0x03);
                                            m = 0;
                                            j++;
                                        } // else m == 3
                                    } 
                                }

                                planarRGB++;

                                if (planarRGB == (2 * stripsPerImage)) {
                                    i = 0;
                                    y = 0;
                                }
                            } // end of else if (planarRGB < 2*stripsPerImage)
                            else { // planarRGB >= 2*stripsPerImage

                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;
                                
                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i += 4) {
    
                                        if ((((i / 3) + (2 * buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) +
                                                                                         (2 * buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }
                                        
                                        if (m == 0) {
                                            buffer[i+3] = (byteBuffer[j + currentIndex] & 0xc0) >>> 6;
                                            if (x < xDim - 1) {
                                                m = 1;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // if (m == 0)
                                        else if (m == 1) {
                                            buffer[i+3] = (byteBuffer[j + currentIndex] & 0x30) >>> 4;
                                            if (x < xDim - 1) {
                                                m = 2;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // else if (m == 1)
                                        else if (m == 2) {
                                            buffer[i+3] = (byteBuffer[j + currentIndex] & 0x0c) >>> 2;
                                            if (x < xDim - 1) {
                                                m = 3;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // else if (m == 2)
                                        else { // m == 3
                                            buffer[i+3] = (byteBuffer[j + currentIndex] & 0x03);
                                            m = 0;
                                            j++;
                                        } // else m == 3
                                    } 
                                }

                                planarRGB++;
                            } // end of else for planarRGB >= 2*StripsPerImage    
                        } // else if ((!chunky) && isRGB2)
                        else if (chunky && isRGB4) {
                            progress = slice * buffer.length;
                            progressLength = buffer.length * imageSlice;
                            mod = progressLength / 10;
                            for (j = 0, m = 0; y < yDim; y++) {
                                for (x = 0; x < xDim; x++, i += 4) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                    }

                                    if (m == 0) {
                                        buffer[i] = 255;
                                        buffer[i + 1] = (byteBuffer[j + currentIndex] & 0xf0) >>> 4;
                                        buffer[i + 2] = (byteBuffer[j + currentIndex] & 0x0f);
                                        buffer[i + 3] = (byteBuffer[j + 1 + currentIndex] & 0xf0) >>> 4;
                                        if (x < xDim - 1) {
                                            m = 1;
                                            j++;
                                        }
                                        else {
                                            m = 0;
                                            j += 2;
                                        }
                                    } // if (m == 0)
                                    else if (m == 1) {
                                        buffer[i] = 255;
                                        buffer[i+1] = (byteBuffer[j + currentIndex] & 0x0f);
                                        buffer[i+2] = (byteBuffer[j + 1 + currentIndex] & 0xf0) >>> 4;
                                        buffer[i+3] = (byteBuffer[j + 1 + currentIndex] & 0x0f);
                                        m = 0;
                                        j += 2;
                                    } // else if (m == 1)
                                } 
                            }    
                        } // else if (chunky && isRGB4)
                        else if ((!chunky) && isRGB4) {
                            if (planarRGB < stripsPerImage) {

                                // if (byteBuffer == null)
                                // byteBuffer = new byte[buffer.length];
                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;
                                
                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i += 4) {
    
                                        if ((((i / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + progress) /
                                                                                    progressLength * 100));
                                        }
                                        
                                        buffer[i] = 255;
                                        if (m == 0) {
                                            buffer[i+1] = (byteBuffer[j + currentIndex] & 0xf0) >>> 4;
                                            if (x < xDim - 1) {
                                                m = 1;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // if (m == 0)
                                        else { // m == 1
                                            buffer[i+1] = (byteBuffer[j + currentIndex] & 0x0f);
                                            m = 0;
                                            j++;
                                        } // else m == 1
                                    } 
                                }    

                                planarRGB++;

                                if (planarRGB == stripsPerImage) {
                                    i = 0;
                                    y = 0;
                                }
                            } // end of if (planarRGB < stripsPerImage)
                            else if (planarRGB < (2 * stripsPerImage)) {

                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;

                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i += 4) {
    
                                        if ((((i / 3) + (buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + (buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }
                                        
                                        if (m == 0) {
                                            buffer[i+2] = (byteBuffer[j + currentIndex] & 0xf0) >>> 4;
                                            if (x < xDim - 1) {
                                                m = 1;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // if (m == 0)
                                        else { // m == 1
                                            buffer[i+2] = (byteBuffer[j + currentIndex] & 0x0f);
                                            m = 0;
                                            j++;
                                        } // else m == 1
                                    } 
                                }    
                                
                                planarRGB++;

                                if (planarRGB == (2 * stripsPerImage)) {
                                    i = 0;
                                    y = 0;
                                }
                            } // end of else if (planarRGB < 2*stripsPerImage)
                            else { // planarRGB >= 2*stripsPerImage

                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;
                                
                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i += 4) {
    
                                        if ((((i / 3) + (2 * buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) +
                                                                                         (2 * buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }
                                        
                                        if (m == 0) {
                                            buffer[i+3] = (byteBuffer[j + currentIndex] & 0xf0) >>> 4;
                                            if (x < xDim - 1) {
                                                m = 1;
                                            }
                                            else {
                                                m = 0;
                                                j++;
                                            }
                                        } // if (m == 0)
                                        else { // m == 1
                                            buffer[i+3] = (byteBuffer[j + currentIndex] & 0x0f);
                                            m = 0;
                                            j++;
                                        } // else m == 1
                                    } 
                                }    

                                planarRGB++;
                            } // end of else for planarRGB >= 2*StripsPerImage        
                        } // else if ((!chunky) && isRGB4)
                        else if ((chunky == true) && (packBit == false)) {

                            // if (byteBuffer == null)
                            // byteBuffer = new byte[buffer.length];
                            // raFile.read(byteBuffer, 0, nBytes);
                            progress = slice * buffer.length;
                            progressLength = buffer.length * imageSlice;
                            mod = progressLength / 10;


                            // For the moment I compress RGB images to unsigned bytes.
                            for (j = 0; j < nBytes; j += 3, i += 4) {

                                if (((i + progress) % mod) == 0) {
                                    fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                }

                                buffer[i] = 255;
                                buffer[i + 1] = getUnsignedByte(byteBuffer, j + currentIndex);
                                buffer[i + 2] = getUnsignedByte(byteBuffer, j + currentIndex + 1);
                                buffer[i + 3] = getUnsignedByte(byteBuffer, j + currentIndex + 2);
                            }
                        } // if (chunky == true && packBit == false)
                        else if ((chunky == true) && (packBit == true)) {
                            int rgbLoc = 1;

                            // if (byteBuffer == null)
                            // byteBuffer = new byte[buffer.length];
                            // raFile.read(byteBuffer, 0, nBytes);
                            progress = slice * buffer.length;
                            progressLength = buffer.length * imageSlice;
                            mod = progressLength / 10;

                            j = 0;

                            while (j < nBytes) {

                                // uncompressed data bytes follow
                                // Copy the next n+1 bytes literally
                                if ((byteBuffer[j + currentIndex] & 0x80) == 0) {
                                    iCount = byteBuffer[j + currentIndex] + 1;
                                    j++;

                                    for (iNext = 0; iNext < iCount; iNext++, j++) {

                                        if (((i + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                    progressLength * 100));
                                        }

                                        if (rgbLoc == 1) {
                                            buffer[i++] = 255;
                                        }

                                        buffer[i++] = byteBuffer[j + currentIndex] & 0xff;

                                        if (rgbLoc == 3) {
                                            rgbLoc = 1;
                                        } else {
                                            rgbLoc++;
                                        }
                                    }
                                } // end of if (byteBuffer[j] & 0x80 == 0)

                                // Do nothing if the byte value is -128
                                else if (byteBuffer[j + currentIndex] == -128) {
                                    j++;
                                }
                                // compressed data bytes follow
                                // (~byteBuffer[j]) + 1 is the 2's complement of n or -n
                                // Hence (~byteBuffer[j]) + 2 equals -n + 1
                                else {
                                    iCount = (~byteBuffer[j + currentIndex]) + 2;
                                    j++;

                                    for (iNext = 0; iNext < iCount; iNext++) {

                                        if (((i + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                    progressLength * 100));
                                        }

                                        if (rgbLoc == 1) {
                                            buffer[i++] = 255;
                                        }

                                        buffer[i++] = byteBuffer[j + currentIndex] & 0xff;

                                        if (rgbLoc == 3) {
                                            rgbLoc = 1;
                                        } else {
                                            rgbLoc++;
                                        }
                                    }

                                    j++;
                                } // end of else for compressed data bytes
                            } // end of while (j < nBytes)
                        } // else if (chunky == true && packBit == true)
                        else if (packBit == false) { // planar RGB configuration

                            if (planarRGB < stripsPerImage) {

                                // if (byteBuffer == null)
                                // byteBuffer = new byte[buffer.length];
                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;


                                // For the moment I compress RGB images to unsigned bytes
                                for (j = 0; j < nBytes; j++, i += 4) {

                                    if ((((i / 3) + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) ((i / 3) + progress) /
                                                                                progressLength * 100));
                                    }

                                    buffer[i] = 255;
                                    buffer[i + 1] = getUnsignedByte(byteBuffer, j + currentIndex);
                                }

                                planarRGB++;

                                if (planarRGB == stripsPerImage) {
                                    i = 0;
                                }
                            } // end of if (planarRGB < stripsPerImage)
                            else if (planarRGB < (2 * stripsPerImage)) {

                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;


                                for (j = 0; j < nBytes; j++, i += 4) {

                                    if ((((i / 3) + (buffer.length / 3) + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) ((i / 3) + (buffer.length / 3) +
                                                                                     progress) / progressLength * 100));
                                    }

                                    buffer[i + 2] = getUnsignedByte(byteBuffer, j + currentIndex);
                                }

                                planarRGB++;

                                if (planarRGB == (2 * stripsPerImage)) {
                                    i = 0;
                                }
                            } // end of else if (planarRGB < 2*stripsPerImage)
                            else { // planarRGB >= 2*stripsPerImage

                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;


                                for (j = 0; j < nBytes; j++, i += 4) {

                                    if ((((i / 3) + (2 * buffer.length / 3) + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) ((i / 3) +
                                                                                     (2 * buffer.length / 3) +
                                                                                     progress) / progressLength * 100));
                                    }

                                    buffer[i + 3] = getUnsignedByte(byteBuffer, j + currentIndex);
                                }

                                planarRGB++;
                            } // end of else for planarRGB >= 2*StripsPerImage
                        } // else for planar RGB configuration with packedBit == false

                        break;

                    case ModelStorageBase.ARGB_USHORT:
                        if (isCMYK) {
                            if (chunky) {
                                // if (byteBuffer == null)
                                // byteBuffer = new byte[2 * buffer.length];
                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;


                                // For the moment I compress RGB images to unsigned bytes.
                                for (j = 0; j < nBytes; j += 8, i += 4) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                    }

                                    buffer[i] = 65535;
                                    b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                    b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);

                                    // red = 65535 - cyan
                                    if (endianess) {
                                        buffer[i + 1] = 65535 - ((b1 << 8) + b2);
                                    } else {
                                        buffer[i + 1] = 65535 - ((b2 << 8) + b1);
                                    }

                                    b1 = getUnsignedByte(byteBuffer, j + currentIndex + 2);
                                    b2 = getUnsignedByte(byteBuffer, j + currentIndex + 3);

                                    // green = 65535 - magenta
                                    if (endianess) {
                                        buffer[i + 2] = 65535 - ((b1 << 8) + b2);
                                    } else {
                                        buffer[i + 2] = 65535 - ((b2 << 8) + b1);
                                    }

                                    b1 = getUnsignedByte(byteBuffer, j + currentIndex + 4);
                                    b2 = getUnsignedByte(byteBuffer, j + currentIndex + 5);

                                    // blue = 65535 - yellow
                                    if (endianess) {
                                        buffer[i + 3] = 65535 - ((b1 << 8) + b2);
                                    } else {
                                        buffer[i + 3] = 65535 - ((b2 << 8) + b1);
                                    }
                                    // don't use black
                                }    
                            } // if (chunky)
                            else { // planar
                                if (planarRGB < stripsPerImage) {

                                    // if (byteBuffer == null)
                                    // byteBuffer = new byte[2 * buffer.length];
                                    // raFile.read(byteBuffer, 0, nBytes);
                                    progress = slice * buffer.length;
                                    progressLength = buffer.length * imageSlice;
                                    mod = progressLength / 10;


                                    // For the moment I compress RGB images to unsigned bytes
                                    for (j = 0; j < nBytes; j += 2, i += 4) {

                                        if ((((i / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + progress) /
                                                                                    progressLength * 100));
                                        }

                                        buffer[i] = 65535;
                                        b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                        b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);

                                        // red = 65535 - cyan
                                        if (endianess) {
                                            buffer[i + 1] = 65535 - ((b1 << 8) + b2);
                                        } else {
                                            buffer[i + 1] = 65535 - ((b2 << 8) + b1);
                                        }
                                    }

                                    planarRGB++;

                                    if (planarRGB == stripsPerImage) {
                                        i = 0;
                                    }
                                } // end of if (planarRGB < stripsPerImage)
                                else if (planarRGB < (2 * stripsPerImage)) {

                                    // raFile.read(byteBuffer, 0, nBytes);
                                    progress = slice * buffer.length;
                                    progressLength = buffer.length * imageSlice;
                                    mod = progressLength / 10;


                                    for (j = 0; j < nBytes; j += 2, i += 4) {

                                        if ((((i / 3) + (buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + (buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }

                                        b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                        b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);

                                        // green = 65535 - magenta
                                        if (endianess) {
                                            buffer[i + 2] = 65535 - ((b1 << 8) + b2);
                                        } else {
                                            buffer[i + 2] = 65535 - ((b2 << 8) + b1);
                                        }
                                    }

                                    planarRGB++;

                                    if (planarRGB == (2 * stripsPerImage)) {
                                        i = 0;
                                    }
                                } // end of else if (planarRGB < 2*stripsPerImage)
                                else if (planarRGB < (3 * stripsPerImage)) {

                                    // raFile.read(byteBuffer, 0, nBytes);
                                    progress = slice * buffer.length;
                                    progressLength = buffer.length * imageSlice;
                                    mod = progressLength / 10;


                                    for (j = 0; j < nBytes; j += 2, i += 4) {

                                        if ((((i / 3) + (2 * buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) +
                                                                                         (2 * buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }

                                        b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                        b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);

                                        // blue = 65535 - yellow
                                        if (endianess) {
                                            buffer[i + 3] = 65535 - ((b1 << 8) + b2);
                                        } else {
                                            buffer[i + 3] = 65535 - ((b2 << 8) + b1);
                                        }
                                    }

                                    planarRGB++;
                                } // end of else if (planarRGB < (3 * stripsPerImage)    
                            } // else planar
                        } // if (isCMYK)
                        else if (chunky && isRGB10 && endianess) {
                            progress = slice * buffer.length;
                            progressLength = buffer.length * imageSlice;
                            mod = progressLength / 10;
                            for (j = 0, m = 0; y < yDim; y++) {
                                for (x = 0; x < xDim; x++, i += 4) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                    }

                                    if (m == 0) {
                                        buffer[i] = 65535;
                                        buffer[i + 1] = ((byteBuffer[j + currentIndex] & 0xff) << 2) |
                                                        ((byteBuffer[j + currentIndex + 1] & 0xc0) >>> 6);
                                        buffer[i + 2] = ((byteBuffer[j + currentIndex + 1] & 0x3f) << 4) |
                                                        ((byteBuffer[j + currentIndex + 2] & 0xf0) >>> 4);
                                        buffer[i + 3] = ((byteBuffer[j + currentIndex + 2] & 0x0f) << 6) |
                                                        ((byteBuffer[j + currentIndex + 3] & 0xfc) >>> 2);
                                        if (x < xDim - 1) {
                                            m = 1;
                                            j += 3;
                                        }
                                        else {
                                            m = 0;
                                            j += 4;
                                        }
                                    } // if (m == 0)
                                    else if (m == 1) {
                                        buffer[i] = 65535;
                                        buffer[i+1] = ((byteBuffer[j + currentIndex] & 0x03) << 8) |
                                                      (byteBuffer[j + currentIndex + 1] & 0xff);
                                        buffer[i+2] = ((byteBuffer[j + currentIndex + 2] & 0xff) << 2) |
                                                      ((byteBuffer[j + currentIndex + 3] & 0xc0) >>> 6);
                                        buffer[i+3] = ((byteBuffer[j + currentIndex + 3] & 0x3f) << 4) |
                                                      ((byteBuffer[j + currentIndex + 4] & 0xf0) >>> 4);
                                        if (x < xDim - 1) {
                                            m = 2;
                                            j += 4;
                                        }
                                        else {
                                            m = 0;
                                            j += 5;
                                        }
                                    } // else if (m == 1)
                                    else if (m == 2) {
                                        buffer[i] = 65535;
                                        buffer[i+1] = ((byteBuffer[j + currentIndex] & 0x0f) << 6) |
                                                      ((byteBuffer[j + currentIndex + 1] & 0xfc) >>> 2);
                                        buffer[i+2] = ((byteBuffer[j + currentIndex+ 1] & 0x03) << 8) |
                                                      (byteBuffer[j + currentIndex + 2] & 0xff);
                                        buffer[i+3] = ((byteBuffer[j + currentIndex + 3] & 0xff) << 2) |
                                                      ((byteBuffer[j + currentIndex + 4] & 0xc0) >>> 6);
                                        if (x < xDim - 1) {
                                            m = 3;
                                            j += 4;
                                        }
                                        else {
                                            m = 0;
                                            j += 5;
                                        }
                                    } // else if (m == 2)
                                    else if (m == 3) {
                                        buffer[i] = 65535;
                                        buffer[i + 1] = ((byteBuffer[j + currentIndex] & 0x3f) << 4) |
                                                        ((byteBuffer[j + currentIndex + 1] & 0xf0) >>> 4);
                                        buffer[i + 2] = ((byteBuffer[j + currentIndex + 1] & 0x0f) << 6) |
                                                        ((byteBuffer[j + currentIndex + 2] & 0xfc) >>> 2); 
                                        buffer[i + 3] = ((byteBuffer[j + currentIndex + 2] & 0x03) << 8) |
                                                        (byteBuffer[j + currentIndex + 3] & 0xff);
                                        m = 0;
                                        j += 4;
                                    } // else if (m == 3)
                                } 
                            }
                        } // else if (chunky && isRGB10 && endianess))
                        else if ((!chunky) && isRGB10 && endianess) {
                            if (planarRGB < stripsPerImage) {

                                // if (byteBuffer == null)
                                // byteBuffer = new byte[2 * buffer.length];
                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;
                                
                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i += 4) {

                                        if ((((i / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + progress) /
                                                                                    progressLength * 100));
                                        }

                                        buffer[i] = 65535;
                                        if (m == 0) {
                                            buffer[i+1] = ((byteBuffer[j + currentIndex] & 0xff) << 2) |
                                                        ((byteBuffer[j + currentIndex + 1] & 0xc0) >>> 6);
                                            if (x < xDim - 1) {
                                                m = 1;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 1) {
                                            buffer[i+1] = ((byteBuffer[j + currentIndex] & 0x3f) << 4) |
                                                        ((byteBuffer[j + 1 + currentIndex] & 0xf0) >>> 4);
                                            if (x < xDim - 1) {
                                                m = 2;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 2) {
                                            buffer[i+1] = ((byteBuffer[j + currentIndex] & 0x0f) << 6) |
                                                        ((byteBuffer[j + 1 + currentIndex] & 0xfc)  >>> 2);
                                            if (x < xDim - 1) {
                                                m = 3;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 3) {
                                            buffer[i+1] = ((byteBuffer[j + currentIndex] & 0x03) << 8) |
                                                        (byteBuffer[j + 1 + currentIndex]& 0xff);
                                            m = 0;
                                            j += 2;
                                        }
                                    } 
                                }     

                                planarRGB++;

                                if (planarRGB == stripsPerImage) {
                                    i = 0;
                                    y = 0;
                                }
                            } // end of if (planarRGB < stripsPerImage)
                            else if (planarRGB < (2 * stripsPerImage)) {

                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;

                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i += 4) {

                                        if ((((i / 3) + (buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + (buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }

                                        if (m == 0) {
                                            buffer[i+2] = ((byteBuffer[j + currentIndex] & 0xff) << 2) |
                                                        ((byteBuffer[j + currentIndex + 1] & 0xc0) >>> 6);
                                            if (x < xDim - 1) {
                                                m = 1;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 1) {
                                            buffer[i+2] = ((byteBuffer[j + currentIndex] & 0x3f) << 4) |
                                                        ((byteBuffer[j + 1 + currentIndex] & 0xf0) >>> 4);
                                            if (x < xDim - 1) {
                                                m = 2;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 2) {
                                            buffer[i+2] = ((byteBuffer[j + currentIndex] & 0x0f) << 6) |
                                                        ((byteBuffer[j + 1 + currentIndex] & 0xfc)  >>> 2);
                                            if (x < xDim - 1) {
                                                m = 3;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 3) {
                                            buffer[i+2] = ((byteBuffer[j + currentIndex] & 0x03) << 8) |
                                                        (byteBuffer[j + 1 + currentIndex]& 0xff);
                                            m = 0;
                                            j += 2;
                                        }
                                    } 
                                }     

                                planarRGB++;

                                if (planarRGB == (2 * stripsPerImage)) {
                                    i = 0;
                                    y = 0;
                                }
                            } // end of else if (planarRGB < 2*stripsPerImage)
                            else { // planarRGB >= 2*stripsPerImage

                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;

                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i += 4) {

                                        if ((((i / 3) + (2 * buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) +
                                                                                         (2 * buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }

                                        if (m == 0) {
                                            buffer[i+3] = ((byteBuffer[j + currentIndex] & 0xff) << 2) |
                                                        ((byteBuffer[j + currentIndex + 1] & 0xc0) >>> 6);
                                            if (x < xDim - 1) {
                                                m = 1;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 1) {
                                            buffer[i+3] = ((byteBuffer[j + currentIndex] & 0x3f) << 4) |
                                                        ((byteBuffer[j + 1 + currentIndex] & 0xf0) >>> 4);
                                            if (x < xDim - 1) {
                                                m = 2;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 2) {
                                            buffer[i+3] = ((byteBuffer[j + currentIndex] & 0x0f) << 6) |
                                                        ((byteBuffer[j + 1 + currentIndex] & 0xfc)  >>> 2);
                                            if (x < xDim - 1) {
                                                m = 3;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 3) {
                                            buffer[i+3] = ((byteBuffer[j + currentIndex] & 0x03) << 8) |
                                                        (byteBuffer[j + 1 + currentIndex]& 0xff);
                                            m = 0;
                                            j += 2;
                                        }
                                    } 
                                }     

                                planarRGB++;
                            } // end of else for planarRGB >= 2*StripsPerImage    
                        } // else if ((!chunky) && isRGB10 && endianess)
                        else if (chunky && isRGB12 && endianess) {
                            progress = slice * buffer.length;
                            progressLength = buffer.length * imageSlice;
                            mod = progressLength / 10;
                            for (j = 0, m = 0; y < yDim; y++) {
                                for (x = 0; x < xDim; x++, i += 4) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                    }

                                    if (m == 0) {
                                        buffer[i] = 65535;
                                        buffer[i + 1] = ((byteBuffer[j + currentIndex] & 0xff) << 4) |
                                                        ((byteBuffer[j + currentIndex + 1] & 0xf0) >>> 4);
                                        buffer[i + 2] = ((byteBuffer[j + currentIndex + 1] & 0x0f) << 8) |
                                                        (byteBuffer[j + currentIndex + 2] & 0xff);
                                        buffer[i + 3] = ((byteBuffer[j + currentIndex + 3] & 0xff) << 4) |
                                                        ((byteBuffer[j + currentIndex + 4] & 0xf0) >>> 4);
                                        if (x < xDim - 1) {
                                            m = 1;
                                            j += 4;
                                        }
                                        else {
                                            m = 0;
                                            j += 5;
                                        }
                                    } // if (m == 0)
                                    else if (m == 1) {
                                        buffer[i] = 65535;
                                        buffer[i+1] = ((byteBuffer[j + currentIndex] & 0x0f) << 8) |
                                                      (byteBuffer[j + currentIndex + 1] & 0xff);
                                        buffer[i+2] = ((byteBuffer[j + currentIndex + 2] & 0xff) << 4) |
                                                      ((byteBuffer[j + currentIndex + 3] & 0xf0) >>> 4);
                                        buffer[i+3] = ((byteBuffer[j + currentIndex + 3] & 0x0f) << 8) |
                                                      (byteBuffer[j + currentIndex + 4] & 0xff);
                                        m = 0;
                                        j += 5;
                                    } // else if (m == 1)
                                } 
                            }
                        } // else if (chunky && isRGB12 && endianess))
                        else if ((!chunky) && isRGB12 && endianess) {
                            if (planarRGB < stripsPerImage) {

                                // if (byteBuffer == null)
                                // byteBuffer = new byte[2 * buffer.length];
                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;
                                
                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i += 4) {

                                        if ((((i / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + progress) /
                                                                                    progressLength * 100));
                                        }

                                        buffer[i] = 65535;
                                        if (m == 0) {
                                            buffer[i+1] = ((byteBuffer[j + currentIndex] & 0xff) << 4) |
                                                        ((byteBuffer[j + currentIndex + 1] & 0xf0) >>> 4);
                                            if (x < xDim - 1) {
                                                m = 1;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 1) {
                                            buffer[i+1] = ((byteBuffer[j + currentIndex] & 0x0f) << 8) |
                                                        (byteBuffer[j + 1 + currentIndex] & 0xff);
                                            m = 0;
                                            j += 2;
                                        }
                                    } 
                                }     

                                planarRGB++;

                                if (planarRGB == stripsPerImage) {
                                    i = 0;
                                    y = 0;
                                }
                            } // end of if (planarRGB < stripsPerImage)
                            else if (planarRGB < (2 * stripsPerImage)) {

                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;

                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i += 4) {

                                        if ((((i / 3) + (buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + (buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }

                                        if (m == 0) {
                                            buffer[i+2] = ((byteBuffer[j + currentIndex] & 0xff) << 4) |
                                                        ((byteBuffer[j + currentIndex + 1] & 0xf0) >>> 4);
                                            if (x < xDim - 1) {
                                                m = 1;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 1) {
                                            buffer[i+2] = ((byteBuffer[j + currentIndex] & 0x0f) << 8) |
                                                        (byteBuffer[j + 1 + currentIndex] & 0xff);
                                            m = 0;
                                            j += 2;
                                        }
                                    } 
                                }     

                                planarRGB++;

                                if (planarRGB == (2 * stripsPerImage)) {
                                    i = 0;
                                    y = 0;
                                }
                            } // end of else if (planarRGB < 2*stripsPerImage)
                            else { // planarRGB >= 2*stripsPerImage

                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;

                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i += 4) {

                                        if ((((i / 3) + (2 * buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) +
                                                                                         (2 * buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }

                                        if (m == 0) {
                                            buffer[i+3] = ((byteBuffer[j + currentIndex] & 0xff) << 4) |
                                                        ((byteBuffer[j + currentIndex + 1] & 0xf0) >>> 4);
                                            if (x < xDim - 1) {
                                                m = 1;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 1) {
                                            buffer[i+3] = ((byteBuffer[j + currentIndex] & 0x0f) << 8) |
                                                        (byteBuffer[j + 1 + currentIndex] & 0xff);
                                            m = 0;
                                            j += 2;
                                        }
                                    } 
                                }     

                                planarRGB++;
                            } // end of else for planarRGB >= 2*StripsPerImage      
                        } // else if ((!chunky) && isRGB12 && endianess)
                        else if (chunky && isRGB14 && endianess) {
                            progress = slice * buffer.length;
                            progressLength = buffer.length * imageSlice;
                            mod = progressLength / 10;
                            for (j = 0, m = 0; y < yDim; y++) {
                                for (x = 0; x < xDim; x++, i += 4) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                    }

                                    if (m == 0) {
                                        buffer[i] = 65535;
                                        buffer[i + 1] = ((byteBuffer[j + currentIndex] & 0xff) << 6) |
                                                        ((byteBuffer[j + currentIndex + 1] & 0xfc) >>> 2);
                                        buffer[i + 2] = ((byteBuffer[j + currentIndex + 1] & 0x03) << 12) |
                                                        ((byteBuffer[j + currentIndex + 2] & 0xff) << 4) |
                                                        ((byteBuffer[j + currentIndex + 3] & 0xf0) >>> 4);
                                        buffer[i + 3] = ((byteBuffer[j + currentIndex + 3] & 0x0f) << 10) |
                                                        ((byteBuffer[j + currentIndex + 4] & 0xff) << 2) |
                                                        ((byteBuffer[j + currentIndex + 5] & 0xc0) >>> 6);
                                        if (x < xDim - 1) {
                                            m = 1;
                                            j += 5;
                                        }
                                        else {
                                            m = 0;
                                            j += 6;
                                        }
                                    } // if (m == 0)
                                    else if (m == 1) {
                                        buffer[i] = 65535;
                                        buffer[i+1] = ((byteBuffer[j + currentIndex] & 0x3f) << 8) |
                                                      (byteBuffer[j + currentIndex + 1] & 0xff);
                                        buffer[i+2] = ((byteBuffer[j + currentIndex + 2] & 0xff) << 6) |
                                                      ((byteBuffer[j + currentIndex + 3] & 0xfc) >>> 2);
                                        buffer[i+3] = ((byteBuffer[j + currentIndex + 3] & 0x03) << 12) |
                                                      ((byteBuffer[j + currentIndex + 4] & 0xff) <<  4) |
                                                      ((byteBuffer[j + currentIndex + 5] & 0xf0) >>> 4);
                                        if (x < xDim - 1) {
                                            m = 2;
                                            j += 5;
                                        }
                                        else {
                                            m = 0;
                                            j += 6;
                                        }
                                    } // else if (m == 1)
                                    else if (m == 2) {
                                        buffer[i] = 65535;
                                        buffer[i+1] = ((byteBuffer[j + currentIndex] & 0x0f) << 10) |
                                                      ((byteBuffer[j + currentIndex + 1] & 0xff) << 2) |
                                                      ((byteBuffer[j + currentIndex + 2] & 0xc0) >>> 6);
                                        buffer[i+2] = ((byteBuffer[j + currentIndex+ 2] & 0x3f) << 8) |
                                                      (byteBuffer[j + currentIndex + 3] & 0xff);
                                        buffer[i+3] = ((byteBuffer[j + currentIndex + 4] & 0xff) << 6) |
                                                      ((byteBuffer[j + currentIndex + 5] & 0xfc) >>> 2);
                                        if (x < xDim - 1) {
                                            m = 3;
                                            j += 5;
                                        }
                                        else {
                                            m = 0;
                                            j += 6;
                                        }
                                    } // else if (m == 2)
                                    else if (m == 3) {
                                        buffer[i] = 65535;
                                        buffer[i + 1] = ((byteBuffer[j + currentIndex] & 0x03) << 12) |
                                                        ((byteBuffer[j + currentIndex + 1] & 0xff) << 4) |
                                                        ((byteBuffer[j + currentIndex + 2] & 0xf0) >>> 4);
                                        buffer[i + 2] = ((byteBuffer[j + currentIndex + 2] & 0x0f) << 10) |
                                                        ((byteBuffer[j + currentIndex + 3] & 0xff) << 2) |
                                                        ((byteBuffer[j + currentIndex + 4] & 0xc0) >>> 6); 
                                        buffer[i + 3] = ((byteBuffer[j + currentIndex + 4] & 0x3f) << 8) |
                                                        (byteBuffer[j + currentIndex + 5] & 0xff);
                                        m = 0;
                                        j += 6;
                                    } // else if (m == 3)
                                } 
                            }
                        } // else if (chunky && isRGB14 && endianess))
                        else if ((!chunky) && isRGB14 && endianess) {
                            if (planarRGB < stripsPerImage) {

                                // if (byteBuffer == null)
                                // byteBuffer = new byte[2 * buffer.length];
                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;
                                
                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i += 4) {

                                        if ((((i / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + progress) /
                                                                                    progressLength * 100));
                                        }

                                        buffer[i] = 65535;
                                        if (m == 0) {
                                            buffer[i+1] = ((byteBuffer[j + currentIndex] & 0xff) << 6) |
                                                        ((byteBuffer[j + currentIndex + 1] & 0xfc) >>> 2);
                                            if (x < xDim - 1) {
                                                m = 1;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 1) {
                                            buffer[i+1] = ((byteBuffer[j + currentIndex] & 0x03) << 12) |
                                                        ((byteBuffer[j + 1 + currentIndex] & 0xff) << 4) |
                                                        ((byteBuffer[j + 2 + currentIndex] & 0xf0) >>> 4);
                                            if (x < xDim - 1) {
                                                m = 2;
                                                j += 2;
                                            }
                                            else {
                                                m = 0;
                                                j += 3;
                                            }
                                        }
                                        else if (m == 2) {
                                            buffer[i+1] = ((byteBuffer[j + currentIndex] & 0x0f) << 10) |
                                                        ((byteBuffer[j + 1 + currentIndex] & 0xff)  << 2) |
                                                        ((byteBuffer[j + 2 + currentIndex] & 0xc0) >>> 6);
                                            if (x < xDim - 1) {
                                                m = 3;
                                                j += 2;
                                            }
                                            else {
                                                m = 0;
                                                j += 3;
                                            }
                                        }
                                        else if (m == 3) {
                                            buffer[i+1] = ((byteBuffer[j + currentIndex] & 0x3f) << 8) |
                                                        (byteBuffer[j + 1 + currentIndex]& 0xff);
                                            m = 0;
                                            j += 2;
                                        }
                                    } 
                                }     

                                planarRGB++;

                                if (planarRGB == stripsPerImage) {
                                    i = 0;
                                    y = 0;
                                }
                            } // end of if (planarRGB < stripsPerImage)
                            else if (planarRGB < (2 * stripsPerImage)) {

                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;

                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i += 4) {

                                        if ((((i / 3) + (buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + (buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }

                                        if (m == 0) {
                                            buffer[i+2] = ((byteBuffer[j + currentIndex] & 0xff) << 6) |
                                                        ((byteBuffer[j + currentIndex + 1] & 0xfc) >>> 2);
                                            if (x < xDim - 1) {
                                                m = 1;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 1) {
                                            buffer[i+2] = ((byteBuffer[j + currentIndex] & 0x03) << 12) |
                                                        ((byteBuffer[j + 1 + currentIndex] & 0xff) << 4) |
                                                        ((byteBuffer[j + 2 + currentIndex] & 0xf0) >>> 4);
                                            if (x < xDim - 1) {
                                                m = 2;
                                                j += 2;
                                            }
                                            else {
                                                m = 0;
                                                j += 3;
                                            }
                                        }
                                        else if (m == 2) {
                                            buffer[i+2] = ((byteBuffer[j + currentIndex] & 0x0f) << 10) |
                                                        ((byteBuffer[j + 1 + currentIndex] & 0xff)  << 2) |
                                                        ((byteBuffer[j + 2 + currentIndex] & 0xc0) >>> 6);
                                            if (x < xDim - 1) {
                                                m = 3;
                                                j += 2;
                                            }
                                            else {
                                                m = 0;
                                                j += 3;
                                            }
                                        }
                                        else if (m == 3) {
                                            buffer[i+2] = ((byteBuffer[j + currentIndex] & 0x3f) << 8) |
                                                        (byteBuffer[j + 1 + currentIndex]& 0xff);
                                            m = 0;
                                            j += 2;
                                        }
                                    } 
                                }     

                                planarRGB++;

                                if (planarRGB == (2 * stripsPerImage)) {
                                    i = 0;
                                    y = 0;
                                }
                            } // end of else if (planarRGB < 2*stripsPerImage)
                            else { // planarRGB >= 2*stripsPerImage

                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;

                                for (j = 0, m = 0; y < yDim; y++) {
                                    for (x = 0; x < xDim; x++, i += 4) {

                                        if ((((i / 3) + (2 * buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) +
                                                                                         (2 * buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }

                                        if (m == 0) {
                                            buffer[i+3] = ((byteBuffer[j + currentIndex] & 0xff) << 6) |
                                                        ((byteBuffer[j + currentIndex + 1] & 0xfc) >>> 2);
                                            if (x < xDim - 1) {
                                                m = 1;
                                                j++;
                                            }
                                            else {
                                                m = 0;
                                                j += 2;
                                            }
                                        }
                                        else if (m == 1) {
                                            buffer[i+3] = ((byteBuffer[j + currentIndex] & 0x03) << 12) |
                                                        ((byteBuffer[j + 1 + currentIndex] & 0xff) << 4) |
                                                        ((byteBuffer[j + 2 + currentIndex] & 0xf0) >>> 4);
                                            if (x < xDim - 1) {
                                                m = 2;
                                                j += 2;
                                            }
                                            else {
                                                m = 0;
                                                j += 3;
                                            }
                                        }
                                        else if (m == 2) {
                                            buffer[i+3] = ((byteBuffer[j + currentIndex] & 0x0f) << 10) |
                                                        ((byteBuffer[j + 1 + currentIndex] & 0xff)  << 2) |
                                                        ((byteBuffer[j + 2 + currentIndex] & 0xc0) >>> 6);
                                            if (x < xDim - 1) {
                                                m = 3;
                                                j += 2;
                                            }
                                            else {
                                                m = 0;
                                                j += 3;
                                            }
                                        }
                                        else if (m == 3) {
                                            buffer[i+3] = ((byteBuffer[j + currentIndex] & 0x3f) << 8) |
                                                        (byteBuffer[j + 1 + currentIndex]& 0xff);
                                            m = 0;
                                            j += 2;
                                        }
                                    } 
                                }     

                                planarRGB++;
                            } // end of else for planarRGB >= 2*StripsPerImage      
                        } // else if ((!chunky) && isRGB14 && endianess)
                        else if (chunky == true) {

                            // if (byteBuffer == null)
                            // byteBuffer = new byte[2 * buffer.length];
                            // raFile.read(byteBuffer, 0, nBytes);
                            progress = slice * buffer.length;
                            progressLength = buffer.length * imageSlice;
                            mod = progressLength / 10;


                            // For the moment I compress RGB images to unsigned bytes.
                            for (j = 0; j < nBytes; j += 6, i += 4) {

                                if (((i + progress) % mod) == 0) {
                                    fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                }

                                buffer[i] = 65535;
                                b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);

                                if (endianess) {
                                    buffer[i + 1] = ((b1 << 8) + b2);
                                } else {
                                    buffer[i + 1] = ((b2 << 8) + b1);
                                }

                                b1 = getUnsignedByte(byteBuffer, j + currentIndex + 2);
                                b2 = getUnsignedByte(byteBuffer, j + currentIndex + 3);

                                if (endianess) {
                                    buffer[i + 2] = ((b1 << 8) + b2);
                                } else {
                                    buffer[i + 2] = ((b2 << 8) + b1);
                                }

                                b1 = getUnsignedByte(byteBuffer, j + currentIndex + 4);
                                b2 = getUnsignedByte(byteBuffer, j + currentIndex + 5);

                                if (endianess) {
                                    buffer[i + 3] = ((b1 << 8) + b2);
                                } else {
                                    buffer[i + 3] = ((b2 << 8) + b1);
                                }
                            }
                        } // if (chunky == true)
                        else { // planar RGB configuration

                            if (planarRGB < stripsPerImage) {

                                // if (byteBuffer == null)
                                // byteBuffer = new byte[2 * buffer.length];
                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;


                                // For the moment I compress RGB images to unsigned bytes
                                for (j = 0; j < nBytes; j += 2, i += 4) {

                                    if ((((i / 3) + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) ((i / 3) + progress) /
                                                                                progressLength * 100));
                                    }

                                    buffer[i] = 65535;
                                    b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                    b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);

                                    if (endianess) {
                                        buffer[i + 1] = ((b1 << 8) + b2);
                                    } else {
                                        buffer[i + 1] = ((b2 << 8) + b1);
                                    }
                                }

                                planarRGB++;

                                if (planarRGB == stripsPerImage) {
                                    i = 0;
                                }
                            } // end of if (planarRGB < stripsPerImage)
                            else if (planarRGB < (2 * stripsPerImage)) {

                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;


                                for (j = 0; j < nBytes; j += 2, i += 4) {

                                    if ((((i / 3) + (buffer.length / 3) + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) ((i / 3) + (buffer.length / 3) +
                                                                                     progress) / progressLength * 100));
                                    }

                                    b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                    b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);

                                    if (endianess) {
                                        buffer[i + 2] = ((b1 << 8) + b2);
                                    } else {
                                        buffer[i + 2] = ((b2 << 8) + b1);
                                    }
                                }

                                planarRGB++;

                                if (planarRGB == (2 * stripsPerImage)) {
                                    i = 0;
                                }
                            } // end of else if (planarRGB < 2*stripsPerImage)
                            else { // planarRGB >= 2*stripsPerImage

                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;


                                for (j = 0; j < nBytes; j += 2, i += 4) {

                                    if ((((i / 3) + (2 * buffer.length / 3) + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) ((i / 3) +
                                                                                     (2 * buffer.length / 3) +
                                                                                     progress) / progressLength * 100));
                                    }

                                    b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                    b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);

                                    if (endianess) {
                                        buffer[i + 3] = ((b1 << 8) + b2);
                                    } else {
                                        buffer[i + 3] = ((b2 << 8) + b1);
                                    }
                                }

                                planarRGB++;
                            } // end of else for planarRGB >= 2*StripsPerImage
                        } // end of else for planar RGB configuration

                        break;
                    case ModelStorageBase.ARGB_FLOAT:
                        if (chunky) {
                            if (isRGB24UINTtoFLOAT) { // cast UINTEGER To FLOAT
                                // if (byteBuffer == null)
                                // byteBuffer = new byte[3 * buffer.length];
                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;


                                for (j = 0; j < nBytes; j += 9, i += 4) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                    }

                                    buffer[i] = 255.0f;
                                    b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                    b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);
                                    b3 = getUnsignedByte(byteBuffer, j + currentIndex + 2);

                                    if (endianess) {
                                        buffer[i + 1] = (float)((b1 << 16) | (b2 << 8) | b3);
                                    } else {
                                        buffer[i + 1] = (float)((b3 << 16) | (b2 << 8) | b1);
                                    }
                                    
                                    b1 = getUnsignedByte(byteBuffer, j + currentIndex + 3);
                                    b2 = getUnsignedByte(byteBuffer, j + currentIndex + 4);
                                    b3 = getUnsignedByte(byteBuffer, j + currentIndex + 5);

                                    if (endianess) {
                                        buffer[i + 2] = (float)((b1 << 16) | (b2 << 8) | b3);
                                    } else {
                                        buffer[i + 2] = (float)((b3 << 16) | (b2 << 8) | b1);
                                    }
                                    
                                    b1 = getUnsignedByte(byteBuffer, j + currentIndex + 6);
                                    b2 = getUnsignedByte(byteBuffer, j + currentIndex + 7);
                                    b3 = getUnsignedByte(byteBuffer, j + currentIndex + 8);

                                    if (endianess) {
                                        buffer[i + 3] = (float)((b1 << 16) | (b2 << 8) | b3);
                                    } else {
                                        buffer[i + 3] = (float)((b3 << 16) | (b2 << 8) | b1);
                                    }
                                }    
                            } // if (isRGB24UINTtoFLOAT)
                            else if (isRGB32UINTtoFLOAT) { // cast UINTEGER to FLOAT
                                // if (byteBuffer == null)
                                // byteBuffer = new byte[4 * buffer.length];
                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;


                                for (j = 0; j < nBytes; j += 12, i += 4) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                    }

                                    buffer[i] = 255.0f;
                                    b1L = (getUnsignedByte(byteBuffer, j + currentIndex) & 0xffL);
                                    b2L = (getUnsignedByte(byteBuffer, j + currentIndex + 1) & 0xffL);
                                    b3L = (getUnsignedByte(byteBuffer, j + currentIndex + 2) & 0xffL);
                                    b4L = (getUnsignedByte(byteBuffer, j + currentIndex + 3) & 0xffL);

                                    if (endianess) {
                                        buffer[i + 1] = (float)((b1L << 24) | (b2L << 16) | (b3L << 8) | b4L);
                                    } else {
                                        buffer[i + 1] = (float)((b4L << 24) | (b3L << 16) | (b2L << 8) | b1L);
                                    }
                                    
                                    b1L = (getUnsignedByte(byteBuffer, j + currentIndex + 4) & 0xffL);
                                    b2L = (getUnsignedByte(byteBuffer, j + currentIndex + 5) & 0xffL);
                                    b3L = (getUnsignedByte(byteBuffer, j + currentIndex + 6) & 0xffL);
                                    b4L = (getUnsignedByte(byteBuffer, j + currentIndex + 7) & 0xffL);

                                    if (endianess) {
                                        buffer[i + 2] = (float)((b1L << 24) | (b2L << 16) | (b3L << 8) | b4L);
                                    } else {
                                        buffer[i + 2] = (float)((b4L << 24) | (b3L << 16) | (b2L << 8) | b1L);
                                    }
                                    
                                    b1L = (getUnsignedByte(byteBuffer, j + currentIndex + 8) & 0xffL);
                                    b2L = (getUnsignedByte(byteBuffer, j + currentIndex + 9) & 0xffL);
                                    b3L = (getUnsignedByte(byteBuffer, j + currentIndex + 10) & 0xffL);
                                    b4L = (getUnsignedByte(byteBuffer, j + currentIndex + 11) & 0xffL);

                                    if (endianess) {
                                        buffer[i + 3] = (float)((b1L << 24) | (b2L << 16) | (b3L << 8) | b4L);
                                    } else {
                                        buffer[i + 3] = (float)((b4L << 24) | (b3L << 16) | (b2L << 8) | b1L);
                                    }
                                }        
                            } // else if (isRGB32UINTtoFLOAT)
                            else { // ordinary 32 bit float
//                              if (byteBuffer == null)
                                // byteBuffer = new byte[4 * buffer.length];
                                // raFile.read(byteBuffer, 0, nBytes);
                                progress = slice * buffer.length;
                                progressLength = buffer.length * imageSlice;
                                mod = progressLength / 10;


                                for (j = 0; j < nBytes; j += 12, i += 4) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                                    }

                                    buffer[i] = 255.0f;
                                    b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                    b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);
                                    b3 = getUnsignedByte(byteBuffer, j + currentIndex + 2);
                                    b4 = getUnsignedByte(byteBuffer, j + currentIndex + 3);

                                    if (endianess) {
                                        tmpInt = ((b1 << 24) | (b2 << 16) | (b3 << 8) | b4);
                                    } else {
                                        tmpInt = ((b4 << 24) | (b3 << 16) | (b2 << 8) | b1);
                                    }
                                    buffer[i+1] = Float.intBitsToFloat(tmpInt);
                                    
                                    b1 = getUnsignedByte(byteBuffer, j + currentIndex + 4);
                                    b2 = getUnsignedByte(byteBuffer, j + currentIndex + 5);
                                    b3 = getUnsignedByte(byteBuffer, j + currentIndex + 6);
                                    b4 = getUnsignedByte(byteBuffer, j + currentIndex + 7);

                                    if (endianess) {
                                        tmpInt = ((b1 << 24) | (b2 << 16) | (b3 << 8) | b4);
                                    } else {
                                        tmpInt = ((b4 << 24) | (b3 << 16) | (b2 << 8) | b1);
                                    }
                                    buffer[i+2] = Float.intBitsToFloat(tmpInt);
                                     
                                    b1 = getUnsignedByte(byteBuffer, j + currentIndex + 8);
                                    b2 = getUnsignedByte(byteBuffer, j + currentIndex + 9);
                                    b3 = getUnsignedByte(byteBuffer, j + currentIndex + 10);
                                    b4 = getUnsignedByte(byteBuffer, j + currentIndex + 11);

                                    if (endianess) {
                                        tmpInt = ((b1 << 24) | (b2 << 16) | (b3 << 8) | b4);
                                    } else {
                                        tmpInt = ((b4 << 24) | (b3 << 16) | (b2 << 8) | b1);
                                    }
                                    buffer[i+3] = Float.intBitsToFloat(tmpInt);
                                }            
                            }
                        } // if (chunky)
                        else { // planar
                            if (isRGB24UINTtoFLOAT) { // cast UINTEGER to FLOAT
                                if (planarRGB < stripsPerImage) {

                                    // if (byteBuffer == null)
                                    // byteBuffer = new byte[3 * buffer.length];
                                    // raFile.read(byteBuffer, 0, nBytes);
                                    progress = slice * buffer.length;
                                    progressLength = buffer.length * imageSlice;
                                    mod = progressLength / 10;

                                    for (j = 0; j < nBytes; j += 3, i += 4) {

                                        if ((((i / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + progress) /
                                                                                    progressLength * 100));
                                        }

                                        buffer[i] = 255.0f;
                                        b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                        b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);
                                        b3 = getUnsignedByte(byteBuffer, j + currentIndex + 2);

                                        if (endianess) {
                                            buffer[i + 1] = (float)((b1 << 16) | (b2 << 8) | b3);
                                        } else {
                                            buffer[i + 1] = (float)((b3 << 16) | (b2 << 8) | b1);
                                        }
                                    }

                                    planarRGB++;

                                    if (planarRGB == stripsPerImage) {
                                        i = 0;
                                    }
                                } // end of if (planarRGB < stripsPerImage)
                                else if (planarRGB < (2 * stripsPerImage)) {

                                    // raFile.read(byteBuffer, 0, nBytes);
                                    progress = slice * buffer.length;
                                    progressLength = buffer.length * imageSlice;
                                    mod = progressLength / 10;


                                    for (j = 0; j < nBytes; j += 3, i += 4) {

                                        if ((((i / 3) + (buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + (buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }

                                        b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                        b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);
                                        b3 = getUnsignedByte(byteBuffer, j + currentIndex + 2);

                                        if (endianess) {
                                            buffer[i + 2] = (float)((b1 << 16) | (b2 << 8) | b3);
                                        } else {
                                            buffer[i + 2] = (float)((b3 << 16) | (b2 << 8) | b1);
                                        }
                                    }

                                    planarRGB++;

                                    if (planarRGB == (2 * stripsPerImage)) {
                                        i = 0;
                                    }
                                } // end of else if (planarRGB < 2*stripsPerImage)
                                else { // planarRGB >= 2*stripsPerImage

                                    // raFile.read(byteBuffer, 0, nBytes);
                                    progress = slice * buffer.length;
                                    progressLength = buffer.length * imageSlice;
                                    mod = progressLength / 10;


                                    for (j = 0; j < nBytes; j += 3, i += 4) {

                                        if ((((i / 3) + (2 * buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) +
                                                                                         (2 * buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }

                                        b1 = getUnsignedByte(byteBuffer, j + currentIndex);
                                        b2 = getUnsignedByte(byteBuffer, j + currentIndex + 1);
                                        b3 = getUnsignedByte(byteBuffer, j + currentIndex + 2);

                                        if (endianess) {
                                            buffer[i + 3] = (float)((b1 << 16) | (b2 << 8) | b3);
                                        } else {
                                            buffer[i + 3] = (float)((b3 << 16) | (b2 << 8) | b1);
                                        }
                                    }

                                    planarRGB++;
                                } // end of else for planarRGB >= 2*StripsPerImage    
                            } // if (isRGB24UINTtoFLOAT)
                            else if (isRGB32UINTtoFLOAT) { // cast UINTEGER to FLOAT
                                if (planarRGB < stripsPerImage) {

                                    // if (byteBuffer == null)
                                    // byteBuffer = new byte[3 * buffer.length];
                                    // raFile.read(byteBuffer, 0, nBytes);
                                    progress = slice * buffer.length;
                                    progressLength = buffer.length * imageSlice;
                                    mod = progressLength / 10;

                                    for (j = 0; j < nBytes; j += 4, i += 4) {

                                        if ((((i / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + progress) /
                                                                                    progressLength * 100));
                                        }

                                        buffer[i] = 255.0f;
                                        b1L = (getUnsignedByte(byteBuffer, j + currentIndex) & 0xffL);
                                        b2L = (getUnsignedByte(byteBuffer, j + currentIndex + 1) & 0xffL);
                                        b3L = (getUnsignedByte(byteBuffer, j + currentIndex + 2) & 0xffL);
                                        b4L = (getUnsignedByte(byteBuffer, j + currentIndex + 3) & 0xffL);

                                        if (endianess) {
                                            buffer[i + 1] = (float)((b1L << 24) | (b2L << 16) | (b3L << 8) | b4L);
                                        } else {
                                            buffer[i + 1] = (float)((b4L << 24) | (b3L << 16) | (b2L << 8) | b1L);
                                        }
                                    }

                                    planarRGB++;

                                    if (planarRGB == stripsPerImage) {
                                        i = 0;
                                    }
                                } // end of if (planarRGB < stripsPerImage)
                                else if (planarRGB < (2 * stripsPerImage)) {

                                    // raFile.read(byteBuffer, 0, nBytes);
                                    progress = slice * buffer.length;
                                    progressLength = buffer.length * imageSlice;
                                    mod = progressLength / 10;


                                    for (j = 0; j < nBytes; j += 4, i += 4) {

                                        if ((((i / 3) + (buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) + (buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }

                                        b1L = (getUnsignedByte(byteBuffer, j + currentIndex) & 0xffL);
                                        b2L = (getUnsignedByte(byteBuffer, j + currentIndex + 1) & 0xffL);
                                        b3L = (getUnsignedByte(byteBuffer, j + currentIndex + 2) & 0xffL);
                                        b4L = (getUnsignedByte(byteBuffer, j + currentIndex + 3) & 0xffL);

                                        if (endianess) {
                                            buffer[i + 2] = (float)((b1L << 24) | (b2L << 16) | (b3L << 8) | b4L);
                                        } else {
                                            buffer[i + 2] = (float)((b4L << 24) | (b3L << 16) | (b2L << 8) | b1L);
                                        }
                                    }

                                    planarRGB++;

                                    if (planarRGB == (2 * stripsPerImage)) {
                                        i = 0;
                                    }
                                } // end of else if (planarRGB < 2*stripsPerImage)
                                else { // planarRGB >= 2*stripsPerImage

                                    // raFile.read(byteBuffer, 0, nBytes);
                                    progress = slice * buffer.length;
                                    progressLength = buffer.length * imageSlice;
                                    mod = progressLength / 10;


                                    for (j = 0; j < nBytes; j += 4, i += 4) {

                                        if ((((i / 3) + (2 * buffer.length / 3) + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) ((i / 3) +
                                                                                         (2 * buffer.length / 3) +
                                                                                         progress) / progressLength * 100));
                                        }

                                        b1L = (getUnsignedByte(byteBuffer, j + currentIndex) & 0xffL);
                                        b2L = (getUnsignedByte(byteBuffer, j + currentIndex + 1) & 0xffL);
                                        b3L = (getUnsignedByte(byteBuffer, j + currentIndex + 2) & 0xffL);
                                        b4L = (getUnsignedByte(byteBuffer, j + currentIndex + 3) & 0xffL);

                                        if (endianess) {
                                            buffer[i + 3] = (float)((b1L << 24) | (b2L << 16) | (b3L << 8) | b4L);
                                        } else {
                                            buffer[i + 3] = (float)((b4L << 24) | (b3L << 16) | (b2L << 8) | b1L);
                                        }
                                    }

                                    planarRGB++;
                                } // end of else for planarRGB >= 2*StripsPerImage        
                            } // else if (isRGB32UINTtoFLOAT)
                        } // else planar
                        break;
                        
                }
            } catch (OutOfMemoryError error) {
                System.gc();
                throw error;
            }
        }
        if (isYCbCr) {
            YCbCrtoRGB(buffer, YBuffer, CbInBuffer, CrInBuffer);
        }
    }

    /**
     * Reads a slice of data at a time and stores the results in the buffer.
     *
     * @param      slice   offset into the file stored in the dataOffset array
     * @param      buffer  buffer where the info is stored
     *
     * @exception  IOException  if there is an error reading the file
     */
    private void readDoubleBuffer(int slice, double[] buffer) throws IOException {
        int i, j, a;
        long b1, b2, b3, b4, b5, b6, b7, b8;
        long progress, progressLength, mod;

        // long pointer;
        int idx = 0;
        int nBytes;

        // int planarRGB = 0; // Use this for planar RGB where you must read a stripsPerImage
        // number of red strips, followed by a stripsPerImage number of
        // green strips, followed by a stripsPerImage number of blue strips.
        int nIndex = dataOffsets[slice].size();

        // int stripsPerImage = nIndex/3; // used for planar RGB
        long tmpLong;

        // System.err.println("Datatype is: " + fileInfo.getDataType());
        // System.err.println("number of data offsets: " + nIndex);
        // System.err.println("buffer length: " + buffer.length);
        // ben mod: try calculating total length you'd need to read in ALL at one time
        int firstIndex = ((Index) (dataOffsets[slice].elementAt(0))).index;
        int lastIndex = ((Index) (dataOffsets[slice].elementAt(nIndex - 1))).index;
        int totalLength = (lastIndex - firstIndex) + ((Index) (dataOffsets[slice].elementAt(nIndex - 1))).byteCount;
        int currentIndex = 0;

        // System.err.println("first index: " + firstIndex + ", last index: " + lastIndex + ", totalLength: " +
        // totalLength);
        // System.err.println("packbit is: " + packBit);
        byteBuffer = new byte[totalLength];
        raFile.seek(firstIndex);
        raFile.read(byteBuffer, 0, totalLength);
        i = 0;

        for (a = 0; a < nIndex; a++, idx++) {

            try {
                currentIndex = ((Index) (dataOffsets[slice].elementAt(idx))).index - firstIndex;

                // raFile.seek( ( (Index) (dataOffsets[slice].elementAt(idx))).index);
                nBytes = ((Index) (dataOffsets[slice].elementAt(idx))).byteCount;

                if (nBytes == 0) {
                    nBytes = buffer.length;
                }

                ;

                if (byteBuffer == null) {
                    byteBuffer = new byte[8 * buffer.length];
                }

                raFile.read(byteBuffer, 0, nBytes);
                progress = slice * buffer.length;
                progressLength = buffer.length * imageSlice;
                mod = progressLength / 10;


                for (j = 0; j < nBytes; j += 8, i++) {

                    if (((i + progress) % mod) == 0) {
                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength * 100));
                    }

                    b1 = byteBuffer[j + currentIndex] & 0xff;
                    b2 = byteBuffer[j + currentIndex + 1] & 0xff;
                    b3 = byteBuffer[j + currentIndex + 2] & 0xff;
                    b4 = byteBuffer[j + currentIndex + 3] & 0xff;
                    b5 = byteBuffer[j + currentIndex + 4] & 0xff;
                    b6 = byteBuffer[j + currentIndex + 5] & 0xff;
                    b7 = byteBuffer[j + currentIndex + 6] & 0xff;
                    b8 = byteBuffer[j + currentIndex + 7] & 0xff;

                    if (endianess) {
                        tmpLong = ((b1 << 56) | (b2 << 48) | (b3 << 40) | (b4 << 32) | (b5 << 24) | (b6 << 16) |
                                       (b7 << 8) | b8); // Big Endian
                    } else {
                        tmpLong = ((b8 << 56) | (b7 << 48) | (b6 << 40) | (b5 << 32) | (b4 << 24) | (b3 << 16) |
                                       (b2 << 8) | b1);
                    }

                    buffer[i] = Double.longBitsToDouble(tmpLong);
                } // for (j =0; j < nBytes; j+=8, i++ )
            } catch (OutOfMemoryError error) {
                System.gc();
                throw error;
            }
        }
    }

    /**
     * Reads a slice of data at a time and stores the results in the buffer.
     *
     * @param      slice   image slice
     * @param      buffer  buffer where the info is stored
     *
     * @exception  IOException  if there is an error reading the file
     */
    private void readTileBuffer(int slice, float[] buffer) throws IOException {
        int a, i, j;
        int iCount, iNext;
        int b1, b2, b3, b4, b5, b6, b7, b8;
        long b1L, b2L, b3L, b4L, b5L, b6L, b7L, b8L;
        long tmpLong;
        double tmpDouble;
        long progress, progressLength, mod;
        int nBytes;
        int nLength;
        int xTile, yTile;
        int x, y;
        byte[] decomp = null;
        int resultLength = 0;
        i = 0;
        xTile = 0;
        yTile = 0;
        x = 0;
        y = 0;
        byte[] data;
        
        int planarRGB = 0; // Use this for planar RGB where you must read a stripsPerImage
                           // number of red strips, followed by a stripsPerImage number of
                           // green strips, followed by a stripsPerImage number of blue strips.
        int stripsPerImage = tilesPerSlice/3; // used for planar RGB
        // Buffers needed if photometric is YCbCr
        int YBuffer[] = null;
        int CbInBuffer[] = null;
        int CrInBuffer[] = null;
        int CbCrXSize;
        int CbCrYSize;
        int CbCrInSize;
        int Y00;
        int Y01;
        int Y10;
        int Y11;
        int Cb00;
        int Cr00;
        int h;
        int w;
        int halfXDim = xDim/2;
        int rowsToDo;
        
        if (isYCbCr) {
            YBuffer = new int[buffer.length/4];
            CbCrXSize = xDim/YCbCrSubsampleHoriz;
            if ((xDim % YCbCrSubsampleHoriz) != 0) {
                CbCrXSize++;
            }
            CbCrYSize = yDim/YCbCrSubsampleVert;
            if ((yDim % YCbCrSubsampleVert) != 0) {
                CbCrYSize++;
            }
            CbCrInSize = CbCrXSize * CbCrYSize;
            CbInBuffer = new int[CbCrInSize];
            CrInBuffer = new int[CbCrInSize];
        }

        for (a = 0; a < tilesPerSlice; a++) {

            try {

                // System.err.println("___________________________________________________");
                // System.err.println("A = " + a + ", seeking to " + tileOffsets[slice * tilesPerSlice + a]);
                // System.err.println("Starting with x = " + x + " and y = " + y);
                raFile.seek(tileOffsets[(slice * tilesPerSlice) + a]);
                nBytes = tileByteCounts[(slice * tilesPerSlice) + a];

                switch (fileInfo.getDataType()) {

                    case ModelStorageBase.BOOLEAN:
                        progress = slice * xDim * yDim;
                        progressLength = imageSlice * xDim * yDim;
                        mod = progressLength / 100;

                        nLength = 8 * ((nBytes + 63) >> 6); // new BitSet(size) = new long[(size+63)>>6];
                        if (lzwCompression || zlibCompression || fax3Compression || fax4Compression || modHuffmanCompression) {

                            if (byteBuffer == null) {
                                byteBuffer = new byte[tileMaxByteCount];
                            }

                            raFile.read(byteBuffer, 0, nBytes);
                            if (zlibCompression) {
                                zlibDecompresser.setInput(byteBuffer, 0, nBytes);
                            }
                            

                            if (decomp == null) {
                                decomp = new byte[tileWidth * tileLength];
                            }

                            if (lzwCompression) {
                                //lzwDecoder.decode(byteBuffer, decomp, tileLength);
                                rowsToDo = Math.min(tileLength, yDim - y);
                                LZWDecompresser(byteBuffer, nBytes, decomp, y, rowsToDo, tileWidth);
                                resultLength = decomp.length;
                            }
                            else if (fax3Compression || fax4Compression) {
                                data = new byte[nBytes];
                                for (j = 0; j < nBytes; j++) {
                                    data[j] = byteBuffer[j];
                                }
                                // Note that resultLength is in bytes,
                                // but we use each bit int the byte
                                resultLength = fax34Decompresser(decomp, data);
                            }
                            else if (modHuffmanCompression) {
                                data = new byte[nBytes];
                                for (j = 0; j < nBytes; j++) {
                                    data[j] = byteBuffer[j];
                                }
                                // Note that resultLength is in bytes,
                                // but we use each bit int the byte
                                resultLength = modHuffmanDecompresser(decomp, data);
                            }
                            else { // zlibCompression
                                try {
                                    resultLength = zlibDecompresser.inflate(decomp);
                                }
                                catch (DataFormatException e){
                                    MipavUtil.displayError("DataFormatException on zlibDecompresser.inflate(decomp)");  
                                }
                                zlibDecompresser.reset();
                            }

                            for (j = 0; j < 8*resultLength; j++) {

                                if ((x < xDim) && (y < yDim)) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength *
                                                                                100));
                                    }

                                    buffer[x + (y * xDim)] = decomp[j >> 3] & (1 << (7 - (j %8)));
                                    i++;
                                }

                                x++;

                                if (x == ((xTile + 1) * tileWidth)) {
                                    x = xTile * tileWidth;
                                    if ((j % 8) != 7) {
                                        j = j + 7 - (j % 8);
                                    }
                                    y++;
                                }
                            }
                        } else {
                            byteBuffer = new byte[nLength];
                            raFile.read(byteBuffer, 0, nLength);

                            for (j = 0; j < nBytes; j++) {

                                if ((x < xDim) && (y < yDim)) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength *
                                                                                100));
                                    }

                                    buffer[x + (y * xDim)] = byteBuffer[j >> 3] & (1 << (j % 8));
                                    i++;
                                } // if ((x < xDim) && (y < yDim))

                                x++;

                                if (x == ((xTile + 1) * tileWidth)) {
                                    x = xTile * tileWidth;
                                    y++;
                                }
                            } // for (j = 0; j < nBytes; j++)
                        }

                        xTile++;
                        if (xTile == tilesAcross) {
                            xTile = 0;
                            yTile++;
                        }

                        x = xTile * tileWidth;
                        y = yTile * tileLength;
                        break;

                    case ModelStorageBase.BYTE:
                        if (packBit == false) {

                            if (byteBuffer == null) {

                                if (lzwCompression || zlibCompression) {
                                    byteBuffer = new byte[tileMaxByteCount];
                                } else {
                                    byteBuffer = new byte[nBytes];
                                }
                            }

                            raFile.read(byteBuffer, 0, nBytes);
                            if (zlibCompression) {
                                zlibDecompresser.setInput(byteBuffer, 0, nBytes);
                            }
                            progress = slice * xDim * yDim;
                            progressLength = imageSlice * xDim * yDim;
                            mod = progressLength / 100;


                            if (lzwCompression || zlibCompression) {

                                if (decomp == null) {
                                    decomp = new byte[tileWidth * tileLength];
                                }

                                if (lzwCompression) {
                                    //lzwDecoder.decode(byteBuffer, decomp, tileLength);
                                    rowsToDo = Math.min(tileLength, yDim - y);
                                    LZWDecompresser(byteBuffer, nBytes, decomp, y, rowsToDo, tileWidth);
                                    resultLength = decomp.length;
                                }
                                else { // zlibCompression
                                    try {
                                        resultLength = zlibDecompresser.inflate(decomp);
                                    }
                                    catch (DataFormatException e){
                                        MipavUtil.displayError("DataFormatException on zlibDecompresser.inflate(decomp)");  
                                    }
                                    zlibDecompresser.reset();
                                }

                                for (j = 0; j < resultLength; j++) {

                                    if ((x < xDim) && (y < yDim)) {

                                        if (((i + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                    progressLength * 100));
                                        }

                                        buffer[x + (y * xDim)] = decomp[j] & 0xff;
                                        i++;
                                    }

                                    x++;

                                    if (x == ((xTile + 1) * tileWidth)) {
                                        x = xTile * tileWidth;
                                        y++;
                                    }
                                }
                            } else {

                                for (j = 0; j < nBytes; j++) {

                                    if ((x < xDim) && (y < yDim)) {

                                        if (((i + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                    progressLength * 100));
                                        }

                                        buffer[x + (y * xDim)] = byteBuffer[j];
                                        i++;
                                    } // if ((x < xDim) && (y < yDim))

                                    x++;

                                    if (x == ((xTile + 1) * tileWidth)) {
                                        x = xTile * tileWidth;
                                        y++;
                                    }
                                } // for (j = 0; j < nBytes; j++)
                            }

                            xTile++;

                            if (xTile == tilesAcross) {
                                xTile = 0;
                                yTile++;
                            }

                            x = xTile * tileWidth;
                            y = yTile * tileLength;
                        } // if (packBit == false)
                        else if (packBit == true) {
                            byteBuffer = new byte[nBytes];
                            raFile.read(byteBuffer, 0, nBytes);
                            progress = slice * xDim * yDim;
                            progressLength = imageSlice * xDim * yDim;
                            mod = progressLength / 100;

                            j = 0;

                            while (j < nBytes) {

                                // uncompressed data bytes follow
                                // Copy the next n+1 bytes literally
                                if ((byteBuffer[j] & 0x80) == 0) {
                                    iCount = byteBuffer[j] + 1;
                                    j++;

                                    for (iNext = 0; iNext < iCount; iNext++, j++) {

                                        if ((x < xDim) && (y < yDim)) {

                                            if (((i + progress) % mod) == 0) {
                                                fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                        progressLength * 100));
                                            }

                                            buffer[x + (y * xDim)] = byteBuffer[j];
                                            i++;
                                        } // if ((x < xDim) && (y < yDim))

                                        x++;

                                        if (x == ((xTile + 1) * tileWidth)) {
                                            x = xTile * tileWidth;
                                            y++;
                                        } // if (x == (xTile+1)*tileWidth)
                                    } // for (iNext = 0; iNext < iCount; iNext++,j++)
                                } // end of if (byteBuffer[j] & 0x80 == 0)

                                // Do nothing if the byte value is -128
                                else if (byteBuffer[j] == -128) {
                                    j++;
                                }
                                // compressed data bytes follow
                                // (~byteBuffer[j]) + 1 is the 2's complement of n or -n
                                // Hence (~byteBuffer[j]) + 2 equals -n + 1
                                else {
                                    iCount = (~byteBuffer[j]) + 2;
                                    j++;

                                    for (iNext = 0; iNext < iCount; iNext++, i++) {

                                        if ((x < xDim) && (y < yDim)) {

                                            if (((i + progress) % mod) == 0) {
                                                fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                        progressLength * 100));
                                            }

                                            buffer[x + (y * xDim)] = byteBuffer[j];
                                        } // if ((x < xDim) && (y < yDim))

                                        x++;

                                        if (x == ((xTile + 1) * tileWidth)) {
                                            x = xTile * tileWidth;
                                            y++;
                                        } // if (x == (xTile+1)*tileWidth)
                                    } // for (iNext = 0; iNext < iCount; iNext++,i++)

                                    j++;
                                } // end of else for compressed data bytes
                            } // end of while (j < nBytes)

                            xTile++;

                            if (xTile == tilesAcross) {
                                xTile = 0;
                                yTile++;
                            }

                            x = xTile * tileWidth;
                            y = yTile * tileLength;
                        } // else if (packBit == true)

                        break;

                    case ModelStorageBase.UBYTE:
                        if (packBit == false) {

                            if (byteBuffer == null) {

                                if (lzwCompression || zlibCompression || ThunderScanCompression) {
                                    byteBuffer = new byte[tileMaxByteCount];
                                } else {
                                    byteBuffer = new byte[nBytes];
                                }
                            }

                            raFile.read(byteBuffer, 0, nBytes);
                            if (zlibCompression) {
                                zlibDecompresser.setInput(byteBuffer, 0, nBytes);
                            }
                            progress = slice * xDim * yDim;
                            progressLength = imageSlice * xDim * yDim;
                            mod = progressLength / 100;


                            if (lzwCompression || zlibCompression || ThunderScanCompression) {

                                if (decomp == null) {
                                    decomp = new byte[tileWidth * tileLength];
                                }

                                if (lzwCompression) {
                                    //lzwDecoder.decode(byteBuffer, decomp, tileLength);
                                    rowsToDo = Math.min(tileLength, yDim - y);
                                    LZWDecompresser(byteBuffer, nBytes, decomp, y, rowsToDo, tileWidth);
                                    resultLength = decomp.length;
                                }
                                else if (ThunderScanCompression) {
                                    rowsToDo = Math.min(tileLength, yDim - y);
                                    resultLength = ThunderScanDecompresser(decomp, byteBuffer, nBytes, rowsToDo);
                                }
                                else { // zlibCompression
                                    try {
                                        resultLength = zlibDecompresser.inflate(decomp);
                                    }
                                    catch (DataFormatException e){
                                        MipavUtil.displayError("DataFormatException on zlibDecompresser.inflate(decomp)");  
                                    }
                                    zlibDecompresser.reset();
                                }

                                for (j = 0; j < resultLength; j++) {

                                    if ((x < xDim) && (y < yDim)) {

                                        if (((i + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                    progressLength * 100));
                                        }

                                        buffer[x + (y * xDim)] = decomp[j] & 0xff;
                                        if (fileInfo.getPhotometric() == 0) {
                                            // white is 0 in original readin
                                            if (ThunderScanCompression) {
                                                buffer[x + (y * xDim)] = 15 - buffer[x + (y * xDim)];
                                            }
                                            else {
                                                buffer[x + (y * xDim)] = 255 - buffer[x + (y * xDim)];
                                            }
                                        } // if (fileInfo.getPhotometric() == 0)
                                        i++;
                                    }

                                    x++;

                                    if (x == ((xTile + 1) * tileWidth)) {
                                        x = xTile * tileWidth;
                                        y++;
                                    }
                                }
                            } else {

                                for (j = 0; j < nBytes; j++) {

                                    if ((x < xDim) && (y < yDim)) {

                                        if (((i + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                    progressLength * 100));
                                        }

                                        buffer[x + (y * xDim)] = byteBuffer[j] & 0xff;
                                        if (fileInfo.getPhotometric() == 0) {
                                            // white is 0 in original readin
                                            buffer[x + (y * xDim)] = 255 - buffer[x + (y * xDim)];
                                        }
                                        i++;
                                    } // if ((x < xDim) && (y < yDim))

                                    x++;

                                    if (x == ((xTile + 1) * tileWidth)) {
                                        x = xTile * tileWidth;
                                        y++;
                                    }
                                } // for (j = 0; j < nBytes; j++)
                            }

                            xTile++;

                            if (xTile == tilesAcross) {
                                xTile = 0;
                                yTile++;
                            }

                            x = xTile * tileWidth;
                            y = yTile * tileLength;
                        } // if (packBit == false)
                        else if (packBit == true) {
                            byteBuffer = new byte[nBytes];
                            raFile.read(byteBuffer, 0, nBytes);
                            progress = slice * xDim * yDim;
                            progressLength = imageSlice * xDim * yDim;
                            mod = progressLength / 100;

                            j = 0;

                            while (j < nBytes) {

                                // uncompressed data bytes follow
                                // Copy the next n+1 bytes literally
                                if ((byteBuffer[j] & 0x80) == 0) {
                                    iCount = byteBuffer[j] + 1;
                                    j++;

                                    for (iNext = 0; iNext < iCount; iNext++, j++) {

                                        if ((x < xDim) && (y < yDim)) {

                                            if (((i + progress) % mod) == 0) {
                                                fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                        progressLength * 100));
                                            }

                                            buffer[x + (y * xDim)] = byteBuffer[j] & 0xff;
                                            i++;
                                        } // if ((x < xDim) && (y < yDim))

                                        x++;

                                        if (x == ((xTile + 1) * tileWidth)) {
                                            x = xTile * tileWidth;
                                            y++;
                                        } // if (x == (xTile+1)*tileWidth)
                                    } // for (iNext = 0; iNext < iCount; iNext++,j++)
                                } // if (byteBuffer[j] & 0x80 == 0)

                                // Do nothing if the byte value is -128
                                else if (byteBuffer[j] == -128) {
                                    j++;
                                }
                                // compressed data bytes follow
                                // (~byteBuffer[j]) + 1 is the 2's complement of n or -n
                                // Hence (~byteBuffer[j]) + 2 equals -n + 1
                                else {
                                    iCount = (~byteBuffer[j]) + 2;
                                    j++;

                                    for (iNext = 0; iNext < iCount; iNext++, i++) {

                                        if ((x < xDim) && (y < yDim)) {

                                            if (((i + progress) % mod) == 0) {
                                                fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                        progressLength * 100));
                                            }

                                            buffer[x + (y * xDim)] = byteBuffer[j] & 0xff;
                                        } // if ((x < xDim) && (y < yDim))

                                        x++;

                                        if (x == ((xTile + 1) * tileWidth)) {
                                            x = xTile * tileWidth;
                                            y++;
                                        } // if (x == (xTile+1)*tileWidth)
                                    } // for (iNext = 0; iNext < iCount; iNext++,i++)

                                    j++;
                                } // else for compressed data bytes
                            } // while (j < nBytes)

                            xTile++;

                            if (xTile == tilesAcross) {
                                xTile = 0;
                                yTile++;
                            }

                            x = xTile * tileWidth;
                            y = yTile * tileLength;
                        } // else if (packBit == true)

                        break;

                    case ModelStorageBase.SHORT:
                        if (byteBuffer == null) {

                            if (lzwCompression || zlibCompression || SGILogCompression) {
                                byteBuffer = new byte[tileMaxByteCount];
                            } else {
                                byteBuffer = new byte[nBytes];
                            }
                        }

                        raFile.read(byteBuffer, 0, nBytes);
                        if (zlibCompression) {
                            zlibDecompresser.setInput(byteBuffer, 0, nBytes);
                        }
                        progress = slice * xDim * yDim;
                        progressLength = imageSlice * xDim * yDim;
                        mod = progressLength / 100;

                        if (lzwCompression || zlibCompression || SGILogCompression) {

                            if (decomp == null) {
                                decomp = new byte[tileWidth * tileLength * 3];
                            }

                            if (lzwCompression) {
                                //lzwDecoder.decode(byteBuffer, decomp, tileLength);
                                rowsToDo = Math.min(tileLength, yDim - y);
                                LZWDecompresser(byteBuffer, nBytes, decomp, y, rowsToDo, 2* tileWidth);
                                resultLength = decomp.length;
                            }
                            else if (zlibCompression){ 
                                try {
                                    resultLength = zlibDecompresser.inflate(decomp);
                                }
                                catch (DataFormatException e){
                                    MipavUtil.displayError("DataFormatException on zlibDecompresser.inflate(decomp)");  
                                }
                                zlibDecompresser.reset();
                            }
                            else { // SGILogCompression
                                rowsToDo = Math.min(tileLength, yDim - y);
                                resultLength = LogL16Decompresser(decomp, byteBuffer, nBytes, rowsToDo);    
                            }

                            for (j = 0; j < resultLength; j += 2) {

                                if ((x < xDim) && (y < yDim)) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength *
                                                                                100));
                                    }

                                    b1 = getUnsignedByte(decomp, j);
                                    b2 = getUnsignedByte(decomp, j + 1);

                                    if (endianess) {
                                        buffer[x + (y * xDim)] = (short) ((b1 << 8) + b2);
                                    } else {
                                        buffer[x + (y * xDim)] = (short) ((b2 << 8) + b1);
                                    }
                                }

                                x++;

                                if (x == ((xTile + 1) * tileWidth)) {
                                    x = xTile * tileWidth;
                                    y++;
                                }
                            }
                        } else {

                            for (j = 0; j < nBytes; j += 2) {

                                if ((x < xDim) && (y < yDim)) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength *
                                                                                100));
                                    }

                                    b1 = getUnsignedByte(byteBuffer, j);
                                    b2 = getUnsignedByte(byteBuffer, j + 1);

                                    if (endianess) {
                                        buffer[x + (y * xDim)] = (short) ((b1 << 8) + b2);
                                    } else {
                                        buffer[x + (y * xDim)] = (short) ((b2 << 8) + b1);
                                    }

                                    i++;
                                } // if ((x < xDim) && (y < yDim))

                                x++;

                                if (x == ((xTile + 1) * tileWidth)) {
                                    x = xTile * tileWidth;
                                    y++;
                                }
                            } // for (j = 0; j < nBytes; j+= 2)
                        }

                        xTile++;
                        if (xTile == tilesAcross) {
                            xTile = 0;
                            yTile++;
                        }

                        x = xTile * tileWidth;
                        y = yTile * tileLength;
                        
                        break;

                    case ModelStorageBase.USHORT:
                        if (byteBuffer == null) {

                            if (lzwCompression || zlibCompression) {
                                byteBuffer = new byte[tileMaxByteCount];
                            } else {
                                byteBuffer = new byte[nBytes];
                            }
                        }

                        raFile.read(byteBuffer, 0, nBytes);
                        if (zlibCompression) {
                            zlibDecompresser.setInput(byteBuffer, 0, nBytes);
                        }
                        progress = slice * xDim * yDim;
                        progressLength = imageSlice * xDim * yDim;
                        mod = progressLength / 100;

                        if (lzwCompression || zlibCompression) {

                            if (decomp == null) {
                                decomp = new byte[tileWidth * tileLength * 3];
                            }

                            if (lzwCompression) {
                                //lzwDecoder.decode(byteBuffer, decomp, tileLength);
                                rowsToDo = Math.min(tileLength, yDim - y);
                                LZWDecompresser(byteBuffer, nBytes, decomp, y, rowsToDo, 2* tileWidth);
                                resultLength = decomp.length;
                            }
                            else { // zlibCompression
                                try {
                                    resultLength = zlibDecompresser.inflate(decomp);
                                }
                                catch (DataFormatException e){
                                    MipavUtil.displayError("DataFormatException on zlibDecompresser.inflate(decomp)");  
                                }
                                zlibDecompresser.reset();
                            }

                            for (j = 0; j < resultLength; j += 2) {

                                if ((x < xDim) && (y < yDim)) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength *
                                                                                100));
                                    }

                                    b1 = getUnsignedByte(decomp, j);
                                    b2 = getUnsignedByte(decomp, j + 1);

                                    if (endianess) {
                                        buffer[x + (y * xDim)] = ((b1 << 8) + b2);
                                    } else {
                                        buffer[x + (y * xDim)] = ((b2 << 8) + b1);
                                    }
                                }

                                x++;

                                if (x == ((xTile + 1) * tileWidth)) {
                                    x = xTile * tileWidth;
                                    y++;
                                }
                            }
                        } else {

                            for (j = 0; j < nBytes; j += 2) {

                                if ((x < xDim) && (y < yDim)) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength *
                                                                                100));
                                    }

                                    b1 = getUnsignedByte(byteBuffer, j);
                                    b2 = getUnsignedByte(byteBuffer, j + 1);

                                    if (endianess) {
                                        buffer[x + (y * xDim)] = ((b1 << 8) + b2);
                                    } else {
                                        buffer[x + (y * xDim)] = ((b2 << 8) + b1);
                                    }

                                    i++;
                                } // if ((x < xDim) && (y < yDim))

                                x++;

                                if (x == ((xTile + 1) * tileWidth)) {
                                    x = xTile * tileWidth;
                                    y++;
                                }
                            } // for (j = 0; j < nBytes; j+= 2)
                        }

                        xTile++;
                        if (xTile == tilesAcross) {
                            xTile = 0;
                            yTile++;
                        }

                        x = xTile * tileWidth;
                        y = yTile * tileLength;
                        break;

                    case ModelStorageBase.INTEGER:
                        if (byteBuffer == null) {

                            if (lzwCompression || zlibCompression) {
                                byteBuffer = new byte[tileMaxByteCount];
                            } else {
                                byteBuffer = new byte[nBytes];
                            }
                        }

                        raFile.read(byteBuffer, 0, nBytes);
                        if (zlibCompression) {
                            zlibDecompresser.setInput(byteBuffer, 0, nBytes);
                        }
                        progress = slice * xDim * yDim;
                        progressLength = imageSlice * xDim * yDim;
                        mod = progressLength / 100;

                        if (lzwCompression || zlibCompression) {

                            if (decomp == null) {
                                decomp = new byte[tileWidth * tileLength * 3];
                            }

                            if (lzwCompression) {
                                //lzwDecoder.decode(byteBuffer, decomp, tileLength);
                                rowsToDo = Math.min(tileLength, yDim - y);
                                LZWDecompresser(byteBuffer, nBytes, decomp, y, rowsToDo, 4* tileWidth);
                                resultLength = decomp.length;
                            }
                            else { // zlibCompression
                                try {
                                    resultLength = zlibDecompresser.inflate(decomp);
                                }
                                catch (DataFormatException e){
                                    MipavUtil.displayError("DataFormatException on zlibDecompresser.inflate(decomp)");  
                                }
                                zlibDecompresser.reset();
                            }

                            for (j = 0; j < resultLength; j += 4) {

                                if ((x < xDim) && (y < yDim)) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength *
                                                                                100));
                                    }

                                    b1 = getUnsignedByte(decomp, j);
                                    b2 = getUnsignedByte(decomp, j + 1);
                                    b3 = getUnsignedByte(decomp, j + 2);
                                    b4 = getUnsignedByte(decomp, j + 3);

                                    if (endianess) {
                                        buffer[x + (y * xDim)] = ((b1 << 24) | (b2 << 16) | (b3 << 8) | b4); // Big
                                                                                                             // Endian
                                    } else {
                                        buffer[x + (y * xDim)] = ((b4 << 24) | (b3 << 16) | (b2 << 8) | b1); // Little
                                                                                                             // Endian
                                    }

                                    i++;
                                } // if ((x < xDim) && (y < yDim))

                                x++;

                                if (x == ((xTile + 1) * tileWidth)) {
                                    x = xTile * tileWidth;
                                    y++;
                                }
                            } // for (j = 0; j < decomp.length; j+= 4)
                        } else {

                            for (j = 0; j < nBytes; j += 4) {

                                if ((x < xDim) && (y < yDim)) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength *
                                                                                100));
                                    }

                                    b1 = getUnsignedByte(byteBuffer, j);
                                    b2 = getUnsignedByte(byteBuffer, j + 1);
                                    b3 = getUnsignedByte(byteBuffer, j + 2);
                                    b4 = getUnsignedByte(byteBuffer, j + 3);

                                    if (endianess) {
                                        buffer[x + (y * xDim)] = ((b1 << 24) | (b2 << 16) | (b3 << 8) | b4); // Big
                                                                                                             // Endian
                                    } else {
                                        buffer[x + (y * xDim)] = ((b4 << 24) | (b3 << 16) | (b2 << 8) | b1); // Little
                                                                                                             // Endian
                                    }

                                    i++;
                                } // if ((x < xDim) && (y < yDim))

                                x++;

                                if (x == ((xTile + 1) * tileWidth)) {
                                    x = xTile * tileWidth;
                                    y++;
                                }
                            } // for (j = 0; j < nBytes; j+= 4)
                        }

                        xTile++;
                        if (xTile == tilesAcross) {
                            xTile = 0;
                            yTile++;
                        }

                        x = xTile * tileWidth;
                        y = yTile * tileLength;
                        break;

                    case ModelStorageBase.UINTEGER:
                        if (byteBuffer == null) {

                            if (lzwCompression || zlibCompression) {
                                byteBuffer = new byte[tileMaxByteCount];
                            } else {
                                byteBuffer = new byte[nBytes];
                            }
                        }

                        raFile.read(byteBuffer, 0, nBytes);
                        if (zlibCompression) {
                            zlibDecompresser.setInput(byteBuffer, 0, nBytes);
                        }
                        progress = slice * xDim * yDim;
                        progressLength = imageSlice * xDim * yDim;
                        mod = progressLength / 100;

                        if (lzwCompression || zlibCompression) {

                            if (decomp == null) {
                                decomp = new byte[tileWidth * tileLength * 3];
                            }

                            if (lzwCompression) {
                                //lzwDecoder.decode(byteBuffer, decomp, tileLength);
                                rowsToDo = Math.min(tileLength, yDim - y);
                                LZWDecompresser(byteBuffer, nBytes, decomp, y, rowsToDo, 4* tileWidth);
                                resultLength = decomp.length;
                            }
                            else { // zlibCompression
                                try {
                                    resultLength = zlibDecompresser.inflate(decomp);
                                }
                                catch (DataFormatException e){
                                    MipavUtil.displayError("DataFormatException on zlibDecompresser.inflate(decomp)");  
                                }
                                zlibDecompresser.reset();
                            }

                            for (j = 0; j < resultLength; j += 4) {

                                if ((x < xDim) && (y < yDim)) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength *
                                                                                100));
                                    }

                                    b1 = getUnsignedByte(decomp, j);
                                    b2 = getUnsignedByte(decomp, j + 1);
                                    b3 = getUnsignedByte(decomp, j + 2);
                                    b4 = getUnsignedByte(decomp, j + 3);

                                    if (endianess) {
                                        buffer[x + (y * xDim)] = (((long) b1 << 24) | ((long) b2 << 16) |
                                                                      ((long) b3 << 8) | (long) b4); // Big Endian
                                    } else {
                                        buffer[x + (y * xDim)] = (((long) b4 << 24) | ((long) b3 << 16) |
                                                                      ((long) b2 << 8) | (long) b1); // Little Endian
                                    }

                                    i++;
                                } // if ((x < xDim) && (y < yDim))

                                x++;

                                if (x == ((xTile + 1) * tileWidth)) {
                                    x = xTile * tileWidth;
                                    y++;
                                }
                            } // for (j = 0; j < decomp.length; j+= 4)
                        } else {

                            for (j = 0; j < nBytes; j += 4) {

                                if ((x < xDim) && (y < yDim)) {

                                    if (((i + progress) % mod) == 0) {
                                        fireProgressStateChanged(Math.round((float) (i + progress) / progressLength *
                                                                                100));
                                    }

                                    b1 = getUnsignedByte(byteBuffer, j);
                                    b2 = getUnsignedByte(byteBuffer, j + 1);
                                    b3 = getUnsignedByte(byteBuffer, j + 2);
                                    b4 = getUnsignedByte(byteBuffer, j + 3);

                                    if (endianess) {
                                        buffer[x + (y * xDim)] = (((long) b1 << 24) | ((long) b2 << 16) |
                                                                      ((long) b3 << 8) | (long) b4); // Big Endian
                                    } else {
                                        buffer[x + (y * xDim)] = (((long) b4 << 24) | ((long) b3 << 16) |
                                                                      ((long) b2 << 8) | (long) b1); // Little Endian
                                    }

                                    i++;
                                } // if ((x < xDim) && (y < yDim))

                                x++;

                                if (x == ((xTile + 1) * tileWidth)) {
                                    x = xTile * tileWidth;
                                    y++;
                                }
                            } // for (j = 0; j < nBytes; j+= 4)
                        }

                        xTile++;
                        if (xTile == tilesAcross) {
                            xTile = 0;
                            yTile++;
                        }

                        x = xTile * tileWidth;
                        y = yTile * tileLength;
                        break;

                    case ModelStorageBase.ARGB:
                        if (isYCbCr && (!jpegCompression)) {
                            if (chunky == true) {
                                
                                if (byteBuffer == null) {
    
                                    if (lzwCompression || zlibCompression) {
                                        byteBuffer = new byte[tileMaxByteCount];
                                    } else {
                                        byteBuffer = new byte[nBytes];
                                    }
                                }
    
                                // System.err.println("About to read " + nBytes + " bytes");
                                raFile.read(byteBuffer, 0, nBytes);
                                if (zlibCompression) {
                                    zlibDecompresser.setInput(byteBuffer, 0, nBytes);
                                }
    
                                // System.err.println("________");
                                progress = slice * xDim * yDim;
                                progressLength = imageSlice * xDim * yDim;
                                mod = progressLength / 100;
    
    
                                if (lzwCompression || zlibCompression) {
    
                                    //System.err.println("Read " + nBytes + " from raFile");
                                    if (decomp == null) {
                                        decomp = new byte[tileWidth * tileLength * samplesPerPixel];
                                    }
    
                                    if (lzwCompression) {
                                        //lzwDecoder.decode(byteBuffer, decomp, tileLength);
                                        rowsToDo = Math.min(tileLength, yDim - y);
                                        LZWDecompresser(byteBuffer, nBytes, decomp, y, rowsToDo,
                                                        tileWidth + (2 * tileWidth)/(YCbCrSubsampleHoriz * YCbCrSubsampleVert));
                                        resultLength = decomp.length;
                                    }
                                    else { // zlibCompression
                                        try {
                                            resultLength = zlibDecompresser.inflate(decomp);
                                        }
                                        catch (DataFormatException e){
                                            MipavUtil.displayError("DataFormatException on zlibDecompresser.inflate(decomp)");  
                                        }
                                        zlibDecompresser.reset();
                                    }
                                }
                                else {
                                    decomp = byteBuffer;
                                }
                                    
                                //System.err.println("Decoded byte length: " + decomp.length);
                                if ((YCbCrSubsampleHoriz == 2) && (YCbCrSubsampleVert == 2)) {
                                    // This is the default subsampling
                                    for (j = 0, h = 0; h < tileLength; h += 2) {
                                        for (w = 0; w < tileWidth; w += 2, j += 6) {
                                                Y00 = getUnsignedByte(decomp, j);
                                                Y01 = getUnsignedByte(decomp, j+1);
                                                Y10 = getUnsignedByte(decomp, j+2);
                                                Y11 = getUnsignedByte(decomp, j+3);
                                                Cb00 = getUnsignedByte(decomp, j+4);
                                                Cr00 = getUnsignedByte(decomp, j+5);
                                                if ((x < xDim) && (y < yDim)) {
                                                    YBuffer[y*xDim + x] = Y00;
                                                    CbInBuffer[y*halfXDim/2 + x/2] = Cb00;
                                                    CrInBuffer[y*halfXDim/2 + x/2] = Cr00;
                                                }
                                                if (((x+1) < xDim) && (y < yDim)) {
                                                    YBuffer[y*xDim + x + 1] = Y01;
                                                }
                                                if ((x < xDim) && ((y+1) < yDim)) {
                                                    YBuffer[(y+1)*xDim + x] = Y10;
                                                }
                                                if (((x+1) < xDim) && ((y+1) < yDim)) {
                                                    YBuffer[(y+1)*xDim + x + 1] = Y11;
                                                }
                                            
                                            x += 2;
                                            if (x >= ((xTile + 1) * tileWidth)) {
                                                x = xTile * tileWidth;
                                                y += 2;
                                            }
                                        }
                                    }
                                } // if ((YCbCrSubsampleHoriz == 2) && (YCbCrSubsampleVert == 2))
                                else if ((YCbCrSubsampleHoriz == 2) && (YCbCrSubsampleVert == 1)) {
                                    for (j = 0, h = 0; h < tileLength; h ++) {
                                        for (w = 0; w < tileWidth; w += 2, j += 4) {
                                                Y00 = getUnsignedByte(decomp, j);
                                                Y01 = getUnsignedByte(decomp, j+1);
                                                Cb00 = getUnsignedByte(decomp, j+2);
                                                Cr00 = getUnsignedByte(decomp, j+3);
                                                if ((x < xDim) && (y < yDim)) {
                                                    YBuffer[y*xDim + x] = Y00;
                                                    CbInBuffer[y*halfXDim + x/2] = Cb00;
                                                    CrInBuffer[y*halfXDim + x/2] = Cr00;
                                                }
                                                if (((x+1) < xDim) && (y < yDim)) {
                                                    YBuffer[y*xDim + x + 1] = Y01;
                                                }
                                            
                                            x += 2;
                                            if (x >= ((xTile + 1) * tileWidth)) {
                                                x = xTile * tileWidth;
                                                y++;
                                            }
                                        }
                                    }    
                                } // else if ((YCbCrSubsampleHoriz == 2) && (YCbCrSubsampleVert == 1))
                                else if ((YCbCrSubsampleHoriz == 1) && (YCbCrSubsampleVert == 2)) {
                                    for (j = 0, h = 0; h < tileLength; h += 2) {
                                        for (w = 0; w < tileWidth; w++, j += 4) {
                                                Y00 = getUnsignedByte(decomp, j);
                                                Y10 = getUnsignedByte(decomp, j+1);
                                                Cb00 = getUnsignedByte(decomp, j+2);
                                                Cr00 = getUnsignedByte(decomp, j+3);
                                                if ((x < xDim) && (y < yDim)) {
                                                    YBuffer[y*xDim + x] = Y00;
                                                    CbInBuffer[y*xDim/2 + x] = Cb00;
                                                    CrInBuffer[y*xDim/2 + x] = Cr00;
                                                }
                                                if ((x < xDim) && ((y+1) < yDim)) {
                                                    YBuffer[(y+1)*xDim + x] = Y10;
                                                }
                                            
                                            x++;
                                            if (x == ((xTile + 1) * tileWidth)) {
                                                x = xTile * tileWidth;
                                                y += 2;
                                            }
                                        }
                                    }    
                                } // else if ((YCbCrSubsampleHoriz == 1) && (YCbCrSubsampleVert == 2))
                                else if ((YCbCrSubsampleHoriz == 1) && (YCbCrSubsampleVert == 1)) {
                                    for (j = 0, h = 0; h < tileLength; h ++) {
                                        for (w = 0; w < tileWidth; w ++, j += 3) {
                                            if ((x < xDim) && (y < yDim)) {
                                                Y00 = getUnsignedByte(decomp, j);
                                                Cb00 = getUnsignedByte(decomp, j+1);
                                                Cr00 = getUnsignedByte(decomp, j+2);
                                                YBuffer[y*xDim + x] = Y00;
                                                CbInBuffer[y*xDim + x] = Cb00;
                                                CrInBuffer[y*xDim + x] = Cr00;
                                            }
                                            
                                            x++;
                                            if (x == ((xTile + 1) * tileWidth)) {
                                                x = xTile * tileWidth;
                                                y++;
                                            }
                                        }
                                    }        
                                } // else if ((YCbCrSubsampleHoriz == 1) && (YCbCrSubsampleVert == 1))
                                xTile++;
                                
                                if (xTile == tilesAcross) {
                                    xTile = 0;
                                    yTile++;
                                }
    
                                x = xTile * tileWidth;
                                y = yTile * tileLength;
                            } // if (chunky == true)    
                        } // if (isYCbCr && (!jpegCompression))
                        else { //  notYCbCr || jpegCompression
                            if (chunky == true) {
    
                                if (byteBuffer == null) {
    
                                    if (lzwCompression || zlibCompression || jpegCompression || SGILogCompression ||
                                        SGILog24Compression) {
                                        byteBuffer = new byte[tileMaxByteCount];
                                    } else {
                                        byteBuffer = new byte[nBytes];
                                    }
                                }
    
                                // System.err.println("About to read " + nBytes + " bytes");
                                raFile.read(byteBuffer, 0, nBytes);
                                if (zlibCompression) {
                                    zlibDecompresser.setInput(byteBuffer, 0, nBytes);
                                }
    
                                // System.err.println("________");
                                progress = slice * xDim * yDim;
                                progressLength = imageSlice * xDim * yDim;
                                mod = progressLength / 100;
    
    
                                if (lzwCompression || zlibCompression || jpegCompression || SGILogCompression ||
                                    SGILog24Compression) {
    
                                    //System.err.println("Read " + nBytes + " from raFile");
                                    if (decomp == null) {
                                        decomp = new byte[tileWidth * tileLength * samplesPerPixel];
                                    }
    
                                    if (lzwCompression) {
                                        //lzwDecoder.decode(byteBuffer, decomp, tileLength);
                                        rowsToDo = Math.min(tileLength, yDim - y);
                                        LZWDecompresser(byteBuffer, nBytes, decomp, y, rowsToDo, tileWidth * samplesPerPixel);
                                        resultLength = decomp.length;
                                    }
                                    else if (jpegCompression) {
                                        data = new byte[nBytes];
                                        for (j = 0; j < nBytes; j++) {
                                            data[j] = byteBuffer[j];
                                        }
                                        resultLength = jpegDecompresser(decomp, data);
                                    }
                                    else if (zlibCompression) {
                                        try {
                                            resultLength = zlibDecompresser.inflate(decomp);
                                        }
                                        catch (DataFormatException e){
                                            MipavUtil.displayError("DataFormatException on zlibDecompresser.inflate(decomp)");  
                                        }
                                        zlibDecompresser.reset();
                                    }
                                    else if (SGILogCompression) {
                                        rowsToDo = Math.min(tileLength, yDim - y);
                                        resultLength = LogLuv32Decompresser(decomp, byteBuffer, nBytes, rowsToDo);       
                                    }
                                    else { // SGILog24Compression
                                        rowsToDo = Math.min(tileLength, yDim - y);
                                        resultLength = LogLuv24Decompresser(decomp, byteBuffer, nBytes, rowsToDo);      
                                    }
    
                                    //System.err.println("Decoded byte length: " + resultLength);
                                    if (samplesPerPixel == 3) {
    
                                        for (j = 0; j < resultLength; j += 3) {
    
                                            if ((x < xDim) && (y < yDim)) {
    
                                                if (((i + progress) % mod) == 0) {
                                                    fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                            progressLength * 100));
                                                }
    
                                                buffer[4 * (x + (y * xDim))] = 255;
                                                buffer[(4 * (x + (y * xDim))) + 1] = getUnsignedByte(decomp, j);
                                                buffer[(4 * (x + (y * xDim))) + 2] = getUnsignedByte(decomp, j + 1);
                                                buffer[(4 * (x + (y * xDim))) + 3] = getUnsignedByte(decomp, j + 2);
                                                i++;
                                            }
    
                                            x++;
    
                                            if (x == ((xTile + 1) * tileWidth)) {
                                                x = xTile * tileWidth;
                                                y++;
                                            }
                                        }
                                    } // if (samplesPerPixel == 3)
                                    else if (samplesPerPixel == 4) {
    
                                        for (j = 0; j < resultLength; j += 4) {
    
                                            if ((x < xDim) && (y < yDim)) {
    
                                                if (((i + progress) % mod) == 0) {
                                                    fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                            progressLength * 100));
                                                }
    
                                                buffer[4 * (x + (y * xDim))] = getUnsignedByte(decomp, j + 3);
                                                buffer[(4 * (x + (y * xDim))) + 1] = getUnsignedByte(decomp, j);
                                                buffer[(4 * (x + (y * xDim))) + 2] = getUnsignedByte(decomp, j + 1);
                                                buffer[(4 * (x + (y * xDim))) + 3] = getUnsignedByte(decomp, j + 2);
                                                i++;
                                            }
    
                                            x++;
    
                                            if (x == ((xTile + 1) * tileWidth)) {
                                                x = xTile * tileWidth;
                                                y++;
                                            }
                                        }
                                    }
                                } else {
    
                                    for (j = 0; j < nBytes; j += 3) {
    
                                        if ((x < xDim) && (y < yDim)) {
    
                                            if (((i + progress) % mod) == 0) {
                                                fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                        progressLength * 100));
                                            }
    
                                            buffer[4 * (x + (y * xDim))] = 255;
                                            buffer[(4 * (x + (y * xDim))) + 1] = getUnsignedByte(byteBuffer, j);
                                            buffer[(4 * (x + (y * xDim))) + 2] = getUnsignedByte(byteBuffer, j + 1);
                                            buffer[(4 * (x + (y * xDim))) + 3] = getUnsignedByte(byteBuffer, j + 2);
                                            i++;
                                        } // if ((x < xDim) && (y < yDim))
    
                                        x++;
    
                                        if (x == ((xTile + 1) * tileWidth)) {
                                            x = xTile * tileWidth;
                                            y++;
                                        }
                                    } // for (j = 0; j < nBytes; j+= 3)
                                }
    
                                xTile++;
    
                                if (xTile == tilesAcross) {
                                    xTile = 0;
                                    yTile++;
                                }
    
                                x = xTile * tileWidth;
                                y = yTile * tileLength;
                            } // if (chunky == true)
                            else { // not chunky RRRRR, GGGGG, BBBBB
                                if (byteBuffer == null) {
                                    
                                    if (lzwCompression || zlibCompression) {
                                        byteBuffer = new byte[tileMaxByteCount];
                                    } else {
                                        byteBuffer = new byte[nBytes];
                                    }
                                }
    
                                // System.err.println("About to read " + nBytes + " bytes");
                                raFile.read(byteBuffer, 0, nBytes);
                                if (zlibCompression) {
                                    zlibDecompresser.setInput(byteBuffer, 0, nBytes);
                                }
    
                                // System.err.println("________");
                                progress = slice * xDim * yDim;
                                progressLength = imageSlice * xDim * yDim;
                                mod = progressLength / 100;
    
    
                                if (lzwCompression || zlibCompression) {
    
                                    //System.err.println("Read " + nBytes + " from raFile");
                                    
                                    if (decomp == null) {
                                        decomp = new byte[tileWidth * tileLength];
                                    }
    
                                    if (lzwCompression) {
                                        //lzwDecoder.decode(byteBuffer, decomp, tileLength);
                                        rowsToDo = Math.min(tileLength, yDim - y);
                                        LZWDecompresser(byteBuffer, nBytes, decomp, y, rowsToDo, tileWidth);
                                        resultLength = decomp.length;
                                    }
                                    else { // zlibCompression
                                        try {
                                            resultLength = zlibDecompresser.inflate(decomp);
                                        }
                                        catch (DataFormatException e){
                                            MipavUtil.displayError("DataFormatException on zlibDecompresser.inflate(decomp)");  
                                        }
                                        zlibDecompresser.reset();
                                    }
    
                                    //System.err.println("Decoded byte length: " + resultLength);
                                    if (planarRGB < stripsPerImage) {
    
                                        for (j = 0; j < resultLength; j++, i += 4) {
    
                                            if ((x < xDim) && (y < yDim)) {
    
                                                if (((i + progress) % mod) == 0) {
                                                    fireProgressStateChanged(Math.round((float) (i/3 + progress) /
                                                                                            progressLength * 100));
                                                }
    
                                                buffer[4 * (x + (y * xDim))] = 255;
                                                buffer[(4 * (x + (y * xDim))) + 1] = getUnsignedByte(decomp, j);
                                            }
    
                                            x++;
    
                                            if (x == ((xTile + 1) * tileWidth)) {
                                                x = xTile * tileWidth;
                                                y++;
                                            }
                                        } // for (j = 0; j < decomp.length; j++, i += 4)
                                    } // if (planarRGB < stripsPerImage)
                                    else if (planarRGB < (2 * stripsPerImage)) {
                                        for (j = 0; j < resultLength; j++, i += 4) {
                                            
                                            if ((x < xDim) && (y < yDim)) {
    
                                                if (((i + progress) % mod) == 0) {
                                                    fireProgressStateChanged(Math.round((float) (i/3 + buffer.length/3 + 
                                                                             progress) / progressLength * 100));
                                                }
    
                                                buffer[(4 * (x + (y * xDim))) + 2] = getUnsignedByte(decomp, j);
                                            }
    
                                            x++;
    
                                            if (x == ((xTile + 1) * tileWidth)) {
                                                x = xTile * tileWidth;
                                                y++;
                                            }
                                        } // for (j = 0; j < decomp.length; j++, i += 4)    
                                    } // else if (planarRGB < (2 * stripsPerImage))
                                    else { // planarRGB >= (2 * stripsPerImage)
                                        for (j = 0; j < resultLength; j++, i += 4) {
                                            
                                            if ((x < xDim) && (y < yDim)) {
    
                                                if (((i + progress) % mod) == 0) {
                                                    fireProgressStateChanged(Math.round((float) (i/3 + 
                                                                             2*buffer.length/3 + progress) /
                                                                             progressLength * 100));
                                                }
    
                                                buffer[(4 * (x + (y * xDim))) + 3] = getUnsignedByte(decomp, j);
                                            }
    
                                            x++;
    
                                            if (x == ((xTile + 1) * tileWidth)) {
                                                x = xTile * tileWidth;
                                                y++;
                                            }
                                        } // for (j = 0; j < decomp.length; j++, i += 4)    
                                    } // else planarRGB >= (2 * stripsPerImage)
                                } else { // not (lzwCompression ||| zlibCompression)
                                    if (planarRGB < stripsPerImage) {
    
                                        for (j = 0; j < nBytes; j++, i += 4) {
        
                                            if ((x < xDim) && (y < yDim)) {
        
                                                if (((i + progress) % mod) == 0) {
                                                    fireProgressStateChanged(Math.round((float) (i/3 + progress) /
                                                                                            progressLength * 100));
                                                }
        
                                                buffer[4 * (x + (y * xDim))] = 255;
                                                buffer[(4 * (x + (y * xDim))) + 1] = getUnsignedByte(byteBuffer, j);
                                            } // if ((x < xDim) && (y < yDim))
        
                                            x++;
        
                                            if (x == ((xTile + 1) * tileWidth)) {
                                                x = xTile * tileWidth;
                                                y++;
                                            }
                                        } // for (j = 0; j < nBytes; j++, i += 4)
                                    } // if (planarRGB < stripsPerImage)
                                    else if (planarRGB < (2 * stripsPerImage)) {
                                        for (j = 0; j < nBytes; j++, i += 4) {
                                            
                                            if ((x < xDim) && (y < yDim)) {
        
                                                if (((i + progress) % mod) == 0) {
                                                    fireProgressStateChanged(Math.round((float) (i/3 + 
                                                                             buffer.length/3 + progress) /
                                                                                            progressLength * 100));
                                                }
        
                                                buffer[(4 * (x + (y * xDim))) + 2] = getUnsignedByte(byteBuffer, j);
                                            } // if ((x < xDim) && (y < yDim))
        
                                            x++;
        
                                            if (x == ((xTile + 1) * tileWidth)) {
                                                x = xTile * tileWidth;
                                                y++;
                                            }
                                        } // for (j = 0; j < nBytes; j++, i += 4)    
                                    } // else if (planarRGB < (2 * stripsPerImage))
                                    else { // planarRGB >= (2 * stripsPerImage)
                                        for (j = 0; j < nBytes; j++, i += 4) {
                                            
                                            if ((x < xDim) && (y < yDim)) {
        
                                                if (((i + progress) % mod) == 0) {
                                                    fireProgressStateChanged(Math.round((float) (i/3 + 
                                                                             2*buffer.length/3 + progress) /
                                                                                            progressLength * 100));
                                                }
        
                                                buffer[(4 * (x + (y * xDim))) + 3] = getUnsignedByte(byteBuffer, j);
                                            } // if ((x < xDim) && (y < yDim))
        
                                            x++;
        
                                            if (x == ((xTile + 1) * tileWidth)) {
                                                x = xTile * tileWidth;
                                                y++;
                                            }
                                        } // for (j = 0; j < nBytes; j++, i += 4)     
                                    } // else planarRGB >= (2 * stripsPerImage)
                                } // else not lzwCompression
    
                                xTile++;
    
                                if (xTile == tilesAcross) {
                                    xTile = 0;
                                    yTile++;
                                }
    
                                x = xTile * tileWidth;
                                y = yTile * tileLength;  
                                planarRGB++;
                                if ((planarRGB == stripsPerImage) || (planarRGB == 2*stripsPerImage)) {
                                    i = 0;
                                    x = 0;
                                    y = 0;
                                    xTile = 0;
                                    yTile = 0;
                                }
                            } // not chunky
                        } // else not YCbCr

                        break;
                        
                    case ModelStorageBase.ARGB_USHORT:
                        if (chunky == true) {

                            if (byteBuffer == null) {

                                if (lzwCompression || zlibCompression) {
                                    byteBuffer = new byte[tileMaxByteCount];
                                } else {
                                    byteBuffer = new byte[nBytes];
                                }
                            }

                            // System.err.println("About to read " + nBytes + " bytes");
                            raFile.read(byteBuffer, 0, nBytes);
                            if (zlibCompression) {
                                zlibDecompresser.setInput(byteBuffer, 0, nBytes);
                            }

                            // System.err.println("________");
                            progress = slice * xDim * yDim;
                            progressLength = imageSlice * xDim * yDim;
                            mod = progressLength / 100;


                            if (lzwCompression || zlibCompression) {

                                // System.err.println("Read " + nBytes + " from raFile");
                                
                                if (decomp == null) {
                                    decomp = new byte[tileWidth * tileLength * samplesPerPixel * 2];
                                }

                                if (lzwCompression) {
                                    //lzwDecoder.decode(byteBuffer, decomp, tileLength);
                                    rowsToDo = Math.min(tileLength, yDim - y);
                                    LZWDecompresser(byteBuffer, nBytes, decomp, y, rowsToDo, 2* tileWidth * samplesPerPixel);
                                    resultLength = decomp.length;
                                }
                                else { // zlibCompression
                                    try {
                                        resultLength = zlibDecompresser.inflate(decomp);
                                    }
                                    catch (DataFormatException e){
                                        MipavUtil.displayError("DataFormatException on zlibDecompresser.inflate(decomp)");  
                                    }
                                    zlibDecompresser.reset();
                                }

                                // System.err.println("Decoded byte length: " + resultLength);
                                if (samplesPerPixel == 3) {

                                    for (j = 0; j < resultLength; j += 6) {

                                        if ((x < xDim) && (y < yDim)) {

                                            if (((i + progress) % mod) == 0) {
                                                fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                        progressLength * 100));
                                            }
                                            buffer[4 * (x + (y * xDim))] = 65535;
                                            b1 = getUnsignedByte(decomp, j);
                                            b2 = getUnsignedByte(decomp, j+1);
                                            b3 = getUnsignedByte(decomp, j+2);
                                            b4 = getUnsignedByte(decomp, j+3);
                                            b5 = getUnsignedByte(decomp, j+4);
                                            b6 = getUnsignedByte(decomp, j+5);
                                            if (endianess) {
                                                buffer[(4 * (x + (y * xDim))) + 1] = ((b1 << 8) + b2);
                                                buffer[(4 * (x + (y * xDim))) + 2] = ((b3 << 8) + b4);
                                                buffer[(4 * (x + (y * xDim))) + 3] = ((b5 << 8) + b6);
                                            }
                                            else {
                                                buffer[(4 * (x + (y * xDim))) + 1] = ((b2 << 8) + b1);
                                                buffer[(4 * (x + (y * xDim))) + 2] = ((b4 << 8) + b3);
                                                buffer[(4 * (x + (y * xDim))) + 3] = ((b6 << 8) + b5);    
                                            }
                                            i++;
                                        }

                                        x++;

                                        if (x == ((xTile + 1) * tileWidth)) {
                                            x = xTile * tileWidth;
                                            y++;
                                        }
                                    }
                                } // if (samplesPerPixel == 3)
                                else if (samplesPerPixel == 4) {

                                    for (j = 0; j < resultLength; j += 8) {

                                        if ((x < xDim) && (y < yDim)) {

                                            if (((i + progress) % mod) == 0) {
                                                fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                        progressLength * 100));
                                            }
                                            b1 = getUnsignedByte(decomp, j);
                                            b2 = getUnsignedByte(decomp, j+1);
                                            b3 = getUnsignedByte(decomp, j+2);
                                            b4 = getUnsignedByte(decomp, j+3);
                                            b5 = getUnsignedByte(decomp, j+4);
                                            b6 = getUnsignedByte(decomp, j+5);
                                            b7 = getUnsignedByte(decomp, j+6);
                                            b8 = getUnsignedByte(decomp, j+7);
                                            if (endianess) {
                                                buffer[(4 * (x + (y * xDim)))] = ((b7 << 8) + b8);
                                                buffer[(4 * (x + (y * xDim))) + 1] = ((b1 << 8) + b2);
                                                buffer[(4 * (x + (y * xDim))) + 2] = ((b3 << 8) + b4);
                                                buffer[(4 * (x + (y * xDim))) + 3] = ((b5 << 8) + b6);
                                            }
                                            else {
                                                buffer[(4 * (x + (y * xDim)))] = ((b8 << 8) + b7);
                                                buffer[(4 * (x + (y * xDim))) + 1] = ((b2 << 8) + b1);
                                                buffer[(4 * (x + (y * xDim))) + 2] = ((b4 << 8) + b3);
                                                buffer[(4 * (x + (y * xDim))) + 3] = ((b6 << 8) + b5);    
                                            }
                                            i++;
                                        }

                                        x++;

                                        if (x == ((xTile + 1) * tileWidth)) {
                                            x = xTile * tileWidth;
                                            y++;
                                        }
                                    }
                                }
                            } else {

                                for (j = 0; j < nBytes; j += 6) {

                                    if ((x < xDim) && (y < yDim)) {

                                        if (((i + progress) % mod) == 0) {
                                            fireProgressStateChanged(Math.round((float) (i + progress) /
                                                                                    progressLength * 100));
                                        }
                                        buffer[4 * (x + (y * xDim))] = 65535;
                                        b1 = getUnsignedByte(decomp, j);
                                        b2 = getUnsignedByte(decomp, j+1);
                                        b3 = getUnsignedByte(decomp, j+2);
                                        b4 = getUnsignedByte(decomp, j+3);
                                        b5 = getUnsignedByte(decomp, j+4);
                                        b6 = getUnsignedByte(decomp, j+5);
                                        if (endianess) {
                                            buffer[(4 * (x + (y * xDim))) + 1] = ((b1 << 8) + b2);
                                            buffer[(4 * (x + (y * xDim))) + 2] = ((b3 << 8) + b4);
                                            buffer[(4 * (x + (y * xDim))) + 3] = ((b5 << 8) + b6);
                                        }
                                        else {
                                            buffer[(4 * (x + (y * xDim))) + 1] = ((b2 << 8) + b1);
                                            buffer[(4 * (x + (y * xDim))) + 2] = ((b4 << 8) + b3);
                                            buffer[(4 * (x + (y * xDim))) + 3] = ((b6 << 8) + b5);    
                                        }
                                        i++;
                                    } // if ((x < xDim) && (y < yDim))

                                    x++;

                                    if (x == ((xTile + 1) * tileWidth)) {
                                        x = xTile * tileWidth;
                                        y++;
                                    }
                                } // for (j = 0; j < nBytes; j+= 6)
                            }

                            xTile++;

                            if (xTile == tilesAcross) {
                                xTile = 0;
                                yTile++;
                            }

                            x = xTile * tileWidth;
                            y = yTile * tileLength;
                        } // if (chunky == true)

                        break;
                    case ModelStorageBase.ARGB_FLOAT:
                        if (chunky == false && bitsPerSample[0] == 64) {
                            if (byteBuffer == null) {
                                
                                if (lzwCompression || zlibCompression) {
                                    byteBuffer = new byte[tileMaxByteCount];
                                } else {
                                    byteBuffer = new byte[nBytes];
                                }
                            }

                            // System.err.println("About to read " + nBytes + " bytes");
                            raFile.read(byteBuffer, 0, nBytes);
                            if (zlibCompression) {
                                zlibDecompresser.setInput(byteBuffer, 0, nBytes);
                            }

                            // System.err.println("________");
                            progress = slice * xDim * yDim;
                            progressLength = imageSlice * xDim * yDim;
                            mod = progressLength / 100;


                            if (lzwCompression || zlibCompression) {

                                //System.err.println("Read " + nBytes + " from raFile");
                                
                                if (decomp == null) {
                                    decomp = new byte[8 *tileWidth * tileLength];
                                }

                                if (lzwCompression) {
                                    //lzwDecoder.decode(byteBuffer, decomp, tileLength);
                                    rowsToDo = Math.min(tileLength, yDim - y);
                                    LZWDecompresser(byteBuffer, nBytes, decomp, y, rowsToDo, tileWidth);
                                    resultLength = decomp.length;
                                }
                                else { // zlibCompression
                                    try {
                                        resultLength = zlibDecompresser.inflate(decomp);
                                    }
                                    catch (DataFormatException e){
                                        MipavUtil.displayError("DataFormatException on zlibDecompresser.inflate(decomp)");  
                                    }
                                    zlibDecompresser.reset();
                                }

                                //System.err.println("Decoded byte length: " + resultLength);
                                if (planarRGB < stripsPerImage) {

                                    for (j = 0; j < resultLength; j+= 8, i += 4) {

                                        if ((x < xDim) && (y < yDim)) {

                                            if (((i + progress) % mod) == 0) {
                                                fireProgressStateChanged(Math.round((float) (i/3 + progress) /
                                                                                        progressLength * 100));
                                            }

                                            buffer[4 * (x + (y * xDim))] = 255.0f;
                                            b1L = getUnsignedByte(decomp,j) & 0xff;
                                            b2L = getUnsignedByte(decomp,j+1) & 0xff;
                                            b3L = getUnsignedByte(decomp,j+2) & 0xff;
                                            b4L = getUnsignedByte(decomp,j+3) & 0xff;
                                            b5L = getUnsignedByte(decomp,j+4) & 0xff;
                                            b6L = getUnsignedByte(decomp,j+5) & 0xff;
                                            b7L = getUnsignedByte(decomp,j+6) & 0xff;
                                            b8L = getUnsignedByte(decomp,j+7) & 0xff;

                                            if (endianess) {
                                                tmpLong = ((b1L << 56) | (b2L << 48) | (b3L << 40) | (b4L << 32) | (b5L << 24) | (b6L << 16) |
                                                               (b7L << 8) | b8L); // Big Endian
                                            } else {
                                                tmpLong = ((b8L << 56) | (b7L << 48) | (b6L << 40) | (b5L << 32) | (b4L << 24) | (b3L << 16) |
                                                               (b2L << 8) | b1L);
                                            }

                                            tmpDouble = Double.longBitsToDouble(tmpLong);
                                            buffer[(4* (x + (y * xDim))) + 1] = (float)tmpDouble;
                                        }

                                        x++;

                                        if (x == ((xTile + 1) * tileWidth)) {
                                            x = xTile * tileWidth;
                                            y++;
                                        }
                                    } // for (j = 0; j < decomp.length; j+=  8, i += 4)
                                } // if (planarRGB < stripsPerImage)
                                else if (planarRGB < (2 * stripsPerImage)) {
                                    for (j = 0; j < resultLength; j+= 8, i += 4) {
                                        
                                        if ((x < xDim) && (y < yDim)) {

                                            if (((i + progress) % mod) == 0) {
                                                fireProgressStateChanged(Math.round((float) (i/3 + buffer.length/3 + 
                                                                         progress) / progressLength * 100));
                                            }

                                            b1L = getUnsignedByte(decomp,j) & 0xff;
                                            b2L = getUnsignedByte(decomp,j+1) & 0xff;
                                            b3L = getUnsignedByte(decomp,j+2) & 0xff;
                                            b4L = getUnsignedByte(decomp,j+3) & 0xff;
                                            b5L = getUnsignedByte(decomp,j+4) & 0xff;
                                            b6L = getUnsignedByte(decomp,j+5) & 0xff;
                                            b7L = getUnsignedByte(decomp,j+6) & 0xff;
                                            b8L = getUnsignedByte(decomp,j+7) & 0xff;

                                            if (endianess) {
                                                tmpLong = ((b1L << 56) | (b2L << 48) | (b3L << 40) | (b4L << 32) | (b5L << 24) | (b6L << 16) |
                                                               (b7L << 8) | b8L); // Big Endian
                                            } else {
                                                tmpLong = ((b8L << 56) | (b7L << 48) | (b6L << 40) | (b5L << 32) | (b4L << 24) | (b3L << 16) |
                                                               (b2L << 8) | b1L);
                                            }

                                            tmpDouble = Double.longBitsToDouble(tmpLong);
                                            buffer[(4* (x + (y * xDim))) + 2] = (float)tmpDouble;
                                        }

                                        x++;

                                        if (x == ((xTile + 1) * tileWidth)) {
                                            x = xTile * tileWidth;
                                            y++;
                                        }
                                    } // for (j = 0; j < decomp.length; j+= 8, i += 4)    
                                } // else if (planarRGB < (2 * stripsPerImage))
                                else { // planarRGB >= (2 * stripsPerImage)
                                    for (j = 0; j < resultLength; j+= 8, i += 4) {
                                        
                                        if ((x < xDim) && (y < yDim)) {

                                            if (((i + progress) % mod) == 0) {
                                                fireProgressStateChanged(Math.round((float) (i/3 + 
                                                                         2*buffer.length/3 + progress) /
                                                                         progressLength * 100));
                                            }

                                            b1L = getUnsignedByte(decomp,j) & 0xff;
                                            b2L = getUnsignedByte(decomp,j+1) & 0xff;
                                            b3L = getUnsignedByte(decomp,j+2) & 0xff;
                                            b4L = getUnsignedByte(decomp,j+3) & 0xff;
                                            b5L = getUnsignedByte(decomp,j+4) & 0xff;
                                            b6L = getUnsignedByte(decomp,j+5) & 0xff;
                                            b7L = getUnsignedByte(decomp,j+6) & 0xff;
                                            b8L = getUnsignedByte(decomp,j+7) & 0xff;

                                            if (endianess) {
                                                tmpLong = ((b1L << 56) | (b2L << 48) | (b3L << 40) | (b4L << 32) | (b5L << 24) | (b6L << 16) |
                                                               (b7L << 8) | b8L); // Big Endian
                                            } else {
                                                tmpLong = ((b8L << 56) | (b7L << 48) | (b6L << 40) | (b5L << 32) | (b4L << 24) | (b3L << 16) |
                                                               (b2L << 8) | b1L);
                                            }

                                            tmpDouble = Double.longBitsToDouble(tmpLong);
                                            buffer[(4* (x + (y * xDim))) + 3] = (float)tmpDouble;
                                        }

                                        x++;

                                        if (x == ((xTile + 1) * tileWidth)) {
                                            x = xTile * tileWidth;
                                            y++;
                                        }
                                    } // for (j = 0; j < decomp.length; j+= 8, i += 4)    
                                } // else planarRGB >= (2 * stripsPerImage)
                            } else { // not (lzwCompression ||| zlibCompression)
                                if (planarRGB < stripsPerImage) {

                                    for (j = 0; j < nBytes; j+= 8, i += 4) {
    
                                        if ((x < xDim) && (y < yDim)) {
    
                                            if (((i + progress) % mod) == 0) {
                                                fireProgressStateChanged(Math.round((float) (i/3 + progress) /
                                                                                        progressLength * 100));
                                            }
    
                                            buffer[4 * (x + (y * xDim))] = 255.0f;
                                            b1L = getUnsignedByte(decomp,j) & 0xff;
                                            b2L = getUnsignedByte(decomp,j+1) & 0xff;
                                            b3L = getUnsignedByte(decomp,j+2) & 0xff;
                                            b4L = getUnsignedByte(decomp,j+3) & 0xff;
                                            b5L = getUnsignedByte(decomp,j+4) & 0xff;
                                            b6L = getUnsignedByte(decomp,j+5) & 0xff;
                                            b7L = getUnsignedByte(decomp,j+6) & 0xff;
                                            b8L = getUnsignedByte(decomp,j+7) & 0xff;

                                            if (endianess) {
                                                tmpLong = ((b1L << 56) | (b2L << 48) | (b3L << 40) | (b4L << 32) | (b5L << 24) | (b6L << 16) |
                                                               (b7L << 8) | b8L); // Big Endian
                                            } else {
                                                tmpLong = ((b8L << 56) | (b7L << 48) | (b6L << 40) | (b5L << 32) | (b4L << 24) | (b3L << 16) |
                                                               (b2L << 8) | b1L);
                                            }

                                            tmpDouble = Double.longBitsToDouble(tmpLong);
                                            buffer[(4* (x + (y * xDim))) + 1] = (float)tmpDouble;
                                        } // if ((x < xDim) && (y < yDim))
    
                                        x++;
    
                                        if (x == ((xTile + 1) * tileWidth)) {
                                            x = xTile * tileWidth;
                                            y++;
                                        }
                                    } // for (j = 0; j < nBytes; j+= 8, i += 4)
                                } // if (planarRGB < stripsPerImage)
                                else if (planarRGB < (2 * stripsPerImage)) {
                                    for (j = 0; j < nBytes; j+= 8, i += 4) {
                                        
                                        if ((x < xDim) && (y < yDim)) {
    
                                            if (((i + progress) % mod) == 0) {
                                                fireProgressStateChanged(Math.round((float) (i/3 + 
                                                                         buffer.length/3 + progress) /
                                                                                        progressLength * 100));
                                            }
    
                                            b1L = getUnsignedByte(decomp,j) & 0xff;
                                            b2L = getUnsignedByte(decomp,j+1) & 0xff;
                                            b3L = getUnsignedByte(decomp,j+2) & 0xff;
                                            b4L = getUnsignedByte(decomp,j+3) & 0xff;
                                            b5L = getUnsignedByte(decomp,j+4) & 0xff;
                                            b6L = getUnsignedByte(decomp,j+5) & 0xff;
                                            b7L = getUnsignedByte(decomp,j+6) & 0xff;
                                            b8L = getUnsignedByte(decomp,j+7) & 0xff;

                                            if (endianess) {
                                                tmpLong = ((b1L << 56) | (b2L << 48) | (b3L << 40) | (b4L << 32) | (b5L << 24) | (b6L << 16) |
                                                               (b7L << 8) | b8L); // Big Endian
                                            } else {
                                                tmpLong = ((b8L << 56) | (b7L << 48) | (b6L << 40) | (b5L << 32) | (b4L << 24) | (b3L << 16) |
                                                               (b2L << 8) | b1L);
                                            }

                                            tmpDouble = Double.longBitsToDouble(tmpLong);
                                            buffer[(4* (x + (y * xDim))) + 2] = (float)tmpDouble;
                                        } // if ((x < xDim) && (y < yDim))
    
                                        x++;
    
                                        if (x == ((xTile + 1) * tileWidth)) {
                                            x = xTile * tileWidth;
                                            y++;
                                        }
                                    } // for (j = 0; j < nBytes; j+= 8, i += 4)    
                                } // else if (planarRGB < (2 * stripsPerImage))
                                else { // planarRGB >= (2 * stripsPerImage)
                                    for (j = 0; j < nBytes; j+= 8, i += 4) {
                                        
                                        if ((x < xDim) && (y < yDim)) {
    
                                            if (((i + progress) % mod) == 0) {
                                                fireProgressStateChanged(Math.round((float) (i/3 + 
                                                                         2*buffer.length/3 + progress) /
                                                                                        progressLength * 100));
                                            }
    
                                            b1L = getUnsignedByte(decomp,j) & 0xff;
                                            b2L = getUnsignedByte(decomp,j+1) & 0xff;
                                            b3L = getUnsignedByte(decomp,j+2) & 0xff;
                                            b4L = getUnsignedByte(decomp,j+3) & 0xff;
                                            b5L = getUnsignedByte(decomp,j+4) & 0xff;
                                            b6L = getUnsignedByte(decomp,j+5) & 0xff;
                                            b7L = getUnsignedByte(decomp,j+6) & 0xff;
                                            b8L = getUnsignedByte(decomp,j+7) & 0xff;

                                            if (endianess) {
                                                tmpLong = ((b1L << 56) | (b2L << 48) | (b3L << 40) | (b4L << 32) | (b5L << 24) | (b6L << 16) |
                                                               (b7L << 8) | b8L); // Big Endian
                                            } else {
                                                tmpLong = ((b8L << 56) | (b7L << 48) | (b6L << 40) | (b5L << 32) | (b4L << 24) | (b3L << 16) |
                                                               (b2L << 8) | b1L);
                                            }

                                            tmpDouble = Double.longBitsToDouble(tmpLong);
                                            buffer[(4* (x + (y * xDim))) + 3] = (float)tmpDouble;
                                        } // if ((x < xDim) && (y < yDim))
    
                                        x++;
    
                                        if (x == ((xTile + 1) * tileWidth)) {
                                            x = xTile * tileWidth;
                                            y++;
                                        }
                                    } // for (j = 0; j < nBytes; j+= 8, i += 4)     
                                } // else planarRGB >= (2 * stripsPerImage)
                            } // else not lzwCompression

                            xTile++;

                            if (xTile == tilesAcross) {
                                xTile = 0;
                                yTile++;
                            }

                            x = xTile * tileWidth;
                            y = yTile * tileLength;  
                            planarRGB++;
                            if ((planarRGB == stripsPerImage) || (planarRGB == 2*stripsPerImage)) {
                                i = 0;
                                x = 0;
                                y = 0;
                                xTile = 0;
                                yTile = 0;
                            }        
                        } // if (chunky == false && bitsPerSample[0] == 64)
                        break;
                } // switch(fileInfo.getDataType())
            } // try
            catch (OutOfMemoryError error) {
                System.gc();
                throw error;
            }
        } // for (i = 0; i < tilesPerSlice; i++)
        
        if (isYCbCr && (!jpegCompression)) {
            YCbCrtoRGB(buffer, YBuffer, CbInBuffer, CrInBuffer);
        }

        decomp = null;
    }

    /**
     * Writes color map to the TIFF file.
     *
     * @param   location  Location to write at.
     *
     * @throws  IOException  DOCUMENT ME!
     */
    private void writeColorMap(int location) throws IOException {
        int i, j;
        boolean endianess = image.getFileInfo(0).getEndianess();
        long saveLoc = raFile.getFilePointer();
        raFile.seek(location);

        byte[] colorTable = new byte[768 * 2];
        j = 0;

        for (i = 0; i < 256; i++) {
            setBufferShort(colorTable, (short) (LUT.getFloat(1, i) + 0.5), j, endianess);
            setBufferShort(colorTable, (short) (LUT.getFloat(2, i) + 0.5), j + 512, endianess);
            setBufferShort(colorTable, (short) (LUT.getFloat(3, i) + 0.5), j + 1024, endianess);
            j += 2;
        }

        raFile.write(colorTable);
        raFile.seek(saveLoc);
    }

    /**
     * Writes TIFF starting file header.
     *
     * @throws  IOException  DOCUMENT ME!
     */
    private void writeHeader() throws IOException {
        boolean endianess = image.getFileInfo(0).getEndianess();
        byte[] hdr = new byte[8];

        if (endianess == true) {
            hdr[0] = 77; // Big endian.
            hdr[1] = 77;
            hdr[2] = 0; // 42 magic number
            hdr[3] = 42;
            hdr[4] = 0; // 8 (offset to first IFD)
            hdr[5] = 0;
            hdr[6] = 0;
            hdr[7] = 8;
        } else {
            hdr[0] = 73; // "49" little endian
            hdr[1] = 73;
            hdr[2] = 42; // 42 magic number
            hdr[3] = 0;
            hdr[4] = 8; // 8 (offset to first IFD)
            hdr[5] = 0;
            hdr[6] = 0;
            hdr[7] = 0;
        }

        raFile.write(hdr);
    }

    /**
     * Writes one 12 byte IFD entry.
     *
     * @param   tag     DOCUMENT ME!
     * @param   type    DOCUMENT ME!
     * @param   count   DOCUMENT ME!
     * @param   value   DOCUMENT ME!
     * @param   value2  DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    private void writeIFD(int tag, int type, int count, int value, int value2) throws IOException {
        boolean endianess = image.getFileInfo(0).getEndianess();
        writeShort((short) tag, endianess);
        writeShort((short) type, endianess);
        writeInt(count, endianess);

        if ((type == SHORT) && (count < 3)) {
            writeShort((short) value, endianess);
            writeShort((short) value2, endianess);
        } else {
            writeInt(value, endianess);
        }
    }

    /**
     * Writes one IFD (Image File Directory). One IFD per image
     *
     * @param               image          offset to next IFD. If equal to zero then end of images
     * @param               imageOffset    DOCUMENT ME!
     * @param               nextIFD        DOCUMENT ME!
     * @param               index          image index for file information.
     * @param               theStripCount  DOCUMENT ME!
     * @param               writePackBit   DOCUMENT ME!
     *
     * @throws              IOException  DOCUMENT ME!
     *
     * @theStripByteCounts  number of bytes in the strip
     */
    private void writeIFDs(ModelImage image, int imageOffset, int nextIFD, int index, int theStripCount,
                           boolean writePackBit) throws IOException {
        boolean endianess = image.getFileInfo(0).getEndianess();
        int type;
        int bitsPerSample;
        int bytesPerSample = 2;
        int samplesPerPixel;
        int bitsPerSampleOffset = 0;
        int sampleFormatOffset = 0;
        int resolutionOffset;
        int ztEntries = 0;
        int zResOffset = 0;
        int tResOffset = 0;
        int resolutionCount = 16;
        int zResCount = 0;
        int tResCount = 0;
        int rgbCount = 0;
        int resXYUnit = 0;
        int resYUnit = 0;
        int resZUnit = 0;
        float xResol = (float) 0.0;
        float yResol = (float) 0.0;

        // TIFF 6.0 standard only allows 3 different units of measurement -
        // 1 for no unit, 2 for inch, and 3 for centimeter.
        // This unit of measurement must be applied to both the
        // X and Y resolution.
        resXYUnit = image.getFileInfo(index).getUnitsOfMeasure(0);
        resYUnit = image.getFileInfo(index).getUnitsOfMeasure(1);
        xResol = image.getFileInfo(index).getResolutions()[0];
        yResol = image.getFileInfo(index).getResolutions()[1];

        if ((resXYUnit != FileInfoBase.UNKNOWN_MEASURE) && (resXYUnit != FileInfoBase.INCHES) &&
                (resXYUnit != FileInfoBase.CENTIMETERS) && (resXYUnit != FileInfoBase.MILLIMETERS) &&
                (resXYUnit != FileInfoBase.METERS) && (resXYUnit != FileInfoBase.ANGSTROMS) &&
                (resXYUnit != FileInfoBase.NANOMETERS) && (resXYUnit != FileInfoBase.MICROMETERS) &&
                (resXYUnit != FileInfoBase.MILES) && (resXYUnit != FileInfoBase.KILOMETERS)) {
            resXYUnit = FileInfoBase.UNKNOWN_MEASURE;
        }

        if (resXYUnit == FileInfoBase.ANGSTROMS) {
            resXYUnit = FileInfoBase.CENTIMETERS;
            xResol = 1.0e-8f * xResol;
        } else if (resXYUnit == FileInfoBase.NANOMETERS) {
            resXYUnit = FileInfoBase.CENTIMETERS;
            xResol = 1.0e-7f * xResol;
        } else if (resXYUnit == FileInfoBase.MICROMETERS) {
            resXYUnit = FileInfoBase.CENTIMETERS;
            xResol = 1.0e-4f * xResol;
        } else if (resXYUnit == FileInfoBase.MILLIMETERS) {
            resXYUnit = FileInfoBase.CENTIMETERS;
            xResol = 0.1f * xResol;
        } else if (resXYUnit == FileInfoBase.METERS) {
            resXYUnit = FileInfoBase.CENTIMETERS;
            xResol = 100.0f * xResol;
        } else if (resXYUnit == FileInfoBase.KILOMETERS) {
            resXYUnit = FileInfoBase.CENTIMETERS;
            xResol = 1.0e5f * xResol;
        } else if (resXYUnit == FileInfoBase.MILES) {
            resXYUnit = FileInfoBase.INCHES;
            xResol = 63360.0f * xResol;
        }

        if (resXYUnit == FileInfoBase.CENTIMETERS) {

            // Change the Y resolution units to centimeters
            if (resYUnit == FileInfoBase.ANGSTROMS) {
                yResol = 1.0e-8f * yResol;
            } else if (resYUnit == FileInfoBase.NANOMETERS) {
                yResol = 1.0e-7f * yResol;
            } else if (resYUnit == FileInfoBase.MICROMETERS) {
                yResol = 1.0e-4f * yResol;
            } else if (resYUnit == FileInfoBase.MILLIMETERS) {
                yResol = 0.1f * yResol;
            } else if (resYUnit == FileInfoBase.INCHES) {
                yResol = 2.54f * yResol;
            } else if (resYUnit == FileInfoBase.METERS) {
                yResol = 100.0f * yResol;
            } else if (resYUnit == FileInfoBase.KILOMETERS) {
                yResol = 1.0e5f * yResol;
            } else if (resYUnit == FileInfoBase.MILES) {
                yResol = 1.6093e5f * yResol;
            }
        } // if (resXYUnit == FileInfoBase.CENTIMETERS)
        else if (resXYUnit == FileInfoBase.INCHES) {

            // Change the Y resolution units to inches
            if (resYUnit == FileInfoBase.ANGSTROMS) {
                yResol = 3.937e-9f * yResol;
            } else if (resYUnit == FileInfoBase.NANOMETERS) {
                yResol = 3.937e-8f * yResol;
            } else if (resYUnit == FileInfoBase.MICROMETERS) {
                yResol = 3.937e-5f * yResol;
            } else if (resYUnit == FileInfoBase.MILLIMETERS) {
                yResol = 3.937e-2f * yResol;
            } else if (resYUnit == FileInfoBase.CENTIMETERS) {
                yResol = 3.937e-1f * yResol;
            } else if (resYUnit == FileInfoBase.METERS) {
                yResol = 39.37f * yResol;
            } else if (resYUnit == FileInfoBase.KILOMETERS) {
                yResol = 3.937e4f * yResol;
            } else if (resYUnit == FileInfoBase.MILES) {
                yResol = 63360.0f * yResol;
            }
        } // else if (resXYUnit == FileInfoBase.INCHES)

        zRes = -1.0;

        if ((image.getNDims() > 2) && (image.getFileInfo(index).getResolutions().length > 2)) {
            zRes = (double) (image.getFileInfo(index).getResolutions()[2]);
            resZUnit = image.getFileInfo(index).getUnitsOfMeasure(2);

            // The EchoTech standard uses mm for the ResolutionZ field,
            // so convert to millimeters
            if (resZUnit == FileInfoBase.METERS) {
                zRes = 1000.0f * zRes;
            } else if (resZUnit == FileInfoBase.CENTIMETERS) {
                zRes = 10.0f * zRes;
            } else if (resZUnit == FileInfoBase.INCHES) {
                zRes = 25.4f * zRes;
            } else if (resZUnit == FileInfoBase.MICROMETERS) {
                zRes = 1.0e-3f * zRes;
            }
        }

        tRes = -1.0;

        if ((image.getNDims() > 3) && (image.getFileInfo(index).getResolutions().length > 3)) {
            tRes = (double) (image.getFileInfo(index).getResolutions()[3]);
        }

        if (zRes >= 0.0) {
            zResCount = 8;
            ztEntries++;
        }

        if (tRes >= 0.0) {
            tResCount = 8;
            ztEntries++;
        }

        type = image.getFileInfo(index).getDataType();

        switch (type) {

            case ModelStorageBase.BOOLEAN:
                bitsPerSample = 1;
                bytesPerSample = 1;
                samplesPerPixel = 1;
                nDirEntries = (short) (11 + ztEntries);
                rgbCount = 0;
                break;

            case ModelStorageBase.BYTE:
                bitsPerSample = 8;
                bytesPerSample = 1;
                samplesPerPixel = 1;
                if (image.getFileInfo(index).getPhotometric() == 3) {
                    nDirEntries = (short) (13 + ztEntries); // Add one for color map
                } else {
                    nDirEntries = (short) (12 + ztEntries);
                }

                rgbCount = 0;
                break;

            case ModelStorageBase.UBYTE:
                bitsPerSample = 8;
                bytesPerSample = 1;
                samplesPerPixel = 1;
                if (image.getFileInfo(index).getPhotometric() == 3) {
                    nDirEntries = (short) (13 + ztEntries); // Add one for color map
                } else {
                    nDirEntries = (short) (12 + ztEntries);
                }

                rgbCount = 0;
                break;

            case ModelStorageBase.USHORT:
            case ModelStorageBase.SHORT:
                bytesPerSample = 2;
                samplesPerPixel = 1;
                bitsPerSample = 16;
                nDirEntries = (short) (12 + ztEntries);
                rgbCount = 0;
                break;

            case ModelStorageBase.INTEGER:
            case ModelStorageBase.UINTEGER:
            case ModelStorageBase.FLOAT:
                bytesPerSample = 4;
                samplesPerPixel = 1;
                bitsPerSample = 32;
                nDirEntries = (short) (12 + ztEntries);
                rgbCount = 0;
                break;

            case ModelStorageBase.DOUBLE:
                bytesPerSample = 8;
                samplesPerPixel = 1;
                bitsPerSample = 64;
                nDirEntries = (short) (12 + ztEntries);
                rgbCount = 0;
                break;

            case ModelStorageBase.ARGB:
                bitsPerSample = 8;
                bytesPerSample = 1; // since SamplesPerPixel is defined as 3 for RGB images
                samplesPerPixel = 3;
                nDirEntries = (short) (14 + ztEntries);

                // RGB stores 8,8,8 for bitsPerSample
                // RGB stores 1,1,1 for sampleFormat
                bitsPerSampleOffset = (int) raFile.getFilePointer() + (2 + (nDirEntries * 12) + 4 + resolutionCount);
                rgbCount = 6;
                break;

            case ModelStorageBase.ARGB_USHORT:
                bitsPerSample = 16;
                bytesPerSample = 2; // since SamplesPerPixel is defined as 3 for RGB images
                samplesPerPixel = 3;
                nDirEntries = (short) (14 + ztEntries);

                // RGB stores 16,16,16 for bitsPerSample
                // RGB stores 1,1,1 for sampleFormat
                bitsPerSampleOffset = (int) raFile.getFilePointer() + (2 + (nDirEntries * 12) + 4 + resolutionCount);
                rgbCount = 6;
                break;

            default:
                throw new IOException("Unsupported ModelStorageBase type\n");
        }

        resolutionOffset = (int) raFile.getFilePointer() + (2 + (nDirEntries * 12) + 4);
        zResOffset = (int) raFile.getFilePointer() + (2 + (nDirEntries * 12) + 4 + resolutionCount + rgbCount);
        tResOffset = (int) raFile.getFilePointer() +
                     (2 + (nDirEntries * 12) + 4 + resolutionCount + rgbCount + zResCount);
        sampleFormatOffset = (int) raFile.getFilePointer() +
                             (2 + (nDirEntries * 12) + 4 + resolutionCount + rgbCount + zResCount + tResCount);
        writeShort(nDirEntries, endianess);
        writeIFD(IMAGE_WIDTH, SHORT, 1, image.getExtents()[0], 0);
        writeIFD(IMAGE_LENGTH, SHORT, 1, image.getExtents()[1], 0);

        if (type != ModelStorageBase.BOOLEAN) {

            if ((type == ModelStorageBase.ARGB) || (type == ModelStorageBase.ARGB_USHORT)) {
                writeIFD(BITS_PER_SAMPLE, SHORT, 3, bitsPerSampleOffset, 0);
            } else {
                writeIFD(BITS_PER_SAMPLE, SHORT, 1, bitsPerSample, 0);
            }
        }

        if (writePackBit == false) {
            writeIFD(COMPRESSION, SHORT, 1, 1, 0);
        } else {
            writeIFD(COMPRESSION, SHORT, 1, (short) 32773, 0);
        }

        writeIFD(PHOTO_INTERP, SHORT, 1, image.getFileInfo(index).getPhotometric(), 0);
        writeIFD(STRIP_OFFSETS, LONG, 1, imageOffset, 0);

        if ((type == ModelStorageBase.ARGB) || (type == ModelStorageBase.ARGB_USHORT)) {
            writeIFD(SAMPLES_PER_PIXEL, SHORT, 1, 3, 0);
        }

        writeIFD(ROWS_PER_STRIP, LONG, 1, image.getExtents()[1], 0);
        writeIFD(STRIP_BYTE_COUNTS, LONG, 1, theStripCount * bytesPerSample * samplesPerPixel, 0);
        writeIFD(XRESOLUTION, RATIONAL, 1, resolutionOffset, 0);
        writeIFD(YRESOLUTION, RATIONAL, 1, resolutionOffset + 8, 0);

        if (zRes >= 0.0) {
            writeIFD(ZRESOLUTION, DOUBLE, 1, zResOffset, 0);
        }

        if (tRes >= 0.0) {
            writeIFD(TRESOLUTION, DOUBLE, 1, tResOffset, 0);
        }

        if ((type == ModelStorageBase.ARGB) || (type == ModelStorageBase.ARGB_USHORT)) {
            writeIFD(PLANAR_CONFIG, SHORT, 1, 1, 0); // chucky format (rgb,rgb,rgb ...) baseline tiff
        }

        writeIFD(RESOLUTION_UNIT, SHORT, 1, resXYUnit, 0);

        if ((LUT != null) && ((type == ModelStorageBase.BYTE) || (type == ModelStorageBase.UBYTE)) &&
                (image.getFileInfo(index).getPhotometric() == 3)) {
            writeIFD(COLOR_MAP, SHORT, 768, LUTOffset, 0);
        }

        if ((type == ModelStorageBase.BOOLEAN) || (type == ModelStorageBase.UBYTE) ||
                (type == ModelStorageBase.USHORT) || (type == ModelStorageBase.UINTEGER)) {
            writeIFD(SAMPLE_FORMAT, SHORT, 1, 1, 0);
        } else if ((type == ModelStorageBase.BYTE) || (type == ModelStorageBase.SHORT) ||
                       (type == ModelStorageBase.INTEGER)) {
            writeIFD(SAMPLE_FORMAT, SHORT, 1, 2, 0);
        } else if ((type == ModelStorageBase.ARGB) || (type == ModelStorageBase.ARGB_USHORT)) {
            writeIFD(SAMPLE_FORMAT, SHORT, 3, sampleFormatOffset, 0);
        } else if ((type == ModelStorageBase.FLOAT) || (type == ModelStorageBase.DOUBLE)) {
            writeIFD(SAMPLE_FORMAT, SHORT, 1, 3, 0);
        } else if ((type == ModelStorageBase.ARGB_FLOAT)) {
            writeIFD(SAMPLE_FORMAT, SHORT, 3, sampleFormatOffset, 0);
        }

        writeInt(nextIFD, endianess);

        int numerator, denominator;
        int scale;

        // Largest int is 2,147,483,647
        scale = (int) Math.min(2.0e9 / xResol, 2.0e9);
        numerator = (int) (scale * xResol);
        denominator = scale;
        writeInt(denominator, endianess); // xResolution - RATIONAL
        writeInt(numerator, endianess); // xResolution - RATIONAL
        scale = (int) Math.min(2.0e9 / yResol, 2.0e9);
        numerator = (int) (scale * yResol);
        denominator = scale;
        writeInt(denominator, endianess); // yResolution - RATIONAL
        writeInt(numerator, endianess); // yResolution - RATIONAL

        if (type == ModelStorageBase.ARGB) {
            writeShort((short) 8, endianess); // RGB bitsPerSample ( R plane)
            writeShort((short) 8, endianess); // RGB bitsPerSample ( G plane)
            writeShort((short) 8, endianess); // RGB bitsPerSample ( B plane)
        } else if (type == ModelStorageBase.ARGB_USHORT) {
            writeShort((short) 16, endianess); // RGB bitsPerSample ( R plane)
            writeShort((short) 16, endianess); // RGB bitsPerSample ( G plane)
            writeShort((short) 16, endianess); // RGB bitsPerSample ( B plane)
        }

        if (zRes >= 0.0) {
            writeDouble(zRes, endianess);
        }

        if (tRes >= 0.0) {
            writeDouble(tRes, endianess);
        }

        if ((type == ModelStorageBase.ARGB) || (type == ModelStorageBase.ARGB_USHORT)) {
            writeShort((short) 1, endianess);
            writeShort((short) 1, endianess);
            writeShort((short) 1, endianess);
        } else if (type == ModelStorageBase.ARGB_FLOAT) {
            writeShort((short) 3, endianess);
            writeShort((short) 3, endianess);
            writeShort((short) 3, endianess);
        }
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    /**
     * Simple class to store image offsets and bytes located at the offset.
     */
    private class Index {

        /** DOCUMENT ME! */
        public int byteCount = 0;

        /** DOCUMENT ME! */
        public int index = 0;

        /**
         * Creates a new Index object.
         *
         * @param  _index  DOCUMENT ME!
         */
        public Index(int _index) {
            index = _index;
        }
    }
}
