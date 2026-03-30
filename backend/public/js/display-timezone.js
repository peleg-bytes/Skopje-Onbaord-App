/**
 * Browser mirror of backend/lib/display-timezone.js — keep logic in sync.
 * Exposes skopjeSurveyDate(iso), skopjeSurveyTime(iso) on window.
 */
(function (w) {
  var TZ = 'Europe/Skopje';
  w.skopjeSurveyDate = function (iso) {
    if (!iso) return '';
    var d = new Date(iso);
    return new Intl.DateTimeFormat('en-CA', {
      timeZone: TZ,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).format(d);
  };
  w.skopjeSurveyTime = function (iso) {
    if (!iso) return '';
    var d = new Date(iso);
    var parts = new Intl.DateTimeFormat('en-GB', {
      timeZone: TZ,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    }).formatToParts(d);
    function pick(t) {
      for (var i = 0; i < parts.length; i++) {
        if (parts[i].type === t) return parts[i].value;
      }
      return '00';
    }
    return pick('hour') + ':' + pick('minute') + ':' + pick('second');
  };
})(typeof window !== 'undefined' ? window : globalThis);
