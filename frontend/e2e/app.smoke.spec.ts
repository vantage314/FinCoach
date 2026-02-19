import { test, expect } from '@playwright/test';
import { loginUser } from './utils/auth';
import { assertNoConsoleErrors, startConsoleErrorTracking } from './utils/console';

test('app smoke: key pages', async ({ page }) => {
  const consoleErrors = startConsoleErrorTracking(page);

  await loginUser(page);

  await page.goto('/app/diagnosis');
  await expect(page.getByText('资产结构透视')).toBeVisible();

  await page.goto('/app/notifications');
  await expect(page.getByRole('heading', { name: '通知中心' })).toBeVisible();
  await expect(page.getByRole('button', { name: '全部已读' })).toBeVisible();

  await page.goto('/app/debt');
  await expect(page.getByRole('heading', { name: '债务管理' })).toBeVisible();
  await expect(page.getByRole('button', { name: '导入 CSV' })).toBeVisible();

  await page.goto('/app/cashflow');
  await expect(page.getByRole('heading', { name: '现金流管理' })).toBeVisible();
  await expect(page.getByRole('button', { name: '导入 CSV' })).toBeVisible();

  await page.goto('/admin/dashboard');
  await expect(page).toHaveURL(/\/403$/);
  await expect(page.getByText('无权限访问')).toBeVisible();

  assertNoConsoleErrors(consoleErrors);
});