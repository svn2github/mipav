// Geometric Tools, Inc.
// http://www.geometrictools.com
// Copyright (c) 1998-2006.  All Rights Reserved
//
// The Wild Magic Version 4 Restricted Libraries source code is supplied
// under the terms of the license agreement
//     http://www.geometrictools.com/License/Wm4RestrictedLicense.pdf
// and may not be copied or disclosed except in accordance with the terms
// of that agreement.
//
// Version: 4.0.0 (2006/06/28)

package gov.nih.mipav.view.WildMagic.LibGraphics.Shaders;

import java.util.Vector;
import gov.nih.mipav.view.WildMagic.LibGraphics.ObjectSystem.*;
import gov.nih.mipav.view.WildMagic.LibGraphics.Rendering.*;

public abstract class Shader extends WmObject
    implements StreamInterface
{
    // The name of the shader object.  The program object has a name that
    // contains the shader name as a substring, but adds additional text
    // as needed (the path to a shader on disk, the identifying information
    // for a procedurally generated shader).
    public String GetShaderName ()
    {
        return m_kShaderName;
    }

    // Access to textures and image names.
    public void SetTextureQuantity (int iQuantity)
    {
        m_kTextures.clear();
        m_kTextures.setSize(iQuantity);
        for (int i = 0; i < iQuantity; i++)
        {
            m_kTextures.set(i, new Texture());
        }

        m_kImageNames.clear();
        m_kImageNames.setSize(iQuantity);
    }

    public int GetTextureQuantity ()
    {
        return (int)m_kTextures.size();
    }

    public Texture GetTexture (int i)
    {
        if (0 <= i && i < (int)m_kTextures.size())
        {
            return m_kTextures.get(i);
        }
        return null;
    }

    public Texture GetTexture (String rkName)
    {
        if (m_spkProgram != null)
        {
            for (int i = 0; i < (int)m_kTextures.size(); i++)
            {
                Texture pkTexture = m_kTextures.get(i);
                SamplerInformation pkSI = pkTexture.GetSamplerInformation();
                if (pkSI.GetName() == rkName)
                {
                    return pkTexture;
                }
            }
        }
        return null;
    }

    public void SetImageName (int i, String rkName)
    {
        int iQuantity = (int)m_kImageNames.size();
        if (i >= iQuantity)
        {
            m_kImageNames.setSize(i+1);
        }

        m_kImageNames.set(i, rkName);
    }

    public String GetImageName (int i)
    {
        assert(0 <= i && i < (int)m_kImageNames.size());
        return m_kImageNames.get(i);
    }


    // Support for streaming.
    public Shader () {}

    // The constructor called by the derived classes VertexShader and
    // PixelShader.
    protected Shader (String rkShaderName)
    {
        m_kShaderName = new String(rkShaderName);
    }


    // The shader name, which contributes to a uniquely identifying string
    // for a shader program.
    protected String m_kShaderName;

    // The shader program, which is dependent on graphics API.
    protected Program m_spkProgram;

    // The user-defined data are specific to each shader object.  The Program
    // object knows only the name, which register to assign the value to, and
    // how many registers to use.  The storage provided here is for the
    // convenience of Shader-derived classes.  However, a derived class may
    // provide alternate storage by calling UserConstant::SetDataSource for
    // each user constant of interest.
    protected Vector<Float> m_kUserData = new Vector<Float>();

    // The names of images used by an instance of a shader program.  The
    // Texture objects store the actual images and the samplers that are
    // used to sample the images.
    protected Vector<String> m_kImageNames = new Vector<String>();
    protected Vector<Texture> m_kTextures = new Vector<Texture>();

    // internal use
    public void OnLoadProgram (Program pkProgram)
    {
        assert((m_spkProgram == null) && (pkProgram != null));
        m_spkProgram = pkProgram;

        // The data sources must be set for the user constants.  Determine how
        // many float channels are needed for the storage.
        int iUCQuantity = m_spkProgram.GetUCQuantity();
        int i, iChannels;
        UserConstant pkUC;
        for (i = 0, iChannels = 0; i < iUCQuantity; i++)
        {
            pkUC = m_spkProgram.GetUC(i);
            assert(pkUC != null);
            iChannels += 4*pkUC.GetRegisterQuantity();
        }
        m_kUserData.setSize(iChannels);

        // Set the data sources for the user constants.
//         for (i = 0, iChannels = 0; i < iUCQuantity; i++)
//         {
//             pkUC = m_spkProgram.GetUC(i);
//             assert(pkUC != null);
//             //pkUC.SetDataSource(m_kUserData.get(iChannels));
//             int iSize = 4*pkUC.GetRegisterQuantity();
//             float[] afData = new float[iSize];
//             for ( int j = 0; j < iSize; j++ )
//             {
//                 afData[j] = m_kUserData.get(iChannels + j);
//             }
//             pkUC.SetDataSource(afData);
//             iChannels += 4*pkUC.GetRegisterQuantity();
//         }

        // Load the images into the textures.  If the image is already in
        // system memory (in the image catalog), it is ready to be used.  If
        // it is not in system memory, an attempt is made to load it from
        // disk storage.  If the image file does not exist on disk, a default
        // magenta image is used.
        int iSIQuantity = m_spkProgram.GetSIQuantity();
        m_kImageNames.setSize(iSIQuantity);
        m_kTextures.setSize(iSIQuantity);
        for (i = 0; i < iSIQuantity; i++)
        {
            Image pkImage = ImageCatalog.GetActive().Find(m_kImageNames.get(i));
            assert(pkImage != null);
            if (m_kTextures.get(i) == null)
            {
                m_kTextures.set(i, new Texture());
            }
            m_kTextures.get(i).SetImage(pkImage);
            m_kTextures.get(i).SetSamplerInformation(m_spkProgram.GetSI(i));
        }
    }

    public void OnReleaseProgram ()
    {
        // Destroy the program.  The texture images, if any, will be destroyed
        // by the shader destructor.  If the shader has the last reference to
        // an image, that image will be deleted from the image catalog
        // automatically.
        m_spkProgram = null;
    }

    public void Load (Stream rkStream, Stream.Link pkLink)
    {
        super.Load(rkStream,pkLink);

        // native data
        m_kShaderName = rkStream.ReadString();

        int iQuantity = rkStream.ReadInt();
        m_kImageNames.setSize(iQuantity);
        for (int i = 0; i < iQuantity; i++)
        {
            m_kImageNames.set(i, rkStream.ReadString());
        }

        // link data
        iQuantity = rkStream.ReadInt();
        m_kTextures.setSize(iQuantity);
        for (int i = 0; i < iQuantity; i++)
        {
            int iLinkID = rkStream.ReadInt();  // m_kTextures[i]
            pkLink.Add(iLinkID);
        }

        // The data members m_spkProgram and m_kUserData are both set during
        // resource loading at program runtime.
    }

    public void Link (Stream rkStream, Stream.Link pkLink)
    {
        super.Link(rkStream,pkLink);

        for (int i = 0; i < m_kTextures.size(); i++)
        {
            int iLinkID = pkLink.GetLinkID();
            m_kTextures.set(i, (Texture)rkStream.GetFromMap(iLinkID));
        }
    }

    public boolean Register (Stream rkStream)
    {
        if (!super.Register(rkStream))
        {
            return false;
        }

        for (int i = 0; i < m_kTextures.size(); i++)
        {
            if (m_kTextures.get(i) != null)
            {
                m_kTextures.get(i).Register(rkStream);
            }
        }

        return true;
    }

    public void Save (Stream rkStream)
    {
        super.Save(rkStream);

        // native data
        rkStream.Write(m_kShaderName);

        int iQuantity = m_kImageNames.size();
        rkStream.Write(iQuantity);
        for (int i = 0; i < iQuantity; i++)
        {
            rkStream.Write(m_kImageNames.get(i));
        }

        // link data
        iQuantity = m_kTextures.size();
        rkStream.Write(iQuantity);
        for (int i = 0; i < iQuantity; i++)
        {
            rkStream.Write(m_kTextures.get(i).GetID());
        }

        // The data members m_spkProgram and m_kUserData are both set during
        // resource loading at program runtime.
    }

    public int GetDiskUsed (StreamVersion rkVersion) 
    {
        int iSize = super.GetDiskUsed(rkVersion) +
            Stream.SIZEOF_INT + //sizeof(int) +
            m_kShaderName.length();

        int iQuantity = m_kImageNames.size();
        iSize += Stream.SIZEOF_INT; //sizeof(int);
        for (int i = 0; i < iQuantity; i++)
        {
            iSize += Stream.SIZEOF_INT + //sizeof(int) +
                m_kImageNames.get(i).length();
        }

        iQuantity = m_kTextures.size();
        iSize += Stream.SIZEOF_INT + //sizeof(int) +
            iQuantity*Stream.SIZEOF_INT; //sizeof(m_kTextures[0]);

        return iSize;
    }

    public StringTree SaveStrings (final String acTitle)
    {
        StringTree pkTree = new StringTree();
        // strings
        pkTree.Append(StringTree.Format("Shader",GetName()));
        pkTree.Append(super.SaveStrings(null));
        pkTree.Append(StringTree.Format("shader name =",m_kShaderName));

        for (int i = 0; i < m_kImageNames.size(); i++)
        {
            String kPrefix = new String("image[" + i + "] =");
            pkTree.Append(StringTree.Format(kPrefix,m_kImageNames.get(i)));
        }

        // children
        for (int i = 0; i < (int)m_kTextures.size(); i++)
        {
            pkTree.Append(m_kTextures.get(i).SaveStrings(null));
        }

        return pkTree;
    }
}
