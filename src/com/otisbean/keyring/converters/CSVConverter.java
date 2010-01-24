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
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.ExcelCSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import com.otisbean.keyring.Item;
import com.otisbean.keyring.Ring;

/**
 * Export CSV format as Keyring for webOS.
 * 
 * @author Matt Williams, Dirk Bergstrom
 */
public class CSVConverter extends Converter {

	public CSVConverter() {
		needsInputFilePassword = false;
	}

	public LabeledCSVParser determineFileFormat(String csvFile)
			throws Exception {
		FileReader fr = new FileReader(csvFile);
		BufferedReader br = new BufferedReader(fr);

		// Look thru the file and see if it's Excel format or plain CSV
		boolean isExcelFormat = false;
		String thisLine = br.readLine();
		while (thisLine != null) {
			// Excel uses doubled quotes to escape a quote character
			if (thisLine.indexOf("\"\"") >= 0) {
				isExcelFormat = true;
				break;
			}
			thisLine = br.readLine();
		}
		br.close();
		fr.close();

		// Now reopen the file and return the parser.
		LabeledCSVParser lcsvp = null;
		fr = new FileReader(csvFile);
		br = new BufferedReader(fr);
		if (isExcelFormat) {
			lcsvp = new LabeledCSVParser(new ExcelCSVParser(br));
		} else {
			lcsvp = new LabeledCSVParser(new CSVParser(br));
		}
		return lcsvp;

	}

	@Override
	public int export(String outPassword, String unused, String csvFile,
			String outFile) throws Exception {

		LabeledCSVParser lcsvp = determineFileFormat(csvFile);
		String[] labels = lcsvp.getLabels();
		String[][] entries = lcsvp.getAllValues();

		Ring ring = new Ring(SCHEMA_VERSION, outPassword);

		// Determine column indexes
		int nameidx = -1;
		int categoryidx = -1;
		int accountidx = -1;
		int passwordidx = -1;
		int notesidx = -1;
		int urlidx = -1;
		String[] entry = labels; // entries[0];
		for (int ii = 0; ii < entry.length; ii++) {
			if (entry[ii].equalsIgnoreCase("name"))
				nameidx = ii;
			if (entry[ii].equalsIgnoreCase("category"))
				categoryidx = ii;
			if (entry[ii].equalsIgnoreCase("account"))
				accountidx = ii;
			if (entry[ii].equalsIgnoreCase("password"))
				passwordidx = ii;
			if (entry[ii].toLowerCase().startsWith("note"))
				notesidx = ii;
			if (entry[ii].equalsIgnoreCase("url"))
				urlidx = ii;
		}

		if (nameidx == -1 || accountidx == -1 || passwordidx == -1) {
			throw new Exception("Input file format is invalid.  Must contain " +
					"header row with labels name, category, account, password, " +
					"notes.  Any other labels will be ignored.  The name, " +
					"account, and password fields are mandatory, others are optional.");
		}

		Date now = new Date();
		int exported = 0;
		for (int ii = 0; ii < entries.length; ii++) {
			entry = entries[ii];

			String name = nameidx == -1 ? "" : entry[nameidx];
			if (ring.getItem(name) != null) {
				System.out.println("WARNING: Entry #" + (ii + 1)
						+ " - Duplicate entry, skipping: [" + name + "].");
			} else {
				if (accountidx >= entry.length || passwordidx >= entry.length) {
					System.out
							.println("WARNING: Entry #"
									+ (ii + 1)
									+ " is invalid: account or password not found. Entry: ["
									+ name + "].");
					continue;
				}
				String category = categoryidx == -1
						|| categoryidx >= entry.length ? "Unfiled"
						: entry[categoryidx];
				String account = accountidx == -1 || accountidx >= entry.length ? ""
						: entry[accountidx];
				String password = passwordidx == -1
						|| passwordidx >= entry.length ? ""
						: entry[passwordidx];
				String notes = notesidx == -1 || notesidx >= entry.length ? ""
						: entry[notesidx];
				String url = urlidx == -1 || urlidx >= entry.length ? ""
						: entry[urlidx];
				/* Pretend that each item was created when it was imported. */
				long changed = now.getTime();

				ring.addItem(new Item(ring, account, password, url, notes,
						name, category, changed, changed, changed));
				exported++;
			}
		}

		writeOutputFile(ring, outFile);
		return exported;
	}
}
