package gov.nih.mipav.view.renderer.WildMagic;

import javax.media.opengl.*;
import gov.nih.mipav.view.WildMagic.LibFoundation.Mathematics.*;
import gov.nih.mipav.view.WildMagic.LibGraphics.Effects.*;
import gov.nih.mipav.view.WildMagic.LibGraphics.Rendering.*;
import gov.nih.mipav.view.WildMagic.LibGraphics.SceneGraph.*;
import gov.nih.mipav.view.WildMagic.LibRenderers.OpenGLRenderer.*;

public class VolumeRayCast extends VolumeObject
{

    public VolumeRayCast( VolumeImage kImageA )
    {
        super(kImageA);
    }

    public void PreRender( Renderer kRenderer, Culler kCuller )
    {
        if ( !m_bDisplay )
        {
            return;
        }
        m_spkScene.UpdateGS();
        if ( !m_bDisplaySecond )
        {
            // First rendering pass:
            // Draw the proxy geometry to a color buffer, to generate the
            // back-facing texture-coordinates:
            m_kMesh.DetachAllEffects();
            m_kMesh.AttachEffect( m_spkVertexColor3Shader );
            kCuller.ComputeVisibleSet(m_spkScene);
            // Enable rendering to the PBuffer:
            kRenderer.SetBackgroundColor(ColorRGBA.BLACK);
            kRenderer.ClearBuffers();
            // Cull front-facing polygons:
            m_spkCull.CullFace = CullState.CullMode.CT_FRONT;
            kRenderer.DrawScene(kCuller.GetVisibleSet());
            // Undo culling:
            m_spkCull.CullFace = CullState.CullMode.CT_BACK;
        }
        else
        {
            // First rendering pass:
            // Draw the proxy geometry to a color buffer, to generate the
            // back-facing texture-coordinates:
            m_kMesh.DetachAllEffects();
            m_kMesh.AttachEffect( m_spkVertexColor3Shader );
            kCuller.ComputeVisibleSet(m_spkScene);
            // Enable rendering to the PBuffer:
            m_pkPBuffer.Enable();
            kRenderer.SetBackgroundColor(ColorRGBA.BLACK);
            kRenderer.ClearBuffers();
            
            kCuller.ComputeVisibleSet(m_spkScene);
            
            // Cull front-facing polygons:
            m_spkCull.CullFace = CullState.CullMode.CT_FRONT;
            kRenderer.DrawScene(kCuller.GetVisibleSet());
            // Undo culling:
            m_spkCull.CullFace = CullState.CullMode.CT_BACK;
        }
        
    }

    public void PostPreRender( Renderer kRenderer, Culler kCuller )
    {
        // Disable the PBuffer
        m_pkPBuffer.Disable();
    }

    public void Render( Renderer kRenderer, Culler kCuller )
    {
        if ( !m_bDisplay )
        {
            return;
        }
        //System.err.println( "GPU" );        
        // Second rendering pass:
        // Draw the proxy grometry with the volume ray-tracing shader:
        m_kMesh.DetachAllEffects();
        m_kMesh.AttachEffect( m_kVolumeShaderEffect );
        kCuller.ComputeVisibleSet(m_spkScene);
        kRenderer.DrawScene(kCuller.GetVisibleSet());

//         // Draw screne polygon:
//         kRenderer.SetCamera(m_spkScreenCamera);
//         kRenderer.Draw(m_spkScenePolygon);
    }

    public void SetDisplaySecond( boolean bDisplay )
    {
        m_bDisplaySecond = bDisplay;
    }

    public void dispose()
    {
        if ( m_spkScenePolygon != null )
        {
            m_spkScenePolygon.dispose();
            m_spkScenePolygon = null;
        }

        if ( m_spkSceneImage != null )
        {
            m_spkSceneImage.dispose();
            m_spkSceneImage = null;
        }
        if ( m_pkSceneTarget != null )
        {
            m_pkSceneTarget.dispose();
            m_pkSceneTarget = null;
        }
        if ( m_pkPBuffer != null )
        {
            m_pkPBuffer.dispose();
            m_pkPBuffer = null;
        }
        if ( m_spkWireframe != null )
        {
            m_spkWireframe.dispose();
            m_spkWireframe = null;
        }
        if ( m_kMaterial != null )
        {
            m_kMaterial.dispose();
            m_kMaterial = null;
        }
        if ( m_kMesh != null )
        {
            m_kMesh.dispose();
            m_kMesh = null;
        }
        if ( m_kVolumeShaderEffect != null )
        {
            m_kVolumeShaderEffect.dispose();
            m_kVolumeShaderEffect = null;
        }
    }
    
