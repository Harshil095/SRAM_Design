import java.awt.geom.Point2D;
import java.io.File;
import java.util.Iterator;
import java.util.List;
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

public class BuildFinalChip {

    static Cell[] buildMultiFinger(Library lib, String baseName, int numFingers, EditingPreferences ep) throws Exception {
        Cell invSch = lib.findNodeProto("inverter{sch}");
        Cell invIc  = lib.findNodeProto("inverter{ic}");
        Cell invLay = lib.findNodeProto("inverter{lay}");

        // 1. Schematic
        Cell sch = Cell.makeInstance(lib, baseName + "{sch}");
        ArcProto wire = sch.getTechnology().findArcProto("wire");
        NodeProto offPage = sch.getTechnology().findNodeProto("Off-Page");
        NodeProto pwrProto = sch.getTechnology().findNodeProto("Power");
        NodeProto gndProto = sch.getTechnology().findNodeProto("Ground");

        NodeInst pIn = NodeInst.makeInstance(offPage, new Point2D.Double(-20.0, 0.0), 0, 0, sch);
        NodeInst pOut = NodeInst.makeInstance(offPage, new Point2D.Double(20.0, 0.0), 0, 0, sch);
        NodeInst pPwr = NodeInst.makeInstance(pwrProto, new Point2D.Double(0.0, 20.0), 0, 0, sch);
        NodeInst pGnd = NodeInst.makeInstance(gndProto, new Point2D.Double(0.0, -20.0), 0, 0, sch);

        NodeInst[] instsSch = new NodeInst[numFingers];
        for (int i = 0; i < numFingers; i++) {
            instsSch[i] = NodeInst.makeInstance(invIc, new Point2D.Double(0.0, (i - numFingers / 2.0) * 10.0), 0, 0, sch);
            ArcInst.makeInstance(wire, ep, pIn.getPortInst(0), instsSch[i].findPortInstFromProto(invIc.findExport("in")));
            ArcInst.makeInstance(wire, ep, pOut.getPortInst(0), instsSch[i].findPortInstFromProto(invIc.findExport("out")));
        }
        Export.newInst(sch, pIn.getPortInst(0), "in", ep);
        Export.newInst(sch, pOut.getPortInst(0), "out", ep);
        Export.newInst(sch, pPwr.getPortInst(0), "vdd", ep);
        Export.newInst(sch, pGnd.getPortInst(0), "gnd", ep);

        IconParameters ip = IconParameters.makeInstance(true);
        Cell ic = ip.makeIconForCell(sch, ep);

        // 2. Layout
        Cell lay = Cell.makeInstance(lib, baseName + "{lay}");
        ArcProto m1 = lay.getTechnology().findArcProto("Metal-1");
        ArcProto m2 = lay.getTechnology().findArcProto("Metal-2");
        NodeProto via12Proto = lay.getTechnology().findNodeProto("Metal-1-Metal-2-Con");
        NodeProto m2Pin = lay.getTechnology().findNodeProto("Metal-2-Pin");

        NodeInst[] instsLay = new NodeInst[numFingers];
        double pitch = 50.0;
        for (int i = 0; i < numFingers; i++) {
            instsLay[i] = NodeInst.makeInstance(invLay, new Point2D.Double(i * pitch, 0.0), 0, 0, lay);
        }

        // Input bus at Y = 38.0 on Metal-2
        NodeInst[] jIn_top = new NodeInst[numFingers];
        for (int i = 0; i < numFingers; i++) {
            PortInst pInLay = instsLay[i].findPortInstFromProto(invLay.findExport("in"));
            NodeInst v12_i = NodeInst.makeInstance(via12Proto, new Point2D.Double(i * pitch - 25.0, -1.0), 5.0, 5.0, lay);
            jIn_top[i] = NodeInst.makeInstance(m2Pin, new Point2D.Double(i * pitch - 25.0, 38.0), 3.0, 3.0, lay);
            ArcInst.makeInstance(m1, ep, pInLay, v12_i.getPortInst(0));
            ArcInst.makeInstance(m2, ep, v12_i.getPortInst(0), jIn_top[i].getPortInst(0));
        }
        for (int i = 0; i < numFingers - 1; i++) {
            ArcInst.makeInstance(m2, ep, jIn_top[i].getPortInst(0), jIn_top[i + 1].getPortInst(0));
        }

        // Output bus at Y = -38.0 on Metal-2
        NodeInst[] jOut_bot = new NodeInst[numFingers];
        for (int i = 0; i < numFingers; i++) {
            PortInst pOutLay = instsLay[i].findPortInstFromProto(invLay.findExport("out"));
            NodeInst v12_o = NodeInst.makeInstance(via12Proto, new Point2D.Double(i * pitch + 8.0, -1.5), 5.0, 5.0, lay);
            jOut_bot[i] = NodeInst.makeInstance(m2Pin, new Point2D.Double(i * pitch + 8.0, -38.0), 3.0, 3.0, lay);
            ArcInst.makeInstance(m1, ep, pOutLay, v12_o.getPortInst(0));
            ArcInst.makeInstance(m2, ep, v12_o.getPortInst(0), jOut_bot[i].getPortInst(0));
        }
        for (int i = 0; i < numFingers - 1; i++) {
            ArcInst.makeInstance(m2, ep, jOut_bot[i].getPortInst(0), jOut_bot[i + 1].getPortInst(0));
        }

        // Connect VDD rails (Metal-1 at Y = 21.5)
        PortInst prevVdd = instsLay[0].findPortInstFromProto(invLay.findExport("vdd"));
        for (int i = 1; i < numFingers; i++) {
            PortInst curVdd = instsLay[i].findPortInstFromProto(invLay.findExport("vdd"));
            ArcInst.makeInstance(m1, ep, prevVdd, curVdd);
            prevVdd = curVdd;
        }

        // Connect GND rails (Metal-1 at Y = -23.0)
        PortInst prevGnd = instsLay[0].findPortInstFromProto(invLay.findExport("gnd"));
        for (int i = 1; i < numFingers; i++) {
            PortInst curGnd = instsLay[i].findPortInstFromProto(invLay.findExport("gnd"));
            ArcInst.makeInstance(m1, ep, prevGnd, curGnd);
            prevGnd = curGnd;
        }

        // Export ports
        Export.newInst(lay, jIn_top[0].getPortInst(0), "in", ep);
        Export.newInst(lay, jOut_bot[numFingers - 1].getPortInst(0), "out", ep);
        Export.newInst(lay, instsLay[0].findPortInstFromProto(invLay.findExport("vdd")), "vdd", ep);
        Export.newInst(lay, instsLay[0].findPortInstFromProto(invLay.findExport("gnd")), "gnd", ep);

        return new Cell[]{sch, ic, lay};
    }

    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildFinalChip", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), new File("SRAM.jelib").toURI().toURL(), null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();
                    IconParameters ip = IconParameters.makeInstance(true);

