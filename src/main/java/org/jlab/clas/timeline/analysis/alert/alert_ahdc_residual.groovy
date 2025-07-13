package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ALERTFitter

class alert_ahdc_residual {

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
      def h1 = dir.getObject(String.format("/ALERT/AHDC_RESIDUAL_layer%d", layer_number))
      if(h1!=null) {
        if (h1.getBinContent(h1.getMaximumBin()) > 30 && h1.getEntries()>300){
          data[run].put(String.format("AHDC_RESIDUAL_layer%d", layer_number),  h1)
          def f1 = ALERTFitter.residual_fitter(h1)
          data[run].put(String.format("fit_AHDC_RESIDUAL_layer%d", layer_number),  f1)
          data[run].put(String.format("peak_location_AHDC_RESIDUAL_layer%d", layer_number),  f1.getParameter(1))
          data[run].put(String.format("sigma_AHDC_RESIDUAL_layer%d", layer_number),  f1.getParameter(2).abs())
          data[run].put(String.format("integral_normalized_to_trigger_AHDC_RESIDUAL_layer%d", layer_number),  Math.sqrt(2*3.141597f) * f1.getParameter(0) * f1.getParameter(2).abs()/trigger.getBinContent(reference_trigger_bit) )
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

    ['peak_location', 'sigma', 'integral_normalized_to_trigger'].each{variable->
      TDirectory out = new TDirectory()
      out.mkdir('/timelines')
      (0..<8).collect{layer->
        int layer_number = layer_encoding[layer]
        def name = String.format("AHDC_RESIDUAL_layer%d", layer_number)
        def gr = new GraphErrors(name)
        gr.setTitle(  String.format("AHDC_RESIDUAL %s laayer%d", variable.replace('_', ' '), layer_number))
        gr.setTitleY( String.format("AHDC_RESIDUAL %s (ns)", variable.replace('_', ' ')))
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
      }
      out.cd('/timelines')
      out.addDataSet(gr)
      out.writeFile(String.format('alert_ahdc_residual_%s.hipo', variable))
    }
  }
}
