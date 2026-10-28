import { greeting } from "@/lib/greeting";

export function Welcome({ name }: { name?: string }) {
  return <h1 className="text-3xl font-semibold">{greeting(name)}</h1>;
}
