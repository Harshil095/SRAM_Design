import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
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
import com.sun.electric.tool.user.IconParameters;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;

public class BuildFinalMacroCompacted {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildFinalMacroCompacted", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();
                    IconParameters ip = IconParameters.makeInstance(true);

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

                    // Instances in Schematic
                    NodeInst instData   = NodeInst.makeInstance(icData, new Point2D.Double(0.0, 0.0), 0, 0, sch);
                    NodeInst instRowDec = NodeInst.makeInstance(icDec,  new Point2D.Double(-200.0, 50.0), 0, 0, sch);
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

                    // Timing gates
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
                    PortInst pPreB = instData.findPortInstFromProto(icData.findExport("PRE_B"));
                    ArcInst.makeInstance(wire, ep, pExec, pPreB);
                    ArcInst.makeInstance(wire, ep, pExec, invD1.findPortInstFromProto(icInv.findExport("in")));

                    PortInst pD1 = invD1.findPortInstFromProto(icInv.findExport("out"));
                    ArcInst.makeInstance(wire, ep, pD1, invD2.findPortInstFromProto(icInv.findExport("in")));

                    PortInst pD2 = invD2.findPortInstFromProto(icInv.findExport("out"));
                    ArcInst.makeInstance(wire, ep, pD2, invD3.findPortInstFromProto(icInv.findExport("in")));
                    ArcInst.makeInstance(wire, ep, pD2, nandWe.findPortInstFromProto(icNand.findExport("A")));

                    PortInst pD3 = invD3.findPortInstFromProto(icInv.findExport("out"));
                    ArcInst.makeInstance(wire, ep, pD3, nandSae.findPortInstFromProto(icNand.findExport("A")));

                    NodeInst pinRw = NodeInst.makeInstance(offPage, new Point2D.Double(-350.0, -250.0), 0, 0, sch);
                    ArcInst.makeInstance(wire, ep, pinRw.getPortInst(0), nandWe.findPortInstFromProto(icNand.findExport("B")));
                    ArcInst.makeInstance(wire, ep, pinRw.getPortInst(0), invRw.findPortInstFromProto(icInv.findExport("in")));

                    PortInst pRwB = invRw.findPortInstFromProto(icInv.findExport("out"));
                    ArcInst.makeInstance(wire, ep, pRwB, nandSae.findPortInstFromProto(icNand.findExport("B")));

                    ArcInst.makeInstance(wire, ep, nandWe.findPortInstFromProto(icNand.findExport("out")), invWe.findPortInstFromProto(icInv.findExport("in")));
                    ArcInst.makeInstance(wire, ep, invWe.findPortInstFromProto(icInv.findExport("out")), instWd.findPortInstFromProto(icWd.findExport("WE")));

                    ArcInst.makeInstance(wire, ep, nandSae.findPortInstFromProto(icNand.findExport("out")), invSae.findPortInstFromProto(icInv.findExport("in")));
                    ArcInst.makeInstance(wire, ep, invSae.findPortInstFromProto(icInv.findExport("out")), instSa.findPortInstFromProto(icSa.findExport("SAE")));

                    // Global I/O
                    NodeInst pinClk = NodeInst.makeInstance(offPage, new Point2D.Double(-400.0, -50.0), 0, 0, sch);
                    ArcInst.makeInstance(wire, ep, pinClk.getPortInst(0), instSreg.findPortInstFromProto(icSreg.findExport("CLK")));
                    ArcInst.makeInstance(wire, ep, pinClk.getPortInst(0), instDiv8.findPortInstFromProto(icDiv8.findExport("CLK")));

                    NodeInst pinAddrIn = NodeInst.makeInstance(offPage, new Point2D.Double(-400.0, 50.0), 0, 0, sch);
                    ArcInst.makeInstance(wire, ep, pinAddrIn.getPortInst(0), instSreg.findPortInstFromProto(icSreg.findExport("ADDR_IN")));

                    NodeInst pinDin = NodeInst.makeInstance(offPage, new Point2D.Double(-100.0, -150.0), 0, 0, sch);
                    ArcInst.makeInstance(wire, ep, pinDin.getPortInst(0), instWd.findPortInstFromProto(icWd.findExport("WD")));

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

                    ArcProto m1 = lay.getTechnology().findArcProto("Metal-1");
                    ArcProto m2 = lay.getTechnology().findArcProto("Metal-2");
                    ArcProto m3 = lay.getTechnology().findArcProto("Metal-3");
                    ArcProto poly = lay.getTechnology().findArcProto("Polysilicon-1");

                    NodeProto via12Proto = lay.getTechnology().findNodeProto("Metal-1-Metal-2-Con");
                    NodeProto via23Proto = lay.getTechnology().findNodeProto("Metal-2-Metal-3-Con");
                    NodeProto conPolyM1  = lay.getTechnology().findNodeProto("Metal-1-Polysilicon-1-Con");
                    NodeProto m1Pin      = lay.getTechnology().findNodeProto("Metal-1-Pin");
                    NodeProto m2Pin      = lay.getTechnology().findNodeProto("Metal-2-Pin");
                    NodeProto m3Pin      = lay.getTechnology().findNodeProto("Metal-3-Pin");
                    NodeProto polyPin    = lay.getTechnology().findNodeProto("Polysilicon-1-Pin");

