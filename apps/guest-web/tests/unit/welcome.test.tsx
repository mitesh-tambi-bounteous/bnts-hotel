import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Welcome } from "@/components/welcome";
import { greeting } from "@/lib/greeting";

describe("greeting", () => {
  it("greets by name", () => {
    expect(greeting("Ada")).toBe("Welcome, Ada");
  });
  it("falls back to a default", () => {
    expect(greeting()).toBe("Welcome to our hotel");
  });
});

describe("Welcome", () => {
  it("renders the heading", () => {
    render(<Welcome />);
    expect(screen.getByRole("heading").textContent).toBe("Welcome to our hotel");
  });
});
