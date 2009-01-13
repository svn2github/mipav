import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

import gov.nih.mipav.model.algorithms.AlgorithmBase;
import gov.nih.mipav.model.algorithms.AlgorithmInterface;
import gov.nih.mipav.model.file.DicomDictionary;
import gov.nih.mipav.model.file.FileDicom;
import gov.nih.mipav.model.file.FileDicomKey;
import gov.nih.mipav.model.file.FileDicomSQ;
import gov.nih.mipav.model.file.FileDicomTag;
import gov.nih.mipav.model.file.FileDicomTagTable;
import gov.nih.mipav.model.file.FileInfoBase;
import gov.nih.mipav.model.file.FileInfoDicom;
import gov.nih.mipav.plugins.JDialogStandaloneScriptablePlugin;
import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.Preferences;
import gov.nih.mipav.view.ViewJFrameGraph;
import gov.nih.mipav.view.ViewUserInterface;
import gov.nih.mipav.view.dialogs.JDialogBase;
import gov.nih.mipav.view.dialogs.JDialogScriptableBase;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.Vector;

/**
 * @author joshim2
 *
 */
public class PlugInDialogImageSubmit extends JDialogStandaloneScriptablePlugin implements AlgorithmInterface {

	// ~ Static fields -------------------------------------------------------------------------
	
	public static final String BROWSE_FILE_INPUT = "Browse file input";
	
	public static final String BROWSE_FILE_SUBMIT = "Browse file submit";

	public static final String BROWSE_AVAILABLE_TAGS = "Browse available tags";
	
	public static final String REMOVE = "Remove file";
	
	public static final String REMOVE_ALL = "Remove all file";
	
	// ~ Instance fields ------------------------------------------------------------------------
	
	/** GridBagLayout * */
    private GridBagLayout mainPanelGridBagLayout;
    
    /** GridBagConstraints * */
    private GridBagConstraints mainPanelConstraints;
    
    /** Panels * */
    private JPanel mainPanel, imagePanel, OKCancelPanel;
    
    /** Labels **/
    private JLabel inputFileLabel, anatLabel, tagListSampleLabel, 
    				sourceNameLabel, sourceOrgLabel, sourceProjLabel,
    				testingLabel, detailLabel, tagListLabel, submitFileLabel;
    
    /** Buttons **/
    private JButton inputFileBrowseButton, submitFileBrowseButton, tagEditorBrowseButton;
    
    private JButton removeFileButton, removeAllFileButton;
    
    /**All files to generate image information for*/
    private JList inputFileList;

	/** Textfields **/
    private JTextField anatField, sourceNameField, sourceOrgField,
    					sourceProjField, tagListTextField, submitField; 
   	
	/** File chooser object */
	private JFileChooser fileChooser;
	
	/** Files selected by the user */
	private File[] selectedFiles = new File[0];
	
	/** List of current files to work with (adding a folder through GUI will add all of the folder's files */
	private Vector<String> fileList;
	
	/**Submission location*/
	private String submitLocation;
	
	/** Algorithm instance */
    private PlugInAlgorithmImageSubmit algoImageSubmit;

    /**Areas for adding additional file information*/
	private JTextArea testingArea, detailArea;

	/**File (directory) that will contain anonymized image copies*/
	private File submitFile;

	/** Array of tags from FileDicom*/
	private String[] tagArray;
	
	/**InfoGathering threads to find Dicom data*/
	private ArrayList<InformationUpdate> infoGather;
	
	private TagEditorDialog currentTagEditor;
	
	//	~ Constructors --------------------------------------------------------------------------

    /**
     * Empty constructor needed for dynamic instantiation (used during scripting).
     */
    public PlugInDialogImageSubmit() { }
    
    /**
     * Sets up variables but does not show dialog.
     *
     * @param  theParentFrame  Parent frame.
     * @param  im              Source image.
     */
    public PlugInDialogImageSubmit(boolean modal) {
        super(modal); 
        fileList = new Vector<String>();
        infoGather = new ArrayList<InformationUpdate>();
        
    	init();
    }
    
    // ~ Methods ----------------------------------------------------------------------------------
    
