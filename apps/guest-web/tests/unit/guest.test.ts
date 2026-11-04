import { describe, expect, it } from "vitest";
import { validateGuest } from "@/lib/guest";

describe("validateGuest", () => {
  it("accepts a name and an email, with no phone", () => {
    expect(validateGuest({ name: "Asha Rao", email: "asha@example.com", phone: "" })).toEqual({});
  });

  it("asks for a name and an email when blank", () => {
    expect(validateGuest({ name: "  ", email: "", phone: "" })).toEqual({
      name: "Enter your full name",
      email: "Enter your email address",
    });
  });

  it("rejects an email that is not an address", () => {
    expect(validateGuest({ name: "Asha", email: "asha@", phone: "" }).email).toBe("Enter a valid email address");
  });
});
