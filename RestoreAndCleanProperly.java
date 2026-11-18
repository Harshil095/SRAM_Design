
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.simulation.SimulationTool;
import com.sun.electric.tool.user.User;
import com.sun.electric.util.math.Orientation;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.*;

public class RestoreAndCleanProperly {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("RestoreAndCleanProperly", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    // Restore from bak_before_precharge_fix
                    File fBak = new File("SRAM.jelib.bak_before_precharge_fix");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), fBak.toURI().toURL(), null, FileType.JELIB, false);
                    
                    // 1. Fix Precharge_Bank_16 pin@0
                    Cell preLay = lib.findNodeProto("Precharge_Bank_16{lay}");
                    NodeInst oldSub = null;
                    for (Iterator<NodeInst> it = preLay.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        if (ni.getName().equals("substr@0") || ni.getName().equals("pin@0")) oldSub = ni;
                    }
                    for (Iterator<Export> it = preLay.getExports(); it.hasNext(); ) {
                        Export exp = it.next();
                        if (exp.getName().equals("gnd")) { exp.kill(); break; }
                    }
                    if (oldSub != null) oldSub.kill();

                    PrimitiveNode m1PinProto = (PrimitiveNode) preLay.getTechnology().findNodeProto("Metal-1-Pin");
                    Point2D pt = new Point2D.Double(-60.0, -20.0);
                    NodeInst newPin = NodeInst.makeInstance(m1PinProto, getEditingPreferences(), pt, 3.0, 3.0, preLay, Orientation.IDENT, "pin@0");
                    PortInst pInst = newPin.getPortInst(0);
                    Export.newInstanceNoIcon(preLay, pInst, "gnd", getEditingPreferences(), com.sun.electric.database.prototype.PortCharacteristic.GND);

                    // 2. Connect Precharg@0.gnd in SRAM_Core_Datapath{lay}
                    Cell dpLay = lib.findNodeProto("SRAM_Core_Datapath{lay}");
                    NodeInst precharg = null, contact191 = null;
                    for (Iterator<NodeInst> it = dpLay.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        if (ni.getName().equals("Precharg@0")) precharg = ni;
                        if (ni.getName().equals("contact@191")) contact191 = ni;
                    }
                    PortInst gndPi = precharg.findPortInstFromProto(((Cell)precharg.getProto()).findExport("gnd"));
                    ArcProto m1 = dpLay.getTechnology().findArcProto("Metal-1");
                    ArcInst.makeInstance(m1, getEditingPreferences(), gndPi, contact191.getPortInst(0));

                    // 3. ONLY delete SIM_spice_card (do NOT touch SIM_spice_model)
                    int strippedCards = 0;
                    for (Iterator<Cell> cit = lib.getCells(); cit.hasNext(); ) {
                        Cell c = cit.next();
                        for (Iterator<NodeInst> nit = c.getNodes(); nit.hasNext(); ) {
                            NodeInst ni = nit.next();
                            List<Variable> toDel = new ArrayList<Variable>();
                            for (Iterator<Variable> vit = ni.getVariables(); vit.hasNext(); ) {
                                Variable v = vit.next();
                                if (v.getKey().getName().equals("SIM_spice_card")) {
                                    toDel.add(v);
                                }
                            }
                            for (Variable v : toDel) {
                                ni.delVar(v.getKey());
                                strippedCards++;
                                System.out.println("Deleted SIM_spice_card from " + c.getName() + " node " + ni.getName());
                            }
                        }
                    }
                    System.out.println("Total SIM_spice_cards removed: " + strippedCards);

                    // Save to SRAM.jelib
                    File fOut = new File("SRAM.jelib");
                    Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                    System.out.println("Clean SRAM.jelib written!");

                    // Verify NCC
                    Cell schChip = lib.findNodeProto("SRAM_Chip_Final{sch}");
                    Cell layChip = lib.findNodeProto("SRAM_Chip_Final{lay}");
                    NccOptions opts = new NccOptions();
                    opts.operation = NccOptions.HIER_EACH_CELL;
                    NccResults resChip = Ncc.compare(schChip, null, layChip, null, opts);
                    System.out.println("SRAM_Chip_Final NCC: Match=" + resChip.match() + " Exports=" + resChip.exportMatch() + " Topology=" + resChip.topologyMatch());

                    // Re-export SPICE deck
                    Spice.SpicePreferences sp = (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, schChip, true, null);
                    sp.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                    sp.level = "3";
                    sp.doOutput(schChip, null, "SRAM_Chip_Final.spi");
                    System.out.println("Clean SRAM_Chip_Final.spi re-exported successfully!");

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
