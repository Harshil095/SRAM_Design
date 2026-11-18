import os

nodes = []
arcs = []
exports = []

with open("macro_sch_data.txt") as f:
    for line in f:
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split("|")
        if parts[0] == "NODE":
            # name, proto, x, y, w, h
            nodes.append({
                "name": parts[1],
                "proto": parts[2],
                "x": float(parts[3]),
                "y": float(parts[4]),
                "w": float(parts[5]),
                "h": float(parts[6])
            })
        elif parts[0] == "ARC":
            arcs.append({
                "hx": float(parts[1]),
                "hy": float(parts[2]),
                "tx": float(parts[3]),
                "ty": float(parts[4])
            })
        elif parts[0] == "EXP":
            exports.append({
                "name": parts[1],
                "x": float(parts[2]),
                "y": float(parts[3])
            })

# Coordinate transform:
# Electric Y increases upwards. SVG Y increases downwards.
min_x = min(min(n["x"] for n in nodes), min(a["hx"] for a in arcs)) - 40
max_x = max(max(n["x"] for n in nodes), max(a["hx"] for a in arcs)) + 40
min_y = min(min(n["y"] for n in nodes), min(a["hy"] for a in arcs)) - 40
max_y = max(max(n["y"] for n in nodes), max(a["hy"] for a in arcs)) + 40

scale = 4.0
pad = 60.0
svg_w = int((max_x - min_x) * scale + pad * 2)
svg_h = int((max_y - min_y) * scale + pad * 2)

def to_svg(x, y):
    sx = pad + (x - min_x) * scale
    sy = pad + (max_y - y) * scale
    return sx, sy

svg_lines = []
svg_lines.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {svg_w} {svg_h}" width="{svg_w}" height="{svg_h}">')
svg_lines.append('<defs>')
svg_lines.append('  <style>')
svg_lines.append('    .bg { fill: #0f172a; }')
svg_lines.append('    .grid { stroke: #1e293b; stroke-width: 1; stroke-dasharray: 4,4; }')
svg_lines.append('    .wire { stroke: #38bdf8; stroke-width: 2.5; stroke-linecap: round; stroke-linejoin: round; }')
svg_lines.append('    .wire-pwr { stroke: #f43f5e; stroke-width: 2; }')
svg_lines.append('    .wire-gnd { stroke: #10b981; stroke-width: 2; }')
svg_lines.append('    .box-block { fill: #1e293b; stroke: #60a5fa; stroke-width: 2; rx: 6; }')
svg_lines.append('    .box-gate { fill: #1e1b4b; stroke: #a855f7; stroke-width: 1.5; rx: 4; }')
svg_lines.append('    .box-pwr { fill: #f43f5e; stroke: none; }')
svg_lines.append('    .box-gnd { fill: #10b981; stroke: none; }')
svg_lines.append('    .pin-dot { fill: #0284c7; stroke: #38bdf8; stroke-width: 1.5; }')
svg_lines.append('    .port-label { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace; font-size: 11px; fill: #f8fafc; font-weight: 600; text-anchor: middle; }')
svg_lines.append('    .block-title { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 13px; fill: #93c5fd; font-weight: bold; text-anchor: middle; }')
svg_lines.append('    .export-tag { fill: #f59e0b; stroke: #d97706; stroke-width: 1.5; rx: 4; }')
svg_lines.append('    .export-text { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace; font-size: 12px; fill: #0f172a; font-weight: bold; text-anchor: middle; }')
svg_lines.append('    .title { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 22px; fill: #f8fafc; font-weight: bold; }')
svg_lines.append('    .subtitle { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 13px; fill: #94a3b8; }')
svg_lines.append('  </style>')
svg_lines.append('</defs>')

# Background
svg_lines.append(f'<rect width="{svg_w}" height="{svg_h}" class="bg"/>')

# Title Banner
svg_lines.append(f'<text x="60" y="50" class="title">SRAM_256_Macro{{sch}} (Clean Orthogonal Architecture)</text>')
svg_lines.append(f'<text x="60" y="75" class="subtitle">Complete 256-Bit SRAM Macro Schematic - Pitch-Matched Decoders, Modular Datapath, Clean Orthogonal Bus Routing</text>')

# 1. Draw Wires (Arcs)
for arc in arcs:
    x1, y1 = to_svg(arc["hx"], arc["hy"])
    x2, y2 = to_svg(arc["tx"], arc["ty"])
    svg_lines.append(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" class="wire"/>')

# 2. Draw Nodes
for node in nodes:
    x, y = to_svg(node["x"], node["y"])
    w = max(node["w"] * scale, 10.0)
    h = max(node["h"] * scale, 10.0)
    proto = node["proto"]
    name = node["name"]

    if proto in ["Decoder_4x16", "ShiftReg_8bit", "Div8_Counter", "SRAM_Core_Datapath", "sense_amplifier", "write_driver"]:
        # Large functional block
        bx = x - w/2
        by = y - h/2
        svg_lines.append(f'<rect x="{bx:.1f}" y="{by:.1f}" width="{w:.1f}" height="{h:.1f}" class="box-block"/>')
        disp_name = proto.replace("_", " ")
        if "Decoder" in proto:
            disp_name = "Row Decoder" if "Decoder_@0" in name else "Col Decoder"
        svg_lines.append(f'<text x="{x:.1f}" y="{(y + 4):.1f}" class="block-title">{disp_name}</text>')
    elif proto in ["inverter", "nand4"]:
        bx = x - w/2
        by = y - h/2
        svg_lines.append(f'<rect x="{bx:.1f}" y="{by:.1f}" width="{w:.1f}" height="{h:.1f}" class="box-gate"/>')
        gate_label = "INV" if proto == "inverter" else "NAND4"
        svg_lines.append(f'<text x="{x:.1f}" y="{(y + 3):.1f}" class="port-label">{gate_label}</text>')
    elif proto == "Power":
        svg_lines.append(f'<polygon points="{x:.1f},{(y-6):.1f} {(x-6):.1f},{(y+4):.1f} {(x+6):.1f},{(y+4):.1f}" class="box-pwr"/>')
        svg_lines.append(f'<text x="{x:.1f}" y="{(y-9):.1f}" class="port-label" style="fill:#f43f5e;">VDD</text>')
    elif proto == "Ground":
        svg_lines.append(f'<polygon points="{x:.1f},{(y+6):.1f} {(x-6):.1f},{(y-4):.1f} {(x+6):.1f},{(y-4):.1f}" class="box-gnd"/>')
        svg_lines.append(f'<text x="{x:.1f}" y="{(y+16):.1f}" class="port-label" style="fill:#10b981;">GND</text>')
    elif proto == "Wire_Pin":
        svg_lines.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="3" class="pin-dot"/>')
    elif proto == "Off-Page":
        # Export connector
        pass

# 3. Draw Exports
for exp in exports:
    x, y = to_svg(exp["x"], exp["y"])
    name = exp["name"]
    tw = max(len(name) * 9 + 16, 50)
    th = 22
    bx = x - tw/2
    by = y - th/2
    svg_lines.append(f'<rect x="{bx:.1f}" y="{by:.1f}" width="{tw}" height="{th}" class="export-tag"/>')
    svg_lines.append(f'<text x="{x:.1f}" y="{(y + 4.5):.1f}" class="export-text">{name}</text>')

svg_lines.append('</svg>')

os.makedirs("docs", exist_ok=True)
svg_path = "docs/schematic_SRAM_256_Macro.svg"
with open(svg_path, "w") as f:
    f.write("\n".join(svg_lines))

print(f"Rendered {svg_path} successfully ({svg_w}x{svg_h} px)!")
