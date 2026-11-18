import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.database.prototype.NodeProto;
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
import com.sun.electric.tool.user.IconParameters;
import com.sun.electric.tool.user.User;
import com.sun.electric.util.math.Orientation;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;
import java.util.*;

public class BuildDecoder4x16 {
    public static final double[] WL_Y = {
        5.0, 153.0, 175.0, 323.0, 345.0, 493.0, 515.0, 663.0,
        685.0, 833.0, 855.0, 1003.0, 1025.0, 1173.0, 1195.0, 1343.0
    };

    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildDecoder4x16", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();
                    Technology mocmos = Technology.getMocmosTechnology();

                    Cell nandIc  = lib.findNodeProto("nand4{ic}");
                    Cell nandLay = lib.findNodeProto("nand4{lay}");
                    Cell invIc   = lib.findNodeProto("inverter{ic}");
                    Cell invLay  = lib.findNodeProto("inverter{lay}");

                    // Delete existing Decoder_4x16 cells if present
                    Cell oldSch = lib.findNodeProto("Decoder_4x16{sch}");
                    if (oldSch != null) oldSch.kill();
                    Cell oldLay = lib.findNodeProto("Decoder_4x16{lay}");
                    if (oldLay != null) oldLay.kill();
                    Cell oldIc  = lib.findNodeProto("Decoder_4x16{ic}");
                    if (oldIc != null) oldIc.kill();

                    // ==========================================
                    // 1. SCHEMATIC: Decoder_4x16{sch} (Clean Bus Architecture)
                    // ==========================================
                    System.out.println("Building Decoder_4x16{sch}...");
                    Cell decSch = Cell.makeInstance(lib, "Decoder_4x16{sch}");
                    ArcProto wire = decSch.getTechnology().findArcProto("wire");
                    NodeProto pinProto = decSch.getTechnology().findNodeProto("Wire_Pin");
                    NodeProto offPageProto = decSch.getTechnology().findNodeProto("Off-Page");
                    NodeProto pwrProto = decSch.getTechnology().findNodeProto("Power");
                    NodeProto gndProto = decSch.getTechnology().findNodeProto("Ground");

                    // 8 Vertical Rails at X coordinates:
                    // Rail 0: A[3]  at X = -80
                    // Rail 1: Ab[3] at X = -70
                    // Rail 2: A[2]  at X = -60
                    // Rail 3: Ab[2] at X = -50
                    // Rail 4: A[1]  at X = -40
                    // Rail 5: Ab[1] at X = -30
                    // Rail 6: A[0]  at X = -20
                    // Rail 7: Ab[0] at X = -10
                    double[] xRail = {-80.0, -70.0, -60.0, -50.0, -40.0, -30.0, -20.0, -10.0};

                    List<List<NodeInst>> railNodes = new ArrayList<>();
                    for (int r = 0; r < 8; r++) railNodes.add(new ArrayList<>());

                    // 1. Address Inverter stage at top (Y in [310, 285, 260, 235])
                    double[] yAddr = {235.0, 260.0, 285.0, 310.0}; // for b = 0, 1, 2, 3
                    for (int b = 3; b >= 0; b--) {
                        double y = yAddr[b];
                        int rA = (3 - b) * 2;
                        int rAb = rA + 1;

                        // Input Off-Page
                        NodeInst inPin = NodeInst.makeInstance(offPageProto, new Point2D.Double(-170.0, y), 3.0, 3.0, decSch);
                        Export.newInst(decSch, inPin.getPortInst(0), "A[" + b + "]", ep);

                        // Branch pin before inverter
                        NodeInst branchA = NodeInst.makeInstance(pinProto, new Point2D.Double(-145.0, y), 2.0, 2.0, decSch);
                        ArcInst.makeInstance(wire, ep, inPin.getPortInst(0), branchA.getPortInst(0));

                        // Inverter
                        NodeInst inv = NodeInst.makeInstance(invIc, new Point2D.Double(-125.0, y), 10.0, 10.0, decSch);
                        ArcInst.makeInstance(wire, ep, branchA.getPortInst(0), inv.findPortInstFromProto(invIc.findExport("in")));

                        // Tap at top of Rail A
                        NodeInst topA = NodeInst.makeInstance(pinProto, new Point2D.Double(xRail[rA], y), 2.0, 2.0, decSch);
                        ArcInst.makeInstance(wire, ep, branchA.getPortInst(0), topA.getPortInst(0));
                        railNodes.get(rA).add(topA);

                        // Tap at top of Rail Ab
                        NodeInst topAb = NodeInst.makeInstance(pinProto, new Point2D.Double(xRail[rAb], y), 2.0, 2.0, decSch);
                        ArcInst.makeInstance(wire, ep, inv.findPortInstFromProto(invIc.findExport("out")), topAb.getPortInst(0));
                        railNodes.get(rAb).add(topAb);
                    }

