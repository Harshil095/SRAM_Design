with open("BuildCompactedMacroCleanV2.java", "r") as f:
    code = f.read()

old_block = """                    NodeInst[] tmGates = {invD1, invD2, invD3, invRw, nandWe, invWe, nandSae, invSae};
                    for (NodeInst tg : tmGates) {
                        ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), tg.findPortInstFromProto(tg.getProto().findExport("vdd")));
                        ArcInst.makeInstance(wire, ep, gnd.getPortInst(0), tg.findPortInstFromProto(tg.getProto().findExport("gnd")));
                    }"""

new_block = """                    NodeInst[] tmInvs = {invD1, invD2, invD3, invRw, invWe, invSae};
                    for (NodeInst tg : tmInvs) {
                        ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), tg.findPortInstFromProto(icInv.findExport("vdd")));
                        ArcInst.makeInstance(wire, ep, gnd.getPortInst(0), tg.findPortInstFromProto(icInv.findExport("gnd")));
                    }
                    NodeInst[] tmNands = {nandWe, nandSae};
                    for (NodeInst tg : tmNands) {
                        ArcInst.makeInstance(wire, ep, pwr.getPortInst(0), tg.findPortInstFromProto(icNand.findExport("vdd")));
                        ArcInst.makeInstance(wire, ep, gnd.getPortInst(0), tg.findPortInstFromProto(icNand.findExport("gnd")));
                    }"""

code = code.replace(old_block, new_block)
with open("BuildCompactedMacroCleanV2.java", "w") as f:
    f.write(code)
print("Updated successfully")
