BASE_DIR=`dirname $(readlink -f $0)`

GENERATED_WRAPPER_DIR=$BASE_DIR/target/generated-wrappers
if [ ! -d $GENERATED_WRAPPER_DIR ]; then
    mkdir -p $GENERATED_WRAPPER_DIR
fi

rm -rf $GENERATED_WRAPPER_DIR/*
javac -d $GENERATED_WRAPPER_DIR -s $GENERATED_WRAPPER_DIR $BASE_DIR/src/main/java/metro4j/*.java
javah -o $BASE_DIR/src/main/c/include/metro4j_gen.h -classpath $GENERATED_WRAPPER_DIR -stubs -force metro4j.Metro4J metro4j.MetroResult

BUILD_DIR=$BASE_DIR/build
if [ ! -d $BUILD_DIR ]; then
    mkdir -p $BUILD_DIR
fi
pushd $BUILD_DIR
cmake ..
make clean
make
popd
