package org.jlab.clas.timeline.timeline.helicity
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F

class helicity {

    def data = new ConcurrentHashMap()

    def processDirectory(dir, run) {
        def h = dir.getObject('/TRIGGER/bits')
        data[run] = [run:run, Bits:h]
    }

    def close() {
        TDirectory out = new TDirectory()
        out.mkdir('/timelines')
        ["0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25","26","27","28","29","30","31","32","33","34","35","36","37","38","39","40","41","42","43","44","45","46","47","48","49","50","51","52","53","54","55","56","57","58","59","60","61","62","63"].each{ name ->
            def gr = new GraphErrors(name)
            gr.setTitle("Trigger Bit $name")
            gr.setTitleY("Event Fraction")
            gr.setTitleX("Run Number")
            data.sort{it.key}.each{run,it->
                it[name].setTitle("Trigger Bit $name")
                out.mkdir('/'+it.run)
                out.cd('/'+it.run)
                out.addDataSet(it[name])
                gr.addPoint(it.run, 0.0)
                for (def i=0; i<it[name].getAxis().getNBins(); ++i)
                    gr.addPoint(it.run, it[name].getDataX(i) / it[name].getEntries());
            }
            out.cd('/timelines')
            out.addDataSet(gr)
        }
        out.writeFile('helicity_efficiency.hipo')
    }

}