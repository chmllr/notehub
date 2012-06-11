# NoteHub Readme

[NoteHub](http://notehub.org) is a free and hassle-free anonymous hosting for markdown pages. It can be used for publishing of markdown-formatted text.

## Writing

A new note can be entered and previewed at [notehub.org/new](http://notehub.org/new).

## Publishing

Once a page is published, it gets accessible through an URL of the format:

    http://notehub.org/%YEAR/%MONTH/%DAY/%TITLE

Hence, every date represents a name space.

## Exporting & Statistics

By appending of `/stat` everyone can see a rudimentary statistics (currently, the number of note views only).
By appending of `/export` the original markdown content will be displayed in plain text format.

## Displaying

There are some experimental features, which allow to change via the note URL the design of the note page.
E.g., by appending of `&theme=dark` to the URL, the design colors will be inverted.
Currently it is also possible to change the header and the article text by appending of the Google Webfont names like this:

    notehub.org/.../title?header-font=Anton&text-font=Cagliostro
