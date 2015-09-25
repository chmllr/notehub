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
  views: Sequelize.INTEGER,
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
