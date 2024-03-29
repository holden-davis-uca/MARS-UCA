package mars.venus;

import mars.mips.hardware.*;
import mars.util.*;
import mars.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.PropertyChangeListener;
import javax.swing.filechooser.FileFilter;

/*
Copyright (c) 2003-2010,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
*/

/**
 * Tabbed pane for the editor. Each of its tabs represents an open file.
 * 
 * @author Sanderson
 **/

public class EditTabbedPane extends JTabbedPane implements MouseListener {
	EditPane editTab;
	final MainPane mainPane;
	JPanel fileTab;

	private final VenusUI mainUI;
	private final Editor editor;
	private final FileOpener fileOpener;

	/**
	 * Constructor for the EditTabbedPane class.
	 **/

	public EditTabbedPane(VenusUI appFrame, Editor editor, MainPane mainPane) {
		super();
		addMouseListener(this);
		this.mainUI = appFrame;
		this.editor = editor;
		this.fileOpener = new FileOpener(editor);
		this.mainPane = mainPane;
		this.editor.setEditTabbedPane(this);
		this.addChangeListener(e -> {
			EditPane editPane = (EditPane) getSelectedComponent();
			if (editPane != null) {
				// New IF statement to permit free traversal of edit panes w/o invalidating
				// assembly if assemble-all is selected. DPS 9-Aug-2011
				if (Globals.getSettings().getBooleanSetting(Settings.ASSEMBLE_ALL_ENABLED)) {
					EditTabbedPane.this.updateTitles(editPane);
				} else {
					EditTabbedPane.this.updateTitlesAndMenuState(editPane);
					EditTabbedPane.this.mainPane.getExecutePane().clearPane();
				}
				editPane.tellEditingComponentToRequestFocusInWindow();
			}
		});
	}

	public void mouseClicked(MouseEvent e) {
		int tabNumber=getUI().tabForCoordinate(this, e.getX(), e.getY());
		if (tabNumber < 0) return;
		Rectangle rect=((CloseTabIcon)getIconAt(tabNumber)).getBounds();
		if (rect.contains(e.getX(), e.getY())) {
			//Need to call appropriate methods to remove the tab
			this.closeCurrentFile();
		}
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}

	public void addTab(String title, Component component) {
		this.addTab(title, component, null);
	}

	public void addTab(String title, Component component, Icon extraIcon) {
		super.addTab(title, new CloseTabIcon(extraIcon), component);
	}
	static class CloseTabIcon implements Icon {
		private int x_pos;
		private int y_pos;
		private final int width;
		private final int height;
		private final Icon fileIcon;

		public CloseTabIcon(Icon fileIcon) {
			this.fileIcon=fileIcon;
			width=16;
			height=16;
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			this.x_pos=x;
			this.y_pos=y;

			Color col=g.getColor();

			g.setColor(Color.black);
			int y_p=y+2;
			g.drawLine(x+1, y_p, x+12, y_p);
			g.drawLine(x+1, y_p+13, x+12, y_p+13);
			g.drawLine(x, y_p+1, x, y_p+12);
			g.drawLine(x+13, y_p+1, x+13, y_p+12);
			g.drawLine(x+3, y_p+3, x+10, y_p+10);
			g.drawLine(x+3, y_p+4, x+9, y_p+10);
			g.drawLine(x+4, y_p+3, x+10, y_p+9);
			g.drawLine(x+10, y_p+3, x+3, y_p+10);
			g.drawLine(x+10, y_p+4, x+4, y_p+10);
			g.drawLine(x+9, y_p+3, x+3, y_p+9);
			g.setColor(col);
			if (fileIcon != null) {
				fileIcon.paintIcon(c, g, x+width, y_p);
			}
		}

		public int getIconWidth() {
			return width + (fileIcon != null? fileIcon.getIconWidth() : 0);
		}

		public int getIconHeight() {
			return height;
		}

		public Rectangle getBounds() {
			return new Rectangle(x_pos, y_pos, width, height);
		}
	}

	/**
	 * The current EditPane representing a file. Returns null if no files open.
	 *
	 * @return the current editor pane
	 */
	public EditPane getCurrentEditTab() {
		return (EditPane) this.getSelectedComponent();
	}

