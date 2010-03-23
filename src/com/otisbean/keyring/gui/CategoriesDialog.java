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

// CategoriesDialog.java

// 23.11.2004

// 24.11.2004: size of categoryname is 15 characters
// 29.12.2004: renamed variable "enum"
// 24.05.2005: added Button "Delete"

package com.otisbean.keyring.gui;

import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*; // property change stuff

/**
 * This dialog allows the user to change the category-names.
 */
public class CategoriesDialog extends JDialog implements ActionListener, PropertyChangeListener {
	// ----------------------------------------------------------------
	// variables
	// ----------------------------------------------------------------
	/**
	 * Reference to the Gui frame
	 */
	private Frame frame;

	private JOptionPane optionPane;

	private String btnString1 = "OK";
	private String btnString2 = "Cancel";
	private String btnString3 = "Delete";

	private JComboBox allCategories;

	private JTextField editCategory;

	/**
	 * Index of the selected combobox item
	 */
	private int index = 0;

	/**
	 * Vector of category-names for the combobox
	 */
	//private Vector<String> myCategories; // Java 1.5
	private Vector myCategories;

	// ----------------------------------------------------------------
	// constructor
	// ----------------------------------------------------------------
	/**
	 * Default constructor generates Dialog.
	 *
	 * @param frame Reference to the Gui frame
	 * @param cat Vector of category-names
	 */
	public CategoriesDialog(Frame frame, Vector cat) {
		super(frame, "Edit categories", true);

		this.frame = frame;

		//myCategories = new Vector<String>(); // Java 1.5
		myCategories = new Vector();

		// generate Vector of category-names for JComboBox
		for(Enumeration e = cat.elements(); e.hasMoreElements(); ) {
			//Object temp = e.nextElement(); // Java 1.5
			//myCategories.add((String)temp); // Java 1.5
			Object temp = e.nextElement();
			myCategories.add(temp);
		}

		// resize to 16 categories
		for(int i=myCategories.size(); i<16; i++) {
			String empty = new String("- empty -");
			//myCategories.add(empty); // Java 1.5
			myCategories.add((Object)empty);
		}

		// generate JComboBox with category-names
		allCategories = new JComboBox(myCategories);
		allCategories.setModel(new DefaultComboBoxModel(myCategories));
		//allCategories.setEditable(true);

		editCategory = new JTextField(16);

		Object[] array = {allCategories, editCategory};

		Object[] options = {btnString1, btnString2, btnString3};

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
				editCategory.requestFocusInWindow();
			}
		});

		//Register an event handler that puts the text into the option pane.
		allCategories.addActionListener(this);

		//Register an event handler that reacts to option pane state changes.
		optionPane.addPropertyChangeListener(this);

		editCategory.setText((String)allCategories.getSelectedItem());
	}

	// ----------------------------------------------------------------
	// public ---------------------------------------------------------
	// ----------------------------------------------------------------

	/**
	 * This method returns the updated category-names.
	 *
	 * @return Vector of category-names
	 */
	//public Vector<String> getNewCategories() { // Java 1.5
	public Vector getNewCategories() {
		return myCategories;
	}

	/**
	 * This method gets the edited category-name and updates the Vector of category-names.
	 *
	 * @param e the ActionEvent to process
	 */
	public void actionPerformed(ActionEvent e) {
		// save category
		String temp = editCategory.getText();
		
		if(index == 0 && temp.equals("- empty -")) {
			JOptionPane.showMessageDialog(this.frame,
				"First Categoryname is not deleteable.",
				"Information",
				JOptionPane.INFORMATION_MESSAGE);
				
			editCategory.setText((String)allCategories.getSelectedItem());
            index = allCategories.getSelectedIndex();
		}
		else {
           //myCategories.setElementAt(temp, index); // Java 1.5
            myCategories.setElementAt((Object)temp, index);

            // view selected category
            editCategory.setText((String)allCategories.getSelectedItem());
            index = allCategories.getSelectedIndex();
		}
	}

	/**
	 * This method processes the pressed button.
	 * If button is OK all categoriy-names are cut to 15 characters
	 * and saved to Vector myCategories otherwise myCategories is set to null.
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
				// Button OK

				// save last edited category
				String lastCategory = editCategory.getText();
				//myCategories.setElementAt(lastCategory, index); // Java 1.5
				myCategories.setElementAt((Object)lastCategory, index);

				// check for empty strings
				//Vector<String> temp = new Vector<String>(); // Java 1.5
				Vector temp = new Vector();
				for(Enumeration elem = myCategories.elements(); elem.hasMoreElements(); ) {
					String category = (String)elem.nextElement();

					// shrink length to 15 characters (keyring)
					if(category.length() > 15) {
						JOptionPane.showMessageDialog(this.frame,
							"Categoryname '" + category + "' will be shrinked to 15 characters.",
							"Information",
							JOptionPane.INFORMATION_MESSAGE);

						category = category.substring(0, 15);
					}

					if(!(category.equals("- empty -"))) {
						temp.add(category);
					}
				}

				myCategories = temp;
				clearAndHide();
			}
			else {
				if(btnString2.equals(value)) {
					// Button Cancel
					myCategories = null;
					clearAndHide();
				}
				else {
                    // Button Delete
                    editCategory.setText("- empty -"); // set empty
					index = allCategories.getSelectedIndex();
					
                    // jump to first category
                    allCategories.setSelectedIndex(0);
				}
			}
		}
	}

	// ----------------------------------------------------------------
	// private --------------------------------------------------------
	// ----------------------------------------------------------------

	/**
	 * This method hides the dialog.
	 */
	private void clearAndHide() {
		editCategory.setText(null);
		setVisible(false);
	}
}
