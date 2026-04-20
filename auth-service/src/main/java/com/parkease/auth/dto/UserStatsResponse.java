package com.parkease.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {

    private long totalUsers;
    private long totalDrivers;
    private long totalManagers;
    private long totalAdmins;
    private long activeUsers;
    private long inactiveUsers;
}
