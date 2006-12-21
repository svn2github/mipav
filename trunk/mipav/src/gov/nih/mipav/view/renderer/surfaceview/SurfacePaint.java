package gov.nih.mipav.view.renderer.surfaceview;


import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.*;
import gov.nih.mipav.view.renderer.*;

import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.picking.*;

import java.awt.event.*;

import java.util.*;

import javax.media.j3d.*;

import javax.swing.*;

import javax.vecmath.*;


/*
 * SurfacePaint class performs paint operations on a ModelTriangleMesh
 * surfaces. When the mouse is moved over the surface, the PickCanvas is used
 * to retrieve the picked triangle in the mesh. The triangle vertex colors are
 * set to a user-specified color.
 * 
 * @see SurfaceAttributes.java
 * @see JPanelSurface.java
 */
public class SurfacePaint
    implements MouseListener,
               MouseMotionListener
{

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /**
     * Radius of the paint brush.
     */
    private float m_fRadius = .10f;

    /** Enables painting */
    private boolean m_bEnabled = false;

    /** PickCanvas, for triangle picking. */
    private PickCanvas m_kPickCanvas = null;

//     private ModelTriangleMesh m_kOriginal;
//     private ModelTriangleMesh m_kSurface;
//     private ModelTriangleMesh m_kModified;
//     private ModelTriangleMesh m_kSurfaceBackup;

    /** Reference to the JPanelSurface: */
    private JPanelSurface m_kPanel = null;
    

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /** Default Constructor */
    public SurfacePaint() {}


    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * @param  bAll  bAll, when true clears all the paint operations, when false, clears the last paint:
     */
    public void clear(boolean bAll) {
//         if (bAll) {
//             /* replace the modified mesh with the original: */
//             m_kPanel.replaceMesh(m_kModified, m_kOriginal);
//             m_kSurface = null;
//             m_kModified = null;
//         } else {
//             /* replace the modified mesh with the backup: */
//             m_kPanel.replaceMesh(m_kModified, m_kSurfaceBackup);
//             m_kModified = m_kSurfaceBackup;
//             m_kSurface = m_kSurfaceBackup;
//         }
    }

    /**
     * Deletes all member variables, clean memory.
     */
    public void dispose() {}



    /**
     * One of the overrides necessary to be a MouseListener. This function is invoked when a button has been pressed and
     * released.
     *
     * @param  kMouseEvent  the mouse event generated by a mouse clicked
     */
    public void mouseClicked(MouseEvent kMouseEvent) {}

    /**
     * mouseDragged.
     *
     * @param  kMouseEvent  MouseEvent
     */
    public void mouseDragged(MouseEvent kMouseEvent) {}


    /**
     * One of the overrides necessary to be a MouseListener. Invoked when the mouse enters a component.
     *
     * @param  kMouseEvent  the mouse event generated by a mouse entered
     */
    public void mouseEntered(MouseEvent kMouseEvent) {}

    /**
     * One of the overrides necessary to be a MouseListener. Invoked when the mouse leaves a component.
     *
     * @param  kMouseEvent  the mouse event generated by a mouse exit
     */
    public void mouseExited(MouseEvent kMouseEvent) {}


    /**
     * @param  kMouseEvent  MouseEvent
     */
    public void mouseMoved(MouseEvent kMouseEvent)
    {
        /* Only capture mouse events when enabled, and only when the control
         * key is down and the left mouse button is pressed. */
        if (m_bEnabled &&
            kMouseEvent.isControlDown() )
        {
            /* If the pickCanvas is null, then do not try to pick */
            if (m_kPickCanvas == null) {
                System.err.println( "SurfacePaint.mouseDragged: pickCanvas is null" );
                return;
            }

            /* Set the location for picking that was stored when the mouse was
             * presed: */
            m_kPickCanvas.setShapeLocation(kMouseEvent);

            PickResult kPickResult = null;

            /* Try to get the closest picked polygon, catch the
             * javax.media.j3d.CapabilityNotSetException. */
            try {
                kPickResult = m_kPickCanvas.pickClosest();
            } catch (javax.media.j3d.CapabilityNotSetException e) {
                System.err.println("pickClosest failed: " + e.getMessage());
                return;
            }

            /* If the pickResult is not null, mark the picked point and, if
             * this is the second point in a sequence, then draw the geodesic
             * curve. */
            if (kPickResult != null) {

                /* Pick the first intersection since we executed a pick
                 * closest. */
                PickIntersection kPick = kPickResult.getIntersection(0);

                /* Get the coordinates of the picked point on the mesh. */
                Point3f kPickPoint = new Point3f(kPick.getPointCoordinates());

                /* Get the coordinates of the picked point on the mesh. */
                ModelTriangleMesh kMesh = (ModelTriangleMesh) kPickResult.getGeometryArray();
                int[] indices = kPick.getPrimitiveCoordinateIndices();
                Point3f[] vertices = kMesh.getVertexCopy();
                int closest = indices[0];
                if ( kPickPoint.equals( vertices[ indices[1] ] ) )
                {
                    closest = indices[1];
                }
                if ( kPickPoint.equals( vertices[ indices[2] ] ) )
                {
                    closest = indices[2];
                }
                kMesh.setColor( indices[0], new Color4f( 1f, 0f, 0f, 1f ) );
                kMesh.setColor( indices[1], new Color4f( 1f, 0f, 0f, 1f ) );
                kMesh.setColor( indices[2], new Color4f( 1f, 0f, 0f, 1f ) );

                System.err.println( "kPickPoint: " + kPickPoint.x + " " + kPickPoint.y + " " + kPickPoint.z );
                System.err.println( "kPickPoint: " + vertices[indices[0]].x + " " + vertices[indices[0]].y + " " + vertices[indices[0]].z );
                System.err.println( "kPickPoint: " + vertices[indices[1]].x + " " + vertices[indices[1]].y + " " + vertices[indices[1]].z );
                System.err.println( "kPickPoint: " + vertices[indices[2]].x + " " + vertices[indices[2]].y + " " + vertices[indices[2]].z );
                System.err.println( "Dist1: " + kPickPoint.distance( vertices[indices[0]] ) );
                System.err.println( "Dist2: " + kPickPoint.distance( vertices[indices[1]] ) );
                System.err.println( "Dist3: " + kPickPoint.distance( vertices[indices[2]] ) );
            }

        }
    }

    /**
     * One of the overrides necessary to be a MouseListener. Invoked when a mouse button is pressed.
     *
     * @param  kMouseEvent  the mouse event generated by a mouse press
     */
    public void mousePressed(MouseEvent kMouseEvent) {}


    /**
     * One of the overrides necessary to be a MouseListener. Invoked when a mouse button is released.
     *
     * @param  kMouseEvent  the mouse event generated by a mouse release
     */
    public void mouseReleased(MouseEvent kMouseEvent) {}
    
    /**
     * Enables painting with the mouse on the triangle mesh.
     *
     * @param  bEnable  set the mouse picking enabled or not.
     */
    public void setEnable(boolean bEnable)
    {
        m_bEnabled = bEnable;
    }

    /**
     * Access on when painting with the mouse is enabled.
     *
     * @return  boolean picking is enabled or not.
     */
    public boolean getEnable() {
        return m_bEnabled;
    }

    /**
     * Access function to set the pickCanvas.
     *
     * @param  kPickCanvas  PickCanvas
     */
    public void setPickCanvas(PickCanvas kPickCanvas) {
        m_kPickCanvas = kPickCanvas;
        m_kPickCanvas.getCanvas().addMouseListener(this);
        m_kPickCanvas.getCanvas().addMouseMotionListener(this);
    }

    /**
     * Set the radius of the paint brush.
     *
     * @param  fRadius  the size of the paint brush.
     */
    public void setRadius(float fRadius) {
        m_fRadius = fRadius;
    }
}
