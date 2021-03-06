package gov.nih.mipav.model.algorithms;


import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.awt.*;

import java.io.*;

import java.util.*;


/**
 * Snake-like algorithm deriviative using BSplines. The algorithm is supplied a polygon (VOI - contour) and that polygon
 * is allowed to evolve to edge of the object generated by calculating the gradient magnitude (i.e. the energy function)
 * at scale define by the user. The user/programmer supplies the sigmas (scales) at which to calculate the gradient
 * magnitude. A large scale slows the snake and causes the snake to conform to large scale structure. A small sigma
 * value causes the snake to conform to the small scale structure is therefore more sensitive to noise. The
 * three-dimensional version is really a two-and-half dimensional algorithm where resultant contour in a slice is
 * projected into the adjacent slice and is used as an initialization to the evolution in the new slice.
 *
 * @version  0.1 Feb 11, 1998
 * @author   Matthew J. McAuliffe, Ph.D.
 * @see      AlgorithmBSpline
 * @see      GenerateGaussian
 * @see      AlgorithmVOISimplexOpt
 * @see      AlgorithmSnake
 */
public class AlgorithmBSnake extends AlgorithmBase {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Do not propagate the source VOI to any slices (2D). */
    public static final int PROP_SINGLE = 0;

    /** Propagate the source VOI to slices above its current slice in the volume. */
    public static final int PROP_NEXT = 1;

    /** Propagate the source VOI to slices below its current slice in the volume. */
    public static final int PROP_PREV = 2;

    /** Propagate the source VOI to slices above and below its current slice. */
    public static final int PROP_ALL = 3;

    /** The maximum ratio of change in the energy under the VOI after propagation. */
    private static final float PROP_THRESHOLD = 0.5f;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** Maximum number of snake iterations. */
    private int boundaryIterations = 20;

    /** Storage location of the first derivative of the Gaussian in the X direction. */
    private float[] GxData;

    /** Storage location of the first derivative of the Gaussian in the Y direction. */
    private float[] GyData;

    /** Storage location of the first derivative of the Gaussian in the Z direction. */
    private float[] GzData;

    /** Dimensionality of the kernel. */
    private int[] kExtents;

    /** The VOI propagation mode to use. */
    private int propagationType = PROP_ALL;

    /** The resultant polygon and the evolution has completed. */
    private VOI resultVOI;

    /** Standard deviations of the gaussian used to calculate the kernels. */
    private float[] sigmas;

    /** Source image. */
    private ModelImage srcImage;

    /** The initial VOI to initialize the evolution process. */
    private VOI srcVOI;

