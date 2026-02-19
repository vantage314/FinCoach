import { test, expect } from '@playwright/test';
import { loginAdmin } from './utils/auth';
import { assertNoConsoleErrors, startConsoleErrorTracking } from './utils/console';

test('admin smoke: core routes', async ({ page }) => {
  const consoleErrors = startConsoleErrorTracking(page);

  await loginAdmin(page);

  await page.goto('/admin/dashboard');
  await expect(page.getByRole('heading', { name: '仪表盘' })).toBeVisible();
  await expect(page.getByText('用户数')).toBeVisible();

  await page.goto('/admin/alerts');
  await expect(page.getByRole('heading', { name: '预警管理' })).toBeVisible();
  await expect(page.getByRole('table')).toBeVisible();

  await page.goto('/admin/data-source');
  await expect(page.getByRole('heading', { name: '数据源与抓取' })).toBeVisible();

  await page.goto('/admin/users');
  await expect(page.getByRole('heading', { name: '用户管理' })).toBeVisible();
  await expect(page.getByRole('button', { name: '查询' })).toBeVisible();

  await page.goto('/admin/profile');
  await expect(page.getByRole('heading', { name: '个人中心' })).toBeVisible();
  await expect(page.getByRole('button', { name: '退出登录' })).toBeVisible();

  await page.goto('/app/notifications');
  await expect(page).toHaveURL(/\/(admin\/dashboard|403)$/);

  assertNoConsoleErrors(consoleErrors);
});