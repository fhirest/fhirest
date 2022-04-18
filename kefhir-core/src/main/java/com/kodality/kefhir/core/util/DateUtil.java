/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package com.kodality.kefhir.core.util;

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
    return new SimpleDateFormat(pattern).format(date);
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
