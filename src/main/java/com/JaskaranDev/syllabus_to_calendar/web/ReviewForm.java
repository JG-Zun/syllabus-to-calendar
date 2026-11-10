package com.JaskaranDev.syllabus_to_calendar.web;

import java.util.ArrayList;
import java.util.List;

public class ReviewForm {

    private List<RowForm> rows = new ArrayList<>();

    public List<RowForm> getRows() { return rows; }
    public void setRows(List<RowForm> rows) { this.rows = rows; }
}