# Automation & CAD Layout Scripts

This directory contains programmatic CAD automation tools, procedural layout generators, DRC error analyzers, and routing rectifiers developed to construct and verify the SRAM macro.

## 📁 Subdirectories

- **`java/`**: Java automation scripts executed inside the **Electric VLSI CAD** engine using Electric's internal Java API (`com.sun.electric.*`).
- **`python/`**: Python utility scripts for geometry parsing, DRC error isolation, and code generation.

---

## ☕ Java Scripts (`scripts/java/`)

The Java tools run within the Electric VLSI batch environment:

### 1. Macro & Chip Builders
- `BuildCompactChip.java`: Programmatically instantiates, places, and connects the pad frame, ESD structures, and SRAM core macro.
- `BuildCompactedMacroClean.java` (and iterative versions `V2`–`V23`): Procedural placers and routers that align the $16\times 16$ array, column mux, write driver, and decoders with pitch-matched orthogonal Manhattan routing on Metal 1, Metal 2, and Metal 3.
- `BuildCoreDatapath.java`: Assembles the core datapath by pitch-matching the precharge bank, memory matrix, and sense amplifier.
- `BuildDecoder4x16.java`: Generates the 4-to-16 predecoded NOR address decoder.
- `BuildDiv8Counter.java`: Generates the modulo-8 synchronous clock counter layout.
- `BuildCleanShiftReg.java`: Assembles the folded 8-bit SIPO shift register layout.

### 2. DRC Analysis & Verification
- `AnalyzeChipDRC.java`: Headless batch runner that invokes Electric's built-in DRC engine and logs all geometric violations with coordinates.
- `DetailedDRCBreakdown.java`: Categorizes DRC errors by rule (spacing, width, enclosure, notch, antenna) and affected layers.
- `VerifyRealGUI_DRC.java`: Simulates full interactive GUI DRC checks to ensure 100% compliance with interactive tool standards.
- `DumpExactDrcErrors.java`: Extracts exact bounding box coordinates of design rule violations for automated script repair.

### 3. Routing & Geometry Rectifiers
- `FixMacroRouting.java`: Cleans crooked arcs, eliminates acute routing angles, and rectifies off-grid via placements.
- `CleanDatapathGnd.java` / `ConnectDatapathGnd.java`: Resolves substrate tap connections and low-impedance ground mesh routing.
- `CleanSpiceCardsAndExport.java`: Sanitizes simulation models and exports production-ready SPICE cards.

---

## 🐍 Python Scripts (`scripts/python/`)

- `BuildCleanV8.py`, `BuildFlawlessMacroV9.py`–`V11.py`: Automated code generators that iteratively refined Java routing algorithms until zero DRC errors were achieved.
- `FixAllMacroRouting.py`: Batch geometry rectifier repairing metal overlap rules.
- `fix_sch.py` / `create_dff_sch.py`: Programmatic schematic generator ensuring 100% topological parity with physical layouts.
- `test_min_size.py`: Automated transistor width and length minimum sizing validator against TSMC 180nm rules.
