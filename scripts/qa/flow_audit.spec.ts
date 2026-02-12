import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';

type ConsoleEntry = {
  ts: string;
  level: string;
  text: string;
  location?: string;
  caseId?: string;
};

type NetworkEntry = {
  ts: string;
  caseId?: string;
  type: 'requestfailed' | 'http_error' | 'api_code_error' | 'long_request' | 'response';
  method: string;
  url: string;
  status?: number;
  durationMs?: number;
  failureText?: string;
  responseSnippet?: string;
  code?: number;
};

type KeyResponse = {
  url: string;
  method: string;
  status?: number;
  code?: number;
  bodySnippet?: string;
};

type CaseResult = {
  id: string;
  title: string;
  startAt: string;
  endAt?: string;
  url?: string;
  status: 'pass' | 'fail' | 'blocked' | 'partial';
  expected: string[];
  actual: string[];
  notes: string[];
  errors: string[];
  selectors: string[];
  console: ConsoleEntry[];
  network: NetworkEntry[];
  keyResponses: KeyResponse[];
  screenshot?: string;
};

type RawAudit = {
  meta: {
    date: string;
    baseURL: string;
    frontend: string;
    backend: string;
    runner: string;
    mcpAvailable: boolean;
    headless: boolean;
  };
  cases: CaseResult[];
  console: ConsoleEntry[];
  network: NetworkEntry[];
  summary?: any;
};

const BASE_URL = 'http://localhost:5173';
const API_BASE = 'http://localhost:8080';
const REPO_ROOT = path.resolve(__dirname, '../..');
const OUTPUT_DIR = path.resolve(REPO_ROOT, 'doc/qa');
const SCREENSHOT_DIR = path.resolve(OUTPUT_DIR, 'screenshots/20260212');
const RAW_LOG_PATH = path.resolve(OUTPUT_DIR, '20260212_flow_audit_raw.json');
const REPORT_PATH = path.resolve(OUTPUT_DIR, '20260212_user_flow_audit.md');
const PROGRESS_LOG = path.resolve(OUTPUT_DIR, '20260212_flow_audit_progress.log');

const KEY_ENDPOINTS = [
  '/api/market/securities',
  '/api/market/detail',
  '/api/market/kline',
  '/api/market/finance',
  '/api/market/notices',
  '/api/invest/watchlist',
  '/api/invest/watchlist/toggle',
  '/api/asset/list',
  '/api/asset/summary',
  '/api/asset/add',
  '/api/risk/assess',
  '/api/risk/latest',
  '/api/plan/generate',
  '/api/plan/execute',
  '/api/plan/list',
  '/api/ai/chat',
  '/api/health/check'
];

const nowISO = () => new Date().toISOString();

const ensureDir = (dir: string) => {
  fs.mkdirSync(dir, { recursive: true });
};

const redactText = (text: string) => {
  if (!text) return text;
  return text
    .replace(/Bearer\s+[A-Za-z0-9\-._]+/g, 'Bearer ***')
    .replace(/"token"\s*:\s*"[^"]+"/g, '"token":"***"')
    .replace(/"password"\s*:\s*"[^"]+"/g, '"password":"***"')
    .replace(/"Authorization"\s*:\s*"[^"]+"/g, '"Authorization":"***"');
};

const toSnippet = (input: any) => {
  if (input == null) return undefined;
  const raw = typeof input === 'string' ? input : JSON.stringify(input);
  const trimmed = raw.slice(0, 1000);
  return redactText(trimmed);
};

const isKeyEndpoint = (url: string) => KEY_ENDPOINTS.some((endpoint) => url.includes(endpoint));

const sanitizeUrl = (url: string) => {
  try {
    const u = new URL(url);
    u.search = '';
    return u.toString();
  } catch {
    return url.split('?')[0];
  }
};

