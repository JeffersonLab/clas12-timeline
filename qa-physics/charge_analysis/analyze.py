#!/usr/bin/env python3
##################################################################################
# ADAPTED FROM BHAWANI SINGH's ORIGINAL SCRIPT:
#   /w/hallb-scshelf2102/clas12/singh/Softwares/QADB_studies/python/main2.py
##################################################################################
import numpy as np
import os
import sys
import logging
from glob import glob
import matplotlib.pyplot as plt
import hipolib

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# plt.style.use('seaborn-darkgrid')

def main():

    if len(sys.argv) != 3:
        print(f'''
USAGE: {sys.argv[0]} [INPUT_HIPO_FILE] [OUTPUT_FILE_SUFFIX]
    INPUT_HIPO_FILE       input HIPO file
    OUTPUT_FILE_SUFFIX    append this string to the output
                          file name; useful if you are comparing
                          output files before and after reheating
        ''')
        exit(2)
    hipo_file = sys.argv[1]

    hipo_prefix = os.getenv('HIPO')
    if hipo_prefix == None:
        raise ValueError("HIPO env var not set")

    logger.info(f"Processing file: {hipo_file}")

    reader = hipolib.hreader(f'{hipo_prefix}/lib')
    reader.open_with_tag(hipo_file, 1)  # filter by tag at open time
    reader.define('RUN::config')
    reader.define('RUN::scaler')

    timestamps, fcups, fcupgateds, live_times = [], [], [], []

    counter = 0
    while reader.next():
        if counter % 10000 == 0 and counter > 0:
            logger.info(f'Processing event # {counter}')
        counter += 1

        if reader.getSize('RUN::config') == 0 or reader.getSize('RUN::scaler') == 0:
            # logger.warning(f"Skipping empty bank at event {counter}")
            continue

        timestamp = reader.getEntry('RUN::config', 'timestamp')
        fcup = reader.getEntry('RUN::scaler', 'fcup')
        fcupgated = reader.getEntry('RUN::scaler', 'fcupgated')
        live_time = reader.getEntry('RUN::scaler', 'livetime')

        timestamps.append(timestamp[0])
        fcups.append(fcup[0])
        fcupgateds.append(fcupgated[0])
        live_times.append(live_time[0])

    logger.info(f"Processed {counter} events.")

    # Sort data by timestamps
    sorted_data = sorted(zip(timestamps, fcups, fcupgateds, live_times))
    if not sorted_data:
        raise ValueError("No data to plot.")

    timestamps, fcups, fcupgateds, live_times = zip(*sorted_data)

    timestamps = np.array(timestamps)
    fcups = np.array(fcups)
    fcupgateds = np.array(fcupgateds)
    live_times = np.array(live_times)

    run_number = os.path.splitext(os.path.basename(hipo_file))[0]

    # ---------- Plot 1: Per-event data ----------
    # ---------- Plot 1: Per-event data ----------
    fig1, axs1 = plt.subplots(2, 2, figsize=(14, 8))
    fig1.suptitle(f'Run {run_number} - Event-Level Detector Data', fontsize=16)

    plots1 = [
        (axs1[0, 0], fcups, 'FCUP', 'FCUP vs Timestamp', 'darkgreen', 'line'),
        (axs1[0, 1], fcupgateds, 'FCUP Gated', 'FCUP Gated vs Timestamp', 'darkorange', 'line'),
        (axs1[1, 0], live_times, 'Live Time', 'Live Time vs Timestamp', 'purple', 'scatter'),
        (axs1[1, 1], fcups * live_times, 'FCUP × Live Time', 'FCUP × Live Time vs Timestamp', 'steelblue', 'line'),
    ]

    for ax, data, label, title, color, style in plots1:
        if style == 'line':
            ax.plot(timestamps, data, label=label, color=color, linewidth=1.5)
        elif style == 'scatter':
            ax.scatter(timestamps, data, label=label, color=color, s=10, alpha=0.7)

        ax.set_title(title, fontsize=12)
        ax.set_xlabel('Timestamp', fontsize=10)
        ax.set_ylabel(label, fontsize=10)
        ax.legend(fontsize=9)
        ax.grid(True, linestyle='--', alpha=0.6)
        ax.tick_params(axis='both', labelsize=9)

    fig1.tight_layout(rect=[0, 0.03, 1, 0.95])
    fig1.savefig(f'fcup_vs_timestamp_{run_number}.png', bbox_inches='tight', dpi=300)
    plt.close(fig1)
    # ---------- Compute Chunked FCUP Gated with neighbor handling ----------
    chunk_size = 2000
    num_chunks = len(timestamps) // chunk_size

    chunk_caseA, chunk_caseB, chunk_caseC, chunk_default = [], [], [], []
    cum_caseA, cum_caseB, cum_caseC, cum_default = [], [], [], []
    chunk_indices, skipped_counts = [], []

    runA, runB, runC, runDef = 0, 0, 0, 0
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

        sumA, sumB, sumC, sumDef = 0, 0, 0, 0
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
                else:
                    skipped_in_chunk += 1
                    total_skipped += 1
                    logger.warning(
                        f"No valid positive LT neighbor at chunk {i}, local index {j}, "
                        f"timestamp {timestamps[start+1+j]}"
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

        chunk_caseA.append(sumA)
        chunk_caseB.append(sumB)
        chunk_caseC.append(sumC)
        chunk_default.append(sumDef)
        cum_caseA.append(runA)
        cum_caseB.append(runB)
        cum_caseC.append(runC)
        cum_default.append(runDef)
        chunk_indices.append(i)
        skipped_counts.append(skipped_in_chunk)

    logger.info(f"Computed chunked FCUP Gated values with neighbor handling (Cases A, B, C).")
    logger.info(f"Total skipped events (no valid LT neighbor): {total_skipped}")

    # ---------- Plot 2: Chunked FCUP Gated + Ratios + Skips + LT Distribution ----------
    fig2, (ax_top, ax_mid, ax_bottom, ax_ltdist) = plt.subplots(
        4, 1, figsize=(12, 14), sharex=False,
        gridspec_kw={'height_ratios': [3, 1, 1, 2]}
    )
    fig2.suptitle(f'Run {run_number} - Chunked FCUP Gated (Neighbor Handling)', fontsize=16)

    # Top: cumulative sums
    ax_top.plot(chunk_indices, cum_caseA, label='Cumulative Case A (LT_nn × FCUPungated)', color='darkred', marker='o')
    #ax_top.plot(chunk_indices, cum_caseB, label='Cumulative Case B (LT_nn × FCUPungated_nn)', color='darkgreen', marker='s')
    ax_top.plot(chunk_indices, cum_caseC, label='Cumulative Case C (20-NN mean × FCUPungated)', color='darkorange', marker='d')
    ax_top.plot(chunk_indices, cum_default, label='Cumulative Default (FCUPgated)', color='blue', marker='^')
    ax_top.set_ylabel('Cumulative Σ', fontsize=11)
    ax_top.grid(True, linestyle='--', alpha=0.6)
    ax_top.legend(fontsize=10)
    ax_top.tick_params(axis='both', labelsize=10)

    # Middle: ratios wrt default
    ratioA = np.divide(cum_caseA, cum_default, out=np.full_like(cum_caseA, np.nan, dtype=float), where=np.array(cum_default) != 0)
    #ratioB = np.divide(cum_caseB, cum_default, out=np.full_like(cum_caseB, np.nan, dtype=float), where=np.array(cum_default) != 0)
    ratioC = np.divide(cum_caseC, cum_default, out=np.full_like(cum_caseC, np.nan, dtype=float), where=np.array(cum_default) != 0)

    ax_mid.plot(chunk_indices, ratioA, label='Case A / Default', color='darkred', marker='o')
    #ax_mid.plot(chunk_indices, ratioB, label='Case B / Default', color='darkgreen', marker='s')
    ax_mid.plot(chunk_indices, ratioC, label='Case C / Default', color='darkorange', marker='d')
    ax_mid.axhline(1.0, color='black', linestyle='--', linewidth=1)
    ax_mid.set_ylabel('Ratio', fontsize=11)
    ax_mid.grid(True, linestyle='--', alpha=0.6)
    ax_mid.legend(fontsize=10)
    ax_mid.tick_params(axis='both', labelsize=10)

    # Bottom-1: skipped events count
    ax_bottom.bar(chunk_indices, skipped_counts, color='gray', alpha=0.7)
    ax_bottom.set_xlabel(f'Chunk Index (Each = {chunk_size} events)', fontsize=11)
    ax_bottom.set_ylabel('# Skipped', fontsize=11)
    ax_bottom.grid(True, linestyle='--', alpha=0.6)
    ax_bottom.tick_params(axis='both', labelsize=10)

    # Bottom-2: livetime distributions
    bins = 100
    mean_raw, sigma_raw = np.mean(live_times), np.std(live_times)
    mean_A, sigma_A = np.mean(corrected_livetimes_A), np.std(corrected_livetimes_A)
    mean_B, sigma_B = np.mean(corrected_livetimes_B), np.std(corrected_livetimes_B)
    mean_C, sigma_C = np.mean(corrected_livetimes_C), np.std(corrected_livetimes_C)

    ax_ltdist.hist(live_times, bins=bins, alpha=0.4,
                   label=f'Raw LT (μ={mean_raw:.3f}, σ={sigma_raw:.3f})', color='purple')
    ax_ltdist.hist(corrected_livetimes_A, bins=bins, alpha=0.4,
                   label=f'Case A LT (μ={mean_A:.3f}, σ={sigma_A:.3f})', color='red')
    #ax_ltdist.hist(corrected_livetimes_B, bins=bins, alpha=0.4,
     #              label=f'Case B LT (μ={mean_B:.3f}, σ={sigma_B:.3f})', color='green')
    ax_ltdist.hist(corrected_livetimes_C, bins=bins, alpha=0.4,
                   label=f'Case C LT (μ={mean_C:.3f}, σ={sigma_C:.3f})', color='orange')

    ax_ltdist.set_xlabel('Live Time', fontsize=11)
    ax_ltdist.set_ylabel('Counts', fontsize=11)
    ax_ltdist.legend(fontsize=9)
    ax_ltdist.grid(True, linestyle='--', alpha=0.6)
    ax_ltdist.tick_params(axis='both', labelsize=10)

    fig2.tight_layout(rect=[0, 0.03, 1, 0.95])
    fig2.savefig(f'chunked_fcupgated_comparison_{run_number}.png', bbox_inches='tight', dpi=300)
    plt.close(fig2)


if __name__ == "__main__":
    main()
    logger.info("HipoReader example completed.")
