/** @type {import('tailwindcss').Config} */
/** 配色对齐 design-tokens/theme.json（深色医疗科技风） */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        med: {
          50: '#f0fdfa',
          100: '#ccfbf1',
          400: '#5eead4',
          500: '#2dd4bf',
          600: '#14b8a6',
          700: '#0f766e',
          900: '#0b1520',
        },
      },
    },
  },
  plugins: [],
}
