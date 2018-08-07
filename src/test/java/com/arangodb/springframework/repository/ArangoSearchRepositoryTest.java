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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.testdata.CommonViewEntity;
import com.arangodb.springframework.testdata.ViewEntityA;
import com.arangodb.springframework.testdata.ViewEntityB;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoSearchRepositoryTest extends AbstractArangoTest {

	@Autowired
	ArangoSearchTestRepository repository;
	@Autowired
	ArangoOperations template;

	public ArangoSearchRepositoryTest() {
		super(ViewEntityA.class, ViewEntityB.class);
	}

	@Override
	@Before
	public void before() {
		super.before();
		template.insert(new ViewEntityA("1", "test0", "a"));
		template.insert(new ViewEntityA("2", "test1", "a"));
		template.insert(new ViewEntityB("3", "test0", "b"));
		template.insert(new ViewEntityB("4", "test1", "b"));
		try {
			Thread.sleep(1000);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void findById() {
		final Optional<CommonViewEntity> findById = repository.findById("1");
		assertThat(findById.isPresent(), is(true));
	}

}