                    // ==========================================
                    // 1. Build Multi-Finger Subcells
                    // ==========================================
                    System.out.println("=== 1. Building Multi-Finger Inverters (inv_3x, inv_9x, inv_27x) ===");
                    Cell[] c3  = buildMultiFinger(lib, "inv_3x",  3,  ep);
                    Cell[] c9  = buildMultiFinger(lib, "inv_9x",  9,  ep);
                    Cell[] c27 = buildMultiFinger(lib, "inv_27x", 27, ep);

                    Cell invSch = lib.findNodeProto("inverter{sch}");
                    Cell invIc  = lib.findNodeProto("inverter{ic}");
                    Cell invLay = lib.findNodeProto("inverter{lay}");

                    ArcProto wire = invSch.getTechnology().findArcProto("wire");
                    NodeProto offPage = invSch.getTechnology().findNodeProto("Off-Page");
                    NodeProto pwrProto = invSch.getTechnology().findNodeProto("Power");
                    NodeProto gndProto = invSch.getTechnology().findNodeProto("Ground");

                    // ==========================================
                    // 2. Build Input_Pad
                    // ==========================================
                    System.out.println("=== 2. Building Input_Pad ===");
                    Cell inPadSch = Cell.makeInstance(lib, "Input_Pad{sch}");
                    NodeInst pinInPadIn = NodeInst.makeInstance(offPage, new Point2D.Double(-40.0, 0.0), 0, 0, inPadSch);
                    NodeInst pinInPadOut = NodeInst.makeInstance(offPage, new Point2D.Double(40.0, 0.0), 0, 0, inPadSch);
                    NodeInst pwrInPad = NodeInst.makeInstance(pwrProto, new Point2D.Double(0.0, 20.0), 0, 0, inPadSch);
                    NodeInst gndInPad = NodeInst.makeInstance(gndProto, new Point2D.Double(0.0, -20.0), 0, 0, inPadSch);

                    NodeInst instSt1 = NodeInst.makeInstance(invIc, new Point2D.Double(-20.0, 0.0), 0, 0, inPadSch);
                    NodeInst instSt2 = NodeInst.makeInstance(c3[1], new Point2D.Double(20.0, 0.0), 0, 0, inPadSch);

                    ArcInst.makeInstance(wire, ep, pinInPadIn.getPortInst(0), instSt1.findPortInstFromProto(invIc.findExport("in")));
                    ArcInst.makeInstance(wire, ep, instSt1.findPortInstFromProto(invIc.findExport("out")), instSt2.findPortInstFromProto(c3[1].findExport("in")));
                    ArcInst.makeInstance(wire, ep, instSt2.findPortInstFromProto(c3[1].findExport("out")), pinInPadOut.getPortInst(0));

                    Export.newInst(inPadSch, pinInPadIn.getPortInst(0), "PAD_IN", ep);
                    Export.newInst(inPadSch, pinInPadOut.getPortInst(0), "CHIP_OUT", ep);
                    Export.newInst(inPadSch, pwrInPad.getPortInst(0), "vdd", ep);
                    Export.newInst(inPadSch, gndInPad.getPortInst(0), "gnd", ep);

                    Cell inPadIc = ip.makeIconForCell(inPadSch, ep);

                    // Layout of Input_Pad
                    Cell inPadLay = Cell.makeInstance(lib, "Input_Pad{lay}");
                    ArcProto m1 = inPadLay.getTechnology().findArcProto("Metal-1");
                    ArcProto m2 = inPadLay.getTechnology().findArcProto("Metal-2");
                    ArcProto m3 = inPadLay.getTechnology().findArcProto("Metal-3");
                    NodeProto via12Proto = inPadLay.getTechnology().findNodeProto("Metal-1-Metal-2-Con");
                    NodeProto via23Proto = inPadLay.getTechnology().findNodeProto("Metal-2-Metal-3-Con");
                    NodeProto m1Pin = inPadLay.getTechnology().findNodeProto("Metal-1-Pin");
                    NodeProto m2Pin = inPadLay.getTechnology().findNodeProto("Metal-2-Pin");
                    NodeProto m3Pin = inPadLay.getTechnology().findNodeProto("Metal-3-Pin");

                    NodeInst lSt1 = NodeInst.makeInstance(invLay, new Point2D.Double(0.0, 0.0), 0, 0, inPadLay);
                    NodeInst lSt2 = NodeInst.makeInstance(c3[2],  new Point2D.Double(60.0, 0.0), 0, 0, inPadLay);

