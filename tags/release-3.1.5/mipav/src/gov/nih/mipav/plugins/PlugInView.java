package gov.nih.mipav.plugins;


import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;

import java.awt.*;


/**
 * DOCUMENT ME!
 */
public interface PlugInView extends PlugIn {

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * run.
     *
     * @param       UI           MIPAV main user interface.
     * @param       parentFrame  frame that displays the MIPAV image. Can be used as a parent frame when building
     *                           dialogs.
     * @param       image        model of the MIPAV image.
     *
     * @see         ModelImage
     * @see         ViewJFrameImage
     * @deprecated  Parameter UI will be removed since ViewUserInterface is now a static singelton.
     */
    void run(ViewUserInterface UI, Frame parentFrame, ModelImage image);
}
