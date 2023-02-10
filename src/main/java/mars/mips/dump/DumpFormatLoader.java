package mars.mips.dump;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

import mars.util.FilenameFinder;

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

/****************************************************************************/
/* This class provides functionality to bring external memory dump format definitions
 * into MARS.  This is adapted from the ToolLoader class, which is in turn adapted
 * from Bret Barker's GameServer class from the book "Developing Games In Java".
 */

public class DumpFormatLoader {

	private static final String CLASS_PREFIX = "mars.mips.dump.";
	private static final String DUMP_DIRECTORY_PATH = "mars/mips/dump";
	private static final String SYSCALL_INTERFACE = "DumpFormat.class";
	private static final String CLASS_EXTENSION = "class";

	private static ArrayList formatList = null;

	/**
	 * Dynamically loads dump formats into an ArrayList. This method is adapted from
	 * the loadGameControllers() method in Bret Barker's GameServer class. Barker
	 * (bret@hypefiend.com) is co-author of the book "Developing Games in Java".
	 * Also see the ToolLoader and SyscallLoader classes elsewhere in MARS.
	 */

	public ArrayList loadDumpFormats() {
		// The list will be populated only the first time this method is called.
		if (formatList == null) {
			formatList = new ArrayList();
			// grab all class files in the dump directory
			final ArrayList candidates = FilenameFinder.getFilenameList(this.getClass().getClassLoader(),
					DUMP_DIRECTORY_PATH, CLASS_EXTENSION);
            for (Object candidate : candidates) {
                final String file = (String) candidate;
                try {
                    // grab the class, make sure it implements DumpFormat, instantiate, add to list
                    final String formatClassName = CLASS_PREFIX + file.substring(0, file.indexOf(CLASS_EXTENSION) - 1);
                    final Class clas = Class.forName(formatClassName);
                    if (DumpFormat.class.isAssignableFrom(clas) && !Modifier.isAbstract(clas.getModifiers())
                            && !Modifier.isInterface(clas.getModifiers())) {
                        formatList.add(clas.newInstance());
                    }
                } catch (final Exception e) {
                    System.out.println("Error instantiating DumpFormat from file " + file + ": " + e);
                }
            }
		}
		return formatList;
	}

	public static DumpFormat findDumpFormatGivenCommandDescriptor(final ArrayList formatList,
			final String formatCommandDescriptor) {
		DumpFormat match = null;
        for (Object o : formatList) {
            if (((DumpFormat) o).getCommandDescriptor().equals(formatCommandDescriptor)) {
                match = (DumpFormat) o;
                break;
            }
        }
		return match;
	}

}
