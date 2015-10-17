var express = require('express');
var page = require('./src/page');
var storage = require('./src/storage');
var md5 = require('md5');
var LRU = require("lru-cache")
var bodyParser = require('body-parser');

var app = express();

app.use(bodyParser.urlencoded({ extended: true }));

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
  var body = req.body, session = body.session, note = body.note; 
  if (session.indexOf(getTimeStamp()) != 0)
    return sendResponse(res, 400, "Session expired");
  var expectedSignature = md5(session + note.replace(/[\n\r]/g, "")); 
  if (expectedSignature != body.signature)
    return sendResponse(res, 400, "Signature mismatch");
  sendResponse(res, 200, JSON.stringify(body));
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

var sendResponse = (res, code, message) => {
  res.status(code);
  res.send(message);
};

var server = app.listen(3000, function () {
  console.log('NoteHub server listening on port %s', server.address().port);
});
