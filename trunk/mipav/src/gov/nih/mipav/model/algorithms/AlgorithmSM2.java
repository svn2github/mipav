package gov.nih.mipav.model.algorithms;


import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.util.BitSet;

import java.awt.*;

import java.io.*;

import java.text.*;


/**
 * Based on the document provided by Daniel Reich:
 * Notes on DCE with SM2 (standard model, aka Tofts model, 2-compartment) February 4, 2010 - revision 3
 * 3 model parameters are fit for each voxel in 3D:
 * 1) K_trans in [1.0E-5, 0.99]
 * 2) ve in [1.0E-5, 0.99]
 * 3) f_vp in [0, 0.99]
 
 * srcImage is a dynamic "4D volume" of MRI signal (3D over time).
 
 
 References:
 1.) "A Unified Magnetic Resonance Imaging Pharmacokinetic Theory: Intravascular and Extracellular Contrast
 Reagents" by Xin Li, William D. Rooney, and Charles S. Springer, Jr., Magnetic Resonance in Medicine,
 Vol. 54, 2005, pp. 1351-1359.
 2.) Erratum: Magnetic Resonance in Medicine, Vol. 55, 2006, p.1217.
 3.) Quantitative MRI of the Brain, Edited by Paul Tofts, 2003, John Wiley & Sons, Ltd.,
 ISBN: 0-47084721-2, Chapter 10, T1-w DCE-MRI: T1-weighted Dynamic Contrast-enhanced MRI by
 Geoff J. M. Parker and Anwar R. Padhani, pp. 341-364.
 4.) Larsson HBW, Courivaud F, Rostrup E, Hansen AE.  Measurement of brain perfusion, blood volume, and 
 blood-brain barrier permeability, using dynamic contrast-enhanced T1-weighted MRI at 3 tesla.
 Magnetic Resonance in Medicine 2009; 62(5):1270-1281.
 5.) Li X, Rooney WD, Springer CS.  A unified magnetic resonance imaging pharmacokinetic theory:
 intravascular and extracellular contrast reagents.  Magnetic Resonance in Medicine
 2005 Dec; 54(6): 1351-1359.
 6.) Tofts PS, Modeling tracer kinetics in dynamic Gd-DPTA MR imaging.  Journal of Magnetic
 Resonance Imaging, 1997, 7(1), pp. 91-101.
 7.) Tofts PS, Brix G, Buckley DL, Evelhoch JL, Henderson E, Knopp MV, Larsson HB, Mayr NA,
 Parker GJ, Port RE, Taylor J, Weisskoff RM.  Estimating kinetic parameters from dynamic 
 contrast-enhanced T(1)-weighted MRI of a diffusable tracer: standardized quantitites and
 symbols.  J. Magn. Reson Imaging 1999 Sep; 10(3), pp. 223-232. 
 */
public class AlgorithmSM2 extends AlgorithmBase {

    //~ Instance fields ------------------------------------------------------------------------------------------------
    
    /** A vector of center times for each volume */
    private double timeVals[] = null;
    
    private double r1t0[];
    
    private double r1tj[];
    
    private double r1pt0;
    
    private double r1ptj[];
    
    private double min_constr[];
    private double max_constr[];
    private double ymodel[];
    private int i;
    private int xDim;
    private int yDim;
    private int zDim;
    private int tDim;
    private double initial[] = new double[3];
    private double trapezoidSlope[];
    private double trapezoidConstant[];
    private double ktransDivVe;
    private double exparray[][];
    private int[] exitStatus;
    private int[] paramNaN = new int[3];
    private int[] paramInf = new int[3];
    private double[] paramMin = new double[3];
    private double[] paramMax = new double[3];
    
    private ModelImage tissueImage;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates a new AlgorithmDEMRI3 object.
     
