import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
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
import com.sun.electric.util.math.Orientation;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

public class BuildAndCommitPhase2 {
    public static void main(String[] args) throws Exception {
        // Backup SRAM.jelib first
        File orig = new File("SRAM.jelib");
        File bak = new File("SRAM.jelib.phase2_backup");
        Files.copy(orig.toPath(), bak.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Backed up SRAM.jelib to SRAM.jelib.phase2_backup");

        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildAndCommitPhase2", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();
                    IconParameters ip = IconParameters.makeInstance(true);

                    // ==========================================
                    // 1. Task 6.1: SRAM_Row_16
                    // ==========================================
                    System.out.println("--- Building SRAM_Row_16 ---");
                    Cell ic6T = lib.findNodeProto("6T{ic}");
                    Cell lay6T = lib.findNodeProto("6T{lay}");
                    
                    Cell schRow = Cell.makeInstance(lib, "SRAM_Row_16{sch}");
                    Cell layRow = Cell.makeInstance(lib, "SRAM_Row_16{lay}");
                    
                    com.sun.electric.technology.ArcProto wireProto = schRow.getTechnology().findArcProto("wire");
                    com.sun.electric.technology.ArcProto m1Proto = layRow.getTechnology().findArcProto("Metal-1");
                    com.sun.electric.technology.ArcProto m2Proto = layRow.getTechnology().findArcProto("Metal-2");
                    com.sun.electric.database.prototype.NodeProto viaProto = layRow.getTechnology().findNodeProto("Metal-1-Metal-2-Con");

                    NodeInst[] sch6TInsts = new NodeInst[16];
                    NodeInst[] lay6TInsts = new NodeInst[16];
                    for (int i = 0; i < 16; i++) {
                        sch6TInsts[i] = NodeInst.makeInstance(ic6T, new Point2D.Double(i * 20.0, 0), 0, 0, schRow);
                        lay6TInsts[i] = NodeInst.makeInstance(lay6T, new Point2D.Double(i * 69.0, 0), 0, 0, layRow);
                        
                        Export.newInst(schRow, sch6TInsts[i].findPortInstFromProto(ic6T.findExport("BL")), "BL[" + i + "]", ep);
                        Export.newInst(schRow, sch6TInsts[i].findPortInstFromProto(ic6T.findExport("BLB")), "BLB[" + i + "]", ep);
                        Export.newInst(layRow, lay6TInsts[i].findPortInstFromProto(lay6T.findExport("BL")), "BL[" + i + "]", ep);
                        Export.newInst(layRow, lay6TInsts[i].findPortInstFromProto(lay6T.findExport("BLB")), "BLB[" + i + "]", ep);
                    }
                    for (int i = 0; i < 15; i++) {
                        ArcInst.makeInstance(wireProto, ep, 
                            sch6TInsts[i].findPortInstFromProto(ic6T.findExport("WL")), sch6TInsts[i+1].findPortInstFromProto(ic6T.findExport("WL")));
                        ArcInst.makeInstance(wireProto, ep, 
                            sch6TInsts[i].findPortInstFromProto(ic6T.findExport("vdd")), sch6TInsts[i+1].findPortInstFromProto(ic6T.findExport("vdd")));
                        ArcInst.makeInstance(wireProto, ep, 
                            sch6TInsts[i].findPortInstFromProto(ic6T.findExport("gnd")), sch6TInsts[i+1].findPortInstFromProto(ic6T.findExport("gnd")));
                        
                        ArcInst.makeInstance(m1Proto, ep, 
                            lay6TInsts[i].findPortInstFromProto(lay6T.findExport("WL")), lay6TInsts[i+1].findPortInstFromProto(lay6T.findExport("WL")));
                        ArcInst.makeInstance(m1Proto, ep, 
                            lay6TInsts[i].findPortInstFromProto(lay6T.findExport("vdd")), lay6TInsts[i+1].findPortInstFromProto(lay6T.findExport("vdd")));
                        ArcInst.makeInstance(m1Proto, ep, 
                            lay6TInsts[i].findPortInstFromProto(lay6T.findExport("gnd")), lay6TInsts[i+1].findPortInstFromProto(lay6T.findExport("gnd")));
                    }
                    Export.newInst(schRow, sch6TInsts[0].findPortInstFromProto(ic6T.findExport("WL")), "WL", ep);
                    Export.newInst(schRow, sch6TInsts[0].findPortInstFromProto(ic6T.findExport("vdd")), "vdd", ep);
                    Export.newInst(schRow, sch6TInsts[0].findPortInstFromProto(ic6T.findExport("gnd")), "gnd", ep);
                    Export.newInst(layRow, lay6TInsts[0].findPortInstFromProto(lay6T.findExport("WL")), "WL", ep);
                    Export.newInst(layRow, lay6TInsts[0].findPortInstFromProto(lay6T.findExport("vdd")), "vdd", ep);
                    Export.newInst(layRow, lay6TInsts[0].findPortInstFromProto(lay6T.findExport("gnd")), "gnd", ep);

                    Cell icRow = ip.makeIconForCell(schRow, ep);
                    System.out.println("  SRAM_Row_16 created: sch, lay, ic");

                    // ==========================================
                    // 2. Task 6.2: SRAM_Array_16x16
                    // ==========================================
                    System.out.println("--- Building SRAM_Array_16x16 ---");
                    Cell layArray = Cell.makeInstance(lib, "SRAM_Array_16x16{lay}");
                    NodeInst[] rows = new NodeInst[16];
                    for (int r = 0; r < 16; r++) {
                        double y = (r / 2) * 170.0 + (r % 2 == 1 ? 158.0 : 0.0);
                        Orientation orient = (r % 2 == 1) ? Orientation.Y : Orientation.IDENT;
                        rows[r] = NodeInst.makeInstance(layRow, ep, new Point2D.Double(0, y), 0, 0, layArray, orient, "row@" + r);
                    }
                    
                    // Vertical bitline connections
                    for (int c = 0; c < 16; c++) {
                        String blName = "BL[" + c + "]";
                        String blbName = "BLB[" + c + "]";
                        for (int r = 0; r < 15; r++) {
                            PortInst blTop = rows[r].findPortInstFromProto(layRow.findExport(blName));
                            PortInst blBot = rows[r+1].findPortInstFromProto(layRow.findExport(blName));
                            ArcInst.makeInstance(m2Proto, ep, blTop, blBot);
                            
                            PortInst blbTop = rows[r].findPortInstFromProto(layRow.findExport(blbName));
                            PortInst blbBot = rows[r+1].findPortInstFromProto(layRow.findExport(blbName));
                            ArcInst.makeInstance(m2Proto, ep, blbTop, blbBot);
                        }
                    }
                    
                    // VDD horizontal pairs
                    for (int k = 0; k < 8; k++) {
                        PortInst v0 = rows[2*k].findPortInstFromProto(layRow.findExport("vdd"));
                        PortInst v1 = rows[2*k+1].findPortInstFromProto(layRow.findExport("vdd"));
                        ArcInst.makeInstance(m1Proto, ep, v0, v1);
                    }

                    // VDD vertical bus at X = -55.0
                    NodeInst[] vddVias = new NodeInst[16];
                    for (int r = 0; r < 16; r++) {
                        PortInst vRow = rows[r].findPortInstFromProto(layRow.findExport("vdd"));
                        double yVdd = vRow.getCenter().getY();
                        vddVias[r] = NodeInst.makeInstance(viaProto, new Point2D.Double(-55.0, yVdd), 5.0, 5.0, layArray);
                        ArcInst.makeInstance(m1Proto, ep, vRow, vddVias[r].findPortInstFromProto(viaProto.getPort(0)));
                    }
                    for (int r = 0; r < 15; r++) {
                        ArcInst.makeInstance(m2Proto, ep, 
                            vddVias[r].findPortInstFromProto(viaProto.getPort(0)), 
                            vddVias[r+1].findPortInstFromProto(viaProto.getPort(0)));
                    }

                    // GND vertical bus at X = -75.0
                    NodeInst[] gndVias = new NodeInst[16];
                    for (int r = 0; r < 16; r++) {
                        PortInst gRow = rows[r].findPortInstFromProto(layRow.findExport("gnd"));
                        double yGnd = gRow.getCenter().getY();
                        gndVias[r] = NodeInst.makeInstance(viaProto, new Point2D.Double(-75.0, yGnd), 5.0, 5.0, layArray);
                        ArcInst.makeInstance(m1Proto, ep, gRow, gndVias[r].findPortInstFromProto(viaProto.getPort(0)));
                    }
                    for (int r = 0; r < 15; r++) {
                        ArcInst.makeInstance(m2Proto, ep, 
                            gndVias[r].findPortInstFromProto(viaProto.getPort(0)), 
                            gndVias[r+1].findPortInstFromProto(viaProto.getPort(0)));
                    }

                    // Exports on layArray (bottom edge for BL/BLB, left edge for WL)
                    for (int c = 0; c < 16; c++) {
                        Export.newInst(layArray, rows[0].findPortInstFromProto(layRow.findExport("BL[" + c + "]")), "BL[" + c + "]", ep);
                        Export.newInst(layArray, rows[0].findPortInstFromProto(layRow.findExport("BLB[" + c + "]")), "BLB[" + c + "]", ep);
                    }
                    for (int r = 0; r < 16; r++) {
                        Export.newInst(layArray, rows[r].findPortInstFromProto(layRow.findExport("WL")), "WL[" + r + "]", ep);
                    }
                    Export.newInst(layArray, vddVias[0].findPortInstFromProto(viaProto.getPort(0)), "vdd", ep);
                    Export.newInst(layArray, gndVias[0].findPortInstFromProto(viaProto.getPort(0)), "gnd", ep);

                    // Schematic for SRAM_Array_16x16
                    Cell schArray = Cell.makeInstance(lib, "SRAM_Array_16x16{sch}");
                    NodeInst[] schRows = new NodeInst[16];
                    for (int r = 0; r < 16; r++) {
                        schRows[r] = NodeInst.makeInstance(icRow, new Point2D.Double(0, r * 30.0), 0, 0, schArray);
                    }
                    for (int c = 0; c < 16; c++) {
                        String blName = "BL[" + c + "]";
                        String blbName = "BLB[" + c + "]";
                        for (int r = 0; r < 15; r++) {
                            ArcInst.makeInstance(wireProto, ep, 
                                schRows[r].findPortInstFromProto(icRow.findExport(blName)), 
                                schRows[r+1].findPortInstFromProto(icRow.findExport(blName)));
                            ArcInst.makeInstance(wireProto, ep, 
                                schRows[r].findPortInstFromProto(icRow.findExport(blbName)), 
                                schRows[r+1].findPortInstFromProto(icRow.findExport(blbName)));
                        }
                    }
                    for (int r = 0; r < 15; r++) {
                        ArcInst.makeInstance(wireProto, ep, 
                            schRows[r].findPortInstFromProto(icRow.findExport("vdd")), 
                            schRows[r+1].findPortInstFromProto(icRow.findExport("vdd")));
                        ArcInst.makeInstance(wireProto, ep, 
                            schRows[r].findPortInstFromProto(icRow.findExport("gnd")), 
                            schRows[r+1].findPortInstFromProto(icRow.findExport("gnd")));
                    }
                    for (int c = 0; c < 16; c++) {
                        Export.newInst(schArray, schRows[0].findPortInstFromProto(icRow.findExport("BL[" + c + "]")), "BL[" + c + "]", ep);
                        Export.newInst(schArray, schRows[0].findPortInstFromProto(icRow.findExport("BLB[" + c + "]")), "BLB[" + c + "]", ep);
                    }
                    for (int r = 0; r < 16; r++) {
                        Export.newInst(schArray, schRows[r].findPortInstFromProto(icRow.findExport("WL")), "WL[" + r + "]", ep);
                    }
                    Export.newInst(schArray, schRows[0].findPortInstFromProto(icRow.findExport("vdd")), "vdd", ep);
                    Export.newInst(schArray, schRows[0].findPortInstFromProto(icRow.findExport("gnd")), "gnd", ep);

                    Cell icArray = ip.makeIconForCell(schArray, ep);
                    System.out.println("  SRAM_Array_16x16 created: sch, lay, ic");

                    // ==========================================
                    // 3. Task 6.3A: Precharge_Bank_16
                    // ==========================================
                    System.out.println("--- Building Precharge_Bank_16 ---");
                    Cell icPre = lib.findNodeProto("pre_charge{ic}");
                    Cell layPre = lib.findNodeProto("pre_charge{lay}");
                    
                    Cell schPreBank = Cell.makeInstance(lib, "Precharge_Bank_16{sch}");
                    Cell layPreBank = Cell.makeInstance(lib, "Precharge_Bank_16{lay}");
                    
                    double prePitch = 69.0;
                    NodeInst[] schPreInsts = new NodeInst[16];
                    NodeInst[] layPreInsts = new NodeInst[16];
                    for (int i = 0; i < 16; i++) {
                        schPreInsts[i] = NodeInst.makeInstance(icPre, new Point2D.Double(i * 20.0, 0), 0, 0, schPreBank);
                        layPreInsts[i] = NodeInst.makeInstance(layPre, new Point2D.Double(i * prePitch, 0), 0, 0, layPreBank);
                        
                        Export.newInst(schPreBank, schPreInsts[i].findPortInstFromProto(icPre.findExport("BL")), "BL[" + i + "]", ep);
                        Export.newInst(schPreBank, schPreInsts[i].findPortInstFromProto(icPre.findExport("BLB")), "BLB[" + i + "]", ep);
                        Export.newInst(layPreBank, layPreInsts[i].findPortInstFromProto(layPre.findExport("BL")), "BL[" + i + "]", ep);
                        Export.newInst(layPreBank, layPreInsts[i].findPortInstFromProto(layPre.findExport("BLB")), "BLB[" + i + "]", ep);
                    }
                    for (int i = 0; i < 15; i++) {
                        ArcInst.makeInstance(wireProto, ep, 
                            schPreInsts[i].findPortInstFromProto(icPre.findExport("PRE")), 
                            schPreInsts[i+1].findPortInstFromProto(icPre.findExport("PRE")));
                        ArcInst.makeInstance(wireProto, ep, 
                            schPreInsts[i].findPortInstFromProto(icPre.findExport("vdd")), 
                            schPreInsts[i+1].findPortInstFromProto(icPre.findExport("vdd")));
                    }
                    Export.newInst(schPreBank, schPreInsts[0].findPortInstFromProto(icPre.findExport("PRE")), "PRE_B", ep);
                    Export.newInst(schPreBank, schPreInsts[0].findPortInstFromProto(icPre.findExport("vdd")), "vdd", ep);

                    com.sun.electric.database.prototype.NodeProto gndSchProto = schPreBank.getTechnology().findNodeProto("Ground");
                    NodeInst gndSch = NodeInst.makeInstance(gndSchProto, new Point2D.Double(-20.0, 0), 0, 0, schPreBank);
                    Export.newInst(schPreBank, gndSch.findPortInstFromProto(gndSchProto.getPort(0)), "gnd", ep);

                    for (int i = 0; i < 15; i++) {
                        ArcInst aVdd = ArcInst.makeInstance(m1Proto, ep, 
                            layPreInsts[i].findPortInstFromProto(layPre.findExport("vdd")), 
                            layPreInsts[i+1].findPortInstFromProto(layPre.findExport("vdd")));
                        aVdd.setLambdaBaseWidth(4.0);
                        
                        ArcInst aPre = ArcInst.makeInstance(m2Proto, ep, 
                            layPreInsts[i].findPortInstFromProto(layPre.findExport("PRE")), 
                            layPreInsts[i+1].findPortInstFromProto(layPre.findExport("PRE")));
                        aPre.setLambdaBaseWidth(3.0);
                    }
                    Export.newInst(layPreBank, layPreInsts[0].findPortInstFromProto(layPre.findExport("PRE")), "PRE_B", ep);
                    Export.newInst(layPreBank, layPreInsts[0].findPortInstFromProto(layPre.findExport("vdd")), "vdd", ep);

                    com.sun.electric.database.prototype.NodeProto m1PinProto = layPreBank.getTechnology().findNodeProto("Metal-1-Pin");
                    NodeInst m1Pin = NodeInst.makeInstance(m1PinProto, new Point2D.Double(-60.0, -20.0), 3.0, 3.0, layPreBank);
                    Export.newInst(layPreBank, m1Pin.findPortInstFromProto(m1PinProto.getPort(0)), "gnd", ep);

                    Cell icPreBank = ip.makeIconForCell(schPreBank, ep);
                    System.out.println("  Precharge_Bank_16 created: sch, lay, ic");

                    // ==========================================
                    // 4. Task 6.3B: Col_Mux_Bank_16
                    // ==========================================
                    System.out.println("--- Building Col_Mux_Bank_16 ---");
                    Cell icMux = lib.findNodeProto("col_mux_1x{ic}");
                    Cell layMux = lib.findNodeProto("col_mux_1x{lay}");
                    
                    Cell schMuxBank = Cell.makeInstance(lib, "Col_Mux_Bank_16{sch}");
                    Cell layMuxBank = Cell.makeInstance(lib, "Col_Mux_Bank_16{lay}");

                    double muxPitch = 76.0;
                    NodeInst[] schMuxInsts = new NodeInst[16];
                    NodeInst[] layMuxInsts = new NodeInst[16];
                    for (int i = 0; i < 16; i++) {
                        schMuxInsts[i] = NodeInst.makeInstance(icMux, new Point2D.Double(i * 25.0, 0), 0, 0, schMuxBank);
                        layMuxInsts[i] = NodeInst.makeInstance(layMux, new Point2D.Double(i * muxPitch, 0), 0, 0, layMuxBank);
                        
                        Export.newInst(schMuxBank, schMuxInsts[i].findPortInstFromProto(icMux.findExport("BL")), "BL[" + i + "]", ep);
                        Export.newInst(schMuxBank, schMuxInsts[i].findPortInstFromProto(icMux.findExport("BLB")), "BLB[" + i + "]", ep);
                        Export.newInst(schMuxBank, schMuxInsts[i].findPortInstFromProto(icMux.findExport("COL_SEL")), "COL_SEL[" + i + "]", ep);
                        Export.newInst(schMuxBank, schMuxInsts[i].findPortInstFromProto(icMux.findExport("COL_SEL_B")), "COL_SEL_B[" + i + "]", ep);

                        Export.newInst(layMuxBank, layMuxInsts[i].findPortInstFromProto(layMux.findExport("BL")), "BL[" + i + "]", ep);
                        Export.newInst(layMuxBank, layMuxInsts[i].findPortInstFromProto(layMux.findExport("BLB")), "BLB[" + i + "]", ep);
                        Export.newInst(layMuxBank, layMuxInsts[i].findPortInstFromProto(layMux.findExport("COL_SEL")), "COL_SEL[" + i + "]", ep);
                        Export.newInst(layMuxBank, layMuxInsts[i].findPortInstFromProto(layMux.findExport("COL_SEL_B")), "COL_SEL_B[" + i + "]", ep);
                    }

                    // Schematic
                    for (int i = 0; i < 15; i++) {
                        ArcInst.makeInstance(wireProto, ep, 
                            schMuxInsts[i].findPortInstFromProto(icMux.findExport("GBL")), schMuxInsts[i+1].findPortInstFromProto(icMux.findExport("GBL")));
                        ArcInst.makeInstance(wireProto, ep, 
                            schMuxInsts[i].findPortInstFromProto(icMux.findExport("GBLB")), schMuxInsts[i+1].findPortInstFromProto(icMux.findExport("GBLB")));
                        ArcInst.makeInstance(wireProto, ep, 
                            schMuxInsts[i].findPortInstFromProto(icMux.findExport("vdd")), schMuxInsts[i+1].findPortInstFromProto(icMux.findExport("vdd")));
                        ArcInst.makeInstance(wireProto, ep, 
                            schMuxInsts[i].findPortInstFromProto(icMux.findExport("gnd")), schMuxInsts[i+1].findPortInstFromProto(icMux.findExport("gnd")));
                    }
                    Export.newInst(schMuxBank, schMuxInsts[0].findPortInstFromProto(icMux.findExport("GBL")), "GBL", ep);
                    Export.newInst(schMuxBank, schMuxInsts[0].findPortInstFromProto(icMux.findExport("GBLB")), "GBLB", ep);
                    Export.newInst(schMuxBank, schMuxInsts[0].findPortInstFromProto(icMux.findExport("vdd")), "vdd", ep);
                    Export.newInst(schMuxBank, schMuxInsts[0].findPortInstFromProto(icMux.findExport("gnd")), "gnd", ep);

                    // Layout shared power rails
                    for (int i = 0; i < 15; i++) {
                        ArcInst aVdd = ArcInst.makeInstance(m1Proto, ep, 
                            layMuxInsts[i].findPortInstFromProto(layMux.findExport("vdd")), 
                            layMuxInsts[i+1].findPortInstFromProto(layMux.findExport("vdd")));
                        aVdd.setLambdaBaseWidth(4.0);

                        ArcInst aGnd = ArcInst.makeInstance(m1Proto, ep, 
                            layMuxInsts[i].findPortInstFromProto(layMux.findExport("gnd")), 
                            layMuxInsts[i+1].findPortInstFromProto(layMux.findExport("gnd")));
                        aGnd.setLambdaBaseWidth(4.0);
                    }

                    // GBL: Via at (i*pitch - 5.0, 0.0), down to -45.0 on Metal-2
                    NodeInst[] gblMidVias = new NodeInst[16];
                    NodeInst[] gblBotVias = new NodeInst[16];
                    for (int i = 0; i < 16; i++) {
                        double xi = i * muxPitch - 5.0;
                        gblMidVias[i] = NodeInst.makeInstance(viaProto, new Point2D.Double(xi, 0.0), 5.0, 5.0, layMuxBank);
                        gblBotVias[i] = NodeInst.makeInstance(viaProto, new Point2D.Double(xi, -45.0), 5.0, 5.0, layMuxBank);
                        
                        ArcInst.makeInstance(m1Proto, ep, 
                            layMuxInsts[i].findPortInstFromProto(layMux.findExport("GBL")), 
                            gblMidVias[i].findPortInstFromProto(viaProto.getPort(0)));
                        
                        ArcInst.makeInstance(m2Proto, ep, 
                            gblMidVias[i].findPortInstFromProto(viaProto.getPort(0)), 
                            gblBotVias[i].findPortInstFromProto(viaProto.getPort(0)));
                    }
                    for (int i = 0; i < 15; i++) {
                        ArcInst.makeInstance(m2Proto, ep, 
                            gblBotVias[i].findPortInstFromProto(viaProto.getPort(0)), 
                            gblBotVias[i+1].findPortInstFromProto(viaProto.getPort(0)));
                    }

                    // GBLB: Via at (i*pitch + 35.0, 0.0), UP to +45.0 on Metal-2
                    NodeInst[] gblbMidVias = new NodeInst[16];
                    NodeInst[] gblbTopPins = new NodeInst[16];
                    for (int i = 0; i < 16; i++) {
                        double xi = i * muxPitch + 35.0;
                        gblbMidVias[i] = NodeInst.makeInstance(viaProto, new Point2D.Double(xi, 0.0), 5.0, 5.0, layMuxBank);
                        gblbTopPins[i] = NodeInst.makeInstance(viaProto, new Point2D.Double(xi, 45.0), 5.0, 5.0, layMuxBank);
                        
                        ArcInst.makeInstance(m1Proto, ep, 
                            layMuxInsts[i].findPortInstFromProto(layMux.findExport("GBLB")), 
                            gblbMidVias[i].findPortInstFromProto(viaProto.getPort(0)));
                        
                        ArcInst.makeInstance(m2Proto, ep, 
                            gblbMidVias[i].findPortInstFromProto(viaProto.getPort(0)), 
                            gblbTopPins[i].findPortInstFromProto(viaProto.getPort(0)));
                    }
                    for (int i = 0; i < 15; i++) {
                        ArcInst.makeInstance(m2Proto, ep, 
                            gblbTopPins[i].findPortInstFromProto(viaProto.getPort(0)), 
                            gblbTopPins[i+1].findPortInstFromProto(viaProto.getPort(0)));
                    }

                    Export.newInst(layMuxBank, gblBotVias[0].findPortInstFromProto(viaProto.getPort(0)), "GBL", ep);
                    Export.newInst(layMuxBank, gblbTopPins[0].findPortInstFromProto(viaProto.getPort(0)), "GBLB", ep);
                    Export.newInst(layMuxBank, layMuxInsts[0].findPortInstFromProto(layMux.findExport("vdd")), "vdd", ep);
                    Export.newInst(layMuxBank, layMuxInsts[0].findPortInstFromProto(layMux.findExport("gnd")), "gnd", ep);

                    Cell icMuxBank = ip.makeIconForCell(schMuxBank, ep);
                    System.out.println("  Col_Mux_Bank_16 created: sch, lay, ic");

                    // ==========================================
                    // Save Library
                    // ==========================================
                    System.out.println("--- Saving SRAM.jelib ---");
                    boolean saved = Output.saveJelib("SRAM.jelib", lib);
                    System.out.println("Output.saveJelib returned: " + saved);

                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(5000);
    }
}
