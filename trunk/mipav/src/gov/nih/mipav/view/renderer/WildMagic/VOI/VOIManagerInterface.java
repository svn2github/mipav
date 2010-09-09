package gov.nih.mipav.view.renderer.WildMagic.VOI;

import gov.nih.mipav.MipavCoordinateSystems;
import gov.nih.mipav.model.algorithms.AlgorithmVOIExtraction;
import gov.nih.mipav.model.algorithms.AlgorithmVOIExtractionPaint;
import gov.nih.mipav.model.algorithms.utilities.AlgorithmFlip;
import gov.nih.mipav.model.algorithms.utilities.AlgorithmRotate;
import gov.nih.mipav.model.file.FileInfoBase;
import gov.nih.mipav.model.file.FileInfoDicom;
import gov.nih.mipav.model.file.FileUtility;
import gov.nih.mipav.model.file.FileVOI;
import gov.nih.mipav.model.provenance.ProvenanceRecorder;
import gov.nih.mipav.model.scripting.ScriptRecorder;
import gov.nih.mipav.model.scripting.actions.ActionMaskToPaint;
import gov.nih.mipav.model.scripting.actions.ActionMaskToVOI;
import gov.nih.mipav.model.scripting.actions.ActionOpenAllVOIs;
import gov.nih.mipav.model.scripting.actions.ActionOpenVOI;
import gov.nih.mipav.model.scripting.actions.ActionPaintToVOI;
import gov.nih.mipav.model.scripting.actions.ActionSaveAllVOIs;
import gov.nih.mipav.model.scripting.actions.ActionVOIToMask;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.model.structures.ModelLUT;
import gov.nih.mipav.model.structures.ModelRGB;
import gov.nih.mipav.model.structures.ModelStorageBase;
import gov.nih.mipav.model.structures.TransMatrix;
import gov.nih.mipav.model.structures.UpdateVOIEvent;
import gov.nih.mipav.model.structures.UpdateVOISelectionListener;
import gov.nih.mipav.model.structures.VOI;
import gov.nih.mipav.model.structures.VOIBase;
import gov.nih.mipav.model.structures.VOIPoint;
import gov.nih.mipav.model.structures.VOIPolyLineSlice;
import gov.nih.mipav.model.structures.VOIVector;
import gov.nih.mipav.model.structures.event.VOIEvent;
import gov.nih.mipav.model.structures.event.VOIListener;
import gov.nih.mipav.model.structures.event.VOIVectorEvent;
import gov.nih.mipav.model.structures.event.VOIVectorListener;
import gov.nih.mipav.view.CustomUIBuilder;
import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.Preferences;
import gov.nih.mipav.view.VOIHandlerInterface;
import gov.nih.mipav.view.ViewImageFileFilter;
import gov.nih.mipav.view.ViewJColorChooser;
import gov.nih.mipav.view.ViewJFrameBase;
import gov.nih.mipav.view.ViewJFrameGraph;
import gov.nih.mipav.view.ViewJFrameImage;
import gov.nih.mipav.view.ViewJPopupPt;
import gov.nih.mipav.view.ViewJPopupVOI;
import gov.nih.mipav.view.ViewJProgressBar;
import gov.nih.mipav.view.ViewOpenVOIUI;
import gov.nih.mipav.view.ViewToolBarBuilder;
import gov.nih.mipav.view.ViewUserInterface;
import gov.nih.mipav.view.ViewVOIVector;
import gov.nih.mipav.view.dialogs.JDialogAGVF;
import gov.nih.mipav.view.dialogs.JDialogBSmooth;
import gov.nih.mipav.view.dialogs.JDialogBSnake;
import gov.nih.mipav.view.dialogs.JDialogBase;
import gov.nih.mipav.view.dialogs.JDialogFlip;
import gov.nih.mipav.view.dialogs.JDialogGVF;
import gov.nih.mipav.view.dialogs.JDialogIntensityThreshold;
import gov.nih.mipav.view.dialogs.JDialogLivewire;
import gov.nih.mipav.view.dialogs.JDialogMask;
import gov.nih.mipav.view.dialogs.JDialogOpacityControls;
import gov.nih.mipav.view.dialogs.JDialogPointArea;
import gov.nih.mipav.view.dialogs.JDialogSaveMergedVOIs;
import gov.nih.mipav.view.dialogs.JDialogSnake;
import gov.nih.mipav.view.dialogs.JDialogSurfaceReconstruction;
import gov.nih.mipav.view.dialogs.JDialogTrim;
import gov.nih.mipav.view.dialogs.JDialogVOIShapeInterpolation;
import gov.nih.mipav.view.dialogs.JDialogVOIStatistics;
import gov.nih.mipav.view.dialogs.JDialogVOIStats;
import gov.nih.mipav.view.renderer.WildMagic.ProstateFramework.JDialogLoadProstateMask;
import gov.nih.mipav.view.renderer.WildMagic.ProstateFramework.JDialogProstateFeaturesClassification;
import gov.nih.mipav.view.renderer.WildMagic.ProstateFramework.JDialogProstateFeaturesTrain;
import gov.nih.mipav.view.renderer.WildMagic.ProstateFramework.JDialogProstateSaveFeatures;


import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.BitSet;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.event.EventListenerList;

import WildMagic.LibFoundation.Mathematics.ColorRGB;
import WildMagic.LibFoundation.Mathematics.ColorRGBA;
import WildMagic.LibFoundation.Mathematics.Vector3f;


/**
 * VOIManagerInterface manages all the VOI user-interaction for classes that display the ModelImage.
 * ViewJFrameImage, ViewJFrameTriImage, ViewJFrameRegistration, ViewJFrameRegistrationTool, and
 * the VolumeTriPlanarInterface class for the GPU-based volume renderer.
 * 
 * This class further divides the VOI user-interaction into actions that occur within a single canvas and actions
 * that are shared across canvases.  All user-interaction that occurs within a single canvas, for example
 * direct user-manipulation of a single VOI contour, adding points, moving points, etc. are managed by the 
 * VOIManager class.  The ViewJFrameTriImage has three canvases when displaying a single ModelImage. Each
 * canvas is tied to a separate VOIManager class, which handles all the direct user-manipulation of the VOIs 
 * it displays. The VOIManagerInterface class contains the three VOIManagers and handles the communication 
 * between the VOIManager and the VOI, the ModelImage containing the VOI, and the class displaying the ModelImage. 
 * @see VOIManager.
 * 
 * In addition to managing the multiple VOIManager objects, this class handles all the VOI ActionEvents
 * generated by the VOI toolbars, VOI menu, and VOI popup interfaces.  Although this class provides many
 * public functions, the best way to communicate to this class is through the actionPerformed(ActionEvent)
 * function. Button parameters and ActionCommands are defined in the CustomUIBuilder class.
 * @see CustomUIBuilder. 
 *
 */
public class VOIManagerInterface implements ActionListener, VOIHandlerInterface, VOIListener, VOIVectorListener
{
    /**
     * Pick up the selected color and call method to change the color.
     */
    class OkColorListener implements ActionListener {

        /** Color Button */
        JButton button;

        /**
         * Creates a new OkColorListener object.
         * @param  color button
         */
        OkColorListener(JButton _button) {
            super();
            button = _button;
        }

        /**
         * Get color from chooser and set button and color.
         * @param  e  Event that triggered function.
         */
        public void actionPerformed(ActionEvent e) {
            saveVOIs( CustomUIBuilder.PARAM_VOI_COLOR.getActionCommand() );

            Color color = colorChooser.getColor();

            button.setBackground(color);
            setButtonColor(button, color);
        }
    }
    
    /** Reference to the parent frame. */
    private VOIManagerInterfaceListener m_kParent = null;
    /** Reference to imageA */
    private ModelImage m_kImageA;
    /** Reference to imageB */
    private ModelImage m_kImageB;
    /** The toolbar builder that constructs the VOI toolbar -- it stores the VOI color button. */
    private ViewToolBarBuilder toolbarBuilder;
    /** Reference to the VOI toolbar. */
    private JToolBar m_kVOIToolbar;
    /** Reference to the color chooser. */
    protected ViewJColorChooser colorChooser;
    /** List of VOIManagers, one per canvas displayed. */
    private Vector<VOIManager> m_kVOIManagers;
    /** The index of the current active VOIManager. */
    private int m_iActive = 0;
    /** VOI ID used to set the VOI color button on a call to newVOI() */
    private int voiUID = 0;
    /** The current active VOI */
    private VOI m_kCurrentVOIGroup = null;
    /** List of undo commands */
    private Vector<String> m_kUndoCommands = new Vector<String>();
    /** List of re-do commands */
    private Vector<String> m_kRedoCommands = new Vector<String>();
    /** Opacity for setting the JDialogOpacityControls, used for VOI opacity. */
    private float m_fOpacity = 1f;
    /** Set to true if this VOIManagerInterface is used for the GPU-based Volume Renderer */
    private boolean m_bGPURenderer = false;
    /** VOI Properties dialog -- from the popup menu or drop-down menu. */
    private JDialogVOIStats m_kVOIDialog;
    /** Saved VOI states for undo. */
    private Vector<VOISaveState> m_kUndoList = new Vector<VOISaveState>();
    /** Saved VOI states for re-do. */
    private Vector<VOISaveState> m_kRedoList = new Vector<VOISaveState>();

    /** Single-level undo/redo for image masks, undo-imageA: */
    private Object m_kImageAUndo = null;
    /** Single-level undo/redo for image masks, re-do-imageA: */
    private Object m_kImageARedo = null;
    /** Single-level undo/redo for image masks, undo-imageB: */
    private Object m_kImageBUndo = null;
    /** Single-level undo/redo for image masks, re-do-imageB: */
    private Object m_kImageBRedo = null;

    /** Bounding box for a group of selected VOIs for group-move. */
    private Vector3f[] m_akBounds = new Vector3f[]{ new Vector3f(), new Vector3f() };

    /** Set to false to disable VOI interaction (not currently used). */
    private boolean m_bEnabled = true;

    /**
     * created to handle VOI updates. Must fireVOIUpdate(...) to get listeners
     * to handle the update. Perhaps better location for the VOIupdate is in
     * <code>ViewJCompoenentEditImage</code>, but this listenerlist will handle
     * listeners of more than one type.
     */
    protected EventListenerList listenerList = new EventListenerList();

    /** Popup Menu for VOIs (non-point). */
    protected ViewJPopupVOI popup = null;

    /** Popup Menu for VOIPoints. */
    protected ViewJPopupPt popupPt = null;

    /** Restores the VOI color button after QuickLUT. */
    protected Color currentColor = null;
    /** Restores the current VOI after QuickLUT. */
    protected VOI saveGroup = null;

    /** List of active VOIBase for a moving several selected VOIs. */
    private Vector<VOIBase> m_kActiveList = new Vector<VOIBase>();

    /** The Default pointer button is set from outside this class if the 
     * default pointer button is not part of the VOI toolbar, or if the VOI toolbar is not displayed.
     * The VOIManagers query the default pointer button to determine if VOI interaction is enabled. */
    private JToggleButton m_kPointerButton = null;

    /** Used in the save VOI functions */
    private String voiSavedFileName = null;
    
    /** Statistics dialog VOI->Statistics generator... */
    private JDialogVOIStatistics imageStatList;
    
    private float presetHue = -1.0f;
    
    private boolean m_bDefaultImage;
    private ModelImage m_kTempImage = null;

    /**
     * Creates a VOIManagerInterface object.
     * @param kParent the parent frame, must be a VOIManagerInterfaceListener
     * @param kImageA imageA
     * @param kImageB imageB
     * @param iNViews number of views displayed in the parent.
     * @param bGPU set to true if this VOIManagerInterface is part of the GPU-based Volume Renderer.
     * @param kVOIGroup for ViewJFrameImage and ViewJFrameTriImage, so the VOI Toolbar can be part of a larger button group.
     */
    public VOIManagerInterface ( VOIManagerInterfaceListener kParent,
            ModelImage kImageA, ModelImage kImageB, int iNViews, boolean bGPU, ButtonGroup kVOIGroup )
    {
        m_kParent = kParent;
        m_kImageA = kImageA;
        m_kImageB = kImageB;        

        toolbarBuilder = new ViewToolBarBuilder(this);
        m_kVOIToolbar =
            toolbarBuilder.buildVolumeTriPlanarVOIToolBar( m_kImageA.getNDims(),
                    -1, bGPU, bGPU, kVOIGroup);
        m_kVOIToolbar.setVisible(false);
        m_kPointerButton = toolbarBuilder.getPointerButton();
        m_kVOIManagers = new Vector<VOIManager>();
        Color kColor = toolbarBuilder.getVOIColorButton().getBackground();
        new ColorRGB( kColor.getRed()/255.0f,
                kColor.getGreen()/255.0f,
                kColor.getBlue()/255.0f );
        for ( int i = 0; i < iNViews; i++ )
        {
            m_kVOIManagers.add(new VOIManager(this));
        }
        m_bGPURenderer = bGPU;


        /**
         * Create Popup Dialogs for VOIs and VOI points
         */
        popup = new ViewJPopupVOI(this);

        if (m_kParent.getActiveImage().getNDims() < 3) {
            popup.setEnabledPropagate(false);
        }

        popupPt = new ViewJPopupPt(this);

        if (m_kParent.getActiveImage().getNDims() < 3) {
            popupPt.setEnabledGraph(false);
            popupPt.setEnabledProp(false);
        }

        for ( int i = 0; i < iNViews; i++ )
        {
            m_kVOIManagers.elementAt(i).setPopupVOI(popup);
            m_kVOIManagers.elementAt(i).setPopupPt(popupPt);
        }
        if ( m_kImageA != null && m_kImageA.getVOIs() != null )
        {
            m_kImageA.getVOIs().addVectorListener(this);
            VOIVector kVOIs = m_kImageA.getVOIs();
            for ( int i = 0; i < kVOIs.size(); i++ )
            {
                kVOIs.elementAt(i).addVOIListener(this);
            }
        }
        if ( m_kImageB != null && m_kImageB.getVOIs() != null )
        {
            m_kImageB.getVOIs().addVectorListener(this);
            VOIVector kVOIs = m_kImageB.getVOIs();
            for ( int i = 0; i < kVOIs.size(); i++ )
            {
                kVOIs.elementAt(i).addVOIListener(this);
            }
        }
    }

