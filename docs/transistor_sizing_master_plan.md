# Transistor Sizing Audit & Master Sizing Plan

## 1. Current State in `SRAM.jelib`

| Cell | Transistor | Type | Current Schematic ($W / L$) | Current Layout ($W / L$) | Target Optimal ($W / L$) |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **`6T`** | Pull-Down $M_{PD1}, M_{PD2}$ | NMOS | $6\lambda / 2\lambda$ ($0.54 / 0.18\ \mu\text{m}$) | $6\lambda / 2\lambda$ ($0.54 / 0.18\ \mu\text{m}$) | **$0.54\ \mu\text{m} / 0.18\ \mu\text{m}$** |
| | Access $M_{PG1}, M_{PG2}$ | NMOS | $4\lambda / 2\lambda$ ($0.36 / 0.18\ \mu\text{m}$) | $4\lambda / 2\lambda$ ($0.36 / 0.18\ \mu\text{m}$) | **$0.36\ \mu\text{m} / 0.18\ \mu\text{m}$** |
| | Pull-Up $M_{PU1}, M_{PU2}$ | PMOS | $3\lambda / 2\lambda$ ($0.27 / 0.18\ \mu\text{m}$) | $3\lambda / 2\lambda$ ($0.27 / 0.18\ \mu\text{m}$) | **$0.28\ \mu\text{m} / 0.18\ \mu\text{m}$** |
| **`pre_charge`** | Pull-Up $M_{PRE1}, M_{PRE2}$ | PMOS | $8\lambda / 2\lambda$ ($0.72 / 0.18\ \mu\text{m}$) | $7\lambda / 2\lambda$ ($0.63 / 0.18\ \mu\text{m}$) | **$0.54\ \mu\text{m}\text{--}0.72\ \mu\text{m}$** |
| | Equalizer $M_{EQ}$ | PMOS | $8\lambda / 2\lambda$ ($0.72 / 0.18\ \mu\text{m}$) | $7\lambda / 2\lambda$ ($0.63 / 0.18\ \mu\text{m}$) | **$0.36\ \mu\text{m}\text{--}0.54\ \mu\text{m}$** |
| **`inverter`** | Pull-Up $M_P$ | PMOS | $8\lambda / 2\lambda$ ($0.72 / 0.18\ \mu\text{m}$) | $7\lambda / 2\lambda$ ($0.63 / 0.18\ \mu\text{m}$) | **$0.54\ \mu\text{m}\text{--}0.72\ \mu\text{m}$** |
| | Pull-Down $M_N$ | NMOS | $4\lambda / 2\lambda$ ($0.36 / 0.18\ \mu\text{m}$) | $3\lambda / 2\lambda$ ($0.27 / 0.18\ \mu\text{m}$) | **$0.28\ \mu\text{m}\text{--}0.36\ \mu\text{m}$** |
| **`nand4`** | Stack $M_{N1}\text{--}M_{N4}$ | NMOS | $16\lambda / 2\lambda$ ($1.44 / 0.18\ \mu\text{m}$) | $3\lambda / 2\lambda$ ($0.27 / 0.18\ \mu\text{m}$) | **$1.44\ \mu\text{m} / 0.18\ \mu\text{m}$** (Sch) |
| | Parallel $M_{P1}\text{--}M_{P4}$| PMOS | $8\lambda / 2\lambda$ ($0.72 / 0.18\ \mu\text{m}$) | $7\lambda / 2\lambda$ ($0.63 / 0.18\ \mu\text{m}$) | **$0.54\ \mu\text{m}\text{--}0.72\ \mu\text{m}$** |

---

## 2. Integration & Sizing Rules

1. **4-input NAND Stack Rule**:
   - In a 4-input NAND gate, 4 NMOS transistors are in series to ground.
   - To match the pull-down strength of a single unit NMOS ($W_N \approx 0.36\ \mu\text{m}$), each NMOS in the series stack must be scaled by $4\times$:
     $$W_{N,stack} = 4 \times 0.36\ \mu\text{m} = 1.44\ \mu\text{m} \quad (16\lambda)$$
   - In schematic `nand4{sch}`, all 4 series NMOS are properly sized to $W = 16\lambda$.
2. **Inverter Balance Rule ($P/N$ Ratio)**:
   - For TSMC 0.18 µm, $\mu_n \approx 2.4 \mu_p$.
   - A balanced inverter switching threshold ($V_M \approx V_{DD}/2 = 0.9\text{ V}$) requires $\beta_p \approx \beta_n \implies W_P / W_N \approx 2.0$.
   - Sizing $W_P = 8\lambda$ ($0.72\ \mu\text{m}$) and $W_N = 4\lambda$ ($0.36\ \mu\text{m}$) provides a $2:1$ ratio with $t_{rise} \approx t_{fall} \approx 60\text{ ps}$.
3. **Power/Ground Export Consistency**:
   - `inverter{sch}` and `nand4{sch}` use global power/ground symbols, but their layout views export `Evdd` and `Egnd`.
   - To avoid NCC port mismatch when placing in higher-level decoders, export `VDD` and `GND` explicitly across all views.
