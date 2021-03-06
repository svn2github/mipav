package gov.nih.mipav.model.scripting.actions;


import gov.nih.mipav.model.scripting.*;
import gov.nih.mipav.model.scripting.parameters.*;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.view.MipavUtil;


/**
 * A script action which changes the name of an image.
 */
public class ActionChangeName extends ActionImageProcessorBase {

    /**
     * The label to use for the parameter indicating the new image name.
     */
    private static final String IMAGE_NAME_LABEL = "new_image_name";
    
    /**
     * The new name given to the image.
     */
    private String recordingNewImageName;
    
    /**
     * The old image name.
     */
    private String recordingOldImageName;
    
    /**
     * Constructor for the dynamic instantiation and execution of the script action.
     */
    public ActionChangeName() {
        super();
    }
    
    /**
     * Constructor used to record the ChangeName script action line.
     * 
     * @param  image         The image whose name was changed.
     * @param  oldImageName  The old name of the image.
     * @param  newImageName  The new name given to the image (the image's current name).
     */
    public ActionChangeName(ModelImage image, String oldImageName, String newImageName) {
        super(image);
        recordingOldImageName = oldImageName;
        recordingNewImageName = newImageName;
    }
    
    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void insertScriptLine() {
        // change the image name stored in the image table if it has been used in the script before.
        // if it hasn't been used before storing it will be handled in createInputImageParameter()
        ImageVariableTable imageTable = ScriptRecorder.getReference().getImageTable();
        if (imageTable.isImageStored(recordingOldImageName)) {
            imageTable.changeImageName(recordingOldImageName, recordingNewImageName);
        }
        
        ParameterTable parameters = new ParameterTable();
        try {
            parameters.put(createInputImageParameter());
            parameters.put(ParameterFactory.newString(IMAGE_NAME_LABEL, recordingNewImageName));
        } catch (ParserException pe) {
            MipavUtil.displayError("Error encountered while recording " + getActionName() + " script action:\n" + pe);
            return;
        }
        
        ScriptRecorder.getReference().addLine(getActionName(), parameters);
    }

    /**
     * {@inheritDoc}
     */
    public void scriptRun(ParameterTable parameters) {
        ModelImage inputImage = parameters.getImage(INPUT_IMAGE_LABEL);
        
        String oldImageName = inputImage.getImageName();
        String newImageName = parameters.getString(IMAGE_NAME_LABEL);
        
        inputImage.updateFileName(newImageName);
        
        ScriptRunner.getReference().getImageTable().changeImageName(oldImageName, newImageName);
    }
    
    /**
     * Changes the image name which should be recorded as given to the input image.
     * 
     * @param name  The new name given to the input image.
     */
    public void setNewImageName(String name) {
        recordingNewImageName = name;
    }
    
    /**
     * Changes the old name of the input image.
     * 
     * @param name  The old name of the input image.
     */
    public void setOldImageName(String name) {
        recordingOldImageName = name;
    }
}
