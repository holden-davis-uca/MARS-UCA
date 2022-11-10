package mars.venus;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import mars.Globals;
import mars.ProcessingException;
import mars.mips.hardware.RegisterFile;
import mars.simulator.ProgramArgumentList;
import mars.simulator.Simulator;

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
 * Action for the Run -> Step menu item
 */
public class RunStepAction extends GuiAction {

	/**
	 *
	 */
	private static final long serialVersionUID = 3825313078523126501L;
	String name;
	ExecutePane executePane;

	public RunStepAction(final String name, final Icon icon, final String descrip, final Integer mnemonic,
			final KeyStroke accel, final VenusUI gui) {
		super(name, icon, descrip, mnemonic, accel, gui);
	}

	/**
	 * perform next simulated instruction step.
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {
		name = getValue(Action.NAME).toString();
		executePane = mainUI.getMainPane().getExecutePane();
		if (FileStatus.isAssembled()) {
			if (!VenusUI.getStarted()) {  // DPS 17-July-2008
				processProgramArgumentsIfAny();
			}
			VenusUI.setStarted(true);
			mainUI.messagesPane.setSelectedComponent(mainUI.messagesPane.runTab);
			executePane.getTextSegmentWindow().setCodeHighlighting(true);
			try {
				Globals.program.simulateStepAtPC(this);
			} catch (final ProcessingException ev) {}
		} else {
			// note: this should never occur since "Step" is only enabled after successful assembly.
			JOptionPane.showMessageDialog(mainUI, "The program must be assembled before it can be run.");
		}
	}

	// When step is completed, control returns here (from execution thread, indirectly)
	// to update the GUI.
	public void stepped(final boolean done, final int reason, final ProcessingException pe) {
		executePane.getRegistersWindow().updateRegisters();
		executePane.getCoprocessor1Window().updateRegisters();
		executePane.getCoprocessor0Window().updateRegisters();
		executePane.getDataSegmentWindow().updateValues();
		if (!done) {
			executePane.getTextSegmentWindow().highlightStepAtPC();
			FileStatus.set(FileStatus.RUNNABLE);
		}
		if (done) {
			RunGoAction.resetMaxSteps();
			executePane.getTextSegmentWindow().unhighlightAllSteps();
			FileStatus.set(FileStatus.TERMINATED);
		}
		if (done && pe == null) {
			mainUI.getMessagesPane().postMarsMessage("\n" + name + ": execution "
					+ (reason == Simulator.CLIFF_TERMINATION ? "terminated due to null instruction."
							: "completed successfully.") + "\n\n");
			mainUI.getMessagesPane().postRunMessage("\n-- program is finished running "
					+ (reason == Simulator.CLIFF_TERMINATION ? "(dropped off bottom)" : "") + " --\n\n");
			mainUI.getMessagesPane().selectRunMessageTab();
		}
		if (pe != null) {
			RunGoAction.resetMaxSteps();
			mainUI.getMessagesPane().postMarsMessage(pe.errors().generateErrorReport());
			mainUI.getMessagesPane().postMarsMessage("\n" + name + ": execution terminated with errors.\n\n");
			mainUI.getRegistersPane().setSelectedComponent(executePane.getCoprocessor0Window());
			FileStatus.set(FileStatus.TERMINATED); // should be redundant.
			executePane.getTextSegmentWindow().setCodeHighlighting(true);
			executePane.getTextSegmentWindow().unhighlightAllSteps();
			executePane.getTextSegmentWindow().highlightStepAtAddress(RegisterFile.getProgramCounter() - 4);
		}
		VenusUI.setReset(false);
	}

	////////////////////////////////////////////////////////////////////////////////////
	// Method to store any program arguments into MIPS memory and registers before
	// execution begins. Arguments go into the gap between $sp and kernel memory.
	// Argument pointers and count go into runtime stack and $sp is adjusted accordingly.
	// $a0 gets argument count (argc), $a1 gets stack address of first arg pointer (argv).
	private void processProgramArgumentsIfAny() {
		final String programArguments = executePane.getTextSegmentWindow().getProgramArguments();
		if (programArguments == null || programArguments.length() == 0 || !Globals.getSettings()
				.getProgramArguments()) {
			return;
		}
		new ProgramArgumentList(programArguments).storeProgramArguments();
	}
}
