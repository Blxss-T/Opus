export function formatCurrency(amount: number, currency = "USD"): string {
  return new Intl.NumberFormat(undefined, { style: "currency", currency }).format(amount);
}
