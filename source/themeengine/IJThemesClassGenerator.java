/*
 * Copyright 2020 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package themeengine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * This tool creates look and feel classes for all themes listed in themes.json.
 *
 * @author Karl Tauber
 */
public class IJThemesClassGenerator {

	public static void main(final String[] args) {
		final IJThemesManager themesManager = new IJThemesManager();
		themesManager.loadBundledThemes();

		final String toPath = "../flatlaf-intellij-themes/src/main/java" + IJThemesPanel.THEMES_PACKAGE + "..";

		final StringBuilder allInfos = new StringBuilder();
		final StringBuilder markdownTable = new StringBuilder();
		markdownTable.append("Name | Class\n");
		markdownTable.append("-----|------\n");

		for (final IJThemeInfo ti : themesManager.bundledThemes) {
			if (ti.sourceCodeUrl == null || ti.sourceCodePath == null) { continue; }

			generateClass(ti, toPath, allInfos, markdownTable);
		}

		final Path out = new File(toPath, "FlatAllIJThemes.java").toPath();
		final String allThemes = CLASS_HEADER + ALL_THEMES_TEMPLATE.replace("${allInfos}", allInfos);
		writeFile(out, allThemes);

		System.out.println(markdownTable);
	}

	private static void generateClass(final IJThemeInfo ti, final String toPath, final StringBuilder allInfos,
			final StringBuilder markdownTable) {
		String resourceName = ti.resourceName;
		String resourcePath = null;
		final int resSep = resourceName.indexOf('/');
		if (resSep >= 0) {
			resourcePath = resourceName.substring(0, resSep);
			resourceName = resourceName.substring(resSep + 1);
		}

		String name = ti.name;
		final int nameSep = name.indexOf('/');
		if (nameSep >= 0) { name = name.substring(nameSep + 1).trim(); }

		final StringBuilder buf = new StringBuilder();
		for (final String n : name.split(" ")) {
			if (n.length() == 0 || n.equals("-")) { continue; }

			if (Character.isUpperCase(n.charAt(0))) {
				buf.append(n);
			} else {
				buf.append(Character.toUpperCase(n.charAt(0))).append(n.substring(1));
			}
		}

		final String subPackage = resourcePath != null ? '.' + resourcePath.replace("-", "") : "";
		final String themeClass = "Flat" + buf + "IJTheme";
		final String themeFile = resourceName;

		final String classBody = CLASS_HEADER + CLASS_TEMPLATE.replace("${subPackage}", subPackage).replace(
				"${themeClass}", themeClass).replace("${themeFile}", themeFile);

		File toDir = new File(toPath);
		if (resourcePath != null) { toDir = new File(toDir, resourcePath.replace("-", "")); }

		final Path out = new File(toDir, themeClass + ".java").toPath();
		writeFile(out, classBody);

		if (allInfos.length() > 0) { allInfos.append('\n'); }
		allInfos.append(THEME_TEMPLATE.replace("${subPackage}", subPackage).replace("${themeClass}", themeClass)
				.replace("${themeName}", name));

		markdownTable.append(String.format("[%s](%s) | `themeengine.include.com.formdev.flatlaf.intellijthemes%s.%s`\n", name,
				ti.sourceCodeUrl, subPackage, themeClass));
	}

	private static void writeFile(final Path out, final String content) {
		try {
			Files.write(out, content.getBytes(StandardCharsets.ISO_8859_1), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}

	private static final String CLASS_HEADER = "/*\n" + " * Copyright 2020 FormDev Software GmbH\n" + " *\n"
			+ " * Licensed under the Apache License, Version 2.0 (the \"License\");\n"
			+ " * you may not use this file except in compliance with the License.\n"
			+ " * You may obtain a copy of the License at\n" + " *\n"
			+ " *     https://www.apache.org/licenses/LICENSE-2.0\n" + " *\n"
			+ " * Unless required by applicable law or agreed to in writing, software\n"
			+ " * distributed under the License is distributed on an \"AS IS\" BASIS,\n"
			+ " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
			+ " * See the License for the specific language governing permissions and\n"
			+ " * limitations under the License.\n" + " */\n" + "\n";

	private static final String CLASS_TEMPLATE = "package themeengine.include.com.formdev.flatlaf.intellijthemes${subPackage};\n" + "\n"
			+ "import themeengine.include.com.formdev.flatlaf.IntelliJTheme;\n" + "\n" + "/**\n" + " * @author Karl Tauber\n" + " */\n"
			+ "public class ${themeClass}\n" + "	extends IntelliJTheme.ThemeLaf\n" + "{\n"
			+ "	public static boolean install( ) {\n" + "		try {\n"
			+ "			return install( new ${themeClass}() );\n" + "		} catch( RuntimeException ex ) {\n"
			+ "			return false;\n" + "		}\n" + "	}\n" + "\n" + "	public ${themeClass}() {\n"
			+ "		super( Utils.loadTheme( \"${themeFile}\" ) );\n" + "	}\n" + "}\n";

	private static final String ALL_THEMES_TEMPLATE = "package themeengine.include.com.formdev.flatlaf.intellijthemes;\n" + "\n"
			+ "import javax.swing.UIManager.LookAndFeelInfo;\n" + "\n" + "/**\n" + " * @author Karl Tauber\n" + " */\n"
			+ "public class FlatAllIJThemes\n" + "{\n" + "	public static final LookAndFeelInfo[] INFOS = {\n"
			+ "${allInfos}\n" + "	};\n" + "}\n";

	private static final String THEME_TEMPLATE = "		new LookAndFeelInfo( \"${themeName}\", \"themeengine.include.com.formdev.flatlaf.intellijthemes${subPackage}.${themeClass}\" ),";
}
