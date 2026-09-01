export interface ApiErrorDetail {
  loc?: (string | number)[];
  field?: string;
  msg?: string;
  message?: string;
  type?: string;
}

export class ApiError extends Error {
  status: number;
  code?: string;
  details?: ApiErrorDetail[];

  constructor(message: string, status: number, code?: string, details?: ApiErrorDetail[]) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://127.0.0.1:8000/api/v1";

// In-Memory Access Token Storage
let inMemoryAccessToken: string | null = null;

export const setAccessToken = (token: string | null) => {
  inMemoryAccessToken = token;
};

export const getAccessToken = (): string | null => {
  return inMemoryAccessToken;
};

// Variable & promise to coordinate concurrent token refresh attempts
let refreshPromise: Promise<string | null> | null = null;

async function attemptRefreshToken(): Promise<string | null> {
  if (refreshPromise) {
    return refreshPromise;
  }

  refreshPromise = (async () => {
    try {
      const res = await fetch(`${BASE_URL}/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include", // Sends the HttpOnly paradox_refresh_token cookie
      });

      if (!res.ok) {
        setAccessToken(null);
        return null;
      }

      const data = await res.json();
      const newToken = data.access_token || data.data?.access_token;
      if (newToken) {
        setAccessToken(newToken);
        return newToken;
      }
      setAccessToken(null);
      return null;
    } catch {
      setAccessToken(null);
      return null;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  isRetry: boolean = false
): Promise<T> {
  const url = `${BASE_URL}${path}`;
  const headers = new Headers(options.headers);

  if (!headers.has("Content-Type") && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  // Attach Bearer Access Token if available
  if (inMemoryAccessToken && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${inMemoryAccessToken}`);
  }

  const fetchOptions: RequestInit = {
    ...options,
    headers,
    credentials: "include", // Always include cookies for session rotation & refresh
  };

  const response = await fetch(url, fetchOptions);

  // Handle 401 Unauthorized with Automatic Refresh & Request Retry
  if (response.status === 401 && !isRetry && !path.startsWith("/auth/login") && !path.startsWith("/auth/refresh")) {
    const newAccessToken = await attemptRefreshToken();
    if (newAccessToken) {
      // Retry original request with fresh access token
      const retryHeaders = new Headers(options.headers);
      retryHeaders.set("Authorization", `Bearer ${newAccessToken}`);
      return request<T>(path, { ...options, headers: retryHeaders }, true);
    }
  }

  if (!response.ok) {
    let errorMessage = "An error occurred";
    let errorCode = "INTERNAL_ERROR";
    let errorDetails: ApiErrorDetail[] | undefined;

    try {
      const errorJson = await response.json();
      errorMessage =
        errorJson.error?.message ||
        errorJson.message ||
        errorJson.detail ||
        errorMessage;
      errorCode = errorJson.error?.code || errorCode;
      if (Array.isArray(errorJson.error?.details)) {
        errorDetails = errorJson.error.details;
      } else if (Array.isArray(errorJson.details)) {
        errorDetails = errorJson.details;
      } else if (Array.isArray(errorJson.detail)) {
        errorDetails = errorJson.detail;
      }
    } catch {
      errorMessage = response.statusText || errorMessage;
    }

    throw new ApiError(errorMessage, response.status, errorCode, errorDetails);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json() as Promise<T>;
}

export const client = {
  get: <T>(path: string, options?: RequestInit) =>
    request<T>(path, { ...options, method: "GET" }),

  post: <T>(path: string, body?: any, options?: RequestInit) =>
    request<T>(path, {
      ...options,
      method: "POST",
      body: body ? JSON.stringify(body) : undefined,
    }),

  put: <T>(path: string, body?: any, options?: RequestInit) =>
    request<T>(path, {
      ...options,
      method: "PUT",
      body: body ? JSON.stringify(body) : undefined,
    }),

  patch: <T>(path: string, body?: any, options?: RequestInit) =>
    request<T>(path, {
      ...options,
      method: "PATCH",
      body: body ? JSON.stringify(body) : undefined,
    }),

  delete: <T>(path: string, options?: RequestInit) =>
    request<T>(path, { ...options, method: "DELETE" }),
};
