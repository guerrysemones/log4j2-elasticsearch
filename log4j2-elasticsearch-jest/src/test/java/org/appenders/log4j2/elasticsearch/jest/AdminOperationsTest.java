package org.appenders.log4j2.elasticsearch.jest;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import io.searchbox.action.TemplateActionIntrospector;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.indices.template.TemplateAction;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import static org.appenders.log4j2.elasticsearch.jest.JestHttpObjectFactoryTest.createTestObjectFactoryBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdminOperationsTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void passesIndexTemplateToClient() throws IOException {

        //given
        JestHttpObjectFactory factory = spy(createTestObjectFactoryBuilder().build());

        JestClient jestClient = mockedJestClient(factory);

        mockedJestResult(jestClient, true);

        IndexTemplate indexTemplate = spy(IndexTemplate.newBuilder()
                .withPath("classpath:indexTemplate.json")
                .withName("testName")
                .build());

        String expectedPayload = indexTemplate.getSource();

        // when
        factory.execute(indexTemplate);

        // then
        ArgumentCaptor<TemplateAction> requestArgumentCaptor = ArgumentCaptor.forClass(TemplateAction.class);
        verify(jestClient).execute(requestArgumentCaptor.capture());

        String actualPayload = extractPayload(requestArgumentCaptor.getValue());

        Assert.assertEquals(actualPayload, expectedPayload);

    }

    @Test
    public void throwsIfTemplateActionNotSucceeded() throws IOException {

        //given
        JestHttpObjectFactory factory = spy(createTestObjectFactoryBuilder().build());

        JestClient jestClient = mockedJestClient(factory);

        mockedJestResult(jestClient, false);

        IndexTemplate indexTemplate = spy(IndexTemplate.newBuilder()
                .withPath("classpath:indexTemplate.json")
                .withName("testName")
                .build());

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("IndexTemplate not added");

        // when
        factory.execute(indexTemplate);

    }


    @Test
    public void throwsOnIOException() {

        //given
        JestHttpObjectFactory factory = spy(createTestObjectFactoryBuilder().build());

        final String expectedMessage = "test-exception";

        when(factory.createClient()).thenAnswer((Answer<JestResult>) invocation -> {
            throw new IOException(expectedMessage);
        });

        IndexTemplate indexTemplate = spy(IndexTemplate.newBuilder()
                .withPath("classpath:indexTemplate.json")
                .withName("testName")
                .build());

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage(expectedMessage);

        // when
        factory.execute(indexTemplate);

    }

    private void mockedJestResult(JestClient jestClient, boolean isSucceeded) throws IOException {
        JestResult result = mock(JestResult.class);
        when(jestClient.execute(any())).thenReturn(result);

        when(result.isSucceeded()).thenReturn(isSucceeded);
    }

    private JestClient mockedJestClient(JestHttpObjectFactory factory) {
        ClientProvider clientProvider = mock(ClientProvider.class);
        when(factory.getClientProvider(any())).thenReturn(clientProvider);

        JestClient jestClient = mock(JestClient.class);
        when(clientProvider.createClient()).thenReturn(jestClient);
        return jestClient;
    }

    private String extractPayload(TemplateAction templateAction) {
        return new TemplateActionIntrospector().getPayload(templateAction);
    }

}
