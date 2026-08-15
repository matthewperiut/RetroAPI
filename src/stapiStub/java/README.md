Signature-only stubs of the small slice of StationAPI that
`compat/stationapi/StationApiItemColors` compiles against.

Nothing here is ever shipped or loaded. The output of this source set is put on the MAIN COMPILE
classpath and nowhere else - not the runtime classpath, not the jar (`verifyNoStubsInJar` in
build.gradle enforces that, because a shipped stub would shadow the real StationAPI at runtime).

The real artifact would work too, but it is a 32 MB shell whose classes all live in nested jars that
never reach a compile classpath, so depending on it means depending on its individual submodules plus
UnsafeEvents from Jitpack - three repositories and ~130 MB to resolve seven method signatures. Same
reasoning, and the same layout, as RetroDragon's own `src/stapiStub`.

If StationAPI ever changes one of these signatures the listener will compile here and fail at
runtime, so re-check this directory against the real jar after a StationAPI bump.
