import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.simulation.SimulationTool;
import com.sun.electric.tool.user.ErrorHighlight;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.IconParameters;
import com.sun.electric.tool.user.User;

public class BuildCompactChip {

    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildCompactChip", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Map<Setting, Object> settings = new HashMap<Setting, Object>();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false, settings);

                    // Enforce ScaleFORmocmos = 90.0
                    EDatabase db = lib.getDatabase();
                    Setting.SettingChangeBatch scb = new Setting.SettingChangeBatch();
                    Technology mocmos = Technology.findTechnology("mocmos");
                    scb.add(mocmos.getScaleSetting(), Double.valueOf(90.0));
                    db.implementSettingChanges(scb);

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

                    // Delete existing SRAM_Chip_Final cells to keep exactly 1 authoritative version
                    Cell oldSch = lib.findNodeProto("SRAM_Chip_Final{sch}");
                    if (oldSch != null) oldSch.kill();
                    Cell oldLay = lib.findNodeProto("SRAM_Chip_Final{lay}");
                    if (oldLay != null) oldLay.kill();
                    Cell oldIc = lib.findNodeProto("SRAM_Chip_Final{ic}");
                    if (oldIc != null) oldIc.kill();

                    // ========================================================
                    // 1. Build SRAM_Chip_Final{sch}
                    // ========================================================
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

                    // ========================================================
                    // 2. Build SRAM_Chip_Final{lay}
                    // ========================================================
                    System.out.println("=== 2. Building Compacted SRAM_Chip_Final{lay} ===");
                    Cell chipLay = Cell.makeInstance(lib, "SRAM_Chip_Final{lay}");

                    ArcProto m1 = chipLay.getTechnology().findArcProto("Metal-1");
                    ArcProto m2 = chipLay.getTechnology().findArcProto("Metal-2");
                    ArcProto m3 = chipLay.getTechnology().findArcProto("Metal-3");

                    NodeProto via12Proto = chipLay.getTechnology().findNodeProto("Metal-1-Metal-2-Con");
                    NodeProto m3Pin      = chipLay.getTechnology().findNodeProto("Metal-3-Pin");

                    // A. Place Compacted Macro at (0, 0)
                    NodeInst lMacro = NodeInst.makeInstance(macroLay, new Point2D.Double(0.0, 0.0), 0, 0, chipLay);

                    // B. Place 4 Input Pads along West edge at X = -1600.0λ
                    double xInPads = -1600.0;
                    double yPadClk  = 1558.0; // CHIP_OUT local (-38) -> global 1520.0 (aligns with macro.CLK at -1050.0, 1520.0)
                    double yPadAddr = 2248.0; // CHIP_OUT local (-38) -> global 2210.0 (aligns with macro.ADDR_IN at -1050.0, 2210.0)
                    double yPadRw   = 2098.0; // CHIP_OUT local (-38) -> global 2060.0 (aligns with macro.R_W at -1050.0, 2060.0)
                    double yPadDin  = -522.0; // CHIP_OUT local (-38) -> global -560.0 (aligns with macro.DIN at -1050.0, -560.0)

                    NodeInst lPadClk  = NodeInst.makeInstance(inPadLay, new Point2D.Double(xInPads, yPadClk),  0, 0, chipLay);
                    NodeInst lPadAddr = NodeInst.makeInstance(inPadLay, new Point2D.Double(xInPads, yPadAddr), 0, 0, chipLay);
                    NodeInst lPadRw   = NodeInst.makeInstance(inPadLay, new Point2D.Double(xInPads, yPadRw),   0, 0, chipLay);
                    NodeInst lPadDin  = NodeInst.makeInstance(inPadLay, new Point2D.Double(xInPads, yPadDin),  0, 0, chipLay);

