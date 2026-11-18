import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.simulation.SimulationTool;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.IconParameters;
import com.sun.electric.tool.user.User;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class BuildCoreDatapath {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});

        // Backup SRAM.jelib
        File orig = new File("SRAM.jelib");
        File bak = new File("SRAM.jelib.phase3_task8_1.bak");
        Files.copy(orig.toPath(), bak.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Backed up SRAM.jelib to SRAM.jelib.phase3_task8_1.bak");

        Job j = new Job("BuildCoreDatapath", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    EditingPreferences ep = getEditingPreferences();
                    IconParameters ip = IconParameters.makeInstance(true);

                    // Remove existing test cells if any
                    Cell oldSch = lib.findNodeProto("SRAM_Core_Datapath{sch}");
                    if (oldSch != null) oldSch.kill();
                    Cell oldLay = lib.findNodeProto("SRAM_Core_Datapath{lay}");
                    if (oldLay != null) oldLay.kill();
                    Cell oldIc = lib.findNodeProto("SRAM_Core_Datapath{ic}");
                    if (oldIc != null) oldIc.kill();

                    Cell oldTestSch = lib.findNodeProto("Test_Core_Datapath{sch}");
                    if (oldTestSch != null) oldTestSch.kill();
                    Cell oldTestLay = lib.findNodeProto("Test_Core_Datapath{lay}");
                    if (oldTestLay != null) oldTestLay.kill();
                    Cell oldStacked = lib.findNodeProto("TestStackedVia{lay}");
                    if (oldStacked != null) oldStacked.kill();
                    Cell oldOffset = lib.findNodeProto("TestOffsetVia{lay}");
                    if (oldOffset != null) oldOffset.kill();
                    Cell oldMuxPin = lib.findNodeProto("TestMuxPinVia{lay}");
                    if (oldMuxPin != null) oldMuxPin.kill();

                    Cell icPre = lib.findNodeProto("Precharge_Bank_16{ic}");
                    Cell layPre = lib.findNodeProto("Precharge_Bank_16{lay}");

                    Cell icArr = lib.findNodeProto("SRAM_Array_16x16{ic}");
                    Cell layArr = lib.findNodeProto("SRAM_Array_16x16{lay}");

                    Cell icMux = lib.findNodeProto("Col_Mux_Bank_16{ic}");
                    Cell layMux = lib.findNodeProto("Col_Mux_Bank_16{lay}");

                    // ==========================================
                    // 1. SCHEMATIC: SRAM_Core_Datapath{sch}
                    // ==========================================
                    System.out.println("--- Building SRAM_Core_Datapath{sch} ---");
                    Cell schData = Cell.makeInstance(lib, "SRAM_Core_Datapath{sch}");
                    com.sun.electric.technology.ArcProto wireProto = schData.getTechnology().findArcProto("wire");

                    NodeInst schPreInst = NodeInst.makeInstance(icPre, new Point2D.Double(0, 100.0), 0, 0, schData);
                    NodeInst schArrInst = NodeInst.makeInstance(icArr, new Point2D.Double(0, 0.0), 0, 0, schData);
                    NodeInst schMuxInst = NodeInst.makeInstance(icMux, new Point2D.Double(0, -100.0), 0, 0, schData);

                    for (int c = 0; c < 16; c++) {
                        String bl = "BL[" + c + "]";
                        String blb = "BLB[" + c + "]";
                        ArcInst.makeInstance(wireProto, ep,
                            schPreInst.findPortInstFromProto(icPre.findExport(bl)),
                            schArrInst.findPortInstFromProto(icArr.findExport(bl)));
                        ArcInst.makeInstance(wireProto, ep,
                            schPreInst.findPortInstFromProto(icPre.findExport(blb)),
                            schArrInst.findPortInstFromProto(icArr.findExport(blb)));
                        ArcInst.makeInstance(wireProto, ep,
                            schArrInst.findPortInstFromProto(icArr.findExport(bl)),
                            schMuxInst.findPortInstFromProto(icMux.findExport(bl)));
                        ArcInst.makeInstance(wireProto, ep,
                            schArrInst.findPortInstFromProto(icArr.findExport(blb)),
                            schMuxInst.findPortInstFromProto(icMux.findExport(blb)));
                    }

                    ArcInst.makeInstance(wireProto, ep,
                        schPreInst.findPortInstFromProto(icPre.findExport("vdd")),
                        schArrInst.findPortInstFromProto(icArr.findExport("vdd")));
                    ArcInst.makeInstance(wireProto, ep,
                        schArrInst.findPortInstFromProto(icArr.findExport("vdd")),
                        schMuxInst.findPortInstFromProto(icMux.findExport("vdd")));

                    ArcInst.makeInstance(wireProto, ep,
                        schPreInst.findPortInstFromProto(icPre.findExport("gnd")),
                        schArrInst.findPortInstFromProto(icArr.findExport("gnd")));
                    ArcInst.makeInstance(wireProto, ep,
                        schArrInst.findPortInstFromProto(icArr.findExport("gnd")),
                        schMuxInst.findPortInstFromProto(icMux.findExport("gnd")));

                    // Exports
                    for (int r = 0; r < 16; r++) {
                        Export.newInst(schData, schArrInst.findPortInstFromProto(icArr.findExport("WL[" + r + "]")), "WL[" + r + "]", ep);
                    }
                    Export.newInst(schData, schPreInst.findPortInstFromProto(icPre.findExport("PRE_B")), "PRE_B", ep);
                    for (int c = 0; c < 16; c++) {
                        Export.newInst(schData, schMuxInst.findPortInstFromProto(icMux.findExport("COL_SEL[" + c + "]")), "COL_SEL[" + c + "]", ep);
                        Export.newInst(schData, schMuxInst.findPortInstFromProto(icMux.findExport("COL_SEL_B[" + c + "]")), "COL_SEL_B[" + c + "]", ep);
                    }
                    Export.newInst(schData, schMuxInst.findPortInstFromProto(icMux.findExport("GBL")), "GBL", ep);
                    Export.newInst(schData, schMuxInst.findPortInstFromProto(icMux.findExport("GBLB")), "GBLB", ep);
                    Export.newInst(schData, schArrInst.findPortInstFromProto(icArr.findExport("vdd")), "vdd", ep);
                    Export.newInst(schData, schArrInst.findPortInstFromProto(icArr.findExport("gnd")), "gnd", ep);

                    Cell icData = ip.makeIconForCell(schData, ep);
                    System.out.println("  SRAM_Core_Datapath{sch} and {ic} generated.");

                    // ==========================================
                    // 2. LAYOUT: SRAM_Core_Datapath{lay}
                    // ==========================================
                    System.out.println("--- Building SRAM_Core_Datapath{lay} ---");
                    Cell layData = Cell.makeInstance(lib, "SRAM_Core_Datapath{lay}");
                    com.sun.electric.technology.ArcProto m1Proto = layData.getTechnology().findArcProto("Metal-1");
                    com.sun.electric.technology.ArcProto m2Proto = layData.getTechnology().findArcProto("Metal-2");
                    com.sun.electric.technology.ArcProto m3Proto = layData.getTechnology().findArcProto("Metal-3");
                    com.sun.electric.database.prototype.NodeProto via12Proto = layData.getTechnology().findNodeProto("Metal-1-Metal-2-Con");
                    com.sun.electric.database.prototype.NodeProto via23Proto = layData.getTechnology().findNodeProto("Metal-2-Metal-3-Con");
                    com.sun.electric.database.prototype.NodeProto m2PinProto = layData.getTechnology().findNodeProto("Metal-2-Pin");

                    double preX = -2.25;
                    double preY = 1395.0; // Perfect abutment at DeltaY = 0.0 with Array top (Y = 1353.0)
                    double arrX = 0.0;
                    double arrY = 0.0;
                    double muxX = 0.0;
                    double muxY = -395.0; // Col Mux placed with clean routing channel for pitch adaptation

                    NodeInst layPreInst = NodeInst.makeInstance(layPre, new Point2D.Double(preX, preY), 0, 0, layData);
                    NodeInst layArrInst = NodeInst.makeInstance(layArr, new Point2D.Double(arrX, arrY), 0, 0, layData);
                    NodeInst layMuxInst = NodeInst.makeInstance(layMux, new Point2D.Double(muxX, muxY), 0, 0, layData);

                    // A. Connect Precharge -> Array (Top Abutment)
                    for (int c = 0; c < 16; c++) {
                        PortInst preBl = layPreInst.findPortInstFromProto(layPre.findExport("BL[" + c + "]"));
                        PortInst arrBl = layArrInst.findPortInstFromProto(layArr.findExport("BL[" + c + "]"));
                        double blArrX = -34.0 + c * 69.0;
                        NodeInst viaBl = NodeInst.makeInstance(via12Proto, new Point2D.Double(blArrX, 1355.0), 5.0, 5.0, layData);
                        ArcInst.makeInstance(m1Proto, ep, preBl, viaBl.findPortInstFromProto(via12Proto.getPort(0)));
                        ArcInst.makeInstance(m2Proto, ep, viaBl.findPortInstFromProto(via12Proto.getPort(0)), arrBl);

                        PortInst preBlb = layPreInst.findPortInstFromProto(layPre.findExport("BLB[" + c + "]"));
                        PortInst arrBlb = layArrInst.findPortInstFromProto(layArr.findExport("BLB[" + c + "]"));
                        double blbArrX = 24.5 + c * 69.0;
                        NodeInst viaBlb = NodeInst.makeInstance(via12Proto, new Point2D.Double(blbArrX, 1355.0), 5.0, 5.0, layData);
                        ArcInst.makeInstance(m1Proto, ep, preBlb, viaBlb.findPortInstFromProto(via12Proto.getPort(0)));
                        ArcInst.makeInstance(m2Proto, ep, viaBlb.findPortInstFromProto(via12Proto.getPort(0)), arrBlb);
                    }

                    // B. Array -> Col Mux Channel Router with Topological Track Assignment
                    int N = 32;
                    double[] Xarr = new double[N];
                    double[] Xmux = new double[N];
                    String[] netNames = new String[N];
                    PortInst[] arrPorts = new PortInst[N];
                    PortInst[] muxPorts = new PortInst[N];

                    for (int c = 0; c < 16; c++) {
                        int idxBl = 2 * c;
                        int idxBlb = 2 * c + 1;

                        netNames[idxBl] = "BL[" + c + "]";
                        Xarr[idxBl] = -34.0 + c * 69.0;
                        Xmux[idxBl] = -26.5 + c * 76.0;
                        arrPorts[idxBl] = layArrInst.findPortInstFromProto(layArr.findExport(netNames[idxBl]));
                        muxPorts[idxBl] = layMuxInst.findPortInstFromProto(layMux.findExport(netNames[idxBl]));

                        netNames[idxBlb] = "BLB[" + c + "]";
                        Xarr[idxBlb] = 24.5 + c * 69.0;
                        Xmux[idxBlb] = 13.5 + c * 76.0;
                        arrPorts[idxBlb] = layArrInst.findPortInstFromProto(layArr.findExport(netNames[idxBlb]));
                        muxPorts[idxBlb] = layMuxInst.findPortInstFromProto(layMux.findExport(netNames[idxBlb]));
                    }

                    // Build topological constraints to prevent vertical M2 and M3 crossing conflicts
                    List<List<Integer>> adj = new ArrayList<>();
                    int[] inDegree = new int[N];
                    for (int i = 0; i < N; i++) adj.add(new ArrayList<>());

                    for (int jNet = 0; jNet < N; jNet++) {
                        for (int kNet = 0; kNet < N; kNet++) {
                            if (jNet == kNet) continue;
                            if (Math.abs(Xarr[jNet] - Xmux[kNet]) < 8.5) {
                                adj.get(jNet).add(kNet);
                                inDegree[kNet]++;
                            }
                        }
                    }

                    Queue<Integer> q = new LinkedList<>();
                    for (int i = 0; i < N; i++) {
                        if (inDegree[i] == 0) q.add(i);
                    }

                    List<Integer> topOrder = new ArrayList<>();
                    while (!q.isEmpty()) {
                        int u = q.poll();
                        topOrder.add(u);
                        for (int v : adj.get(u)) {
                            inDegree[v]--;
                            if (inDegree[v] == 0) q.add(v);
                        }
                    }

                    // Assign tracks: Y_track = -20.0 - trackIdx * 10.0
                    double[] Ytrack = new double[N];
                    for (int trackIdx = 0; trackIdx < N; trackIdx++) {
                        int net = topOrder.get(trackIdx);
                        Ytrack[net] = -20.0 - trackIdx * 10.0;
                    }

                    // Route each net using clean layer-orthogonal architecture
                    for (int net = 0; net < N; net++) {
                        double xA = Xarr[net];
                        double xM = Xmux[net];
                        double yT = Ytrack[net];

                        PortInst pArr = arrPorts[net];
                        PortInst pMux = muxPorts[net];

                        if (Math.abs(xA - xM) < 6.0) {
                            // Small span (BLB[1] span 4.0, BLB[2] span 3.0):
                            // Pre-jog at Y = -10.0 on Metal-2 from xA to xM (open space under Array)
                            NodeInst cornerArr1 = NodeInst.makeInstance(m2PinProto, new Point2D.Double(xA, -10.0), 3.0, 3.0, layData);
                            NodeInst cornerArr2 = NodeInst.makeInstance(m2PinProto, new Point2D.Double(xM, -10.0), 3.0, 3.0, layData);
                            ArcInst.makeInstance(m2Proto, ep, pArr, cornerArr1.findPortInstFromProto(m2PinProto.getPort(0)));
                            ArcInst.makeInstance(m2Proto, ep, cornerArr1.findPortInstFromProto(m2PinProto.getPort(0)), cornerArr2.findPortInstFromProto(m2PinProto.getPort(0)));

                            // Run straight down on Metal-2 at xM to viaB_23 at (xM, yT)
                            NodeInst viaB_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(xM, yT), 6.0, 6.0, layData);
                            ArcInst.makeInstance(m2Proto, ep, cornerArr2.findPortInstFromProto(m2PinProto.getPort(0)), viaB_23.findPortInstFromProto(via23Proto.getPort(0)));

                            // Metal-3 runs down from viaB_23 to viaC_23 at (xM, muxY)
                            NodeInst viaC_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(xM, muxY), 5.0, 5.0, layData);
                            NodeInst viaC_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(xM, muxY), 6.0, 6.0, layData);
                            ArcInst.makeInstance(m2Proto, ep, viaC_12.findPortInstFromProto(via12Proto.getPort(0)), viaC_23.findPortInstFromProto(via23Proto.getPort(0)));
                            ArcInst.makeInstance(m3Proto, ep, viaB_23.findPortInstFromProto(via23Proto.getPort(0)), viaC_23.findPortInstFromProto(via23Proto.getPort(0)));
                            ArcInst.makeInstance(m1Proto, ep, viaC_12.findPortInstFromProto(via12Proto.getPort(0)), pMux);
                        } else {
                            // Span is >= 7.5 lambda: use Metal-1 horizontal jog
                            NodeInst viaA = NodeInst.makeInstance(via12Proto, new Point2D.Double(xA, yT), 5.0, 5.0, layData);
                            ArcInst.makeInstance(m2Proto, ep, pArr, viaA.findPortInstFromProto(via12Proto.getPort(0)));

                            NodeInst viaB_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(xM, yT), 5.0, 5.0, layData);
                            NodeInst viaB_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(xM, yT), 6.0, 6.0, layData);
                            ArcInst.makeInstance(m2Proto, ep, viaB_12.findPortInstFromProto(via12Proto.getPort(0)), viaB_23.findPortInstFromProto(via23Proto.getPort(0)));
                            ArcInst.makeInstance(m1Proto, ep, viaA.findPortInstFromProto(via12Proto.getPort(0)), viaB_12.findPortInstFromProto(via12Proto.getPort(0)));

                            NodeInst viaC_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(xM, muxY), 5.0, 5.0, layData);
                            NodeInst viaC_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(xM, muxY), 6.0, 6.0, layData);
                            ArcInst.makeInstance(m2Proto, ep, viaC_12.findPortInstFromProto(via12Proto.getPort(0)), viaC_23.findPortInstFromProto(via23Proto.getPort(0)));
                            ArcInst.makeInstance(m3Proto, ep, viaB_23.findPortInstFromProto(via23Proto.getPort(0)), viaC_23.findPortInstFromProto(via23Proto.getPort(0)));
                            ArcInst.makeInstance(m1Proto, ep, viaC_12.findPortInstFromProto(via12Proto.getPort(0)), pMux);
                        }
                    }

                    // C. Power Rails: VDD & GND
                    PortInst arrVdd = layArrInst.findPortInstFromProto(layArr.findExport("vdd"));
                    PortInst preVdd = layPreInst.findPortInstFromProto(layPre.findExport("vdd"));
                    PortInst muxVdd = layMuxInst.findPortInstFromProto(layMux.findExport("vdd"));

                    // Precharge VDD
                    NodeInst viaVddPre = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, 1411.5), 5.0, 5.0, layData);
                    ArcInst.makeInstance(m1Proto, ep, preVdd, viaVddPre.findPortInstFromProto(via12Proto.getPort(0)));
                    ArcInst.makeInstance(m2Proto, ep, viaVddPre.findPortInstFromProto(via12Proto.getPort(0)), arrVdd);

                    // Col Mux VDD: run Metal-2 down at X = -55.0 to muxY + 32.0, jog M2 to (0.0, muxY + 32.0), Via12 to muxVdd
                    NodeInst viaVddCorner = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, muxY + 32.0), 5.0, 5.0, layData);
                    NodeInst viaVddMux = NodeInst.makeInstance(via12Proto, new Point2D.Double(0.0, muxY + 32.0), 5.0, 5.0, layData);
                    ArcInst.makeInstance(m2Proto, ep, arrVdd, viaVddCorner.findPortInstFromProto(via12Proto.getPort(0)));
                    ArcInst.makeInstance(m2Proto, ep, viaVddCorner.findPortInstFromProto(via12Proto.getPort(0)), viaVddMux.findPortInstFromProto(via12Proto.getPort(0)));
                    ArcInst.makeInstance(m1Proto, ep, viaVddMux.findPortInstFromProto(via12Proto.getPort(0)), muxVdd);

                    // Precharge GND
                    PortInst arrGnd = layArrInst.findPortInstFromProto(layArr.findExport("gnd"));
                    PortInst preGnd = layPreInst.findPortInstFromProto(layPre.findExport("gnd"));
                    PortInst muxGnd = layMuxInst.findPortInstFromProto(layMux.findExport("gnd"));

                    NodeInst viaGndPre = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, 1375.0), 5.0, 5.0, layData);
                    ArcInst.makeInstance(m1Proto, ep, preGnd, viaGndPre.findPortInstFromProto(via12Proto.getPort(0)));
                    ArcInst.makeInstance(m2Proto, ep, viaGndPre.findPortInstFromProto(via12Proto.getPort(0)), arrGnd);

                    // Col Mux GND: run Metal-2 down at X = -75.0 to muxY - 32.0, Via23 to Metal-3, run Metal-3 to (35.0, muxY - 32.0), Stacked Via12+Via23 to muxGnd
                    NodeInst viaGndCorner_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(-75.0, muxY - 32.0), 6.0, 6.0, layData);
                    ArcInst.makeInstance(m2Proto, ep, arrGnd, viaGndCorner_23.findPortInstFromProto(via23Proto.getPort(0)));

                    NodeInst viaGndMux_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(35.0, muxY - 32.0), 5.0, 5.0, layData);
                    NodeInst viaGndMux_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(35.0, muxY - 32.0), 6.0, 6.0, layData);
                    ArcInst.makeInstance(m2Proto, ep, viaGndMux_12.findPortInstFromProto(via12Proto.getPort(0)), viaGndMux_23.findPortInstFromProto(via23Proto.getPort(0)));
                    ArcInst.makeInstance(m3Proto, ep, viaGndCorner_23.findPortInstFromProto(via23Proto.getPort(0)), viaGndMux_23.findPortInstFromProto(via23Proto.getPort(0)));
                    ArcInst.makeInstance(m1Proto, ep, viaGndMux_12.findPortInstFromProto(via12Proto.getPort(0)), muxGnd);

                    // D. Exports on Layout (matching schematic)
                    for (int r = 0; r < 16; r++) {
                        Export.newInst(layData, layArrInst.findPortInstFromProto(layArr.findExport("WL[" + r + "]")), "WL[" + r + "]", ep);
                    }
                    Export.newInst(layData, layPreInst.findPortInstFromProto(layPre.findExport("PRE_B")), "PRE_B", ep);
                    for (int c = 0; c < 16; c++) {
                        Export.newInst(layData, layMuxInst.findPortInstFromProto(layMux.findExport("COL_SEL[" + c + "]")), "COL_SEL[" + c + "]", ep);
                        Export.newInst(layData, layMuxInst.findPortInstFromProto(layMux.findExport("COL_SEL_B[" + c + "]")), "COL_SEL_B[" + c + "]", ep);
                    }
                    Export.newInst(layData, layMuxInst.findPortInstFromProto(layMux.findExport("GBL")), "GBL", ep);
                    Export.newInst(layData, layMuxInst.findPortInstFromProto(layMux.findExport("GBLB")), "GBLB", ep);
                    Export.newInst(layData, arrVdd, "vdd", ep);
                    Export.newInst(layData, arrGnd, "gnd", ep);
                    System.out.println("  SRAM_Core_Datapath{lay} created with all 53 matching exports.");

                    // ==========================================
                    // 3. VERIFICATION: DRC & NCC
                    // ==========================================
                    System.out.println("--- Running DRC on SRAM_Core_Datapath{lay} ---");
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, layData, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                    errLog.termLogging(true);
                    System.out.println(">>> DRC Result: " + errLog.getNumErrors() + " errors, " + errLog.getNumWarnings() + " warnings <<<");
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        System.out.println("  [DRC] " + it.next().getMessageString());
                    }

                    System.out.println("--- Running NCC LVS on SRAM_Core_Datapath ---");
                    NccOptions opts = new NccOptions();
                    opts.checkSizes = false;
                    NccResults res = Ncc.compare(schData, null, layData, null, opts);
                    System.out.println(">>> NCC Result: " + res + " <<<");
                    System.out.println("  Match: " + res.match());
                    System.out.println("  Exports Match: " + res.exportMatch());
                    System.out.println("  Topology Match: " + res.topologyMatch());

                    // ==========================================
                    // 4. Save Library & Export SPICE
                    // ==========================================
                    if (errLog.getNumErrors() == 0 && res.match()) {
                        System.out.println("--- Saving SRAM.jelib ---");
                        Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                        System.out.println("Library save status: SUCCESS");

                        System.out.println("--- Exporting SPICE deck: SRAM_Core_Datapath.spi ---");
                        Spice.SpicePreferences sp = 
                            (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, schData, true, null);
                        sp.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                        sp.level = "3";
                        sp.doOutput(schData, null, "SRAM_Core_Datapath.spi");
                        File spiFile = new File("SRAM_Core_Datapath.spi");
                        System.out.println("SPICE deck exported successfully (" + spiFile.length() + " bytes).");
                    } else {
                        System.err.println("Verification FAILED! Library will not be saved.");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(8000);
    }
}
