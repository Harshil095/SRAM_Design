* SPICE Testbench: tb_SRAM_Chip.sp
* Complete 256-Bit SRAM Chip with Package Parasitics & Bond Wire Model
* Technology: TSMC 0.18um CMOS (tsmc018.lib)
* Target Deck: SRAM_Chip_Final.spi

.include ./tsmc018.lib
.include ./SRAM_Chip_Final.spi

* ==============================================================================
* Task 10.1: Package Parasitic Model (Bond Wire & Pad Model)
* ==============================================================================
.SUBCKT PACKAGE_MODEL pad_external chip_internal
L_bond pad_external node1 2nH
R_bond node1 chip_internal 0.2
C_pad chip_internal 0 1pF
.ENDS

* Instantiate Package Model for the 5 External Chip Pins
XPKG_CLK     ext_CLK     pad_CLK     PACKAGE_MODEL
XPKG_ADDR_IN ext_ADDR_IN pad_ADDR_IN PACKAGE_MODEL
XPKG_R_W     ext_R_W     pad_R_W     PACKAGE_MODEL
XPKG_DIN     ext_DIN     pad_DIN     PACKAGE_MODEL
XPKG_DOUT    ext_DOUT    pad_DOUT    PACKAGE_MODEL

* Off-Chip DOUT Load Capacitance (PCB trace / test fixture)
C_load ext_DOUT 0 2pF

* Ideal Power Supply: 1.8V to vdd and 0V to gnd attached directly to chip nodes
V_vdd vdd 0 DC 1.8

* ==============================================================================
* Task 10.2: Power & Performance Transient Stimulus (100MHz / 10ns Period)
* ==============================================================================
* Clock Setup: PULSE source on CLK with 10ns period (50% duty cycle)
V_CLK ext_CLK 0 PULSE(0 1.8 0 100ps 100ps 4.9ns 10ns)

* Address In: Shift address 11111111 (Row 15, Col 15) over 8 clock cycles
V_ADDR_IN ext_ADDR_IN 0 DC 1.8

* Data In: Write 1
V_DIN ext_DIN 0 DC 1.8

* R_W Control: Write (1.8V) from 0ns to 80ns, Read (0V) from 80ns to 160ns
V_R_W ext_R_W 0 PWL(0ns 1.8 79.9ns 1.8 80.1ns 0 160ns 0)

* ==============================================================================
* Initial Conditions for Divide-by-8 Synchronous Ripple Counter & Output Pad
* ==============================================================================
.ic v(XSRAM_256@0.XDiv8_Cou@0.net@24)=1.8 v(XSRAM_256@0.XDiv8_Cou@0.net@7)=0
.ic v(XSRAM_256@0.XDiv8_Cou@0.XDFF@0.net@1)=0 v(XSRAM_256@0.XDiv8_Cou@0.XDFF@0.net@3)=1.8 v(XSRAM_256@0.XDiv8_Cou@0.XDFF@0.net@5)=1.8

.ic v(XSRAM_256@0.XDiv8_Cou@0.net@31)=1.8 v(XSRAM_256@0.XDiv8_Cou@0.net@9)=0
.ic v(XSRAM_256@0.XDiv8_Cou@0.XDFF@1.net@1)=0 v(XSRAM_256@0.XDiv8_Cou@0.XDFF@1.net@3)=1.8 v(XSRAM_256@0.XDiv8_Cou@0.XDFF@1.net@5)=1.8

.ic v(XSRAM_256@0.XDiv8_Cou@0.net@38)=1.8 v(XSRAM_256@0.XDiv8_Cou@0.net@11)=0
.ic v(XSRAM_256@0.XDiv8_Cou@0.XDFF@2.net@1)=0 v(XSRAM_256@0.XDiv8_Cou@0.XDFF@2.net@3)=1.8 v(XSRAM_256@0.XDiv8_Cou@0.XDFF@2.net@5)=1.8

.ic v(ext_DOUT)=0 v(pad_DOUT)=0

* Transient Simulation: 50ps maximum step up to 160ns
.options method=gear reltol=2e-3 chgtol=1e-15 trtol=1
.tran 50ps 160ns uic

* ==============================================================================
* Task 10.2 & 10.3: Power & Performance Measurement Cards
* ==============================================================================
* Average current from VDD during Write Execute cycle (70ns to 80ns):
.MEAS TRAN I_write_avg AVG I(V_vdd) FROM=70ns TO=80ns

* Average current from VDD during Read cycle (80ns to 90ns):
.MEAS TRAN I_read_avg AVG I(V_vdd) FROM=80ns TO=90ns

* Average current from VDD during Steady-State Read cycle (150ns to 160ns):
.MEAS TRAN I_read_steady AVG I(V_vdd) FROM=150ns TO=160ns

* Rise time of DOUT (from 10% to 90% of VDD = 0.18V to 1.62V):
.MEAS TRAN t_rise TRIG v(ext_DOUT) VAL=0.18 RISE=1 TARG v(ext_DOUT) VAL=1.62 RISE=1

* Propagation delay: from 8th clock rising edge (80ns) until DOUT reaches 90% VDD (1.62V):
.MEAS TRAN t_prop TRIG v(ext_CLK) VAL=0.9 RISE=8 TARG v(ext_DOUT) VAL=1.62 RISE=1

.END
