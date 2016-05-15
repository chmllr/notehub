var marked = require("marked");
var fs = require("fs");

var TOS = fs.readFileSync("resources/TOS.md", "utf-8");
var pageTemplate = fs.readFileSync("resources/template.html", "utf-8");
var footerTemplate = fs.readFileSync("resources/footer.html", "utf-8");
var editTemplate = fs.readFileSync("resources/edit.html", "utf-8");
var misuseScript = fs.readFileSync("resources/misuse.txt", "utf-8");
var misuses = new Set(fs.readFileSync("resources/misuses.txt", "utf-8").split(/\s+/));

var deriveTitle = text => text
  .split(/[\n\r]/)[0].slice(0,25)
  .replace(/[^a-zA-Z0-9\s]/g, "");

var renderPage = (id, title, content, footer) => pageTemplate
  .replace("%MISUSE%", misuses.has(id) ? misuseScript : "")
  .replace("%TITLE%", title)
  .replace("%CONTENT%", content.replace(/<meta.*?>/gi, "").replace(/<script[\s\S.]*?\/script>/gi, ""))
  .replace("%FOOTER%", footer || "");
  
module.exports.renderPage = renderPage;

module.exports.renderStats = note => renderPage(note.id, deriveTitle(note.text), 
  `<h2>Statistics</h2>
  <table>
    <tr><td>Published</td><td>${note.published}</td></tr>
    <tr><td>Edited</td><td>${note.edited || "N/A"}</td></tr>
    <tr><td>Views</td><td>${note.views}</td></tr>
  </table>`);

module.exports.renderTOS = () => 
  renderPage("tos", "Terms of Service", marked(TOS));

module.exports.renderNote = note => renderPage(note.id, deriveTitle(note.text), 
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