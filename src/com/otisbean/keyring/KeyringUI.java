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
import com.otisbean.keyring.gui.Editor;

/**
 * Java desktop UI for Keyring for webOS.
 * 
 * @author Dirk Bergstrom
 */
public class KeyringUI {

	private static void usage(int exitCode) {
		System.err.println(
			"Usage:\n" +
			"java -jar keyring-ui.jar [json-db-file]\n" +
		    "    To start up the full-featured GUI, optionally loading the given db.\n" +
			"OR\n" +
			"java -jar keyring-ui.jar input-file json-output-file [keyring|csv|ewallet|codewallet]\n" +
			"    To convert input-file from the given format and write to json-output-file.");
		System.exit(exitCode);
	}
	
	private static String getPasswordFromConsole(String prompt) throws IOException {
		System.out.println(prompt);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));						
		return in.readLine();
	}

	private static void doConsole(String[] args) throws Exception {
		 String inFile = args[0];
		 String jsonFile = args[1];
		 if (inFile.equals(jsonFile)) {
			 System.err.println("The input and output (JSON) files need to have different names.");
			 System.exit(1);
		 }
		 String type = args[2];
		 // FIXME validate path info
		 Converter converter = Converter.getConverter(type);
		 String inPass = null;
		 if (converter.needsInputFilePassword) {
			 inPass = getPasswordFromConsole("Enter password for input file: ");
		 }
		 String jsonPass = getPasswordFromConsole("Enter password for JSON (output) file: ");
		 int count = converter.export(jsonPass, inPass, inFile, jsonFile);
		 System.out.println(count + " Items converted and written to " + jsonFile);
	}
	
	public static void main(String[] args) {
		try {
			if (args.length == 3) {
				doConsole(args);
			} else if (args.length == 1 && args[0].matches("^(--?[hH?](elp)?|/[hH?])$")) {
				usage(0);
			} else if (args.length < 2){
				Editor.main(args);
			} else {
				usage(1);
			}
		} catch (Exception e) {
			System.err.println("Horrible error: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}			
	}
}
