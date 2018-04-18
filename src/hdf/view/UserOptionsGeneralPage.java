/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the files COPYING and Copyright.html. *
 * COPYING can be found at the root of the source code distribution tree.    *
 * Or, see https://support.hdfgroup.org/products/licenses.html               *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.view;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.Vector;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * UserOptionsGeneralPage.java - Configuration page for general application settings.
 */
public class UserOptionsGeneralPage extends UserOptionsDefaultPage {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserOptionsGeneralPage.class);

    private Text                  UGField, workField, maxMemberField, startMemberField;
    private Combo                 fontSizeChoice, fontTypeChoice, delimiterChoice, imageOriginChoice, indexBaseChoice;
    private Button                checkCurrentUserDir, checkAutoContrast, checkConvertEnum, checkShowValues, checkShowRegRefValues;
    private Button                currentDirButton, rwButton, helpButton;
    private Button                checkReadOnly, checkReadAll;

    private boolean               isFontChanged;

    private boolean               isUserGuideChanged;

    private boolean               isWorkDirChanged;

    private static String         fontname;

    public UserOptionsGeneralPage() {
        super("General Settings");
        isFontChanged = false;
        isUserGuideChanged = false;
        isWorkDirChanged = false;
    }
    /**
     * Notifies that the OK button of this page's container has been pressed.
     *
     * @return <code>false</code> to abort the container's OK processing and
     * <code>true</code> to allow the OK to happen
     */
    public boolean performOk() {
        ViewProperties store = (ViewProperties)getPreferenceStore();

        String UGPath = UGField.getText();
        if ((UGPath != null) && (UGPath.length() > 0)) {
            UGPath = UGPath.trim();
            isUserGuideChanged = !UGPath.equals(ViewProperties.getUsersGuide());
            ViewProperties.setUsersGuide(UGPath);
        }

        String workPath = workField.getText();
        if (checkCurrentUserDir.getSelection())
            workPath = "user.home";

        if ((workPath != null) && (workPath.length() > 0)) {
            workPath = workPath.trim();
            isWorkDirChanged = !workPath.equals(ViewProperties.getWorkDir());
            ViewProperties.setWorkDir(workPath);
        }

        // set font size and type
        try {
            String ftype = (String) fontTypeChoice.getItem(fontTypeChoice.getSelectionIndex());
            int fsize = Integer.parseInt((String) fontSizeChoice.getItem(fontSizeChoice.getSelectionIndex()));

            if (ViewProperties.getFontSize() != fsize) {
                ViewProperties.setFontSize(fsize);
                isFontChanged = true;
            }

            if (!ftype.equalsIgnoreCase(ViewProperties.getFontType())) {
                ViewProperties.setFontType(ftype);
                isFontChanged = true;
            }
        }
        catch (Exception ex) {
            isFontChanged = false;
        }

        // set data delimiter
        ViewProperties.setDataDelimiter((String) delimiterChoice.getItem(delimiterChoice.getSelectionIndex()));
        ViewProperties.setImageOrigin((String) imageOriginChoice.getItem(imageOriginChoice.getSelectionIndex()));

        if (indexBaseChoice.getSelectionIndex() == 0)
            ViewProperties.setIndexBase1(false);
        else
            ViewProperties.setIndexBase1(true);

        return true;
    }

    public boolean isFontChanged() {
        return isFontChanged;
    }

    public boolean isUserGuideChanged() {
        return isUserGuideChanged;
    }

    public boolean isWorkDirChanged() {
        return isWorkDirChanged;
    }

    /**
     * Loads all stored values in the <code>FieldEditor</code>s.
     */
    protected void load() {
        ViewProperties store = (ViewProperties)getPreferenceStore();

        try {
            curFont = new Font(
                    Display.getCurrent(),
                    ViewProperties.getFontType(),
                    ViewProperties.getFontSize(),
                    SWT.NORMAL);
        }
        catch (Exception ex) {
            curFont = null;
        }

        workDir = ViewProperties.getWorkDir();
        if (workDir == null)
            workDir = rootDir;

        workField.setText(workDir);

        if (workDir.equals(System.getProperty("user.home"))) {
            checkCurrentUserDir.setSelection(true);
            workField.setEnabled(false);
        }

        log.trace("UserOptionsGeneralPage: workDir={}", workDir);

        UGField.setText(ViewProperties.getUsersGuide());

        checkReadOnly.setSelection(ViewProperties.isReadOnly());

        rwButton.setSelection(!ViewProperties.isReadOnly());

        try {
            int selectionIndex = fontSizeChoice.indexOf(String.valueOf(ViewProperties.getFontSize()));
            fontSizeChoice.select(selectionIndex);
        }
        catch (Exception ex) {
            fontSizeChoice.select(0);
        }

        fontname = ViewProperties.getFontType();

        checkAutoContrast.setSelection(ViewProperties.isAutoContrast());

        checkShowValues.setSelection(ViewProperties.showImageValues());

        String[] imageOriginChoices = { ViewProperties.ORIGIN_UL, ViewProperties.ORIGIN_LL, ViewProperties.ORIGIN_UR,
                ViewProperties.ORIGIN_LR };
        imageOriginChoice.setItems(imageOriginChoices);

        try {
            int selectionIndex = imageOriginChoice.indexOf(ViewProperties.getImageOrigin());
            imageOriginChoice.select(selectionIndex);
        }
        catch (Exception ex) {
            imageOriginChoice.select(0);
        }

        //        helpButton.setImage(ViewProperties.getHelpIcon());

        if (ViewProperties.isIndexBase1())
            indexBaseChoice.select(1);
        else
            indexBaseChoice.select(0);

        String[] delimiterChoices = { ViewProperties.DELIMITER_TAB, ViewProperties.DELIMITER_COMMA,
                ViewProperties.DELIMITER_SPACE, ViewProperties.DELIMITER_COLON, ViewProperties.DELIMITER_SEMI_COLON };
        delimiterChoice.setItems(delimiterChoices);

        try {
            int selectionIndex = delimiterChoice.indexOf(ViewProperties.getDataDelimiter());
            delimiterChoice.select(selectionIndex);
        }
        catch (Exception ex) {
            delimiterChoice.select(0);
        }

        int nMax = ViewProperties.getMaxMembers();
        checkReadAll.setSelection((nMax<=0) || (nMax==Integer.MAX_VALUE));

        startMemberField.setText(String.valueOf(ViewProperties.getStartMembers()));

        maxMemberField.setText(String.valueOf(ViewProperties.getMaxMembers()));
    }

    /**
     * Creates and returns the SWT control for the customized body of this
     * preference page under the given parent composite.
     *
     * @param parent the parent composite
     * @return the new control
     */
    protected Control createContents(Composite parent) {
        shell = parent.getShell();
        ScrolledComposite scroller = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        scroller.setExpandHorizontal(true);
        scroller.setExpandVertical(true);

        Composite composite = new Composite(scroller, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));
        scroller.setContent(composite);

        org.eclipse.swt.widgets.Group workingDirectoryGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        workingDirectoryGroup.setLayout(new GridLayout(3, false));
        workingDirectoryGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        workingDirectoryGroup.setFont(curFont);
        workingDirectoryGroup.setText("Default Working Directory");

        checkCurrentUserDir = new Button(workingDirectoryGroup, SWT.CHECK);
        checkCurrentUserDir.setFont(curFont);
        checkCurrentUserDir.setText("\"Current Working Directory\" or");
        checkCurrentUserDir.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        checkCurrentUserDir.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean isCheckCurrentUserDirSelected = checkCurrentUserDir.getSelection();
                workField.setEnabled(!isCheckCurrentUserDirSelected);
                currentDirButton.setEnabled(!isCheckCurrentUserDirSelected);
            }
        });

        workField = new Text(workingDirectoryGroup, SWT.SINGLE | SWT.BORDER);
        workField.setFont(curFont);
        workField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        currentDirButton = new Button(workingDirectoryGroup, SWT.PUSH);
        currentDirButton.setFont(curFont);
        currentDirButton.setText("Browse...");
        currentDirButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        currentDirButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                final DirectoryDialog dChooser = new DirectoryDialog(shell);
                dChooser.setFilterPath(workDir);
                dChooser.setText("Select a Directory");

                String dir = dChooser.open();

                if(dir == null) return;

                workField.setText(dir);
            }
        });

        org.eclipse.swt.widgets.Group helpDocumentGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        helpDocumentGroup.setLayout(new GridLayout(3, false));
        helpDocumentGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        helpDocumentGroup.setFont(curFont);
        helpDocumentGroup.setText("Help Document");

        Label label = new Label(helpDocumentGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("User's Guide:  ");

        UGField = new Text(helpDocumentGroup, SWT.SINGLE | SWT.BORDER);
        UGField.setFont(curFont);
        UGField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        Button browseButton = new Button(helpDocumentGroup, SWT.PUSH);
        browseButton.setFont(curFont);
        browseButton.setText("Browse...");
        browseButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        browseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                final FileDialog fChooser = new FileDialog(shell, SWT.OPEN);
                fChooser.setFilterPath(rootDir);
                fChooser.setFilterExtensions(new String[] {"*.*"});
                fChooser.setFilterNames(new String[] {"All Files"});
                fChooser.setFilterIndex(0);

                if(fChooser.open() == null) {
                    return;
                }

                File chosenFile = new File(fChooser.getFilterPath() + File.separator + fChooser.getFileName());

                if(!chosenFile.exists()) {
                    // Give an error
                    return;
                }

                UGField.setText(chosenFile.getAbsolutePath());
            }
        });

        Composite fileOptionComposite = new Composite(composite, SWT.NONE);
        fileOptionComposite.setLayout(new GridLayout(13, true));
        fileOptionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        org.eclipse.swt.widgets.Group fileAccessModeGroup = new org.eclipse.swt.widgets.Group(fileOptionComposite, SWT.NONE);
        fileAccessModeGroup.setLayout(new GridLayout(2, true));
        fileAccessModeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        fileAccessModeGroup.setFont(curFont);
        fileAccessModeGroup.setText("Default File Access Mode");

        checkReadOnly = new Button(fileAccessModeGroup, SWT.RADIO);
        checkReadOnly.setFont(curFont);
        checkReadOnly.setText("Read Only");
        checkReadOnly.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));

        rwButton = new Button(fileAccessModeGroup, SWT.RADIO);
        rwButton.setFont(curFont);
        rwButton.setText("Read/Write");
        rwButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));

        org.eclipse.swt.widgets.Group textFontGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        textFontGroup.setLayout(new GridLayout(4, false));
        textFontGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        textFontGroup.setFont(curFont);
        textFontGroup.setText("Text Font");

        label = new Label(textFontGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Font Size: ");

        String[] fontSizeChoices = { "8", "10", "12", "14", "16", "18", "20", "22", "24", "26", "28", "30", "32", "34", "36", "48" };
        fontSizeChoice = new Combo(textFontGroup, SWT.SINGLE | SWT.READ_ONLY);
        fontSizeChoice.setFont(curFont);
        fontSizeChoice.setItems(fontSizeChoices);
        fontSizeChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        label = new Label(textFontGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Font Type: ");

        String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

        boolean isFontValid = false;
        if (fontNames != null) {
            for (int i = 0; i < fontNames.length; i++) {
                if (fontNames[i].equalsIgnoreCase(fontname)) {
                    isFontValid = true;
                }
            }
        }
        if (!isFontValid) {
            //fontname = (viewer).getFont().getFamily();
            //ViewProperties.setFontType(fontname);
        }

        fontTypeChoice = new Combo(textFontGroup, SWT.SINGLE | SWT.READ_ONLY);
        fontTypeChoice.setFont(curFont);
        fontTypeChoice.setItems(fontNames);
        fontTypeChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        try {
            int selectionIndex = fontTypeChoice.indexOf(fontname);
            fontTypeChoice.select(selectionIndex);
        }
        catch (Exception ex) {
            fontTypeChoice.select(0);
        }

        org.eclipse.swt.widgets.Group imageGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        imageGroup.setLayout(new GridLayout(5, false));
        imageGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        imageGroup.setFont(curFont);
        imageGroup.setText("Image");

        Button helpButton = new Button(imageGroup, SWT.PUSH);
        helpButton.setToolTipText("Help on Auto Contrast");
        helpButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                final String msg = "Auto Contrast does the following to compute a gain/bias \n"
                        + "that will stretch the pixels in the image to fit the pixel \n"
                        + "values of the graphics system. For example, it stretches unsigned\n"
                        + "short data to fit the full range of an unsigned short. Later \n"
                        + "code simply takes the high order byte and passes it to the graphics\n"
                        + "system (which expects 0-255). It uses some statistics on the pixels \n"
                        + "to prevent outliers from throwing off the gain/bias calculations much.\n\n"
                        + "To compute the gain/bias we... \n"
                        + "Find the mean and std. deviation of the pixels in the image \n" + "min = mean - 3 * std.dev. \n"
                        + "max = mean + 3 * std.dev. \n" + "small fudge factor because this tends to overshoot a bit \n"
                        + "Stretch to 0-USHRT_MAX \n" + "        gain = USHRT_MAX / (max-min) \n"
                        + "        bias = -min \n" + "\n" + "To apply the gain/bias to a pixel, use the formula \n"
                        + "data[i] = (data[i] + bias) * gain \n" + "\n"
                        // +
                        // "Finally, for auto-ranging the sliders for gain/bias, we do the following \n"
                        // + "gain_min = 0 \n"
                        // + "gain_max = gain * 3.0 \n"
                        // + "bias_min = -fabs(bias) * 3.0 \n"
                        // + "bias_max = fabs(bias) * 3.0 \n"
                        + "\n\n";

                MessageDialog.openInformation(shell, shell.getText(), msg);
            }
        });

        checkAutoContrast = new Button(imageGroup, SWT.CHECK);
        checkAutoContrast.setFont(curFont);
        checkAutoContrast.setText("Autogain Image Contrast");
        checkAutoContrast.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        checkShowValues = new Button(imageGroup, SWT.CHECK);
        checkShowValues.setFont(curFont);
        checkShowValues.setText("Show Values");
        checkShowValues.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        label = new Label(imageGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Image Origin: ");

        imageOriginChoice = new Combo(imageGroup, SWT.SINGLE | SWT.READ_ONLY);
        imageOriginChoice.setFont(curFont);
        imageOriginChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        org.eclipse.swt.widgets.Group dataGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        dataGroup.setLayout(new GridLayout(4, false));
        dataGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        dataGroup.setFont(curFont);
        dataGroup.setText("Data");

        helpButton = new Button(dataGroup, SWT.PUSH);
        helpButton.setToolTipText("Help on Convert Enum");
        helpButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                final String msg = "Convert enum data to strings. \n"
                        + "For example, a dataset of an enum type of (R=0, G=, B=2) \n"
                        + "has values of (0, 2, 2, 2, 1, 1). With conversion, the data values are \n"
                        + "shown as (R, B, B, B, G, G).\n\n\n";

                MessageDialog.openInformation(shell, shell.getText(), msg);
            }
        });

        checkConvertEnum = new Button(dataGroup, SWT.CHECK);
        checkConvertEnum.setFont(curFont);
        checkConvertEnum.setText("Convert Enum");
        checkConvertEnum.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));

        checkShowRegRefValues = new Button(dataGroup, SWT.CHECK);
        checkShowRegRefValues.setFont(curFont);
        checkShowRegRefValues.setText("Show RegRef Values");
        checkShowRegRefValues.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        // Add dummy label
        label = new Label(dataGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("");

        label = new Label(dataGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Index Base: ");

        String[] indexBaseChoices = { "0-based", "1-based" };
        indexBaseChoice = new Combo(dataGroup, SWT.SINGLE | SWT.READ_ONLY);
        indexBaseChoice.setFont(curFont);
        indexBaseChoice.setItems(indexBaseChoices);
        indexBaseChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        Label delimLabel = new Label(dataGroup, SWT.RIGHT);
        delimLabel.setFont(curFont);
        delimLabel.setText("Data Delimiter: ");
        delimLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

        delimiterChoice = new Combo(dataGroup, SWT.SINGLE | SWT.READ_ONLY);
        delimiterChoice.setFont(curFont);
        delimiterChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        org.eclipse.swt.widgets.Group objectsGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        objectsGroup.setLayout(new GridLayout(5, false));
        objectsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        objectsGroup.setFont(curFont);
        objectsGroup.setText("Objects to Open");

        checkReadAll = new Button(objectsGroup, SWT.CHECK);
        checkReadAll.setFont(curFont);
        checkReadAll.setText("Open All");
        checkReadAll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        checkReadAll.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                startMemberField.setEnabled(!checkReadAll.getSelection());
                maxMemberField.setEnabled(!checkReadAll.getSelection());
            }
        });

        label = new Label(objectsGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Start Member: ");

        startMemberField = new Text(objectsGroup, SWT.SINGLE | SWT.BORDER);
        startMemberField.setFont(curFont);
        startMemberField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        label = new Label(objectsGroup, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Member Count: ");

        maxMemberField = new Text(objectsGroup, SWT.SINGLE | SWT.BORDER);
        maxMemberField.setFont(curFont);
        maxMemberField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        startMemberField.setEnabled(!checkReadAll.getSelection());
        maxMemberField.setEnabled(!checkReadAll.getSelection());

        load();
        return scroller;
    }
}
