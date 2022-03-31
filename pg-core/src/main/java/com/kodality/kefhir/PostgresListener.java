package com.kodality.kefhir;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariProxyConnection;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.BeanInitializedEventListener;
import io.micronaut.context.event.BeanInitializingEvent;
import io.micronaut.scheduling.annotation.Scheduled;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

@Requires(property = "datasources.default")
@Singleton
public class PostgresListener implements BeanInitializedEventListener<Object> {
  private HikariDataSource ds;
  private final Map<String, List<Consumer<PostgresListenerEvent>>> listeners = new HashMap<>();

  public PostgresListener(Environment env) {
    HikariConfig conf = new HikariConfig();
    conf.setDriverClassName("org.postgresql.Driver");
    conf.setJdbcUrl(env.getProperty("datasources.default.url", String.class).orElseThrow());
    conf.setUsername(env.getProperty("datasources.default.username", String.class).orElseThrow());
    conf.setPassword(env.getProperty("datasources.default.password", String.class).orElseThrow());
    conf.setMaximumPoolSize(1);
    conf.setMinimumIdle(1);
    conf.setMaxLifetime(0);
    conf.setReadOnly(true);
    conf.setConnectionInitSql("LISTEN object_notify");
    this.ds = new HikariDataSource(conf);
  }

  @Override
  public Object onInitialized(BeanInitializingEvent<Object> event) {
    Object bean = event.getBean();
    Stream.of(bean.getClass().getMethods()).filter(m -> m.isAnnotationPresent(PostgresChangeListener.class)).forEach(m -> {
      PostgresChangeListener a = m.getAnnotation(PostgresChangeListener.class);
      listeners.computeIfAbsent(a.table(), x -> new ArrayList<>()).add(p -> {
        try {
          m.invoke(bean);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    });
    return bean;
  }

  private PGConnection getConnection() {
    try {
      HikariProxyConnection hikariConn = (HikariProxyConnection) ds.getConnection();
      Connection conn = hikariConn.unwrap(Connection.class);
      hikariConn.close(); // return to pool
      return (PGConnection) conn;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // this does not actually poll db
  @Scheduled(fixedRate = "5s")
  public void poll() throws SQLException {
    PGNotification notifications[] = getConnection().getNotifications();
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

  @Target({ ElementType.METHOD })
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface PostgresChangeListener {
    String table();
  }
}
