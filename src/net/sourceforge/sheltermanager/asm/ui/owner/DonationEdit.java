/*
 Animal Shelter Manager
 Copyright(c)2000-2011, R. Rawson-Tetley

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
package net.sourceforge.sheltermanager.asm.ui.owner;

import net.sourceforge.sheltermanager.asm.bo.Adoption;
import net.sourceforge.sheltermanager.asm.bo.AuditTrail;
import net.sourceforge.sheltermanager.asm.bo.Configuration;
import net.sourceforge.sheltermanager.asm.bo.LookupCache;
import net.sourceforge.sheltermanager.asm.bo.OwnerDonation;
import net.sourceforge.sheltermanager.asm.globals.Global;
import net.sourceforge.sheltermanager.asm.ui.animal.AnimalLink;
import net.sourceforge.sheltermanager.asm.ui.animal.AnimalLinkListener;
import net.sourceforge.sheltermanager.asm.ui.ui.ASMForm;
import net.sourceforge.sheltermanager.asm.ui.ui.CurrencyField;
import net.sourceforge.sheltermanager.asm.ui.ui.DateField;
import net.sourceforge.sheltermanager.asm.ui.ui.Dialog;
import net.sourceforge.sheltermanager.asm.ui.ui.IconManager;
import net.sourceforge.sheltermanager.asm.ui.ui.UI;
import net.sourceforge.sheltermanager.asm.utility.Utils;
import net.sourceforge.sheltermanager.cursorengine.DBConnection;

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;


/**
 * Edit a donation
 *
 * @author Robin Rawson-Tetley
 */
