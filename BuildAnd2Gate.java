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
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.IconParameters;
import com.sun.electric.tool.user.User;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;

public class BuildAnd2Gate {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildAnd2Gate", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();
                    IconParameters ip = IconParameters.makeInstance(true);

                    Cell n4Ic = lib.findNodeProto("nand4{ic}");
                    Cell n4Lay = lib.findNodeProto("nand4{lay}");
                    Cell invIc = lib.findNodeProto("inverter{ic}");
                    Cell invLay = lib.findNodeProto("inverter{lay}");

                    // 1. SCHEMATIC
                    Cell sch = Cell.makeInstance(lib, "and2{sch}");
                    ArcProto wire = sch.getTechnology().findArcProto("wire");

                    NodeInst n4Sch = NodeInst.makeInstance(n4Ic, new Point2D.Double(0, 0), 0, 0, sch);
                    NodeInst invSch = NodeInst.makeInstance(invIc, new Point2D.Double(40.0, 0), 0, 0, sch);

                    // Connect nand4 out to inverter in
                    ArcInst.makeInstance(wire, ep, n4Sch.findPortInstFromProto(n4Ic.findExport("out")), invSch.findPortInstFromProto(invIc.findExport("in")));

                    // Tie C and D to vdd
                    ArcInst.makeInstance(wire, ep, n4Sch.findPortInstFromProto(n4Ic.findExport("C")), n4Sch.findPortInstFromProto(n4Ic.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, n4Sch.findPortInstFromProto(n4Ic.findExport("D")), n4Sch.findPortInstFromProto(n4Ic.findExport("vdd")));

                    // Tie power
                    ArcInst.makeInstance(wire, ep, n4Sch.findPortInstFromProto(n4Ic.findExport("vdd")), invSch.findPortInstFromProto(invIc.findExport("vdd")));
                    ArcInst.makeInstance(wire, ep, n4Sch.findPortInstFromProto(n4Ic.findExport("gnd")), invSch.findPortInstFromProto(invIc.findExport("gnd")));

                    // Exports
                    Export.newInst(sch, n4Sch.findPortInstFromProto(n4Ic.findExport("A")), "A", ep);
                    Export.newInst(sch, n4Sch.findPortInstFromProto(n4Ic.findExport("B")), "B", ep);
                    Export.newInst(sch, invSch.findPortInstFromProto(invIc.findExport("out")), "Y", ep);
                    Export.newInst(sch, n4Sch.findPortInstFromProto(n4Ic.findExport("vdd")), "vdd", ep);
                    Export.newInst(sch, n4Sch.findPortInstFromProto(n4Ic.findExport("gnd")), "gnd", ep);

                    Cell ic = ip.makeIconForCell(sch, ep);
                    System.out.println("  and2{sch} and {ic} created.");

                    // 2. LAYOUT
                    Cell lay = Cell.makeInstance(lib, "and2{lay}");
                    ArcProto m1 = lay.getTechnology().findArcProto("Metal-1");
                    ArcProto m2 = lay.getTechnology().findArcProto("Metal-2");
                    NodeProto via12 = lay.getTechnology().findNodeProto("Metal-1-Metal-2-Con");

                    // Place nand4 at (0, 0), inverter at (70.0, 0)
                    NodeInst n4Inst = NodeInst.makeInstance(n4Lay, new Point2D.Double(0, 0), 0, 0, lay);
                    NodeInst invInst = NodeInst.makeInstance(invLay, new Point2D.Double(70.0, 0), 0, 0, lay);

                    // In nand4{lay}:
                    // out is at (-28.0, 45.0) on Metal-2
                    // In inverter{lay}:
                    // in is at 70.0 + (-25.0, -1.0) = (45.0, -1.0) on Metal-1
                    // Connect out to in:
                    // Drop via at (45.0, 45.0) to connect M2 to M1
                    NodeInst viaOut = NodeInst.makeInstance(via12, new Point2D.Double(45.0, 45.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, n4Inst.findPortInstFromProto(n4Lay.findExport("out")), viaOut.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, viaOut.getPortInst(0), invInst.findPortInstFromProto(invLay.findExport("in")));

                    // Tie C and D to vdd in nand4
                    // In nand4{lay}:
                    // C is at (-11.5, -52.5) on Metal-1
                    // D is at (1.5, -52.5) on Metal-1
                    // vdd is at (-28.0, 28.5) on Metal-1
                    // We can connect C and D together on M1, and run M1/M2 to vdd
                    // Or connect C, D to vdd rail
                    PortInst pVddN4 = n4Inst.findPortInstFromProto(n4Lay.findExport("vdd"));
                    PortInst pC = n4Inst.findPortInstFromProto(n4Lay.findExport("C"));
                    PortInst pD = n4Inst.findPortInstFromProto(n4Lay.findExport("D"));

                    NodeInst viaVdd = NodeInst.makeInstance(via12, new Point2D.Double(15.0, 28.5), 5.0, 5.0, lay);
                    NodeInst viaCD = NodeInst.makeInstance(via12, new Point2D.Double(15.0, -52.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pVddN4, viaVdd.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, viaVdd.getPortInst(0), viaCD.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, pD, viaCD.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, pC, pD);

                    // Power connections between nand4 and inverter
                    PortInst pVddInv = invInst.findPortInstFromProto(invLay.findExport("vdd"));
                    PortInst pGndN4 = n4Inst.findPortInstFromProto(n4Lay.findExport("gnd"));
                    PortInst pGndInv = invInst.findPortInstFromProto(invLay.findExport("gnd"));

                    // vdd: n4 vdd at (-28.0, 28.5), inv vdd at (70 - 9.5, 21.5) = (60.5, 21.5)
                    NodeInst viaVddMid = NodeInst.makeInstance(via12, new Point2D.Double(60.5, 28.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, viaVdd.getPortInst(0), viaVddMid.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, viaVddMid.getPortInst(0), pVddInv);

                    // gnd: n4 gnd at (-28.0, -28.0), inv gnd at (60.5, -23.0)
                    NodeInst viaGnd1 = NodeInst.makeInstance(via12, new Point2D.Double(-28.0, -40.0), 5.0, 5.0, lay);
                    NodeInst viaGnd2 = NodeInst.makeInstance(via12, new Point2D.Double(60.5, -40.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pGndN4, viaGnd1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, viaGnd1.getPortInst(0), viaGnd2.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, viaGnd2.getPortInst(0), pGndInv);

                    // Exports
                    Export.newInst(lay, n4Inst.findPortInstFromProto(n4Lay.findExport("A")), "A", ep);
                    Export.newInst(lay, n4Inst.findPortInstFromProto(n4Lay.findExport("B")), "B", ep);
                    Export.newInst(lay, invInst.findPortInstFromProto(invLay.findExport("out")), "Y", ep);
                    Export.newInst(lay, pVddN4, "vdd", ep);
                    Export.newInst(lay, pGndN4, "gnd", ep);

                    System.out.println("  and2{lay} created.");

                    // DRC
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, lay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println("and2 DRC: " + errLog.getNumErrors() + " errors, " + errLog.getNumWarnings() + " warnings");
                    for (java.util.Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        System.out.println("  [DRC] " + it.next().getMessageString());
                    }

                    // NCC
                    NccOptions opts = new NccOptions();
                    opts.checkSizes = false;
                    NccResults res = Ncc.compare(sch, null, lay, null, opts);
                    System.out.println("and2 NCC: " + res);
                    System.out.println("  Match: " + res.match());
                    System.out.println("  Exports Match: " + res.exportMatch());
                    System.out.println("  Topology Match: " + res.topologyMatch());

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(3000);
    }
}
