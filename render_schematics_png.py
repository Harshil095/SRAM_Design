import zlib
import struct

def create_canvas(w, h, bg=(255, 255, 255)):
    buf = bytearray(w * h * 4)
    for i in range(0, len(buf), 4):
        buf[i] = bg[0]
        buf[i+1] = bg[1]
        buf[i+2] = bg[2]
        buf[i+3] = 255
    return buf

def set_pixel(buf, w, h, x, y, color):
    if 0 <= x < w and 0 <= y < h:
        idx = (y * w + x) * 4
        buf[idx] = color[0]
        buf[idx+1] = color[1]
        buf[idx+2] = color[2]
        buf[idx+3] = color[3] if len(color) > 3 else 255

def draw_rect(buf, w, h, x1, y1, x2, y2, color, fill=None):
    if fill:
        for y in range(max(0, y1), min(h, y2+1)):
            for x in range(max(0, x1), min(w, x2+1)):
                set_pixel(buf, w, h, x, y, fill)
    for x in range(max(0, x1), min(w, x2+1)):
        set_pixel(buf, w, h, x, y1, color)
        set_pixel(buf, w, h, x, y2, color)
    for y in range(max(0, y1), min(h, y2+1)):
        set_pixel(buf, w, h, x1, y, color)
        set_pixel(buf, w, h, x2, y, color)

def draw_thick_line(buf, w, h, x1, y1, x2, y2, color, thickness=3):
    dx = abs(x2 - x1)
    dy = abs(y2 - y1)
    steps = max(dx, dy) * 2
    if steps == 0:
        return
    for i in range(steps + 1):
        x = int(x1 + (x2 - x1) * i / steps)
        y = int(y1 + (y2 - y1) * i / steps)
        t_half = thickness // 2
        for tx in range(-t_half, t_half + 1):
            for ty in range(-t_half, t_half + 1):
                set_pixel(buf, w, h, x + tx, y + ty, color)

# Simple 5x7 bitmap font for rendering labels
FONT = {
    'A': ['01110','10001','10001','11111','10001','10001','10001'],
    'B': ['11110','10001','10001','11110','10001','10001','11110'],
    'C': ['01111','10000','10000','10000','10000','10000','01111'],
    'D': ['11110','10001','10001','10001','10001','10001','11110'],
    'E': ['11111','10000','10000','11110','10000','10000','11111'],
    'F': ['11111','10000','10000','11110','10000','10000','10000'],
    'G': ['01111','10000','10000','10111','10001','10001','01110'],
    'H': ['10001','10001','10001','11111','10001','10001','10001'],
    'I': ['11111','00100','00100','00100','00100','00100','11111'],
    'L': ['10000','10000','10000','10000','10000','10000','11111'],
    'M': ['10001','11011','10101','10001','10001','10001','10001'],
    'N': ['10001','11001','10101','10011','10001','10001','10001'],
    'O': ['01110','10001','10001','10001','10001','10001','01110'],
    'P': ['11110','10001','10001','11110','10000','10000','10000'],
    'R': ['11110','10001','10001','11110','10100','10010','10001'],
    'S': ['01111','10000','10000','01110','00001','00001','11110'],
    'T': ['11111','00100','00100','00100','00100','00100','00100'],
    'U': ['10001','10001','10001','10001','10001','10001','01110'],
    'V': ['10001','10001','10001','10001','10001','01010','00100'],
    'W': ['10001','10001','10001','10101','10101','11011','10001'],
    'X': ['10001','10001','01010','00100','01010','10001','10001'],
    '0': ['01110','10001','10011','10101','11001','10001','01110'],
    '1': ['00100','01100','00100','00100','00100','00100','01110'],
    '2': ['01110','10001','00001','00110','01000','10000','11111'],
    '3': ['11110','00001','00001','01110','00001','00001','11110'],
    '4': ['10001','10001','10001','11111','00001','00001','00001'],
    '5': ['11111','10000','11110','00001','00001','10001','01110'],
    '6': ['01110','10000','11110','10001','10001','10001','01110'],
    '7': ['11111','00001','00010','00100','01000','01000','01000'],
    '8': ['01110','10001','10001','01110','10001','10001','01110'],
    '=': ['00000','11111','00000','11111','00000','00000','00000'],
    ':': ['00000','00100','00000','00000','00100','00000','00000'],
    '-': ['00000','00000','00000','11111','00000','00000','00000'],
    '_': ['00000','00000','00000','00000','00000','00000','11111'],
    '(': ['00010','00100','01000','01000','01000','00100','00010'],
    ')': ['01000','00100','00010','00010','00010','00100','01000'],
    '.': ['00000','00000','00000','00000','00000','00110','00110'],
    '/': ['00001','00010','00100','00100','01000','10000','00000'],
    ' ': ['00000','00000','00000','00000','00000','00000','00000']
}

