package mars;

import java.awt.Color;
import java.awt.Font;
import java.util.Observable;
import java.util.StringTokenizer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import mars.util.Binary;
import mars.util.EditorFont;
import mars.venus.editors.jeditsyntax.SyntaxStyle;
import mars.venus.editors.jeditsyntax.SyntaxUtilities;
import mars.venus.editors.jeditsyntax.tokenmarker.Token;

/*
 * Copyright (c) 2003-2013, Pete Sanderson and Kenneth Vollmar
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
 * Contains various IDE settings. Persistent settings are maintained for the
 * current user and on the current machine using Java's Preference objects.
 * Failing that, default setting values come from Settings.properties file. If
 * both of those fail, default values come from static arrays defined in this
 * class. The latter can can be modified prior to instantiating Settings object.
 * NOTE: If the Preference objects fail due to security exceptions, changes to
 * settings will not carry over from one MARS session to the next. Actual
 * implementation of the Preference objects is platform-dependent. For Windows,
 * they are stored in Registry. To see, run regedit and browse to:
 * HKEY_CURRENT_USER\Software\JavaSoft\Prefs\mars
 *
 * @author Pete Sanderson
 **/

public class Settings extends Observable {

	/* Properties file used to hold default settings. */
	private static String settingsFile = "Settings";
	/////////////////////////////  PROPERTY ARRAY INDEXES /////////////////////////////
	// Because MARS is programmed to Java 1.4, we cannot use an enumerated type.

	// BOOLEAN SETTINGS...
	/**
	 * Flag to determine whether or not program being assembled is limited to basic
	 * MIPS instructions and formats.
	 */
	public static final int EXTENDED_ASSEMBLER_ENABLED = 0;
	/**
	 * Flag to determine whether or not program being assembled is limited to using
	 * register numbers instead of names. NOTE: Its default value is false and the
	 * IDE provides no means to change it!
	 */
	public static final int BARE_MACHINE_ENABLED = 1;
	/**
	 * Flag to determine whether or not a file is immediately and automatically
	 * assembled upon opening. Handy when using externa editor like mipster.
	 */
	public static final int ASSEMBLE_ON_OPEN_ENABLED = 2;
	/**
	 * Flag to determine whether only the current editor source file (enabled false)
	 * or all files in its directory (enabled true) will be assembled when assembly
	 * is selected.
	 */
	public static final int ASSEMBLE_ALL_ENABLED = 3;
	/**
	 * Default visibilty of label window (symbol table). Default only, dynamic
	 * status maintained by ExecutePane
	 */
	public static final int LABEL_WINDOW_VISIBILITY = 4;
	/**
	 * Default setting for displaying addresses and values in hexidecimal in the
	 * Execute pane.
	 */
	public static final int DISPLAY_ADDRESSES_IN_HEX = 5;
	public static final int DISPLAY_VALUES_IN_HEX = 6;
	/**
	 * Flag to determine whether the currently selected exception handler source
	 * file will be included in each assembly operation.
	 */
	public static final int EXCEPTION_HANDLER_ENABLED = 7;
	/**
	 * Flag to determine whether or not delayed branching is in effect at MIPS
	 * execution. This means we simulate the pipeline and statement FOLLOWING a
	 * successful branch is executed before branch is taken. DPS 14 June 2007.
	 */
	public static final int DELAYED_BRANCHING_ENABLED = 8;
	/** Flag to determine whether or not the editor will display line numbers. */
	public static final int EDITOR_LINE_NUMBERS_DISPLAYED = 9;
	/**
	 * Flag to determine whether or not assembler warnings are considered errors.
	 */
	public static final int WARNINGS_ARE_ERRORS = 10;
	/** Flag to determine whether or not to display and use program arguments */
	public static final int PROGRAM_ARGUMENTS = 11;
	/**
	 * Flag to control whether or not highlighting is applied to data segment window
	 */
	public static final int DATA_SEGMENT_HIGHLIGHTING = 12;
	/**
	 * Flag to control whether or not highlighting is applied to register windows
	 */
	public static final int REGISTERS_HIGHLIGHTING = 13;
	/**
	 * Flag to control whether or not assembler automatically initializes program
	 * counter to 'main's address
	 */
	public static final int START_AT_MAIN = 14;
	/**
	 * Flag to control whether or not editor will highlight the line currently being
	 * edited
	 */
	public static final int EDITOR_CURRENT_LINE_HIGHLIGHTING = 15;
	/**
	 * Flag to control whether or not editor will provide popup instruction guidance
	 * while typing
	 */
	public static final int POPUP_INSTRUCTION_GUIDANCE = 16;
	/**
	 * Flag to control whether or not simulator will use popup dialog for input
	 * syscalls
	 */
	public static final int POPUP_SYSCALL_INPUT = 17;
	/**
	 * Flag to control whether or not to use generic text editor instead of
	 * language-aware styled editor
	 */
	public static final int GENERIC_TEXT_EDITOR = 18;
	/**
	 * Flag to control whether or not language-aware editor will use auto-indent
	 * feature
	 */
	public static final int AUTO_INDENT = 19;
	/**
	 * Flag to determine whether a program can write binary code to the text or data
	 * segment and execute that code.
	 */
	public static final int SELF_MODIFYING_CODE_ENABLED = 20;

	// NOTE: key sequence must match up with labels above which are used for array indexes!
	private static String[] booleanSettingsKeys = { "ExtendedAssembler", "BareMachine", "AssembleOnOpen", "AssembleAll", "LabelWindowVisibility", "DisplayAddressesInHex", "DisplayValuesInHex", "LoadExceptionHandler", "DelayedBranching", "EditorLineNumbersDisplayed", "WarningsAreErrors", "ProgramArguments", "DataSegmentHighlighting", "RegistersHighlighting", "StartAtMain", "EditorCurrentLineHighlighting", "PopupInstructionGuidance", "PopupSyscallInput", "GenericTextEditor", "AutoIndent", "SelfModifyingCode" };

	/**
	 * Last resort default values for boolean settings; will use only if neither the
	 * Preferences nor the properties file work. If you wish to change them, do so
	 * before instantiating the Settings object. Values are matched to keys by list
	 * position.
	 */
	public static boolean[] defaultBooleanSettingsValues = { // match the above list by position
			true, false, false, false, false, true, true, false, false, true, false, false, true, true, false, true, true, false, false, true, false };

	// STRING SETTINGS.  Each array position has associated name.
	/** Current specified exception handler file (a MIPS assembly source file) */
	public static final int EXCEPTION_HANDLER = 0;
	/** Order of text segment table columns */
	public static final int TEXT_COLUMN_ORDER = 1;
	/** State for sorting label window display */
	public static final int LABEL_SORT_STATE = 2;
	/** Identifier of current memory configuration */
	public static final int MEMORY_CONFIGURATION = 3;
	/** Caret blink rate in milliseconds, 0 means don't blink. */
	public static final int CARET_BLINK_RATE = 4;
	/** Editor tab size in characters. */
	public static final int EDITOR_TAB_SIZE = 5;
	/**
	 * Number of letters to be matched by editor's instruction guide before popup
	 * generated (if popup enabled)
	 */
	public static final int EDITOR_POPUP_PREFIX_LENGTH = 6;
	// Match the above by position.
	private static final String[] stringSettingsKeys = { "ExceptionHandler", "TextColumnOrder", "LabelSortState", "MemoryConfiguration", "CaretBlinkRate", "EditorTabSize", "EditorPopupPrefixLength" };

	/**
	 * Last resort default values for String settings; will use only if neither the
	 * Preferences nor the properties file work. If you wish to change, do so before
	 * instantiating the Settings object. Must match key by list position.
	 */
	private static String[] defaultStringSettingsValues = { "", "0 1 2 3 4", "0", "", "500", "8", "2" };

