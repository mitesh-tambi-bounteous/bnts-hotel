import { expect, test, type Page } from "@playwright/test";

const stubApi = "http://localhost:4100";

function isoDate(daysFromNow: number): string {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() + daysFromNow);
  return date.toISOString().slice(0, 10);
}

test.beforeEach(async ({ request }) => {
  await request.post(`${stubApi}/__reset`);
});

async function searchMumbai(page: Page) {
  await page.goto("/");
  await page.getByLabel("City").selectOption("Mumbai");
  await page.getByLabel("Check-in").fill(isoDate(20));
  await page.getByLabel("Check-out").fill(isoDate(22));
  await page.getByLabel("Guests").selectOption("4");
  await page.getByLabel("Rooms").selectOption("2");
  await page.getByRole("button", { name: "Search hotels" }).click();
}

async function chooseTwoRooms(page: Page) {
  await searchMumbai(page);
  await page.getByRole("link", { name: "View rooms" }).first().click();
  await page.getByRole("button", { name: "Select room" }).first().click();
  await page.getByRole("button", { name: "Select room" }).first().click();
  await page.getByRole("link", { name: "Continue to checkout" }).click();
}

async function guest(page: Page, name: string, email: string) {
  await page.getByLabel("Full name").fill(name);
  await page.getByLabel("Email").fill(email);
}

test("search, choose two rooms and book with one reference", async ({ page }) => {
  await searchMumbai(page);

  await expect(page.getByRole("heading", { name: "Hotels in Mumbai" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Harbour View Mumbai" })).toBeVisible();
  await expect(page.getByText("Per night, taxes included").first()).toBeVisible();

  await page.getByRole("link", { name: "View rooms" }).first().click();
  await expect(page.getByRole("heading", { name: "Harbour View Mumbai", level: 1 })).toBeVisible();
  await expect(page.getByRole("tab")).toHaveText(["Overview", "Amenities", "Location"]);
  await page.getByRole("tab", { name: "Amenities" }).click();
  await expect(page.getByText("Free Wi-Fi, rooftop pool")).toBeVisible();

  const suite = page.getByRole("heading", { name: "Harbour suite" }).locator("xpath=ancestor::div[contains(@class,'card')][1]");
  await expect(suite.getByText("Sold out")).toBeVisible();
  await expect(suite.getByRole("button")).toHaveCount(0);
  const compact = page.getByRole("heading", { name: "Compact single room" }).locator("xpath=ancestor::div[contains(@class,'card')][1]");
  await expect(compact.getByText(/sleeps 1, and you're searching 4 guests in 2 rooms/)).toBeVisible();
  await expect(compact.getByRole("button")).toHaveCount(0);

  await page.getByRole("button", { name: "Select room" }).first().click();
  await page.getByRole("button", { name: "Select room" }).first().click();
  await expect(page.getByText("2 rooms, 2 nights, taxes included")).toBeVisible();
  await page.getByRole("link", { name: "Continue to checkout" }).click();

  await expect(page.getByRole("heading", { name: "Checkout" })).toBeVisible();
  await page.getByRole("button", { name: /^Pay/ }).click();
  await expect(page.getByRole("alert").filter({ hasText: "Fix these to continue" })).toBeFocused();
  await expect(page.getByText("Enter your full name").first()).toBeVisible();

  await guest(page, "Asha Rao", "Asha@Example.com");
  await page.getByRole("button", { name: /^Pay/ }).click();

  await expect(page.getByText("Your rooms are booked")).toBeVisible();
  await expect(page.getByText("HB-7K2M9Q")).toHaveCount(1);
  await expect(page.getByText("We've sent your confirmation to asha@example.com.")).toBeVisible();
  await expect(page.getByText(/We've created an account for asha@example.com/)).toBeVisible();

  await page.getByRole("link", { name: "View confirmation email" }).click();
  await expect(page.getByText("Your stay at Harbour View Mumbai is booked")).toBeVisible();
});

test("a booking by an existing guest is linked to their account", async ({ page }) => {
  await chooseTwoRooms(page);
  await guest(page, "Priya Shah", "priya@example.com");
  await page.getByRole("button", { name: /^Pay/ }).click();

  await expect(page.getByText(/linked to your existing account for priya@example.com/)).toBeVisible();
});

test("a room taken at checkout sends the guest back to the room list, uncharged", async ({ page }) => {
  await chooseTwoRooms(page);
  await guest(page, "Asha Rao", "taken@example.com");
  await page.getByRole("button", { name: /^Pay/ }).click();

  await expect(page.getByText("That room is no longer available")).toBeVisible();
  await expect(page.getByText(/You haven't been charged/)).toBeVisible();
  await expect(page.getByText("1 room, 2 nights, taxes included")).toBeVisible();
});

test("when the booking can't be saved the guest is told the payment was reversed", async ({ page }) => {
  await chooseTwoRooms(page);
  await guest(page, "Asha Rao", "fail@example.com");
  await page.getByRole("button", { name: /^Pay/ }).click();

  await expect(page.getByText("We couldn't complete your booking")).toBeVisible();
  await expect(page.getByText(/reversed in full/)).toBeVisible();
  await expect(page.getByRole("button", { name: /^Pay/ })).toBeEnabled();
});

test("search fits a 375px phone with no sideways scroll", async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 800 });
  await searchMumbai(page);

  await expect(page.getByRole("heading", { name: "Hotels in Mumbai" })).toBeVisible();
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(0);
});
