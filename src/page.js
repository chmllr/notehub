var marked = require("marked");
var fs = require("fs");

var pageTemplate = fs.readFileSync("resources/template.html", "utf-8");
var newNoteTemplate = fs.readFileSync("resources/new.html", "utf-8");
var buildPage = (id, title, content) => pageTemplate
  .replace("%TITLE%", title)
  .replace(/%LINK%/g, id)
  .replace("%CONTENT%", content);

module.exports.buildNote = note => buildPage(note.id, note.title, marked(note.text));

module.exports.newNotePage = session => newNoteTemplate
  .replace("%METHOD%", "POST")
  .replace("%SESSION%", session);
