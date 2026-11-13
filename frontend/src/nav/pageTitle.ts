export function titleForPath(pathname: string): string {
  if (pathname.startsWith("/dashboard")) {
    return "Dashboard";
  }
  if (pathname === "/products/new") {
    return "New product";
  }
  if (pathname.startsWith("/products/")) {
    return "Product";
  }
  if (pathname.startsWith("/products")) {
    return "Products";
  }
  if (pathname === "/customers/new") {
    return "New customer";
  }
  if (pathname.startsWith("/customers/")) {
    return "Customer";
  }
  if (pathname.startsWith("/customers")) {
    return "Customers";
  }
  if (pathname === "/suppliers/new") {
    return "New supplier";
  }
  if (pathname.startsWith("/suppliers/")) {
    return "Supplier";
  }
  if (pathname.startsWith("/suppliers")) {
    return "Suppliers";
  }
  if (pathname.startsWith("/purchasing")) {
    return "Purchase orders";
  }
  if (pathname.startsWith("/sales")) {
    return "Sales orders";
  }
  if (pathname === "/employees/me") {
    return "My profile";
  }
  if (pathname.startsWith("/employees")) {
    return "Employees";
  }
  if (pathname.startsWith("/users")) {
    return "Users";
  }
  if (pathname.startsWith("/organizations")) {
    return "Organization";
  }
  if (pathname.startsWith("/api-keys")) {
    return "API keys";
  }
  if (pathname.startsWith("/audit")) {
    return "Audit log";
  }
  return "Opus";
}
