import { NavLink } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import { navItems } from "./navConfig";

export function NavSidebar() {
  const { hasRole } = useAuth();

  return (
    <nav className="nav-sidebar">
      <ul>
        {navItems
          .filter((item) => hasRole(item.requiredRoles))
          .map((item) =>
            item.enabled ? (
              <li key={item.path}>
                <NavLink to={item.path} className={({ isActive }) => (isActive ? "nav-link--active" : undefined)}>
                  {item.label}
                </NavLink>
              </li>
            ) : (
              <li key={item.path} className="nav-link--disabled" title="Coming soon">
                {item.label}
              </li>
            ),
          )}
      </ul>
    </nav>
  );
}
