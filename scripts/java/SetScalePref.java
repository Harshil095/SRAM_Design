
import java.util.prefs.Preferences;

public class SetScalePref {
    public static void main(String[] args) throws Exception {
        Preferences p = Preferences.userRoot().node("/com/sun/electric/technology/technologies");
        p.putDouble("ScaleFORmocmos", 90.0);
        p.flush();
        System.out.println("Saved ScaleFORmocmos = " + p.getDouble("ScaleFORmocmos", 0.0) + " into user preferences!");
    }
}
