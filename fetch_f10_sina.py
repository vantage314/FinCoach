import requests
from bs4 import BeautifulSoup
import time
import random
import sys
import os
from sqlalchemy import create_engine, text

# ================= 配置区域 =================
DB_URL = "mysql+pymysql://root:030314@localhost:3306/fincoach?charset=utf8mb4"

# ================= 强制禁用代理 (关键) =================
# 确保 requests 不会走任何可能失效的代理
os.environ['http_proxy'] = ''
os.environ['https_proxy'] = ''

# ================= 数据库连接 =================
try:
    engine = create_engine(DB_URL)
    print(f"[INFO] 数据库连接成功")
except Exception as e:
    print(f"[ERROR] 数据库连接失败: {e}")
    sys.exit(1)

# ================= 核心工具: 抓取新浪 F10 =================

def fetch_sina_details(code):
    """
    访问新浪财经 HTML 页面抓取真实资料
    """
    # 新浪 F10 页面 URL (非常稳定)
    url = f"https://vip.stock.finance.sina.com.cn/corp/go.php/vCI_CorpInfo/stockid/{code}.phtml"
    
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36",
        "Referer": "https://finance.sina.com.cn/"
    }

    try:
        # 1. 发起请求
        resp = requests.get(url, headers=headers, timeout=8)
        
        # 2. 处理编码 (新浪老页面通常是 GBK)
        resp.encoding = 'gbk'
        
        if resp.status_code != 200:
            print(f" (HTTP {resp.status_code})", end="")
            return None

        # 3. 解析 HTML
        soup = BeautifulSoup(resp.text, 'lxml')
        
        # 4. 提取数据
        # 新浪的页面结构是表格布局，我们需要找到包含 "公司简介" 的那个表格
        # 通常在 id="comInfo1" 的表格里
        
        description = "暂无简介"
        industry = "A股"
        
        # --- 提取简介 ---
        # 寻找包含 "公司简介" 文本的 td，然后找它的下一个 td
        desc_label = soup.find('td', string=lambda text: text and '公司简介' in text)
        if desc_label:
            description = desc_label.find_next_sibling('td').get_text(strip=True)
        else:
            # 备选：尝试找 "经营范围"
            scope_label = soup.find('td', string=lambda text: text and '经营范围' in text)
            if scope_label:
                description = scope_label.find_next_sibling('td').get_text(strip=True)

        # --- 提取行业 ---
        # 寻找包含 "所属行业" 文本的 td
        ind_label = soup.find('td', string=lambda text: text and '所属行业' in text)
        if ind_label:
            # 行业通常是一个链接 <a>
            ind_link = ind_label.find_next_sibling('td').find('a')
            if ind_link:
                industry = ind_link.get_text(strip=True)
            else:
                industry = ind_label.find_next_sibling('td').get_text(strip=True)

        # 验证一下数据有效性
        if len(description) < 5:
            return None

        return {
            'description': description,
            'industry': industry
        }

    except Exception as e:
        print(f" (抓取错误: {e})", end="")
        return None

# ================= 主逻辑 =================

def main():
    print("\n[INFO] 开始 F10 真实数据抓取 (新浪源)...")
    print("---------------------------------------")
    
    # 1. 准备表结构
    with engine.begin() as conn:
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS company_profile (
                stock_code VARCHAR(20) PRIMARY KEY, 
                description TEXT, 
                industry VARCHAR(100),
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """))

    # 2. 获取股票列表
    with engine.connect() as conn:
        result = conn.execute(text("SELECT code, name FROM market_security WHERE type='STOCK'")) # 只抓股票
        stocks = result.fetchall()
    
    total = len(stocks)
    print(f"[INFO] 待处理: {total} 只股票\n")
    
    success_count = 0
    
    for index, (code, name) in enumerate(stocks):
        print(f"[{index+1}/{total}] {name}({code})...", end="")
        
        # 执行抓取
        info = fetch_sina_details(code)
        
        if info:
            # 入库
            try:
                with engine.begin() as conn:
                    # 插入 profile 表
                    # 注意：Truncate description if too long
                    desc_val = info['description'][:2000]
                    conn.execute(text("""
                        INSERT INTO company_profile (stock_code, description, business_scope) 
                        VALUES (:code, :desc, :ind)
                        ON DUPLICATE KEY UPDATE description=:desc, business_scope=:ind
                    """), {'code': code, 'desc': desc_val, 'ind': info['industry']})
                    
                    # 更新 market_security 的 sector 字段
                    conn.execute(text("UPDATE market_security SET sector=:ind WHERE code=:code"), 
                                 {'ind': info['industry'], 'code': code})
                    
                print(f" [OK] [{info['industry']}]")
                success_count += 1
            except Exception as e:
                print(f" [ERROR] 入库失败: {e}")
        else:
            print(f" [WARN] 无数据")
        
        # 随机休眠
        time.sleep(random.uniform(1.0, 2.0))

    print(f"\n[INFO] 任务完成! 成功抓取: {success_count}/{total}")
    print("[INFO] 请刷新前端网页，F10 资料应该是真实的了。")

if __name__ == "__main__":
    main()