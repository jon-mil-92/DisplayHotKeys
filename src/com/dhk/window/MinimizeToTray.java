package com.dhk.window;

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;

/**
 * This class enables the application to be minimized to the system tray and restored from the system tray.
 * 
 * @author Jonathan Miller
 * @version 1.1.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class MinimizeToTray {
	private Frame frame;
	private AppRefresher appRefresher;
	SystemTray systemTray;
	TrayIcon trayIcon;
	PopupMenu trayPopupMenu;
	
	/**
	 * Constructor for the MinimizeToTray class.
	 * 
	 * @param frame - The frame for the view to minimize.
	 * @param appRefresher - The refresher for the given frame.
	 * @param iconResourcePath - The icon resource path for the tray icon.
	 */
	public MinimizeToTray(Frame frame, AppRefresher appRefresher, String iconResourcePath) {
		// Get the frame for the application.
    	this.frame = frame;
    	
    	// Get the refresher for the frame.
    	this.appRefresher = appRefresher;
    	
    	// Get the minimized tray icon.
	    Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource(iconResourcePath));
    	
		// If the system has tray support.
	    if(SystemTray.isSupported()){
	        // Get the system tray of the system.
		    systemTray = SystemTray.getSystemTray();
	
		    // Create a system tray pop-up menu.
		    trayPopupMenu = new PopupMenu();
		    
		    // Set the system tray icon.
		    trayIcon = new TrayIcon(icon, "Display Hot Keys", trayPopupMenu);
		    
		    // Allow the system tray to automatically set the icon size.
		    trayIcon.setImageAutoSize(true);
		    
		    // Add the menu items to the tray's pop-up menu.
		    initListeners();
	    }
	}
	
	/**
	 * This method initializes the items to be included in the system tray pop-up menu.
	 */
	private void initListeners() {
		// Create a restore option for the system tray pop-up menu.
	    MenuItem restore = new MenuItem("Restore");
	    
	    // Create an exit option for the system tray pop-up menu.
	    MenuItem exit = new MenuItem("Exit");
		
		// Create the action listener for the restore option.
	    restore.addActionListener(new ActionListener() {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	        	// Remove the tray icon when it is being restored.
	        	systemTray.remove(trayIcon);
		        
	        	// Restore the frame.
	            frame.setExtendedState(JFrame.NORMAL);
	            
	            // Un-hide the frame.
		        frame.setVisible(true);
		        
		        // Resume the app refresher so the frame continues to update.
		        appRefresher.resume();
	        }
	    });
	    
	    // Add the restore pop-up menu item.
	    trayPopupMenu.add(restore);
	    
	    // Create the action listener for the exit option.
	    exit.addActionListener(new ActionListener() {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	            System.exit(0);             
	        }
	    });
	    
	    // Add the exit pop-up menu item.
	    trayPopupMenu.add(exit);
	    
	    // Create the mouse listener for the double-click to restore feature.
	    trayIcon.addMouseListener(new MouseAdapter() {
	        @Override
	        public void mouseClicked(MouseEvent e) {
	        	// If the user double-clicked on the tray icon.
	            if (e.getClickCount() == 2) {
	            	// Remove the tray icon when it is being restored.
		        	systemTray.remove(trayIcon);
			        
		        	// Restore the frame.
		            frame.setExtendedState(JFrame.NORMAL);
		            
		            // Un-hide the frame.
			        frame.setVisible(true);
			        
			        // Resume the frame refresher.
			        appRefresher.resume();
	            }
	        }
	    });
	}
	
	/**
	 * This method minimizes the application to the system tray.
	 */
	public void execute() {
		// If the system has tray support.
	    if(SystemTray.isSupported()){
			try{
		    	// Minimize the application to the system tray.
		        systemTray.add(trayIcon);
		        
		        // Hide the taskbar icon.
		        frame.setVisible(false);
		    }catch(AWTException awtException){
		        awtException.printStackTrace();
		    }
	    }
	}
}
