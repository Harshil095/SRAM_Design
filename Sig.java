
import java.lang.reflect.Method;
import com.sun.electric.tool.drc.Quick;

public class Sig {
    public static void main(String[] args) {
        for (Method m : Quick.class.getMethods()) {
            if (m.getName().equals("checkDesignRules")) {
                System.out.println(m);
            }
        }
    }
}
