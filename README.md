# 256-Bit Full-Custom CMOS SRAM Macro (16×16)

A complete, full-custom **256-bit ($16 \times 16$) Static Random-Access Memory (SRAM) macro** designed, physically laid out, and verified in **TSMC 180nm CMOS** technology ($V_{DD} = 1.8\text{ V}$) using **Electric VLSI** and **ngspice**.

Developed as the Final VLSI Design Project for B.Tech Electrical Engineering at the **Indian Institute of Technology Hyderabad (IITH)**.

---


## ⚡ Key Specifications

| Parameter | Specification | Verified Value | Notes |
| :--- | :---: | :---: | :--- |
| **Technology Node** | TSMC 180 nm | TSMC 0.18 µm 1P6M | BSIM3v3.1 (`LEVEL=49`) |
| **Supply Voltage ($V_{DD}$)** | 1.8 V | 1.8 V | Room temperature ($27^\circ\text{C}$) |
| **Grid Unit ($\lambda$)** | 0.09 µm | $L_{min} = 2\lambda = 0.18\,\mu\text{m}$ | MOSIS scalable CMOS (`mocmos`) |
| **Array Architecture** | $16 \times 16$ | 256 bits | 16 rows $\times$ 16 columns |
| **Operating Frequency** | 100 MHz (nominal) | **133 MHz ($F_{max}$)** | Single-phase synchronous clock |
| **Read Access Delay ($t_{prop}$)**| $< 10\text{ ns}$ | **6.256 ns** | From `CLK` rising edge to `DOUT` |
| **Read Energy per Bit** | — | **29.93 pJ/bit** | Full cycle active dynamic energy |
| **Write Energy per Bit** | — | **20.34 pJ/bit** | Full rail BL discharge + core latch |
| **Static Noise Margin (SNM)** | $> 200\text{ mV}$ | **286 mV (Hold) / 194 mV (Read)**| Butterfly curves simulated in SPICE |
| **Physical Layout Area** | Compact | $35.72 \times 10^6\,\lambda^2$ | Zero DRC errors, 100% matched NCC |
| **External Package Pins** | Bounded | **5 Pins** | `CLK`, `DIN`, `DOUT`, `ADDR_IN`, `R_W` |

---

## 🏛 Architecture & Block Diagram

```
                              5-PIN EXTERNAL INTERFACE
                   [CLK]    [ADDR_IN]    [DIN]    [R_W]    [DOUT]
                     |          |          |        |        ^
                     v          v          |        |        |
             +---------------+             |        |        |
             | 8-Bit Serial  |             |        |        |
             | Shift Register|             |        |        |
             |   + Div-8     |             |        |        |
             +---------------+             |        |        |
               | (4b)     | (4b)           |        |        |
               v          v                |        |        |
          +---------+ +---------+          |        |        |
          | Row Dec | | Col Dec |          |        |        |
          | (4-to-16| | (4-to-16|          |        |        |
          +---------+ +---------+          |        |        |
               | WL[15:0] | COL_SEL[15:0]  |        |        |
               |          |                |        |        |
               v          |                |        |        |
    +-------------------+ |                |        |        |
    |                   | |                |        |        |
    |   16 x 16 CORE    | |                |        |        |
    |   SRAM ARRAY      | |                |        |        |
    |   (256 Cells)     | |                |        |        |
    |                   | |                |        |        |
    +-------------------+ |                |        |        |
         | BL / BLB       |                |        |        |
         v                v                |        |        |
    +----------------------------------+   |        |        |
    |   Precharge & Equalizer Bank     |   |        |        |
    +----------------------------------+   |        |        |
    |   Write Driver Bank (16x)        |<--+--------+        |
    +----------------------------------+                     |
    |   16:1 Transmission Gate Col Mux |<--+                 |
    +----------------------------------+                     |
    |   Latch Sense Amplifier + Strobe |---------------------+
    +----------------------------------+
```

---
