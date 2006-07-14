package gov.nih.mipav.view.dialogs;


import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.scripting.ParserException;
import gov.nih.mipav.model.scripting.parameters.ParameterFactory;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import javax.swing.*;


/**
 * Dialog to call AlgorithmAutoCovariance.
 */
public class JDialogAutoCovariance extends JDialogScriptableBase implements AlgorithmInterface{

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = 5044633994224797404L;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** DOCUMENT ME! */
    private AlgorithmAutoCovariance algoAutoCovariance = null;

    /** DOCUMENT ME! */
    private boolean haveBlue = false;

    /** DOCUMENT ME! */
    private boolean haveGreen = false;

    /** DOCUMENT ME! */
    private boolean haveRed = false;

    /** DOCUMENT ME! */
    private ModelImage image; // source image

    /** DOCUMENT ME! */
    private double maxR, maxG, maxB;

    /** DOCUMENT ME! */
    private double minR, minG, minB;

    /** DOCUMENT ME! */
    private ModelImage resultImage = null; // result image

    /** DOCUMENT ME! */
    private ModelImage resultImageB = null;

    /** DOCUMENT ME! */
    private ModelImage resultImageG = null;

    /** DOCUMENT ME! */
    private ModelImage resultImageR = null;

    /** DOCUMENT ME! */
    private String[] titles;

    /** DOCUMENT ME! */
    private ViewUserInterface UI;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Empty constructor needed for dynamic instantiation (used during scripting).
     */
    public JDialogAutoCovariance() { }

