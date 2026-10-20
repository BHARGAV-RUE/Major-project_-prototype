/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        navy: "#0c1a33",
        cobalt: {
          DEFAULT: "#2456f0",
          dark: "#1b44c4",
        },
        teal: "#12a594",
        paper: "#f3f6fb",
        soft: "#f7f9fd",
        line: "#dfe5ef",
        bodytext: "#10192b",
        muted: "#5b6880",
        danger: "#d92d4a",
      },
      fontFamily: {
        display: ['"Bricolage Grotesque"', '"Figtree"', "system-ui", "sans-serif"],
        body: ['"Figtree"', "system-ui", "-apple-system", "sans-serif"],
      },
      boxShadow: {
        widget: "0 40px 70px -28px rgba(5,12,30,0.55), 0 0 0 1px rgba(12,26,51,0.06)",
      },
      keyframes: {
        rise: {
          "0%": { opacity: "0", transform: "translateY(24px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
      },
      animation: {
        rise: "rise 0.7s cubic-bezier(0.2, 0.7, 0.2, 1) both",
      },
    },
  },
  plugins: [],
};