    /* 
     * Handles all VOI Action commands from the VOI toolbar and VOI Menu.
     * @param event ActionEvent
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();
        //System.err.println( command );
        if ( command.equals(CustomUIBuilder.PARAM_VOI_COLOR.getActionCommand()) ) {
            showColorDialog();
            setDefaultCursor();
        } 
        else if (command.equals(CustomUIBuilder.PARAM_VOI_NEW.getActionCommand()) ) {
            newVOI(true, false);
            setDefaultCursor();
        } 
        else if ( command.equals(CustomUIBuilder.PARAM_VOI_LIVEWIRE.getActionCommand()) )
        {
            final JDialogLivewire dialog = new JDialogLivewire(null);
            if ( !dialog.isCancelled()) {
                boolean iActive = false;
                for (int i = 0; i < m_kVOIManagers.size(); i++) {
                    m_kVOIManagers.elementAt(i).liveWire( dialog.getSelection() );
                    iActive |= m_kVOIManagers.elementAt(i).isActive();
                }
                m_kParent.PointerActive(iActive);
            }
            else
            {
                setDefaultCursor();
            }
        } 
        else if (command.equals(CustomUIBuilder.PARAM_VOI_UNDO.getActionCommand()) ) {
            undoVOI();
            setDefaultCursor();
        }  
        else if (command.equals(CustomUIBuilder.PARAM_VOI_REDO.getActionCommand()) ) {
            redoVOI();
            setDefaultCursor();
        } 
        else if (command.equals("OpacityPaint")) {
            new JDialogOpacityControls(null, this, m_fOpacity);
        } 
        else if ( command.equals(CustomUIBuilder.PARAM_VOI_QUICK_AND_OP.getActionCommand() ) ) {
            saveImage(CustomUIBuilder.PARAM_VOI_QUICK_AND_OP.getActionCommand());
            createMask( CustomUIBuilder.PARAM_VOI_QUICK_AND_OP.getActionCommand() );
            setDefaultCursor();
        } 
        else if ( command.equals(CustomUIBuilder.PARAM_VOI_QUICK_NOT_OP.getActionCommand() ) ) {
            saveImage(CustomUIBuilder.PARAM_VOI_QUICK_NOT_OP.getActionCommand());
            createMask( CustomUIBuilder.PARAM_VOI_QUICK_NOT_OP.getActionCommand() );
            setDefaultCursor();
        } 
        else if (command.equals(CustomUIBuilder.PARAM_VOI_3D_INTERSECTION.getActionCommand()) ) {
            m_kParent.create3DVOI(true);
            setDefaultCursor();
        } 
        else if (command.equals(CustomUIBuilder.PARAM_VOI_3D_UNION.getActionCommand()) ) {
            m_kParent.create3DVOI(false);
            setDefaultCursor();
        } 
        else if (command.equals(CustomUIBuilder.PARAM_VOI_PROPERTIES.getActionCommand())) {
            showVOIProperties();
            setDefaultCursor();
        } 
        else if (command.equals(CustomUIBuilder.PARAM_OPEN_VOI.getActionCommand())) {
            final boolean success = openVOI(false, false);

            if (success) {
                ScriptRecorder.getReference().addLine(new ActionOpenVOI(getActiveImage()));
                ProvenanceRecorder.getReference().addLine(new ActionOpenVOI(getActiveImage()));
            }
        } 
        else if (command.equals(CustomUIBuilder.PARAM_OPEN_VOI_ALL.getActionCommand())) {
            loadAllVOIs(false);

            ScriptRecorder.getReference().addLine(new ActionOpenAllVOIs(getActiveImage()));
            ProvenanceRecorder.getReference().addLine(new ActionOpenAllVOIs(getActiveImage()));
        } 
        else if (command.equals(CustomUIBuilder.PARAM_OPEN_VOI_ALL_FROM.getActionCommand())) {

            // get the voi directory
            String fileName = null;
            String directory = null;
            String voiDir = null;

            final JFileChooser chooser = new JFileChooser();

            if (ViewUserInterface.getReference().getDefaultDirectory() != null) {
                chooser.setCurrentDirectory(new File(ViewUserInterface.getReference().getDefaultDirectory()));
            } else {
                chooser.setCurrentDirectory(new File(System.getProperties().getProperty("user.dir")));
            }

            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            final int returnVal = chooser.showOpenDialog(m_kParent.getFrame());

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                fileName = chooser.getSelectedFile().getName();
                directory = String.valueOf(chooser.getCurrentDirectory()) + File.separatorChar;
            }

            if (fileName != null) {
                voiDir = new String(directory + fileName + File.separator);
                loadAllVOIsFrom(voiDir, false);
            }
        } 
        else if (command.equals(CustomUIBuilder.PARAM_OPEN_VOI_LABEL.getActionCommand())) {
            openVOI(false, true);
        } 
        else if (command.equals(CustomUIBuilder.PARAM_SAVE_SELECTED_CONTOURS.getActionCommand())) {
            saveVOI(false);
        } 
        else if (command.equals(CustomUIBuilder.PARAM_SAVE_SELECTED_CONTOURS_AS.getActionCommand())) {
            saveVOIAs(false);
        } 
        else if (command.equals(CustomUIBuilder.PARAM_SAVE_VOI.getActionCommand())) {
            saveVOI(true);
        } 
        else if (command.equals(CustomUIBuilder.PARAM_SAVE_VOI_AS.getActionCommand())) {
            saveVOIAs(true);
        } 
        else if (command.equals(CustomUIBuilder.PARAM_SAVE_ALL_VOI.getActionCommand())) {
            saveAllVOIs();

            ScriptRecorder.getReference().addLine(new ActionSaveAllVOIs(getActiveImage()));
            ProvenanceRecorder.getReference().addLine(new ActionSaveAllVOIs(getActiveImage()));
        } 
        else if (command.equals(CustomUIBuilder.PARAM_SAVE_ALL_VOI_TO.getActionCommand())) {

            // get the voi directory
            String fileName = null;
            String directory = null;
            String voiDir = null;

            final JFileChooser chooser = new JFileChooser();

            if (ViewUserInterface.getReference().getDefaultDirectory() != null) {
                chooser.setCurrentDirectory(new File(ViewUserInterface.getReference().getDefaultDirectory()));
            } else {
                chooser.setCurrentDirectory(new File(System.getProperties().getProperty("user.dir")));
            }

            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            final int returnVal = chooser.showSaveDialog(m_kParent.getFrame());

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                fileName = chooser.getSelectedFile().getName();
                directory = String.valueOf(chooser.getCurrentDirectory()) + File.separatorChar;
            }

            if (fileName != null) {
                voiDir = new String(directory + fileName + File.separator);
                saveAllVOIsTo(voiDir);

                ScriptRecorder.getReference().addLine(new ActionSaveAllVOIs(getActiveImage(), voiDir));
                ProvenanceRecorder.getReference().addLine(new ActionSaveAllVOIs(getActiveImage(), voiDir));
            }
        } 
        else if (command.equals(CustomUIBuilder.PARAM_SAVE_VOI_INTENSITIES.getActionCommand())) {
            saveVOIIntensities();
        } 
        else if (command.equals(CustomUIBuilder.PARAM_SAVE_SELECTED_LABEL.getActionCommand())) {
            saveLabels(false);
        } 
        else if (command.equals(CustomUIBuilder.PARAM_SAVE_ALL_LABEL.getActionCommand())) {
            saveLabels(true);
        } 
        else if (command.equals("XOR")) {
            if( event.getSource() instanceof JCheckBoxMenuItem )
            {
                ViewUserInterface.getReference().setUseVOIXOR(((JCheckBoxMenuItem)event.getSource()).isSelected());
            }
        } 
        else if (command.equals("PaintMask")) {
            if ( !checkForActiveVOIs()) {
                MipavUtil.displayWarning("Please select a VOI!");
                return;
            }
            getActiveImage().setMask(getActiveImage().generateVOIMask(Preferences.is(Preferences.PREF_USE_VOI_XOR), true));
            m_kParent.setPaintMask(getActiveImage().getMask());
            updateDisplay();
            getActiveImage().notifyImageDisplayListeners();

        } 
        else if (command.equals("BinaryMask")) {
            if (getActiveImage().getVOIs().size() == 0) {
                MipavUtil.displayWarning("There are no VOIs in this image");
                return;
            }
            ModelImage maskImage = null;

            try {

                if (getActiveVOICount() == 0) {
                    selectAllVOIs(true);
                }
                maskImage = getActiveImage().generateBinaryImage(Preferences.is(Preferences.PREF_USE_VOI_XOR), false);
               
                if (maskImage != null) {
                    maskImage.setImageName(getActiveImage().getImageName() + "_bmask");
                    maskImage.getMatrixHolder().replaceMatrices(getActiveImage().getMatrixHolder().getMatrices());
                    maskImage.getFileInfo(0).setOrigin(getActiveImage().getFileInfo(0).getOrigin());
                    new ViewJFrameImage(maskImage, null, new Dimension(610, 200), false);
                }
            } catch (final OutOfMemoryError error) {
                MipavUtil.displayError("Out of memory: unable to open new frame");

                if (maskImage != null) {
                    maskImage.disposeLocal();
                }

                maskImage = null;

                return;
            }

            ScriptRecorder.getReference().addLine(
                    new ActionVOIToMask(getActiveImage(), maskImage, ActionVOIToMask.MASK_BINARY));
            ProvenanceRecorder.getReference().addLine(
                    new ActionVOIToMask(getActiveImage(), maskImage, ActionVOIToMask.MASK_BINARY));
        } else if (command.equals("ShortMask")) {
            if (getActiveImage().getVOIs().size() == 0) {
                MipavUtil.displayWarning("There are no VOIs in this image");
                return;
            }
            ModelImage shortImage = null;

            try {

                if (getActiveVOICount() == 0) {
                    selectAllVOIs(true);
                }
                shortImage = getActiveImage().generateShortImage(1, Preferences.is(Preferences.PREF_USE_VOI_XOR), false);

                if (shortImage != null) {
                    shortImage.setImageName(getActiveImage().getImageName() + "_smask");
                    shortImage.getMatrixHolder().replaceMatrices(getActiveImage().getMatrixHolder().getMatrices());
                    shortImage.getFileInfo(0).setOrigin(getActiveImage().getFileInfo(0).getOrigin());
                    new ViewJFrameImage(shortImage, null, new Dimension(610, 200), false);
                }
            } catch (final OutOfMemoryError error) {
                MipavUtil.displayError("Out of memory: unable to open new frame");

                if (shortImage != null) {
                    shortImage.disposeLocal();
                }

                shortImage = null;

                return;
            }

            ScriptRecorder.getReference().addLine(
                    new ActionVOIToMask(getActiveImage(), shortImage, ActionVOIToMask.MASK_SHORT));
            ProvenanceRecorder.getReference().addLine(
                    new ActionVOIToMask(getActiveImage(), shortImage, ActionVOIToMask.MASK_SHORT));
        } else if (command.equals("UnsignedByteMask")) {
            if (getActiveImage().getVOIs().size() == 0) {
                MipavUtil.displayWarning("There are no VOIs in this image");
                return;
            }
            ModelImage uByteImage = null;

            try {

                if (getActiveVOICount() == 0) {
                    selectAllVOIs(true);
                }
                uByteImage = getActiveImage().generateUnsignedByteImage(1, Preferences.is(Preferences.PREF_USE_VOI_XOR), false);

                if (uByteImage != null) {
                    uByteImage.setImageName(getActiveImage().getImageName() + "_ubmask");
                    uByteImage.getMatrixHolder().replaceMatrices(getActiveImage().getMatrixHolder().getMatrices());
                    uByteImage.getFileInfo(0).setOrigin(getActiveImage().getFileInfo(0).getOrigin());
                    new ViewJFrameImage(uByteImage, null, new Dimension(610, 200), false);
                }
            } catch (final OutOfMemoryError error) {
                MipavUtil.displayError("Out of memory: unable to open new frame");

                if (uByteImage != null) {
                    uByteImage.disposeLocal();
                }

                uByteImage = null;

                return;
            }

            ScriptRecorder.getReference().addLine(
                    new ActionVOIToMask(getActiveImage(), uByteImage, ActionVOIToMask.MASK_UBYTE));
            ProvenanceRecorder.getReference().addLine(
                    new ActionVOIToMask(getActiveImage(), uByteImage, ActionVOIToMask.MASK_UBYTE));
        } else if (command.equals("BinaryMaskSelected")) {

            if ( !checkForActiveVOIs()) {
                MipavUtil.displayWarning("Please select at least 1 VOI!");
                return;
            }

            ModelImage maskImage = null;

            try {

                maskImage = getActiveImage().generateBinaryImage(Preferences.is(Preferences.PREF_USE_VOI_XOR), true);

                if (maskImage != null) {
                    maskImage.setImageName(getActiveImage().getImageName() + "_bmask");
                    maskImage.getMatrixHolder().replaceMatrices(getActiveImage().getMatrixHolder().getMatrices());
                    maskImage.getFileInfo(0).setOrigin(getActiveImage().getFileInfo(0).getOrigin());
                    new ViewJFrameImage(maskImage, null, new Dimension(610, 200), false);
                }
            } catch (final OutOfMemoryError error) {
                MipavUtil.displayError("Out of memory: unable to open new frame");

                if (maskImage != null) {
                    maskImage.disposeLocal();
                }

                maskImage = null;

                return;
            }

            ScriptRecorder.getReference().addLine(
                    new ActionVOIToMask(getActiveImage(), maskImage, ActionVOIToMask.MASK_BINARY));
            ProvenanceRecorder.getReference().addLine(
                    new ActionVOIToMask(getActiveImage(), maskImage, ActionVOIToMask.MASK_BINARY));
        } else if (command.equals("ShortMaskSelected")) {

            if ( !checkForActiveVOIs()) {
                MipavUtil.displayWarning("Please select at least 1 VOI!");
                return;
            }

            ModelImage shortImage = null;

            try {

                shortImage = getActiveImage().generateShortImage(1, Preferences.is(Preferences.PREF_USE_VOI_XOR), true);

                if (shortImage != null) {
                    shortImage.setImageName(getActiveImage().getImageName() + "_smask");
                    shortImage.getMatrixHolder().replaceMatrices(getActiveImage().getMatrixHolder().getMatrices());
                    shortImage.getFileInfo(0).setOrigin(getActiveImage().getFileInfo(0).getOrigin());
                    new ViewJFrameImage(shortImage, null, new Dimension(610, 200), false);
                }
            } catch (final OutOfMemoryError error) {
                MipavUtil.displayError("Out of memory: unable to open new frame");

                if (shortImage != null) {
                    shortImage.disposeLocal();
                }

                shortImage = null;

                return;
            }

            ScriptRecorder.getReference().addLine(
                    new ActionVOIToMask(getActiveImage(), shortImage, ActionVOIToMask.MASK_SHORT));
            ProvenanceRecorder.getReference().addLine(
                    new ActionVOIToMask(getActiveImage(), shortImage, ActionVOIToMask.MASK_SHORT));
        } else if (command.equals("UnsignedByteMaskSelected")) {
            if ( !checkForActiveVOIs()) {
                MipavUtil.displayWarning("Please select at least 1 VOI!");
                return;
            }

            ModelImage uByteImage = null;

            try {

                uByteImage = getActiveImage().generateUnsignedByteImage(1, Preferences.is(Preferences.PREF_USE_VOI_XOR), true);

                if (uByteImage != null) {
                    uByteImage.setImageName(getActiveImage().getImageName() + "_ubmask");
                    uByteImage.getMatrixHolder().replaceMatrices(getActiveImage().getMatrixHolder().getMatrices());
                    uByteImage.getFileInfo(0).setOrigin(getActiveImage().getFileInfo(0).getOrigin());
                    new ViewJFrameImage(uByteImage, null, new Dimension(610, 200), false);
                }
            } catch (final OutOfMemoryError error) {
                MipavUtil.displayError("Out of memory: unable to open new frame");

                if (uByteImage != null) {
                    uByteImage.disposeLocal();
                }

                uByteImage = null;

                return;
            }

            ScriptRecorder.getReference().addLine(
                    new ActionVOIToMask(getActiveImage(), uByteImage, ActionVOIToMask.MASK_UBYTE));
            ProvenanceRecorder.getReference().addLine(
                    new ActionVOIToMask(getActiveImage(), uByteImage, ActionVOIToMask.MASK_UBYTE));
        } 
        else if (command.equals("MaskToVOI")) {
            final AlgorithmVOIExtraction VOIExtractionAlgo = new AlgorithmVOIExtraction(getActiveImage());

            ViewJProgressBar progressBar = new ViewJProgressBar(getActiveImage().getImageName(), "Extracting VOI ...", 0, 100, true);
            progressBar.setSeparateThread(false);
            VOIExtractionAlgo.addProgressChangeListener(progressBar);
            VOIExtractionAlgo.setProgressValues(0, 100);

            // VOIExtractionAlgo.setActiveImage(false);
            VOIExtractionAlgo.run();

            ScriptRecorder.getReference().addLine(new ActionMaskToVOI(getActiveImage()));
            ProvenanceRecorder.getReference().addLine(new ActionMaskToVOI(getActiveImage()));
            updateDisplay();
        } else if (command.equals("MaskToPaint")) {
            m_kParent.maskToPaint();
        }
        else if (command.equals("PaintToVOI")) {
            saveVOIs(command);
            paintToVOI();
        } 
        else if (command.equals("PaintToUbyteMask")) {
            m_kParent.paintToUbyteMask();
        } else if (command.equals("PaintToShortMask")) {
            m_kParent.paintToShortMask();
        } 
        
        else if (command.equals("Snake") || 
        		command.equals("AGVF") || 
        		command.equals("GVF") || command.equals("BSnake")) {
            if ( !checkForActiveVOIs()) {
                MipavUtil.displayWarning("Please select a VOI!");
                return;
            }
            evolveBoundary2D(command);
        } else if (command.equals("SmoothVOI")) {
            if ( !checkForActiveVOIs()) {
                MipavUtil.displayWarning("Please select a VOI!");
                return;
            }
            saveVOIs(command);
            new JDialogBSmooth(m_kParent.getFrame(), getActiveImage(), getSlice());
        } // Paint
        else if (command.equals(CustomUIBuilder.PARAM_VOI_FLIPY.getActionCommand())) {
            if ( !checkForActiveVOIs()) {
                MipavUtil.displayWarning("Please select a VOI!");
                return;
            }
            saveVOIs(command);
            final JDialogFlip flip = new JDialogFlip(m_kParent.getFrame(), getActiveImage(), AlgorithmFlip.Y_AXIS,
                    AlgorithmFlip.VOI_TYPE);
            flip.callAlgorithm();
        } else if (command.equals(CustomUIBuilder.PARAM_VOI_FLIPX.getActionCommand())) {
            if ( !checkForActiveVOIs()) {
                MipavUtil.displayWarning("Please select a VOI!");
                return;
            }
            saveVOIs(command);
            final JDialogFlip flip = new JDialogFlip(m_kParent.getFrame(), getActiveImage(), AlgorithmFlip.X_AXIS,
                    AlgorithmFlip.VOI_TYPE);
            flip.callAlgorithm();
        } else if (command.equals(CustomUIBuilder.PARAM_VOI_FLIPZ.getActionCommand())) {
            if ( !checkForActiveVOIs()) {
                MipavUtil.displayWarning("Please select a VOI!");
                return;
            }
            saveVOIs(command);
            final JDialogFlip flip = new JDialogFlip(m_kParent.getFrame(), getActiveImage(), AlgorithmFlip.Z_AXIS,
                    AlgorithmFlip.VOI_TYPE);
            flip.callAlgorithm();
        } else if (command.equals("interpolateVOIs")) {
            saveVOIs(command);
            interpolateVOIs();
        } 
        else if (command.equals("Trim")) {
            saveVOIs(command);
            final JDialogTrim trimSettings = new JDialogTrim(m_kParent.getFrame(), getActiveImage());

            trimSettings.setVisible(true);
        }
        else if (command.equals(CustomUIBuilder.PARAM_VOI_GRAPH_OPEN.getActionCommand())) {
            new ViewJFrameGraph("Graph", true);
        } 
        else if (command.equals(CustomUIBuilder.PARAM_VOI_GRAPH_BOUNDARY_INTENSITY.getActionCommand())) {
            graphVOI();
        } 
        else if (command.equals(CustomUIBuilder.PARAM_VOI_GRAPH_TOTAL_INTENSITY.getActionCommand())) {
            graph25VOI_CalcInten(true, false, 0);
        } 
        else if (command.equals(CustomUIBuilder.PARAM_VOI_GRAPH_AVERAGE_INTENSITY.getActionCommand())) {
            graph25VOI_CalcInten(false, false, 0);
        } 
        else if (command.equals(CustomUIBuilder.PARAM_VOI_GRAPH_TOTAL_INTENSITY_THRESHOLD.getActionCommand())) {
            new JDialogIntensityThreshold(getFrame(), this,
                                          false);
        } else if (command.equals(CustomUIBuilder.PARAM_VOI_GRAPH_AVERAGE_INTENSITY_THRESHOLD.getActionCommand())) {
            new JDialogIntensityThreshold(getFrame(), this,
                                          true);
        }
        else if (command.equals(CustomUIBuilder.PARAM_VOI_GRAPH_SHOW.getActionCommand())) {
            setGraphVisible();
        } 
        else if (command.equals(CustomUIBuilder.PARAM_VOI_GRAPH_PAAI.getActionCommand())) {
            setPAAIGraphVisible();        
        }
        else if (command.equals(CustomUIBuilder.PARAM_VOI_STATISTICS.getActionCommand())) {
            showStatisticsCalculator();
        } else if (command.equals("ProstateMergedVOIs")) {
            saveMergedVOIs();
        } else if (command.equals("ProstateReconstruct")) {
            reconstructSurfaceFromVOIs();
        } else if (command.equals("ProstateExtract")) {
            // extractSurfaceFromVOIs();
        } else if (command.equals("ProstateFeaturesSave")) {
            saveProstateFeatures();
        } else if (command.equals("ProstateFeaturesTest")) {
            testProstateFeatures();
        } else if (command.equals("ProstateFeaturesTrain")) {
            testProstateFeaturesTrain();
        } else if (command.equals("ProstateFeaturesClassification")) {
            testProstateFeaturesClassification();
        } else if (command.equals("LoadProstateMask")) {
            loadProstateMask();
        } else if (command.equals("ProstateSegAuto")) {
        	prostateSegAuto();
        } else if (command.equals("SaveDicomMatrix")) {
            saveDicomMatrixInfo();
        } 
        else {
            doVOI(command);
        }

    }

    /**
     * Add a new VOIBase. This function should only be called from VOIManager when a new VOIBase
     * is created with the mouse.
     * @param kNew the new VOIBase to add.
     * @param bQuickLUT true if this is a QuickLUT VOI.
     * @param bUpdate when true call updateDisplay() after the VOIBase is added
     * @param isFinished true if this VOIBase is complete, used for open contours.
     */
    public void addVOI( VOIBase kNew, boolean bQuickLUT, boolean bUpdate, boolean isFinished )
    {
        ModelImage kActive = m_kParent.getActiveImage();
        if ( kActive != null )
        {
            addVOI( kActive, kNew, bQuickLUT, bUpdate, isFinished );
            if (presetHue >= 0.0f) {
            	kNew.getGroup().setColor(presetHue);
            }
            if ( kActive.isRegistered( m_kCurrentVOIGroup ) == -1 )
            {
                kActive.registerVOI( m_kCurrentVOIGroup );
            }
        }
    }
    
    public VOIManager addVOIManager(ModelImage kImageA, ModelImage kImageB, Component kComponent, 
            ScreenCoordinateListener kContext, int iOrientation, int iSlice)
    {
        VOIManager kVOIManager = new VOIManager(this);
        kVOIManager.init(getFrame(), kImageA, kImageB,
                    kComponent, kContext,
                    iOrientation, iSlice);
        kVOIManager.setPopupVOI(popup);
        kVOIManager.setPopupPt(popupPt);
        m_kVOIManagers.add(kVOIManager);
        return kVOIManager;
    }
    
    public void algorithmPerformed()
    {
    	if ( !m_bDefaultImage && (m_kTempImage != null))
    	{
            int[] axisA = getActiveImage().getAxisOrientation();
            int[] axisB = m_kTempImage.getAxisOrientation();
            int[] axisOrder = { 0, 1, 2, 3 };
            boolean[] axisFlip = { false, false, false, false };
            if ( MipavCoordinateSystems.matchOrientation( axisA, axisB, axisOrder, axisFlip ) )
            {
                AlgorithmRotate rotateAlgo = new AlgorithmRotate( m_kTempImage, axisOrder, axisFlip );
                rotateAlgo.setRunningInSeparateThread(false);
                rotateAlgo.run();
                m_kTempImage = rotateAlgo.returnImage();
                getActiveImage().unregisterAllVOIs();            
                getActiveImage().setVOIs( m_kTempImage.getVOIs() );
                

                if ( getActiveImage() != null && getActiveImage().getVOIs() != null )
                {
                    getActiveImage().getVOIs().addVectorListener(this);
                    VOIVector kVOIs = getActiveImage().getVOIs();
                    for ( int i = 0; i < kVOIs.size(); i++ )
                    {
                        kVOIs.elementAt(i).addVOIListener(this);
                    }
                }
            }
            m_bDefaultImage = true;
            m_kTempImage = null;
    	}

        if ( m_kVOIDialog != null )
        {
            ModelImage kActive = getActiveImage();
            ViewVOIVector VOIs = kActive.getVOIs();
            for (int i = 0; i < VOIs.size(); i++) {
                if (VOIs.VOIAt(i).isActive()) {
                    m_kVOIDialog.updateVOI( VOIs.VOIAt(i), kActive );
                    m_kVOIDialog.updateTree();
                    return;
                }
            }
            if (VOIs.size() > 0) {
                m_kVOIDialog.updateVOI( VOIs.VOIAt(0), kActive );
                m_kVOIDialog.updateTree();
            }
        }
    	updateDisplay();
    }
    
    public void removeVOIManager( VOIManager kVOIManager )
    {
        if ( kVOIManager != null )
        {
            m_kVOIManagers.remove(kVOIManager);
            kVOIManager.dispose();
        }
    }

    /**
     * Adds a UpdateVOISelectionListener.
     * @param listener will receive VOI selection events.
     */
    public void addVOIUpdateListener(UpdateVOISelectionListener listener) {
        listenerList.add(UpdateVOISelectionListener.class, listener);
    }

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#deleteSelectedVOI(boolean)
     */
    public void deleteSelectedVOI(boolean contoursOnly)
    {
        ModelImage kActive = m_kParent.getActiveImage();
        ViewVOIVector VOIs = kActive.getVOIs();
        for ( int i = VOIs.size() -1; i >= 0 ; i-- )
        {
            if ( VOIs.get(i).isActive() && !contoursOnly )
            {
                if ( m_kImageA != null ) { m_kImageA.unregisterVOI(VOIs.get(i)); }
                if ( m_kImageB != null ) { m_kImageB.unregisterVOI(VOIs.get(i)); }
            }
            else if ( contoursOnly )
            {
                for ( int j = VOIs.get(i).getCurves().size()-1; j >= 0; j-- )
                {
                    if ( VOIs.get(i).getCurves().get(j).isActive() )
                    {
                        VOIs.get(i).getCurves().remove(j);
                    }
                }
            }
        }
        updateDisplay();
    }

