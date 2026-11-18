* Inspect internal signals
.include ./tsmc018.lib
.include ./SRAM_Chip_Final_clean.spi

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

.tran 100ps 160ns
.print tran v(ext_CLK) v(ext_DOUT) v(pad_DOUT) v(XSRAM_256@0.DOUT) v(XSRAM_256@0.net@94) v(XSRAM_256@0.net@104) v(XSRAM_256@0.net@106)
.END
