#!/usr/bin/env node
/**
 * FinCoach UI Color Audit
 * - Scans source files for white-ish backgrounds and other risky light colors.
 * - Outputs:
 *   - doc/qa/color-audit/color_audit_report.md
 *   - doc/qa/color-audit/color_audit_report.json
 *
 * Usage:
 *   node scripts/color-audit.mjs
 *   node scripts/color-audit.mjs --strict
 *   node scripts/color-audit.mjs --strict --threshold 0
 */

import fs from "node:fs";
import path from "node:path";

const args = new Set(process.argv.slice(2));
const STRICT = args.has("--strict");
const thresholdIdx = process.argv.indexOf("--threshold");
const THRESHOLD =
    thresholdIdx !== -1 ? Number(process.argv[thresholdIdx + 1] ?? "0") : null;

// ---- Config ----
const ROOT = process.cwd();
const INCLUDE_EXT = new Set([".vue", ".ts", ".tsx", ".js", ".jsx", ".scss", ".css"]);
const IGNORE_DIRS = new Set([
    "node_modules",
    "dist",
    "build",
    "coverage",
    ".git",
    "doc/qa/color-audit", // output
]);

// Allowlist: "glass" transparent white is OK if alpha is small.
const ALLOWED_RGBA_WHITE_ALPHA_MAX = 0.12;

// Pattern groups (we report with categories)
const RULES = [
    {
        id: "bg-white-hex",
        title: "背景使用纯白/近白 HEX（#fff/#ffffff/#fefefe/#fdfdfd）",
        severity: "high",
        re: /\b(background(?:-color)?|fill)\s*:\s*#(?:fff(?:fff)?|fefefe|fdfdfd)\b/gi,
    },
    {
        id: "bg-white-keyword",
        title: "背景使用 white/ivory/snow 等关键字",
        severity: "high",
        re: /\b(background(?:-color)?|fill)\s*:\s*(white|snow|ivory)\b/gi,
    },
    {
        id: "bg-white-rgb",
        title: "背景使用 rgb(255,255,255)",
        severity: "high",
        re: /\b(background(?:-color)?|fill)\s*:\s*rgb\(\s*255\s*,\s*255\s*,\s*255\s*\)/gi,
    },
    {
        id: "bg-white-rgba",
        title: "背景使用 rgba(255,255,255,α)（α过大可能变白块）",
        severity: "medium",
        re: /\b(background(?:-color)?|fill)\s*:\s*rgba\(\s*255\s*,\s*255\s*,\s*255\s*,\s*([0-9.]+)\s*\)/gi,
        postCheck: (m) => {
            const alpha = Number(m[2]);
            return Number.isFinite(alpha) && alpha > ALLOWED_RGBA_WHITE_ALPHA_MAX;
        },
    },
    {
        id: "inline-style-white",
        title: "模板 inline style 出现白色（style=\"...\" 或 :style=\"...\"）",
        severity: "high",
        re: /\b(style\s*=\s*["'][^"']*(#fff(?:fff)?|white|rgb\(\s*255\s*,\s*255\s*,\s*255\s*\)|rgba\(\s*255\s*,\s*255\s*,\s*255\s*,\s*[0-9.]+\s*\))[^"']*["'])/gi,
    },
    {
        id: "hardcoded-light-text",
        title: "硬编码深色文字（#111/#000 等）在暗底可读性不稳定（提醒）",
        severity: "low",
        re: /\b(color)\s*:\s*#(?:000000|000|111111|111|222222|222)\b/gi,
    },
    {
        id: "shadow-white-glow",
        title: "box-shadow 使用纯白发光（可能显得廉价/刺眼）",
        severity: "low",
        re: /\bbox-shadow\s*:\s*[^;]*(#fff(?:fff)?|rgba\(\s*255\s*,\s*255\s*,\s*255\s*,\s*[0-9.]+\s*\))/gi,
    },
    {
        id: "bg-legacy-light-token",
        title: "背景误用浅色 Token（background: var(--fc-text) 或 #f1f5f9 等亮色做背景）",
        severity: "high",
        re: /\b(background(?:-color)?)\s*:\s*(var\(--fc-text\)|#f1f5f9|#f8fafc|#e2e8f0|#e5e7eb)\b/gi,
    },
];

function isIgnoredDir(p) {
    const parts = p.split(path.sep);
    return parts.some((seg) => IGNORE_DIRS.has(seg));
}

function walk(dir) {
    const res = [];
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const e of entries) {
        const full = path.join(dir, e.name);
        if (isIgnoredDir(full)) continue;
        if (e.isDirectory()) res.push(...walk(full));
        else if (INCLUDE_EXT.has(path.extname(e.name))) res.push(full);
    }
    return res;
}

function readText(file) {
    try {
        return fs.readFileSync(file, "utf8");
    } catch {
        return "";
    }
}

function lineAt(text, idx) {
    const upto = text.slice(0, idx);
    const line = upto.split(/\r?\n/).length;
    const col = idx - (upto.lastIndexOf("\n") + 1);
    return { line, col };
}

function snippet(text, idx, len = 140) {
    const start = Math.max(0, idx - 40);
    const end = Math.min(text.length, idx + len);
    return text.slice(start, end).replace(/\s+/g, " ").trim();
}

function collectFindings(files) {
    const findings = [];

    for (const file of files) {
        const rel = path.relative(ROOT, file);
        const text = readText(file);
        if (!text) continue;

        for (const rule of RULES) {
            rule.re.lastIndex = 0;
            let m;
            while ((m = rule.re.exec(text))) {
                // If postCheck exists and returns false -> ignore
                if (typeof rule.postCheck === "function" && !rule.postCheck(m)) continue;

                const pos = lineAt(text, m.index);
                findings.push({
                    file: rel,
                    ruleId: rule.id,
                    title: rule.title,
                    severity: rule.severity,
                    line: pos.line,
                    col: pos.col,
                    match: m[0],
                    context: snippet(text, m.index),
                });
            }
        }
    }

    return findings;
}

function groupBy(arr, keyFn) {
    const map = new Map();
    for (const x of arr) {
        const k = keyFn(x);
        if (!map.has(k)) map.set(k, []);
        map.get(k).push(x);
    }
    return map;
}

function ensureDir(p) {
    fs.mkdirSync(p, { recursive: true });
}

function renderMd(findings) {
    const total = findings.length;
    const bySeverity = groupBy(findings, (x) => x.severity);
    const sevOrder = ["high", "medium", "low"];

    const lines = [];
    lines.push(`# UI Color Audit Report`);
    lines.push(``);
    lines.push(`- Generated: ${new Date().toISOString()}`);
    lines.push(`- Total findings: **${total}**`);
    lines.push(`- Allowed rgba(255,255,255,alpha) max alpha: **${ALLOWED_RGBA_WHITE_ALPHA_MAX}**`);
    lines.push(`- Strict mode: **${STRICT ? "ON" : "OFF"}**`);
    lines.push(``);

    for (const sev of sevOrder) {
        const items = bySeverity.get(sev) ?? [];
        lines.push(`## ${sev.toUpperCase()} (${items.length})`);
        lines.push(``);
        if (items.length === 0) {
            lines.push(`✅ No findings.`);
            lines.push(``);
            continue;
        }

        const byRule = groupBy(items, (x) => x.ruleId);
        for (const [ruleId, ruleItems] of byRule.entries()) {
            const ruleTitle = ruleItems[0]?.title ?? ruleId;
            lines.push(`### ${ruleTitle} — \`${ruleId}\` (${ruleItems.length})`);
            lines.push(``);
            // limit per rule in MD, avoid huge output
            const limit = 80;
            const shown = ruleItems.slice(0, limit);

            for (const f of shown) {
                lines.push(`- **${f.file}:${f.line}:${f.col}**`);
                lines.push(`  - match: \`${f.match.replace(/`/g, "\\`").slice(0, 180)}\``);
                lines.push(`  - context: \`${f.context.replace(/`/g, "\\`").slice(0, 220)}\``);
            }
            if (ruleItems.length > limit) {
                lines.push(`- … truncated, see JSON for full list`);
            }
            lines.push(``);
        }
    }

    return lines.join("\n");
}

