package com.asseco.assecods;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.springframework.http.codec.multipart.FilePart;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final StoreService storeService;

    public Flux<FileData> uploadMany(Flux<FilePart> files) {
        return files.flatMap(this::uploadSingle);
    }

    public Mono<FileData> uploadSingle(FilePart file) {
        return file.content()
                .reduce(DataBuffer::write)
                .flatMap(dataBuffer -> {
                    byte[] content = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(content);
                    DataBufferUtils.release(dataBuffer);

                    String checksum = DigestUtils.md5Hex(content);

                    FileData fileData = FileData.builder()
                            .fileName(file.filename())
                            .checksum(checksum)
                            .size((long) content.length)
                            .build();

                    InputStream contentStream = new ByteArrayInputStream(content);
                    return Mono.fromRunnable(() -> storeService.store(file.filename(), contentStream))
                            .subscribeOn(Schedulers.boundedElastic())
                            .then(Mono.just(fileData));
                })
                .flatMap(fileRepository::save);
    }
}
