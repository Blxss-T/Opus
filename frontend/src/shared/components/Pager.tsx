import type { PageMeta } from "../../api/types";
import { Button } from "./Button";

interface PagerProps {
  page: PageMeta;
  onPrevious: () => void;
  onNext: () => void;
}

export function Pager({ page, onPrevious, onNext }: PagerProps) {
  const totalPages = Math.max(page.totalPages, 1);
  return (
    <div className="mt-3 flex items-center gap-3">
      <Button variant="secondary" onClick={onPrevious} disabled={page.number <= 0}>
        Previous
      </Button>
      <span className="text-small text-muted tabular-nums">
        Page {page.number + 1} of {totalPages}
      </span>
      <Button variant="secondary" onClick={onNext} disabled={page.number + 1 >= totalPages}>
        Next
      </Button>
    </div>
  );
}
