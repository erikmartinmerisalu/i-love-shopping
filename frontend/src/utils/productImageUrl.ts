export const resolveProductImageUrl = (
  url: string | null | undefined,
  fallback: string
): string => {
  if (!url || url.trim() === '') {
    return fallback;
  }

  if (url.startsWith('http')) {
    return url;
  }

  return url.startsWith('/api') ? url : `/api${url}`;
};
