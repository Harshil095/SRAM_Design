import java.io.File;
import java.net.URL;
import java.util.*;
import java.awt.geom.Point2D;
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
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class BuildRectifiedFinalChip {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildRectifiedFinalChip", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();

                    Cell macroLay = lib.findNodeProto("SRAM_256_Macro{lay}");
                    Cell inPadLay = lib.findNodeProto("Input_Pad{lay}");
                    Cell outPadLay = lib.findNodeProto("Output_Pad{lay}");

                    Cell oldChipLay = lib.findNodeProto("SRAM_Chip_Final{lay}");
                    if (oldChipLay != null) oldChipLay.kill();

                    Cell chipLay = Cell.makeInstance(lib, "SRAM_Chip_Final{lay}");

                    ArcProto poly = chipLay.getTechnology().findArcProto("Polysilicon-1");
                    ArcProto m1   = chipLay.getTechnology().findArcProto("Metal-1");
                    ArcProto m2   = chipLay.getTechnology().findArcProto("Metal-2");
                    ArcProto m3   = chipLay.getTechnology().findArcProto("Metal-3");

                    NodeProto conPolyM1  = chipLay.getTechnology().findNodeProto("Metal-1-Polysilicon-1-Con");
                    NodeProto via12Proto = chipLay.getTechnology().findNodeProto("Metal-1-Metal-2-Con");
                    NodeProto via23Proto = chipLay.getTechnology().findNodeProto("Metal-2-Metal-3-Con");
                    NodeProto m1Pin      = chipLay.getTechnology().findNodeProto("Metal-1-Pin");
                    NodeProto m2Pin      = chipLay.getTechnology().findNodeProto("Metal-2-Pin");
                    NodeProto m3Pin      = chipLay.getTechnology().findNodeProto("Metal-3-Pin");
                    NodeProto polyPin    = chipLay.getTechnology().findNodeProto("Polysilicon-1-Pin");

                    // 1. Place Macro at (0, 0)
                    NodeInst lMacro = NodeInst.makeInstance(macroLay, new Point2D.Double(0.0, 0.0), 0, 0, chipLay);

                    // 2. Power Rings Parameters
                    // Set ring width to 8.0 lambda (prevents wide-metal edge notches with standard 6x6 vias)
                    double ringW = 8.0;
                    double viaRingSz = 8.0;

                    // Inner VDD Ring (clearance > 150 lambda from Macro)
                    double vddL = -1150.0;
                    double vddR = 4450.0;
                    double vddT = 2500.0;
                    double vddB = -2800.0;

                    // Outer GND Ring (clearance 100 lambda from VDD ring)
                    double gndL = -1250.0;
                    double gndR = 4550.0;
                    double gndT = 2600.0;
                    double gndB = -2900.0;

                    // Corner Vias for Rings
                    NodeInst v12_vdd_TL = NodeInst.makeInstance(via12Proto, new Point2D.Double(vddL, vddT), viaRingSz, viaRingSz, chipLay);
                    NodeInst v12_vdd_TR = NodeInst.makeInstance(via12Proto, new Point2D.Double(vddR, vddT), viaRingSz, viaRingSz, chipLay);
                    NodeInst v12_vdd_BR = NodeInst.makeInstance(via12Proto, new Point2D.Double(vddR, vddB), viaRingSz, viaRingSz, chipLay);
                    NodeInst v12_vdd_BL = NodeInst.makeInstance(via12Proto, new Point2D.Double(vddL, vddB), viaRingSz, viaRingSz, chipLay);

                    NodeInst v12_gnd_TL = NodeInst.makeInstance(via12Proto, new Point2D.Double(gndL, gndT), viaRingSz, viaRingSz, chipLay);
                    NodeInst v12_gnd_TR = NodeInst.makeInstance(via12Proto, new Point2D.Double(gndR, gndT), viaRingSz, viaRingSz, chipLay);
                    NodeInst v12_gnd_BR = NodeInst.makeInstance(via12Proto, new Point2D.Double(gndR, gndB), viaRingSz, viaRingSz, chipLay);
                    NodeInst v12_gnd_BL = NodeInst.makeInstance(via12Proto, new Point2D.Double(gndL, gndB), viaRingSz, viaRingSz, chipLay);

                    // Top Rails: M1 width 8.0
                    ArcInst aVddTop = ArcInst.makeInstance(m1, ep, v12_vdd_TL.getPortInst(0), v12_vdd_TR.getPortInst(0));
                    aVddTop.setLambdaBaseWidth(ringW);
                    ArcInst aGndTop = ArcInst.makeInstance(m1, ep, v12_gnd_TL.getPortInst(0), v12_gnd_TR.getPortInst(0));
                    aGndTop.setLambdaBaseWidth(ringW);

                    // Right Rails: M2 width 8.0
                    ArcInst aVddRight = ArcInst.makeInstance(m2, ep, v12_vdd_TR.getPortInst(0), v12_vdd_BR.getPortInst(0));
                    aVddRight.setLambdaBaseWidth(ringW);
                    ArcInst aGndRight = ArcInst.makeInstance(m2, ep, v12_gnd_TR.getPortInst(0), v12_gnd_BR.getPortInst(0));
                    aGndRight.setLambdaBaseWidth(ringW);

                    // 3. Place Input Pads at X = -1600.0
                    double xInPads = -1600.0;
                    double[] yInPads = {2250.0, 1950.0, 1650.0, -560.0};
                    NodeInst lPadClk  = NodeInst.makeInstance(inPadLay, new Point2D.Double(xInPads, yInPads[0]), 0, 0, chipLay);
                    NodeInst lPadAddr = NodeInst.makeInstance(inPadLay, new Point2D.Double(xInPads, yInPads[1]), 0, 0, chipLay);
                    NodeInst lPadRw   = NodeInst.makeInstance(inPadLay, new Point2D.Double(xInPads, yInPads[2]), 0, 0, chipLay);
                    NodeInst lPadDin  = NodeInst.makeInstance(inPadLay, new Point2D.Double(xInPads, yInPads[3]), 0, 0, chipLay);

                    NodeInst[] inPadInsts = {lPadClk, lPadAddr, lPadRw, lPadDin};

                    // Left Power Rails with Taps
                    NodeInst prevVddLeft = v12_vdd_TL;
                    NodeInst prevGndLeft = v12_gnd_TL;

                    for (int i = 0; i < 4; i++) {
                        double yV = yInPads[i] + 21.5;
                        double yG = yInPads[i] - 23.0;

                        // VDD tap on Left M2 rail at (vddL, yV)
                        NodeInst tapV_m2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(vddL, yV), 5.0, 5.0, chipLay);
                        ArcInst aV = ArcInst.makeInstance(m2, ep, prevVddLeft.getPortInst(0), tapV_m2.getPortInst(0));
                        aV.setLambdaBaseWidth(ringW);
                        prevVddLeft = tapV_m2;

                        PortInst pPadV = inPadInsts[i].findPortInstFromProto(inPadLay.findExport("vdd"));
                        // M1 tap from (vddL, yV) across gndL to Input Pad vdd pin
                        ArcInst aTapV = ArcInst.makeInstance(m1, ep, tapV_m2.getPortInst(0), pPadV);
                        aTapV.setLambdaBaseWidth(4.0);

                        // GND tap on Left M2 rail at (gndL, yG)
                        NodeInst tapG_m2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(gndL, yG), 5.0, 5.0, chipLay);
                        ArcInst aG = ArcInst.makeInstance(m2, ep, prevGndLeft.getPortInst(0), tapG_m2.getPortInst(0));
                        aG.setLambdaBaseWidth(ringW);
                        prevGndLeft = tapG_m2;

                        PortInst pPadG = inPadInsts[i].findPortInstFromProto(inPadLay.findExport("gnd"));
                        ArcInst aTapG = ArcInst.makeInstance(m1, ep, tapG_m2.getPortInst(0), pPadG);
                        aTapG.setLambdaBaseWidth(4.0);
                    }
                    ArcInst aVddLeftBot = ArcInst.makeInstance(m2, ep, prevVddLeft.getPortInst(0), v12_vdd_BL.getPortInst(0));
                    aVddLeftBot.setLambdaBaseWidth(ringW);
                    ArcInst aGndLeftBot = ArcInst.makeInstance(m2, ep, prevGndLeft.getPortInst(0), v12_gnd_BL.getPortInst(0));
                    aGndLeftBot.setLambdaBaseWidth(ringW);

                    // 4. Place Output Pad at Y = -3200.0
                    double xOutPad = 300.0;
                    double yOutPad = -3200.0;
                    NodeInst lPadDout = NodeInst.makeInstance(outPadLay, new Point2D.Double(xOutPad, yOutPad), 0, 0, chipLay);

                    // Bottom Taps: Macro Power
                    PortInst pmV = lMacro.findPortInstFromProto(macroLay.findExport("vdd"));
                    NodeInst v12_pmV_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, vddB), viaRingSz, viaRingSz, chipLay);
                    ArcInst aMacVdd = ArcInst.makeInstance(m2, ep, pmV, v12_pmV_bot.getPortInst(0));
                    aMacVdd.setLambdaBaseWidth(ringW);

                    PortInst pmG = lMacro.findPortInstFromProto(macroLay.findExport("gnd"));
                    NodeInst v12_pmG_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, gndB), viaRingSz, viaRingSz, chipLay);
                    ArcInst aMacGnd = ArcInst.makeInstance(m2, ep, pmG, v12_pmG_bot.getPortInst(0));
                    aMacGnd.setLambdaBaseWidth(ringW);

                    // Bottom Taps: Output Pad Power (tapped safely to the WEST at X = 150 and X = 110)
                    PortInst poV = lPadDout.findPortInstFromProto(outPadLay.findExport("vdd"));
                    NodeInst v12_poV_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(150.0, yOutPad + 21.5), 5.0, 5.0, chipLay);
                    NodeInst v12_poV_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(150.0, vddB), 5.0, 5.0, chipLay);
                    ArcInst aPoV_m1 = ArcInst.makeInstance(m1, ep, v12_poV_top.getPortInst(0), poV);
                    aPoV_m1.setLambdaBaseWidth(4.0);
                    ArcInst aOutVdd = ArcInst.makeInstance(m2, ep, v12_poV_top.getPortInst(0), v12_poV_bot.getPortInst(0));
                    aOutVdd.setLambdaBaseWidth(4.0);

                    PortInst poG = lPadDout.findPortInstFromProto(outPadLay.findExport("gnd"));
                    NodeInst v12_poG_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(110.0, yOutPad - 23.0), 5.0, 5.0, chipLay);
                    NodeInst v12_poG_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(110.0, gndB), 5.0, 5.0, chipLay);
                    ArcInst aPoG_m1 = ArcInst.makeInstance(m1, ep, v12_poG_top.getPortInst(0), poG);
                    aPoG_m1.setLambdaBaseWidth(4.0);
                    ArcInst aOutGnd = ArcInst.makeInstance(m2, ep, v12_poG_top.getPortInst(0), v12_poG_bot.getPortInst(0));
                    aOutGnd.setLambdaBaseWidth(4.0);

                    // Daisy-chain Bottom VDD rail (M1 at Y = vddB, width 8.0)
                    ArcInst aVddB1 = ArcInst.makeInstance(m1, ep, v12_vdd_BL.getPortInst(0), v12_pmV_bot.getPortInst(0));
                    aVddB1.setLambdaBaseWidth(ringW);
                    ArcInst aVddB2 = ArcInst.makeInstance(m1, ep, v12_pmV_bot.getPortInst(0), v12_poV_bot.getPortInst(0));
                    aVddB2.setLambdaBaseWidth(ringW);
                    ArcInst aVddB3 = ArcInst.makeInstance(m1, ep, v12_poV_bot.getPortInst(0), v12_vdd_BR.getPortInst(0));
                    aVddB3.setLambdaBaseWidth(ringW);

                    // Daisy-chain Bottom GND rail (M1 at Y = gndB, width 8.0)
                    ArcInst aGndB1 = ArcInst.makeInstance(m1, ep, v12_gnd_BL.getPortInst(0), v12_pmG_bot.getPortInst(0));
                    aGndB1.setLambdaBaseWidth(ringW);
                    ArcInst aGndB2 = ArcInst.makeInstance(m1, ep, v12_pmG_bot.getPortInst(0), v12_poG_bot.getPortInst(0));
                    aGndB2.setLambdaBaseWidth(ringW);
                    ArcInst aGndB3 = ArcInst.makeInstance(m1, ep, v12_poG_bot.getPortInst(0), v12_gnd_BR.getPortInst(0));
                    aGndB3.setLambdaBaseWidth(ringW);

                    // 5. Signal Connections from Pads to Macro
                    // 1. CLK: CHIP_OUT at (xInPads + 168.0, 2212.0)
                    PortInst pClkOut = lPadClk.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroClk = lMacro.findPortInstFromProto(macroLay.findExport("CLK"));
                    NodeInst v23_clk_pad = NodeInst.makeInstance(via23Proto, new Point2D.Double(xInPads + 185.0, 2212.0), 6.0, 6.0, chipLay);
                    NodeInst jClk_m3_top = NodeInst.makeInstance(m3Pin, new Point2D.Double(xInPads + 185.0, 2250.0), 5.0, 5.0, chipLay);
                    NodeInst v23_clk_mac = NodeInst.makeInstance(via23Proto, new Point2D.Double(-685.0, 2250.0), 6.0, 6.0, chipLay);
                    ArcInst aClk1 = ArcInst.makeInstance(m2, ep, pClkOut, v23_clk_pad.getPortInst(0));
                    aClk1.setLambdaBaseWidth(4.0);
                    ArcInst aClk2 = ArcInst.makeInstance(m3, ep, v23_clk_pad.getPortInst(0), jClk_m3_top.getPortInst(0));
                    aClk2.setLambdaBaseWidth(4.0);
                    ArcInst aClk3 = ArcInst.makeInstance(m3, ep, jClk_m3_top.getPortInst(0), v23_clk_mac.getPortInst(0));
                    aClk3.setLambdaBaseWidth(4.0);
                    ArcInst aClk4 = ArcInst.makeInstance(m2, ep, v23_clk_mac.getPortInst(0), pMacroClk);
                    aClk4.setLambdaBaseWidth(4.0);

                    // 2. ADDR_IN: CHIP_OUT at (xInPads + 168.0, 1912.0)
                    PortInst pAddrOut = lPadAddr.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroAddr = lMacro.findPortInstFromProto(macroLay.findExport("ADDR_IN"));
                    NodeInst v23_addr_pad = NodeInst.makeInstance(via23Proto, new Point2D.Double(xInPads + 185.0, 1912.0), 6.0, 6.0, chipLay);
                    NodeInst jAddr_m3_1   = NodeInst.makeInstance(m3Pin, new Point2D.Double(-800.0, 1912.0), 5.0, 5.0, chipLay);
                    NodeInst jAddr_m3_2   = NodeInst.makeInstance(m3Pin, new Point2D.Double(-800.0, 2210.0), 5.0, 5.0, chipLay);
                    NodeInst v23_addr_mac = NodeInst.makeInstance(via23Proto, new Point2D.Double(-560.0, 2210.0), 6.0, 6.0, chipLay);
                    NodeInst v12_addr_mac = NodeInst.makeInstance(via12Proto, new Point2D.Double(-560.0, 2210.0), 5.0, 5.0, chipLay);
                    ArcInst aA1 = ArcInst.makeInstance(m2, ep, pAddrOut, v23_addr_pad.getPortInst(0));
                    aA1.setLambdaBaseWidth(4.0);
                    ArcInst aA2 = ArcInst.makeInstance(m3, ep, v23_addr_pad.getPortInst(0), jAddr_m3_1.getPortInst(0));
                    aA2.setLambdaBaseWidth(4.0);
                    ArcInst aA3 = ArcInst.makeInstance(m3, ep, jAddr_m3_1.getPortInst(0), jAddr_m3_2.getPortInst(0));
                    aA3.setLambdaBaseWidth(4.0);
                    ArcInst aA4 = ArcInst.makeInstance(m3, ep, jAddr_m3_2.getPortInst(0), v23_addr_mac.getPortInst(0));
                    aA4.setLambdaBaseWidth(4.0);
                    ArcInst aA5 = ArcInst.makeInstance(m2, ep, v23_addr_mac.getPortInst(0), v12_addr_mac.getPortInst(0));
                    aA5.setLambdaBaseWidth(4.0);
                    ArcInst aA6 = ArcInst.makeInstance(m1, ep, v12_addr_mac.getPortInst(0), pMacroAddr);
                    aA6.setLambdaBaseWidth(4.0);

                    // 3. R_W: CHIP_OUT at (xInPads + 168.0, 1612.0)
                    PortInst pRwOut = lPadRw.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroRw = lMacro.findPortInstFromProto(macroLay.findExport("R_W"));
                    NodeInst v23_rw_pad = NodeInst.makeInstance(via23Proto, new Point2D.Double(xInPads + 185.0, 1612.0), 6.0, 6.0, chipLay);
                    NodeInst jRw_m3_1   = NodeInst.makeInstance(m3Pin, new Point2D.Double(-700.0, 1612.0), 5.0, 5.0, chipLay);
                    ArcInst aR1 = ArcInst.makeInstance(m2, ep, pRwOut, v23_rw_pad.getPortInst(0));
                    aR1.setLambdaBaseWidth(4.0);
                    ArcInst aR2 = ArcInst.makeInstance(m3, ep, v23_rw_pad.getPortInst(0), jRw_m3_1.getPortInst(0));
                    aR2.setLambdaBaseWidth(4.0);
                    ArcInst aR3 = ArcInst.makeInstance(m3, ep, jRw_m3_1.getPortInst(0), pMacroRw);
                    aR3.setLambdaBaseWidth(4.0);

                    // 4. DIN: CHIP_OUT at (xInPads + 168.0, -598.0)
                    PortInst pDinOut = lPadDin.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroDin = lMacro.findPortInstFromProto(macroLay.findExport("DIN"));
                    NodeInst v23_din_pad = NodeInst.makeInstance(via23Proto, new Point2D.Double(xInPads + 185.0, -598.0), 6.0, 6.0, chipLay);
                    NodeInst v23_din_mid = NodeInst.makeInstance(via23Proto, new Point2D.Double(-180.0, -598.0), 6.0, 6.0, chipLay);
                    NodeInst jDin_m2     = NodeInst.makeInstance(m2Pin, new Point2D.Double(-180.0, -560.0), 3.0, 3.0, chipLay);
                    NodeInst v12_din_mac = NodeInst.makeInstance(via12Proto, new Point2D.Double(-85.0, -560.0), 5.0, 5.0, chipLay);
                    NodeInst cPoly_din   = NodeInst.makeInstance(conPolyM1,  new Point2D.Double(-85.0, -560.0), 5.0, 5.0, chipLay);
                    ArcInst aD1 = ArcInst.makeInstance(m2, ep, pDinOut, v23_din_pad.getPortInst(0));
                    aD1.setLambdaBaseWidth(4.0);
                    ArcInst aD2 = ArcInst.makeInstance(m3, ep, v23_din_pad.getPortInst(0), v23_din_mid.getPortInst(0));
                    aD2.setLambdaBaseWidth(4.0);
                    ArcInst aD3 = ArcInst.makeInstance(m2, ep, v23_din_mid.getPortInst(0), jDin_m2.getPortInst(0));
                    aD3.setLambdaBaseWidth(4.0);
                    ArcInst aD4 = ArcInst.makeInstance(m2, ep, jDin_m2.getPortInst(0), v12_din_mac.getPortInst(0));
                    aD4.setLambdaBaseWidth(4.0);
                    ArcInst aD5 = ArcInst.makeInstance(m1, ep, v12_din_mac.getPortInst(0), cPoly_din.getPortInst(0));
                    aD5.setLambdaBaseWidth(4.0);
                    ArcInst aD6 = ArcInst.makeInstance(poly, ep, cPoly_din.getPortInst(0), pMacroDin);
                    aD6.setLambdaBaseWidth(4.0);

                    // 5. DOUT: macro.DOUT at (213.5, -650) -> drop M2 down to (213.5, yOutPad - 1.0) -> east to (260.0, yOutPad - 1.0) -> Output_Pad.CHIP_IN
                    PortInst pMacroDout = lMacro.findPortInstFromProto(macroLay.findExport("DOUT"));
                    PortInst pDoutIn    = lPadDout.findPortInstFromProto(outPadLay.findExport("CHIP_IN"));
                    NodeInst jDout_m2   = NodeInst.makeInstance(m2Pin, new Point2D.Double(213.5, yOutPad - 1.0), 5.0, 5.0, chipLay);
                    NodeInst v12_dout_pad = NodeInst.makeInstance(via12Proto, new Point2D.Double(260.0, yOutPad - 1.0), 5.0, 5.0, chipLay);
                    ArcInst aDo1 = ArcInst.makeInstance(m2, ep, pMacroDout, jDout_m2.getPortInst(0));
                    aDo1.setLambdaBaseWidth(4.0);
                    ArcInst aDo2 = ArcInst.makeInstance(m2, ep, jDout_m2.getPortInst(0), v12_dout_pad.getPortInst(0));
                    aDo2.setLambdaBaseWidth(4.0);
                    ArcInst aDo3 = ArcInst.makeInstance(m1, ep, v12_dout_pad.getPortInst(0), pDoutIn);
                    aDo3.setLambdaBaseWidth(4.0);

                    // 6. External Wire-Bonding Pads (Metal-3)
                    double xWireBond = -2000.0;
                    NodeInst pinPadClk = NodeInst.makeInstance(m3Pin, new Point2D.Double(xWireBond, yInPads[0] - 1.0), 16.0, 16.0, chipLay);
                    ArcInst aBondClk = ArcInst.makeInstance(m3, ep, pinPadClk.getPortInst(0), lPadClk.findPortInstFromProto(inPadLay.findExport("PAD_IN")));
                    aBondClk.setLambdaBaseWidth(6.0);

                    NodeInst pinPadAddr = NodeInst.makeInstance(m3Pin, new Point2D.Double(xWireBond, yInPads[1] - 1.0), 16.0, 16.0, chipLay);
                    ArcInst aBondAddr = ArcInst.makeInstance(m3, ep, pinPadAddr.getPortInst(0), lPadAddr.findPortInstFromProto(inPadLay.findExport("PAD_IN")));
                    aBondAddr.setLambdaBaseWidth(6.0);

                    NodeInst pinPadRw = NodeInst.makeInstance(m3Pin, new Point2D.Double(xWireBond, yInPads[2] - 1.0), 16.0, 16.0, chipLay);
                    ArcInst aBondRw = ArcInst.makeInstance(m3, ep, pinPadRw.getPortInst(0), lPadRw.findPortInstFromProto(inPadLay.findExport("PAD_IN")));
                    aBondRw.setLambdaBaseWidth(6.0);

                    NodeInst pinPadDin = NodeInst.makeInstance(m3Pin, new Point2D.Double(xWireBond, yInPads[3] - 1.0), 16.0, 16.0, chipLay);
                    ArcInst aBondDin = ArcInst.makeInstance(m3, ep, pinPadDin.getPortInst(0), lPadDin.findPortInstFromProto(inPadLay.findExport("PAD_IN")));
                    aBondDin.setLambdaBaseWidth(6.0);

                    NodeInst pinPadDout = NodeInst.makeInstance(m3Pin, new Point2D.Double(2368.0, yOutPad - 350.0), 16.0, 16.0, chipLay);
                    ArcInst aBondDout = ArcInst.makeInstance(m3, ep, lPadDout.findPortInstFromProto(outPadLay.findExport("PAD_OUT")), pinPadDout.getPortInst(0));
                    aBondDout.setLambdaBaseWidth(6.0);

                    // Exports on SRAM_Chip_Final{lay}
                    Export.newInst(chipLay, pinPadClk.getPortInst(0), "pad_CLK", ep);
                    Export.newInst(chipLay, pinPadAddr.getPortInst(0), "pad_ADDR_IN", ep);
                    Export.newInst(chipLay, pinPadRw.getPortInst(0), "pad_R_W", ep);
                    Export.newInst(chipLay, pinPadDin.getPortInst(0), "pad_DIN", ep);
                    Export.newInst(chipLay, pinPadDout.getPortInst(0), "pad_DOUT", ep);
                    Export.newInst(chipLay, v12_vdd_TL.getPortInst(0), "vdd", ep);
                    Export.newInst(chipLay, v12_gnd_TL.getPortInst(0), "gnd", ep);

                    System.out.println("=== SRAM_Chip_Final{lay} rebuilt successfully! ===");

                    // 7. Verify DRC
                    DRC.resetDRCDates(true);
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, chipLay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println(">>> New Chip DRC Errors: " + errLog.getNumErrors() + " <<<");

                    // 8. Verify Hierarchical NCC against SRAM_Chip_Final{sch}
                    Cell chipSch = lib.findNodeProto("SRAM_Chip_Final{sch}");
                    NccOptions opts = new NccOptions();
                    NccResults res = Ncc.compare(chipSch, null, chipLay, null, opts);
                    System.out.println(">>> NCC Result: " + res + " <<<");

                    if (res != null && res.match()) {
                        Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                        System.out.println("Library saved successfully!");
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