                    // Anchor coordinates: Compact, zero-overlap, wide corridors
                    double xRowDec = -640.0, yRowDec = 0.0;
                    double xColDec = -640.0, yColDec = -2250.0;
                    double xData = 0.0, yData = 0.0;
                    double xWd = 0.0, yWd = -560.0;
                    double xSa = 200.0, ySa = -560.0;
                    double xDiv8 = 0.0, yDiv8 = 1550.0;
                    double yTiming = 1950.0;
                    double xSreg = 0.0, ySreg = 2150.0;

                    NodeInst lInstData   = NodeInst.makeInstance(layData, new Point2D.Double(xData, yData), 0, 0, lay);
                    NodeInst lInstRowDec = NodeInst.makeInstance(layDec,  new Point2D.Double(xRowDec, yRowDec), 0, 0, lay);
                    NodeInst lInstColDec = NodeInst.makeInstance(layDec,  new Point2D.Double(xColDec, yColDec), 0, 0, lay);
                    NodeInst lInstWd     = NodeInst.makeInstance(layWd,   new Point2D.Double(xWd, yWd), 0, 0, lay);
                    NodeInst lInstSa     = NodeInst.makeInstance(laySa,   new Point2D.Double(xSa, ySa), 0, 0, lay);
                    NodeInst lInstDiv8   = NodeInst.makeInstance(layDiv8, new Point2D.Double(xDiv8, yDiv8), 0, 0, lay);
                    NodeInst lInstSreg   = NodeInst.makeInstance(laySreg, new Point2D.Double(xSreg, ySreg), 0, 0, lay);

                    // 16 inverters along Y = -720.0
                    double yColInvs = -720.0;
                    NodeInst[] lColInvs = new NodeInst[16];
                    for (int c = 0; c < 16; c++) {
                        lColInvs[c] = NodeInst.makeInstance(layInv, new Point2D.Double(c * 76.0 + 35.0, yColInvs), 0, 0, lay);
                    }

                    // Timing gates at Y = 1950.0
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

                        // Use 6.0x6.0 for both stacked vias to avoid 0.5 lambda Metal-2 notch step
                        NodeInst v12_left = NodeInst.makeInstance(via12Proto, new Point2D.Double(-100.0, y), 6.0, 6.0, lay);
                        NodeInst v23_left = NodeInst.makeInstance(via23Proto, new Point2D.Double(-100.0, y), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pDecOut, v12_left.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_left.getPortInst(0), v23_left.getPortInst(0));

                        NodeInst v23_right = NodeInst.makeInstance(via23Proto, new Point2D.Double(10.0, y), 6.0, 6.0, lay);
                        NodeInst v12_right = NodeInst.makeInstance(via12Proto, new Point2D.Double(10.0, y), 6.0, 6.0, lay);
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

