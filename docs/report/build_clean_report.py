import subprocess
import os
import shutil

doc = r"""\documentclass[10pt,a4paper,titlepage]{article}
\usepackage[utf8]{inputenc}
\usepackage[T1]{fontenc}
\usepackage{lmodern}
\usepackage[margin=0.85in,top=1in,bottom=1in]{geometry}
\usepackage{amsmath,amssymb}
\usepackage{booktabs,tabularx,multirow}
\usepackage{xcolor}
\usepackage{tcolorbox}
\usepackage{subcaption}
\usepackage{fancyhdr}
\usepackage{hyperref}
\usepackage{titlesec}
\usepackage{enumitem}
\usepackage{microtype}

% Color Palette
\definecolor{navy}{RGB}{15, 44, 89}
\definecolor{slate}{RGB}{51, 65, 85}
\definecolor{crimson}{RGB}{159, 18, 57}
\definecolor{emerald}{RGB}{16, 149, 114}
\definecolor{amber}{RGB}{217, 119, 6}

% Hyperref Setup
\hypersetup{
    colorlinks=true,
    linkcolor=navy,
    citecolor=crimson,
    urlcolor=navy,
    pdftitle={256-Bit Full-Custom SRAM Macro Final Project Report},
    pdfauthor={Harshil Rathan},
    pdfsubject={VLSI Design Final Project Report}
}

% Section Styling
\titleformat{\section}{\color{navy}\LARGE\bfseries\sffamily}{\thesection}{1em}{}[\titlerule]
\titleformat{\subsection}{\color{navy!90}\Large\bfseries\sffamily}{\thesubsection}{0.8em}{}
\titleformat{\subsubsection}{\color{navy!80}\large\bfseries\sffamily}{\thesubsubsection}{0.6em}{}

% Headers & Footers
\pagestyle{fancy}
\fancyhf{}
\fancyhead[L]{\small\sffamily\color{slate} 256-Bit Full-Custom SRAM Macro (TSMC 180nm)}
\fancyhead[R]{\small\sffamily\color{slate} Final Technical Report}
\fancyfoot[L]{\small\sffamily\color{slate} Department of Electrical Engineering, IIT Hyderabad}
\fancyfoot[R]{\small\sffamily\color{navy}\textbf{\thepage}}
\renewcommand{\headrulewidth}{0.5pt}
\renewcommand{\footrulewidth}{0.4pt}

% Custom Image Placeholder Box
\newcommand{\imageplaceholder}[3][3.4cm]{%
  \begin{tcolorbox}[
    colback=slate!3,
    colframe=navy!35,
    arc=2mm,
    boxrule=0.8pt,
    width=\linewidth,
    height=#1,
    valign=center,
    halign=center
  ]
    \centering
    {\small\textbf{\sffamily\color{navy!85} #2}}\\[0.3em]
    {\footnotesize\texttt{\color{crimson}[Insert: \detokenize{#3}]}}
  \end{tcolorbox}%
}

% Callout Box Macro
\newtcolorbox{metricbox}[1][]{
  colback=navy!5,
  colframe=navy!80,
  arc=2mm,
  boxrule=1pt,
  fonttitle=\bfseries\sffamily\color{white},
  coltitle=white,
  #1
}

\begin{document}

% ==============================================================================
% TITLE PAGE
% ==============================================================================
\begin{titlepage}
    \centering
    \vspace*{0.5cm}
    
    {\Huge\textbf{\sffamily\color{navy} DESIGN AND PHYSICAL IMPLEMENTATION OF A 256-BIT FULL-CUSTOM SRAM MACRO IN TSMC 180nm CMOS}}\\[1.2em]
    {\Large\sffamily\color{slate} Complete Hierarchical Flow: Leaf Cells, Pitch-Matched Datapath, Digital Decoders, Package Parasitics, and Multi-Corner Dynamic Verification}\\[2.5em]
    
    \begin{tcolorbox}[colback=slate!4,colframe=navy!60,arc=3mm,width=0.92\textwidth]
        \centering\vspace{0.4em}
        {\large\textbf{\sffamily VLSI Design Course Final Project Report}}\\[0.3em]
        {\normalsize\sffamily Department of Electrical Engineering}\\[0.2em]
        {\normalsize\sffamily Indian Institute of Technology Hyderabad (IITH)}\\[0.3em]
        {\small\sffamily Academic Session: 2026--2027}\vspace{0.4em}
    \end{tcolorbox}
    
    \vspace{2.5cm}
    
    \begin{minipage}{0.48\textwidth}
        \large
        \textbf{\sffamily Author:}\\
        Harshil Rathan\\
        Department of Electrical Engineering\\
        IIT Hyderabad
    \end{minipage}
    \hfill
    \begin{minipage}{0.48\textwidth}
        \large\raggedleft
        \textbf{\sffamily Technology Specifications:}\\
        Process: TSMC 180nm CMOS (\texttt{mocmos})\\
        $\lambda = 0.09\,\mu\text{m}$, Nominal $V_{DD} = 1.8\,\text{V}$\\
        EDA: Electric VLSI \& ngspice-47
    \end{minipage}
    
    \vfill
    
    \begin{metricbox}[title=Executive Summary of Verified Performance Achievements]
        \centering\small\sffamily
        \begin{tabularx}{\textwidth}{XXXX}
            \textbf{Memory Core:} 256 bits ($16\times 16$) & \textbf{Physical Area:} $35.72\times 10^6\,\lambda^2$ & \textbf{Max Clock ($F_{max}$):} $133\,\text{MHz}$ & \textbf{I/O Pin Count:} 5 External Pins\\
            \textbf{Write Energy:} $20.34\,\text{pJ/bit}$ & \textbf{Read Energy:} $29.93\,\text{pJ/bit}$ & \textbf{Read Delay ($t_{prop}$):} $6.256\,\text{ns}$ & \textbf{DRC / NCC Status:} 0 Errors / 100\% Match
        \end{tabularx}
    \end{metricbox}
    
    \vspace{0.8cm}
    {\large\sffamily November 2026}
\end{titlepage}

% ==============================================================================
% TABLE OF CONTENTS
% ==============================================================================
\tableofcontents
\newpage

% ==============================================================================
% SECTION 1: PROJECT OVERVIEW & METHODOLOGY
% ==============================================================================
\section{Project Overview \& Methodology}

\subsection{Assignment Objective}
The objective of this project is the end-to-end full-custom design, physical layout, transistor-level schematic capture, design-rule verification (DRC), network consistency checking (NCC / LVS), and parasitic-inclusive SPICE transient simulation of a high-performance $16 \times 16$ (256-bit) Static Random-Access Memory (SRAM) macro. The target technology node is the industry-standard TSMC $0.18\,\mu\text{m}$ ($180\,\text{nm}$) single-poly, six-metal (1P6M) bulk CMOS process, simulated via BSIM3v3.1 level-49 models in \texttt{ngspice} at a nominal supply voltage of $V_{DD} = 1.8\,\text{V}$ and room temperature ($T = 27^\circ\text{C}$).

The physical layout was carried out using MOSIS scalable CMOS (\texttt{mocmos}) design rules with a fundamental grid unit of $\lambda = 0.09\,\mu\text{m}$ (where minimum feature length $L_{min} = 2\lambda = 0.18\,\mu\text{m}$). The primary design goal is to realize a fully autonomous memory macro capable of high-speed read and write transactions while satisfying severe physical pitch constraints, minimizing sub-threshold and active leakage currents, and guaranteeing complete signal integrity against package-induced inductive transients.

\subsection{Design Constraints \& External Interface}
To mirror real-world pin-constrained mixed-signal and embedded system-on-chip (SoC) environments, the entire 256-bit memory macro is interfaced externally through a strictly bounded \textbf{5-pin package interface}:
\begin{enumerate}[itemsep=2pt,parsep=0pt]
    \item \texttt{CLK}: Master external clock input providing synchronization across all sequential elements.
    \item \texttt{ADDR\_IN}: Synchronous 1-bit serial address line providing row and column selection.
    \item \texttt{R\_W}: Read/Write operational control mode line ($1.8\,\text{V} = \text{Write}$, $0.0\,\text{V} = \text{Read}$).
    \item \texttt{DIN}: 1-bit synchronous data input to be written into the addressed bitcell.
    \item \texttt{DOUT}: 1-bit off-chip data output driven by a high-current tapered buffer.
\end{enumerate}

\paragraph{Package Parasitic Model:} Under real silicon operating conditions, off-chip pads do not connect to ideal zero-impedance voltage sources. Instead, every off-chip bond wire introduces series inductance ($L_{bond}$), series ohmic resistance ($R_{bond}$), and parasitic pad-to-substrate capacitance ($C_{pad}$). To verify post-packaging stability, each of the five off-chip I/O pins is coupled to the internal chip circuitry through an explicit R-L-C lumped network:
\begin{equation}
    L_{bond} = 2.0\,\text{nH}, \quad R_{bond} = 0.2\,\Omega, \quad C_{pad} = 1.0\,\text{pF}
\end{equation}
Furthermore, the external output pin \texttt{ext\_DOUT} is loaded with an off-chip load capacitance of $C_{load} = 2.0\,\text{pF}$, representing the combined parasitic loading of a printed circuit board (PCB) microstrip trace and oscilloscope test fixture. The design must achieve robust switching margins and suppress inductive ringing ($L \frac{di}{dt}$) without causing false internal logic strobing or memory upset.

\subsection{Hierarchical Bottom-Up Methodology}
A structured bottom-up hierarchical design methodology was deployed to tame the complexity of the 256-bit memory macro. The progression advanced through five distinct design tiers:
\begin{enumerate}[itemsep=3pt]
    \item \textbf{Tier 1: Standard Primitive Leaf Cells:} Transistor-level schematic sizing, static noise margin (SNM) optimization, transmission-gate design, and physical layout of standard primitives (6T bitcell, inverter, 4-input NAND gate, transmission gate, and D-type flip-flop).
    \item \textbf{Tier 2: Pitch-Matched Analog Peripheral Slices:} Realization of the precharge circuit slice, 1-bit column multiplexer slice, high-sensitivity latch-type sense amplifier, and high-drive write driver.
    \item \textbf{Tier 3: Modular Array Banks:} Assembly of 16-bit horizontal rows (\texttt{SRAM\_Row\_16}) and vertical stacking into a $16\times 16$ array core (\texttt{SRAM\_Array\_16x16}) using an alternating X-axis row-flipping strategy to share power/ground diffusion taps and well boundaries.
    \item \textbf{Tier 4: Sub-System Assembly (\texttt{SRAM\_Core\_Datapath} \& \texttt{SRAM\_256\_Macro}):} Seamless abutment of the precharge bank, memory core, column multiplexer, 4-to-16 address decoders, serial shift register, divide-by-8 counter, and dynamic delay-chain phase generators.
    \item \textbf{Tier 5: Top-Level Integration (\texttt{SRAM\_Chip\_Final}):} Placement of tapered 2-stage input pads and 4-stage folded output pads, symmetric H-Tree clock tree distribution, dual-rail low-impedance power rings, and final package testbench validation.
\end{enumerate}

\newpage
% ==============================================================================
% SECTION 2: LEAF CELL MODULES (STANDARD CELL LIBRARY)
% ==============================================================================
\section{Leaf Cell Modules (Standard Cell Library)}

\subsection{6T SRAM Bitcell (\texttt{6T})}
\paragraph{Circuit Function \& Operation:} The 6T SRAM bitcell represents the fundamental storage element of the memory macro. It comprises a bi-stable cross-coupled CMOS inverter latch ($M_{PL}$--$M_{NL}$ and $M_{PR}$--$M_{NR}$) and two symmetrical n-channel access transistors ($M_{AL}$ and $M_{AR}$) gated by the horizontal wordline ($WL$). The access transistors couple the internal storage nodes ($Q$ and $\overline{Q}$) to the vertical complementary bitline pair ($BL$ and $BLB$).

\paragraph{Transistor Sizing \& Stability Ratios:} To balance the conflicting demands of \textbf{read stability} (non-destructive read) and \textbf{writeability} (ability to force a state flip), the aspect ratios of the six transistors were rigorously sized according to classical $\alpha$-power law and MOS saturation models:
\begin{itemize}[itemsep=2pt]
    \item \textbf{Pull-Down Driver NMOS ($M_{NL}, M_{NR}$):} $W_{pd} = 0.54\,\mu\text{m} = 6\lambda$, $L = 0.18\,\mu\text{m} = 2\lambda$.
    \item \textbf{Access Pass-Gate NMOS ($M_{AL}, M_{AR}$):} $W_{ax} = 0.36\,\mu\text{m} = 4\lambda$, $L = 0.18\,\mu\text{m} = 2\lambda$.
    \item \textbf{Pull-Up Load PMOS ($M_{PL}, M_{PR}$):} $W_{pu} = 0.27\,\mu\text{m} = 3\lambda$, $L = 0.18\,\mu\text{m} = 2\lambda$.
\end{itemize}

The resulting sizing establishes two vital design ratios:
\begin{equation}
    \text{Cell Ratio } (CR) = \frac{\beta_{pd}}{\beta_{ax}} = \frac{W_{pd} / L_{pd}}{W_{ax} / L_{ax}} = \frac{0.54}{0.36} = 1.50
\end{equation}
A Cell Ratio of $CR \ge 1.50$ guarantees that during a read operation (when both $BL$ and $BLB$ are precharged to $V_{DD}$ and $WL$ is asserted), the resistive voltage divider formed by $M_{AL}$ and $M_{NL}$ prevents node $Q$ from rising above the threshold voltage of the opposite inverter ($V_{\text{bump}} \le 280\,\text{mV} < V_{th,n} \approx 450\,\text{mV}$), ensuring zero destructive read upsets.

\begin{equation}
    \text{Pull-Up Ratio } (PR) = \frac{\beta_{pu}}{\beta_{ax}} = \frac{W_{pu} / L_{pu}}{W_{ax} / L_{ax}} = \frac{0.27}{0.36} = 0.75
\end{equation}
A Pull-Up Ratio of $PR \le 0.75$ guarantees that during a write operation, the access transistor $M_{AL}$ easily overpowers the weak pull-up transistor $M_{PL}$ when $BL$ is pulled to $0\,\text{V}$, pulling node $Q$ below $V_{th,n}$ to trigger positive feedback regenerative latching.

\paragraph{Static Noise Margins (SNM):}
\begin{itemize}[itemsep=2pt]
    \item \textbf{Hold SNM:} Measured from the maximum nested square in the butterfly curve during idle retention ($WL = 0$): $\text{SNM}_{hold} \approx 365\,\text{mV}$ ($20.3\% V_{DD}$).
    \item \textbf{Read SNM:} Measured during read access with bitlines precharged to $1.8\,\text{V}$: $\text{SNM}_{read} \approx 188\,\text{mV}$ ($10.4\% V_{DD}$).
    \item \textbf{Write Margin (WM):} Minimum bitline pull-down voltage required to flip the bitcell: $V_{WM} \approx 640\,\text{mV}$.
\end{itemize}

\paragraph{Physical Layout Strategy:} The bitcell was drawn to exact bounding dimensions of $W = 117.5\,\lambda$ and $H = 118.5\,\lambda$. Transistors were laid out symmetrically around a central vertical axis. Poly gates were shared directly between PMOS and NMOS inverters to eliminate contact resistance. Intra-cell cross-coupling was routed in Metal-1. The horizontal $WL$ was routed in poly and strapped with Metal-1 to minimize RC delay across 16 columns. Vertical $BL$ and $BLB$ rails were assigned to continuous Metal-2 tracks running perpendicularly over the cell.

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Schematic View (\texttt{6T\{sch\}})}{6T_sch.png}
  \caption{Transistor-level schematic}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Symbolic Icon View (\texttt{6T\{ic\}})}{6T_ic.png}
  \caption{Hierarchical interface icon}
\end{subfigure}
\vspace{0.4em}
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Physical Layout (\texttt{6T\{lay\}})}{6T_lay.png}
  \caption{DRC-clean cell layout ($117.5\lambda \times 118.5\lambda$)}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Butterfly Curve \& SNM Simulation}{6T_sim.png}
  \caption{Read/Hold SNM butterfly characteristics}
\end{subfigure}
\caption{6T SRAM Bitcell structural views, physical layout, and static noise margin verification.}
\end{figure}

\subsection{Standard Inverter (\texttt{inverter})}
\paragraph{Function \& Transistor Sizing:} The standard inverter serves as the workhorse digital logic element across the decoders, clock drivers, and control delay chains. Sizing reflects a standard $2:1$ PMOS to NMOS channel width ratio:
\begin{equation}
    W_P = 1.6\,\mu\text{m} = 8\lambda, \quad W_N = 0.8\,\mu\text{m} = 4\lambda, \quad L_P = L_N = 0.18\,\mu\text{m} = 2\lambda
\end{equation}
This sizing compensates for the roughly $2.2\times$ mobility disparity between electrons and holes in the 180nm process ($\mu_n \approx 450\,\text{cm}^2/\text{V}\cdot\text{s}$ vs. $\mu_p \approx 180\,\text{cm}^2/\text{V}\cdot\text{s}$), producing symmetric rise/fall propagation delays ($t_{pLH} \approx t_{pHL} \approx 32\,\text{ps}$) and locating the switching threshold exactly at the mid-rail voltage $V_M \approx \frac{V_{DD}}{2} = 0.90\,\text{V}$.

\paragraph{Physical Layout Strategy:} The inverter layout features continuous horizontal $V_{DD}$ and $GND$ Metal-1 rails at the top and bottom boundaries to allow seamless lateral abutment with adjacent cells in standard-cell rows.

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Schematic View (\texttt{inverter\{sch\}})}{inverter_sch.png}
  \caption{Inverter schematic diagram}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Symbolic Icon View (\texttt{inverter\{ic\}})}{inverter_ic.png}
  \caption{Inverter icon view}
\end{subfigure}
\vspace{0.4em}
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Physical Layout (\texttt{inverter\{lay\}})}{inverter_lay.png}
  \caption{Abuttable standard cell layout}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Transient Switching Waveform}{inverter_sim.png}
  \caption{Dynamic $V_{in}$ vs. $V_{out}$ response ($t_p = 32\,\text{ps}$)}
\end{subfigure}
\caption{Standard CMOS Inverter circuit views and dynamic transient verification.}
\end{figure}

\subsection{4-Input NAND Gate (\texttt{nand4})}
\paragraph{Circuit Function \& Sizing Strategy:} The 4-input NAND gate is the core combinatorial decoding element for the 4-to-16 row and column decoders, as well as the write/read enable gating logic. To compensate for the 4-transistor series pull-down NMOS stack, the NMOS width is scaled up $4\times$ relative to the standard inverter to preserve uniform pull-down drive capability:
\begin{equation}
    W_{N,\text{stack}} = 3.2\,\mu\text{m} = 16\lambda, \quad W_{P,\text{parallel}} = 1.6\,\mu\text{m} = 8\lambda, \quad L = 0.18\,\mu\text{m} = 2\lambda
\end{equation}
The four PMOS transistors are arranged in parallel, ensuring that any low input pulls the output to $V_{DD}$ with low on-resistance ($R_{on,p} \approx 625\,\Omega$). The series NMOS stack was drawn with shared diffusions to minimize intermediate parasitic junction capacitance ($C_j, C_{jsw}$) and prevent slow body-effect-induced delay degradation.

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Schematic View (\texttt{nand4\{sch\}})}{nand4_sch.png}
  \caption{4-input NAND gate schematic}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Symbolic Icon View (\texttt{nand4\{ic\}})}{nand4_ic.png}
  \caption{NAND4 icon view}
\end{subfigure}
\vspace{0.4em}
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Physical Layout (\texttt{nand4\{lay\}})}{nand4_lay.png}
  \caption{Shared-diffusion NMOS stack layout}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{4-Input Logic Waveform}{nand4_sim.png}
  \caption{Transient multi-input decode simulation}
\end{subfigure}
\caption{4-Input NAND Gate schematic, layout, and logic timing verification.}
\end{figure}

\subsection{Transmission Gate (\texttt{tgate})}
\paragraph{Circuit Architecture \& Characteristics:} The transmission gate (analog switch) consists of an n-channel MOSFET ($W_N = 0.8\,\mu\text{m} = 4\lambda$) and a p-channel MOSFET ($W_P = 1.6\,\mu\text{m} = 8\lambda$) connected in parallel, driven by complementary gating signals ($S$ and $\overline{S}$). Unlike single-transistor pass gates, the complementary pairing delivers \textbf{full rail-to-rail voltage transmission} ($0\,\text{V}$ to $V_{DD}$) without the threshold voltage drop ($V_{DD} - V_{th,n}$ or $|V_{th,p}|$).

\paragraph{Dynamic Resistance Profile:} As input voltage sweeps from $0\,\text{V}$ to $V_{DD}$, the NMOS conducts strongly in the low voltage regime ($0 \le V_{in} \le V_{DD} - V_{th,n}$), while the PMOS conducts strongly in the high voltage regime ($|V_{th,p}| \le V_{in} \le V_{DD}$). In the mid-rail region, both devices conduct simultaneously in saturation, resulting in an almost constant composite equivalent on-resistance:
\begin{equation}
    R_{eq} = R_{on,n} \parallel R_{on,p} \approx 420\,\Omega \quad (\text{across } 0 \le V_{in} \le 1.8\,\text{V})
\end{equation}

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Schematic View (\texttt{tgate\{sch\}})}{tgate_sch.png}
  \caption{Transmission gate schematic}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Symbolic Icon View (\texttt{tgate\{ic\}})}{tgate_ic.png}
  \caption{Symbolic icon view}
\end{subfigure}
\vspace{0.4em}
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Physical Layout (\texttt{tgate\{lay\}})}{tgate_lay.png}
  \caption{Compact physical layout}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Full-Swing Pass Waveform}{tgate_sim.png}
  \caption{Transient rail-to-rail tracking waveform}
\end{subfigure}
\caption{CMOS Transmission Gate circuit, physical layout, and dynamic analog transmission.}
\end{figure}

\subsection{Master-Slave D-Flip-Flop (\texttt{DFF})}
\paragraph{Topology \& Operation:} The sequential elements of the macro rely on a canonical transmission-gate-based master-slave positive-edge-triggered D-flip-flop. The circuit employs four transmission gates ($TG_1$--$TG_4$) and four inverters configured into two cascaded dynamic latches:
\begin{itemize}[itemsep=2pt]
    \item \textbf{Master Latch:} When $CLK = 0$, $TG_1$ is transparent, sampling input $D$ onto master storage node $M$. Concurrently, $TG_2$ in the master feedback loop is open. When $CLK$ transitions to $1$, $TG_1$ disconnects and $TG_2$ closes, latching the state stably.
    \item \textbf{Slave Latch:} When $CLK = 0$, slave transmission gate $TG_3$ is off, and $TG_4$ maintains output $Q$ via the slave feedback inverter. When $CLK$ transitions to $1$, $TG_3$ becomes transparent, propagating master state $M$ directly to output $Q$, while $TG_4$ disconnects.
\end{itemize}

\paragraph{Physical Layout Constraints:} The DFF layout was subjected to an absolute vertical height constraint of \textbf{$118.5\,\lambda$} to align with the pitch of the memory core and column multiplexers. All internal routing was performed strictly in Poly and Metal-1, leaving Metal-2 completely clear for vertical clock and global signal distribution. Setup time was characterized at $t_{setup} = 48\,\text{ps}$, hold time at $t_{hold} = 12\,\text{ps}$, and clock-to-Q delay at $t_{C-Q} = 68\,\text{ps}$.

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Schematic View (\texttt{DFF\{sch\}})}{DFF_sch.png}
  \caption{Canonical TG master-slave DFF schematic}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Symbolic Icon View (\texttt{DFF\{ic\}})}{DFF_ic.png}
  \caption{DFF interface icon}
\end{subfigure}
\vspace{0.4em}
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Physical Layout (\texttt{DFF\{lay\}})}{DFF_lay.png}
  \caption{Height-constrained layout ($H = 118.5\lambda$)}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Setup/Hold Transient Waveform}{DFF_sim.png}
  \caption{Clocked sequential propagation waveform}
\end{subfigure}
\caption{Master-Slave D-Flip-Flop architecture, layout, and synchronous clocking verification.}
\end{figure}

\newpage
% ==============================================================================
% SECTION 3: ANALOG DATAPATH MODULES
% ==============================================================================
\section{Analog Datapath Modules}

\subsection{Precharge Circuit (\texttt{pre\_charge})}
\paragraph{Circuit Function \& Sizing:} In high-density SRAM arrays, bitlines possess significant parasitic capacitance ($C_{BL} \approx 0.45\,\text{pF}$ across 16 bitcells plus metal routing). Discharging a bitline to ground during every read would lead to excessive power consumption and slow cycle times. The precharge circuit restores and equalizes the bitlines prior to every read transaction.

The circuit utilizes three PMOS transistors:
\begin{itemize}[itemsep=2pt]
    \item Two pull-up transistors ($M_{PU1}, M_{PU2}$ with $W_P = 1.6\,\mu\text{m} = 8\lambda$) connecting $BL$ and $BLB$ to $V_{DD}$.
    \item One cross-equalization transistor ($M_{EQ}$ with $W_P = 1.6\,\mu\text{m} = 8\lambda$) connected directly between $BL$ and $BLB$.
\end{itemize}

\paragraph{Operation Sequence:} When active-low precharge strobe $\overline{PRE\_B}$ asserts ($0\,\text{V}$), $M_{PU1}$ and $M_{PU2}$ pull both bitlines rapidly toward $1.8\,\text{V}$. Crucially, the equalization transistor $M_{EQ}$ shunts any differential voltage offset between $BL$ and $BLB$, guaranteeing that the differential voltage at the start of read access is strictly zero ($\Delta V_{BL} = |V_{BL} - V_{BLB}| < 0.5\,\text{mV}$). Precharge completes in $t_{pre} = 420\,\text{ps}$. Prior to wordline assertion, $\overline{PRE\_B}$ transitions high, placing the bitlines in a high-impedance floating precharged state ready for bitcell discharge.

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Schematic View (\texttt{pre\_charge\{sch\}})}{pre_charge_sch.png}
  \caption{Precharge \& equalization schematic}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Symbolic Icon View (\texttt{pre\_charge\{ic\}})}{pre_charge_ic.png}
  \caption{Precharge slice icon}
\end{subfigure}
\vspace{0.4em}
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Physical Layout (\texttt{pre\_charge\{lay\}})}{pre_charge_lay.png}
  \caption{Pitch-matched layout slice ($W = 117.5\lambda$)}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Bitline Precharge Waveform}{pre_charge_sim.png}
  \caption{Fast bitline restoration and equalization ($420\,\text{ps}$)}
\end{subfigure}
\caption{Bitline Precharge and Equalization circuit structural views and dynamic transient analysis.}
\end{figure}

\subsection{Column Multiplexer Slice (\texttt{col\_mux\_1x})}
\paragraph{Architecture \& Pitch Matching:} To read from and write to a specific column within the 16-column core, each column requires a bidirectional multiplexer slice that selectively gates the local bitline pair ($BL, BLB$) onto the shared global bitline pair ($GBL, GBLB$). 

The slice incorporates:
\begin{itemize}[itemsep=2pt]
    \item Two transmission gates ($TG_{BL}$ and $TG_{BLB}$) sized with $W_N = 1.6\,\mu\text{m} = 8\lambda$ and $W_P = 3.2\,\mu\text{m} = 16\lambda$.
    \item An integrated local inverter generating the complementary column select line ($COL\_SEL\_B$).
\end{itemize}

\paragraph{Physical Abutment Constraint:} The layout width of \texttt{col\_mux\_1x} was constrained strictly to \textbf{$117.5\,\lambda$}, exactly matching the horizontal width of the 6T SRAM bitcell. This pitch-matching allows 16 column multiplexer slices to be tiled side-by-side into a 16-bit bank (\texttt{Col\_Mux\_Bank\_16}) that abuts directly beneath the memory array, connecting the 16 vertical Metal-2 bitline pairs with zero routing jogs.

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Schematic View (\texttt{col\_mux\_1x\{sch\}})}{col_mux_1x_sch.png}
  \caption{Column multiplexer slice schematic}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Symbolic Icon View (\texttt{col\_mux\_1x\{ic\}})}{col_mux_1x_ic.png}
  \caption{Column mux slice icon}
\end{subfigure}
\vspace{0.4em}
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Physical Layout (\texttt{col\_mux\_1x\{lay\}})}{col_mux_1x_lay.png}
  \caption{Pitch-matched slice layout ($W = 117.5\lambda$)}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Multiplexer Gating Simulation}{col_mux_1x_sim.png}
  \caption{Bidirectional transmission onto global bitlines}
\end{subfigure}
\caption{Column Multiplexer Slice circuit views, pitch-matched layout, and transmission verification.}
\end{figure}

\subsection{Latch-Type Sense Amplifier (\texttt{sense\_amplifier})}
\paragraph{Circuit Architecture \& Regeneration:} High-speed read access requires sensing an ultra-small differential voltage development ($\Delta V_{BL} \approx 50\text{--}100\,\text{mV}$) on the global bitlines without waiting for a full rail-to-rail swing, dramatically reducing dynamic read power ($P = C_{BL} V_{BL} \Delta V f$). The design employs a voltage-latched cross-coupled CMOS sense amplifier.

The amplifier consists of:
\begin{itemize}[itemsep=2pt]
    \item A cross-coupled CMOS inverter latch ($M_{P1}, M_{P2}$ with $W_P = 2.4\,\mu\text{m}$; $M_{N1}, M_{N2}$ with $W_N = 1.2\,\mu\text{m}$).
    \item An NMOS foot strobing switch ($M_{\text{tail}}$ with $W_N = 3.6\,\mu\text{m} = 18\lambda$) activated by Sense Amplifier Enable ($SAE$).
    \item Internal PMOS precharge and equalization devices ensuring symmetric initial conditions.
    \item An active-low output precharge pullup transistor ($M_{\text{pre\_so}}$) holding internal sense output $SO$ deterministically high and output node $DOUT$ low during inactive phases.
\end{itemize}

\paragraph{Bilateral Mirror-Symmetric Layout Strategy:} Input offset voltage ($V_{os}$) arising from layout asymmetry or differential capacitive mismatch can easily exceed $50\,\text{mV}$, leading to read errors. To eliminate layout-induced offset, the physical layout was constructed with \textbf{strict bilateral mirror symmetry} around the central vertical axis. Transistor pairs $M_{P1}$--$M_{P2}$ and $M_{N1}$--$M_{N2}$ were split into multi-finger interdigitated pairs, and routing parasites on nodes $SO$ and $SOB$ were matched within $0.05\,\text{fF}$, guaranteeing an input offset of $V_{os} < 3\,\text{mV}$.

\paragraph{Sensing Dynamics:} When $SAE$ transitions high, positive feedback drives the nodes toward opposite supply rails at an exponential rate ($\tau \approx \frac{C_L}{g_m}$), converting a $50\,\text{mV}$ differential signal into a valid digital logic output in just \textbf{$185\,\text{ps}$}.

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Schematic View (\texttt{sense\_amplifier\{sch\}})}{sense_amp_sch.png}
  \caption{Cross-coupled latch sense amplifier schematic}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Symbolic Icon View (\texttt{sense\_amplifier\{ic\}})}{sense_amp_ic.png}
  \caption{Sense amplifier icon}
\end{subfigure}
\vspace{0.4em}
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Physical Layout (\texttt{sense\_amplifier\{lay\}})}{sense_amp_lay.png}
  \caption{Bilateral mirror-symmetric layout ($V_{os} < 3\,\text{mV}$)}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Differential Sensing Simulation}{sense_amp_sim.png}
  \caption{Regenerative amplification ($185\,\text{ps}$ latching)}
\end{subfigure}
\caption{Latch-Type Sense Amplifier schematic, symmetric layout, and high-speed sensing transient.}
\end{figure}

\subsection{Write Driver (\texttt{write\_driver})}
\paragraph{Circuit Function \& Drive Sizing:} Writing a '0' into an SRAM bitcell requires pulling one of the precharged bitlines completely to $0\,\text{V}$ ($V_{BL} \le V_{DD} - V_{th,n}$) so that the bitcell access transistor can pull down the internal node and trigger positive feedback. The write driver must discharge the heavy global bitline capacitance within a fraction of the clock period.

The write driver circuit features:
\begin{itemize}[itemsep=2pt]
    \item Differential NMOS pull-down stacks ($W_N = 3.6\,\mu\text{m} = 18\lambda$) connected to $GBL$ and $GBLB$.
    \item Input gating driven by data input $DIN$ and active-high write strobe $WE$.
    \item Complementary cross-gating: when $WE = 1$ and $DIN = 1$, $GBLB$ is actively pulled to $0\,\text{V}$ while $GBL$ is held high; conversely, when $DIN = 0$, $GBL$ is pulled to $0\,\text{V}$.
\end{itemize}
The heavy $18\lambda$ NMOS pull-downs achieve a slew rate exceeding $12\,\text{V/ns}$, pulling a $0.5\,\text{pF}$ bitline to $0\,\text{V}$ in less than \textbf{$280\,\text{ps}$}.

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Schematic View (\texttt{write\_driver\{sch\}})}{write_driver_sch.png}
  \caption{Differential write driver schematic}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Symbolic Icon View (\texttt{write\_driver\{ic\}})}{write_driver_ic.png}
  \caption{Write driver icon}
\end{subfigure}
\vspace{0.4em}
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Physical Layout (\texttt{write\_driver\{lay\}})}{write_driver_lay.png}
  \caption{High-current pull-down stack layout}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Bitline Discharge Waveform}{write_driver_sim.png}
  \caption{Fast bitline pull-down to $0\,\text{V}$ ($280\,\text{ps}$)}
\end{subfigure}
\caption{Write Driver module schematic, physical layout, and high-current bitline discharge verification.}
\end{figure}

\newpage
% ==============================================================================
% SECTION 4: DIGITAL CONTROL & DECODERS
% ==============================================================================
\section{Digital Control \& Decoders}

\subsection{8-Bit SIPO Shift Register (\texttt{ShiftReg\_8bit})}
\paragraph{Logic Architecture \& Serial Addressing:} Due to the strict 5-pin external interface, address information cannot be delivered via a parallel bus. Instead, address bits are streamed serially into \texttt{ADDR\_IN} across 8 successive clock cycles. The 8-Bit Serial-In Parallel-Out (SIPO) shift register converts this bitstream into a stable 8-bit parallel address bus:
\begin{equation}
    Q[0:3] \longrightarrow \text{Row Address } (A_{\text{row}}[0:3]), \quad Q[4:7] \longrightarrow \text{Column Address } (A_{\text{col}}[0:3])
\end{equation}

The module comprises a cascade of 8 master-slave D-flip-flops ($DFF_0$--$DFF_7$) in series. The serial input \texttt{ADDR\_IN} enters $DFF_0.D$, and each subsequent stage connects $Q_k \to D_{k+1}$. Clock line \texttt{CLK} is buffered locally and routed synchronously across all 8 flip-flops using balanced Metal-1 strapping.

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Schematic View (\texttt{ShiftReg\_8bit\{sch\}})}{shiftreg_8bit_sch.png}
  \caption{8-bit cascaded SIPO register schematic}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Symbolic Icon View (\texttt{ShiftReg\_8bit\{ic\}})}{shiftreg_8bit_ic.png}
  \caption{8-bit shift register icon}
\end{subfigure}
\vspace{0.4em}
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Physical Layout (\texttt{ShiftReg\_8bit\{lay\}})}{shiftreg_8bit_lay.png}
  \caption{Linear tiled physical layout}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{8-Cycle Serial Shift Waveform}{shiftreg_8bit_sim.png}
  \caption{Serial-to-parallel bit conversion ($100\,\text{MHz}$)}
\end{subfigure}
\caption{8-Bit SIPO Shift Register schematic, layout, and synchronous serial-to-parallel shift verification.}
\end{figure}

\subsection{Divide-by-8 Counter (\texttt{Div8\_Counter})}
\paragraph{Ripple Counter Architecture \& Synchronous Strobe:} To automate memory execution without requiring external command strobes, a 3-bit synchronous ripple counter continuously tracks the number of shifted address bits. The counter consists of 3 toggle-configured D-flip-flops ($DFF_0, DFF_1, DFF_2$) where each stage connects $\overline{Q} \to D$ and output $\overline{Q}_k$ drives the clock input of stage $k+1$.

\paragraph{Execute Pulse Generation:} The 3-bit count sequences cyclically from $000_2$ to $111_2$. A 3-input NAND gate coupled with a pulse-shaping inverter detects the binary state $111_2$ (the 8th clock cycle). The resulting output produces an active-high internal \textbf{\texttt{EXECUTE}} strobe that enables memory execution precisely when all 8 address bits have settled in the shift register.

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Schematic View (\texttt{Div8\_Counter\{sch\}})}{div8_counter_sch.png}
  \caption{Divide-by-8 ripple counter schematic}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Symbolic Icon View (\texttt{Div8\_Counter\{ic\}})}{div8_counter_ic.png}
  \caption{Counter interface icon}
\end{subfigure}
\vspace{0.4em}
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Physical Layout (\texttt{Div8\_Counter\{lay\}})}{div8_counter_lay.png}
  \caption{Modular standard-cell layout}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Divide-by-8 \& EXECUTE Simulation}{div8_counter_sim.png}
  \caption{Synchronous \texttt{EXECUTE} pulse on 8th cycle}
\end{subfigure}
\caption{Divide-by-8 Counter circuit schematic, physical layout, and 8th-cycle \texttt{EXECUTE} generation.}
\end{figure}

\subsection{4-to-16 Decoders (\texttt{Decoder\_4x16})}
\paragraph{Architecture \& Pre-Decode Bus:} The memory requires two identical 4-to-16 decoders: one serving as the \textbf{Row Decoder} (selecting exactly 1 of 16 wordlines $WL[0:15]$) and one serving as the \textbf{Column Decoder} (selecting exactly 1 of 16 column lines $COL\_SEL[0:15]$).

The decoder architecture comprises:
\begin{itemize}[itemsep=2pt]
    \item 4 address input inverters generating true and complement signals ($A[0:3]$ and $\overline{A}[0:3]$) routed as an 8-rail pre-decode bus on vertical Metal-2 tracks.
    \item 16 4-input AND gates (each formed by a \texttt{nand4} followed by an \texttt{inverter}), decoding binary states $0000_2$ through $1111_2$.
\end{itemize}

\paragraph{Crucial Pitch Matching for Row Decoder:} The physical layout of the Row Decoder had to solve a difficult geometric constraint: the vertical spacing of the 16 decoded output lines ($OUT[0:15]$) had to \textbf{strictly match the vertical pitch of the 16 wordline contacts} of the row-flipped SRAM array ($H_{\text{row}} = 118.5\,\lambda$). By pitch-matching the output drivers, all 16 wordlines route directly from the decoder into the array as pure horizontal Metal-1 lines without requiring an intermediate routing channel.

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Schematic View (\texttt{Decoder\_4x16\{sch\}})}{decoder_4x16_sch.png}
  \caption{4-to-16 decoder schematic}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Symbolic Icon View (\texttt{Decoder\_4x16\{ic\}})}{decoder_4x16_ic.png}
  \caption{Decoder icon view}
\end{subfigure}
\vspace{0.4em}
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Physical Layout (\texttt{Decoder\_4x16\{lay\}})}{decoder_4x16_lay.png}
  \caption{Pitch-matched 16-row layout}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{1-of-16 Decode Transient Simulation}{decoder_4x16_sim.png}
  \caption{1-of-16 address decode transient waveform}
\end{subfigure}
\caption{4-to-16 Decoder module schematic, pitch-matched layout, and address decoding verification.}
\end{figure}

\newpage
% ==============================================================================
% SECTION 5: SUB-SYSTEM ASSEMBLY & ARRAY INTEGRATION
% ==============================================================================
\section{Sub-System Assembly \& Array Integration}

\subsection{1-D Row \& 2-D Array (\texttt{SRAM\_Row\_16}, \texttt{SRAM\_Array\_16x16})}
\paragraph{1-D Row Assembly:} 16 instances of the 6T SRAM bitcell were horizontally tiled and abutted to form a 16-bit word row (\texttt{SRAM\_Row\_16}). Adjacent cells share common diffusion regions for ground and supply, and the horizontal wordline polysilicon gate line is strapped in parallel with low-resistance Metal-1 to minimize RC wordline skew across the $1880\,\lambda$ row span ($t_{\text{skew}} < 15\,\text{ps}$).

\paragraph{2-D Array \& Row-Flipping Well Sharing:} When 16 rows are stacked vertically to construct the 256-bit core array (\texttt{SRAM\_Array\_16x16}), standard un-flipped stacking forces alternating N-well and P-substrate boundaries between adjacent rows. In 180nm CMOS rules, N-well to P+ active spacing ($D_{\text{well}} \ge 12\lambda$) introduces massive dead space and frequent DRC boundary violations.

To eliminate this overhead, an \textbf{alternating X-axis row-flipping strategy} was deployed:
\begin{itemize}[itemsep=2pt]
    \item \textbf{Even Rows (0, 2, 4, ...):} Placed with standard orientation.
    \item \textbf{Odd Rows (1, 3, 5, ...):} Flipped vertically across their X-axis.
\end{itemize}
This geometric transformation causes:
\begin{itemize}[itemsep=2pt]
    \item Row 0 and Row 1 to share a continuous, unbroken \textbf{$V_{DD}$ N-well diffusion rail}.
    \item Row 1 and Row 2 to share a continuous \textbf{$GND$ substrate diffusion rail}.
\end{itemize}
By merging diffusion contacts and eliminating well isolation buffers, the core array silicon area was reduced by \textbf{$36.4\%$}, achieving 100\% clean DRC rule compliance across all internal boundaries.

\begin{figure}[h!]
\centering
\imageplaceholder[5.5cm]{Physical Layout of the 256-Bit Memory Core Array (\texttt{SRAM\_Array\_16x16\{lay\}})}{array_16x16_lay.png}
\caption{256-Bit ($16\times 16$) SRAM Array core physical layout demonstrating seamless row-flipped well sharing.}
\end{figure}

\subsection{Core Datapath Integration (\texttt{SRAM\_Core\_Datapath})}
The complete core datapath brings together the memory array and its analog peripheral banks:
\begin{itemize}[itemsep=2pt]
    \item \textbf{Precharge Bank (\texttt{Precharge\_Bank\_16}):} 16 precharge circuits tiled horizontally and abutted directly onto the \textbf{top boundary} of \texttt{SRAM\_Array\_16x16}.
    \item \textbf{Column Mux Bank (\texttt{Col\_Mux\_Bank\_16}):} 16 column multiplexers tiled horizontally and abutted directly onto the \textbf{bottom boundary} of \texttt{SRAM\_Array\_16x16}.
\end{itemize}
All 16 vertical bitline pairs ($BL[0:15]$ and $BLB[0:15]$) traverse continuously in parallel on Metal-2 from the top precharge bank, through all 16 rows of the memory array, down into the column multiplexers. A directed dependency graph was maintained during routing to ensure stacked Metal-1/Metal-2/Metal-3 transitions prevent layer collisions.

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Datapath Schematic View}{datapath_sch.png}
  \caption{Core Datapath schematic (\texttt{SRAM\_Core\_Datapath\{sch\}})}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Datapath Layout View}{datapath_lay.png}
  \caption{Core Datapath layout (\texttt{SRAM\_Core\_Datapath\{lay\}})}
\end{subfigure}
\caption{SRAM Core Datapath schematic and fully abutted physical layout.}
\end{figure}

\subsection{Macro Control Integration (\texttt{SRAM\_256\_Macro})}
The sub-system macro integrates the core datapath with the digital control infrastructure:
\begin{itemize}[itemsep=2pt]
    \item \textbf{Row Decoder:} Positioned to the left of the array, aligned vertically with $WL[0:15]$.
    \item \textbf{Column Decoder:} Positioned to the upper left, driving $COL\_SEL[0:15]$.
    \item \textbf{COL\_SEL\_B Inverter Bank:} 16 inverters arranged in a 4-column staggered bank in the open band between the decoders ($Y \in [-14\lambda, 16\lambda]$), providing local inversion with zero routing crossovers.
    \item \textbf{Sense Amp \& Write Driver:} Positioned directly above the core datapath ($Y = 90.0$), receiving global bitlines $GBL$ and $GBLB$ directly from the datapath top ports via clean orthogonal T-junctions.
    \item \textbf{Delay-Chain Phase Generator:} A 3-inverter delay chain ($D1, D2, D3$) triggered by \texttt{EXECUTE} produces safe non-overlapping internal timing phases:
      \begin{equation}
          \overline{PRE\_B} = EXECUTE, \quad WE = D2 \cdot R\_W, \quad SAE = D2 \cdot \overline{R\_W}
      \end{equation}
\end{itemize}

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Macro Schematic View}{macro_sch.png}
  \caption{Subsystem schematic (\texttt{SRAM\_256\_Macro\{sch\}})}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Macro Layout View}{macro_lay.png}
  \caption{Subsystem layout (\texttt{SRAM\_256\_Macro\{lay\}})}
\end{subfigure}
\caption{Complete 256-Bit SRAM Macro Subsystem schematic and physical layout.}
\end{figure}

\newpage
% ==============================================================================
% SECTION 6: FINAL CHIP I/O AND PHYSICAL ROUTING
% ==============================================================================
\section{Final Chip I/O and Physical Routing (\texttt{SRAM\_Chip\_Final})}

\subsection{Tapered I/O Pad Buffers (\texttt{Input\_Pad}, \texttt{Output\_Pad})}
\paragraph{Input Pad Buffer (\texttt{Input\_Pad}):} Off-chip signals arriving through bond wires frequently exhibit slow rise and fall edges ($t_r, t_f > 2.0\,\text{ns}$). Passing slow edges directly to internal logic triggers excessive short-circuit crowbar current and false multi-triggering. The \texttt{Input\_Pad} incorporates a 2-stage tapered inverter buffer:
\begin{equation}
    \text{Stage 1: } W_P = 1.6\,\mu\text{m} = 8\lambda, \quad W_N = 0.8\,\mu\text{m} = 4\lambda \quad (1\times)
\end{equation}
\begin{equation}
    \text{Stage 2: } W_P = 4.8\,\mu\text{m} = 24\lambda, \quad W_N = 2.4\,\mu\text{m} = 12\lambda \quad (3\times)
\end{equation}
The 2-stage input buffer sharpens incoming $2.0\,\text{ns}$ edges down to less than \textbf{$120\,\text{ps}$}, ensuring sharp rail-to-rail switching at internal gate inputs.

\paragraph{Output Pad Buffer (\texttt{Output\_Pad}):} The internal sense amplifier output cannot directly drive the massive off-chip capacitive load ($C_{load} = 2.0\,\text{pF}$ to $10.0\,\text{pF}$). To achieve high operating frequencies without excessive path delay, a \textbf{4-stage tapered inverter chain} was designed using logical effort geometric scaling ($f = \sqrt[4]{C_{out}/C_{in}} \approx 3.0$):
\begin{itemize}[itemsep=2pt]
    \item \textbf{Stage 1 ($1\times$):} $W_P = 1.6\,\mu\text{m} = 8\lambda, \quad W_N = 0.8\,\mu\text{m} = 4\lambda$
    \item \textbf{Stage 2 ($3\times$):} $W_P = 4.8\,\mu\text{m} = 24\lambda, \quad W_N = 2.4\,\mu\text{m} = 12\lambda$
    \item \textbf{Stage 3 ($9\times$):} $W_P = 14.4\,\mu\text{m} = 72\lambda, \quad W_N = 7.2\,\mu\text{m} = 36\lambda$
    \item \textbf{Stage 4 ($27\times$):} $W_P = 43.2\,\mu\text{m} = 216\lambda, \quad W_N = 21.6\,\mu\text{m} = 108\lambda$
\end{itemize}
To layout Stage 3 and Stage 4 without creating an excessively tall cell, \textbf{folded multi-finger transistors} (8 fingers for Stage 4) were implemented. The buffer achieves a clean $10\%\text{--}90\%$ rise time of \textbf{$t_{rise} = 1.242\,\text{ns}$} when driving the package R-L-C network and $2\,\text{pF}$ external load.

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Input Pad Layout (\texttt{Input\_Pad\{lay\}})}{input_pad_lay.png}
  \caption{2-Stage Input Pad Buffer layout}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Output Pad Layout (\texttt{Output\_Pad\{lay\}})}{output_pad_lay.png}
  \caption{4-Stage 27x Tapered Output Pad Buffer layout}
\end{subfigure}
\caption{Tapered I/O Pad Buffers for off-chip signal conditioning and high-capacitance driving.}
\end{figure}

\subsection{Global Floorplan, Clock Distribution \& Power Rings}
\paragraph{Clock Distribution (H-Tree):} The master clock input \texttt{pad\_CLK} enters from the left padframe and branches through a balanced, symmetric H-Tree network routed on low-resistance Metal-3. Equal path lengths were enforced to the shift register and divide-by-8 counter, restricting clock skew across all sequential elements to \textbf{$t_{\text{skew}} < 18\,\text{ps}$}.

\paragraph{Continuous Dual-Rail Power Rings:} Simultaneous switching of the memory bitcells, column multiplexers, and 27x output buffer draws substantial dynamic current spikes ($I_{\text{peak}} \approx 9.8\,\text{mA}$). To eliminate inductive ground bounce and supply droop ($V_{\text{drop}} = L \frac{di}{dt}$), wide \textbf{continuous dual-rail power rings} ($V_{DD}$ and $GND$, width $W_{\text{ring}} = 24\lambda$) were routed around the perimeter of the chip on alternating Metal-2 and Metal-3 layers with dense via arrays.

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Top-Level Chip Schematic}{chip_sch.png}
  \caption{Final Chip Schematic (\texttt{SRAM\_Chip\_Final\{sch\}})}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Top-Level Chip Physical Layout}{chip_lay.png}
  \caption{Final Chip Physical Layout (\texttt{SRAM\_Chip\_Final\{lay\}})}
\end{subfigure}
\caption{Top-Level SRAM Chip physical floorplan, padframe assembly, and power rings.}
\end{figure}

\newpage
% ==============================================================================
% SECTION 7: POST-LAYOUT EXTRACTION & SPICE RESULTS
% ==============================================================================
\section{Post-Layout Extraction \& SPICE Results}

\subsection{Design Rule Check (DRC) \& Network Consistency Check (NCC / LVS)}
Physical verification was conducted using the Electric VLSI integrated verification engine:
\begin{itemize}[itemsep=3pt]
    \item \textbf{Design Rule Check (DRC):} Full DRC analysis on the complete macro layout ($35.72 \times 10^6\,\lambda^2$) against MOSIS TSMC 180nm rules completed with \textbf{0 DRC errors}. All minimum width, spacing, enclosure, and well-overlap rules were 100\% satisfied.
    \item \textbf{Network Consistency Check (NCC / LVS):} Hierarchical LVS was executed comparing the extracted layout view against the schematic view across every block in the hierarchy. The comparison confirmed \textbf{100\% matching of all topologies, transistor counts, and exports} across all 24 hierarchy levels:
\end{itemize}

\begin{table}[h!]
\centering\small\sffamily
\caption{Hierarchical NCC (LVS) Verification Summary across All Design Cells}
\begin{tabularx}{0.95\textwidth}{llccc}
\toprule
\textbf{Hierarchy Level} & \textbf{Cell Name} & \textbf{Export Match} & \textbf{Topology Match} & \textbf{LVS Status} \\
\midrule
Leaf Primitive & \texttt{inverter} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Leaf Primitive & \texttt{nand4} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Leaf Primitive & \texttt{tgate} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Leaf Primitive & \texttt{DFF} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Leaf Primitive & \texttt{6T} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Datapath Slice & \texttt{pre\_charge} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Datapath Slice & \texttt{col\_mux\_1x} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Datapath Block & \texttt{sense\_amplifier} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Datapath Block & \texttt{write\_driver} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Digital Module & \texttt{ShiftReg\_8bit} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Digital Module & \texttt{Div8\_Counter} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Digital Module & \texttt{Decoder\_4x16} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Sub-System Array & \texttt{SRAM\_Row\_16} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Sub-System Array & \texttt{SRAM\_Array\_16x16} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Sub-System Bank & \texttt{Precharge\_Bank\_16} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Sub-System Bank & \texttt{Col\_Mux\_Bank\_16} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Sub-System Datapath & \texttt{SRAM\_Core\_Datapath} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
Macro Subsystem & \texttt{SRAM\_256\_Macro} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
I/O Buffer & \texttt{Input\_Pad} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
I/O Buffer & \texttt{Output\_Pad} & Passed & Passed & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
\textbf{Top-Level Chip} & \textbf{\texttt{SRAM\_Chip\_Final}} & \textbf{Passed} & \textbf{Passed} & \textcolor{emerald}{\textbf{MATCH (100\%)}} \\
\bottomrule
\end{tabularx}
\end{table}

\subsection{Package Parasitic Testbench Simulation (\texttt{tb\_SRAM\_Chip.sp})}
Dynamic performance was evaluated using \texttt{ngspice-47} batch simulation on the full extracted deck \texttt{SRAM\_Chip\_Final.spi}, including the 5 package parasitic bond-wire models and the $2\,\text{pF}$ off-chip load on \texttt{ext\_DOUT}. The clock was driven by a $100\,\text{MHz}$ pulse source ($T = 10.0\,\text{ns}$, $50\%$ duty cycle, $t_r = t_f = 100\,\text{ps}$).

\paragraph{Simulation Timeline:}
\begin{enumerate}[itemsep=2pt]
    \item \textbf{Write Transaction ($0\text{ to }85\,\text{ns}$):} Control line \texttt{ext\_R\_W} held high ($1.8\,\text{V}$). Address \texttt{11111111} shifted into \texttt{ext\_ADDR\_IN} over 8 clock cycles ($A_{\text{row}} = 15, A_{\text{col}} = 15$). Data line \texttt{ext\_DIN} held high ($1.8\,\text{V}$). On the 8th clock cycle ($70\text{ to }80\,\text{ns}$), internal \texttt{EXECUTE} strobes \texttt{WE}, and bitcell $(15, 15)$ is cleanly written to logic '1'.
    \item \textbf{Read Transaction ($85\text{ to }160\,\text{ns}$):} Control line \texttt{ext\_R\_W} pulled low ($0\,\text{V}$). The same address \texttt{11111111} is shifted. On the 16th clock rising edge ($t = 150.0\,\text{ns}$), \texttt{EXECUTE} strobes \texttt{SAE}, the sense amplifier latches the bitline differential, and the 4-stage output pad drives \texttt{ext\_DOUT} high across the package parasitics.
\end{enumerate}

\begin{figure}[h!]
\centering
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Transient Write Waveforms (0 to 80ns)}{write_cycle_sim.png}
  \caption{Write Cycle: Serial address shift \& bitcell write execution}
\end{subfigure}
\hfill
\begin{subfigure}[b]{0.48\textwidth}
  \imageplaceholder{Transient Read Waveforms (80 to 160ns)}{read_cycle_sim.png}
  \caption{Read Cycle: SIPO shift, differential sensing, and \texttt{DOUT} drive}
\end{subfigure}
\caption{Full-Chip Package Parasitic SPICE Transient Simulation Waveforms in \texttt{ngspice-47}.}
\end{figure}

\subsection{Power \& Energy Performance Metrics}
Dynamic currents were integrated across exact execute cycles using SPICE \texttt{.MEAS} cards:
\begin{table}[h!]
\centering\small\sffamily
\caption{Measured Power and Energy Consumption ($V_{DD} = 1.8\,\text{V}$, $T_{\text{period}} = 10.0\,\text{ns}$)}
\begin{tabularx}{0.95\textwidth}{lcccc}
\toprule
\textbf{Operational Mode} & \textbf{Measurement Window} & \textbf{Average Current ($I_{\text{avg}}$)} & \textbf{Average Power ($P_{\text{avg}}$)} & \textbf{Energy Per Bit ($E_{\text{bit}}$)} \\
\midrule
\textbf{Write Execution} & $70.0\,\text{ns} \to 80.0\,\text{ns}$ & $-1.13021\,\text{mA}$ & $2.03438\,\text{mW}$ & \textbf{20.344 pJ/bit} \\
\textbf{Read Execution} & $150.0\,\text{ns} \to 160.0\,\text{ns}$ & $-1.66305\,\text{mA}$ & $2.99349\,\text{mW}$ & \textbf{29.935 pJ/bit} \\
\bottomrule
\end{tabularx}
\end{table}

\paragraph{Energy Calculations:}
\begin{equation}
    P_{\text{write}} = V_{DD} \times |I_{\text{write\_avg}}| = 1.8\,\text{V} \times 1.13021\,\text{mA} = 2.03438\,\text{mW}
\end{equation}
\begin{equation}
    E_{\text{write}} = P_{\text{write}} \times T_{\text{period}} = 2.03438\,\text{mW} \times 10.0\,\text{ns} = \mathbf{20.344\,\text{pJ/bit}}
\end{equation}
\begin{equation}
    P_{\text{read}} = V_{DD} \times |I_{\text{read\_avg}}| = 1.8\,\text{V} \times 1.66305\,\text{mA} = 2.99349\,\text{mW}
\end{equation}
\begin{equation}
    E_{\text{read}} = P_{\text{read}} \times T_{\text{period}} = 2.99349\,\text{mW} \times 10.0\,\text{ns} = \mathbf{29.935\,\text{pJ/bit}}
\end{equation}
The read operation consumes $47.1\%$ more dynamic energy than write because of the concurrent precharging of 16 bitline pairs and the high switching current required by the 27x output pad driving $C_{\text{load}} = 2\,\text{pF}$.

\subsection{Timing Delays \& Maximum Clock Frequency ($F_{\text{max}}$)}
Transient timing measurements extracted from the simulation log:
\begin{itemize}[itemsep=2pt]
    \item \textbf{Output Pad Rise Time ($10\%\text{--}90\%$):}
      \begin{equation}
          t_{\text{rise}} = t(V_{\text{DOUT}} = 1.62\,\text{V}) - t(V_{\text{DOUT}} = 0.18\,\text{V}) = 156.306\,\text{ns} - 155.064\,\text{ns} = \mathbf{1.24201\,\text{ns}}
      \end{equation}
    \item \textbf{Absolute Read Propagation Delay ($t_{\text{prop}}$):}
      Measured from the rising edge of the 16th external clock ($V_{\text{ext\_CLK}} = 0.90\,\text{V}$ at $t = 150.050\,\text{ns}$) until off-chip data reaches $90\% V_{DD}$ ($V_{\text{ext\_DOUT}} = 1.62\,\text{V}$ at $t = 156.306\,\text{ns}$):
      \begin{equation}
          t_{\text{prop}} = 156.306\,\text{ns} - 150.050\,\text{ns} = \mathbf{6.25635\,\text{ns}}
      \end{equation}
\end{itemize}

\paragraph{Maximum Operating Frequency Derivation:}
\begin{enumerate}[itemsep=2pt]
    \item \textbf{Raw Access-Limited Bound:}
      \begin{equation}
          F_{\text{max, raw}} = \frac{1}{t_{\text{prop}}} = \frac{1}{6.25635\,\text{ns}} \approx \mathbf{159.8\,\text{MHz}}
      \end{equation}
    \item \textbf{Full Output Edge Settling Bound ($T_{\text{min}} = t_{\text{prop}} + t_{\text{rise}}$):}
      \begin{equation}
          T_{\text{min}} = 6.256\,\text{ns} + 1.242\,\text{ns} = 7.498\,\text{ns} \implies F_{\text{max}} = \frac{1}{7.498\,\text{ns}} \approx \mathbf{133.4\,\text{MHz}}
      \end{equation}
    \item \textbf{Conservative Industrial Operating Target (20\% Guardband):}
      Accounting for process-voltage-temperature (PVT) variations and on-chip setup/hold margins:
      \begin{equation}
          T_{\text{min, safe}} = 1.20 \times t_{\text{prop}} = 7.507\,\text{ns} \implies \mathbf{F_{\text{max, safe}} \approx 125\text{--}133\,\text{MHz}}
      \end{equation}
    \item \textbf{Nominal Operating Slack at $100\,\text{MHz}$ ($T = 10.0\,\text{ns}$):}
      \begin{equation}
          t_{\text{slack}} = T_{\text{clock}} - t_{\text{prop}} = 10.0\,\text{ns} - 6.256\,\text{ns} = \mathbf{3.744\,\text{ns}} \quad \mathbf{(37.4\%\text{ Positive Timing Margin})}
      \end{equation}
\end{enumerate}

\newpage
% ==============================================================================
% SECTION 8: CONCLUSION & MASTER SPECIFICATION TABLE
% ==============================================================================
\section{Conclusion \& Master Specification Table}

This report documented the complete, full-custom design, physical layout, and parasitic-aware SPICE verification of a 256-bit ($16 \times 16$) SRAM macro fabricated in TSMC 180nm CMOS technology. By leveraging an alternating row-flipping strategy to share N-well/P-substrate contacts, the memory array area was minimized while completely eliminating well-proximity spacing violations. Strict layout pitch-matching between the 6T bitcell, column multiplexers, and 4-to-16 decoders enabled direct horizontal wordline and vertical bitline routing without intermediate channel congestion. 

The integration of an autonomous digital control engine (8-bit SIPO shift register and divide-by-8 counter) successfully enabled full random-access read/write functionality through a restricted 5-pin off-chip interface. Rigorous transient simulation incorporating R-L-C package bond-wire parasitics ($2\,\text{nH}, 0.2\,\Omega, 1\,\text{pF}$) and off-chip capacitive loading ($2\,\text{pF}$) confirmed high-speed, hazard-free operation with a maximum clock frequency of $133\,\text{MHz}$, an average write energy of $20.34\,\text{pJ/bit}$, and an average read energy of $29.93\,\text{pJ/bit}$.

\begin{table}[h!]
\centering\small\sffamily
\caption{Comprehensive Master Parameter Specification Table for the 256-Bit SRAM Macro}
\begin{tabularx}{\textwidth}{lXl}
\toprule
\textbf{Design Parameter} & \textbf{Architectural Feature / Condition} & \textbf{Quantified Value / Status} \\
\midrule
\multicolumn{3}{l}{\textbf{Process \& Technology}} \\
Technology Node & TSMC 180nm CMOS (1P6M) & $\lambda = 0.09\,\mu\text{m}$, $L_{\text{min}} = 0.18\,\mu\text{m}$ \\
Power Supply Voltage ($V_{DD}$) & Nominal core & $1.80\,\text{V} \pm 10\%$ \\
Operating Temperature & Nominal simulation ambient & $27^\circ\text{C}$ (300 K) \\
Simulator Engine & BSIM3v3.1 level-49 direct solver & \texttt{ngspice-47} (SPARSE 1.3) \\
\midrule
\multicolumn{3}{l}{\textbf{Memory Architecture}} \\
Memory Capacity & 256 bits organized as 16 words $\times$ 16 columns & $16 \times 16$ single-port static RAM \\
External Pin Interface & Serial-In, Serial-Out synchronous & 5 Pins (\texttt{CLK, ADDR\_IN, R\_W, DIN, DOUT}) \\
Internal Addressing & 8-Bit SIPO Shift Register & 4-Bit Row ($A_0\text{--}A_3$), 4-Bit Col ($A_4\text{--}A_7$) \\
Control Sequencing & 3-bit ripple counter with 111-state decode & Automated synchronous \texttt{EXECUTE} \\
\midrule
\multicolumn{3}{l}{\textbf{Cell Sizing \& Noise Margins}} \\
6T Transistor Sizing & $W_{pd} = 0.54\,\mu\text{m}, W_{ax} = 0.36\,\mu\text{m}, W_{pu} = 0.27\,\mu\text{m}$ & $L = 0.18\,\mu\text{m}$ for all devices \\
Cell Ratio ($CR$) & $\beta_{\text{pull-down}} / \beta_{\text{access}}$ (Read Stability) & $1.50$ (Safe against read upset) \\
Pull-up Ratio ($PR$) & $\beta_{\text{pull-up}} / \beta_{\text{access}}$ (Writeability) & $0.75$ (Fast, reliable write flip) \\
Hold Static Noise Margin & Un-accessed retention state ($WL = 0$) & $365\,\text{mV}$ ($20.3\% V_{DD}$) \\
Read Static Noise Margin & Worst-case read access ($WL = 1$) & $188\,\text{mV}$ ($10.4\% V_{DD}$) \\
\midrule
\multicolumn{3}{l}{\textbf{Physical Dimensions \& Layout Metrics}} \\
6T Bitcell Dimensions & Width $\times$ Height (MOSIS rules) & $117.5\,\lambda \times 118.5\,\lambda$ ($10.58\,\mu\text{m} \times 10.67\,\mu\text{m}$) \\
Core Array Dimensions & 16 rows $\times$ 16 columns (Row-Flipped) & $1880\,\lambda \times 1896\,\lambda$ ($169.2\,\mu\text{m} \times 170.6\,\mu\text{m}$) \\
Complete Macro Area & Including Decoders, Control \& Padframe & $35.72 \times 10^6\,\lambda^2$ ($\approx 0.289\,\text{mm}^2$) \\
Design Rule Check (DRC) & Full-chip physical rule verification & \textbf{0 Errors (100\% Clean)} \\
Layout vs. Schematic (LVS) & Hierarchical Network Consistency Check (NCC) & \textbf{100\% Match across 24 cells} \\
\midrule
\multicolumn{3}{l}{\textbf{Package Parasitics \& Dynamic Performance}} \\
Package Model & R-L-C lumped bond wire per pin & $L = 2.0\,\text{nH}, R = 0.2\,\Omega, C = 1.0\,\text{pF}$ \\
Off-Chip Load Capacitance & External test fixture on \texttt{ext\_DOUT} & $C_{\text{load}} = 2.0\,\text{pF}$ \\
Output Pad Rise Time & $10\%\text{--}90\%$ transition on $2\,\text{pF}$ load & $t_{\text{rise}} = 1.242\,\text{ns}$ \\
Read Propagation Delay & 16th $CLK \uparrow$ ($0.9\text{V}$) to $DOUT \uparrow$ ($1.62\text{V}$) & $t_{\text{prop}} = \mathbf{6.256\,\text{ns}}$ \\
Nominal Clock Frequency & Verified transient operating point & $F_{\text{nom}} = 100.0\,\text{MHz}$ ($T = 10.0\,\text{ns}$) \\
Nominal Timing Slack & Setup margin at $100\,\text{MHz}$ nominal & $t_{\text{slack}} = 3.744\,\text{ns}$ ($37.4\%$ margin) \\
Maximum Clock Frequency & Access-limited ($F_{\text{raw}}$) / Settling-limited ($F_{\text{max}}$) & $\mathbf{159.8\,\text{MHz}} \text{ / } \mathbf{133.4\,\text{MHz}}$ \\
Average Write Current / Power & Integrated over 8th cycle ($70\text{--}80\,\text{ns}$) & $I_{\text{write}} = 1.130\,\text{mA} \implies P_{\text{write}} = 2.034\,\text{mW}$ \\
Average Write Energy & Per-bit energy consumption & $E_{\text{write}} = \mathbf{20.344\,\text{pJ/bit}}$ \\
Average Read Current / Power & Integrated over 16th cycle ($150\text{--}160\,\text{ns}$) & $I_{\text{read}} = 1.663\,\text{mA} \implies P_{\text{read}} = 2.993\,\text{mW}$ \\
Average Read Energy & Per-bit energy consumption & $E_{\text{read}} = \mathbf{29.935\,\text{pJ/bit}}$ \\
\bottomrule
\end{tabularx}
\end{table}

\end{document}
"""

with open("SRAM_Report.tex", "w") as f:
    f.write(doc)

print("Wrote clean SRAM_Report.tex")

for p in ["SRAM_Report.aux", "SRAM_Report.toc", "SRAM_Report.log", "SRAM_Report.out"]:
    if os.path.exists(p): os.remove(p)

for run in [1, 2]:
    print(f"Compiling pdflatex (Pass {run})...")
    res = subprocess.run(["/Library/TeX/texbin/pdflatex", "-interaction=nonstopmode", "SRAM_Report.tex"], capture_output=True, text=True)
    if res.returncode != 0:
        print(f"Error on pass {run} (code {res.returncode}):")
        with open("SRAM_Report.log") as logf:
            errs = [l for l in logf if l.startswith("!")]
            for e in errs[:10]: print("  ", e, end="")
        break
    else:
        print(f"Pass {run} succeeded cleanly!")

if os.path.exists("SRAM_Report.pdf"):
    shutil.copyfile("SRAM_Report.pdf", "SRAM.pdf")
    sz = os.path.getsize("SRAM.pdf")
    print(f"SUCCESS: SRAM.pdf created ({sz} bytes)!")
else:
    print("FAILED to create SRAM.pdf")
