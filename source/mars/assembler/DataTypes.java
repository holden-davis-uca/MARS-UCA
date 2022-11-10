package mars.assembler;

/*
 * Copyright (c) 2003-2006, Pete Sanderson and Kenneth Vollmar
 *
 * Developed by Pete Sanderson (psanderson@otterbein.edu) and Kenneth Vollmar
 * (kenvollmar@missouristate.edu)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * (MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * Information about MIPS data types.
 *
 * @author Pete Sanderson
 * @version August 2003
 **/

public final class DataTypes {

	/** Number of bytes occupied by MIPS double is 8. **/
	public static final int DOUBLE_SIZE = 8;
	/** Number of bytes occupied by MIPS float is 4. **/
	public static final int FLOAT_SIZE = 4;
	/** Number of bytes occupied by MIPS word is 4. **/
	public static final int WORD_SIZE = 4;
	/** Number of bytes occupied by MIPS halfword is 2. **/
	public static final int HALF_SIZE = 2;
	/** Number of bytes occupied by MIPS byte is 1. **/
	public static final int BYTE_SIZE = 1;
	/** Number of bytes occupied by MIPS character is 1. **/
	public static final int CHAR_SIZE = 1;
	/** Maximum value that can be stored in a MIPS word is 2<sup>31</sup>-1 **/
	public static final int MAX_WORD_VALUE = Integer.MAX_VALUE;
	/** Lowest value that can be stored in a MIPS word is -2<sup>31</sup> **/
	public static final int MIN_WORD_VALUE = Integer.MIN_VALUE;
	/** Maximum value that can be stored in a MIPS halfword is 2<sup>15</sup>-1 **/
	public static final int MAX_HALF_VALUE = 32767; //(int)Math.pow(2,15) - 1;
	/** Lowest value that can be stored in a MIPS halfword is -2<sup>15</sup> **/
	public static final int MIN_HALF_VALUE = -32768; //0 - (int) Math.pow(2,15);
	/**
	 * Maximum value that can be stored in an unsigned MIPS halfword is
	 * 2<sup>16</sup>-1
	 **/
	public static final int MAX_UHALF_VALUE = 65535;
	/** Lowest value that can be stored in na unsigned MIPS halfword is 0 **/
	public static final int MIN_UHALF_VALUE = 0;
	/** Maximum value that can be stored in a MIPS byte is 2<sup>7</sup>-1 **/
	public static final int MAX_BYTE_VALUE = Byte.MAX_VALUE;
	/** Lowest value that can be stored in a MIPS byte is -2<sup>7</sup> **/
	public static final int MIN_BYTE_VALUE = Byte.MIN_VALUE;
	/**
	 * Maximum positive finite value that can be stored in a MIPS float is same as
	 * Java Float
	 **/
	public static final double MAX_FLOAT_VALUE = Float.MAX_VALUE;
	/**
	 * Largest magnitude negative value that can be stored in a MIPS float (negative
	 * of the max)
	 **/
	public static final double LOW_FLOAT_VALUE = -Float.MAX_VALUE;
	/**
	 * Maximum positive finite value that can be stored in a MIPS double is same as
	 * Java Double
	 **/
	public static final double MAX_DOUBLE_VALUE = Double.MAX_VALUE;
	/**
	 * Largest magnitude negative value that can be stored in a MIPS double(negative
	 * of the max)
	 **/
	public static final double LOW_DOUBLE_VALUE = -Double.MAX_VALUE;

	/**
	 * Get length in bytes for numeric MIPS directives.
	 *
	 * @param direct Directive to be measured.
	 * @return Returns length in bytes for values of that type. If type is not
	 *         numeric (or not implemented yet), returns 0.
	 **/

	public static int getLengthInBytes(final Directives direct) {
		if (direct == Directives.FLOAT) {
			return FLOAT_SIZE;
		} else if (direct == Directives.DOUBLE) {
			return DOUBLE_SIZE;
		} else if (direct == Directives.WORD) {
			return WORD_SIZE;
		} else if (direct == Directives.HALF) {
			return HALF_SIZE;
		} else if (direct == Directives.BYTE) {
			return BYTE_SIZE;
		} else {
			return 0;
		}
	}

	/**
	 * Determines whether given integer value falls within value range for given
	 * directive.
	 *
	 * @param direct Directive that controls storage allocation for value.
	 * @param value  The value to be stored.
	 * @return Returns <tt>true</tt> if value can be stored in the number of bytes
	 *         allowed by the given directive (.word, .half, .byte), <tt>false</tt>
	 *         otherwise.
	 **/
	public static boolean outOfRange(final Directives direct, final int value) {
		if (direct == Directives.HALF && (value < MIN_HALF_VALUE || value > MAX_HALF_VALUE)) {
			return true;
		} else if (direct == Directives.BYTE && (value < MIN_BYTE_VALUE || value > MAX_BYTE_VALUE)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Determines whether given floating point value falls within value range for
	 * given directive. For float, this refers to range of the data type, not
	 * precision. Example: 1.23456789012345 be stored in a float with loss of
	 * precision. It's within the range. But 1.23e500 cannot be stored in a float
	 * because the exponent 500 is too large (float allows 8 bits for exponent).
	 *
	 * @param direct Directive that controls storage allocation for value.
	 * @param value  The value to be stored.
	 * @return Returns <tt>true</tt> if value is within range of the given directive
	 *         (.float, .double), <tt>false</tt> otherwise.
	 **/
	public static boolean outOfRange(final Directives direct, final double value) {
		if (direct == Directives.FLOAT && (value < LOW_FLOAT_VALUE || value > MAX_FLOAT_VALUE)) {
			return true;
		} else {
			return false;
		}
	}
}
