# SPICE Netlists, Simulations & Results

This directory contains extracted SPICE netlists, characterization testbenches, simulation results, and analysis scripts for the SRAM project.

## 📁 Directory Structure

```
spice/
├── netlists/         # Extracted transistor-level SPICE netlists (.spi)
├── testbenches/      # ngspice simulation testbenches (.sp, .cir)
├── results/          # Simulation outputs, waveforms, and measurement logs (.out, .txt)
└── scripts/          # Python automation scripts for parameter sweeps and data extraction
```

## 🔌 Netlists (`netlists/`)

Transistor netlists extracted from Electric VLSI with BSIM3v3.1 models (`pdk/tsmc018.lib`):

- **Full Chip & Macros**:
  - `SRAM_Chip_Final.spi` / `SRAM_Chip_Final_clean.spi`: Pad-framed full chip netlist.
  - `SRAM_256_Macro.spi` / `SRAM_256_Macro_clean.spi`: 256-bit macro netlist.
  - `SRAM_Core_Datapath.spi`: Integrated datapath netlist.
  - `SRAM_Array_16x16.spi`: 256-bit bitcell matrix.
- **Decoders & Addressing**:
  - `Decoder_4x16.spi`: Row/Column address decoder.
  - `ShiftReg_8bit.spi`: 8-bit serial address shift register.
  - `Div8_Counter.spi`: Modulo-8 clock divider.
- **Leaf Cells**:
  - `6T.spi`: 6T bitcell.
  - `pre_charge.spi`: Precharge and equalization subcircuit.
  - `write_driver.spi`: Write driver circuit.
  - `sense_amplifier.spi`: Voltage latch sense amplifier.
  - `col_mux_1x.spi`: Column transmission gate multiplexer.
  - `DFF.spi`: D-Flip-Flop subcircuit.

## 🧪 Testbenches (`testbenches/`)

- **Chip-Level Tests (`.sp`)**:
  - `tb_SRAM_Chip.sp`: Full-chip read and write transaction verification under realistic 5-pin timing.
  - `tb_counter.sp`: Verification of modulo-8 counter cycle and strobe pulse generation.
  - `tb_rw82.sp`: Sequential 8-write, 8-read regression testbench.
  - `tb_debug_80ns.sp`: High-resolution timing inspection around the first read transaction.
- **Block-Level Characterization (`.cir`)**:
  - `tb_snm.cir`: Static Noise Margin (SNM) butterfly curve simulation during hold state.
  - `tb_hold_snm.cir`: Retention SNM sweep across supply variations.
  - `tb_precharge.cir`: Precharge rise time characterization into 200 fF bitline capacitance.
  - `tb_write_driver.cir`: Write trip point and bitline discharge delay.
  - `tb_sense_amp.cir`: Differential sense amplifier latching speed and strobe delay.
  - `tb_col_mux.cir`: Column multiplexer insertion loss and transmission delay.
  - `tb_row_decoder.cir`: 4-to-16 decoding propagation delay.

## 🚀 Running Simulations

Ensure `ngspice` is installed:

```bash
cd spice/testbenches

# Full-chip functional test
ngspice -b tb_SRAM_Chip.sp -r ../results/tb_SRAM_Chip.raw

# 6T SNM butterfly curve
ngspice -b tb_snm.cir

# Modulo-8 counter
ngspice -b tb_counter.sp
```
