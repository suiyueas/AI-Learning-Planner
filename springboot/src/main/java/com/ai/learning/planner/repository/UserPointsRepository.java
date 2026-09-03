package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.UserPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户积分总表 Repository
 */
@Repository
public interface UserPointsRepository extends JpaRepository<UserPoints, Long> {

    Optional<UserPoints> findByUserId(Long userId);

    @Query("SELECT COALESCE(p.availablePoints, 0) FROM UserPoints p WHERE p.userId = :userId")
    Long getAvailablePoints(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserPoints p SET p.availablePoints = p.availablePoints + :points, " +
           "p.totalEarned = p.totalEarned + :earned WHERE p.userId = :userId")
    int addPoints(@Param("userId") Long userId, @Param("points") Long points, @Param("earned") Long earned);

    @Modifying
    @Query("UPDATE UserPoints p SET p.availablePoints = p.availablePoints - :points WHERE p.userId = :userId AND p.availablePoints >= :points")
    int deductPoints(@Param("userId") Long userId, @Param("points") Long points);
}
