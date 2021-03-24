package com.company;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

class ThreadPointsGenerator extends Thread {
    public ThreadPointsGenerator(String name, long numberOfPoints) {
        super(name);
        this.numberOfPoints = numberOfPoints;
    }

    public boolean generatePoint() {
        double x = ThreadLocalRandom.current().nextDouble(1);
        double y = ThreadLocalRandom.current().nextDouble(1);
        return Math.sqrt(x * x + y * y) <= 1;
    }

    public long generateNPoint(long N) {
        long localCount = 0;
        for (long i = 0; i < N; i++) {
            localCount += generatePoint() ? 1 : 0;
        }
        return localCount;
    }

    private long pointsInCircle = 0;
    private final long numberOfPoints;
    private boolean isThreadCompleted = false;

    @Override
    public void run() {
        pointsInCircle = generateNPoint(numberOfPoints);
        isThreadCompleted = true;
    }

    public void checkPoint(AtomicLong n) {
        if (isThreadCompleted) {
            n.addAndGet(pointsInCircle);
        }
    }
}

class PiResult {
    double Pi;
    long In, Out;

    PiResult(double pi, long in, long out) {
        Pi = pi;
        In = in;
        Out = out;
    }
}

public class Main {

    static void println(Object line) {
        System.out.println(line);
    }

    private static final Random rand = new Random();

    private static void ShowResult(PiResult piResult, long timeElapsed, String name) {
        println("\n" + name);
        println("timeElapsed : " + timeElapsed / 1000000000.0);
        println("pi_approx = " + piResult.Pi);
        println("In = " + piResult.In);
        println("Out = " + piResult.Out);
    }

    private static PiResult standardCalculatePi(long N) {
        double pi_approx;
        int in = 0;
        int out = 0;

        for (int i = 1; i <= N; i++) {
            Double x = rand.nextDouble();
            Double y = rand.nextDouble();

            if ((x * y) + (y * y) <= 1) {
                in++;
            } else {
                out++;
            }
        }
        pi_approx = 4.0 * in / N;
        return new PiResult(pi_approx, in, out);
    }

    private static void RunStandardCalculatePiWithLogging(long N) {
        long startTime = System.nanoTime();
        var standardResult = standardCalculatePi(N);
        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;
        ShowResult(standardResult, timeElapsed, "RunStandardCalculatePiWithLogging");
    }

    private static PiResult ThreadCalculatePi(long N, int threadsCount) {

        AtomicLong pointsInCircle = new AtomicLong(0);
        ArrayList<ThreadPointsGenerator> threadPool = new ArrayList<>();
        long batch = N / threadsCount;

        for (int i = 0; i < (threadsCount - 1); i++) {
            threadPool.add(new ThreadPointsGenerator("point generator #" + i, batch));

            threadPool.get(i).start();
        }
        threadPool.add(new ThreadPointsGenerator("point generator #" + (threadsCount - 1), batch + N % batch));

        threadPool.get(threadsCount - 1).start();
        for (int i = 0; i < threadsCount; i++) {
            try {
                threadPool.get(i).join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            threadPool.get(i).checkPoint(pointsInCircle);
        }
        double piEstimation = pointsInCircle.get() * 4.0 / N;

        return new PiResult(piEstimation, pointsInCircle.intValue(), (N - pointsInCircle.longValue()));
    }

    private static void RunThreadCalculatePiWithLogging(long N, int threadsCount) {
        long startTime = System.nanoTime();
        var threadResult = ThreadCalculatePi(N, threadsCount);
        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;
        ShowResult(threadResult, timeElapsed, "RunThreadCalculatePiWithLogging");
    }

    public static void main(String[] args) {
        long N = args.length > 0 ? Long.parseLong(args[0]) : 10000;
        int threadsCount = args.length > 1 ? Integer.parseInt(args[1]) : 1;

        println("N iterations = " + N);
        println("threadsCount = " + threadsCount);

        RunStandardCalculatePiWithLogging(N);
        RunThreadCalculatePiWithLogging(N, threadsCount);
    }
}
