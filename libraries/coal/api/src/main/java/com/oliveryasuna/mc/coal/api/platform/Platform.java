package com.oliveryasuna.mc.coal.api.platform;

import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executor;

public interface Platform {

    //==================================================
    // Methods
    //==================================================

    Path configDir();

    Executor mainThreadExecutor();

    Logger logger(String name);

    Environment environment();

    Optional<Path> gameDir();

    Optional<String> loaderName();

    Optional<String> loaderVersion();

}
