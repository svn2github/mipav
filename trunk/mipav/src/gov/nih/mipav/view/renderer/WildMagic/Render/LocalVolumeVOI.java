package gov.nih.mipav.view.renderer.WildMagic.Render;

import gov.nih.mipav.MipavMath;
import gov.nih.mipav.model.file.FileInfoBase;
import gov.nih.mipav.model.structures.VOI;
import gov.nih.mipav.model.structures.VOIBase;
import gov.nih.mipav.view.renderer.WildMagic.PlaneRender_WM;
import gov.nih.mipav.view.renderer.WildMagic.VolumeTriPlanarRender;
import gov.nih.mipav.view.renderer.WildMagic.VOI.ScreenCoordinateListener;
import gov.nih.mipav.view.renderer.WildMagic.VOI.VOIManager;

import java.awt.Graphics;
import java.util.Vector;

import javax.media.opengl.GLAutoDrawable;

import WildMagic.LibFoundation.Distance.DistanceVector3Segment3;
import WildMagic.LibFoundation.Mathematics.ColorRGBA;
import WildMagic.LibFoundation.Mathematics.Matrix3f;
import WildMagic.LibFoundation.Mathematics.Segment3f;
import WildMagic.LibFoundation.Mathematics.Vector3f;
import WildMagic.LibGraphics.Effects.ShaderEffect;
import WildMagic.LibGraphics.Effects.VertexColor3Effect;
import WildMagic.LibGraphics.Rendering.Camera;
import WildMagic.LibGraphics.Rendering.Renderer;
import WildMagic.LibGraphics.Rendering.ZBufferState;
import WildMagic.LibGraphics.SceneGraph.Attributes;
import WildMagic.LibGraphics.SceneGraph.Culler;
import WildMagic.LibGraphics.SceneGraph.Polyline;
import WildMagic.LibGraphics.SceneGraph.VertexBuffer;
import WildMagic.LibGraphics.Shaders.Program;

public abstract class LocalVolumeVOI extends VOIBase
{
    private static final long serialVersionUID = 7912877738874526203L;
    protected static int ID = 0;
    
    protected ScreenCoordinateListener m_kDrawingContext = null;

    protected Vector3f m_kLocalCenter = new Vector3f();
    protected boolean m_bUpdateCenter = true;

    protected VOIManager m_kParent;
    protected int m_iVOIType;
    protected int m_iVOISpecialType;
    protected int m_iAnchorIndex = -1;
    protected boolean m_bClosed = true;
    protected int m_iOrientation;

    protected int m_iXMin;
    protected int m_iYMin;
    protected int m_iZMin;
    protected int m_iXMax;
    protected int m_iYMax;
    protected int m_iZMax;
    protected VOI m_kGroup;
    protected VolumeVOI m_kVolumeVOI;


    protected int m_iCirclePts = 32;
    protected double[] m_adCos = new double[m_iCirclePts];
    protected double[] m_adSin = new double[m_iCirclePts];
    protected Polyline m_kBallPoint = null;
    protected ZBufferState m_kZState = null;
    protected Attributes m_kVOIAttr = null;

    public LocalVolumeVOI( VOIManager parent, ScreenCoordinateListener kContext, int iOrientation, int iType, int iSType, Vector<Vector3f> kLocal, int iZ )
    {
        m_kParent = parent;
        m_kDrawingContext = kContext;
        m_iOrientation = iOrientation;
        m_iVOIType = iType;
        name = new String("VOI"+ID++);
        for ( int i = 0; i < kLocal.size(); i++ )
        {
            Vector3f kPos = m_kParent.fileCoordinatesToPatient( kLocal.get(i) );
            kPos.Z = iZ;
            //Local.add( kPos );
            add( parent.patientCoordinatesToFile( kPos ) );
        }
        lastPoint = size() - 1;
        m_iVOISpecialType = iSType;
        if ( (m_iVOISpecialType == VOIManager.LIVEWIRE) )
        {
            m_bClosed = false;
        }
    }        
    
