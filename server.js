var express = require('express');
var page = require('./src/page');
var app = express();

app.use(express.static(__dirname + '/resources/public'));

app.get('/api', function (req, res) {
  res.send(page.build("api"));
});

var server = app.listen(3000, function () {
  console.log('NoteHub server listening on port %s', server.address().port);
});