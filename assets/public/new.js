"use strict";

function $(id) { return document.getElementById(id) }

function toggleButton() { $('publish-button').disabled = !$('tos').checked }

function submitForm(token) {
    var id = $("id").value;
    var text = $("text").value;
    var deletion = id != "" && text == "";
    if (deletion && !confirm("Do you want to delete this note?")) {
        return;
    }
    var resp = post("/", { 
        "id": id,
        "text": text,
        "tos": $("tos").value,
        "password": $("password").value,
        "token": token
    }, function (status, responseRaw) {
        var response = JSON.parse(responseRaw);
        if (status < 400 && response.Success) {
            window.location.replace(deletion ? "/" : "/" + response.Payload)
        } else {
            grecaptcha.reset();
            $('feedback').innerHTML = status + ": " + response.Payload;
        }
    })
}
