package gov.nih.mipav.model.structures;


import java.util.*;
import java.awt.*;

/**
*   Histogram model extends ModelStorageBase and stores and processes
*   information about histograms
*
*		@version 0.1 Oct 1, 1997
*		@author Matthew J. McAuliffe, Ph.D.
*/

public class ModelHistogram extends ModelStorageBase {

    private Vector      histogramFrameVector 	= new Vector();
    private int         totalPixels;
    private int         stRange,endRange;
    private int         mode;
    private float       mean;
    private int         maxEntropyThreshold;
    private int         otsuThreshold;

	/**
	* ModelHistogram
	* @param type        indicates type of data in histogram
	* @param dimExtents  array indicating extents (1 - dimension)
	*/
	public ModelHistogram(int type, int dimExtents[]){
	    super(type, dimExtents);
	}

    /**
    *   Gets the total pixel count
    *   @return         total number of pixels in histogram
    */
    public int      getTotalPixels() {return totalPixels;}

    /**
    *   Gets the starting range of pixels
    *   @return         starting range
    */
    public int      getStartRange()  {return stRange;}

    /**
    *   Gets the starting range of pixels
    *   @return        ending range
    */
    public int      getEndRange()    {return endRange;}

    /**
     * Gets the maximum entropy threshold
     * @return int threshold
     */
    public int      getMaxEntropyThreshold()     { return maxEntropyThreshold; }

    /**
    *   Gets the mean pixel intensity
    *   @return        mean pixel intensity
    */
    public float    getMean()        {return mean;}

    /**
    *   Gets the mode of the pixel intensities
    *   @return       total number of pixels in histogram
    */
    public int      getMode()        {return mode;}


    /**
     * Gets the otsu threshold as calculated
     * in AlgorithmThreshold
     * @return int otsu threshold
     */
    public int getOtsuThreshold() { return otsuThreshold; }

    /**
    *   Sets total number of pixels of histogram
    *   @param pixels     total number of pixels
    */
    public void setTotalPixels(int pixels)  {totalPixels = pixels;}


    /**
    *   Sets the starting range of histogram
    *   @param start     start range of histogram
    */
    public void setStartRange(int start)    {stRange   = start;}

    /**
    *   Sets the ending range of histogram
    *   @param end     end range of histogram
    */
    public void setEndRange(int end)        {endRange  = end;}

    /**
     * Sets maximum entropy threshold
     * @param thres int threshold
     */
    public void setMaxEntropyThreshold(int thres) { this.maxEntropyThreshold = thres; }

    /**
    *   Sets mean intensity of histogram
    *   @param mean   mean value of intensity
    */
    public void setMean(float mean)         {this.mean      = mean;}

    /**
    *   Sets the mode intensity of histogram
    *   @param mode   mode value of intensity
    */
    public void setMode(int mode)           {this.mode      = mode;}

    /**
     * Sets the Otsu threshold
     * @param otsu int threshold
     */
    public void setOtsuThreshold(int otsu)  {this.otsuThreshold = otsu; }

    /**
    *   Registers the image Frame
    *   @param frame  - Frame to be registered with image model so
    *                   when the image model changes the view of the
    *                   image will change
    */
    public void registerFrame(Frame frame){
        histogramFrameVector.addElement(frame);
    }

    /**
    *   Unregisters the image Frame
    *   @param frame  - Frame to be registered
    */
    public void unregisterFrame(Frame frame){
        histogramFrameVector.removeElement(frame);
    }

    /**
    * Updates all frames where this image model is being displayed
    * @param LUT
    */
//    public  void updateFrames(ModelLUT LUT){
 //       int i;
//
 //       for (i = 0; i < histogramFrameVector.size(); i ++) {
 //          ((ViewFrameHistogramBase)(histogramFrameVector.elementAt(i))).updateImages(LUT);
 //       }
 //   }




}
