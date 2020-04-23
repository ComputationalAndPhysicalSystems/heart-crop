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
import sc.iview.SciView;
import sc.iview.SciViewService;

import java.awt.*;

@Plugin(type = Command.class, label = "Heart Crop - Crop")
public class Visualize implements Command {
    @Parameter
    private SciViewService sciViewService;

    @Parameter
    private Dataset img;

    @Parameter
    private Mesh mesh;

    @Parameter
    private float[] resolution;

    private RoiManager roiManager;
    private Volume volume = null;
    //private float[] resolution = new float[]{(float) 0.6500002, (float) 0.6500002, 2f};

    /*
     * Sync the ROI manager to the volume with sphere annotations for points
     */
    public void syncRoiManager() {
        // Add new children for each point ROI
        for(Roi r : roiManager.getRoisAsArray() ) {
            if( r instanceof PointRoi) {
                Sphere s = new Sphere(0.02f, 10);
                Point p = r.iterator().next();
                float sf = 0.01f;
                float x = (float) (( p.x - volume.getScale().x()/2 ) * resolution[0] * volume.getPixelToWorldRatio() * sf);
                float y = (float) (( p.y - volume.getScale().y()/2 ) * resolution[1] * volume.getPixelToWorldRatio() * sf);
                float z = (float) (( ((PointRoi) r).getPointPosition(0) - volume.getScale().z()/2 ) * resolution[2] * volume.getPixelToWorldRatio() * sf);
                s.setPosition(new Vector3f(x,y,z));
                //s.setParent(volume);
                //sciView.addNode(s,false);
            }
        }
    }

    @Override
    public void run() {
        SciView sciView = null;
        try {
            sciView = sciViewService.getOrCreateActiveSciView();
        } catch (Exception e) {
            e.printStackTrace();
        }

        roiManager = RoiManager.getRoiManager();

        RandomAccessibleInterval<RealType> frame = Views.hyperSlice((RandomAccessibleInterval) img, 3, 0);

        sciView.waitForSceneInitialisation();

        //sciView.animate(1, this::syncRoiManager );
        sciView.addVolume(frame, "img", (float) resolution[0], (float) resolution[1], (float) resolution[2]);

        syncRoiManager();

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
