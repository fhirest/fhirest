/*
 * MIT License
 *
 * Copyright (c) 2024 FHIREST community
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

package ee.fhir.fhirest.structure.defs;

import ee.fhir.fhirest.structure.api.ResourceRepresentation;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.hl7.fhir.r5.formats.IParser;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.model.DomainResource;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.stereotype.Component;

@Component
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
      String script = """
        function addUrlParameter(name, value) {
          const urlParams = new URLSearchParams(window.location.search);
          urlParams.set(name, value);
          window.location.search = decodeURIComponent(urlParams);
        }
      """;
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
      return "<html><head><style>"+css+"</style><script>"+script+"</script></head><body>"
             + "<div class=\"header\">This is a HTML representation of a resource. "
             + "<a href=\"javascript:addUrlParameter('_format', 'json');\">Raw json</a></div>"
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