def draw_text(buf, w, h, x, y, text, color, scale=2):
    curr_x = x
    for ch in text.upper():
        bitmap = FONT.get(ch, FONT[' '])
        for row_idx, row in enumerate(bitmap):
            for col_idx, bit in enumerate(row):
                if bit == '1':
                    for sx in range(scale):
                        for sy in range(scale):
                            set_pixel(buf, w, h, curr_x + col_idx * scale + sx, y + row_idx * scale + sy, color)
        curr_x += 6 * scale

def save_png(filename, buf, w, h):
    raw_scanlines = bytearray()
    for y in range(h):
        raw_scanlines.append(0)
        start = y * w * 4
        raw_scanlines.extend(buf[start:start + w * 4])
    compressed = zlib.compress(raw_scanlines, 9)
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    ihdr = struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)
    png_data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', compressed) + chunk(b'IEND', b'')
    with open(filename, 'wb') as f:
        f.write(png_data)

# 1. Render schematic_tgate.png
W1, H1 = 800, 500
c1 = create_canvas(W1, H1, (248, 250, 252))
draw_rect(c1, W1, H1, 20, 20, W1-20, H1-20, (203, 213, 225), (255, 255, 255))
draw_rect(c1, W1, H1, 40, 35, 360, 75, (30, 41, 59), (30, 41, 59))
draw_text(c1, W1, H1, 55, 47, "TGATE - LEVEL 1", (255, 255, 255), scale=2)
draw_text(c1, W1, H1, 380, 47, "CMOS TRANSMISSION GATE", (100, 116, 139), scale=2)

# IN line
draw_thick_line(c1, W1, H1, 50, 250, 220, 250, (2, 132, 199), 4)
draw_text(c1, W1, H1, 60, 220, "EXPORT: IN", (3, 105, 161), scale=2)

# Branches
draw_thick_line(c1, W1, H1, 220, 250, 220, 150, (2, 132, 199), 4)
draw_thick_line(c1, W1, H1, 220, 150, 320, 150, (2, 132, 199), 4)
draw_thick_line(c1, W1, H1, 220, 250, 220, 350, (2, 132, 199), 4)
draw_thick_line(c1, W1, H1, 220, 350, 320, 350, (2, 132, 199), 4)

# PMOS Box
draw_rect(c1, W1, H1, 320, 105, 520, 195, (220, 38, 38), (254, 242, 242))
draw_text(c1, W1, H1, 340, 120, "PMOS (MP)", (153, 27, 27), scale=2)
draw_text(c1, W1, H1, 340, 145, "W = 12 (1.08 UM)", (185, 28, 28), scale=2)
draw_text(c1, W1, H1, 340, 170, "L = 2 (0.18 UM)", (127, 29, 29), scale=2)

# PMOS Gate (SEL_B)
draw_thick_line(c1, W1, H1, 420, 105, 420, 50, (147, 51, 234), 4)
draw_text(c1, W1, H1, 435, 60, "EXPORT: SEL_B", (126, 34, 206), scale=2)

# PMOS Bulk (VDD)
draw_thick_line(c1, W1, H1, 490, 105, 540, 80, (234, 88, 12), 3)
draw_text(c1, W1, H1, 550, 75, "VDD (BULK)", (194, 65, 12), scale=2)

# NMOS Box
draw_rect(c1, W1, H1, 320, 305, 520, 395, (37, 99, 235), (239, 246, 255))
draw_text(c1, W1, H1, 340, 320, "NMOS (MN)", (30, 64, 175), scale=2)
draw_text(c1, W1, H1, 340, 345, "W = 8 (0.72 UM)", (29, 78, 216), scale=2)
draw_text(c1, W1, H1, 340, 370, "L = 2 (0.18 UM)", (30, 58, 138), scale=2)

# NMOS Gate (SEL)
draw_thick_line(c1, W1, H1, 420, 395, 420, 450, (5, 150, 105), 4)
draw_text(c1, W1, H1, 435, 430, "EXPORT: SEL", (4, 120, 87), scale=2)

# NMOS Bulk (GND)
draw_thick_line(c1, W1, H1, 490, 395, 540, 420, (71, 85, 105), 3)
draw_text(c1, W1, H1, 550, 415, "GND (BULK)", (51, 65, 85), scale=2)

# Output lines recombination
draw_thick_line(c1, W1, H1, 520, 150, 620, 150, (2, 132, 199), 4)
draw_thick_line(c1, W1, H1, 520, 350, 620, 350, (2, 132, 199), 4)
draw_thick_line(c1, W1, H1, 620, 150, 620, 350, (2, 132, 199), 4)
draw_thick_line(c1, W1, H1, 620, 250, 750, 250, (2, 132, 199), 4)
draw_text(c1, W1, H1, 640, 220, "EXPORT: OUT", (3, 105, 161), scale=2)

