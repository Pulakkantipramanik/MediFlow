package com.mediflow.order.entity;

// PURPOSE:
// Defines all valid states in the order lifecycle.
//
// WHY:
// Using an enum prevents invalid status values and makes
// the order workflow predictable and easier to maintain.
public enum OrderStatus {

    PENDING,
    APPROVED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    REJECTED,
    CANCELLED
}