    public Node GetScene()
    {
        return m_spkScene;
    }
    
    /**
     * Called by the init() function. Creates and initialized the scene-graph.
     * @param arg0, the GLCanvas
     */
    public void CreateScene ( FrameBuffer.FormatType eFormat,
            FrameBuffer.DepthType eDepth, FrameBuffer.StencilType eStencil,
            FrameBuffer.BufferingType eBuffering,
            FrameBuffer.MultisamplingType eMultisampling,
            int iWidth, int iHeight,
            GLAutoDrawable arg0, Renderer kRenderer)
    {
        // The screen camera is designed to map (x,y,z) in [0,1]^3 to (x',y,'z')
        // in [-1,1]^2 x [0,1].
        m_spkScreenCamera = new Camera();
        m_spkScreenCamera.Perspective = false;
        m_spkScreenCamera.SetFrustum(0.0f,1.0f,0.0f,1.0f,0.0f,1.0f);
        m_spkScreenCamera.SetFrame(Vector3f.ZERO,Vector3f.UNIT_Z,
                Vector3f.UNIT_Y,Vector3f.UNIT_X);

        // Create a scene graph with the face model as the leaf node.
        m_spkScene = new Node();
        CreateBox();
        m_spkScene.AttachChild( m_kMesh );
        m_spkWireframe = new WireframeState();
        m_spkScene.AttachGlobalState(m_spkWireframe);
        m_spkCull = new CullState();
        m_spkScene.AttachGlobalState(m_spkCull);

        m_spkAlpha = new AlphaState();
        m_spkAlpha.BlendEnabled = true;
        //m_spkAlpha.SrcBlend = AlphaState.SrcBlendMode.SBF_ONE_MINUS_DST_COLOR;
        //m_spkAlpha.DstBlend = AlphaState.DstBlendMode.DBF_ONE;
        m_spkScene.AttachGlobalState(m_spkAlpha);

        // Create a screen polygon to use as the RGBA render target.
        Attributes kAttr = new Attributes();
        kAttr.SetPChannels(3);
        kAttr.SetTChannels(0,2);
        VertexBuffer pkVBuffer = new VertexBuffer(kAttr,4);
        pkVBuffer.SetPosition3(0,0.0f,0.0f,0.0f);
        pkVBuffer.SetPosition3(1,0.2f,0.0f,0.0f);
        pkVBuffer.SetPosition3(2,0.2f,0.2f,0.0f);
        pkVBuffer.SetPosition3(3,0.0f,0.2f,0.0f);
        pkVBuffer.SetTCoord2(0,0,0.0f,0.0f);
        pkVBuffer.SetTCoord2(0,1,1.0f,0.0f);
        pkVBuffer.SetTCoord2(0,2,1.0f,1.0f);
        pkVBuffer.SetTCoord2(0,3,0.0f,1.0f);
        IndexBuffer pkIBuffer = new IndexBuffer(6);
        int[] aiIndex = pkIBuffer.GetData();
        aiIndex[0] = 0;  aiIndex[1] = 1;  aiIndex[2] = 2;
        aiIndex[3] = 0;  aiIndex[4] = 2;  aiIndex[5] = 3;
        m_spkScenePolygon = new TriMesh(pkVBuffer,pkIBuffer);

        // Create a red image for the purposes of debugging.  The image itself
        // should not show up because the frame-buffer object writes to the
        // texture memory.  But if this fails, the red image should appear.
        byte[] aucData = new byte[4*iWidth*iHeight];
        for (int i = 0; i < iWidth*iHeight; i++)
        {
            aucData[i++] = (byte)0xFF;
            aucData[i++] = 0x00;
            aucData[i++] = 0x00;
            aucData[i++] = (byte)0xFF;
        }
        m_spkSceneImage = new GraphicsImage(GraphicsImage.FormatMode.IT_RGBA8888,iWidth,iHeight,aucData,
        "SceneImage");

        // Create the texture effect for the scene polygon.  The resources are
        // loaded so that the scene target texture has everything needed for
        // FrameBuffer::Create to succeed.
        TextureEffect pkEffect = new TextureEffect("SceneImage");
        m_pkSceneTarget = pkEffect.GetPTexture(0,0);
        m_pkSceneTarget.SetFilterType(Texture.FilterType.NEAREST);
        m_pkSceneTarget.SetWrapType(0,Texture.WrapType.CLAMP_BORDER);
        m_pkSceneTarget.SetWrapType(1,Texture.WrapType.CLAMP_BORDER);

        m_pkSceneTarget.SetOffscreenTexture(true);
        //m_pkSceneTarget.SetDepthCompare (DepthCompare.DC_LESS);

        
        
        m_spkScenePolygon.AttachEffect(pkEffect);
        m_spkScenePolygon.UpdateGS();
        m_spkScenePolygon.UpdateRS();
        kRenderer.LoadResources(m_spkScenePolygon);


        // Create the RGBA frame-buffer object to be bound to the scene polygon.
        m_pkPBuffer = new OpenGLFrameBuffer(eFormat,eDepth,eStencil,
                eBuffering,eMultisampling,kRenderer,m_pkSceneTarget,arg0);
        assert(m_pkPBuffer != null);

        m_spkScene.UpdateGS();
        m_kTranslate = new Vector3f( m_spkScene.WorldBound.GetCenter() );
        m_kTranslate.negEquals();
        m_spkScene.GetChild(0).Local.SetTranslate( m_kTranslate );

        
        m_kVolumeShaderEffect = new VolumeShaderEffect_WM( m_kVolumeImageA,  
                m_pkSceneTarget);
        kRenderer.LoadResources(m_kVolumeShaderEffect);
        m_kVolumeShaderEffect.SetPassQuantity(1);
        m_kVolumeShaderEffect.CMPMode(kRenderer);
        m_kVolumeShaderEffect.Blend(0.75f);

        m_spkScene.UpdateGS();
        m_spkScene.UpdateRS();
    }

