/*
KeyringEditor

Copyright 2004 Markus Griessnig
Vienna University of Technology
Institute of Computer Technology

KeyringEditor is based on:
Java Keyring v0.6
Copyright 2004 Frank Taylor <keyring@lieder.me.uk>

These programs are distributed in the hope that they will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.
*/

// Prop.java

// 24.11.2004

// 07.12.2004: csvFilename deleted

package com.otisbean.keyring.gui;

import java.util.*;
import java.io.*;

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

			editor.PASSWORD_TIMEOUT = timeout * 60 * 1000;  // ms // Default: 1 minute
		}
	}
}
