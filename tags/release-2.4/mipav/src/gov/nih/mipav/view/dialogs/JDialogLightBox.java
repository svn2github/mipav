package gov.nih.mipav.view.dialogs;

import gov.nih.mipav.view.*;

import java.util.*;
import java.awt.event.*;
import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.BevelBorder;

/**
*   This class builds the control dialog used in ViewJFrameLightbox class.
*   Widgets are created and added to the panel and the listener. This dialog
*   will allow the user to set the number of rows or columns, the color of the
*   borders and the background, the magnification of the images and if its a
*   4D image, the 4th dimension slider.
*
*		@version    1.0 June 31st, 1999
*		@author     Matthew J. McAuliffe, Ph.D. annd Tun Jie, M.D.
*       @see        ViewJFrameLightBox
*/
public class JDialogLightBox extends JDialogBase implements ChangeListener
{

    /**
    *   definitions indicating that user input has been validated
    */
    private static int  VALID_INPUT     = 0;
    private static int  SAME_INPUT      = 1;
    private static int  INVALID_INPUT   = 2;

    private     ViewJFrameLightBox  frame;                          // LightBox frame
    private     Font                font12, font12B;

    private     JPanel              panelRowColumn;
	private     JRadioButton        radioRow;
	private     JRadioButton        radioColumn;
	private     boolean             row_dependent;
	private     JPanel              rowPanel;
	private     JPanel              colPanel;
	private     JLabel              rowLabel;
	private     JLabel              colLabel;
	private     JTextField          rowText;
	private     JTextField          colText;
    private     JTextField          gridsizeText;
    private     JLabel              gridsizeL;
    private     JTextField          bordersizeText;
    private     JLabel              bordersizeL;
    private     JPanel              colorPanel;
	private     JButton             borderB, backgroundB;

    private     JPanel              magSliderPanel;
    private     JSlider             magSlider;
    private     JWindow             magniWindow;    // a "tooltip"
    private     JLabel              toolTip;
	private     Hashtable           magSliderDictionary = new Hashtable();
    private     JPanel              tSliderPanel;
    private     JSlider             tSlider;
	private     Hashtable           tSliderDictionary = new Hashtable();

	private     ViewJColorChooser   colorChooser;

	private     JButton             resetButton;

	// values obtained from settings
	private     boolean             rowBFlag;
	private     int                 row;
	private     int                 col;
	private     int                 gridSize;
	private     int                 borderSize;
	private     Color               borderColor;
	private     Color               bgColor;
	private     int                 tSlice;
	private     float               magnification;

	private     boolean             lastRowBFlag;
	private     int                 lastRow;
	private     int                 lastCol;
	private     Color               lastBorderColor;
	private     Color               lastBgColor;
	private     int                 lastGridSize;
	private     int                 lastBorderSize;
	private     int                 lastTSlice;
	private     float               lastMagnification;

    /**
    *  constructs new control dialog of the Lightbox View
    *  @param  _frame   reference to the lightbox frame
    */
	public JDialogLightBox(ViewJFrameLightBox _frame)  {

	    super(_frame, false);
		setTitle("Lightbox Settings");
	    font12  = MipavUtil.font12;
        font12B = MipavUtil.font12B;

        frame          = _frame;
        buildDialog();
	}

