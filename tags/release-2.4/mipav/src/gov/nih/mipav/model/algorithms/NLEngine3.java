package gov.nih.mipav.model.algorithms;

import gov.nih.mipav.view.Preferences;
import  gov.nih.mipav.model.structures.jama.*;


 /**
  * Port of MATLAB nlinfit Nonlinearleast-squares fitting by the Gauss-Newton
  * method.
  *
  */

public abstract class NLEngine3 {


	//variables
	protected static double	    anew[];
	protected int			    ma; // number of coefficients
	protected int               ndata; // number of data points
	protected int               kk; // number of iterations
	protected double            a[]; // fitted coefficients
	protected double            xseries[],  yseries[];
	protected double            gues[];

	private double J[][]; // Jacobian
	private int maxiter;
	private double atol;
	private double rtol;
	private double sse;
	private double sseold;
	private double seps;
	private double za[];
	private double s10;
	private double eyep[][];
    private double zerosp[];
    private double anorm;
    private double temp;
    private double yfit[];
    private double r[]; // residuals
    private double delta[];
    private double nb;
    private double aplus[];
    private double yplus[];
    private Matrix Jplus;
    private Matrix rplus;
    private Matrix St;
    private double step[];
    private double yfitnew[];
    private double rnew[];
    private int iter1;

    /**
    *   NLEngine    - non-linear fit to a function.
    *   @param nPts   number of points to fit to the function to
    *   @param _ma    number of parameters of function
    */
    public NLEngine3 (int nPts, int _ma) {

        try {
            ndata   = nPts;
            ma      = _ma;

            xseries = new double[nPts];
            yseries = new double[nPts];
            yfit    = new double[nPts];
            yplus   = new double[nPts];
            r       = new double[nPts];

            a       = new double[ma];
            gues    = new double[ma];
            anew    = new double[ma];
            J       = new double[nPts][ma];
            for (int i = 0; i < ndata; i++) {
                for (int j = 0; j < ma; j++) {
                    J[i][j] = 0.0;
                }
            }
            za = new double[ma];
            for (int i = 0; i < ma; i++) {
                za[i] = 0.0;
            }
            eyep = new double[ma][ma];
            for (int i = 0; i < ma; i++) {
                for (int j = 0; j < ma; j++) {
                    if (i == j) {
                        eyep[i][j] = 1.0;
                    }
                    else {
                        eyep[i][j] = 0.0;
                    }
                }
            }
            zerosp = new double[ma];
            for (int i = 0; i < ma; i++) {
                zerosp[i] = 0.0;
            }
            delta = new double[ma];
            aplus = new double[ma];
            Jplus = new Matrix(nPts+ma,ma);
            rplus = new Matrix(nPts+ma,1);
            step = new double[ma];
            yfitnew = new double[nPts];
            rnew = new double[nPts];
        }
        catch (OutOfMemoryError error){
        }
    }


    /**
    *   getParameters   accessor to function parameters
    *   @return the function parameters determined by the algorithm
    */
    public double[] getParameters() {
        return a;
    }

    /**
     *
     * @return double[] yfit
     */
    public double[] getYfit() {
        return yfit;
    }

