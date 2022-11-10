package mars.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import mars.Globals;
import mars.MIPSprogram;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Memory;
import mars.mips.hardware.Register;
import mars.mips.hardware.RegisterFile;
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

/**
 * An abstract class that provides generic components to facilitate
 * implementation of a MarsTool and/or stand-alone Mars-based application.
 * Provides default definitions of both the action() method required to
 * implement MarsTool and the go() method conventionally used to launch a
 * Mars-based stand-alone application. It also provides generic definitions for
 * interactively controlling the application. The generic controls for MarsTools
 * are 3 buttons: connect/disconnect to MIPS resource (memory and/or registers),
 * reset, and close (exit). The generic controls for stand-alone Mars apps
 * include: button that triggers a file open dialog, a text field to display
 * status messages, the run-speed slider to control execution rate when running
 * a MIPS program, a button that assembles and runs the current MIPS program, a
 * button to interrupt the running MIPS program, a reset button, and an exit
 * button. Pete Sanderson, 14 November 2006.
 */
public abstract class AbstractMarsToolAndApplication extends JFrame implements MarsTool, Observer {

	/**
	 *
	 */
	private static final long serialVersionUID = -7573778295941039774L;
	protected boolean isBeingUsedAsAMarsTool = false;  // can use to determine whether invoked as MarsTool or stand-alone.
	protected AbstractMarsToolAndApplication thisMarsApp;
	private JDialog dialog;  // used only for MarsTool use.  This is the pop-up dialog that appears when menu item selected.
	protected Window theWindow;  // highest level GUI component (a JFrame for app, a JDialog for MarsTool)

	// Major GUI components
	JLabel headingLabel;
	private final String title;  // descriptive title for title bar provided to constructor.
	private final String heading; // Text to be displayed in the top portion of the main window.

	// Some GUI settings
	private final EmptyBorder emptyBorder = new EmptyBorder(4, 4, 4, 4);
	private final Color backgroundColor = Color.WHITE;

	private final int lowMemoryAddress = Memory.dataSegmentBaseAddress;
	private final int highMemoryAddress = Memory.stackBaseAddress;
	// For MarsTool, is set true when "Connect" clicked, false when "Disconnect" clicked.
	// For app, is set true when "Assemble and Run" clicked, false when program terminates.
	private volatile boolean observing = false;

	// Several structures required for stand-alone use only (not MarsTool use)
	private File mostRecentlyOpenedFile = null;
	private final Runnable interactiveGUIUpdater = new GUIUpdater();
	private MessageField operationStatusMessages;
	private JButton openFileButton, assembleRunButton, stopButton;
	private boolean multiFileAssemble = false;

	// Structure required for MarsTool use only (not stand-alone use). Want subclasses to have access.
	protected ConnectButton connectButton;

