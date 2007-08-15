package gov.nih.mipav.view.dialogs;


import gov.nih.mipav.model.provenance.*;
import gov.nih.mipav.view.*;
import gov.nih.mipav.view.components.WidgetFactory;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 * Simple dialog used to show the image or system data provenance (by passing in a provenanceholder
 * Displays data in table format, and the currently selected item will show up in the JTextArea (not editable, but selectable)
 *
 */
public class JDialogDataProvenance extends JDialogBase implements ProvenanceChangeListener {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = 3861014709972568409L;

    /** Column names for data provenance*/
    public static final String [] dpColumnNames = new String[] {"Time","Action","JVM","Mipav","User"};

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /**
     * Describes the initial size of the textual display area when the dialog is created. The value is given in pixel
     * size rather than the number of characters since the display area has no characters to display.
     */
    protected final Dimension DEFAULT_SIZE = new Dimension(650, 400);


    /** DOCUMENT ME! */
    private JPanel buttonPanel;

    /** DOCUMENT ME! */
    private JScrollPane scrollPane; // here so we can set scroll correct

    /** DOCUMENT ME! */
    private JTextArea textArea;

    private ProvenanceHolder pHolder;
    
    private ViewTableModel dpModel;
    
    private boolean isSystem;
    
    private String name;
    private String path;
    
    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Default constructor for displaying data provenance (image or system)
     */
    public JDialogDataProvenance(Frame parent, String name, String path, ProvenanceHolder ph, boolean is_system) {
        super(parent, false);
        this.pHolder = ph;
        pHolder.addProvenanceChangeListener(this);
        setResizable(true);
        
        this.name = name;
        this.path = path;
        
        this.isSystem = is_system;
        if (isSystem) {
        	name = "Mipav system";
        	
        	path = Preferences.getProperty(Preferences.PREF_DATA_PROVENANCE_FILENAME);

            if (path == null) {
            	path = System.getProperty("user.home") + File.separator + "mipav" + File.separator +
                                     "dataprovenance.xmp";
                Preferences.setProperty(Preferences.PREF_DATA_PROVENANCE_FILENAME, path);
            }
        }
        init(name);
        scrollPane.getVerticalScrollBar().addAdjustmentListener(new ScrollCorrector());
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Closes dialog box when the "Close" button is pressed.
     *
     * @param  event  Event that triggers this function.
     */
    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();

        if (source == cancelButton) {
            dispose();
        } else if (event.getActionCommand().equals("save")) {
        	save();
        } else if (event.getActionCommand().equals("open")) {
        	
        }
    }
    
    public void provenanceStateChanged(ProvenanceChangeEvent e) {
    	System.err.println("pstatechange");
    	addProvenanceData(e.getEntry());
    }
   
    /**
     * Initializes the dialog box to a certain size and adds the components.
     *
     * @param  title  Title of the dialog box.
     */
    private void init(String title) {
        JPanel scrollPanel;

        Box scrollingBox = new Box(BoxLayout.Y_AXIS);
        
        setTitle(title + " data provenance");

        dpModel = new ViewTableModel();
        JTable dpTable = new JTable(dpModel);

        for (int i = 0; i < dpColumnNames.length; i++) {
            dpModel.addColumn(dpColumnNames[i]);
        }
        dpTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        dpTable.getColumn("Time").setMinWidth(60);
        dpTable.getColumn("Time").setMaxWidth(200);
    //    dpTable.getColumn("Action").setMinWidth(100);
        dpTable.getColumn("JVM").setMinWidth(60);
        dpTable.getColumn("JVM").setMaxWidth(60);
        dpTable.getColumn("Mipav").setMinWidth(40);
        dpTable.getColumn("Mipav").setMaxWidth(40);
        dpTable.getColumn("User").setMinWidth(80);
        dpTable.getColumn("User").setMaxWidth(100);

        dpTable.getTableHeader().setReorderingAllowed(false);
        
               
        int size = pHolder.size();
        String rose [] = null;
        for (int i = 0; i < size; i++) {
        	addProvenanceData(pHolder.elementAt(i));
        }
        
        textArea = new JTextArea();
        textArea.setBackground(Color.white);
        textArea.setEditable(false);
        textArea.setBorder(this.buildTitledBorder("Currently selected value"));
        
        scrollingBox.add(dpTable.getTableHeader());
        scrollingBox.add(dpTable);
        scrollingBox.add(textArea);
        
        SelectionListener listener = new SelectionListener(dpTable, textArea);
        dpTable.getSelectionModel().addListSelectionListener(listener);
        dpTable.getColumnModel().getSelectionModel().addListSelectionListener(listener);
        
        scrollPane = new JScrollPane(scrollingBox, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPanel = new JPanel();
        scrollPanel.setLayout(new BorderLayout());
        scrollPanel.add(scrollPane);
        scrollPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JToolBar tBar = WidgetFactory.initToolbar();
        ViewToolBarBuilder toolbarBuilder = new ViewToolBarBuilder(this);
        tBar.add(toolbarBuilder.buildButton("Open", "Open mipav data-provenance file", "open"));
        tBar.add(toolbarBuilder.buildButton("Save", "Save mipav data-provenance file", "save"));
        
        
        buttonPanel = new JPanel();
        buildCancelButton();
        cancelButton.setText("Close");
        buttonPanel.add(cancelButton);
        
        JMenu fileMenu = new JMenu("File");

        fileMenu.setFont(MipavUtil.font12B);

        JMenuItem itemOpen = new JMenuItem("Open");
        itemOpen.addActionListener(this);
        itemOpen.setActionCommand("Open");
        itemOpen.setFont(MipavUtil.font12B);
        fileMenu.add(itemOpen);

        JMenuItem itemSave = new JMenuItem("Save");
        itemSave.addActionListener(this);
        itemSave.setActionCommand("Save");
        itemSave.setFont(MipavUtil.font12B);
        fileMenu.add(itemSave);

        fileMenu.addSeparator();

        JMenuItem itemExit = new JMenuItem("Exit");
        itemExit.addActionListener(this);
        itemExit.setActionCommand("Exit");
        itemExit.setFont(MipavUtil.font12B);
        fileMenu.add(itemExit);
        
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        
        getContentPane().add(tBar, BorderLayout.NORTH);
        getContentPane().add(scrollPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setSize(DEFAULT_SIZE);
        setVisible(true);
    }

    private void save() {
        String fileName;

        JFileChooser chooser = new JFileChooser();
        ViewImageFileFilter filter = new ViewImageFileFilter(ViewImageFileFilter.DATA_PROVENANCE);

        chooser.setFileFilter(filter);

        // if (userInterface.getDefaultDirectory()!=null)
        chooser.setCurrentDirectory(new File(Preferences.getScriptsDirectory()));

        // else
        // chooser.setCurrentDirectory(new File(System.getProperties().getProperty("user.dir")));
        int returnVal = chooser.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getName();

            if (fileName.lastIndexOf('.') == -1) {
                fileName = fileName + ".xmp";
            }

            
            
        } else {
            return;
        }
    }
    
    private void open() {

        JFileChooser chooser = new JFileChooser();

        // if (userInterface.getDefaultDirectory()!=null)
        chooser.setCurrentDirectory(new File(Preferences.getScriptsDirectory()));
        // else chooser.setCurrentDirectory(new File(System.getProperties().getProperty("user.dir")));

        chooser.addChoosableFileFilter(new ViewImageFileFilter(ViewImageFileFilter.DATA_PROVENANCE));

        int returnVal = chooser.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
           
        } else {
            return;
        }
    }
    
    private void addProvenanceData(ProvenanceEntry entry) {
    	
    	String[] rose = new String[dpColumnNames.length];

    	rose[0] = entry.getTimeStamp();
    	rose[1] = entry.getAction();
    	rose[2] = entry.getJavaVersion();
    	rose[3] = entry.getMipavVersion();
    	rose[4] = entry.getUser();        	
    	
    	dpModel.addRow(rose);
    }
    
    public static class SelectionListener implements ListSelectionListener {
        JTable table;
        JTextArea textArea;
        
        SelectionListener(JTable table, JTextArea textArea) {
            this.table = table;
            this.textArea = textArea;
        }
        public void valueChanged(ListSelectionEvent e) {	
        	 if (e.getValueIsAdjusting()) {
                 // The mouse button has not yet been released
        		 return;
             }
        	 textArea.setText((String)table.getModel().getValueAt(table.getSelectedRow(), table.getSelectedColumn()));
        }
    }
}
