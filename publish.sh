#!/bin/bash

# Call build script to make sure the image is built at latest version
./build.sh

# Push the built image to GitHub Container Registry
docker push ghcr.io/heig-vd-dai-iseni-jacobs/no-tion

# Display confirmation message for user
echo -e "\e[37m\e[46mPushed no-tion:latest to GitHub Container Registry\e[0m"