    /**
     * Deletes the input VOIBase from the VOI. If it is the only VOIBase in
     * the VOI, that VOI is removed from the ModelImage.
     * This function is called from VOIManager.
     * @param kOld the VOIBase to remove.
     */
    public void deleteVOI(VOIBase kOld) {
        m_kCurrentVOIGroup = null;
        if ( kOld == null )
        {
            return;
        }
        VOI kGroup = kOld.getGroup();
        if ( kGroup != null )
        {
            kGroup.getCurves().remove(kOld);
            if ( kGroup.isEmpty() )
            {
                m_kParent.getActiveImage().unregisterVOI(kGroup);
                if ( m_kImageA != null ) { m_kImageA.unregisterVOI(kGroup); }
                if ( m_kImageB != null ) { m_kImageB.unregisterVOI(kGroup); }
            }
        }
        updateDisplay();
    }

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#deleteVOIs()
     */
    public void deleteVOIs()
    {
        saveVOIs("deleteVOIs");
        deleteAllVOI();
    }



    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#disposeLocal(boolean)
     */
    public void disposeLocal(boolean flag)
    {
        
        if ( m_kImageA != null && m_kImageA.getVOIs() != null )
        {
            VOIVector kVOIs = m_kImageA.getVOIs();
            kVOIs.removeVectorListener(this);
            for ( int i = 0; i < kVOIs.size(); i++ )
            {
                kVOIs.elementAt(i).removeVOIListener(this);
            }
        }
        if ( m_kImageB != null && m_kImageB.getVOIs() != null )
        {
            VOIVector kVOIs = m_kImageB.getVOIs();
            kVOIs.removeVectorListener(this);
            for ( int i = 0; i < kVOIs.size(); i++ )
            {
                kVOIs.elementAt(i).removeVOIListener(this);
            }
        }
        
        if (popup != null) {
            popup = null;
        }

        if (popupPt != null) {
            popupPt = null;
        }

        m_kImageA = null;
        m_kImageB = null;

        toolbarBuilder = null;
        m_kVOIToolbar = null;
        colorChooser = null;
        m_kUndoCommands = null;
        m_kRedoCommands = null;

        for ( int i = m_kVOIManagers.size() - 1; i >= 0; i-- )
        {
            VOIManager kManager = m_kVOIManagers.remove(i);
            kManager.dispose();
        }
        m_kVOIManagers = null;   

        m_kImageAUndo = null;
        m_kImageARedo = null;        
        m_kImageBUndo = null;
        m_kImageBRedo = null;

        listenerList = null;
        m_kParent = null;
    }


    /**
     * Performs the VOI Action Command. See CustomUIBuilder.
     * This function should be replaced withe actionPerformed(ActionEvent).
     * @param kCommand VOI action command.
     */
    public void doVOI( String kCommand )
    {        
        boolean bDraw = isDrawCommand(kCommand);
        m_kParent.enableBoth(!bDraw);

        if ( kCommand.equals(CustomUIBuilder.PARAM_LUT_QUICK.getActionCommand()) )
        {
            saveGroup = m_kCurrentVOIGroup;
            m_kCurrentVOIGroup = null;
            currentColor = toolbarBuilder.getVOIColorButton().getBackground();
        }

        if ( kCommand.equals(CustomUIBuilder.PARAM_VOI_PROPAGATE_UP.getActionCommand()) )
        {
            if ( copy() > 0 )
            {
                saveVOIs(kCommand);
                propagate(1);
            }
            setDefaultCursor();
        }
        else if ( kCommand.equals(CustomUIBuilder.PARAM_VOI_PROPAGATE_DOWN.getActionCommand()) )
        {
            if ( copy() > 0 )
            {
                saveVOIs(kCommand);
                propagate(-1);
            }
            setDefaultCursor();
        }
        else if (kCommand.equals(CustomUIBuilder.PARAM_VOI_PROPAGATE_ALL.getActionCommand()) ) {
            if ( copy() > 0 )
            {
                saveVOIs(kCommand);
                pasteAll();
            }
            setDefaultCursor();
        }
        else if ( kCommand.equals(CustomUIBuilder.PARAM_VOI_POINT_DELETE.getActionCommand()) ) 
        {
            saveVOIs(kCommand);
            deleteActiveVOI();
        }
        else if ( kCommand.equals("deleteVOIActivePt") ) 
        {
            saveVOIs(kCommand);
            deleteVOIActivePt();
        }
        else if (kCommand.equals(CustomUIBuilder.PARAM_VOI_SELECT_ALL.getActionCommand()) )
        {
            selectAllVOIs(true);
        }
        else if (kCommand.equals(CustomUIBuilder.PARAM_VOI_SELECT_NONE.getActionCommand())) {
            selectAllVOIs(false);
        } 
        else if (kCommand.equals(CustomUIBuilder.PARAM_VOI_GROUP.getActionCommand())) {
            if ( !checkForActiveVOIs()) {
                MipavUtil.displayWarning("Please select VOIs!");
                return;
            }
            saveVOIs(kCommand);
            m_kParent.getActiveImage().groupVOIs();
            fireVOISelectionChange(null);
        } else if (kCommand.equals(CustomUIBuilder.PARAM_VOI_UNGROUP.getActionCommand())) {
            saveVOIs(kCommand);
            m_kParent.getActiveImage().ungroupVOIs();
            fireVOISelectionChange(null);
        } 
        else if (kCommand.equals(CustomUIBuilder.PARAM_VOI_DELETE.getActionCommand()) ) {
            saveVOIs(kCommand);
            deleteSelectedVOI(false);
            setDefaultCursor();
        } 
        else if (kCommand.equals(CustomUIBuilder.PARAM_VOI_CUT.getActionCommand()) ) {
            cut();
            setDefaultCursor();
        } 
        else if (kCommand.equals(CustomUIBuilder.PARAM_VOI_COPY.getActionCommand()) ) {
            copy();
            setDefaultCursor();
        } 
        else if (kCommand.equals(CustomUIBuilder.PARAM_VOI_PASTE.getActionCommand()) ) {
            paste();
            setDefaultCursor();
        }
        else if ( kCommand.equals("MoveUP") )
        {
            saveVOIs(kCommand);
            moveVOI( m_kVOIManagers.elementAt(m_iActive), new Vector3f( 0, 1, 0 ), -1, true );
        }
        else if ( kCommand.equals("MoveDown") )
        {
            saveVOIs(kCommand);
            moveVOI( m_kVOIManagers.elementAt(m_iActive), new Vector3f( 0,-1, 0 ), -1, true  );
        }
        else if ( kCommand.equals("MoveLeft") )
        {
            saveVOIs(kCommand);
            moveVOI( m_kVOIManagers.elementAt(m_iActive), new Vector3f(-1, 0, 0 ), -1, true  );
        }
        else if ( kCommand.equals("MoveRight") )
        {
            saveVOIs(kCommand);
            moveVOI( m_kVOIManagers.elementAt(m_iActive), new Vector3f( 1, 0, 0 ), -1, true  );
        }        
        else if (kCommand.equals(CustomUIBuilder.PARAM_VOI_FRONT.getActionCommand())) {
            changeVOIOrder(false, VOI.FRONT);
        } 
        else if (kCommand.equals(CustomUIBuilder.PARAM_VOI_BACK.getActionCommand())) {
            changeVOIOrder(false, VOI.BACK);
        } 
        else if (kCommand.equals(CustomUIBuilder.PARAM_VOI_FORWARD.getActionCommand())) {
            changeVOIOrder(false, VOI.FORWARD);
        } 
        else if (kCommand.equals(CustomUIBuilder.PARAM_VOI_BACKWARD.getActionCommand())) {
            changeVOIOrder(false, VOI.BACKWARD);
        } 
        else if (kCommand.equals(CustomUIBuilder.PARAM_CONTOUR_FRONT.getActionCommand())) {
            changeVOIOrder(true, VOI.FRONT);
        } 
        else if (kCommand.equals(CustomUIBuilder.PARAM_CONTOUR_BACK.getActionCommand())) {
            changeVOIOrder(true, VOI.BACK);
        } 
        else if (kCommand.equals(CustomUIBuilder.PARAM_CONTOUR_FORWARD.getActionCommand())) {
            changeVOIOrder(true, VOI.FORWARD);
        } 
        else if (kCommand.equals(CustomUIBuilder.PARAM_CONTOUR_BACKWARD.getActionCommand())) {
            changeVOIOrder(true, VOI.BACKWARD);
        } 
        else
        {
            boolean iActive = false;
            for (int i = 0; i < m_kVOIManagers.size(); i++) {
                m_kVOIManagers.elementAt(i).doVOI( kCommand, bDraw );
                iActive |= m_kVOIManagers.elementAt(i).isActive();
            }
            m_kParent.PointerActive(iActive);
        }
    }


    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#fireVOISelectionChange(gov.nih.mipav.model.structures.VOI)
     */
    public void fireVOISelectionChange(VOI voi) {
        //fireVOISelectionChange(voi, null);
        //System.err.println( "fireVOISelectionChange 1" );
    }



    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#fireVOISelectionChange(gov.nih.mipav.model.structures.VOI, gov.nih.mipav.model.structures.VOIBase)
     */
    public void fireVOISelectionChange(VOI voi, VOIBase curve) {
        //System.err.println( "fireVOISelectionChange 2" );
        /*
        try {

            // only if there are listeners to send events to should we
            // bother with creating an event and bothering the event queue.
            if (listenerList.getListenerCount(UpdateVOISelectionListener.class) == 0) {
                return;
            }
        } catch (NullPointerException npe) {
            return;
        }

        // always create a new Event, since we need to carry
        // the changed VOI around.
        UpdateVOIEvent voiUpdate = new UpdateVOIEvent(this, voi, curve);

        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {

            if (listeners[i] == UpdateVOISelectionListener.class) {
                ((UpdateVOISelectionListener) listeners[i + 1])
                .selectionChanged(voiUpdate);
            }
        }
        updateDisplay();
        */
    }


    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#getActiveImage()
     */
    public ModelImage getActiveImage() {
        return m_kParent.getActiveImage();
    }

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#getComponentImage()
     */
    public Component getComponentImage() {
        return m_kVOIManagers.elementAt(m_iActive).getComponent();
    }


    /**
     * Returns the pointer button, used by VOIManager to determine
     * if the pointer button is active.
     * @return current pointer button.
     */
    public JToggleButton getPointerButton( )
    {
        return m_kPointerButton;
    }
    
    /**
     * Returns the VOIToolbar.
     * @return VOIToolbar.
     */
    public JToolBar getToolBar()
    {
        return m_kVOIToolbar;
    }


    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#getVOI_ID()
     */
    public int getVOI_ID() {
        return 0;
    }

    /**
     * Returns the VOIManager at the given position.
     * @param i the index of the VOIManager to return.
     * @return VOIManager.
     */
    public VOIManager getVOIManager(int i)
    {
        if ( i < m_kVOIManagers.size() )
        {
            return m_kVOIManagers.elementAt(i);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#graph25VOI_CalcInten(boolean, boolean, float)
     */
    public void graph25VOI_CalcInten(boolean totalIntensity,
            boolean useThreshold, float threshold) {

        int i, j, s;
        int nVOI;
        ViewVOIVector VOIs;
        VOI v;
        float intensitySum;
        float[] position;
        float[] intensity;
        float[][] rgbPositions;
        float[][] rgbIntensities;
        int numPixels;
        ViewUserInterface.getReference();


        ModelImage kImage = m_kParent.getActiveImage();

        if (kImage.getNDims() == 3) {

            if (kImage.isColorImage() == true) {

                try {
                    rgbPositions = new float[3][kImage
                                                .getExtents()[2]];
                    rgbIntensities = new float[3][kImage
                                                  .getExtents()[2]];

                    VOIs = kImage.getVOIs();
                    nVOI = VOIs.size();

                    for (i = 0; i < nVOI; i++) {

                        if (VOIs.VOIAt(i).isActive()
                                && (VOIs.VOIAt(i).isVisible() == true)) {
                            v = VOIs.VOIAt(i);

                            Vector<VOIBase>[] curves = v.getSortedCurves( VOIBase.ZPLANE, kImage.getExtents()[2]);
                            for ( s = 0; s < kImage.getExtents()[2]; s++ )
                            {
                                for (int c = 0; c < 3; c++) {
                                    numPixels = 0;
                                    intensitySum = 0;
                                    if ( curves[s] != null )
                                    {                                            
                                        for (j = 0; j < curves[s].size(); j++)
                                        {                                       
                                            if (useThreshold) {
                                                intensitySum += curves[s].elementAt(j).calcRGBIntensityThreshold(
                                                        kImage,
                                                        c,
                                                        threshold);
                                            } else {
                                                intensitySum += curves[s].elementAt(j).calcRGBIntensity(kImage, c);
                                            }

                                            numPixels += curves[s].elementAt(j).getLastNumPixels();
                                        }
                                    }
                                    rgbPositions[c][s] = s;

                                    if (totalIntensity
                                            || (numPixels == 0)) {
                                        rgbIntensities[c][s] = intensitySum;
                                    } else {
                                        rgbIntensities[c][s] = intensitySum
                                        / numPixels;
                                    }
                                }
                            }

                            ViewJFrameGraph contourGraph = new ViewJFrameGraph(
                                    rgbPositions,
                                    rgbIntensities,
                                    "Intensity Graph",
                                    v,
                                    FileInfoBase
                                    .getUnitsOfMeasureAbbrevStr(kImage
                                            .getFileInfo(0)
                                            .getUnitsOfMeasure(2)));

                            contourGraph
                            .setDefaultDirectory(ViewUserInterface
                                    .getReference()
                                    .getDefaultDirectory());
                            v.setContourGraph(contourGraph);
                            contourGraph.setVisible(true);
                            return;
                        }
                    }

                    if (i == nVOI) {
                        MipavUtil.displayError("Please select a contour VOI!");
                    }
                } catch (OutOfMemoryError error) {
                    System.gc();
                    MipavUtil
                    .displayError("Out of memory: ComponentEditImage.graphVOI");

                    return;
                }
            } else {

                try {
                    VOIs = kImage.getVOIs();
                    nVOI = VOIs.size();

                    for (i = 0; i < nVOI; i++) {

                        if (VOIs.VOIAt(i).isActive() && VOIs.VOIAt(i).isVisible() &&
                                (VOIs.VOIAt(i).getCurveType() == VOI.CONTOUR) ) {

                            position = new float[kImage.getExtents()[2]];
                            intensity = new float[kImage.getExtents()[2]];
                            v = VOIs.VOIAt(i);

                            Vector<VOIBase>[] curves = v.getSortedCurves( VOIBase.ZPLANE, kImage.getExtents()[2]);
                            for ( s = 0; s < kImage.getExtents()[2]; s++ )
                            {
                                numPixels = 0;
                                intensitySum = 0;
                                if ( curves[s] != null )
                                {
                                    for (j = 0; j < curves[s].size(); j++)
                                    {
                                        if (useThreshold) {
                                            intensitySum += curves[s].elementAt(j).calcIntensityThreshold( kImage, threshold, 0 );

                                        } else {
                                            intensitySum += curves[s].elementAt(j).calcIntensity( kImage, 0 );
                                        }

                                        numPixels += curves[s].elementAt(j).getLastNumPixels();                                        
                                    }
                                }

                                position[s] = s;

                                if (totalIntensity || (numPixels == 0)) {
                                    intensity[s] = intensitySum;
                                } else {
                                    intensity[s] = intensitySum
                                    / numPixels;
                                }
                            }

                            ViewJFrameGraph contourGraph = new ViewJFrameGraph(
                                    position,
                                    intensity,
                                    "Intensity Graph",
                                    v,
                                    FileInfoBase
                                    .getUnitsOfMeasureAbbrevStr(kImage
                                            .getFileInfo(0)
                                            .getUnitsOfMeasure(0)),null);

                            contourGraph
                            .setDefaultDirectory(ViewUserInterface
                                    .getReference()
                                    .getDefaultDirectory());
                            v.setContourGraph(contourGraph);
                            contourGraph.setVisible(true);
                            //v.setTotalIntensity(totalIntensity);
                            //v.setPosition(position);
                            //v.setIntensity(intensity);

                            return;
                        }
                    }
                    if (i == nVOI) {
                        MipavUtil.displayError("Please select a contour VOI!");
                    }
                } catch (OutOfMemoryError error) {
                    System.gc();
                    MipavUtil
                    .displayError("Out of memory: ComponentEditImage.graphVOI");

                    return;
                }
            }
        }
        
        else if (kImage.getNDims() == 4) {
            int xDim = kImage.getExtents()[0];
            int yDim = kImage.getExtents()[1];
            int zDim = kImage.getExtents()[2];
            int tDim = kImage.getExtents()[3];
            boolean useFrameRefTime = false;
            FileInfoDicom fileInfo = null;
            String frameRefTimeString = null;
            int frameReferenceTime = 0;
       
            if (kImage.getFileInfo()[0].getFileFormat() == FileUtility.DICOM) {
                boolean frameRefTimeFound = false;
                fileInfo = (FileInfoDicom) (kImage.getFileInfo(0)); 
                frameRefTimeString = ((String) fileInfo.getTagTable().getValue("0054,1300")).trim();
                if (frameRefTimeString != null) {
                    try {
                        frameReferenceTime = new Integer(frameRefTimeString).intValue();
                        frameRefTimeFound = true;
                        Preferences.debug("Frame reference time = " + frameReferenceTime + "\n");
                    } catch (NumberFormatException e) {
                        Preferences.debug("Number format excepton from frame Reference Time String = " +
                                          frameRefTimeString + "\n");
                    }
                    
                    if (frameRefTimeFound) {
                        int response = JOptionPane.showConfirmDialog(ViewUserInterface.getReference().getMainFrame(),
                                                                     new String("Do you wish to use the frame reference time for the graph x axis?"),
                                                                     "Frame Reference Time?",
                                                                     JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE); 
                        if (response == JOptionPane.YES_OPTION) {
                            useFrameRefTime = true;    
                        }
                    } // if (frameRefTimeFound)
                } // if (frameRefTimeString != null)
            } // if (compImage.getActiveImage().getFileInfo()[0].getFileFormat() == FileUtility.DICOM)

            try {

                VOIs = kImage.getVOIs();
                nVOI = VOIs.size();

                for (i = 0; i < nVOI; i++) {

                    if (VOIs.VOIAt(i).isActive() && VOIs.VOIAt(i).isVisible() &&
                            (VOIs.VOIAt(i).getCurveType() == VOI.CONTOUR) ) {

                        v = VOIs.VOIAt(i);


                        position = new float[tDim];
                        intensity = new float[tDim];

                        for (int t = 0; t < tDim; t++) {
                        	numPixels = 0;
                        	intensitySum = 0;

                            Vector<VOIBase>[] curves = v.getSortedCurves( VOIBase.ZPLANE, kImage.getExtents()[2]);
                            for ( s = 0; s < kImage.getExtents()[2]; s++ )
                            {
                            	if ( curves[s] != null )
                            	{
                            		for (j = 0; j < curves[s].size(); j++)
                            		{
                            			if (useThreshold) {
                            				intensitySum += curves[s].elementAt(j).calcIntensityThreshold( kImage, threshold, t );

                            			} else {
                            				intensitySum += curves[s].elementAt(j).calcIntensity( kImage, t );
                            			}

                            			numPixels += curves[s].elementAt(j).getLastNumPixels();                                        
                            		}
                            	}
                            }

                            if (useFrameRefTime) {
                                fileInfo = (FileInfoDicom) (kImage.getFileInfo(t * zDim)); 
                                frameRefTimeString = ((String) fileInfo.getTagTable().getValue("0054,1300")).trim();
                                if (frameRefTimeString != null) {
                                    try {
                                        frameReferenceTime = new Integer(frameRefTimeString).intValue();
                                    } catch (NumberFormatException e) {
                                        MipavUtil.displayError("Number format excepton from frame Reference Time String = " +
                                                          frameRefTimeString);
                                        return;
                                    }
                                    
                                    position[t] = frameReferenceTime;
                                } // if (frameRefTimeString != null) 
                                else {
                                    MipavUtil.displayError("Frame reference time string is null");
                                    return;
                                }
                            } // if (useFrameRefTime)
                            else {
                                position[t] = t;
                            }

                            if (totalIntensity || (numPixels == 0)) {
                                intensity[t] = intensitySum;
                            } else {
                                intensity[t] = intensitySum / numPixels;
                            }
                        }

                        ViewJFrameGraph contourGraph = new ViewJFrameGraph(
                                position,
                                intensity,
                                "Intensity Graph",
                                v,
                                FileInfoBase
                                .getUnitsOfMeasureAbbrevStr(kImage
                                        .getFileInfo(0)
                                        .getUnitsOfMeasure(0)),null);

                        contourGraph
                        .setDefaultDirectory(ViewUserInterface
                                .getReference()
                                .getDefaultDirectory());
                        v.setContourGraph(contourGraph);
                        contourGraph.setVisible(true);
                        //v.setTotalIntensity(totalIntensity);
                        //v.setPosition(position);
                        //v.setIntensity(intensity);

                        return;
                    }
                }

                if (i == nVOI) {
                    MipavUtil.displayError("Please select a contour VOI!");
                }
            } catch (OutOfMemoryError error) {
                System.gc();
                MipavUtil.displayError("Out of memory: ComponentEditImage.graphVOI");

                return;
            }
        }
    }

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#isNewVoiNeeded(int)
     */
    public boolean isNewVoiNeeded(int voiType) {
        return false;
    }

    /**
     * Creates a 3D Surface out of the VOIs.
     * @param bIntersection when true the intersection of the VOIs is used to create the surface, when false
     * a union of the VOIs is used to create the surface.
     * @param kVolume ModelImage, the union or intersection is written into the input ModelImage.
     * @return true if success, false if no voxels are in the union or intersection.
     */
    public boolean make3DVOI( boolean bIntersection, ModelImage kVolume  )
    {
        boolean bCreated = true;
        for (int i = 0; i < m_kVOIManagers.size(); i++) {
            bCreated &= make3DVOI(bIntersection, m_kParent.getActiveImage(), kVolume, null, i);
        }
        return bCreated;
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent event) {

        if (event.getButton() == MouseEvent.BUTTON3) {

            if (event.getSource() instanceof AbstractButton) {
                AbstractButton btnSource = (AbstractButton) event.getSource();
                if ( btnSource.getActionCommand().equals(CustomUIBuilder.PARAM_VOI_QUICK_AND_OP.getActionCommand())
                        || btnSource.getActionCommand().equals(CustomUIBuilder.PARAM_VOI_QUICK_NOT_OP.getActionCommand())) {
                    ViewJFrameBase.handleMaskPopupMenu((Component) event.getSource(), event);
                } 
            }
        }
    }


    /* (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    public void mouseDragged(MouseEvent arg0) {}

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e) {}


    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e) {}



    /* (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
     */
    public void mouseMoved(MouseEvent arg0) {}


    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) {}

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e) {}

