package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ALERTFitter

class alert_atof_time {

def data = new ConcurrentHashMap()
def has_data = new AtomicBoolean(false)
  

  def processRun(dir, run) {

    data[run] = [run:run]
    def trigger = dir.getObject('/TRIGGER/bits')
    def reference_trigger_bit = 0
    // data[run].put('bits',  trigger)
    (0..<10).collect{component->
      def h1 = dir.getObject(String.format('/ALERT/ATOF_Time_component%d', component))
      if(h1!=null) {
        if (h1.getBinContent(h1.getMaximumBin()) > 30 && h1.getEntries()>300){
          data[run].put(String.format('atof_time_%d', component),  h1)
          def f1 = ALERTFitter.atof_time_fitter(h1,component)
          data[run].put(String.format('fit_atof_time_%d', component),  f1)
          data[run].put(String.format('peak_location_atof_time_%d', component),  f1.getParameter(1).abs())
          data[run].put(String.format('sigma_atof_time_%d', component),  f1.getParameter(2).abs())
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

    ['peak_location', 'sigma'].each{variable->
      (0..<4).collect{layer->
        def names = []
        TDirectory out = new TDirectory()
        out.mkdir('/timelines')
        (0..<11).collect{component->
          def name = String.format('atof_time_%d', component)

          def gr = new GraphErrors(name)

          gr.setTitle(  String.format("ATOF Time %s ", variable.replace('_', ' ')))
          gr.setTitleY( String.format("ATOF Time %s  (ns)", variable.replace('_', ' ')))
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
        out.writeFile(String.format('alert_atof_time_%s_sector%02d_layer%d.hipo', variable, sector, layer))
      }
    }
  }
}
