import java.io.File;
import java.util.*;
import java.awt.geom.Point2D;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;

public class AnalyzeExactFixes {

    static void analyzeSenseAmp(Cell uCell, Cell fCell) {
        System.out.println("==================================================");
        System.out.println("ANALYSIS 1: sense_amplifier");
        System.out.println("==================================================");
        // Look for contact@6 and via_se_m12, net_se_m1
        System.out.println("--- USER NODES (Around contact@6 / via_se_m12) ---");
        for (Iterator<NodeInst> it = uCell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            String name = ni.getName();
            if (name.contains("contact@6") || name.contains("se") || ni.getProto().getName().contains("Contact") || ni.getProto().getName().contains("Con")) {
                System.out.println("  User Node: " + ni.getProto().getName() + " '" + name + "' @ " + ni.getAnchorCenter() + " size=" + ni.getXSize() + "x" + ni.getYSize());
            }
        }
        System.out.println("--- FRIEND NODES (Around contact@6 / via_se_m12) ---");
        for (Iterator<NodeInst> it = fCell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            String name = ni.getName();
            if (name.contains("contact@6") || name.contains("se") || ni.getProto().getName().contains("Contact") || ni.getProto().getName().contains("Con")) {
                System.out.println("  Friend Node: " + ni.getProto().getName() + " '" + name + "' @ " + ni.getAnchorCenter() + " size=" + ni.getXSize() + "x" + ni.getYSize());
            }
        }

        System.out.println("--- USER ARCS with 'se' ---");
        for (Iterator<ArcInst> it = uCell.getArcs(); it.hasNext(); ) {
            ArcInst ai = it.next();
            if (ai.getName().contains("se")) {
                System.out.println("  User Arc: " + ai.getProto().getName() + " '" + ai.getName() + "' head=" + ai.getHeadLocation() + " tail=" + ai.getTailLocation() + " width=" + ai.getLambdaBaseWidth());
            }
        }
        System.out.println("--- FRIEND ARCS with 'se' ---");
        for (Iterator<ArcInst> it = fCell.getArcs(); it.hasNext(); ) {
            ArcInst ai = it.next();
            if (ai.getName().contains("se")) {
                System.out.println("  Friend Arc: " + ai.getProto().getName() + " '" + ai.getName() + "' head=" + ai.getHeadLocation() + " tail=" + ai.getTailLocation() + " width=" + ai.getLambdaBaseWidth());
            }
        }
    }

    static void analyzeWlDriverGate(Cell uCell, Cell fCell) {
        System.out.println("\n==================================================");
        System.out.println("ANALYSIS 2: wl_driver_gate");
        System.out.println("==================================================");
        // Look for net_enb_m2_0 and net_wlin_m2
        System.out.println("--- USER ARCS (Metal-2) ---");
        for (Iterator<ArcInst> it = uCell.getArcs(); it.hasNext(); ) {
            ArcInst ai = it.next();
            if (ai.getProto().getName().contains("Metal-2") || ai.getName().contains("m2") || ai.getName().contains("wlin") || ai.getName().contains("enb")) {
                System.out.println("  User Arc: " + ai.getProto().getName() + " '" + ai.getName() + "' head=" + ai.getHeadLocation() + " tail=" + ai.getTailLocation() + " width=" + ai.getLambdaBaseWidth());
            }
        }
        System.out.println("--- FRIEND ARCS (Metal-2) ---");
        for (Iterator<ArcInst> it = fCell.getArcs(); it.hasNext(); ) {
            ArcInst ai = it.next();
            if (ai.getProto().getName().contains("Metal-2") || ai.getName().contains("m2") || ai.getName().contains("wlin") || ai.getName().contains("enb")) {
                System.out.println("  Friend Arc: " + ai.getProto().getName() + " '" + ai.getName() + "' head=" + ai.getHeadLocation() + " tail=" + ai.getTailLocation() + " width=" + ai.getLambdaBaseWidth());
            }
        }
    }

