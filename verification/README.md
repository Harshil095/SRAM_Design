# Verification, DRC Audits & Diagnostics

This directory contains verification audit logs, design-rule violation classifications, and coordinate repair data generated during physical layout and LVS/NCC verification.

## 📋 Verification Results Summary

```
Target Block:        SRAM_Chip_Final{lay} (Integrated Top-Level Chip)
Technology Node:     TSMC 0.18 µm CMOS (mocmos, lambda = 0.09 µm)
Design Rules:        MOSIS CMOS SCMOS (Submicron)
----------------------------------------------------------------------
DRC Status:          0 ERRORS (Clean)
NCC / LVS Match:     100% Topologically Equivalent (0 Mismatches)
Well Spacing:        Compliant (Min 12 lambda N-well/P-substrate)
Metal 1-3 Spacing:   Compliant (Min 3 lambda M1/M2/M3)
Via Enclosure:       Compliant (Min 1 lambda metal overlap of vias)
Antenna Rules:       Compliant with internal diode protection
----------------------------------------------------------------------
```

## 📄 Audit Files

| File | Description |
| :--- | :--- |
| **`drc_breakdown.txt`** | Comprehensive breakdown of design rule verification across all cells in the chip hierarchy. |
| **`all_macro_errors.txt`** | Complete error log from iterative macro routing passes, detailing layer, node, and arc coordinates. |
| **`exact_fixes.txt`** | Exact coordinate transformation table applied to resolve spacing and notch violations. |
| **`top_fixes.txt`** | Routing fixes applied to top-level pad connections, power rings, and global clock distribution lines. |
| **`sa_arcs.txt`** | Sense amplifier interconnect connectivity audit verifying zero floating or shorted nodes. |
| **`diff_output.txt`** | Structural diff comparison between pre-clean and post-clean layout iterations. |
| **`dout_trace.txt`** | Output buffer signal integrity trace verifying rail-to-rail digital levels. |
| **`macro_sch_data.txt`** | Extracted schematic node, arc, and port coordinates used for programmatic SVG rendering. |
