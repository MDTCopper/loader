# CopperLoader
The loader of mods under Copper.

## Using it as a dependency

The published artifacts are `com.github.MDTCopper.loader:core` (the loader API), `:mod` (the core mod
API) and `:desktop` (the desktop launcher):

```groovy
compileOnly "com.github.MDTCopper.loader:core:0.2.0"
compileOnly "com.github.MDTCopper.loader:mod:0.2.0"
```

A release is requested by its tag (`0.2.0`) and a snapshot by `snapshot`, the tag the release
workflow moves to the newest commit on `main`:

```groovy
compileOnly "com.github.MDTCopper.loader:core:snapshot"
```

Both versions are built by JitPack on demand, from the ref that names them. Nothing has to be
published by hand: the version JitPack asks for decides what the build is, so a tag produces a
release and anything else a snapshot, each under the version it was asked for.
