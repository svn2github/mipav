package gov.nih.mipav.view;

import gov.nih.mipav.view.dialogs.*;
import gov.nih.mipav.model.structures.*;

import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;

/**
*
*
*		@version    1.0 July 27, 1999
*		@author     Matthew J. McAuliffe, Ph.D. (primary)
*		@author     Harman Singh
*
*/
public class ViewJPopupVOI extends JPanel implements ActionListener, PopupMenuListener, MouseListener {
    private JPopupMenu popup;
    private JMenu orderSubMenu;
    private JMenu contourOrderSubMenu;
    private JMenu editSubMenu;
    private JMenu graphSubMenu;
    private JMenu propSubMenu;
    private VOIHandler voiHandler;
    private JMenuItem itemProps;
    private JMenuItem itemClose;
    private JMenuItem itemShowVOIName;
    private JMenuItem itemOutputDistance;
    private JMenuItem itemCrop;

    public ViewJPopupVOI(VOIHandler handler) {
        voiHandler = handler;

        popup           = new JPopupMenu();
        graphSubMenu    = ViewMenuBuilder.buildMenu("Graph", 0, true);
        orderSubMenu    = ViewMenuBuilder.buildMenu("VOI Order", 0, true);
        contourOrderSubMenu = ViewMenuBuilder.buildMenu("Contour Order", 0, true);
        editSubMenu     = ViewMenuBuilder.buildMenu("Edit", 0, true);
        propSubMenu     = ViewMenuBuilder.buildMenu("Propagate", 0, true);
        itemShowVOIName      = ViewMenuBuilder.buildCheckBoxMenuItem("Show VOI name", "ShowName", this,
            Preferences.is(Preferences.PREF_SHOW_VOI_NAME));


        itemProps = ViewMenuBuilder.buildMenuItem("Properties","Properties",0,this,null, true);
        popup.add(itemProps);

        graphSubMenu.add(ViewMenuBuilder.buildMenuItem("Contour boundary intensity","boundaryIntensity", 0, this, null, false));

        if(handler.getComponentImage().getActiveImage().getNDims() == 3 || handler.getComponentImage().getActiveImage().getNDims() == 4) {
            graphSubMenu.add(ViewMenuBuilder.buildMenuItem("Total intensity","totalIntensity", 0, this, null, false));
            graphSubMenu.add(ViewMenuBuilder.buildMenuItem("Average intensity","avgIntensity", 0, this, null, false));
            graphSubMenu.add(ViewMenuBuilder.buildMenuItem("Total intensity with threshold","totalIntensityThreshold", 0, this, null, false));
            graphSubMenu.add(ViewMenuBuilder.buildMenuItem("Average intensity with threshold","avgIntensityThreshold", 0, this, null, false));
        }

        popup.add(graphSubMenu);
        popup.addSeparator();

        orderSubMenu.add(ViewMenuBuilder.buildMenuItem("Bring VOI to Front","BringToFront",0,this,"front.gif", true));
        orderSubMenu.add(ViewMenuBuilder.buildMenuItem("Send VOI to Back","SendToBack",0,this,"back.gif", true));
        orderSubMenu.add(ViewMenuBuilder.buildMenuItem("Bring VOI Forward","BringForward",0,this,"forward.gif", true));
        orderSubMenu.add(ViewMenuBuilder.buildMenuItem("Send VOI Backward","SendBackward",0,this,"backward.gif", true));
        popup.add(orderSubMenu);

        contourOrderSubMenu.add(ViewMenuBuilder.buildMenuItem("Bring Contour to Front","BringContourToFront",0,this,"front.gif", true));
        contourOrderSubMenu.add(ViewMenuBuilder.buildMenuItem("Send Contour to Back","SendContourToBack",0,this,"back.gif", true));
        contourOrderSubMenu.add(ViewMenuBuilder.buildMenuItem("Bring Contour Forward","BringContourForward",0,this,"forward.gif", true));
        contourOrderSubMenu.add(ViewMenuBuilder.buildMenuItem("Send Contour Backward","SendContourBackward",0,this,"backward.gif", true));
        popup.add(contourOrderSubMenu);


        editSubMenu.add(ViewMenuBuilder.buildMenuItem("Delete","deleteVOI",0,this,"delete.gif", true));
        editSubMenu.add(ViewMenuBuilder.buildMenuItem("Cut","cutVOI",0,this,"cutpaint.gif", true));
        editSubMenu.add(ViewMenuBuilder.buildMenuItem("Copy","copyVOI",0,this,"copypaint.gif", true));
        editSubMenu.add(ViewMenuBuilder.buildMenuItem("Paste","pasteVOI",0,this,"pastepaint.gif", true));
        popup.add(editSubMenu);

        propSubMenu.add(ViewMenuBuilder.buildMenuItem("To Next Slice","PropVOIUp",0,this,"voipropu.gif", true));
        propSubMenu.add(ViewMenuBuilder.buildMenuItem("To Previous Slice","PropVOIDown",0,this,"voipropd.gif", true));
        propSubMenu.add(ViewMenuBuilder.buildMenuItem("To All Slices","PropVOIAll",0,this, null, true));
        popup.add(propSubMenu);

        itemClose = ViewMenuBuilder.buildMenuItem("Close VOI (polyline->polygon)","closeVOI",0,this, null, true);
        popup.add(itemClose);


        itemCrop = ViewMenuBuilder.buildMenuItem("Crop image","cropImage",0,this, null, true);
        popup.add(itemCrop);
        popup.add(itemShowVOIName);

        itemOutputDistance = ViewMenuBuilder.buildMenuItem("Calculate distances -> Output Window", "calcDistances", 0, this, null, true);

      //  popup.add(ViewMenuBuilder.buildCheckBoxMenuItem()
    }

