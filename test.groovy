import org.jlab.io.hipo.HipoDataSource

infile     = args[0]
def reader = new HipoDataSource()
reader.open(infile)

def i = 0
def j = 0
while(reader.hasEvent()) {
  event = reader.getNextEvent()
  // println "$i: " + \
  //   "config: ${event.hasBank("REC::Particle")}" + "  " + \
  //   "scaler: ${event.hasBank("RUN::scaler")}"
  if(event.hasBank("RUN::scaler"))
    j++
  i++
}
reader.close()
println "counts: $j / $i = ${j/i}"
