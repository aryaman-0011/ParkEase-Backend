package com.parkease.gateway.config;

/**
 * Route reference for ParkEase microservices.
 *
 * The actual route configuration lives in application.yaml so it can be changed
 * without recompiling. This class keeps the intended gateway contract visible
 * in the codebase for future maintenance.
 */
public final class GatewayRoutesInfo {

    private GatewayRoutesInfo() {
    }

    public static final String AUTH = "/auth/**, /oauth2/**, /login/oauth2/**";
    public static final String VEHICLES = "/vehicles/**";
    public static final String LOTS = "/lots/**";
    public static final String SPOTS = "/spots/**";
    public static final String BOOKINGS = "/bookings/**";
    public static final String PAYMENTS = "/payments/**";
    public static final String NOTIFICATIONS = "/notifications/**";
    public static final String ANALYTICS = "/analytics/**";
}
