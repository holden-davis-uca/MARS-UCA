/*
 * Copyright 2019 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package themeengine.include.com.formdev.flatlaf;

/**
 * A Flat LaF that has a light color scheme.
 * <p>
 * The UI defaults are loaded from FlatLightLaf.properties and FlatLaf.properties
 *
 * @author Karl Tauber
 */
public class FlatLightLaf
	extends FlatLaf
{
	public static boolean install( ) {
		return install( new FlatLightLaf() );
	}

	@Override
	public String getName() {
		return "FlatLaf Light";
	}

	@Override
	public String getDescription() {
		return "FlatLaf Light Look and Feel";
	}

	@Override
	public boolean isDark() {
		return false;
	}
}