	/**
	 * Simple constructor
	 *
	 * @param title String containing title bar text
	 */
	protected AbstractMarsToolAndApplication(final String title, final String heading) {
		thisMarsApp = this;
		this.title = title;
		this.heading = heading;
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////      ABSTRACT METHODS       ///////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Required MarsTool method to return Tool name. Must be defined by subclass.
	 *
	 * @return Tool name. MARS will display this in menu item.
	 */
	@Override
	public abstract String getName();

	/**
	 * Abstract method that must be instantiated by subclass to build the main
	 * display area of the GUI. It will be placed in the CENTER area of a
	 * BorderLayout. The title is in the NORTH area, and the controls are in the
	 * SOUTH area.
	 */
	protected abstract JComponent buildMainDisplayArea();

	//////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////  METHODS WITH DEFAULT IMPLEMENTATIONS //////////////////
	//////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Run the simulator as stand-alone application. For this default
	 * implementation, the user-defined main display of the user interface is
	 * identical for both stand-alone and MARS Tools menu use, but the control
	 * buttons are different because the stand-alone must include a mechansim for
	 * controlling the opening, assembling, and executing of an underlying MIPS
	 * program. The generic controls include: a button that triggers a file open
	 * dialog, a text field to display status messages, the run-speed slider to
	 * control execution rate when running a MIPS program, a button that assembles
	 * and runs the current MIPS program, a reset button, and an exit button. This
	 * method calls 3 methods that can be defined/overriden in the subclass:
	 * initializePreGUI() for any special initialization that must be completed
	 * before building the user interface (e.g. data structures whose properties
	 * determine default GUI settings), initializePostGUI() for any special
	 * initialization that cannot be completed until after the building the user
	 * interface (e.g. data structure whose properties are determined by default GUI
	 * settings), and buildMainDisplayArea() to contain application-specific
	 * displays of parameters and results.
	 */
	public void go() {
		theWindow = this;
		isBeingUsedAsAMarsTool = false;
		thisMarsApp.setTitle(title);
		mars.Globals.initialize(true);
		// assure the dialog goes away if user clicks the X
		thisMarsApp.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent e) {
				performAppClosingDuties();
			}
		});
		initializePreGUI();

		final JPanel contentPane = new JPanel(new BorderLayout(5, 5));
		contentPane.setBorder(emptyBorder);
		contentPane.setOpaque(true);
		contentPane.add(buildHeadingArea(), BorderLayout.NORTH);
		contentPane.add(buildMainDisplayArea(), BorderLayout.CENTER);
		contentPane.add(buildButtonAreaStandAlone(), BorderLayout.SOUTH);

		thisMarsApp.setContentPane(contentPane);
		thisMarsApp.pack();
		thisMarsApp.setLocationRelativeTo(null); // center on screen     
		thisMarsApp.setVisible(true);
		initializePostGUI();
	}

	/**
	 * Required MarsTool method to carry out Tool functions. It is invoked when MARS
	 * user selects this tool from the Tools menu. This default implementation
	 * provides generic definitions for interactively controlling the tool. The
	 * generic controls for MarsTools are 3 buttons: connect/disconnect to MIPS
	 * resource (memory and/or registers), reset, and close (exit). Like "go()"
	 * above, this default version calls 3 methods that can be defined/overriden in
	 * the subclass: initializePreGUI() for any special initialization that must be
	 * completed before building the user interface (e.g. data structures whose
	 * properties determine default GUI settings), initializePostGUI() for any
	 * special initialization that cannot be completed until after the building the
	 * user interface (e.g. data structure whose properties are determined by
	 * default GUI settings), and buildMainDisplayArea() to contain
	 * application-specific displays of parameters and results.
	 */

	@Override
	public void action() {
		isBeingUsedAsAMarsTool = true;
		dialog = new JDialog(Globals.getGui(), title);
		// assure the dialog goes away if user clicks the X
		dialog.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent e) {
				performToolClosingDuties();
			}
		});
		theWindow = dialog;
		initializePreGUI();
		final JPanel contentPane = new JPanel(new BorderLayout(5, 5));
		contentPane.setBorder(emptyBorder);
		contentPane.setOpaque(true);
		contentPane.add(buildHeadingArea(), BorderLayout.NORTH);
		contentPane.add(buildMainDisplayArea(), BorderLayout.CENTER);
		contentPane.add(buildButtonAreaMarsTool(), BorderLayout.SOUTH);
		initializePostGUI();
		dialog.setContentPane(contentPane);
		dialog.pack();
		dialog.setLocationRelativeTo(Globals.getGui());
		dialog.setVisible(true);
	}

	/**
	 * Method that will be called once just before the GUI is constructed in the
	 * go() and action() methods. Use it to initialize any data structures needed
	 * for the application whose values will be needed to determine the initial
	 * state of GUI components. By default it does nothing.
	 */
	protected void initializePreGUI() {}

	/**
	 * Method that will be called once just after the GUI is constructed in the go()
	 * and action() methods. Use it to initialize data structures needed for the
	 * application whose values may depend on the initial state of GUI components.
	 * By default it does nothing.
	 */
	protected void initializePostGUI() {}

	/**
	 * Method that will be called each time the default Reset button is clicked. Use
	 * it to reset any data structures and/or GUI components. By default it does
	 * nothing.
	 */
	protected void reset() {}

	/**
	 * Constructs GUI header as label with default positioning and font. May be
	 * overridden.
	 */
	protected JComponent buildHeadingArea() {
		// OVERALL STRUCTURE OF MESSAGE (TOP)
		headingLabel = new JLabel();
		final Box headingPanel = Box.createHorizontalBox();//new JPanel(new BorderLayout());
		headingPanel.add(Box.createHorizontalGlue());
		headingPanel.add(headingLabel);
		headingPanel.add(Box.createHorizontalGlue());
		// Details for heading area (top)
		headingLabel.setText(heading);
		headingLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		headingLabel.setFont(new Font(headingLabel.getFont().getFontName(), Font.PLAIN, 18));
		return headingPanel;
	}

	/**
	 * The MarsTool default set of controls has one row of 3 buttons. It includes a
	 * dual-purpose button to attach or detach simulator to MIPS memory, a button to
	 * reset the cache, and one to close the tool.
	 */
	protected JComponent buildButtonAreaMarsTool() {
		final Box buttonArea = Box.createHorizontalBox();
		final TitledBorder tc = new TitledBorder("Tool Control");
		tc.setTitleJustification(TitledBorder.CENTER);
		buttonArea.setBorder(tc);
		connectButton = new ConnectButton();
		connectButton.setToolTipText("Control whether tool will respond to running MIPS program");
		connectButton.addActionListener(e -> {
			if (connectButton.isConnected()) {
				connectButton.disconnect();
			} else {
				connectButton.connect();
			}
		});
		connectButton.addKeyListener(new EnterKeyListener(connectButton));

		final JButton resetButton = new JButton("Reset");
		resetButton.setToolTipText("Reset all counters and other structures");
		resetButton.addActionListener(e -> reset());
		resetButton.addKeyListener(new EnterKeyListener(resetButton));

		final JButton closeButton = new JButton("Close");
		closeButton.setToolTipText("Close (exit) this tool");
		closeButton.addActionListener(e -> performToolClosingDuties());
		closeButton.addKeyListener(new EnterKeyListener(closeButton));

		// Add all the buttons...
		buttonArea.add(connectButton);
		buttonArea.add(Box.createHorizontalGlue());
		buttonArea.add(resetButton);
		buttonArea.add(Box.createHorizontalGlue());
		final JComponent helpComponent = getHelpComponent();
		if (helpComponent != null) {
			buttonArea.add(helpComponent);
			buttonArea.add(Box.createHorizontalGlue());
		}
		buttonArea.add(closeButton);
		return buttonArea;
	}

	/**
	 * The Mars stand-alone app default set of controls has two rows of controls. It
	 * includes a text field for displaying status messages, a button to trigger an
	 * open file dialog, the MARS run speed slider to control timed execution, a
	 * button to assemble and run the program, a reset button whose action is
	 * determined by the subclass reset() method, and an exit button.
	 */

	protected JComponent buildButtonAreaStandAlone() {
		// Overall structure of control area (two rows).
		final Box operationArea = Box.createVerticalBox();
		final Box fileControlArea = Box.createHorizontalBox();
		final Box buttonArea = Box.createHorizontalBox();
		operationArea.add(fileControlArea);
		operationArea.add(Box.createVerticalStrut(5));
		operationArea.add(buttonArea);
		final TitledBorder ac = new TitledBorder("Application Control");
		ac.setTitleJustification(TitledBorder.CENTER);
		operationArea.setBorder(ac);

		// Top row of controls consists of button to launch file open operation,
		// text field to show filename, and run speed slider.
		openFileButton = new JButton("Open MIPS program...");
		openFileButton.setToolTipText("Select MIPS program file to assemble and run");
		openFileButton.addActionListener(e -> {
			final JFileChooser fileChooser = new JFileChooser();
			final JCheckBox multiFileAssembleChoose = new JCheckBox("Assemble all in selected file's directory",
					multiFileAssemble);
			multiFileAssembleChoose.setToolTipText(
					"If checked, selected file will be assembled first and all other assembly files in directory will be assembled also.");
			fileChooser.setAccessory(multiFileAssembleChoose);
			if (mostRecentlyOpenedFile != null) { fileChooser.setSelectedFile(mostRecentlyOpenedFile); }
			// DPS 13 June 2007.  The next 4 lines add file filter to file chooser.
			final FileFilter defaultFileFilter = FilenameFinder.getFileFilter(Globals.fileExtensions, "Assembler Files",
					true);
			fileChooser.addChoosableFileFilter(defaultFileFilter);
			fileChooser.addChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
			fileChooser.setFileFilter(defaultFileFilter);

			if (fileChooser.showOpenDialog(thisMarsApp) == JFileChooser.APPROVE_OPTION) {
				multiFileAssemble = multiFileAssembleChoose.isSelected();
				File theFile = fileChooser.getSelectedFile();
				try {
					theFile = theFile.getCanonicalFile();
				} catch (final IOException ioe) {
					// nothing to do, theFile will keep current value
				}
				final String currentFilePath = theFile.getPath();
				mostRecentlyOpenedFile = theFile;
				operationStatusMessages.setText("File: " + currentFilePath);
				operationStatusMessages.setCaretPosition(0);
				assembleRunButton.setEnabled(true);
			}
		});
		openFileButton.addKeyListener(new EnterKeyListener(openFileButton));

		operationStatusMessages = new MessageField("No file open.");
		operationStatusMessages.setColumns(40);
		operationStatusMessages.setMargin(new Insets(0, 3, 0, 3)); //(top, left, bottom, right)
		operationStatusMessages.setBackground(backgroundColor);
		operationStatusMessages.setFocusable(false);
		operationStatusMessages.setToolTipText("Display operation status messages");

		final mars.venus.RunSpeedPanel speed = mars.venus.RunSpeedPanel.getInstance();

		// Bottom row of controls consists of the three buttons defined here.
		assembleRunButton = new JButton("Assemble and Run");
		assembleRunButton.setToolTipText("Assemble and run the currently selected MIPS program");
		assembleRunButton.setEnabled(false);
		assembleRunButton.addActionListener(e -> {
			assembleRunButton.setEnabled(false);
			openFileButton.setEnabled(false);
			stopButton.setEnabled(true);
			new Thread(new CreateAssembleRunMIPSprogram()).start();
		});
		assembleRunButton.addKeyListener(new EnterKeyListener(assembleRunButton));

		stopButton = new JButton("Stop");
		stopButton.setToolTipText("Terminate MIPS program execution");
		stopButton.setEnabled(false);
		stopButton.addActionListener(e -> mars.simulator.Simulator.getInstance().stopExecution(null));
		stopButton.addKeyListener(new EnterKeyListener(stopButton));

		final JButton resetButton = new JButton("Reset");
		resetButton.setToolTipText("Reset all counters and other structures");
		resetButton.addActionListener(e -> reset());
		resetButton.addKeyListener(new EnterKeyListener(resetButton));

		final JButton closeButton = new JButton("Exit");
		closeButton.setToolTipText("Exit this application");
		closeButton.addActionListener(e -> performAppClosingDuties());
		closeButton.addKeyListener(new EnterKeyListener(closeButton));

		// Add top row of controls...
		//fileControlArea.add(Box.createHorizontalStrut(5));

		final Box fileDisplayBox = Box.createVerticalBox();
		fileDisplayBox.add(Box.createVerticalStrut(8));
		fileDisplayBox.add(operationStatusMessages);
		fileDisplayBox.add(Box.createVerticalStrut(8));
		fileControlArea.add(fileDisplayBox);

		fileControlArea.add(Box.createHorizontalGlue());
		fileControlArea.add(speed);

		// Add bottom row of buttons...

		buttonArea.add(openFileButton);
		buttonArea.add(Box.createHorizontalGlue());
		buttonArea.add(assembleRunButton);
		buttonArea.add(Box.createHorizontalGlue());
		buttonArea.add(stopButton);
		buttonArea.add(Box.createHorizontalGlue());
		buttonArea.add(resetButton);
		buttonArea.add(Box.createHorizontalGlue());
		final JComponent helpComponent = getHelpComponent();
		if (helpComponent != null) {
			buttonArea.add(helpComponent);
			buttonArea.add(Box.createHorizontalGlue());
		}
		buttonArea.add(closeButton);
		return operationArea;
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//  Rest of the methods.  Some are used by stand-alone (JFrame-based) only, some are
	//  used by MarsTool (JDialog-based) only, others are used by both.
	//////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Called when receiving notice of access to MIPS memory or registers. Default
	 * implementation of method required by Observer interface. This method will
	 * filter out notices originating from the MARS GUI or from direct user editing
	 * of memory or register displays. Only notices arising from MIPS program access
	 * are allowed in. It then calls two methods to be overridden by the subclass
	 * (since they do nothing by default): processMIPSUpdate() then updateDisplay().
	 *
	 * @param resource     the attached MIPS resource
	 * @param accessNotice AccessNotice information provided by the resource
	 */
	@Override
	public void update(final Observable resource, final Object accessNotice) {
		if (((AccessNotice) accessNotice).accessIsFromMIPS()) {
			processMIPSUpdate(resource, (AccessNotice) accessNotice);
			updateDisplay();
		}
	}

	/**
	 * Override this method to process a received notice from MIPS Observable
	 * (memory or register) It will only be called if the notice was generated as
	 * the result of MIPS instruction execution. By default it does nothing. After
	 * this method is complete, the updateDisplay() method will be invoked
	 * automatically.
	 */
	protected void processMIPSUpdate(final Observable resource, final AccessNotice notice) {}

	/**
	 * This method is called when tool/app is exited either through the close/exit
	 * button or the window's X box. Override it to perform any special
	 * housecleaning needed. By default it does nothing.
	 */
	protected void performSpecialClosingDuties() {}

	/**
	 * Add this app/tool as an Observer of desired MIPS Observables (memory and
	 * registers). By default, will add as an Observer of the entire Data Segment in
	 * memory. Override if you want something different. Note that the Memory
	 * methods to add an Observer to memory are flexible (you can register for a
	 * range of addresses) but may throw an AddressErrorException that you need to
	 * catch. This method is called whenever the default "Connect" button on a
	 * MarsTool or the default "Assemble and run" on a stand-alone Mars app is
	 * selected. The corresponding NOTE: if you do not want to register as an
	 * Observer of the entire data segment (starts at address 0x10000000) then
	 * override this to either do some alternative or nothing at all. This method is
	 * also overloaded to allow arbitrary memory subrange.
	 */

	protected void addAsObserver() {
		addAsObserver(lowMemoryAddress, highMemoryAddress);
	}

	/**
	 * Add this app/tool as an Observer of the specified subrange of MIPS memory.
	 * Note that this method is not invoked automatically like the no-argument
	 * version, but if you use this method, you can still take advantage of provided
	 * default deleteAsObserver() since it will remove the app as a memory observer
	 * regardless of the subrange or number of subranges it is registered for.
	 *
	 * @param lowEnd  low end of memory address range.
	 * @param highEnd high end of memory address range; must be >= lowEnd
	 */

	protected void addAsObserver(final int lowEnd, final int highEnd) {
		final String errorMessage = "Error connecting to MIPS memory";
		try {
			Globals.memory.addObserver(thisMarsApp, lowEnd, highEnd);
		} catch (final AddressErrorException aee) {
			if (isBeingUsedAsAMarsTool) {
				headingLabel.setText(errorMessage);
			} else {
				operationStatusMessages.displayTerminatingMessage(errorMessage);
			}
		}
	}

	/**
	 * Add this app/tool as an Observer of the specified MIPS register.
	 */
	protected void addAsObserver(final Register reg) {
		if (reg != null) { reg.addObserver(thisMarsApp); }
	}

	/**
	 * Delete this app/tool as an Observer of MIPS Observables (memory and
	 * registers). By default, will delete as an Observer of memory. Override if you
	 * want something different. This method is called when the default "Disconnect"
	 * button on a MarsTool is selected or when the MIPS program execution triggered
	 * by the default "Assemble and run" on a stand-alone Mars app terminates (e.g.
	 * when the button is re-enabled).
	 */

	protected void deleteAsObserver() {
		Globals.memory.deleteObserver(thisMarsApp);
	}

	/**
	 * Delete this app/tool as an Observer of the specified MIPS register
	 */

	protected void deleteAsObserver(final Register reg) {
		if (reg != null) { reg.deleteObserver(thisMarsApp); }
	}

	/**
	 * Query method to let you know if the tool/app is (or could be) currently
	 * "observing" any MIPS resources. When running as a MarsTool, this will be true
	 * by default after clicking the "Connect to MIPS" button until "Disconnect from
	 * MIPS" is clicked. When running as a stand-alone app, this will be true by
	 * default after clicking the "Assemble and Run" button until until program
	 * execution has terminated either normally or by clicking the "Stop" button.
	 * The phrase "or could be" was added above because depending on how the
	 * tool/app operates, it may be possible to run the MIPS program without first
	 * registering as an Observer -- i.e. addAsObserver() is overridden and takes no
	 * action.
	 *
	 * @return true if tool/app is (or could be) currently active as an Observer.
	 */

	protected boolean isObserving() { return observing; }

	/**
	 * Override this method to implement updating of GUI after each MIPS instruction
	 * is executed, while running in "timed" mode (user specifies execution speed on
	 * the slider control). Does nothing by default.
	 */
	protected void updateDisplay() {}

	/**
	 * Override this method to provide a JComponent (probably a JButton) of your
	 * choice to be placed just left of the Close/Exit button. Its anticipated use
	 * is for a "help" button that launches a help message or dialog. But it can be
	 * any valid JComponent that doesn't mind co-existing among a bunch of JButtons.
	 */
	protected JComponent getHelpComponent() { return null; }

	//////////////////////////////////////////////////////////////////////////////////
	////////////////////  PRIVATE HELPER METHODS    //////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////

	// Closing duties for MarsTool only.
	private void performToolClosingDuties() {
		performSpecialClosingDuties();
		if (connectButton.isConnected()) { connectButton.disconnect(); }
		dialog.setVisible(false);
		dialog.dispose();
	}

	// Closing duties for stand-alone application only.
	private void performAppClosingDuties() {
		performSpecialClosingDuties();
		thisMarsApp.setVisible(false);
		System.exit(0);
	}

	//////////////////////////////////////////////////////////////////////////////////
	////////////////////  PRIVATE HELPER CLASSES    //////////////////////////////////
	//  Specialized inner classes.  Either used by stand-alone (JFrame-based) only  //
	//  or used by MarsTool (JDialog-based) only.                                   //
	//////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// Little class for this dual-purpose button.  It is used only by the MarsTool
	// (not by the stand-alone app).
	protected class ConnectButton extends JButton {

		/**
		 *
		 */
		private static final long serialVersionUID = -3055306487498118908L;
		private static final String connectText = "Connect to MIPS";
		private static final String disconnectText = "Disconnect from MIPS";

		public ConnectButton() {
			super();
			disconnect();
		}

		public void connect() {
			observing = true;
			synchronized (Globals.memoryAndRegistersLock) {// DPS 23 July 2008
				addAsObserver();
			}
			setText(disconnectText);
		}

		public void disconnect() {
			synchronized (Globals.memoryAndRegistersLock) {// DPS 23 July 2008
				deleteAsObserver();
			}
			observing = false;
			setText(connectText);
		}

		public boolean isConnected() { return observing; }
	}

	///////////////////////////////////////////////////////////////////////
	//  Every control button will get one of these so when it has focus
	//  the Enter key can be used instead of a mouse click to perform
	//  its associated action.  It will do nothing if no action listeners
	//  are attached to the button at the time of the call.  Otherwise,
	//  it will call actionPerformed for the first action listener in the
	//  button's list.
	protected class EnterKeyListener extends KeyAdapter {

		AbstractButton myButton;

		public EnterKeyListener(final AbstractButton who) {
			myButton = who;
		}

		@Override
		public void keyPressed(final KeyEvent e) {
			if (e.getKeyChar() == KeyEvent.VK_ENTER) {
				e.consume();
				try {
					myButton.getActionListeners()[0].actionPerformed(new ActionEvent(myButton, 0, myButton.getText()));
				} catch (final ArrayIndexOutOfBoundsException oob) {
					// do nothing, since there is no action listener.
				}
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////
	// called when the Assemble and Run button is pressed.  Used only by stand-alone app.
	private class CreateAssembleRunMIPSprogram implements Runnable {

		@Override
		public void run() {
			String exceptionHandler = null;
			if (Globals.getSettings().getExceptionHandlerEnabled() && Globals.getSettings()
					.getExceptionHandler() != null && Globals.getSettings().getExceptionHandler().length() > 0) {
				exceptionHandler = Globals.getSettings().getExceptionHandler();
			}

			Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
			Thread.yield();
			final MIPSprogram program = new MIPSprogram();
			mars.Globals.program = program; // Shouldn't have to do this...
			final String fileToAssemble = mostRecentlyOpenedFile.getPath();
			ArrayList filesToAssemble = null;
			if (multiFileAssemble) {// setting (check box in file open dialog) calls for multiple file assembly
				filesToAssemble = FilenameFinder.getFilenameList(new File(fileToAssemble).getParent(),
						Globals.fileExtensions);
			} else {
				filesToAssemble = new ArrayList();
				filesToAssemble.add(fileToAssemble);
			}
			ArrayList programsToAssemble = null;
			try {
				operationStatusMessages.displayNonTerminatingMessage("Assembling " + fileToAssemble);
				programsToAssemble = program.prepareFilesForAssembly(filesToAssemble, fileToAssemble, exceptionHandler);
			} catch (final mars.ProcessingException pe) {
				operationStatusMessages.displayTerminatingMessage("Error reading file(s): " + fileToAssemble);
				return;
			}

			try {
				program.assemble(programsToAssemble, Globals.getSettings().getExtendedAssemblerEnabled(), Globals
						.getSettings().getWarningsAreErrors());
			} catch (final mars.ProcessingException pe) {
				operationStatusMessages.displayTerminatingMessage("Assembly Error: " + fileToAssemble);
				return;
			}
			// Moved these three register resets from before the try block to after it.  17-Dec-09 DPS.
			RegisterFile.resetRegisters();
			Coprocessor1.resetRegisters();
			Coprocessor0.resetRegisters();

			addAsObserver();
			observing = true;
			String terminatingMessage = "Normal termination: ";
			try {
				operationStatusMessages.displayNonTerminatingMessage("Running " + fileToAssemble);
				program.simulate(-1); // unlimited steps
			} catch (final NullPointerException npe) {
				// This will occur if program execution is interrupted by Stop button.
				terminatingMessage = "User interrupt: ";
			} catch (final mars.ProcessingException pe) {
				terminatingMessage = "Runtime error: ";
			} finally {
				deleteAsObserver();
				observing = false;
				operationStatusMessages.displayTerminatingMessage(terminatingMessage + fileToAssemble);
			}
			return;
		}
	}

	//////////////////////////////////////////////////////////////////////////
	//  Class for text message field used to update operation status when
	//  assembling and running MIPS programs.
	private class MessageField extends JTextField {

		/**
		 *
		 */
		private static final long serialVersionUID = 1407521907210021445L;

		public MessageField(final String text) {
			super(text);
		}

		private void displayTerminatingMessage(final String text) {
			displayMessage(text, true);
		}

		private void displayNonTerminatingMessage(final String text) {
			displayMessage(text, false);
		}

		private void displayMessage(final String text, final boolean terminating) {
			SwingUtilities.invokeLater(new MessageWriter(text, terminating));
		}

		/////////////////////////////////////////////////////////////////////////////////
		// Little inner-inner class to display processing error message on AWT thread.
		// Used only by stand-alone app.
		private class MessageWriter implements Runnable {

			private final String text;
			private final boolean terminatingMessage;

			public MessageWriter(final String text, final boolean terminating) {
				this.text = text;
				terminatingMessage = terminating;
			}

			@Override
			public void run() {
				if (text != null) {
					operationStatusMessages.setText(text);
					operationStatusMessages.setCaretPosition(0);
				}
				if (terminatingMessage) {
					assembleRunButton.setEnabled(true);
					openFileButton.setEnabled(true);
					stopButton.setEnabled(false);
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	//  For scheduling GUI update on timed runs...used only by stand-alone app.
	private class GUIUpdater implements Runnable {

		@Override
		public void run() {
			updateDisplay();
		}
	}

}
