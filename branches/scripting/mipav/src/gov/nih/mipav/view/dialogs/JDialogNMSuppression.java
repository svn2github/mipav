package gov.nih.mipav.view.dialogs;


import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.algorithms.filters.*;
import gov.nih.mipav.model.file.*;
import gov.nih.mipav.model.scripting.*;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;
import gov.nih.mipav.view.components.*;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import javax.swing.*;


/**
 * Dialog to get user input, then call the algorithm. The user is able to control the degree of blurring in all
 * dimensions and indicate if a correction factor be applied to the z-dimension to account for differing resolutions
 * between the xy resolutions (intra-plane) and the z resolution (inter-plane). The user has the option to generate a
 * new image or replace the source image. User can indicate whether to have algorithm applied to whole image or to the
 * VOI regions. Algorithms are executed in their own thread.
 *
 * @author  Matthew J. McAuliffe, Ph.D.
 * @see     AlgorithmNMSuppression
 */
public class JDialogNMSuppression extends JDialogScriptableBase implements AlgorithmInterface {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = 3261350233947714913L;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** DOCUMENT ME! */
    private ModelImage image; // source image

    /** DOCUMENT ME! */
    private boolean image25D = false;

    /** DOCUMENT ME! */
    private JCheckBox image25DCheckbox;

    /** DOCUMENT ME! */
    private AlgorithmNMSuppression nmSuppressionAlgo;

    /** DOCUMENT ME! */
    private ModelImage resultImage = null; // result image

    /** DOCUMENT ME! */
    private String[] titles;

    /** DOCUMENT ME! */
    private ViewUserInterface userInterface;
    
    private JPanelAlgorithmOutputOptions outputPanel;
    
    private JPanelSigmas sigmaPanel;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Empty constructor needed for dynamic instantiation (used during scripting).
     */
    public JDialogNMSuppression() { }

