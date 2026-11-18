import java.io.File;
import java.net.URL;
import java.util.*;
import java.awt.geom.Point2D;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
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

public class BuildCleanShiftReg {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildCleanShiftReg", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
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
                    ArcProto m3 = layCell.getTechnology().findArcProto("Metal-3");
                    NodeProto via12Proto = layCell.getTechnology().findNodeProto("Metal-1-Metal-2-Con");
                    NodeProto via23Proto = layCell.getTechnology().findNodeProto("Metal-2-Metal-3-Con");
                    NodeProto m1Pin = layCell.getTechnology().findNodeProto("Metal-1-Pin");
                    NodeProto m2Pin = layCell.getTechnology().findNodeProto("Metal-2-Pin");

                    // 4 rows of 2 DFFs
                    // DFF width = 604, height = 118.5 (bounds: x in [-37, 567], y in [-70.5, 48.0])
                    double pitchX = 640.0;
                    double pitchY = 160.0;

                    NodeInst[] layDFF = new NodeInst[8];
                    for (int i = 0; i < 8; i++) {
                        int row = i / 2;
                        int col = i % 2;
                        double x = col * pitchX;
                        double y = row * pitchY;
                        layDFF[i] = NodeInst.makeInstance(dffLay, new Point2D.Double(x, y), 0, 0, layCell, Orientation.IDENT, null);
                    }

                    // 1. Power Distribution:
                    // Place vertical buses on the west side:
                    // VDD bus on Metal-2 at X = -70.0
                    // GND bus on Metal-2 at X = -90.0
                    double xVddBus = -70.0;
                    double xGndBus = -90.0;

                    NodeInst vVddTop = NodeInst.makeInstance(via12Proto, new Point2D.Double(xVddBus, 3 * pitchY + 36.0), 5.0, 5.0, layCell);
                    NodeInst vVddBot = NodeInst.makeInstance(via12Proto, new Point2D.Double(xVddBus, -36.0), 5.0, 5.0, layCell);
                    ArcInst aVddTrunk = ArcInst.makeInstance(m2, ep, vVddTop.getPortInst(0), vVddBot.getPortInst(0));
                    aVddTrunk.setLambdaBaseWidth(6.0);

                    NodeInst vGndTop = NodeInst.makeInstance(via12Proto, new Point2D.Double(xGndBus, 3 * pitchY + 36.0), 5.0, 5.0, layCell);
                    NodeInst vGndBot = NodeInst.makeInstance(via12Proto, new Point2D.Double(xGndBus, -36.0), 5.0, 5.0, layCell);
                    ArcInst aGndTrunk = ArcInst.makeInstance(m2, ep, vGndTop.getPortInst(0), vGndBot.getPortInst(0));
                    aGndTrunk.setLambdaBaseWidth(6.0);