     */
    public AlgorithmSM2(ModelImage destImage, ModelImage srcImage, double min_constr[], double max_constr[],
    		               ModelImage tissueImage, double timeVals[]) {

        super(destImage, srcImage);
        this.min_constr = min_constr;
        this.max_constr = max_constr;
        this.tissueImage = tissueImage;
        this.timeVals = timeVals;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Prepares this class for destruction.
     */
    public void finalize() {
        srcImage = null;
        destImage = null;
        min_constr = null;
        max_constr = null;
        if (tissueImage != null) {
        	tissueImage.disposeLocal();
        	tissueImage = null;
        }
        initial = null;
        super.finalize();
    }

    /**
     * starts the algorithm.
     */
    public void runAlgorithm() {
        ViewVOIVector VOIs;
        BitSet mask;
        int volSize;
        int t;
        double y_array[];
        int size4D;
        FitSM2ConstrainedModel dModel;
        double[] params;
        float destArray[];
        int j;
        // If true, run a self test of NLConstrainedEngine.java
        boolean fullTest = false;
        int voiCount;
        double delT;
        long normalTerminations = 0;
        long abnormalTerminations = 0;
        
        if (fullTest) {
        	new FitAll();
        	return;
        }
        
        if (srcImage.getNDims() != 4) {
            MipavUtil.displayError("srcImage must be 4D");
            setCompleted(false);
            return;
        }
        
        xDim = srcImage.getExtents()[0];
        yDim = srcImage.getExtents()[1];
        zDim = srcImage.getExtents()[2];
        tDim = srcImage.getExtents()[3];
        volSize = xDim * yDim * zDim;
        size4D = volSize * tDim;
        destArray = new float[3 * volSize];
       
        r1t0 = new double[volSize];
        r1tj = new double[size4D];
        r1ptj = new double[tDim];
        
        
    	try {
    	    tissueImage.exportData(0, volSize, r1t0);
    	}
    	catch(IOException e) {
    		MipavUtil.displayError("IOException on tissueImage.exportData(0, volSize, r1t0)");
    		setCompleted(false);
    		return;
    	}
    	tissueImage.disposeLocal();
    	tissueImage = null;
        
       
        VOIs = srcImage.getVOIs();
        int nVOIs = VOIs.size();
        int nBoundingVOIs = 0;

        for (i = 0; i < nVOIs; i++) {

            if ((VOIs.VOIAt(i).getCurveType() == VOI.CONTOUR) || (VOIs.VOIAt(i).getCurveType() == VOI.POLYLINE)) {
                nBoundingVOIs++;
            }
        }
        
        if (nBoundingVOIs == 0) {
            MipavUtil.displayError("No bounding vois around sagittal sinus");
            setCompleted(false);
            return;
        }
        
        if (nBoundingVOIs > 1) {
            MipavUtil.displayError(nBoundingVOIs + " bounding vois around sagittal sinus instead of the expected 1");
            setCompleted(false);
            return;
        }
        
        mask = srcImage.generateVOIMask();
        
        try {
            srcImage.exportData(0, size4D, r1tj);
        }
        catch (IOException e) {
            MipavUtil.displayError("IOException on srcImage.exportData(0, size4D, r1tj");
            setCompleted(false);
            return;
        }
        
        voiCount = 0;
        r1pt0 = 0.0;
        // Actually looking from t = t1 to t = tlast
        for (t = 0; t < tDim; t++) {
            for (i = 0; i < volSize; i++) {
                if (mask.get(i)) {
                    r1ptj[t] += r1tj[i + t*volSize];
                    if (t == 0) {
                    	r1pt0 += r1t0[i];
                        voiCount++;	
                    }
                }  
            }
            if (t == 0) {
            	r1pt0 = r1pt0/voiCount;
            }
            r1ptj[t] = r1ptj[t]/voiCount;
        } // for (t = 0; t < tDim; t++)
        
        // Actually looking from t = t1 to t = tlast
        for (t = 0; t < tDim; t++) {
        	// For t = t1, looking at R1,p(t1) - R10,p, but R1,p(t1) and R10,p are identical except for noise
        	r1ptj[t] = r1ptj[t] - r1pt0;
        	for (i = 0; i < volSize; i++) {
        	    r1tj[i + t*volSize] = r1tj[i + t*volSize] - r1t0[i];	
        	}
        } // for (t = 0; t < tDim; t++)
        r1t0 = null;
        
        trapezoidSlope = new double[tDim-1];
        trapezoidConstant = new double[tDim-1];
        for (t = 0; t < tDim - 1; t++) {
        	delT = timeVals[t+1] - timeVals[t];
        	trapezoidSlope[t] = (r1ptj[t+1]-r1ptj[t])/delT;
        	trapezoidConstant[t] = (r1ptj[t]*timeVals[t+1] - r1ptj[t+1]*timeVals[t])/delT;
        }
        
        for (i = 0; i < 3; i++) {
        	initial[i] = (min_constr[i] + max_constr[i])/2.0;
        }
        
        y_array = new double[tDim-1];
        ymodel = new double[tDim-1];
        exparray = new double[tDim][tDim];
        exitStatus = new int[12356];
        
        for (i = 0; i < 3; i++) {
        	paramMin[i] = Double.MAX_VALUE;
        	paramMax[i] = -Double.MAX_VALUE;
        }
        
        for (i = 0; i < volSize; i++) {
        	fireProgressStateChanged(i * 100/volSize);
            for (t = 1; t < tDim; t++) {
            	y_array[t-1] = r1tj[t*volSize + i];
            }
            // Note that the nPts, tDim-1, is the number of points in the y_array.
            dModel = new FitSM2ConstrainedModel(tDim-1, r1ptj, y_array, initial);
            dModel.driver();
            //dModel.dumpResults();
            params = dModel.getParameters();
            for (j = 0; j < 3; j++) {
            	destArray[j*volSize + i] = (float)params[j];
            	if (Double.isNaN(params[j])) {
            	    paramNaN[j]++;	
            	}
            	else if (Double.isInfinite(params[j])) {
            		paramInf[j]++;
            	}
            	else {
	            	if (params[j] < paramMin[j]) {
	            		paramMin[j] = params[j];
	            	}
	            	if (params[j] > paramMax[j]) {
	            		paramMax[j] = params[j];
	            	}
            	}
            }
            exitStatus[(dModel.getExitStatus() + 11)]++;
        } // for (i = 0; i < volSize; i++)
        
        if (paramNaN[0] > 0) {
        	System.out.println(paramNaN[0] + " of ktrans values are NaN");
        	Preferences.debug(paramNaN[0] + " of ktrans values are NaN\n");
        }
        
        if (paramNaN[1] > 0) {
        	System.out.println(paramNaN[1] + " of ve values are NaN");
        	Preferences.debug(paramNaN[1] + " of ve values are NaN\n");
        }
        
        if (paramNaN[2] > 0) {
        	System.out.println(paramNaN[2] + " of vp values are NaN");
        	Preferences.debug(paramNaN[2] + " of vp values are NaN\n");
        }
        
        if (paramInf[0] > 0) {
        	System.out.println(paramInf[0] + " of ktrans values are infinite");
        	Preferences.debug(paramInf[0] + " of ktrans values are infinite\n");
        }
        
        if (paramInf[1] > 0) {
        	System.out.println(paramInf[1] + " of ve values are infinite");
        	Preferences.debug(paramInf[1] + " of ve values are infinite\n");
        }
        
        if (paramInf[2] > 0) {
        	System.out.println(paramInf[2] + " of vp values are infinite");
        	Preferences.debug(paramInf[2] + " of vp values are infinite\n");
        }
        
        System.out.println("ktrans minimum value = " + paramMin[0]);
        Preferences.debug("ktrans minimum value = " + paramMin[0] + "\n");
        System.out.println("ktrans maximum value = " + paramMax[0]);
        Preferences.debug("ktrans maximum value = " + paramMax[0] + "\n");
        
        System.out.println("ve minimum value = " + paramMin[1]);
        Preferences.debug("ve minimum value = " + paramMin[1] + "\n");
        System.out.println("ve maximum value = " + paramMax[1]);
        Preferences.debug("ve maximum value = " + paramMax[1] + "\n");
        
        System.out.println("vp minimum value = " + paramMin[2]);
        Preferences.debug("vp minimum value = " + paramMin[2] + "\n");
        System.out.println("vp maximum value = " + paramMax[2]);
        Preferences.debug("vp maximum value = " + paramMax[2] + "\n");
        
        if (exitStatus[12351] > 0) {
        	normalTerminations += exitStatus[12351];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)");
            System.out.println("Number = " + exitStatus[12351]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)\n");
            Preferences.debug("Number = " + exitStatus[12351] + "\n");
        }
        
        if (exitStatus[12352] > 0) {
        	normalTerminations += exitStatus[12352];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The last steps were computed with prank <> n at the termination point");
            System.out.println("Number = " + exitStatus[12352]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");	
            Preferences.debug("The last steps were computed with prank <> n at the termination point\n");
            Preferences.debug("Number = " + exitStatus[12352] + "\n");
        }
        
        if (exitStatus[12353] > 0) {
        	normalTerminations += exitStatus[12353];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The method of Newton was used (at least) in the last step");
            System.out.println("Number = " + exitStatus[12353]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");	
            Preferences.debug("The method of Newton was used (at least) in the last step\n");
            Preferences.debug("Number = " + exitStatus[12353] + "\n");
        }
        
        if (exitStatus[12354] > 0) {
        	normalTerminations += exitStatus[12354];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The 2nd but last step was subspace minimization but");
            System.out.println("the last two were Gauss-Newton steps");
            System.out.println("Number = " + exitStatus[12354]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx and\n");
        	Preferences.debug("because we are computing at noise level\n");
        	Preferences.debug("The 2nd but last step was subspace minimization but\n");
            Preferences.debug("the last two were Gauss-Newton steps \n");
            Preferences.debug("Number = " + exitStatus[12354] + "\n");
        }
        
        if (exitStatus[12355] > 0) {
        	normalTerminations += exitStatus[12355];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The steplength was not unit in both the last two steps\n");	
            System.out.println("Number = " + exitStatus[12355] + "\n");
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The steplength was not unit in both the last two steps\n");	
            Preferences.debug("Number = " + exitStatus[12355] + "\n");
        }
        
        if (exitStatus[12311] > 0) {
        	normalTerminations += exitStatus[12311];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx");
        	System.out.println("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)");
            System.out.println("Number = " + exitStatus[12311]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx\n");
        	Preferences.debug("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)\n");
            Preferences.debug("Number = " + exitStatus[12311] + "\n");
        }
        
        if (exitStatus[12312] > 0) {
        	normalTerminations += exitStatus[12312];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx");
        	System.out.println("The last steps were computed with prank <> n at the termination point");
            System.out.println("Number = " + exitStatus[12312]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx\n");
        	Preferences.debug("The last steps were computed with prank <> n at the termination point\n");
            Preferences.debug("Number = " + exitStatus[12312] + "\n");	
        }
        
        if (exitStatus[12313] > 0) {
        	normalTerminations += exitStatus[12313];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx");
        	System.out.println("The method of Newton was used (at least) in the last step");
            System.out.println("Number = " + exitStatus[12313]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx\n");
        	Preferences.debug("The method of Newton was used (at least) in the last step\n");
            Preferences.debug("Number = " + exitStatus[12313] + "\n");	
        }
        
        if (exitStatus[12314] > 0) {
        	normalTerminations += exitStatus[12314];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx");
        	System.out.println("The 2nd but last step was subspace minimization but");
            System.out.println("the last two were Gauss-Newton steps");
            System.out.println("Number = " + exitStatus[12314]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx\n");
        	Preferences.debug("The 2nd but last step was subspace minimization but\n");
            Preferences.debug("the last two were Gauss-Newton steps \n");
            Preferences.debug("Number = " + exitStatus[12314] + "\n");	
        }
        
        if (exitStatus[12315] > 0) {
        	normalTerminations += exitStatus[12315];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx");
        	System.out.println("The steplength was not unit in both the last two steps\n");
            System.out.println("Number = " + exitStatus[12315]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx\n");
        	Preferences.debug("The steplength was not unit in both the last two steps\n");	
            Preferences.debug("Number = " + exitStatus[12315] + "\n");	
        }
        
        if (exitStatus[12051] > 0) {
        	normalTerminations += exitStatus[12051];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2 and");
            System.out.println("because we are computing at noise level");
            System.out.println("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)");
            System.out.println("Number = " + exitStatus[12051]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2 and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)\n");
            Preferences.debug("Number = " + exitStatus[12051] + "\n");
        }
        
        if (exitStatus[12052] > 0) {
        	normalTerminations += exitStatus[12052];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2 and");
            System.out.println("because we are computing at noise level");
            System.out.println("The last steps were computed with prank <> n at the termination point");
            System.out.println("Number = " + exitStatus[12052]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2 and\n");
            Preferences.debug("because we are computing at noise level\n");	
            Preferences.debug("The last steps were computed with prank <> n at the termination point\n");
            Preferences.debug("Number = " + exitStatus[12052] + "\n");
        }
        
        if (exitStatus[12053] > 0) {
        	normalTerminations += exitStatus[12053];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2 and");
            System.out.println("because we are computing at noise level");
            System.out.println("The method of Newton was used (at least) in the last step");
            System.out.println("Number = " + exitStatus[12053]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2 and\n");
            Preferences.debug("because we are computing at noise level\n");	
            Preferences.debug("The method of Newton was used (at least) in the last step\n");
            Preferences.debug("Number = " + exitStatus[12053] + "\n");
        }
        
        if (exitStatus[12054] > 0) {
        	normalTerminations += exitStatus[12054];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2 and");
            System.out.println("because we are computing at noise level");
            System.out.println("The 2nd but last step was subspace minimization but");
            System.out.println("the last two were Gauss-Newton steps");
            System.out.println("Number = " + exitStatus[12054]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because we are computing at noise level\n");
        	Preferences.debug("The 2nd but last step was subspace minimization but\n");
            Preferences.debug("the last two were Gauss-Newton steps \n");
            Preferences.debug("Number = " + exitStatus[12054] + "\n");
        }
        
        if (exitStatus[12055] > 0) {
        	normalTerminations += exitStatus[12055];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2 and");
            System.out.println("because we are computing at noise level");
            System.out.println("The steplength was not unit in both the last two steps\n");	
            System.out.println("Number = " + exitStatus[12055] + "\n");
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2 and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The steplength was not unit in both the last two steps\n");	
            Preferences.debug("Number = " + exitStatus[12055] + "\n");
        }
        
        if (exitStatus[12011] > 0) {
        	normalTerminations += exitStatus[12011];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2");
        	System.out.println("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)");
            System.out.println("Number = " + exitStatus[12011]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2\n");
        	Preferences.debug("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)\n");
            Preferences.debug("Number = " + exitStatus[12011] + "\n");
        }
        
        if (exitStatus[12012] > 0) {
        	normalTerminations += exitStatus[12012];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2");
        	System.out.println("The last steps were computed with prank <> n at the termination point");
            System.out.println("Number = " + exitStatus[12012]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2\n");
        	Preferences.debug("The last steps were computed with prank <> n at the termination point\n");
            Preferences.debug("Number = " + exitStatus[12012] + "\n");	
        }
        
        if (exitStatus[12013] > 0) {
        	normalTerminations += exitStatus[12013];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2");
        	System.out.println("The method of Newton was used (at least) in the last step");
            System.out.println("Number = " + exitStatus[12013]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2\n");
        	Preferences.debug("The method of Newton was used (at least) in the last step\n");
            Preferences.debug("Number = " + exitStatus[12013] + "\n");	
        }
        
        if (exitStatus[12014] > 0) {
        	normalTerminations += exitStatus[12014];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2");
        	System.out.println("The 2nd but last step was subspace minimization but");
            System.out.println("the last two were Gauss-Newton steps");
            System.out.println("Number = " + exitStatus[12014]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2\n");
        	Preferences.debug("The 2nd but last step was subspace minimization but\n");
            Preferences.debug("the last two were Gauss-Newton steps \n");
            Preferences.debug("Number = " + exitStatus[12014] + "\n");	
        }
        
        if (exitStatus[12015] > 0) {
        	normalTerminations += exitStatus[12015];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the sum of squares is less than epsabs**2");
        	System.out.println("The steplength was not unit in both the last two steps\n");
            System.out.println("Number = " + exitStatus[12015]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the sum of squares is less than epsabs**2\n");
        	Preferences.debug("The steplength was not unit in both the last two steps\n");	
            Preferences.debug("Number = " + exitStatus[12015] + "\n");	
        }
        
        if (exitStatus[10351] > 0) {
        	normalTerminations += exitStatus[10351];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)");
            System.out.println("Number = " + exitStatus[10351]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)\n");
            Preferences.debug("Number = " + exitStatus[10351] + "\n");
        }
        
        if (exitStatus[10352] > 0) {
        	normalTerminations += exitStatus[10352];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The last steps were computed with prank <> n at the termination point");
            System.out.println("Number = " + exitStatus[10352]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The last steps were computed with prank <> n at the termination point\n");
            Preferences.debug("Number = " + exitStatus[10352] + "\n");	
        }
        
        if (exitStatus[10353] > 0) {
        	normalTerminations += exitStatus[10353];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The method of Newton was used (at least) in the last step");
            System.out.println("Number = " + exitStatus[10353]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The method of Newton was used (at least) in the last step\n");
            Preferences.debug("Number = " + exitStatus[10353] + "\n");		
        }
        
        if (exitStatus[10354] > 0) {
        	normalTerminations += exitStatus[10354];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The 2nd but last step was subspace minimization but");
            System.out.println("the last two were Gauss-Newton steps");
            System.out.println("Number = " + exitStatus[10354]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The 2nd but last step was subspace minimization but\n");
            Preferences.debug("the last two were Gauss-Newton steps \n");
            Preferences.debug("Number = " + exitStatus[10354] + "\n");			
        }
        
        if (exitStatus[10355] > 0) {
        	normalTerminations += exitStatus[10355];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The steplength was not unit in both the last two steps\n");
            System.out.println("Number = " + exitStatus[10355]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The steplength was not unit in both the last two steps\n");
            Preferences.debug("Number = " + exitStatus[10355] + "\n");				
        }
        
        if (exitStatus[10311] > 0) {
        	normalTerminations += exitStatus[10311];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the relative change in x is less than epsx");
        	System.out.println("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)");
            System.out.println("Number = " + exitStatus[10311]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx\n");
        	Preferences.debug("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)\n");
            Preferences.debug("Number = " + exitStatus[10311] + "\n");
        }
        
        if (exitStatus[10312] > 0) {
        	normalTerminations += exitStatus[10312];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the relative change in x is less than epsx");
        	System.out.println("The last steps were computed with prank <> n at the termination point");
            System.out.println("Number = " + exitStatus[10312]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx\n");
        	Preferences.debug("The last steps were computed with prank <> n at the termination point\n");
            Preferences.debug("Number = " + exitStatus[10312] + "\n");	
        }
        
        if (exitStatus[10313] > 0) {
        	normalTerminations += exitStatus[10313];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the relative change in x is less than epsx");
        	System.out.println("The method of Newton was used (at least) in the last step");
            System.out.println("Number = " + exitStatus[10313]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx\n");
        	Preferences.debug("The method of Newton was used (at least) in the last step\n");
            Preferences.debug("Number = " + exitStatus[10313] + "\n");	
        }
        
        if (exitStatus[10314] > 0) {
        	normalTerminations += exitStatus[10314];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the relative change in x is less than epsx");
        	System.out.println("The 2nd but last step was subspace minimization but");
            System.out.println("the last two were Gauss-Newton steps");
            System.out.println("Number = " + exitStatus[10314]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx\n");
        	Preferences.debug("The 2nd but last step was subspace minimization but\n");
            Preferences.debug("the last two were Gauss-Newton steps \n");
            Preferences.debug("Number = " + exitStatus[10314] + "\n");	
        }
        
        if (exitStatus[10315] > 0) {
        	normalTerminations += exitStatus[10315];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
        	System.out.println("because the relative change in x is less than epsx");
        	System.out.println("The steplength was not unit in both the last two steps\n");
            System.out.println("Number = " + exitStatus[10315]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx\n");
        	Preferences.debug("The steplength was not unit in both the last two steps\n");	
            Preferences.debug("Number = " + exitStatus[10315] + "\n");	
        }
        
        if (exitStatus[10051] > 0) {
        	normalTerminations += exitStatus[10051];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
            System.out.println("because we are computing at noise level");
            System.out.println("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)");
            System.out.println("Number = " + exitStatus[10051]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)\n");
            Preferences.debug("Number = " + exitStatus[10051] + "\n");
        }
        
        if (exitStatus[10052] > 0) {
        	normalTerminations += exitStatus[10052];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
            System.out.println("because we are computing at noise level");
            System.out.println("The last steps were computed with prank <> n at the termination point");
            System.out.println("Number = " + exitStatus[10052]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
            Preferences.debug("because we are computing at noise level\n");	
            Preferences.debug("The last steps were computed with prank <> n at the termination point\n");
            Preferences.debug("Number = " + exitStatus[10052] + "\n");
        }
        
        if (exitStatus[10053] > 0) {
        	normalTerminations += exitStatus[10053];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
            System.out.println("because we are computing at noise level");
            System.out.println("The method of Newton was used (at least) in the last step");
            System.out.println("Number = " + exitStatus[10053]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
            Preferences.debug("because we are computing at noise level\n");	
            Preferences.debug("The method of Newton was used (at least) in the last step\n");
            Preferences.debug("Number = " + exitStatus[10053] + "\n");
        }
        
        if (exitStatus[10054] > 0) {
        	normalTerminations += exitStatus[10054];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
            System.out.println("because we are computing at noise level");
            System.out.println("The 2nd but last step was subspace minimization but");
            System.out.println("the last two were Gauss-Newton steps");
            System.out.println("Number = " + exitStatus[10054]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
        	Preferences.debug("because we are computing at noise level\n");
        	Preferences.debug("The 2nd but last step was subspace minimization but\n");
            Preferences.debug("the last two were Gauss-Newton steps \n");
            Preferences.debug("Number = " + exitStatus[10054] + "\n");
        }
        
        if (exitStatus[10055] > 0) {
        	normalTerminations += exitStatus[10055];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and");
            System.out.println("because we are computing at noise level");
            System.out.println("The steplength was not unit in both the last two steps\n");	
            System.out.println("Number = " + exitStatus[10055] + "\n");
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2 and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The steplength was not unit in both the last two steps\n");	
            Preferences.debug("Number = " + exitStatus[10055] + "\n");
        }
        
        if (exitStatus[10011] > 0) {
        	normalTerminations += exitStatus[10011];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2");
            System.out.println("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)");
            System.out.println("Number = " + exitStatus[10011]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2\n");
            Preferences.debug("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)\n");
            Preferences.debug("Number = " + exitStatus[10011] + "\n");
        }
        
        if (exitStatus[10012] > 0) {
        	normalTerminations += exitStatus[10012];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2");
            System.out.println("The last steps were computed with prank <> n at the termination point");
            System.out.println("Number = " + exitStatus[10012]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2\n");
            Preferences.debug("The last steps were computed with prank <> n at the termination point\n");
            Preferences.debug("Number = " + exitStatus[10012] + "\n");
        }
        
        if (exitStatus[10013] > 0) {
        	normalTerminations += exitStatus[10013];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2");
            System.out.println("The method of Newton was used (at least) in the last step");
            System.out.println("Number = " + exitStatus[10013]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2\n");
            Preferences.debug("The method of Newton was used (at least) in the last step\n");
            Preferences.debug("Number = " + exitStatus[10013] + "\n");
        }
        
        if (exitStatus[10014] > 0) {
        	normalTerminations += exitStatus[10014];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2");
            System.out.println("The 2nd but last step was subspace minimization but");
            System.out.println("the last two were Gauss-Newton steps");
            System.out.println("Number = " + exitStatus[10014]);
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2\n");
        	Preferences.debug("The 2nd but last step was subspace minimization but\n");
            Preferences.debug("the last two were Gauss-Newton steps \n");
            Preferences.debug("Number = " + exitStatus[10014] + "\n");
        }
        
        if (exitStatus[10015] > 0) {
        	normalTerminations += exitStatus[10015];
        	System.out.println("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2");
            System.out.println("The steplength was not unit in both the last two steps\n");	
            System.out.println("Number = " + exitStatus[10015] + "\n");
        	Preferences.debug("Normal terminations because the relative predicted reduction in the objective function is less than epsrel**2\n");
            Preferences.debug("The steplength was not unit in both the last two steps\n");	
            Preferences.debug("Number = " + exitStatus[10015] + "\n");
        }
        
        if (exitStatus[2351] > 0) {
        	normalTerminations += exitStatus[2351];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)");
            System.out.println("Number = " + exitStatus[2351]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)\n");
            Preferences.debug("Number = " + exitStatus[2351] + "\n");
        }
        
        if (exitStatus[2352] > 0) {
        	normalTerminations += exitStatus[2352];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The last steps were computed with prank <> n at the termination point");
            System.out.println("Number = " + exitStatus[2352]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");	
            Preferences.debug("The last steps were computed with prank <> n at the termination point\n");
            Preferences.debug("Number = " + exitStatus[2352] + "\n");
        }
        
        if (exitStatus[2353] > 0) {
        	normalTerminations += exitStatus[2353];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The method of Newton was used (at least) in the last step");
            System.out.println("Number = " + exitStatus[2353]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");	
            Preferences.debug("The method of Newton was used (at least) in the last step\n");
            Preferences.debug("Number = " + exitStatus[2353] + "\n");
        }
        
        if (exitStatus[2354] > 0) {
        	normalTerminations += exitStatus[2354];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The 2nd but last step was subspace minimization but");
            System.out.println("the last two were Gauss-Newton steps");
            System.out.println("Number = " + exitStatus[2354]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx and\n");
        	Preferences.debug("because we are computing at noise level\n");
        	Preferences.debug("The 2nd but last step was subspace minimization but\n");
            Preferences.debug("the last two were Gauss-Newton steps \n");
            Preferences.debug("Number = " + exitStatus[2354] + "\n");
        }
        
        if (exitStatus[2355] > 0) {
        	normalTerminations += exitStatus[2355];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The steplength was not unit in both the last two steps\n");	
            System.out.println("Number = " + exitStatus[2355] + "\n");
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The steplength was not unit in both the last two steps\n");	
            Preferences.debug("Number = " + exitStatus[2355] + "\n");
        }
        
        if (exitStatus[2311] > 0) {
        	normalTerminations += exitStatus[2311];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx");
            System.out.println("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)");
            System.out.println("Number = " + exitStatus[2311]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx\n");
            Preferences.debug("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)\n");
            Preferences.debug("Number = " + exitStatus[2311] + "\n");
        }
        
        if (exitStatus[2312] > 0) {
        	normalTerminations += exitStatus[2312];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx");
            System.out.println("The last steps were computed with prank <> n at the termination point");
            System.out.println("Number = " + exitStatus[2312]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx\n");
            Preferences.debug("The last steps were computed with prank <> n at the termination point\n");
            Preferences.debug("Number = " + exitStatus[2312] + "\n");
        }
        
        if (exitStatus[2313] > 0) {
        	normalTerminations += exitStatus[2313];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx");
            System.out.println("The method of Newton was used (at least) in the last step");
            System.out.println("Number = " + exitStatus[2313]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx\n");
            Preferences.debug("The method of Newton was used (at least) in the last step\n");
            Preferences.debug("Number = " + exitStatus[2313] + "\n");
        }
        
        if (exitStatus[2314] > 0) {
        	normalTerminations += exitStatus[2314];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx");
            System.out.println("The 2nd but last step was subspace minimization but");
            System.out.println("the last two were Gauss-Newton steps");
            System.out.println("Number = " + exitStatus[2314]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx\n");
        	Preferences.debug("The 2nd but last step was subspace minimization but\n");
            Preferences.debug("the last two were Gauss-Newton steps \n");
            Preferences.debug("Number = " + exitStatus[2314] + "\n");
        }
        
        if (exitStatus[2315] > 0) {
        	normalTerminations += exitStatus[2315];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2 and");
        	System.out.println("because the relative change in x is less than epsx");
            System.out.println("The steplength was not unit in both the last two steps\n");	
            System.out.println("Number = " + exitStatus[2315] + "\n");
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because the relative change in x is less than epsx\n");
            Preferences.debug("The steplength was not unit in both the last two steps\n");	
            Preferences.debug("Number = " + exitStatus[2315] + "\n");
        }
        
        if (exitStatus[2051] > 0) {
        	normalTerminations += exitStatus[2051];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2 and");
            System.out.println("because we are computing at noise level");
            System.out.println("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)");
            System.out.println("Number = " + exitStatus[2051]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2 and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)\n");
            Preferences.debug("Number = " + exitStatus[2051] + "\n");
        }
        
        if (exitStatus[2052] > 0) {
        	normalTerminations += exitStatus[2052];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2 and");
            System.out.println("because we are computing at noise level");
            System.out.println("The last steps were computed with prank <> n at the termination point");
            System.out.println("Number = " + exitStatus[2052]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2 and\n");
            Preferences.debug("because we are computing at noise level\n");	
            Preferences.debug("The last steps were computed with prank <> n at the termination point\n");
            Preferences.debug("Number = " + exitStatus[2052] + "\n");
        }
        
        if (exitStatus[2053] > 0) {
        	normalTerminations += exitStatus[2053];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2 and");
            System.out.println("because we are computing at noise level");
            System.out.println("The method of Newton was used (at least) in the last step");
            System.out.println("Number = " + exitStatus[2053]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2 and\n");
            Preferences.debug("because we are computing at noise level\n");	
            Preferences.debug("The method of Newton was used (at least) in the last step\n");
            Preferences.debug("Number = " + exitStatus[2053] + "\n");
        }
        
        if (exitStatus[2054] > 0) {
        	normalTerminations += exitStatus[2054];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2 and");
            System.out.println("because we are computing at noise level");
            System.out.println("The 2nd but last step was subspace minimization but");
            System.out.println("the last two were Gauss-Newton steps");
            System.out.println("Number = " + exitStatus[2054]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2 and\n");
        	Preferences.debug("because we are computing at noise level\n");
        	Preferences.debug("The 2nd but last step was subspace minimization but\n");
            Preferences.debug("the last two were Gauss-Newton steps \n");
            Preferences.debug("Number = " + exitStatus[2054] + "\n");
        }
        
        if (exitStatus[2055] > 0) {
        	normalTerminations += exitStatus[2055];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2 and");
            System.out.println("because we are computing at noise level");
            System.out.println("The steplength was not unit in both the last two steps\n");	
            System.out.println("Number = " + exitStatus[2055] + "\n");
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2 and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The steplength was not unit in both the last two steps\n");	
            Preferences.debug("Number = " + exitStatus[2055] + "\n");
        }
        
        if (exitStatus[2011] > 0) {
        	normalTerminations += exitStatus[2011];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2");
            System.out.println("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)");
            System.out.println("Number = " + exitStatus[2011]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2\n");
            Preferences.debug("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)\n");
            Preferences.debug("Number = " + exitStatus[2011] + "\n");
        }
        
        if (exitStatus[2012] > 0) {
        	normalTerminations += exitStatus[2012];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2");
            System.out.println("The last steps were computed with prank <> n at the termination point");
            System.out.println("Number = " + exitStatus[2012]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2\n");
            Preferences.debug("The last steps were computed with prank <> n at the termination point\n");
            Preferences.debug("Number = " + exitStatus[2012] + "\n");
        }
        
        if (exitStatus[2013] > 0) {
        	normalTerminations += exitStatus[2013];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2");
            System.out.println("The method of Newton was used (at least) in the last step");
            System.out.println("Number = " + exitStatus[2013]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2\n");
            Preferences.debug("The method of Newton was used (at least) in the last step\n");
            Preferences.debug("Number = " + exitStatus[2013] + "\n");
        }
        
        if (exitStatus[2014] > 0) {
        	normalTerminations += exitStatus[2014];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2");
            System.out.println("The 2nd but last step was subspace minimization but");
            System.out.println("the last two were Gauss-Newton steps");
            System.out.println("Number = " + exitStatus[2014]);
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2\n");
        	Preferences.debug("The 2nd but last step was subspace minimization but\n");
            Preferences.debug("the last two were Gauss-Newton steps \n");
            Preferences.debug("Number = " + exitStatus[2014] + "\n");
        }
        
        if (exitStatus[2015] > 0) {
        	normalTerminations += exitStatus[2015];
        	System.out.println("Normal terminations because the sum of squares is less than epsabs**2");
            System.out.println("The steplength was not unit in both the last two steps\n");	
            System.out.println("Number = " + exitStatus[2015] + "\n");
        	Preferences.debug("Normal terminations because the sum of squares is less than epsabs**2\n");
            Preferences.debug("The steplength was not unit in both the last two steps\n");	
            Preferences.debug("Number = " + exitStatus[2015] + "\n");
        }
        
        if (exitStatus[351] > 0) {
        	normalTerminations += exitStatus[351];
        	System.out.println("Normal terminations because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)");
            System.out.println("Number = " + exitStatus[351]);
        	Preferences.debug("Normal terminations because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)\n");
            Preferences.debug("Number = " + exitStatus[351] + "\n");
        }
        
        if (exitStatus[352] > 0) {
        	normalTerminations += exitStatus[352];
        	System.out.println("Normal terminations because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The last steps were computed with prank <> n at the termination point");
            System.out.println("Number = " + exitStatus[352]);
        	Preferences.debug("Normal terminations because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");	
            Preferences.debug("The last steps were computed with prank <> n at the termination point\n");
            Preferences.debug("Number = " + exitStatus[352] + "\n");
        }
        
        if (exitStatus[353] > 0) {
        	normalTerminations += exitStatus[353];
        	System.out.println("Normal terminations because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The method of Newton was used (at least) in the last step");
            System.out.println("Number = " + exitStatus[353]);
        	Preferences.debug("Normal terminations because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");	
            Preferences.debug("The method of Newton was used (at least) in the last step\n");
            Preferences.debug("Number = " + exitStatus[353] + "\n");
        }
        
        if (exitStatus[354] > 0) {
        	normalTerminations += exitStatus[354];
        	System.out.println("Normal terminations because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The 2nd but last step was subspace minimization but");
            System.out.println("the last two were Gauss-Newton steps");
            System.out.println("Number = " + exitStatus[354]);
        	Preferences.debug("Normal terminations because the relative change in x is less than epsx and\n");
        	Preferences.debug("because we are computing at noise level\n");
        	Preferences.debug("The 2nd but last step was subspace minimization but\n");
            Preferences.debug("the last two were Gauss-Newton steps \n");
            Preferences.debug("Number = " + exitStatus[354] + "\n");
        }
        
        if (exitStatus[355] > 0) {
        	normalTerminations += exitStatus[355];
        	System.out.println("Normal terminations because the relative change in x is less than epsx and");
            System.out.println("because we are computing at noise level");
            System.out.println("The steplength was not unit in both the last two steps\n");	
            System.out.println("Number = " + exitStatus[355] + "\n");
        	Preferences.debug("Normal terminations because the relative change in x is less than epsx and\n");
            Preferences.debug("because we are computing at noise level\n");
            Preferences.debug("The steplength was not unit in both the last two steps\n");	
            Preferences.debug("Number = " + exitStatus[355] + "\n");
        }
        
        if (exitStatus[311] > 0) {
        	normalTerminations += exitStatus[311];
        	System.out.println("Normal terminations because the relative change in x is less than epsx");
            System.out.println("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)");
            System.out.println("Number = " + exitStatus[311]);
        	Preferences.debug("Normal terminations because the relative change in x is less than epsx\n");
            Preferences.debug("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)\n");
            Preferences.debug("Number = " + exitStatus[311] + "\n");
        }
        
        if (exitStatus[312] > 0) {
        	normalTerminations += exitStatus[312];
        	System.out.println("Normal terminations because the relative change in x is less than epsx");
            System.out.println("The last steps were computed with prank <> n at the termination point");
            System.out.println("Number = " + exitStatus[312]);
        	Preferences.debug("Normal terminations because the relative change in x is less than epsx\n");	
            Preferences.debug("The last steps were computed with prank <> n at the termination point\n");
            Preferences.debug("Number = " + exitStatus[312] + "\n");
        }
        
        if (exitStatus[313] > 0) {
        	normalTerminations += exitStatus[313];
        	System.out.println("Normal terminations because the relative change in x is less than epsx");
            System.out.println("The method of Newton was used (at least) in the last step");
            System.out.println("Number = " + exitStatus[313]);
        	Preferences.debug("Normal terminations because the relative change in x is less than epsx\n");
            Preferences.debug("The method of Newton was used (at least) in the last step\n");
            Preferences.debug("Number = " + exitStatus[313] + "\n");
        }
        
        if (exitStatus[314] > 0) {
        	normalTerminations += exitStatus[314];
        	System.out.println("Normal terminations because the relative change in x is less than epsx");
            System.out.println("The 2nd but last step was subspace minimization but");
            System.out.println("the last two were Gauss-Newton steps");
            System.out.println("Number = " + exitStatus[314]);
        	Preferences.debug("Normal terminations because the relative change in x is less than epsx\n");
        	Preferences.debug("The 2nd but last step was subspace minimization but\n");
            Preferences.debug("the last two were Gauss-Newton steps \n");
            Preferences.debug("Number = " + exitStatus[314] + "\n");
        }
        
        if (exitStatus[315] > 0) {
        	normalTerminations += exitStatus[315];
        	System.out.println("Normal terminations because the relative change in x is less than epsx");
            System.out.println("The steplength was not unit in both the last two steps\n");	
            System.out.println("Number = " + exitStatus[315] + "\n");
        	Preferences.debug("Normal terminations because the relative change in x is less than epsx\n");
            Preferences.debug("The steplength was not unit in both the last two steps\n");	
            Preferences.debug("Number = " + exitStatus[315] + "\n");
        }
        
        if (exitStatus[51] > 0) {
        	normalTerminations += exitStatus[51];
            System.out.println("Normal terminations because we are computing at noise level");
            System.out.println("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)");
            System.out.println("Number = " + exitStatus[51]);
            Preferences.debug("Normal terminations because we are computing at noise level\n");
            Preferences.debug("The last steps were computed with no trouble (Gauss-Newton the last 3 steps)\n");
            Preferences.debug("Number = " + exitStatus[51] + "\n");
        }
        
        if (exitStatus[52] > 0) {
        	normalTerminations += exitStatus[52];
            System.out.println("Normal terminations because we are computing at noise level");
            System.out.println("The last steps were computed with prank <> n at the termination point");
            System.out.println("Number = " + exitStatus[52]);
            Preferences.debug("Normal terminations because we are computing at noise level\n");	
            Preferences.debug("The last steps were computed with prank <> n at the termination point\n");
            Preferences.debug("Number = " + exitStatus[52] + "\n");
        }
        
        if (exitStatus[53] > 0) {
        	normalTerminations += exitStatus[53];
            System.out.println("Normal terminations because we are computing at noise level");
            System.out.println("The method of Newton was used (at least) in the last step");
            System.out.println("Number = " + exitStatus[53]);
            Preferences.debug("Normal terminations because we are computing at noise level\n");	
            Preferences.debug("The method of Newton was used (at least) in the last step\n");
            Preferences.debug("Number = " + exitStatus[53] + "\n");
        }
        
        if (exitStatus[54] > 0) {
        	normalTerminations += exitStatus[54];
            System.out.println("Normal terminations because we are computing at noise level");
            System.out.println("The 2nd but last step was subspace minimization but");
            System.out.println("the last two were Gauss-Newton steps");
            System.out.println("Number = " + exitStatus[54]);
        	Preferences.debug("Normal terminations because we are computing at noise level\n");
        	Preferences.debug("The 2nd but last step was subspace minimization but\n");
            Preferences.debug("the last two were Gauss-Newton steps \n");
            Preferences.debug("Number = " + exitStatus[54] + "\n");
        }
        
        if (exitStatus[55] > 0) {
        	normalTerminations += exitStatus[55];
            System.out.println("Normal terminations because we are computing at noise level");
            System.out.println("The steplength was not unit in both the last two steps\n");	
            System.out.println("Number = " + exitStatus[55] + "\n");
            Preferences.debug("Normal terminations because we are computing at noise level\n");
            Preferences.debug("The steplength was not unit in both the last two steps\n");	
            Preferences.debug("Number = " + exitStatus[55] + "\n");
        }
        
        if (exitStatus[10] > 0) {
        	abnormalTerminations += exitStatus[10];
        	System.out.println("Abnormal terminations because m < n or n <= 0 or m <= 0 or mdc < m or mdw < n*n + 5*n + 3*m + 6 or");
            System.out.println("maxit <= 0 or epsrel < 0 or epsabs < 0 or epsx < 0 or invalid starting point on entry = " + exitStatus[10]);
            Preferences.debug("Abnormal terminations because m < n or n <= 0 or m <= 0 or mdc < m or mdw < n*n + 5*n + 3*m + 6 or\n");
            Preferences.debug("maxit <= 0 or epsrel < 0 or epsabs < 0 or epsx < 0 or invalid starting point on entry = " + exitStatus[10] + "\n");
        }
        
        if (exitStatus[9] > 0) {
        	abnormalTerminations += exitStatus[9];
        	System.out.println("Abnormal terminations because the number of iterations has exceeded the maximum allowed iterations = "
        			           + exitStatus[9]);
        	Preferences.debug("Abnormal terminations because the number of iterations has exceeded the maximum allowed iterations = "
        			           + exitStatus[9] + "\n");
        }
        
        if (exitStatus[8] > 0) {
        	abnormalTerminations += exitStatus[8];
        	System.out.println("Abnormal terminations because the Hessian emanating from the 2nd order method is not positive definite = "
        			            + exitStatus[8]);
        	Preferences.debug("Abnormal terminations because the Hessian emanating from the 2nd order method is not positive definite = "
        			            + exitStatus[8] + "\n");
        }
        
        if (exitStatus[7] > 0) {
        	abnormalTerminations += exitStatus[7];
        	System.out.println("Abnormal terminations because the algorithm would like to use 2nd derivatives but is not allowed to do that = "
        			           + exitStatus[7]);
        	Preferences.debug("Abnormal terminations because the algorithm would like to use 2nd derivatives but is not allowed to do that = "
        			           + exitStatus[7] + "\n");
        }
        
        if (exitStatus[6] > 0) {
        	abnormalTerminations += exitStatus[6];
        	System.out.println("Abnormal terminations because an undamped step with Newtons method is a failure = " + exitStatus[6]);
        	Preferences.debug("Abnormal terminations because an undamped step with Newtons method is a failure = " + exitStatus[6] + "\n");
        }
        
        if (exitStatus[5] > 0) {
        	abnormalTerminations += exitStatus[5];
        	System.out.println("Abnormal terminations because the latest search direction computed using subspace minimization");
        	System.out.println("was not a descent direction (probably caused by a wrongly computed Jacobian) = " + exitStatus[5]);
        	Preferences.debug("Abnormal terminations because the latest search direction computed using subspace minimization\n");
        	Preferences.debug("was not a descent direction (probably caused by a wrongly computed Jacobian) = " + exitStatus[5] + "\n");
        }
        
        if (exitStatus[4] > 0) {
        	abnormalTerminations += exitStatus[4];
        	System.out.println("Abnormal terminations because there is only one feasible point,");
        	System.out.println("namely X(I) = BL(I) = BU(I), I = 1,2,...,N = " + exitStatus[4]);
        	Preferences.debug("Abnormal terminations because there is only one feasible point,\n");
        	Preferences.debug("namely X(I) = BL(I) = BU(I), I = 1,2,...,N = " + exitStatus[4] + "\n");
        }
        
        System.out.println("\nTotal normal terminations = " + normalTerminations);
        Preferences.debug("\nTotal normal terminations = " + normalTerminations + "\n");
        System.out.println("Total abnormal terminations = " + abnormalTerminations);
        Preferences.debug("Total abnormal terminations = " + abnormalTerminations + "\n");
       
        
        try {
        	destImage.importData(0, destArray, true);
        }
        catch (IOException e) {
        	MipavUtil.displayError("IOException on destImage.importData(0, destArray, true)");
        	setCompleted(false);
        	return;
        }
        
        setCompleted(true);
    }
    
    class FitSM2ConstrainedModel extends NLConstrainedEngine {

        /**
         * Creates a new FitDEMRI3ConstrainedModel object.
         *
         * @param  nPoints  DOCUMENT ME!
         * @param  xData    DOCUMENT ME!
         * @param  yData    DOCUMENT ME!
         * @param  initial  DOCUMENT ME!
         */
        public FitSM2ConstrainedModel(int nPoints, double[] xData, double[] yData,
                                                           double[] initial) {

            // nPoints data points, 3 coefficients, and exponential fitting
            super(nPoints, 3, xData, yData);

            int i;

            bounds = 2; // bounds = 0 means unconstrained

            // bounds = 1 means same lower and upper bounds for
            // all parameters
            // bounds = 2 means different lower and upper bounds
            // for all parameters
            // Constrain parameter 0
            bl[0] = min_constr[0];
            bu[0] = max_constr[0];

            // Constrain parameter 1
            bl[1] = min_constr[1];
            bu[1] = max_constr[1];

            // Constrain parameter 2
            bl[2] = min_constr[2];
            bu[2] = max_constr[2];

            // The default is internalScaling = false
            // To make internalScaling = true and have the columns of the
            // Jacobian scaled to have unit length include the following line.
            //internalScaling = true;
            // Suppress diagnostic messages
            outputMes = false;

            gues[0] = initial[0];
            gues[1] = initial[1];
            gues[2] = initial[2];
        }

        /**
         * Starts the analysis.
         */
        public void driver() {
            super.driver();
        }

        /**
         * Display results of displaying SM2 fitting parameters.
         */
        public void dumpResults() {
            Preferences.debug(" ******* FitSM2ConstrainedModel ********* \n\n");
            Preferences.debug("Number of iterations: " + String.valueOf(iters) + "\n");
            Preferences.debug("Chi-squared: " + String.valueOf(getChiSquared()) + "\n");
            Preferences.debug("a0 " + String.valueOf(a[0]) + "\n");
            Preferences.debug("a1 " + String.valueOf(a[1]) + "\n");
            Preferences.debug("a2 " + String.valueOf(a[2]) + "\n");
        }

        /**
         * 
         *
         * @param  a          The best guess parameter values.
         * @param  residuals  ymodel - yData.
         * @param  covarMat   The derivative values of y with respect to fitting parameters.
         */
        public void fitToFunction(double[] a, double[] residuals, double[][] covarMat) {
            int ctrl;
            int j;
            double ktrans;
            double ve;
            double vp;
            int m;
            double intSum;
            double intSumDerivKtrans;
            double intSumDerivVe;

            try {
                ctrl = ctrlMat[0];
                //Preferences.debug("ctrl = " + ctrl + " a[0] = " + a[0] + " a[1] = " + a[1] + " a[2] = " + a[2] + "\n");
                if ((ctrl == -1) || (ctrl == 1)) {
                	ktrans = a[0];
                	ve = a[1];
                	vp = a[2];
                	ktransDivVe = ktrans/ve;
                	for (j = 0; j <= tDim-1; j++) {
                		for (m = 0; m <= tDim-1; m++) {
                	        exparray[j][m] = Math.exp((timeVals[j] - timeVals[m])*ktransDivVe);
                		}
                	}
                	
                	for (m = 2; m <= tDim; m++) {
                		intSum = 0.0;
                		for (j = 2; j <= m; j++) {
                			intSum += trapezoidConstant[j-2]*(exparray[j-1][m-1] - exparray[j-2][m-1]);
	                        intSum += trapezoidSlope[j-2]* ((exparray[j-1][m-1]*(timeVals[j-1] - 1.0/ktransDivVe)) -
	                                                                (exparray[j-2][m-1]*(timeVals[j-2] - 1.0/ktransDivVe)));
                		} // for (j = 2; j <= m; j++)
                		ymodel[m-2] = intSum + vp * r1ptj[m-1];
                	} // for (m = 2; m <= tDim; m++)
                    // evaluate the residuals[j] = ymodel[j] - ySeries[j]
                    for (j = 0; j < nPts; j++) {
                        residuals[j] = ymodel[j] - ySeries[j];
                        //Preferences.debug("residuals["+ j + "] = " + residuals[j] + "\n");
                    }
                } // if ((ctrl == -1) || (ctrl == 1))
                else if (ctrl == 2) {
                	// Calculate the Jacobian analytically
                	ktrans = a[0];
                	ve = a[1];
                	vp = a[2];
                	ktransDivVe = ktrans/ve;
                	for (j = 0; j <= tDim-1; j++) {
                		for (m = 0; m <= tDim-1; m++) {
                	        exparray[j][m] = Math.exp((timeVals[j] - timeVals[m])*ktransDivVe);
                		}
                	}
                	for (m = 2; m <= tDim; m++) {
                		intSumDerivKtrans = 0.0;
                		intSumDerivVe = 0.0;
                		for (j = 2; j <= m; j++) {
	                        intSumDerivKtrans += trapezoidConstant[j-2]*((timeVals[j-1]-timeVals[m-1])*exparray[j-1][m-1] 
	                                                                   - (timeVals[j-2] - timeVals[m-1])*exparray[j-2][m-1])/ve;
	                        intSumDerivKtrans += trapezoidSlope[j-2]*((exparray[j-1][m-1]*(timeVals[j-1]-timeVals[m-1])*(timeVals[j-1] - 1.0/ktransDivVe)) -
	                        		                                  (exparray[j-2][m-1]*(timeVals[j-2]-timeVals[m-1])*(timeVals[j-2] - 1.0/ktransDivVe)))/ve;
	                        intSumDerivKtrans += trapezoidSlope[j-2]*(exparray[j-1][m-1] - exparray[j-2][m-1])*ve/(ktrans*ktrans);
	                        intSumDerivVe += trapezoidConstant[j-2]*((timeVals[j-1]-timeVals[m-1])*exparray[j-1][m-1] 
	                                                              - (timeVals[j-2]-timeVals[m-1])*exparray[j-2][m-1])*(-ktrans/(ve*ve));
	                        intSumDerivVe += trapezoidSlope[j-2]*((exparray[j-1][m-1]*(timeVals[j-1]-timeVals[m-1])*(timeVals[j-1] - 1.0/ktransDivVe)) -
	                                  (exparray[j-2][m-1]*(timeVals[j-2]-timeVals[m-1])*(timeVals[j-2] - 1.0/ktransDivVe)))*(-ktrans/(ve*ve));
	                        intSumDerivVe += trapezoidSlope[j-2]*(exparray[j-1][m-1] - exparray[j-2][m-1])*(-1.0/ktrans);
                		} // for (j = 2; j <= m; j++)
                		covarMat[m-2][0] = intSumDerivKtrans;
                		covarMat[m-2][1] = intSumDerivVe;
                		covarMat[m-2][2] = r1ptj[m-1];
                		//Preferences.debug("covarMat[" + (m-2) + "][0] = " + covarMat[m-2][0] + "\n");
                		//Preferences.debug("covarMat[" + (m-2) + "][1] = " + covarMat[m-2][1] + "\n");
                		//Preferences.debug("covarMat[" + (m-2) + "][2] = " + covarMat[m-2][2] + "\n");
                	}
                }
                // Calculate the Jacobian numerically
                //else if (ctrl == 2) {
                    //ctrlMat[0] = 0;
                //}
            } catch (Exception exc) {
                Preferences.debug("function error: " + exc.getMessage() + "\n");
            }

            return;
        }
    }
    
    class FitAll extends NLConstrainedEngine {

        /**
         * Creates a new Fit24DModel object.
         *
         * @param  nPoints  DOCUMENT ME!
         * @param  xData    DOCUMENT ME!
         * @param  yData    DOCUMENT ME!
         * @param  initial  DOCUMENT ME!
         */
        public FitAll() {

            // nPoints data points, 3 coefficients, and exponential fitting
            super();

            
        }

        /**
         * Starts the analysis.
         */
        public void driver() {
            super.driver();
        }

        /**
         * Display results of displaying exponential fitting parameters.
         */
        public void dumpResults() {
            
        }

        /**
         * Fit to function - a0 - a1*(a2**x).
         *
         * @param  a          The x value of the data point.
         * @param  residuals  The best guess parameter values.
         * @param  covarMat   The derivative values of y with respect to fitting parameters.
         */
        public void fitToFunction(double[] a, double[] residuals, double[][] covarMat) {
            

            return;
        }
    }
    
   
}
