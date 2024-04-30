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

package ee.fhir.fhirest.core.exception;

import lombok.Getter;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

import static org.hl7.fhir.r5.model.OperationOutcome.IssueType.INVALID;
import static org.hl7.fhir.r5.model.OperationOutcome.IssueType.MULTIPLEMATCHES;
import static org.hl7.fhir.r5.model.OperationOutcome.IssueType.NOTFOUND;
import static org.hl7.fhir.r5.model.OperationOutcome.IssueType.NOTSUPPORTED;
import static org.hl7.fhir.r5.model.OperationOutcome.IssueType.STRUCTURE;

@Getter
public enum FhirestIssue {
  FEST_001(400, NOTSUPPORTED, "not supported: {{desc}}"),
  FEST_002(400, MULTIPLEMATCHES, "was expecting unique resource by {{uri}}; found {{total}}"),
  FEST_003(406, NOTSUPPORTED, "invalid Accept"),
  FEST_004(406, NOTSUPPORTED, "invalid Content-Type"),
  FEST_005(406, NOTSUPPORTED, "unsupported _format value"),
  FEST_006(400, NOTSUPPORTED, "create on update is disabled by conformance"),
  FEST_007(400, INVALID, "invalid conditional update request"),
  FEST_008(404, NOTFOUND, "{{resource}} not found"),
  FEST_009(400, INVALID, "unknown compartment {{compartment}} for {{resource}}"),
  FEST_010(400, INVALID, "operation must start with $"),
  FEST_011(400, INVALID, "Performing an state affecting operation using GET not allowed"),
  FEST_012(400, INVALID, "Operation body required"),
  FEST_013(400, INVALID, "Operation MAY accept Resource in body only if operation definition has exactly one input parameter whose type is a FHIR Resource"),
  FEST_014(400, NOTSUPPORTED, "Operation {{operation}} not defined in capability statement"),
  FEST_015(400, NOTSUPPORTED, "Operation {{operation}} not defined in operation definition"),
  FEST_016(400, INVALID, "invalid query parameter: '{{param}}' in '?{{query}}'"),
  FEST_017(400, INVALID, "Parameter {{key}}:{{modifier}} not defined"),
  FEST_018(400, NOTSUPPORTED, "search parameter '{{param}}' not supported by conformance"),
  FEST_019(400, NOTSUPPORTED, "modifier '{{param}}:{{modifier}}' not supported by conformance"),
  FEST_020(406, NOTSUPPORTED, "could not find matching enabled interaction for: {{interaction}}"),
  FEST_021(400, STRUCTURE, "error during resource parse: {{message}}"),
  FEST_022(400, STRUCTURE, "was expecting {{expected}} but found {{actual}}"),
  FEST_023(400, INVALID, "profile invalid: {{message}}"),
  FEST_024(400, NOTSUPPORTED, "Could not find SearchParameter with code '{{param}}' for resource '{{resource}}'"),
  FEST_025(400, INVALID, "Bundle.request element required"),
  FEST_026(400, INVALID, "Only batch or transaction supported"),
  FEST_027(400, INVALID, "invalid uri: {{uri}}"),
  FEST_028(400, NOTFOUND, "{{ref}} not found"),
  FEST_029(400, INVALID, "_include parameter invalid. ResourceType:SearchParameter[:targetResourceType]"),
  FEST_030(400, INVALID, "invalid composite parameter"),
  FEST_031(400, NOTSUPPORTED, "order by composite param not implemented"),
  FEST_032(400, INVALID, "invalid or unsupported date format: {{date}}"),
  FEST_033(400, INVALID, "invalid Quantity value: {{value}}"),
  FEST_034(400, INVALID, "invalid reference param: {{value}}"),
  FEST_035(400, INVALID, ":not modifier not allowed in token param"),
  FEST_036(400, INVALID, "Invalid reference: {{ref}}"),
  ;

  private final int httpCode;
  private final IssueType type;
  private final String description;

  FhirestIssue(int httpCode, IssueType type, String description) {
    this.httpCode = httpCode;
    this.type = type;
    this.description = description;
  }
}
