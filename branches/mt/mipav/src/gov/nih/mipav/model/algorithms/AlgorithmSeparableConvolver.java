package gov.nih.mipav.model.algorithms;


import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.io.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;


/**
 * Convolves an image with a separable (symmetric) kernel and returns the result. The Gaussian and its derivatives are
 * separable.
 *
 * <p>Faster than the regualar convolver -- reg 2D: n^d*m^d, sep 2D: d*n^d*m ; d = img dimension, n = img size, m = kern
 * size -- but requires more memory -- ~2 times more for 2D, ~3 times for 3D (plus significant extra memory required for
 * algorithms which use this class (2 or 3 times more, depending on the dimension)).</p>
 *
 * <p>Also note that this convolver has a different interface which must be used than the static point convolution
 * functions which most algorithms use from the AlgorithmConvolver.</p>
 *
 * <p>Color and voi mask code not tested (although the voi code is pretty straight-forward).</p>
 *
 * @see     AlgorithmConvolver
 * @author  Evan McCreedy
 */
public class AlgorithmSeparableConvolver extends AlgorithmBase {

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** Buffer size adjustment for color images. */
    private int cFactor = 1;

    /** Flag to indicate if the source image is color. */
    private boolean colorImage = false;

    int curPercent;
    
    int incIndex;
    /** Holds the result image data. */
    private double[] destBuffer;

    /** Whether to convolve the whole image or just pixels inside a mask. */
    private boolean entireImage = true;

    /** Holds the result image data (in float form) only used with the buffer constructor. */
    private float[] floatDestBuffer;

    /** Holds the original image data. */
    private double[] imgBuffer;

    /** The dimensions of the both source and destination images. */
    private int[] imgExtents;

    /** Holds the kernel image. */
    private ModelImage kernel;

    /** The dimensions of the kernel. */
    private int[] kernelExtents;

    /** The separated kernel in the X dimension. */
    private float[] kernelXBuffer;

    /** The separated kernel in the Y dimension. */
    private float[] kernelYBuffer;

    /** The separated kernel in the Z dimension. */
    private float[] kernelZBuffer;

    /** Flags to indicate which color channels to process. */
    private boolean red = true, green = true, blue = true;

    /** Holds the transformed image data between convolutions in different dimensions. */
    private double[] tempImgBuffer;

    //~ Constructors ---------------------------------------------------------------------------------------------------

  
    /**
     * Sets destination, source, and kernel images. Call run() to convolve image.
     *
     * @param  destImg  destination image
     * @param  srcImg   source image
     * @param  kern     kernel image (kernel must be symmetric)
     * @param  minProgressValue the minimum progress value.
     * @param  maxProgressValue the maximum progress value.
     */
    public AlgorithmSeparableConvolver(ModelImage destImg, ModelImage srcImg, ModelImage kern) {
        super(destImg, srcImg);
        kernel = kern;

        imgExtents = srcImg.getExtents();
        kernelExtents = kern.getExtents();

        if (srcImage.isColorImage()) {
            cFactor = 4;
            colorImage = true;
        }

        // fill data arrays for image and kernels
        if (srcImg.getNDims() == 2) {
            convolverSetup2D();
        } else if (srcImg.getNDims() == 3) {
            convolverSetup3D();
        }
    }

