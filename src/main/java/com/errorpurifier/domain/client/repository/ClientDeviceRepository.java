package com.errorpurifier.domain.client.repository;

import com.errorpurifier.domain.client.entity.ClientDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ClientDeviceRepository extends JpaRepository<ClientDevice, UUID> {
}