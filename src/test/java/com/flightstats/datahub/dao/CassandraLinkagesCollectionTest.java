package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;

import static org.mockito.Mockito.*;

public class CassandraLinkagesCollectionTest {

	static final String CHANNEL_NAME = "foochanchanchan";
	public static final StringSerializer STRING_SERIALIZER = StringSerializer.get();

	private final DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();

	@Test
	public void testUpdateLinkages() throws Exception {
		//GIVEN
		DataHubKey insertedKey = new DataHubKey(new Date(1234556L), (short) 0);
		DataHubKey lastUpdateKey = new DataHubKey(new Date(8383748734L), (short) 0);

		CassandraConnector connector = mock(CassandraConnector.class);
		Mutator<String> mutator = mock(Mutator.class);
		RowKeyStrategy rowKeyStrategy = mock(RowKeyStrategy.class);
		HColumn<String, String> expectedPrevColumn = mock(HColumn.class);
		HColumn<String, String> expectedNextColumn = mock(HColumn.class);
		HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);

		when(connector.buildMutator(StringSerializer.get())).thenReturn(mutator);
		when(rowKeyStrategy.buildKey(CHANNEL_NAME, insertedKey)).thenReturn("roe");
		when(rowKeyStrategy.buildKey(CHANNEL_NAME, lastUpdateKey)).thenReturn("roe");
		when(hector.createColumn(keyRenderer.keyToString(insertedKey), keyRenderer.keyToString(lastUpdateKey), STRING_SERIALIZER,
			STRING_SERIALIZER)).thenReturn(expectedPrevColumn);
		when(hector.createColumn(keyRenderer.keyToString(lastUpdateKey), keyRenderer.keyToString(insertedKey), STRING_SERIALIZER,
			STRING_SERIALIZER)).thenReturn(expectedNextColumn);


		CassandraLinkagesCollection testClass = new CassandraLinkagesCollection(connector, hector, keyRenderer, rowKeyStrategy);
		//WHEN
		testClass.updateLinkages(CHANNEL_NAME, insertedKey, lastUpdateKey);

		//THEN
		verify(mutator).addInsertion("roe_previous", CHANNEL_NAME, expectedPrevColumn);
		verify(mutator).addInsertion("roe_next", CHANNEL_NAME, expectedNextColumn);
		verify(mutator).execute();
	}

	@Test
	public void testUpdateLinkages_lastUpdateNull() throws Exception {
		//GIVEN
		DataHubKey insertedKey = new DataHubKey(new Date(1234556L), (short) 0);

		CassandraConnector connector = mock(CassandraConnector.class);

		CassandraLinkagesCollection testClass = new CassandraLinkagesCollection(connector, null, keyRenderer, null);
		//WHEN
		testClass.updateLinkages(CHANNEL_NAME, insertedKey, null);

		//THEN
		verifyNoMoreInteractions(connector);
	}

	@Test
	public void testDeleteLinkages() throws Exception {
		//GIVEN
		DataHubKey keyToDelete = new DataHubKey(new Date(1234556L), (short) 0);
		String columnKey = keyRenderer.keyToString(keyToDelete);

		CassandraConnector connector = mock(CassandraConnector.class);
		Mutator<String> mutator = mock(Mutator.class);
		RowKeyStrategy<String, DataHubKey, DataHubCompositeValue>  rowKeyStrategy = mock(RowKeyStrategy.class);
		HectorFactoryWrapper hector = mock(HectorFactoryWrapper.class);

		when(connector.buildMutator(StringSerializer.get())).thenReturn(mutator);
		when(rowKeyStrategy.buildKey(CHANNEL_NAME, keyToDelete)).thenReturn("roe");


		CassandraLinkagesCollection testClass = new CassandraLinkagesCollection(connector, hector, keyRenderer, rowKeyStrategy);
		//WHEN
		testClass.delete(CHANNEL_NAME, Arrays.asList(keyToDelete));

		//THEN
		verify(mutator).addDeletion("roe_previous", CHANNEL_NAME, columnKey, StringSerializer.get());
		verify(mutator).addDeletion("roe_next", CHANNEL_NAME, columnKey, StringSerializer.get());
		verify(mutator).execute();
	}
}
