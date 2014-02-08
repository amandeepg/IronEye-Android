#!/bin/sh

protoc --java_out=qr_middle_server/ ironeye-protocol-buffers/Message.proto
protoc --java_out=ironeye/src/main/java/ ironeye-protocol-buffers/Message.proto

