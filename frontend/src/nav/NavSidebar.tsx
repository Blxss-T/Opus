import { NavLink, useLocation } from "react-router-dom";
import { motion } from "framer-motion";
import { Building2, PanelLeftClose, PanelLeftOpen } from "lucide-react";
import { useAuth } from "../auth/useAuth";
import { IconButton } from "../shared/components/IconButton";
import { motionEase } from "../shared/motion";
import { cn } from "../shared/utils/cn";
import { navItems } from "./navConfig";
import { UserCard } from "./UserCard";

interface NavSidebarProps {
  collapsed: boolean;
  onToggle?: () => void;
  showToggle?: boolean;
  onNavigate?: () => void;
}

export function NavSidebar({ collapsed, onToggle, showToggle, onNavigate }: NavSidebarProps) {
  const { hasRole, user, organization } = useAuth();
  const location = useLocation();
  const items = navItems.filter((item) => hasRole(item.requiredRoles));

  return (
    <motion.aside
      className="flex h-full shrink-0 flex-col border-r border-border bg-surface"
      animate={{ width: collapsed ? 80 : 260 }}
      transition={motionEase}
      aria-label="Sidebar"
    >
      <div className={cn("flex items-center gap-2 px-3 py-5", collapsed ? "justify-center" : "px-4")}>
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-primary-600 text-white">
          <Building2 size={18} strokeWidth={1.75} aria-hidden />
        </span>
        <span className={cn("text-card text-heading", collapsed && "sr-only")}>Opus</span>
        {showToggle && onToggle && (
          <IconButton
            label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
            className={cn("ml-auto", collapsed && "sr-only")}
            onClick={onToggle}
          >
            {collapsed ? (
              <PanelLeftOpen size={18} strokeWidth={1.75} aria-hidden />
            ) : (
              <PanelLeftClose size={18} strokeWidth={1.75} aria-hidden />
            )}
          </IconButton>
        )}
      </div>
      {showToggle && onToggle && collapsed && (
        <div className="flex justify-center pb-2">
          <IconButton label="Expand sidebar" onClick={onToggle}>
            <PanelLeftOpen size={18} strokeWidth={1.75} aria-hidden />
          </IconButton>
        </div>
      )}

      <nav className="flex-1 overflow-y-auto px-2">
        <ul className="m-0 flex list-none flex-col gap-1 p-0">
          {items.map((item) => {
            const Icon = item.icon;
            const active =
              location.pathname === item.path ||
              (item.path !== "/dashboard" && location.pathname.startsWith(`${item.path}/`));
            return (
              <li key={item.path} className="relative">
                {active && (
                  <motion.span
                    layoutId="nav-active-pill"
                    className="absolute inset-0 rounded-full bg-primary-600"
                    transition={motionEase}
                  />
                )}
                <NavLink
                  to={item.path}
                  onClick={onNavigate}
                  title={collapsed ? item.label : undefined}
                  className={cn(
                    "relative z-10 flex items-center gap-3 rounded-full px-3 py-2.5 text-small font-medium no-underline",
                    active ? "text-white" : "text-heading hover:bg-chip",
                    collapsed && "justify-center px-0",
                  )}
                >
                  <span
                    className={cn(
                      "flex h-8 w-8 shrink-0 items-center justify-center rounded-full",
                      active ? "bg-white/15" : "bg-chip",
                    )}
                  >
                    <Icon size={18} strokeWidth={1.75} aria-hidden />
                  </span>
                  <span className={cn(collapsed && "sr-only")}>{item.label}</span>
                </NavLink>
              </li>
            );
          })}
        </ul>
      </nav>

      {user && (
        <div className="p-3">
          <UserCard user={user} organization={organization} collapsed={collapsed} />
        </div>
      )}
    </motion.aside>
  );
}
