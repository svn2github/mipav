package gov.nih.mipav.view.renderer.WildMagic.DTI_FrameWork;


import gov.nih.mipav.util.MipavMath;

import gov.nih.mipav.model.algorithms.AlgorithmTransform;
import gov.nih.mipav.model.file.FileIO;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;
import gov.nih.mipav.view.renderer.WildMagic.Interface.JInterfaceBase;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.*;


public class JPanelDTILoad extends JInterfaceBase {

    private static final long serialVersionUID = 8011148684175711251L;

    private JButton openDTIimageButton, openDTIColorImageButton, openEVimageButton, openEValueImageButton,
            openFAimageButton;

    private JTextField textDTIimage, textDTIColorImage, textEVimage, textEValueImage, textFAimage;

    private JLabel dtiFileLabel, dtiColorFileLabel, dtiEVFileLabel, dtiEValueFileLabel, dtiFAFileLabel;

    private JButton computeButton;

    /** parent directory for the DTI output images. */
    private String m_kParentDir = null;

    /** Diffusion Tensor image. */
    private ModelImage m_kDTIImage = null;

    /** Eigenvector image * */
    private ModelImage m_kEigenVectorImage;

    /** EigenValue image * */
    private ModelImage m_kEigenValueImage;

    /** Anisotropy image * */
    private ModelImage m_kAnisotropyImage;

    private final VolumeTriPlanarInterfaceDTI parentFrame;

    /** result image * */
    private ModelImage m_kDTIColorImage;

    /** Tract input file. */
    private File m_kTractFile = null;

    /** For TRACTS dialog: number of tracts to display. */
    private JTextField m_kTractsLimit;

    /** For TRACTS dialog: minimum tract length to display. */
    private JTextField m_kTractsMin;

    /** For TRACTS dialog: maximum tract length to display. */
    private JTextField m_kTractsMax;

    /** Fiber bundle tract file input path name text box. */
    private JTextField m_kTractPath;

    public JPanelDTILoad(final VolumeTriPlanarInterfaceDTI _parentFrame) {
        parentFrame = _parentFrame;
        mainPanel = new JPanel();

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        init();
    }

    /**
     * Dispose memory.
     */
    public void disposeLocal() {

    }

    public void init() {

        buildDTIColorLoadPanel();
        buildDTILoadPanel();
        buildEVLoadPanel();
        buildFALoadPanel();
        buildEValueLoadPanel();
        buildLoadTractPanel();

        // build button panel
        final GridBagLayout kGBL = new GridBagLayout();
        final JPanel buttonPanel = new JPanel(kGBL);
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        computeButton = new JButton("Load");
        computeButton.setToolTipText("Load");
        computeButton.addActionListener(this);
        computeButton.setActionCommand("compute");
        computeButton.setVisible(true);
        computeButton.setEnabled(true);
        computeButton.setPreferredSize(new Dimension(90, 30));

        buttonPanel.add(computeButton, gbc);

        mainPanel.add(buttonPanel);

    }

    private void buildLoadTractPanel() {
        final JPanel tractLoadPanel = new JPanel(new BorderLayout());
        tractLoadPanel.setLayout(new GridBagLayout());
        tractLoadPanel.setBorder(JInterfaceBase.buildTitledBorder(""));

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);

