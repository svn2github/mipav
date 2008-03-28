package gov.nih.mipav.view.renderer.WildMagic;


import gov.nih.mipav.model.file.*;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;
import gov.nih.mipav.view.renderer.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import java.io.*;
import java.util.*;
import javax.swing.*;


/**
 * JPanelSurfaceTexture. Enables texture-mapping of the ModelImage data onto a surface triangle mesh. The Texture
 * coordinates of the mesh are calculated in the SurfaceMask class. This class creates the ImageComponent3D object that
 * is passed to the Texture3D object when the surface display attributes (TextureUnitState) are created. The
 * ImageComponent3D object contained within this class updates when the user changes the ModelLUT associated with the
 * texture.
 *
 * <p>The user can change the ModelLUT independently of the ModelLUT associated with the ModelImage, or if the user
 * selects the option of using the ModelLUT associated with the ModelImage, then the texture updates as the user updates
 * that LUT. This class implements the ViewImageUpdateInterface to capture LUT changes.</p>
 *
 * @see  JPanelSurface.java
 * @see  SurfaceMask.java
 * @see  ModelImage.java
 */
public class JPanelSurfaceTexture_WM extends JPanelRendererBase implements ViewImageUpdateInterface {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = 7562070328632922435L;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** Display the independent LUT for Black/White images. */
    private JPanelHistoLUT mHistoLUT;

    /** Display the independent RGB for Color Images. */
    private JPanelHistoRGB mHistoRGB;

    /** ModelImage used to generate the 3D texture:. */
    private ModelImage mImageA;

    /** Reference to ModelImage A for linking the texture to the imageA LUT. */
    private ModelImage mImageALink;

    /** RadioButton for turing on the surface image texture:. */
    private JCheckBox mImageAsTextureCheck;

    /** Stores the currently-loaded ModelImage directory name:. */
    private String mImageDirName;

    /** Stores the currently-loaded ModelImage file name:. */
    private String mImageFileName;

    /** Display the currently-loaded ModelImage file name:. */
    private JLabel mImageFileNameLabel;

    // Interface buttons:
    /** Load a new ModelImage:. */
    private JButton mLoadImageButton;

    /** Use the ModelImage LUT. */
    private JRadioButton mModelImageRadioButton;

    /** Use a separate LUT. */
    private JRadioButton mNewImageRadioButton;

    /** Grouping the radio buttons:. */
    private ButtonGroup mImageButtonGroup = new ButtonGroup();
    
    /** Independent ModelImage for independent LUT. */
    private ModelImage mLUTImageA;

    /** The LUT associated with the ModelImage imageA:. */
    private ModelLUT mLUTModel = null;

    /** The LUT associated with the independent texture LUT:. */
    private ModelLUT mLUTSeparate = null;

    /** Use the ModelImage LUT. */
    private JRadioButton mModelLUTRadioButton;

    /** Use a separate LUT. */
    private JRadioButton mNewLUTRadioButton;

    /** Grouping the radio buttons:. */
    private ButtonGroup mLUTButtonGroup = new ButtonGroup();
    
    /** The RGB LUT associated with the ModelImage imageA:. */
    private ModelRGB mRGBModel = null;

    /** The RGB LUT associated with the independent texture LUT:. */
    private ModelRGB mRGBSeparate = null;


    private JPanelSurface_WM m_kSurfacePanel = null;
    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Constructor:
     *
     */
    public JPanelSurfaceTexture_WM( VolumeViewer kViewer ) {
        super(kViewer);
        mImageA = kViewer.getImageA();
        mImageALink = mImageA;
        mImageALink.addImageDisplayListener(this);
        init();
    }

    //~ Methods --------------------------------------------------------------------------------------------------------
    public void setSurfacePanel( JPanelSurface_WM kSurfacePanel )
    {
        m_kSurfacePanel = kSurfacePanel;
        m_kSurfacePanel.SetLUTNew( mLUTSeparate, mRGBSeparate );
    }

