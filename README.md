# NoteHub

NoteHub is a free and anonymous hosting for markdown pages. It can be used for publishing of markdown-formatted text. For instance, this  service might be interesting to people, who want to publish something, but don't want to start a blog.

## Writing

Once a user started to write, he gets a special draft URL like:

    http://notehub.org/draft/4j32j4g23v23v4vnb234mn23jh6b76686

His changes will be auto-saved every minute, manualy or at a registered page exit. When the same link will be opened again and the page wasn't published yet, the author will be recognized using cookies, or he should enter three non-trivial words from the document, to unlock the draft. The security aspect here is non-critical, since every page will eventually be published on NoteHub anyway. So there is no reason to assume, that a draft contains critical information.

## Publishing

Once a page is published, it gets accessible under and URL like:

    http://notehub.org/%YEAR/%MONTH/%DAY/%TITLE

Hence, every date represents a name space.

## Features

 * Preview (could be used from Vim or any other MD-editor):
    http://notehub.org/preview?t="..."
 * View Control over url, e.g. `http://notehub.org/2012/03/23/My-Thoughts/background/dark` would display the note on a dark background.
 * Short urls available by:
    http://notehub.org/2012/03/23/foobar/...any-options.../shorturl -> http://notehub.org/asd3rf
 * Every page downloadable without any dependencies except of fonts and images (can be improved ?)
