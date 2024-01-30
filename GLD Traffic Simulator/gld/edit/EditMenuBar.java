
/*-----------------------------------------------------------------------
 * Copyright (C) 2001 Green Light District Team, Utrecht University 
 *
 * This program (Green Light District) is free software.
 * You may redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation (version 2 or later).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * See the documentation of Green Light District for further information.
 *------------------------------------------------------------------------*/

package gld.edit;

import gld.*;
import gld.infra.DrivelaneFactory;
import gld.utils.CheckMenu;

import java.awt.*;
import java.util.*;
import java.awt.event.*;

/**
 *
 * The MenuBar for the editor
 *
 * @author Group GUI
 * @version 1.0
 */

public class EditMenuBar extends MenuBar
{
	EditController controller;
	DLMenu dlMenu; //(RM 06) /*POMDPGLD*/ 
	
	public EditMenuBar(EditController ec) {
		controller = ec;
		
		Menu menu; MenuItem item;
		
		add(new FileMenu(controller, true));
		
		menu = new Menu("Edit");
		add(menu);
		EditMenuListener eml = new EditMenuListener();
  	
    item = new MenuItem("Delete", new MenuShortcut(KeyEvent.VK_DELETE));
		menu.add(item);
		item.addActionListener(eml);
 	
		menu.add(new MenuItem("-"));
  	
    item = new MenuItem("Select all", new MenuShortcut(KeyEvent.VK_A));
    menu.add(item);
    item.addActionListener(eml);
  	
    item = new MenuItem("Deselect", new MenuShortcut(KeyEvent.VK_D));
    menu.add(item);
    item.addActionListener(eml);
    
		
		menu = new Menu("Options");
		add(menu);
		OptionMenuListener oml = new OptionMenuListener();
  
		CheckboxMenuItem citem = new CheckboxMenuItem("Toggle grid", false);
		menu.add(citem);
		citem.addItemListener(oml);
		
		item = new MenuItem("Change size...");
		menu.add(item);
		item.addActionListener(oml);

		menu.add(new MenuItem("-"));  
  	
        //(RM 06) /*POMDPGLD*/
        dlMenu = new DLMenu();
        menu.add(dlMenu);
        menu.add(new MenuItem("-"));
        /*POMDPGLD*/
		
	    item = new MenuItem("Validate");
	    menu.add(item);
	    item.addActionListener(oml);
	    
	    item = new MenuItem("Settings...");
	    menu.add(item);
	    item.addActionListener(oml);
	    
	    add(new HelpMenu(controller));
	}

	private class EditMenuListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			String sel = ((MenuItem)e.getSource()).getLabel();
			if(sel.equals("Delete"))
				controller.deleteSelection();
			else if(sel.equals("Select all"))
				controller.selectAll();
			else if(sel.equals("Deselect"))
				controller.deselectAll();
		}
	}

	/*POMDPGLD*/
    //(RM 06)  Menu to controll which types of drivelanes are to be used
    public class DLMenu extends CheckMenu implements ItemListener
    {
        public DLMenu()
        {
            super("Drivelanes", DrivelaneFactory.dlDescs);
            addItemListener(this);
            select(Model.dltype);
        }

        public void itemStateChanged(ItemEvent e)
        {
            controller.setDrivelanetype(getSelectedIndex());
        }

    }
	/*POMDPGLD*/
	
	
	private class OptionMenuListener implements ActionListener, ItemListener
	{
		public void actionPerformed(ActionEvent e) 
		{
			String sel = ((MenuItem) e.getSource()).getLabel();

			if (sel == "Validate")
				controller.validateInfra();
			else if (sel == "Change size...")
				controller.showChangeSizeDialog();
			else if (sel == "Settings...");
				controller.showSettings();
		}
		
		public void itemStateChanged(ItemEvent e)
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
				controller.enableGrid();
			else
				controller.disableGrid();
		}
	}
}