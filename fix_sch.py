with open("BuildCompactedMacroCleanV2.java", "r") as f:
    code = f.read()

bad_block = """                    for (int c = 0; c < 16; c++) {
                        ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), instColInvs[c].findPortInstFromProto(icInv.findExport("vdd")));
                        ArcInst.makeInstance(wire, ep, gnd.getPortInst(0), instColInvs[c].findPortInstFromProto(icInv.findExport("gnd")));
                    }

                    NodeInst[] tmInvs = {invD1, invD2, invD3, invRw, invWe, invSae};
                    for (NodeInst tg : tmInvs) {
                        ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), tg.findPortInstFromProto(icInv.findExport("vdd")));
                        ArcInst.makeInstance(wire, ep, gnd.getPortInst(0), tg.findPortInstFromProto(icInv.findExport("gnd")));
                    }
                    NodeInst[] tmNands = {nandWe, nandSae};
                    for (NodeInst tg : tmNands) {
                        ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), tg.findPortInstFromProto(icNand.findExport("vdd")));
                        ArcInst.makeInstance(wire, ep, gnd.getPortInst(0), tg.findPortInstFromProto(icNand.findExport("gnd")));
                    }"""

good_block = """                    // Tie C and D of nandWe and nandSae to vdd
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), nandWe.findPortInstFromProto(icNand.findExport("C")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), nandWe.findPortInstFromProto(icNand.findExport("D")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), nandSae.findPortInstFromProto(icNand.findExport("C")));
                    ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), nandSae.findPortInstFromProto(icNand.findExport("D")));"""

code = code.replace(bad_block, good_block)
with open("BuildCompactedMacroCleanV2.java", "w") as f:
    f.write(code)
print("Updated successfully")
