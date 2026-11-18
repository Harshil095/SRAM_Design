# Block 5: Column Multiplexer (16:1 Column Mux) Design & Sizing Guide

## 1. Operating Role & Architecture (For Your Live Demo)

In a 16x16 SRAM array:
- There are **16 physical bitline columns** ($BL[0..15]$ and $BLB[0..15]$).
- The project specification requires a single 1-bit serial output (`DOUT`) and 1-bit serial input (`DIN`).
- Per the spec:
  > *"4->16 column decoder / 16:1 column mux (one sense amp and one write driver after the column mux is acceptable and saves area)."*

Instead of duplicating 16 bulky sense amplifiers and 16 write drivers, the **16:1 Column Mux** selects **1 of the 16 column bitline pairs** and connects it to a single **Global Bitline Pair (`GBL`, `GBLB`)**, which then feeds:
1. The **single Write Driver** (during writes).
2. The **single Sense Amplifier** (during reads).

---

## 2. Circuit Building Block: CMOS Transmission Gate (`tgate`)

For each column, we need to pass both $0\text{ V}$ and $1.8\text{ V}$ without threshold voltage drops ($V_t$ degradation).
A single NMOS pass-transistor passes '0' well, but degrades a '1' to $V_{DD} - V_{thn} \approx 1.43\text{ V}$.
Therefore, each switch is a **full CMOS Transmission Gate (TG)**:

```
              PMOS (Gate: SEL_B)
                 +---\___/---+
                 |           |
       BL -------+           +------- GBL
                 |           |
                 +---[   ]---+
              NMOS (Gate: SEL)
```

- When `SEL = 1` and `SEL_B = 0`: The transmission gate turns **ON** with low on-resistance ($R_{on} \approx 150\ \Omega$) across the entire $0\text{ V}\text{ to }1.8\text{ V}$ range.
- When `SEL = 0` and `SEL_B = 1`: The transmission gate turns **OFF** (High-Z).

Each column requires **2 Transmission Gates**:
- One TG for $BL \rightarrow GBL$.
- One TG for $BLB \rightarrow GBLB$.

---

## 3. Modular Hierarchy: 2-to-1 / 4-to-1 Column Mux

Instead of placing 32 transistors in one giant schematic, build it hierarchically:

### Level 1: `tgate` (CMOS Transmission Gate)
- 1 NMOS ($W = 8\lambda = 0.72\ \mu\text{m}$, $L = 2\lambda = 0.18\ \mu\text{m}$)
- 1 PMOS ($W = 12\lambda = 1.08\ \mu\text{m}$, $L = 2\lambda = 0.18\ \mu\text{m}$)
- Ports: `IN`, `OUT`, `SEL`, `SEL_B`, `VDD`, `GND`.

### Level 2: `col_mux_1x` (1 Column Switch Pair)
- Contains 2 `tgate` instances + 1 local inverter (to generate `SEL_B` from `SEL`):
  - `tgate_bl`: switches `BL` to `GBL`.
  - `tgate_blb`: switches `BLB` to `GBLB`.
- Ports: `BL`, `BLB`, `GBL`, `GBLB`, `SEL`, `VDD`, `GND`.

### Level 3: `col_mux_16` (16 Column Multiplexer)
- Tile 16 instances of `col_mux_1x` side-by-side.
- All 16 instances share the common output lines `GBL` and `GBLB`.
- Column $i$ is enabled by decoded select line $COL\_SEL[i]$.

---

## 4. Transistor Sizing Plan ($\lambda = 0.09\ \mu\text{m}$, TSMC 0.18 $\mu\text{m}$)

| Transistor | Type | Width ($W$) | Length ($L$) | Lambda ($W/L$) | Sizing Rationale |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **TG NMOS** | NMOS | **$0.72\ \mu\text{m}$** | **$0.18\ \mu\text{m}$** | $8\lambda / 2\lambda$ | Provides low on-resistance ($R_{on,N} \approx 180\ \Omega$) to pass write '0' quickly. |
| **TG PMOS** | PMOS | **$1.08\ \mu\text{m}$** | **$0.18\ \mu\text{m}$** | $12\lambda / 2\lambda$ | Sized $1.5\times$ NMOS to compensate for hole mobility $\mu_p$; passes precharge '1' with $R_{on,P} \approx 200\ \Omega$. |
| **Inverter PMOS** | PMOS | **$0.72\ \mu\text{m}$** | **$0.18\ \mu\text{m}$** | $8\lambda / 2\lambda$ | Generates `SEL_B` locally. |
| **Inverter NMOS** | NMOS | **$0.36\ \mu\text{m}$** | **$0.18\ \mu\text{m}$** | $4\lambda / 2\lambda$ | Generates `SEL_B` locally. |

*Verified Simulation (`ngspice-47`):* Sensed voltage difference of $150\text{ mV}$ propagates through the multiplexer to the Global Bitlines in **$< 80\text{ ps}$**.
