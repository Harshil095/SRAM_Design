
import java.io.File;
import java.net.URL;
import java.util.Iterator;
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

public class BuildFoldedShiftRegClean {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildFoldedShiftRegClean", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();
                    Cell dffLay = lib.findNodeProto("DFF{lay}");
                    Cell schCell = lib.findNodeProto("ShiftReg_8bit{sch}");

                    Cell layCell = Cell.makeInstance(lib, "ShiftReg_8bit_Clean{lay}");
                    Technology mocmos = layCell.getTechnology();
                    ArcProto m1 = mocmos.findArcProto("Metal-1");
                    ArcProto m2 = mocmos.findArcProto("Metal-2");
                    ArcProto m3 = mocmos.findArcProto("Metal-3");
                    NodeProto via1 = mocmos.findNodeProto("Metal-1-Metal-2-Con");
                    NodeProto via23 = mocmos.findNodeProto("Metal-2-Metal-3-Con");
                    NodeProto m1Pin = mocmos.findNodeProto("Metal-1-Pin");
                    NodeProto m2Pin = mocmos.findNodeProto("Metal-2-Pin");

                    double pitchX = 620.0;
                    double pitchY = 180.0;

                    // 1. Place 8 DFF instances (4 rows x 2 cols)
                    NodeInst[] layDFF = new NodeInst[8];
                    for (int i = 0; i < 8; i++) {
                        int row = i / 2;
                        int col = i % 2;
                        double x = col * pitchX;
                        double y = row * pitchY;
                        layDFF[i] = NodeInst.makeInstance(dffLay, new Point2D.Double(x, y),
                            dffLay.getBounds().getWidth(), dffLay.getBounds().getHeight(), layCell, Orientation.IDENT, null);
                    }

                    // 2. Power Distribution
                    double xVddBus = -70.0;
                    double xGndBus = -90.0;
                    NodeInst[] vddTaps = new NodeInst[4];
                    NodeInst[] gndTaps = new NodeInst[4];

