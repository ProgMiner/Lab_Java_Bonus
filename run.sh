#!/usr/bin/env sh


archs='block nonblock async'
params='array_size:1000..10000,1000 clients:10..100,10 request_delta:0..100,10'

for param in $params ; do
    param_name=${param%%:*}
    param_value=${param#*:}

    for arch in $archs ; do
        args="--arch=$arch --$param_name=$param_value --output_dir=./report/$param_name/$arch"

        echo $args
        java -jar ./target/servertester-1.0-SNAPSHOT-jar-with-dependencies.jar $args
    done
done
