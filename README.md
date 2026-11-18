# 256-Bit Full-Custom CMOS SRAM Macro (16×16)

[![Technology](https://img.shields.io/badge/Technology-TSMC%20180nm%20CMOS-blue.svg)](pdk/tsmc018.lib)
[![Supply Voltage](https://img.shields.io/badge/VDD-1.8V-green.svg)]()
[![CAD Tool](https://img.shields.io/badge/CAD-Electric%20VLSI%209.07-orange.svg)](cad/)
[![SPICE Simulation](https://img.shields.io/badge/Simulation-ngspice--47-purple.svg)](spice/)
[![DRC Status](https://img.shields.io/badge/DRC-0%20Errors%20(Clean)-brightgreen.svg)](verification/)
[![LVS Status](https://img.shields.io/badge/NCC%20%2F%20LVS-100%25%20Matched-brightgreen.svg)](verification/)

A complete, full-custom **256-bit ($16 \times 16$) Static Random-Access Memory (SRAM) macro** designed, physically laid out, and verified in **TSMC 180nm CMOS** technology ($V_{DD} = 1.8\text{ V}$) using **Electric VLSI** and **ngspice**.

Developed as the Final VLSI Design Project for B.Tech Electrical Engineering at the **Indian Institute of Technology Hyderabad (IITH)**.

---

## 📑 Table of Contents

- [Key Specifications](#-key-specifications)
- [Architecture & Block Diagram](#-architecture--block-diagram)
- [5-Pin External Interface](#-5-pin-external-interface)
- [Subsystem Breakdown](#-subsystem-breakdown)
- [Repository Organization](#-repository-organization)
- [Quick Start Guide](#-quick-start-guide)
  - [Opening in Electric VLSI](#opening-in-electric-vlsi)
  - [Running SPICE Simulations](#running-spice-simulations)
- [Verification & DRC Status](#-verification--drc-status)
- [Documentation & Deliverables](#-documentation--deliverables)

---

## ⚡ Key Specifications

| Parameter | Specification | Verified Value | Notes |
| :--- | :---: | :---: | :--- |
| **Technology Node** | TSMC 180 nm | TSMC 0.18 µm 1P6M | BSIM3v3.1 (`LEVEL=49`) |
| **Supply Voltage ($V_{DD}$)** | 1.8 V | 1.8 V | Room temperature ($27^\circ\text{C}$) |
| **Grid Unit ($\lambda$)** | 0.09 µm | $L_{min} = 2\lambda = 0.18\,\mu\text{m}$ | MOSIS scalable CMOS (`mocmos`) |
| **Array Architecture** | $16 \times 16$ | 256 bits | 16 rows $\times$ 16 columns |
| **Operating Frequency** | 100 MHz (nominal) | **133 MHz ($F_{max}$)** | Single-phase synchronous clock |
| **Read Access Delay ($t_{prop}$)**| $< 10\text{ ns}$ | **6.256 ns** | From `CLK` rising edge to `DOUT` |
| **Read Energy per Bit** | — | **29.93 pJ/bit** | Full cycle active dynamic energy |
| **Write Energy per Bit** | — | **20.34 pJ/bit** | Full rail BL discharge + core latch |
| **Static Noise Margin (SNM)** | $> 200\text{ mV}$ | **286 mV (Hold) / 194 mV (Read)**| Butterfly curves simulated in SPICE |
| **Physical Layout Area** | Compact | $35.72 \times 10^6\,\lambda^2$ | Zero DRC errors, 100% matched NCC |
| **External Package Pins** | Bounded | **5 Pins** | `CLK`, `DIN`, `DOUT`, `ADDR_IN`, `R_W` |

---

## 🏛 Architecture & Block Diagram

```
                              5-PIN EXTERNAL INTERFACE
                   [CLK]    [ADDR_IN]    [DIN]    [R_W]    [DOUT]
                     |          |          |        |        ^
                     v          v          |        |        |
             +---------------+             |        |        |
             | 8-Bit Serial  |             |        |        |
             | Shift Register|             |        |        |
             |   + Div-8     |             |        |        |
             +---------------+             |        |        |
               | (4b)     | (4b)           |        |        |
               v          v                |        |        |
          +---------+ +---------+          |        |        |
          | Row Dec | | Col Dec |          |        |        |
          | (4-to-16| | (4-to-16|          |        |        |
          +---------+ +---------+          |        |        |
               | WL[15:0] | COL_SEL[15:0]  |        |        |
               |          |                |        |        |
               v          |                |        |        |
    +-------------------+ |                |        |        |
    |                   | |                |        |        |
    |   16 x 16 CORE    | |                |        |        |
    |   SRAM ARRAY      | |                |        |        |
    |   (256 Cells)     | |                |        |        |
    |                   | |                |        |        |
    +-------------------+ |                |        |        |
         | BL / BLB       |                |        |        |
         v                v                |        |        |
    +----------------------------------+   |        |        |
    |   Precharge & Equalizer Bank     |   |        |        |
    +----------------------------------+   |        |        |
    |   Write Driver Bank (16x)        |<--+--------+        |
    +----------------------------------+                     |
    |   16:1 Transmission Gate Col Mux |<--+                 |
    +----------------------------------+                     |
    |   Latch Sense Amplifier + Strobe |---------------------+
    +----------------------------------+
```

---

## 🔌 5-Pin External Interface

To model realistic pin-limited SoC/embedded packages, the macro operates with only **5 external interface pins**:

1. **`CLK` (Clock Input)**: Synchronous master clock (100–133 MHz) driving the Div-8 serial counter, precharge self-timing, address latches, and output strobes.
2. **`ADDR_IN` (Serial Address Input)**: 8-bit serial address stream shifted into an internal SIPO shift register over 8 clock cycles (Bits `A[7:4]` = Row Address, Bits `A[3:0]` = Column Address).
3. **`DIN` (Data Input)**: Single-bit serial data line sampled during write transactions ($R\_W = 0$).
4. **`R_W` (Read / Write Select)**: Active-high read ($R\_W = 1$), active-low write ($R\_W = 0$).
5. **`DOUT` (Data Output)**: Low-impedance buffered digital output delivering the accessed bit.

---

## 🧩 Subsystem Breakdown

1. **6T SRAM Bitcell (`cad/SRAM_final.jelib:6T{lay}`)**:
   - High cell ratio ($CR = (W/L)_{PD} / (W/L)_{PG} = 1.67$) to guarantee non-destructive read stability.
   - Low pull-up ratio ($PR = (W/L)_{PU} / (W/L)_{PG} = 0.60$) ensuring rapid write completion.
   - Symmetric pitch-matched layout with internal poly straps and Metal 1/2 routing.

2. **Precharge & Equalizer Bank (`cad/SRAM_final.jelib:Precharge_Bank_16{lay}`)**:
   - Triple PMOS topology (2 pull-ups to $V_{DD}$ + 1 equalization bridge).
   - Equalizes bitlines within **1.8 ns** into a 200 fF capacitive load.

3. **Differential Write Driver (`cad/SRAM_final.jelib:write_driver{lay}`)**:
   - Complementary pull-down NMOS network driving either $BL$ or $\overline{BL}$ to ground within **1.2 ns**.

4. **Voltage Latch Sense Amplifier (`cad/SRAM_final.jelib:sense_amplifier{lay}`)**:
   - Cross-coupled inverter latch with NMOS foot transistor activated by self-timed sense strobe.
   - Resolves a differential bitline voltage $\Delta V_{BL} \ge 100\text{ mV}$ in **350 ps**.

5. **16:1 Column Multiplexer (`cad/SRAM_final.jelib:Col_Mux_Bank_16{lay}`)**:
   - Balanced CMOS transmission gates (`col_mux_1x`) with decoded select lines connecting the target column to the shared sense amplifier.

6. **4-to-16 Row & Column Decoders (`cad/SRAM_final.jelib:Decoder_4x16{lay}`)**:
   - NOR-based predecoded architecture with clocked enable gating for low standby power.

7. **Serial Addressing & Timing Controller (`cad/SRAM_final.jelib:ShiftReg_8bit{lay}`, `Div8_Counter{lay}`)**:
   - 8-stage D-Flip-Flop folded shift register paired with a modulo-8 synchronous counter generating wordline enables and latch strobes every 8 clock pulses.

---

## 📁 Repository Organization

```
SRAM_Design/
├── README.md                      # Primary project overview and technical documentation
├── .gitignore                     # Git ignore rules for CAD and SPICE artifacts
│
├── cad/                           # Electric VLSI CAD Databases & Configurations
│   ├── SRAM_final.jelib           # Golden, fully verified DRC/NCC-clean library
│   ├── SRAM_Complete_V1.jelib     # Complete baseline reference library
│   ├── SRAM.jelib                 # Active development database
│   ├── SRAM_work.jelib            # Sandbox working copy
│   ├── projsettings.xml           # Electric VLSI user and display preferences
│   └── backups/                   # Version history and intermediate phase backups
│
├── docs/                          # Comprehensive Technical Documentation
│   ├── report/                    # Formal technical project report
│   │   ├── SRAM_Report.pdf        # Publication-ready final report (PDF)
│   │   ├── SRAM_Report.tex        # LaTeX source document
│   │   ├── SRAM.pdf               # Project assignment specification
│   │   └── generate_report.py     # LaTeX build automation script
│   ├── design_guides/             # Step-by-step block sizing & layout guides (.md)
│   │   ├── 6t_layout_guide.md     # 6T layout and design rule walkthrough
│   │   ├── sram_6t_sizing.md      # Analytical transistor sizing & SNM equations
│   │   ├── transistor_sizing_master_plan.md # Master W/L sizing table
│   │   ├── block1_6t_cell_report.md
│   │   ├── block2_precharge_guide.md
│   │   ├── block3_write_driver_guide.md
│   │   ├── block4_sense_amp_guide.md
│   │   ├── block5_col_mux_guide.md
│   │   └── progress.md            # Design checklist and milestone log
│   └── schematics/                # Schematic SVGs, PNGs, and rendering scripts
│       ├── schematic_SRAM_256_Macro.svg
│       ├── schematic_dff.svg
│       ├── schematic_div8_counter.svg
│       ├── schematic_col_mux_1x.svg
│       └── schematic_tgate.svg
│
├── pdk/                           # Process Design Kit & Tech Libraries
│   └── tsmc018.lib                # TSMC 180nm BSIM3v3.1 SPICE models (Level 49)
│
├── spice/                         # SPICE Simulation Decks, Netlists & Results
│   ├── netlists/                  # Extracted SPICE netlists (.spi)
│   │   ├── 6T.spi                 # 6T cell subcircuit
│   │   ├── sense_amplifier.spi    # Sense amplifier netlist
│   │   ├── write_driver.spi       # Write driver netlist
│   │   ├── Decoder_4x16.spi       # 4-to-16 decoder
│   │   ├── SRAM_Array_16x16.spi   # 16x16 core bitcell array
│   │   ├── SRAM_Core_Datapath.spi # Datapath integration
│   │   ├── SRAM_256_Macro.spi     # Complete 256-bit macro netlist
│   │   └── SRAM_Chip_Final.spi    # Full-chip top-level netlist with pads
│   ├── testbenches/               # Verification simulation decks (.sp & .cir)
│   │   ├── tb_SRAM_Chip.sp        # Full chip read/write transaction testbench
│   │   ├── tb_counter.sp          # Modulo-8 counter verification
│   │   ├── tb_snm.cir             # Static noise margin (butterfly curve) analysis
│   │   ├── tb_col_mux.cir         # Column mux delay characterization
│   │   └── tb_sense_amp.cir       # Sense amp sensitivity & strobe timing
│   ├── results/                   # Simulation outputs, waveforms, and measurement logs
│   └── scripts/                   # Python simulation analysis and parameter sweeps
│
├── scripts/                       # Procedural Layout Automation & CAD Tooling
│   ├── java/                      # Java scripts for Electric VLSI CAD system
│   │   ├── BuildCompactChip.java  # Procedural full-chip assembler
│   │   ├── BuildCompactedMacroClean.java # Macro router and layout generator
│   │   ├── AnalyzeChipDRC.java    # Automated batch DRC analyzer
│   │   └── DetailedDRCBreakdown.java # Error classifier and coordinate locator
│   └── python/                    # Python layout geometry and routing fixers
│       ├── BuildCleanV8.py        # Automated routing and via generation
│       └── fix_sch.py             # Schematic connectivity rectifier
│
└── verification/                  # Verification Reports, DRC Breakdowns & Audits
    ├── drc_breakdown.txt          # Final DRC violation audit log (0 errors)
    ├── all_macro_errors.txt       # Macro routing verification log
    └── exact_fixes.txt            # Coordinate repair mapping table
```

---

## 🚀 Quick Start Guide

### Opening in Electric VLSI

1. Launch **Electric VLSI** (version 9.07 or later):
   ```bash
   java -jar electric-9.07.jar
   ```
2. Open the golden design library:
   - File $\rightarrow$ Open Library $\rightarrow$ select `cad/SRAM_final.jelib`
3. Browse the top-level cells in the cell explorer:
   - `SRAM_Chip_Final{lay}`: Complete pad-framed chip layout
   - `SRAM_256_Macro{lay}`: 256-bit core macro layout
   - `SRAM_Core_Datapath{lay}`: Integrated array + decoders + precharge + write driver + sense amp
   - `6T{lay}`: Pitch-matched 6T SRAM bitcell

### Running SPICE Simulations

Simulations are configured for **ngspice** using the TSMC 180nm models:

```bash
# Navigate to testbenches directory
cd spice/testbenches

# Run top-level SRAM chip functional simulation
ngspice -b tb_SRAM_Chip.sp -r tb_SRAM_Chip.raw

# Run 6T Bitcell Static Noise Margin (SNM) characterization
ngspice -b tb_snm.cir

# Run Modulo-8 clock divider testbench
ngspice -b tb_counter.sp
```

---

## ✅ Verification & DRC Status

The physical design was subjected to strict **Design Rule Checking (DRC)** and **Network Consistency Checking (NCC / LVS)** using the MOSIS Scalable CMOS (`mocmos`) rule set:

```
============================================================
              FINAL VERIFICATION AUDIT SUMMARY
============================================================
Target Block:        SRAM_Chip_Final{lay} (Full Integrated Chip)
Technology:          TSMC 0.18 µm CMOS (mocmos, lambda = 0.09 µm)
Design Rules:        MOSIS CMOS SCMOS (Submicron)
------------------------------------------------------------
DRC Violations:      0 ERRORS (Clean)
NCC / LVS Match:     100% Topologically Equivalent (0 Mismatches)
Well Spacing:        Compliant (Min 12 lambda N-well/P-substrate)
Metal 1-3 Density:   Verified (Orthogonal Manhattan Routing)
Antenna Rules:       Compliant with internal diode/poly protection
============================================================
```

Detailed error audits and historical debugging logs are available in [`verification/`](verification/).

---

## 📚 Documentation & Deliverables

- 📄 **Comprehensive Technical Report**: [`docs/report/SRAM_Report.pdf`](docs/report/SRAM_Report.pdf) — Complete 30+ page formal engineering report with analytical sizing, butterfly curves, transient waveform plots, and layout geometry breakdown.
- 📐 **Block Design Guides**: Detailed design and transistor sizing rationale in [`docs/design_guides/`](docs/design_guides/).
- 🖼 **Vector Schematics**: High-resolution circuit schematics in [`docs/schematics/`](docs/schematics/).

---

## 👤 Author

- **Harshil Rathan**  
  Department of Electrical Engineering  
  Indian Institute of Technology Hyderabad (IITH)  
  GitHub: [@Harshil095](https://github.com/Harshil095)
