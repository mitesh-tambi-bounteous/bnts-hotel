---
id: ADR-0001
category: component
title: Guest Web App
status: accepted
superseded_by: null
component_id: comp_guest_web_app
responsibility: Guest-facing web UI for searching hotels across Indian cities, booking
  rooms and managing reservations.
kind: web_ui
split_reason: 'Distinct client (guest web) named by the human: ''guest-facing web
  app''.'
talks_to:
- component_id: comp_booking_api
  interaction: sync
  evidence: 'Human: guest-facing web app plus APIs'
evidence: 'Human: ''I am expecting guest-facing web app, a staff/admin app, APIs and
  background jobs'''
---

## Context

Human: 'I am expecting guest-facing web app, a staff/admin app, APIs and background jobs'

## Decision

Guest-facing web UI for searching hotels across Indian cities, booking rooms and managing reservations.

Kept separate because Distinct client (guest web) named by the human: 'guest-facing web app'..

## Talks to

- **Booking API** (sync) — Human: guest-facing web app plus APIs
