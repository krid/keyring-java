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

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.otisbean.keyring.Item;
import com.otisbean.keyring.Ring;

import net.sf.gnukeyring.KeyringEntry;
import net.sf.gnukeyring.decoder.PDBKeyringLibrary;

/**
 * Export GnuKeyring as Keyring for webOS.
 * 
 * @author Dirk Bergstrom
 */
public class GnuKeyringConverter extends Converter {

	public GnuKeyringConverter() {
		needsInputFilePassword = true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public int export(String outPassword, String pdbPassword, String pdbFile,
			String outFile) throws Exception {
		PDBKeyringLibrary keylib = new net.sf.gnukeyring.decoder.PDBKeyringLibrary();
		keylib.setFilename(new File(pdbFile));
		if (!keylib.unlock(pdbPassword)) {
			throw new Exception("Can't unlock " + pdbFile);
		}
		List entries = keylib.getEntries();
		Ring ring = new Ring(SCHEMA_VERSION, outPassword);

		int exported = 0;
		for (Iterator i = entries.iterator(); i.hasNext();) {
			KeyringEntry entry = (KeyringEntry) i.next();

			String name = (String) entry.getName();
			String category = (String) entry.getCategory();
			String account = (String) entry.getField("Account");
			String password = (String) entry.getField("Password");
			String notes = (String) entry.getField("Notes");
			/* We'll pretend that each item was created on its changed date. */
			long changed = ((Date) entry.getField("Changed")).getTime();

			ring.addItem(new Item(ring, account, password, "", notes, name,
					category, changed, changed, changed));
			exported++;
		}

		writeOutputFile(ring, outFile);
		return exported;
	}
}