	/**
	*    setup the lightbox's control dialog. Sets
	*    the row/column selector, color, magnification slider,
	*    and, if needed, 4th dimension slider.
	*/
	public void buildDialog() {
	    JPanel centerPanel;
	    GridBagConstraints  gbc;

	    try {
	        gbc         = new GridBagConstraints();
	        centerPanel = new JPanel(new GridBagLayout());

	        buildRowColPanel();
	        gbc.gridwidth  = 1;
            gbc.gridheight = 3;
            gbc.gridx      = 0;
            gbc.gridy      = 0;
            gbc.fill       = GridBagConstraints.BOTH;
            gbc.anchor     = GridBagConstraints.WEST;
            gbc.insets     = new Insets(0, 0, 0, 0);
            gbc.weightx    = 1.0;
            gbc.weighty    = 1.0;
    	    centerPanel.add(panelRowColumn, gbc);

    	    buildColorPanel();
    	    gbc.gridwidth  = 1;
            gbc.gridheight = 1;
            gbc.gridx      = 0;
            gbc.gridy      = 4;
            gbc.fill       = GridBagConstraints.BOTH;
            gbc.anchor     = GridBagConstraints.WEST;
            gbc.insets     = new Insets(0, 0, 0, 0);
            gbc.weightx    = 1.0;
            gbc.weighty    = 1.0;
    	    centerPanel.add(colorPanel, gbc);

    	    buildMagSlider();
    	    gbc.gridwidth  = 1;
            gbc.gridheight = 1;
            gbc.gridx      = 0;
            gbc.gridy      = 5;
            gbc.fill       = GridBagConstraints.BOTH;
            gbc.anchor     = GridBagConstraints.WEST;
            gbc.insets     = new Insets(0, 0, 0, 0);
            gbc.weightx    = 1.0;
            gbc.weighty    = 1.0;
    	    centerPanel.add(magSliderPanel, gbc);


            if (frame.getNumTSlices() > 1) {
                buildTSlider();
                gbc.gridwidth  = 1;
                gbc.gridheight = 1;
                gbc.gridx      = 0;
                gbc.gridy      = 6;
                gbc.fill       = GridBagConstraints.BOTH;
                gbc.anchor     = GridBagConstraints.WEST;
                gbc.insets     = new Insets(0, 0, 0, 0);
                gbc.weightx    = 1.0;
                gbc.weighty    = 1.0;
    	        centerPanel.add(tSliderPanel, gbc);
            }

    	    getContentPane().add(centerPanel, BorderLayout.CENTER);

	        JPanel  buttonsPanel = new JPanel();
	        buildOKButton();
	        OKButton.setText("Apply");
	        OKButton.addActionListener(this);
	        resetButton = new JButton("Reset");
	        resetButton.setActionCommand("Reset");
	        resetButton.setFont(font12B);
	        resetButton.setPreferredSize(MipavUtil.defaultButtonSize);
	        resetButton.setMinimumSize(MipavUtil.defaultButtonSize);
	        resetButton.addActionListener(this);
	        buildCancelButton();
	        cancelButton.setText("Close");
	        buttonsPanel.add(OKButton);
	        buttonsPanel.add(resetButton);
	        buttonsPanel.add(cancelButton);
	        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

	        pack();

	        int xScreen = Toolkit.getDefaultToolkit().getScreenSize().width;
            int yScreen = Toolkit.getDefaultToolkit().getScreenSize().height;
	        setLocation(xScreen/2-getSize().width/2, yScreen/2-getSize().height/2);
	    }
	    catch (OutOfMemoryError error) {
	        System.gc();
	        throw error;
	    }
	}


