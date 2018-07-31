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

package hdf.view.TableView;

import java.lang.reflect.Constructor;
import java.util.BitSet;
import java.util.HashMap;

import hdf.object.Attribute;
import hdf.object.CompoundDS;
import hdf.object.DataFormat;
import hdf.object.FileFormat;
import hdf.object.HObject;
import hdf.object.ScalarDS;
import hdf.view.Tools;
import hdf.view.ViewProperties;
import hdf.view.DataView.DataViewManager;

/**
 * A simple Factory class which returns concrete instances of the default
 * TableView, based on whether the data object to be viewed is a scalar or
 * compound dataset or is an attribute.
 *
 * @author jhenderson
 * @version 1.0 4/18/2018
 */
public class DefaultTableViewFactory extends TableViewFactory {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultTableViewFactory.class);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public TableView getTableView(DataViewManager viewer, HashMap dataPropertiesMap) throws ClassNotFoundException {
        String dataViewName = null;
        Object[] initargs = { viewer, dataPropertiesMap };
        TableView theView = null;
        HObject dataObject = null;

        log.trace("getTableView(): start");

        /* Retrieve the data object to be displayed */
        if (dataPropertiesMap != null)
            dataObject = (HObject) dataPropertiesMap.get(ViewProperties.DATA_VIEW_KEY.OBJECT);

        if (dataObject == null)
            dataObject = viewer.getTreeView().getCurrentObject();

        if (dataObject == null) {
            log.debug("getTableView(): data object is null");
            log.trace("getTableView(): finish");
            return null;
        }

        /*
         * If the name of a specific TableView class to use has been passed in via the
         * data options map, retrieve its name now, otherwise use the default TableView
         * class.
         */
        dataViewName = (String) dataPropertiesMap.get(ViewProperties.DATA_VIEW_KEY.VIEW_NAME);
        if (dataViewName == null) {
            if (dataObject instanceof ScalarDS)
                dataViewName = ViewProperties.DEFAULT_SCALAR_DATASET_TABLEVIEW_NAME;
            else if (dataObject instanceof CompoundDS)
                dataViewName = ViewProperties.DEFAULT_COMPOUND_DATASET_TABLEVIEW_NAME;
            else if (dataObject instanceof Attribute) {
                if (((Attribute) dataObject).getDatatype().isCompound())
                    dataViewName = ViewProperties.DEFAULT_COMPOUND_ATTRIBUTE_TABLEVIEW_NAME;
                else
                    dataViewName = ViewProperties.DEFAULT_SCALAR_ATTRIBUTE_TABLEVIEW_NAME;
            }
            else
                dataViewName = null;
        }

        Class<?> theClass = null;
        try {
            log.trace("getTableView(): Class.forName({})", dataViewName);

            /* Attempt to load the class by the given name */
            theClass = Class.forName(dataViewName);
        }
        catch (Exception ex) {
            log.debug("getTableView(): unable to load default TableView class by name({})", dataViewName);
            theClass = null;
        }

        if (theClass == null) throw new ClassNotFoundException();

        /* Check to see if there is a bitmask to be applied to the data */
        BitSet bitmask = (BitSet) dataPropertiesMap.get(ViewProperties.DATA_VIEW_KEY.BITMASK);
        if (bitmask != null) {
            /*
             * Create a copy of the data object in order to apply the bitmask
             * non-destructively
             */
            HObject d_copy = null;
            Constructor<? extends HObject> constructor = null;
            Object[] paramObj = null;

            try {
                Class<?>[] paramClass = { FileFormat.class, String.class, String.class, long[].class };
                constructor = dataObject.getClass().getConstructor(paramClass);

                paramObj = new Object[] { dataObject.getFileFormat(), dataObject.getName(), dataObject.getPath(),
                        dataObject.getOID() };
            }
            catch (Exception ex) {
                constructor = null;
            }

            try {
                d_copy = constructor.newInstance(paramObj);
            }
            catch (Exception ex) {
                d_copy = null;
            }

            if (d_copy != null) {
                try {
                    ((DataFormat) d_copy).init();
                    log.trace("getTableView(): d_copy inited");

                    int rank = ((DataFormat) dataObject).getRank();
                    System.arraycopy(((DataFormat) dataObject).getDims(), 0, ((DataFormat) d_copy).getDims(), 0, rank);
                    System.arraycopy(((DataFormat) dataObject).getStartDims(), 0, ((DataFormat) d_copy).getStartDims(),0, rank);
                    System.arraycopy(((DataFormat) dataObject).getSelectedDims(), 0, ((DataFormat) d_copy).getSelectedDims(), 0, rank);
                    System.arraycopy(((DataFormat) dataObject).getStride(), 0, ((DataFormat) d_copy).getStride(), 0, rank);
                    System.arraycopy(((DataFormat) dataObject).getSelectedIndex(), 0, ((DataFormat) d_copy).getSelectedIndex(), 0, 3);
                }
                catch (Throwable ex) {
                    ex.printStackTrace();
                }

                dataPropertiesMap.put(ViewProperties.DATA_VIEW_KEY.OBJECT, d_copy);
            }
        }

        try {
            theView = (TableView) Tools.newInstance(theClass, initargs);

            log.trace("getTableView(): returning TableView instance {}", theView);
        }
        catch (Exception ex) {
            log.debug("getTableView(): Error instantiating class:", ex);
            theView = null;
        }

        log.trace("getTableView(): finish");

        return theView;
    }

}
