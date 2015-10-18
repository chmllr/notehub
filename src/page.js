var marked = require("marked");
var fs = require("fs");

var pageTemplate = fs.readFileSync("resources/template.html", "utf-8");
var footerTemplate = fs.readFileSync("resources/footer.html", "utf-8");
var editTemplate = fs.readFileSync("resources/edit.html", "utf-8");
var buildPage = (title, content, footer) => pageTemplate
  .replace("%TITLE%", title)
  .replace("%CONTENT%", content)
  .replace("%FOOTER%", footer);
  
module.exports.buildPage = buildPage;

module.exports.buildNote = note => buildPage(note.title, 
  marked(note.text),
  footerTemplate.replace(/%LINK%/g, note.id));

module.exports.newNotePage = session => editTemplate
  .replace("%ACTION%", "POST")
  .replace("%SESSION%", session)
  .replace("%CONTENT%", "Loading...");

module.exports.editNotePage = (session, note) => editTemplate
  .replace("%ACTION%", "UPDATE")
  .replace("%SESSION%", session)
  .replace("%ID%", note.id)
  .replace("%CONTENT%", note.text);