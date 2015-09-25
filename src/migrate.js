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
  id: {
    type: Sequelize.INTEGER,
    autoIncrement: true,
    unique: true,
    primaryKey: true
  },
  text: Sequelize.TEXT,
  published: {
    type: Sequelize.DATE,
    defaultValue: Sequelize.NOW
  },
  edited: {
    type: Sequelize.DATE,
    allowNull: true,
    defaultValue: null
  },
  publisher: Sequelize.STRING(32),
  password: Sequelize.STRING(16),
  views: Sequelize.INTEGER,
});

var Link = sequelize.define('Link', {
  id: {
    type: Sequelize.STRING,
    unique: true,
    primaryKey: true
  },
  lastUsage: {
    type: Sequelize.DATE,
    allowNull: true,
    defaultValue: null
  },
  params: Sequelize.STRING,
  deprecatedId: Sequelize.STRING
});

Note.hasMany(Link);
Link.belongsTo(Note);

sequelize.sync().then(function() {
  client.hgetall("note", function(err, notes) {
    console.log("notes retrieved:", Object.keys(notes).length);
    client.hgetall("published", function(err, published) {
      console.log("published retrieved:", Object.keys(published).length);
      client.hgetall("publisher", function(err, publisher) {
        console.log("publisher retrieved:", Object.keys(publisher).length);
        client.hgetall("password", function(err, password) {
          console.log("password retrieved:", Object.keys(password).length);
          client.hgetall("views", function(err, views) {
            console.log("views retrieved:", Object.keys(views).length);
            client.hgetall("edited", function(err, edited) {
              console.log("edited retrieved:", Object.keys(edited).length);
              Object.keys(notes).forEach(function(id) {
                client.smembers(id + ":urls", function(err, links) {

                  Note.create({
                    text: notes[id],
                    published: published[id] && new Date(published[id] * 1000) || new Date(),
                    publisher: publisher[id].indexOf("NPY") == -1 && publisher[id] || "NoteHub",
                    password: password[id] && password[id].length == 32 && password[id],
                    edited: !isNaN(edited[id]) && edited[id] && new Date(edited[id] * 1000) || null,
                    views: views[id],
                  }).then(note => {

                    var createLink = LinkId => {
                      client.hget("short-url", LinkId, function(err, result) {


                        var obj = {};
                        if (result) {
                          result = result.replace(/:([\w_-]+)\s/g, '"$1":');
                          try {
                            obj = JSON.parse(result);
                            delete obj.title;
                            delete obj.day;
                            delete obj.year;
                            delete obj.month;
                          } catch (e) {
                            return console.log("PARSE ERROR FOR", result)
                          }
                        }
                        Link.create({
                          id: LinkId,
                          deprecatedId: id,
                          params: Object.keys(obj).length == 0 ? null : JSON.stringify(obj)
                        }).then(link => {

                          link.setNote(note);
                          note.addLink(link);

                        });
                      });
                    };

                    if (links.length == 0) {
                      var tmp = id.split("/");
                      var paramString = '{:day "' + tmp[2] +
                        '", :month "' + tmp[1] + '", :title "' + tmp[3] + '", :year "' + tmp[0] + '"}';
                      client.hget("short-url", paramString, function(err, result) {
                        if (!result) throw("oops:" + paramString + ":" + id);
                        createLink(result);
                      });
                    } else createLink(links[links.length - 1]);




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
