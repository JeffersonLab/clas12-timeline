package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ALERTFitter

class alert_ahdc_adc_layer_number4 {

def data = new ConcurrentHashMap()
def has_data = new AtomicBoolean(false)

def layer_encoding = [11, 21, 22, 31, 32, 41, 42, 51]
def layer_wires    = [47, 56, 56, 72, 72, 87, 87, 99]
int layer_number = 4;
int layer        = layer_encoding[layer_number - 1];
int number_of_wires_this_layer   = layer_wires[layer_number - 1]
int number_of_wires_per_timeline = 15;

  def processRun(dir, run) {

    data[run] = [run:run]
    def trigger = dir.getObject('/TRIGGER/bits')
    def reference_trigger_bit = 0
    // data[run].put('bits',  trigger)
    float integral = 0;
    (1..number_of_wires_this_layer).collect{wire_number->
      def h1 = dir.getObject(String.format('/ALERT/ADC_layer%d_wire_number%d', layer, wire_number))
      if(h1!=null) {
        if (h1.getBinContent(h1.getMaximumBin()) > 30 && h1.getEntries()>300){
          data[run].put(String.format('ahdc_adc_layer%d_wire_number%d', layer, wire_number),  h1)
          integral = h1.integral()
          // def f1 = ALERTFitter.totfitter(h1)
          // data[run].put(String.format('fit_adc_layer%d_wire_number%d', layer, wire_number),  f1)
          // data[run].put(String.format('peak_location_ahdc_adc_layer%d_wire_number%d', layer, wire_number),  peak_location)
          // data[run].put(String.format('sigma_adc_layer%d_wire_number%d', layer, wire_number),  f1.getParameter(2).abs())
          data[run].put(String.format('integral_normalized_to_trigger_ahdc_adc_layer%d_wire_number%d', layer, wire_number),  integral/trigger.getBinContent(reference_trigger_bit) )
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
    int timeline_index_max = number_of_wires_this_layer/number_of_wires_per_timeline + 1
    if (layer_number==1) (timeline_index_max = timeline_index_max-1)
    ['integral_normalized_to_trigger'].each{variable->
      (0..<timeline_index_max).collect{timeline_index->

        TDirectory out = new TDirectory()
        out.mkdir('/timelines')
        def number_of_wires_this_timeline = number_of_wires_per_timeline
        if (layer_number == 1 && timeline_index == timeline_index_max - 1) number_of_wires_this_timeline = number_of_wires_per_timeline + number_of_wires_this_layer%number_of_wires_per_timeline
        (1..number_of_wires_this_timeline).collect{wire_index->
          def wire_number = wire_index + 15* timeline_index
          if (wire_number <= number_of_wires_this_layer){
            def name = String.format('ahdc_adc_layer%d_wire_number%d', layer, wire_number)
            def gr = new GraphErrors(name)
            gr.setTitle(  String.format("AHDC ADC %s layer %d", variable.replace('_', ' '), layer))
            gr.setTitleY( String.format("AHDC ADC %s layer %d", variable.replace('_', ' '), layer))
            gr.setTitleX("run number")
            data.sort{it.key}.each{run,it->
              out.mkdir('/'+it.run)
              out.cd('/'+it.run)
              if (it.containsKey(name)){
                out.addDataSet(it[name])
                // out.addDataSet(it['fit_'+name])
                gr.addPoint(it.run, it[variable + '_' + name], 0, 0)
              }
              else if (variable=="integral_normalized_to_trigger") println(String.format("run %d: %s either does not exist or does not have enough statistics.", it.run, name))
            }
            out.cd('/timelines')
            out.addDataSet(gr)
          }
        }
        out.writeFile(String.format('alert_ahdc_adc_%s_layer%d_%d.hipo', variable, layer, timeline_index))
      }
    }
  }
}
