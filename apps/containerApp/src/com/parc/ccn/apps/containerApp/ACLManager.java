package com.parc.ccn.apps.containerApp;

import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;

import com.parc.ccn.data.ContentName;

public class ACLManager extends JDialog implements ActionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JList userList;
	private JList readWriteList;
	private JList readOnlyList;

	private JButton buttonApply;
	private JButton buttonDefault;
	private JButton buttonCancel;
	private JButton buttonAssignReadOnly;
	private JButton buttonRemoveReadOnly;
	private JButton buttonAssingReadWrite;
	private JButton buttonRemoveReadWrite;
	
	
	private JFrame frame;
	private String path;
	
	//private ArrayList<String> readOnlyPrincipals;
	//private ArrayList<String> readWritePrincipals;

private SortedListModel readOnlyPrincipals = null;
private SortedListModel readWritePrincipals = null;
private SortedListModel userPool = null;

/**
	 * Launch the application
 * @param path2 
	 * @param args
	 */
	
	//Get the list of users
	public void getAvailUserList(String path2)
	{
	// grab the list of available users from somewhere
		String principals[] = {"Paulo","Noel","Eliza","Chico","Alex","Aaron","Kelly"};
		userPool.addAll(principals);		
	}

	public void getReadOnlyUsersList(String path2)
	{
		// grab the content object
		// get the list of users that are read only
		// populate the list with the array of users
		String principals[] = {"Mary","Cathy","Janet","Tracy","Natasha","Natalie","Tanya"};
		readOnlyPrincipals.addAll(principals);
		
	}
	
	public void getReadWriteUsersList(String path2)
	{
		// grab the content object
		// get the list of users that are read only
		// populate the list with the array of users
		
		String principals[] = {"Jim","Fred","Bob","Frank","Matsumoto","Jorge","Frederick","Jane"};
		readWritePrincipals.addAll(principals);
		
	}

	//On Close or Apply do this
	//Applies new permissions to User Items
	public void setReadOnlyUsersList()
	{
		// grab the content object
		// apply the changes to the object
		
	}
	
	//On Close or Apply do this
	//Applies new permissions to User Items
	public void setReadWriteUsersList()
	{
		// grab the content object
		// apply the changes to the object		
	}
	
	/**
	 * Create the dialog
	 * @param frame 
	 * @param path 
	 */
	public ACLManager(String path) {

		super();
		
		readOnlyPrincipals = new SortedListModel();
		readWritePrincipals = new SortedListModel();
		userPool = new SortedListModel();
		
		getAvailUserList(path);
		getReadOnlyUsersList(path);
		getReadWriteUsersList(path);
		
		setBounds(100, 100, 615, 442);
		setTitle("Manage Access Controls for "+path);
		getContentPane().setLayout(null);
				
		buttonApply = new JButton();
		buttonApply.addActionListener(this);
		buttonApply.setBounds(73, 367, 141, 25);
		buttonApply.setText("Apply Changes");
		getContentPane().add(buttonApply);

		buttonDefault = new JButton();
		buttonDefault.addActionListener(this);
		buttonDefault.setBounds(261, 367, 123, 25);
		buttonDefault.setText("Revert Default");
		getContentPane().add(buttonDefault);

		buttonCancel = new JButton();
		buttonCancel.addActionListener(this);
		buttonCancel.setBounds(421, 367, 133, 25);
		buttonCancel.setText("Cancel Changes");
		getContentPane().add(buttonCancel);

		final JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(462, 70, 120, 245);
		getContentPane().add(scrollPane_1);
		
		readWriteList = new JList(readWritePrincipals);
		scrollPane_1.setViewportView(readWriteList);
		readWriteList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		readWriteList.setBorder(new BevelBorder(BevelBorder.LOWERED));
		//readWriteList.setBounds(462, 70, 120, 240);		
		//getContentPane().add(readWriteList);

		
		final JScrollPane scrollPane_2 = new JScrollPane();
		scrollPane_2.setBounds(233, 70, 120, 245);
		getContentPane().add(scrollPane_2);
		
		userList = new JList(userPool);
		scrollPane_2.setViewportView(userList);
		userList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		userList.setBorder(new BevelBorder(BevelBorder.LOWERED));
//		userList.setBounds(233, 70, 120, 240);
//		getContentPane().add(userList);

		buttonAssignReadOnly = new JButton();
		buttonAssignReadOnly.addActionListener(this);
		buttonAssignReadOnly.setMargin(new Insets(2, 2, 2, 2));
		buttonAssignReadOnly.setIconTextGap(2);
		buttonAssignReadOnly.setBounds(136, 66, 84, 25);
		buttonAssignReadOnly.setText("<- assign");
		getContentPane().add(buttonAssignReadOnly);

		buttonRemoveReadOnly = new JButton();
		buttonRemoveReadOnly.addActionListener(this);
		buttonRemoveReadOnly.setMargin(new Insets(2, 2, 2, 2));
		buttonRemoveReadOnly.setBounds(135, 290, 79, 25);
		buttonRemoveReadOnly.setText("remove ->");
		getContentPane().add(buttonRemoveReadOnly);

		buttonAssingReadWrite = new JButton();
		buttonAssingReadWrite.addActionListener(this);
		buttonAssingReadWrite.setMargin(new Insets(2, 2, 2, 2));
		buttonAssingReadWrite.setBounds(370, 65, 79, 25);
		buttonAssingReadWrite.setText("assign ->");
		getContentPane().add(buttonAssingReadWrite);

		buttonRemoveReadWrite = new JButton();
		buttonRemoveReadWrite.addActionListener(this);
		buttonRemoveReadWrite.setMargin(new Insets(2, 2, 2, 2));
		buttonRemoveReadWrite.setBounds(370, 290, 79, 25);
		buttonRemoveReadWrite.setText("<- remove");
		getContentPane().add(buttonRemoveReadWrite);

		final JLabel userAndGroupLabel = new JLabel();
		userAndGroupLabel.setBounds(0, 0, 600, 15);
		userAndGroupLabel.setText("User and Group Permissions for " + path);
		getContentPane().add(userAndGroupLabel);

		final JLabel viewPermissionsLabel = new JLabel();
		viewPermissionsLabel.setBounds(8, 44, 122, 20);
		viewPermissionsLabel.setText("View Permissions");
		getContentPane().add(viewPermissionsLabel);

		final JLabel usersLabel = new JLabel();
		usersLabel.setBounds(267, 44, 69, 20);
		usersLabel.setText("Users");
		getContentPane().add(usersLabel);

		final JLabel modifyPermissionsLabel = new JLabel();
		modifyPermissionsLabel.setBounds(462, 44, 132, 20);
		modifyPermissionsLabel.setText("Modify Permissions");
		getContentPane().add(modifyPermissionsLabel);

		final JSeparator separator_3 = new JSeparator();
		separator_3.setBounds(0, 20, 576, 20);
		getContentPane().add(separator_3);

		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(8, 71, 120, 245);
		getContentPane().add(scrollPane);

		readOnlyList = new JList(readOnlyPrincipals);
		scrollPane.setViewportView(readOnlyList);
		readOnlyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		readOnlyList.setBorder(new BevelBorder(BevelBorder.LOWERED));
				
	}

	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(buttonApply == e.getSource()) {
			
		}else if(buttonDefault == e.getSource()){
			
		}else if(buttonCancel == e.getSource()){
			
		}else if(buttonAssignReadOnly == e.getSource()){
			moveListItems(userList.getSelectedIndices(),userList,readOnlyList);
			
		}else if(buttonRemoveReadOnly == e.getSource()){			
			moveListItems(readOnlyList.getSelectedIndices(),readOnlyList,userList);			
			
		}else if(buttonAssingReadWrite == e.getSource()){
			moveListItems(userList.getSelectedIndices(),userList,readWriteList);
			
		}else if(buttonRemoveReadWrite == e.getSource()){
			moveListItems(readWriteList.getSelectedIndices(),readWriteList,userList);
			
		}
		
	}
	
	private void moveListItems(int selectedIndices[],JList fromList,JList toList)
	{
		//ArrayList of selected items
		ArrayList<Object> itemsSelected = new ArrayList<Object>();
		
		//Items selected that are to be removed from the toList
		//ArrayList<Object> itemsToBeRemoved = new ArrayList<Object>();
		
		for(int i=0;i<selectedIndices.length;i++)
		{
			//remove item from fromList and move to toList
			System.out.println("Index is "+ selectedIndices[i]);
			Object selectedItem = fromList.getModel().getElementAt(selectedIndices[i]);
			itemsSelected.add(selectedItem);			
			
		}
		
		//Bulk adding and removal of items
		
		for(Object item : itemsSelected)
		{
			((SortedListModel)toList.getModel()).add(item);
			((SortedListModel)fromList.getModel()).removeElement(item);
			
		}
//		private JList userList;
//		private JList readWriteList;
//		private JList readOnlyList;
		
//		private SortedListModel readOnlyPrincipals = null;
//		private SortedListModel readWritePrincipals = null;
//		private SortedListModel userPool = null;
		
	}

	

}
