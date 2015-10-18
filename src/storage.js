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
  id: { type: Sequelize.STRING(6), unique: true, primaryKey: true },
  deprecatedId: Sequelize.TEXT,
  text: Sequelize.TEXT,
  published: { type: Sequelize.DATE, defaultValue: Sequelize.NOW },
  edited: { type: Sequelize.DATE, allowNull: true, defaultValue: null },
  password: Sequelize.STRING(16),
  views: { type: Sequelize.INTEGER, defaultValue: 0 }
});

module.exports.getNote = id => {
  console.log("resolving note", id);
  return Note.findById(id);
}

module.exports.getNoteId = deprecatedId => {
  console.log("resolving deprecated Id", deprecatedId);
  return Note.findOne({
    where: { deprecatedId: deprecatedId }
  }).then(note => note.id);
}

var generateId = () => [1, 1, 1, 1, 1]
  .map(() => {
    var code = Math.floor(Math.random() * 36);
    return String.fromCharCode(code + (code < 10 ? 48 : 87));
  })
  .join("");

var getFreeId = () => {
  var id = generateId();
  return Note.findById(id).then(result => result ? getFreeId() : id);
};

module.exports.addNote = (note, password) => getFreeId().then(id => Note.create({
  id: id,
  text: note,
  password: password
}));

module.exports.updateNote = (id, password, text) => Note.findById(id).then(note => {
  if (!note || note.password !== password) return new Promise((resolve, reject) => {
    reject({ message: "Password is wrong" });
  });
  note.text = text;
  return note.save();
});