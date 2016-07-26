// jscs:disable maximumLineLength

/**
 * Highlight Module
 *
 *
 * High Level API:
 *
 *   // Load basic dependencies and language support for present code tags
 *   api.init()
 *
 *
 * Hooks To:
 *
 *  'document:loaded'  ~> highlight.init();
 *
 */
(function (global) {

  // Load minimal requirements
  function loadInitialScriptsAndStyles() {
    var link =
      document.createElement('link');

    var mainScript =
      document.createElement('script');

    link.rel =
      'stylesheet';

    link.href =
      'https://cdnjs.cloudflare.com/ajax/libs/prism/1.5.1/themes/prism-tomorrow.min.css';

    mainScript.src =
      'https://cdnjs.cloudflare.com/ajax/libs/prism/1.5.1/prism.min.js';

    mainScript.addEventListener('load', function () {
      // Escape early if back-end rendering is used
      if (!('marked' in global)) {
        // @TODO Autoload er
        return Prism.highlightAll();
      }

      // Set up autoloading
      marked.setOptions({
        highlight: function (code, language) {
          if (!(language in Prism.languages)) {
            var additionalLanguageScript =
              document.createElement('script');

            additionalLanguageScript.src =
              'https://cdnjs.cloudflare.com/ajax/libs/prism/1.5.1/components/prism-' + language + '.min.js';

            // On success, highlight code for given language
            additionalLanguageScript.addEventListener('load', function () {
              [].forEach.call(document.querySelectorAll('.lang-' + language), function (element) {
                Prism.highlightElement(element);
              });
            });

            // Remove if language not available
            additionalLanguageScript.addEventListener('error', function () {
              document.body.removeChild(additionalLanguageScript);
            });

            document.body.appendChild(additionalLanguageScript);
          }

          return Prism.highlight(code, Prism.languages[language] || Prism.languages.markup);
        },
      });

    });

    document.head.appendChild(link);
    document.body.appendChild(mainScript);
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
