var marked = require("marked");
var fs = require("fs");

var pageTemplate = fs.readFileSync("resources/template.html", "utf-8");
var footerTemplate = fs.readFileSync("resources/footer.html", "utf-8");
var editTemplate = fs.readFileSync("resources/edit.html", "utf-8");

var deriveTitle = text => text
  .split(/[\n\r]/)[0].slice(0,25)
  .replace(/[^a-zA-Z0-9\s]/g, "");

var buildPage = (title, content, footer) => pageTemplate
  .replace("%TITLE%", title)
  .replace("%CONTENT%", content)
  .replace("%FOOTER%", footer);
  
module.exports.buildPage = buildPage;

module.exports.buildStats = note => buildPage(deriveTitle(note.text), 
  `<h2>Statistics</h2>
  <table>
    <tr><td>Published</td><td>${note.published}</td></tr>
    <tr><td>Edited</td><td>${note.edited || "N/A"}</td></tr>
    <tr><td>Views</td><td>${note.views}</td></tr>
  </table>`,
  "");

module.exports.buildNote = note => buildPage(deriveTitle(note.text), 
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