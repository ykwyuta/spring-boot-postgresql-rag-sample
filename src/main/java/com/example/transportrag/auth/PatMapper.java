package com.example.transportrag.auth;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PatMapper {
    String findActiveSubject(String hash);
}
