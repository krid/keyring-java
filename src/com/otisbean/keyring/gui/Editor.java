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

// Editor.java

// 25.11.2004

// 01.12.2004: Keyring database format 5 support added
// 05.12.2004: MenuItem Tools - Convert database added
// 12.01.2004: Model.writeNewDatabase() in main() added
// 24.05.2005: showitem - check no category

package com.otisbean.keyring.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Date;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import com.otisbean.keyring.Item;
import com.otisbean.keyring.Ring;

/**
 * This class handles the gui.
 */
public class Editor extends Gui {
	private static final boolean DEBUG = true;

	// ----------------------------------------------------------------
	// variables
	// ----------------------------------------------------------------
	/**
	 * Separates levels in an item title for the tree view
	 */
	protected char SEPARATOR = '/'; // item title separator

	/**
	 * Last directory to load from
	 */
	File previousDirectory = null; // last directory to load from

	/**
	 * Current loaded keyring database
	 */
	private String dbFilename = "keyring.json"; // default database to save to

	private Gui gui;
	private Ring ring;
	private JFrame frame;
	private PasswordTimeoutWorker timeoutThread;

	// flags
	/**
	 * Show button "Save" only when item text changes (true)
	 */
    private boolean textFieldChanged = true; // show button save only when text changes

	/**
	 * Show item password in clear text (true)
	 */
	private boolean showPassword = false;

	/**
	 * True when application is locked
	 */
    private boolean locked = false;

	// ----------------------------------------------------------------
	// main -----------------------------------------------------------
	// ----------------------------------------------------------------
	/**
	 * main
	 *
	 * @param argv Commandline parameters
	 */
	public static void main(String[] argv) {
		String dbFilename = null;

		Editor myEditor = new Editor();

		myEditor.frame = new JFrame(FRAMETITLE);
		myEditor.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// check command line parameters
		if(argv.length > 1) {
			System.out.println("Usage: java -jar KeyringEditor.jar [keyring-database]");
			return;
		}

		if(argv.length > 0) {
			dbFilename = argv[0];
		}

		// setup gui
		try {
			myEditor.setupGui(dbFilename);
		}
		catch(Exception e) {
			myEditor.msgError(e, "main", true);
		}
	}

	// ----------------------------------------------------------------
	// public ---------------------------------------------------------
	// ----------------------------------------------------------------

	/**
	 * Returns filename of the current loaded keyring database.
	 *
	 * @return Filename
	 */
	public String getFilename() {
		return this.dbFilename;
	}

	/**
	 * Returns separator for title levels.
	 *
	 * @return Separator
	 */
	public char getSeparator() {
		return this.SEPARATOR;
	}

	/**
	 * Returns reference to class Ring.
	 *
	 * @return Reference to class Ring
	 */
	public Ring getRing() {
		return this.ring;
	}
	
	public void setRing(Ring ring) {
		this.ring = ring;
	}

	// ----------------------------------------------------------------
	// private --------------------------------------------------------
	// ----------------------------------------------------------------

	/**
	 * Checks if a file already exists and show warning dialog.
	 *
	 * @param filename Filename
	 *
	 * @return False if file operation should be cancelled
	 */
	private boolean fileDoesntExistOrOverwriteConfirmed(File file) {
		boolean ok = true;
		if (file.exists()) {
			int n = JOptionPane.showConfirmDialog(
				frame, "File already exists. Continue?",
				"Warning",
				JOptionPane.YES_NO_OPTION);

			if (n == JOptionPane.NO_OPTION) {
				ok = false;
			}
		}
		return ok;
	}

