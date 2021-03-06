package gov.nih.mipav.view.dialogs;


import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.algorithms.utilities.*;
import gov.nih.mipav.model.scripting.*;
import gov.nih.mipav.model.scripting.parameters.*;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import javax.swing.*;


/**
 * Dialog to choose images, then call the RGBConcat algorithm.
 *
 * @version  0.1 June 5, 2000
 * @author   Matthew J. McAuliffe, Ph.D.
 */
public class JDialogRGBConcat extends JDialogScriptableBase implements AlgorithmInterface {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = 4183252789435843400L;

    /** Red channel. */
    private static final int RED = 0;

    /** Green channel. */
    private static final int GREEN = 1;

    /** Blue channel. */
    private static final int BLUE = 2;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** DOCUMENT ME! */
    private ModelImage blank = null;

    /** DOCUMENT ME! */
    private JCheckBox cBoxRemap;

    /** DOCUMENT ME! */
    private JComboBox comboBoxImageBlue;

    /** DOCUMENT ME! */
    private JComboBox comboBoxImageGreen;

    /** DOCUMENT ME! */
    private JComboBox comboBoxImageRed;

    /** DOCUMENT ME! */
    private ButtonGroup destinationGroup;

    /** DOCUMENT ME! */
    private JPanel destinationPanel;

    /** DOCUMENT ME! */
    private int displayLoc; // Flag indicating if a new image is to be generated
                            // or if the source image is to be replaced

    /** DOCUMENT ME! */
    private ModelImage imageB;

    /** DOCUMENT ME! */
    private ModelImage imageG;

    /** DOCUMENT ME! */
    private ModelImage imageR; // source image

    /** DOCUMENT ME! */
    private AlgorithmRGBConcat mathAlgo;

    /** DOCUMENT ME! */
    private JRadioButton newImage;

    /** DOCUMENT ME! */
    private boolean remapMode;

    /** DOCUMENT ME! */
    private JRadioButton replaceImage;

    /** DOCUMENT ME! */
    private ModelImage resultImage = null; // result image

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Empty constructor needed for dynamic instantiation (used during scripting).
     */
    public JDialogRGBConcat() { }


    /**
     * Creates new dialog to enter parameters for RGBConcat algorithm.
     *
     * @param  theParentFrame  Parent frame
     * @param  im              Source image
     */
    public JDialogRGBConcat(Frame theParentFrame, ModelImage im) {
        super(theParentFrame, false);

        imageR = im;
        init();
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Closes dialog box when the OK button is pressed and calls the algorithm.
     *
     * @param  event  Event that triggers function
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        if (command.equals("OK")) {

            if (setVariables()) {
                callAlgorithm();
            }
        } else if (command.equals("Cancel")) {
            dispose();
        } else if (command.equals("Help")) {
            MipavUtil.showHelp("U4015");
        }
    }


    // ************************************************************************
    // ************************** Algorithm Events ****************************
    // ************************************************************************

    /**
     * This method is required if the AlgorithmPerformed interface is implemented. It is called by the algorithm when it
     * has completed or failed to to complete, so that the dialog can be display the result image and/or clean up.
     *
     * @param  algorithm  Algorithm that caused the event.
     */
    public void algorithmPerformed(AlgorithmBase algorithm) {

        if (algorithm instanceof AlgorithmRGBConcat) {

            if ((mathAlgo.isCompleted() == true) && (resultImage != null)) {

                // The algorithm has completed and produced a new image to be displayed.
                updateFileInfo(imageR, resultImage);

                try {
                    new ViewJFrameImage(resultImage, null, new Dimension(610, 200));
                } catch (OutOfMemoryError error) {
                    System.gc();
                    MipavUtil.displayError("Out of memory: unable to open new frame");
                }
            } else if ((mathAlgo.isCompleted() == true) && (resultImage == null)) {
                imageR = mathAlgo.getImageR();

                try {
                    new ViewJFrameImage(imageR, null, new Dimension(610, 200));
                } catch (OutOfMemoryError error) {
                    System.gc();
                    MipavUtil.displayError("Out of memory: unable to open new frame");
                }
            } else if (resultImage != null) {

                // algorithm failed but result image still has garbage
                resultImage.disposeLocal(); // clean up memory
                resultImage = null;
                System.gc();
            }
        }

        if (blank != null) {

            // System.err.println("disposed local on blank image");
            blank.disposeLocal();
            blank = null;
        }

        if (algorithm.isCompleted()) {
            insertScriptLine();
        }

        mathAlgo.finalize();
        mathAlgo = null;
        dispose();
    }

