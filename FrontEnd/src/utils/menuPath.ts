import type { MenuTree } from '@/types/menu';

/**
 * 解析菜单完整路径。
 * 本项目叶子菜单 path 约定为绝对短路径（如 system/user）；
 * 分组目录（M）插入后不能改写叶子的 fullPath，否则组件映射与侧栏链接会错位。
 */
export function resolveMenuFullPath(menuPath: string | undefined, parentFullPath = ''): string {
  const cleanPath = (menuPath || '').replace(/^\//, '');
  if (!cleanPath) return parentFullPath;
  if (!parentFullPath) return cleanPath;
  if (cleanPath.startsWith(parentFullPath + '/')) return cleanPath;
  // 叶子绝对路径（含 /）且不属于当前 parent 前缀：保持原 path
  if (cleanPath.includes('/')) return cleanPath;
  return `${parentFullPath}/${cleanPath}`;
}

export function toMenuRelativePath(fullPath: string, parentFullPath = ''): string {
  if (parentFullPath && fullPath.startsWith(parentFullPath + '/')) {
    return fullPath.slice(parentFullPath.length + 1);
  }
  return fullPath;
}

/** 登录后的默认页：当前角色侧栏里的第一个叶子菜单。 */
export function firstMenuLeafPath(menus: MenuTree[] | undefined, parentPath = ''): string | null {
  for (const menu of menus || []) {
    const clean = (menu.path || '').replace(/^\//, '');
    const hasChildren = !!menu.children?.length;
    const fullPath = resolveMenuFullPath(menu.path, parentPath);
    const isGroupingOnly = clean.includes('/') && !!parentPath;
    const childParent = isGroupingOnly ? parentPath : fullPath;
    if (hasChildren) {
      const nested = firstMenuLeafPath(menu.children, childParent);
      if (nested) return nested;
      continue;
    }
    if (!clean || menu.menuType === 'F') continue;
    return `/${clean.includes('/') ? clean : fullPath}`;
  }
  return null;
}