    /**
     * Sets destination, source, and kernel buffers. Call run() to convolve image.
     *
     * @param  destBuffer  destination image data buffer
     * @param  srcBuffer   source image data buffer
     * @param  iExtents    source and destination image dimensions
     * @param  kernBuffer  kernel image data buffer (kernel must be symmetric)
     * @param  kExtents    kernel dimensions
     * @param  color       whether the image is color
     * @param  minProgressValue the minimum progress value.
     * @param  maxProgressValue the maximum progress value.
     */
    public AlgorithmSeparableConvolver(float[] destBuffer, float[] srcBuffer, int[] iExtents, float[] kernBuffer,
                                       int[] kExtents, boolean color) {
        super(null, null);

        imgBuffer = new double[srcBuffer.length];

        for (int i = 0; i < srcBuffer.length; i++) {
            imgBuffer[i] = (double) srcBuffer[i];
        }

        floatDestBuffer = destBuffer;
        imgExtents = iExtents;
        kernelExtents = kExtents;
        colorImage = color;

        if (color) {
            cFactor = 4;
        }

        this.destBuffer = new double[destBuffer.length];

        //tempImgBuffer = new double[imgBuffer.length];

        if (imgExtents.length == 2) {
            int i;
            int length = kernelExtents[0];

            kernelXBuffer = new float[length];

            for (i = 0; i < length; i++) {
                kernelXBuffer[i] = kernBuffer[i];
            }

            length = kernelExtents[1];
            kernelYBuffer = new float[length];

            for (i = 0; i < length; i++) {
                kernelYBuffer[i] = kernBuffer[i * kExtents[0]];
            }
        } else if (imgExtents.length == 3) {
            int i;
            int length = kernelExtents[0];

            kernelXBuffer = new float[length];

            for (i = 0; i < length; i++) {
                kernelXBuffer[i] = kernBuffer[i];
            }

            length = kernelExtents[1];
            kernelYBuffer = new float[length];

            for (i = 0; i < length; i++) {
                kernelYBuffer[i] = kernBuffer[i * kExtents[0]];
            }

            length = kernelExtents[2];
            kernelZBuffer = new float[length];

            for (i = 0; i < length; i++) {
                kernelZBuffer[i] = kernBuffer[i * kExtents[0] * kExtents[1]];
            }
        }
    }

    /**
     * Sets destination, source, and kernel buffers. Call run() to convolve image.
     *
     * @param  destBuffer   destination image data buffer
     * @param  srcBuffer    source image data buffer
     * @param  iExtents     source and destination image dimensions
     * @param  kernXBuffer  kernel image data buffer in X dimension (kernel must be symmetric)
     * @param  kernYBuffer  kernel image data buffer in Y dimension (kernel must be symmetric)
     * @param  color        whether the image is color
     * @param  minProgressValue the minimum progress value.
     * @param  maxProgressValue the maximum progress value.
     */
    public AlgorithmSeparableConvolver(float[] destBuffer, float[] srcBuffer, int[] iExtents, float[] kernXBuffer,
                                       float[] kernYBuffer, boolean color) {
        super(null, null);

        imgBuffer = new double[srcBuffer.length];

        for (int i = 0; i < srcBuffer.length; i++) {
            imgBuffer[i] = (double) srcBuffer[i];
        }

        floatDestBuffer = destBuffer;
        imgExtents = iExtents;
        colorImage = color;

        if (color) {
            cFactor = 4;
        }

        this.destBuffer = new double[destBuffer.length];

        //tempImgBuffer = new double[imgBuffer.length];

        this.kernelXBuffer = kernXBuffer;
        this.kernelYBuffer = kernYBuffer;

        kernelExtents = new int[] { kernelXBuffer.length, kernelYBuffer.length };
    }

