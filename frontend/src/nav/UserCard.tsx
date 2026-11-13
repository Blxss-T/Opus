import type { OrganizationResponse, UserResponse } from "../auth/types";
import { RoleBadge } from "../shared/components/RoleBadge";
import { cn } from "../shared/utils/cn";

function initials(user: UserResponse): string {
  return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
}

export function UserCard({
  user,
  organization,
  collapsed,
}: {
  user: UserResponse;
  organization: OrganizationResponse | null;
  collapsed: boolean;
}) {
  return (
    <div
      className={cn(
        "flex items-center gap-3 rounded-lg bg-chip p-3",
        collapsed && "justify-center p-2",
      )}
    >
      <span
        className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary-600 text-caption font-semibold text-white"
        aria-hidden
      >
        {initials(user)}
      </span>
      <div className={cn("min-w-0", collapsed && "sr-only")}>
        <p className="m-0 truncate text-small font-semibold text-heading">
          {user.firstName} {user.lastName}
        </p>
        <p className="m-0 truncate text-caption text-muted">{user.email}</p>
        <div className="mt-1 flex flex-wrap items-center gap-1">
          <RoleBadge role={user.role} />
          {organization && <span className="truncate text-caption text-muted">{organization.name}</span>}
        </div>
      </div>
    </div>
  );
}
