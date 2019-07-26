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
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Vertex;
import net.imagej.mesh.naive.NaiveDoubleMesh;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.DefaultConvexHull3D;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.RealMask;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.InteractiveCommand;
import org.scijava.io.IOService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.util.Colors;
import org.scijava.widget.Button;
import sc.iview.SciView;
import sc.iview.vector.ClearGLVector3;
import sc.iview.vector.Vector3;
import visad.Irregular3DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.VisADException;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

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
public class MyDemo extends InteractiveCommand {

    @Parameter
    private SciView sciView;

    @Parameter
    private UIService uiService;

    @Parameter
    private IOService ioService;

    @Parameter
    private OpService opService;

    @Parameter(callback = "createMesh")
    private Button createMesh;

    @Parameter(callback = "crop")
    private Button crop;

    private RoiManager roiManager;
    private Dataset img = null;
    private Dataset croppedImg = null;
    private Volume volume = null;
    private HashMap<PointRoi, Node> scPoints;
    private float[] resolution = new float[]{(float) 0.6500002, (float) 0.6500002, 2f};
    private Mesh currentMesh = null;
    private Node currentMeshNode = null;

    @Override
    public void initialize() {
        String filename = "/home/kharrington/Data/Anjalie/C1-fish4_z_stack_red.tif";

        try {
            img = (Dataset) ioService.open(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }

        uiService.show(img);

        roiManager = new RoiManager();
        IJ.setTool("point");
        roiManager.runCommand("Open","/home/kharrington/git/heart-crop/Demo_RoiSet.zip");

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
//        for( Node n : volume.getChildren() ) {
//            volume.removeChild(n);
//        }

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

    /* Create a ConvexHulls of controlPoints */
    public void createMesh() {
        Mesh mesh = new NaiveDoubleMesh();

        System.out.println("Populating point set");
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
                mesh.vertices().add(x, y, z);
            }
        }

        System.out.println("Generating mesh");
        // TODO coudl we just use marching cubes??
        final List<?> result = (List<?>) opService.run(DefaultConvexHull3D.class, mesh );
        Mesh hull = (Mesh) result.get(0);

        System.out.println("Rendering mesh");
        if( currentMesh != null ) {
            sciView.deleteNode(currentMeshNode, true);
        }

        currentMesh = hull;
        currentMeshNode = sciView.addMesh(hull);
    }

    /* Create a ConvexHulls of controlPoints */
    public void crop() throws VisADException {
//        currentMesh = hull;
//        currentMeshNode = sciView.addMesh(hull);

        croppedImg = img.duplicate();

        long vertexCount = currentMesh.vertices().size();
		if (vertexCount > Integer.MAX_VALUE) throw new RuntimeException("Hull too large");
		float[][] samples = new float[3][(int) vertexCount];
		int i = 0;
		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
		float sf = 0.01f;
		for (Vertex vertex : currentMesh.vertices()) {
		    if( minX > vertex.xf() ) minX = vertex.xf();
		    if( minY > vertex.yf() ) minY = vertex.yf();
		    if( minZ > vertex.zf() ) minZ = vertex.zf();
		    if( maxX < vertex.xf() ) maxX = vertex.xf();
		    if( maxY < vertex.yf() ) maxY = vertex.yf();
		    if( maxZ < vertex.zf() ) maxZ = vertex.zf();

		    float x = vertex.xf();
		    float y = vertex.yf();
		    float z = vertex.zf();

		    x = x / sf / volume.getPixelToWorldRatio() / resolution[0] + volume.getSizeX()/2;
		    y = y / sf / volume.getPixelToWorldRatio() / resolution[1] + volume.getSizeY()/2;
		    z = z / sf / volume.getPixelToWorldRatio() / resolution[2] + volume.getSizeZ()/2;

			samples[0][i] = x;
			samples[1][i] = y;
			samples[2][i] = z;
			i++;
		}
		System.out.println( "Min: " + minX + " " + minY + " " + minZ + " -> " + maxX + " " + maxY + " " + maxZ );
		RealType xType = RealType.getRealType("X");
		RealType yType = RealType.getRealType("Y");
		RealType zType = RealType.getRealType("Z");
		RealType[] xyz = {xType, yType, zType};
		RealTupleType xyzType = new RealTupleType(xyz);
		Irregular3DSet set = new Irregular3DSet(xyzType, samples);
		System.out.println("Triangulation computed");

		// Wrap as a RealMask.
		RealMask roi = new RealMask() {
			private ThreadLocal<float[][]> value = new ThreadLocal<float[][]>() {
				@Override
				public float[][] initialValue() {
					return new float[3][1];
				}
			};

			@Override
			public boolean test(RealLocalizable t) {
				float[][] v = value.get();
				try {
					v[0][0] = t.getFloatPosition(0);
					v[1][0] = t.getFloatPosition(1);
					v[2][0] = t.getFloatPosition(2);
					int[] tri = set.valueToTri(v);
					return tri[0] >= 0;
				}
				catch (VisADException exc) {
					throw new RuntimeException(exc);
				}
			}

			@Override
			public int numDimensions() { return 3; }
		};
		System.out.println("ROI created: " + roi);

		class Tester {
            public RealPoint getPoint() {
                return point;
            }

            private RealPoint point = new RealPoint(3);
			public boolean inside(double... p) {
				point.setPosition(p[0], 0);
				point.setPosition(p[1], 1);
				point.setPosition(p[2], 2);
				return roi.test(point);
				//System.out.println(point + " -> " + roi.test(point));
			}
		}
		Tester t = new Tester();

		net.imglib2.Cursor<net.imglib2.type.numeric.RealType<?>> cur = croppedImg.cursor();
		double[] p = new double[3];
		while(cur.hasNext()) {
		    cur.next();
		    cur.localize(p);
		    if( !t.inside(p) ) cur.get().setZero();
        }

        Volume newVol = (Volume) sciView.addVolume(croppedImg, resolution);
        newVol.setPixelToWorldRatio(0.1f);
        volume.setVisible(false);
        currentMeshNode.setVisible(false);
    }

    @Override
    public void cancel() {
        for( Node n : volume.getChildren() ) {
            volume.removeChild(n);
        }
    }

}
