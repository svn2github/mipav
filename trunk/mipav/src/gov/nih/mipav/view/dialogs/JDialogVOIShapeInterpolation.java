package gov.nih.mipav.view.dialogs;

import java.awt.event.ActionEvent;
import java.util.Vector;

import gov.nih.mipav.model.algorithms.AlgorithmBase;
import gov.nih.mipav.model.algorithms.AlgorithmInterface;
import gov.nih.mipav.model.algorithms.AlgorithmVOIShapeInterpolation;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.model.structures.VOI;
import gov.nih.mipav.model.structures.VOIContour;
import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.ViewJFrameLightBox;
import gov.nih.mipav.view.ViewJProgressBar;
import gov.nih.mipav.view.ViewVOIVector;

/**
 * @author pandyan
 * 
 * Hidden dialog for calling AlgorithmVOIShapeInterpolation
 *
 */
public class JDialogVOIShapeInterpolation extends JDialogBase implements AlgorithmInterface {
	
	/** src image **/
	private ModelImage imageA;
	
	/** algorithm **/
	private AlgorithmVOIShapeInterpolation alg;

	/** constructor **/
	public JDialogVOIShapeInterpolation() {
		
	}
	
	/** constructor **/
	public JDialogVOIShapeInterpolation(ModelImage imageA) {
		setVisible(false);
		this.imageA = imageA;
		callAlgorithm();
	}

	/** alg performed **/
	public void algorithmPerformed(AlgorithmBase algorithm) {
		alg = null;
		dispose();

	}

	/** action perf **/
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}
	
	/** call algorithm **/
	protected void callAlgorithm() {

         
         ViewVOIVector VOIs = (ViewVOIVector) imageA.getVOIs();
         int nVOI = VOIs.size();
         int nActiveContour = 0;
         VOIContour VOI1 = null;
         int sliceIndex1 = 0;
         VOIContour VOI2 = null;
         int sliceIndex2 = 0;
         VOI VOIHandle = null;
         
         if(nVOI == 0) {
         	 MipavUtil.displayError("Please select 2 closed VOI contours in non-contiguous slices");
             return;
         }
         
         for (int i = 0; i < nVOI; i++) {

             if (VOIs.VOIAt(i).isActive() && (VOIs.VOIAt(i).getCurveType() == VOI.CONTOUR)) {
             	VOI tempVOI = (VOI)(VOIs.VOIAt(i).clone());
             	tempVOI.setUID(tempVOI.hashCode());
             	Vector[] contours = tempVOI.getCurves();
             	int nSlices = contours.length;
             	
             	
             	for (int j = 0; j < nSlices; j++) {
                     int nContours = contours[j].size();
                     for (int k = 0; k < nContours; k++) {
                         if (((VOIContour) contours[j].elementAt(k)).isActive()) {
                         	nActiveContour = nActiveContour + 1;
                         	if(VOI1 == null) {
                         		VOI1 = (VOIContour)(VOIContour)contours[j].elementAt(k);
                         		VOIHandle = (VOI)(VOIs.VOIAt(i));
                         		sliceIndex1 = j;

                         	}else {
                         		if((VOI)(VOIs.VOIAt(i)) != VOIHandle) {
                         			MipavUtil.displayError("Contours must be from the same VOI");
                                    return;
                         		}
                         		VOI2 = (VOIContour)(VOIContour)contours[j].elementAt(k);
                         		sliceIndex2 = j;

                         	}
                         }
                     }
             	}
             }
         }
         

         
         if(nActiveContour != 2) {
         	MipavUtil.displayError("Please select 2 closed VOI contours in non-contiguous slices");
             return;
         }
         
         if(sliceIndex1 == sliceIndex2) {
         	MipavUtil.displayError("Please select 2 closed VOI contours in non-contiguous slices");
             return;
         }
         
        if((sliceIndex1 == sliceIndex2 + 1) || (sliceIndex1 == sliceIndex2 - 1)) {
         	MipavUtil.displayError("Please select 2 closed VOI contours in non-contiguous slices");
             return;
        }
        
        //ok now we have 2 selected closed VOI contours in non-contiguous slices
        alg = new AlgorithmVOIShapeInterpolation(imageA,sliceIndex1,VOI1,sliceIndex2,VOI2,VOIHandle); 

        createProgressBar(imageA.getImageName(), alg);
        
        if (isRunInSeparateThread()) {

            // Start the thread as a low priority because we wish to still have user interface work fast.
            if (alg.startMethod(Thread.MIN_PRIORITY) == false) {
                MipavUtil.displayError("A thread is already running on this object");
            }
        } else {
            alg.run();

        }

		 
	 }

}
