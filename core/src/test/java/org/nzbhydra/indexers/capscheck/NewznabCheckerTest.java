/*
 *  (C) Copyright 2017 TheOtherP (theotherp@gmx.de)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.nzbhydra.indexers.capscheck;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.nzbhydra.config.BaseConfig;
import org.nzbhydra.config.ConfigProvider;
import org.nzbhydra.config.SearchingConfig;
import org.nzbhydra.config.indexer.IndexerCategoryConfig;
import org.nzbhydra.config.indexer.IndexerConfig;
import org.nzbhydra.fortests.NewznabResponseBuilder;
import org.nzbhydra.indexers.BinsearchTest;
import org.nzbhydra.indexers.Indexer.BackendType;
import org.nzbhydra.indexers.IndexerWebAccess;
import org.nzbhydra.indexers.exceptions.IndexerAccessException;
import org.nzbhydra.mapping.newznab.ActionAttribute;
import org.nzbhydra.mapping.newznab.xml.NewznabXmlRoot;
import org.nzbhydra.mapping.newznab.xml.caps.CapsXmlLimits;
import org.nzbhydra.mapping.newznab.xml.caps.CapsXmlRoot;
import org.nzbhydra.mapping.newznab.xml.caps.CapsXmlSearch;
import org.nzbhydra.mapping.newznab.xml.caps.CapsXmlSearching;
import org.nzbhydra.web.WebConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.oxm.Unmarshaller;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.net.URI;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.nzbhydra.mediainfo.InfoProvider.IdType.*;

@SuppressWarnings("ALL")

public class NewznabCheckerTest {


    private CapsXmlRoot capsRoot;
    @Mock
    private IndexerWebAccess indexerWebAccess;
    @Mock
    private BaseConfig baseConfig;
    @Mock
    private SearchingConfig searchingConfig;
    @Mock
    private ConfigProvider configProviderMock;
    @Mock
    private ApplicationEventPublisher publisherMock;
    @InjectMocks
    private NewznabChecker testee = new NewznabChecker();
    private Unmarshaller unmarshaller = new WebConfiguration().marshaller();


    private IndexerConfig indexerConfig;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        indexerConfig = new IndexerConfig();
        indexerConfig.setHost("http://127.0.0.1:1234");
        indexerConfig.setApiKey("apikey");
        when(searchingConfig.getTimeout()).thenReturn(1);
        when(configProviderMock.getBaseConfig()).thenReturn(baseConfig);
        when(baseConfig.getSearching()).thenReturn(searchingConfig);
        capsRoot = new CapsXmlRoot();
        capsRoot.setSearching(new CapsXmlSearching());
        CapsXmlLimits capsLimits = new CapsXmlLimits(200, 100);
        capsRoot.setLimits(capsLimits);
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=caps"), indexerConfig)).thenReturn(capsRoot);
        testee.PAUSE_BETWEEN_CALLS = 0;
    }

    @Test
    public void shouldCheckCaps() throws Exception {
        NewznabResponseBuilder builder = new NewznabResponseBuilder();
        NewznabXmlRoot thronesResult = builder.getTestResult(1, 100, "Thrones", 0, 100);
        thronesResult.getRssChannel().setGenerator("nzedb");
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=tvsearch&tvdbid=121361"), indexerConfig))
                .thenReturn(thronesResult);
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=tvsearch&rid=24493"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "Thrones", 0, 100));
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=tvsearch&tvmazeid=82"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "Thrones", 0, 100));
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=tvsearch&traktid=1390"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "GOT", 0, 100));
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=movie&tmdbid=1399"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "Avengers", 0, 100));
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=movie&imdbid=0848228"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "Avengers", 0, 100));

        capsRoot.getSearching().setAudioSearch(new CapsXmlSearch("yes", "q"));

        CheckCapsResponse checkCapsRespone = testee.checkCaps(indexerConfig);
        assertEquals(6, checkCapsRespone.getIndexerConfig().getSupportedSearchIds().size());
        assertTrue(checkCapsRespone.getIndexerConfig().getSupportedSearchIds().contains(TVDB));
        assertTrue(checkCapsRespone.getIndexerConfig().getSupportedSearchIds().contains(TVRAGE));
        assertTrue(checkCapsRespone.getIndexerConfig().getSupportedSearchIds().contains(TVMAZE));
        assertTrue(checkCapsRespone.getIndexerConfig().getSupportedSearchIds().contains(TRAKT));
        assertTrue(checkCapsRespone.getIndexerConfig().getSupportedSearchIds().contains(IMDB));
        assertTrue(checkCapsRespone.getIndexerConfig().getSupportedSearchIds().contains(TMDB));

        assertEquals(3, checkCapsRespone.getIndexerConfig().getSupportedSearchTypes().size());
        assertTrue(checkCapsRespone.getIndexerConfig().getSupportedSearchTypes().contains(ActionAttribute.AUDIO));
        assertTrue(checkCapsRespone.getIndexerConfig().getSupportedSearchTypes().contains(ActionAttribute.TVSEARCH));
        assertTrue(checkCapsRespone.getIndexerConfig().getSupportedSearchTypes().contains(ActionAttribute.MOVIE));

        assertEquals(BackendType.NZEDB, checkCapsRespone.getIndexerConfig().getBackend());

        assertTrue(checkCapsRespone.isAllCapsChecked());

        verify(indexerWebAccess, times(7)).get(any(), eq(indexerConfig));
    }

    @Test
    public void shouldIdentifyCategoryMapping() throws Exception {
        String xml = Resources.toString(Resources.getResource(BinsearchTest.class, "/org/nzbhydra/mapping/nzbsOrgCapsResponse.xml"), Charsets.UTF_8);
        capsRoot = (CapsXmlRoot) unmarshaller.unmarshal(new StreamSource(new StringReader(xml)));
        when(indexerWebAccess.get(any(), eq(indexerConfig))).thenReturn(capsRoot);

        IndexerCategoryConfig categoryConfig = testee.setSupportedSearchTypesAndIndexerCategoryMapping(indexerConfig, 100);
        assertThat(categoryConfig.getAnime().isPresent(), is(true));
        assertThat(categoryConfig.getAnime().get(), is(7040));
        assertThat(categoryConfig.getNameFromId(7040), is("Other Anime"));
        assertThat(categoryConfig.getNameFromId(5040), is("TV HD"));

        //Test with dog which has different mappings
        xml = Resources.toString(Resources.getResource(BinsearchTest.class, "/org/nzbhydra/mapping/dognzbCapsResponse.xml"), Charsets.UTF_8);
        capsRoot = (CapsXmlRoot) unmarshaller.unmarshal(new StreamSource(new StringReader(xml)));
        when(indexerWebAccess.get(any(), eq(indexerConfig))).thenReturn(capsRoot);

        categoryConfig = testee.setSupportedSearchTypesAndIndexerCategoryMapping(indexerConfig, 100);
        assertThat(categoryConfig.getAnime().isPresent(), is(true));
        assertThat(categoryConfig.getAnime().get(), is(5070));
        assertThat(categoryConfig.getComic().isPresent(), is(true));
        assertThat(categoryConfig.getComic().get(), is(7030));
        assertThat(categoryConfig.getNameFromId(7040), is("N/A"));
        assertThat(categoryConfig.getNameFromId(5040), is("TV HD"));
    }

    @Test
    public void shouldCheckCapsWithoutSupport() throws Exception {
        NewznabResponseBuilder builder = new NewznabResponseBuilder();
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=tvsearch&tvdbid=121361"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "somethingElse", 0, 100));
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=tvsearch&rid=24493"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "somethingElse", 0, 100));
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=tvsearch&tvmazeid=82"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "somethingElse", 0, 100));
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=tvsearch&traktid=1390"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "somethingElse", 0, 100));
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=movie&tmdbid=1399"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "somethingElse", 0, 100));
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=movie&imdbid=0848228"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "somethingElse", 0, 100));

        CheckCapsResponse checkCapsRespone = testee.checkCaps(indexerConfig);
        assertEquals(0, checkCapsRespone.getIndexerConfig().getSupportedSearchIds().size());
        verify(indexerWebAccess, times(7)).get(any(), eq(indexerConfig));
    }

    @Test
    public void shouldSaySoIfNotAllWereChecked() throws Exception {
        NewznabResponseBuilder builder = new NewznabResponseBuilder();
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=tvsearch&tvdbid=121361"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "Thrones", 0, 100));
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=tvsearch&rid=24493"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "Thrones", 0, 100));
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=tvsearch&tvmazeid=82"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "Thrones", 0, 100));
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=tvsearch&traktid=1390"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "Thrones", 0, 100));
        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=movie&tmdbid=1399"), indexerConfig))
                .thenReturn(builder.getTestResult(1, 100, "Avengers", 0, 100));

        when(indexerWebAccess.get(new URI("http://127.0.0.1:1234/api?apikey=apikey&t=movie&imdbid=0848228"), indexerConfig))
                .thenThrow(new IndexerAccessException("some error"));

        CheckCapsResponse checkCapsRespone = testee.checkCaps(indexerConfig);
        assertEquals(5, checkCapsRespone.getIndexerConfig().getSupportedSearchIds().size());
        assertFalse(checkCapsRespone.isAllCapsChecked());
        verify(indexerWebAccess, times(7)).get(any(), eq(indexerConfig));
    }


}