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
package net.sourceforge.sheltermanager.asm.ui.animal;

import net.sourceforge.sheltermanager.asm.bo.Animal;
import net.sourceforge.sheltermanager.asm.bo.AnimalVaccination;
import net.sourceforge.sheltermanager.asm.bo.AuditTrail;
import net.sourceforge.sheltermanager.asm.bo.LookupCache;
import net.sourceforge.sheltermanager.asm.globals.Global;
import net.sourceforge.sheltermanager.asm.ui.criteria.DiaryCriteria;
import net.sourceforge.sheltermanager.asm.ui.criteria.DiaryCriteriaListener;
import net.sourceforge.sheltermanager.asm.ui.ui.ASMView;
import net.sourceforge.sheltermanager.asm.ui.ui.Dialog;
import net.sourceforge.sheltermanager.asm.ui.ui.IconManager;
import net.sourceforge.sheltermanager.asm.ui.ui.SortableTableModel;
import net.sourceforge.sheltermanager.asm.ui.ui.TableData;
import net.sourceforge.sheltermanager.asm.ui.ui.TableRow;
import net.sourceforge.sheltermanager.asm.ui.ui.UI;
import net.sourceforge.sheltermanager.asm.utility.Utils;
import net.sourceforge.sheltermanager.cursorengine.CursorEngineException;
import net.sourceforge.sheltermanager.cursorengine.DBConnection;
import net.sourceforge.sheltermanager.cursorengine.SQLRecordset;

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;


/**
 * Quick editing of vaccination records.
 *
 * @author Robin Rawson-Tetley
 */
