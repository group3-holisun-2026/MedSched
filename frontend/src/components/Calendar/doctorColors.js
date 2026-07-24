

const PALETTE = [
  { name: "blue", dot: "#2563eb", bg: "#dbeafe", text: "#1e3a8a" },
  { name: "teal", dot: "#0d9488", bg: "#ccfbf1", text: "#134e4a" },
  { name: "purple", dot: "#7c3aed", bg: "#ede9fe", text: "#4c1d95" },
  { name: "orange", dot: "#ea580c", bg: "#ffedd5", text: "#7c2d12" },
  { name: "pink", dot: "#db2777", bg: "#fce7f3", text: "#831843" },
  { name: "green", dot: "#16a34a", bg: "#dcfce7", text: "#14532d" },
  { name: "red", dot: "#dc2626", bg: "#fee2e2", text: "#7f1d1d" },
  { name: "amber", dot: "#d97706", bg: "#fef3c7", text: "#78350f" },
  { name: "indigo", dot: "#4f46e5", bg: "#e0e7ff", text: "#312e81" },
  { name: "cyan", dot: "#0891b2", bg: "#cffafe", text: "#164e63" },
  { name: "lime", dot: "#65a30d", bg: "#ecfccb", text: "#365314" },
  { name: "fuchsia", dot: "#c026d3", bg: "#fae8ff", text: "#701a75" },
];

const FALLBACK = { name: "gray", dot: "#64748b", bg: "#e2e8f0", text: "#334155" };


function hashString(str) {
  let hash = 5381;
  for (let i = 0; i < str.length; i++) {
    hash = (hash * 33) ^ str.charCodeAt(i);
  }
  return Math.abs(hash);
}


export function getDoctorColor(doctorId) {
  if (!doctorId) return FALLBACK;
  const idx = hashString(String(doctorId)) % PALETTE.length;
  return PALETTE[idx];
}

export default getDoctorColor;
