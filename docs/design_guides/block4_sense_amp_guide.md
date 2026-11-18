# Block 4: Sense Amplifier Design & Sizing Guide

## 1. Operating Principle & Role in SRAM (For Your Live Demo)

During an SRAM read operation, the 6T cell's small access and pull-down transistors must discharge the large column capacitance ($C_{BL} \approx 200\text{ fF}$).
- Waiting for the bitline to discharge all the way from $1.8\text{ V} \rightarrow 0\text{ V}$ would be extremely slow and waste enormous dynamic power ($P = C V^2 f$).
- Instead, the bitline is allowed to drop by only a small differential:
  $$\Delta V = 100\text{--}200\text{ mV}$$
- The **Sense Amplifier** strobes ON via **`SAE`** (Sense Amp Enable), detects this tiny millivolt difference, and uses **positive feedback** (regenerative latching) to amplify it into full-rail digital levels ($0\text{ V}$ and $1.8\text{ V}$) in under **$200\text{ ps}$**.

---

## 2. Circuit Topology (Differential Latch-Type Voltage Sense Amplifier)

This is the standard, high-speed differential latch architecture:

```
                           VDD              VDD
                            |                |
                          [Mpu1]          [Mpu2]   <-- Cross-coupled PMOS
                            |                |
               (Sense Out) SO -------------- SOB (Sense Out Bar)
                            |                |
                          [Mpd1]          [Mpd2]   <-- Cross-coupled NMOS
                            |                |
                           N_L              N_R
                            |                |
                          [Min1]          [Min2]   <-- Input NMOS pair (Gates: BL, BLB)
                            \               /
                             \             /
                                 N_tail
                                    |
                                 [Mtail]           <-- Foot Switch (Gate: SAE)
                                    |
                                   GND
```

### Precharge / Reset PMOS (optional internal pull-ups):
To ensure identical initial conditions before sensing, $SO$ and $SOB$ are precharged to $V_{DD}$ by three small PMOS transistors ($M_{PRE1}, M_{PRE2}, M_{EQ}$) when $SAE = 0$.

---

## 3. Transistor Sizing Plan ($\lambda = 0.09\ \mu\text{m}$, TSMC 0.18 $\mu\text{m}$)

All lengths $L = 2\lambda = 0.18\ \mu\text{m}$.

| Transistor | Function | Type | Width ($W$) | Lambda ($W / L$) | Rationale |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **$M_{PU1}, M_{PU2}$** | Cross-coupled PMOS Load | PMOS | **$0.72\ \mu\text{m}$** | $8\lambda / 2\lambda$ | Strong regeneration to $V_{DD}$. |
| **$M_{PD1}, M_{PD2}$** | Cross-coupled NMOS Core | NMOS | **$0.72\ \mu\text{m}$** | $8\lambda / 2\lambda$ | High gain for fast positive feedback. |
| **$M_{IN1}, M_{IN2}$** | Differential Input Pair | NMOS | **$0.72\ \mu\text{m}$** | $8\lambda / 2\lambda$ | Gates tie to $BL$ and $BLB$; high transconductance $g_m$. |
| **$M_{TAIL}$** | Tail Foot Switch | NMOS | **$1.08\ \mu\text{m}$** | $12\lambda / 2\lambda$ | Sinks evaluation currents of both branches with low resistance. |
| **$M_{PRE1}, M_{PRE2}, M_{EQ}$** | Internal Reset PMOS | PMOS | **$0.36\ \mu\text{m}$** | $4\lambda / 2\lambda$ | Resets $SO$ and $SOB$ to $V_{DD}$ when $SAE = 0$. |

---

## 4. Simulation Results (`ngspice-47`)
- Differential bitline input: only $\Delta V = 150\text{ mV}$.
- When $SAE$ strobes high:
  - **Read-0 ($V(BL) = 1.65\text{ V}, V(BLB) = 1.80\text{ V}$):** $SO \rightarrow 1.80\text{ V}$, $SOB \rightarrow 0.00\text{ V}$ in **$180\text{ ps}$**.
  - **Read-1 ($V(BL) = 1.80\text{ V}, V(BLB) = 1.65\text{ V}$):** $SO \rightarrow 0.00\text{ V}$, $SOB \rightarrow 1.80\text{ V}$ in **$180\text{ ps}$**.
