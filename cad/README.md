# Electric VLSI CAD Databases

This directory contains the Electric VLSI (.jelib) libraries, design databases, and project preferences for the 256-bit SRAM macro.

## 🗂 Library Files

| File | Status | Description |
| :--- | :---: | :--- |
| **`SRAM_final.jelib`** | **Golden** | Final, verified library containing all DRC-clean and LVS-verified cells, macros, and top-level chip layouts. |
| **`SRAM_Complete_V1.jelib`** | Baseline | Full integrated baseline library before final routing optimizations. |
| **`SRAM.jelib`** | Development | Active development database. |
| **`SRAM_work.jelib`** | Working Copy | Sandbox copy for rapid experimentation. |
| **`projsettings.xml`** | Config | Electric VLSI environment, technology scaling, and display settings. |

## 🏗 Key Cell Hierarchy (`SRAM_final.jelib`)

- **Top-Level Chip**:
  - `SRAM_Chip_Final{lay}`: Complete chip layout including pad frame, ESD protection, and control wiring.
  - `SRAM_Chip_Final{sch}`: Full chip hierarchical schematic.
- **256-Bit Macro**:
  - `SRAM_256_Macro{lay}`: Core SRAM macro ($16\times 16$ array, decoders, sense amp, write driver, precharge, and control logic).
- **Subsystems**:
  - `SRAM_Core_Datapath{lay}`: Bitcell array directly abutted to write driver, precharge, and column mux.
  - `SRAM_Array_16x16{lay}`: $16 \times 16$ array of pitch-matched 6T bitcells.
  - `Decoder_4x16{lay}`: 4-to-16 NOR decoder with clocked enable.
  - `ShiftReg_8bit{lay}`: 8-stage serial-in parallel-out address shift register.
  - `Div8_Counter{lay}`: Modulo-8 synchronous clock divider and strobe generator.
- **Leaf Cells**:
  - `6T{lay}` / `6T{sch}`: 6T SRAM bitcell ($CR = 1.67, PR = 0.60$).
  - `pre_charge{lay}` / `pre_charge{sch}`: Bitline precharge and equalization circuit.
  - `write_driver{lay}` / `write_driver{sch}`: Differential write driver.
  - `sense_amplifier{lay}` / `sense_amplifier{sch}`: Latch-type voltage sense amplifier.
  - `col_mux_1x{lay}` / `col_mux_1x{sch}`: CMOS transmission gate column multiplexer.
  - `DFF{lay}` / `DFF{sch}`: True Single-Phase Clock / Master-Slave D-Flip-Flop.

## 📦 Backups & Milestones

The `backups/` subdirectory contains timestamped checkpoints and incremental phase backups saved throughout the physical design and verification phases.
