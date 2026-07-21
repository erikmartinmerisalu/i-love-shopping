export const formatAuthProvider = (provider?: string | null): string => {
  if (!provider) {
    return "Google";
  }

  switch (provider.toLowerCase()) {
    case "google":
      return "Google";
    case "facebook":
      return "Facebook";
    default:
      return provider.charAt(0).toUpperCase() + provider.slice(1);
  }
};

export const socialSignInLabel = (provider?: string | null): string =>
  `${formatAuthProvider(provider)} sign-in`;
