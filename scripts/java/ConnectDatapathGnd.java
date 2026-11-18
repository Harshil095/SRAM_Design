
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.util.*;

public class ConnectDatapathGnd {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("ConnectDatapathGnd", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("SRAM_Core_Datapath{lay}");

                    NodeInst precharg = null;
                    NodeInst contact191 = null;
                    for (Iterator<NodeInst> it = c.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        if (ni.getName().equals("Precharg@0")) precharg = ni;
                        if (ni.getName().equals("contact@191")) contact191 = ni;
                    }

                    PortInst gndPi = precharg.findPortInstFromProto(((Cell)precharg.getProto()).findExport("gnd"));
                    ArcProto m1 = c.getTechnology().findArcProto("Metal-1");

                    ArcInst newArc = ArcInst.makeInstance(m1, getEditingPreferences(), gndPi, contact191.getPortInst(0));
                    System.out.println("Created arc: " + newArc + " from " + newArc.getHead().getLocation() + " to " + newArc.getTail().getLocation());

                    // Test NCC on SRAM_Chip_Final
                    Cell schChip = lib.findNodeProto("SRAM_Chip_Final{sch}");
                    Cell layChip = lib.findNodeProto("SRAM_Chip_Final{lay}");
                    NccOptions opts = new NccOptions();
                    opts.operation = NccOptions.HIER_EACH_CELL;
                    NccResults resChip = Ncc.compare(schChip, null, layChip, null, opts);
                    System.out.println("SRAM_Chip_Final NCC: Match=" + resChip.match() + " Exports=" + resChip.exportMatch() + " Topology=" + resChip.topologyMatch());

                    if (resChip.match()) {
                        Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                        System.out.println("Library successfully saved!");
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(4000);
        System.exit(0);
    }
}
