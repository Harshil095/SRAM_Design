with open("BuildCompactedMacroCleanV4.java", "r") as f:
    text = f.read()

text = text.replace("BuildCompactedMacroCleanV4", "BuildCompactedMacroCleanV8")

# 1. ROUTING B: Column Select & Inverters
old_routing_b = """                    // ------------------------------------------
                    // ROUTING B: Column Select & Inverters
                    // ------------------------------------------"""
idx_b = text.find(old_routing_b)
idx_c = text.find("// ROUTING C: Global Bitlines (Orthogonal Manhattan)")

new_routing_b = """                    // ------------------------------------------
                    // ROUTING B: Column Select & Inverters
                    // ------------------------------------------
                    for (int c = 0; c < 16; c++) {
                        PortInst pDecOut = lInstColDec.findPortInstFromProto(layDec.findExport("OUT[" + c + "]"));
                        double yOut = pDecOut.getCenter().getY();
                        PortInst pInvIn  = lColInvs[c].findPortInstFromProto(layInv.findExport("in"));
                        PortInst pInvOut = lColInvs[c].findPortInstFromProto(layInv.findExport("out"));
                        PortInst pDatSel  = lInstData.findPortInstFromProto(layData.findExport("COL_SEL[" + c + "]"));
                        PortInst pDatSelB = lInstData.findPortInstFromProto(layData.findExport("COL_SEL_B[" + c + "]"));

                        NodeInst v12_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(-100.0, yOut), 6.0, 6.0, lay);
                        NodeInst v23_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(-100.0, yOut), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pDecOut, v12_out.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_out.getPortInst(0), v23_out.getPortInst(0));

                        double xSelTrack = c * 76.0 + 10.0;
                        if (c == 0) xSelTrack = -25.0;
                        if (c == 3) xSelTrack = c * 76.0 + 22.0;

                        NodeInst v23_m3_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(xSelTrack, yOut), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_out.getPortInst(0), v23_m3_bot.getPortInst(0));

                        NodeInst v12_inv_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(xSelTrack, yColInvs - 1.0), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, v12_inv_in.getPortInst(0), pInvIn);

                        if (c == 0) {
                            NodeInst v12_m2_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-25.0, yColInvs - 1.0), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_m3_bot.getPortInst(0), v12_m2_bot.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_m2_bot.getPortInst(0), v12_inv_in.getPortInst(0));
                        } else {
                            ArcInst.makeInstance(m2, ep, v23_m3_bot.getPortInst(0), v12_inv_in.getPortInst(0));
                        }

                        // Connect to COL_SEL[c] at Y = -245.0 (below Col_Mux_Bank_16 to clear tgate active diffusions)
                        double ySelBot = -245.0;
                        NodeInst cPoly_sel = NodeInst.makeInstance(conPolyM1, new Point2D.Double(c * 76.0, ySelBot), 5.0, 5.0, lay);
                        NodeInst v12_sel   = NodeInst.makeInstance(via12Proto, new Point2D.Double(c * 76.0, ySelBot), 5.0, 5.0, lay);
                        ArcInst.makeInstance(poly, ep, pDatSel, cPoly_sel.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, cPoly_sel.getPortInst(0), v12_sel.getPortInst(0));

                        if (c == 0) {
                            NodeInst v12_sel_c0 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-25.0, ySelBot), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_m3_bot.getPortInst(0), v12_sel_c0.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_sel_c0.getPortInst(0), v12_sel.getPortInst(0));
                        } else {
                            NodeInst v12_sel_top = NodeInst.makeInstance(via12Proto, new Point2D.Double(xSelTrack, ySelBot), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v12_inv_in.getPortInst(0), v12_sel_top.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_sel_top.getPortInst(0), v12_sel.getPortInst(0));
                        }

                        // Connect to COL_SEL_B[c] at Y = -110.0 (above Col_Mux_Bank_16 in open channel)
                        double x = c * 76.0;
                        double ySelBTop = -110.0;
                        NodeInst cPoly_selb = NodeInst.makeInstance(conPolyM1, new Point2D.Double(x, ySelBTop), 5.0, 5.0, lay);
                        NodeInst v12_selb   = NodeInst.makeInstance(via12Proto, new Point2D.Double(x, ySelBTop), 6.0, 6.0, lay);
                        ArcInst.makeInstance(poly, ep, pDatSelB, cPoly_selb.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, cPoly_selb.getPortInst(0), v12_selb.getPortInst(0));

                        NodeInst v12_inv_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(c * 76.0 + 43.0, yColInvs - 1.5), 6.0, 6.0, lay);
                        NodeInst v23_inv_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(c * 76.0 + 43.0, yColInvs - 1.5), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pInvOut, v12_inv_out.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_inv_out.getPortInst(0), v23_inv_out.getPortInst(0));

                        if (c == 0) {
                            // Metal-1 horizontal jog from (0.0, -110.0) to (-85.0, -110.0) to avoid shorting with M2 VDD rail at -55.0
                            NodeInst v12_selb_left = NodeInst.makeInstance(via12Proto, new Point2D.Double(-85.0, ySelBTop), 6.0, 6.0, lay);
                            NodeInst v23_selb_left = NodeInst.makeInstance(via23Proto, new Point2D.Double(-85.0, ySelBTop), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m1, ep, v12_selb.getPortInst(0), v12_selb_left.getPortInst(0));
                            ArcInst.makeInstance(m2, ep, v12_selb_left.getPortInst(0), v23_selb_left.getPortInst(0));

                            NodeInst v23_selb_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(-85.0, yColInvs - 1.5), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_selb_left.getPortInst(0), v23_selb_bot.getPortInst(0));
                            ArcInst.makeInstance(m3, ep, v23_selb_bot.getPortInst(0), v23_inv_out.getPortInst(0));
                        } else {
                            double xSelBTrack = x - 6.0;
                            NodeInst v23_selb_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xSelBTrack, ySelBTop), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m2, ep, v12_selb.getPortInst(0), v23_selb_top.getPortInst(0));

                            NodeInst v23_selb_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(xSelBTrack, yColInvs - 1.5), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_selb_top.getPortInst(0), v23_selb_bot.getPortInst(0));
                            ArcInst.makeInstance(m3, ep, v23_selb_bot.getPortInst(0), v23_inv_out.getPortInst(0));
                        }
                    }

                    """

