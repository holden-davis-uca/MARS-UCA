package themeengine.include.net.miginfocom.layout;

import java.beans.*;
import java.io.*;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.TreeSet;
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

/** A utility class that has only static helper methods.
 */
public final class LayoutUtil
{
	/** A substitute value for a really large value. Integer.MAX_VALUE is not used since that means a lot of defensive code
	 * for potential overflow must exist in many places. This value is large enough for being unreasonable yet it is hard to
	 * overflow.
	 */
	public static final int INF = (Integer.MAX_VALUE >> 10) - 100; // To reduce likelihood of overflow errors when calculating.

	/** Tag int for a value that in considered "not set". Used as "null" element in int arrays.
	 */
	static final int NOT_SET = Integer.MIN_VALUE + 12346;   // Magic value...

	// Index for the different sizes
	public static final int MIN = 0;
	public static final int PREF = 1;
	public static final int MAX = 2;

	public static final int HORIZONTAL = 0;
	public static final int VERTICAL = 1;

	private static volatile WeakHashMap<Object, String> CR_MAP = null;
	private static volatile WeakHashMap<Object, Boolean> DT_MAP = null;      // The Containers that have design time. Value not used.
	private static int eSz = 0;
	private static int globalDebugMillis = 0;
    public static final boolean HAS_BEANS = hasBeans();

