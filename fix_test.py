with open("TestFixChipDRC.java", "r") as f:
    text = f.read()

old_ncc = """                    // Check NCC
                    System.out.println("=== Checking NCC ===");
                    Cell chipSch = lib.findNodeProto("SRAM_Chip_Final{sch}");
                    NccOptions nccOpt = new NccOptions();
                    nccOpt.checkSizes = false;
                    NccResults nr = Ncc.match(chipLay, chipSch, nccOpt);
                    System.out.println("NCC Result: " + nr.isMatch() + " (" + nr.getSummary() + ")");

                    if (chipPeriphCount <= 2 && nr.isMatch()) {
                        lib.exportLibrary(FileType.JELIB, null);
                        System.out.println("Successfully saved SRAM.jelib!");
                    }"""

new_ncc = """                    // Check NCC
                    System.out.println("=== Checking NCC ===");
                    Cell chipSch = lib.findNodeProto("SRAM_Chip_Final{sch}");
                    NccOptions opts = new NccOptions();
                    NccResults res = Ncc.compare(chipSch, null, chipLay, null, opts);
                    System.out.println(">>> NCC Result: " + res + " <<<");

                    if (chipPeriphCount <= 2 && res != null && res.match()) {
                        com.sun.electric.tool.io.output.Output.writeLibrary(lib, FileType.JELIB, false, false, false, 0);
                        System.out.println("Library saved successfully!");
                    }"""

text = text.replace(old_ncc, new_ncc)
with open("TestFixChipDRC.java", "w") as f:
    f.write(text)