    /**
     * actionPerformed, listens for interface events.
     *
     * @param  event  ActionEvent generated by the interface.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        if (command.equals("LoadNewImage")) {
            JFileChooser chooser = new JFileChooser();
            chooser.setMultiSelectionEnabled(false);
            chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.TECH));

            if (ViewUserInterface.getReference().getDefaultDirectory() != null) {
                chooser.setCurrentDirectory(new File(ViewUserInterface.getReference().getDefaultDirectory()));
            } else {
                chooser.setCurrentDirectory(new File(System.getProperties().getProperty("user.dir")));
            }

            if (JFileChooser.APPROVE_OPTION != chooser.showOpenDialog(null)) {
                return;
            }

            mImageFileName = chooser.getSelectedFile().getName();
            mImageDirName = String.valueOf(chooser.getCurrentDirectory()) + File.separatorChar;
            mImageFileNameLabel.setText(mImageFileName);
            chooser.setVisible(false);

            loadingImage();

        } else if (command.equals("OriginalModelImage")) {
            if ( m_kSurfacePanel != null )
            {
                m_kSurfacePanel.ImageAsTexture(mImageAsTextureCheck.isSelected(),
                        mNewImageRadioButton.isSelected(),
                        mNewLUTRadioButton.isSelected() );
            }
        } else if (command.equals("LoadedModelImage")) {
            if ( m_kSurfacePanel != null )
            {
                m_kSurfacePanel.ImageAsTexture(mImageAsTextureCheck.isSelected(),
                        mNewImageRadioButton.isSelected(),
                        mNewLUTRadioButton.isSelected() );
            }
        } else if (command.equals("ImageAsTexture")) {
            if ( m_kSurfacePanel != null )
            {
                m_kSurfacePanel.ImageAsTexture(mImageAsTextureCheck.isSelected(),
                        mNewImageRadioButton.isSelected(),
                        mNewLUTRadioButton.isSelected() );
            }
        } else if (command.equals("LinkLUTs")) {

            if (!mImageA.isColorImage()) {
                mainPanel.remove(mHistoLUT.getMainPanel());
            } else {
                mainPanel.remove(mHistoRGB.getMainPanel());
            }

            mainPanel.updateUI();
            //updateImages(null, mLUTModel, false, 0);
            if ( m_kSurfacePanel != null )
            {
                m_kSurfacePanel.ImageAsTexture(mImageAsTextureCheck.isSelected(),
                        mNewImageRadioButton.isSelected(),
                        mNewLUTRadioButton.isSelected() );
            }
        } else if (command.equals("SeparateLUTs")) {

            if (!mImageA.isColorImage()) {
                mainPanel.add(mHistoLUT.getMainPanel(), BorderLayout.SOUTH);
            } else {
                mainPanel.add(mHistoRGB.getMainPanel(), BorderLayout.SOUTH);
            }

            mainPanel.updateUI();
            //updateImages(mLUTSeparate, null, false, 0);
            if ( m_kSurfacePanel != null )
            {
                m_kSurfacePanel.ImageAsTexture(mImageAsTextureCheck.isSelected(),
                        mNewImageRadioButton.isSelected(),
                        mNewLUTRadioButton.isSelected() );
            }
        }
    }

    /**
     * Removes this object from the ModelImage imageDisplayListener list.
     */
    public void dispose() {
        mImageALink.removeImageDisplayListener(this);

        if (mLUTImageA != null) {
            mLUTImageA.disposeLocal();
            mLUTImageA = null;
        }
    }

    /**
     * Returns whether the ModelTriangleMesh is to be displayed with the ModelImage data as a texture or not.
     *
     * @return  true when the ModelImageMesh is texture-mapped, false otherwise.
     */
    public boolean getEnabled() {
        return false;//(mTextureStatus == TEXTURE);
    }

