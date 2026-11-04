import { afterEach, describe, expect, it, vi } from "vitest";
import { collectPayment, PaymentCancelled } from "@/lib/razorpay-checkout";

const prefill = { name: "Asha Rao", email: "asha@example.com", phone: "" };

afterEach(() => {
  vi.unstubAllGlobals();
  delete (window as { Razorpay?: unknown }).Razorpay;
});

describe("collectPayment", () => {
  it("skips the Razorpay window when the server has no key (local, in-memory payments)", async () => {
    const proof = await collectPayment({ orderId: "order_fake_1", amountPaise: 3_480_000, keyId: "" }, prefill);

    expect(proof.orderId).toBe("order_fake_1");
    expect(proof.paymentId).toMatch(/^pay_fake_/);
  });

  it("opens Razorpay with the server's order and returns its proof", async () => {
    const options: Record<string, unknown>[] = [];
    (window as { Razorpay?: unknown }).Razorpay = class {
      constructor(opts: Record<string, unknown>) {
        options.push(opts);
        this.opts = opts;
      }
      opts: Record<string, unknown>;
      on() {}
      open() {
        (this.opts.handler as (r: unknown) => void)({
          razorpay_order_id: "order_1",
          razorpay_payment_id: "pay_1",
          razorpay_signature: "sig_1",
        });
      }
    };

    const proof = await collectPayment({ orderId: "order_1", amountPaise: 3_480_000, keyId: "key_test" }, prefill);

    expect(proof).toEqual({ orderId: "order_1", paymentId: "pay_1", signature: "sig_1" });
    expect(options[0]).toMatchObject({ key: "key_test", order_id: "order_1", amount: 3_480_000, currency: "INR" });
    expect(options[0].prefill).toEqual({ name: "Asha Rao", email: "asha@example.com", contact: "" });
  });

  it("rejects with PaymentCancelled when the guest closes the window", async () => {
    (window as { Razorpay?: unknown }).Razorpay = class {
      constructor(private opts: { modal: { ondismiss: () => void } }) {}
      on() {}
      open() {
        this.opts.modal.ondismiss();
      }
    };

    await expect(
      collectPayment({ orderId: "order_1", amountPaise: 100, keyId: "key_test" }, prefill),
    ).rejects.toBeInstanceOf(PaymentCancelled);
  });
});
