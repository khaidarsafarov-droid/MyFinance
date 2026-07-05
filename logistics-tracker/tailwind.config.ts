import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: "#5C59E8",
          50: "#f5f4ff",
          100: "#edeaff",
          200: "#ddd9ff",
          300: "#c4bcff",
          400: "#a596ff",
          500: "#5C59E8",
          600: "#534ff0",
          700: "#4743e8",
          800: "#3c38d4",
          900: "#3431ae",
        },
      },
    },
  },
  plugins: [],
};

export default config;
