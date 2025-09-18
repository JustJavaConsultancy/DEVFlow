package com.justjava.devFlow.tasks;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FormRepository extends JpaRepository<Form, Long> {
    Optional<Form> findByFormCode(String formCode);
}