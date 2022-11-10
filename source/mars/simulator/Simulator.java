package mars.simulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;

import mars.ErrorList;
import mars.ErrorMessage;
import mars.Globals;
import mars.MIPSprogram;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
import mars.mips.instructions.BasicInstruction;
import mars.util.Binary;
import mars.util.SystemIO;
import mars.venus.RunGoAction;
import mars.venus.RunSpeedPanel;
import mars.venus.RunStepAction;

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
 * Used to simulate the execution of an assembled MIPS program.
 *
 * @author Pete Sanderson
 * @version August 2005
 **/

public class Simulator extends Observable {

	private SimThread simulatorThread;
	private static Simulator simulator = null;  // Singleton object
	private static Runnable interactiveGUIUpdater = null;
	// Others can set this true to indicate external interrupt.  Initially used
	// to simulate keyboard and display interrupts.  The device is identified
	// by the address of its MMIO control register.  keyboard 0xFFFF0000 and
	// display 0xFFFF0008.  DPS 23 July 2008.
	public static final int NO_DEVICE = 0;
	public static volatile int externalInterruptingDevice = NO_DEVICE;
	/** various reasons for simulate to end... */
	public static final int BREAKPOINT = 1;
	public static final int EXCEPTION = 2;
	public static final int MAX_STEPS = 3;  // includes step mode (where maxSteps is 1)
	public static final int NORMAL_TERMINATION = 4;
	public static final int CLIFF_TERMINATION = 5; // run off bottom of program
	public static final int PAUSE_OR_STOP = 6;

	/**
	 * Returns the Simulator object
	 *
	 * @return the Simulator object in use
	 */
	public static Simulator getInstance() {
		// Do NOT change this to create the Simulator at load time (in declaration above)!
		// Its constructor looks for the GUI, which at load time is not created yet,
		// and incorrectly leaves interactiveGUIUpdater null!  This causes runtime
		// exceptions while running in timed mode.
		if (simulator == null) { simulator = new Simulator(); }
		return simulator;
	}

	private Simulator() {
		simulatorThread = null;
		if (Globals.getGui() != null) { interactiveGUIUpdater = new UpdateGUI(); }
	}

	/**
	 * Determine whether or not the next instruction to be executed is in a "delay
	 * slot". This means delayed branching is enabled, the branch condition has
	 * evaluated true, and the next instruction executed will be the one following
	 * the branch. It is said to occupy the "delay slot." Normally programmers put a
	 * nop instruction here but it can be anything.
	 *
	 * @return true if next instruction is in delay slot, false otherwise.
	 */

	public static boolean inDelaySlot() {
		return DelayedBranch.isTriggered();
	}

	/**
	 * Simulate execution of given MIPS program. It must have already been
	 * assembled.
	 *
	 * @param p           The MIPSprogram to be simulated.
	 * @param pc          address of first instruction to simulate; this goes into
	 *                    program counter
	 * @param maxSteps    maximum number of steps to perform before returning false
	 *                    (0 or less means no max)
	 * @param breakPoints array of breakpoint program counter values, use null if
	 *                    none
	 * @param actor       the GUI component responsible for this call, usually GO or
	 *                    STEP. null if none.
	 * @return true if execution completed, false otherwise
	 * @throws ProcessingException Throws exception if run-time exception occurs.
	 **/

	public boolean simulate(final MIPSprogram p, final int pc, final int maxSteps, final int[] breakPoints,
			final AbstractAction actor) throws ProcessingException {
		simulatorThread = new SimThread(p, pc, maxSteps, breakPoints, actor);
		simulatorThread.start();

		// Condition should only be true if run from command-line instead of GUI.
		// If so, just stick around until execution thread is finished.
		if (actor == null) {
			simulatorThread.get();
			final ProcessingException pe = simulatorThread.pe;
			final boolean done = simulatorThread.done;
			if (done) {
				SystemIO.resetFiles(); // close any files opened in MIPS progra
			}
			simulatorThread = null;
			if (pe != null) { throw pe; }
			return done;
		}
		return true;
	}

