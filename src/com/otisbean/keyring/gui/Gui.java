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

// Gui.java

// 11.11.2004

// 15.11.2004: newEntry, saveEntry, delEntry
// 17.11.2004: new parameter filename (keyring database); edit notes, edit categories
// 18.11.2004: changed currentCategory to ComboBox; changed Layout; JPasswordField
// 22.11.2004: added DynamicTree; added DocumentListener
// 23.11.2004: currentCategorySelectionListener; PasswordTimeoutWorker
// 24.11.2004: CategoriesDialog;
// 25.11.2004: split class into Gui.java and Editor.java
// 05.12.2004: MenuItem Tools - Convert database added

package com.otisbean.keyring.gui;

import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.awt.*;

/**
 * This class setup the gui and the password timeout functions.
 */
public class Gui {
	// ----------------------------------------------------------------
	// variables
	// ----------------------------------------------------------------
	protected static final String VERSION = "2.0a";
	protected static final String FRAMETITLE = "Keyring Desktop";
    
	/**
     * Time in milliseconds until password timeout.
     * FIXME should take this from ring.prefs instead.
     */
    protected static long PASSWORD_TIMEOUT = 60000; // default: 1 minute = 60 s * 1000 ms

	// MenuBar
	protected JMenuBar menuBar;
	// File
	protected JMenuItem openMenuItem;
	protected JMenuItem openURLMenuItem;
	protected JMenuItem saveAsMenuItem;
	protected JMenuItem saveToURLMenuItem;
	protected JMenuItem closeMenuItem;
	protected JMenuItem quitMenuItem;
	// Tools
	protected JMenuItem categoriesMenuItem;
	protected JMenuItem csvMenuItem;
	protected JMenuItem importMenuItem;
	protected JMenuItem newMenuItem;

	// Help
	protected JMenuItem aboutMenuItem;

	// itemListPane
	protected JComboBox categoryList;
	protected DynamicTree dynTree;

	// itemPane
	protected JComboBox currentCategory;
	protected JTextField currentTitle;
	protected JTextField currentUser;
	protected JPasswordField currentPassword;
	protected JTextField currentUrl;
	protected JLabel dateLabel;
	protected JTextArea currentNotes;
	protected JButton saveItem;

	// buttonPane
	protected JButton newItem;
	protected JButton delItem;
    protected JButton btnLock;
	protected JCheckBox currentPasswordShow;

	// Password Timeout -----------------------------------------------
	/**
	 * This class handles the password timeout.
	 * It is started by class Editor as a thread.
	 */
	protected class PasswordTimeoutWorker implements Runnable {
		/**
		 * Reference to class Editor
		 */
		private Editor editor;

		/**
		 * Time when password timeout started plus user defined password timeout
		 */
		private Date endDate = null;

		private Object sleepGate = new Object();

		/**
		 * Default constructor.
		 *
		 * @param editor Reference to class Editor
		 */
		public PasswordTimeoutWorker(Editor editor) {
			this.editor = editor;
		}

		/**
		 * Sets the variable endDate to new timeout.
		 */
		public synchronized void restartTimeout() {
			endDate = new Date(System.currentTimeMillis() + PASSWORD_TIMEOUT);

			synchronized (sleepGate) {
				sleepGate.notifyAll();
			}
		}

		/**
		 * Sets the variable endDate to null and forces a timeout.
		 */
		public synchronized void setTimeout() {
			endDate = null;
		}

		/**
		 * Returns variable endDate.
		 */
		public synchronized Date getEndDate() {
			return endDate;
		}

