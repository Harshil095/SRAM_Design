import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class SearchCleanRoute {

    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("SearchCleanRoute", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell lay = lib.findNodeProto("SRAM_256_Macro{lay}");

                    ArcProto m1 = lay.getTechnology().findArcProto("Metal-1");
                    NodeProto m1Pin = lay.getTechnology().findNodeProto("Metal-1-Pin");

                    Export expDout = lay.findExport("DOUT");
                    if (expDout != null) {
                        PortInst pi = expDout.getOriginalPort();
                        NodeInst oldPin = pi.getNodeInst();
                        List<ArcInst> arcsToKill = new ArrayList<>();
                        for (Iterator<Connection> it = pi.getConnections(); it.hasNext(); ) {
                            arcsToKill.add(it.next().getArc());
                        }
                        for (ArcInst ai : arcsToKill) ai.kill();
                        expDout.kill();
                        oldPin.kill();
                    }

                    NodeInst instSa = null;
                    for (Iterator<NodeInst> it = lay.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        if (ni.getProto().getName().contains("sense_amplifier")) {
                            instSa = ni;
                            break;
                        }
                    }

                    Cell saProto = (Cell) instSa.getProto();
                    PortInst pSaDout = instSa.findPortInstFromProto(saProto.findExport("SO"));

                    // Try different Y levels for horizontal DOUT wire
                    // from -558.0 to -548.0 with small steps
                    double[] testY = {-557.0, -556.5, -556.0, -555.5, -555.0, -554.5, -554.0, -553.5, -553.0};
                    for (double y : testY) {
                        // Make connection directly or with a tiny jog at SA
                        NodeInst p1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(213.5, y), 0, 0, lay);
                        NodeInst pDout = NodeInst.makeInstance(m1Pin, new Point2D.Double(1238.0, y), 0, 0, lay);
                        Export exp = Export.newInst(lay, pDout.getPortInst(0), "DOUT", getEditingPreferences());

                        ArcInst a1 = null;
                        if (Math.abs(y - (-555.0)) > 0.01) {
                            a1 = ArcInst.makeInstance(m1, getEditingPreferences(), pSaDout, p1.getPortInst(0));
                        }
                        ArcInst a2 = ArcInst.makeInstance(m1, getEditingPreferences(), (a1 != null ? p1.getPortInst(0) : pSaDout), pDout.getPortInst(0));

                        ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                        DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                        dp.ignoreAreaCheck = false;
                        dp.ignoreExtensionRuleChecking = false;
                        dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;
                        dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_DEFAULT;

                        Quick.checkDesignRules(errLog, lay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                        errLog.termLogging(true);

                        System.out.println("y = " + y + " -> Errors: " + errLog.getNumErrors());
                        if (errLog.getNumErrors() <= 2) {
                            for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                                System.out.println("   " + it.next().getMessageString());
                            }
                        }

                        // Clean up for next iteration
                        exp.kill();
                        if (a1 != null) a1.kill();
                        a2.kill();
                        p1.kill();
                        pDout.kill();
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
