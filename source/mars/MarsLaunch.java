package mars;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import javax.swing.SwingUtilities;

import mars.mips.dump.DumpFormat;
import mars.mips.dump.DumpFormatLoader;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.InvalidRegisterAccessException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.mips.hardware.MemoryConfiguration;
import mars.mips.hardware.MemoryConfigurations;
import mars.mips.hardware.RegisterFile;
import mars.simulator.ProgramArgumentList;
import mars.util.Binary;
import mars.util.FilenameFinder;
import mars.util.MemoryDump;
import mars.venus.VenusUI;
import themeengine.ThemeEngine;

/*
 * Copyright (c) 2003-2012, Pete Sanderson and Kenneth Vollmar
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
 * Launch the Mars application
 *
 * @author Pete Sanderson
 * @version December 2009
 **/

public class MarsLaunch {

	/**
	 * Main takes a number of command line arguments.<br>
	 * Usage: Mars [options] filename<br>
	 * Valid options (not case sensitive, separate by spaces) are:<br>
	 * a -- assemble only, do not simulate<br>
	 * ad -- both a and d<br>
	 * ae<n> -- terminate MARS with integer exit code <n> if an assemble error
	 * occurs.<br>
	 * ascii -- display memory or register contents interpreted as ASCII b -- brief
	 * - do not display register/memory address along with contents<br>
	 * d -- print debugging statements<br>
	 * da -- both a and d<br>
	 * db -- MIPS delayed branching is enabled.<br>
	 * dec -- display memory or register contents in decimal.<br>
	 * dump -- dump memory contents to file. Option has 3 arguments, e.g. <br>
	 * <tt>dump &lt;segment&gt; &lt;format&gt; &lt;file&gt;</tt>. Also supports<br>
	 * an address range (see <i>m-n</i> below). Current supported <br>
	 * segments are <tt>.text</tt> and <tt>.data</tt>. Current supported dump
	 * formats <br>
	 * are <tt>Binary</tt>, <tt>HexText</tt>, <tt>BinaryText</tt>.<br>
	 * h -- display help. Use by itself and with no filename</br>
	 * hex -- display memory or register contents in hexadecimal (default)<br>
	 * ic -- display count of MIPS basic instructions 'executed'"); mc -- set memory
	 * configuration. Option has 1 argument, e.g.<br>
	 * <tt>mc &lt;config$gt;</tt>, where &lt;config$gt; is <tt>Default</tt><br>
	 * for the MARS default 32-bit address space, <tt>CompactDataAtZero</tt> for<br>
	 * a 32KB address space with data segment at address 0, or
	 * <tt>CompactTextAtZero</tt><br>
	 * for a 32KB address space with text segment at address 0.<br>
	 * me -- display MARS messages to standard err instead of standard out. Can
	 * separate via redirection.</br>
	 * nc -- do not display copyright notice (for cleaner redirected/piped
	 * output).</br>
	 * np -- No Pseudo-instructions allowed ("ne" will work also).<br>
	 * p -- Project mode - assemble all files in the same directory as given
	 * file.<br>
	 * se<n> -- terminate MARS with integer exit code <n> if a simulation (run)
	 * error occurs.<br>
	 * sm -- Start execution at Main - Execution will start at program statement
	 * globally labeled main.<br>
	 * smc -- Self Modifying Code - Program can write and branch to either text or
	 * data segment<br>
	 * we -- assembler Warnings will be considered Errors<br>
	 * <n> -- where <n> is an integer maximum count of steps to simulate.<br>
	 * If 0, negative or not specified, there is no maximum.<br>
	 * $<reg> -- where <reg> is number or name (e.g. 5, t3, f10) of register whose
	 * <br>
	 * content to display at end of run. Option may be repeated.<br>
	 * <reg_name> -- where <reg_name> is name (e.g. t3, f10) of register whose <br>
	 * content to display at end of run. Option may be repeated. $ not required.<br>
	 * <m>-<n> -- memory address range from <m> to <n> whose contents to<br>
	 * display at end of run. <m> and <n> may be hex or decimal,<br>
	 * <m> <= <n>, both must be on word boundary. Option may be repeated.<br>
	 * pa -- Program Arguments follow in a space-separated list. This<br>
	 * option must be placed AFTER ALL FILE NAMES, because everything<br>
	 * that follows it is interpreted as a program argument to be<br>
	 * made available to the MIPS program at runtime.<br>
	 **/

