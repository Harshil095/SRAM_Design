with open("BuildRectifiedFinalChip.java", "r") as f:
    text = f.read()

# 1. Update Input Pad power tap widths to 8.0
text = text.replace("aTapV.setLambdaBaseWidth(4.0);", "aTapV.setLambdaBaseWidth(ringW);")
text = text.replace("aTapG.setLambdaBaseWidth(4.0);", "aTapG.setLambdaBaseWidth(ringW);")

# 2. Update Output Pad power taps to west side at X = 150.0 and X = 110.0
old_out_pwr = """                    // Bottom Taps: Output Pad Power (tapped safely to the east at X = 350 and X = 380)
                    PortInst poV = lPadDout.findPortInstFromProto(outPadLay.findExport("vdd"));
                    NodeInst v12_poV_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(350.0, yOutPad + 21.5), 5.0, 5.0, chipLay);
                    NodeInst v12_poV_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(350.0, vddB), viaRingSz, viaRingSz, chipLay);
                    ArcInst aPoV_m1 = ArcInst.makeInstance(m1, ep, poV, v12_poV_top.getPortInst(0));
                    aPoV_m1.setLambdaBaseWidth(4.0);
                    ArcInst aOutVdd = ArcInst.makeInstance(m2, ep, v12_poV_top.getPortInst(0), v12_poV_bot.getPortInst(0));
                    aOutVdd.setLambdaBaseWidth(ringW);

                    PortInst poG = lPadDout.findPortInstFromProto(outPadLay.findExport("gnd"));
                    NodeInst v12_poG_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(380.0, yOutPad - 23.0), 5.0, 5.0, chipLay);
                    NodeInst v12_poG_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(380.0, gndB), viaRingSz, viaRingSz, chipLay);
                    ArcInst aPoG_m1 = ArcInst.makeInstance(m1, ep, poG, v12_poG_top.getPortInst(0));
                    aPoG_m1.setLambdaBaseWidth(4.0);
                    ArcInst aOutGnd = ArcInst.makeInstance(m2, ep, v12_poG_top.getPortInst(0), v12_poG_bot.getPortInst(0));
                    aOutGnd.setLambdaBaseWidth(ringW);"""

new_out_pwr = """                    // Bottom Taps: Output Pad Power (tapped safely to the WEST at X = 150 and X = 110)
                    PortInst poV = lPadDout.findPortInstFromProto(outPadLay.findExport("vdd"));
                    NodeInst v12_poV_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(150.0, yOutPad + 21.5), viaRingSz, viaRingSz, chipLay);
                    NodeInst v12_poV_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(150.0, vddB), viaRingSz, viaRingSz, chipLay);
                    ArcInst aPoV_m1 = ArcInst.makeInstance(m1, ep, v12_poV_top.getPortInst(0), poV);
                    aPoV_m1.setLambdaBaseWidth(ringW);
                    ArcInst aOutVdd = ArcInst.makeInstance(m2, ep, v12_poV_top.getPortInst(0), v12_poV_bot.getPortInst(0));
                    aOutVdd.setLambdaBaseWidth(ringW);

                    PortInst poG = lPadDout.findPortInstFromProto(outPadLay.findExport("gnd"));
                    NodeInst v12_poG_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(110.0, yOutPad - 23.0), viaRingSz, viaRingSz, chipLay);
                    NodeInst v12_poG_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(110.0, gndB), viaRingSz, viaRingSz, chipLay);
                    ArcInst aPoG_m1 = ArcInst.makeInstance(m1, ep, v12_poG_top.getPortInst(0), poG);
                    aPoG_m1.setLambdaBaseWidth(ringW);
                    ArcInst aOutGnd = ArcInst.makeInstance(m2, ep, v12_poG_top.getPortInst(0), v12_poG_bot.getPortInst(0));
                    aOutGnd.setLambdaBaseWidth(ringW);"""

assert old_out_pwr in text, "old_out_pwr not found"
text = text.replace(old_out_pwr, new_out_pwr)

# 3. Update daisy chaining on bottom rails since v12_poV_bot is at X=150 (between -55 and 4450)
# and v12_poG_bot is at X=110 (between -75 and 4550)
# Old was: BL -> pmV (-55) -> poV (350) -> BR (4450)
# With X=150, order is still BL -> pmV (-55) -> poV (150) -> BR (4450), which is correct!

with open("BuildRectifiedFinalChip.java", "w") as f:
    f.write(text)

print("BuildRectifiedFinalChip.java updated successfully!")
