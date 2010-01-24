/**
 * @author Dirk Bergstrom
 *
 * Keyring for webOS - Easy password management on your phone.
 * Copyright (C) 2009-2010, Dirk Bergstrom, keyring@otisbean.com
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
package com.otisbean.keyring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.otisbean.keyring.converters.Converter;

/**
 * Translate from an external format to Keyring for webOS export format.
 * 
 * @author Dirk Bergstrom
 */
public class KeyringUI {
	/**
	 * Application name with additional information (URI).
	 */
	public final static String GUI_APP_NAME =
		"KeyringUI - http://www.otisbean.com/keyring/";
	
	/**
	 * Information message about the canceled file selection. 
	 */
	public final static String GUI_SELECTION_CANCELLED_INFO_MESSAGE =
		"The file selection has been cancelled.";
	
	/**
	 * Error message about the equal files (input==output), but this
	 * should never occur because of the FileFilter objects for PDB/JSON.
	 */
	public final static String GUI_SELECTION_SAMEFILES_ERROR_MESSAGE =
		"The inputs and output (JSON) files need to have different file names.";

	/**
	 * Generic (GUI/CONSOLE) question about the PDB password. 
	 */
	public final static String GENERIC_ASK_FOR_IN_PASSWORD =
		"Enter password for input file";
	
	/**
	 * Generic (GUI/CONSOLE) question about the JSON password. 
	 */
	public final static String GENERIC_ASK_FOR_JSON_PASSWORD =
		"Enter password for JSON (output) file";

	private static String getPasswordFromConsole(String prompt) throws IOException {
		System.out.println(prompt);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));						
		return in.readLine();
	}

	private static void doConsole(String[] args) throws Exception {
		 if (args.length != 3) {
			 System.err.println(
					 "Usage: KeyringConverter input-file json-output-file " +
					 "[keyring|csv|ewallet|codewallet]");
			 System.exit(1);
		 }

		 String inFile = args[0];
		 String jsonFile = args[1];
		 String type = args[2];
		 // FIXME validate path info
		 Converter converter = Converter.getConverter(type);
		 String inPass = null;
		 if (converter.needsInputFilePassword) {
			 inPass = getPasswordFromConsole(GENERIC_ASK_FOR_IN_PASSWORD + ": ");
		 }
		 String jsonPass = getPasswordFromConsole(GENERIC_ASK_FOR_JSON_PASSWORD + ": ");
		 int count = converter.export(jsonPass, inPass, inFile, jsonFile);
		 System.out.println(count + " Items converted and written to " + jsonFile);
	}
	
	public static void main(String[] args) {
		try {
			doConsole(args);
		} catch (Exception e) {
			System.err.println("Horrible error: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}			
	}
}
