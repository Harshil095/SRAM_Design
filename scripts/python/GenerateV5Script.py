import re

with open("BuildCompactedMacroCleanV4.java", "r") as f:
    code = f.read()

# Replace class name
code = code.replace("BuildCompactedMacroCleanV4", "BuildCompactedMacroCleanV5")

with open("BuildCompactedMacroCleanV5.java", "w") as f:
    f.write(code)

print("Created BuildCompactedMacroCleanV5.java template")
