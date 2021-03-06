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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

/**
 * This dialog allows the user to enter a password.
 */
public class PasswordDialog extends JDialog implements ActionListener, PropertyChangeListener {
	private static final long serialVersionUID = 1L;

	// ----------------------------------------------------------------
	// variables
	// ----------------------------------------------------------------
	/**
	 * Boolean is true if button "Cancel" pressed, otherwise false
	 */
	private boolean cancelled = false;

	/**
	 * Password field
	 */
	private JPasswordField pwdField;

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
	 */
	public PasswordDialog(Frame aFrame) {
		super(aFrame, "Password", true);

		pwdField = new JPasswordField(40);

		String msgString1 = "Please enter password: ";

		Object[] array = {msgString1, pwdField};

		Object[] options = {btnString1, btnString2};

		// generate Dialog
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
				pwdField.requestFocusInWindow();
			}
		});

		//Register an event handler that puts the text into the option pane.
		pwdField.addActionListener(this);

		//Register an event handler that reacts to option pane state changes.
		optionPane.addPropertyChangeListener(this);

		// center dialog
		Point po = new Point(aFrame.getLocationOnScreen());
		Dimension paren_dim = aFrame.getSize();
		int paren_hei = paren_dim.height;
		int paren_wid = paren_dim.width;
		Dimension dial_dim = this.getSize();
		int dial_hei = dial_dim.height;
		int dial_wid = dial_dim.width;
		int dist_X_mid = ((paren_wid - dial_wid) / 2);
		int dist_Y_mid = ((paren_hei - dial_hei) / 2);
		po.translate(dist_X_mid, dist_Y_mid);
		this.setLocation(po);
	}

	// ----------------------------------------------------------------
	// public ---------------------------------------------------------
	// ----------------------------------------------------------------

	/**
	 * This method returns the typed password.
	 *
	 * @return character-array of the typed password
	 */
	public char[] getPassword() {
		if(cancelled) {
			return null;
		}

		return pwdField.getPassword();
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
	 * If button is OK variable cancelled is set to false, otherwise to true.
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
				cancelled = false;
			}
			else {
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
		setVisible(false);
	}
}