    public LocalVolumeVOI( VOIManager parent, ScreenCoordinateListener kContext, int iOrientation, int iType, Vector<Vector3f> kLocal, boolean bIsFile )
    {
        super();
        m_kParent = parent;
        m_kDrawingContext = kContext;
        m_iOrientation = iOrientation;
        m_iVOIType = VOI.CONTOUR;
        name = new String("VOI"+ID++);
        if ( kLocal != null )
        {
            for ( int i = 0; i < kLocal.size(); i++ )
            {
                Vector3f kPos = new Vector3f( kLocal.get(i) );
                //Local.add( kPos );
                if ( bIsFile )
                {
                    add( kPos );
                }
                else
                {
                    Vector3f kVolumePt = new Vector3f();
                    m_kDrawingContext.screenToFile( (int)kPos.X, (int)kPos.Y, (int)kPos.Z, kVolumePt );
                    add( kVolumePt );
                }
                //System.err.println( parent.screenToFileCoordinates( kPos ).ToString() );
            }
        }
        lastPoint = size() - 1;
        m_iVOISpecialType = iType;
        if ( (m_iVOISpecialType == VOIManager.LIVEWIRE) ||
                (m_iVOISpecialType == VOIManager.POLYLINE) ||
                (m_iVOISpecialType == VOIManager.POLYPOINT) )
        {
            m_bClosed = false;
        }
    }
    
    public abstract void add( VOIManager parent, int iPos, Vector3f kNewPoint, boolean bIsFile  );
    public abstract void add( VOIManager parent, Vector3f kNewPoint, boolean bIsFile  );

    public void calcBoundingBox()
    {
        for ( int i = 0; i < size(); i++ )
        {
            Vector3f kLocalPt = m_kDrawingContext.fileToScreen(get(i));
            if ( i == 0 )
            {
                m_iXMin = (int)kLocalPt.X;
                m_iXMax = (int)kLocalPt.X;
                m_iYMin = (int)kLocalPt.Y;
                m_iYMax = (int)kLocalPt.Y;
                m_iZMin = (int)kLocalPt.Z;
                m_iZMax = (int)kLocalPt.Z;
            }
            m_iXMin = (int)Math.min( m_iXMin, kLocalPt.X );
            m_iXMax = (int)Math.max( m_iXMax, kLocalPt.X );
            m_iYMin = (int)Math.min( m_iYMin, kLocalPt.Y );
            m_iYMax = (int)Math.max( m_iYMax, kLocalPt.Y );
            m_iZMin = (int)Math.min( m_iZMin, kLocalPt.Z );
            m_iZMax = (int)Math.max( m_iZMax, kLocalPt.Z );
        }
    }

    public abstract LocalVolumeVOI Clone(  );

    public abstract LocalVolumeVOI Clone( int iZ );

    public boolean contains(int x, int y, boolean forceReload) {
        return contains(m_iOrientation,x,y,slice());
    }


    public abstract boolean contains( int iOrientation, int iX, int iY, int iZ );


    public void cycleActivePt(int keyCode) {

        if ((lastPoint >= 0) && (lastPoint < this.size()))
        {
            int index = lastPoint;

            switch (keyCode) {

            case UP:
            case RIGHT:
                index++;
                break;

            case DOWN:
            case LEFT:
                index--;
                break;
            }

            if (index < 0) {
                index = this.size() - 1;
            } else if (index > (size() - 1)) {
                index = 0;
            }

            lastPoint = index;
        }
    }

    
    public void delete( int iPos )
    {
        //Local.remove(iPos);
        remove(iPos);
        lastPoint = Math.max( 0, iPos - 1 );  
        m_bUpdateCenter = true;


        if ( m_kVolumeVOI != null )
        {
            m_kVolumeVOI.setVOI(this);
        }
    }
    
    
    public void dispose()
    {
        name = null;
    }

