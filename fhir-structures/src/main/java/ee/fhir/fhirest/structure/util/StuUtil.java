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
package ee.fhir.fhirest.structure.util;

import ee.fhir.fhirest.structure.api.ParseException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.time.DateUtils;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r5.model.Duration;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Identifier;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Period;

import static java.util.stream.Collectors.toList;

public final class StuUtil {
  private static final BigDecimal $24 = BigDecimal.valueOf(24);
  private static final BigDecimal $7 = BigDecimal.valueOf(7);
  private static final BigDecimal $30 = BigDecimal.valueOf(30);
  private static final BigDecimal $365 = BigDecimal.valueOf(365);

  private StuUtil() {
    //
  }

  public static <T> Map<String, T> toMapCodeables(List<CodeableConcept> cc, Function<Coding, T> valueMapper) {
    List<Coding> codings = cc.stream().flatMap(c -> c.getCoding().stream()).collect(toList());
    return toMapCodings(codings, valueMapper);
  }

  public static <T> Map<String, T> toMap(CodeableConcept cc, Function<Coding, T> valueMapper) {
    return toMapCodings(cc.getCoding(), valueMapper);
  }

  public static <T> Map<String, T> toMapCodings(List<Coding> cc, Function<Coding, T> valueMapper) {
    return toMap(cc.stream(), c -> c.getSystem(), valueMapper);
  }

  public static Map<String, Object> toMapExtensions(List<Extension> extensions) {
    return toMap(extensions.stream(), e -> e.getUrl(), e -> e.getValueAsPrimitive().getValue());
  }

  public static Map<String, DataType> toMap(Parameters params) {
    return toMap(params.getParameter().stream(), p -> p.getName(), p -> p.getValue());
  }

  /**
   * ignores null system
   */
  public static Map<String, String> toMapIdentifiers(List<Identifier> identifiers) {
    return toMap(identifiers.stream().filter(e -> e.getSystem() != null), e -> e.getSystem(), e -> e.getValue());
  }

  public static Map<ContactPointSystem, String> toMapContactPoint(List<ContactPoint> telecom, ContactPointUse use) {
    return toMap(telecom.stream().filter(e -> e.getUse() == use), e -> e.getSystem(), e -> e.getValue());
  }

  private static <T, K, V> Map<K, V> toMap(Stream<T> stream, Function<T, K> keyMapper, Function<T, V> valueMapper) {
    return stream.filter(o -> valueMapper.apply(o) != null).collect(Collectors.toMap(keyMapper, valueMapper));
  }

  public static boolean isInPeriod(Date date, Period period) {
    if (period == null) {
      return true;
    }
    return (period.getStart() == null || !period.getStart().after(date))
        && (period.getEnd() == null || !period.getEnd().before(date));
  }

  public static Date addDuration(Date date, Duration d) {
    if (date == null) {
      return null;
    }
    if (d == null || !d.hasValue()) {
      return date;
    }
    String code = d.hasCode() ? d.getCode() : guessUnitCode(d.getUnit());
    switch (code) {
    case "s":
      return DateUtils.addSeconds(date, d.getValue().intValue());
    case "min":
      return DateUtils.addMinutes(date, d.getValue().intValue());
    case "h":
      return DateUtils.addHours(date, d.getValue().intValue());
    case "d":
      return DateUtils.addDays(date, d.getValue().intValue());
    case "a":
      return DateUtils.addYears(date, d.getValue().intValue());
    case "wk":
      return DateUtils.addWeeks(date, d.getValue().intValue());
    case "mo":
      return DateUtils.addMonths(date, d.getValue().intValue());
    default:
      throw new ParseException("unknown duration code " + code);
    }
  }

  public static BigDecimal getDays(Duration d) {
    if (d == null || !d.hasValue()) {
      return BigDecimal.ZERO;
    }
    String code = d.hasCode() ? d.getCode() : guessUnitCode(d.getUnit());
    return getDays(d.getValue(), code);
  }

  public static BigDecimal getDays(BigDecimal value, String unit) {
    switch (unit) {
    case "d":
      return value;
    case "wk":
      return value.multiply($7);
    case "mo":
      return value.multiply($30);
    case "a":
      return value.multiply($365);
    default:
      throw new ParseException("unsupported duration code " + unit);
    }
  }

  public static BigDecimal getHours(BigDecimal value, String unit) {
    switch (unit) {
    case "h":
      return value;
    default:
      return getDays(value, unit).multiply($24);
    }
  }

  private static String guessUnitCode(String unit) {
    if (unit == null) {
      return null;
    }
    switch (unit) {
    case "day":
    case "days":
      return "d";
    case "hour":
    case "hours":
      return "h";
    case "week":
    case "weeks":
      return "wk";
    case "month":
    case "months":
      return "mo";
    default:
      return null;
    }
  }

}
