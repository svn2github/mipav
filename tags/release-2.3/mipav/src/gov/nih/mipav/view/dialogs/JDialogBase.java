package gov.nih.mipav.view.dialogs;


import gov.nih.mipav.model.structures.*;
import gov.nih.mipav.model.file.*;
import gov.nih.mipav.view.*;

import java.awt.event.*;
import java.awt.*;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.border.*;


/**
 *
 *       This class is the base for all the other dialogs.  It has two
 *       important functions that are used by almost all the dialogs.  It
 *       also implements all the listeners except for the action listener.
 *
 *     @version    1.0 Aug 1, 1998
 *     @author     Neva Cherniavsky
 *     @author     Matthew J. McAuliffe, Ph.D.
 *
 */
public abstract class JDialogBase extends JDialog
    implements ActionListener, WindowListener, FocusListener, ItemListener {

    /**
     * REPLACE indicates the current image is replaced after algorithm is run.
     */
    protected static final int REPLACE = 0;

    /**
     * NEW indicates a new image is created after the algorithm is run.
     */
    protected static final int NEW = 1;

    /**
     * OK button is used on most dialogs.  Defining it in the base allows default actions if
     * the user presses return and the button is in focus.
     */
    protected JButton OKButton;

    /**
     * Cancel button is used on most dialogs.  Defining it in the base allows default actions if
     * the user presses return and the button is in focus.
     */
    protected JButton cancelButton;

    /**
     * Close button is used to close the dialog.
     */
    protected JButton closeButton;

    /**
     * Apply button is used to apply the setting of the dialog.
     */
    protected JButton applyButton;

    /**
     * Help button is used on most dialogs.  Defining it in the base allows default actions if
     * the user presses return and the button is in focus.
     */
    protected JButton helpButton;

    /**
     * Parent frame of this dialog, usually of type ViewJFrameImage.
     */
    protected Frame parentFrame;

    /**
     * Flag indicating if the dialog had been cancelled or not.
     */
    public boolean cancelFlag;

    /**
     * Fonts, same as <code>MipavUtil.font12</code> and <code>MipavUtil.font12B.</code>
     */
    protected Font serif12, serif12B;

    /**
     * Flag indicating if the algorithm should run in a separate thread.  Default is <code>true</code>.
     */
    protected boolean runInSeparateThread = true;

    /**
     * Whether we are using this dialog as part of a script.
     */
    protected boolean runningScriptFlag = false;

    /**
     * Whether this is an active image script or a multi-image script (always true if not a script, i think).
     */
    protected boolean isActiveImage = true;

    /**
     * The main panel of the dialog. To be filled by inheriting classes. Defaults to BorderLayout since
     * that's what the contentPane defaults to (makes it easier to change over to using mainDialogPanel
     * without messing up the layout).
     */
    protected JPanel mainDialogPanel = new JPanel( new BorderLayout() );

    /**
     *	Constructor that sets the parent frame of the dialog and whether or not the dialog is modal.
     *	Also adds this as a window listener to all dialogs.
     *	@param parent        Parent frame.
     *	@param modal         Modality of the dialog; <code>true</code> means the user can't do anything until
     *						 this dialog is diposed of.
     */
    public JDialogBase( Frame parent, boolean modal ) {
        super( parent, modal );
        parentFrame = parent;
        serif12 = MipavUtil.font12;
        serif12B = MipavUtil.font12B;
        addWindowListener( this );

        // bind ENTER to okay button, ESC to cancel button
        Action okAction = new OKAction();
        Action cancelAction = new CancelAction();
        //Action helpAction = new HelpAction();
        mainDialogPanel.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( "ENTER" ), "OK" );
        mainDialogPanel.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( "ESCAPE" ), "Cancel" );
        //mainDialogPanel.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( "ENTER" ), "Help" );
        mainDialogPanel.getActionMap().put( "OK", okAction );
        mainDialogPanel.getActionMap().put( "Cancel", cancelAction );
        //mainDialogPanel.getActionMap().put( "Help", helpAction );
    }

    /**
     *	Constructor that forwards the parent dialog whether or not the dialog is modal.
     *	Also adds this as a window listener to all dialogs.
     *	@param parent       Parent Dialog.  Unlike the <code>JDialog(Frame, boolean)</code>
     *                       constructor, this method merely forwards the parent/owner to
     *                       the super-class, and not store a reference locally.
     *                       a higher level, but does not store the
     *	@param modal        Modality of the dialog; <code>true</code> means the user can't do anything until
     *						this dialog is diposed of.
     */
    public JDialogBase( Dialog parent, boolean modal ) {
        super( parent, modal );
        serif12 = MipavUtil.font12;
        serif12B = MipavUtil.font12B;
        addWindowListener( this );
        // setIconImage(MipavUtil.getIconImage(Preferences.getIconName()));

        // bind ENTER to okay button, ESC to cancel button
        Action okAction = new OKAction();
        Action cancelAction = new CancelAction();
        //Action helpAction = new HelpAction();
        mainDialogPanel.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( "ENTER" ), "OK" );
        mainDialogPanel.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( "ESCAPE" ), "Cancel" );
        //mainDialogPanel.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( "ENTER" ), "Help" );
        mainDialogPanel.getActionMap().put( "OK", okAction );
        mainDialogPanel.getActionMap().put( "Cancel", cancelAction );
        //mainDialogPanel.getActionMap().put( "Help", helpAction );
    }

    /**
     *	Constructor that only sets the dialog to modal or not modal.  In this case the parent frame is null.
     *	Also adds this as a window listener.
     *	@param modal			<code>true</code> indicates modal dialog, <code>false</code> otherwise.
     */
    public JDialogBase( boolean modal ) {
        this();
        // setIconImage(MipavUtil.getIconImage(Preferences.getIconName()));
        super.setModal( modal );
    }

    /**
     *   Constructor that sets the dialog to not modal.  There is no parent frame.
     *   No visible dialogs should use this constructor - only for scripting, where
     *   the dialogs are hidden and simply store information.
     */

    public JDialogBase() {
        super();
        super.setModal( false );
        serif12 = MipavUtil.font12;
        serif12B = MipavUtil.font12B;
        addWindowListener( this );
        // setIconImage(MipavUtil.getIconImage(Preferences.getIconName()));

        // bind ENTER to okay button, ESC to cancel button
        Action okAction = new OKAction();
        Action cancelAction = new CancelAction();
        //Action helpAction = new HelpAction();
        mainDialogPanel.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( "ENTER" ), "OK" );
        mainDialogPanel.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( "ESCAPE" ), "Cancel" );
        //mainDialogPanel.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( "ENTER" ), "Help" );
        mainDialogPanel.getActionMap().put( "OK", okAction );
        mainDialogPanel.getActionMap().put( "Cancel", cancelAction );
        //mainDialogPanel.getActionMap().put( "Help", helpAction );
    }

    /**
     *	Makes the dialog visible in center of screen.
     *	@param status      Flag indicating if the dialog should be visible.
     */
    public void setVisible( boolean status ) {
        if (status == true && isVisible() == false)
        {
            // move to center if not yet visible. note that if 'status' param is false
            // the dialog should not be centered, because centering it just before
            // it is made invisible will cause it to flash on screen briefly in the
            // center of the screen. its not noticable if the dialog is already
            // centered, but if the user has moved the dialog, it is annoying to see.
            MipavUtil.centerOnScreen(this);
        }
        super.setVisible(status);
    }

    /**
     *	Makes the dialog visible in center of screen.
     *	@param status      Flag indicating if the dialog should be visible.
     */
    public void setVisible( ) {

        super.setVisible( true );
    }


    /**
     *	Makes the dialog visible by calling super method.  No location set.
     *	@param status      Flag indicating if the dialog should be visible.
     */
    public void setVisibleStandard( boolean status ) {
        super.setVisible( status );
    }

    /**
     *	Accessor that returns whether or not the dialog has been cancelled.
     *	@return          <code>true</code> indicates cancelled, <code>false</code> indicates not cancelled.
     */
    public boolean isCancelled() {
        return cancelFlag;
    }

    /**
     *	Accessor that sets the separate thread flag.
     *	@param flag			<code>true</code> indicates run in separate thread, <code>false<code> otherwise.
     */
    public void setSeparateThread( boolean flag ) {
        runInSeparateThread = flag;
    }

    public void setActiveImage( boolean act ) {
        this.isActiveImage = act;
    }

    /*
     *   Sets the world coordinate flag. If true, change matrix to the world coordinate system.
     */
    public void setWCSystem( boolean wcSys ) {}

    /**
     *   Sets the left-hand coordinate flag. If true, change matrix to the left-hand coordinate system.
     */
    public void setLeftHandSystem( boolean leftHandSys ) {}

    /**
     *	Unchanged.
     */
    public void windowOpened( WindowEvent event ) {}

    /**
     *	Disposes of error dialog, then frame.  Sets cancelled to <code>true</code>.
     */
    public void windowClosing( WindowEvent event ) {
        cancelFlag = true;
        dispose();
    }

    /**
     *	Unchanged.
     */
    public void windowClosed( WindowEvent event ) {}

    /**
     *	Unchanged.
     */
    public void windowIconified( WindowEvent event ) {}

    /**
     *	Unchanged.
     */
    public void windowDeiconified( WindowEvent event ) {}

    /**
     *	Unchanged.
     */
    public void windowActivated( WindowEvent event ) {}

    /**
     *	Unchanged.
     */
    public void windowDeactivated( WindowEvent event ) {}

    /**
     *	Unchanged.
     */
    public void focusGained( FocusEvent event ) {}

    /**
     *	Unchanged.
     */
    public void focusLost( FocusEvent event ) {}

    /**
     *	Unchanged.
     */
    public void itemStateChanged( ItemEvent event ) {}

    /**
     * Handler for keys which should invoke the OK button (such as ENTER). Should be registered by
     * inheriting classes on their main JPanel using getInputMap().put() and getActionMap().put().
     */
    protected class OKAction extends AbstractAction {
        /**
         * Key action event handler.
         * @param event  key action event
         */
        public void actionPerformed( ActionEvent event ) {
            if ( OKButton != null ) {
                ( (JDialogBase) OKButton.getTopLevelAncestor() ).actionPerformed( new ActionEvent( OKButton, 1, "OK" ) );
            }
        }
    }

    /**
     * Handler for keys which should invoke the Cancel button (such as ESC). Should be registered by
     * inheriting classes on their main JPanel using getInputMap().put() and getActionMap().put().
     */
    protected class CancelAction extends AbstractAction {
        /**
         * Key action event handler.
         * @param event  key action event
         */
        public void actionPerformed( ActionEvent event ) {
            if ( cancelButton != null ) {
                ( (JDialogBase) cancelButton.getTopLevelAncestor() ).actionPerformed( new ActionEvent( cancelButton, 1, "Cancel" ) );
            }
        }
    }

    /**
     * Handler for keys which should invoke the Help button (such as F1). Should be registered by
     * inheriting classes on their main JPanel using getInputMap().put() and getActionMap().put().
     */
    protected class HelpAction extends AbstractAction {
        /**
         * Key action event handler.
         * @param event  key action event
         */
        public void actionPerformed( ActionEvent event ) {
            if ( helpButton != null ) {
                ( (JDialogBase) helpButton.getTopLevelAncestor() ).actionPerformed( new ActionEvent( helpButton, 1, "Help" ) );
            }
        }
    }

    /**
     *   Copy important file information to resultant image structure.
     *   @param image       Source image.
     *   @param resultImage Resultant image.
     */
    public static void updateFileInfoStatic( ModelImage image, ModelImage resultImage ) {
        FileInfoBase[] fileInfo;

        if ( resultImage.getNDims() == 2 ) {
            fileInfo = resultImage.getFileInfo();
            fileInfo[0].setModality( image.getFileInfo()[0].getModality() );
            fileInfo[0].setFileDirectory( image.getFileInfo()[0].getFileDirectory() );
            // fileInfo[0].setDataType(image.getFileInfo()[0].getDataType());
            fileInfo[0].setEndianess( image.getFileInfo()[0].getEndianess() );
            fileInfo[0].setUnitsOfMeasure( image.getFileInfo()[0].getUnitsOfMeasure() );
            fileInfo[0].setResolutions( image.getFileInfo()[0].getResolutions() );
            fileInfo[0].setExtents( resultImage.getExtents() );
            fileInfo[0].setMax( resultImage.getMax() );
            fileInfo[0].setMin( resultImage.getMin() );
            fileInfo[0].setImageOrientation( image.getImageOrientation() );
            fileInfo[0].setAxisOrientation( image.getFileInfo()[0].getAxisOrientation() );
            fileInfo[0].setTransformID( image.getFileInfo()[0].getTransformID() );
            fileInfo[0].setOrigin( image.getFileInfo()[0].getOrigin() );
            fileInfo[0].setPixelPadValue( image.getFileInfo()[0].getPixelPadValue() );
            fileInfo[0].setPhotometric( image.getFileInfo()[0].getPhotometric() );
        } else if ( resultImage.getNDims() == 3 ) {
            fileInfo = resultImage.getFileInfo();
            for ( int i = 0; i < resultImage.getExtents()[2]; i++ ) {
                int j = Math.min( i, image.getExtents()[2] - 1 );

                fileInfo[i].setModality( image.getFileInfo()[j].getModality() );
                fileInfo[i].setFileDirectory( image.getFileInfo()[j].getFileDirectory() );
                // fileInfo[i].setDataType(image.getFileInfo()[j].getDataType());
                fileInfo[i].setEndianess( image.getFileInfo()[j].getEndianess() );
                fileInfo[i].setUnitsOfMeasure( image.getFileInfo()[j].getUnitsOfMeasure() );
                fileInfo[i].setResolutions( image.getFileInfo()[j].getResolutions() );
                fileInfo[i].setExtents( resultImage.getExtents() );
                fileInfo[i].setMax( resultImage.getMax() );
                fileInfo[i].setMin( resultImage.getMin() );
                fileInfo[i].setImageOrientation( image.getImageOrientation() );
                fileInfo[i].setAxisOrientation( image.getFileInfo()[j].getAxisOrientation() );
                fileInfo[i].setTransformID( image.getFileInfo()[j].getTransformID() );
                fileInfo[i].setOrigin( image.getFileInfo()[j].getOrigin() );
                fileInfo[i].setPixelPadValue( image.getFileInfo()[j].getPixelPadValue() );
                fileInfo[i].setPhotometric( image.getFileInfo()[j].getPhotometric() );
            }
        } else if ( resultImage.getNDims() == 4 ) {
            fileInfo = resultImage.getFileInfo();
            for ( int i = 0; i < resultImage.getExtents()[2] * resultImage.getExtents()[3]; i++ ) {
                fileInfo[i].setModality( image.getFileInfo()[i].getModality() );
                fileInfo[i].setFileDirectory( image.getFileInfo()[i].getFileDirectory() );
                fileInfo[i].setEndianess( image.getFileInfo()[i].getEndianess() );
                fileInfo[i].setUnitsOfMeasure( image.getFileInfo()[i].getUnitsOfMeasure() );
                fileInfo[i].setResolutions( image.getFileInfo()[i].getResolutions() );
                fileInfo[i].setExtents( resultImage.getExtents() );
                fileInfo[i].setMax( resultImage.getMax() );
                fileInfo[i].setMin( resultImage.getMin() );
                fileInfo[i].setImageOrientation( image.getImageOrientation() );
                fileInfo[i].setAxisOrientation( image.getFileInfo()[i].getAxisOrientation() );
                fileInfo[i].setTransformID( image.getFileInfo()[i].getTransformID() );
                fileInfo[i].setOrigin( image.getFileInfo()[i].getOrigin() );
                fileInfo[i].setPixelPadValue( image.getFileInfo()[i].getPixelPadValue() );
                fileInfo[i].setPhotometric( image.getFileInfo()[i].getPhotometric() );
            }
        }

    }

    /**
     *   Copy important file information to resultant image structure.
     *   @param image       Source image.
     *   @param resultImage Resultant image.
     */
    public void updateFileInfo( ModelImage image, ModelImage resultImage ) {
        FileInfoBase[] fileInfo;

        if ( resultImage.getNDims() == 2 ) {
            fileInfo = resultImage.getFileInfo();
            fileInfo[0].setModality( image.getFileInfo()[0].getModality() );
            fileInfo[0].setFileDirectory( image.getFileInfo()[0].getFileDirectory() );
            // fileInfo[0].setDataType(image.getFileInfo()[0].getDataType());
            fileInfo[0].setEndianess( image.getFileInfo()[0].getEndianess() );
            fileInfo[0].setUnitsOfMeasure( image.getFileInfo()[0].getUnitsOfMeasure() );
            fileInfo[0].setResolutions( image.getFileInfo()[0].getResolutions() );
            fileInfo[0].setExtents( resultImage.getExtents() );
            fileInfo[0].setMax( resultImage.getMax() );
            fileInfo[0].setMin( resultImage.getMin() );
            fileInfo[0].setImageOrientation( image.getImageOrientation() );
            fileInfo[0].setAxisOrientation( image.getFileInfo()[0].getAxisOrientation() );
            fileInfo[0].setTransformID( image.getFileInfo()[0].getTransformID() );
            fileInfo[0].setOrigin( image.getFileInfo()[0].getOrigin() );
            fileInfo[0].setPixelPadValue( image.getFileInfo()[0].getPixelPadValue() );
            fileInfo[0].setPhotometric( image.getFileInfo()[0].getPhotometric() );
        } else if ( resultImage.getNDims() == 3 ) {
            fileInfo = resultImage.getFileInfo();
            for ( int i = 0; i < resultImage.getExtents()[2]; i++ ) {
                int j = Math.min( i, image.getExtents()[2] - 1 );

                fileInfo[i].setModality( image.getFileInfo()[j].getModality() );
                fileInfo[i].setFileDirectory( image.getFileInfo()[j].getFileDirectory() );
                // fileInfo[i].setDataType(image.getFileInfo()[j].getDataType());
                fileInfo[i].setEndianess( image.getFileInfo()[j].getEndianess() );
                fileInfo[i].setUnitsOfMeasure( image.getFileInfo()[j].getUnitsOfMeasure() );
                fileInfo[i].setResolutions( image.getFileInfo()[j].getResolutions() );
                fileInfo[i].setExtents( resultImage.getExtents() );
                fileInfo[i].setMax( resultImage.getMax() );
                fileInfo[i].setMin( resultImage.getMin() );
                fileInfo[i].setImageOrientation( image.getImageOrientation() );
                fileInfo[i].setAxisOrientation( image.getFileInfo()[j].getAxisOrientation() );
                fileInfo[i].setTransformID( image.getFileInfo()[i].getTransformID() );
                fileInfo[i].setOrigin( image.getFileInfo()[j].getOrigin() );
                fileInfo[i].setPixelPadValue( image.getFileInfo()[j].getPixelPadValue() );
                fileInfo[i].setPhotometric( image.getFileInfo()[j].getPhotometric() );
            }
        } else if ( resultImage.getNDims() == 4 ) {
            fileInfo = resultImage.getFileInfo();
            for ( int i = 0; i < resultImage.getExtents()[2] * resultImage.getExtents()[3]; i++ ) {
                fileInfo[i].setModality( image.getFileInfo()[i].getModality() );
                fileInfo[i].setFileDirectory( image.getFileInfo()[i].getFileDirectory() );
                fileInfo[i].setEndianess( image.getFileInfo()[i].getEndianess() );
                fileInfo[i].setUnitsOfMeasure( image.getFileInfo()[i].getUnitsOfMeasure() );
                fileInfo[i].setResolutions( image.getFileInfo()[i].getResolutions() );
                fileInfo[i].setExtents( resultImage.getExtents() );
                fileInfo[i].setMax( resultImage.getMax() );
                fileInfo[i].setMin( resultImage.getMin() );
                fileInfo[i].setImageOrientation( image.getImageOrientation() );
                fileInfo[i].setAxisOrientation( image.getFileInfo()[i].getAxisOrientation() );
                fileInfo[i].setTransformID( image.getFileInfo()[i].getTransformID() );
                fileInfo[i].setOrigin( image.getFileInfo()[i].getOrigin() );
                fileInfo[i].setPixelPadValue( image.getFileInfo()[i].getPixelPadValue() );
                fileInfo[i].setPhotometric( image.getFileInfo()[i].getPhotometric() );
            }
        }
    }


    /**
     *   Copy important file information to resultant image structure, including data type.
     *   @param image    	Source image.
     *   @param resultImage	Resultant image.
     *	 @param type		Data type to set in file info for the resultImage.
     */
    public void updateFileTypeInfo( ModelImage image, ModelImage resultImage, int type ) {
        FileInfoBase[] fileInfo;

        if ( resultImage.getNDims() == 2 ) {
            fileInfo = resultImage.getFileInfo();
            fileInfo[0].setModality( image.getFileInfo()[0].getModality() );
            fileInfo[0].setFileDirectory( image.getFileInfo()[0].getFileDirectory() );
            fileInfo[0].setDataType( type );
            fileInfo[0].setEndianess( image.getFileInfo()[0].getEndianess() );
            fileInfo[0].setUnitsOfMeasure( image.getFileInfo()[0].getUnitsOfMeasure() );
            fileInfo[0].setResolutions( image.getFileInfo()[0].getResolutions() );
            fileInfo[0].setExtents( resultImage.getExtents() );
            fileInfo[0].setMax( resultImage.getMax() );
            fileInfo[0].setMin( resultImage.getMin() );
            fileInfo[0].setImageOrientation( image.getImageOrientation() );
            fileInfo[0].setPixelPadValue( image.getFileInfo()[0].getPixelPadValue() );
            fileInfo[0].setPhotometric( image.getFileInfo()[0].getPhotometric() );
        } else if ( resultImage.getNDims() == 3 ) {
            fileInfo = resultImage.getFileInfo();
            for ( int i = 0; i < resultImage.getExtents()[2]; i++ ) {
                fileInfo[i].setModality( image.getFileInfo()[i].getModality() );
                fileInfo[i].setFileDirectory( image.getFileInfo()[i].getFileDirectory() );
                fileInfo[i].setDataType( type );
                fileInfo[i].setEndianess( image.getFileInfo()[i].getEndianess() );
                fileInfo[i].setUnitsOfMeasure( image.getFileInfo()[i].getUnitsOfMeasure() );
                fileInfo[i].setImageOrientation( image.getImageOrientation() );
                fileInfo[i].setResolutions( image.getFileInfo()[i].getResolutions() );
                fileInfo[i].setExtents( resultImage.getExtents() );
                fileInfo[i].setMax( resultImage.getMax() );
                fileInfo[i].setMin( resultImage.getMin() );
                fileInfo[i].setPixelPadValue( image.getFileInfo()[i].getPixelPadValue() );
                fileInfo[i].setPhotometric( image.getFileInfo()[i].getPhotometric() );
            }
        } else if ( resultImage.getNDims() == 4 ) {
            fileInfo = resultImage.getFileInfo();
            for ( int i = 0; i < resultImage.getExtents()[2] * resultImage.getExtents()[3]; i++ ) {
                fileInfo[i].setModality( image.getFileInfo()[i].getModality() );
                fileInfo[i].setFileDirectory( image.getFileInfo()[i].getFileDirectory() );
                fileInfo[i].setDataType( type );
                fileInfo[i].setEndianess( image.getFileInfo()[i].getEndianess() );
                fileInfo[i].setImageOrientation( image.getImageOrientation() );
                fileInfo[i].setUnitsOfMeasure( image.getFileInfo()[i].getUnitsOfMeasure() );
                fileInfo[i].setResolutions( image.getFileInfo()[i].getResolutions() );
                fileInfo[i].setExtents( resultImage.getExtents() );
                fileInfo[i].setMax( resultImage.getMax() );
                fileInfo[i].setMin( resultImage.getMin() );
                fileInfo[i].setPixelPadValue( image.getFileInfo()[i].getPixelPadValue() );
                fileInfo[i].setPhotometric( image.getFileInfo()[i].getPhotometric() );
            }
        }

    }

    /**
     *   Copy important file information to resultant image structure, including data type.
     *   @param image   Source image.
     *	 @param type	Data type to set in file info.
     */
    public void updateFileTypeInfo( ModelImage image, int type ) {
        FileInfoBase[] fileInfo;

        if ( image.getNDims() == 2 ) {
            fileInfo = image.getFileInfo();
            fileInfo[0].setDataType( type );
        } else if ( image.getNDims() == 3 ) {
            image.changeExtents(image.getExtents());
            fileInfo = image.getFileInfo();
            for ( int i = 0; i < image.getExtents()[2]; i++ ) {
                fileInfo[i].setDataType(type);
                fileInfo[i].setMax( image.getMax() );
                fileInfo[i].setMin( image.getMin() );
            }
        } else if ( image.getNDims() == 4 ) {
            image.changeExtents(image.getExtents());
            fileInfo = image.getFileInfo();
            for ( int i = 0; i < image.getExtents()[2] * image.getExtents()[3]; i++ ) {
                fileInfo[i].setDataType( type );
                fileInfo[i].setMax( image.getMax() );
                fileInfo[i].setMin( image.getMin() );
            }
        }

    }


    /**
     *   Copy important file information to resultant image structure.
     *   In doing a Forward FFT the result image is the same size or bigger than the original
     *   image because of zero padding.  Set file info for the amount of slices in the
     *   FFT image so the original info will be available for all the slices after the
     *   inverse FFT is performed and so the MIPAV FileInfo operations will work properly
     *   on the FFT image.<p>
     *   Because zero stripping is performed at the end of the
     *   inverse FFT process, the image formed after the inverse FFT will be the same size as
     *   the original image before the forward FFT.
     *   Also used for wavlet images since the wavelet tranform is always larger than the
     *   original source image
     *   @param image    	Source image.
     *   @param resultImage	Resultant image.
     *	@param type			Data type to set in file info.
     */
    public void updateFFTFileInfo( ModelImage image, ModelImage resultImage, int type ) {
        FileInfoBase[] fileInfo;

        if ( resultImage.getNDims() == 2 ) {
            fileInfo = resultImage.getFileInfo();
            fileInfo[0].setModality( image.getFileInfo()[0].getModality() );
            fileInfo[0].setFileDirectory( image.getFileInfo()[0].getFileDirectory() );
            fileInfo[0].setDataType( type );
            fileInfo[0].setImageOrientation( image.getImageOrientation() );
            fileInfo[0].setEndianess( image.getFileInfo()[0].getEndianess() );
            fileInfo[0].setUnitsOfMeasure( image.getFileInfo()[0].getUnitsOfMeasure() );
            fileInfo[0].setResolutions( image.getFileInfo()[0].getResolutions() );
            fileInfo[0].setExtents( resultImage.getExtents() );
            fileInfo[0].setMax( resultImage.getMax() );
            fileInfo[0].setMin( resultImage.getMin() );
            fileInfo[0].setPixelPadValue( image.getFileInfo()[0].getPixelPadValue() );
            fileInfo[0].setPhotometric( image.getFileInfo()[0].getPhotometric() );
        } else if ( resultImage.getNDims() == 3 ) {
            resultImage.changeExtents( resultImage.getExtents() );
            fileInfo = resultImage.getFileInfo();
            for ( int j = 0; j < resultImage.getExtents()[2]; j++ ) {
                int i = Math.min( j, resultImage.getOriginalExtents()[2] - 1 );

                fileInfo[j].setModality( image.getFileInfo()[i].getModality() );
                fileInfo[j].setFileDirectory( image.getFileInfo()[i].getFileDirectory() );
                fileInfo[j].setImageOrientation( image.getImageOrientation() );
                fileInfo[j].setDataType( type );
                fileInfo[j].setEndianess( image.getFileInfo()[i].getEndianess() );
                fileInfo[j].setUnitsOfMeasure( image.getFileInfo()[i].getUnitsOfMeasure() );
                fileInfo[j].setResolutions( image.getFileInfo()[i].getResolutions() );
                fileInfo[j].setExtents( resultImage.getExtents() );
                fileInfo[j].setMax( resultImage.getMax() );
                fileInfo[j].setMin( resultImage.getMin() );
                fileInfo[j].setPixelPadValue( image.getFileInfo()[i].getPixelPadValue() );
                fileInfo[j].setPhotometric( image.getFileInfo()[i].getPhotometric() );
            }
        } else if ( resultImage.getNDims() == 4 ) {
            resultImage.changeExtents( resultImage.getExtents() );
            fileInfo = resultImage.getFileInfo();
            for ( int j = 0; j < resultImage.getExtents()[2] * resultImage.getExtents()[3]; j++ ) {
                int i = Math.min( j, resultImage.getOriginalExtents()[2] * resultImage.getOriginalExtents()[3] - 1 );

                fileInfo[j].setModality( image.getFileInfo()[i].getModality() );
                fileInfo[j].setImageOrientation( image.getImageOrientation() );
                fileInfo[j].setFileDirectory( image.getFileInfo()[i].getFileDirectory() );
                fileInfo[j].setDataType( type );
                fileInfo[j].setEndianess( image.getFileInfo()[i].getEndianess() );
                fileInfo[j].setUnitsOfMeasure( image.getFileInfo()[i].getUnitsOfMeasure() );
                fileInfo[j].setResolutions( image.getFileInfo()[i].getResolutions() );
                fileInfo[j].setExtents( resultImage.getExtents() );
                fileInfo[j].setMax( resultImage.getMax() );
                fileInfo[j].setMin( resultImage.getMin() );
                fileInfo[j].setPixelPadValue( image.getFileInfo()[i].getPixelPadValue() );
                fileInfo[j].setPhotometric( image.getFileInfo()[i].getPhotometric() );
            }
        }
    }

    /**
     *  Tests that the entered parameter is larger than the specified value.
     *  @param str        The value entered by the user.
     *  @param minValue   The minimum value this variable may be set to.
     *  @return           <code>true</code> if parameters passed range test, <code>false</code> if failed.
     */
    protected boolean testParameterMin( String str, double minValue ) {
        double tmp;

        try {
            tmp = Double.valueOf( str ).doubleValue();
            if ( tmp < minValue ) {
                MipavUtil.displayError( "Value is smaller than " + String.valueOf( minValue ) );
                return false;
            } else {
                return true;
            }
        } catch ( NumberFormatException error ) {
            MipavUtil.displayError( "Must enter numeric value" );
            return false;
        }
    }

    /**
     *  Tests that the entered parameter is in range.
     *  @param str        The value entered by the user.
     *  @param minValue   The minimum value this variable may be set to.
     *  @param maxValue   The maximum value this variable may be set to.
     *  @return           <code>true</code> if parameters passed range test, <code>false</code> if failed.
     */
    protected boolean testParameter( String str, double minValue, double maxValue ) {
        double tmp;

        try {
            tmp = Double.valueOf( str ).doubleValue();
            if ( tmp > maxValue || tmp < minValue ) {
                MipavUtil.displayError(
                        "Value is out of range: " + String.valueOf( minValue ) + " , " + String.valueOf( maxValue ) );
                return false;
            } else {
                return true;
            }
        } catch ( NumberFormatException error ) {
            MipavUtil.displayError( "Must enter numeric value" );
            return false;
        }
    }

    /**
     *   Makes a string of a floating point number with a specific number of decimal points.
     *   @param number  Number to be converted to a string.
     *   @param decPts  The number of decimal points.
     *   @return        String representation of the number.
     */
    public String makeString( float number, int decPts ) {

        int index = String.valueOf( number ).indexOf( "." );
        int length = String.valueOf( number ).length();

        if ( index + decPts < length ) {
            return ( String.valueOf( number ).substring( 0, index + decPts + 1 ) );
        } else {
            return ( String.valueOf( number ) );
        }
    }

    /**
     *	Helper method for making the result image's name.  Strips the current extension from the original name,
     *	adds the given extension, and returns the new name.
     *	@param ext	Extension to add which gives information about what algorithm was performed on the image.
     *	@return		The new image name.
     */
    public static String makeImageName( String image_name, String ext ) {
        String name;
        int index = image_name.indexOf( "." );

        if ( index == -1 ) {
            name = image_name;
        } else {
            name = image_name.substring( 0, index );
        } // Used for setting image name
        name += ext;
        return name;
    }

    /**
     *	Builds a list of images.  Returns combobox.
     *	@return	Newly created combo box.
     */
    protected JComboBox buildImageComboBox( ModelImage image ) {
        ViewUserInterface UI;
        ModelImage img;

        JComboBox comboBox = new JComboBox();

        comboBox.setFont( serif12 );
        comboBox.setBackground( Color.white );

        UI = image.getUserInterface();
        Enumeration names = UI.getRegisteredImageNames();

        while ( names.hasMoreElements() ) {
            String name = (String) names.nextElement();

            try {
                img = UI.getRegisteredImageByName( name );
                if ( UI.getFrameContainingImage( img ) != null ) {
                    if ( !name.equals( image.getImageName() ) ) {
                        comboBox.addItem( name );
                    }
                }
            } catch ( IllegalArgumentException iae ) {
                // MipavUtil.displayError("There was a problem with the supplied name.\n" );
                Preferences.debug(
                        "Illegal Argument Exception in " + "JDialogBase.buildImageComboBox(). "
                        + "Somehow the Image list sent an incorrect name to " + "the image image hashtable. " + "\n",
                        1 );
                System.out.println( "Bad argument." );
            }
        }
        return comboBox;
    }

    /**
     *   Builds button panel consisting of OK, Cancel and Help buttons.
     */
    protected JPanel buildButtons() {
        JPanel buttonPanel = new JPanel();

        buttonPanel.add( buildOKButton() );
        buttonPanel.add( buildCancelButton() );
        buttonPanel.add( buildHelpButton() );
        return buttonPanel;
    }

    /**
     *  Builds the OK button.  Sets it internally as well
     *  return the just-built button.
     */
    protected JButton buildOKButton() {
        OKButton = new JButton( "OK" );
        OKButton.addActionListener( this );
        // OKButton.setToolTipText("Accept values and perform action.");
        OKButton.setMinimumSize( MipavUtil.defaultButtonSize );
        OKButton.setPreferredSize( MipavUtil.defaultButtonSize );
        OKButton.setFont( serif12B );
        return OKButton;
    }

    /**
     *  Builds the cancel button.  Sets it internally as well
     *  return the just-built button.
     */
    protected JButton buildCancelButton() {
        cancelButton = new JButton( "Cancel" );
        cancelButton.addActionListener( this );
        // cancelButton.setToolTipText("Cancel action.");
        cancelButton.setMinimumSize( MipavUtil.defaultButtonSize );
        cancelButton.setPreferredSize( MipavUtil.defaultButtonSize );
        cancelButton.setFont( serif12B );
        return cancelButton;
    }

    /**
     *  Builds the close button.  Sets it internally as well
     *  return the just-built button.
     */
    protected JButton buildCloseButton() {
        closeButton = new JButton( "Close" );
        closeButton.addActionListener( this );
        closeButton.setToolTipText( "Close dialog." );
        closeButton.setMinimumSize( MipavUtil.defaultButtonSize );
        closeButton.setPreferredSize( MipavUtil.defaultButtonSize );
        closeButton.setFont( serif12B );
        return closeButton;
    }

    /**
     *  Builds the cancel button.  Sets it internally as well
     *  return the just-built button.
     */
    protected JButton buildApplyButton() {
        applyButton = new JButton( "Apply" );
        applyButton.addActionListener( this );
        applyButton.setToolTipText( "Apply settings." );
        applyButton.setMinimumSize( MipavUtil.defaultButtonSize );
        applyButton.setPreferredSize( MipavUtil.defaultButtonSize );
        applyButton.setFont( serif12B );
        return applyButton;
    }

    /**
     *  Builds the help button.  Sets it internally as well
     *  return the just-built button.
     */
    protected JButton buildHelpButton() {
        helpButton = new JButton( "Help" );
        helpButton.addActionListener( this );
        // helpButton.setToolTipText("Find help for this screen.");
        helpButton.setMinimumSize( MipavUtil.defaultButtonSize );
        helpButton.setPreferredSize( MipavUtil.defaultButtonSize );
        helpButton.setFont( serif12B );
        return helpButton;
    }

    /**
     *  Helper method to create a label with the proper font and font color.
     *  @param title	Text of the label.
     *  @return			New label.
     */
    protected JLabel createLabel(String title) {
        JLabel label = new JLabel(title);
        label.setFont(serif12);
        label.setForeground(Color.black);
        return label;
    }

    /**
     *  Helper method to create a text field with the proper font and font color.
     *  @param title	Text int the field.
     *  @return		New text field.
     */
    protected JTextField createTextField(String title) {
        JTextField tf = new JTextField(title);
        tf.setFont(serif12);
        tf.setForeground(Color.black);
        return tf;
    }



    /**
     *   Builds a titled border with the given title, an etched border,
     *   and the proper font and color.
     *   @param title    Title of the border
     *   @return         The titled border.
     */
    protected TitledBorder buildTitledBorder( String title ) {
        return new TitledBorder( new EtchedBorder(), title, TitledBorder.LEFT, TitledBorder.CENTER, MipavUtil.font12B,
                Color.black );
    }

    /**
     * Returns the name of the dialog (e.g. JDialogBase -> Base)
     * @return String dialog name
     */
    protected String getDialogName() {
      String name = new String();

      name = this.getClass().getName();
      try {
        name = name.substring( (name.lastIndexOf(".") + 8), name.length());
      }
      catch (Exception ex) {
        return "Not found";
      }

      return name;
    }

    /**
     * Returns whether the current dialog is being run from within a script.
     * @return  whether a script is running
     */
    protected boolean isScriptRunning() {
        return runningScriptFlag;
    }

    /**
     * Sets whether the dialog is being run by a script.
     * @param flag  whether a script is being executed
     */
    protected void setScriptRunning( boolean flag ) {
        runningScriptFlag = flag;
    }
}