function shouldFail(findings) {
    const highCount = findings.filter((f) => f.severity === "high").length;
    if (STRICT && highCount > 0) return true;
    if (THRESHOLD !== null && findings.length > THRESHOLD) return true;
    return false;
}

function main() {
    const files = walk(ROOT);

    const hasSrc = fs.existsSync(path.join(ROOT, "src"));
    const scanFiles = hasSrc
        ? files.filter((f) => f.startsWith(path.join(ROOT, "src")))
        : files;

    const findings = collectFindings(scanFiles);

    const outDir = path.join(ROOT, "doc", "qa", "color-audit");
    ensureDir(outDir);

    const md = renderMd(findings);
    fs.writeFileSync(path.join(outDir, "color_audit_report.md"), md, "utf8");
    fs.writeFileSync(
        path.join(outDir, "color_audit_report.json"),
        JSON.stringify(
            {
                generatedAt: new Date().toISOString(),
                strict: STRICT,
                allowedRgbaWhiteAlphaMax: ALLOWED_RGBA_WHITE_ALPHA_MAX,
                total: findings.length,
                findings,
            },
            null,
            2
        ),
        "utf8"
    );

    console.log(`Color audit done. Findings: ${findings.length}`);
    console.log(`- MD:   doc/qa/color-audit/color_audit_report.md`);
    console.log(`- JSON: doc/qa/color-audit/color_audit_report.json`);

    if (shouldFail(findings)) {
        console.error(`❌ Color audit failed (strict/threshold).`);
        process.exit(1);
    } else {
        console.log(`✅ Color audit passed.`);
    }
}

main();
