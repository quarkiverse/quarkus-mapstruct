package io.quarkiverse.mapstruct.base_model;

import java.util.Calendar;
import java.util.Date;

public abstract class MapperHelper {
    Calendar mapToCalendar(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar;
    }
}
