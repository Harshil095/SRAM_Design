import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.*;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.Iterator;

public class DebugTiming {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("DebugTiming", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();
                    Cell layInv  = lib.findNodeProto("inverter{lay}");
                    Cell layNand = lib.findNodeProto("nand4{lay}");
                    Cell layDiv8 = lib.findNodeProto("Div8_Counter{lay}");
                    Cell layData = lib.findNodeProto("SRAM_Core_Datapath{lay}");
                    Cell layWd   = lib.findNodeProto("write_driver{lay}");
                    Cell laySa   = lib.findNodeProto("sense_amplifier{lay}");

                    Cell old = lib.findNodeProto("Debug_Timing{lay}");
                    if (old != null) old.kill();
                    Cell lay = Cell.makeInstance(lib, "Debug_Timing{lay}");

                    ArcProto poly = lay.getTechnology().findArcProto("Polysilicon-1");
                    ArcProto m1   = lay.getTechnology().findArcProto("Metal-1");
                    ArcProto m2   = lay.getTechnology().findArcProto("Metal-2");
                    ArcProto m3   = lay.getTechnology().findArcProto("Metal-3");
                    NodeProto conPolyM1  = lay.getTechnology().findNodeProto("Metal-1-Polysilicon-1-Con");
                    NodeProto via12Proto = lay.getTechnology().findNodeProto("Metal-1-Metal-2-Con");
                    NodeProto via23Proto = lay.getTechnology().findNodeProto("Metal-2-Metal-3-Con");
                    NodeProto m1Pin      = lay.getTechnology().findNodeProto("Metal-1-Pin");
                    NodeProto m2Pin      = lay.getTechnology().findNodeProto("Metal-2-Pin");

                    NodeInst lInstData = NodeInst.makeInstance(layData, new Point2D.Double(0.0, 0.0), 0, 0, lay);
                    NodeInst lInstDiv8 = NodeInst.makeInstance(layDiv8, new Point2D.Double(-640.0, 1550.0), 0, 0, lay);
                    NodeInst lInstWd   = NodeInst.makeInstance(layWd, new Point2D.Double(0.0, -560.0), 0, 0, lay);
                    NodeInst lInstSa   = NodeInst.makeInstance(laySa, new Point2D.Double(200.0, -560.0), 0, 0, lay);

                    double yTiming = 1900.0;
                    NodeInst lInvD1   = NodeInst.makeInstance(layInv,  new Point2D.Double(-400.0, yTiming), 0, 0, lay);
                    NodeInst lInvD2   = NodeInst.makeInstance(layInv,  new Point2D.Double(-300.0, yTiming), 0, 0, lay);
                    NodeInst lInvD3   = NodeInst.makeInstance(layInv,  new Point2D.Double(-200.0, yTiming), 0, 0, lay);
                    NodeInst lInvRw   = NodeInst.makeInstance(layInv,  new Point2D.Double(-100.0, yTiming), 0, 0, lay);
                    NodeInst lNandWe  = NodeInst.makeInstance(layNand, new Point2D.Double(0.0, yTiming), 0, 0, lay);
                    NodeInst lInvWe   = NodeInst.makeInstance(layInv,  new Point2D.Double(80.0, yTiming), 0, 0, lay);
                    NodeInst lNandSae = NodeInst.makeInstance(layNand, new Point2D.Double(180.0, yTiming), 0, 0, lay);
                    NodeInst lInvSae  = NodeInst.makeInstance(layInv,  new Point2D.Double(260.0, yTiming), 0, 0, lay);

                    // 1. EXECUTE -> PRE_B & invD1.in
                    PortInst lExec = lInstDiv8.findPortInstFromProto(layDiv8.findExport("EXECUTE"));
                    PortInst lPreB = lInstData.findPortInstFromProto(layData.findExport("PRE_B"));
                    // Drop via23Proto DIRECTLY at lExec (1620.0, 1530.0)
                    NodeInst v23_exec_pin = NodeInst.makeInstance(via23Proto, new Point2D.Double(1620.0, 1530.0), 6.0, 6.0, lay);
                    NodeInst v23_exec_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(1620.0, 1790.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m2, ep, lExec, v23_exec_pin.getPortInst(0));
                    ArcInst.makeInstance(m3, ep, v23_exec_pin.getPortInst(0), v23_exec_top.getPortInst(0));

                    NodeInst v23_pre = NodeInst.makeInstance(via23Proto, new Point2D.Double(-34.25, 1790.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_exec_top.getPortInst(0), v23_pre.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_pre.getPortInst(0), lPreB);

                    NodeInst v23_d1 = NodeInst.makeInstance(via23Proto, new Point2D.Double(-425.0, 1790.0), 6.0, 6.0, lay);
                    NodeInst v12_d1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-425.0, 1899.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_pre.getPortInst(0), v23_d1.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_d1.getPortInst(0), v12_d1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_d1.getPortInst(0), lInvD1.findPortInstFromProto(layInv.findExport("in")));

                    // 2. D1 -> D2
                    PortInst pD1Out = lInvD1.findPortInstFromProto(layInv.findExport("out"));
                    PortInst pD2In  = lInvD2.findPortInstFromProto(layInv.findExport("in"));
                    NodeInst jD1_1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(-360.0, 1898.5), 3.0, 3.0, lay);
                    NodeInst jD1_2 = NodeInst.makeInstance(m1Pin, new Point2D.Double(-360.0, 1899.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, pD1Out, jD1_1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jD1_1.getPortInst(0), jD1_2.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jD1_2.getPortInst(0), pD2In);

                    // 3. D2 -> D3 & nandWe.A
                    PortInst pD2Out = lInvD2.findPortInstFromProto(layInv.findExport("out"));
                    PortInst pD3In  = lInvD3.findPortInstFromProto(layInv.findExport("in"));
                    NodeInst jD2_1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(-260.0, 1898.5), 3.0, 3.0, lay);
                    NodeInst jD2_2 = NodeInst.makeInstance(m1Pin, new Point2D.Double(-260.0, 1899.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, pD2Out, jD2_1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jD2_1.getPortInst(0), jD2_2.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jD2_2.getPortInst(0), pD3In);

                    NodeInst v12_d2_tap = NodeInst.makeInstance(via12Proto, new Point2D.Double(-292.0, 1898.5), 5.0, 5.0, lay);
                    NodeInst v12_d2_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-292.0, 1780.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pD2Out, v12_d2_tap.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_d2_tap.getPortInst(0), v12_d2_bot.getPortInst(0));

