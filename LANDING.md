## Features
- **Color Themes**: specify the color scheme by appending the corresponding parameter to the URL:
    - [Dark](/2014/3/31/demo-note?theme=dark)
    - [Solarized-Dark](/2014/3/31/demo-note?theme=solarized-dark)
    - [Solarized-Light](/2014/3/31/demo-note?theme=solarized-light)
- **Fonts**: specify a font (also one of the [Google Web Fonts](http://www.google.com/webfonts/)) for headers and for the text by appending parameters to the note [URL](/8m4l9).
- **Short URLs**: every page (including theme & font options) has its own short url.
- **Editing**: if you set a password during publishing, you can edit your note any time later.
- **Statistics**: a rudimentary statistics available (date of publishing & view counter).
- **Export**: the original markdown content can be displayed in plain text format.
- **API**: Integrate the publishing functionality into your editor using the official [NoteHub API](/api).
- **Expiration**: All notes with less than 30 views in 30 days from publishing will expire.


## Changelog
 - March 2014: all notes with __less than 30 views in 30 days from publishing__ will expire now
 - February 2014: a simple JS-client for API testing [added](/api-test.html).
 - January 2014:
   - Mobile friendly styling added.
   - NoteHub API [introduced](/api).
   - NoteHub 2.0 released: new theme, better performance, extended markdown.
 - September 2013: Solarized color theme [added](https://github.com/chmllr/NoteHub/pull/4) (thanks Brandon!) ([Demo](http://notehub.org/2012/6/16/how-notehub-is-built?theme=solarized-dark)).
