#include <dirent.h>
#include <getopt.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>

#include "ls.h"

static char *progname = NULL;
static int ls_param = 0;
static char *current_path = NULL;

int main(int argc, char **argv)
{
  progname = argv[0];
  if (ls_parse_args(argc, argv) == -1)
  {
    printf("Try `%s --help' for more informations\n", argv[0]);
    return EXIT_FAILURE;
  }
  if (optind == argc)
  {
    if (ls_display_file(".", 0) == -1)
    {
      perror(progname);
      return EXIT_FAILURE;
    }
  }
  else
  {
    unsigned int i;

    for (i = optind; i < argc; ++i)
      if (ls_display_file(argv[i], 0) == -1)
      {
        perror(progname);
        return EXIT_FAILURE;
      }
  }
  return EXIT_SUCCESS;
}

int ls_display_file(const char *path, const int drop_dir)
{
  struct stat sb;

  if (lstat(path, &sb) == -1)
    return -1;
  if (S_ISDIR(sb.st_mode) && !drop_dir)
    return ls_display_dir(path, sb);
  if (PARAM_IS_INO(ls_param))
    printf("%ld ", sb.st_ino);
  if (PARAM_IS_SIZ(ls_param))
    printf("%ld ", sb.st_blocks);
  if (PARAM_IS_LNG(ls_param) || PARAM_IS_NUM(ls_param))
    return ls_display_file_long(path, sb);
  printf("%s\n", ls_short_path(path));
  return 0;
}

int ls_display_dir(const char *path, const struct stat sb)
{
  int siz;
  struct dirent **namelist;
  int (*filter)(const struct dirent *) = NULL;
  unsigned int i;

  if (!PARAM_IS_ALL(ls_param))
    filter = ls_filter_hidden;
  current_path = (char*) path;
  if ((siz = scandir(path, &namelist, filter,
          (int (*)(const void *a, const void *b)) ls_sort)) == -1)
    return -1;
  if (PARAM_IS_REC(ls_param))
    printf("%s:\n", path);
  for(i = 0U; i < siz; ++i)
  {
    int drop_dir = 1;
    int pathsiz = strlen(path) + strlen(namelist[i]->d_name) + 2;
    char *fullpath = (char*) malloc(pathsiz);
    snprintf (fullpath, pathsiz, "%s/%s", path, namelist[i]->d_name);
    if (PARAM_IS_REC(ls_param) &&
        strcmp(namelist[i]->d_name, ".") &&
        strcmp(namelist[i]->d_name, ".."))
      drop_dir = 0;
    if (ls_display_file(fullpath, drop_dir) == -1)
      return -1;
    free(fullpath);
  }
  for(i = 0U; i < siz; ++i)
    free(namelist[i]);
  free(namelist);
  return 0;
}

int ls_display_file_long(const char *path, const struct stat sb)
{
  char type;
  char perm[10];
  struct tm *date;

  type = ls_get_type(sb);
  printf("%c", type);
  ls_get_perm(sb, perm);
  printf("%s ", perm);
  if (PARAM_IS_NUM(ls_param))
    printf("%d %d ", sb.st_uid, sb.st_gid);
  else
  {
    struct passwd *pwuid;
    struct group *grgid;
    if ((pwuid = getpwuid(sb.st_uid)) == NULL)
      return -1;
    if ((grgid = getgrgid(sb.st_gid)) == NULL)
      return -1;
    printf("%s %s ", pwuid->pw_name, grgid->gr_name);
  }
  printf ("%ld ", sb.st_size);
  if ((date = localtime(&sb.st_ctime)) == NULL)
    return -1;
  printf("%d-%d-%d %d:%d ",
      date->tm_year + 1900,
      date->tm_mon + 1,
      date->tm_mday,
      date->tm_hour,
      date->tm_min);
  printf("%s", path);
  if (S_ISLNK(sb.st_mode))
  {
    char lnkbuf[256];
    char *lnkstr;
    int siz;
    if ((siz = readlink(path, lnkbuf, 256)) == -1)
      return -1;
    lnkstr = (char*) malloc(siz + 1);
    snprintf(lnkstr, siz + 1, "%s", lnkbuf);
    printf(" -> %s", lnkstr);
    free(lnkstr);
  }
  printf("\n");
  return 0;
}

