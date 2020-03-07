#!/usr/bin/env bash

# get eccodes from the server & extract it
wget https://confluence.ecmwf.int/download/attachments/45757960/eccodes-2.16.0-Source.tar.gz
tar -xzf  eccodes-2.16.0-Source.tar.gz

# create the installation dir & cd into it
mkdir eccodes-build ; cd eccodes-build

# compile and install eccodes
sudo env "PATH=$PATH" cmake -DCMAKE_INSTALL_PREFIX=/usr/local ../eccodes-2.16.0-Source
sudo env "PATH=$PATH" make
sudo env "PATH=$PATH" ctest
sudo env "PATH=$PATH" make install

# leave the eccodes-build dir
cd ..

# delete the folder to prevent spotless to check for valid file syntax in these folders
sudo rm -rf eccodes-build eccodes-2.16.0-Source
