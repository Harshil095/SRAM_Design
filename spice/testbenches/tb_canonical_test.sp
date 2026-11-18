* Testbench with Canonical DFF and Initial Conditions
.include ./tsmc018.lib
.include ./SRAM_Chip_Final_canonical.spi

* Package Model
.SUBCKT PACKAGE_MODEL pad_external chip_internal
L_bond pad_external node1 2nH
R_bond node1 chip_internal 0.2
C_pad chip_internal 0 1pF
.ENDS

XPKG_CLK     ext_CLK     pad_CLK     PACKAGE_MODEL
XPKG_ADDR_IN ext_ADDR_IN pad_ADDR_IN PACKAGE_MODEL
XPKG_R_W     ext_R_W     pad_R_W     PACKAGE_MODEL
XPKG_DIN     ext_DIN     pad_DIN     PACKAGE_MODEL
XPKG_DOUT    ext_DOUT    pad_DOUT    PACKAGE_MODEL

C_load ext_DOUT 0 2pF
V_vdd vdd 0 DC 1.8

V_CLK ext_CLK 0 PULSE(0 1.8 0 100ps 100ps 4.9ns 10ns)
V_ADDR_IN ext_ADDR_IN 0 DC 1.8
V_DIN ext_DIN 0 DC 1.8
V_R_W ext_R_W 0 PWL(0ns 1.8 79.9ns 1.8 80.1ns 0 160ns 0)

.ic v(XSRAM_256@0.XDiv8_Cou@0.net@24)=0 v(XSRAM_256@0.XDiv8_Cou@0.net@31)=0 v(XSRAM_256@0.XDiv8_Cou@0.net@38)=0
.ic v(ext_DOUT)=0 v(pad_DOUT)=0 v(XSRAM_256@0.DOUT)=0
.ic v(XSRAM_256@0.XSRAM_Cor@0.XSRAM_Arr@0.XSRAM_Row@15.X_6T@15.Q)=0 v(XSRAM_256@0.XSRAM_Cor@0.XSRAM_Arr@0.XSRAM_Row@15.X_6T@15.net@0)=1.8

.tran 100ps 160ns

.MEAS TRAN I_write_avg AVG I(V_vdd) FROM=70ns TO=80ns
.MEAS TRAN I_read_avg AVG I(V_vdd) FROM=150ns TO=160ns
.MEAS TRAN t_rise TRIG v(ext_DOUT) VAL=0.18 RISE=1 TARG v(ext_DOUT) VAL=1.62 RISE=1
.MEAS TRAN t_prop TRIG v(ext_CLK) VAL=0.9 RISE=16 TARG v(ext_DOUT) VAL=1.62 RISE=1

.print tran v(ext_CLK) v(ext_DOUT) v(XSRAM_256@0.net@94) v(XSRAM_256@0.net@104) v(XSRAM_256@0.net@106)
.END