	private boolean simulate;
	private int displayFormat;
	private boolean verbose;  // display register name or address along with contents
	private boolean assembleProject; // assemble only the given file or all files in its directory
	private boolean pseudo;  // pseudo instructions allowed in source code or not.
	private boolean delayedBranching;  // MIPS delayed branching is enabled.
	private boolean warningsAreErrors; // Whether assembler warnings should be considered errors.
	private boolean startAtMain; // Whether to start execution at statement labeled 'main' 
	private boolean countInstructions; // Whether to count and report number of instructions executed 
	private boolean selfModifyingCode; // Whether to allow self-modifying code (e.g. write to text segment)
	private static final String rangeSeparator = "-";
	private static final int splashDuration = 2000; // time in MS to show splash screen
	private static final int memoryWordsPerLine = 4; // display 4 memory words, tab separated, per line
	private static final int DECIMAL = 0; // memory and register display format
	private static final int HEXADECIMAL = 1;// memory and register display format
	private static final int ASCII = 2;// memory and register display format
	private ArrayList registerDisplayList;
	private ArrayList memoryDisplayList;
	private ArrayList filenameList;
	private MIPSprogram code;
	private int maxSteps;
	private int instructionCount;
	private PrintStream out; // stream for display of command line output
	private ArrayList dumpTriples = null; // each element holds 3 arguments for dump option
	private ArrayList programArgumentList; // optional program args for MIPS program (becomes argc, argv)
	private int assembleErrorExitCode;  // MARS command exit code to return if assemble error occurs
	private int simulateErrorExitCode;// MARS command exit code to return if simulation error occurs

	public MarsLaunch(final String[] args) {
		final boolean gui = args.length == 0;
		Globals.initialize(gui);
		if (gui) {
			launchIDE(args);
		} else { // running from command line.
			// assure command mode works in headless environment (generates exception if not)
			System.setProperty("java.awt.headless", "true");
			simulate = true;
			displayFormat = HEXADECIMAL;
			verbose = true;
			assembleProject = false;
			pseudo = true;
			delayedBranching = false;
			warningsAreErrors = false;
			startAtMain = false;
			countInstructions = false;
			selfModifyingCode = false;
			instructionCount = 0;
			assembleErrorExitCode = 0;
			simulateErrorExitCode = 0;
			registerDisplayList = new ArrayList();
			memoryDisplayList = new ArrayList();
			filenameList = new ArrayList();
			MemoryConfigurations.setCurrentConfiguration(MemoryConfigurations.getDefaultConfiguration());
			// do NOT use Globals.program for command line MARS -- it triggers 'backstep' log.
			code = new MIPSprogram();
			maxSteps = -1;
			out = System.out;
			if (parseCommandArgs(args)) {
				if (runCommand()) {
					displayMiscellaneousPostMortem();
					displayRegistersPostMortem();
					displayMemoryPostMortem();
				}
				dumpSegments();
			}
			System.exit(Globals.exitCode);
		}
	}

	/////////////////////////////////////////////////////////////
	// Perform any specified dump operations.  See "dump" option.
	//

