package mars.mips.instructions.syscalls;

import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.RegisterFile;
import mars.util.SystemIO;

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
 * Service to display string stored starting at address in $a0 onto the console.
 */

public class SyscallPrintString extends AbstractSyscall {

	/**
	 * Build an instance of the Print String syscall. Default service number is 4
	 * and name is "PrintString".
	 */
	public SyscallPrintString() {
		super(4, "PrintString");
	}

	/**
	 * Performs syscall function to print string stored starting at address in $a0.
	 */
	@Override
	public void simulate(final ProgramStatement statement) throws ProcessingException {
		int byteAddress = RegisterFile.getValue(4);
		char ch = 0;
		try {
			ch = (char) Globals.memory.getByte(byteAddress);
			// won't stop until NULL byte reached!
			while (ch != 0) {
				SystemIO.printString(new Character(ch).toString());
				byteAddress++;
				ch = (char) Globals.memory.getByte(byteAddress);
			}
		} catch (final AddressErrorException e) {
			throw new ProcessingException(statement, e);
		}
	}
}
