package gov.nih.mipav.view.renderer.surfaceview;


import gov.nih.mipav.view.*;
import gov.nih.mipav.view.renderer.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;


/**
 * Dialog to get the two parameters needed for surface smoothing, the iterations and the alpha.
 */
public class JPanelSurfaceSmooth extends JPanelRendererBase {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = -4243866003675978933L;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** Alpha smoothing factor. */
    private float alpha;

    /** Text field for getting alpha smoothing factor. */
    private JTextField alphaText;

    /** Number of iterations for smoothing formula. */
    private int iterations;

    /** Text field for getting iterations. */
    private JTextField iterationsText;

    /**
     * If limitCheckBox is selected iterations stop when volume change from initial volume is greater than or equal to
     * volumePercent.
     */
    private JCheckBox limitCheckBox;

    /** DOCUMENT ME! */
    private boolean volumeLimit;

    /** DOCUMENT ME! */
    private float volumePercent = 3.0f;

    /** DOCUMENT ME! */
    private JTextField volumeText;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates new dialog to get iterations and alpha for smoothing a mesh surface.
     *
     * @param  parent  Parent frame.
     */
    public JPanelSurfaceSmooth(SurfaceRender parent) {
        super(parent);
        init();
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Sets iterations and alpha when "OK" is pressed; disposes dialog when "Cancel" is pressed.
     *
     * @param  e  Event that triggered function.
     */
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String command = e.getActionCommand();

        if (source == cancelButton) {
            cancelFlag = true;
            setVisible(false);
            // setVisible(false);
        } else if (source == limitCheckBox) {

            if (limitCheckBox.isSelected()) {
                volumeText.setEnabled(true);
            } else {
                volumeText.setEnabled(false);
            }
        } // else if (source == limitCheckBox)
        else if ((source == OKButton) || command.equals("Smooth")) {

            if (testParameter(iterationsText.getText(), 0, 10000)) {
                iterations = Integer.valueOf(iterationsText.getText()).intValue();
            } else {
                iterationsText.requestFocus();
                iterationsText.selectAll();

                return;
            }

            if (testParameter(alphaText.getText(), 0, 1)) {
                alpha = Float.valueOf(alphaText.getText()).floatValue();
            } else {
                alphaText.requestFocus();
                alphaText.selectAll();

                return;
            }

            if (limitCheckBox.isSelected()) {
                volumeLimit = true;

                if (testParameter(volumeText.getText(), 0.0, 100.0)) {
                    volumePercent = Float.valueOf(volumeText.getText()).floatValue();
                } else {
                    volumeText.requestFocus();
                    volumeText.selectAll();

                    return;
                }
            } else {
                volumeLimit = false;
            }

            // dispose();
            setVisible(false);
        }
    }

    /**
     * Accessor that returns the alpha smoothing factor.
     *
     * @return  Alpha smoothing factor.
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * Accessor that returns the number of iterations.
     *
     * @return  Number of iterations.
     */
    public int getIterations() {
        return iterations;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Accessor that returns whether or not iterations are stopped after the present volume is different from the
     * initial volume by volumePercent or more.
     *
     * @return  volumeLimit
     */
    public boolean getVolumeLimit() {
        return volumeLimit;
    }

    /**
     * Accessor that returns the percentage difference from the initial volume at which iterations stop.
     *
     * @return  volumePercent
     */
    public float getVolumePercent() {
        return volumePercent;
    }

    /**
     * Setup variabls before smoothing.
     */
    public void setVariables() {

        if (testParameter(iterationsText.getText(), 0, 10000)) {
            iterations = Integer.valueOf(iterationsText.getText()).intValue();
        } else {
            iterationsText.requestFocus();
            iterationsText.selectAll();

            return;
        }

        if (testParameter(alphaText.getText(), 0, 1)) {
            alpha = Float.valueOf(alphaText.getText()).floatValue();
        } else {
            alphaText.requestFocus();
            alphaText.selectAll();

            return;
        }

        if (limitCheckBox.isSelected()) {
            volumeLimit = true;

            if (testParameter(volumeText.getText(), 0.0, 100.0)) {
                volumePercent = Float.valueOf(volumeText.getText()).floatValue();
            } else {
                volumeText.requestFocus();
                volumeText.selectAll();

                return;
            }
        } else {
            volumeLimit = false;
        }

    }

    /**
     * Initializes GUI components and displays dialog.
     */
    private void init() {

        // setTitle("Smoothing options");
        JPanel optionsPanel = new JPanel(new GridLayout(3, 2));

        optionsPanel.setBorder(buildTitledBorder("Parameters"));

        JLabel iterationsLabel = new JLabel(" Number of iterations: ");

        iterationsLabel.setFont(serif12);
        iterationsLabel.setForeground(Color.black);
        optionsPanel.add(iterationsLabel);

        iterationsText = new JTextField(5);
        iterationsText.setFont(serif12);
        iterationsText.setText("50");
        optionsPanel.add(iterationsText);

        JLabel alphaLabel = new JLabel(" Smoothing factor: ");

        alphaLabel.setFont(serif12);
        alphaLabel.setForeground(Color.black);
        optionsPanel.add(alphaLabel);

        alphaText = new JTextField(5);
        alphaText.setFont(serif12);
        alphaText.setText(".05");
        optionsPanel.add(alphaText);

        limitCheckBox = new JCheckBox("Volume % change limit:");
        limitCheckBox.setFont(serif12);
        limitCheckBox.setForeground(Color.black);
        limitCheckBox.setSelected(false);
        limitCheckBox.addActionListener(this);
        optionsPanel.add(limitCheckBox);

        volumeText = new JTextField(7);
        volumeText.setFont(serif12);
        volumeText.setText("3.0");
        volumeText.setEnabled(false);
        optionsPanel.add(volumeText);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(optionsPanel);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel buttonPanel = new JPanel();

        OKButton = buildOKButton();
        cancelButton = buildCancelButton();
        buttonPanel.add(OKButton);
        buttonPanel.add(cancelButton);

        // getContentPane().add(mainPanel);
        // getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        // pack();
        // setVisible(true);
    }

}
