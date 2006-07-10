package gov.nih.mipav.view;


import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.dialogs.*;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.util.*;

import javax.swing.*;


/**
 * Generates a MIP image from a 3D volume. When the user clicks on a pixel of the MIP, a region grow is performed
 * starting at the point where the clicked intensity occurs.
 *
 * <p>Rotation of the volume to take different MIPs is something that might be useful to add (it's been partially added
 * (see the MIP menu), but it is untested..).</p>
 *
 * <p>Not tested with color images... probably won't work.</p>
 *
 * <p>Right now, the MIP and the volume partially share the same LUT.</p>
 *
 * @author  Evan McCreedy
 * @see     ViewJFrameImage
 */
public class ViewJFramePaintVasculature extends ViewJFrameBase {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = 1332576394417707204L;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** Display of the MIP image. */
    protected ViewJComponentEditImage componentImage;

    /**
     * Frame adjustment to keep the frame from being just small enough for the ScrollPane to display the scroll bars.
     * Windows seems to find an inset value needing a fudge factor of 3, but Mac OS 10 (X), doesn't, and needs a fudge
     * factor of 7 to not require scroll bars. Either way, insets + fudge-factor must equal 7.
     */
    protected int fudgeFactor = 0; // assume Windos.

    /** Storage for correction parameters where datasets have non isotropic values. */
    protected float heightResFactor;

    /** Buffer used to store image intensities the presently viewed slice of image. */
    protected float[] imageBuffer;

    /**
     * Flag indicating whether or not that the image should be displayed in Log scale. Used primarily for displaying the
     * FFT of an image.
     */
    protected boolean logMagDisplay;

    /** Buffer to store the MIP image that results from the raytrace. */
    protected float[] mipBuffer;

    /** The dimensions of the MIP image. */
    protected int[] mipExtents;

    /** MIP image generated from the volume. */
    protected ModelImage mipImage;

    /** Table which holds the z coordinate on the 3D volume for a point (x,y) on the 2D MIP image. */
    protected Hashtable mipZTable;

    /** Dialog to facilitate vasculature painting. */
    protected JDialogPaintVasculature paintDialog;

    /** Parent frame of this frame. */
    protected ViewJFrameBase parent;

    /**
     * Integer buffer (4 bytes that stores the concatenated Alpha (1 byte), Red (1 byte), Green ( 1 byte ), Blue (1 byte
     * ) data. The ARGB values are generated by using the mipImage intensities as a index into a LUT.
     */
    protected int[] pixBuffer;

    /** Storage of the image voxel resolutions. One resolution value per dimension. */
    protected float[] resols;

    /** VOI containing the possible seed points. */
    protected VOI seedPoints;

    /** Storage of the resolution units of measure. For example, mm, cm, inches ... */
    protected int[] units;

    /** Storage for correction parameters where datasets have non isotropic values. */
    protected float widthResFactor;

    /** Width of the display screen. */
    protected int xScreen;

    /** Height of the display screen. */
    protected int yScreen;

    /** Defaults magnification of image to 1. */
    protected float zoom = 1;

