package gov.nih.mipav.view.renderer.WildMagic.Interface;


import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.model.structures.ModelLUT;
import gov.nih.mipav.model.structures.ModelRGB;
import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.ViewJColorChooser;
import gov.nih.mipav.view.ViewToolBarBuilder;
import gov.nih.mipav.view.dialogs.JDialogSmoothMesh;
import gov.nih.mipav.view.renderer.WildMagic.VolumeTriPlanarInterface;
import gov.nih.mipav.view.renderer.WildMagic.Decimate.TriangleMesh;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import WildMagic.LibFoundation.Mathematics.ColorRGB;
import WildMagic.LibFoundation.Mathematics.ColorRGBA;
import WildMagic.LibFoundation.Mathematics.Vector3f;
import WildMagic.LibGraphics.Detail.ClodMesh;
import WildMagic.LibGraphics.Rendering.MaterialState;
import WildMagic.LibGraphics.Rendering.WireframeState;
import WildMagic.LibGraphics.SceneGraph.IndexBuffer;
import WildMagic.LibGraphics.SceneGraph.Polyline;
import WildMagic.LibGraphics.SceneGraph.TriMesh;
import WildMagic.LibGraphics.SceneGraph.VertexBuffer;


public class JPanelSurface_WM extends JInterfaceBase
        implements ListSelectionListener, ChangeListener {

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = -4600563188022683359L;

    /** The colors for the first six surfaces are fixed. */
    private static ColorRGB[] fixedColor = {
        new ColorRGB(0.0f, 0.0f, 0.5f), // blue
        new ColorRGB(0.0f, 0.5f, 0.0f), // green
        new ColorRGB(0.5f, 0.0f, 0.0f), // red
        new ColorRGB(0.0f, 0.5f, 0.5f), // cyan
        new ColorRGB(0.5f, 0.0f, 0.5f), // violet
        new ColorRGB(0.5f, 0.5f, 0.0f) // yellow
    };

    /** The area label. */
    private JLabel areaLabel;

    /** Displays the area of triangle. */
    private JTextField areaText;

    /** The color button, which calls a color chooser. */
    private JButton colorButton;

    /** The color button label. */
    private JLabel colorLabel;

    /** The polygon mode combo box label. */
    private JLabel comboLabel;

    /** Decimate button. */
    private JButton decimateButton;

    /** The level of detail slider label. */
    private JLabel detailLabel;

    /** Level of detail slider. */
    private JSlider detailSlider;

    /** The labels below the detail slider. */
    private JLabel[] detailSliderLabels;

    /** Save surface button. */
    private JButton levelSButton, levelVButton, levelWButton, levelXMLButton, levelSTLButton, levelPLYButton;

    /** Paint tool-bar (contained in the SurfacePaint class) */
    private SurfacePaint_WM m_kSurfacePaint = null;

    /** The material options button, which launches the material editor window. */
    private JButton m_kAdvancedMaterialOptionsButton;

    /** Opens SurfaceTexture dialog:. */
    private JButton m_kSurfaceTextureButton;

    /** The opacity slider label. */
    private JLabel opacityLabel;

    /** Opacity slider, not enabled yet. */
    private JSlider opacitySlider;

    /** The labels below the opacity slider. */
    private JLabel[] opacitySliderLabels;

    /** The combo box for the polygon mode to display. */
    private JComboBox polygonModeCB;

    /** The scroll pane holding the panel content. Useful when the screen is small. */
    private JScrollPane scroller;

    /** Smooth button. */
    private JButton smooth1Button, smooth2Button, smooth3Button;

    /** Polyline list box in the dialog for surfaces. */
    private JList polylineList;
    
    /** The list box in the dialog for surfaces. */
    private JList surfaceList;

    /** Check Box for surface picking. */
    private JCheckBox surfacePickableCB;

    /** Check Box for surface back face culling. */
    private JCheckBox surfaceBackFaceCB;

    /** Check Box for surface clpping of the volume render. */
    private JCheckBox surfaceClipCB;

    /** Check Box for surface transparency. */
    private JCheckBox surfaceTransparencyCB;

    /** The number of triangles label. */
    private JLabel triangleLabel;

    /** Displays the number of triangles. */
    private JTextField triangleText;

    /** The volume label. */
    private JLabel volumeLabel;

    /** Displays the volume of triangle. */
    private JTextField volumeText;

    /** List of TriMeshes */
    private Vector<TriMesh> m_kMeshes = new Vector<TriMesh>();

    /** Polyline counter list <index, groupID> */
    private DefaultListModel polylineCounterList = new DefaultListModel();
    
    /** constant polyline counter */
    private int polylineCounter = 0;
    
    /** Decimation Percentage */
    private double decimationPercentage = 95.0;
    
    /** triangle mesh for decimation. */
    private TriangleMesh[] tmesh;
    
    /**
     * Constructor.
     * @param kVolumeViewer parent frame.
     */
    public JPanelSurface_WM( VolumeTriPlanarInterface kVolumeViewer )
    {
        super(kVolumeViewer);
        m_kSurfacePaint = new SurfacePaint_WM(this, m_kVolumeViewer);
        init();
    }
    
    /**
     * static function returns the next default surface color, based on the current number of surfaces displayed. If the
     * number of surfaces is less than the fixedColor.length then fixedColor is the source of the surface color,
     * otherwise a random color is generated.
     *
     * @param   index  the number of the new surface
     *
     * @return  Color4f, the default surface color for the new surface.
     */
    public static ColorRGB getNewSurfaceColor(int index) {
        ColorRGB surfaceColor = new ColorRGB();

        if (index < fixedColor.length) {
            // Use the fixed colors for the first six surfaces.
            surfaceColor.Copy( fixedColor[index] );
        }
        else
        {
            Random randomGen = new Random();

            // Use randomly generated colors for the seventh and
            // later surfaces.
            surfaceColor.Set( 0.5f * (1.0f + randomGen.nextFloat()),
                                  0.5f * (1.0f + randomGen.nextFloat()),
                                  0.5f * (1.0f + randomGen.nextFloat()) );
        }

        return surfaceColor;
    }

    /**
     * The override necessary to be an ActionListener. This callback is executed whenever the Add or Remove buttons are
     * clicked, or when the color button or light button is clicked, or when the combo box changes. If the Add button is
     * clicked, a file dialog is launched to allow the user to select new surface meshes to load from disk. If the
     * Remove button is clicked, the currently selected surfaces in the list box are removed from the scene graph.
     *
     * @param  event  The action event.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        if (command.equals("Add")) {
            addSurface();
        } 
        else if (command.equals("Remove")) {
            removeSurface();
        }
        else if (command.equals("updatePlanes")) {
            m_kVolumeViewer.updatePlanes();
        }
        else if (command.equals("AddPolyline")) {
            addPolyline();
        } 
        else if (command.equals("RemovePolyline")) {
            removePolyline();
        }
        else if (command.equals("ChangeColor")) {
            colorChooser = new ViewJColorChooser(new Frame(), "Pick surface color", new OkColorListener(colorButton),
                                                 new CancelListener());
        } /* 
         else if (command.equals("ImageAsTexture")) {
            displayImageAsTexture(getSelectedSurfaces(surfaceList.getSelectedIndices()));
        } */ 
        else if (command.equals("AdvancedMaterialOptions")) {
            displayAdvancedMaterialOptions(surfaceList.getSelectedIndices());
        } 
        else if (command.equals("SurfaceTexture")) {
            m_kVolumeViewer.actionPerformed(new ActionEvent(m_kSurfaceTextureButton, 0, "SurfaceTexture"));
        }

        else if (command.equals("SurfacePickable")) {
            setPickable(surfaceList.getSelectedIndices());
        }  
        else if (command.equals("backface")) {
            setBackface(surfaceList.getSelectedIndices());
        }
        else if (command.equals("transparency")) {
            setTransparency(surfaceList.getSelectedIndices());
        }
        else if (command.equals("Clipping") )
        {
            setClipping(surfaceList.getSelectedIndices());
        }
        else if (command.equals("ChangePolyMode")) {
            changePolyMode(polygonIndexToMode(polygonModeCB.getSelectedIndex()));
        } else if (command.equals("LevelXML") || command.equals("LevelW") || command.equals("LevelS") ||
                   command.equals("LevelV") || command.equals("LevelSTL") || command.equals("LevelPLY"))
        {
            saveSurfaces( surfaceList.getSelectedIndices(), command );
        } 
        else if (command.equals("Smooth")) {
            smoothSurface(surfaceList.getSelectedIndices(), JDialogSmoothMesh.SMOOTH1);
        } else if (command.equals("Smooth2")) {
            smoothSurface(surfaceList.getSelectedIndices(), JDialogSmoothMesh.SMOOTH2);
        } else if (command.equals("Smooth3")) {
            smoothSurface(surfaceList.getSelectedIndices(), JDialogSmoothMesh.SMOOTH3);
        } 
        else if (command.equals("Decimate")) {
            decimate(surfaceList.getSelectedIndices());
        }
    }

    /**
     * Add surface to the volume image. Calls the FileSurface.openSurfaces function to open a file dialog so the user
     * can choose the surfaces to add.
     */
    public void addSurface() {
        TriMesh[] akSurfaces = FileSurface_WM.openSurfaces(m_kVolumeViewer.getImageA());
        if ( akSurfaces != null )
        {
            addSurfaces(akSurfaces);
        }
    }
     
    /**
     * Add surfaces to the Volume Tri-Planar renderer.
     * @param akSurfaces new surfaces.
     */
    public void addSurfaces( TriMesh[] akSurfaces )
    {
        m_kVolumeViewer.addSurface(akSurfaces);

        DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
        int iSize = kList.getSize();
        for ( int i = 0; i < akSurfaces.length; i++ )
        {
            kList.add( iSize + i, akSurfaces[i].GetName() );
            m_kMeshes.add(akSurfaces[i]);
        }
        surfaceList.setSelectedIndex(iSize);
        setElementsEnabled(true);
    }

    /**
     * Changes the polygon mode of the selected surface by detaching it, calling the appropriate method, and reattaching
     * it.
     *
     * @param  mode  The new polygon mode to set.
     */
    public void changePolyMode(WireframeState.FillMode mode) {
        int[] aiSelected = surfaceList.getSelectedIndices();

        if ( m_kVolumeViewer != null )
        {
            DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
            for (int i = 0; i < aiSelected.length; i++) {
                m_kVolumeViewer.setPolygonMode( (String)kList.elementAt(aiSelected[i]), mode);
            }
        }
    }

    /**
     * Dispose the local memory.
     */
    public void dispose() {
    	int i;
    	
    	if ( tmesh != null ) {
    		for ( i = 0; i < tmesh.length; i++ ) {
    			tmesh[i].dispose();
    			tmesh[i] = null;
    		}
    		tmesh = null;
    	}
    	
    	if (  m_kMeshes != null ) {
    		for ( i = 0; i < m_kMeshes.size(); i++ ) {
    			m_kMeshes.set(i, null);
    		}
    		m_kMeshes = null;
    	}
    	
    	if ( m_kSurfacePaint != null ) {
    		m_kSurfacePaint.dispose();
    		m_kSurfacePaint = null;
    	}
    	
    }



    /**
     * Enables/Disables the SurfacePaint per-vertex functions.
     * @param  bEnable  when true the SurfacePaint per-vertex functions (PaintBrush, Dropper, Eraser, BrushSize) are
     *                  enabled, when false they are disabled.
     */
    public void enableSurfacePaint(boolean bEnable) {
        m_kSurfacePaint.enableSurfacePaint(bEnable);
    }


    /**
     * Enables/Disables the SurfacePaint Paint Can function.
     * @param  bEnable  when true the Paint Can function is enabled, when false it is disabled.
     */
    public void enableSurfacePaintCan(boolean bEnable) {
        m_kSurfacePaint.enableSurfacePaintCan(bEnable);
    }

    /**
     * Return the name of the selected surface.
     * @return name of the selected surface.
     */
    public String getSelectedSurface()
    {
        int[] aiSelected = surfaceList.getSelectedIndices();
        if ( aiSelected.length == 0 )
        {
            return null;
        }               
        DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
        return (String)kList.elementAt(aiSelected[aiSelected.length - 1]);
    }

    /**
     * Return the names of the selected surfaces.
     * @return names of the selected surfaces.
     */
    public String[] getSelectedSurfaces() {
        int[] aiSelected = surfaceList.getSelectedIndices();
        String[] akNames = new String[aiSelected.length];
        DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
        for (int i = 0; i < aiSelected.length; i++) {
            akNames[i] = new String((String)kList.elementAt(aiSelected[i]));
        }
        return akNames;
    }


    /**
     * Turn surface texture on/off.
     * @param bTextureOn texture on/off.
     * @param bUseNewImage when true use the user-specified ModelImage, when false use default ModelImage.
     * @param bUseNewLUT when true use the user-specified LUT, when false use the defaulet LUT.
     */
    public void ImageAsTexture( boolean bTextureOn, boolean bUseNewImage, boolean bUseNewLUT )
    {
        int[] aiSelected = surfaceList.getSelectedIndices();
        if ( m_kVolumeViewer != null )
        {
            DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
            for (int i = 0; i < aiSelected.length; i++) {
                m_kVolumeViewer.setSurfaceTexture( (String)kList.elementAt(aiSelected[i]),
                        bTextureOn, bUseNewImage, bUseNewLUT);
            }
        }
        enableSurfacePaintCan(bTextureOn);
    }

    /**
     * Check if the surface pickable checkbox be selected or not.
     * @return  isSelected Surface pickable check box selected or not.
     */
    public boolean isSurfacePickableSelected() {
        return surfacePickableCB.isSelected();
    }

    /**
     * Resizing the control panel with ViewJFrameVolumeView's frame width and height.
     *
     * @param  panelWidth   int width
     * @param  frameHeight  int height
     */
    public void resizePanel(int panelWidth, int frameHeight) {
        frameHeight = frameHeight - (40 * 2);
        scroller.setPreferredSize(new Dimension(panelWidth, frameHeight));
        scroller.setSize(new Dimension(panelWidth, frameHeight));
        scroller.revalidate();
    }
    
    /* (non-Javadoc)
     * @see gov.nih.mipav.view.renderer.WildMagic.Interface.JInterfaceBase#setButtonColor(javax.swing.JButton, java.awt.Color)
     */
    public void setButtonColor(JButton _button, Color _color) {

        super.setButtonColor(_button, _color);

        if ( m_kVolumeViewer != null )
        {
            int[] aiSelected = surfaceList.getSelectedIndices();
            DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
            for (int i = 0; i < aiSelected.length; i++) {
                m_kVolumeViewer.setColor( (String)kList.elementAt(aiSelected[i]),
                        new ColorRGB( _color.getRed()/255.0f, 
                                _color.getGreen()/255.0f,
                                _color.getBlue()/255.0f ));
            }
        }
    }

    /**
     * Set the paint can color.
     * @param kDropperColor color.
     * @param kPickPoint picked point on the surface.
     */
    public void setDropperColor( ColorRGBA kDropperColor, Vector3f kPickPoint )
    {
        if ( m_kSurfacePaint != null )
        {
            m_kSurfacePaint.setDropperColor(kDropperColor, kPickPoint);
        }
    }

    /**
     * Set the user-specified ModelImage to use as the surface texture.
     * @param kImage ModelImage to use as the surface texture.
     */
    public void SetImageNew(  ModelImage kImage )
    {
        int[] aiSelected = surfaceList.getSelectedIndices();
        if ( m_kVolumeViewer != null )
        {
            DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
            for (int i = 0; i < aiSelected.length; i++) {
                m_kVolumeViewer.SetImageNew( (String)kList.elementAt(aiSelected[i]), kImage);
            }
        }
    }

    /**
     * Set the user-specified LUT for surface texture.
     * @param kLUT ModelLUT
     * @param kRGBT ModelRGB for color images.
     */
    public void SetLUTNew(  ModelLUT kLUT, ModelRGB kRGBT )
    {
        int[] aiSelected = surfaceList.getSelectedIndices();
        if ( m_kVolumeViewer != null )
        {
            DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
            for (int i = 0; i < aiSelected.length; i++) {
                m_kVolumeViewer.SetLUTNew( (String)kList.elementAt(aiSelected[i]), kLUT, kRGBT);
            }
        }
    }

    /**
     * Called from the JPanelSurfaceMAterialProperties.java dialog when the dialog is used to change the material
     * properties of a surface. The surface is determined by the index iIndex. The color button is set to the Material
     * diffuse color.
     *
     * @param  kMaterial  Material reference
     * @param  iIndex     int material index
     */
    public void setMaterial(MaterialState kMaterial, int iIndex) {
        colorButton.setBackground( new Color(kMaterial.Diffuse.R, kMaterial.Diffuse.G, kMaterial.Diffuse.B) );
        if ( m_kVolumeViewer != null )
        {
            DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
            m_kVolumeViewer.setMaterial( (String)kList.elementAt(iIndex), kMaterial);
        }
    }

    /* (non-Javadoc)
     * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
     */
    public void stateChanged(ChangeEvent event)
    {
        if (event.getSource() == opacitySlider)
        {
            setTransparency(surfaceList.getSelectedIndices());
        }

        if (event.getSource() == detailSlider) {

            if (detailSlider.getValueIsAdjusting()) {
                // Too many LOD changes occur if you always get the slider
                // value.  Wait until the user stops dragging the slider.

                // Maybe not. Comment out the next line to have the surface update quickly.
                // If the CLOD mesh holds a surface over time one could view the surface evolution !!!!
                return;
            }

            
            
            // value in [0,100], corresponds to percent of maximum LOD
            float fValue = 1.0f - (float)detailSlider.getValue() / (float)detailSlider.getMaximum();
            decimationPercentage = fValue * 100.0;
            
            int numTriangles = 0;
            // construct the lists of items whose LODs need to be changed
            int[] aiSelected = surfaceList.getSelectedIndices();
            TriMesh[] akSurfaces = new TriMesh[ aiSelected.length ];
            DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
            boolean found = false;
            if ( aiSelected.length == 0) return;
            
            for (int i = 0; i < aiSelected.length; i++) {

                if ( m_kMeshes.get(aiSelected[i]) instanceof ClodMesh )
                {
                    ClodMesh kCMesh = (ClodMesh)m_kMeshes.get(aiSelected[i]);
                    int iValue = (int)(fValue * kCMesh.GetMaximumLOD());
                    kCMesh.TargetRecord( iValue );
                    kCMesh.SelectLevelOfDetail();
                    numTriangles += kCMesh.GetTriangleQuantity();
                    triangleText.setText("" + numTriangles);
                }
                else 
                {
                    TriMesh kMesh = m_kMeshes.get(aiSelected[i]);
                    
                    try {
                    	 for ( int j = 0; j < tmesh.length; j++ ) {
                    		 
		                     if ( tmesh[j].GetName().equals(kMesh.GetName())) {
		                    	 found = true;
			                   	 tmesh[j].doDecimation(decimationPercentage);
			                   	 TriMesh mesh = new TriMesh(tmesh[j].getDecimatedVBuffer(), tmesh[j].getDecimatedIBuffer());
			                   	 mesh.SetName(kMesh.GetName());
			                   	 MaterialState kMaterial = new MaterialState();
			                   	 kMaterial.Emissive = new ColorRGB(ColorRGB.BLACK);
			                   	 kMaterial.Ambient = new ColorRGB(0.2f,0.2f,0.2f);
			                   	 kMaterial.Diffuse = new ColorRGB(ColorRGB.WHITE);
			                   	 kMaterial.Specular = new ColorRGB(ColorRGB.WHITE);
			                     kMaterial.Shininess = 32f;
			            		 mesh.AttachGlobalState(kMaterial);
			                     akSurfaces[i] = mesh;
			                     akSurfaces[i].UpdateMS();
			
			                     m_kVolumeViewer.removeSurface( (String)kList.elementAt(aiSelected[i]) );
			
			                     m_kMeshes.set(aiSelected[i], mesh);
			                     
			                     numTriangles = tmesh[j].getDecimatedIBuffer().GetIndexQuantity();
		                     } 
                    	 }
                   	} catch ( Exception e ) {
                   		e.printStackTrace();
                   	}	
                    
                    
                   
                }
            }
            if ( found ) {
	            numTriangles /= 3;
	            triangleText.setText("" + numTriangles);
	            m_kVolumeViewer.addSurface(akSurfaces);
	            // keep the current selected mesh type: fill, points, or lines. 
	            changePolyMode(polygonIndexToMode(polygonModeCB.getSelectedIndex()));
            }
            
        }
    }
    
    /**
     * Returns true if a surface exists in the Renderer.
     * @return true if a surface exists in the Renderer.
     */
    public boolean surfaceAdded()
    {
        DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
        if ( kList.size() > 0 )
        {
            return true;
        }
        return false;
    }
    
    /**
     * Toggle which type of Geodesic is displayed on the surface (Euclidian, Dijkstra, Geodesic).
     * @param iWhich type of Geodesic is displayed on the surface (Euclidian, Dijkstra, Geodesic).
     */
    public void toggleGeodesicPathDisplay(int iWhich) {
        int[] aiSelected = surfaceList.getSelectedIndices();
        if ( m_kVolumeViewer != null )
        {
            DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
            for (int i = 0; i < aiSelected.length; i++) {
                m_kVolumeViewer.toggleGeodesicPathDisplay( (String)kList.elementAt(aiSelected[i]),
                        iWhich);
            }
        }
    }    
    /* (non-Javadoc)
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent kEvent)
    {
        if ( m_kVolumeViewer != null )
        {
            int[] aiSelected = surfaceList.getSelectedIndices();
            if ( aiSelected.length == 1 )
            {
                DefaultListModel kList = (DefaultListModel)surfaceList.getModel();

                triangleText.setText(String.valueOf( m_kMeshes.get(aiSelected[0]).GetTriangleQuantity() ) );
                volumeText.setText(String.valueOf( m_kVolumeViewer.getVolume( (String)kList.elementAt(aiSelected[0]) ) ) );
                areaText.setText(String.valueOf( m_kVolumeViewer.getSurfaceArea( (String)kList.elementAt(aiSelected[0]) ) ) );              

                if ( m_kMeshes.get(aiSelected[0]) instanceof ClodMesh )
                {
                    decimateButton.setEnabled(false);
                    detailSlider.setEnabled(true);
                    detailLabel.setEnabled(true);
                }
                else
                {
                    decimateButton.setEnabled(true);
                    detailSlider.setEnabled(false);
                    detailLabel.setEnabled(false);                    
                }
            }
        }
    }

    /** 
     * Add polyline to the render.
     */
    private void addPolyline() {
    	Polyline[] akPolylines = FilePolyline_WM.openPolylines(m_kVolumeViewer.getImageA());
    	Vector3f m_kTranslate = m_kVolumeViewer.getVolumeGPU().getTranslate();
    	
    	 float m_fMax, m_fX, m_fY, m_fZ;
    	 float fMaxX = (m_kVolumeViewer.getImageA().getExtents()[0] - 1) * m_kVolumeViewer.getImageA().getFileInfo(0).getResolutions()[0];
         float fMaxY = (m_kVolumeViewer.getImageA().getExtents()[1] - 1) * m_kVolumeViewer.getImageA().getFileInfo(0).getResolutions()[1];
         float fMaxZ = (m_kVolumeViewer.getImageA().getExtents()[2] - 1) * m_kVolumeViewer.getImageA().getFileInfo(0).getResolutions()[2];

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
    	
    	if ( akPolylines != null )
        {
        	DefaultListModel kList = (DefaultListModel)polylineList.getModel();
            int iSize = kList.getSize();
            
            for ( int i = 0; i < akPolylines.length ; i++ ) {
            	polylineCounterList.add(iSize + i, polylineCounter);
            	
            	 
            	 for ( int j = 0; j < akPolylines[i].VBuffer.GetVertexQuantity(); j++ )
                 {

           		  akPolylines[i].VBuffer.SetPosition3(j, akPolylines[i].VBuffer.GetPosition3fX(j) - m_kTranslate.X,
           				  akPolylines[i].VBuffer.GetPosition3fY(j) - m_kTranslate.Y, 
           				  akPolylines[i].VBuffer.GetPosition3fZ(j) - m_kTranslate.Z );
           		   akPolylines[i].VBuffer.SetPosition3(j, 
           				akPolylines[i].VBuffer.GetPosition3fX(j) * 1.0f/m_fX,
           				akPolylines[i].VBuffer.GetPosition3fY(j) * 1.0f/m_fY,
           				akPolylines[i].VBuffer.GetPosition3fZ(j) * 1.0f/m_fZ);
                 } 
                
                akPolylines[i].Local.SetTranslate(new Vector3f(m_kTranslate.X, m_kTranslate.Y, m_kTranslate.Z));
            	m_kVolumeViewer.addPolyline(akPolylines[i], polylineCounter);
            	
            	polylineCounter++;
            }
            
            for ( int i = 0; i < akPolylines.length; i++ )
            {
                kList.add( iSize + i, akPolylines[i].GetName() );
               
            }
            polylineList.setSelectedIndex(iSize);
            
        }
    }
    
    /**
     * Build the toolbar.
     */
    private void buildToolBar() {

        ViewToolBarBuilder toolbarBuilder = new ViewToolBarBuilder(this);

        JToolBar toolBar = new JToolBar();
        toolBar.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
        toolBar.setFloatable(false);

        smooth1Button = toolbarBuilder.buildButton("Smooth", "Smooth level 1", "sm1");
        smooth2Button = toolbarBuilder.buildButton("Smooth2", "Smooth level 2", "sm2");
        smooth3Button = toolbarBuilder.buildButton("Smooth3", "Smooth level 3", "sm3");
        decimateButton = toolbarBuilder.buildButton("Decimate", "Decimate the surface", "decimate");
        levelSButton = toolbarBuilder.buildButton("LevelS", "Save single level (.sur)", "levelssave");
        levelVButton = toolbarBuilder.buildButton("LevelV", "Save single level (.wrl)", "levelvsave");
        levelWButton = toolbarBuilder.buildButton("LevelW", "Save multi objects (.wrl)", "levelwsave");
        levelXMLButton = toolbarBuilder.buildButton("LevelXML", "Save xml surface (.xml)", "savexml");
        levelSTLButton = toolbarBuilder.buildButton("LevelSTL", "Save STL binary surface (.stl)", "savestl");
        levelPLYButton = toolbarBuilder.buildButton("LevelPLY", "Save PLY surface (.ply)", "saveply");
        toolBar.add(smooth1Button);
        toolBar.add(smooth2Button);
        toolBar.add(smooth3Button);
        toolBar.add(decimateButton);
        toolBar.add(levelSButton);
        toolBar.add(levelVButton);
        toolBar.add(levelWButton);
        toolBar.add(levelXMLButton);
        toolBar.add(levelSTLButton);
        toolBar.add(levelPLYButton);

        JPanel toolBarPanel = new JPanel();
        toolBarPanel.setLayout(new BorderLayout());
        toolBarPanel.add(toolBar, BorderLayout.WEST);
        toolBarPanel.add(m_kSurfacePaint.getToolBar(), BorderLayout.SOUTH);

        mainPanel.add(toolBarPanel, BorderLayout.NORTH);
    }
    
    /**
     * Creates a label in the proper font and color.
     *
     * @param   title  The title of the label.
     *
     * @return  The new label.
     */
    private JLabel createLabel(String title) {
        JLabel label = new JLabel(title);

        label.setFont(MipavUtil.font12);
        label.setForeground(Color.black);

        return label;
    }

    /**
     * Decimate the selected surfaces.
     * @param  aiSelected   selected surfaces.
     */
    private void decimate(int[] aiSelected) {
        
        if ( m_kVolumeViewer != null )
        {
            TriMesh[] akSurfaces = new TriMesh[ aiSelected.length ];
           
            DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
            tmesh = new TriangleMesh[ aiSelected.length ];
            for (int i = 0; i < aiSelected.length; i++) {
            	
                TriMesh kMesh = m_kMeshes.get(aiSelected[i]);
                VertexBuffer kVBuffer = new VertexBuffer(kMesh.VBuffer);
                IndexBuffer kIBuffer = new IndexBuffer( kMesh.IBuffer);
                
                
                /*
                ClodCreator decimator = new ClodCreator(kVBuffer, kIBuffer);
               
                vertices = decimator.getVertices();
                indices = decimator.getIndices();
                record = decimator.getRecords();
               
        		int iVQuantity = kVBuffer.GetVertexQuantity();
                
                int m_iTQuantity = kIBuffer.GetIndexQuantity()/3;
                int[] m_aiConnect = kIBuffer.GetData();
                indices.get(m_aiConnect);
                kIBuffer = new IndexBuffer(m_aiConnect.length, m_aiConnect);
                
                WildMagic.LibGraphics.SceneGraph.lod.CollapseRecordArray records = new WildMagic.LibGraphics.SceneGraph.lod.CollapseRecordArray(record.length, record);
                WildMagic.LibGraphics.SceneGraph.lod.ClodMesh kClod = new WildMagic.LibGraphics.SceneGraph.lod.ClodMesh(kVBuffer, kIBuffer, records);
                
                */
                
                // CreateClodMesh kDecimator = new CreateClodMesh(kVBuffer, kIBuffer);

                // kDecimator.decimate();
                // ClodMesh kClod = new ClodMesh(kVBuffer, kIBuffer, kDecimator.getRecords());
                // kClod.SetName( kMesh.GetName() );
                tmesh[i] = new TriangleMesh(kVBuffer, kIBuffer);
                TriMesh mesh = new TriMesh(kVBuffer, kIBuffer);
                mesh.SetName( kMesh.GetName() );
                tmesh[i].SetName(kMesh.GetName());
                akSurfaces[i] = mesh;

                m_kVolumeViewer.removeSurface( (String)kList.elementAt(aiSelected[i]) );

                m_kMeshes.set(aiSelected[i], mesh);
            }


            m_kVolumeViewer.addSurface(akSurfaces);


            decimateButton.setEnabled(false);
            detailSlider.setEnabled(true);
            detailLabel.setEnabled(true);

            for (int j = 0; j < detailSliderLabels.length; j++) {
                detailSliderLabels[j].setEnabled(true);
            }
        }
    }

    /**
     * Display the Surface Material dialog for the selected surfaces.
     * @param aiSelected the selected surfaces.
     */
    private void displayAdvancedMaterialOptions(int[] aiSelected) {
        if ( m_kVolumeViewer != null )
        {
            DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
            for (int i = 0; i < aiSelected.length; i++) {
                new JFrameSurfaceMaterialProperties_WM(this, aiSelected[i], m_kVolumeViewer.GetLights(),
                        m_kVolumeViewer.getMaterial( (String)kList.elementAt(aiSelected[i]) ) );
            }
        }
    }
   
    /**
     * Initializes the GUI components.
     */
    private void init() {
        // setSize(400, 256);

        // Scroll panel that hold the control panel layout in order to use JScrollPane
        JPanel mainScrollPanel = new JPanel();
        mainScrollPanel.setLayout(new BorderLayout());

        scroller = new JScrollPane(mainScrollPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        mainPanel = new JPanel(new BorderLayout());
        buildToolBar();

        // Layout
        //
        // +-----------------------------------+
        // |                       color button|
        // |   surfaceList         opacity     |
        // |                       shininess   |
        // |                       triangles   |
        // |                       LOD slider  |
        // |   add  remove         light button|
        // +-----------------------------------+

        JPanel buttonPanel = new JPanel();

        // buttons for add/remove of surfaces from list
        JButton addButton = new JButton("Add");
        addButton.addActionListener(this);
        addButton.setActionCommand("Add");
        addButton.setFont(MipavUtil.font12B);
        addButton.setPreferredSize(MipavUtil.defaultButtonSize);

        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(this);
        removeButton.setActionCommand("Remove");
        removeButton.setFont(MipavUtil.font12B);
        removeButton.setPreferredSize(MipavUtil.defaultButtonSize);
        
        JButton updatePlanes = new JButton("Update Planes");
        updatePlanes.addActionListener(this);
        updatePlanes.setActionCommand("updatePlanes");
        updatePlanes.setFont(MipavUtil.font12B);
        updatePlanes.setPreferredSize(MipavUtil.defaultButtonSize);

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(updatePlanes);

        // list panel for surface filenames
        surfaceList = new JList(new DefaultListModel());
        surfaceList.addListSelectionListener(this);
        surfaceList.setPrototypeCellValue("aaaaaaaaaaaaaaaa.aaa    ");

        JScrollPane kScrollPane = new JScrollPane(surfaceList);
        JPanel scrollPanel = new JPanel();

        scrollPanel.setLayout(new BorderLayout());
        scrollPanel.add(kScrollPane);
        scrollPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BorderLayout());
        listPanel.add(scrollPanel, BorderLayout.CENTER);
        listPanel.add(buttonPanel, BorderLayout.SOUTH);
        listPanel.setBorder(buildTitledBorder("Surface list"));

        // Polyline start
        JPanel buttonPanelPolyline = new JPanel();

        // buttons for add/remove of surfaces from list
        JButton addButtonPolyline = new JButton("Add");

        addButtonPolyline.addActionListener(this);
        addButtonPolyline.setActionCommand("AddPolyline");
        addButtonPolyline.setFont(MipavUtil.font12B);
        addButtonPolyline.setPreferredSize(MipavUtil.defaultButtonSize);

        JButton removeButtonPolyline = new JButton("Remove");

        removeButtonPolyline.addActionListener(this);
        removeButtonPolyline.setActionCommand("RemovePolyline");
        removeButtonPolyline.setFont(MipavUtil.font12B);
        removeButtonPolyline.setPreferredSize(MipavUtil.defaultButtonSize);

        buttonPanelPolyline.add(addButtonPolyline);
        buttonPanelPolyline.add(removeButtonPolyline);

        // list panel for surface filenames
        polylineList = new JList(new DefaultListModel());
        polylineList.addListSelectionListener(this);
        polylineList.setPrototypeCellValue("aaaaaaaaaaaaaaaa.aaa    ");

        JScrollPane kScrollPanePolyline = new JScrollPane(polylineList);
        JPanel scrollPanelPolyline = new JPanel();

        scrollPanelPolyline.setLayout(new BorderLayout());
        scrollPanelPolyline.add(kScrollPanePolyline);
        scrollPanelPolyline.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel listPanelPolyline = new JPanel();
        listPanelPolyline.setLayout(new BorderLayout());
        listPanelPolyline.add(scrollPanelPolyline, BorderLayout.CENTER);
        listPanelPolyline.add(buttonPanelPolyline, BorderLayout.SOUTH);
        listPanelPolyline.setBorder(buildTitledBorder("Polyline list"));
        // polyline end
        
        JPanel paintTexturePanel = new JPanel();
        paintTexturePanel.setLayout(new FlowLayout(FlowLayout.LEFT));


        /* Creates the surface paint button, which launches the surface
         * dialog: */
        m_kSurfaceTextureButton = new JButton("Surface Texture");
        m_kSurfaceTextureButton.addActionListener(this);
        m_kSurfaceTextureButton.setActionCommand("SurfaceTexture");
        m_kSurfaceTextureButton.setSelected(false);
        m_kSurfaceTextureButton.setEnabled(false);
        paintTexturePanel.add(m_kSurfaceTextureButton);

        colorButton = new JButton("   ");
        colorButton.setToolTipText("Change surface color");
        colorButton.addActionListener(this);
        colorButton.setActionCommand("ChangeColor");

        colorLabel = new JLabel("Surface color");
        colorLabel.setFont(MipavUtil.font12B);
        colorLabel.setForeground(Color.black);

        JPanel colorPanel = new JPanel();
        colorPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        colorPanel.add(colorButton);
        colorPanel.add(colorLabel);

        /* Creates the advanced material options button, which launches the
         * material editor dialog: */
        m_kAdvancedMaterialOptionsButton = new JButton("Advanced Options");
        m_kAdvancedMaterialOptionsButton.setToolTipText("Change surface material properties");
        m_kAdvancedMaterialOptionsButton.addActionListener(this);
        m_kAdvancedMaterialOptionsButton.setActionCommand("AdvancedMaterialOptions");
        m_kAdvancedMaterialOptionsButton.setEnabled(false);
        colorPanel.add(m_kAdvancedMaterialOptionsButton);

        // Slider for changing opacity; not currently enabled.
        opacityLabel = new JLabel("Opacity");
        opacityLabel.setFont(MipavUtil.font12B);
        opacityLabel.setForeground(Color.black);

        opacitySliderLabels = new JLabel[3];
        detailSliderLabels = new JLabel[3];
        opacitySliderLabels[0] = createLabel("0");
        opacitySliderLabels[1] = createLabel("50");
        opacitySliderLabels[2] = createLabel("100");
        detailSliderLabels[0] = createLabel("0");
        detailSliderLabels[1] = createLabel("50");
        detailSliderLabels[2] = createLabel("100");

        Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();

        labels.put(new Integer(0), opacitySliderLabels[0]);
        labels.put(new Integer(50), opacitySliderLabels[1]);
        labels.put(new Integer(100), opacitySliderLabels[2]);

        opacitySlider = new JSlider(0, 100, 100);
        opacitySlider.setFont(MipavUtil.font12);
        opacitySlider.setMinorTickSpacing(10);
        opacitySlider.setPaintTicks(true);
        opacitySlider.addChangeListener(this);
        opacitySlider.setLabelTable(labels);
        opacitySlider.setPaintLabels(true);
        opacitySlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        opacityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        opacitySlider.setEnabled(true);
        opacityLabel.setEnabled(true);
        opacitySliderLabels[0].setEnabled(true);
        opacitySliderLabels[1].setEnabled(true);
        opacitySliderLabels[2].setEnabled(true);

        triangleLabel = new JLabel("Number of triangles");
        triangleLabel.setFont(MipavUtil.font12B);
        triangleLabel.setForeground(Color.black);
        triangleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        triangleText = new JTextField(10);
        triangleText.setEditable(false);
        triangleText.setBorder(new EmptyBorder(triangleText.getInsets()));
        triangleText.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel trianglePanel = new JPanel();

        trianglePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        trianglePanel.add(triangleLabel);
        trianglePanel.add(triangleText);
        trianglePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        volumeLabel = new JLabel("Volume of mesh");
        volumeLabel.setFont(MipavUtil.font12B);
        volumeLabel.setForeground(Color.black);
        volumeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        volumeText = new JTextField(10);
        volumeText.setEditable(false);
        volumeText.setBorder(new EmptyBorder(volumeText.getInsets()));
        volumeText.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel volumePanel = new JPanel();

        volumePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        volumePanel.add(volumeLabel);
        volumePanel.add(volumeText);
        volumePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        areaLabel = new JLabel("Surface area");
        areaLabel.setFont(MipavUtil.font12B);
        areaLabel.setForeground(Color.black);
        areaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        areaText = new JTextField(10);
        areaText.setEditable(false);
        areaText.setBorder(new EmptyBorder(areaText.getInsets()));
        areaText.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel areaPanel = new JPanel();

        areaPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        areaPanel.add(areaLabel);
        areaPanel.add(areaText);
        areaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Slider for changing level of detail.  Range is [0,100] with initial
        // value 100.
        detailLabel = new JLabel("Level of Detail");
        detailLabel.setFont(MipavUtil.font12B);
        detailLabel.setForeground(Color.black);

        Hashtable<Integer,JLabel> labels2 = new Hashtable<Integer,JLabel>();

        labels2.put(new Integer(0), detailSliderLabels[0]);
        labels2.put(new Integer(50), detailSliderLabels[1]);
        labels2.put(new Integer(100), detailSliderLabels[2]);

        detailSlider = new JSlider(0, 100, 100);
        detailSlider.setFont(MipavUtil.font12);
        detailSlider.setMinorTickSpacing(10);
        detailSlider.setPaintTicks(true);
        detailSlider.addChangeListener(this);
        detailSlider.setLabelTable(labels2);
        detailSlider.setPaintLabels(true);
        detailSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailSlider.setEnabled(false);
        detailLabel.setEnabled(false);
        detailSliderLabels[0].setEnabled(false);
        detailSliderLabels[1].setEnabled(false);
        detailSliderLabels[2].setEnabled(false);

        JPanel sliderPanel = new JPanel();

        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
        sliderPanel.add(opacityLabel);
        sliderPanel.add(opacitySlider);
        sliderPanel.add(trianglePanel);
        sliderPanel.add(volumePanel);
        sliderPanel.add(areaPanel);
        sliderPanel.add(detailLabel);
        sliderPanel.add(detailSlider);

        comboLabel = new JLabel("Polygon mode:");
        comboLabel.setFont(MipavUtil.font12B);
        comboLabel.setForeground(Color.black);

        polygonModeCB = new JComboBox(new String[] { "Fill", "Line", "Point" });
        polygonModeCB.addActionListener(this);
        polygonModeCB.setActionCommand("ChangePolyMode");
        polygonModeCB.setAlignmentX(Component.LEFT_ALIGNMENT);
        polygonModeCB.setFont(MipavUtil.font12);
        polygonModeCB.setBackground(Color.white);

        surfacePickableCB = new JCheckBox("Surface Pickable", true);
        surfacePickableCB.addActionListener(this);
        surfacePickableCB.setActionCommand("SurfacePickable");
        surfacePickableCB.setFont(MipavUtil.font12B);
        surfacePickableCB.setSelected(false);

        surfaceClipCB = new JCheckBox("Surface Clipping", true);
        surfaceClipCB.addActionListener(this);
        surfaceClipCB.setActionCommand("Clipping");
        surfaceClipCB.setFont(MipavUtil.font12B);
        surfaceClipCB.setSelected(false);
        surfaceClipCB.setEnabled(false);

        surfaceBackFaceCB = new JCheckBox("Backface Culling", true);
        surfaceBackFaceCB.addActionListener(this);
        surfaceBackFaceCB.setActionCommand("backface");
        surfaceBackFaceCB.setFont(MipavUtil.font12B);
        surfaceBackFaceCB.setSelected(true);

        surfaceTransparencyCB = new JCheckBox("Transparency", true);
        surfaceTransparencyCB.addActionListener(this);
        surfaceTransparencyCB.setActionCommand("transparency");
        surfaceTransparencyCB.setFont(MipavUtil.font12B);
        surfaceTransparencyCB.setSelected(true);
        
        JPanel cbSurfacePanel = new JPanel();
        cbSurfacePanel.setLayout(new BoxLayout(cbSurfacePanel, BoxLayout.Y_AXIS));
        cbSurfacePanel.add(surfacePickableCB);
        cbSurfacePanel.add(surfaceClipCB);
        cbSurfacePanel.add(surfaceBackFaceCB);
        cbSurfacePanel.add(surfaceTransparencyCB);

        JPanel cbPanel = new JPanel();
        cbPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        cbPanel.add(comboLabel);
        cbPanel.add(polygonModeCB);
        cbPanel.add(cbSurfacePanel);

        paintTexturePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        colorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sliderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        colorPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        sliderPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        cbPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.add(colorPanel);
        optionsPanel.add(paintTexturePanel);
        optionsPanel.add(sliderPanel);
        optionsPanel.add(cbPanel);
        optionsPanel.setBorder(buildTitledBorder("Surface options"));

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.add(optionsPanel, BorderLayout.NORTH);

        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(MipavUtil.font12B);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        tabbedPane.addChangeListener(this);
        
        tabbedPane.addTab("Surface", null, listPanel);
        // tabbedPane.addTab("Polyline", null, listPanelPolyline);
        tabbedPane.setSelectedIndex(0);
     
        Box contentBox = new Box(BoxLayout.Y_AXIS);

        contentBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        contentBox.add(tabbedPane);
        contentBox.add(rightPanel);
 
        mainScrollPanel.add(contentBox, BorderLayout.NORTH);

        mainPanel.add(scroller, BorderLayout.CENTER);

        // no surfaces yet, so the elements shouldn't be enabled
        setElementsEnabled(false);
 
        
    }

    /**
     * Convert from the polygon mode combo-box list index to the PolygonAttributes.POLYGON_LINE,
     * PolygonAttributes.POLYGON_POINT, and PolygonAttributes.POLYGON_FILL values:
     *
     * @param   index  the index of the selected polygon mode in the polygonModeCB combo box.
     *
     * @return  the corresponding PolygonAttributes defined value.
     */
    private WireframeState.FillMode polygonIndexToMode(int index) {
        switch (index) {

            case 1:
                return WireframeState.FillMode.FM_LINE;

            case 2:
                return WireframeState.FillMode.FM_POINT;

            case 0:
            default:
                return WireframeState.FillMode.FM_FILL;
        }
    }

    /**
     * Remove polyline from the render
     */
    private void removePolyline() {
    	int[] aiSelected = polylineList.getSelectedIndices();
    	int index;
        DefaultListModel kList = (DefaultListModel)polylineList.getModel();
        if ( m_kVolumeViewer != null )
        {
            for (int i = 0; i < aiSelected.length; i++) {
            	index = (Integer)(polylineCounterList.elementAt(aiSelected[i]));
                m_kVolumeViewer.removePolyline( index );
                polylineCounterList.remove(aiSelected[i]);
            }
        }
        for (int i = 0; i < aiSelected.length; i++) {
            kList.remove(aiSelected[i]);
        }
    }

    /**
     * Remove the selected surfaces.
     */
    private void removeSurface()
    {
        int[] aiSelected = surfaceList.getSelectedIndices();

        DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
        if ( m_kVolumeViewer != null )
        {
            for (int i = 0; i < aiSelected.length; i++) {
                m_kVolumeViewer.removeSurface( (String)kList.elementAt(aiSelected[i]) );
            }
        }
        for (int i = 0; i < aiSelected.length; i++) {
            kList.remove(aiSelected[i]);
            m_kMeshes.remove(aiSelected[i]);
        }
    }
    
    /**
     * Save the selected surfaces. The kCommand parameter determines the file type.
     * @param aiSelected selected surfaces.
     * @param kCommand save command, specifies the file type.
     */
    private void saveSurfaces( int[] aiSelected, String kCommand )
    {

        if (aiSelected == null) {
            MipavUtil.displayError("Select a surface to save.");
            return;
        }

        if ( m_kVolumeViewer != null )
        {
            TriMesh[] akSurfaces = new TriMesh[ aiSelected.length ];
            for (int i = 0; i < aiSelected.length; i++) {
                akSurfaces[i] = m_kMeshes.get(aiSelected[i]);
            }
            FileSurface_WM.saveSurfaces(m_kVolumeViewer.getImageA(), akSurfaces, kCommand );
        }
    }
    
    /**
     * Turn backface culling on/off for the selected surfaces.
     * @param aiSelected selected surfaces.
     */
    private void setBackface(int[] aiSelected) {
        if ( m_kVolumeViewer != null )
        {
            DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
            for (int i = 0; i < aiSelected.length; i++) {
                m_kVolumeViewer.setBackface( (String)kList.elementAt(aiSelected[i]),
                        surfaceBackFaceCB.isSelected());
            }

        }
    }
    
    /**
     * Turns Clipping on/off for the selected surfaces.
     * @param  aiSelected  the list of selected surfaces (SurfaceAttributes)
     */
    private void setClipping(int[] aiSelected) {
        if ( m_kVolumeViewer != null )
        {
            DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
            for (int i = 0; i < aiSelected.length; i++) {
                m_kVolumeViewer.setClipping( (String)kList.elementAt(aiSelected[i]),
                                             surfaceClipCB.isSelected());
            }

        }
    }
    
    /**
     * Sets the surface options GUI panel to enabled or disabled. If there are 0 or multiple surfaces selected, all the
     * options should be disabled.
     *
     * @param  flag  Enable or disable.
     */
    private void setElementsEnabled(boolean flag) {
        levelSButton.setEnabled(flag);
        levelVButton.setEnabled(flag);
        levelWButton.setEnabled(flag);
        decimateButton.setEnabled(flag);
        colorButton.setEnabled(flag);
        smooth1Button.setEnabled(flag);
        smooth2Button.setEnabled(flag);
        smooth3Button.setEnabled(flag);
        levelXMLButton.setEnabled(flag);
        levelSTLButton.setEnabled(flag);
        levelPLYButton.setEnabled(flag);

        colorLabel.setEnabled(flag);
        m_kAdvancedMaterialOptionsButton.setEnabled(flag);
        m_kSurfaceTextureButton.setEnabled(flag);
        opacityLabel.setEnabled(flag);
        opacitySlider.setEnabled(flag);
        triangleLabel.setEnabled(flag);
        triangleText.setEnabled(flag);
        volumeLabel.setEnabled(flag);
        volumeText.setEnabled(flag);
        areaLabel.setEnabled(flag);
        areaText.setEnabled(flag);

        for (int i = 0; i < opacitySliderLabels.length; i++) {
            opacitySliderLabels[i].setEnabled(flag);
        }

        polygonModeCB.setEnabled(flag);
        comboLabel.setEnabled(flag);
        surfacePickableCB.setEnabled(flag);
        surfaceClipCB.setEnabled(flag);
        surfaceBackFaceCB.setEnabled(flag);
        surfaceTransparencyCB.setEnabled(flag);
        m_kSurfacePaint.setEnabled(flag);
    }
    
    /**
     * Turn picking culling on/off for the selected surfaces.
     * @param aiSelected selected surfaces.
     */
    private void setPickable(int[] aiSelected) {
        if ( m_kVolumeViewer != null )
        {
            DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
            for (int i = 0; i < aiSelected.length; i++) {
                m_kVolumeViewer.setPickable( (String)kList.elementAt(aiSelected[i]),
                        surfacePickableCB.isSelected());
            }
        }
    }
    
    /**
     * Turns Transparency on/off for the selected surfaces.
     * @param  aiSelected  the list of selected surfaces (SurfaceAttributes)
     */
    private void setTransparency(int[] aiSelected) {

        if ( m_kVolumeViewer != null )
        {
            if ( surfaceTransparencyCB.isSelected() )
            {
                float opacity = opacitySlider.getValue() / 100.0f;
                DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
                for (int i = 0; i < aiSelected.length; i++) {
                    m_kVolumeViewer.setTransparency( (String)kList.elementAt(aiSelected[i]), opacity);
                }
            }
        }
    }


    /**
     * Smoothes the selected surfaces. One dialog per group of selected surfaces is displayed (not a different dialog
     * per-surface).
     *
     * @param  aiSelected    the list of selected surfaces (SurfaceAttributes)
     * @param  iSmoothType  the level of smoothing JDialogSmoothMesh.SMOOTH1, JDialogSmoothMesh.SMOOTH2, or
     *                     JDialogSmoothMesh.SMOOTH3
     */
    private void smoothSurface(int[] aiSelected, int iSmoothType )
    {

        if (aiSelected == null) {
            MipavUtil.displayError("Select a surface to smooth.");
            return;
        }

        JDialogSmoothMesh dialog = new JDialogSmoothMesh(null, true, iSmoothType);

        if (dialog.isCancelled()) {
            return;
        }

        if ( m_kVolumeViewer != null )
        {
            DefaultListModel kList = (DefaultListModel)surfaceList.getModel();
            
            int numTriangles = 0;
            float volume = 0;
            float area = 0;
            
            for (int i = 0; i < aiSelected.length; i++) {

                TriMesh kMesh = m_kMeshes.get(aiSelected[i]);
                int iTarget = 0;
                if (kMesh instanceof ClodMesh) {
                    iTarget = ((ClodMesh)kMesh).TargetRecord();
                    ((ClodMesh)kMesh).TargetRecord(0);
                    ((ClodMesh)kMesh).SelectLevelOfDetail();
                }
                
                if (iSmoothType == JDialogSmoothMesh.SMOOTH1) {
                    m_kVolumeViewer.smoothMesh((String)kList.elementAt(aiSelected[i]),
                            dialog.getIterations(),
                            dialog.getAlpha(),
                            dialog.getVolumeLimit(),
                            dialog.getVolumePercent());
                }
                else if (iSmoothType == JDialogSmoothMesh.SMOOTH2) {
                    m_kVolumeViewer.smoothTwo((String)kList.elementAt(aiSelected[i]),
                            dialog.getIterations(), dialog.getStiffness(), dialog.getVolumeLimit(),
                                        dialog.getVolumePercent());
                }

                else {
                    m_kVolumeViewer.smoothThree((String)kList.elementAt(aiSelected[i]),
                            dialog.getIterations(), dialog.getLambda(), dialog.getMu());
                }

/*
                numTriangles += meshes[j].getIndexCount();
                volume += meshes[j].volume();
                area += meshes[j].area();
                */
                
                if ((kMesh instanceof ClodMesh) && (iTarget != 0 )) {
                    ((ClodMesh)kMesh).TargetRecord(iTarget);
                    ((ClodMesh)kMesh).SelectLevelOfDetail();
                }
            }

            triangleText.setText(String.valueOf(numTriangles / 3));

            // One length across the extracted surface changes from -1 to 1
            // while one length across the actual volume changes by ((dim-1)*res)max
            volumeText.setText("" + volume);
            areaText.setText("" + area);
        }
    }
}
