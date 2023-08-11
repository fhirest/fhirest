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
package com.kodality.kefhir.structure.defs;

import com.kodality.kefhir.structure.api.ResourceRepresentation;
import java.io.IOException;
import java.util.List;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.hl7.fhir.r5.formats.IParser;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.model.DomainResource;
import org.hl7.fhir.r5.model.Resource;

@Singleton
@RequiredArgsConstructor
public class HtmlRepresentation implements ResourceRepresentation {

  @Override
  public List<String> getMimeTypes() {
    return List.of("text/html");
  }

  @Override
  public String getName() {
    return "html";
  }

  @Override
  public String compose(Resource resource) {
    try {
      IParser p = new JsonParser().setOutputStyle(OutputStyle.PRETTY);
      String json = p.composeString(resource);
      json = StringEscapeUtils.escapeHtml4(json);
      String html = (resource instanceof DomainResource dr) ? dr.getText().getDivAsString() : null;
      String css = """
        body {
          margin: 8px
        }
        .header {
          margin: -8px;
          padding: 8px;
          font-size: 1.4em;
          border-bottom: 1px solid;
          padding-bottom: 13px;
          background: #fafafa;
        }
        .resource-text {
          margin-bottom: 20px;
          border: 1px solid #ddd;
          padding: 6px;
          border-radius: 4px;
        }
        .resource-text table tbody {
          font-size: 14px;
        }
        .resource-text table {
          width: 100%;
        }
        .resource-json {
          border: 1px solid #ddd;
          border-radius: 6px;
          background-color: #f5f5f5;
        }
        h1 {
          font-size: 1.8em;
          color: #787878;
          margin-bottom: 0px;
        }
      """;
      return "<html><head><style>"+css+"</style></head><body>"
             + "<div class=\"header\">This is a HTML representation of a resource. <a href=\"?_format=json\">Raw json</a></div>"
             + (html == null ? "" : "<h1>Resource narrative</h1><div class=\"resource-text\">" + html + "</div>")
             + "<h1>Resource json</h1><div class=\"resource-json\"><pre>" + json + "</pre></div>"
             + "</body></html>";
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isParsable(String input) {
    return false;
  }

  @Override
  public <R extends Resource> R parse(String input) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String prettify(String content) {
    return content;
  }

}
