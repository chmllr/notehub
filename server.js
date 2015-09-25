var express = require('express');
var page = require('./src/page');
var storage = require('./src/storage');
var app = express();

app.use(express.static(__dirname + '/resources/public'));

app.get('/new', function (req, res) {
    res.send("opening new note mask")
});

app.get(/(.*)\??.*/, function (req, res) {
  var link = req.params["0"].slice(1);
  storage.getNote(link).then(note => {
      res.send("opening note " + note.text)
  });
});

var server = app.listen(3000, function () {
  console.log('NoteHub server listening on port %s', server.address().port);
});
