# Co-Seller Store Notification Member Count System

## Overview
The co-seller store notification system displays accurate member counts in all notifications. This document explains how member counts are fetched, stored, and displayed in real-time.

---

## 1. System Architecture

### Three-Layer Member Count System:

```
┌─────────────────────────────────────────────────────────────┐
│ Layer 1: Notification Creation                              │
│ (NotificationHelper.kt)                                      │
│ - Fetches accurate member count                              │
│ - Stores in notification.memberCount                         │
└────────────────┬───────────────────