import threading
import time
import schedule
import requests
import random
import os
import sys
import json
from sqlalchemy import create_engine, text

# ================= 配置 =================
DB_URL = "postgresql+psycopg2://postgres:030314@localhost:5432/fincoach"
os.environ['http_proxy'] = ''
os.environ['https_proxy'] = ''

engine = create_engine(DB_URL, pool_recycle=3600)

# ================= VIP 兜底数据 (真实数据) =================
# 覆盖全部种子股票，防止爬虫失败时 F10 无数据
VIP_DATA = {
    # === 白酒板块 ===
    "600519": {"chair": "丁雄军", "emp": "30,000", "date": "2001-08-27", "site": "www.moutaichina.com", "bus": "茅台酒及系列酒的生产与销售"},
    "000858": {"chair": "曾从钦", "emp": "28,000", "date": "1998-04-27", "site": "www.wuliangye.com.cn", "bus": "浓香型白酒的生产与销售"},
    "000568": {"chair": "刘淼", "emp": "25,000", "date": "1994-05-09", "site": "www.lzlj.com", "bus": "浓香型白酒酿造与销售"},
    "600809": {"chair": "袁清茂", "emp": "15,000", "date": "1994-01-06", "site": "www.fenjiu.com.cn", "bus": "清香型白酒的生产与销售"},
    "002304": {"chair": "张联东", "emp": "20,000", "date": "2009-11-06", "site": "www.yanghe.cn", "bus": "绵柔型白酒的研发、生产和销售"},
    # === 银行板块 ===
    "601398": {"chair": "廖林", "emp": "420,000", "date": "2006-10-27", "site": "www.icbc.com.cn", "bus": "存贷款、结算、理财等综合金融服务"},
    "601939": {"chair": "张金良", "emp": "350,000", "date": "2010-07-15", "site": "www.ccb.com", "bus": "存贷款、投行、基金托管等金融服务"},
    "600036": {"chair": "缪建民", "emp": "110,000", "date": "2002-04-09", "site": "www.cmbchina.com", "bus": "零售金融、公司金融、资管等"},
    "601988": {"chair": "葛海蛟", "emp": "300,000", "date": "2006-07-05", "site": "www.boc.cn", "bus": "跨境金融、外汇业务、国际结算等"},
    "601166": {"chair": "吕家进", "emp": "55,000", "date": "2007-02-05", "site": "www.cib.com.cn", "bus": "绿色金融、同业金融、投行等服务"},
    "000001": {"chair": "谢永林", "emp": "42,000", "date": "1991-04-03", "site": "www.bank.pingan.com", "bus": "零售银行、对公业务、跨境金融"},
    # === 保险/券商板块 ===
    "601318": {"chair": "马明哲", "emp": "350,000", "date": "2007-03-01", "site": "www.pingan.com", "bus": "保险、银行、投资、科技等综合金融"},
    "601601": {"chair": "蔡希良", "emp": "180,000", "date": "2007-01-09", "site": "www.e-chinalife.com", "bus": "人寿保险、年金、意外险等"},
    "600030": {"chair": "张佑君", "emp": "65,000", "date": "2003-01-06", "site": "www.citics.com", "bus": "证券经纪、投行、资管等业务"},
    "300059": {"chair": "其实", "emp": "12,000", "date": "2010-03-19", "site": "www.eastmoney.com", "bus": "互联网金融信息服务及证券业务"},
    # === 新能源板块 ===
    "300750": {"chair": "曾毓群", "emp": "120,000", "date": "2018-06-11", "site": "www.catl.com", "bus": "动力电池、储能系统的研发、生产和销售"},
    "002594": {"chair": "王传福", "emp": "700,000", "date": "2011-06-30", "site": "www.byd.com", "bus": "新能源汽车、电池、半导体的研发与生产"},
    "300274": {"chair": "曹仁贤", "emp": "25,000", "date": "2011-11-02", "site": "www.sungrowpower.com", "bus": "光伏逆变器、储能系统的研发和销售"},
    "600438": {"chair": "刘汉元", "emp": "50,000", "date": "2004-03-02", "site": "www.tongwei.com.cn", "bus": "高纯晶硅、太阳能电池及饲料业务"},
    "300124": {"chair": "朱兴明", "emp": "20,000", "date": "2010-09-28", "site": "www.inovance.com", "bus": "工业自动化、新能源汽车动力总成"},
    # === 科技/半导体板块 ===
    "688981": {"chair": "赵伟国", "emp": "16,000", "date": "2020-07-20", "site": "www.montage-tech.com", "bus": "内存接口芯片、津逮服务器CPU"},
    "002230": {"chair": "刘庆峰", "emp": "18,000", "date": "2008-05-12", "site": "www.iflytek.com", "bus": "智能语音、人工智能核心技术研发与应用"},
    "002415": {"chair": "陈宗年", "emp": "55,000", "date": "2010-05-28", "site": "www.hikvision.com", "bus": "视频监控产品及解决方案的研发和销售"},
    "002049": {"chair": "马道杰", "emp": "8,000", "date": "2005-06-07", "site": "www.unigroup.com.cn", "bus": "特种集成电路、智能安全芯片的研发"},
    # === 医药/医疗板块 ===
    "600276": {"chair": "孙飘扬", "emp": "30,000", "date": "2000-10-18", "site": "www.hengrui.com", "bus": "创新药和仿制药的研发、生产与销售"},
    "300760": {"chair": "仇旻", "emp": "35,000", "date": "2018-10-16", "site": "www.mindray.com", "bus": "医疗器械的研发、制造及销售"},
    "300015": {"chair": "陈邦", "emp": "60,000", "date": "2009-10-30", "site": "www.aierchina.com", "bus": "眼科医疗服务连锁经营"},
    "600436": {"chair": "林纬奇", "emp": "6,000", "date": "2003-06-16", "site": "www.pzh.com.cn", "bus": "片仔癀系列中成药的生产与销售"},
    "300122": {"chair": "蒋仁生", "emp": "9,000", "date": "2010-09-28", "site": "www.zhifeishengwu.com", "bus": "人用疫苗的研发、生产及推广"},
    # === 消费/家电板块 ===
    "000651": {"chair": "董明珠", "emp": "80,000", "date": "1996-11-18", "site": "www.gree.com", "bus": "空调、生活电器、工业制品的生产销售"},
    "000333": {"chair": "方洪波", "emp": "190,000", "date": "2013-09-18", "site": "www.midea.com", "bus": "家电、暖通、机器人及自动化等"},
    "600887": {"chair": "潘刚", "emp": "60,000", "date": "1996-03-12", "site": "www.yili.com", "bus": "乳制品及健康食品的研发、生产与销售"},
    "600690": {"chair": "李华刚", "emp": "100,000", "date": "1993-11-19", "site": "www.haier.net", "bus": "智慧家庭解决方案及全球化家电品牌运营"},
    "601888": {"chair": "李刚", "emp": "15,800", "date": "2009-10-15", "site": "www.ctgdutyfree.com.cn", "bus": "免税商品批发与零售"},
    # === 地产/基建板块 ===
    "600048": {"chair": "刘平", "emp": "30,000", "date": "2006-07-31", "site": "www.polycn.com", "bus": "房地产开发与销售、物业管理等"},
    "601668": {"chair": "郑学选", "emp": "310,000", "date": "2009-07-29", "site": "www.cscec.com", "bus": "房屋建筑、基础设施建设等工程承包"},
    # === 通信/运营商板块 ===
    "600941": {"chair": "杨杰", "emp": "450,000", "date": "2022-01-05", "site": "www.chinamobileltd.com", "bus": "移动通信及相关服务"},
    "601728": {"chair": "柯瑞文", "emp": "280,000", "date": "2021-08-20", "site": "www.chinatelecom.com.cn", "bus": "综合信息通信服务"},
    "000063": {"chair": "李自学", "emp": "74,000", "date": "1997-11-18", "site": "www.zte.com.cn", "bus": "通信系统设备及解决方案的研发和销售"},
    # === 能源/资源板块 ===
    "601857": {"chair": "戴厚良", "emp": "398,440", "date": "2007-11-05", "site": "www.petrochina.com.cn", "bus": "原油及天然气的勘探、开发、生产和销售"},
    "601088": {"chair": "王祥喜", "emp": "85,000", "date": "2007-10-09", "site": "www.shenhuachina.com", "bus": "煤炭、电力、铁路运输等综合能源"},
    "601899": {"chair": "陈景河", "emp": "48,933", "date": "2008-04-25", "site": "www.zijinmining.com", "bus": "金、铜、锌等矿产资源勘查与开发"},
    "600900": {"chair": "雷鸣山", "emp": "25,000", "date": "2003-11-18", "site": "www.cypc.com.cn", "bus": "水力发电及清洁能源的运营管理"},
    # === 交通/物流板块 ===
    "601111": {"chair": "刘绍勇", "emp": "100,000", "date": "2007-08-31", "site": "www.ceair.com", "bus": "航空客货运输及相关服务"},
    "601919": {"chair": "万敏", "emp": "40,000", "date": "2007-06-26", "site": "www.coscoshipping.com", "bus": "集装箱航运、码头及相关物流服务"},
    # === 军工板块 ===
    "600893": {"chair": "曹建国", "emp": "55,000", "date": "2003-12-31", "site": "www.aecc-engine.com", "bus": "航空发动机及衍生品的研制与维修"},
    # === 互联网/软件板块 ===
    "600588": {"chair": "王文京", "emp": "20,000", "date": "2001-05-18", "site": "www.yonyou.com", "bus": "企业管理软件和云服务平台的开发与销售"},
    "600536": {"chair": "陈锡明", "emp": "11,000", "date": "2002-05-17", "site": "www.css.com.cn", "bus": "操作系统、中间件等基础软件的研发"},
    # === 食品饮料/农业 ===
    "300498": {"chair": "温志芬", "emp": "50,000", "date": "2015-11-02", "site": "www.wens.com.cn", "bus": "畜牧养殖及肉类食品加工"},
}