                    for (int r = 0; r < 4; r++) {
                        double yV = r * pitchY + 36.0;
                        double yG = r * pitchY - 36.0;

                        // Tap from VDD trunk to row r
                        NodeInst tapV = NodeInst.makeInstance(via12Proto, new Point2D.Double(xVddBus, yV), 5.0, 5.0, layCell);
                        PortInst pV0 = layDFF[r * 2].findPortInstFromProto(dffLay.findExport("vdd"));
                        PortInst pV1 = layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("vdd"));
                        ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), pV0).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, pV0, pV1).setLambdaBaseWidth(4.0);

                        // Tap from GND trunk to row r
                        NodeInst tapG = NodeInst.makeInstance(via12Proto, new Point2D.Double(xGndBus, yG), 5.0, 5.0, layCell);
                        PortInst pG0 = layDFF[r * 2].findPortInstFromProto(dffLay.findExport("gnd"));
                        PortInst pG1 = layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("gnd"));
                        ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), pG0).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, pG0, pG1).setLambdaBaseWidth(4.0);
                    }

                    Export.newInst(layCell, vVddBot.getPortInst(0), "vdd", ep);
                    Export.newInst(layCell, vGndBot.getPortInst(0), "gnd", ep);

                    // 2. Clock Distribution:
                    // Place vertical Metal-2 clock trunks in the gaps:
                    // Col 0: at X = -50.0 (between DFF left at -37 and power bus at -70)
                    // Col 1: at X = pitchX - 50.0 = 590.0 (between col 0 at 567 and col 1 at 603)
                    double xClk0 = -50.0;
                    double xClk1 = pitchX - 50.0;

                    NodeInst[] clkVias0 = new NodeInst[4];
                    NodeInst[] clkVias1 = new NodeInst[4];
                    for (int r = 0; r < 4; r++) {
                        double yClk = r * pitchY - 1.0;
                        clkVias0[r] = NodeInst.makeInstance(via12Proto, new Point2D.Double(xClk0, yClk), 5.0, 5.0, layCell);
                        PortInst pClk0 = layDFF[r * 2].findPortInstFromProto(dffLay.findExport("CLK"));
                        ArcInst.makeInstance(m1, ep, pClk0, clkVias0[r].getPortInst(0)).setLambdaBaseWidth(4.0);

                        clkVias1[r] = NodeInst.makeInstance(via12Proto, new Point2D.Double(xClk1, yClk), 5.0, 5.0, layCell);
                        PortInst pClk1 = layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("CLK"));
                        ArcInst.makeInstance(m1, ep, pClk1, clkVias1[r].getPortInst(0)).setLambdaBaseWidth(4.0);

                        if (r > 0) {
                            ArcInst.makeInstance(m2, ep, clkVias0[r - 1].getPortInst(0), clkVias0[r].getPortInst(0)).setLambdaBaseWidth(4.0);
                            ArcInst.makeInstance(m2, ep, clkVias1[r - 1].getPortInst(0), clkVias1[r].getPortInst(0)).setLambdaBaseWidth(4.0);
                        }
                    }

                    // Bridge the two clock trunks at the bottom Y = -55.0
                    NodeInst vClkBridge0 = NodeInst.makeInstance(via12Proto, new Point2D.Double(xClk0, -55.0), 5.0, 5.0, layCell);
                    NodeInst vClkBridge1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(xClk1, -55.0), 5.0, 5.0, layCell);
                    ArcInst.makeInstance(m2, ep, clkVias0[0].getPortInst(0), vClkBridge0.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, clkVias1[0].getPortInst(0), vClkBridge1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m1, ep, vClkBridge0.getPortInst(0), vClkBridge1.getPortInst(0)).setLambdaBaseWidth(4.0);

                    Export.newInst(layCell, clkVias0[0].getPortInst(0), "CLK", ep);

                    // 3. ADDR_IN input pin
                    PortInst pD0 = layDFF[0].findPortInstFromProto(dffLay.findExport("D"));
                    NodeInst pinAddr = NodeInst.makeInstance(m1Pin, new Point2D.Double(-20.0, -10.0), 4.0, 4.0, layCell);
                    ArcInst.makeInstance(m1, ep, pD0, pinAddr.getPortInst(0)).setLambdaBaseWidth(4.0);
                    Export.newInst(layCell, pinAddr.getPortInst(0), "ADDR_IN", ep);

                    // 4. In-Row Cascades (DFF[2r] -> DFF[2r+1]):
                    // Q is at X = 565.0, Y = r * pitchY - 1.5
                    // D is at X = pitchX + 80.0 = 720.0, Y = r * pitchY
                    // Route in the channel at Y = r * pitchY + 60.0 (which is in the clear gap!)
                    for (int r = 0; r < 4; r++) {
                        PortInst pQ = layDFF[r * 2].findPortInstFromProto(dffLay.findExport("Q"));
                        PortInst pD = layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("D"));

                        double yCh = r * pitchY + 60.0;
                        NodeInst vQ = NodeInst.makeInstance(via12Proto, new Point2D.Double(565.0, yCh), 5.0, 5.0, layCell);
                        NodeInst vD = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX + 80.0, yCh), 5.0, 5.0, layCell);

                        ArcInst.makeInstance(m1, ep, pQ, vQ.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vQ.getPortInst(0), vD.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, vD.getPortInst(0), pD).setLambdaBaseWidth(4.0);
                    }

                    // 5. Cross-Row Cascades (DFF[2r+1] -> DFF[2r+2]):
                    // DFF[2r+1].Q is at X = pitchX + 565.0 = 1205.0, Y = r * pitchY - 1.5
                    // DFF[2r+2].D is at X = 80.0, Y = (r + 1) * pitchY
                    // Route in the channel at Y = r * pitchY + 80.0
                    // On Metal-3 so it crosses over the internal DFFs and vertical M2 trunks cleanly!
                    for (int r = 0; r < 3; r++) {
                        PortInst pQ = layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("Q"));
                        PortInst pD = layDFF[(r + 1) * 2].findPortInstFromProto(dffLay.findExport("D"));

                        double yCh = r * pitchY + 80.0;
                        NodeInst vQ_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX + 565.0, yCh), 5.0, 5.0, layCell);
                        NodeInst vQ_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(pitchX + 565.0, yCh), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m1, ep, pQ, vQ_12.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vQ_12.getPortInst(0), vQ_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        NodeInst vD_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(80.0, yCh), 6.0, 6.0, layCell);
                        NodeInst vD_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(80.0, yCh), 5.0, 5.0, layCell);
                        ArcInst.makeInstance(m3, ep, vQ_23.getPortInst(0), vD_23.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vD_23.getPortInst(0), vD_12.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, vD_12.getPortInst(0), pD).setLambdaBaseWidth(4.0);
                    }

                    // 6. Exports Q[0..7]
                    for (int i = 0; i < 8; i++) {
                        PortInst pQ = layDFF[i].findPortInstFromProto(dffLay.findExport("Q"));
                        Export.newInst(layCell, pQ, "Q[" + i + "]", ep);
                    }

                    System.out.println("Folded ShiftReg_8bit{lay} bounds: " + layCell.getBounds());

                    // Check DRC
                    DRC.resetDRCDates(true);
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, layCell, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println(">>> Folded ShiftReg_8bit DRC Errors: " + errLog.getNumErrors() + " <<<");

                    // Check NCC
                    NccOptions opts = new NccOptions();
                    opts.checkSizes = false;
                    NccResults nr = Ncc.compare(schCell, null, layCell, null, opts);
                    System.out.println(">>> Folded ShiftReg_8bit NCC: " + (nr != null && nr.match() ? "PASS" : "FAIL") + " <<<");

                    if (errLog.getNumErrors() == 0 && nr != null && nr.match()) {
                        Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                        System.out.println("Saved SRAM.jelib with folded ShiftReg_8bit!");
                    } else if (errLog.getNumErrors() > 0) {
                        Map<String, Integer> counts = new HashMap<>();
                        for (Iterator it = errLog.getLogs(); it.hasNext(); ) {
                            ErrorLogger.MessageLog ml = (ErrorLogger.MessageLog) it.next();
                            counts.put(ml.getMessageString(), counts.getOrDefault(ml.getMessageString(), 0) + 1);
                        }
                        for (Map.Entry<String, Integer> e : counts.entrySet()) {
                            System.out.println("  " + e.getValue() + "x : " + e.getKey());
                        }
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
