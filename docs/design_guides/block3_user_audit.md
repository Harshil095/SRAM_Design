# Block 3: Write Driver Audit Report (`write_driver{sch}` & `write_driver{ic}`)

## 1. Schematic & Icon Audit of `write_driver`

### Cells Audited:
1. **`write_driver;1{sch}` (Schematic View)**:
   - **Data Inverter**:
     - `pmos@0`: Gate connects to `WD`, drain connects to `VDD`, source connects to internal node `WD_b` (`pin@0`).
     - `nmos@4`: Gate connects to `WD`, drain connects to `WD_b` (`pin@0`), source connects to `GND`.
   - **BL Pull-Down Stack**:
     - `nmos@0`: Drain connects to `BL`, gate connects to `WE`, source connects to `net@0`.
     - `nmos@3`: Drain connects to `net@0`, gate connects to `WD`, source connects to `GND`.
   - **BLB (BL_b) Pull-Down Stack**:
     - `nmos@1`: Drain connects to `BL_b`, gate connects to `WE`, source connects to `net@1`.
     - `nmos@2`: Drain connects to `net@1`, gate connects to `WD_b`, source connects to `GND`.
   - **Power and Ground**:
     - `pwr@0` tied to `pmos@0` drain. Exported as `Evdd`.
     - `gnd@0` tied to NMOS sources. Exported as `Egnd`.
   - **Exports**: `BL`, `BL_b`, `WD`, `WE`, `VDD`, `GND`.

2. **`write_driver;1{ic}` (Icon View)**:
   - Exports: `BL`, `BL_b` (alias `BLB`), `WD`, `WE`, `VDD`, `GND`.
   - All 6 ports match between schematic and icon.

---

## 2. Key Audit Findings & Minor Adjustments

### A. Transistor Sizing (Schematic has default $W=2, L=2$):
In `write_driver{sch}`, all 6 transistors currently have the default text **`2`** ($W = 2\lambda = 0.18\ \mu\text{m}$):
- For the series NMOS stacks (`nmos@0, nmos@3` and `nmos@1, nmos@2`), two $2\lambda$ NMOS in series give an equivalent strength of only $1\lambda$ ($0.09\ \mu\text{m}$).
- To pull down the $200\text{ fF}$ bitline column rapidly ($< 1\text{ ns}$):
  - Set the 4 series NMOS (`nmos@0`, `nmos@1`, `nmos@2`, `nmos@3`) to **Width = 12** ($12\lambda = 1.08\ \mu\text{m}$).
  - Inverter PMOS (`pmos@0`): set **Width = 8** ($8\lambda = 0.72\ \mu\text{m}$).
  - Inverter NMOS (`nmos@4`): set **Width = 4** ($4\lambda = 0.36\ \mu\text{m}$).

### B. Data Polarity Note (For Your Live Demo):
- When `WD = 1` and `WE = 1`: `nmos@3` is ON $\rightarrow$ **`BL` is pulled to $0\text{ V}$**.
- When `WD = 0` and `WE = 1`: `nmos@2` is ON $\rightarrow$ **`BL_b` is pulled to $0\text{ V}$**.
*(In our 6T cell, pulling `BL` to $0\text{ V}$ writes a '0' to $Q$, and pulling `BL_b` to $0\text{ V}$ writes a '1' to $Q$. So `WD = 1` writes '0', and `WD = 0` writes '1'. If you prefer `WD = 1` to write '1', simply swap the gate connections of `nmos@3` and `nmos@2`!)*

### C. SPICE Card Fix:
The SPICE card on `pin@18` was missing `.include ./tsmc018.lib`, had an extra `.end`, and had no pull-up / precharge on the bitlines.
A clean calibrated SPICE deck has been verified and provided.
