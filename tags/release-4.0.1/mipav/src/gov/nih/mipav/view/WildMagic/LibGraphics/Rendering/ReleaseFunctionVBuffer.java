// Wild Magic Source Code
// David Eberly
// http://www.geometrictools.com
// Copyright (c) 1998-2007
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation; either version 2.1 of the License, or (at
// your option) any later version.  The license is available for reading at
// either of the locations:
//     http://www.gnu.org/copyleft/lgpl.html
//     http://www.geometrictools.com/License/WildMagicLicense.pdf
//
// Version: 4.0.0 (2006/06/28)
//
// Ported to Java by Alexandra Bokinsky, PhD, Geometric Tools, Inc. (July 2007)
//

package gov.nih.mipav.view.WildMagic.LibGraphics.Rendering;

public class ReleaseFunctionVBuffer extends ReleaseFunction
{
    /** Create a ReleaseFunctionVBuffer, store the Renderer to access the
     * Renderer.ReleaseVBuffer function call.
     * @param kRenderer, Renderer for calling the Renderer.ReleaseVBuffer function.
     */
    public ReleaseFunctionVBuffer ( Renderer kRenderer )
    {
        super(kRenderer);
    }
    /** Call the Renderer.ReleaseVBuffer function.
     * @param kBindable, parameter to ReleaseVBuffer.
     */
    public final void Release ( Bindable kBindable )
    {
        m_kRenderer.ReleaseVBuffer( kBindable );
    }
}
