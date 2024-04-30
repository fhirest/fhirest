/*
 * MIT License
 *
 * Copyright (c) 2024 FhirEST community
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

package ee.fhir.fhirest.structure.util;

import java.util.List;
import java.util.Set;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Composition;
import org.hl7.fhir.r5.model.Reference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public final class ResourcePropertyUtilTest {

  @Test
  public void findReferences() {
    Composition c = new Composition();
    c.setSubject(List.of(new Reference().setReference("patient")));
    c.addEvent().addDetail().setReference(new Reference("event"));
    Bundle b = new Bundle();
    b.addEntry().setResource(c);
    List<Reference> refs = ResourcePropertyUtil.findProperties(b, Reference.class).collect(toList());
    Assertions.assertEquals(2, refs.size());
    Set<String> refStrings = refs.stream().map(ref -> ref.getReference()).collect(toSet());
    Assertions.assertTrue(refStrings.contains("event"));
    Assertions.assertTrue(refStrings.contains("patient"));
  }

}
