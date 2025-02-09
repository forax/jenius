#ifndef _LS_H
#define _LS_H

#include <dirent.h>
#include <sys/stat.h>

#define PARAM_SET_ALL(x) (x |= 1)
#define PARAM_IS_ALL(x) (x & 1)

#define PARAM_SET_DIR(x) (x |= 2)
#define PARAM_IS_DIR(x) (x & 2)

#define PARAM_SET_INO(x) (x |= 4)
#define PARAM_IS_INO(x) (x & 4)

#define PARAM_SET_LNG(x) (x |= 8)
#define PARAM_IS_LNG(x) (x & 8)

#define PARAM_SET_NUM(x) (x |= 16)
#define PARAM_IS_NUM(x) (x & 16)

#define PARAM_SET_REC(x) (x |= 32)
#define PARAM_IS_REC(x) (x & 32)

#define PARAM_SET_SIZ(x) (x |= 64)
#define PARAM_IS_SIZ(x) (x & 64)

int   ls_display_file(const char *path, const int drop_dir);
int   ls_display_dir(const char *path, const struct stat sb);
int   ls_display_file_long(const char *path, const struct stat sb);
char  ls_get_type(const struct stat sb);
void  ls_get_perm(const struct stat sb, char *perm);
int   ls_filter_hidden(const struct dirent *dir);
int   ls_is_dir(const struct dirent *dir);
int   ls_sort(const struct dirent **a, const struct dirent **b);
char* ls_short_path(const char *path);
int   ls_parse_args(int argc, char **argv);
void  ls_display_help(const char *progname);

#endif /* _LS_H */