    /**
     * Called from the VOIManager class when multiple contours are selected and moved as
     * a group. This function calculates the group bounding box and tests the move to ensure
     * the contours are not moved out of the image bounds.
     * @param kActive VOIManager initiating the call.
     * @param kDiff the move distance
     * @param iPlane the plane of the VOIManager (equivalent to Axial, Coronal, Sagittal)
     * @param bFirstMove true if this is the first move for the selected group of contours.
     */
    public void moveVOI( VOIManager kActive, Vector3f kDiff, int iPlane, boolean bFirstMove )
    {
        if ( bFirstMove )
        {
            boolean bFirst = true;
            m_kActiveList = new Vector<VOIBase>();
            VOIVector kVOIs = m_kParent.getActiveImage().getVOIs();
            for ( int i = 0; i < kVOIs.size(); i++ )
            {
                VOI kCurrentGroup = kVOIs.get(i);
                for ( int j = 0; j < kCurrentGroup.getCurves().size(); j++ )
                {
                    VOIBase kCurrentVOI = kCurrentGroup.getCurves().get(j);
                    if ( kCurrentVOI.isActive() && !m_kActiveList.contains(kCurrentVOI) )
                    {
                        m_kActiveList.add( kCurrentVOI );
                        Vector3f[] kBounds = kCurrentVOI.getImageBoundingBox();
                        if ( bFirst )
                        {
                            bFirst = false;
                            m_akBounds[0].Copy(kBounds[0]);
                            m_akBounds[1].Copy(kBounds[1]);
                        }
                        m_akBounds[0].Min(kBounds[0]);
                        m_akBounds[1].Max(kBounds[1]);
                    }
                }
            }
        }

        if ( kActive.testMove( kDiff, m_akBounds ) )
        {
            for ( int i = 0; i < m_kActiveList.size(); i++ )
            {
                VOIBase kCurrentVOI = m_kActiveList.get(i);
                if ( iPlane == (iPlane & kCurrentVOI.getPlane()) || (iPlane == -1) )
                {
                    kActive.move( kCurrentVOI, kDiff );
                }
            }
        }

        updateDisplay();
    }

    /**
     * Initiate a new VOI.
     * @param bPropagate when true propagate the newVOI command to the VOIManagers.
     * @param bSplit true if this is a new SplitVOI.
     */
    public void newVOI( boolean bPropagate, boolean bSplit )
    {
        if ( !bSplit )
        {
            selectAllVOIs(false);
        }
        if ( bPropagate )
        {
            doVOI(CustomUIBuilder.PARAM_VOI_NEW.getActionCommand());
        }
        m_kCurrentVOIGroup = null;
        voiUID++;
        if (presetHue >= 0.0) {
        	Color color = Color.getHSBColor(presetHue, 1.0f, 1.0f);
        	toolbarBuilder.getVOIColorButton().setVOIColor(color);
        	toolbarBuilder.getVOIColorButton().setBackground(color);
        }
        else {
            toolbarBuilder.getVOIColorButton().setVOIColor(voiUID);
        }
        setButtonColor(toolbarBuilder.getVOIColorButton(), 
                toolbarBuilder.getVOIColorButton().getBackground() );


        short sID = (short)(m_kParent.getActiveImage().getVOIs().getUniqueID());
        m_kCurrentVOIGroup = new VOI( sID,  new String( "_" + sID ) );
        m_kCurrentVOIGroup.addVOIListener(this);
        if (presetHue >= 0.0) {
        	m_kCurrentVOIGroup.setColor(presetHue);
        }
        m_kCurrentVOIGroup.setOpacity(1f);
    }

    /**
     * Paste the input VOIBase.
     * @param kNew the enw VOIBase to paste.
     */
    public void pasteVOI(VOIBase kNew)
    {
        if ( kNew.getGroup() == null )
        {
            if ( m_kCurrentVOIGroup == null )
            {
                newVOI(false, false);
            }
            kNew.setGroup(m_kCurrentVOIGroup);
        }
        kNew.getGroup().getCurves().add(kNew);
        if (m_kParent.getActiveImage().isRegistered(kNew.getGroup()) == -1 )
        {
            m_kParent.getActiveImage().registerVOI(kNew.getGroup());
        }

        if ( (m_kUndoCommands.size() > 0) && (m_kUndoCommands.lastElement() != null) && 
                !m_kUndoCommands.lastElement().equals(CustomUIBuilder.PARAM_VOI_PROPAGATE_ALL.getActionCommand()) )
        {
            setCenter(kNew.getGeometricCenter(), true );
        }
        updateDisplay();
    }

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#propVOI(int, boolean)
     */
    public boolean propVOI(int direction, boolean active)
    {
        if ( copy() <= 0 )
        {
            return false;
        }
        if ( direction > 0 )
        {
            saveVOIs(CustomUIBuilder.PARAM_VOI_PROPAGATE_UP.getActionCommand());
        }
        else
        {
            saveVOIs(CustomUIBuilder.PARAM_VOI_PROPAGATE_DOWN.getActionCommand());
        }
        propagate(direction);
        updateDisplay();
        return true;
    }

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#propVOIAll()
     */
    public boolean propVOIAll() {
        if ( copy() <= 0 )
        {
            return false;
        }
        saveVOIs(CustomUIBuilder.PARAM_VOI_PROPAGATE_ALL.getActionCommand());
        deleteActiveVOI();
        pasteAll();
        return true;        
    }


    /**
     * Called from the VOIManager class when a user has drawn the QuickLUT rectangle.
     * This function calculates the new LUT and updates the images.
     * @param kLUT VOIBase defining the QuickLUT rectangle.
     */
    public void quickLUT( VOIBase kLUT )
    {
        kLUT.update();
        Vector3f[] kBounds = kLUT.getImageBoundingBox();
        m_akBounds[0].Copy(kBounds[0]);
        m_akBounds[1].Copy(kBounds[1]);        
        deleteVOI( kLUT );
        if (m_kParent.getActiveImage().isColorImage() == false) {
            quickLUT( m_akBounds, m_kParent.getActiveImage(), m_kParent.getActiveLUT() );
            m_kParent.getActiveImage().notifyImageDisplayListeners(m_kParent.getActiveLUT(), true);            
        } else { // RGB image
            quickRGB( m_akBounds, m_kParent.getActiveImage(), m_kParent.getActiveRGB() );
            m_kParent.getActiveImage().notifyImageDisplayListeners(true, 1, m_kParent.getActiveRGB());
        }

        if ( (getActiveImage().isColorImage()) && (getActiveImage().getHistoRGBFrame() != null)) {
            getActiveImage().getHistoRGBFrame().update();
        } else if (getActiveImage().getHistoLUTFrame() != null) {
            getActiveImage().getHistoLUTFrame().update();
        }    

        toolbarBuilder.getVOIColorButton().setBackground( currentColor );
        m_kCurrentVOIGroup = saveGroup;
    }

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#removeVOIUpdateListener(gov.nih.mipav.model.structures.UpdateVOISelectionListener)
     */
    public void removeVOIUpdateListener(UpdateVOISelectionListener listener) {
        listenerList.remove(UpdateVOISelectionListener.class, listener);
    }

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#resetLivewire()
     */
    public void resetLivewire() 
    {
        for (int i = 0; i < m_kVOIManagers.size(); i++) {
            m_kVOIManagers.elementAt(i).doVOI( "ResetLiveWire", false );
        }
    }

    /**
     * Save the current VOIState to the undo/re-do list.
     * @param kCommand the VOI Action Command about to be issued.
     */
    public void saveVOIs( String kCommand )
    {
        //System.err.println( "saveVOIs " + kCommand );
        m_kUndoCommands.add( kCommand );
        m_kUndoList.add( getVOIState() );
        m_kRedoCommands.clear();
        m_kRedoList.clear();
    }

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#selectAllVOIs(boolean)
     */
    public void selectAllVOIs(boolean bActive)
    {
        VOIVector kVOIs = m_kParent.getActiveImage().getVOIs();
        for ( int i = 0; i < kVOIs.size(); i++ )
        {
            VOI kCurrentGroup = kVOIs.get(i);
            kCurrentGroup.setAllActive(bActive);
            m_kCurrentVOIGroup = kCurrentGroup;
        }
        updateDisplay();
    }

    /**
     * Called from the VOIManager. Sets the current active VOIManager.
     * @param kManager calling VOIManager.
     */
    public void setActive( VOIManager kManager, ModelImage kActiveImage )
    {
        for ( int i = 0; i < m_kVOIManagers.size(); i++ )
        {
            if ( kManager == m_kVOIManagers.elementAt(i) )
            {
                m_iActive = i;
                if ( kActiveImage != m_kParent.getActiveImage() )
                {
                    m_kParent.setActiveImage( kActiveImage );
                }
                break;
            }
        }
    }

    /**
     * Set the color of the button. Derived classes may also perform other functions.
     * @param _button button.
     * @param _color color.
     */
    public void setButtonColor(JButton _button, Color _color)
    {
        if ( (_button != null) && (_color != null) )
        {
            _button.setBackground(_color);
        }
        if ( m_kCurrentVOIGroup != null )
        {
            ColorRGBA kColor = new ColorRGBA( _color.getRed()/255.0f,
                    _color.getGreen()/255.0f,
                    _color.getBlue()/255.0f, 
                    m_kCurrentVOIGroup.getOpacity() );
            m_kCurrentVOIGroup.setColor( _color );
            for ( int i = 0; i < m_kCurrentVOIGroup.getCurves().size(); i++ )
            {
                VOIBase kVOI3D = (m_kCurrentVOIGroup.getCurves().get(i));
                kVOI3D.update( kColor );
            }
        }
        updateDisplay();
    }

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#setCenter(WildMagic.LibFoundation.Mathematics.Vector3f)
     */
    public void setCenter( Vector3f center )
    {
        for (int i = 0; i < m_kVOIManagers.size(); i++)
        {
            m_kVOIManagers.elementAt(i).setCenter(center);
        }
    }

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#setCenter(WildMagic.LibFoundation.Mathematics.Vector3f, boolean)
     */
    public void setCenter( Vector3f center, boolean bParent )
    {
        if ( bParent )
        {
            m_kParent.setCenter(center);
            return;
        }
        setCenter(center);
    }

    /**
     * Called from VOIManager to change the mouse cursor.
     * @param kCursor new Cursor.
     */
    public void setCursor(Cursor kCursor) {
        m_kParent.setCursor(kCursor);    
    }


    /**
     * Called from VOIManager to set the default mouse cursor.
     * This function triggers events in the parent frame.
     */
    public void setDefaultCursor() {         
        toolbarBuilder.setPointerSelected();
        actionPerformed( new ActionEvent ( this, 0, CustomUIBuilder.PARAM_VOI_DEFAULT_POINTER.getActionCommand()) );
    }

    /**
     * Currently not used.
     * @param flag
     */
    public void setEnabled( boolean flag )
    {
        m_bEnabled = flag;
    }

    /**
     * Called from JDialogOpacityControls. Sets the VOI opacity.
     * @param fVal VOI opacity.
     */
    public void setOpacity( float fVal )
    {
        m_fOpacity = fVal;
        if ( m_kCurrentVOIGroup != null )
        {
            m_kCurrentVOIGroup.setOpacity( m_fOpacity );
        }
    }

    /**
     * Called when the Default Pointer Button is not part of the default VOI Toolbar.
     * @param button default pointer button.
     */
    public void setPointerButton( JToggleButton button )
    {
        m_kPointerButton = button;
    }

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#setPresetHue(float)
     */
    public void setPresetHue(float presetHue) {
    	this.presetHue = presetHue;
        int colorIncrement = Preferences.getVOIColorIncrement();
        float hue;
        if (presetHue >= 0.0f) {
            hue = presetHue;
        } else {
            hue = (float) ((((voiUID++ + colorIncrement) * 35) % 360) / 360.0);
        }

        Color color = Color.getHSBColor(hue, 1.0f, 1.0f);
        setButtonColor(toolbarBuilder.getVOIColorButton(), color );
    }

    /**
     * Called from the VOIManager when a new VOI is selected by the mouse.
     * @param kSelected selected VOI.
     * @param bSelectAll when true select all contours in the VOI (usually shift-key is down).
     * @param bUnSelectVOI when true unselect the previously selected VOI 
     * (if the control-key is down multiple different VOIs can be selected).
     */
    public void setSelectedVOI( VOI kSelected, boolean bSelectAll, boolean bUnSelectVOI )
    {
        if ( kSelected == null )
        {
            return;
        }
        if ( bUnSelectVOI )
        {
            selectAllVOIs(false);
        }
        m_kCurrentVOIGroup = kSelected;
        if ( bUnSelectVOI || bSelectAll )
        {
            m_kCurrentVOIGroup.setAllActive(bSelectAll);
        }
        m_kCurrentVOIGroup.setActive(true);
        setButtonColor(toolbarBuilder.getVOIColorButton(), 
                m_kCurrentVOIGroup.getColor());


        m_fOpacity = m_kCurrentVOIGroup.getOpacity();
        fireVOISelectionChange(m_kCurrentVOIGroup);
    }

    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#setVOI_IDs(int, int)
     */
    public void setVOI_IDs(int ID, int UID) {}
    
    /* (non-Javadoc)
     * @see gov.nih.mipav.view.VOIHandlerInterface#showColorDialog()
     */
    public void showColorDialog()
    {
        colorChooser = new ViewJColorChooser(null, "Pick surface color", 
                new OkColorListener(toolbarBuilder.getVOIColorButton()),
                null);
    }

    /**
     * Called from the VOIManager.
     * Displays the Intensity Graph for the input VOIBase.
     * @param kVOI VOIBase to graph.
     */
    public void showIntensityGraph( VOIBase kVOI ) {
        ViewJFrameGraph lineGraph;

        ModelImage kImage = m_kParent.getActiveImage();
        int[] unitsOfMeasure = kImage.getUnitsOfMeasure();
        float[] resolutions = kImage.getResolutions(0);

        kVOI.getLengthPtToPt(resolutions);
        Vector<Vector3f> positions = new Vector<Vector3f>();
        Vector<ColorRGB> colors = new Vector<ColorRGB>();

        int pts = kVOI.findPositionAndIntensity( kImage, positions, colors );

        if ( kImage.isColorImage() )
        {
            float[][] rgbPos = new float[3][pts];
            float[][] rgbColors = new float[3][pts];

            float rgbMeanIntenR = 0;
            float rgbMeanIntenG = 0;
            float rgbMeanIntenB = 0;

            for (int m = 0; m < pts; m++) {
                rgbPos[0][m] = positions.get(m).Z;
                rgbPos[1][m] = positions.get(m).Z;
                rgbPos[2][m] = positions.get(m).Z;

                rgbColors[0][m] = colors.get(m).R;
                rgbColors[1][m] = colors.get(m).G;
                rgbColors[2][m] = colors.get(m).B;
                rgbMeanIntenR += rgbColors[0][m];
                rgbMeanIntenG += rgbColors[1][m];
                rgbMeanIntenB += rgbColors[2][m];
            }
            rgbMeanIntenR /= pts;
            rgbMeanIntenG /= pts;
            rgbMeanIntenB /= pts;


            float rgbStdDevIntenR = 0;
            float rgbStdDevIntenG = 0;
            float rgbStdDevIntenB = 0;
            for (int m = 0; m < pts; m++) {
                float diff = rgbColors[0][m] - rgbMeanIntenR;
                rgbStdDevIntenR += diff * diff;
                diff = rgbColors[1][m] - rgbMeanIntenG;
                rgbStdDevIntenG += diff * diff;
                diff = rgbColors[2][m] - rgbMeanIntenB;
                rgbStdDevIntenB += diff * diff;
            }
            rgbStdDevIntenR = (float)Math.sqrt(rgbStdDevIntenR/pts);
            rgbStdDevIntenG = (float)Math.sqrt(rgbStdDevIntenG/pts);
            rgbStdDevIntenB = (float)Math.sqrt(rgbStdDevIntenB/pts);

            if (kVOI.getGroup().getContourGraph() == null) {
                ViewJFrameGraph contourGraph = new ViewJFrameGraph(rgbPos, rgbColors, "Intensity Graph", kVOI.getGroup(),
                        FileInfoBase.getUnitsOfMeasureAbbrevStr(unitsOfMeasure[0]));

                contourGraph.setDefaultDirectory(ViewUserInterface.getReference().getDefaultDirectory());
                contourGraph.setVisible(true);
                kVOI.getGroup().setContourGraph(contourGraph);
                contourGraph.setVOI(kVOI.getGroup());
            } else {
                kVOI.getGroup().getContourGraph().setUnitsInLabel(FileInfoBase.getUnitsOfMeasureAbbrevStr(unitsOfMeasure[1]));
                kVOI.getGroup().getContourGraph().saveNewFunction(rgbPos, rgbColors, 0);
            }

            ViewUserInterface.getReference().setDataText(
                    "Line\tmean \tstandard deviation " + "\n");
            ViewUserInterface.getReference()
            .setDataText(
                    "Red\t" + rgbMeanIntenR + "\t"
                    + rgbStdDevIntenR + "\n");
            ViewUserInterface.getReference().setDataText(
                    "Green\t" + rgbMeanIntenG + "\t" + rgbStdDevIntenG
                    + "\n");
            ViewUserInterface.getReference().setDataText(
                    "Blue\t" + rgbMeanIntenB + "\t" + rgbStdDevIntenB
                    + "\n");
        } else {
            float[] pos = new float[pts];
            float[] inten = new float[pts];
            int[][] xyCoords = new int[pts][2];

            float min = Float.MAX_VALUE;
            float max = Float.MIN_VALUE;
            float totalInten = 0;
            float rgbMeanIntenR = 0;
            for (int m = 0; m < pts; m++) {
                xyCoords[m][0] = (int)positions.get(m).X;
                xyCoords[m][1] = (int)positions.get(m).Y;
                pos[m] = positions.get(m).Z;
                inten[m] = colors.get(m).R;

                totalInten += inten[m];
                rgbMeanIntenR += inten[m];
                min = Math.min( inten[m], min );
                max = Math.max( inten[m], max );
            }
            rgbMeanIntenR /= pts;
            float rgbStdDevIntenR = 0;
            for (int m = 0; m < pts; m++) {
                float diff = inten[m] - rgbMeanIntenR;
                rgbStdDevIntenR += diff * diff;
            }
            rgbStdDevIntenR = (float)Math.sqrt(rgbStdDevIntenR/pts);

            if (kVOI.getGroup().getContourGraph() == null) {
                lineGraph = new ViewJFrameGraph(pos, inten, "Line VOI Graph", kVOI.getGroup(),
                        FileInfoBase.getUnitsOfMeasureAbbrevStr(unitsOfMeasure[0]),xyCoords);
                lineGraph.setDefaultDirectory(ViewUserInterface.getReference().getDefaultDirectory());
                lineGraph.setVisible(true);
                kVOI.getGroup().setContourGraph(lineGraph);
                lineGraph.setVOI(kVOI.getGroup());
            } else {
                kVOI.getGroup().getContourGraph().setUnitsInLabel(FileInfoBase.getUnitsOfMeasureAbbrevStr(unitsOfMeasure[1]));
                kVOI.getGroup().getContourGraph().replaceFunction(pos, inten, xyCoords, kVOI.getGroup(), 0);
            }

            ViewUserInterface.getReference().setDataText(
                    "Line\tmin \tmax \ttotal \tmean \tstandard deviation " + "\n");
            ViewUserInterface.getReference().setDataText(
                    "\t" + min + "\t" + max + "\t" + totalInten + "\t" + rgbMeanIntenR + "\t" + rgbStdDevIntenR + "\n");
        }
    }


