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

import java.awt.GridLayout;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.otisbean.keyring.Item;
import com.otisbean.keyring.Ring;

/**
 * This class is used to view and manipulate the entries in the tree view.
 * 
 * FIXME Replace the DynamicTree with a JList, since we're not using the
 * tree aspect at all.
 * 
 * public class RingListModel extends AbstractListModel { ... }
 * 
 * RingListModel rlm = new RingListModel();
 * JList list = new JList(rlm);
 * list.addListSelectionListener(new ListSelectionListener() {
 *    public void valueChanged(ListSelectionEvent e) {
 *        if (!e.getValueIsAdjusting()) {
 *            Item item = (Item) list.getSelectedValue();
 *            showItem(item);
 *        }
 *    }
 * });
 */
public class DynamicTree extends JPanel {
	private static final long serialVersionUID = 1L;

	// ----------------------------------------------------------------
	// variables
	// ----------------------------------------------------------------

	/**
	 * Root node
	 */
	private DefaultMutableTreeNode rootNode;

	private DefaultTreeModel treeModel;

	/**
	 * A control that displays a set of hierarchical data as an outline
	 */
	 private JTree tree;

	/**
	 * Reference to the class Editor
	 */
	private Editor editor;

	/**
	 * Category-filter (0 = show all)
	 */
	private int filterCategory = 0;

	// ----------------------------------------------------------------
	// constructor
	// ----------------------------------------------------------------
	/**
	 * Default constructor generates tree view.
	 *
	 * @param editor Reference to the class Editor
	 */
	 public DynamicTree(Editor editor) {
		super(new GridLayout(1,0));

		this.editor = editor;

		rootNode = new DefaultMutableTreeNode("");
		treeModel = new DefaultTreeModel(rootNode);

		tree = new JTree(treeModel);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setRootVisible(true);

		JScrollPane scrollPane = new JScrollPane(tree);
		add(scrollPane);
	}

	// ----------------------------------------------------------------
	// public ---------------------------------------------------------
	// ----------------------------------------------------------------

	/**
	 * This method selects a tree node according to the item object.
	 *
	 * @param newitem Item object
	 */
	@SuppressWarnings("unchecked")
	public void show(Object newitem) {
		Object nodeInfo;
		
		DefaultMutableTreeNode node = rootNode;

		for(Enumeration enum1 = rootNode.depthFirstEnumeration(); enum1.hasMoreElements(); ) {
			node = (DefaultMutableTreeNode) enum1.nextElement();
			
			if(null != node) {
				if(node.isLeaf()) {
					nodeInfo = node.getUserObject();
					
					Item e1 = (Item) nodeInfo;
					Item e2 = (Item) newitem;
					
					if(e1.getTitle().equals(e2.getTitle())) {
						// New item found
						tree.setSelectionPath(new TreePath(node.getPath()));
					}
				}
			}
		}
	}

	/**
	 * This method returns variable tree.
	 *
	 * @return variable tree
	 */
	public JTree getTree() {
		return tree;
	}

	/**
	 * This method sets the category-filter and refreshes the tree view.
	 *
	 * @return True if category changed otherwise false
	 */
	public boolean setCategoryFilter(int filterCategory) {
		boolean changed = (filterCategory != this.filterCategory);
		this.filterCategory = filterCategory;

		populate();

		return changed;
	}

	/**
	 * This method sets the category-filter to zero (show all) and refreshes the tree view.
	 */
	public void clearFilter() {
		filterCategory = 0;
		populate();
	}

	/**
	 * This method returns the item data of the tree node.
	 *
	 * @param node tree-node
	 *
	 * @return Item data of tree-node
	 */
	public Item getItem(DefaultMutableTreeNode node) {
		Object nodeInfo = node.getUserObject();

		return (Item)nodeInfo;
	}

	/**
	 * This method returns the last selected tree node.
	 *
	 * @return DefaultMutableTreeNode or null if node is not a leaf
	 */
	public DefaultMutableTreeNode getLastNode() {
		DefaultMutableTreeNode temp = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
		if (null != temp && temp.isLeaf()) {
			return temp;
		} else {
			return null;
		}
	}

	// reload TreeModel
	/**
	 * This method reloads the tree model.
	 */
	public void reload() {
		treeModel.reload();
	}

	// remove all children
	/**
	 * This method removes all tree nodes and reloads the tree.
	 */
	public void clear() {
		rootNode.setUserObject("");
		rootNode.removeAllChildren();
		treeModel.reload();
	}

	// remove object
	/**
	 * This method removes the current selected node from the tree.
	 */
	public void removeCurrentNode() {
		TreePath currentSelection = tree.getSelectionPath();
		if(currentSelection != null) {
			DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)(currentSelection.getLastPathComponent());
			MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());
			if(parent != null) {
				treeModel.removeNodeFromParent(currentNode);
				return;
			}
		}
	}

	// populate tree
	/**
	 * This method populates the tree with entries.
	 */
	public void populate() {
		clear();

		// sort Entries
		Ring myRing = editor.getRing();
		if(myRing == null) {
			return;
		}
		Vector<Item> myEntries = new Vector<Item>(myRing.getItems());
		// TODO User selectable sorting of entries
		Collections.sort(myEntries); // sort entries by title

		rootNode.setUserObject(editor.getFilename());
		
		DefaultMutableTreeNode firstNode = null;
		for(Item item : myEntries) {
			if (filterCategory != 0 &&
					(filterCategory - 1) != item.getCategoryId()) {
				continue;
			}
			DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(item);
			treeModel.insertNodeInto(childNode, rootNode, rootNode.getChildCount());
			if (null == firstNode) {
				firstNode = childNode;
			}
		}
		
		// Expand tree
		if (null != firstNode) {
			tree.scrollPathToVisible(new TreePath(firstNode.getPath()));
		}
		treeModel.reload();
	}
}