	// FONT SETTINGS.  Each array position has associated name.
	/** Font for the text editor */
	public static final int EDITOR_FONT = 0;
	/** Font for table even row background (text, data, register displays) */
	public static final int EVEN_ROW_FONT = 1;
	/** Font for table odd row background (text, data, register displays) */
	public static final int ODD_ROW_FONT = 2;
	/** Font for table odd row foreground (text, data, register displays) */
	public static final int TEXTSEGMENT_HIGHLIGHT_FONT = 3;
	/** Font for text segment delay slot highlighted background */
	public static final int TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_FONT = 4;
	/** Font for text segment highlighted background */
	public static final int DATASEGMENT_HIGHLIGHT_FONT = 5;
	/** Font for register highlighted background */
	public static final int REGISTER_HIGHLIGHT_FONT = 6;

	private static final String[] fontFamilySettingsKeys = { "EditorFontFamily", "EvenRowFontFamily", "OddRowFontFamily", " TextSegmentHighlightFontFamily", "TextSegmentDelayslotHighightFontFamily", "DataSegmentHighlightFontFamily", "RegisterHighlightFontFamily" };
	private static final String[] fontStyleSettingsKeys = { "EditorFontStyle", "EvenRowFontStyle", "OddRowFontStyle", " TextSegmentHighlightFontStyle", "TextSegmentDelayslotHighightFontStyle", "DataSegmentHighlightFontStyle", "RegisterHighlightFontStyle" };
	private static final String[] fontSizeSettingsKeys = { "EditorFontSize", "EvenRowFontSize", "OddRowFontSize", " TextSegmentHighlightFontSize", "TextSegmentDelayslotHighightFontSize", "DataSegmentHighlightFontSize", "RegisterHighlightFontSize" };

	/**
	 * Last resort default values for Font settings; will use only if neither the
	 * Preferences nor the properties file work. If you wish to change, do so before
	 * instantiating the Settings object. Must match key by list position shown
	 * above.
	 */

	// DPS 3-Oct-2012
	// Changed default font family from "Courier New" to "Monospaced" after receiving reports that Mac were not
	// correctly rendering the left parenthesis character in the editor or text segment display.
	// See http://www.mirthcorp.com/community/issues/browse/MIRTH-1921?page=com.atlassian.jira.plugin.system.issuetabpanels:all-tabpanel
	private static final String[] defaultFontFamilySettingsValues = { "Monospaced", "Monospaced", "Monospaced", "Monospaced", "Monospaced", "Monospaced", "Monospaced" };
	private static final String[] defaultFontStyleSettingsValues = { "Plain", "Plain", "Plain", "Plain", "Plain", "Plain", "Plain" };
	private static final String[] defaultFontSizeSettingsValues = { "12", "12", "12", "12", "12", "12", "12", };

	// COLOR SETTINGS.  Each array position has associated name.
	/** RGB color for table even row background (text, data, register displays) */
	public static final int EVEN_ROW_BACKGROUND = 0;
	/** RGB color for table even row foreground (text, data, register displays) */
	public static final int EVEN_ROW_FOREGROUND = 1;
	/** RGB color for table odd row background (text, data, register displays) */
	public static final int ODD_ROW_BACKGROUND = 2;
	/** RGB color for table odd row foreground (text, data, register displays) */
	public static final int ODD_ROW_FOREGROUND = 3;
	/** RGB color for text segment highlighted background */
	public static final int TEXTSEGMENT_HIGHLIGHT_BACKGROUND = 4;
	/** RGB color for text segment highlighted foreground */
	public static final int TEXTSEGMENT_HIGHLIGHT_FOREGROUND = 5;
	/** RGB color for text segment delay slot highlighted background */
	public static final int TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_BACKGROUND = 6;
	/** RGB color for text segment delay slot highlighted foreground */
	public static final int TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_FOREGROUND = 7;
	/** RGB color for text segment highlighted background */
	public static final int DATASEGMENT_HIGHLIGHT_BACKGROUND = 8;
	/** RGB color for text segment highlighted foreground */
	public static final int DATASEGMENT_HIGHLIGHT_FOREGROUND = 9;
	/** RGB color for register highlighted background */
	public static final int REGISTER_HIGHLIGHT_BACKGROUND = 10;
	/** RGB color for register highlighted foreground */
	public static final int REGISTER_HIGHLIGHT_FOREGROUND = 11;
	// Match the above by position.
	private static final String[] colorSettingsKeys = { "EvenRowBackground", "EvenRowForeground", "OddRowBackground", "OddRowForeground", "TextSegmentHighlightBackground", "TextSegmentHighlightForeground", "TextSegmentDelaySlotHighlightBackground", "TextSegmentDelaySlotHighlightForeground", "DataSegmentHighlightBackground", "DataSegmentHighlightForeground", "RegisterHighlightBackground", "RegisterHighlightForeground" };
	/**
	 * Last resort default values for color settings; will use only if neither the
	 * Preferences nor the properties file work. If you wish to change, do so before
	 * instantiating the Settings object. Must match key by list position.
	 */
	private static String[] defaultColorSettingsValues = { "0x00e0e0e0", "0", "0x00ffffff", "0", "0x00ffff99", "0", "0x0033ff00", "0", "0x0099ccff", "0", "0x0099cc55", "0" };

	private final boolean[] booleanSettingsValues;
	private final String[] stringSettingsValues;
	private final String[] fontFamilySettingsValues;
	private final String[] fontStyleSettingsValues;
	private final String[] fontSizeSettingsValues;
	private final String[] colorSettingsValues;

	private final Preferences preferences;

	/**
	 * Create Settings object and set to saved values. If saved values not found,
	 * will set based on defaults stored in Settings.properties file. If file
	 * problems, will set based on defaults stored in this class.
	 */
	public Settings() { this(true); }

	/**
	 * Create Settings object and set to saved values. If saved values not found,
	 * will set based on defaults stored in Settings.properties file. If file
	 * problems, will set based on defaults stored in this class.
	 *
	 * @param gui true if running the graphical IDE, false if running from command
	 * line. Ignored as of release 3.6 but retained for compatability.
	 */

	public Settings(final boolean gui) {
		booleanSettingsValues = new boolean[booleanSettingsKeys.length];
		stringSettingsValues = new String[stringSettingsKeys.length];
		fontFamilySettingsValues = new String[fontFamilySettingsKeys.length];
		fontStyleSettingsValues = new String[fontStyleSettingsKeys.length];
		fontSizeSettingsValues = new String[fontSizeSettingsKeys.length];
		colorSettingsValues = new String[colorSettingsKeys.length];
		// This determines where the values are actually stored.  Actual implementation
		// is platform-dependent.  For Windows, they are stored in Registry.  To see,
		// run regedit and browse to: HKEY_CURRENT_USER\Software\JavaSoft\Prefs\mars
		preferences = Preferences.userNodeForPackage(this.getClass());
		// The gui parameter, formerly passed to initialize(), is no longer needed
		// because I removed (1/21/09) the call to generate the Font object for the text editor.
		// Font objects are now generated only on demand so the "if (gui)" guard
		// is no longer necessary.  Originally added by Berkeley b/c they were running it on a
		// headless server and running in command mode.  The Font constructor resulted in Swing
		// initialization which caused problems.  Now this will only occur on demand from
		// Venus, which happens only when running as GUI.
		initialize();
	}

	/**
	 * Return whether backstepping is permitted at this time. Backstepping is
	 * ability to undo execution steps one at a time. Available only in the IDE.
	 * This is not a persistent setting and is not under MARS user control.
	 *
	 * @return true if backstepping is permitted, false otherwise.
	 */
	public boolean getBackSteppingEnabled() {
		return Globals.program != null && Globals.program.getBackStepper() != null && Globals.program.getBackStepper()
			.enabled();
	}

	/**
	 * Reset settings to default values, as described in the constructor comments.
	 *
	 * @param gui true if running from GUI IDE and false if running from command
	 * mode. Ignored as of release 3.6 but retained for compatibility.
	 */
	public void reset(final boolean gui) { initialize(); }

