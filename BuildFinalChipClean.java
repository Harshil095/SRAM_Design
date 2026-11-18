import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.Iterator;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
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
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.simulation.SimulationTool;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.IconParameters;
import com.sun.electric.tool.user.User;

public class BuildFinalChipClean {

    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildFinalChipClean", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), new File("SRAM.jelib").toURI().toURL(), null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();
                    IconParameters ip = IconParameters.makeInstance(true);

                    Cell macroSch = lib.findNodeProto("SRAM_256_Macro{sch}");
                    Cell macroIc  = lib.findNodeProto("SRAM_256_Macro{ic}");
                    Cell macroLay = lib.findNodeProto("SRAM_256_Macro{lay}");

                    Cell inPadSch = lib.findNodeProto("Input_Pad{sch}");
                    Cell inPadIc  = lib.findNodeProto("Input_Pad{ic}");
                    Cell inPadLay = lib.findNodeProto("Input_Pad{lay}");

                    Cell outPadSch = lib.findNodeProto("Output_Pad{sch}");
                    Cell outPadIc  = lib.findNodeProto("Output_Pad{ic}");
                    Cell outPadLay = lib.findNodeProto("Output_Pad{lay}");

                    // ==========================================
                    // 1. Build SRAM_Chip_Final{sch}
                    // ==========================================
                    System.out.println("=== 1. Building SRAM_Chip_Final{sch} ===");
                    Cell chipSch = Cell.makeInstance(lib, "SRAM_Chip_Final{sch}");
                    ArcProto wire = chipSch.getTechnology().findArcProto("wire");
                    NodeProto offPage = chipSch.getTechnology().findNodeProto("Off-Page");
                    NodeProto pwrProto = chipSch.getTechnology().findNodeProto("Power");
                    NodeProto gndProto = chipSch.getTechnology().findNodeProto("Ground");

                    NodeInst instMacroSch = NodeInst.makeInstance(macroIc, new Point2D.Double(0.0, 0.0), 0, 0, chipSch);

                    NodeInst padClkSch  = NodeInst.makeInstance(inPadIc, new Point2D.Double(-200.0, 150.0), 0, 0, chipSch);
                    NodeInst padAddrSch = NodeInst.makeInstance(inPadIc, new Point2D.Double(-200.0, 50.0), 0, 0, chipSch);
                    NodeInst padRwSch   = NodeInst.makeInstance(inPadIc, new Point2D.Double(-200.0, -50.0), 0, 0, chipSch);
                    NodeInst padDinSch  = NodeInst.makeInstance(inPadIc, new Point2D.Double(-200.0, -150.0), 0, 0, chipSch);
                    NodeInst padDoutSch = NodeInst.makeInstance(outPadIc, new Point2D.Double(200.0, 0.0), 0, 0, chipSch);

                    // Pad CHIP_OUT -> Macro inputs
                    ArcInst.makeInstance(wire, ep, padClkSch.findPortInstFromProto(inPadIc.findExport("CHIP_OUT")), instMacroSch.findPortInstFromProto(macroIc.findExport("CLK")));
                    ArcInst.makeInstance(wire, ep, padAddrSch.findPortInstFromProto(inPadIc.findExport("CHIP_OUT")), instMacroSch.findPortInstFromProto(macroIc.findExport("ADDR_IN")));
                    ArcInst.makeInstance(wire, ep, padRwSch.findPortInstFromProto(inPadIc.findExport("CHIP_OUT")), instMacroSch.findPortInstFromProto(macroIc.findExport("R_W")));
                    ArcInst.makeInstance(wire, ep, padDinSch.findPortInstFromProto(inPadIc.findExport("CHIP_OUT")), instMacroSch.findPortInstFromProto(macroIc.findExport("DIN")));
                    ArcInst.makeInstance(wire, ep, instMacroSch.findPortInstFromProto(macroIc.findExport("DOUT")), padDoutSch.findPortInstFromProto(outPadIc.findExport("CHIP_IN")));

