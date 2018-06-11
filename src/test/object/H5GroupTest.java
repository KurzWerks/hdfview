package test.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.structs.H5G_info_t;
import hdf.object.Attribute;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.h5.H5Datatype;
import hdf.object.h5.H5File;
import hdf.object.h5.H5Group;

/**
 * @author xcao
 *
 */
public class H5GroupTest {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5GroupTest.class);
    private static final H5File H5FILE = new H5File();
    private static final int NLOOPS = 5;
    private static final int TEST_VALUE_INT = Integer.MAX_VALUE;
    private static final float TEST_VALUE_FLOAT = Float.MAX_VALUE;
    private static final String TEST_VALUE_STR = "H5GroupTest";
    private static final String GNAME = H5TestFile.NAME_GROUP_ATTR;
    private static final String GNAME_SUB = H5TestFile.NAME_GROUP_SUB;

    private H5Datatype typeInt = null;
    private H5Datatype typeFloat = null;
    private H5Datatype typeStr = null;
    private H5File testFile = null;
    private H5Group testGroup = null;

    @BeforeClass
    public static void createFile() throws Exception {
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                System.out.println("H5GroupTest BeforeClass: Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            H5TestFile.createTestFile(null);
        }
        catch (final Exception ex) {
            System.out.println("*** Unable to create HDF5 test file. " + ex);
            System.exit(-1);
        }
    }

    @AfterClass
    public static void checkIDs() throws Exception {
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                System.out.println("H5GroupTest AfterClass: Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @SuppressWarnings("deprecation")
    @Before
    public void openFiles() throws Exception {
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                log.debug("Before: Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        testFile = (H5File) H5FILE.open(H5TestFile.NAME_FILE_H5, FileFormat.WRITE);
        assertNotNull(testFile);

        typeInt = new H5Datatype(Datatype.CLASS_INTEGER, H5TestFile.DATATYPE_SIZE, -1, -1);
        typeFloat = new H5Datatype(Datatype.CLASS_FLOAT, H5TestFile.DATATYPE_SIZE, -1, -1);
        typeStr = new H5Datatype(Datatype.CLASS_STRING, H5TestFile.STR_LEN, -1, -1);

        testGroup = (H5Group) testFile.get(GNAME);
        assertNotNull(testGroup);
    }

    @After
    public void removeFiles() throws Exception {
        if (testFile != null) {
            try {
                testFile.close();
            }
            catch (final Exception ex) {
            }
            testFile = null;
        }
        try {
            int openID = H5.getOpenIDCount();
            if (openID > 0)
                log.debug("After: Number of IDs still open: " + openID);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Test method for {@link hdf.object.h5.H5Group#setName(java.lang.String)}.
     * <p>
     * What to test:
     * <ul>
     * <li>Test for boundary conditions
     * <ul>
     * <li>Set name to null
     * </ul>
     * <li>Test for failure
     * <ul>
     * <li>Set a name that already exists in file.
     * </ul>
     * <li>Test for general functionality
     * <ul>
     * <li>change the dataset name
     * <li>close/re-open the file
     * <li>get the dataset with the new name
     * <li>failure test: get the dataset with the original name
     * <li>set the name back to the original name
     * </ul>
     * </ul>
     */
    @Test
    public void testSetName() {
        log.debug("testSetName");
        final String newName = "tmpName";

        // test set name to null
        H5.H5error_off();
        try {
            testGroup.setName(null);
        }
        catch (final Exception ex) {
            ; // Expected - intentional
        }

        // set to an existing name
        try {
            testGroup.setName(H5TestFile.NAME_DATASET_FLOAT);
        }
        catch (final Exception ex) {
            ; // Expected - intentional
        }
        H5.H5error_on();

        try {
            testGroup.setName(newName);
        }
        catch (final Exception ex) {
            fail("setName() failed. " + ex);
        }

        // close the file and reopen it
        try {
            testFile.close();
            testFile.open();
            testGroup = (H5Group) testFile.get(newName);
        }
        catch (final Exception ex) {
            fail("setName() failed. " + ex);
        }

        // test the old name
        H5Group tmpDset = null;
        try {
            H5.H5error_off();
            tmpDset = (H5Group) testFile.get(GNAME);
            H5.H5error_on();
        }
        catch (final Exception ex) {
            fail("setName() get(oldname) failed. " + ex);
        }
        assertNull("The dataset should be null because it has been renamed", tmpDset);

        // set back the original name
        try {
            testGroup.setName(GNAME);
        }
        catch (final Exception ex) {
            fail("setName() failed. " + ex);
        }

        // make sure the dataset is OK
        try {
            testGroup = (H5Group) testFile.get(GNAME);
        }
        catch (final Exception ex) {
            fail("setName() failed. " + ex);
        }
        assertNotNull(testGroup);
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.h5.H5Group#setPath(java.lang.String)}.
     */
    @Test
    public void testSetPath() {
        log.debug("testSetPath");
        final String newPath = "tmpName";

        try {
            H5.H5error_off();
            testGroup.setPath(newPath);
            H5.H5error_on();
        }
        catch (final Exception ex) {
            fail("setPath() failed. " + ex);
        }
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.h5.H5Group#open()}.
     * <p>
     * What to test:
     * <ul>
     * <li>open a group identifier
     * <li>Check if gid is valid
     * <li>Close the group
     * <li>Repeat all above
     * </ul>
     */
    @Test
    public void testOpen() {
        log.debug("testOpen");
        long gid = -1;

        for (int loop = 0; loop < NLOOPS; loop++) {
            gid = -1;
            try {
                gid = testGroup.open();
            }
            catch (final Exception ex) {
                fail("open() failed. " + ex);
            }

            assertTrue(gid > 0);

            testGroup.close(gid);
        }
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.h5.H5Group#close(int)}.
     * <p>
     * What to test:
     * <ul>
     * <li>open a group identifier
     * <li>Check if gid is valid
     * <li>Close the group
     * <li>Repeat all above
     * </ul>
     */
    @Test
    public void testClose() {
        log.debug("testClose");
        testOpen();
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.h5.H5Group#clear()}.
     * <p>
     * What to test:
     * <ul>
     * <li>Read attributes from file
     * <li>clear the group
     * <li>make sure that the attribute list is empty
     * </ul>
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testClear() {
        log.debug("testClear");
        Vector attrs = null;
        try {
            attrs = (Vector) testGroup.getMetadata();
        }
        catch (final Exception ex) {
            fail("clear() failed. " + ex);
        }
        assertTrue(attrs.size() > 0);

        // clear up the dataset
        testGroup.clear();

        // attribute is empty
        try {
            attrs = (Vector) testGroup.getMetadata();
        }
        catch (final Exception ex) {
            fail("clear() failed. " + ex);
        }
        assertTrue(attrs.size() <= 0);
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for
     * {@link hdf.object.h5.H5Group#H5Group(hdf.object.FileFormat, java.lang.String, java.lang.String, hdf.object.Group)}
     * .
     * <p>
     * What to test:
     * <ul>
     * <li>Construct an H5Group object that exits in file
     * <ul>
     * <li>new H5Group (file, null, fullpath, pgroup)
     * <li>new H5Group (file, fullname, null, pgroup)
     * <li>new H5Group (file, name, path, pgroup)
     * </ul>
     * <li>Construct an H5Group object that does not exist in file
     * </ul>
     */
    @Test
    public void testH5GroupFileFormatStringStringGroup() {
        log.debug("testH5GroupFileFormatStringStringGroup");
        Group pgroup = null;
        final String[] names = { null, GNAME_SUB, GNAME_SUB.substring(4) };
        final String[] paths = { GNAME_SUB, null, H5TestFile.NAME_GROUP };

        final H5File file = (H5File) testGroup.getFileFormat();
        assertNotNull(file);

        try {
            pgroup = (Group) testFile.get(H5TestFile.NAME_GROUP);
        }
        catch (final Exception ex) {
            fail("testFile.get() failed. " + ex);
        }
        assertNotNull(pgroup);

        for (int idx = 0; idx < names.length; idx++) {
            final H5Group grp = new H5Group(file, names[idx], paths[idx], pgroup);
            final long gid = grp.open();
            assertTrue(gid > 0);
            grp.close(gid);
        }

        H5.H5error_off();
        final H5Group grp = new H5Group(file, "NO_SUCH_DATASET", "NO_SUCH_PATH", pgroup);
        final long gid = grp.open();
        H5.H5error_on();
        assertTrue(gid <= 0);
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for
     * {@link hdf.object.h5.H5Group#H5Group(hdf.object.FileFormat, java.lang.String, java.lang.String, hdf.object.Group, long[])}
     * .
     * <p>
     * What to test:
     * <ul>
     * <li>Construct an H5Group object that exits in file
     * <ul>
     * <li>new H5Group (file, null, fullpath, pgroup, oid)
     * <li>new H5Group (file, fullname, null, pgroup, oid)
     * <li>new H5Group (file, name, path, pgroup, oid)
     * </ul>
     * <li>Construct an H5Group object that does not exist in file
     * </ul>
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testH5GroupFileFormatStringStringGroupLongArray() {
        log.debug("testH5GroupFileFormatStringStringGroupLongArray");
        Group pgroup = null;
        final String[] names = { null, GNAME_SUB, GNAME_SUB.substring(4) };
        final String[] paths = { GNAME_SUB, null, H5TestFile.NAME_GROUP };

        final H5File file = (H5File) testGroup.getFileFormat();
        assertNotNull(file);

        try {
            pgroup = (Group) testFile.get(H5TestFile.NAME_GROUP);
        }
        catch (final Exception ex) {
            fail("testFile.get() failed. " + ex);
        }
        assertNotNull(pgroup);

        long[] oid = null;
        for (int idx = 0; idx < names.length; idx++) {
            try {
                final byte[] ref_buf = H5.H5Rcreate(file.getFID(), GNAME_SUB, HDF5Constants.H5R_OBJECT, -1);
                final long l = HDFNativeData.byteToLong(ref_buf, 0);
                oid = new long[1];
                oid[0] = l; // save the object ID
            }
            catch (final HDF5Exception ex) {
                fail("H5.H5Rcreate() failed. " + ex);
            }

            assertNotNull(oid);

            final H5Group grp = new H5Group(file, names[idx], paths[idx], pgroup, oid);
            final long gid = grp.open();
            assertTrue(gid > 0);
            grp.close(gid);
        }

        // test a non-existing dataset
        H5.H5error_off();
        final H5Group grp = new H5Group(file, "NO_SUCH_DATASET", "NO_SUCH_PATH", pgroup, null);
        final long gid = grp.open();
        H5.H5error_on();
        assertTrue(gid <= 0);
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.h5.H5Group#getMetadata()}.
     * <p>
     * Cases tested:
     * <ul>
     * <li>Get all the attributes
     * <li>Check the content of the attributes
     * </ul>
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testGetMetadata() {
        log.debug("testGetMetadata");
        Vector attrs = null;

        try {
            attrs = (Vector) testGroup.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }
        assertNotNull(attrs);
        assertTrue(attrs.size() > 0);

        final int n = attrs.size();
        for (int i = 0; i < n; i++) {
            final Attribute attr = (Attribute) attrs.get(i);
            final H5Datatype dtype = (H5Datatype) attr.getDatatype();
            if (dtype.isString()) {
                try {
                    assertTrue(H5TestFile.ATTRIBUTE_STR.getName().equals(attr.getName()));
                    assertTrue(
                            ((String[]) H5TestFile.ATTRIBUTE_STR.getData())[0].equals(((String[]) attr.getData())[0]));
                } catch (Exception ex) {
                    log.trace("testGetMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                } catch (OutOfMemoryError e) {
                    log.trace("testGetMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
            else if (dtype.getDatatypeClass() == Datatype.CLASS_INTEGER) {
                try {
                    assertTrue(H5TestFile.ATTRIBUTE_INT_ARRAY.getName().equals(attr.getName()));
                    final int[] expected = (int[]) H5TestFile.ATTRIBUTE_INT_ARRAY.getData();
                    assertNotNull(expected);
                    final int[] ints = (int[]) attr.getData();
                    assertNotNull(ints);
                    for (int j = 0; j < expected.length; j++) {
                        assertEquals(expected[j], ints[j]);
                    }
                } catch (Exception ex) {
                    log.trace("testGetMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                } catch (OutOfMemoryError e) {
                    log.trace("testGetMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
        } // for (int i=0; i<n; i++) {
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.h5.H5Group#writeMetadata(java.lang.Object)}.
     * <p>
     * What to test:
     * <ul>
     * <li>Update the value of an existing attribute
     * <li>Attach a new attribute
     * <li>Close and re-open file to check if the change is made in file
     * <li>Restore to the orginal state
     * </ul>
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testWriteMetadata() {
        log.debug("testWriteMetadata");
        Vector attrs = null;
        Attribute attr = null;

        try {
            attrs = (Vector) testGroup.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }
        assertNotNull(attrs);
        assertTrue(attrs.size() > 0);

        // update existing attribute
        int n = attrs.size();
        for (int i = 0; i < n; i++) {
            attr = (Attribute) attrs.get(i);
            final H5Datatype dtype = (H5Datatype) attr.getDatatype();
            if (dtype.isString()) {
                try {
                    final String[] strs = (String[]) attr.getData();
                    strs[0] = TEST_VALUE_STR;
                } catch (Exception ex) {
                    log.trace("testWriteMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                } catch (OutOfMemoryError e) {
                    log.trace("testWriteMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
            else if (dtype.getDatatypeClass() == Datatype.CLASS_INTEGER) {
                try {
                    final int[] ints = (int[]) attr.getData();
                    assertNotNull(ints);
                    for (int j = 0; j < ints.length; j++) {
                        ints[j] = TEST_VALUE_INT;
                    }
                } catch (Exception ex) {
                    log.trace("testWriteMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                } catch (OutOfMemoryError e) {
                    log.trace("testWriteMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
            try {
                attr.write();
            }
            catch (final Exception ex) {
                fail("writeMetadata() failed. " + ex);
            }
        } // for (int i=0; i<n; i++) {

        // attache a new attribute
        attr = new Attribute(testGroup, "float attribute", typeFloat, new long[] { 1 },
                new float[] { TEST_VALUE_FLOAT });
        try {
            attr.write();
        }
        catch (final Exception ex) {
            fail("writeMetadata() failed. " + ex);
        }

        // close the file and reopen it
        try {
            testGroup.clear();
            testFile.close();
            testFile.open();
            testGroup = (H5Group) testFile.get(GNAME);
        }
        catch (final Exception ex) {
            fail("write() failed. " + ex);
        }

        // check the change in file
        try {
            attrs = (Vector) testGroup.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }
        assertNotNull(attrs);
        assertTrue(attrs.size() > 0);

        n = attrs.size();
        Attribute newAttr = null;
        for (int i = 0; i < n; i++) {
            attr = (Attribute) attrs.get(i);
            final H5Datatype dtype = (H5Datatype) attr.getDatatype();
            if (dtype.isString()) {
                try {
                    assertTrue(H5TestFile.ATTRIBUTE_STR.getName().equals(attr.getName()));
                    assertTrue(TEST_VALUE_STR.equals(((String[]) attr.getData())[0]));
                } catch (Exception ex) {
                    log.trace("testWriteMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                } catch (OutOfMemoryError e) {
                    log.trace("testWriteMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
            else if (dtype.getDatatypeClass() == Datatype.CLASS_INTEGER) {
                try {
                    assertTrue(H5TestFile.ATTRIBUTE_INT_ARRAY.getName().equals(attr.getName()));
                    final int[] ints = (int[]) attr.getData();
                    assertNotNull(ints);
                    for (int j = 0; j < ints.length; j++) {
                        assertEquals(TEST_VALUE_INT, ints[j]);
                    }
                } catch (Exception ex) {
                    log.trace("testWriteMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                } catch (OutOfMemoryError e) {
                    log.trace("testWriteMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
            else if (dtype.getDatatypeClass() == Datatype.CLASS_FLOAT) {
                try {
                    newAttr = attr;
                    final float[] floats = (float[]) attr.getData();
                    assertEquals(TEST_VALUE_FLOAT, floats[0], Float.MIN_VALUE);
                } catch (Exception ex) {
                    log.trace("testWriteMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                } catch (OutOfMemoryError e) {
                    log.trace("testWriteMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
        } // for (int i=0; i<n; i++) {

        // remove the new attribute
        try {
            testGroup.removeMetadata(newAttr);
        }
        catch (final Exception ex) {
            fail("removeMetadata() failed. " + ex);
        }

        // set the value to original
        n = attrs.size();
        for (int i = 0; i < n; i++) {
            attr = (Attribute) attrs.get(i);
            final H5Datatype dtype = (H5Datatype) attr.getDatatype();
            if (dtype.isString()) {
                try {
                    final String[] strs = (String[]) attr.getData();
                    strs[0] = ((String[]) H5TestFile.ATTRIBUTE_STR.getData())[0];
                } catch (Exception ex) {
                    log.trace("testWriteMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                } catch (OutOfMemoryError e) {
                    log.trace("testWriteMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
            else if (dtype.getDatatypeClass() == Datatype.CLASS_INTEGER) {
                try {
                    final int[] ints = (int[]) attr.getData();
                    assertNotNull(ints);
                    for (int j = 0; j < ints.length; j++) {
                        final int[] expected = (int[]) H5TestFile.ATTRIBUTE_INT_ARRAY.getData();
                        ints[j] = expected[j];
                    }
                } catch (Exception ex) {
                    log.trace("testWriteMetadata(): getData() failure:", ex);
                    fail("getData() failure " + ex);
                } catch (OutOfMemoryError e) {
                    log.trace("testWriteMetadata(): Out of memory");
                    fail("Out of memory");
                }
            }
            try {
                attr.write();
            }
            catch (final Exception ex) {
                fail("writeMetadata() failed. " + ex);
            }
        } // for (int i=0; i<n; i++) {
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.h5.H5Group#removeMetadata(java.lang.Object)}.
     * <p>
     * What to test:
     * <ul>
     * <li>Remove all existing attributes
     * <li>Close and reopen file to check if all attribute are removed from file
     * <li>Restore to the orginal state
     * </ul>
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testRemoveMetadata() {
        log.debug("testRemoveMetadata");
        Vector attrs = null;
        try {
            attrs = (Vector) testGroup.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }
        assertNotNull(attrs);
        assertTrue(attrs.size() > 0);

        // remove all attributes
        final int n = attrs.size();
        final Object[] arrayAttr = attrs.toArray();
        for (int i = 0; i < n; i++) {
            try {
                testGroup.removeMetadata(arrayAttr[i]);
            }
            catch (final Exception ex) {
                fail("removeMetadata() failed. " + ex);
            }
        }

        // close the file and reopen it
        try {
            testGroup.clear();
            testFile.close();
            testFile.open();
            testGroup = (H5Group) testFile.get(GNAME);
        }
        catch (final Exception ex) {
            fail("write() failed. " + ex);
        }
        attrs = null;

        try {
            attrs = (Vector) testGroup.getMetadata();
        }
        catch (final Exception ex) {
            fail("getMetadata() failed. " + ex);
        }
        assertNotNull(attrs);
        assertFalse(attrs.size() > 0);

        // restore to the original
        try {
            H5TestFile.ATTRIBUTE_STR.setParentObject(testGroup);
            H5TestFile.ATTRIBUTE_INT_ARRAY.setParentObject(testGroup);
            H5TestFile.ATTRIBUTE_STR.write();
            H5TestFile.ATTRIBUTE_INT_ARRAY.write();
        }
        catch (final Exception ex) {
            fail("writeMetadata() failed. " + ex);
        }
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.h5.H5Group#create(java.lang.String, hdf.object.Group)} .
     * <p>
     * What to test:
     * <ul>
     * <li>Create a new group
     * <li>Close and reopen the file
     * <li>Check the new group
     * <li>Restore to the orginal file (remove the new group)
     * </ul>
     */
    @Test
    public void testCreate() {
        log.debug("testCreate");
        Group grp = null;
        final String nameNew = "/tmpH5Group";
        try {
            final Group rootGrp = (Group) testFile.get("/");
            grp = H5Group.create(nameNew, rootGrp);
        }
        catch (final Exception ex) {
            fail("H5Group.create failed. " + ex);
        }
        assertNotNull(grp);

        try {
            testFile.close();
            testFile.open();
        }
        catch (final Exception ex) {
            fail("testFile.get() failed. " + ex);
        }

        try {
            grp = (Group) testFile.get(nameNew);
        }
        catch (final Exception ex) {
            fail("testFile.get() failed. " + ex);
        }
        assertNotNull(grp);

        try {
            testFile.delete(grp); // delete the new datast
        }
        catch (final Exception ex) {
            fail("testFile.delete() failed. " + ex);
        }

        try {
            testFile.close();
            testFile.open();
        }
        catch (final Exception ex) {
            fail("testFile.get() failed. " + ex);
        }

        grp = null;
        try {
            H5.H5error_off();
            grp = (Group) testFile.get(nameNew);
            H5.H5error_on();
        }
        catch (final Exception ex) {
            fail("testFile.get(deleted_newname) failed. " + ex);
        }
        assertNull(grp);
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.h5.H5Group#create(java.lang.String, hdf.object.Group, int)} .
     * <p>
     * What to test:
     * <ul>
     * <li>Sets group creation property list identifier
     * <li>Sets link storage and creation order
     * <li>Check that group is not created when the order of group property list is incorrect.
     * <li>Create a new group
     * <li>Create subgroups
     * <li>Close and reopen the file
     * <li>Check the new group and subgroup
     * <li>Check name of ith link in group by creation order and storage type
     * <li>Restore to the original file (remove the new group)
     * </ul>
     */
    @Test
    public void testCreateWithGroupplist() {
        log.debug("testCreateWithGroupplist");
        Group grp = null;
        final String nameNew = "/Group1";
        long gcpl = -1;
        long gid = -1;
        H5G_info_t ginfo;
        Group grp2 = null, grp3 = null;

        try {
            gcpl = H5.H5Pcreate(HDF5Constants.H5P_GROUP_CREATE);
            if (gcpl >= 0) {
                H5.H5Pset_link_creation_order(gcpl, HDF5Constants.H5P_CRT_ORDER_TRACKED
                        + HDF5Constants.H5P_CRT_ORDER_INDEXED); // Set link creation order.
                H5.H5Pset_link_phase_change(gcpl, 3, 2); // Set link storage.
            }
        }
        catch (final Exception ex) {
            fail("H5.H5Pcreate() failed. " + ex);
        }

        H5.H5error_off();
        try {
            final Group rootGrp = (Group) testFile.get("/");
            grp = H5Group.create(nameNew, rootGrp, gcpl);
        }
        catch (final Exception ex) {
            ; // Expected -intentional as the order of gplist is invalid.
        }
        H5.H5error_on();
        assertNull(grp);

        try {
            final Group rootGrp = (Group) testFile.get("/");
            grp = H5Group.create(nameNew, rootGrp, HDF5Constants.H5P_DEFAULT, gcpl);
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            fail("H5Group.create failed. " + ex);
        }
        assertNotNull(grp);

        try {
            grp2 = H5Group.create("G5", grp); // create subgroups
            grp3 = H5Group.create("G3", grp);
        }
        catch (final Exception ex) {
            fail("H5Group.create failed. " + ex);
        }
        assertNotNull(grp2);
        assertNotNull(grp3);

        H5.H5error_off();
        try {
            H5.H5Pclose(gcpl);
        }
        catch (final Exception ex) {
        }
        H5.H5error_on();

        try {
            testFile.close(); // Close and reopen file.
            testFile.open();
        }
        catch (final Exception ex) {
            fail("testFile.close() failed. " + ex);
        }
        grp = null;
        try {
            grp = (Group) testFile.get(nameNew);
        }
        catch (final Exception ex) {
            fail("testFile.get() failed. " + ex);
        }
        assertNotNull(grp);

        try {
            gid = grp.open();
        }
        catch (final Exception ex) {
            fail("grp.open() failed. " + ex);
        }
        assertTrue(gid > 0);

        try {
            ginfo = H5.H5Gget_info(gid); // Get group info.
            String name = H5.H5Lget_name_by_idx(gid, ".", HDF5Constants.H5_INDEX_CRT_ORDER, HDF5Constants.H5_ITER_INC,
                    1, HDF5Constants.H5P_DEFAULT); // Get name of ith link.
            assertEquals("G3", name);
            assertEquals(HDF5Constants.H5G_STORAGE_TYPE_COMPACT, ginfo.storage_type);
        }
        catch (final Exception ex) {
            fail("H5.H5Lget_name_by_idx() failed. " + ex);
        }

        grp.close(gid);

        try {
            testFile.delete(grp); // delete the new group
        }
        catch (final Exception ex) {
            fail("testFile.delete() failed. " + ex);
        }

        try {
            testFile.close(); // Close and reopen file.
            testFile.open();
        }
        catch (final Exception ex) {
            fail("testFile.get() failed. " + ex);
        }

        grp = null;
        try {
            H5.H5error_off();
            grp = (Group) testFile.get(nameNew);
            H5.H5error_on();
        }
        catch (final Exception ex) {
            fail("testFile.get(deleted_newgroup) failed. " + ex);
        }
        assertNull(grp);

        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

    /**
     * Test method for {@link hdf.object.h5.H5Group} IsSerializable.
     */
    @Test
    public void testIsSerializable() {
        log.debug("testIsSerializable");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(out);
            oos.writeObject(testGroup);
            oos.close();
        }
        catch (IOException err) {
            err.printStackTrace();
            fail("ObjectOutputStream failed: " + err);
        }
        assertTrue(out.toByteArray().length > 0);
    }

    /**
     * Test method for {@link hdf.object.h5.H5ScalarDS} SerializeToDisk.
     * <p>
     * What to test:
     * <ul>
     * <li>serialize a group identifier
     * <li>deserialize a group identifier
     * <li>open a group identifier
     * <li>Check if gid is valid
     * <li>Close the group
     * </ul>
     */
    @Test
    public void testSerializeToDisk() {
        log.debug("testSerializeToDisk");
        try {
            FileOutputStream fos = new FileOutputStream("temph5grp.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(testGroup);
            oos.close();
        }
        catch (Exception ex) {
            fail("Exception thrown during test: " + ex.toString());
        }

        H5Group test = null;
        try {
            FileInputStream fis = new FileInputStream("temph5grp.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            test = (hdf.object.h5.H5Group) ois.readObject();
            ois.close();

            // Clean up the file
            new File("temph5grp.ser").delete();
        }
        catch (Exception ex) {
            fail("Exception thrown during test: " + ex.toString());
        }

        long gid = -1;

        for (int loop = 0; loop < NLOOPS; loop++) {
            gid = -1;
            try {
                gid = test.open();
            }
            catch (final Exception ex) {
                fail("open() failed. " + ex);
            }

            assertTrue(gid > 0);

            test.close(gid);
        }
        long nObjs = 0;
        try {
            nObjs = H5.H5Fget_obj_count(testFile.getFID(), HDF5Constants.H5F_OBJ_ALL);
        }
        catch (final Exception ex) {
            fail("H5.H5Fget_obj_count() failed. " + ex);
        }
        assertEquals(1, nObjs); // file id should be the only one left open
    }

}
