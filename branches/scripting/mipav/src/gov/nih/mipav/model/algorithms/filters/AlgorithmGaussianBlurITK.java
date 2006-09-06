package gov.nih.mipav.model.algorithms.filters;


import gov.nih.mipav.model.algorithms.*;
import gov.nih.mipav.model.structures.*;

import InsightToolkit.*;

import java.io.*;


/**
 * Implementation of a Gaussian blur using functionality provided by the InsightToolkit library (www.itk.org).
 *
 * <p>The application of this algorithm blurs an image or VOI region of the image with a Gaussian function at a user
 * defined scale (sigma - standard deviation). In essence, convolving a Gaussian function produces the same result as a
 * low-pass or smoothing filter. A low-pass filter attenuates high frequency components of the image (i.e. edges) and
 * passes low frequency components and thus results in the blurring of the image. Smoothing filters are typically used
 * for noise reduction and for blurring. The standard deviation (SD) of the Gaussian function controls the amount of
 * blurring:a large SD (i.e. > 2) significantly blurs while a small SD (i.e. 0.5) blurs less. If the objective is to
 * achieve noise reduction, a rank filter (median) might be more useful.</p>
 *
 * <p>1D Gaussian = (1/sqrt(2*PI*sigma*sigma))*exp(-x*x/(2*sigma*sigma));</p>
 *
 * <p>Advantages to convolving the Gaussian function to blur an image include:</p>
 *
 * <p>1. Structure will not be added to the image. 2. Can be analytically calculated, as well as the Fourier Transform
 * of the Gaussian. 3. By varying the SD a Gaussian scale-space can easily be constructed.</p>
 */
public class AlgorithmGaussianBlurITK extends AlgorithmBase {

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** Flags indicate which color channel to process. True indicates the channel should be processed. */
    private boolean[] abProcessChannel = new boolean[] {
                                             false, // alpha
                                             true, // red
                                             true, // green
                                             true // blue
                                         };

    /** Dimensionality of the kernel. */
    private int[] kExtents;

    /** Standard deviations of the gaussian used to calculate the kernels. */
    private float[] sigmas;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates a new AlgorithmGaussianBlurITK object.
     *
     * @param  srcImg  source image model
     * @param  sigmas  Gaussian's standard deviations in the each dimension
     * @param  img25D  Flag, if true, indicates that each slice of the 3D volume should be processed independently. 2D
     *                 images disregard this flag.
     */
    public AlgorithmGaussianBlurITK(ModelImage srcImg, float[] sigmas, boolean img25D, 
            int minProgressValue, int maxProgressValue) {
        super(null, srcImg, minProgressValue, maxProgressValue);

        this.sigmas = sigmas;
        image25D = img25D;
    }

