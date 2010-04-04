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
import java.util.Iterator;

import com.otisbean.keyring.Item;
import com.otisbean.keyring.Ring;

/**
 * Export CodeWallet Pro export format as Keyring for webOS.
 * 
 * @author Matt Williams, Dirk Bergstrom
 */
public class EWalletExportConverter extends Converter {

	public EWalletExportConverter() {
		needsInputFilePassword = false;
	}

	private Collection<String[]> readInputFile(String inFile) throws Exception {
		Hashtable<String, String[]> cards = new Hashtable<String, String[]>();
		try {
			Reader fr = getReader(inFile);
			BufferedReader br = new BufferedReader(fr);
			String thisline = br.readLine();
			String lastline = "";
			if (null == thisline) {
				throw new Exception("Input file is empty!");
			}

			int nameidx = 0;
			int categoryidx = 1;
			int accountidx = 2;
			int passwordidx = 3;
			int notesidx = 4;
			int urlidx = 5;

			// boolean readingName = false;
			boolean entryStart = false;
			boolean entryEnd = false;
			boolean readingNote = false;
			String[] thisEntry = new String[] { "", "", "", "", "", "" };
			String thisCategory = "";

			while (thisline != null) {
				// log(thisline);
				if (thisline.startsWith("Category: ")) {
					entryEnd = true;
				} else if (thisline.length() == 0) {
					if (readingNote) {
						thisEntry[notesidx] += thisline + "\n";
					}
				} else {
					if (!readingNote) {
						// If we hit the next card, have
						// to add current card to list and start the new one here.
						if ((thisline.toLowerCase().startsWith("card name ") ||
								(thisline.toLowerCase().startsWith("card")
								&& !thisline.startsWith("Card Notes")
								&& !thisline.startsWith("Card Number")
								&& !thisline.startsWith("Card Provider")))
								&& lastline.length() == 0) {
							if (entryStart) {
								// Code copied from below for adding card to list
								thisEntry[categoryidx] = thisCategory;
								if (cards.containsKey(thisEntry[nameidx])) {
									log("WARNING: Duplicate entry, skipping: ["
											+ thisEntry[nameidx] + "]");
								} else {
									log("Adding to list: " + thisEntry[nameidx]);
									cards.put(thisEntry[nameidx], thisEntry);
									thisEntry = new String[] { "", "", "", "", "", "" };
								}
								// entryStart = false;
								entryEnd = false;
							}
							// Code copied from above for starting new card.
							entryStart = true;
							int keystart = 5;
							if (thisline.startsWith("Card Name ")) {
								keystart = "Card Name ".length();
							}
							thisEntry[nameidx] = thisline.substring(keystart);
						} else if (thisline.toLowerCase().startsWith("user name ")) {
							thisEntry[accountidx] =
									thisline.substring("User Name ".length());
						} else if (thisline.toLowerCase().startsWith("password ")) {
							thisEntry[passwordidx] =
								thisline.substring("Password ".length());
						} else if (thisline.toLowerCase().startsWith("url ")) {
							thisEntry[urlidx] = thisline.substring("URL "
									.length());
						} else if (thisline.equals("Card Notes")) {
							readingNote = true;
							thisEntry[notesidx] += thisline + "\n";
						} else {
							// Anything else, write to the notes.
							thisEntry[notesidx] += thisline + "\n";
						}
					} else {
						// If we are reading a note and hit the next card, have
						// to add current card to list and start the new one
						// here.
						if ((thisline.toLowerCase().startsWith("card name ") ||
								(thisline.toLowerCase().startsWith("card")
								&& !thisline.startsWith("Card Notes")
								&& !thisline.startsWith("Card Number")
								&& !thisline.startsWith("Card Provider")))
								&& lastline.length() == 0) {
							readingNote = false;
							// Code copied from below for adding card to list
							thisEntry[categoryidx] = thisCategory;
							if (cards.containsKey(thisEntry[nameidx])) {
								log("WARNING: Duplicate entry, skipping: ["
										+ thisEntry[nameidx] + "]");
							} else {
								log("Adding to list: " + thisEntry[nameidx]);
								cards.put(thisEntry[nameidx], thisEntry);
								thisEntry = new String[] { "", "", "", "", "", "" };
							}
							entryEnd = false;

							// Code copied from above for starting new card.
							entryStart = true;
							int keystart = 5;
							if (thisline.startsWith("Card Name "))
								keystart = "Card Name ".length();
							thisEntry[nameidx] = thisline.substring(keystart);
						} else {
							thisEntry[notesidx] += thisline + "\n";
						}
					}
				}

				if (entryEnd) {
					if (entryStart) {
						thisEntry[categoryidx] = thisCategory;
						if (cards.containsKey(thisEntry[nameidx])) {
							log("WARNING: Duplicate entry, skipping: ["
									+ thisEntry[nameidx] + "]");
						} else {
							log("Adding to list: " + thisEntry[nameidx]);
							cards.put(thisEntry[nameidx], thisEntry);
							thisEntry = new String[] { "", "", "", "", "", "" };
							// entries.add(thisEntry);
						}
					}
					entryEnd = false;
					if (thisline.startsWith("Category: ")) {
						thisCategory = thisline.substring("Category: ".length());
						readingNote = false;
						entryStart = false;
					}
				}

				lastline = thisline;
				thisline = br.readLine();
			} // (thisline != null)

			// Add last record if it hasn't been done before we hit EOF
			if (entryStart && !entryEnd) {
				thisEntry[categoryidx] = thisCategory;
				if (cards.containsKey(thisEntry[nameidx])) {
					log("WARNING: Duplicate entry, skipping: ["
							+ thisEntry[nameidx] + "]");
				} else {
					log("Adding to list: " + thisEntry[nameidx]);
					cards.put(thisEntry[nameidx], thisEntry);
				}
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

	@SuppressWarnings("unchecked")
	@Override
	public Ring convert(String inFile, String inPassword, String outPassword)
			throws Exception {
		Collection<String[]> entries = readInputFile(inFile);
		Ring ring = new Ring(outPassword);

		Date now = new Date();
		int exported = 0;
		for (Iterator i = entries.iterator(); i.hasNext();) {
			String[] entry = (String[]) i.next();

			String name = entry[0];
			String category = entry[1];
			String account = entry[2];
			String password = entry[3];
			String notes = entry[4];
			String url = entry[5];
			/* We'll pretend that each item was created when it was imported. */
			long changed = now.getTime();

			ring.addItem(new Item(ring, account, password, url, notes, name,
					category, changed, changed, changed));
			exported++;
		}
		return ring;
	}

	private String detectEncoding(InputStream is) {
		String encoding = System.getProperty("file.encoding");
		try {
			byte[] bytes = new byte[2];// {0,0};
			is.read(bytes);
			if (bytes[0] == 0)
				encoding = "UTF-16BE";
			else if (bytes[1] == 0)
				encoding = "UTF-16LE";
			else if (bytes[0] < 0)
				encoding = "UTF-16";
		} catch (IOException ex) {
			logError("Error detecting file encoding, using default.");
		}

		return encoding;
	}

	protected Reader getReader(String infileName) throws FileNotFoundException {
		try {
			FileInputStream fis = new FileInputStream(infileName);
			String encoding = detectEncoding(fis);
			fis.close();
			log("Using " + encoding + " encoding to read input file ("
					+ infileName + ")");
			return new InputStreamReader(new FileInputStream(infileName),
					encoding);
		} catch (UnsupportedEncodingException ex) {
			return new FileReader(infileName);
		} catch (IOException ex) {
			return new FileReader(infileName);
		}
	}
}
