# Ops Log: M8-3 Advice Engine v2

**Date:** 2026-02-17  
**Status:** Completed

## 1. Goal
Deliver Advice Engine v2 with:
- Cashflow + Debt based suggestions
- Rebalance template + hot reload
- Explainable advice output (DTO + meta)
- Admin debug shows template + thresholds

## 2. DoD
- [x] Rebalance template table + seed
- [x] Registry snapshot + reload
- [x] Admin APIs with RBAC
- [x] Advice Engine v2 (cashflow/debt + rebalance)
- [x] Debug output includes template + thresholds
- [x] Tests + `mvn clean test` PASS

## 3. Execution Log
### Step A: Locate key files
- Score rules registry + report generation + debug DTO/mapper + RBAC seeds

### Step B: DB Migration
- Added `fc_rebalance_template` table
- Seeded BALANCED template
- Extended score rule params for advice thresholds

### Step C: Registry + Service
- `RebalanceTemplateRegistry` with AtomicReference snapshot
- Publish triggers reload

### Step D: Admin API
- `/api/admin/rebalance-templates/*` with RBAC

### Step E: Advice Engine v2
- Added Advice DTO + meta output
- Rules: emergency fund, debt ratio, surplus rate, rebalance

### Step F: Debug
- Admin debug includes rebalance template + thresholds + warnings

### Step G: Tests
- Advice engine tests
- Rebalance deviation test
- Admin controller RBAC tests

## 4. Verification
- `cd backend && mvn clean test`