char ls_get_type(const struct stat sb)
{
  unsigned int i;
  struct _typemap
  {
    char type;
    short macro;
  } typemap[] = {
    {'-', S_ISREG(sb.st_mode)},
    {'d', S_ISDIR(sb.st_mode)},
    {'c', S_ISCHR(sb.st_mode)},
    {'b', S_ISBLK(sb.st_mode)},
    {'p', S_ISFIFO(sb.st_mode)},
    {'l', S_ISLNK(sb.st_mode)},
    {'s', S_ISSOCK(sb.st_mode)}
  };
  for (i = 0U; i < 7U; ++i)
    if (typemap[i].macro == 1)
    {
      return typemap[i].type ;
    }
  return '?';
}

void ls_get_perm(const struct stat sb, char *perm)
{
  unsigned int i;
  struct _permmap
  {
    char perm;
    int value;
  } permmap[] = {
    {'r', S_IRUSR & sb.st_mode},
    {'w', S_IWUSR & sb.st_mode},
    {'x', S_IXUSR & sb.st_mode},
    {'r', S_IRGRP & sb.st_mode},
    {'w', S_IWGRP & sb.st_mode},
    {'x', S_IXGRP & sb.st_mode},
    {'r', S_IROTH & sb.st_mode},
    {'w', S_IWOTH & sb.st_mode},
    {'x', S_IXOTH & sb.st_mode}
  };
  for (i = 0U; i < 9U; ++i)
  {
    if (permmap[i].value > 0)
      perm[i] = permmap[i].perm;
    else
      perm[i] = '-';
  }
  perm[9] = '\0';
}

int ls_filter_hidden(const struct dirent *dir)
{
  if (dir->d_name[0] == '.')
    return 0;
  return 1;
}

int ls_is_dir(const struct dirent *dir)
{
  struct stat sb;
  int pathsiz = strlen(current_path) + strlen(dir->d_name) + 2;
  char *fullpath = (char*) malloc(pathsiz);

  snprintf (fullpath, pathsiz, "%s/%s", current_path, dir->d_name);
  if (stat(fullpath, &sb) == -1)
  {
    perror(progname);
    return EXIT_FAILURE;
  }
  free(fullpath);
  return S_ISDIR(sb.st_mode);
}

int ls_sort(const struct dirent **a, const struct dirent **b)
{
  if ((ls_is_dir(*a) && ls_is_dir(*b)) || (!ls_is_dir(*a) && !ls_is_dir(*b)))
    return alphasort(a, b);
  if (ls_is_dir(*a))
    return 1;
  return -1;
}

char* ls_short_path(const char *path)
{
  char *shortpath;
  if ((shortpath = strrchr(path, '/')) != NULL)
    return shortpath + 1;
  return (char*) path;
}

int ls_parse_args(int argc, char **argv)
{
  struct option longopts[] =
  {
    {"all", 0, NULL, 'a'},
    {"directory", 0, NULL, 'd'},
    {"help", 0, NULL, 'h'},
    {"inode", 0, NULL, 'i'},
    {"long", 0, NULL, 'l'},
    {"numeric-uid-gid", 0, NULL, 'n'},
    {"recursive", 0, NULL, 'R'},
    {"size", 0, NULL, 's'}
  };
  int longindex;
  int r;

  while ((r = getopt_long(
          argc, argv, "adilnRs", longopts, &longindex)) != -1)
  {
    switch(r)
    {
      case 'a': PARAM_SET_ALL(ls_param); break;
      case 'd': PARAM_SET_DIR(ls_param); break;
      case 'h': ls_display_help(argv[0]);break;
      case 'i': PARAM_SET_INO(ls_param); break;
      case 'l': PARAM_SET_LNG(ls_param); break;
      case 'n': PARAM_SET_NUM(ls_param); break;
      case 'R': PARAM_SET_REC(ls_param); break;
      case 's': PARAM_SET_SIZ(ls_param); break;
      case '?': return -1;
    }
  }
  return 0;
}

void ls_display_help(const char *progname)
{
  printf("Usage: %s [OPTION]... [FILE]...\n", progname);
  printf("  -a, --all                 do not ignore entries starting with .\n");
  printf("  -d, --directory           list directory entries instead of contents,\n                               and do not dereference symbolic links\n");
  printf("      --help                print this help\n");
  printf("  -i, --inode               print the index number of each file\n");
  printf("  -l,                       use a long listing format\n");
  printf("  -n, --numeric-uid-gid     like -l, but list numeric user and groups IDS\n");
  printf("  -s, --size                print the size of each file, in blocks\n");
}
