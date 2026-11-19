import { useEffect } from "react";
import { Outlet, useLocation } from "react-router-dom";
import { Menu } from "lucide-react";
import { NavSidebar } from "../nav/NavSidebar";
import { titleForPath } from "../nav/pageTitle";
import { IconButton } from "../shared/components/IconButton";
import { PageHeader } from "../shared/components/PageHeader";
import { useMediaQuery } from "../shared/hooks/useMediaQuery";
import { useUiStore } from "../shared/stores/uiStore";
import { MobileNavDrawer } from "./MobileNavDrawer";
import { UserMenu } from "./UserMenu";

export function AppShell() {
  const location = useLocation();
  const isMobile = useMediaQuery("(max-width: 767px)");
  const isTablet = useMediaQuery("(min-width: 768px) and (max-width: 1023px)");
  const sidebarCollapsed = useUiStore((state) => state.sidebarCollapsed);
  const toggleSidebar = useUiStore((state) => state.toggleSidebar);
  const mobileNavOpen = useUiStore((state) => state.mobileNavOpen);
  const setMobileNavOpen = useUiStore((state) => state.setMobileNavOpen);

  const collapsed = !isMobile && (isTablet || sidebarCollapsed);
  const title = titleForPath(location.pathname);

  useEffect(() => {
    setMobileNavOpen(false);
  }, [location.pathname, setMobileNavOpen]);

  return (
    <div className="min-h-screen bg-bg p-0 md:p-4">
      <div className="mx-auto flex min-h-screen max-w-[1600px] overflow-hidden md:min-h-[calc(100vh-2rem)] md:rounded-xl md:shadow-md">
        {!isMobile && (
          <NavSidebar collapsed={collapsed} onToggle={toggleSidebar} showToggle={!isTablet} />
        )}
        {isMobile && <MobileNavDrawer open={mobileNavOpen} onOpenChange={setMobileNavOpen} />}
        <div className="flex min-w-0 flex-1 flex-col bg-bg">
          <header className="flex items-center gap-3 px-4 py-4 md:px-6">
            {isMobile && (
              <IconButton label="Open navigation" onClick={() => setMobileNavOpen(true)}>
                <Menu size={20} strokeWidth={1.75} aria-hidden />
              </IconButton>
            )}
            <PageHeader title={title} />
            <UserMenu />
          </header>
          <main className="min-w-0 flex-1 overflow-auto px-4 pb-6 md:px-6">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}
