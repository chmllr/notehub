var express = require('express');
var view = require('./src/view');
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
  res.send(view.newNotePage(getTimeStamp() + md5(Math.random())));
});

app.post('/note', function (req, res) {
  var body = req.body, 
  session = body.session, 
  note = body.note, 
  password = md5(body.password);
  var goToNote = note => res.redirect("/" + note.id);
  if (session.indexOf(getTimeStamp()) != 0)
    return sendResponse(res, 400, "Session expired");
  var expectedSignature = md5(session + note.replace(/[\n\r]/g, ""));
  if (expectedSignature != body.signature)
    return sendResponse(res, 400, "Signature mismatch");
    console.log(body)
  if (body.action == "POST")
    storage.addNote(note, password).then(goToNote);
  else
    storage.updateNote(body.id, password, note).then(note => {
      CACHE.del(note.id);
      goToNote(note);
    },
      error => sendResponse(res, 403, error.message))
});

app.get("/:year/:month/:day/:title", function (req, res) {
  var P = req.params;
  storage.getNoteId(P.year + "/" + P.month + "/" + P.day + "/" + P.title)
    .then(id => res.redirect("/" + id));
});

app.get(/\/([a-z0-9]+\/edit)/, function (req, res) {
  var link = req.params["0"].replace("/edit", "");
  storage.getNote(link).then(note => res.send(note 
  ? view.editNotePage(getTimeStamp() + md5(Math.random()), note)
  : notFound(res)));
});

app.get(/\/([a-z0-9]+\/export)/, function (req, res) {
  var link = req.params["0"].replace("/export", "");
  res.set({ 'Content-Type': 'text/plain', 'Charset': 'utf-8' });
  storage.getNote(link).then(note => note 
  ? res.send(note.text)
  : notFound(res));
});

app.get(/\/([a-z0-9]+\/stats)/, function (req, res) {
  var link = req.params["0"].replace("/stats", "");
  storage.getNote(link).then(note => note 
  ? res.send(view.buildStats(note))
  : notFound(res));
});

app.get(/\/([a-z0-9]+)/, function (req, res) {
  var link = req.params["0"];
  if (CACHE.has(link)) res.send(CACHE.get(link));
  else storage.getNote(link).then(note => {
    if (!note) return notFound(res);
    var content = view.buildNote(note);
    CACHE.set(link, content);
    res.send(content);
  });
});

var sendResponse = (res, code, message) =>
  res.status(code).send(view.buildPage(message, "<h1>" + message + "</h1>", "")); 

var notFound = res => sendResponse(res, 404, "Not found");

var server = app.listen(3000, function () {
  console.log('NoteHub server listening on port %s', server.address().port);
});