    public void init() {
    	setForeground(Color.black);
        setTitle("Image Submission Tool");
        
        mainPanelGridBagLayout = new GridBagLayout();
        mainPanelConstraints = new GridBagConstraints();
        mainPanelConstraints.anchor = GridBagConstraints.NORTH;

        mainPanel = new JPanel(mainPanelGridBagLayout);
        
        // Image Panel
        imagePanel = buildImagePanel();
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanelConstraints.gridwidth = 3;
        mainPanel.add(imagePanel, mainPanelConstraints);
        
        //Provenance panel
        JPanel subProv = buildProvPanel();        
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 1;
        mainPanelConstraints.gridwidth = 3;
        mainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(subProv, mainPanelConstraints);
        
        //Testing panel
        JPanel subPanelDetail = buildTestPanel();
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 2;
        mainPanelConstraints.gridwidth = 3;
        mainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(subPanelDetail, mainPanelConstraints);
        
        // OK,Cancel 
        OKCancelPanel = new JPanel();
        buildOKButton();
        OKButton.setActionCommand("ok");
        OKCancelPanel.add(OKButton, BorderLayout.WEST);
        buildCancelButton();
        cancelButton.setActionCommand("cancel");
        OKCancelPanel.add(cancelButton, BorderLayout.EAST);

        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(OKCancelPanel, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setVisible(true);
        requestFocus();  
    }
    
    private JPanel buildImagePanel() {
    	
    	//image browser
    	JPanel imagePanel = new JPanel(new GridBagLayout());
        imagePanel.setBorder(MipavUtil.buildTitledBorder("Image information"));
        GridBagConstraints imageConstraints = new GridBagConstraints();
        imageConstraints.gridx = 0;
        imageConstraints.gridy = 0;
        imageConstraints.gridheight = 3;
        imageConstraints.anchor = GridBagConstraints.WEST;
        imageConstraints.insets = new Insets(15, 5, 15, 0);
        inputFileLabel = new JLabel(" Input files : ");
        imagePanel.add(inputFileLabel, imageConstraints);
        imageConstraints.gridx = 1;
        imageConstraints.gridy = 0;
        imageConstraints.gridheight = 3;
        imageConstraints.insets = new Insets(15, 70, 15, 0);
        imageConstraints.fill = GridBagConstraints.BOTH;
        
        inputFileList = new JList(fileList);
        inputFileList.setVisibleRowCount(4);
        inputFileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        inputFileList.setMinimumSize(new Dimension(300, 94));
        inputFileList.setMaximumSize(new Dimension(300, 500));
        JScrollPane scrollPaneFile = new JScrollPane(inputFileList);
        scrollPaneFile.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPaneFile.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);    
        scrollPaneFile.setMinimumSize(new Dimension(300, 94));
        scrollPaneFile.setPreferredSize(new Dimension(300, 94));
        imagePanel.add(scrollPaneFile, imageConstraints);

        imageConstraints.gridx = 2;
        imageConstraints.gridy = 0;
        imageConstraints.gridheight = 1;
        imageConstraints.insets = new Insets(15, 5, 5, 5);
        imageConstraints.fill = GridBagConstraints.HORIZONTAL;
        inputFileBrowseButton = new JButton("Browse");
        inputFileBrowseButton.addActionListener(this);
        inputFileBrowseButton.setActionCommand(BROWSE_FILE_INPUT);
        imagePanel.add(inputFileBrowseButton, imageConstraints);
        
        imageConstraints.gridx = 2;
        imageConstraints.gridy = 1;
        imageConstraints.fill = GridBagConstraints.HORIZONTAL;
        imageConstraints.insets = new Insets(5, 5, 5, 5);
        removeFileButton = new JButton("Remove");
        removeFileButton.addActionListener(this);
        removeFileButton.setActionCommand(REMOVE);
        imagePanel.add(removeFileButton, imageConstraints);
        
        imageConstraints.gridx = 2;
        imageConstraints.gridy = 2;
        imageConstraints.fill = GridBagConstraints.NONE;
        imageConstraints.insets = new Insets(5, 5, 15, 5);
        removeAllFileButton = new JButton("Remove All");
        removeAllFileButton.addActionListener(this);
        removeAllFileButton.setActionCommand(REMOVE_ALL);
        imagePanel.add(removeAllFileButton, imageConstraints);
        
        //location copy selector
        imageConstraints.gridx = 0;
        imageConstraints.gridy = 3;
        imageConstraints.anchor = GridBagConstraints.CENTER;
        imageConstraints.insets = new Insets(15, 5, 15, 0);
        submitFileLabel = new JLabel(" Submission location : ");
        imagePanel.add(submitFileLabel, imageConstraints);

        imageConstraints.gridx = 1;
        imageConstraints.gridy = 3;
        
        imageConstraints.insets = new Insets(15, 70, 15, 5);
        imageConstraints.fill = GridBagConstraints.BOTH;
        submitField = new JTextField(45);
        imagePanel.add(submitField, imageConstraints);
        imageConstraints.gridx = 2;
        imageConstraints.gridy = 3;
        imageConstraints.insets = new Insets(5, 5, 5, 5);
        imageConstraints.fill = GridBagConstraints.HORIZONTAL;
        submitFileBrowseButton = new JButton("Browse");
        submitFileBrowseButton.addActionListener(this);
        submitFileBrowseButton.setActionCommand(BROWSE_FILE_SUBMIT);
        imagePanel.add(submitFileBrowseButton, imageConstraints);
        
        // Anatomical area
        imageConstraints.gridx = 0;
        imageConstraints.gridy = 4;
        imageConstraints.insets = new Insets(15, 5, 15, 5);
        anatLabel = new JLabel(" Anatomical Area : ");
        imagePanel.add(anatLabel, imageConstraints);
        
        imageConstraints.gridx = 1;
        imageConstraints.gridy = 4;
        imageConstraints.anchor = GridBagConstraints.WEST;
        imageConstraints.insets = new Insets(15, 70, 5, 5);
        anatField = new JTextField(45);
        imagePanel.add(anatField, imageConstraints);
        
        // Tag list
        imageConstraints.gridx = 0;
        imageConstraints.gridy = 5;
        imageConstraints.fill = GridBagConstraints.NONE;
        imageConstraints.insets = new Insets(15, 5, 15, 5);
        tagListLabel = new JLabel(" Anonymize tags : ");
        imagePanel.add(tagListLabel, imageConstraints);
        
        imageConstraints.gridx = 1;
        imageConstraints.gridy = 5;
        imageConstraints.anchor = GridBagConstraints.WEST;
        imageConstraints.insets = new Insets(15, 70, 5, 5);
        tagListTextField = new JTextField(45);
        imagePanel.add(tagListTextField, imageConstraints);
        
        imageConstraints.gridx = 2;
        imageConstraints.gridy = 5;
        imageConstraints.fill = GridBagConstraints.HORIZONTAL;
        imageConstraints.anchor = GridBagConstraints.CENTER;
        imageConstraints.insets = new Insets(15, 5, 5, 5);
        tagEditorBrowseButton = new JButton("Browse");
        tagEditorBrowseButton.setEnabled(false);
        tagEditorBrowseButton.addActionListener(this);
        tagEditorBrowseButton.setActionCommand(BROWSE_AVAILABLE_TAGS);
        imagePanel.add(tagEditorBrowseButton, imageConstraints);

        imageConstraints.gridx = 1;
        imageConstraints.gridy = 6;
        imageConstraints.anchor = GridBagConstraints.WEST;
        imageConstraints.insets = new Insets(2, 70, 5, 5);
        tagListSampleLabel = new JLabel(" Additional tags format: group,element;group,element e.g. 0002,0000;0002,0001  ");
        imagePanel.add(tagListSampleLabel, imageConstraints);
        
        return imagePanel;
    }
    
