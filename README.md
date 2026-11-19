# نویسه — Persian Spell Checker

An Android app for correcting Persian text with the Nevise Hugging Face Space.

## Features

- Persian right-to-left interface
- Send text to the Nevise correction model
- Receive the result through Gradio's server-sent event stream
- Copy the corrected text or use it as the next input

## API

The app calls the Space at:

`https://soheybsamadi-nevise-api.hf.space`

It first sends `POST /gradio_api/call/correct_text` with a JSON `text` field, then listens to `GET /gradio_api/call/correct_text/{event_id}` until the `complete` event arrives.

## Build an APK

1. Open the repository's **Actions** tab.
2. Select **Build Android APK**.
3. Press **Run workflow** on the `main` branch.
4. When the workflow finishes, download the `Nevise-debug-apk` artifact.

The workflow is manual-only; ordinary pushes do not trigger an APK build.
