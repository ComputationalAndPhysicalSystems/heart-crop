/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2018 SciView developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package com.capsidaho.heartcrop;

import graphics.scenery.Node;
import graphics.scenery.volumes.Volume;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import net.imagej.Dataset;
import net.imagej.mesh.Mesh;
import net.imagej.ops.OpService;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.command.InteractiveCommand;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.display.event.input.InputEvent;
import org.scijava.display.event.input.KyEvent;
import org.scijava.display.event.input.KyReleasedEvent;
import org.scijava.event.EventHandler;
import org.scijava.event.SciJavaEvent;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.DefaultIntTable;
import org.scijava.table.Table;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import sc.iview.SciView;
import sc.iview.SciViewService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static sc.iview.commands.MenuWeights.DEMO;

/**
 * 3D Crop using the IJ1 RoiManager
 *
 * TODO:
 * - Make this interactive command into a metacommand that runs other commands
 * - show in sciview
 * - generate mesh
 * - crop
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, label = "Interactive 3D Crop",
        menu = { @Menu(label = "Image"), //
                 @Menu(label = "Interactive 3D Crop") })
public class HeartCropMenu extends InteractiveCommand {

    @Parameter
    private DisplayService dService;

    @Parameter
    private UIService uiService;

    @Parameter
    private IOService ioService;

    @Parameter
    private OpService opService;

    @Parameter
    private LogService logService;

    @Parameter
    private SciViewService sciViewService;

    @Parameter
    private CommandService command;

    @Parameter
    private ThreadService thread;

    @Parameter
    private File imageFile;

    @Parameter(callback = "openImage")
    private Button openImage;

    @Parameter(callback = "savePoints")
    private Button savePoints;

    @Parameter(callback = "loadPoints")
    private Button loadPoints;

    @Parameter(callback = "createMesh")
    private Button createMesh;

    @Parameter(callback = "visualize")
    private Button visualize;

    @Parameter(callback = "generateMask")
    private Button generateMask;

    @Parameter(callback = "crop")
    private Button crop;

    // We will handle opening our own dataset
    private Dataset img = null;

    private RoiManager roiManager;
    private Dataset croppedImg = null;
    private Volume volume = null;
    private HashMap<PointRoi, Node> scPoints;
    private float[] resolution = new float[]{(float) 1, (float)1, 1f};
    private Mesh currentMesh = null;
    private Node currentMeshNode = null;

    private Mesh cropMesh = null;
    private Display<?> display = null;
    private ImagePlus imp = null;
    private Table table;
    private Display<Table> tableDisplay = null;


    @Override
    public void initialize() {
        roiManager = RoiManager.getRoiManager();

        IJ.setTool("point");

        // Tutorial message
        //JOptionPane.showMessageDialog(null, "Select points to use in crop by clicking at the x,y,z location\nPress t to add each point (pointROI has been preselected).\nWhen complete createMesh");

        scPoints = new HashMap<>();
    }

    public void savePoints() {
        HashMap<String, Object> argmap = new HashMap<>();

        argmap.put("table", table);

        command.run(SavePoints.class, true, argmap);
    }

    public void loadPoints() {
        HashMap<String, Object> argmap = new HashMap<>();

        Future<CommandModule> res = command.run(OpenPoints.class, true, argmap);

        System.out.println("Waiting for result");
        thread.run(() -> {
            try {
                table = (Table) res.get().getOutput("table");

                System.out.println("Table is: " + table);

                tableDisplay = (Display<Table>) dService.getDisplay("table");

                //tableDisplay = (Display<Table>) dService.createDisplay("loadedTable", table);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });

//        System.out.println("waiting for res");
//        while( !res.isCancelled() && !res.isDone() ) {
//            try {
//                Thread.sleep(20);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//                return;
//            }
//        }
//        System.out.println("done waiting for res");
//
//        try {
//            table = (Table) res.get().getOutput("table");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }
    }

    public void openImage() throws IOException {
        img = (Dataset) ioService.open( imageFile.getAbsolutePath() );

        display = dService.createDisplay( imageFile.getName(), img );

        imp = WindowManager.getImage(imageFile.getName());

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
        int x = ((PointRoi) imp.getRoi()).getContainedPoints()[0].x;
        int y = ((PointRoi) imp.getRoi()).getContainedPoints()[0].y;
        int z = imp.getZ();


        System.out.println("x " + x + " y " + y + " z " + z + " roi " + imp.getRoi());

        String tableName = imageFile.getName() + "_points";

        if( table == null ) {
            table = new DefaultIntTable();
            table.appendColumn("x");
            table.appendColumn("y");
            table.appendColumn("z");
            //uiService.show(tableName, table);
            tableDisplay = (Display<Table>) dService.createDisplay(tableName, table);
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

    // I expect IJ2 to work like this
//    public void createMesh() {
//        Map<String, Object> argmap = new HashMap<>();
//        argmap.put("img", img);
//        argmap.put("resolution", resolution);
//        Future<CommandModule> future = command.run(CreateMesh.class, false, argmap);
//
//        CommandModule result = null;
//        try {
//            result = future.get(5, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        } catch (TimeoutException e) {
//            e.printStackTrace();
//        }
//
//        cropMesh = (Mesh) result.getOutput("mesh");
//        System.out.println("Mesh created: " + cropMesh);
//    }

    public void createMesh() {
        CreateMesh cm = new CreateMesh();

        float[] resolution = new float[img.numDimensions()];
        for( int d = 0; d < resolution.length; d++ ) {
            resolution[d] = (float) img.axis(d).averageScale(0, 1);
        }

        cm.setImg(img);
        cm.setResolution(resolution);
        cm.setLogService(logService);
        cm.setOpService(opService);
        cm.setTable(table);

        cm.run();

        cropMesh = cm.getMesh();
        System.out.println("Mesh created: " + cropMesh);
    }

    public void generateMask() {
        if( cropMesh == null )  {
            JOptionPane.showMessageDialog(null, "Mesh has not been computed, cannot crop. Run CreateMesh");
            return;
        }
        GenerateMask g = new GenerateMask();

        g.setImg(img);
        g.setMesh(cropMesh);
        g.setLogService(logService);
        g.setOpService(opService);

        g.run();
        System.out.println("Generate mask complete");
    }

    public void crop() {
        if( cropMesh == null )  {
            JOptionPane.showMessageDialog(null, "Mesh has not been computed, cannot crop. Run CreateMesh");
            return;
        }
        Crop c = new Crop();

        c.setImg(img);
        c.setMesh(cropMesh);
        c.setLogService(logService);

        c.run();
        System.out.println("Crop complete");
    }

    public void visualize() {

//        JOptionPane.showMessageDialog(null, "Visualization temporarily disabled");
//        return;

        Visualize v = new Visualize();

        v.setImg(img);
        v.setMesh(currentMesh);
        v.setResolution(resolution);
        v.setSciViewService(sciViewService);

        v.run();
    }

    @EventHandler
    protected void onInput(InputEvent event) {
        System.out.println("Caught event");
        if( event instanceof KyReleasedEvent ) {
            KyReleasedEvent kre = (KyReleasedEvent) event;
            if( kre.getCharacter() == 't' ) {
                ImagePlus imp = WindowManager.getImage(imageFile.getName());
                System.out.println("Caught event 't': " + imp);
            }
        }
    }

    @EventHandler
    protected void onEvent(SciJavaEvent event) {
        System.out.println("Caught event " + event);
        if( event instanceof KyReleasedEvent ) {
            KyReleasedEvent kre = (KyReleasedEvent) event;
            if( kre.getCharacter() == 't' ) {
                ImagePlus imp = WindowManager.getImage(imageFile.getName());
                System.out.println("Caught event 't': " + imp);
            }
        }
    }

    @Override
    public void cancel() {
        // Clear up variables?
    }

}
