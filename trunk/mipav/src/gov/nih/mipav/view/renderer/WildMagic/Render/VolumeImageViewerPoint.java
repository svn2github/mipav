package gov.nih.mipav.view.renderer.WildMagic.Render;

import gov.nih.mipav.MipavInitGPU;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.model.structures.ModelSimpleImage;
import gov.nih.mipav.model.structures.TransMatrix;
import gov.nih.mipav.view.Preferences;

import java.awt.Frame;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import WildMagic.LibFoundation.Mathematics.ColorRGB;
import WildMagic.LibFoundation.Mathematics.ColorRGBA;
import WildMagic.LibFoundation.Mathematics.Mathf;
import WildMagic.LibFoundation.Mathematics.Matrix3f;
import WildMagic.LibFoundation.Mathematics.Matrix4f;
import WildMagic.LibFoundation.Mathematics.Vector3f;
import WildMagic.LibFoundation.Mathematics.Vector4f;
import WildMagic.LibGraphics.Effects.ShaderEffect;
import WildMagic.LibGraphics.Rendering.AlphaState;
import WildMagic.LibGraphics.Rendering.GraphicsImage;
import WildMagic.LibGraphics.Rendering.Texture;
import WildMagic.LibGraphics.Rendering.WireframeState;
import WildMagic.LibGraphics.SceneGraph.Attributes;
import WildMagic.LibGraphics.SceneGraph.Geometry;
import WildMagic.LibGraphics.SceneGraph.IndexBuffer;
import WildMagic.LibGraphics.SceneGraph.Node;
import WildMagic.LibGraphics.SceneGraph.Polypoint;
import WildMagic.LibGraphics.SceneGraph.StandardMesh;
import WildMagic.LibGraphics.SceneGraph.Transformation;
import WildMagic.LibGraphics.SceneGraph.TriMesh;
import WildMagic.LibGraphics.SceneGraph.VertexBuffer;
import WildMagic.LibGraphics.Shaders.CompiledProgramCatalog;
import WildMagic.LibGraphics.Shaders.ImageCatalog;
import WildMagic.LibGraphics.Shaders.PixelProgramCatalog;
import WildMagic.LibGraphics.Shaders.SamplerInformation;
import WildMagic.LibGraphics.Shaders.VertexProgramCatalog;
import WildMagic.LibRenderers.OpenGLRenderer.OpenGLFrameBuffer;
import WildMagic.LibRenderers.OpenGLRenderer.OpenGLRenderer;

import com.sun.opengl.util.Animator;

