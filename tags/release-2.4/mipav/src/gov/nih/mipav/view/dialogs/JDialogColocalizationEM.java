package gov.nih.mipav.view.dialogs;

import gov.nih.mipav.view.*;
import gov.nih.mipav.model.structures.*;
import gov.nih.mipav.model.algorithms.*;

import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

/**
*   Dialog to get user input
*   Identify colocalized pixels
*   Algorithms are executed in their own thread.
*
*/
public class JDialogColocalizationEM extends JDialogBase implements AlgorithmInterface, ScriptableInterface {

    private     ModelImage  firstImage;
    private     ModelImage  secondImage = null;
    private     ModelImage  resultImage = null;   // result image
    // Class segmentation shown
    private     ModelImage  segImage = null;
    private     int         leftPad = 80;
    private     int         rightPad = 40;
    private     int         bottomPad = 60;
    private     int         topPad = 40;
    private     JLabel      threshold1Label;
    private     JTextField  threshold1Text;
    private     float       threshold1 = 1.0f;
    private     JLabel      threshold2Label;
    private     JTextField  threshold2Text;
    private     float       threshold2 = 1.0f;
    private     JRadioButton orButton;
    private     JRadioButton andButton;
    private     boolean      doOr = true;
    private     JCheckBox   regCheckBox;
    private     boolean     register;
    private     JLabel      labelCost;
    private     JComboBox   comboBoxCostFunct;
    private     int         cost;
    private     JLabel      bin1Label;
    private     JTextField  bin1Text;
    private     int         bin1;
    private     JLabel      bin2Label;
    private     JTextField  bin2Text;
    private     int         bin2;
    private     double      possibleIntValues;
    private     double      possibleInt2Values;
    private     boolean     bin1Default;
    private     boolean     bin2Default;
    private     String      titles[];
    private     JComboBox   imageComboBox;
    private     ViewUserInterface UI;
    private     JLabel      labelImage;
    private     double      minR, minG, minB;
    private     double      maxR, maxG, maxB;
    private     double      minV, maxV;
    private     double      secondMinV, secondMaxV;
    private     double      minRV, minGV, minBV;
    private     double      maxRV, maxGV, maxBV;
    private     JCheckBox   redCheckBox;
    private     JCheckBox   greenCheckBox;
    private     JCheckBox   blueCheckBox;
    private     boolean     useRed = false;
    private     boolean     useGreen = false;
    private     boolean     useBlue = false;
    private     int         colorsPresent = 0;
    private     JLabel      gaussianLabel;
    private     JTextField  gaussianText;
    private     int         gaussians = 4;
    private     JLabel      iterationLabel;
    private     JTextField  iterationText;
    private     int         iterations = 20;
    private     JRadioButton wholeImage;
    private     JRadioButton VOIRegions;
    private     boolean     entireImage = true;
    private     int         nBoundingVOIs;
    private     BitSet      mask = null;
    private     int         xDim;
    private     int         yDim;
    private     int         zDim;
    private     int         imageLength;
    private     float       buffer[] = null;
    private     String      secondName = null;

    private     AlgorithmColocalizationEM colocalizationAlgo = null;


    /**
    *  Creates new dialog.
    *  @param theParentFrame    Parent frame
    *  @param im                Source image
    */
    public JDialogColocalizationEM(Frame theParentFrame, ModelImage im) {
        super(theParentFrame, true);
        firstImage = im;
        init();
    }

    public JDialogColocalizationEM(ViewUserInterface UI, ModelImage firstImage) {
        super();
        this.UI = UI;
        this.firstImage = firstImage;
        parentFrame = firstImage.getParentFrame();
    }

    /**
     * Empty constructor needed for dynamic instantiation (used during scripting).
     */
    public JDialogColocalizationEM() {}

