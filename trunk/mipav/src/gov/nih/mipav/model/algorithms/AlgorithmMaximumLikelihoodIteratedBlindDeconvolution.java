package gov.nih.mipav.model.algorithms;


import gov.nih.mipav.model.algorithms.utilities.*;
import gov.nih.mipav.model.algorithms.filters.*;
import gov.nih.mipav.model.structures.*;
import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;
import gov.nih.mipav.view.dialogs.*;

import java.io.*;
import java.awt.*;

/**
 * An implementation of Maximum Likelihood Iterated Blind Deconvolution based
 * on the following papers:
 *
 * Holmes, T.J., Maximum Likelihood Image Restoration Adapted for Noncoherent
 * Optical Imaging, JOSA-A, 5(5): 666-673, 1988.
 *
 * Holmes, T., Bhattacharyya, S., Cooper, J., Hanzel, D., Krishnamurthi, V.,
 * Lin, W., Roysam, B., Szarowski, D., Turner, J., Light Microscopic Images
 * Reconstructed by Maximum Likelihood Deconvolution, Ch. 24, Handbook of
 * Biological Confocal Microscopy, J. Pawley, Plenum, 1995.
 *
 * Holmes, T., Liu, Y., Image Restoration for 2D and 3D Fluorescence
 * Microscopy, Visualization in Biomedical Microscopies, A. Kriete, VCH, 1992.
 *
 * Holmes, T., Blind Deconvolution of Quantum-Limited Noncoherent Imagery,
 * JOSA-A, 9: 1052 - 1061, 1992.
 *
 */
public class AlgorithmMaximumLikelihoodIteratedBlindDeconvolution extends AlgorithmBase {

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** Number of iterations for the deconvolution: */
    private int m_iNumberIterations;

    /** Show the deconvolved image in progress every m_iNumberProgress steps: */
    private int m_iNumberProgress;

    /** Physically-based parameters: */
    /** numerical aperature of the imaging lense: */
    private float m_fObjectiveNumericalAperature;
    /** wavelength of the reflected or fluorescencing light: */
    private float m_fWavelength;
    /** sample index of refraction: */
    private float m_fRefractiveIndex;

    /** source image to be reconstructed! */
    private ModelImage m_kSourceImage;
    /** source image to be reconstructed! */
    private ModelImage m_kOriginalSourceImage;

    /** point spread function image: */
    private ModelImage m_kPSFImage;
    /** estimated of the reconstructed image: */
    private ModelImage m_kEstimatedImage;

    /** For color images: the source is converted to gray for the
     * deconvolution. Once the estimate and psf are found, each component is
     * reconstructed. */
    private ModelImage m_kSourceRed = null;
    private ModelImage m_kSourceGreen = null;
    private ModelImage m_kSourceBlue = null;

