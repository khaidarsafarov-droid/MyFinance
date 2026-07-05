import { test, expect } from "@playwright/test";

/**
 * E2E tests for load management.
 * For full auth flow tests (add/delete load), use test credentials in .env.test:
 * E2E_TEST_EMAIL, E2E_TEST_PASSWORD
 */
/**
 * Route protection tests: middleware redirects unauthenticated users to /login.
 * Requires valid Supabase env (NEXT_PUBLIC_SUPABASE_URL, NEXT_PUBLIC_SUPABASE_ANON_KEY).
 */
test.describe("Route protection", () => {
  test.skip("home redirects to login when not logged in", async ({ page }) => {
    await page.goto("/", { waitUntil: "commit" });
    await expect(page).toHaveURL(/\/login/, { timeout: 15000 });
  });

  test.skip("analytics redirects to login when not logged in", async ({ page }) => {
    await page.goto("/analytics", { waitUntil: "commit" });
    await expect(page).toHaveURL(/\/login/, { timeout: 15000 });
  });

  test.skip("settings redirects to login when not logged in", async ({ page }) => {
    await page.goto("/settings", { waitUntil: "commit" });
    await expect(page).toHaveURL(/\/login/, { timeout: 15000 });
  });
});