    /** The scrollPane where the image is displayed. */
    private JScrollPane scrollPane;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Constructs the MIP frame and pops up the region grow dialog.
     *
     * @param  img    volume to perform the MIP on
     * @param  lut    LUT of the volume
     * @param  loc    where to place the frame
     * @param  frame  the parent frame (which must have the component image containing img)
     */
    public ViewJFramePaintVasculature(ModelImage img, ModelLUT lut, Dimension loc, ViewJFrameBase frame) {
        super(img, null);

        this.parent = frame;

        this.LUTa = lut;
        this.logMagDisplay = img.getLogMagDisplay();

        mipZTable = new Hashtable();

        int xDim = imageA.getExtents()[0];
        int yDim = imageA.getExtents()[1];
        int zDim = imageA.getExtents()[2];

        // do initial MIP straight-on, through the z-dim
        calcMIPBuffer(xDim, yDim, zDim);

        initComponentImage();

        initFrame(loc);

        mipImage.addImageDisplayListener(parent);

        componentImage.setMode(ViewJComponentEditImage.DEFAULT);
        paintDialog = new JDialogPaintVasculature(this);
        paintDialog.setLocation(getLocation().x + getSize().width, getLocation().y);

        componentImage.setGrowDialog(paintDialog);
        ((ViewJFrameImage) parent).getComponentImage().setGrowDialog(paintDialog);

        findSeedPoints(paintDialog.getSeedIntensity());
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Listen for events from the MIP frame GUI.
     *
     * @param  event  event triggered by GUI
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        if (command.equals("EraseAll")) {
            componentImage.eraseAllPaint(false);
            paintDialog.commitGrow(true);
        } else if (command.equals("MagImage")) {
            float magZoom = 2.0f * componentImage.getZoomX();

            updateFrame(magZoom, magZoom);
        } else if (command.equals("UnMagImage")) {
            float magZoom = 0.5f * componentImage.getZoomX();

            updateFrame(magZoom, magZoom);
        } else if (command.equals("DisplayLUT")) {

            if (componentImage.getActiveImage().getType() == ModelStorageBase.BOOLEAN) {
                MipavUtil.displayError(" Cannot change the LUT of a Boolean image.");
            } else {

                if (mipImage.getHistoLUTFrame() == null) {
                    JDialogHistogramLUT histogramDialog = null;

                    if (mipImage.isColorImage() == false) {

                        try {
                            histogramDialog = new JDialogHistogramLUT(this, mipImage, imageB, componentImage.getLUTa(),
                                                                      componentImage.getLUTb(), userInterface);
                        } catch (OutOfMemoryError error) {
                            MipavUtil.displayError("Out of memory: unable to open LUT frame.");
                        }
                    } else {

                        try {
                            histogramDialog = new JDialogHistogramLUT(this, mipImage, imageB, componentImage.getRGBTA(),
                                                                      componentImage.getRGBTB(), userInterface);
                        } catch (OutOfMemoryError error) {
                            MipavUtil.displayError("Out of memory: unable to open LUT frame.");
                        }
                    }

                    if (componentImage.getActiveImage().getVOIs().size() == 0) {
                        histogramDialog.histogramLUT(true);
                    } else {
                        histogramDialog.constructDialog();
                    }
                }
            }
        } else if (command.equals("quickLUT")) {
            componentImage.setMode(ViewJComponentEditImage.QUICK_LUT);
        } else if (command.equals("resetLUTs")) {
            componentImage.resetLUTs();

            if ((componentImage.getActiveImage().isColorImage()) &&
                    (componentImage.getActiveImage().getHistoRGBFrame() != null)) {
                componentImage.getActiveImage().getHistoRGBFrame().update();
            } else if (componentImage.getActiveImage().getHistoLUTFrame() != null) {
                componentImage.getActiveImage().getHistoLUTFrame().update();
            }

            if (componentImage.getActiveImage() == mipImage) {
                componentImage.getActiveImage().notifyImageDisplayListeners(componentImage.getLUTa(), true);
            } else {
                componentImage.getActiveImage().notifyImageDisplayListeners(componentImage.getLUTb(), true);
            }
        } else if (command.equals("Pointer")) {
            paintDialog.setPaintGrowMode(JDialogPaintVasculature.AUTO_POINT);
        } else if (command.equals("PointRegionGrow")) {
            paintDialog.setPaintGrowMode(JDialogPaintVasculature.AUTO_POINT);
        } else if (command.equals("ArbitraryRegionGrow")) {
            paintDialog.setPaintGrowMode(JDialogPaintVasculature.CLICK_POINT);
        }
    }

