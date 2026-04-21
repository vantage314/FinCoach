# UI Color Audit Report

- Generated: 2026-03-01T03:47:29.160Z
- Total findings: **11**
- Allowed rgba(255,255,255,alpha) max alpha: **0.12**
- Strict mode: **ON**

## HIGH (0)

✅ No findings.

## MEDIUM (8)

### 背景使用 rgba(255,255,255,α)（α过大可能变白块） — `bg-white-rgba` (8)

- **src\theme\element-overwrite.scss:1382:4**
  - match: `background-color: rgba(255, 255, 255, 0.15)`
  - context: `========== .el-scrollbar__thumb { background-color: rgba(255, 255, 255, 0.15) !important; &:hover { background-color: rgba(255, 255, 255, 0.25) !important;`
- **src\theme\element-overwrite.scss:1385:8**
  - match: `background-color: rgba(255, 255, 255, 0.25)`
  - context: `) !important; &:hover { background-color: rgba(255, 255, 255, 0.25) !important; } } // ======================== Empty ======================== .el-empty__`
- **src\theme\element-overwrite.scss:1477:8**
  - match: `background-color: rgba(255, 255, 255, 0.15)`
  - context: `ar .el-scrollbar__thumb { background-color: rgba(255, 255, 255, 0.15) !important; &:hover { background-color: rgba(255, 255, 255, 0.25) !impor`
- **src\theme\element-overwrite.scss:1480:12**
  - match: `background-color: rgba(255, 255, 255, 0.25)`
  - context: `tant; &:hover { background-color: rgba(255, 255, 255, 0.25) !important; } } } // ======================== Descriptions（全链路暗色）=============`
- **src\views\asset\Analysis.vue:158:43**
  - match: `background: rgba(255,255,255,0.2)`
  - context: `ore-circle { width: 60px; height: 60px; background: rgba(255,255,255,0.2); border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-direction: column;`
- **src\views\market\SecurityDetail.vue:532:4**
  - match: `background: rgba(255, 255, 255, 0.15)`
  - context: `color: white; &:hover { background: rgba(255, 255, 255, 0.15); } .btn-icon { width: 18px; height: 18px; margin-right: 8px; } } .buy-`
- **src\views\risk\AssessmentResult.vue:374:4**
  - match: `background: rgba(255, 255, 255, 0.2)`
  - context: `x; span { height: 2px; background: rgba(255, 255, 255, 0.2); } } /* 温度计底座 */ .thermometer-bulb { width: 70px; height: 70px; border-radius: 50%;`
- **src\views\risk\AssessmentWizard.vue:300:4**
  - match: `background: rgba(255, 255, 255, 0.15)`
  - context: `der-radius: 12px; &:hover { background: rgba(255, 255, 255, 0.15); border-color: rgba(255, 255, 255, 0.3); } } /* 提交 Loading 遮罩 */ .submit-overlay { pos`

## LOW (3)

### box-shadow 使用纯白发光（可能显得廉价/刺眼） — `shadow-white-glow` (3)

- **src\views\dashboard\Index.vue:563:6**
  - match: `box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1)`
  - context: `255, 255, 255, 0.05) !important; box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important; border-radius: 12px; } :deep(.el-input__inner) {`
- **src\views\dashboard\Index.vue:591:6**
  - match: `box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1)`
  - context: `255, 255, 255, 0.05) !important; box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important; border-radius: 12px; } :deep(.el-select__placeholder`
- **src\views\risk\AssessmentWizard.vue:193:2**
  - match: `box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1)`
  - context: `border: 1px solid var(--fc-border); box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.1); } .question-card { width: 100%; bord`
