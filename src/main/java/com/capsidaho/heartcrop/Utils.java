package com.capsidaho.heartcrop;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import org.scijava.table.Table;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static List<RealLocalizable> fromTable(Table table) {
        List<RealLocalizable> l = new ArrayList<>();

        int rc = table.getRowCount();
        int cc = table.getColumnCount();

        for (int k = 0; k < rc; k++) {
            RealPoint p = new RealPoint(cc);
            for (int i = 0; i < cc; i++) {
                p.setPosition((int)table.get(i, k), i);
            }
            l.add(p);
        }

        return l;
    }
}
