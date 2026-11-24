import { lazy, Suspense, type LazyExoticComponent } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';
import BasicLayout from '@/layouts';
import componentMap from './componentMap';
import type { MenuTree } from '@/types/menu';
import { PageLoading } from '@ant-design/pro-components';
import { RouteErrorPage } from '@/components/PageErrorBoundary';
import { getRouterBasename } from '@/utils/basePath';
import { firstMenuLeafPath, pageRouteFromMenu } from '@/utils/menuPath';

// eslint-disable-next-line react-refresh/only-export-components
function PagePlaceholder() {
  const navigate = useNavigate();
  return (
    <Result
      status='warning'
      title='页面开发中...'
      subTitle='该菜单对应的页面组件尚未创建，请在 src/pages/ 下添加对应路径的 index.tsx 文件'
      extra={
        <Button
          type='primary'
          onClick={() => navigate('/system/user')}
        >
          返回首页
        </Button>
      }
    />
  );
}

const Login = lazy(() => import('@/pages/login'));
const NotFound = lazy(() => import('@/pages/404'));

function withSuspense(Component: LazyExoticComponent<any>) {
  return (
    <Suspense fallback={<PageLoading />}>
      <Component />
    </Suspense>
  );
}

function collectPageRoutes(menus: MenuTree[]): any[] {
  const seen = new Set<string>();
  const routes: any[] = [];

  const walk = (nodes?: MenuTree[]) => {
    for (const menu of nodes || []) {
      const routePath = pageRouteFromMenu(menu);
      const elementKey = routePath ? `/${routePath}` : '';
      if (routePath && !seen.has(elementKey)) {
        seen.add(elementKey);
        routes.push({
          path: routePath,
          element: componentMap[elementKey] ? withSuspense(componentMap[elementKey]) : <PagePlaceholder />,
          errorElement: <RouteErrorPage />,
        });
      }
      if (menu.children?.length) walk(menu.children);
    }
  };

  walk(menus);
  return routes;
}

export function createAppRouter(menus: MenuTree[], isLoggedIn: boolean) {
  const basename = getRouterBasename();
  const routerOpts = basename ? { basename } : undefined;

  if (!isLoggedIn) {
    return createBrowserRouter(
      [
        {
          path: '/login',
          element: withSuspense(Login),
          errorElement: <RouteErrorPage />,
        },
        {
          path: '*',
          element: (
            <Navigate
              to='/login'
              replace
            />
          ),
        },
      ],
      routerOpts,
    );
  }

  const dynamicRoutes = collectPageRoutes(menus);

  return createBrowserRouter(
    [
      {
        path: '/login',
        element: (
          <Navigate
            to='/'
            replace
          />
        ),
      },
      {
        path: '/',
        element: <BasicLayout />,
        children: [
          {
            index: true,
            element: (
              <Navigate
                to={firstMenuLeafPath(menus) || '/login'}
                replace
              />
            ),
          },
          ...dynamicRoutes,
          { path: '*', element: <NotFound />, errorElement: <RouteErrorPage /> },
        ],
      },
    ],
    routerOpts,
  );
}
