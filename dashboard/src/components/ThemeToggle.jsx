export function ThemeToggle({ theme, onToggle }) {
  return (
    <button type="button" className="theme-toggle" onClick={onToggle}>
      {theme === 'dark' ? '☾ Dark' : '☀ Light'}
    </button>
  )
}
