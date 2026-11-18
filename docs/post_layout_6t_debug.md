# 6T Layout Post-Layout Verification & Waveform Viewer Debug Report

## 1. Post-Layout Extracted Netlist Audit (`6T.spi`)
- Technology: TSMC 0.18 µm CMOS (`tsmc018.lib`, Level 49 BSIM3v3.1)
- Extracted parasitic capacitances and geometries:
  - Pull-down NMOS: $W = 0.54\ \mu\text{m}$, $L = 0.18\ \mu\text{m}$, $A_S = 0.731\text{ pF}$, $P_S = 4.815\ \mu\text{m}$
  - Access NMOS: $W = 0.36\ \mu\text{m}$, $L = 0.18\ \mu\text{m}$
  - Pull-up PMOS: $W = 0.27\ \mu\text{m}$, $L = 0.18\ \mu\text{m}$
- **DRC**: 0 Errors.
- **NCC**: 0 Errors (Topologically equivalent to `6T{sch}`).

---

## 2. Root Cause of the "Stuck / White Screen" in Electric GUI

Electric runs the following shell command when you invoke SPICE from the GUI:
```bash
/opt/homebrew/bin/ngspice -b 6T.spi -r 6T.raw -o 6T.out
```

### Why it appears stuck / white:
1. **The SPICE simulation finished completely in 0.118 seconds**:
   The output log `6T.out` shows:
   ```text
   Doing analysis at TEMP = 27.000000 and TNOM = 27.000000
   No. of Data Rows : 3910
   Total elapsed time = 0.118 s
   ```
   Ngspice **did not freeze or hang**. It completed the entire 38 ns transient analysis.
2. **Missing Waveform Plot Directive**:
   Because there was no `.control ... run ... plot v(bl) v(blb) ... .endc` block inside the SPICE code card, ngspice ran in silent batch mode.
3. **Electric Epic Parser Incompatibility**:
   Electric's built-in Java waveform window ("Epic") attempts to parse `6T.out`. On modern ngspice versions (like `ngspice-47`), ngspice prints banner messages like:
   ```text
   Note: No compatibility mode selected!
   ```
   Electric throws:
   `Error in line 2: Unrecognized Epic line: Note: No compatibility mode selected!`
   Because Epic crashes parsing the text banner, it fails to draw the traces and leaves a blank white window.

---

## 3. Post-Layout Simulation Results Extracted from `6T.raw`

The post-layout simulation with all layout-extracted parasitics ran with 100% functionality:

| Time | Operation | $WL$ | $BL$ | $BLB$ | $Q$ (`net@3`) | $QB$ (`net@4`) | Result |
| :---: | :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **$4.00\text{ ns}$** | **Write-0** | $1.80\text{ V}$ | $0.00\text{ V}$ | $1.80\text{ V}$ | $0.000\text{ V}$ | $1.800\text{ V}$ | **PASS**: Stored '0' written cleanly. |
| **$8.00\text{ ns}$** | **Hold-0** | $0.00\text{ V}$ | $1.80\text{ V}$ | $1.80\text{ V}$ | $0.000\text{ V}$ | $1.800\text{ V}$ | **PASS**: Retention verified. |
| **$12.50\text{ ns}$** | **Read-0** | $1.80\text{ V}$ | $0.48\text{ V}$ | $1.80\text{ V}$ | $0.178\text{ V}$ (bump) | $1.798\text{ V}$ | **PASS**: Bump is only $0.178\text{ V} \ll V_{thn}$ ($0.366\text{ V}$). Bitline differential $\Delta V = \mathbf{1326.3\text{ mV}}$. |
| **$21.00\text{ ns}$** | **Write-1** | $1.80\text{ V}$ | $1.80\text{ V}$ | $0.00\text{ V}$ | $1.800\text{ V}$ | $0.000\text{ V}$ | **PASS**: Cell flips cleanly to state '1'. |
| **$25.00\text{ ns}$** | **Hold-1** | $0.00\text{ V}$ | $1.80\text{ V}$ | $1.80\text{ V}$ | $1.800\text{ V}$ | $0.000\text{ V}$ | **PASS**: Retention verified. |
| **$29.50\text{ ns}$** | **Read-1** | $1.80\text{ V}$ | $1.80\text{ V}$ | $0.48\text{ V}$ | $1.798\text{ V}$ | $0.180\text{ V}$ (bump) | **PASS**: Bitline differential $\Delta V = \mathbf{1319.1\text{ mV}}$. |

---

## 4. The Fix to View Waveforms in Electric GUI

### Fix A: Add `.control` with `plot` into the SPICE code card
In your SPICE code card in `6T{lay}` (or `6T_tb{sch}`), replace the plain `.tran 10p 38n` with:
```spice
.control
run
plot v(bl) v(blb) v(wl)
plot v(net@3) v(net@4)
.endc
```
This causes ngspice itself to pop up its native macOS X11/Aqua waveform plot window directly.

### Fix B: Suppress the ngspice banner in Electric Preferences
In Electric:
1. Go to **File $\rightarrow$ Preferences $\rightarrow$ Tools $\rightarrow$ Spice/CDL**.
2. For the **Spice Engine Run Line**, change:
   ```bash
   ngspice -b %s -r %s.raw -o %s.out
   ```
   to:
   ```bash
   ngspice -b -q %s -r %s.raw -o %s.out
   ```
   *(The `-q` flag turns on quiet mode, suppressing the "Note: No compatibility mode selected!" banner that breaks Electric's internal Epic parser).*
