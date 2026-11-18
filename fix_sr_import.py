with open("BuildFoldedShiftReg.java", "r") as f:
    text = f.read()
text = "import java.util.*;\n" + text
with open("BuildFoldedShiftReg.java", "w") as f:
    f.write(text)
