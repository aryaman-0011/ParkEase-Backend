package com.parkease.vehicle_service.dto;

import lombok.Data;

@Data
public class UpdateVehicleRequest {
    private String make;
    private String model;
    private String color;
    private String vehicleType;
    private Boolean isEV;
    private Boolean isActive;
}
