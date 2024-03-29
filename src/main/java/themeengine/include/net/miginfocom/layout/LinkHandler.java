package themeengine.include.net.miginfocom.layout;

import java.util.HashMap;
import java.util.WeakHashMap;
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

/**
 */
public final class LinkHandler
{
	public static final int X = 0;
	public static final int Y = 1;
	public static final int WIDTH = 2;
	public static final int HEIGHT = 3;
	public static final int X2 = 4;
	public static final int Y2 = 5;

	// indices for values of LAYOUTS
	private static final int VALUES = 0;
	private static final int VALUES_TEMP = 1;

	private static final WeakHashMap<Object, HashMap<String, int[]>[]> LAYOUTS = new WeakHashMap<>();

	private LinkHandler()
	{
	}

	public synchronized static Integer getValue(Object layout, String key, int type)
	{
		Integer ret = null;

		HashMap<String, int[]>[] layoutValues = LAYOUTS.get(layout);
		if (layoutValues != null) {
			int[] rect = layoutValues[VALUES_TEMP].get(key);
			if (rect != null && rect[type] != LayoutUtil.NOT_SET) {
				ret = rect[type];
			} else {
				rect = layoutValues[VALUES].get(key);
				ret = (rect != null && rect[type] != LayoutUtil.NOT_SET) ? rect[type] : null;
			}
		}
		return ret;
	}

	/** Sets a key that can be linked to from any component.
	 * @param layout The MigLayout instance
	 * @param key The key to link to. This is the same as the ID in a component constraint.
	 * @param x x
	 * @param y y
	 * @param width Width
	 * @param height Height
	 * @return If the value was changed
	 */
	public synchronized static boolean setBounds(Object layout, String key, int x, int y, int width, int height)
	{
		return setBounds(layout, key, x, y, width, height, false, false);
	}

	synchronized static boolean setBounds(Object layout, String key, int x, int y, int width, int height, boolean temporary, boolean incCur)
	{
		HashMap<String, int[]>[] layoutValues = LAYOUTS.get(layout);
		if (layoutValues != null) {
			HashMap<String, int[]> map = layoutValues[temporary ? VALUES_TEMP : VALUES];
			int[] old = map.get(key);

			if (old == null || old[X] != x || old[Y] != y || old[WIDTH] != width || old[HEIGHT] != height) {
				if (old == null || !incCur) {
					map.put(key, new int[] {x, y, width, height, x + width, y + height});
					return true;
				} else {
					boolean changed = false;

					if (x != LayoutUtil.NOT_SET) {
						if (old[X] == LayoutUtil.NOT_SET || x < old[X]) {
							old[X] = x;
							old[WIDTH] = old[X2] - x;
							changed = true;
						}

						if (width != LayoutUtil.NOT_SET) {
							int x2 = x + width;
							if (old[X2] == LayoutUtil.NOT_SET || x2 > old[X2]) {
								old[X2] = x2;
								old[WIDTH] = x2 - old[X];
								changed = true;
							}
						}
					}

					if (y != LayoutUtil.NOT_SET) {
						if (old[Y] == LayoutUtil.NOT_SET || y < old[Y]) {
							old[Y] = y;
							old[HEIGHT] = old[Y2] - y;
							changed = true;
						}

						if (height != LayoutUtil.NOT_SET) {
							int y2 = y + height;
							if (old[Y2] == LayoutUtil.NOT_SET || y2 > old[Y2]) {
								old[Y2] = y2;
								old[HEIGHT] = y2 - old[Y];
								changed = true;
							}
						}
					}
					return changed;
				}
			}
			return false;
		}

		int[] bounds = new int[] {x, y, width, height, x + width, y + height};

		HashMap<String, int[]> values_temp = new HashMap<>(4);
		if (temporary)
			values_temp.put(key, bounds);

		HashMap<String, int[]> values = new HashMap<>(4);
		if (!temporary)
			values.put(key, bounds);

		LAYOUTS.put(layout, new HashMap[] {values, values_temp});

		return true;
	}

	/** This method clear any weak references right away instead of waiting for the GC. This might be advantageous
	 * if lots of layout are created and disposed of quickly to keep memory consumption down.
	 * @since 3.7.4
	 */
	public synchronized static void clearWeakReferencesNow()
	{
		LAYOUTS.clear();
	}

	public synchronized static boolean clearBounds(Object layout, String key)
	{
		HashMap<String, int[]>[] layoutValues = LAYOUTS.get(layout);
		if (layoutValues != null)
			return layoutValues[VALUES].remove(key) != null;
		return false;
	}

	synchronized static void clearTemporaryBounds(Object layout)
	{
		HashMap<String, int[]>[] layoutValues = LAYOUTS.get(layout);
		if (layoutValues != null)
			layoutValues[VALUES_TEMP].clear();
	}
}
