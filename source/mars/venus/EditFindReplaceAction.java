package mars.venus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import mars.Globals;
import mars.venus.editors.MARSTextEditingArea;

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
 * Action for the Edit -> Find/Replace menu item
 */
public class EditFindReplaceAction extends GuiAction {

	/**
	 *
	 */
	private static final long serialVersionUID = 3192628187646002444L;
	private static String searchString = "";
	private static boolean caseSensitivity = true;
	private static final String DIALOG_TITLE = "Find and Replace";
	JDialog findReplaceDialog;

	public EditFindReplaceAction(final String name, final Icon icon, final String descrip, final Integer mnemonic,
			final KeyStroke accel, final VenusUI gui) {
		super(name, icon, descrip, mnemonic, accel, gui);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		findReplaceDialog = new FindReplaceDialog(Globals.getGui(), DIALOG_TITLE, false);
		findReplaceDialog.setVisible(true);
	}

	//////////////////////////////////////////////////////////////////////////////
	//
	//   Private class to do all the work!
	//
	private class FindReplaceDialog extends JDialog {

		/**
		 *
		 */
		private static final long serialVersionUID = 8265100526072978827L;
		JButton findButton, replaceButton, replaceAllButton, closeButton;
		JTextField findInputField, replaceInputField;
		JCheckBox caseSensitiveCheckBox;
		JRadioButton linearFromStart, circularFromCursor;
		private JLabel resultsLabel;

		public static final String FIND_TOOL_TIP_TEXT = "Find next occurrence of given text; wraps around at end";
		public static final String REPLACE_TOOL_TIP_TEXT = "Replace current occurrence of text then find next";
		public static final String REPLACE_ALL_TOOL_TIP_TEXT = "Replace all occurrences of text";
		public static final String CLOSE_TOOL_TIP_TEXT = "Close the dialog";
		public static final String RESULTS_TOOL_TIP_TEXT = "Outcome of latest operation (button click)";

		public static final String RESULTS_TEXT_FOUND = "Text found";
		public static final String RESULTS_TEXT_NOT_FOUND = "Text not found";
		public static final String RESULTS_TEXT_REPLACED = "Text replaced and found next";
		public static final String RESULTS_TEXT_REPLACED_LAST = "Text replaced; last occurrence";
		public static final String RESULTS_TEXT_REPLACED_ALL = "Replaced";
		public static final String RESULTS_NO_TEXT_TO_FIND = "No text to find";