    /**
     * Creates a new AlgorithmGaussianBlurITK object.
     *
     * @param  destImg  image model where result image is to stored
     * @param  srcImg   source image model
     * @param  sigmas   Gaussian's standard deviations in the each dimension
     * @param  img25D   Flag, if true, indicates that each slice of the 3D volume should be processed independently. 2D
     *                  images disregard this flag.
     */
    public AlgorithmGaussianBlurITK(ModelImage destImg, ModelImage srcImg, float[] sigmas, boolean img25D, 
            int minProgressValue, int maxProgressValue) {
        super(destImg, srcImg, minProgressValue, maxProgressValue);

        this.sigmas = sigmas;
        image25D = img25D;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Prepares this class for destruction.
     */
    public void finalize() {
        destImage = null;
        srcImage = null;
        kExtents = null;
        sigmas = null;
        abProcessChannel = null;
        super.finalize();
    }

    /**
     * Starts the program.
     */
    public void runAlgorithm() {

        if (srcImage == null) {
            displayError("Source Image is null");
            finalize();

            return;
        }

        if (threadStopped) {
            finalize();

            return;
        }

        if ((srcImage.getNDims() == 3) && image25D) {
            fireProgressStateChanged(minProgressValue, null, "Blurring slices ...");
        } else {
            fireProgressStateChanged(minProgressValue, null, "Blurring ...");
        }

        

        // Make note of the sample spacing.  The ITK image created will
        // already have the sample spacing applied to it, but Gaussian
        // is to be applied in sample space not in real space.
        float[] afResolutions = srcImage.getFileInfo(0).getResolutions();

        // 2D or 2.5D
        if ((2 == srcImage.getNDims()) || ((3 == srcImage.getNDims()) && image25D)) {

            itkRecursiveGaussianImageFilterF2F2_Pointer kFilterX = itkRecursiveGaussianImageFilterF2F2.itkRecursiveGaussianImageFilterF2F2_New();
            itkRecursiveGaussianImageFilterF2F2_Pointer kFilterY = itkRecursiveGaussianImageFilterF2F2.itkRecursiveGaussianImageFilterF2F2_New();

            kFilterX.SetDirection(0);
            kFilterY.SetDirection(1);

            kFilterX.SetZeroOrder();
            kFilterY.SetZeroOrder();

            kFilterX.SetSigma(sigmas[0] * afResolutions[0]);
            kFilterY.SetSigma(sigmas[1] * afResolutions[1]);

            kFilterY.SetInput(kFilterX.GetOutput());

            itkImageToImageFilterF2F2 kFilterIn = kFilterX.GetPointer();
            itkImageToImageFilterF2F2 kFilterOut = kFilterY.GetPointer();

            kFilterX = null;
            kFilterY = null;

            // Color 2D
            if ((2 == srcImage.getNDims()) && srcImage.isColorImage()) {

                // store result in target image
                if (null != destImage) {

                    try {
                        destImage.setLock(ModelStorageBase.RW_LOCKED);
                    } catch (IOException error) {
                        errorCleanUp("GaussianBlurITK: Image(s) locked", false);

                        return;
                    }

                    for (int iChannel = 0; iChannel < 4; iChannel++) {
                        itkImageF2 kImageSrcITK = InsightToolkitSupport.itkCreateImageColor2D(srcImage, iChannel);

                        // filter channel and write result to target image
                        if (abProcessChannel[iChannel]) {
                            kFilterIn.SetInput(kImageSrcITK);
                            kFilterOut.Update();

                            InsightToolkitSupport.itkTransferImageColor2D(kImageSrcITK, kFilterOut.GetOutput(), mask,
                                                                          destImage, iChannel);
                        }
                        // just copy channel from source to target image
                        else {
                            InsightToolkitSupport.itkTransferImageColor2D(kImageSrcITK, kImageSrcITK, mask, destImage,
                                                                          iChannel);
                        }
                    }

                    destImage.releaseLock();

                }

                // store result back into the source image
                else {

                    for (int iChannel = 0; iChannel < 4; iChannel++) {

                        if (abProcessChannel[iChannel]) {
                            itkImageF2 kImageSrcITK = InsightToolkitSupport.itkCreateImageColor2D(srcImage, iChannel);
                            kFilterIn.SetInput(kImageSrcITK);
                            kFilterOut.Update();

                            InsightToolkitSupport.itkTransferImageColor2D(kImageSrcITK, kFilterOut.GetOutput(), mask,
                                                                          srcImage, iChannel);
                        }
                    }
                }
            }


            // Single channel 2D
            else if ((2 == srcImage.getNDims()) && !srcImage.isColorImage()) {
                itkImageF2 kImageSrcITK = InsightToolkitSupport.itkCreateImageSingle2D(srcImage);
                kFilterIn.SetInput(kImageSrcITK);
                kFilterOut.Update();

                // store result in target image
                if (null != destImage) {

                    try {
                        destImage.setLock(ModelStorageBase.RW_LOCKED);
                    } catch (IOException error) {
                        errorCleanUp("GaussianBlurITK: Image(s) locked", false);

                        return;
                    }

                    InsightToolkitSupport.itkTransferImageSingle2D(kImageSrcITK, kFilterOut.GetOutput(), mask,
                                                                   destImage);

                    destImage.releaseLock();
                }

                // store result back in source image
                else {
                    InsightToolkitSupport.itkTransferImageSingle2D(kImageSrcITK, kFilterOut.GetOutput(), mask,
                                                                   srcImage);
                }
            }

            // Color 2.5D
            else if (image25D && srcImage.isColorImage()) {

                // store result in target image
                if (null != destImage) {

                    try {
                        destImage.setLock(ModelStorageBase.RW_LOCKED);
                    } catch (IOException error) {
                        errorCleanUp("GaussianBlurITK: Image(s) locked", false);

                        return;
                    }

                    int iNumSlices = srcImage.getExtents()[2];

                    for (int iSlice = 0; iSlice < iNumSlices; iSlice++) {

                        fireProgressStateChanged(getProgressFromFloat((float) (iSlice + 1) / iNumSlices), null, null);
                       
                        if (threadStopped) {

                            finalize();

                            return;
                        }

                        for (int iChannel = 0; iChannel < 4; iChannel++) {
                            itkImageF2 kImageSrcITK = InsightToolkitSupport.itkCreateImageColorSlice(srcImage, iSlice,
                                                                                                     iChannel);

                            // filter channel and write result to target image
                            if (abProcessChannel[iChannel]) {
                                kFilterIn.SetInput(kImageSrcITK);
                                kFilterOut.Update();

                                InsightToolkitSupport.itkTransferImageColorSlice(kImageSrcITK, kFilterOut.GetOutput(),
                                                                                 mask, destImage, iSlice, iChannel);
                            }
                            // just copy channel from source to target image
                            else {
                                InsightToolkitSupport.itkTransferImageColorSlice(kImageSrcITK, kImageSrcITK, mask,
                                                                                 destImage, iSlice, iChannel);
                            }
                        }
                    }

                    destImage.releaseLock();
                }

                // store result back into the source image
                else {
                    int iNumSlices = srcImage.getExtents()[2];

                    for (int iSlice = 0; iSlice < iNumSlices; iSlice++) {

                        fireProgressStateChanged(getProgressFromFloat((float) (iSlice + 1) / iNumSlices), null, null);
                        
                        if (threadStopped) {

                            finalize();

                            return;
                        }

                        for (int iChannel = 0; iChannel < 4; iChannel++) {

                            if (abProcessChannel[iChannel]) {
                                itkImageF2 kImageSrcITK = InsightToolkitSupport.itkCreateImageColorSlice(srcImage,
                                                                                                         iSlice,
                                                                                                         iChannel);
                                kFilterIn.SetInput(kImageSrcITK);
                                kFilterOut.Update();

                                InsightToolkitSupport.itkTransferImageColorSlice(kImageSrcITK, kFilterOut.GetOutput(),
                                                                                 mask, srcImage, iSlice, iChannel);
                            }
                        }
                    }
                }
            }

            // Single channel 2.5D
            else if (image25D && !srcImage.isColorImage()) {

                // store result in target image
                if (null != destImage) {

                    try {
                        destImage.setLock(ModelStorageBase.RW_LOCKED);
                    } catch (IOException error) {
                        errorCleanUp("GaussianBlurITK: Image(s) locked", false);

                        return;
                    }

                    int iNumSlices = srcImage.getExtents()[2];

                    for (int iSlice = 0; iSlice < iNumSlices; iSlice++) {

                        fireProgressStateChanged(getProgressFromFloat((float) (iSlice + 1) / iNumSlices), null, null);
                        

                        if (threadStopped) {

                            finalize();

                            return;
                        }

                        itkImageF2 kImageSrcITK = InsightToolkitSupport.itkCreateImageSingleSlice(srcImage, iSlice);
                        kFilterIn.SetInput(kImageSrcITK);
                        kFilterOut.Update();

                        InsightToolkitSupport.itkTransferImageSingleSlice(kImageSrcITK, kFilterOut.GetOutput(), mask,
                                                                          destImage, iSlice);
                    }

                    destImage.releaseLock();
                }

                // store result back into the source image
                else {

                    int iNumSlices = srcImage.getExtents()[2];

                    for (int iSlice = 0; iSlice < iNumSlices; iSlice++) {

                        fireProgressStateChanged(getProgressFromFloat((float) (iSlice + 1) / iNumSlices), null, null);
                        

                        if (threadStopped) {

                            finalize();

                            return;
                        }

                        itkImageF2 kImageSrcITK = InsightToolkitSupport.itkCreateImageSingleSlice(srcImage, iSlice);
                        kFilterIn.SetInput(kImageSrcITK);
                        kFilterOut.Update();

                        InsightToolkitSupport.itkTransferImageSingleSlice(kImageSrcITK, kFilterOut.GetOutput(), mask,
                                                                          srcImage, iSlice);
                    }

                }
            }
        }

        // 3D
        else if ((3 == srcImage.getNDims()) && !image25D) {

            itkRecursiveGaussianImageFilterF3F3_Pointer kFilterX = itkRecursiveGaussianImageFilterF3F3.itkRecursiveGaussianImageFilterF3F3_New();
            itkRecursiveGaussianImageFilterF3F3_Pointer kFilterY = itkRecursiveGaussianImageFilterF3F3.itkRecursiveGaussianImageFilterF3F3_New();
            itkRecursiveGaussianImageFilterF3F3_Pointer kFilterZ = itkRecursiveGaussianImageFilterF3F3.itkRecursiveGaussianImageFilterF3F3_New();

            kFilterX.SetDirection(0);
            kFilterY.SetDirection(1);
            kFilterZ.SetDirection(2);

            kFilterX.SetZeroOrder();
            kFilterY.SetZeroOrder();
            kFilterZ.SetZeroOrder();

            kFilterX.SetSigma(sigmas[0] * afResolutions[0]);
            kFilterY.SetSigma(sigmas[1] * afResolutions[1]);
            kFilterZ.SetSigma(sigmas[2] * afResolutions[2]);

            kFilterY.SetInput(kFilterX.GetOutput());
            kFilterZ.SetInput(kFilterY.GetOutput());

            itkImageToImageFilterF3F3 kFilterIn = kFilterX.GetPointer();
            itkImageToImageFilterF3F3 kFilterOut = kFilterZ.GetPointer();

            kFilterX = null;
            kFilterY = null;
            kFilterZ = null;

            // Color 3D
            if (srcImage.isColorImage()) {

                // store result in target image
                if (null != destImage) {

                    try {
                        destImage.setLock(ModelStorageBase.RW_LOCKED);
                    } catch (IOException error) {
                        errorCleanUp("GaussianBlurITK: Image(s) locked", false);

                        return;
                    }

                    for (int iChannel = 0; iChannel < 4; iChannel++) {
                        itkImageF3 kImageSrcITK = InsightToolkitSupport.itkCreateImageColor3D(srcImage, iChannel);

                        // filter channel and write result to target image
                        if (abProcessChannel[iChannel]) {
                            kFilterIn.SetInput(kImageSrcITK);
                            kFilterOut.Update();

                            InsightToolkitSupport.itkTransferImageColor3D(kImageSrcITK, kFilterOut.GetOutput(), mask,
                                                                          destImage, iChannel);
                        }
                        // just copy channel from source to target image
                        else {
                            InsightToolkitSupport.itkTransferImageColor3D(kImageSrcITK, kImageSrcITK, mask, destImage,
                                                                          iChannel);
                        }
                    }

                    destImage.releaseLock();

                }

                // store result back into the source image
                else {

                    for (int iChannel = 0; iChannel < 4; iChannel++) {

                        if (abProcessChannel[iChannel]) {
                            itkImageF3 kImageSrcITK = InsightToolkitSupport.itkCreateImageColor3D(srcImage, iChannel);
                            kFilterIn.SetInput(kImageSrcITK);
                            kFilterOut.Update();

                            InsightToolkitSupport.itkTransferImageColor3D(kImageSrcITK, kFilterOut.GetOutput(), mask,
                                                                          srcImage, iChannel);
                        }
                    }
                }
            }

            // Single channel 3D
            else {
                itkImageF3 kImageSrcITK = InsightToolkitSupport.itkCreateImageSingle3D(srcImage);
                kFilterIn.SetInput(kImageSrcITK);
                kFilterOut.Update();

                // store result in target image
                if (null != destImage) {

                    try {
                        destImage.setLock(ModelStorageBase.RW_LOCKED);
                    } catch (IOException error) {
                        errorCleanUp("GaussianBlurITK: Image(s) locked", false);

                        return;
                    }

                    InsightToolkitSupport.itkTransferImageSingle3D(kImageSrcITK, kFilterOut.GetOutput(), mask,
                                                                   destImage);

                    destImage.releaseLock();
                }

                // store result back in source image
                else {
                    InsightToolkitSupport.itkTransferImageSingle3D(kImageSrcITK, kFilterOut.GetOutput(), mask,
                                                                   srcImage);
                }
            }
        }

        // unsupported
        else {
            displayError("Algorithm Gaussian Blur ITK: unsupported resolution");
            finalize();

            return;
        }

        constructLog();
        setCompleted(true);
    }

    /**
     * Sets the flag for the blue channel.
     *
     * @param  flag  if set to true then the blue channel is processed.
     */
    public void setBlue(boolean flag) {
        abProcessChannel[3] = flag;
    }

    /**
     * Sets the flag for the green channel.
     *
     * @param  flag  if set to true then the green channel is processed.
     */
    public void setGreen(boolean flag) {
        abProcessChannel[2] = flag;
    }

    /**
     * Sets the flag for the red channel.
     *
     * @param  flag  if set to true then the red channel is processed.
     */
    public void setRed(boolean flag) {
        abProcessChannel[1] = flag;
    }

    /**
     * Constructs a string of the contruction parameters and out puts the string to the messsage frame if the history
     * logging procedure is turned on.
     */
    private void constructLog() {
        String sigmaStr = new String();

        for (int i = 0; i < sigmas.length; i++) {
            sigmaStr += (" " + String.valueOf(sigmas[i]) + ", ");
        }

        if (srcImage.isColorImage()) {
            historyString = new String("GaussianBlur(" + sigmaStr + String.valueOf(null == mask) + ", " +
                                       String.valueOf(image25D) + ", " + abProcessChannel[0] + ", " +
                                       abProcessChannel[1] + ", " + abProcessChannel[2] + ")\n");
        } else {
            historyString = new String("GaussianBlur(" + sigmaStr + String.valueOf(null == mask) + ", " +
                                       String.valueOf(image25D) + ")\n");
        }
    }
}