text = text[:idx_b] + new_routing_b + text[idx_c:]

# 2. ROUTING D: Address Bus (All East routing with jog past Column 1)
old_routing_d = """                    // ------------------------------------------
                    // ROUTING D: Address Bus (Proven Manhattan Architecture)
                    // ------------------------------------------"""
idx_d = text.find(old_routing_d)
idx_e = text.find("// ------------------------------------------\n                    // ROUTING E: Timing Logic & Control Lines")

new_routing_d = """                    // ------------------------------------------
                    // ROUTING D: Address Bus (All-East Channel Routing)
                    // ------------------------------------------
                    for (int b = 0; b < 8; b++) {
                        PortInst pQ = lInstSreg.findPortInstFromProto(laySreg.findExport("Q[" + b + "]"));
                        PortInst pA;
                        double xTrack;
                        if (b < 4) {
                            pA = lInstRowDec.findPortInstFromProto(layDec.findExport("A[" + b + "]"));
                            xTrack = -950.0 - b * 12.0;
                        } else {
                            pA = lInstColDec.findPortInstFromProto(layDec.findExport("A[" + (b - 4) + "]"));
                            xTrack = -1000.0 - (b - 4) * 12.0;
                        }

                        double xQ = pQ.getCenter().getX();
                        double yQ = pQ.getCenter().getY();
                        double xEastTrack = 560.0 + b * 12.0;
                        double yTopJog    = 2820.0 + b * 12.0;
                        double xA         = pA.getCenter().getX();
                        double yA         = pA.getCenter().getY();

                        NodeInst v12_q = NodeInst.makeInstance(via12Proto, new Point2D.Double(xQ, yQ), 6.0, 6.0, lay);
                        NodeInst v23_q = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yQ), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pQ, v12_q.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_q.getPortInst(0), v23_q.getPortInst(0));

                        if (b % 2 == 0) {
                            // Even bit (Col 0, X = -75.0):
                            // Run M3 to X = 400.0, jog Y to yQ - 20.0 on M2, then M3 to xEastTrack
                            NodeInst v23_mid1 = NodeInst.makeInstance(via23Proto, new Point2D.Double(400.0, yQ), 6.0, 6.0, lay);
                            NodeInst v23_mid2 = NodeInst.makeInstance(via23Proto, new Point2D.Double(400.0, yQ - 20.0), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_q.getPortInst(0), v23_mid1.getPortInst(0));
                            ArcInst.makeInstance(m2, ep, v23_mid1.getPortInst(0), v23_mid2.getPortInst(0));

                            NodeInst v23_east_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(xEastTrack, yQ - 20.0), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_mid2.getPortInst(0), v23_east_bot.getPortInst(0));

                            NodeInst v23_east_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xEastTrack, yTopJog), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_east_bot.getPortInst(0), v23_east_top.getPortInst(0));

                            NodeInst v23_track_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yTopJog), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_east_top.getPortInst(0), v23_track_top.getPortInst(0));

                            NodeInst v12_track_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(xTrack, yA), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_track_top.getPortInst(0), v12_track_bot.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_track_bot.getPortInst(0), pA);
                        } else {
                            // Odd bit (Col 1, X = 545.0):
                            // Run M3 directly east to xEastTrack at yQ
                            NodeInst v23_east_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(xEastTrack, yQ), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_q.getPortInst(0), v23_east_bot.getPortInst(0));

                            NodeInst v23_east_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xEastTrack, yTopJog), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_east_bot.getPortInst(0), v23_east_top.getPortInst(0));

                            NodeInst v23_track_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yTopJog), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_east_top.getPortInst(0), v23_track_top.getPortInst(0));

                            NodeInst v12_track_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(xTrack, yA), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_track_top.getPortInst(0), v12_track_bot.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_track_bot.getPortInst(0), pA);
                        }
                    }

"""

