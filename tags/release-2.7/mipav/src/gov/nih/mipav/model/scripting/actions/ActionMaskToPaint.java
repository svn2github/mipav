package gov.nih.mipav.model.scripting.actions;


import java.awt.event.ActionEvent;

import gov.nih.mipav.model.scripting.*;
import gov.nih.mipav.model.scripting.parameters.*;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.ViewJFrameImage;
import gov.nih.mipav.view.dialogs.AlgorithmParameters;


/**
 * A script action which generates a paint mask based on a mask image.
 */
public class ActionMaskToPaint implements ScriptableActionInterface {

    /**
     * The label to use for the input image parameter.
     */
    private static final String INPUT_IMAGE_LABEL = AlgorithmParameters.getInputImageLabel(1);
    
    /**
     * The mask image used to generate a paint mask (which should now be recorded).
     */
    private ModelImage recordingInputImage;

    /**
     * Constructor for the dynamic instantiation and execution of the MaskToPaint script action.
     */
    public ActionMaskToPaint() {}
    
    /**
     * Constructor used to record the MaskToPaint script action line.
     * @param inputImage  The mask image which was used to generate the paint mask.
     */
    public ActionMaskToPaint(ModelImage inputImage) {
        recordingInputImage = inputImage;
    }
    
    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void insertScriptLine() {
        ParameterTable parameters = new ParameterTable();
        try {
            parameters.put(ParameterFactory.newImage(INPUT_IMAGE_LABEL, recordingInputImage.getImageName()));
        } catch (ParserException pe) {
            MipavUtil.displayError("Error encountered creating input image parameter while recording MaskToPaint script action:\n" + pe);
            return;
        }
        
        ScriptRecorder.getReference().addLine("MaskToPaint", parameters);
    }

    /**
     * {@inheritDoc}
     */
    public void scriptRun(ParameterTable parameters) {
        ModelImage inputImage = parameters.getImage(INPUT_IMAGE_LABEL);
        ViewJFrameImage frame = inputImage.getParentFrame();

        frame.actionPerformed(new ActionEvent(frame, 0, "MaskToPaint"));
    }
    
    /**
     * Changes the mask image used to generate a new paint mask.
     * @param inputImage  The mask image used to generate a paint mask.
     */
    public void setInputImage(ModelImage inputImage) {
        recordingInputImage = inputImage;
    }
}
