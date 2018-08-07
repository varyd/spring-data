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

package com.arangodb.springframework.testdata;

import com.arangodb.springframework.annotation.Document;

/**
 * @author Mark Vollmary
 *
 */
@Document
public class ViewEntityA extends CommonViewEntity {

	private String a;

	public ViewEntityA(final String id, final String value, final String a) {
		super(id, value);
		this.a = a;
	}

	public String getA() {
		return a;
	}

	public void setA(final String a) {
		this.a = a;
	}

}