    /**
     * Called from VOIManager. Causes the parent frame to redraw the ModelImage and VOIs.
     */
    public void updateDisplay() {
        m_kParent.setModified(); 
    }

    /**
     * Called from the VOIManager when the VOIBase that is currently being graphed has changed.
     * @param kVOI the modified VOIBase.
     */
    public void updateGraph( VOIBase kVOI )
    {
        if ( kVOI.getType() == VOI.LINE )
        {
            showIntensityGraph(kVOI);
        }
        else if ( kVOI.getType() == VOI.POINT )
        {
            graphPointVOI( kVOI.getGroup(), (VOIPoint)kVOI, kVOI.getGroup().getCurves().indexOf(kVOI),
                    true);
        }
    }

    private void addVOI( ModelImage kImage, VOIBase kNew, boolean bQuickLUT, boolean bUpdate, boolean isFinished )
    {       
        if ( kNew.getGroup() == null )
        {
            saveVOIs("addVOI");
            findCompatibleType(kImage, kNew, isFinished);
            if ( m_kCurrentVOIGroup == null )
            {
                short sID = (short)(kImage.getVOIs().getUniqueID());
                String kName = kNew.getClass().getName();
                int index = kName.lastIndexOf('.') + 1;
                kName = kName.substring(index);
                m_kCurrentVOIGroup = new VOI( sID,  kName + "_" + sID, kNew.getType(), presetHue );
                m_kCurrentVOIGroup.addVOIListener(this);
                m_kCurrentVOIGroup.setOpacity(1f);
                kImage.registerVOI( m_kCurrentVOIGroup );
            }   

            else if ( m_kCurrentVOIGroup.getName().indexOf( "_" ) == 0 )
            {
                String kName = kNew.getClass().getName();
                int index = kName.lastIndexOf('.') + 1;
                kName = kName.substring(index);
                m_kCurrentVOIGroup.setName( kName + m_kCurrentVOIGroup.getName() );
            }
            if ( kNew instanceof VOIPoint &&  kNew.getType() == VOI.POLYLINE_SLICE )
            {
                VOIPoint kNewPoint = (VOIPoint)kNew;
                boolean bAdded = false;
                for ( int i = 0; i < m_kCurrentVOIGroup.getCurves().size(); i++ )
                {
                    if ( m_kCurrentVOIGroup.getCurves().get(i).isActive() )
                    {
                        ((VOIPolyLineSlice)m_kCurrentVOIGroup.getCurves().get(i)).add(kNewPoint);
                        bAdded = true;
                        break;
                    }                    
                }
                if ( !bAdded && m_kCurrentVOIGroup.getCurves().size() > 0 )
                {
                    ((VOIPolyLineSlice)m_kCurrentVOIGroup.getCurves().lastElement()).add(kNewPoint);
                    bAdded = true;
                }
                if ( !bAdded )
                {
                    m_kCurrentVOIGroup.getCurves().add( new VOIPolyLineSlice(kNewPoint) );             
                }
            }
            else
            {
                m_kCurrentVOIGroup.getCurves().add(kNew);
            }
        }
        else
        {
            m_kCurrentVOIGroup = kNew.getGroup();
        }
        if ( !bQuickLUT )
        {
            setCurrentColor();
        }
        else
        {
            m_kCurrentVOIGroup.setColor( Color.yellow );
        }
        if ( bUpdate )
        {
            updateDisplay();
        }
    }
    private void changeVOIOrder(boolean doContour, int direction) {
        if ((direction != VOI.FORWARD) && (direction != VOI.BACKWARD)
                && (direction != VOI.FRONT) && (direction != VOI.BACK))
        {
            return;
        }

        ViewVOIVector VOIs = m_kParent.getActiveImage().getVOIs();
        for (int i = 0; i < VOIs.size(); i++) {

            if (VOIs.VOIAt(i).isActive() && VOIs.VOIAt(i).isVisible()) {

                if (!doContour) {
                    if ( (i == VOIs.size()-1) && (direction == VOI.FORWARD  || direction == VOI.FRONT) )
                    {
                        return;
                    }
                    if ( i == 0 && (direction == VOI.BACKWARD || direction == VOI.BACK ) )
                    {
                        return;
                    }
                    VOI kVOI = VOIs.remove(i);
                    if ( direction == VOI.FORWARD )
                    {
                        VOIs.add( i+1, kVOI );
                        //compImage.getActiveImage().notifyImageDisplayListeners(null, true);
                        return;
                    }
                    if ( direction == VOI.BACKWARD )
                    {
                        VOIs.add( i-1, kVOI );
                        //compImage.getActiveImage().notifyImageDisplayListeners(null, true);
                        return;                        
                    }
                    if ( direction == VOI.FRONT )
                    {
                        VOIs.add( kVOI );
                        //compImage.getActiveImage().notifyImageDisplayListeners(null, true);
                        return;
                    }
                    VOIs.add( 0, kVOI );
                    //compImage.getActiveImage().notifyImageDisplayListeners(null, true);
                    return;                      
                }

                Vector<VOIBase> curves = VOIs.VOIAt(i).getCurves();

                for (int j = 0; j < curves.size(); j++) {
                    if (curves.get(j).isActive()) {

                        if ( (j == curves.size()-1) && (direction == VOI.FORWARD  || direction == VOI.FRONT) )
                        {
                            return;
                        }
                        if ( j == 0 && (direction == VOI.BACKWARD || direction == VOI.BACK ) )
                        {
                            return;
                        }
                        VOIBase kVOI = curves.remove(j);
                        if ( direction == VOI.FORWARD )
                        {
                            curves.add( j+1, kVOI );
                            //compImage.getActiveImage().notifyImageDisplayListeners(null, true);
                            return;
                        }
                        if ( direction == VOI.BACKWARD )
                        {
                            curves.add( j-1, kVOI );
                            //compImage.getActiveImage().notifyImageDisplayListeners(null, true);
                            return;                        
                        }
                        if ( direction == VOI.FRONT )
                        {
                            curves.add( kVOI );
                            //compImage.getActiveImage().notifyImageDisplayListeners(null, true);
                            return;
                        }
                        curves.add( 0, kVOI );
                        //compImage.getActiveImage().notifyImageDisplayListeners(null, true);
                        return;                      
                    }
                }
            }
        }
        MipavUtil.displayWarning("Please select a VOI!");
    }


    private boolean checkForActiveVOIs() {
        boolean foundActive = false;
        ViewVOIVector VOIs;
        int nVOI;

        VOIs = m_kParent.getActiveImage().getVOIs();
        nVOI = VOIs.size();

        for (int i = 0; i < nVOI; i++) {

            if (VOIs.VOIAt(i).isActive() && VOIs.VOIAt(i).isVisible()) {

                foundActive = true;
                break;
            }
        }

        return foundActive;
    }


    private int copy()
    {        
        // Get the reference to the Global copy list:
        Vector<VOIBase> copyList = ViewUserInterface.getReference().getCopyVOIs();
        copyList.clear();
        VOIVector kVOIs = m_kParent.getActiveImage().getVOIs();
        for ( int i = 0; i < kVOIs.size(); i++ )
        {
            VOI kCurrentGroup = kVOIs.get(i);
            for ( int j = 0; j < kCurrentGroup.getCurves().size(); j++ )
            {
                VOIBase kCurrentVOI = kCurrentGroup.getCurves().get(j);
                if ( kCurrentVOI.isActive() && !copyList.contains(kCurrentVOI) )
                {
                    copyList.add(kCurrentVOI);
                }
            }
        }
        return copyList.size();
    }

    private void createMask( ModelImage kActive, boolean bInside )
    {
        int iSize = kActive.getSize();
        if ( kActive.isColorImage() )
        {
            iSize /= 4;
        }
        kActive.createMask(iSize);
        boolean bMask = true;
        for (int i = 0; i < m_kVOIManagers.size(); i++) {
            bMask &= make3DVOI( false, kActive, kActive, kActive.getMask(), i);
        }
        if ( !bMask )
        {
            return;
        }
        if ( !bInside )
        {
            kActive.getMask().flip(0, iSize );
        }
        kActive.useMask(true);
        m_kParent.updateData(false);
    }

    private void createMask( String command )
    {
        if (getActiveImage().getVOIs().size() < 1) {
            MipavUtil.displayWarning("Must have at least one VOI to perform quick mask");
            return;
        }
        if ( m_bGPURenderer )
        {
            createMask( getActiveImage(), command.equals(CustomUIBuilder.PARAM_VOI_QUICK_AND_OP.getActionCommand()) );
        }
        else
        {
            new JDialogMask(getActiveImage(), false, command.equals(CustomUIBuilder.PARAM_VOI_QUICK_NOT_OP.getActionCommand()));
        }
    }

    private int cut()
    {
        int count = copy();
        if ( count > 0 )
        {            
            saveVOIs(CustomUIBuilder.PARAM_VOI_CUT.getActionCommand());
            deleteActiveVOI();
        }
        return count;
    }

    private void deleteActiveVOI()
    {
        Vector<VOIBase> deleteList = new Vector<VOIBase>();

        VOIVector kVOIs = m_kParent.getActiveImage().getVOIs();
        for ( int i = 0; i < kVOIs.size(); i++ )
        {
            VOI kCurrentGroup = kVOIs.get(i);
            for ( int j = 0; j < kCurrentGroup.getCurves().size(); j++ )
            {
                VOIBase kCurrentVOI = kCurrentGroup.getCurves().get(j);
                if ( kCurrentVOI.isActive() && !deleteList.contains(kCurrentVOI) )
                {
                    deleteList.add(kCurrentVOI);
                }
            }
        }
        while ( deleteList.size() > 0 )
        {
            deleteVOI( deleteList.remove(0) );
        }
    }


    private void deleteAllVOI()
    {
        if ( m_kImageA != null ) { m_kImageA.unregisterAllVOIs(); }
        if ( m_kImageB != null ) { m_kImageB.unregisterAllVOIs(); }
    }

    private void deleteVOIActivePt()
    {
        Vector<VOIBase> activeList = new Vector<VOIBase>();

        VOIVector kVOIs = m_kParent.getActiveImage().getVOIs();
        for ( int i = 0; i < kVOIs.size(); i++ )
        {
            VOI kCurrentGroup = kVOIs.get(i);
            for ( int j = 0; j < kCurrentGroup.getCurves().size(); j++ )
            {
                VOIBase kCurrentVOI = kCurrentGroup.getCurves().get(j);
                if ( kCurrentVOI.isActive() && !activeList.contains(kCurrentVOI) )
                {
                    activeList.add(kCurrentVOI);
                }
            }
        }

        Vector<VOIBase> deleteList = new Vector<VOIBase>();
        for ( int i = 0; i < activeList.size(); i++ )
        {
            VOIBase kCurrentVOI = activeList.get(i);
            VOIManager kManager = m_kVOIManagers.elementAt(0);
            for ( int j = 0; j < m_kVOIManagers.size(); j++ )
            {
                if ( kCurrentVOI.getPlane() == m_kVOIManagers.elementAt(j).getPlane() )
                {
                    kManager = m_kVOIManagers.elementAt(j);
                    break;
                }
            }
            if ( kManager.deleteVOIActivePt( kCurrentVOI ) <= 0 )
            {
                deleteList.add( kCurrentVOI );
            }            
        }

        while ( deleteList.size() > 0 )
        {
            deleteVOI( deleteList.remove(0) );
        }
        updateDisplay();
    }
    
    private void evolveBoundary2D(String command)
    {
    	m_bDefaultImage = true;
    	m_kTempImage = m_kVOIManagers.get(m_iActive).getLocalImage();
        if ( m_kTempImage != getActiveImage() )
        {
        	m_bDefaultImage = false;
        	m_kTempImage = (ModelImage)m_kTempImage.clone();
        }
        saveVOIs(command);
    	if (command.equals("Snake") ) {
    		JDialogSnake kEvolve = new JDialogSnake(m_kParent.getFrame(), m_kTempImage);
    		kEvolve.setVOIManager(this);
    	} 
    	else if (command.equals("AGVF")) {
    		JDialogAGVF kEvolve = new JDialogAGVF(m_kParent.getFrame(), m_kTempImage);
    		kEvolve.setVOIManager(this);
    	} 
    	else if (command.equals("GVF")) {
    		JDialogGVF kEvolve = new JDialogGVF(m_kParent.getFrame(), m_kTempImage);
    		kEvolve.setVOIManager(this);
    	}
    	else if (command.equals("BSnake")) {
    		JDialogBSnake kEvolve = new JDialogBSnake(m_kParent.getFrame(), m_kTempImage);
    		kEvolve.setVOIManager(this);
    	}
    }

    private void findCompatibleType( ModelImage kImage, VOIBase kNew, boolean isFinished)
    {
        if ( kNew.isQuickLUT() )
        {
            return;
        }
        if ( m_kCurrentVOIGroup != null && m_kCurrentVOIGroup.getCurveType() == kNew.getType() )
        {
            return;
        }
        if ( m_kCurrentVOIGroup != null && m_kCurrentVOIGroup.isEmpty() )
        {
            short sID = (short)(kImage.getVOIs().getUniqueID());
            m_kParent.getActiveImage().unregisterVOI(m_kCurrentVOIGroup);
            String kName = kNew.getClass().getName();
            int index = kName.lastIndexOf('.') + 1;
            kName = kName.substring(index);
            m_kCurrentVOIGroup.setName( kName + "_" + sID );
            m_kCurrentVOIGroup.setCurveType(kNew.getType());
            return;
        }
        VOIVector kVOIs = kImage.getVOIs();
        for ( int i = 0; i < kVOIs.size(); i++ )
        {
            if ( kVOIs.get(i).isActive() && matches(kVOIs.get(i).getCurveType(), kNew.getType(), isFinished) )
            {
                m_kCurrentVOIGroup = kVOIs.get(i);
                if (presetHue >= 0.0f) {
                	m_kCurrentVOIGroup.setColor(presetHue);
                }
                setSelectedVOI(m_kCurrentVOIGroup, false, true);
                return;
            }
        }
        for ( int i = 0; i < kVOIs.size(); i++ )
        {
            if ( matches(kVOIs.get(i).getCurveType(), kNew.getType(), isFinished) )
            {
                m_kCurrentVOIGroup = kVOIs.get(i);
                if (presetHue >= 0.0f) {
                	m_kCurrentVOIGroup.setColor(presetHue);
                }
                setSelectedVOI(m_kCurrentVOIGroup, false, true);
                return;
            }
        }
        if ( m_kCurrentVOIGroup != null )
        {
            newVOI(false, kNew.isSplit());
        }
        m_kCurrentVOIGroup = null;
    }

    private int getActiveVOICount()
    {
        ModelImage kActive = m_kParent.getActiveImage();
        ViewVOIVector VOIs = kActive.getVOIs();
        int nActive = 0;
        for (int i = 0; i < VOIs.size(); i++) {
            if (VOIs.VOIAt(i).isActive()) {
                nActive++;
            }
        }
        return nActive;
    }

    private JFrame getFrame() {
        return m_kParent.getFrame();
    }

    private int getSlice() {
        return (int)m_kParent.getCenterPt().Z;
    }

    private VOISaveState getVOIState( )
    {
        VOISaveState kVOIState = new VOISaveState();
        kVOIState.voiVectorA = m_kImageA.getVOIsCopy();
        if ( m_kImageB != null )
        {
            kVOIState.voiVectorB = m_kImageB.getVOIsCopy();
        }
        if ( m_kCurrentVOIGroup != null )
        {
            kVOIState.currentVOI = m_kImageA.isRegistered( m_kCurrentVOIGroup );
        }
        else
        {
            kVOIState.currentVOI = -1;
        }        
        kVOIState.currentCenter.Copy( m_kParent.getCenterPt() );
        return kVOIState;
    }

    private void graphPointVOI(VOI v, VOIPoint voiPt, int j,
            boolean useFrameRefTime) {

        /** Buffer for holding intensities at specific points. */
        float[] ptIntensity;

        /** buffer used when graphing a VOIPoint on a grayscale image. */
        float[] ptPosition;

        /** Buffer for holding RGB intensities [0,1,2] at specific points. */
        float[][] ptRGBIntensities = null;

        /** Buffer used for graphing VOIPoints for an RGB image. */
        float[][] ptRGBPositions = null;


        int t, s;
        Vector3f pt;
        FileInfoDicom fileInfo;
        String frameRefTimeString;
        int frameReferenceTime;

        ModelImage kImage = m_kParent.getActiveImage();

        if ((kImage.getNDims() != 3)
                && (kImage.getNDims() != 4)) {
            return;
        }

        if (kImage.getNDims() == 3) {

            if (kImage.isColorImage() == true) {
                ptRGBPositions = new float[3][kImage
                                              .getExtents()[2]];
                ptRGBIntensities = new float[3][kImage
                                                .getExtents()[2]];
                pt = voiPt.exportPoint();

                for (s = 0; s < kImage.getExtents()[2]; s++) {

                    for (int c = 0; c < 3; c++) {
                        ptRGBPositions[c][s] = s;
                        ptRGBIntensities[c][s] = kImage
                        .getFloat(
                                ((4 * ((s * kImage
                                        .getSliceSize())
                                        + ((int) pt.Y * kImage
                                                .getExtents()[0]) + (int) pt.X))
                                                + c + 1));
                    }
                }

                if (v.getContourGraph() == null) {
                    ViewJFrameGraph contourGraph = new ViewJFrameGraph(
                            ptRGBPositions, ptRGBIntensities,
                            "Intensity Graph", v, FileInfoBase
                            .getUnitsOfMeasureAbbrevStr(kImage.getFileInfo(0)
                                    .getUnitsOfMeasure(0)));

                    contourGraph.setDefaultDirectory(ViewUserInterface
                            .getReference().getDefaultDirectory());
                    contourGraph.setVisible(false);
                    v.setContourGraph(contourGraph);
                } else {
                    v.getContourGraph().setUnitsInLabel(
                            FileInfoBase.getUnitsOfMeasureAbbrevStr(kImage.getFileInfo(0)
                                    .getUnitsOfMeasure(0)));
                    v.getContourGraph().saveNewFunction(ptRGBPositions,
                            ptRGBIntensities, j);
                }

                return;
            }
            try {
                ptPosition = new float[kImage.getExtents()[2]];
                ptIntensity = new float[kImage.getExtents()[2]];

                for (s = 0; s < kImage.getExtents()[2]; s++) {

                    pt = voiPt.exportPoint();
                    ptPosition[s] = s;
                    ptIntensity[s] = kImage.getFloat(
                            (int) ((s * kImage
                                    .getSliceSize())
                                    + (pt.Y * kImage
                                            .getExtents()[0]) + pt.X));
                }

                if (v.getContourGraph() == null) {
                    ViewJFrameGraph contourGraph = new ViewJFrameGraph(
                            ptPosition, ptIntensity, "Intensity Graph", v,
                            FileInfoBase
                            .getUnitsOfMeasureAbbrevStr(kImage
                                    .getFileInfo(0)
                                    .getUnitsOfMeasure(0)),null);

                    contourGraph.setDefaultDirectory(ViewUserInterface
                            .getReference().getDefaultDirectory());
                    contourGraph.setVisible(false);
                    v.setContourGraph(contourGraph);
                } else if (useFrameRefTime) {
                    v.getContourGraph().setUnitsInLabel(
                            FileInfoBase
                            .getUnitsOfMeasureAbbrevStr(kImage
                                    .getFileInfo(0)
                                    .getUnitsOfMeasure(0)));
                    v.getContourGraph().update(ptPosition,
                            ptIntensity, j);
                }  else {
                    v.getContourGraph().setUnitsInLabel(
                            FileInfoBase
                            .getUnitsOfMeasureAbbrevStr(kImage
                                    .getFileInfo(0)
                                    .getUnitsOfMeasure(0)));
                    v.getContourGraph().saveNewFunction(ptPosition,
                            ptIntensity, j);
                }

                return;
            } catch (OutOfMemoryError error) {
                System.gc();
                MipavUtil
                .displayError("Out of memory: ComponentEditImage.graphVOI");

                return;
            }
        } else if (kImage.getNDims() == 4) {
            int xDim = kImage.getExtents()[0];
            int yDim = kImage.getExtents()[1];
            int zDim = kImage.getExtents()[2];

            try {
                ptPosition = new float[kImage.getExtents()[3]];
                ptIntensity = new float[kImage.getExtents()[3]];

                for (t = 0; t < kImage.getExtents()[3]; t++) {

                    pt = voiPt.exportPoint();
                    if (useFrameRefTime) {
                        fileInfo = (FileInfoDicom) (kImage
                                .getFileInfo(t * zDim));
                        frameRefTimeString = ((String) fileInfo.getTagTable()
                                .getValue("0054,1300")).trim();
                        if (frameRefTimeString != null) {
                            try {
                                frameReferenceTime = new Integer(
                                        frameRefTimeString).intValue();
                            } catch (NumberFormatException e) {
                                MipavUtil
                                .displayError("Number format excepton from frame Reference Time String = "
                                        + frameRefTimeString);
                                return;
                            }

                            ptPosition[t] = frameReferenceTime;
                        } // if (frameRefTimeString != null)
                        else {
                            MipavUtil
                            .displayError("Frame reference time string is null");
                            return;
                        }
                    } // if (useFrameRefTime)
                    else {
                        ptPosition[t] = t;
                    }
                    ptIntensity[t] = kImage
                    .getFloat(
                            (int) ((t * xDim * yDim * zDim)
                                    + (pt.Z * xDim * yDim)
                                    + (pt.Y * xDim) + pt.X));
                }

                if (v.getContourGraph() == null) {
                    ViewJFrameGraph contourGraph = new ViewJFrameGraph(
                            ptPosition, ptIntensity, "Intensity Graph", v,
                            FileInfoBase.getUnitsOfMeasureAbbrevStr(kImage.getFileInfo(0)
                                    .getUnitsOfMeasure(3)),null);
                    contourGraph.setDefaultDirectory(ViewUserInterface
                            .getReference().getDefaultDirectory());
                    contourGraph.setVisible(false);
                    v.setContourGraph(contourGraph);
                } else if (useFrameRefTime) {
                    v.getContourGraph().setUnitsInLabel(
                            FileInfoBase.getUnitsOfMeasureAbbrevStr(kImage.getFileInfo(0)
                                    .getUnitsOfMeasure(3)));
                    v.getContourGraph().replaceFunction(ptPosition,
                            ptIntensity, null, v, j);
                } else {
                    v.getContourGraph().setUnitsInLabel(
                            FileInfoBase.getUnitsOfMeasureAbbrevStr(kImage.getFileInfo(0)
                                    .getUnitsOfMeasure(3)));
                    v.getContourGraph().saveNewFunction(ptPosition,
                            ptIntensity, j);
                }

                return;

            } catch (OutOfMemoryError error) {
                System.gc();
                MipavUtil
                .displayError("Out of memory: ComponentEditImage.graphVOI");

                return;
            }
        }

    }

