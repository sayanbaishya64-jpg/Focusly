interface FocuslyBrandMarkProps {
  size?: number;
}

export function FocuslyBrandMark({ size = 48 }: FocuslyBrandMarkProps) {
  return (
    <div
      className="inline-flex items-center justify-center rounded-2xl bg-[#101B32] border border-[#233554] shrink-0"
      style={{ width: size, height: size }}
      aria-hidden="true"
    >
      <svg
        width={Math.round(size * 0.54)}
        height={Math.round(size * 0.54)}
        viewBox="0 0 28 28"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <circle
          cx="14"
          cy="14"
          r="10.5"
          stroke="#3B82F6"
          strokeWidth="2.5"
          strokeDasharray="48 18"
          strokeLinecap="round"
        />
        <path
          d="M15.2 7.5L10.5 14.8H14.5L12.8 20.5L17.5 13.2H13.5L15.2 7.5Z"
          fill="#22D3EE"
        />
      </svg>
    </div>
  );
}