                    // PAD_IN exported on Metal-3-Pin at (-45.0, -1.0)
                    // Placed at X = -35.0 to give >= 3 lambda clearance to inverter contact at -25.0
                    NodeInst v12_padin = NodeInst.makeInstance(via12Proto, new Point2D.Double(-35.0, -1.0), 5.0, 5.0, inPadLay);
                    NodeInst v23_padin = NodeInst.makeInstance(via23Proto, new Point2D.Double(-35.0, -1.0), 6.0, 6.0, inPadLay);
                    NodeInst pinPadInLay = NodeInst.makeInstance(m3Pin, new Point2D.Double(-45.0, -1.0), 3.0, 3.0, inPadLay);
                    ArcInst aInM1 = ArcInst.makeInstance(m1, ep, lSt1.findPortInstFromProto(invLay.findExport("in")), v12_padin.getPortInst(0));
                    aInM1.setLambdaBaseWidth(4.0);
                    ArcInst.makeInstance(m2, ep, v12_padin.getPortInst(0), v23_padin.getPortInst(0));
                    ArcInst.makeInstance(m3, ep, v23_padin.getPortInst(0), pinPadInLay.getPortInst(0));

                    // St1 out to St2 in
                    PortInst pSt1Out = lSt1.findPortInstFromProto(invLay.findExport("out"));
                    PortInst pSt2In  = lSt2.findPortInstFromProto(c3[2].findExport("in"));
                    NodeInst v12_st1_o = NodeInst.makeInstance(via12Proto, new Point2D.Double(8.0, -1.5), 5.0, 5.0, inPadLay);
                    NodeInst jSt1_m2   = NodeInst.makeInstance(m2Pin, new Point2D.Double(8.0, 38.0), 3.0, 3.0, inPadLay);
                    ArcInst.makeInstance(m1, ep, pSt1Out, v12_st1_o.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_st1_o.getPortInst(0), jSt1_m2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, jSt1_m2.getPortInst(0), pSt2In);

                    // VDD and GND rails
                    ArcInst.makeInstance(m1, ep, lSt1.findPortInstFromProto(invLay.findExport("vdd")), lSt2.findPortInstFromProto(c3[2].findExport("vdd")));
                    ArcInst.makeInstance(m1, ep, lSt1.findPortInstFromProto(invLay.findExport("gnd")), lSt2.findPortInstFromProto(c3[2].findExport("gnd")));

                    PortInst pSt2Out = lSt2.findPortInstFromProto(c3[2].findExport("out"));
                    Export.newInst(inPadLay, pinPadInLay.getPortInst(0), "PAD_IN", ep);
                    Export.newInst(inPadLay, pSt2Out, "CHIP_OUT", ep);
                    Export.newInst(inPadLay, lSt1.findPortInstFromProto(invLay.findExport("vdd")), "vdd", ep);
                    Export.newInst(inPadLay, lSt1.findPortInstFromProto(invLay.findExport("gnd")), "gnd", ep);

                    // ==========================================
                    // 3. Build Output_Pad
                    // ==========================================
                    System.out.println("=== 3. Building Output_Pad ===");
                    Cell outPadSch = Cell.makeInstance(lib, "Output_Pad{sch}");
                    NodeInst pinChipIn = NodeInst.makeInstance(offPage, new Point2D.Double(-80.0, 0.0), 0, 0, outPadSch);
                    NodeInst pinPadOut = NodeInst.makeInstance(offPage, new Point2D.Double(80.0, 0.0), 0, 0, outPadSch);
                    NodeInst pwrOutPad = NodeInst.makeInstance(pwrProto, new Point2D.Double(0.0, 20.0), 0, 0, outPadSch);
                    NodeInst gndOutPad = NodeInst.makeInstance(gndProto, new Point2D.Double(0.0, -20.0), 0, 0, outPadSch);

                    NodeInst oSt1 = NodeInst.makeInstance(invIc,  new Point2D.Double(-60.0, 0.0), 0, 0, outPadSch);
                    NodeInst oSt2 = NodeInst.makeInstance(c3[1],  new Point2D.Double(-20.0, 0.0), 0, 0, outPadSch);
                    NodeInst oSt3 = NodeInst.makeInstance(c9[1],  new Point2D.Double(20.0, 0.0), 0, 0, outPadSch);
                    NodeInst oSt4 = NodeInst.makeInstance(c27[1], new Point2D.Double(60.0, 0.0), 0, 0, outPadSch);

                    ArcInst.makeInstance(wire, ep, pinChipIn.getPortInst(0), oSt1.findPortInstFromProto(invIc.findExport("in")));
                    ArcInst.makeInstance(wire, ep, oSt1.findPortInstFromProto(invIc.findExport("out")), oSt2.findPortInstFromProto(c3[1].findExport("in")));
                    ArcInst.makeInstance(wire, ep, oSt2.findPortInstFromProto(c3[1].findExport("out")), oSt3.findPortInstFromProto(c9[1].findExport("in")));
                    ArcInst.makeInstance(wire, ep, oSt3.findPortInstFromProto(c9[1].findExport("out")), oSt4.findPortInstFromProto(c27[1].findExport("in")));
                    ArcInst.makeInstance(wire, ep, oSt4.findPortInstFromProto(c27[1].findExport("out")), pinPadOut.getPortInst(0));

                    Export.newInst(outPadSch, pinChipIn.getPortInst(0), "CHIP_IN", ep);
                    Export.newInst(outPadSch, pinPadOut.getPortInst(0), "PAD_OUT", ep);
                    Export.newInst(outPadSch, pwrOutPad.getPortInst(0), "vdd", ep);
                    Export.newInst(outPadSch, gndOutPad.getPortInst(0), "gnd", ep);

