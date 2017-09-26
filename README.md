# README

> "Make every detail perfect and limit the number of details to perfect."   
> — _Jack Dorsey_

## About

Dead simple hosting for markdown notes.

## Running

1. Install `dep` using Homebrew and run it inside project to install the dependencies: `dep ensure`.
2. Create a data base by running `make db`.
3. Run the app with `make run`.

### ENV variables used:

- For emailing of report abuse:
  - `SMTP_SERVER`: 
  - `SMTP_USER`
  - `SMTP_PASSWORD`
  - `NOTEHUB_ADMIN_EMAIL`
- Recaptcha secret:
  - `RECAPTCHA_SECRET`
- Test mode:
  - `TEST_MODE` (expected to be non-empty; skips captcha, no writes buffering for stats)

## Testing

1. Install `frisby`: `go get -u github.com/verdverm/frisby`.
2. Run `make tests`
