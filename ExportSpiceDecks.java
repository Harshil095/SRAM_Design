import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.simulation.SimulationTool;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.net.URL;

public class ExportSpiceDecks {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("ExportSpiceDecks", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell macro = lib.findNodeProto("SRAM_256_Macro{sch}");
                    Cell chip = lib.findNodeProto("SRAM_Chip_Final{sch}");

                    Spice.SpicePreferences spMacro = (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, macro, true, null);
                    spMacro.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                    spMacro.level = "3";
                    spMacro.doOutput(macro, null, "SRAM_256_Macro_clean.spi");
                    System.out.println("Exported SRAM_256_Macro_clean.spi successfully.");

                    Spice.SpicePreferences spChip = (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, chip, true, null);
                    spChip.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                    spChip.level = "3";
                    spChip.doOutput(chip, null, "SRAM_Chip_Final_clean.spi");
                    System.out.println("Exported SRAM_Chip_Final_clean.spi successfully.");

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
