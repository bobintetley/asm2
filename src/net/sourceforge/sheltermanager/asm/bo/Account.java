/*
Animal Shelter Manager
Copyright(c)2000-2010, R. Rawson-Tetley

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
package net.sourceforge.sheltermanager.asm.bo;

import net.sourceforge.sheltermanager.asm.utility.Utils;
import net.sourceforge.sheltermanager.cursorengine.CursorEngineException;
import net.sourceforge.sheltermanager.cursorengine.DBConnection;
import net.sourceforge.sheltermanager.cursorengine.UserInfoBO;

import java.util.Date;


public class Account extends UserInfoBO<Account> {
    public static final int BANK = 1;
    public static final int CREDITCARD = 2;
    public static final int LOAN = 3;
    public static final int EXPENSE = 4;
    public static final int INCOME = 5;
    public static final int PENSION = 6;
    public static final int SHARES = 7;
    public static final int ASSET = 8;
    public static final int LIABILITY = 9;

    public Account() {
        tableName = "accounts";
    }

    public Account(String where) {
        this();
        openRecordset(where);
    }

    public void addNew() throws CursorEngineException {
        super.addNew();
        setCode("");
        setDescription("");
        setAccountType(new Integer(1));
        setDonationTypeID(new Integer(1));
    }

    public Account copy() throws CursorEngineException {
        Account a = new Account();
        a.openRecordset("ID = 0");
        a.addNew();
        a.setCode(getCode());
        a.setDescription(getDescription());
        a.setAccountType(getAccountType());
        a.setDonationTypeID(getDonationTypeID());

        return a;
    }

    public Integer getID() throws CursorEngineException {
        return (Integer) rs.getField("ID");
    }

    public void setID(Integer newValue) throws CursorEngineException {
        rs.setField("ID", newValue);
    }

    public String getCode() throws CursorEngineException {
        return (String) rs.getField("Code");
    }

    public void setCode(String newValue) throws CursorEngineException {
        rs.setField("Code", newValue);
    }

    public String getDescription() throws CursorEngineException {
        return (String) rs.getField("Description");
    }

    public void setDescription(String newValue) throws CursorEngineException {
        rs.setField("Description", newValue);
    }

    public Integer getAccountType() throws CursorEngineException {
        return (Integer) rs.getField("AccountType");
    }

    public String getAccountTypeName() throws CursorEngineException {
        return LookupCache.getAccountTypeNameForID(getAccountType());
    }

    public void setAccountType(Integer newValue) throws CursorEngineException {
        rs.setField("AccountType", newValue);
    }

    public Integer getDonationTypeID() throws CursorEngineException {
        return (Integer) rs.getField("DonationTypeID");
    }

    public void setDonationTypeID(Integer newValue)
        throws CursorEngineException {
        rs.setField("DonationTypeID", newValue);
    }

    public String getCreatedBy() throws CursorEngineException {
        return (String) rs.getField("CreatedBy");
    }

    public void setCreatedBy(String newValue) throws CursorEngineException {
        rs.setField("CreatedBy", newValue);
    }

    public Date getCreatedDate() throws CursorEngineException {
        return (Date) rs.getField("CreatedDate");
    }

    public void setCreatedDate(Date newValue) throws CursorEngineException {
        rs.setField("CreatedDate", newValue);
    }

    public String getLastChangedBy() throws CursorEngineException {
        return (String) rs.getField("LastChangedBy");
    }

    public void setLastChangedBy(String newValue) throws CursorEngineException {
        rs.setField("LastChangedBy", newValue);
    }

    public Date getLastChangedDate() throws CursorEngineException {
        return (Date) rs.getField("LastChangedDate");
    }

    public void setLastChangedDate(Date newValue) throws CursorEngineException {
        rs.setField("LastChangedDate", newValue);
    }

    /** Returns a list of all accounts, ordered by type and code */
    public static Account getAllAccounts() throws CursorEngineException {
        Account a = new Account();
        a.openRecordset("ID > 0 ORDER BY AccountType, Code");

        return a;
    }

    /** Returns an account with a specific ID */
    public static Account getAccountByID(Integer id)
        throws CursorEngineException {
        Account a = new Account();
        a.openRecordset("ID = " + id);

        return a;
    }

    /** Returns an account with a specific code */
    public static Account getAccountByCode(String code)
        throws CursorEngineException {
        Account a = new Account();
        a.openRecordset("Code = '" + code + "'");

        return a;
    }

    /** Marks everything in this account reconciled upto today's date */
    public void markReconciledToDate() throws Exception {
        DBConnection.executeAction("UPDATE accountstrx SET Reconciled=1 WHERE " +
            "(SourceAccountID = " + getID() + " OR DestinationAccountID = " +
            getID() + ") AND " + "TrxDate <= '" + Utils.getSQLDate(new Date()) +
            "'");
    }

    /** Calculates the balance for this account */
    public double getAccountBalance() throws Exception {
        // Withdrawals
        double withdrawal = DBConnection.executeForDouble(
                "SELECT SUM(Amount) FROM accountstrx WHERE SourceAccountID = " +
                getID());

        // Deposits
        double deposit = DBConnection.executeForDouble(
                "SELECT SUM(Amount) FROM accountstrx WHERE DestinationAccountID = " +
                getID());
        double rounded = Utils.round(deposit - withdrawal, 2);

        // Income and expense accounts should be positive
        if ((getAccountType().intValue() == INCOME) ||
                (getAccountType().intValue() == EXPENSE)) {
            rounded = Math.abs(rounded);
        }

        return rounded;
    }

    /** Calculates the reconciled amount for this account */
    public double getReconciled() throws Exception {
        // Withdrawals
        double withdrawal = DBConnection.executeForDouble(
                "SELECT SUM(Amount) FROM accountstrx WHERE Reconciled = 1 AND SourceAccountID = " +
                getID());

        // Deposits
        double deposit = DBConnection.executeForDouble(
                "SELECT SUM(Amount) FROM accountstrx WHERE Reconciled = 1 AND DestinationAccountID = " +
                getID());
        double rounded = Utils.round(deposit - withdrawal, 2);

        // Income and expense accounts should be positive
        if ((getAccountType().intValue() == INCOME) ||
                (getAccountType().intValue() == EXPENSE)) {
            rounded = Math.abs(rounded);
        }

        return rounded;
    }

    /** Calculates the balance for this account to a certain date */
    public static double getAccountBalanceToDate(Integer accountId, Date limit)
        throws Exception {
        // Withdrawals
        double withdrawal = DBConnection.executeForDouble(
                "SELECT SUM(Amount) FROM accountstrx WHERE SourceAccountID = " +
                accountId + " AND TrxDate < '" + Utils.getSQLDate(limit) + "'");

        // Deposits
        double deposit = DBConnection.executeForDouble(
                "SELECT SUM(Amount) FROM accountstrx WHERE DestinationAccountID = " +
                accountId + " AND TrxDate < '" + Utils.getSQLDate(limit) + "'");
        double rounded = Utils.round(deposit - withdrawal, 2);

        int accountType = DBConnection.executeForInt(
                "SELECT AccountType FROM accounts WHERE ID = " + accountId);

        // Income and expense accounts should always be positive
        if ((accountType == INCOME) || (accountType == EXPENSE)) {
            rounded = Math.abs(rounded);
        }

        return rounded;
    }
}