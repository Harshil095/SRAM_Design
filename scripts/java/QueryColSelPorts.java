import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.net.URL;

public class QueryColSelPorts {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("QueryColSelPorts", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("SRAM_Core_Datapath{lay}");
                    for (int i = 0; i < 16; i++) {
                        Export expSel = c.findExport("COL_SEL[" + i + "]");
                        Export expSelB = c.findExport("COL_SEL_B[" + i + "]");
                        System.out.printf("COL_SEL[%d] at (%.1f, %.1f), COL_SEL_B[%d] at (%.1f, %.1f)%n",
                            i, expSel.getOriginalPort().getCenter().getX(), expSel.getOriginalPort().getCenter().getY(),
                            i, expSelB.getOriginalPort().getCenter().getX(), expSelB.getOriginalPort().getCenter().getY());
                    }
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