text = text[:idx_d] + new_routing_d + text[idx_e:]

# 3. Update EXECUTE low line from 1445.0 to 1420.0
text = text.replace("new Point2D.Double(xExec, 1445.0)", "new Point2D.Double(xExec, 1420.0)")
text = text.replace("new Point2D.Double(-34.25, 1445.0)", "new Point2D.Double(-34.25, 1420.0)")
text = text.replace("new Point2D.Double(xExec + 50.0, 1445.0)", "new Point2D.Double(xExec + 50.0, 1420.0)")

# 4. In Timing logic:
# nandSae.B at X = 138.5 (instead of 124.0) so it does not overlap nandSae.A at X = 124.0!
old_sae_b_line = """                    NodeInst v23_sae_b = NodeInst.makeInstance(via23Proto, new Point2D.Double(124.0, yTiming - 120.0), 6.0, 6.0, lay);
                    NodeInst v12_sae_b = NodeInst.makeInstance(via12Proto, new Point2D.Double(124.0, yTiming - 37.5), 5.0, 5.0, lay);"""

new_sae_b_line = """                    NodeInst v23_sae_b = NodeInst.makeInstance(via23Proto, new Point2D.Double(138.5, yTiming - 70.0), 6.0, 6.0, lay);
                    NodeInst v12_sae_b = NodeInst.makeInstance(via12Proto, new Point2D.Double(138.5, yTiming - 37.5), 5.0, 5.0, lay);"""

text = text.replace(old_sae_b_line, new_sae_b_line)
text = text.replace("yTiming - 120.0", "yTiming - 70.0")

# 5. Fix R_W Pin input route: M3 from -950 to -125 at Y = 1949.0
old_rw_route = """                    NodeInst pinRwLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-950.0, yTiming + 20.0), 5.0, 5.0, lay);
                    Export.newInst(lay, pinRwLay.getPortInst(0), "R_W", ep);

                    NodeInst v12_rw = NodeInst.makeInstance(via12Proto, new Point2D.Double(-125.0, yTiming + 20.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pinRwLay.getPortInst(0), v12_rw.getPortInst(0));
                    NodeInst v12_rw_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(-125.0, yTiming - 1.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_rw.getPortInst(0), v12_rw_in.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_rw_in.getPortInst(0), lInvRw.findPortInstFromProto(layInv.findExport("in")));

                    NodeInst v23_rw_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(-125.0, yTiming + 20.0), 6.0, 6.0, lay);
                    NodeInst v23_rw_b   = NodeInst.makeInstance(via23Proto, new Point2D.Double(-56.0, yTiming + 20.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_rw.getPortInst(0), v23_rw_top.getPortInst(0));
                    ArcInst.makeInstance(m3, ep, v23_rw_top.getPortInst(0), v23_rw_b.getPortInst(0));"""

