"""Emulation layer to make Chaquopy look like Rubicon."""

import java

JavaClass = java.jclass

def JavaInterface(name):
    # FIXME won't work if a class implements multiple interfaces: does Toga need that?
    return java.dynamic_proxy(java.jclass(name))

# In Chaquopy, all Java objects exposed to Python code already have global refs.
Object = java.jclass("java.lang.Object")
def __global__(self):
    return self
Object.__global__ = __global__

Object._alternates = []
