import re

with open("BuildCompactedMacroCleanV4.java", "r") as f:
    text = f.read()

text = text.replace("BuildCompactedMacroCleanV4", "BuildCompactedMacroCleanV6")

# 1. Replace ROUTING B
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

                        // Vertical M3 line at offset track: -85.0 for c=0 (clearing datapath net@265), and x - 6.0 for c >= 1
                        double xSelBTrack = (c == 0) ? -85.0 : (x - 6.0);
                        NodeInst v23_selb_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xSelBTrack, ySelBTop), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m2, ep, v12_selb.getPortInst(0), v23_selb_top.getPortInst(0));

                        NodeInst v23_selb_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(xSelBTrack, yColInvs - 1.5), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_selb_top.getPortInst(0), v23_selb_bot.getPortInst(0));
                        ArcInst.makeInstance(m3, ep, v23_selb_bot.getPortInst(0), v23_inv_out.getPortInst(0));
                    }

                    """

text = text[:idx_b] + new_routing_b + text[idx_c:]

# 2. Replace ROUTING D (Address Bus)
old_routing_d = """                    // ------------------------------------------
                    // ROUTING D: Address Bus (Proven Manhattan Architecture)
                    // ------------------------------------------"""
idx_d = text.find(old_routing_d)
idx_e = text.find("// ------------------------------------------\n                    // ROUTING E: Timing Logic & Control Lines")

new_routing_d = """                    // ------------------------------------------
                    // ROUTING D: Address Bus (Clean Non-Overlapping Architecture)
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

                        // Offset horizontal Y for even vs odd bits so Q[0] and Q[1] don't overlap at Y=2198.5
                        double yQ_h = (b % 2 == 0) ? (yQ - 15.0) : (yQ + 15.0);

                        NodeInst v12_q = NodeInst.makeInstance(via12Proto, new Point2D.Double(xQ, yQ), 6.0, 6.0, lay);
                        NodeInst v23_q = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yQ), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pQ, v12_q.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_q.getPortInst(0), v23_q.getPortInst(0));

                        // Vertical M2 jog from yQ to yQ_h
                        NodeInst v23_q_h = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yQ_h), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m2, ep, v23_q.getPortInst(0), v23_q_h.getPortInst(0));

                        // Horizontal M3 to East channel at yQ_h
                        NodeInst v23_east_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(xEastTrack, yQ_h), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_q_h.getPortInst(0), v23_east_bot.getPortInst(0));

                        // Vertical M3 up East channel to yTopJog
                        NodeInst v23_east_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xEastTrack, yTopJog), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_east_bot.getPortInst(0), v23_east_top.getPortInst(0));

                        // Horizontal M3 across top (Y > 2803) to West decoder track
                        NodeInst v23_track_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yTopJog), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_east_top.getPortInst(0), v23_track_top.getPortInst(0));

                        // Vertical M2 down West track to decoder input yA
                        NodeInst v12_track_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(xTrack, yA), 5.0, 5.0, lay);
                        ArcInst.makeInstance(m2, ep, v23_track_top.getPortInst(0), v12_track_bot.getPortInst(0));

                        // Horizontal M1 into decoder input
                        ArcInst.makeInstance(m1, ep, v12_track_bot.getPortInst(0), pA);
                    }

"""

text = text[:idx_d] + new_routing_d + text[idx_e:]

# 3. Update EXECUTE low line from 1445.0 to 1420.0
text = text.replace("new Point2D.Double(xExec, 1445.0)", "new Point2D.Double(xExec, 1420.0)")
text = text.replace("new Point2D.Double(-34.25, 1445.0)", "new Point2D.Double(-34.25, 1420.0)")
text = text.replace("new Point2D.Double(xExec + 50.0, 1445.0)", "new Point2D.Double(xExec + 50.0, 1420.0)")

# 4. Update R_W_b line from yTiming - 120.0 (1830.0) to yTiming - 70.0 (1880.0)
text = text.replace("yTiming - 120.0", "yTiming - 70.0")

# 5. Replace WE and SAE connections
idx_we_search = text.find("NodeInst v23_we_drop = NodeInst.makeInstance(via23Proto, new Point2D.Double(17.5, -520.0), 6.0, 6.0, lay);")
idx_f = text.find("// ------------------------------------------\n                    // ROUTING F: Global I/O (CLK, ADDR_IN, DIN, DOUT)")

new_we_sae = """NodeInst v23_we_drop = NodeInst.makeInstance(via23Proto, new Point2D.Double(17.5, -500.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_we_r2.getPortInst(0), v23_we_drop.getPortInst(0));

                    NodeInst v12_we_pin  = NodeInst.makeInstance(via12Proto, new Point2D.Double(17.5, -500.0), 6.0, 6.0, lay);
                    NodeInst cPoly_we    = NodeInst.makeInstance(conPolyM1,  new Point2D.Double(17.5, -500.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v23_we_drop.getPortInst(0), v12_we_pin.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_we_pin.getPortInst(0), cPoly_we.getPortInst(0));
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

                    NodeInst v23_sae_drop = NodeInst.makeInstance(via23Proto, new Point2D.Double(150.0, -540.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_sae_r2.getPortInst(0), v23_sae_drop.getPortInst(0));

                    NodeInst v12_sae_pin = NodeInst.makeInstance(via12Proto, new Point2D.Double(150.0, -598.0), 6.0, 6.0, lay);
                    NodeInst cPoly_sae   = NodeInst.makeInstance(conPolyM1,  new Point2D.Double(150.0, -598.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v23_sae_drop.getPortInst(0), v12_sae_pin.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sae_pin.getPortInst(0), cPoly_sae.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_sae.getPortInst(0), pSaSae);

                    """

text = text[:idx_we_search] + new_we_sae + text[idx_f:]

# 6. Replace DIN connection (at X = -75.0, Y = -560.0)
old_din = """                    NodeInst cPoly_din = NodeInst.makeInstance(conPolyM1, new Point2D.Double(-45.0, -560.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pinDinLay.getPortInst(0), cPoly_din.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_din.getPortInst(0), pWdDin);"""

new_din = """                    NodeInst cPoly_din = NodeInst.makeInstance(conPolyM1, new Point2D.Double(-75.0, -560.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pinDinLay.getPortInst(0), cPoly_din.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_din.getPortInst(0), pWdDin);"""

text = text.replace(old_din, new_din)

with open("BuildCompactedMacroCleanV6.java", "w") as f:
    f.write(text)

print("BuildCompactedMacroCleanV6.java written successfully!")
