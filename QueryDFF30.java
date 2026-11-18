
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

public class QueryDFF30 {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("QueryDFF30", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell dff = lib.findNodeProto("DFF{lay}");

                    for (Iterator it = dff.getArcs(); it.hasNext(); ) {
                        ArcInst ai = (ArcInst) it.next();
                        double x1 = ai.getHeadLocation().getX(), y1 = ai.getHeadLocation().getY();
                        double x2 = ai.getTailLocation().getX(), y2 = ai.getTailLocation().getY();
                        if (Math.abs(x1 - 30.0) < 20.0 || Math.abs(x2 - 30.0) < 20.0) {
                            if (Math.abs(y1 - 0.0) < 10.0 || Math.abs(y2 - 0.0) < 10.0) {
                                System.out.println("Arc: " + ai.getProto().getName() + " from (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ") width: " + ai.getLambdaBaseWidth());
                            }
                        }
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
