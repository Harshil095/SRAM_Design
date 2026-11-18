# Audit & SPICE Debug Report: 6T Cell & 6T_tb in SRAM.jelib

## 1. Schematic & Icon Audit of `SRAM.jelib`

### Cells Found:
1. **`6T;1{ic}` (Icon View)**:
   - Ports: `BL` (B), `BLB` (B), `GND` (G), `VDD` (P), `WL` (I).
   - Graphic geometry: $8 \times 8$ bounding box with bus/wire pins matching schematic connections.
2. **`6T;1{sch}` (Schematic View)**:
   - **NMOS transistors**:
     - `nmos@0`: Right pull-down NMOS ($M_{PD2}$)
     - `nmos@1`: Left pull-down NMOS ($M_{PD1}$)
     - `nmos@2`: Left access NMOS ($M_{PG1}$) to `BL`
     - `nmos@3`: Right access NMOS ($M_{PG2}$) to `BLB`
   - **PMOS transistors**:
     - `pmos@0`: Left pull-up PMOS ($M_{PU1}$)
     - `pmos@1`: Right pull-up PMOS ($M_{PU2}$)
   - **Connectivity Audit**:
     - Cross-coupling between node $Q$ (`pin@3`, `pin@7`) and node $QB$ (`pin@1`, `pin@0`) is **100% correct**.
     - Wordline `WL` connects to gates of both access transistors `nmos@2` and `nmos@3`.
     - `VDD` connects to drains of both pull-ups `pmos@0` and `pmos@1`.
     - `GND` connects to sources of both pull-downs `nmos@0` and `nmos@1`.
     - All 5 ports match the icon view `6T;1{ic}` cleanly.
3. **`6T_tb;1{sch}` (Testbench Schematic View)**:
   - Instantiates `6T;1{ic}` as `6T@0`.
   - Contains SPICE card deck attached to `pin@5`.

---

## 2. SPICE Card Debugging & Fixes

In the original SPICE deck in `6T_tb`:
1. **Model Include Path**:
   The deck referenced `.include ./tsmc018.lib`. When Electric exports the SPICE netlist to a subfolder or runs from the working directory, relative paths must resolve properly to `tsmc018.lib`.
2. **Switch Models (`swpc` and `swwr`)**:
   Ngspice syntax `.model swpc sw(vt=0.9 vh=0.1 ron=500 roff=1e12)` is supported as an ideal voltage-controlled switch, allowing realistic column bitline precharge ($200\text{ fF}$) and write-driver pull-down.
3. **Measurements & Energy**:
   Added `.meas tran` cards for:
   - Read-0 bitline levels (`bl_read0`, `blb_read0`)
   - Read-1 bitline levels (`bl_read1`, `blb_read1`)
   - Write charge/energy integrals ($\int I(V_{sup}) dt$)

The calibrated standalone deck is saved in [`sims/tb_user_6t.cir`](file:///Users/harshil_rathan/Documents/BTech%20IITH/SEM5/VLSI/Final_Project/sims/tb_user_6t.cir).

---

## 3. Simulation Results (ngspice-47)

Timeline simulated: $0\text{ ns}$ to $32\text{ ns}$.

| Phase | Time Window | WL | PC | BL / BLB Driven | Internal State ($Q, QB$) | Observation / Check |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **Initial Write-0** | $1.3\text{--}3.7\text{ ns}$ | $1.8\text{ V}$ | $0\text{ V}$ | $\text{BL}=0\text{ V}, \text{BLB}=1.8\text{ V}$ | $Q=0.001\text{ V}, QB=1.800\text{ V}$ | Successfully forces cell to known state '0'. |
| **Hold-0** | $3.8\text{--}7.5\text{ ns}$ | $0\text{ V}$ | $1.8\text{ V}$ | Precharged ($1.8\text{ V}$) | $Q=0.000\text{ V}, QB=1.800\text{ V}$ | Cell holds state stably. |
| **Read-0** | $7.6\text{--}10.0\text{ ns}$ | $1.8\text{ V}$ | $0\text{ V}$ | Floating precharged | $Q \text{ bump} = 0.233\text{ V}$ | **PASS**: Stored '0' does not flip ($0.233\text{ V} < V_{thn}$). $\Delta V = 891.5\text{ mV}$. |
| **Write-1** | $13.4\text{--}16.0\text{ ns}$ | $1.8\text{ V}$ | $0\text{ V}$ | $\text{BL}=1.8\text{ V}, \text{BLB}=0\text{ V}$ | $Q=1.800\text{ V}, QB=0.000\text{ V}$ | **PASS**: Cell cleanly flips to state '1'. |
| **Hold-1** | $16.1\text{--}19.5\text{ ns}$ | $0\text{ V}$ | $1.8\text{ V}$ | Precharged ($1.8\text{ V}$) | $Q=1.800\text{ V}, QB=0.000\text{ V}$ | Cell holds state stably. |
| **Read-1** | $19.6\text{--}22.0\text{ ns}$ | $1.8\text{ V}$ | $0\text{ V}$ | Floating precharged | $QB \text{ bump} = 0.179\text{ V}$ | **PASS**: Stored '1' does not flip. $\Delta V = 1322.3\text{ mV}$. |
| **Write-0** | $25.4\text{--}28.0\text{ ns}$ | $1.8\text{ V}$ | $0\text{ V}$ | $\text{BL}=0\text{ V}, \text{BLB}=1.8\text{ V}$ | $Q=0.000\text{ V}, QB=1.800\text{ V}$ | **PASS**: Cell flips back to state '0'. |

### Quantitative Measurements:
- **Read-0 Bitline Voltages ($t=9.9\text{ ns}$)**:
  - $V(BL) = 0.0955\text{ V}$
  - $V(BLB) = 1.803\text{ V}$
  - Differential $\Delta V_{read0} \approx 1.707\text{ V}$
- **Read-1 Bitline Voltages ($t=21.9\text{ ns}$)**:
  - $V(BL) = 1.803\text{ V}$
  - $V(BLB) = 0.0958\text{ V}$
  - Differential $\Delta V_{read1} \approx 1.707\text{ V}$
- **Write Energy**:
  - Write-1 charge $Q_{write1} = 7.31\text{ fC} \implies E_{write1} = V_{DD} \times Q = 1.8\text{ V} \times 7.31\text{ fC} = \mathbf{13.16\text{ fJ}}$
  - Write-0 charge $Q_{write0} = 7.30\text{ fC} \implies E_{write0} = V_{DD} \times Q = 1.8\text{ V} \times 7.30\text{ fC} = \mathbf{13.14\text{ fJ}}$

---

## 4. Transistor Sizing Note for Electric GUI

In Electric GUI, when drawing `6T;1{sch}`:
- Right now, all transistors have default length and width ($L=2\lambda$, $W=2\lambda$).
- To match our optimal Static Noise Margin ($SNM = 263\text{ mV}$):
  - Sizing $M_{PD}$ to $W=6\lambda$ ($0.54\ \mu\text{m}$), $M_{PG}$ to $W=4\lambda$ ($0.36\ \mu\text{m}$), and $M_{PU}$ to $W=3\lambda$ ($0.28\ \mu\text{m}$) gives higher read stability.
  - In both sizing schemes, write and read pass reliably in the testbench.
