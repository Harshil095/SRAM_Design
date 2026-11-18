with open("generate_report.py") as f:
    text = f.read()

# Fix imageplaceholder definition
old_def = r"{\footnotesize\texttt{\color{crimson}[Insert: #3]}}"
new_def = r"{\footnotesize\texttt{\color{crimson}[Insert: \detokenize{#3}]}}"
text = text.replace(old_def, new_def)

# Fix caption {6T\{sch\}} to avoid curly brace mismatch in captions
# In captions, \texttt{6T\{sch\}}: { and } inside \texttt in argument of \caption can confuse parser
text = text.replace(r"\texttt{6T\{sch\}}", r"\texttt{6T(sch)}")
text = text.replace(r"\texttt{6T\{ic\}}", r"\texttt{6T(ic)}")
text = text.replace(r"\texttt{6T\{lay\}}", r"\texttt{6T(lay)}")

text = text.replace(r"\texttt{inverter\{sch\}}", r"\texttt{inverter(sch)}")
text = text.replace(r"\texttt{inverter\{ic\}}", r"\texttt{inverter(ic)}")
text = text.replace(r"\texttt{inverter\{lay\}}", r"\texttt{inverter(lay)}")

text = text.replace(r"\texttt{nand4\{sch\}}", r"\texttt{nand4(sch)}")
text = text.replace(r"\texttt{nand4\{ic\}}", r"\texttt{nand4(ic)}")
text = text.replace(r"\texttt{nand4\{lay\}}", r"\texttt{nand4(lay)}")

text = text.replace(r"\texttt{tgate\{sch\}}", r"\texttt{tgate(sch)}")
text = text.replace(r"\texttt{tgate\{ic\}}", r"\texttt{tgate(ic)}")
text = text.replace(r"\texttt{tgate\{lay\}}", r"\texttt{tgate(lay)}")

text = text.replace(r"\texttt{DFF\{sch\}}", r"\texttt{DFF(sch)}")
text = text.replace(r"\texttt{DFF\{ic\}}", r"\texttt{DFF(ic)}")
text = text.replace(r"\texttt{DFF\{lay\}}", r"\texttt{DFF(lay)}")

text = text.replace(r"\texttt{pre_charge\{sch\}}", r"\texttt{pre\_charge(sch)}")
text = text.replace(r"\texttt{pre_charge\{ic\}}", r"\texttt{pre\_charge(ic)}")
text = text.replace(r"\texttt{pre_charge\{lay\}}", r"\texttt{pre\_charge(lay)}")

text = text.replace(r"\texttt{col_mux_1x\{sch\}}", r"\texttt{col\_mux\_1x(sch)}")
text = text.replace(r"\texttt{col_mux_1x\{ic\}}", r"\texttt{col\_mux\_1x(ic)}")
text = text.replace(r"\texttt{col_mux_1x\{lay\}}", r"\texttt{col\_mux\_1x(lay)}")

text = text.replace(r"\texttt{sense_amp\{sch\}}", r"\texttt{sense\_amp(sch)}")
text = text.replace(r"\texttt{sense_amp\{ic\}}", r"\texttt{sense\_amp(ic)}")
text = text.replace(r"\texttt{sense_amp\{lay\}}", r"\texttt{sense\_amp(lay)}")

text = text.replace(r"\texttt{write_driver\{sch\}}", r"\texttt{write\_driver(sch)}")
text = text.replace(r"\texttt{write_driver\{ic\}}", r"\texttt{write\_driver(ic)}")
text = text.replace(r"\texttt{write_driver\{lay\}}", r"\texttt{write\_driver(lay)}")

text = text.replace(r"\texttt{shiftreg_8bit\{sch\}}", r"\texttt{shiftreg\_8bit(sch)}")
text = text.replace(r"\texttt{shiftreg_8bit\{ic\}}", r"\texttt{shiftreg\_8bit(ic)}")
text = text.replace(r"\texttt{shiftreg_8bit\{lay\}}", r"\texttt{shiftreg\_8bit(lay)}")

text = text.replace(r"\texttt{div8_counter\{sch\}}", r"\texttt{div8\_counter(sch)}")
text = text.replace(r"\texttt{div8_counter\{ic\}}", r"\texttt{div8\_counter(ic)}")
text = text.replace(r"\texttt{div8_counter\{lay\}}", r"\texttt{div8\_counter(lay)}")

text = text.replace(r"\texttt{decoder_4x16\{sch\}}", r"\texttt{decoder\_4x16(sch)}")
text = text.replace(r"\texttt{decoder_4x16\{ic\}}", r"\texttt{decoder\_4x16(ic)}")
text = text.replace(r"\texttt{decoder_4x16\{lay\}}", r"\texttt{decoder\_4x16(lay)}")

text = text.replace(r"\texttt{SRAM_Array_16x16\{lay\}}", r"\texttt{SRAM\_Array\_16x16(lay)}")
text = text.replace(r"\texttt{SRAM_Core_Datapath\{sch\}}", r"\texttt{SRAM\_Core\_Datapath(sch)}")
text = text.replace(r"\texttt{SRAM_Core_Datapath\{lay\}}", r"\texttt{SRAM\_Core\_Datapath(lay)}")
text = text.replace(r"\texttt{SRAM_256_Macro\{sch\}}", r"\texttt{SRAM\_256\_Macro(sch)}")
text = text.replace(r"\texttt{SRAM_256_Macro\{lay\}}", r"\texttt{SRAM\_256\_Macro(lay)}")
text = text.replace(r"\texttt{Input_Pad\{lay\}}", r"\texttt{Input\_Pad(lay)}")
text = text.replace(r"\texttt{Output_Pad\{lay\}}", r"\texttt{Output\_Pad(lay)}")
text = text.replace(r"\texttt{SRAM_Chip_Final\{sch\}}", r"\texttt{SRAM\_Chip\_Final(sch)}")
text = text.replace(r"\texttt{SRAM_Chip_Final\{lay\}}", r"\texttt{SRAM\_Chip\_Final(lay)}")

# Replace raw underscores in cell names
names = [
    "pre_charge", "col_mux_1x", "sense_amplifier", "write_driver",
    "ShiftReg_8bit", "Div8_Counter", "Decoder_4x16", "SRAM_Row_16",
    "SRAM_Array_16x16", "Precharge_Bank_16", "Col_Mux_Bank_16",
    "SRAM_Core_Datapath", "SRAM_256_Macro", "Input_Pad", "Output_Pad",
    "SRAM_Chip_Final", "tb_SRAM_Chip.sp", "SRAM_Chip_Final.spi",
    "pad_CLK", "pad_ADDR_IN", "pad_R_W", "pad_DIN", "pad_DOUT",
    "ext_CLK", "ext_ADDR_IN", "ext_R_W", "ext_DIN", "ext_DOUT",
    "ADDR_IN", "R_W", "PRE_B", "COL_SEL", "COL_SEL_B", "WL_EN",
    "I_write_avg", "I_read_avg", "V_read_bump", "R_on,n", "R_on,p",
    "M_pre_so"
]

for nm in names:
    escaped = nm.replace("_", r"\_")
    text = text.replace(f"\\texttt{{{nm}}}", f"\\texttt{{{escaped}}}")
    # Also replace bare occurrences in text
    # but not inside python code or file names in open()
    text = text.replace(f" {nm} ", f" {escaped} ")
    text = text.replace(f"({nm})", f"({escaped})")
    text = text.replace(f"`{nm}`", f"\\texttt{{{escaped}}}")

with open("generate_report.py", "w") as f:
    f.write(text)

print("Updated generate_report.py!")
