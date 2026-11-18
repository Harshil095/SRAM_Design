# Process Design Kit (PDK) & Model Libraries

This directory contains the technology model files and simulation parameters for the TSMC 180nm CMOS process.

## 📦 Model File

- **`tsmc018.lib`**: TSMC 0.18 µm ($180\,\text{nm}$) single-poly, six-metal (1P6M) bulk CMOS SPICE model deck.
  - **Model Level**: BSIM3v3.1 (`LEVEL=49`)
  - **Nominal Supply Voltage ($V_{DD}$)**: 1.8 V
  - **Nominal Threshold Voltages ($T = 27^\circ\text{C}$)**:
    - NMOS (`cmosn`): $V_{th0} \approx +0.366\text{ V}$
    - PMOS (`cmosp`): $V_{th0} \approx -0.391\text{ V}$
  - **Gate Oxide Thickness ($T_{ox}$)**: $4.1\times 10^{-9}\text{ m}$ ($41\,\text{Å}$)
  - **Minimum Channel Length ($L_{min}$)**: $0.18\,\mu\text{m}$ ($2\lambda$ at $\lambda = 0.09\,\mu\text{m}$)

## ⚙️ Layout Grid Parameters (MOSIS SCMOS)

Physical layouts are drawn using the **MOSIS scalable CMOS (`mocmos`)** submicron rules:
- **$\lambda$ (lambda)**: $0.09\,\mu\text{m}$ ($90\,\text{nm}$)
- **Grid Resolution**: $1\,\lambda = 0.09\,\mu\text{m}$
- **Minimum Gate Width ($W_{min}$)**: $3\lambda = 0.27\,\mu\text{m}$ (Leaf cell pull-up PMOS)
- **Minimum Gate Length ($L_{min}$)**: $2\lambda = 0.18\,\mu\text{m}$
