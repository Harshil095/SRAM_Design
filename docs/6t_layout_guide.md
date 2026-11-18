# 6T SRAM Cell Layout Floorplan & DRC Guide (Electric VLSI 0.18 µm mocmos)

## 1. Cell Sizing & Grid Summary ($\lambda = 0.09\ \mu\text{m}$)

| Transistor | Width ($W$) | Length ($L$) | Lambda Dimensions ($W \times L$) |
| :--- | :---: | :---: | :---: |
| **Pull-Down NMOS ($M_{PD1}, M_{PD2}$)** | $0.54\ \mu\text{m}$ | $0.18\ \mu\text{m}$ | **$6\lambda \times 2\lambda$** |
| **Access NMOS ($M_{PG1}, M_{PG2}$)** | $0.36\ \mu\text{m}$ | $0.18\ \mu\text{m}$ | **$4\lambda \times 2\lambda$** |
| **Pull-Up PMOS ($M_{PU1}, M_{PU2}$)** | $0.28\ \mu\text{m}$ | $0.18\ \mu\text{m}$ | **$3\lambda \times 2\lambda$** |

---

## 2. Standard Thin-Cell Floorplan Architecture

To achieve optimal area and clean 2D array tiling (where adjacent cells share $V_{DD}$, $GND$, $BL$, $BLB$, and $N\text{-Well}$ contacts by simple mirroring), standard industrial 6T SRAM layout uses a **symmetric split-NMOS / central-PMOS** or **two-inverter column** topology:

```
        Y
        ^
        |   +-----------------------------------------------------------+
        |   |                 VDD Line (Metal 1 / Metal 2)               |
        |   +-----------------------------------------------------------+
        |   |   [MPU1: 3x2] (P-Active)    |    [MPU2: 3x2] (P-Active)   |  <-- N-Well Region
        |   +-----------------------------------------------------------+
        |   |     Cross-coupling: Poly gates <--> Metal 1 Jumpers       |
        |   +-----------------------------------------------------------+
        |   |   [MPD1: 6x2] (N-Active)    |    [MPD2: 6x2] (N-Active)   |  <-- P-Substrate
        |   |   [MPG1: 4x2] (N-Active)    |    [MPG2: 4x2] (N-Active)   |
        |   +-----------------------------------------------------------+
        |   |                 GND Line (Metal 1 / Metal 2)               |
        |   +-----------------------------------------------------------+
        |         |                                           |
        |        BL (Metal 2 vertical)                      BLB (Metal 2 vertical)
        +--------------------------------------------------------------------> X
                     <---- Wordline WL (Polysilicon / Metal 1 horizontal) ---->
```

---

## 3. Step-by-Step Drawing Guide in Electric GUI

### Step 1: Place Transistors in `6T{lay}`
1. In the **Components** tab on the left:
   - Select **MOSIS CMOS (mocmos)** technology.
2. Place the **NMOS Transistors**:
   - Place two **N-Transistors** for pull-down: double-click each, set **Width = 6**, **Length = 2**.
   - Place two **N-Transistors** for access: double-click each, set **Width = 4**, **Length = 2**.
3. Place the **PMOS Transistors**:
   - Place two **P-Transistors** for pull-up: double-click each, set **Width = 3**, **Length = 2**.
   - Electric will automatically enclose P-transistors in an **N-Well**.

### Step 2: Active Sharing & Contacts
1. **Source/Drain Active Sharing**:
   - Merge the drain of $M_{PD1}$ with the source of $M_{PG1}$ in a single contiguous N-active strip (diffusion sharing eliminates contact capacitance and saves layout height).
   - Merge the drain of $M_{PD2}$ with the source of $M_{PG2}$ similarly on the right side.
2. **Active Contacts**:
   - Place **Metal-1-N-Active-Con** on:
     - Source of $M_{PD1}$ and $M_{PD2}$ (connect to $GND$).
     - Drain of $M_{PG1}$ (connect to $BL$).
     - Drain of $M_{PG2}$ (connect to $BLB$).
   - Place **Metal-1-P-Active-Con** on:
     - Source of $M_{PU1}$ and $M_{PU2}$ (connect to $VDD$).
     - Drains of $M_{PU1}$ and $M_{PU2}$ (internal nodes $Q$ and $QB$).

### Step 3: Poly Gate Routing & Cross-Coupling
1. The gates of $M_{PD1}$ and $M_{PU1}$ share a single vertical **Polysilicon-1** strip (forms input of inverter 1).
2. The gates of $M_{PD2}$ and $M_{PU2}$ share a single vertical **Polysilicon-1** strip (forms input of inverter 2).
3. Connect the Wordline:
   - Route a horizontal line across the gates of $M_{PG1}$ and $M_{PG2}$ (Polysilicon-1 or Metal-1 via Poly-Contact).
4. Cross-Coupling:
   - Connect node $Q$ (drains of $M_{PD1}/M_{PU1}$) to the gate poly of Inverter 2 using a **Metal-1-Polysilicon-1-Con**.
   - Connect node $QB$ (drains of $M_{PD2}/M_{PU2}$) to the gate poly of Inverter 1 using a **Metal-1-Polysilicon-1-Con**.

### Step 4: Routing Rails & Bitlines
1. **Power & Ground**:
   - Route horizontal **Metal 1** rails across the top ($VDD$) and bottom ($GND$).
2. **Bitlines**:
   - Route vertical **Metal 2** tracks for $BL$ (left) and $BLB$ (right), connecting to the access transistor contacts via **Metal-1-Metal-2-Con** (Via 1).
3. **Well & Substrate Taps**:
   - Place at least one **N-Well Contact** (tied to $VDD$) and one **Substrate Tap / P-Well Contact** (tied to $GND$) to prevent latchup.

---

## 4. Port Exporting in `6T{lay}`

Under **Export $\rightarrow$ Create Export** (or `Ctrl+E` / `Cmd+E`), export the 5 external pins with names identical to `6T{sch}`:
- **`BL`** (Bidirectional): on the top or bottom of the left Metal 2 bitline track.
- **`BLB`** (Bidirectional): on the top or bottom of the right Metal 2 bitline track.
- **`WL`** (Input): on the horizontal Wordline track.
- **`VDD`** (Power): on the $VDD$ rail.
- **`GND`** (Ground): on the $GND$ rail.

---

## 5. Verification: DRC and NCC (Schematic vs. Layout)

1. **Design Rule Check (DRC)**:
   - Press **`F5`** (or **Tools $\rightarrow$ DRC $\rightarrow$ Check Hierarchically**).
   - Ensure the message window says: `0 errors found`.
2. **Network Consistency Check (NCC / LVS)**:
   - Make sure both `6T{sch}` and `6T{lay}` are in the cell `6T`.
   - Go to **Tools $\rightarrow$ NCC $\rightarrow$ Schematic and Layout Views of Current Cell**.
   - NCC will compare the netlist extracted from your layout with your schematic and confirm:
     `Cells '6T{sch}' and '6T{lay}' are topologically equivalent`.