	/* **************************************************************************
	This section contains all code related to syntax highlighting styles settings.
	A style includes 3 components: color, bold (t/f), italic (t/f)
	
	The fallback defaults will come not from an array here, but from the
	existing static method SyntaxUtilities.getDefaultSyntaxStyles()
	in the mars.venus.editors.jeditsyntax package.  It returns an array
	of SyntaxStyle objects.
	
	*/
	private String[] syntaxStyleColorSettingsValues;
	private boolean[] syntaxStyleBoldSettingsValues;
	private boolean[] syntaxStyleItalicSettingsValues;

	private static final String SYNTAX_STYLE_COLOR_PREFIX = "SyntaxStyleColor_";
	private static final String SYNTAX_STYLE_BOLD_PREFIX = "SyntaxStyleBold_";
	private static final String SYNTAX_STYLE_ITALIC_PREFIX = "SyntaxStyleItalic_";

	private static String[] syntaxStyleColorSettingsKeys, syntaxStyleBoldSettingsKeys, syntaxStyleItalicSettingsKeys;
	private static String[] defaultSyntaxStyleColorSettingsValues;
	private static boolean[] defaultSyntaxStyleBoldSettingsValues;
	private static boolean[] defaultSyntaxStyleItalicSettingsValues;

	public void setEditorSyntaxStyleByPosition(final int index, final SyntaxStyle syntaxStyle) {
		syntaxStyleColorSettingsValues[index] = syntaxStyle.getColorAsHexString();
		syntaxStyleItalicSettingsValues[index] = syntaxStyle.isItalic();
		syntaxStyleBoldSettingsValues[index] = syntaxStyle.isBold();
		saveEditorSyntaxStyle(index);
	}

	public SyntaxStyle getEditorSyntaxStyleByPosition(final int index) {
		return new SyntaxStyle(
				getSyntaxColorValueByPosition(index, syntaxStyleColorSettingsValues),
				syntaxStyleItalicSettingsValues[index],
				syntaxStyleBoldSettingsValues[index]);
	}

	public SyntaxStyle getDefaultEditorSyntaxStyleByPosition(final int index) {
		return new SyntaxStyle(
				getSyntaxColorValueByPosition(index, defaultSyntaxStyleColorSettingsValues),
				defaultSyntaxStyleItalicSettingsValues[index],
				defaultSyntaxStyleBoldSettingsValues[index]);
	}

	private void saveEditorSyntaxStyle(final int index) {
		try {
			preferences.put(syntaxStyleColorSettingsKeys[index], syntaxStyleColorSettingsValues[index]);
			preferences.putBoolean(syntaxStyleBoldSettingsKeys[index], syntaxStyleBoldSettingsValues[index]);
			preferences.putBoolean(syntaxStyleItalicSettingsKeys[index], syntaxStyleItalicSettingsValues[index]);
			preferences.flush();
		} catch (final SecurityException se) {
			// cannot write to persistent storage for security reasons
		} catch (final BackingStoreException bse) {
			// unable to communicate with persistent storage (strange days)
		}
	}

	// For syntax styles, need to initialize from SyntaxUtilities defaults.
	// Taking care not to explicitly create a Color object, since it may trigger
	// Swing initialization (that caused problems for UC Berkeley when we
	// created Font objects here).  It shouldn't, but then again Font shouldn't
	// either but they said it did.  (see HeadlessException)
	// On othe other hand, the first statement of this method causes Color objects
	// to be created!  It is possible but a real pain in the rear to avoid using
	// Color objects totally.  Requires new methods for the SyntaxUtilities class.
	private void initializeEditorSyntaxStyles() {
		final SyntaxStyle syntaxStyle[] = SyntaxUtilities.getDefaultSyntaxStyles();
		final int tokens = syntaxStyle.length;
		syntaxStyleColorSettingsKeys = new String[tokens];
		syntaxStyleBoldSettingsKeys = new String[tokens];
		syntaxStyleItalicSettingsKeys = new String[tokens];
		defaultSyntaxStyleColorSettingsValues = new String[tokens];
		defaultSyntaxStyleBoldSettingsValues = new boolean[tokens];
		defaultSyntaxStyleItalicSettingsValues = new boolean[tokens];
		syntaxStyleColorSettingsValues = new String[tokens];
		syntaxStyleBoldSettingsValues = new boolean[tokens];
		syntaxStyleItalicSettingsValues = new boolean[tokens];
		for (int i = 0; i < tokens; i++) {
			syntaxStyleColorSettingsKeys[i] = SYNTAX_STYLE_COLOR_PREFIX + i;
			syntaxStyleBoldSettingsKeys[i] = SYNTAX_STYLE_BOLD_PREFIX + i;
			syntaxStyleItalicSettingsKeys[i] = SYNTAX_STYLE_ITALIC_PREFIX + i;
			syntaxStyleColorSettingsValues[i] = defaultSyntaxStyleColorSettingsValues[i] = syntaxStyle[i]
				.getColorAsHexString();
			syntaxStyleBoldSettingsValues[i] = defaultSyntaxStyleBoldSettingsValues[i] = syntaxStyle[i].isBold();
			syntaxStyleItalicSettingsValues[i] = defaultSyntaxStyleItalicSettingsValues[i] = syntaxStyle[i].isItalic();
		}
	}

	private void getEditorSyntaxStyleSettingsFromPreferences() {
		for (int i = 0; i < syntaxStyleColorSettingsKeys.length; i++) {
			syntaxStyleColorSettingsValues[i] = preferences.get(
				syntaxStyleColorSettingsKeys[i],
				syntaxStyleColorSettingsValues[i]);
			syntaxStyleBoldSettingsValues[i] = preferences.getBoolean(
				syntaxStyleBoldSettingsKeys[i],
				syntaxStyleBoldSettingsValues[i]);
			syntaxStyleItalicSettingsValues[i] = preferences.getBoolean(
				syntaxStyleItalicSettingsKeys[i],
				syntaxStyleItalicSettingsValues[i]);
		}
	}
	// *********************************************************************************

	////////////////////////////////////////////////////////////////////////
	//  Setting Getters
	////////////////////////////////////////////////////////////////////////

	/**
	 * Fetch value of a boolean setting given its identifier.
	 *
	 * @param id int containing the setting's identifier (constants listed above)
	 * @return corresponding boolean setting.
	 * @throws IllegalArgumentException if identifier is invalid.
	 */
	public boolean getBooleanSetting(final int id) {
		if (id >= 0 && id < booleanSettingsValues.length) {
			return booleanSettingsValues[id];
		} else {
			throw new IllegalArgumentException("Invalid boolean setting ID");
		}
	}

	/**
	 * Setting for whether user programs limited to "bare machine" formatted basic
	 * instructions. This was added 8 Aug 2006 but is fixed at false for now, due to
	 * uncertainty as to what exactly constitutes "bare machine".
	 *
	 * @return true if only bare machine instructions allowed, false otherwise.
	 * @deprecated Use <code>getBooleanSetting(int id)</code> with the appropriate
	 * boolean setting ID (e.g. <code>Settings.BARE_MACHINE_ENABLED</code>)
	 */
	@Deprecated
	public boolean getBareMachineEnabled() { return booleanSettingsValues[BARE_MACHINE_ENABLED]; }

	/**
	 * Setting for whether user programs can use pseudo-instructions or extended
	 * addressing modes or alternative instruction formats (all are implemented as
	 * pseudo-instructions).
	 *
	 * @return true if pseudo-instructions and formats permitted, false otherwise.
	 * @deprecated Use <code>getBooleanSetting(int id)</code> with the appropriate
	 * boolean setting ID (e.g. <code>Settings.EXTENDED_ASSEMBLER_ENABLED</code>)
	 */
	@Deprecated
	public boolean getExtendedAssemblerEnabled() { return booleanSettingsValues[EXTENDED_ASSEMBLER_ENABLED]; }

