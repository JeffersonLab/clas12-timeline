// get run number for a given HIPO file, using RUN::config
import org.jlab.io.hipo.HipoDataSource

if(args.length<1) {
  System.err.println """
  USAGE: run-groovy ${this.class.getSimpleName()}.groovy [HIPO file]
  Returns run number for a given file
  """
  System.exit(101)
}

def infile = args[0]
def reader = new HipoDataSource()
def runnum = 0
reader.open(infile)

while(reader.hasEvent()) {
  event = reader.getNextEvent()
  if(event.hasBank("RUN::config")) {
    runnum = BigInteger.valueOf(event.getBank("RUN::config").getInt('run',0))
    break
  }
}
reader.close()
System.out.println(runnum)

if(runnum<=0) {
  System.err.println("[ERROR]: run number not found for file $infile")
  System.exit(100)
}