	/**
	*  sets up the row and column panel. Displays the current
	*  setting of rows and columns in the light box takes input
	*  from the text field, radio buttons determine which
	*  parameter to be changed modifies the layout of lightbox.
	*/
	private void buildRowColPanel() {
        ButtonGroup     g1;
        String          label;
        JPanel          rcPanel, gridPanel, borderPanel;

        panelRowColumn = new JPanel();
        panelRowColumn.setLayout(new BorderLayout());
        panelRowColumn.setForeground(Color.black);
        panelRowColumn.setBorder(buildTitledBorder("Lightbox layout settings"));

        rcPanel = new JPanel();
        rcPanel.setLayout(new GridLayout(2, 2, 5, 1));
        rcPanel.setForeground(Color.black);
        rcPanel.setBorder(buildTitledBorder("Row/column"));

        g1 = new ButtonGroup();
        row_dependent = frame.getRowDependent();
        if (row_dependent) {  // columns are independent
		    radioRow = new JRadioButton("Row", false);
            radioColumn = new JRadioButton("Column", true);
		    rowBFlag = false;
		}
		else {  //rows are independent
		    radioRow = new JRadioButton("Row", true);
            radioColumn = new JRadioButton("Column", false);
		    rowBFlag = true;
		}
		//radioRow.setActionCommand("Row");
		radioRow.setFont(font12B);
		radioRow.setToolTipText("Display Lightbox by Rows");
		g1.add(radioRow);
		rcPanel.add(radioRow);

        //radioColumn.setActionCommand("Column");
		radioColumn.setFont(font12B);
		radioColumn.setToolTipText("Display Lightbox by Columns");
		g1.add(radioColumn);
        rcPanel.add(radioColumn);

        row = frame.getGridRow();
        col = frame.getGridColumn();

        // initially the last row and col are the same
        lastRow = row;
        lastCol = col;

        // build row label and text field
        rowPanel = new JPanel(new FlowLayout());
        rowLabel = new JLabel("No. Rows: ", SwingConstants.LEFT);
        rowLabel.setFont(font12);
        rowLabel.setForeground(Color.black);
        rowText = new JTextField(Integer.toString(row), 4);
        rowText.setFont(font12);
        rowText.setBackground(Color.white);
        rowText.setForeground(Color.black);
        rowText.setToolTipText("A value of 0, means to automatically find best fit");
        rowPanel.add(rowLabel);
        rowPanel.add(rowText);
        rcPanel.add(rowPanel);

        // build col label and text field
        colPanel = new JPanel(new FlowLayout());
        colLabel = new JLabel("No. Columns: ", SwingConstants.LEFT);
        colLabel.setFont(font12);
        colLabel.setForeground(Color.black);
        colText = new JTextField(Integer.toString(col), 4);
        colText.setFont(font12);
        colText.setBackground(Color.white);
        colText.setForeground(Color.black);
        colText.setToolTipText("A value of 0, means to automatically find best fit");
        colPanel.add(colLabel);
        colPanel.add(colText);
        rcPanel.add(colPanel);

        panelRowColumn.add(rcPanel, "North");

        // Grid Size
        gridPanel = new JPanel();
        gridPanel.setLayout(new GridLayout(1, 2, 5, 1));
        gridPanel.setForeground(Color.black);
        gridPanel.setBorder(buildTitledBorder("Grid size"));

        gridSize = frame.getGridSpacing();
        lastGridSize = gridSize;

        label = "" + gridSize;
        gridsizeText = new JTextField(label, 5);
        gridPanel.add(gridsizeText);

        label = "Grid spacing = " + gridSize;
        gridsizeL = new JLabel(label);
        gridsizeL.setFont(font12);
        gridsizeL.setForeground(Color.black);

        gridPanel.add(gridsizeL);
        panelRowColumn.add(gridPanel, "Center");

        // Border Size
        borderPanel = new JPanel();
        borderPanel.setLayout(new GridLayout(1, 2, 5, 1));
        borderPanel.setForeground(Color.black);
        borderPanel.setBorder(buildTitledBorder("Frame border size"));

        borderSize = frame.getBorderSize();
        lastBorderSize = borderSize;

        label = "" + borderSize;
        bordersizeText = new JTextField(label, 5);
        borderPanel.add(bordersizeText);

        label = "Border size = " + borderSize;
        bordersizeL = new JLabel(label);
        bordersizeL.setFont(font12);
        bordersizeL.setForeground(Color.black);

        borderPanel.add(bordersizeL);
        panelRowColumn.add(borderPanel, "South");

        // make sure that row/col radio buttons are intialized right
        radioRow.setSelected(rowBFlag);
        radioColumn.setSelected(!rowBFlag);

	}

