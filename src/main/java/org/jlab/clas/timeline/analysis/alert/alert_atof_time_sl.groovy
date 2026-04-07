package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ALERTFitter

class alert_atof_time_sl {

  def data = new ConcurrentHashMap()
  def has_data = new AtomicBoolean(false)
  int sector
  int layer

  alert_atof_time_sl(int sector, int layer) {
    this.sector = sector
    this.layer  = layer
  }

  def getName() {
    return String.format("%s_sector%02d_layer%d", this.class.simpleName, sector, layer)
  }

  def processRun(dir, run) {
    data[run] = [run:run]
    (0..<11).each { component ->
      def h1 = dir.getObject(String.format('/ALERT/ATOF_Time_sector%02d_layer%02d_component%02d', sector, layer, component))
      if (h1 != null) {
        if (h1.getBinContent(h1.getMaximumBin()) > 3 && h1.getEntries() > 10) {
          def name = String.format('atof_time_sl_s%02d_l%d_c%02d', sector, layer, component)
          data[run].put(name, h1)
          double fit_min = h1.getXaxis().getBinCenter(1)
          double fit_max = h1.getXaxis().getBinCenter(h1.getXaxis().getNBins())
          def f1 = ALERTFitter.atof_time_fitter(h1, component, fit_min, fit_max)
          data[run].put('fit_' + name, f1)
          data[run].put('peak_location_' + name, f1.getParameter(1))
          data[run].put('sigma_' + name, f1.getParameter(2).abs())
          has_data.set(true)
        }
      }
    }
  }

  def write() {
    if (!has_data.get()) {
      System.err.println "WARNING: no data for this timeline, not producing"
      return
    }
    ['peak_location', 'sigma'].each { variable ->
      TDirectory out = new TDirectory()
      out.mkdir('/timelines')
      (0..<11).each { component ->
        def name = String.format('atof_time_sl_s%02d_l%d_c%02d', sector, layer, component)
        def gr = new GraphErrors(name)
        gr.setTitle(String.format("ATOF Time %s Sector %02d Layer %d", variable.replace('_', ' '), sector, layer))
        gr.setTitleY(String.format("ATOF Time %s (ns)", variable.replace('_', ' ')))
        gr.setTitleX("run number")
        data.sort { it.key }.each { run, it ->
          out.mkdir('/' + it.run)
          out.cd('/' + it.run)
          if (it.containsKey(name)) {
            out.addDataSet(it[name])
            out.addDataSet(it['fit_' + name])
            gr.addPoint(it.run, it[variable + '_' + name], 0, 0)
          } else if (variable == 'peak_location') {
            println(String.format("run %d: %s either does not exist or does not have enough statistics.", it.run, name))
          }
        }
        out.cd('/timelines')
        out.addDataSet(gr)
      }
      out.writeFile(String.format('alert_atof_time_sl_%s_sector%02d_layer%d.hipo', variable, sector, layer))
    }
  }
}
