package com.kodality.kefhir.openapi;

//@Singleton
//@RequiredArgsConstructor
//public class HapiOpenapiComposer {
//  private final HapiContextHolder hapiContextHolder;
//
//  public String generateOpenApi() {
//    try {
//      ByteArrayOutputStream os = new ByteArrayOutputStream();
//      Writer w = new Writer(os);
//      new OpenApiGenerator(hapiContextHolder.getHapiContext(), ConformanceHolder.getCapabilityStatement(), w).generate("xx", "aa");
//      w.commit();
//      return os.toString(StandardCharsets.UTF_8);
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//  }
//}
