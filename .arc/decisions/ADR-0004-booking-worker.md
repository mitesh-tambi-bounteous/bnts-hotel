---
id: ADR-0004
category: component
title: Booking Worker
status: accepted
superseded_by: null
component_id: comp_booking_worker
responsibility: 'All background jobs: notifications, booking lifecycle tasks, reconciliation
  and reports.'
kind: worker
split_reason: Background work named by the human; one worker until a job has a differing
  NFR.
talks_to:
- component_id: comp_relational_database
  interaction: sync
  evidence: Worker reads and updates booking data
evidence: 'Human: ''background jobs'''
---

## Context

Human: 'background jobs'

## Decision

All background jobs: notifications, booking lifecycle tasks, reconciliation and reports.

Kept separate because Background work named by the human; one worker until a job has a differing NFR..

## Talks to

- **Relational Database** (sync) — Worker reads and updates booking data
