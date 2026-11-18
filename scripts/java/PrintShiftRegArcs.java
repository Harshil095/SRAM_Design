
import java.io.File;
import java.net.URL;
import java.util.Iterator;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;

public class PrintShiftRegArcs {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("PrintShiftRegArcs", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell sr = lib.findNodeProto("ShiftReg_8bit_Clean{lay}");
                    if (sr == null) sr = lib.findNodeProto("ShiftReg_8bit{lay}");
                    for (Iterator it = sr.getArcs(); it.hasNext(); ) {
                        ArcInst ai = (ArcInst) it.next();
                        double y1 = ai.getHeadLocation().getY();
                        double y2 = ai.getTailLocation().getY();
                        if (Math.abs(y1 - 540.0) < 50.0 || Math.abs(y2 - 540.0) < 50.0) {
                            System.out.println("Arc: " + ai.getProto().getName() + " width=" + ai.getLambdaBaseWidth() + " from (" + ai.getHeadLocation().getX() + "," + y1 + ") to (" + ai.getTailLocation().getX() + "," + y2 + ")");
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(3000);
    }
}
