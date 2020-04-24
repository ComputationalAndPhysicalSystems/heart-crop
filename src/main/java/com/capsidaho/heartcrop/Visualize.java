package com.capsidaho.heartcrop;

import cleargl.GLVector;
import graphics.scenery.Sphere;
import graphics.scenery.volumes.Volume;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import net.imagej.Dataset;
import net.imagej.mesh.Mesh;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Menu;
import sc.iview.SciView;
import sc.iview.SciViewService;

import java.awt.*;

@Plugin(type = Command.class, label = "Heart Crop - Visualize",
        menu = { @Menu(label = "Plugins"), //
                 @Menu(label = "Interactive 3D Crop"),
                 @Menu(label = "Visualize") })
public class Visualize implements Command {
    @Parameter
    private SciViewService sciViewService;

    @Parameter
    private Dataset img;

    @Parameter
    private Mesh mesh;

    @Parameter
    private float[] resolution;

    private Volume volume = null;
    //private float[] resolution = new float[]{(float) 0.6500002, (float) 0.6500002, 2f};


    @Override
    public void run() {
        SciView sciView = null;
        try {
            sciView = sciViewService.getOrCreateActiveSciView();
        } catch (Exception e) {
            e.printStackTrace();
        }

        RandomAccessibleInterval<RealType> frame = Views.hyperSlice((RandomAccessibleInterval) img, 3, 0);

        sciView.waitForSceneInitialisation();

        sciView.addVolume(frame, "img", (float) resolution[0], (float) resolution[1], (float) resolution[2]);
    }

    public Dataset getImg() {
        return img;
    }

    public void setImg(Dataset img) {
        this.img = img;
    }

    public Mesh getMesh() {
        return mesh;
    }

    public void setMesh(Mesh mesh) {
        this.mesh = mesh;
    }

    public float[] getResolution() {
        return resolution;
    }

    public void setResolution(float[] resolution) {
        this.resolution = resolution;
    }

    public void setSciViewService(SciViewService sciViewService) {
        this.sciViewService = sciViewService;
    }
}
