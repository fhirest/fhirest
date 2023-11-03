package com.kodality.kefhir.rest;

import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.rest.filter.KefhirRequestExecutionInterceptor;
import com.kodality.kefhir.rest.interaction.FhirInteraction;
import com.kodality.kefhir.rest.interaction.InteractionUtil;
import com.kodality.kefhir.rest.model.KefhirRequest;
import com.kodality.kefhir.rest.model.KefhirResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import jakarta.inject.Named;;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

import static java.util.stream.Collectors.toList;

@Named("default")
@Slf4j
@Singleton
@RequiredArgsConstructor
public class KefhirEndpointService {
  private final List<KefhirRequestExecutionInterceptor> interceptors;
  private final Map<String, List<KefhirEnabledOperation>> operations = new HashMap<>();

  public Map<String, List<KefhirEnabledOperation>> getEnabledOperations() {
    return operations;
  }

  public KefhirEnabledOperation findOperation(KefhirRequest request) {
    List<KefhirEnabledOperation> typeOperations = operations.get(request.getType());
    KefhirEnabledOperation op = typeOperations == null ? null : typeOperations.stream().filter(i -> matches(i, request)).sorted(
        Comparator.<KefhirEnabledOperation>comparingInt(o -> StringUtils.countMatches(o.getPath(), "{}")).thenComparingInt(o -> -o.getPath().length())
        // quick solution. need to think of a way to choose more precise match when multiple operations match.
        // example: Patient/_history vs Patient/123, operations vs compartments
    ).findFirst().orElse(null);
    if (op == null) {
      throw new FhirException(406, IssueType.NOTSUPPORTED,
          "could not find matching enabled interaction for: " + request.getMethod() + " " + StringUtils.defaultString(request.getType()) + "/" + request.getPath());
    }
    return op;
  }

  public KefhirResponse execute(KefhirRequest request) {
    interceptors.forEach(i -> i.beforeExecute(request));
    return invoke(request.getOperation(), request);
  }


  public void startRoot(FhirRootServer service) {
    List<Method> methods = InteractionUtil.findAllMethods(FhirRootServer.class);
    addOperation(null, methods, service);
  }

  public void start(String type, String interaction, FhirResourceServer service) {
    List<Method> methods = InteractionUtil.findMethods(interaction, FhirResourceServer.class);
    addOperation(type, methods, service);
  }

  private void addOperation(String type, List<Method> methods, Object service) {
    List<KefhirEnabledOperation> ops = methods.stream().map(m -> new KefhirEnabledOperation(m, service)).collect(toList());
    this.operations.computeIfAbsent(type, x -> new ArrayList<>()).addAll(ops);
  }

  private KefhirResponse invoke(KefhirEnabledOperation op, KefhirRequest req) {
    try {
      return (KefhirResponse) op.getMethod().invoke(op.getService(), req);
    } catch (IllegalAccessException | InvocationTargetException ex) {
      Throwable e = ex.getCause() == null ? ex : ex.getCause();
      throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  private boolean matches(KefhirEnabledOperation op, KefhirRequest req) {
    return req.getMethod().equals(op.getVerb()) && op.getPathMatcher().matcher(req.getPath()).matches();
  }

  @Getter
  public static class KefhirEnabledOperation {
    private final String verb;
    private final String path;
    private final Pattern pathMatcher;
    private final Method method;
    private final Object service;
    private final String interaction;

    public KefhirEnabledOperation(Method m, Object service) {
      FhirInteraction bi = m.getAnnotation(FhirInteraction.class);
      this.method = m;
      this.service = service;
      this.interaction = bi.interaction();
      String[] p = bi.mapping().split(" ");
      this.verb = p[0];
      this.path = StringUtils.removeStart(p[1], "/");
      this.pathMatcher = Pattern.compile(path.replaceAll("\\$", "\\\\\\$").replaceAll("\\{\\}", "[^/]+"));
    }
  }
}