	private void dumpSegments() {

		if (dumpTriples == null) { return; }

		for (int i = 0; i < dumpTriples.size(); i++) {
			final String[] triple = (String[]) dumpTriples.get(i);
			final File file = new File(triple[2]);
			Integer[] segInfo = MemoryDump.getSegmentBounds(triple[0]);
			// If not segment name, see if it is address range instead.  DPS 14-July-2008
			if (segInfo == null) {
				try {
					final String[] memoryRange = checkMemoryAddressRange(triple[0]);
					segInfo = new Integer[2];
					segInfo[0] = new Integer(Binary.stringToInt(memoryRange[0])); // low end of range
					segInfo[1] = new Integer(Binary.stringToInt(memoryRange[1])); // high end of range
				} catch (final NumberFormatException nfe) {
					segInfo = null;
				} catch (final NullPointerException npe) {
					segInfo = null;
				}
			}
			if (segInfo == null) {
				out.println("Error while attempting to save dump, segment/address-range " + triple[0] + " is invalid!");
				continue;
			}
			final DumpFormatLoader loader = new DumpFormatLoader();
			final ArrayList dumpFormats = loader.loadDumpFormats();
			final DumpFormat format = DumpFormatLoader.findDumpFormatGivenCommandDescriptor(dumpFormats, triple[1]);
			if (format == null) {
				out.println("Error while attempting to save dump, format " + triple[1] + " was not found!");
				continue;
			}
			try {
				final int highAddress = Globals.memory.getAddressOfFirstNull(segInfo[0], segInfo[1])
						- Memory.WORD_LENGTH_BYTES;
				if (highAddress < segInfo[0].intValue()) {
					out.println("This segment has not been written to, there is nothing to dump.");
					continue;
				}
				format.dumpMemoryRange(file, segInfo[0], highAddress);
			} catch (final FileNotFoundException e) {
				out.println("Error while attempting to save dump, file " + file + " was not found!");
				continue;
			} catch (final AddressErrorException e) {
				out.println("Error while attempting to save dump, file " + file + "!  Could not access address: " + e
						.getAddress() + "!");
				continue;
			} catch (final IOException e) {
				out.println("Error while attempting to save dump, file " + file + "!  Disk IO failed!");
				continue;
			}
		}
	}

	/////////////////////////////////////////////////////////////////
	// There are no command arguments, so run in interactive mode by
	// launching the GUI-fronted integrated development environment.

	private void launchIDE(String[] args) {
		// System.setProperty("apple.laf.useScreenMenuBar", "true"); // Puts MARS menu on Mac OS menu bar
		new MarsSplashScreen(splashDuration).showSplash();
		SwingUtilities.invokeLater(() -> {
			ThemeEngine.setup(args);
			new VenusUI("MARS-UCA " + Globals.version);
		});
		return;
	}

	//////////////////////////////////////////////////////////////////////
	// Parse command line arguments.  The initial parsing has already been
	// done, since each space-separated argument is already in a String array
	// element.  Here, we check for validity, set switch variables as appropriate
	// and build data structures.  For help option (h), display the help.
	// Returns true if command args parse OK, false otherwise.