    public void draw( float zoomX, float zoomY, 
            float[] resolutions, int[] unitsOfMeasure, int slice, int orientation,
            Graphics g ) 
    {
        if ( (orientation != m_iOrientation) || (slice() != slice) )
        {
            return;
        }
        drawSelf( resolutions, unitsOfMeasure, g, slice, orientation );
    }


    public void draw( PlaneRender_WM kDisplay, Renderer kRenderer, Culler kCuller, 
            float[] afResolutions, int[] aiUnits,
            int[] aiAxisOrder, Vector3f kCenter, int iSlice, int iOrientation,
            Vector3f kVolumeScale, Vector3f kTranslate)
    {
        if ( m_kVolumeVOI == null )
        {
            return;
        }

        //if ( iOrientation == m_iOrientation )
        {
            //if ( slice() == iSlice )
            {
                boolean bDisplaySave = m_kVolumeVOI.GetDisplay();
                Matrix3f kSave = new Matrix3f(m_kVolumeVOI.GetScene().Local.GetRotate());
                m_kVolumeVOI.GetScene().Local.SetRotateCopy(Matrix3f.IDENTITY);
                m_kVolumeVOI.SetDisplay(true);                
                m_kVolumeVOI.showTextBox(false);
                m_kVolumeVOI.setZCompare(false);

                //System.err.println( aiAxisOrder[2] + " " + kCenter.Y );
                //
                if ( aiAxisOrder[2] == 0 )
                {
                    m_kVolumeVOI.setSlice(true, aiAxisOrder[2], kCenter.X);
                }
                else if ( aiAxisOrder[2] == 1 )
                {
                    m_kVolumeVOI.setSlice(true, aiAxisOrder[2], kCenter.Y);
                }
                else
                {
                    m_kVolumeVOI.setSlice(true, aiAxisOrder[2], kCenter.Z);
                }
                m_kVolumeVOI.Render( kRenderer, kCuller, false, true );
                //m_kVolumeVOI.setSlice(true, aiAxisOrder[2], kCenter.Y);
                //m_kVolumeVOI.Render( kRenderer, kCuller, false, true );
                //m_kVolumeVOI.setSlice(true, aiAxisOrder[2], kCenter.Z);
                //m_kVolumeVOI.Render( kRenderer, kCuller, false, true );

                m_kVolumeVOI.setZCompare(true);
                m_kVolumeVOI.showTextBox(true);
                m_kVolumeVOI.GetScene().Local.SetRotateCopy(kSave);
                m_kVolumeVOI.SetDisplay(bDisplaySave);
                m_kVolumeVOI.setSlice(false, 0, -1);
                

                drawVOI( kRenderer, iSlice, afResolutions, aiUnits, m_kVolumeVOI, kVolumeScale, kTranslate,
                        iOrientation, aiAxisOrder );
            }
        }
    }

    public boolean draw( VolumeTriPlanarRender kDisplay, Renderer kRenderer, GLAutoDrawable kDrawable, 
            Camera kCamera, VolumeImage kVolumeImage, Vector3f kTranslate)
    {
        boolean bReturn = false;
        if ( m_kVolumeVOI == null )
        {
            createVolumeVOI( kRenderer, kDrawable, kCamera, kVolumeImage, kTranslate);
            bReturn = true;
        }

        m_kVolumeVOI.showTextBox(true);
        m_kVolumeVOI.setZCompare(true);
        kDisplay.addVolumeVOI( m_kVolumeVOI );
        //m_kVolumeVOI.Render( kRenderer, kCuller, bPreRender, bSolid );
        return bReturn;
    }


    public void drawSelf(float zoomX, float zoomY, float resolutionX,
            float resolutionY, float originX, float originY, float[] resols,
            int[] unitsOfMeasure, int orientation, Graphics g,
            boolean boundingBox, FileInfoBase fileInfo, int dim, int thickness) { }


