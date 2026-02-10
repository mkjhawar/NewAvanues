from __future__ import annotations

import csv
from collections import Counter
from pathlib import Path


ROOT = Path('/Volumes/M-Drive/Coding/NewAvanues')
RECOVERED_CANDIDATES = ROOT / 'contextsave/merge_review_candidates_2026-02-10-wave4_recovered.csv'
OUT_DIR = ROOT / 'Docs/project-info/execution-artifacts/2026-02-10-wave6-recovery'

WAVE5_LABEL = '2026-02-10-wave5d-recovered'

# Deterministic exclusions from previously completed wave6 batches 1-4
EXCLUDED_TOPICS_BATCH1_TO_4 = {
    'todo',
    'spec',
    'changelog',
    'plan',
    'voiceos-developer-manual',
    'tasks',
    'voiceos-spec',
    'voiceos-user-manual',
    'voiceos-changelog-2025-10',
    'developer-manual',
    'project-status',
    'voiceos-plan',
    '00-index',
    'voiceos-current-status',
    'reorganization-plan',
}


def load_recovered_rows() -> list[dict[str, str]]:
    with RECOVERED_CANDIDATES.open(newline='') as f:
        return list(csv.DictReader(f))


def is_voiceos_path(path: str) -> bool:
    return '/Docs/VoiceOS/' in path


def build_wave5d_recovered(rows: list[dict[str, str]]) -> list[dict[str, str]]:
    out: list[dict[str, str]] = []
    for r in rows:
        source_path = (r.get('source_path') or '').strip()
        bucket = 'B' if is_voiceos_path(source_path) else 'OUTSIDE_B'
        action = 'needs_manual_review' if bucket == 'B' else 'skipped_bucket_filter'
        reason = (
            'Recovered queue item in Docs/VoiceOS scope; staged for manual review.'
            if action == 'needs_manual_review'
            else 'Outside Docs/VoiceOS scope for wave5d bucket-B run.'
        )
        out.append(
            {
                'label': WAVE5_LABEL,
                'normalized_topic': (r.get('normalized_topic') or '').strip(),
                'source_path': source_path,
                'suggested_canonical': (r.get('suggested_canonical') or '').strip(),
                'bucket': bucket,
                'action': action,
                'reason': reason,
            }
        )
    return out


def write_csv(path: Path, rows: list[dict[str, str]], fieldnames: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def write_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)


def summarize_topic_batch(rows: list[dict[str, str]], excluded_topics: set[str]) -> tuple[list[str], list[dict[str, str]], int]:
    remaining = [r for r in rows if (r.get('normalized_topic') or '') not in excluded_topics]
    topic_counts = Counter((r.get('normalized_topic') or '') for r in remaining)
    selected_topics = [t for t, _ in topic_counts.most_common(5)]
    selected_rows = [r for r in remaining if (r.get('normalized_topic') or '') in selected_topics]
    return selected_topics, selected_rows, len(remaining)


def main() -> None:
    if not RECOVERED_CANDIDATES.exists():
        raise FileNotFoundError(RECOVERED_CANDIDATES)

    recovered_rows = load_recovered_rows()
    wave5_rows = build_wave5d_recovered(recovered_rows)

    execution_csv = OUT_DIR / f'merge_review_execution_{WAVE5_LABEL}.csv'
    execution_summary = OUT_DIR / f'merge_review_execution_{WAVE5_LABEL}_summary.txt'

    fieldnames = [
        'label',
        'normalized_topic',
        'source_path',
        'suggested_canonical',
        'bucket',
        'action',
        'reason',
    ]
    write_csv(execution_csv, wave5_rows, fieldnames)

    manual_rows = [r for r in wave5_rows if r.get('action') == 'needs_manual_review']
    skipped_rows = [r for r in wave5_rows if r.get('action') == 'skipped_bucket_filter']

    execution_summary_text = '\n'.join(
        [
            f'label={WAVE5_LABEL}',
            f'candidate_rows={len(wave5_rows)}',
            f'needs_manual_review={len(manual_rows)}',
            f'skipped_bucket_filter={len(skipped_rows)}',
            'archived=0',
            'errors=0',
            f'execution_csv={execution_csv}',
        ]
    ) + '\n'
    write_text(execution_summary, execution_summary_text)

    # Continue deterministically from prior completed wave6 batches 1-4
    excluded = set(EXCLUDED_TOPICS_BATCH1_TO_4)
    batch_index = 5
    batch_summaries: list[str] = []

    while True:
        selected_topics, batch_rows, remaining_after_exclusion = summarize_topic_batch(manual_rows, excluded)
        if not batch_rows:
            break

        batch_csv = OUT_DIR / f'merge_review_wave6_voiceos_topic_batch{batch_index}_recovered.csv'
        batch_summary = OUT_DIR / f'merge_review_wave6_voiceos_topic_batch{batch_index}_recovered_summary.txt'
        write_csv(
            batch_csv,
            batch_rows,
            ['label', 'normalized_topic', 'source_path', 'suggested_canonical', 'bucket', 'action', 'reason'],
        )

        counts = Counter((r.get('normalized_topic') or '') for r in batch_rows)
        topic_count_pairs = ', '.join(f'{t} ({counts[t]})' for t in selected_topics)
        summary_text = '\n'.join(
            [
                f'batch=batch{batch_index}',
                f'manual_pool={len(manual_rows)}',
                f'remaining_after_exclusion={remaining_after_exclusion}',
                f'selected_topics={topic_count_pairs}',
                f'batch_rows={len(batch_rows)}',
                f'batch_csv={batch_csv}',
            ]
        ) + '\n'
        write_text(batch_summary, summary_text)

        batch_summaries.append(
            f'batch{batch_index}: rows={len(batch_rows)} | selected_topics={topic_count_pairs} | summary={batch_summary}'
        )

        excluded.update(selected_topics)
        batch_index += 1

    final_summary = OUT_DIR / 'merge_review_wave6_recovery_batch5_to_finish_summary.txt'
    final_text = '\n'.join(
        [
            f'wave5_label={WAVE5_LABEL}',
            f'execution_csv={execution_csv}',
            f'execution_summary={execution_summary}',
            f'manual_pool={len(manual_rows)}',
            f'initial_excluded_topics={len(EXCLUDED_TOPICS_BATCH1_TO_4)}',
            f'batches_generated={len(batch_summaries)}',
            *batch_summaries,
        ]
    ) + '\n'
    write_text(final_summary, final_text)

    print(f'out_dir={OUT_DIR}')
    print(f'execution_csv={execution_csv}')
    print(f'execution_summary={execution_summary}')
    print(f'manual_pool={len(manual_rows)}')
    print(f'batches_generated={len(batch_summaries)}')
    print(f'final_summary={final_summary}')


if __name__ == '__main__':
    main()
