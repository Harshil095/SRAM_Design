# Block 3: Write Driver Design, Sizing & Layout Guide

## 1. Circuit Role & Operating Principle
In an SRAM column, the write driver pulls one bitline strongly to $0\text{ V}$ to overpower the cell's cross-coupled inverters and flip the cell's state.

- **Inputs**:
  - `DIN` (1-bit data input)
  - `WR_EN` (active-high Write Enable)
  - `VDD`, `GND`
- **Outputs**:
  - `BL`, `BLB` (connected to the column bitlines)

### Operation:
1. **Standby / Read Mode (`WR_EN = 0`)**:
   - The NMOS pull-down paths to ground are completely turned OFF (high-impedance / High-Z).
   - The write driver has zero interference with precharge or read operations.
2. **Write-0 Mode (`WR_EN = 1`, `DIN = 0`)**:
   - `DIN_B` is $1.8\text{ V}$.
   - The series NMOS path pulls **`BL` down to $0\text{ V}$** in $< 1\text{ ns}$.
   - `BLB` pull-down is OFF, leaving `BLB` at $1.8\text{ V}$.
3. **Write-1 Mode (`WR_EN = 1`, `DIN = 1`)**:
   - `DIN` is $1.8\text{ V}$.
   - The series NMOS path pulls **`BLB` down to $0\text{ V}$** in $< 1\text{ ns}$.
   - `BL` pull-down is OFF, leaving `BL` at $1.8\text{ V}$.

---

## 2. Transistor Sizing Plan ($\lambda = 0.09\ \mu\text{m}$, TSMC 0.18 $\mu\text{m}$)

All lengths $L = 2\lambda = 0.18\ \mu\text{m}$.

| Transistor | Function | Type | Width ($W$) | Lambda ($W / L$) | Rationale |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **$M_{PINV}$** | Inverter Pull-Up (for $DIN \rightarrow DIN\_B$) | PMOS | **$0.72\ \mu\text{m}$** | $8\lambda / 2\lambda$ | Matches standard inverter cell ($2:1$ ratio). |
| **$M_{NINV}$** | Inverter Pull-Down (for $DIN \rightarrow DIN\_B$) | NMOS | **$0.36\ \mu\text{m}$** | $4\lambda / 2\lambda$ | Matches standard inverter cell. |
| **$M_{WREN1}, M_{WRD1}$** | BL Pull-Down Series Stack | NMOS | **$1.08\ \mu\text{m}$** | $12\lambda / 2\lambda$ | $2\times$ series NMOS: equivalent drive is $0.54\ \mu\text{m}$, discharging $200\text{ fF}$ in $0.97\text{ ns}$. |
| **$M_{WREN2}, M_{WRD2}$** | BLB Pull-Down Series Stack | NMOS | **$1.08\ \mu\text{m}$** | $12\lambda / 2\lambda$ | Symmetrical to BL stack, discharging $200\text{ fF}$ in $0.94\text{ ns}$. |

---

## 3. Step-by-Step Instructions in Electric GUI

### Step 1: Schematic (`write_driver{sch}`)
1. Create new cell: `write_driver{sch}`.
2. Place **1 Inverter** (`inverter{ic}` or instantiate 1 PMOS $8\lambda$ + 1 NMOS $4\lambda$):
   - Input: **`DIN`**
   - Output: **`DIN_B`**
3. Place **4 NMOS Transistors** ($W = 12\lambda$, $L = 2\lambda$):
   - **BL Path**:
     - NMOS 1: Drain $\rightarrow$ `BL`, Gate $\rightarrow$ `WR_EN`, Source $\rightarrow$ intermediate node.
     - NMOS 2: Drain $\rightarrow$ intermediate node, Gate $\rightarrow$ `DIN_B`, Source $\rightarrow$ `GND`.
   - **BLB Path**:
     - NMOS 3: Drain $\rightarrow$ `BLB`, Gate $\rightarrow$ `WR_EN`, Source $\rightarrow$ intermediate node.
     - NMOS 4: Drain $\rightarrow$ intermediate node, Gate $\rightarrow$ `DIN`, Source $\rightarrow$ `GND`.
4. Create Exports (`Ctrl+E`):
   - **`BL`** (Bidirectional)
   - **`BLB`** (Bidirectional)
   - **`DIN`** (Input)
   - **`WR_EN`** (Input)
   - **`VDD`** (Power)
   - **`GND`** (Ground)

### Step 2: Icon (`write_driver{ic}`)
- Create icon view:
  - Top: `BL`, `BLB` (aligns with bottom of bitcell column or column mux).
  - Bottom/Left: `DIN`, `WR_EN`.
  - Power/Ground: `VDD`, `GND`.

### Step 3: Layout (`write_driver{lay}`)
- Draw in `mocmos`:
  - 1 PMOS in N-Well for inverter ($W = 8\lambda$).
  - 1 NMOS for inverter pull-down ($W = 4\lambda$).
  - 4 NMOS in P-substrate for the two pull-down stacks ($W = 12\lambda$).
  - Connect $BL$ and $BLB$ to vertical Metal 2 tracks matching the column bitline pitch.
  - Run DRC (`F5`) and NCC to ensure 0 errors.
