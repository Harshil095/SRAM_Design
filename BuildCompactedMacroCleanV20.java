import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.simulation.SimulationTool;
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
import com.sun.electric.tool.user.IconParameters;
import com.sun.electric.tool.user.User;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;

public class BuildCompactedMacroCleanV20 {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildCompactedMacroCleanV20", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
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

                    // Instantiations in Schematic
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

                    // Safe coordinates: verified no cell overlap, clear routing channels
                    double xRowDec = -640.0, yRowDec = 0.0;
                    double xColDec = -640.0, yColDec = -2250.0;
                    double xData = 0.0, yData = 0.0;
                    double xWd = 0.0, yWd = -560.0;
                    double xSa = 200.0, ySa = -560.0;
                    double xDiv8 = -640.0, yDiv8 = 1550.0;
                    double yTiming = 1950.0;
                    double xSreg = -640.0, ySreg = 2200.0;

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

                        // 6x6 on both stacked vias to prevent 0.5 lambda Metal-2 notch
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

                        // Connect to COL_SEL[c] at Y = -245.0 (below Col_Mux_Bank_16 to clear tgate active diffusions)
                        double ySelBot = -245.0;
                        NodeInst cPoly_sel = NodeInst.makeInstance(conPolyM1, new Point2D.Double(c * 76.0, ySelBot), 5.0, 5.0, lay);
                        NodeInst v12_sel   = NodeInst.makeInstance(via12Proto, new Point2D.Double(c * 76.0, ySelBot), 5.0, 5.0, lay);
                        ArcInst.makeInstance(poly, ep, pDatSel, cPoly_sel.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, cPoly_sel.getPortInst(0), v12_sel.getPortInst(0));

