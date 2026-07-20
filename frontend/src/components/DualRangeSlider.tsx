type DualRangeSliderProps = {
  min: number;
  max: number;
  valueMin: number;
  valueMax: number;
  step?: number;
  onChange: (min: number, max: number) => void;
  formatLabel?: (value: number) => string;
};

const DualRangeSlider = ({
  min,
  max,
  valueMin,
  valueMax,
  step = 1,
  onChange,
  formatLabel = (value) => String(value),
}: DualRangeSliderProps) => {
  const safeMin = Math.min(valueMin, valueMax);
  const safeMax = Math.max(valueMin, valueMax);
  const span = max - min || 1;
  const minPercent = ((safeMin - min) / span) * 100;
  const maxPercent = ((safeMax - min) / span) * 100;

  const handleMinChange = (nextMin: number) => {
    onChange(Math.min(nextMin, safeMax), safeMax);
  };

  const handleMaxChange = (nextMax: number) => {
    onChange(safeMin, Math.max(nextMax, safeMin));
  };

  return (
    <div className="space-y-3">
      <div className="flex justify-between text-xs text-gray-400">
        <span>{formatLabel(safeMin)}</span>
        <span>{formatLabel(safeMax)}</span>
      </div>

      <div className="relative h-8">
        <div className="absolute top-1/2 h-1.5 w-full -translate-y-1/2 rounded-full bg-gray-700" />
        <div
          className="absolute top-1/2 h-1.5 -translate-y-1/2 rounded-full bg-primary"
          style={{
            left: `${minPercent}%`,
            width: `${Math.max(maxPercent - minPercent, 0)}%`,
          }}
        />

        <input
          type="range"
          min={min}
          max={max}
          step={step}
          value={safeMin}
          onChange={(event) => handleMinChange(Number(event.target.value))}
          className="dual-range-thumb absolute inset-0 z-20 w-full cursor-pointer appearance-none bg-transparent"
          aria-label="Minimum price"
        />
        <input
          type="range"
          min={min}
          max={max}
          step={step}
          value={safeMax}
          onChange={(event) => handleMaxChange(Number(event.target.value))}
          className="dual-range-thumb absolute inset-0 z-30 w-full cursor-pointer appearance-none bg-transparent"
          aria-label="Maximum price"
        />
      </div>
    </div>
  );
};

export default DualRangeSlider;
