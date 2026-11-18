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
import java.util.Iterator;

public class BuildShiftReg8bit {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildShiftReg8bit", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();
                    Cell dffLay = lib.findNodeProto("DFF{lay}");
                    Cell dffSch = lib.findNodeProto("DFF{sch}");
                    Cell dffIc = lib.findNodeProto("DFF{ic}");
                    Technology mocmos = Technology.getMocmosTechnology();

                    ArcProto m1 = mocmos.findArcProto("Metal-1");
                    ArcProto m2 = mocmos.findArcProto("Metal-2");
                    NodeProto via1 = mocmos.findNodeProto("Metal-1-Metal-2-Con");
                    NodeProto m1Pin = mocmos.findNodeProto("Metal-1-Pin");
                    NodeProto m2Pin = mocmos.findNodeProto("Metal-2-Pin");

                    // Remove existing ShiftReg_8bit cells if present
                    Cell oldSch = lib.findNodeProto("ShiftReg_8bit{sch}");
                    Cell oldLay = lib.findNodeProto("ShiftReg_8bit{lay}");
                    Cell oldIc  = lib.findNodeProto("ShiftReg_8bit{ic}");
                    if (oldIc != null) oldIc.kill();
                    if (oldSch != null) oldSch.kill();
                    if (oldLay != null) oldLay.kill();

                    // ==========================================
                    // 1. SCHEMATIC: ShiftReg_8bit{sch}
                    // ==========================================
                    Cell schCell = Cell.makeInstance(lib, "ShiftReg_8bit{sch}");
                    Technology generic = schCell.getTechnology();
                    ArcProto schWire = generic.findArcProto("wire");
                    NodeProto offPage = generic.findNodeProto("Off-Page");
                    NodeProto pwrProto = generic.findNodeProto("Power");
                    NodeProto gndProto = generic.findNodeProto("Ground");

                    NodeInst[] schDFF = new NodeInst[8];
                    for (int i = 0; i < 8; i++) {
                        schDFF[i] = NodeInst.makeInstance(dffIc, new Point2D.Double(i * 20.0, 0.0),
                            dffIc.getDefWidth(ep), dffIc.getDefHeight(ep), schCell, Orientation.IDENT, null);
                    }

                    // ADDR_IN input pin
                    NodeInst pinAddrIn = NodeInst.makeInstance(offPage, new Point2D.Double(-15.0, 0.0),
                        offPage.getDefWidth(ep), offPage.getDefHeight(ep), schCell, Orientation.IDENT, null);
                    ArcInst.makeInstance(schWire, ep, pinAddrIn.getPortInst(0), schDFF[0].findPortInstFromProto(dffIc.findExport("D")));
                    Export.newInst(schCell, pinAddrIn.getPortInst(0), "ADDR_IN", ep);

                    // CLK input pin
                    NodeInst pinClk = NodeInst.makeInstance(offPage, new Point2D.Double(-15.0, -10.0),
                        offPage.getDefWidth(ep), offPage.getDefHeight(ep), schCell, Orientation.IDENT, null);
                    Export.newInst(schCell, pinClk.getPortInst(0), "CLK", ep);
                    for (int i = 0; i < 8; i++) {
                        ArcInst.makeInstance(schWire, ep, pinClk.getPortInst(0), schDFF[i].findPortInstFromProto(dffIc.findExport("CLK")));
                    }

                    // Cascade Q[i] -> D[i+1]
                    for (int i = 0; i < 7; i++) {
                        ArcInst.makeInstance(schWire, ep, schDFF[i].findPortInstFromProto(dffIc.findExport("Q")),
                            schDFF[i+1].findPortInstFromProto(dffIc.findExport("D")));
                    }

                    // Outputs Q[0] .. Q[7]
                    for (int i = 0; i < 8; i++) {
                        NodeInst pinQ = NodeInst.makeInstance(offPage, new Point2D.Double(i * 20.0 + 8.0, 10.0),
                            offPage.getDefWidth(ep), offPage.getDefHeight(ep), schCell, Orientation.IDENT, null);
                        ArcInst.makeInstance(schWire, ep, pinQ.getPortInst(0), schDFF[i].findPortInstFromProto(dffIc.findExport("Q")));
                        Export.newInst(schCell, pinQ.getPortInst(0), "Q[" + i + "]", ep);
                    }