    public void drawSelf(float zoomX, float zoomY, float resolutionX,
            float resolutionY, float originX, float originY, float[] resols,
            int[] unitsOfMeasure, int orientation, Graphics g,
            boolean boundingBox, int thickness) {}

    public abstract void drawSelf(float[] resols, int[] unitsOfMeasure, Graphics g, int slice, int orientation );
    public Vector3f getActivePt() {
        Vector3f pt = null;
        if ((lastPoint >= 0) && (lastPoint < this.size()))
        {
            pt = elementAt(lastPoint);
        }
        return pt;
    }
    
    public int getAnchor()
    {
        return m_iAnchorIndex;
    }
    
    public boolean getClosed()
    {
        return m_bClosed;
    }
    

    
    public int getContourID()
    {
        if ( m_kGroup != null )
        {
            return m_kGroup.getCurves()[0].indexOf(this);
        }
        return -1;
    }

    public VOI getGroup()
    {
        return m_kGroup;
    }

    public Vector3f getLocalCenter()
    {
        if ( m_bUpdateCenter )
        {
            m_bUpdateCenter = false;
            m_kLocalCenter.Set(0,0,0);
            for ( int i = 0; i < size(); i++ )
            {
                m_kLocalCenter.Add(get(i));
            }
            m_kLocalCenter.Scale( 1.0f/size() );
            m_kLocalCenter = m_kDrawingContext.fileToScreen(m_kLocalCenter);
        }
        return m_kLocalCenter;
    }

    public int getNearPoint()
    {
        return nearPoint;
    }

    public int getOrientation()
    {
        return m_iOrientation;
    }

    public float[] getOrigin(FileInfoBase fileInfo, int dim, float originX,
            float originY, float[] resols) 
    {
        return null;
    }
    public int getSelectedPoint()
    {
        return lastPoint;
    }
    public int getSType()
    {
        return m_iVOISpecialType;
    }
    public int getType()
    {
        return m_iVOIType;
    }

    public int GetVertexQuantity()
    {
        return size();
    }

    public void importArrays(float[] x, float[] y, float[] z, int n) {
        this.removeAllElements();
        for ( int i = 0; i < Math.min(Math.min(x.length,y.length),Math.min(z.length,n)); i++ )
        {
            add( new Vector3f( x[i], y[i], z[i] ) );
        }
    }

    public void importArrays(int[] x, int[] y, int[] z, int n) {
        this.removeAllElements();
        for ( int i = 0; i < Math.min(Math.min(x.length,y.length),Math.min(z.length,n)); i++ )
        {
            add( new Vector3f( x[i], y[i], z[i] ) );
        }        
    }

    public void importPoints(Vector3f[] pt) {
        this.removeAllElements();
        for ( int i = 0; i < pt.length; i++ )
        {
            add( new Vector3f( pt[i] ) );
        }        
    }

    public void move( VOIManager parent, Vector3f kDiff )
    {            
        if ( m_iVOISpecialType == VOIManager.LEVELSET )
        {
            return;
        }
        Vector3f kTest = new Vector3f();
        int iSlice = slice();
        calcBoundingBox();
        if ( m_kDrawingContext.screenToFile( (int)(m_iXMin + kDiff.X), (int)(m_iYMin + kDiff.Y), iSlice, kTest ) || 
                m_kDrawingContext.screenToFile( (int)(m_iXMax + kDiff.X), (int)(m_iYMax + kDiff.Y), iSlice, kTest )  )
        {
            return;
        }         
        int iNumPoints = size();
        if ( iNumPoints > 0 )
        {
            for ( int i = 0; i < iNumPoints; i++ )
            {                
                Vector3f kPos = get( i );
                Vector3f kLocal = m_kDrawingContext.fileToScreen( kPos );
                kLocal.Add( kDiff );
                Vector3f kVolumePt = new Vector3f();
                m_kDrawingContext.screenToFile( (int)kLocal.X, (int)kLocal.Y, (int)kLocal.Z, kVolumePt );
                set( i, kVolumePt );
            }
        }
        m_bUpdateCenter = true;

        if ( m_kVolumeVOI != null )
        {
            m_kVolumeVOI.setVOI(this);
        }
    }

