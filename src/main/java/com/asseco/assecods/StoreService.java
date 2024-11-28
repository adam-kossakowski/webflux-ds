package com.asseco.assecods;

import java.io.InputStream;

public interface StoreService {
    void store(String fileName, InputStream content);
}
