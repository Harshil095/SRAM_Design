* Task 10.1 & 10.2: Package Parasitic Testbench for SRAM_Chip_Final
.include ./tsmc018.lib
.include ./SRAM_Chip_Final_clean.spi

* Package Model
.SUBCKT PACKAGE_MODEL pad_external chip_internal
L_bond pad_external node1 2nH
R_bond node1 chip_internal 0.2
C_pad chip_internal 0 1pF
.ENDS

* Package Parasitic Instantiations (5 pins)
XPKG_CLK     ext_CLK     pad_CLK     PACKAGE_MODEL
XPKG_ADDR_IN ext_ADDR_IN pad_ADDR_IN PACKAGE_MODEL
XPKG_R_W     ext_R_W     pad_R_W     PACKAGE_MODEL
XPKG_DIN     ext_DIN     pad_DIN     PACKAGE_MODEL
XPKG_DOUT    ext_DOUT    pad_DOUT    PACKAGE_MODEL

* Off-chip Load Capacitance on DOUT
C_load ext_DOUT 0 2pF

* Power Supply
V_vdd vdd 0 DC 1.8

* Stimulus
* Clock: 100MHz (10ns period)
V_CLK ext_CLK 0 PULSE(0 1.8 0 100ps 100ps 4.9ns 10ns)

* Address In: Shift address 11111111 (all 1s)
V_ADDR_IN ext_ADDR_IN 0 DC 1.8

* Data In: Write 1
V_DIN ext_DIN 0 DC 1.8

* R_W: Write (1.8V) from 0 to 80ns, Read (0V) from 80ns to 160ns
V_R_W ext_R_W 0 PWL(0ns 1.8 79.9ns 1.8 80.1ns 0 160ns 0)

* Transient Analysis
.tran 50ps 160ns

* Power Measurement Commands:
.MEAS TRAN I_write_avg AVG I(V_vdd) FROM=70ns TO=80ns
.MEAS TRAN I_read_avg AVG I(V_vdd) FROM=150ns TO=160ns

* Timing Measurements:
.MEAS TRAN t_rise TRIG v(ext_DOUT) VAL=0.18 RISE=1 TARG v(ext_DOUT) VAL=1.62 RISE=1
.MEAS TRAN t_prop TRIG v(ext_CLK) VAL=0.9 RISE=16 TARG v(ext_DOUT) VAL=1.62 RISE=1

.END
