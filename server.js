var express = require('express');
var view = require('./view');
var storage = require('./storage');
var md5 = require('md5');
var LRU = require("lru-cache")
var bodyParser = require('body-parser');
var fs = require('fs');
var blackList;

var app = express();

app.use(bodyParser.urlencoded({ extended: true, limit: '200kb' }));
app.use(express.static(__dirname + '/resources/public'));
app.use(function (error, req, res, next) {
    if (error) {
        sendResponse(res, 400, "Bad request", error.message);
        log("REQUEST ERROR:", error);
    } else {
        next();
    }
});

var MODELS = {};
var CACHE = new LRU({
    max: 50,
    dispose: key => {
        log("disposing", key, "from cache");
        var model = MODELS[key];
        model && model.save();
        delete MODELS[key];
    }
});

var log = function() {
    var date = new Date();
    var timestamp = date.getDate() + "/" + date.getMonth() + " " + date.getHours() + ":" +
        date.getMinutes() + ":" + date.getSeconds() + "." + date.getMilliseconds();
    var message = Array.prototype.slice.call(arguments);
    message.unshift("--");
    message.unshift(timestamp);
    console.log.apply(console, message);
}

app.get('/TOS', (req, res) => res.send(view.renderTOS()));

app.get('/new', (req, res) => {
    log(req.ip, "opens /new");
    res.send(view.newNotePage(md5("new")));
});

app.post('/note', (req, res) => {
    var body = req.body,
        session = body.session,
        note = body.note,
        password = body.password,
        action = body.action,
        id = body.id;
    log(req.ip, "calls /note to", action, id);
    var goToNote = note => res.redirect("/" + note.id);
    if (!note || session.indexOf(md5('edit/' + id)) != 0 && session.indexOf(md5('new')) != 0)
        return sendResponse(res, 400, "Invalid session");
    if (body.signature != md5(session + note.replace(/[\n\r]/g, "")))
        return sendResponse(res, 400, "Signature mismatch");
    if (action == "POST")
        storage.addNote(note, password).then(goToNote);
    else {
        CACHE.del(id);
        if (body.button == "Delete") {
            log("deleting note", id);
            storage.deleteNote(id, password).then(
                () => sendResponse(res, 200, "Note deleted"),
                error => sendResponse(res, 400, "Bad request", error.message));
        } else {
            log("updating note", id);
            storage.updateNote(id, password, note).then(goToNote,
                error => sendResponse(res, 400, "Bad request", error.message));
        }
    }
});

app.get("/:year/:month/:day/:title", (req, res) => {
    var P = req.params, url = P.year + "/" + P.month + "/" + P.day + "/" + P.title;
    log(req.ip, "resolves deprecated id", url);
    if (CACHE.has(url)) {
        log(url, "is cached!");
        var id = CACHE.get(url);
        if (id) res.redirect("/" + id);
        else notFound(res);
    } else storage.getNoteId(url).then(note => {
        log(url, "is not cached, resolving...");
        if (note) {
            CACHE.set(url, note.id);
            res.redirect("/" + note.id)
        } else {
            CACHE.set(url, null);
            notFound(res);
        }
    });
});

app.get(/\/([a-z0-9]+)\/edit/, (req, res) => {
    var id = req.params["0"];
    log(req.ip, "calls /edit on", id);
    storage.getNote(id).then(note => res.send(note
        ? view.editNotePage(md5('edit/' + id), note)
        : notFound(res)));
});

app.get(/\/([a-z0-9]+)\/export/, (req, res) => {
    var id = req.params["0"];
    log(req.ip, "calls /export on", id);
    res.set({ 'Content-Type': 'text/plain', 'Charset': 'utf-8' });
    storage.getNote(id).then(note => note
        ? res.send(note.text)
        : notFound(res));
});

app.get(/\/([a-z0-9]+)\/stats/, (req, res) => {
    var id = req.params["0"];
    log(req.ip, "calls /stats on", id);
    var promise = id in MODELS
        ? new Promise(resolve => resolve(MODELS[id]))
        : storage.getNote(id);
    promise.then(note => note
        ? res.send(view.renderStats(note))
        : notFound(res));
});

app.get(/\/([a-z0-9]+)/, (req, res) => {
    var id = req.params["0"];
    log(req.ip, "open note", id, "from", req.get("Referer"));
    if (CACHE.has(id)) {
        log(id, "is cached!");
        var note = MODELS[id];
        if (!note) return notFound(res);
        note.views++;
        res.send(CACHE.get(id));
    } else storage.getNote(id).then(note => {
        log(id, "is not cached, resolving...");
        if (!note) {
            CACHE.set(id, null);
            return notFound(res);
        }
        var content = view.renderNote(note, blackList);
        CACHE.set(id, content);
        MODELS[id] = note;
        note.views++;
        res.send(content);
    });
});

var sendResponse = (res, code, message, details) => {
    log("sending response", code, message);
    res.status(code).send(view.renderPage(null, message, 
        `<h1>${message}</h1><br/>` +
        `<center>${details || "¯\\_(ツ)_/¯"}</center>`, ""));
}

var notFound = res => sendResponse(res, 404, "Not found");

var server = app.listen(process.env.PORT || 3000, 
    () => log('NoteHub server listening on port', server.address().port));

setInterval(() => {
    var keys = Object.keys(MODELS);
    log("saving stats for", keys.length, "models...");
    keys.forEach(id => MODELS[id].save())
}, 5 * 60 * 1000);

var updateBlackList = () => {
    var ids = fs.readFileSync(process.env.BLACK_LIST || "/dev/null", "utf-8").split(/\n+/).filter(Boolean);
    ids.forEach(id => CACHE.del(id))
    blackList = new Set(ids);
    log("black list updated, entries:", blackList.size);
};

setInterval(updateBlackList, 60 * 60 * 1000)

updateBlackList();
