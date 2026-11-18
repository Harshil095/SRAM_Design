import java.io.File;
import java.net.URL;
import java.util.*;
import java.lang.reflect.Method;
import java.awt.geom.Point2D;
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorHighlight;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class DeepCellDiff {

    static void printCellErrors(Cell cell) {
        DRC.resetDRCDates(true);
        ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
        DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
        dp.ignoreAreaCheck = false;
        dp.ignoreExtensionRuleChecking = false;
        dp.errorType = DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE;
        dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;

        try {
            Method mCheckNet = DRC.class.getDeclaredMethod("checkNetworks", ErrorLogger.class, Cell.class, boolean.class);
            mCheckNet.setAccessible(true);
            mCheckNet.invoke(null, errLog, cell, true);
        } catch (Exception ignored) {}

        Quick.checkDesignRules(errLog, cell, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
        errLog.termLogging(true);
        System.out.println(">>> ERRORS FOR " + cell.getName() + " (Total: " + errLog.getNumErrors() + "):");
        int count = 0;
        for (Iterator<ErrorLogger.MessageLog> it = errLog.getLogs(); it.hasNext(); ) {
            ErrorLogger.MessageLog ml = it.next();
            count++;
            System.out.println("  #" + count + " " + ml.getMessageString());
        }
    }

    static void compareCellDetails(Cell uCell, Cell fCell) {
        System.out.println("=== COMPARING CELL: " + uCell.getName() + " ===");
        System.out.println("User Node Count: " + uCell.getNumNodes() + " | Friend Node Count: " + fCell.getNumNodes());
        System.out.println("User Arc Count:  " + uCell.getNumArcs() + " | Friend Arc Count:  " + fCell.getNumArcs());
        int uExp = 0; for (Iterator it = uCell.getExports(); it.hasNext(); ) { it.next(); uExp++; }
        int fExp = 0; for (Iterator it = fCell.getExports(); it.hasNext(); ) { it.next(); fExp++; }
        System.out.println("User Export Count: " + uExp + " | Friend Export Count: " + fExp);

        // Compare bounding boxes
        System.out.println("User BBox:   " + uCell.getBounds());
        System.out.println("Friend BBox: " + fCell.getBounds());

        // Compare subcell instances if any
        Map<String, List<Point2D>> uInsts = new HashMap<>();
        for (Iterator<NodeInst> it = uCell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            if (ni.getProto() instanceof Cell) {
                String cName = ((Cell)ni.getProto()).getName();
                uInsts.computeIfAbsent(cName, k -> new ArrayList<>()).add(ni.getAnchorCenter());
            }
        }
        Map<String, List<Point2D>> fInsts = new HashMap<>();
        for (Iterator<NodeInst> it = fCell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            if (ni.getProto() instanceof Cell) {
                String cName = ((Cell)ni.getProto()).getName();
                fInsts.computeIfAbsent(cName, k -> new ArrayList<>()).add(ni.getAnchorCenter());
            }
        }
        if (!uInsts.isEmpty() || !fInsts.isEmpty()) {
            System.out.println("Subcell Instances:");
            Set<String> allSub = new TreeSet<>();
            allSub.addAll(uInsts.keySet());
            allSub.addAll(fInsts.keySet());
            for (String sub : allSub) {
                List<Point2D> ul = uInsts.getOrDefault(sub, Collections.emptyList());
                List<Point2D> fl = fInsts.getOrDefault(sub, Collections.emptyList());
                System.out.println("  Subcell " + sub + ": User has " + ul.size() + ", Friend has " + fl.size());
                if (ul.size() <= 16 && fl.size() <= 16) {
                    System.out.println("    User locs:   " + ul);
                    System.out.println("    Friend locs: " + fl);
                }
            }
        }
        System.out.println("------------------------------------------------------------\n");
    }

    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("DeepCellDiff", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String p1 = "/Users/harshil_rathan/Documents/BTech IITH/SEM5/VLSI/SRAM_Complete_V1_suraj.jelib";
                    String p2 = "/Users/harshil_rathan/Downloads/SRAM_Complete_V1_suraj_antigravity_BACKUP_DRC0_LVS0.jelib";

                    Library uLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p1).toURI().toURL(), null, FileType.JELIB, false);
                    Library fLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p2).toURI().toURL(), null, FileType.JELIB, false);

                    String[] targetCells = {"sense_amplifier", "wl_driver_gate", "wl_driver_x16", "sram_edgetriggered"};

                    System.out.println("\n**************** USER CELL DRC ERRORS ****************\n");
                    for (String name : targetCells) {
                        Cell uc = uLib.findNodeProto(name + "{lay}");
                        if (uc != null) {
                            printCellErrors(uc);
                        }
                    }

                    System.out.println("\n**************** DETAILED CELL COMPARISON ****************\n");
                    for (String name : targetCells) {
                        Cell uc = uLib.findNodeProto(name + "{lay}");
                        Cell fc = fLib.findNodeProto(name + "{lay}");
                        if (uc != null && fc != null) {
                            compareCellDetails(uc, fc);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(12000);
    }
}
