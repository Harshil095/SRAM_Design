import java.util.*;
import com.sun.electric.database.topology.PortInst;
import java.io.File;
import java.net.URL;
import java.awt.geom.Point2D;
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

public class BuildFoldedShiftReg {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildFoldedShiftReg", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();

                    Cell dffLay = lib.findNodeProto("DFF{lay}");
                    Cell schCell = lib.findNodeProto("ShiftReg_8bit{sch}");

                    Cell oldLay = lib.findNodeProto("ShiftReg_8bit{lay}");
                    if (oldLay != null) oldLay.kill();

                    Cell layCell = Cell.makeInstance(lib, "ShiftReg_8bit{lay}");
                    ArcProto m1 = layCell.getTechnology().findArcProto("Metal-1");
                    ArcProto m2 = layCell.getTechnology().findArcProto("Metal-2");
                    NodeProto via12Proto = layCell.getTechnology().findNodeProto("Metal-1-Metal-2-Con");
                    NodeProto m1Pin = layCell.getTechnology().findNodeProto("Metal-1-Pin");
                    NodeProto m2Pin = layCell.getTechnology().findNodeProto("Metal-2-Pin");

                    // 4 rows of 2 DFFs:
                    // Row 0: DFF[0] at (0, 0), DFF[1] at (620, 0)
                    // Row 1: DFF[2] at (0, 130), DFF[3] at (620, 130)
                    // Row 2: DFF[4] at (0, 260), DFF[5] at (620, 260)
                    // Row 3: DFF[6] at (0, 390), DFF[7] at (620, 390)
                    double pitchX = 620.0;
                    double pitchY = 130.0;

                    NodeInst[] layDFF = new NodeInst[8];
                    for (int i = 0; i < 8; i++) {
                        int row = i / 2;
                        int col = i % 2;
                        double x = col * pitchX;
                        double y = row * pitchY;
                        layDFF[i] = NodeInst.makeInstance(dffLay, new Point2D.Double(x, y), 0, 0, layCell, Orientation.IDENT, null);
                    }

                    // Power Rails:
                    // In each row r, VDD is at Y = r * pitchY + 36.0, GND is at Y = r * pitchY - 36.0
                    // Connect horizontal rails across col 0 and col 1
                    NodeInst[] vddRowLeft = new NodeInst[4];
                    NodeInst[] gndRowLeft = new NodeInst[4];

                    for (int r = 0; r < 4; r++) {
                        NodeInst dffL = layDFF[r * 2];
                        NodeInst dffR = layDFF[r * 2 + 1];

                        // VDD
                        ArcInst aV = ArcInst.makeInstance(m1, ep,
                            dffL.findPortInstFromProto(dffLay.findExport("vdd")),
                            dffR.findPortInstFromProto(dffLay.findExport("vdd")));
                        aV.setLambdaBaseWidth(4.0);
                        vddRowLeft[r] = dffL;

                        // GND
                        ArcInst aG = ArcInst.makeInstance(m1, ep,
                            dffL.findPortInstFromProto(dffLay.findExport("gnd")),
                            dffR.findPortInstFromProto(dffLay.findExport("gnd")));
                        aG.setLambdaBaseWidth(4.0);
                        gndRowLeft[r] = dffL;
                    }

