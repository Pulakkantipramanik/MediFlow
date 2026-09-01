package com.mediflow.payment.entity;

// PURPOSE:
// Payment-এর possible lifecycle/status define করার জন্য enum ব্যবহার করছি.
// WHY:
// String ব্যবহার করলে spelling mistake হতে পারে এবং invalid status save হওয়ার risk থাকে.
public enum PaymentStatus {

    PENDING,
    SUCCESS,
    FAILED
}