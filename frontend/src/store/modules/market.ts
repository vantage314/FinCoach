import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { Security, MarketNews, MarketIndex } from '@/api/market';
import { getSecurities } from '@/api/market';

// Mock 数据 - 50+ 条证券数据（涵盖科技、消费、新能源、医疗、ETF等板块）
const mockSecurities: Security[] = [
    // ===== 白酒板块 =====
    {
        id: 1, name: '贵州茅台', code: '600519', type: 'stock', currentPrice: 1688.88, changePercent: 2.35, riskLevel: 'R3', sector: '白酒', description: '中国高端白酒龙头企业，主营贵州茅台酒系列产品的生产和销售。',
        marketCap: '2.12万亿', peRatio: 28.5, volume: '2.5万手', high52w: 1850.00, low52w: 1550.00
    },
    {
        id: 2, name: '五粮液', code: '000858', type: 'stock', currentPrice: 156.80, changePercent: 1.56, riskLevel: 'R3', sector: '白酒', description: '中国知名白酒企业，主营五粮液系列浓香型白酒。',
        marketCap: '6085亿', peRatio: 18.2, volume: '18.5万手', high52w: 175.50, low52w: 128.00
    },
    {
        id: 3, name: '泸州老窖', code: '000568', type: 'stock', currentPrice: 198.35, changePercent: -0.85, riskLevel: 'R3', sector: '白酒', description: '浓香型白酒典型代表，拥有国窖1573等高端品牌。',
        marketCap: '2920亿', peRatio: 22.1, volume: '8.2万手', high52w: 255.00, low52w: 165.00
    },
    {
        id: 4, name: '山西汾酒', code: '600809', type: 'stock', currentPrice: 265.42, changePercent: 3.12, riskLevel: 'R3', sector: '白酒', description: '清香型白酒龙头，中国名酒之一。',
        marketCap: '3250亿', peRatio: 30.5, volume: '5.6万手', high52w: 310.00, low52w: 210.00
    },

    // ===== 互联网/科技板块 =====
    {
        id: 5, name: '腾讯控股', code: '00700', type: 'stock', currentPrice: 388.20, changePercent: -1.28, riskLevel: 'R4', sector: '互联网', description: '全球领先的互联网科技公司，业务涵盖社交、游戏、金融科技等领域。',
        marketCap: '3.6万亿', peRatio: 25.8, volume: '1500万股', high52w: 420.00, low52w: 280.00
    },
    {
        id: 11, name: '苹果', code: 'AAPL', type: 'stock', currentPrice: 189.25, changePercent: 1.12, riskLevel: 'R4', sector: '科技', description: '全球市值最大的科技公司，iPhone、Mac等产品制造商。',
        marketCap: '2.95万亿美元', peRatio: 29.5, volume: '4500万股', high52w: 199.62, low52w: 150.00
    },

    // ===== 新能源板块 =====
    { id: 15, name: '比亚迪', code: '002594', type: 'stock', currentPrice: 268.50, changePercent: 4.56, riskLevel: 'R4', sector: '新能源', description: '全球领先的新能源汽车和电池制造商。' },
    { id: 16, name: '宁德时代', code: '300750', type: 'stock', currentPrice: 185.60, changePercent: 3.25, riskLevel: 'R4', sector: '新能源', description: '全球动力电池龙头企业，市占率第一。' },
    { id: 17, name: '隆基绿能', code: '601012', type: 'stock', currentPrice: 25.82, changePercent: -3.15, riskLevel: 'R4', sector: '光伏', description: '全球最大的单晶硅片制造商。' },
    { id: 18, name: '阳光电源', code: '300274', type: 'stock', currentPrice: 68.45, changePercent: 2.18, riskLevel: 'R4', sector: '光伏', description: '光伏逆变器龙头企业，新能源系统解决方案提供商。' },
    { id: 19, name: '特斯拉', code: 'TSLA', type: 'stock', currentPrice: 245.80, changePercent: -2.85, riskLevel: 'R5', sector: '新能源', description: '全球电动汽车领导者，能源存储和太阳能技术公司。' },
    { id: 20, name: '理想汽车', code: '02015', type: 'stock', currentPrice: 168.30, changePercent: 1.95, riskLevel: 'R5', sector: '新能源', description: '中国造车新势力，专注增程式电动汽车。' },
    { id: 21, name: '蔚来', code: 'NIO', type: 'stock', currentPrice: 8.25, changePercent: -4.52, riskLevel: 'R5', sector: '新能源', description: '中国高端智能电动汽车制造商。' },

    // ===== 银行/金融板块 =====
    { id: 22, name: '招商银行', code: '600036', type: 'stock', currentPrice: 35.68, changePercent: 0.85, riskLevel: 'R2', sector: '银行', description: '中国领先的股份制商业银行，专注零售银行业务。' },
    { id: 23, name: '工商银行', code: '601398', type: 'stock', currentPrice: 5.28, changePercent: 0.38, riskLevel: 'R2', sector: '银行', description: '全球最大的商业银行之一。' },
    { id: 24, name: '建设银行', code: '601939', type: 'stock', currentPrice: 7.15, changePercent: 0.56, riskLevel: 'R2', sector: '银行', description: '中国四大国有商业银行之一。' },
    { id: 25, name: '平安银行', code: '000001', type: 'stock', currentPrice: 12.35, changePercent: -0.65, riskLevel: 'R3', sector: '银行', description: '平安集团旗下银行，零售转型领先。' },
    { id: 26, name: '中国平安', code: '601318', type: 'stock', currentPrice: 48.92, changePercent: 1.25, riskLevel: 'R3', sector: '保险', description: '中国最大的综合金融服务集团。' },

    // ===== 医疗健康板块 =====
    { id: 27, name: '迈瑞医疗', code: '300760', type: 'stock', currentPrice: 285.60, changePercent: 1.85, riskLevel: 'R4', sector: '医疗器械', description: '中国医疗器械龙头，产品覆盖生命信息与支持、医学影像等。' },
    { id: 28, name: '药明康德', code: '603259', type: 'stock', currentPrice: 68.25, changePercent: -2.35, riskLevel: 'R4', sector: '医药研发', description: '全球领先的医药研发服务平台。' },
    { id: 29, name: '恒瑞医药', code: '600276', type: 'stock', currentPrice: 42.80, changePercent: 2.65, riskLevel: 'R4', sector: '创新药', description: '中国创新药龙头企业。' },
    { id: 30, name: '爱尔眼科', code: '300015', type: 'stock', currentPrice: 15.65, changePercent: -1.28, riskLevel: 'R4', sector: '医疗服务', description: '全球最大的眼科医疗连锁机构。' },
    { id: 31, name: '片仔癀', code: '600436', type: 'stock', currentPrice: 268.50, changePercent: 0.95, riskLevel: 'R3', sector: '中药', description: '国家级中药保护品种，稀缺中药龙头。' },

    // ===== 消费板块 =====
    { id: 32, name: '海天味业', code: '603288', type: 'stock', currentPrice: 38.25, changePercent: -0.52, riskLevel: 'R3', sector: '调味品', description: '中国最大的调味品生产企业。' },
    { id: 33, name: '伊利股份', code: '600887', type: 'stock', currentPrice: 28.65, changePercent: 1.15, riskLevel: 'R3', sector: '乳业', description: '中国最大的乳制品企业。' },
    { id: 34, name: '美的集团', code: '000333', type: 'stock', currentPrice: 68.90, changePercent: 2.05, riskLevel: 'R3', sector: '家电', description: '全球领先的家电制造商。' },
    { id: 35, name: '格力电器', code: '000651', type: 'stock', currentPrice: 42.15, changePercent: -0.85, riskLevel: 'R3', sector: '家电', description: '全球最大的空调制造商。' },
    { id: 36, name: '贵州茅台', code: '600519', type: 'stock', currentPrice: 1688.88, changePercent: 2.35, riskLevel: 'R3', sector: '白酒', description: '中国高端白酒龙头。' },

    // ===== 基金板块 =====
    { id: 37, name: '易方达蓝筹精选', code: '005827', type: 'fund', currentPrice: 2.1580, changePercent: 1.12, riskLevel: 'R3', sector: '混合基金', description: '投资于具有核心竞争力的蓝筹上市公司。' },
    { id: 38, name: '南方中证500ETF', code: '510500', type: 'fund', currentPrice: 6.235, changePercent: -0.45, riskLevel: 'R3', sector: '指数基金', description: '跟踪中证500指数，投资中小盘成长股。' },
    { id: 39, name: '华夏科技创新', code: '007349', type: 'fund', currentPrice: 1.8920, changePercent: 3.21, riskLevel: 'R4', sector: '科技主题', description: '重点投资科技创新领域的上市公司。' },
    { id: 40, name: '科创50ETF', code: '588000', type: 'fund', currentPrice: 0.985, changePercent: 2.85, riskLevel: 'R4', sector: '指数基金', description: '跟踪科创板50指数，投资科技创新企业。' },
    { id: 41, name: '沪深300ETF', code: '510300', type: 'fund', currentPrice: 3.856, changePercent: 0.65, riskLevel: 'R3', sector: '指数基金', description: '跟踪沪深300指数，投资A股核心资产。' },
    { id: 42, name: '中证500ETF', code: '510500', type: 'fund', currentPrice: 6.125, changePercent: -0.35, riskLevel: 'R3', sector: '指数基金', description: '跟踪中证500指数，投资中盘股。' },
    { id: 43, name: '红利低波ETF', code: '512890', type: 'fund', currentPrice: 1.285, changePercent: 0.28, riskLevel: 'R2', sector: '策略基金', description: '投资高股息低波动股票，追求稳健收益。' },
    { id: 44, name: '医疗ETF', code: '512170', type: 'fund', currentPrice: 0.658, changePercent: -1.85, riskLevel: 'R4', sector: '行业基金', description: '跟踪中证医疗指数，投资医疗健康板块。' },
    { id: 45, name: '新能源ETF', code: '516160', type: 'fund', currentPrice: 0.785, changePercent: 2.56, riskLevel: 'R4', sector: '行业基金', description: '跟踪新能源指数，投资新能源产业链。' },
    { id: 46, name: '半导体ETF', code: '512480', type: 'fund', currentPrice: 1.125, changePercent: 4.15, riskLevel: 'R5', sector: '行业基金', description: '跟踪半导体指数，投资芯片产业链。' },
    { id: 47, name: '消费ETF', code: '510150', type: 'fund', currentPrice: 2.365, changePercent: 0.85, riskLevel: 'R3', sector: '行业基金', description: '跟踪消费指数，投资消费龙头。' },
    { id: 48, name: '恒生科技ETF', code: '513180', type: 'fund', currentPrice: 0.625, changePercent: -1.25, riskLevel: 'R4', sector: '跨境基金', description: '跟踪恒生科技指数，投资港股科技龙头。' },

    // ===== 债券板块 =====
    { id: 49, name: '国债2024-01', code: '019701', type: 'bond', currentPrice: 100.25, changePercent: 0.02, riskLevel: 'R1', sector: '国债', description: '中华人民共和国财政部发行的记账式国债。' },
    { id: 50, name: '国债2025-03', code: '019725', type: 'bond', currentPrice: 99.85, changePercent: -0.05, riskLevel: 'R1', sector: '国债', description: '2025年发行的3年期国债。' },
    { id: 51, name: '国开债2024', code: '220210', type: 'bond', currentPrice: 101.20, changePercent: 0.08, riskLevel: 'R1', sector: '政策性金融债', description: '国家开发银行发行的政策性金融债券。' },
    { id: 52, name: '企业债AAA', code: '143521', type: 'bond', currentPrice: 102.50, changePercent: 0.15, riskLevel: 'R2', sector: '企业债', description: 'AAA级企业债券，信用评级最高。' },
];

