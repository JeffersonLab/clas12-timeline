#!/usr/bin/env python3
##################################################################################
# ADAPTED FROM BHAWANI SINGH's ORIGINAL SCRIPT:
#   /w/hallb-scshelf2102/clas12/singh/Softwares/QADB_studies/python/main2.py
##################################################################################
import numpy as np
import os
import sys
from glob import glob
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import hipolib

# plt.style.use('seaborn-darkgrid')

def main():

    if len(sys.argv) != 4:
        print(f'''
USAGE: {sys.argv[0]} [INPUT_HIPO_PATH] [OUTPUT_DIR] [OUTPUT_FILE_SUFFIX]
    INPUT_HIPO_PATH       input HIPO. If it ends in ".hipo" it is treated as a
                          single file; otherwise it is treated as a directory and
                          ALL "*.hipo" files inside it (assumed to be for the same
                          run) are read and aggregated together. Useful for DSTs.
    OUTPUT_DIR            directory where output plots are written
    OUTPUT_FILE_SUFFIX    append this string to the output
                          file name; useful if you are comparing
                          output files before and after reheating
        ''')
        exit(2)
    hipo_path     = sys.argv[1]
    output_dir    = sys.argv[2]
    output_suffix = sys.argv[3]

    hipo_prefix = os.getenv('HIPO')
    if hipo_prefix == None:
        raise ValueError("HIPO env var not set")

    # Make sure the output directory exists before writing any plots into it.
    os.makedirs(output_dir, exist_ok=True)

    # Resolve the input into a concrete list of .hipo files. A path ending in
    # ".hipo" is a single file; anything else is a directory whose *.hipo files
    # (all for the same run) are read and aggregated together.
    if hipo_path.endswith('.hipo'):
        hipo_files    = [hipo_path]
        file_basename = os.path.splitext(os.path.basename(hipo_path))[0]
    else:
        hipo_files    = sorted(glob(os.path.join(hipo_path, '*.hipo')))
        file_basename = os.path.basename(os.path.normpath(hipo_path))

    if not hipo_files:
        raise ValueError(f"No .hipo files found for input path: {hipo_path}")

    print(f"Found {len(hipo_files)} HIPO file(s) to process under: {hipo_path}")

    reader = hipolib.hreader(f'{hipo_prefix}/lib')

    timestamps, fcups, fcupgateds, live_times = [], [], [], []

    # Helicity-latched FC charge from HEL::scaler. There can be more than one row
    # per event (pile-up), so every row is read. We keep the latched helicity and
    # both its ungated ('fcup') and gated ('fcupgated') FC charge, tagged with the
    # event timestamp.
    hel_timestamps, hel_helicities, hel_fcups, hel_fcupgateds = [], [], [], []

    counter = 0
    last_timestamp = 0
    for file_idx, hipo_file in enumerate(hipo_files):
        print(f"[{file_idx + 1}/{len(hipo_files)}] Processing file: {hipo_file}")
        reader.open_with_tag(hipo_file, 1)  # filter by tag at open time
        # Banks must be (re)declared for each newly opened file handle.
        reader.define('RUN::config')
        reader.define('RUN::scaler')
        reader.define('HEL::scaler')

        while reader.next():
            if counter % 100000 == 0 and counter > 0:
                print(f'Processing event # {counter}')
            counter += 1

            # Track the most recent config timestamp so HEL::scaler rows can be
            # placed on the same time axis even when RUN::scaler is empty.
            if reader.getSize('RUN::config') > 0:
                last_timestamp = reader.getEntry('RUN::config', 'timestamp')[0]

            # ----- HEL::scaler: loop over ALL rows (pile-up) -----
            hel_size = reader.getSize('HEL::scaler')
            if hel_size > 0:
                hel_h   = reader.getEntry('HEL::scaler', 'helicity')
                hel_fc  = reader.getEntry('HEL::scaler', 'fcup')
                hel_fcg = reader.getEntry('HEL::scaler', 'fcupgated')
                for r in range(hel_size):
                    hel_timestamps.append(last_timestamp)
                    hel_helicities.append(hel_h[r])
                    hel_fcups.append(hel_fc[r])
                    hel_fcupgateds.append(hel_fcg[r])

            if reader.getSize('RUN::config') == 0 or reader.getSize('RUN::scaler') == 0:
                # print(f"WARNING [analyze_charge]: Skipping empty bank at event {counter}", file=sys.stderr)
                continue

            timestamp = reader.getEntry('RUN::config', 'timestamp')
            fcup = reader.getEntry('RUN::scaler', 'fcup')
            fcupgated = reader.getEntry('RUN::scaler', 'fcupgated')
            live_time = reader.getEntry('RUN::scaler', 'livetime')

            timestamps.append(timestamp[0])
            fcups.append(fcup[0])
            fcupgateds.append(fcupgated[0])
            live_times.append(live_time[0])

    print(f"Processed {counter} events across {len(hipo_files)} file(s).")
    print(f"Collected {len(hel_helicities)} HEL::scaler rows.")

    # Sort data by timestamps
    sorted_data = sorted(zip(timestamps, fcups, fcupgateds, live_times))
    if not sorted_data:
        raise ValueError("No data to plot.")

    timestamps, fcups, fcupgateds, live_times = zip(*sorted_data)

    timestamps = np.array(timestamps)
    fcups = np.array(fcups)
    fcupgateds = np.array(fcupgateds)
    live_times = np.array(live_times)

    # ---------- Helicity-latched FC charge prep (HEL::scaler) ----------
    # RUN::scaler 'fcup'/'fcupgated' are cumulative counters (diffed in the chunked
    # loop below); HEL::scaler 'fcup'/'fcupgated' are per-window deltas, so the
    # charge for a given helicity state is a running SUM of the rows latched to that
    # state. Prepared once here; the helicity content is merged into the timestamp
    # figure (extra columns) and the chunked figure (no standalone figure).
    chunk_size = 100
    # Bin HEL::scaler at the same size as RUN::scaler so the helicity panel's chunk
    # axis lines up with the other chunked panels (~same number of bins). Note the
    # two still index different banks (HEL has pile-up rows), so counts differ slightly.
    hel_chunk_size = chunk_size
    has_hel = bool(hel_helicities)
    if has_hel:
        hel_sorted = sorted(zip(hel_timestamps, hel_helicities, hel_fcups, hel_fcupgateds))
        h_ts, h_hel, h_fcup, h_fcupg = zip(*hel_sorted)
        h_ts    = np.array(h_ts)
        h_hel   = np.array(h_hel)
        h_fcup  = np.array(h_fcup,  dtype=float)   # ungated per-window charge
        h_fcupg = np.array(h_fcupg, dtype=float)   # gated per-window charge

        hel_masks = {
            '-1': h_hel < 0,
            '+1': h_hel > 0,
            '0':  h_hel == 0,
        }
        print(
            f"HEL::scaler rows by helicity: "
            f"-1={np.count_nonzero(hel_masks['-1'])}, "
            f"+1={np.count_nonzero(hel_masks['+1'])}, "
            f"0={np.count_nonzero(hel_masks['0'])}"
        )

        # Bin the helicity rows and accumulate ungated/gated charge per state vs bin.
        hbin = hel_chunk_size
        if len(h_hel) < 2 * hbin:
            hbin = max(1, len(h_hel) // 50)
        num_hel_bins = len(h_hel) // hbin
        hel_bin_idx  = np.arange(num_hel_bins)
        hel_xlabel   = f'Chunk Index (Each = {hbin} helicity windows)'
        # End-of-bin timestamp, so the binned helicity charge can also be shown
        # against the time axis (used by the fcup_vs_timestamp figure).
        hel_bin_ts   = h_ts[(hel_bin_idx + 1) * hbin - 1]

        # cumulative charge per state: 'u' = ungated (fcup), 'g' = gated (fcupgated)
        hel_cum       = {'u': {k: [] for k in hel_masks}, 'g': {k: [] for k in hel_masks}}
        hel_cum_total = {'u': [], 'g': []}
        _run       = {'u': {k: 0.0 for k in hel_masks}, 'g': {k: 0.0 for k in hel_masks}}
        _run_total = {'u': 0.0, 'g': 0.0}
        for i in range(num_hel_bins):
            s, e = i * hbin, (i + 1) * hbin
            for k, m in hel_masks.items():
                sub = m[s:e]
                _run['u'][k] += h_fcup[s:e][sub].sum()
                _run['g'][k] += h_fcupg[s:e][sub].sum()
                hel_cum['u'][k].append(_run['u'][k])
                hel_cum['g'][k].append(_run['g'][k])
            _run_total['u'] += h_fcup[s:e].sum()
            _run_total['g'] += h_fcupg[s:e].sum()
            hel_cum_total['u'].append(_run_total['u'])
            hel_cum_total['g'].append(_run_total['g'])

        for kind in ('u', 'g'):
            for k in hel_masks:
                hel_cum[kind][k] = np.array(hel_cum[kind][k], dtype=float)
            hel_cum_total[kind] = np.array(hel_cum_total[kind], dtype=float)

        # Running charge asymmetry (Q+ - Q-)/(Q+ + Q-), ungated and gated.
        hel_asym = {}
        for kind in ('u', 'g'):
            qp, qm = hel_cum[kind]['+1'], hel_cum[kind]['-1']
            denom  = qp + qm
            hel_asym[kind] = np.divide(qp - qm, denom, out=np.full_like(denom, np.nan), where=denom != 0)

        # Integrated totals per state (ungated + gated), for the bar chart + print.
        hel_tot = {
            'u': {k: float(h_fcup[m].sum())  for k, m in hel_masks.items()},
            'g': {k: float(h_fcupg[m].sum()) for k, m in hel_masks.items()},
        }
        hel_tot['u']['total'] = float(h_fcup.sum())
        hel_tot['g']['total'] = float(h_fcupg.sum())
    else:
        print("WARNING [analyze_charge]: No HEL::scaler data found; helicity-latched panels will be skipped.", file=sys.stderr)

    # ---------- Plot 1: Per-event data (cols 1-2) + helicity-latched (cols 3-4) ----------
    fig1, axs1 = plt.subplots(2, 4, figsize=(24, 10))
    fig1.suptitle(f'{file_basename}', fontsize=16, y=0.995)

    plots1 = [
        (axs1[0, 0], fcups, 'DSC2:FCup', 'DSC2:FCup vs Timestamp', 'darkgreen', 'line'),
        (axs1[0, 1], fcupgateds, 'DSC2:FCupgated', 'DSC2:FCupgated vs Timestamp', 'darkorange', 'line'),
        (axs1[1, 0], live_times, 'Live Time', 'Live Time vs Timestamp', 'purple', 'scatter'),
        (axs1[1, 1], fcups * live_times, 'DSC2:FCup × Live Time', 'DSC2:FCup × Live Time vs Timestamp', 'steelblue', 'line'),
    ]
    for ax, data, label, title, color, style in plots1:
        if style == 'line':
            ax.plot(timestamps, data, label=label, color=color, linewidth=1.5)
        elif style == 'scatter':
            ax.scatter(timestamps, data, label=label, color=color, s=10, alpha=0.7)
        ax.set_title(title, fontsize=12)
        ax.set_xlabel('Timestamp', fontsize=10, loc='center')
        ax.set_ylabel(label, fontsize=10)
        ax.legend(fontsize=9)
        ax.grid(True, linestyle='--', alpha=0.6)
        ax.tick_params(axis='both', labelsize=9)

    # Right two columns: helicity-latched charge (HEL::scaler)
    ax_hu   = axs1[0, 2]   # cumulative ungated per helicity
    ax_hg   = axs1[0, 3]   # cumulative gated per helicity
    ax_hasy = axs1[1, 2]   # running charge asymmetry
    ax_hbar = axs1[1, 3]   # total per state (bar chart)

    if has_hel:
        for ax, kind, ylab, title in (
            (ax_hu, 'u', 'Cumulative STRUCK FCup',      'Cumulative Ungated Charge (STRUCK FCup) per Helicity'),
            (ax_hg, 'g', 'Cumulative STRUCK FCupgated', 'Cumulative Gated Charge (STRUCK FCupgated) per Helicity'),
        ):
            ax.plot(hel_bin_ts, hel_cum[kind]['+1'], label='Helicity +1', color='crimson', marker='^', markersize=3)
            ax.plot(hel_bin_ts, hel_cum[kind]['-1'], label='Helicity -1', color='navy',    marker='v', markersize=3)
            ax.plot(hel_bin_ts, hel_cum[kind]['0'],  label='Helicity  0', color='gray',    marker='s', markersize=3)
            ax.plot(hel_bin_ts, hel_cum_total[kind], label='Total (+1, -1, 0)', color='black', linestyle='--')
            ax.set_title(title, fontsize=11)
            ax.set_xlabel('Timestamp', fontsize=10)
            ax.set_ylabel(ylab, fontsize=10)
            ax.legend(fontsize=9)
            ax.grid(True, linestyle='--', alpha=0.6)
            ax.tick_params(axis='both', labelsize=9)

        ax_hasy.axhline(0.0, color='black', linestyle='--', linewidth=1)
        ax_hasy.plot(hel_bin_ts, hel_asym['u'], label='Ungated asymmetry', color='darkorange', marker='o', markersize=3)
        ax_hasy.plot(hel_bin_ts, hel_asym['g'], label='Gated asymmetry',   color='teal',       marker='d', markersize=3)
        ax_hasy.set_title('Running Charge Asymmetry  (Q+ - Q-)/(Q+ + Q-)', fontsize=11)
        ax_hasy.set_xlabel('Timestamp', fontsize=10)
        ax_hasy.set_ylabel('Asymmetry', fontsize=10)
        ax_hasy.set_ylim(-0.02, 0.02)
        ax_hasy.legend(fontsize=9)
        ax_hasy.grid(True, linestyle='--', alpha=0.6)
        ax_hasy.tick_params(axis='both', labelsize=9)

        states = ['+1', '-1', '0']
        labels = ['Hel +1', 'Hel -1', 'Undefined']
        x = np.arange(len(states))
        w = 0.38
        ung = [hel_tot['u'][k] for k in states]
        gat = [hel_tot['g'][k] for k in states]
        ax_hbar.bar(x - w / 2, ung, w, label='Ungated (STRUCK FCup)',     color='darkorange', alpha=0.85)
        ax_hbar.bar(x + w / 2, gat, w, label='Gated (STRUCK FCupgated)', color='teal',       alpha=0.85)
        ax_hbar.set_xticks(x)
        ax_hbar.set_xticklabels(labels)
        ax_hbar.set_title('Total STRUCK Latched Charge per Helicity State', fontsize=11)
        ax_hbar.set_ylabel('Integrated Charge', fontsize=10)
        ax_hbar.legend(fontsize=9)
        ax_hbar.grid(True, axis='y', linestyle='--', alpha=0.6)
        ax_hbar.tick_params(axis='both', labelsize=9)
    else:
        for ax in (ax_hu, ax_hg, ax_hasy, ax_hbar):
            ax.text(0.5, 0.5, 'No HEL::scaler data', ha='center', va='center', transform=ax.transAxes)

    fig1.tight_layout(rect=[0, 0, 1, 0.985])
    fig1.savefig(f'{output_dir}/fcup_vs_timestamp_{file_basename}_{output_suffix}.png', bbox_inches='tight', dpi=300)
    plt.close(fig1)
    # ---------- Compute Chunked FCUP Gated with neighbor handling ----------
    num_chunks = len(timestamps) // chunk_size
    xlabel     = f'Bin num. (size={chunk_size} scalers)'


    chunk_caseA, chunk_caseB, chunk_caseC, chunk_default, chunk_default_ungated = [], [], [], [], []
    cum_caseA, cum_caseB, cum_caseC, cum_default, cum_default_ungated = [], [], [], [], []
    chunk_indices, skipped_counts = [], []

    runA, runB, runC, runDef, runDefUng = 0, 0, 0, 0, 0
    total_skipped = 0

    corrected_livetimes_A = []
    corrected_livetimes_B = []
    corrected_livetimes_C = []

    for i in range(num_chunks):
        start = i * chunk_size
        end = start + chunk_size
        if end >= len(fcups):
            break

        # use np.diff for correct increments
        fcup_diff = np.diff(fcups[start:end])
        fcupgated_diff = np.diff(fcupgateds[start:end])
        live_sub = live_times[start+1:end]

        sumA, sumB, sumC, sumDef, sumDefUng = 0, 0, 0, 0, 0
        skipped_in_chunk = 0

        for j, lt in enumerate(live_sub):
            if lt > 0:
                # Case A
                sumA += lt * fcup_diff[j]
                corrected_livetimes_A.append(lt)
                # Case B
                sumB += lt * fcup_diff[j]
                corrected_livetimes_B.append(lt)
                # Case C
                sumC += lt * fcup_diff[j]
                corrected_livetimes_C.append(lt)
                # Default
                sumDef += fcupgated_diff[j]
                # Default ungated
                sumDefUng += fcup_diff[j]
            else:
                # ----- Case A/B nearest-neighbor substitution -----
                idx_candidates = []
                if j - 1 >= 0 and live_sub[j - 1] > 0:
                    idx_candidates.append(j - 1)
                if j + 1 < len(live_sub) and live_sub[j + 1] > 0:
                    idx_candidates.append(j + 1)

                if idx_candidates:
                    nn = min(
                        idx_candidates,
                        key=lambda k: abs(timestamps[start + 1 + k] - timestamps[start + 1 + j])
                    )
                    lt_nn = live_sub[nn]

                    # Case A
                    sumA += lt_nn * fcup_diff[j]
                    corrected_livetimes_A.append(lt_nn)

                    # Case B
                    sumB += lt_nn * fcupgated_diff[nn]
                    corrected_livetimes_B.append(lt_nn)

                    # Default
                    sumDef += fcupgated_diff[j]

                    # Default ungated
                    sumDefUng += fcup_diff[j]
                else:
                    skipped_in_chunk += 1
                    total_skipped += 1
                    print(
                        f"WARNING [analyze_charge]: No valid positive LT neighbor at chunk {i}, local index {j}, "
                        f"timestamp {timestamps[start+1+j]}",
                        file=sys.stderr
                    )

                # ----- Case C: mean of ±20 neighbors -----
                window = 10
                idx_range = range(max(0, j - window), min(len(live_sub), j + window + 1))
                neigh_lts = [live_sub[k] for k in idx_range if live_sub[k] > 0]
                if neigh_lts:
                    lt_mean = np.mean(neigh_lts)
                    sumC += lt_mean * fcup_diff[j]
                    corrected_livetimes_C.append(lt_mean)

        runA += sumA
        runB += sumB
        runC += sumC
        runDef += sumDef
        runDefUng += sumDefUng

        chunk_caseA.append(sumA)
        chunk_caseB.append(sumB)
        chunk_caseC.append(sumC)
        chunk_default.append(sumDef)
        chunk_default_ungated.append(sumDefUng)
        cum_caseA.append(runA)
        cum_caseB.append(runB)
        cum_caseC.append(runC)
        cum_default.append(runDef)
        cum_default_ungated.append(runDefUng)
        chunk_indices.append(i)
        skipped_counts.append(skipped_in_chunk)

    print(f"Computed chunked FCUP Gated values with neighbor handling (Cases A, B, C).")
    print(f"Total skipped events (no valid LT neighbor): {total_skipped}")

    # ---------- Plot 2: Chunked FCUP Gated + Ratios + Helicity-Latched Charge + Asymmetry ----------
    fig2, (ax_top, ax_mid, ax_gatedrat, ax_hel, ax_asym) = plt.subplots(
        5, 1, figsize=(12, 19), sharex=False,
        gridspec_kw={'height_ratios': [3, 1, 1, 3, 2]}
    )
    fig2.suptitle(f'{file_basename}', fontsize=16, y=0.995)

    # Top: cumulative sums
    ax_top.plot(chunk_indices, cum_default_ungated, label='U: ungated DSC2:FCup',    color='black',       marker='o', markersize=4, linestyle='--')
    ax_top.plot(chunk_indices, cum_default,         label='G: gated DSC2:FCupgated', color='red',         marker='^', markersize=4)
    ax_top.plot(chunk_indices, cum_caseA,           label='G\': LiveTime × U',    color='deepskyblue', marker='x', markersize=4, linestyle='--')
    # ax_top.plot(chunk_indices, cum_caseB, label='Cumulative Case B (LT_nn × FCUPungated_nn)', color='darkgreen',  marker='s', markersize=4)
    # ax_top.plot(chunk_indices, cum_caseC, label='Cumulative Case C (20-NN mean × U)',         color='darkorange', marker='d', markersize=4)
    ax_top.set_ylabel('Cumulative Σ', fontsize=11)
    ax_top.set_xlabel(xlabel, fontsize=11, loc='right')
    ax_top.grid(True, linestyle='--', alpha=0.6)
    ax_top.legend(fontsize=10)
    ax_top.tick_params(axis='both', labelsize=10)

    # Middle: ratios wrt default
    ratioA = np.divide(cum_caseA, cum_default, out=np.full_like(cum_caseA, np.nan, dtype=float), where=np.array(cum_default) != 0)
    #ratioB = np.divide(cum_caseB, cum_default, out=np.full_like(cum_caseB, np.nan, dtype=float), where=np.array(cum_default) != 0)
    ratioC = np.divide(cum_caseC, cum_default, out=np.full_like(cum_caseC, np.nan, dtype=float), where=np.array(cum_default) != 0)
    ratioDefUng = np.divide(cum_default_ungated, cum_default, out=np.full_like(cum_default_ungated, np.nan, dtype=float), where=np.array(cum_default) != 0)

    ax_mid.plot(chunk_indices, ratioA, label='G\' / G', color='magenta', marker='x', markersize=4, linestyle='--')
    #ax_mid.plot(chunk_indices, ratioB, label='Case B / G', color='darkgreen', marker='s', markersize=4)
    # ax_mid.plot(chunk_indices, ratioC, label='Case C / G', color='darkorange', marker='d', markersize=4)
    # ax_mid.plot(chunk_indices, ratioDefUng, label='U / G', color='teal', marker='x', markersize=4, linestyle='--')
    ax_mid.axhline(1.0, color='black', linestyle='--', linewidth=1)
    ax_mid.set_ylabel('Ratio', fontsize=11)
    ax_mid.set_xlabel(xlabel, fontsize=11, loc='right')
    ax_mid.grid(True, linestyle='--', alpha=0.6)
    ax_mid.legend(fontsize=10)
    ax_mid.tick_params(axis='both', labelsize=10)

    # Gated / Ungated ratio panel
    ratio_gated_ung = np.divide(cum_default, cum_default_ungated, out=np.full_like(cum_default, np.nan, dtype=float), where=np.array(cum_default_ungated) != 0)
    ax_gatedrat.plot(chunk_indices, ratio_gated_ung, label='G / U', color='orange', marker='o', markersize=4)
    ax_gatedrat.axhline(1.0, color='black', linestyle='--', linewidth=1)
    ax_gatedrat.set_ylabel('Gated / Ungated', fontsize=11)
    ax_gatedrat.set_xlabel(xlabel, fontsize=11, loc='right')
    ax_gatedrat.grid(True, linestyle='--', alpha=0.6)
    ax_gatedrat.legend(fontsize=10)
    ax_gatedrat.tick_params(axis='both', labelsize=10)

    # Bottom: chunked helicity-latched cumulative gated charge per state vs bin
    if has_hel:
        ax_hel.plot(hel_bin_idx, hel_cum['g']['+1'], label='Helicity +1', color='crimson', marker='^', markersize=3)
        ax_hel.plot(hel_bin_idx, hel_cum['g']['-1'], label='Helicity -1', color='navy',    marker='v', markersize=3)
        ax_hel.plot(hel_bin_idx, hel_cum['g']['0'],  label='Helicity  0', color='gray',    marker='s', markersize=3)
        ax_hel.plot(hel_bin_idx, hel_cum_total['g'], label='Total (sum)', color='black', linestyle='--')
        ax_hel.legend(fontsize=10, ncol=2)
    else:
        ax_hel.text(0.5, 0.5, 'No HEL::scaler data', ha='center', va='center', transform=ax_hel.transAxes)
    ax_hel.set_title('STRUCK Helicity-Latched Gated Charge (HEL::scaler)', fontsize=11)
    ax_hel.set_ylabel('Cumulative STRUCK FCupgated', fontsize=11)
    ax_hel.set_xlabel(hel_xlabel if has_hel else xlabel, fontsize=11, loc='right')
    ax_hel.grid(True, linestyle='--', alpha=0.6)
    ax_hel.tick_params(axis='both', labelsize=10)

    # Bottom: running charge asymmetry (Q+ - Q-)/(Q+ + Q-) vs chunk number
    ax_asym.axhline(0.0, color='black', linestyle='--', linewidth=1)
    if has_hel:
        ax_asym.plot(hel_bin_idx, hel_asym['u'], label='Ungated asymmetry', color='darkorange', marker='o', markersize=3)
        ax_asym.plot(hel_bin_idx, hel_asym['g'], label='Gated asymmetry',   color='teal',       marker='d', markersize=3)
        ax_asym.legend(fontsize=10)
    else:
        ax_asym.text(0.5, 0.5, 'No HEL::scaler data', ha='center', va='center', transform=ax_asym.transAxes)
    ax_asym.set_title('Running Charge Asymmetry  (Q+ - Q-)/(Q+ + Q-)', fontsize=11)
    ax_asym.set_ylabel('Asymmetry', fontsize=11)
    ax_asym.set_ylim(-0.02, 0.02)
    ax_asym.set_xlabel(hel_xlabel if has_hel else xlabel, fontsize=11, loc='right')
    ax_asym.grid(True, linestyle='--', alpha=0.6)
    ax_asym.tick_params(axis='both', labelsize=10)

    fig2.tight_layout(rect=[0, 0, 1, 0.985])
    fig2.savefig(f'{output_dir}/chunked_fcupgated_comparison_{file_basename}_{output_suffix}.png', bbox_inches='tight', dpi=300)
    plt.close(fig2)

    print(f'TOTAL UNGATED CHARGE = {fcups[-1]-fcups[0]}')
    print(f'TOTAL   GATED CHARGE = {fcupgateds[-1]-fcupgateds[0]}')

    if has_hel:
        g = hel_tot['g']
        print(f'HELICITY-LATCHED GATED CHARGE: '
              f"hel(-1)={g['-1']}, hel(+1)={g['+1']}, hel(0)={g['0']}, total={g['total']}")


if __name__ == "__main__":
    main()
    print("charge_analysis completed.")
