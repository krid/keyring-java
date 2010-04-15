/*
 * @author Dirk Bergstrom
 *
 * Keyring Desktop Client - Easy password management on your phone or desktop.
 * Copyright (C) 2009-2010, Dirk Bergstrom, keyring@otisbean.com
 * 
 * Adapted from KeyringEditor v1.1
 * Copyright 2006 Markus Griessnig
 * http://www.ict.tuwien.ac.at/keyring/
 * Markus graciously gave his assent to release the modified code under the GPLv3.
 *     
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.otisbean.keyring.gui;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * This class is used to load parameters from the file keyringeditor.ini.
 */
public class Prop {
	// ----------------------------------------------------------------
	// variables
	// ----------------------------------------------------------------

	/**
	 * Default filename
	 */
	private static final String iniFilename = "keyringeditor.ini";

	/**
	 * Reference to class Editor
	 */
	private Editor editor;

	// ----------------------------------------------------------------
	// constructor
	// ----------------------------------------------------------------
	/**
	 * Default constructor.
	 *
	 * @param editor Reference to class Editor
	 */
	public Prop(Editor editor) {
		this.editor = editor;
	}

	// ----------------------------------------------------------------
	// public ---------------------------------------------------------
	// ----------------------------------------------------------------
	/**
	 * This method opens the file keyringeditor.ini and reads the
	 * parameters "TitleSeparator", "CsvSeparator" and "PasswordTimeout".
	 *
	 * If no file is found, default values are used.
	 *
	 * TitleSeparator separates levels in an entry title for the tree view ('/').
	 * CsvSeparator is used as the separator for converting entries to a csv-file (';').
	 * PasswordTimeout is the time in minutes after inactivity forces a lock of the application ('1').
	 */
	public void setup() {
		Properties props = new Properties();

		// load KeyringEditor.ini
		try {
			FileInputStream in = new FileInputStream(iniFilename);
			props.load(in);
			in.close();
		}
		catch(Exception e) {
			System.err.println("Prop.java: File " + iniFilename + " not found. Using default values.");
			return;
		}

		/*
		String csvFilename = props.getProperty("CsvFilename");
		if(csvFilename != null) {
			editor.getModel().setCsvFilename(csvFilename); // Default: 'keyring.csv'
		}
		*/

		String pwTimeout = props.getProperty("PasswordTimeout");
		if(pwTimeout != null) {
			int timeout = Integer.parseInt(pwTimeout); // minutes

			Gui.PASSWORD_TIMEOUT = timeout * 60 * 1000;  // ms // Default: 1 minute
		}
	}
}
