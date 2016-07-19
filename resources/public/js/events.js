/**
 * Simple Event Bus
 *
 * Allows to easily hook into various page rendering / markdown parsing stages with custom modules
 *
 *
 * High Level API:
 *
 *   // Subscribe
 *   events.subscribe(eventName:String, eventHandler:Function)
 *
 *   // Publish
 *   events.publish(eventName:String, optionalArgument1, optionalArgument2, ..., optionalArgumentN);
 *
 *
 * Sample Usage:
 *
 *   event.subscribe('markdown:parsed', function () {
 *     console.log('Markdown Parsed');
 *   });
 *
 *   event.subscribe('markdown:parsed', function (title) {
 *     console.log('Markdown Parsed For Document: ' + title);
 *   });
 *
 *   events.publish('markdown:parsed', 'SampleDocument.md');
 *   // Markdown Parsed
 *   // Markdown Parsed For Document: SampleDocument.md
 *
 */
(function (global) {

  var eventBus = {
    subscribers: [],
  };

  global.events = global.events || {

    subscribe: function (eventName, eventHandler) {
      // Initialize an array of event listeners if doesn't exist already
      eventBus.subscribers[eventName] = eventBus.subscribers[eventName] || [];

      eventBus.subscribers[eventName].push(eventHandler);
    },

    publish: function (eventName /*, arg1, arg2, ..., argN */) {
      var eventArguments = [].slice.call(arguments, 1);

      if (eventArguments.length) {
        console.log('[Hooks] "%s" with args %O', eventName, eventArguments);
      } else {
        console.log('[Hooks] "%s"', eventName);
      }

      // Call event handlers with given attributes
      (eventBus.subscribers[eventName] || []).forEach(function (eventHandler) {
        eventHandler.apply(null, eventArguments);
      });
    },

  };
}(window));
