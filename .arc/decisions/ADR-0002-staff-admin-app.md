---
id: ADR-0002
category: component
title: Staff Admin App
status: accepted
superseded_by: null
component_id: comp_staff_admin_app
responsibility: Staff/admin web UI for hotel staff to manage properties, rooms, inventory,
  rates and bookings.
kind: web_ui
split_reason: Different client and audience (staff vs guests) with its own access
  rules; named by the human.
talks_to:
- component_id: comp_booking_api
  interaction: sync
  evidence: 'Human: staff/admin app plus APIs'
evidence: 'Human: ''a staff/admin app'''
---

## Context

Human: 'a staff/admin app'

## Decision

Staff/admin web UI for hotel staff to manage properties, rooms, inventory, rates and bookings.

Kept separate because Different client and audience (staff vs guests) with its own access rules; named by the human..

## Talks to

- **Booking API** (sync) — Human: staff/admin app plus APIs
