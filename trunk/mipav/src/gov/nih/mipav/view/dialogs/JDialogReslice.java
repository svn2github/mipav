package gov.nih.mipav.view.dialogs;


import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.algorithms.utilities.*;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;


/**
 * Dialog to get interpolation choice, then call reslice algorithm.
 *
 * @version  0.1 Nov 17, 1998
 * @author   Matthew J. McAuliffe, Ph.D.
 * @see      AlgorithmReslice
 */
public class JDialogReslice extends JDialogBase implements AlgorithmInterface, ScriptableInterface {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = 293226884205657186L;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** Used for choosing interpolation. */
    private JComboBox comboBoxInterp;

    /** Image to reslice. */
    private ModelImage image;

    /** Interpolation mode. */
    private int mode;

    /** Reslice algorithm. */
    private AlgorithmReslice resliceAlgo;

    /** resliced image. */
    private ModelImage resultImage = null;

    /** User interface pointer. */
    private ViewUserInterface userInterface;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Empty constructor needed for dynamic instantiation (used during scripting).
     */
    public JDialogReslice() { }

    /**
     * Creates new reslice dialog.
     *
     * @param  theParentFrame  parent frame
     * @param  im              source image
     */
    public JDialogReslice(Frame theParentFrame, ModelImage im) {
        super(theParentFrame, false);
        image = im;
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
    public JDialogReslice(ViewUserInterface UI, ModelImage im) {
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
            setVariables();
            callAlgorithm();
        } else if (command.equals("Cancel")) {
            dispose();
        } else if (command.equals("Help")) {
            MipavUtil.showHelp("10035");
        }
    }

    // ************************************************************************
    // ************************** Algorithm Events ****************************
    // ************************************************************************

    /**
     * This method is required if the AlgorithmPerformed interface is implemented. It is called by the algorithms when
     * it has completed or failed to to complete, so that the dialog can be display the result image and/or clean up.
     *
     * @param  algorithm  algorithm that caused the event.
     */
    public void algorithmPerformed(AlgorithmBase algorithm) {

        ViewJFrameImage imageFrame = null;

        if (algorithm instanceof AlgorithmReslice) {

            if (resliceAlgo.isCompleted() == true) {
                resultImage = resliceAlgo.getResultImage();

                if (resultImage != null) {

                    // The algorithm has completed and produced a new image to be displayed.
                    try {
                        resultImage.setImageName("Isotropic");
                        imageFrame = new ViewJFrameImage(resultImage, null, new Dimension(610, 200));
                    } catch (OutOfMemoryError error) {
                        MipavUtil.displayError("Out of memory: unable to open new frame");
                    }
                }
            } else if (resliceAlgo.getResultImage() != null) {

                // algorithm failed but result image still has garbage
                resliceAlgo.getResultImage().disposeLocal(); // clean up memory
            }

            insertScriptLine(algorithm);

            resliceAlgo.finalize();
            resliceAlgo = null;
            dispose();

        } // if (algorithm instanceof AlgorithmReslice)
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

                userInterface.getScriptDialog().putVar(resultImage.getImageName());
                userInterface.getScriptDialog().append("Reslice " +
                                                       userInterface.getScriptDialog().getVar(image.getImageName()) +
                                                       " " +
                                                       userInterface.getScriptDialog().getVar(resultImage.getImageName()) +
                                                       " " + mode + "\n");
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

        try {
            setMode(parser.getNextInteger());
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        setSeparateThread(false);
        callAlgorithm();

        if (!srcImageKey.equals(destImageKey)) {
            parser.putVariable(destImageKey, getResultImage().getImageName());
        }
    }

    /**
     * Accessor that sets the mode: linear, cubic, or cubic bspline.
     *
     * @param  type  The mode to set to.
     */
    public void setMode(int type) {
        mode = type;
    }

    /**
     * Calls the algorithm using the mode.
     */
    private void callAlgorithm() {
        System.gc();

        int[] destExtents = new int[3];

        destExtents[0] = image.getExtents()[0];
        destExtents[1] = image.getExtents()[1];
        destExtents[2] = image.getExtents()[2];

        try {

            // Make algorithm
            resliceAlgo = new AlgorithmReslice(image, mode);

            // This is very important. Adding this object as a listener allows the algorithm to
            // notify this object when it has completed of failed. See algorithm performed event.
            // This is made possible by implementing AlgorithmedPerformed interface
            resliceAlgo.addListener(this);

            // Hide dialog
            setVisible(false);

            if (isRunInSeparateThread()) {

                // Start the thread as a low priority because we wish to still have user interface work fast
                if (resliceAlgo.startMethod(Thread.MIN_PRIORITY) == false) {
                    MipavUtil.displayError("A thread is already running on this object");
                }
            } else {
                if (!userInterface.isAppFrameVisible()) {
                    resliceAlgo.setProgressBarVisible(false);
                }

                resliceAlgo.run();
            }
        } catch (OutOfMemoryError x) {
            MipavUtil.displayError("Dialog reslice: unable to allocate enough memory");

            if (resliceAlgo.getResultImage() != null) {
                resliceAlgo.getResultImage().disposeLocal(); // Clean up destination image memory
            }

            return;
        }
    }

    /**
     * Sets up the GUI (panels, buttons, etc) and displays it on the screen.
     */
    private void init() {

        if (image.getNDims() != 3) {
            MipavUtil.displayError("Source Image is not 3D");
            dispose();

            return;
        }

        setForeground(Color.black);
        setTitle("Reslice");

        JLabel labelInterp = new JLabel("Interpolation:");
        labelInterp.setForeground(Color.black);
        labelInterp.setFont(serif12);
        labelInterp.setAlignmentX(Component.LEFT_ALIGNMENT);

        comboBoxInterp = new JComboBox();
        comboBoxInterp.setFont(serif12);
        comboBoxInterp.setBackground(Color.white);
        comboBoxInterp.setAlignmentX(Component.LEFT_ALIGNMENT);

        comboBoxInterp.addItem("Linear");
        comboBoxInterp.addItem("Cubic convolution");
        comboBoxInterp.addItem("Cubic Bspline");

        JPanel mainPanel = new JPanel();
        mainPanel.add(labelInterp);
        mainPanel.add(comboBoxInterp);
        mainPanel.setBorder(buildTitledBorder("Choose interpolation"));

        getContentPane().add(mainPanel);
        getContentPane().add(buildButtons(), BorderLayout.SOUTH);
        pack();
        setVisible(true);
    }

    /**
     * Sets the mode variable based on what was selected in the GUI.
     */
    private void setVariables() {
        mode = 0;

        switch (comboBoxInterp.getSelectedIndex()) {

            case 0:
                mode = AlgorithmReslice.LINEAR;
                break;

            case 1:
                mode = AlgorithmReslice.CUBIC;
                break;

            case 2:
                mode = AlgorithmReslice.CUBIC_BSPLINE;
                break;
        }
    }

}
