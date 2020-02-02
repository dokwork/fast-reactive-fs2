# fast-reactive-fs2
Alternative realization of the reactive streams for fs2. 

This version is not cross-platform, but faster than official module because it doesn't run an `IO` 
on every new element of the stream.