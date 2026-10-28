---
id: ADR-0009
category: deployment_target
title: "Deployment target: AWS, ap-south-1 (Mumbai) only, single region, containers\
  \ spread across multiple\u2026"
status: accepted
superseded_by: null
component_id: null
decision: AWS, ap-south-1 (Mumbai) only, single region, containers spread across multiple
  Availability Zones (Multi-AZ), with automated backups; environments prod and staging.
  No cross-region replica or DR region. Orchestration service (ECS/EKS) still open.
evidence: 'Human: ''AWS (Mumbai region), containers'' and ''Mumbai only, with Multi-AZ
  and backups'''
contested: true
alternatives_considered:
- 'Mumbai primary with Hyderabad replica for DR: not chosen by the human'
- 'Active-passive across Mumbai and Hyderabad: not chosen by the human'
consequences:
- A full Mumbai region outage means downtime until restore from backups
- All data stays in India, supporting residency
- Simplest operations and lowest cost
---

## Context

Human: 'AWS (Mumbai region), containers' and 'Mumbai only, with Multi-AZ and backups'

## Decision

AWS, ap-south-1 (Mumbai) only, single region, containers spread across multiple Availability Zones (Multi-AZ), with automated backups; environments prod and staging. No cross-region replica or DR region. Orchestration service (ECS/EKS) still open.

## Alternatives considered

- Mumbai primary with Hyderabad replica for DR: not chosen by the human
- Active-passive across Mumbai and Hyderabad: not chosen by the human

## Consequences

- A full Mumbai region outage means downtime until restore from backups
- All data stays in India, supporting residency
- Simplest operations and lowest cost
