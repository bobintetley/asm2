/*
 Animal Shelter Manager
 Copyright(c)2000-2009, R. Rawson-Tetley

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation; either version 2 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTIBILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the
 Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston
 MA 02111-1307, USA.

 Contact me by electronic mail: bobintetley@users.sourceforge.net
 */
package net.sourceforge.sheltermanager.asm.db;

import net.sourceforge.sheltermanager.asm.globals.Global;
import net.sourceforge.sheltermanager.asm.ui.ui.Dialog;
import net.sourceforge.sheltermanager.cursorengine.DBConnection;

import java.io.File;

import java.sql.Connection;


public class DatabaseCopier {
    public void start() {
        String s = Dialog.getJDBCUrl(Global.i18n("db", "copy_database"));

        if (s.equals("")) {
            return;
        }

        try {
            // Do we have a local database and they want to
            // copy locally?
            if ((DBConnection.DBType == DBConnection.HSQLDB) &&
                    (s.indexOf("hsqldb:file") != -1)) {
                Dialog.showError(Global.i18n("db", "database_already_local",
                        Global.tempDirectory + File.separator + "localdb.*"),
                    Global.i18n("db", "cant_copy"));

                return;
            }

            // Is it a local copy and we already have a local database? 
            // If so, prompt them to remove it.
            if (s.indexOf("hsqldb:file") != -1) {
                File f = new File(Global.tempDirectory + File.separator +
                        "localdb.properties");

                if (f.exists()) {
                    if (Dialog.showYesNo(Global.i18n("db", "have_existing_local"),
                                Global.i18n("db", "existing_local"))) {
                        f.delete();
                        f = new File(Global.tempDirectory + File.separator +
                                "localdb.data");
                        f.delete();
                        f = new File(Global.tempDirectory + File.separator +
                                "localdb.script");
                        f.delete();
                        f = new File(Global.tempDirectory + File.separator +
                                "localdb.log");
                        f.delete();
                    }
                }
            }

            // Make sure the user knows they have to have created
            // the schema for mysql
            if (s.indexOf("mysql") != -1) {
                if (!Dialog.showYesNo(Global.i18n("db", "need_schema",
                                "mysql.sql",
                                Global.dataDirectory + File.separator + "sql"),
                            Global.i18n("db", "continue"))) {
                    return;
                }
            }

            // Make sure the user knows they have to have created
            // the schema for postgresql
            if (s.indexOf("postgre") != -1) {
                if (!Dialog.showYesNo(Global.i18n("db", "need_schema",
                                "postgresql.sql",
                                Global.dataDirectory + File.separator + "sql"),
                            Global.i18n("db", "continue"))) {
                    return;
                }
            }

            // Get a connection to our new database
            Connection c = DBConnection.getConnection(s);
            byte dbType = DBConnection.getDBTypeForUrl(s);

            // If it was local, create the database
            if (s.indexOf("hsqldb:file") != -1) {
                DBConnection.executeFile(c,
                    new File(Global.dataDirectory + File.separator + "sql" +
                        File.separator + "hsqldb.sql"));
            }

            // Do the copy
            Copier cop = new Copier(s, c, dbType);
            new Thread(cop).start();
        } catch (Exception e) {
            Global.logException(e, DatabaseCopier.class);
        }
    }
}


class Copier implements Runnable {
    private String url = null;
    private Connection c = null;
    private byte dbType = 0;

    public Copier(String url, Connection c, byte dbType) {
        this.url = url;
        this.c = c;
        this.dbType = dbType;
    }

