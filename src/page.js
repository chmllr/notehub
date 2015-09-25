var marked = require("marked");
var fs = require("fs");

var template = fs.readFileSync("resources/template.html", "utf-8");
var buildHTML = (id, title, content) => template
  .replace("%TITLE%", title)
  .replace(/%LINK%/g, id)
  .replace("%CONTENT%", content);

module.exports.build = note => buildHTML(note.id, note.title, marked(note.text));
