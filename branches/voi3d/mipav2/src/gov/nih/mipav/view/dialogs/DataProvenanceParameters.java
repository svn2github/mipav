package gov.nih.mipav.view.dialogs;


import gov.nih.mipav.model.scripting.*;
import gov.nih.mipav.model.scripting.parameters.*;
import gov.nih.mipav.model.structures.*;
import gov.nih.mipav.model.provenance.*;


/**
 * <p>This class standardizes the parameter names given to many common parameters used in algorithms. It also provides
 * helper methods to store/retrieve those common parameters.</p>
 *
 * @see  gov.nih.mipav.view.dialogs.JDialogGaussianBlur
 */
public class DataProvenanceParameters extends AlgorithmParameters {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    
    
    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates a new DataProvenanceParameters object to be used to record the current parameters entered into the algorithm's
     * GUI by the user.
     */
    public DataProvenanceParameters() {
    	super();
    }

    //~ Methods --------------------------------------------------------------------------------------------------------
   
    /**
     * Store an image in the script recorder image variable table. Used to store input/output images while recording a
     * script. Should not be used while running a script.
     *
     * @param   image  The image to store in the variable table.
     *
     * @return  The image placeholder variable assigned to the image by the variable table.
     */
    public String storeImageInRecorder(ModelImage image) {
    	
    	return ProvenanceRecorder.getReference().storeImage(image.getImageName());
    	
    }

    /**
     * Stores an input image in the list of parameters for the algorithm.
     *
     * @param   image       The image to store.
     * @param   paramLabel  The label to give to the new image parameter.
     *
     * @return  The image placeholder variable assigned to the image by the variable table.
     *
     * @throws  ParserException  If there is a problem creating one of the new parameters.
     */
    public String storeImage(ModelImage image, String paramLabel) throws ParserException {
        boolean isExternalImage = !isImageStoredInRecorder(image);

        String var = storeImageInRecorder(image);
        params.put(ParameterFactory.newImage(paramLabel, var, isExternalImage));

        return var;
    }
       
    /**
     * Stores an input image in the list of parameters for the algorithm. This image is stored with a new parameter
     * label incremented with each new call to this method.
     *
     * @param   inputImage  The input image to store.
     *
     * @return  The image placeholder variable assigned to the image by the variable table.
     *
     * @throws  ParserException  If there is a problem creating one of the new parameters.
     */
    public String storeInputImage(ModelImage inputImage) throws ParserException {
        String var = storeImage(inputImage, getInputImageLabel(currentInputImageLabelNumber));

        ProvenanceRecorder.getReference().addInputImage(var);
        
        currentInputImageLabelNumber++;

        return var;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   numIters  DOCUMENT ME!
     *
     * @throws  ParserException  DOCUMENT ME!
     */
    public void storeNumIterations(int numIters) throws ParserException {
        params.put(ParameterFactory.newParameter(NUM_ITERATIONS, numIters));
    }

    /**
     * Stores information about the output of images for this algorithm in the parameter table. It first stores whether
     * a new output image should be generated by the algorithm, and then (if a new image should be generated) stores the
     * new output image in the variable table.
     *
     * @param   outputImage  The result image generated by the algorithm (may be <code>null</code> if <code>
     *                       isNewImage</code> is <code>false</code>.
     * @param   isNewImage   Whether the algorithm should output a new image when it is executed.
     *
     * @return  The new image placeholder variable assigned to the result image (or <code>null</code> if no output image
     *          should be generated).
     *
     * @throws  ParserException  If there is a problem creating one of the new parameters.
     */
    public String storeOutputImageParams(ModelImage outputImage, boolean isNewImage) throws ParserException {
        params.put(ParameterFactory.newBoolean(DO_OUTPUT_NEW_IMAGE, isNewImage));

        
        
        if (isNewImage) {
//        	add and get register name (if already there, gets register name)
            String var = storeImageInRecorder(outputImage);
            
            //set this to the new
            ProvenanceRecorder.getReference().addOutputImage(var);
        	return var;
        
        } else {
        	return null;
        }
    }

    /**
     * Returns whether an image has been registered in the script recorder. If it has not been used, it must be
     * specified externally when this script is run later.
     *
     * @param   image  The image to look for in the recorder's image table.
     *
     * @return  <code>True</code> if the image has been stored in the recorder's image table, <code>false</code>
     *          otherwise.
     */
    protected static final boolean isImageStoredInRecorder(ModelImage image) {
    	
    	return ProvenanceRecorder.getReference().getImageTable().isImageStored(image.getImageName());
    	
    }
}
