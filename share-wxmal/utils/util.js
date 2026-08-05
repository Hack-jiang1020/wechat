function formatTime(value) {
  if (!value) return '';
  let str = String(value).replace('T', ' ').replace(/\.\d+$/, '');
  return str.substring(0, 16);
}

function formatViews(views) {
  const v = views || 0;
  if (v >= 10000) return (v / 10000).toFixed(1) + 'w';
  if (v >= 1000) return (v / 1000).toFixed(1) + 'k';
  return String(v);
}

module.exports = {
  formatTime: formatTime,
  formatViews: formatViews
};