// Mock 新闻数据（增加详情内容）
const mockNews: MarketNews[] = [
    {
        id: 1,
        title: '央行维持LPR利率不变，市场预期平稳',
        source: '财经日报',
        publishTime: '10:30',
        content: '中国人民银行授权全国银行间同业拆借中心公布，2026年2月20日贷款市场报价利率（LPR）为：1年期LPR为3.45%，5年期以上LPR为3.95%，均与上月持平。分析人士指出，在当前经济稳步复苏的背景下，央行选择维持LPR利率不变，有助于稳定市场预期，支持实体经济发展。'
    },
    {
        id: 2,
        title: 'A股三大指数集体高开，北向资金净流入超50亿',
        source: '证券时报',
        publishTime: '09:45',
        content: '今日A股市场三大指数集体高开，上证指数涨0.35%，深证成指涨0.52%，创业板指涨0.68%。盘面上，半导体、新能源、医药等板块涨幅居前。北向资金早盘持续流入，截至午盘，净流入超过50亿元，显示外资对A股市场信心增强。'
    },
    {
        id: 3,
        title: '新能源板块持续走强，比亚迪市值突破万亿',
        source: '金融界',
        publishTime: '11:20',
        content: '受益于新能源汽车销量持续增长，新能源板块今日表现强势。比亚迪股价大涨超过4%，总市值突破1万亿元大关，成为A股市值最大的汽车企业。分析师认为，随着全球新能源汽车渗透率持续提升，龙头企业将持续受益。'
    },
    {
        id: 4,
        title: '美联储会议纪要释放鸽派信号，美股期货上涨',
        source: '华尔街见闻',
        publishTime: '08:00',
        content: '美联储公布的最新会议纪要显示，多数委员倾向于在未来几个月内启动降息周期。受此消息影响，美股三大股指期货盘前全线上涨，纳斯达克期货涨幅超过1%。市场预期美联储可能在6月份开始首次降息。'
    },
    {
        id: 5,
        title: '国务院发布促进民营经济发展若干措施',
        source: '新华社',
        publishTime: '14:00',
        content: '国务院近日印发《关于促进民营经济发展壮大的意见》，提出31条具体措施，涵盖优化营商环境、加大金融支持、完善法治保障等方面。意见强调要为民营企业创造公平竞争的市场环境，支持民营企业参与国家重大战略。'
    },
    {
        id: 6,
        title: '科创板改革深化，做市商制度效果显著',
        source: '上海证券报',
        publishTime: '15:30',
        content: '科创板做市商制度实施一年来，市场流动性明显改善。数据显示，参与做市的股票日均换手率提升约30%，买卖价差收窄约15%。业内人士表示，做市商制度有效提升了科创板的定价效率和交易活跃度。'
    },
    {
        id: 7,
        title: 'ChatGPT引发AI投资热潮，科技股集体上涨',
        source: '第一财经',
        publishTime: '16:45',
        content: '随着人工智能技术的快速发展，AI概念股持续受到市场追捧。今日A股市场人工智能板块大涨超3%，多只个股涨停。分析师认为，AI技术的应用前景广阔，相关产业链公司有望持续受益。'
    },
];

