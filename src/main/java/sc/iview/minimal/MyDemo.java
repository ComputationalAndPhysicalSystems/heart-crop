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
package sc.iview.minimal;

import cleargl.GLVector;
import edu.mines.jtk.ogl.Gl;
import graphics.scenery.Node;
import graphics.scenery.Sphere;
import graphics.scenery.volumes.Volume;
import ij.IJ;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import net.imagej.Dataset;
import org.scijava.command.Command;
import org.scijava.io.IOService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.util.Colors;
import sc.iview.SciView;
import sc.iview.vector.ClearGLVector3;
import sc.iview.vector.Vector3;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;

import static sc.iview.commands.MenuWeights.DEMO;
import static sc.iview.commands.MenuWeights.DEMO_LINES;

/**
 * 3D Crop using the IJ1 RoiManager
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, label = "My Demo", menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "My Demo") })
public class MyDemo implements Command {

    @Parameter
    private SciView sciView;

    @Parameter
    private UIService uiService;

    @Parameter
    private IOService ioService;

    private RoiManager roiManager;
    private Dataset img = null;
    private Volume volume = null;
    private HashMap<PointRoi, Node> scPoints;
    private float[] resolution = new float[]{(float) 0.6500002, (float) 0.6500002, 2f};

    @Override
    public void run() {

        String filename = "/home/kharrington/Data/Anjalie/C1-fish4_z_stack_red.tif";

        try {
            img = (Dataset) ioService.open(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }

        uiService.show(img);

        roiManager = new RoiManager();
        IJ.setTool("point");

        volume = (Volume) sciView.addVolume(img, resolution);
        volume.setPixelToWorldRatio(0.1f);

        scPoints = new HashMap<>();

        sciView.animate(1, this::syncRoiManager );

    }

    /*
     * Sync the ROI manager to the volume with sphere annotations for points
     */
    public void syncRoiManager() {
        //System.out.println("Syncing ROIManager");
        // Remove previous children
        for( Node n : volume.getChildren() ) {
            volume.removeChild(n);
        }

        // Add new children for each point ROI
        for(Roi r : roiManager.getRoisAsArray() ) {
            if( r instanceof PointRoi ) {
                Sphere s = new Sphere(0.02f, 10);
                Point p = r.iterator().next();
                float sf = 0.01f;
                float x = ( p.x - volume.getSizeX()/2 ) * resolution[0] * volume.getPixelToWorldRatio() * sf;
                float y = ( p.y - volume.getSizeY()/2 ) * resolution[1] * volume.getPixelToWorldRatio() * sf;
                float z = ( ((PointRoi) r).getPointPosition(0) - volume.getSizeZ()/2 ) * resolution[2] * volume.getPixelToWorldRatio() * sf;
                s.setPosition(new GLVector(x,y,z));
                //s.setParent(volume);
                sciView.addNode(s,false);
            }
        }
    }

}
