with open("MakeShiftReg.java", "r") as f:
    text = f.read()

# Replace cascade sections
old_cascades = """                    // 5. In-Row Cascades (DFF[2r] -> DFF[2r+1]):
                    // DFF[2r].Q at (565.0, r*pitchY - 1.5)
                    // DFF[2r+1].D at (pitchX + 80.0, r*pitchY)
                    // Route via (565.0, r*pitchY + 50.0) -> (pitchX + 80.0, r*pitchY + 50.0) on M2
                    for (int r = 0; r < 4; r++) {
                        PortInst pQ = layDFF[r * 2].findPortInstFromProto(dffLay.findExport("Q"));
                        PortInst pD = layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("D"));

                        double yBridge = r * pitchY + 50.0;
                        NodeInst vQ_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(565.0, yBridge), 5.0, 5.0, layCell);
                        NodeInst vQ_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(565.0, yBridge), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m1, ep, pQ, vQ_12.getPortInst(0)).setLambdaBaseWidth(5.0);
                        ArcInst.makeInstance(m2, ep, vQ_12.getPortInst(0), vQ_23.getPortInst(0)).setLambdaBaseWidth(5.0);

                        NodeInst vD_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX + 80.0, r * pitchY + 10.0), 5.0, 5.0, layCell);
                        NodeInst vD_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(pitchX + 80.0, yBridge), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m1, ep, pD, vD_12.getPortInst(0)).setLambdaBaseWidth(5.0);
                        ArcInst.makeInstance(m2, ep, vD_12.getPortInst(0), vD_23.getPortInst(0)).setLambdaBaseWidth(5.0);

                        ArcInst.makeInstance(m3, ep, vQ_23.getPortInst(0), vD_23.getPortInst(0)).setLambdaBaseWidth(4.0);
                    }

                    // 6. Cross-Row Cascades (DFF[2r+1] -> DFF[2r+2]):
                    // DFF[2r+1].Q at (pitchX + 565.0, r*pitchY - 1.5)
                    // DFF[2r+2].D at (80.0, (r+1)*pitchY)
                    // Route on Metal-3 at Y = r*pitchY + 80.0
                    for (int r = 0; r < 3; r++) {
                        PortInst pQ = layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("Q"));
                        PortInst pD = layDFF[(r + 1) * 2].findPortInstFromProto(dffLay.findExport("D"));

                        double yQCh = r * pitchY + 50.0;
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
                        ArcInst.makeInstance(m1, ep, vD_12.getPortInst(0), pD).setLambdaBaseWidth(5.0);
                    }"""

new_cascades = """                    // 5. In-Row Cascades (DFF[2r] -> DFF[2r+1]):
                    // Bridge at yBridge = r*pitchY + 40.0
                    for (int r = 0; r < 4; r++) {
                        PortInst pQ = layDFF[r * 2].findPortInstFromProto(dffLay.findExport("Q"));
                        PortInst pD = layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("D"));

                        double yBridge = r * pitchY + 40.0;
                        NodeInst vQ_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(565.0, yBridge), 4.0, 4.0, layCell);
                        NodeInst vQ_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(565.0, yBridge), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m1, ep, pQ, vQ_12.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vQ_12.getPortInst(0), vQ_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        NodeInst vD_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX + 80.0, r * pitchY + 10.0), 4.0, 4.0, layCell);
                        NodeInst vD_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(pitchX + 80.0, yBridge), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m1, ep, pD, vD_12.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vD_12.getPortInst(0), vD_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        ArcInst.makeInstance(m3, ep, vQ_23.getPortInst(0), vD_23.getPortInst(0)).setLambdaBaseWidth(4.0);
                    }

                    // 6. Cross-Row Cascades (DFF[2r+1] -> DFF[2r+2]):
                    // Bridge at yQCh = r*pitchY + 65.0 (25 lambda above in-row bridge)
                    // Staggered X trunks on west side: X = -15.0 - r*10.0
                    for (int r = 0; r < 3; r++) {
                        PortInst pQ = layDFF[r * 2 + 1].findPortInstFromProto(dffLay.findExport("Q"));
                        PortInst pD = layDFF[(r + 1) * 2].findPortInstFromProto(dffLay.findExport("D"));

                        double yQCh = r * pitchY + 65.0;
                        NodeInst vQ_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(pitchX + 565.0, yQCh), 4.0, 4.0, layCell);
                        NodeInst vQ_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(pitchX + 565.0, yQCh), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m1, ep, pQ, vQ_12.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m2, ep, vQ_12.getPortInst(0), vQ_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        double xCrossTrunk = -15.0 - r * 10.0;
                        NodeInst vWest_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(xCrossTrunk, yQCh), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m3, ep, vQ_23.getPortInst(0), vWest_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        double yNextCh = (r + 1) * pitchY + 65.0;
                        NodeInst vNorth_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(xCrossTrunk, yNextCh), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m2, ep, vWest_23.getPortInst(0), vNorth_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        NodeInst vNextD_23 = NodeInst.makeInstance(via23Proto, new Point2D.Double(80.0, yNextCh), 6.0, 6.0, layCell);
                        ArcInst.makeInstance(m3, ep, vNorth_23.getPortInst(0), vNextD_23.getPortInst(0)).setLambdaBaseWidth(4.0);

                        double yNextD = (r + 1) * pitchY + 10.0;
                        NodeInst vD_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(80.0, yNextD), 4.0, 4.0, layCell);
                        ArcInst.makeInstance(m2, ep, vNextD_23.getPortInst(0), vD_12.getPortInst(0)).setLambdaBaseWidth(4.0);
                        ArcInst.makeInstance(m1, ep, vD_12.getPortInst(0), pD).setLambdaBaseWidth(4.0);
                    }"""

assert old_cascades in text, "old_cascades not found"
text = text.replace(old_cascades, new_cascades)

# Fix ADDR_IN via to 4.0
text = text.replace("NodeInst vD0_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(80.0, 10.0), 5.0, 5.0, layCell);",
                    "NodeInst vD0_12 = NodeInst.makeInstance(via12Proto, new Point2D.Double(80.0, 10.0), 4.0, 4.0, layCell);")
text = text.replace("ArcInst.makeInstance(m1, ep, pD0, vD0_12.getPortInst(0)).setLambdaBaseWidth(5.0);",
                    "ArcInst.makeInstance(m1, ep, pD0, vD0_12.getPortInst(0)).setLambdaBaseWidth(4.0);")

with open("MakeShiftReg.java", "w") as f:
    f.write(text)

print("MakeShiftReg.java cleanly updated!")
