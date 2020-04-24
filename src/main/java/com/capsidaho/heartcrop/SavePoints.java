package com.capsidaho.heartcrop;

import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import net.imagej.Dataset;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.naive.NaiveDoubleMesh;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.DefaultConvexHull3D;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Menu;
import org.scijava.table.Table;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Plugin(type = Command.class, label = "Heart Crop - SavePoints",
        menu = { @Menu(label = "Plugins"), //
                 @Menu(label = "Interactive 3D Crop"),
                 @Menu(label = "Save Crop Points") })
public class SavePoints implements Command {
    @Parameter
    private LogService logService;

    @Parameter
    private OpService opService;

    @Parameter
    private DisplayService displayService;

    @Parameter(required = false)
    private Table table = null;

    @Parameter(style="save")
    private File file = null;

    @Override
    public void run() {
        BufferedWriter w;

        if( table == null ) {
            String tableName = "interactive3DCrop";

            Display<Table> tableDisplay = (Display<Table>) displayService.getDisplay(tableName);
            if( tableDisplay != null ) {
                table = tableDisplay.get(0);
            }
        }

        if( table == null ) {
            logService.error("Cannot find table");
            return;
        }

        try {
            w = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


        int rc = table.getRowCount();
        int cc = table.getColumnCount();
        try {
            for (int k = 0; k < rc; k++) {
                for (int i = 0; i < cc; i++) {
                    if (i > 0)
                        w.write(", ");
                    w.write("" + table.get(i, k));
                }
                w.write("\n");
            }
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
