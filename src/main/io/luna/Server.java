package io.luna;

import io.luna.game.GameService;
import io.luna.net.LunaChannelInitializer;
import io.luna.net.LunaNetworkConstants;
import io.luna.util.StringUtils;
import io.luna.util.yaml.deserialize.NpcDefinitionDeserializer;
import io.luna.util.yaml.deserialize.PersistentFieldDeserializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import plugin.PluginBootstrap;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Initializes the individual modules to launch {@link Luna}.
 * 
 * @author lare96 <http://github.org/lare96>
 */
public final class Server {

    /**
     * The logger that will print important information.
     */
    private static final Logger LOGGER = LogManager.getLogger(Server.class);

    /**
     * The {@link ExecutorService} that will execute startup tasks.
     */
    private final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactoryBuilder().setNameFormat(
        "LunaInitializationThread").build());

    /**
     * The {@link LunaContext} that this {@code Server} will be managed with.
     */
    private final LunaContext context = new LunaContext();

    /**
     * A package-private constructor to discourage external instantiation
     * outside of the {@code io.luna} package.
     */
    Server() {}

    /**
     * Creates {@link Luna} by initializing all of the individual modules.
     * 
     * @throws Exception If any exceptions are thrown during initialization.
     */
    public void create() throws Exception {
        LOGGER.info("Luna is being initialized...");

        initAsyncTasks();
        initGame();
        service.shutdown();
        service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        context.getService().awaitRunning();
        bind();

        LOGGER.info("Luna is now online on port " + LunaNetworkConstants.PORT + ".");
    }

    /**
     * Initializes the Netty implementation. Will block indefinitely until the
     * {@link ServerBootstrap} is bound.
     * 
     * @throws Exception If any exceptions are thrown while binding.
     */
    private void bind() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup loopGroup = new NioEventLoopGroup();

        ResourceLeakDetector.setLevel(LunaNetworkConstants.RESOURCE_LEAK_DETECTION);

        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.group(loopGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childHandler(new LunaChannelInitializer(context));
        bootstrap.bind(LunaNetworkConstants.PORT).syncUninterruptibly();

        if (!LunaNetworkConstants.PREFERRED_PORTS.contains(LunaNetworkConstants.PORT)) {
            String prefix = "The preferred ports for Runescape servers are ";
            LOGGER.info(StringUtils.joinWithAnd(prefix, ".", LunaNetworkConstants.PREFERRED_PORTS.asList()));
        }
    }

    /**
     * Initializes the {@link GameService} asynchronously, does not wait for it
     * to enter a {@code RUNNING} state.
     * 
     * @throws Exception If any exceptions are thrown during initialization of
     *         the {@code GameService}.
     */
    private void initGame() throws Exception {
        context.getService().startAsync();
    }

    /**
     * Executes all startup tasks asynchronously in the background using
     * {@link ExecutorService}.
     * 
     * @throws Exception If any exceptions are thrown while executing startup
     *         tasks.
     */
    private void initAsyncTasks() throws Exception {
        service.execute(new PluginBootstrap(LogManager.getLogger(PluginBootstrap.class), context));
        service.execute(new NpcDefinitionDeserializer());
        service.execute(new PersistentFieldDeserializer());
    }
}