    static void analyzeWlDriverX16(Cell uCell, Cell fCell) {
        System.out.println("\n==================================================");
        System.out.println("ANALYSIS 3: wl_driver_x16");
        System.out.println("==================================================");
        // Compare VDD and other bus routing in wl_driver_x16
        System.out.println("User Arc Count: " + uCell.getNumArcs() + " | Friend Arc Count: " + fCell.getNumArcs());
        // Find differences in Metal-2 arcs in wl_driver_x16
        Map<String, String> uArcs = new HashMap<>();
        for (Iterator<ArcInst> it = uCell.getArcs(); it.hasNext(); ) {
            ArcInst ai = it.next();
            uArcs.put(ai.getName(), ai.getProto().getName() + " (" + ai.getHeadLocation() + " -> " + ai.getTailLocation() + ") w=" + ai.getLambdaBaseWidth());
        }
        Map<String, String> fArcs = new HashMap<>();
        for (Iterator<ArcInst> it = fCell.getArcs(); it.hasNext(); ) {
            ArcInst ai = it.next();
            fArcs.put(ai.getName(), ai.getProto().getName() + " (" + ai.getHeadLocation() + " -> " + ai.getTailLocation() + ") w=" + ai.getLambdaBaseWidth());
        }

        Set<String> allNames = new TreeSet<>();
        allNames.addAll(uArcs.keySet());
        allNames.addAll(fArcs.keySet());
        int diffCount = 0;
        for (String name : allNames) {
            String uDesc = uArcs.get(name);
            String fDesc = fArcs.get(name);
            if (!Objects.equals(uDesc, fDesc)) {
                diffCount++;
                if (diffCount <= 30) {
                    System.out.println("  Arc '" + name + "':");
                    System.out.println("    User:   " + uDesc);
                    System.out.println("    Friend: " + fDesc);
                }
            }
        }
        System.out.println("Total Arc Differences in wl_driver_x16: " + diffCount);

        // Check node differences
        Map<String, String> uNodes = new HashMap<>();
        for (Iterator<NodeInst> it = uCell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            uNodes.put(ni.getName(), ni.getProto().getName() + " @ " + ni.getAnchorCenter());
        }
        Map<String, String> fNodes = new HashMap<>();
        for (Iterator<NodeInst> it = fCell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            fNodes.put(ni.getName(), ni.getProto().getName() + " @ " + ni.getAnchorCenter());
        }
        int nodeDiffs = 0;
        for (String name : uNodes.keySet()) {
            if (!Objects.equals(uNodes.get(name), fNodes.get(name))) {
                nodeDiffs++;
                if (nodeDiffs <= 20) {
                    System.out.println("  Node '" + name + "': User=" + uNodes.get(name) + " vs Friend=" + fNodes.get(name));
                }
            }
        }
        System.out.println("Total Node Differences in wl_driver_x16: " + nodeDiffs);
    }

    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("AnalyzeExactFixes", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String p1 = "/Users/harshil_rathan/Documents/BTech IITH/SEM5/VLSI/SRAM_Complete_V1_suraj.jelib";
                    String p2 = "/Users/harshil_rathan/Downloads/SRAM_Complete_V1_suraj_antigravity_BACKUP_DRC0_LVS0.jelib";

                    Library uLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p1).toURI().toURL(), null, FileType.JELIB, false);
                    Library fLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p2).toURI().toURL(), null, FileType.JELIB, false);

                    analyzeSenseAmp(uLib.findNodeProto("sense_amplifier{lay}"), fLib.findNodeProto("sense_amplifier{lay}"));
                    analyzeWlDriverGate(uLib.findNodeProto("wl_driver_gate{lay}"), fLib.findNodeProto("wl_driver_gate{lay}"));
                    analyzeWlDriverX16(uLib.findNodeProto("wl_driver_x16{lay}"), fLib.findNodeProto("wl_driver_x16{lay}"));

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