public class DonationEdit extends ASMForm implements AnimalLinkListener,
    OwnerLinkListener {
    private static final long serialVersionUID = 9186238748769049351L;
    private DonationSelector parent = null;
    private OwnerDonation od = null;
    private int animalID = 0;
    private int ownerID = 0;
    private int movementID = 0;
    private UI.Button btnCancel;
    private UI.Button btnOk;
    private UI.CheckBox chkGiftAid;
    private UI.ComboBox cboFrequency;
    private UI.TextArea txtComments;
    private DateField txtDateDue;
    private DateField txtDateReceived;
    private CurrencyField txtDonation;
    private UI.ComboBox cboDonationType;
    private OwnerLink olOwner;
    private AnimalLink embAnimal;
    private String audit = null;
    private boolean isNew = false;

    /** Creates new form EditOwnerDonation */
    public DonationEdit(DonationSelector parent, int animalID, int ownerID,
        int movementID) {
        this.animalID = animalID;
        this.ownerID = ownerID;
        this.movementID = movementID;
        this.parent = parent;
        init("", IconManager.getIcon(IconManager.SCREEN_EDITOWNERDONATION),
            "uiowner");
    }

    public Vector<Object> getTabOrder() {
        Vector<Object> ctl = new Vector<Object>();
        ctl.add(txtDateDue.getTextField());
        ctl.add(txtDateReceived.getTextField());
        ctl.add(txtDonation.getTextField());
        ctl.add(cboFrequency);
        ctl.add(cboDonationType);
        ctl.add(chkGiftAid);
        ctl.add(txtComments);
        ctl.add(btnOk);
        ctl.add(btnCancel);

        return ctl;
    }

    public Object getDefaultFocusedComponent() {
        return txtDateDue.getTextField();
    }

    public void dispose() {
        try {
            od.free();
        } catch (Exception e) {
        }

        parent = null;
        od = null;
        super.dispose();
    }

    public void openForEdit(OwnerDonation od) {
        this.od = od;

        try {
            txtDateDue.setText(Utils.formatDate(od.getDateDue()));
            txtDateReceived.setText(Utils.formatDate(od.getDateReceived()));
            txtDonation.setValue(od.getDonation().intValue());
            cboFrequency.setSelectedIndex(((Integer) od.getFrequency()).intValue());
            chkGiftAid.setSelected(od.getIsGiftAid().intValue() == 1);
            Utils.setComboFromID(LookupCache.getDonationTypeLookup(),
                "DonationName", od.getDonationTypeID(), cboDonationType);
            txtComments.setText(od.getComments());

            if ((od.getOwnerID() != null) && (olOwner != null)) {
                olOwner.setID(od.getOwnerID());
            }

            if ((od.getAnimalID().intValue() != 0) && (embAnimal != null)) {
                embAnimal.loadFromID(od.getAnimalID());
            }

            setTitle(i18n("edit_owner_donation"));
            audit = UI.messageAudit(od.getCreatedDate(), od.getCreatedBy(),
                    od.getLastChangedDate(), od.getLastChangedBy());
        } catch (Exception e) {
            Global.logException(e, getClass());
        }
    }

    public void openForNew() {
        try {
            this.od = new OwnerDonation();
            od.openRecordset("ID = 0");
            od.addNew();
            od.setAnimalID(new Integer(animalID));
            od.setOwnerID(new Integer(ownerID));
            od.setMovementID(new Integer(movementID));

            if (movementID > 0) {
                this.txtDonation.setValue(LookupCache.getDonationAmountForMovementSpecies(
                        movementID).intValue());
            }

            // Set default donation type if we have one
            Utils.setComboFromID(LookupCache.getDonationTypeLookup(),
                "DonationName",
                new Integer(Configuration.getInteger("AFDefaultDonationType")),
                cboDonationType);

            // Set gift aid from registered flag if locale is UK
            if (Global.settings_Locale.equalsIgnoreCase("en_GB")) {
                chkGiftAid.setSelected(DBConnection.executeForInt(
                        "SELECT IsGiftAid FROM owner WHERE ID = " + ownerID) == 1);
            }

            this.setTitle(i18n("new_owner_donation"));
            isNew = true;
        } catch (Exception e) {
            Dialog.showError(i18n("unable_to_create_new_ownerdonation") +
                e.getMessage());
            Global.logException(e, getClass());
        }
    }

    public void initComponents() {
        UI.Panel top = UI.getPanel(UI.getGridLayout(2, new int[] { 30, 70 }));
        UI.Panel mid = UI.getPanel(UI.getGridLayout(2, new int[] { 30, 70 }));
        UI.Panel but = UI.getPanel(UI.getFlowLayout());

        txtDateDue = (DateField) UI.addComponent(top, i18n("date_due"),
                UI.getDateField());

        txtDateReceived = (DateField) UI.addComponent(top,
                i18n("date_received"), UI.getDateField());

        txtDonation = (CurrencyField) UI.addComponent(top, i18n("donation"),
                UI.getCurrencyField());

        cboFrequency = UI.getCombo(LookupCache.getDonationFreqLookup(),
                "Frequency");
        cboFrequency.setSelectedIndex(0);
        UI.addComponent(top, i18n("frequency"), cboFrequency);

        cboDonationType = UI.getCombo(i18n("type"),
                LookupCache.getDonationTypeLookup(), "DonationName");
        UI.addComponent(top, i18n("type"), cboDonationType);

        chkGiftAid = UI.getCheckBox(i18n("Gift_Aid"));

        if (Global.settings_Locale.equalsIgnoreCase("en_GB")) {
            top.add(UI.getLabel());
            top.add(chkGiftAid);
        }

        // If we have no owner ID, allow box to choose it
        if (ownerID == 0) {
            olOwner = (OwnerLink) UI.addComponent(top, i18n("Owner"),
                    new OwnerLink(OwnerLink.MODE_ONELINE,
                        OwnerLink.FILTER_NONE, "OWNER"));
            olOwner.setParent(this);
        }

        // If we have no animal ID, allow box to choose it
        if (animalID == 0) {
            embAnimal = (AnimalLink) UI.addComponent(top, i18n("Animal"),
                    new AnimalLink(this));
        }

        txtComments = (UI.TextArea) UI.addComponent(mid, i18n("comments"),
                UI.getTextArea());

        btnOk = (UI.Button) but.add(UI.getButton(i18n("ok"), null, 'o', null,
                    UI.fp(this, "saveData")));
        btnCancel = (UI.Button) but.add(UI.getButton(i18n("cancel"), null, 'c',
                    null, UI.fp(this, "dispose")));

        add(top, UI.BorderLayout.NORTH);
        add(mid, UI.BorderLayout.CENTER);
        add(but, UI.BorderLayout.SOUTH);
    }

    public void loadData() {
    }

    public boolean formClosing() {
        return false;
    }

    public String getAuditInfo() {
        return audit;
    }

    public void setSecurity() {
    }

    public boolean saveData() {
        // Save values
        try {
            od.setDateReceived(Utils.parseDate(txtDateReceived.getText()));
            od.setDateDue(Utils.parseDate(txtDateDue.getText()));
            od.setDonation(new Integer(txtDonation.getValue()));
            od.setFrequency(new Integer(cboFrequency.getSelectedIndex()));
            od.setIsGiftAid(new Integer(chkGiftAid.isSelected() ? 1 : 0));

            if (embAnimal != null) {
                od.setAnimalID(embAnimal.getID());
            }

            od.setDonationTypeID(Utils.getIDFromCombo(
                    LookupCache.getDonationTypeLookup(), "DonationName",
                    cboDonationType));
            od.setComments(txtComments.getText());

            // Do we have a frequency > 0, the nextcreated flag isn't set
            // and there's a datereceived and a datedue?
            if ((od.getDateDue() != null) && (od.getDateReceived() != null) &&
                    (((Integer) od.getFrequency()).intValue() > 0) &&
                    (((Integer) od.getNextCreated()).intValue() == 0)) {
                OwnerDonation od2 = od.copy();

                // Clear the date received and update the date due
                Calendar c = Calendar.getInstance();
                c.setTime(((Date) od.getDateDue()));

                int freq = ((Integer) od.getFrequency()).intValue();

                switch (freq) {
                case 1: // weekly
                    c.add(Calendar.WEEK_OF_YEAR, 1);

                    break;

                case 2: // monthly
                    c.add(Calendar.MONTH, 1);

                    break;

                case 3: // quarterly
                    c.add(Calendar.MONTH, 3);

                    break;

                case 4: // half-yearly
                    c.add(Calendar.MONTH, 6);

                    break;

                case 5: // annually
                    c.add(Calendar.YEAR, 1);

                    break;
                }

                od2.setDateReceived(null);
                od2.setDateDue(c.getTime());

                // Save our next instalment
                od2.save(Global.currentUserName);

                // Update the created flag for this donation
                od.setNextCreated(new Integer(1));
            }

            // Save the record
            od.save(Global.currentUserName);

            if (AuditTrail.enabled()) {
                AuditTrail.updated(isNew, "ownerdonation",
                    od.getOwner().getOwnerName() + " " +
                    od.getDonationTypeName());
            }

            // Update the accounting system - regardless of whether it's
            // enabled or not so that things are kept correctly in sync
            od.updateAccountTrx();

            // If this donation mapped to a movement, update that
            // movement's denormalised donation total field 
            if (movementID != 0) {
                Adoption.updateDonation(movementID);
            }

            // Update parent
            parent.updateList();

            dispose();

            return true;
        } catch (Exception e) {
            Dialog.showError(e.getMessage());
            Global.logException(e, getClass());
        }

        return false;
    }

    public void animalChanged(int animalid) {
        try {
            this.animalID = animalid;
            od.setAnimalID(new Integer(animalid));
        } catch (Exception e) {
            Global.logException(e, getClass());
        }
    }

    public void ownerChanged(int ownerid, String id) {
        try {
            if (id.equals("OWNER")) {
                this.ownerID = ownerid;
                od.setOwnerID(new Integer(ownerid));
            }
        } catch (Exception e) {
            Global.logException(e, getClass());
        }
    }
}
