import java.io.File;
import java.net.URL;
import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.drc.Quick;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

public class BuildSRAMFinal {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("BuildSRAMFinal", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    String userPath = "/Users/harshil_rathan/.gemini/antigravity/brain/4b2b74ac-eb63-4490-a996-70074e0522bc/.user_uploaded/media_1795002561961_7f7dd016.jelib";
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), new File(userPath).toURI().toURL(), null, FileType.JELIB, false);

                    System.out.println("Loaded library: " + lib.getName());

                    // 1. Fix orphan pin in Precharge_Bank_16{lay}
                    Cell pre = lib.findNodeProto("Precharge_Bank_16{lay}");
                    if (pre != null) {
                        List<NodeInst> toKill = new ArrayList<>();
                        for (Iterator<NodeInst> it = pre.getNodes(); it.hasNext(); ) {
                            NodeInst ni = it.next();
                            if (ni.getName().equals("pin@0") && ni.getNumConnections() == 0) {
                                toKill.add(ni);
                            }
                        }
                        for (NodeInst ni : toKill) {
                            System.out.println("Killed orphan pin in Precharge_Bank_16: " + ni.getName());
                            ni.kill();
                        }
                    }

                    // 2. Set technology scale to 90.0 nm
                    EDatabase db = lib.getDatabase();
                    Setting.SettingChangeBatch scb = new Setting.SettingChangeBatch();
                    Technology mocmos = Technology.findTechnology("mocmos");
                    scb.add(mocmos.getScaleSetting(), Double.valueOf(90.0));
                    db.implementSettingChanges(scb);

                    // Also set library variable ScaleFORmocmos if available
                    lib.newVar("ScaleFORmocmos", Double.valueOf(90.0), getEditingPreferences());

                    // 3. Rename library to SRAM_final and set file URL
                    lib.setName("SRAM_final");
                    File outFile = new File("SRAM_final.jelib");
                    lib.setLibFile(outFile.toURI().toURL());

                    // 4. Write library using LibrarySave
                    com.sun.electric.tool.io.output.LibrarySave.writeLibrary(lib, outFile, null, FileType.JELIB, false);
                    System.out.println("LibrarySave.writeLibrary completed: " + outFile.getAbsolutePath());

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
