import { expect, Page } from '@playwright/test';

type LoginEnv = {
  userKey: string;
  passKey: string;
  expectedUrl: RegExp;
};

const requireEnv = (key: string): string => {
  const value = process.env[key];
  if (!value) {
    throw new Error(`Missing env var: ${key}`);
  }
  return value;
};

const login = async (page: Page, env: LoginEnv) => {
  const username = requireEnv(env.userKey);
  const password = requireEnv(env.passKey);

  await page.goto('/login');
  await page.getByPlaceholder('请输入用户名').fill(username);
  await page.getByPlaceholder('请输入密码').fill(password);
  await page.getByRole('button', { name: '立即登录' }).click();
  await expect(page).toHaveURL(env.expectedUrl);
};

export const loginAdmin = async (page: Page) => {
  await login(page, {
    userKey: 'E2E_ADMIN_USER',
    passKey: 'E2E_ADMIN_PASS',
    expectedUrl: /\/admin\//,
  });
};

export const loginUser = async (page: Page) => {
  await login(page, {
    userKey: 'E2E_USER_USER',
    passKey: 'E2E_USER_PASS',
    expectedUrl: /\/app\//,
  });
};