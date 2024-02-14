package ee.tehik.fhirest.core.util;

import java.util.Collection;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class BeanContext {
  private static ApplicationContext ctx;

  public BeanContext(ApplicationContext ctx) {
    BeanContext.ctx = ctx;
  }

  public static <T> T getBean(Class<T> clazz) {
    return ctx.getBean(clazz);
  }

  public static <T> Collection<T> getBeans(Class<T> clazz) {
    return ctx.getBeansOfType(clazz).values();
  }
}