    /**
     * Sets destination, source, and kernel buffers. Call run() to convolve image.
     *
     * @param  destBuffer   destination image data buffer
     * @param  srcBuffer    source image data buffer
     * @param  iExtents     source and destination image dimensions
     * @param  kernXBuffer  kernel image data buffer in X dimension (kernel must be symmetric)
     * @param  kernYBuffer  kernel image data buffer in Y dimension (kernel must be symmetric)
     * @param  kernZBuffer  kernel image data buffer in Z dimension (kernel must be symmetric)
     * @param  color        whether the image is color
     * @param  minProgressValue the minimum progress value.
     * @param  maxProgressValue the maximum progress value.
     */
    public AlgorithmSeparableConvolver(float[] destBuffer, float[] srcBuffer, int[] iExtents, float[] kernXBuffer,
                                       float[] kernYBuffer, float[] kernZBuffer, boolean color) {
        super(null, null);

        imgBuffer = new double[srcBuffer.length];

        for (int i = 0; i < srcBuffer.length; i++) {
            imgBuffer[i] = (double) srcBuffer[i];
        }

        floatDestBuffer = destBuffer;
        imgExtents = iExtents;
        colorImage = color;

        if (color) {
            cFactor = 4;
        }

        this.destBuffer = new double[destBuffer.length];

        tempImgBuffer = new double[imgBuffer.length];

        this.kernelXBuffer = kernXBuffer;
        this.kernelYBuffer = kernYBuffer;
        this.kernelZBuffer = kernZBuffer;

        kernelExtents = new int[] { kernelXBuffer.length, kernelYBuffer.length, kernelZBuffer.length };
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Prepares this class for destruction.
     */
    public void finalize() {
        kernel = null;
        destImage = null;
        srcImage = null;
        imgBuffer = null;
        imgExtents = null;
        tempImgBuffer = null;
        kernelExtents = null;
        kernelXBuffer = null;
        kernelYBuffer = null;
        kernelZBuffer = null;
        destBuffer = null;
        floatDestBuffer = null;

        super.finalize();
    }

    /**
     * Begins the excution of the 2D convolver.
     */
    public void run2D() {
        double sum = 0;
        double norm = 0;

        int offsetX, offsetY;
        int startX, endX;
        int startY, endY;

        int xDim = imgExtents[0];
        int yDim = imgExtents[1];

        int xKDim;
        int yKDim;

        int halfxKDim;
        int halfyKDim;
        int imageLength = xDim * yDim * cFactor;
        int stepY;

        int offset = xDim * cFactor;

        int offsetYTimesOffset;

        boolean skipRed = false;
        boolean skipGreen = false;
        boolean skipBlue = false;

        if (colorImage && !red) {
            skipRed = true;
        }

        if (colorImage && !green) {
            skipGreen = true;
        }

        if (colorImage && !blue) {
            skipBlue = true;
        }

        // x kernel dimensions
        xKDim = kernelXBuffer.length;
        yKDim = 1;
        halfxKDim = xKDim / 2;
        halfyKDim = 0;

        int halfXKDimTimesCFactor = halfxKDim * cFactor;

        int stepProgressValue = (imgExtents[0] * imgExtents[1] * cFactor * 2) / 100;

        int progress = 0;
        int index = 0;
        // convolve the image with the X dimension kernel
        for (int pix = 0; (pix < imageLength) && !threadStopped; pix++, index++) {

            if (index % stepProgressValue == 0) {
                fireProgressStateChanged(progress++);
            }

            if (skipRed && ((pix % 4) == 1)) {
                tempImgBuffer[pix] = imgBuffer[pix];
            } else if (skipGreen && ((pix % 4) == 2)) {
                tempImgBuffer[pix] = imgBuffer[pix];
            } else if (skipBlue && ((pix % 4) == 3)) {
                tempImgBuffer[pix] = imgBuffer[pix];
            } else if (entireImage || mask.get(pix / cFactor)) {
                offsetX = (pix % offset) - halfXKDimTimesCFactor;

                offsetY = (pix / offset);

                offsetYTimesOffset = offset * offsetY;

                sum = 0;
                int count = 0;
                norm = 0;
                startX = offsetX;
                endX = startX + ((xKDim - 1) * cFactor);

                if (startX < 0) {
                    count = count - ((offsetX - (cFactor - 1)) / cFactor);
                    if (cFactor > 1) {
                        startX = (pix % cFactor);
                    }
                    else {
                        startX = 0;
                    }
                }

                if (endX >= offset) {
                    endX = offset - 1;
                }

                // Evan do we really need to recalc the norm all the time?
                // Also can (offsetY * offset) be precalced.
                // Other speedups ?
                for (int i = startX; i <= endX; i += cFactor) {
                    sum += kernelXBuffer[count] * imgBuffer[i + offsetYTimesOffset];

                    if (kernelXBuffer[count] >= 0) {
                        norm += kernelXBuffer[count];
                    } else {
                        norm -= kernelXBuffer[count];
                    }
                    
                    count++;
                    
                }

                tempImgBuffer[pix] = sum / norm;
                
            } else {
                tempImgBuffer[pix] = imgBuffer[pix];
            }
        }

        // y kernel dimensions
        xKDim = 1;
        yKDim = kernelYBuffer.length;
        halfxKDim = 0;
        halfyKDim = yKDim / 2;
        stepY = (yKDim - 1) * offset;

        // convolve the result image from above with the Y dimension kernel
        for (int pix = 0; (pix < imageLength) && !threadStopped; pix++, index++) {

            if (index % stepProgressValue == 0) {
                fireProgressStateChanged( progress++);
            }

            if (skipRed && ((pix % 4) == 1)) {
                destBuffer[pix] = tempImgBuffer[pix];
            } else if (skipGreen && ((pix % 4) == 2)) {
                destBuffer[pix] = tempImgBuffer[pix];
            } else if (skipBlue && ((pix % 4) == 3)) {
                destBuffer[pix] = tempImgBuffer[pix];
            } else if (entireImage || mask.get(pix / cFactor)) {
                offsetX = (pix % offset);
                offsetY = (pix / offset) - halfyKDim;

                sum = 0;
                int count = 0;
                norm = 0;
                startY = offsetY * offset;
                endY = startY + stepY;

                if (startY < 0) {
                    count = count - offsetY;
                    startY = 0;
                }

                if (endY > (offset * (yDim - 1))) {
                    endY = offset * (yDim - 1);
                }

                for (int i = startY; i <= endY; i += offset) {
                    sum += kernelYBuffer[count] * tempImgBuffer[offsetX + i];

                    if (kernelYBuffer[count] >= 0) {
                        norm += kernelYBuffer[count];
                    } else {
                        norm -= kernelYBuffer[count];
                    }

                    count++;
                }

                destBuffer[pix] = sum / norm;
            } else {
                destBuffer[pix] = tempImgBuffer[pix];
            }
        }
        
        fireProgressStateChanged(100);
        
        setCompleted(true);
    }

    /**
     * Begins the excution of the 3D convolver.
     */
    public void run3D() {
        double sum = 0;
        double norm = 0;
        int offsetX, offsetY, offsetZ;
        int start, end;

        int xDim = imgExtents[0];
        int yDim = imgExtents[1];
        int zDim = imgExtents[2];

        int kDim;
        int halfKDim;

        int size = imgBuffer.length;
        int sliceSize = xDim * yDim * cFactor;
        int step;

        int offset = xDim * cFactor;

        boolean skipRed = false;
        boolean skipGreen = false;
        boolean skipBlue = false;

        if (colorImage && !red) {
            skipRed = true;
        }

        if (colorImage && !green) {
            skipGreen = true;
        }

        if (colorImage && !blue) {
            skipBlue = true;
        }

        // x kernel dimensions
        kDim = kernelXBuffer.length;
        halfKDim = kDim / 2;

        // used for reducing repetitive calculations
        int combined = 0;

        int halfKDimTimesCFactor = halfKDim * cFactor;

        int stepProgressValue = (imgExtents[0] * imgExtents[1] * imgExtents[2] * cFactor * 3) /100;
        // convolve the image with the X dimension kernel
        int progress  = 0;
        int index = 0;
        for (int pix = 0; (pix < size) && !threadStopped; pix++, index++) {

            if (index % stepProgressValue == 0) {
                fireProgressStateChanged(progress++);
            }

            if (skipRed && ((pix % 4) == 1)) {
                tempImgBuffer[pix] = imgBuffer[pix];
            } else if (skipGreen && ((pix % 4) == 2)) {
                tempImgBuffer[pix] = imgBuffer[pix];
            } else if (skipBlue && ((pix % 4) == 3)) {
                tempImgBuffer[pix] = imgBuffer[pix];
            } else if (entireImage || mask.get(pix / cFactor)) {
                offsetX = (pix % offset) - halfKDimTimesCFactor;
                offsetY = (pix % sliceSize) / offset;
                offsetZ = (pix / sliceSize);

                combined = (offsetY * offset) + (offsetZ * sliceSize);

                int count = 0;
                sum = 0;
                norm = 0;
                start = offsetX;
                end = start + ((kDim - 1) * cFactor);

                if (start < 0) {
                    count = count - ((offsetX - (cFactor - 1)) / cFactor);
                    if (cFactor > 1) {
                        start = (pix % cFactor);
                    }
                    else {
                        start = 0;
                    }
                }

                if (end >= offset) {
                    end = offset - 1;
                }

                for (int i = start; i <= end; i += cFactor) {
                    sum += kernelXBuffer[count] * imgBuffer[i + combined];

                    if (kernelXBuffer[count] >= 0) {
                        norm += kernelXBuffer[count];
                    } else {
                        norm -= kernelXBuffer[count];
                    }

                    count++;
                }

                tempImgBuffer[pix] = sum / norm;
            } else {
                tempImgBuffer[pix] = imgBuffer[pix];
            }
        }

        // y kernel dimensions
        kDim = kernelYBuffer.length;
        halfKDim = kDim / 2;
        step = (kDim - 1) * offset;

        // convolve the result image from above with the Y dimension kernel
        for (int pix = 0; (pix < size) && !threadStopped; pix++, index++) {

            if (index % stepProgressValue == 0) {
                fireProgressStateChanged(progress++);
            }

            if (skipRed && ((pix % 4) == 1)) {
                imgBuffer[pix] = tempImgBuffer[pix];
            } else if (skipGreen && ((pix % 4) == 2)) {
                imgBuffer[pix] = tempImgBuffer[pix];
            } else if (skipBlue && ((pix % 4) == 3)) {
                imgBuffer[pix] = tempImgBuffer[pix];
            } else if (entireImage || mask.get(pix / cFactor)) {
                offsetX = (pix % offset);
                offsetY = ((pix % sliceSize) / offset) - halfKDim;
                offsetZ = (pix / sliceSize);

                combined = offsetX + (offsetZ * sliceSize);

                int count = 0;
                sum = 0;
                norm = 0;
                start = offsetY * offset;
                end = start + step;

                if (start < 0) {
                    count = count - offsetY;
                    start = 0;
                }

                if (end > (offset * (yDim - 1))) {
                    end = offset * (yDim - 1);
                }

                for (int i = start; i <= end; i += offset) {
                    sum += kernelYBuffer[count] * tempImgBuffer[i + combined];

                    if (kernelYBuffer[count] >= 0) {
                        norm += kernelYBuffer[count];
                    } else {
                        norm -= kernelYBuffer[count];
                    }

                    count++;
                }

                // use imgBuffer as a temp buffer since we won't need to use it again
                imgBuffer[pix] = sum / norm;
            } else {
                imgBuffer[pix] = tempImgBuffer[pix];
            }
        }

        // z kernel dimensions
        kDim = kernelZBuffer.length;
        halfKDim = kDim / 2;
        step = (kDim - 1) * sliceSize;

        for (int pix = 0; (pix < size) && !threadStopped; pix++, index++) {

            if (index % stepProgressValue == 0) {
                fireProgressStateChanged(progress++);
            }

            if (skipRed && ((pix % 4) == 1)) {
                destBuffer[pix] = imgBuffer[pix];
            } else if (skipGreen && ((pix % 4) == 2)) {
                destBuffer[pix] = imgBuffer[pix];
            } else if (skipBlue && ((pix % 4) == 3)) {
                destBuffer[pix] = imgBuffer[pix];
            } else if (entireImage || mask.get(pix / cFactor)) {
                offsetX = (pix % offset);
                offsetY = (pix % sliceSize) / offset;
                offsetZ = (pix / sliceSize) - halfKDim;

                combined = (offsetY * offset) + offsetX;

                int count = 0;
                sum = 0;
                norm = 0;
                start = offsetZ * sliceSize;
                end = start + step;

                if (start < 0) {
                    count = count - offsetZ;
                    start = 0;
                }

                if (end > (sliceSize * (zDim - 1))) {
                    end = sliceSize * (zDim - 1);
                }

                for (int i = start; i <= end; i += sliceSize) {

                    // imgBuffer now holds the result of convolving with X and Y kernels
                    sum += kernelZBuffer[count] * imgBuffer[i + combined];

                    if (kernelZBuffer[count] >= 0) {
                        norm += kernelZBuffer[count];
                    } else {
                        norm -= kernelZBuffer[count];
                    }

                    count++;
                }

                destBuffer[pix] = sum / norm;
            } else {

                // imgBuffer now holds the result of convolving with X and Y kernels
                destBuffer[pix] = imgBuffer[pix];
            }
        }
        
      
        setCompleted(true);
    }

    public void run3DMT() {
        int size = imgBuffer.length;
//      setProgressStep((imgExtents[0] * imgExtents[1] * imgExtents[2] * cFactor * 3) /100);
      setProgressStep(100.00f/(imgExtents[0] * imgExtents[1] * imgExtents[2] * cFactor * 3));
        // convolve the image with the X dimension kernel
        int index = 0;
        int nthreads = gov.nih.mipav.util.MipavUtil.getAvailableCores();
        final CountDownLatch doneSignalx = new CountDownLatch(nthreads);
        float step2 = size/nthreads;
        for(int i = 0; i < nthreads; i++){
            final int start2 = (int)(step2*i);
            final int end2 = (int)(step2*(i+1));
            Runnable task = new Runnable(){
                public void run(){
              	  convolveX(start2, end2);
              	  doneSignalx.countDown();
                }
            };
            gov.nih.mipav.util.MipavUtil.threadPool.execute(task);
        }
        try{
      	  doneSignalx.await();
        }catch(InterruptedException e){
      	  
        }

        // y kernel dimensions
        final CountDownLatch doneSignaly = new CountDownLatch(nthreads);
        for(int i = 0; i < nthreads; i++){
            final int start2 = (int)(step2*i);
            final int end2 = (int)(step2*(i+1));
            Runnable task = new Runnable(){
                public void run(){
              	  convolveY(start2, end2);
              	  doneSignaly.countDown();
                }
            };
            gov.nih.mipav.util.MipavUtil.threadPool.execute(task);
        }
        try{
      	  doneSignaly.await();
        }catch(InterruptedException e){
      	  
        }

        // z kernel dimensions
        final CountDownLatch doneSignalz = new CountDownLatch(nthreads);
        for(int i = 0; i < nthreads; i++){
            final int start2 = (int)(step2*i);
            final int end2 = (int)(step2*(i+1));
            Runnable task = new Runnable(){
                public void run(){
              	  convolveZ(start2, end2);
              	  doneSignalz.countDown();
                }
            };
            gov.nih.mipav.util.MipavUtil.threadPool.execute(task);
        }
        try{
      	  doneSignalz.await();
        }catch(InterruptedException e){
      	  
        }
        setCompleted(true);
    }

    private void convolveX(final int from, final int to){
        int xDim = imgExtents[0];
        int yDim = imgExtents[1];

    	int offset = xDim;
    	int sliceSize = xDim * yDim;
    	int offsetX, offsetY, offsetZ;
        int kDim = kernelXBuffer.length;
        int halfKDim = kDim / 2;
        int halfKDimTimesCFactor = halfKDim * cFactor;
        int combined, start, end;
		double sum, norm;
		int index = 0;
		float ps = getProgressStep();
		int period = (int)(1.0/getProgressStep());
		for (int pix = from; (pix < to) && !threadStopped; pix++) {
            if (colorImage && !red && ((pix % 4) == 1)) {
                destBuffer[pix] = imgBuffer[pix];
            } else if (colorImage && !green && ((pix % 4) == 2)) {
                destBuffer[pix] = imgBuffer[pix];
            } else if (colorImage && !blue && ((pix % 4) == 3)) {
                destBuffer[pix] = imgBuffer[pix];
            } else if (entireImage || mask.get(pix / cFactor)) {
				offsetX = (pix % offset) - halfKDimTimesCFactor;
				offsetY = (pix % sliceSize) / offset;
				offsetZ = (pix / sliceSize);

				combined = (offsetY * offset) + (offsetZ * sliceSize);

				int count = 0;
				sum = 0;
				norm = 0;
				start = offsetX;
				end = start + ((kDim - 1) * cFactor);

				if (start < 0) {
					count = count - ((offsetX - (cFactor - 1)) / cFactor);
					if (cFactor > 1) {
						start = (pix % cFactor);
					} else {
						start = 0;
					}
				}

				if (end >= offset) {
					end = offset - 1;
				}

				for (int i = start; i <= end; i += cFactor) {
					sum += kernelXBuffer[count] * imgBuffer[i + combined];

					if (kernelXBuffer[count] >= 0) {
						norm += kernelXBuffer[count];
					} else {
						norm -= kernelXBuffer[count];
					}

					count++;
				}

				destBuffer[pix] = sum / norm;
			}else{
				destBuffer[pix] = imgBuffer[pix];
			}
            index++;
            if(index % period == 0){
            	makeProgress(ps*period);
            	fireProgressStateChanged((int)getProgress());
            }
		}
	}
        
    private void convolveY(final int from, final int to){
        int xDim = imgExtents[0];
        int yDim = imgExtents[1];

    	int offset = xDim;
    	int sliceSize = xDim * yDim;
    	int offsetX, offsetY, offsetZ;
        int kDim = kernelYBuffer.length;
        int halfKDim = kDim / 2;
        int combined, start, end;
        int step = (kDim - 1) * offset;
		double sum, norm;
		int index = 0;
		float ps = getProgressStep();
		int period = (int)(1.0/getProgressStep());
        for (int pix = from; (pix < to) && !threadStopped; pix++) {
            if (colorImage && !red && ((pix % 4) == 1)) {
                destBuffer[pix] = imgBuffer[pix];
            } else if (colorImage && !green && ((pix % 4) == 2)) {
                destBuffer[pix] = imgBuffer[pix];
            } else if (colorImage && !blue && ((pix % 4) == 3)) {
                destBuffer[pix] = imgBuffer[pix];
            } else if (entireImage || mask.get(pix / cFactor)) {
                offsetX = (pix % offset);
                offsetY = ((pix % sliceSize) / offset) - halfKDim;
                offsetZ = (pix / sliceSize);

                combined = offsetX + (offsetZ * sliceSize);

                int count = 0;
                sum = 0;
                norm = 0;
                start = offsetY * offset;
                end = start + step;

                if (start < 0) {
                    count = count - offsetY;
                    start = 0;
                }

                if (end > (offset * (yDim - 1))) {
                    end = offset * (yDim - 1);
                }

                for (int i = start; i <= end; i += offset) {
                    sum += kernelYBuffer[count] * destBuffer[i + combined];

                    if (kernelYBuffer[count] >= 0) {
                        norm += kernelYBuffer[count];
                    } else {
                        norm -= kernelYBuffer[count];
                    }

                    count++;
                }

                // use imgBuffer as a temp buffer since we won't need to use it again
                imgBuffer[pix] = sum / norm;
            }else{
            	imgBuffer[pix] = destBuffer[pix];
            }
            index++;
            if(index % period == 0){
            	makeProgress(ps*period);
            	fireProgressStateChanged((int)getProgress());
            }
        }
	}
        
    private void convolveZ(final int from, final int to){
        int xDim = imgExtents[0];
        int yDim = imgExtents[1];
        int zDim = imgExtents[2];

    	int offset = xDim;
    	int sliceSize = xDim * yDim;
    	int offsetX, offsetY, offsetZ;
        int kDim = kernelZBuffer.length;
        int halfKDim = kDim / 2;
        int combined, start, end;
		double sum, norm;
		int step = (kDim - 1) * sliceSize;
		int index = 0;
		float ps = getProgressStep();
		int period = (int)(1.0/ps);

        for (int pix = from; (pix < to) && !threadStopped; pix++) {
            if (colorImage && !red && ((pix % 4) == 1)) {
                destBuffer[pix] = imgBuffer[pix];
            } else if (colorImage && !green && ((pix % 4) == 2)) {
                destBuffer[pix] = imgBuffer[pix];
            } else if (colorImage && !blue && ((pix % 4) == 3)) {
                destBuffer[pix] = imgBuffer[pix];
            } else if (entireImage || mask.get(pix / cFactor)) {
                offsetX = (pix % offset);
                offsetY = (pix % sliceSize) / offset;
                offsetZ = (pix / sliceSize) - halfKDim;

                combined = (offsetY * offset) + offsetX;

                int count = 0;
                sum = 0;
                norm = 0;
                start = offsetZ * sliceSize;
                end = start + step;

                if (start < 0) {
                    count = count - offsetZ;
                    start = 0;
                }

                if (end > (sliceSize * (zDim - 1))) {
                    end = sliceSize * (zDim - 1);
                }

                for (int i = start; i <= end; i += sliceSize) {

                    // imgBuffer now holds the result of convolving with X and Y kernels
                    sum += kernelZBuffer[count] * imgBuffer[i + combined];

                    if (kernelZBuffer[count] >= 0) {
                        norm += kernelZBuffer[count];
                    } else {
                        norm -= kernelZBuffer[count];
                    }

                    count++;
                }

                destBuffer[pix] = sum / norm;
            }else{
            	destBuffer[pix] = imgBuffer[pix];
            }
            index++;
            if(index % period == 0){
            	makeProgress(ps*period);
            	fireProgressStateChanged((int)getProgress());
            }
        }
	}
        
    /**
	 * Starts the convolution of the source image with the provided kernel.
	 */
    public void runAlgorithm() {
    	long start = System.nanoTime();
        if (imgExtents.length == 2) {
            run2D();
        } else if (imgExtents.length > 2) {
//            run3DMT();
            run3D();
        }

        if (destImage != null) {

            try {
                destImage.importData(0, destBuffer, true);
            } catch (IOException ioe) {
                MipavUtil.displayError("Unable to import data to destination image.");
            }

            destImage.notifyImageDisplayListeners(null, true);
        } else {

            // put results into destination buffer as floats
            for (int i = 0; i < destBuffer.length; i++) {
                floatDestBuffer[i] = (float) destBuffer[i];
            }
        }
        System.out.println("Time consumed: " + (System.nanoTime()-start));
    }

    /**
     * Sets what color channels to convolve and tells the convolver that it is working on a color image.
     *
     * @param  _red    process the red channel
     * @param  _green  process the green channel
     * @param  _blue   process the blue channel
     */
    public void setColorChannels(boolean _red, boolean _green, boolean _blue) {
        red = _red;
        green = _green;
        blue = _blue;
        colorImage = true;
    }

    /**
     * Sets the mask to convolve within.
     *
     * @param  newMask  mask to convolve within
     */
    public void setMask(BitSet newMask) {
        mask = newMask;
        entireImage = false;
    }
 
    /**
     * Convolves kernel with a 2D image - The convolution occurs if the kernel is completely or partially contained in
     * the image.
     */
    private void convolverSetup2D() {

        if (srcImage == null) {
            displayError("Source Image is null");

            return;
        }

        if (srcImage.getNDims() != 2) {
            displayError("Source Image is not 2D");

            return;
        }

        if (kernel == null) {
            displayError("Kernel image is null");

            return;
        }

        if (kernel.getNDims() != 2) {
            displayError("Kernel Image is not 2D");

            return;
        }

        if (destImage == null) {
            displayError("Destination Image is null");

            return;
        }

        if (destImage.getNDims() != 2) {
            displayError("Destination Image is not 2D");

            return;
        }

        try {
            int length = imgExtents[0] * imgExtents[1] * cFactor;

            imgBuffer = new double[length];
            srcImage.exportData(0, length, imgBuffer); // locks and releases lock

            tempImgBuffer = new double[length];

            destBuffer = new double[length];

            kernel.setLock();

            int i;

            length = kernel.getExtents()[0];
            kernelXBuffer = new float[length];

            for (i = 0; i < length; i++) {
                kernelXBuffer[i] = kernel.getFloat(i);
            }

            length = kernel.getExtents()[1];
            kernelYBuffer = new float[length];

            for (i = 0; i < length; i++) {
                kernelYBuffer[i] = kernel.getFloat(i * kernel.getExtents()[0]);
            }

            kernel.releaseLock();
        } catch (IOException error) {
            displayError("Algorithm Convolver: Image(s) locked");
            setCompleted(false);

            return;
        } catch (OutOfMemoryError e) {
            displayError("Algorithm Convolver: Out of memory");
            setCompleted(false);

            return;
        }
    }

    /**
     * Convolving a kernel with a 3D image - The convolution occurs if the kernel is completely or partially contained
     * in the image.
     */
    private void convolverSetup3D() {

        if (srcImage == null) {
            displayError("Source Image is null");

            return;
        }

        if (srcImage.getNDims() != 3) {
            displayError("Source Image is not 3D");

            return;
        }

        if (kernel == null) {
            displayError("Kernel image is null");

            return;
        }

        if (kernel.getNDims() != 3) {
            displayError("Kernel Image is not 3D");

            return;
        }

        if (destImage == null) {
            displayError("Destination Image is null");

            return;
        }

        if (destImage.getNDims() != 3) {
            displayError("Destination Image is not 3D");

            return;
        }

        try {
            int length = imgExtents[0] * imgExtents[1] * imgExtents[2] * cFactor;

            imgBuffer = new double[length];
            srcImage.exportData(0, length, imgBuffer); // locks and releases lock

            tempImgBuffer = new double[length];

            destBuffer = new double[length];

            kernel.setLock();

            int i;

            length = kernel.getExtents()[0];
            kernelXBuffer = new float[length];

            for (i = 0; i < length; i++) {
                kernelXBuffer[i] = kernel.getFloat(i);
            }

            length = kernel.getExtents()[1];
            kernelYBuffer = new float[length];

            for (i = 0; i < length; i++) {
                kernelYBuffer[i] = kernel.getFloat(i * kernel.getExtents()[0]);
            }

            length = kernel.getExtents()[2];
            kernelZBuffer = new float[length];

            for (i = 0; i < length; i++) {
                kernelZBuffer[i] = kernel.getFloat(i * kernel.getExtents()[0] * kernel.getExtents()[1]);
            }

            kernel.releaseLock();
        } catch (IOException error) {
            displayError("Algorithm Convolver: Image(s) locked");
            setCompleted(false);

            return;
        } catch (OutOfMemoryError e) {
            displayError("Algorithm Convolver: Out of memory");
            setCompleted(false);

            return;
        }
    }
}
