package com.asseco.assecods;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "file_data")
@Builder
@Data
public class FileData {

  @Id private Long id;
  private String fileName;
  private String checksum;
  private Long size;
}
