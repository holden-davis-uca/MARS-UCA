package mars.mips.instructions.syscalls;

import java.util.Random;

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
 * Service to set seed for the underlying Java pseudorandom number generator. No
 * values are returned.
 */

public class SyscallRandSeed extends AbstractSyscall {

	/**
	 * Build an instance of the syscall with its default service number and name.
	 */
	public SyscallRandSeed() {
		super(40, "RandSeed");
	}

	/**
	 * Set the seed of the underlying Java pseudorandom number generator.
	 */
	@Override
	public void simulate(final ProgramStatement statement) throws ProcessingException {
		// Arguments: $a0 = index of pseudorandom number generator
		//   $a1 = seed for pseudorandom number generator.
		// Result: No values are returned. Sets the seed of the underlying Java pseudorandom number generator.

		final Integer index = new Integer(RegisterFile.getValue(4));
		final Random stream = (Random) RandomStreams.randomStreams.get(index);
		if (stream == null) {
			RandomStreams.randomStreams.put(index, new Random(RegisterFile.getValue(5)));
		} else {
			stream.setSeed(RegisterFile.getValue(5));
		}
	}

}
