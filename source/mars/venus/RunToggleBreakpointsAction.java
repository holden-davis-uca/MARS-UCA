package mars.venus;

import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.KeyStroke;

import mars.Globals;

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
 * Action class for the Run menu item to clear execution breakpoints that have
 * been set. It is a listener and is notified whenever a breakpoint is added or
 * removed, thus will set its enabled status true or false depending on whether
 * breakpoints remain after that action.
 */
public class RunToggleBreakpointsAction extends GuiAction {

	/**
	 *
	 */
	private static final long serialVersionUID = 2366625948398238933L;

	/**
	 * Create the object and register with text segment window as a listener on its
	 * table model. The table model has not been created yet, so text segment window
	 * will hang onto this registration info and transfer it to the table model upon
	 * creation (which happens with each successful assembly).
	 */
	public RunToggleBreakpointsAction(final String name, final Icon icon, final String descrip, final Integer mnemonic,
			final KeyStroke accel, final VenusUI gui) {
		super(name, icon, descrip, mnemonic, accel, gui);
	}

	/**
	 * When this option is selected, tell text segment window to clear breakpoints
	 * in its table model.
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {
		Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow().toggleBreakpoints();
	}

}
