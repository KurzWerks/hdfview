/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the files COPYING and Copyright.html. *
 * COPYING can be found at the root of the source code distribution tree.    *
 * Or, see http://hdfgroup.org/products/hdf-java/doc/Copyright.html.         *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.view;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Font;

import org.eclipse.swt.dnd.*;

import swing2swt.layout.BorderLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;

import hdf.HDFVersions;

/**
 * HDFView is the main class of this HDF visual tool. It is used to layout the
 * graphical components of the hdfview. The major GUI components of the HDFView
 * include Menubar, Toolbar, TreeView, ContentView, and MessageArea.
 * <p>
 * The HDFView is designed in such a way that it does not have direct access to
 * the HDF library. All the HDF library access is done through HDF objects.
 * Therefore, the HDFView package depends on the object package but not the
 * library package. The source code of the view package (hdf.view) should
 * be compiled with the library package (hdf.hdflib and hdf.hdf5lib).
 * 
 * @author Jordan T. Henderson
 * @version 2.4 DATE
 */
public class HDFView implements ViewManager, DropTargetListener {	
	
	private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HDFView.class);
	
	private static final Display display = new Display();
	private static Shell mainWindow;
	
	/* The directory where HDFView is installed */
	private String					rootDir;
	
	/* The current working directory */
	private String					currentDir;
	
	/* The current working file */
	private String					currentFile = null;
	
	/* The view properties */
	private ViewProperties			props;
	
	/* A list of tree view implementations. */
    private static List<String>		treeViews;

    /* A list of image view implementations. */
    private static List<String>		imageViews;

    /* A list of tree table implementations. */
    private static List<?>			tableViews;

    /* A list of Text view implementations. */
    private static List<String>		textViews;

    /* A list of metadata view implementations. */
    private static List<?>			metaDataViews;

    /* A list of palette view implementations. */
    private static List<?>			paletteViews;
	
	/* A list of help view implementations. */
	private static List<?>			helpViews;
	
	/* The list of GUI components related to HDF4 */
	private final List<MenuItem> 	h4GUIs = new Vector<MenuItem>();
	
	/* The list of GUI components related to HDF5 */
	private final List<MenuItem> 	h5GUIs = new Vector<MenuItem>();
	
	/* The list of GUI components related to editing */
	//private final List<?>			editGUIs;
	
	/* GUI component: the TreeView */
	private TreeView				treeView;
	
	private static final String 	HDF4_VERSION = HDFVersions.HDF4_VERSION;
	private static final String 	HDF5_VERSION = HDFVersions.HDF5_VERSION;
	private static final String 	HDFVIEW_VERSION = HDFVersions.HDFVIEW_VERSION;
	private static final String 	HDFVIEW_USERSGUIDE_URL = "http://www.hdfgroup.org/products/java/hdfview/UsersGuide/index.html";
	private static final String 	JAVA_COMPILER = "jdk 1.7";
	private static final String		JAVA_VER_INFO = "Compiled at " + JAVA_COMPILER + "\nRunning at " + System.getProperty("java.version");
	
	private static final String		aboutHDFView = "HDF Viewer, " + "Version " + ViewProperties.VERSION + "\n"
    + "For " + System.getProperty("os.name") + "\n\n"
    + "Copyright " + '\u00a9' + " 2006-2015 The HDF Group.\n"
    + "All rights reserved.";
	
	/* String buffer holding the status message */
	private StringBuffer 			message;
	
	/* String buffer holding the metadata information */
	private StringBuffer			metadata;
	
	/* GUI component: The toolbar for open, close, help and hdf4 and hdf5 library information */
	private ToolBar 				toolBar;
	
	/* GUI component: Area to hold file structure tree and data content pane */
	private Composite				contentArea;
	
	/* GUI component: Area where data view windows are shown */
	private Composite				dataArea;
	
	/* GUI component: The text area for showing status messages */
	private Text    				status;
	
	/* GUI component: The text area for quick attribute view */
	private Text					attributeArea;
	
	/* GUI component: To add and display URLs */
	private Combo					url_bar;
	
	/* GUI component: A list of current data windows */
	//private final Menu				windowMenu;
	
	/* GUI component: File menu on the menubar */
	//private final Menu				fileMenu;
	
	/* The offset when a new dataview is added into the main window. */
	private int						frameOffset = 0;
	
	private UserOptionsDialog		userOptionDialog;
	
	private Constructor<?>			ctrSrbFileDialog 	= null;
	
	private Dialog					srbFileDialog 		= null;
	
	/**
     * Constructs HDFView with a given root directory, where the HDFView is
     * installed, and opens the given files in the viewer.
     * <p>
     * 
     * @param root
     *            the directory where the HDFView is installed.
     * @param flist
     *            a list of files to open.
     */	
	public HDFView(String root, List<File> flist, int width, int height, int x, int y) {		
		log.debug("Root is {}", root);
		
		rootDir = root;
		//userOptionsDialog = null;
		//ctrSrbFileDialog = null;
		
		//editGUIs = new Vector<Object>();
		
		ViewProperties.loadIcons();
		ViewProperties.loadExtClass();
		
		props = new ViewProperties(rootDir);
		try {
			props.load();
		} catch (Exception ex) {
			log.debug("Failed to load View Properties from {}", rootDir);
		}
		
		currentDir = ViewProperties.getWorkDir();
		if (currentDir == null) currentDir = System.getProperty("user.home");
		
		log.info("Current directory is {}", currentDir);
		
		treeViews = ViewProperties.getTreeViewList();
        metaDataViews = ViewProperties.getMetaDataViewList();
        textViews = ViewProperties.getTextViewList();
        tableViews = ViewProperties.getTableViewList();
        imageViews = ViewProperties.getImageViewList();
        paletteViews = ViewProperties.getPaletteViewList();
        helpViews = ViewProperties.getHelpViewList();
		
        int n = treeViews.size();
        Class<?> theClass = null;
        for (int i = 0; i < n; i++) {
        	// Use the first available treeview
        	String className = treeViews.get(i);
        	
        	// Enables use of JHDF5 in JNLP (Web Start) applications, the system
        	// class loader with reflection first.
        	try {
        		theClass = Class.forName(className);
        	} catch (Exception ex) {
        		try {
        			theClass = ViewProperties.loadExtClass().loadClass(className);
        		} catch (Exception ex2) {
        			theClass = null;
        		}
        	}
        	
        	if (theClass != null) break;
        }
        
        if (theClass != null) {
        	try {
        		@SuppressWarnings("rawtypes")
        		Class[] paramClass = { Class.forName("hdf.view.ViewManager") };
        		Constructor<?> constructor = theClass.getConstructor(paramClass);
        		Object[] paramObj = { this };
        		treeView = (TreeView) constructor.newInstance(paramObj);
        	} catch (Exception ex) {
        		treeView = null;
        	}
        }
        
        // Initialize all GUI components
		mainWindow = createMainWindow();
		
		try {
			Font font = null;
			String fType = ViewProperties.getFontType();
			int fSize = ViewProperties.getFontSize();
			
			try {
				font = new Font(display, fType, fSize, SWT.NORMAL);
			} catch (Exception ex) {
				font = null;
			}
			
			if (font != null)
				updateFontSize(font);
			
		} catch (Exception ex) {
			log.debug("Failed to load Font properties");
		}
		
		// new DropTarget(display, DND.DROP_COPY);
		
		// Make sure all GUI components are in place before
		// opening any files
		mainWindow.pack();
		
		int nfiles = flist.size();
		File theFile = null;
		for (int i = 0; i < nfiles; i++) {
			theFile = flist.get(i);
			
			if (theFile.isFile()) {
				currentDir = theFile.getParentFile().getAbsolutePath();
				currentFile = theFile.getAbsolutePath();
				
				try {
					treeView.openFile(currentFile, FileFormat.WRITE);
					
					try {
						url_bar.remove(currentFile);
					} catch (Exception ex) {
					}
					
					url_bar.add(currentFile, 0);
					url_bar.select(0);
				} catch (Exception ex) {
					showStatus(ex.toString());
				}
			} else {
				currentDir = theFile.getAbsolutePath();
			}
			
			log.info("CurrentDir is {}", currentDir);
		}
		
		if (FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4) == null)
			setEnabled(h4GUIs, false);
		
		if (FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5) == null)
			setEnabled(h5GUIs, false);
		
		// Set size of main window
		// float inset = 0.17f; // for UG only.
		float inset = 0.04f;
		Point winDim = new Point(width, height);
		
		// If given height and width are too small, adjust accordingly
		if (height <= 300) {
			winDim.y = (int) ((1 - 2 * inset) * mainWindow.getSize().y);
		}
		
		if (width <= 300) {
			winDim.x = (int) (0.9 * (double) mainWindow.getSize().y);
		}
		
		// TEST
        //if (treeView.getClass().getName().startsWith("ext.erdc")) {
        //    topSplitPane.setDividerLocation(500);
        //    winDim.x = (int) (0.9 * mainWindow.getSize().x);
        //    winDim.y = (int) (winDim.x * 0.618);
        //}
        
        mainWindow.setLocation(x, y);
		mainWindow.setMinimumSize(winDim.x, winDim.y);
		
		// Display the window
		mainWindow.open();
		
		while(!mainWindow.isDisposed()) {
			if(!display.readAndDispatch()){
				display.sleep();
			}
		}
		
		try {
			props.save();
		} catch (Exception ex) {
		}
		
		// Close all open files
		try {
			List<FileFormat> filelist = treeView.getCurrentFiles();
			
			if((filelist != null) && (filelist.size() > 0)) {
				Object[] files = filelist.toArray();
				
				for (int i = 0; i < files.length; i++) {
					try {
						treeView.closeFile((FileFormat) files[i]);
					} catch (Throwable ex) {
						continue;
					}
				}
			}
		} catch (Exception ex) {
		}
		
		display.dispose();
	}
	
	/**
     * Creates and lays out GUI components.
     * 
     * <pre>
     * ||=========||=============================||
     * ||         ||                             ||
     * ||         ||                             ||
     * || TreeView||       ContentPane           ||
     * ||         ||                             ||
     * ||=========||=============================||
     * ||            Message Area                ||
     * ||========================================||
     * </pre>
     */
	private Shell createMainWindow() {		
		// Create a new display window
		final Shell shell = new Shell(display);
		shell.setImage(ViewProperties.getHdfIcon());
		shell.setText("HDFView " + HDFVIEW_VERSION);
		shell.setLayout(new BorderLayout(0, 0));
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				closeAllWindows();
				
				List<FileFormat> files = treeView.getCurrentFiles();
                while (!files.isEmpty()) {
                    try {
                        treeView.closeFile(files.get(0));
                    } catch (Exception ex) {
                        
                    }
                }
			}
		});
		
		createMenuBar(shell);
		createToolbar(shell);
		createContentArea(shell);
		
		log.info("Main Window created");
		
		return shell;
	}
	
	private void createMenuBar(final Shell shell) {
		Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);
		
		MenuItem menuItem = new MenuItem(menu, SWT.CASCADE);
		menuItem.setText("&File");
		
		Menu fileMenu = new Menu(menuItem);
		menuItem.setMenu(fileMenu);
		
		MenuItem item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("&Open \tCtrl-O");
		item.setAccelerator(SWT.MOD1 + 'O');
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openLocalFile(null, FileFormat.WRITE);
			}
		});
		
		if(!ViewProperties.isReadOnly()) {
			item = new MenuItem(fileMenu, SWT.PUSH);
			item.setText("Open &Read-Only");
			item.setAccelerator(SWT.MOD1 + 'R');
			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					openLocalFile(null, FileFormat.READ);
				}
			});
		}
		
		// boolean isSrbSupported = true;
        // try {
        // Class.forName("hdf.srb.H5SRB");
        // Class.forName("hdf.srb.SRBFileDialog");
        // } catch (Throwable ex) {isSrbSupported = false;}
        //
        // if (isSrbSupported) {
        // item = new JMenuItem( "Open from iRODS");
        // item.setMnemonic(KeyEvent.VK_S);
        // item.addActionListener(this);
        // item.setActionCommand("Open from irods");
        // fileMenu.add(item);
        // }

		new MenuItem(fileMenu, SWT.SEPARATOR);
		
		MenuItem fileNewMenu = new MenuItem(fileMenu, SWT.CASCADE);
		fileNewMenu.setText("New");
		
		Menu newMenu = new Menu(fileNewMenu);
		fileNewMenu.setMenu(newMenu);
		
		item = new MenuItem(newMenu, SWT.PUSH);
		item.setText("HDF&4");
		h4GUIs.add(item);
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				NewFileDialog dialog = new NewFileDialog(shell, currentDir, FileFormat.FILE_TYPE_HDF4, treeView.getCurrentFiles());
				String filename = dialog.open();
				
				if(!dialog.isFileCreated() || filename == null)
					return;
				
				try {
					treeView.openFile(filename, FileFormat.WRITE);
					currentFile = filename;
					
					try {
						url_bar.remove(filename);
					} catch (Exception ex) {
					}
					
					url_bar.add(filename, 0);
					url_bar.select(0);
				} catch (Exception ex) {
					display.beep();
					showError(ex.getMessage() + "\n" + filename, null);
				}
			}
		});
		
		item = new MenuItem(newMenu, SWT.PUSH);
		item.setText("HDF&5");
		h5GUIs.add(item);
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				NewFileDialog dialog = new NewFileDialog(shell, currentDir, FileFormat.FILE_TYPE_HDF5, treeView.getCurrentFiles());
				String filename = dialog.open();
				
				if(!dialog.isFileCreated() || filename == null)
					return;
				
				try {
					treeView.openFile(filename, FileFormat.WRITE);
					currentFile = filename;
					
					try {
						url_bar.remove(filename);
					} catch (Exception ex) {
					}
					
					url_bar.add(filename, 0);
					url_bar.select(0);
				} catch (Exception ex) {
					display.beep();
					showError(ex.getMessage() + "\n" + filename, null);
				}
			}
		});
		
		new MenuItem(fileMenu, SWT.SEPARATOR);
		
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("&Close");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				closeFile(treeView.getSelectedFile());
			}
		});
		
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("Close &All");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				closeAllWindows();
				
				List<FileFormat> files = treeView.getCurrentFiles();
				while (!files.isEmpty()) {
					try {
						treeView.closeFile(files.get(0));
					} catch (Exception ex) {
						
					}
				}
				
				currentFile = null;
				attributeArea.setText("");
				url_bar.setText("");
			}
		});
		
		new MenuItem(fileMenu, SWT.SEPARATOR);
		
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("&Save");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (treeView.getCurrentFiles().size() <= 0) {
					showError("No files currently open.", shell.getText());
					return;
				}
				
				// Save what has been changed in memory into file
				try {
					FileFormat file = treeView.getSelectedFile();
					List<Shell> views = getDataViews();
					Object theView = null;
					TableView tableView = null;
					TextView textView = null;
					FileFormat theFile = null;
					
					for (Iterator<Shell> i = views.iterator(); i.hasNext(); ) {
						theView = i.next();
						
						if (theView instanceof TableView) {
							tableView = (TableView) theView;
							theFile = tableView.getDataObject().getFileFormat();
							if (file.equals(theFile)) tableView.updateValueInFile();
						}
						else if (theView instanceof TextView) {
							textView = (TextView) theView;
							theFile = textView.getDataObject().getFileFormat();
							if (file.equals(theFile)) textView.updateValueInFile();
						}
					}
				} catch (Exception ex) {
					display.beep();
					showError(ex.getMessage(), shell.getText());
				}
			}
		});
		
	    item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("S&ave As");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (treeView.getCurrentFiles().size() <= 0) {
					showError("No files currently open.", shell.getText());
					return;
				}
				
				try {
					treeView.saveFile(treeView.getSelectedFile());
				} catch (Exception ex) {
					display.beep();
					showError(ex.getMessage(), shell.getText());
				}
			}
		});
		
		new MenuItem(fileMenu, SWT.SEPARATOR);
		
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("E&xit \tCtrl-Q");
		item.setAccelerator(SWT.MOD1 + 'Q');
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
                List<FileFormat> files = treeView.getCurrentFiles();
                while (!files.isEmpty()) {
                    try {
                        treeView.closeFile(files.get(0));
                    } catch (Exception ex) {
                        
                    }
                }
			    
			    display.dispose();
			}
		});
		
		menuItem = new MenuItem(menu, SWT.CASCADE);
		menuItem.setText("&Window");
		
		Menu windowMenu = new Menu(menuItem);
		menuItem.setMenu(windowMenu);
		
		item = new MenuItem(windowMenu, SWT.PUSH);
		item.setText("&Cascade");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				//cascadeWindows();
				notYetImplemented();
			}
		});
		
		item = new MenuItem(windowMenu, SWT.PUSH);
		item.setText("&Tile");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				//tileWindows();
				notYetImplemented();
			}
		});
		
		new MenuItem(windowMenu, SWT.SEPARATOR);
		
		item = new MenuItem(windowMenu, SWT.PUSH);
		item.setText("Close &Window");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (shell.getShells().length <= 0 || (display.getActiveShell().equals(shell)))
					return;
				
				display.getActiveShell().dispose();
			}
		});
		
		item = new MenuItem(windowMenu, SWT.PUSH);
		item.setText("Close &All");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				closeAllWindows();
			}
		});
		
		menuItem = new MenuItem(menu, SWT.CASCADE);
		menuItem.setText("&Tools");
		
		Menu toolsMenu = new Menu(menuItem);
		menuItem.setMenu(toolsMenu);
		
		MenuItem convertMenuItem = new MenuItem(toolsMenu, SWT.CASCADE);
		convertMenuItem.setText("Convert Image To");
		
		Menu convertMenu = new Menu(convertMenuItem);
		convertMenuItem.setMenu(convertMenu);
		
		item = new MenuItem(convertMenu, SWT.PUSH);
		item.setText("HDF4");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				convertFile(Tools.FILE_TYPE_IMAGE, FileFormat.FILE_TYPE_HDF4);
			}
		});
		h4GUIs.add(item);
		
		item = new MenuItem(convertMenu, SWT.PUSH);
		item.setText("HDF5");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				convertFile(Tools.FILE_TYPE_IMAGE, FileFormat.FILE_TYPE_HDF5);
			}
		});
		h5GUIs.add(item);
		
		new MenuItem(toolsMenu, SWT.SEPARATOR);
		
		item = new MenuItem(toolsMenu, SWT.PUSH);
		item.setText("User &Options");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (userOptionDialog == null)
					userOptionDialog = new UserOptionsDialog(shell, rootDir);
				
				userOptionDialog.open();
				
				if (userOptionDialog.isWorkDirChanged())
					currentDir = ViewProperties.getWorkDir();
				
				if (userOptionDialog.isFontChanged()) {
					Font font = null;
					
					try {
						font = new Font(display, ViewProperties.getFontType(), ViewProperties.getFontSize(), SWT.NORMAL);
					} catch (Exception ex) {
						font = null;
					}
					
					if (font != null)
						updateFontSize(font);
				}
			}
		});
		
		new MenuItem(toolsMenu, SWT.SEPARATOR);
		
		item = new MenuItem(toolsMenu, SWT.PUSH);
		item.setText("&Register File Format");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				registerFileFormat();
			}
		});
		
		item = new MenuItem(toolsMenu, SWT.PUSH);
		item.setText("&Unregister File Format");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				unregisterFileFormat();
			}
		});
		
		menuItem = new MenuItem(menu, SWT.CASCADE);
		menuItem.setText("&Help");
		
		Menu helpMenu = new Menu(menuItem);
		menuItem.setMenu(helpMenu);
		
		item = new MenuItem(helpMenu, SWT.PUSH);
		item.setText("&User's Guide");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.swt.program.Program.launch(HDFVIEW_USERSGUIDE_URL);
			}
		});
		
		if ((helpViews != null) && (helpViews.size() > 0)) {
            int n = helpViews.size();
            for (int i = 0; i < n; i++) {
                HelpView theView = (HelpView) helpViews.get(i);
                item = new MenuItem(helpMenu, SWT.PUSH);
                item.setText(theView.getLabel());
                //item.setActionCommand(theView.getActionCommand());
            }
        }
		
		new MenuItem(helpMenu, SWT.SEPARATOR);
		
		item = new MenuItem(helpMenu, SWT.PUSH);
		item.setText("HDF&4 Library Version");
		h4GUIs.add(item);
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				LibraryVersionDialog dialog = new LibraryVersionDialog(shell, FileFormat.FILE_TYPE_HDF4);
				dialog.open();
			}
		});
		
		item = new MenuItem(helpMenu, SWT.PUSH);
		item.setText("HDF&5 Library Version");
		h5GUIs.add(item);
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				LibraryVersionDialog dialog = new LibraryVersionDialog(shell, FileFormat.FILE_TYPE_HDF5);
				dialog.open();
			}
		});
		
		item = new MenuItem(helpMenu, SWT.PUSH);
		item.setText("&Java Version");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				MessageBox versionInfo = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
				versionInfo.setText(shell.getText());
				versionInfo.setMessage(JAVA_VER_INFO);
				// Add custom HDF Icon
				versionInfo.open();
			}
		});
		
		new MenuItem(helpMenu, SWT.SEPARATOR);
		
		item = new MenuItem(helpMenu, SWT.PUSH);
		item.setText("Supported Fi&le Formats");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Enumeration<?> formatKeys = FileFormat.getFileFormatKeys();
				
				String formats = "\nSupported File Formats: \n";
				while (formatKeys.hasMoreElements()) {
					formats += "    " + formatKeys.nextElement() + "\n";
				}
				formats += "\n";
				
				MessageBox message = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
				message.setText(shell.getText());
				message.setMessage(formats);
				message.open();
			}
		});
		
		new MenuItem(helpMenu, SWT.SEPARATOR);
		
		item = new MenuItem(helpMenu, SWT.PUSH);
		item.setText("&About...");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				MessageBox info = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
				info.setText(shell.getText());
				info.setMessage(aboutHDFView);
				// Add HDF Icon
				info.open();
			}
		});
		
		log.info("Menubar created");
	}

	private void createToolbar(final Shell shell) {
		toolBar = new ToolBar(shell, SWT.HORIZONTAL | SWT.RIGHT | SWT.BORDER_DOT);
		toolBar.setFont(Display.getCurrent().getSystemFont());
		toolBar.setLayoutData(BorderLayout.NORTH);
		
		ToolItem openItem = new ToolItem(toolBar, SWT.PUSH);
		openItem.setToolTipText("Open");
		openItem.setImage(ViewProperties.getFileopenIcon());
		openItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openLocalFile(null, FileFormat.WRITE);
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR).setWidth(4);
		
		ToolItem closeItem = new ToolItem(toolBar, SWT.PUSH);
		closeItem.setImage(ViewProperties.getFilecloseIcon());
		closeItem.setToolTipText("Close");
		closeItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				closeFile(treeView.getSelectedFile());
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR).setWidth(20);
		
		ToolItem helpItem = new ToolItem(toolBar, SWT.PUSH);
		helpItem.setImage(ViewProperties.getHelpIcon());
		helpItem.setToolTipText("Help");
		helpItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String ugPath = ViewProperties.getUsersGuide();
				
				if(ugPath == null || !ugPath.startsWith("http://")) {
					String sep = File.separator;
					File tmpFile = new File(ugPath);
					
					if(!(tmpFile.exists())) {
						ugPath = rootDir + sep + "UsersGuide" + sep + "index.html";
						tmpFile = new File(ugPath);
						
						if(!(tmpFile.exists())) {
							ugPath = HDFVIEW_USERSGUIDE_URL;
						}
						
						ViewProperties.setUsersGuide(ugPath);
					}
				}
				
				try {
					org.eclipse.swt.program.Program.launch(ugPath);
				} catch (Exception ex) {
					MessageBox error = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
					error.setText(shell.getText());
					error.setMessage(ex.getMessage());
					// Add HDF Icon
					error.open();
				}
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR).setWidth(4);
		
		ToolItem hdf4Item = new ToolItem(toolBar, SWT.PUSH);
		hdf4Item.setImage(ViewProperties.getH4Icon());
		hdf4Item.setToolTipText("HDF4 Library Version");
		hdf4Item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				LibraryVersionDialog dialog = new LibraryVersionDialog(shell, FileFormat.FILE_TYPE_HDF4);
				dialog.open();
			}
		});
		
		if(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4) == null) {
			hdf4Item.setEnabled(false);
		}
		
		new ToolItem(toolBar, SWT.SEPARATOR).setWidth(4);
		
		ToolItem hdf5Item = new ToolItem(toolBar, SWT.PUSH);
		hdf5Item.setImage(ViewProperties.getH5Icon());
		hdf5Item.setToolTipText("HDF5 Library Version");
		hdf5Item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				LibraryVersionDialog dialog = new LibraryVersionDialog(shell, FileFormat.FILE_TYPE_HDF5);
				dialog.open();
			}
		});
		
		if(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5) == null) {
			hdf5Item.setEnabled(false);
		}
		
		// Make the toolbar as wide as the window and as
		// tall as the buttons
		toolBar.setSize(shell.getClientArea().width, openItem.getBounds().height);
		toolBar.setLocation(0, 0);
		
		log.info("Toolbar created");
	}

	private void createContentArea(final Shell shell) {
		SashForm content = new SashForm(shell, SWT.VERTICAL);
		content.setSashWidth(10);
		
		// Add Url Toolbar, Data content area and Status Area to main window
		Composite container = new Composite(content, SWT.NONE);
		container.setLayout(new FormLayout());
		
	    Composite statusArea = new Composite(content, SWT.NONE);
	    statusArea.setLayout(new FillLayout(SWT.HORIZONTAL));
	    
	    Composite urlToolbarContainer = new Composite(container, SWT.NONE);
	    urlToolbarContainer.setLayout(new FormLayout());
	    FormData formData = new FormData();
	    formData.top = new FormAttachment(0, 0);
	    formData.left = new FormAttachment(0, 0);
	    formData.right = new FormAttachment(100, 0);
	    urlToolbarContainer.setLayoutData(formData);
		
		Button btnRecentFiles = new Button(urlToolbarContainer, SWT.NONE);
		btnRecentFiles.setText("Recent Files");
		btnRecentFiles.setToolTipText("List of recent files");
		btnRecentFiles.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				url_bar.setListVisible(true);
			}
		});
		
		url_bar = new Combo(urlToolbarContainer, SWT.NONE);
		url_bar.setItems(ViewProperties.getMRF().toArray(new String[0]));
		url_bar.setVisibleItemCount(ViewProperties.MAX_RECENT_FILES);
		url_bar.deselectAll();
		url_bar.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.CR) {
					String filename = url_bar.getText();
					if (filename == null || filename.length() < 1 || filename.equals(currentFile)) {
						return;
					}
					
					if(!(filename.startsWith("http://") || filename.startsWith("ftp://"))) {
					    openLocalFile(filename, FileFormat.WRITE);
					} else {
						openRemoteFile(filename);
					}
				}
			}
		});
		url_bar.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String filename = url_bar.getText();
				
				if(filename.length() <= 0) return;
				
				if(!(filename.startsWith("http://") || filename.startsWith("ftp://"))) {
			        openLocalFile(filename, FileFormat.WRITE);
				} else {
					openRemoteFile(filename);
				}
			}
		});
		
		Button btnClearText = new Button(urlToolbarContainer, SWT.NONE);
		btnClearText.setToolTipText("Clear current selection");
		btnClearText.setText("Clear Text");
		btnClearText.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				url_bar.setText("");
				url_bar.deselectAll();
			}
		});
		
		// Set layout for Recent Files button
		formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		formData.bottom = new FormAttachment(100, 0);
		formData.right = new FormAttachment(url_bar, 0);
		btnRecentFiles.setLayoutData(formData);
		
		// Set layout for URL toolbar
		formData = new FormData();
		formData.top = new FormAttachment(0, 1);
		formData.left = new FormAttachment(10, 0);
		formData.bottom = new FormAttachment(100, 0);
		formData.right = new FormAttachment(92, 0);
		url_bar.setLayoutData(formData);
		
		// Set layout for Clear Text button
		formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(url_bar, 0);
		formData.bottom = new FormAttachment(100, 0);
		formData.right = new FormAttachment(100, 0);
		btnClearText.setLayoutData(formData);
		
		log.info("URL Toolbar created");
		
		SashForm contentArea = new SashForm(container, SWT.HORIZONTAL);
	    contentArea.setSashWidth(10);
	    formData = new FormData();
	    formData.left = new FormAttachment(0, 0);
	    formData.right = new FormAttachment(100, 0);
	    formData.top = new FormAttachment(urlToolbarContainer, 0);
	    formData.bottom = new FormAttachment(100, 0);
	    contentArea.setLayoutData(formData);
	    
	    // Add TreeView and DataView to content area pane
		ScrolledComposite treeArea = new ScrolledComposite(contentArea, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		treeArea.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		treeArea.setExpandHorizontal(true);
		treeArea.setExpandVertical(true);
		
		dataArea = new Composite(contentArea, SWT.BORDER);
		dataArea.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		
		// Could not load user's treeview, use default treeview.
		if (treeView == null) treeView = new DefaultTreeView(this, treeArea);
		treeArea.setContent(treeView.getTree());
		
        // Create status area for displaying messages and metadata
        CTabFolder tabFolder = new CTabFolder(statusArea, SWT.BORDER | SWT.FLAT);
		tabFolder.setTabPosition(SWT.BOTTOM);
		tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		
		CTabItem tbtmLogInfo = new CTabItem(tabFolder, SWT.NONE);
		tbtmLogInfo.setText("Log Info");
		
		status = new Text(tabFolder, SWT.V_SCROLL | SWT.MULTI);
		status.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
		status.setEditable(false);
		message = new StringBuffer();
		metadata = new StringBuffer();
		showStatus("HDFView root - " + rootDir);
		showStatus("User property file - " + ViewProperties.getPropertyFile());
		
		tbtmLogInfo.setControl(status);
		
		CTabItem tbtmNewItem = new CTabItem(tabFolder, SWT.NONE);
		tbtmNewItem.setText("Metadata");
		
		attributeArea = new Text(tabFolder, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		attributeArea.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
		attributeArea.setEditable(false);
		tbtmNewItem.setControl(attributeArea);
		
		// Set Log Info to show first in status area
		tabFolder.setSelection(0);
		
		content.setWeights(new int[] {9, 1});
		contentArea.setWeights(new int[] {1, 3});
	}
	
	private void registerFileFormat() {
		String msg = "Register a new file format by \nKEY:FILE_FORMAT:FILE_EXTENSION\n"
            + "where, KEY: the unique identifier for the file format"
            + "\n           FILE_FORMAT: the full class name of the file format"
            + "\n           FILE_EXTENSION: the file extension for the file format" + "\n\nFor example, "
            + "\n\t to add NetCDF, \"NetCDF:hdf.object.nc2.NC2File:nc\""
            + "\n\t to add FITS, \"FITS:hdf.object.fits.FitsFile:fits\"\n\n";
		
		// Add custom HDFLarge icon to dialog
		String str = (new InputDialog(mainWindow, SWT.ICON_INFORMATION, 
		        "Register a file format", msg)).open();

		if ((str == null) || (str.length() < 1)) return;

    	int idx1 = str.indexOf(':');
    	int idx2 = str.lastIndexOf(':');

    	if ((idx1 < 0) || (idx2 <= idx1)) {
    	    showError("Failed to register " + str
                    + "\n\nMust in the form of KEY:FILE_FORMAT:FILE_EXTENSION",
                    "Register File Format");
        	return;
    	}

    	String key = str.substring(0, idx1);
    	String className = str.substring(idx1 + 1, idx2);
    	String extension = str.substring(idx2 + 1);

    	// Check if the file format has been registered or the key is taken.
    	String theKey = null;
    	String theClassName = null;
    	Enumeration<?> local_enum = FileFormat.getFileFormatKeys();
    	while (local_enum.hasMoreElements()) {
        	theKey = (String) local_enum.nextElement();
        	if (theKey.endsWith(key)) {
        	    showError("Invalid key: " + key + " is taken.", "Register File Format");
            	return;
        	}

        	theClassName = FileFormat.getFileFormat(theKey).getClass().getName();
        	if (theClassName.endsWith(className)) {
        	    showError("The file format has already been registered: " + className,
        	            "Register File Format");
            	return;
        	}
    	}

    	// Enables use of JHDF5 in JNLP (Web Start) applications, the system
    	// class loader with reflection first.
    	Class<?> theClass = null;
    	try {
        	theClass = Class.forName(className);
    	}
    	catch (Exception ex) {
        	try {
            	theClass = ViewProperties.loadExtClass().loadClass(className);
        	}
        	catch (Exception ex2) {
            	theClass = null;
        	}
    	}
    	
    	if (theClass == null) {
    		return;
    	}

    	try {
        	Object theObject = theClass.newInstance();
        	if (theObject instanceof FileFormat) {
        		FileFormat.addFileFormat(key, (FileFormat) theObject);
        	}
    	}
    	catch (Throwable ex) {
    	    showError("Failed to register " + str + "\n\n" + ex, "Register File Format");
        	return;
    	}

    	if ((extension != null) && (extension.length() > 0)) {
        	extension = extension.trim();
        	String ext = ViewProperties.getFileExtension();
        	ext += ", " + extension;
        	ViewProperties.setFileExtension(ext);
    	}
	}
	
	private void unregisterFileFormat() {
		Enumeration<?> keys = FileFormat.getFileFormatKeys();
		ArrayList<Object> keyList = new ArrayList<Object>();
		
		while (keys.hasMoreElements())
			keyList.add((Object) keys.nextElement());
		
		// Add custom HDFLarge icon to dialog
		String theKey = (new InputDialog(mainWindow, "Unregister a file format", 
		        "Unregister a file format:\n\n" + keyList.toArray())).open();
		
		if (theKey == null) return;
		
		FileFormat.removeFileFormat(theKey);
	}

    /**
     * Returns a list of all open DataViews
     */
    public List<Shell> getDataViews() {
        Shell[] openShells = mainWindow.getShells();
        if ((openShells == null) || (openShells.length <= 0)) return null;

        Vector<Shell> views = new Vector<Shell>(openShells.length);
        for (int i = 0; i < openShells.length; i++) {
        	if (openShells[i] instanceof DataView)
        		views.add(openShells[i]);
        }

        return views;
    }
	
	/**
     * @return a list of treeview implementations.
     */
    public static final List<String> getListOfTreeView() {
        return treeViews;
    }

    /**
     * @return a list of imageview implementations.
     */
    public static final List<String> getListOfImageView() {
        return imageViews;
    }

    /**
     * @return a list of tableview implementations.
     */
    public static final List<?> getListOfTableView() {
        return tableViews;
    }

    /**
     * @return a list of textview implementations.
     */
    public static final List<?> getListOfTextView() {
        return textViews;
    }

    /**
     * @return a list of metaDataview implementations.
     */
    public static final List<?> getListOfMetaDataView() {
        return metaDataViews;
    }

    /**
     * @return a list of paletteview implementations.
     */
    public static final List<?> getListOfPaletteView() {
        return paletteViews;
    }
    
	public TreeView getTreeView() {
		return treeView;
	}
    
    /**
     * Display feedback message.
     * 
     * @param msg
     *            the message to display.
     */
    public void showStatus(String msg) {
        message.append(msg);
        message.append("\n");
        status.setText(message.toString());
    }
    
    private void showError(String errorMsg, String title) {
    	MessageBox error = new MessageBox(mainWindow, SWT.ICON_ERROR | SWT.OK);
    	error.setText(title);
    	error.setMessage(errorMsg);
    	error.open();
    }
    
    public void showMetaData(HObject obj) {
        if (obj == null || currentFile == null) return;

        log.trace("showMetaData: start");
        metadata.setLength(0);
        metadata.append(obj.getName());

        String oidStr = null;
        long[] OID = obj.getOID();
        if (OID != null) {
            oidStr = String.valueOf(OID[0]);
            for (int i = 1; i < OID.length; i++) {
                oidStr += ", " + OID[i];
            }
        }
        metadata.append(" (");
        metadata.append(oidStr);
        metadata.append(")");

        if (obj instanceof Group) {
            log.trace("showMetaData: instanceof Group");
            Group g = (Group) obj;
            metadata.append("\n    Group size = ");
            metadata.append(g.getMemberList().size());
        }
        else if (obj instanceof Dataset) {
            log.trace("showMetaData: instanceof Dataset");
            Dataset d = (Dataset) obj;
            if (d.getRank() <= 0) {
                d.init();
            }
            log.trace("showMetaData: inited");

            metadata.append("\n    ");
            if (d instanceof ScalarDS) {
                Datatype dtype = d.getDatatype();
                if (dtype != null) metadata.append(dtype.getDatatypeDescription());
            }
            else if (d instanceof CompoundDS) {
                metadata.append("Compound/Vdata");
            }
            metadata.append(",    ");

            long dims[] = d.getDims();

            if (dims != null) {
                metadata.append(dims[0]);
                for (int i = 1; i < dims.length; i++) {
                    metadata.append(" x ");
                    metadata.append(dims[i]);
                }
            }
        } // else if (obj instanceof Dataset)
        else {
            log.debug("obj not instanceof Group or Dataset");
        }

        List<?> attrList = null;
        try {
            log.trace("showMetaData: getMetadata");
            attrList = obj.getMetadata();
        }
        catch (Exception ex) {
            log.debug("getMetadata failure: ", ex);
        }

        if (attrList == null) {
            metadata.append("\n    Number of attributes = 0");
        }
        else {
            int n = attrList.size();
            log.trace("showMetaData: append {} attributes", n);
            metadata.append("\n    Number of attributes = ");
            metadata.append(n);

            for (int i = 0; i < n; i++) {
                log.trace("showMetaData: append Object[{}]", i);
                Object attrObj = attrList.get(i);
                if (!(attrObj instanceof Attribute)) {
                    continue;
                }
                Attribute attr = (Attribute) attrObj;
                metadata.append("\n        ");
                metadata.append(attr.getName());
                metadata.append(" = ");
                metadata.append(attr.toString(","));
                log.trace("showMetaData: append Object[{}]={}", i, attr.getName());
            }
        }

        attributeArea.setText(metadata.toString());
        attributeArea.setSelection(0);
        log.trace("showMetaData: finish");
    }
    
    public void reloadFile() {
    	int temp_index_type = 0;
    	int temp_index_order = 0;
    	
    	FileFormat theFile = treeView.getSelectedFile();
    	if (theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
    		temp_index_type = theFile.getIndexType(null);
    		temp_index_order = theFile.getIndexOrder(null);
    	}
    	closeFile(theFile);
    	
    	if (theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))) {
    		theFile.setIndexType(temp_index_type);
    		theFile.setIndexOrder(temp_index_order);
    	}
    	
    	try {
    		treeView.reopenFile(theFile);
    	} catch (Exception ex) {
    	}
    }
    
    public void addDataView(DataView dataView) {
    	
    }
    
    public void removeDataView(DataView dataView) {
    	
    }
    
    public DataView getDataView(HObject dataObject) {
    	
    	return null;
    }
    
    public void dragEnter(DropTargetEvent evt) {
    }
    
    public void dragLeave(DropTargetEvent evt) {
    }
    
    public void dragOperationChanged(DropTargetEvent evt) {
    }
    
    public void dragOver(DropTargetEvent evt) {
    }
    
    public void drop(DropTargetEvent evt) {
    	
    }
    
    public void dropAccept(DropTargetEvent evt) {
    }
    
    // Get the data area which HDFView uses to display
    // DataViews in
    public Composite getDataArea() {
    	return dataArea;
    }

    /**
     * Set default UI fonts.
     */
    private void updateFontSize(Font font) {
    	if (font == null) return;
    	
    	/*
    	UIDefaults defaults = UIManager.getLookAndFeelDefaults();

        for (Iterator<?> i = defaults.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            if (defaults.getFont(key) != null) {
                UIManager.put(key, new javax.swing.plaf.FontUIResource(font));
            }
        }
        
        SwingUtilities.updateComponentTreeUI(this);
        */
    }
    
    /**
     * Bring window to the front.
     * <p>
     * 
     * @param name
     * 			  the name of the window to show.
     */
    private void showWindow(final Shell shell) {
    	// Return if main window (shell) is the only open shell
    	if (mainWindow.getShells().length < 1) return;
    	
    	shell.getDisplay().asyncExec(new Runnable() {
    		public void run() {
    			shell.forceActive();
    		}
    	});
    }
    
    /**
     * Cascade all windows.
     */
    private void cascadeWindows() {
    	Shell[] sList = mainWindow.getShells();
    	
    	// Return if main window (shell) is the only open shell
    	if (sList.length < 1) return;
    	
    	int x = 2, y = 2;
    	Shell shell = null;    	
    	Point p = dataArea.getSize();
    	int w = Math.max(50, p.x - 100);
    	int h = Math.max(50, p.y - 100);
    	
    	for (int i = 0; i < sList.length; i++) {
    		shell = sList[i];
    		shell.setBounds(x, y, w, h);
    		shell.setActive();
    		x += 20;
    		y += 20;
    	}
    }
    
    /**
     * Tile all windows.
     */
    private void tileWindows() {
    	Shell[] sList = mainWindow.getShells();
    	
    	// Return if main window (shell) is the only open shell
    	if (sList.length < 1) return;
    	
    	int x = 0, y = 0, idx = 0;
    	Shell shell = null;
    	
    	int n = sList.length;
    	int cols = (int) Math.sqrt(n);
    	int rows = (int) Math.ceil((double) n / (double) cols);
    	
    	Point p = contentArea.getSize();
    	int w = p.x / cols;
    	int h = p.y / rows;
    	
    	for (int i = 0; i < rows; i++) {
    		x = 0;
    		
    		for (int j = 0; j < cols; j++) {
    			idx = i * cols + j;
    			if (idx >= n)
    				return;
    			
    			shell = sList[idx];
    			shell.setBounds(x, y, w, h);
    			x += w;
    		}
    		
    		y += h;
    	}
    }
    
    /**
     * Closes all windows.
     */
    private void closeAllWindows() {
    	Shell[] sList = mainWindow.getShells();
    	
    	for(int i = 0; i < sList.length; i++) {
    		System.out.println("Shell: " + sList[i].getText());
    	}
    	
    	// Return if main window (shell) is the only open shell
    	if (sList.length < 1) return;
    	
    	Shell shell = null;
    	for (int i = 0; i < sList.length; i++) {
    		shell = sList[i];
    		shell.dispose();
    	}
    	
    	shell = null;
    }
    
    /* Enable and disable GUI components */
    private static void setEnabled(List<MenuItem> list, boolean b) {
    	Iterator<MenuItem> it = list.iterator();
    	
    	while (it.hasNext())
    		it.next().setEnabled(b);
    }
    
    /** Open local file */
    private void openLocalFile(String filename, int fileAccessID) {
    	int accessMode = fileAccessID;
    	if (ViewProperties.isReadOnly()) accessMode = FileFormat.READ;
    	
    	String[] selectedFilenames = null;
    	File[] chosenFiles = null;
    	
    	if (filename != null) {
    		File file = new File(filename);
    		if(!file.exists()) {
    		    showError("File " + filename + " does not exist.", "Open File");
    		    return;
    		}
    		
			if(file.isDirectory()) {
				currentDir = filename;
				openLocalFile(null, FileFormat.WRITE); // needs to be edited
			} else {
    		    currentFile = filename;
			}
			
			try {
		        url_bar.remove(filename);
		    }
		    catch (Exception ex) {
		    }
		    
		    url_bar.add(filename, 0);
            url_bar.select(0);
    		
    		try {
		        treeView.openFile(filename, accessMode);
		    }
		    catch (Throwable ex) {
		        try {
	                treeView.openFile(filename, FileFormat.READ);
	            }
	            catch (Throwable ex2) {
	                display.beep();
	                url_bar.deselectAll();
	                showError("Failed to open file " + filename + "\n" + ex2, mainWindow.getText());
	                currentFile = null;
	            }
		    }
    	} else {
    		FileDialog fChooser = new FileDialog(mainWindow, SWT.OPEN | SWT.MULTI);
    		fChooser.setFilterExtensions(new String[] {"*.*", "*.h4;*.h5;*.hdf;*.hdf4;*.hdf5;*.he2;*.he5"});
    		fChooser.setFilterNames(new String[] {"All Files", "HDF File"});
    		fChooser.setFilterIndex(0);
    		fChooser.setFilterPath(null);
    		fChooser.open();
    		
    		selectedFilenames = fChooser.getFileNames();
    		if(selectedFilenames.length <= 0) return;
    		
    		chosenFiles = new File[selectedFilenames.length];
    		for(int i = 0; i < chosenFiles.length; i++) {
    		    chosenFiles[i] = new File(fChooser.getFilterPath() + File.separator + selectedFilenames[i]);
    		    
    		    if(!chosenFiles[i].exists()) {
        		    showError("File " + chosenFiles[i].getName() + " does not exist.", "Open File");
        		    continue;
        		}
    		    
    		    if (chosenFiles[i].isDirectory()) {
                    currentDir = chosenFiles[i].getPath();
                } else {
                    currentDir = chosenFiles[i].getParent();
                }
    		    
    		    try {
    		        url_bar.remove(chosenFiles[i].getAbsolutePath());
    		    }
    		    catch (Exception ex) {
    		    }
    		    
    		    url_bar.add(chosenFiles[i].getAbsolutePath(), 0);
	            url_bar.select(0);
    		    
    		    try {
    		        treeView.openFile(chosenFiles[i].getAbsolutePath(), accessMode + FileFormat.OPEN_NEW);
    		    }
    		    catch (Throwable ex) {
    		        try {
    	                treeView.openFile(chosenFiles[i].getAbsolutePath(), FileFormat.READ);
    	            }
    	            catch (Throwable ex2) {
    	                display.beep();
    	                url_bar.deselectAll();
    	                showError("Failed to open file " + selectedFilenames[i] + "\n" + ex2, mainWindow.getText());
    	                currentFile = null;
    	            }
    		    }
    		}
    		
    		currentFile = chosenFiles[0].getAbsolutePath();
    	}
    }
    
    /** Load remote file and save it to local temporary directory */
    private String openRemoteFile(String urlStr) {
    	if (urlStr == null)
    		return null;
    	
    	String localFile = null;
    	
    	if(urlStr.startsWith("http://")) {
    		localFile = urlStr.substring(7);
    	}
    	else if (urlStr.startsWith("ftp://")) {
    		localFile = urlStr.substring(6);
    	}
    	else {
    		return null;
    	}
    	
    	localFile = localFile.replace('/', '@');
    	localFile = localFile.replace('\\', '@');
    	
    	// Search the local file cache
    	String tmpDir = System.getProperty("java.io.tmpdir");
    	
    	File tmpFile = new File(tmpDir);
    	if (!tmpFile.canWrite()) tmpDir = System.getProperty("user.home");
    	
    	localFile = tmpDir + localFile;
    	
    	tmpFile = new File(localFile);
    	if (tmpFile.exists())
    		return localFile;
    	
    	URL url = null;
    	
    	try {
    		url = new URL(urlStr);
    	} catch (Exception ex) {
    		url = null;
    		display.beep();
    		showError(ex.getMessage(), mainWindow.getText());
    		return null;
    	}
    	
    	BufferedInputStream in = null;
    	BufferedOutputStream out = null;
    	
    	try {
    		in = new BufferedInputStream(url.openStream());
    		out = new BufferedOutputStream(new FileOutputStream(tmpFile));
    	} catch (Exception ex) {
    		in = null;
    		display.beep();
    		showError(ex.getMessage(), mainWindow.getText());
    		
    		try {
    			out.close();
    		} catch (Exception ex2) {
    			log.debug("Remote file: ", ex2);
    		}
    		
    		return null;
    	}
    	
    	mainWindow.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT));
    	byte[] buff = new byte[512]; // set default buffer size to 512
    	try {
    		int n = 0;
    		while ((n = in.read(buff)) > 0) {
    			out.write(buff, 0, n);
    		}
    	}
    	catch (Exception ex) {
    		log.debug("Remote file: ", ex);
    	}
    	
    	try {
    		in.close();
    	} catch (Exception ex) {
    		log.debug("Remote file: ", ex);
    	}
    	
    	try {
    		out.close();
    	} catch (Exception ex) {
    		log.debug("Remote file: ", ex);
    	}
    	
    	mainWindow.setCursor(null);
    	
    	return localFile;
    }
    
    /** Open file from SRB server */
    private void openFromSRB() throws Exception {
        if (ctrSrbFileDialog == null) {
            Class<?> theClass = null;

            try {
                theClass = Class.forName("hdf.srb.SRBFileDialog");
            }
            catch (Exception ex) {
                theClass = null;
                showStatus(ex.toString());
                throw (new ClassNotFoundException("Cannot find SRBFileDialog"));
            }

            try {
                @SuppressWarnings("rawtypes")
                Class[] paramClass = { Class.forName("java.awt.Frame") };
                ctrSrbFileDialog = theClass.getConstructor(paramClass);
            }
            catch (Exception ex) {
                ctrSrbFileDialog = null;
                throw (new InstantiationException("Cannot construct SRBFileDialog"));
            }
        }

        if (srbFileDialog == null) {
            //try {
            //    Object[] paramObj = { (java.awt.Frame) this };
            //   srbFileDialog = (JDialog) ctrSrbFileDialog.newInstance(paramObj);
            //}
            //catch (Exception ex) {
            //    throw ex;
            //}
        }
        else {
            //srbFileDialog.setVisible(true);
        }

        // currentFile = srbFileDialog.getName();
    }
    
    private void closeFile(FileFormat theFile) {
    	if (theFile == null) {
    		display.beep();
    		showError("Select a file to close", mainWindow.getText());
    		return;
    	}
    	
    	// Close all the data windows of this file
    	Shell[] views = mainWindow.getShells();
        if (views != null) {
            for (int i = 0; i < views.length; i++) {
                HObject obj = (HObject) (((DataView) views[i]).getDataObject());
                if (obj == null) {
                    continue;
                }

                if (obj.getFileFormat().equals(theFile)) {
                    views[i].dispose();
                    views[i] = null;
                }
            }
        }
    	
    	String fName = (String) url_bar.getItem(url_bar.getSelectionIndex());
    	if (theFile.getFilePath().equals(fName)) {
    		currentFile = null;
    		url_bar.setText("");
    	}
    	
    	try {
    		treeView.closeFile(theFile);
    	} catch (Exception ex) {	
    	}
    	
    	theFile = null;
    	attributeArea.setText("");
    	System.gc();
    }
    
    private void convertFile(String typeFrom, String typeTo) {
    	ImageConversionDialog dialog = new ImageConversionDialog(mainWindow, typeFrom, typeTo,
    			currentDir, treeView.getCurrentFiles());
    	dialog.open();
    	
    	if (dialog.isFileConverted()) {
    		String filename = dialog.getConvertedFile();
    		File theFile = new File(filename);
    		
    		if (!theFile.exists()) return;
    	    
    		currentDir = theFile.getParentFile().getAbsolutePath();
    		currentFile = theFile.getAbsolutePath();
    		
    		try {
    			treeView.openFile(filename, FileFormat.WRITE);
    			
    			try {
    				url_bar.remove(filename);
    			} catch (Exception ex) {
    			}
    			
    			url_bar.add(filename, 0);
				url_bar.select(0);
    		} catch (Exception ex) {
    			showStatus(ex.toString());
    		}
    	}
    }
    
    private void notYetImplemented() {
    	showError("Functionality not yet implemented.", mainWindow.getText());
    }
    
    private class LibraryVersionDialog extends Dialog {
    	private String libType;
    	private String message;
    	
    	public LibraryVersionDialog(Shell parent, String libType) {
    		super(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
    		this.libType = libType;
    		
    		if(libType == FileFormat.FILE_TYPE_HDF4)
    			setMessage("HDF4 " + HDF4_VERSION);
    		else if (libType == FileFormat.FILE_TYPE_HDF5)
    			setMessage("HDF5 " + HDF5_VERSION);
    	}
    	
    	public String getMessage() {
    		return message;
    	}
    	
    	public void setMessage(String message) {
    		this.message = message;
    	}
    	
    	public void open() {
    		Shell dialog = new Shell(getParent(), getStyle());
    		dialog.setText("HDFView");
    		createContents(dialog);
    		dialog.pack();

    		// Center the window relative to the main HDFView window
    		Point winCenter = new Point(
    				mainWindow.getBounds().x + (mainWindow.getBounds().width / 2),
    				mainWindow.getBounds().y + (mainWindow.getBounds().height / 2));
    		
    		dialog.setSize(250, 200);
    		dialog.setLocation(winCenter.x - (dialog.getSize().x / 2), winCenter.y - (dialog.getSize().y / 2));
    		
    		dialog.open();
    		
    		Display display = getParent().getDisplay();
    		while (!dialog.isDisposed()) {
    			if (!display.readAndDispatch()) {
    				display.sleep();
    			}
    		}
    	}
    	
    	private void createContents(final Shell shell) {
    		shell.setLayout(new BorderLayout(0,0));
    		
    		// Draw HDF Icon and Version string
    		Composite canvasComposite = new Composite(shell, SWT.NONE);
    		canvasComposite.setLayoutData(BorderLayout.CENTER);
    		canvasComposite.setLayout(new FillLayout());
    		
    		Canvas canvas = new Canvas(canvasComposite, SWT.NO_REDRAW_RESIZE);
    		final Image hdfLarge = new Image(display, HDFView.class.getResourceAsStream("icons/hdf_large.gif"));
    		canvas.addPaintListener(new PaintListener() {
    			public void paintControl(PaintEvent e) {
    				e.gc.drawImage(hdfLarge, 10, 20);
    				e.gc.drawText(message, 100, 40);
    			}
    		});
    		
    		Composite buttonComposite = new Composite(shell, SWT.NONE);
    		buttonComposite.setLayoutData(BorderLayout.SOUTH);
    		RowLayout buttonLayout = new RowLayout();
    		buttonLayout.center = true;
    		buttonLayout.justify = true;
    		buttonLayout.type = SWT.HORIZONTAL;
    		buttonComposite.setLayout(buttonLayout);
    		
    		Button okButton = new Button(buttonComposite, SWT.PUSH);
    		okButton.setText("OK");
    		shell.setDefaultButton(okButton);
    		okButton.addSelectionListener(new SelectionAdapter() {
    			public void widgetSelected(SelectionEvent e) {
    				shell.dispose();
    			}
    		});
    	}
    }
    
    /**
     * The starting point of this application.
     * 
     * <pre>
     * Usage: java(w)
     *        -Dhdf.hdf5lib.H5.hdf5lib="your HDF5 library path"
     *        -Dhdf.hdflib.HDFLibrary.hdflib="your HDF4 library path"
     *        -root "the directory where the HDFView is installed"
     *        [filename] "the file to open"
     * </pre>
     */
	public static void main(String[] args) {
		String rootDir = System.getProperty("hdfview.root");
		if(rootDir == null) rootDir = System.getProperty("user.dir");
		
		File tmpFile = null;
		int j = args.length;
		int W = display.getClientArea().width / 2,
			H = display.getClientArea().height,
			X = 0,
			Y = 0;
		
		for(int i = 0; i < args.length; i++) {
			if ("-root".equalsIgnoreCase(args[i])) {
				j--;
				try {
					j--;
					tmpFile = new File(args[++i]);
					
					if(tmpFile.isDirectory()) {
						rootDir = tmpFile.getPath();
					}
					else if(tmpFile.isFile()) {
						rootDir = tmpFile.getParent();
					}
				} catch (Exception ex) {
					
				}
			}
			else if("-g".equalsIgnoreCase(args[i]) || "-geometry".equalsIgnoreCase(args[i])) {
				j--;
				// -geometry WIDTHxHEIGHT+XOFF+YOFF
				try {
					String geom = args[++i];
					j--;
					
					int idx = 0;
					int idx2 = geom.lastIndexOf('-');
					int idx3 = geom.lastIndexOf('+');
					
					idx = Math.max(idx2, idx3);
					if(idx > 0) {
						Y = Integer.parseInt(geom.substring(idx + 1));
						
						if(idx == idx2)
							Y = -Y;
						
						geom = geom.substring(0, idx);
						idx2 = geom.lastIndexOf('-');
						idx3 = geom.lastIndexOf('+');
						idx = Math.max(idx2, idx3);
						
						if(idx > 0) {
							X = Integer.parseInt(geom.substring(idx + 1));
							
							if(idx == idx2)
								X = -X;
							
							geom = geom.substring(0, idx);
						}
					}
					
					idx = geom.indexOf('x');
					
					if(idx > 0) {
						W = Integer.parseInt(geom.substring(0, idx));
						H = Integer.parseInt(geom.substring(idx + 1));
					}
					
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			else if("-java.version".equalsIgnoreCase(args[i])) {
				/* Set icon to ViewProperties.getLargeHdfIcon() */
				MessageBox info = new MessageBox(mainWindow, SWT.ICON_INFORMATION | SWT.OK);
				info.setText(mainWindow.getText());
				info.setMessage(JAVA_VER_INFO);
				info.open();
				System.exit(0);
			}
		}
		
		Vector<File> fList = new Vector<File>();
		tmpFile = null;
		
		if(j >= 0) {
			for(int i = args.length - j; i < args.length; i++) {
				tmpFile = new File(args[i]);
				if(tmpFile.exists() && (tmpFile.isFile() || tmpFile.isDirectory()))
					fList.add(new File(tmpFile.getAbsolutePath()));
			}
		}
		
		final Vector<File> the_fList = fList;
		final String the_rootDir = rootDir;
		final int the_X = X, the_Y = Y, the_W = W, the_H = H;
		
		display.syncExec(new Runnable() {
			public void run() {
				new HDFView(the_rootDir, the_fList, the_W, the_H, the_X, the_Y);
			}
		});
	}
}
