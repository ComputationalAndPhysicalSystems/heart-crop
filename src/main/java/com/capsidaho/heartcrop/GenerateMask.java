package com.capsidaho.heartcrop;

import ij.IJ;
import net.imagej.Dataset;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Vertex;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.RealMask;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import visad.Irregular3DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.VisADException;

@Plugin(type = Command.class, label = "Heart Crop - Crop",
        menu = { @Menu(label = "Plugins"), //
                 @Menu(label = "Interactive 3D Crop"),
                 @Menu(label = "Generate Mask") })
public class GenerateMask implements Command {
    @Parameter
    private Dataset img;

    @Parameter
    private Mesh mesh;

    @Parameter
    private LogService logService;

    @Parameter
    private OpService opService;

    @Override
    public void run() {
        Dataset croppedImg = img;

        System.out.println(img.dimension(0) + " " + img.dimension(1) + " " + img.dimension(2) + " " + img.dimension(3));

        Img<UnsignedByteType> mask = opService.create().img(Views.hyperSlice(img, 3, 0), new UnsignedByteType());

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

			samples[0][i] = x;
			samples[1][i] = y;
			samples[2][i] = z;
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
				catch (Exception exc) {
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
		Tester t = new Tester();

		// Perform the actual crop
		Cursor<UnsignedByteType> cur = mask.cursor();
		double[] p = new double[4];

		int lastReport = -1;

		while(cur.hasNext()) {
		    cur.next();
		    cur.localize(p);

		    if( !t.inside(p) )
		        cur.get().setZero();
		    else
		        cur.get().set(255);
        }

		ImageJFunctions.show(mask);
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

    public void setOpService(OpService opService) {
        this.opService = opService;
    }
}
