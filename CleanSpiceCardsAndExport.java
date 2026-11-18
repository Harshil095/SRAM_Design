
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.simulation.SimulationTool;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.util.*;

public class CleanSpiceCardsAndExport {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("CleanSpiceCardsAndExport", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    int strippedCount = 0;
                    for (Iterator<Cell> cit = lib.getCells(); cit.hasNext(); ) {
                        Cell c = cit.next();
                        for (Iterator<NodeInst> nit = c.getNodes(); nit.hasNext(); ) {
                            NodeInst ni = nit.next();
                            List<Variable> toDel = new ArrayList<Variable>();
                            for (Iterator<Variable> vit = ni.getVariables(); vit.hasNext(); ) {
                                Variable v = vit.next();
                                if (v.getKey().getName().toUpperCase().contains("SPICE")) {
                                    toDel.add(v);
                                }
                            }
                            for (Variable v : toDel) {
                                ni.delVar(v.getKey());
                                strippedCount++;
                                System.out.println("Stripped " + v.getKey().getName() + " from " + c.getName() + " node " + ni.getName());
                            }
                        }
                    }
                    System.out.println("Total SPICE cards stripped from cells: " + strippedCount);

                    Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                    System.out.println("SRAM.jelib saved clean!");

                    Cell chipSch = lib.findNodeProto("SRAM_Chip_Final{sch}");
                    Spice.SpicePreferences sp = (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, chipSch, true, null);
                    sp.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                    sp.level = "3";
                    sp.doOutput(chipSch, null, "SRAM_Chip_Final.spi");
                    System.out.println("Clean SRAM_Chip_Final.spi successfully re-exported!");

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
