/**
 * 
 */
package com.otisbean.keyring.converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import com.otisbean.keyring.Ring;

/**
 * Base class for foreign format converters.
 * 
 * @author krid
 */
public abstract class Converter {
	
	/**
	 * Version 3 was the first to have categories.
	 */
	public static final int SCHEMA_VERSION = 4;
	
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
	 * @throws Exception If the supplied type is invalid.
	 */
	public static Converter getConverter(String type) throws Exception {
		if ("keyring".equalsIgnoreCase(type)) {
			return new GnuKeyringConverter();
		} else if ("csv".equalsIgnoreCase(type)) {
			return new CSVConverter();
		} else if ("ewallet".equalsIgnoreCase(type)) {
			return new EWalletExportConverter();
		} else if ("codewallet".equalsIgnoreCase(type)) {
			return new CodeWalletExportConverter();
		} else {
			throw new Exception("Invalid type: \"" + type + "\".");
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
	public abstract int export(String outPassword, String inPassword, String inFile,
			String outFile) throws Exception;

	/**
	 * Write the converted data to the specified file.
	 * 
	 * @param ring The Ring containing converted data.
	 * @param outFile Path to the output file
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	protected void writeOutputFile(Ring ring, String outFile)
	        throws IOException, GeneralSecurityException {
		OutputStream os;
		if (outFile.equals("-")) {
			os = System.out;
		} else {
			os = new FileOutputStream(new File(outFile));
		}
		OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
		writer.write(ring.getExportData());
		if (outFile.equals("-")) {
			writer.write("\n");
			writer.flush();
		} else {
			writer.close();
		}
	}
	
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
