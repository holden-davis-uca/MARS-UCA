package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
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
 * Service to display integer stored in $a0 on the console.
 */

public class SyscallPrintInt extends AbstractSyscall {

	/**
	 * Build an instance of the Print Integer syscall. Default service number is 1
	 * and name is "PrintInt".
	 */
	public SyscallPrintInt() {
		super(1, "PrintInt");
	}

	/**
	 * Performs syscall function to print on the console the integer stored in $a0.
	 */
	@Override
	public void simulate(final ProgramStatement statement) throws ProcessingException {
		SystemIO.printString(new Integer(RegisterFile.getValue(4)).toString());
	}
}
