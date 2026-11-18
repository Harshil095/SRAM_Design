import re

user_jelib = "/Users/harshil_rathan/.gemini/antigravity/brain/4b2b74ac-eb63-4490-a996-70074e0522bc/.user_uploaded/media_1795002561961_7f7dd016.jelib"
out_jelib = "SRAM_final.jelib"

with open(user_jelib, 'r') as f:
    content = f.read()

# 1. Update library name in header
content = re.sub(r'^Hmedia_[0-9_]+\|', 'HSRAM_final|', content, flags=re.MULTILINE)
content = re.sub(r'^HSRAM\|', 'HSRAM_final|', content, flags=re.MULTILINE)

# 2. Add technology scale header if not present
if "ScaleFORmocmos" not in content:
    views_pos = content.find("# Views:\n")
    if views_pos != -1:
        next_sec = content.find("\n# ", views_pos + 1)
        tech_block = "\n# Technologies:\nTmocmos|ScaleFORmocmos()D90.0\n"
        if next_sec != -1:
            content = content[:next_sec] + tech_block + content[next_sec:]
        else:
            content += tech_block

# 3. Remove external library dependency
content = re.sub(r'# External Libraries:\s*LSRAM_Complete_V1\|SRAM_Complete_V1\s*', '# External Libraries:\n\n', content)

# 4. In write_driver_16;1{sch}, replace external writing_driver references with local write_driver
content = content.replace("ISRAM_Complete_V1:writing_driver;1{ic}|", "Iwrite_driver;1{ic}|")

# Also replace BL_b with BLB on write_driver instances in write_driver_16;1{sch}
content = content.replace("|BL_b|", "|BLB|")

# 5. Remove orphan pin in 6T;1{lay}: Ngeneric:Invisible-Pin|pin@36||75|45.5||||
content = re.sub(r'Ngeneric:Invisible-Pin\|pin@36\|\|75\|45\.5\|\|\|\|\n', '', content)

# 6. Remove orphan pin in pre_charge;1{lay}: Ngeneric:Invisible-Pin|pin@17||-52|6||||
content = re.sub(r'Ngeneric:Invisible-Pin\|pin@17\|\|-52\|6\|\|\|\|\n', '', content)

with open(out_jelib, 'w') as f:
    f.write(content)

print(f"Generated {out_jelib} successfully, size: {len(content)} bytes")