    /**
     * Constructs new transform dialog and sets up GUI components.
     *
     * @param  theParentFrame  Parent frame.
     * @param  im              Source image.
     */
    public JDialogAutoCovariance(Frame theParentFrame, ModelImage im) {
        super(theParentFrame, true);
        image = im;
        UI = image.getUserInterface();
        init();
    }

   

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Closes dialog box when the OK button is pressed, sets the variables, and calls the algorithm.
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
        }
    }

    
    
    /**
     * Record the parameters just used to run this algorithm in a script.
     * 
     * @throws  ParserException  If there is a problem creating/recording the new parameters.
     */
    protected  void storeParamsFromGUI() throws ParserException{
        try{

            scriptParameters.storeInputImage(image);
            
            scriptParameters.getParams().put(ParameterFactory.newParameter("haveBlue",haveBlue));
            scriptParameters.getParams().put(ParameterFactory.newParameter("haveGreen",haveGreen));
            scriptParameters.getParams().put(ParameterFactory.newParameter("haveRed",haveRed));
            scriptParameters.getParams().put(ParameterFactory.newParameter("maxR",maxR));
            scriptParameters.getParams().put(ParameterFactory.newParameter("maxG",maxG));
            scriptParameters.getParams().put(ParameterFactory.newParameter("maxB",maxB));
            scriptParameters.getParams().put(ParameterFactory.newParameter("minR",minR));
            scriptParameters.getParams().put(ParameterFactory.newParameter("minG",minG));
            scriptParameters.getParams().put(ParameterFactory.newParameter("minB",minB));
        }catch (ParserException pe){
            MipavUtil.displayError("Error encountered saving script params:\n" + pe);
        }  
    }
    
    /**
     * Set the dialog GUI using the script parameters while running this algorithm as part of a script.
     */
    protected void setGUIFromParams(){
        haveBlue = scriptParameters.getParams().getBoolean("haveBlue");
        haveGreen = scriptParameters.getParams().getBoolean("haveGreen");
        haveRed = scriptParameters.getParams().getBoolean("haveRed");
        maxR = scriptParameters.getParams().getDouble("maxR");
        maxG = scriptParameters.getParams().getDouble("maxG");
        maxB = scriptParameters.getParams().getDouble("maxB");
        minR = scriptParameters.getParams().getDouble("minR");
        minG = scriptParameters.getParams().getDouble("minG");
        minB = scriptParameters.getParams().getDouble("minB");
    }
    
    /**
     * Used to perform actions after the execution of the algorithm is completed (e.g., put the result image in the image table).
     * Defaults to no action, override to actually have it do something.
     */
    public void doPostAlgorithmActions() {
             AlgorithmParameters.storeImageInRunner(getResultImage());
             AlgorithmParameters.storeImageInRunner(getResultImageB());
             AlgorithmParameters.storeImageInRunner(getResultImageG());
             AlgorithmParameters.storeImageInRunner(getResultImageR());
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
        ViewJFrameImage imageFrameR = null;
        ViewJFrameImage imageFrameG = null;
        ViewJFrameImage imageFrameB = null;
        int verticalPosition = 200;

        if (algorithm instanceof AlgorithmAutoCovariance) {
            image.clearMask();

            if (image.isColorImage()) {

                if ((algoAutoCovariance.isCompleted() == true) &&
                        ((resultImageR != null) || (resultImageG != null) || (resultImageB != null))) {
                    // The algorithm has completed and produced a new image to be displayed.

                    if (resultImageR != null) {
                        updateFileInfo(image, resultImageR);
                        resultImageR.clearMask();
                    }

                    if (resultImageG != null) {
                        updateFileInfo(image, resultImageG);
                        resultImageG.clearMask();
                    }

                    if (resultImageB != null) {
                        updateFileInfo(image, resultImageB);
                        resultImageB.clearMask();
                    }

                    if (resultImageR != null) {

                        try {
                            imageFrameR = new ViewJFrameImage(resultImageR, null, new Dimension(610, verticalPosition));
                            verticalPosition += 20;
                        } catch (OutOfMemoryError error) {
                            System.gc();
                            MipavUtil.displayError("Out of memory: unable to open imageFrameR");
                        }
                    } // if (resultImageR != null)

                    if (resultImageG != null) {

                        try {
                            imageFrameG = new ViewJFrameImage(resultImageG, null, new Dimension(610, verticalPosition));
                            verticalPosition += 20;
                        } catch (OutOfMemoryError error) {
                            System.gc();
                            MipavUtil.displayError("Out of memory: unable to open imageFrameG");
                        }
                    } // if (resultImageG != null)

                    if (resultImageB != null) {

                        try {
                            imageFrameB = new ViewJFrameImage(resultImageB, null, new Dimension(610, verticalPosition));
                        } catch (OutOfMemoryError error) {
                            System.gc();
                            MipavUtil.displayError("Out of memory: unable to open imageFrameB");
                        }
                    } // if (resultImageB != null)
                } else if ((resultImageR == null) && (resultImageG == null) && (resultImageB == null)) {

                    // These next lines set the titles in all frames where the source image is displayed to
                    // image name so as to indicate that the image is now unlocked!
                    // The image frames are enabled and then registered to the userinterface.
                    Vector imageFrames = image.getImageFrameVector();

                    for (int i = 0; i < imageFrames.size(); i++) {
                        ((ViewJFrameBase) (imageFrames.elementAt(i))).setTitle(titles[i]);
                        ((ViewJFrameBase) (imageFrames.elementAt(i))).setEnabled(true);

                        if (((Frame) (imageFrames.elementAt(i))) != parentFrame) {
                            UI.registerFrame((Frame) (imageFrames.elementAt(i)));

                        }
                    }

                    if (parentFrame != null) {
                        UI.registerFrame(parentFrame);
                    }

                    image.notifyImageDisplayListeners(null, true);
                } else if ((resultImageR != null) || (resultImageG != null) || (resultImageB != null)) {

                    // algorithm failed but result image still has garbage
                    if (resultImageR != null) {
                        resultImageR.disposeLocal(); // clean up memory
                        resultImageR = null;
                    }

                    if (resultImageG != null) {
                        resultImageG.disposeLocal(); // clean up memory
                        resultImageG = null;
                    }

                    if (resultImageB != null) {
                        resultImageB.disposeLocal(); // clean up memory
                        resultImageB = null;
                    }

                    System.gc();

                }
            } // if (image.isColorImage())
            else { // image black and white

                if ((algoAutoCovariance.isCompleted() == true) && (resultImage != null)) {
                    // The algorithm has completed and produced a new image to be displayed.

                    updateFileInfo(image, resultImage);
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
                        ((ViewJFrameBase) (imageFrames.elementAt(i))).setTitle(titles[i]);
                        ((ViewJFrameBase) (imageFrames.elementAt(i))).setEnabled(true);

                        if (((Frame) (imageFrames.elementAt(i))) != parentFrame) {
                            UI.registerFrame((Frame) (imageFrames.elementAt(i)));

                        }
                    }

                    if (parentFrame != null) {
                        UI.registerFrame(parentFrame);
                    }

                    image.notifyImageDisplayListeners(null, true);
                } else if (resultImage != null) {

                    // algorithm failed but result image still has garbage
                    resultImage.disposeLocal(); // clean up memory
                    resultImage = null;
                    System.gc();

                }
            } // else image black and white
        } // if ( algorithm instanceof AlgorithmAutoCovariance)

        insertScriptLine();
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
     * Accessor that returns the blue image.
     *
     * @return  The result blue image
     */
    public ModelImage getResultImageB() {
        return resultImageB;
    }

    /**
     * Accessor that returns the green image.
     *
     * @return  The result green image
     */
    public ModelImage getResultImageG() {
        return resultImageG;
    }

    /**
     * Accessor that returns the red image.
     *
     * @return  The result red image
     */
    public ModelImage getResultImageR() {
        return resultImageR;
    }

 
 

    /**
     * Calls the algorithm with the set variables.
     */
    protected void callAlgorithm() {
        // Can correlate from 0 to extents - 1 in every dimension.
        // Therefore, make the source image and result image
        // extents identical.

        try {

            if (image.isColorImage()) {

                if (haveRed) {
                    String nameR = makeImageName(image.getImageName(), "_autocovarianceR");
                    resultImageR = new ModelImage(ModelStorageBase.FLOAT, image.getExtents(), nameR,
                                                  image.getUserInterface());
                }

                if (haveGreen) {
                    String nameG = makeImageName(image.getImageName(), "_autocovarianceG");
                    resultImageG = new ModelImage(ModelStorageBase.FLOAT, image.getExtents(), nameG,
                                                  image.getUserInterface());
                }

                if (haveBlue) {
                    String nameB = makeImageName(image.getImageName(), "_autocovarianceB");
                    resultImageB = new ModelImage(ModelStorageBase.FLOAT, image.getExtents(), nameB,
                                                  image.getUserInterface());
                }

                algoAutoCovariance = new AlgorithmAutoCovariance(resultImageR, resultImageG, resultImageB, image);
            } else {
                String name = makeImageName(image.getImageName(), "_autocovariance");
                resultImage = new ModelImage(ModelStorageBase.FLOAT, image.getExtents(), name,
                                             image.getUserInterface());

                algoAutoCovariance = new AlgorithmAutoCovariance(resultImage, image);
            }

            // This is very important. Adding this object as a listener allows
            // the algorithm to notify this object when it has completed of failed.
            // See algorithm performed event. This is made possible by implementing
            algoAutoCovariance.addListener(this);
            // Start the thread as a low priority because we wish to still have
            // user interface work fast

            if (isRunInSeparateThread()) {

                if (algoAutoCovariance.startMethod(Thread.MIN_PRIORITY) == false) {
                    MipavUtil.displayError("A thread is already running on this object");
                }
            } else {
                if (!UI.isAppFrameVisible()) {
                    algoAutoCovariance.setProgressBarVisible(false);
                }

                algoAutoCovariance.run();
            }
        } catch (OutOfMemoryError x) {

            if (resultImage != null) {
                resultImage.disposeLocal(); // Clean up memory of result image
                resultImage = null;
            }

            if (resultImageR != null) {
                resultImageR.disposeLocal(); // Clean up memory of result image
                resultImageR = null;
            }

            if (resultImageG != null) {
                resultImageG.disposeLocal(); // Clean up memory of result image
                resultImageG = null;
            }

            if (resultImageB != null) {
                resultImageB.disposeLocal(); // Clean up memory of result image
                resultImageB = null;
            }

            System.gc();
            MipavUtil.displayError("Dialog Autocovariance: unable to allocate enough memory");

            return;
        }
    }

    /**
     * Initializes the dialog box to a certain size and adds the components.
     */
    private void init() {
        int colorsPresent;
        JPanel mainPanel = null;
        JLabel limitLabel = null;
        GridBagConstraints gbc;
        setForeground(Color.black);
        setTitle("AutoCovariance coefficients");

        JPanel buttonPanel = new JPanel();

        if (image.isColorImage()) {
            minR = image.getMinR();
            maxR = image.getMaxR();

            if (minR != maxR) {
                haveRed = true;
            }

            minG = image.getMinG();
            maxG = image.getMaxG();

            if (minG != maxG) {
                haveGreen = true;
            }

            minB = image.getMinB();
            maxB = image.getMaxB();

            if (minB != maxB) {
                haveBlue = true;
            }

            colorsPresent = 0;

            if (haveRed) {
                colorsPresent++;
            }

            if (haveGreen) {
                colorsPresent++;
            }

            if (haveBlue) {
                colorsPresent++;
            }

            if (colorsPresent == 0) {
                MipavUtil.displayError("Red, green, and blue are all single valued");

                return;
            } else if (colorsPresent == 1) {
                mainPanel = new JPanel(new GridBagLayout());
                gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.weightx = 1;
                gbc.anchor = gbc.WEST;
                gbc.gridwidth = 1;
                gbc.fill = gbc.BOTH;
                gbc.insets = new Insets(5, 5, 5, 5);

                if (haveRed) {
                    limitLabel = new JLabel("Only the red channel has more than 1 value");
                } else if (haveGreen) {
                    limitLabel = new JLabel("Only the green channel has more than 1 value");
                } else {
                    limitLabel = new JLabel("Only the blue channel has more than 1 value");
                }

                limitLabel.setForeground(Color.black);
                limitLabel.setFont(serif12);
                mainPanel.add(limitLabel, gbc);
            } // else if (colorsPresent == 1)
            else if (colorsPresent == 2) {
                mainPanel = new JPanel(new GridBagLayout());
                gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.weightx = 1;
                gbc.anchor = gbc.WEST;
                gbc.gridwidth = 1;
                gbc.fill = gbc.BOTH;
                gbc.insets = new Insets(5, 5, 5, 5);

                if (haveRed && haveGreen) {
                    limitLabel = new JLabel("Only the red and green channels have more than 1 value");
                } else if (haveRed && haveBlue) {
                    limitLabel = new JLabel("Only the red and blue channels have more than 1 value");
                } else {
                    limitLabel = new JLabel("Only the green and blue channels have more than 1 value");
                }

                limitLabel.setForeground(Color.black);
                limitLabel.setFont(serif12);
                mainPanel.add(limitLabel, gbc);
            } // else if (colorsPresent == 2)
        } // if (image.isColorImage())

        buildOKButton();
        buildCancelButton();
        buttonPanel.add(OKButton);
        buttonPanel.add(cancelButton);

        if (mainPanel != null) {
            mainDialogPanel.add(mainPanel);
            mainDialogPanel.add(buttonPanel, BorderLayout.SOUTH);
        } else {
            mainDialogPanel.add(buttonPanel);
        }

        getContentPane().add(mainDialogPanel);

        pack();
        setVisible(true);
    }

    /**
     * Sets the variables needed to run the algorithm.
     *
     * @return  Flag indicating successful set of the variables.
     */
    private boolean setVariables() {

        // Hide dialog
        setVisible(false);

        return true;
    }

}
