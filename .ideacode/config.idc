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
FNM:project-context:PROJECT-CONTEXT.md:docs/project-info/
FNM:architecture:ARCHITECTURE.md:docs/project-info/
FNM:api-contracts:API-CONTRACTS.md:docs/project-info/
FNM:ipc-methods:IPC-METHODS.md:docs/project-info/
FNM:intent-registry:INTENT-REGISTRY.md:docs/project-info/
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