const mockIndices: MarketIndex[] = [
    { name: '上证指数', code: 'SH000001', value: 3089.26, changePercent: 0.85, icon: '📈' },
    { name: '纳斯达克', code: 'IXIC', value: 16892.35, changePercent: 1.23, icon: '🇺🇸' },
    { name: '黄金现货', code: 'XAU', value: 2035.80, changePercent: -0.32, icon: '🥇' },
    { name: '恒生指数', code: 'HSI', value: 16589.45, changePercent: -0.58, icon: '🇭🇰' },
];

export const useMarketStore = defineStore('market', () => {
    const securities = ref<Security[]>([]);
    const news = ref<MarketNews[]>([]);
    const indices = ref<MarketIndex[]>([]);
    const loading = ref(false);

    // 分页状态
    const total = ref(0);
    const currentPage = ref(1);
    const pageSize = ref(10);

    // 查询参数
    interface QueryParams {
        type?: string;
        keyword?: string;
    }

    // 获取证券列表（支持类型、关键词搜索和分页）
    const fetchSecurities = async (params: any, silent: boolean = false) => {
        // 只有非静默模式才显示 Loading
        if (!silent) {
            loading.value = true;
        }
        try {
            const res: any = await getSecurities(params);

            // 🚀 [调试核心]：在浏览器控制台打印真实数据结构
            console.log('📊 [Market Debug] 原始响应:', res);

            // 定义临时变量接收列表和总数
            let list: any[] = [];
            let totalCount = 0;

            // 🕵️ 场景 1: 经过 Axios 拦截器处理，res 直接就是 Response Body
            if (res.data) {
                // 情况 A: 标准分页 { records: [...], total: 10 }
                if (Array.isArray(res.data.records)) {
                    console.log('✅ 识别为: MyBatis Plus 分页对象');
                    list = res.data.records;
                    totalCount = Number(res.data.total);
                }
                // 情况 B: 直接是数组 [ ... ]
                else if (Array.isArray(res.data)) {
                    console.log('✅ 识别为: 纯数组');
                    list = res.data;
                    totalCount = list.length;
                }
            }
            // 🕵️ 场景 2: 拦截器可能没剥离外层，或者结构特殊
            else if (res.records && Array.isArray(res.records)) {
                console.log('✅ 识别为: 解包后的分页对象');
                list = res.records;
                totalCount = Number(res.total);
            }
            else if (Array.isArray(res)) {
                console.log('✅ 识别为: 解包后的数组');
                list = res;
                totalCount = list.length;
            }

            // 赋值
            securities.value = list;
            total.value = totalCount;

            console.log(`🎉 最终解析: ${list.length} 条数据`);

        } catch (error) {
            console.error('❌ 获取市场数据崩溃:', error);
            securities.value = [];
        } finally {
            // 无论是否静默，最后都要把 loading 关掉（但只在非静默模式下才设为 false）
            if (!silent) {
                loading.value = false;
            }
        }
    };

    // 获取市场新闻
    const fetchNews = async () => {
        await new Promise(resolve => setTimeout(resolve, 200));
        news.value = [...mockNews];
    };

    // 获取市场指数
    const fetchIndices = async () => {
        await new Promise(resolve => setTimeout(resolve, 200));
        indices.value = [...mockIndices];
    };

    // 根据 ID 获取证券详情
    const getSecurityById = (id: number): Security | undefined => {
        return mockSecurities.find(s => s.id === id);
    };

    return {
        securities,
        news,
        indices,
        loading,
        total,
        currentPage,
        pageSize,
        fetchSecurities,
        fetchNews,
        fetchIndices,
        getSecurityById
    };
});
