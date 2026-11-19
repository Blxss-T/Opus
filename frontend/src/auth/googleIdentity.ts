const SCRIPT_ID = "google-identity-services";
const SCRIPT_SRC = "https://accounts.google.com/gsi/client";

interface GoogleCredentialResponse {
  credential?: string;
}

interface GooglePromptNotification {
  isNotDisplayed(): boolean;
  isSkippedMoment(): boolean;
}

interface GoogleIdentity {
  initialize(config: {
    client_id: string;
    callback: (response: GoogleCredentialResponse) => void;
    error_callback?: () => void;
    auto_select?: boolean;
    ux_mode?: "popup" | "redirect";
    context?: "signin" | "signup" | "use";
  }): void;
  prompt(callback?: (notification: GooglePromptNotification) => void): void;
  renderButton(parent: HTMLElement, config: Record<string, unknown>): void;
}

declare global {
  interface Window {
    google?: { accounts: { id: GoogleIdentity } };
  }
}

export function getGoogleClientId(): string {
  return import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim() ?? "";
}

export function preloadGoogleIdentity(): void {
  if (!getGoogleClientId() || window.google?.accounts?.id || document.getElementById(SCRIPT_ID)) {
    return;
  }
  const script = document.createElement("script");
  script.id = SCRIPT_ID;
  script.src = SCRIPT_SRC;
  script.async = true;
  document.head.appendChild(script);
}

function loadGoogleIdentity(): Promise<GoogleIdentity> {
  if (window.google?.accounts?.id) {
    return Promise.resolve(window.google.accounts.id);
  }

  return new Promise((resolve, reject) => {
    const fail = () => reject(new Error("Google sign-in failed to load."));
    const succeed = () => {
      if (window.google?.accounts?.id) resolve(window.google.accounts.id);
      else fail();
    };

    let script = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null;
    if (!script) {
      script = document.createElement("script");
      script.id = SCRIPT_ID;
      script.src = SCRIPT_SRC;
      script.async = true;
      document.head.appendChild(script);
    }

    script.addEventListener("load", succeed, { once: true });
    script.addEventListener("error", fail, { once: true });
  });
}

export async function requestGoogleIdToken(): Promise<string> {
  const clientId = getGoogleClientId();
  if (!clientId) {
    throw new Error("Google sign-in is not configured.");
  }

  const googleId = await loadGoogleIdentity();

  return new Promise((resolve, reject) => {
    const host = document.createElement("div");
    host.setAttribute("aria-hidden", "true");
    host.style.position = "fixed";
    host.style.width = "1px";
    host.style.height = "1px";
    host.style.overflow = "hidden";
    host.style.opacity = "0";
    document.body.appendChild(host);

    const cleanup = () => host.remove();
    const fail = (message: string) => {
      cleanup();
      reject(new Error(message));
    };

    googleId.initialize({
      client_id: clientId,
      ux_mode: "popup",
      context: "signin",
      auto_select: false,
      callback: (response) => {
        cleanup();
        if (response.credential) resolve(response.credential);
        else reject(new Error("Google sign-in was cancelled."));
      },
      error_callback: () => fail("Google sign-in was cancelled."),
    });

    googleId.renderButton(host, {
      type: "standard",
      theme: "filled_black",
      size: "large",
      text: "continue_with",
      ux_mode: "popup",
    });

    const officialButton = host.querySelector<HTMLElement>("div[role='button']");
    if (officialButton) {
      officialButton.click();
      return;
    }

    googleId.prompt((notification) => {
      if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
        fail("Google sign-in is blocked in this browser. Allow third-party sign-in and try again.");
      }
    });
  });
}
