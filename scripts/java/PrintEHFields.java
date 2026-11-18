
import com.sun.electric.tool.user.ErrorHighlight;
import java.lang.reflect.Field;

public class PrintEHFields {
    public static void main(String[] args) {
        for (Field f : ErrorHighlight.class.getDeclaredFields()) {
            System.out.println(f);
        }
    }
}
