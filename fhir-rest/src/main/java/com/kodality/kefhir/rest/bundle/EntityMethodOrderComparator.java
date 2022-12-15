package com.kodality.kefhir.rest.bundle;

import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;

public class EntityMethodOrderComparator implements Comparator<BundleEntryComponent> {
  private static final List<HTTPVerb> order = List.of(HTTPVerb.DELETE, HTTPVerb.POST, HTTPVerb.PUT, HTTPVerb.GET);

  @Override
  public int compare(BundleEntryComponent o1, BundleEntryComponent o2) {
    return ObjectUtils.compare(order.indexOf(o1.getRequest().getMethod()),
        order.indexOf(o2.getRequest().getMethod()));
  }

}
