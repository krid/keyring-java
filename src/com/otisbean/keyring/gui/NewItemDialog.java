/*
 * @author Dirk Bergstrom
 *
 * Keyring Desktop Client - Easy password management on your phone or desktop.
 * Copyright (C) 2009-2010, Dirk Bergstrom, keyring@otisbean.com
 * 
 * Adapted from KeyringEditor v1.1
 * Copyright 2006 Markus Griessnig
 * http://www.ict.tuwien.ac.at/keyring/
 * Markus graciously gave his assent to release the modified code under the GPLv3.
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

package com.otisbean.keyring.gui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * This dialog allows the user to enter a new item.
 */
public class NewItemDialog extends JDialog implements ActionListener, PropertyChangeListener {

	private static final long serialVersionUID = 1L;

	// ----------------------------------------------------------------
	// variables
	// ----------------------------------------------------------------
	/**
	 * Variable contains saved input fields
	 *
	 * returnParameter[0] = category
	 * returnParameter[1] = title
	 * returnParameter[2] = user
	 * returnParameter[3] = password
	 * returnParameter[4] = url
	 * returnParameter[5] = notes
	 */
	private Object[] returnParameter;

	private JComboBox categoryList;
	private JTextField textTitle;
	private JTextField textUser;
	private JTextField textPassword;
	private JTextField textUrl;
	private JTextArea textNotes;

	private JOptionPane optionPane;

	private String btnString1 = "OK";
	private String btnString2 = "Cancel";

	// ----------------------------------------------------------------
	// constructor
	// ----------------------------------------------------------------
	/**
	 * Default constructor generates Dialog.
	 *
	 * @param aFrame Reference to the Gui frame
	 * @param cat Vector of category-names
	 */
	public NewItemDialog(Frame aFrame, Vector<String> cat) {
		super(aFrame, "New Item", true);

		returnParameter = new Object[6];

		// list of category-names
		categoryList = new JComboBox(cat);
		categoryList.setModel(new DefaultComboBoxModel(cat));

		// text fields
		textTitle = new JTextField(40);
		textUser = new JTextField(40);
		textPassword = new JTextField(40);
		textUrl = new JTextField(40);
		textNotes = new JTextArea(5, 40);
		JScrollPane currentNotesScroll = new JScrollPane(textNotes);

		// labels
		String msgString1 = "Category: ";
		String msgString2 = "Title: ";
		String msgString3 = "User: ";
		String msgString4 = "Password: ";
		String msgString5 = "URL: ";
		String msgString6 = "Notes: ";

		Object[] array = {msgString1, categoryList,
			msgString2, textTitle,
			msgString3, textUser,
			msgString4, textPassword,
			msgString5, textUrl,
			msgString6, currentNotesScroll};

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
				textTitle.requestFocusInWindow();
			}
		});

		//Register an event handler that puts the text into the option pane.
		textTitle.addActionListener(this);

		//Register an event handler that reacts to option pane state changes.
		optionPane.addPropertyChangeListener(this);
	}

	// ----------------------------------------------------------------
	// public ---------------------------------------------------------
	// ----------------------------------------------------------------

	/**
	 * This method returns the variable returnParameter.
	 * If returnParameter[0] equals null the dialog was cancelled.
	 *
	 * @return object-array
	 */
	public Object[] getNewItem() {
		return returnParameter;
	}

	/**
	 * This method sets OptionPane to the value of button "OK".
	 *
	 * @param e the ActionEvent to process
	 */
	public void actionPerformed(ActionEvent e) {
		optionPane.setValue(btnString1);
	}

	/**
	 * This method processes the pressed button.
	 * If button is OK account and title are checked to be not empty and variable returnParameter is set,
	 * otherwise variable returnParameter is set to null.
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

			//Reset the JOptionPane's value.
			//If you don't do this, then if the user
			//presses the same button next time, no
			//property change event will be fired.
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

			if(btnString1.equals(value)) {
				// check for empty strings
				if(textTitle.getText().length() == 0) {
					JOptionPane.showMessageDialog(NewItemDialog.this, "Sorry, title must not be empty!\n",
                    	"Error", JOptionPane.ERROR_MESSAGE);
                    returnParameter[0] = null;
                    textTitle.requestFocusInWindow();
                    return;
                }

				// ok
				setReturnParameter();
			}
			else {
				returnParameter[0] = null;
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
		textTitle.setText(null);
		textUser.setText(null);
		textPassword.setText(null);
		textUrl.setText(null);
		textNotes.setText(null);

		setVisible(false);
	}

	/**
	 * This method gets the input fields and updates the variable returnParameter.
	 */
	private void setReturnParameter() {
		Integer cat = new Integer(categoryList.getSelectedIndex());

		returnParameter[0] = (Object) cat;
		returnParameter[1] = (Object) textTitle.getText();
		returnParameter[2] = (Object) textUser.getText();
		returnParameter[3] = (Object) textPassword.getText();
		returnParameter[4] = (Object) textUrl.getText();
		returnParameter[5] = (Object) textNotes.getText();
	}
}
