import { Outlet } from "react-router-dom";
import { NavSidebar } from "../nav/NavSidebar";
import { useAuth } from "../auth/useAuth";
import { Button } from "../shared/components/Button";

export function AppShell() {
  const { user, organization, logout } = useAuth();

  return (
    <div className="app-shell">
      <header className="app-shell__topbar">
        <span className="app-shell__org">{organization?.name}</span>
        <span className="app-shell__user">
          {user?.firstName} {user?.lastName} ({user?.role})
        </span>
        <Button variant="secondary" onClick={() => logout()}>
          Log out
        </Button>
      </header>
      <div className="app-shell__body">
        <NavSidebar />
        <main className="app-shell__content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
