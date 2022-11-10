package mars.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.util.Observable;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.util.Binary;

/*
 * Copyright (c) 2003-2011, Pete Sanderson and Kenneth Vollmar
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
 * A data cache simulator. It can be run either as a stand-alone Java
 * application having access to the mars package, or through MARS as an item in
 * its Tools menu. It makes maximum use of methods inherited from its abstract
 * superclass AbstractMarsToolOrApp. Pete Sanderson, v 1.0: 16-18 October 2006,
 * v 1.1: 7 November 2006. v 1.2: 23 December 2010.
 * <p>
 * Version 1.2 fixes a bug in the hit/miss animator under full or N-way set
 * associative. It was animating the block of initial access (first block of
 * set). Now it animates the block of final access (where address found or
 * stored). Also added log display to GUI (previously System.out).
 * </p>
 */
public class CacheSimulator extends AbstractMarsToolAndApplication {

	/**
	 *
	 */
	private static final long serialVersionUID = -967183514374424213L;
	private static boolean debug = false; // controls display of debugging info
	private static String version = "Version 1.2";
	private static String heading = "Simulate and illustrate data cache performance";
	// Major GUI components
	private JComboBox cacheBlockSizeSelector, cacheBlockCountSelector, cachePlacementSelector, cacheReplacementSelector,
			cacheSetSizeSelector;
	private JTextField memoryAccessCountDisplay, cacheHitCountDisplay, cacheMissCountDisplay, replacementPolicyDisplay,
			cachableAddressesDisplay, cacheSizeDisplay;
	private JProgressBar cacheHitRateDisplay;
	private Animation animations;

	private JPanel logPanel;
	private JScrollPane logScroll;
	private JTextArea logText;
	private JCheckBox logShow;

	// Some GUI settings
	private final EmptyBorder emptyBorder = new EmptyBorder(4, 4, 4, 4);
	private final Font countFonts = new Font("Times", Font.BOLD, 12);
	private final Color backgroundColor = Color.WHITE;

	// Values for Combo Boxes
	private int[] cacheBlockSizeChoicesInt, cacheBlockCountChoicesInt;
	private final String[] cacheBlockSizeChoices = { "1", "2", "4", "8", "16", "32", "64", "128", "256", "512", "1024",
			"2048" };
	private final String[] cacheBlockCountChoices = { "1", "2", "4", "8", "16", "32", "64", "128", "256", "512", "1024",
			"2048" };
	private final String[] placementPolicyChoices = { "Direct Mapping", "Fully Associative", "N-way Set Associative" };
	private final int DIRECT = 0, FULL = 1, SET = 2; // NOTE: these have to match placementPolicyChoices order!
	private final String[] replacementPolicyChoices = { "LRU", "Random" };
	private final int LRU = 0, RANDOM = 1; // NOTE: these have to match replacementPolicyChoices order!
	private String[] cacheSetSizeChoices; // will change dynamically based on the other selections
	private final int defaultCacheBlockSizeIndex = 2;
	private final int defaultCacheBlockCountIndex = 3;
	private final int defaultPlacementPolicyIndex = DIRECT;
	private final int defaultReplacementPolicyIndex = LRU;
	private final int defaultCacheSetSizeIndex = 0;

	// Cache-related data structures
	private AbstractCache theCache;
	private int memoryAccessCount, cacheHitCount, cacheMissCount;
	private double cacheHitRate;

	// RNG used for random replacement policy.  For testing, set seed for reproducible stream
	private final Random randu = new Random(0);

	/**
	 * Simple constructor, likely used to run a stand-alone cache simulator.
	 *
	 * @param title   String containing title for title bar
	 * @param heading String containing text for heading shown in upper part of
	 *                window.
	 */
	public CacheSimulator(final String title, final String heading) {
		super(title, heading);
	}

	/**
	 * Simple constructor, likely used by the MARS Tools menu mechanism
	 */
	public CacheSimulator() {
		super("Data Cache Simulation Tool, " + version, heading);
	}

	/**
	 * Main provided for pure stand-alone use. Recommended stand-alone use is to
	 * write a driver program that instantiates a CacheSimulator object then invokes
	 * its go() method. "stand-alone" means it is not invoked from the MARS Tools
	 * menu. "Pure" means there is no driver program to invoke the Cache Simulator.
	 */
	public static void main(final String[] args) {
		new CacheSimulator("Data Cache Simulator stand-alone, " + version, heading).go();
	}

