with open("MakeShiftReg.java", "r") as f:
    text = f.read()

# Replace the cross-row routing
old_cross = """                        double yCh = (r + 1) * pitchY + 50.0;
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

new_cross = """                        double yQCh = r * pitchY + 50.0;
                        NodeInst vQ_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX + 565.0, yQCh), 5.0, 5.0, layCell);
                        NodeInst vQ_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(pitchX + 565.0, yQCh), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m1, ep, pQ, vQ_12.getPortInst(0)).setLambdaBaseWidth(5.0);
                        ArcInst.makeInstance(m2, ep, vQ_12.getPortInst(0), vQ_23.getPortInst(0)).setLambdaBaseWidth(5.0);

                        // Route on Metal-3 west to X = -20.0
                        double xCrossTrunk = -20.0;
                        NodeInst vWest_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(xCrossTrunk, yQCh), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m3, ep, vQ_23.getPortInst(0), vWest_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        // Travel north on Metal-2 at X = -20.0 to (r+1)*pitchY + 50.0
                        double yNextCh = (r + 1) * pitchY + 50.0;
                        NodeInst vNorth_m2 = NodeInst.makeInstance(m2Pin, new Point2D.Double(xCrossTrunk, yNextCh), 4.0, 4.0, layCell);
                        ArcInst.makeInstance(m2, ep, vWest_23.getPortInst(0), vNorth_m2.getPortInst(0)).setLambdaBaseWidth(4.0);

                        // Run east on Metal-2 to X = 80.0
                        NodeInst vNextD_top = NodeInst.makeInstance(m2Pin, new Point2D.Double(80.0, yNextCh), 4.0, 4.0, layCell);
                        ArcInst.makeInstance(m2, ep, vNorth_m2.getPortInst(0), vNextD_top.getPortInst(0)).setLambdaBaseWidth(4.0);

                        // Drop from yNextCh down to (r+1)*pitchY + 10.0 on Metal-2, then via12 to D port
                        double yNextD = (r + 1) * pitchY + 10.0;
                        NodeInst vD_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(80.0, yNextD), 5.0, 5.0, layCell);
                        ArcInst.makeInstance(m2, ep, vNextD_top.getPortInst(0), vD_12.getPortInst(0)).setLambdaBaseWidth(5.0);
                        ArcInst.makeInstance(m1, ep, vD_12.getPortInst(0), pD).setLambdaBaseWidth(5.0);"""

assert old_cross in text, "old_cross not found"
text = text.replace(old_cross, new_cross)

with open("MakeShiftReg.java", "w") as f:
    f.write(text)

print("MakeShiftReg.java updated with flawless cross-row routing!")