	private boolean parseCommandArgs(final String[] args) {
		final String noCopyrightSwitch = "nc";
		final String displayMessagesToErrSwitch = "me";
		boolean argsOK = true;
		boolean inProgramArgumentList = false;
		programArgumentList = null;
		if (args.length == 0) {
			return true; // should not get here...
		}
		// If the option to display MARS messages to standard erro is used,
		// it must be processed before any others (since messages may be
		// generated during option parsing).
		processDisplayMessagesToErrSwitch(args, displayMessagesToErrSwitch);
		displayCopyright(args, noCopyrightSwitch);  // ..or not..
		if (args.length == 1 && args[0].equals("h")) {
			displayHelp();
			return false;
		}
		for (int i = 0; i < args.length; i++) {
			// We have seen "pa" switch, so all remaining args are program args
			// that will become "argc" and "argv" for the MIPS program.
			if (inProgramArgumentList) {
				if (programArgumentList == null) { programArgumentList = new ArrayList(); }
				programArgumentList.add(args[i]);
				continue;
			}
			// Once we hit "pa", all remaining command args are assumed
			// to be program arguments.
			if (args[i].toLowerCase().equals("pa")) {
				inProgramArgumentList = true;
				continue;
			}
			// messages-to-standard-error switch already processed, so ignore.
			if (args[i].toLowerCase().equals(displayMessagesToErrSwitch)) { continue; }
			// no-copyright switch already processed, so ignore.
			if (args[i].toLowerCase().equals(noCopyrightSwitch)) { continue; }
			if (args[i].toLowerCase().equals("dump")) {
				if (args.length <= i + 3) {
					out.println("Dump command line argument requires a segment, format and file name.");
					argsOK = false;
				} else {
					if (dumpTriples == null) { dumpTriples = new ArrayList(); }
					dumpTriples.add(new String[] { args[++i], args[++i], args[++i] });
					//simulate = false;
				}
				continue;
			}
			if (args[i].toLowerCase().equals("mc")) {
				final String configName = args[++i];
				final MemoryConfiguration config = MemoryConfigurations.getConfigurationByName(configName);
				if (config == null) {
					out.println("Invalid memory configuration: " + configName);
					argsOK = false;
				} else {
					MemoryConfigurations.setCurrentConfiguration(config);
				}
				continue;
			}
			// Set MARS exit code for assemble error
			if (args[i].toLowerCase().indexOf("ae") == 0) {
				final String s = args[i].substring(2);
				try {
					assembleErrorExitCode = Integer.decode(s);
					continue;
				} catch (final NumberFormatException nfe) {
					// Let it fall thru and get handled by catch-all
				}
			}
			// Set MARS exit code for simulate error
			if (args[i].toLowerCase().indexOf("se") == 0) {
				final String s = args[i].substring(2);
				try {
					simulateErrorExitCode = Integer.decode(s);
					continue;
				} catch (final NumberFormatException nfe) {
					// Let it fall thru and get handled by catch-all
				}
			}
			if (args[i].toLowerCase().equals("d")) {
				Globals.debug = true;
				continue;
			}
			if (args[i].toLowerCase().equals("a")) {
				simulate = false;
				continue;
			}
			if (args[i].toLowerCase().equals("ad") || args[i].toLowerCase().equals("da")) {
				Globals.debug = true;
				simulate = false;
				continue;
			}
			if (args[i].toLowerCase().equals("p")) {
				assembleProject = true;
				continue;
			}
			if (args[i].toLowerCase().equals("dec")) {
				displayFormat = DECIMAL;
				continue;
			}
			if (args[i].toLowerCase().equals("hex")) {
				displayFormat = HEXADECIMAL;
				continue;
			}
			if (args[i].toLowerCase().equals("ascii")) {
				displayFormat = ASCII;
				continue;
			}
			if (args[i].toLowerCase().equals("b")) {
				verbose = false;
				continue;
			}
			if (args[i].toLowerCase().equals("db")) {
				delayedBranching = true;
				continue;
			}
			if (args[i].toLowerCase().equals("np") || args[i].toLowerCase().equals("ne")) {
				pseudo = false;
				continue;
			}
			if (args[i].toLowerCase().equals("we")) { // added 14-July-2008 DPS
				warningsAreErrors = true;
				continue;
			}
			if (args[i].toLowerCase().equals("sm")) { // added 17-Dec-2009 DPS
				startAtMain = true;
				continue;
			}
			if (args[i].toLowerCase().equals("smc")) { // added 5-Jul-2013 DPS
				selfModifyingCode = true;
				continue;
			}
			if (args[i].toLowerCase().equals("ic")) { // added 19-Jul-2012 DPS
				countInstructions = true;
				continue;
			}

			if (args[i].indexOf("$") == 0) {
				if (RegisterFile.getUserRegister(args[i]) == null && Coprocessor1.getRegister(args[i]) == null) {
					out.println("Invalid Register Name: " + args[i]);
				} else {
					registerDisplayList.add(args[i]);
				}
				continue;
			}
			// check for register name w/o $.  added 14-July-2008 DPS
			if (RegisterFile.getUserRegister("$" + args[i]) != null || Coprocessor1.getRegister("$"
					+ args[i]) != null) {
				registerDisplayList.add("$" + args[i]);
				continue;
			}
			if (new File(args[i]).exists()) {  // is it a file name?
				filenameList.add(args[i]);
				continue;
			}
			// Check for stand-alone integer, which is the max execution steps option
			try {
				Integer.decode(args[i]);
				maxSteps = Integer.decode(args[i]); // if we got here, it has to be OK 
				continue;
			} catch (final NumberFormatException nfe) {}
			// Check for integer address range (m-n)
			try {
				final String[] memoryRange = checkMemoryAddressRange(args[i]);
				memoryDisplayList.add(memoryRange[0]); // low end of range
				memoryDisplayList.add(memoryRange[1]); // high end of range
				continue;
			} catch (final NumberFormatException nfe) {
				out.println("Invalid/unaligned address or invalid range: " + args[i]);
				argsOK = false;
				continue;
			} catch (final NullPointerException npe) {
				// Do nothing.  next statement will handle it
			}
			out.println("Invalid Command Argument: " + args[i]);
			argsOK = false;
		}
		return argsOK;
	}

