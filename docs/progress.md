# 16x16 SRAM Project Progress Log

## Project Overview
- **Technology**: TSMC 0.18 µm CMOS (`tsmc018.lib`, BSIM3v3.1, LEVEL=49)
- **Power Supply ($V_{DD}$)**: 1.8 V
- **Architecture**: 16x16 (256-bit) 6T SRAM
- **Interface**: 5 external pins (`CLK`, `DIN`, `DOUT`, `ADDR_IN`, `R_W`)
- **Addressing**: 8-bit serial shift register + address latch (4-bit row, 4-bit column)

---

## Progress Checklist & Status

| Block / Step | Design | SPICE TB | Simulation | Status | Notes |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **0. Setup & Audit** | Done | Done | Done | **PASSED** | Environment confirmed (`ngspice-47`), models audited. Starting clean build. |
| **1. 6T SRAM Cell (Sch & Lay)** | Done | Done | Done | **PASSED** | Schematic & layout verified: 0 DRC, 0 NCC. Post-layout sim passed with extraction. |
| **2. Precharge & Equalizer** | Done | Done | Done | **PASSED** | `pre_charge` sch, ic, and lay complete and verified. Rise time ~1.8ns on 200fF. |
| **3. Write Driver** | Pending | Pending | Pending | **NEXT** | Full-rail discharge of BL or BLB during write. |
| **3. Write Driver** | Pending | Pending | Pending | Pending | Full-rail discharge of BL or BLB during write. |
| **4. Sense Amplifier** | Pending | Pending | Pending | Pending | Latch-type voltage sense amplifier + strobe. |
| **5. Column Mux (16:1)** | Pending | Pending | Pending | Pending | 16:1 column muxing with decoded select lines. |
| **6. Row & Column Decoders** | Pending | Pending | Pending | Pending | 4-to-16 decoders with enable gating. |
| **7. Serial Addr Shift Reg + Latch** | Pending | Pending | Pending | Pending | 8-bit serial-in parallel-out + latch. |
| **8. Control & Timing Generator** | Pending | Pending | Pending | Pending | Clock-phase driven self-timing. |
| **9. 1-Column Read/Write Verif.** | Pending | Pending | Pending | Pending | Full read/write cycle verification. |
| **10. 16x16 Array Integration** | Pending | Pending | Pending | Pending | Complete 256-bit core array. |
| **11. I/O Pads & Clock Tree** | Pending | Pending | Pending | Pending | R-L-C pad models, ESD, buffered clock distribution. |
| **12. Layout & Post-Layout Sim** | Pending | Pending | Pending | Pending | Extraction, DRC/NCC guidance, power & frequency metrics. |

---

## Log of Actions
- **2026-10-23**: Initialized repository structure (`backups/`, `sims/`, `docs/`).
- **2026-10-23**: Backed up `SRAM.jelib` to `backups/SRAM_initial_20261023_024555.jelib`. Created working library copy `SRAM_work.jelib`.
- **2026-10-23**: Verified ngspice-47 is available. Audited BSIM3v3.1 models in `tsmc018.lib` ($V_{thn} \approx 0.366\text{ V}$, $V_{thp} \approx -0.391\text{ V}$).
