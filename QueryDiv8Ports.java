import java.util.Iterator;
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.net.URL;

public class QueryDiv8Ports {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("QueryDiv8Ports", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);
                    Cell c = lib.findNodeProto("Div8_Counter{lay}");
                    for (Iterator<Export> it = c.getExports(); it.hasNext(); ) {
                        Export exp = it.next();
                        System.out.printf("Div8 Export %-15s at (%.1f, %.1f)%n",
                            exp.getName(), exp.getOriginalPort().getCenter().getX(), exp.getOriginalPort().getCenter().getY());
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
