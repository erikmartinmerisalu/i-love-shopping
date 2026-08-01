type StatusBannerProps = {
  variant: "success" | "error" | "info";
  title: string;
  message?: string;
};

const variantStyles: Record<StatusBannerProps["variant"], string> = {
  success: "border-green-500/40 bg-green-500/10",
  error: "border-red-500/40 bg-red-500/10",
  info: "border-sky-500/40 bg-sky-500/10",
};

const iconStyles: Record<StatusBannerProps["variant"], string> = {
  success: "bg-green-500/20 text-green-400 border-green-500/30",
  error: "bg-red-500/20 text-red-400 border-red-500/30",
  info: "bg-sky-500/20 text-sky-400 border-sky-500/30",
};

const titleStyles: Record<StatusBannerProps["variant"], string> = {
  success: "text-green-400",
  error: "text-red-400",
  info: "text-sky-400",
};

const messageStyles: Record<StatusBannerProps["variant"], string> = {
  success: "text-green-300",
  error: "text-red-300",
  info: "text-sky-300",
};

const icons: Record<StatusBannerProps["variant"], string> = {
  success: "✓",
  error: "!",
  info: "ℹ",
};

const StatusBanner = ({ variant, title, message }: StatusBannerProps) => {
  return (
    <div
      role="status"
      className={`flex gap-4 rounded-xl border p-4 ${variantStyles[variant]}`}
    >
      <div
        className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full border text-lg font-bold ${iconStyles[variant]}`}
      >
        {icons[variant]}
      </div>
      <div className="min-w-0">
        <p className={`font-semibold ${titleStyles[variant]}`}>{title}</p>
        {message && <p className={`mt-1 text-sm ${messageStyles[variant]}`}>{message}</p>}
      </div>
    </div>
  );
};

export default StatusBanner;
