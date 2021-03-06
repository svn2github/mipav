package gov.nih.mipav.view.dialogs;


import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.algorithms.filters.*;
import gov.nih.mipav.model.file.*;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import javax.swing.*;


/**
 * Dialog to get user input, then call the algorithm. The user is able to control the degree of blurring in all
 * dimensions and indicate if a correction factor be applied to the z-dimension to account for differing resolutions
 * between the xy resolutions (intra-plane) and the z resolution (inter-plane). The user has the option to generate a
 * new image or replace the source image. In addition the user can indicate if he/she wishes to have the algorithm
 * applied to whole image or to the VOI regions. It should be noted that the algorithms are executed in their own
 * thread.
 *
 * @see  AlgorithmGaussianBlurITK
 */
public class JDialogGaussianBlurITK extends JDialogBase
        implements AlgorithmInterface, ScriptableInterface, DialogDefaultsInterface {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = -4057161307906161677L;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** DOCUMENT ME! */
    public long start, end;

    /** DOCUMENT ME! */
    private boolean blue = false;

    /** DOCUMENT ME! */
    private JCheckBox blueCheckbox;

    /** DOCUMENT ME! */
    private ButtonGroup destinationGroup;

    /** DOCUMENT ME! */
    private JPanel destinationPanel;

    /** DOCUMENT ME! */
    private int displayLoc; // Flag indicating if a new image is to be generated

    /** DOCUMENT ME! */
    private AlgorithmGaussianBlurITK gaussianBlurAlgo;

    /** DOCUMENT ME! */
    private boolean green = false;

    /** DOCUMENT ME! */
    private JCheckBox greenCheckbox;

    /** DOCUMENT ME! */
    private ModelImage image; // source image

    /** false = apply algorithm only to VOI regions. */
    private boolean image25D = false; // flag indicating if slices should be blurred independently

    /** DOCUMENT ME! */
    private JCheckBox image25DCheckbox;

    /** DOCUMENT ME! */
    private ButtonGroup imageVOIGroup;

    /** DOCUMENT ME! */
    private JPanel imageVOIPanel;

    /** DOCUMENT ME! */
    private JLabel labelCorrected;

    /** DOCUMENT ME! */
    private JLabel labelGaussX;

    /** DOCUMENT ME! */
    private JLabel labelGaussY;

    /** DOCUMENT ME! */
    private JLabel labelGaussZ;

    /** DOCUMENT ME! */
    private JRadioButton newImage;

    /** DOCUMENT ME! */
    private float normFactor = 1; // normalization factor to adjust for resolution

    /** DOCUMENT ME! */
    private boolean red = false; // set to true if requested for ARGB images

    /** DOCUMENT ME! */
    private JCheckBox redCheckbox;

    /** DOCUMENT ME! */
    private JRadioButton replaceImage;

    /** difference between x,y resolutions (in plane) and z resolution (between planes). */
    private JCheckBox resolutionCheckbox;

    /** DOCUMENT ME! */
    private ModelImage resultImage = null; // result image

    /** DOCUMENT ME! */
    private JPanel scalePanel;

    /** DOCUMENT ME! */
    private float scaleX;

    /** DOCUMENT ME! */
    private float scaleY;

    /** DOCUMENT ME! */
    private float scaleZ;

    /** DOCUMENT ME! */
    private JTextField textGaussX;

    /** DOCUMENT ME! */
    private JTextField textGaussY;

    /** DOCUMENT ME! */
    private JTextField textGaussZ;

    /** DOCUMENT ME! */
    private String[] titles;

    /** DOCUMENT ME! */
    private ViewUserInterface userInterface;

    /** or if the source image is to be replaced. */
    private boolean useVOI = false;

    /** DOCUMENT ME! */
    private JRadioButton VOIRegions;

    /** DOCUMENT ME! */
    private JRadioButton wholeImage;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Empty constructor needed for dynamic instantiation.
     */
    public JDialogGaussianBlurITK() { }

    /**
     * Creates a new JDialogGaussianBlurITK object.
     *
     * @param  theParentFrame  Parent frame.
     * @param  im              Source image.
     */
    public JDialogGaussianBlurITK(Frame theParentFrame, ModelImage im) {
        super(theParentFrame, false);
        image = im;
        userInterface = ((ViewJFrameBase) (parentFrame)).getUserInterface();
        init();
        loadDefaults();
        setVisible(true);
    }

    /**
     * Used primarily for the script to store variables and run the algorithm. No actual dialog will appear but the set
     * up info and result image will be stored here.
     *
     * @param  UI  The user interface, needed to create the image frame.
     * @param  im  Source image.
     */
    public JDialogGaussianBlurITK(ViewUserInterface UI, ModelImage im) {
        super();
        userInterface = UI;
        image = im;
        parentFrame = image.getParentFrame();
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Closes dialog box when the OK button is pressed and calls the algorithm.
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
            MipavUtil.showHelp("10009");
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

        ViewJFrameImage imageFrame = null;

        if (Preferences.is(Preferences.PREF_SAVE_DEFAULTS) && (this.getOwner() != null) && !isScriptRunning()) {
            saveDefaults();
        }

        end = System.currentTimeMillis();
        Preferences.debug("Gaussian Elapsed: " + (end - start));
        image.clearMask();

        if ((gaussianBlurAlgo.isCompleted() == true) && (resultImage != null)) {

            // The algorithm has completed and produced a new image to be displayed.
            if (resultImage.isColorImage()) {
                updateFileInfo(image, resultImage);
            }

            resultImage.clearMask();

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
            Vector imageFrames = image.getImageFrameVector();

            for (int i = 0; i < imageFrames.size(); i++) {
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
            System.gc();

        }

        insertScriptLine(algorithm);

        if (gaussianBlurAlgo != null) {
            gaussianBlurAlgo.finalize();
            gaussianBlurAlgo = null;
        }

        dispose();
    }

    /**
     * When the user clicks the mouse out of a text field, resets the neccessary variables.
     *
     * @param  event  event that triggers this function
     */
    public void focusLost(FocusEvent event) {
        Object source = event.getSource();
        JTextField field;
        String text;
        float tempNum;

        if (source == textGaussZ) {
            field = (JTextField) source;
            text = field.getText();

            if (resolutionCheckbox.isSelected()) {
                tempNum = normFactor * Float.valueOf(textGaussZ.getText()).floatValue();
                labelCorrected.setText("      Corrected scale = " + makeString(tempNum, 3));
            } else {
                labelCorrected.setText(" ");
            }
        }
    }

    /**
     * Construct a delimited string that contains the parameters to this algorithm.
     *
     * @param   delim  the parameter delimiter (defaults to " " if empty)
     *
     * @return  the parameter string
     */
    public String getParameterString(String delim) {

        if (delim.equals("")) {
            delim = " ";
        }

        String str = new String();
        str += useVOI + delim;
        str += image25D + delim;
        str += scaleX + delim;
        str += scaleY + delim;
        str += textGaussZ.getText() + delim;
        str += red + delim;
        str += green + delim;
        str += blue;

        return str;
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

                // check to see if the match image is already in the ImgTable
                if (userInterface.getScriptDialog().getImgTableVar(image.getImageName()) == null) {

                    if (userInterface.getScriptDialog().getActiveImgTableVar(image.getImageName()) == null) {
                        userInterface.getScriptDialog().putActiveVar(image.getImageName());
                    }
                }

                String line = "GaussianBlurITK " + userInterface.getScriptDialog().getVar(image.getImageName()) + " ";

                if (displayLoc == NEW) {
                    userInterface.getScriptDialog().putVar(resultImage.getImageName());
                    line += userInterface.getScriptDialog().getVar(resultImage.getImageName()) + " " +
                            getParameterString(" ") + "\n";
                } else {
                    line += userInterface.getScriptDialog().getVar(image.getImageName()) + " " +
                            getParameterString(" ") + "\n";

                }

                userInterface.getScriptDialog().append(line);
            }
        }
    }

    // *******************************************************************
    // ************************* Item Events ****************************
    // *******************************************************************

    /**
     * Changes labels based on whether or not check box is checked.
     *
     * @param  event  event that cause the method to fire
     */
    public void itemStateChanged(ItemEvent event) {
        Object source = event.getSource();
        float tempNum;

        if (source == resolutionCheckbox) {

            if (resolutionCheckbox.isSelected()) {
                tempNum = normFactor * Float.valueOf(textGaussZ.getText()).floatValue();
                labelCorrected.setText("      Corrected scale = " + makeString(tempNum, 3));
            } else {
                labelCorrected.setText(" ");
            }
        } else if (source == image25DCheckbox) {

            if (image25DCheckbox.isSelected()) {
                resolutionCheckbox.setEnabled(false); // Image is only 2D or 2.5D, thus this checkbox
                labelGaussZ.setEnabled(false); // is not relevent
                textGaussZ.setEnabled(false);
                labelCorrected.setEnabled(false);
            } else {
                resolutionCheckbox.setEnabled(true);
                labelGaussZ.setEnabled(true);
                textGaussZ.setEnabled(true);
                labelCorrected.setEnabled(true);
            }
        }
    }

    /**
     * Loads the default settings from Preferences to set up the dialog.
     */
    public void loadDefaults() {
        String defaultsString = Preferences.getDialogDefaults(getDialogName());

        if ((defaultsString != null) && (VOIRegions != null)) {

            try {
                Preferences.debug(defaultsString);

                StringTokenizer st = new StringTokenizer(defaultsString, ",");

                if (MipavUtil.getBoolean(st)) {
                    wholeImage.setSelected(true);
                } else {
                    VOIRegions.setSelected(true);
                }

                image25DCheckbox.setSelected(MipavUtil.getBoolean(st));
                textGaussX.setText("" + MipavUtil.getFloat(st));
                textGaussY.setText("" + MipavUtil.getFloat(st));
                textGaussZ.setText("" + MipavUtil.getFloat(st));

                redCheckbox.setSelected(MipavUtil.getBoolean(st));
                greenCheckbox.setSelected(MipavUtil.getBoolean(st));
                blueCheckbox.setSelected(MipavUtil.getBoolean(st));

                resolutionCheckbox.setSelected(MipavUtil.getBoolean(st));

                if (MipavUtil.getBoolean(st)) {
                    newImage.setSelected(true);
                } else {
                    replaceImage.setSelected(true);
                }
            } catch (Exception ex) {

                // since there was a problem parsing the defaults string, start over with the original defaults
                Preferences.debug("Resetting defaults for dialog: " + getDialogName());
                Preferences.removeProperty(getDialogName());
            }
        }
    }

    /**
     * Saves the default settings into the Preferences file.
     */
    public void saveDefaults() {
        String defaultsString = new String(getParameterString(",") + "," + resolutionCheckbox.isSelected() + "," +
                                           newImage.isSelected());
        Preferences.saveDialogDefaults(getDialogName(), defaultsString);
    }

    /**
     * Run this algorithm from a script.
     *
     * @param   parser  the script parser we get the state from
     *
     * @throws  IllegalArgumentException  if there is something wrong with the arguments in the script
     */
    public void scriptRun(AlgorithmScriptParser parser) throws IllegalArgumentException {
        setScriptRunning(true);

        String srcImageKey = null;
        String destImageKey = null;

        try {
            srcImageKey = parser.getNextString();
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        ModelImage im = parser.getImage(srcImageKey);

        image = im;
        userInterface = image.getUserInterface();
        parentFrame = image.getParentFrame();

        // the result image
        try {
            destImageKey = parser.getNextString();
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        if (srcImageKey.equals(destImageKey)) {
            this.setDisplayLocReplace();
        } else {
            this.setDisplayLocNew();
        }

        try {
            setUseVOI(parser.getNextBoolean());
            setImage25D(parser.getNextBoolean());
            setScaleX(parser.getNextFloat());
            setScaleY(parser.getNextFloat());
            setScaleZ(parser.getNextFloat());
            setRed(parser.getNextBoolean());
            setGreen(parser.getNextBoolean());
            setBlue(parser.getNextBoolean());
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        setActiveImage(parser.isActiveImage());
        setSeparateThread(false);
        callAlgorithm();

        if (!srcImageKey.equals(destImageKey)) {
            parser.putVariable(destImageKey, getResultImage().getImageName());
        }
    }

    /**
     * Accessor that sets the color flag.
     *
     * @param  flag  <code>true</code> indicates ARG image, blue.
     */
    public void setBlue(boolean flag) {
        blue = flag;
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
     * Accessor that sets the color flag.
     *
     * @param  flag  <code>true</code> indicates ARG image, green.
     */
    public void setGreen(boolean flag) {
        green = flag;
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
     * Accessor that sets the color flag.
     *
     * @param  flag  <code>true</code> indicates ARG image, red.
     */
    public void setRed(boolean flag) {
        red = flag;
    }

    /**
     * Accessor that sets the x scale.
     *
     * @param  scale  Value to set x scale to (should be between 0.0 and 10.0).
     */
    public void setScaleX(float scale) {
        scaleX = scale;
    }

    /**
     * Accessor that sets the y scale.
     *
     * @param  scale  Value to set y scale to (should be between 0.0 and 10.0).
     */
    public void setScaleY(float scale) {
        scaleY = scale;
    }

    /**
     * Accessor that sets the z scale.
     *
     * @param  scale  Value to set z scale to (should be between 0.0 and 10.0).
     */
    public void setScaleZ(float scale) {
        scaleZ = scale;
    }

    /**
     * Accessor that sets the use VOI flag.
     *
     * @param  flag  <code>true</code> indicates the VOI is blurred, <code>false</code> indicates the whole image.
     */
    public void setUseVOI(boolean flag) {
        useVOI = flag;
    }

    /**
     * Once all the necessary variables are set, call the Gaussian Blur algorithm based on what type of image this is
     * and whether or not there is a separate destination image.
     */
    private void callAlgorithm() {
        String name = makeImageName(image.getImageName(), "_gblur");

        start = System.currentTimeMillis();

        if (image.getNDims() == 2) { // source image is 2D and kernel not separable

            float[] sigmas = new float[2];

            sigmas[0] = scaleX; // set standard deviations (sigma) in X and Y
            sigmas[1] = scaleY;

            if (displayLoc == NEW) {

                try {

                    // Make result image
                    if (image.getType() == ModelImage.ARGB) {
                        resultImage = new ModelImage(ModelImage.ARGB, image.getExtents(), name, userInterface);
                    } else if (image.getType() == ModelImage.ARGB_USHORT) {
                        resultImage = new ModelImage(ModelImage.ARGB_USHORT, image.getExtents(), name, userInterface);
                    } else if (image.getType() == ModelImage.ARGB_FLOAT) {
                        resultImage = new ModelImage(ModelImage.ARGB_FLOAT, image.getExtents(), name, userInterface);
                    } else {

                        // resultImage     = new ModelImage(ModelImage.FLOAT, destExtents, name, userInterface);
                        resultImage = (ModelImage) image.clone();
                        resultImage.setImageName(name);

                        if ((resultImage.getFileInfo()[0]).getFileFormat() == FileBase.DICOM) {
                            ((FileInfoDicom) (resultImage.getFileInfo(0))).setValue("0002,0002",
                                                                                    "1.2.840.10008.5.1.4.1.1.7 ", 26); // Secondary Capture SOP UID
                            ((FileInfoDicom) (resultImage.getFileInfo(0))).setValue("0008,0016",
                                                                                    "1.2.840.10008.5.1.4.1.1.7 ", 26);
                            ((FileInfoDicom) (resultImage.getFileInfo(0))).setValue("0002,0012", "1.2.840.34379.17",
                                                                                    16); // bogus Implementation UID
                                                                                         // made up by Matt
                            ((FileInfoDicom) (resultImage.getFileInfo(0))).setValue("0002,0013", "MIPAV--NIH", 10); //
                        }
                    }

                    // Make algorithm
                    gaussianBlurAlgo = new AlgorithmGaussianBlurITK(resultImage, image, sigmas, false);

                    // This is very important. Adding this object as a listener allows the algorithm to
                    // notify this object when it has completed of failed. See algorithm performed event.
                    // This is made possible by implementing AlgorithmedPerformed interface
                    gaussianBlurAlgo.addListener(this);
                    gaussianBlurAlgo.setRed(red);
                    gaussianBlurAlgo.setGreen(green);
                    gaussianBlurAlgo.setBlue(blue);

                    if (useVOI) {
                        gaussianBlurAlgo.setMask(image.generateVOIMask());
                    }

                    // Hide dialog
                    setVisible(false);

                    if (runInSeparateThread) {

                        // Start the thread as a low priority because we wish to still have user interface work fast.
                        if (gaussianBlurAlgo.startMethod(Thread.MIN_PRIORITY) == false) {
                            MipavUtil.displayError("A thread is already running on this object");
                        }
                    } else {

                        // gaussianBlurAlgo.setActiveImage(isActiveImage);
                        gaussianBlurAlgo.setActiveImage(isActiveImage);
                        gaussianBlurAlgo.run();
                    }
                } catch (OutOfMemoryError x) {

                    if (resultImage != null) {
                        resultImage.disposeLocal(); // Clean up memory of result image
                        resultImage = null;
                    }

                    System.gc();
                    MipavUtil.displayError("Dialog Gaussian blur: unable to allocate enough memory");

                    return;
                }
            } else {

                try {

                    // No need to make new image space because the user has choosen to replace the source image
                    // Make the algorithm class
                    gaussianBlurAlgo = new AlgorithmGaussianBlurITK(image, sigmas, false);

                    // This is very important. Adding this object as a listener allows the algorithm to
                    // notify this object when it has completed of failed. See algorithm performed event.
                    // This is made possible by implementing AlgorithmedPerformed interface
                    gaussianBlurAlgo.addListener(this);
                    gaussianBlurAlgo.setRed(red);
                    gaussianBlurAlgo.setGreen(green);
                    gaussianBlurAlgo.setBlue(blue);

                    if (useVOI) {
                        gaussianBlurAlgo.setMask(image.generateVOIMask());
                    }

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

                    if (runInSeparateThread) {

                        // Start the thread as a low priority because we wish to still have user interface work fast.
                        if (gaussianBlurAlgo.startMethod(Thread.MIN_PRIORITY) == false) {
                            MipavUtil.displayError("A thread is already running on this object");
                        }
                    } else {

                        // gaussianBlurAlgo.setActiveImage(isActiveImage);
                        gaussianBlurAlgo.setActiveImage(isActiveImage);
                        gaussianBlurAlgo.run();
                    }
                } catch (OutOfMemoryError x) {
                    System.gc();
                    MipavUtil.displayError("Dialog Gaussian blur: unable to allocate enough memory");

                    return;
                }
            }
        } else if (image.getNDims() >= 3) { // kerenl not separable

            float[] sigmas = new float[3];

            sigmas[0] = scaleX;
            sigmas[1] = scaleY;
            sigmas[2] = scaleZ; // normalized  - scaleZ * resolutionX/resolutionZ; !!!!!!!

            if (displayLoc == NEW) {

                try {

                    // Make result image
                    if (image.getType() == ModelImage.ARGB) {
                        resultImage = new ModelImage(ModelImage.ARGB, image.getExtents(), name, userInterface);
                    } else if (image.getType() == ModelImage.ARGB_USHORT) {
                        resultImage = new ModelImage(ModelImage.ARGB_USHORT, image.getExtents(), name, userInterface);
                    } else if (image.getType() == ModelImage.ARGB_FLOAT) {
                        resultImage = new ModelImage(ModelImage.ARGB_FLOAT, image.getExtents(), name, userInterface);
                    } else {

                        // resultImage     = new ModelImage(ModelImage.FLOAT, destExtents, name, userInterface);
                        resultImage = (ModelImage) image.clone();
                        resultImage.setImageName(name);

                        if ((resultImage.getFileInfo()[0]).getFileFormat() == FileBase.DICOM) {

                            for (int i = 0; i < resultImage.getExtents()[2]; i++) {
                                ((FileInfoDicom) (resultImage.getFileInfo(i))).setValue("0002,0002",
                                                                                        "1.2.840.10008.5.1.4.1.1.7 ",
                                                                                        26); // Secondary Capture SOP
                                                                                             // UID
                                ((FileInfoDicom) (resultImage.getFileInfo(i))).setValue("0008,0016",
                                                                                        "1.2.840.10008.5.1.4.1.1.7 ",
                                                                                        26);
                                ((FileInfoDicom) (resultImage.getFileInfo(i))).setValue("0002,0012", "1.2.840.34379.17",
                                                                                        16); // bogus Implementation
                                                                                             // UID made up by Matt
                                ((FileInfoDicom) (resultImage.getFileInfo(i))).setValue("0002,0013", "MIPAV--NIH", 10); //
                            }
                        }
                    }

                    // Make algorithm
                    gaussianBlurAlgo = new AlgorithmGaussianBlurITK(resultImage, image, sigmas, image25D);

                    // This is very important. Adding this object as a listener allows the algorithm to
                    // notify this object when it has completed of failed. See algorithm performed event.
                    // This is made possible by implementing AlgorithmedPerformed interface
                    gaussianBlurAlgo.addListener(this);
                    gaussianBlurAlgo.setRed(red);
                    gaussianBlurAlgo.setGreen(green);
                    gaussianBlurAlgo.setBlue(blue);

                    if (useVOI) {
                        gaussianBlurAlgo.setMask(image.generateVOIMask());
                    }

                    // Hide dialog
                    setVisible(false);

                    if (runInSeparateThread) {

                        // Start the thread as a low priority because we wish to still have user interface work fast.
                        if (gaussianBlurAlgo.startMethod(Thread.MIN_PRIORITY) == false) {
                            MipavUtil.displayError("A thread is already running on this object");
                        }
                    } else {

                        // gaussianBlurAlgo.setActiveImage(isActiveImage);
                        gaussianBlurAlgo.setActiveImage(isActiveImage);
                        gaussianBlurAlgo.run();
                    }
                } catch (OutOfMemoryError x) {

                    if (resultImage != null) {
                        resultImage.disposeLocal(); // Clean up image memory
                        resultImage = null;
                    }

                    System.gc();
                    MipavUtil.displayError("Dialog Gaussian blur: unable to allocate enough memory");

                    return;
                }
            } else {

                try {

                    // Make algorithm
                    gaussianBlurAlgo = new AlgorithmGaussianBlurITK(image, sigmas, image25D);

                    // This is very important. Adding this object as a listener allows the algorithm to
                    // notify this object when it has completed of failed. See algorithm performed event.
                    // This is made possible by implementing AlgorithmedPerformed interface
                    gaussianBlurAlgo.addListener(this);
                    gaussianBlurAlgo.setRed(red);
                    gaussianBlurAlgo.setGreen(green);
                    gaussianBlurAlgo.setBlue(blue);

                    if (useVOI) {
                        gaussianBlurAlgo.setMask(image.generateVOIMask());
                    }

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

                    if (runInSeparateThread) {

                        // Start the thread as a low priority because we wish to still have user interface work fast.
                        if (gaussianBlurAlgo.startMethod(Thread.MIN_PRIORITY) == false) {
                            MipavUtil.displayError("A thread is already running on this object");
                        }
                    } else {

                        // gaussianBlurAlgo.setActiveImage(isActiveImage);
                        gaussianBlurAlgo.setActiveImage(isActiveImage);
                        gaussianBlurAlgo.run();
                    }
                } catch (OutOfMemoryError x) {
                    System.gc();
                    MipavUtil.displayError("Dialog Gaussian blur: unable to allocate enough memory");

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

        setTitle("Gaussian Blur");
        getContentPane().setLayout(new BorderLayout());

        JPanel mainPanel;

        mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        mainPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.anchor = gbc.WEST;
        gbc.weightx = 1;
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        scalePanel = new JPanel(new GridLayout(3, 2));
        scalePanel.setForeground(Color.black);
        scalePanel.setBorder(buildTitledBorder("Scale of the Gaussian"));
        mainPanel.add(scalePanel, gbc);

        labelGaussX = createLabel("X dimension (0.0 - 10.0) ");
        scalePanel.add(labelGaussX);
        textGaussX = createTextField("1.0");
        scalePanel.add(textGaussX);

        labelGaussY = createLabel("Y dimension (0.0 - 10.0) ");
        scalePanel.add(labelGaussY);
        textGaussY = createTextField("1.0");
        scalePanel.add(textGaussY);

        labelGaussZ = createLabel("Z dimension (0.0 - 10.0) ");
        scalePanel.add(labelGaussZ);
        textGaussZ = createTextField("1.0");
        scalePanel.add(textGaussZ);

        gbc.gridx = 0;
        gbc.gridy = 1;

        JPanel resPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc2 = new GridBagConstraints();

        gbc2.gridwidth = 1;
        gbc2.gridheight = 1;
        gbc2.anchor = gbc.WEST;
        gbc2.weightx = 1;
        gbc2.insets = new Insets(3, 3, 3, 3);
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        resPanel.setBorder(buildTitledBorder("Options"));

        resolutionCheckbox = new JCheckBox("Use image resolutions to normalize Z scale");
        resolutionCheckbox.setFont(serif12);
        gbc2.gridx = 0;
        gbc2.gridy = 1;
        resPanel.add(resolutionCheckbox, gbc2);
        resolutionCheckbox.setSelected(true);

        image25DCheckbox = new JCheckBox("Process each slice independently (2.5D)");
        image25DCheckbox.setFont(serif12);
        gbc2.gridx = 0;
        gbc2.gridy = 3;
        resPanel.add(image25DCheckbox, gbc2);
        image25DCheckbox.setSelected(false);
        image25DCheckbox.addItemListener(this);

        if (image.getNDims() >= 3) { // if the source image is 3D then allow
            resolutionCheckbox.setEnabled(true); // the user to indicate if it wishes to
            resolutionCheckbox.addItemListener(this); // use the correction factor
            textGaussZ.addFocusListener(this);
            textGaussZ.setEnabled(true);

            if (image.getNDims() == 4) {
                image25DCheckbox.setEnabled(false);
            }
        } else {
            resolutionCheckbox.setEnabled(false); // Image is only 2D, thus this checkbox
            labelGaussZ.setEnabled(false); // is not relevent
            textGaussZ.setEnabled(false);
            image25DCheckbox.setEnabled(false);
        }

        if (image.getNDims() >= 3) { // Source image is 3D, thus show correction factor

            int index = image.getExtents()[2] / 2;
            float xRes = image.getFileInfo(index).getResolutions()[0];
            float zRes = image.getFileInfo(index).getResolutions()[2];

            normFactor = xRes / zRes; // Calculate correction factor
            labelCorrected = new JLabel("      Corrected scale = " +
                                        String.valueOf(normFactor * Float.valueOf(textGaussZ.getText()).floatValue()));
            labelCorrected.setForeground(Color.black);
            labelCorrected.setFont(serif12);
            gbc2.gridx = 0;
            gbc2.gridy = 2;
            resPanel.add(labelCorrected, gbc2);
        }

        mainPanel.add(resPanel, gbc);

        JPanel RGBPanel = new JPanel(new GridLayout(3, 1));

        RGBPanel.setForeground(Color.black);
        RGBPanel.setBorder(buildTitledBorder("Color channel selection"));
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(RGBPanel, gbc);

        redCheckbox = new JCheckBox("Process red channel.");
        redCheckbox.setFont(serif12);
        RGBPanel.add(redCheckbox);
        redCheckbox.setSelected(true);
        redCheckbox.addItemListener(this);

        greenCheckbox = new JCheckBox("Process green channel.");
        greenCheckbox.setFont(serif12);
        RGBPanel.add(greenCheckbox);
        greenCheckbox.setSelected(true);
        greenCheckbox.addItemListener(this);

        blueCheckbox = new JCheckBox("Process blue channel.");
        blueCheckbox.setFont(serif12);
        RGBPanel.add(blueCheckbox);
        blueCheckbox.setSelected(true);
        blueCheckbox.addItemListener(this);

        if (image.isColorImage() == false) {
            redCheckbox.setEnabled(false);
            greenCheckbox.setEnabled(false);
            blueCheckbox.setEnabled(false);
        }

        JPanel outputOptPanel = new JPanel(new GridLayout(1, 2));

        destinationPanel = new JPanel(new BorderLayout());
        destinationPanel.setForeground(Color.black);
        destinationPanel.setBorder(buildTitledBorder("Destination"));
        outputOptPanel.add(destinationPanel);
        // mainPanel.add(destinationPanel);

        destinationGroup = new ButtonGroup();
        newImage = new JRadioButton("New image", true);
        newImage.setFont(serif12);
        destinationGroup.add(newImage);
        destinationPanel.add(newImage, BorderLayout.NORTH);

        replaceImage = new JRadioButton("Replace image", false);
        replaceImage.setFont(serif12);
        destinationGroup.add(replaceImage);
        destinationPanel.add(replaceImage, BorderLayout.CENTER);

        // Only if the image is unlocked can it be replaced.
        if (image.getLockStatus() == ModelStorageBase.UNLOCKED) {
            replaceImage.setEnabled(true);
        } else {
            replaceImage.setEnabled(false);
        }

        imageVOIPanel = new JPanel();
        imageVOIPanel.setLayout(new BorderLayout());
        imageVOIPanel.setForeground(Color.black);
        imageVOIPanel.setBorder(buildTitledBorder("Blur"));
        outputOptPanel.add(imageVOIPanel);

        imageVOIGroup = new ButtonGroup();
        wholeImage = new JRadioButton("Whole image", true);
        wholeImage.setFont(serif12);
        imageVOIGroup.add(wholeImage);
        imageVOIPanel.add(wholeImage, BorderLayout.NORTH);

        VOIRegions = new JRadioButton("VOI region(s)", false);
        VOIRegions.setFont(serif12);
        imageVOIGroup.add(VOIRegions);
        imageVOIPanel.add(VOIRegions, BorderLayout.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 3;
        mainPanel.add(outputOptPanel, gbc);

        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(buildButtons(), BorderLayout.SOUTH);
        pack();
        setResizable(true);

        if (image25DCheckbox.isEnabled()) {
            image25DCheckbox.setSelected(image.getFileInfo()[0].getIs2_5D());
        }

        // setVisible( true );

        System.gc();
    }

    /**
     * Use the GUI results to set up the variables needed to run the algorithm.
     *
     * @return  <code>true</code> if parameters set successfully, <code>false</code> otherwise.
     */
    private boolean setVariables() {
        String tmpStr;

        if (replaceImage.isSelected()) {
            displayLoc = REPLACE;
        } else if (newImage.isSelected()) {
            displayLoc = NEW;
        }

        useVOI = VOIRegions.isSelected();
        image25D = image25DCheckbox.isSelected();

        tmpStr = textGaussX.getText();

        if (testParameter(tmpStr, 0.0, 10.0)) {
            scaleX = Float.valueOf(tmpStr).floatValue();
        } else {
            textGaussX.requestFocus();
            textGaussX.selectAll();

            return false;
        }

        tmpStr = textGaussY.getText();

        if (testParameter(tmpStr, 0.0, 10.0)) {
            scaleY = Float.valueOf(tmpStr).floatValue();
        } else {
            textGaussY.requestFocus();
            textGaussY.selectAll();

            return false;
        }

        if (!image25D) {
            tmpStr = textGaussZ.getText();

            if (testParameter(tmpStr, 0.0, 10.0)) {
                scaleZ = Float.valueOf(tmpStr).floatValue();
            } else {
                textGaussZ.requestFocus();
                textGaussZ.selectAll();

                return false;
            }
        }

        // Apply normalization if requested!
        if (!image25D) {

            if (resolutionCheckbox.isSelected()) {
                scaleZ = scaleZ * normFactor;
            }
        }

        // set up red, green, and blue ARGB values
        if (redCheckbox.isSelected() && redCheckbox.isEnabled()) {
            red = true;
        }

        if (greenCheckbox.isSelected() && greenCheckbox.isEnabled()) {
            green = true;
        }

        if (blueCheckbox.isSelected() && blueCheckbox.isEnabled()) {
            blue = true;
        }

        return true;
    }

}
