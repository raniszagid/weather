package ru.spbpu.weather.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.spbpu.weather.model.RequestHistoryEntity;
import ru.spbpu.weather.model.User;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<RequestHistoryEntity, Integer> {
    public List<RequestHistoryEntity> findRequestHistoryEntitiesByUser(User user);
}