    public Vector3f GetTranslate()
    {
        return m_kTranslate;
    }

    /**
     * Called by CreateScene. Creates the bounding-box proxy geometry scene
     * node.
     */
    private void CreateBox ()
    {
        int iXBound = m_kVolumeImageA.GetImage().getExtents()[0];
        int iYBound = m_kVolumeImageA.GetImage().getExtents()[1];
        int iZBound = m_kVolumeImageA.GetImage().getExtents()[2];
        Box(iXBound,iYBound,iZBound);
        m_spkVertexColor3Shader = new VertexColor3Effect();
    }

    /**
     * Called by CreateBox. Creates the bounding-box proxy geometry (VertexBuffer, IndexBuffer).
     * @param iXBound image x-extent.
     * @param iYBound image y-extent.
     * @param iZBound image z-extent.
     * @return TriMesh, new geometry.
     */
    private TriMesh Box (int iXBound, int iYBound, int iZBound)
    {
        Attributes kAttr = new Attributes();
        kAttr.SetPChannels(3);
        kAttr.SetCChannels(0,3);
        kAttr.SetTChannels(0,3);
        kAttr.SetTChannels(1,3);

        float fMaxX = (float) (iXBound - 1) * m_kVolumeImageA.GetImage().getFileInfo(0).getResolutions()[0];
        float fMaxY = (float) (iYBound - 1) * m_kVolumeImageA.GetImage().getFileInfo(0).getResolutions()[1];
        float fMaxZ = (float) (iZBound - 1) * m_kVolumeImageA.GetImage().getFileInfo(0).getResolutions()[2];

        m_fMax = fMaxX;
        if (fMaxY > m_fMax) {
            m_fMax = fMaxY;
        }
        if (fMaxZ > m_fMax) {
            m_fMax = fMaxZ;
        }
        m_fX = fMaxX/m_fMax;
        m_fY = fMaxY/m_fMax;
        m_fZ = fMaxZ/m_fMax;

        int iVQuantity = 24;
        int iTQuantity = 12;
        VertexBuffer pkVB = new VertexBuffer(kAttr,iVQuantity);
        IndexBuffer pkIB = new IndexBuffer(3*iTQuantity);

        // generate connectivity (outside view)
        int i = 0;
        int[] aiIndex = pkIB.GetData();

        // generate geometry
        // front
        pkVB.SetPosition3(0,0,0,0);
        pkVB.SetPosition3(1,m_fX,0,0);
        pkVB.SetPosition3(2,m_fX,m_fY,0);
        pkVB.SetPosition3(3,0,m_fY,0);
        pkVB.SetColor3(0,0,0,0,0);
        pkVB.SetColor3(0,1,1,0,0);
        pkVB.SetColor3(0,2,1,1,0);
        pkVB.SetColor3(0,3,0,1,0);
        aiIndex[i++] = 0;  aiIndex[i++] = 2;  aiIndex[i++] = 1;
        aiIndex[i++] = 0;  aiIndex[i++] = 3;  aiIndex[i++] = 2;

        // back
        pkVB.SetPosition3(4,0,0,m_fZ);
        pkVB.SetPosition3(5,m_fX,0,m_fZ);
        pkVB.SetPosition3(6,m_fX,m_fY,m_fZ);
        pkVB.SetPosition3(7,0,m_fY,m_fZ);
        pkVB.SetColor3(0,4,0,0,1);
        pkVB.SetColor3(0,5,1,0,1);
        pkVB.SetColor3(0,6,1,1,1);
        pkVB.SetColor3(0,7,0,1,1);
        aiIndex[i++] = 4;  aiIndex[i++] = 5;  aiIndex[i++] = 6;
        aiIndex[i++] = 4;  aiIndex[i++] = 6;  aiIndex[i++] = 7;

        // top
        pkVB.SetPosition3(8,0,m_fY,0);
        pkVB.SetPosition3(9,m_fX,m_fY,0);
        pkVB.SetPosition3(10,m_fX,m_fY,m_fZ);
        pkVB.SetPosition3(11,0,m_fY,m_fZ);
        pkVB.SetColor3(0,8,0,1,0);
        pkVB.SetColor3(0,9,1,1,0);
        pkVB.SetColor3(0,10,1,1,1);
        pkVB.SetColor3(0,11,0,1,1);
        aiIndex[i++] = 8;  aiIndex[i++] = 10;  aiIndex[i++] = 9;
        aiIndex[i++] = 8;  aiIndex[i++] = 11;  aiIndex[i++] = 10;

        // bottom
        pkVB.SetPosition3(12,0,0,0);
        pkVB.SetPosition3(13,m_fX,0,0);
        pkVB.SetPosition3(14,m_fX,0,m_fZ);
        pkVB.SetPosition3(15,0,0,m_fZ);
        pkVB.SetColor3(0,12,0,0,0);
        pkVB.SetColor3(0,13,1,0,0);
        pkVB.SetColor3(0,14,1,0,1);
        pkVB.SetColor3(0,15,0,0,1);
        aiIndex[i++] = 12;  aiIndex[i++] = 13;  aiIndex[i++] = 14;
        aiIndex[i++] = 12;  aiIndex[i++] = 14;  aiIndex[i++] = 15;

        // right
        pkVB.SetPosition3(16,m_fX,0,0);
        pkVB.SetPosition3(17,m_fX,m_fY,0);
        pkVB.SetPosition3(18,m_fX,m_fY,m_fZ);
        pkVB.SetPosition3(19,m_fX,0,m_fZ);
        pkVB.SetColor3(0,16,1,0,0);
        pkVB.SetColor3(0,17,1,1,0);
        pkVB.SetColor3(0,18,1,1,1);
        pkVB.SetColor3(0,19,1,0,1);
        aiIndex[i++] = 16;  aiIndex[i++] = 17;  aiIndex[i++] = 18;
        aiIndex[i++] = 16;  aiIndex[i++] = 18;  aiIndex[i++] = 19;

        // left
        pkVB.SetPosition3(20,0,0,0);
        pkVB.SetPosition3(21,0,m_fY,0);
        pkVB.SetPosition3(22,0,m_fY,m_fZ);
        pkVB.SetPosition3(23,0,0,m_fZ);
        pkVB.SetColor3(0,20,0,0,0);
        pkVB.SetColor3(0,21,0,1,0);
        pkVB.SetColor3(0,22,0,1,1);
        pkVB.SetColor3(0,23,0,0,1);
        aiIndex[i++] = 20;  aiIndex[i++] = 22;  aiIndex[i++] = 21;
        aiIndex[i++] = 20;  aiIndex[i++] = 23;  aiIndex[i++] = 22;

        if (kAttr.GetMaxTCoords() > 0)
        {
            for (int iUnit = 0; iUnit < kAttr.GetMaxTCoords(); iUnit++)
            {
                if (kAttr.HasTCoord(iUnit))
                {
                    pkVB.SetTCoord3(iUnit,0,0,0,0);
                    pkVB.SetTCoord3(iUnit,1,1,0,0);
                    pkVB.SetTCoord3(iUnit,2,1,1,0);
                    pkVB.SetTCoord3(iUnit,3,0,1,0);

                    pkVB.SetTCoord3(iUnit,4,0,0,1);
                    pkVB.SetTCoord3(iUnit,5,1,0,1);
                    pkVB.SetTCoord3(iUnit,6,1,1,1);
                    pkVB.SetTCoord3(iUnit,7,0,1,1);

                    pkVB.SetTCoord3(iUnit,8,0,1,0);
                    pkVB.SetTCoord3(iUnit,9,1,1,0);
                    pkVB.SetTCoord3(iUnit,10,1,1,1);
                    pkVB.SetTCoord3(iUnit,11,0,1,1);

                    pkVB.SetTCoord3(iUnit,12,0,0,0);
                    pkVB.SetTCoord3(iUnit,13,1,0,0);
                    pkVB.SetTCoord3(iUnit,14,1,0,1);
                    pkVB.SetTCoord3(iUnit,15,0,0,1);

                    pkVB.SetTCoord3(iUnit,16,1,0,0);
                    pkVB.SetTCoord3(iUnit,17,1,1,0);
                    pkVB.SetTCoord3(iUnit,18,1,1,1);
                    pkVB.SetTCoord3(iUnit,19,1,0,1);

                    pkVB.SetTCoord3(iUnit,20,0,0,0);
                    pkVB.SetTCoord3(iUnit,21,0,1,0);
                    pkVB.SetTCoord3(iUnit,22,0,1,1);
                    pkVB.SetTCoord3(iUnit,23,0,0,1);
                }
            }
        }
        m_kMesh = new TriMesh(pkVB,pkIB);

        // polished gold
        m_kMaterial = new MaterialState();
        m_kMaterial.Emissive = new ColorRGB(ColorRGB.BLACK);
        m_kMaterial.Ambient = new ColorRGB(0.1f,0.1f,0.1f);
        m_kMaterial.Diffuse = new ColorRGB(1f,1f,1f);
        m_kMaterial.Specular = new ColorRGB(1f,1f,1f);
        m_kMaterial.Shininess = 64f;
        m_kMesh.AttachGlobalState(m_kMaterial);
        m_kMesh.UpdateMS(true);
        return m_kMesh;
    }

