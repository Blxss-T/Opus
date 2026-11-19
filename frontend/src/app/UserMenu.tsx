import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { ChevronDown, KeyRound, LogOut } from "lucide-react";
import { useState } from "react";
import { useAuth } from "../auth/useAuth";
import { cn } from "../shared/utils/cn";
import { ChangePasswordModal } from "./ChangePasswordModal";

export function UserMenu() {
  const { user, logout } = useAuth();
  const [passwordOpen, setPasswordOpen] = useState(false);

  if (!user) {
    return null;
  }

  const displayName = `${user.firstName} ${user.lastName}`;
  const initials = `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();

  return (
    <>
      <DropdownMenu.Root>
        <DropdownMenu.Trigger
          className={cn(
            "inline-flex items-center gap-2 rounded-full bg-surface py-1 pr-2 pl-1 text-small font-medium text-heading",
            "hover:bg-chip",
            "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600",
          )}
          aria-label={`${displayName} account menu`}
        >
          <span className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-600 text-caption font-semibold text-white">
            {initials}
          </span>
          <span className="hidden max-w-40 truncate sm:inline">{displayName}</span>
          <ChevronDown size={16} strokeWidth={1.75} className="text-muted" aria-hidden />
        </DropdownMenu.Trigger>
        <DropdownMenu.Portal>
          <DropdownMenu.Content
            align="end"
            sideOffset={8}
            className="z-50 min-w-48 rounded-md border border-border bg-surface p-1 shadow-md"
          >
            <DropdownMenu.Item
              className="flex cursor-pointer items-center gap-2 rounded-sm px-2 py-2 text-small text-heading outline-none data-highlighted:bg-primary-50"
              onSelect={() => setPasswordOpen(true)}
            >
              <KeyRound size={16} strokeWidth={1.75} aria-hidden />
              Change password
            </DropdownMenu.Item>
            <DropdownMenu.Item
              className="flex cursor-pointer items-center gap-2 rounded-sm px-2 py-2 text-small text-heading outline-none data-highlighted:bg-primary-50"
              onSelect={() => {
                void logout();
              }}
            >
              <LogOut size={16} strokeWidth={1.75} aria-hidden />
              Log out
            </DropdownMenu.Item>
          </DropdownMenu.Content>
        </DropdownMenu.Portal>
      </DropdownMenu.Root>
      <ChangePasswordModal open={passwordOpen} onOpenChange={setPasswordOpen} />
    </>
  );
}
