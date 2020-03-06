#!/usr/bin/env sh

wget https://confluence.ecmwf.int/download/attachments/45757960/eccodes-2.16.0-Source.tar.gz
tar -xzf  eccodes-2.16.0-Source.tar.gz
mkdir build ; cd build
sudo env "PATH=$PATH" cmake -DCMAKE_INSTALL_PREFIX=/usr/bin/grib_get_data ../eccodes-2.16.0-Source
sudo env "PATH=$PATH" make
sudo env "PATH=$PATH" ctest
sudo env "PATH=$PATH" make install
cd ..