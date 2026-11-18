import java.io.File;
import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;

public class AnalyzeTopLevelFixes {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("AnalyzeTopLevelFixes", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String p1 = "/Users/harshil_rathan/Documents/BTech IITH/SEM5/VLSI/SRAM_Complete_V1_suraj.jelib";
                    String p2 = "/Users/harshil_rathan/Downloads/SRAM_Complete_V1_suraj_antigravity_BACKUP_DRC0_LVS0.jelib";

                    Library uLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p1).toURI().toURL(), null, FileType.JELIB, false);
                    Library fLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p2).toURI().toURL(), null, FileType.JELIB, false);

                    Cell uc = uLib.findNodeProto("sram_edgetriggered{lay}");
                    Cell fc = fLib.findNodeProto("sram_edgetriggered{lay}");

                    System.out.println("=== 1. PULSE_b ARC COMPARISON ===");
                    for (Iterator<ArcInst> it = uc.getArcs(); it.hasNext(); ) {
                        ArcInst a = it.next();
                        if (a.getName().contains("PULSE") || a.getName().contains("pulse")) {
                            System.out.println("  User Pulse Arc: " + a.getName() + " layer=" + a.getProto().getName() + " " + a.getHeadLocation() + " -> " + a.getTailLocation());
                        }
                    }
                    for (Iterator<ArcInst> it = fc.getArcs(); it.hasNext(); ) {
                        ArcInst a = it.next();
                        if (a.getName().contains("PULSE") || a.getName().contains("pulse")) {
                            System.out.println("  Friend Pulse Arc: " + a.getName() + " layer=" + a.getProto().getName() + " " + a.getHeadLocation() + " -> " + a.getTailLocation());
                        }
                    }

                    System.out.println("\n=== 2. ADDR_IN & ADDR_0..3 COMPARISON ===");
                    for (Iterator<ArcInst> it = uc.getArcs(); it.hasNext(); ) {
                        ArcInst a = it.next();
                        if (a.getName().startsWith("ADDR")) {
                            System.out.println("  User ADDR Arc: " + a.getName() + " layer=" + a.getProto().getName() + " " + a.getHeadLocation() + " -> " + a.getTailLocation());
                        }
                    }
                    for (Iterator<ArcInst> it = fc.getArcs(); it.hasNext(); ) {
                        ArcInst a = it.next();
                        if (a.getName().startsWith("ADDR")) {
                            System.out.println("  Friend ADDR Arc: " + a.getName() + " layer=" + a.getProto().getName() + " " + a.getHeadLocation() + " -> " + a.getTailLocation());
                        }
                    }

                    System.out.println("\n=== 3. CLK, VDD, GND BUS COMPARISON ===");
                    // Check CLK arcs
                    for (Iterator<ArcInst> it = uc.getArcs(); it.hasNext(); ) {
                        ArcInst a = it.next();
                        if (a.getName().equals("CLK") || a.getName().startsWith("CLK_")) {
                            System.out.println("  User CLK Arc: " + a.getName() + " layer=" + a.getProto().getName() + " " + a.getHeadLocation() + " -> " + a.getTailLocation());
                        }
                    }
                    for (Iterator<ArcInst> it = fc.getArcs(); it.hasNext(); ) {
                        ArcInst a = it.next();
                        if (a.getName().equals("CLK") || a.getName().startsWith("CLK_")) {
                            System.out.println("  Friend CLK Arc: " + a.getName() + " layer=" + a.getProto().getName() + " " + a.getHeadLocation() + " -> " + a.getTailLocation());
                        }
                    }

                    System.out.println("\n=== 4. VIA_GBLB SPACING COMPARISON ===");
                    for (Iterator<NodeInst> it = uc.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        if (ni.getName().contains("gblb")) {
                            System.out.println("  User GBLB Node: " + ni.getName() + " " + ni.getProto().getName() + " @ " + ni.getAnchorCenter());
                        }
                    }
                    for (Iterator<NodeInst> it = fc.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        if (ni.getName().contains("gblb")) {
                            System.out.println("  Friend GBLB Node: " + ni.getName() + " " + ni.getProto().getName() + " @ " + ni.getAnchorCenter());
                        }
                    }

                    System.out.println("\n=== 5. EXTRA INVERTER IN FRIEND ===");
                    for (Iterator<NodeInst> it = fc.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        if (ni.getProto() instanceof Cell && ((Cell)ni.getProto()).getName().equals("inverter")) {
                            System.out.println("  Friend Inverter Node: " + ni.getName() + " @ " + ni.getAnchorCenter());
                            for (Iterator cit = ni.getConnections(); cit.hasNext(); ) {
                                com.sun.electric.database.topology.Connection con = (com.sun.electric.database.topology.Connection) cit.next();
                                ArcInst a = con.getArc();
                                System.out.println("    connected arc: " + a.getName() + " (" + a.getProto().getName() + ") " + a.getHeadLocation() + " -> " + a.getTailLocation());
                            }
                        }
                    }

                    System.out.println("\n=== 6. WD, WE, R_DATA, GND IN USER VS FRIEND ===");
                    for (Iterator<ArcInst> it = uc.getArcs(); it.hasNext(); ) {
                        ArcInst a = it.next();
                        if (a.getName().equals("WD") || a.getName().equals("WE") || a.getName().equals("R_data")) {
                            System.out.println("  User: " + a.getName() + " (" + a.getProto().getName() + ") " + a.getHeadLocation() + " -> " + a.getTailLocation());
                        }
                    }
                    for (Iterator<ArcInst> it = fc.getArcs(); it.hasNext(); ) {
                        ArcInst a = it.next();
                        if (a.getName().equals("WD") || a.getName().equals("WE") || a.getName().equals("R_data")) {
                            System.out.println("  Friend: " + a.getName() + " (" + a.getProto().getName() + ") " + a.getHeadLocation() + " -> " + a.getTailLocation());
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(6000);
    }
}
