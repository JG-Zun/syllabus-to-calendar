package com.JaskaranDev.syllabus_to_calendar.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportControllerTest {

    @Test
    void assignsCourseCodeFromBrightspaceCalendarName() {
        String ics = "BEGIN:VCALENDAR\r\nX-WR-CALNAME:CSI 2110 - Fall 2026\r\n"
                + "BEGIN:VEVENT\r\nUID:assignment-1\r\nSUMMARY:Assignment 1 due\r\n"
                + "DTSTART;VALUE=DATE:20261120\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
        MockMultipartFile file = new MockMultipartFile("file", "deadlines.ics", "text/calendar",
                ics.getBytes());
        MockHttpSession session = new MockHttpSession();

        new ImportController().importFeed(null, file, session, new RedirectAttributesModelMap());

        RowForm imported = UploadController.rows(session).get(0);
        assertEquals("CSI2110", imported.getCourse());
        assertEquals("Assignment 1", imported.getTitle());
    }

    @Test
    void prefersCourseCodeFoundInEventDescription() {
        String ics = "BEGIN:VCALENDAR\r\nX-WR-CALNAME:Other course calendar\r\n"
                + "BEGIN:VEVENT\r\nUID:project-1\r\nSUMMARY:Final Project due\r\n"
                + "DESCRIPTION:Course code: MAT 1320\\nAssignment instructions\r\n"
                + "URL:https://brightspace.example/assignment/1\r\nCOMMENT:Bring your lab manual\r\n"
                + "DTSTART;VALUE=DATE:20261120\r\n"
                + "END:VEVENT\r\nEND:VCALENDAR\r\n";
        MockMultipartFile file = new MockMultipartFile("file", "deadlines.ics", "text/calendar",
                ics.getBytes());
        MockHttpSession session = new MockHttpSession();

        new ImportController().importFeed(null, file, session, new RedirectAttributesModelMap());

                RowForm imported = UploadController.rows(session).get(0);
                assertEquals("MAT1320", imported.getCourse());
                assertEquals("Course code: MAT 1320\nAssignment instructions", imported.getDescription());
                assertEquals("COMMENT: Bring your lab manual", imported.getNotes());
                assertEquals("https://brightspace.example/assignment/1", imported.getLinks().get(0));
                assertEquals("Final Project", imported.fullTitle());
    }
}