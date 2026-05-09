package com.parkease.analytics_service.repository;

import com.parkease.analytics_service.entity.OccupancyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<OccupancyLog, Long> {

    List<OccupancyLog> findByLotId(Long lotId);

    List<OccupancyLog> findByLotIdAndTimestampBetween(Long lotId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT AVG(o.occupancyRate) FROM OccupancyLog o WHERE o.lotId = :lotId")
    Double avgOccupancyByLotId(@Param("lotId") Long lotId);

    @Query("SELECT FUNCTION('HOUR', o.timestamp) AS hr, AVG(o.occupancyRate) " +
           "FROM OccupancyLog o WHERE o.lotId = :lotId " +
           "GROUP BY FUNCTION('HOUR', o.timestamp) ORDER BY hr")
    List<Object[]> findPeakHoursByLotId(@Param("lotId") Long lotId);

    List<OccupancyLog> findByVehicleType(String vehicleType);

    // Count today's logs for a specific lot
    @Query("SELECT COUNT(o) FROM OccupancyLog o WHERE o.lotId = :lotId " +
           "AND o.timestamp >= CURRENT_DATE")
    Long countTodayByLotId(@Param("lotId") Long lotId);

    @Query("SELECT DISTINCT o.lotId FROM OccupancyLog o")
    List<Long> findDistinctLotIds();
}
