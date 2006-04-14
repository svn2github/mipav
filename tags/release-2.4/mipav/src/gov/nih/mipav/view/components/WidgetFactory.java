package gov.nih.mipav.view.components;


import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.icons.PlaceHolder;
import java.awt.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.net.URL;
import javax.swing.*;
import javax.swing.border.*;


/**
 * This class is a collection of GUI widget generation methods. Widgets are created in a MIPAV-consistent manner and
 * style.
 * 
 * @todo menu and menu item generation is still not moved from ViewMenuBuilder to here due to quicklist and menu item
 *       vector considerations
 * 
 * @author mccreedy
 */
public class WidgetFactory {
    /** A 10 point, plain, serif font */
    public static final Font font10 = new Font("Serif", Font.PLAIN, 10);

    /** A 12 point, plain, serif font */
    public static final Font font12 = new Font("Serif", Font.PLAIN, 12);

    /** A 12 point, bold, serif font */
    public static final Font font12B = new Font("Serif", Font.BOLD, 12);

    /** A 12 point, italic, serif font */
    public static final Font font12I = new Font("Serif", Font.ITALIC, 12);

    /** A 13 point, bold, serif font */
    public static final Font font13B = new Font("Serif", Font.BOLD, 13);

    /** A 14 point, plain, serif font */
    public static final Font font14 = new Font("Serif", Font.PLAIN, 14);

    /** A 14 point, bold, serif font */
    public static final Font font14B = new Font("Serif", Font.BOLD, 14);

    /** A 16 point, bold, serif font */
    public static final Font font16B = new Font("Serif", Font.BOLD, 16);

    /** A 18 point, bold, serif font */
    public static final Font font18B = new Font("Serif", Font.BOLD, 18);

    /** A 10 point, plain, courier font */
    public static final Font courier10 = new Font("Courier", Font.PLAIN, 10);

    /** A 12 point, plain, courier font */
    public static final Font courier12 = new Font("Courier", Font.PLAIN, 12);

    /** A 12 point, bold, courier font */
    public static final Font courier12B = new Font("Courier", Font.BOLD, 12);

    /**
     * Builds a new border of the type used by toolbars.
     * @return a new border
     */
    public static final Border buildToolbarBorder() {
        return BorderFactory.createEtchedBorder();
    }

    /**
     * Builds a new border of the type used when a toggle button is depressed.
     * @return a new border
     */
    public static final Border buildPressedButtonBorder() {
        return BorderFactory.createLoweredBevelBorder();
    }

    /**
     * Builds a titled border with the given title, an etched border, and the proper font and color.
     * @param title the title of the border
     * @return the titled border.
     */
    public static final Border buildTitledBorder(String title) {
        return new TitledBorder(new EtchedBorder(), title, TitledBorder.LEFT, TitledBorder.CENTER, font12B, Color.black);
    }

    /**
     * Finds the icon of the specified name. Uses the PlaceHolder class, which is in the same directory as the icons, to
     * locate the icons.
     * @param name name of the icon
     * @return the icon
     */
    public static final ImageIcon getIcon(String name) {
        URL res;
        ImageIcon icon = null;
        res = PlaceHolder.class.getResource(name);
        if (res != null) {
            icon = new ImageIcon(res);
        } else {
            System.err.println("Unable to find icon: " + name);
        }
        return icon;
    }

    /**
     * Finds the image of the specified name. Uses the PlaceHolder class, which is in the same directory as the icons
     * images, to locate the images.
     * @param name name of the image
     * @return the image
     * @throws FileNotFoundException if we can't find the icon file
     */
    public static final Image getIconImage(String name) throws FileNotFoundException {
        URL res;
        Image img = null;
        res = PlaceHolder.class.getResource(name);
        if (res != null) {
            img = new ImageIcon(res).getImage();
        } else {
            // System.err.println( "Unable to find image: " + name );
            throw new FileNotFoundException(name);
        }
        return img;
    }

    // TODO toolbar -- removed temporarily, see ViewToolbarBuilder
    /**
     * Create a blank toolbar and set it up.
     * @return a new toolbar
     */
    public static final JToolBar initToolbar() {
        JToolBar bar = new JToolBar();
        bar.setBorder(buildToolbarBorder());
        bar.setBorderPainted(true);
        bar.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
        bar.setFloatable(false);
        return bar;
    }

