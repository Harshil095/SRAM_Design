import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.simulation.SimulationTool;
import com.sun.electric.tool.user.User;
import java.awt.geom.Rectangle2D;
import java.io.File;

public class ExportAndMeasure {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("ExportAndMeasure", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), f.toURI().toURL(), null, FileType.JELIB, false);
                    Cell chipLay = lib.findNodeProto("SRAM_Chip_Final{lay}");
                    Cell macroLay = lib.findNodeProto("SRAM_256_Macro{lay}");

                    Rectangle2D bChip = chipLay.getBounds();
                    Rectangle2D bMacro = macroLay.getBounds();

                    System.out.println("SRAM_256_Macro Bounds: X=[" + bMacro.getMinX() + ", " + bMacro.getMaxX() + "] (W=" + bMacro.getWidth() + "), Y=[" + bMacro.getMinY() + ", " + bMacro.getMaxY() + "] (H=" + bMacro.getHeight() + ")");
                    System.out.println("SRAM_Chip_Final Bounds: X=[" + bChip.getMinX() + ", " + bChip.getMaxX() + "] (W=" + bChip.getWidth() + "), Y=[" + bChip.getMinY() + ", " + bChip.getMaxY() + "] (H=" + bChip.getHeight() + ")");

                    double scaleNm = 90.0;
                    double chipW_um = bChip.getWidth() * scaleNm / 1000.0;
                    double chipH_um = bChip.getHeight() * scaleNm / 1000.0;
                    double chipArea_mm2 = (chipW_um * chipH_um) / 1000000.0;
                    System.out.println("Physical Dimensions: " + chipW_um + " um x " + chipH_um + " um (Area = " + chipArea_mm2 + " mm^2)");

                    Spice.SpicePreferences sp = (Spice.SpicePreferences) Output.getOutputPreferences(FileType.SPICE, chipLay, true, null);
                    sp.engine = SimulationTool.SpiceEngine.SPICE_ENGINE_NG;
                    sp.level = "3";
                    sp.doOutput(chipLay, null, "SRAM_Chip_Final.spi");
                    File spiFile = new File("SRAM_Chip_Final.spi");
                    System.out.println("SPICE deck exported successfully (" + spiFile.length() + " bytes).");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(5000);
        System.exit(0);
    }
}