test.describe.serial('FinCoach user flow audit', () => {
  test.setTimeout(30 * 60 * 1000);

  test('TC01-TC09', async ({ page, context, browserName }) => {
    ensureDir(OUTPUT_DIR);
    ensureDir(SCREENSHOT_DIR);
    fs.writeFileSync(PROGRESS_LOG, `[QA] start ${nowISO()}\n`, 'utf-8');

    const raw: RawAudit = {
      meta: {
        date: new Date().toISOString().slice(0, 10),
        baseURL: BASE_URL,
        frontend: BASE_URL,
        backend: API_BASE,
        runner: `playwright-${browserName}`,
        mcpAvailable: false,
        headless: true,
      },
      cases: [],
      console: [],
      network: [],
    };

    const requestTimings = new Map<any, number>();
    const requestCounts = new Map<string, number>();
    let currentCase: CaseResult | null = null;
    let lastKnownCode: string | null = null;
    let lastKnownName: string | null = null;
    let loggedIn = false;
    let createdUser: { username: string; password: string } | null = null;

    const attachConsole = (entry: ConsoleEntry) => {
      raw.console.push(entry);
      if (currentCase) {
        currentCase.console.push(entry);
      }
    };

    const attachNetwork = (entry: NetworkEntry) => {
      raw.network.push(entry);
      if (currentCase) {
        currentCase.network.push(entry);
      }
    };

    const addNote = (text: string) => {
      if (currentCase) currentCase.notes.push(text);
    };

    const addActual = (text: string) => {
      if (currentCase) currentCase.actual.push(text);
    };

    const addExpected = (text: string) => {
      if (currentCase) currentCase.expected.push(text);
    };

    const addSelector = (text: string) => {
      if (currentCase) currentCase.selectors.push(text);
    };

    const addError = (text: string) => {
      if (currentCase) currentCase.errors.push(text);
    };

    const takeScreenshot = async (caseId: string, label: string) => {
      const safeLabel = label.replace(/\s+/g, '_').replace(/[^a-zA-Z0-9_\-]/g, '');
      const fileName = `${caseId}_${safeLabel}.png`;
      const filePath = path.join(SCREENSHOT_DIR, fileName);
      await page.screenshot({ path: filePath, fullPage: true });
      if (currentCase) currentCase.screenshot = fileName;
      return fileName;
    };

    const logProgress = (text: string) => {
      fs.appendFileSync(PROGRESS_LOG, text + '\n');
    };

    const runCase = async (id: string, title: string, fn: () => Promise<void>) => {
      console.log(`[QA] start ${id} ${title}`);
      logProgress(`[QA] start ${id} ${title} ${nowISO()}`);
      currentCase = {
        id,
        title,
        startAt: nowISO(),
        status: 'pass',
        expected: [],
        actual: [],
        notes: [],
        errors: [],
        selectors: [],
        console: [],
        network: [],
        keyResponses: [],
      };
      raw.cases.push(currentCase);

      try {
        const caseTimeoutMs = 120000;
        await Promise.race([
          fn(),
          new Promise((_, reject) =>
            setTimeout(() => reject(new Error(`Case timeout ${caseTimeoutMs}ms`)), caseTimeoutMs)
          ),
        ]);
      } catch (error: any) {
        currentCase.status = 'fail';
        addError(error?.message || String(error));
      } finally {
        currentCase.endAt = nowISO();
        currentCase.url = page.url();
        if (!currentCase.screenshot) {
          try {
            await takeScreenshot(id, 'auto');
          } catch {
            // ignore screenshot failures
          }
        }
        console.log(`[QA] end ${id} status=${currentCase.status}`);
        logProgress(`[QA] end ${id} status=${currentCase.status} ${nowISO()}`);
        currentCase = null;
      }
    };

    await context.route('**/*', async (route) => {
      const headers = {
        ...route.request().headers(),
        'Cache-Control': 'no-cache',
        Pragma: 'no-cache',
      };
      await route.continue({ headers });
    });

    page.on('console', (msg) => {
      const level = msg.type();
      if (!['error', 'warning', 'warn'].includes(level)) return;
      const location = msg.location();
      attachConsole({
        ts: nowISO(),
        level,
        text: redactText(msg.text()),
        location: location?.url ? `${location.url}:${location.lineNumber || 0}` : undefined,
        caseId: currentCase?.id,
      });
    });

    page.on('pageerror', (err) => {
      attachConsole({
        ts: nowISO(),
        level: 'pageerror',
        text: redactText(err.message || String(err)),
        caseId: currentCase?.id,
      });
    });

    page.on('request', (request) => {
      requestTimings.set(request, Date.now());
      const key = `${request.method()} ${sanitizeUrl(request.url())}`;
      requestCounts.set(key, (requestCounts.get(key) || 0) + 1);
    });

    page.on('requestfailed', (request) => {
      const start = requestTimings.get(request) || Date.now();
      const duration = Date.now() - start;
      attachNetwork({
        ts: nowISO(),
        caseId: currentCase?.id,
        type: 'requestfailed',
        method: request.method(),
        url: request.url(),
        durationMs: duration,
        failureText: request.failure()?.errorText,
      });
    });

    page.on('response', async (response) => {
      const request = response.request();
      const url = response.url();
      const status = response.status();
      const start = requestTimings.get(request);
      const duration = start ? Date.now() - start : undefined;

      if (duration && duration > 2000) {
        attachNetwork({
          ts: nowISO(),
          caseId: currentCase?.id,
          type: 'long_request',
          method: request.method(),
          url,
          status,
          durationMs: duration,
        });
      }

      if (status >= 400) {
        attachNetwork({
          ts: nowISO(),
          caseId: currentCase?.id,
          type: 'http_error',
          method: request.method(),
          url,
          status,
          durationMs: duration,
        });
      }

      if (!url.includes('/api/')) return;

      const contentType = response.headers()['content-type'] || '';
      let parsed: any = null;
      if (contentType.includes('application/json')) {
        try {
          parsed = await response.json();
        } catch {
          parsed = null;
        }
      }

      const responseSnippet = parsed ? toSnippet(parsed) : undefined;
      if (parsed && typeof parsed.code !== 'undefined' && Number(parsed.code) !== 200) {
        attachNetwork({
          ts: nowISO(),
          caseId: currentCase?.id,
          type: 'api_code_error',
          method: request.method(),
          url,
          status,
          durationMs: duration,
          code: Number(parsed.code),
          responseSnippet,
        });
      }

      if (currentCase && isKeyEndpoint(url)) {
        currentCase.keyResponses.push({
          url: sanitizeUrl(url),
          method: request.method(),
          status,
          code: parsed && typeof parsed.code !== 'undefined' ? Number(parsed.code) : undefined,
          bodySnippet: responseSnippet,
        });
      }
    });

    const ensureVisible = async (locator: any, description: string, timeout = 8000) => {
      addSelector(description);
      try {
        await expect(locator).toBeVisible({ timeout });
        return true;
      } catch (err: any) {
        addError(`定位失败: ${description}`);
        return false;
      }
    };

    const tryLogin = async (username: string, password: string) => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' });
      const userInput = page.getByPlaceholder('请输入用户名');
      const passInput = page.getByPlaceholder('请输入密码');
      const loginButton = page.getByRole('button', { name: '立即登录' });

      const hasUser = await ensureVisible(userInput, '登录页: 用户名输入框');
      const hasPass = await ensureVisible(passInput, '登录页: 密码输入框');
      const hasBtn = await ensureVisible(loginButton, '登录页: 立即登录按钮');
      if (!hasUser || !hasPass || !hasBtn) {
        return { success: false, reason: '登录表单不可用' };
      }

      await userInput.fill(username);
      await passInput.fill(password);
      await loginButton.click();

      try {
        await page.waitForURL('**/dashboard', { timeout: 8000 });
      } catch {
        // ignore, will check token
      }

      const token = await page.evaluate(() => localStorage.getItem('token'));
      if (token) {
        return { success: true };
      }
      return { success: false, reason: '登录后未发现 token' };
    };

    const tryRegister = async () => {
      const suffix = Date.now().toString().slice(-6);
      const username = `qa_user_${suffix}`;
      const password = `QaPass${suffix}`;
      await page.goto(`${BASE_URL}/register`, { waitUntil: 'domcontentloaded' });
      const userInput = page.getByPlaceholder('设置您的用户名');
      const passInput = page.getByPlaceholder('设置登录密码');
      const confirmInput = page.getByPlaceholder('请再次输入密码');
      const submitButton = page.getByRole('button', { name: '注册账户' });

      const hasUser = await ensureVisible(userInput, '注册页: 用户名输入框');
      const hasPass = await ensureVisible(passInput, '注册页: 密码输入框');
      const hasConfirm = await ensureVisible(confirmInput, '注册页: 确认密码输入框');
      const hasBtn = await ensureVisible(submitButton, '注册页: 注册账户按钮');
      if (!hasUser || !hasPass || !hasConfirm || !hasBtn) {
        return { success: false };
      }

      await userInput.fill(username);
      await passInput.fill(password);
      await confirmInput.fill(password);
      await submitButton.click();

      try {
        await page.waitForURL('**/login', { timeout: 8000 });
      } catch {
        return { success: false };
      }

      return { success: true, username, password };
    };

    // TC01
    await runCase('TC01', '首页（未登录）', async () => {
      addExpected('未登录访问首页自动跳转登录页或展示访客态');
      addExpected('页面无空白/报错');

      await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(1000);

      const url = page.url();
      if (url.includes('/login')) {
        addActual('访问首页后自动跳转到登录页');
      } else {
        addActual(`访问首页后停留在 ${url}`);
      }

      const loginVisible = await page.locator('.login-page').isVisible().catch(() => false);
      const topNavVisible = await page.locator('.fixed-header').isVisible().catch(() => false);
      if (loginVisible) {
        addActual('登录页表单可见');
      } else if (topNavVisible) {
        addActual('检测到顶部导航，可能存在访客态');
      } else {
        currentCase!.status = 'partial';
        addError('首页未出现登录表单或导航元素，疑似白屏');
      }

      await takeScreenshot('TC01', 'home');
    });

    // TC02
    await runCase('TC02', '登录', async () => {
      addExpected('登录成功进入首页 /dashboard');
      addExpected('localStorage 写入 token');

      const loginAttempt = await tryLogin('admin', '123456');
      if (!loginAttempt.success) {
        addActual('默认账号 admin/123456 登录失败，尝试注册新账号');
        const regAttempt = await tryRegister();
        if (!regAttempt.success) {
          addError('注册失败，无法继续登录相关用例');
          currentCase!.status = 'fail';
          await takeScreenshot('TC02', 'login_failed');
          return;
        }
        createdUser = { username: regAttempt.username!, password: regAttempt.password! };
        addActual(`注册新账号成功: ${createdUser.username}`);
        const loginAttempt2 = await tryLogin(createdUser.username, createdUser.password);
        if (!loginAttempt2.success) {
          addError('注册后登录仍失败');
          currentCase!.status = 'fail';
          await takeScreenshot('TC02', 'login_failed_after_register');
          return;
        }
      }

      loggedIn = true;
      const token = await page.evaluate(() => localStorage.getItem('token'));
      addActual(`登录成功，token=${token ? '已写入' : '未写入'}`);
      await takeScreenshot('TC02', 'login_success');
    });

    // TC03
    await runCase('TC03', '市场列表（securities）', async () => {
      addExpected('市场列表可加载，分页可用');

      if (!loggedIn) {
        currentCase!.status = 'blocked';
        addError('未登录，无法进入市场页');
        await takeScreenshot('TC03', 'blocked');
        return;
      }

      await page.goto(`${BASE_URL}/market`, { waitUntil: 'domcontentloaded' });
      const table = page.locator('.market-table-section');
      await ensureVisible(table, '市场页: 列表区域');
      await page.waitForTimeout(1500);

      const rows = page.locator('.el-table__body-wrapper tbody tr');
      const rowCount = await rows.count();
      addActual(`市场列表行数: ${rowCount}`);
      if (rowCount === 0) {
        currentCase!.status = 'fail';
        addError('市场列表未渲染数据');
      } else {
        lastKnownName = await rows.first().locator('.stock-name').textContent();
        lastKnownCode = await rows.first().locator('.stock-code').textContent();
        if (lastKnownCode) lastKnownCode = lastKnownCode.trim();
        if (lastKnownName) lastKnownName = lastKnownName.trim();
        addActual(`选取样本标的: ${lastKnownName || '--'} ${lastKnownCode || ''}`);
      }

      await takeScreenshot('TC03', 'market');
    });

    // TC04
    await runCase('TC04', '股票详情 + K线（kline/initChart）', async () => {
      addExpected('进入证券详情页');
      addExpected('K 线区域正常渲染，无 Request failed');

      if (!loggedIn) {
        currentCase!.status = 'blocked';
        addError('未登录，无法进入股票详情');
        await takeScreenshot('TC04', 'blocked');
        return;
      }

      await page.goto(`${BASE_URL}/market`, { waitUntil: 'domcontentloaded' });
      const firstRow = page.locator('.el-table__body-wrapper tbody tr').first();
      const nameCell = firstRow.locator('.name-cell');
      const canClick = await ensureVisible(nameCell, '市场表格: 第一行名称');
      if (!canClick) {
        currentCase!.status = 'fail';
        await takeScreenshot('TC04', 'no_row');
        return;
      }

      await nameCell.click();
      await page.waitForTimeout(1500);

      if (!page.url().includes('/market/detail/')) {
        currentCase!.status = 'fail';
        addError(`未进入详情页，当前 URL=${page.url()}`);
      }

      const chartBox = page.locator('.echart-box');
      const chartVisible = await ensureVisible(chartBox, 'K线图容器');
      if (chartVisible) {
        const canvas = chartBox.locator('canvas');
        const hasCanvas = await canvas.count();
        addActual(`K线图 canvas 数量: ${hasCanvas}`);
        if (hasCanvas === 0) {
          currentCase!.status = 'partial';
          addError('K线容器内未检测到 canvas');
        }
      }

      await takeScreenshot('TC04', 'kline');
    });

    // TC05
    await runCase('TC05', '自选（watchlist 增删查）', async () => {
      addExpected('标的可加入自选并在自选列表中可见');
      addExpected('刷新后仍可见（持久化）');

      if (!loggedIn) {
        currentCase!.status = 'blocked';
        addError('未登录，无法操作自选');
        await takeScreenshot('TC05', 'blocked');
        return;
      }

      if (lastKnownCode) {
        await page.goto(`${BASE_URL}/market/detail/${lastKnownCode}`, { waitUntil: 'domcontentloaded' });
      } else {
        await page.goto(`${BASE_URL}/market`, { waitUntil: 'domcontentloaded' });
        const firstRow = page.locator('.el-table__body-wrapper tbody tr').first();
        lastKnownCode = (await firstRow.locator('.stock-code').textContent())?.trim() || null;
        await firstRow.locator('.name-cell').click();
      }

      await page.waitForTimeout(1200);
      const watchButton = page.getByRole('button', { name: /加自选|已自选/ });
      const hasWatch = await ensureVisible(watchButton, '详情页: 加自选按钮');
      if (!hasWatch) {
        currentCase!.status = 'fail';
        await takeScreenshot('TC05', 'no_watch_button');
        return;
      }

      await watchButton.click();
      await page.waitForTimeout(1000);

      await page.goto(`${BASE_URL}/investment`, { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(1500);

      const watchListItems = page.locator('.sidebar-watchlist .stock-item');
      const watchCount = await watchListItems.count();
      addActual(`自选列表数量: ${watchCount}`);

      let found = false;
      if (lastKnownCode) {
        const codeLocator = page.locator(`.sidebar-watchlist .stock-item .code:text-is("${lastKnownCode}")`);
        found = (await codeLocator.count()) > 0;
      }

      if (!found) {
        currentCase!.status = 'partial';
        addError('自选列表未找到刚添加的标的');
      } else {
        addActual(`自选列表包含标的 ${lastKnownCode}`);
      }

      await page.reload({ waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(1200);
      const watchCountAfter = await page.locator('.sidebar-watchlist .stock-item').count();
      addActual(`刷新后自选列表数量: ${watchCountAfter}`);

      await takeScreenshot('TC05', 'watchlist');
    });

    // TC06
    await runCase('TC06', '资产列表/统计（asset list/summary）', async () => {
      addExpected('资产列表可见');
      addExpected('新增资产后统计刷新');

      if (!loggedIn) {
        currentCase!.status = 'blocked';
        addError('未登录，无法进入资产页');
        await takeScreenshot('TC06', 'blocked');
        return;
      }

      await page.goto(`${BASE_URL}/asset/manage`, { waitUntil: 'domcontentloaded' });
      const addButton = page.getByRole('button', { name: /记一笔/ });
      const ok = await ensureVisible(addButton, '资产管理: 记一笔按钮');
      if (!ok) {
        currentCase!.status = 'fail';
        await takeScreenshot('TC06', 'no_add_button');
        return;
      }

      await addButton.click();
      const dialog = page.locator('.el-dialog');
      await ensureVisible(dialog, '资产管理: 新增资产弹窗');

      const typeItem = dialog.locator('.el-form-item').filter({ hasText: '资产类型' }).first();
      const typeSelect = typeItem.locator('.el-select').first();
      await typeSelect.click({ timeout: 3000, force: true });
      const cashOption = page.getByRole('option', { name: '现金/存款' });
      await ensureVisible(cashOption, '资产类型: 现金/存款');
      await cashOption.click();

      const nameInput = dialog.getByPlaceholder('如: 招商银行持仓');
      await ensureVisible(nameInput, '资产名称输入框');
      await nameInput.fill(`现金资产-${Date.now().toString().slice(-4)}`);

      const amountInput = dialog.locator('.el-input-number input').first();
      await ensureVisible(amountInput, '资产金额输入框');
      await amountInput.fill('5000');

      const confirmBtn = dialog.getByRole('button', { name: '确定' });
      await confirmBtn.click();
      const addResp = await page.waitForResponse(
        (resp) => resp.url().includes('/api/asset/add'),
        { timeout: 8000 }
      ).catch(() => null);
      await page.waitForTimeout(1500);

      if (addResp) {
        let addJson: any = null;
        try {
          addJson = await addResp.json();
        } catch {
          addJson = null;
        }
        if (addJson && typeof addJson.code !== 'undefined' && Number(addJson.code) !== 200) {
          currentCase!.status = 'fail';
          addError(`资产新增失败: code=${addJson.code}, message=${addJson.message || 'unknown'}`);
        }
      } else {
        currentCase!.status = 'partial';
        addError('未捕获到 /api/asset/add 响应');
      }

      await page.goto(`${BASE_URL}/dashboard`, { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(1500);
      const totalAsset = page.locator('.stat-card').first().locator('.stat-value');
      const totalText = await totalAsset.textContent();
      addActual(`资产总览总资产显示: ${totalText?.trim() || '--'}`);

      await takeScreenshot('TC06', 'asset');
    });

    // TC07
    await runCase('TC07', '风险测评 -> 体检闭环（risk->health）', async () => {
      addExpected('风险测评提交后跳转体检页');
      addExpected('体检结果刷新');

      if (!loggedIn) {
        currentCase!.status = 'blocked';
        addError('未登录，无法进入风险测评');
        await takeScreenshot('TC07', 'blocked');
        return;
      }

      try {
        await page.goto(`${BASE_URL}/risk/assessment`, { waitUntil: 'domcontentloaded', timeout: 10000 });
      } catch (err: any) {
        currentCase!.status = 'fail';
        addError(`进入风险测评页失败: ${err?.message || err}`);
        await takeScreenshot('TC07', 'risk_nav_fail');
        return;
      }
      await page.waitForTimeout(800);

      for (let i = 0; i < 10; i++) {
        if (!page.url().includes('/risk/assessment')) break;
        const options = page.locator('.option-btn');
        const count = await options.count();
        if (count === 0) {
          addError('风险测评题目选项未找到');
          break;
        }
        await options.first().click({ force: true, timeout: 2000 });
        await page.waitForTimeout(450);
      }

      try {
        await page.waitForURL('**/diagnosis', { timeout: 8000 });
        addActual('成功跳转到体检页 /diagnosis');
      } catch {
        currentCase!.status = 'partial';
        addError(`未跳转至体检页，当前 URL=${page.url()}`);
      }

      const score = page.locator('.score-num');
      const scoreVisible = await score.isVisible().catch(() => false);
      if (scoreVisible) {
        addActual(`体检分数显示: ${await score.textContent()}`);
      } else {
        currentCase!.status = 'partial';
        addError('体检页未显示健康分数');
      }

      await takeScreenshot('TC07', 'risk_health');
    });

    // TC08
    await runCase('TC08', '计划生成/执行闭环（plan generate/execute）', async () => {
      addExpected('可生成计划并执行');
      addExpected('资产统计刷新');

      if (!loggedIn) {
        currentCase!.status = 'blocked';
        addError('未登录，无法进入计划页');
        await takeScreenshot('TC08', 'blocked');
        return;
      }

      logProgress('[QA] TC08 step: goto /plan');
      await page.goto(`${BASE_URL}/plan`, { waitUntil: 'domcontentloaded', timeout: 10000 });
      const createBtn = page.getByRole('button', { name: '生成新计划' });
      const canCreate = await ensureVisible(createBtn, '计划页: 生成新计划按钮');
      if (!canCreate) {
        currentCase!.status = 'fail';
        await takeScreenshot('TC08', 'no_plan_button');
        return;
      }

      logProgress('[QA] TC08 step: open create dialog');
      await createBtn.click({ timeout: 3000, force: true });
      const dialog = page.locator('.el-dialog');
      await ensureVisible(dialog, '生成计划弹窗');

      logProgress('[QA] TC08 step: fill amount');
      const amountInput = dialog.locator('.el-input-number input').first();
      await ensureVisible(amountInput, '计划投入金额输入框');
      await amountInput.fill('5000');
      const generateBtn = dialog.getByRole('button', { name: /生成 AI 建议/ });
      await ensureVisible(generateBtn, '生成 AI 建议按钮');
      logProgress('[QA] TC08 step: click generate');
      await generateBtn.click({ timeout: 3000, force: true });
      await page.waitForTimeout(2000);

      logProgress('[QA] TC08 step: check plan list');
      const planCards = page.locator('.plan-card');
      const planCount = await planCards.count();
      addActual(`计划列表数量: ${planCount}`);

      if (planCount === 0) {
        currentCase!.status = 'fail';
        addError('计划生成后列表未出现新计划');
        await takeScreenshot('TC08', 'plan_empty');
        return;
      }

      logProgress('[QA] TC08 step: open plan detail');
      await planCards.first().click({ timeout: 3000, force: true });
      const detailDialog = page.locator('.el-dialog');
      await ensureVisible(detailDialog, '计划详情弹窗');
      const executeBtn = detailDialog.getByRole('button', { name: /一键执行计划/ });
      const executeVisible = await executeBtn.isVisible().catch(() => false);
      if (executeVisible) {
        logProgress('[QA] TC08 step: execute plan');
        await executeBtn.click({ timeout: 3000, force: true });
        const confirmBtn = page.getByRole('button', { name: '立即执行' });
        await ensureVisible(confirmBtn, '执行确认按钮');
        await confirmBtn.click({ timeout: 3000, force: true });
        await page.waitForTimeout(2000);
        addActual('计划执行触发');
      } else {
        addActual('计划已执行或执行按钮不可用');
      }

      await takeScreenshot('TC08', 'plan');
    });

    // TC09
    await runCase('TC09', 'AI 咨询（/api/ai/chat）', async () => {
      addExpected('AI 返回 Markdown 文本');
      addExpected('Network 200，无前端报错');

      if (!loggedIn) {
        currentCase!.status = 'blocked';
        addError('未登录，无法进入 AI 咨询');
        await takeScreenshot('TC09', 'blocked');
        return;
      }

      await page.goto(`${BASE_URL}/chat`, { waitUntil: 'domcontentloaded' });
      const textarea = page.getByPlaceholder('问点什么吧，例如：宁德时代现在值得买吗？');
      const sendBtn = page.locator('.send-btn');

      const ok = await ensureVisible(textarea, 'AI 咨询输入框');
      if (!ok) {
        currentCase!.status = 'fail';
        await takeScreenshot('TC09', 'no_input');
        return;
      }

      await textarea.fill('请帮我分析一下 宁德时代 (300750) 的近期走势');
      const responsePromise = page.waitForResponse(
        (resp) => resp.url().includes('/api/ai/chat') && resp.status() === 200,
        { timeout: 10000 }
      ).catch(() => null);
      await sendBtn.click({ timeout: 3000, force: true });
      const aiResponse = await responsePromise;

      const assistantBubble = page.locator('.message-row.assistant .bubble').last();
      let assistantText = await assistantBubble.textContent();
      if (!assistantText || assistantText.trim().length === 0) {
        await page.waitForTimeout(3000);
        assistantText = await assistantBubble.textContent();
      }
      const normalized = (assistantText || '').trim();
      if (!aiResponse) {
        currentCase!.status = 'fail';
        addError('AI 请求未返回 200（可能超时或失败）');
      }

      if (!normalized || normalized.length === 0) {
        currentCase!.status = 'fail';
        addError('AI 回复为空');
      } else if (normalized.startsWith('❌') || normalized.includes('网络请求失败') || normalized.includes('服务暂时不可用')) {
        currentCase!.status = 'fail';
        addError(`AI 回复异常: ${normalized}`);
      } else {
        addActual(`AI 回复长度: ${normalized.length}`);
      }

      await takeScreenshot('TC09', 'ai');
    });

    // Summary + Report
    const total = raw.cases.length;
    const passed = raw.cases.filter((c) => c.status === 'pass').length;
    const failed = raw.cases.filter((c) => c.status === 'fail').length;
    const blocked = raw.cases.filter((c) => c.status === 'blocked').length;
    const partial = raw.cases.filter((c) => c.status === 'partial').length;

    const issues: { id: string; title: string; severity: 'P0' | 'P1' | 'P2'; reason: string }[] = [];
    for (const c of raw.cases) {
      if (c.status === 'pass') continue;
      let severity: 'P0' | 'P1' | 'P2' = 'P1';
      if (c.id === 'TC02') severity = 'P0';
      if (c.status === 'blocked' && ['TC03', 'TC04', 'TC05', 'TC06', 'TC07', 'TC08', 'TC09'].includes(c.id)) {
        severity = 'P0';
      }
      if (c.status === 'partial') severity = 'P2';
      issues.push({
        id: c.id,
        title: c.title,
        severity,
        reason: c.errors.join('; ') || '未满足预期',
      });
    }

    const top5 = issues.slice(0, 5);

    raw.summary = {
      total,
      passed,
      failed,
      blocked,
      partial,
      issues: top5,
      repeatedRequests: Array.from(requestCounts.entries())
        .filter(([, count]) => count >= 10)
        .map(([key, count]) => ({ key, count })),
    };

    fs.writeFileSync(RAW_LOG_PATH, JSON.stringify(raw, null, 2), 'utf-8');

    const mdLines: string[] = [];
    mdLines.push('# FinCoach 用户链路巡检 + Bug 捕获 (2026-02-12)');
    mdLines.push('');
    mdLines.push('## 概览');
    mdLines.push(`- 运行方式: Playwright (MCP 未检测到可用工具，已自动降级)`);
    mdLines.push(`- 前端: ${BASE_URL}`);
    mdLines.push(`- 后端: ${API_BASE}`);
    mdLines.push(`- 通过率: ${passed}/${total}`);
    mdLines.push(`- 失败: ${failed} | 阻塞: ${blocked} | 部分通过: ${partial}`);
    mdLines.push('');

    mdLines.push('## Top 5 问题');
    if (top5.length === 0) {
      mdLines.push('- 无');
    } else {
      for (const issue of top5) {
        mdLines.push(`- ${issue.severity} ${issue.id} ${issue.title}: ${issue.reason}`);
      }
    }
    mdLines.push('');

    mdLines.push('## 用例明细');
    for (const c of raw.cases) {
      mdLines.push(`### ${c.id} ${c.title}`);
      mdLines.push(`- 状态: ${c.status}`);
      mdLines.push(`- URL: ${c.url || '--'}`);
      if (c.expected.length) {
        mdLines.push('- 预期结果:');
        for (const item of c.expected) mdLines.push(`  - ${item}`);
      }
      if (c.actual.length) {
        mdLines.push('- 实际结果:');
        for (const item of c.actual) mdLines.push(`  - ${item}`);
      }
      if (c.errors.length) {
        mdLines.push('- 异常/问题:');
        for (const item of c.errors) mdLines.push(`  - ${item}`);
      }
      if (c.screenshot) {
        mdLines.push(`- 截图: ${path.posix.join('doc/qa/screenshots/20260212', c.screenshot)}`);
      }
      if (c.console.length) {
        mdLines.push('- Console 错误/警告 (前 10 条):');
        for (const entry of c.console.slice(0, 10)) {
          mdLines.push(`  - [${entry.level}] ${entry.text}`);
        }
      }
      if (c.network.length) {
        mdLines.push('- Network 失败/异常 (前 10 条):');
        for (const entry of c.network.slice(0, 10)) {
          mdLines.push(`  - ${entry.type} ${entry.method} ${sanitizeUrl(entry.url)} ${entry.status || ''} ${entry.durationMs ? entry.durationMs + 'ms' : ''}`.trim());
        }
      }
      if (c.keyResponses.length) {
        mdLines.push('- 关键接口响应摘要:');
        for (const resp of c.keyResponses) {
          mdLines.push(`  - ${resp.method} ${resp.url} status=${resp.status ?? '--'} code=${resp.code ?? '--'} body=${resp.bodySnippet ?? '--'}`);
        }
      }
      mdLines.push('');
    }

    mdLines.push('## Bug 清单 (P0/P1/P2)');
    if (issues.length === 0) {
      mdLines.push('- 无');
    } else {
      for (const issue of issues) {
        mdLines.push(`- ${issue.severity} | ${issue.id} ${issue.title}`);
        mdLines.push(`  - 复现步骤: 见 ${issue.id} 用例执行流程`);
        const caseInfo = raw.cases.find((c) => c.id === issue.id);
        if (caseInfo?.screenshot) {
          mdLines.push(`  - 证据: ${path.posix.join('doc/qa/screenshots/20260212', caseInfo.screenshot)}`);
        }
        if (caseInfo?.console.length) {
          mdLines.push(`  - Console: ${caseInfo.console.slice(0, 2).map((c) => c.text).join(' | ')}`);
        }
        if (caseInfo?.network.length) {
          mdLines.push(`  - Network: ${caseInfo.network.slice(0, 2).map((n) => `${n.method} ${sanitizeUrl(n.url)} ${n.status || ''}`).join(' | ')}`);
        }
        mdLines.push(`  - 疑似根因: ${issue.reason}`);
        mdLines.push('  - 建议修复点: 检查前后端对应接口/页面渲染逻辑与权限校验');
      }
    }
    mdLines.push('');

    mdLines.push('## 附录');
    mdLines.push('### 失败请求表 (status>=400 或 requestfailed)');
    const failedReqs = raw.network.filter((n) => ['requestfailed', 'http_error', 'api_code_error'].includes(n.type));
    if (failedReqs.length === 0) {
      mdLines.push('- 无');
    } else {
      for (const entry of failedReqs) {
        mdLines.push(`- ${entry.type} ${entry.method} ${sanitizeUrl(entry.url)} status=${entry.status ?? '--'} duration=${entry.durationMs ?? '--'}ms`);
      }
    }

    mdLines.push('');
    mdLines.push('### Console 错误表');
    if (raw.console.length === 0) {
      mdLines.push('- 无');
    } else {
      for (const entry of raw.console.slice(0, 20)) {
        mdLines.push(`- [${entry.level}] ${entry.text}`);
      }
    }

    mdLines.push('');
    mdLines.push('### 截图清单');
    for (const c of raw.cases) {
      if (c.screenshot) {
        mdLines.push(`- ${c.id}: ${path.posix.join('doc/qa/screenshots/20260212', c.screenshot)}`);
      }
    }

    fs.writeFileSync(REPORT_PATH, mdLines.join('\n'), 'utf-8');
  });
});