                    // Vertical VDD bus on Metal-2 at X = 120.0 connecting rows 0..3
                    // In DFF, vdd export is at X = 120.0, Y = +36.0
                    for (int r = 0; r < 3; r++) {
                        double y1 = r * pitchY + 36.0;
                        double y2 = (r + 1) * pitchY + 36.0;
                        NodeInst v1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(120.0, y1), 5.0, 5.0, layCell);
                        NodeInst v2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(120.0, y2), 5.0, 5.0, layCell);
                        ArcInst.makeInstance(m1, ep, layDFF[r * 2].findPortInstFromProto(dffLay.findExport("vdd")), v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, layDFF[(r + 1) * 2].findPortInstFromProto(dffLay.findExport("vdd")), v2.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, v1.getPortInst(0), v2.getPortInst(0)).setLambdaBaseWidth(4.0);
                    }

                    // Vertical GND bus on Metal-2 at X = 140.0 connecting rows 0..3
                    for (int r = 0; r < 3; r++) {
                        double y1 = r * pitchY - 36.0;
                        double y2 = (r + 1) * pitchY - 36.0;
                        NodeInst v1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(140.0, y1), 5.0, 5.0, layCell);
                        NodeInst v2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(140.0, y2), 5.0, 5.0, layCell);
                        ArcInst.makeInstance(m1, ep, layDFF[r * 2].findPortInstFromProto(dffLay.findExport("gnd")), v1.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, layDFF[(r + 1) * 2].findPortInstFromProto(dffLay.findExport("gnd")), v2.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, v1.getPortInst(0), v2.getPortInst(0)).setLambdaBaseWidth(4.0);
                    }

                    // Export VDD and GND on row 0
                    Export.newInst(layCell, layDFF[0].findPortInstFromProto(dffLay.findExport("vdd")), "vdd", ep);
                    Export.newInst(layCell, layDFF[0].findPortInstFromProto(dffLay.findExport("gnd")), "gnd", ep);

                    // Clock Distribution:
                    // DFF.CLK is at X = col*pitchX - 35.0, Y = row*pitchY - 1.0
                    // Place vertical Metal-2 trunk at X = -50.0 for col 0, and X = pitchX - 50.0 (570.0) for col 1
                    NodeInst[] clkCol0Vias = new NodeInst[4];
                    NodeInst[] clkCol1Vias = new NodeInst[4];
                    for (int r = 0; r < 4; r++) {
                        double y = r * pitchY - 1.0;
                        clkCol0Vias[r] = NodeInst.makeInstance(via12Proto, new Point2D.Double(-50.0, y), 5.0, 5.0, layCell);
                        ArcInst.makeInstance(m1, ep, layDFF[r * 2].findPortInstFromProto(dffLay.findExport("CLK")), clkCol0Vias[r].getPortInst(0)).setLambdaBaseWidth(4.0);

                        clkCol1Vias[r] = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX - 50.0, y), 5.0, 5.0, layCell);
                        ArcInst.makeInstance(m1, ep, layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("CLK")), clkCol1Vias[r].getPortInst(0)).setLambdaBaseWidth(4.0);

                        if (r > 0) {
                            ArcInst.makeInstance(m2, ep, clkCol0Vias[r - 1].getPortInst(0), clkCol0Vias[r].getPortInst(0)).setLambdaBaseWidth(4.0);
                            ArcInst.makeInstance(m2, ep, clkCol1Vias[r - 1].getPortInst(0), clkCol1Vias[r].getPortInst(0)).setLambdaBaseWidth(4.0);
                        }
                    }
                    // Bridge the two clock columns using horizontal M1 line at Y = -55.0
                    NodeInst vClkB0 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-50.0, -55.0), 5.0, 5.0, layCell);
                    NodeInst vClkB1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX - 50.0, -55.0), 5.0, 5.0, layCell);
                    ArcInst.makeInstance(m2, ep, clkCol0Vias[0].getPortInst(0), vClkB0.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, clkCol1Vias[0].getPortInst(0), vClkB1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m1, ep, vClkB0.getPortInst(0), vClkB1.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // Export CLK
                    Export.newInst(layCell, clkCol0Vias[0].getPortInst(0), "CLK", ep);

                    // Signal: ADDR_IN input pin to DFF[0].D
                    PortInst pD0 = layDFF[0].findPortInstFromProto(dffLay.findExport("D"));
                    NodeInst pinAddr = NodeInst.makeInstance(m1Pin, new Point2D.Double(80.0, -20.0), 4.0, 4.0, layCell);
                    ArcInst.makeInstance(m1, ep, pD0, pinAddr.getPortInst(0)).setLambdaBaseWidth(4.0);
                    Export.newInst(layCell, pinAddr.getPortInst(0), "ADDR_IN", ep);

                    // Cascade Wiring:
                    // In-row connections: DFF[0]->DFF[1], DFF[2]->DFF[3], DFF[4]->DFF[5], DFF[6]->DFF[7]
                    // From DFF[2r].Q at (565.0, r*130 - 1.5) to DFF[2r+1].D at (620 + 80 = 700.0, r*130)
                    for (int r = 0; r < 4; r++) {
                        PortInst pQ = layDFF[r * 2].findPortInstFromProto(dffLay.findExport("Q"));
                        PortInst pD = layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("D"));
                        double yMid = r * pitchY + 50.0;
                        NodeInst vQ_m2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(565.0, yMid), 5.0, 5.0, layCell);
                        NodeInst vD_m2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX + 80.0, yMid), 5.0, 5.0, layCell);
                        ArcInst.makeInstance(m1, ep, pQ, vQ_m2.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, pD, vD_m2.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vQ_m2.getPortInst(0), vD_m2.getPortInst(0)).setLambdaBaseWidth(4.0);
                    }

                    // Cross-row connections: DFF[1]->DFF[2], DFF[3]->DFF[4], DFF[5]->DFF[6]
                    // DFF[2r+1].Q is at (pitchX + 565 = 1185.0, r*130 - 1.5)
                    // DFF[2r+2].D is at (80.0, (r+1)*130)
                    for (int r = 0; r < 3; r++) {
                        PortInst pQ = layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("Q"));
                        PortInst pD = layDFF[(r + 1) * 2].findPortInstFromProto(dffLay.findExport("D"));

                        double yRoute = (r + 1) * pitchY - 15.0; // In horizontal channel between rows
                        NodeInst vQ1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX + 565.0, yRoute), 5.0, 5.0, layCell);
                        NodeInst vD1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(80.0, yRoute), 5.0, 5.0, layCell);

                        ArcInst.makeInstance(m1, ep, pQ, vQ1.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vQ1.getPortInst(0), vD1.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, vD1.getPortInst(0), pD).setLambdaBaseWidth(4.0);
                    }

                    // Export Q[0..7]
                    for (int i = 0; i < 8; i++) {
                        PortInst pQ = layDFF[i].findPortInstFromProto(dffLay.findExport("Q"));
                        Export.newInst(layCell, pQ, "Q[" + i + "]", ep);
                    }

                    System.out.println("ShiftReg_8bit{lay} bounds: " + layCell.getBounds());

                    // Check DRC
                    DRC.resetDRCDates(true);
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, layCell, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println("ShiftReg_8bit DRC Errors: " + errLog.getNumErrors());
                    Map<String, Integer> counts = new HashMap<>();
                    for (Iterator it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = (ErrorLogger.MessageLog) it.next();
                        counts.put(ml.getMessageString(), counts.getOrDefault(ml.getMessageString(), 0) + 1);
                    }
                    for (Map.Entry<String, Integer> e : counts.entrySet()) {
                        System.out.println("  " + e.getValue() + "x : " + e.getKey());
                    }

                    // Check NCC
                    NccOptions opts = new NccOptions();
                    opts.checkSizes = false;
                    NccResults nr = Ncc.compare(schCell, null, layCell, null, opts);
                    System.out.println("ShiftReg_8bit NCC: " + (nr != null && nr.match() ? "PASS" : "FAIL"));

                    if (errLog.getNumErrors() == 0 && nr != null && nr.match()) {
                        Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                        System.out.println("Saved SRAM.jelib with folded ShiftReg_8bit!");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(5000);
    }
}