    private JPanel buildProvPanel() {
    	//source name
        JPanel subProv = new JPanel(new GridBagLayout());
        subProv.setBorder(MipavUtil.buildTitledBorder("Data provenance"));
        GridBagConstraints provConstraints = new GridBagConstraints();
        provConstraints.gridx = 0;
        provConstraints.gridy = 0;
        provConstraints.weightx = 0;
        provConstraints.anchor = GridBagConstraints.WEST;
        provConstraints.insets = new Insets(15, 5, 15, 5);
        sourceNameLabel = new JLabel(" Source name:  ");
        subProv.add(sourceNameLabel, provConstraints);
        
        provConstraints.gridx = 1;
        provConstraints.gridy = 0;
        provConstraints.weightx = 1;
        provConstraints.insets = new Insets(10, 60, 5, 5);
        sourceNameField = new JTextField(45);
        subProv.add(sourceNameField, provConstraints);
        
        //source organization
        provConstraints.gridx = 0;
        provConstraints.gridy = 1;
        provConstraints.weightx = 0;
        provConstraints.insets = new Insets(15, 5, 15, 5);
        sourceOrgLabel = new JLabel(" Source organization:  ");
        subProv.add(sourceOrgLabel, provConstraints);
        
        provConstraints.gridx = 1;
        provConstraints.gridy = 1;
        provConstraints.weightx = 1;
        provConstraints.insets = new Insets(10, 60, 5, 5);
        sourceOrgField = new JTextField(45);
        subProv.add(sourceOrgField, provConstraints);
        
        //source project
        provConstraints.gridx = 0;
        provConstraints.gridy = 2;
        provConstraints.weightx = 0;
        provConstraints.insets = new Insets(15, 5, 15, 5);
        sourceProjLabel = new JLabel(" Source project:  ");
        subProv.add(sourceProjLabel, provConstraints);
        
        provConstraints.gridx = 1;
        provConstraints.gridy = 2;
        provConstraints.weightx = 1;
        provConstraints.insets = new Insets(10, 60, 5, 5);
        sourceProjField = new JTextField(45);
        subProv.add(sourceProjField, provConstraints);

        return subProv;
    }
    
