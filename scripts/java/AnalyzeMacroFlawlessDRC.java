import java.util.*;
import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.topology.*;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import java.io.File;
import java.net.URL;

public class AnalyzeMacroFlawlessDRC {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        // Run BuildCompactedMacroFlawless first or inspect the errors produced by it
    }
}
