package org.apache.dubbo.testing.demo;


import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import site.zfei.demo.Client;

import java.util.concurrent.TimeUnit;

//@BenchmarkMode({Mode.Throughput, Mode.AverageTime, Mode.SampleTime})
@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class Stater {

    @Param({"currentCommitId"})
    private String commitId;

    @Benchmark
    public void test() {
        new Client().addByMap();
    }

    public static void main(String[] args) throws Exception {
        System.out.println(args);

        Options opt;
        ChainedOptionsBuilder optBuilder = new OptionsBuilder()
                .include(Stater.class.getSimpleName())
                .warmupIterations(1)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(1)
                .measurementTime(TimeValue.seconds(1))
                .threads(2)
                .forks(1);

        opt = doOptions(optBuilder).build();

        new Runner(opt).run();

    }

    private static ChainedOptionsBuilder doOptions(ChainedOptionsBuilder optBuilder) {
        String userDir = System.getProperty("user.dir");
        optBuilder.result(userDir + "/data/output.json");
        optBuilder.resultFormat(ResultFormatType.JSON);
        return optBuilder;
    }
}
