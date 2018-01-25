package main

import (
	"strings"

	simplejson "github.com/bitly/go-simplejson"
	"github.com/verdverm/frisby"
)

func main() {

	service := "http://localhost:3000"

	frisby.Create("Test Notehub landing page").
		Get(service).
		Send().
		ExpectHeader("Content-Type", "text/html; charset=utf-8").
		ExpectStatus(200).
		ExpectContent("Pastebin for One-Off Markdown Publishing")

	frisby.Create("Test Notehub TOS Page").
		Get(service+"/TOS.md").
		Send().
		ExpectHeader("Content-Type", "text/html; charset=UTF-8").
		ExpectStatus(200).
		ExpectContent("Terms of Service")

	frisby.Create("Test /new page").
		Get(service+"/new").
		Send().
		ExpectHeader("Content-Type", "text/html; charset=UTF-8").
		ExpectStatus(200).
		ExpectContent("Publish Note")

	frisby.Create("Test non-existing page").
		Get(service+"/xxyyzz").
		Send().
		ExpectStatus(404).
		ExpectHeader("Content-Type", "text/plain; charset=UTF-8").
		ExpectContent("Not found")

	frisby.Create("Test non-existing page: query params").
		Get(service + "/xxyyzz?q=v"). // TODO: test the same for valid note
		Send().
		ExpectStatus(404).
		ExpectContent("Not found")

	frisby.Create("Test non-existing page: alphabet violation").
		Get(service + "/login.php").
		Send().
		ExpectStatus(404).
		ExpectContent("Not found")

	frisby.Create("Test publishing: no input").
		Post(service+"/").
		Send().
		ExpectStatus(412).
		ExpectJson("Success", false).
		ExpectJson("Payload", "Precondition failed")

	frisby.Create("Test publishing attempt: only TOS set").
		Post(service+"/").
		SetData("tos", "on").
		Send().
		ExpectStatus(400).
		ExpectJson("Success", false).
		ExpectJson("Payload", "Bad request: note length not accepted")

	testNote := "# Hello World!\nThis is a _test_ note!"

	tooLongNote := testNote
	for len(tooLongNote) < 50000 {
		tooLongNote += tooLongNote
	}

	frisby.Create("Test publishing: too long note").
		Post(service+"/").
		SetData("tos", "on").
		SetData("text", tooLongNote).
		Send().
		ExpectStatus(400).
		ExpectJson("Success", false).
		ExpectJson("Payload", "Bad request: note length not accepted")

	var id string
	frisby.Create("Test publishing: correct inputs; no password").
		Post(service+"/").
		SetData("tos", "on").
		SetData("text", testNote).
		Send().
		ExpectStatus(201).
		ExpectJson("Success", true).
		AfterJson(func(F *frisby.Frisby, json *simplejson.Json, err error) {
			noteID, err := json.Get("Payload").String()
			if err != nil {
				F.AddError(err.Error())
				return
			}
			id = noteID
		})

	testNoteHTML := "<h1>Hello World!</h1>\n<p>This is a <em>test</em> note!</p>"
	frisby.Create("Test retrieval of new note").
		Get(service + "/" + id).
		Send().
		// simulate 3 requests (for stats)
		Get(service + "/" + id).
		Send().
		Get(service + "/" + id).
		Send().
		ExpectStatus(200).
		ExpectContent(testNoteHTML)

	frisby.Create("Test export of new note").
		Get(service+"/"+id+"/export").
		Send().
		ExpectStatus(200).
		ExpectHeader("Content-type", "text/plain; charset=UTF-8").
		ExpectContent(testNote)

	frisby.Create("Test opening fake service on note").
		Get(service + "/" + id + "/asd").
		Send().
		ExpectStatus(404).
		ExpectContent("Not Found")

	// TODO: fix this
	// frisby.Create("Test opening fake service on note 2").
	// 	Get(service + "/" + id + "/exports").
	// 	Send().
	// 	ExpectStatus(404).
	// 	ExpectContent("Not Found")

	frisby.Create("Test stats of new note").
		Get(service + "/" + id + "/stats").
		Send().
		ExpectStatus(200).
		ExpectContent("<tr><td>Views</td><td>4</td></tr>").
		ExpectContent("Published")

	frisby.Create("Test edit page of new note").
		Get(service+"/"+id+"/edit").
		Send().
		ExpectStatus(200).
		ExpectHeader("Content-type", "text/html; charset=UTF-8").
		ExpectContent(testNote)

	frisby.Create("Test invalid editing attempt: empty inputs").
		Post(service+"/").
		SetData("id", id).
		Send().
		ExpectStatus(412)

	frisby.Create("Test invalid editing attempt: tos only").
		Post(service+"/").
		SetData("id", id).
		SetData("tos", "on").
		Send().
		ExpectStatus(400).
		ExpectJson("Success", false).
		ExpectJson("Payload", "Bad request: password is empty")

	frisby.Create("Test invalid editing attempt: tos and password").
		Post(service+"/").
		SetData("id", id).
		SetData("tos", "on").
		SetData("password", "aazzss").
		Send().
		ExpectStatus(401).
		ExpectJson("Success", false).
		ExpectJson("Payload", "Unauthorized: password is wrong")

	frisby.Create("Test invalid editing attempt: tos and password, but too short note").
		Post(service+"/").
		SetData("id", id).
		SetData("tos", "on").
		SetData("text", "Test").
		SetData("password", "aazzss").
		Send().
		ExpectStatus(400).
		ExpectJson("Success", false).
		ExpectJson("Payload", "Bad request: note length not accepted")

	frisby.Create("Test publishing: correct inputs; with password").
		Post(service+"/").
		SetData("tos", "on").
		SetData("password", "aa11qq").
		SetData("text", testNote).
		Send().
		ExpectStatus(201).
		ExpectJson("Success", true).
		AfterJson(func(F *frisby.Frisby, json *simplejson.Json, err error) {
			noteID, err := json.Get("Payload").String()
			if err != nil {
				F.AddError(err.Error())
				return
			}
			id = noteID
		})

	updatedNote := strings.Replace(testNote, "is a", "is an updated", -1)
	frisby.Create("Test invalid editing attempt: tos only").
		Post(service+"/").
		SetData("id", id).
		SetData("tos", "on").
		Send().
		ExpectStatus(400).
		ExpectJson("Success", false).
		ExpectJson("Payload", "Bad request: password is empty")

	frisby.Create("Test invalid editing attempt: tos and wrong password").
		Post(service+"/").
		SetData("id", id).
		SetData("tos", "on").
		SetData("text", updatedNote).
		SetData("password", "aazzss").
		Send().
		ExpectStatus(401).
		ExpectJson("Success", false).
		ExpectJson("Payload", "Unauthorized: password is wrong")

	frisby.Create("Test editing: valid inputs, no tos").
		Post(service+"/").
		SetData("id", id).
		SetData("text", updatedNote).
		SetData("password", "aa11qq").
		Send().
		ExpectStatus(412).
		ExpectJson("Success", false).
		ExpectJson("Payload", "Precondition failed")

	frisby.Create("Test editing: valid inputs").
		Post(service+"/").
		SetData("id", id).
		SetData("tos", "on").
		SetData("text", updatedNote).
		SetData("password", "aa11qq").
		Send().
		ExpectStatus(200).
		ExpectJson("Success", true)

	frisby.Create("Test retrieval of updated note").
		Get(service + "/" + id).
		Send().
		ExpectStatus(200).
		ExpectContent(strings.Replace(testNoteHTML, "is a", "is an updated", -1))

	frisby.Create("Test export of new note").
		Get(service+"/"+id+"/export").
		Send().
		ExpectStatus(200).
		ExpectHeader("Content-type", "text/plain; charset=UTF-8").
		ExpectContent(updatedNote)

	frisby.Create("Test deletion: valid inputs").
		Post(service+"/").
		SetData("id", id).
		SetData("tos", "on").
		SetData("text", "").
		SetData("password", "aa11qq").
		Send().
		ExpectStatus(200).
		ExpectJson("Success", true)

	frisby.Create("Test retrieval of deleted note").
		Get(service + "/" + id).
		Send().
		ExpectStatus(404)

	fraudNote := "http://n.co https://a.co ftp://b.co"

	frisby.Create("Test publishing fraudulent note").
		Post(service+"/").
		SetData("tos", "on").
		SetData("password", "aa22qq").
		SetData("text", fraudNote).
		Send().
		ExpectStatus(201).
		ExpectJson("Success", true).
		AfterJson(func(F *frisby.Frisby, json *simplejson.Json, err error) {
			noteID, err := json.Get("Payload").String()
			if err != nil {
				F.AddError(err.Error())
				return
			}
			id = noteID
		})

	frisby.Create("Test new fraudulent note").
		Get(service+"/"+id).
		Send().
		ExpectStatus(200).
		ExpectHeader("Content-type", "text/html; charset=UTF-8").
		ExpectContent(`<a href="http://n.co">http://n.co</a> <a href="https://a.co">https://a.co</a> <a href="ftp://b.co">ftp://b.co</a>`)

	frisby.Create("Test export of fraudulent note").
		Get(service+"/"+id+"/export").
		Send().
		ExpectStatus(200).
		ExpectHeader("Content-type", "text/plain; charset=UTF-8").
		ExpectContent(fraudNote)

	// access fraudulent note more than 100 times
	f := frisby.Create("Test export of fraudulent note again")
	for i := 0; i < 100; i++ {
		f.Get(service + "/" + id).Send()
	}

	frisby.Create("Test stats of fradulent note").
		Get(service + "/" + id + "/stats").
		Send().
		ExpectStatus(200).
		ExpectContent("<tr><td>Views</td><td>102</td></tr>").
		ExpectContent("Published")

	frisby.Create("Test export of fraudulent note").
		Get(service+"/"+id+"/export").
		Send().
		ExpectStatus(403).
		ExpectHeader("Content-type", "text/plain; charset=UTF-8").
		ExpectContent("Forbidden")

	frisby.Create("Test deletion of fraudulent note: wrong password inputs").
		Post(service+"/").
		SetData("id", id).
		SetData("tos", "on").
		SetData("text", "").
		SetData("password", "aa11qq").
		Send().
		ExpectStatus(401).
		ExpectJson("Success", false)

	frisby.Create("Test deletion of fraudulent note: correct password inputs").
		Post(service+"/").
		SetData("id", id).
		SetData("tos", "on").
		SetData("text", "").
		SetData("password", "aa22qq").
		Send().
		ExpectStatus(200).
		ExpectJson("Success", true)

	frisby.Create("Test publishing malicious note").
		Post(service+"/").
		SetData("tos", "on").
		SetData("password", "qwerty").
		SetData("text", "Foo <script>alert(1)</script> Bar <iframe src=''></iframe>").
		Send().
		ExpectStatus(201).
		ExpectJson("Success", true).
		AfterJson(func(F *frisby.Frisby, json *simplejson.Json, err error) {
			noteID, err := json.Get("Payload").String()
			if err != nil {
				F.AddError(err.Error())
				return
			}
			id = noteID
		})

	frisby.Create("Test export of fraudulent note").
		Get(service + "/" + id).
		Send().
		ExpectStatus(200).
		ExpectContent("Foo  Bar")

	frisby.Create("Test deletion of malicious note").
		Post(service+"/").
		SetData("id", id).
		SetData("tos", "on").
		SetData("text", "").
		SetData("password", "qwerty").
		Send().
		ExpectStatus(200).
		ExpectJson("Success", true)

	frisby.Global.PrintReport()
}
