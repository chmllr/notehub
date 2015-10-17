var marked = require("marked");
var fs = require("fs");

var pageTemplate = fs.readFileSync("resources/template.html", "utf-8");
var editTemplate = fs.readFileSync("resources/edit.html", "utf-8");
var buildPage = (id, title, content) => pageTemplate
  .replace("%TITLE%", title)
  .replace(/%LINK%/g, id)
  .replace("%CONTENT%", content);

module.exports.buildNote = note => buildPage(note.id, note.title, marked(note.text));

module.exports.newNotePage = session => editTemplate
  .replace("%METHOD%", "POST")
  .replace("%SESSION%", session)
  .replace("%CONTENT%", "Loading...");

module.exports.editNotePage = (session, note) => editTemplate
  .replace("%METHOD%", "UPDATE")
  .replace("%SESSION%", session)
  .replace("%CONTENT%", note.text);