    public void moveActivePt(int keyCode, int xDim, int yDim) {
        if ((this.size() > lastPoint) && (lastPoint >= 0))
        {
            Vector3f kPosFile = get(lastPoint);
            Vector3f kPos = m_kDrawingContext.fileToScreen(kPosFile);
            
            float x = kPos.X;
            float y = kPos.Y;

            switch (keyCode) {

            case UP:
                y -= 1;
                break;

            case LEFT:
                x -= 1;
                break;

            case DOWN:
                y += 1;
                break;

            case RIGHT:
                x += 1;
                break;

            default:
                return;
            }

            if ((x >= 0) && (x < xDim) && (y >= 0) && (y < yDim)) {
                 m_kDrawingContext.screenToFile((int)x, (int)y, (int)kPos.Z, kPosFile);
                 this.set(lastPoint, kPosFile);
            }
        }        
    }


    public boolean nearLine(int iX, int iY, int iZ )
    {
        Vector3f kVOIPoint = new Vector3f(iX, iY, iZ );
        for (int i = 0; i < (size() - 1); i++)
        {
            Vector3f kPosFile = get(i);
            Vector3f kPos0 = m_kDrawingContext.fileToScreen(kPosFile);

            kPosFile = get(i+1);
            Vector3f kPos1 = m_kDrawingContext.fileToScreen(kPosFile);
            
            Vector3f kDir = new Vector3f();
            kDir.Sub( kPos1, kPos0 );
            float fLength = kDir.Normalize();
            Segment3f kSegment = new Segment3f(kPos0, kDir, fLength);
            DistanceVector3Segment3 kDist = new DistanceVector3Segment3( kVOIPoint, kSegment );
            float fDist = kDist.Get();
            if ( fDist < 3 )
            {
                setNearPoint(i);
                return true;
            }
        }

        Vector3f kPosFile = get(0);
        Vector3f kPos0 = m_kDrawingContext.fileToScreen(kPosFile);

        kPosFile = get(size() - 1);
        Vector3f kPos1 = m_kDrawingContext.fileToScreen(kPosFile);

        Vector3f kDir = new Vector3f();
        kDir.Sub( kPos1, kPos0 );
        float fLength = kDir.Normalize();
        Segment3f kSegment = new Segment3f(kPos0, kDir, fLength);
        DistanceVector3Segment3 kDist = new DistanceVector3Segment3( kVOIPoint, kSegment );
        float fDist = kDist.Get();
        if ( fDist < 3 )
        {
            setNearPoint(size() - 1);
            return true;
        }
        return false;
    }

    public boolean nearPoint( int iX, int iY, int iZ) {

        Vector3f kVOIPoint = new Vector3f(iX, iY, iZ );
        for ( int i = 0; i < size(); i++ )
        {
            Vector3f kFilePos = get(i);
            Vector3f kPos = m_kDrawingContext.fileToScreen(kFilePos);
            Vector3f kDiff = new Vector3f();
            kDiff.Sub( kPos, kVOIPoint );
            if ( (Math.abs( kDiff.X ) < 3) &&  (Math.abs( kDiff.Y ) < 3) && (Math.abs( kDiff.Z ) < 3) )
            {
                setNearPoint(i);
                return true;
            }
        }
        return false;
    }

    public void setAnchor()
    {
        m_iAnchorIndex = size()-1;
    }

    public void setClosed( boolean bClosed )
    {
        m_bClosed = bClosed;
        if ( m_bClosed )
        {
            m_iVOIType = VOI.CONTOUR;
        }
        else
        {
            m_iVOIType = VOI.POLYLINE;
        }
    }