    /**
     * Returns The SurfaceRender ModelImage imageA for linking to the LUT.
     *
     * @return  mImageALink, for identifying the ModelLUT associated with mImageA.
     */
    public ModelImage getImageLink() {
        return mImageALink;
    }

    /**
     * Returns the ModelImage associated with the independent LUT.
     *
     * @return  the ModelImage associated with the independent LUT
     */
    public ModelImage getImageSeparate() {
        return mLUTImageA;
    }

    /**
     * Return the current ModelLUT:.
     *
     * @return  the currently used ModelLUT
     */
    public ModelLUT getLUT() {

        if (mModelLUTRadioButton.isSelected()) {
            return mLUTModel;
        }

        return mLUTSeparate;
    }


    /**
     * Returns the mainPanel.
     *
     * @return  mainPanel;
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Return the current ModelRGBT:.
     *
     * @return  the currently used ModelRGBT
     */
    public ModelRGB getRGBT() {

        if (mModelLUTRadioButton.isSelected()) {
            return mRGBModel;
        }

        return mRGBSeparate;
    }

    /**
     * Returns The ModelImage that is the data source for the Texture3D.
     *
     * @return  mImageA, the ModelImage used to generate the Texture3D
     */
    public ModelImage getTextureImage() {
        return mImageA;
    }


    /**
     * Enables or disables the interface. Called when a surface is added/removed from the JPanelSurface class.
     *
     * @param  flag  when true enable the interface, when false disable the interface.
     */
    public void setEnabled(boolean flag) {
        mImageAsTextureCheck.setEnabled(flag);
        mModelLUTRadioButton.setEnabled(flag);
        mNewLUTRadioButton.setEnabled(flag);
    }

    /**
     * Update the ModelRGB associated with the separate texture, and regenerate the ImageComponente3D volume texture.
     *
     * @param  RGBTa  the new ModelRGB for the separate texture.
     */
    public void setRGBTA(ModelRGB RGBTa) {

        if (RGBTa != null) {
            mRGBSeparate = RGBTa;
            if ( m_kSurfacePanel != null )
            {
                m_kSurfacePanel.SetLUTNew( mLUTSeparate, mRGBSeparate );
            }
        }
    }

    /**
     * Update the ModelRGB associated with the separate texture, and regenerate the ImageComponente3D volume texture.
     *
     * @param  RGBTa  the new ModelRGB for the separate texture.
     */
    public void setRGBTB(ModelRGB RGBTb) {}

    /**
     * Initializes the interface, and generates the first default texture.
     */
    private void init() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;

        JPanel imagePanel = new JPanel();
        imagePanel.setBorder(buildTitledBorder("Image source options"));
        imagePanel.setLayout(new GridBagLayout());
        imagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        gbc.gridx = 0;
        gbc.gridy = 0;

        /* Button: Load Surface */
        mLoadImageButton = new JButton();
        mLoadImageButton.setText("Select Image ...");
        mLoadImageButton.setActionCommand("LoadNewImage");
        mLoadImageButton.addActionListener(this);
        imagePanel.add(mLoadImageButton, gbc);      
        gbc.gridx++;