	/**
	 * Setting for whether selected program will be automatically assembled upon
	 * opening. This can be useful if user employs an external editor such as
	 * MIPSter.
	 *
	 * @return true if file is to be automatically assembled upon opening and false
	 * otherwise.
	 * @deprecated Use <code>getBooleanSetting(int id)</code> with the appropriate
	 * boolean setting ID (e.g. <code>Settings.ASSEMBLE_ON_OPEN_ENABLED</code>)
	 */
	@Deprecated
	public boolean getAssembleOnOpenEnabled() { return booleanSettingsValues[ASSEMBLE_ON_OPEN_ENABLED]; }

	/**
	 * Setting for whether Addresses in the Execute pane will be displayed in
	 * hexadecimal.
	 *
	 * @return true if addresses are displayed in hexadecimal and false otherwise
	 * (decimal).
	 * @deprecated Use <code>getBooleanSetting(int id)</code> with the appropriate
	 * boolean setting ID (e.g. <code>Settings.DISPLAY_ADDRESSES_IN_HEX</code>)
	 */
	@Deprecated
	public boolean getDisplayAddressesInHex() { return booleanSettingsValues[DISPLAY_ADDRESSES_IN_HEX]; }

	/**
	 * Setting for whether values in the Execute pane will be displayed in
	 * hexadecimal.
	 *
	 * @return true if values are displayed in hexadecimal and false otherwise
	 * (decimal).
	 * @deprecated Use <code>getBooleanSetting(int id)</code> with the appropriate
	 * boolean setting ID (e.g. <code>Settings.DISPLAY_VALUES_IN_HEX</code>)
	 */
	@Deprecated
	public boolean getDisplayValuesInHex() { return booleanSettingsValues[DISPLAY_VALUES_IN_HEX]; }

	/**
	 * Setting for whether the assemble operation applies only to the file currently
	 * open in the editor or whether it applies to all files in that file's
	 * directory (primitive project capability). If the "assemble on open" setting
	 * is set, this "assemble all" setting will be applied as soon as the file is
	 * opened.
	 *
	 * @return true if all files are to be assembled, false if only the file open in
	 * editor.
	 * @deprecated Use <code>getBooleanSetting(int id)</code> with the appropriate
	 * boolean setting ID (e.g. <code>Settings.ASSEMBLE_ALL_ENABLED</code>)
	 */
	@Deprecated
	public boolean getAssembleAllEnabled() { return booleanSettingsValues[ASSEMBLE_ALL_ENABLED]; }

	/**
	 * Setting for whether the currently selected exception handler (a MIPS source
	 * file) will be automatically included in each assemble operation.
	 *
	 * @return true if exception handler is to be included in assemble, false
	 * otherwise.
	 * @deprecated Use <code>getBooleanSetting(int id)</code> with the appropriate
	 * boolean setting ID (e.g. <code>Settings.EXCEPTION_HANDLER_ENABLED</code>)
	 */
	@Deprecated
	public boolean getExceptionHandlerEnabled() { return booleanSettingsValues[EXCEPTION_HANDLER_ENABLED]; }

	/**
	 * Setting for whether delayed branching will be applied during MIPS program
	 * execution. If enabled, the statement following a successful branch will be
	 * executed and then the branch is taken! This simulates pipelining and all MIPS
	 * processors do it. However it is confusing to assembly language students so is
	 * disabled by default. SPIM does same thing.
	 *
	 * @return true if delayed branching is enabled, false otherwise.
	 * @deprecated Use <code>getBooleanSetting(int id)</code> with the appropriate
	 * boolean setting ID (e.g. <code>Settings.DELAYED_BRANCHING_ENABLED</code>)
	 */
	@Deprecated
	public boolean getDelayedBranchingEnabled() { return booleanSettingsValues[DELAYED_BRANCHING_ENABLED]; }

	/**
	 * Setting concerning whether or not to display the Labels Window -- symbol
	 * table.
	 *
	 * @return true if label window is to be displayed, false otherwise.
	 * @deprecated Use <code>getBooleanSetting(int id)</code> with the appropriate
	 * boolean setting ID (e.g. <code>Settings.LABEL_WINDOW_VISIBILITY</code>)
	 */
	@Deprecated
	public boolean getLabelWindowVisibility() { return booleanSettingsValues[LABEL_WINDOW_VISIBILITY]; }

	/**
	 * Setting concerning whether or not the editor will display line numbers.
	 *
	 * @return true if line numbers are to be displayed, false otherwise.
	 * @deprecated Use <code>getBooleanSetting(int id)</code> with the appropriate
	 * boolean setting ID (e.g. <code>Settings.EDITOR_LINE_NUMBERS_DISPLAYED</code>)
	 */
	@Deprecated
	public boolean getEditorLineNumbersDisplayed() { return booleanSettingsValues[EDITOR_LINE_NUMBERS_DISPLAYED]; }

	/**
	 * Setting concerning whether or not assembler will consider warnings to be
	 * errors.
	 *
	 * @return true if warnings are considered errors, false otherwise.
	 * @deprecated Use <code>getBooleanSetting(int id)</code> with the appropriate
	 * boolean setting ID (e.g. <code>Settings.WARNINGS_ARE_ERRORS</code>)
	 */
	@Deprecated
	public boolean getWarningsAreErrors() { return booleanSettingsValues[WARNINGS_ARE_ERRORS]; }

	/**
	 * Setting concerning whether or not program arguments can be entered and used.
	 *
	 * @return true if program arguments can be entered/used, false otherwise.
	 * @deprecated Use <code>getBooleanSetting(int id)</code> with the appropriate
	 * boolean setting ID (e.g. <code>Settings.PROGRAM_ARGUMENTS</code>)
	 */
	@Deprecated
	public boolean getProgramArguments() { return booleanSettingsValues[PROGRAM_ARGUMENTS]; }

	/**
	 * Setting concerning whether or not highlighting is applied to Data Segment
	 * window.
	 *
	 * @return true if highlighting is to be applied, false otherwise.
	 * @deprecated Use <code>getBooleanSetting(int id)</code> with the appropriate
	 * boolean setting ID (e.g. <code>Settings.DATA_SEGMENT_HIGHLIGHTING</code>)
	 */
	@Deprecated
	public boolean getDataSegmentHighlighting() { return booleanSettingsValues[DATA_SEGMENT_HIGHLIGHTING]; }

	/**
	 * Setting concerning whether or not highlighting is applied to Registers,
	 * Coprocessor0, and Coprocessor1 windows.
	 *
	 * @return true if highlighting is to be applied, false otherwise.
	 * @deprecated Use <code>getBooleanSetting(int id)</code> with the appropriate
	 * boolean setting ID (e.g. <code>Settings.REGISTERS_HIGHLIGHTING</code>)
	 */
	@Deprecated
	public boolean getRegistersHighlighting() { return booleanSettingsValues[REGISTERS_HIGHLIGHTING]; }

	/**
	 * Setting concerning whether or not assembler will automatically initialize the
	 * program counter to address of statement labeled 'main' if defined.
	 *
	 * @return true if it initializes to 'main', false otherwise.
	 * @deprecated Use <code>getBooleanSetting(int id)</code> with the appropriate
	 * boolean setting ID (e.g. <code>Settings.START_AT_MAIN</code>)
	 */
	@Deprecated
	public boolean getStartAtMain() { return booleanSettingsValues[START_AT_MAIN]; }

	/**
	 * Name of currently selected exception handler file.
	 *
	 * @return String pathname of current exception handler file, empty if none.
	 */
	public String getExceptionHandler() { return stringSettingsValues[EXCEPTION_HANDLER]; }

	/**
	 * Returns identifier of current built-in memory configuration.
	 *
	 * @return String identifier of current built-in memory configuration, empty if
	 * none.
	 */
	public String getMemoryConfiguration() { return stringSettingsValues[MEMORY_CONFIGURATION]; }

