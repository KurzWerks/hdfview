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

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hdf.object.DataFormat;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;

/**
 * NewLinkDialog shows a message dialog requesting user input for creating
 * new links.
 * 
 * @author Jordan T. Henderson
 * @version 2.4 1/1/2016
 */
public class NewLinkDialog extends Dialog {
	private static final long serialVersionUID = 7100424106041533918L;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NewLinkDialog.class);

    private Shell         shell;
    
    private Text          nameField;

    private Combo         parentChoice, targetObject;
    
    private String        currentDir;
    
    private Text          targetFile;
    
    private Button        targetFileButton;
    
    private Button        hardLink, softLink, externalLink;

    /** a list of current groups */
    private List<Group> groupList;

    /** a list of current objects */
    private List<?>       objList;
    
    private HObject       newObject;
    private Group         parentGroup;

    private FileFormat    fileFormat;
    
    private final List<?> fileList;
    
    /**
     * Constructs a NewLinkDialog with specified list of possible parent groups.
     * 
     * @param parent
     *            the parent shell of the dialog
     * @param pGroup
     *            the parent group which the new group is added to.
     * @param objs
     *            the list of all objects.
     * @param files
     *            the list of all files open in the TreeView
     */
    public NewLinkDialog(Shell parent, Group pGroup, List<?> objs, List<FileFormat> files) {
    	super(parent, SWT.APPLICATION_MODAL);
    	
    	newObject = null;
    	parentGroup = pGroup;
    	objList = objs;
        
        fileFormat = pGroup.getFileFormat();
        currentDir = ViewProperties.getWorkDir();
        fileList = files;
    }
    
    public void open() {
    	Shell parent = getParent();
    	shell = new Shell(parent, SWT.TITLE | SWT.CLOSE |
    			SWT.BORDER | SWT.APPLICATION_MODAL);
    	shell.setText("New Link...");
    	shell.setImage(ViewProperties.getHdfIcon());
    	shell.setLayout(new GridLayout(1, true));
    	
    	// Create the main content region
    	Composite content = new Composite(shell, SWT.NONE);
    	content.setLayout(new GridLayout(2, false));
    	content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    	Label label = new Label(content, SWT.LEFT);
    	label.setText("Link name: ");
    	
    	nameField = new Text(content, SWT.SINGLE | SWT.BORDER);
    	nameField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    	label = new Label(content, SWT.LEFT);
    	label.setText("Parent group: ");
    	
    	parentChoice = new Combo(content, SWT.DROP_DOWN | SWT.BORDER);
    	parentChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	parentChoice.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			parentGroup = (Group) groupList.get(parentChoice.getSelectionIndex());
    		}
    	});
    	
        Composite helpComposite = new Composite(content, SWT.NONE);
        helpComposite.setLayout(new GridLayout(1, true));
        helpComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        
        label = new Label(helpComposite, SWT.LEFT);
        label.setText("Type of Link: ");
        
        Button helpButton = new Button(helpComposite, SWT.PUSH);
        helpButton.setImage(ViewProperties.getHelpIcon());
        helpButton.setToolTipText("Help on Links");
        helpButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        helpButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		final String msg = "The Type of Link specifies which type of link the user wants to create. \n"
                        + "It could be hard, soft or external links. \n\n"
                        + "Hard Link: \n"
                        + "Hard Link creates a hard link to a pre-existing object in an HDF5 file. \n"    
                        + "The target object must already exist in the file.\n" 
                        + "The HDF5 library keeps a count of all hard links pointing to an object. \n\n"
                        + "Soft Link: \n"
                        + "Soft Link creates a new soft link to an object in an HDF5 file. \n" 
                        + "Soft links are only for use only if the target object is in the current file. \n"
                        + "Unlike hard links, a soft link in an HDF5 file is allowed to dangle, \n" 
                        + "meaning that the target object need not exist at the time that the link is created."
                        + "The HDF5 library does not keep a count of soft links.  \n\n"
                        + "External Link: \n"
                        + "External Link creates a new soft link to an external object, which is an \n" 
                        + "object in a different HDF5 file from the location of the link. External links are"
                        + "allowed to dangle like soft links. \n\n"
                        + "Soft links and external links are also known as symbolic links as they use " 
                        + "a name to point to an object; hard links employ an object's address in the file.  \n\n\n";
                
        		MessageBox info = new MessageBox(shell, SWT.ICON_INFORMATION);
        		info.setText(shell.getText());
        		info.setMessage(msg);
        		info.open();
        	}
        });
        
        Composite typeComposite = new Composite(content, SWT.BORDER);
        typeComposite.setLayout(new GridLayout(3, true));
        typeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        hardLink = new Button(typeComposite, SWT.RADIO);
        hardLink.setText("Hard Link");
        hardLink.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        hardLink.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		targetFile.setEnabled(false);
                targetFileButton.setEnabled(false);
                targetObject.setEnabled(true);
                targetObject.addKeyListener(new KeyListener() {
                	public void keyPressed(KeyEvent e) {
                		e.doit = false;
                	}
                	
                	public void keyReleased(KeyEvent e) {
                		e.doit = false;
                	}
                });
                
                retrieveObjects(fileFormat);
        	}
        });
        
        softLink = new Button(typeComposite, SWT.RADIO);
        softLink.setText("Soft Link");
        softLink.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        softLink.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		targetFile.setEnabled(false);
                targetFileButton.setEnabled(false);
                targetObject.setEnabled(true);
                //targetObject.setEditable(true);
                
                retrieveObjects(fileFormat);
        	}
        });
        
        externalLink = new Button(typeComposite, SWT.RADIO);
        externalLink.setText("External Link");
        externalLink.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        externalLink.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		targetFile.setEnabled(true);
                targetFileButton.setEnabled(true);
                targetObject.setEnabled(true);
                //targetObject.setEditable(true);
                targetObject.removeAll();
        	}
        });
        
        label = new Label(content, SWT.LEFT);
        label.setText("Target File: ");
        
        Composite fileComposite = new Composite(content, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 0;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        fileComposite.setLayout(layout);
        fileComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        targetFile = new Text(fileComposite, SWT.SINGLE | SWT.BORDER);
        targetFile.setEnabled(false);
        targetFile.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        targetFileButton = new Button(fileComposite, SWT.PUSH);
        targetFileButton.setText("Browse...");
        targetFileButton.setEnabled(false);
        targetFileButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        targetFileButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		String filename = null;
                filename = openTargetFile();
                
                if (filename == null) {
                    return;
                }
                
                targetFile.setText(filename);
        	}
        });
        
        label = new Label(content, SWT.LEFT);
        label.setText("Target Object: ");
        
        targetObject = new Combo(content, SWT.DROP_DOWN | SWT.READ_ONLY);
        targetObject.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        groupList = new Vector<Group>(objList.size());
        Object obj = null;
        Iterator<?> iterator = objList.iterator();
        String full_name = null;
        int idx_root = -1, idx = -1;
        while (iterator.hasNext()) {
        	obj = iterator.next();
            idx++;
            
            if (obj instanceof Group) {
                Group g = (Group) obj;
                groupList.add(g);
                if (g.isRoot()) {
                    full_name = HObject.separator;
                    idx_root = idx;
                }
                else {
                    full_name = g.getPath() + g.getName() + HObject.separator;
                }
                parentChoice.add(full_name);
            }
            else {
                full_name = ((HObject) obj).getPath() + ((HObject) obj).getName();
            }

            targetObject.add(full_name);
        }
    	
        targetObject.remove(idx_root);
        objList.remove(idx_root);
    	
        if (parentGroup.isRoot()) {
            parentChoice.select(parentChoice.indexOf(HObject.separator));
        }
        else {
            parentChoice.select(parentChoice.indexOf(parentGroup.getPath() + parentGroup.getName()
                    + HObject.separator));
        }
    	
    	
    	// Create the Ok/Cancel button region
    	Composite buttonComposite = new Composite(shell, SWT.NONE);
    	buttonComposite.setLayout(new GridLayout(2, true));
    	buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    	Button okButton = new Button(buttonComposite, SWT.PUSH);
    	okButton.setText("   &Ok   ");
        GridData gridData = new GridData(SWT.END, SWT.FILL, true, false);
        gridData.widthHint = 70;
        okButton.setLayoutData(gridData);
        okButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		newObject = createLink();

                if (newObject != null) {
                    shell.dispose();
                }
        	}
        });
    	
        Button cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setText("&Cancel");
        gridData = new GridData(SWT.BEGINNING, SWT.FILL, true, false);
        gridData.widthHint = 70;
        cancelButton.setLayoutData(gridData);
        cancelButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		newObject = null;
                shell.dispose();
                ((Vector<Group>) groupList).setSize(0);
        	}
        });
        
        hardLink.setSelection(true);
    	
        shell.pack();
        
        Point computedSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        shell.setSize(computedSize.x + 200, computedSize.y);
        
        Rectangle parentBounds = parent.getBounds();
        Point shellSize = shell.getSize();
        shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                          (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));
        
        shell.open();
        
        Display display = parent.getDisplay();
        while(!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }
    
    private HObject createLink() {
        String name = null;
        Group pgroup = null;
        HObject obj = null;

        name = nameField.getText().trim();
        if ((name == null) || (name.length() < 1)) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            error.setText(shell.getText());
            error.setMessage("Link name is not specified.");
            error.open();
            return null;
        }

        if (name.indexOf(HObject.separator) >= 0) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            error.setText(shell.getText());
            error.setMessage("Link name cannot contain path.");
            error.open();
            return null;
        }

        pgroup = (Group) groupList.get(parentChoice.getSelectionIndex());

        if (pgroup == null) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            error.setText(shell.getText());
            error.setMessage("Parent group is null.");
            error.open();
            return null;
        }    

        if (hardLink.getSelection()) {
            HObject targetObj = (HObject) objList.get(targetObject
                    .getSelectionIndex());

            if (targetObj == null) {
                shell.getDisplay().beep();
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                error.setText(shell.getText());
                error.setMessage("Target object is null.");
                error.open();
                return null;
            }

            if ((targetObj instanceof Group) && ((Group) targetObj).isRoot()) {
                shell.getDisplay().beep();
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                error.setText(shell.getText());
                error.setMessage("Cannot make a link to the root group.");
                error.open();
                return null;
            }
            
            try {
                obj = fileFormat.createLink(pgroup, name, targetObj);
            }
            catch (Exception ex) {
                shell.getDisplay().beep();
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                error.setText(shell.getText());
                error.setMessage(ex.getMessage());
                error.open();
                return null;
            }          
        }
        else if (softLink.getSelection()){
            String target_name = targetObject.getText();
            if (target_name.length() < 1)  {
                shell.getDisplay().beep();
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                error.setText(shell.getText());
                error.setMessage("Target object name is not specified.");
                error.open();
                return null;
            }

            HObject targetObj = null;
            try {
                targetObj = fileFormat.get(targetObject.getText());
            } 
            catch (Exception ex) {
            	log.debug("soft link:", ex);
            }
                   
            String tObj = null;
            if(targetObj==null){
                tObj = targetObject.getText();
                
                if (!tObj.startsWith(HObject.separator)) {
                    tObj = HObject.separator + tObj;
                }
            }
            
            if ((targetObj instanceof Group) && ((Group) targetObj).isRoot()) {
                shell.getDisplay().beep();
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                error.setText(shell.getText());
                error.setMessage("Cannot make a link to the root group.");
                error.open();
                return null;
            }

            try {
                if(targetObj !=null)
                    obj = fileFormat.createLink(pgroup, name, targetObj, Group.LINK_TYPE_SOFT);
                else if(tObj!=null)
                    obj = fileFormat.createLink(pgroup, name, tObj, Group.LINK_TYPE_SOFT);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                shell.getDisplay().beep();
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                error.setText(shell.getText());
                error.setMessage(ex.getMessage());
                error.open();
                return null;
            }
        }
        else if (externalLink.getSelection()){
            String TargetFileName = targetFile.getText();
            FileFormat TargetFileFormat = null;
            int fileAccessID = FileFormat.FILE_CREATE_OPEN;

            File TargetFile = new File(TargetFileName);

            if (!TargetFile.exists()) {               
                return null;
            }
            FileFormat h5format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
            try {
                //h5format.close();
                TargetFileFormat = h5format.createInstance(TargetFileName, fileAccessID);
                TargetFileFormat.open(); //open the file
            } 
            catch (Exception ex) {
            	log.debug("external link:", ex);
                return null;
            } 
            
            HObject targetObj = null;
            try{
                targetObj = TargetFileFormat.get(targetObject.getText());
            }
            catch (Exception ex) {
                ex.printStackTrace();
            } 
            
            try {                            
                TargetFileFormat.close();
            } 
            catch (Exception ex) {
            	log.debug("external link:", ex);
            }
                 
            String tFileObj = null;         
            if(targetObj==null){
                String tObj = null;
                tObj = targetObject.getText();
                if (tObj.length() < 1)  {
                    shell.getDisplay().beep();
                    MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                    error.setText(shell.getText());
                    error.setMessage("Target object name not specified.");
                    error.open();
                    return null;
                }
                tFileObj = TargetFileName + FileFormat.FILE_OBJ_SEP + tObj;
            }
            // should allow to link to the root of an external file            
            //            if ((targetObj instanceof Group) && ((Group) targetObj).isRoot()) {
            //                toolkit.beep();
            //                JOptionPane.showMessageDialog(this,
            //                        "Cannot make a link to the root group.", getTitle(),
            //                        JOptionPane.ERROR_MESSAGE);
            //                return null;
            //            }

            try {
                if(targetObj !=null)
                	obj = fileFormat.createLink(pgroup, name, targetObj, Group.LINK_TYPE_EXTERNAL);
                else if(tFileObj!=null)
                    obj = fileFormat.createLink(pgroup, name, tFileObj, Group.LINK_TYPE_EXTERNAL);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                shell.getDisplay().beep();
                MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
                error.setText(shell.getText());
                error.setMessage(ex.getMessage());
                error.open();
                return null;
            }
        }
        
        return obj;
    }
    
    private String openTargetFile() {
    	FileDialog fchooser = new FileDialog(shell, SWT.OPEN);
    	fchooser.setFilterPath(currentDir);
    	fchooser.setFilterExtensions(new String[] {"*.*, *.h5;*.hdf4;*.hdf;*.h4;*.he5;*.he2;*.hdf5"});
    	fchooser.setFilterNames(new String[] {"All Files", "HDF & more"});
    	fchooser.setFilterIndex(1);
    	//fchooser.setFileFilter(DefaultFileFilter.getFileFilter());

    	if(fchooser.open() == null) return null;

    	File choosedFile = new File(fchooser.getFilterPath() + File.separator + fchooser.getFileName());

    	if (!choosedFile.exists()) {
    		return null;
    	}

    	if (choosedFile.isDirectory()) {
    		currentDir = choosedFile.getPath();
    	} 
    	else {
    		currentDir = choosedFile.getParent();
    	}

    	return choosedFile.getAbsolutePath();
    }
    
    //Function to check if the target File is open in TreeView
    private boolean isFileOpen(String filename)
    {
    	boolean isOpen = false;
    	FileFormat theFile = null;

    	Iterator<?> iterator = fileList.iterator();
    	while(iterator.hasNext()) {
    		theFile = (FileFormat)iterator.next();
    		if (theFile.getFilePath().equals(filename)) {
    			isOpen = true;
    			if(!theFile.isThisType(FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5))){
    				targetObject.setEnabled(false);       
    			}
    			retrieveObjects(theFile);
    			break;
    		}
    	} // while(iterator.hasNext())

    	return isOpen;
    }
    
    // Retrieves the list of objects from the file
    private void retrieveObjects(FileFormat file) {
        HObject obj = null;
        Iterator<?> iterator = objList.iterator();
        String full_name = null;
        targetObject.removeAll();
        while (iterator.hasNext()) {
            obj = (HObject) iterator.next();

            if (obj instanceof Group) {
                Group g = (Group) obj;
                if (g.isRoot()) {
                    full_name = HObject.separator;
                }
                else {
                    full_name = g.getPath() + g.getName() + HObject.separator;
                }
            }
            else {
                full_name = obj.getPath() + obj.getName();
            }
            
            targetObject.add(full_name);
        }
    }

    /** Returns the new dataset created. */
    public DataFormat getObject() {
        return newObject;
    }

    /** Returns the parent group of the new dataset. */
    public Group getParentGroup() {
        return parentGroup;
    }
}
