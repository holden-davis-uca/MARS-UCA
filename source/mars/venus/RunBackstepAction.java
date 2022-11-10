package mars.venus;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import mars.Globals;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;

/*
 * Copyright (c) 2003-2009, Pete Sanderson and Kenneth Vollmar
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
 * Action for the Run -> Backstep menu item
 */
public class RunBackstepAction extends GuiAction {

	/**
	 *
	 */
	private static final long serialVersionUID = 4814940944425610191L;
	String name;
	ExecutePane executePane;

	public RunBackstepAction(final String name, final Icon icon, final String descrip, final Integer mnemonic,
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
		if (!FileStatus.isAssembled()) {
			// note: this should never occur since backstepping is only enabled after successful assembly.
			JOptionPane.showMessageDialog(mainUI, "The program must be assembled before it can be run.");
			return;
		}
		VenusUI.setStarted(true);
		mainUI.messagesPane.setSelectedComponent(mainUI.messagesPane.runTab);
		executePane.getTextSegmentWindow().setCodeHighlighting(true);

		if (Globals.getSettings().getBackSteppingEnabled()) {
			final boolean inDelaySlot = Globals.program.getBackStepper().inDelaySlot(); // Added 25 June 2007
			Memory.getInstance().addObserver(executePane.getDataSegmentWindow());
			RegisterFile.addRegistersObserver(executePane.getRegistersWindow());
			Coprocessor0.addRegistersObserver(executePane.getCoprocessor0Window());
			Coprocessor1.addRegistersObserver(executePane.getCoprocessor1Window());
			Globals.program.getBackStepper().backStep();
			Memory.getInstance().deleteObserver(executePane.getDataSegmentWindow());
			RegisterFile.deleteRegistersObserver(executePane.getRegistersWindow());
			executePane.getRegistersWindow().updateRegisters();
			executePane.getCoprocessor1Window().updateRegisters();
			executePane.getCoprocessor0Window().updateRegisters();
			executePane.getDataSegmentWindow().updateValues();
			executePane.getTextSegmentWindow().highlightStepAtPC(inDelaySlot); // Argument aded 25 June 2007
			FileStatus.set(FileStatus.RUNNABLE);
			// if we've backed all the way, disable the button
			//    if (Globals.program.getBackStepper().empty()) {
			//     ((AbstractAction)((AbstractButton)e.getSource()).getAction()).setEnabled(false);
			//}
			/*
			if (pe !=null) {
			RunGoAction.resetMaxSteps();
			mainUI.getMessagesPane().postMarsMessage(
			                    pe.errors().generateErrorReport());
			mainUI.getMessagesPane().postMarsMessage(
			                    "\n"+name+": execution terminated with errors.\n\n");
			mainUI.getRegistersPane().setSelectedComponent(executePane.getCoprocessor0Window());
			FileStatus.set(FileStatus.TERMINATED); // should be redundant.
								executePane.getTextSegmentWindow().setCodeHighlighting(true);
				executePane.getTextSegmentWindow().unhighlightAllSteps();
			executePane.getTextSegmentWindow().highlightStepAtAddress(RegisterFile.getProgramCounter()-4);
			}
			*/
			VenusUI.setReset(false);
		}
	}
}
