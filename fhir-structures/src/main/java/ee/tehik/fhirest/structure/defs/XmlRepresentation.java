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
package ee.tehik.fhirest.structure.defs;

import ee.tehik.fhirest.structure.api.ParseException;
import ee.tehik.fhirest.structure.api.ResourceRepresentation;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.formats.IParser;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.stereotype.Component;

@Component
public class XmlRepresentation implements ResourceRepresentation {

  @Override
  public List<String> getMimeTypes() {
    return Arrays.asList("application/fhir+xml", "application/xml+fhir", "application/xml", "text/xml", "xml");
  }

  @Override
  public String getName() {
    return "xml";
  }

  @Override
  public String compose(Resource resource) {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      XmlParser parser = new XmlParser();
      parser.compose(output, resource, true);
      return output.toString(StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isParsable(String input) {
    String strip = StringUtils.stripStart(input, null);
    return StringUtils.startsWith(strip, "<");
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R extends Resource> R parse(String input) {
    try {
      return (R) new XmlParser().parse(input);
    } catch (Exception e) {
      throw new ParseException(e);
    }
  }

  @Override
  public String prettify(String content) {
    //TODO: ugly
    IParser p = new XmlParser().setOutputStyle(OutputStyle.PRETTY);
    try {
      return p.composeString(p.parse(content));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
