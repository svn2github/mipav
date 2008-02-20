package gov.nih.mipav.model.algorithms.itk.autoItk;

import java.util.List;
import java.util.Iterator;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;


public class ItkFilterSelectorDialog extends JDialog implements ActionListener {
    private static ItkFilterSelectorDialog dialog;
    private static boolean value = false;
    private JPanel m_CheckBoxPanel = null;
    private List<FilterRecord> m_FilterRecord = null;
    

    /**
     * Set up and show the dialog.  The first Component argument
     * determines which frame the dialog depends on; it should be
     * a component in the dialog's controlling frame. The second
     * Component argument should be null if you want the dialog
     * to come up with its left corner in the center of the screen;
     * otherwise, it should be the component on top of which the
     * dialog should appear.
     */
    public static boolean showDialog(Component frameComp,
                                    Component locationComp,
                                    String labelText,
                                    String title,
                                    List<FilterRecord> possibleValues) {
        Frame frame = JOptionPane.getFrameForComponent(frameComp);
        dialog = new ItkFilterSelectorDialog(frame,
                                locationComp,
                                labelText,
                                title,
                                possibleValues);
        dialog.setVisible(true);
        return value;
    }

    private void setValue(boolean newValue) {
        value = newValue;
        //list.setSelectedValue(value, true);
    }

    ItkFilterSelectorDialog(Frame frame,
                            Component locationComp,
                            String labelText,
                            String title,
                            List<FilterRecord> data) {
        super(frame, title, true);
        m_FilterRecord = data;

        //Create and initialize the buttons.
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        //
        final JButton setButton = new JButton("Set");
        setButton.setActionCommand("Set");
        setButton.addActionListener(this);
        getRootPane().setDefaultButton(setButton);

        // Main content - list of filters, turn on/off.
        m_CheckBoxPanel = new JPanel();
        m_CheckBoxPanel.setLayout(new BoxLayout(m_CheckBoxPanel, BoxLayout.PAGE_AXIS));
        // add checkboxes.
        for(Iterator<FilterRecord> it = data.iterator(); it.hasNext(); ) {
            FilterRecord filter_rec = it.next();
            String name = filter_rec.m_Name + "     " + filter_rec.m_State.getName();
            JCheckBox cb = new JCheckBox(name, filter_rec.m_Active);
            if (filter_rec.m_State == FilterRecord.FilterState.REMOVED) {
                cb.setEnabled(false);
            }
            m_CheckBoxPanel.add(cb);
        }

        JScrollPane listScroller = new JScrollPane(m_CheckBoxPanel);
        //listScroller.setPreferredSize(new Dimension(250, 80));
        listScroller.setAlignmentX(LEFT_ALIGNMENT);

        //Create a container so that we can add a title around
        //the scroll pane.  Can't add a title directly to the
        //scroll pane because its background would be white.
        //Lay out the label and scroll pane from top to bottom.
        JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        JLabel label = new JLabel(labelText);
        label.setLabelFor(m_CheckBoxPanel);
        listPane.add(label);
        listPane.add(Box.createRigidArea(new Dimension(0,5)));
        listPane.add(listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        //Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(setButton);

        //Put everything together, using the content pane's BorderLayout.
        Container contentPane = getContentPane();
        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);

        //Initialize values.
        setValue(false);
        pack();
        
    }

    public void actionPerformed(ActionEvent e) {
        if ("Set".equals(e.getActionCommand())) {
            System.out.println("Set: change filter file.");
            ItkFilterSelectorDialog.value = true;
            Iterator<FilterRecord> it = m_FilterRecord.iterator(); 
            for (Component cmp : m_CheckBoxPanel.getComponents() ) {
                JCheckBox cb = (JCheckBox)cmp;
                System.out.println(cb.getText() + " " + cb.isSelected());
                it.next().m_Active = cb.isSelected();
            }
        }
        ItkFilterSelectorDialog.dialog.setVisible(false);
    }

}
