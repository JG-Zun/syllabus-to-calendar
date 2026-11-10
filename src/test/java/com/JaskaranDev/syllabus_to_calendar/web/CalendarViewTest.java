package com.JaskaranDev.syllabus_to_calendar.web;

import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;
import com.JaskaranDev.syllabus_to_calendar.model.EventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CalendarViewTest {

    @Autowired
    private org.springframework.web.context.WebApplicationContext context;

    @Test
    void rendersImportedEventDetailsOnCalendar() throws Exception {
        MockHttpSession session = new MockHttpSession();
        RowForm event = RowForm.from(new CalendarEvent("Soldering lab submission", LocalDate.of(2026, 11, 9),
                EventType.ASSIGNMENT), "GNG2101B");
        event.setDescription("Follow the lab manual");
        event.setUrl("https://brightspace.example/assignment/1");
        event.setNotes("Location: Engineering lab");
        event.setFeedUid("feed-1");
        event.setFeedTitle("Soldering lab submission");
        event.setFeedDate("2026-11-09");
        session.setAttribute(UploadController.ROWS, List.of(event));

        MockMvc mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(post("/review/done").session(session)
                .param("rows[0].keep", "on")
                .param("_rows[0].keep", "on")
                .param("rows[0].date", "2026-11-09")
                .param("rows[0].type", "ASSIGNMENT")
                .param("rows[0].course", "GNG2101B")
                .param("rows[0].title", "Soldering lab submission")
                        .param("rows[0].feedUid", "feed-1")
                        .param("rows[0].feedTitle", "Soldering lab submission")
                        .param("rows[0].feedDate", "2026-11-09"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/calendar"));

        MvcResult result = mvc.perform(get("/calendar").session(session))
                .andExpect(status().isOk())
                .andReturn();
        ModelAndView view = result.getModelAndView();

        assertEquals("calendar", view.getViewName());
        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("Soldering lab submission"));
        assertTrue(html.contains("GNG2101B"));
        assertTrue(html.contains("Follow the lab manual"));
        assertTrue(html.contains("Location: Engineering lab"));
        assertTrue(html.contains("https://brightspace.example/assignment/1"));
    }
}