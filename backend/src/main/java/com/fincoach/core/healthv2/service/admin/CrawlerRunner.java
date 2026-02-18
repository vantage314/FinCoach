package com.fincoach.core.healthv2.service.admin;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public interface CrawlerRunner {
    CrawlerRunResult run(CrawlerRunRequest request, Consumer<String> lineConsumer, AtomicBoolean stopSignal);
}
