import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

public class BuildCleanDFF {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildCleanDFF", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File jelibFile = new File("SRAM.jelib");
                    File backupFile = new File("SRAM.jelib.dff_cleanup.bak");
                    Files.copy(jelibFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Backed up SRAM.jelib to SRAM.jelib.dff_cleanup.bak");

                    URL url = jelibFile.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();

                    Technology schemTech = Technology.findTechnology("schematic");
                    Technology genTech = Technology.findTechnology("generic");
                    ArcProto wire = schemTech.findArcProto("wire");

                    Cell invIc = lib.findNodeProto("inverter{ic}");
                    Cell tgIc = lib.findNodeProto("tgate{ic}");
                    NodeProto offPageProto = schemTech.findNodeProto("Off-Page");
                    NodeProto pinProto = schemTech.findNodeProto("Wire_Pin");
                    NodeProto pwrProto = schemTech.findNodeProto("Power");
                    NodeProto gndProto = schemTech.findNodeProto("Ground");
                    NodeProto invisPinProto = genTech.findNodeProto("Invisible-Pin");

                    Cell dffSch = lib.findNodeProto("DFF{sch}");
                    if (dffSch != null) {
                        dffSch.kill();
                        System.out.println("Killed old DFF{sch}");
                    }

                    Cell newDffSch = Cell.makeInstance(lib, "DFF{sch}");

                    // 1. Off-Page Ports & Global Power/Ground
                    NodeInst dIn = NodeInst.makeInstance(offPageProto, new Point2D.Double(-130.0, 15.0), 3.0, 3.0, newDffSch);
                    Export.newInst(newDffSch, dIn.getPortInst(0), "D", ep);

                    NodeInst clkIn = NodeInst.makeInstance(offPageProto, new Point2D.Double(-130.0, -25.0), 3.0, 3.0, newDffSch);
                    Export.newInst(newDffSch, clkIn.getPortInst(0), "CLK", ep);

                    NodeInst qOut = NodeInst.makeInstance(offPageProto, new Point2D.Double(125.0, 15.0), 3.0, 3.0, newDffSch);
                    Export.newInst(newDffSch, qOut.getPortInst(0), "Q", ep);

                    NodeInst mainPwr = NodeInst.makeInstance(pwrProto, new Point2D.Double(0.0, 35.0), 3.0, 3.0, newDffSch);
                    Export.newInst(newDffSch, mainPwr.getPortInst(0), "vdd", ep);

                    NodeInst mainGnd = NodeInst.makeInstance(gndProto, new Point2D.Double(0.0, -35.0), 3.0, 3.0, newDffSch);
                    Export.newInst(newDffSch, mainGnd.getPortInst(0), "gnd", ep);

                    // SPICE Simulation Card on Invisible Pin
                    String[] spiceCard = {
                        ".include ./tsmc018.lib",
                        ".param VDD = 1.8",
                        "VDD1 vdd 0 DC 1.8",
                        "* DFF transient test: clock 100MHz (period 10ns)",
                        "VCLK CLK 0 PULSE(0 1.8 1n 0.1n 0.1n 4.9n 10n)",
                        "* Data pattern: D toggles with setup/hold relative to clock edges",
                        "VD D 0 PWL(0n 0 7n 0 7.2n 1.8 23n 1.8 23.2n 0 35n 0 35.2n 1.8 60n 1.8)",
                        "CQ Q 0 20f",
                        ".tran 0.1n 60n"
                    };
                    NodeInst spicePin = NodeInst.makeInstance(invisPinProto, new Point2D.Double(-130.0, 35.0), 1.0, 1.0, newDffSch);
                    spicePin.newDisplayVar(Variable.newKey("SIM_spice_card"), spiceCard, ep);

                    // 2. Gate Instances
                    // Forward Path at Y = 15.0:
                    NodeInst tg1 = NodeInst.makeInstance(tgIc, new Point2D.Double(-105.0, 15.0), tgIc.getDefWidth(ep), tgIc.getDefHeight(ep), newDffSch, Orientation.R, "TG1");
                    NodeInst inv3 = NodeInst.makeInstance(invIc, new Point2D.Double(-65.0, 15.0), 10.0, 10.0, newDffSch, Orientation.IDENT, "INV3");
                    NodeInst tg3 = NodeInst.makeInstance(tgIc, new Point2D.Double(0.0, 15.0), tgIc.getDefWidth(ep), tgIc.getDefHeight(ep), newDffSch, Orientation.R, "TG3");
                    NodeInst inv5 = NodeInst.makeInstance(invIc, new Point2D.Double(40.0, 15.0), 10.0, 10.0, newDffSch, Orientation.IDENT, "INV5");
                    NodeInst inv6 = NodeInst.makeInstance(invIc, new Point2D.Double(85.0, 15.0), 10.0, 10.0, newDffSch, Orientation.IDENT, "INV6");

                    // Feedback Path at Y = 2.0:
                    NodeInst inv4 = NodeInst.makeInstance(invIc, new Point2D.Double(-55.0, 2.0), 10.0, 10.0, newDffSch, Orientation.RR, "INV4");
                    NodeInst tg2 = NodeInst.makeInstance(tgIc, new Point2D.Double(-75.0, 2.0), tgIc.getDefWidth(ep), tgIc.getDefHeight(ep), newDffSch, Orientation.RRR, "TG2");
                    NodeInst tg4 = NodeInst.makeInstance(tgIc, new Point2D.Double(40.0, 2.0), tgIc.getDefWidth(ep), tgIc.getDefHeight(ep), newDffSch, Orientation.RRR, "TG4");

                    // Clock Generation at Y = -25.0:
                    NodeInst inv1 = NodeInst.makeInstance(invIc, new Point2D.Double(-105.0, -25.0), 10.0, 10.0, newDffSch, Orientation.IDENT, "INV1");
                    NodeInst inv2 = NodeInst.makeInstance(invIc, new Point2D.Double(-65.0, -25.0), 10.0, 10.0, newDffSch, Orientation.IDENT, "INV2");

                    // 3. Forward Data Wiring (Strictly Manhattan on Y = 15.0):
                    ArcInst.makeInstance(wire, ep, dIn.getPortInst(0), tg1.findPortInstFromProto(tgIc.findExport("IN")));

                    NodeInst pinMin = NodeInst.makeInstance(pinProto, new Point2D.Double(-85.0, 15.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg1.findPortInstFromProto(tgIc.findExport("OUT")), pinMin.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, pinMin.getPortInst(0), inv3.findPortInstFromProto(invIc.findExport("in")));

                    NodeInst pinMout = NodeInst.makeInstance(pinProto, new Point2D.Double(-45.0, 15.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, inv3.findPortInstFromProto(invIc.findExport("out")), pinMout.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, pinMout.getPortInst(0), tg3.findPortInstFromProto(tgIc.findExport("IN")));

                    NodeInst pinSin = NodeInst.makeInstance(pinProto, new Point2D.Double(20.0, 15.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg3.findPortInstFromProto(tgIc.findExport("OUT")), pinSin.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, pinSin.getPortInst(0), inv5.findPortInstFromProto(invIc.findExport("in")));

                    NodeInst pinSout = NodeInst.makeInstance(pinProto, new Point2D.Double(60.0, 15.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, inv5.findPortInstFromProto(invIc.findExport("out")), pinSout.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, pinSout.getPortInst(0), inv6.findPortInstFromProto(invIc.findExport("in")));

                    ArcInst.makeInstance(wire, ep, inv6.findPortInstFromProto(invIc.findExport("out")), qOut.getPortInst(0));

                    // 4. Feedback Loops (Strict Manhattan rectangular loops):
                    // Master Feedback:
                    NodeInst fbM_c1 = NodeInst.makeInstance(pinProto, new Point2D.Double(-45.0, 2.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, pinMout.getPortInst(0), fbM_c1.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, fbM_c1.getPortInst(0), inv4.findPortInstFromProto(invIc.findExport("in")));

                    ArcInst.makeInstance(wire, ep, inv4.findPortInstFromProto(invIc.findExport("out")), tg2.findPortInstFromProto(tgIc.findExport("IN")));

                    NodeInst fbM_c2 = NodeInst.makeInstance(pinProto, new Point2D.Double(-85.0, 2.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg2.findPortInstFromProto(tgIc.findExport("OUT")), fbM_c2.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, fbM_c2.getPortInst(0), pinMin.getPortInst(0));

                    // Slave Feedback:
                    NodeInst fbS_c1 = NodeInst.makeInstance(pinProto, new Point2D.Double(60.0, 2.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, pinSout.getPortInst(0), fbS_c1.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, fbS_c1.getPortInst(0), tg4.findPortInstFromProto(tgIc.findExport("IN")));

                    NodeInst fbS_c2 = NodeInst.makeInstance(pinProto, new Point2D.Double(20.0, 2.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg4.findPortInstFromProto(tgIc.findExport("OUT")), fbS_c2.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, fbS_c2.getPortInst(0), pinSin.getPortInst(0));

                    // 5. Clock Generation & Distribution:
                    ArcInst.makeInstance(wire, ep, clkIn.getPortInst(0), inv1.findPortInstFromProto(invIc.findExport("in")));

                    NodeInst pinClkb = NodeInst.makeInstance(pinProto, new Point2D.Double(-85.0, -25.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, inv1.findPortInstFromProto(invIc.findExport("out")), pinClkb.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, pinClkb.getPortInst(0), inv2.findPortInstFromProto(invIc.findExport("in")));

                    NodeInst pinClkBuf = NodeInst.makeInstance(pinProto, new Point2D.Double(-45.0, -25.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, inv2.findPortInstFromProto(invIc.findExport("out")), pinClkBuf.getPortInst(0));

                    // CLKB Rail at Y = -10.0:
                    NodeInst rClkb_tap = NodeInst.makeInstance(pinProto, new Point2D.Double(-85.0, -10.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, pinClkb.getPortInst(0), rClkb_tap.getPortInst(0));

                    // CLK_buf Rail at Y = -15.0:
                    NodeInst rClkBuf_tap = NodeInst.makeInstance(pinProto, new Point2D.Double(-45.0, -15.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, pinClkBuf.getPortInst(0), rClkBuf_tap.getPortInst(0));

                    // CLKB rail horizontal taps:
                    NodeInst rClkb_tg1 = NodeInst.makeInstance(pinProto, new Point2D.Double(-105.0, -10.0), 2.0, 2.0, newDffSch);
                    NodeInst rClkb_tg2 = NodeInst.makeInstance(pinProto, new Point2D.Double(-75.0, -10.0), 2.0, 2.0, newDffSch);
                    NodeInst rClkb_tg3 = NodeInst.makeInstance(pinProto, new Point2D.Double(-10.0, -10.0), 2.0, 2.0, newDffSch);
                    NodeInst rClkb_tg4 = NodeInst.makeInstance(pinProto, new Point2D.Double(48.0, -10.0), 2.0, 2.0, newDffSch);

                    ArcInst.makeInstance(wire, ep, rClkb_tg1.getPortInst(0), rClkb_tap.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, rClkb_tap.getPortInst(0), rClkb_tg2.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, rClkb_tg2.getPortInst(0), rClkb_tg3.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, rClkb_tg3.getPortInst(0), rClkb_tg4.getPortInst(0));

                    // CLK_buf rail horizontal taps:
                    NodeInst rClkBuf_tg1 = NodeInst.makeInstance(pinProto, new Point2D.Double(-118.0, -15.0), 2.0, 2.0, newDffSch);
                    NodeInst rClkBuf_tg2 = NodeInst.makeInstance(pinProto, new Point2D.Double(-67.0, -15.0), 2.0, 2.0, newDffSch);
                    NodeInst rClkBuf_tg3 = NodeInst.makeInstance(pinProto, new Point2D.Double(0.0, -15.0), 2.0, 2.0, newDffSch);
                    NodeInst rClkBuf_tg4 = NodeInst.makeInstance(pinProto, new Point2D.Double(40.0, -15.0), 2.0, 2.0, newDffSch);

                    ArcInst.makeInstance(wire, ep, rClkBuf_tg1.getPortInst(0), rClkBuf_tg2.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, rClkBuf_tg2.getPortInst(0), rClkBuf_tap.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, rClkBuf_tap.getPortInst(0), rClkBuf_tg3.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, rClkBuf_tg3.getPortInst(0), rClkBuf_tg4.getPortInst(0));

                    // Clock drops to TG control ports:
                    // TG1: EN -> CLKB (straight vertical drop)
                    ArcInst.makeInstance(wire, ep, tg1.findPortInstFromProto(tgIc.findExport("EN")), rClkb_tg1.getPortInst(0));

                    // TG1: ENB -> CLK_buf (up to Y=22, left to X=-118, down to Y=-15)
                    NodeInst tg1_enb_c1 = NodeInst.makeInstance(pinProto, new Point2D.Double(-105.0, 22.0), 2.0, 2.0, newDffSch);
                    NodeInst tg1_enb_c2 = NodeInst.makeInstance(pinProto, new Point2D.Double(-118.0, 22.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg1.findPortInstFromProto(tgIc.findExport("ENB")), tg1_enb_c1.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, tg1_enb_c1.getPortInst(0), tg1_enb_c2.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, tg1_enb_c2.getPortInst(0), rClkBuf_tg1.getPortInst(0));

                    // TG2: ENB -> CLKB (straight vertical drop from Y=-1.0 to Y=-10.0)
                    ArcInst.makeInstance(wire, ep, tg2.findPortInstFromProto(tgIc.findExport("ENB")), rClkb_tg2.getPortInst(0));

                    // TG2: EN -> CLK_buf (up to Y=8.0, right to X=-67.0, down to Y=-15.0)
                    NodeInst tg2_en_c1 = NodeInst.makeInstance(pinProto, new Point2D.Double(-75.0, 8.0), 2.0, 2.0, newDffSch);
                    NodeInst tg2_en_c2 = NodeInst.makeInstance(pinProto, new Point2D.Double(-67.0, 8.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg2.findPortInstFromProto(tgIc.findExport("EN")), tg2_en_c1.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, tg2_en_c1.getPortInst(0), tg2_en_c2.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, tg2_en_c2.getPortInst(0), rClkBuf_tg2.getPortInst(0));

                    // TG3: EN -> CLK_buf (straight vertical drop from Y=10.0 to Y=-15.0)
                    ArcInst.makeInstance(wire, ep, tg3.findPortInstFromProto(tgIc.findExport("EN")), rClkBuf_tg3.getPortInst(0));

                    // TG3: ENB -> CLKB (up to Y=22, left to X=-10.0, down to Y=-10.0)
                    NodeInst tg3_enb_c1 = NodeInst.makeInstance(pinProto, new Point2D.Double(0.0, 22.0), 2.0, 2.0, newDffSch);
                    NodeInst tg3_enb_c2 = NodeInst.makeInstance(pinProto, new Point2D.Double(-10.0, 22.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg3.findPortInstFromProto(tgIc.findExport("ENB")), tg3_enb_c1.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, tg3_enb_c1.getPortInst(0), tg3_enb_c2.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, tg3_enb_c2.getPortInst(0), rClkb_tg3.getPortInst(0));

                    // TG4: ENB -> CLK_buf (straight vertical drop from Y=-1.0 to Y=-15.0)
                    ArcInst.makeInstance(wire, ep, tg4.findPortInstFromProto(tgIc.findExport("ENB")), rClkBuf_tg4.getPortInst(0));

                    // TG4: EN -> CLKB (up to Y=8.0, right to X=48.0, down to Y=-10.0)
                    NodeInst tg4_en_c1 = NodeInst.makeInstance(pinProto, new Point2D.Double(40.0, 8.0), 2.0, 2.0, newDffSch);
                    NodeInst tg4_en_c2 = NodeInst.makeInstance(pinProto, new Point2D.Double(48.0, 8.0), 2.0, 2.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg4.findPortInstFromProto(tgIc.findExport("EN")), tg4_en_c1.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, tg4_en_c1.getPortInst(0), tg4_en_c2.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, tg4_en_c2.getPortInst(0), rClkb_tg4.getPortInst(0));

                    // 6. Power and Ground Connections for TG1..TG4 (100% vertical):
                    // TG1: vdd at (-112.0, 13.0), gnd at (-100.5, 13.0)
                    NodeInst pwr_tg1 = NodeInst.makeInstance(pwrProto, new Point2D.Double(-112.0, 18.0), 3.0, 3.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg1.findPortInstFromProto(tgIc.findExport("vdd")), pwr_tg1.getPortInst(0));

                    NodeInst gnd_tg1 = NodeInst.makeInstance(gndProto, new Point2D.Double(-100.5, 6.5), 3.0, 3.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg1.findPortInstFromProto(tgIc.findExport("gnd")), gnd_tg1.getPortInst(0));

                    // TG2: vdd at (-68.0, 4.0), gnd at (-79.5, 4.0)
                    NodeInst pwr_tg2 = NodeInst.makeInstance(pwrProto, new Point2D.Double(-68.0, 9.0), 3.0, 3.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg2.findPortInstFromProto(tgIc.findExport("vdd")), pwr_tg2.getPortInst(0));

                    NodeInst gnd_tg2 = NodeInst.makeInstance(gndProto, new Point2D.Double(-79.5, -2.5), 3.0, 3.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg2.findPortInstFromProto(tgIc.findExport("gnd")), gnd_tg2.getPortInst(0));

                    // TG3: vdd at (-7.0, 13.0), gnd at (4.5, 13.0)
                    NodeInst pwr_tg3 = NodeInst.makeInstance(pwrProto, new Point2D.Double(-7.0, 18.0), 3.0, 3.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg3.findPortInstFromProto(tgIc.findExport("vdd")), pwr_tg3.getPortInst(0));

                    NodeInst gnd_tg3 = NodeInst.makeInstance(gndProto, new Point2D.Double(4.5, 6.5), 3.0, 3.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg3.findPortInstFromProto(tgIc.findExport("gnd")), gnd_tg3.getPortInst(0));

                    // TG4: vdd at (47.0, 4.0), gnd at (35.5, 4.0)
                    NodeInst pwr_tg4 = NodeInst.makeInstance(pwrProto, new Point2D.Double(47.0, 9.0), 3.0, 3.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg4.findPortInstFromProto(tgIc.findExport("vdd")), pwr_tg4.getPortInst(0));

                    NodeInst gnd_tg4 = NodeInst.makeInstance(gndProto, new Point2D.Double(35.5, -2.5), 3.0, 3.0, newDffSch);
                    ArcInst.makeInstance(wire, ep, tg4.findPortInstFromProto(tgIc.findExport("gnd")), gnd_tg4.getPortInst(0));

                    // Verify Manhattan routing
                    int nonManhattan = 0;
                    for (Iterator<ArcInst> it = newDffSch.getArcs(); it.hasNext(); ) {
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

                    // 7. Verification:
                    // A. DFF{sch} vs DFF{lay}
                    Cell dffLay = lib.findNodeProto("DFF{lay}");
                    NccOptions opts = new NccOptions();
                    opts.checkSizes = true;
                    NccResults resDFF = Ncc.compare(newDffSch, null, dffLay, null, opts);
                    System.out.println("=== NCC DFF{sch} vs DFF{lay} ===");
                    System.out.println("Match: " + resDFF.match());
                    System.out.println("Summary: " + resDFF.summary(false));
                    if (!resDFF.match()) {
                        throw new RuntimeException("DFF NCC match FAILED!");
                    }

                    // B. ShiftReg_8bit{sch} vs ShiftReg_8bit{lay}
                    Cell srSch = lib.findNodeProto("ShiftReg_8bit{sch}");
                    Cell srLay = lib.findNodeProto("ShiftReg_8bit{lay}");
                    NccResults resSR = Ncc.compare(srSch, null, srLay, null, opts);
                    System.out.println("=== NCC ShiftReg_8bit ===");
                    System.out.println("Match: " + resSR.match());
                    if (!resSR.match()) {
                        throw new RuntimeException("ShiftReg_8bit NCC match FAILED!");
                    }

                    // C. Div8_Counter{sch} vs Div8_Counter{lay}
                    Cell divSch = lib.findNodeProto("Div8_Counter{sch}");
                    Cell divLay = lib.findNodeProto("Div8_Counter{lay}");
                    NccOptions divOpts = new NccOptions();
                    divOpts.checkSizes = false;
                    NccResults resDiv = Ncc.compare(divSch, null, divLay, null, divOpts);
                    System.out.println("=== NCC Div8_Counter ===");
                    System.out.println("Match: " + resDiv.match());
                    if (!resDiv.match()) {
                        throw new RuntimeException("Div8_Counter NCC match FAILED!");
                    }

                    // 8. Commit and save to jelib
                    System.out.println("Writing updated library to SRAM.jelib...");
                    Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                    System.out.println("SRAM.jelib saved successfully!");

                    // 9. Export DFF.spi
                    System.out.println("Exporting SPICE netlist: DFF.spi...");
                    Spice.SpicePreferences sp = (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, newDffSch, true, null);
                    sp.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                    sp.level = "3";
                    sp.doOutput(newDffSch, null, "DFF.spi");
                    System.out.println("DFF.spi exported successfully!");

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