		/**
		 * Timeout thread.
		 */
		public void run() {
			while(true) {
				if(endDate != null) {
					long difference = endDate.getTime() - System.currentTimeMillis();

					// timed out
					if(difference <= 0) {
						endDate = null;
					}

					try {
						Thread.sleep(1000); // sleep 1 s
					}
					catch (InterruptedException ignore) {};
				}
				else { // timed out
					try {
						synchronized (sleepGate) {
							sleepGate.wait();
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
    }

	// ----------------------------------------------------------------
	// protected ------------------------------------------------------
	// ----------------------------------------------------------------

	// setMenuBar -----------------------------------------------------
	/**
	 * Setup menubar.
	 */
	protected JMenuBar setMenuBar() {
	// Function: creates menu bar
	// Parameters: -
	// Returns: JMenubar
		menuBar = new JMenuBar();
		//frame.setJMenuBar(menuBar);

		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		menuBar.add(fileMenu);

		openMenuItem = new JMenuItem("Open", KeyEvent.VK_O);
		fileMenu.add(openMenuItem);
		openURLMenuItem = new JMenuItem("Open URL", KeyEvent.VK_U);
		fileMenu.add(openURLMenuItem);
		
		saveAsMenuItem = new JMenuItem("Save As", KeyEvent.VK_A);
		fileMenu.add(saveAsMenuItem);
		saveAsMenuItem.setEnabled(false);
		saveToURLMenuItem = new JMenuItem("Save To URL", KeyEvent.VK_A);
		fileMenu.add(saveToURLMenuItem);
		saveToURLMenuItem.setEnabled(false);
		
		closeMenuItem = new JMenuItem("Close", KeyEvent.VK_C);
		fileMenu.add(closeMenuItem);
		closeMenuItem.setEnabled(false);

		fileMenu.addSeparator();

		quitMenuItem = new JMenuItem("Quit", KeyEvent.VK_Q);
		fileMenu.add(quitMenuItem);

		JMenu catMenu = new JMenu("Tools");
		catMenu.setMnemonic(KeyEvent.VK_T);
		menuBar.add(catMenu);

		categoriesMenuItem = new JMenuItem("Edit categories", KeyEvent.VK_E);
		catMenu.add(categoriesMenuItem);

		catMenu.addSeparator();

		csvMenuItem = new JMenuItem("Export to CSV file", KeyEvent.VK_S);
		catMenu.add(csvMenuItem);

		importMenuItem = new JMenuItem("Import database", KeyEvent.VK_C);
		catMenu.add(importMenuItem);

		newMenuItem = new JMenuItem("New database", KeyEvent.VK_N);
		catMenu.add(newMenuItem);

		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic(KeyEvent.VK_H);
		menuBar.add(helpMenu);

		aboutMenuItem = new JMenuItem("About", KeyEvent.VK_A);
		helpMenu.add(aboutMenuItem);

		return menuBar;
	}

	// setLayout ------------------------------------------------------
	/**
	 * Setup gui layout.
	 */
	protected JSplitPane setLayout(Editor editor) {
	// Function: creates gui elements (GridBagLayout)
	// Parameters: -
	// Returns: JSplitPane
		int gridy = 0;

		// Gui Elements
		categoryList = new JComboBox();
		dynTree = new DynamicTree(editor);

		currentCategory = new JComboBox();
		currentTitle = new JTextField();
		currentUser = new JTextField();
		currentPassword = new JPasswordField();
		currentUrl = new JTextField();
		currentNotes = new JTextArea();

		currentPasswordShow = new JCheckBox("Hide Passwords?", true);
		btnLock = new JButton("Lock");
		newItem = new JButton("New Item");
		saveItem = new JButton("Save");
		delItem = new JButton("Delete");

		// Panes
		JPanel itemListPane = new JPanel();
		itemListPane.setBorder(BorderFactory.createLineBorder(Color.black));

		JPanel itemPane = new JPanel();
		itemPane.setBorder(BorderFactory.createLineBorder(Color.black));

		JPanel buttonPane = new JPanel();
		buttonPane.setBorder(BorderFactory.createLineBorder(Color.black));

		// Item-List-Pane --------------------------------------------
		GridBagLayout gridbag1 = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		itemListPane.setLayout(gridbag1);

		c.ipadx = 2;
		c.ipady = 2;
		c.insets = new Insets(2, 2, 2, 2);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0;
		c.weighty = 0;

		gridbag1.setConstraints(categoryList, c);
		itemListPane.add(categoryList);

		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;

		gridbag1.setConstraints(dynTree, c);
		itemListPane.add(dynTree);

		// Item-Pane -------------------------------------------------
		GridBagLayout gridbag2 = new GridBagLayout();
		itemPane.setLayout(gridbag2);

		c.ipadx = 2;
		c.ipady = 2;
		c.insets = new Insets(2, 2, 2, 2);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;

		// category
		JLabel categoryLabel = new JLabel("Category: ");
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 0.0;
		gridbag2.setConstraints(categoryLabel, c);
		itemPane.add(categoryLabel);

		c.gridx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 1.0;
		gridbag2.setConstraints(currentCategory, c);
		itemPane.add(currentCategory);

		// title
		JLabel titleLabel = new JLabel("Title: ");
		c.gridx = 0;
		c.gridy = ++gridy;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 0.0;
		gridbag2.setConstraints(titleLabel, c);
		itemPane.add(titleLabel);

		c.gridx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 1.0;
		gridbag2.setConstraints(currentTitle, c);
		itemPane.add(currentTitle);

		// account
		JLabel accountNameLabel = new JLabel("User: ");
		c.gridx = 0;
		c.gridy = ++gridy;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 0.0;
		gridbag2.setConstraints(accountNameLabel, c);
		itemPane.add(accountNameLabel);

		c.gridx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 1.0;
		gridbag2.setConstraints(currentUser, c);
		itemPane.add(currentUser);

		// password
		JLabel passwordLabel = new JLabel("Password: ");
		c.gridx = 0;
		c.gridy = ++gridy;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 0.0;
		gridbag2.setConstraints(passwordLabel, c);
		itemPane.add(passwordLabel);

		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 1.0;
		gridbag2.setConstraints(currentPassword, c);
		itemPane.add(currentPassword);

		// URL
		JLabel urlLabel = new JLabel("URL: ");
		c.gridx = 0;
		c.gridy = ++gridy;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 0.0;
		gridbag2.setConstraints(urlLabel, c);
		itemPane.add(urlLabel);

		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 1.0;
		gridbag2.setConstraints(currentUrl, c);
		itemPane.add(currentUrl);

		// date
		// FIXME find a cleaner way to handle this
		dateLabel = new JLabel("Created: Changed: Viewed:");
		c.gridx = 0;
		c.gridy = ++gridy;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0.0;
		gridbag2.setConstraints(dateLabel, c);
		itemPane.add(dateLabel);
		
		// notes
		c.gridx = 0;
		c.gridy = ++gridy;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 1.0;
		c.weighty = 1.0;

		JScrollPane currentNotesScroll = new JScrollPane(currentNotes);
		gridbag2.setConstraints(currentNotesScroll, c);
		itemPane.add(currentNotesScroll);

		// save
		c.gridx = 0;
		c.gridy = ++gridy;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 0.0;
		c.weighty = 0.0;
		gridbag2.setConstraints(saveItem, c);
		itemPane.add(saveItem);

		// buttonPane
		//FlowLayout flow = new FlowLayout(FlowLayout.LEADING);
		//BoxLayout flow = new BoxLayout(BoxLayout.X_AXIS);
		//buttonPane.setLayout(flow);
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));

		buttonPane.add(newItem);
		buttonPane.add(Box.createRigidArea(new Dimension(5,0)));
		buttonPane.add(delItem);
		buttonPane.add(Box.createRigidArea(new Dimension(5,0)));
		buttonPane.add(btnLock);
		buttonPane.add(Box.createRigidArea(new Dimension(5,0)));
		buttonPane.add(currentPasswordShow);

		// JSplitPane -------------------------------------------------
		JSplitPane top = new JSplitPane(JSplitPane.VERTICAL_SPLIT, itemPane, buttonPane);
		top.setResizeWeight(1);

		JSplitPane contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, itemListPane, top);
		contentPane.setResizeWeight(0.33);

		itemListPane.setMinimumSize(new Dimension(200, 440));
		itemPane.setMinimumSize(new Dimension(300, 400));
		buttonPane.setMinimumSize(new Dimension(300, 40));

		itemListPane.setPreferredSize(new Dimension(150, 340));
		itemPane.setPreferredSize(new Dimension(500, 350));
		buttonPane.setPreferredSize(new Dimension(450, 40));

		//frame.setContentPane(contentPane);
		return contentPane;
	}
}
