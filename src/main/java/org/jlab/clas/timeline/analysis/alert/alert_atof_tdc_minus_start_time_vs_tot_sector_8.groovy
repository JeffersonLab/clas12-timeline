package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ALERTFitter
import org.jlab.groot.data.H1F;

class alert_atof_tdc_minus_start_time_vs_tot_sector_0 {

def data = new ConcurrentHashMap()
def has_data = new AtomicBoolean(false)

  def processRun(dir, run) {

    data[run] = [run:run]
    def trigger = dir.getObject('/TRIGGER/bits')
    def reference_trigger_bit = 0
    // data[run].put('bits',  trigger)
    (384..<432).collect{index->
      int sector    = index / (12 * 4);
      int layer     = (index % (12 * 4)) / 12;
      int component = index % 12;
      def file_index = '';
      if (component <= 10) file_index = String.format('sector%d_layer%d_component_%d_order_0', sector, layer, component)
      else file_index = String.format('sector%d_layer%d_component_%d_order_1', sector, layer, component-1)
      def h2 = dir.getObject(String.format('/ALERT/TDC_minus_start_time_vs_TOT_%s', file_index))
      if(h2!=null) {
        if (h2.getEntries()>300){
          data[run].put(String.format('atof_tdc_minus_start_time_vs_tot_%s', file_index),  h2)
          def binx_when_20 = h2.getXAxis().getBin(20);
          def binx_when_40 = h2.getXAxis().getBin(40);
          def binx_max     = h2.getXAxis().getNBins();
          ArrayList<H1F> h2_slices_by_X = h2.getSlicesX();

          def h1_1 = h2_slices_by_X.get(0)
          def h1_2 = h2_slices_by_X.get(binx_when_20);
          def h1_3 = h2_slices_by_X.get(binx_when_40);

          (1..<binx_when_20).each{it ->
            h1_1.add( h2_slices_by_X.get(it))
          }
          (binx_when_20+1..<binx_when_40).each{it ->
            h1_2.add( h2_slices_by_X.get(it))
          }
          (binx_when_40+1..<binx_max).each{it ->
            h1_3.add( h2_slices_by_X.get(it))
          }

          if (h1_1.getBinContent(h1_1.getMaximumBin()) > 10){
            data[run].put(String.format('mean_tot_range1_atof_tdc_minus_start_time_vs_tot_%s', file_index),  h1_1.getMean())
            data[run].put(String.format('rms_tot_range1_atof_tdc_minus_start_time_vs_tot_%s', file_index),   h1_1.getRMS())
            h1_1.setTitle("tdc - start time (0 ns < TOT < 20 ns) (ns)")
          }
          if (h1_2.getBinContent(h1_2.getMaximumBin()) > 10){
            data[run].put(String.format('mean_tot_range2_atof_tdc_minus_start_time_vs_tot_%s', file_index),  h1_2.getMean())
            data[run].put(String.format('rms_tot_range2_atof_tdc_minus_start_time_vs_tot_%s', file_index),   h1_2.getRMS())
            h1_2.setTitle("tdc - start time (20 ns < TOT < 40 ns) (ns)")
          }
          if (h1_3.getBinContent(h1_3.getMaximumBin()) > 10){
            data[run].put(String.format('mean_tot_range3_atof_tdc_minus_start_time_vs_tot_%s', file_index),  h1_3.getMean())
            data[run].put(String.format('rms_tot_range3_tdc_minus_start_time_vs_tot_%s', file_index),        h1_3.getRMS())
            h1_3.setTitle("tdc - start time (40 ns < TOT < 70 ns) (ns)")
          }
          has_data.set(true)
        }
      }
    }
  }



  def write() {

    if(!has_data.get()) {
      System.err.println "WARNING: no data for this timeline, not producing"
      return
    }

    ['mean_tot_range1', 'rms_tot_range1', 'mean_tot_range2', 'rms_tot_range2', 'mean_tot_range3', 'rms_tot_range3'].each{variable->
      (8..8).collect{sector->
        (0..<4).collect{layer->
          def names = []
          TDirectory out = new TDirectory()
          out.mkdir('/timelines')
          (0..<12).collect{component->
            def file_index = ''
            if (component <= 10) file_index = String.format('sector%d_layer%d_component_%d_order_0', sector, layer, component)
            else file_index = String.format('sector%d_layer%d_component_%d_order_1', sector, layer, component-1)
            names << String.format('atof_tdc_minus_start_time_vs_tot_%s', file_index)
          }
          names.each{ name ->
            def gr = new GraphErrors(name)
            gr.setTitle(  String.format("%s sector %d layer %d", variable.replace('_', ' '), sector, layer))
            gr.setTitleY( String.format("%s sector %d layer %d (ns)", variable.replace('_', ' '), sector, layer))
            gr.setTitleX("run number")
            data.sort{it.key}.each{run,it->
              out.mkdir('/'+it.run)
              out.cd('/'+it.run)
              if (it.containsKey(name)){
                out.addDataSet(it[name])
                // out.addDataSet(it['fit_'+name])
                if (it.containsKey(variable + '_' + name)) gr.addPoint(it.run, it[variable + '_' + name], 8..8)
              }
              else if (variable=="mean_tot_range1") println(String.format("run %d: %s either does not exist or does not have enough statistics.", it.run, name))
            }
            out.cd('/timelines')
            out.addDataSet(gr)
          }
          out.writeFile(String.format('alert_atof_tdc_minus_start_time_vs_tot_%s_sector%d_layer%d.hipo', variable, sector, layer))
        }
      }
    }
  }
}
