package mars.mips.instructions;

import java.util.ArrayList;
import java.util.HashMap;

import mars.Globals;
import mars.mips.instructions.syscalls.Syscall;
import mars.mips.instructions.syscalls.SyscallNumberOverride;
import mars.util.FilenameFinder;

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

/****************************************************************************/
/* This class provides functionality to bring external Syscall definitions
 * into MARS.  This permits anyone with knowledge of the Mars public interfaces,
 * in particular of the Memory and Register classes, to write custom MIPS syscall
 * functions. This is adapted from the ToolLoader class, which is in turn adapted
 * from Bret Barker's GameServer class from the book "Developing Games In Java".
 */

class SyscallLoader {

	private static final String CLASS_PREFIX = "mars.mips.instructions.syscalls.";
	private static final String SYSCALLS_DIRECTORY_PATH = "mars/mips/instructions/syscalls";
	private static final String SYSCALL_INTERFACE = "Syscall.class";
	private static final String SYSCALL_ABSTRACT = "AbstractSyscall.class";
	private static final String CLASS_EXTENSION = "class";

	private ArrayList syscallList;

	/*
	  *  Dynamically loads Syscalls into an ArrayList.  This method is adapted from
	  *  the loadGameControllers() method in Bret Barker's GameServer class.
	  *  Barker (bret@hypefiend.com) is co-author of the book "Developing Games
	  *  in Java".  Also see the "loadMarsTools()" method from ToolLoader class.
	  */
	void loadSyscalls() {
		syscallList = new ArrayList();
		// grab all class files in the same directory as Syscall
		final ArrayList candidates = FilenameFinder.getFilenameList(this.getClass().getClassLoader(),
				SYSCALLS_DIRECTORY_PATH, CLASS_EXTENSION);
		final HashMap syscalls = new HashMap();
		for (int i = 0; i < candidates.size(); i++) {
			final String file = (String) candidates.get(i);
			// Do not add class if already encountered (happens if run in MARS development directory)
			if (syscalls.containsKey(file)) {
				continue;
			} else {
				syscalls.put(file, file);
			}
			if (!file.equals(SYSCALL_INTERFACE) && !file.equals(SYSCALL_ABSTRACT)) {
				try {
					// grab the class, make sure it implements Syscall, instantiate, add to list
					final String syscallClassName = CLASS_PREFIX + file.substring(0, file.indexOf(CLASS_EXTENSION) - 1);
					final Class clas = Class.forName(syscallClassName);
					if (!Syscall.class.isAssignableFrom(clas)) { continue; }
					final Syscall syscall = (Syscall) clas.newInstance();
					if (findSyscall(syscall.getNumber()) == null) {
						syscallList.add(syscall);
					} else {
						throw new Exception("Duplicate service number: " + syscall.getNumber()
								+ " already registered to " + findSyscall(syscall.getNumber()).getName());
					}
				} catch (final Exception e) {
					System.out.println("Error instantiating Syscall from file " + file + ": " + e);
					System.exit(0);
				}
			}
		}
		syscallList = processSyscallNumberOverrides(syscallList);
		return;
	}

	// Will get any syscall number override specifications from MARS config file and
	// process them.  This will alter syscallList entry for affected names.
	private ArrayList processSyscallNumberOverrides(final ArrayList syscallList) {
		final ArrayList overrides = new Globals().getSyscallOverrides();
		SyscallNumberOverride override;
		Syscall syscall;
		for (int index = 0; index < overrides.size(); index++) {
			override = (SyscallNumberOverride) overrides.get(index);
			boolean match = false;
			for (int i = 0; i < syscallList.size(); i++) {
				syscall = (Syscall) syscallList.get(i);
				if (override.getName().equals(syscall.getName())) {
					// we have a match to service name, assign new number
					syscall.setNumber(override.getNumber());
					match = true;
				}
			}
			if (!match) {
				System.out.println("Error: syscall name '" + override.getName()
						+ "' in config file does not match any name in syscall list");
				System.exit(0);
			}
		}
		// Wait until end to check for duplicate numbers.  To do so earlier
		// would disallow for instance the exchange of numbers between two
		// services.  This is N-squared operation but N is small.
		// This will also detect duplicates that accidently occur from addition
		// of a new Syscall subclass to the collection, even if the config file
		// does not contain any overrides.
		Syscall syscallA, syscallB;
		boolean duplicates = false;
		for (int i = 0; i < syscallList.size(); i++) {
			syscallA = (Syscall) syscallList.get(i);
			for (int j = i + 1; j < syscallList.size(); j++) {
				syscallB = (Syscall) syscallList.get(j);
				if (syscallA.getNumber() == syscallB.getNumber()) {
					System.out.println("Error: syscalls " + syscallA.getName() + " and " + syscallB.getName()
							+ " are both assigned same number " + syscallA.getNumber());
					duplicates = true;
				}
			}
		}
		if (duplicates) { System.exit(0); }
		return syscallList;
	}

	/*
	 * Method to find Syscall object associated with given service number.
	 * Returns null if no associated object found.
	 */
	Syscall findSyscall(final int number) {
		// linear search is OK since number of syscalls is small.
		Syscall service, match = null;
		if (syscallList == null) { loadSyscalls(); }
		for (int index = 0; index < syscallList.size(); index++) {
			service = (Syscall) syscallList.get(index);
			if (service.getNumber() == number) { match = service; }
		}
		return match;
	}
}
