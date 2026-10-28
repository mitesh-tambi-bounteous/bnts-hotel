---
id: ADR-0007
category: nfr
title: "NFR: Scale: concurrent users (Booking API) \u2014 1,000 concurrent users"
status: accepted
superseded_by: null
dimension: 'Scale: concurrent users'
metric: Concurrent users sustained at peak with p95 API latency under 500 ms
threshold: 1,000 concurrent users
component_id: comp_booking_api
needs_dedicated_tracking: true
evidence: 'Human: ''Concurrent Users - Needs to be taken care'' gave no number; threshold
  proposed by the agent from 1k bookings/day and 100 hotels, to be confirmed'
contested: true
alternatives_considered:
- 'Fewer than 500 concurrent users: ignores sale or festival-season spikes'
- 'Fixed large number such as 10,000: overbuilt for 1k bookings per day'
consequences:
- API containers must autoscale horizontally
- Load test at this level before launch
---

## Context

Human: 'Concurrent Users - Needs to be taken care' gave no number; threshold proposed by the agent from 1k bookings/day and 100 hotels, to be confirmed

## Decision

Concurrent users sustained at peak with p95 API latency under 500 ms: 1,000 concurrent users

Applies to Booking API.

## Alternatives considered

- Fewer than 500 concurrent users: ignores sale or festival-season spikes
- Fixed large number such as 10,000: overbuilt for 1k bookings per day

## Consequences

- API containers must autoscale horizontally
- Load test at this level before launch
