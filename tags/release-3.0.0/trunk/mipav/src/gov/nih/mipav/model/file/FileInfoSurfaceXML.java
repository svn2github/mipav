package gov.nih.mipav.model.file;


import gov.nih.mipav.model.structures.*;

import gov.nih.mipav.view.dialogs.*;

import javax.media.j3d.*;

import javax.vecmath.*;


/**
 * This structure contains the information that describes how an XML surface (see surface.xsd and FileSurfaceXML.java)
 * is stored on disk.
 *
 * @see  FileIO
 * @see  FileInfoXML
 * @see  FileSurfaceXML
 */
public class FileInfoSurfaceXML extends FileInfoXML {

    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /** Use serialVersionUID for interoperability. */
    private static final long serialVersionUID = 6815854469776062772L;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** Unique ID, for the surface:. */
    private int m_iUnique_ID;

    /** Material properties of the surface:. */
    private Material m_kMaterial = null;

    /** Surface triangle mesh:. */
    private ModelTriangleMesh[] m_kMesh = null;

    /** Type keyword for the surface:. */
    private String m_kType = null;

    /** surface opacity:. */
    private float m_kOpacity;

    /** surface level of detail:. */
    private int m_kLevelDetail;

    /** mesh index. */
    private int meshIndex = 0;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Main constructor for FileInfoSurfaceXML.
     *
     * @param  name       String file name
     * @param  directory  String file directory
     * @param  format     int file format (data type)
     */
    public FileInfoSurfaceXML(String name, String directory, int format) {
        super(name, directory, format);
        m_kMaterial = new Material();
        m_kMaterial.setCapability(Material.ALLOW_COMPONENT_READ);
        m_kMaterial.setCapability(Material.ALLOW_COMPONENT_WRITE);
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Displays the file information.
     *
     * @param  dlog  JDialogBase dialog box that is written to
     * @param  kMat  DOCUMENT ME!
     */
    public void displayAboutInfo(JDialogBase dlog, TransMatrix kMat) { }

    /**
     * Prepares the class for cleanup.
     */
    public void finalize() {
        m_kMaterial = null;
        m_kType = null;
        m_kMesh = null;

        super.finalize();
    }

    /**
     * Returns the unique id for the surface:
     *
     * @return  DOCUMENT ME!
     */
    public int getID() {
        return m_iUnique_ID;
    }

    /**
     * Returns the material properties for the surface:
     *
     * @return  DOCUMENT ME!
     */
    public Material getMaterial() {
        return m_kMaterial;
    }

    /**
     * Returns the surface opacity value.
     *
     * @return  surface opacity.
     */
    public float getOpacity() {
      return m_kOpacity;
    }

    /**
     * Returns the surface level of detail.
     *
     * @return  surface level of detail.
     */
    public int getLevelDetail() {
      return m_kLevelDetail;
    }

    /**
     * Returns the ModelTriangleMesh representing the surface:
     *
     * @return  DOCUMENT ME!
     */
    public ModelTriangleMesh[] getMesh() {
        return m_kMesh;
    }

    /**
     * Returns the surface type keyword:
     *
     * @return  DOCUMENT ME!
     */
    public String getType() {
        return m_kType;
    }

    /**
     * Sets the ambient color of the surface:
     *
     * @param  kColor  DOCUMENT ME!
     */
    public void setAmbient(Color3f kColor) {
        m_kMaterial.setAmbientColor(kColor);
    }

    /**
     * Sets the diffuse color of the surface:
     *
     * @param  kColor  DOCUMENT ME!
     */
    public void setDiffuse(Color3f kColor) {
        m_kMaterial.setDiffuseColor(kColor);
    }

    /**
     * Sets the emissive color of the surface:
     *
     * @param  kColor  DOCUMENT ME!
     */
    public void setEmissive(Color3f kColor) {
        m_kMaterial.setEmissiveColor(kColor);
    }

    /**
     * Set the unique id for the surface:
     *
     * @param  iID  DOCUMENT ME!
     */
    public void setID(int iID) {
        m_iUnique_ID = iID;
    }

    /**
     * Sets the material properties for the surface:
     *
     * @param  kMaterial  DOCUMENT ME!
     */
    public void setMaterial(Material kMaterial) {
        m_kMaterial = kMaterial;
    }

    /**
     * Sets the ModelTriangleMesh representing the surface:
     *
     * @param  kMesh  DOCUMENT ME!
     */
    public void setMesh(ModelTriangleMesh[] kMesh) {
        m_kMesh = kMesh;
    }

    /**
     * Creates the ModelTriangleMesh for the surface:
     *
     * @param  kVertices       DOCUMENT ME!
     * @param  kNormals        DOCUMENT ME!
     * @param  aiConnectivity  DOCUMENT ME!
     */
    public void setMesh(Point3f[] kVertices, Vector3f[] kNormals, int[] aiConnectivity) {

       int i;
       if ( m_kMesh == null ) {
         m_kMesh = new ModelTriangleMesh[1];
       } else {
         ModelTriangleMesh[] mesh = new ModelTriangleMesh[m_kMesh.length];
         for (i = 0; i < m_kMesh.length; i++) {
           mesh[i] = m_kMesh[i];
         }
         m_kMesh = new ModelTriangleMesh[meshIndex + 1];
         for (i = 0; i < meshIndex; i++) {
           m_kMesh[i] = mesh[i];
         }
       }
        if (kNormals != null) {
            m_kMesh[meshIndex++] = new ModelTriangleMesh(kVertices, kNormals, aiConnectivity);
        } else {
            m_kMesh[meshIndex++] = new ModelTriangleMesh(kVertices, aiConnectivity);
        }
    }

    /**
     * Sets the surface shininess:
     *
     * @param  fShininess  DOCUMENT ME!
     */
    public void setShininess(float fShininess) {
        m_kMaterial.setShininess(fShininess);
    }

    /**
     * set the surface opacity keyword:
     *
     * @param  opacity  surface opacity
     */
    public void setOpacity(float opacity) {
      m_kOpacity = opacity;
    }

    /**
     * set the surface level of detail keyword:
     *
     * @param  level of detail
     */
    public void setLevelDetail(int levelDetail) {
      m_kLevelDetail = levelDetail;
    }

    /**
     * Sets the specular color of the surface:
     *
     * @param  kColor  DOCUMENT ME!
     */
    public void setSpecular(Color3f kColor) {
        m_kMaterial.setSpecularColor(kColor);
    }

    /**
     * set the surface type keyword:
     *
     * @param  kType  DOCUMENT ME!
     */
    public void setType(String kType) {
        m_kType = kType;
    }


    /**
     * Used to propogate all FileInfoSurfaceXML private variables to other FileInfosSurfaceXML.
     *
     * @param  fInfo  FileInfoSurfaceXML file info to be copied into
     */
    public void updateFileInfos(FileInfoXML fInfo) {

        if (this == fInfo) {
            return;
        }

        fInfo.setImageDescription(this.getImageDescription());
        ((FileInfoSurfaceXML) fInfo).setID(this.getID());
        ((FileInfoSurfaceXML) fInfo).setMaterial(this.getMaterial());
        ((FileInfoSurfaceXML) fInfo).setType(this.getType());
        ((FileInfoSurfaceXML) fInfo).setOpacity(this.getOpacity());
        ((FileInfoSurfaceXML) fInfo).setLevelDetail(this.getLevelDetail());
        ((FileInfoSurfaceXML) fInfo).setMesh(this.getMesh());
    }
}