    /**
     * Generates a MIP image from the volume.
     *
     * @param  firstDim   the "x" dimension of the generated MIP
     * @param  secondDim  the slowest moving dimension -- the "y" dimension of the generated MIP
     * @param  thirdDim   the fastest moving dimension -- the one we are finding the maximum intensity of
     */
    public void calcMIPBuffer(int firstDim, int secondDim, int thirdDim) {
        int ix, iy, iz;
        float max;
        float maxZ;
        float value;
        int index;

        mipExtents = new int[2];
        mipExtents[0] = firstDim;
        mipExtents[1] = secondDim;

        mipBuffer = new float[firstDim * secondDim];
        imageBuffer = new float[firstDim * secondDim * thirdDim];

        try {
            imageA.exportData(0, firstDim * secondDim * thirdDim, imageBuffer);
        } catch (IOException ioe) {
            MipavUtil.displayError("Error loading volume. Could not generate MIP.");

            return;
        }

        for (iy = 0; iy < secondDim; iy++) {

            for (ix = 0; ix < firstDim; ix++) {
                maxZ = 0;
                max = (float) imageA.getMin();

                for (iz = 0; iz < thirdDim; iz++) {
                    value = imageBuffer[(iz * secondDim * firstDim) + (iy * firstDim) + ix];

                    if (value > max) {
                        max = value;
                        maxZ = iz;
                    }
                }

                index = (iy * firstDim) + ix;
                mipBuffer[index] = max;
                mipZTable.put(new Integer(index), new Float(maxZ));
            }
        }

        // generate an image from the mip buffer
        mipImage = new ModelImage(ModelStorageBase.FLOAT, new int[] { firstDim, secondDim },
                                  imageA.getImageName() + "_MIP", userInterface);

        try {
            mipImage.importData(0, mipBuffer, true);
        } catch (IOException ioe) {
            MipavUtil.displayError("Error loading mip data into image.");

            return;
        }
    }