    /**
     * Perform the copy
     */
    public void run() {
        // Run a few UPDATEs to ensure no NULLs can get through from
        // any old MySQL databases (if it is a MySQL DB)
        if (DBConnection.DBType == DBConnection.MYSQL) {
            try {
                DBConnection.executeAction(
                    "UPDATE animal SET DateOfBirth = CURRENT_DATE WHERE DateOfBirth Is Null OR DateOfBirth = '0000-00-00 00:00'");
            } catch (Exception e) {
                Global.logException(e, getClass());
            }

            try {
                DBConnection.executeAction(
                    "UPDATE animal SET DateBroughtIn = CURRENT_DATE WHERE DateBroughtIn Is Null OR DateBroughtIn = '0000-00-00 00:00'");
            } catch (Exception e) {
                Global.logException(e, getClass());
            }

            try {
                DBConnection.executeAction(
                    "UPDATE animal SET MostRecentEntryDate = CURRENT_DATE WHERE MostRecentEntryDate Is Null OR MostRecentEntryDate = '0000-00-00 00:00'");
            } catch (Exception e) {
                Global.logException(e, getClass());
            }

            try {
                DBConnection.executeAction(
                    "UPDATE animalmedical SET StartDate = CURRENT_DATE WHERE StartDate Is Null OR StartDate = '0000-00-00 00:00'");
            } catch (Exception e) {
                Global.logException(e, getClass());
            }

            try {
                DBConnection.executeAction(
                    "UPDATE animallost SET DateLost = CURRENT_DATE WHERE DateLost Is Null OR DateLost = '0000-00-00 00:00'");
            } catch (Exception e) {
                Global.logException(e, getClass());
            }

            try {
                DBConnection.executeAction(
                    "UPDATE animalfound SET DateFound = CURRENT_DATE WHERE DateFound Is Null OR DateFound = '0000-00-00 00:00'");
            } catch (Exception e) {
                Global.logException(e, getClass());
            }
        }

        Global.mainForm.initStatusBarMax(43);

        copyTable(c, dbType, "adoption", true, "copytool");
        copyTable(c, dbType, "animaldiet", true, "copytool");
        copyTable(c, dbType, "animalfound", true, "copytool");
        copyTable(c, dbType, "animallitter", false, "copytool");
        copyTable(c, dbType, "animallost", true, "copytool");
        copyTable(c, dbType, "animalmedical", true, "copytool");
        copyTable(c, dbType, "animalmedicaltreatment", true, "copytool");
        copyTable(c, dbType, "animal", true, "copytool");
        copyTable(c, dbType, "animalname", false, "copytool");
        copyTable(c, dbType, "animaltype", false, "copytool");
        copyTable(c, dbType, "animalvaccination", true, "copytool");
        copyTable(c, dbType, "animalwaitinglist", true, "copytool");
        copyTable(c, dbType, "basecolour", false, "copytool");
        copyTable(c, dbType, "breed", false, "copytool");
        copyTable(c, dbType, "configuration", false, "copytool");
        copyTable(c, dbType, "customreport", false, "copytool");
        copyTable(c, dbType, "deathreason", false, "copytool");
        copyTable(c, dbType, "diary", true, "copytool");
        copyTable(c, dbType, "diarytaskdetail", false, "copytool");
        copyTable(c, dbType, "diarytaskhead", false, "copytool");
        copyTable(c, dbType, "diet", false, "copytool");
        copyTable(c, dbType, "donationtype", false, "copytool");
        copyTable(c, dbType, "entryreason", false, "copytool");
        copyTable(c, dbType, "internallocation", false, "copytool");
        copyTable(c, dbType, "lksdiarylink", false, "copytool");
        copyTable(c, dbType, "lksex", false, "copytool");
        copyTable(c, dbType, "lksize", false, "copytool");
        copyTable(c, dbType, "lksloglink", false, "copytool");
        copyTable(c, dbType, "lksmedialink", false, "copytool");
        copyTable(c, dbType, "lksmovementtype", false, "copytool");
        copyTable(c, dbType, "lkurgency", false, "copytool");
        copyTable(c, dbType, "log", true, "copytool");
        copyTable(c, dbType, "logtype", false, "copytool");
        copyTable(c, dbType, "media", false, "copytool");
        copyTable(c, dbType, "medicalprofile", true, "copytool");
        copyTable(c, dbType, "medicalpayment", true, "copytool");
        copyTable(c, dbType, "medicalpaymenttype", false, "copytool");
        copyTable(c, dbType, "ownerdonation", false, "copytool");
        copyTable(c, dbType, "owner", true, "copytool");
        copyTable(c, dbType, "ownervoucher", false, "copytool");
        copyTable(c, dbType, "species", false, "copytool");
        copyTable(c, dbType, "users", false, "copytool");
        copyTable(c, dbType, "vaccinationtype", false, "copytool");
        copyTable(c, dbType, "voucher", false, "copytool");
        DBConnection.copyDBFS(c);

        try {
            c.close();
        } catch (Exception e) {
        }

        Global.mainForm.resetStatusBar();
        Global.mainForm.setStatusText("");

        Dialog.showInformation(Global.i18n("db", "copy_successful"),
            Global.i18n("db", "copy_complete"));

        if (Dialog.showYesNo(Global.i18n("db", "use_copied_in_future"),
                    Global.i18n("db", "switch_copied"))) {
            LocateDatabase.saveJDBCUrl(url);
        }
    }

    public void copyTable(Connection c, byte dbType, String tableName,
        boolean userInfo, String userName) {
        Global.mainForm.setStatusText(Global.i18n("db", "copying_table",
                tableName));
        Global.mainForm.incrementStatusBar();
        DBConnection.copyTable(c, dbType, tableName, userInfo, userName);
    }
}