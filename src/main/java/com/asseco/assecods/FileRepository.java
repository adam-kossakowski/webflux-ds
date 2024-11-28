package com.asseco.assecods;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface FileRepository extends ReactiveCrudRepository<FileData, Long> {}
