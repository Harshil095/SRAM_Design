import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.simulation.SimulationTool;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import com.sun.electric.util.math.Orientation;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

public class BuildCleanDiv8Counter {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildCleanDiv8Counter", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File jelibFile = new File("SRAM.jelib");
                    File backupFile = new File("SRAM.jelib.div8_cleanup.bak");
                    Files.copy(jelibFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Backed up SRAM.jelib to SRAM.jelib.div8_cleanup.bak");

                    URL url = jelibFile.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();

                    Technology schemTech = Technology.findTechnology("schematic");
                    ArcProto wire = schemTech.findArcProto("wire");

                    Cell dffIc = lib.findNodeProto("DFF{ic}");
                    Cell invIc = lib.findNodeProto("inverter{ic}");
                    Cell nandIc = lib.findNodeProto("nand4{ic}");

                    NodeProto offPageProto = schemTech.findNodeProto("Off-Page");
                    NodeProto pinProto = schemTech.findNodeProto("Wire_Pin");
                    NodeProto pwrProto = schemTech.findNodeProto("Power");
                    NodeProto gndProto = schemTech.findNodeProto("Ground");

                    Cell oldSch = lib.findNodeProto("Div8_Counter{sch}");
                    if (oldSch != null) {
                        oldSch.kill();
                        System.out.println("Killed old Div8_Counter{sch}");
                    }

                    Cell newDivSch = Cell.makeInstance(lib, "Div8_Counter{sch}");

                    // 1. Global Ports
                    NodeInst pinClk = NodeInst.makeInstance(offPageProto, new Point2D.Double(-120.0, 2.0), 3.0, 3.0, newDivSch);
                    Export.newInst(newDivSch, pinClk.getPortInst(0), "CLK", ep);

                    NodeInst pwrNode = NodeInst.makeInstance(pwrProto, new Point2D.Double(0.0, 30.0), 3.0, 3.0, newDivSch);
                    Export.newInst(newDivSch, pwrNode.getPortInst(0), "vdd", ep);

                    NodeInst gndNode = NodeInst.makeInstance(gndProto, new Point2D.Double(0.0, -40.0), 3.0, 3.0, newDivSch);
                    Export.newInst(newDivSch, gndNode.getPortInst(0), "gnd", ep);

                    // 2. Stages 0, 1, 2
                    double[] xStage = {-60.0, 30.0, 120.0};
                    NodeInst[] dff = new NodeInst[3];
                    NodeInst[] inv = new NodeInst[3];

                    for (int i = 0; i < 3; i++) {
                        double xi = xStage[i];
                        dff[i] = NodeInst.makeInstance(dffIc, new Point2D.Double(xi, 0.0), dffIc.getDefWidth(ep), dffIc.getDefHeight(ep), newDivSch, Orientation.IDENT, "DFF@" + i);
                        inv[i] = NodeInst.makeInstance(invIc, new Point2D.Double(xi + 45.0, 2.0), invIc.getDefWidth(ep), invIc.getDefHeight(ep), newDivSch, Orientation.IDENT, "inverter@" + i);

                        // Local Power and Ground for DFF
                        NodeInst dffPwr = NodeInst.makeInstance(pwrProto, new Point2D.Double(xi - 1.0, -9.0), 3.0, 3.0, newDivSch);
                        ArcInst.makeInstance(wire, ep, dff[i].findPortInstFromProto(dffIc.findExport("vdd")), dffPwr.getPortInst(0));

                        NodeInst dffGnd = NodeInst.makeInstance(gndProto, new Point2D.Double(xi + 1.0, -11.5), 3.0, 3.0, newDivSch);
                        ArcInst.makeInstance(wire, ep, dff[i].findPortInstFromProto(dffIc.findExport("gnd")), dffGnd.getPortInst(0));
                    }

                    // 3. Clock Distribution & Inter-Stage Pipeline
                    // CLK input -> DFF[0].CLK at (xStage[0] - 5.0, 2.0)
                    ArcInst.makeInstance(wire, ep, pinClk.getPortInst(0), dff[0].findPortInstFromProto(dffIc.findExport("CLK")));

                    // Stage 0: INV[0].out at (xStage[0] + 51.5, 2.0) -> tap -> DFF[1].CLK at (xStage[1] - 5.0, 2.0)
                    NodeInst tapClk1 = NodeInst.makeInstance(pinProto, new Point2D.Double(xStage[0] + 55.0, 2.0), 2.0, 2.0, newDivSch);
                    ArcInst.makeInstance(wire, ep, inv[0].findPortInstFromProto(invIc.findExport("out")), tapClk1.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, tapClk1.getPortInst(0), dff[1].findPortInstFromProto(dffIc.findExport("CLK")));

                    // Stage 1: INV[1].out at (xStage[1] + 51.5, 2.0) -> tap -> DFF[2].CLK at (xStage[2] - 5.0, 2.0)
                    NodeInst tapClk2 = NodeInst.makeInstance(pinProto, new Point2D.Double(xStage[1] + 55.0, 2.0), 2.0, 2.0, newDivSch);
                    ArcInst.makeInstance(wire, ep, inv[1].findPortInstFromProto(invIc.findExport("out")), tapClk2.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, tapClk2.getPortInst(0), dff[2].findPortInstFromProto(dffIc.findExport("CLK")));

                    // Stage 2: INV[2].out at (xStage[2] + 51.5, 2.0) -> tap
                    NodeInst tapClk3 = NodeInst.makeInstance(pinProto, new Point2D.Double(xStage[2] + 55.0, 2.0), 2.0, 2.0, newDivSch);
                    ArcInst.makeInstance(wire, ep, inv[2].findPortInstFromProto(invIc.findExport("out")), tapClk3.getPortInst(0));

                    // 4. Toggle Feedback Loops: INV[i].out -> DFF[i].D (at Y = 14.0)
                    NodeInst[] tapFB = {tapClk1, tapClk2, tapClk3};
                    for (int i = 0; i < 3; i++) {
                        double xi = xStage[i];
                        NodeInst c1 = NodeInst.makeInstance(pinProto, new Point2D.Double(xi + 55.0, 14.0), 2.0, 2.0, newDivSch);
                        NodeInst c2 = NodeInst.makeInstance(pinProto, new Point2D.Double(xi - 10.0, 14.0), 2.0, 2.0, newDivSch);
                        NodeInst c3 = NodeInst.makeInstance(pinProto, new Point2D.Double(xi - 10.0, 0.0), 2.0, 2.0, newDivSch);

                        ArcInst.makeInstance(wire, ep, tapFB[i].getPortInst(0), c1.getPortInst(0));
                        ArcInst.makeInstance(wire, ep, c1.getPortInst(0), c2.getPortInst(0));
                        ArcInst.makeInstance(wire, ep, c2.getPortInst(0), c3.getPortInst(0));
                        ArcInst.makeInstance(wire, ep, c3.getPortInst(0), dff[i].findPortInstFromProto(dffIc.findExport("D")));
                    }

                    // 5. Q Connections to Inverter Inputs and Decode Rails
                    double[] yRail = {-22.0, -24.0, -26.0}; // Dedicated Q0, Q1, Q2 rails
                    NodeInst[] qBusTap = new NodeInst[3];

                    for (int i = 0; i < 3; i++) {
                        double xi = xStage[i];
                        // DFF[i].Q is at (xi - 5.0, -2.0)
                        NodeInst qCorner1 = NodeInst.makeInstance(pinProto, new Point2D.Double(xi - 12.0, -2.0), 2.0, 2.0, newDivSch);
                        NodeInst qCorner2 = NodeInst.makeInstance(pinProto, new Point2D.Double(xi - 12.0, -12.0), 2.0, 2.0, newDivSch);
                        NodeInst qJunction = NodeInst.makeInstance(pinProto, new Point2D.Double(xi + 25.0, -12.0), 2.0, 2.0, newDivSch);

                        ArcInst.makeInstance(wire, ep, dff[i].findPortInstFromProto(dffIc.findExport("Q")), qCorner1.getPortInst(0));
                        ArcInst.makeInstance(wire, ep, qCorner1.getPortInst(0), qCorner2.getPortInst(0));
                        ArcInst.makeInstance(wire, ep, qCorner2.getPortInst(0), qJunction.getPortInst(0));

                        // Path to Inverter input at (xi + 41.0, 2.0):
                        NodeInst qToInvCorner = NodeInst.makeInstance(pinProto, new Point2D.Double(xi + 25.0, 2.0), 2.0, 2.0, newDivSch);
                        ArcInst.makeInstance(wire, ep, qJunction.getPortInst(0), qToInvCorner.getPortInst(0));
                        ArcInst.makeInstance(wire, ep, qToInvCorner.getPortInst(0), inv[i].findPortInstFromProto(invIc.findExport("in")));

                        // Path to Decode rail at yRail[i]:
                        NodeInst qDrop = NodeInst.makeInstance(pinProto, new Point2D.Double(xi + 25.0, yRail[i]), 2.0, 2.0, newDivSch);
                        NodeInst qEnd = NodeInst.makeInstance(pinProto, new Point2D.Double(195.0, yRail[i]), 2.0, 2.0, newDivSch);
                        ArcInst.makeInstance(wire, ep, qJunction.getPortInst(0), qDrop.getPortInst(0));
                        ArcInst.makeInstance(wire, ep, qDrop.getPortInst(0), qEnd.getPortInst(0));
                        qBusTap[i] = qEnd;
                    }

                    // 6. Decode Stage: NAND4 + INV3 + EXECUTE
                    double xNand = 210.0;
                    double yNand = -25.0;
                    NodeInst nand = NodeInst.makeInstance(nandIc, new Point2D.Double(xNand, yNand), nandIc.getDefWidth(ep), nandIc.getDefHeight(ep), newDivSch, Orientation.IDENT, "nand4@0");

                    // Connect Q0 -> NAND4.A at (xNand - 5.0, -22.0)
                    ArcInst.makeInstance(wire, ep, qBusTap[0].getPortInst(0), nand.findPortInstFromProto(nandIc.findExport("A")));

                    // Connect Q1 -> NAND4.B at (xNand - 5.0, -24.0)
                    ArcInst.makeInstance(wire, ep, qBusTap[1].getPortInst(0), nand.findPortInstFromProto(nandIc.findExport("B")));

                    // Connect Q2 -> NAND4.C at (xNand - 5.0, -26.0)
                    ArcInst.makeInstance(wire, ep, qBusTap[2].getPortInst(0), nand.findPortInstFromProto(nandIc.findExport("C")));

                    // Connect VDD -> NAND4.D at (xNand - 5.0, -28.0)
                    NodeInst nandPwr = NodeInst.makeInstance(pwrProto, new Point2D.Double(xNand - 15.0, -28.0), 3.0, 3.0, newDivSch);
                    ArcInst.makeInstance(wire, ep, nandPwr.getPortInst(0), nand.findPortInstFromProto(nandIc.findExport("D")));

                    // NAND4.out at (xNand + 5.0, -27.0) -> invOut.in at (xInvOut - 4.0, -27.0)
                    double xInvOut = 240.0;
                    double yInvOut = -27.0;
                    NodeInst invOut = NodeInst.makeInstance(invIc, new Point2D.Double(xInvOut, yInvOut), invIc.getDefWidth(ep), invIc.getDefHeight(ep), newDivSch, Orientation.IDENT, "inverter@3");
                    ArcInst.makeInstance(wire, ep, nand.findPortInstFromProto(nandIc.findExport("out")), invOut.findPortInstFromProto(invIc.findExport("in")));

                    // invOut.out at (xInvOut + 6.5, -27.0) -> EXECUTE pin at (270.0, -27.0)
                    NodeInst pinExec = NodeInst.makeInstance(offPageProto, new Point2D.Double(270.0, -27.0), 3.0, 3.0, newDivSch);
                    ArcInst.makeInstance(wire, ep, invOut.findPortInstFromProto(invIc.findExport("out")), pinExec.getPortInst(0));
                    Export.newInst(newDivSch, pinExec.getPortInst(0), "EXECUTE", ep);

                    // 7. Verify Manhattan Routing
                    int nonManhattan = 0;
                    for (Iterator<ArcInst> it = newDivSch.getArcs(); it.hasNext(); ) {
                        ArcInst ai = it.next();
                        EPoint h = ai.getHead().getPortInst().getCenter();
                        EPoint t = ai.getTail().getPortInst().getCenter();
                        double dx = Math.abs(h.getX() - t.getX());
                        double dy = Math.abs(h.getY() - t.getY());
                        if (dx > 0.001 && dy > 0.001) {
                            System.out.printf("NON-MANHATTAN ARC: %s from %s to %s (dx=%.1f, dy=%.1f)\n",
                                ai.getName(), h, t, dx, dy);
                            nonManhattan++;
                        }
                    }
                    System.out.println("Non-manhattan arcs count: " + nonManhattan);
                    if (nonManhattan > 0) {
                        throw new RuntimeException("Aborting save: Found non-manhattan arcs!");
                    }

                    // 8. NCC Verification with Div8_Counter{lay}
                    Cell divLay = lib.findNodeProto("Div8_Counter{lay}");
                    NccOptions opts = new NccOptions();
                    opts.checkSizes = false;
                    NccResults res = Ncc.compare(newDivSch, null, divLay, null, opts);
                    System.out.println("=== Div8_Counter{sch} vs Div8_Counter{lay} NCC ===");
                    System.out.println("Match: " + res.match());
                    System.out.println("Summary: " + res.summary(false));
                    System.out.println("Export Match: " + res.exportMatch());
                    System.out.println("Topology Match: " + res.topologyMatch());
                    if (!res.match()) {
                        throw new RuntimeException("Div8_Counter NCC match FAILED!");
                    }

                    // 9. Commit & Write to jelib
                    System.out.println("Writing updated library to SRAM.jelib...");
                    Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                    System.out.println("SRAM.jelib saved successfully!");

                    // 10. Export Div8_Counter.spi
                    System.out.println("Exporting SPICE netlist: Div8_Counter.spi...");
                    Spice.SpicePreferences sp = (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, newDivSch, true, null);
                    sp.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                    sp.level = "3";
                    sp.doOutput(newDivSch, null, "Div8_Counter.spi");
                    System.out.println("Div8_Counter.spi exported successfully!");

                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(3000);
    }
}
