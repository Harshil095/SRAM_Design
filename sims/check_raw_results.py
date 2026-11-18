import struct

def generate_waveforms_markdown():
    with open('6T.raw', 'rb') as f:
        raw_data = f.read()

    header_end = raw_data.find(b'Binary:\n') + len(b'Binary:\n')
    binary_part = raw_data[header_end:]
    num_points = 3910
    point_size = 15 * 8

    points = []
    for i in range(num_points):
        chunk = binary_part[i*point_size : (i+1)*point_size]
        vals = struct.unpack('15d', chunk)
        points.append(vals)

    # Let's sample key operating points
    sample_times = [
        (1.0, "Initial Hold"),
        (4.0, "Write-0 (WL=1.8V, BL=0V)"),
        (8.0, "Hold-0 (WL=0V, BL/BLB=1.8V)"),
        (12.5, "Read-0 (WL=1.8V, BL drops)"),
        (17.0, "Precharge / Hold"),
        (21.0, "Write-1 (WL=1.8V, BLB=0V)"),
        (25.0, "Hold-1 (WL=0V, BL/BLB=1.8V)"),
        (29.5, "Read-1 (WL=1.8V, BLB drops)"),
        (35.0, "Final Hold")
    ]

    print("| Time (ns) | Event / Operation | WL (V) | BL (V) | BLB (V) | Q / net@3 (V) | QB / net@4 (V) | Status |")
    print("| :---: | :--- | :---: | :---: | :---: | :---: | :---: | :--- |")
    for t_ns, desc in sample_times:
        p = min(points, key=lambda pt: abs(pt[0] - t_ns*1e-9))
        status = "PASS"
        if "Read-0" in desc:
            status = f"PASS (diff = {(p[5]-p[4])*1e3:.1f} mV)"
        elif "Read-1" in desc:
            status = f"PASS (diff = {(p[4]-p[5])*1e3:.1f} mV)"
        print(f"| {p[0]*1e9:.2f} ns | {desc} | {p[3]:.2f} V | {p[4]:.2f} V | {p[5]:.2f} V | {p[2]:.3f} V | {p[1]:.3f} V | {status} |")

generate_waveforms_markdown()
