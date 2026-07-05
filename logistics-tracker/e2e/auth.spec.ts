import { test, expect } from "@playwright/test";

test.describe("Auth flows", () => {
  test("login page loads and shows form", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByRole("heading", { name: /вход/i })).toBeVisible();
    await expect(page.getByLabel(/email/i)).toBeVisible();
    await expect(page.getByLabel(/пароль/i)).toBeVisible();
    await expect(page.getByRole("button", { name: "Войти", exact: true })).toBeVisible();
  });

  test.skip("unauthenticated user is redirected to login when visiting /", async ({
    page,
  }) => {
    await page.goto("/", { waitUntil: "commit" });
    await expect(page).toHaveURL(/\/login/, { timeout: 15000 });
  });

  test("signup link navigates to signup page", async ({ page }) => {
    await page.goto("/login");
    await page.getByRole("link", { name: /зарегистрироваться/i }).click();
    await expect(page).toHaveURL(/\/signup/);
  });
});
