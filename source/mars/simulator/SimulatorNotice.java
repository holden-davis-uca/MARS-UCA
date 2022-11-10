package mars.simulator;

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
 * Object provided to Observers of the Simulator. They are notified at important
 * phases of the runtime simulator, such as start and stop of simulation.
 *
 * @author Pete Sanderson
 * @version January 2009
 */

public class SimulatorNotice {

	private final int action;
	private final int maxSteps;
	private final double runSpeed;
	private final int programCounter;
	public static final int SIMULATOR_START = 0;
	public static final int SIMULATOR_STOP = 1;

	/**
	 * Constructor will be called only within this package, so assume address and
	 * length are in valid ranges.
	 */
	public SimulatorNotice(final int action, final int maxSteps, final double runSpeed, final int programCounter) {
		this.action = action;
		this.maxSteps = maxSteps;
		this.runSpeed = runSpeed;
		this.programCounter = programCounter;
	}

	/** Fetch the memory address that was accessed. */
	public int getAction() { return action; }

	/** Fetch the length in bytes of the access operation (4,2,1). */
	public int getMaxSteps() { return maxSteps; }

	/** Fetch the value of the access operation (the value read or written). */
	public double getRunSpeed() {
		return runSpeed;
	}

	/** Fetch the value of the access operation (the value read or written). */
	public int getProgramCounter() {
		return programCounter;
	}

	/** String representation indicates access type, address and length in bytes */
	@Override
	public String toString() {
		return (getAction() == SIMULATOR_START ? "START " : "STOP  ") + "Max Steps " + maxSteps + " " + "Speed "
				+ (runSpeed == mars.venus.RunSpeedPanel.UNLIMITED_SPEED ? "unlimited " : "" + runSpeed + " inst/sec")
				+ "Prog Ctr " + programCounter;
	}
}
