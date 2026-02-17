# AdviceEngineV2 Input Quality + Warnings (2026-02-18)

## Context
AdviceEngineV2 previously crashed when evidence maps contained null values. The core fix is already in place. This change makes input degradation explicit and traceable by standardizing warnings for missing inputs and by avoiding nulls in evidence/metadata.

## Design
- Add `AdviceInputQuality` to normalize inputs and emit missing-input warnings.
- Add `WarningCollector` to collect warnings with optional detail and expose both code list and detail list.
- Add `SafeValue` to prevent null values from flowing into evidence/metadata.
- Keep all calculations unchanged; only add visible warnings and safe placeholders.

## Changed files
- `backend/src/main/java/com/fincoach/core/healthv2/advice/AdviceWarningCodes.java`
- `backend/src/main/java/com/fincoach/core/healthv2/advice/WarningCollector.java`
- `backend/src/main/java/com/fincoach/core/healthv2/advice/SafeValue.java`
- `backend/src/main/java/com/fincoach/core/healthv2/advice/AdviceInputQuality.java`
- `backend/src/main/java/com/fincoach/core/healthv2/advice/AdviceEngineV2.java`
- `backend/src/test/java/com/fincoach/core/healthv2/advice/AdviceEngineCashflowDebtTest.java`
- `doc/logs-archive/README.md`

## How to verify
- `cd backend && mvn -q clean test`
- Result: `Tests run: 49, Failures: 0, Errors: 0, Skipped: 0`
- Status: `BUILD SUCCESS` (ByteBuddy/CDS warnings can be ignored)

## Rollback
- `git revert <commit_sha>`

## Known limitations
- Warnings are added as codes + optional detail in `adviceV2.meta`; no UI changes included.
- Some warnings may still be emitted from upstream registries (score rules / rebalance template).
