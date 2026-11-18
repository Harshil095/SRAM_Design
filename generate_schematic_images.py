import subprocess
import os

svg_tgate = '''<svg width="650" height="420" xmlns="http://www.w3.org/2000/svg" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif">
  <defs>
    <filter id="shadow" x="-5%" y="-5%" width="110%" height="110%">
      <feDropShadow dx="2" dy="3" stdDeviation="3" flood-opacity="0.15"/>
    </filter>
  </defs>

  <!-- Background -->
  <rect width="100%" height="100%" fill="#ffffff" rx="12"/>
  <rect x="15" y="15" width="620" height="390" fill="#fcfcfd" stroke="#e2e8f0" stroke-width="2" rx="10"/>

  <!-- Title Badge -->
  <rect x="35" y="28" width="220" height="34" rx="6" fill="#1e293b"/>
  <text x="45" y="51" fill="#ffffff" font-size="16" font-weight="bold">tgate{sch} (Level 1)</text>
  <text x="270" y="51" fill="#64748b" font-size="14">CMOS Full Transmission Gate</text>

  <!-- Input Port IN -->
  <line x1="50" y1="210" x2="160" y2="210" stroke="#0284c7" stroke-width="3"/>
  <circle cx="50" cy="210" r="6" fill="#0284c7"/>
  <text x="45" y="195" font-size="15" font-weight="bold" fill="#0369a1">IN</text>
  <text x="45" y="235" font-size="11" fill="#64748b">(from BL/BLB)</text>

  <!-- Branching to PMOS and NMOS -->
  <line x1="160" y1="210" x2="160" y2="120" stroke="#0284c7" stroke-width="3"/>
  <line x1="160" y1="210" x2="160" y2="300" stroke="#0284c7" stroke-width="3"/>
  <circle cx="160" cy="210" r="4" fill="#0284c7"/>

  <!-- PMOS Branch (Top) -->
  <line x1="160" y1="120" x2="250" y2="120" stroke="#0284c7" stroke-width="3"/>
  
  <!-- PMOS Symbol -->
  <g filter="url(#shadow)">
    <rect x="250" y="80" width="140" height="80" rx="8" fill="#fef2f2" stroke="#dc2626" stroke-width="2.5"/>
    <text x="262" y="108" font-size="14" font-weight="bold" fill="#991b1b">PMOS (Mp)</text>
    <text x="262" y="128" font-size="12" font-weight="600" fill="#b91c1c">Width = 12 (1.08µm)</text>
    <text x="262" y="146" font-size="12" fill="#7f1d1d">Length = 2 (0.18µm)</text>
    <!-- Inversion Bubble -->
    <circle cx="320" cy="70" r="7" fill="#ffffff" stroke="#dc2626" stroke-width="2.5"/>
  </g>

  <!-- PMOS Gate Connection (SEL_B) -->
  <line x1="320" y1="63" x2="320" y2="30" stroke="#9333ea" stroke-width="3"/>
  <circle cx="320" cy="30" r="6" fill="#9333ea"/>
  <text x="332" y="42" font-size="14" font-weight="bold" fill="#7e22ce">SEL_B (Gate)</text>

  <!-- PMOS Bulk Connection (VDD) -->
  <line x1="370" y1="80" x2="420" y2="60" stroke="#ea580c" stroke-width="2"/>
  <circle cx="420" cy="60" r="4" fill="#ea580c"/>
  <text x="428" y="64" font-size="12" font-weight="bold" fill="#c2410c">VDD (Bulk)</text>

  <!-- NMOS Branch (Bottom) -->
  <line x1="160" y1="300" x2="250" y2="300" stroke="#0284c7" stroke-width="3"/>

  <!-- NMOS Symbol -->
  <g filter="url(#shadow)">
    <rect x="250" y="260" width="140" height="80" rx="8" fill="#eff6ff" stroke="#2563eb" stroke-width="2.5"/>
    <text x="262" y="288" font-size="14" font-weight="bold" fill="#1e40af">NMOS (Mn)</text>
    <text x="262" y="308" font-size="12" font-weight="600" fill="#1d4ed8">Width = 8 (0.72µm)</text>
    <text x="262" y="326" font-size="12" fill="#1e3a8a">Length = 2 (0.18µm)</text>
  </g>

  <!-- NMOS Gate Connection (SEL) -->
  <line x1="320" y1="340" x2="320" y2="380" stroke="#059669" stroke-width="3"/>
  <circle cx="320" cy="380" r="6" fill="#059669"/>
  <text x="332" y="385" font-size="14" font-weight="bold" fill="#047857">SEL (Gate)</text>

  <!-- NMOS Bulk Connection (GND) -->
  <line x1="370" y1="340" x2="420" y2="360" stroke="#475569" stroke-width="2"/>
  <circle cx="420" cy="360" r="4" fill="#475569"/>
  <text x="428" y="364" font-size="12" font-weight="bold" fill="#334155">GND (Bulk)</text>

  <!-- Output recombination -->
  <line x1="390" y1="120" x2="480" y2="120" stroke="#0284c7" stroke-width="3"/>
  <line x1="390" y1="300" x2="480" y2="300" stroke="#0284c7" stroke-width="3"/>
  <line x1="480" y1="120" x2="480" y2="300" stroke="#0284c7" stroke-width="3"/>
  <circle cx="480" cy="210" r="4" fill="#0284c7"/>
  <line x1="480" y1="210" x2="590" y2="210" stroke="#0284c7" stroke-width="3"/>
  <circle cx="590" cy="210" r="6" fill="#0284c7"/>
  <text x="545" y="195" font-size="15" font-weight="bold" fill="#0369a1">OUT</text>
  <text x="510" y="235" font-size="11" fill="#64748b">(to GBL/GBLB)</text>
</svg>'''