                    for (int r = 0; r < 4; r++) {
                        double yV = r * pitchY + 60.0;
                        double yG = r * pitchY - 80.0;

                        // VDD row rail
                        NodeInst pV0 = NodeInst.makeInstance(m1Pin, new Point2D.Double(120.0, yV), m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        NodeInst pV1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(pitchX + 120.0, yV), m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst aV0 = ArcInst.makeInstance(m1, ep, layDFF[r * 2].findPortInstFromProto(dffLay.findExport("vdd")), pV0.getPortInst(0));
                        aV0.setLambdaBaseWidth(4.0);
                        ArcInst aV1 = ArcInst.makeInstance(m1, ep, layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("vdd")), pV1.getPortInst(0));
                        aV1.setLambdaBaseWidth(4.0);
                        ArcInst aVH = ArcInst.makeInstance(m1, ep, pV0.getPortInst(0), pV1.getPortInst(0));
                        aVH.setLambdaBaseWidth(4.0);

                        // VDD tap to vertical bus
                        vddTaps[r] = NodeInst.makeInstance(via1, new Point2D.Double(xVddBus, yV), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst aVTap = ArcInst.makeInstance(m1, ep, vddTaps[r].getPortInst(0), pV0.getPortInst(0));
                        aVTap.setLambdaBaseWidth(4.0);
                        if (r > 0) {
                            ArcInst aVTrunk = ArcInst.makeInstance(m2, ep, vddTaps[r - 1].getPortInst(0), vddTaps[r].getPortInst(0));
                            aVTrunk.setLambdaBaseWidth(6.0);
                        }

                        // GND row rail
                        NodeInst pG0 = NodeInst.makeInstance(m1Pin, new Point2D.Double(120.0, yG), m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        NodeInst pG1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(pitchX + 120.0, yG), m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst aG0 = ArcInst.makeInstance(m1, ep, layDFF[r * 2].findPortInstFromProto(dffLay.findExport("gnd")), pG0.getPortInst(0));
                        aG0.setLambdaBaseWidth(4.0);
                        ArcInst aG1 = ArcInst.makeInstance(m1, ep, layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("gnd")), pG1.getPortInst(0));
                        aG1.setLambdaBaseWidth(4.0);
                        ArcInst aGH = ArcInst.makeInstance(m1, ep, pG0.getPortInst(0), pG1.getPortInst(0));
                        aGH.setLambdaBaseWidth(4.0);

                        // GND tap to vertical bus
                        gndTaps[r] = NodeInst.makeInstance(via1, new Point2D.Double(xGndBus, yG), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst aGTap = ArcInst.makeInstance(m1, ep, gndTaps[r].getPortInst(0), pG0.getPortInst(0));
                        aGTap.setLambdaBaseWidth(4.0);
                        if (r > 0) {
                            ArcInst aGTrunk = ArcInst.makeInstance(m2, ep, gndTaps[r - 1].getPortInst(0), gndTaps[r].getPortInst(0));
                            aGTrunk.setLambdaBaseWidth(6.0);
                        }
                    }
                    Export.newInst(layCell, vddTaps[0].getPortInst(0), "vdd", ep);
                    Export.newInst(layCell, gndTaps[0].getPortInst(0), "gnd", ep);

                    // 3. Clock Distribution
                    double xClk0 = -45.0;
                    double xClk1 = pitchX - 45.0; // 575.0
                    NodeInst[] clkVias0 = new NodeInst[4];
                    NodeInst[] clkVias1 = new NodeInst[4];
                    for (int r = 0; r < 4; r++) {
                        double yClk = r * pitchY - 1.0;
                        clkVias0[r] = NodeInst.makeInstance(via1, new Point2D.Double(xClk0, yClk), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst aClk0 = ArcInst.makeInstance(m1, ep, layDFF[r * 2].findPortInstFromProto(dffLay.findExport("CLK")), clkVias0[r].getPortInst(0));
                        aClk0.setLambdaBaseWidth(4.0);

                        clkVias1[r] = NodeInst.makeInstance(via1, new Point2D.Double(xClk1, yClk), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst aClk1 = ArcInst.makeInstance(m1, ep, layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("CLK")), clkVias1[r].getPortInst(0));
                        aClk1.setLambdaBaseWidth(4.0);

                        if (r > 0) {
                            ArcInst.makeInstance(m2, ep, clkVias0[r - 1].getPortInst(0), clkVias0[r].getPortInst(0)).setLambdaBaseWidth(4.0);
                            ArcInst.makeInstance(m2, ep, clkVias1[r - 1].getPortInst(0), clkVias1[r].getPortInst(0)).setLambdaBaseWidth(4.0);
                        }
                    }
                    // Bridge CLK columns below row 0 at Y = -95.0
                    NodeInst vClkB0 = NodeInst.makeInstance(via1, new Point2D.Double(xClk0, -95.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    NodeInst vClkB1 = NodeInst.makeInstance(via1, new Point2D.Double(xClk1, -95.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m2, ep, clkVias0[0].getPortInst(0), vClkB0.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, clkVias1[0].getPortInst(0), vClkB1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m1, ep, vClkB0.getPortInst(0), vClkB1.getPortInst(0)).setLambdaBaseWidth(4.0);
                    Export.newInst(layCell, clkVias0[0].getPortInst(0), "CLK", ep);

                    // 4. ADDR_IN input pin
                    NodeInst pAddrIn = NodeInst.makeInstance(m1Pin, new Point2D.Double(80.0, 10.0), m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(m1, ep, layDFF[0].findPortInstFromProto(dffLay.findExport("D")), pAddrIn.getPortInst(0)).setLambdaBaseWidth(4.0);
                    Export.newInst(layCell, pAddrIn.getPortInst(0), "ADDR_IN", ep);

                    // 5. In-Row Cascades (DFF[2r] -> DFF[2r+1]) on Metal-2 at Y = r*pitchY + 50.0
                    for (int r = 0; r < 4; r++) {
                        NodeInst vQ_m2 = NodeInst.makeInstance(via1, new Point2D.Double(565.0, r * pitchY + 50.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst aQ = ArcInst.makeInstance(m1, ep, layDFF[r * 2].findPortInstFromProto(dffLay.findExport("Q")), vQ_m2.getPortInst(0));
                        aQ.setLambdaBaseWidth(4.0);

                        NodeInst viaD = NodeInst.makeInstance(via1, new Point2D.Double(pitchX + 80.0, r * pitchY + 10.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst aD = ArcInst.makeInstance(m1, ep, layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("D")), viaD.getPortInst(0));
                        aD.setLambdaBaseWidth(4.0);

                        NodeInst dUp_m2 = NodeInst.makeInstance(m2Pin, new Point2D.Double(pitchX + 80.0, r * pitchY + 50.0), m2Pin.getDefWidth(ep), m2Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m2, ep, viaD.getPortInst(0), dUp_m2.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vQ_m2.getPortInst(0), dUp_m2.getPortInst(0)).setLambdaBaseWidth(4.0);
                    }

                    // 6. Cross-Row Cascades (DFF[2r+1] -> DFF[2r+2]) on Metal-3
                    for (int r = 0; r < 3; r++) {
                        // Q output at (pitchX + 565.0, r*pitchY - 1.5)
                        // Take M1 to (pitchX + 565.0, r*pitchY + 50.0), via1 to M2, via23 to M3
                        NodeInst vQ_m2 = NodeInst.makeInstance(via1, new Point2D.Double(pitchX + 565.0, r * pitchY + 50.0), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        NodeInst vQ_m3 = NodeInst.makeInstance(via23, new Point2D.Double(pitchX + 565.0, r * pitchY + 50.0), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m1, ep, layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("Q")), vQ_m2.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vQ_m2.getPortInst(0), vQ_m3.getPortInst(0)).setLambdaBaseWidth(4.0);

                        // Run West on Metal-3 to xCross = -20.0 - r*15.0
                        double xCross = -20.0 - r * 15.0;
                        NodeInst vWest_m3 = NodeInst.makeInstance(via23, new Point2D.Double(xCross, r * pitchY + 50.0), via23.getDefWidth(ep), via23.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m3, ep, vQ_m3.getPortInst(0), vWest_m3.getPortInst(0)).setLambdaBaseWidth(4.0);

                        // Run North on Metal-2 to (r+1)*pitchY + 10.0
                        double yNextD = (r + 1) * pitchY + 10.0;
                        NodeInst vNorth_m2 = NodeInst.makeInstance(m2Pin, new Point2D.Double(xCross, yNextD), m2Pin.getDefWidth(ep), m2Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m2, ep, vWest_m3.getPortInst(0), vNorth_m2.getPortInst(0)).setLambdaBaseWidth(4.0);

                        // Run East on Metal-2 to D pin of DFF[(r+1)*2] at (80.0, yNextD)
                        NodeInst vD_m2 = NodeInst.makeInstance(via1, new Point2D.Double(80.0, yNextD), via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(m2, ep, vNorth_m2.getPortInst(0), vD_m2.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, vD_m2.getPortInst(0), layDFF[(r + 1) * 2].findPortInstFromProto(dffLay.findExport("D"))).setLambdaBaseWidth(4.0);
                    }

                    // 7. Exports Q[0..7]
                    for (int i = 0; i < 8; i++) {
                        Export.newInst(layCell, layDFF[i].findPortInstFromProto(dffLay.findExport("Q")), "Q[" + i + "]", ep);
                    }

                    System.out.println("ShiftReg_8bit_Clean bounds: " + layCell.getBounds());

                    // Check DRC
                    DRC.resetDRCDates(true);
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    // dp.errorType = default
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, layCell, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println(">>> ShiftReg_8bit_Clean DRC Errors: " + errLog.getNumErrors() + " <<<");

                    // Check NCC
                    NccOptions opts = new NccOptions();
                    opts.checkSizes = false;
                    NccResults nr = Ncc.compare(schCell, null, layCell, null, opts);
                    System.out.println(">>> ShiftReg_8bit_Clean NCC: " + (nr != null && nr.match() ? "PASS" : "FAIL") + " <<<");

                    if (errLog.getNumErrors() == 0 && nr != null && nr.match()) {
                        // Replace ShiftReg_8bit{lay} with this clean cell
                        Cell oldSR = lib.findNodeProto("ShiftReg_8bit{lay}");
                        if (oldSR != null) oldSR.kill();
                        layCell.rename("ShiftReg_8bit", "lay");
                        Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                        System.out.println("SUCCESSFULLY replaced ShiftReg_8bit{lay} in SRAM.jelib!");
                    } else if (errLog.getNumErrors() > 0) {
                        for (Iterator it = errLog.getLogs(); it.hasNext(); ) {
                            ErrorLogger.MessageLog ml = (ErrorLogger.MessageLog) it.next();
                            System.out.println("  ERROR: " + ml.getMessageString());
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
