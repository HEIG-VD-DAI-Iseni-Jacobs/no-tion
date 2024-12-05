# No-tion

## Docker

Build docker :

To push on github:

```
docker build --no-cache=true --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') -t ghcr.io/heig-vd-dai-iseni-jacobs/no-tion:latest .
```

For local use:
docker build --no-cache=true --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') -t no-tion .
`BUILD_DATE` is optional, if not set il will just be empty. Note that --no-cache=true is required if you set BUILD_DATE

docker push ghcr.io/heig-vd-dai-iseni-jacobs/no-tion

https://github.com/HEIG-VD-DAI-Iseni-Jacobs/no-tion

TODO : add nocache when building docker with date

--no-cache=true --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ')

add authorisations build.sh:
chmod +x build.sh