    private void graphVOI()
    {
        VOIVector kVOIs = m_kParent.getActiveImage().getVOIs();
        for ( int i = 0; i < kVOIs.size(); i++ )
        {
            VOI kCurrentGroup = kVOIs.get(i);
            for ( int j = 0; j < kCurrentGroup.getCurves().size(); j++ )
            {
                VOIBase kCurrentVOI = kCurrentGroup.getCurves().get(j);
                if ( kCurrentVOI.isActive() )
                {
                    showIntensityGraph( kCurrentVOI );
                }
            }
        }
    }
    
    private void interpolateVOIs()
    {
        ModelImage kImage = m_kVOIManagers.get(m_iActive).getLocalImage();
        if ( kImage == getActiveImage() )
        {
            new JDialogVOIShapeInterpolation(getActiveImage());
        }
        else
        {
        	kImage = (ModelImage)kImage.clone();
            kImage.calcMinMax();            
            new JDialogVOIShapeInterpolation(kImage, false);                  
            int[] axisA = getActiveImage().getAxisOrientation();
            int[] axisB = kImage.getAxisOrientation();
            int[] axisOrder = { 0, 1, 2, 3 };
            boolean[] axisFlip = { false, false, false, false };
            if ( MipavCoordinateSystems.matchOrientation( axisA, axisB, axisOrder, axisFlip ) )
            {
                AlgorithmRotate rotateAlgo = new AlgorithmRotate( kImage, axisOrder, axisFlip );
                rotateAlgo.setRunningInSeparateThread(false);
                rotateAlgo.run();
                kImage = rotateAlgo.returnImage();
                getActiveImage().unregisterAllVOIs();            
                getActiveImage().setVOIs( kImage.getVOIs() );
                
                if ( getActiveImage() != null && getActiveImage().getVOIs() != null )
                {
                    getActiveImage().getVOIs().addVectorListener(this);
                    VOIVector kVOIs = getActiveImage().getVOIs();
                    for ( int i = 0; i < kVOIs.size(); i++ )
                    {
                        kVOIs.elementAt(i).addVOIListener(this);
                    }
                }
            }
        }
        if ( imageStatList != null )
        {
            imageStatList.refreshVOIList(getActiveImage().getVOIs());
        }
        if ( m_kVOIDialog != null )
        {
            ModelImage kActive = getActiveImage();
            ViewVOIVector VOIs = kActive.getVOIs();
            for (int i = 0; i < VOIs.size(); i++) {
                if (VOIs.VOIAt(i).isActive()) {
                    m_kVOIDialog.updateVOI( VOIs.VOIAt(i), kActive );
                    m_kVOIDialog.updateTree();
                    return;
                }
            }
            if (VOIs.size() > 0) {
                m_kVOIDialog.updateVOI( VOIs.VOIAt(0), kActive );
                m_kVOIDialog.updateTree();
            }
        }
    }

    private boolean isDrawCommand( String kCommand )
    {
        if ( kCommand.equals(CustomUIBuilder.PARAM_VOI_PROTRACTOR.getActionCommand()) ||
                kCommand.equals(CustomUIBuilder.PARAM_VOI_LIVEWIRE.getActionCommand()) ||
                kCommand.equals(CustomUIBuilder.PARAM_VOI_SPLITTER.getActionCommand()) ||
                kCommand.equals(CustomUIBuilder.PARAM_VOI_LINE.getActionCommand()) ||
                kCommand.equals(CustomUIBuilder.PARAM_VOI_POLY_SLICE.getActionCommand()) || 
                kCommand.equals(CustomUIBuilder.PARAM_VOI_POINT.getActionCommand()) ||
                kCommand.equals(CustomUIBuilder.PARAM_VOI_TEXT.getActionCommand()) || 
                kCommand.equals(CustomUIBuilder.PARAM_VOI_3D_RECTANGLE.getActionCommand()) ||
                kCommand.equals(CustomUIBuilder.PARAM_VOI_RECTANGLE.getActionCommand()) ||
                kCommand.equals(CustomUIBuilder.PARAM_VOI_ELLIPSE.getActionCommand()) || 
                kCommand.equals(CustomUIBuilder.PARAM_VOI_POLYGON.getActionCommand()) || 
                kCommand.equals(CustomUIBuilder.PARAM_VOI_LEVELSET.getActionCommand()) ||
                kCommand.equals(CustomUIBuilder.PARAM_LUT_QUICK.getActionCommand() ) 
                || kCommand.equals("quickLUT") ) {
            return true;
        } 
        return false;
    }


    /**
     * This method loads all VOIs to the active image from the default VOI directory for that image.
     * @param quietMode if true indicates that warnings should not be displayed.
     */
    private void loadAllVOIs(boolean quietMode) {
        ModelImage img = m_kParent.getActiveImage();

        String fileDir = img.getFileInfo(0).getFileDirectory();
        String imageName;

        // if the image is a dicom image, then base the new directory name
        // on the actual filename, not the image name
        if (img.isDicomImage()) {
            imageName = img.getFileInfo(0).getFileName();

            final int index = imageName.lastIndexOf(".");

            if (index > 0) {
                imageName = imageName.substring(0, index);
            }

            // now, get rid of any numbers at the end of the name (these
            // are part of the dicom file name, but we only want the 'base'
            // part of the name
            int newIndex = imageName.length();

            for (int i = imageName.length() - 1; i >= 0; i--) {
                final char myChar = imageName.charAt(i);

                if (Character.isDigit(myChar)) {
                    newIndex = i;
                } else {
                    break;
                } // as soon as something is NOT a digit, leave loop
            }

            if (newIndex == 0) {

                // give the base name a generic name
                imageName = new String("DICOM");
            } else {
                imageName = imageName.substring(0, newIndex);
            }
        } else {
            imageName = img.getImageName();
        }

        // get rid of any '^' and ',' which may exist in dicom images
        imageName = imageName.replace('^', '_');
        imageName = imageName.replace(',', '_');

        String voiDir = new String(fileDir + File.separator + "defaultVOIs_" + imageName + File.separator);

        loadAllVOIsFrom(voiDir, quietMode);

    } // end loadAllVOIs()

    /**
     * This method loads all VOIs to the active image from a given directory.
     * @param voiDir the directory to load voi's from
     * @param quietMode if true indicates that warnings should not be displayed.
     */
    private void loadAllVOIsFrom(final String voiDir, boolean quietMode) {

        int i, j;
        VOI[] VOIs;
        FileVOI fileVOI;
        ModelImage currentImage = m_kParent.getActiveImage();

        try {

            // if voiDir does not exist, then return
            // if voiDir exists, then get list of voi's from directory (*.voi)
            final File voiFileDir = new File(voiDir);
            final Vector<String> filenames = new Vector<String>();
            final Vector<Boolean> isLabel = new Vector<Boolean>();

            if (voiFileDir.exists() && voiFileDir.isDirectory()) {

                // get list of files
                final File[] files = voiFileDir.listFiles();

                for (final File element : files) {

                    if (element.getName().endsWith(".voi") || element.getName().endsWith(".xml")) {
                        filenames.add(element.getName());
                        isLabel.add(false);
                    } else if (element.getName().endsWith(".lbl")) {
                        filenames.add(element.getName());
                        isLabel.add(true);
                    }
                }
            } else { // voiFileDir either doesn't exist, or isn't a directory

                if ( !quietMode) {
                    MipavUtil.displayError("No VOIs are found in directory: " + voiDir);
                }

                return;
            }

            // open each voi array, then register voi array to this image
            for (i = 0; i < filenames.size(); i++) {

                fileVOI = new FileVOI( (filenames.elementAt(i)), voiDir, currentImage);

                VOIs = fileVOI.readVOI(isLabel.get(i));

                for (j = 0; j < VOIs.length; j++) {
                    currentImage.registerVOI(VOIs[j]);
                    VOIs[j].getGeometricCenter();
                    VOIs[j].addVOIListener(this);
                }
            }

            // when everything's done, notify the image listeners
            currentImage.notifyImageDisplayListeners();

        } catch (final Exception error) {

            if ( !quietMode) {
                MipavUtil.displayError("Error loading all VOIs from " + voiDir + ": " + error);
            }
        }

    } // end loadAllVOIsFrom()


    private boolean make3DVOI( boolean bIntersection, ModelImage kSrc, ModelImage kVolume, BitSet kMask, int iValue )
    {
        VOIVector kVOIs = kSrc.getVOIs();

        boolean bCreated = false;
        for ( int i = 0; i < kVOIs.size(); i++ )
        {
            VOI kCurrentGroup = kVOIs.get(i);
            for ( int j = 0; j < kCurrentGroup.getCurves().size(); j++ )
            {
                VOIBase kCurrentVOI = kCurrentGroup.getCurves().get(j);
                if ( kCurrentVOI.isActive() )
                {
                    bCreated = true;
                    kCurrentVOI.fillVolume( kVolume, kMask, bIntersection, iValue );     
                }
            }
        }
        updateDisplay();        
        return bCreated;
    }

    private boolean matches( int iType1, int iType2, boolean isFinished )
    {
        if ( iType1 == iType2 )
        {
            return true;
        }
        if ( isFinished )
        {
            return false;
        }
        if ( (iType1 == VOI.CONTOUR || iType1 == VOI.POLYLINE) &&
                (iType2 == VOI.CONTOUR || iType2 == VOI.POLYLINE) )
        {
            return true;
        }
        return false;   
    }

    /**
     * This method opens an existing VOI.
     * @param quietMode if true indicates that warnings should not be displayed.
     * @param doLabels DOCUMENT ME!
     * @return whether a VOI was successfully opened (ie - the dialog wasn't cancelled)
     */
    private boolean openVOI(boolean quietMode, final boolean doLabels) {
        ViewOpenVOIUI openVOI = null;

        try {
            openVOI = new ViewOpenVOIUI();
            VOI[] newVOIs = openVOI.open(m_kParent.getActiveImage(), doLabels);
            if ( newVOIs == null) {
                return false;
            }
            for ( int i = 0; i < newVOIs.length; i++ )
            {
                newVOIs[i].getGeometricCenter();
                newVOIs[i].addVOIListener(this);
            }
        } catch (final OutOfMemoryError error) {

            if ( !quietMode) {
                MipavUtil.displayError("Out of memory: ViewJFrameBase.openVOI");
            }

            return false;
        }

        return true;
    }


    private void paintToVOI()
    {
        ModelImage kImage = m_kVOIManagers.get(m_iActive).getLocalImage();
        if ( kImage == getActiveImage() )
        {
            int xDim = kImage.getExtents().length > 0 ? kImage.getExtents()[0] : 1;
            int yDim = kImage.getExtents().length > 1 ? kImage.getExtents()[1] : 1;
            int zDim = kImage.getExtents().length > 2 ? kImage.getExtents()[2] : 1;

            short voiID = (short) kImage.getVOIs().size();

            final AlgorithmVOIExtractionPaint algoPaintToVOI = new AlgorithmVOIExtractionPaint(kImage,
            		kImage.getMask(), xDim, yDim, zDim, voiID);

            algoPaintToVOI.setRunningInSeparateThread(false);
            algoPaintToVOI.run();

            ScriptRecorder.getReference().addLine(new ActionPaintToVOI(kImage));
            ProvenanceRecorder.getReference().addLine(new ActionMaskToPaint(kImage));
        }
        else
        {    
        	kImage = (ModelImage)kImage.clone();
            int xDim = kImage.getExtents().length > 0 ? kImage.getExtents()[0] : 1;
            int yDim = kImage.getExtents().length > 1 ? kImage.getExtents()[1] : 1;
            int zDim = kImage.getExtents().length > 2 ? kImage.getExtents()[2] : 1;

            short voiID = (short) kImage.getVOIs().size();

            final AlgorithmVOIExtractionPaint algoPaintToVOI = new AlgorithmVOIExtractionPaint(kImage,
            		kImage.getMask(), xDim, yDim, zDim, voiID);

            algoPaintToVOI.setRunningInSeparateThread(false);
            algoPaintToVOI.run();

            ScriptRecorder.getReference().addLine(new ActionPaintToVOI(kImage));
            ProvenanceRecorder.getReference().addLine(new ActionMaskToPaint(kImage));
            
            int[] axisA = getActiveImage().getAxisOrientation();
            int[] axisB = kImage.getAxisOrientation();
            int[] axisOrder = { 0, 1, 2, 3 };
            boolean[] axisFlip = { false, false, false, false };
            if ( MipavCoordinateSystems.matchOrientation( axisA, axisB, axisOrder, axisFlip ) )
            {
                AlgorithmRotate rotateAlgo = new AlgorithmRotate( kImage, axisOrder, axisFlip );
                rotateAlgo.setRunningInSeparateThread(false);
                rotateAlgo.run();
                kImage = rotateAlgo.returnImage();
                getActiveImage().unregisterAllVOIs();            
                getActiveImage().setVOIs( kImage.getVOIs() );
                

                if ( getActiveImage() != null && getActiveImage().getVOIs() != null )
                {
                    getActiveImage().getVOIs().addVectorListener(this);
                    VOIVector kVOIs = getActiveImage().getVOIs();
                    for ( int i = 0; i < kVOIs.size(); i++ )
                    {
                        kVOIs.elementAt(i).addVOIListener(this);
                    }
                }
            }
        }

        if ( imageStatList != null )
        {
            imageStatList.refreshVOIList(getActiveImage().getVOIs());
        }
        if ( m_kVOIDialog != null )
        {
            ModelImage kActive = getActiveImage();
            ViewVOIVector VOIs = kActive.getVOIs();
            for (int i = 0; i < VOIs.size(); i++) {
                if (VOIs.VOIAt(i).isActive()) {
                    m_kVOIDialog.updateVOI( VOIs.VOIAt(i), kActive );
                    m_kVOIDialog.updateTree();
                    return;
                }
            }
            if (VOIs.size() > 0) {
                m_kVOIDialog.updateVOI( VOIs.VOIAt(0), kActive );
                m_kVOIDialog.updateTree();
            }
        }
        updateDisplay();
    }

    private void paste()
    {
        // Get the Global copy list:
        Vector<VOIBase> copyList = ViewUserInterface.getReference().getCopyVOIs();
        // Return if no contours to paste:
        if ( copyList.size() == 0 )
        {
            return;
        }
        saveVOIs(CustomUIBuilder.PARAM_VOI_PASTE.getActionCommand());
        // If the copy list is from another image:
        if ( copyList.elementAt(0).getGroup() != null && !copyList.elementAt(0).getGroup().hasListener(this) )
        {
            pasteFromViewUserInterface();
            return;
        }
        // The copy list is from this image/manager: 
        for ( int i = 0; i < copyList.size(); i++ )
        {
            VOIBase kCurrentVOI = copyList.get(i); 
            VOIManager kManager = m_kVOIManagers.elementAt(0);
            for ( int j = 0; j < m_kVOIManagers.size(); j++ )
            {
                if ( kCurrentVOI.getPlane() == m_kVOIManagers.elementAt(j).getPlane() )
                {
                    kManager = m_kVOIManagers.elementAt(j);
                    break;
                }
            }

            kManager.pasteVOI( kCurrentVOI );  
        }
    }
    


    
    private void propagate(int dir)
    {
        // Get the Global copy list:
        Vector<VOIBase> copyList = ViewUserInterface.getReference().getCopyVOIs();
        // Return if no contours to paste:
        if ( copyList.size() == 0 )
        {
            return;
        }
        // If the copy list is from another image:
        if ( copyList.elementAt(0).getGroup() != null && !copyList.elementAt(0).getGroup().hasListener(this) )
        {
            pasteFromViewUserInterface();
            return;
        }
        // The copy list is from this image/manager: 
        for ( int i = 0; i < copyList.size(); i++ )
        {
            VOIBase kCurrentVOI = copyList.get(i); 
            VOIManager kManager = m_kVOIManagers.elementAt(0);
            for ( int j = 0; j < m_kVOIManagers.size(); j++ )
            {
                if ( kCurrentVOI.getPlane() == m_kVOIManagers.elementAt(j).getPlane() )
                {
                    kManager = m_kVOIManagers.elementAt(j);
                    break;
                }
            }

            kManager.propagateVOI( kCurrentVOI, dir );  
        }
    }

    private void pasteAll()
    {
        // Get the Global copy list:
        Vector<VOIBase> copyList = ViewUserInterface.getReference().getCopyVOIs();
        for ( int i = 0; i < copyList.size(); i++ )
        {
            VOIBase kCurrentVOI = copyList.get(i); 
            VOIManager kManager = m_kVOIManagers.elementAt(0);
            for ( int j = 0; j < m_kVOIManagers.size(); j++ )
            {
                if ( kCurrentVOI.getPlane() == m_kVOIManagers.elementAt(j).getPlane() )
                {
                    kManager = m_kVOIManagers.elementAt(j);
                    break;
                }
            }
            kManager.pasteAllVOI( kCurrentVOI );  
        }
    }
    
