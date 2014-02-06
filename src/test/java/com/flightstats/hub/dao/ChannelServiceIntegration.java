package com.flightstats.hub.dao;

public abstract class ChannelServiceIntegration {
    /*private final static Logger logger = LoggerFactory.getLogger(ChannelServiceIntegration.class);

    protected static Injector injector;
    protected ChannelService channelService;
    protected String channelName;
    protected static List<String> channelNames = new ArrayList<>();


    public static void finalStartup(Properties properties) throws Exception {
        Integration.startZooKeeper();
        injector = GuiceContext.construct(properties).getInjector();
        channelNames.clear();
    }

    @Before
    public void setUp() throws Exception {
        verifyStartup();
        channelService = injector.getInstance(ChannelService.class);
        channelName = UUID.randomUUID().toString().replaceAll("-", "");
        channelNames.add(channelName);
    }

    public static void tearDown() throws IOException {
    }

    *//**
     * Do any additional verification here
     *//*
    protected abstract void verifyStartup();

    @Test
    public void testChannelCreation() throws Exception {

        assertNull(channelService.getChannelConfiguration(channelName));
        ChannelConfiguration configuration = ChannelConfiguration.builder().withName(channelName).withTtlDays(1L).build();
        ChannelConfiguration createdChannel = channelService.createChannel(configuration);
        assertEquals(channelName, createdChannel.getName());
        assertEquals(createdChannel, channelService.getChannelConfiguration(channelName));
    }

    @Test
    public void testChannelWriteRead() throws Exception {
        ChannelConfiguration configuration = ChannelConfiguration.builder().withName(channelName).withTtlDays(1L).build();
        channelService.createChannel(configuration);
        assertFalse(channelService.getValue(channelName, new SequenceContentKey(1000).keyToString()).isPresent());
        byte[] bytes = "some data".getBytes();
        Content content = Content.builder().withData(bytes).build();
        ValueInsertionResult insert = channelService.insert(channelName, content);

        Optional<LinkedContent> value = channelService.getValue(channelName, insert.getKey().keyToString());
        assertTrue(value.isPresent());
        LinkedContent compositeValue = value.get();
        assertArrayEquals(bytes, compositeValue.getData());

        assertFalse(compositeValue.getContentType().isPresent());
        assertFalse(compositeValue.getValue().getContentLanguage().isPresent());
    }

    @Test
    public void testChannelOptionals() throws Exception {

        ChannelConfiguration configuration = ChannelConfiguration.builder().withName(channelName).withTtlDays(1L).build();
        channelService.createChannel(configuration);
        byte[] bytes = "testChannelOptionals".getBytes();
        Content content = Content.builder().withData(bytes).withContentLanguage("lang").withContentType("content").build();
        ValueInsertionResult insert = channelService.insert(channelName, content);

        Optional<LinkedContent> value = channelService.getValue(channelName, insert.getKey().keyToString());
        assertTrue(value.isPresent());
        LinkedContent compositeValue = value.get();
        assertArrayEquals(bytes, compositeValue.getData());

        assertEquals("content", compositeValue.getContentType().get());
        assertEquals("lang", compositeValue.getValue().getContentLanguage().get());
    }

    @Test
    public void testChannels() throws Exception {
        Set<String> existing = new HashSet<>(channelNames);
        existing.remove(channelName);
        Set<String> found = new HashSet<>();
        Iterable<ChannelConfiguration> foundChannels = channelService.getChannels();
        Iterator<ChannelConfiguration> iterator = foundChannels.iterator();
        while (iterator.hasNext()) {
            ChannelConfiguration configuration = iterator.next();
            found.add(configuration.getName());
        }
        logger.info("existing " + existing);
        logger.info("found " + found);
        assertTrue(found.containsAll(existing));

        assertTrue(channelService.isHealthy());
    }*/

}
