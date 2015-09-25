var marked = require("marked");
var fs = require("fs");

var template = fs.readFileSync("resources/template.html", "utf-8");
var buildHTML = (title, content) => template
	.replace("%TITLE%", title)
	.replace("%CONTENT%", content);

module.exports.build = note => buildHTML(note.title, marked(note.text));
