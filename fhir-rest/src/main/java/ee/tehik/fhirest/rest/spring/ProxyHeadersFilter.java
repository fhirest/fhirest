package ee.tehik.fhirest.rest.spring;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Component
public class ProxyHeadersFilter extends ForwardedHeaderFilter {
}

