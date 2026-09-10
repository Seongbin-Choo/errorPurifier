package com.errorpurifier.benchmark;

import com.errorpurifier.domain.cache.service.LogPromptRefiner;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class LogPromptRefinerBenchmark {

    @Param({"1000", "10000", "100000", "1000000"})
    private int targetCharacters;

    private LogPromptRefiner refiner;
    private String rawLog;

    @Setup(Level.Trial)
    public void setUp() {
        refiner = LogRefinementBenchmarkSupport.newRefiner();
        rawLog = LogRefinementBenchmarkSupport.generateLog(targetCharacters);
    }

    @Benchmark
    public void refine(Blackhole blackhole) {
        blackhole.consume(refiner.refine(rawLog, null, LogRefinementBenchmarkSupport.PROJECT_TAGS));
    }
}
