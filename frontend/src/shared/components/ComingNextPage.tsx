import { Card } from "./Card";
import { EmptyState } from "./EmptyState";

export function ComingNextPage({ message = "This screen is coming next." }: { message?: string }) {
  return (
    <Card className="flex min-h-64 items-center justify-center">
      <EmptyState message={message} />
    </Card>
  );
}
