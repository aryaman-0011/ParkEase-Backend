package com.parkease.gateway.config;

// Route reference for ParkEase microservices.
// The actual route configuration lives in application.yaml so it can be changed
// without recompiling. This class keeps the intended gateway contract visible
// in the codebase for future maintenance and documentation.
public final class GatewayRoutesInfo {

    private GatewayRoutesInfo() {
    }

    // Auth endpoints (login, register, OAuth2)
    public static final String AUTH = "/auth/**, /oauth2/**, /login/oauth2/**";
    // Vehicle management endpoints
    public static final String VEHICLES = "/vehicles/**";
    // Parking lot CRUD endpoints
    public static final String LOTS = "/lots/**";
    // Parking spot management endpoints
    public static final String SPOTS = "/spots/**";
    // Booking (time-slot reservations) endpoints
    public static final String BOOKINGS = "/bookings/**";
    // Payment (Razorpay, cash) endpoints
    public static final String PAYMENTS = "/payments/**";
    // Notification endpoints (email/SMS alerts)
    public static final String NOTIFICATIONS = "/notifications/**";
    // Analytics and reporting endpoints
    public static final String ANALYTICS = "/analytics/**";
}
