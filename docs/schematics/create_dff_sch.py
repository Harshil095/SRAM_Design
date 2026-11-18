import re

def build_dff_sch():
    lines = []
    lines.append("# Cell DFF;1{sch}")
    lines.append("CDFF;1{sch}||schematic|1793166000000|1793166000000|")
    lines.append("Ngeneric:Facet-Center|art@0||0|0||||AV")
    
    # Power and Ground
    lines.append("NPower|pwr@0||0|30||||")
    lines.append("NGround|gnd@0||0|-30||||")
    
    # Off-Page Connectors for Exports
    lines.append("NOff-Page|conn@d||-80|15||||")       # D
    lines.append("NOff-Page|conn@clk||-80|-15||||")    # CLK
    lines.append("NOff-Page|conn@q||90|15||||")        # Q
    lines.append("NOff-Page|conn@vdd||0|35|||RRR|")    # vdd
    lines.append("NOff-Page|conn@gnd||0|-35|||R|")     # gnd

    # 6 Inverters:
    # INV1 (CLK -> CLKB) at (-60, -15)
    lines.append("Iinverter;1{ic}|INV1||-60|-15|||D5G4;")
    # INV2 (CLKB -> CLK_buf) at (-40, -15)
    lines.append("Iinverter;1{ic}|INV2||-40|-15|||D5G4;")
    # INV3 (Master Fwd: M_in -> M) at (-30, 15)
    lines.append("Iinverter;1{ic}|INV3||-30|15|||D5G4;")
    # INV4 (Master FB: M -> M_fb) at (-30, 0)
    lines.append("Iinverter;1{ic}|INV4||-30|0|||D5G4;")
    # INV5 (Slave Fwd: S_in -> Q_bar) at (35, 15)
    lines.append("Iinverter;1{ic}|INV5||35|15|||D5G4;")
    # INV6 (Slave Output: Q_bar -> Q) at (70, 15)
    lines.append("Iinverter;1{ic}|INV6||70|15|||D5G4;")

    # 4 Transmission Gates:
    # TG1 (D -> M_in, EN=CLKB, ENB=CLK_buf) at (-55, 15)
    lines.append("Itgate;1{ic}|TG1||-55|15|||D5G4;")
    # TG2 (M_fb -> M_in, EN=CLK_buf, ENB=CLKB) at (-10, 0)
    lines.append("Itgate;1{ic}|TG2||-10|0|||D5G4;")
    # TG3 (M -> S_in, EN=CLK_buf, ENB=CLKB) at (10, 15)
    lines.append("Itgate;1{ic}|TG3||10|15|||D5G4;")
    # TG4 (Q_bar -> S_in, EN=CLKB, ENB=CLK_buf) at (55, 0)
    lines.append("Itgate;1{ic}|TG4||55|0|||D5G4;")

    # Invisible Pin for SPICE test simulation card
    spice_card = (
        "[.include ./tsmc018.lib,.param VDD = 1.8,VDD1 vdd 0 DC 1.8,"
        "* DFF transient test: clock 100MHz (period 10ns),"
        "VCLK CLK 0 PULSE(0 1.8 1n 0.1n 0.1n 4.9n 10n),"
        "* Data pattern: D toggles with setup/hold relative to clock edges,"
        "VD D 0 PWL(0n 0 7n 0 7.2n 1.8 23n 1.8 23.2n 0 35n 0 35.2n 1.8 60n 1.8),"
        "CQ Q 0 20f,"
        ".tran 0.1n 60n]"
    )
    lines.append(f"Ngeneric:Invisible-Pin|pin@sp||-80|35|||||SIM_spice_card(D5G1;)S{spice_card}")

    # Wire Pins for junctions and routing
    lines.append("NWire_Pin|pin@clk_in||-70|-15||||")
    lines.append("NWire_Pin|pin@clkb_out||-50|-15||||")
    lines.append("NWire_Pin|pin@clkb_bus1||-55|-5||||")
    lines.append("NWire_Pin|pin@clkb_bus2||-10|-10||||")
    lines.append("NWire_Pin|pin@clkb_bus3||10|-5||||")
    lines.append("NWire_Pin|pin@clkb_bus4||55|-10||||")
    
    lines.append("NWire_Pin|pin@clkbuf_out||-30|-15||||")
    lines.append("NWire_Pin|pin@clkbuf_bus1||-52|-5||||")
    lines.append("NWire_Pin|pin@clkbuf_bus2||-15|-10||||")
    lines.append("NWire_Pin|pin@clkbuf_bus3||5|-5||||")
    lines.append("NWire_Pin|pin@clkbuf_bus4||58|-10||||")

    lines.append("NWire_Pin|pin@d_in||-70|22||||")
    lines.append("NWire_Pin|pin@m_in1||-45|10.5||||")
    lines.append("NWire_Pin|pin@m_node1||-20|15||||")
    lines.append("NWire_Pin|pin@m_node2||-20|0||||")
    lines.append("NWire_Pin|pin@m_node3||-20|22||||")
    lines.append("NWire_Pin|pin@m_fb1||-15|7||||")
    lines.append("NWire_Pin|pin@s_in1||20|10.5||||")
    lines.append("NWire_Pin|pin@q_bar1||45|15||||")
    lines.append("NWire_Pin|pin@q_bar2||45|7||||")
    lines.append("NWire_Pin|pin@q_out||80|15||||")

    # Wire connections
    # 1. D input: conn@d (-80, 15) -> pin@d_in (-70, 22) -> TG1|IN (-55, 22)
    lines.append("Awire|net@d1|||900|conn@d|a|-80|15|pin@d_in||-70|22")
    lines.append("Awire|net@d2|||1800|pin@d_in||-70|22|TG1|IN|-55|22")

    # 2. CLK input: conn@clk (-80, -15) -> pin@clk_in (-70, -15) -> INV1|in (-64, -15)
    lines.append("Awire|net@c1|||1800|conn@clk|a|-80|-15|pin@clk_in||-70|-15")
    lines.append("Awire|net@c2|||1800|pin@clk_in||-70|-15|INV1|in|-64|-15")

    # 3. CLKB generation: INV1|out (-53.5, -15) -> pin@clkb_out (-50, -15) -> INV2|in (-44, -15)
    lines.append("Awire|net@cb1|||1800|INV1|out|-53.5|-15|pin@clkb_out||-50|-15")
    lines.append("Awire|net@cb2|||1800|pin@clkb_out||-50|-15|INV2|in|-44|-15")

    # 4. CLK_buf generation: INV2|out (-33.5, -15) -> pin@clkbuf_out (-30, -15)
    lines.append("Awire|net@cbuf1|||1800|INV2|out|-33.5|-15|pin@clkbuf_out||-30|-15")

    # 5. Clock controls to TG1:
    # TG1.EN (-60, 15) connects to CLKB: pin@clkb_out (-50, -15) -> TG1|EN (-60, 15)
    lines.append("Awire|net@tg1_en|||900|pin@clkb_out||-50|-15|TG1|EN|-60|15")
    # TG1.ENB (-52, 15) connects to CLK_buf: pin@clkbuf_out (-30, -15) -> TG1|ENB (-52, 15)
    lines.append("Awire|net@tg1_enb|||900|pin@clkbuf_out||-30|-15|TG1|ENB|-52|15")

    # 6. Master forward: TG1|OUT (-55, 10.5) -> pin@m_in1 (-45, 10.5) -> INV3|in (-34, 15)
    lines.append("Awire|net@min1|||1800|TG1|OUT|-55|10.5|pin@m_in1||-45|10.5")
    lines.append("Awire|net@min2|||900|pin@m_in1||-45|10.5|INV3|in|-34|15")

    # 7. Master output (Node M):
    # INV3|out (-23.5, 15) -> pin@m_node1 (-20, 15)
    lines.append("Awire|net@m1|||1800|INV3|out|-23.5|15|pin@m_node1||-20|15")
    # pin@m_node1 (-20, 15) -> pin@m_node2 (-20, 0) -> INV4|in (-34, 0)
    lines.append("Awire|net@m2|||900|pin@m_node1||-20|15|pin@m_node2||-20|0")
    lines.append("Awire|net@m3|||1800|pin@m_node2||-20|0|INV4|in|-34|0")
    # pin@m_node1 (-20, 15) -> pin@m_node3 (-20, 22) -> TG3|IN (10, 22)
    lines.append("Awire|net@m4|||900|pin@m_node1||-20|15|pin@m_node3||-20|22")
    lines.append("Awire|net@m5|||1800|pin@m_node3||-20|22|TG3|IN|10|22")

    # 8. Master feedback (M_fb):
    # INV4|out (-23.5, 0) -> pin@m_fb1 (-15, 7) -> TG2|IN (-10, 7)
    lines.append("Awire|net@mfb1|||900|INV4|out|-23.5|0|pin@m_fb1||-15|7")
    lines.append("Awire|net@mfb2|||1800|pin@m_fb1||-15|7|TG2|IN|-10|7")
    # TG2|OUT (-10, -4.5) loops back to pin@m_in1 (-45, 10.5)
    lines.append("Awire|net@mfb3|||900|TG2|OUT|-10|-4.5|pin@m_in1||-45|10.5")

    # 9. Clock controls to TG2:
    # TG2.EN (-15, 0) connects to CLK_buf: pin@clkbuf_out (-30, -15) -> TG2|EN (-15, 0)
    lines.append("Awire|net@tg2_en|||900|pin@clkbuf_out||-30|-15|TG2|EN|-15|0")
    # TG2.ENB (-7, 0) connects to CLKB: pin@clkb_out (-50, -15) -> TG2|ENB (-7, 0)
    lines.append("Awire|net@tg2_enb|||900|pin@clkb_out||-50|-15|TG2|ENB|-7|0")

    # 10. Clock controls to TG3:
    # TG3.EN (5, 15) connects to CLK_buf
    lines.append("Awire|net@tg3_en|||900|pin@clkbuf_out||-30|-15|TG3|EN|5|15")
    # TG3.ENB (13, 15) connects to CLKB
    lines.append("Awire|net@tg3_enb|||900|pin@clkb_out||-50|-15|TG3|ENB|13|15")

    # 11. Slave forward: TG3|OUT (10, 10.5) -> pin@s_in1 (20, 10.5) -> INV5|in (31, 15)
    lines.append("Awire|net@sin1|||1800|TG3|OUT|10|10.5|pin@s_in1||20|10.5")
    lines.append("Awire|net@sin2|||900|pin@s_in1||20|10.5|INV5|in|31|15")

    # 12. Slave output (Node Q_bar):
    # INV5|out (41.5, 15) -> pin@q_bar1 (45, 15)
    lines.append("Awire|net@qb1|||1800|INV5|out|41.5|15|pin@q_bar1||45|15")
    # pin@q_bar1 (45, 15) -> INV6|in (56, 15)
    lines.append("Awire|net@qb2|||1800|pin@q_bar1||45|15|INV6|in|56|15")
    # pin@q_bar1 (45, 15) -> pin@q_bar2 (45, 7) -> TG4|IN (55, 7)
    lines.append("Awire|net@qb3|||900|pin@q_bar1||45|15|pin@q_bar2||45|7")
    lines.append("Awire|net@qb4|||1800|pin@q_bar2||45|7|TG4|IN|55|7")

    # 13. Slave feedback loop: TG4|OUT (55, -4.5) -> pin@s_in1 (20, 10.5)
    lines.append("Awire|net@sfb1|||900|TG4|OUT|55|-4.5|pin@s_in1||20|10.5")

    # 14. Clock controls to TG4:
    # TG4.EN (50, 0) connects to CLKB
    lines.append("Awire|net@tg4_en|||900|pin@clkb_out||-50|-15|TG4|EN|50|0")
    # TG4.ENB (58, 0) connects to CLK_buf
    lines.append("Awire|net@tg4_enb|||900|pin@clkbuf_out||-30|-15|TG4|ENB|58|0")

    # 15. Main data output Q: INV6|out (76.5, 15) -> pin@q_out (80, 15) -> conn@q (90, 15)
    lines.append("Awire|net@q1|||1800|INV6|out|76.5|15|pin@q_out||80|15")
    lines.append("Awire|net@q2|||1800|pin@q_out||80|15|conn@q|a|90|15")

    # 16. Power and Ground distribution to TG1, TG2, TG3, TG4:
    lines.append("Awire|net@pwr1|||2700|conn@vdd|y|0|33.5|pwr@0||0|30")
    lines.append("Awire|net@pwr2|||1800|pwr@0||0|30|TG1|vdd|-57|22")
    lines.append("Awire|net@pwr3|||1800|pwr@0||0|30|TG2|vdd|-12|7")
    lines.append("Awire|net@pwr4|||1800|pwr@0||0|30|TG3|vdd|8|22")
    lines.append("Awire|net@pwr5|||1800|pwr@0||0|30|TG4|vdd|53|7")

    lines.append("Awire|net@gnd1|||900|gnd@0||0|-30|conn@gnd|y|0|-33.5")
    lines.append("Awire|net@gnd2|||1800|gnd@0||0|-30|TG1|gnd|-57|10.5")
    lines.append("Awire|net@gnd3|||1800|gnd@0||0|-30|TG2|gnd|-12|-4.5")
    lines.append("Awire|net@gnd4|||1800|gnd@0||0|-30|TG3|gnd|8|10.5")
    lines.append("Awire|net@gnd5|||1800|gnd@0||0|-30|TG4|gnd|53|-4.5")

    # Exports
    lines.append("ED||D5G2;|conn@d|a|I")
    lines.append("ECLK||D5G2;|conn@clk|a|I")
    lines.append("EQ||D5G2;|conn@q|a|O")
    lines.append("Evdd||D5G2;|pwr@0||P")
    lines.append("Egnd||D5G2;|gnd@0||G")
    lines.append("X")

    return "\n".join(lines) + "\n"

