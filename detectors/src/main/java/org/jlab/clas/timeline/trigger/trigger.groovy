package org.jlab.clas.timeline.timeline.trigger
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F

class trigger {

    def data = new ConcurrentHashMap()

    def processDirectory(dir, run) {
        def h = dir.getObject('/TRIGGER/bits')
        data[run] = [run:run, Bits:h]
    }

    def close() {
        TDirectory out = new TDirectory()
        out.mkdir('/timelines')
        for (int i=0; i<64; ++i) {
            def name = "$i"; 
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
                gr.addPoint(it.run, it[name].getDataX(i) / it[name].getDataX(64));
            }
            out.cd('/timelines')
            out.addDataSet(gr)
        }
        out.writeFile('trigger.hipo')
    }

}