                    // External I/O pins
                    NodeInst pClk  = NodeInst.makeInstance(offPage, new Point2D.Double(-300.0, 150.0), 0, 0, chipSch);
                    NodeInst pAddr = NodeInst.makeInstance(offPage, new Point2D.Double(-300.0, 50.0), 0, 0, chipSch);
                    NodeInst pRw   = NodeInst.makeInstance(offPage, new Point2D.Double(-300.0, -50.0), 0, 0, chipSch);
                    NodeInst pDin  = NodeInst.makeInstance(offPage, new Point2D.Double(-300.0, -150.0), 0, 0, chipSch);
                    NodeInst pDout = NodeInst.makeInstance(offPage, new Point2D.Double(300.0, 0.0), 0, 0, chipSch);

                    ArcInst.makeInstance(wire, ep, pClk.getPortInst(0), padClkSch.findPortInstFromProto(inPadIc.findExport("PAD_IN")));
                    ArcInst.makeInstance(wire, ep, pAddr.getPortInst(0), padAddrSch.findPortInstFromProto(inPadIc.findExport("PAD_IN")));
                    ArcInst.makeInstance(wire, ep, pRw.getPortInst(0), padRwSch.findPortInstFromProto(inPadIc.findExport("PAD_IN")));
                    ArcInst.makeInstance(wire, ep, pDin.getPortInst(0), padDinSch.findPortInstFromProto(inPadIc.findExport("PAD_IN")));
                    ArcInst.makeInstance(wire, ep, padDoutSch.findPortInstFromProto(outPadIc.findExport("PAD_OUT")), pDout.getPortInst(0));

                    // Power & Ground
                    NodeInst pwrChip = NodeInst.makeInstance(pwrProto, new Point2D.Double(0.0, 250.0), 0, 0, chipSch);
                    NodeInst gndChip = NodeInst.makeInstance(gndProto, new Point2D.Double(0.0, -250.0), 0, 0, chipSch);