        final JPanel kParamsPanel = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        final JLabel kNumberTractsLimit = new JLabel("Maximum number of tracts to display:");
        kParamsPanel.add(kNumberTractsLimit, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        m_kTractsLimit = new JTextField("100", 5);
        m_kTractsLimit.setBackground(Color.white);
        kParamsPanel.add(m_kTractsLimit, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        final JLabel m_kTractsMinLength = new JLabel("Minimum tract length:");
        kParamsPanel.add(m_kTractsMinLength, gbc);
        gbc.gridx++;
        m_kTractsMin = new JTextField("50", 5);
        m_kTractsMin.setBackground(Color.white);
        kParamsPanel.add(m_kTractsMin, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        final JLabel m_kTractsMaxLength = new JLabel("Maximum tract length:");
        kParamsPanel.add(m_kTractsMaxLength, gbc);
        gbc.gridx++;
        m_kTractsMax = new JTextField("100", 5);
        m_kTractsMax.setBackground(Color.white);
        kParamsPanel.add(m_kTractsMax, gbc);

        final JPanel filesPanel = new JPanel(new GridBagLayout());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.fill = GridBagConstraints.NONE;

        final JLabel kTractLabel = new JLabel(" DTI tract file: ");
        filesPanel.add(kTractLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        m_kTractPath = new JTextField();
        m_kTractPath.setPreferredSize(new Dimension(275, 21));
        m_kTractPath.setEditable(true);
        m_kTractPath.setBackground(Color.white);
        filesPanel.add(m_kTractPath, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 10, 10, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        final JButton kTractLoadButton = new JButton("Browse");
        kTractLoadButton.addActionListener(this);
        kTractLoadButton.setActionCommand("tractLoad");

        filesPanel.add(kTractLoadButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 0;

        tractLoadPanel.add(kParamsPanel, gbc);
        gbc.gridy = 5;
        tractLoadPanel.add(filesPanel, gbc);

        mainPanel.add(tractLoadPanel);

    }

    public void setDTIimage() {
        parentFrame.setDTIimage(m_kDTIImage);
    }

    public void setEVimage() {
        parentFrame.setEVimage(m_kEigenVectorImage);
    }

    public void setEValueimage() {
        parentFrame.setEValueimage(m_kEigenValueImage);
    }

    public void setFAimage() {
        parentFrame.setFAimage(m_kAnisotropyImage);
    }

    public void setParentDir() {
        parentFrame.setParentDir(m_kParentDir);
    }

    public void setDTIColorImage() {
        parentFrame.setDTIColorImage(m_kDTIColorImage);
    }

    public void actionPerformed(final ActionEvent event) {
        final String command = event.getActionCommand();
        if (command.equals("tractLoad")) {
            loadTractFile();
        } else if (command.equals("browseDTIFile")) {
            loadDTIFile();
        } else if (command.equals("browseDTIColorFile")) {
            loadDTIColorFile();
        } else if (command.equals("browseEVFile")) {
            loadEVFile();
        } else if (command.equals("browseFAFile")) {
            loadFAFile();
        } else if (command.equals("browseEValueFile")) {
            loadEValueFile();
        } else if (command.equalsIgnoreCase("compute")) {
            // processDTI();
            setParentDir();
            setTractParams();
            // setDTIimage();
            setEVimage();
            setEValueimage();
            setFAimage();
            setDTIColorImage();
            parentFrame.setDTIParamsActive();
            setDTIimage();
            parentFrame.getParamPanel().setTractParams(m_kTractFile, m_kTractsLimit, m_kTractsMin, m_kTractsMax,
                    m_kTractPath, m_kDTIImage);
            parentFrame.getParamPanel().processTractFile();
        }

    }

    public void setTractParams() {
        parentFrame.setTractParams(m_kTractFile, m_kTractsLimit, m_kTractsMin, m_kTractsMax, m_kTractPath);
    }

    /**
     * Launches the JFileChooser for the user to select the tract file. Stores the File for the tract file but does not
     * read the file.
     */
    private void loadTractFile() {
        final JFileChooser chooser = new JFileChooser(new File(Preferences.getProperty(Preferences.PREF_IMAGE_DIR)));
        chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.ALL));
        chooser.setDialogTitle("Choose Diffusion Tensor Tract file");
        final int returnValue = chooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            String kDTIName = new String(chooser.getSelectedFile().getName());
            final String kTract = new String("_tract");
            kDTIName = kDTIName.substring(0, kDTIName.length() - kTract.length());
            final FileIO fileIO = new FileIO();

            /*
             * m_kDTIImage = fileIO.readImage(kDTIName, chooser .getCurrentDirectory() + File.separator); if
             * (m_kDTIImage.getNDims() != 4) { MipavUtil .displayError("Diffusion Tensor file does not have correct
             * dimensions"); if (m_kDTIImage != null) { m_kDTIImage.disposeLocal(); m_kDTIImage = null; } } if
             * (m_kDTIImage.getExtents()[3] != 6) { MipavUtil .displayError("Diffusion Tensor does not have correct
             * dimensions"); if (m_kDTIImage != null) { m_kDTIImage.disposeLocal(); m_kDTIImage = null; } }
             */
            m_kTractFile = new File(chooser.getSelectedFile().getAbsolutePath());
            if ( !m_kTractFile.exists() || !m_kTractFile.canRead()) {
                m_kTractFile = null;
                return;
            }
            final int iLength = (int) m_kTractFile.length();
            if (iLength <= 0) {
                m_kTractFile = null;
                return;
            }
            // System.err.println("ruida: " + m_kTractFile.getName());
            m_kTractPath.setText(chooser.getSelectedFile().getAbsolutePath());
            Preferences.setProperty(Preferences.PREF_IMAGE_DIR, chooser.getCurrentDirectory().toString());
        }
    }

    public void buildDTILoadPanel() {

        final JPanel DTIloadPanel = new JPanel();
        DTIloadPanel.setLayout(new GridBagLayout());
        DTIloadPanel.setBorder(JInterfaceBase.buildTitledBorder(""));

        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.CENTER;
        gbc.anchor = GridBagConstraints.WEST;

        openDTIimageButton = new JButton("Browse");
        openDTIimageButton.setToolTipText("Browse Diffusion Tensor image file");
        openDTIimageButton.addActionListener(this);
        openDTIimageButton.setActionCommand("browseDTIFile");
        openDTIimageButton.setEnabled(true);

        textDTIimage = new JTextField();
        textDTIimage.setPreferredSize(new Dimension(275, 21));
        textDTIimage.setEditable(true);
        textDTIimage.setBackground(Color.white);
        textDTIimage.setFont(MipavUtil.font12);

        dtiFileLabel = new JLabel("DTI File: ");
        dtiFileLabel.setEnabled(true);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.fill = GridBagConstraints.CENTER;

        DTIloadPanel.add(dtiFileLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.CENTER;
        DTIloadPanel.add(textDTIimage, gbc);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 10, 10, 0);
        gbc.fill = GridBagConstraints.CENTER;
        DTIloadPanel.add(openDTIimageButton, gbc);

        mainPanel.add(DTIloadPanel);

    }

    public void buildEVLoadPanel() {

        final JPanel DTIloadPanel = new JPanel();
        DTIloadPanel.setLayout(new GridBagLayout());
        DTIloadPanel.setBorder(JInterfaceBase.buildTitledBorder(""));

        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.CENTER;
        gbc.anchor = GridBagConstraints.WEST;

        openEVimageButton = new JButton("Browse");
        openEVimageButton.setToolTipText("Browse EigenVector image file");
        openEVimageButton.addActionListener(this);
        openEVimageButton.setActionCommand("browseEVFile");
        openEVimageButton.setEnabled(true);

        textEVimage = new JTextField();
        textEVimage.setPreferredSize(new Dimension(275, 21));
        textEVimage.setEditable(true);
        textEVimage.setBackground(Color.white);
        textEVimage.setFont(MipavUtil.font12);

        dtiEVFileLabel = new JLabel("EV File: ");
        dtiEVFileLabel.setEnabled(true);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.fill = GridBagConstraints.CENTER;

        DTIloadPanel.add(dtiEVFileLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.CENTER;
        DTIloadPanel.add(textEVimage, gbc);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 10, 10, 0);
        gbc.fill = GridBagConstraints.CENTER;
        DTIloadPanel.add(openEVimageButton, gbc);

        mainPanel.add(DTIloadPanel);

    }

    public void buildFALoadPanel() {

        final JPanel DTIloadPanel = new JPanel();
        DTIloadPanel.setLayout(new GridBagLayout());
        DTIloadPanel.setBorder(JInterfaceBase.buildTitledBorder(""));

        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.CENTER;
        gbc.anchor = GridBagConstraints.WEST;

        openFAimageButton = new JButton("Browse");
        openFAimageButton.setToolTipText("Browse Functinal Anisotropy image file");
        openFAimageButton.addActionListener(this);
        openFAimageButton.setActionCommand("browseFAFile");
        openFAimageButton.setEnabled(true);

        textFAimage = new JTextField();
        textFAimage.setPreferredSize(new Dimension(275, 21));
        textFAimage.setEditable(true);
        textFAimage.setBackground(Color.white);
        textFAimage.setFont(MipavUtil.font12);

        dtiFAFileLabel = new JLabel("FA File: ");
        dtiFAFileLabel.setEnabled(true);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.fill = GridBagConstraints.CENTER;

        DTIloadPanel.add(dtiFAFileLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.CENTER;
        DTIloadPanel.add(textFAimage, gbc);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 10, 10, 0);
        gbc.fill = GridBagConstraints.CENTER;
        DTIloadPanel.add(openFAimageButton, gbc);

        mainPanel.add(DTIloadPanel);

    }

    public void buildEValueLoadPanel() {

        final JPanel DTIloadPanel = new JPanel();
        DTIloadPanel.setLayout(new GridBagLayout());
        DTIloadPanel.setBorder(JInterfaceBase.buildTitledBorder(""));

        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.CENTER;
        gbc.anchor = GridBagConstraints.WEST;

        openEValueImageButton = new JButton("Browse");
        openEValueImageButton.setToolTipText("Browse EigenValue image file");
        openEValueImageButton.addActionListener(this);
        openEValueImageButton.setActionCommand("browseEValueFile");
        openEValueImageButton.setEnabled(true);

        textEValueImage = new JTextField();
        textEValueImage.setPreferredSize(new Dimension(275, 21));
        textEValueImage.setEditable(true);
        textEValueImage.setBackground(Color.white);
        textEValueImage.setFont(MipavUtil.font12);

        dtiEValueFileLabel = new JLabel("EValue : ");
        dtiEValueFileLabel.setEnabled(true);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.fill = GridBagConstraints.CENTER;

        DTIloadPanel.add(dtiEValueFileLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.CENTER;
        DTIloadPanel.add(textEValueImage, gbc);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 10, 10, 0);
        gbc.fill = GridBagConstraints.CENTER;
        DTIloadPanel.add(openEValueImageButton, gbc);

        mainPanel.add(DTIloadPanel);

    }

    public void buildDTIColorLoadPanel() {

        final JPanel DTIloadPanel = new JPanel();
        DTIloadPanel.setLayout(new GridBagLayout());
        DTIloadPanel.setBorder(JInterfaceBase.buildTitledBorder(""));

        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.CENTER;
        gbc.anchor = GridBagConstraints.WEST;

        openDTIColorImageButton = new JButton("Browse");
        openDTIColorImageButton.setToolTipText("Browse Diffusion Tensor color image file");
        openDTIColorImageButton.addActionListener(this);
        openDTIColorImageButton.setActionCommand("browseDTIColorFile");
        openDTIColorImageButton.setEnabled(true);

        textDTIColorImage = new JTextField();
        textDTIColorImage.setPreferredSize(new Dimension(275, 21));
        textDTIColorImage.setEditable(true);
        textDTIColorImage.setBackground(Color.white);
        textDTIColorImage.setFont(MipavUtil.font12);

        dtiColorFileLabel = new JLabel("DTI Color: ");
        dtiColorFileLabel.setEnabled(true);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.fill = GridBagConstraints.CENTER;

        DTIloadPanel.add(dtiColorFileLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.CENTER;
        DTIloadPanel.add(textDTIColorImage, gbc);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 10, 10, 0);
        gbc.fill = GridBagConstraints.CENTER;
        DTIloadPanel.add(openDTIColorImageButton, gbc);

        mainPanel.add(DTIloadPanel);

    }

    /**
     * Resizing the control panel with ViewJFrameVolumeView's frame width and height.
     * 
     * @param panelWidth DOCUMENT ME!
     * @param frameHeight DOCUMENT ME!
     */
    public void resizePanel(final int panelWidth, final int frameHeight) {
        mainPanel.setPreferredSize(new Dimension(panelWidth, frameHeight - 40));
        mainPanel.setSize(new Dimension(panelWidth, frameHeight - 40));
        mainPanel.revalidate();
    }

    /**
     * Launches the JFileChooser for the user to select the Diffusion Tensor Image. Loads the tensor data.
     */
    public void loadDTIFile() {
        final JFileChooser chooser = new JFileChooser(new File(Preferences.getProperty(Preferences.PREF_IMAGE_DIR)));
        chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.TECH));
        chooser.setDialogTitle("Choose Diffusion Tensor file");
        final int returnValue = chooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            final FileIO fileIO = new FileIO();

            m_kDTIImage = fileIO.readImage(chooser.getSelectedFile().getName(), chooser.getCurrentDirectory()
                    + File.separator);

            m_kDTIImage = resampleImage(m_kDTIImage);

            textDTIimage.setText(chooser.getSelectedFile().getAbsolutePath());
            Preferences.setProperty(Preferences.PREF_IMAGE_DIR, chooser.getCurrentDirectory().toString());
        }
    }

    public void loadDTIColorFile() {
        final JFileChooser chooser = new JFileChooser(new File(Preferences.getProperty(Preferences.PREF_IMAGE_DIR)));
        chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.TECH));
        chooser.setDialogTitle("Choose Diffusion Tensor Color image file");
        final int returnValue = chooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            final FileIO fileIO = new FileIO();

            m_kDTIColorImage = fileIO.readImage(chooser.getSelectedFile().getName(), chooser.getCurrentDirectory()
                    + File.separator);

            m_kDTIColorImage = resampleImage(m_kDTIColorImage);

            m_kParentDir = chooser.getCurrentDirectory().getPath();

            textDTIColorImage.setText(chooser.getSelectedFile().getAbsolutePath());
            Preferences.setProperty(Preferences.PREF_IMAGE_DIR, chooser.getCurrentDirectory().toString());
        }
    }

    public void loadEVFile() {
        final JFileChooser chooser = new JFileChooser(new File(Preferences.getProperty(Preferences.PREF_IMAGE_DIR)));
        chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.TECH));
        chooser.setDialogTitle("Choose EigenVector image file");
        final int returnValue = chooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            final FileIO fileIO = new FileIO();

            m_kEigenVectorImage = fileIO.readImage(chooser.getSelectedFile().getName(), chooser.getCurrentDirectory()
                    + File.separator);

            m_kEigenVectorImage = resampleImage(m_kEigenVectorImage);

            textEVimage.setText(chooser.getSelectedFile().getAbsolutePath());
            Preferences.setProperty(Preferences.PREF_IMAGE_DIR, chooser.getCurrentDirectory().toString());
        }
    }

    public void loadFAFile() {
        final JFileChooser chooser = new JFileChooser(new File(Preferences.getProperty(Preferences.PREF_IMAGE_DIR)));
        chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.TECH));
        chooser.setDialogTitle("Choose EigenVector image file");
        final int returnValue = chooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            final FileIO fileIO = new FileIO();

            m_kAnisotropyImage = fileIO.readImage(chooser.getSelectedFile().getName(), chooser.getCurrentDirectory()
                    + File.separator);
            m_kAnisotropyImage = resampleImage(m_kAnisotropyImage);

            textFAimage.setText(chooser.getSelectedFile().getAbsolutePath());
            Preferences.setProperty(Preferences.PREF_IMAGE_DIR, chooser.getCurrentDirectory().toString());
        }
    }

    public void loadEValueFile() {
        final JFileChooser chooser = new JFileChooser(new File(Preferences.getProperty(Preferences.PREF_IMAGE_DIR)));
        chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.TECH));
        chooser.setDialogTitle("Choose EigenVector image file");
        final int returnValue = chooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            final FileIO fileIO = new FileIO();

            m_kEigenValueImage = fileIO.readImage(chooser.getSelectedFile().getName(), chooser.getCurrentDirectory()
                    + File.separator);
            m_kEigenValueImage = resampleImage(m_kEigenValueImage);

            textEValueImage.setText(chooser.getSelectedFile().getAbsolutePath());
            Preferences.setProperty(Preferences.PREF_IMAGE_DIR, chooser.getCurrentDirectory().toString());
        }
    }

    /**
     * Resample 4D image to power of 2
     * 
     * @param srcImage source image need to be resampled
     */
    private ModelImage resampleImage(ModelImage srcImage) {
        final int[] extents = srcImage.getExtents();
        final float[] res = srcImage.getFileInfo(0).getResolutions();
        float[] saveRes;

        if (extents.length == 3) {
            saveRes = new float[] {res[0], res[1], res[2]};
        } else {
            saveRes = new float[] {res[0], res[1], res[2], res[3]};
        }

        final float[] newRes = new float[extents.length];
        final int[] volExtents = new int[extents.length];
        boolean originalVolPowerOfTwo = true;
        int volSize = 1;
        for (int i = 0; i < extents.length; i++) {
            volExtents[i] = MipavMath.dimPowerOfTwo(extents[i]);
            volSize *= volExtents[i];

            if ( (i < 3) && volExtents[i] != extents[i]) {
                originalVolPowerOfTwo = false;
            }
            newRes[i] = (res[i] * (extents[i])) / (volExtents[i]);
            saveRes[i] = (saveRes[i] * (extents[i])) / (volExtents[i]);
        }

        if ( !originalVolPowerOfTwo) {
            AlgorithmTransform transformFunct = new AlgorithmTransform(srcImage, new TransMatrix(4),
                    AlgorithmTransform.TRILINEAR, newRes[0], newRes[1], newRes[2], volExtents[0], volExtents[1],
                    volExtents[2], false, true, false);
            transformFunct.setRunningInSeparateThread(false);
            transformFunct.run();

            if (transformFunct.isCompleted() == false) {
                transformFunct.finalize();
                transformFunct = null;
            }

            srcImage = transformFunct.getTransformedImage();
            srcImage.calcMinMax();

        }
        return srcImage;

    }

}
