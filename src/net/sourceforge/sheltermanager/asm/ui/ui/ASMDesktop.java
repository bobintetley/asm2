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
package net.sourceforge.sheltermanager.asm.ui.ui;

import net.sourceforge.sheltermanager.asm.globals.Global;

import java.awt.*;
import java.awt.event.*;

import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


/**
 * Tabbed pane that allows close buttons
 */
public class ASMDesktop extends JPanel {
    JPanel tabs = null;
    JPanel viewer = null;
    CardLayout viewlayout = null;
    Vector forms = new Vector();
    ASMForm currentview = null;

    public ASMDesktop() {
        super();
        setLayout(new BorderLayout());
        init();
    }

    public void init() {
        tabs = new JPanel();
        tabs.setLayout(new GridLayout(1, 0, 2, 2));
        viewer = new JPanel();
        viewlayout = new CardLayout();
        viewer.setLayout(viewlayout);
        viewer.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        add(tabs, BorderLayout.NORTH);
        add(viewer, BorderLayout.CENTER);
    }

    public void add(final ASMForm f) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // Create a tab and associate the form with us as the parent
                    ASMTab t = new ASMTab(f.getTitle(), f.getTitle(),
                            f.getFrameIcon(), f, ASMDesktop.this);
                    f.setASMDesktop(ASMDesktop.this);

                    // Add the tab header to the screen and keep it in our list
                    tabs.add(t);
                    forms.add(t);

                    // Show the form in the viewer and notify it as loaded
                    if (f.needsScroll()) {
                        int width = getWidth() - 20;
                        f.setMaximumSize(new Dimension(width,
                                f.getScrollHeight()));
                        f.setPreferredSize(new Dimension(width,
                                f.getScrollHeight()));
                        f.setMinimumSize(new Dimension(width,
                                f.getScrollHeight()));

                        JScrollPane jsr = new JScrollPane(f,
                                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                        f.setScroller(jsr);
                        viewer.add(jsr, f.toString());
                    } else {
                        viewer.add(f, f.toString());
                    }

                    select(f);
                }
            });
    }

    /** Returns the currently displaying form */
    public ASMForm getSelectedFrame() {
        return currentview;
    }

    protected void newTabSelected(ASMForm f) {
        // Overridden in child classes
    }

    /** Remove a tab - we get called by ASMForm.dispose() */
    public void close(final ASMForm f) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ASMTab t = getTab(f);
                    tabs.remove(t);
                    forms.remove(t);

                    if (f.needsScroll()) {
                        viewer.remove(f.getScroller());
                        f.getScroller().remove(f);
                    } else {
                        viewer.remove(f);
                    }

                    f.parent = null;
                    f.tab = null;
                    t.form = null;
                    t.desktop = null;
                    currentview = null;

                    // Get the next available form and select it
                    if (forms.size() > 0) {
                        t = (ASMTab) forms.get(forms.size() - 1);
                        select(t.form);
                    }

                    ASMDesktop.this.revalidate();
                }
            });
    }

    /** Returns the tab object for a given ASMForm */
    public ASMTab getTab(ASMForm f) {
        for (int i = 0; i < forms.size(); i++) {
            ASMTab t = (ASMTab) forms.get(i);

            if (t.form.equals(f)) {
                return t;
            }
        }

        return null;
    }

    public void select(ASMForm f) {
        // Update tab button to make it active
        for (int i = 0; i < forms.size(); i++) {
            ASMTab t = (ASMTab) forms.get(i);

            if (t.form.equals(f)) {
                t.setActive(true);
            } else {
                t.setActive(false);
            }
        }

        // Make sure it's visible
        viewlayout.show(viewer, f.toString());
        currentview = f;
        newTabSelected(f);
        revalidate();
    }
}


class ASMTab extends JPanel implements MouseListener {
    String title = "";
    String tooltip = "";
    Icon icon = null;
    JLabel label = null;
    JLabel iconlabel = null;
    JLabel button = null;
    ASMForm form = null;
    ASMDesktop desktop = null;
    Color background = null;
    Font plain = null;
    Font bold = null;
    Border raisedbevel = BorderFactory.createRaisedBevelBorder();
    Border loweredbevel = BorderFactory.createLoweredBevelBorder();

    public ASMTab(String title, String tooltip, Icon icon, ASMForm form,
        ASMDesktop desktop) {
        super();

        this.background = getBackground();

        // Some themes may already have white as the background,
        // so use a light gray instead
        if (this.background == Color.WHITE) {
            this.background = new Color(240, 240, 240);
        }

        this.plain = getFont().deriveFont(Font.PLAIN);
        this.bold = getFont().deriveFont(Font.BOLD);
        this.title = title;
        this.tooltip = tooltip;
        this.icon = icon;
        this.form = form;
        this.desktop = desktop;
        form.tab = this;
        init();
    }

    public void init() {
        setLayout(new BorderLayout());

        label = new JLabel(title);
        label.addMouseListener(this);
        label.setToolTipText(tooltip);
        iconlabel = new JLabel(icon);
        iconlabel.addMouseListener(this);
        iconlabel.setToolTipText(tooltip);
        button = new JLabel(IconManager.getIcon(IconManager.CLOSE));
        button.setToolTipText(Global.i18n("uianimal", "Close"));
        button.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (!form.formClosing()) {
                        desktop.close(form);
                    }
                }

                public void mouseEntered(MouseEvent e) {
                    button.setIcon(IconManager.getIcon(IconManager.CLOSE_HILITE));
                }

                public void mouseExited(MouseEvent e) {
                    button.setIcon(IconManager.getIcon(IconManager.CLOSE));
                }
            });

        add(iconlabel, BorderLayout.WEST);
        add(label, BorderLayout.CENTER);
        add(button, BorderLayout.EAST);
    }

    public void setTitle(String s) {
        label.setText(s);
    }

    public void setActive(boolean b) {
        if (b) {
            setBackground(Color.WHITE);
            label.setFont(bold);
            setBorder(raisedbevel);
        } else {
            setBackground(background);
            label.setFont(plain);
            setBorder(loweredbevel);
        }
    }

    public void mouseClicked(MouseEvent e) {
        desktop.select(form);
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }
}