	/**
	 * Set the volatile stop boolean variable checked by the execution thread at the
	 * end of each MIPS instruction execution. If variable is found to be true, the
	 * execution thread will depart gracefully so the main thread handling the GUI
	 * can take over. This is used by both STOP and PAUSE features.
	 */
	public void stopExecution(final AbstractAction actor) {

		if (simulatorThread != null) {
			simulatorThread.setStop(actor);
			for (final StopListener l : stopListeners) {
				l.stopped(this);
			}
			simulatorThread = null;
		}
	}

	/* This interface is required by the Asker class in MassagesPane
	 * to be notified about the fact that the user has requested to
	 * stop the execution. When that happens, it must unblock the
	 * simulator thread. */
	public interface StopListener {

		void stopped(Simulator s);
	}

	private final ArrayList<StopListener> stopListeners = new ArrayList<>(1);

	public void addStopListener(final StopListener l) {
		stopListeners.add(l);
	}

	public void removeStopListener(final StopListener l) {
		stopListeners.remove(l);
	}

	// The Simthread object will call this method when it enters and returns from
	// its construct() method.  These signal start and stop, respectively, of
	// simulation execution.  The observer can then adjust its own state depending
	// on the execution state.  Note that "stop" and "done" are not the same thing.
	// "stop" just means it is leaving execution state; this could be triggered
	// by Stop button, by Pause button, by Step button, by runtime exception, by
	// instruction count limit, by breakpoint, or by end of simulation (truly done).
	private void notifyObserversOfExecutionStart(final int maxSteps, final int programCounter) {
		setChanged();
		this.notifyObservers(new SimulatorNotice(SimulatorNotice.SIMULATOR_START, maxSteps, RunSpeedPanel.getInstance()
				.getRunSpeed(), programCounter));
	}

	private void notifyObserversOfExecutionStop(final int maxSteps, final int programCounter) {
		setChanged();
		this.notifyObservers(new SimulatorNotice(SimulatorNotice.SIMULATOR_STOP, maxSteps, RunSpeedPanel.getInstance()
				.getRunSpeed(), programCounter));
	}

	/**
	 * SwingWorker subclass to perform the simulated execution in background thread.
	 * It is "interrupted" when main thread sets the "stop" variable to true. The
	 * variable is tested before the next MIPS instruction is simulated. Thus
	 * interruption occurs in a tightly controlled fashion. See SwingWorker.java for
	 * more details on its functionality and usage. It is provided by Sun
	 * Microsystems for download and is not part of the Swing library.
	 */

	class SimThread extends SwingWorker {

		private final MIPSprogram p;
		private final int pc, maxSteps;
		private int[] breakPoints;
		private boolean done;
		private ProcessingException pe;
		private volatile boolean stop = false;
		private volatile AbstractAction stopper;
		private final AbstractAction starter;
		private int constructReturnReason;

		/**
		 * SimThread constructor. Receives all the information it needs to simulate
		 * execution.
		 *
		 * @param p           the MIPSprogram to be simulated
		 * @param pc          address in text segment of first instruction to simulate
		 * @param maxSteps    maximum number of instruction steps to simulate. Default
		 *                    of -1 means no maximum
		 * @param breakPoints array of breakpoints (instruction addresses) specified by
		 *                    user
		 * @param starter     the GUI component responsible for this call, usually GO or
		 *                    STEP. null if none.
		 */
		SimThread(final MIPSprogram p, final int pc, final int maxSteps, final int[] breakPoints,
				final AbstractAction starter) {
			super(Globals.getGui() != null);
			this.p = p;
			this.pc = pc;
			this.maxSteps = maxSteps;
			this.breakPoints = breakPoints;
			done = false;
			pe = null;
			this.starter = starter;
			stopper = null;
		}

		/**
		 * Sets to "true" the volatile boolean variable that is tested after each MIPS
		 * instruction is executed. After calling this method, the next test will yield
		 * "true" and "construct" will return.
		 *
		 * @param actor the Swing component responsible for this call.
		 */
		public void setStop(final AbstractAction actor) {
			stop = true;
			stopper = actor;
		}

		/**
		 * This is comparable to the Runnable "run" method (it is called by
		 * SwingWorker's "run" method). It simulates the program execution in the
		 * backgorund.
		 *
		 * @return boolean value true if execution done, false otherwise
		 */

