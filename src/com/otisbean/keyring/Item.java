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

import java.security.GeneralSecurityException;

import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * A single item on a Keyring.
 *
 * @author Dirk Bergstrom
 */
public class Item implements JSONAware, Comparable<Item> {
	
	// ENCRYPTED_ATTRS: ['username', 'pass', 'url', 'notes'],
	
	// PLAINTEXT_ATTRS: ['title', 'category', 'created', 'viewed', 'changed'],

	private Ring ring;
	private String username;
	private String pass;
	private String url;
	private String notes;
	private String title;
	private int category;
	private long created;
	private long viewed;
	private long changed;
	private String encryptedData;
	private boolean locked;
	
	/**
	 * Create an Item given all the values.
	 */
	public Item(Ring ring, String username, String pass, String url, String notes,
			String title, String categoryName, long created, long viewed, long changed)
	        throws GeneralSecurityException, KeyringException {
		super();
		this.ring = ring;
		this.username = username;
		this.pass = pass;
		this.url = url;
		this.notes = notes;
		this.title = title;
		this.category = ring.categoryIdForName(categoryName);
		this.created = created;
		this.viewed = viewed;
		this.changed = changed;
		lock();
	}
	
	/**
	 * Create a new Item, setting created/viewed/changed to now.
	 */
	public Item(Ring ring, String username, String pass, String url, String notes,
			String title, int categoryId)
		    throws GeneralSecurityException, KeyringException {
		super();
		this.ring = ring;
		this.username = username;
		this.pass = pass;
		this.url = url;
		this.notes = notes;
		this.title = title;
		this.category = categoryId;
		this.created = this.viewed = this.changed = System.currentTimeMillis();
		lock();
	}
	
	/**
	 * Create an item from a JSONObject sourced from a backup file.
	 * 
	 * The Ring need not have an active password, since the encrypted_data
	 * blob is used as-is.
	 * 
	 * XXX??? Since the encrypted blob isn't decrypted, it could be corrupt. 
	 */
	public Item(Ring ring, JSONObject rawItem) {
		super();
		this.ring = ring;
		encryptedData = (String) rawItem.get("encrypted_data");
		title = (String) rawItem.get("title");
		Object tmp = rawItem.get("category");
		category = (int) (null == tmp ? 0 : (Long) tmp);
		tmp = rawItem.get("created");
		created = null == tmp ? 0 : (Long) tmp;
		tmp = rawItem.get("viewed");
		viewed = null == tmp ? 0 : (Long) tmp;
		tmp = rawItem.get("changed");
		changed = null == tmp ? 0 : (Long) tmp;
		// TODO Unlock and re-lock the item to validate that the encrypted data is valid?
		//unlock();
		//lock();
		locked = true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String toJSONString() {
		if (! locked) {
			try {
				lock();
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			} catch (KeyringException e) {
				throw new RuntimeException(e);
			}
		}
		JSONObject itemJson = new JSONObject();
		itemJson.put("title", title);
		itemJson.put("category", category);
		// Dates are stored as an empty string if undefined
		itemJson.put("created", created == 0 ? "" : created);
		itemJson.put("viewed", viewed == 0 ? "" : viewed);
		itemJson.put("changed", changed == 0 ? "" : changed);
		itemJson.put("encrypted_data", encryptedData);

		return itemJson.toJSONString();
	}
	
	@SuppressWarnings("unchecked")
	public void lock() throws GeneralSecurityException, KeyringException {
		if (locked) {
			throw new KeyringException("Locking an already locked record is wrong");
		}
		JSONObject crypted = new JSONObject();
		crypted.put("username", username);
		crypted.put("pass", pass);
		crypted.put("url", url);
		crypted.put("notes", notes);
		encryptedData = ring.encrypt(crypted.toJSONString(), Ring.ITEM_SALT_LENGTH);
		username = pass = url = notes = "";
		locked = true;
	}
	
	public void unlock() throws GeneralSecurityException, KeyringException {
		String decryptedData;
		decryptedData = ring.decrypt(encryptedData);

        JSONObject obj;
		try {
			obj = (JSONObject) ring.parser.parse(decryptedData);
		} catch (ParseException e) {
			// ParseException's toString() method returns a good error message
			throw new KeyringException("Unparseable JSON data: " + e);
		}
		username = (String) obj.get("username");
		pass = (String) obj.get("pass");
		url = (String) obj.get("url");
		notes = (String) obj.get("notes");
		locked = false;
	}
	
	public String getUsername() throws GeneralSecurityException, KeyringException {
		if (locked) {
			unlock();
		}
		return username;
	}
	public void setUsername(String username) throws GeneralSecurityException, KeyringException {
		if (locked) {
			unlock();
		}
		this.username = username;
	}
	public String getPass() throws GeneralSecurityException, KeyringException {
		if (locked) {
			unlock();
		}
		return pass;
	}
	public void setPass(String pass) throws GeneralSecurityException, KeyringException {
		if (locked) {
			unlock();
		}
		this.pass = pass;
	}
	public String getUrl() throws GeneralSecurityException, KeyringException {
		if (locked) {
			unlock();
		}
		return url;
	}
	public void setUrl(String url) throws GeneralSecurityException, KeyringException {
		if (locked) {
			unlock();
		}
		this.url = url;
	}
	public String getNotes() throws GeneralSecurityException, KeyringException {
		if (locked) {
			unlock();
		}
		return notes;
	}
	public void setNotes(String notes) throws GeneralSecurityException, KeyringException {
		if (locked) {
			unlock();
		}
		this.notes = notes;
	}
	public String getTitle() {
		return title;
	}
	public String getEncryptedData() {
		return encryptedData;
	}
	public void setEncryptedData(String encryptedData) {
		this.encryptedData = encryptedData;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	public long getCreated() {
		return created;
	}
	public void setCreated(long created) {
		this.created = created;
	}
	public long getViewed() {
		return viewed;
	}
	public void setViewed(long viewed) {
		this.viewed = viewed;
	}
	public long getChanged() {
		return changed;
	}
	public void setChanged(long changed) {
		this.changed = changed;
	}
	public int getCategoryId() {
		return category;
	}
	public void setCategoryId(int cat) {
		this.category = cat;
	}
	public String getCategory() {
		return ring.categoryNameForId(category);
	}
	public void setCategory(String categoryName) {
		this.category = ring.categoryIdForName(categoryName);
	}

	/**
	 * @return the ring
	 */
	public Ring getRing() {
		return ring;
	}

	/**
	 * @param ring the ring to set
	 */
	public void setRing(Ring ring) {
		this.ring = ring;
	}

	@Override
	public String toString() {
		return title;
	}
	
	@Override
	public int compareTo(Item other) {
		return title.compareTo(other.title);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Item) {
			return title.equals(((Item) other).title);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return title.hashCode();
	}
}
