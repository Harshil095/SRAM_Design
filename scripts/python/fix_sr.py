with open("BuildFoldedShiftReg.java", "r") as f:
    text = f.read()
text = "import com.sun.electric.database.topology.PortInst;\n" + text
with open("BuildFoldedShiftReg.java", "w") as f:
    f.write(text)
