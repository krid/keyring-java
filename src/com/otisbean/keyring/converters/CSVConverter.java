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
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;

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
	public Ring convert(String csvFile, String unused, String outPassword)
	        throws Exception {
		LabeledCSVParser lcsvp = determineFileFormat(csvFile);
		String[] labels = lcsvp.getLabels();
		String[][] entries = lcsvp.getAllValues();

		Ring ring = new Ring(outPassword);

		// Determine column indexes
		int titleidx = -1;
		int categoryidx = -1;
		int accountidx = -1;
		int passwordidx = -1;
		int notesidx = -1;
		int urlidx = -1;
		int createdidx = -1;
		int changedidx = -1;
		int viewedidx = -1;
		String[] entry = labels; // entries[0];
		for (int ii = 0; ii < entry.length; ii++) {
			if (entry[ii].equalsIgnoreCase("name") || entry[ii].equalsIgnoreCase("title"))
				titleidx = ii;
			if (entry[ii].equalsIgnoreCase("category"))
				categoryidx = ii;
			if (entry[ii].equalsIgnoreCase("account") || entry[ii].equalsIgnoreCase("username"))
				accountidx = ii;
			if (entry[ii].equalsIgnoreCase("password"))
				passwordidx = ii;
			if (entry[ii].toLowerCase().startsWith("note"))
				notesidx = ii;
			if (entry[ii].equalsIgnoreCase("url"))
				urlidx = ii;
			if (entry[ii].equalsIgnoreCase("created"))
				createdidx = ii;
			if (entry[ii].equalsIgnoreCase("viewed"))
				viewedidx = ii;
			if (entry[ii].equalsIgnoreCase("changed"))
				changedidx = ii;
		}

		if (titleidx == -1) {
			throw new Exception("Input file format is invalid.  Allowed column " +
					"headers are: name or title, category, account or username, password, " +
					"url, note, viewed, changed, created.  Any other labels will be ignored.  The title " +
					"field is mandatory, others are optional.");
		}

		long now = System.currentTimeMillis();
		int exported = 0;
		for (int ii = 0; ii < entries.length; ii++) {
			entry = entries[ii];

			try {
				String title = entry[titleidx];
				if (ring.getItem(title) != null) {
					error("Duplicate entry, skipping.", title, ii);
				} else {
					String category = categoryidx == -1 || categoryidx >= entry.length ? "Unfiled"
							: entry[categoryidx];
					String account = accountidx == -1 || accountidx >= entry.length ? ""
							: entry[accountidx];
					String password = passwordidx == -1 || passwordidx >= entry.length ? ""
							: entry[passwordidx];
					String notes = notesidx == -1 || notesidx >= entry.length ? ""
							: entry[notesidx];
					String url = urlidx == -1 || urlidx >= entry.length ? ""
							: entry[urlidx];
					/* Dates default to time of import if they're not provided. */
					long changed = changedidx == -1 || changedidx >= entry.length ? now
							: parseDate(entry[changedidx].trim(), title, ii);
					long viewed = viewedidx == -1 || viewedidx >= entry.length ? now
							: parseDate(entry[viewedidx].trim(), title, ii);
					long created = createdidx == -1 || createdidx >= entry.length ? now
							: parseDate(entry[createdidx].trim(), title, ii);

					ring.addItem(new Item(ring, account, password, url, notes,
							title, category, created, viewed, changed));
					exported++;
				}
			}
			catch(ArrayIndexOutOfBoundsException e) {
				error("Wrong number of columns.", "UNKNOWN", ii);
			}
		}
		return ring;
	}

	private final String FULL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	/**
	 * Attempt to parse a string into an epoch time, accepts either ISO dates
	 * or raw epoch times.
	 * 
	 * Makes an effort, but it's not very robust.
	 * 
	 * @return Epoch time, or System.currentTimeMillis() if the input
	 * can't be parsed. 
	 */
	private long parseDate(String dateVal, String title, int index) {
		if (dateVal.contains("-")) {
			// ISO date?
			String format;
			if (dateVal.length() > FULL_DATE_FORMAT.length() ) {
				// Has time zone
				format = FULL_DATE_FORMAT + " Z";
			} else {
				// Use as much of the format as we have date to match
				format = FULL_DATE_FORMAT.substring(0, dateVal.length());
			}
			try {
				return new SimpleDateFormat(format).parse(dateVal).getTime();
			} catch (ParseException e) {
				// Fall through to return below.
			}
		} else {
			try {
				return Long.parseLong(dateVal);
			}
			catch(NumberFormatException nfe) {
				// Fall through to return below.
			}
		}
		error("Unparseable date '" + dateVal + "'", title, index);
		return System.currentTimeMillis();
	}
	
	private void error(String msg, String title, int index) {
		System.err.println("WARNING: Entry #" + (index + 1) + " (\"" + title + "\") is invalid:" +
				msg);
	}
}
