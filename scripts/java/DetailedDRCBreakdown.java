import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.HashMap;

public class DetailedDRCBreakdown {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("DetailedDRCBreakdown", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell lay = lib.findNodeProto("SRAM_256_Macro{lay}");
                    ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                    DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                    com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, lay, null, null, null, null, dp, com.sun.electric.database.geometry.GeometryHandler.GHMode.ALGO_SWEEP, false);
                    
                    HashMap<String, NodeInst> nodes = new HashMap<String, NodeInst>();
                    for (Iterator it = lay.getNodes(); it.hasNext(); ) {
                        NodeInst ni = (NodeInst)it.next();
                        nodes.put(ni.getName(), ni);
                    }
                    HashMap<String, ArcInst> arcs = new HashMap<String, ArcInst>();
                    for (Iterator it = lay.getArcs(); it.hasNext(); ) {
                        ArcInst ai = (ArcInst)it.next();
                        arcs.put(ai.getName(), ai);
                    }

                    PrintWriter pw = new PrintWriter("drc_breakdown.txt");
                    int count = 0;
                    for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
                        String msg = it.next().getMessageString();
                        pw.println("[" + (++count) + "] " + msg);
                        String[] words = msg.replace("'", " ").replace("[", " ").replace("]", " ").split("\\s+");
                        for (String word : words) {
                            if (nodes.containsKey(word)) {
                                NodeInst ni = nodes.get(word);
                                pw.println("    Node: " + word + " proto=" + ni.getProto().getName() + " center=" + ni.getAnchorCenter() + " bounds=" + ni.getBounds());
                            } else if (arcs.containsKey(word)) {
                                ArcInst ai = arcs.get(word);
                                pw.println("    Arc: " + word + " proto=" + ai.getProto().getName() + " head=" + ai.getHeadPortInst().getNodeInst().getAnchorCenter() + " tail=" + ai.getTailPortInst().getNodeInst().getAnchorCenter());
                            }
                        }
                    }
                    pw.close();
                    System.out.println("Finished writing drc_breakdown.txt with " + count + " errors.");
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