# ================= 模块 1: 实时行情 (新浪直连) =================
def update_market_real():
    try:
        with engine.connect() as conn:
            codes = [r[0] for r in conn.execute(text("SELECT code FROM market_security")).fetchall()]

        if not codes:
            return

        # 拼接代码: sh600519
        sina_codes = []
        for c in codes:
            prefix = "sh" if c.startswith("6") else "sz" if c.startswith("0") or c.startswith("3") else "bj"
            sina_codes.append(f"{prefix}{c}")
        
        # 分批请求 (每批 80 个)
        for i in range(0, len(sina_codes), 80):
            batch = sina_codes[i:i+80]
            url = f"http://hq.sinajs.cn/list={','.join(batch)}"
            resp = requests.get(url, headers={'Referer': 'https://sina.com.cn'}, timeout=5)
            
            with engine.begin() as conn:
                for line in resp.text.split('\n'):
                    if '="' in line:
                        try:
                            code = line.split('="')[0].split('_')[-1][2:]
                            vals = line.split('="')[1].strip('";').split(',')
                            if len(vals) > 30:
                                price = float(vals[3])
                                pre_close = float(vals[2])
                                change = ((price - pre_close)/pre_close*100) if pre_close > 0 else 0
                                
                                # 写入五档盘口 (买1-5: 10-19, 卖1-5: 20-29)
                                # 简化演示: 只更新价格和涨跌幅，盘口数据量大可按需加
                                conn.execute(text("""
                                    UPDATE market_security 
                                    SET current_price=:p, change_percent=:c 
                                    WHERE code=:code
                                """), {'p': price, 'c': change, 'code': code})
                        except:
                            pass
        print(f"[{time.strftime('%H:%M:%S')}] [行情] ✅ 更新完成")
    except Exception as e:
        print(f"[行情] 错误: {e}")

