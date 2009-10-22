/**
 *
 *  SQLRecordset Disconnected Recordset functionality
 *  and client-side cursor engine.
 *
 *  Copyright (C) 2002  Robin Rawson-Tetley
 *
 *  www.rawsontetley.org
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *
 *  <robin@rawsontetley.org>
 *
 *
 *  Change Log:
 *
 *        $Log: SQLRecordset.java,v $
 *        Revision 1.15  2006/03/11 15:02:16  bobintetley
 *        Fixes for DB copying
 *
 *        Revision 1.14  2006/03/11 10:01:12  bobintetley
 *        Database copying fix
 *
 *        Revision 1.13  2006/03/04 21:20:29  bobintetley
 *        Database copying code
 *
 *        Revision 1.12  2006/03/04 10:15:07  bobintetley
 *        PostgreSQL and HSQLDB scripts and new startup menu
 *
 *        Revision 1.11  2006/03/03 14:17:32  bobintetley
 *        UI uses new typed business layer correctly now. Version bumped to 2.0.0,
 *        removed apostrophes around tokens in localised strings to fix MessageFormat
 *        bug. Added Postgres and HSQLDB drivers
 *
 *        Revision 1.10  2006/03/02 14:08:16  bobintetley
 *        New typed bo API
 *
 *        Revision 1.9  2006/03/01 16:50:11  bobintetley
 *        Typed database handling and Animal/Adoption done
 *
 *        Revision 1.8  2005/01/24 16:19:56  bobintetley
 *        Bug fix to enable case insensitivity for custom reports again (broke
 *        internal functions), and for correct decimal place rounding in SUM/PCT
 *
 *        Revision 1.7  2005/01/21 08:29:28  bobintetley
 *        Defect list cleared
 *
 *        Revision 1.6  2004/12/15 16:49:45  bobintetley
 *        Data layer optimisations
 *
 *        Revision 1.4  2004/12/08 08:39:44  bobintetley
 *        Editing diary task allows viewing of the notes after creation, fix to bug
 *        that meant horizontal scrollbars appeared on the ownervet box, good with
 *        cats/kids/dogs/housetrained are now tri-state, cloning an animal now
 *        clones it's movements as well
 *
 *        Revision 1.3  2003/08/22 10:00:00  bobintetley
 *
 *        Fix to allow Turkish to work.
 *
 *        Revision 1.2  2003/08/05 10:32:01  bobintetley
 *        Bug fix to 1.20 update path (bad apostrophes). Altered how licence
 *        and credits are displayed.
 *
 *        Revision 1.1.1.1  2003/06/03 06:54:28  bobintetley
 *        Initial
 *
 *
 *        Revision 1.4  2003/05/01 13:32:16 robin
 *        Fixed bug where setting absolute position
 *        doesn't update BOF/EOF flags
 *        MovePrevious has never worked properly
 *
 *        Revision 1.3  2003/03/08 12:25:36 robin
 *        Improved memory handling
 *
 *        Revision 1.2  2003/01/03 09:25:49  robinrt
 *        Improved error handling
 *
 *        Revision 1.1  2002/09/24 09:50:48  robinrt
 *        Memory improvements on SQLRowData to only use
 *        as much as necessary. Also fixes when building
 *        SQL to not use concatenated strings (wasteful
 *        of memory again).
 *
 *        Revision 1.0  2002/05/11 17:18:03  robinrt
 *        Initial Release
 */
package net.sourceforge.sheltermanager.cursorengine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

import java.text.SimpleDateFormat;

import java.util.Hashtable;
import java.util.Vector;


/**
 * This class contains all the actual cursor routines and is used to manipulate
 * the data and write SQL to update it.
 *
 * @see net.sourceforge.sheltermanager.cursorengine.DBConnection
 * @see net.sourceforge.sheltermanager.cursorengine.SQLRowData
 * @see net.sourceforge.sheltermanager.cursorengine.SQLFieldDescriptor
 * @author Robin Rawson-Tetley
 * @version 3.0
 */
