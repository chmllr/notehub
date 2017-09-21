package main

import (
	"fmt"
	"net/smtp"
	"os"
)

func email(id, text string) error {
	smtpServer := os.Getenv("SMTP_SERVER")
	auth := smtp.PlainAuth("", os.Getenv("SMTP_USER"), os.Getenv("SMTP_PASSWORD"), smtpServer)
	to := []string{os.Getenv("NOTEHUB_ADMIN_EMAIL")}
	msg := []byte("Subject: Note reported\r\n\r\n" +
		fmt.Sprintf("Note https://notehub.org/%s was reported: %q\r\n", id, text))
	return smtp.SendMail(smtpServer+":587", auth, to[0], to, msg)
}
