/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive.workflow_catalog.rest.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.ow2.proactive.workflow_catalog.rest.Application;
import org.ow2.proactive.workflow_catalog.rest.assembler.BucketResourceAssembler;
import org.ow2.proactive.workflow_catalog.rest.dto.BucketMetadata;
import org.ow2.proactive.workflow_catalog.rest.entity.Bucket;
import org.ow2.proactive.workflow_catalog.rest.service.repository.BucketRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;


/**
 * @author ActiveEon Team
 */
public class BucketServiceTest {

    private static final String DEFAULT_WORKFLOWS_FOLDER = "/default-workflows";

    private static final String DEFAULT_BUCKET_NAME = "BucketServiceTest";

    @InjectMocks
    private BucketService bucketService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private BucketRepository bucketRepository;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateBucket() throws Exception {
        Bucket mockedBucket = newMockedBucket(1L, "BUCKET-NAME-TEST", LocalDateTime.now());
        when(bucketRepository.save(any(Bucket.class))).thenReturn(mockedBucket);
        BucketMetadata bucketMetadata = bucketService.createBucket("BUCKET-NAME-TEST", DEFAULT_BUCKET_NAME);
        verify(bucketRepository, times(1)).save(any(Bucket.class));
        assertEquals(mockedBucket.getName(), bucketMetadata.name);
        assertEquals(mockedBucket.getCreatedAt(), bucketMetadata.createdAt);
        assertEquals(mockedBucket.getId(), bucketMetadata.id);
        assertEquals(mockedBucket.getOwner(), bucketMetadata.owner);
    }

    @Test
    public void testGetBucketMetadataValidBucket() throws Exception {
        Bucket mockedBucket = newMockedBucket(1L, "BUCKET-NAME-TEST", LocalDateTime.now());
        when(bucketRepository.findOne(anyLong())).thenReturn(mockedBucket);
        BucketMetadata bucketMetadata = bucketService.getBucketMetadata(1L);
        verify(bucketRepository, times(1)).findOne(anyLong());
        assertEquals(mockedBucket.getName(), bucketMetadata.name);
        assertEquals(mockedBucket.getCreatedAt(), bucketMetadata.createdAt);
        assertEquals(mockedBucket.getId(), bucketMetadata.id);
    }

    @Test(expected = BucketNotFoundException.class)
    public void testGetBucketMetadataInvalidBucket() throws Exception {
        when(bucketRepository.findOne(anyLong())).thenReturn(null);
        bucketService.getBucketMetadata(1L);
    }

    @Test
    public void testListBucketsNoOwner() throws Exception {
        listBucket(Optional.empty());
    }

    @Test
    public void testListBucketsWithOwner() throws Exception {
        listBucket(Optional.of("toto"));
    }

    @Test
    public void testPopulateCatalogAndFillBuckets() throws Exception {
        final String[] buckets = { "Examples", "Cloud-automation" };
        final String workflowsFolder = DEFAULT_WORKFLOWS_FOLDER;
        Bucket mockedBucket = newMockedBucket(1L, "mockedBucket", null);
        int totalNbWorkflows = 0;

        for (String bucketName : buckets) {
            File bucketFolder = new File(Application.class.getResource(workflowsFolder).getPath() + File.separator +
                                         bucketName);
            if (bucketFolder.exists()) {
                totalNbWorkflows += bucketFolder.list().length;
            }
        }
        when(bucketRepository.save(any(Bucket.class))).thenReturn(mockedBucket);
        bucketService.populateCatalog(buckets, workflowsFolder);
        verify(bucketRepository, times(buckets.length)).save(any(Bucket.class));
        verify(workflowService, times(totalNbWorkflows)).createWorkflow(anyLong(), any(Optional.class), anyObject());
    }

    @Test
    public void testPopulateCatalogWithEmptyBuckets() throws Exception {
        final String[] buckets = { "Titi", "Tata", "Toto" };
        Bucket mockedBucket = newMockedBucket(1L, "mockedBucket", null);
        when(bucketRepository.save(any(Bucket.class))).thenReturn(mockedBucket);
        bucketService.populateCatalog(buckets, DEFAULT_WORKFLOWS_FOLDER);
        verify(bucketRepository, times(buckets.length)).save(any(Bucket.class));
        verify(workflowService, times(0)).createWorkflow(anyLong(), any(Optional.class), anyObject());
    }

    @Test(expected = DefaultWorkflowsFolderNotFoundException.class)
    public void testPopulateCatalogFromInvalidFolder() throws Exception {
        final String[] buckets = { "NonExistentBucket" };
        Bucket mockedBucket = newMockedBucket(1L, "mockedBucket", null);
        when(bucketRepository.save(any(Bucket.class))).thenReturn(mockedBucket);
        bucketService.populateCatalog(buckets, "/this-folder-doesnt-exist");
    }

    private void listBucket(Optional<String> owner) {
        PagedResourcesAssembler mockedAssembler = mock(PagedResourcesAssembler.class);
        when(bucketRepository.findAll(any(Pageable.class))).thenReturn(null);
        bucketService.listBuckets(owner, null, mockedAssembler);
        if (owner.isPresent()) {
            verify(bucketRepository, times(1)).findByOwner(any(String.class), any(Pageable.class));
        } else {
            verify(bucketRepository, times(1)).findAll(any(Pageable.class));
        }
        verify(mockedAssembler, times(1)).toResource(any(PageImpl.class), any(BucketResourceAssembler.class));
    }

    private Bucket newMockedBucket(Long id, String name, LocalDateTime createdAt) {
        Bucket mockedBucket = mock(Bucket.class);
        when(mockedBucket.getId()).thenReturn(id);
        when(mockedBucket.getName()).thenReturn(name);
        when(mockedBucket.getCreatedAt()).thenReturn(createdAt);
        return mockedBucket;
    }

}