    public void setGroup( VOI kGroup )
    {
        m_kGroup = kGroup;
    }

    public void setNearPoint( int i )
    {
        nearPoint = i;
    }

    public void setPosition( VOIManager parent, int iPos, float fX, float fY, float fZ )
    {
        if ( m_iVOISpecialType == VOIManager.LEVELSET )
        {
            return;
        }
        if ( iPos < size() )
        {
            m_bUpdateCenter = true;
            Vector3f kPos = new Vector3f( fX, fY, fZ );
            Vector3f kVolumePt = new Vector3f();
            m_kDrawingContext.screenToFile( (int)kPos.X, (int)kPos.Y, (int)kPos.Z, kVolumePt );
            set( iPos, kVolumePt );
            lastPoint = iPos;


            if ( m_kVolumeVOI != null )
            {
                m_kVolumeVOI.setVOI(this);
            }
        }

    }
    
    public void setPosition( VOIManager parent, int iPos, Vector3f kPos )
    {
        if ( m_iVOISpecialType == VOIManager.LEVELSET )
        {
            return;
        }
        if ( iPos < size() )
        {
            m_bUpdateCenter = true;
            Vector3f kVolumePt = new Vector3f();
            m_kDrawingContext.screenToFile( (int)kPos.X, (int)kPos.Y, (int)kPos.Z, kVolumePt );
            set( iPos, kVolumePt );
            lastPoint = iPos;

            if ( m_kVolumeVOI != null )
            {
                m_kVolumeVOI.setVOI(this);
            }
        }
    }

    public void setSelectedPoint( int i )
    {
        lastPoint = i;
        nearPoint = i;
    }

    public void setSType(int iType)
    {
        m_iVOISpecialType = iType;
    }


    public int slice()
    {
        Vector3f kPos = m_kParent.fileCoordinatesToPatient( get(0) );
        return (int)kPos.Z;
    }    

    public abstract LocalVolumeVOI split ( Vector3f kStartPt, Vector3f kEndPt );

    public void update()
    {
        if ( m_kVolumeVOI != null )
        {
            m_kVolumeVOI.update();
        }
    }

    
    protected float areaTwice(float ptAx, float ptAy, float ptBx, float ptBy, float ptCx, float ptCy) {
        return (((ptAx - ptCx) * (ptBy - ptCy)) - ((ptAy - ptCy) * (ptBx - ptCx)));
    }

    protected void createSelectedIcon( int[] aiAxisOrder )
    {

        m_kZState = new ZBufferState();
        m_kZState.Compare = ZBufferState.CompareMode.CF_ALWAYS;
        m_kVOIAttr = new Attributes();
        m_kVOIAttr.SetPChannels(3);
        m_kVOIAttr.SetCChannels(0,3);

        VertexBuffer kBuffer = new VertexBuffer(m_kVOIAttr, 4);
        if ( aiAxisOrder[2] == 2 )
        {
            kBuffer.SetPosition3( 0, -1.0f/200f, -1.0f/200f, 0 );
            kBuffer.SetPosition3( 1,  1.0f/200f, -1.0f/200f, 0 );
            kBuffer.SetPosition3( 2,  1.0f/200f,  1.0f/200f, 0 );
            kBuffer.SetPosition3( 3, -1.0f/200f,  1.0f/200f, 0 );
        }
        else if ( aiAxisOrder[2] == 1 )
        {
            kBuffer.SetPosition3( 0, -1.0f/200f, 0, -1.0f/200f );
            kBuffer.SetPosition3( 1,  1.0f/200f, 0, -1.0f/200f );
            kBuffer.SetPosition3( 2,  1.0f/200f, 0,  1.0f/200f );
            kBuffer.SetPosition3( 3, -1.0f/200f, 0,  1.0f/200f );                
        }
        else 
        {
            kBuffer.SetPosition3( 0, 0, -1.0f/200f, -1.0f/200f );
            kBuffer.SetPosition3( 1, 0,  1.0f/200f, -1.0f/200f );
            kBuffer.SetPosition3( 2, 0,  1.0f/200f,  1.0f/200f );
            kBuffer.SetPosition3( 3, 0, -1.0f/200f,  1.0f/200f );                
        }

        for ( int i = 0; i < kBuffer.GetVertexQuantity(); i++ )
        {
            kBuffer.SetColor3( 0, i, 1, 1, 1 );
        }

        m_kBallPoint = new Polyline( kBuffer, true, true );
        m_kBallPoint.AttachEffect( new VertexColor3Effect( "ConstantColor", true ) );
        m_kBallPoint.AttachGlobalState(m_kZState);
        m_kBallPoint.UpdateRS();

        for ( int i = 0; i < m_iCirclePts; i++ )
        {
            m_adCos[i] = Math.cos( Math.PI * 2.0 * i/m_iCirclePts );
            m_adSin[i] = Math.sin( Math.PI * 2.0 * i/m_iCirclePts);
        }
    }


