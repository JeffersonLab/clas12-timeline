package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ALERTFitter

class alert_ahdc_time {

def data = new ConcurrentHashMap()
def has_data = new AtomicBoolean(false)

def layer_encoding = [11, 21, 22, 31, 32, 41, 42, 51]
def layer_wires    = [47, 56, 56, 72, 72, 87, 87, 99]
int layer_number;
int layer;
int number_of_wires_this_layer;
int number_of_wires_per_timeline;

  alert_ahdc_time(int ahdc_layer_number) {
      this.layer_number                 = ahdc_layer_number
      this.layer                        = layer_encoding[layer_number - 1];
      this.number_of_wires_this_layer   = layer_wires[layer_number - 1]
      this.number_of_wires_per_timeline = 15;
  }
  
  def getName() {
    return "${this.class.simpleName}_${layer_number}"
  }

  def processRun(dir, run) {

    data[run] = [run:run]
    def trigger = dir.getObject('/TRIGGER/bits')
    def reference_trigger_bit = 0
    // data[run].put('bits',  trigger)
    float integral = 0;
    (1..number_of_wires_this_layer).collect{wire_number->
      def h1 = dir.getObject(String.format("/ALERT/AHDC_TIME_layer%d_wire_number%02d", layer_number, wire_number))
      if(h1!=null) {
        if (h1.getBinContent(h1.getMaximumBin()) > 30 && h1.getEntries()>300){
          data[run].put(String.format('ahdc_time_layer%d_wire_number%02d', layer_number, wire_number),  h1)
          def maxz = h1.getBinContent(h1.getMaximumBin());
          //int t0bin = (0..<h1.getMaximumBin()).find {
          //  h1.getBinContent(it) >= 0.25 * maxz
          //}
          int firstNonZeroBin = (0..h1.getMaximumBin()).find { h1.getBinContent(it) > 0 } 
          int t0bin = firstNonZeroBin

          float t0 = h1.getAxis().getBinCenter(t0bin)
          int tmaxbin = (h1.getAxis().getNBins() - 1..h1.getMaximumBin()).find {
              h1.getBinContent(it) >= 0.25 * maxz
          }
          float tmax = h1.getAxis().getBinCenter(tmaxbin)
          float width = tmax - t0
          data[run].put(String.format('t0_ahdc_time_layer%d_wire_number%02d', layer_number, wire_number),  t0)
          data[run].put(String.format('tmax_ahdc_time_layer%d_wire_number%02d', layer_number, wire_number),  tmax)
          data[run].put(String.format('width_ahdc_time_layer%d_wire_number%02d', layer_number, wire_number),  width)
          data[run].put(String.format('fit_t0_ahdc_time_layer%d_wire_number%02d', layer_number, wire_number),  ALERTFitter.time_fitter_rising(h1, t0))
          data[run].put(String.format('fit_tmax_ahdc_time_layer%d_wire_number%02d', layer_number, wire_number),  ALERTFitter.time_fitter_falling(h1, tmax))
          data[run].put(String.format('fit_width_ahdc_time_layer%d_wire_number%02d', layer_number, wire_number),  ALERTFitter.time_fitter_width(h1, t0, tmax))
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
    ['t0', 'tmax', 'width'].each{variable->
      (0..<timeline_index_max).collect{timeline_index->

        TDirectory out = new TDirectory()
        out.mkdir('/timelines')
        def number_of_wires_this_timeline = number_of_wires_per_timeline
        if (layer_number == 1 && timeline_index == timeline_index_max - 1) number_of_wires_this_timeline = number_of_wires_per_timeline + number_of_wires_this_layer%number_of_wires_per_timeline
        (1..number_of_wires_this_timeline).collect{wire_index->
          def wire_number = wire_index + 15* timeline_index
          if (wire_number <= number_of_wires_this_layer){
            def name = String.format('ahdc_time_layer%d_wire_number%02d', layer_number, wire_number)
            def gr = new GraphErrors(name)
            gr.setTitle(  String.format("AHDC Time %s Layer %d", variable.replace('_', ' '), layer_number))
            gr.setTitleY( String.format("AHDC Time %s Layer %d", variable.replace('_', ' '), layer_number))
            gr.setTitleX("run number")
            data.sort{it.key}.each{run,it->
              out.mkdir('/'+it.run)
              out.cd('/'+it.run)
              if (it.containsKey(name)){
                out.addDataSet(it[name])
                out.addDataSet(it['fit_'+variable+'_'+name])
                gr.addPoint(it.run, it[variable + '_' + name], 0, 0)
              }
              else if (variable=="t0") println(String.format("run %d: %s either does not exist or does not have enough statistics.", it.run, name))
            }
            out.cd('/timelines')
            out.addDataSet(gr)
          }
        }
        out.writeFile(String.format('alert_ahdc_time_%s_layer%d_%d.hipo', variable, layer_number, timeline_index))
      }
    }
  }
}
