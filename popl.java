import java.util.concurrent.*;

public class Main {

    private static final int NUM_COUNTERS = 3;
    private static final int SIMULATION_TIME_MS = 30000;

    private static final BlockingQueue<Integer> waitingTokens = new LinkedBlockingQueue<>();
    private static final ArrayBlockingQueue<Integer>[] counters = new ArrayBlockingQueue[NUM_COUNTERS];

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        for (int i = 0; i < NUM_COUNTERS; i++) {
            counters[i] = new ArrayBlockingQueue<>(1);  // Each counter handles 1 token at a time
        }

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_COUNTERS + 1);

        // Start Token Generator (separate from allocation)
        executorService.submit(new TokenGenerator());

        // Start each Counter Worker
        for (int i = 0; i < NUM_COUNTERS; i++) {
            executorService.submit(new CounterWorker(i));
        }

        // Let system run for 30 seconds
        try {
            Thread.sleep(SIMULATION_TIME_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executorService.shutdownNow();
    }

    // Generates tokens continuously (no dependency on counters being free)
    static class TokenGenerator implements Runnable {
        private int tokenNumber = 1;

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println("New Token " + tokenNumber + " generated");
                    waitingTokens.put(tokenNumber);  // Add to waiting queue
                    tokenNumber++;
                    Thread.sleep(1000);  // New token every second
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Each counter manages its own token processing
    static class CounterWorker implements Runnable {
        private final int counterIndex;

        public CounterWorker(int index) {
            this.counterIndex = index;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // Wait until counter is free and a new token is available
                    int token = waitingTokens.take();  // Get next token to be served

                    // Allocate the token to this counter
                    counters[counterIndex].put(token);  // Block until counter is ready (shouldn't block normally)
                    System.out.println("Token " + token + " allocated to Counter " + (counterIndex + 1));
                    System.out.println("Counter " + (counterIndex + 1) + " starts serving Token " + token);

                    // Simulate food preparation and delivery
                    Thread.sleep(3000);
                    System.out.println("Food delivered for Token " + token + " at Counter " + (counterIndex + 1));
                    System.out.println("Counter " + (counterIndex + 1) + " is now free");

                    counters[counterIndex].take();  // Free the counter (consume the token from counter's queue)
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}