    protected void createVolumeVOI(Renderer kRenderer, GLAutoDrawable kDrawable, 
            Camera kCamera, VolumeImage kVolumeImage, Vector3f kTranslate)
    {
        m_kVolumeVOI = new VolumeVOI( kRenderer, kDrawable, kVolumeImage, 
                kTranslate, kCamera, this );
    }
    
    protected void drawSelectedPoints( Renderer kRenderer, Vector3f kVolumeScale, Vector3f kTranslate,
            int iOrientation, int[] aiAxisOrder )
    {
        if ( m_kBallPoint == null && (iOrientation == m_iOrientation))
        {
            createSelectedIcon(aiAxisOrder);
        }
        Vector3f kLocalTranslate = new Vector3f();
        for ( int j = 0; j < size(); j++ )
        {
            kLocalTranslate.Copy( get(j) );
            kLocalTranslate.Mult( kVolumeScale );
            kLocalTranslate.Add( kTranslate );
            m_kBallPoint.Local.SetTranslate( kLocalTranslate );
            m_kBallPoint.UpdateGS();

            Program kProgram = ((ShaderEffect)m_kBallPoint.GetEffect(0)).GetCProgram(0);
            if ( kProgram != null )
            {
                if ( kProgram.GetUC("ConstantColor") != null )
                {
                    if ( j == getSelectedPoint() )
                    {
                        kProgram.GetUC("ConstantColor").GetData()[0] = 0;
                        kProgram.GetUC("ConstantColor").GetData()[1] = 1;
                        kProgram.GetUC("ConstantColor").GetData()[2] = 0;
                    }
                    else if ( j == 0 )
                    {
                        kProgram.GetUC("ConstantColor").GetData()[0] = 1;
                        kProgram.GetUC("ConstantColor").GetData()[1] = 1;
                        kProgram.GetUC("ConstantColor").GetData()[2] = 0;                                
                    }
                    else
                    {
                        kProgram.GetUC("ConstantColor").GetData()[0] = 1;
                        kProgram.GetUC("ConstantColor").GetData()[1] = 1;
                        kProgram.GetUC("ConstantColor").GetData()[2] = 1;
                    }
                }

                if ( kProgram.GetUC("UseConstantColor") != null )
                {
                    kProgram.GetUC("UseConstantColor").GetData()[0] = 1.0f;
                }
            }
            kRenderer.Draw( m_kBallPoint );
        }
    }
    
