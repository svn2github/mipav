package gov.nih.mipav.view.dialogs;

import gov.nih.mipav.view.*;
import gov.nih.mipav.model.structures.*;
import gov.nih.mipav.model.algorithms.*;

import java.awt.event.*;
import java.awt.*;


/**
*   Dialog to call the VOI extraction. This dialog will not be
*   visible because it does not require user input at this time. It
*   was made a dialog object because it may in the future require
*   user input and to be consistent with the dialog/algorithm
*   paradigm. In should be noted, that the algorithms are executed in
*   their own thread.
*
*		@version    0.1 Nov 17, 1998
*		@author     Matthew J. McAuliffe, Ph.D.
*
*/
public class JDialogVOIExtraction extends JDialogBase implements AlgorithmInterface {

    private     AlgorithmVOIExtraction  VOIExtractionAlgo;
    private     ModelImage          imageA = null;      // source image
    private     ViewUserInterface   userInterface;

   /**
    *  Sets variables needed for calling algorithm
    *  @param theParentFrame    Parent frame
    *  @param im                Source image
    */
	public JDialogVOIExtraction(Frame theParentFrame, ModelImage imA) {
		super(theParentFrame, true);
        imageA  = imA;
    }

   /**
   *    Does nothing.
   *    @param event    Event that triggers function
   */
   public void actionPerformed(ActionEvent event) {
   }

   /**
   *    Calls the algorithm.
   */
   public void callAlgorithm() {
        try{
            // Make algorithm
            VOIExtractionAlgo = new AlgorithmVOIExtraction(imageA);
            // This is very important. Adding this object as a listener allows the algorithm to
            // notify this object when it has completed or failed. See algorithm performed event.
            // This is made possible by implementing AlgorithmedPerformed interface
            VOIExtractionAlgo.addListener(this);
            // Hide dialog
            setVisible(false);

            // Start the thread as a low priority because we wish to still have user interface work fast.
            if (VOIExtractionAlgo.startMethod(Thread.MIN_PRIORITY) == false){
                MipavUtil.displayError("A thread is already running on this object");
            }
        }
        catch (OutOfMemoryError x){

            System.gc();
            MipavUtil.displayError("Dialog VOI Extraction: unable to allocate enough memory");
            return;
        }
   }


    //************************************************************************
    //************************** Algorithm Events ****************************
    //************************************************************************

    /**
    *   This method is required if the AlgorithmPerformed
    *   interface is implemented. It is called by the
    *   algorithm when it has completed or failed to
    *   to complete, so that the dialog can display
    *   the result image and/or clean up.
    *   @param algorithm   algorithm that caused the event.
    */
    public void algorithmPerformed(AlgorithmBase algorithm) {
        if (algorithm instanceof AlgorithmVOIExtraction) {
            if (VOIExtractionAlgo.isCompleted() == true) {
                // update frame
                ((ViewJFrameBase)parentFrame).updateImages(true);
            }
        }

    }

}