		public FindReplaceDialog(final Frame owner, final String title, final boolean modality) {
			super(owner, title, modality);
			setContentPane(buildDialogPanel());
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosing(final WindowEvent we) {
					performClose();
				}
			});
			pack();
			setLocationRelativeTo(owner);
		}

		// Constructs the dialog's main panel.
		private JPanel buildDialogPanel() {
			final JPanel dialogPanel = new JPanel(new BorderLayout());
			dialogPanel.setBorder(new javax.swing.border.EmptyBorder(10, 10, 10, 10));
			dialogPanel.add(buildInputPanel(), BorderLayout.NORTH);
			dialogPanel.add(buildOptionsPanel());
			dialogPanel.add(buildControlPanel(), BorderLayout.SOUTH);
			return dialogPanel;
		}

		// Top part of the dialog, to contain the two input text fields.
		private Component buildInputPanel() {
			findInputField = new JTextField(30);
			if (searchString.length() > 0) {
				findInputField.setText(searchString);
				findInputField.selectAll();
			}
			replaceInputField = new JTextField(30);
			final JPanel inputPanel = new JPanel();
			final JPanel labelsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
			final JPanel fieldsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
			labelsPanel.add(new JLabel("Find what:"));
			labelsPanel.add(new JLabel("Replace with:"));
			fieldsPanel.add(findInputField);
			fieldsPanel.add(replaceInputField);

			final Box columns = Box.createHorizontalBox();
			columns.add(labelsPanel);
			columns.add(Box.createHorizontalStrut(6));
			columns.add(fieldsPanel);
			inputPanel.add(columns);
			return inputPanel;
		}

		// Center part of the dialog, which contains the check box
		// for case sensitivity along with a label to display the
		// outcome of each operation.
		private Component buildOptionsPanel() {
			final Box optionsPanel = Box.createHorizontalBox();
			caseSensitiveCheckBox = new JCheckBox("Case Sensitive", caseSensitivity);
			final JPanel casePanel = new JPanel(new GridLayout(2, 1));
			casePanel.add(caseSensitiveCheckBox);
			casePanel.setMaximumSize(casePanel.getPreferredSize());
			optionsPanel.add(casePanel);
			optionsPanel.add(Box.createHorizontalStrut(5));
			final JPanel resultsPanel = new JPanel(new GridLayout(1, 1));
			resultsPanel.setBorder(BorderFactory.createTitledBorder("Outcome"));
			resultsLabel = new JLabel("");
			resultsLabel.setForeground(Color.RED);
			resultsLabel.setToolTipText(RESULTS_TOOL_TIP_TEXT);
			resultsPanel.add(resultsLabel);
			optionsPanel.add(resultsPanel);
			return optionsPanel;
		}

		// Row of control buttons to be placed along the button of the dialog
		private Component buildControlPanel() {
			final Box controlPanel = Box.createHorizontalBox();
			controlPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
			findButton = new JButton("Find");
			findButton.setToolTipText(FIND_TOOL_TIP_TEXT);
			findButton.addActionListener(e -> performFind());
			replaceButton = new JButton("Replace then Find");
			replaceButton.setToolTipText(REPLACE_TOOL_TIP_TEXT);
			replaceButton.addActionListener(e -> performReplace());
			replaceAllButton = new JButton("Replace all");
			replaceAllButton.setToolTipText(REPLACE_ALL_TOOL_TIP_TEXT);
			replaceAllButton.addActionListener(e -> performReplaceAll());
			closeButton = new JButton("Close");
			closeButton.setToolTipText(CLOSE_TOOL_TIP_TEXT);
			closeButton.addActionListener(e -> performClose());
			controlPanel.add(Box.createHorizontalGlue());
			controlPanel.add(findButton);
			controlPanel.add(Box.createHorizontalGlue());
			controlPanel.add(replaceButton);
			controlPanel.add(Box.createHorizontalGlue());
			controlPanel.add(replaceAllButton);
			controlPanel.add(Box.createHorizontalGlue());
			controlPanel.add(closeButton);
			controlPanel.add(Box.createHorizontalGlue());
			return controlPanel;
		}

		////////////////////////////////////////////////////////////////////////
		//
		//  Private methods to carry out the button actions

		//  Performs a find.  The operation starts at the current cursor position
		//  which is not known to this object but is maintained by the EditPane
		//  object.  The operation will wrap around when it reaches the end of the
		//  document.  If found, the matching text is selected.
		private void performFind() {
			resultsLabel.setText("");
			if (findInputField.getText().length() > 0) {
				final EditPane editPane = mainUI.getMainPane().getEditPane();
				// Being cautious. Should not be null because find/replace tool button disabled if no file open
				if (editPane != null) {
					searchString = findInputField.getText();
					final int posn = editPane.doFindText(searchString, caseSensitiveCheckBox.isSelected());
					if (posn == MARSTextEditingArea.TEXT_NOT_FOUND) {
						resultsLabel.setText(findButton.getText() + ": " + RESULTS_TEXT_NOT_FOUND);
					} else {
						resultsLabel.setText(findButton.getText() + ": " + RESULTS_TEXT_FOUND);
					}
				}
			} else {
				resultsLabel.setText(findButton.getText() + ": " + RESULTS_NO_TEXT_TO_FIND);
			}
		}

		// Performs a replace-and-find.  If the matched text is current selected with cursor at
		// its end, the replace happens immediately followed by a find for the next occurrence.
		// Otherwise, it performs a find.  This will select the matching text so the next press
		// of Replace will do the replace.  This is apparently common behavior for replace
		// buttons of different apps I've checked.
		private void performReplace() {
			resultsLabel.setText("");
			if (findInputField.getText().length() > 0) {
				final EditPane editPane = mainUI.getMainPane().getEditPane();
				// Being cautious. Should not be null b/c find/replace tool button disabled if no file open
				if (editPane != null) {
					searchString = findInputField.getText();
					final int posn = editPane.doReplace(searchString, replaceInputField.getText(), caseSensitiveCheckBox
							.isSelected());
					String result = replaceButton.getText() + ": ";
					switch (posn) {

					case MARSTextEditingArea.TEXT_NOT_FOUND:
						result += RESULTS_TEXT_NOT_FOUND;
						break;
					case MARSTextEditingArea.TEXT_FOUND:
						result += RESULTS_TEXT_FOUND;
						break;
					case MARSTextEditingArea.TEXT_REPLACED_NOT_FOUND_NEXT:
						result += RESULTS_TEXT_REPLACED_LAST;
						break;
					case MARSTextEditingArea.TEXT_REPLACED_FOUND_NEXT:
						result += RESULTS_TEXT_REPLACED;
						break;
					}
					resultsLabel.setText(result);
				}
			} else {
				resultsLabel.setText(replaceButton.getText() + ": " + RESULTS_NO_TEXT_TO_FIND);
			}

		}

		// Performs a replace-all.  Makes one pass through the document starting at
		// position 0.
		private void performReplaceAll() {
			resultsLabel.setText("");
			if (findInputField.getText().length() > 0) {
				final EditPane editPane = mainUI.getMainPane().getEditPane();
				// Being cautious. Should not be null b/c find/replace tool button disabled if no file open
				if (editPane != null) {
					searchString = findInputField.getText();
					final int replaceCount = editPane.doReplaceAll(searchString, replaceInputField.getText(),
							caseSensitiveCheckBox.isSelected());
					if (replaceCount == 0) {
						resultsLabel.setText(replaceAllButton.getText() + ": " + RESULTS_TEXT_NOT_FOUND);
					} else {
						resultsLabel.setText(replaceAllButton.getText() + ": " + RESULTS_TEXT_REPLACED_ALL + " "
								+ replaceCount + " occurrence" + (replaceCount == 1 ? "" : "s"));
					}
				}
			} else {
				resultsLabel.setText(replaceAllButton.getText() + ": " + RESULTS_NO_TEXT_TO_FIND);
			}
		}

		// Performs the close operation.  Records the current state of the case-sensitivity
		// checkbox into a static variable so it will be remembered across invocations within
		// the session.  This also happens with the contents of the "find" text field.
		private void performClose() {
			caseSensitivity = caseSensitiveCheckBox.isSelected();
			setVisible(false);
			dispose();
		}
		//
		////////////////////////////////////////////////////////////////////////////////
	}

}
