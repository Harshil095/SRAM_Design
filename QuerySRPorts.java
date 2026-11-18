import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.net.URL;

public class QuerySRPorts {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("QuerySRPorts", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("ShiftReg_8bit{lay}");
                    for (int i = 0; i < 8; i++) {
                        Export exp = c.findExport("Q[" + i + "]");
                        System.out.printf("ShiftReg Q[%d] at (%.1f, %.1f)%n",
                            i, exp.getOriginalPort().getCenter().getX(), exp.getOriginalPort().getCenter().getY());
                    }
                    Export expClk = c.findExport("CLK");
                    Export expAddr = c.findExport("ADDR_IN");
                    Export expVdd = c.findExport("vdd");
                    Export expGnd = c.findExport("gnd");
                    System.out.printf("ShiftReg CLK: (%.1f, %.1f), ADDR_IN: (%.1f, %.1f), vdd: (%.1f, %.1f), gnd: (%.1f, %.1f)%n",
                        expClk.getOriginalPort().getCenter().getX(), expClk.getOriginalPort().getCenter().getY(),
                        expAddr.getOriginalPort().getCenter().getX(), expAddr.getOriginalPort().getCenter().getY(),
                        expVdd.getOriginalPort().getCenter().getX(), expVdd.getOriginalPort().getCenter().getY(),
                        expGnd.getOriginalPort().getCenter().getX(), expGnd.getOriginalPort().getCenter().getY());
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
