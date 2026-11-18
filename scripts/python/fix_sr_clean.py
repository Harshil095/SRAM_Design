with open("MakeShiftReg.java", "r") as f:
    text = f.read()

# 1. In-row cascade: use Metal-3 for the bridge from X=565 to X=pitchX+80 at Y=yBridge
old_in_row = """                        double yBridge = r * pitchY + 50.0;
                        NodeInst vQ = NodeInst.makeInstance(via12Proto, new Point2D.Double(565.0, yBridge), 5.0, 5.0, layCell);
                        NodeInst vD = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX + 80.0, r * pitchY + 10.0), 5.0, 5.0, layCell);
                        NodeInst pD_up = NodeInst.makeInstance(m2Pin, new Point2D.Double(pitchX + 80.0, yBridge), 4.0, 4.0, layCell);

                        ArcInst.makeInstance(m1, ep, pQ, vQ.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, pD, vD.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vD.getPortInst(0), pD_up.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vQ.getPortInst(0), pD_up.getPortInst(0)).setLambdaBaseWidth(4.0);"""

new_in_row = """                        double yBridge = r * pitchY + 50.0;
                        NodeInst vQ_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(565.0, yBridge), 5.0, 5.0, layCell);
                        NodeInst vQ_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(565.0, yBridge), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m1, ep, pQ, vQ_12.getPortInst(0)).setLambdaBaseWidth(5.0);
                        ArcInst.makeInstance(m2, ep, vQ_12.getPortInst(0), vQ_23.getPortInst(0)).setLambdaBaseWidth(5.0);

                        NodeInst vD_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX + 80.0, r * pitchY + 10.0), 5.0, 5.0, layCell);
                        NodeInst vD_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(pitchX + 80.0, yBridge), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m1, ep, pD, vD_12.getPortInst(0)).setLambdaBaseWidth(5.0);
                        ArcInst.makeInstance(m2, ep, vD_12.getPortInst(0), vD_23.getPortInst(0)).setLambdaBaseWidth(5.0);

                        ArcInst.makeInstance(m3, ep, vQ_23.getPortInst(0), vD_23.getPortInst(0)).setLambdaBaseWidth(4.0);"""

assert old_in_row in text, "old_in_row not found"
text = text.replace(old_in_row, new_in_row)

# 2. Cross-row cascade: route at yCh = (r+1)*pitchY + 50.0 (ABOVE the next row DFF!)
old_cross_row = """                        double yCh = r * pitchY + 80.0;
                        NodeInst vQ_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX + 565.0, yCh), 5.0, 5.0, layCell);
                        NodeInst vQ_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(pitchX + 565.0, yCh), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m1, ep, pQ, vQ_12.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vQ_12.getPortInst(0), vQ_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        double yNextD = (r + 1) * pitchY + 10.0;
                        NodeInst vD_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(80.0, yNextD), 5.0, 5.0, layCell);
                        NodeInst vD_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(80.0, yCh), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m1, ep, pD, vD_12.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vD_12.getPortInst(0), vD_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        ArcInst.makeInstance(m3, ep, vQ_23.getPortInst(0), vD_23.getPortInst(0)).setLambdaBaseWidth(4.0);"""

new_cross_row = """                        double yCh = (r + 1) * pitchY + 50.0;
                        NodeInst vQ_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX + 565.0, yCh), 5.0, 5.0, layCell);
                        NodeInst vQ_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(pitchX + 565.0, yCh), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m1, ep, pQ, vQ_12.getPortInst(0)).setLambdaBaseWidth(5.0);
                        ArcInst.makeInstance(m2, ep, vQ_12.getPortInst(0), vQ_23.getPortInst(0)).setLambdaBaseWidth(5.0);

                        double yNextD = (r + 1) * pitchY + 10.0;
                        NodeInst vD_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(80.0, yNextD), 5.0, 5.0, layCell);
                        NodeInst vD_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(80.0, yCh), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m1, ep, pD, vD_12.getPortInst(0)).setLambdaBaseWidth(5.0);
                        ArcInst.makeInstance(m2, ep, vD_12.getPortInst(0), vD_23.getPortInst(0)).setLambdaBaseWidth(5.0);

                        ArcInst.makeInstance(m3, ep, vQ_23.getPortInst(0), vD_23.getPortInst(0)).setLambdaBaseWidth(4.0);"""

assert old_cross_row in text, "old_cross_row not found"
text = text.replace(old_cross_row, new_cross_row)

# 3. For ADDR_IN:
text = text.replace("ArcInst.makeInstance(m1, ep, pD0, vD0_12.getPortInst(0)).setLambdaBaseWidth(4.0);",
                    "ArcInst.makeInstance(m1, ep, pD0, vD0_12.getPortInst(0)).setLambdaBaseWidth(5.0);")

with open("MakeShiftReg.java", "w") as f:
    f.write(text)

print("Updates applied to MakeShiftReg.java")