	/**
	*   builds the controls to set the color of the border
	*   the and color of the background.
	*/
	private void buildColorPanel() {
	    JLabel      l1, l2;

	    colorPanel = new JPanel();
        colorPanel.setLayout(new GridLayout(2,2));
        colorPanel.setForeground(Color.black);
        colorPanel.setBorder(buildTitledBorder("Color settings"));

	    bgColor = frame.getGridColor();
	    lastBgColor = bgColor;

	    l1 = new JLabel("Background color:");
	    l1.setFont(font12);
	    l1.setForeground(Color.black);
	    colorPanel.add(l1);
	    backgroundB = new JButton();
	    backgroundB.setActionCommand("background color");
	    backgroundB.setBackground(bgColor);
	    backgroundB.addActionListener(this);
	    colorPanel.add(backgroundB);

	    borderColor = frame.getBorderColor();
	    lastBorderColor = borderColor;

	    l2 = new JLabel("Border color:");
	    l2.setFont(font12);
	    l2.setForeground(Color.black);
	    colorPanel.add(l2);
	    borderB = new JButton();
	    borderB.setActionCommand("border color");
	    borderB.setBackground(borderColor);
	    borderB.addActionListener(this);
	    colorPanel.add(borderB);
	}


	/**
	*   method to set the time (4th dimension) slider if it exists.
	*   @param tSlice   time slice that the slider should be set to.
	*/
	public void setTSlider(int tSlice) {
	    int newValue;

	    if (tSlider == null) return;

	    if (frame.getImageA().getNDims() == 4) {
	      newValue = Math.round(100.0f * tSlice/(frame.getImageA().getExtents()[3]-1) - 0.01f);
	    }
	    else {
	      newValue = Math.round(100.0f * tSlice/(frame.getImageB().getExtents()[3]-1) - 0.01f);
	    }
	    tSlider.removeChangeListener(this);
	    tSlider.setValue(newValue);
	    tSlider.addChangeListener(this);
	}

	/**
	*   method to set the row, col, and row_dependent values.
	*/
	public void setValues (boolean row_dependent, int row, int col) {

        this.row_dependent = row_dependent;
        if (row_dependent) {
		    radioRow.setSelected(false);
            radioColumn.setSelected(true);
		    rowBFlag = false;
		}
		else {
		    radioRow.setSelected(true);
            radioColumn.setSelected(false);
		    rowBFlag = true;
		}

        rowText.setText(Integer.toString(row));
        colText.setText(Integer.toString(col));

	} // end setValues()

	/**
	*   builds the time (4th dimension) slider.
	*/
	private void buildTSlider() {
        tSliderPanel = new JPanel();
        tSliderPanel.setLayout(new BorderLayout());
        tSliderPanel.setForeground(Color.black);
        tSliderPanel.setBorder(buildTitledBorder("Time slices"));

        tSlice = frame.getCurrentTSlice();
        lastTSlice = tSlice;

        buildTSliderLabels(1, frame.getImageA().getExtents()[3]);
        tSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        if (frame.getImageA().getNDims() == 4) {
          tSlider.setMinorTickSpacing(Math.round(100.0f/(frame.getImageA().getExtents()[3]-1)));
        }
        else {
          tSlider.setMinorTickSpacing(Math.round(100.0f/(frame.getImageB().getExtents()[3]-1)));
        }
        tSlider.setSnapToTicks(true);
        tSlider.setPaintTicks(true);
        tSlider.setPaintLabels(true);

        tSlider.setLabelTable(tSliderDictionary);
        tSliderPanel.add(tSlider);
        tSlider.addChangeListener(this);
	}

