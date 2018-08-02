/*
 * DISCLAIMER
 *
 * Copyright 2017 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.springframework.core.mapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.TargetAwareIdentifierAccessor;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import com.arangodb.entity.CollectionType;
import com.arangodb.entity.arangosearch.CollectionLink;
import com.arangodb.entity.arangosearch.ConsolidateThreshold;
import com.arangodb.entity.arangosearch.ConsolidateType;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.arangosearch.ArangoSearchPropertiesOptions;
import com.arangodb.springframework.annotation.ArangoSearchView;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.FieldLink;
import com.arangodb.springframework.annotation.FulltextIndex;
import com.arangodb.springframework.annotation.FulltextIndexes;
import com.arangodb.springframework.annotation.GeoIndex;
import com.arangodb.springframework.annotation.GeoIndexes;
import com.arangodb.springframework.annotation.HashIndex;
import com.arangodb.springframework.annotation.HashIndexes;
import com.arangodb.springframework.annotation.PersistentIndex;
import com.arangodb.springframework.annotation.PersistentIndexes;
import com.arangodb.springframework.annotation.SkiplistIndex;
import com.arangodb.springframework.annotation.SkiplistIndexes;

/**
 * @author Mark Vollmary
 * @param <T>
 *
 */
public class DefaultArangoPersistentEntity<T> extends BasicPersistentEntity<T, ArangoPersistentProperty>
		implements ArangoPersistentEntity<T> {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private final String collection;
	private final String arangoSearch;
	private final Expression collectionExpression;
	private final Expression arangoSearchExpression;
	private final StandardEvaluationContext context;

	private ArangoPersistentProperty arangoIdProperty;
	private ArangoPersistentProperty revProperty;
	private final Collection<ArangoPersistentProperty> hashIndexedProperties;
	private final Collection<ArangoPersistentProperty> skiplistIndexedProperties;
	private final Collection<ArangoPersistentProperty> persistentIndexedProperties;
	private final Collection<ArangoPersistentProperty> geoIndexedProperties;
	private final Collection<ArangoPersistentProperty> fulltextIndexedProperties;
	private final Collection<ArangoPersistentProperty> fieldLinkedProperties;

	private final CollectionCreateOptions collectionOptions;
	private final ArangoSearchPropertiesOptions arangoSearchOptions;

	private final Map<Class<? extends Annotation>, Set<? extends Annotation>> repeatableAnnotationCache;

	public DefaultArangoPersistentEntity(final TypeInformation<T> information) {
		super(information);
		context = new StandardEvaluationContext();
		hashIndexedProperties = new ArrayList<>();
		skiplistIndexedProperties = new ArrayList<>();
		persistentIndexedProperties = new ArrayList<>();
		geoIndexedProperties = new ArrayList<>();
		fulltextIndexedProperties = new ArrayList<>();
		fieldLinkedProperties = new ArrayList<>();
		repeatableAnnotationCache = new HashMap<>();
		final Document document = findAnnotation(Document.class);
		final Edge edge = findAnnotation(Edge.class);
		final ArangoSearchView asView = findAnnotation(ArangoSearchView.class);
		final String uncapitalizeTypeName = StringUtils.uncapitalize(information.getType().getSimpleName());
		if (edge != null) {
			collection = StringUtils.hasText(edge.value()) ? edge.value() : uncapitalizeTypeName;
			collectionOptions = createCollectionOptions(edge);
			collectionExpression = PARSER.parseExpression(collection, ParserContext.TEMPLATE_EXPRESSION);
		} else if (document != null) {
			collection = StringUtils.hasText(document.value()) ? document.value() : uncapitalizeTypeName;
			collectionOptions = createCollectionOptions(document);
			collectionExpression = PARSER.parseExpression(collection, ParserContext.TEMPLATE_EXPRESSION);
		} else if (asView == null) {
			collection = uncapitalizeTypeName;
			collectionOptions = new CollectionCreateOptions().type(CollectionType.DOCUMENT);
			collectionExpression = PARSER.parseExpression(collection, ParserContext.TEMPLATE_EXPRESSION);
		} else {
			collection = null;
			collectionOptions = null;
			collectionExpression = null;
		}
		if (asView != null) {
			arangoSearch = StringUtils.hasText(asView.value()) ? asView.value() : uncapitalizeTypeName;
			arangoSearchOptions = createArangoSearchOptions(asView);
			arangoSearchExpression = PARSER.parseExpression(arangoSearch, ParserContext.TEMPLATE_EXPRESSION);
		} else {
			arangoSearch = null;
			arangoSearchOptions = null;
			arangoSearchExpression = null;
		}
	}

	private static CollectionCreateOptions createCollectionOptions(final Document annotation) {
		final CollectionCreateOptions options = new CollectionCreateOptions().type(CollectionType.DOCUMENT)
				.waitForSync(annotation.waitForSync()).doCompact(annotation.doCompact())
				.isVolatile(annotation.isVolatile()).isSystem(annotation.isSystem());
		if (annotation.journalSize() > -1) {
			options.journalSize(annotation.journalSize());
		}
		if (annotation.replicationFactor() > -1) {
			options.replicationFactor(annotation.replicationFactor());
		}
		if (annotation.satellite()) {
			options.satellite(annotation.satellite());
		}
		final String[] shardKeys = annotation.shardKeys();
		if (shardKeys.length > 1 || (shardKeys.length > 0 && StringUtils.hasText(shardKeys[0]))) {
			options.shardKeys(shardKeys);
		}
		if (annotation.numberOfShards() > -1) {
			options.numberOfShards(annotation.numberOfShards());
		}
		if (annotation.indexBuckets() > -1) {
			options.indexBuckets(annotation.indexBuckets());
		}
		if (annotation.allowUserKeys()) {
			options.keyOptions(annotation.allowUserKeys(), annotation.keyType(),
				annotation.keyIncrement() > -1 ? annotation.keyIncrement() : null,
				annotation.keyOffset() > -1 ? annotation.keyOffset() : null);
		}
		return options;
	}

	private static CollectionCreateOptions createCollectionOptions(final Edge annotation) {
		final CollectionCreateOptions options = new CollectionCreateOptions().type(CollectionType.EDGES)
				.waitForSync(annotation.waitForSync()).doCompact(annotation.doCompact())
				.isVolatile(annotation.isVolatile()).isSystem(annotation.isSystem());
		if (annotation.journalSize() > -1) {
			options.journalSize(annotation.journalSize());
		}
		if (annotation.replicationFactor() > -1) {
			options.replicationFactor(annotation.replicationFactor());
		}
		final String[] shardKeys = annotation.shardKeys();
		if (shardKeys.length > 0) {
			options.shardKeys(shardKeys);
		}
		if (annotation.numberOfShards() > -1) {
			options.numberOfShards(annotation.numberOfShards());
		}
		if (annotation.indexBuckets() > -1) {
			options.indexBuckets(annotation.indexBuckets());
		}
		if (annotation.allowUserKeys()) {
			options.keyOptions(annotation.allowUserKeys(), annotation.keyType(),
				annotation.keyIncrement() > -1 ? annotation.keyIncrement() : null,
				annotation.keyOffset() > -1 ? annotation.keyOffset() : null);
		}
		return options;
	}

	private ArangoSearchPropertiesOptions createArangoSearchOptions(final ArangoSearchView view) {
		final ArangoSearchPropertiesOptions options = new ArangoSearchPropertiesOptions();
		final String locale = view.locale();
		if (!locale.isEmpty()) {
			options.locale(locale);
		}
		final long commitIntervalMsec = view.commitIntervalMsec();
		if (commitIntervalMsec > -1) {
			options.commitIntervalMsec(commitIntervalMsec);
		}
		final long cleanupIntervalStep = view.cleanupIntervalStep();
		if (cleanupIntervalStep > -1) {
			options.cleanupIntervalStep(cleanupIntervalStep);
		}
		final ConsolidateThreshold countThreshold = threshold(ConsolidateType.COUNT, view.countThreshold(),
			view.countSegmentThreshold());
		if (countThreshold != null) {
			options.threshold(countThreshold);
		}
		final ConsolidateThreshold bytesThreshold = threshold(ConsolidateType.BYTES, view.bytesThreshold(),
			view.bytesSegmentThreshold());
		if (bytesThreshold != null) {
			options.threshold(bytesThreshold);
		}
		final ConsolidateThreshold bytesAccumThreshold = threshold(ConsolidateType.BYTES_ACCUM,
			view.bytesAccumThreshold(), view.bytesAccumSegmentThreshold());
		if (bytesAccumThreshold != null) {
			options.threshold(bytesAccumThreshold);
		}
		final ConsolidateThreshold fillThreshold = threshold(ConsolidateType.FILL, view.fillThreshold(),
			view.fillSegmentThreshold());
		if (fillThreshold != null) {
			options.threshold(fillThreshold);
		}
		final String collection = getCollection();
		if (collection != null) {
			final CollectionLink link = CollectionLink.on(collection);
			final String[] analyzers = view.analyzers();
			if (analyzers.length > 0) {
				link.analyzers(analyzers);
			}
			link.includeAllFields(view.includeAllFields());
			link.trackListPositions(view.trackListPositions());
			link.storeValues(view.storeValues());
			fieldLinkedProperties.stream().map(property -> fieldLink(property)).forEach(link::fields);
		}
		return options;
	}

	private static ConsolidateThreshold threshold(
		final ConsolidateType type,
		final double threshold,
		final long segmentThreshold) {
		final ConsolidateThreshold of;
		if (threshold > -1 || segmentThreshold > -1) {
			of = ConsolidateThreshold.of(type);
			if (threshold > -1) {
				of.threshold(threshold);
			}
			if (segmentThreshold > -1) {
				of.segmentThreshold(segmentThreshold);
			}
		} else {
			of = null;
		}
		return of;
	}

	private static com.arangodb.entity.arangosearch.FieldLink fieldLink(final ArangoPersistentProperty property) {
		final com.arangodb.entity.arangosearch.FieldLink fieldLink = com.arangodb.entity.arangosearch.FieldLink
				.on(property.getName());
		final FieldLink an = property.getFieldLink().get();
		final String[] analyzers = an.analyzers();
		if (analyzers.length > 0) {
			fieldLink.analyzers(analyzers);
		}
		fieldLink.includeAllFields(an.includeAllFields());
		fieldLink.trackListPositions(an.trackListPositions());
		fieldLink.storeValues(an.storeValues());
		return fieldLink;
	}

	@Override
	public String getCollection() {
		return collectionExpression != null ? collectionExpression.getValue(context, String.class) : collection;
	}

	@Override
	public String getArangoSearchView() {
		return arangoSearchExpression != null ? arangoSearchExpression.getValue(context, String.class) : arangoSearch;
	}

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
		context.setRootObject(applicationContext);
		context.setBeanResolver(new BeanFactoryResolver(applicationContext));
		context.addPropertyAccessor(new BeanFactoryAccessor());
	}

	@Override
	public void addPersistentProperty(final ArangoPersistentProperty property) {
		super.addPersistentProperty(property);
		if (property.isArangoIdProperty()) {
			arangoIdProperty = property;
		}
		if (property.isRevProperty()) {
			revProperty = property;
		}
		property.getHashIndexed().ifPresent(i -> hashIndexedProperties.add(property));
		property.getSkiplistIndexed().ifPresent(i -> skiplistIndexedProperties.add(property));
		property.getPersistentIndexed().ifPresent(i -> persistentIndexedProperties.add(property));
		property.getGeoIndexed().ifPresent(i -> geoIndexedProperties.add(property));
		property.getFulltextIndexed().ifPresent(i -> fulltextIndexedProperties.add(property));
		property.getFieldLink().ifPresent(i -> fieldLinkedProperties.add(property));
	}

	@Override
	public Optional<ArangoPersistentProperty> getArangoIdProperty() {
		return Optional.ofNullable(arangoIdProperty);
	}

	@Override
	public Optional<ArangoPersistentProperty> getRevProperty() {
		return Optional.ofNullable(revProperty);
	}

	@Override
	public CollectionCreateOptions getCollectionOptions() {
		return collectionOptions;
	}

	@Override
	public ArangoSearchPropertiesOptions getArangoSearchOptions() {
		return arangoSearchOptions;
	}

	@Override
	public Collection<HashIndex> getHashIndexes() {
		final Collection<HashIndex> indexes = getIndexes(HashIndex.class);
		Optional.ofNullable(findAnnotation(HashIndexes.class)).ifPresent(i -> indexes.addAll(Arrays.asList(i.value())));
		return indexes;
	}

	@Override
	public Collection<SkiplistIndex> getSkiplistIndexes() {
		final Collection<SkiplistIndex> indexes = getIndexes(SkiplistIndex.class);
		Optional.ofNullable(findAnnotation(SkiplistIndexes.class))
				.ifPresent(i -> indexes.addAll(Arrays.asList(i.value())));
		return indexes;
	}

	@Override
	public Collection<PersistentIndex> getPersistentIndexes() {
		final Collection<PersistentIndex> indexes = getIndexes(PersistentIndex.class);
		Optional.ofNullable(findAnnotation(PersistentIndexes.class))
				.ifPresent(i -> indexes.addAll(Arrays.asList(i.value())));
		return indexes;
	}

	@Override
	public Collection<GeoIndex> getGeoIndexes() {
		final Collection<GeoIndex> indexes = getIndexes(GeoIndex.class);
		Optional.ofNullable(findAnnotation(GeoIndexes.class)).ifPresent(i -> indexes.addAll(Arrays.asList(i.value())));
		return indexes;
	}

	@Override
	public Collection<FulltextIndex> getFulltextIndexes() {
		final Collection<FulltextIndex> indexes = getIndexes(FulltextIndex.class);
		Optional.ofNullable(findAnnotation(FulltextIndexes.class))
				.ifPresent(i -> indexes.addAll(Arrays.asList(i.value())));
		return indexes;
	}

	public <A extends Annotation> Collection<A> getIndexes(final Class<A> annotation) {
		final List<A> indexes = findAnnotations(annotation).stream().filter(a -> annotation.isInstance(a))
				.map(a -> annotation.cast(a)).collect(Collectors.toList());
		return indexes;
	}

	@Override
	public Collection<ArangoPersistentProperty> getHashIndexedProperties() {
		return hashIndexedProperties;
	}

	@Override
	public Collection<ArangoPersistentProperty> getSkiplistIndexedProperties() {
		return skiplistIndexedProperties;
	}

	@Override
	public Collection<ArangoPersistentProperty> getPersistentIndexedProperties() {
		return persistentIndexedProperties;
	}

	@Override
	public Collection<ArangoPersistentProperty> getGeoIndexedProperties() {
		return geoIndexedProperties;
	}

	@Override
	public Collection<ArangoPersistentProperty> getFulltextIndexedProperties() {
		return fulltextIndexedProperties;
	}

	@SuppressWarnings("unchecked")
	public <A extends Annotation> Set<A> findAnnotations(final Class<A> annotationType) {
		return (Set<A>) repeatableAnnotationCache.computeIfAbsent(annotationType,
			it -> AnnotatedElementUtils.findMergedRepeatableAnnotations(getType(), it));
	}

	private static class AbsentAccessor extends TargetAwareIdentifierAccessor {

		public AbsentAccessor(final Object target) {
			super(target);
		}

		@Override
		@Nullable
		public Object getIdentifier() {
			return null;
		}
	}

	@Override
	public IdentifierAccessor getArangoIdAccessor(final Object bean) {
		return getArangoIdProperty().isPresent() ? new ArangoIdPropertyIdentifierAccessor(this, bean)
				: new AbsentAccessor(bean);
	}

}
