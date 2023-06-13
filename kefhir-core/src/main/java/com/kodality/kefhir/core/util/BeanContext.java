package com.kodality.kefhir.core.util;

import io.micronaut.context.annotation.Context;
import java.util.Collection;

@Context
public class BeanContext {
  private static io.micronaut.context.BeanContext ctx;

  public BeanContext(io.micronaut.context.BeanContext ctx) {
    BeanContext.ctx = ctx;
  }

  public static <T> T getBean(Class<T> clazz) {
    return ctx.getBean(clazz);
  }

  public static <T> Collection<T> getBeans(Class<T> clazz) {
    return ctx.getBeansOfType(clazz);
  }
}
