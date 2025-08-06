package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ALERTFitter

class alert_atof_tot_sector_12 {

def data = new ConcurrentHashMap()
def has_data = new AtomicBoolean(false)
int sector = 12
int index_min = 48*sector;
int index_max = 48*sector + 48
  def processRun(dir, run) {

    data[run] = [run:run]
    def trigger = dir.getObject('/TRIGGER/bits')
    def reference_trigger_bit = 0
    // data[run].put('bits',  trigger)
    (index_min..<index_max).collect{index->
      int sector_ = index / (12 * 4);
      assert sector == sector_;
      int layer     = (index % (12 * 4)) / 12;
      int component = index % 12;
      def file_index = '';
      if (component <= 10) file_index = String.format('sector%d_layer%d_component%d_order0', sector, layer, component)
      else file_index = String.format('sector%d_layer%d_component%d_order1', sector, layer, component-1)
      def h1 = dir.getObject(String.format('/ALERT/TOT_%s', file_index))
      float peak_location = 0
      if(h1!=null) {
        if (h1.getBinContent(h1.getMaximumBin()) > 30 && h1.getEntries()>300){
          data[run].put(String.format('atof_tot_%s', file_index),  h1)

          peak_location = h1.getAxis().getBinCenter(h1.getMaximumBin())
          def f1 = ALERTFitter.totfitter(h1)
          data[run].put(String.format('fit_atof_tot_%s', file_index),  f1)
          data[run].put(String.format('peak_location_atof_tot_%s', file_index),  peak_location)
          // data[run].put(String.format('sigma_atof_tot_%s', file_index),  f1.getParameter(2).abs())
          // data[run].put(String.format('integral_normalized_to_trigger_atof_tot_%s', file_index),  Math.sqrt(2*3.141597f) * f1.getParameter(0).abs() * f1.getParameter(2).abs()/trigger.getBinContent(reference_trigger_bit) )
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

    ['peak_location'].each{variable->
        (0..<4).collect{layer->
          def names = []
          TDirectory out = new TDirectory()
          out.mkdir('/timelines')
          (0..<12).collect{component->
            def file_index = ''
            if (component <= 10) file_index = String.format('sector%d_layer%d_component%d_order0', sector, layer, component)
            else file_index = String.format('sector%d_layer%d_component%d_order1', sector, layer, component-1)
            names << String.format('atof_tot_%s', file_index)
          }
          names.each{ name ->
            def gr = new GraphErrors(name)
            gr.setTitle(  String.format("ATOF TOT %s sector %d layer %d", variable.replace('_', ' '), sector, layer))
            gr.setTitleY( String.format("ATOF TOT %s sector %d layer %d (ns)", variable.replace('_', ' '), sector, layer))
            gr.setTitleX("run number")
            data.sort{it.key}.each{run,it->
              out.mkdir('/'+it.run)
              out.cd('/'+it.run)
              if (it.containsKey(name)){
                out.addDataSet(it[name])
                out.addDataSet(it['fit_'+name])
                gr.addPoint(it.run, it[variable + '_' + name], 0, 0)
              }
              else if (variable=="peak_location") println(String.format("run %d: %s either does not exist or does not have enough statistics.", it.run, name))
            }
            out.cd('/timelines')
            out.addDataSet(gr)
          }
          out.writeFile(String.format('alert_atof_tot_%s_sector%d_layer%d.hipo', variable, sector, layer))
        }
    }
  }
}
