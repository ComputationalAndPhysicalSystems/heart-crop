package com.capsidaho.heartcrop;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PointRoi;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.naive.NaiveDoubleMesh;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.DefaultConvexHull3D;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.type.numeric.RealType;
import org.python.core.imp;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.DefaultIntTable;
import org.scijava.table.Table;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Plugin(type = Command.class, label = "Heart Crop - Crop Image",
        menu = { @Menu(label = "Plugins"), //
                 @Menu(label = "Interactive 3D Crop"),
                 @Menu(label = "Open Image") })
public class OpenImage implements Command {
    @Parameter
    private LogService logService;

    @Parameter
    private OpService opService;

    @Parameter
    private DisplayService displayService;

    @Parameter
    private IOService ioService;

    @Parameter(required = false)
    private Table table;

    @Parameter(style = "file")
    private File imageFile;

    private ImagePlus imp;
    private Display<Table> tableDisplay;

    @Override
    public void run() {

        imp = IJ.openImage(imageFile.getAbsolutePath());
        imp.show();

        IJ.setTool("point");

        KeyListener keyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                if (keyEvent.getKeyChar() == 'q') {
                    //System.out.println("Caught event 'q': " + imp);
                    addCurrentPoint();
                }
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {

            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {

            }
        };

        imp.getCanvas().addKeyListener(keyListener);
        imp.getWindow().addKeyListener(keyListener);
    }

    private void addCurrentPoint() {
        if( imp.getRoi() == null || !(imp.getRoi() instanceof  PointRoi) )
            return;

        int x = ((PointRoi) imp.getRoi()).getContainedPoints()[0].x;
        int y = ((PointRoi) imp.getRoi()).getContainedPoints()[0].y;
        int z = imp.getZ();


        String tableName = "interactive3DCrop";

        tableDisplay = (Display<Table>) displayService.getDisplay(tableName);
        if( tableDisplay != null ) {
            table = tableDisplay.get(0);
        }

        if( table == null ) {
            table = new DefaultIntTable();
            table.appendColumn("x");
            table.appendColumn("y");
            table.appendColumn("z");
            //uiService.show(tableName, table);
            tableDisplay = (Display<Table>) displayService.createDisplay(tableName, table);
            //uiService.show(table);
        }

        int rc = table.getRowCount();

        table.appendRow();
        table.set(0, rc, x);
        table.set(1, rc, y);
        table.set(2, rc, z);

        tableDisplay.update();
        //uiService.getUI(tableName).show(table);
    }
}