    public void setEnabledProps(boolean flag){
        itemProps.setEnabled(flag);
    }

    public void setEnabledPropagate(boolean flag) {
        propSubMenu.setEnabled(flag);
    }

    public void setEnabledOrder(boolean flag) {
        orderSubMenu.setEnabled(flag);
    }

    public void mousePressed(MouseEvent event)  { checkPopup(event);}
    public void mouseClicked(MouseEvent event)  { checkPopup(event);}
    public void mouseEntered(MouseEvent event)  { }
    public void mouseExited(MouseEvent event)   { }
    public void mouseReleased(MouseEvent event) { checkPopup(event);}

    private void checkPopup(MouseEvent event) {
        if (event.isPopupTrigger()) {

            if (isPLineSliceVOI()) {
                popup.add(itemOutputDistance);
                popup.remove(graphSubMenu);
                popup.remove(contourOrderSubMenu);
                popup.remove(propSubMenu);
                popup.remove(itemShowVOIName);
                popup.remove(itemCrop);
                popup.remove(itemClose);
            } else {
                popup.remove(itemClose);
                popup.remove(itemCrop);

                popup.add(graphSubMenu);
                popup.add(contourOrderSubMenu);
                popup.add(propSubMenu);
                popup.add(itemShowVOIName);
                popup.add(itemCrop);

                if (isVOIOpen()) {
                    popup.add(itemClose);
                } else {
                    popup.add(itemCrop);
                }
                popup.remove(itemOutputDistance);
            }


            itemShowVOIName.setSelected(Preferences.is(Preferences.PREF_SHOW_VOI_NAME));
            popup.show(voiHandler.getComponentImage(), event.getX(), event.getY());
       }
    }