    private JPanel buildTestPanel() {
    	//Testing information
        JPanel subPanelDetail = new JPanel(new GridBagLayout());
        subPanelDetail.setBorder(MipavUtil.buildTitledBorder("Testing and details"));
        GridBagConstraints subBagConstraints = new GridBagConstraints();
        subBagConstraints.anchor = GridBagConstraints.NORTH;
        subBagConstraints.gridx = 0;
        subBagConstraints.gridy = 0;
        subBagConstraints.weightx = 0;
        subBagConstraints.anchor = GridBagConstraints.WEST;
        subBagConstraints.insets = new Insets(15, 5, 15, 5);
        testingLabel = new JLabel(" Testing information:  ");
        subPanelDetail.add(testingLabel, subBagConstraints);
        
        subBagConstraints.gridx = 1;
        subBagConstraints.gridy = 0;
        subBagConstraints.weightx = 1;
        subBagConstraints.insets = new Insets(10, 60, 5, 5);
        testingArea = new JTextArea();
        testingArea.setMinimumSize(new Dimension(500, 94));
        testingArea.setMaximumSize(new Dimension(500, 500));
        JScrollPane scrollPaneTesting = new JScrollPane(testingArea);
        scrollPaneTesting.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPaneTesting.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);    
        scrollPaneTesting.setMinimumSize(new Dimension(500, 94));
        scrollPaneTesting.setPreferredSize(new Dimension(500, 94));
        subPanelDetail.add(scrollPaneTesting, subBagConstraints);
        
        //Details
        subBagConstraints.gridx = 0;
        subBagConstraints.gridy = 2;
        subBagConstraints.weightx = 0;
        subBagConstraints.insets = new Insets(15, 5, 15, 5);
        detailLabel = new JLabel(" Details:  ");
        subPanelDetail.add(detailLabel, subBagConstraints);
        
