with open("BuildCompactedMacroCleanV8.java", "r") as f:
    text = f.read()

text = text.replace("BuildCompactedMacroCleanV8", "BuildCompactedMacroCleanV11")

# ==========================================
# 1. ROUTING B: Column Select & Inverters
# ==========================================
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

                        NodeInst v12_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(-100.0, yOut), 5.0, 5.0, lay);
                        NodeInst v23_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(-100.0, yOut), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pDecOut, v12_out.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_out.getPortInst(0), v23_out.getPortInst(0));

                        double xSelTrack = c * 76.0 + 10.0;
                        if (c == 0) xSelTrack = -25.0;
                        if (c == 3) xSelTrack = c * 76.0 + 22.0;

                        NodeInst v23_m3_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(xSelTrack, yOut), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m3, ep, v23_out.getPortInst(0), v23_m3_bot.getPortInst(0));

                        NodeInst v12_inv_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(xSelTrack, yColInvs - 1.0), 5.0, 5.0, lay);
                        ArcInst.makeInstance(m1, ep, v12_inv_in.getPortInst(0), pInvIn);

                        if (c == 0) {
                            NodeInst v12_m2_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(-25.0, yColInvs - 1.0), 5.0, 5.0, lay);
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
                        // Concentric stacked via12 (5x5) and via23 (6x6) at X = x = c * 76.0 (0 notch error)
                        double x = c * 76.0;
                        double ySelBTop = -110.0;
                        NodeInst cPoly_selb = NodeInst.makeInstance(conPolyM1, new Point2D.Double(x, ySelBTop), 5.0, 5.0, lay);
                        NodeInst v12_selb   = NodeInst.makeInstance(via12Proto, new Point2D.Double(x, ySelBTop), 5.0, 5.0, lay);
                        ArcInst.makeInstance(poly, ep, pDatSelB, cPoly_selb.getPortInst(0));
                        ArcInst.makeInstance(m1, ep, cPoly_selb.getPortInst(0), v12_selb.getPortInst(0));

                        NodeInst v12_inv_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(c * 76.0 + 43.0, yColInvs - 1.5), 5.0, 5.0, lay);
                        NodeInst v23_inv_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(c * 76.0 + 43.0, yColInvs - 1.5), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pInvOut, v12_inv_out.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_inv_out.getPortInst(0), v23_inv_out.getPortInst(0));

                        if (c == 0) {
                            // Metal-1 horizontal jog from (0.0, -110.0) to (-85.0, -110.0) to avoid shorting with M2 VDD rail at -55.0
                            NodeInst v12_selb_left = NodeInst.makeInstance(via12Proto, new Point2D.Double(-85.0, ySelBTop), 5.0, 5.0, lay);
                            NodeInst v23_selb_left = NodeInst.makeInstance(via23Proto, new Point2D.Double(-85.0, ySelBTop), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m1, ep, v12_selb.getPortInst(0), v12_selb_left.getPortInst(0));
                            ArcInst.makeInstance(m2, ep, v12_selb_left.getPortInst(0), v23_selb_left.getPortInst(0));

                            NodeInst v23_selb_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(-85.0, yColInvs - 1.5), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_selb_left.getPortInst(0), v23_selb_bot.getPortInst(0));
                            ArcInst.makeInstance(m3, ep, v23_selb_bot.getPortInst(0), v23_inv_out.getPortInst(0));
                        } else {
                            // Direct concentric stack at (x, ySelBTop) with 0 notch, running straight down on Metal-3
                            NodeInst v23_selb_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(x, ySelBTop), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m2, ep, v12_selb.getPortInst(0), v23_selb_top.getPortInst(0));

                            NodeInst v23_selb_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(x, yColInvs - 1.5), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_selb_top.getPortInst(0), v23_selb_bot.getPortInst(0));
                            ArcInst.makeInstance(m3, ep, v23_selb_bot.getPortInst(0), v23_inv_out.getPortInst(0));
                        }
                    }

                    """

text = text[:idx_b] + new_routing_b + text[idx_c:]

# ==========================================
# 2. ROUTING D: Address Bus
# ==========================================
idx_d = text.find("// ROUTING D:")
idx_e = text.find("// ------------------------------------------\n                    // ROUTING E: Timing Logic & Control Lines")

new_routing_d = """                    // ------------------------------------------
                    // ROUTING D: Address Bus (Strict Layer-Separated Manhattan)
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

                        NodeInst v12_q = NodeInst.makeInstance(via12Proto, new Point2D.Double(xQ, yQ), 5.0, 5.0, lay);
                        NodeInst v23_q = NodeInst.makeInstance(via23Proto, new Point2D.Double(xQ, yQ), 6.0, 6.0, lay);
                        ArcInst.makeInstance(m1, ep, pQ, v12_q.getPortInst(0));
                        ArcInst.makeInstance(m2, ep, v12_q.getPortInst(0), v23_q.getPortInst(0));

                        if (b % 2 == 0) {
                            // Even bit (Col 0, X = -75.0):
                            // Run M3 to X = 400.0, drop to M2, run M2 across to xEastTrack (clearing vertical M3 tracks!),
                            // then M3 vertical up to yTopJog
                            NodeInst v23_mid1 = NodeInst.makeInstance(via23Proto, new Point2D.Double(400.0, yQ), 6.0, 6.0, lay);
                            NodeInst v23_mid2 = NodeInst.makeInstance(via23Proto, new Point2D.Double(400.0, yQ - 20.0), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_q.getPortInst(0), v23_mid1.getPortInst(0));
                            ArcInst.makeInstance(m2, ep, v23_mid1.getPortInst(0), v23_mid2.getPortInst(0));

                            NodeInst v23_east_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(xEastTrack, yQ - 20.0), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_mid2.getPortInst(0), v23_east_bot.getPortInst(0));

                            NodeInst v23_east_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xEastTrack, yTopJog), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_east_bot.getPortInst(0), v23_east_top.getPortInst(0));

                            NodeInst v23_track_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xTrack, yTopJog), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m3, ep, v23_east_top.getPortInst(0), v23_track_top.getPortInst(0));

                            NodeInst v12_track_bot = NodeInst.makeInstance(via12Proto, new Point2D.Double(xTrack, yA), 5.0, 5.0, lay);
                            ArcInst.makeInstance(m2, ep, v23_track_top.getPortInst(0), v12_track_bot.getPortInst(0));
                            ArcInst.makeInstance(m1, ep, v12_track_bot.getPortInst(0), pA);
                        } else {
                            // Odd bit (Col 1, X = 545.0):
                            // Run M2 directly east to xEastTrack at yQ (clearing any vertical M3 tracks!)
                            NodeInst v23_east_bot = NodeInst.makeInstance(via23Proto, new Point2D.Double(xEastTrack, yQ), 6.0, 6.0, lay);
                            ArcInst.makeInstance(m2, ep, v12_q.getPortInst(0), v23_east_bot.getPortInst(0));

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

# ==========================================
# 3. ROUTING E: Timing Logic & Control Lines
# ==========================================
idx_e = text.find("// ------------------------------------------\n                    // ROUTING E: Timing Logic & Control Lines")
idx_f = text.find("// ------------------------------------------\n                    // ROUTING F: Global I/O (CLK, ADDR_IN, DIN, DOUT)")

new_routing_e = """// ------------------------------------------
                    // ROUTING E: Timing Logic & Control Lines (West Corridor)
                    // ------------------------------------------
                    PortInst pDiv8Exec = lInstDiv8.findPortInstFromProto(layDiv8.findExport("EXECUTE"));
                    PortInst lPreB     = lInstData.findPortInstFromProto(layData.findExport("PRE_B"));
                    double xExec = pDiv8Exec.getCenter().getX();
                    double yExec = pDiv8Exec.getCenter().getY();

                    // EXECUTE -> run East to X = 560.0 (outside Div8_Counter), drop south to 1420.0, run West to PRE_B and Delay Chain
                    NodeInst v12_exec = NodeInst.makeInstance(via12Proto, new Point2D.Double(xExec, yExec), 5.0, 5.0, lay);
                    NodeInst v23_exec_top = NodeInst.makeInstance(via23Proto, new Point2D.Double(xExec, yExec), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pDiv8Exec, v12_exec.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_exec.getPortInst(0), v23_exec_top.getPortInst(0));

                    NodeInst v23_exec_east = NodeInst.makeInstance(via23Proto, new Point2D.Double(560.0, yExec), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_exec_top.getPortInst(0), v23_exec_east.getPortInst(0));

                    NodeInst v23_exec_low = NodeInst.makeInstance(via23Proto, new Point2D.Double(560.0, 1420.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m2, ep, v23_exec_east.getPortInst(0), v23_exec_low.getPortInst(0));

                    // Direct connection to Datapath PRE_B on Metal-2
                    NodeInst v23_pre_tap = NodeInst.makeInstance(via23Proto, new Point2D.Double(-34.25, 1420.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_exec_low.getPortInst(0), v23_pre_tap.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_pre_tap.getPortInst(0), lPreB);

                    // EXECUTE branch up to Delay Chain (D1)
                    NodeInst v23_exec_up_ch = NodeInst.makeInstance(via23Proto, new Point2D.Double(-120.0, 1420.0), 6.0, 6.0, lay);
                    NodeInst v23_exec_top_ch = NodeInst.makeInstance(via23Proto, new Point2D.Double(-120.0, 1935.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_pre_tap.getPortInst(0), v23_exec_up_ch.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_exec_up_ch.getPortInst(0), v23_exec_top_ch.getPortInst(0));

                    NodeInst v23_d1 = NodeInst.makeInstance(via23Proto, new Point2D.Double(-425.0, 1935.0), 6.0, 6.0, lay);
                    NodeInst v12_d1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-425.0, yTiming - 1.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_exec_top_ch.getPortInst(0), v23_d1.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_d1.getPortInst(0), v12_d1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_d1.getPortInst(0), lInvD1.findPortInstFromProto(layInv.findExport("in")));

                    // D1 -> D2
                    PortInst pD1Out = lInvD1.findPortInstFromProto(layInv.findExport("out"));
                    PortInst pD2In  = lInvD2.findPortInstFromProto(layInv.findExport("in"));
                    NodeInst jD1_1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(-360.0, yTiming - 1.5), 3.0, 3.0, lay);
                    NodeInst jD1_2 = NodeInst.makeInstance(m1Pin, new Point2D.Double(-360.0, yTiming - 1.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, pD1Out, jD1_1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jD1_1.getPortInst(0), jD1_2.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jD1_2.getPortInst(0), pD2In);

                    // D2 -> D3 & nandWe.A
                    PortInst pD2Out = lInvD2.findPortInstFromProto(layInv.findExport("out"));
                    PortInst pD3In  = lInvD3.findPortInstFromProto(layInv.findExport("in"));
                    NodeInst jD2_1 = NodeInst.makeInstance(m1Pin, new Point2D.Double(-260.0, yTiming - 1.5), 3.0, 3.0, lay);
                    NodeInst jD2_2 = NodeInst.makeInstance(m1Pin, new Point2D.Double(-260.0, yTiming - 1.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, pD2Out, jD2_1.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jD2_1.getPortInst(0), jD2_2.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, jD2_2.getPortInst(0), pD3In);

                    // D2 tap -> nandWe.A at Y = 1915.0 on Metal-3
                    NodeInst v12_d2_tap = NodeInst.makeInstance(via12Proto, new Point2D.Double(-292.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_d2     = NodeInst.makeInstance(via23Proto, new Point2D.Double(-292.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v23_d2_m3  = NodeInst.makeInstance(via23Proto, new Point2D.Double(-292.0, 1915.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pD2Out, v12_d2_tap.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_d2_tap.getPortInst(0), v23_d2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_d2.getPortInst(0), v23_d2_m3.getPortInst(0));

                    NodeInst v23_we_a = NodeInst.makeInstance(via23Proto, new Point2D.Double(-56.0, 1915.0), 6.0, 6.0, lay);
                    NodeInst v12_we_a = NodeInst.makeInstance(via12Proto, new Point2D.Double(-56.0, 1897.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_d2_m3.getPortInst(0), v23_we_a.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_we_a.getPortInst(0), v12_we_a.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_we_a.getPortInst(0), lNandWe.findPortInstFromProto(layNand.findExport("A")));

                    // D3 -> nandSae.A at Y = 1905.0 on Metal-3
                    PortInst pD3Out = lInvD3.findPortInstFromProto(layInv.findExport("out"));
                    NodeInst v12_d3_tap = NodeInst.makeInstance(via12Proto, new Point2D.Double(-192.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_d3     = NodeInst.makeInstance(via23Proto, new Point2D.Double(-192.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v23_d3_m3  = NodeInst.makeInstance(via23Proto, new Point2D.Double(-192.0, 1905.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pD3Out, v12_d3_tap.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_d3_tap.getPortInst(0), v23_d3.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_d3.getPortInst(0), v23_d3_m3.getPortInst(0));

                    NodeInst v23_sae_a = NodeInst.makeInstance(via23Proto, new Point2D.Double(124.0, 1905.0), 6.0, 6.0, lay);
                    NodeInst v12_sae_a = NodeInst.makeInstance(via12Proto, new Point2D.Double(124.0, 1897.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_d3_m3.getPortInst(0), v23_sae_a.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_sae_a.getPortInst(0), v12_sae_a.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sae_a.getPortInst(0), lNandSae.findPortInstFromProto(layNand.findExport("A")));

                    // R_W Pin: at X = -1060.0, Y = 2060.0 on M3
                    NodeInst pinRwLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-1060.0, 2060.0), 5.0, 5.0, lay);
                    Export.newInst(lay, pinRwLay.getPortInst(0), "R_W", ep);

                    NodeInst v12_rw_pin = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1060.0, 2060.0), 5.0, 5.0, lay);
                    NodeInst v23_rw_pin = NodeInst.makeInstance(via23Proto, new Point2D.Double(-1060.0, 2060.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pinRwLay.getPortInst(0), v12_rw_pin.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_rw_pin.getPortInst(0), v23_rw_pin.getPortInst(0));

                    NodeInst v23_rw_in = NodeInst.makeInstance(via23Proto, new Point2D.Double(-128.0, 2060.0), 6.0, 6.0, lay);
                    NodeInst v12_rw_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(-128.0, yTiming - 1.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_rw_pin.getPortInst(0), v23_rw_in.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_rw_in.getPortInst(0), v12_rw_in.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_rw_in.getPortInst(0), lInvRw.findPortInstFromProto(layInv.findExport("in")));

                    // Connect to nandWe.B at (-41.5, 1897.5)
                    NodeInst v23_rw_b = NodeInst.makeInstance(via23Proto, new Point2D.Double(-41.5, 2060.0), 6.0, 6.0, lay);
                    NodeInst v12_rw_b = NodeInst.makeInstance(via12Proto, new Point2D.Double(-41.5, 1897.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_rw_in.getPortInst(0), v23_rw_b.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_rw_b.getPortInst(0), v12_rw_b.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_rw_b.getPortInst(0), lNandWe.findPortInstFromProto(layNand.findExport("B")));

                    // invRw.out (R_W_b) -> nandSae.B at Y = 1925.0
                    PortInst pInvRwOut = lInvRw.findPortInstFromProto(layInv.findExport("out"));
                    NodeInst v12_rw_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(-72.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_rw_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(-72.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v23_rw_mid = NodeInst.makeInstance(via23Proto, new Point2D.Double(-72.0, 1925.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pInvRwOut, v12_rw_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_rw_out.getPortInst(0), v23_rw_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_rw_out.getPortInst(0), v23_rw_mid.getPortInst(0));

                    NodeInst v23_sae_b = NodeInst.makeInstance(via23Proto, new Point2D.Double(138.5, 1925.0), 6.0, 6.0, lay);
                    NodeInst v12_sae_b = NodeInst.makeInstance(via12Proto, new Point2D.Double(138.5, 1897.5), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_rw_mid.getPortInst(0), v23_sae_b.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_sae_b.getPortInst(0), v12_sae_b.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sae_b.getPortInst(0), lNandSae.findPortInstFromProto(layNand.findExport("B")));

                    // NandWe C & D tied to VDD (locally on M1/M2)
                    PortInst pNandWeC = lNandWe.findPortInstFromProto(layNand.findExport("C"));
                    PortInst pNandWeD = lNandWe.findPortInstFromProto(layNand.findExport("D"));
                    ArcInst.makeInstance(m1, ep, pNandWeC, pNandWeD);
                    NodeInst v12_we_cd = NodeInst.makeInstance(via12Proto, new Point2D.Double(1.5, 1897.5), 5.0, 5.0, lay);
                    NodeInst jWeCd_m2  = NodeInst.makeInstance(m2Pin, new Point2D.Double(1.5, 1978.5), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, pNandWeD, v12_we_cd.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_we_cd.getPortInst(0), jWeCd_m2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, jWeCd_m2.getPortInst(0), lNandWe.findPortInstFromProto(layNand.findExport("vdd")));

                    // NandSae C & D tied to VDD (locally on M1/M2)
                    PortInst pNandSaeC = lNandSae.findPortInstFromProto(layNand.findExport("C"));
                    PortInst pNandSaeD = lNandSae.findPortInstFromProto(layNand.findExport("D"));
                    ArcInst.makeInstance(m1, ep, pNandSaeC, pNandSaeD);
                    NodeInst v12_sae_cd = NodeInst.makeInstance(via12Proto, new Point2D.Double(181.5, 1897.5), 5.0, 5.0, lay);
                    NodeInst jSaeCd_m2  = NodeInst.makeInstance(m2Pin, new Point2D.Double(181.5, 1978.5), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m1, ep, pNandSaeD, v12_sae_cd.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_sae_cd.getPortInst(0), jSaeCd_m2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, jSaeCd_m2.getPortInst(0), lNandSae.findPortInstFromProto(layNand.findExport("vdd")));

                    // nandWe.out -> invWe.in
                    PortInst pNandWeOut = lNandWe.findPortInstFromProto(layNand.findExport("out"));
                    PortInst pInvWeIn   = lInvWe.findPortInstFromProto(layInv.findExport("in"));
                    NodeInst v12_we_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(55.0, yTiming - 1.0), 5.0, 5.0, lay);
                    NodeInst jWeOut_m2 = NodeInst.makeInstance(m2Pin, new Point2D.Double(55.0, yTiming + 45.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m2, ep, pNandWeOut, jWeOut_m2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, jWeOut_m2.getPortInst(0), v12_we_in.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_we_in.getPortInst(0), pInvWeIn);

                    // nandSae.out -> invSae.in
                    PortInst pNandSaeOut = lNandSae.findPortInstFromProto(layNand.findExport("out"));
                    PortInst pInvSaeIn   = lInvSae.findPortInstFromProto(layInv.findExport("in"));
                    NodeInst v12_sae_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(235.0, yTiming - 1.0), 5.0, 5.0, lay);
                    NodeInst jSaeOut_m2 = NodeInst.makeInstance(m2Pin, new Point2D.Double(235.0, yTiming + 45.0), 3.0, 3.0, lay);
                    ArcInst.makeInstance(m2, ep, pNandSaeOut, jSaeOut_m2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, jSaeOut_m2.getPortInst(0), v12_sae_in.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sae_in.getPortInst(0), pInvSaeIn);

                    // invWe.out (WE) -> write_driver.WE
                    // Run M3 West at Y = 2030.0 to X = -200.0, drop down on M2 at X = -200.0 to Y = -640.0,
                    // run M1 East at Y = -640.0 to (17.5, -640.0), then Poly North to pWdWe at (17.5, -568.0)
                    PortInst pInvWeOut = lInvWe.findPortInstFromProto(layInv.findExport("out"));
                    PortInst pWdWe     = lInstWd.findPortInstFromProto(layWd.findExport("WE"));
                    NodeInst v12_we_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(88.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_we_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(88.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v23_we_tr  = NodeInst.makeInstance(via23Proto, new Point2D.Double(88.0, 2030.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pInvWeOut, v12_we_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_we_out.getPortInst(0), v23_we_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_we_out.getPortInst(0), v23_we_tr.getPortInst(0));

                    NodeInst v23_we_w1 = NodeInst.makeInstance(via23Proto, new Point2D.Double(-200.0, 2030.0), 6.0, 6.0, lay);
                    NodeInst v23_we_w2 = NodeInst.makeInstance(via23Proto, new Point2D.Double(-200.0, -640.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_we_tr.getPortInst(0), v23_we_w1.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_we_w1.getPortInst(0), v23_we_w2.getPortInst(0));

                    NodeInst v12_we_m1_trans = NodeInst.makeInstance(via12Proto, new Point2D.Double(-200.0, -640.0), 5.0, 5.0, lay);
                    NodeInst cPoly_we        = NodeInst.makeInstance(conPolyM1,  new Point2D.Double(17.5, -640.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v23_we_w2.getPortInst(0), v12_we_m1_trans.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_we_m1_trans.getPortInst(0), cPoly_we.getPortInst(0));

                    NodeInst jogWePoly = NodeInst.makeInstance(polyPin, new Point2D.Double(17.5, -568.0), 2.0, 2.0, lay);
                    ArcInst.makeInstance(poly, ep, cPoly_we.getPortInst(0), jogWePoly.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, jogWePoly.getPortInst(0), pWdWe);

                    // invSae.out (SAE) -> sense_amplifier.SAE
                    // Run M3 West at Y = 2045.0 to X = -220.0, drop down on M2 at X = -220.0 to Y = -660.0,
                    // run M1 East at Y = -660.0 to (200.0, -660.0), then Poly North to pSaSae at (200.0, -598.0)
                    PortInst pInvSaeOut = lInvSae.findPortInstFromProto(layInv.findExport("out"));
                    PortInst pSaSae     = lInstSa.findPortInstFromProto(laySa.findExport("SAE"));
                    NodeInst v12_sae_out = NodeInst.makeInstance(via12Proto, new Point2D.Double(268.0, yTiming - 1.5), 5.0, 5.0, lay);
                    NodeInst v23_sae_out = NodeInst.makeInstance(via23Proto, new Point2D.Double(268.0, yTiming - 1.5), 6.0, 6.0, lay);
                    NodeInst v23_sae_tr  = NodeInst.makeInstance(via23Proto, new Point2D.Double(268.0, 2045.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pInvSaeOut, v12_sae_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_sae_out.getPortInst(0), v23_sae_out.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_sae_out.getPortInst(0), v23_sae_tr.getPortInst(0));

                    NodeInst v23_sae_w1 = NodeInst.makeInstance(via23Proto, new Point2D.Double(-220.0, 2045.0), 6.0, 6.0, lay);
                    NodeInst v23_sae_w2 = NodeInst.makeInstance(via23Proto, new Point2D.Double(-220.0, -660.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_sae_tr.getPortInst(0), v23_sae_w1.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_sae_w1.getPortInst(0), v23_sae_w2.getPortInst(0));

                    NodeInst v12_sae_m1_trans = NodeInst.makeInstance(via12Proto, new Point2D.Double(-220.0, -660.0), 5.0, 5.0, lay);
                    NodeInst cPoly_sae        = NodeInst.makeInstance(conPolyM1,  new Point2D.Double(200.0, -660.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v23_sae_w2.getPortInst(0), v12_sae_m1_trans.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sae_m1_trans.getPortInst(0), cPoly_sae.getPortInst(0));

                    NodeInst jogSaePoly = NodeInst.makeInstance(polyPin, new Point2D.Double(200.0, -598.0), 2.0, 2.0, lay);
                    ArcInst.makeInstance(poly, ep, cPoly_sae.getPortInst(0), jogSaePoly.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, jogSaePoly.getPortInst(0), pSaSae);

                    """

text = text[:idx_e] + new_routing_e + text[idx_f:]

# ==========================================
# 4. ROUTING F: Global I/O (CLK, ADDR_IN, DIN, DOUT)
# ==========================================
idx_f = text.find("// ------------------------------------------\n                    // ROUTING F: Global I/O (CLK, ADDR_IN, DIN, DOUT)")
idx_g = text.find("// ------------------------------------------\n                    // ROUTING G: Power & Ground Distribution (Strict Manhattan)")

new_routing_f = """// ------------------------------------------
                    // ROUTING F: Global I/O (CLK, ADDR_IN, DIN, DOUT)
                    // ------------------------------------------
                    // CLK: pin at (-1060.0, 1520.0), jog around Div8 contact@49 by running vertically at X = -710.0
                    NodeInst pinClkLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-1060.0, 1520.0), 5.0, 5.0, lay);
                    Export.newInst(lay, pinClkLay.getPortInst(0), "CLK", ep);

                    PortInst pDiv8Clk = lInstDiv8.findPortInstFromProto(layDiv8.findExport("CLK"));
                    PortInst pSregClk = lInstSreg.findPortInstFromProto(laySreg.findExport("CLK"));

                    NodeInst v12_clk_d8 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-710.0, 1520.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pinClkLay.getPortInst(0), v12_clk_d8.getPortInst(0));

                    NodeInst v12_d8_clk_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(-685.0, 1520.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_clk_d8.getPortInst(0), v12_d8_clk_in.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_d8_clk_in.getPortInst(0), pDiv8Clk);

                    NodeInst v12_clk_sr = NodeInst.makeInstance(via12Proto, new Point2D.Double(-710.0, 2199.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m2, ep, v12_clk_d8.getPortInst(0), v12_clk_sr.getPortInst(0));

                    NodeInst v12_sr_clk_in = NodeInst.makeInstance(via12Proto, new Point2D.Double(-685.0, 2199.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, v12_clk_sr.getPortInst(0), v12_sr_clk_in.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_sr_clk_in.getPortInst(0), pSregClk);

                    // ADDR_IN: pin at (-1060.0, 2210.0), M3 straight across to (-560.0, 2210.0) flying over VDD rail!
                    NodeInst pinAddrLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-1060.0, 2210.0), 5.0, 5.0, lay);
                    Export.newInst(lay, pinAddrLay.getPortInst(0), "ADDR_IN", ep);

                    PortInst pAddrIn = lInstSreg.findPortInstFromProto(laySreg.findExport("ADDR_IN"));
                    NodeInst v12_addr_1 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-1060.0, 2210.0), 5.0, 5.0, lay);
                    NodeInst v23_addr_1 = NodeInst.makeInstance(via23Proto, new Point2D.Double(-1060.0, 2210.0), 6.0, 6.0, lay);
                    ArcInst.makeInstance(m1, ep, pinAddrLay.getPortInst(0), v12_addr_1.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_addr_1.getPortInst(0), v23_addr_1.getPortInst(0));

                    NodeInst v23_addr_2 = NodeInst.makeInstance(via23Proto, new Point2D.Double(-560.0, 2210.0), 6.0, 6.0, lay);
                    NodeInst v12_addr_2 = NodeInst.makeInstance(via12Proto, new Point2D.Double(-560.0, 2210.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m3, ep, v23_addr_1.getPortInst(0), v23_addr_2.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v23_addr_2.getPortInst(0), v12_addr_2.getPortInst(0));
                    ArcInst.makeInstance(m1, ep, v12_addr_2.getPortInst(0), pAddrIn);

                    // DIN: pin at (-1060.0, -560.0)
                    NodeInst pinDinLay = NodeInst.makeInstance(m1Pin, new Point2D.Double(-1060.0, -560.0), 5.0, 5.0, lay);
                    Export.newInst(lay, pinDinLay.getPortInst(0), "DIN", ep);
                    PortInst pWdDin = lInstWd.findPortInstFromProto(layWd.findExport("WD"));
                    NodeInst cPoly_din = NodeInst.makeInstance(conPolyM1, new Point2D.Double(-75.0, -560.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pinDinLay.getPortInst(0), cPoly_din.getPortInst(0));
                    ArcInst.makeInstance(poly, ep, cPoly_din.getPortInst(0), pWdDin);

                    // DOUT: drop down to Y = -700.0 on Metal-2, exported South for Output_Pad alignment
                    PortInst pSaDout = lInstSa.findPortInstFromProto(laySa.findExport("SO"));
                    NodeInst v12_dout = NodeInst.makeInstance(via12Proto, new Point2D.Double(213.5, -555.0), 5.0, 5.0, lay);
                    NodeInst pinDoutLay = NodeInst.makeInstance(m2Pin, new Point2D.Double(213.5, -700.0), 5.0, 5.0, lay);
                    ArcInst.makeInstance(m1, ep, pSaDout, v12_dout.getPortInst(0));
                    ArcInst.makeInstance(m2, ep, v12_dout.getPortInst(0), pinDoutLay.getPortInst(0));
                    Export.newInst(lay, pinDoutLay.getPortInst(0), "DOUT", ep);

                    """

text = text[:idx_f] + new_routing_f + text[idx_g:]

with open("BuildCompactedMacroCleanV11.java", "w") as f:
    f.write(text)

print("BuildCompactedMacroCleanV11.java generated successfully!")