                    Cell outPadIc = ip.makeIconForCell(outPadSch, ep);

                    // Layout of Output_Pad
                    Cell outPadLay = Cell.makeInstance(lib, "Output_Pad{lay}");
                    double xSt1 = 0.0;
                    double xSt2 = 60.0;
                    double xSt3 = 260.0;
                    double xSt4 = 760.0;

                    NodeInst loSt1 = NodeInst.makeInstance(invLay, new Point2D.Double(xSt1, 0.0), 0, 0, outPadLay);
                    NodeInst loSt2 = NodeInst.makeInstance(c3[2],  new Point2D.Double(xSt2, 0.0), 0, 0, outPadLay);
                    NodeInst loSt3 = NodeInst.makeInstance(c9[2],  new Point2D.Double(xSt3, 0.0), 0, 0, outPadLay);
                    NodeInst loSt4 = NodeInst.makeInstance(c27[2], new Point2D.Double(xSt4, 0.0), 0, 0, outPadLay);

                    // CHIP_IN at (-40.0, -1.0)
                    NodeInst pinChipInLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-40.0, -1.0), 3.0, 3.0, outPadLay);
                    ArcInst.makeInstance(m1, ep, pinChipInLay.getPortInst(0), loSt1.findPortInstFromProto(invLay.findExport("in")));

                    // St1 out to St2 in
                    PortInst poSt1Out = loSt1.findPortInstFromProto(invLay.findExport("out"));
                    PortInst poSt2In  = loSt2.findPortInstFromProto(c3[2].findExport("in"));
                    NodeInst vo12_st1_o = NodeInst.makeInstance(via12Proto, new Point2D.Double(xSt1 + 8.0, -1.5), 5.0, 5.0, outPadLay);
                    NodeInst joSt1_m2   = NodeInst.makeInstance(m2Pin, new Point2D.Double(xSt1 + 8.0, 38.0), 3.0, 3.0, outPadLay);
                    ArcInst.makeInstance(m1, ep, poSt1Out, vo12_st1_o.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, vo12_st1_o.getPortInst(0), joSt1_m2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, joSt1_m2.getPortInst(0), poSt2In);

                    // St2 out to St3 in
                    PortInst poSt2Out = loSt2.findPortInstFromProto(c3[2].findExport("out"));
                    PortInst poSt3In  = loSt3.findPortInstFromProto(c9[2].findExport("in"));
                    double xGap23 = 200.0;
                    NodeInst joSt2_mid1 = NodeInst.makeInstance(m2Pin, new Point2D.Double(xGap23, -38.0), 3.0, 3.0, outPadLay);
                    NodeInst joSt2_mid2 = NodeInst.makeInstance(m2Pin, new Point2D.Double(xGap23, 38.0), 3.0, 3.0, outPadLay);
                    ArcInst.makeInstance(m2, ep, poSt2Out, joSt2_mid1.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, joSt2_mid1.getPortInst(0), joSt2_mid2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, joSt2_mid2.getPortInst(0), poSt3In);

                    // St3 out to St4 in
                    PortInst poSt3Out = loSt3.findPortInstFromProto(c9[2].findExport("out"));
                    PortInst poSt4In  = loSt4.findPortInstFromProto(c27[2].findExport("in"));
                    double xGap34 = 700.0;
                    NodeInst joSt3_mid1 = NodeInst.makeInstance(m2Pin, new Point2D.Double(xGap34, -38.0), 3.0, 3.0, outPadLay);
                    NodeInst joSt3_mid2 = NodeInst.makeInstance(m2Pin, new Point2D.Double(xGap34, 38.0), 3.0, 3.0, outPadLay);
                    ArcInst.makeInstance(m2, ep, poSt3Out, joSt3_mid1.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, joSt3_mid1.getPortInst(0), joSt3_mid2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, joSt3_mid2.getPortInst(0), poSt4In);

                    // St4 out exported on Metal-3-Pin at (2068.0, -50.0)
                    PortInst poSt4Out = loSt4.findPortInstFromProto(c27[2].findExport("out"));
                    NodeInst vo23_padout = NodeInst.makeInstance(via23Proto, new Point2D.Double(2068.0, -38.0), 6.0, 6.0, outPadLay);
                    NodeInst pinPadOutLay = NodeInst.makeInstance(m3Pin, new Point2D.Double(2068.0, -50.0), 3.0, 3.0, outPadLay);
                    ArcInst.makeInstance(m2, ep, poSt4Out, vo23_padout.getPortInst(0));
                    ArcInst.makeInstance(m3, ep, vo23_padout.getPortInst(0), pinPadOutLay.getPortInst(0));

                    // VDD and GND rails
                    ArcInst.makeInstance(m1, ep, loSt1.findPortInstFromProto(invLay.findExport("vdd")), loSt2.findPortInstFromProto(c3[2].findExport("vdd")));
                    ArcInst.makeInstance(m1, ep, loSt2.findPortInstFromProto(c3[2].findExport("vdd")),  loSt3.findPortInstFromProto(c9[2].findExport("vdd")));
                    ArcInst.makeInstance(m1, ep, loSt3.findPortInstFromProto(c9[2].findExport("vdd")),  loSt4.findPortInstFromProto(c27[2].findExport("vdd")));

                    ArcInst.makeInstance(m1, ep, loSt1.findPortInstFromProto(invLay.findExport("gnd")), loSt2.findPortInstFromProto(c3[2].findExport("gnd")));
                    ArcInst.makeInstance(m1, ep, loSt2.findPortInstFromProto(c3[2].findExport("gnd")),  loSt3.findPortInstFromProto(c9[2].findExport("gnd")));
                    ArcInst.makeInstance(m1, ep, loSt3.findPortInstFromProto(c9[2].findExport("gnd")),  loSt4.findPortInstFromProto(c27[2].findExport("gnd")));

                    Export.newInst(outPadLay, pinChipInLay.getPortInst(0), "CHIP_IN", ep);
                    Export.newInst(outPadLay, pinPadOutLay.getPortInst(0), "PAD_OUT", ep);
                    Export.newInst(outPadLay, loSt1.findPortInstFromProto(invLay.findExport("vdd")), "vdd", ep);
                    Export.newInst(outPadLay, loSt1.findPortInstFromProto(invLay.findExport("gnd")), "gnd", ep);

                    // ==========================================
                    // 4. Build SRAM_Chip_Final{sch}
                    // ==========================================
                    System.out.println("=== 4. Building SRAM_Chip_Final{sch} ===");
                    Cell macroSch = lib.findNodeProto("SRAM_256_Macro{sch}");
                    Cell macroIc  = lib.findNodeProto("SRAM_256_Macro{ic}");
                    Cell macroLay = lib.findNodeProto("SRAM_256_Macro{lay}");

                    Cell chipSch = Cell.makeInstance(lib, "SRAM_Chip_Final{sch}");
                    NodeInst instMacroSch = NodeInst.makeInstance(macroIc, new Point2D.Double(0.0, 0.0), 0, 0, chipSch);

                    NodeInst padClkSch  = NodeInst.makeInstance(inPadIc, new Point2D.Double(-200.0, 150.0), 0, 0, chipSch);
                    NodeInst padAddrSch = NodeInst.makeInstance(inPadIc, new Point2D.Double(-200.0, 50.0), 0, 0, chipSch);
                    NodeInst padRwSch   = NodeInst.makeInstance(inPadIc, new Point2D.Double(-200.0, -50.0), 0, 0, chipSch);
                    NodeInst padDinSch  = NodeInst.makeInstance(inPadIc, new Point2D.Double(-200.0, -150.0), 0, 0, chipSch);
                    NodeInst padDoutSch = NodeInst.makeInstance(outPadIc, new Point2D.Double(200.0, 0.0), 0, 0, chipSch);

                    ArcInst.makeInstance(wire, ep, padClkSch.findPortInstFromProto(inPadIc.findExport("CHIP_OUT")), instMacroSch.findPortInstFromProto(macroIc.findExport("CLK")));
                    ArcInst.makeInstance(wire, ep, padAddrSch.findPortInstFromProto(inPadIc.findExport("CHIP_OUT")), instMacroSch.findPortInstFromProto(macroIc.findExport("ADDR_IN")));
                    ArcInst.makeInstance(wire, ep, padRwSch.findPortInstFromProto(inPadIc.findExport("CHIP_OUT")), instMacroSch.findPortInstFromProto(macroIc.findExport("R_W")));
                    ArcInst.makeInstance(wire, ep, padDinSch.findPortInstFromProto(inPadIc.findExport("CHIP_OUT")), instMacroSch.findPortInstFromProto(macroIc.findExport("DIN")));
                    ArcInst.makeInstance(wire, ep, instMacroSch.findPortInstFromProto(macroIc.findExport("DOUT")), padDoutSch.findPortInstFromProto(outPadIc.findExport("CHIP_IN")));

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
                    // 5. Build SRAM_Chip_Final{lay}
                    // ==========================================
                    System.out.println("=== 5. Building SRAM_Chip_Final{lay} ===");
                    Cell chipLay = Cell.makeInstance(lib, "SRAM_Chip_Final{lay}");

                    ArcProto poly = chipLay.getTechnology().findArcProto("Polysilicon-1");
                    NodeProto conPolyM1 = chipLay.getTechnology().findNodeProto("Metal-1-Polysilicon-1-Con");

                    // 1. Place Macro at (0,0)
                    NodeInst lMacro = NodeInst.makeInstance(macroLay, new Point2D.Double(0.0, 0.0), 0, 0, chipLay);

                    // 2. Place 4 Input Pads along left edge
                    NodeInst lPadClk  = NodeInst.makeInstance(inPadLay, new Point2D.Double(-1300.0, 2250.0), 0, 0, chipLay);
                    NodeInst lPadAddr = NodeInst.makeInstance(inPadLay, new Point2D.Double(-1300.0, 2050.0), 0, 0, chipLay);
                    NodeInst lPadRw   = NodeInst.makeInstance(inPadLay, new Point2D.Double(-1300.0, 1760.0), 0, 0, chipLay);
                    NodeInst lPadDin  = NodeInst.makeInstance(inPadLay, new Point2D.Double(-1300.0, -560.0), 0, 0, chipLay);

                    // 3. Place Output Pad along bottom edge at (300.0, -2750.0)
                    // Input CHIP_IN will be at (260.0, -2751.0), perfectly aligned to the DOUT channel
                    NodeInst lPadDout = NodeInst.makeInstance(outPadLay, new Point2D.Double(300.0, -2750.0), 0, 0, chipLay);

                    // ------------------------------------------
                    // Signal Connections from Pads to Macro
                    // ------------------------------------------
                    // 1. CLK: Jog up to Y = 2250.0 at X = -1132.0, then horizontal M3 to macro.CLK at (-685.0, 2250.0)
                    PortInst pClkOut = lPadClk.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroClk = lMacro.findPortInstFromProto(macroLay.findExport("CLK"));
                    NodeInst v23_clk_pad = NodeInst.makeInstance(via23Proto, new Point2D.Double(-1132.0, 2212.0), 6.0, 6.0, chipLay);
                    NodeInst jClk_m3_top = NodeInst.makeInstance(m3Pin, new Point2D.Double(-1132.0, 2250.0), 3.0, 3.0, chipLay);
                    NodeInst v23_clk_mac = NodeInst.makeInstance(via23Proto, new Point2D.Double(-685.0, 2250.0), 6.0, 6.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pClkOut, v23_clk_pad.getPortInst(0));
                    ArcInst.makeInstance(m3, ep, v23_clk_pad.getPortInst(0), jClk_m3_top.getPortInst(0));
                    ArcInst.makeInstance(m3, ep, jClk_m3_top.getPortInst(0), v23_clk_mac.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_clk_mac.getPortInst(0), pMacroClk);

                    // 2. ADDR_IN: M3 from (-1132, 2012) to (-800, 2012) -> jog to (-800, 2210) -> M3 to (-560, 2210) -> macro.ADDR_IN
                    PortInst pAddrOut = lPadAddr.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroAddr = lMacro.findPortInstFromProto(macroLay.findExport("ADDR_IN"));
                    NodeInst v23_addr_pad = NodeInst.makeInstance(via23Proto, new Point2D.Double(-1132.0, 2012.0), 6.0, 6.0, chipLay);
                    NodeInst jAddr_m3_1   = NodeInst.makeInstance(m3Pin, new Point2D.Double(-800.0, 2012.0), 3.0, 3.0, chipLay);
                    NodeInst jAddr_m3_2   = NodeInst.makeInstance(m3Pin, new Point2D.Double(-800.0, 2210.0), 3.0, 3.0, chipLay);
                    NodeInst v23_addr_mac = NodeInst.makeInstance(via23Proto, new Point2D.Double(-560.0, 2210.0), 6.0, 6.0, chipLay);
                    NodeInst v12_addr_mac = NodeInst.makeInstance(via12Proto, new Point2D.Double(-560.0, 2210.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pAddrOut, v23_addr_pad.getPortInst(0));
                    ArcInst.makeInstance(m3, ep, v23_addr_pad.getPortInst(0), jAddr_m3_1.getPortInst(0));
                    ArcInst.makeInstance(m3, ep, jAddr_m3_1.getPortInst(0), jAddr_m3_2.getPortInst(0));
                    ArcInst.makeInstance(m3, ep, jAddr_m3_2.getPortInst(0), v23_addr_mac.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_addr_mac.getPortInst(0), v12_addr_mac.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_addr_mac.getPortInst(0), pMacroAddr);

                    // 3. R_W: M3 from (-1132, 1722) to (-700, 1722) -> up to macro.R_W at (-700, 1760)
                    PortInst pRwOut = lPadRw.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroRw = lMacro.findPortInstFromProto(macroLay.findExport("R_W"));
                    NodeInst v23_rw_pad = NodeInst.makeInstance(via23Proto, new Point2D.Double(-1132.0, 1722.0), 6.0, 6.0, chipLay);
                    NodeInst jRw_m3_1   = NodeInst.makeInstance(m3Pin, new Point2D.Double(-700.0, 1722.0), 3.0, 3.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pRwOut, v23_rw_pad.getPortInst(0));
                    ArcInst.makeInstance(m3, ep, v23_rw_pad.getPortInst(0), jRw_m3_1.getPortInst(0));
                    ArcInst.makeInstance(m3, ep, jRw_m3_1.getPortInst(0), pMacroRw);

                    // 4. DIN: M3 from (-1132, -598) to (-180, -598) -> M2 jog to (-180, -560) -> M2 across to (-85, -560) -> Poly to (-75, -560)
                    PortInst pDinOut = lPadDin.findPortInstFromProto(inPadLay.findExport("CHIP_OUT"));
                    PortInst pMacroDin = lMacro.findPortInstFromProto(macroLay.findExport("DIN"));
                    NodeInst v23_din_pad = NodeInst.makeInstance(via23Proto, new Point2D.Double(-1132.0, -598.0), 6.0, 6.0, chipLay);
                    NodeInst v23_din_mid = NodeInst.makeInstance(via23Proto, new Point2D.Double(-180.0, -598.0), 6.0, 6.0, chipLay);
                    NodeInst jDin_m2     = NodeInst.makeInstance(m2Pin, new Point2D.Double(-180.0, -560.0), 3.0, 3.0, chipLay);
                    NodeInst v12_din_mac = NodeInst.makeInstance(via12Proto, new Point2D.Double(-85.0, -560.0), 5.0, 5.0, chipLay);
                    NodeInst cPoly_din   = NodeInst.makeInstance(conPolyM1,  new Point2D.Double(-85.0, -560.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pDinOut, v23_din_pad.getPortInst(0));
                    ArcInst.makeInstance(m3, ep, v23_din_pad.getPortInst(0), v23_din_mid.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_din_mid.getPortInst(0), jDin_m2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, jDin_m2.getPortInst(0), v12_din_mac.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_din_mac.getPortInst(0), cPoly_din.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_din.getPortInst(0), pMacroDin);

                    // 5. DOUT: macro.DOUT at (213.5, -650) -> drop M2 down to (213.5, -2751) -> M2 east to (260, -2751) -> Output_Pad.CHIP_IN
                    PortInst pMacroDout = lMacro.findPortInstFromProto(macroLay.findExport("DOUT"));
                    PortInst pDoutIn    = lPadDout.findPortInstFromProto(outPadLay.findExport("CHIP_IN"));
                    NodeInst jDout_m2   = NodeInst.makeInstance(m2Pin, new Point2D.Double(213.5, -2751.0), 3.0, 3.0, chipLay);
                    NodeInst v12_dout_pad = NodeInst.makeInstance(via12Proto, new Point2D.Double(260.0, -2751.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pMacroDout, jDout_m2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, jDout_m2.getPortInst(0), v12_dout_pad.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_dout_pad.getPortInst(0), pDoutIn);

                    // ------------------------------------------
                    // Power Ring Architecture
                    // ------------------------------------------
                    // Outer Ring: GND
                    //   Top:    Y = 2550.0 (M1)
                    //   Bottom: Y = -2970.0 (M1)
                    //   Left:   X = -1550.0 (M2)
                    //   Right:  X = 4500.0 (M2)
                    // Inner Ring: VDD
                    //   Top:    Y = 2450.0 (M1)
                    //   Bottom: Y = -2900.0 (M1)
                    //   Left:   X = -1450.0 (M2)
                    //   Right:  X = 4400.0 (M2)

                    NodeInst v12_gnd_TL = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1550.0, 2550.0), 6.0, 6.0, chipLay);
                    NodeInst v12_gnd_TR = NodeInst.makeInstance(via12Proto, new Point2D.Double(4500.0, 2550.0), 6.0, 6.0, chipLay);
                    NodeInst v12_gnd_BR = NodeInst.makeInstance(via12Proto, new Point2D.Double(4500.0, -2970.0), 6.0, 6.0, chipLay);
                    NodeInst v12_gnd_BL = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1550.0, -2970.0), 6.0, 6.0, chipLay);

                    NodeInst v12_vdd_TL = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1450.0, 2450.0), 6.0, 6.0, chipLay);
                    NodeInst v12_vdd_TR = NodeInst.makeInstance(via12Proto, new Point2D.Double(4400.0, 2450.0), 6.0, 6.0, chipLay);
                    NodeInst v12_vdd_BR = NodeInst.makeInstance(via12Proto, new Point2D.Double(4400.0, -2900.0), 6.0, 6.0, chipLay);
                    NodeInst v12_vdd_BL = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1450.0, -2900.0), 6.0, 6.0, chipLay);

                    // Left Side Taps for Input Pads
                    double[] yInPads = {2250.0, 2050.0, 1760.0, -560.0};
                    NodeInst[] inPadInsts = {lPadClk, lPadAddr, lPadRw, lPadDin};

                    NodeInst prevVddLeft = v12_vdd_TL;
                    NodeInst prevGndLeft = v12_gnd_TL;

                    for (int i = 0; i < 4; i++) {
                        double yV = yInPads[i] + 21.5;
                        double yG = yInPads[i] - 23.0;

                        // VDD tap on Left M2 rail at (-1450, yV)
                        NodeInst tapV_m2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1450.0, yV), 5.0, 5.0, chipLay);
                        ArcInst.makeInstance(m2, ep, prevVddLeft.getPortInst(0), tapV_m2.getPortInst(0));
                        prevVddLeft = tapV_m2;

                        PortInst pPadV = inPadInsts[i].findPortInstFromProto(inPadLay.findExport("vdd"));
                        ArcInst.makeInstance(m1, ep, tapV_m2.getPortInst(0), pPadV);

                        // GND tap on Left M2 rail at (-1550, yG)
                        NodeInst tapG_m2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1550.0, yG), 5.0, 5.0, chipLay);
                        ArcInst.makeInstance(m2, ep, prevGndLeft.getPortInst(0), tapG_m2.getPortInst(0));
                        prevGndLeft = tapG_m2;

                        PortInst pPadG = inPadInsts[i].findPortInstFromProto(inPadLay.findExport("gnd"));
                        ArcInst.makeInstance(m1, ep, tapG_m2.getPortInst(0), pPadG);
                    }
                    ArcInst.makeInstance(m2, ep, prevVddLeft.getPortInst(0), v12_vdd_BL.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, prevGndLeft.getPortInst(0), v12_gnd_BL.getPortInst(0));

                    // Top Rails (M1)
                    ArcInst.makeInstance(m1, ep, v12_vdd_TL.getPortInst(0), v12_vdd_TR.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_gnd_TL.getPortInst(0), v12_gnd_TR.getPortInst(0));

                    // Right Rails (M2)
                    ArcInst.makeInstance(m2, ep, v12_vdd_TR.getPortInst(0), v12_vdd_BR.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_gnd_TR.getPortInst(0), v12_gnd_BR.getPortInst(0));

                    // Bottom Taps: Macro Power
                    PortInst pmV = lMacro.findPortInstFromProto(macroLay.findExport("vdd"));
                    NodeInst v12_pmV_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, -2900.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pmV, v12_pmV_bot.getPortInst(0));

                    PortInst pmG = lMacro.findPortInstFromProto(macroLay.findExport("gnd"));
                    NodeInst v12_pmG_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, -2970.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m2, ep, pmG, v12_pmG_bot.getPortInst(0));

                    // Bottom Taps: Output Pad Power (tapped to the west of Output_Pad at X = 280 for VDD and X = 270 for GND)
                    PortInst poV = lPadDout.findPortInstFromProto(outPadLay.findExport("vdd"));
                    NodeInst v12_poV_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(280.0, -2728.5), 5.0, 5.0, chipLay);
                    NodeInst v12_poV_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(280.0, -2900.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m1, ep, poV, v12_poV_top.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_poV_top.getPortInst(0), v12_poV_bot.getPortInst(0));

                    PortInst poG = lPadDout.findPortInstFromProto(outPadLay.findExport("gnd"));
                    NodeInst v12_poG_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(270.0, -2773.0), 5.0, 5.0, chipLay);
                    NodeInst v12_poG_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(270.0, -2970.0), 5.0, 5.0, chipLay);
                    ArcInst.makeInstance(m1, ep, poG, v12_poG_top.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_poG_top.getPortInst(0), v12_poG_bot.getPortInst(0));

                    // Daisy-chain Bottom VDD rail (all on M1 at Y = -2900.0)
                    ArcInst.makeInstance(m1, ep, v12_vdd_BL.getPortInst(0), v12_pmV_bot.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_pmV_bot.getPortInst(0), v12_poV_bot.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_poV_bot.getPortInst(0), v12_vdd_BR.getPortInst(0));

                    // Daisy-chain Bottom GND rail (all on M1 at Y = -2970.0)
                    ArcInst.makeInstance(m1, ep, v12_gnd_BL.getPortInst(0), v12_pmG_bot.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_pmG_bot.getPortInst(0), v12_poG_bot.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_poG_bot.getPortInst(0), v12_gnd_BR.getPortInst(0));

                    // ------------------------------------------
                    // Outer Wire-Bonding Pads (Metal-3)
                    // ------------------------------------------
                    // 1. pad_CLK at (-1750.0, 2249.0)
                    NodeInst pinPadClk = NodeInst.makeInstance(m3Pin, new Point2D.Double(-1750.0, 2249.0), 10.0, 10.0, chipLay);
                    ArcInst.makeInstance(m3, ep, pinPadClk.getPortInst(0), lPadClk.findPortInstFromProto(inPadLay.findExport("PAD_IN")));

                    // 2. pad_ADDR_IN at (-1750.0, 2049.0)
                    NodeInst pinPadAddr = NodeInst.makeInstance(m3Pin, new Point2D.Double(-1750.0, 2049.0), 10.0, 10.0, chipLay);
                    ArcInst.makeInstance(m3, ep, pinPadAddr.getPortInst(0), lPadAddr.findPortInstFromProto(inPadLay.findExport("PAD_IN")));

                    // 3. pad_R_W at (-1750.0, 1759.0)
                    NodeInst pinPadRw = NodeInst.makeInstance(m3Pin, new Point2D.Double(-1750.0, 1759.0), 10.0, 10.0, chipLay);
                    ArcInst.makeInstance(m3, ep, pinPadRw.getPortInst(0), lPadRw.findPortInstFromProto(inPadLay.findExport("PAD_IN")));

                    // 4. pad_DIN at (-1750.0, -561.0)
                    NodeInst pinPadDin = NodeInst.makeInstance(m3Pin, new Point2D.Double(-1750.0, -561.0), 10.0, 10.0, chipLay);
                    ArcInst.makeInstance(m3, ep, pinPadDin.getPortInst(0), lPadDin.findPortInstFromProto(inPadLay.findExport("PAD_IN")));

                    // 5. pad_DOUT at (2368.0, -3150.0)
                    NodeInst pinPadDout = NodeInst.makeInstance(m3Pin, new Point2D.Double(2368.0, -3150.0), 10.0, 10.0, chipLay);
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
                    // 6. VERIFICATION: DRC & Hierarchical NCC
                    // ==========================================
                    System.out.println("=== 6. Running DRC on SRAM_Chip_Final{lay} ===");
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, chipLay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println(">>> DRC Result: " + errLog.getNumErrors() + " errors, " + errLog.getNumWarnings() + " warnings <<<");

                    java.lang.reflect.Field fLines = Class.forName("com.sun.electric.tool.user.ErrorHighPoly").getDeclaredField("linesList");
                    fLines.setAccessible(true);
                    java.lang.reflect.Field fP1 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p1");
                    fP1.setAccessible(true);
                    java.lang.reflect.Field fP2 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p2");
                    fP2.setAccessible(true);
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = it.next();
                        System.out.println("   DRC: " + ml.getMessageString());
                        for (Iterator hit = ml.getHighlights(); hit.hasNext(); ) {
                            Object h = hit.next();
                            try {
                                java.util.List lines = (java.util.List) fLines.get(h);
                                if (lines != null) {
                                    for (Object line : lines) {
                                        System.out.println("      Line: " + fP1.get(line) + " -> " + fP2.get(line));
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                    System.out.println("=== 7. Running Hierarchical NCC on SRAM_Chip_Final ===");
                    NccOptions opts = new NccOptions();
                    opts.checkSizes = false;
                    NccResults res = Ncc.compare(chipSch, null, chipLay, null, opts);
                    System.out.println(">>> NCC Result: " + res + " <<<");
                    System.out.println("  Match: " + res.match());
                    System.out.println("  Exports Match: " + res.exportMatch());
                    System.out.println("  Topology Match: " + res.topologyMatch());

                    // Metrics
                    System.out.println("=== Chip Physical Metrics ===");
                    java.awt.geom.Rectangle2D bounds = chipLay.getBounds();
                    System.out.println("Bounding Box: " + bounds);
                    System.out.println("Width:  " + bounds.getWidth() + " lambda (" + (bounds.getWidth() * 0.09) + " um @ 90nm)");
                    System.out.println("Height: " + bounds.getHeight() + " lambda (" + (bounds.getHeight() * 0.09) + " um @ 90nm)");
                    System.out.println("Area:   " + (bounds.getWidth() * bounds.getHeight()) + " lambda^2 (" + (bounds.getWidth() * bounds.getHeight() * 0.0081) + " um^2 @ 90nm)");

                    if (errLog.getNumErrors() == 0 && res.match()) {
                        System.out.println("=== 8. Saving SRAM.jelib ===");
                        Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                        System.out.println("Library save status: SUCCESS");

                        System.out.println("=== 9. Exporting SPICE deck: SRAM_Chip_Final.spi ===");
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
