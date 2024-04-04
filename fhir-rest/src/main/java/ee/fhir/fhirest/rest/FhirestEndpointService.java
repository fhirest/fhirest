package ee.fhir.fhirest.rest;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import ee.fhir.fhirest.rest.filter.FhirestRequestExecutionInterceptor;
import ee.fhir.fhirest.rest.interaction.FhirInteraction;
import ee.fhir.fhirest.rest.interaction.InteractionUtil;
import ee.fhir.fhirest.rest.model.FhirestRequest;
import ee.fhir.fhirest.rest.model.FhirestResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FhirestEndpointService {
  private final List<FhirestRequestExecutionInterceptor> interceptors;
  private final Map<String, List<FhirestEnabledOperation>> operations = new HashMap<>();

  public Map<String, List<FhirestEnabledOperation>> getEnabledOperations() {
    return operations;
  }

  public FhirestEnabledOperation findOperation(FhirestRequest request) {
    List<FhirestEnabledOperation> typeOperations = operations.get(request.getType());
    FhirestEnabledOperation op = typeOperations == null ? null : typeOperations.stream().filter(i -> matches(i, request)).sorted(
        Comparator.<FhirestEnabledOperation>comparingInt(o -> StringUtils.countMatches(o.getPath(), "{}")).thenComparingInt(o -> -o.getPath().length())
        // quick solution. need to think of a way to choose more precise match when multiple operations match.
        // example: Patient/_history vs Patient/123, operations vs compartments
    ).findFirst().orElse(null);
    if (op == null) {
      throw new FhirException(FhirestIssue.FEST_020, "interaction",
          request.getMethod() + " " + StringUtils.defaultString(request.getType()) + "/" + request.getPath());
    }
    return op;
  }

  public FhirestResponse execute(FhirestRequest request) {
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
    List<FhirestEnabledOperation> ops = methods.stream().map(m -> new FhirestEnabledOperation(m, service)).toList();
    this.operations.computeIfAbsent(type, x -> new ArrayList<>()).addAll(ops);
  }

  private FhirestResponse invoke(FhirestEnabledOperation op, FhirestRequest req) {
    try {
      return (FhirestResponse) op.getMethod().invoke(op.getService(), req);
    } catch (IllegalAccessException | InvocationTargetException ex) {
      Throwable e = ex.getCause() == null ? ex : ex.getCause();
      throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  private boolean matches(FhirestEnabledOperation op, FhirestRequest req) {
    return req.getMethod().equals(op.getVerb()) && op.getPathMatcher().matcher(req.getPath()).matches();
  }

  @Getter
  public static class FhirestEnabledOperation {
    private final String verb;
    private final String path;
    private final Pattern pathMatcher;
    private final Method method;
    private final Object service;
    private final String interaction;

    public FhirestEnabledOperation(Method m, Object service) {
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
