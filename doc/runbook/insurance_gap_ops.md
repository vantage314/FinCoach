# Insurance Gap Ops Runbook

## Purpose
Provide operational guidance for insurance gap assessment and advice integration.

## Data Definitions
- dependents: number of people financially dependent on the user.
- annual_income: used for life target and premium ratio.
- annual_premium_total: total annual insurance premium paid.
- existing_cover_*: current coverage for medical/accident/CI/life.

## Target Calculation
- lifeTarget = annual_income * target_life_multiplier
- gaps = max(0, target - existing)

## Premium Ratio
- premiumRatio = annual_premium_total / annual_income (if income > 0)
- Warn when ratio >= premium_ratio_warn
- Danger when ratio >= premium_ratio_danger

## Advice Suggestions (priority)
- If dependents > 0: LIFE first
- MEDICAL baseline always
- ACCIDENT next
- CI after that
- If premium ratio is high: suggest reducing premium load
- If gaps exist and ratio low: suggest gradual fill within warn band

## Troubleshooting
- Missing profile data -> INSURANCE_PROFILE_MISSING warning
- Missing income -> INCOME_MISSING_FOR_PREMIUM_RATIO warning
- Verify config defaults in fc_insurance_config (code=DEFAULT)
