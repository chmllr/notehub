var LRU = require("lru-cache"),
	marked = require("marked"),
	fs = require("fs");

var CACHE = new LRU(30); // create LRU cache of size 30
var template = fs.readFileSync("resources/template.html", "utf-8");
var buildHTML = (title, content) => template
	.replace("%TITLE%", title)
	.replace("%CONTENT%", content);
var apiPage = buildHTML("API", marked(fs.readFileSync("API.md", "utf-8")));

export var build = id => {
	if (CACHE.has(id)) return CACHE.get(id);
	var content = id == "api" ? apiPage : "This is page " + id;
	CACHE.set(id, content);
	return content;
};
