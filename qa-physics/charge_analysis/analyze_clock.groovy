import org.jlab.detector.scalers.DaqScalers;
import org.jlab.detector.scalers.DaqScalersSequence;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.jnp.hipo4.io.HipoReader;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import java.util.List;
import java.util.ArrayList;

if(args.length<3) {
  System.err.println """
  USAGE: groovy ${this.class.getSimpleName()}.groovy [HIPO file] [output directory] [suffix]
  """
  System.exit(101)
}
def in_file = args[0]
def out_dir = args[1]
def suffix  = args[2]

List<String> filenames = new ArrayList<>();
filenames.add(in_file);

ConstantsManager consts = new ConstantsManager();
consts.init("/runcontrol/fcup","/runcontrol/slm","/runcontrol/helicity","/daq/config/scalers/dsc1","/runcontrol/hwp");
DaqScalersSequence seq = DaqScalersSequence.rebuildSequence(1, consts, filenames);

// seq.fixClockRollover();

def out_file_name   = "${out_dir}/clock_${suffix}.dat";
def out_file        = new File(out_file_name);
def out_file_writer = out_file.newWriter(false);

out_file_writer << [ "timestamp/L", "clock_gated/L", "clock_ungated/L" ].join(':') << '\n';

for(String filename : filenames) {
  HipoReader reader = new HipoReader();
  reader.setTags(1);
  reader.open(filename);
  SchemaFactory schema = reader.getSchemaFactory();

  while(reader.hasNext()) {
    Bank rcfgBank = new Bank(schema.getSchema("RUN::config"));
    Event event = new Event();
    reader.nextEvent(event);
    event.read(rcfgBank);
    long timestamp = -1;
    if (rcfgBank.getRows()>0)
      timestamp = rcfgBank.getLong("timestamp",0);
    DaqScalers ds=seq.get(timestamp);
    if (ds!=null) {
      out_file_writer << [ds.getTimestamp(), ds.dsc2.gatedClock, ds.dsc2.clock].join(' ') << '\n';
    }
  }

  reader.close();
}

out_file_writer.flush();
out_file_writer.close();
System.out.println("WROTE $out_file_name");
