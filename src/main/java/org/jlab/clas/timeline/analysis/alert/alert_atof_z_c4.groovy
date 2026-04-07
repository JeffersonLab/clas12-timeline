package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ALERTFitter

class alert_atof_z_c4 {

  def data = new ConcurrentHashMap()
  def has_data = new AtomicBoolean(false)

  def processRun(dir, run) {
    data[run] = [run:run]
    def h1 = dir.getObject('/ALERT/ATOF_z_combined_c4')
    if (h1 != null) {
      if (h1.getBinContent(h1.getMaximumBin()) > 3 && h1.getEntries() > 10) {
        data[run].put('atof_z_c4_combined', h1)
        def f1 = ALERTFitter.atof_z_fitter(h1)
        data[run].put('fit_atof_z_c4_combined', f1)
        data[run].put('peak_location_atof_z_c4_combined', f1.getParameter(1))
        data[run].put('sigma_atof_z_c4_combined', f1.getParameter(2).abs())
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
      def name = 'atof_z_c4_combined'
      def gr = new GraphErrors(name)
      gr.setTitle(String.format("ATOF z c4 %s", variable.replace('_', ' ')))
      gr.setTitleY(String.format("ATOF z c4 %s (mm)", variable.replace('_', ' ')))
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
      out.writeFile(String.format('alert_atof_z_c4_%s.hipo', variable))
    }
  }
}