		@Override
		public Object construct() {
			// The next two statements are necessary for GUI to be consistently updated
			// before the simulation gets underway.  Without them, this happens only intermittently,
			// with a consequence that some simulations are interruptable using PAUSE/STOP and others
			// are not (because one or the other or both is not yet enabled).
			Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
			Thread.yield();  // let the main thread run a bit to finish updating the GUI

			if (breakPoints == null || breakPoints.length == 0) {
				breakPoints = null;
			} else {
				Arrays.sort(breakPoints);  // must be pre-sorted for binary search
			}

			Simulator.getInstance().notifyObserversOfExecutionStart(maxSteps, pc);

			RegisterFile.initializeProgramCounter(pc);
			ProgramStatement statement = null;
			try {
				statement = Globals.memory.getStatement(RegisterFile.getProgramCounter());
			} catch (final AddressErrorException e) {
				final ErrorList el = new ErrorList();
				el.add(new ErrorMessage((MIPSprogram) null, 0, 0, "invalid program counter value: " + Binary
						.intToHexString(RegisterFile.getProgramCounter())));
				pe = new ProcessingException(el, e);
				// Next statement is a hack.  Previous statement sets EPC register to ProgramCounter-4
				// because it assumes the bad address comes from an operand so the ProgramCounter has already been
				// incremented.  In this case, bad address is the instruction fetch itself so Program Counter has
				// not yet been incremented.  We'll set the EPC directly here.  DPS 8-July-2013
				Coprocessor0.updateRegister(Coprocessor0.EPC, RegisterFile.getProgramCounter());
				constructReturnReason = EXCEPTION;
				done = true;
				SystemIO.resetFiles(); // close any files opened in MIPS program
				Simulator.getInstance().notifyObserversOfExecutionStop(maxSteps, pc);
				return new Boolean(done);
			}
			int steps = 0;

			// *******************  PS addition 26 July 2006  **********************
			// A couple statements below were added for the purpose of assuring that when
			// "back stepping" is enabled, every instruction will have at least one entry
			// on the back-stepping stack.  Most instructions will because they write either
			// to a register or memory.  But "nop" and branches not taken do not.  When the
			// user is stepping backward through the program, the stack is popped and if
			// an instruction has no entry it will be skipped over in the process.  This has
			// no effect on the correctness of the mechanism but the visual jerkiness when
			// instruction highlighting skips such instrutions is disruptive.  Current solution
			// is to add a "do nothing" stack entry for instructions that do no write anything.
			// To keep this invisible to the "simulate()" method writer, we
			// will push such an entry onto the stack here if there is none for this instruction
			// by the time it has completed simulating.  This is done by the IF statement
			// just after the call to the simulate method itself.  The BackStepper method does
			// the aforementioned check and decides whether to push or not.  The result
			// is a a smoother interaction experience.  But it comes at the cost of slowing
			// simulation speed for flat-out runs, for every MIPS instruction executed even
			// though very few will require the "do nothing" stack entry.  For stepped or
			// timed execution the slower execution speed is not noticeable.
			//
			// To avoid this cost I tried a different technique: back-fill with "do nothings"
			// during the backstepping itself when this situation is recognized.  Problem
			// was in recognizing all possible situations in which the stack contained such
			// a "gap".  It became a morass of special cases and it seemed every weird test
			// case revealed another one.  In addition, when a program
			// begins with one or more such instructions ("nop" and branches not taken),
			// the backstep button is not enabled until a "real" instruction is executed.
			// This is noticeable in stepped mode.
			// *********************************************************************

			int pc = 0;  // added: 7/26/06 (explanation above)

			while (statement != null) {
				pc = RegisterFile.getProgramCounter(); // added: 7/26/06 (explanation above)
				RegisterFile.incrementPC();
				// Perform the MIPS instruction in synchronized block.  If external threads agree
				// to access MIPS memory and registers only through synchronized blocks on same
				// lock variable, then full (albeit heavy-handed) protection of MIPS memory and
				// registers is assured.  Not as critical for reading from those resources.
				synchronized (Globals.memoryAndRegistersLock) {
					try {
						if (Simulator.externalInterruptingDevice != NO_DEVICE) {
							final int deviceInterruptCode = externalInterruptingDevice;
							Simulator.externalInterruptingDevice = NO_DEVICE;
							throw new ProcessingException(statement, "External Interrupt", deviceInterruptCode);
						}
						final BasicInstruction instruction = (BasicInstruction) statement.getInstruction();
						if (instruction == null) {
							throw new ProcessingException(statement, "undefined instruction (" + Binary.intToHexString(
									statement.getBinaryStatement()) + ")", Exceptions.RESERVED_INSTRUCTION_EXCEPTION);
						}
						// THIS IS WHERE THE INSTRUCTION EXECUTION IS ACTUALLY SIMULATED!
						instruction.getSimulationCode().simulate(statement);

						// IF statement added 7/26/06 (explanation above)
						if (Globals.getSettings().getBackSteppingEnabled()) {
							Globals.program.getBackStepper().addDoNothing(pc);
						}
					} catch (final ProcessingException pe) {
						if (pe.errors() == null) {
							constructReturnReason = NORMAL_TERMINATION;
							done = true;
							SystemIO.resetFiles(); // close any files opened in MIPS program
							Simulator.getInstance().notifyObserversOfExecutionStop(maxSteps, pc);
							return new Boolean(done); // execution completed without error.
						} else {
							// See if an exception handler is present.  Assume this is the case
							// if and only if memory location Memory.exceptionHandlerAddress
							// (e.g. 0x80000180) contains an instruction.  If so, then set the
							// program counter there and continue.  Otherwise terminate the
							// MIPS program with appropriate error message.
							ProgramStatement exceptionHandler = null;
							try {
								exceptionHandler = Globals.memory.getStatement(Memory.exceptionHandlerAddress);
							} catch (final AddressErrorException aee) {} // will not occur with this well-known addres
							if (exceptionHandler != null) {
								RegisterFile.setProgramCounter(Memory.exceptionHandlerAddress);
							} else {
								constructReturnReason = EXCEPTION;
								this.pe = pe;
								done = true;
								SystemIO.resetFiles(); // close any files opened in MIPS program
								Simulator.getInstance().notifyObserversOfExecutionStop(maxSteps, pc);
								return new Boolean(done);
							}
						}
					}
				}// end synchronized block

				///////// DPS 15 June 2007.  Handle delayed branching if it occurs./////
				if (DelayedBranch.isTriggered()) {
					RegisterFile.setProgramCounter(DelayedBranch.getBranchTargetAddress());
					DelayedBranch.clear();
				} else if (DelayedBranch.isRegistered()) { DelayedBranch.trigger(); }//////////////////////////////////////////////////////////////////////

				// Volatile variable initialized false but can be set true by the main thread.
				// Used to stop or pause a running MIPS program.  See stopSimulation() above.
				if (stop == true) {
					constructReturnReason = PAUSE_OR_STOP;
					done = false;
					Simulator.getInstance().notifyObserversOfExecutionStop(maxSteps, pc);
					return new Boolean(done);
				}
				//	Return if we've reached a breakpoint.
				if (breakPoints != null && Arrays.binarySearch(breakPoints, RegisterFile.getProgramCounter()) >= 0) {
					constructReturnReason = BREAKPOINT;
					done = false;
					Simulator.getInstance().notifyObserversOfExecutionStop(maxSteps, pc);
					return new Boolean(done); // false;
				}
				// Check number of MIPS instructions executed.  Return if at limit (-1 is no limit).
				if (maxSteps > 0) {
					steps++;
					if (steps >= maxSteps) {
						constructReturnReason = MAX_STEPS;
						done = false;
						Simulator.getInstance().notifyObserversOfExecutionStop(maxSteps, pc);
						return new Boolean(done);// false;
					}
				}

				// schedule GUI update only if: there is in fact a GUI! AND
				//                              using Run,  not Step (maxSteps > 1) AND
				//                              running slowly enough for GUI to keep up
				//if (Globals.getGui() != null && maxSteps != 1 &&
				if (interactiveGUIUpdater != null && maxSteps != 1 && RunSpeedPanel.getInstance()
						.getRunSpeed() < RunSpeedPanel.UNLIMITED_SPEED) {
					SwingUtilities.invokeLater(interactiveGUIUpdater);
				}
				if (Globals.getGui() != null || Globals.runSpeedPanelExists) { // OR added by DPS 24 July 2008 to enable speed control by stand-alone tool
					if (maxSteps != 1 && RunSpeedPanel.getInstance().getRunSpeed() < RunSpeedPanel.UNLIMITED_SPEED) {
						try {
							Thread.sleep((int) (1000 / RunSpeedPanel.getInstance().getRunSpeed())); // make sure it's never zero!
						} catch (final InterruptedException e) {}
					}
				}

				// Get next instruction in preparation for next iteration.

				try {
					statement = Globals.memory.getStatement(RegisterFile.getProgramCounter());
				} catch (final AddressErrorException e) {
					final ErrorList el = new ErrorList();
					el.add(new ErrorMessage((MIPSprogram) null, 0, 0, "invalid program counter value: " + Binary
							.intToHexString(RegisterFile.getProgramCounter())));
					pe = new ProcessingException(el, e);
					// Next statement is a hack.  Previous statement sets EPC register to ProgramCounter-4
					// because it assumes the bad address comes from an operand so the ProgramCounter has already been
					// incremented.  In this case, bad address is the instruction fetch itself so Program Counter has
					// not yet been incremented.  We'll set the EPC directly here.  DPS 8-July-2013
					Coprocessor0.updateRegister(Coprocessor0.EPC, RegisterFile.getProgramCounter());
					constructReturnReason = EXCEPTION;
					done = true;
					SystemIO.resetFiles(); // close any files opened in MIPS program
					Simulator.getInstance().notifyObserversOfExecutionStop(maxSteps, pc);
					return new Boolean(done);
				}
			}
			// DPS July 2007.  This "if" statement is needed for correct program
			// termination if delayed branching on and last statement in
			// program is a branch/jump.  Program will terminate rather than branch,
			// because that's what MARS does when execution drops off the bottom.
			if (DelayedBranch.isTriggered() || DelayedBranch.isRegistered()) { DelayedBranch.clear(); }
			// If we got here it was due to null statement, which means program
			// counter "fell off the end" of the program.  NOTE: Assumes the
			// "while" loop contains no "break;" statements.
			constructReturnReason = CLIFF_TERMINATION;
			done = true;
			SystemIO.resetFiles(); // close any files opened in MIPS program
			Simulator.getInstance().notifyObserversOfExecutionStop(maxSteps, pc);
			return new Boolean(done); // true;  // execution completed
		}