	/**
	 * Shows a error message.
	 *
	 * @param e Exception
	 * @param info User-defined text
	 * @param showStack True if Exception Stack Trace should be displayed
	 */
	private void msgError(Exception e, String info, boolean showStack) {
		JOptionPane.showMessageDialog(frame,
			info + ": " + e.getMessage(),
			"Error",
			JOptionPane.ERROR_MESSAGE);

		if(showStack || DEBUG) {
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Shows a information message.
	 *
	 * @param info User-defined text
	 */
	private void msgInformation(String info) {
		JOptionPane.showMessageDialog(frame,
			info,
			"Information",
			JOptionPane.INFORMATION_MESSAGE);
	}


	// setupGui -------------------------------------------------------
	/**
	 * Loads menubar, adds ActionListeners, starts PasswordTimeout Thread.
	 *
	 * @param dbFilename Keyring database or null
	 */
	private void setupGui(String dbFilename) throws Exception {
	// Function: setup gui
	// Parameters: keyring-database
	// Returns: -

		// MenuBar
		JMenuBar myMenuBar = setMenuBar();
		frame.setJMenuBar(myMenuBar);

		// Layout
		JSplitPane mySplitPane = setLayout(this);
		frame.setContentPane(mySplitPane);

		// MenuBar Listener
		openMenuItem.addActionListener(new OpenFileListener(this));
		openURLMenuItem.addActionListener(new OpenURLListener(this));
		saveAsMenuItem.addActionListener(new SaveAsListener(this));
		saveToURLMenuItem.addActionListener(new SaveToURLListener(this));
		closeMenuItem.addActionListener(new CloseListener(this));
		quitMenuItem.addActionListener(new QuitListener(this));
		categoriesMenuItem.addActionListener(new editCategoriesListener(this));
		csvMenuItem.addActionListener(new csvListener(this));
		aboutMenuItem.addActionListener(new AboutListener(this));
		importMenuItem.addActionListener(new ImportListener(this));
		newMenuItem.addActionListener(new newListener(this));

		// itemPane Listener
		currentCategory.addActionListener(new currentCategorySelectionListener(this));
		currentTitle.getDocument().addDocumentListener(new documentListener(this));
		currentUser.getDocument().addDocumentListener(new documentListener(this));
		currentPassword.getDocument().addDocumentListener(new documentListener(this));
		currentUrl.getDocument().addDocumentListener(new documentListener(this));
		currentNotes.getDocument().addDocumentListener(new documentListener(this));

		// itemListPane Listener
		categoryList.addActionListener(new CategorySelectionListener(this));
		dynTree.getTree().addTreeSelectionListener(new treeSelectionListener(this));

		// buttonPane Listener
		newItem.addActionListener(new newItemListener(this));
		saveItem.addActionListener(new saveItemListener(this));
		delItem.addActionListener(new delItemListener(this));
		btnLock.addActionListener(new PasswordLockListener(this));
		currentPasswordShow.addActionListener(new PasswordShowListener(this));

		// Frame
		frame.pack();
		frame.setVisible(true);

		// Passwort Timeout
		timeoutThread = new PasswordTimeoutWorker(this);
		new Thread(timeoutThread).start();

		// load Database
		loadDatabase(dbFilename);
	}

	// loadDatabase ---------------------------------------------------
	/**
	 * Loads a Keyring database and setup gui (buttons, menubar) properly.
	 *
	 * @param dbFilename Keyring database or null
	 */
	private void loadDatabase(String filename) throws Exception {
		ring = null;
		if (null != filename) {
			ring = new Ring();
			try {
				ring.load(filename);

				dbFilename = filename;
				/* We only set previousDirectory if dbFilename has a directory,
				 * which is not the case for URLs. */
				File tmpPreviousDirectory = new File(dbFilename).getParentFile();
				if (null != tmpPreviousDirectory) {
					previousDirectory = tmpPreviousDirectory;
				}
			}
			catch(Exception ex) {
				msgError(ex, "Open keyring database", false);

				try {
					// FIXME This is wrong, prompt for a different file?
					loadDatabase(null);
					return;
				}
				catch(Exception ignore) {};
			}

			// Password dialog
			if(checkPassword() == false) {
				loadDatabase(null);
				return;
			}
		}
		initEditorState(null != ring);
	}

	/**
	 * Set state of buttons, menus, etc. according to presence of Ring & dbFilename.
	 */
	private void initEditorState(boolean dbLoaded) {
		String title;
		if (dbLoaded) {
			title = FRAMETITLE + ": " + null == dbFilename ? "UNSAVED" : dbFilename;
			setupCategories(ring.getCategories());
			dynTree.populate();
		} else {
			title = FRAMETITLE;
			setupCategories(null);
			dynTree.clear();
		}
		frame.setTitle(title);
		// Menu bar items
		openMenuItem.setEnabled(! dbLoaded);
		openURLMenuItem.setEnabled(! dbLoaded);
		saveAsMenuItem.setEnabled(dbLoaded);
		saveToURLMenuItem.setEnabled(dbLoaded);
		closeMenuItem.setEnabled(dbLoaded);
		csvMenuItem.setEnabled(dbLoaded);
		categoriesMenuItem.setEnabled(dbLoaded);
		importMenuItem.setEnabled(! dbLoaded);

		enableButtonsAndFields(dbLoaded);
		setBtnLock(false, dbLoaded);
	}
	
	/**
	 * Enable buttons according to loaded database.
	 *
	 * @param enabled True if database is loaded
	 */
	private void enableButtonsAndFields(boolean enabled) {
		delItem.setEnabled(false);
		saveItem.setEnabled(false);
		newItem.setEnabled(enabled);
		currentCategory.setEditable(false);
		currentTitle.setEditable(enabled);
		currentUser.setEditable(enabled);
		currentPassword.setEditable(enabled);
		currentUrl.setEditable(enabled);
		currentNotes.setEditable(enabled);
	}

	/**
	 * Enable button lock according to loaded database and status of password timeout.
	 *
	 * @param locked True if application is locked
	 * @param enabled True if button "Lock" should be enabled
	 */
	private void setBtnLock(boolean locked, boolean enabled) {
		btnLock.setText(locked ? "Unlock" : "Lock");
		btnLock.setEnabled(enabled);
		this.locked = locked;

		saveItem.setBackground(null);
	}

	// categories -----------------------------------------------------
	/**
	 * Setup category filter combobox.
	 *
	 * @param myCategories Categories loaded with Keyring database
	 */
	private void setupCategories(Vector<String> myCategories) {
		// categoryList
		Vector<String> displayCategories;
		if(myCategories != null)
			displayCategories = new Vector<String>(myCategories);
		else
			displayCategories = new Vector<String>();

		displayCategories.add(0, "All");
		categoryList.setModel(new DefaultComboBoxModel(displayCategories));

		// currentCategory
		Vector<String> currentCategories;
		if(myCategories != null)
			currentCategories = (Vector<String>) myCategories.clone();
		else
			currentCategories = new Vector<String>();

		currentCategory.setModel(new DefaultComboBoxModel(currentCategories));
	}

	// password -------------------------------------------------------
	/**
	 * Show password dialog and set Keyring database password.
	 *
	 * @return False if dialog cancelled (Boolean)
	 */
	private boolean checkPassword() {
		char[] password = getPasswordFromDialog();
		if (null == password) {
			return false;
		}
		try {
			if (ring.validatePassword(password)) {
				timeoutThread.restartTimeout();
				return true;
			} else {
				msgInformation("Invalid Password");
				timeoutThread.setTimeout(); // timed out
				return false;
			}
		}
		catch(Exception e) {
			msgError(e, "Error processing password", false);
			timeoutThread.setTimeout(); // timed out
			return false;
		}
	}

	/**
	 * Show password dialog.
	 *
	 * @return Password (null if dialog canceled).
	 */
	private char[] getPasswordFromDialog() {
		PasswordDialog pwdDlg = new PasswordDialog(frame);
		pwdDlg.pack();
		pwdDlg.setVisible(true);

		return pwdDlg.getPassword();
	}

 	// showItem ------------------------------------------------------
	/**
	 * Show item and check password timeout.
	 */
	private void showItem() {
		DefaultMutableTreeNode node = dynTree.getLastNode();

		if(locked == true) {
			return;
		}

		try {
			Date ende = timeoutThread.getEndDate();
			if(ende == null) { // timed out
				clearItem();

				setBtnLock(true, true);
				enableButtonsAndFields(false);
				saveItem.setBackground(null);

				return;
			}
			else {
				timeoutThread.restartTimeout();
			}

			// no item
			if(node == null || !(node.isLeaf()) || node.isRoot()) {
				clearItem();

				saveItem.setEnabled(false);
				saveItem.setBackground(null);
				delItem.setEnabled(false);

				return;
			}

			Object nodeInfo = node.getUserObject();

			Item item = (Item) nodeInfo;

			// set text fields according to item
			// if categoryname was deleted, show first category "no category"
			if (currentCategory.getItemCount() > item.getCategoryId()) {
				currentCategory.setSelectedIndex(item.getCategoryId());
			}
			else {
				currentCategory.setSelectedIndex(0); // no category
			}

			currentTitle.setText(item.getTitle());
			currentUser.setText(item.getUsername());
			currentNotes.setText(item.getNotes());
			currentPassword.setText(item.getPass());
			currentUrl.setText(item.getUrl());
			// FIXME This is sleazy and ugly
			dateLabel.setText("Created: " + ring.formatDate(item.getCreated(), false) +
					" | Changed: " + ring.formatDate(item.getChanged(), false) +
					" | Viewed: " + ring.formatDate(item.getViewed(), false));

			// initialize buttons
			textFieldChanged = false;
			saveItem.setEnabled(false);
			saveItem.setBackground(null);
			delItem.setEnabled(true);
		}
		catch(Exception e) {
			msgError(e, "showItem", true);

			try {
				// FIXME I don't think this makes sense
				loadDatabase(null);
			}
			catch(Exception ignore) {};
		}
	}

	/**
	 * Clear item text fields.
	 */
	private void clearItem() {
		currentCategory.setSelectedIndex(0);
		currentTitle.setText("");
		currentUser.setText("");
		currentPassword.setText("");
		currentUrl.setText("");
		dateLabel.setText("");
		currentNotes.setText("");
	}

	// ----------------------------------------------------------------
	// listener menubar -----------------------------------------------
	// ----------------------------------------------------------------

	/**
	 * MenuItem Open: show a File dialog and load a Keyring JSON database.
	 */
	public class OpenFileListener implements ActionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		protected OpenFileListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method opens a file dialog and loads the choosen database.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			JFileChooser chooser = new JFileChooser();

			chooser.setDialogTitle("Open Keyring database");
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setMultiSelectionEnabled(false);
			chooser.setCurrentDirectory(previousDirectory);

			int returnVal = chooser.showOpenDialog(editor.frame);

			if(returnVal == JFileChooser.APPROVE_OPTION) {
				try {
					File selectedFile = chooser.getSelectedFile();
					previousDirectory = selectedFile.getParentFile();
					dbFilename = selectedFile.getCanonicalPath();
					editor.loadDatabase(dbFilename);
				}
				catch(Exception ex) {
					msgError(ex, "Open Keyring database", false);

					try {
						editor.loadDatabase(null);
					}
					catch(Exception ignore) {};
				}
			}
		}
	}

