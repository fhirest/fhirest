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

package ee.fhir.fhirest;

import ee.fhir.fhirest.tx.TransactionManager;
import ee.fhir.fhirest.tx.TransactionRef;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

public class PgTransactionManager extends TransactionAspectSupport implements TransactionManager {
  protected PlatformTransactionManager tm;

  public PgTransactionManager(DataSource ds) {
    this.tm = new DataSourceTransactionManager(ds);
  }

  @Override
  public void afterPropertiesSet() {
  }

  @Override
  public TransactionRef requireNewTransaction() {
    DefaultTransactionAttribute txAttr = new DefaultTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, "requireNewTransaction");
    return pgTransactionRef(txInfo);
  }

  @Override
  public TransactionRef requireTransaction() {
    DefaultTransactionAttribute txAttr = new DefaultTransactionAttribute();
    TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, "requireTransaction");
    return pgTransactionRef(txInfo);
  }

  private TransactionRef pgTransactionRef(TransactionInfo txInfo) {
    return new TransactionRef() {

      @Override
      public void rollback(Throwable e) {
        completeTransactionAfterThrowing(txInfo, e);
      }

      @Override
      public void commit() {
        commitTransactionAfterReturning(txInfo);
      }

      @Override
      public void cleanupTransaction() {
        cleanupTransactionInfo(txInfo);
      }
    };
  }

}
