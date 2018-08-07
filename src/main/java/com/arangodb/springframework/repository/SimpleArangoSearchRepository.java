/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
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

package com.arangodb.springframework.repository;

import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.arangodb.ArangoCursor;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.util.MapBuilder;

/**
 * @author Mark Vollmary
 *
 */
@Repository
@SuppressWarnings({ "unchecked" })
public class SimpleArangoSearchRepository<T, ID> implements ArangoSearchRepository<T, ID> {

	private final ArangoOperations arangoOperations;
	private final Class<?> domainType;
	private final String view;

	public SimpleArangoSearchRepository(final ArangoOperations arangoOperations, final Class<?> domainType) {
		this.arangoOperations = arangoOperations;
		this.domainType = domainType;
		view = arangoOperations.arangosearch(domainType).name();
	}

	@Override
	public <S extends T> Optional<S> findOne(final Example<S> example) {
		// TODO
		return null;
	}

	@Override
	public <S extends T> Iterable<S> findAll(final Example<S> example) {
		// TODO
		return null;
	}

	@Override
	public <S extends T> Iterable<S> findAll(final Example<S> example, final Sort sort) {
		// TODO
		return null;
	}

	@Override
	public <S extends T> Page<S> findAll(final Example<S> example, final Pageable pageable) {
		// TODO
		return null;
	}

	@Override
	public <S extends T> long count(final Example<S> example) {
		// TODO
		return 0;
	}

	@Override
	public <S extends T> boolean exists(final Example<S> example) {
		// TODO
		return false;
	}

	@Override
	public Iterable<T> findAll(final Sort sort) {
		// TODO
		return null;
	}

	@Override
	public Page<T> findAll(final Pageable pageable) {
		// TODO
		return null;
	}

	@Override
	public Optional<T> findById(final ID id) {
		final Map<String, Object> bindVars = new MapBuilder().put("@view", view).put("id", id).get();
		final ArangoCursor<T> cursor = (ArangoCursor<T>) arangoOperations
				.query("FOR i IN VIEW @@view FILTER i._key == @id RETURN i", bindVars, domainType);
		return Optional.ofNullable(cursor.first());
	}

	@Override
	public boolean existsById(final ID id) {
		// TODO
		return false;
	}

	@Override
	public Iterable<T> findAll() {
		// TODO
		return null;
	}

	@Override
	public Iterable<T> findAllById(final Iterable<ID> ids) {
		// TODO
		return null;
	}

	@Override
	public long count() {
		// TODO
		return 0;
	}

}
