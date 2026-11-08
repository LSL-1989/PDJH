/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        'medical-blue': '#1E88E5',
        'medical-green': '#43A047',
        'medical-orange': '#FB8C00',
        'medical-red': '#E53935',
        'medical-muted': '#9E9E9E',
        'medical-bg': '#F5F6FA'
      },
      boxShadow: {
        card: '0 2px 8px rgba(0, 0, 0, 0.08)'
      }
    }
  },
  plugins: [],
}