    /**
     * Run this algorithm from a script.
     * @param parser the script parser we get the state from
     * @throws IllegalArgumentException if there is something wrong with the arguments in the script
     */
    public void scriptRun (AlgorithmScriptParser parser) throws IllegalArgumentException {
        String image1Key = null;
        String image2Key = null;
        String destImageKey = null;
        String segImageKey = null;

        try {
            image1Key = parser.getNextString();
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
        ModelImage im1 = parser.getImage(image1Key);
        if (!im1.isColorImage()) {
            try {
                image2Key = parser.getNextString();
            } catch (Exception e) {
                throw new IllegalArgumentException();
            }
            setSecondImage(parser.getImage(image2Key));
        }

        firstImage = im1;
        UI = firstImage.getUserInterface();
        parentFrame = firstImage.getParentFrame();

        // the result image
        try {
            destImageKey = parser.getNextString();
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        try {
          segImageKey = parser.getNextString();
        }
        catch (Exception e) {
          throw new IllegalArgumentException();
        }

        try {
            setBin1(parser.getNextInteger());
            setBin2(parser.getNextInteger());
            setBin1Default(parser.getNextBoolean());
            setBin2Default(parser.getNextBoolean());
            setThreshold1(parser.getNextFloat());
            setThreshold2(parser.getNextFloat());
            setDoOr(parser.getNextBoolean());
            setLeftPad(parser.getNextInteger());
            setRightPad(parser.getNextInteger());
            setBottomPad(parser.getNextInteger());
            setTopPad(parser.getNextInteger());
            if (firstImage.isColorImage()) {
                setUseRed(parser.getNextBoolean());
                setUseGreen(parser.getNextBoolean());
                setUseBlue(parser.getNextBoolean());
            }
            setEntireImage(parser.getNextBoolean());
            setRegister(parser.getNextBoolean());
            setCost(parser.getNextInteger());
            setGaussians(parser.getNextInteger());
            setIterations(parser.getNextInteger());
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        setActiveImage(parser.isActiveImage());
        setSeparateThread(false);

        if (secondImage != null) {
            if (bin1Default) {
                possibleIntValues = firstImage.getMax() - firstImage.getMin() + 1;
                if ( (firstImage.getType() == ModelStorageBase.BYTE) ||
                    (firstImage.getType() == ModelStorageBase.UBYTE) ||
                    (firstImage.getType() == ModelStorageBase.SHORT) ||
                    (firstImage.getType() == ModelStorageBase.USHORT) ||
                    (firstImage.getType() == ModelStorageBase.INTEGER) ||
                    (firstImage.getType() == ModelStorageBase.UINTEGER) ||
                    (firstImage.getType() == ModelStorageBase.LONG)) {
                    bin1 = (int) Math.round(possibleIntValues);
                }
            } // if (bin1Default)
            if (bin2Default) {
                possibleInt2Values = secondImage.getMax() - secondImage.getMin() + 1;
                if ( (secondImage.getType() == ModelStorageBase.BYTE) ||
                    (secondImage.getType() == ModelStorageBase.UBYTE) ||
                    (secondImage.getType() == ModelStorageBase.SHORT) ||
                    (secondImage.getType() == ModelStorageBase.USHORT) ||
                    (secondImage.getType() == ModelStorageBase.INTEGER) ||
                    (secondImage.getType() == ModelStorageBase.UINTEGER) ||
                    (secondImage.getType() == ModelStorageBase.LONG)) {
                    bin2 = (int) Math.round(possibleInt2Values);
                }
            } // if (bin2Default)
        } else { // secondImage == null
            if (bin1Default) {
                if (useRed) {
                    possibleIntValues = firstImage.getMaxR()
                        - firstImage.getMinR() + 1;
                } else {
                    possibleIntValues = firstImage.getMaxG()
                        - firstImage.getMinG() + 1;
                }
                if ( (firstImage.getType() == ModelStorageBase.ARGB) ||
                    (firstImage.getType() == ModelStorageBase.ARGB_USHORT)) {
                    bin1 = (int) Math.round(possibleIntValues);
                }
            } // if (bin1Default)
            if (bin2Default) {
                if (useBlue) {
                    possibleInt2Values = firstImage.getMaxB()
                        - firstImage.getMinB() + 1;
                } else {
                    possibleInt2Values = firstImage.getMaxG()
                        - firstImage.getMinG() + 1;
                }
                if ( (firstImage.getType() == ModelStorageBase.ARGB) ||
                    (firstImage.getType() == ModelStorageBase.ARGB_USHORT)) {
                    bin2 = (int) Math.round(possibleInt2Values);
                }
            } // if (bin2Default)
        } // else secondImage == null

        callAlgorithm();
        if (!image1Key.equals(destImageKey)) {
            parser.putVariable(destImageKey, getResultImage().getImageName());
        }
        parser.putVariable(segImageKey,getSegImage().getImageName());
    }

    /**
     * If a script is being recorded and the algorithm is done, add an entry for this algorithm.
     * @param algo the algorithm to make an entry for
     */
    public void insertScriptLine (AlgorithmBase algo) {
        if (algo.isCompleted() == true) {
            if (secondImage != null) {
                if (UI.isScriptRecording()) {

                    //check to see if the first image is already in the ImgTable
                    if (UI.getScriptDialog().getImgTableVar(firstImage.getImageName()) == null) {
                        if (UI.getScriptDialog().getActiveImgTableVar(firstImage.getImageName()) == null) {
                            UI.getScriptDialog().putActiveVar(firstImage.getImageName());
                        }
                    }

                    //then second
                    if (UI.getScriptDialog().getImgTableVar(secondImage.getImageName()) == null) {
                        if (UI.getScriptDialog().getActiveImgTableVar(secondImage.getImageName()) == null) {
                            UI.getScriptDialog().putActiveVar(secondImage.getImageName());
                        }
                    }

                    possibleIntValues = firstImage.getMax() - firstImage.getMin() + 1;
                    if ( ( (firstImage.getType() == ModelStorageBase.BYTE) ||
                          (firstImage.getType() == ModelStorageBase.UBYTE) ||
                          (firstImage.getType() == ModelStorageBase.SHORT) ||
                          (firstImage.getType() == ModelStorageBase.USHORT) ||
                          (firstImage.getType() == ModelStorageBase.INTEGER) ||
                          (firstImage.getType() == ModelStorageBase.UINTEGER) ||
                          (firstImage.getType() == ModelStorageBase.LONG)) &&
                        (bin1 == (int) Math.round(possibleIntValues))) {
                        bin1Default = true;
                    } else {
                        bin1Default = false;
                    }
                    possibleInt2Values = secondImage.getMax() - secondImage.getMin() + 1;
                    if ( ( (secondImage.getType() == ModelStorageBase.BYTE) ||
                          (secondImage.getType() == ModelStorageBase.UBYTE) ||
                          (secondImage.getType() == ModelStorageBase.SHORT) ||
                          (secondImage.getType() == ModelStorageBase.USHORT) ||
                          (secondImage.getType() == ModelStorageBase.INTEGER) ||
                          (secondImage.getType() == ModelStorageBase.UINTEGER) ||
                          (secondImage.getType() == ModelStorageBase.LONG)) &&
                        (bin2 == (int) Math.round(possibleInt2Values))) {
                        bin2Default = true;
                    } else {
                        bin2Default = false;
                    }
                    UI.getScriptDialog().append("ColocalizationEM " +
                                                UI.getScriptDialog().getVar(firstImage.getImageName()) +
                                                " " + UI.getScriptDialog().getVar(secondImage.getImageName()) + " ");
                    UI.getScriptDialog().putVar(resultImage.getImageName());
                    UI.getScriptDialog().append(UI.getScriptDialog().getVar(resultImage.getImageName()) + " ");
                    UI.getScriptDialog().putVar(segImage.getImageName());
                    UI.getScriptDialog().append(UI.getScriptDialog().getVar(segImage.getImageName())
                                                + " " + bin1 + " " + bin2 + " " +
                                                bin1Default + " " + bin2Default + " " + threshold1 + " " +
                                                threshold2 + " " + doOr + " " + leftPad + " " + rightPad + " " +
                                                bottomPad + " " + topPad + " " + entireImage + " " +
                                                register + " " + cost + " " + gaussians + " " + iterations + "\n");
                }

            } else { // if (secondImage != null)
                if (UI.isScriptRecording()) {

                    //check to see if the first image is already in the ImgTable
                    if (UI.getScriptDialog().getImgTableVar(firstImage.getImageName()) == null) {
                        if (UI.getScriptDialog().getActiveImgTableVar(firstImage.getImageName()) == null) {
                            UI.getScriptDialog().putActiveVar(firstImage.getImageName());
                        }
                    }

                    if (useRed) {
                        possibleIntValues = firstImage.getMaxR()
                            - firstImage.getMinR() + 1;
                    } else {
                        possibleIntValues = firstImage.getMaxG()
                            - firstImage.getMinG() + 1;
                    }

                    if (useBlue) {
                        possibleInt2Values = firstImage.getMaxB()
                            - firstImage.getMinB() + 1;
                    } else {
                        possibleInt2Values = firstImage.getMaxG()
                            - firstImage.getMinG() + 1;
                    }
                    if ( ( (firstImage.getType() == ModelStorageBase.ARGB) ||
                          (firstImage.getType() == ModelStorageBase.ARGB_USHORT)) &&
                        (bin1 == (int) Math.round(possibleIntValues))) {
                        bin1Default = true;
                    } else {
                        bin1Default = false;
                    }
                    if ( ( (firstImage.getType() == ModelStorageBase.ARGB) ||
                          (firstImage.getType() == ModelStorageBase.ARGB_USHORT)) &&
                        (bin2 == (int) Math.round(possibleInt2Values))) {
                        bin2Default = true;
                    } else {
                        bin2Default = false;
                    }
                    UI.getScriptDialog().append("ColocalizationEM " +
                                                UI.getScriptDialog().getVar(firstImage.getImageName()) +
                                                " ");
                    UI.getScriptDialog().putVar(resultImage.getImageName());
                    UI.getScriptDialog().append(UI.getScriptDialog().getVar(resultImage.getImageName()) + " ");
                    UI.getScriptDialog().putVar(segImage.getImageName());
                    UI.getScriptDialog().append(UI.getScriptDialog().getVar(segImage.getImageName())
                                                + " " + bin1 + " " + bin2 + " " +
                                                bin1Default + " " + bin2Default + " " + threshold1 + " " +
                                                threshold2 + " " + doOr + " " + leftPad + " " + rightPad + " " +
                                                bottomPad + " " + topPad + " " + useRed + " " + useGreen + " " +
                                                useBlue + " " + entireImage + " " +
                                                register + " " + cost + " " + gaussians + " " +
                                                iterations + "\n");
                }
            }
        } // if (colocalizationAlgo.isCompleted() == true)
    }


    /**
    *	Initializes GUI components and displays dialog.
    */
    private void init() {
        boolean haveRed;
        boolean haveGreen;
        boolean haveBlue;
        JPanel imagePanel;
        int i;
        ViewVOIVector VOIs;
        int nVOIs;

        setForeground(Color.black);
        setTitle("Expectation Maximization Colocalization");
        String firstName = firstImage.getImageName();

        VOIs = firstImage.getVOIs();
        nVOIs = VOIs.size();
        nBoundingVOIs = 0;
        for (i = 0; i < nVOIs; i++) {
            if ((VOIs.VOIAt(i).getCurveType() == VOI.CONTOUR) ||
                (VOIs.VOIAt(i).getCurveType() == VOI.POLYLINE)) {
                nBoundingVOIs++;
            }
        }
        if (nBoundingVOIs > 1) {
            MipavUtil.displayError("Only 1 contour VOI is allowed");
            return;
        }

        xDim = firstImage.getExtents()[0];
        yDim = firstImage.getExtents()[1];
        imageLength = xDim*yDim;
        if (firstImage.getNDims() >= 3) {
            zDim = firstImage.getExtents()[2];
            imageLength = imageLength*zDim;
        }

        if (nBoundingVOIs == 1) {
            mask = firstImage.generateVOIMask();
            if (firstImage.isColorImage()) {
                buffer = new float[4*imageLength];
                try {
                    firstImage.exportData(0,4*imageLength,buffer);
                }
                catch (IOException e) {
                    MipavUtil.displayError("IOException on firstImage.exportData");
                    return;
                }
                minRV = Double.MAX_VALUE;
                minGV = Double.MAX_VALUE;
                minBV = Double.MAX_VALUE;
                maxRV = -Double.MAX_VALUE;
                maxGV = -Double.MAX_VALUE;
                maxBV = -Double.MAX_VALUE;
                for (i = 0; i < imageLength; i++) {
                    if (mask.get(i)) {
                        if (buffer[4*i + 1] < minRV) {
                            minRV = buffer[4*i + 1];
                        }
                        if (buffer[4*i + 2] < minGV) {
                            minGV = buffer[4*i + 2];
                        }
                        if (buffer[4*i + 3] < minBV) {
                            minBV = buffer[4*i + 3];
                        }
                        if (buffer[4*i + 1] > maxRV) {
                            maxRV = buffer[4*i + 1];
                        }
                        if (buffer[4*i + 2] > maxGV) {
                            maxGV = buffer[4*i + 2];
                        }
                        if (buffer[4*i + 3] > maxBV) {
                            maxBV = buffer[4*i + 3];
                        }
                    } // if (mask.get(i))
                } // for (i = 0; i < imageLength; i++)
            } // if (firstImage.isColorImage())
            else { // firstImage is black and white
                buffer = new float[imageLength];
                try {
                    firstImage.exportData(0,imageLength,buffer);
                }
                catch (IOException e) {
                    MipavUtil.displayError("IOException on firstImage.exportData");
                    return;
                }
                minV = Double.MAX_VALUE;
                maxV = -Double.MAX_VALUE;
                for (i = 0; i < imageLength; i++) {
                    if (mask.get(i)) {
                        if (buffer[i] < minV) {
                            minV = buffer[i];
                        }
                        if (buffer[i] > maxV) {
                            maxV = buffer[i];
                        }
                    }
                }
            } // else firstImage is black and white
        } // if (nBoundingVOIs == 1)

        if (firstImage.isColorImage()) {

            haveRed = false;
            haveGreen = false;
            haveBlue = false;
            minR = firstImage.getMinR();
            maxR = firstImage.getMaxR();
            if (minR != maxR) {
                haveRed = true;
            }
            minG = firstImage.getMinG();
            maxG = firstImage.getMaxG();
            if (minG != maxG) {
                haveGreen = true;
            }
            minB = firstImage.getMinB();
            maxB = firstImage.getMaxB();
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
                MipavUtil.displayError("All channels in this color image are single valued");
                return;
            }
            else if (colorsPresent == 1) {
                if (haveRed) {
                    MipavUtil.displayError("Only the red channel has more than 1 bin");
                }
                else if (haveGreen) {
                    MipavUtil.displayError("Only the green channel has more than 1 bin");
                }
                else {
                    MipavUtil.displayError("Only the blue channel has more than 1 bin");
                }
                return;
            } // else if (colorsPresent == 1)
            else if (colorsPresent == 2) {
                if (haveRed && haveGreen) {
                    labelImage = new JLabel("Colocalization with red to green");
                    useRed = true;
                    useGreen = true;
                }
                else if (haveRed && haveBlue) {
                    labelImage = new JLabel("Colocalization with red to blue");
                    useRed = true;
                    useBlue = true;
                }
                else {
                    labelImage = new JLabel("Colocalization with green to blue");
                    useGreen = true;
                    useBlue = true;
                }
                labelImage.setForeground(Color.black);
                labelImage.setFont(serif12);
                imagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                imagePanel.setBorder(buildTitledBorder("Channel selection"));
                imagePanel.add(labelImage);
            } // else if (colorsPresent == 2)
            else { // colorsPresent == 3
                labelImage = new JLabel("Select 2 of the 3 colors");
                labelImage.setForeground(Color.black);
                labelImage.setFont(serif12);

                GridBagConstraints gbc2 = new GridBagConstraints();
                gbc2.gridwidth = 1; gbc2.gridheight = 1; gbc2.anchor = gbc2.WEST; gbc2.weightx = 1;
                gbc2.insets = new Insets(3,3,3,3);
                gbc2.fill = GridBagConstraints.HORIZONTAL;
                gbc2.gridx = 0; gbc2.gridy = 0;

                imagePanel = new JPanel(new GridBagLayout());
                imagePanel.setBorder(buildTitledBorder("Channel selection"));
                imagePanel.add(labelImage,gbc2);

                gbc2.gridy = 1;
                redCheckBox = new JCheckBox("Red");
                redCheckBox.setFont(serif12);
                redCheckBox.setForeground(Color.black);
                redCheckBox.setSelected(true);
                redCheckBox.addItemListener(this);
                imagePanel.add(redCheckBox,gbc2);

                gbc2.gridy = 2;
                greenCheckBox = new JCheckBox("Green");
                greenCheckBox.setFont(serif12);
                greenCheckBox.setForeground(Color.black);
                greenCheckBox.setSelected(true);
                greenCheckBox.addItemListener(this);
                imagePanel.add(greenCheckBox,gbc2);

                gbc2.gridy = 3;
                blueCheckBox = new JCheckBox("Blue");
                blueCheckBox.setFont(serif12);
                blueCheckBox.setForeground(Color.black);
                blueCheckBox.setSelected(false);
                blueCheckBox.addItemListener(this);
                imagePanel.add(blueCheckBox,gbc2);

                useRed = true;
                useGreen = true;
            } // else colorsPresent == 3

            bin1 = 256;
            bin2 = 256;
            if (useRed) {
                possibleIntValues = firstImage.getMaxR() - firstImage.getMinR() + 1;
            }
            else {
                possibleIntValues = firstImage.getMaxG() - firstImage.getMinG() + 1;
            }
            if (((firstImage.getType() == ModelStorageBase.ARGB) ||
                 (firstImage.getType() == ModelStorageBase.ARGB_USHORT)) &&
                 (possibleIntValues < 256)) {
                bin1 = (int)Math.round(possibleIntValues);
            }
            if (useBlue) {
                possibleInt2Values = firstImage.getMaxB() - firstImage.getMinB() + 1;
            }
            else {
                possibleInt2Values = firstImage.getMaxG() - firstImage.getMinG() + 1;
            }
            if (((firstImage.getType() == ModelStorageBase.ARGB) ||
                 (firstImage.getType() == ModelStorageBase.ARGB_USHORT)) &&
                 (possibleInt2Values < 256)) {
                bin2 = (int)Math.round(possibleInt2Values);
            }
        } // if (firstImage.isColorImage())
        else { // !(firstImage.isColorImage())
            labelImage = new JLabel("Colocalization with ["+ firstName + "] and ");
            labelImage.setForeground(Color.black);
            labelImage.setFont(serif12);
            imageComboBox = buildComboBox(firstImage);
            imageComboBox.addItemListener(this);

            UI = firstImage.getUserInterface();
            secondName = (String)imageComboBox.getSelectedItem();
            if (secondName == null) {
                MipavUtil.displayError("No image found to colocalize with");
                return;
            }
            secondImage   = UI.getRegisteredImageByName(secondName);

            if (nBoundingVOIs == 1) {
                try {
                    secondImage.exportData(0,imageLength,buffer);
                }
                catch (IOException e) {
                    MipavUtil.displayError("IOException on secondImage.exportData");
                    return;
                }
                secondMinV = Double.MAX_VALUE;
                secondMaxV = -Double.MAX_VALUE;
                for (i = 0; i < imageLength; i++) {
                    if (mask.get(i)) {
                        if (buffer[i] < secondMinV) {
                            secondMinV = buffer[i];
                        }
                        if (buffer[i] > secondMaxV) {
                            secondMaxV = buffer[i];
                        }
                    }
                }
            } // if (nBoundingVOIs == 1)

            bin1 = 256;
            bin2 = 256;
            possibleIntValues = firstImage.getMax() - firstImage.getMin() + 1;
            if (((firstImage.getType() == ModelStorageBase.BYTE) ||
                 (firstImage.getType() == ModelStorageBase.UBYTE) ||
                 (firstImage.getType() == ModelStorageBase.SHORT) ||
                 (firstImage.getType() == ModelStorageBase.USHORT) ||
                 (firstImage.getType() == ModelStorageBase.INTEGER) ||
                 (firstImage.getType() == ModelStorageBase.UINTEGER) ||
                 (firstImage.getType() == ModelStorageBase.LONG)) &&
                 (possibleIntValues < 256)) {
                bin1 = (int)Math.round(possibleIntValues);
            }
            possibleInt2Values = secondImage.getMax() - secondImage.getMin() + 1;
            if (((secondImage.getType() == ModelStorageBase.BYTE) ||
                 (secondImage.getType() == ModelStorageBase.UBYTE) ||
                 (secondImage.getType() == ModelStorageBase.SHORT) ||
                 (secondImage.getType() == ModelStorageBase.USHORT) ||
                 (secondImage.getType() == ModelStorageBase.INTEGER) ||
                 (secondImage.getType() == ModelStorageBase.UINTEGER) ||
                 (secondImage.getType() == ModelStorageBase.LONG)) &&
                 (possibleInt2Values < 256)) {
                bin2 = (int)Math.round(possibleInt2Values);
            }

            imagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            imagePanel.setBorder(buildTitledBorder("Channel selection"));
            imagePanel.add(labelImage);
            imagePanel.add(imageComboBox);
        } // else !firstImage.isColorImage()

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1; gbc.gridheight = 1; gbc.anchor = gbc.WEST; gbc.weightx = 1;
        gbc.insets = new Insets(3,3,3,3);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        JPanel registrationPanel = new JPanel(new GridBagLayout());
        registrationPanel.setForeground(Color.black);
        registrationPanel.setBorder(buildTitledBorder("Registration"));

        regCheckBox = new JCheckBox("Registration before Colocalization");
        regCheckBox.setFont(serif12);
        regCheckBox.setForeground(Color.black);
        regCheckBox.setSelected(false);
        regCheckBox.addItemListener(this);
        registrationPanel.add(regCheckBox, gbc);

        labelCost = new JLabel("Cost function:");
        labelCost.setForeground(Color.black);
        labelCost.setFont(serif12);
        labelCost.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelCost.setEnabled(false);
        gbc.gridx = 0;
        gbc.gridy = 1;
        registrationPanel.add(labelCost, gbc);

        comboBoxCostFunct = new JComboBox();
        comboBoxCostFunct.setFont(MipavUtil.font12);
        comboBoxCostFunct.setBackground(Color.white);
        comboBoxCostFunct.setToolTipText("Cost function");
        comboBoxCostFunct.addItem("Correlation ratio");
        comboBoxCostFunct.addItem("Least squares");
        comboBoxCostFunct.addItem("Normalized cross correlation");
        comboBoxCostFunct.addItem("Normalized mutual information");
        comboBoxCostFunct.setSelectedIndex(0);
        comboBoxCostFunct.setEnabled(false);
        gbc.gridx = 1;
        gbc.gridy = 1;
        registrationPanel.add(comboBoxCostFunct, gbc);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(registrationPanel,gbc);

        JPanel thresholdPanel = new JPanel(new GridBagLayout());
        thresholdPanel.setForeground(Color.black);
        thresholdPanel.setBorder(buildTitledBorder("Data thresholds"));

        if (useRed) {
            threshold1Label = new JLabel("Red data threshold ");
        }
        else if (useGreen) {
            threshold1Label = new JLabel("Green data threshold ");
        }
        else {
            threshold1Label = new JLabel(firstName + " data threshold ");
        }
        threshold1Label.setForeground(Color.black);
        threshold1Label.setFont(serif12);
        thresholdPanel.add(threshold1Label, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        threshold1Text = new JTextField();
        if ((firstImage.getType() == ModelStorageBase.FLOAT) ||
            (firstImage.getType() == ModelStorageBase.DOUBLE)) {
            threshold1 = (float)firstImage.getMin();
            threshold1Text.setText(String.valueOf(threshold1));
        }
        else {
            threshold1Text.setText("1");
        }
        threshold1Text.setFont(serif12);
        thresholdPanel.add(threshold1Text,gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        if (useBlue) {
            threshold2Label = new JLabel("Blue data threshold ");
        }
        else if (useGreen) {
            threshold2Label = new JLabel("Green data threshold ");
        }
        else {
            threshold2Label = new JLabel(secondName + " data threshold ");
        }
        threshold2Label.setForeground(Color.black);
        threshold2Label.setFont(serif12);
        thresholdPanel.add(threshold2Label, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        threshold2Text = new JTextField();
        if ((secondImage != null) &&
            ((secondImage.getType() == ModelStorageBase.FLOAT) ||
            (secondImage.getType() == ModelStorageBase.DOUBLE))) {
            threshold2 = (float)secondImage.getMin();
            threshold2Text.setText(String.valueOf(threshold2));
        }
        else {
            threshold2Text.setText("1");
        }
        threshold2Text.setFont(serif12);
        thresholdPanel.add(threshold2Text,gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        ButtonGroup thresholdGroup = new ButtonGroup();
        orButton = new JRadioButton("OR", true);
        orButton.setFont(serif12);
        thresholdGroup.add(orButton);
        thresholdPanel.add(orButton, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        andButton = new JRadioButton("AND",false);
        andButton.setFont(serif12);
        thresholdGroup.add(andButton);
        thresholdPanel.add(andButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        mainPanel.add(thresholdPanel,gbc);

        JPanel rescalePanel = new JPanel(new GridBagLayout());
        rescalePanel.setForeground(Color.black);
        rescalePanel.setBorder(buildTitledBorder("Bin numbers"));

        if (useRed) {
            bin1Label = new JLabel("Red bin number ");
        }
        else if (useGreen) {
            bin1Label = new JLabel("Green bin number ");
        }
        else {
            bin1Label = new JLabel(firstName + " bin number ");
        }
        bin1Label.setForeground(Color.black);
        bin1Label.setFont(serif12);
        gbc.gridx = 0;
        gbc.gridy = 0;
        rescalePanel.add(bin1Label, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        bin1Text = new JTextField();
        bin1Text.setText(String.valueOf(bin1));
        bin1Text.setFont(serif12);
        rescalePanel.add(bin1Text,gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        if (useBlue) {
            bin2Label = new JLabel("Blue bin number ");
        }
        else if (useGreen) {
            bin2Label = new JLabel("Green bin number ");
        }
        else {
            bin2Label = new JLabel(secondName + " bin number ");
        }
        bin2Label.setForeground(Color.black);
        bin2Label.setFont(serif12);
        rescalePanel.add(bin2Label, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        bin2Text = new JTextField();
        bin2Text.setText(String.valueOf(bin2));
        bin2Text.setFont(serif12);
        rescalePanel.add(bin2Text,gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(rescalePanel,gbc);

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridBagLayout());
        optionsPanel.setForeground(Color.black);
        optionsPanel.setBorder(buildTitledBorder("Options"));

        gaussianLabel =
            new JLabel("Number of gaussians");
        gaussianLabel.setFont(serif12);
        gaussianLabel.setForeground(Color.black);
        gbc.gridx = 0; gbc.gridy = 0;
        optionsPanel.add(gaussianLabel, gbc);

        gaussianText =
            new JTextField("4");
        gaussianText.setFont(serif12);
        gbc.gridx = 1; gbc.gridy = 0;
        optionsPanel.add(gaussianText, gbc);

        iterationLabel = new JLabel("Number of iterations");
        iterationLabel.setFont(serif12);
        iterationLabel.setForeground(Color.black);
        gbc.gridx = 0; gbc.gridy = 1;
        optionsPanel.add(iterationLabel, gbc);

        iterationText = new JTextField("20");
        iterationText.setFont(serif12);
        gbc.gridx = 1; gbc.gridy = 1;
        optionsPanel.add(iterationText, gbc);


        gbc.gridx = 0; gbc.gridy = 3;
        mainPanel.add(optionsPanel,gbc);

        JPanel imageVOIPanel = new JPanel();
        imageVOIPanel.setLayout(new BorderLayout());
        imageVOIPanel.setForeground(Color.black);
        imageVOIPanel.setBorder(buildTitledBorder("Colocalization region"));

        ButtonGroup imageVOIGroup = new ButtonGroup();
        wholeImage = new JRadioButton("Whole image", true);
        wholeImage.setFont(serif12);
        wholeImage.addItemListener(this);
        imageVOIGroup.add(wholeImage);
        imageVOIPanel.add(wholeImage, BorderLayout.NORTH);

        VOIRegions = new JRadioButton("VOI region", false);
        VOIRegions.setFont(serif12);
        VOIRegions.addItemListener(this);
        imageVOIGroup.add(VOIRegions);
        imageVOIPanel.add(VOIRegions, BorderLayout.CENTER);
        if (nBoundingVOIs != 1) {
            VOIRegions.setEnabled(false);
        }
        gbc.gridx = 0; gbc.gridy = 4;
        mainPanel.add(imageVOIPanel, gbc);


        buildOKButton();
        buildCancelButton();
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(OKButton);
        buttonPanel.add(cancelButton);

        getContentPane().add(imagePanel, BorderLayout.NORTH);
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setVisible(true);
    }

    public void setSecondImage(ModelImage secondImage) {
        this.secondImage = secondImage;
    }

    public void setBin1(int bin1) {
        this.bin1 = bin1;
    }

    public void setBin2(int bin2) {
        this.bin2 = bin2;
    }

    public void setBin1Default(boolean bin1Default) {
        this.bin1Default = bin1Default;
    }

    public void setBin2Default(boolean bin2Default) {
        this.bin2Default = bin2Default;
    }

    public void setThreshold1(float threshold1) {
        this.threshold1 = threshold1;
    }

    public void setThreshold2(float threshold2) {
        this.threshold2 = threshold2;
    }

    public void setDoOr(boolean doOr) {
        this.doOr = doOr;
    }

    public void setLeftPad(int leftPad) {
        this.leftPad = leftPad;
    }

    public void setRightPad(int rightPad) {
        this.rightPad = rightPad;
    }

    public void setBottomPad(int bottomPad) {
        this.bottomPad = bottomPad;
    }

    public void setTopPad(int topPad) {
        this.topPad = topPad;
    }

    public void setUseRed(boolean useRed) {
        this.useRed = useRed;
    }

    public void setUseGreen(boolean useGreen) {
        this.useGreen = useGreen;
    }

    public void setUseBlue(boolean useBlue) {
        this.useBlue = useBlue;
    }

    public void setGaussians(int gaussians) {
        this.gaussians = gaussians;
    }

    public void setEntireImage(boolean entireImage) {
        this.entireImage = entireImage;
    }

    public void setRegister(boolean register) {
        this.register = register;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    /**
    *  Accessor that returns the image.
    *  @return          The result image.
    */
    public ModelImage getResultImage() {
      return resultImage;
    }

    /**
    *  Accessor that returns the image.
    *  @return          The segmented image.
    */
    public ModelImage getSegImage() {
      return segImage;
    }


    /**
    *	Builds a list of images.  Returns combobox.
    *   List must be all color or all black and white.
    *	@return	Newly created combo box.
    */
    private JComboBox buildComboBox(ModelImage image) {
        ViewUserInterface UI;
        ModelImage nextImage;
        boolean doAdd;
        int i;

        JComboBox comboBox = new JComboBox();
        comboBox.setFont(serif12);
        comboBox.setBackground(Color.white);

        UI = image.getUserInterface();
        Enumeration names = UI.getRegisteredImageNames();

        while ( names.hasMoreElements() ) {
            String name = (String)names.nextElement();
            if (!name.equals(image.getImageName())) {
                nextImage = UI.getRegisteredImageByName(name);
                if (UI.getFrameContainingImage(nextImage) != null) {
                    if ((image.isColorImage() == nextImage.isColorImage()) &&
                        (image.getNDims() == nextImage.getNDims())) {
                        doAdd = true;
                        for (i = 0; i < image.getNDims(); i++) {
                            if (image.getExtents()[i] != nextImage.getExtents()[i]) {
                                doAdd = false;
                            }
                        }
                        if (doAdd) {
                            comboBox.addItem(name);
                        }
                    }
                }
            }
        }
        return comboBox;
    }

    /**
   *	Closes dialog box when the OK button is pressed and calls the algorithm.
   *	@param event       Event that triggers function.
   */
   public void actionPerformed(ActionEvent event) {
           String command = event.getActionCommand();

        if (command.equals("OK")) {
            if (setVariables()) {
                callAlgorithm();
            }
        }
        else if (command.equals("Cancel")) {
            dispose();
        }
   }

   /**
    *	Use the GUI results to set up the variables needed to run the algorithm.
    *	@return		<code>true</code> if parameters set successfully, <code>false</code> otherwise.
    */
    private boolean setVariables() {
        String tmpStr;

        entireImage = wholeImage.isSelected();
        if ((!entireImage) && (nBoundingVOIs > 1)) {
            MipavUtil.displayError(
                "Only 1 contour VOI may be present");
                return false;
        }

        tmpStr = gaussianText.getText();
           gaussians = Integer.parseInt(tmpStr);

        tmpStr = iterationText.getText();
        iterations = Integer.parseInt(tmpStr);

        register = regCheckBox.isSelected();

        switch (comboBoxCostFunct.getSelectedIndex()) {
          case 0:
            cost = AlgorithmCostFunctions.CORRELATION_RATIO_SMOOTHED;
            break;
          case 1:
            cost = AlgorithmCostFunctions.LEAST_SQUARES_SMOOTHED;
            break;
            //case 2:  cost = AlgorithmCostFunctions.MUTUAL_INFORMATION_SMOOTHED;             break;
          case 2:
            cost = AlgorithmCostFunctions.NORMALIZED_XCORRELATION_SMOOTHED;
            break;
          case 3:
            cost = AlgorithmCostFunctions.
                NORMALIZED_MUTUAL_INFORMATION_SMOOTHED;
            break;
          default:
            cost = AlgorithmCostFunctions.CORRELATION_RATIO_SMOOTHED;
            break;
        }

        tmpStr = threshold1Text.getText();
           threshold1 = Float.parseFloat(tmpStr);

           tmpStr = threshold2Text.getText();
           threshold2 = Float.parseFloat(tmpStr);

           doOr = orButton.isSelected();

        if (firstImage.isColorImage()) {
            UI = firstImage.getUserInterface();

            if (colorsPresent == 2) {
            }
            else if (((redCheckBox.isSelected()) && (greenCheckBox.isSelected()) &&
                 (!blueCheckBox.isSelected())) || ((redCheckBox.isSelected()) &&
                 (!greenCheckBox.isSelected()) && (blueCheckBox.isSelected())) ||
                ((!redCheckBox.isSelected()) && (greenCheckBox.isSelected()) &&
                 (blueCheckBox.isSelected()))) {
                useRed = redCheckBox.isSelected();
                useGreen = greenCheckBox.isSelected();
                useBlue = blueCheckBox.isSelected();
            }
            else {
                MipavUtil.displayError("Exactly 2 color boxes must be checked");
                return false;
            }

            if (useRed) {
                if (entireImage) {
                    possibleIntValues = firstImage.getMaxR()
                        - firstImage.getMinR() + 1;
                }
                else {
                    possibleIntValues = maxRV - minRV + 1;
                }
            }
            else {
                if (entireImage) {
                    possibleIntValues = firstImage.getMaxG()
                        - firstImage.getMinG() + 1;
                }
                else {
                    possibleIntValues = maxGV - minGV + 1;
                }
            }

            if (useBlue) {
                if (entireImage) {
                    possibleInt2Values = firstImage.getMaxB()
                        - firstImage.getMinB() + 1;
                }
                else {
                    possibleInt2Values = maxBV - minBV + 1;
                }
            }
            else {
                if (entireImage) {
                    possibleInt2Values = firstImage.getMaxG()
                        - firstImage.getMinG() + 1;
                }
                else {
                    possibleInt2Values = maxGV - minGV + 1;
                }
            }


            tmpStr = bin1Text.getText();
               bin1 = Integer.parseInt(tmpStr);
               if (bin1 < 1) {
                   if (useRed) {
                       MipavUtil.displayError("Red must have at least 1 bin");
                   }
                   else {
                       MipavUtil.displayError("Green must have at least 1 bin");
                   }
                   bin1Text.requestFocus();
                   bin1Text.selectAll();
                   return false;
               }

               else if ((bin1 > Math.round(possibleIntValues)) &&
                       ((firstImage.getType() == ModelStorageBase.ARGB) ||
                     (firstImage.getType() == ModelStorageBase.ARGB_USHORT))) {
                if (useRed) {
                    MipavUtil.displayError("Red must not have more than " +
                                       Math.round(possibleIntValues) + " bins");
                }
                else {
                    MipavUtil.displayError("Green must not have more than " +
                                       Math.round(possibleIntValues) + " bins");
                }
                   bin1Text.requestFocus();
                   bin1Text.selectAll();
                   return false;
               }

               tmpStr = bin2Text.getText();
               bin2 = Integer.parseInt(tmpStr);
               if (bin2 < 1) {
                   if (useBlue) {
                       MipavUtil.displayError("Blue must have at least 1 bin");
                   }
                   else {
                       MipavUtil.displayError("Green must have at least 1 bin");
                   }
                   bin2Text.requestFocus();
                   bin2Text.selectAll();
                   return false;
               }
               else if ((bin2 > Math.round(possibleInt2Values)) &&
                    ((firstImage.getType() == ModelStorageBase.ARGB) ||
                     (firstImage.getType() == ModelStorageBase.ARGB_USHORT))) {
                   if (useBlue) {
                    MipavUtil.displayError("Blue must not have more than " +
                                          Math.round(possibleInt2Values) + " bins");
                }
                else {
                    MipavUtil.displayError("Green must not have more than " +
                                          Math.round(possibleInt2Values) + " bins");
                }
                   bin2Text.requestFocus();
                   bin2Text.selectAll();
                   return false;
               }
        } // if (firstImage.isColorImage())
        else { // not color image
            UI = firstImage.getUserInterface();
            String selectedName = (String)imageComboBox.getSelectedItem();
            secondImage   = UI.getRegisteredImageByName(selectedName);
            if (secondImage == null) {
                return false;
            }

            if (entireImage) {
                   possibleIntValues = firstImage.getMax()
                       - firstImage.getMin() + 1;
               }
               else {
                   possibleIntValues = maxV - minV + 1;
               }
            tmpStr = bin1Text.getText();
               bin1 = Integer.parseInt(tmpStr);
               if (bin1 < 1) {
                   MipavUtil.displayError("Image 1 must have at least 1 bin");
                   bin1Text.requestFocus();
                   bin1Text.selectAll();
                   return false;
               }
               else if ((bin1 > Math.round(possibleIntValues)) &&
                       ((firstImage.getType() == ModelStorageBase.BYTE) ||
                     (firstImage.getType() == ModelStorageBase.UBYTE) ||
                     (firstImage.getType() == ModelStorageBase.SHORT) ||
                     (firstImage.getType() == ModelStorageBase.USHORT) ||
                     (firstImage.getType() == ModelStorageBase.INTEGER) ||
                     (firstImage.getType() == ModelStorageBase.UINTEGER) ||
                     (firstImage.getType() == ModelStorageBase.LONG))) {

                MipavUtil.displayError("Image 1 must not have more than " +
                                       Math.round(possibleIntValues) + " bins");
                   bin1Text.requestFocus();
                   bin1Text.selectAll();
                   return false;
               }

               if (entireImage) {
                   possibleInt2Values = secondImage.getMax()
                       - secondImage.getMin() + 1;
               }
               else {
                   possibleInt2Values = secondMaxV - secondMinV + 1;
               }
               tmpStr = bin2Text.getText();
               bin2 = Integer.parseInt(tmpStr);
               if (bin2 < 1) {
                   MipavUtil.displayError("Image 2 must have at least 1 bin");
                   bin2Text.requestFocus();
                   bin2Text.selectAll();
                   return false;
               }
               else if ((bin2 > Math.round(possibleInt2Values)) &&
                    ((secondImage.getType() == ModelStorageBase.BYTE) ||
                     (secondImage.getType() == ModelStorageBase.UBYTE) ||
                     (secondImage.getType() == ModelStorageBase.SHORT) ||
                     (secondImage.getType() == ModelStorageBase.USHORT) ||
                     (secondImage.getType() == ModelStorageBase.INTEGER) ||
                     (secondImage.getType() == ModelStorageBase.UINTEGER) ||
                     (secondImage.getType() == ModelStorageBase.LONG))) {

                MipavUtil.displayError("Image 2 must not have more than " +
                                       Math.round(possibleInt2Values) + " bins");
                   bin2Text.requestFocus();
                   bin2Text.selectAll();
                   return false;
               }
           } // not color image

           return true;
    }

    private void callAlgorithm() {
        String name = makeImageName(firstImage.getImageName(), "_hist2Dim");
        String segName = makeImageName(firstImage.getImageName(), "_seg");
        try {
            int extents[] = new int[2];
            // Allow padding space at left and bottom
            extents[0] = bin1 + leftPad + rightPad;
            extents[1] = bin2 + bottomPad + topPad;
            // Allow log of 1 + counts to be displayed
            resultImage = new ModelImage(ModelStorageBase.DOUBLE, extents,
                                        name, firstImage.getUserInterface());
            segImage = new ModelImage(ModelStorageBase.UBYTE,
                                      firstImage.getExtents(), segName,
                                      firstImage.getUserInterface());


            // Make algorithm
            if (firstImage.isColorImage()) {
                colocalizationAlgo= new AlgorithmColocalizationEM(resultImage,
                                       segImage,
                                       firstImage,
                                       bin1, bin2, threshold1, threshold2, doOr,
                                       leftPad, rightPad,
                                       bottomPad, topPad,
                                       useRed, useGreen, useBlue,
                                       entireImage,
                                       register,cost,gaussians,iterations);
            }
            else {
                colocalizationAlgo= new AlgorithmColocalizationEM(resultImage,
                                       segImage,
                                       firstImage, secondImage,
                                       bin1, bin2, threshold1, threshold2, doOr,
                                       leftPad, rightPad,
                                       bottomPad, topPad,
                                       entireImage,
                                       register,cost,gaussians,iterations);
            }
            // This is very important. Adding this object as a listener allows the algorithm to
            // notify this object when it has completed of failed. See algorithm performed event.
            // This is made possible by implementing AlgorithmedPerformed interface
            colocalizationAlgo.addListener(this);

            // Hide dialog
            setVisible(false);

            if (runInSeparateThread) {
            // Start the thread as a low priority because we wish to still have user interface work fast.
                if (colocalizationAlgo.startMethod(Thread.MIN_PRIORITY) == false){
                    MipavUtil.displayError("A thread is already running on this object");
                }
            }
            else {
              if (!UI.isAppFrameVisible()) {
                colocalizationAlgo.setProgressBarVisible(false);
              }
                colocalizationAlgo.run();
            }
        }
        catch (OutOfMemoryError x){

            if (resultImage != null){
                resultImage.disposeLocal();  // Clean up memory of result image
                resultImage = null;
            }
            System.gc();
            MipavUtil.displayError("Dialog Histogram 2Dim: unable to allocate enough memory");
            return;
        }
    }


    //************************************************************************
    //************************** Algorithm Events ****************************
    //************************************************************************

    /**
    *	This method is required if the AlgorithmPerformed interface is implemented.
    *   It is called by the algorithm when it has completed or failed to to complete,
    *   so that the dialog can be display the result image and/or clean up.
    *   @param algorithm   Algorithm that caused the event.
    */
    public void algorithmPerformed(AlgorithmBase algorithm) {
        insertScriptLine(algorithm);
        dispose();
    }

    //************************* Item Events ****************************
    //*******************************************************************

    /**
    *  itemStateChanged
    */
    public void itemStateChanged(ItemEvent event){
        int i;
        Object source = event.getSource();

        if (source == regCheckBox) {
            if (regCheckBox.isSelected()) {
                labelCost.setEnabled(true);
                comboBoxCostFunct.setEnabled(true);
            }
            else {
                labelCost.setEnabled(false);
                comboBoxCostFunct.setEnabled(false);
            }
        }
        else if ( source == imageComboBox) {
            UI = firstImage.getUserInterface();
            secondName = (String)imageComboBox.getSelectedItem();
            secondImage   = UI.getRegisteredImageByName(secondName);

            if (entireImage) {
                possibleInt2Values = secondImage.getMax()
                    - secondImage.getMin() + 1;
            }
            else {
                try {
                    secondImage.exportData(0,imageLength,buffer);
                }
                catch (IOException e) {
                    MipavUtil.displayError("IOException on secondImage.exportData");
                    return;
                }
                secondMinV = Double.MAX_VALUE;
                secondMaxV = -Double.MAX_VALUE;
                for (i = 0; i < imageLength; i++) {
                    if (mask.get(i)) {
                        if (buffer[i] < secondMinV) {
                            secondMinV = buffer[i];
                        }
                        if (buffer[i] > secondMaxV) {
                            secondMaxV = buffer[i];
                        }
                    }
                }
                possibleInt2Values = secondMaxV - secondMinV + 1;
            }

            bin2 = 256;
            if ((bin2 > Math.round(possibleInt2Values)) &&
                ((secondImage.getType() == ModelStorageBase.BYTE) ||
                 (secondImage.getType() == ModelStorageBase.UBYTE) ||
                 (secondImage.getType() == ModelStorageBase.SHORT) ||
                 (secondImage.getType() == ModelStorageBase.USHORT) ||
                 (secondImage.getType() == ModelStorageBase.INTEGER) ||
                 (secondImage.getType() == ModelStorageBase.UINTEGER) ||
                 (secondImage.getType() == ModelStorageBase.LONG))) {
                bin2 = (int)Math.round(possibleInt2Values);
            }
            threshold2Label.setText(secondName + " data threshold ");
            bin2Label.setText(secondName + " bin number ");
            bin2Text.setText(String.valueOf(bin2));
        } // if ( source == imageComboBox)
        else if ((source == wholeImage) || (source == VOIRegions)) {
            entireImage = wholeImage.isSelected();
            if (firstImage.isColorImage()) {
                bin1 = 256;
                if (useRed) {
                    if (entireImage) {
                        possibleIntValues = firstImage.getMaxR()
                            - firstImage.getMinR() + 1;
                    }
                    else {
                        possibleIntValues = maxRV - minRV + 1;
                    }
                }
                else {
                    if (entireImage) {
                        possibleIntValues = firstImage.getMaxG()
                            - firstImage.getMinG() + 1;
                    }
                    else {
                        possibleIntValues = maxGV - minGV + 1;
                    }
                }
                if (((firstImage.getType() == ModelStorageBase.ARGB) ||
                     (firstImage.getType() == ModelStorageBase.ARGB_USHORT)) &&
                     (possibleIntValues < 256)) {
                    bin1 = (int)Math.round(possibleIntValues);
                }

                bin2 = 256;
                if (useBlue) {
                    if (entireImage) {
                        possibleInt2Values = firstImage.getMaxB()
                            - firstImage.getMinB() + 1;
                    }
                    else {
                        possibleInt2Values = maxBV - minBV + 1;
                    }
                }
                else {
                    if (entireImage) {
                        possibleInt2Values = firstImage.getMaxG()
                            - firstImage.getMinG() + 1;
                    }
                    else {
                        possibleInt2Values = maxGV - minGV + 1;
                    }
                }
                if (((firstImage.getType() == ModelStorageBase.ARGB) ||
                     (firstImage.getType() == ModelStorageBase.ARGB_USHORT)) &&
                     (possibleInt2Values < 256)) {
                    bin2 = (int)Math.round(possibleInt2Values);
                }

                bin1Text.setText(String.valueOf(bin1));
                bin2Text.setText(String.valueOf(bin2));
            } // if (firstImage.isColorImage())
            else { // black and white
                if (entireImage) {
                    possibleIntValues = firstImage.getMax()
                                        - firstImage.getMin() + 1;
                    possibleInt2Values = secondImage.getMax()
                                         - secondImage.getMin() + 1;
                }
                else {
                    possibleIntValues = maxV - minV + 1;
                    possibleInt2Values = secondMaxV - secondMinV + 1;
                }
                bin1 = 256;
                if (((firstImage.getType() == ModelStorageBase.BYTE) ||
                 (firstImage.getType() == ModelStorageBase.UBYTE) ||
                 (firstImage.getType() == ModelStorageBase.SHORT) ||
                 (firstImage.getType() == ModelStorageBase.USHORT) ||
                 (firstImage.getType() == ModelStorageBase.INTEGER) ||
                 (firstImage.getType() == ModelStorageBase.UINTEGER) ||
                 (firstImage.getType() == ModelStorageBase.LONG)) &&
                 (possibleIntValues < 256)) {
                    bin1 = (int)Math.round(possibleIntValues);
                }
                bin1Text.setText(String.valueOf(bin1));

                bin2 = 256;
                if ((bin2 > Math.round(possibleInt2Values)) &&
                ((secondImage.getType() == ModelStorageBase.BYTE) ||
                 (secondImage.getType() == ModelStorageBase.UBYTE) ||
                 (secondImage.getType() == ModelStorageBase.SHORT) ||
                 (secondImage.getType() == ModelStorageBase.USHORT) ||
                 (secondImage.getType() == ModelStorageBase.INTEGER) ||
                 (secondImage.getType() == ModelStorageBase.UINTEGER) ||
                 (secondImage.getType() == ModelStorageBase.LONG))) {
                    bin2 = (int)Math.round(possibleInt2Values);
                }
                bin2Text.setText(String.valueOf(bin2));
            } // else black and white
        } // else if ((source == wholeImage) || (source == VOIRegions))
        else if ((colorsPresent == 3) &&
                 ((source == redCheckBox) || (source == greenCheckBox) ||
                 (source == blueCheckBox))) {
            // Only process if 2 checkBoxes are selected and 1 is not selected
            if (((redCheckBox.isSelected()) && (greenCheckBox.isSelected()) &&
                 (!blueCheckBox.isSelected())) || ((redCheckBox.isSelected()) &&
                 (!greenCheckBox.isSelected()) && (blueCheckBox.isSelected())) ||
                ((!redCheckBox.isSelected()) && (greenCheckBox.isSelected()) &&
                 (blueCheckBox.isSelected()))) {
                useRed = redCheckBox.isSelected();
                useGreen = greenCheckBox.isSelected();
                useBlue = blueCheckBox.isSelected();
                bin1 = 256;
                if (useRed) {
                    if (entireImage) {
                        possibleIntValues = firstImage.getMaxR()
                            - firstImage.getMinR() + 1;
                    }
                    else {
                        possibleIntValues = maxRV - minRV + 1;
                    }
                }
                else {
                    if (entireImage) {
                        possibleIntValues = firstImage.getMaxG()
                            - firstImage.getMinG() + 1;
                    }
                    else {
                        possibleIntValues = maxGV - minGV + 1;
                    }
                }
                if (((firstImage.getType() == ModelStorageBase.ARGB) ||
                     (firstImage.getType() == ModelStorageBase.ARGB_USHORT)) &&
                     (possibleIntValues < 256)) {
                    bin1 = (int)Math.round(possibleIntValues);
                }

                bin2 = 256;
                if (useBlue) {
                    if (entireImage) {
                        possibleInt2Values = firstImage.getMaxB()
                            - firstImage.getMinB() + 1;
                    }
                    else {
                        possibleInt2Values = maxBV - minBV + 1;
                    }
                }
                else {
                    if (entireImage) {
                        possibleInt2Values = firstImage.getMaxG()
                            - firstImage.getMinG() + 1;
                    }
                    else {
                        possibleInt2Values = maxGV - minGV + 1;
                    }
                }
                if (((firstImage.getType() == ModelStorageBase.ARGB) ||
                     (firstImage.getType() == ModelStorageBase.ARGB_USHORT)) &&
                     (possibleInt2Values < 256)) {
                    bin2 = (int)Math.round(possibleInt2Values);
                }

                bin1Text.setText(String.valueOf(bin1));
                bin2Text.setText(String.valueOf(bin2));
                if (useRed) {
                    bin1Label.setText("Red bin number ");
                }
                else {
                    bin1Label.setText("Green bin number ");
                }
                if (useBlue) {
                    bin2Label.setText("Blue bin number ");
                }
                else {
                    bin2Label.setText("Green bin number ");
                }
            }
        }
    }

}
