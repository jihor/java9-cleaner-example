package ru.jihor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jihor (dmitriy_zhikharev@rgs.ru)
 * Created on 2018-01-26
 */
public class FinalizerAndCleanerExample {

    private static byte[] someData = {0x00, 0x10, 0x20, 0x30, 0x40, 0x50};

    private static FileInputStream getStreamWithFinalizer() throws IOException {
        final File tempFile = createTempFile("with_finalizer");

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            out.write(someData);
            out.flush();
            return new FileInputStream(tempFile) {

                // close() will be called by finalize() synchronously
                @Override
                public void close() throws IOException {
                    super.close();
                    cleanup(tempFile);
                }

                @Override
                protected void finalize() throws IOException {
//                    System.out.println("finalize() started");
                    long start = System.currentTimeMillis();
                    super.finalize();
//                    System.out.println("finalize() completed in " + (System.currentTimeMillis() - start) + " msec");
                }

            };
        }
    }

    public static final Cleaner cleaner = Cleaner.create();

    private static FileInputStream getStreamWithCleaner() throws IOException {
        final File tempFile = createTempFile("with_cleaner");

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            out.write(someData);
            return new ResourcefulFileInputStream(tempFile);
        }

    }

    private static List<Cleaner> cleaners = new ArrayList<>();

    private static int SIZE = 5;

    static {
        for (int i = 0; i < SIZE; i++) {
            cleaners.add(Cleaner.create());
        }
    }

    private static AtomicInteger counter = new AtomicInteger();

    private static class ResourcefulFileInputStream extends FileInputStream {

        public ResourcefulFileInputStream(File file) throws FileNotFoundException {
            super(file);
            cleaners.get(Math.abs(counter.getAndIncrement()) % SIZE).register(this, new CleanupTask(file));
        }

        @Override
        public void close() throws IOException {
            super.close();
        }

        // this task will be run asynchronously
        private static class CleanupTask implements Runnable {
            private final File tempFile;

            private CleanupTask(File tempFile) {this.tempFile = tempFile;}

            @Override
            public void run() {
                cleanup(tempFile);
            }
        }
    }

    private static File createTempFile(String suffix) throws IOException {
        return File.createTempFile("demo_", "_" + suffix);
    }

    private static void cleanup(final File file) {
        long start = System.currentTimeMillis();
        // assume cleanup is long running method
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        boolean deleted = file.delete();
        System.out.println("cleanup() completed in " + (System.currentTimeMillis() - start) + " msec on " + Thread.currentThread().getName() + " thread");
    }

    public static void main(String[] args) throws Exception {
        // these will be cleaned using the cleaners pool
        for (int i = 0; i < 10; i++) {
            System.out.println(Arrays.toString(getStreamWithCleaner().readAllBytes()));
        }

        for (int i = 0; i < 12; i++) {
            System.gc();
            TimeUnit.SECONDS.sleep(1);
        }


        // these will be cleaned synchronously using only one finalizer thread
        for (int i = 0; i < 10; i++) {
            System.out.println(Arrays.toString(getStreamWithFinalizer().readAllBytes()));
        }

        // and it will be a long story
        for (int i = 0; i < 100; i++) {
            System.gc();
            TimeUnit.SECONDS.sleep(1);
        }
    }
}
