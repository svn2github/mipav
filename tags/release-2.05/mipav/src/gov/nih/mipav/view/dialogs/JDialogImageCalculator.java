package gov.nih.mipav.view.dialogs;


import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.algorithms.utilities.*;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import javax.swing.*;


/**
 * Dialog to get user input, then call the algorithm. The user has the option to generate a new image or replace the
 * source image. In addition the user can indicate if you wishes to have the algorithm applied to whole image or to the
 * VOI regions. In should be noted, that the algorithms are executed in their own thread.
 *
 * @version  0.1 Dec 21, 1999
 * @author   Matthew J. McAuliffe, Ph.D.
 */
public class JDialogImageCalculator extends JDialogBase implements AlgorithmInterface, ScriptableInterface {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = -7373755862535613674L;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** Advanced function string. */
    private String adOpString = null;

    /** DOCUMENT ME! */
    private JButton advancedButton;

    /** DOCUMENT ME! */
    private int clipMode = AlgorithmImageMath.CLIP;

    /** DOCUMENT ME! */
    private JComboBox comboBoxImage;

    /** DOCUMENT ME! */
    private JComboBox comboBoxOperator;

    /** DOCUMENT ME! */
    private int displayLoc = NEW;

    /** DOCUMENT ME! */
    private ModelImage imageA; // source image

    /** DOCUMENT ME! */
    private ModelImage imageB;

    /** DOCUMENT ME! */
    private boolean isColor = false;

    /** DOCUMENT ME! */
    private AlgorithmImageCalculator mathAlgo;

    /** DOCUMENT ME! */
    private int opType;

    /** DOCUMENT ME! */
    private JRadioButton radioClip;

    /** DOCUMENT ME! */
    private JRadioButton radioNew;

    /** DOCUMENT ME! */
    private JRadioButton radioPromote;

    /** DOCUMENT ME! */
    private JRadioButton radioReplace;

    /** DOCUMENT ME! */
    private ModelImage resultImage = null; // result image

    /** DOCUMENT ME! */
    private String[] titles;

    /** DOCUMENT ME! */
    private ViewUserInterface userInterface;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Empty constructor needed for dynamic instantiation (used during scripting).
     */
    public JDialogImageCalculator() { }

    /**
     * Creates new image calculator dialog and displays.
     *
     * @param  theParentFrame  Parent frame.
     * @param  im              Source image.
     */
    public JDialogImageCalculator(Frame theParentFrame, ModelImage im) {
        super(theParentFrame, true);
        imageA = im;
        isColor = im.isColorImage();
        userInterface = ((ViewJFrameBase) (parentFrame)).getUserInterface();
        init();
    }

    /**
     * Used primarily for the script to store variables and run the algorithm. No actual dialog will appear but the set
     * up info and result image will be stored here.
     *
     * @param  UI  The user interface, needed to create the image frame.
     * @param  im  Source image.
     */
    public JDialogImageCalculator(ViewUserInterface UI, ModelImage im) {
        super();
        userInterface = UI;
        imageA = im;
        isColor = im.isColorImage();
        parentFrame = im.getParentFrame();
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Closes dialog box when the OK button is pressed and calls the algorithm.
     *
     * @param  event  event that triggers function
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        Object source = event.getSource();

        if (command.equals("OK")) {

            if (setVariables()) {
                callAlgorithm();
            }
        } else if (command.equals("Cancel")) {
            dispose();
        } else if (source == advancedButton) {

            if (setVariables()) {
                opType = AlgorithmImageCalculator.ADVANCED;
                callAlgorithm();
            }
        }
    }

    // ************************************************************************
    // ************************** Algorithm Events ****************************
    // ************************************************************************

