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

/**
 * 页面地址以组件为准（payroll/employee/index → payroll/employee）。
 * 菜单换上级目录时不再把这段地址拼到新目录后面。
 */
export function pageRouteFromMenu(menu: { path?: string; component?: string; menuType?: string }): string | null {
  if (menu.menuType === 'M' || menu.menuType === 'F') return null;
  const componentPath = (menu.component || '').replace(/^\//, '').replace(/\/index$/, '');
  if (componentPath && componentPath !== '#') return componentPath;
  const path = (menu.path || '').replace(/^\//, '');
  return path || null;
}

/** 登录后的默认页：当前角色侧栏里的第一个叶子菜单。 */
export function firstMenuLeafPath(menus: MenuTree[] | undefined): string | null {
  for (const menu of menus || []) {
    const routePath = pageRouteFromMenu(menu);
    if (routePath) return `/${routePath}`;
    if (menu.children?.length) {
      const nested = firstMenuLeafPath(menu.children);
      if (nested) return nested;
    }
  }
  return null;
}