    public void actionPerformed(ActionEvent event) {

        try {
            if (event.getActionCommand().equals("boundaryIntensity")) {
                voiHandler.graphVOI();
            }
            else if (event.getActionCommand().equals("totalIntensity")) {
                voiHandler.graph25VOI_CalcInten(true, false, 0);
            }
            else if (event.getActionCommand().equals("avgIntensity")) {
                voiHandler.graph25VOI_CalcInten(false, false, 0);
            }
            else if (event.getActionCommand().equals("totalIntensityThreshold")) {
              new JDialogIntensityThreshold(voiHandler.getComponentImage().getFrame(),
                                            voiHandler.getComponentImage().getActiveImage().getUserInterface(),
                                            voiHandler.getComponentImage(), false);
            }
            else if (event.getActionCommand().equals("avgIntensityThreshold")) {
              new JDialogIntensityThreshold(voiHandler.getComponentImage().getFrame(),
                                            ViewUserInterface.getReference(),
                                            voiHandler.getComponentImage(), true);
            }

            else if (event.getActionCommand().equals("Properties")) {
                voiHandler.showVOIProperties(false);
            }

            /*
            * Brings the selected VOI to the front.
            * (Moves the selected VOI to the first element of the vector.)
            */

            else if (event.getActionCommand().equals("BringToFront")) {
                voiHandler.changeVOIOrder(false, VOIHandler.FRONT);
            }


            /*
            * Sends the selected VOI to the back.
            * (Moves the selected VOI to the last element of the vector.)
            */
            else if (event.getActionCommand().equals("SendToBack")) {
                voiHandler.changeVOIOrder(false, VOIHandler.BACK);
            }

            /*
            * Brings the selected VOI forward one level.
            * (moves the selected VOI to the one element higher (index number decreases by one) in the list)
            */
            else if (event.getActionCommand().equals("BringForward")) {
                voiHandler.changeVOIOrder(false, VOIHandler.FORWARD);
            }

            /*
            * Send the selected VOI back one level.
            * (moves the selected VOI to the one element lower (index number increases by one) in the list)
            */
            else if (event.getActionCommand().equals("SendBackward")) {
                voiHandler.changeVOIOrder(false, VOIHandler.BACKWARD);
            }

            /*
            * Brings the selected VOI's contour to the front.
            * (Moves the selected VOI's contour to the first element of the vector.)
            */

            else if (event.getActionCommand().equals("BringContourToFront")) {
                voiHandler.changeVOIOrder(true, VOIHandler.FRONT);
            }


            /*
            * Sends the selected VOI's contour to the back.
            * (Moves the selected VOI's contour to the last element of the vector.)
            */
            else if (event.getActionCommand().equals("SendContourToBack")) {
                voiHandler.changeVOIOrder(true, VOIHandler.BACK);
            }

            /*
            * Brings the selected VOI's contour forward one level.
            * (moves the selected VOI's contour to the one element higher (index number decreases by one) in the list)
            */
            else if (event.getActionCommand().equals("BringContourForward")) {
                voiHandler.changeVOIOrder(true, VOIHandler.FORWARD);
            }

            /*
            * Send the selected VOI's contour back one level.
            * (moves the selected VOI's contour to the one element lower (index number increases by one) in the list)
            */
            else if (event.getActionCommand().equals("SendContourBackward")) {
                voiHandler.changeVOIOrder(true, VOIHandler.BACKWARD);
            }

            else if (event.getActionCommand().equals("cutVOI")){
                     if (voiHandler.copyVOItoClipBrd())
                         voiHandler.deleteSelectedVOI(true);

                }
            else if (event.getActionCommand().equals("copyVOI")){
                    voiHandler.copyVOItoClipBrd();

                }
            else if (event.getActionCommand().equals("pasteVOI")){
                    voiHandler.pasteVOI();
                }
            else if (event.getActionCommand().equals("deleteVOI")){
                    voiHandler.deleteSelectedVOI(true);
            }
            else if (event.getActionCommand().equals("PropVOIUp")){
                if (voiHandler.propVOI(1, false) == true ) {
                    voiHandler.getComponentImage().getActiveImage().getParentFrame().incSlice();
                }
            }
            else if (event.getActionCommand().equals("PropVOIDown")){
                if (voiHandler.propVOI(-1, false) == true) {
                    voiHandler.getComponentImage().getActiveImage().getParentFrame().decSlice();
                }
            }
            else if (event.getActionCommand().equals("PropVOIAll")){
                voiHandler.propVOIAll();
            }
            else if (event.getActionCommand().equals("closeVOI")) {
                closeVOI();
            }
            else if (event.getActionCommand().equals("cropImage")) {
                new JDialogCrop( voiHandler.getComponentImage().getActiveImage().getParentFrame(),
                                 voiHandler.getComponentImage().getActiveImage(),
                                 voiHandler.getComponentImage().getActiveImage().getParentFrame().getViewableSlice() );
            }
            else if (event.getActionCommand().equals("ShowName")) {
                ViewUserInterface.getReference().setUseVOIName(itemShowVOIName.isSelected());
            }
            else if (event.getActionCommand().equals("calcDistances")) {
                voiHandler.calcPLineSliceDistances();
            }

        }


        catch (OutOfMemoryError error) {
            System.gc();
            MipavUtil.displayError("Out of memory: ViewJPopupVOI.action.");
            return;
        }
    }



    public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
        Preferences.debug("Popup menu will be visible!");
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
        Preferences.debug("Popup menu will be invisible!");
    }

    public void popupMenuCanceled(PopupMenuEvent event) {
        Preferences.debug("Popup menu will be visible!");
    }

    private boolean isPLineSliceVOI() {

        VOIVector VOIs = voiHandler.getComponentImage().getActiveImage().getVOIs();

        Vector [] curves = null;
        VOIBase tester = null;
        for (int i = 0; i < VOIs.size(); i++) {
            if (VOIs.VOIAt(i).isActive() &&
                VOIs.VOIAt(i).getCurveType() == VOI.POLYLINE_SLICE) {
                return true;
            }
        }



        return false;
    }

    /**
     * Checks to see if the active VOI is a polyline
     * @return boolean is the active VOI a polyline (not polygon)
     */
    private boolean isVOIOpen() {
        VOIVector VOIs = voiHandler.getComponentImage().getActiveImage().getVOIs();

        Vector [] curves = null;
        VOIBase tester = null;
        for (int i = 0; i < VOIs.size(); i++) {
            if (VOIs.VOIAt(i).isActive()) {
                if (VOIs.VOIAt(i).getCurveType() == VOI.POLYLINE) {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * Sets a POLYLINE VOI to closed CONTOUR(changes from polyline to polygon)
     */
    private void closeVOI() {
        VOIVector VOIs = voiHandler.getComponentImage().getActiveImage().getVOIs();

        Vector [] curves = null;
        VOIBase tester = null;
        for (int i = 0; i < VOIs.size(); i++) {
            if (VOIs.VOIAt(i).isActive()) {
                VOIs.VOIAt(i).setCurveType(VOI.CONTOUR);
                curves = VOIs.VOIAt(i).getCurves();
                for (int j = 0; j < curves.length; j++) {

                    for (int k = 0; k < curves[j].size(); k++) {
                        tester = (VOIBase)curves[j].elementAt(k);

                        if (tester instanceof VOIContour) {
                            ((VOIContour)tester).setClosed(true);
                        }

                    }

                }
                voiHandler.getComponentImage().getActiveImage().notifyImageDisplayListeners( null, true );
                return;
            }
        }
    }

}