	/**
	 * Current editor font. Retained for compatibility but replaced by:
	 * getFontByPosition(Settings.EDITOR_FONT)
	 *
	 * @return Font object for current editor font.
	 */
	public Font getEditorFont() { return getFontByPosition(EDITOR_FONT); }

	/**
	 * Retrieve a Font setting
	 *
	 * @param fontSettingPosition constant that identifies which item
	 * @return Font object for given item
	 */
	public Font getFontByPosition(final int fontSettingPosition) {
		if (fontSettingPosition >= 0 && fontSettingPosition < fontFamilySettingsValues.length) {
			return EditorFont.createFontFromStringValues(
				fontFamilySettingsValues[fontSettingPosition],
				fontStyleSettingsValues[fontSettingPosition],
				fontSizeSettingsValues[fontSettingPosition]);
		} else {
			return null;
		}
	}

	/**
	 * Retrieve a default Font setting
	 *
	 * @param fontSettingPosition constant that identifies which item
	 * @return Font object for given item
	 */
	public Font getDefaultFontByPosition(final int fontSettingPosition) {
		if (fontSettingPosition >= 0 && fontSettingPosition < defaultFontFamilySettingsValues.length) {
			return EditorFont.createFontFromStringValues(
				defaultFontFamilySettingsValues[fontSettingPosition],
				defaultFontStyleSettingsValues[fontSettingPosition],
				defaultFontSizeSettingsValues[fontSettingPosition]);
		} else {
			return null;
		}
	}

	/**
	 * Order of text segment display columns (there are 5, numbered 0 to 4).
	 *
	 * @return Array of int indicating the order. Original order is 0 1 2 3 4.
	 */
	public int[] getTextColumnOrder() { return getTextSegmentColumnOrder(stringSettingsValues[TEXT_COLUMN_ORDER]); }

	/**
	 * Retrieve the caret blink rate in milliseconds. Blink rate of 0 means do not
	 * blink.
	 *
	 * @return int blink rate in milliseconds
	 */

	public int getCaretBlinkRate() {
		int rate = 500;
		try {
			rate = Integer.parseInt(stringSettingsValues[CARET_BLINK_RATE]);
		} catch (final NumberFormatException nfe) {
			rate = Integer.parseInt(defaultStringSettingsValues[CARET_BLINK_RATE]);
		}
		return rate;
	}

	/**
	 * Get the tab size in characters.
	 *
	 * @return tab size in characters.
	 */
	public int getEditorTabSize() {
		int size = 8;
		try {
			size = Integer.parseInt(stringSettingsValues[EDITOR_TAB_SIZE]);
		} catch (final NumberFormatException nfe) {
			size = getDefaultEditorTabSize();
		}
		return size;
	}

	/**
	 * Get number of letters to be matched by editor's instruction guide before
	 * popup generated (if popup enabled). Should be 1 or 2. If 1, the popup will be
	 * generated after first letter typed, based on all matches; if 2, the popup
	 * will be generated after second letter typed.
	 *
	 * @return number of letters (should be 1 or 2).
	 */
	public int getEditorPopupPrefixLength() {
		int length = 2;
		try {
			length = Integer.parseInt(stringSettingsValues[EDITOR_POPUP_PREFIX_LENGTH]);
		} catch (final NumberFormatException nfe) {

		}
		return length;
	}

	/**
	 * Get the text editor default tab size in characters
	 *
	 * @return tab size in characters
	 */
	public int getDefaultEditorTabSize() { return Integer.parseInt(defaultStringSettingsValues[EDITOR_TAB_SIZE]); }

	/**
	 * Get the saved state of the Labels Window sorting (can sort by either label or
	 * address and either ascending or descending order). Default state is 0, by
	 * ascending addresses.
	 *
	 * @return State value 0-7, as a String.
	 */
	public String getLabelSortState() { return stringSettingsValues[LABEL_SORT_STATE]; }

	/**
	 * Get Color object for specified settings key. Returns null if key is not found
	 * or its value is not a valid color encoding.
	 *
	 * @param key the Setting key
	 * @return corresponding Color, or null if key not found or value not valid
	 * color
	 */
	public Color getColorSettingByKey(final String key) { return getColorValueByKey(key, colorSettingsValues); }

	/**
	 * Get default Color value for specified settings key. Returns null if key is
	 * not found or its value is not a valid color encoding.
	 *
	 * @param key the Setting key
	 * @return corresponding default Color, or null if key not found or value not
	 * valid color
	 */
	public Color getDefaultColorSettingByKey(final String key) {
		return getColorValueByKey(key, defaultColorSettingsValues);
	}

	/**
	 * Get Color object for specified settings name (a static constant). Returns
	 * null if argument invalid or its value is not a valid color encoding.
	 *
	 * @param position the Setting name (see list of static constants)
	 * @return corresponding Color, or null if argument invalid or value not valid
	 * color
	 */
	public Color getColorSettingByPosition(final int position) {
		return getColorValueByPosition(position, colorSettingsValues);
	}

	/**
	 * Get default Color object for specified settings name (a static constant).
	 * Returns null if argument invalid or its value is not a valid color encoding.
	 *
	 * @param position the Setting name (see list of static constants)
	 * @return corresponding default Color, or null if argument invalid or value not
	 * valid color
	 */
	public Color getDefaultColorSettingByPosition(final int position) {
		return getColorValueByPosition(position, defaultColorSettingsValues);
	}

	////////////////////////////////////////////////////////////////////////
	//  Setting Setters
	////////////////////////////////////////////////////////////////////////

	/**
	 * Set value of a boolean setting given its id and the value.
	 *
	 * @param id int containing the setting's identifier (constants listed above)
	 * @param value boolean value to store
	 * @throws IllegalArgumentException if identifier is not valid.
	 */
	public void setBooleanSetting(final int id, final boolean value) {
		if (id >= 0 && id < booleanSettingsValues.length) {
			internalSetBooleanSetting(id, value);
		} else {
			throw new IllegalArgumentException("Invalid boolean setting ID");
		}
	}

	/**
	 * Establish setting for whether or not pseudo-instructions and formats are
	 * permitted in user programs. User can change this setting via the IDE. If
	 * setting changes, new setting will be written to properties file.
	 *
	 * @param value True to permit, false otherwise.
	 * @deprecated Use <code>setBooleanSetting(int id, boolean value)</code> with
	 * the appropriate boolean setting ID (e.g.
	 * <code>Settings.EXTENDED_ASSEMBLER_ENABLED</code>)
	 */
	@Deprecated
	public void setExtendedAssemblerEnabled(final boolean value) {
		internalSetBooleanSetting(EXTENDED_ASSEMBLER_ENABLED, value);
	}

	/**
	 * Establish setting for whether a file will be automatically assembled as soon
	 * as it is opened. This is handy for those using an external text editor such
	 * as Mipster. If setting changes, new setting will be written to properties
	 * file.
	 *
	 * @param value True to automatically assemble, false otherwise.
	 * @deprecated Use <code>setBooleanSetting(int id, boolean value)</code> with
	 * the appropriate boolean setting ID (e.g.
	 * <code>Settings.ASSEMBLE_ON_OPEN_ENABLED</code>)
	 */
	@Deprecated
	public void setAssembleOnOpenEnabled(final boolean value) {
		internalSetBooleanSetting(ASSEMBLE_ON_OPEN_ENABLED, value);
	}

	/**
	 * Establish setting for whether a file will be assembled by itself (false) or
	 * along with all other files in its directory (true). This permits multi-file
	 * programs and a primitive "project" capability. If setting changes, new
	 * setting will be written to properties file.
	 *
	 * @param value True to assemble all, false otherwise.
	 * @deprecated Use <code>setBooleanSetting(int id, boolean value)</code> with
	 * the appropriate boolean setting ID (e.g.
	 * <code>Settings.ASSEMBLE_ALL_ENABLED</code>)
	 */
	@Deprecated
	public void setAssembleAllEnabled(final boolean value) { internalSetBooleanSetting(ASSEMBLE_ALL_ENABLED, value); }

