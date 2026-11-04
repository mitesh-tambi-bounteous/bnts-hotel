"use client";

import { createPaymentOrder, submitCheckout } from "@/app/checkout/actions";
import { CheckoutForm } from "@/components/checkout-form";
import { collectPayment } from "@/lib/razorpay-checkout";

type Props = Pick<React.ComponentProps<typeof CheckoutForm>, "hotel" | "stay" | "nights" | "rooms">;

/** Wires the checkout form to the server actions and Razorpay's browser window. */
export function CheckoutScreen(props: Props) {
  return <CheckoutForm {...props} createOrder={createPaymentOrder} collectPayment={collectPayment} submit={submitCheckout} />;
}
