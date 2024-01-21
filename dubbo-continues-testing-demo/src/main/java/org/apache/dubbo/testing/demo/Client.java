package org.apache.dubbo.testing.demo;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.dubbo.benchmark.ClientGrpc;
import org.apache.dubbo.benchmark.ClientPb;
import org.apache.dubbo.benchmark.bean.Page;
import org.apache.dubbo.benchmark.bean.User;
import org.apache.dubbo.benchmark.rpc.AbstractClient;
import org.apache.dubbo.benchmark.service.UserService;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class Client extends AbstractClient {
    private static final int CONCURRENCY = 32;

    private final ClassPathXmlApplicationContext context;
    private final UserService userService;

    @Param({"currentCommitId"})
    private String commitId;

    public Client() {
        context = new ClassPathXmlApplicationContext("consumer.xml");
        context.start();
        userService = (UserService) context.getBean("userService");
    }

    @Override
    protected UserService getUserService() {
        return userService;
    }

    @TearDown
    public void close() {
        context.close();
    }

    @Benchmark
    @Override
    public boolean existUser() throws Exception {
        return super.existUser();
    }

    @Benchmark
    @Override
    public boolean createUser() throws Exception {
        return super.createUser();
    }

    @Benchmark
    @Override
    public User getUser() throws Exception {
        return super.getUser();
    }

    @Benchmark
    @Override
    public Page<User> listUser() throws Exception {
        return super.listUser();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("begin dubbo client");
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();

        options.addOption(Option.builder().longOpt("warmupIterations").hasArg().build());
        options.addOption(Option.builder().longOpt("warmupTime").hasArg().build());
        options.addOption(Option.builder().longOpt("measurementIterations").hasArg().build());
        options.addOption(Option.builder().longOpt("measurementTime").hasArg().build());

        CommandLineParser parser = new DefaultParser();

        CommandLine line = parser.parse(options, args);

        int warmupIterations = Integer.valueOf(line.getOptionValue("warmupIterations", "3"));
        int warmupTime = Integer.valueOf(line.getOptionValue("warmupTime", "10"));
        int measurementIterations = Integer.valueOf(line.getOptionValue("measurementIterations", "3"));
        int measurementTime = Integer.valueOf(line.getOptionValue("measurementTime", "10"));

        Options opt;
        ChainedOptionsBuilder optBuilder = new OptionsBuilder()
                .include(Client.class.getSimpleName())
                .exclude(ClientPb.class.getSimpleName())
                .exclude(ClientGrpc.class.getSimpleName())
                .warmupIterations(warmupIterations)
                .warmupTime(TimeValue.seconds(warmupTime))
                .measurementIterations(measurementIterations)
                .measurementTime(TimeValue.seconds(measurementTime))
                .threads(CONCURRENCY)
                .forks(1);

        opt = doOptions(optBuilder).build();

        System.out.println("before run");
        new Runner(opt).run();
        System.out.println("after run");

    }

    private static ChainedOptionsBuilder doOptions(ChainedOptionsBuilder optBuilder) {
        String userDir = System.getProperty("user.dir");
        optBuilder.result(userDir + "/data/output.json");
        System.out.println("result dir: " + userDir + "/data/output.json");
        optBuilder.resultFormat(ResultFormatType.JSON);
        return optBuilder;
    }
}