	//////////////////////////////////////////////////////////////////////
	// Carry out the mars command: assemble then optionally run
	// Returns false if no simulation (run) occurs, true otherwise.

	private boolean runCommand() {
		boolean programRan = false;
		if (filenameList.size() == 0) { return programRan; }
		try {
			Globals.getSettings().setBooleanSettingNonPersistent(Settings.DELAYED_BRANCHING_ENABLED, delayedBranching);
			Globals.getSettings().setBooleanSettingNonPersistent(Settings.SELF_MODIFYING_CODE_ENABLED,
					selfModifyingCode);
			final File mainFile = new File((String) filenameList.get(0)).getAbsoluteFile();// First file is "main" file
			ArrayList filesToAssemble;
			if (assembleProject) {
				filesToAssemble = FilenameFinder.getFilenameList(mainFile.getParent(), Globals.fileExtensions);
				if (filenameList.size() > 1) {
					// Using "p" project option PLUS listing more than one filename on command line.
					// Add the additional files, avoiding duplicates.
					filenameList.remove(0); // first one has already been processed
					final ArrayList moreFilesToAssemble = FilenameFinder.getFilenameList(filenameList,
							FilenameFinder.MATCH_ALL_EXTENSIONS);
					// Remove any duplicates then merge the two lists.
					for (int index2 = 0; index2 < moreFilesToAssemble.size(); index2++) {
						for (int index1 = 0; index1 < filesToAssemble.size(); index1++) {
							if (filesToAssemble.get(index1).equals(moreFilesToAssemble.get(index2))) {
								moreFilesToAssemble.remove(index2);
								index2--; // adjust for left shift in moreFilesToAssemble...
								break;    // break out of inner loop...
							}
						}
					}
					filesToAssemble.addAll(moreFilesToAssemble);
				}
			} else {
				filesToAssemble = FilenameFinder.getFilenameList(filenameList, FilenameFinder.MATCH_ALL_EXTENSIONS);
			}
			if (Globals.debug) { out.println("--------  TOKENIZING BEGINS  -----------"); }
			final ArrayList MIPSprogramsToAssemble = code.prepareFilesForAssembly(filesToAssemble, mainFile
					.getAbsolutePath(), null);
			if (Globals.debug) { out.println("--------  ASSEMBLY BEGINS  -----------"); }
			// Added logic to check for warnings and print if any. DPS 11/28/06
			final ErrorList warnings = code.assemble(MIPSprogramsToAssemble, pseudo, warningsAreErrors);
			if (warnings != null && warnings.warningsOccurred()) { out.println(warnings.generateWarningReport()); }
			RegisterFile.initializeProgramCounter(startAtMain); // DPS 3/9/09
			if (simulate) {
				// store program args (if any) in MIPS memory
				new ProgramArgumentList(programArgumentList).storeProgramArguments();
				// establish observer if specified
				establishObserver();
				if (Globals.debug) { out.println("--------  SIMULATION BEGINS  -----------"); }
				programRan = true;
				final boolean done = code.simulate(maxSteps);
				if (!done) { out.println("\nProgram terminated when maximum step limit " + maxSteps + " reached."); }
			}
			if (Globals.debug) { out.println("\n--------  ALL PROCESSING COMPLETE  -----------"); }
		} catch (final ProcessingException e) {
			Globals.exitCode = programRan ? simulateErrorExitCode : assembleErrorExitCode;
			out.println(e.errors().generateErrorAndWarningReport());
			out.println("Processing terminated due to errors.");
		}
		return programRan;
	}

