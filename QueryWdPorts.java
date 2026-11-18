import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.net.URL;
import java.util.Iterator;

public class QueryWdPorts {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("QueryWdPorts", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("write_driver{lay}");
                    Cell testCell = Cell.makeInstance(lib, "Test_Wd_Ports{lay}");
                    NodeInst wd = NodeInst.makeInstance(c, new java.awt.geom.Point2D.Double(0.0, -340.0), 0, 0, testCell);
                    for (Iterator<Export> it = c.getExports(); it.hasNext(); ) {
                        Export exp = it.next();
                        PortInst p = wd.findPortInstFromProto(exp);
                        System.out.printf("WD Export %-10s at (%.2f, %.2f) protoPort=%s%n",
                            exp.getName(), p.getCenter().getX(), p.getCenter().getY(), p.getPortProto().getName());
                    }
                    testCell.kill();
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
