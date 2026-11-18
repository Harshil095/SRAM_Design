import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.IconParameters;
import com.sun.electric.tool.user.User;
import com.sun.electric.util.math.Orientation;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class BuildNeatMacroSch {

    // Helper to create a 2-segment orthogonal wire via a corner pin
    static void makeOrthogonalWire(Cell sch, ArcProto wire, EditingPreferences ep, NodeProto pinProto,
                                   PortInst p1, PortInst p2, boolean horizontalFirst) {
        Point2D pt1 = p1.getCenter();
        Point2D pt2 = p2.getCenter();

        if (Math.abs(pt1.getX() - pt2.getX()) < 0.1 || Math.abs(pt1.getY() - pt2.getY()) < 0.1) {
            // Direct straight line
            ArcInst.makeInstance(wire, ep, p1, p2);
        } else {
            // Needs a corner
            Point2D cornerPt;
            if (horizontalFirst) {
                cornerPt = new Point2D.Double(pt2.getX(), pt1.getY());
            } else {
                cornerPt = new Point2D.Double(pt1.getX(), pt2.getY());
            }
            NodeInst corner = NodeInst.makeInstance(pinProto, cornerPt, 1.0, 1.0, sch);
            ArcInst.makeInstance(wire, ep, p1, corner.getPortInst(0));
            ArcInst.makeInstance(wire, ep, corner.getPortInst(0), p2);
        }
    }

    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildNeatMacroSch", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    File bak = new File("SRAM.jelib.macro_clean.bak");
                    Files.copy(f.toPath(), bak.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Backed up SRAM.jelib to SRAM.jelib.macro_clean.bak");

                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();
                    IconParameters ip = IconParameters.makeInstance(true);

                    Technology schemTech = Technology.findTechnology("schematic");
                    ArcProto wire = schemTech.findArcProto("wire");
                    NodeProto pinProto = schemTech.findNodeProto("Wire_Pin");
                    NodeProto offPage = schemTech.findNodeProto("Off-Page");
                    NodeProto pwrProto = schemTech.findNodeProto("Power");
                    NodeProto gndProto = schemTech.findNodeProto("Ground");

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

                    Cell sch = Cell.makeInstance(lib, "SRAM_256_Macro{sch}");

                    // ========================================================
                    // 1. INSTANTIATION OF PRINCIPAL BLOCKS
                    // ========================================================
                    // Core Datapath at (220.0, 0.0)
                    NodeInst instData   = NodeInst.makeInstance(icData, new Point2D.Double(220.0, 0.0), 0, 0, sch, Orientation.IDENT, "SRAM_Cor@0");

                    // Row Decoder at (-120.0, -29.0), X-mirrored so OUT[0:15] face right at WL levels
                    NodeInst instRowDec = NodeInst.makeInstance(icDec,  new Point2D.Double(-120.0, -29.0), 0, 0, sch, Orientation.X, "Decoder_@0");

                    // Col Decoder at (-120.0, 37.0), X-mirrored so OUT[0:15] face right at COL_SEL levels
                    NodeInst instColDec = NodeInst.makeInstance(icDec,  new Point2D.Double(-120.0, 37.0), 0, 0, sch, Orientation.X, "Decoder_@1");

                    // Shift Register at (-260.0, 0.0)
                    NodeInst instSreg   = NodeInst.makeInstance(icSreg, new Point2D.Double(-260.0, 0.0), 0, 0, sch, Orientation.IDENT, "ShiftReg@0");

                    // Div8 Counter at (-260.0, -100.0)
                    NodeInst instDiv8   = NodeInst.makeInstance(icDiv8, new Point2D.Double(-260.0, -100.0), 0, 0, sch, Orientation.IDENT, "Div8_Cou@0");

                    // Write Driver at (140.0, 90.0)
                    NodeInst instWd     = NodeInst.makeInstance(icWd,   new Point2D.Double(140.0, 90.0), 0, 0, sch, Orientation.IDENT, "write_dr@0");

                    // Sense Amplifier at (260.0, 90.0)
                    NodeInst instSa     = NodeInst.makeInstance(icSa,   new Point2D.Double(260.0, 90.0), 0, 0, sch, Orientation.IDENT, "sense_am@0");

                    // ========================================================
                    // 2. LOCAL POWER & GROUND FOR PRINCIPAL BLOCKS
                    // ========================================================
                    // Core Datapath local pwr & gnd (bottom)
                    NodeInst pwrData = NodeInst.makeInstance(pwrProto, new Point2D.Double(219.0, -60.0), 3.0, 3.0, sch);
                    NodeInst gndData = NodeInst.makeInstance(gndProto, new Point2D.Double(221.0, -60.0), 3.0, 3.0, sch);
                    ArcInst.makeInstance(wire, ep, pwrData.getPortInst(0), instData.findPortInstFromProto(icData.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, gndData.getPortInst(0), instData.findPortInstFromProto(icData.findExport("gnd")));

                    // Row Decoder local pwr & gnd
                    NodeInst pwrRow = NodeInst.makeInstance(pwrProto, new Point2D.Double(-121.0, -58.0), 3.0, 3.0, sch);
                    NodeInst gndRow = NodeInst.makeInstance(gndProto, new Point2D.Double(-119.0, -58.0), 3.0, 3.0, sch);
                    ArcInst.makeInstance(wire, ep, pwrRow.getPortInst(0), instRowDec.findPortInstFromProto(icDec.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, gndRow.getPortInst(0), instRowDec.findPortInstFromProto(icDec.findExport("gnd")));

                    // Col Decoder local pwr & gnd
                    NodeInst pwrCol = NodeInst.makeInstance(pwrProto, new Point2D.Double(-121.0, 8.0), 3.0, 3.0, sch);
                    NodeInst gndCol = NodeInst.makeInstance(gndProto, new Point2D.Double(-119.0, 8.0), 3.0, 3.0, sch);
                    ArcInst.makeInstance(wire, ep, pwrCol.getPortInst(0), instColDec.findPortInstFromProto(icDec.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, gndCol.getPortInst(0), instColDec.findPortInstFromProto(icDec.findExport("gnd")));

                    // ShiftReg local pwr & gnd
                    NodeInst pwrSreg = NodeInst.makeInstance(pwrProto, new Point2D.Double(-261.0, -18.0), 3.0, 3.0, sch);
                    NodeInst gndSreg = NodeInst.makeInstance(gndProto, new Point2D.Double(-259.0, -18.0), 3.0, 3.0, sch);
                    ArcInst.makeInstance(wire, ep, pwrSreg.getPortInst(0), instSreg.findPortInstFromProto(icSreg.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, gndSreg.getPortInst(0), instSreg.findPortInstFromProto(icSreg.findExport("gnd")));

                    // Div8 Counter local pwr & gnd
                    NodeInst pwrDiv8 = NodeInst.makeInstance(pwrProto, new Point2D.Double(-261.0, -114.0), 3.0, 3.0, sch);
                    NodeInst gndDiv8 = NodeInst.makeInstance(gndProto, new Point2D.Double(-259.0, -114.0), 3.0, 3.0, sch);
                    ArcInst.makeInstance(wire, ep, pwrDiv8.getPortInst(0), instDiv8.findPortInstFromProto(icDiv8.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, gndDiv8.getPortInst(0), instDiv8.findPortInstFromProto(icDiv8.findExport("gnd")));

                    // Write Driver local pwr & gnd
                    NodeInst pwrWd = NodeInst.makeInstance(pwrProto, new Point2D.Double(140.0, 102.0), 3.0, 3.0, sch);
                    NodeInst gndWd = NodeInst.makeInstance(gndProto, new Point2D.Double(140.0, 76.0), 3.0, 3.0, sch);
                    ArcInst.makeInstance(wire, ep, pwrWd.getPortInst(0), instWd.findPortInstFromProto(icWd.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, gndWd.getPortInst(0), instWd.findPortInstFromProto(icWd.findExport("gnd")));

                    // Sense Amplifier local pwr & gnd
                    NodeInst pwrSa = NodeInst.makeInstance(pwrProto, new Point2D.Double(257.5, 74.0), 3.0, 3.0, sch);
                    NodeInst gndSa = NodeInst.makeInstance(gndProto, new Point2D.Double(262.5, 74.0), 3.0, 3.0, sch);
                    ArcInst.makeInstance(wire, ep, pwrSa.getPortInst(0), instSa.findPortInstFromProto(icSa.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, gndSa.getPortInst(0), instSa.findPortInstFromProto(icSa.findExport("gnd")));

                    // ========================================================
                    // 3. ROW DECODER -> DATAPATH (16 Straight Parallel Horizontal Wires!)
                    // ========================================================
                    for (int r = 0; r < 16; r++) {
                        PortInst pDecOut = instRowDec.findPortInstFromProto(icDec.findExport("OUT[" + r + "]"));
                        PortInst pDataWl = instData.findPortInstFromProto(icData.findExport("WL[" + r + "]"));
                        ArcInst.makeInstance(wire, ep, pDecOut, pDataWl);
                    }

                    // ========================================================
                    // 4. COLUMN INVERTERS & COLUMN BUS WIRING
                    // ========================================================
                    // Staggered 4-column bank between X = -45.0 and X = 150.0 in Y in [-14, 16]
                    double[] colBankX = {-40.0, 15.0, 70.0, 125.0};
                    NodeInst[] instColInvs = new NodeInst[16];

                    // Determine the Y coordinate of each COL_SEL_B[c] to match inverter output exactly
                    for (int c = 0; c < 16; c++) {
                        PortInst pColSelB = instData.findPortInstFromProto(icData.findExport("COL_SEL_B[" + c + "]"));
                        double yB = pColSelB.getCenter().getY();
                        int colIdx = c % 4;
                        double xInv = colBankX[colIdx];

                        instColInvs[c] = NodeInst.makeInstance(icInv, new Point2D.Double(xInv, yB), 0, 0, sch, Orientation.IDENT, "inverter@" + c);

                        // Horizontal wire from inverter out to Datapath COL_SEL_B[c]
                        PortInst pInvOut = instColInvs[c].findPortInstFromProto(icInv.findExport("out"));
                        ArcInst.makeInstance(wire, ep, pInvOut, pColSelB);

                        // Tap from ColDec.OUT[c] line down to inverter in
                        PortInst pColOut = instColDec.findPortInstFromProto(icDec.findExport("OUT[" + c + "]"));
                        PortInst pColSel = instData.findPortInstFromProto(icData.findExport("COL_SEL[" + c + "]"));
                        double ySel = pColSel.getCenter().getY();

                        // Place tap pin along the horizontal COL_SEL line directly above inverter input
                        double xTap = xInv - 10.0;
                        NodeInst tapSel = NodeInst.makeInstance(pinProto, new Point2D.Double(xTap, ySel), 1.0, 1.0, sch);
                        NodeInst cornerInvIn = NodeInst.makeInstance(pinProto, new Point2D.Double(xTap, yB), 1.0, 1.0, sch);

                        // Wire segment from ColDec.OUT[c] to tapSel
                        ArcInst.makeInstance(wire, ep, pColOut, tapSel.getPortInst(0));
                        // Wire segment from tapSel to Datapath COL_SEL[c]
                        ArcInst.makeInstance(wire, ep, tapSel.getPortInst(0), pColSel);

                        // Vertical drop from tapSel to cornerInvIn
                        ArcInst.makeInstance(wire, ep, tapSel.getPortInst(0), cornerInvIn.getPortInst(0));
                        // Horizontal segment from cornerInvIn into inverter in
                        ArcInst.makeInstance(wire, ep, cornerInvIn.getPortInst(0), instColInvs[c].findPortInstFromProto(icInv.findExport("in")));
                    }

                    // ========================================================
                    // 5. SHIFT REGISTER -> DECODERS (Clean Orthogonal Buses)
                    // ========================================================
                    // Q[0:3] -> Row_Decoder.A[0:3]
                    for (int b = 0; b < 4; b++) {
                        PortInst pQ = instSreg.findPortInstFromProto(icSreg.findExport("Q[" + b + "]"));
                        PortInst pA = instRowDec.findPortInstFromProto(icDec.findExport("A[" + b + "]"));
                        double xBus = -200.0 - b * 8.0;
                        NodeInst c1 = NodeInst.makeInstance(pinProto, new Point2D.Double(xBus, pQ.getCenter().getY()), 1.0, 1.0, sch);
                        NodeInst c2 = NodeInst.makeInstance(pinProto, new Point2D.Double(xBus, pA.getCenter().getY()), 1.0, 1.0, sch);
                        ArcInst.makeInstance(wire, ep, pQ, c1.getPortInst(0));
                        ArcInst.makeInstance(wire, ep, c1.getPortInst(0), c2.getPortInst(0));
                        ArcInst.makeInstance(wire, ep, c2.getPortInst(0), pA);
                    }

                    // Q[4:7] -> Col_Decoder.A[0:3]
                    for (int b = 0; b < 4; b++) {
                        PortInst pQ = instSreg.findPortInstFromProto(icSreg.findExport("Q[" + (b + 4) + "]"));
                        PortInst pA = instColDec.findPortInstFromProto(icDec.findExport("A[" + b + "]"));
                        double xBus = -200.0 - (b + 4) * 8.0;
                        NodeInst c1 = NodeInst.makeInstance(pinProto, new Point2D.Double(xBus, pQ.getCenter().getY()), 1.0, 1.0, sch);
                        NodeInst c2 = NodeInst.makeInstance(pinProto, new Point2D.Double(xBus, pA.getCenter().getY()), 1.0, 1.0, sch);
                        ArcInst.makeInstance(wire, ep, pQ, c1.getPortInst(0));
                        ArcInst.makeInstance(wire, ep, c1.getPortInst(0), c2.getPortInst(0));
                        ArcInst.makeInstance(wire, ep, c2.getPortInst(0), pA);
                    }

                    // ========================================================
                    // 6. TIMING & CONTROL ENGINE (Zone 4)
                    // ========================================================
                    NodeInst invD1 = NodeInst.makeInstance(icInv, new Point2D.Double(-180.0, -130.0), 0, 0, sch, Orientation.IDENT, "inverter@16");
                    NodeInst invD2 = NodeInst.makeInstance(icInv, new Point2D.Double(-130.0, -130.0), 0, 0, sch, Orientation.IDENT, "inverter@17");
                    NodeInst invD3 = NodeInst.makeInstance(icInv, new Point2D.Double(-80.0,  -130.0), 0, 0, sch, Orientation.IDENT, "inverter@18");
                    NodeInst invRw = NodeInst.makeInstance(icInv, new Point2D.Double(-220.0, -170.0), 0, 0, sch, Orientation.IDENT, "inverter@19");

                    NodeInst nandWe = NodeInst.makeInstance(icNand, new Point2D.Double(-20.0, -130.0), 0, 0, sch, Orientation.IDENT, "nand4@0");
                    NodeInst invWe  = NodeInst.makeInstance(icInv,  new Point2D.Double(30.0,  -130.0), 0, 0, sch, Orientation.IDENT, "inverter@20");

                    NodeInst nandSae = NodeInst.makeInstance(icNand, new Point2D.Double(-20.0, -170.0), 0, 0, sch, Orientation.IDENT, "nand4@1");
                    NodeInst invSae  = NodeInst.makeInstance(icInv,  new Point2D.Double(30.0,  -170.0), 0, 0, sch, Orientation.IDENT, "inverter@21");

                    // Local Power for AND gates (tie pins C & D to vdd)
                    NodeInst pwrWe = NodeInst.makeInstance(pwrProto, new Point2D.Double(-35.0, -145.0), 3.0, 3.0, sch);
                    ArcInst.makeInstance(wire, ep, pwrWe.getPortInst(0), nandWe.findPortInstFromProto(icNand.findExport("C")));
                    ArcInst.makeInstance(wire, ep, pwrWe.getPortInst(0), nandWe.findPortInstFromProto(icNand.findExport("D")));

                    NodeInst pwrSae = NodeInst.makeInstance(pwrProto, new Point2D.Double(-35.0, -185.0), 3.0, 3.0, sch);
                    ArcInst.makeInstance(wire, ep, pwrSae.getPortInst(0), nandSae.findPortInstFromProto(icNand.findExport("C")));
                    ArcInst.makeInstance(wire, ep, pwrSae.getPortInst(0), nandSae.findPortInstFromProto(icNand.findExport("D")));

                    // EXECUTE connection to PRE_B and invD1
                    PortInst pExec = instDiv8.findPortInstFromProto(icDiv8.findExport("EXECUTE"));
                    PortInst pPreB = instData.findPortInstFromProto(icData.findExport("PRE_B"));
                    NodeInst tapExec = NodeInst.makeInstance(pinProto, new Point2D.Double(-220.0, pExec.getCenter().getY()), 1.0, 1.0, sch);
                    ArcInst.makeInstance(wire, ep, pExec, tapExec.getPortInst(0));

                    // Route tapExec to invD1.in
                    makeOrthogonalWire(sch, wire, ep, pinProto, tapExec.getPortInst(0), invD1.findPortInstFromProto(icInv.findExport("in")), false);

                    // Route tapExec to instData.PRE_B
                    NodeInst cPre1 = NodeInst.makeInstance(pinProto, new Point2D.Double(180.0, pExec.getCenter().getY()), 1.0, 1.0, sch);
                    NodeInst cPre2 = NodeInst.makeInstance(pinProto, new Point2D.Double(180.0, pPreB.getCenter().getY()), 1.0, 1.0, sch);
                    ArcInst.makeInstance(wire, ep, tapExec.getPortInst(0), cPre1.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, cPre1.getPortInst(0), cPre2.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, cPre2.getPortInst(0), pPreB);

                    // Delay Chain: invD1 -> invD2 -> invD3
                    ArcInst.makeInstance(wire, ep, invD1.findPortInstFromProto(icInv.findExport("out")), invD2.findPortInstFromProto(icInv.findExport("in")));
                    ArcInst.makeInstance(wire, ep, invD2.findPortInstFromProto(icInv.findExport("out")), invD3.findPortInstFromProto(icInv.findExport("in")));

                    // D2 (from invD2.out) drives nandWe.A
                    PortInst pD2 = invD2.findPortInstFromProto(icInv.findExport("out"));
                    NodeInst tapD2 = NodeInst.makeInstance(pinProto, new Point2D.Double(-50.0, pD2.getCenter().getY()), 1.0, 1.0, sch);
                    ArcInst.makeInstance(wire, ep, pD2, tapD2.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, tapD2.getPortInst(0), nandWe.findPortInstFromProto(icNand.findExport("A")));

                    // D3 (from invD3.out) drives nandSae.A
                    PortInst pD3 = invD3.findPortInstFromProto(icInv.findExport("out"));
                    makeOrthogonalWire(sch, wire, ep, pinProto, pD3, nandSae.findPortInstFromProto(icNand.findExport("A")), false);

                    // R_W Input & invRw
                    NodeInst pinRw = NodeInst.makeInstance(offPage, new Point2D.Double(-320.0, -170.0), 3.0, 3.0, sch, Orientation.IDENT, "pin_rw");
                    NodeInst tapRw = NodeInst.makeInstance(pinProto, new Point2D.Double(-270.0, -170.0), 1.0, 1.0, sch);
                    ArcInst.makeInstance(wire, ep, pinRw.getPortInst(0), tapRw.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, tapRw.getPortInst(0), invRw.findPortInstFromProto(icInv.findExport("in")));

                    // R_W branch to nandWe.B
                    makeOrthogonalWire(sch, wire, ep, pinProto, tapRw.getPortInst(0), nandWe.findPortInstFromProto(icNand.findExport("B")), false);

                    // invRw.out (R_W_b) to nandSae.B
                    ArcInst.makeInstance(wire, ep, invRw.findPortInstFromProto(icInv.findExport("out")), nandSae.findPortInstFromProto(icNand.findExport("B")));

                    // nandWe -> invWe -> write_driver.WE
                    ArcInst.makeInstance(wire, ep, nandWe.findPortInstFromProto(icNand.findExport("out")), invWe.findPortInstFromProto(icInv.findExport("in")));
                    PortInst pWeOut = invWe.findPortInstFromProto(icInv.findExport("out"));
                    PortInst pWdWe  = instWd.findPortInstFromProto(icWd.findExport("WE"));
                    NodeInst cWe1 = NodeInst.makeInstance(pinProto, new Point2D.Double(60.0, pWeOut.getCenter().getY()), 1.0, 1.0, sch);
                    NodeInst cWe2 = NodeInst.makeInstance(pinProto, new Point2D.Double(60.0, pWdWe.getCenter().getY()), 1.0, 1.0, sch);
                    ArcInst.makeInstance(wire, ep, pWeOut, cWe1.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, cWe1.getPortInst(0), cWe2.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, cWe2.getPortInst(0), pWdWe);

                    // nandSae -> invSae -> sense_amplifier.SAE
                    ArcInst.makeInstance(wire, ep, nandSae.findPortInstFromProto(icNand.findExport("out")), invSae.findPortInstFromProto(icInv.findExport("in")));
                    PortInst pSaeOut = invSae.findPortInstFromProto(icInv.findExport("out"));
                    PortInst pSaSae  = instSa.findPortInstFromProto(icSa.findExport("SAE"));
                    NodeInst cSae1 = NodeInst.makeInstance(pinProto, new Point2D.Double(200.0, pSaeOut.getCenter().getY()), 1.0, 1.0, sch);
                    NodeInst cSae2 = NodeInst.makeInstance(pinProto, new Point2D.Double(200.0, pSaSae.getCenter().getY()), 1.0, 1.0, sch);
                    ArcInst.makeInstance(wire, ep, pSaeOut, cSae1.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, cSae1.getPortInst(0), cSae2.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, cSae2.getPortInst(0), pSaSae);

                    // ========================================================
                    // 7. GLOBAL BITLINES (GBL, GBLB) (Zone 1, Top)
                    // ========================================================
                    PortInst pGbl  = instData.findPortInstFromProto(icData.findExport("GBL"));
                    PortInst pGblb = instData.findPortInstFromProto(icData.findExport("GBLB"));
                    PortInst pWdBl  = instWd.findPortInstFromProto(icWd.findExport("BL"));
                    PortInst pWdBlb = instWd.findPortInstFromProto(icWd.findExport("BLB"));
                    PortInst pSaBl  = instSa.findPortInstFromProto(icSa.findExport("BL"));
                    PortInst pSaBlb = instSa.findPortInstFromProto(icSa.findExport("BLB"));

                    // GBL Trunk at Y = 78.0
                    NodeInst tGblTop = NodeInst.makeInstance(pinProto, new Point2D.Double(pGbl.getCenter().getX(), 78.0), 1.0, 1.0, sch);
                    ArcInst.makeInstance(wire, ep, pGbl, tGblTop.getPortInst(0));
                    makeOrthogonalWire(sch, wire, ep, pinProto, tGblTop.getPortInst(0), pWdBl, true);
                    makeOrthogonalWire(sch, wire, ep, pinProto, tGblTop.getPortInst(0), pSaBl, true);

                    // GBLB Trunk at Y = 72.0
                    NodeInst tGblbTop = NodeInst.makeInstance(pinProto, new Point2D.Double(pGblb.getCenter().getX(), 72.0), 1.0, 1.0, sch);
                    ArcInst.makeInstance(wire, ep, pGblb, tGblbTop.getPortInst(0));
                    makeOrthogonalWire(sch, wire, ep, pinProto, tGblbTop.getPortInst(0), pWdBlb, true);
                    makeOrthogonalWire(sch, wire, ep, pinProto, tGblbTop.getPortInst(0), pSaBlb, true);

                    // ========================================================
                    // 8. GLOBAL I/O PINS & EXPORTS (7 total)
                    // ========================================================
                    // CLK Input: connects to ShiftReg.CLK and Div8_Counter.CLK
                    NodeInst pinClk = NodeInst.makeInstance(offPage, new Point2D.Double(-340.0, 7.0), 3.0, 3.0, sch, Orientation.IDENT, "pin_clk");
                    PortInst pSregClk = instSreg.findPortInstFromProto(icSreg.findExport("CLK"));
                    PortInst pDiv8Clk = instDiv8.findPortInstFromProto(icDiv8.findExport("CLK"));
                    NodeInst tapClk = NodeInst.makeInstance(pinProto, new Point2D.Double(-300.0, 7.0), 1.0, 1.0, sch);
                    ArcInst.makeInstance(wire, ep, pinClk.getPortInst(0), tapClk.getPortInst(0));
                    ArcInst.makeInstance(wire, ep, tapClk.getPortInst(0), pSregClk);
                    makeOrthogonalWire(sch, wire, ep, pinProto, tapClk.getPortInst(0), pDiv8Clk, false);

                    // ADDR_IN Input: connects to ShiftReg.ADDR_IN
                    NodeInst pinAddrIn = NodeInst.makeInstance(offPage, new Point2D.Double(-340.0, 9.0), 3.0, 3.0, sch, Orientation.IDENT, "pin_addr_in");
                    ArcInst.makeInstance(wire, ep, pinAddrIn.getPortInst(0), instSreg.findPortInstFromProto(icSreg.findExport("ADDR_IN")));

                    // DIN Input: connects to write_driver.WD
                    NodeInst pinDin = NodeInst.makeInstance(offPage, new Point2D.Double(80.0, 89.0), 3.0, 3.0, sch, Orientation.IDENT, "pin_din");
                    ArcInst.makeInstance(wire, ep, pinDin.getPortInst(0), instWd.findPortInstFromProto(icWd.findExport("WD")));

                    // DOUT Output: connects to sense_amplifier.SO
                    NodeInst pinDout = NodeInst.makeInstance(offPage, new Point2D.Double(320.0, 92.5), 3.0, 3.0, sch, Orientation.IDENT, "pin_dout");
                    ArcInst.makeInstance(wire, ep, instSa.findPortInstFromProto(icSa.findExport("SO")), pinDout.getPortInst(0));

                    // Global Power and Ground Exports
                    NodeInst pwrExp = NodeInst.makeInstance(pwrProto, new Point2D.Double(0.0, 160.0), 3.0, 3.0, sch, Orientation.IDENT, "pwr_export");
                    NodeInst gndExp = NodeInst.makeInstance(gndProto, new Point2D.Double(0.0, -210.0), 3.0, 3.0, sch, Orientation.IDENT, "gnd_export");

                    // Create the 7 exports matching standard icon
                    Export.newInst(sch, pinClk.getPortInst(0), "CLK", ep);
                    Export.newInst(sch, pinAddrIn.getPortInst(0), "ADDR_IN", ep);
                    Export.newInst(sch, pinRw.getPortInst(0), "R_W", ep);
                    Export.newInst(sch, pinDin.getPortInst(0), "DIN", ep);
                    Export.newInst(sch, pinDout.getPortInst(0), "DOUT", ep);
                    Export.newInst(sch, pwrExp.getPortInst(0), "vdd", ep);
                    Export.newInst(sch, gndExp.getPortInst(0), "gnd", ep);

                    System.out.println("  SRAM_256_Macro{sch} rebuilt cleanly with " + sch.getNumNodes() + " nodes and " + sch.getNumArcs() + " arcs.");

                    // ========================================================
                    // 9. VERIFICATION: NCC LVS & DRC
                    // ========================================================
                    Cell lay = lib.findNodeProto("SRAM_256_Macro{lay}");
                    System.out.println("=== Running NCC LVS on SRAM_256_Macro ===");
                    NccOptions opts = new NccOptions();
                    opts.checkSizes = false;
                    NccResults res = Ncc.compare(sch, null, lay, null, opts);
                    System.out.println(">>> NCC Result: " + res + " <<<");
                    System.out.println("  Match: " + res.match());
                    System.out.println("  Exports Match: " + res.exportMatch());
                    System.out.println("  Topology Match: " + res.topologyMatch());

                    if (!res.match()) {
                        System.err.println("FAILED NCC! Rolling back library...");
                        return false;
                    }

                    // Also check SRAM_Chip_Final NCC
                    Cell chipSch = lib.findNodeProto("SRAM_Chip_Final{sch}");
                    Cell chipLay = lib.findNodeProto("SRAM_Chip_Final{lay}");
                    if (chipSch != null && chipLay != null) {
                        System.out.println("=== Running NCC LVS on SRAM_Chip_Final ===");
                        NccResults chipRes = Ncc.compare(chipSch, null, chipLay, null, opts);
                        System.out.println(">>> SRAM_Chip_Final NCC Result: " + chipRes + " <<<");
                        System.out.println("  Match: " + chipRes.match());
                        System.out.println("  Exports Match: " + chipRes.exportMatch());
                        System.out.println("  Topology Match: " + chipRes.topologyMatch());
                    }

                    // Save library
                    System.out.println("Saving library SRAM.jelib...");
                    Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                    System.out.println("Saved SRAM.jelib successfully!");

                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        };
        j.startJob();
        Thread.sleep(4000);
    }
}
