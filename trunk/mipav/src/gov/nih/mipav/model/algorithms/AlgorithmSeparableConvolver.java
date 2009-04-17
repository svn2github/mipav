package gov.nih.mipav.model.algorithms;


import gov.nih.mipav.util.MipavUtil;

import java.util.BitSet;
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
    private double[] outputBuffer;

    /** Whether to convolve the whole image or just pixels inside a mask. */
    private boolean entireImage = true;

    /** Holds the original image data. */
    private double[] inputBuffer;

    /** The dimensions of the both source and destination images. */
    private int[] imgExtents;

    /** The dimensions of the kernel. */
    private int[] kernelExtents;

    private float[][] kernelBuffer;
    /** Flags to indicate which color channels to process. */
    private boolean red = true, green = true, blue = true;
    
    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Sets destination, source, and kernel buffers. Call run() to convolve image.
     *
     * @param  srcBuffer   source image data buffer
     * @param  iExtents    source and destination image dimensions
     * @param  kernBuffer  kernel image data buffer (kernel must be symmetric)
     * @param  kExtents    kernel dimensions
     * @param  color       whether the image is color
     * @param  minProgressValue the minimum progress value.
     * @param  maxProgressValue the maximum progress value.
     */
    public AlgorithmSeparableConvolver(float[] srcBuffer, int[] iExtents, float[][] kernBuffer, 
            boolean colorImage) {
        super(null, null);
        this.inputBuffer = new double[srcBuffer.length];
        MipavUtil.arrayCopy(srcBuffer, 0, inputBuffer, 0, srcBuffer.length);

        if(multiThreadingEnabled){
            this.outputBuffer = new double[inputBuffer.length];
        } else {
            this.outputBuffer = this.inputBuffer;
        }

        this.imgExtents = iExtents;
        this.kernelBuffer = kernBuffer;
        kernelExtents = new int[kernBuffer.length];
        for(int i = 0; i < kernelBuffer.length; i++){
            kernelExtents[i] = kernelBuffer[i].length;
        }
        this.colorImage = colorImage;

        if (colorImage) {
            cFactor = 4;
        }
    }


    public AlgorithmSeparableConvolver(float[] srcBuffer, int[] iExtents, float[] kernBuffers, int[] kExtents, 
    		boolean colorImage) {
        super(null, null);
			this.inputBuffer = new double[srcBuffer.length];
			for(int i = 0; i < srcBuffer.length; i++){
			    inputBuffer[i] = srcBuffer[i];
			}
//			System.arraycopy(srcBuffer, 0, inputBuffer, 0, srcBuffer.length);

		if(multiThreadingEnabled){
			this.outputBuffer = new double[inputBuffer.length];
		} else {
			this.outputBuffer = this.inputBuffer;
		}


        this.imgExtents = iExtents;
        this.kernelBuffer = new float[kExtents.length][];
        this.kernelExtents = kExtents;

        int interval = 1;
        for(int i = 0; i < kernelExtents.length; i++){
        	kernelBuffer[i] = new float[kernelExtents[i]];
        	for(int j = 0; j < kernelExtents[i]; j++){
        		kernelBuffer[i][j] = kernBuffers[j*interval];
        	}
        	interval *= kernelExtents[i];
        }
        this.colorImage = colorImage;

        if (colorImage) {
            cFactor = 4;
        }
    }


    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Prepares this class for destruction.
     */
    public void finalize() {
        inputBuffer = null;
        imgExtents = null;
        kernelExtents = null;
        kernelBuffer = null;
        outputBuffer = null;

        super.finalize();
    }

    /**
     * Perform one-dimension convolution.
     * @param imageBuffer
     * @param kernelBuffer
     * @param resultBuffer
     */
    private void convolve(double[] imageBuffer, float[] kernelBuffer, double[] resultBuffer){
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
    	int kernelDim = kernelBuffer.length;
    	int halfKernelDimTimesCFactor = kernelDim/2 * cFactor;
    	for (int i = 0; i < imageBuffer.length; i++) {
			if (skipRed && ((i % 4) == 1)) {
				resultBuffer[i] = imageBuffer[i];
			} else if (skipGreen && ((i % 4) == 2)) {
				resultBuffer[i] = imageBuffer[i];
			} else if (skipBlue && ((i % 4) == 3)) {
				resultBuffer[i] = imageBuffer[i];
			} else if (entireImage || mask.get(i / cFactor)) {
				int count = 0;
				double sum = 0;
				double norm = 0;
				int start = i - halfKernelDimTimesCFactor;
				int end = start + kernelDim * cFactor;
				if (start < 0) {
					count = count - ((start - (cFactor - 1)) / cFactor);
					if (cFactor > 1) {
						start = i % cFactor;
					} else {
						start = 0;
					}
				}
				if (end > imageBuffer.length) {
					end = imageBuffer.length;
				}
				for (int j = start; j < end; j += cFactor) {
					sum += kernelBuffer[count] * imageBuffer[j];
					if (kernelBuffer[count] > 0) {
						norm += kernelBuffer[count];
					} else {
						norm -= kernelBuffer[count];
					}
					count++;
				}
				resultBuffer[i] = sum / norm;
			}else{
				resultBuffer[i] = imageBuffer[i];
			}
    	}
	}
    
    public void performMT(){
        int size = inputBuffer.length;
        if(imgExtents.length < 3 || kernelBuffer.length < 3){
        	setProgressStep((imgExtents[0] * imgExtents[1] * cFactor * 2)/(maxProgressValue-minProgressValue));
        }else{
            setProgressStep((imgExtents[0] * imgExtents[1] * imgExtents[2] * cFactor * 3)/(maxProgressValue-minProgressValue));
        }
        
        progress = minProgressValue;

        // convolve the image with the X dimension kernel
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
            gov.nih.mipav.util.MipavUtil.mipavThreadPool.execute(task);
        }
        try{
      	  doneSignalx.await();
        }catch(InterruptedException e){
      	  gov.nih.mipav.view.MipavUtil.displayError(e.getMessage());
      	  return;
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
            gov.nih.mipav.util.MipavUtil.mipavThreadPool.execute(task);
        }
        try{
      	  doneSignaly.await();
        }catch(InterruptedException e){
            gov.nih.mipav.view.MipavUtil.displayError(e.getMessage());
            return;
        }

        if(imgExtents.length < 3 || kernelBuffer.length < 3){
        	System.arraycopy(inputBuffer, 0, outputBuffer, 0, inputBuffer.length);
        	return;
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
            gov.nih.mipav.util.MipavUtil.mipavThreadPool.execute(task);
        }
        try{
      	  doneSignalz.await();
        }catch(InterruptedException e){
            gov.nih.mipav.view.MipavUtil.displayError(e.getMessage());
            return;      	  
        }
    }
    
    /**
	 * Begins the excution of the 3D convolver.
	 */
    public void perform() {
    	int xDim = imgExtents[0];
    	int yDim = imgExtents[1];
    	int zDim = (imgExtents.length == 2)?1:imgExtents[2];

        int xoffset = xDim * cFactor;
        int yoffset = yDim * cFactor;
        int zoffset = zDim * cFactor;
        
        if(zDim == 1 || kernelBuffer.length == 2){
        	progressStep = (xDim*yDim) / (maxProgressValue-minProgressValue);
        }else{
        	progressStep = (yDim*zDim + xDim*zDim + xDim*yDim) / (maxProgressValue-minProgressValue);
        }

        progress = minProgressValue;
        double[] xrowBuffer = new double[xoffset];
        double[] xresultBuffer = new double[xoffset];
        // Convolve the image with the X dimension kernel
        for (int row = 0; (row < yDim*zDim) && !threadStopped; row++) {

            if (row % progressStep == 0) {
                fireProgressStateChanged((int)progress++);
            }
            System.arraycopy(inputBuffer, row*xoffset, xrowBuffer, 0, xoffset);
            convolve(xrowBuffer, kernelBuffer[0], xresultBuffer);
            System.arraycopy(xresultBuffer, 0, inputBuffer, row*xoffset, xoffset);
        }

        // Convolve the result image from above with the Y dimension kernel
        double[] yrowBuffer = new double[yoffset*cFactor];
        double[] yresultBuffer = new double[yoffset*cFactor];
	     for ( int row = 0; (row < zDim * xDim ) && !threadStopped; row++ ) {
			if (row % progressStep == 0) {
				fireProgressStateChanged((int)progress++);
			}
			MipavUtil.rowCopy(inputBuffer, row, yrowBuffer, 0, yoffset, xoffset, cFactor);
			convolve(yrowBuffer, kernelBuffer[1], yresultBuffer);
			MipavUtil.rowCopy(yresultBuffer, 0, inputBuffer, row, yoffset, cFactor, xoffset);
	    }
		
        // Convolve the result image from above with the Z dimension kernel
        if (zDim > 1 && kernelBuffer.length > 2) {
            double[] zrowBuffer = new double[zoffset*cFactor];
            double[] zresultBuffer = new double[zoffset*cFactor];
			for (int row = 0; (row < xDim * yDim) && !threadStopped; row++) {
				if (row % progressStep == 0) {
					fireProgressStateChanged((int)progress++);
				}
				MipavUtil.rowCopy(inputBuffer, row, zrowBuffer, 0, zoffset, yoffset, cFactor);
				convolve(zrowBuffer, kernelBuffer[2], zresultBuffer);
				MipavUtil.rowCopy(zresultBuffer, 0, inputBuffer, row, zoffset, cFactor, yoffset);
			}
		}
    }

    private void convolveX(final int from, final int to){
        int xDim = imgExtents[0];
        int yDim = imgExtents[1];

    	int offset = xDim * cFactor;
    	int sliceSize = xDim * yDim * cFactor;
    	int offsetX, offsetY, offsetZ;
        int kDim = kernelBuffer[0].length;
        int halfKDim = kDim / 2;
        int halfKDimTimesCFactor = halfKDim * cFactor;
        int combined, start, end;
		double sum, norm;
		int index = 0;
		for (int pix = from; (pix < to) && !threadStopped; pix++) {
            if (colorImage && !red && ((pix % 4) == 1)) {
                outputBuffer[pix] = inputBuffer[pix];
            } else if (colorImage && !green && ((pix % 4) == 2)) {
                outputBuffer[pix] = inputBuffer[pix];
            } else if (colorImage && !blue && ((pix % 4) == 3)) {
                outputBuffer[pix] = inputBuffer[pix];
            } else if (entireImage || mask.get(pix / cFactor)) {
				offsetX = (pix % offset) - halfKDimTimesCFactor;
				offsetY = (pix % sliceSize) / offset;
				offsetZ = (pix / sliceSize);

				combined = (offsetY * offset) + (offsetZ * sliceSize);

				int count = 0;
				sum = 0;
				norm = 0;
				start = offsetX;
				end = start + (kDim * cFactor);

				if (start < 0) {
					count =  -((offsetX - (cFactor - 1))/ cFactor);
					if (cFactor > 1) {
						start = (pix % cFactor);
					} else {
						start = 0;
					}
				}

				if (end > offset) {
					end = offset;
				}

				for (int i = start; i < end; i += cFactor) {
					sum += kernelBuffer[0][count] * inputBuffer[i + combined];

					if (kernelBuffer[0][count] >= 0) {
						norm += kernelBuffer[0][count];
					} else {
						norm -= kernelBuffer[0][count];
					}

					count++;
				}

				outputBuffer[pix] = sum / norm;
			}else{
				outputBuffer[pix] = inputBuffer[pix];
			}
            index++;
            if(index % progressStep == 0){
            	makeProgress(1);
            	fireProgressStateChanged((int)getProgress());
            }
		}
	}
        
    private void convolveY(final int from, final int to){
        int xDim = imgExtents[0];
        int yDim = imgExtents[1];

    	int offset = xDim * cFactor;
    	int sliceSize = xDim * yDim * cFactor;
    	int offsetX, offsetY, offsetZ;
        int kDim = kernelBuffer[1].length;
        int halfKDim = kDim / 2;
        int combined, start, end;
        int step = kDim * offset;
		double sum, norm;
		int index = 0;
        for (int pix = from; (pix < to) && !threadStopped; pix++) {
            if (colorImage && !red && ((pix % 4) == 1)) {
            	inputBuffer[pix] = outputBuffer[pix];
            } else if (colorImage && !green && ((pix % 4) == 2)) {
            	inputBuffer[pix] = outputBuffer[pix];
            } else if (colorImage && !blue && ((pix % 4) == 3)) {
            	inputBuffer[pix] = outputBuffer[pix];
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
                    count = -offsetY;
                    start = 0;
                }

                if (end > (offset * yDim )) {
                    end = offset * yDim;
                }

                for (int i = start; i < end; i += offset) {
                    sum += kernelBuffer[1][count] * outputBuffer[i + combined];

                    if (kernelBuffer[1][count] >= 0) {
                        norm += kernelBuffer[1][count];
                    } else {
                        norm -= kernelBuffer[1][count];
                    }

                    count++;
                }

                // use imgBuffer as a temp buffer since we won't need to use it again
                inputBuffer[pix] = sum / norm;
            }else{
            	inputBuffer[pix] = outputBuffer[pix];
            }
            index++;
            if(index % progressStep == 0){
            	makeProgress(1);
            	fireProgressStateChanged((int)getProgress());
            }
        }
	}
        
    private void convolveZ(final int from, final int to){
        int xDim = imgExtents[0];
        int yDim = imgExtents[1];
        int zDim = imgExtents[2];

    	int offset = xDim * cFactor;
    	int sliceSize = xDim * yDim * cFactor;
    	int offsetX, offsetY, offsetZ;
        int kDim = kernelBuffer[2].length;
        int halfKDim = kDim / 2;
        int combined, start, end;
		double sum, norm;
		int step = kDim * sliceSize;
		int index = 0;

        for (int pix = from; (pix < to) && !threadStopped; pix++) {
            if (colorImage && !red && ((pix % 4) == 1)) {
                outputBuffer[pix] = inputBuffer[pix];
            } else if (colorImage && !green && ((pix % 4) == 2)) {
                outputBuffer[pix] = inputBuffer[pix];
            } else if (colorImage && !blue && ((pix % 4) == 3)) {
                outputBuffer[pix] = inputBuffer[pix];
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
                    count = -offsetZ;
                    start = 0;
                }

                if (end > (sliceSize * zDim)) {
                    end = sliceSize * zDim;
                }

                for (int i = start; i < end; i += sliceSize) {

                    // imgBuffer now holds the result of convolving with X and Y kernels
                    sum += kernelBuffer[2][count] * inputBuffer[i + combined];

                    if (kernelBuffer[2][count] >= 0) {
                        norm += kernelBuffer[2][count];
                    } else {
                        norm -= kernelBuffer[2][count];
                    }

                    count++;
                }

                outputBuffer[pix] = sum / norm;
            }else{
            	outputBuffer[pix] = inputBuffer[pix];
            }
            index++;
            if(index % progressStep == 0){
            	makeProgress(1);
            	fireProgressStateChanged((int)getProgress());
            }
        }
	}
       
    public void beforeExecute(){
    	fireProgressStateChanged(minProgressValue);
    }
    
    public void execute(){
    	if(multiThreadingEnabled){
    		performMT();
    	}else{
    		perform();
    	}
    }
    
    public void afterExecute(){
        fireProgressStateChanged(maxProgressValue);        
        setCompleted(true);
    }
    
    /**
	 * Starts the convolution of the source image with the provided kernel.
	 */
    public void runAlgorithm() {
    	long start = System.currentTimeMillis();
    	beforeExecute();
    	execute();
    	afterExecute();
        System.out.println("Time consumed: " + (System.currentTimeMillis()-start));
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
 
    public float[] getOutputBuffer(){
        float[] buffer = new float[outputBuffer.length];
        MipavUtil.arrayCopy(outputBuffer, 0, buffer, 0, outputBuffer.length);
        return buffer;
    }
}
