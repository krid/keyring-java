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

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import net.iharder.Base64;

import org.json.simple.JSONObject;

/**
 * A Keyring of Items.
 * 
 * The Mojo.Model.encrypt/decrypt API hides a lot of complexity under the
 * hood.  I did a lot of investigating and pestered Palm repeatedly, and
 * finally got the details of what their API does.  The following quotes
 * are from Palm engineers:
 * 
 * """We use blowfish 64-bit block cipher. The input string is treated
 * as UTF-8 bytes which become the "key." The output from the
 * encrypter (which is binary) is base64 encoded, and returned to
 * JavaScript as a string. Decrypt takes the base64'ed string,
 * converts it to the binary stream, and runs the blowfish 64-bit
 * block decoder on it. It is assumed the same key "string" is used
 * in both cases."""
 *
 * """"At it's core our Mojo Blowfish is using openssl to implement the
 * algorithm. We pass the full key string in (no padding or truncation).
 * We do pass in an initialization vector (8 zero bytes) and use CFB64
 * Blowfish."""
 *
 * This is documented on Palm's developer forums here:
 *
 * http://developer.palm.com/distribution/viewtopic.php?f=8&t=1281
 *
 * @author Dirk Bergstrom
 */
public class Ring {

	public static final int DB_SALT_LENGTH = 16;
	public static final int ITEM_SALT_LENGTH = 4;
	
	private String salt;
	private String checkData;
	private SecretKeySpec key;
	private int schemaVersion;
	private Cipher cipher;
	private Map<Integer, String> categoriesById = new HashMap<Integer, String>();
	private Map<String, Integer> categoriesByName = new HashMap<String, Integer>();
	private Map<String, Item> db = new HashMap<String, Item>();
	private int nextCategory = 1;
	private Random rnd;
	
	public Ring(int schemaVersion, String password)
	        throws GeneralSecurityException, UnsupportedEncodingException {
		this.schemaVersion = schemaVersion;
		this.rnd = new Random();
		salt = saltString(12, null);
		
		/* The following code looks like a lot of monkey-motion, but it yields
		 * results compatible with the on-phone Keyring Javascript and Mojo code.
		 * 
		 * In newPassword() in ring.js, we have this (around line 165):
		 * this._key = b64_sha256(this._salt + newPassword); */
		byte[] keyBase = (salt + password).getBytes("UTF-8");
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(keyBase, 0, keyBase.length);
		byte[] keyHash = md.digest();
		String paddedBase64Key = Base64.encodeBytes(keyHash);
		/* The Javascript SHA-256 library used in Keyring doesn't pad base64 output,
		 * so we need to trim off any trailing "=" signs. */
		String base64Key = paddedBase64Key.replace("=", "");
		byte[] keyBytes = base64Key.getBytes("UTF-8");
		
		/* Keyring passes data to Mojo.Model.encrypt(key, data), which eventually
		 * make a JNI call to OpenSSL's blowfish api.  The following is the
		 * equivalent in straight up JCE. */
		key = new SecretKeySpec(keyBytes, "Blowfish");
	    cipher = Cipher.getInstance("Blowfish/CFB64/NoPadding");
	    IvParameterSpec iv = new IvParameterSpec( new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 } );
	    try {
	    	cipher.init(Cipher.ENCRYPT_MODE, key, iv);
	    }
	    catch (InvalidKeyException ike) {
	    	throw new GeneralSecurityException("InvalidKeyException: " +
	    			ike.getLocalizedMessage() + "\nYou (probably) need to " +
	    			"install the \"Java Cryptography Extension (JCE) " +
	    			"Unlimited Strength Jurisdiction Policy\" files.  Go to " +
	    			"http://java.sun.com/javase/downloads/index.jsp, download them, " +
	    			"and follow the instructions.");
	    }
	    
	    checkData = encrypt("{" + base64Key + "}", 8);
	}
	
	/**
	 * The format for Keyring export is:
	 * {
	 *  schema_version: this.SCHEMA_VERSION,
	 *  salt: this._salt,
	 *  db: encrypt(JSON.stringify(this._dataObject()))
	 * }
	 *
	 * Where _dataObject() returns
	 * {
	 *     db: this.db,
	 *     categories: this.categories,
     *     crypt: {
     *         salt: this._salt,
     *         checkData: this._checkData
     *     },
     *     prefs: this.prefs
     * }
	 * 
	 * @return a JSON string of the export-formatted data.
	 * @throws GeneralSecurityException
	 * @throws UnsupportedEncodingException 
	 */
	@SuppressWarnings("unchecked")
	public String getExportData() throws GeneralSecurityException, UnsupportedEncodingException {
		JSONObject dataObject = new JSONObject();
		dataObject.put("db", db);
		dataObject.put("categories", categoriesById);
		JSONObject crypt = new JSONObject();
		crypt.put("salt", salt);
		crypt.put("checkData", checkData);
		dataObject.put("crypt", crypt);

		JSONObject export = new JSONObject();
		export.put("schema_version", schemaVersion);
		export.put("salt", salt);
		export.put("db", encrypt(dataObject.toJSONString(), DB_SALT_LENGTH));
		
		return export.toJSONString();
	}
	
	/**
	 * Encrypt the given data with our key, prepending saltLength random
	 * characters.

	 * @return Base64 encoded representation of the encrypted data.
	 */
	String encrypt(String data, int saltLength) throws GeneralSecurityException, UnsupportedEncodingException {
		String salted = saltString(saltLength, data);
		byte[] crypted = cipher.doFinal(salted.getBytes("UTF-8"));
		return Base64.encodeBytes(crypted);
	}

	/**
	 * Generate random salt characters, optionally prepending them to the
	 * supplied suffix.
	 * 
	 * @param numChars Generate this many random characters.
	 * @param suffix If non-null, append to the salt. 
	 */
	private String saltString(int numChars, String suffix) {
		StringBuilder salted = new StringBuilder();
		for (int i = 0; i < numChars; i++) {
			// Random character from ASCII 33 to 122 
			char c = (char) (rnd.nextInt(89) + 33);
			salted.append(c);
		}
		if (null != suffix) {
			salted.append(suffix);
		}
		return salted.toString();
	}
	
	public void addItem(Item item) {
		db.put(item.getTitle(), item);
	}
	
	public Item getItem(String title) {
		return db.get(title);
	}

	public Collection<Item> getItems() {
		return db.values();
	}

	public String getSalt() {
		return salt;
	}

	public synchronized int categoryIdForName(String categoryName) {
		if ("Unfiled".equalsIgnoreCase(categoryName)) {
			return 0;
		}
		Integer retval = categoriesByName.get(categoryName);
		if (null == retval) {
			retval = nextCategory;
			nextCategory++;
			categoriesByName.put(categoryName, retval);
			categoriesById.put(retval, categoryName);
		}
		return retval;
	}
	
	public String categoryNameForId(int categoryid) {
		if (0 == categoryid) {
			return "Unfiled";
		}
		String retval = categoriesById.get(categoryid);
		if (null == retval) {
			throw new RuntimeException("No category found for id " + categoryid);
		}
		return retval;
	}
}