svg_col_mux = '''<svg width="780" height="520" xmlns="http://www.w3.org/2000/svg" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif">
  <defs>
    <filter id="shadow2" x="-5%" y="-5%" width="110%" height="110%">
      <feDropShadow dx="2" dy="3" stdDeviation="3" flood-opacity="0.15"/>
    </filter>
  </defs>

  <rect width="100%" height="100%" fill="#ffffff" rx="12"/>
  <rect x="15" y="15" width="750" height="490" fill="#fcfcfd" stroke="#e2e8f0" stroke-width="2" rx="10"/>

  <!-- Title Badge -->
  <rect x="35" y="28" width="260" height="34" rx="6" fill="#1e293b"/>
  <text x="45" y="51" fill="#ffffff" font-size="16" font-weight="bold">col_mux_1x{sch} (Level 2)</text>
  <text x="310" y="51" fill="#64748b" font-size="14">Single Column Switch Pair with Local Inverter</text>

  <!-- Inputs from Column -->
  <line x1="40" y1="130" x2="220" y2="130" stroke="#0284c7" stroke-width="3"/>
  <circle cx="40" cy="130" r="6" fill="#0284c7"/>
  <text x="40" y="115" font-size="16" font-weight="bold" fill="#0369a1">BL (Column Bitline)</text>

  <line x1="40" y1="390" x2="220" y2="390" stroke="#0284c7" stroke-width="3"/>
  <circle cx="40" cy="390" r="6" fill="#0284c7"/>
  <text x="40" y="375" font-size="16" font-weight="bold" fill="#0369a1">BLB (Column Bitline Bar)</text>

  <!-- tgate_bl Instance Box -->
  <g filter="url(#shadow2)">
    <rect x="220" y="90" width="220" height="80" rx="8" fill="#f8fafc" stroke="#0284c7" stroke-width="2.5"/>
    <text x="240" y="125" font-size="16" font-weight="bold" fill="#0f172a">tgate (tgate_bl)</text>
    <text x="240" y="145" font-size="12" fill="#64748b">Switches BL to GBL</text>
  </g>

  <!-- tgate_blb Instance Box -->
  <g filter="url(#shadow2)">
    <rect x="220" y="350" width="220" height="80" rx="8" fill="#f8fafc" stroke="#0284c7" stroke-width="2.5"/>
    <text x="240" y="385" font-size="16" font-weight="bold" fill="#0f172a">tgate (tgate_blb)</text>
    <text x="240" y="405" font-size="12" fill="#64748b">Switches BLB to GBLB</text>
  </g>

  <!-- Outputs to Global Bitlines -->
  <line x1="440" y1="130" x2="680" y2="130" stroke="#0284c7" stroke-width="3"/>
  <circle cx="680" cy="130" r="6" fill="#0284c7"/>
  <text x="600" y="115" font-size="16" font-weight="bold" fill="#0369a1">GBL (Global BL)</text>

  <line x1="440" y1="390" x2="680" y2="390" stroke="#0284c7" stroke-width="3"/>
  <circle cx="680" cy="390" r="6" fill="#0284c7"/>
  <text x="590" y="375" font-size="16" font-weight="bold" fill="#0369a1">GBLB (Global BLB)</text>

  <!-- Inverter in Middle -->
  <g filter="url(#shadow2)">
    <rect x="240" y="220" width="180" height="70" rx="8" fill="#fdf4ff" stroke="#a855f7" stroke-width="2"/>
    <text x="255" y="250" font-size="15" font-weight="bold" fill="#7e22ce">inverter (inv_sel)</text>
    <text x="255" y="270" font-size="12" fill="#9333ea">Wp=8λ, Wn=4λ</text>
  </g>

  <!-- SEL Input line -->
  <line x1="40" y1="255" x2="240" y2="255" stroke="#059669" stroke-width="3"/>
  <circle cx="40" cy="255" r="6" fill="#059669"/>
  <text x="40" y="240" font-size="16" font-weight="bold" fill="#047857">SEL (Column Select)</text>

  <!-- Branching SEL to tgate_bl and tgate_blb SEL pin -->
  <line x1="160" y1="255" x2="160" y2="160" stroke="#059669" stroke-width="2.5"/>
  <line x1="160" y1="160" x2="220" y2="160" stroke="#059669" stroke-width="2.5"/>
  <circle cx="160" cy="255" r="4" fill="#059669"/>
  <text x="170" y="155" font-size="11" font-weight="bold" fill="#059669">SEL</text>

  <line x1="160" y1="255" x2="160" y2="360" stroke="#059669" stroke-width="2.5"/>
  <line x1="160" y1="360" x2="220" y2="360" stroke="#059669" stroke-width="2.5"/>
  <text x="170" y="375" font-size="11" font-weight="bold" fill="#059669">SEL</text>

  <!-- Inverter output (SEL_B) routing to SEL_B pin of tgates -->
  <line x1="420" y1="255" x2="490" y2="255" stroke="#9333ea" stroke-width="2.5"/>
  <circle cx="490" cy="255" r="4" fill="#9333ea"/>
  <text x="430" y="245" font-size="13" font-weight="bold" fill="#9333ea">SEL_B</text>

  <line x1="490" y1="255" x2="490" y2="160" stroke="#9333ea" stroke-width="2.5"/>
  <line x1="490" y1="160" x2="440" y2="160" stroke="#9333ea" stroke-width="2.5"/>
  <text x="445" y="155" font-size="11" font-weight="bold" fill="#9333ea">SEL_B</text>

  <line x1="490" y1="255" x2="490" y2="360" stroke="#9333ea" stroke-width="2.5"/>
  <line x1="490" y1="360" x2="440" y2="360" stroke="#9333ea" stroke-width="2.5"/>
  <text x="445" y="375" font-size="11" font-weight="bold" fill="#9333ea">SEL_B</text>

  <!-- Power Rails Hint -->
  <text x="40" y="475" font-size="12" fill="#64748b">Global Power Rails: VDD (Power) and GND (Ground) connect to inverter and tgate bulk pins.</text>
</svg>'''

with open("docs/schematic_tgate.svg", "w") as f:
    f.write(svg_tgate)

with open("docs/schematic_col_mux_1x.svg", "w") as f:
    f.write(svg_col_mux)

print("SVG schematics generated successfully!")