    /**
     * Closes window and disposes of frame and component.
     */
    public void close() {

        if (Preferences.is(Preferences.PREF_CLOSE_FRAME_CHECK)) {
            int reply = JOptionPane.showConfirmDialog(this, "Do you really want to close this frame?", "Close Frame",
                                                      JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (reply == JOptionPane.NO_OPTION) {
                return;
            }
        }

        if (userInterface.isScriptRecording()) {
            userInterface.getScriptDialog().append("CloseFrame " +
                                                   userInterface.getScriptDialog().getVar(componentImage.getActiveImage().getImageName()) +
                                                   "\n");
        }

        if (componentImage.growDialog != null) {
            componentImage.growDialog.resetDialogs();
        }

        if (componentImage != null) {
            componentImage.dispose(false);
        }

        componentImage = null;

        if (mipImage != null) {
            mipImage.disposeLocal();
        }

        mipImage = null;

        imageBuffer = null;
        pixBuffer = null;
        mipBuffer = null;
        mipExtents = null;
        scrollPane = null;

        mipZTable = null;

        paintDialog = null;

        super.close();

        System.gc();
    }

    /**
     * Resizes frame and all components.
     *
     * @param  event  event that triggered function
     */
    public synchronized void componentResized(ComponentEvent event) {
        int width, height;
        float bigger;

        if ((getSize().width >= (xScreen - 20)) || (getSize().height >= (yScreen - 20))) {
            return;
        }

        removeComponentListener(this);

        width = getSize().width - (2 * getInsets().left) - fudgeFactor;
        height = getSize().height - getInsets().top - getInsets().bottom - fudgeFactor;
        bigger = Math.max(width, height);
        zoom = (int) Math.min((bigger - 1) / ((mipExtents[0] * widthResFactor) - 1),
                              (bigger - 1) / ((mipExtents[1] * heightResFactor) - 1));

        if (zoom > componentImage.getZoomX()) {
            componentImage.setZoom((int) zoom, (int) zoom); // ***************************

            // setZoom(zoom, zoom);
            updateImages(true);

            if ((componentImage.getSize(null).width + 200) > xScreen) {
                width = xScreen - 200;
            } else {
                width = componentImage.getSize(null).width /* + fudgeFactor*/;
            }

            if ((componentImage.getSize(null).height + 200) > yScreen) {
                height = yScreen - 200;
            } else {
                height = componentImage.getSize(null).height /* + fudgeFactor*/;
            }
        } else if ((width < componentImage.getSize(null).width) && (height >= componentImage.getSize(null).height)) {

            // width += fudgeFactor;
            height = componentImage.getSize(null).height /* + fudgeFactor*/ +
                     scrollPane.getHorizontalScrollBar().getHeight();
        } else if ((width >= componentImage.getSize(null).width) && (height < componentImage.getSize(null).height)) {
            width = componentImage.getSize(null).width /* + fudgeFactor*/ +
                    scrollPane.getVerticalScrollBar().getWidth();
            // height += fudgeFactor;
        } else if ((width < componentImage.getSize(null).width) || (height < componentImage.getSize(null).height)) { // width += fudgeFactor;

            // height += fudgeFactor;
        } else if ((width > componentImage.getSize(null).width) || (height > componentImage.getSize(null).height)) {

            if (width > componentImage.getSize(null).width) {
                width = componentImage.getSize(null).width; // ?????

                // height += fudgeFactor;
            }

            if (height > componentImage.getSize(null).height) {
                height = componentImage.getSize(null).height /* + fudgeFactor*/;
                // width += fudgeFactor;
            }
        } else {
            addComponentListener(this);

            return;
        }

        width += scrollPane.getInsets().left + scrollPane.getInsets().right;
        height += scrollPane.getInsets().top + scrollPane.getInsets().bottom;

        scrollPane.setSize(width, height);
        setSize(scrollPane.getSize().width + getInsets().left + getInsets().right + fudgeFactor, // 7 FOR THE MAC
                scrollPane.getSize().height + getInsets().top + getInsets().bottom + fudgeFactor);

        validate();
        setTitle();
        addComponentListener(this);
        updateImages(true);
        // componentImage.frameHeight = getSize().height;
        // componentImage.frameWidth = getSize().width;
    }

    /**
     * Cleans memory.
     *
     * @throws  Throwable  if something goes wrong in the parent constructor
     */
    public void finalize() throws Throwable {

        if (componentImage != null) {
            componentImage.dispose(false);
        }

        componentImage = null;

        if (mipImage != null) {
            mipImage.disposeLocal();
        }

        mipImage = null;

        imageBuffer = null;
        pixBuffer = null;
        mipBuffer = null;
        mipExtents = null;

        scrollPane = null;

        mipZTable = null;

        paintDialog = null;

        super.finalize();
    }

    /**
     * Mark all points within the image which match a certain intensity with point VOIs. If points are too close to one
     * another in the volume, only one is marked.
     *
     * @param  val  the intensity value to mark
     */
    public void findSeedPoints(int val) {
        boolean isNewVOI;

        if (seedPoints == null) {
            isNewVOI = true;
            seedPoints = new VOI((short) mipImage.getVOIs().size(), "possibleseedpoints.voi", 1, VOI.POINT, -1.0f);
            seedPoints.setColor(Color.blue.darker());
        } else {
            isNewVOI = false;
            seedPoints.removeCurves(0);
        }

        Vector points = new Vector();
        int vascVal = val;
        int count = 0;

        for (int i = 0; i < mipBuffer.length; i++) {

            if (mipBuffer[i] == vascVal) {
                count++;

                int x = i % mipExtents[0];
                int y = i / mipExtents[0];
                int z = (int) getMIPZValue(i);

                points.add(new Point3Df(x, y, z));
            }
        }

        int[] x = new int[1];
        int[] y = new int[1];
        int[] z = new int[1];
        Point3Df prev = new Point3Df(0, 0, 0);

        for (int i = 0; i < points.size(); i++) {
            Point3Df cur = (Point3Df) points.get(i);

            /// good distance?
            if (distance(cur, prev) > 50) {
                x[0] = (int) cur.x;
                y[0] = (int) cur.y;
                z[0] = (int) cur.z;
                seedPoints.importCurve(x, y, z, 0);
            }

            prev = (Point3Df) points.get(i);
        }

        if (isNewVOI) {
            mipImage.registerVOI(seedPoints);
        } else {
            mipImage.notifyImageDisplayListeners();
        }
    }

    /**
     * Returns the reference to the component image.
     *
     * @return  component image
     */
    public ViewJComponentEditImage getComponentImage() {
        return componentImage;
    }

    /**
     * Placeholder.
     *
     * @return  null
     */
    public ViewControlsImage getControls() {
        return null;
    }

    /**
     * Returns the reference to mipImage.
     *
     * @return  image
     */
    public ModelImage getImageA() {

        if (componentImage != null) {
            return componentImage.getImageA();
        } else {
            return null;
        }
    }

    /**
     * Placeholder required by ViewJFrameBase.
     *
     * @return      the second image
     *
     * @deprecated  DOCUMENT ME!
     */
    public ModelImage getImageB() {
        return null;
    }

    /**
     * Gets the Z value where a pixel in the MIP image occurred in the volume. Used to translate points on the MIP to
     * points on the volume.
     *
     * @param   index  index into the mip buffer of the point to get ( index = x + y * xDimSize )
     *
     * @return  z value of the point on the volume which was used for the MIP image point
     */
    public float getMIPZValue(int index) {
        return ((Float) mipZTable.get(new Integer(index))).floatValue();
    }

    /**
     * Get the parent frame of this MIP image frame.
     *
     * @return  the parent frame (the main RFAST frame)
     */
    public ViewJFrameBase getParentFrame() {
        return parent;
    }

    /**
     * Gets the RGB LUT table for ARGB image A.
     *
     * @return  RGBT the new RGB LUT to be applied to the image
     */
    public ModelRGB getRGBTA() {
        return (componentImage.getRGBTA());
    }

    /**
     * Placeholder required by ViewJFrameBase.
     *
     * @return      the RGBTB data
     *
     * @deprecated  DOCUMENT ME!
     */
    public ModelRGB getRGBTB() {
        return null;
    }

    /**
     * Return the points that have been marked on the MIP as possible seed points.
     *
     * @return  a VOI containing the possible seed points
     */
    public VOI getSeedPoints() {
        return seedPoints;
    }

    /**
     * Initializes the 2D MIP image frame.
     *
     * @param   loc  where the frame should be placed
     *
     * @throws  OutOfMemoryError  if there is not enough memory available to construct the MIP window
     */
    public void initFrame(Dimension loc) throws OutOfMemoryError {

        try {
            setIconImage(MipavUtil.getIconImage(Preferences.getIconName()));
        } catch (FileNotFoundException error) {
            Preferences.debug("Exception ocurred while getting <" + error.getMessage() +
                              ">.  Check that this file is available.\n");
            System.err.println("Exception ocurred while getting <" + error.getMessage() +
                               ">.  Check that this file is available.\n");
        }

        setResizable(true);

        setTitle();

        // The component image will be displayed in a scrollpane.
        scrollPane = new JScrollPane(componentImage, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        getContentPane().add(scrollPane);
        scrollPane.setBackground(Color.black);
        setBackground(Color.black);

        setSize(scrollPane.getSize().width + getInsets().left + getInsets().right + fudgeFactor,
                scrollPane.getSize().height + getInsets().top + getInsets().bottom + fudgeFactor);

        if (loc != null) {
            setLocation(loc.width, loc.height);
        }

        addComponentListener(this);

        // MUST register frame to image models
        mipImage.addImageDisplayListener(this);

        pack();
        setVisible(true);

        // User interface will have list of frames
        userInterface.registerFrame(this);
        updateImages(true);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    /**
     * Placeholder.
     */
    public void removeControls() { }

    /**
     * Placeholder required by ViewJFrameBase.
     *
     * @param       val  the active image number
     *
     * @deprecated  DOCUMENT ME!
     */
    public void setActiveImage(int val) { }

    /**
     * Placeholder required by ViewJFrameBase.
     *
     * @param       val  alpha blending value
     *
     * @deprecated  DOCUMENT ME!
     */
    public void setAlphaBlend(int val) { }

    /**
     * Placeholder.
     */
    public void setControls() { }

    /**
     * Controls whether or not the images/VOIs of the frame can be modified.
     *
     * @param  flag  if true the image/VOIs can be modified; if false image/VOIs can NOT be modified
     */
    public void setEnabled(boolean flag) {

        if (componentImage != null) {
            componentImage.setEnabled(flag);
        }
    }

    /**
     * Placeholder required by ViewJFrameBase.
     *
     * @param       imB  the second image
     *
     * @deprecated  DOCUMENT ME!
     */
    public void setImageB(ModelImage imB) { }

    /**
     * Sets the LUT for image A.
     *
     * @param  LUT  the LUT
     */
    public void setLUTa(ModelLUT LUT) {
        componentImage.setLUTa(LUT);
        super.setLUTa(LUT);
    }

    /**
     * Placeholder required by ViewJFrameBase.
     *
     * @param       val  whether to show the paint mask
     *
     * @deprecated  DOCUMENT ME!
     */
    public void setPaintBitmapSwitch(boolean val) { }

    /**
     * Sets the RGB LUT table for ARGB image A.
     *
     * @param  RGBT  the new RGB LUT to be applied to the image
     */
    public void setRGBTA(ModelRGB RGBT) {

        if (componentImage != null) {
            componentImage.setRGBTA(RGBT);
        }
    }

    /**
     * Placeholder required by ViewJFrameBase.
     *
     * @param       val  the RGBTB data
     *
     * @deprecated  DOCUMENT ME!
     */
    public void setRGBTB(ModelRGB val) { }

    /**
     * Placeholder required by ViewJFrameBase.
     *
     * @param       val  the slice
     *
     * @deprecated  DOCUMENT ME!
     */
    public void setSlice(int val) { }

    /**
     * Placeholder required by ViewJFrameBase.
     *
     * @param       val  the time slice
     *
     * @deprecated  DOCUMENT ME!
     */
    public void setTimeSlice(int val) { }

    /**
     * Sets the title of the frame with the image name of slice location.
     */
    public void setTitle() {
        String str;

        str = mipImage.getImageName() + "  M:" + makeString(componentImage.getZoomX(), 2);
        setTitle(str);
        userInterface.getMainFrame().setTitle(str);
    }

    /**
     * Updates the this frame's size the compnents sizes. If the magnified image fits into the frame that will fit into
     * the screen then frame and image are sized appropriately. If the frame, to fit the image, exceeds the screen size
     * the frame remains the same size and the image magnified and placed in the scroll pane.
     *
     * @param  sX  zoom in the x dimension
     * @param  sY  zoom in the y dimension
     */
    public void updateFrame(float sX, float sY) {
        componentImage.setZoom(sX, sY);
        updateImages(false);

        if (((componentImage.getSize(null).width + 200) > xScreen) ||
                ((componentImage.getSize(null).height + 200) > yScreen)) { }
        else if ((getSize().width > componentImage.getSize(null).width) ||
                     (getSize().height > componentImage.getSize(null).height)) {
            removeComponentListener(this);
            scrollPane.setSize(componentImage.getSize(null).width + scrollPane.getInsets().left +
                               scrollPane.getInsets().right,
                               componentImage.getSize(null).height + scrollPane.getInsets().top +
                               scrollPane.getInsets().bottom);

            setSize(scrollPane.getSize().width + getInsets().left + getInsets().right + fudgeFactor,
                    scrollPane.getSize().height + getInsets().top + fudgeFactor + getInsets().bottom);
            addComponentListener(this);
        } else {

            if (((componentImage.getSize(null).width + 200) < xScreen) &&
                    ((componentImage.getSize(null).height + 200) < yScreen)) {
                removeComponentListener(this);
                scrollPane.setSize(componentImage.getSize(null).width + scrollPane.getInsets().left +
                                   scrollPane.getInsets().right,
                                   componentImage.getSize(null).height + scrollPane.getInsets().top +
                                   scrollPane.getInsets().bottom);

                setSize(scrollPane.getSize().width + getInsets().left + fudgeFactor + getInsets().right,
                        scrollPane.getSize().height + getInsets().top + fudgeFactor + getInsets().bottom);
                addComponentListener(this);
            } else {
                removeComponentListener(this);
                scrollPane.setSize(getSize().width - getInsets().left - // wth??
                                   getInsets().right - 1, getSize().height - getInsets().top - getInsets().bottom - 1);
                addComponentListener(this);
            }
        }

        validate();
        setTitle();
        updateImages(false);
    }

    /**
     * This methods calls the componentImage's REPAINT method to redraw the screen. The extents on this image have
     * changed, so the extents need to be read in again and menus, panes and slide bars adjusted accordingly.
     *
     * @return  whether the update was successful.
     */
    public boolean updateImageExtents() {

        // update the image buffers since their sizes may have changed
        int[] extents = null;

        extents = new int[2];
        extents[0] = Math.round(mipExtents[0]);
        extents[1] = Math.round(mipExtents[1]);

        int bufferFactor = 1;

        if (mipImage.isColorImage()) {
            bufferFactor = 4;
        }

        imageBuffer = new float[bufferFactor * imageA.getSliceSize()];
        pixBuffer = new int[extents[0] * extents[1]];

        // initComponentImage();
        componentImage.setBuffers(imageBuffer, null, pixBuffer, null);

        if (resols[1] >= resols[0]) {
            componentImage.setResolutions(1, heightResFactor);
        } else {
            componentImage.setResolutions(widthResFactor, 1);
        }

        // reset the title, since dimensions may have changed
        setTitle();

        return true;

    } // end updateImageExtents()

    /**
     * This methods calls the componentImage's update method to redraw the screen - fastest of the three update methods.
     *
     * @return  boolean confirming successful update
     */
    public boolean updateImages() {

        if (componentImage == null) {
            return false;
        }

        try {
            componentImage.paintComponent(componentImage.getGraphics());
            // componentImage.repaint(); // problems with this method on some machines seems to eat lots of  memory on
            // JVM 1.3
        } catch (OutOfMemoryError error) {
            System.gc();
        }

        return true;
    }

    /**
     * This methods calls the componentImage's update method to redraw the screen. Without LUT changes.
     *
     * @param   forceShow  forces show to re import image and calc. java image
     *
     * @return  boolean confirming successful update
     */
    public boolean updateImages(boolean forceShow) {

        if (componentImage == null) {
            return false;
        }

        if (componentImage.show(0, 0, null, null, forceShow, -1) == false) {
            return false;
        }

        return true;
    }

    /**
     * This methods calls the componentImage's update method to redraw the screen.
     *
     * @param   _LUTa       LUT used to update imageA (and mipImage)
     * @param   _LUTb       LUT used to update imageB
     * @param   forceShow   forces show to re import image and calc. java image
     * @param   interpMode  image interpolation method (Nearest or Smooth)
     *
     * @return  boolean confirming successful update
     */
    public boolean updateImages(ModelLUT _LUTa, ModelLUT _LUTb, boolean forceShow, int interpMode) {

        if (componentImage == null) {
            return false;
        }

        if (componentImage.show(0, 0, _LUTa, _LUTb, forceShow, interpMode) == false) {
            return false;
        }

        // update the luts in this frame
        if (_LUTa != null) {
            setLUTa(_LUTa);
        }

        if (_LUTb != null) {
            setLUTb(_LUTb);
        }

        return true;
    }

    /**
     * Closes window and disposes of frame and component.
     *
     * @param  event  Event that triggered function
     */
    public void windowClosing(WindowEvent event) {
        close();
    }

    /**
     * Calculates the 3D euclidian distance between two points.
     *
     * @param   pt1  first point
     * @param   pt2  second point
     *
     * @return  returns the distance
     */
    protected static final double distance(Point3Df pt1, Point3Df pt2) {
        return Math.sqrt(((pt2.x - pt1.x) * (pt2.x - pt1.x)) + ((pt2.y - pt1.y) * (pt2.y - pt1.y)) +
                         ((pt2.z - pt1.z) * (pt2.z - pt1.z)));
    }

    /**
     * Construct the component image which contains the MIP image (should be called after calcMIPBuffer).
     */
    private void initComponentImage() {

        // init resolutions
        resols = imageA.getFileInfo(0).getResolutions();
        units = imageA.getFileInfo(0).getUnitsOfMeasure();

        for (int r = 0; r < imageA.getNDims(); r++) {

            if (resols[r] < 0) {
                resols[r] = Math.abs(resols[r]);
            } else if (resols[r] == 0) {
                resols[r] = 1.0f;
            }
        }

        widthResFactor = 1.0f;
        heightResFactor = 1.0f;

        if ((resols[1] >= resols[0]) && (resols[1] < (20.0f * resols[0])) && (units[0] == units[1])) {
            heightResFactor = resols[1] / resols[0];
        } else if ((resols[0] > resols[1]) && (resols[0] < (20.0f * resols[1])) && (units[0] == units[1])) {
            widthResFactor = resols[0] / resols[1];
        }

        // init zoom
        float zoomX = 1, zoomY = 1;

        // get the screen size
        xScreen = Toolkit.getDefaultToolkit().getScreenSize().width;
        yScreen = Toolkit.getDefaultToolkit().getScreenSize().height;

        if ((mipExtents[0] * widthResFactor) > (xScreen - 300)) {
            zoomX = (xScreen - 300.0f) / (mipExtents[0] * widthResFactor);
        }

        if ((mipExtents[1] * heightResFactor) > (yScreen - 300)) {
            zoomY = (yScreen - 300.0f) / (mipExtents[1] * heightResFactor);
        }

        zoom = Math.min(zoomX, zoomY);

        if (zoom < 1) {

            if (zoom > 0.5) {
                zoom = 0.5f;
            } else if (zoom > 0.25) {
                zoom = 0.25f;
            } else if (zoom > 0.125) {
                zoom = 0.125f;
            } else if (zoom > 0.0625) {
                zoom = 0.0625f;
            } else if (zoom > 0.03125) {
                zoom = 0.03125f;
            } else {
                zoom = 0.015625f;
            }
        }

        // init LUT
        // if LUTa is null then make a LUT
        if (LUTa == null) {
            int[] dimExtentsLUT = new int[2];

            dimExtentsLUT[0] = 4;
            dimExtentsLUT[1] = 256;

            LUTa = new ModelLUT(ModelLUT.GRAY, 256, dimExtentsLUT);

            float min, max;

            if (mipImage.getType() == ModelStorageBase.UBYTE) {
                min = 0;
                max = 255;
            } else if (mipImage.getType() == ModelStorageBase.BYTE) {
                min = -128;
                max = 127;
            } else {
                min = (float) mipImage.getMin();
                max = (float) mipImage.getMax();
            }

            float imgMin = (float) mipImage.getMin();
            float imgMax = (float) mipImage.getMax();

            LUTa.resetTransferLine(min, imgMin, max, imgMax);
        } // end if LUTa is null

        // init buffers
        int[] extents = null;

        extents = new int[2];
        extents[0] = Math.round(mipExtents[0]);
        extents[1] = Math.round(mipExtents[1]);

        pixBuffer = new int[extents[0] * extents[1]];

        // init component image
        componentImage = new ViewJComponentEditImage(this, mipImage, LUTa, mipBuffer, null, null, null, pixBuffer, zoom,
                                                     extents, logMagDisplay, ViewJComponentEditImage.NA );

        componentImage.setBuffers(mipBuffer, null, pixBuffer, null);
        componentImage.resetLUTs();

        if (resols[1] >= resols[0]) {
            componentImage.setResolutions(1, heightResFactor);
        } else {
            componentImage.setResolutions(widthResFactor, 1);
        }
    }
}
