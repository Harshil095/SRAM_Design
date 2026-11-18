with open("BuildFoldedShiftReg.java", "r") as f:
    text = f.read()

text = text.replace('System.out.println("ShiftReg_8bit DRC Errors: " + errLog.getNumErrors());',
'''System.out.println("ShiftReg_8bit DRC Errors: " + errLog.getNumErrors());
                    Map<String, Integer> counts = new HashMap<>();
                    for (Iterator it = errLog.getLogs(); it.hasNext(); ) {
                        ErrorLogger.MessageLog ml = (ErrorLogger.MessageLog) it.next();
                        counts.put(ml.getMessageString(), counts.getOrDefault(ml.getMessageString(), 0) + 1);
                    }
                    for (Map.Entry<String, Integer> e : counts.entrySet()) {
                        System.out.println("  " + e.getValue() + "x : " + e.getKey());
                    }''')

with open("BuildFoldedShiftReg.java", "w") as f:
    f.write(text)