new_rw_route = """                    NodeInst pinRwLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-950.0, yTiming - 1.0), 5.0, 5.0, lay);
                    Export.newInst(lay, pinRwLay.getPortInst(0), "R_W", ep);

                    NodeInst v12_rw_pin = NodeInst.makeInstance(via12Proto, new Point2D.Double(-950.0, yTiming - 1.0), 6.0, 6.0, lay);
                    NodeInst v23_rw_pin = NodeInst.makeInstance(via23Proto, new Point2D.Double(-950.0, yTiming - 1.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pinRwLay.getPortInst(0), v12_rw_pin.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_rw_pin.getPortInst(0), v23_rw_pin.getPortInst(0));

                    NodeInst v23_rw_in = NodeInst.makeInstance(via23Proto, new Point2D.Double(-125.0, yTiming - 1.0), 6.0, 6.0, lay);
                    NodeInst v12_rw_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(-125.0, yTiming - 1.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_rw_pin.getPortInst(0), v23_rw_in.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_rw_in.getPortInst(0), v12_rw_in.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_rw_in.getPortInst(0), lInvRw.findPortInstFromProto(layInv.findExport("in")));

                    NodeInst v23_rw_b = NodeInst.makeInstance(via23Proto, new Point2D.Double(-56.0, yTiming - 1.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_rw_in.getPortInst(0), v23_rw_b.getPortInst(0));"""

text = text.replace(old_rw_route, new_rw_route)

# 6. Replace WE and SAE connections:
# WE horizontal runs on Metal-2 at Y = -515.0!
# SAE horizontal runs on Metal-2 at Y = -540.0!
idx_we_search = text.find("NodeInst v23_we_drop = NodeInst.makeInstance(via23Proto, new Point2D.Double(17.5, -520.0), 6.0, 6.0, lay);")
idx_f = text.find("// ------------------------------------------\n                    // ROUTING F: Global I/O (CLK, ADDR_IN, DIN, DOUT)")

new_we_sae = """NodeInst v12_we_drop = NodeInst.makeInstance(via12Proto, new Point2D.Double(17.5, -515.0), 6.0, 6.0, lay);
                    NodeInst cPoly_we    = NodeInst.makeInstance(conPolyM1,  new Point2D.Double(17.5, -515.0), 5.0, 5.0, lay);
                    // M2 across from (1200.0, -515.0) directly into v12_we_drop (clears vertical M3 column select lines!)
                    ArcInst.makeInstance(m2, ep, v23_we_r2.getPortInst(0), v12_we_drop.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_we_drop.getPortInst(0), cPoly_we.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_we.getPortInst(0), pWdWe);

                    // invSae.out (SAE) -> sense_amplifier.SAE
                    PortInst pInvSaeOut = lInvSae.findPortInstFromProto(layInv.findExport("out"));
                    PortInst pSaSae     = lInstSa.findPortInstFromProto(laySa.findExport("SAE"));
                    NodeInst v12_sae_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(268.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_sae_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(268.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v23_sae_tr  = NodeInst.makeInstance(via23Proto, new Point2D.Double(268.0, 1870.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pInvSaeOut, v12_sae_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_sae_out.getPortInst(0), v23_sae_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_sae_out.getPortInst(0), v23_sae_tr.getPortInst(0));

                    NodeInst v23_sae_r1 = NodeInst.makeInstance(via23Proto, new Point2D.Double(1210.0, 1870.0), 6.0, 6.0, lay);
                    NodeInst v23_sae_r2 = NodeInst.makeInstance(via23Proto, new Point2D.Double(1210.0, -540.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_sae_tr.getPortInst(0), v23_sae_r1.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_sae_r1.getPortInst(0), v23_sae_r2.getPortInst(0));

                    NodeInst v12_sae_pin = NodeInst.makeInstance(via12Proto, new Point2D.Double(150.0, -540.0), 6.0, 6.0, lay);
                    NodeInst cPoly_sae   = NodeInst.makeInstance(conPolyM1,  new Point2D.Double(150.0, -540.0), 5.0, 5.0, lay);
                    // M2 across from (1210.0, -540.0) directly into v12_sae_pin (clears vertical M3 column select lines!)
                    ArcInst.makeInstance(m2, ep, v23_sae_r2.getPortInst(0), v12_sae_pin.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sae_pin.getPortInst(0), cPoly_sae.getPortInst(0));

                    NodeInst jogSaePoly = NodeInst.makeInstance(polyPin, new Point2D.Double(150.0, -598.0), 2.0, 2.0, lay);
                    ArcInst.makeInstance(poly, ep, cPoly_sae.getPortInst(0), jogSaePoly.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, jogSaePoly.getPortInst(0), pSaSae);

                    """

