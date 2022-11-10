package mars.assembler;

import java.util.ArrayList;

import mars.ErrorList;
import mars.ErrorMessage;
import mars.Globals;
import mars.mips.instructions.Instruction;
import mars.util.Binary;

/*
 * Copyright (c) 2003-2008, Pete Sanderson and Kenneth Vollmar
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
 * Provides utility method related to MIPS operand formats.
 *
 * @author Pete Sanderson
 * @version August 2003
 */

public class OperandFormat {

	private OperandFormat() {}

	/*
	* Syntax test for correct match in both numbers and types of operands.
	*
	* @param candidateList List of tokens generated from programmer's MIPS statement.
	* @param spec The (resumably best matched) MIPS instruction.
	* @param errors ErrorList into which any error messages generated here will be added.
	*
	* @return Returns <tt>true</tt> if the programmer's statement matches the MIPS
	* specification, else returns <tt>false</tt>.
	*/

	static boolean tokenOperandMatch(final TokenList candidateList, final Instruction inst, final ErrorList errors) {
		if (!numOperandsCheck(candidateList, inst, errors)) { return false; }
		if (!operandTypeCheck(candidateList, inst, errors)) { return false; }
		return true;
	}

	/*
	* If candidate operator token matches more than one instruction mnemonic, then select
	* first such Instruction that has an exact operand match.  If none match,
	* return the first Instruction and let client deal with operand mismatches.
	*/
	static Instruction bestOperandMatch(final TokenList tokenList, final ArrayList instrMatches) {
		if (instrMatches == null) { return null; }
		if (instrMatches.size() == 1) { return (Instruction) instrMatches.get(0); }
		for (int i = 0; i < instrMatches.size(); i++) {
			final Instruction potentialMatch = (Instruction) instrMatches.get(i);
			if (tokenOperandMatch(tokenList, potentialMatch, new ErrorList())) { return potentialMatch; }
		}
		return (Instruction) instrMatches.get(0);
	}

	// Simply check to see if numbers of operands are correct and generate error message if not.
	private static boolean numOperandsCheck(final TokenList cand, final Instruction spec, final ErrorList errors) {
		final int numOperands = cand.size() - 1;
		final int reqNumOperands = spec.getTokenList().size() - 1;
		final Token operator = cand.get(0);
		if (numOperands == reqNumOperands) {
			return true;
		} else if (numOperands < reqNumOperands) {
			final String mess = "Too few or incorrectly formatted operands. Expected: " + spec.getExampleFormat();
			generateMessage(operator, mess, errors);
		} else {
			final String mess = "Too many or incorrectly formatted operands. Expected: " + spec.getExampleFormat();
			generateMessage(operator, mess, errors);
		}
		return false;
	}

	// Generate error message if operand is not of correct type for this operation & operand position
	private static boolean operandTypeCheck(final TokenList cand, final Instruction spec, final ErrorList errors) {
		Token candToken, specToken;
		TokenTypes candType, specType;
		for (int i = 1; i < spec.getTokenList().size(); i++) {
			candToken = cand.get(i);
			specToken = spec.getTokenList().get(i);
			candType = candToken.getType();
			specType = specToken.getType();
			// Type mismatch is error EXCEPT when (1) spec calls for register name and candidate is
			// register number, (2) spec calls for register number, candidate is register name and
			// names are permitted, (3)spec calls for integer of specified max bit length and
			// candidate is integer of smaller bit length.
			// Type match is error when spec calls for register name, candidate is register name, and
			// names are not permitted.

			// added 2-July-2010 DPS
			// Not an error if spec calls for identifier and candidate is operator, since operator names can be used as labels.
			if (specType == TokenTypes.IDENTIFIER && candType == TokenTypes.OPERATOR) {
				final Token replacement = new Token(TokenTypes.IDENTIFIER, candToken.getValue(), candToken
						.getSourceMIPSprogram(), candToken.getSourceLine(), candToken.getStartPos());
				cand.set(i, replacement);
				continue;
			}
			// end 2-July-2010 addition

			if ((specType == TokenTypes.REGISTER_NAME || specType == TokenTypes.REGISTER_NUMBER)
					&& candType == TokenTypes.REGISTER_NAME) {
				if (Globals.getSettings().getBareMachineEnabled()) {
					// On 10-Aug-2010, I noticed this cannot happen since the IDE provides no access
					// to this setting, whose default value is false.
					generateMessage(candToken, "Use register number instead of name.  See Settings.", errors);
					return false;
				} else {
					continue;
				}
			}
			if (specType == TokenTypes.REGISTER_NAME && candType == TokenTypes.REGISTER_NUMBER) { continue; }
			if (specType == TokenTypes.INTEGER_16 && candType == TokenTypes.INTEGER_5
					|| specType == TokenTypes.INTEGER_16U && candType == TokenTypes.INTEGER_5
					|| specType == TokenTypes.INTEGER_32 && candType == TokenTypes.INTEGER_5
					|| specType == TokenTypes.INTEGER_32 && candType == TokenTypes.INTEGER_16U
					|| specType == TokenTypes.INTEGER_32 && candType == TokenTypes.INTEGER_16) {
				continue;
			}
			if (candType == TokenTypes.INTEGER_16U || candType == TokenTypes.INTEGER_16) {
				final int temp = Binary.stringToInt(candToken.getValue());
				if (specType == TokenTypes.INTEGER_16 && candType == TokenTypes.INTEGER_16U
						&& temp >= DataTypes.MIN_HALF_VALUE && temp <= DataTypes.MAX_HALF_VALUE) {
					continue;
				}
				if (specType == TokenTypes.INTEGER_16U && candType == TokenTypes.INTEGER_16
						&& temp >= DataTypes.MIN_UHALF_VALUE && temp <= DataTypes.MAX_UHALF_VALUE) {
					continue;
				}
			}
			if (specType == TokenTypes.INTEGER_5 && candType == TokenTypes.INTEGER_16
					|| specType == TokenTypes.INTEGER_5 && candType == TokenTypes.INTEGER_16U
					|| specType == TokenTypes.INTEGER_5 && candType == TokenTypes.INTEGER_32
					|| specType == TokenTypes.INTEGER_16 && candType == TokenTypes.INTEGER_16U
					|| specType == TokenTypes.INTEGER_16U && candType == TokenTypes.INTEGER_16
					|| specType == TokenTypes.INTEGER_16U && candType == TokenTypes.INTEGER_32
					|| specType == TokenTypes.INTEGER_16 && candType == TokenTypes.INTEGER_32) {
				generateMessage(candToken, "operand is out of range", errors);
				return false;
			}
			if (candType != specType) {
				generateMessage(candToken, "operand is of incorrect type", errors);
				return false;
			}
		}

		/********
		 * nice little debugging code to see which operand format the operands for this
		 * source code instruction matched. System.out.print("Candidate: "); for (int
		 * i=1; i<spec.size(); i++) { System.out.print(cand.get(i).getValue()+" "); }
		 * System.out.print("Matched Spec: "); for (int i=1; i<spec.size(); i++) {
		 * System.out.print(spec.get(i).getValue()+" "); } System.out.println();
		 */

		return true;
	}

	// Handy utility for all parse errors...
	private static void generateMessage(final Token token, final String mess, final ErrorList errors) {
		errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\""
				+ token.getValue() + "\": " + mess));
		return;
	}

}
