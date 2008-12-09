package gov.nih.mipav.view.renderer.WildMagic.Render;

import gov.nih.mipav.MipavCoordinateSystems;
import gov.nih.mipav.model.algorithms.AlgorithmExtractSurface;
import gov.nih.mipav.model.algorithms.AlgorithmExtractSurfaceCubes;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.view.ViewUserInterface;
import gov.nih.mipav.view.ViewJFrameImage;
import gov.nih.mipav.view.dialogs.JDialogBase;
import gov.nih.mipav.view.renderer.WildMagic.VolumeTriPlanarInterface;
import gov.nih.mipav.view.renderer.WildMagic.Interface.FileSurface_WM;
import gov.nih.mipav.view.renderer.WildMagic.Interface.SurfaceExtractorCubes;

import java.awt.Frame;
import java.awt.event.KeyListener;
import java.io.File;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import WildMagic.LibFoundation.Meshes.VETMesh;
import WildMagic.LibGraphics.Rendering.GraphicsImage;
import WildMagic.LibGraphics.Rendering.Texture;
import WildMagic.LibGraphics.SceneGraph.IndexBuffer;
import WildMagic.LibGraphics.SceneGraph.TriMesh;

import com.sun.opengl.util.Animator;

public class VolumeImageExtract extends VolumeImageViewer
    implements GLEventListener, KeyListener
{
    private static int ms_iSurface = 0;
    private VolumeCalcEffect m_spkEffect2;
    private GraphicsImage m_kCalcImage;
    private Texture m_pkVolumeCalcTarget;
    private SurfaceExtractImage m_kCalcImage2;
    private Texture m_pkVolumeCalcTarget2;
    private boolean m_bDisplayFirst = true;
    private boolean m_bDisplaySecond = true;
    private VolumeClipEffect m_kClipEffect = null;
    private int m_iSize = 0;
    public VolumeImageExtract( VolumeTriPlanarInterface kParentFrame, VolumeImage kVolumeImage, VolumeClipEffect kClip )
    {
        super(kParentFrame, kVolumeImage );

        m_kClipEffect = kClip;
    }
    /**
     * @param args
     */
    public static void main( VolumeTriPlanarInterface kParentFrame, VolumeImage kVolumeImage, VolumeClipEffect kClip )
    {
        VolumeImageExtract kWorld = new VolumeImageExtract(kParentFrame, kVolumeImage, kClip);
        Frame frame = new Frame(kWorld.GetWindowTitle());
        frame.add( kWorld.GetCanvas() );
         final Animator animator = new Animator( kWorld.GetCanvas() );
         // setting the frame to be undecorated removes the frame title bar and edges
         // this prevents flashing on-screen.
         frame.setUndecorated(true);
         // frame must be set to visible for the gl canvas to be properly initialized.
         frame.setVisible(true);
         frame.setBounds(0,0,
                 kWorld.GetWidth(), kWorld.GetHeight() );
         frame.setVisible(false);
         kWorld.SetAnimator(animator);
         kWorld.SetFrame(frame);
         animator.start();
    }

    public void display(GLAutoDrawable arg0) {
        while ( m_bDisplayFirst )
        {
            float fZ = ((float)m_iSlice)/(m_iSize -1);
            UpdateSlice(fZ);
            m_pkPlane.DetachAllEffects();
            m_pkPlane.AttachEffect(m_spkEffect);
            m_kCuller.ComputeVisibleSet(m_spkScene);
            m_pkRenderer.ClearBuffers();
            if (m_pkRenderer.BeginScene())
            {          
                m_pkRenderer.DrawScene(m_kCuller.GetVisibleSet());
                m_pkRenderer.EndScene();
            }
            m_pkRenderer.FrameBufferToTexSubImage3D( m_pkVolumeCalcTarget, m_iSlice, false );
            //m_pkRenderer.DisplayBackBuffer();
            m_iSlice++; 
            if ( m_iSlice >= m_iSize)
            {
                m_iSlice = 0;
                m_bDisplayFirst = false;
            }
        }

          while ( m_bDisplaySecond )
          {
              float fZ = ((float)m_iSlice)/(m_iSize -1);
              UpdateSlice(fZ);
              m_pkPlane.DetachAllEffects();
              m_pkPlane.AttachEffect(m_spkEffect2);
              m_kCuller.ComputeVisibleSet(m_spkScene);
              m_pkRenderer.ClearBuffers();
              if (m_pkRenderer.BeginScene())
              {          
                  m_pkRenderer.DrawScene(m_kCuller.GetVisibleSet());
                  m_pkRenderer.EndScene();
                  //writeImage();
              }
              m_pkRenderer.FrameBufferToTexSubImage3D( m_pkVolumeCalcTarget2, m_iSlice, true );
              //m_pkRenderer.DisplayBackBuffer();
              m_iSlice++; 
              if ( m_iSlice >= m_iSize)
              {
                  m_bDisplaySecond = false;
                  m_iSlice = 0;
                  //System.err.println( m_kCalcImage2.Min + " " + m_kCalcImage2.Max + " " + m_kCalcImage2.TriTable.size() );
                  
                  int[] direction = MipavCoordinateSystems.getModelDirections(m_kVolumeImage.GetImage());
                  float[] startLocation = m_kVolumeImage.GetImage().getFileInfo(0).getOrigin();
                  SurfaceExtractorCubes kExtractor = new SurfaceExtractorCubes(256, 256, 256, m_kCalcImage2.Data,
                          1, 1, 1, direction,
                          startLocation, null);
                  TriMesh kMesh = kExtractor.getLevelSurface(50, m_kCalcImage2.TriTable);
//                Get the adjacent triangles:
                  VETMesh kVETMesh = new VETMesh( 2* kMesh.VBuffer.GetVertexQuantity(), .9f,
                          2 * kMesh.IBuffer.GetIndexQuantity(), .9f,
                          2 * kMesh.GetTriangleQuantity(), .9f,
                          kMesh.IBuffer.GetData() );
                  kMesh.IBuffer = new IndexBuffer( kVETMesh.GetTriangles() );
                  TriMesh[] kMeshes = new TriMesh[1];
                  kMeshes[0] = kMesh;
                  if ( kMeshes[0] != null )
                  {
                      m_kParent.getVolumeGPU().displayVolumeRaycast(false);
                      String kSurfaceName = JDialogBase.makeImageName(m_kVolumeImage.GetImage().getImageName(), ms_iSurface + "_extract.sur");
                      kMeshes[0].SetName( kSurfaceName );
                      m_kParent.getSurfacePanel().addSurfaces(kMeshes);
                      m_kParent.getRendererGUI().setDisplaySurfaceCheck( true );
                      m_kParent.getRendererGUI().setDisplayVolumeCheck( false );
                      ms_iSurface++;
                  }

                  m_pkVolumeCalcTarget.dispose();
                  m_kCalcImage.dispose();
                  m_pkVolumeCalcTarget2.dispose();
                  m_kCalcImage2.dispose();
                  m_spkEffect2.dispose();
                  
                  /*
                  //System.err.println("Done second pass");
                  ModelImage kImage = m_kVolumeImage.CreateImageFromTexture(m_pkVolumeCalcTarget2.GetImage());
                  float[] res = new float[]{1f,1f,1f};
                  for (int i = 0; i < kImage.getExtents()[2]; i++) {
                      kImage.getFileInfo()[i].setResolutions(res);
                  }
                  kImage.calcMinMax();
                  //new ViewJFrameImage(kImage, null, new java.awt.Dimension(610, 200), false);

                  String kSurfaceName = JDialogBase.makeImageName(kImage.getImageName(), "_extract.sur");
                  AlgorithmExtractSurfaceCubes extractSurAlgo = 
                      new AlgorithmExtractSurfaceCubes(kImage, 50, AlgorithmExtractSurfaceCubes.LEVEL_MODE,
                                                       false, false, 0, kSurfaceName );
                  extractSurAlgo.extractSurface(false);  
                  TriMesh[] kMeshes = new TriMesh[1];
                  kMeshes[0] = extractSurAlgo.mesh;
                  if ( kMeshes[0] != null )
                  {
                      m_kParent.getVolumeGPU().displayVolumeRaycast(false);
                      kSurfaceName = JDialogBase.makeImageName(kImage.getImageName(), ms_iSurface + "_extract.sur");
                      kMeshes[0].SetName( kSurfaceName );
                      m_kParent.getSurfacePanel().addSurfaces(kMeshes);
                      m_kParent.getRendererGUI().setDisplaySurfaceCheck( true );
                      m_kParent.getRendererGUI().setDisplayVolumeCheck( false );
                      ms_iSurface++;
                  }
                  kImage.disposeLocal();
                  kImage = null;
*/
              }
          }

        m_kAnimator.stop();
        m_kFrame.setVisible(false);

    }

    protected void CreateScene ()
    {
        CreatePlaneNode();
        m_iSize = Math.max( Math.max(m_kVolumeImage.GetImage().getExtents()[0], m_kVolumeImage.GetImage().getExtents()[1]),
                m_kVolumeImage.GetImage().getExtents()[2]);
        float fStep = 1.0f/(float)(m_iSize-1);
        
        
        m_spkEffect = new VolumeCalcEffect( m_kVolumeImage, m_kClipEffect, false );
        m_pkPlane.AttachEffect(m_spkEffect);
        m_pkRenderer.LoadResources(m_pkPlane);
        ((VolumeCalcEffect)m_spkEffect).SetStepSize(fStep, fStep, fStep);
        m_pkPlane.DetachAllEffects();

        

        m_kCalcImage2 = new SurfaceExtractImage(GraphicsImage.FormatMode.IT_RGBA8888,
                                         m_iWidth,m_iHeight,
                                         m_iSize, 
                                         new byte[m_iWidth*m_iHeight*m_iSize*4],
                                         "VolumeExtract2" );
        m_pkVolumeCalcTarget2 = new Texture();
        m_pkVolumeCalcTarget2.SetImage(m_kCalcImage2);
        m_spkEffect2 = new VolumeCalcEffect( "VolumeExtract2", m_pkVolumeCalcTarget2, "SurfaceExtract_P2" );
        m_pkPlane.AttachEffect(m_spkEffect2);
        m_pkRenderer.LoadResources(m_pkPlane);
        m_spkEffect2.SetStepSize(fStep, fStep, fStep);
        m_pkPlane.DetachAllEffects();
        
        
        m_kCalcImage = new GraphicsImage(GraphicsImage.FormatMode.IT_RGBA8888,
                                         m_iWidth,m_iHeight,
                                         m_iSize, 
                                         new byte[m_iWidth*m_iHeight*m_iSize*4],
                                         "VolumeExtract" );
        m_pkVolumeCalcTarget = new Texture();
        m_pkVolumeCalcTarget.SetImage(m_kCalcImage);
        m_spkEffect2 = new VolumeCalcEffect( "VolumeExtract", m_pkVolumeCalcTarget, "SurfaceExtract_P2" );
        m_pkPlane.AttachEffect(m_spkEffect2);
        m_pkRenderer.LoadResources(m_pkPlane);
        (m_spkEffect2).SetStepSize(fStep, fStep, fStep);
        m_pkPlane.DetachAllEffects();
        
        

    }
}
