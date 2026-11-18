import com.sun.electric.Main;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.ncc.Ncc;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.result.NccResults;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.net.URL;
import java.util.*;

public class FullDatapathRegression {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Job j = new Job("Regression", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER) {
            public boolean doIt() {
                try {
                    File f = new File("SRAM.jelib");
                    URL url = f.toURI().toURL();
                    Library lib = LibraryFiles.readLibrary(getEditingPreferences(), url, null, FileType.JELIB, false);

                    String[] checkCells = {
                        "tgate",
                        "col_mux_1x",
                        "Col_Mux_Bank_16",
                        "pre_charge",
                        "Precharge_Bank_16",
                        "6T",
                        "SRAM_Row_16",
                        "SRAM_Array_16x16",
                        "DFF",
                        "ShiftReg_8bit",
                        "Div8_Counter",
                        "Decoder_4x16",
                        "write_driver",
                        "sense_amplifier",
                        "SRAM_Core_Datapath"
                    };

                    System.out.println("==========================================================================");
                    System.out.println(String.format("%-22s | %-12s | %-15s | %-15s", "Cell Name", "DRC Errors", "DRC Warnings", "NCC Match"));
                    System.out.println("==========================================================================");

                    for (String cName : checkCells) {
                        Cell lay = lib.findNodeProto(cName + "{lay}");
                        Cell sch = lib.findNodeProto(cName + "{sch}");

                        int drcErr = -1;
                        int drcWarn = -1;
                        if (lay != null) {
                            ErrorLogger errLog = DRC.getDRCErrorLogger(true, null);
                            DRC.DRCPreferences dp = new DRC.DRCPreferences(false);
                            dp.minAreaAlgoOption = DRC.DRCCheckMinArea.AREA_MANHATTAN;
                            com.sun.electric.tool.drc.Quick.checkDesignRules(errLog, lay, null, null, null, null, dp, GeometryHandler.GHMode.ALGO_SWEEP, false);
                            errLog.termLogging(true);
                            drcErr = errLog.getNumErrors();
                            drcWarn = errLog.getNumWarnings();
                        }

                        String nccStatus = "N/A";
                        if (sch != null && lay != null) {
                            NccOptions opts = new NccOptions();
                            opts.checkSizes = false;
                            NccResults res = Ncc.compare(sch, null, lay, null, opts);
                            nccStatus = res.match() ? "PASS (100%)" : "FAIL";
                        }

                        System.out.println(String.format("%-22s | %-12s | %-15s | %-15s",
                            cName,
                            (drcErr >= 0 ? String.valueOf(drcErr) : "No lay"),
                            (drcWarn >= 0 ? String.valueOf(drcWarn) : "No lay"),
                            nccStatus));
                    }
                    System.out.println("==========================================================================");

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        j.startJob();
        Thread.sleep(12000);
    }
}
