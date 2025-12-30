<<<<<<< HEAD
# IDEACODE Configuration Format v1.0
# Type: IDC
# Extension: .idc
---
schema: idc-1.0
version: 12.0.0
project: newavanues
metadata:
  file: config.idc
  category: configuration
  count: 42
---
PRJ:nav:NewAvanues:monorepo:12.0.0
CFG:framework:/Volumes/M-Drive/Coding/ideacode:12.0.0
CFG:voice_first:true:bool
CFG:auto_verify:true:bool
CFG:visual_verification:false:bool
CFG:default_profile:default:string
PRF:default:50:40:true:false
PRF:strict:30:30:true:true
PRF:prototype:90:80:false:false
PRF:production:40:40:true:true
GAT:test_coverage:90:true
GAT:ipc_coverage:100:true
GAT:api_documentation:100:true
GAT:intent_registration:100:true
THR:technical_debt:70:warn
THR:test_coverage:90:enforce
THR:documentation_coverage:70:recommend
THR:auto_redirect:90:percent
SWM:parallel:5:0.8:true:true:3
PTH:specs:docs/ideacode/specs
PTH:features:docs/ideacode/features
PTH:registries:docs/ideacode/registries
PTH:archive:docs/ideacode/archive-features
PTH:docs:docs
PTH:project-info:docs/project-info
PTH:living-docs:Docs/{Module}/LivingDocs
PTH:module-registries:Modules/{Module}/.ideacode/registries
REG:living-docs:LD-*-V#.md:Docs/{Module}/LivingDocs/
REG:specs:*-Spec-*-YDDMM-V#.md:docs/ideacode/specs/
REG:plans:*-Plan-*-YDDMM-V#.md:docs/ideacode/plans/
FNM:project-context:NAV-Docs-ProjectContext-5121522-V1.md:Docs/project-info/
FNM:architecture:NAV-Docs-Architecture-5121522-V1.md:Docs/project-info/
FNM:api-contracts:NAV-Docs-APIContracts-5121522-V1.md:Docs/project-info/
FNM:ipc-methods:NAV-Docs-IPCMethods-5121522-V1.md:Docs/project-info/
FNM:intent-registry:NAV-Docs-IntentRegistry-5121522-V1.md:Docs/project-info/
MOD:VoiceOS:Modules/VoiceOS:active
MOD:AVA:Modules/AVA:active
MOD:WebAvanue:Modules/WebAvanue:active
MOD:Cockpit:Modules/Cockpit:active
MOD:NLU:Modules/NLU:active
MOD:Common:Common:active
---
aliases:
  cfg: [config, configuration]
  prj: [project]
  prf: [profile]
  gat: [gate, quality_gate]
  thr: [threshold]
  swm: [swarm]
  pth: [path]
  reg: [registry]
  fnm: [file_naming]
  mod: [module]
=======
{
  "vos": "voiceos",
  "version": "10.1",
  "framework_version": "10.1",
  "framework_path": "/Volumes/M-Drive/Coding/ideacode",
  "project": {
    "id": "vos",
    "name": "VoiceOS",
    "type": "voice-os",
    "profile": "android-app",
    "voice_first": true
  },
  "project_info": {
    "local_path": "docs/project-info",
    "required_files": [
      "PROJECT-CONTEXT.md",
      "ARCHITECTURE.md",
      "API-CONTRACTS.md",
      "IPC-METHODS.md",
      "INTENT-REGISTRY.md"
    ],
    "ai_must_reference": true
  },
  "standards": {
    "enabled": true,
    "global_path": "/Volumes/M-Drive/Coding/ideacode/programming-standards",
    "project_path": "docs/ideacode/project-standards",
    "auto_detect": true,
    "always_load": [
      "ideacode-core.md"
    ]
  },
  "file_locations": {
    "specs_location": "docs/ideacode/specs",
    "features_location": "docs/ideacode/features",
    "registries_location": "docs/ideacode/registries",
    "archive_location": "docs/ideacode/archive-features",
    "docs_location": "docs"
  },
  "quality_gates": {
    "test_coverage": {
      "minimum": 90
    },
    "ipc_coverage": 100,
    "api_documentation": 100,
    "intent_registration": 100
  },
  "intelligence": {
    "thresholds": {
      "technicalDebt": 70,
      "testCoverage": 90,
      "documentationCoverage": 70,
      "autoRedirect": 0.90
    }
  },
  "swarm": {
    "parallelization": {
      "conflict_threshold": 0.8,
      "dependency_depth": 3,
      "auto_rollback": true,
      "conflict_monitoring": true,
      "max_parallel_agents": 5
    }
  },
  "author": {
    "name": "Manoj Jhawar",
    "email": "manoj@ideahq.net"
  }
}
>>>>>>> origin/AVA-Development
