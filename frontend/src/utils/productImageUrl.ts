const PLACEHOLDER_IMAGE_PATTERN = /placeholder\.png$/i;

export const resolveProductImageUrl = (
  url: string | null | undefined,
  fallback: string
): string => {
  if (!url || PLACEHOLDER_IMAGE_PATTERN.test(url)) {
    return fallback;
  }

  if (url.startsWith("http")) {
    return url;
  }

  return url.startsWith("/api") ? url : `/api${url}`;
};