    /**
    *   driver
    */
	public void driver() {
	    int i,j,k;
		try {

			// provide an initial guess for the parameters a.
			for (i = 0; i < ma; i++) {
				a[i] = gues[i];
				anew[i] = a[i] + 1.0;
		    }
		    maxiter = 2000;
		    kk = 0;
		    atol = 1.0e-4;
		    rtol = 1.0e-4;
		    sse = 1.0;
		    seps = Math.sqrt(Math.pow(2.0,-52.0));
		    s10 = Math.sqrt(10.0);

		    anorm = 0.0;
		    for (i = 0; i < ma; i++) {
		        temp = (anew[i]-a[i])/(a[i] + seps);
		        anorm += temp*temp;
		    }
		    anorm = Math.sqrt(anorm);
		    while (((anorm > atol) || (Math.abs((sseold - sse)/(sse+seps)) > rtol)) &&
		             kk < maxiter) {
		        if (kk > 0) {
		            for (i = 0; i < ma; i++) {
		                a[i] = anew[i];
		            }
		        }

		        kk++;
		        sseold = 0.0;
                        yfit = fitToFunction(xseries, a);
		        for (i = 0; i < ndata; i++) {
			        r[i] = yseries[i] - yfit[i];
			        sseold += r[i]*r[i];
			    }

			    for (k = 0; k < ma; k++) {
			        for (i = 0; i < ma; i++) {
			            delta[i] = za[i];
			        }
			        if (a[k] == 0.0) {
			            anorm = 0.0;
			            for (i = 0; i < ma; i++) {
			                anorm += a[i]*a[i];
			            }
			            nb = Math.sqrt(Math.sqrt(anorm));
			            if (nb == 0.0) {
			                delta[k] = seps;
			            }
			            else {
			                delta[k] = seps * nb;
			            }
			        } // if (a[k] == 0.0)
			        else {
			            delta[k] = seps * a[k];
			        }
			        for (i = 0; i < ma; i++) {
			            aplus[i] = a[i] + delta[i];
			        }
                                yplus = fitToFunction(xseries, aplus);
			        for (i = 0; i < ndata; i++) {
			            J[i][k] = (yplus[i] - yfit[i])/delta[k];
			        }
			    } // for (k = 0; k < ma; k++)

			    for (i = 0; i < ndata; i++) {
			        for (j = 0; j < ma; j++) {
			            Jplus.set(i,j,J[i][j]);
			        }
			    }
			    for (i = 0; i < ma; i++) {
			        for (j = 0; j < ma; j++) {
			            Jplus.set(i+ndata,j,1.0e-2*eyep[i][j]);
			        }
			    }

			    for (i = 0; i < ndata; i++) {
			        rplus.set(i,0,r[i]);
			    }
			    for (i = 0; i < ma; i++) {
			        rplus.set(i+ndata,0,zerosp[i]);
			    }

			    St = Jplus.solve(rplus);
			    for (i = 0; i < ma; i++) {
			        step[i] = St.get(i,0);
			        anew[i] = a[i] + step[i];
			    }

			    sse = 0.0;
                            yfitnew = fitToFunction(xseries, anew);
			    for (i = 0; i < ndata; i++) {
			        rnew[i] = yseries[i] - yfitnew[i];
			        sse += rnew[i]*rnew[i];
			    }

			    iter1 = 0;
			    while ((sse > sseold) && (iter1 < 12)) {
			        for (i = 0; i < ma; i++) {
			            step[i] = step[i]/s10;
			            anew[i] = a[i] + step[i];
			        } // for (i = 0; i < ma; i++)

			        sse = 0.0;
                                yfitnew = fitToFunction(xseries, anew);
			        for (i = 0; i < ndata; i++) {
			            rnew[i] = yseries[i] - yfitnew[i];
			            sse += rnew[i]*rnew[i];
			        } // for (i = 0; i < ndata; i++)
			        iter1++;
			    } // while ((sse > sseold) && (iter1 < 12))

		        anorm = 0.0;
		        for (i = 0; i < ma; i++) {
		            temp = (anew[i]-a[i])/(a[i] + seps);
		            anorm += temp*temp;
		        }
		        anorm = Math.sqrt(anorm);
		    } // while (((anorm > atol) || (Math.abs((sseold - sse)/(sse+seps)) > rtol)) &&
		            // kk < maxiter)
		    if (kk == maxiter) {
		        Preferences.debug("NLINFIT did not converge");
		        Preferences.debug("Returing results from last iteration");
		    }
		}
		catch (Exception e) {
			Preferences.debug("driver error: " + e.getMessage());
		}
	}



	public abstract double[] fitToFunction(double x1[], double atry[]);




	/*
    *   fitLine -
    *   @param x1    the x value of the data point
    *   @param atry  the best guess parameter values
    *   @return      the calculated y value
    */
/*	public double[] fitLine(double x1[], double atry[]) {
        // called by mrqcof
        // mrqcof supplies x1 and atry[]
        // function returns the calculated ymod
        double ymod[] = new double[x1.length];
        int i;
        double fac;
  		try {
                      for (i = 0; i < x1.length; i++) {
			fac     = atry[1] * x1[i];
			ymod[i]    = atry[0] + fac;
                     }
		}
		catch (Exception e) {
			if (Preferences.isDebug()) System.err.println("function error: " + e.getMessage());
		}
		return ymod;
    }
  */
    /*
    *   fitExponential - a0 + a1*exp(a2*x)
    *   @param x1    the x value of the data point
    *   @param atry  the best guess parameter values
    *   @return      the calculated y value
    */
    /*public double[] fitExponential(double x1[], double atry[]) {
        // mrqcof calls function
        // mrqcof supplies x1 and best guess parameters atry[]
        // function returns the calculated ymod
        double ymod[] = new double[x1.length];
        int i;
  		try {
                 for (i = 0; i < x1.length; i++) {
  		    ymod[i] = atry[0] + atry[1]*Math.exp(atry[2] * x1[i]);
                 }
		}
		catch (Exception e) {
			if (Preferences.isDebug()) System.err.println("function error: " + e.getMessage());
		}
		return ymod;
    }*/


}
