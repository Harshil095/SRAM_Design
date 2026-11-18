*** Canonical DFF Test
.include ./tsmc018.lib
.global gnd vdd

.SUBCKT SRAM__inverter in out
Mnmos@0 out in gnd gnd CMOSN L=0.4U W=0.8U
Mpmos@0 vdd in out vdd CMOSP L=0.4U W=1.6U
.ENDS SRAM__inverter

.SUBCKT SRAM__tgate EN ENB gnd IN OUT vdd
Mnmos@0 IN EN OUT gnd CMOSN L=0.4U W=1.6U
Mpmos@0 OUT ENB IN vdd CMOSP L=0.4U W=2.4U
.ENDS SRAM__tgate

.SUBCKT SRAM__DFF CLK D gnd Q vdd
XINV1 CLK net@20 SRAM__inverter
XINV2 net@20 net@22 SRAM__inverter
XINV3 net@1 net@3 SRAM__inverter
XINV4 net@3 net@12 SRAM__inverter
XINV5 net@5 Q SRAM__inverter
XINV6 Q net@14 SRAM__inverter
XTG1 net@20 net@22 gnd D net@1 vdd SRAM__tgate
XTG2 net@22 net@20 gnd net@12 net@1 vdd SRAM__tgate
XTG3 net@22 net@20 gnd net@3 net@5 vdd SRAM__tgate
XTG4 net@20 net@22 gnd net@14 net@5 vdd SRAM__tgate
.ENDS SRAM__DFF

V_vdd vdd 0 DC 1.8
VCLK CLK 0 PULSE(0 1.8 1n 0.1n 0.1n 4.9n 10n)
VD D 0 PWL(0n 0 7n 0 7.2n 1.8 23n 1.8 23.2n 0 35n 0 35.2n 1.8 60n 1.8)
XDFF CLK D 0 Q vdd SRAM__DFF

.tran 0.1n 60n
.print tran v(CLK) v(D) v(Q)
.END
