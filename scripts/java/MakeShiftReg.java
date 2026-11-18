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

public class MakeShiftReg {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("MakeShiftReg", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
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

                    double pitchX = 640.0;
                    double pitchY = 180.0;

                    // 1. Instantiate 8 DFFs (4 rows x 2 cols)
                    NodeInst[] layDFF = new NodeInst[8];
                    for (int i = 0; i < 8; i++) {
                        int row = i / 2;
                        int col = i % 2;
                        double x = col * pitchX;
                        double y = row * pitchY;
                        layDFF[i] = NodeInst.makeInstance(dffLay, new Point2D.Double(x, y), 0, 0, layCell, Orientation.IDENT, null);
                    }

                    // 2. Power Distribution:
                    double xVddBus = -70.0;
                    double xGndBus = -95.0;

                    NodeInst[] vddTapVias = new NodeInst[4];
                    NodeInst[] gndTapVias = new NodeInst[4];

                    for (int r = 0; r < 4; r++) {
                        double yV = r * pitchY + 60.0;
                        double yG = r * pitchY - 80.0;

                        // Row VDD:
                        PortInst pV0 = layDFF[r * 2].findPortInstFromProto(dffLay.findExport("vdd"));
                        PortInst pV1 = layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("vdd"));
                        NodeInst pV0_up = NodeInst.makeInstance(m1Pin, new Point2D.Double(120.0, yV), 4.0, 4.0, layCell);
                        NodeInst pV1_up = NodeInst.makeInstance(m1Pin, new Point2D.Double(pitchX + 120.0, yV), 4.0, 4.0, layCell);
                        ArcInst.makeInstance(m1, ep, pV0, pV0_up.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, pV1, pV1_up.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, pV0_up.getPortInst(0), pV1_up.getPortInst(0)).setLambdaBaseWidth(4.0);

                        // Tap from VDD bus
                        vddTapVias[r] = NodeInst.makeInstance(via12Proto, new Point2D.Double(xVddBus, yV), 5.0, 5.0, layCell);
                        ArcInst.makeInstance(m1, ep, vddTapVias[r].getPortInst(0), pV0_up.getPortInst(0)).setLambdaBaseWidth(4.0);
                        if (r > 0) {
                            ArcInst.makeInstance(m2, ep, vddTapVias[r - 1].getPortInst(0), vddTapVias[r].getPortInst(0)).setLambdaBaseWidth(6.0);
                        }

                        // Row GND:
                        PortInst pG0 = layDFF[r * 2].findPortInstFromProto(dffLay.findExport("gnd"));
                        PortInst pG1 = layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("gnd"));
                        NodeInst pG0_dn = NodeInst.makeInstance(m1Pin, new Point2D.Double(120.0, yG), 4.0, 4.0, layCell);
                        NodeInst pG1_dn = NodeInst.makeInstance(m1Pin, new Point2D.Double(pitchX + 120.0, yG), 4.0, 4.0, layCell);
                        ArcInst.makeInstance(m1, ep, pG0, pG0_dn.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, pG1, pG1_dn.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, pG0_dn.getPortInst(0), pG1_dn.getPortInst(0)).setLambdaBaseWidth(4.0);

