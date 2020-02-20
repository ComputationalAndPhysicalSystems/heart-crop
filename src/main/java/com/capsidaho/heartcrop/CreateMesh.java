package com.capsidaho.heartcrop;

import cleargl.GLVector;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import net.imagej.Dataset;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.naive.NaiveDoubleMesh;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.DefaultConvexHull3D;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.util.List;

@Plugin(type = Command.class, label = "Heart Crop - CreateMesh")
public class CreateMesh implements Command {
    @Parameter
    private LogService logService;

    @Parameter
    private OpService opService;

    @Parameter
    private Dataset img;

    @Parameter
    private float[] resolution;

    @Parameter(type = ItemIO.OUTPUT)
    private Mesh mesh;

    private RoiManager roiManager;

    @Override
    public void run() {
        roiManager = RoiManager.getRoiManager();

        /* Create a ConvexHulls of controlPoints */
        mesh = new NaiveDoubleMesh();

        resolution[0] = 1;
        resolution[1] = 1;
        resolution[2] = 1;

        logService.info("Populating point set");
        for(Roi r : roiManager.getRoisAsArray() ) {
            if( r instanceof PointRoi) {
                Point p = r.iterator().next();
                float sf = 0.01f;
                float x = (float) (p.x * resolution[0]);
                float y = (float) (p.y * resolution[1]);
                float z = (float) (((PointRoi) r).getPointPosition(0) * resolution[2]);
                mesh.vertices().add(x, y, z);

                //System.out.println("V: " + x + " " + y + " " + z);
            }
        }

        logService.info("Generating mesh");

        final List<?> result = (List<?>) opService.run(DefaultConvexHull3D.class, mesh );
        mesh = (Mesh) result.get(0);
    }

    public LogService getLogService() {
        return logService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

    public OpService getOpService() {
        return opService;
    }

    public void setOpService(OpService opService) {
        this.opService = opService;
    }

    public Dataset getImg() {
        return img;
    }

    public void setImg(Dataset img) {
        this.img = img;
    }

    public float[] getResolution() {
        return resolution;
    }

    public void setResolution(float[] resolution) {
        this.resolution = resolution;
    }

    public Mesh getMesh() {
        return mesh;
    }

    public void setMesh(Mesh mesh) {
        this.mesh = mesh;
    }
}
