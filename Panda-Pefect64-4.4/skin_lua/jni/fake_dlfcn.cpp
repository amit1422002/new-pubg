#include "fake_dlfcn.h"

#include <cstdlib>
#include <cstring>
#include <elf.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <unistd.h>

#ifdef __aarch64__
#define Elf_Ehdr Elf64_Ehdr
#define Elf_Shdr Elf64_Shdr
#define Elf_Sym  Elf64_Sym
#else
#error "guest fake_dlfcn: arm64 only"
#endif

struct ElfCtx {
    intptr_t load_addr;
    void *dynsym;
    void *dynstr;
    int dynsym_count;
    void *symtab;
    void *symstr;
    int symtab_count;
    off_t bias;
};

static void *symAddr(const ElfCtx *ctx, const Elf_Sym *sym) {
    if (sym->st_value == 0) {
        return nullptr;
    }
    return reinterpret_cast<void *>(ctx->load_addr + static_cast<intptr_t>(sym->st_value) - ctx->bias);
}

static void *lookupInTable(const ElfCtx *ctx, const Elf_Sym *table, int count,
                           const char *strings, const char *name) {
    if (table == nullptr || strings == nullptr) {
        return nullptr;
    }
    for (int i = 0; i < count; ++i) {
        const Elf_Sym *sym = table + i;
        if (sym->st_name == 0) {
            continue;
        }
        if (ELF64_ST_TYPE(sym->st_info) != STT_FUNC && ELF64_ST_TYPE(sym->st_info) != STT_OBJECT) {
            continue;
        }
        if (strcmp(strings + sym->st_name, name) == 0) {
            return symAddr(ctx, sym);
        }
    }
    return nullptr;
}

int fake_dlclose(void *handle) {
    if (handle == nullptr) {
        return 0;
    }
    auto *ctx = static_cast<ElfCtx *>(handle);
    free(ctx->dynsym);
    free(ctx->dynstr);
    free(ctx->symtab);
    free(ctx->symstr);
    free(ctx);
    return 0;
}

void *fake_dlopen_ex(const char *libpath, intptr_t load_addr) {
    if (libpath == nullptr || load_addr == 0) {
        return nullptr;
    }

    int fd = open(libpath, O_RDONLY);
    if (fd < 0) {
        return nullptr;
    }

    off_t size = lseek(fd, 0, SEEK_END);
    if (size <= 0) {
        close(fd);
        return nullptr;
    }

    auto *elf = static_cast<Elf_Ehdr *>(mmap(nullptr, static_cast<size_t>(size),
                                             PROT_READ, MAP_SHARED, fd, 0));
    close(fd);
    if (elf == MAP_FAILED) {
        return nullptr;
    }

    auto *ctx = static_cast<ElfCtx *>(calloc(1, sizeof(ElfCtx)));
    if (ctx == nullptr) {
        munmap(elf, static_cast<size_t>(size));
        return nullptr;
    }
    ctx->load_addr = load_addr;

    const auto *shoff = reinterpret_cast<const Elf_Shdr *>(reinterpret_cast<intptr_t>(elf) + elf->e_shoff);
    const Elf_Shdr *symtab_hdr = nullptr;

    for (int i = 0; i < elf->e_shnum; ++i) {
        const Elf_Shdr *sh = shoff + i;
        switch (sh->sh_type) {
            case SHT_DYNSYM:
                ctx->dynsym = malloc(sh->sh_size);
                if (ctx->dynsym != nullptr) {
                    memcpy(ctx->dynsym, reinterpret_cast<const void *>(reinterpret_cast<intptr_t>(elf) + sh->sh_offset),
                           sh->sh_size);
                    ctx->dynsym_count = static_cast<int>(sh->sh_size / sizeof(Elf_Sym));
                }
                break;
            case SHT_SYMTAB:
                symtab_hdr = sh;
                break;
            case SHT_STRTAB:
                if (ctx->dynstr == nullptr) {
                    ctx->dynstr = malloc(sh->sh_size);
                    if (ctx->dynstr != nullptr) {
                        memcpy(ctx->dynstr,
                               reinterpret_cast<const void *>(reinterpret_cast<intptr_t>(elf) + sh->sh_offset),
                               sh->sh_size);
                    }
                }
                break;
            case SHT_PROGBITS:
                if (ctx->dynstr != nullptr && ctx->dynsym != nullptr && ctx->bias == 0) {
                    ctx->bias = static_cast<off_t>(sh->sh_addr) - static_cast<off_t>(sh->sh_offset);
                }
                break;
            default:
                break;
        }
    }

    if (symtab_hdr != nullptr && symtab_hdr->sh_link < elf->e_shnum) {
        const Elf_Shdr *str_sh = shoff + symtab_hdr->sh_link;
        ctx->symtab = malloc(symtab_hdr->sh_size);
        ctx->symstr = malloc(str_sh->sh_size);
        if (ctx->symtab != nullptr && ctx->symstr != nullptr) {
            memcpy(ctx->symtab,
                   reinterpret_cast<const void *>(reinterpret_cast<intptr_t>(elf) + symtab_hdr->sh_offset),
                   symtab_hdr->sh_size);
            memcpy(ctx->symstr,
                   reinterpret_cast<const void *>(reinterpret_cast<intptr_t>(elf) + str_sh->sh_offset),
                   str_sh->sh_size);
            ctx->symtab_count = static_cast<int>(symtab_hdr->sh_size / sizeof(Elf_Sym));
        } else {
            free(ctx->symtab);
            free(ctx->symstr);
            ctx->symtab = nullptr;
            ctx->symstr = nullptr;
        }
    }

    munmap(elf, static_cast<size_t>(size));

    if (ctx->dynsym == nullptr && ctx->symtab == nullptr) {
        fake_dlclose(ctx);
        return nullptr;
    }
    return ctx;
}

void *fake_dlsym(void *handle, const char *name) {
    if (handle == nullptr || name == nullptr) {
        return nullptr;
    }
    auto *ctx = static_cast<ElfCtx *>(handle);
    void *addr = lookupInTable(ctx, static_cast<Elf_Sym *>(ctx->dynsym), ctx->dynsym_count,
                               static_cast<char *>(ctx->dynstr), name);
    if (addr != nullptr) {
        return addr;
    }
    return lookupInTable(ctx, static_cast<Elf_Sym *>(ctx->symtab), ctx->symtab_count,
                         static_cast<char *>(ctx->symstr), name);
}
