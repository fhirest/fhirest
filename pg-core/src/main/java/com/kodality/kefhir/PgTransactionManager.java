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
 package com.kodality.kefhir;

import com.kodality.kefhir.tx.TransactionManager;
import com.kodality.kefhir.tx.TransactionRef;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

public class PgTransactionManager extends TransactionAspectSupport implements TransactionManager {
  protected PlatformTransactionManager tm;

  public PgTransactionManager(DataSource ds) {
    this.tm = new DataSourceTransactionManager(ds);
  }

  @Override
  public TransactionRef requireTransaction() {
    DefaultTransactionAttribute txAttr = new DefaultTransactionAttribute();
    TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, null);
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
