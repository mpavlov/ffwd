package com.spotify.ffwd;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import com.spotify.ffwd.module.FastForwardModule;

@Slf4j
public class FastForwardAgent {
    public static void main(String argv[]) {
        final List<Class<? extends FastForwardModule>> modules = new ArrayList<>();

        modules.add(com.spotify.ffwd.kafka.KafkaModule.class);

        final AgentCore core = AgentCore.builder().modules(modules).build();

        try {
            core.run();
        } catch(Exception e) {
            log.error("Error in agent, exiting", e);
            System.exit(1);
            return;
        }

        System.exit(0);
    }
}
