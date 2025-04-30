package org.jlab.clas12.monitoring;

import j4np.geom.prim.Vector3D;
import j4np.hipo5.data.Bank;
import j4np.hipo5.io.HipoReader;
import twig.data.GraphErrors;
import twig.data.H1F;
import twig.graphics.TGCanvas;

import java.util.ArrayList;
import java.util.List;

public class Efficiency {

    private int layer = 1;
    private int counter = 0;
    private int counterMatch = 0;
    private double[] cuts = new double[]{0.5, 2.0};
    private int sector = 1;

    public Efficiency() {}

    public Efficiency(int s, int l) {
        setSector(s);
        setLayer(l);
    }

    public void setLayer(int l) {
        if (l < 1 || l > 6) {
            System.out.println("Error: unknown layer " + l + ", setting to 1");
            this.layer = 1;
        } else {
            this.layer = l;
        }
    }

    public void setSector(int s) {
        this.sector = s;
    }

    public boolean isValid(int[] ids) {
        for (int i = 0; i < 3; i++) if (ids[i] <= 0) return false;
        return true;
    }

    public List<Vector3D> getIntersection(Bank b, int layer, int id) {
        List<Vector3D> list = new ArrayList<>();
        for (int i = 0; i < b.getRows(); i++) {
            if (b.getInt("layer", i) == layer && b.getInt("id", i) == id && b.getInt("sector", i) == sector) {
                list.add(new Vector3D(b.getFloat("x", i), b.getFloat("y", i), b.getFloat("z", i)));
            }
        }
        return list;
    }

    public int countExcept(int[] ids, int layer) {
        int count = 0;
        for (int i = 3; i < ids.length; i++) if (i != layer && ids[i] > 0) count++;
        return count;
    }

    public List<Vector3D> getClusters(Bank b, int layer) {
        List<Vector3D> list = new ArrayList<>();
        for (int i = 0; i < b.getRows(); i++) {
            if (b.getInt("layer", i) == layer) {
                list.add(new Vector3D(b.getFloat("x1", i), b.getFloat("y1", i), b.getFloat("z1", i)));
            }
        }
        return list;
    }

    public double result() {
        return (double) counterMatch / counter;
    }

    public String summary() {
        return String.format("Sector = %d, Layer = %d, Efficiency = %.6f", sector, layer, result());
    }

    public void process(Bank[] banks) {
        int[] ids = banks[0].getIntArray(9, "Cross1_ID", 0);
        if (isValid(ids) && countExcept(ids, layer + 2) > 3) {
            List<Vector3D> intersections = getIntersection(banks[1], layer + 6, 1);
            if (!intersections.isEmpty()) {
                List<Vector3D> clusters = getClusters(banks[2], layer);
                counter++;
                boolean matched = clusters.stream().anyMatch(cluster -> {
                    if (layer == 1 || layer == 4 || layer == 6) {
                        return (cluster.z() - intersections.get(0).z()) < cuts[0];
                    } else {
                        double angle = Math.abs(intersections.get(0).phi() * 57.2958 - cluster.phi() * 57.2958);
                        return angle < cuts[1];
                    }
                });
                if (matched) counterMatch++;
            }
        }
    }

    public static void main(String[] args) {
        String file = args.length > 0 ? args[0] : "rec_clas_17077_00.hipo";

        HipoReader reader = new HipoReader(file);
        Bank[] banks = reader.getBanks("CVTRec::Tracks", "CVTRec::Trajectory", "BMTRec::Clusters");

        List<Efficiency> processors = new ArrayList<>();
        for (int s = 1; s <= 3; s++)
            for (int l = 1; l <= 6; l++)
                processors.add(new Efficiency(s, l));

        while (reader.nextEvent(banks)) {
            if (banks[0].getRows() == 1) {
                processors.forEach(e -> e.process(banks));
            }
        }

        processors.forEach(e -> System.out.println(e.summary()));

        double[] data = processors.stream().mapToDouble(Efficiency::result).toArray();
        H1F h = new H1F("Efficiency", 0.5, data.length + 0.5, data);
        h.attr().setFillColor(2);

        TGCanvas canvas = new TGCanvas();
        canvas.draw(h);
    }
}