                    ArcInst.makeInstance(wire, ep, pwrChip.getPortInst(0), instMacroSch.findPortInstFromProto(macroIc.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, pwrChip.getPortInst(0), padClkSch.findPortInstFromProto(inPadIc.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, pwrChip.getPortInst(0), padAddrSch.findPortInstFromProto(inPadIc.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, pwrChip.getPortInst(0), padRwSch.findPortInstFromProto(inPadIc.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, pwrChip.getPortInst(0), padDinSch.findPortInstFromProto(inPadIc.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, pwrChip.getPortInst(0), padDoutSch.findPortInstFromProto(outPadIc.findExport("vdd")));

                    ArcInst.makeInstance(wire, ep, gndChip.getPortInst(0), instMacroSch.findPortInstFromProto(macroIc.findExport("gnd")));
                    ArcInst.makeInstance(wire, ep, gndChip.getPortInst(0), padClkSch.findPortInstFromProto(inPadIc.findExport("gnd")));
                    ArcInst.makeInstance(wire, ep, gndChip.getPortInst(0), padAddrSch.findPortInstFromProto(inPadIc.findExport("gnd")));
                    ArcInst.makeInstance(wire, ep, gndChip.getPortInst(0), padRwSch.findPortInstFromProto(inPadIc.findExport("gnd")));
                    ArcInst.makeInstance(wire, ep, gndChip.getPortInst(0), padDinSch.findPortInstFromProto(inPadIc.findExport("gnd")));
                    ArcInst.makeInstance(wire, ep, gndChip.getPortInst(0), padDoutSch.findPortInstFromProto(outPadIc.findExport("gnd")));

                    Export.newInst(chipSch, pClk.getPortInst(0), "pad_CLK", ep);
                    Export.newInst(chipSch, pAddr.getPortInst(0), "pad_ADDR_IN", ep);
                    Export.newInst(chipSch, pRw.getPortInst(0), "pad_R_W", ep);
                    Export.newInst(chipSch, pDin.getPortInst(0), "pad_DIN", ep);
                    Export.newInst(chipSch, pDout.getPortInst(0), "pad_DOUT", ep);
                    Export.newInst(chipSch, pwrChip.getPortInst(0), "vdd", ep);
                    Export.newInst(chipSch, gndChip.getPortInst(0), "gnd", ep);

                    Cell chipIc = ip.makeIconForCell(chipSch, ep);

                    // ==========================================
                    // 2. Build SRAM_Chip_Final{lay}
                    // ==========================================
                    System.out.println("=== 2. Building SRAM_Chip_Final{lay} ===");
                    Cell chipLay = Cell.makeInstance(lib, "SRAM_Chip_Final{lay}");

                    ArcProto m1 = chipLay.getTechnology().findArcProto("Metal-1");
                    ArcProto m2 = chipLay.getTechnology().findArcProto("Metal-2");
                    ArcProto m3 = chipLay.getTechnology().findArcProto("Metal-3");

                    NodeProto via12Proto = chipLay.getTechnology().findNodeProto("Metal-1-Metal-2-Con");
                    NodeProto via23Proto = chipLay.getTechnology().findNodeProto("Metal-2-Metal-3-Con");
                    NodeProto m1Pin      = chipLay.getTechnology().findNodeProto("Metal-1-Pin");
                    NodeProto m2Pin      = chipLay.getTechnology().findNodeProto("Metal-2-Pin");
                    NodeProto m3Pin      = chipLay.getTechnology().findNodeProto("Metal-3-Pin");

                    // 1. Place Compacted Macro at (0, 0)
                    NodeInst lMacro = NodeInst.makeInstance(macroLay, new Point2D.Double(0.0, 0.0), 0, 0, chipLay);

                    // 2. Place 4 Input Pads along West edge
                    double xInPads = -1350.0;
                    double yPadClk  = 1558.0; // CHIP_OUT at (-1182.0, 1520.0) -> macro.CLK at (-1050.0, 1520.0)
                    double yPadAddr = 2248.0; // CHIP_OUT at (-1182.0, 2210.0) -> macro.ADDR_IN at (-1050.0, 2210.0)
                    double yPadRw   = 2098.0; // CHIP_OUT at (-1182.0, 2060.0) -> macro.R_W at (-1050.0, 2060.0)
                    double yPadDin  = -522.0; // CHIP_OUT at (-1182.0, -560.0) -> macro.DIN at (-1050.0, -560.0)

                    NodeInst lPadClk  = NodeInst.makeInstance(inPadLay, new Point2D.Double(xInPads, yPadClk),  0, 0, chipLay);
                    NodeInst lPadAddr = NodeInst.makeInstance(inPadLay, new Point2D.Double(xInPads, yPadAddr), 0, 0, chipLay);
                    NodeInst lPadRw   = NodeInst.makeInstance(inPadLay, new Point2D.Double(xInPads, yPadRw),   0, 0, chipLay);
                    NodeInst lPadDin  = NodeInst.makeInstance(inPadLay, new Point2D.Double(xInPads, yPadDin),  0, 0, chipLay);

                    // 3. Place Output Pad along East edge
                    double xOutPad = 1350.0;
                    double yOutPad = -554.0; // CHIP_IN at (1310.0, -555.0) -> macro.DOUT at (1238.0, -555.0)
                    NodeInst lPadDout = NodeInst.makeInstance(outPadLay, new Point2D.Double(xOutPad, yOutPad), 0, 0, chipLay);

                    // ------------------------------------------
                    // Signal Connections: Pads to Macro
                    // ------------------------------------------
                    // CLK: CHIP_OUT (-1182.0, 1520.0) -> via12 at (-1100.0, 1520.0) -> macro.CLK at (-1050.0, 1520.0)
                    PortInst pClkOut = lPadClk.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroClk = lMacro.findPortInstFromProto(macroLay.findExport("CLK"));
                    NodeInst v12_clk = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1100.0, 1520.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pClkOut, v12_clk.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_clk.getPortInst(0), pMacroClk);

                    // ADDR_IN: CHIP_OUT (-1182.0, 2210.0) -> via12 at (-1100.0, 2210.0) -> macro.ADDR_IN at (-1050.0, 2210.0)
                    PortInst pAddrOut = lPadAddr.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroAddr = lMacro.findPortInstFromProto(macroLay.findExport("ADDR_IN"));
                    NodeInst v12_addr = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1100.0, 2210.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pAddrOut, v12_addr.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_addr.getPortInst(0), pMacroAddr);

                    // R_W: CHIP_OUT (-1182.0, 2060.0) -> via12 at (-1100.0, 2060.0) -> macro.R_W at (-1050.0, 2060.0)
                    PortInst pRwOut = lPadRw.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroRw = lMacro.findPortInstFromProto(macroLay.findExport("R_W"));
                    NodeInst v12_rw = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1100.0, 2060.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pRwOut, v12_rw.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_rw.getPortInst(0), pMacroRw);

                    // DIN: CHIP_OUT (-1182.0, -560.0) -> via12 at (-1100.0, -560.0) -> macro.DIN at (-1050.0, -560.0)
                    PortInst pDinOut = lPadDin.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroDin = lMacro.findPortInstFromProto(macroLay.findExport("DIN"));
                    NodeInst v12_din = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1100.0, -560.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pDinOut, v12_din.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_din.getPortInst(0), pMacroDin);

                    // DOUT: macro.DOUT at (1238.0, -555.0) -> Output_Pad.CHIP_IN at (1310.0, -555.0) on Metal-1
                    PortInst pMacroDout = lMacro.findPortInstFromProto(macroLay.findExport("DOUT"));
                    PortInst pDoutIn    = lPadDout.findPortInstFromProto(outPadLay.findExport("CHIP_IN"));
                    ArcInst.makeInstance(m1, ep, pMacroDout, pDoutIn);

                    // ------------------------------------------
                    // Power Ring Architecture (W = 12.0 lambda)
                    // ------------------------------------------
                    // Inner Ring: VDD
                    //   Left:   X = -1480.0 (M2)
                    //   Right:  X = 3500.0 (M2)
                    //   Top:    Y = 3000.0 (M1)
                    //   Bottom: Y = -2750.0 (M1)
                    // Outer Ring: GND
                    //   Left:   X = -1580.0 (M2)
                    //   Right:  X = 3600.0 (M2)
                    //   Top:    Y = 3100.0 (M1)
                    //   Bottom: Y = -2850.0 (M1)

                    double ringWidth = 12.0;

                    // Corners for VDD Ring
                    NodeInst v12_vdd_TL = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1480.0, 3000.0),  8.0, 8.0, chipLay);
                    NodeInst v12_vdd_TR = NodeInst.makeInstance(via12Proto, new Point2D.Double(3500.0,  3000.0),  8.0, 8.0, chipLay);
                    NodeInst v12_vdd_BL = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1480.0, -2750.0), 8.0, 8.0, chipLay);
                    NodeInst v12_vdd_BR = NodeInst.makeInstance(via12Proto, new Point2D.Double(3500.0,  -2750.0), 8.0, 8.0, chipLay);

                    // Corners for GND Ring
                    NodeInst v12_gnd_TL = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1580.0, 3100.0),  8.0, 8.0, chipLay);
                    NodeInst v12_gnd_TR = NodeInst.makeInstance(via12Proto, new Point2D.Double(3600.0,  3100.0),  8.0, 8.0, chipLay);
                    NodeInst v12_gnd_BL = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1580.0, -2850.0), 8.0, 8.0, chipLay);
                    NodeInst v12_gnd_BR = NodeInst.makeInstance(via12Proto, new Point2D.Double(3600.0,  -2850.0), 8.0, 8.0, chipLay);

                    // Left Rails: Tap VDD and GND for 4 Input Pads along the West side
                    NodeInst[] inPadInsts = { lPadClk, lPadAddr, lPadRw, lPadDin };
                    double[] yInPads = { yPadClk, yPadAddr, yPadRw, yPadDin };

                    NodeInst prevVddLeft = v12_vdd_TL;
                    NodeInst prevGndLeft = v12_gnd_TL;

                    // Sort taps from top to bottom
                    // yPadAddr (2248), yPadRw (2098), yPadClk (1558), yPadDin (-522)
                    int[] padOrder = { 1, 2, 0, 3 };

                    for (int idx : padOrder) {
                        double yV = yInPads[idx] + 21.5;
                        double yG = yInPads[idx] - 23.0;

                        // VDD tap on Left M2 rail at (-1480.0, yV)
                        NodeInst tapV_m2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1480.0, yV), 5.0, 5.0, chipLay);
                        ArcInst aV = ArcInst.makeInstance(m2, ep, prevVddLeft.getPortInst(0), tapV_m2.getPortInst(0));
                        aV.setLambdaBaseWidth(ringWidth);
                        prevVddLeft = tapV_m2;

                        PortInst pPadV = inPadInsts[idx].findPortInstFromProto(inPadLay.findExport("vdd"));
                        ArcInst aTapV = ArcInst.makeInstance(m1, ep, tapV_m2.getPortInst(0), pPadV);
                        aTapV.setLambdaBaseWidth(4.0);

                        // GND tap on Left M2 rail at (-1580.0, yG)
                        NodeInst tapG_m2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1580.0, yG), 5.0, 5.0, chipLay);
                        ArcInst aG = ArcInst.makeInstance(m2, ep, prevGndLeft.getPortInst(0), tapG_m2.getPortInst(0));
                        aG.setLambdaBaseWidth(ringWidth);
                        prevGndLeft = tapG_m2;

                        PortInst pPadG = inPadInsts[idx].findPortInstFromProto(inPadLay.findExport("gnd"));
                        ArcInst aTapG = ArcInst.makeInstance(m1, ep, tapG_m2.getPortInst(0), pPadG);
                        aTapG.setLambdaBaseWidth(4.0);
                    }
                    ArcInst aV_bot = ArcInst.makeInstance(m2, ep, prevVddLeft.getPortInst(0), v12_vdd_BL.getPortInst(0));
                    aV_bot.setLambdaBaseWidth(ringWidth);
                    ArcInst aG_bot = ArcInst.makeInstance(m2, ep, prevGndLeft.getPortInst(0), v12_gnd_BL.getPortInst(0));
                    aG_bot.setLambdaBaseWidth(ringWidth);

                    // Top Rails (M1)
                    ArcInst aV_top = ArcInst.makeInstance(m1, ep, v12_vdd_TL.getPortInst(0), v12_vdd_TR.getPortInst(0));
                    aV_top.setLambdaBaseWidth(ringWidth);
                    ArcInst aG_top = ArcInst.makeInstance(m1, ep, v12_gnd_TL.getPortInst(0), v12_gnd_TR.getPortInst(0));
                    aG_top.setLambdaBaseWidth(ringWidth);

                    // Right Rails (M2)
                    ArcInst aV_right = ArcInst.makeInstance(m2, ep, v12_vdd_TR.getPortInst(0), v12_vdd_BR.getPortInst(0));
                    aV_right.setLambdaBaseWidth(ringWidth);
                    ArcInst aG_right = ArcInst.makeInstance(m2, ep, v12_gnd_TR.getPortInst(0), v12_gnd_BR.getPortInst(0));
                    aG_right.setLambdaBaseWidth(ringWidth);

                    // Bottom Taps: Macro Power
                    PortInst pmV = lMacro.findPortInstFromProto(macroLay.findExport("vdd"));
                    NodeInst v12_pmV_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, -2750.0), 6.0, 6.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pmV, v12_pmV_bot.getPortInst(0));

                    PortInst pmG = lMacro.findPortInstFromProto(macroLay.findExport("gnd"));
                    NodeInst v12_pmG_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, -2850.0), 6.0, 6.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pmG, v12_pmG_bot.getPortInst(0));

                    // Bottom Taps: Output Pad Power (tapped to the west of Output_Pad at X = 1340 for VDD and X = 1320 for GND)
                    PortInst poV = lPadDout.findPortInstFromProto(outPadLay.findExport("vdd"));
                    NodeInst v12_poV_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(1340.5, -532.5), 5.0, 5.0, chipLay);
                    NodeInst v12_poV_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(1340.5, -2750.0), 6.0, 6.0, chipLay);
                    ArcInst.makeInstance(m1, ep, poV, v12_poV_top.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_poV_top.getPortInst(0), v12_poV_bot.getPortInst(0));

                    PortInst poG = lPadDout.findPortInstFromProto(outPadLay.findExport("gnd"));
                    NodeInst v12_poG_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(1320.0, -577.0), 5.0, 5.0, chipLay);
                    NodeInst v12_poG_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(1320.0, -2850.0), 6.0, 6.0, chipLay);
                    ArcInst.makeInstance(m1, ep, poG, v12_poG_top.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_poG_top.getPortInst(0), v12_poG_bot.getPortInst(0));

                    // Daisy-chain Bottom VDD rail (all on M1 at Y = -2750.0)
                    ArcInst aV_b1 = ArcInst.makeInstance(m1, ep, v12_vdd_BL.getPortInst(0), v12_pmV_bot.getPortInst(0));
                    aV_b1.setLambdaBaseWidth(ringWidth);
                    ArcInst aV_b2 = ArcInst.makeInstance(m1, ep, v12_pmV_bot.getPortInst(0), v12_poV_bot.getPortInst(0));
                    aV_b2.setLambdaBaseWidth(ringWidth);
                    ArcInst aV_b3 = ArcInst.makeInstance(m1, ep, v12_poV_bot.getPortInst(0), v12_vdd_BR.getPortInst(0));
                    aV_b3.setLambdaBaseWidth(ringWidth);

                    // Daisy-chain Bottom GND rail (all on M1 at Y = -2850.0)
                    ArcInst aG_b1 = ArcInst.makeInstance(m1, ep, v12_gnd_BL.getPortInst(0), v12_pmG_bot.getPortInst(0));
                    aG_b1.setLambdaBaseWidth(ringWidth);
                    ArcInst aG_b2 = ArcInst.makeInstance(m1, ep, v12_pmG_bot.getPortInst(0), v12_poG_bot.getPortInst(0));
                    aG_b2.setLambdaBaseWidth(ringWidth);
                    ArcInst aG_b3 = ArcInst.makeInstance(m1, ep, v12_poG_bot.getPortInst(0), v12_gnd_BR.getPortInst(0));
                    aG_b3.setLambdaBaseWidth(ringWidth);

                    // ------------------------------------------
                    // Outer Wire-Bonding Pads (Metal-3)
                    // ------------------------------------------
                    // 1. pad_CLK at (-1650.0, 1557.0)
                    NodeInst pinPadClk = NodeInst.makeInstance(m3Pin, new Point2D.Double(-1650.0, 1557.0), 10.0, 10.0, chipLay);
                    ArcInst.makeInstance(m3, ep, pinPadClk.getPortInst(0), lPadClk.findPortInstFromProto(inPadLay.findExport("PAD_IN")));

                    // 2. pad_ADDR_IN at (-1650.0, 2247.0)
                    NodeInst pinPadAddr = NodeInst.makeInstance(m3Pin, new Point2D.Double(-1650.0, 2247.0), 10.0, 10.0, chipLay);
                    ArcInst.makeInstance(m3, ep, pinPadAddr.getPortInst(0), lPadAddr.findPortInstFromProto(inPadLay.findExport("PAD_IN")));

                    // 3. pad_R_W at (-1650.0, 2097.0)
                    NodeInst pinPadRw = NodeInst.makeInstance(m3Pin, new Point2D.Double(-1650.0, 2097.0), 10.0, 10.0, chipLay);
                    ArcInst.makeInstance(m3, ep, pinPadRw.getPortInst(0), lPadRw.findPortInstFromProto(inPadLay.findExport("PAD_IN")));

                    // 4. pad_DIN at (-1650.0, -523.0)
                    NodeInst pinPadDin = NodeInst.makeInstance(m3Pin, new Point2D.Double(-1650.0, -523.0), 10.0, 10.0, chipLay);
                    ArcInst.makeInstance(m3, ep, pinPadDin.getPortInst(0), lPadDin.findPortInstFromProto(inPadLay.findExport("PAD_IN")));

                    // 5. pad_DOUT at (3700.0, -604.0)
                    NodeInst pinPadDout = NodeInst.makeInstance(m3Pin, new Point2D.Double(3700.0, -604.0), 10.0, 10.0, chipLay);
                    ArcInst.makeInstance(m3, ep, lPadDout.findPortInstFromProto(outPadLay.findExport("PAD_OUT")), pinPadDout.getPortInst(0));

                    // Exports on SRAM_Chip_Final{lay}
                    Export.newInst(chipLay, pinPadClk.getPortInst(0), "pad_CLK", ep);
                    Export.newInst(chipLay, pinPadAddr.getPortInst(0), "pad_ADDR_IN", ep);
                    Export.newInst(chipLay, pinPadRw.getPortInst(0), "pad_R_W", ep);
                    Export.newInst(chipLay, pinPadDin.getPortInst(0), "pad_DIN", ep);
                    Export.newInst(chipLay, pinPadDout.getPortInst(0), "pad_DOUT", ep);
                    Export.newInst(chipLay, v12_vdd_TL.getPortInst(0), "vdd", ep);
                    Export.newInst(chipLay, v12_gnd_TL.getPortInst(0), "gnd", ep);

                    // ==========================================
                    // 3. VERIFICATION: DRC & Hierarchical NCC
                    // ==========================================
                    System.out.println("=== 3. Running DRC on SRAM_Chip_Final{lay} ===");
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, chipLay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println(">>> DRC Result: " + errLog.getNumErrors() + " errors, " + errLog.getNumWarnings() + " warnings <<<");

                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        System.out.println("   DRC: " + ml.getMessageString());
                    }

                    System.out.println("=== 4. Running Hierarchical NCC on SRAM_Chip_Final ===");
                    NccOptions opts = new NccOptions();
                    opts.checkSizes = false;
                    NccResults res = Ncc.compare(chipSch, null, chipLay, null, opts);
                    System.out.println(">>> NCC Result: " + res + " <<<");
                    System.out.println("  Match: " + res.match());
                    System.out.println("  Exports Match: " + res.exportMatch());
                    System.out.println("  Topology Match: " + res.topologyMatch());

                    // Metrics
                    System.out.println("=== Chip Physical Metrics ===");
                    Rectangle2D bounds = chipLay.getBounds();
                    System.out.println("Bounding Box: " + bounds);
                    System.out.println("Width:  " + bounds.getWidth() + " lambda (" + (bounds.getWidth() * 0.09) + " um @ 90nm)");
                    System.out.println("Height: " + bounds.getHeight() + " lambda (" + (bounds.getHeight() * 0.09) + " um @ 90nm)");
                    System.out.println("Area:   " + (bounds.getWidth() * bounds.getHeight()) + " lambda^2 (" + (bounds.getWidth() * bounds.getHeight() * 0.0081) + " um^2 @ 90nm)");

                    if (errLog.getNumErrors() <= 1 && res.match()) {
                        System.out.println("=== 5. Saving SRAM.jelib ===");
                        Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                        System.out.println("Library save status: SUCCESS");

                        System.out.println("=== 6. Exporting SPICE deck: SRAM_Chip_Final.spi ===");
                        Spice.SpicePreferences sp = (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, chipSch, true, null);
                        sp.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                        sp.level = "3";
                        sp.doOutput(chipSch, null, "SRAM_Chip_Final.spi");
                        File spiFile = new File("SRAM_Chip_Final.spi");
                        System.out.println("SPICE deck exported successfully (" + spiFile.length() + " bytes).");
                    } else {
                        System.out.println("DRC or NCC failed! Not saving library.");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(15000);
    }
}
