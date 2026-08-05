package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ALERTFitter
import org.jlab.groot.math.F1D

/**
 * Produces the timeline of `mean` and `width` of residual per layer.
 * Wire level is integrated.
 * When the `good_statistics` condition is satisfied, the histogram is fitted with Gaussian
 * If not, the histogram's mean and width achieved by its own `getMean()` and `getRMS()` methods.
 * An artificial, arbitrary `offset` of -2 is given to not `good_statistics` histograms for visualization purpose.
 * While `good_statistics` condition is subject to refinement,
 * it is defined as follows.
 * (1) the histogram's peak is higher than 20.
 * (2) the histogram's total entry is larger than 100.
 * 
 * @author Sangbaek Lee
*/

class alert_ahdc_residual_layer {

  def data = new ConcurrentHashMap()
  def has_data = new AtomicBoolean(false)
  
  def layer_wires    = [47, 56, 56, 72, 72, 87, 87, 99]

  def processRun(dir, run) {

    data[run] = [run:run]
    (1..8).collect{layer_number->
      def h_integrated = null
      long integrated_entries = 0 // See groot issue #4 
      def number_of_wires_this_layer   = layer_wires[layer_number - 1]
      (1..number_of_wires_this_layer).collect{wire_number->
        def h1 = dir.getObject(String.format("/ALERT/AHDC_RESIDUAL_layer%d_wire_number%02d", layer_number, wire_number))
        if(h1!=null) {
          integrated_entries += h1.getEntries()
          if (h_integrated == null) {
            h_integrated = h1.histClone(String.format("ahdc_residual_layer%d", layer_number))
            h_integrated.setTitle(String.format("AHDC Residual layer %d", layer_number))
          } else {
            h_integrated.add(h1)
          }
        }
      }
      if (h_integrated!=null) {
        if (h_integrated.getBinContent(h_integrated.getMaximumBin()) > 20 && integrated_entries>100){ //`good_statistics`
          data[run].put(String.format('ahdc_residual_layer%d', layer_number),  h_integrated)
          def f_integrated = ALERTFitter.residual_fitter(h_integrated)
          data[run].put(String.format("fit_ahdc_residual_layer%d", layer_number),  f_integrated)
          data[run].put(String.format("mean_ahdc_residual_layer%d", layer_number),  f_integrated.getParameter(1))
          data[run].put(String.format("width_ahdc_residual_layer%d", layer_number),  f_integrated.getParameter(2).abs())
          has_data.set(true)
        }
        else{ //`not_good_statistics`
          data[run].put(String.format('ahdc_residual_layer%d', layer_number),  h_integrated)
          def h_integrated_mean = h_integrated.getMean()
          def h_integrated_rms  = h_integrated.getRMS()
          def f_integrated = new F1D("fit:"+h_integrated.getName(),"[cst]", h_integrated_mean - h_integrated_rms, h_integrated_mean + h_integrated_rms);
          f_integrated.setParameter(0, 1);
          data[run].put(String.format("fit_ahdc_residual_layer%d", layer_number), f_integrated)
          data[run].put(String.format("mean_ahdc_residual_layer%d", layer_number), -2.0 + h_integrated_mean) //`offset`
          data[run].put(String.format("width_ahdc_residual_layer%d", layer_number), -2.0 + h_integrated_rms) //`offset`
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
    ['mean', 'width'].each{variable->
      TDirectory out = new TDirectory()
      out.mkdir('/timelines')
      (1..8).collect{layer_number->
        def name = String.format('ahdc_residual_layer%d', layer_number)
        def gr = new GraphErrors(name)
        gr.setTitle(  String.format("AHDC Residual %s", variable.replace('_', ' ')))
        gr.setTitleY( String.format("AHDC Residual %s (mm)", variable.replace('_', ' ')))
        gr.setTitleX("run number")
        data.sort{it.key}.each{run,it->
          out.mkdir('/'+it.run)
          out.cd('/'+it.run)
          if (it.containsKey(name)){
            out.addDataSet(it[name])
            out.addDataSet(it['fit_'+name])
            gr.addPoint(it.run, it[variable + '_' + name], 0, 0)
          }
          else if (variable=="mean") println(String.format("run %d: %s either does not exist or does not have enough statistics.", it.run, name))
        }
        out.cd('/timelines')
        out.addDataSet(gr)
      }
      out.writeFile(String.format('alert_ahdc_residual_%s.hipo', variable))
    }
  }
}
