package gov.nih.mipav.model.scripting.actions;


import gov.nih.mipav.model.scripting.*;
import gov.nih.mipav.model.scripting.parameters.*;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.view.dialogs.AlgorithmParameters;


/**
 * A base class for script actions which perform their action using an input image.
 */
public abstract class ActionImageProcessorBase extends ActionBase {
    /**
     * The label to use for the input image parameter.
     */
    protected static final String INPUT_IMAGE_LABEL = AlgorithmParameters.getInputImageLabel(1);
    
    /**
     * The image whose processing should be recorded in the script.  The actual processing must be done elsewhere.
     */
    protected ModelImage recordingInputImage;
    
    /**
     * Constructor for the dynamic instantiation and execution of the script action.
     */
    public ActionImageProcessorBase() {}
    
    /**
     * Constructor used to record the Clone script action line.
     * @param input  The image which was cloned.
     */
    public ActionImageProcessorBase(ModelImage input) {
        recordingInputImage = input;
    }
    
    /**
     * {@inheritDoc}
     */
    public abstract void insertScriptLine();
    
    /**
     * {@inheritDoc}
     */
    public abstract void scriptRun(ParameterTable parameters);
    
    /**
     * Changes the image whose processing should be recorded in the script.
     * @param inputImage  The image which was processed.
     */
    public void setInputImage(ModelImage inputImage) {
        recordingInputImage = inputImage;
    }
    
    /**
     * Returns whether an image has been registered in the script recorder.  If it has not been used, it must be specified externally when this script is run later.
     * 
     * @param   image  The image to look for in the recorder's image table.
     * 
     * @return  <code>True</code> if the image has been stored in the recorder's image table, <code>false</code> otherwise.
     */
    protected static final boolean isImageStoredInRecorder(ModelImage image) {
        return ScriptRecorder.getReference().getImageTable().isImageStored(image.getImageName());
    }
    
    /**
     * Creates a new image parameter for the action's input image, determining whether it needs to be externally-specified or will be generated from another script action.
     * 
     * @return  A new image parameter pointing to this action's input image.
     * 
     * @throws  ParserException  If there is a problem encountered while creating the new parameter.
     */
    protected ParameterImage createInputImageParameter() throws ParserException {
        boolean isExternalImage = isImageStoredInRecorder(recordingInputImage);
        
        String var = storeImageInRecorder(recordingInputImage);
        
        return ParameterFactory.newImage(INPUT_IMAGE_LABEL, var, isExternalImage);
    }
    
    /**
     * Store an image in the script recorder image variable table.  Used to store input/output images while recording a script.  Should not be used while running a script.
     *
     * @param   image  The image to store in the variable table.
     *
     * @return  The image placeholder variable assigned to the image by the variable table.
     */
    protected static final String storeImageInRecorder(ModelImage image) {
        return ScriptRecorder.getReference().storeImage(image.getImageName());
    }
    
    /**
     * Store an image in the script runner image variable table.  Used to store input/output images while running a script.  Should not be used while recording a script.
     *
     * @param   image  The image to store in the variable table.
     *
     * @return  The image placeholder variable assigned to the image by the variable table.
     */
    protected static final String storeImageInRunner(ModelImage image) {
        return ScriptRunner.getReference().storeImage(image.getImageName());
    }
}
