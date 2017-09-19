var frisby = require('frisby');

frisby.create('Landing page')
    .get('http://localhost:3000/')
    .expectStatus(200)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('Markdown Publishing')
    .toss();

frisby.create('Open note page')
    .get('http://localhost:3000/new')
    .expectStatus(200)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('Terms of Service')
    .toss();

frisby.create('Open TOS')
    .get('http://localhost:3000/TOS.md')
    .expectStatus(200)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('Site Terms of Use Modifications')
    .toss();

frisby.create('Incurrect URL')
    .get('http://localhost:3000/abcdef')
    .expectStatus(404)
    .expectBodyContains('Not found')
    .toss();

frisby.create('Invalid posting 1')
    .post('http://localhost:3000/note')
    .expectStatus(412)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('Precondition failed')
    .toss();

frisby.create('Invalid posting 2')
    .post('http://localhost:3000/note', { tos: "on" })
    .expectStatus(400)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('Bad request')
    .toss();

frisby.create('Invalid posting 3')
    .post('http://localhost:3000/note', {
        text: "too short",
        password: '',
        tos: 'on',
    })
    .expectStatus(400)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('length not accepted')
    .toss();

let testNote = 'This is a test note';

frisby.create('Invalid posting 4')
    .post('http://localhost:3000/note', {
        note: testNote
    })
    .expectStatus(412)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('Precondition failed')
    .toss();

frisby.create('Valid posting')
    .post('http://localhost:3000/note', {
        password: '',
        tos: 'on',
        text: testNote
    })
    .expectStatus(301)
    .expectHeaderContains('content-type', 'text/plain; charset=utf-8')
    .expectHeaderContains('Location', '/')
    .after(function(err, res, body) {
        let noteId = res.headers.location.replace('/', '');
        frisby.create('Read posted note')
            .get('http://localhost:3000/' + noteId)
            .expectStatus(200)
            .expectBodyContains(testNote)
            .after((err, res, body) => {
                frisby.create('Illegal note editing attempt with empty password')
                    .post('http://localhost:3000/note', {
                        id: noteId,
                        tos: 'on',
                        action: 'UPDATE',
                        text: testNote + '!!!',
                        password: ''
                    })
                    .expectStatus(400)
                    .expectBodyContains('password is empty')
                    .toss();
            })
            .after((err, res, body) => {
                frisby.create('Illegal note editing attempt')
                    .post('http://localhost:3000/note', {
                        id: noteId,
                        tos: 'on',
                        action: 'UPDATE',
                        text: testNote + '!!!',
                        password: 'aaabbb'
                    })
                    .expectStatus(401)
                    .expectBodyContains('id or password is wrong')
                    .toss();

            })
            .toss();
    })
    .toss();

frisby.create('Valid posting, editing and more')
    .post('http://localhost:3000/note', {
        password: 'aabbcc',
        tos: 'on',
        text: testNote
    })
    .expectStatus(301)
    .expectHeaderContains('Location', '/')
    .expectHeaderContains('content-type', 'text/plain; charset=utf-8')
    .after(function(err, res, body) {
        let noteId = res.headers.location.replace('/', '');
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
                        tos: 'on',
                        text: testNote + '!!!',
                        password: 'abbcc'
                    })
                    .expectStatus(401)
                    .expectBodyContains('password is wrong')
                    .toss();
            })
            .after((err, res, body) => {
                frisby.create('Valid note editing attempt')
                    .post('http://localhost:3000/note', {
                        id: noteId,
                        tos: 'on',
                        text: 'Changed text!',
                        password: 'aabbcc'
                    })
                    .expectStatus(301)
                    .after((err, res, body) => {
                        frisby.create('Read changed note')
                            .get('http://localhost:3000/' + noteId)
                            .expectStatus(200)
                            .expectBodyContains('Changed text!') 
                            .toss();
                    })
                    .toss();
            })
            .toss();
        frisby.create('Read export of posted note')
            .expectStatus(200)
            .get('http://localhost:3000/' + noteId + '/export')
            .expectHeaderContains('content-type', 'text/plain; charset=utf-8')
            .expectBodyContains(testNote)
            .toss();
        frisby.create('Open /edit on posted note')
            .expectStatus(200)
            .expectBodyContains('<textarea autofocus name="text">' + testNote + '</textarea>')
            .get('http://localhost:3000/' + noteId + '/edit')
            .toss();
        frisby.create('Read stats of posted note')
            .get('http://localhost:3000/' + noteId + '/stats')
            .expectHeaderContains('content-type', 'text/html; charset=utf-8')
            .expectStatus(200)
            .expectBodyContains('Statistics')
            .expectBodyContains('<tr><td>Views</td><td>0</td></tr>')
            .toss();
    })
    .toss();

var tooLongNote = 'ABCD';

while (tooLongNote.length < 1024*200) tooLongNote += tooLongNote;

frisby.create('Invalid posting of too long note')
    .post('http://localhost:3000/note', {
        action: 'POST',
        tos: 'on',
        password: 'aabbcc',
        text: tooLongNote
    })
    .expectStatus(400)
    .expectHeaderContains('content-type', 'text/html; charset=utf-8')
    .expectBodyContains('length not accepted')
    .toss();

