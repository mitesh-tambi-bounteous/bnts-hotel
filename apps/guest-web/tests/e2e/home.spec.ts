import { expect, test } from "@playwright/test";

test("home page shows welcome heading", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Welcome to our hotel" })).toBeVisible();
});