                    // Power & Ground
                    NodeInst pwrNode = NodeInst.makeInstance(pwrProto, new Point2D.Double(-15.0, 20.0),
                        pwrProto.getDefWidth(ep), pwrProto.getDefHeight(ep), schCell, Orientation.IDENT, null);
                    NodeInst gndNode = NodeInst.makeInstance(gndProto, new Point2D.Double(-15.0, -20.0),
                        gndProto.getDefWidth(ep), gndProto.getDefHeight(ep), schCell, Orientation.IDENT, null);
                    Export.newInst(schCell, pwrNode.getPortInst(0), "vdd", ep);
                    Export.newInst(schCell, gndNode.getPortInst(0), "gnd", ep);

                    for (int i = 0; i < 8; i++) {
                        ArcInst.makeInstance(schWire, ep, pwrNode.getPortInst(0), schDFF[i].findPortInstFromProto(dffIc.findExport("vdd")));
                        ArcInst.makeInstance(schWire, ep, gndNode.getPortInst(0), schDFF[i].findPortInstFromProto(dffIc.findExport("gnd")));
                    }

                    // ==========================================
                    // 2. ICON: ShiftReg_8bit{ic}
                    // ==========================================
                    IconParameters ip = IconParameters.makeInstance(true);
                    Cell icCell = ip.makeIconForCell(schCell, ep);
                    System.out.println("Icon generated: " + icCell.getName());

                    // ==========================================
                    // 3. LAYOUT: ShiftReg_8bit{lay}
                    // ==========================================
                    Cell layCell = Cell.makeInstance(lib, "ShiftReg_8bit{lay}");
                    double pitch = 620.0;
                    NodeInst[] layDFF = new NodeInst[8];
                    for (int i = 0; i < 8; i++) {
                        layDFF[i] = NodeInst.makeInstance(dffLay, new Point2D.Double(i * pitch, 0.0),
                            dffLay.getBounds().getWidth(), dffLay.getBounds().getHeight(), layCell, Orientation.IDENT, null);
                    }