    /**
     * Makes a separator for the use in the toolbars - a button with the proper icon.
     * @return The new separator.
     */
    public static final JButton makeToolbarSeparator() {
        JButton separator = new JButton(MipavUtil.getIcon("separator.gif"));
        separator.setMargin(new Insets(0, 0, 0, 0));
        separator.setBorderPainted(false);
        separator.setFocusPainted(false);
        return (separator);
    }

    // TODO menu -- see ViewMenuBuilder
    
    /**
     * Returns the default size used for most text buttons.
     * @return the default text button size
     */
    public static final Dimension getDefaultButtonSize() {
        return new Dimension(90, 30);
    }

    /**
     * Helper method to build a text button.
     * @param text Text for button.
     * @param toolTip Tool tip to be associated with button.
     * @param action Action command for button.
     * @param listener the listener for this button's actions
     * @return a new text button
     */
    public static final JButton buildTextButton(String text, String toolTip, String action, ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        button.setToolTipText(toolTip);
        button.setFont(WidgetFactory.font12B);
        button.setMinimumSize(new Dimension(20, 20));
        button.setPreferredSize(new Dimension(90, 20));
        button.setMargin(new Insets(2, 7, 2, 7));
        button.setActionCommand(action);
        return button;
    }

    // TODO spinner
    // TODO color chooser
    
    /**
     * Helper method to create a text field with the proper font and font color.
     * @param text Text int the field.
     * @return New text field.
     */
    public static final JTextField buildTextField(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(WidgetFactory.font12);
        tf.setForeground(Color.black);
        return tf;
    }

    /**
     * Builds a new text area.
     * @param text the text to put inside the text area
     * @param isEditable whether the text area should be editable by the user
     * @return the new text area
     */
    public static final JTextArea buildTextArea(String text, boolean isEditable) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(isEditable);
        textArea.setFont(WidgetFactory.font12);
        textArea.setMargin(new Insets(3, 3, 3, 3));
        return textArea;
    }

    /**
     * Create a new scroll pane, containing a component.
     * @param component the component to put inside the scroll pane
     * @param preferredWidth the width of the scroll pane
     * @param preferredHeight the height of the scroll pane
     * @return the new scroll pane
     */
    public static final JScrollPane buildScrollPane(JComponent component, int preferredWidth, int preferredHeight) {
        JScrollPane scrollPane = new JScrollPane(component, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        return scrollPane;
    }

    /**
     * Builds a label with the proper font and font color.
     * @param text text of the label
     * @return the new label
     */
    public static final JLabel buildLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(WidgetFactory.font12);
        label.setForeground(Color.black);
        return label;
    }

    /**
     * Builds a new radio button component.
     * @param label the label to place in front of the radio button
     * @param isSelected whether the check box should initially be selected
     * @param group the button group to add the new radio button to
     * @return the new radio button
     */
    public static final JRadioButton buildRadioButton(String label, boolean isSelected, ButtonGroup group) {
        JRadioButton radio = new JRadioButton(label, isSelected);
        radio.setFont(WidgetFactory.font12);
        group.add(radio);
        return radio;
    }

    /**
     * Builds a new check box component.
     * @param label the label to place in front of the check box
     * @param isSelected whether the check box should initially be selected
     * @return the new check box
     */
    public static final JCheckBox buildCheckBox(String label, boolean isSelected) {
        JCheckBox checkbox = new JCheckBox(label);
        checkbox.setFont(WidgetFactory.font12);
        checkbox.setSelected(isSelected);
        return checkbox;
    }

    /**
     * Builds a new check box component.
     * @param label the label to place in front of the check box
     * @param isSelected whether the check box should initially be selected
     * @param listener the object to notify of ItemEvents generated by this check box (on selection/deselction)
     * @return the new check box
     */
    public static final JCheckBox buildCheckBox(String label, boolean isSelected, ItemListener listener) {
        JCheckBox checkbox = buildCheckBox(label, isSelected);
        checkbox.addItemListener(listener);
        return checkbox;
    }
}
