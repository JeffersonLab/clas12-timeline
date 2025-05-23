package org.jlab.clas.timeline.histograms;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.TDirectory;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataBank;

import org.jlab.geom.prim.Vector3D;

import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author Christopher Dilks, Davit Martiryan
 */
public class CVT {

  public class Efficiency {

    public H1F h_counts;

    private int sector = 1;
    private int layer  = 1;
    private final double[] cuts = new double[]{0.5, 2.0};

    public Efficiency(int s, int l) {
      if (l < 1 || l > 6)
        System.err.println("ERROR: unknown layer " + l + ", setting to 1");
      this.sector = s;
      this.layer  = l;
      String sl_string = "S" + this.sector + "L" + this.layer;
      h_counts = new H1F("H_CVT_counts_" + sl_string, "CVT " + sl_string, 2, 0, 2);
      h_counts.setTitleX("bin 0 = all counts, bin 1 = matched counts");
    }

    public int countExcept(int[] ids, int layer) {
      int count = 0;
      for(int i = 3; i < ids.length; i++) {
        if(i != layer && ids[i] > 0)
          count++;
      }
      return count;
    }

    public List<Vector3D> getIntersection(DataBank b, int layer, int id) {
      List<Vector3D> list = new ArrayList<>();
      for(int i = 0; i < b.rows(); i++) {
        if(b.getInt("layer", i) == layer && b.getInt("id", i) == id && b.getInt("sector", i) == sector) {
          list.add(new Vector3D(b.getFloat("x", i), b.getFloat("y", i), b.getFloat("z", i)));
        }
      }
      return list;
    }

    public List<Vector3D> getClusters(DataBank b, int layer) {
      List<Vector3D> list = new ArrayList<>();
      for(int i = 0; i < b.rows(); i++) {
        if(b.getInt("layer", i) == layer) {
          list.add(new Vector3D(b.getFloat("x1", i), b.getFloat("y1", i), b.getFloat("z1", i)));
        }
      }
      return list;
    }

    public void process(DataBank bank_cvt_tracks, DataBank bank_cvt_trajectory, DataBank bank_bmt_clusters) {
      // get IDs from `Cross1_ID`, `Cross2_ID`, etc.
      final int ids_size = 9;
      int[] ids = new int[ids_size];
      for(int i = 0; i < ids_size; i++)
        ids[i] = bank_cvt_tracks.getInt(String.format("Cross%d_ID", i+1), 0);
      // check if `ids` isValid
      for(int i = 0; i < 3; i++) {
        if(ids[i] <= 0)
          return;
      }
      // fill `h_counts`
      if(countExcept(ids, layer + 2) > 3) {
        List<Vector3D> intersections = getIntersection(bank_cvt_trajectory, layer + 6, 1);
        if(!intersections.isEmpty()) {
          List<Vector3D> clusters = getClusters(bank_bmt_clusters, layer);
          h_counts.fill(0);
          boolean matched = clusters.stream().anyMatch(cluster -> {
            if(layer == 1 || layer == 4 || layer == 6) {
              return (cluster.z() - intersections.get(0).z()) < cuts[0];
            } else {
              double angle = Math.abs(intersections.get(0).phi() * 57.2958 - cluster.phi() * 57.2958);
              return angle < cuts[1];
            }
          });
          if(matched)
            h_counts.fill(1);
        }
      }
    }

  }

  //////////////////////////////////////////////////////////////////////////////////

  private List<Efficiency> processors;

  public CVT() {
    processors = new ArrayList<>();
    for(int s = 1; s <= 3; s++)
      for(int l = 1; l <= 6; l++)
        processors.add(new Efficiency(s, l));
  }

  public void processEvent(DataEvent event){
    if(event.hasBank("CVTRec::Tracks") && event.hasBank("CVTRec::Trajectory") && event.hasBank("BMTRec::Clusters")) {
      processors.forEach(e -> e.process(event.getBank("CVTRec::Tracks"), event.getBank("CVTRec::Trajectory"), event.getBank("BMTRec::Clusters")));
    }
  }

  public void write(String outputDir, int runNumber) {
    TDirectory dir = new TDirectory();
    dir.mkdir("/CVT/");
    dir.cd("/CVT/");
    processors.forEach(e -> dir.addDataSet(e.h_counts));
    dir.writeFile(outputDir + String.format("/out_CVT_%d.hipo",runNumber));
  }

}
