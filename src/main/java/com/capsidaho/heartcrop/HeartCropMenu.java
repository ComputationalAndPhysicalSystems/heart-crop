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
import ij.gui.PointRoi;
import ij.plugin.frame.RoiManager;
import net.imagej.Dataset;
import net.imagej.mesh.Mesh;
import net.imagej.ops.OpService;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.command.InteractiveCommand;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import sc.iview.SciView;
import sc.iview.SciViewService;

import javax.swing.*;
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

    @Parameter(callback = "createMesh")
    private Button createMesh;

    @Parameter(callback = "visualize")
    private Button visualize;

    @Parameter
    private Dataset img;

    @Parameter(callback = "crop")
    private Button crop;

    private RoiManager roiManager;
    private Dataset croppedImg = null;
    private Volume volume = null;
    private HashMap<PointRoi, Node> scPoints;
    private float[] resolution = new float[]{(float) 0.6500002, (float) 0.6500002, 2f};
    private Mesh currentMesh = null;
    private Node currentMeshNode = null;

    private Mesh cropMesh = null;

    @Override
    public void initialize() {
        roiManager = RoiManager.getRoiManager();

        float[] resolution = new float[img.numDimensions()];
        for( int d = 0; d < resolution.length; d++ ) {
            resolution[d] = (float) img.axis(d).averageScale(0, 1);
        }

        scPoints = new HashMap<>();
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

        cm.setImg(img);
        cm.setResolution(resolution);
        cm.setLogService(logService);
        cm.setOpService(opService);

        cm.run();

        cropMesh = cm.getMesh();
        System.out.println("Mesh created: " + cropMesh);
    }

    public void crop() {
        // TODO check and warn if mesh isn't computed
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

        JOptionPane.showMessageDialog(null, "Visualization temporarily disabled");
        return;

//        Visualize v = new Visualize();
//
//        v.setImg(img);
//        v.setMesh(currentMesh);
//        v.setResolution(resolution);
//        v.setSciViewService(sciViewService);
//
//        v.run();
    }

    @Override
    public void cancel() {
        // Clear up variables?
    }

}
