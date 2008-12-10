package gov.nih.mipav.model.algorithms;


import java.util.Arrays;

import Jama.Matrix;
import gov.nih.mipav.view.*;


/**
 * FitLorentz -fits an array of points to a lorentz distribution, thresholding techniques come from ViewJFrameGraph,
 * no need to implement here
 *
 * @author   senseneyj
 * @see      NLEngine
 * @version  0.1
 */
public class FitLorentz extends NLFittedFunction {
	
	//~ Instance fields ------------------------------------------------------------------------------------------------
    
    /**Location in xDataOrg where Gaussian data starts */
    private int dataStart;
    
    /**Location in xDataOrg where Gaussian data ends */
    private int dataEnd;
    
    /**Amplitude parameter*/
    private double amp;
    
    /**Center parameter*/
    private double xInit;
    
    /**Gamma parameter*/
    private double gamma;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * FitLorentz - test constructor, builds and test function.
     */
    public FitLorentz() {

        // nPoints data points, 3 coefficients, and exponential fitting
        super(5, 3);
        testData();
    }

    /**
     * FitLorentz.
     *
     * @param  nPoints  number of points in the function
     * @param  xData    DOCUMENT ME!
     * @param  yData    DOCUMENT ME!
     */
    public FitLorentz(int nPoints, double[] xData, double[] yData) {

        // nPoints data points, 3 coefficients, and exponential fitting
        super(nPoints, 3);

        this.xDataOrg = xData;
        this.yDataOrg = yData;
        
        //yDataOrg = applyKernel();
        
        estimateInitial();
        
    }

