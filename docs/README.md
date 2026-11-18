# Documentation & Design Reports

This directory houses the formal engineering documentation, technical reports, block-by-block design walkthroughs, and high-resolution circuit schematics for the 256-bit SRAM macro.

## 📁 Subdirectories

```
docs/
├── report/           # Formal technical report (PDF & LaTeX source)
├── design_guides/    # Block-level design, sizing, and layout walkthroughs (.md)
└── schematics/       # Circuit schematics and layout floorplans (.svg, .png)
```

---

## 📑 Technical Report (`docs/report/`)

- **`SRAM_Report.pdf`**: The complete, publication-ready formal engineering final report (30+ pages). Includes:
  - Theoretical transistor sizing derivations (Cell Ratio, Pull-up Ratio, static noise margin formulas).
  - Analytical and simulation-derived butterfly curves.
  - High-speed timing diagrams and critical path latency breakdowns.
  - Power dissipation budgets (active dynamic power vs. sub-threshold leakage).
  - Physical layout floorplan, metal stack usage (M1–M3), and pad frame integration.
  - DRC audit results and LVS/NCC verification evidence.
- **`SRAM_Report.tex`**: Complete LaTeX source with custom formatting, tables, and color palettes.
- **`SRAM.pdf`**: Original project assignment specifications and requirements sheet.
- **`generate_report.py`** / **`build_clean_report.py`**: Python scripts to build the LaTeX document automatically.

---

## 📐 Design Guides (`docs/design_guides/`)

Step-by-step technical guides covering each subsystem:

| Document | Content |
| :--- | :--- |
| **`6t_layout_guide.md`** | Physical design rules, cell symmetry, and pitch matching for the 6T SRAM bitcell. |
| **`sram_6t_sizing.md`** | Analytical equations for read stability ($CR \ge 1.5$) and write margin ($PR \le 0.7$). |
| **`transistor_sizing_master_plan.md`** | Master reference table of $W/L$ ratios for all transistors in the chip. |
| **`block1_6t_cell_report.md`** | Characterization report for the 6T bitcell (hold/read SNM, access delay). |
| **`block2_precharge_guide.md`** | Design and sizing of the bitline precharge and equalization network. |
| **`block3_write_driver_guide.md`** | Sizing criteria for single-ended bitline pull-down during write operations. |
| **`block4_sense_amp_guide.md`** | Differential voltage latch-type sense amplifier design, sensitivity, and strobe timing. |
| **`block5_col_mux_guide.md`** | 16:1 column multiplexer design using complementary CMOS transmission gates. |
| **`progress.md`** | Milestone tracker and design verification checklist. |

---

## 🖼 Schematics (`docs/schematics/`)

Vector SVG and rendered PNG circuit diagrams:
- `schematic_SRAM_256_Macro.svg`: 256-bit SRAM Macro schematic.
- `schematic_dff.svg`: D-Flip-Flop schematic.
- `schematic_div8_counter.svg`: Modulo-8 synchronous counter schematic.
- `schematic_col_mux_1x.svg` / `.png`: 1-bit column multiplexer cell.
- `schematic_tgate.svg` / `.png`: CMOS transmission gate circuit.
