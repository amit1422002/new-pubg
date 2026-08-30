#ifndef GUEST_FAKE_DLFCN_H
#define GUEST_FAKE_DLFCN_H

#include <cstdint>

void *fake_dlopen_ex(const char *libpath, intptr_t load_addr);

void *fake_dlsym(void *handle, const char *name);

int fake_dlclose(void *handle);

#endif
