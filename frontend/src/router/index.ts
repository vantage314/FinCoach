import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import AuthLayout from '../layout/AuthLayout.vue';
import Login from '../pages/auth/Login.vue';
import Register from '../pages/auth/Register.vue';

const routes: Array<RouteRecordRaw> = [
    {
        path: '/',
        redirect: '/login',
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
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/Index.vue'),
        meta: { requiresAuth: true, title: '资产总览 - FinCoach' }
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
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

// 全局路由守卫
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token');

    if (to.meta.title) {
        document.title = to.meta.title as string;
    }

    if (to.meta.requiresAuth && !token) {
        next('/login');
    } else {
        next();
    }
});

export default router;
