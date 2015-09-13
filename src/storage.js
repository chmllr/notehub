var redis = require("redis"),
  client = redis.createClient({
      return_buffers: true,
      detect_buffers: false
    });

var zlib = require('zlib');

var Sequelize = require('sequelize');
var sequelize = new Sequelize('database', null, null, {
  dialect: 'sqlite',
  pool: {
    max: 5,
    min: 0,
    idle: 10000
  },
  storage: 'database.sqlite'
});

client.hget("note", "2015/7/12/hom", function (err, B) {
  debugger
  console.log("to unpack:", B.toString("base64"));
  var R = zlib.gunzipSync(B);
  console.log(R);
});

/*
var User = sequelize.define('User', {
  username: Sequelize.STRING,
  birthday: Sequelize.DATE
});

sequelize.sync().then(function() {
  return User.create({
    username: 'janedoe',
    birthday: new Date(1980, 6, 20)
  });
}).then(function(jane) {
  console.log(jane.get({
    plain: true
  }))
});

*/