                    // VDD horizontal rail at Y = 60.0
                    NodeInst[] vddPins = new NodeInst[8];
                    for (int i = 0; i < 8; i++) {
                        vddPins[i] = NodeInst.makeInstance(m1Pin, new Point2D.Double(i * pitch + 120.0, 60.0),
                            m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst a = ArcInst.makeInstance(m1, ep, layDFF[i].findPortInstFromProto(dffLay.findExport("vdd")), vddPins[i].getPortInst(0));
                        a.setLambdaBaseWidth(4.0);
                        if (i > 0) {
                            ArcInst aH = ArcInst.makeInstance(m1, ep, vddPins[i-1].getPortInst(0), vddPins[i].getPortInst(0));
                            aH.setLambdaBaseWidth(4.0);
                        }
                    }
                    Export.newInst(layCell, vddPins[0].getPortInst(0), "vdd", ep);

                    // GND horizontal rail at Y = -80.0
                    NodeInst[] gndPins = new NodeInst[8];
                    for (int i = 0; i < 8; i++) {
                        gndPins[i] = NodeInst.makeInstance(m1Pin, new Point2D.Double(i * pitch + 120.0, -80.0),
                            m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst a = ArcInst.makeInstance(m1, ep, layDFF[i].findPortInstFromProto(dffLay.findExport("gnd")), gndPins[i].getPortInst(0));
                        a.setLambdaBaseWidth(4.0);
                        if (i > 0) {
                            ArcInst aH = ArcInst.makeInstance(m1, ep, gndPins[i-1].getPortInst(0), gndPins[i].getPortInst(0));
                            aH.setLambdaBaseWidth(4.0);
                        }
                    }
                    Export.newInst(layCell, gndPins[0].getPortInst(0), "gnd", ep);

                    // CLK horizontal rail at Y = -100.0 on Metal-2
                    NodeInst[] clkM2Pins = new NodeInst[8];
                    for (int i = 0; i < 8; i++) {
                        NodeInst vClk = NodeInst.makeInstance(via1, new Point2D.Double(i * pitch - 45.0, -1.0),
                            via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst aClkM1 = ArcInst.makeInstance(m1, ep, layDFF[i].findPortInstFromProto(dffLay.findExport("CLK")), vClk.getPortInst(0));
                        aClkM1.setLambdaBaseWidth(4.0);

                        clkM2Pins[i] = NodeInst.makeInstance(m2Pin, new Point2D.Double(i * pitch - 45.0, -100.0),
                            m2Pin.getDefWidth(ep), m2Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst aClkM2 = ArcInst.makeInstance(m2, ep, vClk.getPortInst(0), clkM2Pins[i].getPortInst(0));
                        aClkM2.setLambdaBaseWidth(4.0);
                        if (i > 0) {
                            ArcInst aH = ArcInst.makeInstance(m2, ep, clkM2Pins[i-1].getPortInst(0), clkM2Pins[i].getPortInst(0));
                            aH.setLambdaBaseWidth(4.0);
                        }
                    }
                    Export.newInst(layCell, clkM2Pins[0].getPortInst(0), "CLK", ep);

                    // ADDR_IN input pin
                    NodeInst pAddrIn = NodeInst.makeInstance(m1Pin, new Point2D.Double(80.0, 10.0),
                        m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst aIn = ArcInst.makeInstance(m1, ep, layDFF[0].findPortInstFromProto(dffLay.findExport("D")), pAddrIn.getPortInst(0));
                    aIn.setLambdaBaseWidth(4.0);
                    Export.newInst(layCell, pAddrIn.getPortInst(0), "ADDR_IN", ep);

                    // Cascaded Q to D (0..6)
                    for (int i = 0; i < 7; i++) {
                        NodeInst vQ_m2 = NodeInst.makeInstance(via1, new Point2D.Double(i * pitch + 565.0, 50.0),
                            via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst aQ = ArcInst.makeInstance(m1, ep, layDFF[i].findPortInstFromProto(dffLay.findExport("Q")), vQ_m2.getPortInst(0));
                        aQ.setLambdaBaseWidth(4.0);

                        NodeInst viaD = NodeInst.makeInstance(via1, new Point2D.Double((i + 1) * pitch + 80.0, 10.0),
                            via1.getDefWidth(ep), via1.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst aD = ArcInst.makeInstance(m1, ep, layDFF[i+1].findPortInstFromProto(dffLay.findExport("D")), viaD.getPortInst(0));
                        aD.setLambdaBaseWidth(4.0);

                        NodeInst dUp_m2 = NodeInst.makeInstance(m2Pin, new Point2D.Double((i + 1) * pitch + 80.0, 50.0),
                            m2Pin.getDefWidth(ep), m2Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                        ArcInst aD2 = ArcInst.makeInstance(m2, ep, viaD.getPortInst(0), dUp_m2.getPortInst(0));
                        aD2.setLambdaBaseWidth(4.0);

                        ArcInst aBridge = ArcInst.makeInstance(m2, ep, vQ_m2.getPortInst(0), dUp_m2.getPortInst(0));
                        aBridge.setLambdaBaseWidth(4.0);

                        Export.newInst(layCell, layDFF[i].findPortInstFromProto(dffLay.findExport("Q")), "Q[" + i + "]", ep);
                    }

                    // For Q[7]:
                    NodeInst vQ7 = NodeInst.makeInstance(m1Pin, new Point2D.Double(7 * pitch + 565.0, 50.0),
                        m1Pin.getDefWidth(ep), m1Pin.getDefHeight(ep), layCell, Orientation.IDENT, null);
                    ArcInst aQ7 = ArcInst.makeInstance(m1, ep, layDFF[7].findPortInstFromProto(dffLay.findExport("Q")), vQ7.getPortInst(0));
                    aQ7.setLambdaBaseWidth(4.0);
                    Export.newInst(layCell, layDFF[7].findPortInstFromProto(dffLay.findExport("Q")), "Q[7]", ep);

                    // ==========================================
                    // 4. VERIFICATION: DRC & NCC
                    // ==========================================
                    System.out.println("Running DRC on ShiftReg_8bit{lay}...");
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, layCell, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println("ShiftReg_8bit DRC Errors: " + errLog.getNumErrors() + ", Warnings: " + errLog.getNumWarnings());

                    System.out.println("Running NCC between ShiftReg_8bit{sch} and ShiftReg_8bit{lay}...");
                    NccOptions opts = new NccOptions();
                    opts.checkSizes = true;
                    NccResults res = Ncc.compare(schCell, null, layCell, null, opts);
                    System.out.println("ShiftReg_8bit NCC match = " + res.match());
                    System.out.println("ShiftReg_8bit NCC summary: " + res.summary(false));

                    if (errLog.getNumErrors() == 0 && res.match()) {
                        System.out.println("SUCCESS! Writing ShiftReg_8bit to SRAM.jelib...");
                        Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);

                        System.out.println("Exporting SPICE netlist ShiftReg_8bit.spi...");
                        Spice.SpicePreferences sp = (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, schCell, true, null);
                        sp.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                        sp.level = "3";
                        sp.doOutput(schCell, null, "ShiftReg_8bit.spi");
                        System.out.println("ShiftReg_8bit.spi exported successfully.");
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