	/**
	 * Establish setting for whether addresses in the Execute pane will be displayed
	 * in hexadecimal format.
	 *
	 * @param value True to display addresses in hexadecimal, false for decimal.
	 * @deprecated Use <code>setBooleanSetting(int id, boolean value)</code> with
	 * the appropriate boolean setting ID (e.g.
	 * <code>Settings.DISPLAY_ADDRESSES_IN_HEX</code>)
	 */
	@Deprecated
	public void setDisplayAddressesInHex(final boolean value) {
		internalSetBooleanSetting(DISPLAY_ADDRESSES_IN_HEX, value);
	}

	/**
	 * Establish setting for whether values in the Execute pane will be displayed in
	 * hexadecimal format.
	 *
	 * @param value True to display values in hexadecimal, false for decimal.
	 * @deprecated Use <code>setBooleanSetting(int id, boolean value)</code> with
	 * the appropriate boolean setting ID (e.g.
	 * <code>Settings.DISPLAY_VALUES_IN_HEX</code>)
	 */
	@Deprecated
	public void setDisplayValuesInHex(final boolean value) { internalSetBooleanSetting(DISPLAY_VALUES_IN_HEX, value); }

	/**
	 * Establish setting for whether the labels window (i.e. symbol table) will be
	 * displayed as part of the Text Segment display. If setting changes, new
	 * setting will be written to properties file.
	 *
	 * @param value True to dispay labels window, false otherwise.
	 * @deprecated Use <code>setBooleanSetting(int id, boolean value)</code> with
	 * the appropriate boolean setting ID (e.g.
	 * <code>Settings.LABEL_WINDOW_VISIBILITY</code>)
	 */
	@Deprecated
	public void setLabelWindowVisibility(final boolean value) {
		internalSetBooleanSetting(LABEL_WINDOW_VISIBILITY, value);
	}

	/**
	 * Establish setting for whether the currently selected exception handler (a
	 * MIPS source file) will be automatically included in each assemble operation.
	 * If setting changes, new setting will be written to properties file.
	 *
	 * @param value True to assemble exception handler, false otherwise.
	 * @deprecated Use <code>setBooleanSetting(int id, boolean value)</code> with
	 * the appropriate boolean setting ID (e.g.
	 * <code>Settings.EXCEPTION_HANDLER_ENABLED</code>)
	 */
	@Deprecated
	public void setExceptionHandlerEnabled(final boolean value) {
		internalSetBooleanSetting(EXCEPTION_HANDLER_ENABLED, value);
	}

	/**
	 * Establish setting for whether delayed branching will be applied during MIPS
	 * program execution. If enabled, the statement following a successful branch
	 * will be executed and then the branch is taken! This simulates pipelining and
	 * all MIPS processors do it. However it is confusing to assembly language
	 * students so is disabled by default. SPIM does same thing.
	 *
	 * @param value True to enable delayed branching, false otherwise.
	 * @deprecated Use <code>setBooleanSetting(int id, boolean value)</code> with
	 * the appropriate boolean setting ID (e.g.
	 * <code>Settings.DELAYED_BRANCHING_ENABLED</code>)
	 */

	@Deprecated
	public void setDelayedBranchingEnabled(final boolean value) {
		internalSetBooleanSetting(DELAYED_BRANCHING_ENABLED, value);
	}

	/**
	 * Establish setting for whether line numbers will be displayed by the text
	 * editor.
	 *
	 * @param value True to display line numbers, false otherwise.
	 * @deprecated Use <code>setBooleanSetting(int id, boolean value)</code> with
	 * the appropriate boolean setting ID (e.g.
	 * <code>Settings.EDITOR_LINE_NUMBERS_DISPLAYED</code>)
	 */
	@Deprecated
	public void setEditorLineNumbersDisplayed(final boolean value) {
		internalSetBooleanSetting(EDITOR_LINE_NUMBERS_DISPLAYED, value);
	}

	/**
	 * Establish setting for whether assembler warnings will be considered errors.
	 *
	 * @param value True to consider warnings to be errors, false otherwise.
	 * @deprecated Use <code>setBooleanSetting(int id, boolean value)</code> with
	 * the appropriate boolean setting ID (e.g.
	 * <code>Settings.WARNINGS_ARE_ERRORS</code>)
	 */
	@Deprecated
	public void setWarningsAreErrors(final boolean value) { internalSetBooleanSetting(WARNINGS_ARE_ERRORS, value); }

	/**
	 * Establish setting for whether program arguments can be ented/used.
	 *
	 * @param value True if program arguments can be entered/used, false otherwise.
	 * @deprecated Use <code>setBooleanSetting(int id, boolean value)</code> with
	 * the appropriate boolean setting ID (e.g.
	 * <code>Settings.PROGRAM_ARGUMENTS</code>)
	 */
	@Deprecated
	public void setProgramArguments(final boolean value) { internalSetBooleanSetting(PROGRAM_ARGUMENTS, value); }

	/**
	 * Establish setting for whether highlighting is to be applied to Data Segment
	 * window.
	 *
	 * @param value True if highlighting is to be applied, false otherwise.
	 * @deprecated Use <code>setBooleanSetting(int id, boolean value)</code> with
	 * the appropriate boolean setting ID (e.g.
	 * <code>Settings.DATA_SEGMENT_HIGHLIGHTING</code>)
	 */
	@Deprecated
	public void setDataSegmentHighlighting(final boolean value) {
		internalSetBooleanSetting(DATA_SEGMENT_HIGHLIGHTING, value);
	}

	/**
	 * Establish setting for whether highlighting is to be applied to Registers,
	 * Coprocessor0 and Coprocessor1 windows.
	 *
	 * @param value True if highlighting is to be applied, false otherwise.
	 * @deprecated Use <code>setBooleanSetting(int id, boolean value)</code> with
	 * the appropriate boolean setting ID (e.g.
	 * <code>Settings.REGISTERS_HIGHLIGHTING</code>)
	 */
	@Deprecated
	public void setRegistersHighlighting(final boolean value) {
		internalSetBooleanSetting(REGISTERS_HIGHLIGHTING, value);
	}

	/**
	 * Establish setting for whether assembler will automatically initialize program
	 * counter to address of statement labeled 'main' if defined.
	 *
	 * @param value True if PC set to address of 'main', false otherwise.
	 * @deprecated Use <code>setBooleanSetting(int id, boolean value)</code> with
	 * the appropriate boolean setting ID (e.g. <code>Settings.START_AT_MAIN</code>)
	 */
	@Deprecated
	public void setStartAtMain(final boolean value) { internalSetBooleanSetting(START_AT_MAIN, value); }

	/**
	 * Temporarily establish boolean setting. This setting will NOT be written to
	 * persisent store! Currently this is used only when running MARS from the
	 * command line
	 *
	 * @param id setting identifier. These are defined for this class as static
	 * final int.
	 * @param value True to enable the setting, false otherwise.
	 */
	public void setBooleanSettingNonPersistent(final int id, final boolean value) {
		if (id >= 0 && id < booleanSettingsValues.length) {
			booleanSettingsValues[id] = value;
		} else {
			throw new IllegalArgumentException("Invalid boolean setting ID");
		}
	}

