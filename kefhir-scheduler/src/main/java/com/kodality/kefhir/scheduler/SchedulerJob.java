package com.kodality.kefhir.scheduler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SchedulerJob {
  private Long id;
  private String type;
  private String identifier;
}