	//////////////////////////////////////////////////////////////////////
	// Check for memory address subrange.  Has to be two integers separated
	// by "-"; no embedded spaces.  e.g. 0x00400000-0x00400010
	// If number is not multiple of 4, will be rounded up to next higher.

	private String[] checkMemoryAddressRange(final String arg) throws NumberFormatException {
		String[] memoryRange = null;
		if (arg.indexOf(rangeSeparator) > 0 && arg.indexOf(rangeSeparator) < arg.length() - 1) {
			// assume correct format, two numbers separated by -, no embedded spaces.
			// If that doesn't work it is invalid.
			memoryRange = new String[2];
			memoryRange[0] = arg.substring(0, arg.indexOf(rangeSeparator));
			memoryRange[1] = arg.substring(arg.indexOf(rangeSeparator) + 1);
			// NOTE: I will use homegrown decoder, because Integer.decode will throw
			// exception on address higher than 0x7FFFFFFF (e.g. sign bit is 1).
			if (Binary.stringToInt(memoryRange[0]) > Binary.stringToInt(memoryRange[1]) || !Memory.wordAligned(Binary
					.stringToInt(memoryRange[0])) || !Memory.wordAligned(Binary.stringToInt(memoryRange[1]))) {
				throw new NumberFormatException();
			}
		}
		return memoryRange;
	}

