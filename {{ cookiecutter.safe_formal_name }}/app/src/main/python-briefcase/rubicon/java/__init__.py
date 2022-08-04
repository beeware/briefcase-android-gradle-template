"""Implements the Rubicon API using Chaquopy"""

from java import cast, chaquopy, dynamic_proxy, jclass


# --- Globals -----------------------------------------------------------------

JavaClass = jclass

# This wouldn't work if a class implements multiple interfaces, but Rubicon
# doesn't support that anyway.
def JavaInterface(name):
    return dynamic_proxy(jclass(name))


# --- Class attributes --------------------------------------------------------

@property
def __null__(cls):
    return cast(_java_class(cls), None)
chaquopy.JavaClass.__null__ = __null__

def __cast__(cls, obj, globalref=False):
    return cast(_java_class(cls), obj)
chaquopy.JavaClass.__cast__ = __cast__

def _java_class(cls):
    if isinstance(cls, chaquopy.DynamicProxyClass):
        # Remove the dynamic_proxy wrapper which JavaInterface added above.
        return cls.implements[0]
    else:
        return cls

# This isn't part of Rubicon's public API, but Toga uses it to work around
# limitations in Rubicon's discovery of which interfaces a class implements.
@property
def _alternates(cls):
    return []
chaquopy.JavaClass._alternates = _alternates


# --- Instance attributes -----------------------------------------------------

Object = jclass("java.lang.Object")

# In Chaquopy, all Java objects exposed to Python code already have global JNI
# references.
def __global__(self):
    return self
Object.__global__ = __global__