	/**
	 * Required MarsTool method to return Tool name.
	 *
	 * @return Tool name. MARS will display this in menu item.
	 */
	@Override
	public String getName() { return "Data Cache Simulator"; }

	/**
	 * Method that constructs the main cache simulator display area. It is organized
	 * vertically into three major components: the cache configuration which an be
	 * modified using combo boxes, the cache performance which is updated as the
	 * attached MIPS program executes, and the runtime log which is optionally used
	 * to display log of each cache access.
	 *
	 * @return the GUI component containing these three areas
	 */
	@Override
	protected JComponent buildMainDisplayArea() {
		// OVERALL STRUCTURE OF MAIN UI (CENTER)
		final Box results = Box.createVerticalBox();
		results.add(buildOrganizationArea());
		results.add(buildPerformanceArea());
		results.add(buildLogArea());
		return results;
	}

	////////////////////////////////////////////////////////////////////////////
	private JComponent buildLogArea() {
		logPanel = new JPanel();
		final TitledBorder ltb = new TitledBorder("Runtime Log");
		ltb.setTitleJustification(TitledBorder.CENTER);
		logPanel.setBorder(ltb);
		logShow = new JCheckBox("Enabled", debug);
		logShow.addItemListener(e -> {
			debug = e.getStateChange() == ItemEvent.SELECTED;
			resetLogDisplay();
			logText.setEnabled(debug);
			logText.setBackground(debug ? Color.WHITE : logPanel.getBackground());
		});
		logPanel.add(logShow);
		logText = new JTextArea(5, 70);
		logText.setEnabled(debug);
		logText.setBackground(debug ? Color.WHITE : logPanel.getBackground());
		logText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		logText.setToolTipText("Displays cache activity log if enabled");
		logScroll = new JScrollPane(logText, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		logPanel.add(logScroll);
		return logPanel;
	}

	////////////////////////////////////////////////////////////////////////////
	private JComponent buildOrganizationArea() {
		final JPanel organization = new JPanel(new GridLayout(3, 2));
		final TitledBorder otb = new TitledBorder("Cache Organization");
		otb.setTitleJustification(TitledBorder.CENTER);
		organization.setBorder(otb);
		cachePlacementSelector = new JComboBox(placementPolicyChoices);
		cachePlacementSelector.setEditable(false);
		cachePlacementSelector.setBackground(backgroundColor);
		cachePlacementSelector.setSelectedIndex(defaultPlacementPolicyIndex);
		cachePlacementSelector.addActionListener(e -> {
			updateCacheSetSizeSelector();
			reset();
		});

		cacheReplacementSelector = new JComboBox(replacementPolicyChoices);
		cacheReplacementSelector.setEditable(false);
		cacheReplacementSelector.setBackground(backgroundColor);
		cacheReplacementSelector.setSelectedIndex(defaultReplacementPolicyIndex);

		cacheBlockSizeSelector = new JComboBox(cacheBlockSizeChoices);
		cacheBlockSizeSelector.setEditable(false);
		cacheBlockSizeSelector.setBackground(backgroundColor);
		cacheBlockSizeSelector.setSelectedIndex(defaultCacheBlockSizeIndex);
		cacheBlockSizeSelector.addActionListener(e -> {
			updateCacheSizeDisplay();
			reset();
		});
		cacheBlockCountSelector = new JComboBox(cacheBlockCountChoices);
		cacheBlockCountSelector.setEditable(false);
		cacheBlockCountSelector.setBackground(backgroundColor);
		cacheBlockCountSelector.setSelectedIndex(defaultCacheBlockCountIndex);
		cacheBlockCountSelector.addActionListener(e -> {
			updateCacheSetSizeSelector();
			theCache = createNewCache();
			resetCounts();
			updateDisplay();
			updateCacheSizeDisplay();
			animations.fillAnimationBoxWithCacheBlocks();
		});

		cacheSetSizeSelector = new JComboBox(cacheSetSizeChoices);
		cacheSetSizeSelector.setEditable(false);
		cacheSetSizeSelector.setBackground(backgroundColor);
		cacheSetSizeSelector.setSelectedIndex(defaultCacheSetSizeIndex);
		cacheSetSizeSelector.addActionListener(e -> reset());

		// ALL COMPONENTS FOR "CACHE ORGANIZATION" SECTION
		final JPanel placementPolicyRow = getPanelWithBorderLayout();
		placementPolicyRow.setBorder(emptyBorder);
		placementPolicyRow.add(new JLabel("Placement Policy "), BorderLayout.WEST);
		placementPolicyRow.add(cachePlacementSelector, BorderLayout.EAST);

		final JPanel replacementPolicyRow = getPanelWithBorderLayout();
		replacementPolicyRow.setBorder(emptyBorder);
		replacementPolicyRow.add(new JLabel("Block Replacement Policy "), BorderLayout.WEST);
		/*       replacementPolicyDisplay = new JTextField("N/A",6);
		 replacementPolicyDisplay.setEditable(false);
		 replacementPolicyDisplay.setBackground(backgroundColor);
		 replacementPolicyDisplay.setHorizontalAlignment(JTextField.RIGHT); */
		replacementPolicyRow.add(cacheReplacementSelector, BorderLayout.EAST);

		final JPanel cacheSetSizeRow = getPanelWithBorderLayout();
		cacheSetSizeRow.setBorder(emptyBorder);
		cacheSetSizeRow.add(new JLabel("Set size (blocks) "), BorderLayout.WEST);
		cacheSetSizeRow.add(cacheSetSizeSelector, BorderLayout.EAST);

		// Cachable address range "selection" removed for now...
		/*
		 JPanel cachableAddressesRow = getPanelWithBorderLayout();
		 cachableAddressesRow.setBorder(emptyBorder);
		 cachableAddressesRow.add(new JLabel("Cachable Addresses "),BorderLayout.WEST);
		 cachableAddressesDisplay = new JTextField("all data segment");
		 cachableAddressesDisplay.setEditable(false);
		 cachableAddressesDisplay.setBackground(backgroundColor);
		 cachableAddressesDisplay.setHorizontalAlignment(JTextField.RIGHT);
		 cachableAddressesRow.add(cachableAddressesDisplay, BorderLayout.EAST);
		*/
		final JPanel cacheNumberBlocksRow = getPanelWithBorderLayout();
		cacheNumberBlocksRow.setBorder(emptyBorder);
		cacheNumberBlocksRow.add(new JLabel("Number of blocks "), BorderLayout.WEST);
		cacheNumberBlocksRow.add(cacheBlockCountSelector, BorderLayout.EAST);

		final JPanel cacheBlockSizeRow = getPanelWithBorderLayout();
		cacheBlockSizeRow.setBorder(emptyBorder);
		cacheBlockSizeRow.add(new JLabel("Cache block size (words) "), BorderLayout.WEST);
		cacheBlockSizeRow.add(cacheBlockSizeSelector, BorderLayout.EAST);

		final JPanel cacheTotalSizeRow = getPanelWithBorderLayout();
		cacheTotalSizeRow.setBorder(emptyBorder);
		cacheTotalSizeRow.add(new JLabel("Cache size (bytes) "), BorderLayout.WEST);
		cacheSizeDisplay = new JTextField(8);
		cacheSizeDisplay.setHorizontalAlignment(SwingConstants.RIGHT);
		cacheSizeDisplay.setEditable(false);
		cacheSizeDisplay.setBackground(backgroundColor);
		cacheSizeDisplay.setFont(countFonts);
		cacheTotalSizeRow.add(cacheSizeDisplay, BorderLayout.EAST);
		updateCacheSizeDisplay();

		// Lay 'em out in the grid...
		organization.add(placementPolicyRow);
		organization.add(cacheNumberBlocksRow);
		organization.add(replacementPolicyRow);
		organization.add(cacheBlockSizeRow);
		//organization.add(cachableAddressesRow);
		organization.add(cacheSetSizeRow);
		organization.add(cacheTotalSizeRow);
		return organization;
	}

	////////////////////////////////////////////////////////////////////////////
	private JComponent buildPerformanceArea() {
		final JPanel performance = new JPanel(new GridLayout(1, 2));
		final TitledBorder ptb = new TitledBorder("Cache Performance");
		ptb.setTitleJustification(TitledBorder.CENTER);
		performance.setBorder(ptb);
		final JPanel memoryAccessCountRow = getPanelWithBorderLayout();
		memoryAccessCountRow.setBorder(emptyBorder);
		memoryAccessCountRow.add(new JLabel("Memory Access Count "), BorderLayout.WEST);
		memoryAccessCountDisplay = new JTextField(10);
		memoryAccessCountDisplay.setHorizontalAlignment(SwingConstants.RIGHT);
		memoryAccessCountDisplay.setEditable(false);
		memoryAccessCountDisplay.setBackground(backgroundColor);
		memoryAccessCountDisplay.setFont(countFonts);
		memoryAccessCountRow.add(memoryAccessCountDisplay, BorderLayout.EAST);

		final JPanel cacheHitCountRow = getPanelWithBorderLayout();
		cacheHitCountRow.setBorder(emptyBorder);
		cacheHitCountRow.add(new JLabel("Cache Hit Count "), BorderLayout.WEST);
		cacheHitCountDisplay = new JTextField(10);
		cacheHitCountDisplay.setHorizontalAlignment(SwingConstants.RIGHT);
		cacheHitCountDisplay.setEditable(false);
		cacheHitCountDisplay.setBackground(backgroundColor);
		cacheHitCountDisplay.setFont(countFonts);
		cacheHitCountRow.add(cacheHitCountDisplay, BorderLayout.EAST);

		final JPanel cacheMissCountRow = getPanelWithBorderLayout();
		cacheMissCountRow.setBorder(emptyBorder);
		cacheMissCountRow.add(new JLabel("Cache Miss Count "), BorderLayout.WEST);
		cacheMissCountDisplay = new JTextField(10);
		cacheMissCountDisplay.setHorizontalAlignment(SwingConstants.RIGHT);
		cacheMissCountDisplay.setEditable(false);
		cacheMissCountDisplay.setBackground(backgroundColor);
		cacheMissCountDisplay.setFont(countFonts);
		cacheMissCountRow.add(cacheMissCountDisplay, BorderLayout.EAST);

		final JPanel cacheHitRateRow = getPanelWithBorderLayout();
		cacheHitRateRow.setBorder(emptyBorder);
		cacheHitRateRow.add(new JLabel("Cache Hit Rate "), BorderLayout.WEST);
		cacheHitRateDisplay = new JProgressBar(SwingConstants.HORIZONTAL, 0, 100);
		cacheHitRateDisplay.setStringPainted(true);
		cacheHitRateDisplay.setForeground(Color.BLUE);
		cacheHitRateDisplay.setBackground(backgroundColor);
		cacheHitRateDisplay.setFont(countFonts);
		cacheHitRateRow.add(cacheHitRateDisplay, BorderLayout.EAST);

		resetCounts();
		updateDisplay();

		// Vertically align these 4 measures in a grid, then add to left column of main grid.
		final JPanel performanceMeasures = new JPanel(new GridLayout(4, 1));
		performanceMeasures.add(memoryAccessCountRow);
		performanceMeasures.add(cacheHitCountRow);
		performanceMeasures.add(cacheMissCountRow);
		performanceMeasures.add(cacheHitRateRow);
		performance.add(performanceMeasures);

		// LET'S TRY SOME ANIMATION ON THE RIGHT SIDE...
		animations = new Animation();
		animations.fillAnimationBoxWithCacheBlocks();
		final JPanel animationsPanel = new JPanel(new GridLayout(1, 2));
		final Box animationsLabel = Box.createVerticalBox();
		final JPanel tableTitle1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JPanel tableTitle2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		tableTitle1.add(new JLabel("Cache Block Table"));
		tableTitle2.add(new JLabel("(block 0 at top)"));
		animationsLabel.add(tableTitle1);
		animationsLabel.add(tableTitle2);
		final Dimension colorKeyBoxSize = new Dimension(8, 8);

		final JPanel emptyKey = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JPanel emptyBox = new JPanel();
		emptyBox.setSize(colorKeyBoxSize);
		emptyBox.setBackground(animations.defaultColor);
		emptyBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		emptyKey.add(emptyBox);
		emptyKey.add(new JLabel(" = empty"));

		final JPanel missBox = new JPanel();
		final JPanel missKey = new JPanel(new FlowLayout(FlowLayout.LEFT));
		missBox.setSize(colorKeyBoxSize);
		missBox.setBackground(animations.missColor);
		missBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		missKey.add(missBox);
		missKey.add(new JLabel(" = miss"));

		final JPanel hitKey = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JPanel hitBox = new JPanel();
		hitBox.setSize(colorKeyBoxSize);
		hitBox.setBackground(animations.hitColor);
		hitBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		hitKey.add(hitBox);
		hitKey.add(new JLabel(" = hit"));

		animationsLabel.add(emptyKey);
		animationsLabel.add(hitKey);
		animationsLabel.add(missKey);
		animationsLabel.add(Box.createVerticalGlue());
		animationsPanel.add(animationsLabel);
		animationsPanel.add(animations.getAnimationBox());

		performance.add(animationsPanel);
		return performance;
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//  Rest of the protected methods.  These override do-nothing methods inherited from
	//  the abstract superclass.
	//////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Apply caching policies and update display when connected MIPS program
	 * accesses (data) memory.
	 *
	 * @param memory       the attached memory
	 * @param accessNotice information provided by memory in MemoryAccessNotice
	 *                     object
	 */
	@Override
	protected void processMIPSUpdate(final Observable memory, final AccessNotice accessNotice) {
		final MemoryAccessNotice notice = (MemoryAccessNotice) accessNotice;
		memoryAccessCount++;
		final CacheAccessResult cacheAccessResult = theCache.isItAHitThenReadOnMiss(notice.getAddress());
		if (cacheAccessResult.isHit()) {
			cacheHitCount++;
			animations.showHit(cacheAccessResult.getBlock());
		} else {
			cacheMissCount++;
			animations.showMiss(cacheAccessResult.getBlock());
		}
		cacheHitRate = cacheHitCount / (double) memoryAccessCount;
	}

	/**
	 * Initialize all JComboBox choice structures not already initialized at
	 * declaration. Also creates initial default cache object. Overrides inherited
	 * method that does nothing.
	 */
	@Override
	protected void initializePreGUI() {
		cacheBlockSizeChoicesInt = new int[cacheBlockSizeChoices.length];
		for (int i = 0; i < cacheBlockSizeChoices.length; i++) {
			try {
				cacheBlockSizeChoicesInt[i] = Integer.parseInt(cacheBlockSizeChoices[i]);
			} catch (final NumberFormatException nfe) {
				cacheBlockSizeChoicesInt[i] = 1;
			}
		}
		cacheBlockCountChoicesInt = new int[cacheBlockCountChoices.length];
		for (int i = 0; i < cacheBlockCountChoices.length; i++) {
			try {
				cacheBlockCountChoicesInt[i] = Integer.parseInt(cacheBlockCountChoices[i]);
			} catch (final NumberFormatException nfe) {
				cacheBlockCountChoicesInt[i] = 1;
			}
		}
		cacheSetSizeChoices = determineSetSizeChoices(defaultCacheBlockCountIndex, defaultPlacementPolicyIndex);
	}

	/**
	 * The only post-GUI initialization is to create the initial cache object based
	 * on the default settings of the various combo boxes. Overrides inherited
	 * method that does nothing.
	 */

	@Override
	protected void initializePostGUI() {
		theCache = createNewCache();
	}

	/**
	 * Method to reset cache, counters and display when the Reset button selected.
	 * Overrides inherited method that does nothing.
	 */
	@Override
	protected void reset() {
		theCache = createNewCache();
		resetCounts();
		updateDisplay();
		animations.reset();
		resetLogDisplay();
	}

	/**
	 * Updates display immediately after each update (AccessNotice) is processed,
	 * after cache configuration changes as needed, and after each execution step
	 * when Mars is running in timed mode. Overrides inherited method that does
	 * nothing.
	 */
	@Override
	protected void updateDisplay() {
		updateMemoryAccessCountDisplay();
		updateCacheHitCountDisplay();
		updateCacheMissCountDisplay();
		updateCacheHitRateDisplay();
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//  Private methods defined to support the above.
	//////////////////////////////////////////////////////////////////////////////////////

	// Will determine range of choices for "set size in blocks", which is determined both by
	// the number of blocks in the cache and by placement policy.
	private String[] determineSetSizeChoices(final int cacheBlockCountIndex, final int placementPolicyIndex) {
		String[] choices;
		final int firstBlockCountIndex = 0;
		final int lastBlockCountIndex = cacheBlockCountIndex;
		switch (placementPolicyIndex) {
		case DIRECT:
			choices = new String[1];
			choices[0] = cacheBlockCountChoices[firstBlockCountIndex]; // set size fixed at 1
			break;
		case SET:
			choices = new String[lastBlockCountIndex - firstBlockCountIndex + 1];
			for (int i = 0; i < choices.length; i++) {
				choices[i] = cacheBlockCountChoices[firstBlockCountIndex + i];
			}
			break;
		case FULL:   // 1 set total, so set size fixed at current number of blocks
		default:
			choices = new String[1];
			choices[0] = cacheBlockCountChoices[lastBlockCountIndex];
		}
		return choices;
	}

	// Update the Set Size combo box selection in response to other selections..
	private void updateCacheSetSizeSelector() {
		cacheSetSizeSelector.setModel(new DefaultComboBoxModel(determineSetSizeChoices(cacheBlockCountSelector
				.getSelectedIndex(), cachePlacementSelector.getSelectedIndex())));
	}

	// create and return a new cache object based on current specs
	private AbstractCache createNewCache() {
		AbstractCache theNewCache = null;
		int setSize = 1;
		try {
			setSize = Integer.parseInt((String) cacheSetSizeSelector.getSelectedItem());
		} catch (final NumberFormatException nfe) { // if this happens its my fault!
		}
		theNewCache = new AnyCache(cacheBlockCountChoicesInt[cacheBlockCountSelector.getSelectedIndex()],
				cacheBlockSizeChoicesInt[cacheBlockSizeSelector.getSelectedIndex()], setSize);
		return theNewCache;
	}

	private void resetCounts() {
		memoryAccessCount = 0;
		cacheHitCount = 0;
		cacheMissCount = 0;
		cacheHitRate = 0.0;
	}

	private void updateMemoryAccessCountDisplay() {
		memoryAccessCountDisplay.setText(new Integer(memoryAccessCount).toString());
	}

	private void updateCacheHitCountDisplay() {
		cacheHitCountDisplay.setText(new Integer(cacheHitCount).toString());
	}

	private void updateCacheMissCountDisplay() {
		cacheMissCountDisplay.setText(new Integer(cacheMissCount).toString());
	}

	private void updateCacheHitRateDisplay() {
		cacheHitRateDisplay.setValue((int) Math.round(cacheHitRate * 100));
	}

	private void updateCacheSizeDisplay() {
		final int cacheSize = cacheBlockSizeChoicesInt[cacheBlockSizeSelector.getSelectedIndex()]
				* cacheBlockCountChoicesInt[cacheBlockCountSelector.getSelectedIndex()] * Memory.WORD_LENGTH_BYTES;
		cacheSizeDisplay.setText(Integer.toString(cacheSize));
	}

	private JPanel getPanelWithBorderLayout() { return new JPanel(new BorderLayout(2, 2)); }

	private void resetLogDisplay() {
		logText.setText("");
	}

	private void writeLog(final String text) {
		logText.append(text);
		logText.setCaretPosition(logText.getDocument().getLength());
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//  Specialized inner classes for cache modeling and animation.
	//////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////////////////////////////////////////////////////
	// Represents a block in the cache.  Since we are only simulating
	// cache performance, there's no need to actually store memory contents.
	private class CacheBlock {

		private boolean valid;
		private int tag;
		private final int sizeInWords;
		private int mostRecentAccessTime;

		public CacheBlock(final int sizeInWords) {
			valid = false;
			tag = 0;
			this.sizeInWords = sizeInWords;
			mostRecentAccessTime = -1;
		}
	}

	/////////////////////////////////////////////////////////////////////////
	// Represents the outcome of a cache access.  There are two parts:
	// whether it was a hit or not, and in which block is the value stored.
	// In the case of a hit, the block associated with address.  In the case of
	// a miss, the block where new association is made.	DPS 23-Dec-2010
	private class CacheAccessResult {

		private final boolean hitOrMiss;
		private final int blockNumber;

		public CacheAccessResult(final boolean hitOrMiss, final int blockNumber) {
			this.hitOrMiss = hitOrMiss;
			this.blockNumber = blockNumber;
		}

		public boolean isHit() { return hitOrMiss; }

		public int getBlock() { return blockNumber; }
	}

	//////////////////////////////////////////////////////////////////////
	// Abstract Cache class.  Subclasses will implement specific policies.
	private abstract class AbstractCache {

		private final int numberOfBlocks, blockSizeInWords, setSizeInBlocks, numberOfSets;
		protected CacheBlock[] blocks;

		protected AbstractCache(final int numberOfBlocks, final int blockSizeInWords, final int setSizeInBlocks) {
			this.numberOfBlocks = numberOfBlocks;
			this.blockSizeInWords = blockSizeInWords;
			this.setSizeInBlocks = setSizeInBlocks;
			numberOfSets = numberOfBlocks / setSizeInBlocks;
			blocks = new CacheBlock[numberOfBlocks];
			reset();
		}

		public int getNumberOfBlocks() { return numberOfBlocks; }

		public int getNumberOfSets() { return numberOfSets; }

		public int getSetSizeInBlocks() { return setSizeInBlocks; }

		public int getBlockSizeInWords() { return blockSizeInWords; }

		public int getCacheSizeInWords() { return numberOfBlocks * blockSizeInWords; }

		public int getCacheSizeInBytes() { return numberOfBlocks * blockSizeInWords * Memory.WORD_LENGTH_BYTES; }

		// This will work regardless of placement.
		// For direct map, #sets==#blocks
		// For full assoc, #sets==1 so anything % #sets == 0
		// For n-way assoc, it extracts the set bits in address.
		public int getSetNumber(final int address) {
			return address / Memory.WORD_LENGTH_BYTES / blockSizeInWords % numberOfSets;
		}

		// This will work regardless of placement policy (direct map, n-way or full assoc)
		public int getTag(final int address) {
			return address / Memory.WORD_LENGTH_BYTES / blockSizeInWords / numberOfSets;
		}

		// This will work regardless of placement policy (direct map, n-way or full assoc)
		// Returns absolute block offset into the cache.
		public int getFirstBlockToSearch(final int address) {
			return getSetNumber(address) * setSizeInBlocks;
		}

		// This will work regardless of placement policy (direct map, n-way or full assoc)
		// Returns absolute block offset into the cache.
		public int getLastBlockToSearch(final int address) {
			return getFirstBlockToSearch(address) + setSizeInBlocks - 1;
		}

		/* Reset the cache contents. */
		public void reset() {
			for (int i = 0; i < numberOfBlocks; i++) {
				blocks[i] = new CacheBlock(blockSizeInWords);
			}
			System.gc(); // scoop 'em up now
		}

		// Subclass must implement this according to its policies
		public abstract CacheAccessResult isItAHitThenReadOnMiss(int address);
	}

	//////////////////////////////////////////////////////////////////////////////
	// Implements any of the well-known cache organizations.  Physical memory
	// address is partitioned depending on organization:
	//    Direct Mapping:    [ tag | block | word | byte ]
	//    Fully Associative: [ tag | word | byte ]
	//    Set Associative:   [ tag | set | word | byte ]
	//
	// Bit lengths of each part are determined as follows:
	// Direct Mapping:
	//   byte  = log2 of #bytes in a word (typically 4)
	//   word  = log2 of #words in a block
	//   block = log2 of #blocks in the cache
	//   tag   = #bytes in address - (byte+word+block)
	// Fully Associative:
	//   byte  = log2 of #bytes in a word (typically 4)
	//   word  = log2 of #words in a block
	//   tag   = #bytes in address - (byte+word)
	// Set Associative:
	//   byte  = log2 of #bytes in a word (typically 4)
	//   word  = log2 of #words in a block
	//   set   = log2 of #sets in the cache
	//   tag   = #bytes in address - (byte+word+set)
	//
	// Direct Mapping (1 way set associative):
	// The block value for a given address identifies its block index into the cache.
	// That's why its called "direct mapped."  This is the only cache block it can
	// occupy.  If that cache block is empty or if it is occupied by a different tag,
	// this is a MISS.  If that cache block is occupied by the same tag, this is a HIT.
	// There is no replacement policy: upon a cache miss of an occupied block, the old
	// block is written out (unless write-through) and the new one read in.
	// Those actions are not simulated here.
	//
	// Fully Associative:
	// There is one set, and very tag has to be searched before determining hit or miss.
	// If tag is matched, it is a hit.  If tag is not matched and there is at least one
	// empty block, it is a miss and the new tag will occupy it.  If tag is not matched
	// and every block is occupied, it is a miss and one of the occupied blocks will be
	// selected for removal and the new tag will replace it.
	//
	// n-way Set Associative:
	// Each set consists of n blocks, and the number of sets in the cache is total number
	// of blocks divided by n.  The set bits in the address will identify which set to
	// search, and every tag in that set has to be searched before determining hit or miss.
	// If tag is matched, it is a hit.  If tag is not matched and there is at least one
	// empty block, it is a miss and the new tag will occupy it.  If tag is not matched
	// and every block is occupied, it is a miss and one of the occupied blocks will be
	// selected for removal and the new tag will replace it.
	//
	private class AnyCache extends AbstractCache {

		public AnyCache(final int numberOfBlocks, final int blockSizeInWords, final int setSizeInBlocks) {
			super(numberOfBlocks, blockSizeInWords, setSizeInBlocks);
		}

		private final int SET_FULL = 0, HIT = 1, MISS = 2;

		// This method works for any of the placement policies:
		// direct mapped, full associative or n-way set associative.
		@Override
		public CacheAccessResult isItAHitThenReadOnMiss(final int address) {
			int result = SET_FULL;
			final int firstBlock = getFirstBlockToSearch(address);
			final int lastBlock = getLastBlockToSearch(address);
			if (debug) {
				writeLog("(" + memoryAccessCount + ") address: " + Binary.intToHexString(address) + " (tag " + Binary
						.intToHexString(getTag(address)) + ") " + " block range: " + firstBlock + "-" + lastBlock
						+ "\n");
			}
			CacheBlock block;
			int blockNumber = 0;
			// Will do a sequential instead of associative search!
			for (blockNumber = firstBlock; blockNumber <= lastBlock; blockNumber++) {
				block = blocks[blockNumber];
				if (debug) {
					writeLog("   trying block " + blockNumber + (block.valid ? " tag " + Binary.intToHexString(
							block.tag) : " empty"));
				}
				if (block.valid && block.tag == getTag(address)) {//  it's a hit!
					if (debug) { writeLog(" -- HIT\n"); }
					result = HIT;
					block.mostRecentAccessTime = memoryAccessCount;
					break;
				}
				if (!block.valid) {// it's a miss but I got it now because it is empty!
					if (debug) { writeLog(" -- MISS\n"); }
					result = MISS;
					block.valid = true;
					block.tag = getTag(address);
					block.mostRecentAccessTime = memoryAccessCount;
					break;
				}
				if (debug) { writeLog(" -- OCCUPIED\n"); }
			}
			if (result == SET_FULL) {
				//select one to replace and replace it...
				if (debug) { writeLog("   MISS due to FULL SET"); }
				final int blockToReplace = selectBlockToReplace(firstBlock, lastBlock);
				block = blocks[blockToReplace];
				block.tag = getTag(address);
				block.mostRecentAccessTime = memoryAccessCount;
				blockNumber = blockToReplace;
			}
			return new CacheAccessResult(result == HIT, blockNumber);
		}

		// call this if all blocks in the set are full.  If the set contains more than one block,
		// It will pick on to replace based on selected replacement policy.
		private int selectBlockToReplace(final int first, final int last) {
			int replaceBlock = first;
			if (first != last) {
				switch (cacheReplacementSelector.getSelectedIndex()) {
				case RANDOM:
					replaceBlock = first + randu.nextInt(last - first + 1);
					if (debug) { writeLog(" -- Random replace block " + replaceBlock + "\n"); }
					break;
				case LRU:
				default:
					int leastRecentAccessTime = memoryAccessCount; // all of them have to be less than this
					for (int block = first; block <= last; block++) {
						if (blocks[block].mostRecentAccessTime < leastRecentAccessTime) {
							leastRecentAccessTime = blocks[block].mostRecentAccessTime;
							replaceBlock = block;
						}
					}
					if (debug) {
						writeLog(" -- LRU replace block " + replaceBlock + "; unused since (" + leastRecentAccessTime
								+ ")\n");
					}
					break;
				}
			}
			return replaceBlock;
		}
	}

	//////////////////////////////////////////////////////////////
	//  Class to display animated cache
	//
	private class Animation {

		private final Box animation;
		private JTextField[] blocks;
		public final Color hitColor = Color.GREEN;
		public final Color missColor = Color.RED;
		public final Color defaultColor = Color.WHITE;

		public Animation() {
			animation = Box.createVerticalBox();
		}

		private Box getAnimationBox() { return animation; }

		public int getNumberOfBlocks() { return blocks == null ? 0 : blocks.length; }

		public void showHit(final int blockNum) {
			blocks[blockNum].setBackground(hitColor);
		}

		public void showMiss(final int blockNum) {
			blocks[blockNum].setBackground(missColor);
		}

		public void reset() {
			for (int i = 0; i < blocks.length; i++) {
				blocks[i].setBackground(defaultColor);
			}
		}

		// initialize animation of cache blocks
		private void fillAnimationBoxWithCacheBlocks() {
			animation.setVisible(false);
			animation.removeAll();
			final int numberOfBlocks = cacheBlockCountChoicesInt[cacheBlockCountSelector.getSelectedIndex()];
			final int totalVerticalPixels = 128;
			final int blockPixelHeight = numberOfBlocks > totalVerticalPixels ? 1
					: totalVerticalPixels / numberOfBlocks;
			final int blockPixelWidth = 40;
			final Dimension blockDimension = new Dimension(blockPixelWidth, blockPixelHeight);
			blocks = new JTextField[numberOfBlocks];
			for (int i = 0; i < numberOfBlocks; i++) {
				blocks[i] = new JTextField();
				blocks[i].setEditable(false);
				blocks[i].setBackground(defaultColor);
				blocks[i].setSize(blockDimension);
				blocks[i].setPreferredSize(blockDimension);
				animation.add(blocks[i]);
			}
			animation.repaint();
			animation.setVisible(true);
		}

	}

}