def build_dff_ic():
    lines = []
    lines.append("# Cell DFF;1{ic}")
    lines.append("CDFF;1{ic}||artwork|1793166000000|1793166000000|E")
    lines.append("Ngeneric:Facet-Center|art@0||0|0||||AV")
    # DFF box with DFF label
    lines.append("NOpened-Thicker-Polygon|art@1||0|0|8|12|||SCHEM_function(D5G2;)SDFF|trace()V[-4/-6,-4/6,4/6,4/-6,-4/-6]")
    # Clock input wedge on the left
    lines.append("NOpened-Thicker-Polygon|art@2||-4|-3|1|2|||trace()V[-4/-4,-2.5/-3,-4/-2]")
    
    # Pins
    # D input pin on left at (-6, 3)
    lines.append("Nschematic:Bus_Pin|pin@0||-6|3||||")
    lines.append("Nschematic:Wire_Pin|pin@1||-4|3||||")
    # CLK input pin on left at (-6, -3)
    lines.append("Nschematic:Bus_Pin|pin@2||-6|-3||||")
    lines.append("Nschematic:Wire_Pin|pin@3||-4|-3||||")
    # Q output pin on right at (6, 0)
    lines.append("Nschematic:Bus_Pin|pin@4||6|0||||")
    lines.append("Nschematic:Wire_Pin|pin@5||4|0||||")
    # vdd pin on top at (0, 8)
    lines.append("Nschematic:Bus_Pin|pin@6||0|8||||")
    lines.append("Nschematic:Wire_Pin|pin@7||0|6||||")
    # gnd pin on bottom at (0, -8)
    lines.append("Nschematic:Bus_Pin|pin@8||0|-8||||")
    lines.append("Nschematic:Wire_Pin|pin@9||0|-6||||")

    # Wires to pins
    lines.append("Aschematic:wire|net@0|||0|pin@0||-6|3|pin@1||-4|3")
    lines.append("Aschematic:wire|net@1|||0|pin@2||-6|-3|pin@3||-4|-3")
    lines.append("Aschematic:wire|net@2|||1800|pin@5||4|0|pin@4||6|0")
    lines.append("Aschematic:wire|net@3|||2700|pin@7||0|6|pin@6||0|8")
    lines.append("Aschematic:wire|net@4|||900|pin@9||0|-6|pin@8||0|-8")

    # Exports
    lines.append("ED||D5G2;|pin@0||I")
    lines.append("ECLK||D5G2;|pin@2||I")
    lines.append("EQ||D5G2;|pin@4||O")
    lines.append("Evdd||D5G2;|pin@6||P")
    lines.append("Egnd||D5G2;|pin@8||G")
    lines.append("X")

    return "\n".join(lines) + "\n"

# Add DFF;1{ic} and DFF;1{sch} to SRAM.jelib
with open("SRAM.jelib", "r") as f:
    text = f.read()

dff_ic = build_dff_ic()
dff_sch = build_dff_sch()

# Append to jelib before end of file or at a good spot
text = text + "\n" + dff_ic + "\n" + dff_sch

with open("SRAM.jelib", "w") as f:
    f.write(text)

print("Added DFF;1{ic} and DFF;1{sch} to SRAM.jelib successfully!")
