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
package net.sourceforge.sheltermanager.asm.bo;

import net.sourceforge.sheltermanager.asm.globals.Global;
import net.sourceforge.sheltermanager.cursorengine.BOValidationException;
import net.sourceforge.sheltermanager.cursorengine.CursorEngineException;
import net.sourceforge.sheltermanager.cursorengine.UserInfoBO;

import java.util.Date;


public class OwnerDonation extends UserInfoBO {
    public OwnerDonation() {
        tableName = "ownerdonation";
    }

    public Integer getID() throws CursorEngineException {
        return (Integer) rs.getField("ID");
    }

    public void setID(Integer newValue) throws CursorEngineException {
        rs.setField("ID", newValue);
    }

    public Integer getOwnerID() throws CursorEngineException {
        return (Integer) rs.getField("OwnerID");
    }

    public void setOwnerID(Integer newValue) throws CursorEngineException {
        rs.setField("OwnerID", newValue);
    }

    public Integer getMovementID() throws CursorEngineException {
        return (Integer) rs.getField("MovementID");
    }

    public void setMovementID(Integer newValue) throws CursorEngineException {
        rs.setField("MovementID", newValue);
    }

    public Integer getDonationTypeID() throws CursorEngineException {
        return (Integer) rs.getField("DonationTypeID");
    }

    public String getDonationTypeName() throws CursorEngineException {
        return LookupCache.getDonationTypeName(getDonationTypeID());
    }

    public void setDonationTypeID(Integer newValue)
        throws CursorEngineException {
        rs.setField("DonationTypeID", newValue);
    }

    public Date getDateReceived() throws CursorEngineException {
        return (Date) rs.getField("Date");
    }

    public void setDateReceived(Date newValue) throws CursorEngineException {
        rs.setField("Date", newValue);
    }

    public Date getDateDue() throws CursorEngineException {
        return (Date) rs.getField("DateDue");
    }

    public void setDateDue(Date newValue) throws CursorEngineException {
        rs.setField("DateDue", newValue);
    }

    public Double getDonation() throws CursorEngineException {
        return (Double) rs.getField("Donation");
    }

    public void setDonation(Double newValue) throws CursorEngineException {
        rs.setField("Donation", newValue);
    }

    /**
     * Returns the ID formatted with padded zeroes to make a receipt number.
     */
    public String getReceiptNum() throws CursorEngineException {
        String pad = "0000000000";
        String newnum = getID().toString();
        newnum = pad.substring(0, 9 - newnum.length()) + newnum;

        return newnum;
    }

    public String getComments() throws CursorEngineException {
        return (String) rs.getField("Comments");
    }

    public void setComments(String newValue) throws CursorEngineException {
        rs.setField("Comments", newValue);
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

    public void validate() throws BOValidationException {
        try {
            // Check for blank or null donation and change it to 0
            if (getDonation() == null) {
                setDonation(new Double(0));
            }

            if ((getDateDue() == null) && (getDateReceived() == null)) {
                throw new BOValidationException(Global.i18n("bo",
                        "ownerdonation_must_have_at_least_one_date"));
            }
        } catch (Exception e) {
            Global.logException(e, this.getClass());
            throw new BOValidationException(Global.i18n("bo",
                    "An_error_occurred_while_validating_the_object:_") +
                e.getMessage());
        }
    }
}