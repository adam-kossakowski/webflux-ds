package com.asseco.assecods;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.io.IOException;

@WebFluxTest(FileController.class)
public class FileControllerItTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockBean
  private FileService fileUploadService;

  @MockBean
  private FileRepository repository;

  @BeforeEach
  public void setup() {
    Mockito.reset(fileUploadService);
  }

  @Test
  public void testUploadSingleFile() throws IOException {
    FileData fileData = FileData.builder()
            .fileName("test.txt")
            .checksum("098f6bcd4621d373cade4e832627b4f6")
            .size(12L)
            .build();

    MockMultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
    ByteArrayResource resource = new ByteArrayResource(mockFile.getBytes());

    when(fileUploadService.uploadSingle(any(FilePart.class))).thenReturn(Mono.just(fileData));

    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("file", resource).filename("test.txt");

    webTestClient
            .post()
            .uri("/files/upload/single")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(FileData.class)
            .isEqualTo(fileData);
  }

  @Test
  public void testUploadMultipleFiles() throws IOException {
    FileData fileData1 = FileData.builder()
            .fileName("test.txt")
            .checksum("098f6bcd4621d373cade4e832627b4f6")
            .size(12L)
            .build();

    FileData fileData2 = FileData.builder()
            .fileName("test2.txt")
            .checksum("ad0234829205b9033196ba818f7a872b")
            .size(12L)
            .build();

    MockMultipartFile mockFile1 = new MockMultipartFile("files", "test.txt", "text/plain", "content1".getBytes());
    MockMultipartFile mockFile2 = new MockMultipartFile("files", "test2.txt", "text/plain", "content2".getBytes());

    // Mocking the service to return a Flux of FileData for multiple files
    when(fileUploadService.uploadMany(any(Flux.class))).thenReturn(Flux.just(fileData1, fileData2));

    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("files", new ByteArrayResource(mockFile1.getBytes())).filename("test.txt");
    builder.part("files", new ByteArrayResource(mockFile2.getBytes())).filename("test2.txt");

    webTestClient
            .post()
            .uri("/files/upload/many")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(FileData.class)
            .contains(fileData1, fileData2);
  }

  @Test
  public void testUploadFile_InvalidFormat() throws IOException {
    MockMultipartFile mockFile = new MockMultipartFile("file", "test.txt", "application/pdf", new byte[0]);
    ByteArrayResource resource = new ByteArrayResource(mockFile.getBytes());

    // Mocking the service to throw an error for invalid file format
    when(fileUploadService.uploadSingle(any(FilePart.class)))
            .thenReturn(Mono.error(new IllegalArgumentException("Invalid file format")));

    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("file", resource).filename("test.txt");

    webTestClient
            .post()
            .uri("/files/upload/single")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().is5xxServerError();
  }
}