    public void InitClip( float[] afClip )
    {
        m_kVolumeShaderEffect.InitClip(afClip);
    }

    public void SetClip( int iWhich, float[] data)
    {
        m_kVolumeShaderEffect.SetClip(iWhich, data);
    }


    public void SetClipEye( float[] afEquation )
    {
        m_kVolumeShaderEffect.SetClipEye(afEquation);
    }


    public void SetClipEyeInv( float[] afEquation )
    {
        m_kVolumeShaderEffect.SetClipEyeInv(afEquation);
    }

    public void SetClipArb( float[] afEquation )
    {
        m_kVolumeShaderEffect.SetClipArb(afEquation);
    }

    public void ReloadVolumeShader( Renderer kRenderer )
    {
        m_kVolumeShaderEffect.Reload( kRenderer );
    }


    public void SetLight( String kLightType, float[] afType )
    {
        m_kVolumeShaderEffect.SetLight(kLightType, afType);
    }

    /**
     * Display the volume in MIP mode.
     */
    public void MIPMode( Renderer kRenderer )
    {
        m_kVolumeShaderEffect.MIPMode(kRenderer);
    }

    /**
     * Display the volume in DDR mode.
     */
    public void DDRMode(Renderer kRenderer)
    {
        m_kVolumeShaderEffect.DDRMode(kRenderer);
    }

