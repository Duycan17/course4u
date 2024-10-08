package com.mgmtp.cfu.repository;

import com.mgmtp.cfu.entity.Registration;
import com.mgmtp.cfu.enums.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long>, JpaSpecificationExecutor<Registration> {
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.course.id = ?1 and (r.status in ?2)")
    int countRegistrationInCourse(Long courseId, List<RegistrationStatus> Statuses);

    boolean existsByIdAndUserId(Long registrationId, Long userId);

    @Query("SELECT r FROM Registration r " +
            "WHERE r.status = :status " +
            "AND (LOWER(r.user.username) LIKE CONCAT('%', :search ,'%')  " +
            "OR LOWER(r.user.fullName) LIKE CONCAT('%', :search ,'%') " +
            "OR LOWER(r.course.name) LIKE CONCAT('%', :search ,'%'))")
    Page<Registration> getOptionalRegistrationsWithStatus(@Param("status") RegistrationStatus status, @Param("search") String search, PageRequest pageRequest);

    @Query("SELECT r FROM Registration r " +
            "WHERE r.status != 'DRAFT' " +
            "AND (LOWER(r.user.username) LIKE CONCAT('%', :search ,'%')  " +
            "OR LOWER(r.user.fullName) LIKE CONCAT('%', :search ,'%') " +
            "OR LOWER(r.course.name) LIKE CONCAT('%', :search ,'%'))")
    Page<Registration> getOptionalRegistrationsWithoutStatus(@Param("search") String search, PageRequest pageRequest);
    List<Registration> findAllByStatus(RegistrationStatus registrationStatus);



    @Query("select r from Registration r " +
            "where r.user.id = :userId " +
            "order by " +
            "case when r.lastUpdated is null then 1 else 0 end, " +
            "r.lastUpdated desc, " +
            "r.id desc")
    List<Registration> getSortedRegistrations(@Param("userId") Long userId);
}
