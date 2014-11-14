## About

[NoteHub](http://notehub.org) is a free and hassle-free pastebin for one-off markdown publishing.

NoteHub _was_ an one-app-one-language [experiment](http://notehub.org/2012/6/16/how-notehub-is-built) and was initially implemented entirely in [Clojure](http://clojure.org) (ClojureScript).
NoteHub's persistence layer is based on the key-value store [redis](http://redis.io).
Currently, NoteHub is hosted for free on [Heroku](http://heroku.com).

NoteHub supports an [API](https://github.com/chmllr/NoteHub/blob/master/API.md) and can be integrated as a publishing platform.

## How to Use?
First, create [a new page](http://notehub.org/new) using the [Markdown syntax](http://daringfireball.net/projects/markdown/).
When the note is published, you'll see a subtle panel at the bottom of the screen.
From this panel you can go to a rudimentary statistics of the article, or you can export the original markdown, or copy the short url of the note.
Besides this, you also can invert the color scheme by appending to the note url:

    notehub.org/.../title?theme=dark

The same way you can specify a [Google Web Font](http://www.google.com/webfonts/) for headers by appending to the note url:

    notehub.org/.../title?header-font=FONT-NAME

and for the text itself:

    notehub.org/.../title?header-font=FONT-NAME&text-font=FONT-NAME2

Analogously, you can specify you can change the text or header size by specifying a scale factor:

    notehub.org/.../title?header-font=FONT-NAME&text-font=FONT-NAME2&text-size=1.1&header-size=1.2

See an example of the font formatting [here](http://www.notehub.org/8m4l9).

After you've specified this in the url, you can copy the corresponding short url of the article and share it.

## After Publishing

During the note publishing a password can be set.
This password unlocks the note for editing.
The edit mode can be entered by appending of `/edit` to the note url.
By appending of `/stats` to any note url, everyone can see a rudimentary statistics (currently, the number of note views only).
By appending of `/export`, the original markdown content will be displayed in plain text format.
