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

package ee.fhir.fhirest.core.model;

public final class InteractionType {
  // http://hl7.org/fhir/restful-interaction
  public static final String READ = "read";
  public static final String VREAD = "vread";
  public static final String UPDATE = "update";
  public static final String DELETE = "delete";
  public static final String HISTORYINSTANCE = "history-instance";
  public static final String HISTORYTYPE = "history-type";
  public static final String HISTORYSYSTEM = "history-system";
  public static final String CREATE = "create";
  public static final String SEARCHTYPE = "search-type";
  public static final String SEARCHSYSTEM = "search-system";
  public static final String VALIDATE = "validate";
  public static final String CONFORMANCE = "conformance";
  public static final String TRANSACTION = "transaction";
  public static final String OPERATION = "operation";
}
