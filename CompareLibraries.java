import java.io.File;
import java.net.URL;
import java.util.*;
import java.lang.reflect.Method;
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorHighlight;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class CompareLibraries {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("CompareLibraries", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String friendPath = "/Users/harshil_rathan/.gemini/antigravity/brain/4b2b74ac-eb63-4490-a996-70074e0522bc/.user_uploaded/media_1795001289210_12b2d076.jelib";
                    
                    System.out.println("=== LOADING FRIEND LIBRARY ===");
                    Map<Setting, Object> friendSettings = new HashMap<>();
                    Library friendLib = LibraryFiles.readLibrary(getEditingPreferences(), new File(friendPath).toURI().toURL(), null, FileType.JELIB, false, friendSettings);
                    System.out.println("Friend lib name: " + friendLib.getName());
                    System.out.println("Friend settings: " + friendSettings);
                    
                    System.out.println("\n=== LOADING USER LIBRARIES ===");
                    File[] userFiles = new File[]{ new File("SRAM_Complete_V1.jelib"), new File("SRAM.jelib") };
                    for (File uf : userFiles) {
                        if (!uf.exists()) continue;
                        System.out.println("\n--- Checking: " + uf.getName() + " ---");
                        Map<Setting, Object> userSettings = new HashMap<>();
                        Library uLib = LibraryFiles.readLibrary(getEditingPreferences(), uf.toURI().toURL(), null, FileType.JELIB, false, userSettings);
                        System.out.println("User lib settings: " + userSettings);
                    }

                    // List cells in friend lib
                    System.out.println("\n=== CELLS IN FRIEND LIB ===");
                    List<String> fCells = new ArrayList<>();
                    for (Iterator<Cell> it = friendLib.getCells(); it.hasNext(); ) {
                        Cell c = it.next();
                        fCells.add(c.getName() + "{" + c.getView() + "}");
                    }
                    Collections.sort(fCells);
                    for (String s : fCells) {
                        System.out.println("  " + s);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(5000);
    }
}
