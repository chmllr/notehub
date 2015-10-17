var $ = function(id) {
  return document.getElementById(id);
}
var iosDetected = navigator.userAgent.match("(iPad|iPod|iPhone)");
var timer = null;
var timerDelay = iosDetected ? 800 : 400;
var show = function(elem) {
  elem.style.display = "block"
}
var $note, $action, $preview, $plain_password, $input_elems, $dashed_line, $proposed_title, updatePreview;
var backendTimer;

function md2html(input) {
  return marked(input);
}

function onLoad() {
  $note = $("note");
  $action = document.getElementsByTagName("form")[0].method;
  $preview = $("preview");
  $plain_password = $("plain-password");
  $proposed_title = $("proposed-title");
  $input_elems = $("input-elems");
  $dashed_line = $("dashed-line");
  updatePreview = function() {
    clearTimeout(timer);
    var content = $note.value;
    var delay = Math.min(timerDelay, timerDelay * (content.length / 400));
    timer = setTimeout(function() {
      show($dashed_line);
      show($input_elems);
      $preview.innerHTML = md2html(content);
    }, delay);
  };
  if ($action.value == "update") updatePreview();
  else $note.value = "";
  $note.onkeyup = updatePreview;
  $("publish-button").onclick = function(e) {
    if ($plain_password.value != "") $("password").value = md5($plain_password.value);
    $plain_password.value = null;
    $("signature").value = md5($("session").value + 
      $note.value.replace(/[\n\r]/g, ""));
  }
  if (iosDetected) $note.className += " ui-border";
  else $note.focus();
}