    /**
     * This method is required if the AlgorithmPerformed interface is implemented. It is called by the algorithms when
     * it has completed or failed to to complete, so that the dialog can be display the result image and/or clean up.
     *
     * @param  algorithm  Algorithm that caused the event.
     */
    public void algorithmPerformed(AlgorithmBase algorithm) {

        ViewJFrameImage imageFrame = null;

        if (algorithm instanceof AlgorithmImageCalculator) {

            if ((mathAlgo.isCompleted() == true) && (resultImage != null)) {

                // The algorithm has completed and produced a new image to be displayed.
                updateFileInfo(imageA, resultImage);

                try {
                    imageFrame = new ViewJFrameImage(resultImage, null, new Dimension(610, 200));
                } catch (OutOfMemoryError error) {
                    System.gc();
                    MipavUtil.displayError("Out of memory: unable to open new frame");
                }
            } else if (resultImage == null) {

                // These next lines set the titles in all frames where the source image is displayed to
                // image name so as to indicate that the image is now unlocked!
                // The image frames are enabled and then registered to the userinterface.
                Vector imageFrames = imageA.getImageFrameVector();

                for (int i = 0; i < imageFrames.size(); i++) {
                    ((ViewJFrameBase) (imageFrames.elementAt(i))).setTitle(titles[i]);
                    ((ViewJFrameBase) (imageFrames.elementAt(i))).setEnabled(true);

                    if (((Frame) (imageFrames.elementAt(i))) != parentFrame) {
                        userInterface.registerFrame((Frame) (imageFrames.elementAt(i)));
                    }
                }

                if (parentFrame != null) {
                    userInterface.registerFrame(parentFrame);
                }

                imageA.notifyImageDisplayListeners(null, true);
            } else if (resultImage != null) {

                // algorithm failed but result image still has garbage
                resultImage.disposeLocal(); // clean up memory
                resultImage = null;
                System.gc();
            }
        }

        insertScriptLine(algorithm);

        // Update frame
        mathAlgo.finalize();
        mathAlgo = null;
        dispose();
    }

    /**
     * Accessor that returns the image.
     *
     * @return  The result image.
     */
    public ModelImage getResultImage() {
        return resultImage;
    }

    /**
     * If a script is being recorded and the algorithm is done, add an entry for this algorithm.
     *
     * @param  algo  the algorithm to make an entry for
     */
    public void insertScriptLine(AlgorithmBase algo) {

        if (algo.isCompleted()) {

            if (userInterface.isScriptRecording()) {
                adOpString = mathAlgo.getAdvFunction();

                // check to see if the  imageA is already in the ImgTable
                if (userInterface.getScriptDialog().getImgTableVar(imageA.getImageName()) == null) {

                    if (userInterface.getScriptDialog().getActiveImgTableVar(imageA.getImageName()) == null) {
                        userInterface.getScriptDialog().putActiveVar(imageA.getImageName());
                    }
                }

                // check to see if the  imageA is already in the ImgTable
                if (userInterface.getScriptDialog().getImgTableVar(imageB.getImageName()) == null) {

                    if (userInterface.getScriptDialog().getActiveImgTableVar(imageB.getImageName()) == null) {
                        userInterface.getScriptDialog().putActiveVar(imageB.getImageName());
                    }
                }

                userInterface.getScriptDialog().append("ImageCalculator " +
                                                       userInterface.getScriptDialog().getVar(imageA.getImageName()) +
                                                       " " +
                                                       userInterface.getScriptDialog().getVar(imageB.getImageName()) +
                                                       " ");

                if (displayLoc == NEW) {
                    userInterface.getScriptDialog().putVar(resultImage.getImageName());
                    userInterface.getScriptDialog().append(userInterface.getScriptDialog().getVar(resultImage.getImageName()) +
                                                           " " + opType + " " + clipMode + " " + adOpString + "\n");
                } else {
                    userInterface.getScriptDialog().append(userInterface.getScriptDialog().getVar(imageA.getImageName()) +
                                                           " " + opType + " " + clipMode + " " + adOpString + "\n");

                }
            }
        }
    }

    // *******************************************************************
    // ************************* Item Events ****************************
    // *******************************************************************

    /**
     * Changes chosen operator based on combo box's selected index.
     *
     * @param  event  DOCUMENT ME!
     */
    public void itemStateChanged(ItemEvent event) {
        Object source = event.getSource();

        if (source == comboBoxOperator) {

            if (comboBoxOperator.getSelectedIndex() == 0) {
                opType = AlgorithmImageCalculator.ADD;
            } else if (comboBoxOperator.getSelectedIndex() == 1) {
                opType = AlgorithmImageCalculator.AND;
            } else if (comboBoxOperator.getSelectedIndex() == 2) {
                opType = AlgorithmImageCalculator.AVERAGE;
            } else if (comboBoxOperator.getSelectedIndex() == 3) {
                opType = AlgorithmImageCalculator.DIFFERENCE;
            } else if (comboBoxOperator.getSelectedIndex() == 4) {
                opType = AlgorithmImageCalculator.DIVIDE;
            } else if (comboBoxOperator.getSelectedIndex() == 5) {
                opType = AlgorithmImageCalculator.MAXIMUM;
            } else if (comboBoxOperator.getSelectedIndex() == 6) {
                opType = AlgorithmImageCalculator.MINIMUM;
            } else if (comboBoxOperator.getSelectedIndex() == 7) {
                opType = AlgorithmImageCalculator.MULTIPLY;
            } else if (comboBoxOperator.getSelectedIndex() == 8) {
                opType = AlgorithmImageCalculator.OR;
            } else if (comboBoxOperator.getSelectedIndex() == 9) {
                opType = AlgorithmImageCalculator.SUBTRACT;
            } else if (comboBoxOperator.getSelectedIndex() == 10) {
                opType = AlgorithmImageCalculator.XOR;
            }
        }
    }