	/**
	 * MenuItem Open URL: show a simple dialog and load a Keyring JSON database
	 * from a URL.
	 */
	public class OpenURLListener implements ActionListener {
		protected Editor editor;
		
		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		protected OpenURLListener(Editor editor) {
			this.editor = editor;
		}
		
		/**
		 * This method opens a simple dialog and loads the choosen database.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			String urlval = null != dbFilename && dbFilename.startsWith("http") ?
					dbFilename : "";
			String url = (String) JOptionPane.showInputDialog(
                    frame,
                    "URL (must start with \"http\"):",
                    "Load from URL",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    urlval);
			
			if (null != url) {
				try {
					editor.loadDatabase(url);
				}
				catch(Exception ex) {
					msgError(ex, "Open Keyring database", false);
					
					try {
						editor.loadDatabase(null);
					}
					catch(Exception ignore) {};
				}
			}
		}
	}
	
	/**
	 * MenuItem Close: close Keyring database.
	 */
	public class CloseListener implements ActionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		protected CloseListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method closes the loaded database.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			try {
				editor.loadDatabase(null);
			}
			catch(Exception ignore) {};
		}
	}

	/**
	 * MenuItem Quit: exit Application.
	 */
	public class QuitListener implements ActionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		protected QuitListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method ends the application.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	}

	/**
	 * MenuItem Edit categories: shows the categories dialog for editing category-names.
	 */
	public class editCategoriesListener implements ActionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		protected editCategoriesListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method opens the categories dialog and updates the category combo boxes.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			if (true) {
				// FIXME
				msgInformation("Category editing not yet supported");
				return;
			}
			if (locked == true) {
				msgInformation("Unlock application first.");
				return;
			}

			// show category dialog
			CategoriesDialog catDialog = new CategoriesDialog(frame, ring.getCategories());

			catDialog.pack();
			catDialog.setVisible(true);

			// update category combo boxes
			//Vector<String> newCategories = catDialog.getNewCategories(); // Java 1.5
			Vector newCategories = catDialog.getNewCategories();
			if(newCategories != null) { // changed categories
				setupCategories(newCategories);
				//ring.setCategories(newCategories);

				// save database
				try {
					editor.ring.save(dbFilename);
				}
				catch(Exception ex) {
					msgError(ex, "Could not save entries to " + dbFilename, false);
				}
			}
		}
	}

	/**
	 * MenuItem Save database to csv: save the loaded Keyring database as a CSV file.
	 */
	public class csvListener implements ActionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		protected csvListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method opens a file dialog and saves the database entries to a csv file.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			if(locked == true) {
				msgInformation("Unlock application first.");
				return;
			}

			// show File dialog
			JFileChooser chooser = new JFileChooser();

			chooser.setDialogTitle("Save Keyring database to CSV-File");
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setMultiSelectionEnabled(false);
			chooser.setCurrentDirectory(previousDirectory);

			int returnVal = chooser.showSaveDialog(editor.frame);

			File selectedFile = chooser.getSelectedFile();
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				try {
					previousDirectory = selectedFile.getParentFile();
					String csvFilename = selectedFile.getCanonicalPath();

					// check if file exists
					boolean ok = fileDoesntExistOrOverwriteConfirmed(selectedFile);

					if(ok == true) {
						// save entries to csv file
						editor.ring.exportToCSV(csvFilename);

						msgInformation("Entries saved to: " + csvFilename);
					}
				}
				catch(Exception ex) {
					msgError(ex, "Could not save entries to " +
							selectedFile.getAbsolutePath(), false);
				}
			}
		}
	}

	/**
	 * MenuItem Save As: save the loaded Keyring database to specified file.
	 */
	public class SaveAsListener implements ActionListener {
		protected Editor editor;
		
		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		protected SaveAsListener(Editor editor) {
			this.editor = editor;
		}
		
		/**
		 * This method opens a file dialog and saves the database entries to a csv file.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			if(locked == true) {
				msgInformation("Unlock application first.");
				return;
			}
			
			// show File dialog
			JFileChooser chooser = new JFileChooser();
			
			chooser.setDialogTitle("Save Keyring database");
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setMultiSelectionEnabled(false);
			chooser.setCurrentDirectory(previousDirectory);
			
			int returnVal = chooser.showSaveDialog(editor.frame);
			
			File selectedFile = chooser.getSelectedFile();
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				try {
					previousDirectory = selectedFile.getParentFile();
					String filename = selectedFile.getCanonicalPath();
					
					if (fileDoesntExistOrOverwriteConfirmed(selectedFile)) {
						// save entries to csv file
						editor.ring.save(filename);
						dbFilename = filename;
						// FIXME set window title & dyntree root to reflect new filename
						
						msgInformation("Keyring saved to: " + filename);
					}
				}
				catch(Exception ex) {
					msgError(ex, "Could not save entries to " +
							selectedFile.getAbsolutePath(), false);
				}
			}
		}
	}
	
	/**
	 * MenuItem Save As: save the loaded Keyring database to specified file.
	 */
	public class SaveToURLListener implements ActionListener {
		protected Editor editor;
		
		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		protected SaveToURLListener(Editor editor) {
			this.editor = editor;
		}
		
		/**
		 * This method opens a file dialog and saves the database entries to a csv file.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			if(locked == true) {
				msgInformation("Unlock application first.");
				return;
			}
			
			String urlval = null != dbFilename && dbFilename.startsWith("http") ?
					dbFilename : "";
			String url = (String) JOptionPane.showInputDialog(
                    frame,
                    "URL (must start with \"http\"):",
                    "Load from URL",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    urlval);
			
			if (null != url) {
				try {
					editor.ring.save(url);
					dbFilename = url;
					// FIXME set window title & dyntree root to reflect new URL
					msgInformation("Keyring saved to: " + url);
				}
				catch(Exception ex) {
					msgError(ex, "Could not save entries to " +
							url, false);
				}
			}
		}
	}
	
	/**
	 * MenuItem Import: show import dialog and load new database.
	 */
	public class ImportListener implements ActionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		protected ImportListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method opens the import dialog and closes the loaded database.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			// show import dialog
			ImportDialog importDialog = new ImportDialog(editor.frame, editor);

			importDialog.pack();
			importDialog.setVisible(true);

			if(importDialog.getCancelled() == false) {
				msgInformation("Database imported.");
				initEditorState(true);
			}
		}
	}

	/**
	 * MenuItem New empty database: generates a new empty database
	 */
	public class newListener implements ActionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		protected newListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method opens a file dialog and generates a new minimal database.
		 *
		 * FIXME this is just totally wrong.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			if(true) {
				msgInformation("Creating a new db not currently supported.");
				return;
			}
			if(locked == true) {
				msgInformation("Unlock application first.");
				return;
			}
			try {
				ring = new Ring();
				dbFilename = "";
				msgInformation("Empty database generated.");
			}
			catch(Exception ex) {
				msgError(ex, "Could not generate new database.", false);
			}
		}
	}

	/**
	 * MenuItem About: show Copyright information.
	 */
	public class AboutListener implements ActionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		protected AboutListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method shows a copyright information dialog.
		 *
		 * FIXME update this.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(editor.frame,
				Gui.FRAMETITLE + " " + "v" + Gui.VERSION +
				"\n\nCopyright 2010 Dirk Bergstrom <keyring@otisbean.com>\n" +
				"http://otisbean.com/keyring/\n\n" +
				"Keyring Desktop is based on:\n" +
				"KeyringEditor v1.1\n" +
				"Copyright 2006 Markus Griessnig <markus.griessnig@gmx.at>\n\n" +
				"This program is free software: you can redistribute it and/or modify\n" +
				"it under the terms of the GNU General Public License as published by\n" +
				"the Free Software Foundation, either version 3 of the License, or\n" +
				"(at your option) any later version.\n\n" +
				"This program is distributed in the hope that it will be useful,\n" +
				"but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
				"MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
				"GNU General Public License for more details.\n\n" +
				"You should have received a copy of the GNU General Public License\n" +
				"along with this program.  If not, see <http://www.gnu.org/licenses/>.",
				"About",
				JOptionPane.INFORMATION_MESSAGE);
		}
	}

	// ----------------------------------------------------------------
	// listener buttons -----------------------------------------------
	// ----------------------------------------------------------------

	// new
	/**
	 * Button New: show edit dialog and save new item to database.
	 */
	public class newItemListener implements ActionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		protected newItemListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method opens the new item dialog and and saves the new item to database.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			// show edit dialog
			NewItemDialog editDlg = new NewItemDialog(editor.frame, editor.ring.getCategories());
			editDlg.pack();
			editDlg.setVisible(true);

			Object[] buffer = editDlg.getNewItem();

			try {
				// new item
				if(buffer[0] != null) {
					/* Edit dialog returns (All String except category is Integer)
					 * returnParameter[0] = category
					 * returnParameter[1] = title
					 * returnParameter[2] = user
					 * returnParameter[3] = password
					 * returnParameter[4] = url
					 * returnParameter[5] = notes 
			         * 
			         * Item expects
			         * String username,
			         * String pass,
			         * String url,
			         * String notes,
			         * String title,
			         * int categoryId
					 */
					Item myItem = new Item(
							editor.ring,
							(String) buffer[2],
							(String) buffer[3],
							(String) buffer[4],
							(String) buffer[5],
							(String) buffer[1],
							((Integer) buffer[0]).intValue()
					);

					// register new item to vector entries
					editor.ring.addItem(myItem);

					// update tree view
					editor.dynTree.populate();

					// save database
					editor.ring.save(dbFilename);

					// show new item
					editor.dynTree.show(myItem);
					
					msgInformation("Entries saved to: " + editor.dbFilename);
				}
			}
			catch(Exception ex) {
				msgError(ex, "newItemListener", true);
			}
		}
	}

	// save
	/**
	 * Button Save: save changed item to database.
	 */
	public class saveItemListener implements ActionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		public saveItemListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method saves a changed item to database.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			DefaultMutableTreeNode node = editor.dynTree.getLastNode();

			try {
				// last selected tree node
				if(node != null) {
					// get updated item
					Item myItem = editor.dynTree.getItem(node);

					// save changes
					myItem.setTitle(editor.currentTitle.getText());
					myItem.setUsername(editor.currentUser.getText());
					myItem.setPass(String.valueOf(editor.currentPassword.getPassword()));
					myItem.setUrl(editor.currentUrl.getText());
					myItem.setNotes(editor.currentNotes.getText());
					myItem.setCategoryId(editor.currentCategory.getSelectedIndex());

					// update tree view
					editor.dynTree.populate();

					// save database
					editor.ring.save(dbFilename);
					
					// Redisplay the item
					editor.dynTree.show(myItem);
					editor.showItem();

					msgInformation("Entries saved to: " + editor.dbFilename);
				}
			}
			catch(Exception ex) {
				msgError(ex, "saveItemListener", true);
			}
		}
	}

	// del
	/**
	 * Button Delete: delete current item.
	 */
	public class delItemListener implements ActionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		public delItemListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method removes a item from database.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			DefaultMutableTreeNode node = editor.dynTree.getLastNode();

			if(node != null) {
				Item myItem = editor.dynTree.getItem(node);

				// delete item
				editor.ring.removeItem(myItem);

				// update tree view
				editor.dynTree.populate();

				// save changes
				try {
					editor.ring.save(dbFilename);

					msgInformation("Item " + myItem.getTitle() + " deleted. Database " + editor.dbFilename + " updated.");
				}
				catch(Exception ex) {
					msgError(ex, "delItemListener", false);
				}
			}
		}
	}

	// ----------------------------------------------------------------
	// listener -------------------------------------------------------
	// ----------------------------------------------------------------

	// textfields
	/**
	 * DocumentListener: check if item is updated and set button "Save" according.
	 */
	public class documentListener implements DocumentListener {
		protected Editor editor;

		protected documentListener(Editor editor) {
			this.editor = editor;
		}

		public void insertUpdate(DocumentEvent e) {
			updateLog(e, "insert");
		}

		public void removeUpdate(DocumentEvent e) {
			updateLog(e, "remove");
		}

		public void changedUpdate(DocumentEvent e) {
			updateLog(e, "changed");
		}

		public void updateLog(DocumentEvent e, String action) {
			if(editor.textFieldChanged == false && editor.locked == false) {
				editor.textFieldChanged = true;

				editor.saveItem.setEnabled(true);
				editor.saveItem.setBackground(Color.YELLOW);
			}
		}
	}

	// tree
	/**
	 * TreeSelectionListener: show selected item.
	 */
	public class treeSelectionListener implements TreeSelectionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		protected treeSelectionListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method shows the selected item.
		 *
		 * @param e the ActionEvent to process
		 */
		public void valueChanged(TreeSelectionEvent e) {
			editor.showItem();
		}
	}

	/**
	 * CategorySelectionListener: filter tree view according to selected category.
	 */
	public class CategorySelectionListener implements ActionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		public CategorySelectionListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method filters the tree view according to selected category.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {

			editor.dynTree.setCategoryFilter(editor.categoryList.getSelectedIndex());

			editor.showItem();
		}
	}

	/**
	 * currentCategorySelectionListener: check if item category is changed an set button "Save" according.
	 */
	public class currentCategorySelectionListener implements ActionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		public currentCategorySelectionListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method sets button "Save" according to state of variable locked and textFieldChanged.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			//System.out.println(editor.textFieldChanged);

			if(editor.textFieldChanged == false && editor.locked == false) {
				editor.textFieldChanged = true;

				editor.saveItem.setEnabled(true);
				editor.saveItem.setBackground(Color.YELLOW);
			}
		}
	}

	/**
	 * PasswordShowListener: show item password in clear text according to check box "Hide passwords?"
	 */
	public class PasswordShowListener implements ActionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		protected PasswordShowListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method shows password in clear text according to variable showPassword.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			if(showPassword == false) {
				showPassword = true;
				editor.currentPassword.setEchoChar('\0'); // show password in plaintext
			}
			else {
				showPassword = false;
				editor.currentPassword.setEchoChar('*');
			}
		}
	}

	/**
	 * Button Lock / Unlock: set buttons according to variable locked.
	 */
	public class PasswordLockListener implements ActionListener {
		protected Editor editor;

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		protected PasswordLockListener(Editor editor) {
			this.editor = editor;
		}

		/**
		 * This method sets the button "Lock / Unlock" according to variable locked.
		 *
		 * @param e the ActionEvent to process
		 */
		public void actionPerformed(ActionEvent e) {
			if(editor.locked == false) {
				editor.setBtnLock(true, true);
				editor.enableButtonsAndFields(false);
				editor.clearItem();
			}
			else {
				if(editor.checkPassword() == true) {
					editor.setBtnLock(false, true);
					editor.enableButtonsAndFields(true);
					editor.showItem();
				}
			}
		}
	}
}