                    NodeInst v12_we_a = NodeInst.makeInstance(via12Proto, new Point2D.Double(-56.0, 1780.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_d2_bot.getPortInst(0), v12_we_a.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_we_a.getPortInst(0), lNandWe.findPortInstFromProto(layNand.findExport("A")));

                    // 4. D3 -> nandSae.A
                    PortInst pD3Out = lInvD3.findPortInstFromProto(layInv.findExport("out"));
                    NodeInst v12_d3_tap = NodeInst.makeInstance(via12Proto, new Point2D.Double(-192.0, 1898.5), 5.0, 5.0, lay);
                    NodeInst v12_d3_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-192.0, 1800.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pD3Out, v12_d3_tap.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_d3_tap.getPortInst(0), v12_d3_bot.getPortInst(0));

                    NodeInst v12_sae_a = NodeInst.makeInstance(via12Proto, new Point2D.Double(124.0, 1800.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_d3_bot.getPortInst(0), v12_sae_a.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sae_a.getPortInst(0), lNandSae.findPortInstFromProto(layNand.findExport("A")));

                    // 5. R_W -> invRw.in & nandWe.B
                    NodeInst pinRwLay = NodeInst.makeInstance(m2Pin, new Point2D.Double(-700.0, 1760.0), 3.0, 3.0, lay);
                    NodeInst v12_rw_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(-125.0, 1760.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, pinRwLay.getPortInst(0), v12_rw_in.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_rw_in.getPortInst(0), lInvRw.findPortInstFromProto(layInv.findExport("in")));

                    NodeInst v12_we_b = NodeInst.makeInstance(via12Proto, new Point2D.Double(-41.5, 1760.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_rw_in.getPortInst(0), v12_we_b.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_we_b.getPortInst(0), lNandWe.findPortInstFromProto(layNand.findExport("B")));

                    // 6. R_W_b -> nandSae.B
                    PortInst pRwBOut = lInvRw.findPortInstFromProto(layInv.findExport("out"));
                    NodeInst v12_rwb_tap = NodeInst.makeInstance(via12Proto, new Point2D.Double(-92.0, 1898.5), 5.0, 5.0, lay);
                    NodeInst v12_rwb_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-92.0, 1770.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pRwBOut, v12_rwb_tap.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_rwb_tap.getPortInst(0), v12_rwb_bot.getPortInst(0));

                    NodeInst v12_sae_b = NodeInst.makeInstance(via12Proto, new Point2D.Double(138.5, 1770.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_rwb_bot.getPortInst(0), v12_sae_b.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sae_b.getPortInst(0), lNandSae.findPortInstFromProto(layNand.findExport("B")));

                    // 7. nandWe C and D tied together & to VDD
                    PortInst pNandWeC = lNandWe.findPortInstFromProto(layNand.findExport("C"));
                    PortInst pNandWeD = lNandWe.findPortInstFromProto(layNand.findExport("D"));
                    ArcInst.makeInstance(m1, ep, pNandWeC, pNandWeD);
                    NodeInst jWeCD_m1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(20.0, 1847.5), 3.0, 3.0, lay);
                    NodeInst v12_we_cd_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(20.0, 1847.5), 5.0, 5.0, lay);
                    NodeInst v12_we_cd_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(20.0, 1960.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pNandWeD, jWeCD_m1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jWeCD_m1.getPortInst(0), v12_we_cd_bot.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_we_cd_bot.getPortInst(0), v12_we_cd_top.getPortInst(0));