	/** builds the image magnification slider. */
	private void buildMagSlider() {
	    float m_max = 0.0f, m_min = 0.0f;

        magSliderPanel = new JPanel();
        magSliderPanel.setLayout(new BorderLayout());
        magSliderPanel.setForeground(Color.black);
        magSliderPanel.setBorder(buildTitledBorder("Magnification"));

        m_max = frame.getMagMax();
        m_min = frame.getMagMin();

        magnification = frame.getMagnification();
        lastMagnification = magnification;

        buildMagSliderLabels(m_min, m_max);
        magSlider = new JSlider(JSlider.HORIZONTAL, Math.round(m_min),
                           Math.round(m_max), Math.round(magnification));

        magSlider.setMajorTickSpacing( Math.round((m_max - m_min)/4.0f) );
        magSlider.setMinorTickSpacing( Math.round((m_max - m_min)/20.0f));
        magSlider.setPaintTicks(true);
        magSlider.setPaintLabels(true);
        magSlider.setLabelTable(magSliderDictionary);
        magSliderPanel.add(magSlider);
        magSlider.addChangeListener(this);

        magniWindow = new JWindow();
        toolTip = new JLabel(Integer.toString(magSlider.getValue())+"%");
        toolTip.setBackground(Color.yellow);
        toolTip.setForeground(Color.black);
        toolTip.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        magniWindow.getContentPane().add(toolTip);
        magniWindow.pack();

	}

    /**
    *   builds labels used by the time slider
    *   @param min   minimum value for the slider
    *   @param max   maximum value for the slider
    */
    private void buildTSliderLabels(int min, int max) {
        String s;

        float rangeF = (max)/4.0f;

        s = "" + min;
        JLabel label1 = new JLabel(s);
        label1.setForeground(Color.black);
        label1.setFont(font12);
        tSliderDictionary.put(new Integer(0), label1);

        if ((max - min) > 3){
            JLabel label2 = new JLabel(Integer.toString(Math.round(rangeF*2)));
            label2.setForeground(Color.black);
            label2.setFont(font12);
            tSliderDictionary.put(new Integer(50), label2);
        }

        JLabel label5 = new JLabel(Integer.toString(max));
        label5.setForeground(Color.black);
        label5.setFont(font12);
        tSliderDictionary.put(new Integer(100), label5);
    }

    /**
    *   builds labels used by the magnification slider
    *   @param min   minimum value for the slider
    *   @param max   maximum value for the slider
    */
    private void buildMagSliderLabels(float min, float max) {
        String  s;
        float     range = 0.0f, midpoint=0.0f;


        range = max - min;
        midpoint = range/2 + min;

        s = "" + Math.round(min) + "%";
        JLabel label1 = new JLabel(s);
        label1.setForeground(Color.black);
        label1.setFont(font12);
        magSliderDictionary.put(new Integer(Math.round(min)), label1);

        if ((range) > 10.0){
            s = "" + Math.round(midpoint) + "%";
            JLabel label2 = new JLabel(s);
            label2.setForeground(Color.black);
            label2.setFont(font12);
            magSliderDictionary.put(new Integer(Math.round(midpoint)), label2);
        }
        s = "" + Math.round(max) + "%";
        JLabel label5 = new JLabel(s);
        label5.setForeground(Color.black);
        label5.setFont(font12);
        magSliderDictionary.put(new Integer(Math.round(max)), label5);
    }


    /** checks the settings of light box row/column controls  */
    private int checkRCTextField ()
    {
        int size=0;

        // first set the "last" values to the unchanged values
        lastRow = row;
        lastCol = col;
        lastRowBFlag = rowBFlag;

        size = frame.getNumTotalSlices();

        try {
            row = Integer.parseInt(rowText.getText());
            col = Integer.parseInt(colText.getText());

            // get the rowBFlag
            if (radioRow.isSelected())
                rowBFlag = true;
            else
                rowBFlag = false;

            // if values haven't changed, return same input
            if (row == lastRow && col == lastCol && rowBFlag == lastRowBFlag)
                return SAME_INPUT;

            if (row > size || col > size) {
                MipavUtil.displayError ("Too many rows or columns.");
                row = lastRow;
                col = lastCol;
                rowBFlag = lastRowBFlag;
                rowText.setText(Integer.toString(row));
                colText.setText(Integer.toString(col));
                radioRow.setSelected(rowBFlag);
                return INVALID_INPUT;
            }
        }
        catch (NumberFormatException error)
        {
            MipavUtil.displayError("Must enter an integer for row and column");
            row = lastRow;
            col = lastCol;
            rowBFlag = lastRowBFlag;
            rowText.setText(Integer.toString(row));
            colText.setText(Integer.toString(col));
            radioRow.setSelected(rowBFlag);
            return INVALID_INPUT;
        }

        if (rowBFlag == true)
            row_dependent = false;
        else
            row_dependent = true;

        return VALID_INPUT;
    }