    /**
     * Run this algorithm from a script.
     *
     * @param   parser  the script parser we get the state from
     *
     * @throws  IllegalArgumentException  if there is something wrong with the arguments in the script
     */
    public void scriptRun(AlgorithmScriptParser parser) throws IllegalArgumentException {
        String image1Key = null;
        String image2Key = null;
        String destImageKey = null;

        try {
            image1Key = parser.getNextString();
            image2Key = parser.getNextString();
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        ModelImage im1 = parser.getImage(image1Key);
        ModelImage im2 = parser.getImage(image2Key);

        imageA = im1;
        isColor = im1.isColorImage();
        setImageB(im2);
        userInterface = imageA.getUserInterface();
        parentFrame = imageA.getParentFrame();

        // the result image
        try {
            destImageKey = parser.getNextString();
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        if (image1Key.equals(destImageKey)) {
            this.setDisplayLocReplace();
        } else {
            this.setDisplayLocNew();
        }

        try {
            setOperator(parser.getNextInteger());
            setClipMode(parser.getNextInteger());
            setAdOpString(parser.getNextString());
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        setActiveImage(parser.isActiveImage());
        setSeparateThread(false);
        callAlgorithm();

        if (!image1Key.equals(destImageKey)) {
            parser.putVariable(destImageKey, getResultImage().getImageName());
        }
    }

    /**
     * Accessor that sets the advanced function string.
     *
     * @param  fStr  of an advanced function
     */
    public void setAdOpString(String fStr) {
        adOpString = fStr;
    }

    /**
     * Accessor that sets the clip mode.
     *
     * @param  n  the clip mode to be used when performing the math algorithm
     */
    public void setClipMode(int n) {
        clipMode = n;
    }

    /**
     * Accessor that sets the display loc variable to new, so that a new image is created once the algorithm completes.
     */
    public void setDisplayLocNew() {
        displayLoc = NEW;
    }

    /**
     * Accessor that sets the display loc variable to replace, so the current image is replaced once the algorithm
     * completes.
     */
    public void setDisplayLocReplace() {
        displayLoc = REPLACE;
    }

    /**
     * Accessor that sets image B.
     *
     * @param  im  Image B.
     */
    public void setImageB(ModelImage im) {
        imageB = im;
    }

    /**
     * Accessor that sets the operator type.
     *
     * @param  n  operator type
     */
    public void setOperator(int n) {
        opType = n;
    }

    /**
     * Builds a list of images to operate on from the template image.
     */
    private void buildComboBoxImage() {
        int j;
        ViewUserInterface UI;
        boolean sameDims = true;

        comboBoxImage = new JComboBox();
        comboBoxImage.setFont(serif12);
        comboBoxImage.setBackground(Color.white);

        UI = imageA.getUserInterface();

        Enumeration names = UI.getRegisteredImageNames();

        // Add images from user interface that have the same exact dimensionality
        // Guaranteed to have at least one unique potential image B, because it's
        // tested for in ViewJFrameImage before this dialog is created.
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            sameDims = true;

            if (!imageA.getImageName().equals(name)) {
                ModelImage img = UI.getRegisteredImageByName(name);

                if (UI.getFrameContainingImage(img) != null) {

                    if (imageA.getNDims() == img.getNDims()) {

                        for (j = 0; j < imageA.getNDims(); j++) {

                            if (imageA.getExtents()[j] != img.getExtents()[j]) {
                                sameDims = false;
                            }
                        }

                        if ((sameDims == true) && (isColor == img.isColorImage())) {
                            comboBoxImage.addItem(name);
                        }
                    } else if ((imageA.getNDims() == 3) && (img.getNDims() == 2)) {

                        if ((imageA.getExtents()[0] == img.getExtents()[0]) &&
                                (imageA.getExtents()[1] == img.getExtents()[1]) && (isColor == img.isColorImage())) {
                            comboBoxImage.addItem(name);
                        }
                    }
                }
            }
        }
    }

    /**
     * Once all the necessary variables are set, call the Gaussian Blur algorithm based on what type of image this is
     * and whether or not there is a separate destination image.
     */
    private void callAlgorithm() {
        System.gc();

        int i;

        if (imageA.getNDims() <= 5) {

            if (displayLoc == NEW) {

                try {

                    // Make result image of float type
                    resultImage = new ModelImage(imageA.getType(), imageA.getExtents(),
                                                 makeImageName(imageA.getImageName(), "_calc"), userInterface);

                    // Make algorithm
                    mathAlgo = new AlgorithmImageCalculator(resultImage, imageA, imageB, opType, clipMode, true,
                                                            adOpString);

                    // This is very important. Adding this object as a listener allows the algorithm to
                    // notify this object when it has completed of failed. See algorithm performed event.
                    // This is made possible by implementing AlgorithmedPerformed interface
                    mathAlgo.addListener(this);

                    // Hide dialog
                    setVisible(false);

                    if (runInSeparateThread) {

                        // Start the thread as a low priority because we wish to still have user interface work fast.
                        if (mathAlgo.startMethod(Thread.MIN_PRIORITY) == false) {
                            MipavUtil.displayError("A thread is already running on this object");
                        }
                    } else {
                        mathAlgo.setActiveImage(isActiveImage);

                        if (!userInterface.isAppFrameVisible()) {
                            mathAlgo.setProgressBarVisible(false);
                        }

                        mathAlgo.run();
                    }
                } catch (OutOfMemoryError x) {

                    if (resultImage != null) {
                        resultImage.disposeLocal(); // Clean up memory of result image
                        resultImage = null;
                    }

                    System.gc();
                    MipavUtil.displayError("Dialog Image math: unable to allocate enough memory");

                    return;
                }
            } else {

                try {

                    // No need to make new image space because the user has choosen to replace the source image
                    // Make the algorithm class
                    mathAlgo = new AlgorithmImageCalculator(imageA, imageB, opType, clipMode, true, adOpString);

                    // This is very important. Adding this object as a listener allows the algorithm to
                    // notify this object when it has completed of failed. See algorithm performed event.
                    // This is made possible by implementing AlgorithmedPerformed interface
                    mathAlgo.addListener(this);

                    // Hide the dialog since the algorithm is about to run.
                    setVisible(false);

                    // These next lines set the titles in all frames where the source image is displayed to
                    // "locked - " image name so as to indicate that the image is now read/write locked!
                    // The image frames are disabled and then unregisted from the userinterface until the
                    // algorithm has completed.
                    Vector imageFrames = imageA.getImageFrameVector();
                    titles = new String[imageFrames.size()];

                    for (i = 0; i < imageFrames.size(); i++) {
                        titles[i] = ((ViewJFrameBase) (imageFrames.elementAt(i))).getTitle();
                        ((ViewJFrameBase) (imageFrames.elementAt(i))).setTitle("Locked: " + titles[i]);
                        ((ViewJFrameBase) (imageFrames.elementAt(i))).setEnabled(false);
                        userInterface.unregisterFrame((Frame) (imageFrames.elementAt(i)));
                    }

                    if (runInSeparateThread) {

                        // Start the thread as a low priority because we wish to still have user interface.
                        if (mathAlgo.startMethod(Thread.MIN_PRIORITY) == false) {
                            MipavUtil.displayError("A thread is already running on this object");
                        }
                    } else {
                        mathAlgo.setActiveImage(isActiveImage);

                        if (!userInterface.isAppFrameVisible()) {
                            mathAlgo.setProgressBarVisible(false);
                        }

                        mathAlgo.run();
                    }
                } catch (OutOfMemoryError x) {
                    System.gc();
                    MipavUtil.displayError("Dialog Image Math: unable to allocate enough memory");

                    return;
                }
            }
        }
    }

    /**
     * Sets up the GUI (panels, buttons, etc) and displays it on the screen.
     */
    private void init() {
        setForeground(Color.black);
        setTitle("Image Calculator");

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setForeground(Color.black);

        inputPanel.setBorder(buildTitledBorder("ImageA  <operator>  ImageB"));

        JLabel labelUse = new JLabel("Image A:");
        labelUse.setForeground(Color.black);
        labelUse.setFont(serif12);

        JLabel labelImageA = new JLabel(imageA.getImageName());
        labelImageA.setForeground(Color.black);
        labelImageA.setFont(serif12);

        JLabel labelImageB = new JLabel("Image B: ");
        labelImageB.setForeground(Color.black);
        labelImageB.setFont(serif12);

        buildComboBoxImage();

        JLabel labelOperator = new JLabel("Operator:");
        labelOperator.setForeground(Color.black);
        labelOperator.setFont(serif12);

        comboBoxOperator = new JComboBox();
        comboBoxOperator.setFont(serif12);
        comboBoxOperator.setBackground(Color.white);

        comboBoxOperator.addItem("Add");
        comboBoxOperator.addItem("AND");
        comboBoxOperator.addItem("Average");
        comboBoxOperator.addItem("Difference");
        comboBoxOperator.addItem("Divide");
        comboBoxOperator.addItem("Maximum");
        comboBoxOperator.addItem("Minimum");
        comboBoxOperator.addItem("Multiply");
        comboBoxOperator.addItem("OR");
        comboBoxOperator.addItem("Subtract");
        comboBoxOperator.addItem("XOR");

        comboBoxOperator.addItemListener(this);

        ButtonGroup group = new ButtonGroup();
        radioClip = new JRadioButton("Clip", true);
        radioClip.setFont(serif12);
        group.add(radioClip);

        radioPromote = new JRadioButton("Promote destination image type", false);
        radioPromote.setFont(serif12);
        group.add(radioPromote);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.anchor = gbc.WEST;
        gbc.weightx = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        inputPanel.add(labelUse, gbc);
        gbc.gridx = 1;
        inputPanel.add(labelImageA, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(labelOperator, gbc);
        gbc.gridx = 1;
        gbc.fill = gbc.HORIZONTAL;
        inputPanel.add(comboBoxOperator, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = gbc.NONE;
        inputPanel.add(labelImageB, gbc);
        gbc.gridx = 1;
        gbc.fill = gbc.HORIZONTAL;
        inputPanel.add(comboBoxImage, gbc);


        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = gbc.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);
        inputPanel.add(radioClip, gbc);
        gbc.gridy = 4;
        gbc.gridwidth = gbc.REMAINDER;
        inputPanel.add(radioPromote, gbc);

        JPanel outputPanel = new JPanel(new GridBagLayout());
        outputPanel.setForeground(Color.black);
        outputPanel.setBorder(buildTitledBorder("Destination"));

        ButtonGroup group2 = new ButtonGroup();
        radioNew = new JRadioButton("New image", true);
        radioNew.setFont(serif12);
        group2.add(radioNew);

        radioReplace = new JRadioButton("Replace image A", false);
        radioReplace.setFont(serif12);
        group2.add(radioReplace);

        gbc.gridx = 0;
        gbc.gridy = 0;
        outputPanel.add(radioNew, gbc);
        gbc.gridy = 1;
        outputPanel.add(radioReplace, gbc);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(inputPanel);
        mainPanel.add(outputPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel buttonPanel = new JPanel();

        buildOKButton();
        advancedButton = new JButton("Advanced");
        advancedButton.addActionListener(this);
        advancedButton.setMinimumSize(MipavUtil.defaultButtonSize);
        advancedButton.setPreferredSize(MipavUtil.defaultButtonSize);
        advancedButton.setFont(serif12B);
        buildCancelButton();
        buttonPanel.add(OKButton);
        buttonPanel.add(advancedButton);
        buttonPanel.add(cancelButton);

        getContentPane().add(mainPanel);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setVisible(true);
    }

    /**
     * Use the GUI results to set up the variables needed to run the algorithm.
     *
     * @return  <code>true</code> if parameters set successfully, <code>false</code> otherwise.
     */
    private boolean setVariables() {

        if (radioNew.isSelected()) {
            displayLoc = NEW;
        } else {
            displayLoc = REPLACE;
        }

        if (radioClip.isSelected()) {
            clipMode = AlgorithmImageCalculator.CLIP;
        } else if (radioPromote.isSelected()) {
            clipMode = AlgorithmImageCalculator.PROMOTE;
        }

        ViewUserInterface UI = imageA.getUserInterface();
        String selectedName = (String) comboBoxImage.getSelectedItem();
        imageB = UI.getRegisteredImageByName(selectedName);

        return true;
    }

}