        /* Label: name of loaded surface file */
        mImageFileNameLabel = new JLabel();
        mImageFileNameLabel.setPreferredSize(new Dimension(130, 21));
        mImageFileNameLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        mImageFileName = mImageA.getImageName();
        mImageFileNameLabel.setText(mImageFileName);
        imagePanel.add(mImageFileNameLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        mModelImageRadioButton = new JRadioButton("Use Original ModelImage");
        mModelImageRadioButton.addActionListener(this);
        mModelImageRadioButton.setActionCommand("OriginalModelImage");
        mModelImageRadioButton.setSelected(true);
        mModelImageRadioButton.setEnabled(false);
        mImageButtonGroup.add(mModelImageRadioButton);
        imagePanel.add(mModelImageRadioButton, gbc);

        gbc.gridx++;
        mNewImageRadioButton = new JRadioButton("Use Loaded ModelImage");
        mNewImageRadioButton.addActionListener(this);
        mNewImageRadioButton.setActionCommand("LoadedModelImage");
        mNewImageRadioButton.setSelected(false);
        mNewImageRadioButton.setEnabled(false);
        mImageButtonGroup.add(mNewImageRadioButton);
        imagePanel.add(mNewImageRadioButton, gbc);


        JPanel texturePanel = new JPanel();
        texturePanel.setBorder(buildTitledBorder("Image display options"));
        texturePanel.setLayout(new GridBagLayout());
        texturePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        gbc.gridx = 0;
        gbc.gridy = 0;

        mImageAsTextureCheck = new JCheckBox("3D Texture");
        mImageAsTextureCheck.addActionListener(this);
        mImageAsTextureCheck.setActionCommand("ImageAsTexture");
        mImageAsTextureCheck.setSelected(false);
        mImageAsTextureCheck.setEnabled(false);
        texturePanel.add(mImageAsTextureCheck, gbc);

        JPanel lutPanel = new JPanel();
        lutPanel.setBorder(buildTitledBorder("LUT options"));
        lutPanel.setLayout(new GridBagLayout());
        lutPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        gbc.gridx = 0;
        gbc.gridy = 0;

        mModelLUTRadioButton = new JRadioButton("Use ModelImage LUT");
        mModelLUTRadioButton.addActionListener(this);
        mModelLUTRadioButton.setActionCommand("LinkLUTs");
        mModelLUTRadioButton.setSelected(false);
        mModelLUTRadioButton.setEnabled(false);
        mLUTButtonGroup.add(mModelLUTRadioButton);
        lutPanel.add(mModelLUTRadioButton, gbc);

        gbc.gridx++;
        mNewLUTRadioButton = new JRadioButton("Use Separate LUT");
        mNewLUTRadioButton.addActionListener(this);
        mNewLUTRadioButton.setActionCommand("SeparateLUTs");
        mNewLUTRadioButton.setSelected(true);
        mNewLUTRadioButton.setEnabled(false);
        mLUTButtonGroup.add(mNewLUTRadioButton);
        lutPanel.add(mNewLUTRadioButton, gbc);


        JPanel controlsPanel = new JPanel(new BorderLayout());
        controlsPanel.add(imagePanel, BorderLayout.NORTH);
        controlsPanel.add(texturePanel, BorderLayout.CENTER);
        controlsPanel.add(lutPanel, BorderLayout.SOUTH);

        mainPanel = new JPanel(new BorderLayout());

        // mainPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        mainPanel.add(controlsPanel, BorderLayout.NORTH);
        initLUT();
    }


