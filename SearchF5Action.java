
import java.io.InputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Scanner;

public class SearchF5Action {
    public static void main(String[] args) throws Exception {
        ZipFile zf = new ZipFile("/Users/harshil_rathan/Downloads/9.08.1.jar");
        for (var e = zf.entries(); e.hasMoreElements(); ) {
            ZipEntry ze = e.nextElement();
            if (ze.getName().contains("CheckDesignRules") || ze.getName().contains("DRCMenu") || ze.getName().contains("drc/DRC")) {
                System.out.println(ze.getName());
            }
        }
    }
}