    protected void drawText( Renderer kRenderer, int iX, int iY, ColorRGBA kColor, char[] acText )
    {
        kRenderer.Draw( iX, iY-1, ColorRGBA.BLACK, acText);
        kRenderer.Draw( iX, iY+1, ColorRGBA.BLACK, acText);
        kRenderer.Draw( iX-1, iY, ColorRGBA.BLACK, acText);
        kRenderer.Draw( iX+1, iY, ColorRGBA.BLACK, acText);

        kRenderer.Draw( iX, iY, kColor, acText);        
    }

        
    protected void drawVOI( Renderer kRenderer, int iSlice, float[] afResolutions, int[] aiUnits, 
            VolumeVOI kVolumeVOI, Vector3f kVolumeScale, Vector3f kTranslate, int iOrientation, int[] aiAxisOrder )
    {             
        if ( isActive() )
        {
            int iNumPoints = GetVertexQuantity();
            if ( iNumPoints > 0 )
            {
                if ( iSlice == slice() )
                {

                    Vector3f kCenter = getLocalCenter();

                    if ( (m_iVOISpecialType != VOIManager.SPLITLINE) ||
                            (m_iVOISpecialType != VOIManager.POLYPOINT) )
                    {
                        String kMessage = new String("+");
                        char[] acText = kMessage.toCharArray();
                        int[] aiSize = kRenderer.GetSizeOnScreen( acText );
                        drawText( kRenderer, (int)kCenter.X - aiSize[0]/2, (int)kCenter.Y + aiSize[1]/2, kVolumeVOI.getColor(), acText );

                        int iContourID = getContourID();
                        if ( iContourID != -1 )
                        {
                            iContourID++;
                            kMessage = String.valueOf(iContourID);
                            acText = kMessage.toCharArray();
                            drawText( kRenderer, (int)kCenter.X - aiSize[0]/2 - 10, (int)kCenter.Y + aiSize[1]/2 - 5, kVolumeVOI.getColor(), acText );
                        }
                    }          
                    drawSelectedPoints( kRenderer, kVolumeScale, kTranslate, iOrientation, aiAxisOrder );
                }
            }
        }
    }

    protected String getLengthString(int iPos0, int iPos1, float[] afResolutions, int[] aiUnits)
    {
        if ( iPos0 >= size() || iPos1 >= size() )
        {
            return null;
        }
        Vector3f kStart = m_kParent.fileCoordinatesToPatient( get(iPos0) );
        Vector3f kEnd = m_kParent.fileCoordinatesToPatient( get(iPos1) );
        float[] x = new float[2];
        x[0] = kStart.X;
        x[1] = kEnd.X;

        float[] y = new float[2];
        y[0] = kStart.Y;
        y[1] = kEnd.Y;

        double length = MipavMath.length(x, y, afResolutions );

        String tmpString = String.valueOf(length);
        int i = tmpString.indexOf('.');

        if (tmpString.length() >= (i + 3)) {
            tmpString = tmpString.substring(0, i + 3);
        }

        tmpString = tmpString + " " + FileInfoBase.getUnitsOfMeasureAbbrevStr(aiUnits[0]);
        return tmpString;
    }

    protected double getTotalLength(float[] afResolutions)
    {
        float[] x = new float[2];
        float[] y = new float[2];
        double length = 0;
        for ( int i = 0; i < size()-1; i++ )
        {
            Vector3f kStart = m_kParent.fileCoordinatesToPatient( get(i) );
            Vector3f kEnd = m_kParent.fileCoordinatesToPatient( get(i+1) );
            x[0] = kStart.X;            x[1] = kEnd.X;
            y[0] = kStart.Y;            y[1] = kEnd.Y;

            length += MipavMath.length(x, y, afResolutions );
        }
        return length;
    }

    protected String getTotalLengthString(float[] afResolutions, int[] aiUnits)
    {
        double length = getTotalLength(afResolutions);

        String tmpString = String.valueOf(length);
        int i = tmpString.indexOf('.');

        if (tmpString.length() >= (i + 3)) {
            tmpString = tmpString.substring(0, i + 3);
        }

        tmpString = tmpString + " " + FileInfoBase.getUnitsOfMeasureAbbrevStr(aiUnits[0]);
        return tmpString;
        
    }
}
