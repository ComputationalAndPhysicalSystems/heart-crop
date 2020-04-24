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
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.type.numeric.RealType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Menu;
import org.scijava.table.Table;

import java.awt.*;
import java.util.List;

@Plugin(type = Command.class, label = "Heart Crop - CreateMesh",
        menu = { @Menu(label = "Plugins"), //
                 @Menu(label = "Interactive 3D Crop"),
                 @Menu(label = "Create Mesh") })
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

    @Parameter
    private Table table;

    @Override
    public void run() {

        /* Create a ConvexHulls of controlPoints */
        mesh = new NaiveDoubleMesh();

//        resolution[0] = 1;
//        resolution[1] = 1;
//        resolution[2] = 1;

        long numChannels = img.dimension(2);
        long numZ = img.dimension(3);
        long numTime = img.dimension(4);

        List<RealLocalizable> l = Utils.fromTable(table);

        logService.info("Populating point set");
        for( RealLocalizable p : l ) {
            float x = p.getFloatPosition(0);
            float y = p.getFloatPosition(1);
            float z = p.getFloatPosition(2);

            mesh.vertices().add(x, y, z);
        }

        logService.info("Generating mesh");

        final List<?> result = (List<?>) opService.run(DefaultConvexHull3D.class, mesh );
        mesh = (Mesh) result.get(0);
    }

    private float convertPointPositionToZ(RandomAccessibleInterval<RealType<?>> img, int pointPosition) {
        // This function has an issue with handling positions at multiple timepoints
        // ImageJ uses XY CZT order



        //System.out.println(pointPosition + " " + img.dimension(2) + " " + img.dimension(3) + " " + pointPosition / img.dimension(3) + " " + pointPosition / img.dimension(2) + " " + pointPosition / 204 + " " + pointPosition % 204 + " " + pointPosition % img.dimension(2));
        return pointPosition;
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

    public void setTable(Table table) {
        this.table = table;
    }
}
