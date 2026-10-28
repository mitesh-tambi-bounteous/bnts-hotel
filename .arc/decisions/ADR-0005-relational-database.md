---
id: ADR-0005
category: component
title: Relational Database
status: accepted
superseded_by: null
component_id: comp_relational_database
responsibility: System of record for hotels, rooms, inventory, bookings and guests;
  product not yet chosen.
kind: managed_service
split_reason: Backing service the system needs but does not build; transactional booking
  data.
evidence: Inferred from hotel booking domain; product open
---

## Context

Inferred from hotel booking domain; product open

## Decision

System of record for hotels, rooms, inventory, bookings and guests; product not yet chosen.

Kept separate because Backing service the system needs but does not build; transactional booking data..
