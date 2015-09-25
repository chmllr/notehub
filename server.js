var express = require('express');
var page = require('./src/page');
var storage = require('./src/storage');
var LRU = require("lru-cache");

var app = express();
var CACHE = new LRU(30);

app.use(express.static(__dirname + '/resources/public'));

app.get('/new', function (req, res) {
    res.send("opening new note mask")
});

app.get("/:year/:month/:day/:title", function (req, res) {
  var P = req.params;
  storage.getNoteId(P.year + "/" + P.month + "/" + P.day + "/" + P.title)
    .then(id => res.redirect("/" + id));
});

app.get(/\/([a-zA-Z0-9]*)/, function (req, res) {
  var link = req.params["0"].toLowerCase();
  if (CACHE.has(link)) res.send(CACHE.get(link));
  else storage.getNote(link).then(note => {
    var content = page.build(note);
    CACHE.set(link, content);
    res.send(content);
  });
});

var server = app.listen(3000, function () {
  console.log('NoteHub server listening on port %s', server.address().port);
});
