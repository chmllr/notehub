var $ = function(id) {
    return document.getElementById(id);
};
var iosDetected = navigator.userAgent.match('(iPad|iPod|iPhone)');
var timer = null;
var timerDelay = iosDetected ? 800 : 400;
var $note, $action, $preview, $plain_password, $tableau;
var backendTimer;

document.addEventListener('DOMContentLoaded', function () {
    marked.setOptions({
        langPrefix: 'hljs lang-',
        highlight: function (code) {
            return hljs.highlightAuto(code).value;
        },
    });
});

function md2html(input) {
    return marked(input);
}

function saveDraft() {
    if ($action == 'UPDATE') return;
    console.log('draft autosave...');
    $tableau.innerHTML = 'Draft autosaved.';
    localStorage.setItem('draft', $note.value);
}

function enableButton() {
    var checkbox = $('tos');
    var button = $('publish-button');
    button.disabled = !checkbox.checked;
}

function onLoad() {
    $note = $('note');
    $action = $('action').value;
    $preview = $('draft');
    $tableau = $('tableau');
    $plain_password = $('plain-password');
    var updatePreview = function() {
        clearTimeout(timer);
        var content = $note.value;
        var delay = Math.min(timerDelay, timerDelay * (content.length / 400));
        timer = setTimeout(function() {
            $preview.innerHTML = md2html(content);
            $tableau.innerHTML = content.split(/\s+/).length + ' words';
        }, delay);
    };
    if ($action == 'UPDATE') updatePreview();
    else {
        $('delete-button').style.display = 'none';
        $note.value = '';
        var draft = localStorage.getItem('draft');
        if (draft) {
            $note.value = draft;
            updatePreview();
        }
    }
    $note.onkeyup = updatePreview;
    $('delete-button').onclick = $('publish-button').onclick = function(e) {
        localStorage.removeItem('draft');
        self.onbeforeunload = null;
        if ($plain_password.value !== '') $('password').value = md5($plain_password.value);
        $plain_password.value = null;
        $('signature').value = md5($('session').value + $note.value.replace(/[\n\r]/g, ''));
    };
    if (iosDetected) $note.className += ' ui-border';
    else $note.focus();
    self.onbeforeunload = saveDraft;
    setInterval(saveDraft, 60 * 1000);
}
