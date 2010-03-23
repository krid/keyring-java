/*
KeyringEditor

Copyright 2004 Markus Griessnig
Vienna University of Technology
Institute of Computer Technology

KeyringEditor is based on:
Java Keyring v0.6
Copyright 2004 Frank Taylor <keyring@lieder.me.uk>

These programs are distributed in the hope that they will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.
*/

// ConvertDialog.java

// 07.12.2004
// 26.01.2005: check for empty password

package com.otisbean.keyring.gui;

import javax.swing.event.*;
import java.awt.event.*;
import javax.swing.*;
import java.beans.*; // property change stuff
import java.util.*;
import java.awt.*;
import java.io.*;

import com.otisbean.keyring.Item;
import com.otisbean.keyring.KeyringException;
import com.otisbean.keyring.Ring;
import com.otisbean.keyring.converters.Converter;

/**
 * This dialog allows the user to convert a database to different keyring database formats.
 */
public class ImportDialog extends JDialog implements ActionListener, PropertyChangeListener {
	// ----------------------------------------------------------------
	// variables
	// ----------------------------------------------------------------
	/**
	 * Reference to the Gui frame
	 */
	private Frame frame;

	private JComboBox cboFormat;
	private JPasswordField inPassword;
	private JPasswordField outPassword;
	private Vector<String> format;

	private JOptionPane optionPane;

	private String btnString1 = "Select File to Import";
	private String btnString2 = "Cancel";

	private Editor editor;
	
	/**
	 * True if button "Cancel" pressed, otherwise false
	 */
	private boolean cancelled = false;

	// ----------------------------------------------------------------
	// constructor
	// ----------------------------------------------------------------
	/**
	 * Default constructor generates Dialog.
	 *
	 * @param frame Reference to the Gui frame
	 * @param ring Reference to class Ring
	 * @param fromFormat Database format of the loaded database
	 */
	public ImportDialog(Frame frame, Editor editor) {
		super(frame, "Import Database", true);

		this.frame = frame;
		this.editor = editor;

		// TODO move format choices to Converter
		format = new Vector<String>();
		format.add("Keyring for PalmOS");
		format.add("CodeWallet");
		format.add("eWallet");
		format.add("CSV");

		cboFormat = new JComboBox(format);
		cboFormat.setModel(new DefaultComboBoxModel(format));
		// FIXME need to toggle inPassword input based on format

		inPassword = new JPasswordField(20);
		outPassword = new JPasswordField(20);

		// labels
		String msgString1 = "Format: ";
		String msgString2 = "Password for import file: ";
		String msgString3 = "Password for new database: ";

		Object[] array = {msgString1, cboFormat, msgString2, inPassword,
				msgString3, outPassword};

		Object[] options = {btnString1, btnString2};

		// generate dialog
		optionPane = new JOptionPane(array,
			JOptionPane.PLAIN_MESSAGE,
			JOptionPane.YES_NO_OPTION,
			null,
			options,
			options[0]);

		setContentPane(optionPane);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
			/*
			 * Instead of directly closing the window,
			 * we're going to change the JOptionPane's
			 * value property.
			 */
			 optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
			}
		});

		addComponentListener(new ComponentAdapter() {
			public void componentShown(ComponentEvent ce) {
				inPassword.requestFocusInWindow();
			}
		});

		//Register an event handler that puts the text into the option pane.
		//allCategories.addActionListener(this);

		//Register an event handler that reacts to option pane state changes.
		optionPane.addPropertyChangeListener(this);
	}

	// ----------------------------------------------------------------
	// public ---------------------------------------------------------
	// ----------------------------------------------------------------

	/**
	 * This method returns status of variable cancelled.
	 *
	 * @return variable cancelled
	 */
	public boolean getCancelled() {
		return cancelled;
	}

	/**
	 * This method is empty.
	 *
	 * @param e the ActionEvent to process
	 */
	public void actionPerformed(ActionEvent e) {
	}

	/**
	 * This method processes the pressed button.
	 * If button is OK database is converted to selected keyring database format and
	 * encrypted with new password and selected cryptographic function.
	 * Otherwise variable cancelled is set to true.
	 *
	 * @param e the PropertyChangeEvent to process
	 */
	public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();

		if (isVisible()
			&& (e.getSource() == optionPane)
			&& (JOptionPane.VALUE_PROPERTY.equals(prop) ||
				JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {

			Object value = optionPane.getValue();

			if(value == JOptionPane.UNINITIALIZED_VALUE) {
				//ignore reset
				return;
			}

			String type = null;
			int typeIndex = cboFormat.getSelectedIndex();
			// TODO move typeIndex decoding into Converter
			switch(typeIndex) {
			    case 0: type = "keyring"; break;
				case 1: type = "codewallet"; break;
				case 2: type = "ewallet"; break;
				case 3: type = "csv"; break;
			}
			Converter converter;
			try {
				converter = Converter.getConverter(type);
			} catch (KeyringException e1) {
				throw new RuntimeException("Unknown/unsupported conversion type '" + type + "' selected.");
			}
			
			//Reset the JOptionPane's value.
			//If you don't do this, then if the user
			//presses the same button next time, no
			//property change event will be fired.
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

			if(btnString1.equals(value)) {
				// check for empty passwords
				if (outPassword.getPassword().length == 0) {
					JOptionPane.showMessageDialog(ImportDialog.this,
							"New database password required\n",
							"Error", JOptionPane.ERROR_MESSAGE);
					cancelled = true;
					outPassword.requestFocusInWindow();
					return;
				}
				if (converter.needsInputFilePassword &&
						inPassword.getPassword().length == 0) {
					JOptionPane.showMessageDialog(ImportDialog.this,
							"Import password required\n",
                    	"Error", JOptionPane.ERROR_MESSAGE);
					cancelled = true;
                    inPassword.requestFocusInWindow();
                    return;
                }

				// ok
				JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle("Select File to Import");
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				chooser.setCurrentDirectory(editor.previousDirectory);
				chooser.setMultiSelectionEnabled(false);

				int returnVal = chooser.showOpenDialog(this.frame);

				if(returnVal == JFileChooser.APPROVE_OPTION) {
				try {
					File selectedFile = chooser.getSelectedFile();
					String importFilename = selectedFile.getCanonicalPath();

					// convert database
					// FIXME need to be more secure with passwords
					editor.setRing(converter.convert(importFilename,
							new String(inPassword.getPassword()),
					        new String(outPassword.getPassword())));
				}
				catch(Exception ex) {
					// cancel
					cancelled = true;

					JOptionPane.showMessageDialog(frame,
						"Convert database: " + ex.getMessage(),
						"Error",
						JOptionPane.ERROR_MESSAGE);

					ex.printStackTrace(System.err);
				}
				}
				else {
					// cancel
					cancelled = true;
				}
			}
			else {
				// cancel
				cancelled = true;
			}

			clearAndHide();
		}
	}

	// ----------------------------------------------------------------
	// private --------------------------------------------------------
	// ----------------------------------------------------------------

	/**
	 * This method hides the dialog.
	 */
	private void clearAndHide() {
		inPassword.setText(null);
		setVisible(false);
	}
}