    /** Starting slice to evolve contour. */
    private int stSlice;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Set up the snake algorithm so that it can be run.
     *
     * @param  srcImg              2D or 3D source image
     * @param  sigmas              describe the scale of the gaussian in each dimension
     * @param  boundaryIterations  maximum number of snake iterations
     * @param  srcVOI              VOI that is to be evolved
     */
    public AlgorithmBSnake(ModelImage srcImg, float[] sigmas, int boundaryIterations, VOI srcVOI) {

        srcImage = srcImg;
        this.srcVOI = srcVOI;
        this.sigmas = sigmas;
        this.boundaryIterations = boundaryIterations;

        if (srcImg.getNDims() == 2) {
            makeKernels2D();
            resultVOI = new VOI((short) srcImage.getVOIs().size(), "BsnakeVOI", 1, VOI.CONTOUR, -1.0f);
        } else if (srcImg.getNDims() > 2) {

            if (sigmas[2] == 0.0f) {
                makeKernels2D();
            } else {
                makeKernels3D();
            }

            resultVOI = new VOI((short) srcImage.getVOIs().size(), "BsnakeVOI", srcImage.getExtents()[2], VOI.CONTOUR,
                                -1.0f);
        }
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Prepares this class for destruction.
     */
    public void finalize() {
        GxData = null;
        GyData = null;
        GzData = null;
        srcImage = null;
        sigmas = null;
        kExtents = null;
        super.finalize();
    }

    /**
     * Accessor that returns the resultant VOI.
     *
     * @return  resultant VOI that has localized to the boundaries of the object
     */
    public VOI getResultVOI() {
        return resultVOI;
    }


    /**
     * Starts the snake algorithm.
     */
    public void runAlgorithm() {

        if (srcImage == null) {
            displayError("Source Image is null");

            return;
        } else {

            if (srcImage.getNDims() == 2) {
                calc2D();
            } else if (srcImage.getNDims() > 2) {
                calc3D();
            }
        }

    }

    /**
     * Sets the propagation type.
     *
     * @param  type  if PROP_ALL, result contour from a slice is propagated to the adjacent slice and used to initialize
     *               the snake algorithm for that slice. If PROP_NEXT, result contour from the original slice is
     *               propagated to the next slice and used to initialize the snake algorithm for that slice. If
     *               PROP_PREV, result contour from the orginal slice is propagated to the previous slice. If
     *               PROP_SINGLE, the snake algorithm stops after optimizing the boundary in the present slice.
     */
    public void setPropagation(int type) {
        propagationType = type;
    }

    /**
     * Prepares the data and runs the algorithm for a 2D image.
     */
    private void calc2D() {

        float[] imgBuffer;
        int xDim, yDim, length;
        float[] xPoints = null;
        float[] yPoints = null;
        Polygon resultGon = new Polygon();
        Polygon[] gons = null;
        Vector[] contours;
        int nContours;

        xDim = srcImage.getExtents()[0];
        yDim = srcImage.getExtents()[1];

        try {
            length = xDim * yDim;
            imgBuffer = new float[length];
            srcImage.exportData(0, length, imgBuffer); // locks and releases lock
            fireProgressStateChanged(srcImage.getImageName(), "Bspline snake: Evolving boundary ...");
            
        } catch (IOException error) {
            displayError("Algorithm Bsnake: Image(s) locked");
            setCompleted(false);

            return;
        } catch (OutOfMemoryError e) {
            displayError("Algorithm BSnake:  Out of Memory");
            setCompleted(false);

            return;
        }

        fireProgressStateChanged(25);

        contours = srcVOI.getCurves();
        nContours = contours[0].size();
        fireProgressStateChanged(30);

        for (int j = 0; j < nContours; j++) {

            if (((VOIContour) (contours[0].elementAt(j))).isActive()) {
                gons = srcVOI.exportPolygons(0);
                xPoints = new float[gons[j].npoints + 5];
                yPoints = new float[gons[j].npoints + 5];
                resultGon = new Polygon();
                setPoints(xPoints, yPoints, gons[j]);
                runSnake(xPoints, yPoints, imgBuffer, resultGon);
                resultVOI.importPolygon(resultGon, 0);
                ((VOIContour) (resultVOI.getCurves()[0].lastElement())).trimPoints(Preferences.getTrim(),
                                                                                   Preferences.getTrimAdjacient());
            } else {
                gons = srcVOI.exportPolygons(0);
                resultVOI.importPolygon(gons[j], 0);
            }

            fireProgressStateChanged(30 + (((j / nContours) - 1) * 70));
        }

        fireProgressStateChanged(100);

        if (threadStopped) {
            finalize();

            return;
        }

        
        setCompleted(true);
    }

    /**
     * Prepares the data and runs the algorithm for a 3D image.
     */
    private void calc3D() {

        int length;
        float[] imgBuffer;
        int slice;
        float baseEnergy = 0, energy, tempEnergy;
        int baseNPts = 0, nPts;
        Polygon tempGon;
        Polygon baseGon;
        float[] xPoints, yPoints;

        Polygon resultGon = null;
        Polygon[] gons = null;
        Vector[] contours;
        int nContours;
        int nSlices = srcImage.getExtents()[2];
        boolean failureFlag = false;

        try {
            length = srcImage.getSliceSize();
            imgBuffer = new float[length];

            fireProgressStateChanged(srcImage.getImageName(), "Bspline snake: Evolving boundary ...");
            
        } catch (OutOfMemoryError e) {
            displayError("Algorithm Bsnake: Out of memory");
            setCompleted(false);

            return;
        }

        contours = srcVOI.getCurves();
        fireProgressStateChanged(30);

        for (slice = 0; slice < nSlices; slice++) {
            fireProgressStateChanged((int) (30 + (((float) slice / (nSlices - 1)) * 70)));

            try {
                srcImage.exportData(slice * length, length, imgBuffer);
            } catch (IOException error) {
                displayError("Algorithm Bsnake: Image(s) locked");
                setCompleted(false);

                return;
            }

            nContours = contours[slice].size();

            for (int j = 0; j < nContours; j++) {

                if (((VOIContour) (contours[slice].elementAt(j))).isActive()) {

                    ((VOIContour) (contours[slice].elementAt(j))).makeClockwise();

                    gons = srcVOI.exportPolygons(slice);

                    // @todo + 2 in AlgorithmSnake (why?)
                    xPoints = new float[gons[j].npoints + 5];
                    yPoints = new float[gons[j].npoints + 5];

                    resultGon = new Polygon();
                    setPoints(xPoints, yPoints, gons[j]);

                    baseEnergy = runSnake(xPoints, yPoints, imgBuffer, resultGon);

                    baseNPts = resultGon.npoints;
                    resultVOI.importPolygon(resultGon, slice);
                    ((VOIContour) (resultVOI.getCurves()[slice].lastElement())).trimPoints(Preferences.getTrim(),
                                                                                           Preferences.getTrimAdjacient());
                    stSlice = slice;
                } else {
                    gons = srcVOI.exportPolygons(slice);
                    resultVOI.importPolygon(gons[j], slice);
                }
            }
        }

        if (propagationType == PROP_SINGLE) {
            fireProgressStateChanged(100);
            setCompleted(true);
            

            return;
        }

        slice = stSlice + 1;
        baseGon = new Polygon(resultGon.xpoints, resultGon.ypoints, resultGon.npoints);
        tempGon = resultGon;
        resultGon = new Polygon();

        Polygon optGon = null;

        tempEnergy = baseEnergy;

        // @todo + 2 in AlgorithmSnake (why?)
        xPoints = new float[tempGon.npoints + 5];
        yPoints = new float[tempGon.npoints + 5];
        fireProgressStateChanged(25);

        int percent = 25;
        int increment = (100 - percent) / (nSlices);

        AlgorithmVOISimplexOpt voiSimplex = new AlgorithmVOISimplexOpt(srcImage, sigmas, AlgorithmVOISimplexOpt.MAXSUM);

        if (propagationType != PROP_PREV) {

            while (!threadStopped) {

                try {
                    srcImage.exportData(slice * length, length, imgBuffer);
                } catch (IOException error) {
                    displayError("Algorithm Bsnake: Image(s) locked");
                    setCompleted(false);
                    

                    return;
                }

                optGon = voiSimplex.goOptimize(imgBuffer, tempGon);
                percent += increment;
                fireProgressStateChanged(percent);
                setPoints(xPoints, yPoints, tempGon);

                energy = runSnake(xPoints, yPoints, imgBuffer, resultGon);
                nPts = resultGon.npoints;

                // Preferences.debug(" Points = " + nPts + " energy = " + Math.abs(energy/tempEnergy) + " energy/pt = "
                // + Math.abs((energy/nPts)/(baseEnergy/baseNPts)));

                if ((nPts < 8) || (Math.abs((energy / tempEnergy) - 1) > PROP_THRESHOLD) ||
                        (Math.abs(((energy / nPts) / (baseEnergy / baseNPts)) - 1) > PROP_THRESHOLD)) {

                    // if the energy sum changed too much, then the single propagation that we wanted to do has failed
                    if (propagationType == PROP_NEXT) {
                        setCompleted(false);
                        failureFlag = true;
                    }

                    break;
                } else {
                    resultVOI.importPolygon(resultGon, slice);
                    ((VOIContour) (resultVOI.getCurves()[slice].lastElement())).trimPoints(Preferences.getTrim(),
                                                                                           Preferences.getTrimAdjacient());
                }

                // @todo + 2 in AlgorithmSnake (why?)
                xPoints = new float[resultGon.npoints + 5];
                yPoints = new float[resultGon.npoints + 5];
                tempGon = resultGon;
                resultGon = new Polygon();
                tempEnergy = energy;
                slice++;

                if (slice >= srcImage.getExtents()[2]) {

                    // if we tried to propagate to the next slice, which doesn't exist
                    if (propagationType == PROP_NEXT) {
                        setCompleted(false);
                        failureFlag = true;
                    }

                    break;
                }

                // only propagate to one other slice if PROP_NEXT
                if (propagationType == PROP_NEXT) {
                    fireProgressStateChanged(100);

                    if (!failureFlag) {
                        setCompleted(true);
                    }

                    

                    return;
                }
            }

            if (threadStopped) {
                finalize();

                return;
            }
        }

        if (propagationType != PROP_NEXT) {
            slice = stSlice - 1;
            tempEnergy = baseEnergy;
            tempGon = baseGon;

            // @todo + 2 in AlgorithmSnake (why?)
            xPoints = new float[tempGon.npoints + 5];
            yPoints = new float[tempGon.npoints + 5];
            resultGon = new Polygon();
            fireProgressStateChanged(percent);

            while (!threadStopped) {

                try {
                    srcImage.exportData(slice * length, length, imgBuffer);
                } catch (IOException error) {
                    displayError("Algorithm Bsnake: Image(s) locked");
                    setCompleted(false);
                    

                    return;
                }

                optGon = voiSimplex.goOptimize(imgBuffer, tempGon);
                percent += increment;
                fireProgressStateChanged(percent);
                setPoints(xPoints, yPoints, tempGon);
                energy = runSnake(xPoints, yPoints, imgBuffer, resultGon);
                nPts = resultGon.npoints;

                if ((nPts < 8) || (Math.abs((energy / tempEnergy) - 1) > PROP_THRESHOLD) ||
                        (Math.abs(((energy / nPts) / (baseEnergy / baseNPts)) - 1) > PROP_THRESHOLD)) {

                    // if the energy sum changed too much, then the single propagation that we wanted to do has failed
                    if (propagationType == PROP_PREV) {
                        setCompleted(false);
                        failureFlag = true;
                    }

                    break;
                } else {
                    resultVOI.importPolygon(resultGon, slice);
                    ((VOIContour) (resultVOI.getCurves()[slice].lastElement())).trimPoints(Preferences.getTrim(),
                                                                                           Preferences.getTrimAdjacient());
                }

                // @todo + 2 in AlgorithmBSnake (why?)
                xPoints = new float[resultGon.npoints + 5];
                yPoints = new float[resultGon.npoints + 5];
                tempGon = resultGon;
                resultGon = new Polygon();
                tempEnergy = energy;
                slice--;

                if (slice < 0) {

                    // if we tried to propagate to the previous slice, which doesn't exsist
                    if (propagationType == PROP_PREV) {
                        setCompleted(false);
                        failureFlag = true;
                    }

                    break;
                }

                // only propagate to one other slice if PROP_PREV
                if (propagationType == PROP_PREV) {
                    fireProgressStateChanged(100);

                    if (!failureFlag) {
                        setCompleted(true);
                    }

                    

                    return;
                }
            }

            if (threadStopped) {
                finalize();

                return;
            }
        }

        fireProgressStateChanged(100);

        if (!failureFlag) {
            setCompleted(true);
        }

        
    }


    /**
     * Makes 2D derivative kernels to be used in the calculation of the gradient magnitude.
     */
    private void makeKernels2D() {
        int xkDim, ykDim;
        int[] derivOrder = new int[2];

        kExtents = new int[2];
        derivOrder[0] = 1;
        derivOrder[1] = 0;

        xkDim = Math.round(5 * sigmas[0]);

        if ((xkDim % 2) == 0) {
            xkDim++;
        }

        kExtents[0] = xkDim;

        ykDim = Math.round(5 * sigmas[1]);

        if ((ykDim % 2) == 0) {
            ykDim++;
        }

        kExtents[1] = ykDim;

        GxData = new float[xkDim * ykDim];

        GenerateGaussian Gx = new GenerateGaussian(GxData, kExtents, sigmas, derivOrder);
        Gx.calc(false);

        derivOrder[0] = 0;
        derivOrder[1] = 1;
        GyData = new float[xkDim * ykDim];

        GenerateGaussian Gy = new GenerateGaussian(GyData, kExtents, sigmas, derivOrder);
        Gy.calc(true);
    }


    /**
     * Makes 3D derivative kernels to be used in the calculation of the gradient magnitude.
     */
    private void makeKernels3D() {
        int xkDim, ykDim, zkDim;
        int[] derivOrder = new int[3];

        kExtents = new int[3];

        xkDim = Math.round(5 * sigmas[0]);

        if ((xkDim % 2) == 0) {
            xkDim++;
        }

        kExtents[0] = xkDim;

        ykDim = Math.round(5 * sigmas[1]);

        if ((ykDim % 2) == 0) {
            ykDim++;
        }

        kExtents[1] = ykDim;

        zkDim = Math.round(5 * sigmas[2]);

        if ((zkDim % 2) == 0) {
            zkDim++;
        }

        kExtents[2] = zkDim;

        derivOrder[0] = 1;
        derivOrder[1] = 0;
        derivOrder[2] = 0;
        GxData = new float[xkDim * ykDim * zkDim];

        GenerateGaussian Gx = new GenerateGaussian(GxData, kExtents, sigmas, derivOrder);
        Gx.calc(false);

        derivOrder[0] = 0;
        derivOrder[1] = 1;
        derivOrder[2] = 0;
        GyData = new float[xkDim * ykDim * zkDim];

        GenerateGaussian Gy = new GenerateGaussian(GyData, kExtents, sigmas, derivOrder);
        Gy.calc(true);

        derivOrder[0] = 0;
        derivOrder[1] = 0;
        derivOrder[2] = 1;
        GzData = new float[xkDim * ykDim * zkDim];

        GenerateGaussian Gz = new GenerateGaussian(GzData, kExtents, sigmas, derivOrder);
        Gz.calc(true);
    }


    /**
     * Actual function that evolves the boundary.
     *
     * @param   xPoints    x coordinates that describe the contour
     * @param   yPoints    y coordinates that describe the contour
     * @param   image      image data
     * @param   resultGon  resultant polygon
     *
     * @return  the sum of the energy along the boundary of the <code>resultGon</code>
     */
    private float runSnake(float[] xPoints, float[] yPoints, float[] image, Polygon resultGon) {
        int i, j;
        int nPts;
        float pct;
        float index;
        Point2Df interpPt = new Point2Df();
        Point2Df inNormPt = new Point2Df();
        Point2Df outNormPt = new Point2Df();
        Point2Df tangentDir = new Point2Df();
        Point2Df normDir = new Point2Df();
        Point2Df normStep = new Point2Df();
        float[] newXPts, newYPts;
        float normLength;
        float stepPct = (float) 0.75;
        float ix, iy;
        float gradMag, inGradMag, outGradMag;
        float energy = 0, oldEnergy = 0;

        AlgorithmArcLength arcLength = new AlgorithmArcLength(xPoints, yPoints);
        AlgorithmBSpline bSpline = new AlgorithmBSpline();

        nPts = Math.round(arcLength.getTotalArcLength() / 2);
        newXPts = new float[nPts + 5];
        newYPts = new float[nPts + 5];


        for (int z = 0; (z < boundaryIterations) && (!threadStopped); z++) {
            energy = 0;

            for (i = 0; i < nPts; i++) {
                pct = i / (float) (nPts);

                // Note that a pct of 0 gives an index of 2 and
                // a pct of 1 gives an index 2 less than the maximum
                // possible
                index = arcLength.invlen(pct);
                interpPt = bSpline.bSplineJetXY(0, index, xPoints, yPoints);
                tangentDir = bSpline.bSplineJetXY(1, index, xPoints, yPoints);

                normLength = (float) Math.sqrt((tangentDir.x * tangentDir.x) + (tangentDir.y * tangentDir.y));
                normDir.x = -tangentDir.y / normLength;
                normDir.y = tangentDir.x / normLength;

                normStep.x = stepPct * normDir.x;
                normStep.y = stepPct * normDir.y;

                outNormPt.x = normStep.x + interpPt.x;
                outNormPt.y = normStep.y + interpPt.y;

                inNormPt.x = -normStep.x + interpPt.x;
                inNormPt.y = -normStep.y + interpPt.y;

                ix = AlgorithmConvolver.convolve2DPt(interpPt, srcImage.getExtents(), image, kExtents, GxData);
                iy = AlgorithmConvolver.convolve2DPt(interpPt, srcImage.getExtents(), image, kExtents, GyData);

                // gradMag = (float)Math.sqrt(ix*ix + iy*iy);
                gradMag = (ix * ix) + (iy * iy);


                ix = AlgorithmConvolver.convolve2DPt(inNormPt, srcImage.getExtents(), image, kExtents, GxData);
                iy = AlgorithmConvolver.convolve2DPt(inNormPt, srcImage.getExtents(), image, kExtents, GyData);

                // inGradMag = (float)Math.sqrt(ix*ix + iy*iy);
                inGradMag = (ix * ix) + (iy * iy);

                ix = AlgorithmConvolver.convolve2DPt(outNormPt, srcImage.getExtents(), image, kExtents, GxData);
                iy = AlgorithmConvolver.convolve2DPt(outNormPt, srcImage.getExtents(), image, kExtents, GyData);

                // outGradMag = (float)Math.sqrt(ix*ix + iy*iy);
                outGradMag = (ix * ix) + (iy * iy);

                if ((outGradMag > gradMag) || (inGradMag > gradMag)) {

                    if (outGradMag > inGradMag) {
                        newXPts[i + 2] = outNormPt.x;
                        newYPts[i + 2] = outNormPt.y;
                        energy += outGradMag;
                    } else {
                        newXPts[i + 2] = inNormPt.x;
                        newYPts[i + 2] = inNormPt.y;
                        energy += inGradMag;
                    }
                } else {
                    newXPts[i + 2] = interpPt.x;
                    newYPts[i + 2] = interpPt.y;
                    energy += gradMag;
                }
            }
            // After an interation the first point starts at an index of 2
            // and the last points is present at an index of nPts + 1
            // The first point at an index of 2 must be used twice -
            // once for the segment going from the first to the second
            // point and once for the segment going from the last to the
            // first point.  The last 2 points at each end then provide
            // wrap around segments.

            newXPts[0] = newXPts[nPts];
            newYPts[0] = newYPts[nPts];
            newXPts[1] = newXPts[nPts + 1];
            newYPts[1] = newYPts[nPts + 1];
            newXPts[nPts + 2] = newXPts[2];
            newYPts[nPts + 2] = newYPts[2];
            newXPts[nPts + 3] = newXPts[3];
            newYPts[nPts + 3] = newYPts[3];
            newXPts[nPts + 4] = newXPts[4];
            newYPts[nPts + 4] = newYPts[4];

            xPoints = newXPts;
            yPoints = newYPts;

            arcLength.setPoints(xPoints, yPoints);

            newXPts = new float[xPoints.length];
            newYPts = new float[xPoints.length];

            if (Math.abs((energy / oldEnergy) - 1) < 0.0001) {
                break;
            }

            oldEnergy = energy;
        }

        for (j = 2; j < (yPoints.length - 3); j++) {
            resultGon.addPoint(Math.round(xPoints[j]), Math.round(yPoints[j]));
        }

        return energy;
    }

    /**
     * Takes the polygon and forms two special arrays for use in the Bspline.
     *
     * @param  xPoints  storage location of array of x coord. points
     * @param  yPoints  storage location array of y coord. points
     * @param  gon      initial polygon
     */
    private void setPoints(float[] xPoints, float[] yPoints, Polygon gon) {
        int i;

        /** Note that 0 is used twice - once in the 0 to 1 segment and once
         * in the n-1 to zero segment. */
        xPoints[0] = gon.xpoints[gon.npoints - 2];
        yPoints[0] = gon.ypoints[gon.npoints - 2];

        xPoints[1] = gon.xpoints[gon.npoints - 1];
        yPoints[1] = gon.ypoints[gon.npoints - 1];

        for (i = 0; i < gon.npoints; i++) {
            xPoints[i + 2] = gon.xpoints[i];
            yPoints[i + 2] = gon.ypoints[i];
        }

        xPoints[gon.npoints + 2] = gon.xpoints[0];
        yPoints[gon.npoints + 2] = gon.ypoints[0];

        xPoints[gon.npoints + 3] = gon.xpoints[1];
        yPoints[gon.npoints + 3] = gon.ypoints[1];

        xPoints[gon.npoints + 4] = gon.xpoints[2];
        yPoints[gon.npoints + 4] = gon.ypoints[2];
    }
}
