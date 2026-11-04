import type { PaymentOrder } from "@/lib/api";
import type { PaymentProof } from "@/lib/checkout";

export class PaymentCancelled extends Error {
  constructor() {
    super("Payment cancelled");
  }
}

type RazorpayResponse = { razorpay_order_id: string; razorpay_payment_id: string; razorpay_signature: string };

type RazorpayOptions = {
  key: string;
  order_id: string;
  amount: number;
  currency: string;
  name: string;
  prefill: { name: string; email: string; contact: string };
  handler: (response: RazorpayResponse) => void;
  modal: { ondismiss: () => void };
};

type RazorpayInstance = { open(): void; on(event: string, listener: (response: unknown) => void): void };
type RazorpayConstructor = new (options: RazorpayOptions) => RazorpayInstance;

declare global {
  interface Window {
    Razorpay?: RazorpayConstructor;
  }
}

const SCRIPT_URL = "https://checkout.razorpay.com/v1/checkout.js";

function loadScript(): Promise<void> {
  if (window.Razorpay) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = SCRIPT_URL;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Couldn't load the payment window"));
    document.body.appendChild(script);
  });
}

/**
 * Takes the payment in Razorpay's own window, so card details never touch this app or our API.
 * With no key from the server (local development, in-memory payments) it returns a fake proof instead.
 */
export async function collectPayment(
  order: PaymentOrder,
  prefill: { name: string; email: string; phone: string },
): Promise<PaymentProof> {
  if (!order.keyId) {
    return { orderId: order.orderId, paymentId: `pay_fake_${Date.now()}`, signature: "fake" };
  }
  await loadScript();
  return new Promise<PaymentProof>((resolve, reject) => {
    const Razorpay = window.Razorpay!;
    const checkout = new Razorpay({
      key: order.keyId,
      order_id: order.orderId,
      amount: order.amountPaise,
      currency: "INR",
      name: "Guest Web",
      prefill: { name: prefill.name, email: prefill.email, contact: prefill.phone },
      handler: (response) =>
        resolve({
          orderId: response.razorpay_order_id,
          paymentId: response.razorpay_payment_id,
          signature: response.razorpay_signature,
        }),
      modal: { ondismiss: () => reject(new PaymentCancelled()) },
    });
    checkout.on("payment.failed", () => reject(new Error("Payment failed")));
    checkout.open();
  });
}
