package org.jlab.clas.timeline.timeline.helicity
import java.util.concurrent.ConcurrentHashMap
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors

class helicity {

    def data = new ConcurrentHashMap()

    def processDirectory(dir, run) {
        def h1 = dir.getObject('/HELICITY/offlineRaw')
        def h2 = dir.getObject('/HELICITY/onlineRaw')
        def h3 = dir.getObject('/HELICITY/boardRaw')
        data[run] = [run:run, OfflineRaw:h1, OnlineRaw:h2, BoardRaw:h3]
    }

    def close() {
        TDirectory out = new TDirectory()
        out.mkdir('/timelines')
        ["OfflineRaw", "OnlineRaw","BoardRaw"].each{ name ->
            def gr = new GraphErrors(name)
            gr.setTitle("Helicity Delay Correction Infficiency")
            gr.setTitleY("Efficiency")
            gr.setTitleX("run number")
            data.sort{it.key}.each{run,it->
                it[name].setTitle("$name Helicity")
                out.mkdir('/'+it.run)
                out.cd('/'+it.run)
                out.addDataSet(it[name])
                def minus = it[name].getBinContent(it[name].getAxis().getBin(-1.0));
                def plus = it[name].getBinContent(it[name].getAxis().getBin(1.0));
                def udf = it[name].getBinContent(it[name].getAxis().getBin(0.0));
                if ((minus+plus+udf) > 0)
                    gr.addPoint(it.run, (minus+plus)/(minus+plus+udf), 0, 0)
                else
                    gr.addPoint(it.run, -1, 0, 0)
            }
            out.cd('/timelines')
            out.addDataSet(gr)
        }
        out.writeFile('helicity_efficiency.hipo')
    }

}
