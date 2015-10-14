package com.flightstats.hub.org.eclipse.jetty.server;

import com.flightstats.hub.app.HubMain;
import com.flightstats.hub.app.ShutdownResource;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.SelectorManager;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.component.*;
import org.eclipse.jetty.util.thread.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public class HubServerConnector implements NetworkConnector {

    private final static Logger logger = LoggerFactory.getLogger(HubServerConnector.class);

    private ServerConnector serverConnector;

    public HubServerConnector(ServerConnector serverConnector) {
        this.serverConnector = serverConnector;
    }

    /**
     * This method is the reason for this class to exist.
     * We want to be able to handle "Too many open files" IOExceptions.
     *
     * @param acceptorID
     * @throws IOException
     */
    public void accept(int acceptorID) throws IOException {
        try {
            serverConnector.accept(acceptorID);
        } catch (IOException e) {
            if ("Too many open files".equals(e.getMessage())) {
                logger.warn("too many open files, shutting down!");
                ShutdownResource shutdownResource = HubMain.getInjector().getInstance(ShutdownResource.class);
                shutdownResource.shutdown();
            }
            throw e;
        }
    }

    public void setHost(String host) {
        serverConnector.setHost(host);
    }

    @Override
    @ManagedAttribute("The network interface this connector binds to as an IP address or a hostname.  If null or 0.0.0.0, then bind to all interfaces.")
    public String getHost() {
        return serverConnector.getHost();
    }

    public void setPort(int port) {
        serverConnector.setPort(port);
    }

    @Override
    @ManagedAttribute("Port this connector listens on. If set the 0 a random port is assigned which may be obtained with getLocalPort()")
    public int getPort() {
        return serverConnector.getPort();
    }

    @Override
    public String toString() {
        return serverConnector.toString();
    }

    public void addConnectionFactory(ConnectionFactory factory) {
        serverConnector.addConnectionFactory(factory);
    }

    public void addFirstConnectionFactory(ConnectionFactory factory) {
        serverConnector.addFirstConnectionFactory(factory);
    }

    public void addIfAbsentConnectionFactory(ConnectionFactory factory) {
        serverConnector.addIfAbsentConnectionFactory(factory);
    }

    public void clearConnectionFactories() {
        serverConnector.clearConnectionFactories();
    }

    @ManagedAttribute("The priority delta to apply to acceptor threads")
    public int getAcceptorPriorityDelta() {
        return serverConnector.getAcceptorPriorityDelta();
    }

    @ManagedAttribute("number of acceptor threads")
    public int getAcceptors() {
        return serverConnector.getAcceptors();
    }

    @Override
    public Collection<EndPoint> getConnectedEndPoints() {
        return serverConnector.getConnectedEndPoints();
    }

    @Override
    public Collection<ConnectionFactory> getConnectionFactories() {
        return serverConnector.getConnectionFactories();
    }

    @ManagedAttribute("This connector\'s default protocol")
    public String getDefaultProtocol() {
        return serverConnector.getDefaultProtocol();
    }

    @Override
    public String getName() {
        return serverConnector.getName();
    }

    @Override
    @ManagedAttribute("Protocols supported by this connector")
    public List<String> getProtocols() {
        return serverConnector.getProtocols();
    }

    @Override
    public Scheduler getScheduler() {
        return serverConnector.getScheduler();
    }

    public void join() throws InterruptedException {
        serverConnector.join();
    }

    public void join(long timeout) throws InterruptedException {
        serverConnector.join(timeout);
    }

    public ConnectionFactory removeConnectionFactory(String protocol) {
        return serverConnector.removeConnectionFactory(protocol);
    }

    public void setAcceptorPriorityDelta(int acceptorPriorityDelta) {
        serverConnector.setAcceptorPriorityDelta(acceptorPriorityDelta);
    }

    public void setConnectionFactories(Collection<ConnectionFactory> factories) {
        serverConnector.setConnectionFactories(factories);
    }

    public void setDefaultProtocol(String defaultProtocol) {
        serverConnector.setDefaultProtocol(defaultProtocol);
    }

    public void setIdleTimeout(long idleTimeout) {
        serverConnector.setIdleTimeout(idleTimeout);
    }

    public void setName(String name) {
        serverConnector.setName(name);
    }

    public boolean addBean(Object o) {
        return serverConnector.addBean(o);
    }

    public boolean addBean(Object o, boolean managed) {
        return serverConnector.addBean(o, managed);
    }

/*    public boolean addBean(Object o, ContainerLifeCycle.Managed managed) {
        return serverConnector.addBean(o, managed);
    }*/

    public void addEventListener(Container.Listener listener) {
        serverConnector.addEventListener(listener);
    }

    public void addManaged(LifeCycle lifecycle) {
        serverConnector.addManaged(lifecycle);
    }

    public boolean contains(Object bean) {
        return serverConnector.contains(bean);
    }

    public void destroy() {
        serverConnector.destroy();
    }

    @ManagedOperation("Dump the object to a string")
    public String dump() {
        return serverConnector.dump();
    }

    public static String dump(Dumpable dumpable) {
        return ContainerLifeCycle.dump(dumpable);
    }

    public void dump(Appendable out) throws IOException {
        serverConnector.dump(out);
    }

    public void dump(Appendable out, String indent) throws IOException {
        serverConnector.dump(out, indent);
    }

    public static void dump(Appendable out, String indent, Collection<?>... collections) throws IOException {
        ContainerLifeCycle.dump(out, indent, collections);
    }

    public static void dumpObject(Appendable out, Object o) throws IOException {
        ContainerLifeCycle.dumpObject(out, o);
    }

    @ManagedOperation("Dump the object to stderr")
    public void dumpStdErr() {
        serverConnector.dumpStdErr();
    }

    public <T> T getBean(Class<T> clazz) {
        return serverConnector.getBean(clazz);
    }

    public Collection<Object> getBeans() {
        return serverConnector.getBeans();
    }

    public <T> Collection<T> getBeans(Class<T> clazz) {
        return serverConnector.getBeans(clazz);
    }

    public boolean isManaged(Object bean) {
        return serverConnector.isManaged(bean);
    }

    public void manage(Object bean) {
        serverConnector.manage(bean);
    }

    public boolean removeBean(Object o) {
        return serverConnector.removeBean(o);
    }

    public void removeBeans() {
        serverConnector.removeBeans();
    }

    public void removeEventListener(Container.Listener listener) {
        serverConnector.removeEventListener(listener);
    }

    public void setBeans(Collection<Object> beans) {
        serverConnector.setBeans(beans);
    }

    public void setStopTimeout(long stopTimeout) {
        serverConnector.setStopTimeout(stopTimeout);
    }

    public void unmanage(Object bean) {
        serverConnector.unmanage(bean);
    }

    public void updateBean(Object oldBean, Object newBean) {
        serverConnector.updateBean(oldBean, newBean);
    }

    public void updateBean(Object oldBean, Object newBean, boolean managed) {
        serverConnector.updateBean(oldBean, newBean, managed);
    }

    public void updateBeans(Object[] oldBeans, Object[] newBeans) {
        serverConnector.updateBeans(oldBeans, newBeans);
    }

    @Override
    public void addLifeCycleListener(Listener listener) {
        serverConnector.addLifeCycleListener(listener);
    }

    @ManagedAttribute(
            value = "Lifecycle State for this instance",
            readonly = true
    )
    public String getState() {
        return serverConnector.getState();
    }

    public static String getState(LifeCycle lc) {
        return AbstractLifeCycle.getState(lc);
    }

    @ManagedAttribute("The stop timeout in milliseconds")
    public long getStopTimeout() {
        return serverConnector.getStopTimeout();
    }

    @Override
    public boolean isFailed() {
        return serverConnector.isFailed();
    }

    @Override
    public boolean isStopped() {
        return serverConnector.isStopped();
    }

    @Override
    public boolean isStopping() {
        return serverConnector.isStopping();
    }

    @Override
    public void removeLifeCycleListener(Listener listener) {
        serverConnector.removeLifeCycleListener(listener);
    }

    @Override
    public void open() throws IOException {
        serverConnector.open();
    }

    @Override
    public void close() {
        serverConnector.close();
    }

    @Override
    public boolean isOpen() {
        return serverConnector.isOpen();
    }

    @Override
    @ManagedAttribute("local port")
    public int getLocalPort() {
        return serverConnector.getLocalPort();
    }

    @Override
    public Server getServer() {
        return serverConnector.getServer();
    }

    @Override
    public Executor getExecutor() {
        return serverConnector.getExecutor();
    }

    @Override
    public ByteBufferPool getByteBufferPool() {
        return serverConnector.getByteBufferPool();
    }

    @Override
    public ConnectionFactory getConnectionFactory(String protocol) {
        return serverConnector.getConnectionFactory(protocol);
    }

    @Override
    public <T> T getConnectionFactory(Class<T> factoryType) {
        return serverConnector.getConnectionFactory(factoryType);
    }

    @Override
    @ManagedAttribute("Idle timeout")
    public long getIdleTimeout() {
        return serverConnector.getIdleTimeout();
    }

    @Override
    public ConnectionFactory getDefaultConnectionFactory() {
        return serverConnector.getDefaultConnectionFactory();
    }

    @Override
    public Future<Void> shutdown() {
        return serverConnector.shutdown();
    }

    @Override
    @ManagedOperation(
            value = "Starts the instance",
            impact = "ACTION"
    )
    public void start() throws Exception {
        serverConnector.start();
    }

    @Override
    @ManagedOperation(
            value = "Stops the instance",
            impact = "ACTION"
    )
    public void stop() throws Exception {
        serverConnector.stop();
    }

    @Override
    public boolean isRunning() {
        return serverConnector.isRunning();
    }

    @Override
    public boolean isStarted() {
        return serverConnector.isStarted();
    }

    @Override
    public boolean isStarting() {
        return serverConnector.isStarting();
    }

    @ManagedAttribute("Accept Queue size")
    public int getAcceptQueueSize() {
        return serverConnector.getAcceptQueueSize();
    }

    public boolean getReuseAddress() {
        return serverConnector.getReuseAddress();
    }

    public SelectorManager getSelectorManager() {
        return serverConnector.getSelectorManager();
    }

    @Deprecated
    public int getSelectorPriorityDelta() {
        return serverConnector.getSelectorPriorityDelta();
    }

    @ManagedAttribute("TCP/IP solinger time or -1 to disable")
    public int getSoLingerTime() {
        return serverConnector.getSoLingerTime();
    }

    @Override
    public Object getTransport() {
        return serverConnector.getTransport();
    }

    public boolean isInheritChannel() {
        return serverConnector.isInheritChannel();
    }

    public void setAcceptQueueSize(int acceptQueueSize) {
        serverConnector.setAcceptQueueSize(acceptQueueSize);
    }

    public void setInheritChannel(boolean inheritChannel) {
        serverConnector.setInheritChannel(inheritChannel);
    }

    public void setReuseAddress(boolean reuseAddress) {
        serverConnector.setReuseAddress(reuseAddress);
    }

    @Deprecated
    public void setSelectorPriorityDelta(int selectorPriorityDelta) {
        serverConnector.setSelectorPriorityDelta(selectorPriorityDelta);
    }

    public void setSoLingerTime(int lingerTime) {
        serverConnector.setSoLingerTime(lingerTime);
    }
}