    private static boolean hasBeans()
    {
        try {
            LayoutUtil.class.getClassLoader().loadClass("java.beans.Beans");
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

	private LayoutUtil()
	{
	}

	/** Returns the current version of MiG Layout.
	 * @return The current version of MiG Layout. E.g. "3.6.3" or "4.0"
	 */
	public static String getVersion()
	{
		return "5.0";
	}

	/** If global debug should be on or off. If &gt; 0 then debug is turned on for all MigLayout
	 * instances.
	 * @return The current debug milliseconds.
	 * @see LC#setDebugMillis(int)
	 */
	public static int getGlobalDebugMillis()
	{
		return globalDebugMillis;
	}

	/** If global debug should be on or off. If &gt; 0 then debug is turned on for all MigLayout
	 * instances.
	 * <p>
	 * Note! This is a passive value and will be read by panels when the needed, which is normally
	 * when they repaint/layout.
	 * @param millis The new debug milliseconds. 0 turns of global debug and leaves debug up to every
	 * individual panel.
	 * @see LC#setDebugMillis(int)
	 */
	public static void setGlobalDebugMillis(int millis)
	{
		globalDebugMillis = millis;
	}

	/** Sets if design time is turned on for a Container in {@link ContainerWrapper}.
	 * @param cw The container to set design time for. <code>null</code> is legal and can be used as
	 * a key to turn on/off design time "in general". Note though that design time "in general" is
	 * always on as long as there is at least one ContainerWrapper with design time.
	 * <p>
	 * <strong>If this method has not ever been called it will default to what
	 * <code>Beans.isDesignTime()</code> returns.</strong> This means that if you call
	 * this method you indicate that you will take responsibility for the design time value.
	 * @param b <code>true</code> means design time on.
	 */
	public static void setDesignTime(ContainerWrapper cw, boolean b)
	{
		if (DT_MAP == null)
			DT_MAP = new WeakHashMap<>();

		DT_MAP.put((cw != null ? cw.getComponent() : null), b);
	}

	/** Returns if design time is turned on for a Container in {@link ContainerWrapper}.
	 * @param cw The container to set design time for. <code>null</code> is legal will return <code>true</code>
	 * if there is at least one <code>ContainerWrapper</code> (or <code>null</code>) that have design time
	 * turned on.
	 * @return If design time is set for <code>cw</code>.
	 */
	public static boolean isDesignTime(ContainerWrapper cw)
	{
		if (DT_MAP == null)
			return HAS_BEANS && Beans.isDesignTime();

		// assume design time "in general" (cw is null) if there is at least one container with design time
		// (for storing constraints creation strings in method putCCString())
		if (cw == null && DT_MAP != null && !DT_MAP.isEmpty() )
			return true;

		if (cw != null && !DT_MAP.containsKey(cw.getComponent()))
			cw = null;

		Boolean b = DT_MAP.get(cw != null ? cw.getComponent() : null);
		return b != null && b;
	}

	/** The size of an empty row or columns in a grid during design time.
	 * @return The number of pixels. Default is 15.
	 */
	public static int getDesignTimeEmptySize()
	{
		return eSz;
	}

	/** The size of an empty row or columns in a grid during design time.
	 * @param pixels The number of pixels. Default is 0 (it was 15 prior to v3.7.2, but since that meant different behaviour
	 * under design time by default it was changed to be 0, same as non-design time). IDE vendors can still set it to 15 to
	 * get the old behaviour.
	 */
	public static void setDesignTimeEmptySize(int pixels)
	{
		eSz = pixels;
	}

	/** Associates <code>con</code> with the creation string <code>s</code>. The <code>con</code> object should
	 * probably have an equals method that compares identities or <code>con</code> objects that .equals() will only
	 * be able to have <b>one</b> creation string.
	 * <p>
	 * If {@link LayoutUtil#isDesignTime(ContainerWrapper)} returns <code>false</code> the method does nothing.
	 * @param con The object. if <code>null</code> the method does nothing.
	 * @param s The creation string. if <code>null</code> the method does nothing.
	 */
	static void putCCString(Object con, String s)
	{
		if (s != null && con != null && isDesignTime(null)) {
			if (CR_MAP == null)
				CR_MAP = new WeakHashMap<>(64);

			CR_MAP.put(con, s);
		}
	}

	/** Sets/add the persistence delegates to be used for a class.
	 * @param c The class to set the registered delegate for.
	 * @param del The new delegate or <code>null</code> to erase to old one.
	 */
	static synchronized void setDelegate(Class<?> c, PersistenceDelegate del)
	{
		try {
			Introspector.getBeanInfo(c, Introspector.IGNORE_ALL_BEANINFO).getBeanDescriptor().setValue("persistenceDelegate", del);
		} catch (Exception ignored) {
		}
	}

	/** Returns strings set with {@link #putCCString(Object, String)} or <code>null</code> if nothing is associated or
	 * {@link LayoutUtil#isDesignTime(ContainerWrapper)} returns <code>false</code>.
	 * @param con The constrain object.
	 * @return The creation string or <code>null</code> if nothing is registered with the <code>con</code> object.
	 */
	static String getCCString(Object con)
	{
		return CR_MAP != null ? CR_MAP.get(con) : null;
	}

	static void throwCC()
	{
		throw new IllegalStateException("setStoreConstraintData(true) must be set for strings to be saved.");
	}

	/** Takes a number on min/preferred/max sizes and resize constraints and returns the calculated sizes which sum should add up to <code>bounds</code>. Whether the sum
	 * will actually equal <code>bounds</code> is dependent on the pref/max sizes and resize constraints.
	 * @param sizes [ix],[MIN][PREF][MAX]. Grid.CompWrap.NOT_SET will be treated as N/A or 0. A "[MIN][PREF][MAX]" array with null elements will be interpreted as very flexible (no bounds)
	 * but if the array itself is null it will not get any size.
	 * @param resConstr Elements can be <code>null</code> and the whole array can be <code>null</code>. <code>null</code> means that the size will not be flexible at all.
	 * Can have length less than <code>sizes</code> in which case the last element should be used for the elements missing.
	 * @param defPushWeights If there is no grow weight for a resConstr the corresponding value of this array is used.
	 * These forced resConstr will be grown last though and only if needed to fill to the bounds.
	 * @param startSizeType The initial size to use. E.g. {@link themeengine.include.net.miginfocom.layout.LayoutUtil#MIN}.
	 * @param bounds To use for relative sizes.
	 * @return The sizes. Array length will match <code>sizes</code>.
	 */
	static int[] calculateSerial(int[][] sizes, ResizeConstraint[] resConstr, Float[] defPushWeights, int startSizeType, int bounds)
	{
		float[] lengths = new float[sizes.length];	// heights/widths that are set
		float usedLength = 0.0f;

		// Give all preferred size to start with
		for (int i = 0; i < sizes.length; i++) {
			if (sizes[i] != null) {
				float len = sizes[i][startSizeType] != NOT_SET ? sizes[i][startSizeType] : 0;
				int newSizeBounded = getBrokenBoundary(len, sizes[i][MIN], sizes[i][MAX]);
				if (newSizeBounded != NOT_SET)
					len = newSizeBounded;

				usedLength += len;
				lengths[i] = len;
			}
		}

		int useLengthI = Math.round(usedLength);
		if (useLengthI != bounds && resConstr != null) {
			boolean isGrow = useLengthI < bounds;

			// Create a Set with the available priorities
			TreeSet<Integer> prioList = new TreeSet<>();
			for (int i = 0; i < sizes.length; i++) {
				ResizeConstraint resC = (ResizeConstraint) getIndexSafe(resConstr, i);
				if (resC != null)
					prioList.add(isGrow ? resC.growPrio : resC.shrinkPrio);
			}
			Integer[] prioIntegers = prioList.toArray(new Integer[0]);

			for (int force = 0; force <= ((isGrow && defPushWeights != null) ? 1 : 0); force++) {    // Run twice if defGrow and the need for growing.
				for (int pr = prioIntegers.length - 1; pr >= 0; pr--) {
					int curPrio = prioIntegers[pr];

					float totWeight = 0f;
					Float[] resizeWeight = new Float[sizes.length];
					for (int i = 0; i < sizes.length; i++) {
						if (sizes[i] == null)   // if no min/pref/max size at all do not grow or shrink.
							continue;

						ResizeConstraint resC = (ResizeConstraint) getIndexSafe(resConstr, i);
						if (resC != null) {
							int prio = isGrow ? resC.growPrio : resC.shrinkPrio;

							if (curPrio == prio) {
								if (isGrow) {
									resizeWeight[i] = (force == 0 || resC.grow != null) ? resC.grow : (defPushWeights[i < defPushWeights.length ? i : defPushWeights.length - 1]);
								} else {
									resizeWeight[i] = resC.shrink;
								}
								if (resizeWeight[i] != null)
									totWeight += resizeWeight[i];
							}
						}
					}

					if (totWeight > 0f) {
						boolean hit;
						do {
							float toChange = bounds - usedLength;
							hit = false;
							float changedWeight = 0f;
							for (int i = 0; i < sizes.length && totWeight > 0.0001f; i++) {

								Float weight = resizeWeight[i];
								if (weight != null) {
									float sizeDelta = toChange * weight / totWeight;
									float newSize = lengths[i] + sizeDelta;

									if (sizes[i] != null) {
										int newSizeBounded = getBrokenBoundary(newSize, sizes[i][MIN], sizes[i][MAX]);
										if (newSizeBounded != NOT_SET) {
											resizeWeight[i] = null;
											hit = true;
											changedWeight += weight;
											newSize = newSizeBounded;
											sizeDelta = newSize - lengths[i];
										}
									}

									lengths[i] = newSize;
									usedLength += sizeDelta;
								}
							}
							totWeight -= changedWeight;
						} while (hit);
					}
				}
			}
		}
		return roundSizes(lengths);
	}

	static Object getIndexSafe(Object[] arr, int ix)
	{
		return arr != null ? arr[ix < arr.length ? ix : arr.length - 1] : null;
	}

	/** Returns the broken boundary if <code>sz</code> is outside the boundaries <code>lower</code> or <code>upper</code>. If both boundaries
	 * are broken, the lower one is returned. If <code>sz</code> is &lt; 0 then <code>new Float(0f)</code> is returned so that no sizes can be
	 * negative.
	 * @param sz The size to check
	 * @param lower The lower boundary (or <code>null</code> for no boundary).
	 * @param upper The upper boundary (or <code>null</code> for no boundary).
	 * @return The broken boundary.
	 */
	private static int getBrokenBoundary(float sz, int lower, int upper)
	{
		if (lower != NOT_SET) {
			if (sz < lower)
				return lower;
		} else if (sz < 0f) {
			return 0;
		}

		if (upper != NOT_SET && sz > upper)
			return upper;

		return NOT_SET;
	}


	static int sum(int[] terms, int start, int len)
	{
		int s = 0;
		for (int i = start, iSz = start + len; i < iSz; i++)
			s += terms[i];
		return s;
	}

	static int sum(int[] terms)
	{
		return sum(terms, 0, terms.length);
	}

	/** Keeps f within min and max. Min is of higher priority if min is larger than max.
	 * @param f The value to clamp
	 * @param min
	 * @param max
	 * @return The clamped value, between min and max.
	 */
	static float clamp(float f, float min, float max)
	{
		return Math.max(min, Math.min(f, max));
	}

	/** Keeps i within min and max. Min is of higher priority if min is larger than max.
	 * @param i The value to clamp
	 * @param min
	 * @param max
	 * @return The clamped value, between min and max.
	 */
	static int clamp(int i, int min, int max)
	{
		return Math.max(min, Math.min(i, max));
	}

	public static int getSizeSafe(int[] sizes, int sizeType)
	{
		if (sizes == null || sizes[sizeType] == NOT_SET)
			return sizeType == MAX ? LayoutUtil.INF : 0;
		return sizes[sizeType];
	}

	static BoundSize derive(BoundSize bs, UnitValue min, UnitValue pref, UnitValue max)
	{
		if (bs == null || bs.isUnset())
			return new BoundSize(min, pref, max, null);

		return new BoundSize(
				min != null ? min : bs.getMin(),
				pref != null ? pref : bs.getPreferred(),
				max != null ? max : bs.getMax(),
				bs.getGapPush(),
				null);
	}

	/** Returns if left-to-right orientation is used. If not set explicitly in the layout constraints the Locale
	 * of the <code>parent</code> is used.
	 * @param lc The constraint if there is one. Can be <code>null</code>.
	 * @param container The parent that may be used to get the left-to-right if lc does not specify this.
	 * @return If left-to-right orientation is currently used.
	 */
	public static boolean isLeftToRight(LC lc, ContainerWrapper container)
	{
		if (lc != null && lc.getLeftToRight() != null)
			return lc.getLeftToRight();

		return container == null || container.isLeftToRight();
	}

	/** Round a number of float sizes into int sizes so that the total length match up
	 * @param sizes The sizes to round
	 * @return An array of equal length as <code>sizes</code>.
	 */
	static int[] roundSizes(float[] sizes)
	{
		int[] retInts = new int[sizes.length];
		float posD = 0;

		for (int i = 0; i < retInts.length; i++) {
			int posI = (int) (posD + 0.5f);

			posD += sizes[i];

			retInts[i] = (int) (posD + 0.5f) - posI;
		}

		return retInts;
	}

	/** Safe equals. null == null, but null never equals anything else.
	 * @param o1 The first object. May be <code>null</code>.
	 * @param o2 The second object. May be <code>null</code>.
	 * @return Returns <code>true</code> if <code>o1</code> and <code>o2</code> are equal (using .equals()) or both are <code>null</code>.
	 */
	static boolean equals(Object o1, Object o2)
	{
		return Objects.equals(o1, o2);
	}

//	static int getBaselineCorrect(Component comp)
//	{
//		Dimension pSize = comp.getPreferredSize();
//		int baseline = comp.getBaseline(pSize.width, pSize.height);
//		int nextBaseline = comp.getBaseline(pSize.width, pSize.height + 1);
//
//		// Amount to add to height when calculating where baseline
//		// lands for a particular height:
//		int padding = 0;
//
//		// Where the baseline is relative to the mid point
//		int baselineOffset = baseline - pSize.height / 2;
//		if (pSize.height % 2 == 0 && baseline != nextBaseline) {
//			padding = 1;
//		} else if (pSize.height % 2 == 1 && baseline == nextBaseline) {
//			baselineOffset--;
//			padding = 1;
//		}
//
//		// The following calculates where the baseline lands for
//		// the height z:
//		return (pSize.height + padding) / 2 + baselineOffset;
//	}


	/** Returns the insets for the side.
	 * @param side top == 0, left == 1, bottom = 2, right = 3.
	 * @param getDefault If <code>true</code> the default insets will get retrieved if <code>lc</code> has none set.
	 * @return The insets for the side. Never <code>null</code>.
	 */
	static UnitValue getInsets(LC lc, int side, boolean getDefault)
	{
		UnitValue[] i = lc.getInsets();
		return (i != null && i[side] != null) ? i[side] : (getDefault ? PlatformDefaults.getPanelInsets(side) : UnitValue.ZERO);
	}

	/** Writes the object and CLOSES the stream. Uses the persistence delegate registered in this class.
	 * @param os The stream to write to. Will be closed.
	 * @param o The object to be serialized.
	 * @param listener The listener to receive the exceptions if there are any. If <code>null</code> not used.
	 */
	static void writeXMLObject(OutputStream os, Object o, ExceptionListener listener)
	{
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(LayoutUtil.class.getClassLoader());

		XMLEncoder encoder = new XMLEncoder(os);

		if (listener != null)
			encoder.setExceptionListener(listener);

		encoder.writeObject(o);
        encoder.close();    // Must be closed to write.

		Thread.currentThread().setContextClassLoader(oldClassLoader);
	}

	private static ByteArrayOutputStream writeOutputStream = null;
	/** Writes an object to XML.
	 * @param out The object out to write to. Will not be closed.
	 * @param o The object to write.
	 */
	public static synchronized void writeAsXML(ObjectOutput out, Object o) throws IOException
	{
		if (writeOutputStream == null)
			writeOutputStream = new ByteArrayOutputStream(16384);

		writeOutputStream.reset();

		writeXMLObject(writeOutputStream, o, e -> e.printStackTrace());

		byte[] buf = writeOutputStream.toByteArray();

		out.writeInt(buf.length);
		out.write(buf);
	}

	private static byte[] readBuf = null;
	/** Reads an object from <code>in</code> using the
	 * @param in The object input to read from.
	 * @return The object. Never <code>null</code>.
	 * @throws IOException If there was a problem saving as XML
	 */
	public static synchronized Object readAsXML(ObjectInput in) throws IOException
	{
		if (readBuf == null)
			readBuf = new byte[16384];

		Thread cThread = Thread.currentThread();
		ClassLoader oldCL = null;

		try {
			oldCL = cThread.getContextClassLoader();
			cThread.setContextClassLoader(LayoutUtil.class.getClassLoader());
		} catch(SecurityException ignored) {
		}

		Object o = null;
		try {
			int length = in.readInt();
			if (length > readBuf.length)
				readBuf = new byte[length];

			in.readFully(readBuf, 0, length);

			o = new XMLDecoder(new ByteArrayInputStream(readBuf, 0, length)).readObject();

		} catch(EOFException ignored) {
		}

		if (oldCL != null)
			cThread.setContextClassLoader(oldCL);

		return o;
	}

	private static final IdentityHashMap<Object, Object> SER_MAP = new IdentityHashMap<>(2);

	/** Sets the serialized object and associates it with <code>caller</code>.
	 * @param caller The object created <code>o</code>
	 * @param o The just serialized object.
	 */
	public static void setSerializedObject(Object caller, Object o)
	{
		synchronized(SER_MAP) {
			SER_MAP.put(caller, o);
		}
	}

	/** Returns the serialized object that are associated with <code>caller</code>. It also removes it from the list.
	 * @param caller The original creator of the object.
	 * @return The object.
	 */
	public static Object getSerializedObject(Object caller)
	{
		synchronized(SER_MAP) {
			return SER_MAP.remove(caller);
		}
	}
}
