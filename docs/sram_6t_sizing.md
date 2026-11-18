# 6T SRAM Cell Sizing & Design Theory

## 1. Operating Requirements
A standard 6T SRAM cell consists of two cross-coupled CMOS inverters ($M_{PU1}/M_{PD1}$ and $M_{PU2}/M_{PD2}$) and two NMOS access (pass-gate) transistors ($M_{PG1}, M_{PG2}$) controlled by the Wordline ($WL$).
Storage nodes are labeled $Q$ and $QB$. Bitlines are $BL$ and $BLB$.

```
           VDD                 VDD
            |                   |
         [MPU1]              [MPU2]
            |                   |
BL ---[MPG1]--- Q ----------- QB ---[MPG2]--- BLB
            |                   |
         [MPD1]              [MPD2]
            |                   |
           GND                 GND
             ^                   ^
             |--- WL (gates) ----|
```

The cell has three fundamental stability and operational criteria:
1. **Hold Stability**: When $WL = 0$, cross-coupled inverters retain state against subthreshold leakage and noise.
2. **Read Stability**: During read ($WL = V_{DD}$, $BL = BLB = V_{DD}$ precharged), the node storing '0' (say $Q=0$) is pulled up by current through $M_{PG1}$ from $BL$. To prevent this voltage $V_{READ}$ from tripping inverter 2 ($M_{PU2}/M_{PD2}$), the **Pull-Down NMOS must be stronger than the Access NMOS**:
   $$\beta_{ratio} = CR = \frac{(W/L)_{PD}}{(W/L)_{PG}} > 1.2 \sim 1.5$$
3. **Write Ability**: During write ($WL = V_{DD}$, $BL = 0$, $BLB = V_{DD}$ to write '0' into $Q$), the access transistor $M_{PG1}$ pulls node $Q$ down from $V_{DD}$. To overwrite the state, $M_{PG1}$ must overpower the pull-up PMOS $M_{PU1}$:
   $$\mu_{ratio} = PR = \frac{(W/L)_{PU}}{(W/L)_{PG}} < 1$$

## 2. Transistor Sizing Selection (TSMC 0.18 µm, VDD = 1.8 V)
- $L = L_{\min} = 0.18\ \mu\text{m}$ for all transistors to maximize speed and minimize cell area.
- Minimum contactable active width: $W_{\min} = 0.28\ \mu\text{m}$.
- **Pull-Up PMOS ($M_{PU1}, M_{PU2}$)**:
  - $W_{PU} = 0.28\ \mu\text{m}$, $L_{PU} = 0.18\ \mu\text{m}$
  - Kept at minimum size to facilitate easy write-overpowering while maintaining adequate hold margin.
- **Access NMOS ($M_{PG1}, M_{PG2}$)**:
  - $W_{PG} = 0.36\ \mu\text{m}$, $L_{PG} = 0.18\ \mu\text{m}$
  - $W_{PG} > W_{PU}$ ensures fast and reliable write margin without contention.
- **Pull-Down NMOS ($M_{PD1}, M_{PD2}$)**:
  - Nominal: $W_{PD} = 0.54\ \mu\text{m}$, $L_{PD} = 0.18\ \mu\text{m}$
  - Ratio $CR = W_{PD} / W_{PG} = 0.54 / 0.36 = 1.50$
  - Ensures low $V_{READ} \approx 0.18\text{ V} < V_{thn}$ ($0.366\text{ V}$), yielding robust Static Noise Margin ($SNM > 200\text{ mV}$) during read operations.

We will sweep $W_{PD} \in \{0.28, 0.36, 0.44, 0.54, 0.65\}\ \mu\text{m}$ to quantitatively demonstrate the trade-off between Read SNM and cell area.
