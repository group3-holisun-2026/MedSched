import React from "react";

const DEFAULT_THRESHOLDS = { medium: 50, high: 80 };

const getColorClasses = (percentage, thresholds) => {
  if (percentage >= thresholds.high) {
    return { bar: "bg-rose-500", text: "text-rose-600", track: "bg-rose-50" };
  }
  if (percentage >= thresholds.medium) {
    return {
      bar: "bg-amber-500",
      text: "text-amber-600",
      track: "bg-amber-50",
    };
  }
  return {
    bar: "bg-emerald-500",
    text: "text-emerald-600",
    track: "bg-emerald-50",
  };
};

const RateBar = ({
  percentage = 0,
  label,
  occupiedMinutes,
  totalMinutes,
  thresholds = DEFAULT_THRESHOLDS,
  size = "md",
  showPercentageLabel = true,
  className = "",
}) => {
  const numericPercentage = Number.isFinite(percentage) ? percentage : 0;
  const clamped = Math.min(100, Math.max(0, numericPercentage));
  const { bar, text, track } = getColorClasses(clamped, thresholds);
  const heightClass = size === "sm" ? "h-1.5" : "h-2.5";

  return (
    <div className={`w-full ${className}`}>
      {(label || showPercentageLabel) && (
        <div className="flex items-center justify-between mb-1">
          {label && <span className="text-sm text-gray-700">{label}</span>}
          {showPercentageLabel && (
            <span className={`text-xs font-semibold ${text}`}>
              {clamped.toFixed(0)}%
            </span>
          )}
        </div>
      )}

      <div
        className={`w-full ${heightClass} rounded-full ${track} overflow-hidden`}
      >
        <div
          className={`${heightClass} rounded-full ${bar} transition-all duration-300`}
          style={{ width: `${clamped}%` }}
          role="progressbar"
          aria-valuenow={Math.round(clamped)}
          aria-valuemin={0}
          aria-valuemax={100}
        />
      </div>

      {occupiedMinutes !== undefined && totalMinutes !== undefined && (
        <p className="text-xs text-gray-400 mt-1">
          {occupiedMinutes} / {totalMinutes} min ocupate
        </p>
      )}
    </div>
  );
};

export default RateBar;