    /** checks the settings of grid size controls */
    private int checkGSTextField ()
    {
        String s;

        // set the 'last' gridSize
        lastGridSize = gridSize;

        try {
            gridSize = Integer.parseInt(gridsizeText.getText());

            // if values haven't changed, return same input
            if (gridSize == lastGridSize)
                return SAME_INPUT;

            if (gridSize < 0) {
                MipavUtil.displayError("Invalid Grid Size setting");
                gridSize = lastGridSize;
                s = "Grid Spacing = " + gridSize;
                gridsizeL.setText(s);
                return INVALID_INPUT;
            }
            else if (gridSize > ViewJFrameLightBox.MAX_GRID_SIZE) {
                MipavUtil.displayError("Grid Spacing setting out of bound (0 - " +
                                        ViewJFrameLightBox.MAX_GRID_SIZE + ")");
                gridSize = lastGridSize;
                s = "Grid Spacing = " + gridSize;
                gridsizeL.setText(s);
                return INVALID_INPUT;
            }

        }
        catch (NumberFormatException error) {
            MipavUtil.displayError("Must enter an integer for grid size");
            gridSize = lastGridSize;
            s = "Grid Spacing = " + gridSize;
            gridsizeL.setText(s);
            return INVALID_INPUT;
        }

        s = "Grid Spacing = " + gridSize;
        gridsizeL.setText(s);
        //gridsizeText.setText(Integer.toString(gridSize));
        return VALID_INPUT;

    }


    /** check border size of the frame, range (0,10) */
    private int checkBSTextField ()
    {
        String s;

        // set the 'last' borderSize
        lastBorderSize = borderSize;

        try {
            borderSize = Integer.parseInt(bordersizeText.getText());

            // if values haven't changed, return same input
            if (borderSize == lastBorderSize)
                return SAME_INPUT;

            if (borderSize < 0) {
                MipavUtil.displayError("Invalid Border Size setting");
                borderSize = lastBorderSize;
                s = "Border Size = " + borderSize;
                bordersizeL.setText(s);
                return INVALID_INPUT;
            }
            else if (borderSize > ViewJFrameLightBox.MAX_GRID_BORDER) {
                MipavUtil.displayError("Border Size setting out of bound (0 - " +
                                        ViewJFrameLightBox.MAX_GRID_BORDER + ")");
                borderSize = lastBorderSize;
                s = "Light Box Border Size = " + borderSize;
                bordersizeL.setText(s);
                return INVALID_INPUT;
            }

        }
        catch (NumberFormatException error) {
            MipavUtil.displayError("Must enter an integer for border size");
            borderSize = lastBorderSize;
            s = "Border Size = " + borderSize;
            bordersizeL.setText(s);
            return INVALID_INPUT;
        }

        s = "Border Size = " + borderSize;
        bordersizeL.setText(s);
        return VALID_INPUT;

    }


    //**************************************************************************
    //*************************** Change listener ******************************
    //**************************************************************************

