/**
 *
 */
package com.arangodb.springframework.core.mapping;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.arangodb.entity.ViewType;
import com.arangodb.entity.arangosearch.ArangoSearchPropertiesEntity;
import com.arangodb.entity.arangosearch.CollectionLink;
import com.arangodb.entity.arangosearch.ConsolidateThreshold;
import com.arangodb.entity.arangosearch.ConsolidateType;
import com.arangodb.entity.arangosearch.FieldLink;
import com.arangodb.model.arangosearch.ArangoSearchPropertiesOptions;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.annotation.ArangoSearchView;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.core.ArangoSearchOperations;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoSearchMappingTest extends AbstractArangoTest {

	@Test
	public void createByName() {
		template.collection("view-test-collection");
		final ArangoSearchPropertiesOptions options = new ArangoSearchPropertiesOptions().cleanupIntervalStep(15L)
				.commitIntervalMsec(65000L).threshold(ConsolidateThreshold.of(ConsolidateType.COUNT).threshold(1.))
				.link(CollectionLink.on("view-test-collection").fields(FieldLink.on("value").includeAllFields(true)));
		final ArangoSearchOperations arangosearch = template.arangosearch("testView", options);
		final ArangoSearchPropertiesEntity properties = arangosearch.getProperties();
		assertThat(properties.getType(), is(ViewType.ARANGO_SEARCH));
		assertThat(properties.getName(), is("testView"));
		assertThat(properties.getCleanupIntervalStep(), is(15L));
		assertThat(properties.getCommitIntervalMsec(), is(65000L));
		final ConsolidateThreshold next = properties.getThresholds().iterator().next();
		assertThat(next.getType(), is(ConsolidateType.COUNT));
		assertThat(next.getThreshold(), is(1.));
		assertThat(properties.getLinks().size(), is(1));
		final CollectionLink collectionLink = properties.getLinks().iterator().next();
		assertThat(collectionLink.getName(), is("view-test-collection"));
		assertThat(collectionLink.getFields().size(), is(1));
		final FieldLink fieldLink = collectionLink.getFields().iterator().next();
		assertThat(fieldLink.getName(), is("value"));
		assertThat(fieldLink.getIncludeAllFields(), is(true));
	}

	@ArangoSearchView(value = "byEntityClassView", cleanupIntervalStep = 15L, commitIntervalMsec = 65000L, countThreshold = 1.)
	@Document("view-test-collection")
	public static class SimpleTestEntity {

		@com.arangodb.springframework.annotation.FieldLink(includeAllFields = true)
		private String value;

	}

	@Test
	public void createByEntityClass() {
		final ArangoSearchOperations arangosearch = template.arangosearch(SimpleTestEntity.class);
		final ArangoSearchPropertiesEntity properties = arangosearch.getProperties();
		assertThat(properties.getType(), is(ViewType.ARANGO_SEARCH));
		assertThat(properties.getName(), is("byEntityClassView"));
		assertThat(properties.getCleanupIntervalStep(), is(15L));
		assertThat(properties.getCommitIntervalMsec(), is(65000L));
		assertThat(properties.getThresholds().size(), is(1));
		final ConsolidateThreshold next = properties.getThresholds().iterator().next();
		assertThat(next.getType(), is(ConsolidateType.COUNT));
		assertThat(next.getThreshold(), is(1.));
		assertThat(properties.getLinks().size(), is(1));
		final CollectionLink collectionLink = properties.getLinks().iterator().next();
		assertThat(collectionLink.getName(), is("view-test-collection"));
		assertThat(collectionLink.getFields().size(), is(1));
		final FieldLink fieldLink = collectionLink.getFields().iterator().next();
		assertThat(fieldLink.getName(), is("value"));
		assertThat(fieldLink.getIncludeAllFields(), is(true));
	}

}
