package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ALERTFitter

class alert_ahdc_time {

def data = new ConcurrentHashMap()
def has_data = new AtomicBoolean(false)
def layer_encoding          = [11, 21, 22, 31, 32, 41, 42, 51];

  def processRun(dir, run) {

    data[run] = [run:run]
    def trigger = dir.getObject('/TRIGGER/bits')
    def reference_trigger_bit = 0
    // data[run].put('bits',  trigger)
    (0..<8).collect{layer->
      int layer_number = layer_encoding[layer]
      def h1 = dir.getObject(String.format("AHDC_TIME_layer%d", layer_number))
      if(h1!=null) {
        if (h1.getBinContent(h1.getMaximumBin()) > 30 && h1.getEntries()>300){
          data[run].put(String.format("/ALERT/AHDC_TIME_layer%d", layer_number),  h1)
          def f1 = ALERTFitter.time_fitter_rising(h1)
          def f2 = ALERTFitter.time_fitter_falling(h1)
          def t0, tmax
          def width = tmax - t0
          def f3 = ALERTFitter.time_fitter_width(h1)
          data[run].put(String.format("fit_t0_AHDC_TIME_layer%d", layer_number),  f1)
          data[run].put(String.format("fit_tmax_AHDC_TIME_layer%d", layer_number),  f2)
          data[run].put(String.format("fit_width_AHDC_TIME_layer%d", layer_number),  f3)
          data[run].put(String.format("t0_AHDC_TIME_layer%d", layer_number),  t0)
          data[run].put(String.format("tmax_AHDC_TIME_layer%d", layer_number),  tmax)
          data[run].put(String.format("width_normalized_to_trigger_AHDC_TIME_layer%d", layer_number),  width)
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

    ['t0', 'tmax', 'width'].each{variable->
      TDirectory out = new TDirectory()
      out.mkdir('/timelines')
      def gr = new GraphErrors(name)
      gr.setTitle(  String.format("AHDC TIME %s", variable.replace('_', ' ')))
      gr.setTitleY( String.format("AHDC TIME %s (ns)", variable.replace('_', ' ')))
      gr.setTitleX("run number")
      (0..<8).collect{layer->
        int layer_number = layer_encoding[layer]
        def name = String.format("AHDC_RESIDUAL_layer%d", layer_number)
        data.sort{it.key}.each{run,it->
          out.mkdir('/'+it.run)
          out.cd('/'+it.run)
          if (it.containsKey(name)){
            out.addDataSet(it[name])
            out.addDataSet(it['fit_'+variable+'_'+name])
            gr.addPoint(it.run, it[variable + '_' + name], 0, 0)
          }
          else if (variable=="peak_location") println(String.format("run %d: %s either does not exist or does not have enough statistics.", it.run, name))
        }
      }
      out.cd('/timelines')
      out.addDataSet(gr)
      out.writeFile(String.format('alert_ahdc_time_%s.hipo', variable))
    }
  }
}
