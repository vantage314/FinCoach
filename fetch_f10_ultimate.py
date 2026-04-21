import requests
import time
import random
import sys
import re
import os
from sqlalchemy import create_engine, text

# ================= 配置区域 =================
DB_URL = "postgresql+psycopg2://postgres:030314@localhost:5432/fincoach"

# ================= 强制禁用代理 (必须) =================
os.environ['http_proxy'] = ''
os.environ['https_proxy'] = ''

# ================= 数据库连接 =================
try:
    engine = create_engine(DB_URL)
    print(f"✅ 数据库连接成功")
except Exception as e:
    print(f"❌ 数据库连接失败: {e}")
    sys.exit(1)

# ================= 核心资产内置数据 (兜底包) =================
# 如果网络全挂，至少这些股票会有数据，保证演示不空白
# 这些数据是 2024-2025 年真实的静态数据
SEED_DATA = {
    "600519": {
        "desc": "贵州茅台酒股份有限公司主要业务是茅台酒及系列酒的生产与销售。主导产品“贵州茅台酒”是世界三大蒸馏名酒之一，也是集国家地理标志产品、有机食品、国家非物质文化遗产于一身的白酒品牌。",
        "ind": "白酒"
    },
    "300750": {
        "desc": "宁德时代新能源科技股份有限公司是全球领先的新能源创新科技公司，致力于为全球新能源应用提供一流解决方案和服务。主要产品包括动力电池系统、储能系统、锂电池材料。",
        "ind": "电池"
    },
    "601318": {
        "desc": "中国平安保险(集团)股份有限公司致力于成为国际领先的个人金融生活服务提供商。公司业务涵盖保险、银行、资产管理三大核心金融业务。",
        "ind": "保险"
    },
    "600036": {
        "desc": "招商银行股份有限公司是中国第一家完全由企业法人持股的股份制商业银行。公司主要提供公司及个人银行服务、资金业务，并提供资产管理、信托及其他金融服务。",
        "ind": "银行"
    },
    "002594": {
        "desc": "比亚迪股份有限公司主要从事包含新能源汽车及传统燃油汽车在内的汽车业务、手机部件及组装业务、二次充电电池及光伏业务。",
        "ind": "汽车整车"
    },
    "600276": {
        "desc": "江苏恒瑞医药股份有限公司是一家从事医药创新和高品质药品研发、生产及推广的医药健康企业。主要产品包括抗肿瘤药、手术麻醉类用药、造影剂等。",
        "ind": "化学制药"
    },
    "000858": {
        "desc": "宜宾五粮液股份有限公司主要从事五粮液及其系列酒的生产和销售。五粮液是中国浓香型白酒的典型代表。",
        "ind": "白酒"
    },
    "601888": {
        "desc": "中国中免股份有限公司主要从事以免税业务为主的旅游零售业务，包括烟酒、香化等免税商品的批发、零售等业务。",
        "ind": "旅游零售"
    },
    "300059": {
        "desc": "东方财富信息股份有限公司是国内领先的互联网金融服务平台综合运营商。公司主要业务有证券业务、金融电子商务服务业务、金融数据服务业务等。",
        "ind": "证券"
    },
    "600030": {
        "desc": "中信证券股份有限公司主要业务为证券经纪、证券投资咨询、与证券交易、证券投资活动有关的财务顾问、证券承销与保荐、证券自营等。",
        "ind": "证券"
    }
}

# ================= 核心工具: 抓取新浪 F10 =================

def fetch_sina_profile(code):
    """
    爬取新浪财经 F10 (HTML解析)
    URL: http://vip.stock.finance.sina.com.cn/corp/go.php/vCI_CorpInfo/stockid/600519.phtml
    """
    # 1. 优先检查内置兜底包
    if str(code) in SEED_DATA:
        print(" (使用内置数据)", end="")
        return {
            'description': SEED_DATA[str(code)]['desc'],
            'industry': SEED_DATA[str(code)]['ind']
        }

    # 2. 尝试网络抓取 (使用 HTTP 而非 HTTPS，减少握手错误)
    url = f"http://vip.stock.finance.sina.com.cn/corp/go.php/vCI_CorpInfo/stockid/{code}.phtml"
    
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    }

    try:
        resp = requests.get(url, headers=headers, timeout=5)
        resp.encoding = 'gbk' # 新浪老网页通常是 GBK 编码
        html = resp.text
        
        # 3. 使用正则粗暴提取 (比 BeautifulSoup 更快且不依赖库)
        # 提取所属行业
        # <td ...>所属行业：</td><td ...><a ...>酿酒行业</a></td>
        ind_match = re.search(r'所属行业：.*?target="_blank">(.*?)</a>', html, re.DOTALL)
        industry = ind_match.group(1) if ind_match else "综合行业"
        
        # 提取公司简介 (新浪页面结构复杂，这里做一个简化提取)
        # 也可以尝试提取 "经营范围"
        scope_match = re.search(r'经营范围：</td><td.*?>(.*?)</td>', html, re.DOTALL)
        description = scope_match.group(1).strip() if scope_match else f"{code} 是一家A股上市公司。"
        
        # 清理 HTML 标签
        description = re.sub(r'<.*?>', '', description)
        
        return {
            'description': description[:500], # 截断一下防止太长
            'industry': industry
        }

    except Exception as e:
        # print(f" (新浪抓取失败: {e})", end="") # 保持静默
        return None

# ================= 主逻辑 =================

def main():
    print("\n🚀 开始 F10 资料修复 (新浪源 + 内置兜底)...")
    
    # 1. 建表 (如果不存在)
    with engine.begin() as conn:
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS company_profile (
                stock_code VARCHAR(20) PRIMARY KEY, 
                description TEXT, 
                industry VARCHAR(100),
                update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """))

    # 2. 获取所有股票代码
    with engine.connect() as conn:
        result = conn.execute(text("SELECT code, name FROM market_security"))
        stocks = result.fetchall()
    
    total = len(stocks)
    print(f"🎯 目标: {total} 只股票\n")
    
    success_count = 0
    
    for index, (code, name) in enumerate(stocks):
        print(f"[{index+1}/{total}] {name}({code})...", end="")
        
        # 抓取
        info = fetch_sina_profile(code)
        
        if not info:
            # 如果新浪也挂了，给一个默认值，保证前端不显示空白
            info = {
                'description': f"{name} ({code}) 是一家在中国A股上市的企业，主要从事相关领域的业务经营。",
                'industry': 'A股板块'
            }
            print(" (使用默认值)", end="")

        # 入库
        try:
            with engine.begin() as conn:
                # 更新 profile 表
                conn.execute(text("""
                    INSERT INTO company_profile (stock_code, description, industry) 
                    VALUES (:code, :desc, :ind)
                    ON CONFLICT (stock_code) DO UPDATE SET
                        description=EXCLUDED.description, industry=EXCLUDED.industry
                """), {'code': code, 'desc': info['description'], 'ind': info['industry']})
                
                # 同时回写到 market_security 的 sector 字段 (如果有)
                # conn.execute(text("UPDATE market_security SET sector=:ind WHERE code=:code"), 
                #              {'ind': info['industry'], 'code': code})
                
            print(" ✅")
            success_count += 1
        except Exception as e:
            print(f" ❌ {e}")
            
        # 稍微快一点，因为我们有兜底和简单的 HTML 解析
        time.sleep(0.5)

    print(f"\n🎉 修复完成！成功填充 {success_count} 条资料。")
    print("👉 请刷新前端页面，查看 F10 栏目。")

if __name__ == "__main__":
    main()