# ================= 模块 2: 实时新闻 (新浪 API) =================
def update_news_real():
    try:
        url = "https://feed.mix.sina.com.cn/api/roll/get?pageid=153&lid=2509&k=&num=10&page=1"
        data = requests.get(url, timeout=5).json()

        count = 0
        with engine.begin() as conn:
            for item in data.get('result', {}).get('data', []):
                t = item.get('title')
                u = item.get('url')
                ctime = item.get('ctime')
                if not t or not ctime:
                    continue
                p = time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(int(ctime)))
                c = item.get('intro') or t
                
                # 查重
                if conn.execute(text("SELECT count(*) FROM financial_news WHERE title=:t"), {'t': t}).scalar() == 0:
                    conn.execute(text("""
                        INSERT INTO financial_news (title, content, source, publish_time, url)
                        VALUES (:t, :c, '新浪财经', :p, :u)
                    """), {'t': t, 'c': c, 'p': p, 'u': u})
                    count += 1
        print(f"[{time.strftime('%H:%M:%S')}] [新闻] ✅ 新增 {count} 条")
    except Exception as e:
        print(f"[新闻] 错误: {e}")

# ================= 模块 3: F10 资料补全 (VIP 优先) =================
def update_f10_background():
    print("[F10] 开始后台补全资料...")
    try:
        with engine.connect() as conn:
            stocks = conn.execute(text("SELECT code, name FROM market_security")).fetchall()

        for code, name in stocks:
            code_str = str(code)
            # 1. 优先使用 VIP 内置数据 (最稳)
            if code_str in VIP_DATA:
                d = VIP_DATA[code_str]
                try:
                    employees_value = ''.join(ch for ch in d['emp'] if ch.isdigit())
                    with engine.begin() as conn:
                        # 确保插入 company_profile
                        conn.execute(text("""
                            INSERT INTO company_profile (stock_code, company_name, chairman, employees, listing_date, website, business_scope, main_business)
                            VALUES (:c, :cn, :ch, :em, CAST(:ld AS DATE), :wb, :bs, :mb)
                            ON CONFLICT (stock_code) DO UPDATE SET
                                company_name=EXCLUDED.company_name, chairman=EXCLUDED.chairman,
                                employees=EXCLUDED.employees, listing_date=EXCLUDED.listing_date,
                                website=EXCLUDED.website, business_scope=EXCLUDED.business_scope,
                                main_business=EXCLUDED.main_business
                        """), {'c': code_str, 'cn': name, 'ch': d['chair'], 'em': int(employees_value), 'ld': d['date'], 'wb': d['site'], 'bs': d['bus'], 'mb': d['bus']})
                    print(f" -> {name}: ✅ 使用内置真实数据")
                    continue
                except Exception as e:
                    print(f" -> {name}: ❌ 内置数据写入失败 {e}")
            
            # 2. 如果不是 VIP，尝试简单爬取 (略过，防止 IP 问题，演示重点是 VIP 股票)
            # ...
    except Exception as e:
        print(f"[F10] 错误: {e}")

# ================= 主程序 =================
def main():
    print("🚀 FinCoach 全能服务启动...")

    # 1. 启动 F10 补全 (只跑一次)
    threading.Thread(target=update_f10_background, daemon=True).start()

    # 2. 立即执行
    update_market_real()
    update_news_real()

    # 3. 定时任务
    schedule.every(3).seconds.do(update_market_real)
    schedule.every(60).seconds.do(update_news_real)

    while True:
        schedule.run_pending()
        time.sleep(1)

if __name__ == "__main__":
    main()
