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
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import com.sun.electric.util.math.Orientation;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;
import java.util.Iterator;

public class BuildCompactDiv8Counter {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildCompactDiv8Counter", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();

                    Cell dffLay = lib.findNodeProto("DFF{lay}");
                    Cell invLay = lib.findNodeProto("inverter{lay}");
                    Cell nandLay = lib.findNodeProto("nand4{lay}");
                    Cell schCell = lib.findNodeProto("Div8_Counter{sch}");

                    Cell oldLay = lib.findNodeProto("Div8_Counter{lay}");
                    if (oldLay != null) oldLay.kill();

                    Cell layCell = Cell.makeInstance(lib, "Div8_Counter{lay}");
                    Technology mocmos = layCell.getTechnology();
                    ArcProto m1 = mocmos.findArcProto("Metal-1");
                    ArcProto m2 = mocmos.findArcProto("Metal-2");
                    ArcProto m3 = mocmos.findArcProto("Metal-3");
                    NodeProto via1 = mocmos.findNodeProto("Metal-1-Metal-2-Con");
                    NodeProto via23 = mocmos.findNodeProto("Metal-2-Metal-3-Con");
                    NodeProto m1Pin = mocmos.findNodeProto("Metal-1-Pin");
                    NodeProto m2Pin = mocmos.findNodeProto("Metal-2-Pin");

                    // ========================================================
                    // 1. Placement (2 rows, width <= 1250)
                    // ========================================================
                    // Row 0: DFF0 at (0, 0), DFF1 at (620, 0)
                    NodeInst dff0 = NodeInst.makeInstance(dffLay, new Point2D.Double(0.0, 0.0), dffLay.getBounds().getWidth(), dffLay.getBounds().getHeight(), layCell, Orientation.IDENT, null);
                    NodeInst dff1 = NodeInst.makeInstance(dffLay, new Point2D.Double(620.0, 0.0), dffLay.getBounds().getWidth(), dffLay.getBounds().getHeight(), layCell, Orientation.IDENT, null);

                    // Row 1: DFF2 at (0, 180), inverters and nand at X >= 640, Y = 180
                    NodeInst dff2 = NodeInst.makeInstance(dffLay, new Point2D.Double(0.0, 180.0), dffLay.getBounds().getWidth(), dffLay.getBounds().getHeight(), layCell, Orientation.IDENT, null);

                    double xInv0 = 640.0;
                    double xInv1 = 760.0;
                    double xInv2 = 870.0;
                    double xNand = 990.0;
                    double xInvOut = 1100.0;

                    NodeInst inv0 = NodeInst.makeInstance(invLay, new Point2D.Double(xInv0, 180.0), invLay.getBounds().getWidth(), invLay.getBounds().getHeight(), layCell, Orientation.IDENT, null);
                    NodeInst inv1 = NodeInst.makeInstance(invLay, new Point2D.Double(xInv1, 180.0), invLay.getBounds().getWidth(), invLay.getBounds().getHeight(), layCell, Orientation.IDENT, null);
                    NodeInst inv2 = NodeInst.makeInstance(invLay, new Point2D.Double(xInv2, 180.0), invLay.getBounds().getWidth(), invLay.getBounds().getHeight(), layCell, Orientation.IDENT, null);
                    NodeInst nand = NodeInst.makeInstance(nandLay, new Point2D.Double(xNand, 180.0), nandLay.getBounds().getWidth(), nandLay.getBounds().getHeight(), layCell, Orientation.IDENT, null);
                    NodeInst invOut = NodeInst.makeInstance(invLay, new Point2D.Double(xInvOut, 180.0), invLay.getBounds().getWidth(), invLay.getBounds().getHeight(), layCell, Orientation.IDENT, null);