        subBagConstraints.gridx = 1;
        subBagConstraints.gridy = 2;
        subBagConstraints.weightx = 1;
        subBagConstraints.insets = new Insets(10, 60, 5, 5);
        detailArea = new JTextArea();
        detailArea.setMinimumSize(new Dimension(500, 94));
        detailArea.setMaximumSize(new Dimension(500, 500));
        JScrollPane scrollPaneDetail = new JScrollPane(detailArea);
        scrollPaneDetail.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPaneDetail.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);    
        scrollPaneDetail.setMinimumSize(new Dimension(500, 94));
        scrollPaneDetail.setPreferredSize(new Dimension(500, 94));
        subPanelDetail.add(scrollPaneDetail, subBagConstraints);
        
        return subPanelDetail;
    }
    
    private boolean createTagArray () {
    	String tagList;
    	try {
    		tagList = tagListTextField.getText();
    	} catch (NullPointerException npe) {
    		tagArray = null;
    		return true;
    	}
    	
    	tagArray = tagList.toUpperCase().split(";");
    	return true;
       	
    }
    
    /**
     * Tests whether f already has been added to the GUI
     * @param f
     * @return
     */
    private boolean fileAlreadyAdded(File f) {
    	boolean fileExists = false;
    	for(int i=0; i<fileList.size(); i++) {
    		if(f.getAbsolutePath().equals(fileList.get(i))) {
    			fileExists = true;
    			break;
    		}
    	}
    	
    	return fileExists;
    }
    
    /**
     * Once all the necessary variables are set, call the image submission algorithm.
     */
    
    protected void callAlgorithm() {
    
    	try{
    		System.gc();
    		selectedFiles = new File[fileList.size()];
    		for(int i=0; i<fileList.size(); i++) {
    			System.out.println("Working with file "+fileList.get(i));
    			selectedFiles[i] = new File(fileList.get(i));
    		}
    		
    		//map keys exact xml tag to particular text
    		HashMap<String, String> map = new HashMap();
    		map.put("anatomical-area", anatField.getText());
    		map.put("source-name", sourceNameField.getText());
    		map.put("source-org", sourceOrgField.getText());
    		map.put("source-project", sourceProjField.getText());
    		map.put("testing-information", testingArea.getText());
    		map.put("details", detailArea.getText());
    		
    		submitLocation = submitField.getText();
    		
    		//Make algorithm.
    		algoImageSubmit = new PlugInAlgorithmImageSubmit(selectedFiles, map, submitLocation, tagArray, true);
    		
    		// This is very important. Adding this object as a listener allows the algorithm to
            // notify this object when it has completed of failed. See algorithm performed event.
            // This is made possible by implementing AlgorithmedPerformed interface
            algoImageSubmit.addListener(this);
            
            createProgressBar("Image Submission Tool", algoImageSubmit);
            
            setVisible(false);
            
            if (isRunInSeparateThread()) {

                // Start the thread as a low priority because we wish to still have user interface work fast.
                if (algoImageSubmit.startMethod(Thread.MIN_PRIORITY) == false) {
                    MipavUtil.displayError("A thread is already running on this object");
                }
            } else {
                algoImageSubmit.run();
            }
    		
    	} catch (OutOfMemoryError x) {
            System.gc();
            MipavUtil.displayError("Dialog Plugin Anonymize DICOM: unable to allocate enough memory");

            return;
    	}
    
    }
    
    /**
     * Closes dialog box when the OK button is pressed and calls the algorithm.
     *
     * @param  event  Event that triggers function.
     */
    public void actionPerformed(ActionEvent event) {
    	
    	String command = event.getActionCommand();
    	if (command.equals(BROWSE_FILE_INPUT)) {

    		fileChooser = new JFileChooser(Preferences.getImageDirectory());
        	fileChooser.setFont(MipavUtil.defaultMenuFont);
        	fileChooser.setMultiSelectionEnabled(true);
        	fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        	
        	Dimension d = new Dimension(700, 400);
            fileChooser.setMinimumSize(d);
            fileChooser.setPreferredSize(d);
            
            int returnVal = fileChooser.showOpenDialog(null);
                        
            if (returnVal == JFileChooser.APPROVE_OPTION) {
            	selectedFiles = fileChooser.getSelectedFiles();
            	Preferences.setImageDirectory(fileChooser.getCurrentDirectory());
            	for(int i=0; i<selectedFiles.length; i++) {
            		boolean fileExists = false;
            		for(int j=0; j<fileList.size(); j++) {
            			if(selectedFiles[i].getAbsolutePath().equals(fileList.get(j))) {
            				fileExists = true;
            			}
            		}
            		if(!fileExists) {
            			if(selectedFiles[i].isDirectory()) {
            				for(File f : selectedFiles[i].listFiles()) {
            					if(!f.isDirectory() && !fileAlreadyAdded(f)) {
            						fileList.add(f.getAbsolutePath());
            					}
            				}
            			} else {
            				fileList.add(selectedFiles[i].getAbsolutePath());
            			}
            		}
            	}
            	inputFileList.updateUI();         	
            	InformationUpdate update = new InformationUpdate(fileList, this);
            	if(update.isActionable()) {
            		infoGather.add(update);
            		Thread t = new Thread(update);
            		t.start();
            	}
            	if(selectedFiles[0].isDirectory()) {
            		submitField.setText(selectedFiles[0] + File.separator + "submit");
            	} else {
            		submitField.setText(selectedFiles[0].getParent() + File.separator + "submit");
            	}
            	tagEditorBrowseButton.setEnabled(true);
            }
            
            
        } else if(command.equals(BROWSE_FILE_SUBMIT)) { 
        	String startingFile;
        	if(submitField.getText() != null && submitField.getText().length() > 0) {
        		startingFile = submitField.getText();
        	} else {
        		startingFile = Preferences.getImageDirectory();
        	}
        	
        	fileChooser = new JFileChooser(startingFile);
        	fileChooser.setFont(MipavUtil.defaultMenuFont);
        	fileChooser.setMultiSelectionEnabled(false);
        	fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        	
        	Dimension d = new Dimension(700, 400);
        	fileChooser.setMinimumSize(d);
        	fileChooser.setPreferredSize(d);
        	
        	int returnVal = fileChooser.showOpenDialog(null);
        	
        	if(returnVal == JFileChooser.APPROVE_OPTION) {
        		submitFile = fileChooser.getSelectedFile();
        		submitField.setText(submitFile.getAbsolutePath());
        	}
        } else if (command.equalsIgnoreCase("Cancel")) {
        	dispose();
        } else if (command.equalsIgnoreCase("OK")) {
        	createTagArray();
        	if(!(selectedFiles.length > 0)) {
        		MipavUtil.displayError("No files were selected for submission");
        	} else if(!(submitField.getText().length() > 0)) {
        		MipavUtil.displayError("A submission location is required.");
        	} else {
        		callAlgorithm();
        	}
    	
        } else if(command.equals(REMOVE_ALL)) {
        	fileList.removeAllElements();
        	clearEntries();
        	imagePanel.updateUI();
        } else if(command.equals(REMOVE)) {
        	Object[] objAr = inputFileList.getSelectedValues();
        	for(Object obj : objAr) {
        		fileList.remove(obj);
        	}
        	if(fileList.size() == 0) {
        		clearEntries();
        	}
        	inputFileList.updateUI();
        } else if(command.equals(BROWSE_AVAILABLE_TAGS)) {
        	currentTagEditor.setVisible();
        	System.out.println("DICOM tag browsing");
        	
        }
    }
    
    private void clearEntries() {
    	anatField.setText("");
    	submitField.setText("");
    	tagEditorBrowseButton.setEnabled(false);
    }

	/**
     * This method is required if the AlgorithmPerformed interface is implemented. It is called by the algorithms when
     * it has completed or failed to complete, so that the dialog can display the result image and/or clean up.
     *
     * @param  algorithm  Algorithm that caused the event.
     */
    public void algorithmPerformed(AlgorithmBase algorithm) {
    	
    	progressBar.dispose();
    	algoImageSubmit.finalize();
    	algoImageSubmit = null;
    	
    }
    
    protected void setGUIFromParams() {
    	
    }
    protected void storeParamsFromGUI() {
    	
    }
    
    private class InformationUpdate implements Runnable {

    	private Vector<File> fileListUpdate;
    	private boolean likelyActionable;
    	private FileDicom imageFile;
    	private PlugInDialogImageSubmit parent;
    	
		public InformationUpdate(Vector<String> fileListString, PlugInDialogImageSubmit parent) {
			this.parent = parent;
			this.fileListUpdate = new Vector<File>();
			for(int i=0; i<fileListString.size(); i++) {
				fileListUpdate.add(new File(fileListString.get(i)));
			}
			this.likelyActionable = examineFileList(fileListUpdate);
		}
    	
		public void run() {
			if(!likelyActionable) {
				return;
			}
			FileInfoBase info = null;
			try {
	    		imageFile = new FileDicom(fileListUpdate.get(0).getName(), fileListUpdate.get(0).getParent()+File.separator);
	            imageFile.setQuiet(true); // if we want quiet, we tell the reader, too.
	            imageFile.readHeader(true); // can we read the header?	
	            info = imageFile.getFileInfo();
			} catch(Exception e) {
				e.printStackTrace();
			}
            if(info instanceof FileInfoDicom) {
            	FileDicomTagTable tagTable = ((FileInfoDicom) info).getTagTable();
            	Object obj = tagTable.get(new FileDicomKey("0018,0015"));
            	if(obj instanceof FileDicomTag) {
            		if(((FileDicomTag) obj).getValue(true).toString().length() > 0) {
            			anatField.setText(((FileDicomTag) obj).getValue(true).toString());
            		}
            	}
            	
            	currentTagEditor = new TagEditorDialog(tagTable, parent);
            }
		}
    	
    	private boolean examineFileList(Vector<File> f) {
    		if(f.size() == 0) {
    			return false;
    		} else if(f.size() == 1) {
    			return true;
    		} else {
    			return true;
    		}	
    	}	
    	
    	public FileDicom getImageFile() {
    		return imageFile;
    	}
    	
    	public boolean isActionable() {
    		return likelyActionable;
    	}
    }
    
    private class TagEditorDialog extends JDialogBase implements ListSelectionListener {

    	private static final String ADD_TAG = "Add";

		private static final String CLOSE = "Close";

		private static final String CLEAR_TAGS = "Clear tags";

		/**List of all file's elements for each group of a FileDicomKey set*/
		private TreeMap<String, ArrayList<String>> groupToElement;
    	
		/**FileDicomTag to its name in the Dicom dictionary*/
    	private TreeMap<String, String> keyToName;

    	/**FileDicomTag to its value in the file*/
    	private TreeMap<String, String> keyToValue;
    	
		private GridBagLayout subPanelGridBagLayout;

		private GridBagConstraints subPanelConstraints;

		/**Panels used by this dialog box*/
		private JPanel tagSelectorPanel, tagInformationPanel;

		/**Lists used to display available DICOM tags*/
		private JList groupList, elementList;

		/**Buttons used by this dialog*/
		private JButton addButton, clearButton, closeButton;

		private JLabel nameValue;

		private JLabel propertyValue;
    	
		public TagEditorDialog(FileDicomTagTable tagTable, PlugInDialogImageSubmit parent) {
			super(parent, false);
			this.groupToElement = new TreeMap<String, ArrayList<String>>();
			this.keyToName = new TreeMap<String, String>();
			this.keyToValue = new TreeMap<String, String>();
			buildGroupElementMap(tagTable);
			
			init();
		}
		
		public void init() {
			setForeground(Color.black);
	        setTitle("Tag Selector Tool");
	        
	        subPanelGridBagLayout = new GridBagLayout();
	        subPanelConstraints = new GridBagConstraints();
	        subPanelConstraints.anchor = GridBagConstraints.NORTH;

	        tagSelectorPanel = new JPanel(subPanelGridBagLayout);
	        tagSelectorPanel.setBorder(MipavUtil.buildTitledBorder("Select DICOM tags"));
	        
	        // Dialog column
	        subPanelConstraints.gridx = 0;
	        subPanelConstraints.gridy = 0;
	        subPanelConstraints.gridheight = 4;
	        subPanelConstraints.insets = new Insets(5, 5, 5, 5);
	        subPanelConstraints.anchor = GridBagConstraints.CENTER;
	        JLabel firstLabel = new JLabel("<html>Select<br>tags:</html>");
	        tagSelectorPanel.add(firstLabel, subPanelConstraints);
	        
	        // Group Column
	        subPanelConstraints.gridx = 1;
	        subPanelConstraints.gridy = 0;
	        subPanelConstraints.gridheight = 1;
	        subPanelConstraints.anchor = GridBagConstraints.WEST;
	        subPanelConstraints.insets = new Insets(5, 15, 5, 15);
	        JLabel groupLabel = new JLabel("Group:");
	        tagSelectorPanel.add(groupLabel, subPanelConstraints);
	        
	        subPanelConstraints.gridx = 1;
	        subPanelConstraints.gridy = 1;
	        subPanelConstraints.gridheight = 3;
	        
	        subPanelConstraints.anchor = GridBagConstraints.CENTER;
	        Vector<String> vGroup;
	        Collections.sort(vGroup = new Vector<String>(groupToElement.keySet()), new NumberComparator());
	        groupList = new JList(vGroup);
	        groupList.setSelectedIndex(0);
	        groupList.setVisibleRowCount(4);
	        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	        groupList.setMinimumSize(new Dimension(150, 94));
	        groupList.setMaximumSize(new Dimension(150, 500));
	        groupList.getSelectionModel().addListSelectionListener(this);
	        JScrollPane scrollPaneGroup = new JScrollPane(groupList);
	        scrollPaneGroup.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	        scrollPaneGroup.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);    
	        scrollPaneGroup.setMinimumSize(new Dimension(150, 94));
	        scrollPaneGroup.setPreferredSize(new Dimension(150, 94));
	        tagSelectorPanel.add(scrollPaneGroup, subPanelConstraints);
	        
	        // Element Column
	        subPanelConstraints.gridx = 2;
	        subPanelConstraints.gridy = 0;
	        subPanelConstraints.gridheight = 1;
	        subPanelConstraints.anchor = GridBagConstraints.WEST;
	        JLabel elementLabel = new JLabel("Element:");
	        tagSelectorPanel.add(elementLabel, subPanelConstraints);
	        
	        subPanelConstraints.gridx = 2;
	        subPanelConstraints.gridy = 1;
	        subPanelConstraints.gridheight = 3;
	        subPanelConstraints.anchor = GridBagConstraints.CENTER;
	        Vector<String> vElement;
	        Collections.sort(vElement = new Vector<String>(groupToElement.get(vGroup.get(0))), new NumberComparator());
	        elementList = new JList(vElement);
	        elementList.setSelectedIndex(0);
	        elementList.setVisibleRowCount(4);
	        elementList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	        elementList.setMinimumSize(new Dimension(150, 94));
	        elementList.setMaximumSize(new Dimension(150, 500));
	        elementList.getSelectionModel().addListSelectionListener(this);
	        JScrollPane scrollPaneElement = new JScrollPane(elementList);
	        scrollPaneElement.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	        scrollPaneElement.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);    
	        scrollPaneElement.setMinimumSize(new Dimension(150, 94));
	        scrollPaneElement.setPreferredSize(new Dimension(150, 94));
	        tagSelectorPanel.add(scrollPaneElement, subPanelConstraints);
	        
	        //Buttons Column
	        subPanelConstraints.gridx = 3;
	        subPanelConstraints.gridy = 1;
	        subPanelConstraints.gridheight = 1;
	        subPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
	        subPanelConstraints.insets = new Insets(5, 5, 5, 5);
	        addButton = new JButton("Add");
	        addButton.setActionCommand(ADD_TAG);
	        addButton.addActionListener(this);
	        tagSelectorPanel.add(addButton, subPanelConstraints);
	        
	        subPanelConstraints.gridx = 3;
	        subPanelConstraints.gridy = 2;
	        clearButton = new JButton("Clear");
	        clearButton.setActionCommand(CLEAR_TAGS);
	        clearButton.addActionListener(this);
	        tagSelectorPanel.add(clearButton, subPanelConstraints);
	        
	        subPanelConstraints.gridx = 3;
	        subPanelConstraints.gridy = 3;
	        closeButton = new JButton("Close");
	        closeButton.setActionCommand(CLOSE);
	        closeButton.addActionListener(this);
	        tagSelectorPanel.add(closeButton, subPanelConstraints);
	        
	        subPanelGridBagLayout = new GridBagLayout();
	        subPanelConstraints = new GridBagConstraints();
	        subPanelConstraints.anchor = GridBagConstraints.NORTH;

	        tagInformationPanel = new JPanel(subPanelGridBagLayout);
	        tagInformationPanel.setBorder(MipavUtil.buildTitledBorder("DICOM tag information"));
	        
	        //tag name row
	        subPanelConstraints.gridx = 0;
	        subPanelConstraints.gridy = 0;
	        subPanelConstraints.insets = new Insets(5, 10, 5, 10);
	        subPanelConstraints.anchor = GridBagConstraints.WEST;
	        subPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
	        subPanelConstraints.weightx = 0;
	        JLabel nameLabel = new JLabel("Tag name:");
	        tagInformationPanel.add(nameLabel, subPanelConstraints);
	        
	        subPanelConstraints.gridx = 1;
	        subPanelConstraints.gridy = 0;
	        subPanelConstraints.weightx = 1;
	        String tagName = groupList.getSelectedValue().toString()+","+elementList.getSelectedValue().toString();
	        nameValue = new JLabel(keyToName.get(tagName));
	        tagInformationPanel.add(nameValue, subPanelConstraints);
	        
	        //tag value row
	        subPanelConstraints.gridx = 0;
	        subPanelConstraints.gridy = 1;
	        subPanelConstraints.weightx = 0;
	        JLabel propertyLabel = new JLabel("Tag value:");
	        tagInformationPanel.add(propertyLabel, subPanelConstraints);
	        
	        subPanelConstraints.gridx = 1;
	        subPanelConstraints.gridy = 1;
	        subPanelConstraints.weightx = 1;
	        propertyValue = new JLabel(keyToValue.get(tagName));
	        tagInformationPanel.add(propertyValue, subPanelConstraints);

	        //tag sequence row
	        
	        add(tagSelectorPanel, BorderLayout.NORTH);
	        add(tagInformationPanel, BorderLayout.SOUTH);
	        
	        pack();
		}
		
		private void buildGroupElementMap(FileDicomTagTable tagTable) {
			Hashtable<FileDicomKey, FileDicomTag> tagHash = tagTable.getTagList();
			Enumeration<FileDicomKey> e = tagHash.keys();
			while(e.hasMoreElements()) {
				FileDicomKey key = e.nextElement();
				ArrayList<String> allElements = groupToElement.get(key.getGroup());
				if(allElements == null) {
					allElements = new ArrayList<String>(); 
					groupToElement.put(key.getGroup(), allElements);
				}
				if(Integer.valueOf(key.getElement(), 16) != 0) {
					allElements.add(key.getElement());
				}
				String tagName = DicomDictionary.getName(key);
				if(tagName == null || tagName.length() == 0) {
					keyToName.put(key.toString(), "Private Tag");
				} else {
					keyToName.put(key.toString(), tagName);
				}
				Object obj = tagTable.getValue(key);
				if(obj instanceof FileDicomSQ) {
					keyToValue.put(key.toString(), "Sequence");
				} else if(obj == null) {
					keyToValue.put(key.toString(), "Unknown");
				} else {
					keyToValue.put(key.toString(), obj.toString());
				}
			}
		}
		
		private String printObjects(ArrayList<String> ar) {
			String arStr = "";
			for(String arItem : ar) {
				arStr += arItem+" ";
			}
			return arStr;
		}
    	
		public void actionPerformed(ActionEvent e) {
			if(e.getActionCommand().equals(CLEAR_TAGS)) {
				tagListTextField.setText("");
			} else if(e.getActionCommand().equals(ADD_TAG)) {
				if(!tagExistsInField(groupList.getSelectedValue()+","+elementList.getSelectedValue())) {
					String existingText = tagListTextField.getText();
					String prefix = existingText.length() == 0 || existingText.charAt(existingText.length()-1) == ';' ? "" : ";";
					tagListTextField.setText(existingText+prefix+groupList.getSelectedValue()+","+elementList.getSelectedValue()+";");
				}
			} else if(e.getActionCommand().equals(CLOSE)) {
				this.dispose();
			}
		}
    	
    	private boolean tagExistsInField(String text) {
    		String[] tagList = tagListTextField.getText().split(";");
    		for(int i=0; i<tagList.length; i++) {
    			if(tagList[i].equals(text)) {
    				return true;
    			}
    		}
    		return false;
    	}
    	
		public void valueChanged(ListSelectionEvent e) {
			if(e.getSource().equals(groupList.getSelectionModel())) {
				Vector<String> vElement;
		        Collections.sort(vElement = new Vector<String>(groupToElement.get(groupList.getSelectedValue())), new NumberComparator());
		        elementList.setListData(vElement);
		        elementList.setSelectedIndex(0);
		        elementList.updateUI();
			}
			if(groupList.getSelectedIndex() != -1 && elementList.getSelectedIndex() != -1) {
				String tagName = groupList.getSelectedValue().toString()+","+elementList.getSelectedValue().toString();
				nameValue.setText(keyToName.get(tagName));
				propertyValue.setText(keyToValue.get(tagName));
				tagInformationPanel.updateUI();
			}
		}
    	
    	private class NumberComparator implements Comparator<String>, Serializable {

			public int compare(String o1, String o2) {
				return Integer.valueOf(o1, 16) - Integer.valueOf(o2, 16);
			}
    		
    	}
    }
    
    public static void main(String[] args) {
    	new PlugInDialogImageSubmit(true);
    }
    
}
