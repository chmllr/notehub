var express = require('express');
var page = require('./src/page');
var storage = require('./src/storage');
var md5 = require('md5');
var LRU = require("lru-cache");

var app = express();
var CACHE = new LRU(30);

var getTimeStamp = () => {
  var timestamp = new Date().getTime();
  timestamp = Math.floor(timestamp / 10000000);
  return (timestamp).toString(16)
}

app.use(express.static(__dirname + '/resources/public'));

app.get('/new', function (req, res) {
  res.send(page.newNotePage(getTimeStamp() + md5(Math.random())));
});

app.post('/note', function (req, res) {
  console.log(req.params);
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
    var content = page.buildNote(note);
    CACHE.set(link, content);
    res.send(content);
  });
});

var server = app.listen(3000, function () {
  console.log('NoteHub server listening on port %s', server.address().port);
});
