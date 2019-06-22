package Common;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class Common {

    private static <T> CompletableFuture<T> supplyAsync(Callable<T> c) {
        CompletableFuture<T> f = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                f.complete(c.call());
            } catch (Throwable t) {
                f.completeExceptionally(t);
            }
        });
        return f;
    }
}
