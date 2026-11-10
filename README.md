# Syllabus to Calendar

Turn course deadlines from a syllabus or Brightspace calendar subscription into a calendar you can review, edit, and export.

## Live website

**[Open Syllabus to Calendar](https://syllabus-to-calendar-vrxd.onrender.com)**

The site is hosted on Render’s free tier, so it may take about a minute to wake up after a period of inactivity.

## What it does

- Extracts assignment, quiz, exam, and project dates from syllabus PDFs.
- Lets you review and edit detected event dates, titles, courses, and types before adding them to the calendar.
- Imports deadlines from a Brightspace calendar subscription URL or an `.ics` file.
- Shows event details—including course, description, notes, and available links—when you select an event on the calendar.
- Exports reviewed events as an `.ics` file for importing into another calendar app.
- Supports syncing events from an imported Brightspace subscription link while its session remains active.

## Using the website

1. Open the [live website](https://syllabus-to-calendar-vrxd.onrender.com).
2. Upload a syllabus PDF. Enter the term start date if the syllabus does not clearly identify the term.
3. Review the extracted events, correct anything needed, and choose **View calendar**.
4. To import Brightspace deadlines instead, paste a calendar subscription link or upload an `.ics` file, then review the imported events.
5. Select an event on the calendar to see its available details. Use **Export calendar** to download an `.ics` file.

## Run locally

### Requirements

- Java 25 or newer
- Git (to clone the repository)

The Maven wrapper is included, so a separate Maven installation is not required.

### Start the app

```sh
git clone https://github.com/JG-Zun/syllabus-to-calendar.git
cd syllabus-to-calendar
./mvnw spring-boot:run
```

Then open [http://localhost:8080](http://localhost:8080).

On Windows, use `mvnw.cmd spring-boot:run` instead of `./mvnw spring-boot:run`.

### Run tests

```sh
./mvnw test
```

## Hosting

The repository includes a [Render Blueprint](render.yaml) and [Dockerfile](Dockerfile) for deployment. Connect the GitHub repository to Render and deploy the Blueprint; Render will build and run the service using the port assigned by the platform.

Render’s free web services can spin down when idle, so the first visit after inactivity may be slow. The free service also has limited resources and an ephemeral filesystem.

## Data and privacy notes

This app currently has no user accounts or persistent database. Extracted events and the Brightspace subscription URL are kept in the server-side web session, which expires after 12 hours; a server restart or free-host sleep/restart may clear session data earlier. Uploaded PDFs are read for text extraction and are not saved as permanent files by the application.

The live website is public and does not require sign-in. Avoid using sensitive documents or private Brightspace subscription links on the public demo. Anyone with access to a subscription URL may be able to view its calendar feed.

## Built with

- Java and Spring Boot
- Thymeleaf
- Apache PDFBox
- Maven
