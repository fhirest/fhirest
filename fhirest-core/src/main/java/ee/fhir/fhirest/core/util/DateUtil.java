/*
 * MIT License
 *
 * Copyright (c) 2024 FHIRest community
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ee.fhir.fhirest.core.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

public class DateUtil {
  public static final String DATE = "yyyy-MM-dd";
  public static final String ISO_DATETIME = "yyyy-MM-dd'T'HH:mm:ssX";
  public static final String FHIR_DATETIME = "yyyy-MM-dd'T'HH:mm:ssXXX";
  public static final String RFC_DATETIME = "yyyy-MM-dd'T'HH:mm:ssZ";
  public static final String ISO_DATETIME_MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
  public static final String TIMESTAMP_PG = "yyyy-MM-dd HH:mm:ssX";

  public static String format(Date date, String pattern) {
    return date == null ? null : new SimpleDateFormat(pattern).format(date);
  }

  public static String reformat(String dateString, String pattern) {
    return new SimpleDateFormat(pattern).format(parse(dateString));
  }

  public static Date parse(String date) {
    if (date == null) {
      return null;
    }
    return parse(date, TIMESTAMP_PG, ISO_DATETIME_MILLIS, ISO_DATETIME, FHIR_DATETIME, DATE)
        .orElseThrow(() -> new IllegalArgumentException("Cannot parse date: " + date));
  }

  public static Optional<Date> parse(String date, String... formats) {
    if (date == null) {
      return null;
    }
    for (String format : formats) {
      try {
        return Optional.of(new SimpleDateFormat(format).parse(date));
      } catch (ParseException e) {
        // next try
      }
    }
    return Optional.empty();
  }

  public static boolean isInPeriod(Date date, Date start, Date end) {
    return (start == null || !start.after(date)) && (end == null || !end.before(date));
  }

  public static LocalDateTime toLocalDateTime(Date date) {
    return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
  }

}
