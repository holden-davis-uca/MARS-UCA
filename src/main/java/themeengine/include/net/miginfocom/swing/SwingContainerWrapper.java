package themeengine.include.net.miginfocom.swing;
/*
 * License (BSD):
 * ==============
 *
 * Copyright (c) 2004, Mikael Grev, MiG InfoCom AB. (miglayout (at) miginfocom (dot) com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * Neither the name of the MiG InfoCom AB nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * @version 1.0
 * @author Mikael Grev, MiG InfoCom AB
 *         Date: 2006-sep-08
 */

import themeengine.include.net.miginfocom.layout.ComponentWrapper;
import themeengine.include.net.miginfocom.layout.ContainerWrapper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;

/**
 */
public final class SwingContainerWrapper extends SwingComponentWrapper implements ContainerWrapper
{
	/** Debug color for cell outline.
	 */
	private static final Color DB_CELL_OUTLINE = new Color(255, 0, 0);

	public SwingContainerWrapper(Container c)
	{
		super(c);
	}

	@Override
	public ComponentWrapper[] getComponents()
	{
		Container c = (Container) getComponent();
		ComponentWrapper[] cws = new ComponentWrapper[c.getComponentCount()];
		for (int i = 0; i < cws.length; i++)
			cws[i] = new SwingComponentWrapper(c.getComponent(i));
		return cws;
	}

	@Override
	public int getComponentCount()
	{
		return ((Container) getComponent()).getComponentCount();
	}

	@Override
	public Object getLayout()
	{
		return ((Container) getComponent()).getLayout();
	}

	@Override
	public boolean isLeftToRight()
	{
		return ((Container) getComponent()).getComponentOrientation().isLeftToRight();
	}

	@Override
	public void paintDebugCell(int x, int y, int width, int height)
	{
		Component c = (Component) getComponent();
		if (!c.isShowing())
			return;

		Graphics2D g = (Graphics2D) c.getGraphics();
		if (g == null)
			return;

		g.setStroke(new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10f, new float[] {2f, 3f}, 0));
		g.setPaint(DB_CELL_OUTLINE);
		g.drawRect(x, y, width - 1, height - 1);
	}

	@Override
	public int getComponentType(boolean disregardScrollPane)
	{
		return TYPE_CONTAINER;
	}

	// Removed for 2.3 because the parent.isValid() in MigLayout will catch this instead.
	@Override
	public int getLayoutHashCode()
	{
		long n = System.nanoTime();
		int h = super.getLayoutHashCode();

		if (isLeftToRight())
			h += 416343;

		return 0;
	}
}