    /**
     * Display the volume in Composite mode.
     */
    public void CMPMode(Renderer kRenderer)
    {
        m_kVolumeShaderEffect.CMPMode(kRenderer);
    }

    /**
     * Display the volume in Composite Surface mode.
     */
    public void SURMode(Renderer kRenderer)
    {
        m_kVolumeShaderEffect.SURMode(kRenderer);
    }

    /**
     * Display the volume in Surface mode.
     */
    public void SURFASTMode(Renderer kRenderer)
    {
        m_kVolumeShaderEffect.SURFASTMode(kRenderer);
    }

    /**
     * Sets blending between imageA and imageB.
     * @param fValue, the blend value (0-1)
     */
    public void Blend( float fValue )
    {
        m_kVolumeShaderEffect.Blend(fValue);
    }

    /**
     * Sets the raytracing steps size.
     * @param fValue, the steps value (0-450)
     */
    public void StepsSize( float fValue )
    {
        m_kVolumeShaderEffect.setSteps(fValue);
    }

    /**
     * Sets the background color.
     * @param kColor, new background color.
     */
    public void SetBackgroundColor( ColorRGBA kColor )
    {
        m_kVolumeShaderEffect.SetBackgroundColor( kColor );
    }

    /**
     * Enables/Disables Gradient Magnitude filter.
     * @param bShow, gradient magnitude filter on/off
     */
    public void SetGradientMagnitude(boolean bShow)
    {
        m_kVolumeShaderEffect.SetGradientMagnitude(bShow);
    }

