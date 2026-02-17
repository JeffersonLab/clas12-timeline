package org.jlab.clas.timeline.daqconfig
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.clas.timeline.fitter.ALERTFitter

class alert_daqconfig {

def data = new ConcurrentHashMap()
def has_data = new AtomicBoolean(false)


  def processRun(fname, run) {

    File file = new File(fname)
    def lines = file.readLines()
    def lineCount = lines.size()

    data[run] = [run:run]
    if (lineCount>=10){
        (0..14).collect{slot->
            def name = String.format('atof_threshold_%d', slot)
            def string_to_be_parsed = lines[lineCount -1 - (14-slot)*2]
            def value = string_to_be_parsed.split(/\s+/)[1].toFloat()
            data[run].put(name, value)
        }
    }
  }



  def write() {

    TDirectory out = new TDirectory()
    out.mkdir('/timelines')
    (0..14).collect{slot->
        def name = String.format('atof_threshold_%d', slot)
        def gr = new GraphErrors(name)
        gr.setTitle(  String.format("ATOF Threshold") )
        gr.setTitleY( String.format("ATOF Threshold") )
        gr.setTitleX("run number")
        data.sort{it.key}.each{run,it->
            out.mkdir('/'+it.run)
            out.cd('/'+it.run)
            if (it.containsKey(name)){
            // out.addDataSet(it[name])
            // out.addDataSet(it['fit_'+name])
            gr.addPoint(it.run, it[name], 0, 0)
            }
            else println(String.format("run %d: %s does not contain the desired information.", it.run, name))
        }
        out.cd('/timelines')
        out.addDataSet(gr)
    }
    out.writeFile(String.format('alert_daqconfig.hipo'))
  }
}
