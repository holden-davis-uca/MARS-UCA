/*
 * Copyright (c) 2008, Felipe Lessa
 *
 * Developed by Felipe Lessa (felipe.lessa@gmail.com)
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
package mars.tools;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Observable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import mars.ProgramStatement;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.mips.instructions.BasicInstruction;
import mars.mips.instructions.BasicInstructionFormat;

/**
 * Instruction counter tool. Can be used to know how many instructions were
 * executed to complete a given program. Code slightly based on
 * MemoryReferenceVisualization.
 *
 * @author Felipe Lessa <felipe.lessa@gmail.com>
 */
//@SuppressWarnings("serial")
public class InstructionCounter extends AbstractMarsToolAndApplication {

	/**
	 *
	 */
	private static final long serialVersionUID = 8585913723201262800L;
	private static String name = "Instruction Counter";
	private static String version = "Version 1.0 (Felipe Lessa)";
	private static String heading = "Counting the number of instructions executed";

	/**
	 * Number of instructions executed until now.
	 */
	protected int counter = 0;
	private JTextField counterField;

	/**
	 * Number of instructions of type R.
	 */
	protected int counterR = 0;
	private JTextField counterRField;
	private JProgressBar progressbarR;

	/**
	 * Number of instructions of type I.
	 */
	protected int counterI = 0;
	private JTextField counterIField;
	private JProgressBar progressbarI;

	/**
	 * Number of instructions of type J.
	 */
	protected int counterJ = 0;
	private JTextField counterJField;
	private JProgressBar progressbarJ;

	/**
	 * The last address we saw. We ignore it because the only way for a program to
	 * execute twice the same instruction is to enter an infinite loop, which is not
	 * insteresting in the POV of counting instructions.
	 */
	protected int lastAddress = -1;

	/**
	 * Simple constructor, likely used to run a stand-alone memory reference
	 * visualizer.
	 *
	 * @param title   String containing title for title bar
	 * @param heading String containing text for heading shown in upper part of
	 *                window.
	 */
	public InstructionCounter(final String title, final String heading) {
		super(title, heading);
	}

	/**
	 * Simple construction, likely used by the MARS Tools menu mechanism.
	 */
	public InstructionCounter() {
		super(name + ", " + version, heading);
	}

	//	@Override
	@Override
	public String getName() { return name; }

	//	@Override
	@Override
	protected JComponent buildMainDisplayArea() {
		// Create everything
		final JPanel panel = new JPanel(new GridBagLayout());

		counterField = new JTextField("0", 10);
		counterField.setEditable(false);

		counterRField = new JTextField("0", 10);
		counterRField.setEditable(false);
		progressbarR = new JProgressBar(SwingConstants.HORIZONTAL);
		progressbarR.setStringPainted(true);

		counterIField = new JTextField("0", 10);
		counterIField.setEditable(false);
		progressbarI = new JProgressBar(SwingConstants.HORIZONTAL);
		progressbarI.setStringPainted(true);

		counterJField = new JTextField("0", 10);
		counterJField.setEditable(false);
		progressbarJ = new JProgressBar(SwingConstants.HORIZONTAL);
		progressbarJ.setStringPainted(true);

		// Add them to the panel

		// Fields
		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.LINE_START;
		c.gridheight = c.gridwidth = 1;
		c.gridx = 3;
		c.gridy = 1;
		c.insets = new Insets(0, 0, 17, 0);
		panel.add(counterField, c);

		c.insets = new Insets(0, 0, 0, 0);
		c.gridy++;
		panel.add(counterRField, c);

		c.gridy++;
		panel.add(counterIField, c);

		c.gridy++;
		panel.add(counterJField, c);

		// Labels
		c.anchor = GridBagConstraints.LINE_END;
		c.gridx = 1;
		c.gridwidth = 2;
		c.gridy = 1;
		c.insets = new Insets(0, 0, 17, 0);
		panel.add(new JLabel("Instructions so far: "), c);

		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 2;
		c.gridwidth = 1;
		c.gridy++;
		panel.add(new JLabel("R-type: "), c);

		c.gridy++;
		panel.add(new JLabel("I-type: "), c);

		c.gridy++;
		panel.add(new JLabel("J-type: "), c);

		// Progress bars
		c.insets = new Insets(3, 3, 3, 3);
		c.gridx = 4;
		c.gridy = 2;
		panel.add(progressbarR, c);

		c.gridy++;
		panel.add(progressbarI, c);

		c.gridy++;
		panel.add(progressbarJ, c);

		return panel;
	}

	//	@Override
	@Override
	protected void addAsObserver() {
		addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
	}

	//	@Override
	@Override
	protected void processMIPSUpdate(final Observable resource, final AccessNotice notice) {
		if (!notice.accessIsFromMIPS()) { return; }
		if (notice.getAccessType() != AccessNotice.READ) { return; }
		final MemoryAccessNotice m = (MemoryAccessNotice) notice;
		final int a = m.getAddress();
		if (a == lastAddress) { return; }
		lastAddress = a;
		counter++;
		try {
			final ProgramStatement stmt = Memory.getInstance().getStatement(a);
			final BasicInstruction instr = (BasicInstruction) stmt.getInstruction();
			final BasicInstructionFormat format = instr.getInstructionFormat();
			if (format == BasicInstructionFormat.R_FORMAT) {
				counterR++;
			} else if (format == BasicInstructionFormat.I_FORMAT || format == BasicInstructionFormat.I_BRANCH_FORMAT) {
				counterI++;
			} else if (format == BasicInstructionFormat.J_FORMAT) { counterJ++; }
		} catch (final AddressErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		updateDisplay();
	}

	//	@Override
	@Override
	protected void initializePreGUI() {
		counter = counterR = counterI = counterJ = 0;
		lastAddress = -1;
	}

	// @Override
	@Override
	protected void reset() {
		counter = counterR = counterI = counterJ = 0;
		lastAddress = -1;
		updateDisplay();
	}

	//	@Override
	@Override
	protected void updateDisplay() {
		counterField.setText(String.valueOf(counter));

		counterRField.setText(String.valueOf(counterR));
		progressbarR.setMaximum(counter);
		progressbarR.setValue(counterR);

		counterIField.setText(String.valueOf(counterI));
		progressbarI.setMaximum(counter);
		progressbarI.setValue(counterI);

		counterJField.setText(String.valueOf(counterJ));
		progressbarJ.setMaximum(counter);
		progressbarJ.setValue(counterJ);

		if (counter == 0) {
			progressbarR.setString("0%");
			progressbarI.setString("0%");
			progressbarJ.setString("0%");
		} else {
			progressbarR.setString(counterR * 100 / counter + "%");
			progressbarI.setString(counterI * 100 / counter + "%");
			progressbarJ.setString(counterJ * 100 / counter + "%");
		}
	}
}
