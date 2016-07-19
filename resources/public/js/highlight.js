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
 *   // Function that loads additional languages support
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

  // Converts arguments-like / querySelector results data to a simple array
  function toArray(data) {
    return [].slice.call(data);
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
  var debouncedHighlight = debounce(highlight, 300);

  // Collect a list of unique langages to be highlighted
  function collectLanguages() {
    return []
            .concat(toArray(document.querySelectorAll('code[class*="lang-"]')))
            .concat(toArray(document.querySelectorAll('code[class*="language-"]')))
            .map(function (element) {
              // Collect languages from code elements, e.g. `lang-css`, `language-javascript`
              // and then remove `lang-` and `language-` parts
              return (element.className.match(/lang(uage)?\-\w+/g) || [])
                      .map(function (languageClass) {
                        return languageClass.replace(/lang(uage)?\-/g, '').trim();
                      })
                      .map(function (language) {
                        // Common language abbreviations mapped to full language names
                        var mappings = {
                          js: 'javascript',
                        };

                        return mappings[language] || language;
                      });
            })
            .reduce(function (uniqueLanguages, elementLanguages) {
              elementLanguages.forEach(function (language) {
                // Add language to the pool if not detected already
                if (uniqueLanguages.indexOf(language) === -1) {
                  uniqueLanguages.push(language);
                }
              });

              return uniqueLanguages;
            }, []);
  }

  // Load scripts for additional languages support
  function loadAdditionalLanguageSupport() {
    collectLanguages()
    .forEach(function (language) {
      var resource = 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.5.1/components/prism-%s.min.js'
                     .replace('%s', language);

      // Escape early if language support is already loaded
      if (document.querySelector('script[src="' + resource + '"]')) {
        debouncedHighlight();
        return;
      }

      // Load language support file otherwise
      var script = document.createElement('script');

      script.src = resource;

      script.addEventListener('load', debouncedHighlight);
      script.addEventListener('error', function (event) {
        // Remove element that wasn't successful
        document.body.removeChild(event.srcElement);

        // Highlight code anyway
        debouncedHighlight();
      });

      document.body.appendChild(script);
    });
  }

  // Load minimal requirements
  function loadInitialScriptsAndStyles() {
    var link   = document.createElement('link');
    var script = document.createElement('script');

    link.rel      = 'stylesheet';
    link.href     = 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.5.1/themes/prism.min.css';
    script.src    = 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.5.1/prism.min.js';

    script.addEventListener('load', global.highlight.update);

    document.head.appendChild(link);
    document.body.appendChild(script);
  }

  // High Level API
  global.highlight = global.highlight || {

    init: function () {
      loadInitialScriptsAndStyles();
    },

    update: function () {
      loadAdditionalLanguageSupport();
    },

  };

  // Hooks
  if ('events' in global) {
    events.subscribe('document:loaded', global.highlight.init);
    events.subscribe('content:rendered', global.highlight.update);
  }

}(window));
