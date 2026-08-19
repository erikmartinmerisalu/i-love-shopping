type StarRatingProps = {
  rating: number;
  max?: number;
  size?: 'sm' | 'md';
  showValue?: boolean;
};

export default function StarRating({
  rating,
  max = 5,
  size = 'sm',
  showValue = true,
}: StarRatingProps) {
  const safeRating = Math.max(0, Math.min(max, rating));
  const textSize = size === 'md' ? 'text-base' : 'text-sm';

  return (
    <span
      className={`inline-flex items-center gap-1 text-yellow-300 ${textSize}`}
      aria-label={`Rated ${safeRating.toFixed(1)} out of ${max} stars`}
    >
      <span aria-hidden="true">★</span>
      {showValue && <span className="font-medium">{safeRating.toFixed(1)}</span>}
    </span>
  );
}