	/**
	 * Select the specified EditPane to be the current tab.
	 *
	 * @param editPane The EditPane tab to become current.
	 */
	public void setCurrentEditTab(EditPane editPane) {
		this.setSelectedComponent(editPane);
	}

	/**
	 * If the given file is open in the tabbed pane, make it the current tab. If not
	 * opened, open it in a new tab and make it the current tab. If file is unable
	 * to be opened, leave current tab as is.
	 * 
	 * @param file File object for the desired file.
	 * @return EditPane for the specified file, or null if file is unable to be
	 *         opened in an EditPane
	 */
	public EditPane getCurrentEditTabForFile(File file) {
		EditPane result = null;
		EditPane tab = getEditPaneForFile(file.getPath());
		if (tab != null) {
			if (tab != getCurrentEditTab()) {
				setCurrentEditTab(tab);
			}
			return tab;
		}
		// If no return yet, then file is not open. Try to open it.
		if (openFile(file)) {
			result = getCurrentEditTab();
		}
		return result;
	}

	/**
	 * Carries out all necessary operations to implement the New operation from the
	 * File menu.
	 */
	public void newFile() {
		EditPane editPane = new EditPane(this.mainUI);
		editPane.setSourceCode("", true);
		editPane.setShowLineNumbersEnabled(true);
		editPane.setFileStatus(FileStatus.NEW_NOT_EDITED);
		String name = editor.getNextDefaultFilename();
		editPane.setPathname(name);
		this.addTab(name, editPane);

		FileStatus.reset();
		FileStatus.setName(name);
		FileStatus.set(FileStatus.NEW_NOT_EDITED);

		RegisterFile.resetRegisters();
		VenusUI.setReset(true);
		mainPane.getExecutePane().clearPane();
		mainPane.setSelectedComponent(this);
		editPane.displayCaretPosition(new Point(1, 1));
		this.setSelectedComponent(editPane);
		updateTitlesAndMenuState(editPane);
		editPane.tellEditingComponentToRequestFocusInWindow();
	}

	/**
	 * Carries out all necessary operations to implement the Open operation from the
	 * File menu. This begins with an Open File dialog.
	 * 
	 * @return true if file was opened, false otherwise.
	 */
	public boolean openFile() {
		return fileOpener.openFile();
	}

	/**
	 * Carries out all necessary operations to open the specified file in the
	 * editor.
	 * 
	 * @return true if file was opened, false otherwise.
	 */
	public boolean openFile(File file) {
		return fileOpener.openFile(file);
	}

