import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Iterator;

public class ExportSchSvgData {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("ExportSvgData", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM_final.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell sch = lib.findNodeProto("SRAM_256_Macro{sch}");

                    PrintWriter out = new PrintWriter("macro_sch_data.txt");
                    out.println("# NODES: name,proto,x,y,w,h");
                    for (Iterator<NodeInst> it = sch.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        out.printf("NODE|%s|%s|%.2f|%.2f|%.2f|%.2f%n",
                            ni.getName(),
                            ni.getProto().getName(),
                            ni.getAnchorCenter().getX(),
                            ni.getAnchorCenter().getY(),
                            ni.getProto().getDefWidth(getEditingPreferences()),
                            ni.getProto().getDefHeight(getEditingPreferences()));
                    }

                    out.println("# ARCS: headX,headY,tailX,tailY");
                    for (Iterator<ArcInst> it = sch.getArcs(); it.hasNext(); ) {
                        ArcInst ai = it.next();
                        out.printf("ARC|%.2f|%.2f|%.2f|%.2f%n",
                            ai.getHeadLocation().getX(),
                            ai.getHeadLocation().getY(),
                            ai.getTailLocation().getX(),
                            ai.getTailLocation().getY());
                    }

                    out.println("# EXPORTS: name,x,y");
                    for (Iterator<Export> it = sch.getExports(); it.hasNext(); ) {
                        Export exp = it.next();
                        PortInst pi = exp.getOriginalPort();
                        out.printf("EXP|%s|%.2f|%.2f%n",
                            exp.getName(),
                            pi.getNodeInst().getAnchorCenter().getX(),
                            pi.getNodeInst().getAnchorCenter().getY());
                    }
                    out.close();
                    System.out.println("Exported macro_sch_data.txt successfully!");
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
