package com.umc.pediupatrao.dashboard;

import java.time.Clock;
import org.springframework.context.annotation.*;
@Configuration
public class DashboardConfig {
    @Bean public Clock dashboardClock() { return Clock.systemUTC(); }
}
