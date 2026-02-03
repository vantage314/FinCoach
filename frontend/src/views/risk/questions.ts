/**
 * 风险测评题库
 * 科学模型：核心维度加权法 + 一票否决机制
 */

export interface Option {
    label: string;   // 选项文字
    score: number;   // 分值
    value: string;   // 后端标识 (A/B/C/D/E)
}

export interface Question {
    id: number;
    title: string;
    options: Option[];
    category: 'capacity' | 'tolerance' | 'liquidity';
    isKiller?: boolean; // 一票否决题
}

export const questions: Question[] = [
    // ===== 维度A：客观实力 (Capacity) =====
    {
        id: 1,
        category: 'capacity',
        title: '您的年龄段是？',
        options: [
            { label: 'A. 60岁以上', value: 'A', score: 2 },
            { label: 'B. 50-59岁', value: 'B', score: 4 },
            { label: 'C. 40-49岁', value: 'C', score: 6 },
            { label: 'D. 30-39岁', value: 'D', score: 8 },
            { label: 'E. 18-29岁', value: 'E', score: 10 }
        ]
    },
    {
        id: 2,
        category: 'capacity',
        title: '您的家庭年收入水平？',
        options: [
            { label: 'A. 10万以下', value: 'A', score: 2 },
            { label: 'B. 10-30万', value: 'B', score: 4 },
            { label: 'C. 30-50万', value: 'C', score: 6 },
            { label: 'D. 50-100万', value: 'D', score: 8 },
            { label: 'E. 100万以上', value: 'E', score: 10 }
        ]
    },
    {
        id: 3,
        category: 'liquidity',
        title: '这笔投资资金您计划多长时间内使用？',
        isKiller: true, // ⚠️ 一票否决题
        options: [
            { label: 'A. 6个月内（短期刚需）', value: 'A', score: 0 }, // 触发熔断
            { label: 'B. 6个月-1年', value: 'B', score: 4 },
            { label: 'C. 1-3年', value: 'C', score: 8 },
            { label: 'D. 3-5年', value: 'D', score: 12 },
            { label: 'E. 5年以上', value: 'E', score: 15 }
        ]
    },
    {
        id: 4,
        category: 'capacity',
        title: '您的家庭总资产（扣除负债后）大约是？',
        options: [
            { label: 'A. 10万以下', value: 'A', score: 2 },
            { label: 'B. 10-50万', value: 'B', score: 4 },
            { label: 'C. 50-200万', value: 'C', score: 6 },
            { label: 'D. 200-500万', value: 'D', score: 8 },
            { label: 'E. 500万以上', value: 'E', score: 10 }
        ]
    },

    // ===== 维度B：主观意愿 (Tolerance) =====
    {
        id: 5,
        category: 'tolerance',
        title: '如果您的投资一个月缩水了 15%，您会？',
        options: [
            { label: 'A. 无法接受，全部卖出', value: 'A', score: 2 },
            { label: 'B. 非常焦虑，卖出一部分', value: 'B', score: 4 },
            { label: 'C. 有些担心，但继续持有', value: 'C', score: 8 },
            { label: 'D. 正常波动，持有观望', value: 'D', score: 12 },
            { label: 'E. 兴奋，这是加仓良机', value: 'E', score: 15 }
        ]
    },
    {
        id: 6,
        category: 'tolerance',
        title: '您投资的主要目的是？',
        options: [
            { label: 'A. 保本，不能有任何亏损', value: 'A', score: 2 },
            { label: 'B. 跑赢银行存款利率即可', value: 'B', score: 4 },
            { label: 'C. 跑赢通胀，保持购买力', value: 'C', score: 8 },
            { label: 'D. 追求资产增值，愿承担一定风险', value: 'D', score: 12 },
            { label: 'E. 追求高收益，能承受大幅波动', value: 'E', score: 15 }
        ]
    },
    {
        id: 7,
        category: 'tolerance',
        title: '您有多少投资经验？',
        options: [
            { label: 'A. 从未投资过', value: 'A', score: 2 },
            { label: 'B. 仅买过银行理财/货币基金', value: 'B', score: 4 },
            { label: 'C. 买过债券/债基', value: 'C', score: 6 },
            { label: 'D. 买过股票/股票型基金', value: 'D', score: 10 },
            { label: 'E. 有丰富的股票/期货/期权经验', value: 'E', score: 13 }
        ]
    },
    {
        id: 8,
        category: 'tolerance',
        title: '以下哪种情况最符合您的投资偏好？',
        options: [
            { label: 'A. 收益5%，亏损0%', value: 'A', score: 2 },
            { label: 'B. 收益10%，可能亏损5%', value: 'B', score: 5 },
            { label: 'C. 收益20%，可能亏损10%', value: 'C', score: 8 },
            { label: 'D. 收益40%，可能亏损20%', value: 'D', score: 10 },
            { label: 'E. 收益100%，可能亏损50%', value: 'E', score: 12 }
        ]
    }
];

// 计算理论最高分（用于前端进度展示）
export const MAX_SCORE = questions.reduce((sum, q) => {
    const maxOption = Math.max(...q.options.map(o => o.score));
    return sum + maxOption;
}, 0);

// 题目分类标签
export const categoryLabels: Record<string, string> = {
    capacity: '📊 客观实力',
    tolerance: '💭 风险偏好',
    liquidity: '💰 资金规划'
};
