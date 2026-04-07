package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ALERTFitter

class alert_atof_z_c4_sl {

  def data = new ConcurrentHashMap()
  def has_data = new AtomicBoolean(false)
  int sector
  int layer

  alert_atof_z_c4_sl(int sector, int layer) {
    this.sector = sector
    this.layer  = layer
  }

  def getName() {
    return String.format("%s_sector%02d_layer%d", this.class.simpleName, sector, layer)
  }

  def processRun(dir, run) {
    data[run] = [run:run]
    def h1 = dir.getObject(String.format('/ALERT/ATOF_z_c4_sector%02d_layer%02d', sector, layer))
    if (h1 != null) {
      if (h1.getBinContent(h1.getMaximumBin()) > 3 && h1.getEntries() > 10) {
        def name = String.format('atof_z_c4_sl_s%02d_l%d', sector, layer)
        data[run].put(name, h1)
        def f1 = ALERTFitter.atof_z_fitter(h1)
        data[run].put('fit_' + name, f1)
        data[run].put('peak_location_' + name, f1.getParameter(1))
        data[run].put('sigma_' + name, f1.getParameter(2).abs())
        has_data.set(true)
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
      def name = String.format('atof_z_c4_sl_s%02d_l%d', sector, layer)
      def gr = new GraphErrors(name)
      gr.setTitle(String.format("ATOF z c4 %s Sector %02d Layer %d", variable.replace('_', ' '), sector, layer))
      gr.setTitleY(String.format("ATOF z c4 %s (cm)", variable.replace('_', ' ')))
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
      out.writeFile(String.format('alert_atof_z_c4_sl_%s_sector%02d_layer%d.hipo', variable, sector, layer))
    }
  }
}
