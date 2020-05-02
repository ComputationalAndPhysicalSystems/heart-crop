package com.capsidaho.heartcrop;

import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.RoiDecoder;
import ij.plugin.frame.RoiManager;
import net.imagej.ops.OpService;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.DefaultIntTable;
import org.scijava.table.Table;
import org.scijava.ui.UIService;

import java.awt.*;
import java.io.*;

@Plugin(type = Command.class, label = "Heart Crop - OpenPoints",
        menu = { @Menu(label = "Plugins"), //
                 @Menu(label = "Interactive 3D Crop"),
                 @Menu(label = "0 Import ROIs as Crop Points") })
public class ImportRoisAsPoints implements Command {
    @Parameter
    private LogService logService;

    @Parameter
    private OpService opService;

    @Parameter
    private UIService uiService;

    @Parameter
    private DisplayService displayService;

    @Parameter(style="file")
    private File file = null;

    @Parameter(type = ItemIO.OUTPUT)
    private Table table = null;

    @Override
    public void run() {

        // TODO read ROIs from file
        RoiManager roiManager = RoiManager.getRoiManager();

        roiManager.runCommand("Open", file.getAbsolutePath());

        table = new DefaultIntTable();
        table.appendColumn("x");
        table.appendColumn("y");
        table.appendColumn("z");

        for( Roi roi : roiManager.getRoisAsArray() ) {

            int rc = table.getRowCount();
            PointRoi pointRoi = (PointRoi) roi;

            Point pt = pointRoi.getContainedPoints()[0];
            int x = pt.x;
            int y = pt.y;
            int z = pointRoi.getZPosition();

            table.appendRow();
            table.set(0, rc, x);
            table.set(1, rc, y);
            table.set(2, rc, z);
        }

        String tableName = "interactive3DCrop";

        Display<?> display = displayService.createDisplay(tableName, table);

    }

}
