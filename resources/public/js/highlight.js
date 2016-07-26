// jscs:disable maximumLineLength

/**
 * Highlight Module
 *
 *
 * High Level API:
 *
 *   api.init()
 *
 *
 * Hooks To:
 *
 *  'document:loaded' ~> highlight.init();
 *
 */
(function (global) {

  function loadInitialScriptsAndStyles() {
    var link =
      document.createElement('link');

    var mainScript =
      document.createElement('script');

    link.rel =
      'stylesheet';

    link.href =
      'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.5.0/styles/zenburn.min.css';

    mainScript.src =
      'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.5.0/highlight.min.js';

    // On main script load, configure marked
    mainScript.addEventListener('load', function () {
      marked.setOptions({
        langPrefix: 'hljs lang-',
        highlight: function (code) {
          return hljs.highlightAuto(code).value;
        },
      });

    });

    // Extend marked.js on edit page only
    if ('marked' in global) {
      document.body.appendChild(mainScript);
    }

    // Preview page requires scripts only
    document.head.appendChild(link);
  }

  // High Level API
  var api = {
    init: function () {
      loadInitialScriptsAndStyles();
    },
  };

  // Hooks
  if ('events' in global) {
    events.subscribe('document:loaded', api.init);
  }

}(window));
