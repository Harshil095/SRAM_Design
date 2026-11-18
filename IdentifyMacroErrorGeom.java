
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.geometry.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.util.*;

public class IdentifyMacroErrorGeom {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("IdentifyMacroErrorGeom", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell macro = lib.findNodeProto("SRAM_256_Macro{lay}");

                    double[][] testPts = {
                        {-75.0, 1212.25}, // E1
                        {-75.0, 362.25},  // E2
                        {453.5, 2755.5},  // E3
                        {-166.5, 2755.5}, // E4
                        {397.0, 2208.75}, // E5
                        {-302.5, 1315.75} // E12
                    };

                    for (int p = 0; p < testPts.length; p++) {
                        double tx = testPts[p][0];
                        double ty = testPts[p][1];
                        System.out.println("\n=== Test Point (" + tx + ", " + ty + ") ===");

                        // Check macro level primitives
                        for (Iterator<NodeInst> it = macro.getNodes(); it.hasNext(); ) {
                            NodeInst ni = it.next();
                            if (ni.getProto() instanceof Cell) continue;
                            if (Math.abs(ni.getTrueCenter().getX() - tx) < 20 && Math.abs(ni.getTrueCenter().getY() - ty) < 20) {
                                System.out.println("  Macro Node: " + ni.getName() + " of " + ni.getProto().getName() + " at " + ni.getTrueCenter() + " bounds=" + ni.getBounds());
                            }
                        }
                        for (Iterator<ArcInst> it = macro.getArcs(); it.hasNext(); ) {
                            ArcInst ai = it.next();
                            double x0 = ai.getHeadLocation().getX(), y0 = ai.getHeadLocation().getY();
                            double x1 = ai.getTailLocation().getX(), y1 = ai.getTailLocation().getY();
                            double minX = Math.min(x0, x1), maxX = Math.max(x0, x1);
                            double minY = Math.min(y0, y1), maxY = Math.max(y0, y1);
                            if (tx >= minX - 10 && tx <= maxX + 10 && ty >= minY - 10 && ty <= maxY + 10) {
                                System.out.println("  Macro Arc: " + ai.getName() + " layer=" + ai.getProto().getName() + " from (" + x0 + "," + y0 + ") to (" + x1 + "," + y1 + ") width=" + ai.getLambdaBaseWidth());
                            }
                        }

                        // Check subcells
                        for (Iterator<NodeInst> it = macro.getNodes(); it.hasNext(); ) {
                            NodeInst sub = it.next();
                            if (!(sub.getProto() instanceof Cell)) continue;
                            Cell subCell = (Cell) sub.getProto();
                            // Transform point to subcell coords
                            java.awt.geom.Point2D ptSub = sub.transformIn().transform(new java.awt.geom.Point2D.Double(tx, ty), null);
                            for (Iterator<NodeInst> nit = subCell.getNodes(); nit.hasNext(); ) {
                                NodeInst ni = nit.next();
                                if (ni.getProto() instanceof Cell) continue;
                                if (Math.abs(ni.getTrueCenter().getX() - ptSub.getX()) < 10 && Math.abs(ni.getTrueCenter().getY() - ptSub.getY()) < 10) {
                                    System.out.println("  In Subcell " + sub.getName() + " (" + subCell.getName() + "): Node " + ni.getName() + " of " + ni.getProto().getName() + " at " + ni.getTrueCenter() + " (macro: " + sub.transformOut().transform(ni.getTrueCenter(), null) + ")");
                                }
                            }
                            for (Iterator<ArcInst> ait = subCell.getArcs(); ait.hasNext(); ) {
                                ArcInst ai = ait.next();
                                java.awt.geom.Point2D p0 = sub.transformOut().transform(ai.getHeadLocation(), null);
                                java.awt.geom.Point2D p1 = sub.transformOut().transform(ai.getTailLocation(), null);
                                double minX = Math.min(p0.getX(), p1.getX()), maxX = Math.max(p0.getX(), p1.getX());
                                double minY = Math.min(p0.getY(), p1.getY()), maxY = Math.max(p0.getY(), p1.getY());
                                if (tx >= minX - 10 && tx <= maxX + 10 && ty >= minY - 10 && ty <= maxY + 10) {
                                    System.out.println("  In Subcell " + sub.getName() + " (" + subCell.getName() + "): Arc " + ai.getName() + " layer=" + ai.getProto().getName() + " from " + p0 + " to " + p1);
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(6000);
        System.exit(0);
    }
}
