package gov.nih.mipav.view.renderer.WildMagic.Render;

import WildMagic.LibGraphics.Effects.*;
import WildMagic.LibGraphics.Shaders.*;

/**
 * MipavLightingEffect uses the lights defined in the Volume/Surface/Tri-Planar view in the light shader.
 */
public class MipavLightingEffect extends ShaderEffect
{

    /** Creates a MIPAV lighting effect. */
    public MipavLightingEffect ()
    {
        this(false);
    }
    
    /**
     * Create a new MIPAV lighting effect. 
     * @param bUnique determines if the VertexShader is shared or unique.
     */
    public MipavLightingEffect ( boolean bUnique )
    {
        super(1);
        m_kVShader.set(0, new VertexShader("MipavLighting", bUnique));
        m_kPShader.set(0, new PixelShader("PassThrough4"));
    }

    /** This function is called in LoadPrograms once the shader programs are
     * created.  It gives the ShaderEffect-derived classes a chance to do
     * any additional work to hook up the effect with the low-level objects.
     * @param iPass the ith rendering pass
     */
    public void OnLoadPrograms (int iPass, Program pkVProgram,
                                Program pkPProgram)
    {
        Blend(1.0f);
    }
    
    /**
     * Sets the light type for the given light.
     * @param kLightType the name of the light to set (Light0, Light1, etc.)
     * @param afType the type of light (Ambient = 0, Directional = 1, Point = 2, Spot = 3).
     */
    public void SetLight( String kLightType, float[] afType )
    {
        Program pkProgram = GetVProgram(0);
        if ( pkProgram == null )
        {
            return;
        }
        if ( pkProgram.GetUC(kLightType) != null)
        {
            pkProgram.GetUC(kLightType).SetDataSource(afType);
        }
    }
    
    /**
     * Set surface blend value.
     * @param fValue surface blend/transparency value.
     */
    public void Blend( float fValue )
    {
        Program pkProgram = GetVProgram(0);
        if ( pkProgram == null )
        {
            return;
        }
        if ( pkProgram.GetUC("Blend") != null)
        {
            pkProgram.GetUC("Blend").GetData()[0] = fValue;
        }
    }
}