    private void pasteFromViewUserInterface()
    {
        ModelImage kActive = getActiveImage();
        int xDim = kActive.getExtents().length > 0 ? kActive.getExtents()[0] : 1;
        int yDim = kActive.getExtents().length > 1 ? kActive.getExtents()[1] : 1;
        int zDim = kActive.getExtents().length > 2 ? kActive.getExtents()[2] : 1;

        int[] xBounds = new int[2];
        int[] yBounds = new int[2];
        int[] zBounds = new int[2];
        

        // Get the Global copy list:
        Vector<VOIBase> copyList = ViewUserInterface.getReference().getCopyVOIs();
        
        // make new VOI groups:
        VOIVector newVOIs = new VOIVector();
        int iAdded = 0;
        while ( iAdded < copyList.size() )
        {
            VOIBase kCurrentVOI = copyList.elementAt(iAdded++ );
            
            short sID = (short)(kActive.getVOIs().getUniqueID());
            String kName = kCurrentVOI.getClass().getName();
            int index = kName.lastIndexOf('.') + 1;
            kName = kName.substring(index);
            VOI kNewVOI = new VOI(sID, kName + "_" + sID );
            kNewVOI.setCurveType( kCurrentVOI.getType() );
            kNewVOI.setColor( kCurrentVOI.getGroup().getColor() );
                        
            
            VOI kOldGroup = kCurrentVOI.getGroup();
            kNewVOI.getCurves().add( kCurrentVOI );
            for ( int i = iAdded; i < copyList.size(); i++ )
            {
                if ( copyList.elementAt(i).getGroup() == kOldGroup )
                {
                    kNewVOI.getCurves().add( copyList.elementAt(i) );
                }
                else
                {
                    iAdded = i;
                    break;
                }
            }
            if ( kNewVOI.getCurves().size() > 0 )
            {
                newVOIs.add( kNewVOI );
            }
        }
        // Paste new VOI groups:
        
        
        for ( int i = 0; i < newVOIs.size(); i++ )
        {
            VOI currentVOI = newVOIs.get(i);
            currentVOI.getBounds( xBounds, yBounds, zBounds );
            if ( (xBounds[1] < xDim) && (yBounds[1] < yDim) && (zBounds[1] < zDim) )
            {
                kActive.registerVOI(currentVOI);
                currentVOI.addVOIListener(this);
            }
            else
            {
                // Copy the VOI and contents so we can modify it for this image.
                // May want to paste again into another image, so don't modify the original from the copy list.
                currentVOI = new VOI(currentVOI);
                for ( int j = currentVOI.getCurves().size()-1; j >= 0; j-- )
                {
                    VOIBase kCurrentVOI = currentVOI.getCurves().get(j);
                    Vector3f[] kBounds = kCurrentVOI.getImageBoundingBox();
                    if ( (kBounds[1].X - kBounds[0].X < xDim) && 
                         (kBounds[1].Y - kBounds[0].Y < yDim) && 
                         (kBounds[1].Z - kBounds[0].Z < zDim) )
                    {
                        for ( int k = 0; k < kCurrentVOI.size(); k++ )
                        {
                            if ( kBounds[1].X >= xDim )
                            {
                                kCurrentVOI.elementAt(k).X -= (kBounds[1].X - (xDim-1));
                            }
                            if ( kBounds[1].Y >= yDim )
                            {
                                kCurrentVOI.elementAt(k).Y -= (kBounds[1].Y - (yDim-1));
                            }
                            if ( kBounds[1].Z >= zDim )
                            {
                                kCurrentVOI.elementAt(k).Z -= (kBounds[1].Z - (zDim-1));
                            }
                        }
                        kCurrentVOI.update();
                    }
                    else
                    {
                        currentVOI.getCurves().remove(kCurrentVOI);
                    }
                }              
                if ( currentVOI.getCurves().size() > 0 )
                {               
                    kActive.registerVOI(currentVOI);
                    currentVOI.addVOIListener(this);
                }
            }  
        }
        updateDisplay();
    }

    private void quickLUT(Vector3f[] akMinMax, ModelImage image, ModelLUT LUT) {
        if ( !(((akMinMax[1].Z - akMinMax[0].Z) > 5) ||
                ((akMinMax[1].Y - akMinMax[0].Y) > 5) ||
                ((akMinMax[1].X - akMinMax[0].X) > 5) )  )
        {
            return;
        }        
        float min = Float.MAX_VALUE;
        float max = -100000000;
        float val;
        for ( int z = (int)akMinMax[0].Z; z <= akMinMax[1].Z; z++ )
        {
            for ( int y = (int)akMinMax[0].Y; y <= akMinMax[1].Y; y++ )
            {
                for ( int x = (int)akMinMax[0].X; x <= akMinMax[1].X; x++ )
                {
                    val = image.getFloat(x,y,z);
                    if ( val < min )
                    {
                        min = val;
                    }
                    if ( val > max )
                    {
                        max = val;
                    }                        
                }
            }
        }

        float[] x = new float[4];
        float[] y = new float[4];
        new Dimension(256, 256);
        float minImage, maxImage;

        if (image.getType() == ModelStorageBase.UBYTE) {
            minImage = 0;
            maxImage = 255;
        } else if (image.getType() == ModelStorageBase.BYTE) {
            minImage = -128;
            maxImage = 127;
        } else {
            minImage = (float) image.getMin();
            maxImage = (float) image.getMax();
        }

        // Set LUT min max values;
        x[0] = minImage;
        x[1] = min;
        x[2] = max;
        x[3] = maxImage;

        y[0] = 255;
        y[1] = 255;
        y[2] = 0;
        y[3] = 0;

        LUT.getTransferFunction().importArrays(x, y, 4);
    }

    /**
     * DOCUMENT ME!
     * 
     * @param xS DOCUMENT ME!
     * @param wS DOCUMENT ME!
     * @param yS DOCUMENT ME!
     * @param hS DOCUMENT ME!
     * @param imageBuffer DOCUMENT ME!
     * @param image DOCUMENT ME!
     * @param RGB DOCUMENT ME!
     */
    private void quickRGB(final Vector3f[] akMinMax, final ModelImage image, final ModelRGB RGB) {
        if ( !(((akMinMax[1].Z - akMinMax[0].Z) > 5) ||
                ((akMinMax[1].Y - akMinMax[0].Y) > 5) ||
                ((akMinMax[1].X - akMinMax[0].X) > 5) )  )
        {
            return;
        }        
        final float[] minC = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        final float[] maxC = { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        float val;

        for ( int z = (int)akMinMax[0].Z; z <= akMinMax[1].Z; z++ )
        {
            for ( int y = (int)akMinMax[0].Y; y <= akMinMax[1].Y; y++ )
            {
                for ( int x = (int)akMinMax[0].X; x <= akMinMax[1].X; x++ )
                {
                    for (int c = 1; c < 4; c++) {

                        val = image.getFloatC(x,y,z,c);     
                        if ( val < minC[c-1] )
                        {
                            minC[c-1] = val;
                        }
                        if ( val > maxC[c-1] )
                        {
                            maxC[c-1] = val;
                        }                        
                    }
                }
            }
        }
        final float[][] x = new float[3][4];
        final float[][] y = new float[3][4];
        final Dimension dim = new Dimension(256, 256);
        for (int i = 0; i < 3; i++) {
            x[i][1] = minC[i];
            x[i][2] = maxC[i];

            x[i][0] = 0;
            x[i][3] = 255;

            y[i][0] = dim.height - 1;
            y[i][1] = dim.height - 1;
            y[i][2] = 0;
            y[i][3] = 0;
        }
        RGB.getRedFunction().importArrays(x[0], y[0], 4);
        RGB.getGreenFunction().importArrays(x[1], y[1], 4);
        RGB.getBlueFunction().importArrays(x[2], y[2], 4);
        RGB.makeRGB( -1);
    }

    private void redoImage( )
    {
        if ( m_kImageARedo == null )
        {
            return;
        }
        setCursor( MipavUtil.waitCursor );
        try {
            m_kImageA.importData(m_kImageARedo);
            if ( m_kImageB != null && m_kImageBRedo != null)
            {
                m_kImageB.importData(m_kImageBRedo);
            }
        } catch (IOException e) {}
        m_kImageARedo = null;
        m_kImageBRedo = null;
        updateDisplay();
    }

    private void redoVOI()
    {
        if ( m_kRedoCommands.isEmpty() )
        {
            return;
        }        
        String lastCommand = m_kRedoCommands.remove( m_kRedoCommands.size() -1 );
        m_kUndoCommands.add(lastCommand);
        if ( lastCommand.equals( CustomUIBuilder.PARAM_VOI_QUICK_AND_OP.getActionCommand() ) ||
                lastCommand.equals( CustomUIBuilder.PARAM_VOI_QUICK_NOT_OP.getActionCommand() ) )
        {
            if ( m_bGPURenderer )
            {                
                m_kParent.getActiveImage().useMask(true); 
            }
            else
            {
                redoImage();
            }
            m_kParent.updateData(false);
        }
        else
        {
            redoVOIs();
        }
    }


    private void redoVOIs()
    {
        if ( m_kRedoList.isEmpty() )
        {
            return;
        }
        m_kUndoList.add( getVOIState() );
        setVOIState( m_kRedoList.remove( m_kRedoList.size() - 1) );
        if ( imageStatList != null )
        {
            imageStatList.refreshVOIList(getActiveImage().getVOIs());
        }
        if (m_kVOIDialog != null) {
            m_kVOIDialog.updateVOI(m_kCurrentVOIGroup, m_kParent.getActiveImage() );
        }
        updateDisplay();
    }

    /**
     * This method saves all VOIs for the active image to the default VOI directory for that image.
     */
    private void saveAllVOIs() {

        String fileDir;
        String tmpImageName;
        String imageName;
        String voiDir;
        ModelImage img = m_kParent.getActiveImage();
        fileDir = img.getFileInfo(0).getFileDirectory();

        // if the image is a dicom image, then base the new directory name
        // on the actual filename, not the image name
        if (img.isDicomImage()) {
            tmpImageName = img.getFileInfo(0).getFileName();

            final int index = tmpImageName.lastIndexOf(".");

            if (index > 0) {
                tmpImageName = tmpImageName.substring(0, index);
            }

            // now, get rid of any numbers at the end of the name (these
            // are part of the dicom file name, but we only want the 'base'
            // part of the name
            int newIndex = tmpImageName.length();

            for (int i = tmpImageName.length() - 1; i >= 0; i--) {
                final char myChar = tmpImageName.charAt(i);

                if (Character.isDigit(myChar)) {
                    newIndex = i;
                } else {
                    break;
                } // as soon as something is NOT a digit, leave loop
            }

            if (newIndex == 0) {

                // give the base name a generic name
                tmpImageName = new String("DICOM");
            } else {
                tmpImageName = tmpImageName.substring(0, newIndex);
            }
        } else {
            tmpImageName = img.getImageName();
        }

        // get rid of any '^' and ',' which may exist in dicom images
        imageName = tmpImageName.replace('^', '_');
        imageName = imageName.replace(',', '_');

        voiDir = new String(fileDir + File.separator + "defaultVOIs_" + imageName + File.separator);

        saveAllVOIsTo(voiDir);

    } // end saveAllVOIs()

    /**
     * This method saves all VOIs for the active image to a given directory.
     * @param voiDir directory that contains VOIs for this image.
     */
    private void saveAllVOIsTo(final String voiDir) {
        try {

            ModelImage currentImage = m_kParent.getActiveImage();
            ViewVOIVector VOIs = currentImage.getVOIs();

            final File voiFileDir = new File(voiDir);

            if (voiFileDir.exists() && voiFileDir.isDirectory()) { // do nothing
            } else if (voiFileDir.exists() && !voiFileDir.isDirectory()) { // voiFileDir.delete();
            } else { // voiFileDir does not exist
                voiFileDir.mkdir();
            }

            int nVOI = VOIs.size();

            for (int i = 0; i < nVOI; i++) {
                if (VOIs.VOIAt(i).getCurveType() != VOI.ANNOTATION) {
                    FileVOI fileVOI = new FileVOI(VOIs.VOIAt(i).getName() + ".xml", voiDir, currentImage);
                    fileVOI.writeVOI(VOIs.VOIAt(i), true);
                } else {
                    FileVOI fileVOI = new FileVOI(VOIs.VOIAt(i).getName() + ".lbl", voiDir, currentImage);
                    fileVOI.writeAnnotationInVoiAsXML(VOIs.VOIAt(i).getName(),true);
                }
            }

        } catch (final IOException error) {
            MipavUtil.displayError("Error writing all VOIs to " + voiDir + ": " + error);
        }

    } // end saveAllVOIsTo()

    private void saveImage( String kCommand )
    {
        m_kUndoCommands.add( kCommand );

        if ( m_kImageAUndo == null )
        {
            try {
                m_kImageAUndo = m_kImageA.exportData(0, m_kImageA.getSize() );
            } catch (IOException e) {}
        }
        if ( m_kImageB != null )
        {
            if ( m_kImageBUndo == null )
            {
                try {
                    m_kImageBUndo = m_kImageB.exportData(0, m_kImageB.getSize() );
                } catch (IOException e) {}
            }
        }

        m_kRedoCommands.clear();
        m_kRedoList.clear();
    }

    private void saveLabels(final boolean saveAll) {
        String fileName;
        String directory;
        JFileChooser chooser;

        int nVOI;
        int i;
        ViewVOIVector VOIs;
        boolean foundLabel = false;

        ModelImage kImage = m_kParent.getActiveImage();
        VOIs = kImage.getVOIs();
        nVOI = VOIs.size();

        for (i = 0; i < nVOI; i++) {

            if ( (VOIs.VOIAt(i).isActive() || saveAll) && (VOIs.VOIAt(i).getCurveType() == VOI.ANNOTATION)) {
                foundLabel = true;
            }
        }

        if ( !foundLabel) {
            MipavUtil.displayWarning("There are no labels on the image.");

            return;
        }

        chooser = new JFileChooser();
        chooser.setDialogTitle("Save label(s) as");

        if (ViewUserInterface.getReference().getDefaultDirectory() != null) {
            chooser.setCurrentDirectory(new File(ViewUserInterface.getReference().getDefaultDirectory()));
        } else {
            chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        }

        chooser.addChoosableFileFilter(new ViewImageFileFilter(new String[] {".lbl"}));

        final int returnVal = chooser.showSaveDialog(m_kParent.getFrame());

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getName();
            if ( !fileName.endsWith(".lbl")) {
                fileName += ".lbl";
            }
            directory = String.valueOf(chooser.getCurrentDirectory()) + File.separatorChar;
            ViewUserInterface.getReference().setDefaultDirectory(directory);

            this.voiSavedFileName = directory + fileName;

        } else {
            return;
        }

        try {

            final FileVOI fileVOI = new FileVOI(fileName, directory, kImage);

            fileVOI.writeAnnotationXML(saveAll);

        } catch (final IOException error) {
            MipavUtil.displayError("Error writing labels");
        }

    }

    /**
     * This method saves a selected VOI
     * @param saveAllContours if true all contours are saved
     */
    private void saveVOI(final boolean saveAllContours) {

        int nVOI;
        int i;
        ViewVOIVector VOIs;
        FileVOI fileVOI;
        String extension = ".xml";

        try {

            ModelImage kImage = m_kParent.getActiveImage();
            VOIs = kImage.getVOIs();
            nVOI = VOIs.size();

            for (i = 0; i < nVOI; i++) {

                if (VOIs.VOIAt(i).isActive()) {
                    break;
                }
            }

            if (i == nVOI) {
                MipavUtil.displayError("Please select a VOI.");

                return;
            }

            if (VOIs.VOIAt(i).getCurveType() != VOI.ANNOTATION) {
                if (VOIs.VOIAt(i).getExtension().equals(".voi")) {
                    extension = ".voi";
                }
                fileVOI = new FileVOI(VOIs.VOIAt(i).getName() + extension,
                        kImage.getFileInfo(0).getFileDirectory(), kImage);
                fileVOI.writeVOI(VOIs.VOIAt(i), saveAllContours);
            } else {
                fileVOI = new FileVOI(VOIs.VOIAt(i) + ".lbl", kImage.getFileInfo(0).getFileDirectory(), kImage);
                fileVOI.writeAnnotationInVoiAsXML(VOIs.VOIAt(i).getName(), saveAllContours);
            }
        } catch (final IOException error) {
            MipavUtil.displayError("Error writing VOI" + error);
        }
    }


    /**
     * This method allows the user to choose how to save the VOI.
     * @param saveAllContours if true all contours are saved
     */
    private void saveVOIAs(final boolean saveAllContours) {
        String fileName;
        String directory;
        JFileChooser chooser;

        int nVOI;
        int i;
        ViewVOIVector VOIs;
        boolean doPoint = false, doAnnotation = false;


        ModelImage kImage = m_kParent.getActiveImage();
        VOIs = kImage.getVOIs();
        nVOI = VOIs.size();

        for (i = 0; i < nVOI; i++) {

            if (VOIs.VOIAt(i).isActive()) {
                break;
            }
        }

        if (i == nVOI) {
            MipavUtil.displayError("Please select a VOI.");

            return;
        }

        chooser = new JFileChooser();
        chooser.setDialogTitle("Save VOI as");
        if (ViewUserInterface.getReference().getDefaultDirectory() != null) {
            chooser.setCurrentDirectory(new File(ViewUserInterface.getReference().getDefaultDirectory()));
        } else {
            chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        }
        chooser.addChoosableFileFilter(new ViewImageFileFilter(new String[] {".xml"}));

        final int returnVal = chooser.showSaveDialog(m_kParent.getFrame());

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getName();
            if(!fileName.endsWith(".xml")) {
                MipavUtil.displayError("VOI files must end in .xml");
                return;
            }
            directory = String.valueOf(chooser.getCurrentDirectory()) + File.separatorChar;
            ViewUserInterface.getReference().setDefaultDirectory(directory);

            this.voiSavedFileName = directory + fileName;

        } else {
            return;
        }

