package mars.venus;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;

import mars.ErrorList;
import mars.ErrorMessage;
import mars.Globals;
import mars.MIPSprogram;
import mars.ProcessingException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
import mars.util.FilenameFinder;
import mars.util.SystemIO;

/*
 * Copyright (c) 2003-2010, Pete Sanderson and Kenneth Vollmar
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
 * Action class for the Run -> Assemble menu item (and toolbar icon)
 */
public class RunAssembleAction extends GuiAction {

	/**
	 *
	 */
	private static final long serialVersionUID = 1219029937418143070L;
	private static ArrayList MIPSprogramsToAssemble;
	private static boolean extendedAssemblerEnabled;
	private static boolean warningsAreErrors;
	// Threshold for adding filename to printed message of files being assembled.
	private static final int LINE_LENGTH_LIMIT = 60;

	public RunAssembleAction(final String name, final Icon icon, final String descrip, final Integer mnemonic,
			final KeyStroke accel, final VenusUI gui) {
		super(name, icon, descrip, mnemonic, accel, gui);
	}

	// These are both used by RunResetAction to re-assemble under identical conditions.
	static ArrayList getMIPSprogramsToAssemble() { return MIPSprogramsToAssemble; }

	static boolean getExtendedAssemblerEnabled() { return extendedAssemblerEnabled; }

	static boolean getWarningsAreErrors() { return warningsAreErrors; }

	@Override
	public void actionPerformed(final ActionEvent e) {
		final String name = getValue(Action.NAME).toString();
		mainUI.getMainPane().getEditPane();
		final ExecutePane executePane = mainUI.getMainPane().getExecutePane();
		final RegistersPane registersPane = mainUI.getRegistersPane();
		extendedAssemblerEnabled = Globals.getSettings().getExtendedAssemblerEnabled();
		warningsAreErrors = Globals.getSettings().getWarningsAreErrors();
		if (FileStatus.getFile() != null) {
			if (FileStatus.get() == FileStatus.EDITED) { mainUI.editor.save(); }
			try {
				Globals.program = new MIPSprogram();
				ArrayList filesToAssemble;
				if (Globals.getSettings().getAssembleAllEnabled()) {// setting calls for multiple file assembly
					filesToAssemble = FilenameFinder.getFilenameList(new File(FileStatus.getName()).getParent(),
							Globals.fileExtensions);
				} else {
					filesToAssemble = new ArrayList();
					filesToAssemble.add(FileStatus.getName());
				}
				String exceptionHandler = null;
				if (Globals.getSettings().getExceptionHandlerEnabled() && Globals.getSettings()
						.getExceptionHandler() != null && Globals.getSettings().getExceptionHandler().length() > 0) {
					exceptionHandler = Globals.getSettings().getExceptionHandler();
				}
				MIPSprogramsToAssemble = Globals.program.prepareFilesForAssembly(filesToAssemble, FileStatus.getFile()
						.getPath(), exceptionHandler);
				mainUI.messagesPane.postMarsMessage(buildFileNameList(name + ": assembling ", MIPSprogramsToAssemble));
				// added logic to receive any warnings and output them.... DPS 11/28/06
				final ErrorList warnings = Globals.program.assemble(MIPSprogramsToAssemble, extendedAssemblerEnabled,
						warningsAreErrors);
				if (warnings.warningsOccurred()) {
					mainUI.messagesPane.postMarsMessage(warnings.generateWarningReport());
				}
				mainUI.messagesPane.postMarsMessage(name + ": operation completed successfully.\n\n");
				FileStatus.setAssembled(true);
				FileStatus.set(FileStatus.RUNNABLE);
				RegisterFile.resetRegisters();
				Coprocessor1.resetRegisters();
				Coprocessor0.resetRegisters();
				executePane.getTextSegmentWindow().setupTable();
				executePane.getDataSegmentWindow().setupTable();
				executePane.getDataSegmentWindow().highlightCellForAddress(Memory.dataBaseAddress);
				executePane.getDataSegmentWindow().clearHighlighting();
				executePane.getLabelsWindow().setupTable();
				executePane.getTextSegmentWindow().setCodeHighlighting(true);
				executePane.getTextSegmentWindow().highlightStepAtPC();
				registersPane.getRegistersWindow().clearWindow();
				registersPane.getCoprocessor1Window().clearWindow();
				registersPane.getCoprocessor0Window().clearWindow();
				VenusUI.setReset(true);
				VenusUI.setStarted(false);
				mainUI.getMainPane().setSelectedComponent(executePane);

				// Aug. 24, 2005 Ken Vollmar
				SystemIO.resetFiles();  // Ensure that I/O "file descriptors" are initialized for a new program run

			} catch (final ProcessingException pe) {
				final String errorReport = pe.errors().generateErrorAndWarningReport();
				mainUI.messagesPane.postMarsMessage(errorReport);
				mainUI.messagesPane.postMarsMessage(name + ": operation completed with errors.\n\n");
				// Select editor line containing first error, and corresponding error message.
				final ArrayList errorMessages = pe.errors().getErrorMessages();
				for (int i = 0; i < errorMessages.size(); i++) {
					final ErrorMessage em = (ErrorMessage) errorMessages.get(i);
					// No line or position may mean File Not Found (e.g. exception file). Don't try to open. DPS 3-Oct-2010
					if (em.getLine() == 0 && em.getPosition() == 0) { continue; }
					if (!em.isWarning() || warningsAreErrors) {
						Globals.getGui().getMessagesPane().selectErrorMessage(em.getFilename(), em.getLine(), em
								.getPosition());
						// Bug workaround: Line selection does not work correctly for the JEditTextArea editor
						// when the file is opened then automatically assembled (assemble-on-open setting).
						// Automatic assemble happens in EditTabbedPane's openFile() method, by invoking
						// this method (actionPerformed) explicitly with null argument.  Thus e!=null test.
						// DPS 9-Aug-2010
						if (e != null) {
							Globals.getGui().getMessagesPane().selectEditorTextLine(em.getFilename(), em.getLine(), em
									.getPosition());
						}
						break;
					}
				}
				FileStatus.setAssembled(false);
				FileStatus.set(FileStatus.NOT_EDITED);
			}
		}
	}

	// Handy little utility for building comma-separated list of filenames
	// while not letting line length get out of hand.
	private String buildFileNameList(final String preamble, final ArrayList programList) {
		String result = preamble;
		int lineLength = result.length();
		for (int i = 0; i < programList.size(); i++) {
			final String filename = ((MIPSprogram) programList.get(i)).getFilename();
			result += filename + (i < programList.size() - 1 ? ", " : "");
			lineLength += filename.length();
			if (lineLength > LINE_LENGTH_LIMIT) {
				result += "\n";
				lineLength = 0;
			}
		}
		return result + (lineLength == 0 ? "" : "\n") + "\n";
	}
}