public class VolumeImageViewerPoint extends VolumeImageViewer
implements GLEventListener, KeyListener
{
    protected ImageReduceEffect m_akImageReduceEntropy;
    protected ImageReduceEffect[] m_akImageReduceSum;
    protected ImageReduceEffect[][] m_akImageReduceSumX;
    protected ImageReduceEffect[][] m_akImageReduceSumY;
    protected VolumeHistogramEffect m_kImageEffectDual;
    private GLAutoDrawable m_kGLAutoDrawable = null;
    
    private Polypoint m_kImagePointsDual;

    private OpenGLFrameBuffer m_kHistogramOutput;
    private OpenGLFrameBuffer[] m_akReduceOutput;
    private OpenGLFrameBuffer[][] m_akReduceOutputX;
    private OpenGLFrameBuffer[][] m_akReduceOutputY;
    
    private int m_iCount = 0;
    private AlphaState m_kAlpha;
    private ModelSimpleImage m_kImageA;
    private ModelSimpleImage m_kImageB;
    private String m_kNameA = "imageA";
    private String m_kNameB = "imageB";
    private Texture m_kTextureA;
    private Texture m_kTextureB;
    
    private double m_dHx, m_dHy, m_dHxy;
    private double m_dOverlap;
    private Matrix4f m_kImageTransform = new Matrix4f(false);

    private boolean m_bDispose = false;
    private float[] m_afJoint;
    private boolean m_bUseJoint = false;

    private boolean m_bPrint = false;
    
    public VolumeImageViewerPoint( ModelSimpleImage kImageA, ModelSimpleImage kImageB, int iNBins )
    {
        //super( Math.max( kImageA.extents[0], kImageB.extents[0]), 
        //        Math.max( kImageA.extents[1], kImageB.extents[1]));
        //super( kImageA.extents[0],kImageA.extents[1] );
        super( iNBins, iNBins );
        m_afJoint = new float[iNBins*iNBins*4];
        
        m_kImageA = kImageA;
        m_kImageB = kImageB;
        
        String kExternalDirs = MipavInitGPU.getExternalDirs();        
        ImageCatalog.SetActive( new ImageCatalog("Main", kExternalDirs) );      
        VertexProgramCatalog.SetActive(new VertexProgramCatalog("Main", kExternalDirs));       
        PixelProgramCatalog.SetActive(new PixelProgramCatalog("Main", kExternalDirs));
        CompiledProgramCatalog.SetActive(new CompiledProgramCatalog());
    }
    /**
     * @param args
     */
    public static VolumeImageViewerPoint create( ModelSimpleImage kImageA, ModelSimpleImage kImageB, boolean bShowFrame, int iNBins )
    {
        final VolumeImageViewerPoint kWorld = new VolumeImageViewerPoint(kImageA, kImageB, iNBins);
        final Frame frame = new Frame(kWorld.GetWindowTitle());
        frame.add( kWorld.GetCanvas() );
        final Animator animator = new Animator( kWorld.GetCanvas() );  
        /*
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // Run this on another thread than the AWT event queue to
                // avoid deadlocks on shutdown on some platforms
                new Thread(new Runnable() {
                    public void run() {
                        animator.stop();
                    }
                }).start();
                frame.setVisible(false);
            }
        });
        */
        // setting the frame to be undecorated removes the frame title bar and edges
        // this prevents flashing on-screen.
        frame.setUndecorated(!bShowFrame);
        // frame must be set to visible for the gl canvas to be properly initialized.
        frame.setVisible(true);
        frame.setBounds(0,0,
                kWorld.GetWidth(), kWorld.GetHeight() );
        frame.setVisible(bShowFrame);
        kWorld.SetAnimator(animator);
        kWorld.SetFrame(frame);
        //animator.start();
        //kWorld.GetCanvas().display();
        return kWorld;
    }

    public void display(GLAutoDrawable arg0) {
        if ( m_bDispose )
        {
            dispose(arg0);
            return;
        }
        if ( m_kAnimator == null )
        {
            return;
        }
        if ( !m_bInit )
        {
            init(arg0);
        }
        if ( !m_bDisplay )
        {
            return;
        }
        m_bDisplay = false;
        calcEntropy();
    }


    public void dispose()
    {
        m_bDispose = true;
        GetCanvas().display();  
    }

    public void dispose(GLAutoDrawable arg0)
    {
        //System.err.println( "VolumeImageViewerPoint dispose()" );
        if ( m_kImagePointsDual != null )
        {
            m_pkRenderer.ReleaseResources(m_kImagePointsDual);
            m_kImagePointsDual.dispose();
            m_kImagePointsDual = null;
        }
        if ( m_kTextureA != null )
        {
            m_kTextureA.GetImage().dispose();
            m_kTextureA.dispose();
            m_kTextureA = null;
        }
        if ( m_kTextureB != null )
        {
            m_kTextureB.GetImage().dispose();
            m_kTextureB.dispose();
            m_kTextureB = null;
        }
        m_spkScene = null;

        m_kImageEffectDual.dispose();
              

        m_kHistogramOutput.GetTarget(0).GetImage().dispose();
        m_kHistogramOutput.GetTarget(0).dispose();
        m_kHistogramOutput.TerminateBuffer();
        m_kHistogramOutput.dispose();

        m_pkRenderer.ReleaseResources(m_akImageReduceEntropy);
        m_akImageReduceEntropy.dispose();

        m_pkRenderer.ReleaseResources(m_pkPlane);
        m_pkPlane.dispose();   
        
        for ( int i = 0; i < (m_iCount-1); i++ )
        {

            m_akReduceOutput[i].GetTarget(0).GetImage().dispose();  
            m_akReduceOutput[i].GetTarget(0).dispose();   
            m_akReduceOutput[i].TerminateBuffer();   
            m_akReduceOutput[i].dispose();            
            m_pkRenderer.ReleaseResources(m_akImageReduceSum[i]);
            m_akImageReduceSum[i].dispose();
        }
    
        ImageCatalog.GetActive().dispose();
        VertexProgramCatalog.GetActive().dispose();     
        PixelProgramCatalog.GetActive().dispose();
        CompiledProgramCatalog.GetActive().dispose();
        
        super.dispose(arg0);
    }
    
    public double getError(TransMatrix kTransform) {
        setTransform(kTransform);
        m_bDisplay = true;
        GetCanvas().display();  
        m_bUseJoint = false;
        return m_dHxy / (m_dHx + m_dHy);
    }

    public void init(GLAutoDrawable arg0) {
        m_kGLAutoDrawable = arg0; 
        if ( m_bInit )
        {
            return;
        }
        
        arg0.setAutoSwapBufferMode( false );

        ((OpenGLRenderer)m_pkRenderer).SetDrawable( arg0 );
        ((OpenGLRenderer)m_pkRenderer).InitializeState();

        CreateScene();

        // initial update of objects
        m_spkScene.UpdateGS();
        m_spkScene.UpdateRS();


        //((OpenGLRenderer)m_pkRenderer).ClearDrawable( );
        m_bInit = true;
        m_spkCamera = null;
        m_pkRenderer.Activate();
    }
    
    public void reshape(GLAutoDrawable arg0, int iX, int iY, int iWidth, int iHeight){}
    
    public void setTransform( TransMatrix kTransform )
    {
        if ( kTransform.getDim() == 3 )
        {
            m_kImageTransform.MakeIdentity();
            m_kImageTransform.M00 = kTransform.M00;
            m_kImageTransform.M01 = kTransform.M01;
            m_kImageTransform.M02 = 0;
            m_kImageTransform.M03 = kTransform.M02;
            

            m_kImageTransform.M10 = kTransform.M10;
            m_kImageTransform.M11 = kTransform.M11;
            m_kImageTransform.M12 = 0;
            m_kImageTransform.M13 = kTransform.M12;
        }
        else
        {
            m_kImageTransform.Copy(kTransform);
        }
        if ( m_kImageEffectDual != null )
        {
            m_kImageEffectDual.SetTransform(m_kImageTransform);
        }
    }

    protected void CreateImageMesh(int iWidth, int iHeight)
    {
        Attributes kAttributes = new Attributes();
        kAttributes.SetPChannels(3);
        kAttributes.SetTChannels(0,3);
        /*
        int iVQuantity = iWidth*iHeight;
        VertexBuffer pkVB = new VertexBuffer(kAttributes,iVQuantity);

        // generate geometry
        float fInv0 = 1.0f/(iWidth - 1.0f);
        float fInv1 = 1.0f/(iHeight - 1.0f);
        for (int y = 0, i = 0; y < iHeight; y++)
        {
            for (int x = 0; x < iWidth; x++)
            {
                pkVB.SetPosition3(i, x*fInv0, y*fInv1, 0 );
                pkVB.SetTCoord3(0, i, x*fInv0, y*fInv1, 0);
                i++;
            }
        }
        m_kImagePointsDual = new Polypoint( pkVB );
        */
        StandardMesh kSM = new StandardMesh(kAttributes);
        TriMesh kMesh = kSM.Rectangle(iWidth,iHeight,1,1);
        m_kImagePointsDual = new Polypoint( kMesh.VBuffer );
        m_kAlpha = new AlphaState();
        m_kAlpha.BlendEnabled = true;
        m_kAlpha.SrcBlend = AlphaState.SrcBlendMode.SBF_ONE;
        m_kAlpha.DstBlend = AlphaState.DstBlendMode.DBF_ONE;
        m_kImagePointsDual.AttachGlobalState(m_kAlpha);
        m_kImagePointsDual.UpdateGS();
        m_kImagePointsDual.UpdateRS();    
    }
    
    protected TriMesh CreateLocalPlaneNode(int iWidth, int iHeight)
    {
        Attributes kAttributes = new Attributes();
        kAttributes.SetPChannels(3);
        kAttributes.SetTChannels(0,3);
        StandardMesh kSM = new StandardMesh(kAttributes);
        TriMesh kPlane = kSM.Rectangle(iWidth,iHeight,1,1);
        return kPlane;
    }

    
    private void CreateImageTextures()
    {
        //ModelImage kImageA = new ModelImage( m_kImageA, "newA" );
        //ModelImage kImageB = new ModelImage( m_kImageB, "newB" );
        SamplerInformation.Type eSamplerType = SamplerInformation.Type.SAMPLER_3D;
        
        m_kTextureA = new Texture();
        m_kTextureA.SetImage(VolumeImage.UpdateData(m_kImageA, 0, m_kNameA ));
        //m_kTextureA.SetImage(VolumeImage.UpdateData(kImageA, 0, null, null, null, m_kNameA, true ));
        
        
        m_kTextureA.SetShared(true);
        m_kTextureA.SetFilterType(Texture.FilterType.NEAREST);
        m_kTextureA.SetWrapType(0,Texture.WrapType.CLAMP_BORDER);
        m_kTextureA.SetWrapType(1,Texture.WrapType.CLAMP_BORDER);
        m_kTextureA.SetWrapType(2,Texture.WrapType.CLAMP_BORDER);                
        m_kTextureA.SetSamplerInformation( new SamplerInformation( m_kNameA, eSamplerType, 0, 0 ) );
        m_pkRenderer.LoadTexture( m_kTextureA );
        
        m_kTextureB = new Texture();
        m_kTextureB.SetImage(VolumeImage.UpdateData(m_kImageB, 0, m_kNameB ));
        //m_kTextureB.SetImage(VolumeImage.UpdateData(kImageB, 0, null, null, null, m_kNameB, true ));
        m_kTextureB.SetShared(true);
        m_kTextureB.SetFilterType(Texture.FilterType.NEAREST);
        m_kTextureB.SetWrapType(0,Texture.WrapType.CLAMP_BORDER);
        m_kTextureB.SetWrapType(1,Texture.WrapType.CLAMP_BORDER);
        m_kTextureB.SetWrapType(2,Texture.WrapType.CLAMP_BORDER);           
        m_kTextureB.SetSamplerInformation( new SamplerInformation( m_kNameB, eSamplerType, 0, 0 ) );
        m_pkRenderer.LoadTexture( m_kTextureB );
    }

    protected void CreateScene ()
    {           
        m_spkScene = new Node();

        m_pkPlane = CreateLocalPlaneNode(m_iWidth,m_iHeight);      
        CreateImageTextures();
        
        m_kImageEffectDual = new VolumeHistogramEffect( m_kTextureA, m_kTextureB,
                m_kImageA.min, m_kImageA.max, m_kImageB.min, m_kImageB.max, 
                m_kImageA.extents[0],m_kImageA.extents[1], m_iWidth, m_kImageTransform, true );
        
        CreateImageMesh(m_kImageA.extents[0],m_kImageA.extents[1]);
        m_kImagePointsDual.AttachEffect(m_kImageEffectDual);
        
        int iWidth = m_iWidth;
        int iHeight = m_iHeight;
        m_kHistogramOutput = CreateRenderTarget( "Histogram2D", iWidth, iHeight );

        double dSize = m_kImageA.dataSize;
        //m_akImageReduceEntropy = new ImageReduceEffect( m_kHistogramOutput.GetTarget(0), m_kImageA.extents[0], m_kImageA.extents[1], dSize, ImageReduceEffect.ENTROPY );
        m_akImageReduceEntropy = new ImageReduceEffect( m_kHistogramOutput.GetTarget(0), iWidth, iHeight, dSize, ImageReduceEffect.ENTROPY );
        
        

        int iSize = Math.min( iWidth, iHeight );
        m_iCount = (int)Mathf.Log2( iSize );

        iWidth = Math.max(iWidth/2, 1);
         iHeight = Math.max(iHeight/2, 1);
        m_akImageReduceSum = new ImageReduceEffect[m_iCount-1];
        m_akReduceOutput = new OpenGLFrameBuffer[m_iCount-1];
        for ( int i = 0; i < (m_iCount-1); i++ )
        {
            String kImageName = new String("Reduce" + iWidth + "_" + iHeight);
            m_akReduceOutput[i] = CreateRenderTarget( kImageName, iWidth, iHeight );            
            m_akImageReduceSum[i] = new ImageReduceEffect(m_akReduceOutput[i].GetTarget(0), iWidth, iHeight, 0, ImageReduceEffect.SUM );
            
            
            iWidth = Math.max(iWidth/2, 1);
            iHeight = Math.max(iHeight/2, 1);  
            if ( iWidth < 2 || iHeight < 2 )
            {
                m_iCount = i+1;
                break;
            }
        }

        setupReferenceReduce(dSize);
        setupReferenceMoving(dSize);
        
        
    }
    
    private void setupReferenceMoving(double dSize)
    {

        

        int iWidth = m_iWidth;
        int iHeight = m_iHeight;

        int iSize =  iHeight;
        int iCountY = (int)Mathf.Log2( iSize );
        //System.err.println( iSize + " " + m_iCount );

        m_akImageReduceSumY = new ImageReduceEffect[2][];
        m_akImageReduceSumY[0] = new ImageReduceEffect[iCountY+1];
        m_akReduceOutputY = new OpenGLFrameBuffer[2][];
        m_akReduceOutputY[0] = new OpenGLFrameBuffer[iCountY+1];
        
        m_akImageReduceSumY[0][0] = new ImageReduceEffect( m_kHistogramOutput.GetTarget(0), iWidth, iHeight, 0, ImageReduceEffect.SUM_YA );
        iHeight = Math.max(iHeight/2, 1);
        
        Texture kLast = null;
        for ( int i = 1; i < iCountY+1; i++ )
        {
            String kImageName = new String("Reduce2dto1D" + iWidth + "_" + iHeight + "_" + 1);
            m_akReduceOutputY[0][i] = CreateRenderTarget( kImageName, iWidth, iHeight );            
            m_akImageReduceSumY[0][i] = new ImageReduceEffect(m_akReduceOutputY[0][i].GetTarget(0), iWidth, iHeight, 0, ImageReduceEffect.SUM_YA );
            kLast = m_akReduceOutputY[0][i].GetTarget(0);

            iHeight = Math.max(iHeight/2, 1);
            if ( iHeight < 1 )
            {
                break;
            }
        }       
        

        iWidth = m_iWidth;
        iHeight = 1;

        iSize =  iWidth;
        int iCountX = (int)Mathf.Log2( iSize ) + 1;
        //System.err.println( iSize + " " + m_iCount );

        m_akImageReduceSumY[1] = new ImageReduceEffect[iCountX];
        m_akReduceOutputY[1] = new OpenGLFrameBuffer[iCountX];
        
        m_akImageReduceSumY[1][0] = new ImageReduceEffect( kLast, iWidth, iHeight, dSize, ImageReduceEffect.ENTROPY_X );
        iWidth = Math.max(iWidth/2, 1);
        
        for ( int i = 1; i < iCountX; i++ )
        {
            String kImageName = new String("Reduce2dto1D" + iWidth + "_" + iHeight + "_" + 1);
            m_akReduceOutputY[1][i] = CreateRenderTarget( kImageName, iWidth, iHeight );            
            m_akImageReduceSumY[1][i] = new ImageReduceEffect(m_akReduceOutputY[1][i].GetTarget(0), iWidth, iHeight, 0, ImageReduceEffect.SUM_XA );

            iWidth = Math.max(iWidth/2, 1);
            if ( iWidth < 4 )
            {
                break;
            }
        }
        
        
    }
    
    
    private void setupReferenceReduce(double dSize)
    {

        

        int iWidth = m_iWidth;
        int iHeight = m_iHeight;

        int iSize =  iWidth;
        int iCountX = (int)Mathf.Log2( iSize );
        //System.err.println( iSize + " " + m_iCount );

        m_akImageReduceSumX = new ImageReduceEffect[2][];
        m_akImageReduceSumX[0] = new ImageReduceEffect[iCountX+1];
        m_akReduceOutputX = new OpenGLFrameBuffer[2][];
        m_akReduceOutputX[0] = new OpenGLFrameBuffer[iCountX+1];
        
        m_akImageReduceSumX[0][0] = new ImageReduceEffect( m_kHistogramOutput.GetTarget(0), iWidth, iHeight, 0, ImageReduceEffect.SUM_XA );
        iWidth = Math.max(iWidth/2, 1);
        Texture kLast = null;
        for ( int i = 1; i < iCountX+1; i++ )
        {
            String kImageName = new String("Reduce2dto1D" + iWidth + "_" + iHeight + "_" + 1);
            m_akReduceOutputX[0][i] = CreateRenderTarget( kImageName, iWidth, iHeight );            
            m_akImageReduceSumX[0][i] = new ImageReduceEffect(m_akReduceOutputX[0][i].GetTarget(0), iWidth, iHeight, 0, ImageReduceEffect.SUM_XA );
            kLast = m_akReduceOutputX[0][i].GetTarget(0);

            iWidth = Math.max(iWidth/2, 1);
            if ( iWidth < 1 )
            {
                break;
            }
        }       
        

        iWidth = 1;
        iHeight = m_iHeight;

        iSize =  iHeight;
        int iCountY = (int)Mathf.Log2( iSize ) + 1;
        //System.err.println( iSize + " " + m_iCount );

        m_akImageReduceSumX[1] = new ImageReduceEffect[iCountY];
        m_akReduceOutputX[1] = new OpenGLFrameBuffer[iCountY];
        
        m_akImageReduceSumX[1][0] = new ImageReduceEffect( kLast, iWidth, iHeight, dSize, ImageReduceEffect.ENTROPY_Y );
        iHeight = Math.max(iHeight/2, 1);
        
        for ( int i = 1; i < iCountY; i++ )
        {
            String kImageName = new String("Reduce2dto1D" + iWidth + "_" + iHeight + "_" + 1);
            m_akReduceOutputX[1][i] = CreateRenderTarget( kImageName, iWidth, iHeight );            
            m_akImageReduceSumX[1][i] = new ImageReduceEffect(m_akReduceOutputX[1][i].GetTarget(0), iWidth, iHeight, 0, ImageReduceEffect.SUM_YA );

            iHeight = Math.max(iHeight/2, 1);
            if ( iHeight < 4 )
            {
                break;
            }
        }
        
        
    }
    
    private void calcEntropy()
    {        
        calcEntropy( m_kImageA, m_kImageA.dataSize ); 

        int nVoxels = m_kImageA.dataSize;
        if ( ( (m_kImageA.nDims < 3) && (m_dOverlap > 1000) ) ||
                ( (m_kImageA.nDims == 3) && (m_dOverlap > (0.15 * nVoxels)) ) ) {
            double nRatio = ((double) nVoxels) / m_dOverlap;

            m_dHx  = (nRatio * m_dHx) - Math.log(nRatio);
            m_dHy  = (nRatio * m_dHy) - Math.log(nRatio);
            m_dHxy = (nRatio * m_dHxy) - Math.log(nRatio);
            if ( m_bPrint )
            {
                System.err.println( "GPU: " + m_kImageA.dataSize + " "  + m_dHx + " " + m_dHy + " " + m_dHxy + " " + m_dOverlap );
            }

        } else {
            m_dHx = Math.log(nVoxels);
            m_dHy = Math.log(nVoxels);
            m_dHxy = 2.0 * Math.log(nVoxels);
            if ( m_bPrint )
            {
                System.out.println("nOvelap not high enough, less than 15% of voxels.");
                System.err.println( "GPU: " + m_kImageA.dataSize + " "  + m_dHx + " " + m_dHy + " " + m_dHxy + " " + m_dOverlap );
            }
        }
    }
    
    
    private long m_lStartTime, m_lEstimatedTime;
    private void calcEntropy( ModelSimpleImage kImage, double dNumSamples )
    {
        if ( m_bUseJoint )
        {
            Texture kTarget = m_kHistogramOutput.GetTarget(0);
            kTarget.GetImage().SetData( m_afJoint, m_iWidth, m_iHeight);
            kTarget.Release();
            m_pkRenderer.LoadTexture( kTarget );
        }
        else
        {
            //m_lStartTime = System.nanoTime();        
            m_kHistogramOutput.Enable();
            m_pkRenderer.SetBackgroundColor(new ColorRGBA(0,0,0,0));
            m_pkRenderer.ClearBuffers();
            int iZExtents = (kImage.nDims == 3) ? kImage.extents[2]: 1;
            for ( int i = iZExtents-1; i >= 0; i-- )
                //for ( int i = 0; i < iZExtents; i++ )
            {
                if ( iZExtents == 1 )
                {
                    m_kImageEffectDual.ZSlice( 0f, 0.5f ); 
                }
                else
                {
                    m_kImageEffectDual.ZSlice( (float)i / (float)(iZExtents), (1.0f/(float)((iZExtents)*2.0)) );
                }
                //if ( i == 0 )
                {
                    m_pkRenderer.Draw(m_kImagePointsDual);
                    //writeImage();
                }
            }
            m_kHistogramOutput.Disable();
        }
        //m_pkRenderer.DisplayBackBuffer();
        ReduceDualA(dNumSamples);
    }
    
    private void printTarget( String kMsg, Texture kTarget )
    {
        m_pkRenderer.GetTexImage( kTarget );
        float[] afData = kTarget.GetImage().GetFloatData();
        System.err.println( kMsg + "   TEXTURE NAME = " + kTarget.GetName() + " size = " +
                kTarget.GetImage().GetBound(0) + " " + kTarget.GetImage().GetBound(1));
        int iSum = 0;
        float fMax = 0, fMin = 1;
        for ( int i = 0; i < afData.length; i+=4 )
        {
            //if ( afData[i] != 0.0f )
            {
                System.err.println( i + " " + afData[i] );
            }
            if ( afData[i] > fMax )
            {
                fMax = afData[i];
            }
            if ( afData[i] < fMin )
            {
                fMin = afData[i];
            }
            iSum += afData[i+3];
        }
        System.err.println( "TOTAL: " + iSum + " " + fMin + " " + fMax );
    }
    
    
    private void calcCPU( Texture kTarget, double dNumSamples )
    {
        double dSize = m_kImageA.dataSize;
        m_pkRenderer.GetTexImage( kTarget );
        float[] afData = kTarget.GetImage().GetFloatData();
        int iWidth = kTarget.GetImage().GetBound(0);
        int iHeight = kTarget.GetImage().GetBound(1);
        float[][] af2DData = new float[iWidth][iHeight];
        float[] aXSum = new float[iHeight];
        float[] aYSum = new float[iWidth];
        float fXEntropy = 0;
        float fYEntropy = 0;
        int iSum = 0;
        for ( int i = 0; i < iWidth; i++ )
        {
            for ( int j = 0; j < iHeight; j++ )
            {
                af2DData[i][j] = afData[4*(i*iHeight + j)];
                iSum += af2DData[i][j];
            }
        }
/*
        for ( int i = 0; i < iWidth; i++ )
        {
            aYSum[i] = 0;
            for ( int j = 0; j < iHeight; j++ )
            {
                aYSum[i] += af2DData[i][j];
            }
            if ( aYSum[i] != 0 )
            {
                System.err.print( i + " " + aYSum[i] + " " );
                aYSum[i] = (float)(-aYSum[i] * (Math.log(aYSum[i]) -  Math.log(dSize) ));
                System.err.println( aYSum[i] );
                fXEntropy += aYSum[i];
            }
        }
        System.err.println( "" );
        System.err.println( "" );
        System.err.println( "" );
        System.err.println( "" );
        for ( int i = 0; i < iHeight; i++ )
        {
            aXSum[i] = 0;
            for ( int j = 0; j < iWidth; j++ )
            {
                aXSum[i] += af2DData[j][i];
            }

            if ( aXSum[i] != 0 )
            {
                System.err.print( i + " " + aXSum[i] + " " );
                aXSum[i] = (float)(-aXSum[i] * (Math.log(aXSum[i]) -  Math.log(dSize) ));
                System.err.println( aXSum[i] );
                fYEntropy += aXSum[i];
            }
        }
        fXEntropy /= dNumSamples;
        fYEntropy /= dNumSamples;
        System.err.println( "" );
        System.err.println( "" );
        System.err.println( "" );
        System.err.println( "" );
        System.err.println( fXEntropy + " " + fYEntropy );
        System.err.println( "" );
        System.err.println( "" );
        System.err.println( "" );
        System.err.println( "" );
        */
        System.err.println( iSum );
    }
    
    private void ReduceDualA( double dNumSamples )
    {
        Texture kTarget = null;

        kTarget = m_akImageReduceSumX[0][0].GetTexture(0, 0);
        //printTarget( "Reading", kTarget );
        
        for ( int i = 0; i < 2; i++ )
        {
            kTarget = m_akImageReduceSumX[i][0].GetTexture(0, 0);
            if ( i == 1 )
            {
                //printTarget( "Reading", kTarget );
            }


            m_pkPlane.DetachAllEffects();
            m_pkPlane.AttachEffect( m_akImageReduceSumX[i][0] );
            m_pkPlane.UpdateGS();
            //System.err.println( "Reading " + kTarget.GetName() );
            //System.err.println( "Effect " + m_akImageReduceSumX[i][0].GetName() );
            for ( int iTarget = 1; iTarget < m_akReduceOutputX[i].length; iTarget++ )
            {                    
                //System.err.println( "Writing " + m_akReduceOutputX[i][iTarget].GetTarget(0).GetName() );
                m_akReduceOutputX[i][iTarget].Enable();
                m_pkRenderer.SetBackgroundColor(ColorRGBA.BLACK);
                m_pkRenderer.ClearBuffers();
                m_pkRenderer.Draw(m_pkPlane);
                m_akReduceOutputX[i][iTarget].Disable();
                kTarget = m_akReduceOutputX[i][iTarget].GetTarget(0);


                //printTarget( new String("Round" + iTarget), kTarget );


                m_pkPlane.DetachAllEffects();
                if ( (iTarget+1 < m_akReduceOutputX[i].length) && (m_akImageReduceSumX[i][iTarget+1] != null) )
                {
                    m_pkPlane.AttachEffect( m_akImageReduceSumX[i][iTarget] );
                    m_pkPlane.UpdateGS();
                   // System.err.println( "Reading " + m_akImageReduceSumX[i][iTarget].GetTexture(0, 0).GetName() );
                    //System.err.println( "Effect " + m_akImageReduceSumX[i][iTarget].GetName() );
                }
                else
                {
                    break;
                }
            }
        }

        //printTarget( "Result", kTarget );
        double fEntropyX = 0;
        double fOverlapX = 0;
        if ( kTarget != null )
        {
            m_pkRenderer.GetTexImage( kTarget );
            float[] afData = kTarget.GetImage().GetFloatData();
            for ( int i = 0; i < afData.length; i+=4 )
            {
                fEntropyX += afData[i];
                fOverlapX += afData[i+1];
            }
        }            
        //System.err.println( "Entropy = " + fEntropyX );
        fEntropyX/=dNumSamples;
        //System.err.println( "Entropy = " + fEntropyX );
        
        

        
        
        
        
        
        
        
        

        for ( int i = 0; i < 2; i++ )
        {
            kTarget = m_akImageReduceSumY[i][0].GetTexture(0, 0);
            //printTarget( "Reading", kTarget );

            m_pkPlane.DetachAllEffects();
            m_pkPlane.AttachEffect( m_akImageReduceSumY[i][0] );
            m_pkPlane.UpdateGS();
            //System.err.println( "Reading " + kTarget.GetName() );
            //System.err.println( "Effect " + m_akImageReduceSumY[i][0].GetName() );
            for ( int iTarget = 1; iTarget < m_akReduceOutputY[i].length; iTarget++ )
            {                    
                //System.err.println( "Writing " + m_akReduceOutputY[i][iTarget].GetTarget(0).GetName() );
                m_akReduceOutputY[i][iTarget].Enable();
                m_pkRenderer.SetBackgroundColor(ColorRGBA.BLACK);
                m_pkRenderer.ClearBuffers();
                m_pkRenderer.Draw(m_pkPlane);
                m_akReduceOutputY[i][iTarget].Disable();
                kTarget = m_akReduceOutputY[i][iTarget].GetTarget(0);


                //printTarget( new String("Round" + iTarget), kTarget );


                m_pkPlane.DetachAllEffects();
                if ( (iTarget+1 < m_akReduceOutputY[i].length) && (m_akImageReduceSumY[i][iTarget+1] != null) )
                {
                    m_pkPlane.AttachEffect( m_akImageReduceSumY[i][iTarget] );
                    m_pkPlane.UpdateGS();
                    //System.err.println( "Reading " + m_akImageReduceSumY[i][iTarget].GetTexture(0, 0).GetName() );
                    //System.err.println( "Effect " + m_akImageReduceSumY[i][iTarget].GetName() );
                }
                else
                {
                    break;
                }
            }
        }

        //printTarget( "Result", kTarget );
        double fEntropyY = 0;
        double fOverlapY = 0;
        if ( kTarget != null )
        {
            m_pkRenderer.GetTexImage( kTarget );
            float[] afData = kTarget.GetImage().GetFloatData();
            for ( int i = 0; i < afData.length; i+=4 )
            {
                fEntropyY += afData[i];
                fOverlapY += afData[i+1];
            }
        }            
        //System.err.println( "Entropy = " + fEntropyY );
        fEntropyY/=dNumSamples;
        //System.err.println( "Entropy = " + fEntropyY );
        
        

        
        
        
        
        
        
        
        
        
        
        m_pkPlane.DetachAllEffects();
        m_pkPlane.AttachEffect( m_akImageReduceEntropy );
        m_pkPlane.UpdateGS();
        for ( int iTarget = 0; iTarget < (m_iCount-1); iTarget++ )
        {                    
            m_akReduceOutput[iTarget].Enable();
            m_pkRenderer.SetBackgroundColor(ColorRGBA.BLACK);
            m_pkRenderer.ClearBuffers();
            m_pkRenderer.Draw(m_pkPlane);
            m_akReduceOutput[iTarget].Disable();
            kTarget = m_akReduceOutput[iTarget].GetTarget(0);
            m_pkPlane.DetachAllEffects();
            m_pkPlane.AttachEffect( m_akImageReduceSum[iTarget] );
            m_pkPlane.UpdateGS();
        }

        //m_pkRenderer.Finish();
        //m_lEstimatedTime = System.nanoTime() - m_lStartTime;
        //System.err.println( "reduce =   " + m_lEstimatedTime );
        double dEntropyMoving = 0;
        double dEntropyDual = 0;
        double dOverlap = 0;
        if ( kTarget != null )
        {
            int iIndex = 0;
            int iStep = (int)Math.max( 1.0, (int)(kTarget.GetImage().GetBytesPerPixel()/4.0f) );
            //m_lStartTime = System.nanoTime();
            m_pkRenderer.GetTexImage( kTarget );
            //m_pkRenderer.Finish();
            //m_lEstimatedTime = System.nanoTime() - m_lStartTime;
            //System.err.println( "GetTexImage =     " + m_lEstimatedTime );
            for ( int i = 0; i < kTarget.GetImage().GetBound(1); i++  )
            {
                for ( int j = 0; j < kTarget.GetImage().GetBound(0); j++  )
                {
                    dEntropyMoving += kTarget.GetImage().GetFloatData()[iIndex];
                    dOverlap += kTarget.GetImage().GetFloatData()[iIndex+1];
                    //dEntropyDual += kTarget.GetImage().GetFloatData()[iIndex+2];
                    dEntropyDual += kTarget.GetImage().GetFloatData()[iIndex];
                    //System.err.println( i + " " + j + " " + dEntropyMoving + " " + dOverlap + " " + dEntropyDual );
                    iIndex += iStep;
                }
            }
        }            
        
        m_dOverlap = fOverlapX;
        m_dHx = fEntropyX;
        m_dHy = fEntropyY;
        m_dHxy = dEntropyDual/dNumSamples;
        //System.err.println( "GPU: " + m_iWidth + " " + dNumSamples + " " + m_dOverlap + " " + m_dHx + " " + m_dHy + " " + m_dHxy );
    }
    
    

    private OpenGLFrameBuffer CreateSingleRenderTarget( String kImageName, int iWidth, int iHeight )
    {        
        float[] afData = new float[iWidth*iHeight];
        GraphicsImage pkSceneImage = new GraphicsImage(GraphicsImage.FormatMode.IT_L32F,iWidth,iHeight,afData,
                kImageName);
        Texture[] akSceneTarget = new Texture[1];
        akSceneTarget[0] = new Texture();
        akSceneTarget[0].SetImage(pkSceneImage);
        akSceneTarget[0].SetShared(true);
        akSceneTarget[0].SetFilterType(Texture.FilterType.NEAREST);
        akSceneTarget[0].SetWrapType(0,Texture.WrapType.CLAMP);
        akSceneTarget[0].SetWrapType(1,Texture.WrapType.CLAMP);
        akSceneTarget[0].SetSamplerInformation( new SamplerInformation( kImageName, SamplerInformation.Type.SAMPLER_2D, 0, 0 ) );
        m_pkRenderer.LoadTexture( akSceneTarget[0] );
        
        return new OpenGLFrameBuffer(m_eFormat,m_eDepth,m_eStencil,
                m_eBuffering,m_eMultisampling,m_pkRenderer,akSceneTarget,m_kGLAutoDrawable,0);
    }
    
    public void setJoint( float[] afJoint )
    {
        for ( int i = 0; i < afJoint.length; i++ )
        {
            m_afJoint[i] = afJoint[i];
        }
        m_bUseJoint = true;
    }
    
    public void Print( boolean bPrint )
    {
        m_bPrint = bPrint;
    }

    private OpenGLFrameBuffer CreateRenderTarget( String kImageName, int iWidth, int iHeight )
    {      
        float[] afData = new float[iWidth*iHeight*4];
        GraphicsImage pkSceneImage = new GraphicsImage(GraphicsImage.FormatMode.IT_RGBA32,iWidth,iHeight,afData,
                kImageName);

        Texture[] akSceneTarget = new Texture[1];
        akSceneTarget[0] = new Texture();
        akSceneTarget[0].SetImage(pkSceneImage);
        akSceneTarget[0].SetShared(false);
        akSceneTarget[0].SetFilterType(Texture.FilterType.NEAREST);
        akSceneTarget[0].SetWrapType(0,Texture.WrapType.CLAMP);
        akSceneTarget[0].SetWrapType(1,Texture.WrapType.CLAMP);
        akSceneTarget[0].SetSamplerInformation( new SamplerInformation( kImageName, SamplerInformation.Type.SAMPLER_2D, 0, 0 ) );
        m_pkRenderer.LoadTexture( akSceneTarget[0] );
        
        return new OpenGLFrameBuffer(m_eFormat,m_eDepth,m_eStencil,
                m_eBuffering,m_eMultisampling,m_pkRenderer,akSceneTarget,m_kGLAutoDrawable,0);
    }
}