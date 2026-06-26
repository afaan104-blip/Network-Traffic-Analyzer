CREATE DATABASE network_analyzer;
USE network_analyzer;

CREATE TABLE packets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    time_epoch VARCHAR(50),
    src_ip VARCHAR(45),
    dst_ip VARCHAR(45),
    protocol VARCHAR(20),
    size INT,
    country VARCHAR(100),
    latitude DOUBLE,
    longitude DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE alerts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    time_epoch VARCHAR(50),
    src_ip VARCHAR(45),
    dst_ip VARCHAR(45),
    protocol VARCHAR(20),
    size INT,
    type VARCHAR(20),
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