                    // 8. nandSae C and D tied together & to VDD
                    PortInst pNandSaeC = lNandSae.findPortInstFromProto(layNand.findExport("C"));
                    PortInst pNandSaeD = lNandSae.findPortInstFromProto(layNand.findExport("D"));
                    ArcInst.makeInstance(m1, ep, pNandSaeC, pNandSaeD);
                    NodeInst jSaeCD_m1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(200.0, 1847.5), 3.0, 3.0, lay);
                    NodeInst v12_sae_cd_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(200.0, 1847.5), 5.0, 5.0, lay);
                    NodeInst v12_sae_cd_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(200.0, 1960.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pNandSaeD, jSaeCD_m1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jSaeCD_m1.getPortInst(0), v12_sae_cd_bot.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_sae_cd_bot.getPortInst(0), v12_sae_cd_top.getPortInst(0));

                    // 9. nandWe.out -> invWe.in
                    PortInst pNandWeOut = lNandWe.findPortInstFromProto(layNand.findExport("out"));
                    PortInst pInvWeIn   = lInvWe.findPortInstFromProto(layInv.findExport("in"));
                    NodeInst v12_we_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(55.0, 1945.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, pNandWeOut, v12_we_in.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_we_in.getPortInst(0), pInvWeIn);

                    // 10. nandSae.out -> invSae.in
                    PortInst pNandSaeOut = lNandSae.findPortInstFromProto(layNand.findExport("out"));
                    PortInst pInvSaeIn   = lInvSae.findPortInstFromProto(layInv.findExport("in"));
                    NodeInst v12_sae_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(235.0, 1945.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, pNandSaeOut, v12_sae_in.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sae_in.getPortInst(0), pInvSaeIn);

                    // 11. invWe.out (WE) -> write_driver.WE
                    PortInst pInvWeOut = lInvWe.findPortInstFromProto(layInv.findExport("out"));
                    PortInst pWdWe     = lInstWd.findPortInstFromProto(layWd.findExport("WE"));
                    NodeInst v12_we_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(88.0, 1898.5), 5.0, 5.0, lay);
                    NodeInst jWe_mid_top = NodeInst.makeInstance(m2Pin, new Point2D.Double(50.0, 1898.5), 3.0, 3.0, lay);
                    NodeInst v12_we_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(50.0, -450.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pInvWeOut, v12_we_top.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_we_top.getPortInst(0), jWe_mid_top.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, jWe_mid_top.getPortInst(0), v12_we_bot.getPortInst(0));

                    NodeInst cPoly_we = NodeInst.makeInstance(conPolyM1, new Point2D.Double(17.5, -450.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_we_bot.getPortInst(0), cPoly_we.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_we.getPortInst(0), pWdWe);

                    // 12. invSae.out (SAE) -> sense_amplifier.SAE
                    PortInst pInvSaeOut = lInvSae.findPortInstFromProto(layInv.findExport("out"));
                    PortInst pSaSae     = lInstSa.findPortInstFromProto(laySa.findExport("SAE"));
                    NodeInst v12_sae_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(268.0, 1898.5), 5.0, 5.0, lay);
                    NodeInst v12_sae_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(268.0, -598.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pInvSaeOut, v12_sae_top.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_sae_top.getPortInst(0), v12_sae_bot.getPortInst(0));

                    NodeInst cPoly_sae = NodeInst.makeInstance(conPolyM1, new Point2D.Double(240.0, -598.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_sae_bot.getPortInst(0), cPoly_sae.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_sae.getPortInst(0), pSaSae);

                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, lay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println("Debug Timing DRC: " + errLog.getNumErrors() + " errors, " + errLog.getNumWarnings() + " warnings");
                    java.lang.reflect.Field fLines = Class.forName("com.sun.electric.tool.user.ErrorHighPoly").getDeclaredField("linesList");
                    fLines.setAccessible(true);
                    java.lang.reflect.Field fP1 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p1");
                    fP1.setAccessible(true);
                    java.lang.reflect.Field fP2 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p2");
                    fP2.setAccessible(true);

                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        System.out.println("DRC: " + ml.getMessageString());
                        for (Iterator hit = ml.getHighlights(); hit.hasNext(); ) {
                            Object h = hit.next();
                            try {
                                java.util.List lines = (java.util.List) fLines.get(h);
                                if (lines != null) {
                                    for (Object line : lines) {
                                        System.out.println("   Line: " + fP1.get(line) + " -> " + fP2.get(line));
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                } catch (Exception e) { e.printStackTrace(); }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(4000);
    }
}