    /**
     * Initializes or re-initializes the Histogram LUT interface based on the currently-loaded ModelImage.
     */
    private void initLUT() {

        /* Create LUT interface: */
        mImageA.calcMinMax();

        if (!mImageA.isColorImage()) {
            float fMin = (float) mImageA.getMin();
            float fMax = (float) mImageA.getMax();

            int[] iExtents = { 256, 256 };
            mLUTImageA = new ModelImage(ModelStorageBase.FLOAT, iExtents, "temp");
            mLUTImageA.addImageDisplayListener(this);

            for (int i = 0; i < 256; i++) {

                for (int j = 0; j < 256; j++) {
                    mLUTImageA.set(i, j, fMin + ((float) i / 255.0f * (fMax - fMin)));
                }
            }

            mLUTImageA.calcMinMax();

            /* Create LUT */
            int[] dimExtentsLUT = { 4, 256 };
            mLUTSeparate = new ModelLUT(ModelLUT.GRAY, 256, dimExtentsLUT);
            mLUTSeparate.resetTransferLine(fMin, fMin, fMax, fMax);

            /* Remove old LUT if it exists: */
            if (mHistoLUT != null) {
                mainPanel.remove(mHistoLUT.getMainPanel());
                mHistoLUT = null;
            }

            /* Create LUT panel: */
            mHistoLUT = new JPanelHistoLUT(mLUTImageA, null, mLUTSeparate, null, true);
        } else {
            float fMinR = (float) mImageA.getMinR();
            float fMaxR = (float) mImageA.getMaxR();

            float fMinG = (float) mImageA.getMinG();
            float fMaxG = (float) mImageA.getMaxG();

            float fMinB = (float) mImageA.getMinB();
            float fMaxB = (float) mImageA.getMaxB();

            int[] iExtents = { 256, 256 };
            mLUTImageA = new ModelImage(ModelStorageBase.ARGB_FLOAT, iExtents, "temp");
            mLUTImageA.addImageDisplayListener(this);

            for (int j = 0; j < 256; j++) {

                for (int i = 0; i < 256; i++) {
                    mLUTImageA.setC(i, j, 0, 255.0f);
                    mLUTImageA.setC(i, j, 1, fMinR + ((float) j / 255.0f * (fMaxR - fMinR)));
                    mLUTImageA.setC(i, j, 2, fMinG + ((float) j / 255.0f * (fMaxG - fMinG)));
                    mLUTImageA.setC(i, j, 3, fMinB + ((float) j / 255.0f * (fMaxB - fMinB)));
                }
            }

            mLUTImageA.calcMinMax();

            /* Create LUT */
            int[] dimExtentsLUT = { 4, 256 };
            mRGBSeparate = new ModelRGB(dimExtentsLUT);

            /* Remove old lut if it exists: */
            if (mHistoRGB != null) {
                mainPanel.remove(mHistoRGB.getMainPanel());
                mHistoRGB = null;
            }

            /* Create LUT panel: */
            mHistoRGB = new JPanelHistoRGB(mLUTImageA, null, mRGBSeparate, null, true);
        }

        if (!mImageA.isColorImage()) {
            mainPanel.add(mHistoLUT.getMainPanel(), BorderLayout.SOUTH);
        } else {
            mainPanel.add(mHistoRGB.getMainPanel(), BorderLayout.SOUTH);
        }

        mainPanel.updateUI();
        if ( m_kSurfacePanel != null )
        {
            m_kSurfacePanel.SetLUTNew( mLUTSeparate, mRGBSeparate );
        }
    }

    /**
     * Load a new ModelImage to use for the 3D Texture display:
     */
    private void loadingImage() {

        /* If the ModelImage m_kImage is not defined (null) or the name
         * doesn't equal the name set in m_kImageFile, then load the new
         * ModelImage: */
        if ((mImageA == null) || ((mImageA != null) && (!mImageFileName.equals(mImageA.getImageName())))) {
            FileIO fileIO = new FileIO();
            fileIO.setQuiet(true);

            mImageA = fileIO.readImage(mImageFileName, mImageDirName, false, null);
            mImageFileName = mImageA.getImageName();
            mImageFileNameLabel.setText(mImageFileName);
            initLUT();
            mainPanel.updateUI();
            
            mModelImageRadioButton.setEnabled(true);
            mNewImageRadioButton.setEnabled(true);
            if ( m_kSurfacePanel != null )
            {
                m_kSurfacePanel.SetImageNew( mImageA );
            }
        }
    }

    public void setSlice(int slice) {
        // TODO Auto-generated method stub
        
    }

    public void setTimeSlice(int tSlice) {
        // TODO Auto-generated method stub
        
    }

    public boolean updateImageExtents() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean updateImages() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean updateImages(boolean flag) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean updateImages(ModelLUT LUTa, ModelLUT LUTb, boolean flag, int interpMode) {
        if ( m_kSurfacePanel != null )
        {
            m_kSurfacePanel.ImageAsTexture(mImageAsTextureCheck.isSelected(),
                    mNewImageRadioButton.isSelected(),
                    mNewLUTRadioButton.isSelected() );
            m_kSurfacePanel.SetLUTNew( mLUTSeparate, mRGBSeparate );
        }
        return true;
    }
}
