package de.htwsaar.messagebroker.repository;

import de.htwsaar.messagebroker.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<ClientEntity, String> {
}
