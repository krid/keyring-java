/**
 * 
 */
package com.otisbean.keyring.converters;

import java.util.ArrayList;
import java.util.List;

import com.otisbean.keyring.KeyringException;
import com.otisbean.keyring.Ring;

/**
 * Base class for foreign format converters.
 * 
 * @author krid
 */
public abstract class Converter {
		
	/**
	 * Errors encountered during processing are stored here.
	 */
	protected List<String> errors = new ArrayList<String>();

	/**
	 * Does the implementing class require a password for the input file?
	 */
	public boolean needsInputFilePassword;

	/**
	 * Factory method to select a converter subclass based on supplied type.
	 * 
	 * @param type One of keyring|csv|ewallet|codewallet.
	 * @return A converter subclass.
	 * @throws KeyringException On unknown type.
	 */
	public static Converter getConverter(String type) throws KeyringException {
		if ("keyring".equalsIgnoreCase(type)) {
			return new GnuKeyringConverter();
		} else if ("csv".equalsIgnoreCase(type)) {
			return new CSVConverter();
		} else if ("ewallet".equalsIgnoreCase(type)) {
			return new EWalletExportConverter();
		} else if ("codewallet".equalsIgnoreCase(type)) {
			return new CodeWalletExportConverter();
		} else {
			throw new KeyringException("Invalid type: \"" + type + "\".");
		}
	}
	
	/**
	 * Does the actual work of conversion.
	 * 
	 * @param outPassword The password used to encrypt the output file.
	 * @param inPassword The password used to decrypt the input file.  Not
	 * used for all subclasses.
	 * @param inFile The file to be converted.
	 * @param outFile Where to store the Keyring formatted results.
	 * @return The number of records converted.
	 * @throws Exception When bad things happen.
	 */
	public int export(String outPassword, String inPassword, String inFile,
			String outFile) throws Exception {
		Ring ring = convert(inFile, inPassword, outPassword);
		ring.save(outFile);
		return ring.getItems().size();
	}

	public abstract Ring convert(String inFile, String inPassword,
			String outPassword) throws Exception;
		
	/**
	 * Make note of a processing error.
	 */
	protected void logError(String message) {
		errors.add(message);
	}

	/**
	 * Log a message.
	 * 
	 * TODO Currently logs to stdout, really should do something more creative.
	 */
	protected void log(String message) {
		System.out.println(message);
	}
}
