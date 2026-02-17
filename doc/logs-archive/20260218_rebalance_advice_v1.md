# Rebalance Advice V1 (2026-02-18)

## Context
Provide a minimal drift-based rebalance suggestion with warnings and safe degradation. This output is independent of existing scoring/advice formulas.

## Inputs / Outputs
- Inputs: current allocation (from portfolio), target allocation (from rebalance template if present), correlation matrix (optional)
- Output: `portfolio.rebalanceAdviceV1` with threshold, triggered, drifts, actions, warnings

## Trigger logic
- Compute drift = currentWeight - targetWeight
- triggered = max(|drift|) >= threshold
- action: drift > 0 => SELL; drift < 0 => BUY; |drift| < threshold/2 => HOLD

## Degrade / Warnings
- Missing target: use target=current, emit `REBAL_TARGET_MISSING`, triggered=false
- Missing current: emit `REBAL_CURRENT_MISSING`
- If correlation matrix missing and overweight pairs exist: emit `REBAL_CORR_UNAVAILABLE`
- If corr > 0.8 for overweight pairs: emit `REBAL_HIGH_CORR_OVERWEIGHT:{A,B}`

## How to verify
- `cd backend && mvn clean test`
- Latest run: `Tests run: 56, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS`

## Rollback
- `git revert df0e513d 393f535a 0d758bcd 9d7b33cd`
