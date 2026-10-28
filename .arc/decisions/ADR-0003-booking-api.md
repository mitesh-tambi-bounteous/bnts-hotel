---
id: ADR-0003
category: component
title: Booking API
status: accepted
superseded_by: null
component_id: comp_booking_api
responsibility: 'Single API serving both apps: hotel/room catalogue, availability,
  bookings, guests and staff operations. Gateway/BFF shape still open.'
kind: api_service
split_reason: Shared API runtime for both clients; named by the human ('APIs').
talks_to:
- component_id: comp_relational_database
  interaction: sync
  evidence: API persists booking data
- component_id: comp_booking_worker
  interaction: async
  evidence: 'Human: background jobs; hand-off mechanism open'
evidence: 'Human: ''APIs'''
---

## Context

Human: 'APIs'

## Decision

Single API serving both apps: hotel/room catalogue, availability, bookings, guests and staff operations. Gateway/BFF shape still open.

Kept separate because Shared API runtime for both clients; named by the human ('APIs')..

## Talks to

- **Relational Database** (sync) — API persists booking data
- **Booking Worker** (async) — Human: background jobs; hand-off mechanism open