    /** Original data size: */
    private int m_iArrayLength = 1;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates a new AlgorithmMaximumLikelihoodIteratedBlindDeconvolution object.
     *
     * @param  kSrcImg, the input image to be reconstructed
     * @param iIterations, the number of times to iterate in the deconvolution
     * @param fObjectiveNumericalAperature, the numerical aperature of the imaging lense
     * @param fWavelength, the reflected or fluorescening light
     * @param fRefractiveIndex, the index of refraction for the sample
     */
    public AlgorithmMaximumLikelihoodIteratedBlindDeconvolution( ModelImage kSrcImg,
                                                                 int iIterations,
                                                                 int iProgress,
                                                                 float fObjectiveNumericalAperature,
                                                                 float fWavelength,
                                                                 float fRefractiveIndex )
    {
        m_kOriginalSourceImage = kSrcImg;
        m_kSourceImage = (ModelImage)kSrcImg.clone();
        m_iNumberIterations =  iIterations;
        m_iNumberProgress = iProgress;
        m_fObjectiveNumericalAperature = fObjectiveNumericalAperature;
        m_fWavelength = fWavelength;
        m_fRefractiveIndex = fRefractiveIndex;
        
        /* Determine the array length: */
        m_iArrayLength = 1;
        for ( int i = 0; i < m_kSourceImage.getNDims(); i++ )
        {
            m_iArrayLength *= m_kSourceImage.getExtents()[ i ];
        }
        /* convert color images: */
        if ( m_kSourceImage.isColorImage() && (m_iNumberIterations != 0) )
        {
            m_kSourceImage.disposeLocal();
            m_kSourceImage = convertToGray( kSrcImg );
        }
        /* Convert to float: */
        if ( !m_kSourceImage.isColorImage() )
        {
            try {
                m_kSourceImage.convertToFloat();
            } catch( java.io.IOException e ) {}
        }

        /* The initial guess at the estimated image is the original image: */
        m_kEstimatedImage = (ModelImage)(m_kSourceImage.clone());
        m_kEstimatedImage.setImageName( "estimate" + 0 );

        /* The initial psf is a gaussian of size 3x3: */
        m_kPSFImage = initPSF( m_kSourceImage.getNDims(), m_kSourceImage.getExtents() );
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Dispose of local variables that may be taking up lots of room.
     */
    public void disposeLocal() {
        m_kEstimatedImage.disposeLocal();
        m_kEstimatedImage = null;
        System.gc();
    }


    /**
     * Prepares this class for destruction.
     */
    public void finalize() {
        disposeLocal();
        super.finalize();
    }

    /** Convert the input image, kImage, to a grayscale image using the
     * AlgorithmRGBtoGray. The deconvolution is done on the grayscale image,
     * however after deconvolution, the original color values are
     * reconstructed from the psf and grayscale estimate.
     * @param kImage, the color image to conver to grayscale:
     * @return the converted grayscale image
     */
    private ModelImage convertToGray( ModelImage kImage )
    {
        /* First save each of the red, green, and blue channels in a
         * separate ModelImage for reconstruction. */
        /* Determine the type of the color image: */
        int iType = kImage.getType();
        if ( iType == ModelStorageBase.ARGB )
        {
            iType = ModelStorageBase.UBYTE;
        }
        if ( iType == ModelStorageBase.ARGB_FLOAT )
        {
            iType = ModelStorageBase.FLOAT;
        }
        if ( iType == ModelStorageBase.ARGB_USHORT )
        {
            iType = ModelStorageBase.USHORT;
        }
        
        /* Create three separate ModelImages of the same type: */
        m_kSourceRed   = new ModelImage(iType,
                                        kImage.getExtents(),
                                        null, null);
        m_kSourceGreen = new ModelImage(iType,
                                        kImage.getExtents(),
                                        null, null);
        m_kSourceBlue  = new ModelImage(iType,
                                        kImage.getExtents(),
                                        null, null);
        /* Copy the red, green, and blue components into the separate
         * images: */
        for ( int i = 0; i < m_iArrayLength; i++ )
        {
            m_kSourceRed.set( i,   kImage.get( i * 4 + 1 ) );
            m_kSourceGreen.set( i, kImage.get( i * 4 + 2 ) );
            m_kSourceBlue.set( i,  kImage.get( i * 4 + 3 ) );
        }
        try {
            /* Convert to float: */
            m_kSourceRed.convertToFloat();
            m_kSourceGreen.convertToFloat();
            m_kSourceBlue.convertToFloat();
        } catch( java.io.IOException e ) {}

        /* Convert the input image kImage to gray: */
        ModelImage kResult = new ModelImage(iType, kImage.getExtents(),
                                            null, null);
        AlgorithmRGBtoGray kRGBAlgo =
            new AlgorithmRGBtoGray( kResult, kImage );
        kRGBAlgo.setActiveImage(false);
        kRGBAlgo.setProgressBarVisible(false);
        kRGBAlgo.run();
        kRGBAlgo.finalize();
        kRGBAlgo = null;
        System.gc();
        return kResult;
    }

    /** 
     * Called after the deconvolution algorithm is run. The deconvolution
     * processes the grayscale image. Once the estimated and psf images are
     * calculated, they are used to reconstruct the red, green, and blue
     * channels separately, which are then recombined into a color image.
     * @param kRed, the reconstructed red image
     * @param kGreen, the reconstructed green image
     * @param kBlue, the reconstructed blue image
     * @return the RGB image
     */
    private ModelImage convertFromGray( ModelImage kRed,
                                        ModelImage kGreen,
                                        ModelImage kBlue )
    {
        ModelImage kResult = (ModelImage)m_kOriginalSourceImage.clone();
        float fRed, fGreen, fBlue;
        for ( int i = 0; i < m_iArrayLength; i++ )
        {
            fRed = Math.max( Math.min( kRed.getFloat( i ), 255.0f ), 0f );
            fGreen = Math.max( Math.min( kGreen.getFloat( i ), 255.0f ), 0f );
            fBlue = Math.max( Math.min( kBlue.getFloat( i ), 255.0f ), 0f );
            kResult.set( i * 4 + 0, 255f );
            kResult.set( i * 4 + 1, fRed );
            kResult.set( i * 4 + 2, fGreen );
            kResult.set( i * 4 + 3, fBlue );
        }
        return kResult;
    }

    /**
     * Initialize the first PSF guess as a 3x3 Gaussian.
     * @param, iNumberDimensions, the dimensions of the data
     * @param, aiExtents, the ranges in each dimension
     * @return, the ModelImage containing the Gaussian.
     */
    private ModelImage initPSF( int iNumberDimensions, int[] aiExtents )
    {
        /* Create a new ModelImage to contain the Gaussian: */
        ModelImage kImage = 
            new ModelImage( ModelStorageBase.FLOAT, aiExtents, "psf" + 0);

        /* Set up the data for the GenerateGaussian class: */
        float[] fGaussData = new float[ m_iArrayLength ];
        float[] fSigmas = new float[ iNumberDimensions ];
        int[] iDerivOrder = new int[ iNumberDimensions ];
        for ( int i = 0; i < iNumberDimensions; i++ )
        {
            fSigmas[ i ] = 3f;
            iDerivOrder[ i ] = 0;
        }
        /* Generate the Gaussian: */
        GenerateGaussian kGauss =
            new GenerateGaussian( fGaussData, aiExtents, fSigmas, iDerivOrder );
        kGauss.calc(false);
        kGauss.finalize();
        kGauss = null;
        /* Copy the Gaussian data into the ModelImage: */
        for ( int i = 0; i < m_iArrayLength; i++ )
        {
            kImage.set( i, fGaussData[ i ] );
        }
        fGaussData = null;
        fSigmas = null;
        iDerivOrder = null;

        /* Apply constraints to the image and return: */
        ModelImage kReturn = constraints( kImage );
        kImage.disposeLocal();
        kImage = null;
        return kReturn;
    }


    /**
     * Runs the deconvolution algorithm.
     * @param kSource, the original image data
     * @param kEstimate the initial estimated guess at the reconstructed image
     * @param kPSF, the initial point spread function guess
     * @param bCopy, when true the new estimates after interating are copied
     * back into the data members.
     * @return, the resulting estimated image
     */
    private ModelImage runDeconvolution( ModelImage kSource,
                                         ModelImage kEstimate,
                                         ModelImage kPSF,
                                         boolean bCopy )
    {
        ModelImage tempImageConvolve;
        ModelImage tempImageCalc;
        ModelImage originalDividedByEstimateCPSF;
        /* estimated and psf mirror images: */
        ModelImage kEstimateMirror = mirror( kEstimate );
        ModelImage kPSFMirror = mirror( kPSF );

        /* Loop over the number of iterations: */
        for ( int i = 1; i <= m_iNumberIterations; i++ )
        {
            /* Convolve the current estimate with the current PSF: */
            tempImageConvolve = convolve( kEstimate, kPSF, 1f );
            
            /* Divide the original image by the result and store: */
            originalDividedByEstimateCPSF = calc( kSource, tempImageConvolve,
                                                  AlgorithmImageCalculator.DIVIDE );
            tempImageConvolve.disposeLocal();

            /* Create new kEstimate based on original image and psf: */
            /* Convolve the divided image with the psf mirror: */
            tempImageConvolve = convolve( originalDividedByEstimateCPSF, kPSFMirror, 0 );
            /* multiply the result by the current estimated image: */
            tempImageCalc = calc( tempImageConvolve, kEstimate,
                                  AlgorithmImageCalculator.MULTIPLY );
            tempImageConvolve.disposeLocal();
            kEstimate.disposeLocal();
            kEstimate = tempImageCalc;
            kEstimate.setImageName( "estimated" + i );
            

            /* Create new psf based on original image and psf: */
            /* convolve the divided image with the estimated mirror: */
            tempImageConvolve =
                convolve( originalDividedByEstimateCPSF, kEstimateMirror, 0 );
            originalDividedByEstimateCPSF.disposeLocal();

            /* multiply the result by the current psf image: */
            /* Apply constraints to new PSF: */
            tempImageCalc = calc( tempImageConvolve, kPSF,
                                  AlgorithmImageCalculator.MULTIPLY );
            tempImageConvolve.disposeLocal();
            kPSF.disposeLocal();
            kPSF = constraints( tempImageCalc );
            kPSF.setImageName( "psf" + i );
            tempImageCalc.disposeLocal();

            /* Mirror the new estimate and new psf: */
            kEstimateMirror.disposeLocal();
            kEstimateMirror = mirror( kEstimate );
            kPSFMirror.disposeLocal();
            kPSFMirror = mirror( kPSF );
            
            /* Display progression: */
            if ( ((i%m_iNumberProgress) == 0) && (i != m_iNumberIterations) )
            {
                new ViewJFrameImage((ModelImage)(kEstimate.clone()),
                                    null, new Dimension(610, 200));
            }
        }
        if ( bCopy )
        {
            m_kEstimatedImage.disposeLocal();
            m_kEstimatedImage = kEstimate;

            m_kPSFImage.disposeLocal();
            m_kPSFImage = kPSF;
        }
        else
        {
            kPSF.disposeLocal();
        }

        kEstimateMirror.disposeLocal();
        kPSFMirror.disposeLocal();

        return kEstimate;
    } 

    /**
     * Convolves two image by computing the fast fourier transforms of each
     * image, multiplying the ffts and then computing the inverse fft of the
     * result:
     * @param kImage1, the first input image
     * @param kImage2, the second input image
     * @param fNoise, amount of Noise to add when the result of the
     * convolution is going to be the denominator in the next equation to
     * avoid divide by zero errors.
     * @return kResult, the result of convolving the two images
     */
    private ModelImage convolve( ModelImage kImage1, ModelImage kImage2,
                                  float fNoise )
    {
        /* Calculate the forward fft of the two input images: */
        ModelImage kImageSpectrum1 = fft( kImage1, 1 );
        ModelImage kImageSpectrum2 = fft( kImage2, 1 );
        
        /* multiply the resulting spectrums: */
        ModelImage kConvolve = calc( kImageSpectrum1, kImageSpectrum2,
                                     AlgorithmImageCalculator.MULTIPLY );
        
        /* Set the image Convolve flag to be true: */
        kConvolve.setConvolve( true );
        /* Calculate the inverse FFT: */
        ModelImage kResult = fft( kConvolve, -1 );
        
        /* Add noise factor: */
        for ( int i = 0; i < m_iArrayLength; i++ )
        {
            kResult.set( i, kResult.getFloat( i ) + fNoise );
        }
        /* Clean up temporary data: */
        kImageSpectrum1.disposeLocal();
        kImageSpectrum1 = null;
        kImageSpectrum2.disposeLocal();
        kImageSpectrum2 = null;
        kConvolve.disposeLocal();
        kConvolve = null;

        /* Return result: */
        return kResult;
    }


    /**
     * Computes the fft of the input image, returns the fft. Calls AlgorithmFFT.
     * @param kImage, the input image
     * @param iDir, forward fft when iDir = 1, inverse fft when iDir = -1
     * @return kResult, the fft of kImage
     */
    private ModelImage fft( ModelImage kImage, int iDir )
    {
        ModelImage kImageSpectrum = (ModelImage)kImage.clone();

        // FFT parameters
        boolean logMagDisplay = false;
        boolean unequalDim = false;
        boolean image25D = false;
        boolean imageCrop = true;
        int kernelDiameter = 15;
        int filterType = 2;
        float freq1 = 0.0f;
        float freq2 = 1.0f;
        int constructionMethod = 1;
        int butterworthOrder = 0;

        // take the FFT of the first input image: 
        AlgorithmFFT kFFT = new AlgorithmFFT( kImageSpectrum, kImage,
                                              iDir, logMagDisplay,
                                              unequalDim, image25D, imageCrop,
                                              kernelDiameter, filterType, freq1, freq2,
                                              constructionMethod, butterworthOrder);
        kFFT.setActiveImage(false);
        kFFT.setProgressBarVisible( false );
        kFFT.run();
        kFFT.finalize();
        kFFT = null;
        System.gc();
        return kImageSpectrum;
    }

    /**
     * Performs a AlgorithmImageCalculator calculation on the two input
     * images. Used here to either multiply or divide two images. 
     * @param kImage1, the first input image
     * @param kImage2, the second input image
     * @param iType, the type of calculation (multiply or divide)
     * @return kReturn, the result of the image calculation
     */
    private ModelImage calc( ModelImage kImage1, ModelImage kImage2, int iType )
    {
        ModelImage kReturn = (ModelImage)(kImage1.clone());
        AlgorithmImageCalculator algImageCalc =
            new AlgorithmImageCalculator( kReturn, kImage1, kImage2,
                                          iType,
                                          AlgorithmImageMath.CLIP, true,
                                          null);

        algImageCalc.setActiveImage(false);
        algImageCalc.setProgressBarVisible( false );
        algImageCalc.run();
        algImageCalc.finalize();
        algImageCalc = null;
        System.gc();
        return kReturn;
    }

    /** Applies constraints to the input image kImage and writes the
     * constrained values into the m_kPSFImage.
     * Constraints are the following:
     * 2). Hourglass constraint
     * 3). Bandlimit and missing cone constraint
     * 4). Nonnegativity constraint
     * 1). Unit summation constraint
     *
     * @param kImage, the unconstrained PSFImage
     */
    private ModelImage constraints( ModelImage kImage )
    {
        ModelImage kReturn = (ModelImage)kImage.clone();

        int iDimX = kImage.getExtents()[0];
        int iDimY = kImage.getExtents()[1];
        int iDimZ = 1;
        if ( kImage.getNDims() > 2 )
        {
            iDimZ = kImage.getExtents()[2];
        }

        /* 2). Hourglass constraint: */
        /*
        float fRadius = (3 * 0.61f * m_fWavelength) / m_fObjectiveNumericalAperature;
        float fTheta = (float)Math.asin( m_fObjectiveNumericalAperature / m_fRefractiveIndex );
        float fRadiusZ = 0.0f;
        for ( int z = 1; z <= iDimZ; z++ )
        {
            for ( int x = 1; x <= iDimX; x++ )
            {
                for ( int y = 1; y <= iDimY; y++ )
                {
                    fRadiusZ = (float)(Math.abs( z - (iDimZ+1)/2 ) * Math.tan( fTheta ));
                    if ( ( ((x - (iDimX+1)/2) * (x - (iDimX+1)/2)) + 
                           ((y - (iDimY+1)/2) * (y - (iDimY+1)/2))   ) > 
                         ( ( fRadius + fRadiusZ ) * ( fRadius + fRadiusZ ) ) )
                    {
                        kImage.set( (z - 1) * (iDimY * iDimX ) + 
                                    (y - 1) * iDimX +
                                    (x - 1),
                                    0f );
                    }
                }
            }
        }
        */
        /* 3). Bandlimit and missing cone constraint: */
        /*
        float fRadialBandLimit = (float)( 2.0f * m_fObjectiveNumericalAperature / m_fWavelength );
        float fAxialBandLimit = (float)(( m_fObjectiveNumericalAperature *
                                          m_fObjectiveNumericalAperature )  /
                                        (2.0 * m_fRefractiveIndex * m_fWavelength ));
        ModelImage kImageFFT = fft( kImage, 1 );
        for ( int z = 1; z <= iDimZ; z++ )
        {
            for ( int x = 1; x <= iDimX; x++ )
            {
                for ( int y = 1; y <= iDimY; y++ )
                {
                    fRadiusZ = (float)(Math.abs( z - (iDimZ+1)/2 ) * Math.tan( fTheta ));
                    if ( ( ( ((x - (iDimX+1)/2) * (x - (iDimX+1)/2)) + 
                             ((y - (iDimY+1)/2) * (y - (iDimY+1)/2))   ) > 
                           fRadialBandLimit ) ||
                         ( (float)(Math.abs( z - (iDimZ+1)/2 )) > fAxialBandLimit ) ||
                         ( ( ((x - (iDimX+1)/2) * (x - (iDimX+1)/2)) + 
                                ((y - (iDimY+1)/2) * (y - (iDimY+1)/2))   ) <  
                              fRadius )
                         )
                    {
                        kImageFFT.set( (z - 1) * (iDimY * iDimX ) + 
                                       (y - 1) * iDimX +
                                       (x - 1),
                                       0f );
                    }
                }
            }
        }
        ModelImage kImageInvFFT = fft( kImageFFT, -1 );
        kImageFFT.disposeLocal();
        */
        ModelImage kImageInvFFT = (ModelImage)kImage.clone();
        /* 4). Nonnegativity constraint: */
        float fSum = 0;
        for ( int i = 0; i < m_iArrayLength; i++ )
        {
            if ( kImageInvFFT.getFloat(i) < 0 )
            {
                kImageInvFFT.set( i, 0f );
            }
            fSum += kImageInvFFT.getFloat( i );
        }
        /* 1). Unit summation constraint: */
        for ( int i = 0; i < m_iArrayLength; i++ )
        {
            kReturn.set( i, kImageInvFFT.getFloat( i ) / fSum );
        }
        kImageInvFFT.disposeLocal();
        
        return kReturn;
    }

    /**
     * Does a mirror operation on an image, so that x, y, and z values are mirrored
     * Calls JDialogFlip
     * @param kImage, the input image
     * @param kReturn, the mirror of kImage
     */
    private ModelImage mirror( ModelImage kImage )
    {
        ModelImage kReturn = (ModelImage)(kImage.clone());
        JDialogFlip flipy =
            new JDialogFlip( ViewUserInterface.getReference(),
                             kReturn, AlgorithmFlip.Y_AXIS);
        flipy.setActiveImage(false);
        flipy.setSeparateThread( false );
        flipy.callAlgorithm();
        flipy.dispose();
        flipy = null;
        JDialogFlip flipx =
            new JDialogFlip( ViewUserInterface.getReference(),
                             kReturn, AlgorithmFlip.X_AXIS);
        flipx.setActiveImage(false);
        flipx.setSeparateThread( false );
        flipx.callAlgorithm();
        flipx.dispose();
        flipx = null;
        System.gc();
        return kReturn;
    }



    /**
     * Starts the program.
     */
    public void runAlgorithm() {

        if (m_kSourceImage == null) {
            MipavUtil.displayError("AlgorithmMaximumLikelihoodIteratedBlindDeconvolution  Source Image is null");

            return;
        }

        if ( m_iNumberIterations != 0 )
        {
            runDeconvolution( m_kSourceImage, m_kEstimatedImage, m_kPSFImage, true );
            
            if (threadStopped) {
                finalize();
                
                return;
            }
            if ( m_kOriginalSourceImage.isColorImage() )
            {
                m_iNumberIterations = 1;
                /* doesn't matter, just has to be higher than m_iNumberIterations */
                m_iNumberProgress = 2;
            
                /* get new estimates for each color channel by runing the
                 * deconvolution with the estimates from the grayscale image --
                 * deconvolution does not iterated again: */
                m_kSourceRed = runDeconvolution( m_kSourceRed,
                                                 (ModelImage)m_kEstimatedImage.clone(),
                                                 (ModelImage)m_kPSFImage.clone(),
                                                 false );
                m_kSourceGreen = runDeconvolution( m_kSourceGreen,
                                                   (ModelImage)m_kEstimatedImage.clone(),
                                                   (ModelImage)m_kPSFImage.clone(),
                                                   false );
                m_kSourceBlue = runDeconvolution( m_kSourceBlue,
                                                  (ModelImage)m_kEstimatedImage.clone(),
                                                  (ModelImage)m_kPSFImage.clone(),
                                                  false );
                
                /* Use the reconstructed color channels to generate new color image: */
                m_kEstimatedImage.disposeLocal();
                m_kEstimatedImage = convertFromGray( m_kSourceRed,
                                                     m_kSourceGreen,
                                                     m_kSourceBlue );
            }
        }

        cleanUp();
        setCompleted(true);

    } // end runAlgorithm

    /**
     * Returns the reconstructed image:
     * @return m_kEstimatedImage
     */
    public ModelImage getReconstructedImage()
    {
        return (ModelImage)m_kEstimatedImage.clone();
    }

    /** 
     * Dispose of temporary images after the deconvolution process is
     * completed:
     */
    private void cleanUp()
    {
        m_kSourceImage.disposeLocal();
        m_kSourceImage = null;
        
        m_kOriginalSourceImage = null;

        m_kPSFImage.disposeLocal();
        m_kPSFImage = null;

        if ( m_kSourceRed != null )
        {
            m_kSourceRed.disposeLocal();
            m_kSourceRed = null;
        }
        if ( m_kSourceGreen != null )
        {
            m_kSourceGreen.disposeLocal();
            m_kSourceGreen = null;
        }
        if ( m_kSourceBlue != null )
        {
            m_kSourceBlue.disposeLocal();
            m_kSourceBlue = null;
        }
    }
}