		/**
		 * This method is invoked by the SwingWorker when the "construct" method
		 * returns. It will update the GUI appropriately. According to Sun's
		 * documentation, it is run in the main thread so should work OK with Swing
		 * components (which are not thread-safe). Its action depends on what caused the
		 * return from construct() and what action led to the call of construct() in the
		 * first place.
		 */

		@Override
		public void finished() {
			// If running from the command-line, then there is no GUI to update.
			if (Globals.getGui() == null) { return; }
			final String starterName = (String) starter.getValue(Action.NAME);
			if (starterName.equals("Step")) { ((RunStepAction) starter).stepped(done, constructReturnReason, pe); }
			if (starterName.equals("Go")) {
				if (done) {
					((RunGoAction) starter).stopped(pe, constructReturnReason);
				} else if (constructReturnReason == BREAKPOINT) {
					((RunGoAction) starter).paused(done, constructReturnReason, pe);
				} else {
					final String stopperName = (String) stopper.getValue(Action.NAME);
					if ("Pause".equals(stopperName)) {
						((RunGoAction) starter).paused(done, constructReturnReason, pe);
					} else if ("Stop".equals(stopperName)) {
						((RunGoAction) starter).stopped(pe, constructReturnReason);
					}
				}
			}
			return;
		}

	}

	private class UpdateGUI implements Runnable {

		@Override
		public void run() {
			if (Globals.getGui().getRegistersPane().getSelectedComponent() == Globals.getGui().getMainPane()
					.getExecutePane().getRegistersWindow()) {
				Globals.getGui().getMainPane().getExecutePane().getRegistersWindow().updateRegisters();
			} else {
				Globals.getGui().getMainPane().getExecutePane().getCoprocessor1Window().updateRegisters();
			}
			Globals.getGui().getMainPane().getExecutePane().getDataSegmentWindow().updateValues();
			Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow().setCodeHighlighting(true);
			Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow().highlightStepAtPC();
		}
	}

}
