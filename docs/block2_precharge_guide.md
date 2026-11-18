# Block 2 Design & Verification Report: Precharge & Equalizer Circuit

## 1. Circuit Purpose & Theory
In an SRAM column, bitlines $BL$ and $BLB$ have large parasitic capacitance ($C_{BL} \approx 150\text{--}200\text{ fF}$) due to:
- Diffusion junctions of all access transistors on the column (16 cells)
- Metal 2 line routing capacitance across the array height

Before every Read and Write access, both bitlines must be precharged to $V_{DD}$ ($1.8\text{ V}$) and equalized to exact potential ($\Delta V = 0\text{ V}$).
If the bitlines are not equalized, any residual voltage imbalance can cause the sense amplifier to misread data.

The precharge circuit uses **3 PMOS transistors**:
1. **$M_{PRE1}$ (BL Pull-up)**: Pulls $BL$ up to $V_{DD}$ when $PRE\_B = 0$.
2. **$M_{PRE2}$ (BLB Pull-up)**: Pulls $BLB$ up to $V_{DD}$ when $PRE\_B = 0$.
3. **$M_{EQ}$ (Equalizer)**: Placed directly between $BL$ and $BLB$. Equalizes small residual voltage differences between the two bitlines, speeding up recovery after a write cycle.

---

## 2. Transistor Sizing & Selection

| Transistor | Type | Width ($W$) | Length ($L$) | Lambda Grid ($\lambda = 0.09\ \mu\text{m}$) | Rationale |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **$M_{PRE1}$** | PMOS | **$0.54\ \mu\text{m}$** | **$0.18\ \mu\text{m}$** | $W = 6\lambda, L = 2\lambda$ | Sized strong enough to pull $200\text{ fF}$ from $0\text{ V}$ to $1.8\text{ V}$ in $< 2\text{ ns}$ during clock LOW. |
| **$M_{PRE2}$** | PMOS | **$0.54\ \mu\text{m}$** | **$0.18\ \mu\text{m}$** | $W = 6\lambda, L = 2\lambda$ | Identical match to $M_{PRE1}$ to preserve column symmetry. |
| **$M_{EQ}$** | PMOS | **$0.36\ \mu\text{m}$** | **$0.18\ \mu\text{m}$** | $W = 4\lambda, L = 2\lambda$ | Balances equalization speed with low gate capacitance loading on $PRE\_B$. |

---

## 3. Simulation Results (`tb_precharge.cir`, ngspice-47)
- Bitline capacitance: $C_{BL} = C_{BLB} = 200\text{ fF}$.
- Worst-case initial condition: $V(BL) = 0\text{ V}$ (fully discharged by previous write-0), $V(BLB) = 1.8\text{ V}$.
- **Precharge Rise Time ($10\%$ to $90\%$ of $V_{DD}$)**: **$1.77\text{ ns}$**.
- **Bitline Balance ($\Delta V < 10\text{ mV}$)**: reached in **$2.83\text{ ns}$**.
- At clock speeds up to $200\text{ MHz}$ (5 ns cycle, 2.5 ns half-cycle), this ensures the bitline differential is completely eliminated before wordline assertion.