@SuppressWarnings("serial")
public class VaccinationView extends ASMView implements VaccinationParent,
    DiaryCriteriaListener {
    public int type = 0;
    public Date dateUpto = new Date();
    public Date dateFrom = new Date();
    public Date dateTo = new Date();
    public String ourtitle = "";
    private UI.Button btnComplete;
    private UI.Button btnDeleteVacc;
    private UI.Button btnEditVacc;
    private UI.Button btnOpenAnimal;
    private UI.Button btnRefresh;
    private UI.Button btnReschedule;
    private UI.CheckBox chkDeceased;
    private UI.CheckBox chkOffShelter;

    /** Creates new form EditVaccinations */
    public VaccinationView() {
        Global.mainForm.addChild(new DiaryCriteria(this,
                Global.i18n("uianimal", "Vaccination_Diary_Criteria")));
    }

    public void startForm() {
        init(Global.i18n("uianimal", "Vaccination_Book"),
            IconManager.getIcon(IconManager.SCREEN_VACCINATIONBOOK),
            "uianimal", true, true, null);
        Global.mainForm.addChild(this);
        updateList();
    }

    public void dateChosen(Date from, Date to) {
        dateFrom = from;
        dateTo = to;
        type = DiaryCriteria.BETWEEN_TWO;
        startForm();
    }

    public void normalChosen() {
        type = DiaryCriteria.UPTO_TODAY;
        startForm();
    }

    public void uptoChosen(Date upto) {
        dateUpto = upto;
        type = DiaryCriteria.UPTO_SPECIFIED;
        startForm();
    }

    public Vector<Object> getTabOrder() {
        Vector<Object> ctl = new Vector<Object>();
        ctl.add(chkDeceased);
        ctl.add(chkOffShelter);
        ctl.add(btnEditVacc);
        ctl.add(btnDeleteVacc);
        ctl.add(getTable());

        return ctl;
    }

    public Object getDefaultFocusedComponent() {
        return chkDeceased;
    }

    public boolean hasData() {
        return getTable().getRowCount() > 0;
    }

    public void setLink(int x, int y) {
    }

    public boolean formClosing() {
        return false;
    }

    public String getAuditInfo() {
        return null;
    }

    public boolean saveData() {
        return true;
    }

    public void loadData() {
    }

    public void dispose() {
        dateUpto = null;
        dateFrom = null;
        dateTo = null;
        ourtitle = null;
        super.dispose();
    }

    public void setSecurity() {
        if (!Global.currentUserObject.getSecChangeAnimalVaccination()) {
            btnEditVacc.setEnabled(false);
        }

        if (!Global.currentUserObject.getSecDeleteAnimalVaccination()) {
            btnDeleteVacc.setEnabled(false);
        }

        if (!Global.currentUserObject.getSecBulkCompleteAnimalVaccination()) {
            btnComplete.setEnabled(false);
        }
    }

    public void updateList() {
        // Spawn the updateList code on a new thread as there can be quite a
        // lot of vaccinations
        new Thread() {
                public void run() {
                    updateListThreaded();
                }
            }.start();
    }

    public synchronized void updateListThreaded() {
        boolean deceased = chkDeceased.isSelected();
        boolean offshelter = chkOffShelter.isSelected();

        String sql = "SELECT av.DateOfVaccination, av.DateRequired, a.AnimalName, " +
            "a.ShelterCode, a.ShortCode, a.ShelterLocation, a.NonShelterAnimal, " +
            "a.DeceasedDate, a.ActiveMovementID, a.ActiveMovementType, av.Comments, " +
            "vt.VaccinationType, a.ID AS AnimalID, av.ID AS AnimalVaccinationID " +
            "FROM animalvaccination av " +
            "INNER JOIN animal a ON av.AnimalID = a.ID " +
            "INNER JOIN vaccinationtype vt ON av.VaccinationID = vt.ID " +
            "WHERE ";

        switch (type) {
        case DiaryCriteria.UPTO_TODAY:
            sql += ("DateOfVaccination Is Null AND DateRequired <= '" +
            Utils.getSQLDate(Calendar.getInstance()) + "'");

            break;

        case DiaryCriteria.UPTO_SPECIFIED:
            sql += ("DateOfVaccination Is Null AND DateRequired <= '" +
            Utils.getSQLDate(dateUpto) + "'");

            break;

        case DiaryCriteria.BETWEEN_TWO:
            sql += ("DateOfVaccination Is Null AND DateRequired >= '" +
            Utils.getSQLDate(dateFrom) + "' AND DateRequired <= '" +
            Utils.getSQLDate(dateTo) + "'");

            break;
        }

        if (!deceased) {
            sql += " AND DeceasedDate Is Null";
        }

        if (!offshelter) {
            sql += " AND Archived = 0";
        }

        try {
            SQLRecordset av = new SQLRecordset(sql);

            initStatusBarMax((int) av.getRecordCount());
            setStatusText(i18n("Filling_list..."));

            int i = 0;
            TableData data = new TableData();

            while (!av.getEOF()) {
                TableRow row = new TableRow(9);

                row.set(0,
                    Animal.getAnimalCode(av.getString("ShelterCode"),
                        av.getString("ShortCode")));
                row.set(1, av.getString("AnimalName"));
                row.set(2,
                    Animal.getDisplayLocation(av.getInt("ShelterLocation"),
                        av.getInt("NonShelterAnimal"),
                        av.getInt("ActiveMovementID"),
                        av.getInt("ActiveMovementType"),
                        av.getDate("DeceasedDate")));
                row.set(3, av.getString("VaccinationType"));
                row.set(4, Utils.formatTableDate(av.getDate("DateRequired")));
                row.set(5,
                    Utils.formatTableDate(av.getDate("DateOfVaccination")));
                row.set(6, Utils.nullToEmptyString(av.getString("Comments")));
                row.set(7, av.getString("AnimalID"));
                row.set(8, av.getString("AnimalVaccinationID"));

                data.add(row);

                i++;
                av.moveNext();
                incrementStatusBar();
            }

            resetStatusBar();

            String[] columnheaders = {
                    i18n("Code"), i18n("Name"), i18n("Location"), i18n("Type"),
                    i18n("Required"), i18n("Given"), i18n("Comments")
                };
            setTableData(columnheaders, data.toTableData(), data.size(), 9, 8);
        } catch (Exception e) {
            Dialog.showError(e.getMessage());
            Global.logException(e, getClass());
        } finally {
            Global.mainForm.resetStatusBar();
            Global.mainForm.setStatusText("");
        }
    }

    public void addToolButtons() {
        btnEditVacc = UI.getButton(null,
                i18n("Edit_the_highlighted_vaccination_record"), 'e',
                IconManager.getIcon(IconManager.SCREEN_EDITVACCINATIONS_EDIT),
                UI.fp(this, "actionEdit"));
        addToolButton(btnEditVacc, true);

        btnDeleteVacc = UI.getButton(null,
                i18n("Delete_the_highlighted_vaccination_record"), 'd',
                IconManager.getIcon(IconManager.SCREEN_EDITVACCINATIONS_DELETE),
                UI.fp(this, "actionDelete"));
        addToolButton(btnDeleteVacc, true);

        btnRefresh = UI.getButton(null, i18n("Refresh_the_list"), 'r',
                IconManager.getIcon(IconManager.SCREEN_EDITVACCINATIONS_REFRESH),
                UI.fp(this, "updateList"));
        addToolButton(btnRefresh, false);

        btnOpenAnimal = UI.getButton(null,
                i18n("Open_the_animal_record_for_this_vaccination"), 'a',
                IconManager.getIcon(
                    IconManager.SCREEN_EDITVACCINATIONS_OPENANIMAL),
                UI.fp(this, "actionOpenAnimal"));
        addToolButton(btnOpenAnimal, true);

        btnComplete = UI.getButton(null,
                i18n("complete_selected_vaccinations"), 'c',
                IconManager.getIcon(
                    IconManager.SCREEN_EDITVACCINATIONS_MARKCOMPLETE),
                UI.fp(this, "actionComplete"));
        addToolButton(btnComplete, true);

        btnReschedule = UI.getButton(null,
                i18n("complete_and_reschedule_selected_vaccinations"), 'r',
                IconManager.getIcon(
                    IconManager.SCREEN_EDITVACCINATIONS_RESCHEDULE),
                UI.fp(this, "actionReschedule"));
        addToolButton(btnReschedule, true);

        UI.Panel top = getTopPanel();
        chkDeceased = (UI.CheckBox) top.add(UI.getCheckBox(i18n("include_deceased")));
        chkOffShelter = (UI.CheckBox) top.add(UI.getCheckBox(i18n("include_off_shelter")));
    }

    public void actionComplete() {
        int id = getTable().getSelectedID();

        if (id == -1) {
            return;
        }

        int[] selrows = getTable().getSelectedRows();
        SortableTableModel tablemodel = (SortableTableModel) getTable()
                                                                 .getModel();

        for (int i = 0; i < selrows.length; i++) {
            // Get the ID for the selected row
            String avID = (String) tablemodel.getValueAt(selrows[i], 8);

            String sql = "UPDATE animalvaccination SET DateOfVaccination = '" +
                Utils.getSQLDate(Calendar.getInstance()) + "' " +
                "WHERE ID = " + avID;

            // Update the onscreen value
            try {
                tablemodel.setValueAt(Utils.formatTableDate(
                        Calendar.getInstance()), selrows[i], 5);
            } catch (Exception e) {
            }

            getTable().updateRow(selrows[i]);

            try {
                DBConnection.executeAction(sql);

                if (AuditTrail.enabled()) {
                    AuditTrail.changed("animalvaccination",
                        tablemodel.getValueAt(selrows[i], 0).toString() + " " +
                        tablemodel.getValueAt(selrows[i], 1).toString() + " " +
                        tablemodel.getValueAt(selrows[i], 5).toString() + " " +
                        tablemodel.getValueAt(selrows[i], 3).toString());
                }
            } catch (Exception e) {
                Dialog.showError(e.getMessage());
                Global.logException(e, getClass());
            }
        }
    }

    public void actionReschedule() {
        int id = getTable().getSelectedID();

        if (id == -1) {
            return;
        }

        int[] selrows = getTable().getSelectedRows();
        SortableTableModel tablemodel = (SortableTableModel) getTable()
                                                                 .getModel();

        for (int i = 0; i < selrows.length; i++) {
            // Get the ID for the selected row
            String avID = (String) tablemodel.getValueAt(selrows[i], 8);

            try {
                // Complete the selected row
                AnimalVaccination av = new AnimalVaccination();
                av.openRecordset("ID = " + avID);

                if (!av.getEOF()) {
                    av.setDateOfVaccination(new Date());
                    av.save(Global.currentUserName);
                }

                if (AuditTrail.enabled()) {
                    AuditTrail.changed("animalvaccination",
                        tablemodel.getValueAt(selrows[i], 0).toString() + " " +
                        tablemodel.getValueAt(selrows[i], 1).toString() + " " +
                        tablemodel.getValueAt(selrows[i], 5).toString() + " " +
                        tablemodel.getValueAt(selrows[i], 3).toString());
                }

                // Add a new rescheduled vacc for one year ahead
                AnimalVaccination v = new AnimalVaccination();
                v.openRecordset("ID = 0");
                v.addNew();
                v.setAnimalID(av.getAnimalID());

                Calendar c = Calendar.getInstance();
                c.add(Calendar.YEAR, 1);
                v.setDateRequired(c.getTime());
                v.setVaccinationID(av.getVaccinationID());
                v.setComments(av.getComments());
                v.setCost(av.getCost());
                v.save(Global.currentUserName);

                if (AuditTrail.enabled()) {
                    AuditTrail.create("animalvaccination",
                        tablemodel.getValueAt(selrows[i], 0).toString() + " " +
                        tablemodel.getValueAt(selrows[i], 1).toString() + " " +
                        Utils.formatDate(c) + " " +
                        tablemodel.getValueAt(selrows[i], 3).toString());
                }

                // Update the list
                updateList();
            } catch (Exception e) {
                Dialog.showError(e.getMessage());
                Global.logException(e, getClass());
            }
        }
    }

    public void tableDoubleClicked() {
        actionEdit();
    }

    public void tableClicked() {
    }

    public void actionOpenAnimal() {
        int id = getTable().getSelectedID();

        if (id == -1) {
            return;
        }

        id = Integer.parseInt((String) getTable().getModel()
                                           .getValueAt(getTable()
                                                           .getSelectedRow(), 7));

        UI.cursorToWait();

        // Create a new EditAnimal screen
        AnimalEdit ea = new AnimalEdit();
        Animal animal = LookupCache.getAnimalByID(new Integer(id));

        // Kick it off into edit mode, passing the animal
        ea.openForEdit(animal);

        // Attach it to the main screen
        Global.mainForm.addChild(ea);
        ea = null;
    }

    public void actionDelete() {
        int id = getTable().getSelectedID();

        if (id == -1) {
            return;
        }

        int[] selrows = getTable().getSelectedRows();
        SortableTableModel tablemodel = (SortableTableModel) getTable()
                                                                 .getModel();

        // Make sure they are sure about this
        if (Dialog.showYesNo(UI.messageDeleteConfirm(), UI.messageReallyDelete())) {
            for (int i = 0; i < selrows.length; i++) {
                // Get the ID for the selected row
                String avID = (String) tablemodel.getValueAt(selrows[i], 8);

                String sql = "Delete From animalvaccination Where ID = " +
                    avID;

                try {
                    DBConnection.executeAction(sql);

                    if (AuditTrail.enabled()) {
                        AuditTrail.deleted("animalvaccination",
                            tablemodel.getValueAt(selrows[i], 0).toString() +
                            " " +
                            tablemodel.getValueAt(selrows[i], 1).toString() +
                            " " +
                            tablemodel.getValueAt(selrows[i], 5).toString() +
                            " " +
                            tablemodel.getValueAt(selrows[i], 3).toString());
                    }
                } catch (Exception e) {
                    Dialog.showError(UI.messageDeleteError() + e.getMessage());
                    Global.logException(e, getClass());
                }
            }

            updateList();
        }
    }

    public void actionEdit() {
        int id = getTable().getSelectedID();

        if (id == -1) {
            return;
        }

        UI.cursorToWait();

        // Create a new EditAnimalVaccination screen
        VaccinationEdit ea = new VaccinationEdit(this);

        // Kick it off into edit mode, passing the ID
        ea.openForEdit(id);

        // Attach it to the main screen
        Global.mainForm.addChild(ea);
        ea = null;
    }

    /** Tells the parent to update it's list of vaccinations - we don't do it
        on this screen because there can be so many */
    public void updateVaccinations() {
    }
}
