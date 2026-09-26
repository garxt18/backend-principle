(function () {
  var theme = 'dark';
  try {
    var saved = localStorage.getItem('bp-theme');
    if (saved === 'light' || saved === 'dark') theme = saved;
    else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches) theme = 'light';
  } catch (e) { /* storage blocked: fall back to dark */ }
  document.documentElement.dataset.theme = theme;
})();