                    // 2. 16 Decode Stages
                    double xNand = 40.0;
                    double xInvOut = 80.0;
                    double xOutPin = 120.0;

                    for (int i = 0; i < 16; i++) {
                        double y = 200.0 - i * 25.0;

                        NodeInst nand = NodeInst.makeInstance(nandIc, new Point2D.Double(xNand, y), 10.0, 10.0, decSch);
                        NodeInst invOut = NodeInst.makeInstance(invIc, new Point2D.Double(xInvOut, y - 2.0), 10.0, 10.0, decSch);

                        // NAND out to Inv in
                        ArcInst.makeInstance(wire, ep, nand.findPortInstFromProto(nandIc.findExport("out")),
                                                       invOut.findPortInstFromProto(invIc.findExport("in")));

                        // Inv out to OUT[i] pin
                        NodeInst outPin = NodeInst.makeInstance(offPageProto, new Point2D.Double(xOutPin, y - 2.0), 3.0, 3.0, decSch);
                        ArcInst.makeInstance(wire, ep, invOut.findPortInstFromProto(invIc.findExport("out")), outPin.getPortInst(0));
                        Export.newInst(decSch, outPin.getPortInst(0), "OUT[" + i + "]", ep);

                        // Decode connections:
                        // Port A (bit 3) at y + 3.0
                        int b3 = (i >> 3) & 1;
                        int r3 = (b3 == 1) ? 0 : 1;
                        double yA = y + 3.0;
                        NodeInst tapA = NodeInst.makeInstance(pinProto, new Point2D.Double(xRail[r3], yA), 2.0, 2.0, decSch);
                        railNodes.get(r3).add(tapA);
                        ArcInst.makeInstance(wire, ep, tapA.getPortInst(0), nand.findPortInstFromProto(nandIc.findExport("A")));

                        // Port B (bit 2) at y + 1.0
                        int b2 = (i >> 2) & 1;
                        int r2 = (b2 == 1) ? 2 : 3;
                        double yB = y + 1.0;
                        NodeInst tapB = NodeInst.makeInstance(pinProto, new Point2D.Double(xRail[r2], yB), 2.0, 2.0, decSch);
                        railNodes.get(r2).add(tapB);
                        ArcInst.makeInstance(wire, ep, tapB.getPortInst(0), nand.findPortInstFromProto(nandIc.findExport("B")));

                        // Port C (bit 1) at y - 1.0
                        int b1 = (i >> 1) & 1;
                        int r1 = (b1 == 1) ? 4 : 5;
                        double yC = y - 1.0;
                        NodeInst tapC = NodeInst.makeInstance(pinProto, new Point2D.Double(xRail[r1], yC), 2.0, 2.0, decSch);
                        railNodes.get(r1).add(tapC);
                        ArcInst.makeInstance(wire, ep, tapC.getPortInst(0), nand.findPortInstFromProto(nandIc.findExport("C")));

                        // Port D (bit 0) at y - 3.0
                        int b0 = (i >> 0) & 1;
                        int r0 = (b0 == 1) ? 6 : 7;
                        double yD = y - 3.0;
                        NodeInst tapD = NodeInst.makeInstance(pinProto, new Point2D.Double(xRail[r0], yD), 2.0, 2.0, decSch);
                        railNodes.get(r0).add(tapD);
                        ArcInst.makeInstance(wire, ep, tapD.getPortInst(0), nand.findPortInstFromProto(nandIc.findExport("D")));
                    }

                    // Add bottom termination pins so all 8 rails have clean aligned ends
                    for (int r = 0; r < 8; r++) {
                        NodeInst botPin = NodeInst.makeInstance(pinProto, new Point2D.Double(xRail[r], -190.0), 2.0, 2.0, decSch);
                        railNodes.get(r).add(botPin);
                    }

