package mars.mips.instructions.syscalls;

import javax.swing.JOptionPane;

import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
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
 * Service to display a message to user.
 */

public class SyscallMessageDialogInt extends AbstractSyscall {

	/**
	 * Build an instance of the syscall with its default service number and name.
	 */
	public SyscallMessageDialogInt() {
		super(56, "MessageDialogInt");
	}

	/**
	 * System call to display a message to user.
	 */
	@Override
	public void simulate(final ProgramStatement statement) throws ProcessingException {
		// Input arguments:
		//   $a0 = address of null-terminated string that is an information-type message to user
		//   $a1 = int value to display in string form after the first message
		// Output: none

		String message = new String(); // = "";
		int byteAddress = RegisterFile.getValue(4);
		final char ch[] = { ' ' }; // Need an array to convert to String
		try {
			ch[0] = (char) Globals.memory.getByte(byteAddress);
			while (ch[0] != 0) // only uses single location ch[0]
			{
				message = message.concat(new String(ch)); // parameter to String constructor is a char[] array
				byteAddress++;
				ch[0] = (char) Globals.memory.getByte(byteAddress);
			}
		} catch (final AddressErrorException e) {
			throw new ProcessingException(statement, e);
		}

		// Display the dialog.
		JOptionPane.showMessageDialog(null, message + Integer.toString(RegisterFile.getValue(5)), null,
				JOptionPane.INFORMATION_MESSAGE);

	}

}