                    // ========================================================
                    // 2. Power Distribution (VDD & GND)
                    // ========================================================
                    // Row 0 VDD at Y = 60.0, GND at Y = -80.0
                    NodeInst pV0_0 = NodeInst.makeInstance(m1Pin, new Point2D.Double(120.0, 60.0), m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    NodeInst pV0_1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(740.0, 60.0), m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff0.findPortInstFromProto(dffLay.findExport("vdd")), pV0_0.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m1, ep, dff1.findPortInstFromProto(dffLay.findExport("vdd")), pV0_1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m1, ep, pV0_0.getPortInst(0), pV0_1.getPortInst(0)).setLambdaBaseWidth(4.0);

                    NodeInst pG0_0 = NodeInst.makeInstance(m1Pin, new Point2D.Double(120.0, -80.0), m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    NodeInst pG0_1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(740.0, -80.0), m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff0.findPortInstFromProto(dffLay.findExport("gnd")), pG0_0.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m1, ep, dff1.findPortInstFromProto(dffLay.findExport("gnd")), pG0_1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m1, ep, pG0_0.getPortInst(0), pG0_1.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // Row 1 VDD at Y = 240.0, GND at Y = 100.0
                    NodeInst pV1_0 = NodeInst.makeInstance(m1Pin, new Point2D.Double(120.0, 240.0), m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff2.findPortInstFromProto(dffLay.findExport("vdd")), pV1_0.getPortInst(0)).setLambdaBaseWidth(4.0);

                    NodeInst pG1_0 = NodeInst.makeInstance(m1Pin, new Point2D.Double(120.0, 100.0), m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff2.findPortInstFromProto(dffLay.findExport("gnd")), pG1_0.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // Inverters VDD and GND taps (using rx = invX - 40.0 from BuildDiv8Counter)
                    NodeInst[] gates = {inv0, inv1, inv2, invOut};
                    double[] gateX = {xInv0, xInv1, xInv2, xInvOut};
                    NodeInst prevV1 = pV1_0;
                    NodeInst prevG1 = pG1_0;

                    for (int i = 0; i < 4; i++) {
                        double rx = gateX[i] - 40.0;
                        // VDD tap
                        NodeInst vddRailTap = NodeInst.makeInstance(via1, new Point2D.Double(rx, 240.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, prevV1.getPortInst(0), vddRailTap.getPortInst(0)).setLambdaBaseWidth(4.0);
                        prevV1 = vddRailTap;

                        NodeInst vVddLocal = NodeInst.makeInstance(via1, new Point2D.Double(rx, 201.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, gates[i].findPortInstFromProto(invLay.findExport("vdd")), vVddLocal.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vVddLocal.getPortInst(0), vddRailTap.getPortInst(0)).setLambdaBaseWidth(4.0);

                        // GND tap
                        NodeInst gndRailTap = NodeInst.makeInstance(via1, new Point2D.Double(rx, 100.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, prevG1.getPortInst(0), gndRailTap.getPortInst(0)).setLambdaBaseWidth(4.0);
                        prevG1 = gndRailTap;

                        NodeInst vGndLocal = NodeInst.makeInstance(via1, new Point2D.Double(rx, 157.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, gates[i].findPortInstFromProto(invLay.findExport("gnd")), vGndLocal.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vGndLocal.getPortInst(0), gndRailTap.getPortInst(0)).setLambdaBaseWidth(4.0);
                    }

                    // NAND VDD tap and D input tied to VDD (at X = xNand + 1.5 = 991.5)
                    double xNandD_Vdd = xNand + 1.5; // 991.5
                    NodeInst vddNandTap = NodeInst.makeInstance(via1, new Point2D.Double(xNandD_Vdd, 240.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, prevV1.getPortInst(0), vddNandTap.getPortInst(0)).setLambdaBaseWidth(4.0);

                    NodeInst vNandVdd = NodeInst.makeInstance(via1, new Point2D.Double(xNandD_Vdd, 208.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, nand.findPortInstFromProto(nandLay.findExport("vdd")), vNandVdd.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, vNandVdd.getPortInst(0), vddNandTap.getPortInst(0)).setLambdaBaseWidth(4.0);

                    NodeInst vNandD = NodeInst.makeInstance(via1, new Point2D.Double(xNandD_Vdd, 127.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, nand.findPortInstFromProto(nandLay.findExport("D")), vNandD.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, vNandD.getPortInst(0), vNandVdd.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // NAND GND tap (at X = xNand - 28.0 = 962.0)
                    double xNandGnd = xNand - 28.0; // 962.0
                    NodeInst vNandGnd = NodeInst.makeInstance(via1, new Point2D.Double(xNandGnd, 152.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, nand.findPortInstFromProto(nandLay.findExport("gnd")), vNandGnd.getPortInst(0)).setLambdaBaseWidth(4.0);

                    NodeInst gndNandTap = NodeInst.makeInstance(via1, new Point2D.Double(xNandGnd, 100.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, prevG1.getPortInst(0), gndNandTap.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, vNandGnd.getPortInst(0), gndNandTap.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // West Power Trunks (M2 vertical buses):
                    double xVddBus = -110.0;
                    double xGndBus = -130.0;
                    NodeInst vVdd0 = NodeInst.makeInstance(via1, new Point2D.Double(xVddBus, 60.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    NodeInst vVdd1 = NodeInst.makeInstance(via1, new Point2D.Double(xVddBus, 240.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, vVdd0.getPortInst(0), pV0_0.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m1, ep, vVdd1.getPortInst(0), pV1_0.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, vVdd0.getPortInst(0), vVdd1.getPortInst(0)).setLambdaBaseWidth(6.0);
                    Export.newInst(layCell, vVdd0.getPortInst(0), "vdd", ep);

                    NodeInst vGnd0 = NodeInst.makeInstance(via1, new Point2D.Double(xGndBus, -80.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    NodeInst vGnd1 = NodeInst.makeInstance(via1, new Point2D.Double(xGndBus, 100.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, vGnd0.getPortInst(0), pG0_0.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m1, ep, vGnd1.getPortInst(0), pG1_0.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, vGnd0.getPortInst(0), vGnd1.getPortInst(0)).setLambdaBaseWidth(6.0);
                    Export.newInst(layCell, vGnd0.getPortInst(0), "gnd", ep);

                    // ========================================================
                    // 3. CLK Input: to dff0.CLK at (-45.0, -1.0)
                    // ========================================================
                    NodeInst vClk_v1 = NodeInst.makeInstance(via1, new Point2D.Double(-45.0, -1.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff0.findPortInstFromProto(dffLay.findExport("CLK")), vClk_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst pClk = NodeInst.makeInstance(m2Pin, new Point2D.Double(-45.0, -30.0), m2Pin.getDefWidth(ep), m2Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vClk_v1.getPortInst(0), pClk.getPortInst(0)).setLambdaBaseWidth(4.0);
                    Export.newInst(layCell, pClk.getPortInst(0), "CLK", ep);

                    // ========================================================
                    // 4. Signal Routing
                    // ========================================================
                    double y_Q0b = 30.0;
                    double y_Q0 = 85.0;
                    double y_Q1 = 115.0;
                    double y_NandOut = 225.0;
                    double y_Q1b = 255.0;
                    double y_Q2 = 270.0;
                    double y_Q2b = 285.0;

                    // --------------------------------------------------------
                    // NET Q0: dff0.Q -> inv0.in and nand.A (M3 Track: Y = 85.0)
                    // --------------------------------------------------------
                    NodeInst vQ0_src_v1 = NodeInst.makeInstance(via1, new Point2D.Double(565.0, 50.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff0.findPortInstFromProto(dffLay.findExport("Q")), vQ0_src_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ0_src_v2 = NodeInst.makeInstance(via23, new Point2D.Double(565.0, y_Q0), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ0_src_v1.getPortInst(0), vQ0_src_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    double xInv0In = xInv0 - 25.0; // 615.0
                    NodeInst vInv0In_v1 = NodeInst.makeInstance(via1, new Point2D.Double(xInv0In, 179.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, inv0.findPortInstFromProto(invLay.findExport("in")), vInv0In_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vInv0In_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xInv0In, y_Q0), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vInv0In_v1.getPortInst(0), vInv0In_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    double xNandA = xNand - 56.0; // 934.0
                    NodeInst vNandA_bot = NodeInst.makeInstance(via1, new Point2D.Double(xNandA, 127.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, nand.findPortInstFromProto(nandLay.findExport("A")), vNandA_bot.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vNandA_top_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xNandA, y_Q0), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vNandA_bot.getPortInst(0), vNandA_top_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // M3 Horizontal trunk chained through taps
                    ArcInst.makeInstance(m3, ep, vQ0_src_v2.getPortInst(0), vInv0In_v2.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m3, ep, vInv0In_v2.getPortInst(0), vNandA_top_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // --------------------------------------------------------
                    // NET Q0_b / inv0.out -> dff1.CLK and dff0.D (M3 Track: Y = 30.0)
                    // --------------------------------------------------------
                    double xInv0Out = xInv0 + 8.0; // 648.0
                    NodeInst vInv0Out_v1 = NodeInst.makeInstance(via1, new Point2D.Double(xInv0Out, 178.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, inv0.findPortInstFromProto(invLay.findExport("out")), vInv0Out_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vInv0Out_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xInv0Out, y_Q0b), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vInv0Out_v1.getPortInst(0), vInv0Out_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    double xDff1Clk = 575.0;
                    NodeInst vDff1Clk_v1 = NodeInst.makeInstance(via1, new Point2D.Double(xDff1Clk, -1.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff1.findPortInstFromProto(dffLay.findExport("CLK")), vDff1Clk_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vDff1Clk_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xDff1Clk, y_Q0b), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vDff1Clk_v1.getPortInst(0), vDff1Clk_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    double xDff0D = 80.0;
                    NodeInst vDff0D_v1 = NodeInst.makeInstance(via1, new Point2D.Double(xDff0D, 10.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff0.findPortInstFromProto(dffLay.findExport("D")), vDff0D_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vDff0D_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xDff0D, y_Q0b), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vDff0D_v1.getPortInst(0), vDff0D_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // M3 Horizontal trunk chained through taps
                    ArcInst.makeInstance(m3, ep, vInv0Out_v2.getPortInst(0), vDff1Clk_v2.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m3, ep, vDff1Clk_v2.getPortInst(0), vDff0D_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // --------------------------------------------------------
                    // NET Q1: dff1.Q -> inv1.in and nand.B (M3 Track: Y = 115.0)
                    // --------------------------------------------------------
                    double xDff1Q = 1185.0;
                    NodeInst vQ1_src_v1 = NodeInst.makeInstance(via1, new Point2D.Double(xDff1Q, 50.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff1.findPortInstFromProto(dffLay.findExport("Q")), vQ1_src_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ1_src_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xDff1Q, y_Q1), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ1_src_v1.getPortInst(0), vQ1_src_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    double xNandB = xNand - 41.5; // 948.5
                    NodeInst vNandB_bot = NodeInst.makeInstance(via1, new Point2D.Double(xNandB, 127.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, nand.findPortInstFromProto(nandLay.findExport("B")), vNandB_bot.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vNandB_top_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xNandB, y_Q1), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vNandB_bot.getPortInst(0), vNandB_top_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    double xInv1In = xInv1 - 25.0; // 735.0
                    NodeInst vInv1In_v1 = NodeInst.makeInstance(via1, new Point2D.Double(xInv1In, 179.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, inv1.findPortInstFromProto(invLay.findExport("in")), vInv1In_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vInv1In_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xInv1In, y_Q1), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vInv1In_v1.getPortInst(0), vInv1In_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // M3 Horizontal trunk chained through taps
                    ArcInst.makeInstance(m3, ep, vQ1_src_v2.getPortInst(0), vNandB_top_v2.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m3, ep, vNandB_top_v2.getPortInst(0), vInv1In_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // --------------------------------------------------------
                    // NET Q1_b / inv1.out -> dff1.D and dff2.CLK (M3 Track: Y = 255.0)
                    // --------------------------------------------------------
                    double xInv1Out = xInv1 + 8.0; // 768.0
                    NodeInst vInv1Out_v1 = NodeInst.makeInstance(via1, new Point2D.Double(xInv1Out, 178.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, inv1.findPortInstFromProto(invLay.findExport("out")), vInv1Out_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vInv1Out_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xInv1Out, y_Q1b), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vInv1Out_v1.getPortInst(0), vInv1Out_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    double xDff1D = 700.0;
                    NodeInst vDff1D_v1 = NodeInst.makeInstance(via1, new Point2D.Double(xDff1D, 10.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff1.findPortInstFromProto(dffLay.findExport("D")), vDff1D_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vDff1D_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xDff1D, y_Q1b), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vDff1D_v1.getPortInst(0), vDff1D_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    double xDff2Clk = -45.0;
                    NodeInst vDff2Clk_v1 = NodeInst.makeInstance(via1, new Point2D.Double(xDff2Clk, 179.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff2.findPortInstFromProto(dffLay.findExport("CLK")), vDff2Clk_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vDff2Clk_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xDff2Clk, y_Q1b), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vDff2Clk_v1.getPortInst(0), vDff2Clk_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // M3 Horizontal trunk chained through taps
                    ArcInst.makeInstance(m3, ep, vInv1Out_v2.getPortInst(0), vDff1D_v2.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m3, ep, vDff1D_v2.getPortInst(0), vDff2Clk_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // --------------------------------------------------------
                    // NET Q2: dff2.Q -> inv2.in and nand.C (M3 Track: Y = 270.0)
                    // --------------------------------------------------------
                    double xDff2Q = 565.0;
                    NodeInst vQ2_src_v1 = NodeInst.makeInstance(via1, new Point2D.Double(xDff2Q, 230.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff2.findPortInstFromProto(dffLay.findExport("Q")), vQ2_src_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vQ2_src_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xDff2Q, y_Q2), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vQ2_src_v1.getPortInst(0), vQ2_src_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    double xInv2In = xInv2 - 25.0; // 845.0
                    NodeInst vInv2In_v1 = NodeInst.makeInstance(via1, new Point2D.Double(xInv2In, 179.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, inv2.findPortInstFromProto(invLay.findExport("in")), vInv2In_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vInv2In_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xInv2In, y_Q2), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vInv2In_v1.getPortInst(0), vInv2In_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    double xNandC = xNand - 11.5; // 978.5 (ZERO M2 in nand4 at X = 978.5!)
                    NodeInst vNandC_bot = NodeInst.makeInstance(via1, new Point2D.Double(xNandC, 127.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, nand.findPortInstFromProto(nandLay.findExport("C")), vNandC_bot.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vNandC_top_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xNandC, y_Q2), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vNandC_bot.getPortInst(0), vNandC_top_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // M3 Horizontal trunk chained through taps
                    ArcInst.makeInstance(m3, ep, vQ2_src_v2.getPortInst(0), vInv2In_v2.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m3, ep, vInv2In_v2.getPortInst(0), vNandC_top_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // --------------------------------------------------------
                    // NET Q2_b / inv2.out -> dff2.D (M3 Track: Y = 285.0)
                    // --------------------------------------------------------
                    double xInv2Out = xInv2 + 8.0; // 878.0
                    NodeInst vInv2Out_v1 = NodeInst.makeInstance(via1, new Point2D.Double(xInv2Out, 178.5), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, inv2.findPortInstFromProto(invLay.findExport("out")), vInv2Out_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vInv2Out_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xInv2Out, y_Q2b), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vInv2Out_v1.getPortInst(0), vInv2Out_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    double xDff2D = 80.0;
                    NodeInst vDff2D_v1 = NodeInst.makeInstance(via1, new Point2D.Double(xDff2D, 180.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, dff2.findPortInstFromProto(dffLay.findExport("D")), vDff2D_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vDff2D_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xDff2D, y_Q2b), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vDff2D_v1.getPortInst(0), vDff2D_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // M3 Horizontal trunk
                    ArcInst.makeInstance(m3, ep, vInv2Out_v2.getPortInst(0), vDff2D_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // --------------------------------------------------------
                    // NET NAND_OUT: nand.out -> invOut.in (M3 Track: Y = 225.0)
                    // --------------------------------------------------------
                    double xNandOut = xNand - 28.0; // 962.0 (on Metal-2)
                    NodeInst vNandOut_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xNandOut, y_NandOut), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, nand.findPortInstFromProto(nandLay.findExport("out")), vNandOut_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    double xInvOutIn = xInvOut - 25.0; // 1075.0
                    NodeInst vInvOutIn_v1 = NodeInst.makeInstance(via1, new Point2D.Double(xInvOutIn, 179.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, invOut.findPortInstFromProto(invLay.findExport("in")), vInvOutIn_v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    NodeInst vInvOutIn_v2 = NodeInst.makeInstance(via23, new Point2D.Double(xInvOutIn, y_NandOut), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, vInvOutIn_v1.getPortInst(0), vInvOutIn_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // M3 Horizontal trunk
                    ArcInst.makeInstance(m3, ep, vNandOut_v2.getPortInst(0), vInvOutIn_v2.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // --------------------------------------------------------
                    // EXECUTE: invOut.out at (xInvOut + 8.0, 178.5)
                    // --------------------------------------------------------
                    double xExec = xInvOut + 8.0; // 1108.0
                    NodeInst pExec = NodeInst.makeInstance(m1Pin, new Point2D.Double(xExec, 178.5), m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, invOut.findPortInstFromProto(invLay.findExport("out")), pExec.getPortInst(0)).setLambdaBaseWidth(4.0);
                    Export.newInst(layCell, pExec.getPortInst(0), "EXECUTE", ep);

                    System.out.println("Div8_Counter{lay} bounds: " + layCell.getBounds());

                    // Check DRC
                    DRC.resetDRCDates(true);
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, layCell, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println(">>> Div8_Counter{lay} DRC Errors: " + errLog.getNumErrors() + " <<<");

                    // Check NCC
                    NccOptions opts = new NccOptions();
                    opts.checkSizes = false;
                    NccResults nr = Ncc.compare(schCell, null, layCell, null, opts);
                    System.out.println(">>> Div8_Counter NCC: " + (nr != null && nr.match() ? "PASS" : "FAIL") + " <<<");

                    if (errLog.getNumErrors() == 0 && nr != null && nr.match()) {
                        Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                        System.out.println("SUCCESSFULLY saved compact Div8_Counter{lay} in SRAM.jelib!");
                    } else if (errLog.getNumErrors() > 0) {
                        java.lang.reflect.Field fLines = Class.forName("com.sun.electric.tool.user.ErrorHighPoly").getDeclaredField("linesList");
                        fLines.setAccessible(true);
                        java.lang.reflect.Field fP1 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p1");
                        fP1.setAccessible(true);
                        java.lang.reflect.Field fP2 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p2");
                        fP2.setAccessible(true);

                        for (Iterator it = errLog.getLogs(); it.hasNext(); ) {
                            ErrorLogger.MessageLog ml = (ErrorLogger.MessageLog) it.next();
                            System.out.println("  ERROR: " + ml.getMessageString());
                            for (Iterator hit = ml.getHighlights(); hit.hasNext(); ) {
                                Object eh = hit.next();
                                if (eh.getClass().getSimpleName().equals("ErrorHighPoly")) {
                                    java.util.List lines = (java.util.List) fLines.get(eh);
                                    for (Object line : lines) {
                                        System.out.println("     " + fP1.get(line) + " -> " + fP2.get(line));
                                    }
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(4000);
    }
}
