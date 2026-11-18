import re

with open("BuildRectifiedFinalChip.java", "r") as f:
    text = f.read()

# Replace size 4.0 for via12 with size 5.0
text = text.replace("4.0, 4.0, chipLay", "5.0, 5.0, chipLay")

with open("BuildRectifiedFinalChip.java", "w") as f:
    f.write(text)

