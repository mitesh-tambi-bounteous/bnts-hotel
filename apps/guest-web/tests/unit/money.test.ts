import { describe, expect, it } from "vitest";
import { formatPaise } from "@/lib/money";

describe("formatPaise", () => {
  it("shows whole rupees with Indian grouping", () => {
    expect(formatPaise(780_000)).toBe("₹7,800");
    expect(formatPaise(3_480_000)).toBe("₹34,800");
    expect(formatPaise(12_345_600)).toBe("₹1,23,456");
  });

  it("shows paise only when they are not whole", () => {
    expect(formatPaise(780_050)).toBe("₹7,800.50");
  });
});
