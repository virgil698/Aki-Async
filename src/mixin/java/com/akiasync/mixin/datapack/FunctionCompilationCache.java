package com.akiasync.mixin.datapack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.UnboundEntryAction;

import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public final class FunctionCompilationCache {
    private static final int MAX_CONTEXTS = 4;
    private static final int MAX_ENTRIES_PER_CONTEXT = 32_768;
    private static final Object CONTEXT_LOCK = new Object();
    private static final ArrayDeque<State> STATES = new ArrayDeque<>();
    private static volatile State current;

    private FunctionCompilationCache() {
    }

    public static <T extends ExecutionCommandSource<T>> UnboundEntryAction<T> getOrCompile(
            CommandDispatcher<T> dispatcher,
            T source,
            StringReader command,
            Compiler<T> compiler
    ) throws CommandSyntaxException {
        State state = stateFor(dispatcher, source);
        String key = command.getCursor() == 0
                ? command.getString()
                : command.getCursor() + "\0" + command.getString();
        if (state.entries.size() >= MAX_ENTRIES_PER_CONTEXT && !state.entries.containsKey(key)) {
            DataPackOptimizationMetrics.functionMiss();
            return compiler.compile();
        }

        CompletableFuture<UnboundEntryAction<?>> pending = new CompletableFuture<>();
        CompletableFuture<UnboundEntryAction<?>> existing = state.entries.putIfAbsent(key, pending);
        if (existing != null) {
            DataPackOptimizationMetrics.functionHit();
            return await(existing);
        }

        DataPackOptimizationMetrics.functionMiss();
        try {
            UnboundEntryAction<T> compiled = compiler.compile();
            pending.complete(compiled);
            return compiled;
        } catch (CommandSyntaxException | RuntimeException | Error failure) {
            state.entries.remove(key, pending);
            pending.completeExceptionally(failure);
            throw failure;
        }
    }

    static int entryCount() {
        synchronized (CONTEXT_LOCK) {
            return STATES.stream().mapToInt(state -> state.entries.size()).sum();
        }
    }

    static void clear() {
        synchronized (CONTEXT_LOCK) {
            current = null;
            STATES.clear();
        }
    }

    private static State stateFor(Object dispatcher, Object source) {
        State state = current;
        if (state != null && state.matches(dispatcher, source)) {
            return state;
        }
        synchronized (CONTEXT_LOCK) {
            for (State candidate : STATES) {
                if (candidate.matches(dispatcher, source)) {
                    current = candidate;
                    return candidate;
                }
            }
            State created = new State(dispatcher, source);
            if (STATES.size() >= MAX_CONTEXTS) {
                STATES.removeFirst();
            }
            STATES.addLast(created);
            current = created;
            return created;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> UnboundEntryAction<T> await(CompletableFuture<UnboundEntryAction<?>> future)
            throws CommandSyntaxException {
        try {
            return (UnboundEntryAction<T>) future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof CommandSyntaxException syntaxException) {
                throw syntaxException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw exception;
        }
    }

    @FunctionalInterface
    public interface Compiler<T> {
        UnboundEntryAction<T> compile() throws CommandSyntaxException;
    }

    private static final class State {
        private final Object dispatcher;
        private final Object source;
        private final ConcurrentHashMap<String, CompletableFuture<UnboundEntryAction<?>>> entries = new ConcurrentHashMap<>();

        private State(Object dispatcher, Object source) {
            this.dispatcher = dispatcher;
            this.source = source;
        }

        private boolean matches(Object expectedDispatcher, Object expectedSource) {
            return dispatcher == expectedDispatcher && source == expectedSource;
        }
    }
}
