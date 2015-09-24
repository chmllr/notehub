var express = require('express');
var page = require('./src/page');
var app = express();

app.use(express.static(__dirname + '/resources/public'));

app.get('/:year/:month/:day/:title', function (req, res) {
  var params = req.params;
  var id = params.year + "/" + params.month + "/" + params.day + "/" + params.title;
  res.send("opening note " + id);  
});

app.get('/:link', function (req, res) {
  
});


var server = app.listen(3000, function () {
  console.log('NoteHub server listening on port %s', server.address().port);
});