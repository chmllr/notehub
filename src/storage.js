var redis = require("redis"),
  client = redis.createClient();

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

var Note = sequelize.define('Note', {
  id: { type: Sequelize.STRING, unique: true, primaryKey: true },
  text: Sequelize.TEXT,
  published: { type: Sequelize.DATE, defaultValue: Sequelize.NOW },
  edited: { type: Sequelize.DATE, allowNull: true, defaultValue: null },
  publisher: Sequelize.STRING(32),
  password: Sequelize.STRING(16),
  views: Sequelize.INTEGER,
});

sequelize.sync().then(function () {
  client.hgetall("note", function (err, notes) {
    console.log("notes retrieved:", Object.keys(notes).length);
    client.hgetall("published", function (err, published) {
      console.log("published retrieved:", Object.keys(published).length);
      client.hgetall("publisher", function (err, publisher) {
        console.log("publisher retrieved:", Object.keys(publisher).length);
        client.hgetall("password", function (err, password) {
          console.log("password retrieved:", Object.keys(password).length);
          client.hgetall("views", function (err, views) {
            console.log("views retrieved:", Object.keys(views).length);
            client.hgetall("edited", function (err, edited) {
              console.log("edited retrieved:", Object.keys(edited).length);
              console.log("all infos retrieved");
              Object.keys(notes).forEach(function (id) {
                Note.create({
                  id: id,
                  text: notes[id],
                  published: published[id] && new Date(published[id] * 1000) || new Date(),
                  publisher: publisher[id].indexOf("NPY") == -1 && publisher[id] || "NoteHub",
                  password: password[id] && password[id].length == 32 && password[id],
                  edited: !isNaN(edited[id]) && edited[id] && new Date(edited[id] * 1000) || null,
                  views: views[id],
                });
              });
            });
          });
        });
      });
    });
  });
})
