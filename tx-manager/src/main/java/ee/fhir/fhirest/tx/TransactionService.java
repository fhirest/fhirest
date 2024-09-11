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

package ee.fhir.fhirest.tx;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;

/**
 * <p>FHIRest manages transactions manually.</p>
 * <p>Since there may be several different databases depending on your configuration, transactions for all of them are started and finished simultaneously</p>
 * <p>Every database implementation should also implement and provide {@link TransactionManager} bean</p>
 *
 * @see TransactionManager
 */
@Component
@RequiredArgsConstructor
public class TransactionService {
  private final List<TransactionManager> txManagers;

  /**
   * Force make a new transaction
   * @param fn Function to be called inside this transaction
   * @return Result of provided function
   */
  public <T> T newTransaction(Supplier<T> fn) {
    return transaction(fn, tx -> tx.requireNewTransaction());
  }

  /**
   * Support current transaction if present or make a new one.
   * @param fn Function to be called inside this transaction
   * @return Result of provided function
   */
  public <T> T transaction(Supplier<T> fn) {
    return transaction(fn, tx -> tx.requireTransaction());
  }

  private <T> T transaction(Supplier<T> fn, Function<TransactionManager, TransactionRef> txFn) {
    List<TransactionRef> txs = txManagers.stream().map(txFn).collect(toList());
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
