# File Registry - NLU Module v12.0.0

## File Naming Conventions

### Documentation Files
| Type | Pattern | Example |
|------|---------|---------|
| Living Docs | `LD-NLU-{Desc}-V#.md` | `LD-NLU-Module-State-V1.md` |
| Specs | `NLU-Spec-{Feature}-YDDMM-V#.md` | `NLU-Spec-Intent-51212-V1.md` |
| Plans | `NLU-Plan-{Feature}-YDDMM-V#.md` | `NLU-Plan-Training-51212-V1.md` |

### Code Files
| Type | Pattern | Example |
|------|---------|---------|
| Kotlin | `{Component}.kt` | `IntentRecognizer.kt` |
| Python (ML) | `{script}.py` | `train_model.py` |
| Models | `{model_name}_v{version}.{ext}` | `intent_classifier_v1.pkl` |

### Configuration Files
| Type | Pattern | Location |
|------|---------|----------|
| Module Config | `config.idc` | `.ideacode/config.idc` |
| Training Config | `training_config.yaml` | `training/` |

## Prohibited Patterns

❌ `NLU-NLU-*.md` (redundant module name)
❌ `*.json` for config (use .idc format for IDEACODE configs)
❌ Spaces in filenames

---

Updated: 2025-12-15 | Version: 12.0.0
