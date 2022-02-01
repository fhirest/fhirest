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
package com.kodality.kefhir.tx;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;

import static java.util.stream.Collectors.toList;

@Singleton
@RequiredArgsConstructor
public class TransactionService {
  private final List<TransactionManager> txManagers;

  public <T> T transaction(Supplier<T> fn) {
    List<TransactionRef> txs = txManagers.stream().map(tx -> tx.requireTransaction()).collect(toList());
    Collections.reverse(txs); // close transactions in reverse order
    T returnme = null;
    try {
      returnme = fn.get();
    } catch (Throwable ex) {
      txs.forEach(tx -> tx.rollback(ex));
      throw ex;
    } finally {
      txs.forEach(tx -> tx.cleanupTransaction());
    }
    txs.forEach(tx -> tx.commit());
    return returnme;
  }

}
