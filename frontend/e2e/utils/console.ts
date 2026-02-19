import { expect, Page } from '@playwright/test';

const DEFAULT_IGNORE_PATTERNS: RegExp[] = [
  /ResizeObserver loop limit exceeded/i,
  /ResizeObserver loop completed with undelivered notifications/i,
];

export const startConsoleErrorTracking = (
  page: Page,
  ignorePatterns: RegExp[] = DEFAULT_IGNORE_PATTERNS
) => {
  const errors: string[] = [];
  const shouldIgnore = (text: string) => ignorePatterns.some((pattern) => pattern.test(text));
  const record = (text: string) => {
    if (!text || shouldIgnore(text)) return;
    errors.push(text);
  };

  page.on('console', (msg) => {
    if (msg.type() !== 'error') return;
    record(msg.text());
  });

  page.on('pageerror', (error) => {
    record(error?.message || String(error));
  });

  return errors;
};

export const assertNoConsoleErrors = (errors: string[]) => {
  expect(errors, `Console errors:\n${errors.join('\n')}`).toEqual([]);
};