    /**
     * Constructs new fit lorentz distribution.
     *
     * @param  nPoints  Number of points in the function
     * @param  xData    DOCUMENT ME!
     * @param  yData    DOCUMENT ME!
     */
    public FitLorentz(int nPoints, float[] xData, float[] yData) {
    	
        // nPoints data points, 3 coefficients, and exponential fitting
        super(nPoints, 3);

        this.xDataOrg = new double[nPoints];
        this.yDataOrg = new double[nPoints];
        for (int i = 0; i < nPoints; i++) {
            xDataOrg[i] = xData[i];
            yDataOrg[i] = yData[i];
        }
        
        //yDataOrg = applyKernel();
        
        estimateInitial();

    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Apply small Gaussian kernel to smooth out data.  Note should not be called if doing data comparison.
     */
    private double[] applyKernel() {
    	
        int size = 7;
        int start = size / 2;
        double[] sortY = new double[size];
        double[] newY = new double[yDataOrg.length];
        double[] gaussY = new double[yDataOrg.length];
        int end = yDataOrg.length - start;

        newY[0] = yDataOrg[0];
        newY[1] = getMedian(new double[] { yDataOrg[0], yDataOrg[1], yDataOrg[2] });
        newY[2] = getMedian(new double[] { yDataOrg[0], yDataOrg[1], yDataOrg[2], yDataOrg[3], yDataOrg[4] });

        newY[end] = getMedian(new double[] { yDataOrg[end - 2], yDataOrg[end - 1], yDataOrg[end], yDataOrg[end + 1], yDataOrg[end + 2] });
        newY[end + 1] = getMedian(new double[] { yDataOrg[end], yDataOrg[end + 1], yDataOrg[end + 2] });
        newY[end + 2] = yDataOrg[end + 2];

        for (int i = start; i < end; i++) {
            for (int j = 0; j < size; j++) {
                sortY[j] = yDataOrg[i + j - start];
            }
            newY[i] = getMedian(sortY);
        }

        GaussianOneDimKernel g = new  GaussianOneDimKernel();
        float[] kernel = g.make(1.0f);
        gaussY[0] = newY[0];
        gaussY[1] = newY[1];
        gaussY[2] = newY[2];

        // System.err.println("gaussY length: " + gaussY.length + " end: " + end);
        end = gaussY.length - 3;

        gaussY[end] = newY[end];
        gaussY[end + 1] = newY[end + 1];
        gaussY[end + 2] = newY[end + 2];

        for (int i = 3; i < end; i++) {
            gaussY[i] = (newY[i - 3] * kernel[0]) + (newY[i - 2] * kernel[1]) + (newY[i - 1] * kernel[2]) +
                        (newY[i] * kernel[3]) + (newY[i + 1] * kernel[4]) + (newY[i + 2] * kernel[5]) +
                        (newY[i + 3] * kernel[6]);
        }
        
        return gaussY;
    }
    
    /**
     * get median of given array
     */
    private double getMedian(double[] toSort) {
        int length = toSort.length;

        Arrays.sort(toSort);

        return toSort[(length / 2)];
    }
    
    private void estimateInitial() {
    	int offset = 15;
    	
    	//determine location of start data, note 
    	//basic thresholding will already have been performed
    	dataStart = 0;
    	for(int i=0; i<yDataOrg.length; i++) {
    		if(yDataOrg[i] != 0 && i > 0) {
    			dataStart = i > offset ? i-offset : 0;
    			break;
    		}		
    	}
    	
    	//estimate xInit
    	int maxIndex = 0;
    	double totalDataCount = 0;
    	int numIndexWithData = 0;
    	for(int i=dataStart; i<yDataOrg.length; i++) {
    		if(yDataOrg[i] > yDataOrg[maxIndex]) {
    			maxIndex = i;
    		}
    		if(yDataOrg[i] > 0) {
    			numIndexWithData++;
    			totalDataCount += yDataOrg[i];
    		}
    	}	
    	xInit = xDataOrg[maxIndex];
    	
    	//determine location of end data
    	dataEnd = 0;
    	for(int i=maxIndex; i<yDataOrg.length; i++) {
    		if(yDataOrg[i] == 0) {
    			dataEnd = i+offset < yDataOrg.length-1 ? i+offset : yDataOrg.length-1;
    			break;
    		}
    	}

    	//find location of one sigma data collection point
    	double dataCollectedOneSigma = yDataOrg[maxIndex], dataCollectedTwoSigma = yDataOrg[maxIndex];
    	int xStopLeftIndex = maxIndex, xStopRightIndex = maxIndex;
    	boolean left = true;
    	while(dataCollectedOneSigma / totalDataCount < .68 && 
    			xStopLeftIndex > dataStart+1 && xStopRightIndex < dataEnd-1) {
    		if(left) 
    			dataCollectedOneSigma += yDataOrg[--xStopLeftIndex];
    		if(!left)
    			dataCollectedOneSigma += yDataOrg[++xStopRightIndex];
    		left = !left;
    	}
    	
    	//estimate one sigma from stopping locations
    	double oneSigmaEstimate = 0;
    	if(dataCollectedOneSigma / totalDataCount >= .68) {
    		double sigmaLeft = Math.abs(xDataOrg[maxIndex] - xDataOrg[xStopLeftIndex]);
    		double sigmaRight = Math.abs(xDataOrg[maxIndex] - xDataOrg[xStopLeftIndex]);
    		oneSigmaEstimate = sigmaLeft + sigmaRight / 2.0;
    	}
    	
    	//find location of two sigma data collection point
    	dataCollectedTwoSigma = dataCollectedOneSigma;
    	while(dataCollectedTwoSigma / totalDataCount < .95 && 
    			xStopLeftIndex > dataStart+1 && xStopRightIndex < dataEnd-1) {
    		if(left) 
    			dataCollectedTwoSigma += yDataOrg[--xStopLeftIndex];
    		if(!left)
    			dataCollectedTwoSigma += yDataOrg[++xStopRightIndex];
    		left = !left;
    	}
    	
    	//estimate two sigma from stopping location
    	double twoSigmaEstimate = 0;
    	if(dataCollectedOneSigma / totalDataCount >= .68) {
    		double sigmaLeft = Math.abs(xDataOrg[maxIndex] - xDataOrg[xStopLeftIndex]);
    		double sigmaRight = Math.abs(xDataOrg[maxIndex] - xDataOrg[xStopLeftIndex]);
    		twoSigmaEstimate = sigmaLeft + sigmaRight / 2.0;
    	}
    	
    	//use both measurements to estimate stdev
    	if(twoSigmaEstimate != 0)
    		gamma = (oneSigmaEstimate + .5*twoSigmaEstimate) / 2;
    	else 
    		gamma = oneSigmaEstimate;
    	
    	//estimate for amplitude
    	amp = yDataOrg[maxIndex];
    	
    	a[0] = amp;
    	a[1] = xInit;
    	a[2] = gamma;
    }
    
    /**
     * Starts the analysis. For some reason a guess with the wrong sign for a2 will not converge. Therefore, try both
     * sign and take the one with the lowest chi-squared value.
     */
    public void driver() {
        
    	boolean converged = false;
    	kk = 0;
    	
    	System.out.println("Initial guess\tAmp: "+amp+"\txInit: "+xInit+"\tGamma: "+gamma);
    	
    	while(!converged && kk < MAX_ITR) {
    		double oldAmp = amp;
        	double oldXInit = xInit;
        	double oldSigma = gamma;
    	
	    	Matrix jacobian = generateJacobian();
	    	Matrix residuals = generateResiduals();
	    	
	    	Matrix lhs = jacobian.transpose().times(jacobian);
	    	Matrix rhs = jacobian.transpose().times(residuals);
	    	
	    	Matrix dLambda = lhs.solve(rhs);
	    	
	    	amp = amp + dLambda.get(0, 0);
	    	xInit = xInit + dLambda.get(1, 0);
	    	gamma = gamma + dLambda.get(2, 0);
	    	
	    	System.out.println("Iteration "+kk+"\tAmp: "+amp+"\txInit: "+xInit+"\tGamma: "+gamma);
	    	
	    	if(Math.abs(Math.abs(oldAmp - amp) / ((oldAmp + amp) / 2)) < EPSILON && 
	    			Math.abs(Math.abs(oldXInit - xInit) / ((oldXInit + xInit) / 2)) < EPSILON && 
	    			Math.abs(Math.abs(oldSigma - gamma) / ((oldSigma + gamma) / 2)) < EPSILON && kk > MIN_ITR) {
	    		converged = true;    		
	    		Preferences.debug("Converged after "+kk+" iterations.");
	    		System.out.println("Converged after "+kk+" iterations.");
	    	} else {
	    		oldAmp = amp;
	    		oldXInit = xInit;
	    		oldSigma = gamma;
	    		kk++;
	    	}
    	}
    	
    	if(!converged) {
    		Preferences.debug("Did not converge after "+kk+" iterations.");
    		System.out.println("Did not converge after "+kk+" iterations.");
    	} else {
    		calculateChiSq();
    	}
    	
    	//a already initialized in super constructor, used to hold parameters for output
    	a[0] = amp;
    	a[1] = xInit;
    	a[2] = gamma;
    	
    }
    
    private void calculateChiSq() {
    	Matrix residuals = generateResiduals();
    	double sum = 0;
    	for(int i=dataStart; i<dataEnd; i++) {
    		double resTemp = residuals.get(i-dataStart, 0);
    		residuals.set(i-dataStart, 0, Math.pow(resTemp, 2)/lorentz(xDataOrg[i]));
    		System.out.println("xValue: "+xDataOrg[i]+"\tActual: "+yDataOrg[i]+"\tExpected: "+lorentz(xDataOrg[i])+"\tResidual: "+resTemp+"\tChi squared value: "+residuals.get(i-dataStart, 0));
    		sum += Math.pow(resTemp, 2)/lorentz(xDataOrg[i]);
    	}
    	System.out.println("Sum "+sum+"\tcompared to chisq "+chisq);
    	chisq = residuals.norm1();
    }

    @Override
	protected void calculateFittedY() {
		yDataFitted = new double[xDataOrg.length];
		for(int i=0; i<xDataOrg.length; i++) {
			yDataFitted[i] = lorentz(xDataOrg[i]);
		}
	}

	/**
     * Display results of displaying exponential fitting parameters.
     */
    public void displayResults() {
    	ViewJFrameMessageGraph messageGraph = new ViewJFrameMessageGraph("Fitting Data");
    	
    	messageGraph.append(" ******* FitLorentz ********* \n\n");
    	messageGraph.append("Number of iterations: " + kk + "\n");
        messageGraph.append("Chi-squared: " + chisq + "\n\n");

        messageGraph.append("Valid for data from "+xDataOrg[dataStart]+" to "+xDataOrg[dataEnd]+" in "+(dataEnd-dataStart)+" parts\n\n");
        
        messageGraph.append("Fitting of gaussian function\n");
        messageGraph.append(" y = .5 * " + amp + " * exp(sqrt(x-" + xInit +
                            ")/(" + gamma + "^3))\n");
        messageGraph.append("\n");
        
        messageGraph.append("amp: " + amp + "\n"); 
        messageGraph.append("Xo: " + xInit + "\n");
        messageGraph.append("sigma: " + gamma + "\n\n");
        
        if (messageGraph.isVisible() == false) {
        	messageGraph.setLocation(100, 50);
            messageGraph.setSize(500, 300);
            messageGraph.setVisible(true);
        }
    }
    
    @Override
	public double fitToFunction(double x1, double[] atry, double[] dyda) {
		// TODO Auto-generated method stub
		return 0;
	}

    /**
     * Test data to test fitting of gaussian.
     */
    private void testData() {
    }
    
    /**
     * Gaussian evaluated at a point with given parameters
     */
    private double lorentz(double x) {
    	double exp = -Math.pow(x-xInit, .5) / (Math.pow(gamma, 3));
    	
    	double f = amp*Math.exp(exp);
    	
    	return f;
    }
    
    /**
     * Partial derivative of Lorentz distribution with respect to A.
     */
    private double dLdA(double x) {
    	double exp = -Math.pow(x-xInit, .5) / (Math.pow(gamma, 3));
    	
    	double f = Math.exp(exp);
    	
    	return f;
    	
    }
    
    /**
     * Partial derivative of Lorentz distribution with respect to x.
     */
    private double dLdxInit(double x) {
    	double exp = -.5*Math.pow(x-xInit, -.5) / (Math.pow(gamma, 3));
    	
    	double coeff = (amp * (x-xInit))/(Math.pow(gamma, 2));
    	
    	double f = coeff*Math.exp(exp);
    	
    	return f;
    }
    
    /**
     * Partial derivative of Lorentz distribution with respect to gamma.
     */
    private double dLdgamma(double x) {
    	double exp = -Math.pow(x-xInit, .5) / (Math.pow(gamma, 3));
    	
    	double coeff = (amp/3 * Math.pow(x-xInit, .5))/(Math.pow(gamma, 4));
    	
    	double f = coeff*Math.exp(exp);
    	
    	return f;
    }
    
    /**
     * Jacobian used for non-linear least squares fitting.
     */
    private Matrix generateJacobian() {
    	Matrix jacobian = new Matrix(dataEnd - dataStart, 3);
    	for(int i=dataStart; i<dataEnd; i++) {
    		jacobian.set(i-dataStart, 0, dLdA(xDataOrg[i]));
    		jacobian.set(i-dataStart, 1, dLdxInit(xDataOrg[i]));
    		jacobian.set(i-dataStart, 2, dLdgamma(xDataOrg[i]));
    	}
    	
    	return jacobian;
    }
    
    private Matrix generateResiduals() {
    	Matrix residuals = new Matrix(dataEnd - dataStart, 1);
    	for(int i=dataStart; i<dataEnd; i++) {
    		double r = yDataOrg[i] - lorentz(xDataOrg[i]);
    		residuals.set(i-dataStart, 0, r);
    	}
    	
    	return residuals;
    }
    
}
