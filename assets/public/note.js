"use strict";

function post(url, vals, cb) {
    var data = new FormData();
    for (var key in vals) {
        data.append(key, vals[key]);
    }
    var xhr = new XMLHttpRequest();
    xhr.open('POST', url)
    xhr.onreadystatechange = function() { if (xhr.readyState === XMLHttpRequest.DONE) return cb(xhr.status, xhr.responseText) };
    xhr.send(data);
}

function report(id) {
    var resp = prompt("Please shortly explain the problem with this note.");
    if (resp) {
        post('/' + id + '/report', { "report": resp })
        alert("Thank you!")
    }
} 
