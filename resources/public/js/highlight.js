// jscs:disable maximumLineLength

/**
 * Highlight Module
 *
 *
 * High Level API:
 *
 *   // Load basic dependencies and language support for present code tags
 *
 *   highlight.init()
 *
 *
 *   // Re-highlights all code tags on demand
 *   // Useful when markdown has been parsed again
 *   // and new language occurred in the output for example
 *
 *   highlight.update();
 *
 *
 *  Hooks To:
 *
 *  'document:loaded'  ~> highlight.init();
 *  'content:rendered' ~> highlight.update();
 *
 */
(function (global) {

  // Simplified debounce version (no args support)
  function debounce(callback, milliseconds) {
    var timeout;

    return function () {
      clearTimeout(timeout);

      timeout = setTimeout(function () {
        callback();
      }, milliseconds);
    };
  }

  // Highlight callback
  function highlight() {
    if (!('Prism' in global)) {
      throw new Error(
        '[Highlight] Prism not detected. Please run `highlight.init` to load all dependencies'
      );
    }

    global.Prism.highlightAll();
  }

  // Debounced highlight callback
  var debouncedHighlight =
    debounce(highlight, 300);

  // Load minimal requirements
  function loadInitialScriptsAndStyles() {
    var link =
      document.createElement('link');

    var mainScript =
      document.createElement('script');

    var autoloaderScript =
      document.createElement('script');

    link.rel =
      'stylesheet';

    link.href =
      'https://cdnjs.cloudflare.com/ajax/libs/prism/1.5.1/themes/prism-tomorrow.min.css';

    mainScript.src =
      'https://cdnjs.cloudflare.com/ajax/libs/prism/1.5.1/prism.min.js';

    autoloaderScript.src =
      'https://cdnjs.cloudflare.com/ajax/libs/prism/1.5.1/plugins/autoloader/prism-autoloader.min.js';

    mainScript.addEventListener('load', function () {
      // Load autoloader after Prism loads
      document.body.appendChild(autoloaderScript);
    });

    autoloaderScript.addEventListener('load', function () {
      global.Prism.plugins.autoloader.languages_path =
        'https://cdnjs.cloudflare.com/ajax/libs/prism/1.5.1/components/';

      global.highlight.update();
    });

    document.head.appendChild(link);
    document.body.appendChild(mainScript);
  }

  // High Level API
  global.highlight = global.highlight || {

    init: function () {
      loadInitialScriptsAndStyles();
    },

    update: function () {
      debouncedHighlight();
    },

  };

  // Hooks
  if ('events' in global) {
    events.subscribe('document:loaded', global.highlight.init);
    events.subscribe('content:rendered', global.highlight.update);
  }

}(window));