save_png("docs/schematic_tgate.png", c1, W1, H1)

# 2. Render schematic_col_mux_1x.png
W2, H2 = 900, 600
c2 = create_canvas(W2, H2, (248, 250, 252))
draw_rect(c2, W2, H2, 20, 20, W2-20, H2-20, (203, 213, 225), (255, 255, 255))
draw_rect(c2, W2, H2, 40, 35, 420, 75, (30, 41, 59), (30, 41, 59))
draw_text(c2, W2, H2, 55, 47, "COL_MUX_1X - LEVEL 2", (255, 255, 255), scale=2)
draw_text(c2, W2, H2, 440, 47, "COLUMN SWITCH PAIR", (100, 116, 139), scale=2)

# BL Input & Output
draw_thick_line(c2, W2, H2, 40, 150, 280, 150, (2, 132, 199), 4)
draw_text(c2, W2, H2, 50, 120, "EXPORT: BL", (3, 105, 161), scale=2)

# TGATE BL box
draw_rect(c2, W2, H2, 280, 105, 580, 195, (2, 132, 199), (241, 245, 249))
draw_text(c2, W2, H2, 310, 130, "TGATE (TGATE_BL)", (15, 23, 42), scale=2)
draw_text(c2, W2, H2, 310, 160, "IN:BL -> OUT:GBL", (100, 116, 139), scale=2)

draw_thick_line(c2, W2, H2, 580, 150, 850, 150, (2, 132, 199), 4)
draw_text(c2, W2, H2, 700, 120, "EXPORT: GBL", (3, 105, 161), scale=2)

# BLB Input & Output
draw_thick_line(c2, W2, H2, 40, 450, 280, 450, (2, 132, 199), 4)
draw_text(c2, W2, H2, 50, 420, "EXPORT: BLB", (3, 105, 161), scale=2)

# TGATE BLB box
draw_rect(c2, W2, H2, 280, 405, 580, 495, (2, 132, 199), (241, 245, 249))
draw_text(c2, W2, H2, 310, 430, "TGATE (TGATE_BLB)", (15, 23, 42), scale=2)
draw_text(c2, W2, H2, 310, 460, "IN:BLB -> OUT:GBLB", (100, 116, 139), scale=2)

draw_thick_line(c2, W2, H2, 580, 450, 850, 450, (2, 132, 199), 4)
draw_text(c2, W2, H2, 690, 420, "EXPORT: GBLB", (3, 105, 161), scale=2)

# Inverter in middle
draw_rect(c2, W2, H2, 310, 265, 550, 335, (168, 85, 247), (253, 244, 255))
draw_text(c2, W2, H2, 330, 285, "INVERTER (INV_SEL)", (126, 34, 206), scale=2)
draw_text(c2, W2, H2, 330, 310, "WP=8, WN=4", (147, 51, 234), scale=2)

# SEL line
draw_thick_line(c2, W2, H2, 40, 300, 310, 300, (5, 150, 105), 4)
draw_text(c2, W2, H2, 50, 275, "EXPORT: SEL", (4, 120, 87), scale=2)

# SEL distribution to TGATE SEL pins
draw_thick_line(c2, W2, H2, 200, 300, 200, 185, (5, 150, 105), 3)
draw_thick_line(c2, W2, H2, 200, 185, 280, 185, (5, 150, 105), 3)
draw_text(c2, W2, H2, 210, 170, "SEL", (5, 150, 105), scale=2)

draw_thick_line(c2, W2, H2, 200, 300, 200, 415, (5, 150, 105), 3)
draw_thick_line(c2, W2, H2, 200, 415, 280, 415, (5, 150, 105), 3)
draw_text(c2, W2, H2, 210, 425, "SEL", (5, 150, 105), scale=2)

# Inverter output (SEL_B) distribution to TGATE SEL_B pins
draw_thick_line(c2, W2, H2, 550, 300, 660, 300, (147, 51, 234), 3)
draw_text(c2, W2, H2, 570, 280, "SEL_B", (126, 34, 206), scale=2)

draw_thick_line(c2, W2, H2, 660, 300, 660, 185, (147, 51, 234), 3)
draw_thick_line(c2, W2, H2, 660, 185, 580, 185, (147, 51, 234), 3)
draw_text(c2, W2, H2, 600, 170, "SEL_B", (147, 51, 234), scale=2)

draw_thick_line(c2, W2, H2, 660, 300, 660, 415, (147, 51, 234), 3)
draw_thick_line(c2, W2, H2, 660, 415, 580, 415, (147, 51, 234), 3)
draw_text(c2, W2, H2, 600, 425, "SEL_B", (147, 51, 234), scale=2)

save_png("docs/schematic_col_mux_1x.png", c2, W2, H2)
print("PNG diagrams rendered successfully!")