                        if (c == 0) {
                            NodeInst v12_sel_c0 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-25.0, ySelBot), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_m3_bot.getPortInst(0), v12_sel_c0.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_sel_c0.getPortInst(0), v12_sel.getPortInst(0));
                        } else {
                            NodeInst v12_sel_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(xSelTrack, ySelBot), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v12_inv_in.getPortInst(0), v12_sel_top.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_sel_top.getPortInst(0), v12_sel.getPortInst(0));
                        }

                        // Connect to COL_SEL_B[c] at Y = -110.0 (above Col_Mux_Bank_16 in open channel)
                        // Concentric stacked via12 (5x5) and via23 (6x6) at X = x = c * 76.0 (0 notch error)
                        double x = c * 76.0;
                        double ySelBTop = -110.0;
                        NodeInst cPoly_selb = NodeInst.makeInstance(conPolyM1, new Point2D.Double(x, ySelBTop), 5.0, 5.0, lay);
                        NodeInst v12_selb   = NodeInst.makeInstance(via12Proto, new Point2D.Double(x, ySelBTop), 5.0, 5.0, lay);
                        ArcInst.makeInstance(poly, ep, pDatSelB, cPoly_selb.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, cPoly_selb.getPortInst(0), v12_selb.getPortInst(0));

                        NodeInst v12_inv_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(c * 76.0 + 43.0, yColInvs - 1.5), 5.0, 5.0, lay);
                        NodeInst v23_inv_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(c * 76.0 + 43.0, yColInvs - 1.5), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pInvOut, v12_inv_out.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_inv_out.getPortInst(0), v23_inv_out.getPortInst(0));

                        if (c == 0) {
                            // Metal-1 horizontal jog from (0.0, -110.0) to (-85.0, -110.0) to avoid shorting with M2 VDD rail at -55.0
                            NodeInst v12_selb_left = NodeInst.makeInstance(via12Proto, new Point2D.Double(-85.0, ySelBTop), 5.0, 5.0, lay);
                            NodeInst v23_selb_left = NodeInst.makeInstance(via23Proto, new Point2D.Double(-85.0, ySelBTop), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m1, ep, v12_selb.getPortInst(0), v12_selb_left.getPortInst(0));
                            ArcInst.makeInstance(m2, ep, v12_selb_left.getPortInst(0), v23_selb_left.getPortInst(0));

                            NodeInst v23_selb_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(-85.0, yColInvs - 1.5), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_selb_left.getPortInst(0), v23_selb_bot.getPortInst(0));
                            ArcInst.makeInstance(m3, ep, v23_selb_bot.getPortInst(0), v23_inv_out.getPortInst(0));
                        } else {
                            // Direct concentric stack at (x, ySelBTop) with 0 notch, running straight down on Metal-3
                            NodeInst v23_selb_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(x, ySelBTop), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m2, ep, v12_selb.getPortInst(0), v23_selb_top.getPortInst(0));

                            NodeInst v23_selb_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(x, yColInvs - 1.5), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_selb_top.getPortInst(0), v23_selb_bot.getPortInst(0));
                            ArcInst.makeInstance(m3, ep, v23_selb_bot.getPortInst(0), v23_inv_out.getPortInst(0));
                        }
                    }

                    // ROUTING C: Global Bitlines (Orthogonal Manhattan)
                    // ------------------------------------------
                    PortInst lGbl   = lInstData.findPortInstFromProto(layData.findExport("GBL"));
                    PortInst lGblb  = lInstData.findPortInstFromProto(layData.findExport("GBLB"));
                    PortInst pWdBl  = lInstWd.findPortInstFromProto(layWd.findExport("BL"));
                    PortInst pWdBlb = lInstWd.findPortInstFromProto(layWd.findExport("BLB"));
                    PortInst pSaBl  = lInstSa.findPortInstFromProto(laySa.findExport("BL"));
                    PortInst pSaBlb = lInstSa.findPortInstFromProto(laySa.findExport("BLB"));

                    // GBL: Datapath at (-5.0, -225.0)
                    NodeInst jGbl_h = NodeInst.makeInstance(m2Pin, new Point2D.Double(-12.0, -225.0), 3.0, 3.0, lay);
                    NodeInst v12_gbl_trunk = NodeInst.makeInstance(via12Proto, new Point2D.Double(-12.0, -485.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, lGbl, jGbl_h.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, jGbl_h.getPortInst(0), v12_gbl_trunk.getPortInst(0));

                    NodeInst v12_gbl_wd = NodeInst.makeInstance(via12Proto, new Point2D.Double(11.5, -485.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_gbl_trunk.getPortInst(0), v12_gbl_wd.getPortInst(0));
                    NodeInst v12_wd_bl = NodeInst.makeInstance(via12Proto, new Point2D.Double(11.5, -564.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_gbl_wd.getPortInst(0), v12_wd_bl.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_wd_bl.getPortInst(0), pWdBl);

                    NodeInst v12_gbl_sa = NodeInst.makeInstance(via12Proto, new Point2D.Double(155.0, -485.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_gbl_wd.getPortInst(0), v12_gbl_sa.getPortInst(0));
                    NodeInst v12_sa_bl = NodeInst.makeInstance(via12Proto, new Point2D.Double(155.0, -578.0), 5.0, 5.0, lay);
                    NodeInst cPoly_sa_bl = NodeInst.makeInstance(conPolyM1, new Point2D.Double(155.0, -578.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_gbl_sa.getPortInst(0), v12_sa_bl.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sa_bl.getPortInst(0), cPoly_sa_bl.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_sa_bl.getPortInst(0), pSaBl);

                    // GBLB: Datapath at (35.0, -135.0)
                    NodeInst v23_gblb_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(65.0, -135.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m2, ep, lGblb, v23_gblb_top.getPortInst(0));

                    NodeInst v23_gblb_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(65.0, -495.0), 6.0, 6.0, lay);
                    NodeInst v12_gblb_trunk = NodeInst.makeInstance(via12Proto, new Point2D.Double(65.0, -495.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_gblb_top.getPortInst(0), v23_gblb_bot.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_gblb_bot.getPortInst(0), v12_gblb_trunk.getPortInst(0));

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
                    // ROUTING D: Address Bus (Strict Layer-Separated Manhattan)
                    // ------------------------------------------
                    for (int b = 0; b < 8; b++) {
                        PortInst pQ = lInstSreg.findPortInstFromProto(laySreg.findExport("Q[" + b + "]"));
                        PortInst pA;
                        double xTrack;
                        if (b < 4) {
                            pA = lInstRowDec.findPortInstFromProto(layDec.findExport("A[" + b + "]"));
                            xTrack = -950.0 - b * 12.0;
                        } else {
                            pA = lInstColDec.findPortInstFromProto(layDec.findExport("A[" + (b - 4) + "]"));
                            xTrack = -1000.0 - (b - 4) * 12.0;
                        }

                        double xQ = pQ.getCenter().getX();
                        double yQ = pQ.getCenter().getY();
                        double xEastTrack = 560.0 + b * 12.0;
                        double yTopJog    = 2820.0 + b * 12.0;
                        double xA         = pA.getCenter().getX();
                        double yA         = pA.getCenter().getY();

                        NodeInst v12_q = NodeInst.makeInstance(via12Proto, new Point2D.Double(xQ, yQ), 5.0, 5.0, lay);
                        NodeInst v23_q = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yQ), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pQ, v12_q.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_q.getPortInst(0), v23_q.getPortInst(0));

                        if (b % 2 == 0) {
                            // Even bit (Col 0, X = -75.0):
                            // Drop M2 South by 20 lambda to clear odd bit at X = 545.0, then M3 East to xEastTrack
                            NodeInst v23_q_drop = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yQ - 20.0), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_q.getPortInst(0), v23_q_drop.getPortInst(0));

                            NodeInst v23_east_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(xEastTrack, yQ - 20.0), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_q_drop.getPortInst(0), v23_east_bot.getPortInst(0));

                            NodeInst v23_east_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xEastTrack, yTopJog), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_east_bot.getPortInst(0), v23_east_top.getPortInst(0));

                            NodeInst v23_track_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yTopJog), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_east_top.getPortInst(0), v23_track_top.getPortInst(0));

                            NodeInst v12_track_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(xTrack, yA), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_track_top.getPortInst(0), v12_track_bot.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_track_bot.getPortInst(0), pA);
                        } else {
                            // Odd bit (Col 1, X = 545.0):
                            // Run M3 directly East to xEastTrack
                            NodeInst v23_east_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(xEastTrack, yQ), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_q.getPortInst(0), v23_east_bot.getPortInst(0));

                            NodeInst v23_east_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xEastTrack, yTopJog), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_east_bot.getPortInst(0), v23_east_top.getPortInst(0));

                            NodeInst v23_track_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yTopJog), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_east_top.getPortInst(0), v23_track_top.getPortInst(0));

                            NodeInst v12_track_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(xTrack, yA), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_track_top.getPortInst(0), v12_track_bot.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_track_bot.getPortInst(0), pA);
                        }
                    }
