with open("MakeShiftReg.java", "r") as f:
    text = f.read()

target = '''                    } else if (errLog.getNumErrors() > 0) {
                        Map<String, Integer> counts = new HashMap<>();
                        for (Iterator it = errLog.getLogs(); it.hasNext(); ) {
                            ErrorLogger.MessageLog ml = (ErrorLogger.MessageLog) it.next();
                            counts.put(ml.getMessageString(), counts.getOrDefault(ml.getMessageString(), 0) + 1);
                        }
                        for (Map.Entry<String, Integer> e : counts.entrySet()) {
                            System.out.println("  " + e.getValue() + "x : " + e.getKey());
                        }
                    }'''

replacement = '''                    } else if (errLog.getNumErrors() > 0) {
                        java.lang.reflect.Field fLines = Class.forName("com.sun.electric.tool.user.ErrorHighPoly").getDeclaredField("linesList");
                        fLines.setAccessible(true);
                        java.lang.reflect.Field fP1 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p1");
                        fP1.setAccessible(true);
                        java.lang.reflect.Field fP2 = Class.forName("com.sun.electric.tool.user.ErrorHighLine").getDeclaredField("p2");
                        fP2.setAccessible(true);

                        for (Iterator it = errLog.getLogs(); it.hasNext(); ) {
                            ErrorLogger.MessageLog ml = (ErrorLogger.MessageLog) it.next();
                            System.out.println("ERROR: " + ml.getMessageString());
                            for (Iterator hit = ml.getHighlights(); hit.hasNext(); ) {
                                Object eh = hit.next();
                                if (eh.getClass().getSimpleName().equals("ErrorHighPoly")) {
                                    List lines = (List) fLines.get(eh);
                                    for (Object line : lines) {
                                        System.out.println("   " + fP1.get(line) + " -> " + fP2.get(line));
                                    }
                                }
                            }
                        }
                    }'''

assert target in text
text = text.replace(target, replacement)
with open("MakeShiftReg.java", "w") as f:
    f.write(text)

