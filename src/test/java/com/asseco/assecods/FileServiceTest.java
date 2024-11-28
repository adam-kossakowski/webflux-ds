package com.asseco.assecods;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.io.IOException;

public class FileServiceTest {

    @Mock
    private StoreService storeService;

    @Mock
    private FileRepository repository;

    @InjectMocks
    private FileService fileUploadService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testUploadFile_Success() throws IOException {
        FileData fileData = FileData.builder()
                .fileName("test.txt")
                .checksum("098f6bcd4621d373cade4e832627b4f6")
                .size(12L)
                .build();

        when(repository.save(any())).thenReturn(Mono.just(fileData));

        FilePart filePart = Mockito.mock(FilePart.class);
        Mockito.when(filePart.filename()).thenReturn("test.txt");

        DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
        DataBuffer dataBuffer = bufferFactory.wrap("test content".getBytes());

        Mockito.when(filePart.content()).thenReturn(Flux.just(dataBuffer));


        Mono<FileData> result = fileUploadService.uploadSingle(filePart);

        StepVerifier.create(result)
                .expectNextMatches(fm -> fm.getFileName().equals("test.txt") && fm.getChecksum().equals("098f6bcd4621d373cade4e832627b4f6"))
                .verifyComplete();

        verify(storeService, times(1)).store(any(), any());
    }

    @Test
    public void testUploadFiles_MultipleFiles() {
        FilePart filePart = Mockito.mock(FilePart.class);
        Mockito.when(filePart.filename()).thenReturn("test1.txt");

        DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
        DataBuffer dataBuffer = bufferFactory.wrap("content1".getBytes());

        Mockito.when(filePart.content()).thenReturn(Flux.just(dataBuffer));

        FilePart filePart2 = Mockito.mock(FilePart.class);
        Mockito.when(filePart2.filename()).thenReturn("test2.txt");

        DefaultDataBufferFactory bufferFactory2 = new DefaultDataBufferFactory();
        DataBuffer dataBuffer2 = bufferFactory2.wrap("content2".getBytes());

        Mockito.when(filePart2.content()).thenReturn(Flux.just(dataBuffer2));


        FileData fileData = FileData.builder()
                .fileName("test1.txt")
                .checksum("098f6bcd4621d373cade4e832627b4f6")
                .size(8L)
                .build();

        FileData fileData2 = FileData.builder()
                .fileName("test2.txt")
                .checksum("ad0234829205b9033196ba818f7a872b")
                .size(8L)
                .build();

        when(repository.save(any(FileData.class)))
                .thenReturn(Mono.just(fileData))
                .thenReturn(Mono.just(fileData2));

        Flux<FileData> result = fileUploadService.uploadMany(Flux.just(filePart, filePart2));

        StepVerifier.create(result)
                .expectNext(fileData)
                .expectNext(fileData2)
                .verifyComplete();

        verify(storeService, times(2)).store(any(), any());
    }

    @Test
    public void testUploadFile_StorageServiceError() {
        FilePart filePart = Mockito.mock(FilePart.class);
        Mockito.when(filePart.filename()).thenReturn("test.txt");

        DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
        DataBuffer dataBuffer = bufferFactory.wrap("test content".getBytes());

        Mockito.when(filePart.content()).thenReturn(Flux.just(dataBuffer));

        doThrow(new RuntimeException("Storage service failed"))
                .when(storeService).store(any(), any());

        Mono<FileData> result = fileUploadService.uploadSingle(filePart);

        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("Storage service failed"))
                .verify();
    }
}

