package com.parkease.gateway.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GatewayRoutesInfoTest {
    @Test void authRoute() { assertEquals("/auth/**, /oauth2/**, /login/oauth2/**", GatewayRoutesInfo.AUTH); }
    @Test void vehiclesRoute() { assertEquals("/vehicles/**", GatewayRoutesInfo.VEHICLES); }
    @Test void lotsRoute() { assertEquals("/lots/**", GatewayRoutesInfo.LOTS); }
    @Test void spotsRoute() { assertEquals("/spots/**", GatewayRoutesInfo.SPOTS); }
    @Test void bookingsRoute() { assertEquals("/bookings/**", GatewayRoutesInfo.BOOKINGS); }
    @Test void paymentsRoute() { assertEquals("/payments/**", GatewayRoutesInfo.PAYMENTS); }
    @Test void notificationsRoute() { assertEquals("/notifications/**", GatewayRoutesInfo.NOTIFICATIONS); }
    @Test void analyticsRoute() { assertEquals("/analytics/**", GatewayRoutesInfo.ANALYTICS); }
}