    /**
     * Creates a new JDialogNMSuppression object.
     *
     * @param  theParentFrame  Parent frame.
     * @param  im              Source image.
     */
    public JDialogNMSuppression(Frame theParentFrame, ModelImage im) {
        super(theParentFrame, false);
        userInterface = ViewUserInterface.getReference();
        image = im;
        init();
        loadDefaults();
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Closes dialog box when the OK button is pressed, sets variables and calls algorithm.
     *
     * @param  event  Event that triggers function.
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
            MipavUtil.showHelp("10021");
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
        int i;

        if (algorithm instanceof AlgorithmNMSuppression) {
            image.clearMask();

            if ((nmSuppressionAlgo.isCompleted() == true) && (resultImage != null)) {

                updateFileInfo(image, resultImage);
                resultImage.clearMask();

                // The algorithm has completed and produced a new image to be displayed.
                try {
                    new ViewJFrameImage(resultImage, null, new Dimension(610, 200));
                } catch (OutOfMemoryError error) {
                    MipavUtil.displayError("Out of memory: unable to open new frame");
                }
            } else if (resultImage == null) {

                // These next lines set the titles in all frames where the source image is displayed to
                // image name so as to indicate that the image is now unlocked!
                // The image frames are enabled and then registed to the userinterface.
                Vector imageFrames = image.getImageFrameVector();

                for (i = 0; i < imageFrames.size(); i++) {
                    ((Frame) (imageFrames.elementAt(i))).setTitle(titles[i]);
                    ((Frame) (imageFrames.elementAt(i))).setEnabled(true);

                    if (((Frame) (imageFrames.elementAt(i))) != parentFrame) {
                        userInterface.registerFrame((Frame) (imageFrames.elementAt(i)));
                    }
                }

                if (parentFrame != null) {
                    userInterface.registerFrame(parentFrame);
                }

                image.notifyImageDisplayListeners(null, true);
            } else if (resultImage != null) {

                // algorithm failed but result image still has garbage
                resultImage.disposeLocal(); // clean up memory
                resultImage = null;
            }
        }

        if (algorithm.isCompleted()) {
            insertScriptLine();
        }

        saveDefaults();

        nmSuppressionAlgo.finalize();
        nmSuppressionAlgo = null;
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
     * Store the parameters from the dialog to record the execution of this algorithm.
     * 
     * @throws  ParserException  If there is a problem creating one of the new parameters.
     */
    protected void storeParamsFromGUI() throws ParserException {
        scriptParameters.storeInputImage(image);
        scriptParameters.storeOutputImageParams(resultImage, outputPanel.isOutputNewImageSet());

        scriptParameters.storeProcessingOptions(outputPanel.isProcessWholeImageSet(), image25D);
        scriptParameters.storeSigmas(sigmaPanel);
    }

    // *******************************************************************
    // ************************* Item Events ****************************
    // *******************************************************************

    /**
     * itemStateChanged - method to handle item events.
     *
     * @param  event  event that cause the method to fire
     */
    public void itemStateChanged(ItemEvent event) {
        Object source = event.getSource();

        if (source == image25DCheckbox) {
            sigmaPanel.enable3DComponents(!image25DCheckbox.isSelected());
        }
    }

    /**
     * Loads the default settings from Preferences to set up the dialog.
     */
    public void loadDefaults() {
        String defaultsString = Preferences.getDialogDefaults(getDialogName());

        if (defaultsString != null) {

            try {
                StringTokenizer st = new StringTokenizer(defaultsString, ",");

                sigmaPanel.setSigmaX(MipavUtil.getFloat(st));
                sigmaPanel.setSigmaY(MipavUtil.getFloat(st));
                sigmaPanel.setSigmaZ(MipavUtil.getFloat(st));
                
                outputPanel.setOutputNewImage(MipavUtil.getBoolean(st));

                outputPanel.setProcessWholeImage(MipavUtil.getBoolean(st));

                sigmaPanel.enableResolutionCorrection(MipavUtil.getBoolean(st));
                image25DCheckbox.setSelected(MipavUtil.getBoolean(st));
            } catch (NoSuchElementException nsee) {
                return;
            } catch (Exception ex) {

                // since there was a problem parsing the defaults string, start over with the original defaults
                System.out.println("Resetting defaults for dialog: " + getDialogName());
                Preferences.removeProperty(getDialogName());
                ex.printStackTrace();
            }
        }
    }

    /**
     * Saves the default settings into the Preferences file.
     */
    public void saveDefaults() {
        String delim = ",";
        
        String defaultsString = sigmaPanel.getUnnormalized3DSigmas()[0] + delim;
        defaultsString += sigmaPanel.getUnnormalized3DSigmas()[1] + delim;
        defaultsString += sigmaPanel.getUnnormalized3DSigmas()[2] + delim;
        defaultsString += outputPanel.isOutputNewImageSet() + delim;
        defaultsString += outputPanel.isProcessWholeImageSet() + delim;
        defaultsString += sigmaPanel.isResolutionCorrectionEnabled() + delim;
        defaultsString += image25DCheckbox.isSelected();

        Preferences.saveDialogDefaults(getDialogName(), defaultsString);
    }
    
    /**
     * Perform any actions required after the running of the algorithm is complete.
     */
    protected void doPostAlgorithmActions() {
        if (outputPanel.isOutputNewImageSet()) {
            AlgorithmParameters.storeImageInRunner(getResultImage());
        }
    }

    /**
     * Set up the dialog GUI based on the parameters before running the algorithm as part of a script.
     */
    protected void setGUIFromParams() {
        image = scriptParameters.retrieveInputImage();
        userInterface = ViewUserInterface.getReference();
        parentFrame = image.getParentFrame();

        outputPanel = new JPanelAlgorithmOutputOptions(image);
        sigmaPanel = new JPanelSigmas(image);
        
        scriptParameters.setOutputOptionsGUI(outputPanel);
        setImage25D(scriptParameters.getParams().getBoolean(AlgorithmParameters.DO_PROCESS_3D_AS_25D));
        scriptParameters.setSigmasGUI(sigmaPanel);
    }

    /**
     * Accessor that sets the slicing flag.
     *
     * @param  flag  <code>true</code> indicates slices should be blurred independently.
     */
    public void setImage25D(boolean flag) {
        image25D = flag;
    }

    /**
     * Once all the necessary variables are set, call the Gaussian Blur algorithm based on what type of image this is
     * and whether or not there is a separate destination image.
     */
    protected void callAlgorithm() {
        String name = makeImageName(image.getImageName(), "_nmsuppression");

        if (image.getNDims() == 2) { // source image is 2D

            int[] destExtents = new int[2];
            destExtents[0] = image.getExtents()[0]; // X dim
            destExtents[1] = image.getExtents()[1]; // Y dim

            float[] sigmas = sigmaPanel.getNormalizedSigmas();

            if (outputPanel.isOutputNewImageSet()) {

                try {

                    // Make result image of float type
                    resultImage = new ModelImage(ModelImage.FLOAT, destExtents, name, userInterface);

                    // resultImage = (ModelImage)image.clone();
                    resultImage.setImageName(name);

                    if ((resultImage.getFileInfo()[0]).getFileFormat() == FileBase.DICOM) {
                        ((FileInfoDicom) (resultImage.getFileInfo(0))).setValue("0002,0002",
                                                                                "1.2.840.10008.5.1.4.1.1.7 ", 26); // Secondary Capture SOP UID
                        ((FileInfoDicom) (resultImage.getFileInfo(0))).setValue("0008,0016",
                                                                                "1.2.840.10008.5.1.4.1.1.7 ", 26);
                        ((FileInfoDicom) (resultImage.getFileInfo(0))).setValue("0002,0012", "1.2.840.34379.17", 16); // bogus Implementation UID made up by Matt
                        ((FileInfoDicom) (resultImage.getFileInfo(0))).setValue("0002,0013", "MIPAV--NIH", 10); //
                    }

                    // Make algorithm
                    nmSuppressionAlgo = new AlgorithmNMSuppression(resultImage, image, sigmas, outputPanel.isProcessWholeImageSet(), false);

                    // This is very important. Adding this object as a listener allows the algorithm to
                    // notify this object when it has completed of failed. See algorithm performed event.
                    // This is made possible by implementing AlgorithmedPerformed interface
                    nmSuppressionAlgo.addListener(this);

                    // Hide dialog
                    setVisible(false);

                    if (isRunInSeparateThread()) {

                        // Start the thread as a low priority because we wish to still have user interface work fast.
                        if (nmSuppressionAlgo.startMethod(Thread.MIN_PRIORITY) == false) {
                            MipavUtil.displayError("A thread is already running on this object");
                        }
                    } else {
                        if (!userInterface.isAppFrameVisible()) {
                            nmSuppressionAlgo.setProgressBarVisible(false);
                        }

                        nmSuppressionAlgo.run();
                    }

                } catch (OutOfMemoryError x) {
                    MipavUtil.displayError("Dialog NMSuppression: unable to allocate enough memory");

                    if (resultImage != null) {
                        resultImage.disposeLocal(); // Clean up memory of result image
                        resultImage = null;
                    }

                    return;
                }
            } else {

                try {

                    // No need to make new image space because the user has choosen to replace the source image
                    // Make the algorithm class
                    nmSuppressionAlgo = new AlgorithmNMSuppression(image, sigmas, outputPanel.isProcessWholeImageSet(), false);

                    // This is very important. Adding this object as a listener allows the algorithm to
                    // notify this object when it has completed of failed. See algorithm performed event.
                    // This is made possible by implementing AlgorithmedPerformed interface
                    nmSuppressionAlgo.addListener(this);

                    // Hide the dialog since the algorithm is about to run.
                    setVisible(false);

                    // These next lines set the titles in all frames where the source image is displayed to
                    // "locked - " image name so as to indicate that the image is now read/write locked!
                    // The image frames are disabled and then unregisted from the userinterface until the
                    // algorithm has completed.
                    Vector imageFrames = image.getImageFrameVector();
                    titles = new String[imageFrames.size()];

                    for (int i = 0; i < imageFrames.size(); i++) {
                        titles[i] = ((Frame) (imageFrames.elementAt(i))).getTitle();
                        ((Frame) (imageFrames.elementAt(i))).setTitle("Locked: " + titles[i]);
                        ((Frame) (imageFrames.elementAt(i))).setEnabled(false);
                        userInterface.unregisterFrame((Frame) (imageFrames.elementAt(i)));
                    }

                    if (isRunInSeparateThread()) {

                        // Start the thread as a low priority because we wish to still have user interface work fast.
                        if (nmSuppressionAlgo.startMethod(Thread.MIN_PRIORITY) == false) {
                            MipavUtil.displayError("A thread is already running on this object");
                        }
                    } else {
                        if (!userInterface.isAppFrameVisible()) {
                            nmSuppressionAlgo.setProgressBarVisible(false);
                        }

                        nmSuppressionAlgo.run();
                    }

                } catch (OutOfMemoryError x) {
                    MipavUtil.displayError("Dialog NMSuppression: unable to allocate enough memory");

                    return;
                }
            }
        } else if (image.getNDims() == 3) {
            int[] destExtents = new int[3];
            destExtents[0] = image.getExtents()[0];
            destExtents[1] = image.getExtents()[1];
            destExtents[2] = image.getExtents()[2];

            float[] sigmas = sigmaPanel.getNormalizedSigmas();

            if (outputPanel.isOutputNewImageSet()) {

                try {

                    // Make result image of float type
                    resultImage = new ModelImage(ModelImage.FLOAT, destExtents, name, userInterface);

                    // resultImage = (ModelImage)image.clone();
                    resultImage.setImageName(name);

                    if ((resultImage.getFileInfo()[0]).getFileFormat() == FileBase.DICOM) {

                        for (int i = 0; i < resultImage.getExtents()[2]; i++) {
                            ((FileInfoDicom) (resultImage.getFileInfo(i))).setValue("0002,0002",
                                                                                    "1.2.840.10008.5.1.4.1.1.7 ", 26); // Secondary Capture SOP UID
                            ((FileInfoDicom) (resultImage.getFileInfo(i))).setValue("0008,0016",
                                                                                    "1.2.840.10008.5.1.4.1.1.7 ", 26);
                            ((FileInfoDicom) (resultImage.getFileInfo(i))).setValue("0002,0012", "1.2.840.34379.17",
                                                                                    16); // bogus Implementation UID
                                                                                         // made up by Matt
                            ((FileInfoDicom) (resultImage.getFileInfo(i))).setValue("0002,0013", "MIPAV--NIH", 10); //
                        }
                    }

                    // Make algorithm
                    nmSuppressionAlgo = new AlgorithmNMSuppression(resultImage, image, sigmas, outputPanel.isProcessWholeImageSet(), image25D);

                    // This is very important. Adding this object as a listener allows the algorithm to
                    // notify this object when it has completed of failed. See algorithm performed event.
                    // This is made possible by implementing AlgorithmedPerformed interface
                    nmSuppressionAlgo.addListener(this);

                    // Hide dialog
                    setVisible(false);

                    if (isRunInSeparateThread()) {

                        // Start the thread as a low priority because we wish to still have user interface work fast.
                        if (nmSuppressionAlgo.startMethod(Thread.MIN_PRIORITY) == false) {
                            MipavUtil.displayError("A thread is already running on this object");
                        }
                    } else {
                        if (!userInterface.isAppFrameVisible()) {
                            nmSuppressionAlgo.setProgressBarVisible(false);
                        }

                        nmSuppressionAlgo.run();
                    }

                } catch (OutOfMemoryError x) {
                    MipavUtil.displayError("Dialog NMSuppression: unable to allocate enough memory");

                    if (resultImage != null) {
                        resultImage.disposeLocal(); // Clean up image memory
                        resultImage = null;
                    }

                    return;
                }
            } else {

                try {

                    // Make algorithm
                    nmSuppressionAlgo = new AlgorithmNMSuppression(image, sigmas, outputPanel.isProcessWholeImageSet(), image25D);

                    // This is very important. Adding this object as a listener allows the algorithm to
                    // notify this object when it has completed of failed. See algorithm performed event.
                    // This is made possible by implementing AlgorithmedPerformed interface
                    nmSuppressionAlgo.addListener(this);

                    // Hide dialog
                    setVisible(false);

                    // These next lines set the titles in all frames where the source image is displayed to
                    // "locked - " image name so as to indicate that the image is now read/write locked!
                    // The image frames are disabled and then unregisted from the userinterface until the
                    // algorithm has completed.
                    Vector imageFrames = image.getImageFrameVector();
                    titles = new String[imageFrames.size()];

                    for (int i = 0; i < imageFrames.size(); i++) {
                        titles[i] = ((Frame) (imageFrames.elementAt(i))).getTitle();
                        ((Frame) (imageFrames.elementAt(i))).setTitle("Locked: " + titles[i]);
                        ((Frame) (imageFrames.elementAt(i))).setEnabled(false);
                        userInterface.unregisterFrame((Frame) (imageFrames.elementAt(i)));
                    }

                    if (isRunInSeparateThread()) {

                        // Start the thread as a low priority because we wish to still have user interface work fast.
                        if (nmSuppressionAlgo.startMethod(Thread.MIN_PRIORITY) == false) {
                            MipavUtil.displayError("A thread is already running on this object");
                        }
                    } else {
                        if (!userInterface.isAppFrameVisible()) {
                            nmSuppressionAlgo.setProgressBarVisible(false);
                        }

                        nmSuppressionAlgo.run();
                    }

                } catch (OutOfMemoryError x) {
                    MipavUtil.displayError("Dialog NMSuppression: unable to allocate enough memory");

                    return;
                }
            }
        }

    }

    /**
     * Initializes the GUI by creating the components, placing them in the dialog, and displaying them.
     */
    private void init() {

        setForeground(Color.black);

        getContentPane().setLayout(new BorderLayout());
        setTitle("Non-maximum suppression");

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
        
        sigmaPanel = new JPanelSigmas(image);
        mainPanel.add(sigmaPanel, gbc);

        JPanel optionPanel = new JPanel(new BorderLayout());
        optionPanel.setBorder(buildTitledBorder("Options"));
        image25DCheckbox = new JCheckBox("Process each slice independently (2.5D)");
        image25DCheckbox.setFont(serif12);
        optionPanel.add(image25DCheckbox, BorderLayout.SOUTH);
        image25DCheckbox.setSelected(false);
        image25DCheckbox.addItemListener(this);

        if (image.getNDims() != 3) {
            image25DCheckbox.setEnabled(false);
        }
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        mainPanel.add(optionPanel, gbc);
        
        outputPanel = new JPanelAlgorithmOutputOptions(image);
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(outputPanel, gbc);

        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(buildButtons(), BorderLayout.SOUTH);
        pack();
        setResizable(true);
        setVisible(true);

        System.gc();
    }

    /**
     * Use the GUI results to set up the variables needed to run the algorithm.
     *
     * @return  <code>true</code> if parameters set successfully, <code>false</code> otherwise.
     */
    private boolean setVariables() {
        System.gc();

        if (image25DCheckbox.isSelected()) {
            image25D = true;
        }

        if (!sigmaPanel.testSigmaValues()) {
            return false;
        }

        return true;
    }

}
