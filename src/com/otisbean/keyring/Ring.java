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
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import net.iharder.Base64;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.Ostermiller.util.CSVPrinter;

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

	/**
	 * Version 4 introduced salting of data.
	 * Version 3 was the first to have categories.
	 */
	public static final int SCHEMA_VERSION = 4;

	public static final int DB_SALT_LENGTH = 16;
	public static final int ITEM_SALT_LENGTH = 4;
	
	private String salt;
	private String checkData;
	private SecretKeySpec key;
	private IvParameterSpec iv;
	private int schemaVersion;
	private Cipher cipher;
	private Map<Integer, String> categoriesById = new HashMap<Integer, String>();
	private SortedMap<String, Integer> categoriesByName = new TreeMap<String, Integer>();
	private Map<String, Item> db = new HashMap<String, Item>();
	private int nextCategory = 1;
	private Random rnd;
	JSONParser parser;

	private boolean fullyLoaded;

	private String cryptedDb;

	private JSONObject prefs;
	
	/**
	 * Initialize the Ring with a String password.
	 * 
	 * @param password
	 * @throws GeneralSecurityException
	 * @deprecated Need to use the more secure char[] method.
	 */
	public Ring(String password) throws GeneralSecurityException {
		this(password.toCharArray());
	}
	
	public Ring(char[] password) throws GeneralSecurityException {
		this();
	    String rawCheckData = initCipher(password);
	    checkData = encrypt(rawCheckData, 8);
	}

	public Ring() throws GeneralSecurityException {
		log("Ring()");
		this.schemaVersion = SCHEMA_VERSION;
		this.rnd = new Random();
		salt = saltString(12, null);
		cipher = Cipher.getInstance("Blowfish/CFB64/NoPadding");
		// TODO we don't need the parser if we're just doing format conversion
		setDefaultCategories();
		parser = new JSONParser();
	}
	
	/**
	 * Initialize the cipher object and create the key object.
	 * 
	 * @param password
	 * @return A checkData string, which can be compared against the existing
	 * one to determine if the password is valid.
	 * @throws GeneralSecurityException
	 */
	private String initCipher(char[] password)
			throws GeneralSecurityException {
		log("initCipher()");
		String base64Key = null;
		try {
			// Convert a char array into a UTF-8 byte array
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStreamWriter out = new OutputStreamWriter(baos, "UTF-8");
			try {
				out.write(password);
				out.close();
			} catch (IOException e) {
				// the only reason this would throw is an encoding problem.
				throw new RuntimeException(e.getLocalizedMessage());
			}
			byte[] passwordBytes = baos.toByteArray();

			/* The following code looks like a lot of monkey-motion, but it yields
			 * results compatible with the on-phone Keyring Javascript and Mojo code.
			 * 
			 * In newPassword() in ring.js, we have this (around line 165):
			 * this._key = b64_sha256(this._salt + newPassword); */
			byte[] saltBytes = salt.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(saltBytes, 0, saltBytes.length);
			md.update(passwordBytes, 0, passwordBytes.length);
			byte[] keyHash = md.digest();
			String paddedBase64Key = Base64.encodeBytes(keyHash);
			/* The Javascript SHA-256 library used in Keyring doesn't pad base64 output,
			 * so we need to trim off any trailing "=" signs. */
			base64Key = paddedBase64Key.replace("=", "");
			byte[] keyBytes = base64Key.getBytes("UTF-8");

			/* Keyring passes data to Mojo.Model.encrypt(key, data), which eventually
			 * make a JNI call to OpenSSL's blowfish api.  The following is the
			 * equivalent in straight up JCE. */
			key = new SecretKeySpec(keyBytes, "Blowfish");
			iv = new IvParameterSpec( new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 } );
		}
		catch (UnsupportedEncodingException e) {
			// This is a bit dodgy, but handling a UEE elsewhere is foolish
			throw new GeneralSecurityException(e.getLocalizedMessage());
		}
		return "{" + base64Key + "}";
	}
	
	public boolean validatePassword(char[] password) throws GeneralSecurityException {
		log("validatePassword()");
		String tmpCheckData = initCipher(password);
		if (! fullyLoaded) {
			/* Startup in process.  See if the supplied password will
			 * decrypt the db. */
			return decryptLoadedData();
		} else {
			return decrypt(checkData).equals(tmpCheckData);
		}
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
	 */
	@SuppressWarnings("unchecked")
	public JSONObject getExportData() throws GeneralSecurityException {
		log("getExportData()");
		JSONObject dataObject = new JSONObject();
		dataObject.put("db", db);
		dataObject.put("categories", categoriesById);
		JSONObject crypt = new JSONObject();
		crypt.put("salt", salt);
		crypt.put("checkData", checkData);
		dataObject.put("crypt", crypt);
		
		if (null != prefs) {
			dataObject.put("prefs", prefs);
		}

		JSONObject export = new JSONObject();
		export.put("schema_version", schemaVersion);
		export.put("salt", salt);
		export.put("db", encrypt(dataObject.toJSONString(), DB_SALT_LENGTH));
		
		return export;
	}
	
	/**
	 * Encrypt the given data with our key, prepending saltLength random
	 * characters.

	 * @return Base64 encoded representation of the encrypted data.
	 */
	String encrypt(String data, int saltLength) throws GeneralSecurityException {
		log("encrypt()");
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
		String salted = saltString(saltLength, data);
		byte[] crypted;
		byte[] saltedBytes;
		try {
			saltedBytes = salted.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new GeneralSecurityException(e.getLocalizedMessage());
		}
		crypted = cipher.doFinal(saltedBytes);
		return Base64.encodeBytes(crypted);
	}

	String decrypt(String cryptext) throws GeneralSecurityException {
		log("decrypt()");
		try {
			cipher.init(Cipher.DECRYPT_MODE, key, iv);
		}
		catch (InvalidKeyException ike) {
			throw new GeneralSecurityException("InvalidKeyException: " +
					ike.getLocalizedMessage() + "\nYou (probably) need to " +
					"install the \"Java Cryptography Extension (JCE) " +
					"Unlimited Strength Jurisdiction Policy\" files.  Go to " +
					"http://java.sun.com/javase/downloads/index.jsp, download them, " +
			"and follow the instructions.");
		}
		byte[] crypted;
		try {
			crypted = Base64.decode(cryptext);
		} catch (IOException e) {
			throw new GeneralSecurityException(e.getLocalizedMessage());
		}
		byte[] decrypted = cipher.doFinal(crypted);
		String salted;
		try {
			salted = new String(decrypted, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new GeneralSecurityException(e.getLocalizedMessage());
		}
        // Remove any leading non-JSON salt characters
        return salted.replaceAll("^[^\\{]*\\{", "{");
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
	
	public boolean removeItem(Item item) {
		return null != db.remove(item.getTitle());		
	}
	
	public void addItem(Item item) {
		db.put(item.getTitle(), item);
		fullyLoaded = true;
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

	/**
	 * @return Ordered list of category names, without the "All" pseudo-category,
	 * and with "Unfiled" at the top.
	 */
	public Vector<String> getCategories() {
		Vector<String> cats = new Vector<String>(categoriesByName.keySet());
		cats.remove("All");
		cats.remove("Unfiled");
		cats.add(0, "Unfiled");
		return cats;
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

	/**
	 * Put the default "Unfiled" and "All" categories into the Maps.
	 */
	private void setDefaultCategories() {
		categoriesById.put(0, "Unfiled");
		categoriesById.put(-1, "All");
		categoriesByName.put("Unfiled", 0);
		categoriesByName.put("All", -1);
	}

	public void load(String inFile) throws IOException, KeyringException {
		log("load(" + inFile + ")");
		InputStream is;
		if (inFile.equals("-")) {
			is = System.in;
		} else if (inFile.startsWith("http")) {
			is = new URL(inFile).openStream();
		} else {
			is = new FileInputStream(new File(inFile));
		}
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        JSONObject obj;
		try {
			obj = (JSONObject) parser.parse(reader);
		} catch (ParseException e) {
			// ParseException's toString() method returns a good error message
			throw new KeyringException("Unparseable JSON data: " + e);
		}
        // Loaded data has three attrs, 'db', 'salt' & 'schema_version'
        salt = (String) obj.get("salt");
        long dbSchemaVersion = (Long) obj.get("schema_version");
        if (schemaVersion != dbSchemaVersion) {
        	// TODO Handle other versions sanely
        	throw new KeyringException("Incompatible schema version " + dbSchemaVersion);
        }
        cryptedDb = (String) obj.get("db");
	}
	
	/**
	 * Attempt to decrypt the loaded data with the supplied key.  If it parses,
	 * the key is good, and loading is complete.  If not, it's a bad password.
	 * @throws GeneralSecurityException 
	 */
	@SuppressWarnings("unchecked")
	private boolean decryptLoadedData() throws GeneralSecurityException {
		log("decryptLoadedData()");
		JSONObject obj;
		try {
			String decryptedJson = decrypt(cryptedDb);
			obj = (JSONObject) parser.parse(decryptedJson);
		}
		catch(ParseException e) {
			/* Can't parse decrypted data.  This is almost always due to a  
			 * bad password, but it's possible that the db is corrupt.
			 * Unfortunately, there's no good way to tell the difference...
			 * 
			 * TODO Hmmm, we could check to see if the last character is a
			 * closing curly brace... */
			return false;
		}
		// Clear temp storage
		cryptedDb = null;
		log("Depot data loaded");

		// We've got our data, pull it apart into usable pieces
		// TODO What if the decrypted data isn't a Keyring backup?
		Map<String, JSONObject> rawDb = (Map<String, JSONObject>) obj.get("db");
		for (Map.Entry<String, JSONObject> ent : rawDb.entrySet()) {
			String title = ent.getKey();
			JSONObject rawItem = ent.getValue();
			db.put(title, new Item(this, rawItem));
		}
		
		// Handle categories
		categoriesById = new HashMap<Integer, String>();
		categoriesByName = new TreeMap<String, Integer>();
		Object tmp = obj.get("categories");
		if (null != tmp) {
			Map<String, String> tmpCats = (Map<String, String>) tmp;
			for (Map.Entry<String, String> cat : tmpCats.entrySet()) {
				int id = Integer.parseInt(cat.getKey());
				categoriesById.put(id, cat.getValue());
				categoriesByName.put(cat.getValue(), id);
			}
		}
		// make sure we always have the "all" and "unfiled" categories
		setDefaultCategories();
		
		checkData = (String) ((JSONObject) obj.get("crypt")).get("checkData");

		// For now, just stash prefs as a JSONObject
		prefs = (JSONObject) obj.get("prefs");
		
		fullyLoaded = true;
		
		log("Depot data processed");
		return true;
	}

	private Writer getWriter(String outFile)
	        throws IOException, GeneralSecurityException {
		OutputStream os;
		if (outFile.equals("-")) {
			os = System.out;
		} else {
			os = new FileOutputStream(new File(outFile));
		}
		OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
		return writer;
	}
	
	private void closeWriter(Writer writer, String outFile) 
	        throws IOException {
		if (outFile.equals("-")) {
			writer.write("\n");
			writer.flush();
		} else {
			writer.close();
		}
	}
	
	/**
	 * Return ISO date representation of the epoch time
	 * 
	 * @param epoch
	 * @param includeTime If true, append HH:mm:ss
	 * @return
	 */
	public String formatDate(long epoch, boolean includeTime) {
		String format;
		if (includeTime) {
			format = "yyyy-MM-dd HH:mm:ss ZZZZ";
		} else {
			format = "yyyy-MM-dd";
		}
		return new SimpleDateFormat(format).format(new Date(epoch));
	}
	
	/**
	 * Export data to the specified file.
	 * 
	 * @param outFile Path to the output file
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public void save(String outFile)
        	throws IOException, GeneralSecurityException {
		log("save(" + outFile + ")");
		if (outFile.startsWith("http")) {
			URL url = new URL(outFile);
			URLConnection urlConn = url.openConnection(); 
		    urlConn.setDoInput(true); 
		    urlConn.setDoOutput(true); 
		    urlConn.setUseCaches(false); 
		    urlConn.setRequestProperty ("Content-Type", "application/x-www-form-urlencoded");

		    DataOutputStream dos = new DataOutputStream (urlConn.getOutputStream()); 
		    String message = "data=" + URLEncoder.encode(getExportData().toJSONString(), "UTF-8");
		    dos.writeBytes(message); 
		    dos.flush(); 
		    dos.close();

		    // the server responds by saying 
		    // "OK" or "ERROR: blah blah"

		    BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream())); 
		    String s = br.readLine(); 
		    if (! s.equals("OK")) {
		    	StringBuilder sb = new StringBuilder();
		    	sb.append("Failed to save to URL '");
		    	sb.append(url);
		    	sb.append("': ");
		    	while ((s = br.readLine()) != null) {
		    		sb.append(s);
		    	}
		    	throw new IOException(sb.toString());
		    }
		    br.close(); 
		} else {
			Writer writer = getWriter(outFile);
			getExportData().writeJSONString(writer);
			closeWriter(writer, outFile);
		}
	}
	
	public void exportToCSV(String outFile)
	        throws IOException, GeneralSecurityException, KeyringException {
		log("exportToCSV(" + outFile + ")");
		Writer writer = getWriter(outFile);
		CSVPrinter csv = new CSVPrinter(writer);
		csv.writeln(new String[] {"title", "username", "password", "url",
				"category", "created", "viewed", "changed", "notes"});
		for (Item i : db.values()) {
			csv.write(i.getTitle());
			csv.write(i.getUsername());
			csv.write(i.getPass());
			csv.write(i.getUrl());
			csv.write(i.getCategory());
			csv.write(formatDate(i.getCreated(), true));
			csv.write(formatDate(i.getViewed(), true));
			csv.write(formatDate(i.getChanged(), true));
			csv.writeln(i.getNotes());
		}
		closeWriter(writer, outFile);
	}
	
	private void log(String message) {
		System.err.println(message);
	}
}