                        // Tap from GND bus
                        gndTapVias[r] = NodeInst.makeInstance(via12Proto, new Point2D.Double(xGndBus, yG), 5.0, 5.0, layCell);
                        ArcInst.makeInstance(m1, ep, gndTapVias[r].getPortInst(0), pG0_dn.getPortInst(0)).setLambdaBaseWidth(4.0);
                        if (r > 0) {
                            ArcInst.makeInstance(m2, ep, gndTapVias[r - 1].getPortInst(0), gndTapVias[r].getPortInst(0)).setLambdaBaseWidth(6.0);
                        }
                    }

                    // Export VDD and GND on the bus vias
                    Export.newInst(layCell, vddTapVias[0].getPortInst(0), "vdd", ep);
                    Export.newInst(layCell, gndTapVias[0].getPortInst(0), "gnd", ep);

                    // 3. Clock Distribution:
                    double xClk0 = -45.0;
                    double xClk1 = pitchX - 45.0;

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

                    // Bridge the clock trunks below row 0 at Y = -95.0
                    NodeInst vClkB0 = NodeInst.makeInstance(via12Proto, new Point2D.Double(xClk0, -95.0), 5.0, 5.0, layCell);
                    NodeInst vClkB1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(xClk1, -95.0), 5.0, 5.0, layCell);
                    ArcInst.makeInstance(m2, ep, clkVias0[0].getPortInst(0), vClkB0.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, clkVias1[0].getPortInst(0), vClkB1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m1, ep, vClkB0.getPortInst(0), vClkB1.getPortInst(0)).setLambdaBaseWidth(4.0);

                    Export.newInst(layCell, clkVias0[0].getPortInst(0), "CLK", ep);

                    // 4. ADDR_IN input pin
                    // In DFF, D port is at (80.0, 0.0). Take it UP to Y = 10.0, then via to M2, then left to X = -20.0
                    PortInst pD0 = layDFF[0].findPortInstFromProto(dffLay.findExport("D"));
                    NodeInst vD0_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(80.0, 10.0), via12Proto.getDefWidth(ep), via12Proto.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, pD0, vD0_12.getPortInst(0)).setLambdaBaseWidth(4.0);

                    NodeInst pAddrPin = NodeInst.makeInstance(m2Pin, new Point2D.Double(-20.0, 10.0), 4.0, 4.0, layCell);
                    ArcInst.makeInstance(m2, ep, vD0_12.getPortInst(0), pAddrPin.getPortInst(0)).setLambdaBaseWidth(4.0);
                    Export.newInst(layCell, pAddrPin.getPortInst(0), "ADDR_IN", ep);

                    // 5. In-Row Cascades (DFF[2r] -> DFF[2r+1]):
                    // Bridge at yBridge = r*pitchY + 30.0
                    for (int r = 0; r < 4; r++) {
                        PortInst pQ = layDFF[r * 2].findPortInstFromProto(dffLay.findExport("Q"));
                        PortInst pD = layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("D"));

                        double yBridge = r * pitchY + 30.0;
                        NodeInst vQ_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(565.0, yBridge), via12Proto.getDefWidth(ep), via12Proto.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        NodeInst vQ_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(565.0, yBridge), via23Proto.getDefWidth(ep), via23Proto.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, pQ, vQ_12.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vQ_12.getPortInst(0), vQ_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        NodeInst vD_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX + 80.0, r * pitchY + 10.0), via12Proto.getDefWidth(ep), via12Proto.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        NodeInst vD_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(pitchX + 80.0, yBridge), via23Proto.getDefWidth(ep), via23Proto.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, pD, vD_12.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vD_12.getPortInst(0), vD_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        ArcInst.makeInstance(m3, ep, vQ_23.getPortInst(0), vD_23.getPortInst(0)).setLambdaBaseWidth(4.0);
                    }

                    // 6. Cross-Row Cascades (DFF[2r+1] -> DFF[2r+2]):
                    // Outgoing: yQCh = r*pitchY + 50.0
                    // Incoming: yNextCh = (r+1)*pitchY + 70.0
                    // Staggered X trunks on west side: X = -15.0 - r*10.0
                    for (int r = 0; r < 3; r++) {
                        PortInst pQ = layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("Q"));
                        PortInst pD = layDFF[(r + 1) * 2].findPortInstFromProto(dffLay.findExport("D"));

                        double yQCh = r * pitchY + 50.0;
                        NodeInst vQ_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX + 565.0, yQCh), via12Proto.getDefWidth(ep), via12Proto.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        NodeInst vQ_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(pitchX + 565.0, yQCh), via23Proto.getDefWidth(ep), via23Proto.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, pQ, vQ_12.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vQ_12.getPortInst(0), vQ_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        double xCrossTrunk = -15.0 - r * 10.0;
                        NodeInst vWest_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(xCrossTrunk, yQCh), via23Proto.getDefWidth(ep), via23Proto.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m3, ep, vQ_23.getPortInst(0), vWest_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        double yNextCh = (r + 1) * pitchY + 70.0;
                        NodeInst vNorth_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(xCrossTrunk, yNextCh), via23Proto.getDefWidth(ep), via23Proto.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m2, ep, vWest_23.getPortInst(0), vNorth_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        NodeInst vNextD_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(80.0, yNextCh), via23Proto.getDefWidth(ep), via23Proto.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m3, ep, vNorth_23.getPortInst(0), vNextD_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        double yNextD = (r + 1) * pitchY + 10.0;
                        NodeInst vD_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(80.0, yNextD), via12Proto.getDefWidth(ep), via12Proto.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m2, ep, vNextD_23.getPortInst(0), vD_12.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, vD_12.getPortInst(0), pD).setLambdaBaseWidth(4.0);
                    }

                    // 7. Exports Q[0..7]
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
                        System.out.println("SUCCESSFULLY saved SRAM.jelib with 0-DRC folded ShiftReg_8bit!");
                    } else if (errLog.getNumErrors() > 0) {
                        java.lang.reflect.Field fLines = Class.forName("com.sun.electric.tool.user.ErrorHighPoly").getDeclaredField("linesList");
                        fLines.setAccessible(true);
                        java.lang.reflect.Field fP1 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p1");
                        fP1.setAccessible(true);
                        java.lang.reflect.Field fP2 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p2");
                        fP2.setAccessible(true);

                        for (Iterator it = errLog.getLogs(); it.hasNext(); ) {
                            ErrorLogger.MessageLog ml = (ErrorLogger.MessageLog) it.next();
                            System.out.println("ERROR: " + ml.getMessageString());
                            for (Iterator hit = ml.getHighlights(); hit.hasNext(); ) {
                                Object eh = hit.next();
                                if (eh.getClass().getSimpleName().equals("ErrorHighPoly")) {
                                    List lines = (List) fLines.get(eh);
                                    for (Object line : lines) {
                                        System.out.println("   " + fP1.get(line) + " -> " + fP2.get(line));
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
        Thread.sleep(5000);
    }
}
