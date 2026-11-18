import java.io.File;
import java.util.*;
import java.lang.reflect.Method;
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Setting;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class SimulateExactGuiCheck {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("SimulateExactGuiCheck", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String p = "/Users/harshil_rathan/.gemini/antigravity/brain/4b2b74ac-eb63-4490-a996-70074e0522bc/.user_uploaded/media_1795002561961_7f7dd016.jelib";
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), new File(p).toURI().toURL(), null, FileType.JELIB, false);

                    // Check without setting scale, check with setting scale
                    // Check hierarchical DRC checkHierarchicalDRC
                    Method[] methods = DRC.class.getDeclaredMethods();
                    System.out.println("Available DRC methods:");
                    for (Method m : methods) {
                        if (m.getName().toLowerCase().contains("hier") || m.getName().toLowerCase().contains("check")) {
                            System.out.println("  " + m.getName() + "(" + Arrays.toString(m.getParameterTypes()) + ")");
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
