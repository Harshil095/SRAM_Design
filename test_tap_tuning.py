with open("BuildRectifiedFinalChip.java", "r") as f:
    text = f.read()

# Replace via size for taps to 4.0 and tap wires to 4.0
text = text.replace("NodeInst tapV_m2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(vddL, yV), viaRingSz, viaRingSz, chipLay);",
                    "NodeInst tapV_m2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(vddL, yV), 4.0, 4.0, chipLay);")

text = text.replace("NodeInst tapG_m2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(gndL, yG), viaRingSz, viaRingSz, chipLay);",
                    "NodeInst tapG_m2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(gndL, yG), 4.0, 4.0, chipLay);")

text = text.replace("aTapV.setLambdaBaseWidth(ringW);", "aTapV.setLambdaBaseWidth(4.0);")
text = text.replace("aTapG.setLambdaBaseWidth(ringW);", "aTapG.setLambdaBaseWidth(4.0);")

# For Output Pad taps to west:
# poV at X=150, poG at X=110
text = text.replace("NodeInst v12_poV_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(150.0, yOutPad + 21.5), viaRingSz, viaRingSz, chipLay);",
                    "NodeInst v12_poV_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(150.0, yOutPad + 21.5), 4.0, 4.0, chipLay);")
text = text.replace("NodeInst v12_poG_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(110.0, yOutPad - 23.0), viaRingSz, viaRingSz, chipLay);",
                    "NodeInst v12_poG_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(110.0, yOutPad - 23.0), 4.0, 4.0, chipLay);")

text = text.replace("NodeInst v12_poV_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(150.0, vddB), viaRingSz, viaRingSz, chipLay);",
                    "NodeInst v12_poV_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(150.0, vddB), 4.0, 4.0, chipLay);")
text = text.replace("NodeInst v12_poG_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(110.0, gndB), viaRingSz, viaRingSz, chipLay);",
                    "NodeInst v12_poG_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(110.0, gndB), 4.0, 4.0, chipLay);")

text = text.replace("aPoV_m1.setLambdaBaseWidth(ringW);", "aPoV_m1.setLambdaBaseWidth(4.0);")
text = text.replace("aPoG_m1.setLambdaBaseWidth(ringW);", "aPoG_m1.setLambdaBaseWidth(4.0);")

text = text.replace("aOutVdd.setLambdaBaseWidth(ringW);", "aOutVdd.setLambdaBaseWidth(4.0);")
text = text.replace("aOutGnd.setLambdaBaseWidth(ringW);", "aOutGnd.setLambdaBaseWidth(4.0);")

with open("BuildRectifiedFinalChip.java", "w") as f:
    f.write(text)

print("Tuning applied.")