                    // 3. Connect vertical rails from top to bottom
                    for (int r = 0; r < 8; r++) {
                        List<NodeInst> nodes = railNodes.get(r);
                        nodes.sort((n1, n2) -> Double.compare(n2.getAnchorCenter().getY(), n1.getAnchorCenter().getY()));
                        for (int k = 0; k < nodes.size() - 1; k++) {
                            ArcInst.makeInstance(wire, ep, nodes.get(k).getPortInst(0), nodes.get(k + 1).getPortInst(0));
                        }
                    }

                    // 4. Power and Ground symbols
                    NodeInst pwrNode = NodeInst.makeInstance(pwrProto, new Point2D.Double(-145.0, 350.0), 3.0, 3.0, decSch);
                    NodeInst gndNode = NodeInst.makeInstance(gndProto, new Point2D.Double(-125.0, 350.0), 3.0, 3.0, decSch);
                    Export.newInst(decSch, pwrNode.getPortInst(0), "vdd", ep);
                    Export.newInst(decSch, gndNode.getPortInst(0), "gnd", ep);

                    // ==========================================
                    // 2. ICON: Decoder_4x16{ic}
                    // ==========================================
                    System.out.println("Generating Decoder_4x16{ic}...");
                    IconParameters ip = IconParameters.makeInstance(true);
                    Cell decIc = ip.makeIconForCell(decSch, ep);
                    System.out.println("Decoder_4x16{ic} created: " + decIc);

                    // ==========================================
                    // 3. LAYOUT: Decoder_4x16{lay}
                    // ==========================================
                    System.out.println("Building Decoder_4x16{lay}...");
                    Cell decLay = Cell.makeInstance(lib, "Decoder_4x16{lay}");
                    ArcProto m1 = mocmos.findArcProto("Metal-1");
                    ArcProto m2 = mocmos.findArcProto("Metal-2");
                    ArcProto m3 = mocmos.findArcProto("Metal-3");
                    NodeProto via1 = mocmos.findNodeProto("Metal-1-Metal-2-Con");
                    NodeProto via2 = mocmos.findNodeProto("Metal-2-Metal-3-Con");
                    NodeProto m1PinLay = mocmos.findNodeProto("Metal-1-Pin");

                    // Bus 0 at x in [-120, -50] (pitch 10)
                    double[] xBus0 = new double[8];
                    for (int b = 0; b < 8; b++) xBus0[b] = -120.0 + b * 10.0;

                    // Column 0 at xNand0 = 50, xInv0 = 130
                    double xNand0 = 50.0;
                    double xInv0  = 130.0;

                    // Bus 1 at x in [220, 290] (pitch 10)
                    double[] xBus1 = new double[8];
                    for (int b = 0; b < 8; b++) xBus1[b] = 220.0 + b * 10.0;

                    // Column 1 at xNand1 = 390, xInv1 = 470
                    double xNand1 = 390.0;
                    double xInv1  = 470.0;
                    double xOutEdge = 540.0;

                    List<List<NodeInst>> busNodes0 = new ArrayList<>();
                    List<List<NodeInst>> busNodes1 = new ArrayList<>();
                    for (int b = 0; b < 8; b++) {
                        busNodes0.add(new ArrayList<>());
                        busNodes1.add(new ArrayList<>());
                    }

