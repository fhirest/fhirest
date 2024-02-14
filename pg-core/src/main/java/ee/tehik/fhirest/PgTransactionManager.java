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
package ee.tehik.fhirest;

import ee.tehik.fhirest.tx.TransactionManager;
import ee.tehik.fhirest.tx.TransactionRef;
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
