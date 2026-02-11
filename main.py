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
DB_URL = "mysql+pymysql://root:030314@localhost:3306/fincoach?charset=utf8mb4"
os.environ['http_proxy'] = ''
os.environ['https_proxy'] = ''

engine = create_engine(DB_URL, pool_recycle=3600)

# ================= VIP 兜底数据 (真实数据) =================
# 防止爬虫解析失败，直接内置核心股票数据
VIP_DATA = {
    "601857": {"chair": "戴厚良", "emp": "398,440", "date": "2007-11-05", "site": "www.petrochina.com.cn", "bus": "原油及天然气的勘探、开发、生产和销售"}, # 中国石油
    "601899": {"chair": "陈景河", "emp": "48,933", "date": "2008-04-25", "site": "www.zijinmining.com", "bus": "金、铜、锌等矿产资源勘查与开发"}, # 紫金矿业
    "600941": {"chair": "杨杰", "emp": "450,000", "date": "2022-01-05", "site": "www.chinamobileltd.com", "bus": "移动通信及相关服务"}, # 中国移动
    "601888": {"chair": "李刚", "emp": "15,800", "date": "2009-10-15", "site": "www.ctgdutyfree.com.cn", "bus": "免税商品批发与零售"}, # 中国中免
    "600519": {"chair": "丁雄军", "emp": "30,000", "date": "2001-08-27", "site": "www.moutaichina.com", "bus": "茅台酒及系列酒的生产与销售"}, # 贵州茅台
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
                            VALUES (:c, :cn, :ch, :em, :ld, :wb, :bs, :mb)
                            ON DUPLICATE KEY UPDATE company_name=:cn, chairman=:ch, employees=:em, listing_date=:ld, website=:wb, business_scope=:bs, main_business=:mb
                        """), {'c': code_str, 'cn': name, 'ch': d['chair'], 'em': employees_value, 'ld': d['date'], 'wb': d['site'], 'bs': d['bus'], 'mb': d['bus']})
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
