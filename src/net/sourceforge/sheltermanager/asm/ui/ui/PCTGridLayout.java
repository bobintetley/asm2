/*
   SwingWT
   Copyright(c)2003-2009, R. Rawson-Tetley

   For more information on distributing and using this program, please
   see the accompanying "COPYING" file.

   Contact me by electronic mail: bobintetley@users.sourceforge.net


 */
package net.sourceforge.sheltermanager.asm.ui.ui;

import java.awt.*;


public class PCTGridLayout implements LayoutManager {
    int hgap;
    int vgap;
    int rows;
    int cols;
    int[] pctwidths = new int[0];

    public PCTGridLayout() {
        this(1, 0, 0, 0);
    }

    public PCTGridLayout(int rows, int cols) {
        this(rows, cols, 0, 0);
    }

    public PCTGridLayout(int rows, int cols, int hgap, int vgap) {
        if ((rows == 0) && (cols == 0)) {
            throw new IllegalArgumentException("zero size");
        }

        this.cols = cols;
        this.rows = rows;
        this.hgap = hgap;
        this.vgap = vgap;
    }

    /**
    * Allows specifying of an array of widths in percentages - these will be
    * used to set the column widths when laying out the container
    */
    public PCTGridLayout(int rows, int cols, int hgap, int vgap, int[] pctwidths) {
        if ((rows == 0) && (cols == 0)) {
            throw new IllegalArgumentException("zero size");
        }

        this.cols = cols;
        this.rows = rows;
        this.hgap = hgap;
        this.vgap = vgap;
        this.pctwidths = pctwidths;

        if (pctwidths.length != cols) {
            throw new IllegalArgumentException("cols != number of widths given");
        }
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        if ((rows == 0) && (this.cols == 0)) {
            throw new IllegalArgumentException("zero size");
        }

        this.rows = rows;
    }

    public int getColumns() {
        return cols;
    }

    public void setColumns(int cols) {
        if ((cols == 0) && (this.rows == 0)) {
            throw new IllegalArgumentException("zero size");
        }

        this.cols = cols;
    }

    public int getHgap() {
        return hgap;
    }

    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    public int getVgap() {
        return vgap;
    }

    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    public void addLayoutComponent(String name, Component comp) {
    }

    public void removeLayoutComponent(Component comp) {
    }

    public Dimension preferredLayoutSize(Container parent) {
        Insets insets = parent.getInsets();
        int ncomponents = parent.getComponentCount();
        int nrows = rows;
        int ncols = cols;

        if (nrows > 0) {
            ncols = ((ncomponents + nrows) - 1) / nrows;
        } else {
            nrows = ((ncomponents + ncols) - 1) / ncols;
        }

        int w = 0;
        int h = 0;

        for (int i = 0; i < ncomponents; i++) {
            Component comp = parent.getComponent(i);
            Dimension d = comp.getPreferredSize();

            if (w < d.width) {
                w = d.width;
            }

            if (h < d.height) {
                h = d.height;
            }
        }

        Dimension d = new Dimension(insets.left + insets.right + (ncols * w) +
                ((ncols - 1) * hgap),
                insets.top + insets.bottom + (nrows * h) +
                ((nrows - 1) * vgap));

        return d;
    }

    public Dimension minimumLayoutSize(Container parent) {
        Insets insets = parent.getInsets();
        int ncomponents = parent.getComponentCount();
        int nrows = rows;
        int ncols = cols;

        if (nrows > 0) {
            ncols = ((ncomponents + nrows) - 1) / nrows;
        } else {
            nrows = ((ncomponents + ncols) - 1) / ncols;
        }

        int w = 0;
        int h = 0;

        for (int i = 0; i < ncomponents; i++) {
            Component comp = parent.getComponent(i);
            Dimension d = comp.getMinimumSize();

            if (w < d.width) {
                w = d.width;
            }

            if (h < d.height) {
                h = d.height;
            }
        }

        return new Dimension(insets.left + insets.right + (ncols * w) +
            ((ncols - 1) * hgap),
            insets.top + insets.bottom + (nrows * h) + ((nrows - 1) * vgap));
    }

    public void layoutContainer(Container parent) {
        Insets insets = parent.getInsets();
        int ncomponents = parent.getComponentCount();
        int nrows = rows;
        int ncols = cols;
        boolean ltr = parent.getComponentOrientation().isLeftToRight();
        boolean usepct = pctwidths.length > 0;

        if (ncomponents == 0) {
            return;
        }

        if (nrows > 0) {
            ncols = ((ncomponents + nrows) - 1) / nrows;
        } else {
            nrows = ((ncomponents + ncols) - 1) / ncols;
        }

        int tw = parent.getWidth() - (insets.left + insets.right);
        int th = parent.getHeight() - (insets.top + insets.bottom);
        int w = (tw - ((ncols - 1) * hgap)) / ncols;
        int h = (th - ((nrows - 1) * vgap)) / nrows;

        if (usepct) {
            int[] colwidths = new int[pctwidths.length];

            for (int c = 0; c < ncols; c++) {
                double d = (double) tw;
                d -= (hgap * (ncols - 1));
                d = (d / 100F) * (double) pctwidths[c];
                colwidths[c] = (int) d;
            }

            for (int c = 0, x = insets.left; c < ncols;
                    c++, x += (colwidths[c - 1] + hgap)) {
                for (int r = 0, y = insets.top; r < nrows;
                        r++, y += (h + vgap)) {
                    int i = (r * ncols) + c;
                    w = colwidths[c];

                    if (i < ncomponents) {
                        parent.getComponent(i).setBounds(x, y, w, h);
                    }
                }
            }
        } else if (ltr) {
            for (int c = 0, x = insets.left; c < ncols; c++, x += (w + hgap)) {
                for (int r = 0, y = insets.top; r < nrows;
                        r++, y += (h + vgap)) {
                    int i = (r * ncols) + c;

                    if (i < ncomponents) {
                        parent.getComponent(i).setBounds(x, y, w, h);
                    }
                }
            }
        } else {
            for (int c = 0, x = parent.getWidth() - insets.right - w;
                    c < ncols; c++, x -= (w + hgap)) {
                for (int r = 0, y = insets.top; r < nrows;
                        r++, y += (h + vgap)) {
                    int i = (r * ncols) + c;

                    if (i < ncomponents) {
                        parent.getComponent(i).setBounds(x, y, w, h);
                    }
                }
            }
        }
    }
}