                    // 4 Address Inverters placed at x = -200, y = -100 - k * 60
                    NodeInst[] layInvA = new NodeInst[4];
                    for (int k = 0; k < 4; k++) {
                        double xInv = -200.0;
                        double yInv = -100.0 - k * 60.0;
                        layInvA[k] = NodeInst.makeInstance(invLay, new Point2D.Double(xInv, yInv),
                            invLay.getBounds().getWidth(), invLay.getBounds().getHeight(), decLay, Orientation.IDENT, null);

                        // Input A[k] jumps to M2 drop at xDrop = -245.0 - k * 10.0
                        double xDrop = -245.0 - k * 10.0;
                        double yTieA = -330.0 - k * 15.0;

                        NodeInst vDropTop = NodeInst.makeInstance(via1, new Point2D.Double(xDrop, yInv - 1.0), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, layInvA[k].findPortInstFromProto(invLay.findExport("in")), vDropTop.getPortInst(0));

                        // External input pin for A[k] at (xDrop - 15.0, yInv - 1.0)
                        NodeInst pExtPin = NodeInst.makeInstance(m1PinLay, new Point2D.Double(xDrop - 15.0, yInv - 1.0), m1PinLay.getDefWidth(ep), m1PinLay.getDefHeight(ep), decLay, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, pExtPin.getPortInst(0), vDropTop.getPortInst(0));
                        Export.newInst(decLay, pExtPin.getPortInst(0), "A[" + k + "]", ep);

                        // M2 vertical drop
                        NodeInst vDropBot = NodeInst.makeInstance(via1, new Point2D.Double(xDrop, yTieA), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                        ArcInst.makeInstance(m2, ep, vDropTop.getPortInst(0), vDropBot.getPortInst(0));

                        // Horizontal M1 tie line to Bus 0 and Bus 1
                        NodeInst vA0 = NodeInst.makeInstance(via1, new Point2D.Double(xBus0[2*k + 1], yTieA), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                        NodeInst vA1 = NodeInst.makeInstance(via1, new Point2D.Double(xBus1[2*k + 1], yTieA), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, vDropBot.getPortInst(0), vA0.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, vA0.getPortInst(0), vA1.getPortInst(0));
                        busNodes0.get(2*k + 1).add(vA0);
                        busNodes1.get(2*k + 1).add(vA1);

                        // Horizontal tie line for A_b[k] at y = yInv - 1.5
                        double yTieAb = yInv - 1.5;
                        NodeInst vAb0 = NodeInst.makeInstance(via1, new Point2D.Double(xBus0[2*k], yTieAb), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                        NodeInst vAb1 = NodeInst.makeInstance(via1, new Point2D.Double(xBus1[2*k], yTieAb), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, layInvA[k].findPortInstFromProto(invLay.findExport("out")), vAb0.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, vAb0.getPortInst(0), vAb1.getPortInst(0));
                        busNodes0.get(2*k).add(vAb0);
                        busNodes1.get(2*k).add(vAb1);
                    }

                    NodeInst[] layNand = new NodeInst[16];
                    NodeInst[] layInvOut = new NodeInst[16];

                    for (int i = 0; i < 16; i++) {
                        boolean isEven = (i % 2 == 0);
                        double wlY = WL_Y[i];

                        int b3 = (i >> 3) & 1;
                        int b2 = (i >> 2) & 1;
                        int b1 = (i >> 1) & 1;
                        int b0 = (i >> 0) & 1;

                        int busIdxA = (b3 == 1) ? (2*3 + 1) : (2*3);
                        int busIdxB = (b2 == 1) ? (2*2 + 1) : (2*2);
                        int busIdxC = (b1 == 1) ? (2*1 + 1) : (2*1);
                        int busIdxD = (b0 == 1) ? (2*0 + 1) : (2*0);

                        int[] busIdxs = { busIdxA, busIdxB, busIdxC, busIdxD };
                        String[] portNames = { "A", "B", "C", "D" };

                        double xNand = isEven ? xNand0 : xNand1;
                        double xInv  = isEven ? xInv0  : xInv1;
                        double[] xBus = isEven ? xBus0 : xBus1;
                        List<List<NodeInst>> busNodes = isEven ? busNodes0 : busNodes1;

                        double yInv = wlY + 1.5;
                        double yNand = wlY - 27.5;

                        layNand[i] = NodeInst.makeInstance(nandLay, new Point2D.Double(xNand, yNand),
                            nandLay.getBounds().getWidth(), nandLay.getBounds().getHeight(), decLay, Orientation.RRR, null);
                        layInvOut[i] = NodeInst.makeInstance(invLay, new Point2D.Double(xInv, yInv),
                            invLay.getBounds().getWidth(), invLay.getBounds().getHeight(), decLay, Orientation.IDENT, null);

                        // NAND out to Inverter in:
                        NodeInst vNandOut = NodeInst.makeInstance(via1, new Point2D.Double(xNand + 45.0, wlY + 0.5), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                        ArcInst.makeInstance(m2, ep, layNand[i].findPortInstFromProto(nandLay.findExport("out")), vNandOut.getPortInst(0));
                        ArcInst aNandInv = ArcInst.makeInstance(m1, ep, vNandOut.getPortInst(0), layInvOut[i].findPortInstFromProto(invLay.findExport("in")));
                        aNandInv.setLambdaBaseWidth(4.0);

                        // Output routing:
                        if (!isEven) {
                            NodeInst pOut = NodeInst.makeInstance(m1PinLay, new Point2D.Double(xOutEdge, wlY), m1PinLay.getDefWidth(ep), m1PinLay.getDefHeight(ep), decLay, Orientation.IDENT, null);
                            ArcInst aOutOdd = ArcInst.makeInstance(m1, ep, layInvOut[i].findPortInstFromProto(invLay.findExport("out")), pOut.getPortInst(0));
                            aOutOdd.setLambdaBaseWidth(4.0);
                            Export.newInst(decLay, pOut.getPortInst(0), "OUT[" + i + "]", ep);
                        } else {
                            NodeInst v1_out = NodeInst.makeInstance(via1, new Point2D.Double(148.0, wlY), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                            NodeInst v2_out = NodeInst.makeInstance(via2, new Point2D.Double(148.0, wlY), via2.getDefWidth(ep), via2.getDefHeight(ep), decLay, Orientation.IDENT, null);
                            ArcInst aOutM1 = ArcInst.makeInstance(m1, ep, layInvOut[i].findPortInstFromProto(invLay.findExport("out")), v1_out.getPortInst(0));
                            aOutM1.setLambdaBaseWidth(4.0);
                            ArcInst.makeInstance(m2, ep, v1_out.getPortInst(0), v2_out.getPortInst(0));

                            NodeInst v2_dest = NodeInst.makeInstance(via2, new Point2D.Double(xOutEdge - 15.0, wlY), via2.getDefWidth(ep), via2.getDefHeight(ep), decLay, Orientation.IDENT, null);
                            NodeInst v1_dest = NodeInst.makeInstance(via1, new Point2D.Double(xOutEdge - 15.0, wlY), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                            ArcInst.makeInstance(m3, ep, v2_out.getPortInst(0), v2_dest.getPortInst(0));
                            ArcInst.makeInstance(m2, ep, v2_dest.getPortInst(0), v1_dest.getPortInst(0));

                            NodeInst pOut = NodeInst.makeInstance(m1PinLay, new Point2D.Double(xOutEdge, wlY), m1PinLay.getDefWidth(ep), m1PinLay.getDefHeight(ep), decLay, Orientation.IDENT, null);
                            ArcInst aDestM1 = ArcInst.makeInstance(m1, ep, v1_dest.getPortInst(0), pOut.getPortInst(0));
                            aDestM1.setLambdaBaseWidth(4.0);
                            Export.newInst(decLay, pOut.getPortInst(0), "OUT[" + i + "]", ep);
                        }

                        // Inputs:
                        double[] portY_RRR = { wlY + 28.5, wlY + 14.0, wlY - 16.0, wlY - 29.0 };
                        for (int p = 0; p < 4; p++) {
                            double py = portY_RRR[p];
                            int bIdx = busIdxs[p];
                            double bx = xBus[bIdx];

                            NodeInst viaBus = NodeInst.makeInstance(via1, new Point2D.Double(bx, py), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                            ArcInst.makeInstance(m1, ep, viaBus.getPortInst(0), layNand[i].findPortInstFromProto(nandLay.findExport(portNames[p])));
                            busNodes.get(bIdx).add(viaBus);
                        }
                    }

                    // Wire Bus 0 and Bus 1 vertically:
                    for (int b = 0; b < 8; b++) {
                        List<NodeInst> nodes0 = busNodes0.get(b);
                        nodes0.sort(Comparator.comparingDouble(n -> n.getTrueCenter().getY()));
                        for (int k = 0; k < nodes0.size() - 1; k++) {
                            ArcInst.makeInstance(m2, ep, nodes0.get(k).getPortInst(0), nodes0.get(k + 1).getPortInst(0));
                        }

                        List<NodeInst> nodes1 = busNodes1.get(b);
                        nodes1.sort(Comparator.comparingDouble(n -> n.getTrueCenter().getY()));
                        for (int k = 0; k < nodes1.size() - 1; k++) {
                            ArcInst.makeInstance(m2, ep, nodes1.get(k).getPortInst(0), nodes1.get(k + 1).getPortInst(0));
                        }
                    }

                    // Power & Ground:
                    List<NodeInst> col0_nand_gnd = new ArrayList<>();
                    List<NodeInst> col0_inv_vdd  = new ArrayList<>();
                    List<NodeInst> col0_inv_gnd  = new ArrayList<>();

                    List<NodeInst> col1_nand_gnd = new ArrayList<>();
                    List<NodeInst> col1_inv_vdd  = new ArrayList<>();
                    List<NodeInst> col1_inv_gnd  = new ArrayList<>();

                    List<NodeInst> invA_vdd = new ArrayList<>();
                    List<NodeInst> invA_gnd = new ArrayList<>();

                    // Connect Gates Power:
                    for (int i = 0; i < 16; i++) {
                        boolean isEven = (i % 2 == 0);
                        double wlY = WL_Y[i];
                        if (isEven) {
                            // Col 0:
                            // NAND GND to vertical rail at x = 22.0
                            NodeInst vNandGnd = NodeInst.makeInstance(via1, new Point2D.Double(22.0, wlY + 0.5), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                            ArcInst aNandGnd = ArcInst.makeInstance(m1, ep, layNand[i].findPortInstFromProto(nandLay.findExport("gnd")), vNandGnd.getPortInst(0));
                            aNandGnd.setLambdaBaseWidth(4.0);
                            col0_nand_gnd.add(vNandGnd);

                            // Inverter GND directly to vertical rail at x = 180.0
                            NodeInst vInvGnd = NodeInst.makeInstance(via1, new Point2D.Double(180.0, wlY - 21.5), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                            ArcInst aInvGnd = ArcInst.makeInstance(m1, ep, layInvOut[i].findPortInstFromProto(invLay.findExport("gnd")), vInvGnd.getPortInst(0));
                            aInvGnd.setLambdaBaseWidth(4.0);
                            col0_inv_gnd.add(vInvGnd);

                            // NAND VDD + Inverter VDD to rail at x = 165.0
                            NodeInst pCornerVdd = NodeInst.makeInstance(m1PinLay, new Point2D.Double(78.5, wlY + 23.0), m1PinLay.getDefWidth(ep), m1PinLay.getDefHeight(ep), decLay, Orientation.IDENT, null);
                            ArcInst aNandVdd = ArcInst.makeInstance(m1, ep, layNand[i].findPortInstFromProto(nandLay.findExport("vdd")), pCornerVdd.getPortInst(0));
                            aNandVdd.setLambdaBaseWidth(4.0);
                            ArcInst aCornerToInv = ArcInst.makeInstance(m1, ep, pCornerVdd.getPortInst(0), layInvOut[i].findPortInstFromProto(invLay.findExport("vdd")));
                            aCornerToInv.setLambdaBaseWidth(4.0);

                            NodeInst vInvVdd = NodeInst.makeInstance(via1, new Point2D.Double(165.0, wlY + 23.0), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                            ArcInst aInvToRail = ArcInst.makeInstance(m1, ep, layInvOut[i].findPortInstFromProto(invLay.findExport("vdd")), vInvVdd.getPortInst(0));
                            aInvToRail.setLambdaBaseWidth(4.0);
                            col0_inv_vdd.add(vInvVdd);
                        } else {
                            // Col 1:
                            // NAND GND to vertical rail at x = 362.0
                            NodeInst vNandGnd = NodeInst.makeInstance(via1, new Point2D.Double(362.0, wlY + 0.5), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                            ArcInst aNandGnd = ArcInst.makeInstance(m1, ep, layNand[i].findPortInstFromProto(nandLay.findExport("gnd")), vNandGnd.getPortInst(0));
                            aNandGnd.setLambdaBaseWidth(4.0);
                            col1_nand_gnd.add(vNandGnd);

                            // Inverter GND directly to vertical rail at x = 505.0
                            NodeInst vInvGnd = NodeInst.makeInstance(via1, new Point2D.Double(505.0, wlY - 21.5), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                            ArcInst aInvGnd = ArcInst.makeInstance(m1, ep, layInvOut[i].findPortInstFromProto(invLay.findExport("gnd")), vInvGnd.getPortInst(0));
                            aInvGnd.setLambdaBaseWidth(4.0);
                            col1_inv_gnd.add(vInvGnd);

                            // NAND VDD + Inverter VDD to rail at x = 490.0
                            NodeInst pCornerVdd = NodeInst.makeInstance(m1PinLay, new Point2D.Double(418.5, wlY + 23.0), m1PinLay.getDefWidth(ep), m1PinLay.getDefHeight(ep), decLay, Orientation.IDENT, null);
                            ArcInst aNandVdd = ArcInst.makeInstance(m1, ep, layNand[i].findPortInstFromProto(nandLay.findExport("vdd")), pCornerVdd.getPortInst(0));
                            aNandVdd.setLambdaBaseWidth(4.0);
                            ArcInst aCornerToInv = ArcInst.makeInstance(m1, ep, pCornerVdd.getPortInst(0), layInvOut[i].findPortInstFromProto(invLay.findExport("vdd")));
                            aCornerToInv.setLambdaBaseWidth(4.0);

                            NodeInst vInvVdd = NodeInst.makeInstance(via1, new Point2D.Double(490.0, wlY + 23.0), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                            ArcInst aInvToRail = ArcInst.makeInstance(m1, ep, layInvOut[i].findPortInstFromProto(invLay.findExport("vdd")), vInvVdd.getPortInst(0));
                            aInvToRail.setLambdaBaseWidth(4.0);
                            col1_inv_vdd.add(vInvVdd);
                        }
                    }

                    // Connect Address Inverters power:
                    for (int k = 0; k < 4; k++) {
                        double yInv = -100.0 - k * 60.0;
                        NodeInst vVdd = NodeInst.makeInstance(via1, new Point2D.Double(-160.0, yInv + 21.5), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, layInvA[k].findPortInstFromProto(invLay.findExport("vdd")), vVdd.getPortInst(0));
                        invA_vdd.add(vVdd);

                        NodeInst vGnd = NodeInst.makeInstance(via1, new Point2D.Double(-150.0, yInv - 23.0), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, layInvA[k].findPortInstFromProto(invLay.findExport("gnd")), vGnd.getPortInst(0));
                        invA_gnd.add(vGnd);
                    }

                    // Vertical rails on M2:
                    col0_nand_gnd.sort(Comparator.comparingDouble(n -> n.getTrueCenter().getY()));
                    for (int k = 0; k < col0_nand_gnd.size() - 1; k++) {
                        ArcInst.makeInstance(m2, ep, col0_nand_gnd.get(k).getPortInst(0), col0_nand_gnd.get(k + 1).getPortInst(0));
                    }

                    col0_inv_vdd.sort(Comparator.comparingDouble(n -> n.getTrueCenter().getY()));
                    for (int k = 0; k < col0_inv_vdd.size() - 1; k++) {
                        ArcInst.makeInstance(m2, ep, col0_inv_vdd.get(k).getPortInst(0), col0_inv_vdd.get(k + 1).getPortInst(0));
                    }

                    col0_inv_gnd.sort(Comparator.comparingDouble(n -> n.getTrueCenter().getY()));
                    for (int k = 0; k < col0_inv_gnd.size() - 1; k++) {
                        ArcInst.makeInstance(m2, ep, col0_inv_gnd.get(k).getPortInst(0), col0_inv_gnd.get(k + 1).getPortInst(0));
                    }

                    col1_nand_gnd.sort(Comparator.comparingDouble(n -> n.getTrueCenter().getY()));
                    for (int k = 0; k < col1_nand_gnd.size() - 1; k++) {
                        ArcInst.makeInstance(m2, ep, col1_nand_gnd.get(k).getPortInst(0), col1_nand_gnd.get(k + 1).getPortInst(0));
                    }

                    col1_inv_vdd.sort(Comparator.comparingDouble(n -> n.getTrueCenter().getY()));
                    for (int k = 0; k < col1_inv_vdd.size() - 1; k++) {
                        ArcInst.makeInstance(m2, ep, col1_inv_vdd.get(k).getPortInst(0), col1_inv_vdd.get(k + 1).getPortInst(0));
                    }

                    col1_inv_gnd.sort(Comparator.comparingDouble(n -> n.getTrueCenter().getY()));
                    for (int k = 0; k < col1_inv_gnd.size() - 1; k++) {
                        ArcInst.makeInstance(m2, ep, col1_inv_gnd.get(k).getPortInst(0), col1_inv_gnd.get(k + 1).getPortInst(0));
                    }

                    invA_vdd.sort(Comparator.comparingDouble(n -> n.getTrueCenter().getY()));
                    for (int k = 0; k < invA_vdd.size() - 1; k++) {
                        ArcInst.makeInstance(m2, ep, invA_vdd.get(k).getPortInst(0), invA_vdd.get(k + 1).getPortInst(0));
                    }

                    invA_gnd.sort(Comparator.comparingDouble(n -> n.getTrueCenter().getY()));
                    for (int k = 0; k < invA_gnd.size() - 1; k++) {
                        ArcInst.makeInstance(m2, ep, invA_gnd.get(k).getPortInst(0), invA_gnd.get(k + 1).getPortInst(0));
                    }

                    // Bottom horizontal tie lines:
                    double yBotVdd = -400.0;
                    double yBotGnd = -420.0;

                    List<List<NodeInst>> vddVLists = Arrays.asList(invA_vdd, col0_inv_vdd, col1_inv_vdd);
                    List<NodeInst> tieNodesVdd = new ArrayList<>();
                    for (int k = 0; k < vddVLists.size(); k++) {
                        double rx = vddVLists.get(k).get(0).getTrueCenter().getX();
                        NodeInst vTie = NodeInst.makeInstance(via1, new Point2D.Double(rx, yBotVdd), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                        ArcInst.makeInstance(m2, ep, vddVLists.get(k).get(0).getPortInst(0), vTie.getPortInst(0));
                        tieNodesVdd.add(vTie);
                    }
                    tieNodesVdd.sort(Comparator.comparingDouble(n -> n.getTrueCenter().getX()));
                    for (int k = 0; k < tieNodesVdd.size() - 1; k++) {
                        ArcInst.makeInstance(m1, ep, tieNodesVdd.get(k).getPortInst(0), tieNodesVdd.get(k + 1).getPortInst(0));
                    }

                    List<List<NodeInst>> gndVLists = Arrays.asList(invA_gnd, col0_nand_gnd, col0_inv_gnd, col1_nand_gnd, col1_inv_gnd);
                    List<NodeInst> tieNodesGnd = new ArrayList<>();
                    for (int k = 0; k < gndVLists.size(); k++) {
                        double rx = gndVLists.get(k).get(0).getTrueCenter().getX();
                        NodeInst vTie = NodeInst.makeInstance(via1, new Point2D.Double(rx, yBotGnd), via1.getDefWidth(ep), via1.getDefHeight(ep), decLay, Orientation.IDENT, null);
                        ArcInst.makeInstance(m2, ep, gndVLists.get(k).get(0).getPortInst(0), vTie.getPortInst(0));
                        tieNodesGnd.add(vTie);
                    }
                    tieNodesGnd.sort(Comparator.comparingDouble(n -> n.getTrueCenter().getX()));
                    for (int k = 0; k < tieNodesGnd.size() - 1; k++) {
                        ArcInst.makeInstance(m1, ep, tieNodesGnd.get(k).getPortInst(0), tieNodesGnd.get(k + 1).getPortInst(0));
                    }

                    Export.newInst(decLay, tieNodesVdd.get(0).getPortInst(0), "vdd", ep);
                    Export.newInst(decLay, tieNodesGnd.get(0).getPortInst(0), "gnd", ep);

                    // ==========================================
                    // 4. VERIFICATION: NCC & DRC
                    // ==========================================
                    System.out.println("Running NCC on Decoder_4x16...");
                    NccOptions nccOpt = new NccOptions();
                    nccOpt.checkSizes = false;
                    NccResults nccRes = Ncc.compare(decSch, null, decLay, null, nccOpt);
                    boolean nccMatch = (nccRes != null && nccRes.match());
                    System.out.println("Decoder_4x16 NCC result: " + (nccMatch ? "PASS (100% MATCH)" : "FAIL"));
                    if (nccRes != null) {
                        System.out.println("  Summary: " + nccRes.summary(false));
                        System.out.println("  Export match: " + nccRes.exportMatch());
                        System.out.println("  Topology match: " + nccRes.topologyMatch());
                    }

                    System.out.println("Running DRC on Decoder_4x16...");
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, decLay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    int drcErrors = errLog.getNumErrors();
                    int drcWarnings = errLog.getNumWarnings();
                    System.out.println("Decoder_4x16 DRC: " + drcErrors + " errors, " + drcWarnings + " warnings");

                    if (!nccMatch || drcErrors > 0) {
                        System.err.println("FATAL: Decoder_4x16 verification failed! Aborting save.");
                        return false;
                    }

                    // ==========================================
                    // 5. SAVE LIBRARY & EXPORT SPICE
                    // ==========================================
                    System.out.println("Saving SRAM.jelib...");
                    Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                    System.out.println("SRAM.jelib saved successfully!");

                    System.out.println("Exporting SPICE netlist: Decoder_4x16.spi...");
                    Spice.SpicePreferences sp = (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, decSch, true, null);
                    sp.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                    sp.level = "3";
                    sp.doOutput(decSch, null, "Decoder_4x16.spi");
                    System.out.println("Decoder_4x16.spi exported successfully!");

                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(4000);
    }
}
