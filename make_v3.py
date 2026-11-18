with open("BuildCompactedMacroFlawless.java", "r") as f:
    code = f.read()

# Replace class name
code = code.replace("BuildCompactedMacroFlawless", "TestCompactedMacroV3")

# 1. Update coordinates
old_coords = """                    // Optimized Coordinates with verified clearance
                    double xRowDec = -640.0, yRowDec = 0.0;
                    double xColDec = -640.0, yColDec = -1850.0;
                    double xData = 0.0, yData = 0.0;
                    double xWd = 0.0, yWd = -340.0;
                    double xSa = 200.0, ySa = -340.0;
                    double xDiv8 = -640.0, yDiv8 = 1470.0;
                    double yTiming = 1860.0;
                    double xSreg = -640.0, ySreg = 2050.0;"""

new_coords = """                    // Optimized Coordinates with verified clearance
                    double xRowDec = -640.0, yRowDec = 0.0;
                    double xColDec = -640.0, yColDec = -1850.0;
                    double xData = 0.0, yData = 0.0;
                    double xWd = 0.0, yWd = -340.0;
                    double xSa = 200.0, ySa = -340.0;
                    double xDiv8 = -640.0, yDiv8 = 1530.0;
                    double yTiming = 1920.0;
                    double xSreg = -640.0, ySreg = 2100.0;"""

code = code.replace(old_coords, new_coords)

# 2. Update yColInvs to -720.0
code = code.replace("double yColInvs = -460.0;", "double yColInvs = -720.0;")

# 3. Update COL_SEL contacts to -450.0 and -340.0
old_colsel = """                        // Connect to COL_SEL[c] at Y = -200.0
                        NodeInst cPoly_sel = NodeInst.makeInstance(conPolyM1, new Point2D.Double(c * 76.0, -200.0), 5.0, 5.0, lay);
                        NodeInst v12_sel   = NodeInst.makeInstance(via12Proto, new Point2D.Double(c * 76.0, -200.0), 5.0, 5.0, lay);
                        ArcInst.makeInstance(poly, ep, pDatSel, cPoly_sel.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, cPoly_sel.getPortInst(0), v12_sel.getPortInst(0));

                        if (c == 0) {
                            NodeInst v12_sel_c0 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-25.0, -200.0), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_m3_bot.getPortInst(0), v12_sel_c0.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_sel_c0.getPortInst(0), v12_sel.getPortInst(0));
                        } else {
                            NodeInst v12_sel_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(xSelTrack, -200.0), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v12_inv_in.getPortInst(0), v12_sel_top.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_sel_top.getPortInst(0), v12_sel.getPortInst(0));
                        }

                        // Connect to COL_SEL_B[c] at Y = -158.0
                        double x = c * 76.0;
                        NodeInst cPoly_selb = NodeInst.makeInstance(conPolyM1, new Point2D.Double(x, -158.0), 5.0, 5.0, lay);
                        NodeInst v12_selb   = NodeInst.makeInstance(via12Proto, new Point2D.Double(x, -158.0), 5.0, 5.0, lay);
                        ArcInst.makeInstance(poly, ep, pDatSelB, cPoly_selb.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, cPoly_selb.getPortInst(0), v12_selb.getPortInst(0));"""

new_colsel = """                        // Connect to COL_SEL[c] at Y = -450.0 (open channel)
                        NodeInst cPoly_sel = NodeInst.makeInstance(conPolyM1, new Point2D.Double(c * 76.0, -450.0), 5.0, 5.0, lay);
                        NodeInst v12_sel   = NodeInst.makeInstance(via12Proto, new Point2D.Double(c * 76.0, -450.0), 5.0, 5.0, lay);
                        ArcInst.makeInstance(poly, ep, pDatSel, cPoly_sel.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, cPoly_sel.getPortInst(0), v12_sel.getPortInst(0));

                        if (c == 0) {
                            NodeInst v12_sel_c0 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-25.0, -450.0), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_m3_bot.getPortInst(0), v12_sel_c0.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_sel_c0.getPortInst(0), v12_sel.getPortInst(0));
                        } else {
                            NodeInst v12_sel_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(xSelTrack, -450.0), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v12_inv_in.getPortInst(0), v12_sel_top.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_sel_top.getPortInst(0), v12_sel.getPortInst(0));
                        }

                        // Connect to COL_SEL_B[c] at Y = -340.0 (open channel)
                        double x = c * 76.0;
                        NodeInst cPoly_selb = NodeInst.makeInstance(conPolyM1, new Point2D.Double(x, -340.0), 5.0, 5.0, lay);
                        NodeInst v12_selb   = NodeInst.makeInstance(via12Proto, new Point2D.Double(x, -340.0), 5.0, 5.0, lay);
                        ArcInst.makeInstance(poly, ep, pDatSelB, cPoly_selb.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, cPoly_selb.getPortInst(0), v12_selb.getPortInst(0));"""

code = code.replace(old_colsel, new_colsel)

