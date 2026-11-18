import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.technology.ArcProto;
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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.net.URL;

public class BuildCompactedMacroCleanV2 {

    public static final double[] WL_Y = {
        5.0, 153.0, 175.0, 323.0, 345.0, 493.0, 515.0, 663.0,
        685.0, 833.0, 855.0, 1003.0, 1025.0, 1173.0, 1195.0, 1343.0
    };

    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});

        Job j = new Job("BuildCompactedMacroCleanV2", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();
                    IconParameters ip = IconParameters.makeInstance(true);

                    // ==========================================
                    // 1. SCHEMATIC: SRAM_256_Macro{sch}
                    // ==========================================
                    System.out.println("=== 1. Building SRAM_256_Macro{sch} ===");

                    Cell icData = lib.findNodeProto("SRAM_Core_Datapath{ic}");
                    Cell icDec  = lib.findNodeProto("Decoder_4x16{ic}");
                    Cell icSreg = lib.findNodeProto("ShiftReg_8bit{ic}");
                    Cell icDiv8 = lib.findNodeProto("Div8_Counter{ic}");
                    Cell icSa   = lib.findNodeProto("sense_amplifier{ic}");
                    Cell icWd   = lib.findNodeProto("write_driver{ic}");
                    Cell icInv  = lib.findNodeProto("inverter{ic}");
                    Cell icNand = lib.findNodeProto("nand4{ic}");

                    Cell oldSch = lib.findNodeProto("SRAM_256_Macro{sch}");
                    if (oldSch != null) oldSch.kill();
                    Cell oldIc = lib.findNodeProto("SRAM_256_Macro{ic}");
                    if (oldIc != null) oldIc.kill();

                    Cell sch = Cell.makeInstance(lib, "SRAM_256_Macro{sch}");
                    ArcProto wire = sch.getTechnology().findArcProto("wire");
                    NodeProto offPage = sch.getTechnology().findNodeProto("Off-Page");
                    NodeProto pwrProto = sch.getTechnology().findNodeProto("Power");
                    NodeProto gndProto = sch.getTechnology().findNodeProto("Ground");

                    // Instantiations
                    NodeInst instData   = NodeInst.makeInstance(icData, new Point2D.Double(0.0, 0.0), 0, 0, sch);
                    NodeInst instRowDec = NodeInst.makeInstance(icDec,  new Point2D.Double(-200.0, 100.0), 0, 0, sch);
                    NodeInst instColDec = NodeInst.makeInstance(icDec,  new Point2D.Double(-200.0, -100.0), 0, 0, sch);
                    NodeInst instSreg   = NodeInst.makeInstance(icSreg, new Point2D.Double(-350.0, 0.0), 0, 0, sch);
                    NodeInst instDiv8   = NodeInst.makeInstance(icDiv8, new Point2D.Double(-350.0, -150.0), 0, 0, sch);
                    NodeInst instSa     = NodeInst.makeInstance(icSa,   new Point2D.Double(100.0, -150.0), 0, 0, sch);
                    NodeInst instWd     = NodeInst.makeInstance(icWd,   new Point2D.Double(0.0, -150.0), 0, 0, sch);

                    // 16 inverters for COL_SEL_B
                    NodeInst[] instColInvs = new NodeInst[16];
                    for (int c = 0; c < 16; c++) {
                        instColInvs[c] = NodeInst.makeInstance(icInv, new Point2D.Double(-100.0, -50.0 - c * 10.0), 0, 0, sch);
                    }

                    // Timing Inverters
                    NodeInst invD1 = NodeInst.makeInstance(icInv, new Point2D.Double(-250.0, -200.0), 0, 0, sch);
                    NodeInst invD2 = NodeInst.makeInstance(icInv, new Point2D.Double(-200.0, -200.0), 0, 0, sch);
                    NodeInst invD3 = NodeInst.makeInstance(icInv, new Point2D.Double(-150.0, -200.0), 0, 0, sch);
                    NodeInst invRw = NodeInst.makeInstance(icInv, new Point2D.Double(-250.0, -250.0), 0, 0, sch);

                    // AND Gates using nand4 + inverter
                    NodeInst nandWe = NodeInst.makeInstance(icNand, new Point2D.Double(-100.0, -220.0), 0, 0, sch);
                    NodeInst invWe  = NodeInst.makeInstance(icInv,  new Point2D.Double(-50.0, -220.0), 0, 0, sch);

                    NodeInst nandSae = NodeInst.makeInstance(icNand, new Point2D.Double(-100.0, -260.0), 0, 0, sch);
                    NodeInst invSae  = NodeInst.makeInstance(icInv,  new Point2D.Double(-50.0, -260.0), 0, 0, sch);

                    // Power & Ground
                    NodeInst pwr = NodeInst.makeInstance(pwrProto, new Point2D.Double(200.0, 100.0), 0, 0, sch);
                    NodeInst gnd = NodeInst.makeInstance(gndProto, new Point2D.Double(200.0, -100.0), 0, 0, sch);

                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), instData.findPortInstFromProto(icData.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), instRowDec.findPortInstFromProto(icDec.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), instColDec.findPortInstFromProto(icDec.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), instSreg.findPortInstFromProto(icSreg.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), instDiv8.findPortInstFromProto(icDiv8.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), instSa.findPortInstFromProto(icSa.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), instWd.findPortInstFromProto(icWd.findExport("vdd")));

                    ArcInst.makeInstance(wire, ep, gnd.getPortInst(0), instData.findPortInstFromProto(icData.findExport("gnd")));
                    ArcInst.makeInstance(wire, ep, gnd.getPortInst(0), instRowDec.findPortInstFromProto(icDec.findExport("gnd")));
                    ArcInst.makeInstance(wire, ep, gnd.getPortInst(0), instColDec.findPortInstFromProto(icDec.findExport("gnd")));
                    ArcInst.makeInstance(wire, ep, gnd.getPortInst(0), instSreg.findPortInstFromProto(icSreg.findExport("gnd")));
                    ArcInst.makeInstance(wire, ep, gnd.getPortInst(0), instDiv8.findPortInstFromProto(icDiv8.findExport("gnd")));
                    ArcInst.makeInstance(wire, ep, gnd.getPortInst(0), instSa.findPortInstFromProto(icSa.findExport("gnd")));
                    ArcInst.makeInstance(wire, ep, gnd.getPortInst(0), instWd.findPortInstFromProto(icWd.findExport("gnd")));

                    // Tie C and D of nandWe and nandSae to vdd
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), nandWe.findPortInstFromProto(icNand.findExport("C")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), nandWe.findPortInstFromProto(icNand.findExport("D")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), nandSae.findPortInstFromProto(icNand.findExport("C")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), nandSae.findPortInstFromProto(icNand.findExport("D")));

                    // Global Inputs
                    NodeInst pinClk    = NodeInst.makeInstance(offPage, new Point2D.Double(-400.0, 0.0), 0, 0, sch);
                    NodeInst pinAddrIn = NodeInst.makeInstance(offPage, new Point2D.Double(-400.0, 50.0), 0, 0, sch);
                    NodeInst pinRw     = NodeInst.makeInstance(offPage, new Point2D.Double(-400.0, -250.0), 0, 0, sch);
                    NodeInst pinDin    = NodeInst.makeInstance(offPage, new Point2D.Double(-100.0, -180.0), 0, 0, sch);

                    ArcInst.makeInstance(wire, ep, pinClk.getPortInst(0), instSreg.findPortInstFromProto(icSreg.findExport("CLK")));
                    ArcInst.makeInstance(wire, ep, pinClk.getPortInst(0), instDiv8.findPortInstFromProto(icDiv8.findExport("CLK")));
                    ArcInst.makeInstance(wire, ep, pinAddrIn.getPortInst(0), instSreg.findPortInstFromProto(icSreg.findExport("ADDR_IN")));
                    ArcInst.makeInstance(wire, ep, pinDin.getPortInst(0), instWd.findPortInstFromProto(icWd.findExport("WD")));

                    // Address Wiring
                    for (int b = 0; b < 4; b++) {
                        ArcInst.makeInstance(wire, ep,
                            instSreg.findPortInstFromProto(icSreg.findExport("Q[" + b + "]")),
                            instRowDec.findPortInstFromProto(icDec.findExport("A[" + b + "]")));
                        ArcInst.makeInstance(wire, ep,
                            instSreg.findPortInstFromProto(icSreg.findExport("Q[" + (b + 4) + "]")),
                            instColDec.findPortInstFromProto(icDec.findExport("A[" + b + "]")));
                    }

                    // Row Decode -> WL
                    for (int r = 0; r < 16; r++) {
                        ArcInst.makeInstance(wire, ep,
                            instRowDec.findPortInstFromProto(icDec.findExport("OUT[" + r + "]")),
                            instData.findPortInstFromProto(icData.findExport("WL[" + r + "]")));
                    }

                    // Col Decode -> COL_SEL & COL_SEL_B
                    for (int c = 0; c < 16; c++) {
                        PortInst pColOut = instColDec.findPortInstFromProto(icDec.findExport("OUT[" + c + "]"));
                        PortInst pColSel = instData.findPortInstFromProto(icData.findExport("COL_SEL[" + c + "]"));
                        PortInst pColSelB = instData.findPortInstFromProto(icData.findExport("COL_SEL_B[" + c + "]"));

                        ArcInst.makeInstance(wire, ep, pColOut, pColSel);
                        ArcInst.makeInstance(wire, ep, pColOut, instColInvs[c].findPortInstFromProto(icInv.findExport("in")));
                        ArcInst.makeInstance(wire, ep, instColInvs[c].findPortInstFromProto(icInv.findExport("out")), pColSelB);
                    }

                    // Global Bitlines
                    PortInst pGbl = instData.findPortInstFromProto(icData.findExport("GBL"));
                    PortInst pGblb = instData.findPortInstFromProto(icData.findExport("GBLB"));
                    ArcInst.makeInstance(wire, ep, pGbl, instSa.findPortInstFromProto(icSa.findExport("BL")));
                    ArcInst.makeInstance(wire, ep, pGbl, instWd.findPortInstFromProto(icWd.findExport("BL")));
                    ArcInst.makeInstance(wire, ep, pGblb, instSa.findPortInstFromProto(icSa.findExport("BLB")));
                    ArcInst.makeInstance(wire, ep, pGblb, instWd.findPortInstFromProto(icWd.findExport("BLB")));

                    // Timing Logic
                    PortInst pExec = instDiv8.findPortInstFromProto(icDiv8.findExport("EXECUTE"));
                    ArcInst.makeInstance(wire, ep, pExec, instData.findPortInstFromProto(icData.findExport("PRE_B")));
                    ArcInst.makeInstance(wire, ep, pExec, invD1.findPortInstFromProto(icInv.findExport("in")));
                    ArcInst.makeInstance(wire, ep, invD1.findPortInstFromProto(icInv.findExport("out")), invD2.findPortInstFromProto(icInv.findExport("in")));
                    ArcInst.makeInstance(wire, ep, invD2.findPortInstFromProto(icInv.findExport("out")), invD3.findPortInstFromProto(icInv.findExport("in")));

                    ArcInst.makeInstance(wire, ep, pinRw.getPortInst(0), invRw.findPortInstFromProto(icInv.findExport("in")));

                    // Write: D2 AND R_W -> WE
                    ArcInst.makeInstance(wire, ep, invD2.findPortInstFromProto(icInv.findExport("out")), nandWe.findPortInstFromProto(icNand.findExport("A")));
                    ArcInst.makeInstance(wire, ep, pinRw.getPortInst(0), nandWe.findPortInstFromProto(icNand.findExport("B")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), nandWe.findPortInstFromProto(icNand.findExport("C")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), nandWe.findPortInstFromProto(icNand.findExport("D")));
                    ArcInst.makeInstance(wire, ep, nandWe.findPortInstFromProto(icNand.findExport("out")), invWe.findPortInstFromProto(icInv.findExport("in")));
                    ArcInst.makeInstance(wire, ep, invWe.findPortInstFromProto(icInv.findExport("out")), instWd.findPortInstFromProto(icWd.findExport("WE")));

                    // Read: D3 AND R_W_b -> SAE
                    ArcInst.makeInstance(wire, ep, invD3.findPortInstFromProto(icInv.findExport("out")), nandSae.findPortInstFromProto(icNand.findExport("A")));
                    ArcInst.makeInstance(wire, ep, invRw.findPortInstFromProto(icInv.findExport("out")), nandSae.findPortInstFromProto(icNand.findExport("B")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), nandSae.findPortInstFromProto(icNand.findExport("C")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), nandSae.findPortInstFromProto(icNand.findExport("D")));
                    ArcInst.makeInstance(wire, ep, nandSae.findPortInstFromProto(icNand.findExport("out")), invSae.findPortInstFromProto(icInv.findExport("in")));
                    ArcInst.makeInstance(wire, ep, invSae.findPortInstFromProto(icInv.findExport("out")), instSa.findPortInstFromProto(icSa.findExport("SAE")));

                    // Output DOUT
                    NodeInst pinDout = NodeInst.makeInstance(offPage, new Point2D.Double(200.0, -150.0), 0, 0, sch);
                    ArcInst.makeInstance(wire, ep, instSa.findPortInstFromProto(icSa.findExport("SO")), pinDout.getPortInst(0));

                    // Exports on Schematic (7 total)
                    Export.newInst(sch, pinClk.getPortInst(0), "CLK", ep);
                    Export.newInst(sch, pinAddrIn.getPortInst(0), "ADDR_IN", ep);
                    Export.newInst(sch, pinRw.getPortInst(0), "R_W", ep);
                    Export.newInst(sch, pinDin.getPortInst(0), "DIN", ep);
                    Export.newInst(sch, pinDout.getPortInst(0), "DOUT", ep);
                    Export.newInst(sch, pwr.getPortInst(0), "vdd", ep);
                    Export.newInst(sch, gnd.getPortInst(0), "gnd", ep);

                    Cell ic = ip.makeIconForCell(sch, ep);
                    System.out.println("  SRAM_256_Macro{sch} and {ic} generated successfully with 7 exports.");

                    // ==========================================
                    // 2. LAYOUT: SRAM_256_Macro{lay}
                    // ==========================================
                    System.out.println("=== 2. Building Clean Compacted SRAM_256_Macro{lay} ===");

                    Cell layData = lib.findNodeProto("SRAM_Core_Datapath{lay}");
                    Cell layDec  = lib.findNodeProto("Decoder_4x16{lay}");
                    Cell laySreg = lib.findNodeProto("ShiftReg_8bit{lay}");
                    Cell layDiv8 = lib.findNodeProto("Div8_Counter{lay}");
                    Cell laySa   = lib.findNodeProto("sense_amplifier{lay}");
                    Cell layWd   = lib.findNodeProto("write_driver{lay}");
                    Cell layInv  = lib.findNodeProto("inverter{lay}");
                    Cell layNand = lib.findNodeProto("nand4{lay}");

                    Cell oldLay = lib.findNodeProto("SRAM_256_Macro{lay}");
                    if (oldLay != null) oldLay.kill();

                    Cell lay = Cell.makeInstance(lib, "SRAM_256_Macro{lay}");
                    ArcProto poly = lay.getTechnology().findArcProto("Polysilicon-1");
                    ArcProto m1   = lay.getTechnology().findArcProto("Metal-1");
                    ArcProto m2   = lay.getTechnology().findArcProto("Metal-2");
                    ArcProto m3   = lay.getTechnology().findArcProto("Metal-3");

                    NodeProto conPolyM1  = lay.getTechnology().findNodeProto("Metal-1-Polysilicon-1-Con");
                    NodeProto via12Proto = lay.getTechnology().findNodeProto("Metal-1-Metal-2-Con");
                    NodeProto via23Proto = lay.getTechnology().findNodeProto("Metal-2-Metal-3-Con");
                    NodeProto m1Pin      = lay.getTechnology().findNodeProto("Metal-1-Pin");
                    NodeProto m2Pin      = lay.getTechnology().findNodeProto("Metal-2-Pin");
                    NodeProto m3Pin      = lay.getTechnology().findNodeProto("Metal-3-Pin");
                    NodeProto polyPin    = lay.getTechnology().findNodeProto("Polysilicon-1-Pin");

                    // Optimized Coordinates with verified clearance
                    double xRowDec = -640.0, yRowDec = 0.0;
                    double xColDec = -640.0, yColDec = -1850.0;
                    double xData = 0.0, yData = 0.0;
                    double xWd = 0.0, yWd = -340.0;
                    double xSa = 200.0, ySa = -340.0;
                    double xDiv8 = -640.0, yDiv8 = 1530.0;
                    double yTiming = 1920.0;
                    double xSreg = -640.0, ySreg = 2100.0;

                    NodeInst lInstData   = NodeInst.makeInstance(layData, new Point2D.Double(xData, yData), 0, 0, lay);
                    NodeInst lInstRowDec = NodeInst.makeInstance(layDec,  new Point2D.Double(xRowDec, yRowDec), 0, 0, lay);
                    NodeInst lInstColDec = NodeInst.makeInstance(layDec,  new Point2D.Double(xColDec, yColDec), 0, 0, lay);
                    NodeInst lInstWd     = NodeInst.makeInstance(layWd,   new Point2D.Double(xWd, yWd), 0, 0, lay);
                    NodeInst lInstSa     = NodeInst.makeInstance(laySa,   new Point2D.Double(xSa, ySa), 0, 0, lay);
                    NodeInst lInstDiv8   = NodeInst.makeInstance(layDiv8, new Point2D.Double(xDiv8, yDiv8), 0, 0, lay);
                    NodeInst lInstSreg   = NodeInst.makeInstance(laySreg, new Point2D.Double(xSreg, ySreg), 0, 0, lay);

                    // 16 inverters along Y = -720.0 (in the wide open channel below SA/WD)
                    double yColInvs = -720.0;
                    NodeInst[] lColInvs = new NodeInst[16];
                    for (int c = 0; c < 16; c++) {
                        lColInvs[c] = NodeInst.makeInstance(layInv, new Point2D.Double(c * 76.0 + 35.0, yColInvs), 0, 0, lay);
                    }

                    // Timing gates at Y = 1920.0
                    NodeInst lInvD1   = NodeInst.makeInstance(layInv,  new Point2D.Double(-400.0, yTiming), 0, 0, lay);
                    NodeInst lInvD2   = NodeInst.makeInstance(layInv,  new Point2D.Double(-300.0, yTiming), 0, 0, lay);
                    NodeInst lInvD3   = NodeInst.makeInstance(layInv,  new Point2D.Double(-200.0, yTiming), 0, 0, lay);
                    NodeInst lInvRw   = NodeInst.makeInstance(layInv,  new Point2D.Double(-100.0, yTiming), 0, 0, lay);
                    NodeInst lNandWe  = NodeInst.makeInstance(layNand, new Point2D.Double(0.0, yTiming), 0, 0, lay);
                    NodeInst lInvWe   = NodeInst.makeInstance(layInv,  new Point2D.Double(80.0, yTiming), 0, 0, lay);
                    NodeInst lNandSae = NodeInst.makeInstance(layNand, new Point2D.Double(180.0, yTiming), 0, 0, lay);
                    NodeInst lInvSae  = NodeInst.makeInstance(layInv,  new Point2D.Double(260.0, yTiming), 0, 0, lay);

                    // ------------------------------------------
                    // ROUTING A: Wordlines (Row_Decoder -> Datapath)
                    // ------------------------------------------
                    for (int r = 0; r < 16; r++) {
                        PortInst pDecOut = lInstRowDec.findPortInstFromProto(layDec.findExport("OUT[" + r + "]"));
                        PortInst pDatWL  = lInstData.findPortInstFromProto(layData.findExport("WL[" + r + "]"));
                        double y = pDecOut.getCenter().getY();

                        NodeInst v12_left = NodeInst.makeInstance(via12Proto, new Point2D.Double(-100.0, y), 5.0, 5.0, lay);
                        NodeInst v23_left = NodeInst.makeInstance(via23Proto, new Point2D.Double(-100.0, y), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pDecOut, v12_left.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_left.getPortInst(0), v23_left.getPortInst(0));

                        NodeInst v23_right = NodeInst.makeInstance(via23Proto, new Point2D.Double(10.0, y), 6.0, 6.0, lay);
                        NodeInst v12_right = NodeInst.makeInstance(via12Proto, new Point2D.Double(10.0, y), 5.0, 5.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_left.getPortInst(0), v23_right.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v23_right.getPortInst(0), v12_right.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, v12_right.getPortInst(0), pDatWL);
                    }

                    // ------------------------------------------
                    // ROUTING B: Column Select & Inverters
                    // ------------------------------------------
                    for (int c = 0; c < 16; c++) {
                        PortInst pDecOut = lInstColDec.findPortInstFromProto(layDec.findExport("OUT[" + c + "]"));
                        double yOut = pDecOut.getCenter().getY();
                        PortInst pInvIn  = lColInvs[c].findPortInstFromProto(layInv.findExport("in"));
                        PortInst pInvOut = lColInvs[c].findPortInstFromProto(layInv.findExport("out"));
                        PortInst pDatSel  = lInstData.findPortInstFromProto(layData.findExport("COL_SEL[" + c + "]"));
                        PortInst pDatSelB = lInstData.findPortInstFromProto(layData.findExport("COL_SEL_B[" + c + "]"));

                        NodeInst v12_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(-100.0, yOut), 5.0, 5.0, lay);
                        NodeInst v23_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(-100.0, yOut), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pDecOut, v12_out.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_out.getPortInst(0), v23_out.getPortInst(0));

                        double xSelTrack = c * 76.0 + 10.0;
                        if (c == 0) xSelTrack = -25.0;
                        if (c == 3) xSelTrack = c * 76.0 + 22.0;

                        NodeInst v23_m3_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(xSelTrack, yOut), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_out.getPortInst(0), v23_m3_bot.getPortInst(0));

                        NodeInst v12_inv_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(xSelTrack, yColInvs - 1.0), 5.0, 5.0, lay);
                        ArcInst.makeInstance(m1, ep, v12_inv_in.getPortInst(0), pInvIn);

                        if (c == 0) {
                            NodeInst v12_m2_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-25.0, yColInvs - 1.0), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_m3_bot.getPortInst(0), v12_m2_bot.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_m2_bot.getPortInst(0), v12_inv_in.getPortInst(0));
                        } else {
                            ArcInst.makeInstance(m2, ep, v23_m3_bot.getPortInst(0), v12_inv_in.getPortInst(0));
                        }

                        // Connect to COL_SEL[c]: poly tap down to Y = -450.0 (wide open channel)
                        NodeInst cPoly_sel = NodeInst.makeInstance(conPolyM1, new Point2D.Double(c * 76.0, -450.0), 5.0, 5.0, lay);
                        NodeInst v12_sel   = NodeInst.makeInstance(via12Proto, new Point2D.Double(c * 76.0, -450.0), 5.0, 5.0, lay);
                        ArcInst.makeInstance(poly, ep, pDatSel, cPoly_sel.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, cPoly_sel.getPortInst(0), v12_sel.getPortInst(0));

                        if (c == 0) {
                            NodeInst v12_sel_c0 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-25.0, -450.0), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_m3_bot.getPortInst(0), v12_sel_c0.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_sel_c0.getPortInst(0), v12_sel.getPortInst(0));
                        } else {
                            NodeInst v12_sel_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(xSelTrack, -450.0), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v12_inv_in.getPortInst(0), v12_sel_top.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_sel_top.getPortInst(0), v12_sel.getPortInst(0));
                        }

                        // Connect to COL_SEL_B[c]: poly tap down to Y = -340.0
                        double x = c * 76.0;
                        NodeInst cPoly_selb = NodeInst.makeInstance(conPolyM1, new Point2D.Double(x, -340.0), 5.0, 5.0, lay);
                        NodeInst v12_selb   = NodeInst.makeInstance(via12Proto, new Point2D.Double(x, -340.0), 5.0, 5.0, lay);
                        ArcInst.makeInstance(poly, ep, pDatSelB, cPoly_selb.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, cPoly_selb.getPortInst(0), v12_selb.getPortInst(0));

                        NodeInst v12_inv_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(c * 76.0 + 43.0, yColInvs - 1.5), 5.0, 5.0, lay);
                        NodeInst v23_inv_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(c * 76.0 + 43.0, yColInvs - 1.5), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pInvOut, v12_inv_out.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_inv_out.getPortInst(0), v23_inv_out.getPortInst(0));

                        if (c == 0) {
                            NodeInst v23_bridge = NodeInst.makeInstance(via23Proto, new Point2D.Double(-40.0, -340.0), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m2, ep, v12_selb.getPortInst(0), v23_bridge.getPortInst(0));
                            NodeInst jogLeft = NodeInst.makeInstance(m3Pin, new Point2D.Double(-120.0, -340.0), 3.0, 3.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_bridge.getPortInst(0), jogLeft.getPortInst(0));

                            NodeInst v23_b0_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(-120.0, yColInvs - 1.5), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, jogLeft.getPortInst(0), v23_b0_bot.getPortInst(0));
                            ArcInst.makeInstance(m3, ep, v23_b0_bot.getPortInst(0), v23_inv_out.getPortInst(0));
                        } else {
                            NodeInst v23_selb_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(x, -340.0), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m2, ep, v12_selb.getPortInst(0), v23_selb_top.getPortInst(0));

                            NodeInst v23_selb_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(x, yColInvs - 1.5), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_selb_top.getPortInst(0), v23_selb_bot.getPortInst(0));
                            ArcInst.makeInstance(m3, ep, v23_selb_bot.getPortInst(0), v23_inv_out.getPortInst(0));
                        }
                    }

                    // ------------------------------------------
                    // ROUTING C: Global Bitlines (Flawless Manhattan)
                    // ------------------------------------------
                    PortInst lGbl   = lInstData.findPortInstFromProto(layData.findExport("GBL"));
                    PortInst lGblb  = lInstData.findPortInstFromProto(layData.findExport("GBLB"));
                    PortInst pWdBl  = lInstWd.findPortInstFromProto(layWd.findExport("BL"));
                    PortInst pWdBlb = lInstWd.findPortInstFromProto(layWd.findExport("BLB"));
                    PortInst pSaBl  = lInstSa.findPortInstFromProto(laySa.findExport("BL"));
                    PortInst pSaBlb = lInstSa.findPortInstFromProto(laySa.findExport("BLB"));

                    // GBL: starts at (-5.0, -225.0) on Metal-2
                    NodeInst v12_gbl_trunk = NodeInst.makeInstance(via12Proto, new Point2D.Double(-5.0, -265.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, lGbl, v12_gbl_trunk.getPortInst(0));

                    NodeInst v12_gbl_wd = NodeInst.makeInstance(via12Proto, new Point2D.Double(11.5, -265.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_gbl_trunk.getPortInst(0), v12_gbl_wd.getPortInst(0));
                    NodeInst v12_wd_bl = NodeInst.makeInstance(via12Proto, new Point2D.Double(11.5, -344.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_gbl_wd.getPortInst(0), v12_wd_bl.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_wd_bl.getPortInst(0), pWdBl);

                    NodeInst v12_gbl_sa = NodeInst.makeInstance(via12Proto, new Point2D.Double(180.0, -265.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_gbl_wd.getPortInst(0), v12_gbl_sa.getPortInst(0));
                    NodeInst v12_sa_bl = NodeInst.makeInstance(via12Proto, new Point2D.Double(180.0, -320.0), 5.0, 5.0, lay);
                    NodeInst cPoly_sa_bl = NodeInst.makeInstance(conPolyM1, new Point2D.Double(180.0, -320.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_gbl_sa.getPortInst(0), v12_sa_bl.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sa_bl.getPortInst(0), cPoly_sa_bl.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_sa_bl.getPortInst(0), pSaBl);

                    // GBLB: starts at (35.0, -135.0) on Metal-2
                    NodeInst v12_gblb_trunk = NodeInst.makeInstance(via12Proto, new Point2D.Double(35.0, -250.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, lGblb, v12_gblb_trunk.getPortInst(0));

                    NodeInst v12_gblb_wd = NodeInst.makeInstance(via12Proto, new Point2D.Double(23.5, -250.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_gblb_trunk.getPortInst(0), v12_gblb_wd.getPortInst(0));
                    NodeInst v12_wd_blb = NodeInst.makeInstance(via12Proto, new Point2D.Double(23.5, -344.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_gblb_wd.getPortInst(0), v12_wd_blb.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_wd_blb.getPortInst(0), pWdBlb);

                    NodeInst v12_gblb_sa = NodeInst.makeInstance(via12Proto, new Point2D.Double(220.0, -250.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_gblb_trunk.getPortInst(0), v12_gblb_sa.getPortInst(0));
                    NodeInst v12_sa_blb = NodeInst.makeInstance(via12Proto, new Point2D.Double(220.0, -320.0), 5.0, 5.0, lay);
                    NodeInst cPoly_sa_blb = NodeInst.makeInstance(conPolyM1, new Point2D.Double(220.0, -320.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_gblb_sa.getPortInst(0), v12_sa_blb.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sa_blb.getPortInst(0), cPoly_sa_blb.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_sa_blb.getPortInst(0), pSaBlb);

                    // ------------------------------------------
                    // ROUTING D: Address Bus (Proven Manhattan Architecture)
                    // ------------------------------------------
                    // Q[0:3] -> Row_Decoder.A[0:3]
                    for (int b = 0; b < 4; b++) {
                        PortInst pQ = lInstSreg.findPortInstFromProto(laySreg.findExport("Q[" + b + "]"));
                        PortInst pA = lInstRowDec.findPortInstFromProto(layDec.findExport("A[" + b + "]"));

                        double xQ = pQ.getCenter().getX();
                        double yQ = pQ.getCenter().getY();
                        double xTrack = -950.0 - b * 12.0;
                        double xA = pA.getCenter().getX();
                        double yA = pA.getCenter().getY();

                        NodeInst v12_q = NodeInst.makeInstance(via12Proto, new Point2D.Double(xQ, yQ), 5.0, 5.0, lay);
                        NodeInst v23_q = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yQ), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pQ, v12_q.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_q.getPortInst(0), v23_q.getPortInst(0));

                        double yJog = 2720.0 + b * 12.0;
                        NodeInst v23_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yJog), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_q.getPortInst(0), v23_top.getPortInst(0));

                        NodeInst v23_west = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yJog), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_top.getPortInst(0), v23_west.getPortInst(0));

                        NodeInst v12_a_corner = NodeInst.makeInstance(via12Proto, new Point2D.Double(xTrack, yA), 5.0, 5.0, lay);
                        NodeInst v23_a_corner = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yA), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m2, ep, v23_west.getPortInst(0), v23_a_corner.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v23_a_corner.getPortInst(0), v12_a_corner.getPortInst(0));

                        NodeInst v12_a = NodeInst.makeInstance(via12Proto, new Point2D.Double(xA, yA), 5.0, 5.0, lay);
                        ArcInst.makeInstance(m1, ep, v12_a_corner.getPortInst(0), v12_a.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, v12_a.getPortInst(0), pA);
                    }

                    // Q[4:7] -> Col_Decoder.A[0:3]
                    for (int b = 0; b < 4; b++) {
                        PortInst pQ = lInstSreg.findPortInstFromProto(laySreg.findExport("Q[" + (b + 4) + "]"));
                        PortInst pA = lInstColDec.findPortInstFromProto(layDec.findExport("A[" + b + "]"));

                        double xQ = pQ.getCenter().getX();
                        double yQ = pQ.getCenter().getY();
                        double xTrack = -1000.0 - b * 12.0;
                        double xA = pA.getCenter().getX();
                        double yA = pA.getCenter().getY();

                        NodeInst v12_q = NodeInst.makeInstance(via12Proto, new Point2D.Double(xQ, yQ), 5.0, 5.0, lay);
                        NodeInst v23_q = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yQ), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pQ, v12_q.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_q.getPortInst(0), v23_q.getPortInst(0));

                        double yJog = 2770.0 + b * 12.0;
                        NodeInst v23_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yJog), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_q.getPortInst(0), v23_top.getPortInst(0));

                        NodeInst v23_west = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yJog), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_top.getPortInst(0), v23_west.getPortInst(0));

                        NodeInst v12_a_corner = NodeInst.makeInstance(via12Proto, new Point2D.Double(xTrack, yA), 5.0, 5.0, lay);
                        NodeInst v23_a_corner = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yA), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m2, ep, v23_west.getPortInst(0), v23_a_corner.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v23_a_corner.getPortInst(0), v12_a_corner.getPortInst(0));

                        NodeInst v12_a = NodeInst.makeInstance(via12Proto, new Point2D.Double(xA, yA), 5.0, 5.0, lay);
                        ArcInst.makeInstance(m1, ep, v12_a_corner.getPortInst(0), v12_a.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, v12_a.getPortInst(0), pA);
                    }

                    // ------------------------------------------
                    // ROUTING E: Timing Logic & Control
                    // ------------------------------------------
                    // EXECUTE port of Div8: at (468.0, yDiv8 + 178.5) = (468.0, 1708.5)
                    PortInst pExecLay = lInstDiv8.findPortInstFromProto(layDiv8.findExport("EXECUTE"));
                    double xExec = 468.0;
                    double yExec = yDiv8 + 178.5;

                    NodeInst v12_exec = NodeInst.makeInstance(via12Proto, new Point2D.Double(xExec, yExec), 5.0, 5.0, lay);
                    NodeInst v23_exec = NodeInst.makeInstance(via23Proto, new Point2D.Double(xExec, yExec), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pExecLay, v12_exec.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_exec.getPortInst(0), v23_exec.getPortInst(0));

                    // Route M3 to D1 input at (-425.0, yTiming - 1.0)
                    NodeInst v23_d1_in = NodeInst.makeInstance(via23Proto, new Point2D.Double(-425.0, yTiming - 1.0), 6.0, 6.0, lay);
                    NodeInst v12_d1_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(-425.0, yTiming - 1.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_exec.getPortInst(0), v23_d1_in.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_d1_in.getPortInst(0), v12_d1_in.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_d1_in.getPortInst(0), lInvD1.findPortInstFromProto(layInv.findExport("in")));

                    // Route M3 down to PRE_B at (-34.25, 1381.5)
                    PortInst pPreB = lInstData.findPortInstFromProto(layData.findExport("PRE_B"));
                    NodeInst v23_pre = NodeInst.makeInstance(via23Proto, new Point2D.Double(-34.25, 1381.5), 6.0, 6.0, lay);
                    NodeInst v12_pre = NodeInst.makeInstance(via12Proto, new Point2D.Double(-34.25, 1381.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_exec.getPortInst(0), v23_pre.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_pre.getPortInst(0), v12_pre.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_pre.getPortInst(0), pPreB);

                    // Cascade D1 -> D2
                    PortInst pD1Out = lInvD1.findPortInstFromProto(layInv.findExport("out"));
                    PortInst pD2In  = lInvD2.findPortInstFromProto(layInv.findExport("in"));
                    ArcInst.makeInstance(m1, ep, pD1Out, pD2In);

                    // Cascade D2 -> D3
                    PortInst pD2Out = lInvD2.findPortInstFromProto(layInv.findExport("out"));
                    PortInst pD3In  = lInvD3.findPortInstFromProto(layInv.findExport("in"));
                    ArcInst.makeInstance(m1, ep, pD2Out, pD3In);

                    // D2 also drives NAND_WE(A) at (-56.0, yTiming - 52.5)
                    NodeInst v12_d2_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(-292.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_d2_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(-292.0, yTiming - 1.5), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pD2Out, v12_d2_top.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_d2_top.getPortInst(0), v23_d2_top.getPortInst(0));

                    NodeInst v23_d2_nand = NodeInst.makeInstance(via23Proto, new Point2D.Double(-56.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v12_d2_nand = NodeInst.makeInstance(via12Proto, new Point2D.Double(-56.0, yTiming - 1.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_d2_top.getPortInst(0), v23_d2_nand.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_d2_nand.getPortInst(0), v12_d2_nand.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_d2_nand.getPortInst(0), lNandWe.findPortInstFromProto(layNand.findExport("A")));

                    // D3 drives NAND_SAE(A) at (124.0, yTiming - 52.5)
                    PortInst pD3Out = lInvD3.findPortInstFromProto(layInv.findExport("out"));
                    NodeInst v12_d3_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(-192.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_d3_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(-192.0, yTiming - 1.5), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pD3Out, v12_d3_top.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_d3_top.getPortInst(0), v23_d3_top.getPortInst(0));

                    NodeInst v23_d3_nand = NodeInst.makeInstance(via23Proto, new Point2D.Double(124.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v12_d3_nand = NodeInst.makeInstance(via12Proto, new Point2D.Double(124.0, yTiming - 1.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_d3_top.getPortInst(0), v23_d3_nand.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_d3_nand.getPortInst(0), v12_d3_nand.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_d3_nand.getPortInst(0), lNandSae.findPortInstFromProto(layNand.findExport("A")));

                    // External R_W pin at (-700.0, 1800.0)
                    NodeInst pinRwLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-700.0, 1800.0), 3.0, 3.0, lay);
                    NodeInst v12_rw_trunk = NodeInst.makeInstance(via12Proto, new Point2D.Double(-700.0, 1800.0), 5.0, 5.0, lay);
                    NodeInst v23_rw_trunk = NodeInst.makeInstance(via23Proto, new Point2D.Double(-700.0, 1800.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pinRwLay.getPortInst(0), v12_rw_trunk.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_rw_trunk.getPortInst(0), v23_rw_trunk.getPortInst(0));

                    // R_W -> invRw.in at (-125.0, yTiming - 1.0)
                    NodeInst v23_rw_inv = NodeInst.makeInstance(via23Proto, new Point2D.Double(-125.0, 1800.0), 6.0, 6.0, lay);
                    NodeInst v12_rw_inv = NodeInst.makeInstance(via12Proto, new Point2D.Double(-125.0, 1800.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_rw_trunk.getPortInst(0), v23_rw_inv.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_rw_inv.getPortInst(0), v12_rw_inv.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_rw_inv.getPortInst(0), lInvRw.findPortInstFromProto(layInv.findExport("in")));

                    // R_W -> NAND_WE(B) at (-41.5, yTiming - 52.5)
                    NodeInst v23_rw_we = NodeInst.makeInstance(via23Proto, new Point2D.Double(-41.5, 1800.0), 6.0, 6.0, lay);
                    NodeInst v12_rw_we = NodeInst.makeInstance(via12Proto, new Point2D.Double(-41.5, 1800.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_rw_inv.getPortInst(0), v23_rw_we.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_rw_we.getPortInst(0), v12_rw_we.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_rw_we.getPortInst(0), lNandWe.findPortInstFromProto(layNand.findExport("B")));

                    // invRw.out -> NAND_SAE(B) at (138.5, yTiming - 52.5)
                    PortInst pRwBOut = lInvRw.findPortInstFromProto(layInv.findExport("out"));
                    NodeInst v12_rwb_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(-92.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_rwb_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(-92.0, yTiming - 1.5), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pRwBOut, v12_rwb_top.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_rwb_top.getPortInst(0), v23_rwb_top.getPortInst(0));

                    NodeInst v23_rwb_nand = NodeInst.makeInstance(via23Proto, new Point2D.Double(138.5, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v12_rwb_nand = NodeInst.makeInstance(via12Proto, new Point2D.Double(138.5, yTiming - 1.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_rwb_top.getPortInst(0), v23_rwb_nand.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_rwb_nand.getPortInst(0), v12_rwb_nand.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_rwb_nand.getPortInst(0), lNandSae.findPortInstFromProto(layNand.findExport("B")));

                    // Tie NAND_WE C, D to VDD at (0.0, yTiming + 28.5)
                    PortInst pVddNandWe = lNandWe.findPortInstFromProto(layNand.findExport("vdd"));
                    ArcInst.makeInstance(m1, ep, lNandWe.findPortInstFromProto(layNand.findExport("C")), pVddNandWe);
                    ArcInst.makeInstance(m1, ep, lNandWe.findPortInstFromProto(layNand.findExport("D")), pVddNandWe);

                    // Tie NAND_SAE C, D to VDD at (180.0, yTiming + 28.5)
                    PortInst pVddNandSae = lNandSae.findPortInstFromProto(layNand.findExport("vdd"));
                    ArcInst.makeInstance(m1, ep, lNandSae.findPortInstFromProto(layNand.findExport("C")), pVddNandSae);
                    ArcInst.makeInstance(m1, ep, lNandSae.findPortInstFromProto(layNand.findExport("D")), pVddNandSae);

                    // NAND_WE.out -> invWe.in
                    PortInst pNandWeOut = lNandWe.findPortInstFromProto(layNand.findExport("out"));
                    PortInst pInvWeIn   = lInvWe.findPortInstFromProto(layInv.findExport("in"));
                    NodeInst v12_nand_we = NodeInst.makeInstance(via12Proto, new Point2D.Double(-28.0, yTiming + 45.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, pNandWeOut, v12_nand_we.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_nand_we.getPortInst(0), pInvWeIn);

                    // invWe.out -> write_driver.WE at (17.5, -348.0)
                    PortInst pWeOut = lInvWe.findPortInstFromProto(layInv.findExport("out"));
                    NodeInst v12_we_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(88.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_we_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(88.0, yTiming - 1.5), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pWeOut, v12_we_top.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_we_top.getPortInst(0), v23_we_top.getPortInst(0));

                    NodeInst v23_we_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(88.0, -290.0), 6.0, 6.0, lay);
                    NodeInst v12_we_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(88.0, -290.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_we_top.getPortInst(0), v23_we_bot.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_we_bot.getPortInst(0), v12_we_bot.getPortInst(0));

                    NodeInst v12_we_wd = NodeInst.makeInstance(via12Proto, new Point2D.Double(17.5, -290.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_we_bot.getPortInst(0), v12_we_wd.getPortInst(0));

                    NodeInst cPoly_we = NodeInst.makeInstance(conPolyM1, new Point2D.Double(17.5, -315.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_we_wd.getPortInst(0), cPoly_we.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_we.getPortInst(0), lInstWd.findPortInstFromProto(layWd.findExport("WE")));

                    // NAND_SAE.out -> invSae.in
                    PortInst pNandSaeOut = lNandSae.findPortInstFromProto(layNand.findExport("out"));
                    PortInst pInvSaeIn   = lInvSae.findPortInstFromProto(layInv.findExport("in"));
                    NodeInst v12_nand_sae = NodeInst.makeInstance(via12Proto, new Point2D.Double(152.0, yTiming + 45.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, pNandSaeOut, v12_nand_sae.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_nand_sae.getPortInst(0), pInvSaeIn);

                    // invSae.out -> sense_amplifier.SAE at (200.0, -378.0)
                    PortInst pSaeOut = lInvSae.findPortInstFromProto(layInv.findExport("out"));
                    NodeInst v12_sae_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(268.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_sae_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(268.0, yTiming - 1.5), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pSaeOut, v12_sae_top.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_sae_top.getPortInst(0), v23_sae_top.getPortInst(0));

                    NodeInst v23_sae_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(268.0, -300.0), 6.0, 6.0, lay);
                    NodeInst v12_sae_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(268.0, -300.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_sae_top.getPortInst(0), v23_sae_bot.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_sae_bot.getPortInst(0), v12_sae_bot.getPortInst(0));

                    NodeInst v12_sae_sa = NodeInst.makeInstance(via12Proto, new Point2D.Double(200.0, -300.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_sae_bot.getPortInst(0), v12_sae_sa.getPortInst(0));

                    NodeInst cPoly_sae = NodeInst.makeInstance(conPolyM1, new Point2D.Double(200.0, -325.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_sae_sa.getPortInst(0), cPoly_sae.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_sae.getPortInst(0), lInstSa.findPortInstFromProto(laySa.findExport("SAE")));

                    // ------------------------------------------
                    // ROUTING F: Power & Ground Network (Strict Manhattan!)
                    // ------------------------------------------
                    PortInst pVddDat = lInstData.findPortInstFromProto(layData.findExport("vdd"));
                    PortInst pGndDat = lInstData.findPortInstFromProto(layData.findExport("gnd"));

                    // Downward Power:
                    NodeInst v12_vdd_row = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, -200.0), 5.0, 5.0, lay);
                    NodeInst v12_vdd_sa  = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, -288.0), 5.0, 5.0, lay);
                    NodeInst v12_vdd_inv = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, yColInvs + 21.5), 5.0, 5.0, lay);
                    NodeInst v12_vdd_col = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, yColDec - 400.0), 5.0, 5.0, lay);

                    ArcInst.makeInstance(m2, ep, pVddDat, v12_vdd_row.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_vdd_row.getPortInst(0), v12_vdd_sa.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_vdd_sa.getPortInst(0), v12_vdd_inv.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_vdd_inv.getPortInst(0), v12_vdd_col.getPortInst(0));

                    NodeInst v12_gnd_row = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, -220.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_wd  = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, -384.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_inv = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, yColInvs - 23.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_col = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, yColDec - 420.0), 5.0, 5.0, lay);

                    ArcInst.makeInstance(m2, ep, pGndDat, v12_gnd_row.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_row.getPortInst(0), v12_gnd_wd.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_wd.getPortInst(0), v12_gnd_inv.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_inv.getPortInst(0), v12_gnd_col.getPortInst(0));

                    // Row_Decoder power: Manhattan routes
                    PortInst pVddRow = lInstRowDec.findPortInstFromProto(layDec.findExport("vdd"));
                    PortInst pGndRow = lInstRowDec.findPortInstFromProto(layDec.findExport("gnd"));
                    // pVddRow is at (-800.0, -400.0). From (-55.0, -400.0), run M1 to (-800.0, -400.0)
                    NodeInst v12_vdd_row_m1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, -400.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_vdd_sa.getPortInst(0), v12_vdd_row_m1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_vdd_row_m1.getPortInst(0), pVddRow);

                    // pGndRow is at (-790.0, -420.0). From (-75.0, -420.0), run M1 to (-790.0, -420.0)
                    NodeInst v12_gnd_row_m1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, -420.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_gnd_wd.getPortInst(0), v12_gnd_row_m1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_gnd_row_m1.getPortInst(0), pGndRow);

                    // Col_Decoder power: Manhattan routes
                    PortInst pVddCol = lInstColDec.findPortInstFromProto(layDec.findExport("vdd"));
                    PortInst pGndCol = lInstColDec.findPortInstFromProto(layDec.findExport("gnd"));
                    ArcInst.makeInstance(m1, ep, v12_vdd_col.getPortInst(0), pVddCol);
                    ArcInst.makeInstance(m1, ep, v12_gnd_col.getPortInst(0), pGndCol);

                    // WD & SA power: Manhattan routes
                    PortInst pVddWd = lInstWd.findPortInstFromProto(layWd.findExport("vdd"));
                    // pVddWd is at (-45.0, -296.0). From (-55.0, -296.0), run M1 to (-45.0, -296.0)
                    NodeInst v12_vdd_wd = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, -296.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_vdd_sa.getPortInst(0), v12_vdd_wd.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_vdd_wd.getPortInst(0), pVddWd);

                    PortInst pGndWd = lInstWd.findPortInstFromProto(layWd.findExport("gnd"));
                    // pGndWd is at (5.0, -384.0). From (-75.0, -384.0), run M1 to (5.0, -384.0)
                    ArcInst.makeInstance(m1, ep, v12_gnd_wd.getPortInst(0), pGndWd);

                    PortInst pVddSa = lInstSa.findPortInstFromProto(laySa.findExport("vdd"));
                    // pVddSa is at (200.0, -288.0). From (-55.0, -288.0), run M1 to (200.0, -288.0)
                    ArcInst.makeInstance(m1, ep, v12_vdd_sa.getPortInst(0), pVddSa);

                    PortInst pGndSa = lInstSa.findPortInstFromProto(laySa.findExport("gnd"));
                    // pGndSa is at (200.0, -388.0). From (-75.0, -388.0), run M1 to (200.0, -388.0)
                    NodeInst v12_gnd_sa = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, -388.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_gnd_wd.getPortInst(0), v12_gnd_sa.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_gnd_sa.getPortInst(0), pGndSa);

                    // Col Inverters Power: horizontal rails at yColInvs + 21.5 and yColInvs - 23.0
                    for (int c = 0; c < 16; c++) {
                        PortInst pVddInv = lColInvs[c].findPortInstFromProto(layInv.findExport("vdd"));
                        PortInst pGndInv = lColInvs[c].findPortInstFromProto(layInv.findExport("gnd"));
                        NodeInst tapV = NodeInst.makeInstance(m1Pin, new Point2D.Double(c * 76.0 + 25.5, yColInvs + 21.5), 3.0, 3.0, lay);
                        NodeInst tapG = NodeInst.makeInstance(m1Pin, new Point2D.Double(c * 76.0 + 25.5, yColInvs - 23.0), 3.0, 3.0, lay);
                        ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), pVddInv);
                        ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), pGndInv);
                        if (c == 0) {
                            ArcInst.makeInstance(m1, ep, v12_vdd_inv.getPortInst(0), tapV.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_gnd_inv.getPortInst(0), tapG.getPortInst(0));
                        } else {
                            NodeInst prevTapV = NodeInst.makeInstance(m1Pin, new Point2D.Double((c - 1) * 76.0 + 25.5, yColInvs + 21.5), 3.0, 3.0, lay);
                            NodeInst prevTapG = NodeInst.makeInstance(m1Pin, new Point2D.Double((c - 1) * 76.0 + 25.5, yColInvs - 23.0), 3.0, 3.0, lay);
                            ArcInst.makeInstance(m1, ep, prevTapV.getPortInst(0), tapV.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, prevTapG.getPortInst(0), tapG.getPortInst(0));
                        }
                    }

                    // Upward Power: to Div8, Timing Gates, and ShiftReg
                    NodeInst v12_vdd_div8 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, yDiv8 + 60.0), 5.0, 5.0, lay);
                    NodeInst v12_vdd_tm   = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, yTiming + 70.0), 5.0, 5.0, lay);
                    NodeInst v12_vdd_sr   = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, ySreg + 60.0), 5.0, 5.0, lay);

                    ArcInst.makeInstance(m2, ep, pVddDat, v12_vdd_div8.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_vdd_div8.getPortInst(0), v12_vdd_tm.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_vdd_tm.getPortInst(0), v12_vdd_sr.getPortInst(0));

                    NodeInst v12_gnd_div8 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, yDiv8 - 80.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_tm   = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, yTiming - 70.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_sr   = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, ySreg - 80.0), 5.0, 5.0, lay);

                    ArcInst.makeInstance(m2, ep, pGndDat, v12_gnd_div8.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_div8.getPortInst(0), v12_gnd_tm.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_tm.getPortInst(0), v12_gnd_sr.getPortInst(0));

                    // Div8 & ShiftReg power taps
                    PortInst pVddDiv8 = lInstDiv8.findPortInstFromProto(layDiv8.findExport("vdd"));
                    PortInst pGndDiv8 = lInstDiv8.findPortInstFromProto(layDiv8.findExport("gnd"));
                    // pVddDiv8 at (-640 - 110, yDiv8 + 60) = (-750.0, yDiv8 + 60.0)
                    ArcInst.makeInstance(m1, ep, v12_vdd_div8.getPortInst(0), pVddDiv8);
                    // pGndDiv8 at (-640 - 130, yDiv8 - 80) = (-770.0, yDiv8 - 80.0)
                    ArcInst.makeInstance(m1, ep, v12_gnd_div8.getPortInst(0), pGndDiv8);

                    PortInst pVddSr = lInstSreg.findPortInstFromProto(laySreg.findExport("vdd"));
                    PortInst pGndSr = lInstSreg.findPortInstFromProto(laySreg.findExport("gnd"));
                    ArcInst.makeInstance(m1, ep, v12_vdd_sr.getPortInst(0), pVddSr);
                    ArcInst.makeInstance(m1, ep, v12_gnd_sr.getPortInst(0), pGndSr);

                    // Timing Gates Power: horizontal rails along yTiming + 70.0 and yTiming - 70.0
                    double[] xGatesVdd = {-409.5, -309.5, -209.5, -109.5, -28.0, 70.5, 152.0, 250.5};
                    for (int i = 0; i < xGatesVdd.length; i++) {
                        NodeInst tapV = NodeInst.makeInstance(m1Pin, new Point2D.Double(xGatesVdd[i], yTiming + 70.0), 3.0, 3.0, lay);
                        if (i == 0) ArcInst.makeInstance(m1, ep, v12_vdd_tm.getPortInst(0), tapV.getPortInst(0));
                        else {
                            NodeInst prevTapV = NodeInst.makeInstance(m1Pin, new Point2D.Double(xGatesVdd[i - 1], yTiming + 70.0), 3.0, 3.0, lay);
                            ArcInst.makeInstance(m1, ep, prevTapV.getPortInst(0), tapV.getPortInst(0));
                        }
                        if (xGatesVdd[i] == -409.5)      ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lInvD1.findPortInstFromProto(layInv.findExport("vdd")));
                        else if (xGatesVdd[i] == -309.5) ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lInvD2.findPortInstFromProto(layInv.findExport("vdd")));
                        else if (xGatesVdd[i] == -209.5) ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lInvD3.findPortInstFromProto(layInv.findExport("vdd")));
                        else if (xGatesVdd[i] == -109.5) ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lInvRw.findPortInstFromProto(layInv.findExport("vdd")));
                        else if (xGatesVdd[i] == -28.0)  ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lNandWe.findPortInstFromProto(layNand.findExport("vdd")));
                        else if (xGatesVdd[i] == 70.5)   ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lInvWe.findPortInstFromProto(layInv.findExport("vdd")));
                        else if (xGatesVdd[i] == 152.0)  ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lNandSae.findPortInstFromProto(layNand.findExport("vdd")));
                        else if (xGatesVdd[i] == 250.5)  ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lInvSae.findPortInstFromProto(layInv.findExport("vdd")));
                    }

                    double[] xGatesGnd = {-409.5, -309.5, -209.5, -109.5, -28.0, 70.5, 152.0, 250.5};
                    for (int i = 0; i < xGatesGnd.length; i++) {
                        NodeInst tapG = NodeInst.makeInstance(m1Pin, new Point2D.Double(xGatesGnd[i], yTiming - 70.0), 3.0, 3.0, lay);
                        if (i == 0) ArcInst.makeInstance(m1, ep, v12_gnd_tm.getPortInst(0), tapG.getPortInst(0));
                        else {
                            NodeInst prevTapG = NodeInst.makeInstance(m1Pin, new Point2D.Double(xGatesGnd[i - 1], yTiming - 70.0), 3.0, 3.0, lay);
                            ArcInst.makeInstance(m1, ep, prevTapG.getPortInst(0), tapG.getPortInst(0));
                        }
                        if (xGatesGnd[i] == -409.5)      ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lInvD1.findPortInstFromProto(layInv.findExport("gnd")));
                        else if (xGatesGnd[i] == -309.5) ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lInvD2.findPortInstFromProto(layInv.findExport("gnd")));
                        else if (xGatesGnd[i] == -209.5) ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lInvD3.findPortInstFromProto(layInv.findExport("gnd")));
                        else if (xGatesGnd[i] == -109.5) ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lInvRw.findPortInstFromProto(layInv.findExport("gnd")));
                        else if (xGatesGnd[i] == -28.0)  ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lNandWe.findPortInstFromProto(layNand.findExport("gnd")));
                        else if (xGatesGnd[i] == 70.5)   ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lInvWe.findPortInstFromProto(layInv.findExport("gnd")));
                        else if (xGatesGnd[i] == 152.0)  ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lNandSae.findPortInstFromProto(layNand.findExport("gnd")));
                        else if (xGatesGnd[i] == 250.5)  ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lInvSae.findPortInstFromProto(layInv.findExport("gnd")));
                    }

                    // ------------------------------------------
                    // ROUTING G: Global I/O Exports (7 total)
                    // ------------------------------------------
                    // 1. CLK: connects ShiftReg CLK and Div8 CLK along X = -685.0
                    PortInst pClkSreg = lInstSreg.findPortInstFromProto(laySreg.findExport("CLK"));
                    PortInst pClkDiv8 = lInstDiv8.findPortInstFromProto(layDiv8.findExport("CLK"));
                    NodeInst pinClkLay = NodeInst.makeInstance(m2Pin, new Point2D.Double(-685.0, 2105.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m2, ep, pinClkLay.getPortInst(0), pClkSreg);
                    ArcInst.makeInstance(m2, ep, pClkSreg, pClkDiv8);
                    Export.newInst(lay, pinClkLay.getPortInst(0), "CLK", ep);

                    // 2. ADDR_IN: ShiftReg ADDR_IN at (-560.0, ySreg + 10.0)
                    PortInst pAddrIn = lInstSreg.findPortInstFromProto(laySreg.findExport("ADDR_IN"));
                    Export.newInst(lay, pAddrIn, "ADDR_IN", ep);

                    // 3. R_W: pinRwLay at (-700.0, 1800.0)
                    Export.newInst(lay, pinRwLay.getPortInst(0), "R_W", ep);

                    // 4. DIN: connects write_driver.WD (Poly tap LEFT to -75.0)
                    PortInst pDin = lInstWd.findPortInstFromProto(layWd.findExport("WD"));
                    NodeInst pinDinLay = NodeInst.makeInstance(polyPin, new Point2D.Double(-75.0, yWd), 2.0, 2.0, lay);
                    ArcInst.makeInstance(poly, ep, pinDinLay.getPortInst(0), pDin);
                    Export.newInst(lay, pinDinLay.getPortInst(0), "DIN", ep);

                    // 5. DOUT: sense_amplifier.SO via Metal-2 to (213.5, ySa - 80.0)
                    PortInst pDout = lInstSa.findPortInstFromProto(laySa.findExport("SO"));
                    NodeInst v12_dout = NodeInst.makeInstance(via12Proto, new Point2D.Double(213.5, -335.0), 5.0, 5.0, lay);
                    NodeInst pinDoutLay = NodeInst.makeInstance(m2Pin, new Point2D.Double(213.5, -420.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, pDout, v12_dout.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_dout.getPortInst(0), pinDoutLay.getPortInst(0));
                    Export.newInst(lay, pinDoutLay.getPortInst(0), "DOUT", ep);

                    // 6. vdd
                    Export.newInst(lay, v12_vdd_col.getPortInst(0), "vdd", ep);

                    // 7. gnd
                    Export.newInst(lay, v12_gnd_col.getPortInst(0), "gnd", ep);

                    System.out.println("  SRAM_256_Macro{lay} constructed with all 7 exports.");

                    // ==========================================
                    // 3. VERIFICATION: DRC & NCC
                    // ==========================================
                    System.out.println("=== 3. Running DRC on SRAM_256_Macro{lay} ===");
                    DRC.resetDRCDates(true);
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, lay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println(">>> DRC Result: " + errLog.getNumErrors() + " errors, " + errLog.getNumWarnings() + " warnings <<<");

                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        System.out.println("DRC: " + ml.getMessageString());
                    }

                    System.out.println("=== 4. Running NCC LVS on SRAM_256_Macro ===");
                    NccOptions opts = new NccOptions();
                    opts.checkSizes = false;
                    NccResults res = Ncc.compare(sch, null, lay, null, opts);
                    System.out.println(">>> NCC Result: " + res + " <<<");
                    System.out.println("  Match: " + res.match());
                    System.out.println("  Exports Match: " + res.exportMatch());
                    System.out.println("  Topology Match: " + res.topologyMatch());

                    // Metrics
                    Rectangle2D bounds = lay.getBounds();
                    System.out.println("=== Macro Metrics ===");
                    System.out.println("Bounding Box: " + bounds);
                    System.out.println("Width:  " + bounds.getWidth() + " lambda");
                    System.out.println("Height: " + bounds.getHeight() + " lambda");
                    System.out.println("Area:   " + (bounds.getWidth() * bounds.getHeight()) + " lambda^2 (" + (bounds.getWidth() * bounds.getHeight() / 1e6) + " M lambda^2)");

                    // Only save if NCC passes
                    if (res.match()) {
                        System.out.println("=== 5. Saving to SRAM.jelib ===");
                        Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                        System.out.println("Library save status: SUCCESS");

                        System.out.println("=== 6. Exporting SPICE: SRAM_256_Macro.spi ===");
                        Spice.SpicePreferences sp = 
                            (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, sch, true, null);
                        sp.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                        sp.level = "3";
                        sp.doOutput(sch, null, "SRAM_256_Macro.spi");
                        File spiFile = new File("SRAM_256_Macro.spi");
                        System.out.println("SPICE deck exported successfully (" + spiFile.length() + " bytes).");
                    } else {
                        System.err.println("Verification FAILED! Library will not be overwritten.");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(8000);
    }
}
