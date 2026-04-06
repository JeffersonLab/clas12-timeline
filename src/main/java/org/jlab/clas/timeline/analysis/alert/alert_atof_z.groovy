package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class alert_atof_z {

  def data = new ConcurrentHashMap()
  def has_data = new AtomicBoolean(false)

  def processRun(dir, run) {
    data[run] = [run:run]
    def h1 = dir.getObject('/ALERT/ATOF_z_combined')
    if (h1 != null) {
      if (h1.getEntries() > 300) {
        data[run].put('atof_z_combined', h1)
        data[run].put('rms_atof_z_combined', h1.getRMS())
        has_data.set(true)
      }
    }
  }

  def write() {
    if (!has_data.get()) {
      System.err.println "WARNING: no data for this timeline, not producing"
      return
    }
    TDirectory out = new TDirectory()
    out.mkdir('/timelines')
    def name = 'atof_z_combined'
    def gr = new GraphErrors(name)
    gr.setTitle("ATOF z RMS")
    gr.setTitleY("ATOF z RMS (cm)")
    gr.setTitleX("run number")
    data.sort { it.key }.each { run, it ->
      out.mkdir('/' + it.run)
      out.cd('/' + it.run)
      if (it.containsKey(name)) {
        out.addDataSet(it[name])
        gr.addPoint(it.run, it['rms_' + name], 0, 0)
      } else {
        println(String.format("run %d: %s either does not exist or does not have enough statistics.", it.run, name))
      }
    }
    out.cd('/timelines')
    out.addDataSet(gr)
    out.writeFile('alert_atof_z_rms.hipo')
  }
}
