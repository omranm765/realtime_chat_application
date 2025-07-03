package de.htwsaar.messagebroker.repository;

import de.htwsaar.messagebroker.entity.EventTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventTypeRepository extends JpaRepository<EventTypeEntity, Long> {
    Optional<EventTypeEntity> findByName(String name);
}