	/**
	 * Carries out all necessary operations to implement the Close operation from
	 * the File menu. May return false, for instance when file has unsaved changes
	 * and user selects Cancel from the warning dialog.
	 * 
	 * @return true if file was closed, false otherwise.
	 */
	public boolean closeCurrentFile() {
		EditPane editPane = getCurrentEditTab();
		if (editPane != null) {
			if (editsSavedOrAbandoned()) {
				this.remove(editPane);
				mainPane.getExecutePane().clearPane();
				mainPane.setSelectedComponent(this);
				this.editor.newUsageCount--;
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * Carries out all necessary operations to implement the Close All operation
	 * from the File menu.
	 * 
	 * @return true if files closed, false otherwise.
	 */
	public boolean closeAllFiles() {
		boolean result = true;
		boolean unsavedChanges = false;
		int tabCount = getTabCount();
		if (tabCount > 0) {
			mainPane.getExecutePane().clearPane();
			mainPane.setSelectedComponent(this);
			EditPane[] tabs = new EditPane[tabCount];
			for (int i = 0; i < tabCount; i++) {
				tabs[i] = (EditPane) getComponentAt(i);
				if (tabs[i].hasUnsavedEdits()) {
					unsavedChanges = true;
				}
			}
			if (unsavedChanges) {
				switch (confirm("one or more files")) {
				case JOptionPane.YES_OPTION:
					boolean removedAll = true;
					for (int i = 0; i < tabCount; i++) {
						if (tabs[i].hasUnsavedEdits()) {
							setSelectedComponent(tabs[i]);
							boolean saved = saveCurrentFile();
							if (saved) {
								this.remove(tabs[i]);
							} else {
								removedAll = false;
							}
						} else {
							this.remove(tabs[i]);
						}
					}
					return removedAll;
				case JOptionPane.NO_OPTION:
					for (int i = 0; i < tabCount; i++) {
						this.remove(tabs[i]);
					}
					return true;
				case JOptionPane.CANCEL_OPTION:
					return false;
				default: // should never occur
					return false;
				}
			} else {
				for (int i = 0; i < tabCount; i++) {
					this.remove(tabs[i]);
				}
			}
		}
		return result;
	}

	/**
	 * Saves file under existing name. If no name, will invoke Save As.
	 * 
	 * @return true if the file was actually saved.
	 */
	public boolean saveCurrentFile() {
		EditPane editPane = getCurrentEditTab();
		if (saveFile(editPane)) {
			FileStatus.setSaved(true);
			FileStatus.setEdited(false);
			FileStatus.set(FileStatus.NOT_EDITED);
			editPane.setFileStatus(FileStatus.NOT_EDITED);
			updateTitlesAndMenuState(editPane);
			return true;
		}
		return false;
	}

	// Save file associatd with specified edit pane.
	// Returns true if save operation worked, else false.
	private boolean saveFile(EditPane editPane) {
		if (editPane != null) {
			if (editPane.isNew()) {
				File theFile = saveAsFile(editPane);
				if (theFile != null) {
					editPane.setPathname(theFile.getPath());
				}
				return (theFile != null);
			}
			File theFile = new File(editPane.getPathname());
			try {
				BufferedWriter outFileStream = new BufferedWriter(new FileWriter(theFile));
				outFileStream.write(editPane.getSource(), 0, editPane.getSource().length());
				outFileStream.close();
			} catch (java.io.IOException c) {
				JOptionPane.showMessageDialog(null, "Save operation could not be completed due to an error:\n" + c,
						"Save Operation Failed", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Pops up a dialog box to do "Save As" operation. If necessary an additional
	 * overwrite dialog is performed.
	 * 
	 * @return true if the file was actually saved.
	 */
	public boolean saveAsCurrentFile() {
		EditPane editPane = getCurrentEditTab();
		File theFile = saveAsFile(editPane);
		if (theFile != null) {
			FileStatus.setFile(theFile);
			FileStatus.setName(theFile.getPath());
			FileStatus.setSaved(true);
			FileStatus.setEdited(false);
			FileStatus.set(FileStatus.NOT_EDITED);
			editor.setCurrentSaveDirectory(theFile.getParent());
			editPane.setPathname(theFile.getPath());
			editPane.setFileStatus(FileStatus.NOT_EDITED);
			updateTitlesAndMenuState(editPane);
			return true;
		}
		return false;
	}

	// perform Save As for selected edit pane. If the save is performed,
	// return its File object. Otherwise return null.
	private File saveAsFile(EditPane editPane) {
		File theFile = null;
		if (editPane != null) {
			FileDialog saver = null;
			// Set Save As dialog directory in a logical way. If file in
			// edit pane had been previously saved, default to its directory.
			// If a new file (mipsN.asm), default to current save directory.
			// DPS 13-July-2011
			if (editPane.isNew()) {
				saver = new FileDialog(mainUI, "Save as", FileDialog.SAVE);
				saver.setVisible(true);
			} else {
				File f = new File(editPane.getPathname());
				saver = new FileDialog(mainUI, "Save as", FileDialog.SAVE);
				saver.setVisible(true);
			}
			// end of 13-July-2011 code.

			theFile = new File(saver.getDirectory() + saver.getFile());
			// Either file with selected name does not exist or user wants to
			// overwrite it, so go for it!
			try {
				BufferedWriter outFileStream = new BufferedWriter(new FileWriter(theFile));
				outFileStream.write(editPane.getSource(), 0, editPane.getSource().length());
				outFileStream.close();
			} catch (java.io.IOException c) {
				JOptionPane.showMessageDialog(null, "Save As operation could not be completed due to an error:\n" + c,
						"Save As Operation Failed", JOptionPane.ERROR_MESSAGE);
				return null;
			}
		}
		return theFile;
	}

	/**
	 * Saves all files currently open in the editor.
	 * 
	 * @return true if operation succeeded otherwise false.
	 */
	public boolean saveAllFiles() {
		boolean result = false;
		int tabCount = getTabCount();
		if (tabCount > 0) {

			result = true;
			EditPane[] tabs = new EditPane[tabCount];
			EditPane savedPane = getCurrentEditTab();
			for (int i = 0; i < tabCount; i++) {
				tabs[i] = (EditPane) getComponentAt(i);
				if (tabs[i].hasUnsavedEdits()) {
					setCurrentEditTab(tabs[i]);
					if (saveFile(tabs[i])) {
						tabs[i].setFileStatus(FileStatus.NOT_EDITED);
						editor.setTitle(tabs[i].getPathname(), tabs[i].getFilename(), tabs[i].getFileStatus());
					} else {
						result = false;
					}
				}
			}
			setCurrentEditTab(savedPane);
			if (result) {
				EditPane editPane = getCurrentEditTab();
				FileStatus.setSaved(true);
				FileStatus.setEdited(false);
				FileStatus.set(FileStatus.NOT_EDITED);
				editPane.setFileStatus(FileStatus.NOT_EDITED);
				updateTitlesAndMenuState(editPane);
			}
		}
		return result;
	}

	/**
	 * Remove the pane and update menu status
	 */
	public void remove(EditPane editPane) {
		super.remove(editPane);
		editPane = getCurrentEditTab(); // is now next tab or null
		if (editPane == null) {
			FileStatus.set(FileStatus.NO_FILE);
			this.editor.setTitle("", "", FileStatus.NO_FILE);
			Globals.getGui().setMenuState(FileStatus.NO_FILE);
		} else {
			FileStatus.set(editPane.getFileStatus());
			updateTitlesAndMenuState(editPane);
		}
		// When last file is closed, menu is unable to respond to mnemonics
		// and accelerators. Let's have it request focus so it may do so.
		if (getTabCount() == 0)
			mainUI.haveMenuRequestFocus();
	}

	// Handy little utility to update the title on the current tab and the frame
	// title bar
	// and also to update the MARS menu state (controls which actions are enabled).
	private void updateTitlesAndMenuState(EditPane editPane) {
		editor.setTitle(editPane.getPathname(), editPane.getFilename(), editPane.getFileStatus());
		editPane.updateStaticFileStatus(); // for legacy code that depends on the static FileStatus (pre 4.0)
		Globals.getGui().setMenuState(editPane.getFileStatus());
	}

	// Handy little utility to update the title on the current tab and the frame
	// title bar
	// and also to update the MARS menu state (controls which actions are enabled).
	// DPS 9-Aug-2011
	private void updateTitles(EditPane editPane) {
		editor.setTitle(editPane.getPathname(), editPane.getFilename(), editPane.getFileStatus());
		boolean assembled = FileStatus.isAssembled();
		editPane.updateStaticFileStatus(); // for legacy code that depends on the static FileStatus (pre 4.0)
		FileStatus.setAssembled(assembled);
	}

	/**
	 * If there is an EditPane for the given file pathname, return it else return
	 * null.
	 * 
	 * @param pathname Pathname for desired file
	 * @return the EditPane for this file if it is open in the editor, or null if
	 *         not.
	 */
	public EditPane getEditPaneForFile(String pathname) {
		EditPane openPane = null;
		for (int i = 0; i < getTabCount(); i++) {
			EditPane pane = (EditPane) getComponentAt(i);
			if (pane.getPathname().equals(pathname)) {
				openPane = pane;
				break;
			}
		}
		return openPane;
	}

	/**
	 * Check whether file has unsaved edits and, if so, check with user about saving
	 * them.
	 * 
	 * @return true if no unsaved edits or if user chooses to save them or not;
	 *         false if there are unsaved edits and user cancels the operation.
	 */
	public boolean editsSavedOrAbandoned() {
		EditPane currentPane = getCurrentEditTab();
		if (currentPane != null && currentPane.hasUnsavedEdits()) {
			switch (confirm(currentPane.getFilename())) {
			case JOptionPane.YES_OPTION:
				return saveCurrentFile();
			case JOptionPane.NO_OPTION:
				return true;
			case JOptionPane.CANCEL_OPTION:
				return false;
			default: // should never occur
				return false;
			}
		} else {
			return true;
		}
	}

	private int confirm(String name) {
		return JOptionPane.showConfirmDialog(mainUI,
				"Changes to " + name + " will be lost unless you save.  Do you wish to save all changes now?",
				"Save program changes?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
	}

	private class FileOpener {
		private File mostRecentlyOpenedFile;
		private final JFileChooser fileChooser;
		private int fileFilterCount;
		private final ArrayList fileFilterList;
		private final PropertyChangeListener listenForUserAddedFileFilter;

		public FileOpener(Editor theEditor) {
			this.mostRecentlyOpenedFile = null;
			this.fileChooser = new JFileChooser();
			this.listenForUserAddedFileFilter = new ChoosableFileFilterChangeListener();
			this.fileChooser.addPropertyChangeListener(this.listenForUserAddedFileFilter);

			// Note: add sequence is significant - last one added becomes default.
			fileFilterList = new ArrayList();
			fileFilterList.add(fileChooser.getAcceptAllFileFilter());
			fileFilterList.add(FilenameFinder.getFileFilter(Globals.fileExtensions, "Assembler Files", true));
			fileFilterCount = 0; // this will trigger fileChooser file filter load in next line
			setChoosableFileFilters();
		}

		/*
		 * Launch a file chooser for name of file to open. Return true if file opened,
		 * false otherwise
		 */
		private boolean openFile() {

			FileDialog chooser = new FileDialog(mainUI, "Select a file", FileDialog.LOAD);
			chooser.setVisible(true);
			String filename = chooser.getDirectory() + chooser.getFile();
			File theFile = new File(filename);
			if (!openFile(theFile)) {
				return false;
			}
			if (theFile.canRead() && Globals.getSettings().getAssembleOnOpenEnabled()) {
				mainUI.getRunAssembleAction().actionPerformed(null);
			}
			return true;
		}

		/*
		 * Open the specified file. Return true if file opened, false otherwise
		 */

		private boolean openFile(File theFile) {
			try {
				theFile = theFile.getCanonicalFile();
			} catch (IOException ioe) {
				// nothing to do, theFile will keep current value
			}
			String currentFilePath = theFile.getPath();
			// If this file is currently already open, then simply select its tab
			EditPane editPane = getEditPaneForFile(currentFilePath);
			if (editPane != null) {
				setSelectedComponent(editPane);
				// updateTitlesAndMenuState(editPane);
				updateTitles(editPane);
				return false;
			} else {
				editPane = new EditPane(mainUI);
			}
			editPane.setPathname(currentFilePath);
			// FileStatus.reset();
			FileStatus.setName(currentFilePath);
			FileStatus.setFile(theFile);
			FileStatus.set(FileStatus.OPENING);// DPS 9-Aug-2011
			if (theFile.canRead()) {
				Globals.program = new MIPSprogram();
				try {
					Globals.program.readSource(currentFilePath);
				} catch (ProcessingException ignored) {
				}
				// DPS 1 Nov 2006. Defined a StringBuffer to receive all file contents,
				// one line at a time, before adding to the Edit pane with one setText.
				// StringBuffer is preallocated to full filelength to eliminate dynamic
				// expansion as lines are added to it. Previously, each line was appended
				// to the Edit pane as it was read, way slower due to dynamic string alloc.
				StringBuilder fileContents = new StringBuilder((int) theFile.length());
				int lineNumber = 1;
				String line = Globals.program.getSourceLine(lineNumber++);
				while (line != null) {
					fileContents.append(line).append("\n");
					line = Globals.program.getSourceLine(lineNumber++);
				}
				editPane.setSourceCode(fileContents.toString(), true);
				// The above operation generates an undoable edit, setting the initial
				// text area contents, that should not be seen as undoable by the Undo
				// action. Let's get rid of it.
				editPane.discardAllUndoableEdits();
				editPane.setShowLineNumbersEnabled(true);
				editPane.setFileStatus(FileStatus.NOT_EDITED);

				addTab(editPane.getFilename(), editPane);
				setToolTipTextAt(indexOfComponent(editPane), editPane.getPathname());
				setSelectedComponent(editPane);
				FileStatus.setSaved(true);
				FileStatus.setEdited(false);
				FileStatus.set(FileStatus.NOT_EDITED);

				// If assemble-all, then allow opening of any file w/o invalidating assembly.
				// DPS 9-Aug-2011
				if (Globals.getSettings().getBooleanSetting(mars.Settings.ASSEMBLE_ALL_ENABLED)) {
					updateTitles(editPane);
				} else {// this was the original code...
					updateTitlesAndMenuState(editPane);
					mainPane.getExecutePane().clearPane();
				}

				mainPane.setSelectedComponent(EditTabbedPane.this);
				editPane.tellEditingComponentToRequestFocusInWindow();
				mostRecentlyOpenedFile = theFile;
			}
			return true;
		}

		// Private method to generate the file chooser's list of choosable file filters.
		// It is called when the file chooser is created, and called again each time the
		// Open
		// dialog is activated. We do this because the user may have added a new filter
		// during the previous dialog. This can be done by entering e.g. *.txt in the
		// file
		// name text field. Java is funny, however, in that if the user does this then
		// cancels the dialog, the new filter will remain in the list BUT if the user
		// does
		// this then ACCEPTS the dialog, the new filter will NOT remain in the list.
		// However
		// the act of entering it causes a property change event to occur, and we have a
		// handler that will add the new filter to our internal filter list and
		// "restore" it
		// the next time this method is called. Strangely, if the user then similarly
		// adds yet another new filter, the new one becomes simply a description change
		// to the previous one, the previous object is modified AND NO PROPERTY CHANGE
		// EVENT
		// IS FIRED! I could obviously deal with this situation if I wanted to, but
		// enough
		// is enough. The limit will be one alternative filter at a time.
		// DPS... 9 July 2008

		private void setChoosableFileFilters() {
			// See if a new filter has been added to the master list. If so,
			// regenerate the fileChooser list from the master list.
			if (fileFilterCount < fileFilterList.size()
					|| fileFilterList.size() != fileChooser.getChoosableFileFilters().length) {
				fileFilterCount = fileFilterList.size();
				// First, "deactivate" the listener, because our addChoosableFileFilter
				// calls would otherwise activate it! We want it to be triggered only
				// by MARS user action.
				boolean activeListener = false;
				if (fileChooser.getPropertyChangeListeners().length > 0) {
					fileChooser.removePropertyChangeListener(listenForUserAddedFileFilter);
					activeListener = true; // we'll note this, for re-activation later
				}
				// clear out the list and populate from our own ArrayList.
				// Last one added becomes the default.
				fileChooser.resetChoosableFileFilters();
				for (Object o : fileFilterList) {
					fileChooser.addChoosableFileFilter((FileFilter) o);
				}
				// Restore listener.
				if (activeListener) {
					fileChooser.addPropertyChangeListener(listenForUserAddedFileFilter);
				}
			}
		}//////////////////////////////////////////////////////////////////////////////////
		// Private inner class for special property change listener. DPS 9 July 2008.
		// If user adds a file filter, e.g. by typing *.txt into the file text field
		// then pressing
		// Enter, then it is automatically added to the array of choosable file filters.
		// BUT, unless you
		// Cancel out of the Open dialog, it is then REMOVED from the list automatically
		// also. Here
		// we will achieve a sort of persistence at least through the current activation
		// of MARS.

		private class ChoosableFileFilterChangeListener implements PropertyChangeListener {
			public void propertyChange(java.beans.PropertyChangeEvent e) {
				if (e.getPropertyName().equals(JFileChooser.CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY)) {
					FileFilter[] newFilters = (FileFilter[]) e.getNewValue();
					FileFilter[] oldFilters = (FileFilter[]) e.getOldValue();
					if (newFilters.length > fileFilterList.size()) {
						// new filter added, so add to end of master list.
						fileFilterList.add(newFilters[newFilters.length - 1]);
					}
				}
			}
		}

	}

}