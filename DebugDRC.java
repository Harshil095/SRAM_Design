import com.sun.electric.Main;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.GeometryHandler;
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
import java.util.Iterator;

public class DebugDRC {
    public static void main(String[] args) throws Exception {
        Class c = Class.forName("com.sun.electric.tool.user.ErrorHighLine");
        while (c != null) {
            System.out.println("Class: " + c);
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                f.setAccessible(true);
                System.out.println("  " + f);
            }
            c = c.getSuperclass();
        }
    }
}
