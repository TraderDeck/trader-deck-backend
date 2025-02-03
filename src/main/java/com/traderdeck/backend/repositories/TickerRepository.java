package com.traderdeck.backend.repositories;

import com.traderdeck.backend.models.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TickerRepository extends JpaRepository<Ticker, String> {}