    /**
     * Enables/Disables self-shadowing in the Surface mode.
     * @param bShadow, shadow on/off.
     */
    public void SelfShadow(boolean bShadow)
    {
        m_kVolumeShaderEffect.SelfShadow(bShadow);
    }

    public VolumeShaderEffect_WM GetShaderEffect()
    {
        return m_kVolumeShaderEffect;
        
    }

    public void setVolumeBlend( float fBlend )
    {
        if ( m_kVolumeShaderEffect != null )
        {
            m_kVolumeShaderEffect.Blend(fBlend);
        }
    }

    /**
     * Called from the AdvancedMaterialProperties dialog. Sets the material
     * properties for the VolumeShaderSUR (Surface and Composite Surface
     * volume shaders.)
     * @param kMaterial, new material properties for the surface mode.
     */
    public void SetMaterialState( MaterialState kMaterial )
    {
        m_kMesh.DetachGlobalState(GlobalState.StateType.MATERIAL);
        m_kMaterial = kMaterial;
        m_kMesh.AttachGlobalState(m_kMaterial);
        m_kMesh.UpdateMS(true);
        m_kMesh.UpdateRS();
    }

    /**
     * Called from the JPanelDisplay dialog. Gets the material properties for
     * the VolumeShaderSUR (Surface and Composite Surface volume shaders.)
     * @return material properties for the surface mode.
     */
    public MaterialState GetMaterialState( )
    {
        return m_kMaterial;
    }


    //private VolumeImage m_kVolumeImageB;

    /** VolumeShaderEffect applied to proxy-geometry: */
    private VolumeShaderEffect_WM m_kVolumeShaderEffect = null;

    /** Vertex-color shader effect used for the polylines and the first-pass
     * rendering of the proxy-geometry:*/
    private ShaderEffect m_spkVertexColor3Shader;
    
    /** Scene-graph root node: */
    private Node m_spkScene;
    /** Turns wireframe on/off: */
    private WireframeState m_spkWireframe;
    /** Alpha blending state for blending between geometry and the volume. */
    private AlphaState m_spkAlpha;
    /** Culling: turns backface/frontface culling on/off: */
    private CullState m_spkCull;

    /** Normalized volume extents: */
    private float m_fMax;

    /** Material properties for Volume Surface (and Composite Surface) mode*/
    private MaterialState m_kMaterial;
    /** Volume proxy-geometry (cube) */
    private TriMesh m_kMesh;


    /** Screen camera for displaying the screen polygon: */
    private Camera m_spkScreenCamera;
    /** Scene polygon displaying the first-pass rendering of the proxy-geometry: */
    private TriMesh m_spkScenePolygon;
    /** GraphicsImage with the first-pass rendering of the proxy-geometry: */
    private GraphicsImage m_spkSceneImage;
    /** Texture for the first-pass rendering of the proxy-geometry: */
    private Texture m_pkSceneTarget;
    /** Off-screen buffer the first-pass rendering is drawn into: */
    private OpenGLFrameBuffer m_pkPBuffer;
    
    private boolean m_bDisplaySecond = true;
}
