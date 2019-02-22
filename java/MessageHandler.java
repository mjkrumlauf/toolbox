package org.mjkrumlauf.toolbox;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles messages and takes action based on the type and/or value of
 * the incoming message.  The most important quality of MessageHandler is
 * that it processes messages one at a time, regardless how many threads
 * are passing messages to it.  The {@link ConcurrentLinkedQueue} and
 * the {@link AtomicBoolean} are responsible for this capability.
 * <p/>
 * Adopted from code found in
 * <a href="https://github.com/functionaljava/functionaljava">https://github.com/functionaljava/functionaljava</a>
 */
public class MessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MessageHandler.class);


    private final ConcurrentLinkedQueue<Object> mailbox;
    private final AtomicBoolean suspended;
    private final Runnable processor;

    public MessageHandler(final Consumer<Object> consumer) {
        this.mailbox = new ConcurrentLinkedQueue<>();
        this.suspended = new AtomicBoolean(true);

        this.processor = new Runnable() {
            @Override
            public void run() {

                // get next message from the queue
                final Object msg = mailbox.poll();

                // if there is one, process it
                if (null != msg) {
                    // do the work
                    consumer.accept(msg);

                    // try again in case there are more messages
                    run();
                } else {
                    // clear the "lock"
                    suspended.set(true);

                    // work again, in case someone else queued up a message while we were
                    // holding the "lock"
                    work();
                }
            }
        };
    }

    // If there are pending messages, run the processor
    void work() {
        if (!this.mailbox.isEmpty() && this.suspended.compareAndSet(true, false)) {
            this.processor.run();
        }
    }

    // Queue up a message and attempt to run the processor
    public void handle(final Object msg) {
        this.mailbox.offer(msg);
        this.work();
    }

    // Unrecognized messages should be passed here
    public void unhandled(final Object msg) {
        LOG.error("Unrecognized message passed to MessageHandler", msg);
    }
}