	/**
	 * Establish setting for whether delayed branching will be applied during MIPS
	 * program execution. This setting will NOT be written to persisent store! This
	 * method should be called only to temporarily set this setting -- currently
	 * this is needed only when running MARS from the command line.
	 *
	 * @param value True to enabled delayed branching, false otherwise.
	 * @deprecated Use
	 * <code>setBooleanSettingNonPersistent(int id, boolean value)</code> with the
	 * appropriate boolean setting ID (e.g.
	 * <code>Settings.DELAYED_BRANCHING_ENABLED</code>)
	 */
	@Deprecated
	public void setDelayedBranchingEnabledNonPersistent(final boolean value) {
		// Note: Doing assignment to array results in non-persistent
		// setting (lost when MARS terminates).  For persistent, use
		// the internalSetBooleanSetting() method instead.
		booleanSettingsValues[DELAYED_BRANCHING_ENABLED] = value;
	}

	/**
	 * Set name of exception handler file and write it to persistent storage.
	 *
	 * @param newFilename name of exception handler file
	 */
	public void setExceptionHandler(final String newFilename) { setStringSetting(EXCEPTION_HANDLER, newFilename); }

	/**
	 * Store the identifier of the memory configuration.
	 *
	 * @param config A string that identifies the current built-in memory
	 * configuration
	 */

	public void setMemoryConfiguration(final String config) { setStringSetting(MEMORY_CONFIGURATION, config); }

	/**
	 * Set the caret blinking rate in milliseconds. Rate of 0 means no blinking.
	 *
	 * @param rate blink rate in milliseconds
	 */
	public void setCaretBlinkRate(final int rate) { setStringSetting(
		CARET_BLINK_RATE,
		""
			+ rate); }

	/**
	 * Set the tab size in characters.
	 *
	 * @param size tab size in characters.
	 */
	public void setEditorTabSize(final int size) { setStringSetting(
		EDITOR_TAB_SIZE,
		""
			+ size); }

	/**
	 * Set number of letters to be matched by editor's instruction guide before
	 * popup generated (if popup enabled). Should be 1 or 2. If 1, the popup will be
	 * generated after first letter typed, based on all matches; if 2, the popup
	 * will be generated after second letter typed.
	 *
	 * @param number of letters (should be 1 or 2).
	 */
	public void setEditorPopupPrefixLength(final int length) {
		setStringSetting(
			EDITOR_POPUP_PREFIX_LENGTH,
			""
				+ length);
	}

	/**
	 * Set editor font to the specified Font object and write it to persistent
	 * storage. This method retained for compatibility but replaced by:
	 * setFontByPosition(Settings.EDITOR_FONT, font)
	 *
	 * @param font Font object to be used by text editor.
	 */
	public void setEditorFont(final Font font) { setFontByPosition(EDITOR_FONT, font); }

	/**
	 * Store a Font setting
	 *
	 * @param fontSettingPosition Constant that identifies the item the font goes
	 * with
	 * @font The font to set that item to
	 */
	public void setFontByPosition(final int fontSettingPosition, final Font font) {
		if (fontSettingPosition >= 0 && fontSettingPosition < fontFamilySettingsValues.length) {
			fontFamilySettingsValues[fontSettingPosition] = font.getFamily();
			fontStyleSettingsValues[fontSettingPosition] = EditorFont.styleIntToStyleString(font.getStyle());
			fontSizeSettingsValues[fontSettingPosition] = EditorFont.sizeIntToSizeString(font.getSize());
			saveFontSetting(fontSettingPosition, fontFamilySettingsKeys, fontFamilySettingsValues);
			saveFontSetting(fontSettingPosition, fontStyleSettingsKeys, fontStyleSettingsValues);
			saveFontSetting(fontSettingPosition, fontSizeSettingsKeys, fontSizeSettingsValues);
		}
		if (fontSettingPosition == EDITOR_FONT) {
			setChanged();
			notifyObservers();
		}
	}

	/**
	 * Store the current order of Text Segment window table columns, so the ordering
	 * can be preserved and restored.
	 *
	 * @param columnOrder An array of int indicating column order.
	 */

	public void setTextColumnOrder(final int[] columnOrder) {
		String stringifiedOrder = new String();
		for (int i = 0; i < columnOrder.length; i++) {
			stringifiedOrder += Integer.toString(columnOrder[i])
				+ " ";
		}
		setStringSetting(TEXT_COLUMN_ORDER, stringifiedOrder);
	}

	/**
	 * Store the current state of the Labels Window sorter. There are 8 possible
	 * states as described in LabelsWindow.java
	 *
	 * @param state The current labels window sorting state, as a String.
	 */

	public void setLabelSortState(final String state) { setStringSetting(LABEL_SORT_STATE, state); }

	/**
	 * Set Color object for specified settings key. Has no effect if key is invalid.
	 *
	 * @param key the Setting key
	 * @param color the Color to save
	 */
	public void setColorSettingByKey(final String key, final Color color) {
		for (int i = 0; i < colorSettingsKeys.length; i++) {
			if (key.equals(colorSettingsKeys[i])) {
				setColorSettingByPosition(i, color);
				return;
			}
		}
	}

	/**
	 * Set Color object for specified settings name (a static constant). Has no
	 * effect if invalid.
	 *
	 * @param position the Setting name (see list of static constants)
	 * @param color the Color to save
	 */
	public void setColorSettingByPosition(final int position, final Color color) {
		if (position >= 0 && position < colorSettingsKeys.length) {
			setColorSetting(position, color);
		}
	}

	/////////////////////////////////////////////////////////////////////////
	//
	//     PRIVATE HELPER METHODS TO DO THE REAL WORK
	//
	/////////////////////////////////////////////////////////////////////////

	// Initialize settings to default values.
	// Strategy: First set from properties file.
	//           If that fails, set from array.
	//           In either case, use these values as defaults in call to Preferences.

	private void initialize() {
		applyDefaultSettings();
		if (!readSettingsFromPropertiesFile(settingsFile)) {
			System.out.println(
				"MARS System error: unable to read Settings.properties defaults. Using built-in defaults.");
		}
		getSettingsFromPreferences();
	}

	// Default values.  Will be replaced if available from property file or Preferences object.
	private void applyDefaultSettings() {
		for (int i = 0; i < booleanSettingsValues.length; i++) {
			booleanSettingsValues[i] = defaultBooleanSettingsValues[i];
		}
		for (int i = 0; i < stringSettingsValues.length; i++) {
			stringSettingsValues[i] = defaultStringSettingsValues[i];
		}
		for (int i = 0; i < fontFamilySettingsValues.length; i++) {
			fontFamilySettingsValues[i] = defaultFontFamilySettingsValues[i];
			fontStyleSettingsValues[i] = defaultFontStyleSettingsValues[i];
			fontSizeSettingsValues[i] = defaultFontSizeSettingsValues[i];
		}
		for (int i = 0; i < colorSettingsValues.length; i++) {
			colorSettingsValues[i] = defaultColorSettingsValues[i];
		}
		initializeEditorSyntaxStyles();
	}

	// Used by all the boolean setting "setter" methods.
	private void internalSetBooleanSetting(final int settingIndex, final boolean value) {
		if (value != booleanSettingsValues[settingIndex]) {
			booleanSettingsValues[settingIndex] = value;
			saveBooleanSetting(settingIndex);
			setChanged();
			notifyObservers();
		}
	}

	// Used by setter method(s) for string-based settings (initially, only exception handler name)
	private void setStringSetting(final int settingIndex, final String value) {
		stringSettingsValues[settingIndex] = value;
		saveStringSetting(settingIndex);
	}

	// Used by setter methods for color-based settings
	private void setColorSetting(final int settingIndex, final Color color) {
		colorSettingsValues[settingIndex] = Binary.intToHexString(
			color.getRed() << 16
					| color.getGreen() << 8
					| color.getBlue());
		saveColorSetting(settingIndex);
	}

	// Get Color object for this key value.  Get it from values array provided as argument (could be either
	// the current or the default settings array).
	private Color getColorValueByKey(final String key, final String[] values) {
		for (int i = 0; i < colorSettingsKeys.length; i++) {
			if (key.equals(colorSettingsKeys[i])) { return getColorValueByPosition(i, values); }
		}
		return null;
	}

