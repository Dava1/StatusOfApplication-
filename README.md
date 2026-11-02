# StatusOfApplication

A Java-based utility for checking the status of a visa/application submitted through the Czech Foreign Police application-status portal.

This project fetches the official status page, extracts the form fields, builds the required POST parameters, and submits them to the public service endpoint. It then prints the returned HTML response for inspection and parsing.

## What this project does

- Opens the application status page
- Reads and analyzes the HTML form
- Prepares the required request parameters
- Sends a POST request to the FRS portal
- Prints the server response for debugging or further processing

## Main file

- `src/Main/StatusOfApplication.java`

## Prerequisites

- Java JDK 8 or newer
- Jsoup library
- Internet access to the FRS service

## Example usage

The application is launched from `main` in `StatusOfApplication.java`.

```bash
javac -cp "path/to/jsoup.jar" -d out src/Main/StatusOfApplication.java
java -cp "out:path/to/jsoup.jar" Main.StatusOfApplication
```

## Important note

The example in `main` currently uses hardcoded values for:

- application number
- application code
- application year

These values are passed directly to the form submission logic and should be replaced with real values before testing against a live application.

```java
String postParams = http.getFormParams(page, "21880", "DP", "2020");
```

## Project structure

```text
StatusOfApplication/
├── README.md
├── StatusOfApplication.iml
├── src/
│   └── Main/
│       └── StatusOfApplication.java
└── .gitignore
```

## Notes

This project is a practical Java example for working with form-based web requests and HTML parsing. It is useful for understanding how to interact with a public web form programmatically, but it may require updates if the target website changes its form fields or validation requirements.

## License

This project does not currently include a formal license file. If you plan to reuse or distribute it, add an appropriate license before publishing.
