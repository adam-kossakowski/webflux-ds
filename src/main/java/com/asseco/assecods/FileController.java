package com.asseco.assecods;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/upload/many")
    Flux<FileData> uploadMany(@RequestPart("files") Flux<FilePart> files){
        return fileService.uploadMany(files);
    }

    @PostMapping(value = "/upload/single")
    Mono<FileData> uploadSingle(@RequestPart("file") FilePart file){
        return  fileService.uploadSingle(file);
    }

}
