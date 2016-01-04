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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hdf.object.DataFormat;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;

/**
 * NewImageDialog shows a message dialog requesting user input for creating a
 * new HDF4/5 Image.
 * 
 * @author Jordan T. Henderson
 * @version 2.4 1/1/2016
 */
public class NewImageDialog extends Dialog {
	private static final long serialVersionUID = 6204900461720887966L;
	
	private Shell       shell;

    private Text        nameField, widthField, heightField;

    private Combo       parentChoice;

    private Button      checkIndex, checkTrueColor, checkInterlacePixel,
                        checkInterlacePlane;

    /** A list of current groups */
    private List<Group> groupList;
    
    private List<?>     objList;

    private boolean     isH5;

    private HObject     newObject;
    private Group       parentGroup;

    private FileFormat  fileFormat;	
	
    /**
     * Constructs a NewImageDialog with specified list of possible parent groups.
     * 
     * @param parent
     *            the parent shell of the dialog
     * @param pGroup
     *            the parent group which the new group is added to.
     * @param objs
     *            the list of all objects.
     */
	public NewImageDialog(Shell parent, Group pGroup, List<?> objs) {
		super(parent, SWT.APPLICATION_MODAL);
		
		newObject = null;
		parentGroup = pGroup;
		objList = objs;

        isH5 = pGroup.getFileFormat().isThisType(
                FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5));
        fileFormat = pGroup.getFileFormat();
	}

	public void open() {
		Shell parent = getParent();
		shell = new Shell(parent, SWT.TITLE | SWT.CLOSE |
    			SWT.BORDER | SWT.APPLICATION_MODAL);
    	shell.setText("New HDF Image...");
    	shell.setImage(ViewProperties.getHdfIcon());
    	shell.setLayout(new GridLayout(1, true));
    	
    	
    	// Create main content region
    	Composite content = new Composite(shell, SWT.BORDER);
    	content.setLayout(new GridLayout(2, false));
    	content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    	
    	Label label = new Label(content, SWT.LEFT);
    	label.setText("Image name: ");
    	
    	nameField = new Text(content, SWT.SINGLE | SWT.BORDER);
    	nameField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    	label = new Label(content, SWT.LEFT);
    	label.setText("Parent Group: ");
    	
    	parentChoice = new Combo(content, SWT.DROP_DOWN);
    	parentChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	parentChoice.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			parentGroup = groupList.get(parentChoice.getSelectionIndex());
    		}
    	});
    	
        groupList = new Vector<Group>();
        Object obj = null;
        Iterator<?> iterator = objList.iterator();
        while (iterator.hasNext()) {
            obj = iterator.next();
            if (obj instanceof Group) {
                Group g = (Group) obj;
                groupList.add(g);
                if (g.isRoot()) {
                    parentChoice.add(HObject.separator);
                }
                else {
                    parentChoice.add(g.getPath() + g.getName()
                            + HObject.separator);
                }
            }
        }

        if (parentGroup.isRoot()) {
            parentChoice.select(parentChoice.indexOf(HObject.separator));
        }
        else {
            parentChoice.select(parentChoice.indexOf(parentGroup.getPath() + parentGroup.getName()
                    + HObject.separator));
        }
        
        label = new Label(content, SWT.LEFT);
        label.setText("Height: ");
        
        heightField = new Text(content, SWT.SINGLE | SWT.BORDER);
        heightField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        label = new Label(content, SWT.LEFT);
        label.setText("Width: ");
        
        widthField = new Text(content, SWT.SINGLE | SWT.BORDER);
        widthField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        label = new Label(content, SWT.LEFT);
        label.setText("Image type: ");
        
        Composite typeComposite = new Composite(content, SWT.BORDER);
        typeComposite.setLayout(new GridLayout(2, true));
        typeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        checkIndex = new Button(typeComposite, SWT.RADIO);
        checkIndex.setText("Indexed colormap");
        checkIndex.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        checkIndex.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		checkInterlacePixel.setSelection(true);
        		checkInterlacePlane.setSelection(false);
        		checkInterlacePixel.setEnabled(false);
                checkInterlacePlane.setEnabled(false);
        	}
        });
        
        checkTrueColor = new Button(typeComposite, SWT.RADIO);
        checkTrueColor.setText("24-bit truecolor");
        checkTrueColor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        checkTrueColor.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		checkInterlacePixel.setEnabled(true);
                checkInterlacePlane.setEnabled(true);
        	}
        });
        
        label = new Label(content, SWT.LEFT);
        label.setText("Data layout: ");
        
        Composite layoutComposite = new Composite(content, SWT.BORDER);
        layoutComposite.setLayout(new GridLayout(2, true));
        layoutComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        checkInterlacePixel = new Button(layoutComposite, SWT.RADIO);
        checkInterlacePixel.setText("Pixel interlace");
        checkInterlacePixel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        checkInterlacePlane = new Button(layoutComposite, SWT.RADIO);
        checkInterlacePlane.setText("Plane interlace");
        checkInterlacePlane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    	
    	// Create Ok/Cancel button region
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
    			newObject = createHDFimage();
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
    	
    	checkIndex.setSelection(true);
    	checkInterlacePixel.setSelection(true);
        checkInterlacePixel.setEnabled(false);
        checkInterlacePlane.setEnabled(false);
    	
        shell.pack();
        
        shell.setSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        
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
	
	private Dataset createHDFimage() {
        Dataset dataset = null;

        String name = nameField.getText();
        if (name != null) {
            name = name.trim();
        }
        if ((name == null) || (name.length() <= 0)) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            error.setText(shell.getText());
            error.setMessage("Dataset name is not specified.");
            error.open();
            return null;
        }

        if (name.indexOf(HObject.separator) >= 0) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            error.setText(shell.getText());
            error.setMessage("Dataset name cannot contain path.");
            error.open();
            return null;
        }

        Group pgroup = (Group) groupList.get(parentChoice.getSelectionIndex());
        
        if (pgroup == null) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            error.setText(shell.getText());
            error.setMessage("Select a parent group.");
            error.open();
            return null;
        }

        int w = 0, h = 0;
        try {
            w = Integer.parseInt(widthField.getText());
            h = Integer.parseInt(heightField.getText());
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            error.setText(shell.getText());
            error.setMessage(ex.getMessage());
            error.open();
            return null;
        }

        long[] dims = null;
        int tclass = Datatype.CLASS_CHAR;
        int tsign = Datatype.SIGN_NONE;
        int tsize = 1;
        int torder = Datatype.NATIVE;
        int interlace = ScalarDS.INTERLACE_PIXEL;
        int ncomp = 2;

        if (checkIndex.getSelection()) {
            // indexed colormap
            if (isH5) {
                long[] tmpdims = { h, w };
                dims = tmpdims;
            }
            else {
                long[] tmpdims = { w, h };
                dims = tmpdims;
            }
        }
        else {
            // true color image
            if (isH5) {
                // HDF5 true color image
                if (checkInterlacePixel.getSelection()) {
                    long[] tmpdims = { h, w, 3 };
                    dims = tmpdims;
                }
                else {
                    interlace = ScalarDS.INTERLACE_PLANE;
                    long[] tmpdims = { 3, h, w };
                    dims = tmpdims;
                }
            }
            else {
                // HDF4 true color image
                ncomp = 3;
                long[] tmpdims = { w, h };
                dims = tmpdims;
                if (checkInterlacePlane.getSelection()) {
                    interlace = ScalarDS.INTERLACE_PLANE;
                }
            }
        }

        try {
            Datatype datatype = fileFormat.createDatatype(tclass, tsize,
                    torder, tsign);
            dataset = fileFormat.createImage(name, pgroup, datatype, dims,
                    dims, null, -1, ncomp, interlace, null);
            dataset.init();
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            MessageBox error = new MessageBox(shell, SWT.ICON_ERROR);
            error.setText(shell.getText());
            error.setMessage(ex.getMessage());
            error.open();
            return null;
        }

        return dataset;
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
