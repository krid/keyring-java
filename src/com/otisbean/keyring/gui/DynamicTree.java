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

// DynamicTree.java

// 22.11.2004

// 24.11.2004: added getTree()
// 08.12.2004: populateTree() updated

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
 */
public class DynamicTree extends JPanel {
	/**
	 * 
	 */
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

	// get item
	/**
	 * This method selects a tree node according to the item object.
	 *
	 * @param newitem Item object
	 */
	public void show(Object newitem) {
		Object nodeInfo;
		
		DefaultMutableTreeNode node = rootNode;

		for(Enumeration enum1 = rootNode.depthFirstEnumeration(); enum1.hasMoreElements(); ) {
			node = (DefaultMutableTreeNode) enum1.nextElement();
			
			if(node != null) {
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

	// category filter
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

	// get Item
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

	// get last selected path component
	/**
	 * This method returns the last selected tree node.
	 *
	 * @return DefaultMutableTreeNode or null if node is not a leaf
	 */
	public DefaultMutableTreeNode getLastNode() {
		DefaultMutableTreeNode temp = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();

		if(temp != null) {
			if(temp.isLeaf()) {
				return temp;
			}
		}

		return null;
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
		int i;
		int start;
		String parent;
		String child;
		DefaultMutableTreeNode node;
		DefaultMutableTreeNode startNode;

		clear();

		rootNode.setUserObject(editor.getFilename());

		// sort Entries
		Ring myRing = editor.getRing();
		if(myRing == null) {
			return;
		}

		Vector<Item> myEntries = new Vector<Item>(myRing.getItems());
		Collections.sort(myEntries); // sort entries by title

		for(Item item : myEntries) {
			Dummy dummy;

			if(filterCategory != 0) {
				if((filterCategory - 1) != item.getCategoryId()) {
					continue;
				}
			}

			start = 0;
			startNode = rootNode;

			// search for separator
			// FIXME Not sure what this separator stuff is about...
			String title = item.getTitle();
			while((i = title.indexOf(editor.getSeparator(), start)) != -1) {
				parent = title.substring(start, i);
				child = title.substring(i + 1, title.length());
				start = i + 1;

				node = searchForBranchNode(startNode, parent);

				if(node == null) {
					// new branch node
					dummy = new Dummy(parent);
					node = addObject(startNode, (Object)dummy, true); // parent node
				}

				startNode = node;
			}

			addObject(startNode, (Object)item, true); // leaf
		}
	}

	// add object
	/**
	 * This method adds a node to the tree.
	 *
	 * @param parent Parent node
	 * @param child Object to add
	 * @param shouldBeVisible show new node
	 */
	public DefaultMutableTreeNode addObject(
		DefaultMutableTreeNode parent,
		Object child,
		boolean shouldBeVisible) {

		DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);

		if(parent == null) {
			parent = rootNode;
		}

		treeModel.insertNodeInto(childNode, parent, parent.getChildCount());

		if(shouldBeVisible) {
			tree.scrollPathToVisible(new TreePath(childNode.getPath()));
		}

		return childNode;
	}

	// ----------------------------------------------------------------
	// private --------------------------------------------------------
	// ----------------------------------------------------------------

	/**
	 * This method searches if category exists as a branch node.
	 *
	 * @param root Root node
	 * @param category Node to search for
	 */
	private static DefaultMutableTreeNode searchForBranchNode(DefaultMutableTreeNode root, String category) {
		Enumeration e = root.children();
		DefaultMutableTreeNode node;
		Dummy branch;

		while(e.hasMoreElements()) {
			node = (DefaultMutableTreeNode)e.nextElement();
			try {
				branch = (Dummy)node.getUserObject();

				if(category.equals(branch.toString())) {
					return node;
				}
			}
			catch(Exception ex) {}; // ignore Item objects
		}

		return null;
	}

	// used for branch nodes
	/**
	 * This class is used to present branch nodes in the tree view.
	 */
	private static class Dummy {
		String title;

		/**
		 * Default constructor.
		 *
		 * @param title Branch node title
		 */
		public Dummy(String title) {
			this.title = title;
		}

		/**
		 * This method returns the branch node title.
		 */
		public String toString() {
			return this.title;
		}
	}
}
