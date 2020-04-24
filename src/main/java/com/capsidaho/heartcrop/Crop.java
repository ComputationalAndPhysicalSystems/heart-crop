package com.capsidaho.heartcrop;

import ij.Executer;
import ij.IJ;
import net.imagej.Dataset;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Vertex;
import net.imagej.mesh.naive.NaiveDoubleMesh;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.DefaultConvexHull3D;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.RealMask;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.Table;
import visad.Irregular3DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.VisADException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Plugin(type = Command.class, label = "Heart Crop - Crop",
        menu = { @Menu(label = "Plugins"), //
                 @Menu(label = "Interactive 3D Crop"),
                 @Menu(label = "3 Crop") })
public class Crop implements Command {
	@Parameter
	private DisplayService displayService;

	@Parameter
	private OpService opService;

    @Parameter
    private Dataset img;

    @Parameter(required = false)
    private Mesh mesh;

    @Parameter
    private LogService logService;

    @Override
    public void run() {
        Dataset croppedImg = img;

        if( mesh == null ) {

        	mesh = new NaiveDoubleMesh();

			Table table = null;
			if( table == null ) {
				String tableName = "interactive3DCrop";

				Display<Table> tableDisplay = (Display<Table>) displayService.getDisplay(tableName);
				if( tableDisplay != null ) {
					table = tableDisplay.get(0);
				}
			}

			if( table == null ) {
				logService.error("Cannot find table");
				return;
			}

			List<RealLocalizable> l = Utils.fromTable(table);

			logService.info("Populating point set");
			for( RealLocalizable p : l ) {
				float x = p.getFloatPosition(0);
				float y = p.getFloatPosition(1);
				float z = p.getFloatPosition(2);

				mesh.vertices().add(x, y, z);
			}

        	final List<?> result = (List<?>) opService.run(DefaultConvexHull3D.class, mesh );
        	mesh = (Mesh) result.get(0);
		}

        float[] resolution = new float[img.numDimensions()];
        for( int d = 0; d < resolution.length; d++ ) {
            resolution[d] = (float) img.axis(d).averageScale(0, 1);
        }

        logService.info("Resolution: " + Arrays.toString(resolution));

        long vertexCount = mesh.vertices().size();
		if (vertexCount > Integer.MAX_VALUE) throw new RuntimeException("Hull too large");
		float[][] samples = new float[3][(int) vertexCount];
		int i = 0;
		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
		float sf = 0.01f;
		for (Vertex vertex : mesh.vertices()) {
		    if( minX > vertex.xf() ) minX = vertex.xf();
		    if( minY > vertex.yf() ) minY = vertex.yf();
		    if( minZ > vertex.zf() ) minZ = vertex.zf();
		    if( maxX < vertex.xf() ) maxX = vertex.xf();
		    if( maxY < vertex.yf() ) maxY = vertex.yf();
		    if( maxZ < vertex.zf() ) maxZ = vertex.zf();

		    float x = vertex.xf();
		    float y = vertex.yf();
		    float z = vertex.zf();

			samples[0][i] = x;// * resolution[0];
			samples[1][i] = y;// * resolution[1];
			samples[2][i] = z;// * resolution[2];
			i++;
		}
		logService.info( "Min: " + minX + " " + minY + " " + minZ + " -> " + maxX + " " + maxY + " " + maxZ );
		RealType xType = RealType.getRealType("X");
		RealType yType = RealType.getRealType("Y");
		RealType zType = RealType.getRealType("Z");
		RealType[] xyz = {xType, yType, zType};
        RealTupleType xyzType = null;
        Irregular3DSet set = null;
        try {
            xyzType = new RealTupleType(xyz);
            set = new Irregular3DSet(xyzType, samples);
        } catch (VisADException e) {
            e.printStackTrace();
        }
		logService.info("Triangulation computed");

		// Wrap as a RealMask.
        Irregular3DSet finalSet = set;
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
					int[] tri = finalSet.valueToTri(v);
					return tri[0] >= 0;
				}
				catch (VisADException exc) {
					throw new RuntimeException(exc);
				}
			}

			@Override
			public int numDimensions() { return 3; }
		};
		logService.info("ROI created: " + roi);

		// Make a tester for whether the point is contained within the mesh
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

		// Perform the actual crop

		long[] dims = new long[croppedImg.numDimensions()];
		croppedImg.dimensions(dims);
		logService.info("Dimensions: " + Arrays.toString(dims));

		// Parallelize
		int numThreads = Runtime.getRuntime().availableProcessors() - 1;

		ArrayList<Thread> threads = new ArrayList<Thread>();
		for( int threadId = 0; threadId < numThreads; threadId++ ) {

			int finalThreadId = threadId;

			Tester t = new Tester();// testers need to be per thread
			Thread thread = new Thread() {

				@Override
				public void run() {
					super.run();

					int count = 0;
					int current = count * numThreads + finalThreadId;
					// loop over frames
					while( ( current = count * numThreads + finalThreadId ) < croppedImg.dimension(3) ) {

						IntervalView<net.imglib2.type.numeric.RealType<?>> view = Views.hyperSlice(croppedImg, 3, current);

						Cursor<net.imglib2.type.numeric.RealType<?>> cur = view.cursor();
						double[] p = new double[4];

						System.out.println("cropping " + current + " of " + croppedImg.dimension(3));
						while (cur.hasNext()) {
							cur.next();
							cur.localize(p);

							if (!t.inside(p)) cur.get().setZero();
						}

						count++;
					}

				}
			};

			thread.start();
			threads.add(thread);
		}

		// Block until done
		while( threads.size() > 0 ) {
			if( threads.get(0).isAlive() ) {
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				threads.remove(0);
			}

		}

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

    public LogService getLogService() {
        return logService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }
}