public class SQLRecordset {
    private boolean mEOF = false;
    private boolean mBOF = false;
    private int mCurrentRecord = 0;
    private Vector mtheRows = null;
    private Vector mtheFields = null;
    private Hashtable mFieldIndexes = null;
    private byte[] mFieldTypes = null;
    private int mNoFields = 0;
    private int mNoRows = 0;
    private String mTableName = "";

    public void openRecordset(String SQLStatement, String BaseTableName)
        throws Exception {
        openRecordset(DBConnection.con, SQLStatement, BaseTableName);
    }

    /**
     * Supply some SQL with the base table you want to update and the recordset
     * will fill itself with data, which you can then modify and submit updates
     * for.
     *
     * @param SQLStatement
     *            The SQL you would like to execute
     * @param BaseTableName
     *            The name of the table this recordset updates.
     */
    public void openRecordset(Connection c, String SQLStatement,
        String BaseTableName) throws Exception {
        int i;

        // Remember the table
        mTableName = BaseTableName;

        // Open a forward-only resultset using JDBC
        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = DBConnection.getStatement(c);
            rs = DBConnection.openResultset(c, stmt, SQLStatement);
        } catch (Exception e) {
            // Throw error to parent caller
            throw e;
        }

        // Now, scroll through this resultset to build a local, in
        // memory copy of the data:
        try {
            // Field header information
            mNoFields = rs.getMetaData().getColumnCount();
            mtheFields = new Vector(mNoFields + 1);
            mtheFields.add(0, new Object());
            mFieldIndexes = new Hashtable(mNoFields + 1);

            SQLFieldDescriptor fd = null;
            i = 1;
            mFieldTypes = new byte[mNoFields + 1];

            while (i <= mNoFields) {
                fd = new SQLFieldDescriptor();
                fd.name = rs.getMetaData().getColumnName(i);

                // Work out what field type to use based on the JDBC types
                // returned
                switch (rs.getMetaData().getColumnType(i)) {
                case Types.DATE:
                    fd.type = SQLFieldDescriptor.SQLFIELDTYPE_DATE;

                    break;

                case Types.TIMESTAMP:
                    fd.type = SQLFieldDescriptor.SQLFIELDTYPE_DATE;

                    break;

                case Types.LONGVARCHAR:
                    fd.type = SQLFieldDescriptor.SQLFIELDTYPE_STRING;

                    break;

                case Types.CHAR:
                    fd.type = SQLFieldDescriptor.SQLFIELDTYPE_STRING;

                    break;

                case Types.VARCHAR:
                    fd.type = SQLFieldDescriptor.SQLFIELDTYPE_STRING;

                    break;

                case Types.DECIMAL:
                    fd.type = SQLFieldDescriptor.SQLFIELDTYPE_FLOAT;

                    break;

                case Types.FLOAT:
                    fd.type = SQLFieldDescriptor.SQLFIELDTYPE_FLOAT;

                    break;

                case Types.DOUBLE:
                    fd.type = SQLFieldDescriptor.SQLFIELDTYPE_FLOAT;

                    break;

                case Types.REAL:
                    fd.type = SQLFieldDescriptor.SQLFIELDTYPE_FLOAT;

                    break;

                default:
                    fd.type = SQLFieldDescriptor.SQLFIELDTYPE_INTEGER;

                    break;
                }

                mtheFields.add(i, fd);
                mFieldIndexes.put(fd.name.toLowerCase(), new Integer(i));
                mFieldTypes[i] = fd.type;

                i++;
            }

            // Initialise row data
            if (!rs.last()) {
                mtheRows = new Vector(rs.getRow() + 1);
                mtheRows.add(0, new Object());
            } else {
                mtheRows = new Vector();
                mtheRows.add(0, new Object());
            }

            // See if we actually have some data. Set our BOF/EOF
            // if we don't and skip filling the rowdata
            if (!rs.first()) {
                mBOF = true;
                mEOF = true;
                mNoRows = 0;
                mCurrentRecord = 0;
            } else {
                // Loop through the recordset and fill the row data accordingly:
                SQLRowData rd = null;
                mNoRows = 0;
                rs.beforeFirst();

                while (rs.next()) {
                    rd = new SQLRowData(mNoFields + 1);
                    i = 1;

                    while (i <= mNoFields) {
                        if (mFieldTypes[i] == SQLFieldDescriptor.SQLFIELDTYPE_DATE) {
                            rd.theRowData[i] = rs.getTimestamp(i);

                            if (!rs.wasNull()) {
                                rd.theRowData[i] = new java.util.Date(((Timestamp) rd.theRowData[i]).getTime());
                            }
                        }

                        if (mFieldTypes[i] == SQLFieldDescriptor.SQLFIELDTYPE_INTEGER) {
                            rd.theRowData[i] = new Integer(rs.getInt(i));
                        }

                        if (mFieldTypes[i] == SQLFieldDescriptor.SQLFIELDTYPE_FLOAT) {
                            rd.theRowData[i] = new Double(rs.getDouble(i));
                        }

                        if (mFieldTypes[i] == SQLFieldDescriptor.SQLFIELDTYPE_STRING) {
                            rd.theRowData[i] = rs.getString(i);
                        }

                        if (rs.wasNull()) {
                            rd.theRowData[i] = null;
                        }

                        i++;
                    }

                    mNoRows++;
                    mtheRows.add(mNoRows, rd);
                }

                // Move to the first row
                mBOF = false;
                mEOF = false;
                mCurrentRecord = 1;
            }

            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stmt = null;
            rs = null;
        }
    }

    /**
     * Adds a new record to the set
     */
    public void addNew() {
        SQLRowData rd = new SQLRowData(mNoFields + 1);

        // Flag as new
        rd.isNew = true;

        mBOF = false;
        mEOF = false;
        mNoRows = mNoRows + 1;

        // Add it to the data
        mtheRows.add(mNoRows, rd);

        // Move to it
        mCurrentRecord = mNoRows;
    }

    /**
     * Flags a record for deletion and hides it from the set. It is not removed
     * until the save method is called.
     *
     * @throws CursorEngineException
     *             if there is no current record
     */
    public void delete() throws CursorEngineException {
        // Flag the current row as deleted
        if (mBOF || mEOF) {
            throw new CursorEngineException(
                "SQLRecordset.delete - No current record.");
        }

        SQLRowData rd = (SQLRowData) mtheRows.get(mCurrentRecord);
        rd.isDeleted = true;
    }

    /**
     * Returns the absolute position of the cursor currently in the set
     */
    public int getAbsolutePosition() {
        return mCurrentRecord;
    }

    /**
     * Allows you to set the absolute position of the cursor
     *
     * @param value
     *            The new absolute position
     * @throws CursorEngineException
     *             if new absolute position is invalid
     */
    public void setAbsolutePosition(int value) throws CursorEngineException {
        if ((value > 0) && (value <= mNoRows)) {
            mCurrentRecord = value;
            // Reset BOF/EOF flags
            mBOF = false;
            mEOF = false;
        } else {
            throw new CursorEngineException(
                "SQLRecordset.setAbsolutePosition - Invalid absolute position specified.");
        }
    }

    /**
     * Returns the number of records in the set
     *
     * @return The number of records in the set.
     */
    public long getRecordCount() {
        return mNoRows;
    }

    /**
     * Returns true if the cursor is before the beginning of the set
     *
     * @return True if the cursor is before the beginning of the set.
     */
    public boolean getBOF() {
        return mBOF;
    }

    /**
     * Returns true if the cursor is after the end of the set
     *
     * @return True if the cursor is after the end of the set.
     */
    public boolean getEOF() {
        return mEOF;
    }

    /**
     * Moves to the first record.
     *
     * @throws CursorEngineException
     *             if no records are available
     */
    public void moveFirst() throws CursorEngineException {
        if (mNoRows == 0) {
            throw new CursorEngineException(
                "SQLRecordset.moveFirst - no records in set.");
        }

        mCurrentRecord = 1;
        mBOF = false;
        mEOF = false;
    }

    /**
     * Moves to the last record.
     *
     * @throws CursorEngineException
     *             if no records are available
     */
    public void moveLast() throws CursorEngineException {
        if (mNoRows == 0) {
            throw new CursorEngineException(
                "SQLRecordset.moveLast - no records in set.");
        }

        mCurrentRecord = mNoRows;
        mBOF = false;
        mEOF = false;
    }

    /**
     * Moves to the previous record.
     *
     * @throws CursorEngineException
     *             if BOF is set
     */
    public void movePrevious() throws CursorEngineException {
        if (mBOF) {
            throw new CursorEngineException(
                "SQLRecordset.movePrevious - already before beginning of recordset.");
        }

        if (mCurrentRecord <= 1) {
            mBOF = true;
            mCurrentRecord = 0;

            return;
        }

        mCurrentRecord--;

        // If the row has been deleted, step another place backwards
        SQLRowData rd = (SQLRowData) mtheRows.get(mCurrentRecord);

        if (rd.isDeleted) {
            movePrevious();
        }
    }

    /**
     * Moves to the next record.
     *
     * @throws CursorEngineException
     *             if EOF is set
     */
    public void moveNext() throws CursorEngineException {
        if (mEOF) {
            throw new CursorEngineException(
                "SQLRecordset.moveNext - already beyond end of recordset.");
        }

        if (mCurrentRecord == mNoRows) {
            mEOF = true;
            mCurrentRecord = 0;

            return;
        }

        mCurrentRecord++;

        // If the row has been deleted, move another place forwards
        SQLRowData rd = (SQLRowData) mtheRows.get(mCurrentRecord);

        if (rd.isDeleted) {
            moveNext();
        }
    }

    /** Returns the number of fields in the set */
    public int getFieldCount() {
        return mNoFields;
    }

    /**
     * Returns the name of field with index <x> - note that fields use a 1-based
     * count, so you can iterate from 1 to getFieldCount()
     */
    public String getFieldName(int index) {
        SQLFieldDescriptor fd = (SQLFieldDescriptor) mtheFields.get(index);

        return fd.name;
    }

    /**
     * Returns the index of a given field from it's name
     */
    public int getFieldIndex(String name) {
        Object idx = mFieldIndexes.get(name.toLowerCase());

        if (idx == null) {
            return -1;
        }

        return ((Integer) idx).intValue();
    }

    /**
     * Returns true if the given field name exists in the set.
     *
     * @param fieldName
     *            The field to find
     * @return True if the field exists
     */
    public boolean getFieldExists(String fieldName) {
        return mFieldIndexes.get(fieldName.toLowerCase()) != null;
    }

    /**
     * Retrieves the contents of a field by its name. Note that a returned value
     * of ##NULL## is an SQL NULL value.
     *
     * @param fieldName
     *            The name of the field to retrieve the value of
     * @return The value of the field named as a String - a String containing
     *         ##NULL## is returned from NULL values.
     * @throws CursorEngineException
     *             if BOF/EOF is true or the field does not exist.
     *
     */
    public Object getField(String fieldName) throws CursorEngineException {
        // Make sure we have a record
        if (mBOF || mEOF) {
            throw new CursorEngineException(
                "SQLRecordset.getField - BOF or EOF is true.");
        }

        int fieldindex = getFieldIndex(fieldName);

        if (fieldindex == -1) {
            throw new CursorEngineException("SQLRecordset.getField - field '" +
                fieldName + "' does not exist.");
        }

        // Return the value
        SQLRowData rd = (SQLRowData) mtheRows.get(mCurrentRecord);

        if (rd.theRowData[fieldindex] == null) {
            return null;
        } else if (mFieldTypes[fieldindex] == SQLFieldDescriptor.SQLFIELDTYPE_STRING) {
            return apostropheFakeToReal(rd.theRowData[fieldindex].toString());
        } else {
            return rd.theRowData[fieldindex];
        }
    }

    /**
     * Sets the contents of a field by it's name.
     *
     * @param fieldName
     *            The name of the field you want to set a new value to.
     * @param newValue
     *            The value to set this field
     * @throws CursorEngineException
     *             if BOF/EOF is true or the field specified in fieldName does
     *             not exist.
     */
    public void setField(String fieldName, Object newValue)
        throws CursorEngineException {
        // Make sure we have a record
        if (mBOF || mEOF) {
            throw new CursorEngineException(
                "SQLRecordset.setField - BOF or EOF is true.");
        }

        // Find this field's index in the list
        int fieldindex = getFieldIndex(fieldName);

        if (fieldindex == -1) {
            throw new CursorEngineException("SQLRecordset.getField - field '" +
                fieldName + "' does not exist.");
        }

        // Get the current row
        SQLRowData rd = (SQLRowData) mtheRows.get(mCurrentRecord);

        if (newValue == null) {
            rd.theRowData[fieldindex] = null;
        } else if (mFieldTypes[fieldindex] == SQLFieldDescriptor.SQLFIELDTYPE_STRING) {
            rd.theRowData[fieldindex] = apostropheRealToFake(newValue.toString());
        } else {
            rd.theRowData[fieldindex] = newValue;
        }

        rd.needsSaving = true;
    }

    /**
     * Marks every row in a recordset as needing saving and being new ready for
     * insertion. Useful if you want to write the contents of this recordset to
     * another database.
     */
    public void markAllRowsAsNew() {
        for (int i = 0; i < mtheRows.size(); i++) {
            if (mtheRows.get(i) instanceof SQLRowData) {
                SQLRowData rd = (SQLRowData) mtheRows.get(i);

                if (rd != null) {
                    rd.needsSaving = true;
                    rd.isNew = true;
                    rd.isDeleted = false;
                }
            }
        }
    }

    public void save(boolean tableHasUserInfo, String savingUserName)
        throws Exception {
        save(DBConnection.con, DBConnection.DBType, tableHasUserInfo,
            savingUserName);
    }

    public void save(Connection c, byte dbType, boolean tableHasUserInfo,
        String savingUserName) throws Exception {
        save(c, dbType, tableHasUserInfo, savingUserName, true);
    }

    /**
     * Submits all changes that have been made to the recordset to the data
     * source in the form of ANSI-Compliant action queries.
     *
     * @param c
     *            The database connection to write out to
     * @param dbType
     *            One of DBConnection.MYSQL/POSTGRESQL/HSQLDB
     * @param tableHasUserInfo
     *            If this flag is set, the cursor will fill fields named
     *            "CreatedBy", "CreatedDate", "LastChangedDate" and
     *            "LastChangedBy" for audit trail purposes.
     * @param savingUserName
     *            The name of the user to stamp for the audit trail.
     * @param breakOnException
     *            If an exception occurs, throw it
     */
    public void save(Connection c, byte dbType, boolean tableHasUserInfo,
        String savingUserName, boolean breakOnException)
        throws Exception {
        StringBuffer S = new StringBuffer("");
        int l = 0;
        int i = 0;

        // If this table has user info, stamp it
        try {
            if (tableHasUserInfo) {
                stampUserInfo(savingUserName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // If there aren't any rows, don't do anything
        if (mNoRows == 0) {
            return;
        }

        // Loop through our available rows and find the ones that
        // need saving, updating or deleting:
        l = 1;

        while (l <= mNoRows) {
            SQLRowData rd = (SQLRowData) mtheRows.get(l);

            if (S.length() > 0) {
                S.replace(0, S.length(), "");
            }

            if (rd.isDeleted) {
                // Do DELETE script
                SQLFieldDescriptor fd = (SQLFieldDescriptor) mtheFields.get(0);
                S.append("DELETE FROM " + mTableName + " WHERE " + fd.name +
                    " = " + rd.theRowData[0]);

                try {
                    DBConnection.executeAction(c, S.toString());
                } catch (Exception e) {
                    if (breakOnException) {
                        System.out.println(S.toString());
                        throw e;
                    } else {
                        e.printStackTrace();
                    }
                }
            }

            if (rd.needsSaving) {
                if (rd.isNew) {
                    // Do INSERT script
                    S.append("INSERT INTO " + mTableName + " (");
                    // Loop through field names and put them in
                    i = 1;

                    while (i <= mNoFields) {
                        SQLFieldDescriptor fd = (SQLFieldDescriptor) mtheFields.get(i);
                        S.append(fd.name);

                        if (i < (mNoFields)) {
                            S.append(",");
                        }

                        i++;
                    }

                    S.append(") VALUES (");
                    // Loop through our fields again and write the values
                    i = 1;

                    while (i <= mNoFields) {
                        SQLFieldDescriptor fd = (SQLFieldDescriptor) mtheFields.get(i);
                        S.append(getScriptVal(dbType, rd.theRowData[i],
                                fd.type, fd.name));

                        if (i < (mNoFields)) {
                            S.append(",");
                        }

                        i++;
                    }

                    // Close brackets
                    S.append(")");

                    // Execute our insert
                    try {
                        DBConnection.executeAction(c, S.toString());
                    } catch (Exception e) {
                        if (breakOnException) {
                            System.out.println(S.toString());
                            throw e;
                        } else {
                            e.printStackTrace();
                        }
                    }

                    // Record is no longer new
                    rd.isNew = false;
                    rd.needsSaving = false;
                } else {
                    // Do UPDATE script
                    S.append("UPDATE " + mTableName + " SET ");
                    // Loop through our fields and write the name and values.
                    // Note that IDs (first field) is excluded in updates
                    i = 1;

                    while (i <= mNoFields) {
                        SQLFieldDescriptor fd = (SQLFieldDescriptor) mtheFields.get(i);

                        if (!fd.name.equalsIgnoreCase("ID")) {
                            S.append(fd.name + "=" +
                                getScriptVal(dbType, rd.theRowData[i], fd.type,
                                    fd.name));

                            if (i < (mNoFields)) {
                                S.append(",");
                            }
                        }

                        i++;
                    }

                    SQLFieldDescriptor fd = (SQLFieldDescriptor) mtheFields.get(1);
                    S.append(" WHERE " + fd.name + "=" + rd.theRowData[1]);

                    // Execute it
                    try {
                        DBConnection.executeAction(c, S.toString());
                    } catch (Exception e) {
                        if (breakOnException) {
                            System.out.println(S.toString());
                            throw e;
                        } else {
                            e.printStackTrace();
                        }
                    }

                    // Doesn't need saving any more
                    rd.needsSaving = false;
                }
            }

            l++;
        }
    }

    /**
     * Accepts a variant string value, a type to format it to and a field name.
     * It returns the value in SQL script format, ready for pasting into a
     * script to shove in the database.
     *
     * @param dbType
     *            One of DBConnection.MYSQL/POSTGRESQL/HSQLDB
     * @param theVal
     *            The value we are formatting.
     * @param theType
     *            A type constant from
     *            net.sourceforge.sheltermanager.cursorengine.SQLFieldDescriptor
     * @param theFieldName
     *            The name of the field this script is for (not used for
     *            anything at present).
     * @return An appropriate value for pasting into an SQL script.
     */
    private String getScriptVal(byte dbType, Object theVal, byte theType,
        String theFieldName) {
        if (theVal == null) {
            return "Null";
        }

        // Check the field type and format the value we have accordingly.
        switch (theType) {
        case SQLFieldDescriptor.SQLFIELDTYPE_DATE:
            return "'" +
            getSQLRepresentationOfDate(dbType, (java.util.Date) theVal) + "'";

        case SQLFieldDescriptor.SQLFIELDTYPE_STRING:
            return "'" + theVal.toString().replace('\'', '`') + "'";

        case SQLFieldDescriptor.SQLFIELDTYPE_INTEGER:
            return theVal.toString();

        case SQLFieldDescriptor.SQLFIELDTYPE_FLOAT:
            return theVal.toString();
        }

        // In case the switch failed - return nothing
        return "";
    }

    /**
     * Returns the version of the cursor engine
     *
     * @return The cursor engine version.
     */
    public static String getCursorVersion() {
        return "SQLRecordset 3.1 (190308)";
    }

    /**
     * Stamps audit trail information into the table if it has the fields and
     * you specified it in the Save method.
     *
     * @param theUser
     *            The name of the user for audit trail purposes.
     * @throws CursorEngineException
     *             if BOF/EOF is true
     *
     */
    private void stampUserInfo(String theUser) throws CursorEngineException {
        String S = "";
        int LP = this.getAbsolutePosition();
        boolean tBOF = this.getBOF();
        boolean tEOF = this.getEOF();

        // Remember absolute position
        LP = mCurrentRecord;

        // Loop through each record in our
        // records and stamp the user information
        this.moveFirst();

        while (!this.getEOF()) {
            // Grab the current record
            SQLRowData rd = (SQLRowData) mtheRows.get(mCurrentRecord);

            // Make sure it needs saving (don't stamp records that
            // haven't been changed) and that it isn't marked for deletion.
            if (rd.needsSaving && !rd.isDeleted) {
                S = (String) this.getField("CreatedBy");

                // If there is no created by, set it
                if ((S == null) || S.equals("")) {
                    this.setField("CreatedBy", theUser);
                    this.setField("CreatedDate", new java.util.Date());
                }

                // Set last changed by
                this.setField("LastChangedBy", theUser);
                this.setField("LastChangedDate", new java.util.Date());
            }

            this.moveNext();
        }

        // Go back to where we were
        mCurrentRecord = LP;
        mBOF = tBOF;
        mEOF = tEOF;
    }

    public static String getSQLRepresentationOfDate(java.util.Date d) {
        return getSQLRepresentationOfDate(DBConnection.DBType, d);
    }

    public static String getSQLRepresentationOfDateOnly(java.util.Date d) {
        return getSQLRepresentationOfDateOnly(DBConnection.DBType, d);
    }

    /**
     * Given a Java Date, looks up which DB we are using and puts it in a string
     * format suitable for SQL
     *
     * @param d
     *            The date
     * @return A string representation of the date suitable for SQL queries to
     *         the current database.
     */
    public static String getSQLRepresentationOfDate(byte dbType,
        java.util.Date d) {
        if (d == null) {
            return "Null";
        }

        if (dbType == DBConnection.MYSQL) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
        } else if (dbType == DBConnection.POSTGRESQL) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
        } else if (dbType == DBConnection.HSQLDB) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
        }

        return "";
    }

    /**
     * Given a Java Date, looks up which DB we are using and puts it in a string
     * format suitable for SQL without time info
     *
     * @param d
     *            The date
     * @return A string representation of the date suitable for SQL queries to
     *         the current database.
     */
    public static String getSQLRepresentationOfDateOnly(byte dbType,
        java.util.Date d) {
        if (d == null) {
            return "Null";
        }

        if (dbType == DBConnection.MYSQL) {
            return new SimpleDateFormat("yyyy-MM-dd").format(d);
        } else if (dbType == DBConnection.POSTGRESQL) {
            return new SimpleDateFormat("yyyy-MM-dd").format(d);
        } else if (dbType == DBConnection.HSQLDB) {
            return new SimpleDateFormat("yyyy-MM-dd").format(d);
        }

        return "";
    }

    /**
     * Searches the passed string and converts all backwards apostrophes (`) to
     * real apostrophes (') This is called by getField() when decoding
     * apostrophes as you can't have misplaced apostrophes in SQL.
     *
     * @param s
     *            The string to search in.
     * @return The value with fake apostrophes switched to real.
     */
    private String apostropheFakeToReal(String s) {
        return s.replace('`', '\'');
    }

    /**
     * Searches the passed string and converts all real apostrophes (') to
     * backwards apostrophes (`) This is called by setField() when encoding
     * apostrophes as you can't have misplaced apostrophes in SQL.
     *
     * @param s
     *            The string to search in.
     * @return The value with real apostrophes switched to fake.
     */
    private String apostropheRealToFake(String s) {
        return s.replace('\'', '`');
    }

    /**
     * Make sure objects get freed
     */
    public void finalize() throws Throwable {
        free();
    }

    public void free() {
        try {
            mtheRows.removeAllElements();
            mtheFields.removeAllElements();
            mFieldIndexes.clear();
        } catch (Exception e) {
        }

        mtheRows = null;
        mtheFields = null;
        mTableName = null;
        mFieldIndexes = null;
    }
}