	/////////////////////////////////////////////////////////////////
	// Required for counting instructions executed, if that option is specified.
	// DPS 19 July 2012
	private void establishObserver() {
		if (countInstructions) {
			final Observer instructionCounter = new Observer() {

				private int lastAddress = 0;

				@Override
				public void update(final Observable o, final Object obj) {
					if (obj instanceof AccessNotice) {
						final AccessNotice notice = (AccessNotice) obj;
						if (!notice.accessIsFromMIPS()) { return; }
						if (notice.getAccessType() != AccessNotice.READ) { return; }
						final MemoryAccessNotice m = (MemoryAccessNotice) notice;
						final int a = m.getAddress();
						if (a == lastAddress) { return; }
						lastAddress = a;
						instructionCount++;
					}
				}
			};
			try {
				Globals.memory.addObserver(instructionCounter, Memory.textBaseAddress, Memory.textLimitAddress);
			} catch (final AddressErrorException aee) {
				out.println("Internal error: MarsLaunch uses incorrect text segment address for instruction observer");
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// Displays any specified runtime properties. Initially just instruction count
	// DPS 19 July 2012
	private void displayMiscellaneousPostMortem() {
		if (countInstructions) { out.println("\n" + instructionCount); }
	}

	//////////////////////////////////////////////////////////////////////
	// Displays requested register or registers

	private void displayRegistersPostMortem() {
		int value;  // handy local to use throughout the next couple loops
		// Display requested register contents
		out.println();
		final Iterator regIter = registerDisplayList.iterator();
		while (regIter.hasNext()) {
			final String reg = regIter.next().toString();
			if (RegisterFile.getUserRegister(reg) != null) {
				// integer register
				if (verbose) { out.print(reg + "\t"); }
				value = RegisterFile.getUserRegister(reg).getValue();
				out.println(formatIntForDisplay(value));
			} else {
				// floating point register
				final float fvalue = Coprocessor1.getFloatFromRegister(reg);
				final int ivalue = Coprocessor1.getIntFromRegister(reg);
				double dvalue = Double.NaN;
				long lvalue = 0;
				boolean hasDouble = false;
				try {
					dvalue = Coprocessor1.getDoubleFromRegisterPair(reg);
					lvalue = Coprocessor1.getLongFromRegisterPair(reg);
					hasDouble = true;
				} catch (final InvalidRegisterAccessException irae) {}
				if (verbose) { out.print(reg + "\t"); }
				if (displayFormat == HEXADECIMAL) {
					// display float (and double, if applicable) in hex
					out.print(Binary.binaryStringToHexString(Binary.intToBinaryString(ivalue)));
					if (hasDouble) {
						out.println("\t" + Binary.binaryStringToHexString(Binary.longToBinaryString(lvalue)));
					} else {
						out.println("");
					}
				} else if (displayFormat == DECIMAL) {
					// display float (and double, if applicable) in decimal
					out.print(fvalue);
					if (hasDouble) {
						out.println("\t" + dvalue);
					} else {
						out.println("");
					}
				} else { // displayFormat == ASCII
					out.print(Binary.intToAscii(ivalue));
					if (hasDouble) {
						out.println("\t" + Binary.intToAscii(Binary.highOrderLongToInt(lvalue)) + Binary.intToAscii(
								Binary.lowOrderLongToInt(lvalue)));
					} else {
						out.println("");
					}
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// Formats int value for display: decimal, hex, ascii
	private String formatIntForDisplay(final int value) {
		String strValue;
		switch (displayFormat) {
		case DECIMAL:
			strValue = "" + value;
			break;
		case HEXADECIMAL:
			strValue = Binary.intToHexString(value);
			break;
		case ASCII:
			strValue = Binary.intToAscii(value);
			break;
		default:
			strValue = Binary.intToHexString(value);
		}
		return strValue;
	}

	//////////////////////////////////////////////////////////////////////
	// Displays requested memory range or ranges

	private void displayMemoryPostMortem() {
		int value;
		// Display requested memory range contents
		final Iterator memIter = memoryDisplayList.iterator();
		int addressStart = 0, addressEnd = 0;
		while (memIter.hasNext()) {
			try { // This will succeed; error would have been caught during command arg parse
				addressStart = Binary.stringToInt(memIter.next().toString());
				addressEnd = Binary.stringToInt(memIter.next().toString());
			} catch (final NumberFormatException nfe) {}
			int valuesDisplayed = 0;
			for (int addr = addressStart; addr <= addressEnd; addr += Memory.WORD_LENGTH_BYTES) {
				if (addr < 0 && addressEnd > 0) {
					break;  // happens only if addressEnd is 0x7ffffffc
				}
				if (valuesDisplayed % memoryWordsPerLine == 0) {
					out.print(valuesDisplayed > 0 ? "\n" : "");
					if (verbose) { out.print("Mem[" + Binary.intToHexString(addr) + "]\t"); }
				}
				try {
					// Allow display of binary text segment (machine code) DPS 14-July-2008
					if (Memory.inTextSegment(addr) || Memory.inKernelTextSegment(addr)) {
						final Integer iValue = Globals.memory.getRawWordOrNull(addr);
						value = iValue == null ? 0 : iValue;
					} else {
						value = Globals.memory.getWord(addr);
					}
					out.print(formatIntForDisplay(value) + "\t");
				} catch (final AddressErrorException aee) {
					out.print("Invalid address: " + addr + "\t");
				}
				valuesDisplayed++;
			}
			out.println();
		}
	}

	///////////////////////////////////////////////////////////////////////
	//  If option to display MARS messages to standard err (System.err) is
	//  present, it must be processed before all others.  Since messages may
	//  be output as early as during the command parse.
	private void processDisplayMessagesToErrSwitch(final String[] args, final String displayMessagesToErrSwitch) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].toLowerCase().equals(displayMessagesToErrSwitch)) {
				out = System.err;
				return;
			}
		}
	}
	///////////////////////////////////////////////////////////////////////
	//  Decide whether copyright should be displayed, and display
	//  if so.

	private void displayCopyright(final String[] args, final String noCopyrightSwitch) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].toLowerCase().equals(noCopyrightSwitch)) { return; }
		}
		out.println("MARS " + Globals.version + "  Copyright " + Globals.copyrightYears + " " + Globals.copyrightHolders
				+ "\n");
	}

	///////////////////////////////////////////////////////////////////////
	//  Display command line help text

	private void displayHelp() {
		final String[] segmentNames = MemoryDump.getSegmentNames();
		String segments = "";
		for (int i = 0; i < segmentNames.length; i++) {
			segments += segmentNames[i];
			if (i < segmentNames.length - 1) { segments += ", "; }
		}
		final ArrayList dumpFormats = new DumpFormatLoader().loadDumpFormats();
		String formats = "";
		for (int i = 0; i < dumpFormats.size(); i++) {
			formats += ((DumpFormat) dumpFormats.get(i)).getCommandDescriptor();
			if (i < dumpFormats.size() - 1) { formats += ", "; }
		}
		out.println("Usage:  Mars  [options] filename [additional filenames]");
		out.println("  Valid options (not case sensitive, separate by spaces) are:");
		out.println("      a  -- assemble only, do not simulate");
		out.println("  ae<n>  -- terminate MARS with integer exit code <n> if an assemble error occurs.");
		out.println("  ascii  -- display memory or register contents interpreted as ASCII codes.");
		out.println("      b  -- brief - do not display register/memory address along with contents");
		out.println("      d  -- display MARS debugging statements");
		out.println("     db  -- MIPS delayed branching is enabled");
		out.println("    dec  -- display memory or register contents in decimal.");
		out.println("   dump <segment> <format> <file> -- memory dump of specified memory segment");
		out.println("            in specified format to specified file.  Option may be repeated.");
		out.println("            Dump occurs at the end of simulation unless 'a' option is used.");
		out.println("            Segment and format are case-sensitive and possible values are:");
		out.println("            <segment> = " + segments);
		out.println("            <format> = " + formats);
		out.println("      h  -- display this help.  Use by itself with no filename.");
		out.println("    hex  -- display memory or register contents in hexadecimal (default)");
		out.println("     ic  -- display count of MIPS basic instructions 'executed'");
		out.println("     mc <config>  -- set memory configuration.  Argument <config> is");
		out.println("            case-sensitive and possible values are: Default for the default");
		out.println("            32-bit address space, CompactDataAtZero for a 32KB memory with");
		out.println("            data segment at address 0, or CompactTextAtZero for a 32KB");
		out.println("            memory with text segment at address 0.");
		out.println("     me  -- display MARS messages to standard err instead of standard out. ");
		out.println("            Can separate messages from program output using redirection");
		out.println("     nc  -- do not display copyright notice (for cleaner redirected/piped output).");
		out.println("     np  -- use of pseudo instructions and formats not permitted");
		out.println("      p  -- Project mode - assemble all files in the same directory as given file.");
		out.println("  se<n>  -- terminate MARS with integer exit code <n> if a simulation (run) error occurs.");
		out.println("     sm  -- start execution at statement with global label main, if defined");
		out.println("    smc  -- Self Modifying Code - Program can write and branch to either text or data segment");
		out.println("    <n>  -- where <n> is an integer maximum count of steps to simulate.");
		out.println("            If 0, negative or not specified, there is no maximum.");
		out.println(" $<reg>  -- where <reg> is number or name (e.g. 5, t3, f10) of register whose ");
		out.println("            content to display at end of run.  Option may be repeated.");
		out.println("<reg_name>  -- where <reg_name> is name (e.g. t3, f10) of register whose");
		out.println("            content to display at end of run.  Option may be repeated. ");
		out.println("            The $ is not required.");
		out.println("<m>-<n>  -- memory address range from <m> to <n> whose contents to");
		out.println("            display at end of run. <m> and <n> may be hex or decimal,");
		out.println("            must be on word boundary, <m> <= <n>.  Option may be repeated.");
		out.println("     pa  -- Program Arguments follow in a space-separated list.  This");
		out.println("            option must be placed AFTER ALL FILE NAMES, because everything");
		out.println("            that follows it is interpreted as a program argument to be");
		out.println("            made available to the MIPS program at runtime.");
		out.println("If more than one filename is listed, the first is assumed to be the main");
		out.println("unless the global statement label 'main' is defined in one of the files.");
		out.println("Exception handler not automatically assembled.  Add it to the file list.");
		out.println("Options used here do not affect MARS Settings menu values and vice versa.");
	}

}
