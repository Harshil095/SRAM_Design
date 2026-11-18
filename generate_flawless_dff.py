import re

def generate():
    lines = []
    lines.append("# Cell DFF;1{lay}")
    lines.append("CDFF;1{lay}||mocmos|1793166000000|1793166000000|")
    lines.append("Ngeneric:Facet-Center|art@0||0|0||||AV")

    subcells = [
        ("INV1", "inverter", 0),
        ("INV2", "inverter", 60),
        ("TG1", "tgate", 120),
        ("INV3", "inverter", 180),
        ("INV4", "inverter", 240),
        ("TG2", "tgate", 300),
        ("TG3", "tgate", 360),
        ("INV5", "inverter", 420),
        ("TG4", "tgate", 480),
        ("INV6", "inverter", 540),
    ]

    for name, cell_type, x in subcells:
        lines.append(f"I{cell_type};1{{lay}}|{name}||{x}|0|||D5G4;")

    # Export Pins
    lines.append("NMetal-1-Pin|pin@0||80|0||||")       # D
    lines.append("NMetal-1-Pin|pin@1||-45|-1||||")     # CLK
    lines.append("NMetal-1-Pin|pin@2||565|-1.5||||")   # Q

    # VDD rail pins at Y = 36:
    for i, (name, _, x) in enumerate(subcells):
        px = x if "TG" in name else x - 9.5
        lines.append(f"NMetal-1-Pin|pin@{50+i}||{px}|36||||")

    # GND rail pins at Y = -36:
    for i, (name, _, x) in enumerate(subcells):
        px = x if "TG" in name else x - 9.5
        lines.append(f"NMetal-1-Pin|pin@{60+i}||{px}|-36||||")

    # TG Poly Contacts (placed at Xc - 16):
    tg_pcon_map = {
        "TG1": (104, -20, 22),
        "TG2": (284, -20, 22),
        "TG3": (344, -20, 22),
        "TG4": (464, -20, 22)
    }
    pcon_idx = 0
    for tg, (px, py_en, py_enb) in tg_pcon_map.items():
        lines.append(f"NMetal-1-Polysilicon-1-Con|pcon@{pcon_idx}||{px}|{py_en}||||")
        lines.append(f"NMetal-1-Polysilicon-1-Con|pcon@{pcon_idx+1}||{px}|{py_enb}||||")
        pcon_idx += 2

    # Vias (All integer indexed!):
    # CLKB:
    lines.append("NMetal-1-Metal-2-Con|via@0||16|-1.5||||")    # from INV1.out
    lines.append("NMetal-1-Metal-2-Con|via@1||96|-20||||")     # TG1.EN
    lines.append("NMetal-1-Metal-2-Con|via@2||276|22||||")     # TG2.ENB
    lines.append("NMetal-1-Metal-2-Con|via@3||336|22||||")     # TG3.ENB
    lines.append("NMetal-1-Metal-2-Con|via@4||456|-20||||")    # TG4.EN

    # CLK_buf:
    lines.append("NMetal-1-Metal-2-Con|via@10||72|-1.5||||")   # from INV2.out
    lines.append("NMetal-1-Metal-2-Con|via@11||72|-44||||")    # hop over GND
    lines.append("NMetal-1-Metal-2-Con|via@12||72|-60||||")    # hop to bus at Y = -60

    lines.append("NMetal-1-Metal-2-Con|via@13||88|-60||||")    # TG1.ENB from bus
    lines.append("NMetal-1-Metal-2-Con|via@14||88|-44||||")    # hop across CLKB
    lines.append("NMetal-1-Metal-2-Con|via@15||88|22||||")     # drop to pcon@1

    lines.append("NMetal-1-Metal-2-Con|via@16||268|-60||||")   # TG2.EN from bus
    lines.append("NMetal-1-Metal-2-Con|via@17||268|-44||||")   # hop across CLKB
    lines.append("NMetal-1-Metal-2-Con|via@18||268|-20||||")   # drop to pcon@2

    lines.append("NMetal-1-Metal-2-Con|via@19||328|-60||||")   # TG3.EN from bus
    lines.append("NMetal-1-Metal-2-Con|via@20||328|-44||||")   # hop across CLKB
    lines.append("NMetal-1-Metal-2-Con|via@21||328|-20||||")   # drop to pcon@4

    lines.append("NMetal-1-Metal-2-Con|via@22||448|-60||||")   # TG4.ENB from bus
    lines.append("NMetal-1-Metal-2-Con|via@23||448|-44||||")   # hop across CLKB
    lines.append("NMetal-1-Metal-2-Con|via@24||448|22||||")    # drop to pcon@7

    # Master Feedback (Y = -68):
    lines.append("NMetal-1-Metal-2-Con|via@30||306.5|-20||||") # from TG2.OUT
    lines.append("NMetal-1-Metal-2-Con|via@31||306.5|-44||||") # hop across GND
    lines.append("NMetal-1-Metal-2-Con|via@32||306.5|-68||||") # to bus
    lines.append("NMetal-1-Metal-2-Con|via@33||140|-68||||")   # from bus
    lines.append("NMetal-1-Metal-2-Con|via@34||140|-44||||")   # hop across buses
    lines.append("NMetal-1-Metal-2-Con|via@35||140|-20||||")   # drop to INV3.in

    # Slave Feedback (Y = -68):
    lines.append("NMetal-1-Metal-2-Con|via@40||486.5|-20||||") # from TG4.OUT
    lines.append("NMetal-1-Metal-2-Con|via@41||486.5|-44||||") # hop across GND
    lines.append("NMetal-1-Metal-2-Con|via@42||486.5|-68||||") # to bus
    lines.append("NMetal-1-Metal-2-Con|via@43||380|-68||||")   # from bus
    lines.append("NMetal-1-Metal-2-Con|via@44||380|-44||||")   # hop across buses
    lines.append("NMetal-1-Metal-2-Con|via@45||380|-20||||")   # drop to INV5.in

    # Forward Buses above VDD (Y = +46):
    lines.append("NMetal-1-Metal-2-Con|via@50||200|10||||")    # Node M from INV3.out
    lines.append("NMetal-1-Metal-2-Con|via@51||353.5|10||||")  # Node M to TG3.IN
    lines.append("NMetal-1-Metal-2-Con|via@52||440|10||||")    # Node Q_bar from INV5.out
    lines.append("NMetal-1-Metal-2-Con|via@53||515|10||||")    # Node Q_bar to INV6.in

    # Metal-2 Pins:
    lines.append("NMetal-2-Pin|m2pin@0||16|-52||||")           # CLKB bus
    lines.append("NMetal-2-Pin|m2pin@1||96|-52||||")
    lines.append("NMetal-2-Pin|m2pin@2||276|-52||||")
    lines.append("NMetal-2-Pin|m2pin@3||336|-52||||")
    lines.append("NMetal-2-Pin|m2pin@4||456|-52||||")

    lines.append("NMetal-2-Pin|m2pin@30||200|46||||")          # Node M bus
    lines.append("NMetal-2-Pin|m2pin@31||353.5|46||||")

    lines.append("NMetal-2-Pin|m2pin@50||440|46||||")          # Node Q_bar bus
    lines.append("NMetal-2-Pin|m2pin@51||515|46||||")

    # Metal-1 Intermediate Pins:
    lines.append("NMetal-1-Pin|m1pin@0||140|0||||")
    lines.append("NMetal-1-Pin|m1pin@1||140|-1||||")
    lines.append("NMetal-1-Pin|m1pin@2||200|-1.5||||")
    lines.append("NMetal-1-Pin|m1pin@3||200|-1||||")
    lines.append("NMetal-1-Pin|m1pin@4||260|-1.5||||")
    lines.append("NMetal-1-Pin|m1pin@5||260|0||||")
    lines.append("NMetal-1-Pin|m1pin@6||380|0||||")
    lines.append("NMetal-1-Pin|m1pin@7||380|-1||||")
    lines.append("NMetal-1-Pin|m1pin@8||440|-1.5||||")
    lines.append("NMetal-1-Pin|m1pin@9||440|0||||")
    lines.append("NMetal-1-Pin|m1pin@20||20|-1.5||||")
    lines.append("NMetal-1-Pin|m1pin@21||20|-10||||")
    lines.append("NMetal-1-Pin|m1pin@22||30|-10||||")
    lines.append("NMetal-1-Pin|m1pin@23||30|-1||||")
    lines.append("NMetal-1-Pin|m1pin@50||200|12.5||||")
    lines.append("NMetal-1-Pin|m1pin@51||353.5|12.5||||")
    lines.append("NMetal-1-Pin|m1pin@52||440|12.5||||")
    lines.append("NMetal-1-Pin|m1pin@53||515|12.5||||")
        
    # === ARCS ===
    net_id = 0

    # 1. VDD Rail (Metal-1):
    for i, (name, _, x) in enumerate(subcells):
        if "TG" in name:
            lines.append(f"AMetal-1|net@{net_id}||1|S900|{name}|vdd|{x}|32|pin@{50+i}||{x}|36")
        else:
            lines.append(f"AMetal-1|net@{net_id}||1|S900|{name}|vdd|{x-9.5}|21.5|pin@{50+i}||{x-9.5}|36")
        net_id += 1

    for i in range(len(subcells) - 1):
        x1 = subcells[i][2] if "TG" in subcells[i][0] else subcells[i][2] - 9.5
        x2 = subcells[i+1][2] if "TG" in subcells[i+1][0] else subcells[i+1][2] - 9.5
        lines.append(f"AMetal-1|net@{net_id}||1|S1800|pin@{50+i}||{x1}|36|pin@{50+i+1}||{x2}|36")
        net_id += 1

    # 2. GND Rail (Metal-1):
    for i, (name, _, x) in enumerate(subcells):
        if "TG" in name:
            lines.append(f"AMetal-1|net@{net_id}||1|S900|{name}|gnd|{x}|-32|pin@{60+i}||{x}|-36")
        else:
            lines.append(f"AMetal-1|net@{net_id}||1|S900|{name}|gnd|{x-9.5}|-23|pin@{60+i}||{x-9.5}|-36")
        net_id += 1

    for i in range(len(subcells) - 1):
        x1 = subcells[i][2] if "TG" in subcells[i][0] else subcells[i][2] - 9.5
        x2 = subcells[i+1][2] if "TG" in subcells[i+1][0] else subcells[i+1][2] - 9.5
        lines.append(f"AMetal-1|net@{net_id}||1|S1800|pin@{60+i}||{x1}|-36|pin@{60+i+1}||{x2}|-36")
        net_id += 1

    # 3. TG Poly Arcs:
    pcon_idx = 0
    for tg, (px, py_en, py_enb) in tg_pcon_map.items():
        tg_x = 120 if tg=="TG1" else 300 if tg=="TG2" else 360 if tg=="TG3" else 480
        lines.append(f"APolysilicon-1|net@{net_id}|||S1800|pcon@{pcon_idx}||{px}|{py_en}|{tg}|EN|{tg_x}|{py_en}")
        net_id += 1
        lines.append(f"APolysilicon-1|net@{net_id}|||S1800|pcon@{pcon_idx+1}||{px}|{py_enb}|{tg}|ENB|{tg_x}|{py_enb}")
        net_id += 1
        pcon_idx += 2

    # 4. CLK Input:
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|pin@1||-45|-1|INV1|in|-25|-1")
    net_id += 1

    # 5. CLKB:
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|INV1|out|8|-1.5|via@0||16|-1.5")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|via@0||16|-1.5|m1pin@20||20|-1.5")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|m1pin@20||20|-1.5|m1pin@21||20|-10")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|m1pin@21||20|-10|m1pin@22||30|-10")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|m1pin@22||30|-10|m1pin@23||30|-1")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|m1pin@23||30|-1|INV2|in|35|-1")
    net_id += 1

    lines.append(f"AMetal-2|net@{net_id}||1|S900|via@0||16|-1.5|m2pin@0||16|-52")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S1800|m2pin@0||16|-52|m2pin@1||96|-52")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S1800|m2pin@1||96|-52|m2pin@2||276|-52")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S1800|m2pin@2||276|-52|m2pin@3||336|-52")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S1800|m2pin@3||336|-52|m2pin@4||456|-52")
    net_id += 1

    # Drops for CLKB:
    lines.append(f"AMetal-2|net@{net_id}||1|S900|m2pin@1||96|-52|via@1||96|-20")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|via@1||96|-20|pcon@0||104|-20")
    net_id += 1

    lines.append(f"AMetal-2|net@{net_id}||1|S900|m2pin@2||276|-52|via@2||276|22")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|via@2||276|22|pcon@3||284|22")
    net_id += 1

    lines.append(f"AMetal-2|net@{net_id}||1|S900|m2pin@3||336|-52|via@3||336|22")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|via@3||336|22|pcon@5||344|22")
    net_id += 1

    lines.append(f"AMetal-2|net@{net_id}||1|S900|m2pin@4||456|-52|via@4||456|-20")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|via@4||456|-20|pcon@6||464|-20")
    net_id += 1

    # 6. CLK_buf:
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|INV2|out|68|-1.5|via@10||72|-1.5")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S900|via@10||72|-1.5|via@11||72|-44")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|via@11||72|-44|via@12||72|-60")
    net_id += 1
    # Bus at Y = -60 on Metal-2:
    lines.append(f"AMetal-2|net@{net_id}||1|S1800|via@12||72|-60|via@13||88|-60")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S1800|via@13||88|-60|via@16||268|-60")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S1800|via@16||268|-60|via@19||328|-60")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S1800|via@19||328|-60|via@22||448|-60")
    net_id += 1

    # Drops for CLK_buf:
    # TG1.ENB:
    lines.append(f"AMetal-1|net@{net_id}||1|S900|via@13||88|-60|via@14||88|-44")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S900|via@14||88|-44|via@15||88|22")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|via@15||88|22|pcon@1||104|22")
    net_id += 1

    # TG2.EN:
    lines.append(f"AMetal-1|net@{net_id}||1|S900|via@16||268|-60|via@17||268|-44")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S900|via@17||268|-44|via@18||268|-20")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|via@18||268|-20|pcon@2||284|-20")
    net_id += 1

    # TG3.EN:
    lines.append(f"AMetal-1|net@{net_id}||1|S900|via@19||328|-60|via@20||328|-44")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S900|via@20||328|-44|via@21||328|-20")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|via@21||328|-20|pcon@4||344|-20")
    net_id += 1

    # TG4.ENB:
    lines.append(f"AMetal-1|net@{net_id}||1|S900|via@22||448|-60|via@23||448|-44")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S900|via@23||448|-44|via@24||448|22")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|via@24||448|22|pcon@7||464|22")
    net_id += 1

    # 7. D Input:
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|pin@0||80|0|TG1|IN|113.5|0")
    net_id += 1

    # 8. Master Forward / Feedback:
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|TG1|OUT|126.5|0|m1pin@0||140|0")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|m1pin@0||140|0|m1pin@1||140|-1")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|m1pin@1||140|-1|INV3|in|155|-1")
    net_id += 1

    lines.append(f"AMetal-1|net@{net_id}||1|S900|m1pin@0||140|0|via@35||140|-20")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S900|via@35||140|-20|via@34||140|-44")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|via@34||140|-44|via@33||140|-68")
    net_id += 1

    lines.append(f"AMetal-2|net@{net_id}||1|S1800|via@33||140|-68|via@32||306.5|-68")
    net_id += 1

    lines.append(f"AMetal-1|net@{net_id}||1|S900|via@32||306.5|-68|via@31||306.5|-44")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S900|via@31||306.5|-44|via@30||306.5|-20")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|via@30||306.5|-20|TG2|OUT|306.5|0")
    net_id += 1

    # 9. Node M (INV3.out):
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|INV3|out|188|-1.5|m1pin@2||200|-1.5")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|m1pin@2||200|-1.5|m1pin@3||200|-1")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|m1pin@3||200|-1|INV4|in|215|-1")
    net_id += 1

    lines.append(f"AMetal-1|net@{net_id}||1|S900|m1pin@2||200|-1.5|via@50||200|10")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|via@50||200|10|m1pin@50||200|12.5")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S900|via@50||200|10|m2pin@30||200|46")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S1800|m2pin@30||200|46|m2pin@31||353.5|46")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S900|via@51||353.5|10|m2pin@31||353.5|46")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|TG3|IN|353.5|0|via@51||353.5|10")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|via@51||353.5|10|m1pin@51||353.5|12.5")
    net_id += 1

    # 10. INV4.out to TG2.IN:
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|INV4|out|248|-1.5|m1pin@4||260|-1.5")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|m1pin@4||260|-1.5|m1pin@5||260|0")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|m1pin@5||260|0|TG2|IN|293.5|0")
    net_id += 1

    # 11. Slave Forward / Feedback:
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|TG3|OUT|366.5|0|m1pin@6||380|0")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|m1pin@6||380|0|m1pin@7||380|-1")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|m1pin@7||380|-1|INV5|in|395|-1")
    net_id += 1

    lines.append(f"AMetal-1|net@{net_id}||1|S900|m1pin@6||380|0|via@45||380|-20")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S900|via@45||380|-20|via@44||380|-44")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|via@44||380|-44|via@43||380|-68")
    net_id += 1

    lines.append(f"AMetal-2|net@{net_id}||1|S1800|via@43||380|-68|via@42||486.5|-68")
    net_id += 1

    lines.append(f"AMetal-1|net@{net_id}||1|S900|via@42||486.5|-68|via@41||486.5|-44")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S900|via@41||486.5|-44|via@40||486.5|-20")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|via@40||486.5|-20|TG4|OUT|486.5|0")
    net_id += 1

    # 12. Node Q_bar (INV5.out):
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|INV5|out|428|-1.5|m1pin@8||440|-1.5")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|m1pin@8||440|-1.5|m1pin@9||440|0")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|m1pin@9||440|0|TG4|IN|473.5|0")
    net_id += 1

    lines.append(f"AMetal-1|net@{net_id}||1|S900|m1pin@8||440|-1.5|via@52||440|10")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|via@52||440|10|m1pin@52||440|12.5")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S900|via@52||440|10|m2pin@50||440|46")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S1800|m2pin@50||440|46|m2pin@51||515|46")
    net_id += 1
    lines.append(f"AMetal-2|net@{net_id}||1|S900|via@53||515|10|m2pin@51||515|46")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|m1pin@53||515|12.5|via@53||515|10")
    net_id += 1
    lines.append(f"AMetal-1|net@{net_id}||1|S900|via@53||515|10|INV6|in|515|-1")
    net_id += 1

    # 13. Q Output:
    lines.append(f"AMetal-1|net@{net_id}||1|S1800|INV6|out|548|-1.5|pin@2||565|-1.5")
    net_id += 1

    # Exports
    lines.append("ED||D5G2;|pin@0||I")
    lines.append("ECLK||D5G2;|pin@1||I")
    lines.append("EQ||D5G2;|pin@2||O")
    lines.append("Evdd||D5G2;|pin@52||P")
    lines.append("Egnd||D5G2;|pin@62||G")
    lines.append("X")

    return "\n".join(lines)

with open("SRAM.jelib", "r") as f:
    text = f.read()

dff_lay_text = generate()

start_marker = "# Cell DFF;1{lay}"
end_marker = "\nX\n"

idx = text.find(start_marker)
end_idx = text.find(end_marker, idx) + 3
new_text = text[:idx] + dff_lay_text + "\n" + text[end_idx:]

with open("SRAM.jelib", "w") as f:
    f.write(new_text)

print("generate_flawless_dff applied successfully!")