                        NodeInst v12_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(-100.0, yOut), 6.0, 6.0, lay);
                        NodeInst v23_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(-100.0, yOut), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pDecOut, v12_out.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_out.getPortInst(0), v23_out.getPortInst(0));

                        double xSelTrack = c * 76.0 + 10.0;
                        if (c == 0) xSelTrack = -25.0;
                        if (c == 3) xSelTrack = c * 76.0 + 22.0;

                        NodeInst v23_m3_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(xSelTrack, yOut), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_out.getPortInst(0), v23_m3_bot.getPortInst(0));

                        NodeInst v12_inv_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(xSelTrack, yColInvs - 1.0), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, v12_inv_in.getPortInst(0), pInvIn);

                        if (c == 0) {
                            NodeInst v12_m2_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-25.0, yColInvs - 1.0), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_m3_bot.getPortInst(0), v12_m2_bot.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_m2_bot.getPortInst(0), v12_inv_in.getPortInst(0));
                        } else {
                            ArcInst.makeInstance(m2, ep, v23_m3_bot.getPortInst(0), v12_inv_in.getPortInst(0));
                        }

                        // Connect to COL_SEL[c] at Y = -200.0
                        NodeInst cPoly_sel = NodeInst.makeInstance(conPolyM1, new Point2D.Double(c * 76.0, -200.0), 5.0, 5.0, lay);
                        NodeInst v12_sel   = NodeInst.makeInstance(via12Proto, new Point2D.Double(c * 76.0, -200.0), 5.0, 5.0, lay);
                        ArcInst.makeInstance(poly, ep, pDatSel, cPoly_sel.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, cPoly_sel.getPortInst(0), v12_sel.getPortInst(0));

                        if (c == 0) {
                            NodeInst v12_sel_c0 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-25.0, -200.0), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_m3_bot.getPortInst(0), v12_sel_c0.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_sel_c0.getPortInst(0), v12_sel.getPortInst(0));
                        } else {
                            NodeInst v12_sel_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(xSelTrack, -200.0), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v12_inv_in.getPortInst(0), v12_sel_top.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_sel_top.getPortInst(0), v12_sel.getPortInst(0));
                        }

                        // Connect to COL_SEL_B[c] at Y = -158.0
                        double x = c * 76.0;
                        NodeInst cPoly_selb = NodeInst.makeInstance(conPolyM1, new Point2D.Double(x, -158.0), 5.0, 5.0, lay);
                        NodeInst v12_selb   = NodeInst.makeInstance(via12Proto, new Point2D.Double(x, -158.0), 5.0, 5.0, lay);
                        ArcInst.makeInstance(poly, ep, pDatSelB, cPoly_selb.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, cPoly_selb.getPortInst(0), v12_selb.getPortInst(0));

                        NodeInst v12_inv_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(c * 76.0 + 43.0, yColInvs - 1.5), 6.0, 6.0, lay);
                        NodeInst v23_inv_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(c * 76.0 + 43.0, yColInvs - 1.5), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pInvOut, v12_inv_out.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_inv_out.getPortInst(0), v23_inv_out.getPortInst(0));

                        if (c == 0) {
                            NodeInst v23_bridge = NodeInst.makeInstance(via23Proto, new Point2D.Double(-40.0, -158.0), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m2, ep, v12_selb.getPortInst(0), v23_bridge.getPortInst(0));
                            NodeInst jogLeft = NodeInst.makeInstance(m3Pin, new Point2D.Double(-120.0, -158.0), 3.0, 3.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_bridge.getPortInst(0), jogLeft.getPortInst(0));

                            NodeInst v23_b0_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(-120.0, yColInvs - 1.5), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, jogLeft.getPortInst(0), v23_b0_bot.getPortInst(0));
                            ArcInst.makeInstance(m3, ep, v23_b0_bot.getPortInst(0), v23_inv_out.getPortInst(0));
                        } else {
                            NodeInst v23_selb_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(x, -158.0), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m2, ep, v12_selb.getPortInst(0), v23_selb_top.getPortInst(0));

                            NodeInst v23_selb_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(x, yColInvs - 1.5), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_selb_top.getPortInst(0), v23_selb_bot.getPortInst(0));
                            ArcInst.makeInstance(m3, ep, v23_selb_bot.getPortInst(0), v23_inv_out.getPortInst(0));
                        }
                    }

                    // ------------------------------------------
                    // ROUTING C: Global Bitlines (Manhattan orthogonal routing)
                    // ------------------------------------------
                    PortInst lGbl   = lInstData.findPortInstFromProto(layData.findExport("GBL"));
                    PortInst lGblb  = lInstData.findPortInstFromProto(layData.findExport("GBLB"));
                    PortInst pWdBl  = lInstWd.findPortInstFromProto(layWd.findExport("BL"));
                    PortInst pWdBlb = lInstWd.findPortInstFromProto(layWd.findExport("BLB"));
                    PortInst pSaBl  = lInstSa.findPortInstFromProto(laySa.findExport("BL"));
                    PortInst pSaBlb = lInstSa.findPortInstFromProto(laySa.findExport("BLB"));

                    // GBL: starts at (-5.0, -225.0) on Metal-2
                    // Jog M2 horizontally to -12.0 at Y = -225.0, then M2 down to -485.0
                    NodeInst jGbl_h = NodeInst.makeInstance(m2Pin, new Point2D.Double(-12.0, -225.0), 3.0, 3.0, lay);
                    NodeInst v12_gbl_trunk = NodeInst.makeInstance(via12Proto, new Point2D.Double(-12.0, -485.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, lGbl, jGbl_h.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, jGbl_h.getPortInst(0), v12_gbl_trunk.getPortInst(0));

                    // M1 horizontal at Y = -485.0 to WD at X = 11.5 and SA at X = 155.0
                    NodeInst v12_gbl_wd = NodeInst.makeInstance(via12Proto, new Point2D.Double(11.5, -485.0), 5.0, 5.0, lay);
                    NodeInst v12_wd_bl = NodeInst.makeInstance(via12Proto, new Point2D.Double(11.5, -564.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_gbl_trunk.getPortInst(0), v12_gbl_wd.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gbl_wd.getPortInst(0), v12_wd_bl.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_wd_bl.getPortInst(0), pWdBl);

                    NodeInst v12_gbl_sa = NodeInst.makeInstance(via12Proto, new Point2D.Double(155.0, -485.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_gbl_wd.getPortInst(0), v12_gbl_sa.getPortInst(0));
                    NodeInst v12_sa_bl = NodeInst.makeInstance(via12Proto, new Point2D.Double(155.0, -578.0), 5.0, 5.0, lay);
                    NodeInst cPoly_sa_bl = NodeInst.makeInstance(conPolyM1, new Point2D.Double(155.0, -578.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_gbl_sa.getPortInst(0), v12_sa_bl.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sa_bl.getPortInst(0), cPoly_sa_bl.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_sa_bl.getPortInst(0), pSaBl);

                    // GBLB: starts at (35.0, -135.0) on Metal-2
                    // Jog M2 horizontally to 65.0 at Y = -135.0
                    NodeInst v23_gblb_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(65.0, -135.0), 6.0, 6.0, lay);
                    NodeInst jGblb_h = NodeInst.makeInstance(m2Pin, new Point2D.Double(65.0, -135.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m2, ep, lGblb, v23_gblb_top.getPortInst(0));

                    NodeInst v23_gblb_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(65.0, -495.0), 6.0, 6.0, lay);
                    NodeInst v12_gblb_trunk = NodeInst.makeInstance(via12Proto, new Point2D.Double(65.0, -495.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_gblb_top.getPortInst(0), v23_gblb_bot.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_gblb_bot.getPortInst(0), v12_gblb_trunk.getPortInst(0));

                    // M1 horizontal at Y = -495.0 to WD at X = 23.5 and SA at X = 240.0
                    NodeInst v12_gblb_wd = NodeInst.makeInstance(via12Proto, new Point2D.Double(23.5, -495.0), 5.0, 5.0, lay);
                    NodeInst v12_wd_blb = NodeInst.makeInstance(via12Proto, new Point2D.Double(23.5, -564.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_gblb_trunk.getPortInst(0), v12_gblb_wd.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gblb_wd.getPortInst(0), v12_wd_blb.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_wd_blb.getPortInst(0), pWdBlb);

                    NodeInst v12_gblb_sa = NodeInst.makeInstance(via12Proto, new Point2D.Double(240.0, -495.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_gblb_trunk.getPortInst(0), v12_gblb_sa.getPortInst(0));
                    NodeInst v12_sa_blb = NodeInst.makeInstance(via12Proto, new Point2D.Double(240.0, -578.0), 5.0, 5.0, lay);
                    NodeInst cPoly_sa_blb = NodeInst.makeInstance(conPolyM1, new Point2D.Double(240.0, -578.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_gblb_sa.getPortInst(0), v12_sa_blb.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sa_blb.getPortInst(0), cPoly_sa_blb.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_sa_blb.getPortInst(0), pSaBlb);

                    // ------------------------------------------
                    // ROUTING D: Address Bus (Strict Manhattan, External Routing Channel)
                    // ------------------------------------------
                    // Tracks run at X = -950 - b*12 on Metal-2
                    for (int b = 0; b < 8; b++) {
                        PortInst pQ = lInstSreg.findPortInstFromProto(laySreg.findExport("Q[" + b + "]"));
                        PortInst pA = (b < 4) ?
                            lInstRowDec.findPortInstFromProto(layDec.findExport("A[" + b + "]")) :
                            lInstColDec.findPortInstFromProto(layDec.findExport("A[" + (b - 4) + "]"));

                        double xQ = pQ.getCenter().getX();
                        double yQ = pQ.getCenter().getY();
                        double xTrack = -950.0 - b * 12.0;
                        double yJog   = 2800.0 + b * 12.0;
                        double xA     = pA.getCenter().getX();
                        double yA     = pA.getCenter().getY();

                        // Individual X track for Q inside ShiftReg channel to prevent M3-M3 collisions
                        double xQTrack = xQ;
                        if (b == 0) xQTrack = 545.0;
                        if (b == 2) xQTrack = 555.0;
                        if (b == 4) xQTrack = 565.0;
                        if (b == 6) xQTrack = 575.0;
                        if (b == 1) xQTrack = 1165.0;
                        if (b == 3) xQTrack = 1175.0;
                        if (b == 5) xQTrack = 1185.0;
                        if (b == 7) xQTrack = 1195.0;

                        NodeInst v12_q = NodeInst.makeInstance(via12Proto, new Point2D.Double(xQ, yQ), 5.0, 5.0, lay);
                        NodeInst v23_q = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yQ), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pQ, v12_q.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_q.getPortInst(0), v23_q.getPortInst(0));

                        // Horizontal M3 jog from xQ to xQTrack at Y = yQ
                        NodeInst v23_qTrk = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQTrack, yQ), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_q.getPortInst(0), v23_qTrk.getPortInst(0));

                        // Vertical M3 line from yQ up to yJog
                        NodeInst v23_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQTrack, yJog), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_qTrk.getPortInst(0), v23_top.getPortInst(0));

                        // Horizontal M3 line from xQTrack over to xTrack
                        NodeInst v23_track_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yJog), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_top.getPortInst(0), v23_track_top.getPortInst(0));

                        // Vertical M2 line down to yA
                        NodeInst v12_track_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(xTrack, yA), 5.0, 5.0, lay);
                        ArcInst.makeInstance(m2, ep, v23_track_top.getPortInst(0), v12_track_bot.getPortInst(0));

                        // Horizontal M1 line into Decoder input A
                        ArcInst.makeInstance(m1, ep, v12_track_bot.getPortInst(0), pA);
                    }

                    // ------------------------------------------
                    // ROUTING E: Timing Logic & Control Lines
                    // ------------------------------------------
                    PortInst lExec = lInstDiv8.findPortInstFromProto(layDiv8.findExport("EXECUTE"));
                    PortInst lPreB = lInstData.findPortInstFromProto(layData.findExport("PRE_B"));

                    // EXECUTE is at (1108.0, 1728.5)
                    NodeInst v12_exec = NodeInst.makeInstance(via12Proto, new Point2D.Double(1108.0, 1728.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, lExec, v12_exec.getPortInst(0));

                    NodeInst v23_exec_pin = NodeInst.makeInstance(via23Proto, new Point2D.Double(1108.0, 1728.5), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_exec.getPortInst(0), v23_exec_pin.getPortInst(0));

                    NodeInst v23_exec_low = NodeInst.makeInstance(via23Proto, new Point2D.Double(1108.0, 1445.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m2, ep, v23_exec_pin.getPortInst(0), v23_exec_low.getPortInst(0));

                    // Route M3 horizontally to PRE_B at X = -34.25
                    NodeInst v23_pre_tap = NodeInst.makeInstance(via23Proto, new Point2D.Double(-34.25, 1445.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_exec_low.getPortInst(0), v23_pre_tap.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_pre_tap.getPortInst(0), lPreB);

                    // Route M3 to invD1.in at Y = yTiming
                    NodeInst v23_exec_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(1108.0, yTiming - 50.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m2, ep, v23_exec_pin.getPortInst(0), v23_exec_top.getPortInst(0));

                    NodeInst v23_d1 = NodeInst.makeInstance(via23Proto, new Point2D.Double(-425.0, yTiming - 50.0), 6.0, 6.0, lay);
                    NodeInst v12_d1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-425.0, yTiming - 1.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_exec_top.getPortInst(0), v23_d1.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_d1.getPortInst(0), v12_d1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_d1.getPortInst(0), lInvD1.findPortInstFromProto(layInv.findExport("in")));

                    // D1 -> D2
                    PortInst pD1Out = lInvD1.findPortInstFromProto(layInv.findExport("out"));
                    PortInst pD2In  = lInvD2.findPortInstFromProto(layInv.findExport("in"));
                    NodeInst jD1_1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(-360.0, yTiming - 1.5), 3.0, 3.0, lay);
                    NodeInst jD1_2 = NodeInst.makeInstance(m1Pin, new Point2D.Double(-360.0, yTiming - 1.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, pD1Out, jD1_1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jD1_1.getPortInst(0), jD1_2.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jD1_2.getPortInst(0), pD2In);

                    // D2 -> D3 & nandWe.A
                    PortInst pD2Out = lInvD2.findPortInstFromProto(layInv.findExport("out"));
                    PortInst pD3In  = lInvD3.findPortInstFromProto(layInv.findExport("in"));
                    NodeInst jD2_1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(-260.0, yTiming - 1.5), 3.0, 3.0, lay);
                    NodeInst jD2_2 = NodeInst.makeInstance(m1Pin, new Point2D.Double(-260.0, yTiming - 1.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, pD2Out, jD2_1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jD2_1.getPortInst(0), jD2_2.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jD2_2.getPortInst(0), pD3In);

                    NodeInst v12_d2_tap = NodeInst.makeInstance(via12Proto, new Point2D.Double(-292.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_d2_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(-292.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v23_d2_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(-292.0, yTiming - 60.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pD2Out, v12_d2_tap.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_d2_tap.getPortInst(0), v23_d2_top.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_d2_top.getPortInst(0), v23_d2_bot.getPortInst(0));

                    NodeInst v23_we_a = NodeInst.makeInstance(via23Proto, new Point2D.Double(-56.0, yTiming - 60.0), 6.0, 6.0, lay);
                    NodeInst v12_we_a = NodeInst.makeInstance(via12Proto, new Point2D.Double(-56.0, yTiming - 52.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_d2_bot.getPortInst(0), v23_we_a.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_we_a.getPortInst(0), v12_we_a.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_we_a.getPortInst(0), lNandWe.findPortInstFromProto(layNand.findExport("A")));

                    // D3 -> nandSae.A
                    PortInst pD3Out = lInvD3.findPortInstFromProto(layInv.findExport("out"));
                    NodeInst v12_d3_tap = NodeInst.makeInstance(via12Proto, new Point2D.Double(-192.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_d3_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(-192.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v23_d3_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(-192.0, yTiming - 40.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pD3Out, v12_d3_tap.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_d3_tap.getPortInst(0), v23_d3_top.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_d3_top.getPortInst(0), v23_d3_bot.getPortInst(0));

                    NodeInst v23_sae_a = NodeInst.makeInstance(via23Proto, new Point2D.Double(124.0, yTiming - 40.0), 6.0, 6.0, lay);
                    NodeInst v12_sae_a = NodeInst.makeInstance(via12Proto, new Point2D.Double(124.0, yTiming - 52.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_d3_bot.getPortInst(0), v23_sae_a.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_sae_a.getPortInst(0), v12_sae_a.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sae_a.getPortInst(0), lNandSae.findPortInstFromProto(layNand.findExport("A")));

                    // R_W Pin & Inverter
                    NodeInst pinRwLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-950.0, yTiming + 20.0), 5.0, 5.0, lay);
                    Export.newInst(lay, pinRwLay.getPortInst(0), "R_W", ep);

                    NodeInst v12_rw = NodeInst.makeInstance(via12Proto, new Point2D.Double(-125.0, yTiming + 20.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pinRwLay.getPortInst(0), v12_rw.getPortInst(0));
                    NodeInst v12_rw_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(-125.0, yTiming - 1.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_rw.getPortInst(0), v12_rw_in.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_rw_in.getPortInst(0), lInvRw.findPortInstFromProto(layInv.findExport("in")));

                    NodeInst v23_rw_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(-125.0, yTiming + 20.0), 6.0, 6.0, lay);
                    NodeInst v23_rw_b   = NodeInst.makeInstance(via23Proto, new Point2D.Double(-56.0, yTiming + 20.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_rw.getPortInst(0), v23_rw_top.getPortInst(0));
                    ArcInst.makeInstance(m3, ep, v23_rw_top.getPortInst(0), v23_rw_b.getPortInst(0));

                    NodeInst v12_we_b = NodeInst.makeInstance(via12Proto, new Point2D.Double(-56.0, yTiming - 37.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v23_rw_b.getPortInst(0), v12_we_b.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_we_b.getPortInst(0), lNandWe.findPortInstFromProto(layNand.findExport("B")));

                    // R_W_b -> nandSae.B
                    PortInst pRwBOut = lInvRw.findPortInstFromProto(layInv.findExport("out"));
                    NodeInst v12_rwb_tap = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_rwb_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(-75.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v23_rwb_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(-75.0, yTiming - 80.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pRwBOut, v12_rwb_tap.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_rwb_tap.getPortInst(0), v23_rwb_top.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_rwb_top.getPortInst(0), v23_rwb_bot.getPortInst(0));

                    NodeInst v23_sae_b = NodeInst.makeInstance(via23Proto, new Point2D.Double(124.0, yTiming - 80.0), 6.0, 6.0, lay);
                    NodeInst v12_sae_b = NodeInst.makeInstance(via12Proto, new Point2D.Double(124.0, yTiming - 37.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_rwb_bot.getPortInst(0), v23_sae_b.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_sae_b.getPortInst(0), v12_sae_b.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sae_b.getPortInst(0), lNandSae.findPortInstFromProto(layNand.findExport("B")));

                    // nandWe -> invWe -> WD.WE
                    PortInst pWeNandOut = lNandWe.findPortInstFromProto(layNand.findExport("out"));
                    PortInst pWeInvIn   = lInvWe.findPortInstFromProto(layInv.findExport("in"));
                    PortInst pWeInvOut  = lInvWe.findPortInstFromProto(layInv.findExport("out"));

                    NodeInst jWe1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(35.0, yTiming - 4.0), 3.0, 3.0, lay);
                    NodeInst jWe2 = NodeInst.makeInstance(m1Pin, new Point2D.Double(35.0, yTiming - 1.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, pWeNandOut, jWe1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jWe1.getPortInst(0), jWe2.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jWe2.getPortInst(0), pWeInvIn);

                    NodeInst v12_we_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(105.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v12_wd_we  = NodeInst.makeInstance(via12Proto, new Point2D.Double(105.0, yWd - 8.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pWeInvOut, v12_we_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_we_out.getPortInst(0), v12_wd_we.getPortInst(0));

                    PortInst pWdWe = lInstWd.findPortInstFromProto(layWd.findExport("WE"));
                    ArcInst.makeInstance(m1, ep, v12_wd_we.getPortInst(0), pWdWe);

                    // nandSae -> invSae -> SA.SAE
                    PortInst pSaeNandOut = lNandSae.findPortInstFromProto(layNand.findExport("out"));
                    PortInst pSaeInvIn   = lInvSae.findPortInstFromProto(layInv.findExport("in"));
                    PortInst pSaeInvOut  = lInvSae.findPortInstFromProto(layInv.findExport("out"));

                    NodeInst jSae1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(215.0, yTiming - 4.0), 3.0, 3.0, lay);
                    NodeInst jSae2 = NodeInst.makeInstance(m1Pin, new Point2D.Double(215.0, yTiming - 1.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, pSaeNandOut, jSae1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jSae1.getPortInst(0), jSae2.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jSae2.getPortInst(0), pSaeInvIn);

                    NodeInst v12_sae_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(285.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v12_sa_sae  = NodeInst.makeInstance(via12Proto, new Point2D.Double(285.0, ySa - 38.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pSaeInvOut, v12_sae_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_sae_out.getPortInst(0), v12_sa_sae.getPortInst(0));

                    PortInst pSaSae = lInstSa.findPortInstFromProto(laySa.findExport("SAE"));
                    ArcInst.makeInstance(m1, ep, v12_sa_sae.getPortInst(0), pSaSae);

                    // ------------------------------------------
                    // ROUTING F: Global I/O (CLK, ADDR_IN, DIN, DOUT)
                    // ------------------------------------------
                    // CLK
                    NodeInst pinClkLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-950.0, yDiv8 - 30.0), 5.0, 5.0, lay);
                    Export.newInst(lay, pinClkLay.getPortInst(0), "CLK", ep);

                    PortInst pDiv8Clk = lInstDiv8.findPortInstFromProto(layDiv8.findExport("CLK"));
                    PortInst pSregClk = lInstSreg.findPortInstFromProto(laySreg.findExport("CLK"));

                    NodeInst v12_clk_trunk = NodeInst.makeInstance(via12Proto, new Point2D.Double(-70.0, yDiv8 - 30.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pinClkLay.getPortInst(0), v12_clk_trunk.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_clk_trunk.getPortInst(0), pDiv8Clk);

                    NodeInst v12_sreg_clk = NodeInst.makeInstance(via12Proto, new Point2D.Double(-70.0, ySreg - 1.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_clk_trunk.getPortInst(0), v12_sreg_clk.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sreg_clk.getPortInst(0), pSregClk);

                    // ADDR_IN
                    NodeInst pinAddrLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-950.0, ySreg + 10.0), 5.0, 5.0, lay);
                    Export.newInst(lay, pinAddrLay.getPortInst(0), "ADDR_IN", ep);

                    PortInst pAddrIn = lInstSreg.findPortInstFromProto(laySreg.findExport("ADDR_IN"));
                    NodeInst v12_addr_1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-85.0, ySreg + 10.0), 5.0, 5.0, lay);
                    NodeInst v23_addr_1 = NodeInst.makeInstance(via23Proto, new Point2D.Double(-85.0, ySreg + 10.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pinAddrLay.getPortInst(0), v12_addr_1.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_addr_1.getPortInst(0), v23_addr_1.getPortInst(0));

                    NodeInst v23_addr_2 = NodeInst.makeInstance(via23Proto, new Point2D.Double(80.0, ySreg + 10.0), 6.0, 6.0, lay);
                    NodeInst v12_addr_2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(80.0, ySreg + 10.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_addr_1.getPortInst(0), v23_addr_2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_addr_2.getPortInst(0), v12_addr_2.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_addr_2.getPortInst(0), pAddrIn);

                    // DIN
                    NodeInst pinDinLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-950.0, yWd), 5.0, 5.0, lay);
                    Export.newInst(lay, pinDinLay.getPortInst(0), "DIN", ep);
                    PortInst pWdDin = lInstWd.findPortInstFromProto(layWd.findExport("WD"));
                    ArcInst.makeInstance(m1, ep, pinDinLay.getPortInst(0), pWdDin);

                    // DOUT
                    PortInst pSaDout = lInstSa.findPortInstFromProto(laySa.findExport("SO"));
                    NodeInst pinDoutLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(1220.0, ySa + 5.0), 5.0, 5.0, lay);
                    Export.newInst(lay, pinDoutLay.getPortInst(0), "DOUT", ep);
                    ArcInst.makeInstance(m1, ep, pSaDout, pinDoutLay.getPortInst(0));

                    // ------------------------------------------
                    // ROUTING G: Power & Ground Distribution (Strict Manhattan)
                    // ------------------------------------------
                    PortInst pVddDat = lInstData.findPortInstFromProto(layData.findExport("vdd"));
                    PortInst pGndDat = lInstData.findPortInstFromProto(layData.findExport("gnd"));

                    // Downward Power:
                    // Align vias to EXACT Y coordinates of Row_Decoder and Col_Decoder
                    NodeInst v12_vdd_row = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, yRowDec - 400.0), 5.0, 5.0, lay);
                    NodeInst v12_vdd_sa  = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, ySa + 52.0), 5.0, 5.0, lay);
                    NodeInst v12_vdd_inv = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, yColInvs + 21.5), 5.0, 5.0, lay);
                    NodeInst v12_vdd_col = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, yColDec - 400.0), 5.0, 5.0, lay);

                    ArcInst.makeInstance(m2, ep, pVddDat, v12_vdd_row.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_vdd_row.getPortInst(0), v12_vdd_sa.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_vdd_sa.getPortInst(0), v12_vdd_inv.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_vdd_inv.getPortInst(0), v12_vdd_col.getPortInst(0));

                    NodeInst v12_gnd_row = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, yRowDec - 420.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_wd  = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, yWd - 44.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_inv = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, yColInvs - 23.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_col = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, yColDec - 420.0), 5.0, 5.0, lay);

                    ArcInst.makeInstance(m2, ep, pGndDat, v12_gnd_row.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_row.getPortInst(0), v12_gnd_wd.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_wd.getPortInst(0), v12_gnd_inv.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_inv.getPortInst(0), v12_gnd_col.getPortInst(0));

                    // Row_Decoder power (Strictly horizontal on Metal-1)
                    PortInst pVddRow = lInstRowDec.findPortInstFromProto(layDec.findExport("vdd"));
                    PortInst pGndRow = lInstRowDec.findPortInstFromProto(layDec.findExport("gnd"));
                    ArcInst.makeInstance(m1, ep, v12_vdd_row.getPortInst(0), pVddRow);
                    ArcInst.makeInstance(m1, ep, v12_gnd_row.getPortInst(0), pGndRow);

                    // Col_Decoder power (Strictly horizontal on Metal-1)
                    PortInst pVddCol = lInstColDec.findPortInstFromProto(layDec.findExport("vdd"));
                    PortInst pGndCol = lInstColDec.findPortInstFromProto(layDec.findExport("gnd"));
                    ArcInst.makeInstance(m1, ep, v12_vdd_col.getPortInst(0), pVddCol);
                    ArcInst.makeInstance(m1, ep, v12_gnd_col.getPortInst(0), pGndCol);

                    // WD & SA power (Strict Manhattan)
                    PortInst pVddWd = lInstWd.findPortInstFromProto(layWd.findExport("vdd"));
                    PortInst pGndWd = lInstWd.findPortInstFromProto(layWd.findExport("gnd"));
                    PortInst pVddSa = lInstSa.findPortInstFromProto(laySa.findExport("vdd"));
                    PortInst pGndSa = lInstSa.findPortInstFromProto(laySa.findExport("gnd"));

                    // pVddSa is at (200.0, ySa + 52.0). v12_vdd_sa is at (-55.0, ySa + 52.0) -> perfectly horizontal!
                    ArcInst.makeInstance(m1, ep, v12_vdd_sa.getPortInst(0), pVddSa);

                    // pVddWd is at (-45.0, yWd + 44.0).
                    // Jog horizontally to -45.0 at Y = ySa + 52.0, then down to yWd + 44.0
                    NodeInst jogVddWd1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(-45.0, ySa + 52.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_vdd_sa.getPortInst(0), jogVddWd1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jogVddWd1.getPortInst(0), pVddWd);

                    // pGndWd is at (5.0, yWd - 44.0). v12_gnd_wd is at (-75.0, yWd - 44.0) -> perfectly horizontal!
                    ArcInst.makeInstance(m1, ep, v12_gnd_wd.getPortInst(0), pGndWd);

                    // pGndSa is at (200.0, ySa - 48.0).
                    // Jog horizontally to 200.0 at Y = yWd - 44.0, then down to ySa - 48.0
                    NodeInst jogGndSa1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(200.0, yWd - 44.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_gnd_wd.getPortInst(0), jogGndSa1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jogGndSa1.getPortInst(0), pGndSa);

                    // Upward Power:
                    NodeInst v12_vdd_div8 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, yDiv8 + 60.0), 5.0, 5.0, lay);
                    NodeInst v12_vdd_time = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, yTiming + 21.5), 5.0, 5.0, lay);
                    NodeInst v12_vdd_sreg = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, ySreg + 60.0), 5.0, 5.0, lay);

                    ArcInst.makeInstance(m2, ep, pVddDat, v12_vdd_div8.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_vdd_div8.getPortInst(0), v12_vdd_time.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_vdd_time.getPortInst(0), v12_vdd_sreg.getPortInst(0));

                    NodeInst v12_gnd_div8 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, yDiv8 - 80.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_time = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, yTiming - 23.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_sreg = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, ySreg - 80.0), 5.0, 5.0, lay);

                    ArcInst.makeInstance(m2, ep, pGndDat, v12_gnd_div8.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_div8.getPortInst(0), v12_gnd_time.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_time.getPortInst(0), v12_gnd_sreg.getPortInst(0));

                    // Div8 power (horizontal M1)
                    PortInst pVddDiv8 = lInstDiv8.findPortInstFromProto(layDiv8.findExport("vdd"));
                    PortInst pGndDiv8 = lInstDiv8.findPortInstFromProto(layDiv8.findExport("gnd"));
                    ArcInst.makeInstance(m1, ep, v12_vdd_div8.getPortInst(0), pVddDiv8);
                    ArcInst.makeInstance(m1, ep, v12_gnd_div8.getPortInst(0), pGndDiv8);

                    // ShiftReg power (horizontal M1)
                    PortInst pVddSreg = lInstSreg.findPortInstFromProto(laySreg.findExport("vdd"));
                    PortInst pGndSreg = lInstSreg.findPortInstFromProto(laySreg.findExport("gnd"));
                    ArcInst.makeInstance(m1, ep, v12_vdd_sreg.getPortInst(0), pVddSreg);
                    ArcInst.makeInstance(m1, ep, v12_gnd_sreg.getPortInst(0), pGndSreg);

                    // Export top-level vdd and gnd
                    Export.newInst(lay, pVddDat, "vdd", ep);
                    Export.newInst(lay, pGndDat, "gnd", ep);

                    System.out.println("  SRAM_256_Macro{lay} constructed with all 7 exports.");

                    // ==========================================
                    // 3. DRC VERIFICATION
                    // ==========================================
                    System.out.println("=== 3. Running DRC on SRAM_256_Macro{lay} ===");
                    DRC.resetDRCDates(true);
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, lay, null, null, null, null, dp, com.sun.electric.database.geometry.GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);

                    int drcErrors = errLog.getNumErrors();
                    int drcWarnings = errLog.getNumWarnings();
                    System.out.println(">>> DRC Result: " + drcErrors + " errors, " + drcWarnings + " warnings <<<");

                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog msg = it.next();
                        System.out.println("DRC: " + msg.getMessageString());
                    }

                    // ==========================================
                    // 4. NCC LVS VERIFICATION
                    // ==========================================
                    System.out.println("=== 4. Running NCC LVS on SRAM_256_Macro ===");
                    NccOptions opt = new NccOptions();
                    opt.checkSizes = false;
                    NccResults nccRes = Ncc.compare(sch, null, lay, null, opt);
                    System.out.println(">>> NCC Result: " + nccRes + " <<<");
                    System.out.println("  Match: " + nccRes.match());
                    System.out.println("  Exports Match: " + nccRes.exportMatch());
                    System.out.println("  Topology Match: " + nccRes.topologyMatch());

                    // Metrics
                    ERectangle bbox = lay.getBounds();
                    System.out.println("=== Macro Metrics ===");
                    System.out.println("Bounding Box: " + bbox);
                    System.out.println("Width:  " + bbox.getWidth() + " lambda");
                    System.out.println("Height: " + bbox.getHeight() + " lambda");
                    System.out.println("Area:   " + (bbox.getWidth() * bbox.getHeight()) + " lambda^2 (" + (bbox.getWidth() * bbox.getHeight() / 1e6) + " M lambda^2)");

                    if (nccRes.match()) {
                        System.out.println("Verification PASSED! Saving library...");
                        Output.saveJelib("SRAM.jelib", lib);
                        System.out.println("SRAM.jelib saved successfully!");
                    } else {
                        System.out.println("Verification FAILED! Library will not be overwritten.");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(6000);
    }
}
