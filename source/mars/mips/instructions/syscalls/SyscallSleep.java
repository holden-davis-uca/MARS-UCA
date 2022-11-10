package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.RegisterFile;

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
 * Service to cause the MARS Java thread to sleep for (at least) the specified
 * number of milliseconds. This timing will not be precise as the Java
 * implementation will add some overhead.
 */

public class SyscallSleep extends AbstractSyscall {

	/**
	 * Build an instance of the syscall with its default service number and name.
	 */
	public SyscallSleep() {
		super(32, "Sleep");
	}

	/**
	 * System call to cause the MARS Java thread to sleep for (at least) the
	 * specified number of milliseconds. This timing will not be precise as the Java
	 * implementation will add some overhead.
	 */
	@Override
	public void simulate(final ProgramStatement statement) throws ProcessingException {
		// Input arguments: $a0 is the length of time to sleep in milliseconds.

		try {
			Thread.sleep(RegisterFile.getValue(4)); // units of milliseconds  1000 millisec = 1 sec.
		} catch (final InterruptedException e) {
			return; // no exception handling
		}
	}

}
