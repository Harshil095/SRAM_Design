import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.simulation.SimulationTool;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.IconParameters;
import com.sun.electric.tool.user.User;
import com.sun.electric.util.math.Orientation;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;

public class BuildDiv8Counter {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildDiv8Counter", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();

                    Cell dffLay = lib.findNodeProto("DFF{lay}");
                    Cell invLay = lib.findNodeProto("inverter{lay}");
                    Cell nandLay = lib.findNodeProto("nand4{lay}");
                    Cell dffSch = lib.findNodeProto("DFF{sch}");
                    Cell invSch = lib.findNodeProto("inverter{sch}");
                    Cell nandSch = lib.findNodeProto("nand4{sch}");
                    Cell dffIc = lib.findNodeProto("DFF{ic}");
                    Cell invIc = lib.findNodeProto("inverter{ic}");
                    Cell nandIc = lib.findNodeProto("nand4{ic}");

                    Technology mocmos = Technology.getMocmosTechnology();
                    ArcProto m1 = mocmos.findArcProto("Metal-1");
                    ArcProto m2 = mocmos.findArcProto("Metal-2");
                    ArcProto m3 = mocmos.findArcProto("Metal-3");
                    NodeProto via1 = mocmos.findNodeProto("Metal-1-Metal-2-Con");
                    NodeProto via2 = mocmos.findNodeProto("Metal-2-Metal-3-Con");
                    NodeProto m1Pin = mocmos.findNodeProto("Metal-1-Pin");
                    NodeProto m2Pin = mocmos.findNodeProto("Metal-2-Pin");

                    // Remove old Div8_Counter cells if present
                    Cell oldSch = lib.findNodeProto("Div8_Counter{sch}");
                    Cell oldLay = lib.findNodeProto("Div8_Counter{lay}");
                    Cell oldIc  = lib.findNodeProto("Div8_Counter{ic}");
                    if (oldIc != null) oldIc.kill();
                    if (oldSch != null) oldSch.kill();
                    if (oldLay != null) oldLay.kill();

                    // ==========================================
                    // 1. SCHEMATIC: Div8_Counter{sch}
                    // ==========================================
                    Cell schCell = Cell.makeInstance(lib, "Div8_Counter{sch}");
                    Technology generic = schCell.getTechnology();
                    ArcProto schWire = generic.findArcProto("wire");
                    NodeProto offPage = generic.findNodeProto("Off-Page");
                    NodeProto pwrProto = generic.findNodeProto("Power");
                    NodeProto gndProto = generic.findNodeProto("Ground");

                    NodeInst[] schDFF = new NodeInst[3];
                    for (int i = 0; i < 3; i++) {
                        schDFF[i] = NodeInst.makeInstance(dffIc, new Point2D.Double(i * 40.0, 0.0),
                            dffIc.getDefWidth(ep), dffIc.getDefHeight(ep), schCell, Orientation.IDENT, null);
                    }
                    NodeInst[] schInvFB = new NodeInst[3];
                    for (int i = 0; i < 3; i++) {
                        schInvFB[i] = NodeInst.makeInstance(invIc, new Point2D.Double(i * 40.0 + 15.0, 20.0),
                            invIc.getDefWidth(ep), invIc.getDefHeight(ep), schCell, Orientation.IDENT, null);
                    }
                    NodeInst schNand = NodeInst.makeInstance(nandIc, new Point2D.Double(140.0, 0.0),
                        nandIc.getDefWidth(ep), nandIc.getDefHeight(ep), schCell, Orientation.IDENT, null);
                    NodeInst schInvOut = NodeInst.makeInstance(invIc, new Point2D.Double(170.0, 0.0),
                        invIc.getDefWidth(ep), invIc.getDefHeight(ep), schCell, Orientation.IDENT, null);

