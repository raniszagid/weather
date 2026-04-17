package ru.spbpu.weather.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.spbpu.weather.model.RequestHistoryEntity;
import ru.spbpu.weather.model.User;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<RequestHistoryEntity, Integer> {
    public List<RequestHistoryEntity> findRequestHistoryEntitiesByUser(User user);
    // Сортировка по времени запроса (по возрастанию/убыванию)
    List<RequestHistoryEntity> findByUser(User user, Sort sort);

    // Сортировка по названию города (A-Z / Z-A)
    @Query("SELECT r FROM RequestHistoryEntity r WHERE r.user = :user ORDER BY r.address ASC")
    List<RequestHistoryEntity> findByUserOrderByAddressAsc(@Param("user") User user);

    @Query("SELECT r FROM RequestHistoryEntity r WHERE r.user = :user ORDER BY r.address DESC")
    List<RequestHistoryEntity> findByUserOrderByAddressDesc(@Param("user") User user);

    // Сортировка по времени
    @Query("SELECT r FROM RequestHistoryEntity r WHERE r.user = :user ORDER BY r.requestTimestamp ASC")
    List<RequestHistoryEntity> findByUserOrderByTimestampAsc(@Param("user") User user);

    @Query("SELECT r FROM RequestHistoryEntity r WHERE r.user = :user ORDER BY r.requestTimestamp DESC")
    List<RequestHistoryEntity> findByUserOrderByTimestampDesc(@Param("user") User user);
}