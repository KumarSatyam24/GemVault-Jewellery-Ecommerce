/** @type {import('tailwindcss').Config} */
module.exports = {
    content: ["./src/**/*.{html,ts}"],
    darkMode: "class",
    theme: {
      extend: {
        colors: {
          alabaster: "#F9F8F6",
          charcoal: "#1A1A1A",
          taupe: "#EBE5DE",
          muted: "#6C6863",
          gold: "#B48B42",
          ink: "#27221E",
        },
        fontFamily: {
          display: ["Playfair Display", "Georgia", "serif"],
          sans: ["Inter", "Arial", "sans-serif"],
        },
        maxWidth: {
          editorial: "1480px",
        },
      },
    },
    plugins: [],
  };
  