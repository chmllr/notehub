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
  id: { type: Sequelize.INTEGER, autoIncrement: true, unique: true, primaryKey: true },
  text: Sequelize.TEXT,
  published: { type: Sequelize.DATE, defaultValue: Sequelize.NOW },
  edited: { type: Sequelize.DATE, allowNull: true, defaultValue: null },
  publisher: Sequelize.STRING(32),
  password: Sequelize.STRING(16),
  views: Sequelize.INTEGER,
});

var Link = sequelize.define('Link', {
  id: { type: Sequelize.STRING, unique: true, primaryKey: true },
  lastUsage: { type: Sequelize.DATE, allowNull: true, defaultValue: null },
  params: Sequelize.STRING
});

Note.hasMany(Link);
Link.belongsTo(Note);

module.exports.getNote = linkId => {
  console.log("resolving note", linkId);
  return Link.findById(linkId).then(link => Note.findById(link.NoteId));
}
