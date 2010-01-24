/**
 * @author Matt Williams, Dirk Bergstrom
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
package com.otisbean.keyring.converters;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;

import com.otisbean.keyring.Item;
import com.otisbean.keyring.Ring;

/**
 * Export CodeWallet Pro export format as Keyring for webOS.
 * 
 * @author Matt Williams, Dirk Bergstrom
 */
public class CodeWalletExportConverter extends Converter {

	public CodeWalletExportConverter() {
		needsInputFilePassword = false;
	}

	public int export(String outPassword, String unused, String inFile,
			String outFile) throws Exception {
		Collection<String[]> entries = readInputFile(inFile);
		Ring ring = new Ring(SCHEMA_VERSION, outPassword);
		Date now = new Date();
		int exported = 0;
		for (String[] entry : entries) {
			String name = entry[0];
			String category = entry[1];
			String account = entry[2];
			String password = entry[3];
			String notes = entry[4];
			String url = entry[5];
			/* Pretend that each item was created when it was imported. */
			long changed = now.getTime();
			ring.addItem(new Item(ring, account, password, url, notes, name,
					category, changed, changed, changed));
			exported++;
		}
		writeOutputFile(ring, outFile);
		return exported;
	}

	private Collection<String[]> readInputFile(String inFile)
			throws Exception {
		Hashtable<String, String[]> cards = new Hashtable<String, String[]>();
		try {
			Reader fr = getReader(inFile);
			BufferedReader br = new BufferedReader(fr);
			String thisline = br.readLine();
			if (thisline == null) {
				throw new Exception("Input file is empty!");
			}

			boolean readingName = false;
			boolean entryStart = false;
			boolean entryEnd = false;
			String[] thisEntry = new String[] { "", "", "", "", "", "" };
			String thisCategory = "";

			while (thisline != null) {
				if (thisline.startsWith("*-----")) {
					entryStart = true;
					if (entryEnd) {
						entryEnd = false;
						thisEntry = new String[] { "", "", "", "", "", "" };
					}
				} else if (thisline.length() == 0
						|| thisline.equals("<No Information>")) {
					if (entryStart) {
						entryEnd = true;
						readingName = true;
					}
				} else if (entryStart) {
					if (readingName && !thisline.startsWith("Folder: ")) {
						thisEntry[0] = thisline;
						readingName = false;
					} else if (thisline.startsWith("Folder: ")) {
						thisCategory = thisline.substring(thisline
								.indexOf(": ") + 2);
						/* After reading folder name, next useful entry is next
						 * card name */
						readingName = true;
					} else if (thisline.toLowerCase().startsWith("user id: ")) {
						thisEntry[2] = thisline.substring(thisline
								.indexOf(": ") + 2);
					} else if (thisline.toLowerCase().startsWith("password: ")) {
						thisEntry[3] = thisline.substring(thisline
								.indexOf(": ") + 2);
					} else if (thisline.toLowerCase().startsWith("url: ")
							|| thisline.toLowerCase().startsWith("web site: ")
							|| thisline.toLowerCase().startsWith("website: ")) {
						thisEntry[5] = thisline.substring(thisline
								.indexOf(": ") + 2);
					} else {
						// Anything else, write to the notes field.
						thisEntry[4] += thisline + "\n";
					}
				}

				if (entryStart && entryEnd) {
					thisEntry[1] = thisCategory;
					if (cards.containsKey(thisEntry[0])) {
						logError("WARNING: Duplicate entry, skipping: ["
										+ thisEntry[0] + "]");
					} else {
						cards.put(thisEntry[0], thisEntry);
						// entries.add(thisEntry);
					}
					entryStart = false;
				}

				thisline = br.readLine();
			}
			br.close();
			br = null;
			fr.close();
			fr = null;
		} catch (Exception ex) {
			logError("Exception processing input file:" + ex.getMessage());
			throw ex;
		}
		// return entries;
		return cards.values();
	}

	private String detectEncoding(InputStream is) {
		String encoding = System.getProperty("file.encoding");
		try {
			byte[] bytes = new byte[2];// {0,0};
			is.read(bytes);
			if (bytes[0] == 0) {
				encoding = "UTF-16BE";
			} else if (bytes[1] == 0) {
				encoding = "UTF-16LE";
			} else if (bytes[0] < 0) {
				encoding = "UTF-16";
			}
		} catch (Exception ex) {
			logError("Error detecting file encoding, using default.");
		}

		return encoding;
	}

	private Reader getReader(String infileName)	throws FileNotFoundException {
		// InputStreamReader on a FileInputStream
		try {
			FileInputStream fis = new FileInputStream(infileName);
			String encoding = detectEncoding(fis);
			fis.close();
			log("Using " + encoding
					+ " encoding to read input file \"" + infileName + "\"");
			return new InputStreamReader(new FileInputStream(infileName),
					encoding);
		} catch (UnsupportedEncodingException ex) {
			return new FileReader(infileName);
		} catch (IOException ex) {
			return new FileReader(infileName);
		}
	}
}
