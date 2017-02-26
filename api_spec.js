var frisby = require('frisby');
var md5 = require('md5');

frisby.create('Landing page')
    .get('http://localhost:3000/')
    .expectStatus(200)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('Hassle-free')
    .toss();

frisby.create('Open note page')
    .get('http://localhost:3000/new')
    .expectStatus(200)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('Terms of Service')
    .toss();

frisby.create('Open TOS')
    .get('http://localhost:3000/TOS')
    .expectStatus(200)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('Site Terms of Use Modifications')
    .toss();

frisby.create('Incurrect URL')
    .get('http://localhost:3000/abcdef')
    .expectStatus(404)
    .expectBodyContains('Not found')
    .toss();

frisby.create('Invalid posting')
    .post('http://localhost:3000/note')
    .expectStatus(400)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('Bad request')
    .toss();

let testNote = 'This is a test note';

frisby.create('Invalid posting 2')
    .post('http://localhost:3000/note', {
        action: 'POST',
        note: testNote
    })
    .expectStatus(400)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('Bad request')
    .toss();

frisby.create('Invalid posting 3')
    .post('http://localhost:3000/note', {
        action: 'POST',
        session: md5('new'),
        signature: 'assdss',
        note: testNote
    })
    .expectStatus(400)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('Signature mismatch')
    .toss();

frisby.create('Valid posting')
    .post('http://localhost:3000/note', {
        action: 'POST',
        session: md5('new'),
        signature: md5(md5('new') + testNote),
        password: '',
        note: testNote
    })
    .expectStatus(302)
    .expectBodyContains('Found. Redirecting to')
    .expectHeaderContains('content-type', 'text/plain; charset=utf-8')
    .after(function(err, res, body) {
        let noteId = body.replace('Found. Redirecting to /', '');
        frisby.create('Read posted note')
            .get('http://localhost:3000/' + noteId)
            .expectStatus(200)
            .expectBodyContains(testNote)
            .after((err, res, body) => {
                frisby.create('Illegal note editing attempt with empty password')
                    .post('http://localhost:3000/note', {
                        id: noteId,
                        action: 'UPDATE',
                        session: md5('new'),
                        signature: md5(md5('new') + testNote+'!!!'),
                        note: testNote + '!!!',
                        password: ''
                    })
                    .expectStatus(400)
                    .expectBodyContains('Password is wrong')
                    .toss();
            })
            .after((err, res, body) => {
                frisby.create('Illegal note editing attempt')
                    .post('http://localhost:3000/note', {
                        id: noteId,
                        action: 'UPDATE',
                        session: md5('new'),
                        signature: md5(md5('new') + testNote+'!!!'),
                        note: testNote + '!!!',
                        password: 'aaabbb'
                    })
                    .expectStatus(400)
                    .expectBodyContains('Password is wrong')
                    .toss();

            })
            .toss();
    })
    .toss();

frisby.create('Valid posting, editing and removal')
    .post('http://localhost:3000/note', {
        action: 'POST',
        session: md5('new'),
        signature: md5(md5('new') + testNote),
        password: 'aabbcc',
        note: testNote
    })
    .expectStatus(302)
    .expectBodyContains('Found. Redirecting to')
    .expectHeaderContains('content-type', 'text/plain; charset=utf-8')
    .after(function(err, res, body) {
        var noteId = body.replace('Found. Redirecting to /', '');
        frisby.create('Export posted note')
            .get('http://localhost:3000/' + noteId + '/export')
            .expectStatus(200)
            .expectHeaderContains('content-type', 'text/plain; charset=utf-8')
            .expectBodyContains(testNote)
            .toss();
        frisby.create('Read posted note')
            .get('http://localhost:3000/' + noteId)
            .expectStatus(200)
            .expectBodyContains(testNote)
            .expectHeaderContains('content-type', 'text/html; charset=utf-8')
            .after((err, res, body) => {
                frisby.create('Unauthorized note editing attempt')
                    .post('http://localhost:3000/note', {
                        id: noteId,
                        action: 'UPDATE',
                        session: md5('new'),
                        signature: md5(md5('new') + testNote+'!!!'),
                        note: testNote + '!!!',
                        password: 'abbcc'
                    })
                    .expectStatus(400)
                    .expectBodyContains('Password is wrong')
                    .toss();
            })
            .after((err, res, body) => {
                frisby.create('Valid note editing attempt')
                    .post('http://localhost:3000/note', {
                        id: noteId,
                        action: 'UPDATE',
                        session: md5('new'),
                        signature: md5(md5('new') + 'Changed!'),
                        note: 'Changed!',
                        password: 'aabbcc'
                    })
                    .expectStatus(302)
                    .after((err, res, body) => {
                        frisby.create('Read changed note')
                            .get('http://localhost:3000/' + noteId)
                            .expectStatus(200)
                            .expectBodyContains('Changed!') 
                            .toss();
                    })
                    .after((err, res, body) => {
                        frisby.create('Delete posted note')
                            .post('http://localhost:3000/note',{
                                id: noteId,
                                button: 'Delete',
                                action: 'UPDATE',
                                session: md5('new'),
                                signature: md5(md5('new') + 'Changed!'),
                                note: 'Changed!',
                                password: 'aabbcc'
                            })
                            .expectStatus(200)
                            .expectBodyContains('Note deleted') 
                            .toss();
                    })
                    .toss();
            })
            .toss();
        frisby.create('Read stats of posted note')
            .expectStatus(200)
            .get('http://localhost:3000/' + noteId).toss();
        frisby.create('Read stats of posted note')
            .expectStatus(200)
            .get('http://localhost:3000/' + noteId).toss();
        frisby.create('Read stats of posted note')
            .expectStatus(200)
            .get('http://localhost:3000/' + noteId).toss();
        frisby.create('Read stats of posted note')
            .get('http://localhost:3000/' + noteId + '/stats')
            .expectHeaderContains('content-type', 'text/html; charset=utf-8')
            .expectStatus(200)
            .expectBodyContains('Statistics')
            .expectBodyContains('<tr><td>Views</td><td>4</td></tr>')
            .toss();
    })
    .toss();

var tooLongNote = 'ABCD';

while (tooLongNote.length < 1024*200) tooLongNote += tooLongNote;

frisby.create('Invalid posting of too long note')
    .post('http://localhost:3000/note', {
        action: 'POST',
        session: md5('new'),
        signature: md5(md5('new') + testNote),
        password: 'aabbcc',
        note: tooLongNote
    })
    .expectStatus(400)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('too large')
    .toss();

frisby.create('Invalid update without id')
    .post('http://localhost:3000/note', {
        action: 'UPDATE',
        session: md5('new'),
        signature: md5(md5('new') + 'Any note'),
        password: 'aabbcc',
        note: 'Any note'
    })
    .expectStatus(400)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('Wrong note ID')
    .toss();

