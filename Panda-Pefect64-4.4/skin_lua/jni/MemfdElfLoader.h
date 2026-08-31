#pragma once

#include <cstddef>

/** Load ELF bytes via memfd + android_dlopen_ext (no guest .so file on disk). */
bool blaze_load_elf_from_memory(const void *bytes, size_t len);
