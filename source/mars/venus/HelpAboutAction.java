package mars.venus;

import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
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
 * Action for the Help -> About menu item
 */
public class HelpAboutAction extends GuiAction {

	/**
	 *
	 */
	private static final long serialVersionUID = 9170714355603950928L;

	public HelpAboutAction(final String name, final Icon icon, final String descrip, final Integer mnemonic,
			final KeyStroke accel, final VenusUI gui) {
		super(name, icon, descrip, mnemonic, accel, gui);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		JOptionPane.showMessageDialog(mainUI, "MARS " + Globals.version + "    Copyright " + Globals.copyrightYears
				+ "\n" + Globals.copyrightHolders + "\n" + "MARS is the Mips Assembler and Runtime Simulator.\n\n"
				+ "Mars image courtesy of NASA/JPL.\n" + "Toolbar and menu icons are from:\n"
				+ "  *  Tango Desktop Project (tango.freedesktop.org),\n"
				+ "  *  glyFX (www.glyfx.com) Common Toolbar Set,\n"
				+ "  *  KDE-Look (www.kde-look.org) crystalline-blue-0.1,\n"
				+ "  *  Icon-King (www.icon-king.com) Nuvola 1.0.\n"
				+ "Print feature adapted from HardcopyWriter class in David Flanagan's\n"
				+ "Java Examples in a Nutshell 3rd Edition, O'Reilly, ISBN 0-596-00620-9.", "About Mars",
				JOptionPane.INFORMATION_MESSAGE, new ImageIcon("images/RedMars50.gif"));
	}
}