# Also bridge for c==0 at -340.0 instead of -158.0
code = code.replace("new Point2D.Double(-40.0, -158.0)", "new Point2D.Double(-40.0, -340.0)")
code = code.replace("new Point2D.Double(-120.0, -158.0)", "new Point2D.Double(-120.0, -340.0)")
code = code.replace("new Point2D.Double(x, -158.0)", "new Point2D.Double(x, -340.0)")

# 4. Address routing: Jog higher (2720 instead of 2660) and west at -950
old_addr = """                        double yJog = 2660.0 + b * 12.0;
                        NodeInst v23_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yJog), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_q.getPortInst(0), v23_top.getPortInst(0));

                        NodeInst v12_corner = NodeInst.makeInstance(via12Proto, new Point2D.Double(xA, yJog), 5.0, 5.0, lay);
                        ArcInst.makeInstance(m2, ep, v23_top.getPortInst(0), v12_corner.getPortInst(0));

                        NodeInst v12_a = NodeInst.makeInstance(via12Proto, new Point2D.Double(xA, yA), 5.0, 5.0, lay);
                        ArcInst.makeInstance(m2, ep, v12_corner.getPortInst(0), v12_a.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, v12_a.getPortInst(0), pA);"""

new_addr = """                        double yJog = 2720.0 + b * 12.0;
                        NodeInst v23_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yJog), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_q.getPortInst(0), v23_top.getPortInst(0));

                        double xTrack = -950.0 - b * 12.0;
                        NodeInst v23_west = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yJog), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_top.getPortInst(0), v23_west.getPortInst(0));

                        NodeInst v12_a_corner = NodeInst.makeInstance(via12Proto, new Point2D.Double(xTrack, yA), 5.0, 5.0, lay);
                        NodeInst v23_a_corner = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yA), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m2, ep, v23_west.getPortInst(0), v23_a_corner.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v23_a_corner.getPortInst(0), v12_a_corner.getPortInst(0));

                        NodeInst v12_a = NodeInst.makeInstance(via12Proto, new Point2D.Double(xA, yA), 5.0, 5.0, lay);
                        ArcInst.makeInstance(m1, ep, v12_a_corner.getPortInst(0), v12_a.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, v12_a.getPortInst(0), pA);"""

code = code.replace(old_addr, new_addr)

# Q[4:7] address routing
old_addr2 = """                        double xTrack = -950.0 - b * 12.0;
                        double xA = pA.getCenter().getX();
                        double yA = pA.getCenter().getY();

                        NodeInst v12_q = NodeInst.makeInstance(via12Proto, new Point2D.Double(xQ, yQ), 5.0, 5.0, lay);
                        NodeInst v23_q = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yQ), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pQ, v12_q.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_q.getPortInst(0), v23_q.getPortInst(0));

                        double yJog = 2710.0 + b * 12.0;
                        NodeInst v23_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yJog), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_q.getPortInst(0), v23_top.getPortInst(0));

                        NodeInst v23_west = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yJog), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_top.getPortInst(0), v23_west.getPortInst(0));

                        NodeInst v12_a_corner = NodeInst.makeInstance(via12Proto, new Point2D.Double(xTrack, yA), 5.0, 5.0, lay);
                        NodeInst v23_a_corner = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yA), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m2, ep, v23_west.getPortInst(0), v23_a_corner.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v23_a_corner.getPortInst(0), v12_a_corner.getPortInst(0));

                        NodeInst v12_a = NodeInst.makeInstance(via12Proto, new Point2D.Double(xA, yA), 5.0, 5.0, lay);
                        ArcInst.makeInstance(m1, ep, v12_a_corner.getPortInst(0), v12_a.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, v12_a.getPortInst(0), pA);"""

new_addr2 = """                        double xTrack = -1000.0 - b * 12.0;
                        double xA = pA.getCenter().getX();
                        double yA = pA.getCenter().getY();

                        NodeInst v12_q = NodeInst.makeInstance(via12Proto, new Point2D.Double(xQ, yQ), 5.0, 5.0, lay);
                        NodeInst v23_q = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yQ), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pQ, v12_q.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_q.getPortInst(0), v23_q.getPortInst(0));

                        double yJog = 2770.0 + b * 12.0;
                        NodeInst v23_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yJog), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_q.getPortInst(0), v23_top.getPortInst(0));

                        NodeInst v23_west = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yJog), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_top.getPortInst(0), v23_west.getPortInst(0));

                        NodeInst v12_a_corner = NodeInst.makeInstance(via12Proto, new Point2D.Double(xTrack, yA), 5.0, 5.0, lay);
                        NodeInst v23_a_corner = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yA), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m2, ep, v23_west.getPortInst(0), v23_a_corner.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v23_a_corner.getPortInst(0), v12_a_corner.getPortInst(0));

                        NodeInst v12_a = NodeInst.makeInstance(via12Proto, new Point2D.Double(xA, yA), 5.0, 5.0, lay);
                        ArcInst.makeInstance(m1, ep, v12_a_corner.getPortInst(0), v12_a.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, v12_a.getPortInst(0), pA);"""

