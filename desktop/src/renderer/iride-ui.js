document.addEventListener('DOMContentLoaded', () => {
  const btn = document.getElementById('nav-search-rail');
  const input = document.getElementById('search-input');
  if (btn && input) btn.addEventListener('click', (e) => { e.stopPropagation(); input.focus(); input.select(); });
});
