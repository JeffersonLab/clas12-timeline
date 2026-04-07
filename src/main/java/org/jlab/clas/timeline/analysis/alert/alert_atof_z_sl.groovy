package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class alert_atof_z_sl {

  def data = new ConcurrentHashMap()
  def has_data = new AtomicBoolean(false)
  int sector
  int layer

  alert_atof_z_sl(int sector, int layer) {
    this.sector = sector
    this.layer  = layer
  }

  def getName() {
    return String.format("%s_sector%02d_layer%d", this.class.simpleName, sector, layer)
  }

  def processRun(dir, run) {
    data[run] = [run:run]
    def h1 = dir.getObject(String.format('/ALERT/ATOF_z_sector%02d_layer%02d', sector, layer))
    if (h1 != null) {
      if (h1.getEntries() > 10) {
        def name = String.format('atof_z_sl_s%02d_l%d', sector, layer)
        data[run].put(name, h1)
        data[run].put('rms_' + name, h1.getRMS())
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
    def name = String.format('atof_z_sl_s%02d_l%d', sector, layer)
    def gr = new GraphErrors(name)
    gr.setTitle(String.format("ATOF z RMS Sector %02d Layer %d", sector, layer))
    gr.setTitleY("ATOF z RMS (mm)")
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
    out.writeFile(String.format('alert_atof_z_sl_rms_sector%02d_layer%d.hipo', sector, layer))
  }
}