// ------------------------------------------
                    // ROUTING E: Timing Logic & Control Lines
                    // ------------------------------------------
                    PortInst pDiv8Exec = lInstDiv8.findPortInstFromProto(layDiv8.findExport("EXECUTE"));
                    PortInst lPreB     = lInstData.findPortInstFromProto(layData.findExport("PRE_B"));
                    double xExec = pDiv8Exec.getCenter().getX();
                    double yExec = pDiv8Exec.getCenter().getY();

                    // EXECUTE: Port is at (468.0, 1728.5) on Metal-1.
                    NodeInst v12_exec = NodeInst.makeInstance(via12Proto, new Point2D.Double(xExec, yExec), 5.0, 5.0, lay);
                    NodeInst v23_exec = NodeInst.makeInstance(via23Proto, new Point2D.Double(xExec, 1870.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pDiv8Exec, v12_exec.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_exec.getPortInst(0), v23_exec.getPortInst(0));

                    // D1 branch on Metal-3 at Y = 1870.0 (above Div8_Counter)
                    NodeInst v23_d1_jog = NodeInst.makeInstance(via23Proto, new Point2D.Double(-425.0, 1870.0), 6.0, 6.0, lay);
                    NodeInst v12_d1_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(-425.0, yTiming - 1.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_exec.getPortInst(0), v23_d1_jog.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_d1_jog.getPortInst(0), v12_d1_top.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_d1_top.getPortInst(0), lInvD1.findPortInstFromProto(layInv.findExport("in")));

                    // PRE_B branch on Metal-3 East to 560.0, South on M2 to 1420.0, West on M3 to PRE_B tap at (-34.25, 1420.0)
                    NodeInst v23_exec_east = NodeInst.makeInstance(via23Proto, new Point2D.Double(560.0, 1870.0), 6.0, 6.0, lay);
                    NodeInst v23_exec_low  = NodeInst.makeInstance(via23Proto, new Point2D.Double(560.0, 1420.0), 6.0, 6.0, lay);
                    NodeInst v23_pre_tap   = NodeInst.makeInstance(via23Proto, new Point2D.Double(-34.25, 1420.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_exec.getPortInst(0), v23_exec_east.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_exec_east.getPortInst(0), v23_exec_low.getPortInst(0));
                    ArcInst.makeInstance(m3, ep, v23_exec_low.getPortInst(0), v23_pre_tap.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_pre_tap.getPortInst(0), lPreB);

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

                    // D2 tap -> nandWe.A at Y = 1915.0 on Metal-3
                    NodeInst v12_d2_tap = NodeInst.makeInstance(via12Proto, new Point2D.Double(-292.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_d2     = NodeInst.makeInstance(via23Proto, new Point2D.Double(-292.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v23_d2_m3  = NodeInst.makeInstance(via23Proto, new Point2D.Double(-292.0, 1915.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pD2Out, v12_d2_tap.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_d2_tap.getPortInst(0), v23_d2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_d2.getPortInst(0), v23_d2_m3.getPortInst(0));

                    NodeInst v23_we_a = NodeInst.makeInstance(via23Proto, new Point2D.Double(-56.0, 1915.0), 6.0, 6.0, lay);
                    NodeInst v12_we_a = NodeInst.makeInstance(via12Proto, new Point2D.Double(-56.0, 1897.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_d2_m3.getPortInst(0), v23_we_a.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_we_a.getPortInst(0), v12_we_a.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_we_a.getPortInst(0), lNandWe.findPortInstFromProto(layNand.findExport("A")));

                    // D3 tap -> nandSae.A at Y = 1930.0 on Metal-3
                    PortInst pD3Out = lInvD3.findPortInstFromProto(layInv.findExport("out"));
                    NodeInst v12_d3_tap = NodeInst.makeInstance(via12Proto, new Point2D.Double(-192.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_d3     = NodeInst.makeInstance(via23Proto, new Point2D.Double(-192.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v23_d3_m3  = NodeInst.makeInstance(via23Proto, new Point2D.Double(-192.0, 1930.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pD3Out, v12_d3_tap.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_d3_tap.getPortInst(0), v23_d3.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_d3.getPortInst(0), v23_d3_m3.getPortInst(0));

                    NodeInst v23_sae_a = NodeInst.makeInstance(via23Proto, new Point2D.Double(124.0, 1930.0), 6.0, 6.0, lay);
                    NodeInst v12_sae_a = NodeInst.makeInstance(via12Proto, new Point2D.Double(124.0, 1897.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_d3_m3.getPortInst(0), v23_sae_a.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_sae_a.getPortInst(0), v12_sae_a.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sae_a.getPortInst(0), lNandSae.findPortInstFromProto(layNand.findExport("A")));

                    // R_W pin at (-1060.0, 2060.0) -> via stack at (-700.0, 2060.0) -> invRw.in and nandWe.B
                    NodeInst pinRwLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-1050.0, 2060.0), 5.0, 5.0, lay);
                    Export.newInst(lay, pinRwLay.getPortInst(0), "R_W", ep);

                    NodeInst v12_rw_pin = NodeInst.makeInstance(via12Proto, new Point2D.Double(-700.0, 2060.0), 5.0, 5.0, lay);
                    NodeInst v23_rw_pin = NodeInst.makeInstance(via23Proto, new Point2D.Double(-700.0, 2060.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pinRwLay.getPortInst(0), v12_rw_pin.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_rw_pin.getPortInst(0), v23_rw_pin.getPortInst(0));

                    NodeInst v23_rw_in = NodeInst.makeInstance(via23Proto, new Point2D.Double(-128.0, 2060.0), 6.0, 6.0, lay);
                    NodeInst v12_rw_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(-128.0, yTiming - 1.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_rw_pin.getPortInst(0), v23_rw_in.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_rw_in.getPortInst(0), v12_rw_in.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_rw_in.getPortInst(0), lInvRw.findPortInstFromProto(layInv.findExport("in")));

                    // R_W branch to nandWe.B on dedicated Metal-3 Track 3 at Y = 1945.0 (pitch = 15.0 lambda)
                    // M3 to (-100.0, 2060.0) -> M2 South to (-100.0, 1945.0) -> M3 East to (-41.5, 1945.0) -> M2 South to (-41.5, 1897.5) -> into B
                    NodeInst v23_rw_b_w1 = NodeInst.makeInstance(via23Proto, new Point2D.Double(-100.0, 2060.0), 6.0, 6.0, lay);
                    NodeInst v23_rw_b_w2 = NodeInst.makeInstance(via23Proto, new Point2D.Double(-100.0, 1940.0), 6.0, 6.0, lay);
                    NodeInst v23_rw_b    = NodeInst.makeInstance(via23Proto, new Point2D.Double(-41.5, 1940.0), 6.0, 6.0, lay);
                    NodeInst v12_rw_b    = NodeInst.makeInstance(via12Proto, new Point2D.Double(-41.5, 1897.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_rw_in.getPortInst(0), v23_rw_b_w1.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_rw_b_w1.getPortInst(0), v23_rw_b_w2.getPortInst(0));
                    ArcInst.makeInstance(m3, ep, v23_rw_b_w2.getPortInst(0), v23_rw_b.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_rw_b.getPortInst(0), v12_rw_b.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_rw_b.getPortInst(0), lNandWe.findPortInstFromProto(layNand.findExport("B")));

                    // invRw.out (R_W_b) -> nandSae.B at Y = 1960.0 on Metal-3
                    PortInst pInvRwOut = lInvRw.findPortInstFromProto(layInv.findExport("out"));
                    NodeInst v12_rw_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(-92.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_rw_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(-92.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v23_rw_mid = NodeInst.makeInstance(via23Proto, new Point2D.Double(-92.0, 1960.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pInvRwOut, v12_rw_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_rw_out.getPortInst(0), v23_rw_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_rw_out.getPortInst(0), v23_rw_mid.getPortInst(0));

                    NodeInst v23_sae_b = NodeInst.makeInstance(via23Proto, new Point2D.Double(138.5, 1960.0), 6.0, 6.0, lay);
                    NodeInst v12_sae_b = NodeInst.makeInstance(via12Proto, new Point2D.Double(138.5, 1897.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_rw_mid.getPortInst(0), v23_sae_b.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_sae_b.getPortInst(0), v12_sae_b.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sae_b.getPortInst(0), lNandSae.findPortInstFromProto(layNand.findExport("B")));

                    // NandWe C & D tied to VDD along X = 20.0 on Metal-1
                    PortInst pNandWeC = lNandWe.findPortInstFromProto(layNand.findExport("C"));
                    PortInst pNandWeD = lNandWe.findPortInstFromProto(layNand.findExport("D"));
                    ArcInst.makeInstance(m1, ep, pNandWeC, pNandWeD);
                    NodeInst jWeCd_bot = NodeInst.makeInstance(m1Pin, new Point2D.Double(20.0, 1897.5), 3.0, 3.0, lay);
                    NodeInst jWeCd_top = NodeInst.makeInstance(m1Pin, new Point2D.Double(20.0, yTiming + 60.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, pNandWeD, jWeCd_bot.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jWeCd_bot.getPortInst(0), jWeCd_top.getPortInst(0));

                    // NandSae C & D tied to VDD along X = 200.0 on Metal-1
                    PortInst pNandSaeC = lNandSae.findPortInstFromProto(layNand.findExport("C"));
                    PortInst pNandSaeD = lNandSae.findPortInstFromProto(layNand.findExport("D"));
                    ArcInst.makeInstance(m1, ep, pNandSaeC, pNandSaeD);
                    NodeInst jSaeCd_bot = NodeInst.makeInstance(m1Pin, new Point2D.Double(200.0, 1897.5), 3.0, 3.0, lay);
                    NodeInst jSaeCd_top = NodeInst.makeInstance(m1Pin, new Point2D.Double(200.0, yTiming + 60.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, pNandSaeD, jSaeCd_bot.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jSaeCd_bot.getPortInst(0), jSaeCd_top.getPortInst(0));

                    // nandWe.out -> invWe.in
                    PortInst pNandWeOut = lNandWe.findPortInstFromProto(layNand.findExport("out"));
                    PortInst pInvWeIn   = lInvWe.findPortInstFromProto(layInv.findExport("in"));
                    NodeInst v12_we_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(55.0, yTiming - 1.0), 5.0, 5.0, lay);
                    NodeInst jWeOut_m2 = NodeInst.makeInstance(m2Pin, new Point2D.Double(55.0, yTiming + 45.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m2, ep, pNandWeOut, jWeOut_m2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, jWeOut_m2.getPortInst(0), v12_we_in.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_we_in.getPortInst(0), pInvWeIn);

                    // nandSae.out -> invSae.in
                    PortInst pNandSaeOut = lNandSae.findPortInstFromProto(layNand.findExport("out"));
                    PortInst pInvSaeIn   = lInvSae.findPortInstFromProto(layInv.findExport("in"));
                    NodeInst v12_sae_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(235.0, yTiming - 1.0), 5.0, 5.0, lay);
                    NodeInst jSaeOut_m2 = NodeInst.makeInstance(m2Pin, new Point2D.Double(235.0, yTiming + 45.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m2, ep, pNandSaeOut, jSaeOut_m2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, jSaeOut_m2.getPortInst(0), v12_sae_in.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sae_in.getPortInst(0), pInvSaeIn);

                    // invWe.out (WE) -> write_driver.WE via East Corridor (X = 1220.0) -> South Channel (Y = -640.0) -> X = 110.0 (M2 hop) -> (17.5, -525.0)
                    PortInst pInvWeOut = lInvWe.findPortInstFromProto(layInv.findExport("out"));
                    PortInst pWdWe     = lInstWd.findPortInstFromProto(layWd.findExport("WE"));
                    NodeInst v12_we_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(88.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_we_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(88.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v23_we_tr  = NodeInst.makeInstance(via23Proto, new Point2D.Double(88.0, 2030.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pInvWeOut, v12_we_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_we_out.getPortInst(0), v23_we_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_we_out.getPortInst(0), v23_we_tr.getPortInst(0));

                    NodeInst v23_we_east  = NodeInst.makeInstance(via23Proto, new Point2D.Double(1220.0, 2030.0), 6.0, 6.0, lay);
                    NodeInst v23_we_south = NodeInst.makeInstance(via23Proto, new Point2D.Double(1220.0, -640.0), 6.0, 6.0, lay);
                    NodeInst v12_we_south = NodeInst.makeInstance(via12Proto, new Point2D.Double(1220.0, -640.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_we_tr.getPortInst(0), v23_we_east.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_we_east.getPortInst(0), v23_we_south.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_we_south.getPortInst(0), v12_we_south.getPortInst(0));

                    NodeInst v12_we_hop1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(110.0, -640.0), 5.0, 5.0, lay);
                    NodeInst v12_we_hop2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(110.0, -525.0), 5.0, 5.0, lay);
                    NodeInst cPoly_we    = NodeInst.makeInstance(conPolyM1, new Point2D.Double(17.5, -525.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_we_south.getPortInst(0), v12_we_hop1.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_we_hop1.getPortInst(0), v12_we_hop2.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_we_hop2.getPortInst(0), cPoly_we.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_we.getPortInst(0), pWdWe);

                    // invSae.out (SAE) -> sense_amplifier.SAE via East Corridor (X = 1235.0) -> South Channel (Y = -655.0) -> (140.0, -655.0) -> Poly to pSaSae
                    PortInst pInvSaeOut = lInvSae.findPortInstFromProto(layInv.findExport("out"));
                    PortInst pSaSae     = lInstSa.findPortInstFromProto(laySa.findExport("SAE"));
                    NodeInst v12_sae_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(268.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_sae_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(268.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v23_sae_tr  = NodeInst.makeInstance(via23Proto, new Point2D.Double(268.0, 2045.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pInvSaeOut, v12_sae_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_sae_out.getPortInst(0), v23_sae_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_sae_out.getPortInst(0), v23_sae_tr.getPortInst(0));

                    NodeInst v23_sae_east  = NodeInst.makeInstance(via23Proto, new Point2D.Double(1235.0, 2045.0), 6.0, 6.0, lay);
                    NodeInst v23_sae_south = NodeInst.makeInstance(via23Proto, new Point2D.Double(1235.0, -655.0), 6.0, 6.0, lay);
                    NodeInst v12_sae_south = NodeInst.makeInstance(via12Proto, new Point2D.Double(1235.0, -655.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_sae_tr.getPortInst(0), v23_sae_east.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_sae_east.getPortInst(0), v23_sae_south.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_sae_south.getPortInst(0), v12_sae_south.getPortInst(0));

                    NodeInst cPoly_sae = NodeInst.makeInstance(conPolyM1, new Point2D.Double(140.0, -655.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_sae_south.getPortInst(0), cPoly_sae.getPortInst(0));
                    NodeInst jogSaePoly = NodeInst.makeInstance(polyPin, new Point2D.Double(140.0, -598.0), 2.0, 2.0, lay);
                    ArcInst.makeInstance(poly, ep, cPoly_sae.getPortInst(0), jogSaePoly.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, jogSaePoly.getPortInst(0), pSaSae);

                    // ------------------------------------------
                    // ROUTING F: Global I/O (CLK, ADDR_IN, DIN, DOUT)
                    // ------------------------------------------
                    // CLK via corridor X = -720.0 (clears Div8 internal contact@49 at -685.0)
                    NodeInst pinClkLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-1050.0, 1520.0), 5.0, 5.0, lay);
                    Export.newInst(lay, pinClkLay.getPortInst(0), "CLK", ep);

                    PortInst pDiv8Clk = lInstDiv8.findPortInstFromProto(layDiv8.findExport("CLK"));
                    PortInst pSregClk = lInstSreg.findPortInstFromProto(laySreg.findExport("CLK"));

                    NodeInst v12_clk_cor1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-720.0, 1520.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pinClkLay.getPortInst(0), v12_clk_cor1.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_clk_cor1.getPortInst(0), pDiv8Clk);

                    NodeInst v12_clk_cor2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-720.0, 2199.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_clk_cor1.getPortInst(0), v12_clk_cor2.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_clk_cor2.getPortInst(0), pSregClk);

                    // ADDR_IN
                    NodeInst pinAddrLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-1050.0, 2210.0), 5.0, 5.0, lay);
                    Export.newInst(lay, pinAddrLay.getPortInst(0), "ADDR_IN", ep);

                    PortInst pAddrIn = lInstSreg.findPortInstFromProto(laySreg.findExport("ADDR_IN"));
                    NodeInst v12_addr_1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-720.0, 2210.0), 5.0, 5.0, lay);
                    NodeInst v23_addr_1 = NodeInst.makeInstance(via23Proto, new Point2D.Double(-720.0, 2210.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pinAddrLay.getPortInst(0), v12_addr_1.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_addr_1.getPortInst(0), v23_addr_1.getPortInst(0));

                    NodeInst v23_addr_2 = NodeInst.makeInstance(via23Proto, new Point2D.Double(-560.0, 2210.0), 6.0, 6.0, lay);
                    NodeInst v12_addr_2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-560.0, 2210.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_addr_1.getPortInst(0), v23_addr_2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_addr_2.getPortInst(0), v12_addr_2.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_addr_2.getPortInst(0), pAddrIn);

                    // DIN
                    NodeInst pinDinLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-1050.0, -560.0), 5.0, 5.0, lay);
                    Export.newInst(lay, pinDinLay.getPortInst(0), "DIN", ep);
                    PortInst pWdDin = lInstWd.findPortInstFromProto(layWd.findExport("WD"));
                    NodeInst cPoly_din = NodeInst.makeInstance(conPolyM1, new Point2D.Double(-75.0, -560.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pinDinLay.getPortInst(0), cPoly_din.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_din.getPortInst(0), pWdDin);

                    // DOUT
                    PortInst pSaDout = lInstSa.findPortInstFromProto(laySa.findExport("SO"));
                    NodeInst pinDoutLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(1238.0, -555.0), 5.0, 5.0, lay);
                    Export.newInst(lay, pinDoutLay.getPortInst(0), "DOUT", ep);
                    ArcInst.makeInstance(m1, ep, pSaDout, pinDoutLay.getPortInst(0));

// ------------------------------------------
                    // ROUTING G: Power & Ground Distribution (Strict Manhattan)
                    // ------------------------------------------
                    PortInst pVddDat = lInstData.findPortInstFromProto(layData.findExport("vdd"));
                    PortInst pGndDat = lInstData.findPortInstFromProto(layData.findExport("gnd"));

                    // Downward Power
                    NodeInst v12_vdd_row = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, -400.0), 5.0, 5.0, lay);
                    NodeInst v12_vdd_sa  = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, -508.0), 5.0, 5.0, lay);
                    NodeInst v12_vdd_inv = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, yColInvs + 21.5), 5.0, 5.0, lay);
                    NodeInst v12_vdd_col = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, yColDec - 400.0), 5.0, 5.0, lay);

                    ArcInst.makeInstance(m2, ep, pVddDat, v12_vdd_row.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_vdd_row.getPortInst(0), v12_vdd_sa.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_vdd_sa.getPortInst(0), v12_vdd_inv.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_vdd_inv.getPortInst(0), v12_vdd_col.getPortInst(0));

                    NodeInst v12_gnd_row = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, -420.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_wd  = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, -604.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_inv = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, yColInvs - 23.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_col = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, yColDec - 420.0), 5.0, 5.0, lay);

                    ArcInst.makeInstance(m2, ep, pGndDat, v12_gnd_row.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_row.getPortInst(0), v12_gnd_wd.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_wd.getPortInst(0), v12_gnd_inv.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_inv.getPortInst(0), v12_gnd_col.getPortInst(0));

                    // Row_Decoder power (Strictly horizontal)
                    PortInst pVddRow = lInstRowDec.findPortInstFromProto(layDec.findExport("vdd"));
                    PortInst pGndRow = lInstRowDec.findPortInstFromProto(layDec.findExport("gnd"));
                    ArcInst.makeInstance(m1, ep, v12_vdd_row.getPortInst(0), pVddRow);
                    ArcInst.makeInstance(m1, ep, v12_gnd_row.getPortInst(0), pGndRow);

                    // Col_Decoder power (Strictly horizontal)
                    PortInst pVddCol = lInstColDec.findPortInstFromProto(layDec.findExport("vdd"));
                    PortInst pGndCol = lInstColDec.findPortInstFromProto(layDec.findExport("gnd"));
                    ArcInst.makeInstance(m1, ep, v12_vdd_col.getPortInst(0), pVddCol);
                    ArcInst.makeInstance(m1, ep, v12_gnd_col.getPortInst(0), pGndCol);

                    // WD & SA power (Strict Manhattan)
                    PortInst pVddWd = lInstWd.findPortInstFromProto(layWd.findExport("vdd"));
                    PortInst pGndWd = lInstWd.findPortInstFromProto(layWd.findExport("gnd"));
                    PortInst pVddSa = lInstSa.findPortInstFromProto(laySa.findExport("vdd"));
                    PortInst pGndSa = lInstSa.findPortInstFromProto(laySa.findExport("gnd"));

                    // pVddSa is at (200.0, -508.0). v12_vdd_sa is at (-55.0, -508.0) -> perfectly horizontal!
                    ArcInst.makeInstance(m1, ep, v12_vdd_sa.getPortInst(0), pVddSa);

                    // pVddWd is at (-45.0, -516.0).
                    NodeInst jogVddWd1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(-45.0, -508.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_vdd_sa.getPortInst(0), jogVddWd1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jogVddWd1.getPortInst(0), pVddWd);

                    // pGndWd is at (5.0, -604.0). v12_gnd_wd is at (-75.0, -604.0) -> perfectly horizontal!
                    ArcInst.makeInstance(m1, ep, v12_gnd_wd.getPortInst(0), pGndWd);

                    // pGndSa is at (200.0, -608.0).
                    NodeInst jogGndSa1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(200.0, -604.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_gnd_wd.getPortInst(0), jogGndSa1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jogGndSa1.getPortInst(0), pGndSa);

                    // 16 Inverters power along yColInvs
                    PortInst prevVddPi = v12_vdd_inv.getPortInst(0);
                    for (int c = 0; c < 16; c++) {
                        PortInst pVddIc = lColInvs[c].findPortInstFromProto(layInv.findExport("vdd"));
                        ArcInst.makeInstance(m1, ep, prevVddPi, pVddIc);
                        prevVddPi = pVddIc;
                    }
                    PortInst prevGndPi = v12_gnd_inv.getPortInst(0);
                    for (int c = 0; c < 16; c++) {
                        PortInst pGndIc = lColInvs[c].findPortInstFromProto(layInv.findExport("gnd"));
                        ArcInst.makeInstance(m1, ep, prevGndPi, pGndIc);
                        prevGndPi = pGndIc;
                    }

                    // Upward Power:
                    NodeInst v12_vdd_dtop = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, 1425.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_dtop = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, 1440.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, pVddDat, v12_vdd_dtop.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, pGndDat, v12_gnd_dtop.getPortInst(0));

                    NodeInst v12_vdd_rail_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-750.0, 1425.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_rail_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-770.0, 1440.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_vdd_dtop.getPortInst(0), v12_vdd_rail_bot.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_gnd_dtop.getPortInst(0), v12_gnd_rail_bot.getPortInst(0));

                    NodeInst v12_vdd_div8 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-750.0, yDiv8 + 60.0), 5.0, 5.0, lay);
                    NodeInst v12_vdd_tm   = NodeInst.makeInstance(via12Proto, new Point2D.Double(-750.0, yTiming + 60.0), 5.0, 5.0, lay);
                    NodeInst v12_vdd_sreg = NodeInst.makeInstance(via12Proto, new Point2D.Double(-750.0, ySreg + 60.0), 5.0, 5.0, lay);

                    ArcInst.makeInstance(m2, ep, v12_vdd_rail_bot.getPortInst(0), v12_vdd_div8.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_vdd_div8.getPortInst(0), v12_vdd_tm.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_vdd_tm.getPortInst(0), v12_vdd_sreg.getPortInst(0));

                    NodeInst v12_gnd_div8 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-770.0, yDiv8 - 80.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_tm   = NodeInst.makeInstance(via12Proto, new Point2D.Double(-770.0, yTiming - 70.0), 5.0, 5.0, lay);
                    NodeInst v12_gnd_sreg = NodeInst.makeInstance(via12Proto, new Point2D.Double(-770.0, ySreg - 80.0), 5.0, 5.0, lay);

                    ArcInst.makeInstance(m2, ep, v12_gnd_rail_bot.getPortInst(0), v12_gnd_div8.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_div8.getPortInst(0), v12_gnd_tm.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_tm.getPortInst(0), v12_gnd_sreg.getPortInst(0));

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

                    // Timing gates power chain along Y = yTiming + 60.0 (VDD) and Y = yTiming - 70.0 (GND)
                    double[] xGatesVdd = {-409.5, -309.5, -209.5, -109.5, -28.0, 20.0, 70.5, 152.0, 200.0, 250.5};
                    NodeInst prevVddTm = v12_vdd_tm;
                    for (int i = 0; i < xGatesVdd.length; i++) {
                        NodeInst tapV = NodeInst.makeInstance(m1Pin, new Point2D.Double(xGatesVdd[i], yTiming + 60.0), 3.0, 3.0, lay);
                        ArcInst.makeInstance(m1, ep, prevVddTm.getPortInst(0), tapV.getPortInst(0));
                        if (xGatesVdd[i] == -409.5) ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lInvD1.findPortInstFromProto(layInv.findExport("vdd")));
                        else if (xGatesVdd[i] == -309.5) ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lInvD2.findPortInstFromProto(layInv.findExport("vdd")));
                        else if (xGatesVdd[i] == -209.5) ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lInvD3.findPortInstFromProto(layInv.findExport("vdd")));
                        else if (xGatesVdd[i] == -109.5) ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lInvRw.findPortInstFromProto(layInv.findExport("vdd")));
                        else if (xGatesVdd[i] == -28.0)  ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lNandWe.findPortInstFromProto(layNand.findExport("vdd")));
                        else if (xGatesVdd[i] == 20.0)   ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), jWeCd_top.getPortInst(0));
                        else if (xGatesVdd[i] == 70.5)   ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lInvWe.findPortInstFromProto(layInv.findExport("vdd")));
                        else if (xGatesVdd[i] == 152.0)  ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lNandSae.findPortInstFromProto(layNand.findExport("vdd")));
                        else if (xGatesVdd[i] == 200.0)  ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), jSaeCd_top.getPortInst(0));
                        else if (xGatesVdd[i] == 250.5)  ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), lInvSae.findPortInstFromProto(layInv.findExport("vdd")));
                        prevVddTm = tapV;
                    }

                    double[] xGatesGnd = {-409.5, -309.5, -209.5, -109.5, -28.0, 70.5, 152.0, 250.5};
                    NodeInst prevGndTm = v12_gnd_tm;
                    for (int i = 0; i < xGatesGnd.length; i++) {
                        NodeInst tapG = NodeInst.makeInstance(m1Pin, new Point2D.Double(xGatesGnd[i], yTiming - 70.0), 3.0, 3.0, lay);
                        ArcInst.makeInstance(m1, ep, prevGndTm.getPortInst(0), tapG.getPortInst(0));
                        if (xGatesGnd[i] == -409.5) ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lInvD1.findPortInstFromProto(layInv.findExport("gnd")));
                        else if (xGatesGnd[i] == -309.5) ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lInvD2.findPortInstFromProto(layInv.findExport("gnd")));
                        else if (xGatesGnd[i] == -209.5) ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lInvD3.findPortInstFromProto(layInv.findExport("gnd")));
                        else if (xGatesGnd[i] == -109.5) ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lInvRw.findPortInstFromProto(layInv.findExport("gnd")));
                        else if (xGatesGnd[i] == -28.0)  ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lNandWe.findPortInstFromProto(layNand.findExport("gnd")));
                        else if (xGatesGnd[i] == 70.5)   ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lInvWe.findPortInstFromProto(layInv.findExport("gnd")));
                        else if (xGatesGnd[i] == 152.0)  ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lNandSae.findPortInstFromProto(layNand.findExport("gnd")));
                        else if (xGatesGnd[i] == 250.5)  ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), lInvSae.findPortInstFromProto(layInv.findExport("gnd")));
                        prevGndTm = tapG;
                    }

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

                    int printed = 0;
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog msg = it.next();
                        if (printed < 30) {
                            System.out.println("DRC: " + msg.getMessageString());
                            printed++;
                        }
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
                        System.out.println("=== 5. Exporting SPICE: SRAM_256_Macro.spi ===");
                        Spice.SpicePreferences sp = 
                            (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, sch, true, null);
                        sp.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                        sp.level = "3";
                        sp.doOutput(sch, null, "SRAM_256_Macro.spi");
                        File spiFile = new File("SRAM_256_Macro.spi");
                        System.out.println("SPICE deck exported successfully (" + spiFile.length() + " bytes).");

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
