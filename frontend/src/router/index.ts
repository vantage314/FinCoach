import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import AuthLayout from '../layout/AuthLayout.vue';
import UserLayout from '../layouts/UserTopLayout.vue';
import AdminLayout from '../layouts/AdminLayout.vue';
import Login from '../pages/auth/Login.vue';
import Register from '../pages/auth/Register.vue';
import pinia from '../store';
import { useUserStore } from '@/store/modules/user';
import { ElMessage } from 'element-plus';

const routes: Array<RouteRecordRaw> = [
    {
        path: '/',
        redirect: '/app/diagnosis',
    },
    {
        path: '/auth',
        component: AuthLayout,
        children: [
            {
                path: '/login',
                name: 'Login',
                component: Login,
                meta: { title: '登录 - FinCoach' }
            },
            {
                path: '/register',
                name: 'Register',
                component: Register,
                meta: { title: '注册 - FinCoach' }
            }
        ]
    },
    {
        path: '/app',
        component: UserLayout,
        meta: { requiresAuth: true },
        children: [
            {
                path: '',
                redirect: '/app/diagnosis',
            },
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('../views/dashboard/Index.vue'),
                meta: { requiresAuth: true, title: '资产管理 - FinCoach' }
            },
            {
                path: 'market',
                name: 'Market',
                component: () => import('@/views/market/Index.vue'),
                meta: { title: '市场中心', requiresAuth: true }
            },
            {
                path: 'market/detail/:code',
                name: 'StockDetail',
                component: () => import('@/views/market/StockDetail.vue'),
                meta: { title: '证券详情', requiresAuth: true }
            },
            {
                path: 'market/security/:id',
                name: 'SecurityDetail',
                component: () => import('../views/market/SecurityDetail.vue'),
                meta: { requiresAuth: true, title: '证券详情 - FinCoach' }
            },
            {
                path: 'diagnosis',
                name: 'Diagnosis',
                component: () => import('../views/diagnosis/Index.vue'),
                meta: { requiresAuth: true, title: '资产体检 - FinCoach' }
            },
            {
                path: 'plan',
                name: 'Plan',
                component: () => import('../views/plan/Index.vue'),
                meta: { requiresAuth: true, title: '投资计划 - FinCoach' }
            },
            {
                path: 'investment',
                name: 'Investment',
                component: () => import('@/views/investment/Index.vue'),
                meta: { title: '智能投资驾驶舱', requiresAuth: true }
            },
            {
                path: 'chat',
                name: 'AiChat',
                component: () => import('@/views/chat/Index.vue'),
                meta: { title: 'AI 咨询', requiresAuth: true }
            },
            {
                path: 'asset/analysis',
                name: 'AssetAnalysis',
                component: () => import('@/views/asset/Analysis.vue'),
                meta: { title: '资产分析 - FinCoach', requiresAuth: true }
            },
            {
                path: 'asset/manage',
                name: 'AssetManage',
                component: () => import('@/views/asset/Index.vue'),
                meta: { title: '资产管理 - FinCoach', requiresAuth: true }
            },
            {
                path: 'debt',
                name: 'DebtManager',
                component: () => import('@/views/finance/DebtManager.vue'),
                meta: { title: '债务管理 - FinCoach', requiresAuth: true }
            },
            {
                path: 'cashflow',
                name: 'CashflowManager',
                component: () => import('@/views/finance/CashflowManager.vue'),
                meta: { title: '现金流管理 - FinCoach', requiresAuth: true }
            },
            {
                path: 'user/profile',
                name: 'UserProfile',
                component: () => import('../views/user/Profile.vue'),
                meta: { requiresAuth: true, title: '个人中心 - FinCoach' }
            }
        ]
    },
    {
        path: '/admin',
        component: AdminLayout,
        meta: { requiresAuth: true, roles: ['ADMIN'] },
        children: [
            {
                path: '',
                redirect: '/admin/alerts',
            },
            {
                path: 'alerts',
                name: 'AdminAlerts',
                component: () => import('../views/admin/AdminAlerts.vue'),
                meta: { requiresAuth: true, roles: ['ADMIN'], title: '预警管理 - FinCoach' }
            },
            {
                path: 'debug',
                name: 'AdminDebug',
                component: () => import('../views/admin/AdminDebug.vue'),
                meta: { requiresAuth: true, roles: ['ADMIN'], title: 'Debug 快照 - FinCoach' }
            },
            {
                path: 'data-source',
                name: 'AdminDataSource',
                component: () => import('../views/admin/AdminDataSource.vue'),
                meta: { requiresAuth: true, roles: ['ADMIN'], title: '数据源与抓取 - FinCoach' }
            },
            {
                path: 'securities/mapping',
                name: 'AdminSecuritiesMapping',
                component: () => import('../views/admin/AdminSecuritiesMapping.vue'),
                meta: { requiresAuth: true, roles: ['ADMIN'], title: '证券映射 - FinCoach' }
            },
            {
                path: 'securities/snapshots',
                name: 'AdminSecuritiesSnapshots',
                component: () => import('../views/admin/AdminSecuritiesSnapshots.vue'),
                meta: { requiresAuth: true, roles: ['ADMIN'], title: '行情快照 - FinCoach' }
            },
            {
                path: 'securities/quality',
                name: 'AdminSecuritiesQuality',
                component: () => import('../views/admin/AdminSecuritiesQuality.vue'),
                meta: { requiresAuth: true, roles: ['ADMIN'], title: '数据质量 - FinCoach' }
            },
            {
                path: 'advice/rulesets',
                name: 'AdminAdviceRulesets',
                component: () => import('../views/admin/AdminAdviceRulesets.vue'),
                meta: { requiresAuth: true, roles: ['ADMIN'], title: '建议规则集 - FinCoach' }
            }
        ]
    },
    {
        path: '/403',
        name: 'Forbidden',
        component: () => import('../views/common/Forbidden.vue'),
        meta: { requiresAuth: true, title: '无权限 - FinCoach' }
    },
    {
        path: '/risk/assessment',
        name: 'RiskAssessment',
        component: () => import('../views/risk/AssessmentWizard.vue'),
        meta: { requiresAuth: true, title: '风险测评 - FinCoach' }
    },
    {
        path: '/risk/result',
        name: 'RiskResult',
        component: () => import('../views/risk/AssessmentResult.vue'),
        meta: { requiresAuth: true, title: '测评结果 - FinCoach' }
    },
    // Legacy path compatibility
    {
        path: '/dashboard',
        redirect: '/app/dashboard',
    },
    {
        path: '/market',
        redirect: '/app/market',
    },
    {
        path: '/market/detail/:code',
        redirect: (to) => `/app/market/detail/${to.params.code}`,
    },
    {
        path: '/market/security/:id',
        redirect: (to) => `/app/market/security/${to.params.id}`,
    },
    {
        path: '/diagnosis',
        redirect: '/app/diagnosis',
    },
    {
        path: '/plan',
        redirect: '/app/plan',
    },
    {
        path: '/investment',
        redirect: '/app/investment',
    },
    {
        path: '/chat',
        redirect: '/app/chat',
    },
    {
        path: '/asset/analysis',
        redirect: '/app/asset/analysis',
    },
    {
        path: '/asset/manage',
        redirect: '/app/asset/manage',
    },
    {
        path: '/user/profile',
        redirect: '/app/user/profile',
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token');
    const userStore = useUserStore(pinia);

    if (to.meta.title) {
        document.title = to.meta.title as string;
    }

    const whiteList = ['/login', '/register'];
    const isAuthRoute = to.path.startsWith('/app') || to.path.startsWith('/admin');
    const requiresAuth = isAuthRoute || !!to.meta.requiresAuth;

    if (whiteList.includes(to.path)) {
        if (token) {
            const target = userStore.isAdmin ? '/admin/alerts' : '/app/diagnosis';
            next(target);
        } else {
            next();
        }
        return;
    }

    if (requiresAuth && !token) {
        next('/login');
        return;
    }

    const roles = Array.isArray(to.meta.roles) ? to.meta.roles : [];
    const needsAdmin = to.path.startsWith('/admin') || roles.some((role) => {
        const normalized = String(role).toUpperCase();
        return normalized === 'ADMIN' || normalized === 'ROLE_ADMIN';
    });

    if (needsAdmin && !userStore.isAdmin) {
        ElMessage.error('无权限访问该页面');
        next('/403');
        return;
    }

    next();
});

export default router;
