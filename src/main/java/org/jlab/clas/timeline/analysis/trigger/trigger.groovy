package org.jlab.clas.timeline.analysis
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F

class trigger {

    def data = new ConcurrentHashMap()

    def processRun(dir, run) {
        def h = dir.getObject('/TRIGGER/bits')
        data[run] = [run:run, Bits:h]
    }

    def write() {
        TDirectory out = new TDirectory()
        out.mkdir('/timelines')
        for (int i=0; i<64; ++i) {
            def gr = new GraphErrors("bit$i")
            gr.setTitle("Trigger Bit $i")
            gr.setTitleY("Event Fraction")
            gr.setTitleX("Run Number")
            data.sort{it.key}.each{run,it->
                out.mkdir('/'+it.run)
                out.cd('/'+it.run)
                out.addDataSet(it["Bits"])
                gr.addPoint(it.run, it["Bits"].getDataX(i) / it["Bits"].getDataX(64), 0, 0);
            }
            out.cd('/timelines')
            out.addDataSet(gr)
        }
        out.writeFile('trigger.hipo')
    }

}
