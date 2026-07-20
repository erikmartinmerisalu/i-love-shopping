import { useEffect, useState } from "react";

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
  const [activeThumb, setActiveThumb] = useState<"min" | "max" | null>(null);

  const safeMin = Math.min(valueMin, valueMax);
  const safeMax = Math.max(valueMin, valueMax);
  const span = max - min || 1;
  const minPercent = ((safeMin - min) / span) * 100;
  const maxPercent = ((safeMax - min) / span) * 100;
  const thumbsClose = safeMax - safeMin <= step;

  useEffect(() => {
    const clearActiveThumb = () => setActiveThumb(null);
    window.addEventListener("pointerup", clearActiveThumb);
    return () => window.removeEventListener("pointerup", clearActiveThumb);
  }, []);

  const minZIndex =
    activeThumb === "min" ? 40 : activeThumb === "max" ? 20 : thumbsClose ? 40 : 20;
  const maxZIndex =
    activeThumb === "max" ? 40 : activeThumb === "min" ? 20 : thumbsClose ? 30 : 30;

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
          onPointerDown={() => setActiveThumb("min")}
          onChange={(event) => handleMinChange(Number(event.target.value))}
          className="dual-range-thumb absolute inset-0 w-full cursor-pointer appearance-none bg-transparent"
          style={{ zIndex: minZIndex }}
          aria-label="Minimum price"
        />
        <input
          type="range"
          min={min}
          max={max}
          step={step}
          value={safeMax}
          onPointerDown={() => setActiveThumb("max")}
          onChange={(event) => handleMaxChange(Number(event.target.value))}
          className="dual-range-thumb absolute inset-0 w-full cursor-pointer appearance-none bg-transparent"
          style={{ zIndex: maxZIndex }}
          aria-label="Maximum price"
        />
      </div>
    </div>
  );
};

export default DualRangeSlider;