	// Get Color object for this key array position.  Get it from values array provided as argument (could be either
	// the current or the default settings array).
	private Color getColorValueByPosition(final int position, final String[] values) {
		Color color = null;
		if (position >= 0 && position < colorSettingsKeys.length) {
			try {
				color = Color.decode(values[position]);
			} catch (final NumberFormatException nfe) {
				color = null;
			}
		}
		return color;
	}

	// Use only for syntax style!!
	private Color getSyntaxColorValueByPosition(final int position, final String[] values) {
		Color color = null;
		if (position >= 0 && position < Token.ID_COUNT) {
			try {
				color = Color.decode(values[position]);
			} catch (final NumberFormatException nfe) {
				color = null;
			}
		}
		return color;
	}

	// Maybe someday I'll convert the whole shebang to use Maps.  In the meantime, we use
	// linear search of array.  Not a huge deal as settings are little-used.
	// Returns index or -1 if not found.
	private int getIndexOfKey(final String key, final String[] array) {
		int index = -1;
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(key)) {
				index = i;
				break;
			}
		}
		return index;
	}

	// Establish the settings from the given properties file.  Return true if it worked,
	// false if it didn't.  Note the properties file exists only to provide default values
	// in case the Preferences fail or have not been recorded yet.
	//
	// Any settings successfully read will be stored in both the xSettingsValues and
	// defaultXSettingsValues arrays (x=boolean,string,color).  The latter will overwrite the
	// last-resort default values hardcoded into the arrays above.
	//
	// NOTE: If there is NO ENTRY for the specified property, Globals.getPropertyEntry() returns
	// null.  This is no cause for alarm.  It will occur during system development or upon the
	// first use of a new MARS release in which new settings have been defined.
	// In that case, this method will NOT make an assignment to the settings array!
	// So consider it a precondition of this method: the settings arrays must already be
	// initialized with last-resort default values.
	private boolean readSettingsFromPropertiesFile(final String filename) {
		String settingValue;
		try {
			for (int i = 0; i < booleanSettingsKeys.length; i++) {
				settingValue = Globals.getPropertyEntry(filename, booleanSettingsKeys[i]);
				if (settingValue != null) {
					booleanSettingsValues[i] = defaultBooleanSettingsValues[i] = Boolean.valueOf(settingValue);
				}
			}
			for (int i = 0; i < stringSettingsKeys.length; i++) {
				settingValue = Globals.getPropertyEntry(filename, stringSettingsKeys[i]);
				if (settingValue != null) {
					stringSettingsValues[i] = defaultStringSettingsValues[i] = settingValue;
				}
			}
			for (int i = 0; i < fontFamilySettingsValues.length; i++) {
				settingValue = Globals.getPropertyEntry(filename, fontFamilySettingsKeys[i]);
				if (settingValue != null) {
					fontFamilySettingsValues[i] = defaultFontFamilySettingsValues[i] = settingValue;
				}
				settingValue = Globals.getPropertyEntry(filename, fontStyleSettingsKeys[i]);
				if (settingValue != null) {
					fontStyleSettingsValues[i] = defaultFontStyleSettingsValues[i] = settingValue;
				}
				settingValue = Globals.getPropertyEntry(filename, fontSizeSettingsKeys[i]);
				if (settingValue != null) {
					fontSizeSettingsValues[i] = defaultFontSizeSettingsValues[i] = settingValue;
				}
			}
			for (int i = 0; i < colorSettingsKeys.length; i++) {
				settingValue = Globals.getPropertyEntry(filename, colorSettingsKeys[i]);
				if (settingValue != null) {
					colorSettingsValues[i] = defaultColorSettingsValues[i] = settingValue;
				}
			}
		} catch (final Exception e) {
			return false;
		}
		return true;
	}

	// Get settings values from Preferences object.  A key-value pair will only be written
	// to Preferences if/when the value is modified.  If it has not been modified, the default value
	// will be returned here.
	//
	// PRECONDITION: Values arrays have already been initialized to default values from
	// Settings.properties file or default value arrays above!
	private void getSettingsFromPreferences() {
		for (int i = 0; i < booleanSettingsKeys.length; i++) {
			booleanSettingsValues[i] = preferences.getBoolean(booleanSettingsKeys[i], booleanSettingsValues[i]);
		}
		for (int i = 0; i < stringSettingsKeys.length; i++) {
			stringSettingsValues[i] = preferences.get(stringSettingsKeys[i], stringSettingsValues[i]);
		}
		for (int i = 0; i < fontFamilySettingsKeys.length; i++) {
			fontFamilySettingsValues[i] = preferences.get(fontFamilySettingsKeys[i], fontFamilySettingsValues[i]);
			fontStyleSettingsValues[i] = preferences.get(fontStyleSettingsKeys[i], fontStyleSettingsValues[i]);
			fontSizeSettingsValues[i] = preferences.get(fontSizeSettingsKeys[i], fontSizeSettingsValues[i]);
		}
		for (int i = 0; i < colorSettingsKeys.length; i++) {
			colorSettingsValues[i] = preferences.get(colorSettingsKeys[i], colorSettingsValues[i]);
		}
		getEditorSyntaxStyleSettingsFromPreferences();
	}

	// Save the key-value pair in the Properties object and assure it is written to persisent storage.
	private void saveBooleanSetting(final int index) {
		try {
			preferences.putBoolean(booleanSettingsKeys[index], booleanSettingsValues[index]);
			preferences.flush();
		} catch (final SecurityException se) {
			// cannot write to persistent storage for security reasons
		} catch (final BackingStoreException bse) {
			// unable to communicate with persistent storage (strange days)
		}
	}

	// Save the key-value pair in the Properties object and assure it is written to persisent storage.
	private void saveStringSetting(final int index) {
		try {
			preferences.put(stringSettingsKeys[index], stringSettingsValues[index]);
			preferences.flush();
		} catch (final SecurityException se) {
			// cannot write to persistent storage for security reasons
		} catch (final BackingStoreException bse) {
			// unable to communicate with persistent storage (strange days)
		}
	}

	// Save the key-value pair in the Properties object and assure it is written to persisent storage.
	private void saveFontSetting(final int index, final String[] settingsKeys, final String[] settingsValues) {
		try {
			preferences.put(settingsKeys[index], settingsValues[index]);
			preferences.flush();
		} catch (final SecurityException se) {
			// cannot write to persistent storage for security reasons
		} catch (final BackingStoreException bse) {
			// unable to communicate with persistent storage (strange days)
		}
	}

	// Save the key-value pair in the Properties object and assure it is written to persisent storage.
	private void saveColorSetting(final int index) {
		try {
			preferences.put(colorSettingsKeys[index], colorSettingsValues[index]);
			preferences.flush();
		} catch (final SecurityException se) {
			// cannot write to persistent storage for security reasons
		} catch (final BackingStoreException bse) {
			// unable to communicate with persistent storage (strange days)
		}
	}

	/*
	 *  Private helper to do the work of converting a string containing Text
	 *  Segment window table column order into int array and returning it.
	 *  If a problem occurs with the parameter string, will fall back to the
	 *  default defined above.
	 */
	private int[] getTextSegmentColumnOrder(final String stringOfColumnIndexes) {
		final StringTokenizer st = new StringTokenizer(stringOfColumnIndexes);
		final int[] list = new int[st.countTokens()];
		int index = 0, value;
		boolean valuesOK = true;
		while (st.hasMoreTokens()) {
			try {
				value = Integer.parseInt(st.nextToken());
			} // could be either NumberFormatException or NoSuchElementException
			catch (final Exception e) {
				valuesOK = false;
				break;
			}
			list[index++] = value;
		}
		if (!valuesOK && !stringOfColumnIndexes.equals(defaultStringSettingsValues[TEXT_COLUMN_ORDER])) {
			return getTextSegmentColumnOrder(defaultStringSettingsValues[TEXT_COLUMN_ORDER]);
		}
		return list;
	}

}