    /**
    *  @param event       event that triggers function
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        if (command.equalsIgnoreCase("border color")){
            colorChooser = new ViewJColorChooser(frame,
                                             "Pick border color",
                                             new OkBorderListener(),
                                             new CancelListener());
        }

        else if (command.equalsIgnoreCase("background color")) {
            colorChooser = new ViewJColorChooser(frame,
                                             "Pick background color",
                                             new OkBgListener(),
                                             new CancelListener());
        }

        else if (command.equalsIgnoreCase("apply")){
            int rcStatus = checkRCTextField();
            if (rcStatus == INVALID_INPUT)
                return;

            int gsStatus = checkGSTextField();
            if (gsStatus == INVALID_INPUT)
                return;

            int bsStatus = checkBSTextField();
            if (bsStatus == INVALID_INPUT)
                return;

            // everything is valid, now update frame
            if (rcStatus == VALID_INPUT)
                frame.updateRowsColumns (row_dependent, row, col);
            if (gsStatus == VALID_INPUT)
                frame.updateGridSpacing(gridSize);
            if (bsStatus == VALID_INPUT)
                frame.updateBorderSize(borderSize);
        }

        else if (command.equalsIgnoreCase("reset")) {
            resetValues();
        }
        else if (command.equalsIgnoreCase("close")) {
            setVisible(false);
            resetValues();
        }

    } // end actionPerformed()

    /**
    *    Resets the values to match the current lightbox.  The magnification
    *    and time slider are not reset, since changes to those fields automatically
    *    update in the lightbox frame.
    */
    public void resetValues() {

        int row, col;
        row_dependent = frame.getRowDependent();
        row = frame.getGridRow();
        col = frame.getGridColumn();

        rowText.setText (Integer.toString(row));
        colText.setText (Integer.toString(col));
        if (row_dependent) {
            rowBFlag = false;
            radioRow.setSelected(false);
            radioColumn.setSelected(true);
        }
        else {
            rowBFlag = true;
            radioRow.setSelected(true);
            radioColumn.setSelected(false);

        }
        gridsizeText.setText(Integer.toString(frame.getGridSpacing()));
        bordersizeText.setText(Integer.toString(frame.getBorderSize()));
        backgroundB.setBackground(frame.getGridColor());
        borderB.setBackground(frame.getBorderColor());

    } // end resetValues()

    /**
    *   pick up the selected color and call method to change the grid color.
    *   For use by the color chooser.
    */
    class OkBgListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            lastBgColor = bgColor;
            bgColor = colorChooser.getColor();
            backgroundB.setBackground(bgColor);
            frame.updateGridColor(bgColor);
        }
    }


    /**
    *   Pick up the selected color and change the image border color
    *   For use by the color chooser.
    */
    class OkBorderListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            lastBorderColor = borderColor;
            borderColor = colorChooser.getColor();
            borderB.setBackground(borderColor);
            frame.updateBorderColor(borderColor);
        }
    }

    /**
    *   does nothing at the moment
    *
    */
    class CancelListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
        }
    }

    //**************************************************************************
    //*************************** Change listener ******************************
    //**************************************************************************

    /**
    *    sets values based on knob along slider
    *    @param ChangeEvent  event that triggered this function
    */
    public void stateChanged(ChangeEvent e){
        Object source = e.getSource();
        int newValue = 1;

        // Slider has changed lets update.
        if (source == magSlider){
            toolTip.setText(Integer.toString(magSlider.getValue())+"%");
            magniWindow.pack();
            if (magSlider.getValueIsAdjusting() == true)
                return;

            magSlider.setToolTipText(Integer.toString(magSlider.getValue())+"%");
            magSliderPanel.setToolTipText(Integer.toString(magSlider.getValue())+"%");
            lastMagnification = magnification;
            magnification = magSlider.getValue();
            frame.updateMagnification((int)magnification);
        }

        else if (source == tSlider){

            if (tSlider.getValueIsAdjusting() == true)
                return;

            lastTSlice = tSlice;
            tSlice = tSlider.getValue();

            if (frame.getImageA().getNDims() == 4) {
              newValue = Math.round(tSlider.getValue()/100.0f *
                                   (frame.getImageA().getExtents()[3]-1) - 0.01f );
            }
            else {
              newValue = Math.round(tSlider.getValue()/100.0f *
                                   (frame.getImageB().getExtents()[3]-1) - 0.01f );
            }
            tSlider.setToolTipText(Integer.toString(newValue+1));
            tSliderPanel.setToolTipText(Integer.toString(newValue+1));
            frame.getImageA().setTimeSlice(newValue);
        }
    } // end stateChanged()

}




