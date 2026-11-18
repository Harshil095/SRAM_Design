
import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.simulation.SimulationTool;
import com.sun.electric.tool.user.User;
import java.io.File;

public class ExportChipSpice {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("ExportChipSpice", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell chipSch = lib.findNodeProto("SRAM_Chip_Final{sch}");

                    System.out.println("Exporting SPICE deck: SRAM_Chip_Final.spi");
                    Spice.SpicePreferences sp = (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, chipSch, true, null);
                    sp.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                    sp.level = "3";
                    sp.doOutput(chipSch, null, "SRAM_Chip_Final.spi");
                    System.out.println("Export completed successfully!");
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(4000);
        System.exit(0);
    }
}
