import com.sun.electric.util.math.Orientation;

public class ListOrientations {
    public static void main(String[] args) {
        for (Orientation o : Orientation.values()) {
            System.out.println(o.name() + " -> " + o);
        }
    }
}