                    NodeInst pwrNode = NodeInst.makeInstance(pwrProto, new Point2D.Double(-20.0, 30.0),
                        pwrProto.getDefWidth(ep), pwrProto.getDefHeight(ep), schCell, Orientation.IDENT, null);
                    NodeInst gndNode = NodeInst.makeInstance(gndProto, new Point2D.Double(-20.0, -30.0),
                        gndProto.getDefWidth(ep), gndProto.getDefHeight(ep), schCell, Orientation.IDENT, null);
                    Export.newInst(schCell, pwrNode.getPortInst(0), "vdd", ep);
                    Export.newInst(schCell, gndNode.getPortInst(0), "gnd", ep);

                    for (int i = 0; i < 3; i++) {
                        ArcInst.makeInstance(schWire, ep, pwrNode.getPortInst(0), schDFF[i].findPortInstFromProto(dffIc.findExport("vdd")));
                        ArcInst.makeInstance(schWire, ep, gndNode.getPortInst(0), schDFF[i].findPortInstFromProto(dffIc.findExport("gnd")));
                    }

                    NodeInst pinClk = NodeInst.makeInstance(offPage, new Point2D.Double(-20.0, 0.0),
                        offPage.getDefWidth(ep), offPage.getDefHeight(ep), schCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(schWire, ep, pinClk.getPortInst(0), schDFF[0].findPortInstFromProto(dffIc.findExport("CLK")));
                    Export.newInst(schCell, pinClk.getPortInst(0), "CLK", ep);

                    // Stage 0: Q0 -> inv0.in -> inv0.out -> DFF0.D & DFF1.CLK
                    ArcInst.makeInstance(schWire, ep, schDFF[0].findPortInstFromProto(dffIc.findExport("Q")), schInvFB[0].findPortInstFromProto(invIc.findExport("in")));
                    ArcInst.makeInstance(schWire, ep, schInvFB[0].findPortInstFromProto(invIc.findExport("out")), schDFF[0].findPortInstFromProto(dffIc.findExport("D")));
                    ArcInst.makeInstance(schWire, ep, schInvFB[0].findPortInstFromProto(invIc.findExport("out")), schDFF[1].findPortInstFromProto(dffIc.findExport("CLK")));

                    // Stage 1: Q1 -> inv1.in -> inv1.out -> DFF1.D & DFF2.CLK
                    ArcInst.makeInstance(schWire, ep, schDFF[1].findPortInstFromProto(dffIc.findExport("Q")), schInvFB[1].findPortInstFromProto(invIc.findExport("in")));
                    ArcInst.makeInstance(schWire, ep, schInvFB[1].findPortInstFromProto(invIc.findExport("out")), schDFF[1].findPortInstFromProto(dffIc.findExport("D")));
                    ArcInst.makeInstance(schWire, ep, schInvFB[1].findPortInstFromProto(invIc.findExport("out")), schDFF[2].findPortInstFromProto(dffIc.findExport("CLK")));

                    // Stage 2: Q2 -> inv2.in -> inv2.out -> DFF2.D
                    ArcInst.makeInstance(schWire, ep, schDFF[2].findPortInstFromProto(dffIc.findExport("Q")), schInvFB[2].findPortInstFromProto(invIc.findExport("in")));
                    ArcInst.makeInstance(schWire, ep, schInvFB[2].findPortInstFromProto(invIc.findExport("out")), schDFF[2].findPortInstFromProto(dffIc.findExport("D")));

                    // NAND4 inputs: A = Q0, B = Q1, C = Q2, D = vdd
                    ArcInst.makeInstance(schWire, ep, schDFF[0].findPortInstFromProto(dffIc.findExport("Q")), schNand.findPortInstFromProto(nandIc.findExport("A")));
                    ArcInst.makeInstance(schWire, ep, schDFF[1].findPortInstFromProto(dffIc.findExport("Q")), schNand.findPortInstFromProto(nandIc.findExport("B")));
                    ArcInst.makeInstance(schWire, ep, schDFF[2].findPortInstFromProto(dffIc.findExport("Q")), schNand.findPortInstFromProto(nandIc.findExport("C")));
                    ArcInst.makeInstance(schWire, ep, pwrNode.getPortInst(0), schNand.findPortInstFromProto(nandIc.findExport("D")));

                    // NAND4 out -> invOut.in
                    ArcInst.makeInstance(schWire, ep, schNand.findPortInstFromProto(nandIc.findExport("out")), schInvOut.findPortInstFromProto(invIc.findExport("in")));

                    // invOut.out -> EXECUTE
                    NodeInst pinExec = NodeInst.makeInstance(offPage, new Point2D.Double(200.0, 0.0),
                        offPage.getDefWidth(ep), offPage.getDefHeight(ep), schCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(schWire, ep, schInvOut.findPortInstFromProto(invIc.findExport("out")), pinExec.getPortInst(0));
                    Export.newInst(schCell, pinExec.getPortInst(0), "EXECUTE", ep);

                    // ==========================================
                    // 2. ICON: Div8_Counter{ic}
                    // ==========================================
                    IconParameters ip = IconParameters.makeInstance(true);
                    Cell icCell = ip.makeIconForCell(schCell, ep);
                    System.out.println("Icon generated: " + icCell.getName());

                    // ==========================================
                    // 3. LAYOUT: Div8_Counter{lay}
                    // ==========================================
                    Cell layCell = Cell.makeInstance(lib, "Div8_Counter{lay}");
                    double pitch = 620.0;
                    NodeInst[] dff = new NodeInst[3];
                    for (int i = 0; i < 3; i++) {
                        dff[i] = NodeInst.makeInstance(dffLay, new Point2D.Double(i * pitch, 0.0),
                            dffLay.getBounds().getWidth(), dffLay.getBounds().getHeight(), layCell, Orientation.IDENT, null);
                    }

                    // 4 Inverters sequentially:
                    // inv0 (1880), inv1 (2000), inv2 (2120), invOut (2240)
                    double[] invX = {1880.0, 2000.0, 2120.0, 2240.0};
                    NodeInst[] invs = new NodeInst[4];
                    for (int i = 0; i < 4; i++) {
                        invs[i] = NodeInst.makeInstance(invLay, new Point2D.Double(invX[i], 0.0),
                            invLay.getBounds().getWidth(), invLay.getBounds().getHeight(), layCell, Orientation.IDENT, null);
                    }
                    NodeInst inv0 = invs[0];
                    NodeInst inv1 = invs[1];
                    NodeInst inv2 = invs[2];
                    NodeInst invOut = invs[3];

                    // NAND gate at X = 2500.0:
                    double nandX = 2500.0;
                    NodeInst nand = NodeInst.makeInstance(nandLay, new Point2D.Double(nandX, 0.0),
                        nandLay.getBounds().getWidth(), nandLay.getBounds().getHeight(), layCell, Orientation.IDENT, null);

                    // Power Rails:
                    // VDD rail at Y = 60.0
                    NodeInst[] vddDff = new NodeInst[3];
                    for (int i = 0; i < 3; i++) {
                        vddDff[i] = NodeInst.makeInstance(m1Pin, new Point2D.Double(i * pitch + 120.0, 60.0), m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst aV = ArcInst.makeInstance(m1, ep, dff[i].findPortInstFromProto(dffLay.findExport("vdd")), vddDff[i].getPortInst(0));
                        aV.setLambdaBaseWidth(4.0);
                        if (i > 0) {
                            ArcInst aH = ArcInst.makeInstance(m1, ep, vddDff[i-1].getPortInst(0), vddDff[i].getPortInst(0));
                            aH.setLambdaBaseWidth(4.0);
                        }
                    }
                    Export.newInst(layCell, vddDff[0].getPortInst(0), "vdd", ep);

                    NodeInst prevV = vddDff[2];
                    NodeInst[] vddTaps = new NodeInst[4];
                    for (int i = 0; i < 4; i++) {
                        double rx = invX[i] - 40.0;
                        vddTaps[i] = NodeInst.makeInstance(via1, new Point2D.Double(rx, 60.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, prevV.getPortInst(0), vddTaps[i].getPortInst(0)).setLambdaBaseWidth(4.0);
                        prevV = vddTaps[i];

                        NodeInst vVdd = NodeInst.makeInstance(via1, new Point2D.Double(rx, 21.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, invs[i].findPortInstFromProto(invLay.findExport("vdd")), vVdd.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vVdd.getPortInst(0), vddTaps[i].getPortInst(0)).setLambdaBaseWidth(4.0);
                    }

                    // VDD rail to NAND on METAL-2!
                    NodeInst vddNandTap = NodeInst.makeInstance(via1, new Point2D.Double(2501.5, 60.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, prevV.getPortInst(0), vddNandTap.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // nand.vdd to (2501.5, 28.5) on M1, then M2 to (2501.5, 60.0)
                    NodeInst vNandVdd = NodeInst.makeInstance(via1, new Point2D.Double(2501.5, 28.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, nand.findPortInstFromProto(nandLay.findExport("vdd")), vNandVdd.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, vNandVdd.getPortInst(0), vddNandTap.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // nand.D to (2501.5, -52.5) on M1, then M2 to (2501.5, 28.5)
                    NodeInst vNandD = NodeInst.makeInstance(via1, new Point2D.Double(2501.5, -52.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, nand.findPortInstFromProto(nandLay.findExport("D")), vNandD.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, vNandD.getPortInst(0), vNandVdd.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // GND rail at Y = -80.0
                    NodeInst[] gndDff = new NodeInst[3];
                    for (int i = 0; i < 3; i++) {
                        gndDff[i] = NodeInst.makeInstance(m1Pin, new Point2D.Double(i * pitch + 120.0, -80.0), m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst aG = ArcInst.makeInstance(m1, ep, dff[i].findPortInstFromProto(dffLay.findExport("gnd")), gndDff[i].getPortInst(0));
                        aG.setLambdaBaseWidth(4.0);
                        if (i > 0) {
                            ArcInst aH = ArcInst.makeInstance(m1, ep, gndDff[i-1].getPortInst(0), gndDff[i].getPortInst(0));
                            aH.setLambdaBaseWidth(4.0);
                        }
                    }
                    Export.newInst(layCell, gndDff[0].getPortInst(0), "gnd", ep);

                    NodeInst prevG = gndDff[2];
                    NodeInst[] gndTaps = new NodeInst[4];
                    for (int i = 0; i < 4; i++) {
                        double rx = invX[i] - 40.0;
                        gndTaps[i] = NodeInst.makeInstance(via1, new Point2D.Double(rx, -80.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, prevG.getPortInst(0), gndTaps[i].getPortInst(0)).setLambdaBaseWidth(4.0);
                        prevG = gndTaps[i];

                        NodeInst vGnd = NodeInst.makeInstance(via1, new Point2D.Double(rx, -23.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, invs[i].findPortInstFromProto(invLay.findExport("gnd")), vGnd.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vGnd.getPortInst(0), gndTaps[i].getPortInst(0)).setLambdaBaseWidth(4.0);
                    }

                    // GND rail to NAND on METAL-2!
                    NodeInst vNandG = NodeInst.makeInstance(via1, new Point2D.Double(2472.0, -28.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, nand.findPortInstFromProto(nandLay.findExport("gnd")), vNandG.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst nandGRail = NodeInst.makeInstance(via1, new Point2D.Double(2472.0, -80.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vNandG.getPortInst(0), nandGRail.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, prevG.getPortInst(0), nandGRail.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // CLK input pin
                    NodeInst vClk = NodeInst.makeInstance(via1, new Point2D.Double(-45.0, -1.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff[0].findPortInstFromProto(dffLay.findExport("CLK")), vClk.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst pClk = NodeInst.makeInstance(m2Pin, new Point2D.Double(-45.0, -20.0), m2Pin.getDefWidth(ep), m2Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vClk.getPortInst(0), pClk.getPortInst(0)).setLambdaBaseWidth(4.0);
                    Export.newInst(layCell, pClk.getPortInst(0), "CLK", ep);

                    // Signal Tracks on Metal-3 at Y >= 100:
                    // NET Q0 (Track Y = 100.0)
                    NodeInst vQ0_src_v1 = NodeInst.makeInstance(via1, new Point2D.Double(565.0, 50.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff[0].findPortInstFromProto(dffLay.findExport("Q")), vQ0_src_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ0_src_v2 = NodeInst.makeInstance(via2, new Point2D.Double(565.0, 100.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ0_src_v1.getPortInst(0), vQ0_src_v2.getPortInst(0)).setLambdaBaseWidth(6.0);

                    NodeInst vQ0_inv0_v1 = NodeInst.makeInstance(via1, new Point2D.Double(1820.0, -1.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, inv0.findPortInstFromProto(invLay.findExport("in")), vQ0_inv0_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ0_inv0_v2 = NodeInst.makeInstance(via2, new Point2D.Double(1820.0, 100.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ0_inv0_v1.getPortInst(0), vQ0_inv0_v2.getPortInst(0)).setLambdaBaseWidth(6.0);
                    ArcInst.makeInstance(m3, ep, vQ0_src_v2.getPortInst(0), vQ0_inv0_v2.getPortInst(0)).setLambdaBaseWidth(6.0);

                    // nand.A drop directly at X = 2444.0 on Metal-3 down to Y = -60.0!
                    NodeInst vQ0_drop_top = NodeInst.makeInstance(via2, new Point2D.Double(2444.0, 100.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m3, ep, vQ0_inv0_v2.getPortInst(0), vQ0_drop_top.getPortInst(0)).setLambdaBaseWidth(6.0);
                    NodeInst vQ0_drop_bot_v2 = NodeInst.makeInstance(via2, new Point2D.Double(2444.0, -60.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m3, ep, vQ0_drop_top.getPortInst(0), vQ0_drop_bot_v2.getPortInst(0)).setLambdaBaseWidth(6.0);
                    NodeInst vQ0_drop_bot_v1 = NodeInst.makeInstance(via1, new Point2D.Double(2444.0, -60.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ0_drop_bot_v2.getPortInst(0), vQ0_drop_bot_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    // Vertical M1 from (2444.0, -60.0) up to nand.A at (2444.0, -52.5)
                    ArcInst.makeInstance(m1, ep, nand.findPortInstFromProto(nandLay.findExport("A")), vQ0_drop_bot_v1.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // NET Q0_b (Track Y = 120.0)
                    NodeInst vQ0b_src_v1 = NodeInst.makeInstance(via1, new Point2D.Double(1900.0, -1.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, inv0.findPortInstFromProto(invLay.findExport("out")), vQ0b_src_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ0b_src_v2 = NodeInst.makeInstance(via2, new Point2D.Double(1900.0, 120.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ0b_src_v1.getPortInst(0), vQ0b_src_v2.getPortInst(0)).setLambdaBaseWidth(6.0);

                    NodeInst vQ0b_clk1_v1 = NodeInst.makeInstance(via1, new Point2D.Double(575.0, -1.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff[1].findPortInstFromProto(dffLay.findExport("CLK")), vQ0b_clk1_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ0b_clk1_v2 = NodeInst.makeInstance(via2, new Point2D.Double(575.0, 120.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ0b_clk1_v1.getPortInst(0), vQ0b_clk1_v2.getPortInst(0)).setLambdaBaseWidth(6.0);
                    ArcInst.makeInstance(m3, ep, vQ0b_src_v2.getPortInst(0), vQ0b_clk1_v2.getPortInst(0)).setLambdaBaseWidth(6.0);

                    NodeInst vQ0b_d0_v1 = NodeInst.makeInstance(via1, new Point2D.Double(80.0, 10.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff[0].findPortInstFromProto(dffLay.findExport("D")), vQ0b_d0_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ0b_d0_v2 = NodeInst.makeInstance(via2, new Point2D.Double(80.0, 120.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ0b_d0_v1.getPortInst(0), vQ0b_d0_v2.getPortInst(0)).setLambdaBaseWidth(6.0);
                    ArcInst.makeInstance(m3, ep, vQ0b_clk1_v2.getPortInst(0), vQ0b_d0_v2.getPortInst(0)).setLambdaBaseWidth(6.0);

                    // NET Q1 (Track Y = 140.0)
                    NodeInst vQ1_src_v1 = NodeInst.makeInstance(via1, new Point2D.Double(1185.0, 50.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff[1].findPortInstFromProto(dffLay.findExport("Q")), vQ1_src_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ1_src_v2 = NodeInst.makeInstance(via2, new Point2D.Double(1185.0, 140.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ1_src_v1.getPortInst(0), vQ1_src_v2.getPortInst(0)).setLambdaBaseWidth(6.0);

                    NodeInst vQ1_inv1_v1 = NodeInst.makeInstance(via1, new Point2D.Double(1940.0, -1.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, inv1.findPortInstFromProto(invLay.findExport("in")), vQ1_inv1_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ1_inv1_v2 = NodeInst.makeInstance(via2, new Point2D.Double(1940.0, 140.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ1_inv1_v1.getPortInst(0), vQ1_inv1_v2.getPortInst(0)).setLambdaBaseWidth(6.0);
                    ArcInst.makeInstance(m3, ep, vQ1_src_v2.getPortInst(0), vQ1_inv1_v2.getPortInst(0)).setLambdaBaseWidth(6.0);

                    // nand.B drop directly at X = 2458.5 on Metal-3 down to Y = -60.0!
                    NodeInst vQ1_drop_top = NodeInst.makeInstance(via2, new Point2D.Double(2458.5, 140.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m3, ep, vQ1_inv1_v2.getPortInst(0), vQ1_drop_top.getPortInst(0)).setLambdaBaseWidth(6.0);
                    NodeInst vQ1_drop_bot_v2 = NodeInst.makeInstance(via2, new Point2D.Double(2458.5, -60.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m3, ep, vQ1_drop_top.getPortInst(0), vQ1_drop_bot_v2.getPortInst(0)).setLambdaBaseWidth(6.0);
                    NodeInst vQ1_drop_bot_v1 = NodeInst.makeInstance(via1, new Point2D.Double(2458.5, -60.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ1_drop_bot_v2.getPortInst(0), vQ1_drop_bot_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    // Vertical M1 from (2458.5, -60.0) up to nand.B at (2458.5, -52.5)
                    ArcInst.makeInstance(m1, ep, nand.findPortInstFromProto(nandLay.findExport("B")), vQ1_drop_bot_v1.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // NET Q1_b (Track Y = 160.0)
                    NodeInst vQ1b_src_v1 = NodeInst.makeInstance(via1, new Point2D.Double(2020.0, -1.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, inv1.findPortInstFromProto(invLay.findExport("out")), vQ1b_src_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ1b_src_v2 = NodeInst.makeInstance(via2, new Point2D.Double(2020.0, 160.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ1b_src_v1.getPortInst(0), vQ1b_src_v2.getPortInst(0)).setLambdaBaseWidth(6.0);

                    NodeInst vQ1b_clk2_v1 = NodeInst.makeInstance(via1, new Point2D.Double(1195.0, -1.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff[2].findPortInstFromProto(dffLay.findExport("CLK")), vQ1b_clk2_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ1b_clk2_v2 = NodeInst.makeInstance(via2, new Point2D.Double(1195.0, 160.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ1b_clk2_v1.getPortInst(0), vQ1b_clk2_v2.getPortInst(0)).setLambdaBaseWidth(6.0);
                    ArcInst.makeInstance(m3, ep, vQ1b_src_v2.getPortInst(0), vQ1b_clk2_v2.getPortInst(0)).setLambdaBaseWidth(6.0);

                    NodeInst vQ1b_d1_v1 = NodeInst.makeInstance(via1, new Point2D.Double(700.0, 10.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff[1].findPortInstFromProto(dffLay.findExport("D")), vQ1b_d1_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ1b_d1_v2 = NodeInst.makeInstance(via2, new Point2D.Double(700.0, 160.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ1b_d1_v1.getPortInst(0), vQ1b_d1_v2.getPortInst(0)).setLambdaBaseWidth(6.0);
                    ArcInst.makeInstance(m3, ep, vQ1b_clk2_v2.getPortInst(0), vQ1b_d1_v2.getPortInst(0)).setLambdaBaseWidth(6.0);

                    // NET Q2 (Track Y = 180.0)
                    NodeInst vQ2_src_v1 = NodeInst.makeInstance(via1, new Point2D.Double(1805.0, 50.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff[2].findPortInstFromProto(dffLay.findExport("Q")), vQ2_src_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ2_src_v2 = NodeInst.makeInstance(via2, new Point2D.Double(1805.0, 180.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ2_src_v1.getPortInst(0), vQ2_src_v2.getPortInst(0)).setLambdaBaseWidth(6.0);

                    NodeInst vQ2_inv2_v1 = NodeInst.makeInstance(via1, new Point2D.Double(2060.0, -1.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, inv2.findPortInstFromProto(invLay.findExport("in")), vQ2_inv2_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ2_inv2_v2 = NodeInst.makeInstance(via2, new Point2D.Double(2060.0, 180.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ2_inv2_v1.getPortInst(0), vQ2_inv2_v2.getPortInst(0)).setLambdaBaseWidth(6.0);
                    ArcInst.makeInstance(m3, ep, vQ2_src_v2.getPortInst(0), vQ2_inv2_v2.getPortInst(0)).setLambdaBaseWidth(6.0);

                    // nand.C drop directly at X = 2488.5 on Metal-3 down to Y = -60.0!
                    NodeInst vQ2_drop_top = NodeInst.makeInstance(via2, new Point2D.Double(2488.5, 180.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m3, ep, vQ2_inv2_v2.getPortInst(0), vQ2_drop_top.getPortInst(0)).setLambdaBaseWidth(6.0);
                    NodeInst vQ2_drop_bot_v2 = NodeInst.makeInstance(via2, new Point2D.Double(2488.5, -60.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m3, ep, vQ2_drop_top.getPortInst(0), vQ2_drop_bot_v2.getPortInst(0)).setLambdaBaseWidth(6.0);
                    NodeInst vQ2_drop_bot_v1 = NodeInst.makeInstance(via1, new Point2D.Double(2488.5, -60.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ2_drop_bot_v2.getPortInst(0), vQ2_drop_bot_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    // Vertical M1 from (2488.5, -60.0) up to nand.C at (2488.5, -52.5)
                    ArcInst.makeInstance(m1, ep, nand.findPortInstFromProto(nandLay.findExport("C")), vQ2_drop_bot_v1.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // NET Q2_b (Track Y = 200.0)
                    NodeInst vQ2b_src_v1 = NodeInst.makeInstance(via1, new Point2D.Double(2140.0, -1.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, inv2.findPortInstFromProto(invLay.findExport("out")), vQ2b_src_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ2b_src_v2 = NodeInst.makeInstance(via2, new Point2D.Double(2140.0, 200.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ2b_src_v1.getPortInst(0), vQ2b_src_v2.getPortInst(0)).setLambdaBaseWidth(6.0);

                    NodeInst vQ2b_d2_v1 = NodeInst.makeInstance(via1, new Point2D.Double(1320.0, 10.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff[2].findPortInstFromProto(dffLay.findExport("D")), vQ2b_d2_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ2b_d2_v2 = NodeInst.makeInstance(via2, new Point2D.Double(1320.0, 200.0), via2.getDefWidth(ep), via2.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ2b_d2_v1.getPortInst(0), vQ2b_d2_v2.getPortInst(0)).setLambdaBaseWidth(6.0);
                    ArcInst.makeInstance(m3, ep, vQ2b_src_v2.getPortInst(0), vQ2b_d2_v2.getPortInst(0)).setLambdaBaseWidth(6.0);

                    // NET NAND_OUT -> INV_OUT.IN (Metal-2 channel at Y = 48.0!)
                    // nand.out is at nandX - 28 = 2472.0, 45.0 (Metal-2-Pin)
                    // invOut.in is at 2240 - 25 = 2215.0, -1.0
                    NodeInst vInvOut_In_v1 = NodeInst.makeInstance(via1, new Point2D.Double(2215.0, -1.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, invOut.findPortInstFromProto(invLay.findExport("in")), vInvOut_In_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vInvOut_Corner = NodeInst.makeInstance(m2Pin, new Point2D.Double(2215.0, 48.0), m2Pin.getDefWidth(ep), m2Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vInvOut_In_v1.getPortInst(0), vInvOut_Corner.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vNandOut_Corner = NodeInst.makeInstance(m2Pin, new Point2D.Double(2472.0, 48.0), m2Pin.getDefWidth(ep), m2Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, nand.findPortInstFromProto(nandLay.findExport("out")), vNandOut_Corner.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, vInvOut_Corner.getPortInst(0), vNandOut_Corner.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // EXECUTE OUTPUT PIN
                    NodeInst vExec = NodeInst.makeInstance(via1, new Point2D.Double(2260.0, -1.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, invOut.findPortInstFromProto(invLay.findExport("out")), vExec.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst pExec = NodeInst.makeInstance(m2Pin, new Point2D.Double(2260.0, -20.0), m2Pin.getDefWidth(ep), m2Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vExec.getPortInst(0), pExec.getPortInst(0)).setLambdaBaseWidth(4.0);
                    Export.newInst(layCell, pExec.getPortInst(0), "EXECUTE", ep);

                    // ==========================================
                    // 4. VERIFICATION: DRC & NCC
                    // ==========================================
                    System.out.println("Running DRC on Div8_Counter{lay}...");
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, layCell, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println("Div8_Counter DRC Errors: " + errLog.getNumErrors() + ", Warnings: " + errLog.getNumWarnings());

                    System.out.println("Running NCC between Div8_Counter{sch} and Div8_Counter{lay}...");
                    NccOptions opts = new NccOptions();
                    opts.checkSizes = false;
                    NccResults res = Ncc.compare(schCell, null, layCell, null, opts);
                    System.out.println("Div8_Counter NCC match = " + res.match());
                    System.out.println("Div8_Counter NCC summary: " + res.summary(false));
                    System.out.println("Export match = " + res.exportMatch());
                    System.out.println("Topology match = " + res.topologyMatch());

                    if (errLog.getNumErrors() == 0 && res.match()) {
                        System.out.println("SUCCESS! Writing Div8_Counter to SRAM.jelib...");
                        Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);

                        System.out.println("Exporting SPICE netlist Div8_Counter.spi...");
                        Spice.SpicePreferences sp = (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, schCell, true, null);
                        sp.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                        sp.level = "3";
                        sp.doOutput(schCell, null, "Div8_Counter.spi");
                        System.out.println("Div8_Counter.spi exported successfully.");
                    } else {
                        System.err.println("Verification FAILED! Library NOT saved.");
                    }

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
