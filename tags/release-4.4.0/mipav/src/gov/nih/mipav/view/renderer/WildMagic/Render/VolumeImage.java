package gov.nih.mipav.view.renderer.WildMagic.Render;

import gov.nih.mipav.MipavMath;
import gov.nih.mipav.model.algorithms.AlgorithmTransform;
import gov.nih.mipav.model.algorithms.utilities.AlgorithmChangeType;
import gov.nih.mipav.model.file.FileIO;
import gov.nih.mipav.model.file.FileInfoBase;
import gov.nih.mipav.model.file.FileUtility;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.model.structures.ModelSimpleImage;
import gov.nih.mipav.model.structures.ModelLUT;
import gov.nih.mipav.model.structures.ModelRGB;
import gov.nih.mipav.model.structures.ModelStorageBase;
import gov.nih.mipav.model.structures.TransMatrix;
import gov.nih.mipav.model.structures.TransferFunction;
import gov.nih.mipav.view.ViewJFrameImage;
import gov.nih.mipav.view.ViewJProgressBar;
import gov.nih.mipav.view.ViewUserInterface;
import gov.nih.mipav.view.dialogs.JDialogBase;
import gov.nih.mipav.view.dialogs.JDialogGradientMagnitude;
import gov.nih.mipav.view.dialogs.JDialogLaplacian;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.Buffer;

import WildMagic.LibFoundation.Mathematics.Vector2f;
import WildMagic.LibGraphics.Rendering.GraphicsImage;
import WildMagic.LibGraphics.Rendering.Texture;


public class VolumeImage implements Serializable
{
    /**  */
    private static final long serialVersionUID = -7254697711265907746L;

    /** Reference to ModelImage image */
    private ModelImage m_kImage;

    /** GraphicsImage contains GM opacity transfer function data: */
    private GraphicsImage m_kOpacityMap_GM = null;
    /** Texture contains texture filter modes and GraphicsImage for opacity
     * transfer function: */
    private Texture m_kOpacityMapTarget_GM = null;

    /** Data storage for volume: */
    private GraphicsImage[] m_kVolume;
    /** Texture object for data: */
    private Texture m_kVolumeTarget;
    
    /** Data storage for normals: */
    private GraphicsImage[] m_kNormal;
    /** Texture object for normal map: */
    private Texture m_kNormalMapTarget;
    
    /** Data storage for color map: */
    private GraphicsImage m_kColorMap;
    /** Texture object for color map: */
    private Texture m_kColorMapTarget;
    
    /** Data storage for opacity map: */
    private GraphicsImage m_kOpacityMap = null;
    /** Texture object for opacity map: */
    private Texture m_kOpacityMapTarget;
    
    /** Data storage for volume gradient magnitude: */
    private GraphicsImage[] m_kVolumeGM;
    /** Texture object for data: */
    private Texture m_kVolumeGMTarget;
    
    /** Data storage for volume  second derivative: */
    private GraphicsImage[] m_kVolumeGMGM;
    /** Texture object for data: */
    private Texture m_kVolumeGMGMTarget;

    /** Data storage for surfaces: */
    private GraphicsImage m_kSurfaceImage;
    /** Texture object for surfaces: */
    private Texture m_kSurfaceTarget;

    /** ModelLUT  */
    private ModelLUT m_kLUT = null;
    /** ModelRGB  */
    private ModelRGB m_kRGBT = null;

    /** Image scale factors for display in 3D */
    private float m_fX = 1, m_fY = 1, m_fZ = 1;
    /** Image name post-fix typically either 'A' or 'B' */
    private String m_kPostfix = null;
    /** Directory for calculated images */
    private String m_kDir = null;
    
    /** Histogram data for multi-histogram interface */
    private GraphicsImage[] m_kHisto = null;
    /** Texture object for data: */
    private Texture m_kHistoTarget;
    /** Texture coordinates for displaying histogram in 2D */
    private Vector2f[] m_akHistoTCoord = null;
    private float m_fDRRNormalize = 255.0f;

    /** Current position in time (4D data) */
    private int m_iTimeSlice = 0;
    /** Total number of time-slices (4D data) */
    private int m_iTimeSteps = 0;
    
    /** 3D sub-images (4D data) */
    private ModelImage[] m_akImages;
    /** When true the supporting images are re-computed */
    private boolean m_bCompute = true;
    
    private float[] m_fGMMin;
    private float[] m_fGMMax;
    
    /**
     * Create a Volume image with the input ModelImage.
     * @param kImage input ModelImage
     * @param kPostfix Postfix for images 'A' or 'B'
     * @param bCompute when true re-compute the image gradient mag, etc. when false read from disk.
     * @param kDir directory for storing calculated images
     * @param iFilterType filter type for resampling image
     * @param aiExtents target extents.
     */

    public VolumeImage() {}
    
    public VolumeImage( ModelImage kImage, String kPostfix, boolean bCompute, String kDir, 
            int iFilterType, int[] aiExtents,
            ViewJProgressBar kProgress, int iProgress )
    {        
        if ( kProgress != null ) { kProgress.setMessage("Creating VolumeImage..."); }
        
        m_kPostfix = new String(kPostfix);
        m_kDir = new String(kDir);
        String kImageName = ModelImage.makeImageName( kImage.getFileInfo(0).getFileName(), "_" + kPostfix);
        File kFile = new File( kDir + kImageName + ".xml" );
        if ( !bCompute && kFile.exists() )
        {
            m_kImage = ReadFromDisk( kImageName, m_kDir );
        }
        else
        {
            bCompute = true;
            ReconfigureImage( kImage, kImageName, m_kDir, iFilterType, aiExtents );
        }
        m_bCompute = bCompute;
        m_kImage.calcMinMax();
        if ( kProgress != null ) { kProgress.updateValueImmed( kProgress.getValue() + iProgress ); }
        init( kProgress, iProgress );
    }
    
    private void init( ViewJProgressBar kProgress, int iProgress  )
    {
        initLUT();
        initImages( m_bCompute, m_kPostfix, m_kDir, kProgress, iProgress );     

        if ( kProgress != null ) { kProgress.setMessage("Creating VolumeImage Normals..."); }
        if ( !m_bCompute )
        {
            for ( int i = 0; i < m_iTimeSteps; i++ )
            {
                String kImageName = ModelImage.makeImageName( m_kImage.getFileInfo(0).getFileName(), "_Normal_" + i);
                //System.err.println( kImageName );
                ModelImage kNormal = ReadFromDisk( kImageName, m_kDir );
                m_kNormal[i] = UpdateData(kNormal, 0, null, m_kNormal[i], m_kNormalMapTarget, kNormal.getImageName(), true );
                kNormal.disposeLocal();
            }
        }
        if ( kProgress != null ) { kProgress.updateValueImmed( kProgress.getValue() + iProgress ); }
    }
    