text = text[:idx_we_search] + new_we_sae + text[idx_f:]

# 7. Update v23_we_r2 to Y = -515.0
text = text.replace("new Point2D.Double(1200.0, -520.0)", "new Point2D.Double(1200.0, -515.0)")

# 8. Update DIN connection (at X = -75.0, Y = -560.0)
old_din = """                    NodeInst cPoly_din = NodeInst.makeInstance(conPolyM1, new Point2D.Double(-45.0, -560.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pinDinLay.getPortInst(0), cPoly_din.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_din.getPortInst(0), pWdDin);"""

new_din = """                    NodeInst cPoly_din = NodeInst.makeInstance(conPolyM1, new Point2D.Double(-75.0, -560.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pinDinLay.getPortInst(0), cPoly_din.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_din.getPortInst(0), pWdDin);"""

text = text.replace(old_din, new_din)

# 9. Update ADDR_IN routing: M3 straight across from -950 to -560
old_addr_route = """                    NodeInst v12_addr = NodeInst.makeInstance(via12Proto, new Point2D.Double(-750.0, ySreg + 10.0), 5.0, 5.0, lay);
                    NodeInst v23_addr = NodeInst.makeInstance(via23Proto, new Point2D.Double(-750.0, ySreg + 10.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pinAddrLay.getPortInst(0), v12_addr.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_addr.getPortInst(0), v23_addr.getPortInst(0));

                    NodeInst v23_addr_mac = NodeInst.makeInstance(via23Proto, new Point2D.Double(-560.0, ySreg + 10.0), 6.0, 6.0, lay);
                    NodeInst v12_addr_mac = NodeInst.makeInstance(via12Proto, new Point2D.Double(-560.0, ySreg + 10.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_addr.getPortInst(0), v23_addr_mac.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_addr_mac.getPortInst(0), v12_addr_mac.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_addr_mac.getPortInst(0), lInstSreg.findPortInstFromProto(laySreg.findExport("ADDR_IN")));"""

new_addr_route = """                    NodeInst v12_addr = NodeInst.makeInstance(via12Proto, new Point2D.Double(-950.0, ySreg + 10.0), 6.0, 6.0, lay);
                    NodeInst v23_addr = NodeInst.makeInstance(via23Proto, new Point2D.Double(-950.0, ySreg + 10.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pinAddrLay.getPortInst(0), v12_addr.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_addr.getPortInst(0), v23_addr.getPortInst(0));

                    NodeInst v23_addr_mac = NodeInst.makeInstance(via23Proto, new Point2D.Double(-560.0, ySreg + 10.0), 6.0, 6.0, lay);
                    NodeInst v12_addr_mac = NodeInst.makeInstance(via12Proto, new Point2D.Double(-560.0, ySreg + 10.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_addr.getPortInst(0), v23_addr_mac.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_addr_mac.getPortInst(0), v12_addr_mac.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_addr_mac.getPortInst(0), lInstSreg.findPortInstFromProto(laySreg.findExport("ADDR_IN")));"""

text = text.replace(old_addr_route, new_addr_route)

with open("BuildCompactedMacroCleanV8.java", "w") as f:
    f.write(text)

print("BuildCompactedMacroCleanV8.java created successfully!")
