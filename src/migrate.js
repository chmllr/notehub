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

var Shortcut = sequelize.define('Shortcut', {
  id: { type: Sequelize.STRING, unique: true, primaryKey: true },
  lastResolution: { type: Sequelize.DATE, allowNull: true, defaultValue: null },
  params: Sequelize.STRING
});

Note.hasMany(Shortcut);
Shortcut.belongsTo(Note);

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
              Object.keys(notes).forEach(function (id) {
                client.smembers(id + ":urls", function (err, links) {

                  Note.create({
                    id: id,
                    text: notes[id],
                    published: published[id] && new Date(published[id] * 1000) || new Date(),
                    publisher: publisher[id].indexOf("NPY") == -1 && publisher[id] || "NoteHub",
                    password: password[id] && password[id].length == 32 && password[id],
                    edited: !isNaN(edited[id]) && edited[id] && new Date(edited[id] * 1000) || null,
                    views: views[id],
                  }).then(note => {

                    links.forEach(shortcutId => {
                      client.hget("short-url", shortcutId, function (err, result) {

                        result = result.replace(/:([\w_-]+)\s/g, '"$1":');

                        var obj = {};
                        try {
                          obj = JSON.parse(result);
                          delete obj.title;
                          delete obj.day;
                          delete obj.year;
                          delete obj.month;

                        } catch (e) {
                          return console.log("PARSE ERROR FOR", result)
                        }

                        Shortcut.create({
                          id: shortcutId,
                          params: Object.keys(obj).length == 0 ? null : JSON.stringify(obj)
                        }).then(shortcut => {

                          shortcut.setNote(note);
                          note.addShortcut(shortcut);

                        })
                      });
                    });
                  });
                })
              });
            });
          });
        });
      });
    });
  });
})