    /**
     * Copy the data from the input GraphicsImage and return a new ModelImage of that data.
     * @return new ModelImage from Volume Texture on GPU.
     * @param kImage Graphics Image to copy
     * @param bSwap when true convert from RGBA (graphics format) to ARGB (ModelImage format)
     * @return new ModelImage
     */
    public static ModelImage CreateImageFromTexture( GraphicsImage kImage, boolean bSwap )
    {
        int iXBound = kImage.GetBound(0);
        int iYBound = kImage.GetBound(1);
        int iZBound = kImage.GetBound(2);
        int iSize = iXBound * iYBound *iZBound;
        int[] extents = new int[]{iXBound, iYBound, iZBound};
        
        ModelImage kResult = null;
        if ( kImage.GetFormat() == GraphicsImage.FormatMode.IT_RGBA8888 )
        {
            byte[] aucData = kImage.GetData();
            if ( bSwap )
            {
                byte bVal = 0;
                aucData = new byte[4*iXBound*iYBound*iZBound];
                for ( int i = 0; i < iSize; i += 4)
                {
                    if ( kImage.GetData()[i+1] > bVal )
                    {
                        bVal = kImage.GetData()[i+1];
                    }
                    aucData[i] = kImage.GetData()[i+3];
                    aucData[i+1] = kImage.GetData()[i+1];
                    aucData[i+2] = kImage.GetData()[i+2];
                    aucData[i+3] = kImage.GetData()[i];
                }
                //System.err.println( bVal );
            }
            try {
                kResult = new ModelImage(ModelStorageBase.ARGB, extents, "" );
                kResult.importData( 0, aucData, true );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
        {
            byte[] aiImageData = kImage.GetData();
            try {
                kResult = new ModelImage( ModelStorageBase.UBYTE, extents, "" );
                kResult.importData( 0, aiImageData, true );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return kResult;
    }
    
    /**
     * Initialize the textures for the color lookup table.
     * @param kLUT the new LUT.
     * @param kRGBT the new RGB table.
     * @param kPostfix the string postfix to concatenate to the "ColorMap" image name.
     * @return GraphicsImage, the new GraphicsImage storing the colormap lookup table.
     */
    public static GraphicsImage InitColorMap ( ModelLUT kLUT, ModelRGB kRGBT, String kPostFix )
    {
        byte[] aucData = new byte[256*4];
        if ( kLUT == null )
        {
            ModelLUT.exportIndexedLUTMin( kRGBT, aucData );
            
        }
        else
        {
            ModelLUT.exportIndexedLUTMin( kLUT, aucData );
        }
        return new GraphicsImage(
                                 GraphicsImage.FormatMode.IT_RGBA8888,256,aucData,
                                 new String( "ColorMap" + kPostFix ) );
    }
    

    /**
     * Sets the Texture object containing the color lookup table based on the ModelRGB.
     * @param kTexture the Texture object containing the colormap GraphicsImage.
     * @param kImage the GraphicsImage containing the colormap data.
     * @param kRGBT the new ModelRGB.
     */
    public static void SetRGBT( Texture kTexture, GraphicsImage kImage, ModelRGB kRGBT )
    {
        if ( kRGBT == null )
        {
            return;
        }
        ModelLUT.exportIndexedLUTMin( kRGBT, kImage.GetData() );
        kTexture.Reload(true);
    }

    /**
     * Update the image volume data on the GPU.
     * @param kImage the new ModelImage
     * @param kVolumeImage the volume data image.
     * @param kVolumeTexture the volume data texture.
     * @param kPostFix the postfix string for the image name.
     */
    public static GraphicsImage UpdateData( ModelImage kImage, int iTimeSlice, ModelImage kNewImage, 
            GraphicsImage kVolumeImage,
            Texture kVolumeTexture, String kImageName, boolean bSwap )
    {
        GraphicsImage kReturn = kVolumeImage;
        int iXBound = kImage.getExtents()[0];
        int iYBound = kImage.getExtents()[1];
        int iZBound = kImage.getExtents()[2];

        byte[] aucData = null;
        int iSize = iXBound*iYBound*iZBound;
        if ( kImage.isColorImage() )
        {
            iSize *= 4;
            aucData = new byte[iSize];
            try {
                kImage.exportData( iTimeSlice * iSize, iSize, aucData );
                if ( bSwap )
                {
                    for ( int i = 0; i < iSize; i += 4)
                    {
                        byte tmp = aucData[i];
                        aucData[i] = aucData[i+1];
                        aucData[i+1] = aucData[i+2];
                        aucData[i+2] = aucData[i+3];
                        aucData[i+3] = tmp;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if ( kReturn == null )
            {
                kReturn =
                    new GraphicsImage( GraphicsImage.FormatMode.IT_RGBA8888,
                            iXBound,iYBound,iZBound,aucData, kImageName);
            }
            else
            {
                kReturn.SetData( aucData, iXBound, iYBound, iZBound );
            }
        }
        else
        {
            aucData = new byte[iSize];
            try {
                kImage.exportData( iTimeSlice * iSize, iSize, aucData );
                if ( kReturn == null )
                {
                    kReturn =
                    new GraphicsImage( GraphicsImage.FormatMode.IT_L8, 
                            iXBound,iYBound,iZBound, aucData, kImageName);
                }
                else
                {
                    kReturn.SetData( aucData, iXBound, iYBound, iZBound );
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        if ( kNewImage != null )
        {
            try {
                kNewImage.importData( 0, aucData, true );
            } catch (IOException e) {}
        }
        if ( kVolumeTexture != null )
        {
            kVolumeTexture.Reload(true);
        }
        return kReturn;
    }
    
    

    /**
     */
    public static GraphicsImage UpdateData( ModelSimpleImage kImage, String kImageName )
    {
        GraphicsImage.FormatMode eType = GraphicsImage.FormatMode.IT_L32F;

        if ( kImage.nDims == 3 )
        {
            return new GraphicsImage( eType, kImage.extents[0],kImage.extents[1],kImage.extents[2], 
                    kImage.data, kImageName);
        }
        return new GraphicsImage( eType, kImage.extents[0],kImage.extents[1], 1, 
                kImage.data, kImageName);
    }

    /**
     * Update the LUT texture sent to the GPU.
     * @param kColorTexture the color-map Texture object.
     * @param kColorMap the color-map GraphicsImage object (stores data).
     * @param kLUT the new LUT.
     */
    public static void UpdateImages(Texture kColorTexture, GraphicsImage kColorMap, ModelLUT kLUT)
    {
        if ( kLUT == null )
        {
            return;
        }
        ModelLUT.exportIndexedLUTMin( kLUT, kColorMap.GetData() );
        //for ( int i = 0; i < kColorMap.GetData().length; i++ )
        //{
        //    System.err.println( kColorMap.GetData()[i]);
        //}
        kColorTexture.Reload(true);
    }
    

    /**
     * Save the normal image data to disk
     * @param i current sub-image (3D or 4D data)
     * @param kImage base image.
     */
    public void CopyNormalFiles(int i, ModelImage kImage)
    {
        kImage.calcMinMax();
        kImage.saveImage( m_kDir, kImage.getImageName(), FileUtility.XML, false, false );
        m_kNormal[i] = UpdateData(kImage, 0, null, m_kNormal[i], m_kNormalMapTarget, kImage.getImageName(), true );
    }
    
    /**
     * Read the current Volume Texture from the GPU and return a new ModelImage of that data.
     * @return new ModelImage from Volume Texture on GPU.
     */
    public ModelImage CreateBinaryImageFromTexture( GraphicsImage kImage )
    {
        int iXBound = kImage.GetBound(0);
        int iYBound = kImage.GetBound(1);
        int iZBound = kImage.GetBound(2);
        int[] extents = new int[]{iXBound, iYBound, iZBound};
        
        ModelImage kResult = new ModelImage( ModelStorageBase.BOOLEAN, extents, JDialogBase.makeImageName(m_kImage.getImageName(), "_temp") );
        int i = 0;
        for (int iZ = 0; iZ < iZBound; iZ++)
        {
            for (int iY = 0; iY < iYBound; iY++)
            {
                for (int iX = 0; iX < iXBound; iX++)
                {
                    if ( kImage.GetData()[i++] > 0 )
                    {
                        kResult.set( iX, iY, iZ, true);
                    }
                }
            }
        }
        JDialogBase.updateFileInfo(m_kImage, kResult);
        return kResult;
    }
    

    /**
     * Memory cleanup.
     */
    public void dispose()
    {
        if ( m_kImage == null )
        {
            return;
        }
        m_kImage.disposeLocal();
        m_kImage = null;

        for ( int i = 0; i < m_kVolume.length; i++ )
        {
            m_kVolume[i].dispose();
        }
        m_kVolume = null;
        m_kVolumeTarget.dispose();
        m_kVolumeTarget = null;
    
        for ( int i = 0; i < m_kNormal.length; i++ )
        {
            m_kNormal[i].dispose();
        }
        m_kNormal = null;
        m_kNormalMapTarget.dispose();
        m_kNormalMapTarget = null;
    
        m_kColorMap.dispose();
        m_kColorMap = null;
        m_kColorMapTarget.dispose();
        m_kColorMapTarget = null;
    
        m_kOpacityMap.dispose();
        m_kOpacityMap = null;
        m_kOpacityMapTarget.dispose();
        m_kOpacityMapTarget = null;
    
        for ( int i = 0; i < m_kVolumeGM.length; i++ )
        {
            m_kVolumeGM[i].dispose();
        }
        m_kVolumeGM = null;
        m_kVolumeGMTarget.dispose();
        m_kVolumeGMTarget = null;
    
        for ( int i = 0; i < m_kVolumeGMGM.length; i++ )
        {
            m_kVolumeGMGM[i].dispose();
        }
        m_kVolumeGMGM = null;
        m_kVolumeGMGMTarget.dispose();
        m_kVolumeGMGMTarget = null;
        
        m_kOpacityMap_GM.dispose();
        m_kOpacityMap_GM = null;
        m_kOpacityMapTarget_GM.dispose();
        m_kOpacityMapTarget_GM = null;

        if ( m_kSurfaceImage != null )
        {
            m_kSurfaceImage.dispose();
            m_kSurfaceImage = null;
            m_kSurfaceTarget.dispose();
            m_kSurfaceTarget = null;
        }

        m_kLUT = null;
        m_kPostfix = null;
    
        for ( int i = 0; i < m_kHisto.length; i++ )
        {
            m_kHisto[i].dispose();
        }
        m_kHisto = null;
        m_akHistoTCoord = null;
    }

    public GraphicsImage GenerateGMImages( ModelImage kImageGM, String kPostFix )
    {
        int iXBound = kImageGM.getExtents()[0];
        int iYBound = kImageGM.getExtents()[1];
        int iZBound = kImageGM.getExtents()[2];
        kImageGM.calcMinMax();
        float fImageMaxGM = (float)kImageGM.getMax();
        float fImageMinGM = (float)kImageGM.getMin();

        byte[] abData = null;
        abData = new byte[iXBound*iYBound*iZBound];
        int i = 0;
        for (int iZ = 0; iZ < iZBound; iZ++)
        {
            for (int iY = 0; iY < iYBound; iY++)
            {
                for (int iX = 0; iX < iXBound; iX++)
                {
                    float fValue = kImageGM.getFloat(iX,iY,iZ);
                    abData[i++] = (byte)(255 * (fValue - fImageMinGM)/(fImageMaxGM - fImageMinGM));
                }
            }
        }
        return
            new GraphicsImage( GraphicsImage.FormatMode.IT_L8, 
                               iXBound,iYBound,iZBound, abData,
                               new String( "VolumeImage" + kPostFix));
    }

    public GraphicsImage GenerateImagesColor( ModelImage kImage, String kPostFix )
    {
        int iXBound = kImage.getExtents()[0];
        int iYBound = kImage.getExtents()[1];
        int iZBound = kImage.getExtents()[2];


        byte[] aucData = new byte[4*iXBound*iYBound*iZBound];
        byte[] aucGMData = new byte[4*iXBound*iYBound*iZBound];
        float temp;
        int iGM = 0;
        try {
            kImage.exportData( 0, kImage.getSize(), aucData );
            for ( int i = 0; i < kImage.getSize(); i += 4)
            {
                temp = (aucData[i+1] + aucData[i+2] + aucData[i+3])/3.0f;
                aucGMData[iGM++] = Float.valueOf(temp).byteValue();
                aucGMData[iGM++] = Float.valueOf(temp).byteValue();
                aucGMData[iGM++] = Float.valueOf(temp).byteValue();
                aucGMData[iGM++] = Float.valueOf(temp).byteValue();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new GraphicsImage( GraphicsImage.FormatMode.IT_RGBA8888,
                iXBound,iYBound,iZBound,aucGMData,
                new String( "VolumeImage" + kPostFix));

    }

    public void GenerateNormalFiles(  )
    {
        if ( !m_bCompute )
        {
            return;
        }
        for ( int i = 0; i < m_iTimeSteps; i++ )
        {
            VolumeImageNormalGM.main( this, m_akImages[i], m_kNormalMapTarget, i, 
                    ModelImage.makeImageName( m_kImage.getFileInfo(0).getFileName(), "_Normal_" + i));
        }
    }

    /**
     * Return the Volume color map Texture.
     * @return Volume color map Texture.
     */
    public Texture GetColorMapTarget()
    {
        return m_kColorMapTarget;
    }

    public float getDRRNorm()
    {
        return m_fDRRNormalize;
    }

    /**
     * Return the Gradient Magnitude Texture.
     * @return Gradient Magnitude Texture.
     */
    public Texture GetGradientMapTarget()
    {
        return m_kVolumeGMTarget;
    }
    

    public String GetHistoName()
    {
        return m_kHisto[m_iTimeSlice].GetName();
    }
    
    public Vector2f[] GetHistoTCoords()
    {
        return m_akHistoTCoord;
    }

    public float GetGMMin()
    {
        return m_fGMMin[m_iTimeSlice];
    }
    
    public float GetGMMax()
    {
        return m_fGMMax[m_iTimeSlice];
    }
    
    /**
     * Return the ModelImage volume data.
     * @return ModelImage volume data.
     */
    public ModelImage GetImage()
    {
        return m_kImage;
    }

    /**
     * Return the Volume LUT.
     * @return Volume LUT.
     */
    public ModelLUT GetLUT()
    {
        return m_kLUT;
    }
    

    /**
     * Return the Volume normal Texture.
     * @return Volume normal Texture.
     */
    public Texture GetNormalMapTarget()
    {
        return m_kNormalMapTarget;
    }    

    /**
     * Return the gradient magnitude opacity transfer function Texture.
     * @return gradient magnitude opacity transfer function Texture.
     */
    public Texture GetOpacityMapGMTarget()
    {
        return m_kOpacityMapTarget_GM;
    }

    /**
     * Return the Volume opacity transfer function Texture.
     * @return Volume opacity transfer function Texture.
     */
    public Texture GetOpacityMapTarget()
    {
        return m_kOpacityMapTarget;
    }

    /**
     * Return the postfix for this VolumeImage.
     * @return postfix for this VolumeImage.
     */
    public String GetPostfix()
    {
        return m_kPostfix;
    }

    /**
     * Return the Volume RGBT.
     * @return Volume RGBT.
     */
    public ModelRGB GetRGB()
    {
        return m_kRGBT;
    }

    /**
     * The ModelImage Volume x-scale factor.
     * @return Volume x-scale factor.
     */
    public float GetScaleX()
    {
        return m_fX;
    }

    /**
     * The ModelImage Volume y-scale factor.
     * @return Volume y-scale factor.
     */
    public float GetScaleY()
    {
        return m_fY;
    }

    /**
     * The ModelImage Volume z-scale factor.
     * @return Volume z-scale factor.
     */
    public float GetScaleZ()
    {
        return m_fZ;
    }
    
    /**
     * Return the 2nd derivative texture.
     * @return 2nd derivative texture.
     */
    public Texture GetSecondDerivativeMapTarget()
    {
        return m_kVolumeGMGMTarget;
    }


    /**
     * Return the surface mask Texture.
     * @return surface mask Texture.
     */
    public Texture GetSurfaceTarget()
    {
        return m_kSurfaceTarget;
    }


    public int GetTimeSlice()
    {
        return m_iTimeSlice;
    }

    /**
     * Return the Texture containing the volume data.
     * @return Texture containing the volume data.
     */
    public Texture GetVolumeTarget()
    {
        return m_kVolumeTarget;
    }
    
    /**
     * Return the Texture containing the volume data.
     * @return Texture containing the volume data.
     */
    public Buffer GetVolumeTargetBuffer()
    {
        return m_kVolumeTarget.GetImage().GetDataBuffer();
    }
    
    private void initImages( boolean bCreate, String kPostfix, String kDir, ViewJProgressBar kProgress, int iProgress  )
    {
        m_fDRRNormalize = computeIntegralNormalizationFactor();
        m_kColorMap = InitColorMap(m_kLUT, m_kRGBT, kPostfix);
        m_kOpacityMap = InitOpacityMap(m_kImage, kPostfix);
        m_kOpacityMap_GM = InitOpacityMap(m_kImage, new String(kPostfix + "_GM"));

        int iXBound = m_kImage.getExtents()[0];
        int iYBound = m_kImage.getExtents()[1];
        int iZBound = m_kImage.getExtents()[2];
        
        /* Map the ModelImage volume data to a texture image, including for
         * the ModelImage gradient magnitude data: */
        int[] aiExtents = m_kImage.getExtents();
        int iNDims = aiExtents.length;
        if ( iNDims == 3 )
        {
            m_iTimeSteps = 1;
            m_fGMMin = new float[1];
            m_fGMMax = new float[1];
            m_akImages = new ModelImage[ m_iTimeSteps ];    
            m_akImages[0] = m_kImage;
            
            m_kVolume = new GraphicsImage[1];
            m_kVolumeGM = new GraphicsImage[1];
            m_kVolumeGMGM = new GraphicsImage[1];
            m_kVolume[0] = UpdateData(m_kImage, m_iTimeSlice, null, null, m_kVolumeTarget, m_kImage.getImageName(), true );
            m_kNormal = new GraphicsImage[1];
            m_kNormal[0] = new GraphicsImage(GraphicsImage.FormatMode.IT_RGBA8888,
                                           iXBound,iYBound,iZBound,new byte[iXBound*iYBound*iZBound*4],
                                           new String("NormalMap"+kPostfix));
        }
        else
        {
            m_iTimeSteps = aiExtents[3];
            int[] aiSubset = new int[]{aiExtents[0], aiExtents[1], aiExtents[2]};
            
            m_akImages = new ModelImage[ m_iTimeSteps ];      
            m_fGMMin = new float[m_iTimeSteps];
            m_fGMMax = new float[m_iTimeSteps];      
            
            m_kVolume = new GraphicsImage[m_iTimeSteps];
            m_kVolumeGM = new GraphicsImage[m_iTimeSteps];
            m_kVolumeGMGM = new GraphicsImage[m_iTimeSteps];     
            m_kNormal = new GraphicsImage[m_iTimeSteps];
            
            for ( int i = 0; i < m_kVolume.length; i++ )
            {
                m_akImages[i] = new ModelImage( m_kImage.getType(), aiSubset, JDialogBase.makeImageName(m_kImage.getImageName(), "_" + i) );
                m_kVolume[i] = UpdateData(m_kImage, i, m_akImages[i], null, m_kVolumeTarget, m_akImages[i].getImageName(), true );      
                
                m_akImages[i].copyFileTypeInfo(m_kImage);
                m_akImages[i].calcMinMax();
                

                m_kNormal[i] = new GraphicsImage(GraphicsImage.FormatMode.IT_RGBA8888,
                        iXBound,iYBound,iZBound,new byte[iXBound*iYBound*iZBound*4],
                        new String("NormalMap"+kPostfix + i));
            }
        }
        GradientMagnitudeImage(m_kImage, kPostfix, kDir, bCreate, kProgress, iProgress );
        GenerateHistogram( m_kVolume, m_kVolumeGM, kPostfix  );
        
        m_kVolumeTarget = new Texture();
        m_kVolumeTarget.SetImage(m_kVolume[0]);
        m_kVolumeTarget.SetShared(true);
        m_kVolumeTarget.SetFilterType(Texture.FilterType.LINEAR);
        m_kVolumeTarget.SetWrapType(0,Texture.WrapType.CLAMP_BORDER);
        m_kVolumeTarget.SetWrapType(1,Texture.WrapType.CLAMP_BORDER);
        m_kVolumeTarget.SetWrapType(2,Texture.WrapType.CLAMP_BORDER);
        
        m_kColorMapTarget = new Texture();
        m_kColorMapTarget.SetImage(m_kColorMap);
        m_kColorMapTarget.SetShared(true);

        m_kOpacityMapTarget = new Texture();
        m_kOpacityMapTarget.SetImage(m_kOpacityMap);
        m_kOpacityMapTarget.SetShared(true);

        m_kNormalMapTarget = new Texture();
        m_kNormalMapTarget.SetImage(m_kNormal[0]);
        m_kNormalMapTarget.SetShared(true);
        m_kNormalMapTarget.SetFilterType(Texture.FilterType.LINEAR);
        m_kNormalMapTarget.SetWrapType(0,Texture.WrapType.CLAMP_BORDER);
        m_kNormalMapTarget.SetWrapType(1,Texture.WrapType.CLAMP_BORDER);
        m_kNormalMapTarget.SetWrapType(2,Texture.WrapType.CLAMP_BORDER);
        
        m_kOpacityMapTarget_GM = new Texture();
        m_kOpacityMapTarget_GM.SetImage(m_kOpacityMap_GM);
        m_kOpacityMapTarget_GM.SetShared(true);
        

        m_kSurfaceImage = new GraphicsImage(GraphicsImage.FormatMode.IT_L8,
                                       iXBound,iYBound,iZBound,new byte[iXBound*iYBound*iZBound],
                                       "SurfaceImage");
        m_kSurfaceTarget = new Texture();
        m_kSurfaceTarget.SetImage(m_kSurfaceImage);
        m_kSurfaceTarget.SetShared(true);
        m_kSurfaceTarget.SetFilterType(Texture.FilterType.LINEAR);
        m_kSurfaceTarget.SetWrapType(0,Texture.WrapType.CLAMP_BORDER);
        m_kSurfaceTarget.SetWrapType(1,Texture.WrapType.CLAMP_BORDER);
        m_kSurfaceTarget.SetWrapType(2,Texture.WrapType.CLAMP_BORDER);
        
        InitScale();
        
        /*
        float[] afGaussX = new float[3*3*3];
        int[] aiExtents = new int[]{3,3,3};
        float[] afSigmas = new float[]{.5f, .5f, .5f};
        int[] aiOrder = new int[]{1,0,0};
        GenerateGaussian Gx = new GenerateGaussian(afGaussX, aiExtents, afSigmas, aiOrder);
        Gx.calc(false);

        float[] afGaussY = new float[3*3*3];
        aiOrder[0] = 0;
        aiOrder[1] = 1;
        GenerateGaussian Gy = new GenerateGaussian(afGaussY, aiExtents, afSigmas, aiOrder);
        Gy.calc(true);       

        float[] afGaussZ = new float[3*3*3];
        aiOrder[0] = 0;
        aiOrder[1] = 0;
        aiOrder[2] = 1;
        GenerateGaussian Gz = new GenerateGaussian(afGaussZ, aiExtents, afSigmas, aiOrder);
        Gz.calc(true);

        for ( int i = 0; i < aiExtents[0]; i++ )
        {
            for ( int j = 0; j < aiExtents[1]; j++ )
            {
                for ( int k = 0; k < aiExtents[2]; k++ )
                {
                    System.err.println( "gaussian[" + i + "][" + j + "][" + k + "].rgb = (" +
                            afGaussX[i*aiExtents[1]*aiExtents[2] + j*aiExtents[2] + k] + ", " + 
                            afGaussY[i*aiExtents[1]*aiExtents[2] + j*aiExtents[2] + k] + ", " + 
                            afGaussZ[i*aiExtents[1]*aiExtents[2] + j*aiExtents[2] + k] + ");" ); 
                    		
                   
                }
                System.err.println( );
            }
            System.err.println( );
            System.err.println( );
        }
        */
    }
    
    /**
     * Initialize the textures for the opacity lookup table.
     * @param kImage the ModelImage the opacity transfer function applies to.
     * @param kPostfix the string postfix to concatenate to the "OpacityMap" image name.
     * @return GraphicsImage, the new GraphicsImage storing the colormap lookup table.
     */
    public GraphicsImage InitOpacityMap (ModelImage kImage, String kPostFix )
    {
        int iLutHeight = 256;
        float[] afData = new float[iLutHeight];
        float fRange = (float)(kImage.getMax() - kImage.getMin());
        float fStep = fRange / iLutHeight;
        float fDataValue = (float)kImage.getMin();
        for (int i = 0; i < iLutHeight; i++) {
            afData[i] = (float)( iLutHeight * (kImage.getMax() - fDataValue) / fRange);
            fDataValue += fStep;
        }

        return new GraphicsImage(
                                 GraphicsImage.FormatMode.IT_L8,iLutHeight,afData,
                                 new String( "OpacityMap" + kPostFix ));
    }
    
    /**
     * Return true if the Volume image is a color image.
     * @return true if the Volume image is a color image.
     */
    public boolean IsColorImage()
    {
        return m_kImage.isColorImage();
    }

    /**
     * Release the Textures containing the volume data. Once
     * Textures are released, they will be re-loaded onto the GPU during the
     * next frame.
     */
    public void ReleaseVolume()
    {
        m_kVolumeTarget.Release();
    }

    
    /**
     * Sets the ModelRGB for the iImage.
     * @param kRGBT new ModelRGB
     */
    public void SetRGBT(ModelRGB kRGBT)
    {
        SetRGBT( m_kColorMapTarget, m_kColorMap, kRGBT );
        m_kRGBT = kRGBT;
    }

    public void SetTimeSlice(int iSlice)
    {
        if ( m_iTimeSlice != iSlice )
        {
            m_iTimeSlice = iSlice;
            update4D();
        }
    }
    
    public void update4D( boolean bForward )
    {
        if (  m_iTimeSteps == 1 )
        {
            return;
        }
        if ( bForward )
        {
            m_iTimeSlice++;
        }
        else
        {
            m_iTimeSlice--;
        }
        if ( m_iTimeSlice >= m_iTimeSteps )
        {
            m_iTimeSlice = 0;
        }
        if ( m_iTimeSlice < 0 )
        {
            m_iTimeSlice = m_iTimeSteps-1;
        }
        
        update4D();
    }   
    /**
     * Update the image data.
     * @param kImage the modified ModelImage
     */
    public void UpdateData( ModelImage kImage )
    {
        m_kImage = kImage;
        VolumeImage.UpdateData( m_kImage, m_iTimeSlice, m_akImages[m_iTimeSlice], m_kVolume[m_iTimeSlice], m_kVolumeTarget, m_kImage.getImageName(), true );
    }    
    /**
     * Update the LUT for the ModelImage.
     * @param kLUT new LUT for ModelImage.
     */
    public void UpdateImages(ModelLUT kLUT)
    {
        if ( kLUT != null )
        {
            this.UpdateImages( m_kColorMapTarget, m_kColorMap, kLUT );
            m_kLUT = kLUT;
        }
    }
    

    /**
     * Update the transfer function for the image iImage.
     * @param kTransfer the new opacity transfer function
     * @param iImage the image to modify (0 = volume image, 2 = gradient mag)
     * @return boolean true when updated, false otherwise.
     */
    public boolean UpdateImages(TransferFunction kTransfer, int iImage, ModelImage kImage)
    {
        if ( iImage == 0 )
        {
            UpdateImages2( m_kImage, m_kColorMapTarget, m_kColorMap, kTransfer );
            return UpdateImages( m_kImage, m_kOpacityMapTarget, m_kOpacityMap, kTransfer );
        }
        else if ( (iImage == 2) &&
                   (kImage != null) &&
                   (m_kOpacityMapTarget_GM != null) &&
                   (m_kOpacityMap_GM != null)  )
         {
             return UpdateImages( kImage, m_kOpacityMapTarget_GM, m_kOpacityMap_GM, kTransfer );
         }
        return false;
    }


    /**
     * In order to map line integrals of image intensity to RGB colors where each color channel is 8 bits, it is
     * necessary to make sure that the integrals are in [0,255]. Producing a theoretical maximum value of a line
     * integral is not tractable in an application. This method constructs an approximate maximum by integrating along
     * each line of voxels in the image with line directions parallel to the coordinate axes. The 'processRay' call
     * adjusts the line integrals using the estimate, but still clamps the integrals to 255 since the estimate might not
     * be the true maximum.
     *
     * @return  float Integral normalization factor.
     */
    protected float computeIntegralNormalizationFactor() {       
        int iXBound = m_kImage.getExtents()[0];
        int iYBound = m_kImage.getExtents()[1];
        int iZBound = m_kImage.getExtents()[2];

        byte[] aucData = null;
        int iSize = iXBound*iYBound*iZBound;
        if ( m_kImage.isColorImage() )
        {
            iSize *= 4;
        }

        aucData = new byte[iSize];

        try {
            m_kImage.exportData( 0, iSize, aucData );
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // compute image normalization factor
        int iX, iY, iZ, iBase, iSteps;
        float fMaxIntegral = 0.0f;
        float fTStep, fIntegral;

        // fix y and z, integrate over x
        for (iY = 0; iY < iYBound; iY++) {

            for (iZ = 0; iZ < iZBound; iZ++) {
                iBase = iXBound * (iY + (iYBound * iZ));
                iSteps = iXBound - 1;
                fIntegral = 0.5f * ((aucData[iBase] & 0x0ff) + (aucData[iBase + iSteps] & 0x0ff));
                fTStep = 1.0f / iSteps;

                for (iX = 1; iX < iSteps; iX++) {
                    fIntegral += (aucData[iBase + iX] & 0x0ff);
                }

                fIntegral *= fTStep;

                if (fIntegral > fMaxIntegral) {
                    fMaxIntegral = fIntegral;
                }
            }
        }
        int iXYProduct = iXBound * iYBound;
        // fix x and z, integrate over y
        for (iX = 0; iX < iXBound; iX++) {

            for (iZ = 0; iZ < iZBound; iZ++) {
                iBase = iX + (iXYProduct * iZ);
                iSteps = iYBound - 1;
                fIntegral = 0.5f * ((aucData[iBase] & 0x0ff) + (aucData[iBase + (iXBound * iSteps)] & 0x0ff));
                fTStep = 1.0f / iSteps;

                for (iY = 1; iY < iSteps; iY++) {
                    fIntegral += (aucData[iBase + (iXBound * iY)] & 0x0ff);
                }

                fIntegral *= fTStep;

                if (fIntegral > fMaxIntegral) {
                    fMaxIntegral = fIntegral;
                }
            }
        }

        // fix x and y, integrate over z
        for (iX = 0; iX < iXBound; iX++) {

            for (iY = 0; iY < iYBound; iY++) {
                iBase = iX + (iXBound * iY);
                iSteps = iZBound - 1;
                fIntegral = 0.5f * ((aucData[iBase] & 0x0ff) + (aucData[iBase + (iXYProduct * iSteps)] & 0x0ff));
                fTStep = 1.0f / iSteps;

                for (iZ = 1; iZ < iSteps; iZ++) {
                    fIntegral += (aucData[iBase + (iXYProduct * iZ)] & 0x0ff);
                }

                fIntegral *= fTStep;

                if (fIntegral > fMaxIntegral) {
                    fMaxIntegral = fIntegral;
                }
            }
        }
        aucData = null;
        return (fMaxIntegral > 0.0f) ? (1.0f / fMaxIntegral) : 0.00f;
    }

    /**
     * Generate 2D histogram from the input image and the gradient-magnitude
     * @param kImage
     * @param kImageGM
     * @param kPostFix post-fix for the image name.
     */
    private void GenerateHistogram( GraphicsImage[] kImage, GraphicsImage[] kImageGM, String kPostFix  )
    {
        int iTMinX = 255, iTMaxX = 0;
        int iTMinY = 255, iTMaxY = 0;
        m_kHisto = new GraphicsImage[m_iTimeSteps];
        for ( int t = 0; t < m_iTimeSteps; t ++ )
        {
            float[] afCount = new float[256*256];
            for(int i=0; i<256*256; ++i) {
                afCount[i] = 0;
            }

            short a1, a2;
            byte[] abHistoData = kImageGM[t].GetData();
            byte[] abData = kImage[t].GetData();
            if ( m_kImage.isColorImage() )
            {
                int iHisto = 0;
                for ( int i = 0; i < abData.length; i +=4 )
                {
                    int iR = (abData[i]);
                    int iG = (abData[i+1]);
                    int iB = (abData[i+2]);
                    a1 = (short)(iR * 0.299 + iG * 0.587 + iB * 0.114);
                    a1 = (short)(a1 & 0x00ff);

                    iR = (abHistoData[i]);
                    iG = (abHistoData[i+1]);
                    iB = (abHistoData[i+2]);
                    a2 = (short)(iR * 0.299 + iG * 0.587 + iB * 0.114);
                    a2 = (short)(a2 & 0x00ff);
                    afCount[a1 +  a2 * 256] += 1;
                    iHisto++;
                }
            }
            else
            {
                int iHisto = 0;
                for ( int i = 0; i < abData.length; i++)
                {
                    a1 = (abData[i]);
                    a1 = (short)(a1 & 0x00ff);
                    a2 = (abHistoData[iHisto]);
                    a2 = (short)(a2 & 0x00ff);
                    afCount[a1 +  a2 * 256] += 1;
                    iHisto++;
                }
            }
            float max = 0;
            for(int i = 0; i< 256*256; ++i)
            {
                afCount[i] = (float)Math.log(afCount[i]);
                max = Math.max(afCount[i], max);
            }           

            byte[] abHisto = new byte[256*256];
            for(int i = 0; i< 256*256; ++i)
            {
                abHisto[i] = new Float(afCount[i]/max*255f).byteValue();
            }
            afCount = null;

            int iMinX = 255, iMaxX = 0;
            int iIndex = 0;
            for( int i=0; i< 256; i++ )
            {
                for( int j = 0; j < 256; j++ )
                {    
                    iIndex = i*256 + j;
                    if ( abHisto[iIndex] != 0 )
                    {
                        if ( iMinX > j )
                        {
                            iMinX = j;
                        }
                        if ( j > iMaxX )
                        {
                            iMaxX = j;
                        }
                    }
                }
            }

            int iMinY = 255, iMaxY = 0;
            for( int j = 0; j < 256; j++ )
            { 
                for( int i=0; i< 256; i++ )
                {
                    iIndex = i*256 + j;
                    if ( abHisto[iIndex] != 0 )
                    {
                        if ( iMinY > i )
                        {
                            iMinY = i;
                        }
                        if ( i > iMaxY )
                        {
                            iMaxY = i;
                        }
                    }
                }
            }
            if ( iTMinX > iMinX )
            {
                iTMinX = iMinX;
            }
            if ( iTMaxX < iMaxX )
            {
                iTMaxX = iMaxX;
            }
            
            if ( iTMinY > iMinY )
            {
                iTMinY = iMinY;
            }
            if ( iTMaxY < iMaxY )
            {
                iTMaxY = iMaxY;
            }
            
            //System.err.println( iMinX + ", " + iMinY + "    " + iMaxX + ", " + iMaxY );
            //iMinX = 0; iMaxX = 255;
            //iMinY = 0; iMaxY = 255;

            m_kHisto[t] = 
                new GraphicsImage( GraphicsImage.FormatMode.IT_L8, 
                        256,256, (byte[])null, new String("VolumeImageHisto" + kPostFix) );
            m_kHisto[t].SetData( abHisto, 256, 256 );
        }
        

        m_kHistoTarget = new Texture();
        m_kHistoTarget.SetImage(m_kHisto[0]);
        m_kHistoTarget.SetShared(true);
        m_kHistoTarget.SetFilterType(Texture.FilterType.LINEAR);
        m_kHistoTarget.SetWrapType(0,Texture.WrapType.CLAMP_BORDER);
        m_kHistoTarget.SetWrapType(1,Texture.WrapType.CLAMP_BORDER);
        m_kHistoTarget.SetWrapType(2,Texture.WrapType.CLAMP_BORDER);
        
        m_akHistoTCoord = new Vector2f[4];
        m_akHistoTCoord[0] = new Vector2f( iTMinX/255.0f, iTMinY/255.0f );
        m_akHistoTCoord[1] = new Vector2f( iTMaxX/255.0f, iTMinY/255.0f );
        m_akHistoTCoord[2] = new Vector2f( iTMaxX/255.0f, iTMaxY/255.0f );
        m_akHistoTCoord[3] = new Vector2f( iTMinX/255.0f, iTMaxY/255.0f );
        
        //System.err.println( iTMinX + ", " + iTMinY + "    " + iTMaxX + ", " + iTMaxY );
    }
   
    
    /**
     * Calculates and stores the gradient magnitude images (3D or 4D) for the input image. Or reads from disk.
     * @param kImage input image
     * @param kPostfix input post-fix
     * @param kDir directory for reading/writing GM images
     * @param bCreate when true generate and write the GM image to disk.
     */
    private void GradientMagnitudeImage(ModelImage kImage, String kPostfix, String kDir, boolean bCreate, ViewJProgressBar kProgress, int iProgress )
    {
        if ( kProgress != null ) { kProgress.setMessage("Creating VolumeImage Gradient Magnitude..."); }
        for ( int i = 0; i < m_iTimeSteps; i++ )
        {
            String kImageName = ModelImage.makeImageName( kImage.getFileInfo(0).getFileName(), 
                    new String ( "_GM_" + i ) );

            ModelImage kImageGM = null;
            if ( !bCreate )
            {
                FileIO fileIO = new FileIO();
                kImageGM = fileIO.readXML( kImageName + ".xml", kDir, false, false );
                //System.err.println( "Reading file " + kImageName );
            }
            if ( kImageGM == null )
            {
                if ( m_iTimeSteps == 1 )
                {
                    kImageGM = (ModelImage)kImage.clone();
                }
                else
                {
                    kImageGM = (ModelImage)m_akImages[i].clone();
                }
                JDialogGradientMagnitude kCalcMagnitude = new JDialogGradientMagnitude( null, kImageGM );
                kCalcMagnitude.setVisible(false);
                kCalcMagnitude.setOutputNewImage( false );
                kCalcMagnitude.setDisplayProgressBar( false );
                kCalcMagnitude.setSeparateThread( false );
                kCalcMagnitude.actionPerformed( new ActionEvent(this, 0, "OK" ) );
                kCalcMagnitude = null;     
                if ( kImageGM != null )
                {
                    //System.err.println( "...writing file " + kImageName );
                    kImageGM.saveImage( kDir, kImageName, FileUtility.XML, false, false );
                }
            }
            if ( kImageGM == null )
            {
                System.err.println( "Gradient magnitude calculation returned null" );
                m_kVolumeGM[i] = VolumeImage.UpdateData( kImage, i, null, null, m_kVolumeGMTarget, kImageName, true );
            }
            else {
                kImageGM.calcMinMax();
                m_fGMMin[i] = (float)kImageGM.getMin();
                m_fGMMax[i] = (float)kImageGM.getMax();
                m_kVolumeGM[i] = VolumeImage.UpdateData( kImageGM, 0, null, null, m_kVolumeGMTarget, kImageName, true );
            }
            ViewJFrameImage kImageFrame = ViewUserInterface.getReference().getFrameContainingImage(kImageGM);
            if ( kImageFrame != null )
            {
                kImageFrame.close();
            }
            else if ( kImageGM != null )
            {
                kImageGM.disposeLocal();
                kImageGM = null;
            }
        }
        m_kVolumeGMTarget = new Texture();
        m_kVolumeGMTarget.SetImage(m_kVolumeGM[0]);
        m_kVolumeGMTarget.SetShared(true);
        m_kVolumeGMTarget.SetFilterType(Texture.FilterType.LINEAR);
        m_kVolumeGMTarget.SetWrapType(0,Texture.WrapType.CLAMP_BORDER);
        m_kVolumeGMTarget.SetWrapType(1,Texture.WrapType.CLAMP_BORDER);
        m_kVolumeGMTarget.SetWrapType(2,Texture.WrapType.CLAMP_BORDER);
        if ( kProgress != null ) { kProgress.updateValueImmed( kProgress.getValue() + iProgress ); }

        if ( kProgress != null ) { kProgress.setMessage("Creating VolumeImage Laplacian..."); }
        for ( int i = 0; i < m_iTimeSteps; i++ )
        {
            String kImageName = ModelImage.makeImageName( kImage.getFileInfo(0).getFileName(), 
                    new String ( "_Laplacian_" + i ) );
            ModelImage kImageGMGM = null;            
            if ( !bCreate )
            {
                FileIO fileIO = new FileIO();
                kImageGMGM = fileIO.readXML( kImageName + ".xml", kDir, false, false );
                //System.err.println( "Reading file " + kImageName );
            }
            if ( kImageGMGM == null )
            {
                if ( m_iTimeSteps == 1 )
                {
                    kImageGMGM = (ModelImage)kImage.clone();
                }
                else
                {
                    kImageGMGM = (ModelImage)m_akImages[i].clone();
                }    
                JDialogLaplacian kCalcLaplacian = new JDialogLaplacian( null, kImageGMGM );
                kCalcLaplacian.setVisible(false);
                kCalcLaplacian.setOutputNewImage( false );
                kCalcLaplacian.setDisplayProgressBar(false);
                kCalcLaplacian.setSeparateThread( false );
                kCalcLaplacian.actionPerformed( new ActionEvent(this, 0, "OK" ) );
                if ( kImageGMGM != null )
                {
                    kImageGMGM.calcMinMax();
                    AlgorithmChangeType changeTypeAlgo = null;          
                    if ( kImageGMGM.isColorImage() )
                    {
                        changeTypeAlgo = new AlgorithmChangeType(kImageGMGM, ModelStorageBase.ARGB, 
                                kImageGMGM.getMin(), kImageGMGM.getMax(), 
                                0, 255, false);
                    }
                    else
                    {
                        changeTypeAlgo = new AlgorithmChangeType(kImageGMGM, ModelStorageBase.UBYTE, 
                                kImageGMGM.getMin(), kImageGMGM.getMax(), 
                                0, 255, false);
                    }
                    changeTypeAlgo.setRunningInSeparateThread(false);
                    changeTypeAlgo.run();
                    kImageGMGM.saveImage( kDir, kImageName, FileUtility.XML, false, false );
                    //System.err.println( "...writing file " + kImageName );
                }
            }
            if ( kImageGMGM != null )
            {            
                m_kVolumeGMGM[i] = VolumeImage.UpdateData( kImageGMGM, 0, null, null, m_kVolumeGMGMTarget, kImageName, true );
            }
            else
            {
                System.err.println( "Laplacian calculation returned null" );
                m_kVolumeGMGM[i] = new GraphicsImage(GraphicsImage.FormatMode.IT_RGBA8888,
                        kImage.getExtents()[0],kImage.getExtents()[1],kImage.getExtents()[2],(byte[])null,
                        new String("VolumeImageGMGM"+kPostfix+kPostfix));
            }
            ViewJFrameImage kImageFrame = ViewUserInterface.getReference().getFrameContainingImage(kImageGMGM);
            if ( kImageFrame != null )
            {
                kImageFrame.close();
            }
            else if ( kImageGMGM != null )
            {
                kImageGMGM.disposeLocal();
                kImageGMGM = null;
            }
        }
        if ( kProgress != null ) { kProgress.updateValueImmed( kProgress.getValue() + iProgress ); }
/*
        m_kVolumeGMGM[0] = new GraphicsImage(GraphicsImage.FormatMode.IT_RGBA8888,
                kImage.getExtents()[0],kImage.getExtents()[1],kImage.getExtents()[2],(byte[])null,
                new String("VolumeImageGMGM"+kPostfix+kPostfix));
                */
        m_kVolumeGMGMTarget = new Texture();
        m_kVolumeGMGMTarget.SetImage(m_kVolumeGMGM[0]);
        m_kVolumeGMGMTarget.SetShared(true);
        m_kVolumeGMGMTarget.SetFilterType(Texture.FilterType.LINEAR);
        m_kVolumeGMGMTarget.SetWrapType(0,Texture.WrapType.CLAMP_BORDER);
        m_kVolumeGMGMTarget.SetWrapType(1,Texture.WrapType.CLAMP_BORDER);
        m_kVolumeGMGMTarget.SetWrapType(2,Texture.WrapType.CLAMP_BORDER);


    }
    
    /**
     * Create a new LUT for the input image.
     * @param kImage ModelImage.
     */
    private void initLUT( )
    {

        if (m_kImage.isColorImage()) {    
            float[] x = new float[4];
            float[] y = new float[4];
            Dimension dim = new Dimension(256, 256);

            // Set ModelRGB min max values;
            x[0] = 0;
            y[0] = dim.height - 1;

            x[1] = 255 * 0.333f;
            y[1] = (dim.height - 1) - ((dim.height - 1) / 3.0f);

            x[2] = 255 * 0.667f;
            y[2] = (dim.height - 1) - ((dim.height - 1) * 0.67f);

            x[3] = 255;
            y[3] = 0;

            int[] RGBExtents = new int[2];
            RGBExtents[0] = 4;
            RGBExtents[1] = 256;
            m_kRGBT = new ModelRGB(RGBExtents);
            m_kRGBT.getRedFunction().importArrays(x, y, 4);
            m_kRGBT.getGreenFunction().importArrays(x, y, 4);
            m_kRGBT.getBlueFunction().importArrays(x, y, 4);
            m_kRGBT.makeRGB(-1);
        } else {
            int[] dimExtentsLUT = new int[2];

            dimExtentsLUT[0] = 4;
            dimExtentsLUT[1] = 256;

            m_kLUT = new ModelLUT(ModelLUT.GRAY, 256, dimExtentsLUT);

            float min, max;

            if (m_kImage.getType() == ModelStorageBase.UBYTE) {
                min = 0;
                max = 255;
            } else if (m_kImage.getType() == ModelStorageBase.BYTE) {
                min = -128;
                max = 127;
            } else {
                min = (float) m_kImage.getMin();
                max = (float) m_kImage.getMax();
            }

            float imgMin = (float) m_kImage.getMin();
            float imgMax = (float) m_kImage.getMax();

            m_kLUT.resetTransferLine(min, imgMin, max, imgMax);
        }
    }
    
    
    /**
     * Initialize the scale factors. Based on the ModelImage Volume.
     */
    private void InitScale()
    {
        float fMaxX = (m_kImage.getExtents()[0] - 1) * m_kImage.getFileInfo(0).getResolutions()[0];
        float fMaxY = (m_kImage.getExtents()[1] - 1) * m_kImage.getFileInfo(0).getResolutions()[1];
        float fMaxZ = (m_kImage.getExtents()[2] - 1) * m_kImage.getFileInfo(0).getResolutions()[2];

        float fMax = fMaxX;
        if (fMaxY > fMax) {
            fMax = fMaxY;
        }
        if (fMaxZ > fMax) {
            fMax = fMaxZ;
        }
        m_fX = fMaxX/fMax;
        m_fY = fMaxY/fMax;
        m_fZ = fMaxZ/fMax;
    }
    
    /**
     * Reads an image from disk.
     * @param kImageName image name
     * @param kDir directory
     * @return ModelImage
     */
    private ModelImage ReadFromDisk( String kImageName, String kDir )
    {
        FileIO fileIO = new FileIO();
        //return fileIO.readImage( kImageName + ".xml", kDir );
        return fileIO.readXML( kImageName + ".xml", kDir, false, false );
    }
    

    /**
     * Reconfigures the ModelImage for GPU rendering. First resizes the image to power-of-two dimensions, then converts the image to unsigned byte.
     * @param kImage input image.
     * @param kImageName image name.
     * @param kDir directory for writing result image
     * @param iFilterType filter type for re-sampling image
     * @param aiExtents target power-of-two extents.
     */
    private void ReconfigureImage(ModelImage kImage, String kImageName, String kDir, int iFilterType, int[] aiExtents)
    {
        
        int[] extents = kImage.getExtents();
        float[] res = kImage.getFileInfo(0).getResolutions();
        
        float[] newRes = res.clone();
        int[] volExtents = extents.clone();
        
        
        // Checking to see if the image has all dimensions that are a power of 2.
        for (int i = 0; i < 3; i++) {
            if ( aiExtents != null )
            {
                volExtents[i] = aiExtents[i];
            }
            else
            {
                volExtents[i] = MipavMath.dimPowerOfTwo(extents[i]);
            }
            newRes[i] = (res[i] * extents[i]) / volExtents[i];
        }

        ModelImage kNewRes = kImage;
        if ( (extents[0] != volExtents[0]) || (extents[1] != volExtents[1]) || (extents[2] != volExtents[2]))
        { 
            AlgorithmTransform transformFunct = new AlgorithmTransform(kImage, new TransMatrix(4), iFilterType, newRes[0], newRes[1],
                                                    newRes[2], volExtents[0], volExtents[1], volExtents[2], false,
                                                    true, false);

            transformFunct.setRunningInSeparateThread(false);
            transformFunct.run();

            if (transformFunct.isCompleted() == false) {

                // What to do
                transformFunct.finalize();
                transformFunct = null;
            }

            kNewRes = transformFunct.getTransformedImage();
            kNewRes.calcMinMax();

            transformFunct.disposeLocal();
            transformFunct = null;
        }
        
        
        ModelImage kNewType = null;    
        if ( (kNewRes.getType() != ModelStorageBase.UBYTE) ||
                (kNewRes.isColorImage() && (kNewRes.getType() != ModelStorageBase.ARGB)) )
        {
            AlgorithmChangeType changeTypeAlgo = null;
            if ( kNewRes.isColorImage() )
            {
                kNewType = new ModelImage( ModelStorageBase.ARGB, kNewRes.getExtents(), kImageName );
                changeTypeAlgo = new AlgorithmChangeType( kNewType, kNewRes, 
                        kNewRes.getMin(), kNewRes.getMax(), 
                        0, 255, false);
            }
            else
            {
                kNewType = new ModelImage(  ModelStorageBase.UBYTE, kNewRes.getExtents(), kImageName );
                changeTypeAlgo = new AlgorithmChangeType( kNewType, kNewRes, 
                        kNewRes.getMin(), kNewRes.getMax(), 
                    0, 255, false);
            }
            changeTypeAlgo.setRunningInSeparateThread(false);
            changeTypeAlgo.run();
            changeTypeAlgo.finalize();
            changeTypeAlgo = null;
        }
        
        if ( (kNewType == null) && (kNewRes == kImage) )
        {
            m_kImage = (ModelImage)kImage.clone();
        }
        else
        {
            m_kImage = (kNewType == null) ? kNewRes : kNewType;
        }

        m_kImage.copyFileTypeInfo(kImage);
        FileInfoBase[] fileInfoBases = m_kImage.getFileInfo();
        for (int i=0;i<fileInfoBases.length;i++) {
            fileInfoBases[i].setResolutions(newRes);
        }
        m_kImage.calcMinMax();
        m_kImage.setImageName(  kImageName );
        m_kImage.saveImage( kDir, kImageName, FileUtility.XML, false, false );
    }
    
    /**
     * Go to the next 3D volume sub-image for the 4D animation. Updates the image data, and supporting images.
     */
    private void update4D()
    {
        m_kVolumeTarget.SetImage(m_kVolume[m_iTimeSlice]);
        m_kVolumeGMTarget.SetImage(m_kVolumeGM[m_iTimeSlice]);
        m_kVolumeGMGMTarget.SetImage(m_kVolumeGMGM[m_iTimeSlice]);
        m_kNormalMapTarget.SetImage(m_kNormal[m_iTimeSlice]);
        m_kHistoTarget.SetImage( m_kHisto[m_iTimeSlice] );
        
        m_kVolumeTarget.Reload(true);
        m_kVolumeGMTarget.Reload(true);
        m_kVolumeGMGMTarget.Reload(true);
        m_kNormalMapTarget.Reload(true);
        m_kHistoTarget.Reload(true);

        m_kImage.setTimeSlice(m_iTimeSlice);
    }

    /**
     * Update the opacity transfer function.
     * @param kImage the ModelImage the transfer function applies to.
     * @param kOpacityTexture the opacity Texture passed to the GPU
     * @param kOpacityMap the opacity data stored in the GraphicsImage
     * @param kTransfer the new transfer function.
     */
    private boolean UpdateImages(ModelImage kImage, Texture kOpacityTexture,
                                 GraphicsImage kOpacityMap, TransferFunction kTransfer)
    {
         int iLutHeight = 256;
         float[] afData = kOpacityMap.GetFloatData();

         float fRange = (float)(kImage.getMax() - kImage.getMin());
         float fStep = fRange / iLutHeight;
         float fDataValue = (float)kImage.getMin();
         for (int i = 0; i < iLutHeight; i++) {
             afData[i] = (kTransfer.getRemappedValue( fDataValue, iLutHeight )/255.0f);
             fDataValue += fStep;
         }
         kOpacityTexture.Reload(true);
         return true;
    }
    
    /**
     * Update the opacity transfer function.
     * @param kImage the ModelImage the transfer function applies to.
     * @param kOpacityTexture the opacity Texture passed to the GPU
     * @param kOpacityMap the opacity data stored in the GraphicsImage
     * @param kTransfer the new transfer function.
     */
    private boolean UpdateImages2(ModelImage kImage, Texture kOpacityTexture,
                                  GraphicsImage kOpacityMap, TransferFunction kTransfer)
    {
         int iLutHeight = kOpacityMap.GetBound(0);
         byte[] abData = kOpacityMap.GetData();

         float fRange = (float)(kImage.getMax() - kImage.getMin());
         float fStep = fRange / iLutHeight;
         float fDataValue = (float)kImage.getMin();
         float fVal;
         for (int i = 0; i < iLutHeight; i++) {
             fVal = (kTransfer.getRemappedValue( fDataValue, iLutHeight )/255.0f);
             abData[i*4 + 3] = (byte)(fVal*255);
             fDataValue += fStep;
         }
         kOpacityTexture.Reload(true);
         return true;
    }

    private void writeObject(java.io.ObjectOutputStream out)
    throws IOException 
    {
        if ( m_kImage != null )
        {
            out.writeObject( m_kDir );
            out.writeObject( m_kImage.getImageName() );
            out.writeObject( m_kPostfix );
            m_kImage.saveImage( m_kDir, m_kImage.getImageName(), FileUtility.XML, false, false );
        }
        else
        {
            out.writeObject( "null" );            
        }
    }
    
    private void readObject(java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException
    {
        m_kDir = (String)in.readObject();
        if ( !m_kDir.equals( "null" ) )
        {
            String kImageName = (String)in.readObject();
            m_kPostfix = (String)in.readObject();
            m_kImage = ReadFromDisk( kImageName, m_kDir );
            m_bCompute = false;
            init(null, 0);
        }
    }

}