    /**
     * Accessor that returns the image.
     *
     * @return  The result image
     */
    public ModelImage getResultImage() {
        return resultImage;
    }

    /**
     * Accessor that sets the Blue Image Source.
     *
     * @param  im  image to set the Blue Image Source to.
     */
    public void setBlueImage(ModelImage im) {
        imageB = im;
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
     * Accessor that sets the Green Image Source.
     *
     * @param  im  image to set the Green Image Source to.
     */
    public void setGreenImage(ModelImage im) {
        imageG = im;
    }

    /**
     * Accessor that sets the remap mode.
     *
     * @param  flag  <code>true</code> indicates remap data.
     */
    public void setRemapMode(boolean flag) {
        remapMode = flag;
    }

    /**
     * Once all the necessary variables are set, call the RGBConcat algorithm based on what type of image this is and
     * whether or not there is a separate destination image.
     */
    protected void callAlgorithm() {

        if (displayLoc == NEW) {

            try {
                System.gc();
                resultImage = new ModelImage(ModelImage.ARGB, imageR.getExtents(),
                                             makeImageName(imageR.getImageName(), "_rgb"));

                // Make algorithm
                mathAlgo = new AlgorithmRGBConcat(imageR, imageG, imageB, resultImage, remapMode, true);

                // This is very important. Adding this object as a listener allows the algorithm to
                // notify this object when it has completed of failed. See algorithm performed event.
                // This is made possible by implementing AlgorithmedPerformed interface
                mathAlgo.addListener(this);

                createProgressBar(imageR.getImageName(), mathAlgo);

                // Hide dialog
                setVisible(false);

                if (isRunInSeparateThread()) {

                    // Start the thread as a low priority because we wish to still have user interface work fast.
                    if (mathAlgo.startMethod(Thread.MIN_PRIORITY) == false) {
                        MipavUtil.displayError("A thread is already running on this object");
                    }
                } else {
                    mathAlgo.run();
                }

            } catch (OutOfMemoryError x) {

                if (resultImage != null) {
                    resultImage.disposeLocal(); // Clean up memory of result image
                    resultImage = null;
                }

                System.gc();
                MipavUtil.displayError("Dialog RGB concat: unable to allocate enough memory");

                return;
            }
        } // if (displayLoc == NEW)
        else { // displayLoc == REPLACE

            try {
                System.gc();

                // No need to make new image space because the user has choosen to replace the source image
                // Make the algorithm class
                // Make algorithm
                mathAlgo = new AlgorithmRGBConcat(imageR, imageG, imageB, remapMode, true);

                // This is very important. Adding this object as a listener allows the algorithm to
                // notify this object when it has completed of failed. See algorithm performed event.
                // This is made possible by implementing AlgorithmedPerformed interface
                mathAlgo.addListener(this);

                createProgressBar(imageR.getImageName(), mathAlgo);

                // Hide the dialog since the algorithm is about to run.
                setVisible(false);

                // These next lines set the titles in all frames where the source image is displayed to
                // "locked - " image name so as to indicate that the image is now read/write locked!
                // The image frames are disabled and then unregisted from the userinterface until the
                // algorithm has completed.
                /*Vector imageFrames = imageR.getImageFrameVector();
                 *
                 * titles = new String[imageFrames.size()]; for ( int i = 0; i < imageFrames.size(); i++ ) { titles[i] = (
                 * (Frame) ( imageFrames.elementAt( i ) ) ).getTitle(); ( (Frame) ( imageFrames.elementAt( i ) )
                 * ).setTitle( "Locked: " + titles[i] ); ( (Frame) ( imageFrames.elementAt( i ) ) ).setEnabled( false );
                 * userInterface.unregisterFrame( (Frame) ( imageFrames.elementAt( i ) ) );}*/

                if (isRunInSeparateThread()) {

                    // Start the thread as a low priority because we wish to still have user interface work fast.
                    if (mathAlgo.startMethod(Thread.MIN_PRIORITY) == false) {
                        MipavUtil.displayError("A thread is already running on this object");
                    }
                } else {
                    mathAlgo.run();
                }

            } catch (OutOfMemoryError x) {
                System.gc();
                MipavUtil.displayError("Dialog RGBConcat: unable to allocate enough memory");

                return;
            }

        } // else displayLoc == REPLACE
    }

    /**
     * Store the result image in the script runner's image table now that the action execution is finished.
     */
    protected void doPostAlgorithmActions() {

        if (displayLoc == NEW) {
            AlgorithmParameters.storeImageInRunner(getResultImage());
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void setGUIFromParams() {
        imageR = scriptParameters.retrieveImage("red_image");
        parentFrame = imageR.getParentFrame();
        setGreenImage(scriptParameters.retrieveImage("green_image"));
        setBlueImage(scriptParameters.retrieveImage("blue_image"));

        if (scriptParameters.getParams().getBoolean(AlgorithmParameters.DO_OUTPUT_NEW_IMAGE)) {
            setDisplayLocNew();
        } else {
            setDisplayLocReplace();
        }

        setRemapMode(scriptParameters.getParams().getBoolean("do_remap_values"));
    }

    /**
     * {@inheritDoc}
     */
    protected void storeParamsFromGUI() throws ParserException {
        scriptParameters.storeImage(imageR, "red_image");
        scriptParameters.storeImage(imageG, "green_image");
        scriptParameters.storeImage(imageB, "blue_image");

        scriptParameters.storeOutputImageParams(getResultImage(), (displayLoc == NEW));

        scriptParameters.getParams().put(ParameterFactory.newParameter("do_remap_values", remapMode));
    }

    /**
     * Builds a list of images to register to the template image.
     *
     * @param  channel  RED, GREEN, or BLUE
     */
    private void buildComboBoxImage(int channel) {
        int j;
        int count = 0;
        ViewUserInterface UI;
        Enumeration names;

        UI = ViewUserInterface.getReference();
        names = UI.getRegisteredImageNames();

        // decide now which comboBox to build
        // default to RED
        JComboBox comboBox = comboBoxImageRed;

        if (channel == GREEN) {
            comboBox = comboBoxImageGreen;
        } else if (channel == BLUE) {
            comboBox = comboBoxImageBlue;
        }

        // Add images from user interface that have the same exact dimensionality
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            ModelImage img = UI.getRegisteredImageByName(name);

            if (UI.getFrameContainingImage(img) != null) {

                if ((imageR.getNDims() == 2) && (img.getNDims() == 2)) {

                    for (j = 0, count = 0; j < 2; j++) {

                        if ((imageR.getExtents()[j] == img.getExtents()[j]) && (img.isColorImage() == false)) {
                            count++;
                        }
                    }

                    if (count == imageR.getNDims()) {
                        comboBox.addItem(name);

                        if (name.equals(imageR.getImageName())) {
                            comboBox.setSelectedItem(name);
                        }
                    }
                } else if ((imageR.getNDims() == 3) && (img.getNDims() == 3)) {

                    for (j = 0, count = 0; j < 3; j++) {

                        if ((imageR.getExtents()[j] == img.getExtents()[j]) && (img.isColorImage() == false)) {
                            count++;
                        }
                    }

                    if (count == imageR.getNDims()) {
                        comboBox.addItem(name);

                        if (name.equals(imageR.getImageName())) {
                            comboBox.setSelectedItem(name);
                        }
                    }
                } else if ((imageR.getNDims() == 4) && (img.getNDims() == 4)) {

                    for (j = 0, count = 0; j < 4; j++) {

                        if ((imageR.getExtents()[j] == img.getExtents()[j]) && (img.isColorImage() == false)) {
                            count++;
                        }
                    }

                    if (count == imageR.getNDims()) {
                        comboBox.addItem(name);

                        if (name.equals(imageR.getImageName())) {
                            comboBox.setSelectedItem(name);
                        }
                    }
                } // end if dimensions
            }
        } // end while

        if (comboBox.getItemCount() == 0) {
            MipavUtil.displayError("No other images to operate upon!");
        }

    } // end buildComboBoxImage()

    /**
     * Sets up the GUI (panels, buttons, etc) and displays it on the screen.
     */
    private void init() {

        setForeground(Color.black);
        setTitle("Concatenate -> RGB");

        JPanel mainPanel;
        mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        mainPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        comboBoxImageRed = new JComboBox();
        comboBoxImageRed.setFont(serif12);
        comboBoxImageRed.setBackground(Color.white);
        comboBoxImageRed.addItem("blank");

        comboBoxImageGreen = new JComboBox();
        comboBoxImageGreen.setFont(serif12);
        comboBoxImageGreen.setBackground(Color.white);
        comboBoxImageGreen.addItem("blank");

        comboBoxImageBlue = new JComboBox();
        comboBoxImageBlue.setFont(serif12);
        comboBoxImageBlue.setBackground(Color.white);
        comboBoxImageBlue.addItem("blank");

        gbc.gridx = 0;
        gbc.gridy = 0;

        JPanel inputPanel = new JPanel(new GridLayout(4, 2));
        inputPanel.setForeground(Color.black);
        inputPanel.setBorder(buildTitledBorder("Images"));
        mainPanel.add(inputPanel, gbc);

        JLabel labelImageRed = new JLabel("Image (red)");
        labelImageRed.setForeground(Color.black);
        labelImageRed.setFont(serif12);
        inputPanel.add(labelImageRed);

        buildComboBoxImage(RED);
        comboBoxImageRed.addItemListener(this);
        inputPanel.add(comboBoxImageRed);

        JLabel labelImageGreen = new JLabel("Image (green)");
        labelImageGreen.setForeground(Color.black);
        labelImageGreen.setFont(serif12);
        inputPanel.add(labelImageGreen);

        buildComboBoxImage(GREEN);
        comboBoxImageGreen.addItemListener(this);

        inputPanel.add(comboBoxImageGreen);

        JLabel labelImageBlue = new JLabel("Image (blue)");
        labelImageBlue.setForeground(Color.black);
        labelImageBlue.setFont(serif12);
        inputPanel.add(labelImageBlue);

        buildComboBoxImage(BLUE);
        comboBoxImageBlue.addItemListener(this);
        inputPanel.add(comboBoxImageBlue);

        cBoxRemap = new JCheckBox("Remap data (0-255)", true);
        cBoxRemap.setFont(serif12);
        inputPanel.add(cBoxRemap);

        gbc.gridx = 0;
        gbc.gridy = 2;
        destinationPanel = new JPanel(new BorderLayout());
        destinationPanel.setForeground(Color.black);
        destinationPanel.setBorder(buildTitledBorder("Destination"));
        mainPanel.add(destinationPanel, gbc);

        destinationGroup = new ButtonGroup();
        newImage = new JRadioButton("New image", true);
        newImage.setFont(serif12);
        destinationGroup.add(newImage);
        destinationPanel.add(newImage, BorderLayout.NORTH);

        replaceImage = new JRadioButton("Replace red image", false);
        replaceImage.setFont(serif12);
        destinationGroup.add(replaceImage);
        destinationPanel.add(replaceImage, BorderLayout.CENTER);

        // Only if the image is unlocked can it be replaced.
        if (imageR.getLockStatus() == ModelStorageBase.UNLOCKED) {
            replaceImage.setEnabled(true);
        } else {
            replaceImage.setEnabled(false);
        }


        JPanel buttonPanel = new JPanel();

        /*
         * buildOKButton(); buttonPanel.add(OKButton); buildCancelButton(); buttonPanel.add(cancelButton);
         */
        buttonPanel.add(buildButtons());

        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

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
        String tmpStr;
        blank = new ModelImage(ModelImage.SHORT, imageR.getExtents(), makeImageName(imageR.getImageName(), ""));

        if (replaceImage.isSelected()) {
            displayLoc = REPLACE;
        } else if (newImage.isSelected()) {
            displayLoc = NEW;
        }

        if (cBoxRemap.isSelected()) {
            remapMode = true;
        } else {
            remapMode = false;
        }

        Enumeration names = ViewUserInterface.getReference().getRegisteredImageNames();

        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            ModelImage img = ViewUserInterface.getReference().getRegisteredImageByName(name);

            tmpStr = (String) comboBoxImageRed.getSelectedItem();

            if (tmpStr.equals("blank")) {
                imageR = blank;
            } else if (tmpStr.equals(name)) {
                imageR = img;
            }

            tmpStr = (String) comboBoxImageGreen.getSelectedItem();

            if (tmpStr.equals("blank")) {
                imageG = blank;
            } else if (tmpStr.equals(name)) {
                imageG = img;
            }

            tmpStr = (String) comboBoxImageBlue.getSelectedItem();

            if (tmpStr.equals("blank")) {
                imageB = blank;
            } else if (tmpStr.equals(name)) {
                imageB = img;
            }
        }

        return true;
    }
}
