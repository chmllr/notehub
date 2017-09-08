# Demo Note

## Text Formatting

This is a _short_ note demonstrating the **capabilities** of Markdown. [Markdown](http://en.wikipedia.org/wiki/Markdown) is a _markup language_ with plain text
formatting syntax. But you also can use <u>standard HTML</u> tags.

## Backquotes, Lists & Code

This is a backquote:

> _"Our greatest glory is not in never falling but in rising every time we fall."_
> — Confucius

To create simple lists, just enumerate all items using a dash in the prefix:

- Alpha
- Beta
- Gamma

Also you can either mark some special `words` or write entire `code` blocks:

    (defn fact [n]
      (if (< n 2) 1
        (* n (fact (- n 1)))))

## Tables

Also simple tables is a piece of cake:

Column 1     | Column 2    | Column 3
---          | ---         | ---
Text 1       | Text 3      | <s>Text 5</s>
Text 2       | Text 4      | <mark>Text 6</mark>

Take a look at the [source code](/Demo.md/export) of this page, to see how it works.
