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

package ee.fhir.fhirest;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariProxyConnection;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.sql.DataSource;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@ConditionalOnProperty("spring.datasource.default.url") //TODO: migranaut. multiple datasources?
@Component
@EnableScheduling
public class PostgresListener {
  private DataSource listenerDs;
  private final Map<String, List<Consumer<PostgresListenerEvent>>> listeners = new HashMap<>();

  public PostgresListener(Environment env) {
    HikariConfig conf = new HikariConfig();
    conf.setDriverClassName("org.postgresql.Driver");
    conf.setJdbcUrl(env.getProperty("spring.datasource.default.url", String.class));
    conf.setUsername(env.getProperty("spring.datasource.default.username", String.class));
    conf.setPassword(env.getProperty("spring.datasource.default.password", String.class));
    conf.setMaximumPoolSize(1);
    conf.setMinimumIdle(1);
    conf.setMaxLifetime(0);
    conf.setReadOnly(true);
    conf.setConnectionInitSql("LISTEN object_notify");
    this.listenerDs = new HikariDataSource(conf);
  }

  @EventListener(ContextRefreshedEvent.class)
  public void myListener(ContextRefreshedEvent event) {
    listeners.clear();
    ApplicationContext ac = event.getApplicationContext();
    Stream.of(ac.getBeanDefinitionNames()).forEach(bn -> {
      Object bean = ac.getBean(bn);
      Stream.of(bean.getClass().getMethods()).filter(m -> m.isAnnotationPresent(PostgresChangeListener.class)).forEach(m -> {
        PostgresChangeListener a = m.getAnnotation(PostgresChangeListener.class);
        listeners.computeIfAbsent(a.table(), x -> new ArrayList<>()).add(p -> invoke(bean, m));
      });
    });
  }

  private static void invoke(Object bean, Method m) {
    try {
      m.invoke(bean);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private PGConnection getConnection() {
    try {
      HikariProxyConnection hikariConn = (HikariProxyConnection) listenerDs.getConnection();
      Connection conn = hikariConn.unwrap(Connection.class);
      hikariConn.close(); // return to pool
      return (PGConnection) conn;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // this does not actually poll db
  @Scheduled(fixedRate = 5000)
  public void poll() throws SQLException {
    PGNotification[] notifications = getConnection().getNotifications();
    if (notifications == null) {
      return;
    }
    Stream.of(notifications).forEach(n -> {
      PostgresListenerEvent param = new PostgresListenerEvent(n.getParameter());
      String key = param.getSchema() + "." + param.getTable();
      if (listeners.containsKey(key)) {
        listeners.get(key).forEach(l -> l.accept(param));
      }
    });
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class PostgresListenerEvent {
    private String operation;
    private String ref;
    private String schema;
    private String table;

    protected PostgresListenerEvent(String formatted) {
      this.operation = StringUtils.substringAfter(formatted, "#");
      this.ref = StringUtils.substringBefore(formatted, "#");
      this.schema = StringUtils.substringBefore(this.ref, ".");
      this.table = StringUtils.substringAfter(this.ref, ".");
    }
  }

  @Target({ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface PostgresChangeListener {
    String table();
  }
}
