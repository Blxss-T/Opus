import { Button } from "./Button";
import type { PageMeta } from "../../api/types";

interface PagerProps {
  page: PageMeta;
  onPrevious: () => void;
  onNext: () => void;
}

export function Pager({ page, onPrevious, onNext }: PagerProps) {
  const totalPages = Math.max(page.totalPages, 1);
  return (
    <div className="pager">
      <Button variant="secondary" onClick={onPrevious} disabled={page.number <= 0}>
        Previous
      </Button>
      <span className="pager__status">
        Page {page.number + 1} of {totalPages}
      </span>
      <Button variant="secondary" onClick={onNext} disabled={page.number + 1 >= totalPages}>
        Next
      </Button>
    </div>
  );
}
