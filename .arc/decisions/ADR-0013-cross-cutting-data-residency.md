---
id: ADR-0013
category: cross_cutting
title: 'Cross-cutting: Data residency'
status: accepted
superseded_by: null
component_id: null
decision: All personal and booking data is stored and processed only in AWS ap-south-1
  (Mumbai); no cross-region replication, and backups and logs stay in-region. Encrypt
  at rest (KMS) and in transit (TLS). Third-party providers (payments, SMS/email,
  identity) must process or store guest data in India, or receive only the minimum
  needed. Restrict cross-border transfer and keep an audit trail of access to guest
  data.
evidence: 'Human: ''Yes, consider best practices for data-residency''; Mumbai only
  per earlier answer'
---

## Context

Human: 'Yes, consider best practices for data-residency'; Mumbai only per earlier answer

## Decision

All personal and booking data is stored and processed only in AWS ap-south-1 (Mumbai); no cross-region replication, and backups and logs stay in-region. Encrypt at rest (KMS) and in transit (TLS). Third-party providers (payments, SMS/email, identity) must process or store guest data in India, or receive only the minimum needed. Restrict cross-border transfer and keep an audit trail of access to guest data.