code = code.replace(old_addr2, new_addr2)

# 5. Fix power diagonal arcs
old_pwr = """                    // Row_Decoder power
                    PortInst pVddRow = lInstRowDec.findPortInstFromProto(layDec.findExport("vdd"));
                    PortInst pGndRow = lInstRowDec.findPortInstFromProto(layDec.findExport("gnd"));
                    ArcInst.makeInstance(m1, ep, v12_vdd_row.getPortInst(0), pVddRow);
                    ArcInst.makeInstance(m1, ep, v12_gnd_row.getPortInst(0), pGndRow);

                    // Col_Decoder power
                    PortInst pVddCol = lInstColDec.findPortInstFromProto(layDec.findExport("vdd"));
                    PortInst pGndCol = lInstColDec.findPortInstFromProto(layDec.findExport("gnd"));
                    ArcInst.makeInstance(m1, ep, v12_vdd_col.getPortInst(0), pVddCol);
                    ArcInst.makeInstance(m1, ep, v12_gnd_col.getPortInst(0), pGndCol);

                    // WD & SA power
                    PortInst pVddWd = lInstWd.findPortInstFromProto(layWd.findExport("vdd"));
                    ArcInst.makeInstance(m1, ep, v12_vdd_sa.getPortInst(0), pVddWd);
                    PortInst pGndWd = lInstWd.findPortInstFromProto(layWd.findExport("gnd"));
                    ArcInst.makeInstance(m1, ep, v12_gnd_wd.getPortInst(0), pGndWd);

                    PortInst pVddSa = lInstSa.findPortInstFromProto(laySa.findExport("vdd"));
                    ArcInst.makeInstance(m1, ep, v12_vdd_sa.getPortInst(0), pVddSa);
                    PortInst pGndSa = lInstSa.findPortInstFromProto(laySa.findExport("gnd"));
                    ArcInst.makeInstance(m1, ep, v12_gnd_wd.getPortInst(0), pGndSa);"""

new_pwr = """                    // Row_Decoder power (Strictly Manhattan at Y = -400 and -420)
                    PortInst pVddRow = lInstRowDec.findPortInstFromProto(layDec.findExport("vdd"));
                    PortInst pGndRow = lInstRowDec.findPortInstFromProto(layDec.findExport("gnd"));
                    NodeInst v12_vdd_row_m1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, -400.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_vdd_sa.getPortInst(0), v12_vdd_row_m1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_vdd_row_m1.getPortInst(0), pVddRow);

                    NodeInst v12_gnd_row_m1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, -420.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_gnd_wd.getPortInst(0), v12_gnd_row_m1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_gnd_row_m1.getPortInst(0), pGndRow);

                    // Col_Decoder power (Strictly Manhattan)
                    PortInst pVddCol = lInstColDec.findPortInstFromProto(layDec.findExport("vdd"));
                    PortInst pGndCol = lInstColDec.findPortInstFromProto(layDec.findExport("gnd"));
                    ArcInst.makeInstance(m1, ep, v12_vdd_col.getPortInst(0), pVddCol);
                    ArcInst.makeInstance(m1, ep, v12_gnd_col.getPortInst(0), pGndCol);

                    // WD & SA power (Strictly Manhattan)
                    PortInst pVddWd = lInstWd.findPortInstFromProto(layWd.findExport("vdd"));
                    NodeInst v12_vdd_wd = NodeInst.makeInstance(via12Proto, new Point2D.Double(-55.0, -296.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_vdd_sa.getPortInst(0), v12_vdd_wd.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_vdd_wd.getPortInst(0), pVddWd);

                    PortInst pGndWd = lInstWd.findPortInstFromProto(layWd.findExport("gnd"));
                    ArcInst.makeInstance(m1, ep, v12_gnd_wd.getPortInst(0), pGndWd);

                    PortInst pVddSa = lInstSa.findPortInstFromProto(laySa.findExport("vdd"));
                    ArcInst.makeInstance(m1, ep, v12_vdd_sa.getPortInst(0), pVddSa);

                    PortInst pGndSa = lInstSa.findPortInstFromProto(laySa.findExport("gnd"));
                    NodeInst v12_gnd_sa = NodeInst.makeInstance(via12Proto, new Point2D.Double(-75.0, -388.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_gnd_wd.getPortInst(0), v12_gnd_sa.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_gnd_sa.getPortInst(0), pGndSa);"""

code = code.replace(old_pwr, new_pwr)

# Pin locations:
code = code.replace("new Point2D.Double(-685.0, 2055.0)", "new Point2D.Double(-685.0, 2105.0)")
code = code.replace("new Point2D.Double(-700.0, 1740.0)", "new Point2D.Double(-700.0, 1800.0)")

# Also update EXECUTE Y
code = code.replace("double yExec = 1648.5;", "double yExec = yDiv8 + 178.5;")

with open("TestCompactedMacroV3.java", "w") as f:
    f.write(code)

print("make_v3.py executed successfully")
