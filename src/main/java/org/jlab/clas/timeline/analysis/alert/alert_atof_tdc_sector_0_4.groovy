package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ALERTFitter

class alert_atof_tdc_sector_0_4 {

def data = new ConcurrentHashMap()
def has_data = new AtomicBoolean(false)

  def processRun(dir, run) {

    data[run] = [run:run]
    def trigger = dir.getObject('/TRIGGER/bits')
    def reference_trigger_bit = 0
    // data[run].put('bits',  trigger)
    (0..<240).collect{index->
      int sector    = index / (12 * 4);
      int layer     = (index % (12 * 4)) / 12;
      int component = index % 12;
      def file_index = '';
      if (component <= 10) file_index = String.format('sector%d_layer%d_component%d_order0', sector, layer, component)
      else file_index = String.format('sector%d_layer%d_component%d_order1', sector, layer, component-1)
      def h1 = dir.getObject(String.format('/ALERT/TDC_%s', file_index))
      float tdc_offset = 100.0f
      if (run<21331) tdc_offset = 350.0f
      if(h1!=null) {
        if (h1.getBinContent(h1.getMaximumBin()) > 30 && h1.getEntries()>300){
          data[run].put(String.format('atof_tdc_%s', file_index),  h1)
          def f1 = ALERTFitter.tdcfitter(h1, tdc_offset, run)
          data[run].put(String.format('fit_atof_tdc_%s', file_index),  f1)
          data[run].put(String.format('peak_location_atof_tdc_%s', file_index),  f1.getParameter(1))
          data[run].put(String.format('sigma_atof_tdc_%s', file_index),  f1.getParameter(2))
          data[run].put(String.format('integral_normalized_to_trigger_atof_tdc_%s', file_index),  Math.sqrt(2*3.141597f) * f1.getParameter(0) * f1.getParameter(2)/trigger.getBinContent(reference_trigger_bit) )
          has_data.set(true)
      }
    }
  }



  def write() {

    if(!has_data.get()) {
      System.err.println "WARNING: no data for this timeline, not producing"
      return
    }

    ['peak_location', 'sigma', 'integral_normalized_to_trigger'].each{variable->
      (0..<5).collect{sector->
        (0..<4).collect{layer->
          def names = []
          TDirectory out = new TDirectory()
          out.mkdir('/timelines')
          (0..<12).collect{component->
            def file_index = ''
            if (component <= 10) file_index = String.format('sector%d_layer%d_component%d_order0', sector, layer, component)
            else file_index = String.format('sector%d_layer%d_component%d_order1', sector, layer, component-1)
            names << String.format('atof_tdc_%s', file_index)
          }
          names.each{ name ->
            def gr = new GraphErrors(name)
            gr.setTitle(  name.substring(0, name.length()-17).replace('_', ' ').replace('atof', 'ATOF').replace('tdc', 'TDC') + variable.replace('_', ' '))
            gr.setTitleY( name.substring(0, name.length()-17).replace('_', ' ').replace('atof', 'ATOF').replace('tdc', 'TDC') + variable.replace('_', ' ') + " (ns)")
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
          out.writeFile(String.format('alert_atof_tdc_%s_sector%d_layer%d.hipo', variable, sector, layer))
        }
      }
    }
  }
}
