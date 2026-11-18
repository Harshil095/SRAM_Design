
import com.sun.electric.Main;
import com.sun.electric.technology.Technology;
import com.sun.electric.database.prototype.NodeProto;

public class ContactSizes {
    public static void main(String[] args) throws Exception {
        Main.main(new String[]{"-batch"});
        Technology mocmos = Technology.findTechnology("mocmos");
        NodeProto pcon = mocmos.findNodeProto("Metal-1-Polysilicon-1-Con");
        NodeProto via1 = mocmos.findNodeProto("Metal-1-Metal-2-Con");
        NodeProto pin = mocmos.findNodeProto("Metal-1-Pin");
        System.out.println("pcon: " + pcon.getDefWidth(null) + " x " + pcon.getDefHeight(null));
        System.out.println("via1: " + via1.getDefWidth(null) + " x " + via1.getDefHeight(null));
        System.out.println("pin: " + pin.getDefWidth(null) + " x " + pin.getDefHeight(null));
        System.exit(0);
    }
}
