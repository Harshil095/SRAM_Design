# Block 1 Report: 6T SRAM Cell Verification & Transistor Sizing

## 1. Circuit Topology & Netlist
- Subcircuit definition: [`sram_6t.cir`](file:///Users/harshil_rathan/Documents/BTech%20IITH/SEM5/VLSI/Final_Project/sims/sram_6t.cir)
- Technology: TSMC 0.18 µm CMOS (`tsmc018.lib`, Level 49 BSIM3v3.1, $V_{DD} = 1.8\text{ V}$)
- Transistors:
  - **Pull-Down NMOS ($M_{PD1}, M_{PD2}$)**: $W_{PD} = 0.54\ \mu\text{m}$, $L = 0.18\ \mu\text{m}$
  - **Access Pass-Gate NMOS ($M_{PG1}, M_{PG2}$)**: $W_{PG} = 0.36\ \mu\text{m}$, $L = 0.18\ \mu\text{m}$
  - **Pull-Up PMOS ($M_{PU1}, M_{PU2}$)**: $W_{PU} = 0.28\ \mu\text{m}$, $L = 0.18\ \mu\text{m}$

## 2. Theoretical Sizing Rationale
1. **Read Stability (Cell Ratio $CR = \beta_{ratio}$)**:
   $$CR = \frac{W_{PD}/L_{PD}}{W_{PG}/L_{PG}} = \frac{0.54}{0.36} = 1.50$$
   During read, precharged bitline ($1.8\text{ V}$) charges node $Q$ via $M_{PG1}$, while $M_{PD1}$ sinks this current to ground. Sizing $CR = 1.50$ restricts the internal bump to $V_{READ} = 0.248\text{ V} < V_{thn} = 0.366\text{ V}$, completely preventing read disturbance or state flipping.
2. **Writability (Pull-Up Ratio $PR = \mu_{ratio}$)**:
   $$PR = \frac{W_{PU}/L_{PU}}{W_{PG}/L_{PG}} = \frac{0.28}{0.36} = 0.78 < 1.0$$
   Because $\mu_n \approx 2.4 \mu_p$, NMOS pass gate $M_{PG}$ overpoweringly discharges the internal node against the weak PMOS $M_{PU}$, ensuring rapid write flips at a write trip voltage of $V_{trip} = 0.746\text{ V}$.
3. **Area Optimization**:
   $W_{PU} = 0.28\ \mu\text{m}$ is the minimum contactable feature size in 0.18 µm scalable rules, minimizing cell height and active silicon area.

---

## 3. Pull-Down Width Sweep ($W_{PD}$) Table

Simulated using [`run_wpd_sweep.py`](file:///Users/harshil_rathan/Documents/BTech%20IITH/SEM5/VLSI/Final_Project/sims/run_wpd_sweep.py):

| $W_{PD}$ ($\mu\text{m}$) | Cell Ratio ($CR$) | Read SNM (mV) | Write Trip $V_{BL}$ (V) | Write Margin ($V_{DD} - V_{trip}$) | Write Status |
| :---: | :---: | :---: | :---: | :---: | :---: |
| 0.28 $\mu\text{m}$ | 0.78 | 149.0 mV | 0.826 V | 0.974 V | PASS |
| 0.36 $\mu\text{m}$ | 1.00 | 192.0 mV | 0.794 V | 1.006 V | PASS |
| 0.44 $\mu\text{m}$ | 1.22 | 228.0 mV | 0.771 V | 1.029 V | PASS |
| **0.54 $\mu\text{m}$ (Nominal)** | **1.50** | **263.0 mV** | **0.746 V** | **1.054 V** | **PASS** |
| 0.65 $\mu\text{m}$ | 1.81 | 292.0 mV | 0.724 V | 1.076 V | PASS |

> **Conclusion**: $W_{PD} = 0.54\ \mu\text{m}$ offers an exceptional Read SNM of **263 mV** (> 14% of $V_{DD}$) while maintaining high write margin ($V_{trip} = 0.746\text{ V}$) and compact cell layout pitch.

---

## 4. Operational Waveform & Verification Summary

From full transient SPICE simulation [`tb_sram_transient.cir`](file:///Users/harshil_rathan/Documents/BTech%20IITH/SEM5/VLSI/Final_Project/sims/tb_sram_transient.cir) with $C_{BL} = 200\text{ fF}$:

- **Hold-0 ($t=1.0\text{ ns}$)**: $V(Q) = 0.000\text{ V}$, $V(QB) = 1.800\text{ V}$ (**Hold SNM = 592 mV**).
- **Read-0 ($t=2.0\text{ ns}\text{ to }4.0\text{ ns}$, $WL=1.8\text{ V}$)**:
  - Max bump on $Q$: $0.248\text{ V}$ (stored-0 does not flip).
  - Bitline differential at 1.0 ns: $\Delta V = V(BLB) - V(BL) = 971.3\text{ mV}$.
- **Write-1 ($t=6.0\text{ ns}\text{ to }8.0\text{ ns}$, $BLB=0\text{ V}$)**:
  - $Q$ flips to $1.800\text{ V}$, $QB$ flips to $0.000\text{ V}$ in $< 200\text{ ps}$.
- **Hold-1 ($t=9.0\text{ ns}$)**: $V(Q) = 1.800\text{ V}$, $V(QB) = 0.000\text{ V}$.
- **Read-1 ($t=10.0\text{ ns}\text{ to }12.0\text{ ns}$, $WL=1.8\text{ V}$)**:
  - Max bump on $QB$: $0.227\text{ V}$.
  - Bitline differential at 1.0 ns: $\Delta V = V(BL) - V(BLB) = 972.2\text{ mV}$.
- **Write-0 ($t=14.0\text{ ns}\text{ to }16.0\text{ ns}$, $BL=0\text{ V}$)**:
  - $Q$ flips cleanly to $0.000\text{ V}$, $QB$ flips to $1.800\text{ V}$.
- **Hold-0 ($t=17.0\text{ ns}$)**: $V(Q) = 0.000\text{ V}$, $V(QB) = 1.800\text{ V}$.