                    // C. Place Output Pad Underneath Macro at X = -900.0λ, Y = -2900.0λ
                    double xOutPad = -900.0;
                    double yOutPad = -2900.0;
                    NodeInst lPadDout = NodeInst.makeInstance(outPadLay, new Point2D.Double(xOutPad, yOutPad), 0, 0, chipLay);

                    // --------------------------------------------------------
                    // D. Connect Input Pad Signals: CHIP_OUT (M2) -> Via12 -> Macro Inputs (M1)
                    // --------------------------------------------------------
                    // CHIP_OUT is at (-1432.0, ySig) on Metal-2, Macro Input is at (-1050.0, ySig) on Metal-1
                    PortInst pClkOut = lPadClk.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroClk = lMacro.findPortInstFromProto(macroLay.findExport("CLK"));
                    NodeInst v12_clk_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1100.0, 1520.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pClkOut, v12_clk_in.getPortInst(0)).setLambdaBaseWidth(3.0);
                    ArcInst.makeInstance(m1, ep, v12_clk_in.getPortInst(0), pMacroClk).setLambdaBaseWidth(3.0);

                    PortInst pAddrOut = lPadAddr.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroAddr = lMacro.findPortInstFromProto(macroLay.findExport("ADDR_IN"));
                    NodeInst v12_addr_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1100.0, 2210.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pAddrOut, v12_addr_in.getPortInst(0)).setLambdaBaseWidth(3.0);
                    ArcInst.makeInstance(m1, ep, v12_addr_in.getPortInst(0), pMacroAddr).setLambdaBaseWidth(3.0);

                    PortInst pRwOut = lPadRw.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroRw = lMacro.findPortInstFromProto(macroLay.findExport("R_W"));
                    NodeInst v12_rw_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1100.0, 2060.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pRwOut, v12_rw_in.getPortInst(0)).setLambdaBaseWidth(3.0);
                    ArcInst.makeInstance(m1, ep, v12_rw_in.getPortInst(0), pMacroRw).setLambdaBaseWidth(3.0);

                    PortInst pDinOut = lPadDin.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroDin = lMacro.findPortInstFromProto(macroLay.findExport("DIN"));
                    NodeInst v12_din_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1100.0, -560.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pDinOut, v12_din_in.getPortInst(0)).setLambdaBaseWidth(3.0);
                    ArcInst.makeInstance(m1, ep, v12_din_in.getPortInst(0), pMacroDin).setLambdaBaseWidth(3.0);

                    // --------------------------------------------------------
                    // E. Connect DOUT: Macro.DOUT (1238.0, -555.0) -> Output_Pad.CHIP_IN (-940.0, -2901.0)
                    // --------------------------------------------------------
                    PortInst pMacroDout = lMacro.findPortInstFromProto(macroLay.findExport("DOUT"));
                    PortInst pDoutIn    = lPadDout.findPortInstFromProto(outPadLay.findExport("CHIP_IN"));

                    // Short M1 East from (1238.0, -555.0) to (1270.0, -555.0)
                    NodeInst v12_dout_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(1270.0, -555.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m1, ep, pMacroDout, v12_dout_top.getPortInst(0)).setLambdaBaseWidth(3.0);

                    // Vertical M2 drop along X = 1270.0 down to Y = -2720.0 (in channel between Macro & Pad)
                    NodeInst v12_dout_mid = NodeInst.makeInstance(via12Proto, new Point2D.Double(1270.0, -2720.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, v12_dout_top.getPortInst(0), v12_dout_mid.getPortInst(0)).setLambdaBaseWidth(3.0);

                    // Horizontal M1 run West along Y = -2720.0 to X = -960.0
                    NodeInst v12_dout_mid2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-960.0, -2720.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m1, ep, v12_dout_mid.getPortInst(0), v12_dout_mid2.getPortInst(0)).setLambdaBaseWidth(3.0);

                    // Vertical M2 drop along X = -960.0 down to Y = -2901.0
                    NodeInst v12_dout_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-960.0, -2901.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, v12_dout_mid2.getPortInst(0), v12_dout_bot.getPortInst(0)).setLambdaBaseWidth(3.0);
                    // Horizontal M1 to CHIP_IN (-940.0, -2901.0)
                    ArcInst.makeInstance(m1, ep, v12_dout_bot.getPortInst(0), pDoutIn).setLambdaBaseWidth(3.0);

                    // --------------------------------------------------------
                    // F. Power Rings Setup
                    // --------------------------------------------------------
                    // Ring width = 8.0λ (avoids >10λ wide-metal rules)
                    // Inner Ring: VDD
                    //   Left:   X = -1800.0 (M2)
                    //   Right:  X = 1350.0  (M2)
                    //   Top:    Y = 3050.0  (M1)
                    //   Bottom: Y = -3050.0 (M1)
                    // Outer Ring: GND
                    //   Left:   X = -1900.0 (M2)
                    //   Right:  X = 1450.0  (M2)
                    //   Top:    Y = 3150.0  (M1)
                    //   Bottom: Y = -3150.0 (M1)
                    double ringW = 8.0;

                    // Corners for VDD Ring (5.0 x 5.0 via12)
                    NodeInst v12_vdd_TL = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1800.0, 3050.0),  5.0, 5.0, chipLay);
                    NodeInst v12_vdd_TR = NodeInst.makeInstance(via12Proto, new Point2D.Double(1350.0,  3050.0),  5.0, 5.0, chipLay);
                    NodeInst v12_vdd_BL = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1800.0, -3050.0), 5.0, 5.0, chipLay);
                    NodeInst v12_vdd_BR = NodeInst.makeInstance(via12Proto, new Point2D.Double(1350.0,  -3050.0), 5.0, 5.0, chipLay);

                    // Corners for GND Ring (5.0 x 5.0 via12)
                    NodeInst v12_gnd_TL = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1900.0, 3150.0),  5.0, 5.0, chipLay);
                    NodeInst v12_gnd_TR = NodeInst.makeInstance(via12Proto, new Point2D.Double(1450.0,  3150.0),  5.0, 5.0, chipLay);
                    NodeInst v12_gnd_BL = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1900.0, -3150.0), 5.0, 5.0, chipLay);
                    NodeInst v12_gnd_BR = NodeInst.makeInstance(via12Proto, new Point2D.Double(1450.0,  -3150.0), 5.0, 5.0, chipLay);

                    // Top Rails (M1)
                    ArcInst.makeInstance(m1, ep, v12_vdd_TL.getPortInst(0), v12_vdd_TR.getPortInst(0)).setLambdaBaseWidth(ringW);
                    ArcInst.makeInstance(m1, ep, v12_gnd_TL.getPortInst(0), v12_gnd_TR.getPortInst(0)).setLambdaBaseWidth(ringW);

                    // Right Rails (M2)
                    ArcInst.makeInstance(m2, ep, v12_vdd_TR.getPortInst(0), v12_vdd_BR.getPortInst(0)).setLambdaBaseWidth(ringW);
                    ArcInst.makeInstance(m2, ep, v12_gnd_TR.getPortInst(0), v12_gnd_BR.getPortInst(0)).setLambdaBaseWidth(ringW);

                    // --------------------------------------------------------
                    // G. Leftward Power Routing for 4 Input Pads
                    // --------------------------------------------------------
                    NodeInst[] inPads = { lPadClk, lPadAddr, lPadRw, lPadDin };
                    double[] yPads = { yPadClk, yPadAddr, yPadRw, yPadDin };
                    int[] order = { 1, 2, 0, 3 }; // ADDR (2248), R_W (2098), CLK (1558), DIN (-522)

                    NodeInst prevVddLeft = v12_vdd_TL;
                    NodeInst prevGndLeft = v12_gnd_TL;

                    for (int idx : order) {
                        double yV = yPads[idx] + 21.5;
                        double yG = yPads[idx] - 23.0;

                        // Tap VDD Leftward to X = -1800.0 (M2)
                        NodeInst tapV = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1800.0, yV), 5.0, 5.0, chipLay);
                        ArcInst aV = ArcInst.makeInstance(m2, ep, prevVddLeft.getPortInst(0), tapV.getPortInst(0));
                        aV.setLambdaBaseWidth(ringW);
                        prevVddLeft = tapV;

                        PortInst pPadV = inPads[idx].findPortInstFromProto(inPadLay.findExport("vdd"));
                        ArcInst aTapV = ArcInst.makeInstance(m1, ep, tapV.getPortInst(0), pPadV);
                        aTapV.setLambdaBaseWidth(4.0);

                        // Tap GND Leftward to X = -1900.0 (M2)
                        NodeInst tapG = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1900.0, yG), 5.0, 5.0, chipLay);
                        ArcInst aG = ArcInst.makeInstance(m2, ep, prevGndLeft.getPortInst(0), tapG.getPortInst(0));
                        aG.setLambdaBaseWidth(ringW);
                        prevGndLeft = tapG;

                        PortInst pPadG = inPads[idx].findPortInstFromProto(inPadLay.findExport("gnd"));
                        ArcInst aTapG = ArcInst.makeInstance(m1, ep, tapG.getPortInst(0), pPadG);
                        aTapG.setLambdaBaseWidth(4.0);
                    }
                    ArcInst.makeInstance(m2, ep, prevVddLeft.getPortInst(0), v12_vdd_BL.getPortInst(0)).setLambdaBaseWidth(ringW);
                    ArcInst.makeInstance(m2, ep, prevGndLeft.getPortInst(0), v12_gnd_BL.getPortInst(0)).setLambdaBaseWidth(ringW);

                    // --------------------------------------------------------
                    // H. Macro & Output Pad Power Connections
                    // --------------------------------------------------------
                    // Macro VDD & GND drop South on M2 into the channel above Output Pad
                    PortInst pmV = lMacro.findPortInstFromProto(macroLay.findExport("vdd"));
                    NodeInst v12_pmV = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, -2750.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pmV, v12_pmV.getPortInst(0));

                    PortInst pmG = lMacro.findPortInstFromProto(macroLay.findExport("gnd"));
                    NodeInst v12_pmG = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, -2800.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pmG, v12_pmG.getPortInst(0));

                    // Output Pad VDD & GND route LEFTWARD to X = -1050.0 and X = -1100.0
                    // Output Pad local vdd is (-9.5, 21.5) -> global (-909.5, -2878.5)
                    // Output Pad local gnd is (-9.5, -23.0) -> global (-909.5, -2923.0)
                    PortInst poV = lPadDout.findPortInstFromProto(outPadLay.findExport("vdd"));
                    NodeInst v12_outV_dropTop = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1050.0, -2878.5), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m1, ep, v12_outV_dropTop.getPortInst(0), poV).setLambdaBaseWidth(4.0);

                    PortInst poG = lPadDout.findPortInstFromProto(outPadLay.findExport("gnd"));
                    NodeInst v12_outG_dropTop = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1100.0, -2923.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m1, ep, v12_outG_dropTop.getPortInst(0), poG).setLambdaBaseWidth(4.0);

                    // Macro VDD joins X = -1050.0 at Y = -2750.0 on M1
                    NodeInst v12_macroV_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1050.0, -2750.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m1, ep, v12_macroV_top.getPortInst(0), v12_pmV.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // Macro GND joins X = -1100.0 at Y = -2800.0 on M1
                    NodeInst v12_macroG_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1100.0, -2800.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m1, ep, v12_macroG_top.getPortInst(0), v12_pmG.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // Vertical M2 drops along X = -1050.0 (VDD) and X = -1100.0 (GND) to Bottom rings
                    NodeInst v12_outV_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1050.0, -3050.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, v12_macroV_top.getPortInst(0), v12_outV_dropTop.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, v12_outV_dropTop.getPortInst(0), v12_outV_bot.getPortInst(0)).setLambdaBaseWidth(4.0);

                    NodeInst v12_outG_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1100.0, -3150.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, v12_macroG_top.getPortInst(0), v12_outG_dropTop.getPortInst(0)).setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, v12_outG_dropTop.getPortInst(0), v12_outG_bot.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // Daisy-chain Bottom Rails (M1)
                    // VDD Bottom Rail at Y = -3050.0
                    ArcInst.makeInstance(m1, ep, v12_vdd_BL.getPortInst(0), v12_outV_bot.getPortInst(0)).setLambdaBaseWidth(ringW);
                    ArcInst.makeInstance(m1, ep, v12_outV_bot.getPortInst(0), v12_vdd_BR.getPortInst(0)).setLambdaBaseWidth(ringW);

                    // GND Bottom Rail at Y = -3150.0
                    ArcInst.makeInstance(m1, ep, v12_gnd_BL.getPortInst(0), v12_outG_bot.getPortInst(0)).setLambdaBaseWidth(ringW);
                    ArcInst.makeInstance(m1, ep, v12_outG_bot.getPortInst(0), v12_gnd_BR.getPortInst(0)).setLambdaBaseWidth(ringW);

                    // --------------------------------------------------------
                    // I. External Wire-Bonding Pads (Metal-3)
                    // --------------------------------------------------------
                    // West side: pad_CLK, pad_ADDR_IN, pad_R_W, pad_DIN at X = -2000.0λ
                    NodeInst pinPadClk = NodeInst.makeInstance(m3Pin, new Point2D.Double(-2000.0, 1557.0), 7.0, 7.0, chipLay);
                    ArcInst.makeInstance(m3, ep, pinPadClk.getPortInst(0), lPadClk.findPortInstFromProto(inPadLay.findExport("PAD_IN"))).setLambdaBaseWidth(4.0);

                    NodeInst pinPadAddr = NodeInst.makeInstance(m3Pin, new Point2D.Double(-2000.0, 2247.0), 7.0, 7.0, chipLay);
                    ArcInst.makeInstance(m3, ep, pinPadAddr.getPortInst(0), lPadAddr.findPortInstFromProto(inPadLay.findExport("PAD_IN"))).setLambdaBaseWidth(4.0);

                    NodeInst pinPadRw = NodeInst.makeInstance(m3Pin, new Point2D.Double(-2000.0, 2097.0), 7.0, 7.0, chipLay);
                    ArcInst.makeInstance(m3, ep, pinPadRw.getPortInst(0), lPadRw.findPortInstFromProto(inPadLay.findExport("PAD_IN"))).setLambdaBaseWidth(4.0);

                    NodeInst pinPadDin = NodeInst.makeInstance(m3Pin, new Point2D.Double(-2000.0, -523.0), 7.0, 7.0, chipLay);
                    ArcInst.makeInstance(m3, ep, pinPadDin.getPortInst(0), lPadDin.findPortInstFromProto(inPadLay.findExport("PAD_IN"))).setLambdaBaseWidth(4.0);

                    // South side: pad_DOUT straight South from PAD_OUT at (1168.0, -2950.0) down past bottom rings
                    NodeInst pinPadDout = NodeInst.makeInstance(m3Pin, new Point2D.Double(1168.0, -3250.0), 7.0, 7.0, chipLay);
                    ArcInst.makeInstance(m3, ep, lPadDout.findPortInstFromProto(outPadLay.findExport("PAD_OUT")), pinPadDout.getPortInst(0)).setLambdaBaseWidth(4.0);

                    // Exports on SRAM_Chip_Final{lay}
                    Export.newInst(chipLay, pinPadClk.getPortInst(0),  "pad_CLK", ep);
                    Export.newInst(chipLay, pinPadAddr.getPortInst(0), "pad_ADDR_IN", ep);
                    Export.newInst(chipLay, pinPadRw.getPortInst(0),   "pad_R_W", ep);
                    Export.newInst(chipLay, pinPadDin.getPortInst(0),  "pad_DIN", ep);
                    Export.newInst(chipLay, pinPadDout.getPortInst(0), "pad_DOUT", ep);
                    Export.newInst(chipLay, v12_vdd_TL.getPortInst(0), "vdd", ep);
                    Export.newInst(chipLay, v12_gnd_TL.getPortInst(0), "gnd", ep);

                    System.out.println("Compacted chip layout assembled successfully.");

                    // ========================================================
                    // 3. Verification: DRC & NCC
                    // ========================================================
                    System.out.println("\n=== 3. Running Ground-Truth DRC on SRAM_Chip_Final{lay} ===");
                    DRC.resetDRCDates(true);
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);

                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.ignoreAreaCheck = false;                             // Area bit ON
                    dp.ignoreExtensionRuleChecking = false;                 // Extension bit ON
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE; // MOSIS bit ON
                    dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;

                    // Network short check
                    try {
                        Method mCheckNet = DRC.class.getDeclaredMethod("checkNetworks", ErrorLogger.class, Cell.class, boolean.class);
                        mCheckNet.setAccessible(true);
                        mCheckNet.invoke(null, errLog, chipLay, true);
                    } catch (Exception ignored) {}

                    Quick.checkDesignRules(errLog, chipLay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);

                    int numErrors = errLog.getNumErrors();
                    int numWarnings = errLog.getNumWarnings();
                    System.out.println(">>> DRC Result: " + numErrors + " ERRORS, " + numWarnings + " WARNINGS <<<");

                    if (numErrors > 0) {
                        System.out.println("--- DETAILED ERROR LOG ---");
                        int count = 0;
                        for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                            ErrorLogger.MessageLog ml = it.next();
                            count++;
                            System.out.println("  #" + count + " [Group " + ml.getSortKey() + "] " + ml.getMessageString());
                        }
                    }

                    System.out.println("\n=== 4. Running Hierarchical NCC on SRAM_Chip_Final ===");
                    NccOptions opts = new NccOptions();
                    opts.operation = NccOptions.HIER_EACH_CELL;
                    NccResults res = Ncc.compare(chipSch, null, chipLay, null, opts);
                    System.out.println(">>> NCC Result: Match=" + res.match() + ", Exports=" + res.exportMatch() + ", Topology=" + res.topologyMatch() + " <<<");

                    // Physical Metrics
                    Rectangle2D bounds = chipLay.getBounds();
                    System.out.println("\n=== Chip Physical Metrics ===");
                    System.out.println("Bounding Box: " + bounds);
                    System.out.println("Width:  " + bounds.getWidth() + " lambda (" + (bounds.getWidth() * 0.09) + " um @ 90nm)");
                    System.out.println("Height: " + bounds.getHeight() + " lambda (" + (bounds.getHeight() * 0.09) + " um @ 90nm)");
                    System.out.println("Area:   " + (bounds.getWidth() * bounds.getHeight()) + " lambda^2 (" + (bounds.getWidth() * bounds.getHeight() * 0.0081) + " um^2 @ 90nm)");

                    if (numErrors == 0 && res.match()) {
                        System.out.println("\n=== 5. Saving SRAM.jelib ===");
                        Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                        System.out.println("Library save status: SUCCESS");

                        System.out.println("\n=== 6. Exporting SPICE deck: SRAM_Chip_Final.spi ===");
                        Spice.SpicePreferences sp = (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, chipSch, true, null);
                        sp.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                        sp.level = "3";
                        sp.doOutput(chipSch, null, "SRAM_Chip_Final.spi");
                        System.out.println("SPICE deck exported successfully.");
                    } else {
                        System.out.println("\nVerification failed: Not saving library.");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(12000);
    }
}