        try {

            if (fileName.endsWith(".voi") && (VOIs.VOIAt(i).getCurveType() == VOI.POINT)) {
                doPoint = true;
            } else if (fileName.endsWith(".lbl") || VOIs.VOIAt(i).getCurveType() == VOI.ANNOTATION) {
                doAnnotation = true;
                if (VOIs.VOIAt(i).getCurveType() == VOI.ANNOTATION && !fileName.endsWith(".lbl")) {
                    fileName += ".lbl";
                }
            } else if ( !fileName.endsWith(".voi") && !fileName.endsWith(".xml")) {
                fileName += ".xml";
            }

            final FileVOI fileVOI = new FileVOI(fileName, directory, kImage);

            if ( !doPoint && !doAnnotation) {

                // use the MIPAV VOI format (not Nauges) since we
                // need to save the curveType in order to correctly
                // rebuild the VOIs when reading the VOI files.
                fileVOI.writeVOI(VOIs.VOIAt(i), saveAllContours);
            } else if (doPoint) {
                fileVOI.writePointVOI(VOIs.VOIAt(i));
            } else if (doAnnotation) {
                fileVOI.writeAnnotationInVoiAsXML(VOIs.VOIAt(i).getName(), saveAllContours);
            }
        } catch (final IOException error) {
            MipavUtil.displayError("Error writing VOI");
        }

    }

    /**
     * Save intensities in VOI to a text file of format x,y,z,intensity on each line if not color or complex. If color
     * use format x,y,z,a,r,g,b on each line and if complex use format x,y,z,real,imaginary on each line.
     */
    private void saveVOIIntensities() {
        String fileName;
        String directory;
        JFileChooser chooser;
        File textFile;
        RandomAccessFile raFile;
        ModelImage selectedImage = null;
        int imageSize;
        int nDims;
        BitSet mask;
        int xDim;
        int yDim;
        int zDim;
        int sliceSize;

        int nVOI;
        int i, j, k;
        ViewVOIVector VOIs = null;
        int x;
        int y;
        int z;
        double buffer[];
        byte entryBytes[];

        selectedImage = m_kParent.getActiveImage();
        VOIs = selectedImage.getVOIs();
        nVOI = VOIs.size();

        for (i = 0; i < nVOI; i++) {

            if (VOIs.VOIAt(i).isActive()) {
                break;
            }
        }

        if (i == nVOI) {
            MipavUtil.displayError("Please select a VOI.");

            return;
        }

        nDims = selectedImage.getNDims();
        xDim = selectedImage.getExtents()[0];
        yDim = selectedImage.getExtents()[1];
        sliceSize = xDim * yDim;
        imageSize = sliceSize;
        if (nDims > 2) {
            zDim = selectedImage.getExtents()[2];
            imageSize *= zDim;
        } else {
            zDim = 1;
        }
        mask = new BitSet(imageSize);
        VOIs.VOIAt(i).createBinaryMask3D(mask, xDim, yDim, selectedImage.getParentFrame().useXOR(), false);
        if (selectedImage.isColorImage()) {
            buffer = new double[4 * imageSize];
        } else if (selectedImage.isComplexImage()) {
            buffer = new double[2 * imageSize];
        } else {
            buffer = new double[imageSize];
        }

        try {
            selectedImage.exportData(0, buffer.length, buffer);
        } catch (final IOException e) {
            MipavUtil.displayError("IOException on selectedImage.exportData");
            return;
        }

        chooser = new JFileChooser();
        chooser.setDialogTitle("Save intensities in VOI as");

        if (ViewUserInterface.getReference().getDefaultDirectory() != null) {
            chooser.setCurrentDirectory(new File(ViewUserInterface.getReference().getDefaultDirectory()));
        } else {
            chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        }

        chooser.addChoosableFileFilter(new ViewImageFileFilter(new String[] {".txt"}));

        final int returnVal = chooser.showSaveDialog(m_kParent.getFrame());

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getName();
            directory = String.valueOf(chooser.getCurrentDirectory()) + File.separatorChar;
            ViewUserInterface.getReference().setDefaultDirectory(directory);

            this.voiSavedFileName = directory + fileName;

        } else {
            return;
        }

        if ( !fileName.endsWith(".txt")) {
            fileName += ".txt";
        }

        textFile = new File(directory + fileName);
        try {
            raFile = new RandomAccessFile(textFile, "rw");
        } catch (final IOException e) {
            MipavUtil.displayError("IOException on raFile = new RandomAccessFile");
            return;
        }
        // Necessary so that if this is an overwritten file there isn't any
        // junk at the end
        try {
            raFile.setLength(0);
        } catch (final IOException e) {
            MipavUtil.displayError("IOException on raFile.setLength(0)");
            return;
        }

        if (selectedImage.isColorImage()) {
            entryBytes = new String("x,y,z,a,red,green,blue\n").getBytes();
        } else if (selectedImage.isComplexImage()) {
            entryBytes = new String("x,y,z,real,imaginary\n").getBytes();
        } else {
            entryBytes = new String("x,y,z,intensity\n").getBytes();
        }
        try {
            raFile.write(entryBytes);
        } catch (final IOException e) {
            MipavUtil.displayError("IOException on raFile.write(entryBytes) for header line");
            return;
        }

        for (z = 0; z < zDim; z++) {
            k = z * sliceSize;
            for (y = 0; y < yDim; y++) {
                j = k + y * xDim;
                for (x = 0; x < xDim; x++) {
                    i = j + x;
                    if (mask.get(i)) {
                        if (selectedImage.isColorImage()) {
                            entryBytes = new String(Integer.toString(x) + "," + Integer.toString(y) + ","
                                    + Integer.toString(z) + "," + Double.toString(buffer[4 * i]) + ","
                                    + Double.toString(buffer[4 * i + 1]) + "," + Double.toString(buffer[4 * i + 2])
                                    + "," + Double.toString(buffer[4 * i + 3]) + "\n").getBytes();
                        } else if (selectedImage.isComplexImage()) {
                            entryBytes = new String(Integer.toString(x) + "," + Integer.toString(y) + ","
                                    + Integer.toString(z) + "," + Double.toString(buffer[2 * i]) + ","
                                    + Double.toString(buffer[2 * i + 1]) + "\n").getBytes();
                        } else {
                            entryBytes = new String(Integer.toString(x) + "," + Integer.toString(y) + ","
                                    + Integer.toString(z) + "," + Double.toString(buffer[i]) + "\n").getBytes();
                        }
                        try {
                            raFile.write(entryBytes);
                        } catch (final IOException e) {
                            MipavUtil.displayError("IOException on raFile.write(entryBytes");
                            return;
                        }
                    }
                }
            }
        }
        try {
            raFile.close();
        } catch (final IOException e) {
            MipavUtil.displayError("IOException on raFile.close()");
        }

    }




    private void setCurrentColor( )
    {
        setButtonColor(toolbarBuilder.getVOIColorButton(), 
                toolbarBuilder.getVOIColorButton().getBackground() );
    }

    private void setGraphVisible() { 
        int nVOI;
        ViewVOIVector VOIs;
        ViewUserInterface UI = ViewUserInterface.getReference();

        ModelImage kImage = m_kParent.getActiveImage();
        VOIs = kImage.getVOIs();
        nVOI = VOIs.size();

        boolean useFrameRefTime = false;
        FileInfoDicom fileInfo = null;
        String frameRefTimeString = null;
        int frameReferenceTime = 0;

        if ((kImage.getNDims() == 4)
                && (kImage.getFileInfo()[0].getFileFormat() == FileUtility.DICOM)) {
            boolean frameRefTimeFound = false;
            fileInfo = (FileInfoDicom) kImage.getFileInfo(0);
            frameRefTimeString = ((String) fileInfo.getTagTable().getValue(
            "0054,1300")).trim();
            if (frameRefTimeString != null) {
                try {
                    frameReferenceTime = new Integer(frameRefTimeString)
                    .intValue();
                    frameRefTimeFound = true;
                    Preferences.debug("Frame reference time = "
                            + frameReferenceTime + "\n");
                } catch (NumberFormatException e) {
                    Preferences
                    .debug("Number format excepton from frame Reference Time String = "
                            + frameRefTimeString + "\n");
                }

                if (frameRefTimeFound) {
                    int response = JOptionPane.showConfirmDialog(
                            UI.getMainFrame(),
                            new String(
                            "Do you wish to use the frame reference time for the graph x axis?"),
                            "Frame Reference Time?",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (response == JOptionPane.YES_OPTION) {
                        useFrameRefTime = true;
                    }
                } // if (frameRefTimeFound)
            } // if (frameRefTimeString != null)
        } // if if ((compImage.getActiveImage().getNDims() == 4)

        for (int i = 0; i < nVOI; i++) {

            if (VOIs.VOIAt(i).isActive() && (VOIs.VOIAt(i).isVisible() == true)) {

                if ((VOIs.VOIAt(i).getCurveType() == VOI.POINT)
                        && (VOIs.VOIAt(i).getContourGraph() != null)) {
                    if (kImage.getNDims() == 4) {
                        VOIs.VOIAt(i).getContourGraph().setUnitsInLabel(
                                FileInfoBase
                                .getUnitsOfMeasureAbbrevStr(kImage
                                        .getFileInfo(0)
                                        .getUnitsOfMeasure(3)));
                    } else {
                        VOIs.VOIAt(i).getContourGraph().setUnitsInLabel(
                                FileInfoBase
                                .getUnitsOfMeasureAbbrevStr(kImage
                                        .getFileInfo(0)
                                        .getUnitsOfMeasure(0)));
                    }

                    if (useFrameRefTime
                            || (kImage.isColorImage() == true)) {

                        for (int j = 0; j < VOIs.VOIAt(i).getCurves().size(); j++) {
                            if (((VOIPoint) (VOIs.VOIAt(i).getCurves().elementAt(j))).isActive()) {
                                graphPointVOI(VOIs.VOIAt(i), (VOIPoint) (VOIs
                                        .VOIAt(i).getCurves().elementAt(j)), j,
                                        useFrameRefTime);
                            }
                        }
                    }

                    VOIs.VOIAt(i).getContourGraph().setVisible(true);
                } else if ((VOIs.VOIAt(i).getCurveType() == VOI.POINT)
                        && (VOIs.VOIAt(i).getContourGraph() == null)) {

                    for (int j = 0; j < VOIs.VOIAt(i).getCurves().size(); j++) {
                        graphPointVOI(VOIs.VOIAt(i),
                                (VOIPoint) (VOIs.VOIAt(i).getCurves().elementAt(j)), j,
                                useFrameRefTime);
                    }

                    VOIs.VOIAt(i).getContourGraph().setVisible(true);
                }
            }
        }

    }


    private void setPAAIGraphVisible() {   
        int nVOI;
        ViewVOIVector VOIs;

        VOIs = m_kParent.getActiveImage().getVOIs();
        nVOI = VOIs.size();

        Vector3f pt;

        for (int i = 0; i < nVOI; i++) {

            if (VOIs.VOIAt(i).getCurveType() == VOI.POINT) {

                // get the x,y coords for each active VOIPoint and open up a
                // JDialogPointArea
                for (int j = 0; j < VOIs.VOIAt(i).getCurves().size(); j++) {

                    if (((VOIPoint) (VOIs.VOIAt(i).getCurves().elementAt(j))).isActive()) {
                        pt = ((VOIPoint) (VOIs.VOIAt(i).getCurves().elementAt(j))).exportPoint();
                        new JDialogPointArea(m_kParent.getFrame(),
                                m_kParent.getActiveImage(), (int) pt.X, (int) pt.Y, true);
                    }
                }
            }
        }

    }

    private void setVOIState( VOISaveState kVOIState )
    {
        m_kImageA.unregisterAllVOIs();
        m_kImageA.setVOIs( kVOIState.voiVectorA );
        if ( m_kImageB != null )
        {
            m_kImageB.unregisterAllVOIs();
            m_kImageB.setVOIs( kVOIState.voiVectorB );
        }
        if ( kVOIState.currentVOI != -1 )
        {
            m_kCurrentVOIGroup = m_kImageA.getVOIs().get(kVOIState.currentVOI);
            if (presetHue >= 0.0) {
            	m_kCurrentVOIGroup.setColor(presetHue);
            }
            setSelectedVOI( m_kCurrentVOIGroup, m_kCurrentVOIGroup.isAllActive(), true );
        }
        else
        {
            m_kCurrentVOIGroup = null;
        }        
        m_kParent.setCenter( new Vector3f( kVOIState.currentCenter ) );
    }

    /**
     * Opens a JDialogStatistics to allow computation ofROI statistics.
     */
    private void showStatisticsCalculator() {

        if (imageStatList == null) {

            if ( (getActiveImage().getVOIs() != null) && (getActiveImage().getVOIs().size() != 0)) {
                imageStatList = new JDialogVOIStatistics(getActiveImage().getVOIs());
                imageStatList.setVisible(true);
                // addVOIUpdateListener(imageStatList); // i'd rather not do it this way...
            } else {
                MipavUtil.displayError("A VOI must be present to use the statistics calculator");
            }
        } else {
            imageStatList.refreshVOIList(getActiveImage().getVOIs());
            imageStatList.setVisible(true);
        }
    }

    private void showVOIProperties() {

        if (m_kVOIDialog == null) {
            m_kVOIDialog = new JDialogVOIStats( this, m_kParent.getActiveImage(), m_kCurrentVOIGroup );
            addVOIUpdateListener(m_kVOIDialog);
        }

        if (m_kVOIDialog != null) {
            m_kVOIDialog.setVisible(true);
            m_kVOIDialog.updateVOI(m_kCurrentVOIGroup, m_kParent.getActiveImage() );
        }
    }



    private void undoImage( )
    {
        if ( m_kImageAUndo == null )
        {
            return;
        }
        setCursor( MipavUtil.waitCursor );
        try {
            m_kImageARedo = m_kImageA.exportData(0, m_kImageA.getSize() );
            m_kImageA.importData(m_kImageAUndo);
            if ( m_kImageB != null && m_kImageBUndo != null )
            {
                m_kImageBRedo = m_kImageB.exportData(0, m_kImageB.getSize() );
                m_kImageB.importData(m_kImageBUndo);
            }
        } catch (IOException e) {}
        updateDisplay();
    }

    private void undoVOI()
    {
        if ( m_kUndoCommands.isEmpty() )
        {
            return;
        }        
        String lastCommand = m_kUndoCommands.remove( m_kUndoCommands.size() -1 );
        m_kRedoCommands.add(lastCommand);
        if ( lastCommand.equals( CustomUIBuilder.PARAM_VOI_QUICK_AND_OP.getActionCommand() ) ||
                lastCommand.equals( CustomUIBuilder.PARAM_VOI_QUICK_NOT_OP.getActionCommand() ) )
        {
            if ( m_bGPURenderer )
            {
                m_kParent.getActiveImage().useMask(false);
            }
            else
            {
                undoImage();
            }
            m_kParent.updateData(false);
        }
        else
        {
            undoVOIs();
        }
    }
    private void undoVOIs()
    {
        if ( m_kUndoList.isEmpty() )
        {
            return;
        }
        m_kRedoList.add( getVOIState() );
        setVOIState( m_kUndoList.remove( m_kUndoList.size() - 1) );
        if ( imageStatList != null )
        {
            imageStatList.refreshVOIList(getActiveImage().getVOIs());
        }
        if (m_kVOIDialog != null) {
            m_kVOIDialog.updateVOI(m_kCurrentVOIGroup, m_kParent.getActiveImage() );
        }
        updateDisplay();
    }

    /*
     * 
    public LocalVolumeVOI getCurrentVOI()
    {
        if ( m_kVOIManager != null )
        {
            return m_kVOIManager.getCurrentVOI();
        }
        return null;
    }

    public void setCurrentVOI( LocalVolumeVOI kCurrentVOI )
    {
        if ( m_kVOIManager != null )
        {
            m_kVOIManager.setCurrentVOI( kCurrentVOI );
        }
        doVOI("");
    }

    public LocalVolumeVOIVector[] getVOICopy()
    {
        if ( m_kVOIManager != null )
        {
            return m_kVOIManager.getVOICopy();
        }
        return null;
    }

    public void setVOICopy(LocalVolumeVOIVector[] kList)
    {       
        if ( m_kVOIManager != null )
        {
            m_kVOIManager.setVOICopy(kList);
        }
        doVOI("");
    }
     */
    
    /**
     * This method merges the 3 axial, sagittal, coronal VOIs and save them into one cloudy points file. This function
     * is used by the prostate surface reconstruction analysis.
     */
    private void saveMergedVOIs() {
        final JDialogBase mergeVOIsDialog = new JDialogSaveMergedVOIs(m_kParent.getFrame());
        mergeVOIsDialog.validate();
    }

    public void saveProstateFeatures() {
        final JDialogBase saveFeaturesDialog = new JDialogProstateSaveFeatures(m_kParent.getFrame(), getActiveImage(), false);
        saveFeaturesDialog.validate();
    }

    public void testProstateFeatures() {
        final JDialogBase saveFeaturesDialog = new JDialogProstateSaveFeatures(m_kParent.getFrame(), getActiveImage(), true);
        saveFeaturesDialog.validate();
    }

    
    private void testProstateFeaturesTrain() {
    	JDialogBase trainFeaturesDialog = new JDialogProstateFeaturesTrain(m_kParent.getFrame());
    	trainFeaturesDialog.validate();
    }
    
    private void testProstateFeaturesClassification() {
    	JDialogBase classificationFeaturesDialog = new JDialogProstateFeaturesClassification(m_kParent.getFrame());
    	classificationFeaturesDialog.validate();
    }
    
    private void loadProstateMask() {
        final JDialogBase loadProstateMaskDialog = new JDialogLoadProstateMask(m_kParent.getFrame(), getActiveImage());
        loadProstateMaskDialog.validate();
    }

    private void prostateSegAuto() {
    	// final JDialogProstateSegmentationAuto prostateSegAutoDialog = new JDialogProstateSegmentationAuto(m_kParent.getFrame(), getActiveImage());
    	// prostateSegAutoDialog.validate();
    }
    
    /**
     * Reconstruct the prostate surface from the coarse VOIs cloudy points.
     */
    private void reconstructSurfaceFromVOIs() {
        final JDialogBase reconstructSurfaceDialog = new JDialogSurfaceReconstruction(m_kParent.getFrame());
        reconstructSurfaceDialog.validate();
    }

    /*
     * public void extractSurfaceFromVOIs() { JDialogBase extractSurfaceDialog = new JDialogExtractSurfaceVOIs(this);
     * extractSurfaceDialog.validate(); }
     */

    /**
     * Save the dicom matrix header info. The .ply file format can't save the dicom info. We decide to save the dicom
     * info when save the VOI file. So, the dicom info will be read when load the .ply surface into the volume render.
     * This ensures the correct scale of surface. The dicom matrix info is saved in the current image directory with the
     * .dicomMatrix suffix.
     */
    private void saveDicomMatrixInfo() {
        int iXDim, iYDim, iZDim;
        final boolean flip = true;

        iXDim = getActiveImage().getExtents()[0];
        iYDim = getActiveImage().getExtents()[1];
        iZDim = getActiveImage().getExtents()[2];

        float fXRes, fYRes, fZRes;

        fXRes = getActiveImage().getFileInfo()[0].getResolutions()[0];
        fYRes = getActiveImage().getFileInfo()[0].getResolutions()[1];
        fZRes = getActiveImage().getFileInfo()[0].getResolutions()[2];

        final float[] box = new float[3];

        box[0] = (iXDim - 1) * fXRes;
        box[1] = (iYDim - 1) * fYRes;
        box[2] = (iZDim - 1) * fZRes;

        final int[] direction = MipavCoordinateSystems.getModelDirections(getActiveImage());
        final float[] startLocation = getActiveImage().getFileInfo(0).getOrigin();

        TransMatrix dicomMatrix = null;
        TransMatrix inverseDicomMatrix = null;
        if (getActiveImage().getMatrixHolder().containsType(TransMatrix.TRANSFORM_SCANNER_ANATOMICAL)) {

            // Get the DICOM transform that describes the transformation
            // from
            // axial to this image orientation
            dicomMatrix = getActiveImage().getMatrix();
            inverseDicomMatrix = new TransMatrix(getActiveImage().getMatrix());
            inverseDicomMatrix.Inverse();
        }

        final String fileDir = getActiveImage().getImageDirectory();
        final String fName = getActiveImage().getImageFileName();
        final int index = fName.indexOf('.');
        final String fileName = fName.substring(0, index);

        System.err.println("Dicom Matrix File Dir = " + fileDir);
        System.err.println("Dicom Matrix File Name = " + (fileName + ".dicomMatrix"));

        try {
            final FileOutputStream fOut = new FileOutputStream(fileDir + fileName + ".dicomMatrix");
            final DataOutputStream kOut = new DataOutputStream(fOut);

            if (inverseDicomMatrix == null) {

                if (flip) {
                    kOut.writeInt(1);
                } else {
                    kOut.writeInt(0);
                }
            } else {

                if (flip) {
                    kOut.writeInt(3);
                } else {
                    kOut.writeInt(2);
                }
            }

            kOut.writeInt(direction[0]);
            kOut.writeInt(direction[1]);
            kOut.writeInt(direction[2]);

            kOut.writeFloat(startLocation[0]);
            kOut.writeFloat(startLocation[1]);
            kOut.writeFloat(startLocation[2]);
            kOut.writeFloat(box[0]);
            kOut.writeFloat(box[1]);
            kOut.writeFloat(box[2]);

            if (inverseDicomMatrix != null) {
                for (int i = 0; i <= 3; i++) {
                    for (int j = 0; j <= 3; j++) {
                        kOut.writeDouble(inverseDicomMatrix.Get(i, j));
                    }
                }
            }
            kOut.close();

        } catch (final Exception e) {
            System.err.println("CAUGHT EXCEPTION WITHIN saveDicomMatrixInfo() of FileVOI");
            e.printStackTrace();
        }

    }

    @Override
    public void addedCurve(VOIEvent added) {
        added.getBase().getGeometricCenter();
        if ( m_kVOIDialog != null )
        {
            m_kVOIDialog.updateVOI( added.getVOI(), getActiveImage() );
            m_kVOIDialog.updateTree();
        }
    }

    @Override
    public void colorChanged(Color c) { }

    @Override
    public void removedCurve(VOIEvent removed) { 
        //System.err.println( "removedCurve " + this );
        if ( m_kVOIDialog != null )
        {
            ModelImage kActive = getActiveImage();
            ViewVOIVector VOIs = kActive.getVOIs();
            for (int i = 0; i < VOIs.size(); i++) {
                if (VOIs.VOIAt(i).isActive()) {
                    m_kVOIDialog.updateVOI( VOIs.VOIAt(i), kActive );
                    m_kVOIDialog.updateTree();
                    return;
                }
            }
            if (VOIs.size() > 0) {
                m_kVOIDialog.updateVOI( VOIs.VOIAt(0), kActive );
                m_kVOIDialog.updateTree();
            }
        }
     }

    @Override
    public void selectedVOI(VOIEvent selection) {
        //System.err.println( "selectedVOI " + this );
        if ( m_kVOIDialog != null )
        {
            m_kVOIDialog.updateVOI( selection.getVOI(), getActiveImage() );
            m_kVOIDialog.updateTree();
        }
    }

    @Override
    public void addedVOI(VOIVectorEvent newVOIselection) {
        //System.err.println( "addedVOI " + this );
        newVOIselection.getVOI().addVOIListener(this);
        if ( m_kVOIDialog != null )
        {
            m_kVOIDialog.updateVOI( newVOIselection.getVOI(), getActiveImage() );
            m_kVOIDialog.updateTree();
        }
        
    }

    @Override
    public void removedVOI(VOIVectorEvent removed) {
        //System.err.println( "removedVOI " + this );
        if ( m_kVOIDialog != null )
        {
            ModelImage kActive = getActiveImage();
            ViewVOIVector VOIs = kActive.getVOIs();
            for (int i = 0; i < VOIs.size(); i++) {
                if (VOIs.VOIAt(i).isActive()) {
                    m_kVOIDialog.updateVOI( VOIs.VOIAt(i), kActive );
                    m_kVOIDialog.updateTree();
                    return;
                }
            }
            if (VOIs.size() > 0) {
                m_kVOIDialog.updateVOI( VOIs.VOIAt(0), kActive );
                m_kVOIDialog.updateTree();
            }
        }
    }

    @Override
    public void vectorSelected(VOIVectorEvent selection) {
        //System.err.println( "vectorSelected " + this );
        if ( m_kVOIDialog != null )
        {
            m_kVOIDialog.updateVOI( selection.getVOI(), getActiveImage() );
            m_kVOIDialog.updateTree();
        }        
    }

}
