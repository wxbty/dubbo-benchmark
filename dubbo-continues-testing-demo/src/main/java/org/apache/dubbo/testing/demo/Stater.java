package org.apache.dubbo.testing.demo;


import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import site.zfei.demo.Client;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class Stater {

    @Param({"param1", "param2"})
    private String commitId;

    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.AverageTime, Mode.SampleTime})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void test() {
        new Client().addByMap();
        System.out.println(commitId);
    }

    public static void main(String[] args) {
        System.out.println(11111);
    }
}
