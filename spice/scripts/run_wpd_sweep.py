import subprocess

wpd_list = [0.28, 0.36, 0.44, 0.54, 0.65]

template_snm = """* SNM VTC for WPD={wpd}u
.include "../tsmc018.lib"

Vdd vdd 0 DC 1.8
Vwl wl 0 DC 1.8
Vblb blb 0 DC 1.8

V_in1 q_in 0 DC 0
Mpd1 qb_out q_in 0   0   CMOSN W={wpd}u L=0.18u
Mpu1 qb_out q_in vdd vdd CMOSP W=0.28u  L=0.18u
Mpg1 blb    wl   qb_out 0 CMOSN W=0.36u  L=0.18u

.control
dc V_in1 0 1.8 0.005
set wr_vecnames
wrdata vtc_wpd_{idx}.txt v(qb_out)
quit
.endc
.end
"""

template_write_ramp = """* Transient Write Trip for WPD={wpd}u
.include "../tsmc018.lib"
.include "sram_6t.cir"

Vdd vdd 0 DC 1.8
Vwl wl 0 DC 1.8
Vblb blb 0 DC 1.8

Xcell wl bl blb vdd 0 q qb sram_6t WPD={wpd}u WPG=0.36u WPU=0.28u L=0.18u
.ic v(q)=1.8 v(qb)=0

Vbl bl 0 PWL(0ns 1.8V 10ns 0V)

.control
tran 0.01ns 10ns uic
set wr_vecnames
wrdata write_ramp_{idx}.txt v(bl) v(q) v(qb)
quit
.endc
.end
"""

def calc_read_snm(filename):
    with open(filename) as f:
        lines = [line.strip().split() for line in f if line.strip()]
    data = [row for row in lines if row[0] not in ('v-sweep', 'title')]
    vin = [float(row[0]) for row in data]
    vout = [float(row[1]) for row in data]
    
    max_S = 0.0
    for vq0_idx, vq0 in enumerate(vin):
        vqb0 = vout[vq0_idx]
        if vq0 > vqb0:
            for s_idx in range(1, 500):
                S = s_idx * 0.001
                target_vqb = vqb0 + S
                if target_vqb > 1.8:
                    break
                k = int(target_vqb / 0.005)
                if k >= len(vin)-1:
                    break
                frac = (target_vqb - vin[k]) / 0.005
                vq_c2 = vout[k] + frac * (vout[k+1] - vout[k])
                if vq_c2 <= vq0 + S:
                    if S > max_S:
                        max_S = S
                    break
    return max_S

def calc_write_trip(filename):
    with open(filename) as f:
        lines = [line.strip().split() for line in f if line.strip() and line.strip().split()[0] != 'time']
    for row in lines:
        vbl = float(row[1])
        vq = float(row[3])
        vqb = float(row[5])
        if vq < vqb:
            return vbl
    return 0.0

results = []
for idx, wpd in enumerate(wpd_list):
    cr = wpd / 0.36
    # 1. Run SNM
    cir_file = f"sweep_snm_{idx}.cir"
    with open(f"sims/{cir_file}", "w") as f:
        f.write(template_snm.format(wpd=wpd, idx=idx))
    subprocess.run(["ngspice", "-b", cir_file], cwd="sims", capture_output=True)
    snm = calc_read_snm(f"sims/vtc_wpd_{idx}.txt")
    
    # 2. Run Write Ramp
    wr_file = f"sweep_wr_ramp_{idx}.cir"
    with open(f"sims/{wr_file}", "w") as f:
        f.write(template_write_ramp.format(wpd=wpd, idx=idx))
    subprocess.run(["ngspice", "-b", wr_file], cwd="sims", capture_output=True)
    trip_vbl = calc_write_trip(f"sims/write_ramp_{idx}.txt")
    write_pass = "PASS" if trip_vbl > 0.3 else "FAIL"
    
    results.append((wpd, cr, snm, trip_vbl, write_pass))

print("\n### Pull-Down NMOS Width (W_PD) Sweep Results (W_PG = 0.36 um, W_PU = 0.28 um, L = 0.18 um)")
print("| W_PD (um) | Cell Ratio (CR = W_PD / W_PG) | Read SNM (mV) | Write Trip V_BL (V) | Write Status |")
print("| :---: | :---: | :---: | :---: | :---: |")
for r in results:
    print(f"| {r[0]:.2f} um | {r[1]:.2f} | {r[2]*1000:.1f} mV | {r[3]:.3f} V | {r[4]} |")
